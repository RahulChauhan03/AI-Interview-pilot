package com.interviewpilot.exception;

/** Ollama answered, but the answer does not match the expected structure or value ranges. */
public class InvalidAiResponseException extends OllamaException {

    public InvalidAiResponseException(String message) {
        super(message);
    }
}
