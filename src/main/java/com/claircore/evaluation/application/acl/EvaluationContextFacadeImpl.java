package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvaluationContextFacadeImpl implements EvaluationContextFacade {

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;
    private final JdbcTemplate jdbcTemplate;

    public EvaluationContextFacadeImpl(
            TelemetryEvaluationQueryService telemetryEvaluationQueryService,
            JdbcTemplate jdbcTemplate
    ) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(TelemetryEvaluation::getRecordedAt);
    }

    @Override
    public List<Map<String, Object>> getHourlyTelemetryAggregation(Instant start, Instant end) {
        String sql = """
                SELECT device_id,
                       AVG(aq_co2) as avg_co2,
                       AVG(pm_pm2_5) as avg_pm2_5,
                       AVG(aq_temperature) as avg_temperature,
                       AVG(aq_humidity) as avg_humidity
                FROM telemetry_evaluations
                WHERE recorded_at >= ? AND recorded_at < ?
                GROUP BY device_id
                """;

        return jdbcTemplate.queryForList(
                sql,
                Timestamp.from(start),
                Timestamp.from(end)
        );
    }
}
