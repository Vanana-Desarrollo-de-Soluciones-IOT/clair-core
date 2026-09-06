package com.claircore.shared.domain.exceptions;

/**
 * Raised when a requested resource does not exist or is no longer available. Mapped to HTTP 404 by
 * the global exception handler, so contexts do not need their own handler entries.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
