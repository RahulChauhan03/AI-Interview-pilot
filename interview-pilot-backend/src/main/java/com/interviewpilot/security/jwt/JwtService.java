package com.interviewpilot.security.jwt;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateToken(String username);

    String extractUsername(String token);

    boolean validateToken(String token, UserDetails userDetails);

    boolean isTokenExpired(String token);

    long getExpirationMs();
}
