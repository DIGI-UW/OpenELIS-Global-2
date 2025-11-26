package org.openelisglobal.alert.valueholder;

public enum AlertType {
    /**
     * Freezer temperature threshold violations (critical high/low temperatures)
     */
    FREEZER_TEMPERATURE,

    /**
     * Equipment malfunction or failure alerts
     */
    EQUIPMENT_FAILURE,

    /**
     * Low inventory level alerts
     */
    INVENTORY_LOW,

    /**
     * Sample tracking and status alerts
     */
    SAMPLE_TRACKING,

    /**
     * Other or custom alert types
     */
    OTHER
}
