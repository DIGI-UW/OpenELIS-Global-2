package org.openelisglobal.biorepository.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Entity representing a single sample item within a transfer request. Links to
 * an existing SampleItem in the system and tracks individual
 * acceptance/rejection status for partial transfer support.
 *
 * When accepted, a BioSample extension record is created for the SampleItem.
 */
@Entity
@Table(name = "sample_transfer_item", schema = "clinlims")
public class SampleTransferItem extends BaseObject<Integer> {

    /**
     * Status of individual item within the transfer.
     */
    public enum ItemStatus {
        PENDING, // Not yet processed
        ACCEPTED, // Accepted into biorepository
        REJECTED // Rejected with reason
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_transfer_item_generator")
    @SequenceGenerator(name = "sample_transfer_item_generator", sequenceName = "sample_transfer_item_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "Transfer request is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_request_id", nullable = false)
    @JsonIgnore
    private SampleTransferRequest transferRequest;

    @NotNull(message = "Sample item is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @NotNull(message = "Item status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemStatus status = ItemStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bio_sample_id")
    private BioSample bioSample;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    public SampleTransferItem() {
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

    public SampleTransferRequest getTransferRequest() {
        return transferRequest;
    }

    public void setTransferRequest(SampleTransferRequest transferRequest) {
        this.transferRequest = transferRequest;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BioSample getBioSample() {
        return bioSample;
    }

    public void setBioSample(BioSample bioSample) {
        this.bioSample = bioSample;
    }

    /**
     * Check if this item is still pending.
     */
    public boolean isPending() {
        return ItemStatus.PENDING.equals(status);
    }

    /**
     * Check if this item was accepted.
     */
    public boolean isAccepted() {
        return ItemStatus.ACCEPTED.equals(status);
    }

    /**
     * Check if this item was rejected.
     */
    public boolean isRejected() {
        return ItemStatus.REJECTED.equals(status);
    }

    /**
     * Get the transfer request ID for serialization.
     */
    public Integer getTransferRequestId() {
        return transferRequest != null ? transferRequest.getId() : null;
    }

    /**
     * Get the sample item ID for serialization.
     */
    public Integer getSampleItemId() {
        return sampleItem != null ? Integer.valueOf(sampleItem.getId()) : null;
    }
}
