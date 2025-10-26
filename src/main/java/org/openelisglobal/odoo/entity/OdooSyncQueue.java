package org.openelisglobal.odoo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Entity class for storing failed Odoo synchronization attempts. This allows
 * for automatic retry and recovery when Odoo is temporarily unavailable.
 */
@Entity
@Table(name = "odoo_sync_queue")
public class OdooSyncQueue extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public enum SyncStatus {
        PENDING, // Waiting to be processed
        PROCESSING, // Currently being processed
        FAILED, // Failed after max retries
        COMPLETED // Successfully synced
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "odoo_sync_queue_generator")
    @SequenceGenerator(name = "odoo_sync_queue_generator", sequenceName = "odoo_sync_queue_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    /**
     * Accession number from the sample (for tracking and debugging)
     */
    @Column(name = "accession_number")
    @Size(max = 50)
    private String accessionNumber;

    /**
     * Sample ID reference
     */
    @Column(name = "sample_id")
    @Size(max = 20)
    private String sampleId;

    /**
     * Serialized invoice data (JSON format) to retry with
     */
    @Lob
    @Column(name = "invoice_data", columnDefinition = "TEXT")
    @NotNull
    private String invoiceData;

    /**
     * Current status of this sync attempt
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @NotNull
    private SyncStatus status;

    /**
     * Number of retry attempts made
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * Maximum number of retries allowed
     */
    @Column(name = "max_retries")
    private Integer maxRetries = 10;

    /**
     * Error message from last failed attempt
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * When this record was created
     */
    @Column(name = "created_date")
    private Timestamp createdDate;

    /**
     * When the last retry attempt was made
     */
    @Column(name = "last_retry_date")
    private Timestamp lastRetryDate;

    /**
     * When the sync was successfully completed
     */
    @Column(name = "completed_date")
    private Timestamp completedDate;

    /**
     * Odoo invoice ID (populated when sync succeeds)
     */
    @Column(name = "odoo_invoice_id")
    private Integer odooInvoiceId;

    public OdooSyncQueue() {
        super();
        this.status = SyncStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 10;
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getInvoiceData() {
        return invoiceData;
    }

    public void setInvoiceData(String invoiceData) {
        this.invoiceData = invoiceData;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getLastRetryDate() {
        return lastRetryDate;
    }

    public void setLastRetryDate(Timestamp lastRetryDate) {
        this.lastRetryDate = lastRetryDate;
    }

    public Timestamp getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Timestamp completedDate) {
        this.completedDate = completedDate;
    }

    public Integer getOdooInvoiceId() {
        return odooInvoiceId;
    }

    public void setOdooInvoiceId(Integer odooInvoiceId) {
        this.odooInvoiceId = odooInvoiceId;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Check if max retries have been exceeded
     */
    public boolean hasExceededMaxRetries() {
        return this.retryCount >= this.maxRetries;
    }

    /**
     * Mark as completed
     */
    public void markCompleted(Integer invoiceId) {
        this.status = SyncStatus.COMPLETED;
        this.odooInvoiceId = invoiceId;
        this.completedDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Mark as failed
     */
    public void markFailed(String errorMessage) {
        this.status = SyncStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
