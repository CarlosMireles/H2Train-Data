package com.h2traindata.web.auth;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProviderOAuthStateStore implements OAuthStateStore {

    private static final String STATES_ATTRIBUTE = "h2train.providerOAuthStates";
    private static final String LOGIN_STATE_MARKER = "__login__";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createState(HttpSession session) {
        String state = randomState();
        states(session).put(state, LOGIN_STATE_MARKER);
        return state;
    }

    @Override
    public String createState(HttpSession session, String userId) {
        String state = randomState();
        states(session).put(state, userId);
        return state;
    }

    @Override
    public boolean consumeState(HttpSession session, String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        Map<String, String> states = states(session);
        if (!LOGIN_STATE_MARKER.equals(states.get(state))) {
            return false;
        }
        states.remove(state);
        return true;
    }

    @Override
    public Optional<String> consumeUserId(HttpSession session, String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> states = states(session);
        String userId = states.get(state);
        if (userId == null || userId.isBlank() || LOGIN_STATE_MARKER.equals(userId)) {
            return Optional.empty();
        }
        states.remove(state);
        return Optional.of(userId);
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
