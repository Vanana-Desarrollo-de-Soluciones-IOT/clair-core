package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EdgeCommandAcknowledgementService {
    public enum Outcome { NOT_FOUND, CONFLICT, OK }

    private final DeviceCommandRepository repository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    @Autowired
    public EdgeCommandAcknowledgementService(DeviceCommandRepository repository,
                                             DeviceAssignmentRepository deviceAssignmentRepository) {
        this.repository = repository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    /**
     * Claims commands received from the edge; the edge delivers ACKs asynchronously
     * from its outbox after local persistence.
     *
     * Selection is row-locked and the lease claim is flushed in this transaction.
     * A five-minute lease permits redelivery of a lost delivery; ordering by the
     * oldest delivery/creation time prevents old in-flight commands starving.
     */
    private static final long LEASE_SECONDS = 300;

    @Transactional
    public List<DeviceCommand> claimForEdge(String hardwareId, Instant since, int limit) {
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(LEASE_SECONDS);
        return hardwareId == null
                ? claim(repository.findPendingForEdge(since, cutoff, org.springframework.data.domain.PageRequest.of(0, limit)), now)
                : claim(repository.findPendingForEdgeByHardware(hardwareId, since, cutoff, org.springframework.data.domain.PageRequest.of(0, limit)), now);
    }

    private List<DeviceCommand> claim(List<DeviceCommand> candidates, Instant now) {
        Instant cutoff = now.minusSeconds(LEASE_SECONDS);
        return candidates.stream()
                .filter(command -> repository.claimForEdge(command.getId(), cutoff, now) == 1)
                .peek(command -> {
                    if (command.getStatus() == DeviceCommandStatus.PENDING) command.markSent();
                    else command.redeliver();
                })
                .toList();
    }

    /** Test helper for domain-only callers; production uses the query overload above. */
    @Transactional
    public List<DeviceCommand> claimForEdge(List<DeviceCommand> candidates) {
        return claim(candidates, Instant.now());
    }

    @Transactional
    public Outcome acknowledge(UUID commandId, EdgeCommandAckRequest body) {
        // The row lock and transition happen in one transaction. A concurrent ACK
        // therefore observes the first terminal state and cannot overwrite it.
        return repository.findByIdForAcknowledgement(commandId)
                .map(command -> acknowledgeLoaded(command, body))
                .orElse(Outcome.NOT_FOUND);
    }

    private Outcome acknowledgeLoaded(DeviceCommand command, EdgeCommandAckRequest body) {
        // Validate hardware ownership before inspecting terminal state, so an ACK
        // from another device cannot learn whether this command has completed.
        if (!command.getDevice().getHardwareId().value().equals(body.hardware_id())) {
            return Outcome.NOT_FOUND;
        }
        DeviceCommandStatus status = command.getStatus();
        if (status == DeviceCommandStatus.EXECUTED || status == DeviceCommandStatus.FAILED) {
            return Outcome.CONFLICT;
        }
        // Pending commands are claimed by the pending endpoint and become SENT
        // before Edge can deliver them. ACKs therefore only accept SENT commands.
        if (status != DeviceCommandStatus.SENT) {
            return Outcome.NOT_FOUND;
        }
        if (body.result() == EdgeCommandAckRequest.Result.FAILED) {
            command.markFailed(body.detail());
        } else {
            command.markExecuted();
            // Execution changes the assignment state as part of the same ACK.
            deviceAssignmentRepository.findByDeviceId(command.getDevice().getId())
                    .ifPresent(assignment -> {
                        switch (command.getType()) {
                            case STANDBY -> assignment.markStandby();
                            case WAKE, RESTART -> assignment.markOnline();
                        }
                        deviceAssignmentRepository.save(assignment);
                    });
        }
        repository.save(command);
        return Outcome.OK;
    }
}
