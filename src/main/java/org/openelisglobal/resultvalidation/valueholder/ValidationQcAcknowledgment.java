package org.openelisglobal.resultvalidation.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Audit trail record for validator acknowledgment of QC failures before
 * releasing results.
 */
@Entity
@Table(name = "validation_qc_acknowledgment")
public class ValidationQcAcknowledgment extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Column(name = "analysis_id", nullable = false)
    private Integer analysisId;

    @NotNull
    @Column(name = "acknowledged_by", nullable = false)
    private Integer acknowledgedBy;

    @NotNull
    @Column(name = "acknowledged_at", nullable = false)
    private Timestamp acknowledgedAt;

    @NotNull
    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;

    public ValidationQcAcknowledgment() {
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public Integer getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(Integer acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
