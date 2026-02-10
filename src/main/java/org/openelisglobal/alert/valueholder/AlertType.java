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
    OTHER,

    /**
     * EQA sample approaching or past deadline
     */
    EQA_DEADLINE,

    /**
     * Any sample nearing expiration date
     */
    SAMPLE_EXPIRATION,

    /**
     * STAT order approaching target time
     */
    STAT_UPCOMING,

    /**
     * STAT order exceeded target time
     */
    STAT_OVERDUE,

    /**
     * Critical alert unacknowledged for more than 4 hours
     */
    CRITICAL_UNACKNOWLEDGED
}
