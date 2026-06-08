package com.h2traindata.infrastructure.email;

import com.h2traindata.application.port.out.PasswordResetMessageSender;
import java.net.URI;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "app.mail.password-reset", name = "enabled", havingValue = "true")
public class SmtpPasswordResetMessageSender implements PasswordResetMessageSender {

    private final JavaMailSender mailSender;
    private final PasswordResetMailProperties properties;

    public SmtpPasswordResetMessageSender(JavaMailSender mailSender,
                                          PasswordResetMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendPasswordReset(String email, String username, URI resetUri, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(email);
        message.setSubject(properties.getSubject());
        message.setText(body(username, resetUri, expiresAt));
        mailSender.send(message);
    }

    private String body(String username, URI resetUri, Instant expiresAt) {
        String greetingName = StringUtils.hasText(username) ? username : "H2Train user";
        return """
                Hello %s,

                Use the following link to reset your H2Train password:

                %s

                This link expires at %s and can only be used once.

                If you did not request this password reset, ignore this message.
                """.formatted(greetingName, resetUri, expiresAt);
    }
}
