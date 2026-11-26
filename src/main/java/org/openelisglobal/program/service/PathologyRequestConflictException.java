package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;

/**
 * A status change was asked of a request that no longer stands where the page
 * said it did: another user changed it first, so the page that sent the change
 * is out of date and nothing is written.
 */
public class PathologyRequestConflictException extends RuntimeException {

    private final RequestStatus storedStatus;

    public PathologyRequestConflictException(Integer requestId, RequestStatus storedStatus,
            RequestStatus expectedStatus) {
        super("request " + requestId + " is " + storedStatus + ", not " + expectedStatus);
        this.storedStatus = storedStatus;
    }

    /** The status the request actually holds. */
    public RequestStatus getStoredStatus() {
        return storedStatus;
    }
}
