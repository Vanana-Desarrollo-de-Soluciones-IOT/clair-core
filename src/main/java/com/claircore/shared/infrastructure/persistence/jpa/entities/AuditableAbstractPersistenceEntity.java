package com.claircore.shared.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for every JPA persistence entity: identifier plus audit timestamps.
 *
 * <p>The identifier is assigned by the aggregate (a plain {@code UUID.randomUUID()} in its
 * constructor), never by the database, so there is no generator here. That would send every insert
 * down {@code SimpleJpaRepository.save()}'s {@code merge} branch, because its default
 * {@code isNew()} is {@code id == null}; {@link Persistable} overrides that test with
 * {@code createdAt == null}, true exactly until {@code @CreatedDate} fills the field on the first
 * insert. Reading it from the state rather than from a transient flag keeps the answer correct for
 * entities a persistence assembler builds fresh on every save.
 *
 * <p>The timestamps are {@link Instant}, but {@link JdbcTypeCode} pins them to a plain
 * {@code timestamp(6)} column rather than the {@code timestamp with time zone} Hibernate would
 * choose by default. Every table in this schema predates the split and has the plain type; changing
 * it is a migration, not a refactor, so it belongs in the shared cleanup phase, where the diff is
 * accepted once for every table together. Deleting the two annotations is that change.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableAbstractPersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false)
    private Instant updatedAt;

    protected AuditableAbstractPersistenceEntity() {
        // JPA
    }

    @Override
    public UUID getId() {
        return id;
    }

    /** @see AuditableAbstractPersistenceEntity the class javadoc for why this is not a flag */
    @Override
    public boolean isNew() {
        return createdAt == null;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
