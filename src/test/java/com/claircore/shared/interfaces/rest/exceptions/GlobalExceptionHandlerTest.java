package com.claircore.shared.interfaces.rest.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapIllegalArgumentToBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST.value(), handler.handleIllegalArgument(new IllegalArgumentException("bad")).getStatusCode().value());
    }

    @Test
    void shouldMapIllegalStateToConflict() {
        assertEquals(HttpStatus.CONFLICT.value(), handler.handleIllegalState(new IllegalStateException("conflict")).getStatusCode().value());
    }

    @Test
    void shouldMapAccessDeniedToForbidden() {
        assertEquals(HttpStatus.FORBIDDEN.value(), handler.handleAccessDenied(new AccessDeniedException("forbidden")).getStatusCode().value());
    }
}
