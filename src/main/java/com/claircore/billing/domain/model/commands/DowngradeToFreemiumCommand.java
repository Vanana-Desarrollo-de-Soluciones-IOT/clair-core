package com.claircore.billing.domain.model.commands;

import com.claircore.billing.domain.model.valueobjects.UserId;

public record DowngradeToFreemiumCommand(UserId userId) {}
