package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.DeviceCommandIssuedIntegrationEvent;
import com.claircore.device.application.internal.outboundservices.acl.DeviceCommandsPendingKafkaPublisher;
import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.services.DeviceControlCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DeviceControlCommandServiceImpl implements DeviceControlCommandService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceCommandsPendingKafkaPublisher deviceCommandsPendingKafkaPublisher;

    public DeviceControlCommandServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository,
            DeviceCommandRepository deviceCommandRepository,
            DeviceCommandsPendingKafkaPublisher deviceCommandsPendingKafkaPublisher
    ) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceCommandsPendingKafkaPublisher = deviceCommandsPendingKafkaPublisher;
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

        DeviceCommand deviceCommand = new DeviceCommand(assignment.getDevice(), command.type(), command.payload());
        DeviceCommand saved = deviceCommandRepository.save(deviceCommand);

        deviceCommandsPendingKafkaPublisher.publish(new DeviceCommandIssuedIntegrationEvent(
                saved.getId().toString(),
                assignment.getDevice().getId().toString(),
                assignment.getDevice().getHardwareId().value(),
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
                DeviceCommandStatus.PENDING,
                PageRequest.of(0, limit)
        );
        commands.forEach(DeviceCommand::markSent);
        return deviceCommandRepository.saveAll(commands);
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
                .findByDeviceId(deviceCommand.getDevice().getId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        switch (deviceCommand.getType()) {
            case STANDBY -> assignment.markStandby();
            case WAKE -> assignment.markOnline();
            case RESTART -> assignment.markOnline();
        }

        deviceAssignmentRepository.save(assignment);
    }
}
