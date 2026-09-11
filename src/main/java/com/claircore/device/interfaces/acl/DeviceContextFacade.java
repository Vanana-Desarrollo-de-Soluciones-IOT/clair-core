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
    /**
     * The instant from which readings of this device belong to its current owner. Consumers must
     * not show anything recorded earlier to that owner: it was measured for someone else.
     */
    Optional<java.time.Instant> findVisibleSinceByDeviceId(UUID deviceId);

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
     * Batch lookup, added so alerting can attach hardware ids to a page of alerts without joining
     * the {@code devices} table from its own query.
     */
    Map<UUID, String> findHardwareIdsByDeviceIds(List<UUID> deviceIds);

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
