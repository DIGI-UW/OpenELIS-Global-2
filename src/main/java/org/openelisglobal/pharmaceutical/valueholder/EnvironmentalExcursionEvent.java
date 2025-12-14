package org.openelisglobal.pharmaceutical.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * EnvironmentalExcursionEvent entity - Records equipment failures and storage excursions.
 * Tracks temperature deviations, contingency actions, and affected samples.
 */
@Entity
@Table(name = "PHARMA_EXCURSION_EVENT")
@DynamicUpdate
public class EnvironmentalExcursionEvent extends BaseObject<Integer> {

    public enum AlertType {
        TEMPERATURE_HIGH, TEMPERATURE_LOW, POWER_FAILURE, DOOR_OPEN, HUMIDITY_HIGH, HUMIDITY_LOW, EQUIPMENT_MALFUNCTION, OTHER
    }

    public enum ExcursionStatus {
        ACTIVE, ACKNOWLEDGED, RESOLVED, ESCALATED, UNDER_INVESTIGATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_excursion_seq")
    @SequenceGenerator(name = "pharma_excursion_seq", sequenceName = "pharma_excursion_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "DEVICE_ID")
    private Integer deviceId;

    @Column(name = "DEVICE_NAME", length = 255)
    private String deviceName;

    @Column(name = "LOCATION_PATH", length = 500)
    private String locationPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "ALERT_TYPE", length = 30, nullable = false)
    private AlertType alertType;

    @Column(name = "TEMPERATURE_READING", precision = 6, scale = 2)
    private BigDecimal temperatureReading;

    @Column(name = "THRESHOLD_MIN", precision = 6, scale = 2)
    private BigDecimal thresholdMin;

    @Column(name = "THRESHOLD_MAX", precision = 6, scale = 2)
    private BigDecimal thresholdMax;

    @Column(name = "THRESHOLD_VIOLATED", length = 100)
    private String thresholdViolated;

    @Column(name = "RECORDED_VALUE")
    private Double recordedValue;

    @Column(name = "THRESHOLD_VALUE")
    private Double thresholdValue;

    @Column(name = "DEVICE_LOCATION", length = 500)
    private String deviceLocation;

    @Column(name = "DURATION_MINUTES")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 30, nullable = false)
    private ExcursionStatus status = ExcursionStatus.ACTIVE;

    @Column(name = "CONTINGENCY_ACTION", length = 2000)
    private String contingencyAction;

    @Column(name = "BACKUP_LOCATION_ID")
    private Integer backupLocationId;

    @Column(name = "BACKUP_LOCATION_NAME", length = 255)
    private String backupLocationName;

    @Column(name = "AFFECTED_SAMPLE_COUNT")
    private Integer affectedSampleCount;

    @Column(name = "AFFECTED_SAMPLE_IDS", length = 4000)
    private String affectedSampleIds;

    @Column(name = "DETECTED_AT", nullable = false)
    private Timestamp detectedAt;

    @Column(name = "ACKNOWLEDGED_AT")
    private Timestamp acknowledgedAt;

    @Column(name = "ACKNOWLEDGED_BY", length = 36)
    private String acknowledgedBy;

    @Column(name = "RESOLVED_AT")
    private Timestamp resolvedAt;

    @Column(name = "RESOLVED_BY", length = 36)
    private String resolvedBy;

    @Column(name = "RESOLVED_BY_ID", length = 36)
    private String resolvedById;

    @Column(name = "RESOLVED_BY_NAME", length = 255)
    private String resolvedByName;

    @Column(name = "ESCALATION_REASON", length = 2000)
    private String escalationReason;

    @Column(name = "RESOLUTION_NOTES", length = 2000)
    private String resolutionNotes;

    @Column(name = "NOTIFICATION_SENT")
    private Boolean notificationSent;

    @Column(name = "NOTIFICATION_SENT_AT")
    private Timestamp notificationSentAt;

    @Column(name = "NOTES", length = 2000)
    private String notes;

    @Column(name = "SYS_USER_ID", nullable = false, length = 36)
    private String sysUserIdValue;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getLocationPath() {
        return locationPath;
    }

    public void setLocationPath(String locationPath) {
        this.locationPath = locationPath;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public BigDecimal getTemperatureReading() {
        return temperatureReading;
    }

    public void setTemperatureReading(BigDecimal temperatureReading) {
        this.temperatureReading = temperatureReading;
    }

    public BigDecimal getThresholdMin() {
        return thresholdMin;
    }

    public void setThresholdMin(BigDecimal thresholdMin) {
        this.thresholdMin = thresholdMin;
    }

    public BigDecimal getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(BigDecimal thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public String getThresholdViolated() {
        return thresholdViolated;
    }

    public void setThresholdViolated(String thresholdViolated) {
        this.thresholdViolated = thresholdViolated;
    }

    public Double getRecordedValue() {
        return recordedValue;
    }

    public void setRecordedValue(Double recordedValue) {
        this.recordedValue = recordedValue;
    }

    public Double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getDeviceLocation() {
        return deviceLocation;
    }

    public void setDeviceLocation(String deviceLocation) {
        this.deviceLocation = deviceLocation;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public ExcursionStatus getStatus() {
        return status;
    }

    public void setStatus(ExcursionStatus status) {
        this.status = status;
    }

    public String getContingencyAction() {
        return contingencyAction;
    }

    public void setContingencyAction(String contingencyAction) {
        this.contingencyAction = contingencyAction;
    }

    public Integer getBackupLocationId() {
        return backupLocationId;
    }

    public void setBackupLocationId(Integer backupLocationId) {
        this.backupLocationId = backupLocationId;
    }

    public String getBackupLocationName() {
        return backupLocationName;
    }

    public void setBackupLocationName(String backupLocationName) {
        this.backupLocationName = backupLocationName;
    }

    public Integer getAffectedSampleCount() {
        return affectedSampleCount;
    }

    public void setAffectedSampleCount(Integer affectedSampleCount) {
        this.affectedSampleCount = affectedSampleCount;
    }

    public String getAffectedSampleIds() {
        return affectedSampleIds;
    }

    public void setAffectedSampleIds(String affectedSampleIds) {
        this.affectedSampleIds = affectedSampleIds;
    }

    public Timestamp getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Timestamp detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolvedById() {
        return resolvedById;
    }

    public void setResolvedById(String resolvedById) {
        this.resolvedById = resolvedById;
    }

    public String getResolvedByName() {
        return resolvedByName;
    }

    public void setResolvedByName(String resolvedByName) {
        this.resolvedByName = resolvedByName;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Timestamp getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(Timestamp notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String getSysUserId() {
        return sysUserIdValue;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserIdValue = sysUserId;
    }

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
