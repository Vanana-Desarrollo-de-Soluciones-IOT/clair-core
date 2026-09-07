package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.queries.GetDeviceRosterQuery;
import com.claircore.device.interfaces.rest.resources.DeviceRosterResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication for {@code /api/v1/edge/**} is {@code ServiceTokenAuthenticationFilter}'s job.
 * This controller used to repeat the token comparison inline, with its own fallback header and its
 * own reading of what a blank secret means.
 */
@RestController
@RequestMapping("/api/v1/edge")
public class DeviceRosterController {
    private final DeviceQueryService deviceQueryService;

    public DeviceRosterController(DeviceQueryService deviceQueryService) {
        this.deviceQueryService = deviceQueryService;
    }

    @GetMapping("/devices")
    @Operation(summary = "Return the authoritative device roster for edge reconciliation")
    public ResponseEntity<DeviceRosterResponse> roster(
            @RequestParam(required = false) String since,
            @RequestParam(required = false) UUID afterId,
            @RequestParam(defaultValue = "200") int limit) {
        if (limit < 1 || limit > 200) return ResponseEntity.badRequest().build();
        var page = deviceQueryService.handle(new GetDeviceRosterQuery(parseSince(since), afterId, limit));
        var rows = page.items();
        var devices = rows.stream().map(d -> new DeviceRosterResponse.DeviceRosterItem(
                d.deviceId().toString(), d.hardwareId(), d.apiKey(),
                d.status().name(), d.deleted(), d.updatedAt().toString())).toList();

        boolean hasMore = page.total() > rows.size();
        String nextSince = hasMore && !rows.isEmpty() ? rows.getLast().updatedAt().toString() : null;
        String nextAfterId = hasMore && !rows.isEmpty() ? rows.getLast().deviceId().toString() : null;
        // The watermark must reflect the last row actually handed back, never wall-clock time:
        // an empty page (e.g. polled before any device exists yet) must not advance the cursor
        // past devices the caller hasn't seen, or they become permanently unreachable since the
        // cursor only ever moves forward and a device's updatedAt never revisits the past.
        String watermark = !rows.isEmpty() ? rows.getLast().updatedAt().toString() : since;
        return ResponseEntity.ok(new DeviceRosterResponse(watermark, devices, hasMore, nextSince, nextAfterId));
    }

    private Instant parseSince(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.matches("\\d+")) return Instant.ofEpochMilli(Long.parseLong(value));
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("since must be epoch milliseconds or ISO-8601", ex);
        }
    }
}
