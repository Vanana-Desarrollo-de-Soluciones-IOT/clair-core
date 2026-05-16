package com.claircore.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateDeviceConfigurationRequest(@NotNull Map<String, String> configuration) {}