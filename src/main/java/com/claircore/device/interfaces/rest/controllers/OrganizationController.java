package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.commands.UpdateOrganizationNameCommand;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.OrganizationCommandService;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.domain.model.queries.GetOrganizationByIdQuery;
import com.claircore.device.domain.model.queries.GetOrganizationsByOwnerQuery;
import com.claircore.device.interfaces.rest.resources.CreateOrganizationRequest;
import com.claircore.device.interfaces.rest.resources.OrganizationResponse;
import com.claircore.device.interfaces.rest.resources.UpdateOrganizationNameRequest;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {

    private final OrganizationCommandService organizationCommandService;
    private final DeviceQueryService deviceQueryService;

    public OrganizationController(
            OrganizationCommandService organizationCommandService,
            DeviceQueryService deviceQueryService) {
        this.organizationCommandService = organizationCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<OrganizationResponse> createOrganization(
            HttpServletRequest request,
            @RequestBody CreateOrganizationRequest req) {

        UUID userId = (UUID) request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        var command = new CreateOrganizationCommand(
            req.name(),
            new UserId(userId)
        );

        Organization org = organizationCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(org));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID organizationId) {
        var query = new GetOrganizationByIdQuery(organizationId);
        return deviceQueryService.handle(query)
            .map(org -> ResponseEntity.ok(toResponse(org)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get organizations for current user")
    public ResponseEntity<List<OrganizationResponse>> getUserOrganizations(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        var query = new GetOrganizationsByOwnerQuery(new UserId(userId));
        List<Organization> orgs = deviceQueryService.handle(query);
        return ResponseEntity.ok(orgs.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{organizationId}")
    @Operation(summary = "Delete organization")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID organizationId) {
        organizationCommandService.handle(new DeleteOrganizationCommand(organizationId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{organizationId}/name")
    @Operation(summary = "Update organization name")
    public ResponseEntity<Void> updateOrganizationName(
            @PathVariable UUID organizationId,
            @RequestBody UpdateOrganizationNameRequest request) {

        organizationCommandService.handle(new UpdateOrganizationNameCommand(organizationId, request.name()));
        return ResponseEntity.ok().build();
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
            org.getId(),
            org.getName(),
            org.getOwnerUserId().userId(),
            org.getAuditFields().getCreatedAt().toInstant(),
            org.getAuditFields().getUpdatedAt().toInstant()
        );
    }
}