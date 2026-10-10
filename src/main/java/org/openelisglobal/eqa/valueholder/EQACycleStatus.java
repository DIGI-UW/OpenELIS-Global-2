package org.openelisglobal.eqa.valueholder;

/**
 * Union of the participant and provider cycle state machines — both write the
 * single eqa_cycle.status column; which machine applies depends on whether this
 * lab is the scheme's provider. Transition legality is enforced at the service
 * layer, not by this enum.
 *
 * <p>
 * Participant: PLANNED → PANEL_RECEIVED → TESTING → READY_TO_SUBMIT → SUBMITTED
 * → SCORED → CLOSED.
 *
 * <p>
 * Provider: PLANNED → PREP_IN_PROGRESS → READY_TO_SHIP → SHIPPED → DELIVERED →
 * SUBMISSIONS_OPEN → SUBMISSIONS_CLOSED → SCORING → SCORED → CLOSED.
 */
public enum EQACycleStatus {
    PLANNED, PANEL_RECEIVED, TESTING, READY_TO_SUBMIT, SUBMITTED, PREP_IN_PROGRESS, READY_TO_SHIP, SHIPPED, DELIVERED,
    SUBMISSIONS_OPEN, SUBMISSIONS_CLOSED, SCORING, SCORED, CLOSED
}
