package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_dst_resolution", schema = "clinlims")
public class MicroDstResolution extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        id = value;
    }

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String value) {
        caseId = value;
    }

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String value) {
        antibioticId = value;
    }

    @Column(name = "evidence_fingerprint", nullable = false, length = 64)
    private String evidenceFingerprint;

    public String getEvidenceFingerprint() {
        return evidenceFingerprint;
    }

    public void setEvidenceFingerprint(String value) {
        evidenceFingerprint = value;
    }

    @Column(name = "interpretation", nullable = false, length = 40)
    private String interpretation;

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String value) {
        interpretation = value;
    }

    @Column(name = "reason", nullable = false)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        reason = value;
    }

    @Column(name = "resolved_by", nullable = false, length = 20)
    private String resolvedBy;

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String value) {
        resolvedBy = value;
    }

    @Column(name = "resolved_at", nullable = false)
    private Timestamp resolvedAt;

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp value) {
        resolvedAt = value;
    }
}
