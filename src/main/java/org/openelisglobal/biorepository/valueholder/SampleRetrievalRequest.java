package org.openelisglobal.biorepository.valueholder;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Entity representing a request to retrieve samples from the Biorepository.
 *
 * Supports requests from researchers, lab supervisors, or other authorized
 * users to check out samples for analysis, transfer, or other purposes.
 *
 * Workflow: 1. Requester creates retrieval request with sample items (DRAFT) 2.
 * Requester submits for approval (PENDING_APPROVAL) 3. Supervisor reviews and
 * approves/rejects 4. On approval: Work order generated, samples can be
 * retrieved (APPROVED -> IN_PROGRESS) 5. Samples retrieved and released to
 * requester 6. Samples returned or consumed (COMPLETED)
 */
@Entity
@Table(name = "sample_retrieval_request", schema = "clinlims")
public class SampleRetrievalRequest extends BaseObject<Integer> {

    /**
     * Status of the retrieval request workflow.
     */
    public enum RequestStatus {
        DRAFT, // Saved but not submitted
        PENDING_APPROVAL, // Awaiting supervisor review
        APPROVED, // Approved, work order generated
        IN_PROGRESS, // Retrieval underway
        PARTIALLY_COMPLETED, // Some items retrieved/returned
        COMPLETED, // All items processed
        REJECTED, // Request denied
        CANCELLED // Withdrawn by requester
    }

    /**
     * Priority level for the request.
     */
    public enum PriorityLevel {
        NORMAL, URGENT, CRITICAL
    }

