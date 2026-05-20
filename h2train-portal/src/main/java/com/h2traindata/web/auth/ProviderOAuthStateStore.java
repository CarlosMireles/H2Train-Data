package com.h2traindata.web.auth;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProviderOAuthStateStore {

    private static final String STATES_ATTRIBUTE = "h2train.providerOAuthStates";

    private final SecureRandom secureRandom = new SecureRandom();

    public String createState(HttpSession session, String userId) {
        String state = randomState();
        states(session).put(state, userId);
        return state;
    }

    public Optional<String> consumeUserId(HttpSession session, String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(states(session).remove(state));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> states(HttpSession session) {
        Object existingStates = session.getAttribute(STATES_ATTRIBUTE);
        if (existingStates instanceof Map<?, ?>) {
            return (Map<String, String>) existingStates;
        }

        Map<String, String> states = new LinkedHashMap<>();
        session.setAttribute(STATES_ATTRIBUTE, states);
        return states;
    }

    private String randomState() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
