package com.interviewpilot.resume.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

/** Loads the JSON schemas under resources/ollama that are sent to Ollama as the response "format". */
public final class JsonSchemas {

    private JsonSchemas() {
    }

    public static Map<String, Object> load(ObjectMapper objectMapper, String classpathLocation) {
        try (InputStream schema = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readValue(schema, new TypeReference<>() { });
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load JSON schema " + classpathLocation, exception);
        }
    }
}
