package org.openelisglobal.qaevent.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;

/**
 * Response DTO for delta check alerts. Avoids serializing raw JPA entities with
 * LAZY-loaded relationships.
 */
public class DeltaCheckAlertResponseForm {

    private Integer id;
    private String resultId;
    private String previousResultId;
    private BigDecimal currentValue;
    private BigDecimal previousValue;
    private BigDecimal changePercent;
    private BigDecimal thresholdPercent;
    private String status;
    private String dismissalReason;
    private String dismissedBy;
    private Timestamp dismissedDate;
    private String escalatedNceId;
    private Timestamp previousResultDate;
    private Timestamp createdDate;

    public DeltaCheckAlertResponseForm() {
    }

    public static DeltaCheckAlertResponseForm fromEntity(DeltaCheckAlert alert) {
        DeltaCheckAlertResponseForm form = new DeltaCheckAlertResponseForm();
        form.setId(alert.getId());
        form.setResultId(alert.getResultId());
        form.setPreviousResultId(alert.getPreviousResultId());
        form.setCurrentValue(alert.getCurrentValue());
        form.setPreviousValue(alert.getPreviousValue());
        form.setChangePercent(alert.getChangePercent());
        form.setThresholdPercent(alert.getThresholdPercent());
        form.setStatus(alert.getStatus());
        form.setDismissalReason(alert.getDismissalReason());
        form.setDismissedBy(alert.getDismissedBy());
        form.setDismissedDate(alert.getDismissedDate());
        form.setEscalatedNceId(alert.getEscalatedNcEvent() != null ? alert.getEscalatedNcEvent().getId() : null);
        form.setPreviousResultDate(alert.getPreviousResultDate());
        form.setCreatedDate(alert.getCreatedDate());
        return form;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getPreviousResultId() {
        return previousResultId;
    }

    public void setPreviousResultId(String previousResultId) {
        this.previousResultId = previousResultId;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = previousValue;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getEscalatedNceId() {
        return escalatedNceId;
    }

    public void setEscalatedNceId(String escalatedNceId) {
        this.escalatedNceId = escalatedNceId;
    }

    public Timestamp getPreviousResultDate() {
        return previousResultDate;
    }

    public void setPreviousResultDate(Timestamp previousResultDate) {
        this.previousResultDate = previousResultDate;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }
}
