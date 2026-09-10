package com.interviewpilot.auth.service.impl;

import com.interviewpilot.auth.service.EmailService;
import com.interviewpilot.exception.EmailSendingException;
import com.interviewpilot.user.entity.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String senderEmail;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    private static final Logger log =
        LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendPasswordResetEmail(User user, String rawToken, long expirationMinutes) {
        try {
            String resetUrl = resetPasswordUrl + (resetPasswordUrl.contains("?") ? "&" : "?")
                    + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(senderEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset your Interview Pilot password");
            helper.setText(emailBody(user.getFirstName(), resetUrl, expirationMinutes), true);
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("Failed to send password reset email", exception);
            throw new EmailSendingException("Unable to send the password reset email.",exception);
        }
    }

    private String emailBody(String firstName, String resetUrl, long expirationMinutes) {
        return "<p>Hello " + HtmlUtils.htmlEscape(firstName) + ",</p>"
                + "<p>We received a request to reset your password.</p>"
                + "<p><a href=\"" + HtmlUtils.htmlEscape(resetUrl) + "\">Reset Password</a></p>"
                + "<p>This link will expire in " + expirationMinutes + " minutes.</p>"
                + "<p>If you did not request a password reset, you can safely ignore this email.</p>"
                + "<p>Regards,<br>Interview Pilot Team</p>";
    }
}
