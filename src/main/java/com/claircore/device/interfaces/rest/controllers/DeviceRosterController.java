package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.interfaces.rest.resources.DeviceRosterResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/edge")
public class DeviceRosterController {
    private final DeviceRepository deviceRepository;
    private final String edgeToken;

    public DeviceRosterController(DeviceRepository deviceRepository, @Value("${EDGE_TO_CORE_TOKEN:${edge.token:}}") String edgeToken) {
        this.deviceRepository = deviceRepository;
        this.edgeToken = edgeToken;
    }

    @GetMapping("/devices")
    @Operation(summary = "Return the authoritative device roster for edge reconciliation")
    public ResponseEntity<DeviceRosterResponse> roster(
            @RequestHeader(value = "X-Edge-Token", required = false) String legacyToken,
            @RequestHeader(value = "X-Core-Token", required = false) String coreToken,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) UUID afterId,
            @RequestParam(defaultValue = "200") int limit) {
        String suppliedToken = coreToken != null ? coreToken : legacyToken;
        if (edgeToken.isBlank() || suppliedToken == null ||
                !MessageDigest.isEqual(edgeToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).build();
        }
        if (limit < 1 || limit > 200) return ResponseEntity.badRequest().build();
        Date parsedSince = parseSince(since);
        var page = deviceRepository.findProvisionedDevices(parsedSince, afterId, PageRequest.of(0, limit));
        var devices = page.getContent().stream().map(d -> new DeviceRosterResponse.DeviceRosterItem(
                d.getDeviceId().toString(), d.getHardwareId(), d.getApiKey(),
                d.getStatus().name(), d.isDeleted(), d.getUpdatedAt().toInstant().toString())).toList();
        String nextSince = page.hasNext() && !page.isEmpty()
                ? page.getContent().getLast().getUpdatedAt().toInstant().toString() : null;
        String nextAfterId = page.hasNext() && !page.isEmpty()
                ? page.getContent().getLast().getDeviceId().toString() : null;
        return ResponseEntity.ok(new DeviceRosterResponse(Instant.now().toString(), devices, page.hasNext(), nextSince, nextAfterId));
    }

    private Date parseSince(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.matches("\\d+")) return new Date(Long.parseLong(value));
            return Date.from(Instant.parse(value));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("since must be epoch milliseconds or ISO-8601", ex);
        }
    }
}
