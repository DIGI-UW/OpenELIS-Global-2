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
    CRITICAL_UNACKNOWLEDGED,

    /**
     * A saved result outside its authored critical bounds (OGC-1022 R3); entity is
     * the ANALYSIS carrying the value
     */
    CRITICAL_RESULT,

    /**
     * Automatic EQA result submission exhausted its retries (FR-V2.2-05); entity is
     * the EQA CYCLE that could not be submitted
     */
    EQA_SUBMISSION_FAILED
}
