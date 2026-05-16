package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.valueobjects.PlanType;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.OrganizationCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationCommandServiceImpl implements OrganizationCommandService {

    private static final int STANDART_MAX_ORGANIZATIONS = 1;
    private static final int MESH_MAX_ORGANIZATIONS = 3;

    private final OrganizationRepository organizationRepository;

    public OrganizationCommandServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public Organization handle(CreateOrganizationCommand command) {
        int currentCount = organizationRepository.countByOwnerUserId(command.ownerUserId());
        int maxAllowed = command.planType() == PlanType.MESH ? MESH_MAX_ORGANIZATIONS : STANDART_MAX_ORGANIZATIONS;

        if (currentCount >= maxAllowed) {
            throw new IllegalStateException(
                "Cannot create organization. User has " + currentCount + " organizations, max allowed for " +
                command.planType() + " plan is " + maxAllowed
            );
        }

        Organization organization = new Organization(
            command.name(),
            command.ownerUserId(),
            command.planType()
        );

        return organizationRepository.save(organization);
    }

    @Override
    @Transactional
    public void handle(DeleteOrganizationCommand command) {
        Organization organization = organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        organizationRepository.delete(organization);
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return organizationRepository.findById(id);
    }

    @Override
    public List<Organization> findByOwnerUserId(UserId userId) {
        return organizationRepository.findByOwnerUserId(userId);
    }

    @Override
    public int countByOwnerUserId(UserId userId) {
        return organizationRepository.countByOwnerUserId(userId);
    }
}