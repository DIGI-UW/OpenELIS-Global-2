package org.openelisglobal.validationpolicy.service;

import java.sql.Timestamp;

/**
 * OGC-1017: the page loaded an older version of the policy than the one stored,
 * so its save would overwrite a change it never showed. Nothing was written.
 */
public class ValidationPolicyStaleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Long policyId;
    private final Timestamp current;

    public ValidationPolicyStaleException(Long policyId, Timestamp current) {
        super("validation policy " + policyId + " changed since the page loaded it");
        this.policyId = policyId;
        this.current = current;
    }

    public Long getPolicyId() {
        return policyId;
    }

    /** The stored version the page did not load. */
    public Timestamp getCurrent() {
        return current;
    }
}
