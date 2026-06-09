package com.claircore.alerting.domain.services;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsByOwnerQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.DailyAlertCount;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AlertQueryService {
    Page<Alert> fetchByDevice(GetAlertsByDeviceQuery query);
    Page<Alert> fetchBySpace(GetAlertsBySpaceQuery query);
    Page<Alert> fetchByOwner(GetAlertsByOwnerQuery query, List<UUID> ownerDeviceIds);
    Page<Alert> fetchByDeviceAndStatus(GetAlertsByDeviceQuery query, List<AlertStatus> statuses);
    Page<Alert> fetchBySpaceAndStatus(GetAlertsBySpaceQuery query, List<AlertStatus> statuses);
    Page<Alert> fetchByOwnerAndStatus(GetAlertsByOwnerQuery query, List<UUID> ownerDeviceIds, List<AlertStatus> statuses);
    List<DailyAlertCount> fetchDailySummaryBySpace(UUID spaceId, int days);
    List<DailyAlertCount> fetchDailySummaryByOwner(UUID ownerUserId, List<UUID> ownerDeviceIds, int days);
}
