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
 * DeviationCAPA entity - Records OOS/deviations with root cause analysis and CAPA actions.
 * Links to assay runs and supports workflow tracking.
 */
@Entity
@Table(name = "PHARMA_DEVIATION_CAPA")
@DynamicUpdate
public class DeviationCAPA extends BaseObject<Integer> {

    public enum DeviationType {
        OOS, DEVIATION, INCIDENT, COMPLAINT
    }

    public enum CAPAStatus {
        OPEN, INVESTIGATION, PENDING_APPROVAL, APPROVED, CLOSED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_dev_capa_seq")
    @SequenceGenerator(name = "pharma_dev_capa_seq", sequenceName = "pharma_dev_capa_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ASSAY_RUN_ID")
    private Integer assayRunId;

    @Column(name = "SAMPLE_ID")
    private Integer sampleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "DEVIATION_TYPE", length = 20, nullable = false)
    private DeviationType deviationType;

    @Column(name = "DEVIATION_NUMBER", length = 50, unique = true)
    private String deviationNumber;

    @Column(name = "DESCRIPTION", length = 2000, nullable = false)
    private String description;

    @Column(name = "ROOT_CAUSE", length = 2000)
    private String rootCause;

    @Column(name = "CORRECTIVE_ACTION", length = 2000)
    private String correctiveAction;

    @Column(name = "PREVENTIVE_ACTION", length = 2000)
    private String preventiveAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private CAPAStatus status = CAPAStatus.OPEN;

    @Column(name = "OWNER_ID", length = 36)
    private String ownerId;

    @Column(name = "OWNER_NAME", length = 255)
    private String ownerName;

    @Column(name = "CREATED_AT")
    private Timestamp createdAt;

    @Column(name = "INVESTIGATION_START")
    private Timestamp investigationStart;

    @Column(name = "CLOSED_AT")
    private Timestamp closedAt;

    @Column(name = "DUE_DATE")
    private Timestamp dueDate;

    @Column(name = "APPROVED_BY_ID", length = 36)
    private String approvedById;

    @Column(name = "APPROVED_BY_NAME", length = 255)
    private String approvedByName;

    @Column(name = "APPROVED_AT")
    private Timestamp approvedAt;

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

    public Integer getAssayRunId() {
        return assayRunId;
    }

    public void setAssayRunId(Integer assayRunId) {
        this.assayRunId = assayRunId;
    }

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public DeviationType getDeviationType() {
        return deviationType;
    }

    public void setDeviationType(DeviationType deviationType) {
        this.deviationType = deviationType;
    }

    public String getDeviationNumber() {
        return deviationNumber;
    }

    public void setDeviationNumber(String deviationNumber) {
        this.deviationNumber = deviationNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public String getPreventiveAction() {
        return preventiveAction;
    }

    public void setPreventiveAction(String preventiveAction) {
        this.preventiveAction = preventiveAction;
    }

    public CAPAStatus getStatus() {
        return status;
    }

    public void setStatus(CAPAStatus status) {
        this.status = status;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getInvestigationStart() {
        return investigationStart;
    }

    public void setInvestigationStart(Timestamp investigationStart) {
        this.investigationStart = investigationStart;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
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
