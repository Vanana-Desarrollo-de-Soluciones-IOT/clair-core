package com.claircore.iam.infrastructure.oauth.google;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthStateManagerTest {

    private final GoogleOAuthStateManager stateManager = new GoogleOAuthStateManager("01234567890123456789012345678901");

    @Test
    void shouldValidateStateWhenTokenWasGeneratedBySameManager() {
        String state = stateManager.generateState();

        assertTrue(stateManager.validateState(state));
    }

    @Test
    void shouldRejectStateWhenTokenIsTampered() {
        String state = stateManager.generateState();
        String tampered = state + "x";

        assertFalse(stateManager.validateState(tampered));
    }
}
