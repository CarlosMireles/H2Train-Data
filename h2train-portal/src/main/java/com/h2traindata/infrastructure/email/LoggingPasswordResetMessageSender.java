package com.h2traindata.infrastructure.email;

import com.h2traindata.application.port.out.PasswordResetMessageSender;
import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.mail.password-reset", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetMessageSender implements PasswordResetMessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMessageSender.class);

    @Override
    public void sendPasswordReset(String email, String username, URI resetUri, Instant expiresAt) {
        log.info(
                "Password reset link generated for email={} username={} expiresAt={} resetUri={}",
                email,
                username,
                expiresAt,
                resetUri
        );
    }
}
