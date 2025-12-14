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
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * DisposalRecord entity - Records disposal or archiving decisions.
 * Tracks disposal method, sign-offs, and retention information.
 */
@Entity
@Table(name = "PHARMA_DISPOSAL_RECORD")
@DynamicUpdate
public class DisposalRecord extends BaseObject<Integer> {

    public enum DisposalReason {
        EXPIRY, EXHAUSTED, QC_FAIL, SAFETY, STUDY_COMPLETE, DAMAGE, OTHER
    }

    public enum DisposalMethod {
        INCINERATION, AUTOCLAVE, CHEMICAL_TREATMENT, RETURN_TO_SPONSOR, ARCHIVAL, OTHER
    }

    public enum DisposalStatus {
        PENDING_APPROVAL, APPROVED, DISPOSED, ARCHIVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_disposal_seq")
    @SequenceGenerator(name = "pharma_disposal_seq", sequenceName = "pharma_disposal_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "SAMPLE_ID")
    private Integer sampleId;

    @Column(name = "ALIQUOT_ID")
    private Integer aliquotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISPOSAL_REASON", length = 20, nullable = false)
    private DisposalReason disposalReason;

    @Column(name = "REASON_DETAILS", length = 1000)
    private String reasonDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISPOSAL_METHOD", length = 30)
    private DisposalMethod disposalMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private DisposalStatus status = DisposalStatus.PENDING_APPROVAL;

    @Column(name = "REQUESTED_BY_ID", length = 36)
    private String requestedById;

    @Column(name = "REQUESTED_BY_NAME", length = 255)
    private String requestedByName;

    @Column(name = "REQUESTED_AT")
    private Timestamp requestedAt;

    @Column(name = "APPROVED_BY_ID", length = 36)
    private String approvedById;

    @Column(name = "APPROVED_BY_NAME", length = 255)
    private String approvedByName;

    @Column(name = "APPROVED_AT")
    private Timestamp approvedAt;

    @Column(name = "DISPOSED_BY_ID", length = 36)
    private String disposedById;

    @Column(name = "DISPOSED_BY_NAME", length = 255)
    private String disposedByName;

    @Column(name = "DISPOSED_AT")
    private Timestamp disposedAt;

    @Column(name = "RETENTION_UNTIL")
    private Timestamp retentionUntil;

    @Column(name = "ARCHIVE_LOCATION", length = 255)
    private String archiveLocation;

    @Column(name = "WITNESS_ID", length = 36)
    private String witnessId;

    @Column(name = "WITNESS_NAME", length = 255)
    private String witnessName;

    @Column(name = "CERTIFICATE_NUMBER", length = 100)
    private String certificateNumber;

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

    public DisposalReason getDisposalReason() {
        return disposalReason;
    }

    public void setDisposalReason(DisposalReason disposalReason) {
        this.disposalReason = disposalReason;
    }

    public String getReasonDetails() {
        return reasonDetails;
    }

    public void setReasonDetails(String reasonDetails) {
        this.reasonDetails = reasonDetails;
    }

    public DisposalMethod getDisposalMethod() {
        return disposalMethod;
    }

    public void setDisposalMethod(DisposalMethod disposalMethod) {
        this.disposalMethod = disposalMethod;
    }

    public DisposalStatus getStatus() {
        return status;
    }

    public void setStatus(DisposalStatus status) {
        this.status = status;
    }

    public String getRequestedById() {
        return requestedById;
    }

    public void setRequestedById(String requestedById) {
        this.requestedById = requestedById;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getApprovedById() {
        return approvedById;
    }

    public void setApprovedById(String approvedById) {
        this.approvedById = approvedById;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Timestamp approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getDisposedById() {
        return disposedById;
    }

    public void setDisposedById(String disposedById) {
        this.disposedById = disposedById;
    }

    public String getDisposedByName() {
        return disposedByName;
    }

    public void setDisposedByName(String disposedByName) {
        this.disposedByName = disposedByName;
    }

    public Timestamp getDisposedAt() {
        return disposedAt;
    }

    public void setDisposedAt(Timestamp disposedAt) {
        this.disposedAt = disposedAt;
    }

    public Timestamp getRetentionUntil() {
        return retentionUntil;
    }

    public void setRetentionUntil(Timestamp retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    public String getArchiveLocation() {
        return archiveLocation;
    }

    public void setArchiveLocation(String archiveLocation) {
        this.archiveLocation = archiveLocation;
    }

    public String getWitnessId() {
        return witnessId;
    }

    public void setWitnessId(String witnessId) {
        this.witnessId = witnessId;
    }

    public String getWitnessName() {
        return witnessName;
    }

    public void setWitnessName(String witnessName) {
        this.witnessName = witnessName;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
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
        if (requestedAt == null) {
            requestedAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
