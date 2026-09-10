package com.interviewpilot.auth.service;

import com.interviewpilot.user.entity.User;

public interface EmailService {

    void sendPasswordResetEmail(User user, String rawToken, long expirationMinutes);
}
