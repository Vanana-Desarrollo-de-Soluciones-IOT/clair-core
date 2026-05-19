package com.claircore.evaluation.interfaces.rest.transform;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResponse;

public class TelemetryEvaluationTransform {

    private TelemetryEvaluationTransform() {}

    public static TelemetryEvaluationResponse toResponse(TelemetryEvaluation e) {
        var aq = e.getAirQuality();
        var pm = e.getParticulateMatter();
        var conn = e.getConnectivity();
        var dh = e.getDeviceHealth();
        var di = e.getDeviceInfo();

        return new TelemetryEvaluationResponse(
                e.getId(),
                e.getDeviceId().value(),
                e.getDeviceTimestamp(),
                e.getUptimeSeconds(),
                new TelemetryEvaluationResponse.AirQualityResponse(
                        aq.co2(), aq.temperature(), aq.humidity(), aq.valid()
                ),
                new TelemetryEvaluationResponse.ParticulateMatterResponse(
                        pm.pm1_0(), pm.pm2_5(), pm.pm10(), pm.valid()
                ),
                new TelemetryEvaluationResponse.ConnectivityResponse(
                        conn.status(), conn.ssid(), conn.ip(), conn.rssi(), conn.mac(), conn.channel()
                ),
                new TelemetryEvaluationResponse.DeviceHealthResponse(
                        dh.freeHeap(), dh.minFreeHeap(), dh.heapSize(), dh.maxAllocHeap(),
                        dh.scd41Status(), dh.pms5003Status(), dh.lastValidAirQualitySec(), dh.lastValidPMSec()
                ),
                new TelemetryEvaluationResponse.DeviceInfoResponse(
                        di.chipModel(), di.chipRevision(), di.cpuFreqMHz(), di.flashSize(), di.sketchSize(), di.freeSketchSpace()
                ),
                e.getStatus(),
                e.getStatusCode(),
                e.getRecordedAt(),
                e.getAuditFields().getCreatedAt() != null
                        ? e.getAuditFields().getCreatedAt().toInstant()
                        : null
        );
    }
}
