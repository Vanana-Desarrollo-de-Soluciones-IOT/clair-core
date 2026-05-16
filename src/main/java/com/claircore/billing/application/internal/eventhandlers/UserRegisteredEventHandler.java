package com.claircore.billing.application.internal.eventhandlers;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.infrastructure.persistence.jpa.repositories.UserPlanRepository;
import com.claircore.iam.domain.model.events.UserRegisteredEvent;
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
    public void on(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {}", event.getUserId());
        var userId = new UserId(event.getUserId());
        var userPlan = new UserPlan(userId);
        userPlanRepository.save(userPlan);
        log.info("Created VISITOR UserPlan for user: {}", event.getUserId());
    }
}
