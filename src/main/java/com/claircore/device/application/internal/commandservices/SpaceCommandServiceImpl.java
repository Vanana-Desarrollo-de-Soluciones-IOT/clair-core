package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.SpaceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SpaceCommandServiceImpl implements SpaceCommandService {

    private final SpaceRepository spaceRepository;

    public SpaceCommandServiceImpl(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    @Override
    @Transactional
    public Space handle(CreateSpaceCommand command) {
        Space space = new Space(
            command.name(),
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
    public List<Space> findByOwnerUserId(UserId userId) {
        return spaceRepository.findByOwnerUserId(userId);
    }
}