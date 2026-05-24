package org.openelisglobal.inventory.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import org.junit.Test;

public class InventoryCartridgeEquipmentMigrationTest {

    @Test
    public void migratesCartridgeWithEquipmentMetadata() {
        assertTrue(InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment("QS-3", null, null, null));
        assertTrue(InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment(null, "SN-1", null, null));
        assertTrue(
                InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment(null, null, "functional", null));
        assertTrue(InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment(null, null, null, "AHRI-1"));
    }

    @Test
    public void doesNotMigrateSupplyCartridgeWithoutMetadata() {
        assertFalse(InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment(null, null, null, null));
        assertFalse(InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment("  ", " ", " ", " "));
    }

    @Test
    public void migratesCartridgeWithMaintenanceDates() {
        var date = new GregorianCalendar(2024, Calendar.JANUARY, 1).getTime();
        assertTrue(InventoryCartridgeEquipmentMigration
                .shouldMigrateCartridgeToEquipmentByMaintenanceDates(date, null, null));
        assertTrue(InventoryCartridgeEquipmentMigration
                .shouldMigrateCartridgeToEquipmentByMaintenanceDates(null, date, null));
        assertTrue(InventoryCartridgeEquipmentMigration
                .shouldMigrateCartridgeToEquipmentByMaintenanceDates(null, null, date));
    }

    @Test
    public void auditSqlTargetsCartridgeWithEquipmentMetadata() {
        assertTrue(InventoryCartridgeEquipmentMigration.AUDIT_REMAINING_CARTRIDGE_WITH_EQUIPMENT_METADATA_SQL
                .contains("item_type = 'CARTRIDGE'"));
        assertTrue(InventoryCartridgeEquipmentMigration.AUDIT_REMAINING_CARTRIDGE_WITH_EQUIPMENT_METADATA_SQL
                .contains("model_number"));
    }

    @Test
    public void afterMigrationHeuristicMachineRowIsEquipmentNotCartridge() {
        String model = "GeneXpert IV";
        String serial = "GX-2020-001";
        assertTrue(InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment(model, serial, null, null));
        assertEquals("EQUIPMENT", simulatedPostMigrationType(model, serial, null, null));
    }

    private static String simulatedPostMigrationType(String model, String serial, String condition, String ahriTag) {
        if (InventoryCartridgeEquipmentMigration.shouldMigrateCartridgeToEquipment(model, serial, condition, ahriTag)) {
            return "EQUIPMENT";
        }
        return "CARTRIDGE";
    }
}
