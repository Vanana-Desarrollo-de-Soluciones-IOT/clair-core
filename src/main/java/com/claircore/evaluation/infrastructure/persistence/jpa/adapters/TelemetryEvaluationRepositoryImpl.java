package com.claircore.evaluation.infrastructure.persistence.jpa.adapters;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.HourlyDeviceAverage;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.infrastructure.persistence.jpa.assemblers.TelemetryEvaluationPersistenceAssembler;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TelemetryEvaluationRepositoryImpl implements TelemetryEvaluationRepository {

    /**
     * Unchanged from the facade that used to hold it. Every column named here is a column analytics
     * depends on; the DDL gate is what keeps them in place.
     */
    private static final String HOURLY_AVERAGES_SQL = """
            SELECT device_id,
                   AVG(aq_co2) as avg_co2,
                   AVG(pm_pm2_5) as avg_pm2_5,
                   AVG(aq_temperature) as avg_temperature,
                   AVG(aq_humidity) as avg_humidity
            FROM telemetry_evaluations
            WHERE recorded_at >= ? AND recorded_at < ?
            GROUP BY device_id
            """;

    private final TelemetryEvaluationPersistenceRepository telemetryEvaluationPersistenceRepository;
    private final JdbcTemplate jdbcTemplate;

    public TelemetryEvaluationRepositoryImpl(
            TelemetryEvaluationPersistenceRepository telemetryEvaluationPersistenceRepository,
            JdbcTemplate jdbcTemplate) {
        this.telemetryEvaluationPersistenceRepository = telemetryEvaluationPersistenceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TelemetryEvaluation save(TelemetryEvaluation evaluation) {
        var saved = telemetryEvaluationPersistenceRepository.save(
                TelemetryEvaluationPersistenceAssembler.toPersistenceFromDomain(evaluation));
        return TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public PageResult<TelemetryEvaluation> findByDeviceId(UUID deviceId, int page, int size) {
        var found = telemetryEvaluationPersistenceRepository
                .findByDeviceIdOrderByRecordedAtDesc(new DeviceId(deviceId), PageRequest.of(page, size));
        return new PageResult<>(
                found.getContent().stream().map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence).toList(),
                page,
                size,
                found.getTotalElements());
    }

    @Override
    public Optional<TelemetryEvaluation> findLatestByDeviceId(UUID deviceId) {
        return telemetryEvaluationPersistenceRepository.findFirstByDeviceIdOrderByRecordedAtDesc(new DeviceId(deviceId))
                .map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<HourlyDeviceAverage> findHourlyAveragesBetween(Instant start, Instant end) {
        return jdbcTemplate.query(
                HOURLY_AVERAGES_SQL,
                (rs, rowNum) -> new HourlyDeviceAverage(
                        toUuid(rs.getObject("device_id")),
                        rs.getDouble("avg_co2"),
                        rs.getDouble("avg_pm2_5"),
                        rs.getDouble("avg_temperature"),
                        rs.getDouble("avg_humidity")),
                Timestamp.from(start),
                Timestamp.from(end));
    }

    /** Postgres hands back a UUID, H2 a String; the caller used to do this itself. */
    private static UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }
}
