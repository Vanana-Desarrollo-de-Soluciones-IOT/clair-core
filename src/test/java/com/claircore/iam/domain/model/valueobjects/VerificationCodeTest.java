package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeTest {

    @Test
    void shouldCreateVerificationCodeWhenFormatIsValid() {
        VerificationCode verificationCode = new VerificationCode("6G13-789D");

        assertEquals("6G13-789D", verificationCode.code());
    }

    @Test
    void shouldThrowExceptionWhenVerificationCodeIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new VerificationCode(" ")
        );

        assertEquals("Verification code cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenVerificationCodeHasInvalidFormat() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new VerificationCode("abcd-1234")
        );

        assertEquals("Verification code must be in format XXXX-XXXX (uppercase alphanumeric)", exception.getMessage());
    }

    @Test
    void shouldReturnTrueWhenVerificationCodeMatches() {
        VerificationCode verificationCode = new VerificationCode("6G13-789D");

        assertTrue(verificationCode.matches("6G13-789D"));
    }

    @Test
    void shouldReturnFalseWhenVerificationCodeDoesNotMatch() {
        VerificationCode verificationCode = new VerificationCode("6G13-789D");

        assertFalse(verificationCode.matches("0000-0000"));
    }
}
