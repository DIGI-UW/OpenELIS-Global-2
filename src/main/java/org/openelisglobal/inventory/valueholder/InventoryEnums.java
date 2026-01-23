package org.openelisglobal.inventory.valueholder;

/**
 * Central container for all inventory-related enums Consolidates enums to
 * reduce number of class files
 */
public final class InventoryEnums {

    private InventoryEnums() {
        // Utility class - prevent instantiation
    }

    /** Types of inventory items */
    public enum ItemType {
        REAGENT, RDT, CARTRIDGE, HIV_KIT, SYPHILIS_KIT, ENZYME, ANTIBIOTICS
    }

    /** Types of storage locations for inventory */
    public enum LocationType {
        ROOM, REFRIGERATOR, FREEZER, SHELF, DRAWER, CABINET
    }

    /** Status of inventory lots */
    public enum LotStatus {
        ACTIVE, IN_USE, EXPIRED, CONSUMED, DISPOSED, QUARANTINED
    }

    /** Quality control status */
    public enum QCStatus {
        PENDING, PASSED, FAILED, QUARANTINED
    }

    /** Types of references for transactions */
    public enum ReferenceType {
        TEST_RESULT, RECEIPT, QC_RUN, MANUAL, ADJUSTMENT
    }

    /** Types of inventory transactions */
    public enum TransactionType {
        RECEIPT, CONSUMPTION, ADJUSTMENT, DISPOSAL, OPENING, QC_TEST, MANUAL
    }

    /**
     * Types of antibiotics for drug susceptibility testing
     */
    public enum AntibioticType {
        AMIKACIN, AMOXICILLIN, AMOXICILLIN_CLAVULANIC_ACID, AMPICILLIN, AMPICILLIN_SULBACTAM, AZITHROMYCIN, AZTREONAM,
        CEFAZOLIN, CEFEPIME, CEFOTAXIME, CEFPODOXIME, CEFTRIAXONE, CEFUROXIME, CEFOXITIN, CEFTAZIDIME, CEFOPERAZONE,
        CHLORAMPHENICOL, CIPROFLOXACIN, CLINDAMYCIN, COLISTIN, DAPTOMYCIN, DOXYCYCLINE, ERYTHROMYCIN, FOSFOMYCIN,
        GENTAMICIN
    }
}
