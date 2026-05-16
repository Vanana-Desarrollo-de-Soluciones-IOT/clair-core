package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.SpaceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SpaceCommandServiceImpl implements SpaceCommandService {

    private static final int MAX_SPACES_PER_ORG = 5;

    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;

    public SpaceCommandServiceImpl(SpaceRepository spaceRepository, OrganizationRepository organizationRepository) {
        this.spaceRepository = spaceRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public Space handle(CreateSpaceCommand command) {
        Organization org = organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (spaceRepository.countByOrganizationId(command.organizationId()) >= MAX_SPACES_PER_ORG) {
            throw new IllegalStateException(
                "Organization has reached maximum spaces limit of " + MAX_SPACES_PER_ORG
            );
        }

        Space space = new Space(
            command.name(),
            command.organizationId(),
            command.ownerUserId()
        );

        return spaceRepository.save(space);
    }

    @Override
    @Transactional
    public void handle(DeleteSpaceCommand command) {
        Space space = spaceRepository
            .findById(command.spaceId())
            .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        spaceRepository.delete(space);
    }

    @Override
    public Optional<Space> findById(UUID id) {
        return spaceRepository.findById(id);
    }

    @Override
    public List<Space> findByOrganizationId(UUID organizationId) {
        return spaceRepository.findByOrganizationId(organizationId);
    }

    @Override
    public int countByOrganizationId(UUID organizationId) {
        return spaceRepository.countByOrganizationId(organizationId);
    }
}