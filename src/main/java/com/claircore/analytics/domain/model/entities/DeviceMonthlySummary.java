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
 * Pre-computed summary of one device's air quality over one calendar month
 * (America/Lima). Cascaded from that month's {@link DeviceDailySummary} rows:
 * averages are reading-count weighted, extremes are extremes-of-extremes, and
 * category breakdowns are summed. {@code summaryMonth} is the first day of the
 * month. Premium-gated at the interface layer.
 */
@Entity
@Table(name = "device_monthly_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "summary_month"}))
@EntityListeners(AuditingEntityListener.class)
public class DeviceMonthlySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_id", nullable = false))
    private DeviceId deviceId;

    @Column(name = "summary_month", nullable = false)
    private LocalDate summaryMonth;

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

    @Column(name = "days_covered", nullable = false)
    private int daysCovered;

    /** AQI change vs the previous month, as a percentage. Null when no prior month exists. */
    @Column(name = "aqi_delta_pct")
    private Double aqiDeltaPct;

    @Embedded
    private MonthlySummaryAudit auditFields = new MonthlySummaryAudit();

    protected DeviceMonthlySummary() {}

    public DeviceMonthlySummary(
            DeviceId deviceId,
            LocalDate summaryMonth,
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
            int daysCovered,
            Double aqiDeltaPct
    ) {
        if (deviceId == null) throw new IllegalArgumentException("deviceId must not be null");
        if (summaryMonth == null) throw new IllegalArgumentException("summaryMonth must not be null");
        if (daysCovered <= 0) throw new IllegalArgumentException("daysCovered must be positive");
        this.deviceId = deviceId;
        this.summaryMonth = summaryMonth.withDayOfMonth(1);
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
        this.daysCovered = daysCovered;
        this.aqiDeltaPct = aqiDeltaPct;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public LocalDate getSummaryMonth() { return summaryMonth; }
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
    public int getDaysCovered() { return daysCovered; }
    public Double getAqiDeltaPct() { return aqiDeltaPct; }
    public MonthlySummaryAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class MonthlySummaryAudit extends AuditableModel {}
}
