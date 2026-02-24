package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.result.valueholder.Result;

/**
 * Entity class for delta check alerts.
 * Delta check alerts are generated when lab result values change by more than
 * the configured threshold compared to previous results for the same patient.
 */
@Entity
@Table(name = "delta_check_alert")
public class DeltaCheckAlert extends BaseObject<Integer> {

    /**
     * Status of delta check alerts
     */
    public enum AlertStatus {
        ACTIVE("Alert is active and needs attention"),
        DISMISSED("Alert has been dismissed by user"),
        ESCALATED_NCE("Alert has been escalated to create an NCE");

        private final String description;

        AlertStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", referencedColumnName = "id")
    private Result result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_result_id", referencedColumnName = "id")
    private Result previousResult;

    @Basic
    @Column(name = "current_value", precision = 15, scale = 6)
    private BigDecimal currentValue;

    @Basic
    @Column(name = "previous_value", precision = 15, scale = 6)
    private BigDecimal previousValue;

    @Basic
    @Column(name = "change_percent", precision = 6, scale = 2)
    private BigDecimal changePercent;

    @Basic
    @Column(name = "threshold_percent", precision = 6, scale = 2)
    private BigDecimal thresholdPercent;

    @Basic
    @Column(name = "status", length = 20)
    private String status;

    @Basic
    @Column(name = "dismissal_reason", columnDefinition = "TEXT")
    private String dismissalReason;

    @Basic
    @Column(name = "dismissed_by", length = 100)
    private String dismissedBy;

    @Basic
    @Column(name = "dismissed_date")
    private Timestamp dismissedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_nce_id", referencedColumnName = "id")
    private NcEvent escalatedNcEvent;

    @Basic
    @Column(name = "created_date")
    private Timestamp createdDate;

    public DeltaCheckAlert() {
        super();
        this.status = AlertStatus.ACTIVE.name();
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    public DeltaCheckAlert(Result result, Result previousResult, BigDecimal currentValue,
                          BigDecimal previousValue, BigDecimal thresholdPercent) {
        this();
        this.result = result;
        this.previousResult = previousResult;
        this.currentValue = currentValue;
        this.previousValue = previousValue;
        this.thresholdPercent = thresholdPercent;
        this.changePercent = calculateChangePercent(currentValue, previousValue);
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Result getPreviousResult() {
        return previousResult;
    }

    public void setPreviousResult(Result previousResult) {
        this.previousResult = previousResult;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public double getCurrentValueAsDouble() {
        return currentValue != null ? currentValue.doubleValue() : 0.0;
    }

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = previousValue;
    }

    public double getPreviousValueAsDouble() {
        return previousValue != null ? previousValue.doubleValue() : 0.0;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public double getChangePercentAsDouble() {
        return changePercent != null ? changePercent.doubleValue() : 0.0;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public double getThresholdPercentAsDouble() {
        return thresholdPercent != null ? thresholdPercent.doubleValue() : 0.0;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AlertStatus getStatusEnum() {
        try {
            return AlertStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return AlertStatus.ACTIVE; // Default fallback
        }
    }

    public void setStatusEnum(AlertStatus status) {
        this.status = status.name();
    }

    public String getDismissalReason() {
        return dismissalReason;
    }

    public void setDismissalReason(String dismissalReason) {
        this.dismissalReason = dismissalReason;
    }

    public String getDismissedBy() {
        return dismissedBy;
    }

    public void setDismissedBy(String dismissedBy) {
        this.dismissedBy = dismissedBy;
    }

    public Timestamp getDismissedDate() {
        return dismissedDate;
    }

    public void setDismissedDate(Timestamp dismissedDate) {
        this.dismissedDate = dismissedDate;
    }

    public NcEvent getEscalatedNcEvent() {
        return escalatedNcEvent;
    }

    public void setEscalatedNcEvent(NcEvent escalatedNcEvent) {
        this.escalatedNcEvent = escalatedNcEvent;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Dismiss this alert with a reason and user info
     */
    public void dismiss(String reason, String dismissedBy) {
        this.status = AlertStatus.DISMISSED.name();
        this.dismissalReason = reason;
        this.dismissedBy = dismissedBy;
        this.dismissedDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Escalate this alert to an NCE
     */
    public void escalateToNCE(NcEvent ncEvent) {
        this.status = AlertStatus.ESCALATED_NCE.name();
        this.escalatedNcEvent = ncEvent;
    }

    /**
     * Check if this alert is still active
     */
    public boolean isActive() {
        return AlertStatus.ACTIVE.name().equals(status);
    }

    /**
     * Check if this alert has been dismissed
     */
    public boolean isDismissed() {
        return AlertStatus.DISMISSED.name().equals(status);
    }

    /**
     * Check if this alert has been escalated to an NCE
     */
    public boolean isEscalatedToNCE() {
        return AlertStatus.ESCALATED_NCE.name().equals(status);
    }

    /**
     * Calculate percentage change between two values
     */
    private static BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = current.subtract(previous);
        BigDecimal percentChange = difference.divide(previous, 4, BigDecimal.ROUND_HALF_UP)
                                            .multiply(BigDecimal.valueOf(100));
        return percentChange.abs(); // Return absolute value
    }

    /**
     * Get a human-readable description of the alert
     */
    public String getAlertDescription() {
        return String.format("Result value changed from %s to %s (%.1f%% change, threshold: %.1f%%)",
                getPreviousValueAsDouble(),
                getCurrentValueAsDouble(),
                getChangePercentAsDouble(),
                getThresholdPercentAsDouble());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeltaCheckAlert that = (DeltaCheckAlert) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DeltaCheckAlert{" +
                "id=" + id +
                ", resultId=" + (result != null ? result.getId() : null) +
                ", previousResultId=" + (previousResult != null ? previousResult.getId() : null) +
                ", currentValue=" + currentValue +
                ", previousValue=" + previousValue +
                ", changePercent=" + changePercent +
                ", thresholdPercent=" + thresholdPercent +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}