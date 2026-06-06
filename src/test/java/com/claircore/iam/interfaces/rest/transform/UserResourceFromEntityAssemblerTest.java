package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class UserResourceFromEntityAssemblerTest {

    @Test
    void shouldMapUserEntityToResourceWhenUserHasIdentifier() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));
        setField(user, "id", UUID.randomUUID());

        var resource = UserResourceFromEntityAssembler.toResourceFromEntity(user);

        assertEquals(user.getId(), resource.id());
        assertEquals("user@example.com", resource.email());
    }
}
