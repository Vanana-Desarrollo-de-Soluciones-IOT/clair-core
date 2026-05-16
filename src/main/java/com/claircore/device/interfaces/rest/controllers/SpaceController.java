package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.SpaceCommandService;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.domain.model.queries.GetSpaceByIdQuery;
import com.claircore.device.domain.model.queries.GetSpacesByOwnerQuery;
import com.claircore.device.interfaces.rest.resources.CreateSpaceRequest;
import com.claircore.device.interfaces.rest.resources.SpaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spaces")
@Tag(name = "Spaces", description = "Space management endpoints")
public class SpaceController {

    private final SpaceCommandService spaceCommandService;
    private final DeviceQueryService deviceQueryService;

    public SpaceController(SpaceCommandService spaceCommandService, DeviceQueryService deviceQueryService) {
        this.spaceCommandService = spaceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping
    @Operation(summary = "Create a new space")
    public ResponseEntity<SpaceResponse> createSpace(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody CreateSpaceRequest request) {

        var command = new CreateSpaceCommand(
            request.name(),
            new UserId(userId)
        );

        Space space = spaceCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(space));
    }

    @GetMapping("/{spaceId}")
    @Operation(summary = "Get space by ID")
    public ResponseEntity<SpaceResponse> getSpace(@PathVariable UUID spaceId) {
        var query = new GetSpaceByIdQuery(spaceId);
        return deviceQueryService.handle(query)
            .map(space -> ResponseEntity.ok(toResponse(space)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get spaces by owner")
    public ResponseEntity<List<SpaceResponse>> getSpacesByOwner(@RequestHeader("X-User-Id") UUID userId) {
        var query = new GetSpacesByOwnerQuery(new UserId(userId));
        List<Space> spaces = deviceQueryService.handle(query);
        return ResponseEntity.ok(spaces.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{spaceId}")
    @Operation(summary = "Delete a space")
    public ResponseEntity<Void> deleteSpace(@PathVariable UUID spaceId) {
        spaceCommandService.handle(new DeleteSpaceCommand(spaceId));
        return ResponseEntity.noContent().build();
    }

    private SpaceResponse toResponse(Space space) {
        return new SpaceResponse(
            space.getId(),
            space.getName(),
            space.getOwnerUserId().userId(),
            space.getAuditFields().getCreatedAt().toInstant(),
            space.getAuditFields().getUpdatedAt().toInstant()
        );
    }
}