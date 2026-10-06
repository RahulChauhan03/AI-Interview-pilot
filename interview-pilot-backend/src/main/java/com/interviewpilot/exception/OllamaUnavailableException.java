package com.interviewpilot.exception;

/**
 * A temporary Ollama failure (refused connection, connect timeout, HTTP 5xx) that is worth retrying.
 * A read timeout is deliberately not one of these: the model was already working on the request.
 * Retries are configured for this type only, see resilience4j.retry.instances.ollama.
 */
public class OllamaUnavailableException extends OllamaException {

    public OllamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
