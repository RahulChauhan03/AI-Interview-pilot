package com.interviewpilot.exception;

public class ExpiredResetTokenException extends CustomException {
    public ExpiredResetTokenException(String message) {
        super(message);
    }
}
