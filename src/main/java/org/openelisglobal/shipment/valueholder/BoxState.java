package org.openelisglobal.shipment.valueholder;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the lifecycle states of a shipping box with valid transition
 * rules.
 *
 * Normal flow: DRAFT → READY_TO_SEND → SENT → IN_TRANSIT → RECEIVED →
 * RECONCILED
 *
 * Special transitions: any state → CANCELLED or LOST_IN_TRANSIT, RECEIVED can
 * come from SENT or IN_TRANSIT.
 */
public enum BoxState {
    DRAFT("Draft"), READY_TO_SEND("Ready to Send"), SENT("Sent"), IN_TRANSIT("In Transit"),
    PARTIALLY_RECEIVED("Partially Received"), RECEIVED("Received"), RECONCILED("Reconciled"), CANCELLED("Cancelled"),
    LOST_IN_TRANSIT("Lost in Transit");

    private final String displayName;

    BoxState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the set of states that this state can transition TO.
     */
    public Set<BoxState> getAllowedTransitions() {
        switch (this) {
        case DRAFT:
            return EnumSet.of(READY_TO_SEND, CANCELLED);
        case READY_TO_SEND:
            return EnumSet.of(SENT, DRAFT, CANCELLED);
        case SENT:
            return EnumSet.of(IN_TRANSIT, RECEIVED, PARTIALLY_RECEIVED, CANCELLED, LOST_IN_TRANSIT);
        case IN_TRANSIT:
            return EnumSet.of(RECEIVED, PARTIALLY_RECEIVED, CANCELLED, LOST_IN_TRANSIT);
        case PARTIALLY_RECEIVED:
            return EnumSet.of(RECEIVED, CANCELLED);
        case RECEIVED:
            return EnumSet.of(RECONCILED);
        case RECONCILED:
        case CANCELLED:
        case LOST_IN_TRANSIT:
            return EnumSet.noneOf(BoxState.class); // terminal states
        default:
            return EnumSet.noneOf(BoxState.class);
        }
    }

    /**
     * Check if this state can transition to the given target state.
     */
    public boolean canTransitionTo(BoxState target) {
        return getAllowedTransitions().contains(target);
    }

    public static BoxState fromString(String text) {
        for (BoxState state : BoxState.values()) {
            if (state.name().equalsIgnoreCase(text) || state.displayName.equalsIgnoreCase(text)) {
                return state;
            }
        }
        throw new IllegalArgumentException("No BoxState constant with text " + text + " found");
    }
}
