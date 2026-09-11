package com.claircore.device.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One claimed command, in the shape the edge firmware parses. Every key is snake_case and
 * {@code issued_at} is a string, both carried over from the map this replaced.
 */
public record EdgeCommandResource(
        @JsonProperty("command_id") String commandId,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("assignment_id") String assignmentId,
        @JsonProperty("hardware_id") String hardwareId,
        @JsonProperty("command_type") String commandType,
        @JsonProperty("payload") Object payload,
        @JsonProperty("issued_at") String issuedAt
) {
}
