package org.openelisglobal.odoo.dto;

import java.sql.Timestamp;
import org.openelisglobal.odoo.entity.OdooSyncQueue;

public class OdooSyncQueueEntryDTO {

    private Long id;
    private String accessionNumber;
    private String status;
    private Integer retryCount;
    private Integer maxRetries;
    private Timestamp createdDate;
    private Timestamp lastRetryDate;
    private Timestamp completedDate;
    private Integer odooInvoiceId;
    private String errorMessage;

    public static OdooSyncQueueEntryDTO fromEntity(OdooSyncQueue queueEntry) {
        OdooSyncQueueEntryDTO dto = new OdooSyncQueueEntryDTO();
        dto.setId(queueEntry.getId());
        dto.setAccessionNumber(queueEntry.getAccessionNumber());
        dto.setStatus(queueEntry.getStatus() != null ? queueEntry.getStatus().name() : null);
        dto.setRetryCount(queueEntry.getRetryCount());
        dto.setMaxRetries(queueEntry.getMaxRetries());
        dto.setCreatedDate(queueEntry.getCreatedDate());
        dto.setLastRetryDate(queueEntry.getLastRetryDate());
        dto.setCompletedDate(queueEntry.getCompletedDate());
        dto.setOdooInvoiceId(queueEntry.getOdooInvoiceId());
        dto.setErrorMessage(queueEntry.getErrorMessage());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
