package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.iam.infrastructure.config.JwtAuthenticationEntryPoint;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
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
    @Autowired MockMvc mockMvc;
    @MockitoBean DeviceRepository deviceRepository;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void rejectsMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/edge/devices"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/edge/devices").header("X-Edge-Token", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSinceReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/edge/devices").header("X-Edge-Token", "test-token").param("since", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsAuthenticatedRosterAndPaginationFlag() throws Exception {
        var projection = org.mockito.Mockito.mock(DeviceRepository.ProvisionedDeviceProjection.class);
        UUID id = UUID.randomUUID();
        when(projection.getDeviceId()).thenReturn(id);
        when(projection.getHardwareId()).thenReturn("HW-1");
        when(projection.getApiKey()).thenReturn("secret");
        when(projection.getStatus()).thenReturn(DeviceStatus.OFFLINE);
        when(projection.isDeleted()).thenReturn(true);
        when(projection.getUpdatedAt()).thenReturn(new Date(1000));
        when(deviceRepository.findProvisionedDevices(isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of(projection)));

        mockMvc.perform(get("/api/v1/edge/devices").header("X-Edge-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices[0].device_id").value(id.toString()))
                .andExpect(jsonPath("$.devices[0].hardware_id").value("HW-1"))
                .andExpect(jsonPath("$.devices[0].api_key").value("secret"))
                .andExpect(jsonPath("$.devices[0].updated_at").value("1970-01-01T00:00:01Z"))
                .andExpect(jsonPath("$.devices[0].deleted").value(true))
                .andExpect(jsonPath("$.has_more").value(false));
    }
}
