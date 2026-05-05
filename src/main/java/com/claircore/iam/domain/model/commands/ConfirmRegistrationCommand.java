package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;

public record ConfirmRegistrationCommand(
    RegistrationSessionId sessionId,
    String verificationCode
) {
    public ConfirmRegistrationCommand {
        if (sessionId == null) throw new IllegalArgumentException("Session ID is required");
        if (verificationCode == null || verificationCode.isBlank()) throw new IllegalArgumentException("Verification code is required");
    }
}
