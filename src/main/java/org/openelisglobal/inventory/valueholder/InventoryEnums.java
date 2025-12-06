package org.openelisglobal.inventory.valueholder;

/**
 * Central container for all inventory-related enums Consolidates enums to
 * reduce number of class files
 */
public final class InventoryEnums {

    private InventoryEnums() {
        // Utility class - prevent instantiation
    }

    /**
     * Types of inventory items
     */
    public enum ItemType {
        REAGENT, RDT, CARTRIDGE, HIV_KIT, SYPHILIS_KIT
    }

    /**
     * Types of storage locations for inventory
     */
    public enum LocationType {
        ROOM, REFRIGERATOR, FREEZER, SHELF, DRAWER, CABINET
    }

    /**
     * Status of inventory lots
     */
    public enum LotStatus {
        ACTIVE, IN_USE, EXPIRED, CONSUMED, DISPOSED, QUARANTINED
    }

    /**
     * Quality control status
     */
    public enum QCStatus {
        PENDING, PASSED, FAILED, QUARANTINED
    }

    /**
     * Types of references for transactions
     */
    public enum ReferenceType {
        TEST_RESULT, RECEIPT, QC_RUN, MANUAL, ADJUSTMENT
    }

    /**
     * Types of inventory transactions
     */
    public enum TransactionType {
        RECEIPT, CONSUMPTION, ADJUSTMENT, DISPOSAL, OPENING, QC_TEST, MANUAL
    }
}
