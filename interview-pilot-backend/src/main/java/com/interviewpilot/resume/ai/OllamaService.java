package com.interviewpilot.resume.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.OllamaException;
import com.interviewpilot.exception.OllamaUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.ConnectException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * The only class that talks to Ollama. Every call asks for structured output (a JSON schema as "format")
 * and is protected by the "ollama" retry and circuit breaker: transient failures are retried, and while
 * the circuit is open calls fail fast with CallNotPermittedException. Exception messages never contain
 * the prompt (resume text, answers, job descriptions).
 *
 * Each schema file may set "x-max-output-tokens": the output budget for that task (Ollama's num_predict),
 * capped by resume.ollama.max-output-tokens. The key is removed before the schema is sent. Together with the
 * maxItems/maxLength bounds in the schemas this stops a small model from generating for ever.
 */
@Service
public class OllamaService {

    private static final String RESUME_SCHEMA_PATH = "ollama/resume-analysis-schema.json";
    static final String MAX_TOKENS_KEY = "x-max-output-tokens";

    private final RestTemplate restTemplate;
    private final Map<String, Object> resumeSchema;

    @Value("${resume.ollama.endpoint}")
    private String endpoint;

    @Value("${resume.ollama.model}")
    private String model;

    @Value("${resume.ollama.temperature}")
    private double temperature;

    /** Ollama's num_predict upper bound for every task; schemas can set a lower per-task budget. */
    @Value("${resume.ollama.max-output-tokens}")
    private int maxOutputTokens;

    /** Ollama's num_ctx: large enough for a resume, a job description and the answer together. */
    @Value("${resume.ollama.context-tokens}")
    private int contextTokens;

    public OllamaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.resumeSchema = JsonSchemas.load(objectMapper, RESUME_SCHEMA_PATH);
    }

    public String modelName() {
        return model;
    }

    /**
     * Cheap health check for the admin dashboard (Ollama's /api/version). Deliberately not retried and not
     * counted by the circuit breaker, so monitoring never affects real AI calls.
     */
    public boolean isReachable() {
        try {
            restTemplate.getForObject(URI.create(endpoint).resolve("/api/version"), String.class);
            return true;
        } catch (RestClientException | IllegalArgumentException exception) {
            return false;
        }
    }

    /** Extracts structured resume data; see resources/ollama/resume-analysis-schema.json. */
    @Retry(name = "ollama")
    @CircuitBreaker(name = "ollama")
    public String analyzeResume(String cleanText) {
        return generate(buildResumePrompt(cleanText), resumeSchema, temperature);
    }

    /** Returns the model's JSON answer for the given prompt and schema, using the configured low temperature. */
    @Retry(name = "ollama")
    @CircuitBreaker(name = "ollama")
    public String generateJson(String prompt, Map<String, Object> schema) {
        return generate(prompt, schema, temperature);
    }

    /** Same as {@link #generateJson(String, Map)} with an explicit temperature, e.g. for more varied questions. */
    @Retry(name = "ollama")
    @CircuitBreaker(name = "ollama")
    public String generateJson(String prompt, Map<String, Object> schema, double temperature) {
        return generate(prompt, schema, temperature);
    }

    private String generate(String prompt, Map<String, Object> schema, double temperature) {
        int tokenBudget = tokenBudget(schema);
        Map<String, Object> format = new LinkedHashMap<>(schema);
        format.remove(MAX_TOKENS_KEY);
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "stream", false,
                "format", format,
                "options", Map.of(
                        "temperature", temperature,
                        "num_predict", tokenBudget,
                        "num_ctx", contextTokens),
                "prompt", prompt
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(endpoint, request, Map.class);
        } catch (ResourceAccessException exception) {
            if (isConnectionFailure(exception)) {
                // Refused connection, unknown host or connect timeout: the request never reached Ollama.
                throw new OllamaUnavailableException("Ollama is unreachable", exception);
            }
            // Ollama accepted the request but no complete answer came back (read timeout or dropped connection).
            // Not retried: repeating the same slow generation would only add more load to a busy model.
            throw new OllamaException("Ollama did not return a complete answer (read timeout or dropped connection)", exception);
        } catch (HttpServerErrorException exception) {
            throw new OllamaUnavailableException("Ollama returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            // 4xx (e.g. unknown model) or an unreadable response: retrying will not help.
            throw new OllamaException("Ollama request failed", exception);
        }
        if (response == null || response.get("response") == null) {
            throw new OllamaException("Ollama returned no response");
        }
        if ("length".equals(response.get("done_reason"))) {
            // The answer was cut off at num_predict, so it is not complete JSON.
            throw new OllamaException("Ollama stopped at the output token limit (" + tokenBudget + ")");
        }
        return response.get("response").toString();
    }

    int tokenBudget(Map<String, Object> schema) {
        return schema.get(MAX_TOKENS_KEY) instanceof Number budget ? Math.min(budget.intValue(), maxOutputTokens) : maxOutputTokens;
    }

    private static boolean isConnectionFailure(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConnectException) {
                return true;
            }
        }
        return false;
    }

    private String buildResumePrompt(String cleanText) {
        return "You are an expert ATS resume parser.\n"
                + "Extract the details of the resume below into the given JSON structure.\n"
                + "Use only information found in the resume. Use an empty string or an empty list when something is missing.\n"
                + "technicalSkills: programming languages, frameworks, tools, databases, cloud and DevOps. "
                + "skills: all skills. keywords: important ATS keywords. "
                + "yearsOfExperience: total professional experience, e.g. \"2.5 years\". "
                + "language: the language the resume is written in.\n"
                + "Resume text:\n" + cleanText;
    }
}
