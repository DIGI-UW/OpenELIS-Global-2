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
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Entity representing a request to transfer samples from an internal lab to the
 * Biorepository.
 *
 * Supports bulk transfers with multiple SampleTransferItems. Each item
 * references an existing SampleItem in the system.
 *
 * The sourceLab field is a free-form string allowing any lab to identify itself
 * when posting samples. Access control for which labs can post can be added in
 * the future.
 *
 * Workflow: 1. Origin lab creates transfer request with sample items 2. Request
 * queued with PENDING status 3. Biorepository staff reviews and accepts/rejects
 * 4. On accept: BioSample extension created for each item 5. On reject:
 * Rejection reason recorded
 */
@Entity
@Table(name = "sample_transfer_request", schema = "clinlims")
public class SampleTransferRequest extends BaseObject<Integer> {

    /**
     * Status of the transfer request.
     */
    public enum TransferStatus {
        PENDING, // Waiting in queue for biorepository review
        ACCEPTED, // All items accepted, BioSamples created
        PARTIALLY_ACCEPTED, // Some items accepted, some rejected
        REJECTED, // All items rejected
        CANCELLED // Withdrawn by origin lab
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_transfer_request_generator")
    @SequenceGenerator(name = "sample_transfer_request_generator", sequenceName = "sample_transfer_request_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotBlank(message = "Source lab is required")
    @Size(max = 255)
    @Column(name = "source_lab", nullable = false, length = 255)
    private String sourceLab;

    @NotBlank(message = "Destination lab is required")
    @Size(max = 50)
    @Column(name = "destination_lab", nullable = false, length = 50)
    private String destinationLab = "BIOREPOSITORY";

    @NotNull(message = "Transfer status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransferStatus status = TransferStatus.PENDING;

    @NotNull(message = "Requested by user is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private SystemUser requestedBy;

    @NotNull(message = "Requested timestamp is required")
    @Column(name = "requested_timestamp", nullable = false)
    private Timestamp requestedTimestamp;

    @Column(name = "request_notes", columnDefinition = "TEXT")
    private String requestNotes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "processed_by_user_id")
    private SystemUser processedBy;

    @Column(name = "processed_timestamp")
    private Timestamp processedTimestamp;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "transferRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SampleTransferItem> items = new ArrayList<>();

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    public SampleTransferRequest() {
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

    public String getSourceLab() {
        return sourceLab;
    }

    public void setSourceLab(String sourceLab) {
        this.sourceLab = sourceLab;
    }

    public String getDestinationLab() {
        return destinationLab;
    }

    public void setDestinationLab(String destinationLab) {
        this.destinationLab = destinationLab;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
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

    public String getRequestNotes() {
        return requestNotes;
    }

    public void setRequestNotes(String requestNotes) {
        this.requestNotes = requestNotes;
    }

    public SystemUser getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(SystemUser processedBy) {
        this.processedBy = processedBy;
    }

    public Timestamp getProcessedTimestamp() {
        return processedTimestamp;
    }

    public void setProcessedTimestamp(Timestamp processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<SampleTransferItem> getItems() {
        return items;
    }

    public void setItems(List<SampleTransferItem> items) {
        this.items = items;
    }

    /**
     * Add a transfer item to this request.
     */
    public void addItem(SampleTransferItem item) {
        items.add(item);
        item.setTransferRequest(this);
    }

    /**
     * Remove a transfer item from this request.
     */
    public void removeItem(SampleTransferItem item) {
        items.remove(item);
        item.setTransferRequest(null);
    }

    /**
     * Check if the transfer request is still pending.
     */
    public boolean isPending() {
        return TransferStatus.PENDING.equals(status);
    }

    /**
     * Check if the transfer request has been processed.
     */
    public boolean isProcessed() {
        return !isPending() && !TransferStatus.CANCELLED.equals(status);
    }

    /**
     * Get total item count.
     */
    public int getTotalItemCount() {
        return items.size();
    }

    /**
     * Get count of accepted items.
     */
    public long getAcceptedItemCount() {
        return items.stream().filter(item -> SampleTransferItem.ItemStatus.ACCEPTED.equals(item.getStatus())).count();
    }

    /**
     * Get count of rejected items.
     */
    public long getRejectedItemCount() {
        return items.stream().filter(item -> SampleTransferItem.ItemStatus.REJECTED.equals(item.getStatus())).count();
    }
}
