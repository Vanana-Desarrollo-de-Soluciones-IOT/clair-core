package com.claircore.analytics.domain.model.entities;

import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pre-computed summary of one device's air quality over one calendar day
 * (America/Lima). Built nightly from raw telemetry, so min/max/peak are true
 * extremes. One row per device per local date.
 */
@Entity
@Table(name = "device_daily_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "summary_date"}))
@EntityListeners(AuditingEntityListener.class)
public class DeviceDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_id", nullable = false))
    private DeviceId deviceId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "co2_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "co2_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "co2_max", nullable = false))
    })
    private MetricStats co2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "pm2_5_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "pm2_5_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "pm2_5_max", nullable = false))
    })
    private MetricStats pm2_5;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "temperature_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "temperature_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "temperature_max", nullable = false))
    })
    private MetricStats temperature;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "humidity_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "humidity_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "humidity_max", nullable = false))
    })
    private MetricStats humidity;

    @Column(name = "peak_pm2_5", nullable = false)
    private Double peakPm2_5;

    @Column(name = "peak_pm2_5_at", nullable = false)
    private Instant peakPm2_5At;

    @Column(name = "average_aqi", nullable = false)
    private Integer averageAqi;

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_aqi_category", nullable = false)
    private AqiCategory dominantAqiCategory;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "good", column = @Column(name = "cat_good", nullable = false)),
            @AttributeOverride(name = "moderate", column = @Column(name = "cat_moderate", nullable = false)),
            @AttributeOverride(name = "unhealthyForSensitive", column = @Column(name = "cat_unhealthy_sensitive", nullable = false)),
            @AttributeOverride(name = "unhealthy", column = @Column(name = "cat_unhealthy", nullable = false)),
            @AttributeOverride(name = "veryUnhealthy", column = @Column(name = "cat_very_unhealthy", nullable = false)),
            @AttributeOverride(name = "hazardous", column = @Column(name = "cat_hazardous", nullable = false))
    })
    private AqiCategoryBreakdown categoryBreakdown;

    @Column(name = "reading_count", nullable = false)
    private long readingCount;

    /** AQI change vs the previous day, as a percentage. Null when no prior day exists. */
    @Column(name = "aqi_delta_pct")
    private Double aqiDeltaPct;

    @Embedded
    private DailySummaryAudit auditFields = new DailySummaryAudit();

    protected DeviceDailySummary() {}

    public DeviceDailySummary(
            DeviceId deviceId,
            LocalDate summaryDate,
            MetricStats co2,
            MetricStats pm2_5,
            MetricStats temperature,
            MetricStats humidity,
            Double peakPm2_5,
            Instant peakPm2_5At,
            Integer averageAqi,
            AqiCategory dominantAqiCategory,
            AqiCategoryBreakdown categoryBreakdown,
            long readingCount,
            Double aqiDeltaPct
    ) {
        if (deviceId == null) throw new IllegalArgumentException("deviceId must not be null");
        if (summaryDate == null) throw new IllegalArgumentException("summaryDate must not be null");
        if (readingCount <= 0) throw new IllegalArgumentException("readingCount must be positive");
        this.deviceId = deviceId;
        this.summaryDate = summaryDate;
        this.co2 = co2;
        this.pm2_5 = pm2_5;
        this.temperature = temperature;
        this.humidity = humidity;
        this.peakPm2_5 = peakPm2_5;
        this.peakPm2_5At = peakPm2_5At;
        this.averageAqi = averageAqi;
        this.dominantAqiCategory = dominantAqiCategory;
        this.categoryBreakdown = categoryBreakdown;
        this.readingCount = readingCount;
        this.aqiDeltaPct = aqiDeltaPct;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public MetricStats getCo2() { return co2; }
    public MetricStats getPm2_5() { return pm2_5; }
    public MetricStats getTemperature() { return temperature; }
    public MetricStats getHumidity() { return humidity; }
    public Double getPeakPm2_5() { return peakPm2_5; }
    public Instant getPeakPm2_5At() { return peakPm2_5At; }
    public Integer getAverageAqi() { return averageAqi; }
    public AqiCategory getDominantAqiCategory() { return dominantAqiCategory; }
    public AqiCategoryBreakdown getCategoryBreakdown() { return categoryBreakdown; }
    public long getReadingCount() { return readingCount; }
    public Double getAqiDeltaPct() { return aqiDeltaPct; }
    public DailySummaryAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DailySummaryAudit extends AuditableModel {}
}
