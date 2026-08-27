package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_isolate", schema = "clinlims")
public class MicroIsolate extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "isolate_label", nullable = false, length = 40)
    private String isolateLabel;

    @Column(name = "organism_id", length = 36)
    private String organismId;

    @Column(name = "preliminary_organism_text")
    private String preliminaryOrganismText;

    @Column(name = "significance", nullable = false, length = 40)
    private String significance = MicroIsolateSignificance.UNKNOWN.name();

    @Column(name = "identification_status", nullable = false, length = 40)
    private String identificationStatus = MicroIsolateIdentificationStatus.PENDING.name();

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getIsolateLabel() {
        return isolateLabel;
    }

    public void setIsolateLabel(String isolateLabel) {
        this.isolateLabel = isolateLabel;
    }

    public String getOrganismId() {
        return organismId;
    }

    public void setOrganismId(String organismId) {
        this.organismId = organismId;
    }

    public String getPreliminaryOrganismText() {
        return preliminaryOrganismText;
    }

    public void setPreliminaryOrganismText(String preliminaryOrganismText) {
        this.preliminaryOrganismText = preliminaryOrganismText;
    }

    public String getSignificance() {
        return significance;
    }

    public void setSignificance(String significance) {
        this.significance = significance;
    }

    public String getIdentificationStatus() {
        return identificationStatus;
    }

    public void setIdentificationStatus(String identificationStatus) {
        this.identificationStatus = identificationStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
