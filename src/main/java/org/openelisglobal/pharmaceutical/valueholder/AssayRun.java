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
 * AssayRun entity - Links pharmaceutical samples to notebook pages for assay execution.
 * Tracks controls, replicates, calculations, acceptance criteria, and OOS flags.
 */
@Entity
@Table(name = "PHARMA_ASSAY_RUN")
@DynamicUpdate
public class AssayRun extends BaseObject<Integer> {

    public enum AssayType {
        HPLC_POTENCY, TLC_ID, DISSOLUTION, MICROBIAL_LIMIT, STERILITY, UNIFORMITY, DISINTEGRATION, HARDNESS, FRIABILITY, OTHER
    }

    public enum AssayStatus {
        DRAFT, IN_PROGRESS, SUBMITTED, PRIMARY_REVIEW, SECONDARY_REVIEW, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_assay_run_seq")
    @SequenceGenerator(name = "pharma_assay_run_seq", sequenceName = "pharma_assay_run_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "SAMPLE_ID")
    private Integer sampleId;

    @Column(name = "ALIQUOT_ID")
    private Integer aliquotId;

    @Column(name = "NOTEBOOK_PAGE_ID", length = 36)
    private String notebookPageId;

    @Column(name = "TEMPLATE_ID", length = 50)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ASSAY_TYPE", length = 30, nullable = false)
    private AssayType assayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private AssayStatus status = AssayStatus.DRAFT;

    @Column(name = "CONTROLS_DATA", length = 4000)
    private String controlsData;

    @Column(name = "REPLICATES_DATA", length = 4000)
    private String replicatesData;

    @Column(name = "CALCULATIONS_DATA", length = 4000)
    private String calculationsData;

    @Column(name = "MEAN_VALUE", precision = 12, scale = 4)
    private BigDecimal meanValue;

    @Column(name = "RSD_PERCENT", precision = 8, scale = 4)
    private BigDecimal rsdPercent;

    @Column(name = "CV_PERCENT", precision = 8, scale = 4)
    private BigDecimal cvPercent;

    @Column(name = "RECOVERY_PERCENT", precision = 8, scale = 4)
    private BigDecimal recoveryPercent;

    @Column(name = "ACCEPTANCE_CRITERIA", length = 1000)
    private String acceptanceCriteria;

    @Column(name = "OOS_FLAG", nullable = false)
    private Boolean oosFlag = false;

    @Column(name = "OOS_REASON", length = 1000)
    private String oosReason;

    @Column(name = "DEVIATION_CAPA_ID")
    private Integer deviationCapaId;

    @Column(name = "ANALYST_ID", length = 36)
    private String analystId;

    @Column(name = "ANALYST_NAME", length = 255)
    private String analystName;

    @Column(name = "STARTED_AT")
    private Timestamp startedAt;

    @Column(name = "COMPLETED_AT")
    private Timestamp completedAt;

    @Column(name = "PRIMARY_REVIEWER_ID", length = 36)
    private String primaryReviewerId;

    @Column(name = "PRIMARY_REVIEWER_NAME", length = 255)
    private String primaryReviewerName;

    @Column(name = "PRIMARY_REVIEWED_AT")
    private Timestamp primaryReviewedAt;

    @Column(name = "PRIMARY_REVIEW_COMMENTS", length = 2000)
    private String primaryReviewComments;

    @Column(name = "SECONDARY_REVIEWER_ID", length = 36)
    private String secondaryReviewerId;

    @Column(name = "SECONDARY_REVIEWER_NAME", length = 255)
    private String secondaryReviewerName;

    @Column(name = "SECONDARY_REVIEWED_AT")
    private Timestamp secondaryReviewedAt;

    @Column(name = "SECONDARY_REVIEW_COMMENTS", length = 2000)
    private String secondaryReviewComments;

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

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public Integer getAliquotId() {
        return aliquotId;
    }

    public void setAliquotId(Integer aliquotId) {
        this.aliquotId = aliquotId;
    }

    public String getNotebookPageId() {
        return notebookPageId;
    }

    public void setNotebookPageId(String notebookPageId) {
        this.notebookPageId = notebookPageId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public AssayType getAssayType() {
        return assayType;
    }

    public void setAssayType(AssayType assayType) {
        this.assayType = assayType;
    }

    public AssayStatus getStatus() {
        return status;
    }

    public void setStatus(AssayStatus status) {
        this.status = status;
    }

    public String getControlsData() {
        return controlsData;
    }

    public void setControlsData(String controlsData) {
        this.controlsData = controlsData;
    }

    public String getReplicatesData() {
        return replicatesData;
    }

    public void setReplicatesData(String replicatesData) {
        this.replicatesData = replicatesData;
    }

    public String getCalculationsData() {
        return calculationsData;
    }

    public void setCalculationsData(String calculationsData) {
        this.calculationsData = calculationsData;
    }

    public BigDecimal getMeanValue() {
        return meanValue;
    }

    public void setMeanValue(BigDecimal meanValue) {
        this.meanValue = meanValue;
    }

    public BigDecimal getRsdPercent() {
        return rsdPercent;
    }

    public void setRsdPercent(BigDecimal rsdPercent) {
        this.rsdPercent = rsdPercent;
    }

    public BigDecimal getCvPercent() {
        return cvPercent;
    }

    public void setCvPercent(BigDecimal cvPercent) {
        this.cvPercent = cvPercent;
    }

    public BigDecimal getRecoveryPercent() {
        return recoveryPercent;
    }

    public void setRecoveryPercent(BigDecimal recoveryPercent) {
        this.recoveryPercent = recoveryPercent;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public Boolean getOosFlag() {
        return oosFlag;
    }

    public void setOosFlag(Boolean oosFlag) {
        this.oosFlag = oosFlag;
    }

    public String getOosReason() {
        return oosReason;
    }

    public void setOosReason(String oosReason) {
        this.oosReason = oosReason;
    }

    public Integer getDeviationCapaId() {
        return deviationCapaId;
    }

    public void setDeviationCapaId(Integer deviationCapaId) {
        this.deviationCapaId = deviationCapaId;
    }

    public String getAnalystId() {
        return analystId;
    }

    public void setAnalystId(String analystId) {
        this.analystId = analystId;
    }

    public String getAnalystName() {
        return analystName;
    }

    public void setAnalystName(String analystName) {
        this.analystName = analystName;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public String getPrimaryReviewerId() {
        return primaryReviewerId;
    }

    public void setPrimaryReviewerId(String primaryReviewerId) {
        this.primaryReviewerId = primaryReviewerId;
    }

    public String getPrimaryReviewerName() {
        return primaryReviewerName;
    }

    public void setPrimaryReviewerName(String primaryReviewerName) {
        this.primaryReviewerName = primaryReviewerName;
    }

    public Timestamp getPrimaryReviewedAt() {
        return primaryReviewedAt;
    }

    public void setPrimaryReviewedAt(Timestamp primaryReviewedAt) {
        this.primaryReviewedAt = primaryReviewedAt;
    }

    public String getPrimaryReviewComments() {
        return primaryReviewComments;
    }

    public void setPrimaryReviewComments(String primaryReviewComments) {
        this.primaryReviewComments = primaryReviewComments;
    }

    public String getSecondaryReviewerId() {
        return secondaryReviewerId;
    }

    public void setSecondaryReviewerId(String secondaryReviewerId) {
        this.secondaryReviewerId = secondaryReviewerId;
    }

    public String getSecondaryReviewerName() {
        return secondaryReviewerName;
    }

    public void setSecondaryReviewerName(String secondaryReviewerName) {
        this.secondaryReviewerName = secondaryReviewerName;
    }

    public Timestamp getSecondaryReviewedAt() {
        return secondaryReviewedAt;
    }

    public void setSecondaryReviewedAt(Timestamp secondaryReviewedAt) {
        this.secondaryReviewedAt = secondaryReviewedAt;
    }

    public String getSecondaryReviewComments() {
        return secondaryReviewComments;
    }

    public void setSecondaryReviewComments(String secondaryReviewComments) {
        this.secondaryReviewComments = secondaryReviewComments;
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
        if (startedAt == null) {
            startedAt = new Timestamp(System.currentTimeMillis());
        }
    }

    /**
     * Checks if assay can be approved (OOS requires linked CAPA).
     * @return true if ready for approval
     */
    public boolean canApprove() {
        if (oosFlag != null && oosFlag) {
            return deviationCapaId != null;
        }
        return true;
    }
}
