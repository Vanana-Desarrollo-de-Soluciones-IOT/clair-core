package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpaceRepository extends JpaRepository<Space, UUID> {
    List<Space> findByOrganizationId(UUID organizationId);
    int countByOrganizationId(UUID organizationId);
    int countByOwnerUserId(UserId ownerUserId);
    boolean existsByOrganizationId(UUID organizationId);
    void deleteByOrganizationId(UUID organizationId);

    boolean existsByIdAndOwnerUserId(UUID id, UserId ownerUserId);
}
