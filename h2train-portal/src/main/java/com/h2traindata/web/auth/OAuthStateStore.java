package com.h2traindata.web.auth;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

public interface OAuthStateStore {

    String createState(HttpSession session);

    String createState(HttpSession session, String userId);

    boolean consumeState(HttpSession session, String state);

    Optional<String> consumeUserId(HttpSession session, String state);
}
