package com.claircore.iam.interfaces.rest.resources;

public record UserResource(
    Long id,
    String username,
    String email
) {}
