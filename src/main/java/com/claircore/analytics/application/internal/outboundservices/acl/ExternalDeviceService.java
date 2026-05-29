package com.claircore.analytics.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.device.interfaces.acl.OrganizationSummaryDto;
import com.claircore.device.interfaces.acl.SpaceSummaryDto;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumer-side ACL for resolving Device identifiers from the Device bounded context.
 */
@Service("analyticsExternalDeviceService")
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    public List<OrganizationSummaryDto> findOrganizationsByOwnerId(UUID ownerUserId) {
        if (ownerUserId == null) return List.of();
        return deviceContextFacade.findOrganizationsByOwnerId(ownerUserId);
    }

    public List<SpaceSummaryDto> findSpacesByOrganizationId(UUID organizationId) {
        if (organizationId == null) return List.of();
        return deviceContextFacade.findSpacesByOrganizationId(organizationId);
    }

    public List<UUID> findDeviceIdsBySpaceId(UUID spaceId, int limit) {
        if (spaceId == null) return List.of();
        return deviceContextFacade.findDeviceIdsBySpaceId(spaceId, limit);
    }

    public Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return Collections.emptyMap();
        return deviceContextFacade.findDeviceNamesByDeviceIds(deviceIds);
    }

    public Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) return Collections.emptyMap();
        return deviceContextFacade.findSpaceNamesBySpaceIds(spaceIds);
    }
}
