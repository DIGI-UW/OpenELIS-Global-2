package org.openelisglobal.inventory.migration;

/**
 * Documents and tests the Liquibase heuristic that reclassifies mis-typed CARTRIDGE
 * catalog rows holding equipment metadata to EQUIPMENT.
 */
public final class InventoryCartridgeEquipmentMigration {

    private InventoryCartridgeEquipmentMigration() {
    }

    /**
     * Rows still matching this query after migration 045/047 should be reviewed manually
     * (likely true analyzer cartridges, not instruments).
     */
    public static final String AUDIT_REMAINING_CARTRIDGE_WITH_EQUIPMENT_METADATA_SQL = """
            SELECT id, name, category, model_number, serial_number, equipment_condition, ahri_tag
            FROM clinlims.inventory_item
            WHERE item_type = 'CARTRIDGE'
              AND (
                NULLIF(TRIM(model_number), '') IS NOT NULL
                OR NULLIF(TRIM(serial_number), '') IS NOT NULL
                OR NULLIF(TRIM(equipment_condition), '') IS NOT NULL
                OR NULLIF(TRIM(ahri_tag), '') IS NOT NULL
              )
            ORDER BY name
            """;

    /**
     * Predicate for changeset inventory-migrate-cartridge-to-equipment (045).
     */
    public static boolean shouldMigrateCartridgeToEquipment(String modelNumber, String serialNumber,
            String equipmentCondition, String ahriTag) {
        return hasText(modelNumber) || hasText(serialNumber) || hasText(equipmentCondition) || hasText(ahriTag);
    }

    /**
     * Additional predicate for changeset inventory-migrate-cartridge-to-equipment-dates (047).
     */
    public static boolean shouldMigrateCartridgeToEquipmentByMaintenanceDates(
            java.util.Date installationDate, java.util.Date lastMaintenanceDate, java.util.Date nextMaintenanceDate) {
        return installationDate != null || lastMaintenanceDate != null || nextMaintenanceDate != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
