package com.claircore.iam.domain.model.queries;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;

public record GetUserByEmailQuery(
    EmailAddress email
) {
    public GetUserByEmailQuery {
        if (email == null) throw new IllegalArgumentException("Email is required");
    }
}
