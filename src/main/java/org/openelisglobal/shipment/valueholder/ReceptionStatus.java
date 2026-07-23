package org.openelisglobal.shipment.valueholder;

/**
 * Represents the reception status of a sample in a box
 */
public enum ReceptionStatus {
    PENDING("Pending"), RECEIVED_GOOD("Received - Good Condition"), RECEIVED_DAMAGED("Received - Damaged"),
    RECEIVED_LEAKED("Received - Leaked"), MISSING("Missing"), REJECTED("Rejected");

    private final String displayName;

    ReceptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReceptionStatus fromString(String text) {
        for (ReceptionStatus status : ReceptionStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No ReceptionStatus constant with text " + text + " found");
    }
}
