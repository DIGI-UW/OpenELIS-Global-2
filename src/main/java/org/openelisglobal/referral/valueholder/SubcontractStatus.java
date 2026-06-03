package org.openelisglobal.referral.valueholder;

/**
 * S-14 / OGC-624 FR-02 inter-lab transfer lifecycle. Lives on
 * ReferralSubcontract; parallel to (and distinct from) {@link ReferralStatus},
 * which tracks the existing per-referral clinical/result lifecycle.
 *
 * <p>
 * Transitions are strict-linear: each state advances only to its immediate
 * successor. CLOSED is terminal. Cancellation is represented on the parent
 * {@link ReferralStatus} (CANCELED), not in this enum.
 */
public enum SubcontractStatus {
    DRAFT, DISPATCHED, RECEIVED, RESULTS_RETURNED, CLOSED;

    /**
     * Single source of truth for legal forward transitions per S-14 FR-02. Both
     * manual operator actions and FHIR auto-triggers consult this.
     */
    public boolean canTransitionTo(SubcontractStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
        case DRAFT -> target == DISPATCHED;
        case DISPATCHED -> target == RECEIVED;
        case RECEIVED -> target == RESULTS_RETURNED;
        case RESULTS_RETURNED -> target == CLOSED;
        case CLOSED -> false;
        };
    }
}
