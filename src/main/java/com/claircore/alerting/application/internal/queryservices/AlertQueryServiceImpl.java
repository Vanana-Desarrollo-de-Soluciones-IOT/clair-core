package com.claircore.alerting.application.internal.queryservices;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.services.AlertQueryService;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertQueryServiceImpl implements AlertQueryService {

    private final AlertRepository alertRepository;

    public AlertQueryServiceImpl(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Alert> fetchByDevice(GetAlertsByDeviceQuery query) {
        return alertRepository.findByDeviceId(query.deviceId(), query.pageable());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Alert> fetchBySpace(GetAlertsBySpaceQuery query) {
        return alertRepository.findBySpaceId(query.spaceId(), query.pageable());
    }
}
