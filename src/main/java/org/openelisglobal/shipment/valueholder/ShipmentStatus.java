package org.openelisglobal.shipment.valueholder;

/**
 * Represents the status of a shipment in transit
 */
public enum ShipmentStatus {
    PENDING("Pending"), IN_TRANSIT("In Transit"), DELIVERED("Delivered"), RETURNED("Returned"), LOST("Lost"),
    CANCELLED("Cancelled");

    private final String displayName;

    ShipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ShipmentStatus fromString(String text) {
        for (ShipmentStatus status : ShipmentStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No ShipmentStatus constant with text " + text + " found");
    }
}
