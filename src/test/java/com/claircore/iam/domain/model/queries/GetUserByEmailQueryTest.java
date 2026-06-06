package com.claircore.iam.domain.model.queries;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetUserByEmailQueryTest {

    @Test
    void shouldCreateGetUserByEmailQueryWhenEmailIsValid() {
        EmailAddress email = new EmailAddress("user@example.com");

        GetUserByEmailQuery query = new GetUserByEmailQuery(email);

        assertEquals(email, query.email());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetUserByEmailQuery(null)
        );

        assertEquals("Email is required", exception.getMessage());
    }
}
