package com.interviewpilot.exception;

/** The request is valid but not allowed in the resource's current state (HTTP 409). */
public class ConflictException extends CustomException {

    public ConflictException(String message) {
        super(message);
    }
}
