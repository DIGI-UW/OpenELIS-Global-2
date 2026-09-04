package org.openelisglobal.alert.valueholder;

public enum AlertType {
    /**
     * Freezer temperature threshold violations (critical high/low temperatures)
     */
    FREEZER_TEMPERATURE,

    /**
     * Freezer/fridge Modbus device stopped responding to polling (dead-man's
     * switch, fires independently of any temperature threshold breach)
     */
    FREEZER_OFFLINE,

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
    CRITICAL_UNACKNOWLEDGED,

    /**
     * Order required-by deadline approaching or past
     */
    REQUIRED_BY_DEADLINE,

    /**
     * Reference lab returned a Critical/Abnormal result awaiting acceptance
     * (OGC-803)
     */
    REFERRAL_CRITICAL_RESULT,

    /**
     * Reference lab referral was rejected — lab-side acknowledgment (OGC-804)
     */
    REFERRAL_REJECTED,

    /**
     * A saved result outside its authored critical bounds (OGC-1022 R3); entity is
     * the ANALYSIS carrying the value
     */
    CRITICAL_RESULT
}
