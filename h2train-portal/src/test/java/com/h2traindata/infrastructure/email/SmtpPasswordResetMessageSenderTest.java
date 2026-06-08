package com.h2traindata.infrastructure.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

class SmtpPasswordResetMessageSenderTest {

    @Test
    void sendsPasswordResetMessageThroughJavaMailSender() {
        CapturingJavaMailSender mailSender = new CapturingJavaMailSender();
        PasswordResetMailProperties properties = new PasswordResetMailProperties();
        properties.setFrom("support@h2train.test");
        properties.setSubject("Recover your H2Train account");
        SmtpPasswordResetMessageSender sender = new SmtpPasswordResetMessageSender(mailSender, properties);

        sender.sendPasswordReset(
                "runner@example.com",
                "runner",
                URI.create("https://h2train.test/account/password/reset?token=abc"),
                Instant.parse("2026-06-08T10:30:00Z")
        );

        assertNotNull(mailSender.message);
        assertEquals("support@h2train.test", mailSender.message.getFrom());
        assertEquals("runner@example.com", mailSender.message.getTo()[0]);
        assertEquals("Recover your H2Train account", mailSender.message.getSubject());
        assertTrue(mailSender.message.getText().contains("Hello runner"));
        assertTrue(mailSender.message.getText().contains("https://h2train.test/account/password/reset?token=abc"));
        assertTrue(mailSender.message.getText().contains("2026-06-08T10:30:00Z"));
    }

    private static final class CapturingJavaMailSender implements JavaMailSender {

        private SimpleMailMessage message;

        @Override
        public MimeMessage createMimeMessage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            this.message = simpleMessage;
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            this.message = simpleMessages[0];
        }
    }
}
