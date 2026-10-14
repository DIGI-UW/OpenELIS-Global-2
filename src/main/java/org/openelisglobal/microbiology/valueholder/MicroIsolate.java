package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Column(name = "amendment_id", length = 36)
    private String amendmentId;

    @Column(name = "isolate_label", nullable = false, length = 40)
    private String isolateLabel;

    @Column(name = "organism_id", length = 36)
    private String organismId;

    @Column(name = "preliminary_organism_text")
    private String preliminaryOrganismText;

    @Column(name = "gram_stain")
    private String gramStain;

    @Column(name = "colony_morphology")
    private String colonyMorphology;

    @Column(name = "identification_method", length = 80)
    private String identificationMethod;

    @Column(name = "identification_confidence", precision = 5, scale = 2)
    private BigDecimal identificationConfidence;

    @Column(name = "significance", nullable = false, length = 40)
    private String significance = MicroIsolateSignificance.UNKNOWN.name();

    @Column(name = "identification_status", nullable = false, length = 40)
    private String identificationStatus = MicroIsolateIdentificationStatus.PENDING.name();

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "cancelled_at")
    private Timestamp cancelledAt;

    @Column(name = "source_culture_id", length = 36)
    private String sourceCultureId;

    @Column(name = "source_sample_item_id", precision = 10, scale = 0)
    @org.hibernate.annotations.Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sourceSampleItemId;

    @Column(name = "sample_item_id", precision = 10, scale = 0)
    @org.hibernate.annotations.Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleItemId;

    @Column(name = "received_from_organization_id", precision = 10, scale = 0)
    @org.hibernate.annotations.Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String receivedFromOrganizationId;

    @Column(name = "reported_organism_id", length = 36)
    private String reportedOrganismId;

    @Column(name = "reported_organism_text", length = 255)
    private String reportedOrganismText;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "identified_at")
    private Timestamp identifiedAt;

    @Column(name = "mismatch_acknowledged_by", length = 20)
    private String mismatchAcknowledgedBy;

    @Column(name = "mismatch_acknowledged_at")
    private Timestamp mismatchAcknowledgedAt;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "source_culture_id", insertable = false, updatable = false)
    private MicroCaseInoculation sourceCulture;

    @jakarta.persistence.OneToMany
    @jakarta.persistence.JoinColumn(name = "isolate_id", insertable = false, updatable = false)
    @jakarta.persistence.OrderBy("eventSequence, changedAt, id")
    private java.util.List<MicroIsolateIdentificationEvent> identificationHistory = new java.util.ArrayList<>();

    public MicroCaseInoculation getSourceCulture() {
        return sourceCulture;
    }

    public void setSourceCulture(MicroCaseInoculation value) {
        sourceCulture = value;
    }

    public java.util.List<MicroIsolateIdentificationEvent> getIdentificationHistory() {
        return identificationHistory;
    }

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

    public String getAmendmentId() {
        return amendmentId;
    }

    public void setAmendmentId(String amendmentId) {
        this.amendmentId = amendmentId;
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

    public String getGramStain() {
        return gramStain;
    }

    public void setGramStain(String gramStain) {
        this.gramStain = gramStain;
    }

    public String getColonyMorphology() {
        return colonyMorphology;
    }

    public void setColonyMorphology(String colonyMorphology) {
        this.colonyMorphology = colonyMorphology;
    }

    public String getIdentificationMethod() {
        return identificationMethod;
    }

    public void setIdentificationMethod(String identificationMethod) {
        this.identificationMethod = identificationMethod;
    }

    public BigDecimal getIdentificationConfidence() {
        return identificationConfidence;
    }

    public void setIdentificationConfidence(BigDecimal identificationConfidence) {
        this.identificationConfidence = identificationConfidence;
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

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getSourceCultureId() {
        return sourceCultureId;
    }

    public void setSourceCultureId(String value) {
        sourceCultureId = value;
    }

    public String getSourceSampleItemId() {
        return sourceSampleItemId;
    }

    public void setSourceSampleItemId(String value) {
        sourceSampleItemId = value;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String value) {
        sampleItemId = value;
    }

    public String getReceivedFromOrganizationId() {
        return receivedFromOrganizationId;
    }

    public void setReceivedFromOrganizationId(String value) {
        receivedFromOrganizationId = value;
    }

    public String getReportedOrganismId() {
        return reportedOrganismId;
    }

    public void setReportedOrganismId(String value) {
        reportedOrganismId = value;
    }

    public String getReportedOrganismText() {
        return reportedOrganismText;
    }

    public void setReportedOrganismText(String value) {
        reportedOrganismText = value;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String value) {
        createdBy = value;
    }

    public Timestamp getIdentifiedAt() {
        return identifiedAt;
    }

    public void setIdentifiedAt(Timestamp value) {
        identifiedAt = value;
    }

    public String getMismatchAcknowledgedBy() {
        return mismatchAcknowledgedBy;
    }

    public void setMismatchAcknowledgedBy(String value) {
        mismatchAcknowledgedBy = value;
    }

    public Timestamp getMismatchAcknowledgedAt() {
        return mismatchAcknowledgedAt;
    }

    public void setMismatchAcknowledgedAt(Timestamp value) {
        mismatchAcknowledgedAt = value;
    }
}
