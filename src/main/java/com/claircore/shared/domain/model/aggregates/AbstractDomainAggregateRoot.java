package com.claircore.shared.domain.model.aggregates;

import org.springframework.data.domain.AbstractAggregateRoot;

/**
 * Base class for aggregate roots that publish domain events.
 *
 * <p>This is the single documented framework dependency allowed inside {@code domain}: it wraps
 * Spring Data's {@link AbstractAggregateRoot} so that aggregates never name the framework type
 * themselves. Nothing else from Spring, JPA or Hibernate may appear under a {@code domain} package.
 *
 * <p>Subclasses record events with the inherited {@code protected registerEvent(E)} and
 * {@code andEvent(E)}; the events are published when the aggregate is saved through its repository.
 * Access is deliberately left at {@code protected}: only the aggregate itself decides what it emits.
 *
 * @param <T> the concrete aggregate type, for fluent {@code andEvent} chaining
 */
public abstract class AbstractDomainAggregateRoot<T extends AbstractDomainAggregateRoot<T>>
        extends AbstractAggregateRoot<T> {
}
