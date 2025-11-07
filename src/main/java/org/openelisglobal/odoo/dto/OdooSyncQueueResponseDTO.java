package org.openelisglobal.odoo.dto;

import java.util.List;

public class OdooSyncQueueResponseDTO {

    private List<OdooSyncQueueEntryDTO> entries;
    private int pendingCount;
    private int failedCount;
    private boolean odooAvailable;
    private String statusMessage;

    public List<OdooSyncQueueEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<OdooSyncQueueEntryDTO> entries) {
        this.entries = entries;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public boolean isOdooAvailable() {
        return odooAvailable;
    }

    public void setOdooAvailable(boolean odooAvailable) {
        this.odooAvailable = odooAvailable;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
