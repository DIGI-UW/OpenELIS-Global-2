package org.openelisglobal.odoo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.odoo.entity.converter.SyncStatusConverter;

@Entity
@Table(name = "odoo_sync_queue")
@Setter
@Getter
public class OdooSyncQueue extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public enum SyncStatus {
        PENDING, PROCESSING, FAILED, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "odoo_sync_queue_generator")
    @SequenceGenerator(name = "odoo_sync_queue_generator", sequenceName = "odoo_sync_queue_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "accession_number")
    @Size(max = 50)
    private String accessionNumber;

    @Column(name = "sample_id")
    @Size(max = 20)
    private String sampleId;

    @Lob
    @Column(name = "invoice_data", columnDefinition = "TEXT")
    @NotNull
    private String invoiceData;

    @Convert(converter = SyncStatusConverter.class)
    @Column(name = "status", length = 20)
    @NotNull
    private SyncStatus status;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 10;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "last_retry_date")
    private Timestamp lastRetryDate;

    @Column(name = "completed_date")
    private Timestamp completedDate;

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

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryDate = new Timestamp(System.currentTimeMillis());
    }

    public boolean hasExceededMaxRetries() {
        return this.retryCount >= this.maxRetries;
    }

    public void markCompleted(Integer invoiceId) {
        this.status = SyncStatus.COMPLETED;
        this.odooInvoiceId = invoiceId;
        this.completedDate = new Timestamp(System.currentTimeMillis());
    }

    public void markFailed(String errorMessage) {
        this.status = SyncStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
