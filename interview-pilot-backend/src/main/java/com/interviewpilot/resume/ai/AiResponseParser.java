package com.interviewpilot.resume.ai;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.InvalidAiResponseException;
import org.springframework.stereotype.Component;

/**
 * Turns a raw Ollama answer into one of the AI response records (e.g. {@link ResumeAnalysis}), rejecting
 * anything that does not match: invalid JSON, missing or null fields, or wrong types (e.g. a string where
 * a list is expected). Error messages describe where the problem is, never the content.
 */
@Component
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parse(String aiJson, Class<T> type) {
        if (aiJson == null || aiJson.isBlank()) {
            throw new InvalidAiResponseException("AI response is empty");
        }
        try {
            return objectMapper.readerFor(type)
                    .with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                            DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .readValue(aiJson);
        } catch (JsonProcessingException exception) {
            // The cause is not attached: Jackson messages can quote parts of the AI response.
            throw new InvalidAiResponseException("AI response does not match the expected format" + describe(exception));
        }
    }

    private String describe(JsonProcessingException exception) {
        if (exception instanceof JsonMappingException mappingException && !mappingException.getPath().isEmpty()) {
            String field = mappingException.getPath().get(0).getFieldName();
            if (field != null) {
                return " (field: " + field + ")";
            }
        }
        JsonLocation location = exception.getLocation();
        return location == null ? "" : " (line " + location.getLineNr() + ", column " + location.getColumnNr() + ")";
    }
}
