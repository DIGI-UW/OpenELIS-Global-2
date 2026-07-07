package org.openelisglobal.sampleacceptance.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Thrown by {@link SampleAcceptanceRecordService#enforceAcceptanceGate} when a
 * sample cannot proceed because its domain's acceptance checklist is MANDATORY
 * and not yet satisfied (FR-08). A distinct type so callers (e.g. the REST gate
 * endpoint) can map it to a 409 without conflating it with other failures.
 */
public class SampleAcceptanceBlockedException extends LIMSRuntimeException {

    private static final long serialVersionUID = 1L;

    public SampleAcceptanceBlockedException(String message) {
        super(message);
    }
}
