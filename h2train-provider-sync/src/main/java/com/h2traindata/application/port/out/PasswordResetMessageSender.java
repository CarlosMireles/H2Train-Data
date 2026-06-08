package com.h2traindata.application.port.out;

import java.net.URI;
import java.time.Instant;

public interface PasswordResetMessageSender {

    void sendPasswordReset(String email, String username, URI resetUri, Instant expiresAt);
}
