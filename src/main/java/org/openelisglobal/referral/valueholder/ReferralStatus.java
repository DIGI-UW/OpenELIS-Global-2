package org.openelisglobal.referral.valueholder;

public enum ReferralStatus {
    DRAFT, REQUESTED, RECEIVED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED,

    /**
     * @deprecated Use {@link #DRAFT}. Slated for removal once legacy referral flow
     *             migrates.
     */
    @Deprecated
    CREATED,
    /**
     * @deprecated Use {@link #REQUESTED}. Slated for removal once legacy referral
     *             flow migrates.
     */
    @Deprecated
    SENT,
    /**
     * @deprecated Use {@link #COMPLETED}. Slated for removal once legacy referral
     *             flow migrates.
     */
    @Deprecated
    FINISHED,
    /**
     * @deprecated Use {@link #CANCELLED}. Backfilled and no remaining writers; kept
     *             one release for deserialization safety.
     */
    @Deprecated
    CANCELED;

    /**
     * Legal forward transitions for the FHIR-aligned subcontract lifecycle.
     * Terminal states (COMPLETED, REJECTED, CANCELLED) have no outgoing edges.
     * Legacy values (CREATED, SENT, FINISHED, CANCELED) are set directly by legacy
     * code paths that never call the transition guard.
     *
     * <p>
     * {@code REQUESTED -> COMPLETED} is allowed (skipping the optional intermediate
     * RECEIVED/IN_PROGRESS states) per OGC-799 FR-OUTSTANDING-005: when results are
     * entered manually for a referral whose peer never sent an explicit RECEIVED
     * ack via FHIR (e.g. phone/fax results from a non-OpenELIS reference lab), the
     * hook advances the local state directly to COMPLETED. The FRS calls
     * draft→requested→received→in-progress→completed the "canonical progression" —
     * i.e. the normal path, not the only path.
     *
     * <p>
     * {@code COMPLETED -> REJECTED} is the one terminal-state exception (OGC-804):
     * a returned result the lab declines to reconcile is rejected from the Returned
     * view, closing the originating Analysis and prompting re-collection. Every
     * other terminal state stays a dead end.
     */
    public boolean canTransitionTo(ReferralStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
        case DRAFT -> target == REQUESTED || target == CANCELLED;
        case REQUESTED -> target == RECEIVED || target == COMPLETED || target == REJECTED || target == CANCELLED;
        case RECEIVED -> target == COMPLETED || target == REJECTED || target == CANCELLED;
        case IN_PROGRESS -> target == COMPLETED || target == REJECTED || target == CANCELLED;
        case COMPLETED -> target == REJECTED;
        case REJECTED, CANCELLED -> false;
        default -> false;
        };
    }

    /**
     * Terminal states have no outgoing lifecycle edges (legacy FINISHED/CANCELED
     * included).
     */
    public boolean isTerminal() {
        return switch (this) {
        case COMPLETED, REJECTED, CANCELLED, FINISHED, CANCELED -> true;
        default -> false;
        };
    }
}
