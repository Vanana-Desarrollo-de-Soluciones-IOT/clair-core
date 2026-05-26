package com.claircore.alerting.domain.services;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import org.springframework.data.domain.Page;

public interface AlertQueryService {
    Page<Alert> fetchByDevice(GetAlertsByDeviceQuery query);
    Page<Alert> fetchBySpace(GetAlertsBySpaceQuery query);
}
