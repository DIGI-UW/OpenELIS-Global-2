package org.openelisglobal.inventory.valueholder;

/**
 * LocationType enum - Physical location types for inventory storage
 *
 * Used by storage_location table to categorize where inventory items (reagents,
 * RDTs, cartridges) are physically stored.
 *
 * This is separate from StorageDevice which is used for sample storage.
 */
public enum LocationType {
    /**
     * Building or site (top-level location)
     */
    SITE,

    /**
     * Room within a building
     */
    ROOM,

    /**
     * Cabinet or storage unit within a room
     */
    CABINET,

    /**
     * Shelf within a cabinet
     */
    SHELF,

    /**
     * Refrigerator for cold storage
     */
    REFRIGERATOR,

    /**
     * Freezer for frozen storage
     */
    FREEZER,

    /**
     * Other or custom location type
     */
    OTHER
}
