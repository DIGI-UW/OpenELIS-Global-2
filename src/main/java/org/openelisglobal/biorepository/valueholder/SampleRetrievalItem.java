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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Entity representing a single sample item within a retrieval request. Links to
 * an existing BioSample in the biorepository and tracks individual
 * retrieval/return status.
 *
 * Tracks the full custody lifecycle: - PENDING: Request submitted, awaiting
 * retrieval - RETRIEVED: Physically removed from storage - IN_ANALYSIS: In use
 * by requester - RETURNED: Back in storage - PARTIALLY_USED: Returned with
 * reduced quantity - CONSUMED: Fully used, not returning - UNAVAILABLE: Sample
 * not available (damaged, missing, etc.)
 */
@Entity
@Table(name = "sample_retrieval_item", schema = "clinlims")
public class SampleRetrievalItem extends BaseObject<Integer> {

    /**
     * Status of individual item within the retrieval.
     */
    public enum ItemStatus {
        PENDING, // Awaiting retrieval
        RETRIEVED, // Checked out from storage
        IN_ANALYSIS, // Being used by requester
        RETURNED, // Back in storage
        PARTIALLY_USED, // Returned with reduced quantity
        CONSUMED, // Fully used, not returning
        UNAVAILABLE // Sample not available (damaged, missing)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_retrieval_item_generator")
    @SequenceGenerator(name = "sample_retrieval_item_generator", sequenceName = "sample_retrieval_item_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "Retrieval request is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrieval_request_id", nullable = false)
    @JsonIgnore
    private SampleRetrievalRequest retrievalRequest;

    @NotNull(message = "BioSample is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bio_sample_id", nullable = false)
    private BioSample bioSample;

    @Column(name = "quantity_requested", precision = 10, scale = 4)
    private BigDecimal quantityRequested;

    @Size(max = 20)
    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @NotNull(message = "Item status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 30)
    private ItemStatus status = ItemStatus.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "retrieved_by_user_id")
    private SystemUser retrievedBy;

    @Column(name = "retrieved_timestamp")
    private Timestamp retrievedTimestamp;

    @Size(max = 50)
    @Column(name = "condition_at_release", length = 50)
    private String conditionAtRelease;

    @Column(name = "condition_notes", columnDefinition = "TEXT")
    private String conditionNotes;

    @NotNull
    @Column(name = "return_expected", nullable = false)
    private Boolean returnExpected = true;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "returned_timestamp")
    private Timestamp returnedTimestamp;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "returned_by_user_id")
    private SystemUser returnedBy;

    @Size(max = 50)
    @Column(name = "returned_condition", length = 50)
    private String returnedCondition;

    @Column(name = "return_notes", columnDefinition = "TEXT")
    private String returnNotes;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    public SampleRetrievalItem() {
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

    public SampleRetrievalRequest getRetrievalRequest() {
        return retrievalRequest;
    }

    public void setRetrievalRequest(SampleRetrievalRequest retrievalRequest) {
        this.retrievalRequest = retrievalRequest;
    }

    public BioSample getBioSample() {
        return bioSample;
    }

    public void setBioSample(BioSample bioSample) {
        this.bioSample = bioSample;
    }

    public BigDecimal getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(BigDecimal quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public SystemUser getRetrievedBy() {
        return retrievedBy;
    }

    public void setRetrievedBy(SystemUser retrievedBy) {
        this.retrievedBy = retrievedBy;
    }

    public Timestamp getRetrievedTimestamp() {
        return retrievedTimestamp;
    }

    public void setRetrievedTimestamp(Timestamp retrievedTimestamp) {
        this.retrievedTimestamp = retrievedTimestamp;
    }

    public String getConditionAtRelease() {
        return conditionAtRelease;
    }

    public void setConditionAtRelease(String conditionAtRelease) {
        this.conditionAtRelease = conditionAtRelease;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public Boolean getReturnExpected() {
        return returnExpected;
    }

    public void setReturnExpected(Boolean returnExpected) {
        this.returnExpected = returnExpected;
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public Timestamp getReturnedTimestamp() {
        return returnedTimestamp;
    }

    public void setReturnedTimestamp(Timestamp returnedTimestamp) {
        this.returnedTimestamp = returnedTimestamp;
    }

    public SystemUser getReturnedBy() {
        return returnedBy;
    }

    public void setReturnedBy(SystemUser returnedBy) {
        this.returnedBy = returnedBy;
    }

    public String getReturnedCondition() {
        return returnedCondition;
    }

    public void setReturnedCondition(String returnedCondition) {
        this.returnedCondition = returnedCondition;
    }

    public String getReturnNotes() {
        return returnNotes;
    }

    public void setReturnNotes(String returnNotes) {
        this.returnNotes = returnNotes;
    }

    /**
     * Check if this item is still pending retrieval.
     */
    public boolean isPending() {
        return ItemStatus.PENDING.equals(status);
    }

    /**
     * Check if this item has been retrieved (checked out).
     */
    public boolean isRetrieved() {
        return ItemStatus.RETRIEVED.equals(status) || ItemStatus.IN_ANALYSIS.equals(status);
    }

    /**
     * Check if this item has been returned.
     */
    public boolean isReturned() {
        return ItemStatus.RETURNED.equals(status) || ItemStatus.PARTIALLY_USED.equals(status);
    }

    /**
     * Check if this item was consumed (not returning).
     */
    public boolean isConsumed() {
        return ItemStatus.CONSUMED.equals(status);
    }

    /**
     * Check if this item is currently checked out (not yet returned).
     */
    public boolean isCheckedOut() {
        return ItemStatus.RETRIEVED.equals(status) || ItemStatus.IN_ANALYSIS.equals(status);
    }

    /**
     * Check if this item's lifecycle is complete.
     */
    public boolean isComplete() {
        return ItemStatus.RETURNED.equals(status) || ItemStatus.PARTIALLY_USED.equals(status)
                || ItemStatus.CONSUMED.equals(status) || ItemStatus.UNAVAILABLE.equals(status);
    }

    /**
     * Check if this item is overdue for return.
     */
    public boolean isOverdue() {
        if (!returnExpected || expectedReturnDate == null) {
            return false;
        }
        return isCheckedOut() && LocalDate.now().isAfter(expectedReturnDate);
    }

    /**
     * Get the retrieval request ID for serialization.
     */
    public Integer getRetrievalRequestId() {
        return retrievalRequest != null ? retrievalRequest.getId() : null;
    }

    /**
     * Get the BioSample ID for serialization.
     */
    public Integer getBioSampleId() {
        return bioSample != null ? bioSample.getId() : null;
    }
}
