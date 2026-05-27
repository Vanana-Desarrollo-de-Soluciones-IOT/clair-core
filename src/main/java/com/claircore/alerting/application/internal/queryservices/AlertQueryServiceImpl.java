package com.claircore.alerting.application.internal.queryservices;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.DailyAlertCount;
import com.claircore.alerting.domain.services.AlertQueryService;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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

    @Override
    @Transactional(readOnly = true)
    public Page<Alert> fetchByDeviceAndStatus(GetAlertsByDeviceQuery query, List<AlertStatus> statuses) {
        return alertRepository.findByDeviceIdAndStatusIn(query.deviceId(), statuses, query.pageable());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Alert> fetchBySpaceAndStatus(GetAlertsBySpaceQuery query, List<AlertStatus> statuses) {
        return alertRepository.findBySpaceIdAndStatusIn(query.spaceId(), statuses, query.pageable());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAlertCount> fetchDailySummaryBySpace(UUID spaceId, int days) {
        Instant since = LocalDate.now(ZoneOffset.UTC).minusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Object[]> results = alertRepository.countAlertsPerDay(spaceId, since);
        return results.stream()
                .map(row -> new DailyAlertCount((LocalDate) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
