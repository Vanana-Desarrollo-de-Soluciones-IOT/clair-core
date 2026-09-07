package com.claircore.device.application.internal.commandservices;

import com.claircore.device.interfaces.events.DeviceCommandIssuedIntegrationEvent;
import com.claircore.device.application.internal.outboundservices.edge.DeviceCommandsPendingPublisher;
import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.application.commandservices.DeviceControlCommandService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DeviceControlCommandServiceImpl implements DeviceControlCommandService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCommandsPendingPublisher deviceCommandsPendingPublisher;

    public DeviceControlCommandServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository,
            DeviceCommandRepository deviceCommandRepository,
            DeviceRepository deviceRepository,
            DeviceCommandsPendingPublisher deviceCommandsPendingPublisher
    ) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceRepository = deviceRepository;
        this.deviceCommandsPendingPublisher = deviceCommandsPendingPublisher;
    }

    @Override
    @Transactional
    public DeviceCommand handle(CreateDeviceCommandCommand command) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceId(command.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        var device = deviceRepository.findById(assignment.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        DeviceCommand deviceCommand = new DeviceCommand(assignment.getDeviceId(), command.type(), command.payload());
        DeviceCommand saved = deviceCommandRepository.save(deviceCommand);

        deviceCommandsPendingPublisher.publish(new DeviceCommandIssuedIntegrationEvent(
                saved.getId().toString(),
                device.getId().toString(),
                device.getHardwareId().value(),
                saved.getType().name(),
                saved.getPayload(),
                Instant.now().toString()
        ));

        return saved;
    }

    @Override
    @Transactional
    public List<DeviceCommand> handle(DispatchPendingDeviceCommandsCommand command) {
        int limit = command.limit() == null ? 100 : Math.min(command.limit(), 500);
        List<DeviceCommand> commands = deviceCommandRepository.findByStatusForDispatch(
                DeviceCommandStatus.PENDING, limit);
        commands.forEach(DeviceCommand::markSent);
        return commands.stream().map(deviceCommandRepository::save).toList();
    }

    @Override
    @Transactional
    public DeviceCommand handle(AcknowledgeDeviceCommandCommand command) {
        DeviceCommand deviceCommand = deviceCommandRepository
                .findByDeviceIdAndCommandId(command.deviceId(), command.commandId())
                .orElseThrow(() -> new IllegalArgumentException("Device command not found"));

        if (command.status() == DeviceCommandStatus.EXECUTED) {
            deviceCommand.markExecuted();
            applyExecutedCommandToDevice(deviceCommand);
        } else {
            deviceCommand.markFailed(command.failureReason());
        }

        return deviceCommandRepository.save(deviceCommand);
    }

    private void applyExecutedCommandToDevice(DeviceCommand deviceCommand) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceIdForUpdate(deviceCommand.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        switch (deviceCommand.getType()) {
            case STANDBY -> assignment.markStandby();
            case WAKE -> assignment.markOnline();
            case RESTART -> assignment.markOnline();
        }

        deviceAssignmentRepository.save(assignment);
    }
}
