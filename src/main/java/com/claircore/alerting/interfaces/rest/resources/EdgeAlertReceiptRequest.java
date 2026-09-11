package com.claircore.alerting.interfaces.rest.resources;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Delivery receipt: the edge stored this transition. Distinct from the business ACK. */
public record EdgeAlertReceiptRequest(@NotBlank String hardware_id, @Min(0) long sequence) {}
