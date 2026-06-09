package com.claircore.evaluation.interfaces.rest.controllers;

import com.claircore.evaluation.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.interfaces.rest.resources.EvaluateTelemetryRequest;
import com.claircore.iam.domain.services.TokenQueryService;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import com.claircore.shared.interfaces.rest.exceptions.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryEvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TelemetryEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TelemetryEvaluationQueryService telemetryEvaluationQueryService;

    @MockitoBean
    private TelemetryEvaluationCommandService telemetryEvaluationCommandService;

    @MockitoBean
    private ExternalDeviceService externalDeviceService;

    @MockitoBean
    private TokenQueryService tokenQueryService; // Required to satisfy context dependencies

    @Test
    void shouldReturnCreatedWhenEvaluatingValidTelemetryWithUuidDevice() throws Exception {
        // Arrange
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(resolvedDeviceId)).thenReturn(Optional.of("HW-001"));

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(resolvedDeviceId), LocalTime.NOON, 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        when(telemetryEvaluationCommandService.handle(any(EvaluateTelemetryCommand.class))).thenReturn(evaluation);

        var requestBody = new EvaluateTelemetryRequest(
                resolvedDeviceId.toString(),
                "12:00:00",
                "3600",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                Instant.now().toString()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(resolvedDeviceId.toString()));

        // Verification: Since a UUID was passed, findDeviceIdByHardwareId should NOT be called
        verify(externalDeviceService, never()).findDeviceIdByHardwareId(anyString());
        verify(externalDeviceService).findHardwareIdByDeviceId(resolvedDeviceId);
    }

    @Test
    void shouldReturnCreatedWhenEvaluatingValidTelemetryWithHardwareIdDevice() throws Exception {
        // Arrange
        String hardwareId = "CLAIR-001";
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findDeviceIdByHardwareId(hardwareId)).thenReturn(Optional.of(resolvedDeviceId));

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(resolvedDeviceId), LocalTime.NOON, 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        when(telemetryEvaluationCommandService.handle(any(EvaluateTelemetryCommand.class))).thenReturn(evaluation);

        var requestBody = new EvaluateTelemetryRequest(
                hardwareId,
                "12:00:00",
                "3600",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                Instant.now().toString()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(resolvedDeviceId.toString()));

        // Verification: Verify findDeviceIdByHardwareId is explicitly called
        verify(externalDeviceService, times(1)).findDeviceIdByHardwareId(hardwareId);
    }

    @Test
    void shouldReturnBadRequestWhenTimestampIsInvalid() throws Exception {
        // Arrange
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(resolvedDeviceId)).thenReturn(Optional.of("HW-002"));

        var requestBody = new EvaluateTelemetryRequest(
                resolvedDeviceId.toString(),
                "invalid-time-format",
                "3600",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUptimeIsInvalid() throws Exception {
        // Arrange
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(resolvedDeviceId)).thenReturn(Optional.of("HW-003"));

        var requestBody = new EvaluateTelemetryRequest(
                resolvedDeviceId.toString(),
                "12:00:00",
                "invalid-uptime-format",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreatedAtIsInvalid() throws Exception {
        // Arrange
        String hardwareId = "CLAIR-002";
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findDeviceIdByHardwareId(hardwareId)).thenReturn(Optional.of(resolvedDeviceId));

        var requestBody = new EvaluateTelemetryRequest(
                hardwareId,
                "12:00:00",
                "3600",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                "invalid-instant"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenEvaluatingTelemetryForUnknownDevice() throws Exception {
        // Arrange
        String unknownDevice = "unknown-device-id";
        when(externalDeviceService.findDeviceIdByHardwareId(unknownDevice)).thenReturn(Optional.empty());

        var requestBody = new EvaluateTelemetryRequest(
                unknownDevice,
                "12:00:00",
                "3600",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenEvaluatingTelemetryForUnknownUuidDevice() throws Exception {
        // Arrange
        UUID unknownDevice = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(unknownDevice)).thenReturn(Optional.empty());

        var requestBody = new EvaluateTelemetryRequest(
                unknownDevice.toString(),
                "12:00:00",
                "3600",
                new EvaluateTelemetryRequest.AirQualityRequest(400.0, 22.0, 45.0),
                new EvaluateTelemetryRequest.ParticulateMatterRequest(10, 15, 25),
                new EvaluateTelemetryRequest.ConnectivityRequest("ONLINE", "WiFi", -50),
                new EvaluateTelemetryRequest.LocationRequest("Chile"),
                85,
                "STABLE",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIdIsMissingFromRequestAttributes() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenUserDoesNotOwnDevice() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId)
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnEvaluationsWhenUserOwnsDevice() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId), LocalTime.NOON, 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        var page = new PageImpl<>(List.of(evaluation));
        when(telemetryEvaluationQueryService.handle(any(GetEvaluationsByDeviceQuery.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId)
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deviceId").value(deviceId.toString()));
    }

    @Test
    void shouldReturnLatestEvaluationWhenUserOwnsDevice() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId), LocalTime.NOON, 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        when(telemetryEvaluationQueryService.handle(any(GetLatestEvaluationByDeviceQuery.class)))
                .thenReturn(Optional.of(evaluation));

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}/latest", deviceId)
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()));
    }

    @Test
    void shouldReturnNotFoundWhenNoLatestEvaluationExists() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);
        when(telemetryEvaluationQueryService.handle(any(GetLatestEvaluationByDeviceQuery.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}/latest", deviceId)
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isNotFound());
    }
}
