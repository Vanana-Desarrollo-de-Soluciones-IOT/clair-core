package com.claircore.device.application.internal.eventhandlers;

import com.claircore.device.domain.model.commands.SeedDevicesCommand;
import com.claircore.device.application.commandservices.DeviceCommandService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DeviceSeedOnStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final DeviceCommandService deviceCommandService;

    public DeviceSeedOnStartup(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        deviceCommandService.handle(new SeedDevicesCommand(5));
    }
}
