package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.interfaces.rest.resources.DeviceRosterResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Authentication for {@code /api/v1/edge/**} is {@code ServiceTokenAuthenticationFilter}'s job.
 * This controller used to repeat the token comparison inline, with its own fallback header and its
 * own reading of what a blank secret means.
 */
@RestController
@RequestMapping("/api/v1/edge")
public class DeviceRosterController {
    private final DeviceRepository deviceRepository;

    public DeviceRosterController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping("/devices")
    @Operation(summary = "Return the authoritative device roster for edge reconciliation")
    public ResponseEntity<DeviceRosterResponse> roster(
            @RequestParam(required = false) String since,
            @RequestParam(required = false) UUID afterId,
            @RequestParam(defaultValue = "200") int limit) {
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
        // The watermark must reflect the last row actually handed back, never wall-clock time:
        // an empty page (e.g. polled before any device exists yet) must not advance the cursor
        // past devices the caller hasn't seen, or they become permanently unreachable since the
        // cursor only ever moves forward and a device's updatedAt never revisits the past.
        String watermark = !devices.isEmpty()
                ? page.getContent().getLast().getUpdatedAt().toInstant().toString()
                : since;
        return ResponseEntity.ok(new DeviceRosterResponse(watermark, devices, page.hasNext(), nextSince, nextAfterId));
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
