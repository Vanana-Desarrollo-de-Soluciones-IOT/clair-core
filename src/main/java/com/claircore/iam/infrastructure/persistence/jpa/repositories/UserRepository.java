package com.claircore.iam.infrastructure.persistence.jpa.repositories;

import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(EmailAddress email);
    boolean existsByEmail(EmailAddress email);
}
