package org.openelisglobal.shipment.valueholder;

/**
 * Represents the lifecycle states of a shipping box
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

    public static BoxState fromString(String text) {
        for (BoxState state : BoxState.values()) {
            if (state.name().equalsIgnoreCase(text) || state.displayName.equalsIgnoreCase(text)) {
                return state;
            }
        }
        throw new IllegalArgumentException("No BoxState constant with text " + text + " found");
    }
}
