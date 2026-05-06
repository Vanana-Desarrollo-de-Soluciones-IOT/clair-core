package com.claircore.iam.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;

@Embeddable
public record EmailAddress(
    @Email
    String address
) {
    @JsonCreator
    public EmailAddress(@JsonProperty("address") String address) {
        this.address = address;
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
    }
}
