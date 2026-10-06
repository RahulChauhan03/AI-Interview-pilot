package com.interviewpilot.resume.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.OllamaException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.resume.config.ResumeProcessingConfig;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/** Uses a real HTTP connection and the real RestTemplate configuration (with short timeouts). */
class OllamaServiceTimeoutTest {

    private HttpServer server;
    private final CountDownLatch releaseHangingRequest = new CountDownLatch(1);

    @AfterEach
    void stopServer() {
        releaseHangingRequest.countDown();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void hangingOllamaIsCutOffByReadTimeout() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/generate", exchange -> {
            try {
                releaseHangingRequest.await(30, TimeUnit.SECONDS); // never answers within the read timeout
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        OllamaService service = serviceFor("http://localhost:" + server.getAddress().getPort() + "/api/generate");

        long start = System.nanoTime();
        assertThatThrownBy(() -> service.analyzeResume("resume text"))
                .isInstanceOf(OllamaException.class)
                .isNotInstanceOf(OllamaUnavailableException.class) // read timeouts are not retried
                .hasMessage("Ollama did not return a complete answer (read timeout or dropped connection)");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void refusedConnectionIsReportedAsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        OllamaService service = serviceFor("http://localhost:" + closedPort + "/api/generate");

        assertThatThrownBy(() -> service.analyzeResume("resume text")).isInstanceOf(OllamaUnavailableException.class);
    }

    private OllamaService serviceFor(String endpoint) throws IOException {
        RestTemplate restTemplate = new ResumeProcessingConfig()
                .restTemplate(new RestTemplateBuilder(), Duration.ofSeconds(1), Duration.ofMillis(500));
        OllamaService service = new OllamaService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "endpoint", endpoint);
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "temperature", 0.1);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 3000);
        ReflectionTestUtils.setField(service, "contextTokens", 8192);
        return service;
    }
}