    /**
     * Type of destination for the retrieved samples.
     */
    public enum DestinationType {
        INTERNAL_LAB, // Within same facility
        EXTERNAL_LAB, // Inter-laboratory transfer
        ANALYSIS_RETURN // Checkout for analysis, will return
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_retrieval_request_generator")
    @SequenceGenerator(name = "sample_retrieval_request_generator", sequenceName = "sample_retrieval_request_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotBlank(message = "Request number is required")
    @Size(max = 50)
    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @NotBlank(message = "Request purpose is required")
    @Column(name = "request_purpose", nullable = false, columnDefinition = "TEXT")
    private String requestPurpose;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    @Size(max = 100)
    @Column(name = "ethics_approval_ref", length = 100)
    private String ethicsApprovalRef;

    @NotNull(message = "Request status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RequestStatus status = RequestStatus.DRAFT;

    @NotNull(message = "Priority level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    private PriorityLevel priorityLevel = PriorityLevel.NORMAL;

    @Column(name = "required_by_date")
    private LocalDate requiredByDate;

    @NotNull(message = "Destination type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 30)
    private DestinationType destinationType;

    @Size(max = 255)
    @Column(name = "destination_details", length = 255)
    private String destinationDetails;

    @NotNull(message = "Requested by user is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private SystemUser requestedBy;

    @NotNull(message = "Requested timestamp is required")
    @Column(name = "requested_timestamp", nullable = false)
    private Timestamp requestedTimestamp;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by_user_id")
    private SystemUser approvedBy;

    @Column(name = "approved_timestamp")
    private Timestamp approvedTimestamp;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Size(max = 50)
    @Column(name = "work_order_number", length = 50)
    private String workOrderNumber;

    @OneToMany(mappedBy = "retrievalRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SampleRetrievalItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_entry_id")
    private NotebookEntry notebookEntry;

    @Size(max = 100)
    @Column(name = "requester_lab_unit", length = 100)
    private String requesterLabUnit;

    @Size(max = 150)
    @Column(name = "requestor_name", length = 150)
    private String requestorName;

    @Size(max = 150)
    @Column(name = "principal_investigator", length = 150)
    private String principalInvestigator;

    @Size(max = 255)
    @Column(name = "project_title", length = 255)
    private String projectTitle;

    @Size(max = 255)
    @Column(name = "requester_contact_info", length = 255)
    private String requesterContactInfo;

    @Column(name = "intended_use_description", columnDefinition = "TEXT")
    private String intendedUseDescription;

    @Column(name = "samples_will_be_destroyed")
    private Boolean samplesWillBeDestroyed;

    @Column(name = "estimated_return_date")
    private LocalDate estimatedReturnDate;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    public SampleRetrievalRequest() {
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getRequestPurpose() {
        return requestPurpose;
    }

    public void setRequestPurpose(String requestPurpose) {
        this.requestPurpose = requestPurpose;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getEthicsApprovalRef() {
        return ethicsApprovalRef;
    }

    public void setEthicsApprovalRef(String ethicsApprovalRef) {
        this.ethicsApprovalRef = ethicsApprovalRef;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public LocalDate getRequiredByDate() {
        return requiredByDate;
    }

    public void setRequiredByDate(LocalDate requiredByDate) {
        this.requiredByDate = requiredByDate;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationDetails() {
        return destinationDetails;
    }

    public void setDestinationDetails(String destinationDetails) {
        this.destinationDetails = destinationDetails;
    }

    public SystemUser getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(SystemUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Timestamp getRequestedTimestamp() {
        return requestedTimestamp;
    }

    public void setRequestedTimestamp(Timestamp requestedTimestamp) {
        this.requestedTimestamp = requestedTimestamp;
    }

    public SystemUser getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(SystemUser approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Timestamp getApprovedTimestamp() {
        return approvedTimestamp;
    }

    public void setApprovedTimestamp(Timestamp approvedTimestamp) {
        this.approvedTimestamp = approvedTimestamp;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public List<SampleRetrievalItem> getItems() {
        return items;
    }

    public void setItems(List<SampleRetrievalItem> items) {
        this.items = items;
    }

    /**
     * Add a retrieval item to this request.
     */
    public void addItem(SampleRetrievalItem item) {
        items.add(item);
        item.setRetrievalRequest(this);
    }

    /**
     * Remove a retrieval item from this request.
     */
    public void removeItem(SampleRetrievalItem item) {
        items.remove(item);
        item.setRetrievalRequest(null);
    }

    public NotebookEntry getNotebookEntry() {
        return notebookEntry;
    }

    public void setNotebookEntry(NotebookEntry notebookEntry) {
        this.notebookEntry = notebookEntry;
    }

    public String getRequesterLabUnit() {
        return requesterLabUnit;
    }

    public void setRequesterLabUnit(String requesterLabUnit) {
        this.requesterLabUnit = requesterLabUnit;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public void setRequestorName(String requestorName) {
        this.requestorName = requestorName;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getRequesterContactInfo() {
        return requesterContactInfo;
    }

    public void setRequesterContactInfo(String requesterContactInfo) {
        this.requesterContactInfo = requesterContactInfo;
    }

    public String getIntendedUseDescription() {
        return intendedUseDescription;
    }

    public void setIntendedUseDescription(String intendedUseDescription) {
        this.intendedUseDescription = intendedUseDescription;
    }

    public Boolean getSamplesWillBeDestroyed() {
        return samplesWillBeDestroyed;
    }

    public void setSamplesWillBeDestroyed(Boolean samplesWillBeDestroyed) {
        this.samplesWillBeDestroyed = samplesWillBeDestroyed;
    }

    public LocalDate getEstimatedReturnDate() {
        return estimatedReturnDate;
    }

    public void setEstimatedReturnDate(LocalDate estimatedReturnDate) {
        this.estimatedReturnDate = estimatedReturnDate;
    }

    /**
     * Check if the request is in draft status.
     */
    public boolean isDraft() {
        return RequestStatus.DRAFT.equals(status);
    }

    /**
     * Check if the request is pending approval.
     */
    public boolean isPendingApproval() {
        return RequestStatus.PENDING_APPROVAL.equals(status);
    }

    /**
     * Check if the request has been approved.
     */
    public boolean isApproved() {
        return RequestStatus.APPROVED.equals(status) || RequestStatus.IN_PROGRESS.equals(status)
                || RequestStatus.PARTIALLY_COMPLETED.equals(status) || RequestStatus.COMPLETED.equals(status);
    }

    /**
     * Check if the request can be cancelled.
     */
    public boolean canBeCancelled() {
        return RequestStatus.DRAFT.equals(status) || RequestStatus.PENDING_APPROVAL.equals(status);
    }

    /**
     * Get total item count.
     */
    public int getTotalItemCount() {
        return (int) items.stream().filter(SampleRetrievalItem::isWorkflowItem).count();
    }

    /**
     * Get count of retrieved items.
     */
    public long getRetrievedItemCount() {
        return items.stream().filter(SampleRetrievalItem::isWorkflowItem)
                .filter(item -> item.getStatus() == SampleRetrievalItem.ItemStatus.RETRIEVED
                        || item.getStatus() == SampleRetrievalItem.ItemStatus.IN_ANALYSIS)
                .count();
    }

    /**
     * Get count of returned items.
     */
    public long getReturnedItemCount() {
        return items.stream().filter(SampleRetrievalItem::isWorkflowItem)
                .filter(item -> item.getStatus() == SampleRetrievalItem.ItemStatus.RETURNED
                        || item.getStatus() == SampleRetrievalItem.ItemStatus.PARTIALLY_USED)
                .count();
    }

    /**
     * Get count of consumed items.
     */
    public long getConsumedItemCount() {
        return items.stream().filter(SampleRetrievalItem::isWorkflowItem)
                .filter(item -> item.getStatus() == SampleRetrievalItem.ItemStatus.CONSUMED).count();
    }

    /**
     * Get count of pending items.
     */
    public long getPendingItemCount() {
        return items.stream().filter(SampleRetrievalItem::isWorkflowItem)
                .filter(item -> item.getStatus() == SampleRetrievalItem.ItemStatus.PENDING).count();
    }

    /**
     * Reference lines awaiting Biorepository sample attachment.
     */
    public long getAwaitingFulfillmentItemCount() {
        return items.stream().filter(SampleRetrievalItem::isReferenceLine)
                .filter(SampleRetrievalItem::isAwaitingFulfillmentStatus).count();
    }

    /**
     * Top-level request lines (reference or direct), excluding fulfillment children.
     */
    public int getRequestLineCount() {
        return (int) items.stream().filter(item -> !item.isFulfillmentLine()).count();
    }

    /**
     * Fulfillment child lines attached to reference request rows.
     */
    public long getFulfillmentItemCount() {
        return items.stream().filter(SampleRetrievalItem::isFulfillmentLine).count();
    }

    /**
     * Returns a human-readable reason when the request cannot be marked complete.
     */
    public String getCompletionBlockReason() {
        long pendingCount = getPendingItemCount();
        if (pendingCount > 0) {
            return "Cannot complete request: " + pendingCount + " item(s) have not been retrieved yet.";
        }

        long retrievedNotReleased = items.stream().filter(SampleRetrievalItem::isWorkflowItem)
                .filter(item -> item.getStatus() == SampleRetrievalItem.ItemStatus.RETRIEVED).count();
        if (retrievedNotReleased > 0) {
            return "Cannot complete request: " + retrievedNotReleased + " item(s) have not been released yet.";
        }

        for (SampleRetrievalItem item : items) {
            if (!item.isReferenceLine()) {
                continue;
            }

            long fulfillmentCount = items.stream().filter(SampleRetrievalItem::isFulfillmentLine)
                    .filter(fulfillment -> item.equals(fulfillment.getFulfillsItem())).count();
            if (fulfillmentCount == 0) {
                return "Cannot complete request: reference line(s) still awaiting sample attachment.";
            }
        }

        if (getTotalItemCount() == 0 && getAwaitingFulfillmentItemCount() > 0) {
            return "Cannot complete request: reference line(s) still awaiting sample attachment.";
        }

        long unfinishedFulfillments = items.stream().filter(SampleRetrievalItem::isFulfillmentLine)
                .filter(item -> !isTerminalWorkflowStatus(item.getStatus())).count();
        if (unfinishedFulfillments > 0) {
            return "Cannot complete request: " + unfinishedFulfillments
                    + " fulfilled sample(s) are not released or returned yet.";
        }

        long unfinishedDirectItems = items.stream().filter(SampleRetrievalItem::isDirectItem)
                .filter(item -> !isTerminalWorkflowStatus(item.getStatus())).count();
        if (unfinishedDirectItems > 0) {
            return "Cannot complete request: " + unfinishedDirectItems
                    + " sample(s) are not released or returned yet.";
        }

        return null;
    }

    private boolean isTerminalWorkflowStatus(SampleRetrievalItem.ItemStatus status) {
        return status == SampleRetrievalItem.ItemStatus.IN_ANALYSIS
                || status == SampleRetrievalItem.ItemStatus.RETURNED
                || status == SampleRetrievalItem.ItemStatus.CONSUMED
                || status == SampleRetrievalItem.ItemStatus.PARTIALLY_USED;
    }

    /**
     * Generate a request number in format RR-YYYYMMDD-XXXXX
     */
    public static String generateRequestNumber(int sequenceNumber) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return String.format("RR-%d%02d%02d-%05d", today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                sequenceNumber);
    }
}
