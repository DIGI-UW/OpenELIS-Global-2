package org.openelisglobal.pharmaceutical.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCCheck entity - Lab-type-specific QC checklist result.
 * Records pass/fail outcomes with rejection reasons for pharmaceutical QC.
 */
@Entity
@Table(name = "PHARMA_QC_CHECK")
@DynamicUpdate
public class QCCheck extends BaseObject<Integer> {

    public enum QCOutcome {
        PASS, FAIL, PENDING, PARTIAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_qc_check_seq")
    @SequenceGenerator(name = "pharma_qc_check_seq", sequenceName = "pharma_qc_check_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAMPLE_ID", nullable = false)
    private PharmaceuticalSample sample;

    @Enumerated(EnumType.STRING)
    @Column(name = "LAB_TYPE", length = 20, nullable = false)
    private PharmaceuticalSample.LabType labType;

    @Column(name = "CHECKLIST_VERSION", length = 20)
    private String checklistVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "OUTCOME", length = 20, nullable = false)
    private QCOutcome outcome = QCOutcome.PENDING;

    @Column(name = "REJECTION_REASONS", length = 2000)
    private String rejectionReasons;

    @Column(name = "REVIEWER_ID", length = 36)
    private String reviewerId;

    @Column(name = "REVIEWER_NAME", length = 255)
    private String reviewerName;

    @Column(name = "REVIEWED_AT")
    private Timestamp reviewedAt;

    @Column(name = "CHECKLIST_DATA", length = 4000)
    private String checklistData;

    @Column(name = "CREATED_AT")
    private Timestamp createdAt;

    @Column(name = "NOTES", length = 1000)
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

    public PharmaceuticalSample getSample() {
        return sample;
    }

    public void setSample(PharmaceuticalSample sample) {
        this.sample = sample;
    }

    public PharmaceuticalSample.LabType getLabType() {
        return labType;
    }

    public void setLabType(PharmaceuticalSample.LabType labType) {
        this.labType = labType;
    }

    public String getChecklistVersion() {
        return checklistVersion;
    }

    public void setChecklistVersion(String checklistVersion) {
        this.checklistVersion = checklistVersion;
    }

    public QCOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(QCOutcome outcome) {
        this.outcome = outcome;
    }

    public String getRejectionReasons() {
        return rejectionReasons;
    }

    public void setRejectionReasons(String rejectionReasons) {
        this.rejectionReasons = rejectionReasons;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getChecklistData() {
        return checklistData;
    }

    public void setChecklistData(String checklistData) {
        this.checklistData = checklistData;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
