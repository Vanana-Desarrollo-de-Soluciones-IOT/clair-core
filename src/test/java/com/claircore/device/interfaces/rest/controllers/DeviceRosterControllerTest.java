package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.ProvisionedDevice;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.shared.domain.model.PageResult;
import com.claircore.iam.infrastructure.config.JwtAuthenticationEntryPoint;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceRosterController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "EDGE_TO_CORE_TOKEN=test-token")
class DeviceRosterControllerTest {
    // Authentication is ServiceTokenAuthenticationFilter's, and is asserted in its own test.

    @Autowired MockMvc mockMvc;
    @MockitoBean DeviceRepository deviceRepository;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void invalidSinceReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/edge/devices").param("since", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheRosterAndPaginationFlag() throws Exception {
        UUID id = UUID.randomUUID();
        var row = new ProvisionedDevice(id, "HW-1", "secret", DeviceStatus.OFFLINE, true,
                Instant.ofEpochMilli(1000));
        when(deviceRepository.findProvisionedDevices(isNull(), isNull(), anyInt()))
                .thenReturn(new PageResult<>(List.of(row), 0, 200, 1));

        mockMvc.perform(get("/api/v1/edge/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices[0].device_id").value(id.toString()))
                .andExpect(jsonPath("$.devices[0].hardware_id").value("HW-1"))
                .andExpect(jsonPath("$.devices[0].api_key").value("secret"))
                .andExpect(jsonPath("$.devices[0].updated_at").value("1970-01-01T00:00:01Z"))
                .andExpect(jsonPath("$.devices[0].deleted").value(true))
                .andExpect(jsonPath("$.has_more").value(false));
    }
}
