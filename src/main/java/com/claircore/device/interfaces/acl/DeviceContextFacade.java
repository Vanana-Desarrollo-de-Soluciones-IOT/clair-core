package com.claircore.device.interfaces.acl;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DeviceContextFacade {

    Optional<UUID> findDeviceIdByApiKey(String apiKey);

    Optional<UUID> findDeviceIdByHardwareId(String hardwareId);

    Optional<UUID> findSpaceIdByDeviceId(UUID deviceId);

    Optional<String> findHardwareIdByDeviceId(UUID deviceId);

    boolean isDeviceOwnedByUser(UUID deviceId, UUID userId);

    Optional<UUID> findOwnerIdByDeviceId(UUID deviceId);

    boolean isSpaceOwnedByUser(UUID spaceId, UUID userId);

    List<UUID> findDeviceIdsByOwnerId(UUID ownerUserId);

    Optional<String> findSpaceNameBySpaceId(UUID spaceId);

    Optional<String> findDeviceNameByDeviceId(UUID deviceId);

    /**
     * Batch lookup to avoid N+1 queries in consumer contexts.
     */
    Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds);

    /**
     * Batch lookup to avoid N+1 queries in consumer contexts.
     */
    Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds);

    /**
     * Ownership-scoped organization summaries for read models.
     */
    List<OrganizationSummary> findOrganizationsByOwnerId(UUID ownerUserId);

    /**
     * Spaces within an organization for read models.
     */
    List<SpaceSummary> findSpacesByOrganizationId(UUID organizationId);

    /**
     * Device IDs assigned to a space (bounded by limit) for read models.
     */
    List<UUID> findDeviceIdsBySpaceId(UUID spaceId, int limit);
}
