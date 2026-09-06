package com.claircore.billing.application.internal.eventhandlers;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.UserPlanRepository;
// Crosses a context boundary: replaced by iam.interfaces.events.UserRegisteredIntegrationEvent in Phase 6.
import com.claircore.iam.interfaces.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class UserRegisteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventHandler.class);
    private final UserPlanRepository userPlanRepository;

    public UserRegisteredEventHandler(UserPlanRepository userPlanRepository) {
        this.userPlanRepository = userPlanRepository;
    }

    @EventListener
    public void on(UserRegisteredIntegrationEvent event) {
        log.info("Received UserRegisteredIntegrationEvent for user: {}", event.userId());
        userPlanRepository.save(new UserPlan(new UserId(event.userId())));
        log.info("Created FREEMIUM UserPlan for user: {}", event.userId());
    }
}
