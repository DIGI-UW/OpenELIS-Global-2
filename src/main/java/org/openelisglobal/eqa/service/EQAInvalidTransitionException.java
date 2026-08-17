package org.openelisglobal.eqa.service;

import org.openelisglobal.eqa.valueholder.EQACycleStatus;

/**
 * An attempted cycle transition is not an edge in the state machine (FR-V2.1-04
 * / FR-V2.1-18). Carries both ends so the API can echo them back rather than
 * making the caller guess what was refused.
 */
public class EQAInvalidTransitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final EQACycleStatus priorState;
    private final EQACycleStatus attemptedState;

    public EQAInvalidTransitionException(EQACycleStatus priorState, EQACycleStatus attemptedState, String detail) {
        super(detail);
        this.priorState = priorState;
        this.attemptedState = attemptedState;
    }

    public EQACycleStatus getPriorState() {
        return priorState;
    }

    public EQACycleStatus getAttemptedState() {
        return attemptedState;
    }
}
