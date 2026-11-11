package org.openelisglobal.batchworkplan.service;

/**
 * No batch with this id belongs to the caller. A batch someone else built is
 * answered the same way as one that does not exist, so ids cannot be probed.
 */
public class BatchWorkplanNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BatchWorkplanNotFoundException(Long id) {
        super("Batch workplan not found: " + id);
    }
}
