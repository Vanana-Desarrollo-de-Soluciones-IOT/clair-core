package com.claircore.device.application.commandservices;

import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand;
import com.claircore.device.domain.model.queries.ClaimPendingEdgeCommandsQuery;

import java.util.List;

/** The edge's half of the command lifecycle: take work, then report what happened to it. */
public interface EdgeCommandService {

    /**
     * Claims commands for delivery and marks them SENT. Only commands this caller actually won are
     * returned, so two edges polling at once never both receive the same command.
     */
    List<PendingEdgeCommand> handle(ClaimPendingEdgeCommandsQuery query);

    AcknowledgementOutcome handle(AcknowledgeEdgeCommandCommand command);

    /** A claimed command with the hardware id the edge needs to route it. */
    record PendingEdgeCommand(DeviceCommand command, String hardwareId) {}

    /**
     * NOT_FOUND also covers an acknowledgement from the wrong unit, so a caller cannot use the
     * response to learn whether someone else's command exists or has already completed.
     */
    enum AcknowledgementOutcome { NOT_FOUND, CONFLICT, OK }
}
