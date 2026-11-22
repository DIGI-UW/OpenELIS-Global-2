package org.openelisglobal.batchworkplan.form;

import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;

public class BatchWorkplanStatusRequest {

    private BatchWorkplanStatus status;

    public BatchWorkplanStatus getStatus() {
        return status;
    }

    public void setStatus(BatchWorkplanStatus status) {
        this.status = status;
    }
}
