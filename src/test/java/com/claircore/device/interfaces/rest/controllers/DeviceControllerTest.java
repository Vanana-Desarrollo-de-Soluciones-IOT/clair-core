package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceQuery;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.domain.services.DeviceStatusQueryService;
import com.claircore.device.interfaces.rest.resources.*;
import com.claircore.iam.domain.services.TokenQueryService;
import com.claircore.shared.interfaces.rest.exceptions.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceCommandService deviceCommandService;

    @MockitoBean
    private DeviceQueryService deviceQueryService;

    @MockitoBean
    private DeviceStatusQueryService deviceStatusQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @BeforeEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCreatedWhenPairingDeviceSucceeds() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655442000");
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655442001");
        Device device = device();
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", deviceId);
        DeviceAssignment assignment = new DeviceAssignment(device, new ClaimToken("AB45-F3B1"));
        when(deviceCommandService.handle(org.mockito.ArgumentMatchers.any(PairDeviceCommand.class))).thenReturn(assignment);

        mockMvc.perform(post("/api/v1/devices/pair")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new PairDeviceRequest("CLAIR-0KBG"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
                .andExpect(jsonPath("$.claimToken").value("AB45-F3B1"));

        verify(deviceCommandService).handle(org.mockito.ArgumentMatchers.any(PairDeviceCommand.class));
    }

    @Test
    void shouldReturnBadRequestWhenPairRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/devices/pair")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("hardwareId", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOkWhenClaimingDeviceSucceeds() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655442000");
        Device device = device();
        DeviceAssignment assignment = new DeviceAssignment(device, ClaimToken.generate());
        assignment.claimToSpace(UUID.fromString("550e8400-e29b-41d4-a716-446655442010"), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655442000")));
        ClaimToken claimToken = new ClaimToken("AB45-F3B1");
        org.springframework.test.util.ReflectionTestUtils.setField(assignment, "claimToken", claimToken);
        org.springframework.test.util.ReflectionTestUtils.setField(assignment.getAuditFields(), "createdAt", new Date());
        org.springframework.test.util.ReflectionTestUtils.setField(assignment.getAuditFields(), "updatedAt", new Date());
        when(deviceCommandService.handle(org.mockito.ArgumentMatchers.any(ClaimDeviceCommand.class))).thenReturn(assignment);

        mockMvc.perform(post("/api/v1/devices/claim")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ClaimDeviceRequest(claimToken.value(), UUID.fromString("550e8400-e29b-41d4-a716-446655442010")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUserId").value("550e8400-e29b-41d4-a716-446655442000"));
    }

    @Test
    void shouldReturnForbiddenWhenStatusIsRequestedWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/devices/{deviceId}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnDeviceListWhenSpaceHasAssignments() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655442000");
        DeviceAssignment assignment = new DeviceAssignment(device(), ClaimToken.generate());
        Page<DeviceAssignment> page = new PageImpl<>(List.of(assignment));
        when(deviceQueryService.handle(org.mockito.ArgumentMatchers.any(GetDevicesBySpaceQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/devices").param("spaceId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serialNumber").value("SN-1000"));
    }

    private Device device() {
        return new Device("SN-1000", "Sensor 1000", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new User(userId, "N/A", Collections.emptyList()), null, Collections.emptyList())
        );
    }
}
