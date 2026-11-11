package org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Immutable audit row written on each "mark week submitted" action (FR-008 /
 * US4). One row per submission; re-submission of a week creates a new distinct
 * row (no update/delete in the service). Captures a JSON snapshot of the
 * visible figures at submit time, the reporting period, the acting user, and
 * the timestamp.
 */
@Entity
@Table(name = "manual_entry_submission_audit", schema = "clinlims")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class ManualEntrySubmissionAudit extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_entry_submission_audit_seq_gen")
    @SequenceGenerator(name = "manual_entry_submission_audit_seq_gen", sequenceName = "manual_entry_submission_audit_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "site_id")
    private Integer siteId;

    @Column(name = "value_snapshot", columnDefinition = "TEXT", nullable = false)
    private String valueSnapshot;

    @Column(name = "submitted_by_user_id", length = 50, nullable = false)
    private String submittedByUserId;

    @Column(name = "submitted_at", nullable = false)
    private Timestamp submittedAt;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    public String getValueSnapshot() {
        return valueSnapshot;
    }

    public void setValueSnapshot(String valueSnapshot) {
        this.valueSnapshot = valueSnapshot;
    }

    public String getSubmittedByUserId() {
        return submittedByUserId;
    }

    public void setSubmittedByUserId(String submittedByUserId) {
        this.submittedByUserId = submittedByUserId;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }
}
