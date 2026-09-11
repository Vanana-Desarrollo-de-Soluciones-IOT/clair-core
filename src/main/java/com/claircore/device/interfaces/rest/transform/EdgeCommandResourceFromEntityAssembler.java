package com.claircore.device.interfaces.rest.transform;

import com.claircore.device.application.commandservices.EdgeCommandService;
import com.claircore.device.interfaces.rest.resources.EdgeCommandResource;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EdgeCommandResourceFromEntityAssembler {

    private EdgeCommandResourceFromEntityAssembler() {
    }

    public static EdgeCommandResource toResourceFromEntity(
            EdgeCommandService.PendingEdgeCommand pending, ObjectMapper mapper) {
        var command = pending.command();
        return new EdgeCommandResource(
                command.getId().toString(),
                command.getDeviceId().toString(),
                command.getAssignmentId() == null ? null : command.getAssignmentId().toString(),
                pending.hardwareId(),
                command.getType().name(),
                parsePayload(command.getPayload(), mapper),
                command.getCreatedAt() == null ? "" : command.getCreatedAt().toString());
    }

    /**
     * A payload that parses as JSON is sent as JSON, and anything else is sent as the string it
     * was stored as. That is what the map did, and the edge relies on both shapes.
     */
    private static Object parsePayload(String payload, ObjectMapper mapper) {
        try {
            return mapper.readTree(payload);
        } catch (Exception notJson) {
            return payload;
        }
    }
}
