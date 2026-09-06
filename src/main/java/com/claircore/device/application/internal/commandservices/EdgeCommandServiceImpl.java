package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.commandservices.EdgeCommandService;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand;
import com.claircore.device.domain.model.queries.ClaimPendingEdgeCommandsQuery;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.EdgeCommandResult;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Claims commands for the edge and applies the acknowledgements it sends back.
 *
 * <p>Selection is row-locked and the lease claim is flushed in this transaction. A five-minute lease
 * permits redelivery of a lost delivery; ordering by the oldest delivery or creation time prevents
 * old in-flight commands starving.
 */
@Service
public class EdgeCommandServiceImpl implements EdgeCommandService {

    private static final long LEASE_SECONDS = 300;

    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final DeviceRepository deviceRepository;

    public EdgeCommandServiceImpl(
            DeviceCommandRepository deviceCommandRepository,
            DeviceAssignmentRepository deviceAssignmentRepository,
            DeviceRepository deviceRepository) {
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public List<PendingEdgeCommand> handle(ClaimPendingEdgeCommandsQuery query) {
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(LEASE_SECONDS);

        List<DeviceCommand> candidates = query.hardwareId() == null
                ? deviceCommandRepository.findPendingForEdge(query.since(), cutoff, query.limit())
                : deviceCommandRepository.findPendingForEdgeByHardware(
                        query.hardwareId(), query.since(), cutoff, query.limit());

        List<DeviceCommand> claimed = claim(candidates, now, cutoff);
        if (claimed.isEmpty()) {
            return List.of();
        }

        // One lookup for the whole page rather than one per command, which is what walking a lazy
        // association used to cost.
        Map<UUID, String> hardwareIds = deviceRepository
                .findAllById(claimed.stream().map(DeviceCommand::getDeviceId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Device::getId, device -> device.getHardwareId().value()));

        return claimed.stream()
                .filter(command -> hardwareIds.containsKey(command.getDeviceId()))
                .map(command -> new PendingEdgeCommand(command, hardwareIds.get(command.getDeviceId())))
                .toList();
    }

    private List<DeviceCommand> claim(List<DeviceCommand> candidates, Instant now, Instant cutoff) {
        return candidates.stream()
                .filter(command -> deviceCommandRepository.claimForEdge(command.getId(), cutoff, now) == 1)
                .peek(command -> {
                    if (command.getStatus() == DeviceCommandStatus.PENDING) command.markSent();
                    else command.redeliver();
                })
                .toList();
    }

    @Override
    @Transactional
    public AcknowledgementOutcome handle(AcknowledgeEdgeCommandCommand command) {
        // The row lock and the transition happen in one transaction, so a concurrent ACK observes
        // the first terminal state and cannot overwrite it.
        return deviceCommandRepository.findByIdForAcknowledgement(command.commandId())
                .map(found -> acknowledge(found, command))
                .orElse(AcknowledgementOutcome.NOT_FOUND);
    }

    private AcknowledgementOutcome acknowledge(DeviceCommand found, AcknowledgeEdgeCommandCommand command) {
        // Ownership is checked before any terminal state is inspected, so an ACK from another unit
        // cannot learn whether this command has completed.
        boolean ownsCommand = deviceRepository.findById(found.getDeviceId())
                .map(device -> device.getHardwareId().value().equals(command.hardwareId()))
                .orElse(false);
        if (!ownsCommand) {
            return AcknowledgementOutcome.NOT_FOUND;
        }

        DeviceCommandStatus status = found.getStatus();
        if (status == DeviceCommandStatus.EXECUTED || status == DeviceCommandStatus.FAILED) {
            return AcknowledgementOutcome.CONFLICT;
        }
        // Pending commands are claimed by the pending endpoint and become SENT before the edge can
        // deliver them, so an ACK only ever applies to a SENT command.
        if (status != DeviceCommandStatus.SENT) {
            return AcknowledgementOutcome.NOT_FOUND;
        }

        if (command.result() == EdgeCommandResult.FAILED) {
            found.markFailed(command.detail());
        } else {
            found.markExecuted();
            applyToAssignment(found);
        }
        deviceCommandRepository.save(found);
        return AcknowledgementOutcome.OK;
    }

    /** Executing a command changes what the device is doing, as part of the same acknowledgement. */
    private void applyToAssignment(DeviceCommand command) {
        deviceAssignmentRepository.findByDeviceIdForUpdate(command.getDeviceId())
                .ifPresent(assignment -> {
                    switch (command.getType()) {
                        case STANDBY -> assignment.markStandby();
                        case WAKE, RESTART -> assignment.markOnline();
                    }
                    deviceAssignmentRepository.save(assignment);
                });
    }
}
