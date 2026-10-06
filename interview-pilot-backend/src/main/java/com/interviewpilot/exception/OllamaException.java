package com.interviewpilot.exception;

public class OllamaException extends CustomException {

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
