package com.interviewpilot.resume.ai;

import com.interviewpilot.exception.OllamaException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;

    @Value("${resume.ollama.endpoint:http://localhost:11434/api/generate}")
    private String endpoint;

    @Value("${resume.ollama.model:llama3.2}")
    private String model;

    @Value("${resume.ollama.temperature:0.1}")
    private double temperature;

    public OllamaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String analyzeResume(String cleanText) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "stream", false,
                "temperature", temperature,
                "format", "json",
                "prompt", buildPrompt(cleanText)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);
            if (response == null || response.get("response") == null) {
                throw new OllamaException("Ollama returned no response");
            }
            System.out.println(response);
            return response.get("response").toString();
        } catch (Exception exception) {
            throw new OllamaException("Failed to call Ollama: " + exception.getMessage());
        }
    }

    private String buildPrompt(String cleanText) {
        return "You are an expert ATS Resume Parser.\n"
                + "Read the resume carefully.\n"
                + "Extract every possible detail.\n"
                + "Return ONLY valid JSON.\n"
                + "Fields: Personal Information (First Name, Last Name, Email, Phone, Location, LinkedIn, Github, Portfolio), "
                + "Experience (Current Company, Previous Companies, Designation, Total Experience), "
                + "Education, Projects, Skills, Technical Skills, Soft Skills, Achievements, Languages, Certifications, "
                + "Tools, Frameworks, Databases, Cloud, DevOps, Summary, Keywords.\n"
                + "Resume text:\n" + cleanText;
    }
}
