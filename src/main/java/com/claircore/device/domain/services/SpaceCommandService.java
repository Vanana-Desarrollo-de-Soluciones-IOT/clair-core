package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceCommandService {
    Space handle(CreateSpaceCommand command);
    void handle(DeleteSpaceCommand command);
    void handle(UpdateSpaceNameCommand command);
    Optional<Space> findById(UUID id);
    List<Space> findByOrganizationId(UUID organizationId);
    int countByOrganizationId(UUID organizationId);
}