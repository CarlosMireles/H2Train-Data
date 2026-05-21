package com.h2traindata.web.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class ProviderOAuthStateStoreTest {

    private final ProviderOAuthStateStore stateStore = new ProviderOAuthStateStore();

    @Test
    void loginStateCannotBeConsumedAsProviderUserState() {
        MockHttpSession session = new MockHttpSession();
        String state = stateStore.createState(session);

        assertTrue(stateStore.consumeUserId(session, state).isEmpty());
        assertTrue(stateStore.consumeState(session, state));
    }

    @Test
    void providerUserStateCannotBeConsumedAsGenericLoginState() {
        MockHttpSession session = new MockHttpSession();
        String state = stateStore.createState(session, "internal-user-1");

        assertFalse(stateStore.consumeState(session, state));
        assertEquals(Optional.of("internal-user-1"), stateStore.consumeUserId(session, state));
    }
}
