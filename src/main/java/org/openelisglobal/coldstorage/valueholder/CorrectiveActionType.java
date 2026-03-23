package org.openelisglobal.coldstorage.valueholder;

public enum CorrectiveActionType {
    /**
     * Adjusting temperature settings
     */
    TEMPERATURE_ADJUSTMENT,

    /**
     * Repairing or replacing equipment
     */
    EQUIPMENT_REPAIR,

    /**
     * Moving samples to another location
     */
    SAMPLE_RELOCATION,

    /**
     * Calibrating equipment or sensors
     */
    CALIBRATION,

    /**
     * Reordering inventory items
     */
    ITEM_REORDER,

    /**
     * Performing maintenance tasks
     */
    MAINTENANCE,

    /**
     * Other or custom corrective actions
     */
    OTHER
}
