package com.h2traindata.application.port.out;

public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String encodedHash);
}
