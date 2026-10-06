package com.interviewpilot.resume.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.interviewpilot.exception.OllamaException;
import com.interviewpilot.exception.OllamaUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Runs OllamaService with real AOP and the retry / circuit breaker settings from application.properties
 * (only the retry wait is shortened), against a mocked Ollama HTTP endpoint.
 */
@SpringBootTest(classes = OllamaServiceTest.TestConfig.class, properties = {
        "resume.ollama.endpoint=" + OllamaServiceTest.ENDPOINT,
        "resilience4j.retry.instances.ollama.wait-duration=10ms"
})
class OllamaServiceTest {

    static final String ENDPOINT = "http://ollama.test/api/generate";
    private static final String OK_RESPONSE = "{\"response\": \"{}\", \"done\": true}";

    @Configuration
    @ImportAutoConfiguration({AopAutoConfiguration.class, JacksonAutoConfiguration.class,
            RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    @Import(OllamaService.class)
    static class TestConfig {
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private OllamaService ollamaService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private MockRestServiceServer ollama;

    @BeforeEach
    void setUp() {
        ollama = MockRestServiceServer.bindTo(restTemplate).build();
        circuitBreakerRegistry.circuitBreaker("ollama").reset();
    }

    @Test
    void sendsTemperatureInsideOptionsAndSchemaAsFormat() {
        ollama.expect(once(), requestTo(ENDPOINT))
                .andExpect(jsonPath("$.options.temperature").value(0.1))
                .andExpect(jsonPath("$.options.num_predict").value(3000))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format.type").value("object"))
                .andExpect(jsonPath("$.format.required", hasItem("technicalSkills")))
                .andExpect(jsonPath("$.format.properties.personalInformation.properties.firstName.type").value("string"))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ollamaService.analyzeResume("resume text")).isEqualTo("{}");
        ollama.verify();
    }

    @Test
    void generateJsonSendsGivenSchemaTemperatureAndContextSize() {
        ollama.expect(once(), requestTo(ENDPOINT))
                .andExpect(jsonPath("$.format.required[0]").value("score"))
                .andExpect(jsonPath("$.options.temperature").value(0.7))
                .andExpect(jsonPath("$.options.num_ctx").value(8192))
                .andExpect(jsonPath("$.prompt").value("score this"))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ollamaService.generateJson("score this", Map.of("type", "object", "required", List.of("score")), 0.7))
                .isEqualTo("{}");
        ollama.verify();
    }

    @Test
    void generateJsonIsRetriedLikeEveryOtherCall() {
        ollama.expect(times(2), requestTo(ENDPOINT)).andRespond(withServiceUnavailable());
        ollama.expect(once(), requestTo(ENDPOINT)).andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ollamaService.generateJson("prompt", Map.of("type", "object"))).isEqualTo("{}");
        ollama.verify();
    }

    @Test
    void retriesTemporaryFailureAndThenSucceeds() {
        ollama.expect(times(2), requestTo(ENDPOINT)).andRespond(withServiceUnavailable());
        ollama.expect(once(), requestTo(ENDPOINT)).andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ollamaService.analyzeResume("resume text")).isEqualTo("{}");
        ollama.verify();
    }

    @Test
    void unavailableOllamaIsRetriedThreeTimesThenFails() {
        ollama.expect(times(3), requestTo(ENDPOINT)).andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text"))
                .isInstanceOf(OllamaUnavailableException.class)
                .hasMessageNotContaining("resume text");
        ollama.verify();
    }

    @Test
    void answerCutOffAtTokenLimitIsRejectedWithoutRetry() {
        ollama.expect(once(), requestTo(ENDPOINT)).andRespond(withSuccess(
                "{\"response\": \"{\\\"keywords\\\": [\\\"Java\\\", \\\"Java\\\", \", \"done\": true, \"done_reason\": \"length\"}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text"))
                .isInstanceOf(OllamaException.class)
                .isNotInstanceOf(OllamaUnavailableException.class)
                .hasMessage("Ollama stopped at the output token limit (3000)");
        ollama.verify();
    }

    @Test
    void readTimeoutIsNotRetried() {
        ollama.expect(once(), requestTo(ENDPOINT)).andRespond(withException(new HttpTimeoutException("Request cancelled")));

        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text"))
                .isInstanceOf(OllamaException.class)
                .isNotInstanceOf(OllamaUnavailableException.class)
                .hasMessage("Ollama did not return a complete answer (read timeout or dropped connection)");
        ollama.verify();
    }

    @Test
    void clientErrorIsNotRetried() {
        ollama.expect(once(), requestTo(ENDPOINT)).andRespond(withBadRequest());

        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text"))
                .isInstanceOf(OllamaException.class)
                .isNotInstanceOf(OllamaUnavailableException.class);
        ollama.verify();
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndThenFailsFast() {
        // minimum-number-of-calls=5: the circuit opens on the 5th failed attempt.
        ollama.expect(times(5), requestTo(ENDPOINT)).andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text")).isInstanceOf(OllamaUnavailableException.class);
        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text")).isInstanceOf(CallNotPermittedException.class);
        assertThatThrownBy(() -> ollamaService.analyzeResume("resume text")).isInstanceOf(CallNotPermittedException.class);

        assertThat(circuitBreakerRegistry.circuitBreaker("ollama").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        ollama.verify(); // no HTTP call was made once the circuit was open
    }
}
