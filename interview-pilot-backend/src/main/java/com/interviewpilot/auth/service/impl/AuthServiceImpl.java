package com.interviewpilot.auth.service.impl;

import com.interviewpilot.auth.service.AuthService;
import com.interviewpilot.auth.dto.LoginRequestDto;
import com.interviewpilot.auth.dto.LoginResponseDto;
import com.interviewpilot.auth.dto.RegisterRequestDto;
import com.interviewpilot.auth.dto.RegisterResponseDto;
import com.interviewpilot.auth.dto.ForgotPasswordRequestDto;
import com.interviewpilot.auth.dto.ResetPasswordRequestDto;
import com.interviewpilot.auth.entity.PasswordResetToken;
import com.interviewpilot.auth.repository.PasswordResetTokenRepository;
import com.interviewpilot.auth.service.EmailService;
import com.interviewpilot.exception.ExpiredResetTokenException;
import com.interviewpilot.exception.InvalidResetTokenException;
import com.interviewpilot.common.enums.Role;
import com.interviewpilot.exception.EmailAlreadyExistsException;
import com.interviewpilot.exception.InvalidCredentialsException;
import com.interviewpilot.security.jwt.JwtService;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.password-reset.token-expiration-minutes:30}")
    private long tokenExpirationMinutes;

    @org.springframework.beans.factory.annotation.Value("${app.password-reset.request-cooldown-seconds:60}")
    private long requestCooldownSeconds;

    @Override
    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account already exists for this email address");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(user);

        return RegisterResponseDto.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user.getEmail());
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresIn(jwtService.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // This deliberately returns normally for an unknown address to prevent user enumeration.
        userRepository.findByEmail(email).ifPresent(user -> {
            LocalDateTime cooldownStart = LocalDateTime.now().minusSeconds(requestCooldownSeconds);
            if (passwordResetTokenRepository.existsByUserAndCreatedAtAfter(user, cooldownStart)) {
                // Preserve the same outward success response; do not generate email/token spam.
                return;
            }

            passwordResetTokenRepository.invalidateActiveTokensForUser(user);
            String rawToken = createSecureToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(sha256(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user, rawToken, tokenExpirationMinutes);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        String tokenHash = sha256(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidResetTokenException("The password reset link is invalid."));

        if (resetToken.isUsed()) {
            throw new InvalidResetTokenException("The password reset link has already been used.");
        }
        if (!resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ExpiredResetTokenException("The password reset link has expired. Request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        resetToken.setUsed(true);
        passwordResetTokenRepository.invalidateActiveTokensForUser(user);
    }

    private String createSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits of entropy; Base64 URL form is about 43 characters.
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
