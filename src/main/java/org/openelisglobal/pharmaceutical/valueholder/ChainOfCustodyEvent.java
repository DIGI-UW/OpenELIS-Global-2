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
 * ChainOfCustodyEvent entity - Logs retrieval, shipping, and transfer events.
 * Tracks handler, condition, timestamp, and approvals for audit compliance.
 */
@Entity
@Table(name = "PHARMA_CHAIN_OF_CUSTODY")
@DynamicUpdate
public class ChainOfCustodyEvent extends BaseObject<Integer> {

    public enum CustodyAction {
        RETRIEVAL, SHIPMENT, TRANSFER, RETURN, DISPOSAL, STORAGE_MOVE
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_coc_seq")
    @SequenceGenerator(name = "pharma_coc_seq", sequenceName = "pharma_coc_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "SAMPLE_ID")
    private Integer sampleId;

    @Column(name = "ALIQUOT_ID")
    private Integer aliquotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION", length = 20, nullable = false)
    private CustodyAction action;

    @Column(name = "HANDLER_ID", length = 36, nullable = false)
    private String handlerId;

    @Column(name = "HANDLER_NAME", length = 255)
    private String handlerName;

    @Column(name = "CONDITION_DESCRIPTION", length = 500)
    private String conditionDescription;

    @Column(name = "TEMPERATURE", length = 50)
    private String temperature;

    @Column(name = "EVENT_TIMESTAMP", nullable = false)
    private Timestamp eventTimestamp;

    @Column(name = "FROM_LOCATION", length = 255)
    private String fromLocation;

    @Column(name = "TO_LOCATION", length = 255)
    private String toLocation;

    @Column(name = "RECIPIENT_NAME", length = 255)
    private String recipientName;

    @Column(name = "RECIPIENT_ORG", length = 255)
    private String recipientOrganization;

    @Enumerated(EnumType.STRING)
    @Column(name = "APPROVAL_STATUS", length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "APPROVED_BY_ID", length = 36)
    private String approvedById;

    @Column(name = "APPROVED_BY_NAME", length = 255)
    private String approvedByName;

    @Column(name = "APPROVED_AT")
    private Timestamp approvedAt;

    @Column(name = "MTA_REFERENCE", length = 100)
    private String mtaReference;

    @Column(name = "SDS_ATTACHED")
    private Boolean sdsAttached;

    @Column(name = "SHIPPING_CHECKLIST", length = 2000)
    private String shippingChecklist;

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

    public CustodyAction getAction() {
        return action;
    }

    public void setAction(CustodyAction action) {
        this.action = action;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getConditionDescription() {
        return conditionDescription;
    }

    public void setConditionDescription(String conditionDescription) {
        this.conditionDescription = conditionDescription;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public Timestamp getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Timestamp eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientOrganization() {
        return recipientOrganization;
    }

    public void setRecipientOrganization(String recipientOrganization) {
        this.recipientOrganization = recipientOrganization;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
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

    public String getMtaReference() {
        return mtaReference;
    }

    public void setMtaReference(String mtaReference) {
        this.mtaReference = mtaReference;
    }

    public Boolean getSdsAttached() {
        return sdsAttached;
    }

    public void setSdsAttached(Boolean sdsAttached) {
        this.sdsAttached = sdsAttached;
    }

    public String getShippingChecklist() {
        return shippingChecklist;
    }

    public void setShippingChecklist(String shippingChecklist) {
        this.shippingChecklist = shippingChecklist;
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
        if (eventTimestamp == null) {
            eventTimestamp = new Timestamp(System.currentTimeMillis());
        }
    }
}
