package org.openelisglobal.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Upgrading from 3.2.x must keep inventory storage: the 3.2.x lot form required
 * a storage location, so every lot there has one. The skeleton those rows live
 * in is dropped on upgrade; this runs the real changelog over the 3.2.x schema
 * shape and checks the locations land in the storage hierarchy and each lot
 * keeps its place.
 */
public class InventoryStorageLocationMigrationTest {

    private static final String SKELETON = "CREATE SEQUENCE clinlims.inventory_storage_location_seq;"
            + " CREATE TABLE clinlims.inventory_storage_location (id bigint PRIMARY KEY, fhir_uuid uuid NOT NULL,"
            + " name varchar(255) NOT NULL, location_code varchar(50) UNIQUE, location_type varchar(50) NOT NULL,"
            + " description text, temperature_min numeric(5,2), temperature_max numeric(5,2),"
            + " parent_location_id bigint REFERENCES clinlims.inventory_storage_location(id),"
            + " is_active boolean NOT NULL DEFAULT true, last_updated timestamp DEFAULT now());"
            + " ALTER TABLE clinlims.inventory_lot ADD COLUMN storage_location_id bigint;"
            + " ALTER TABLE clinlims.inventory_lot ADD CONSTRAINT fk_lot_storage_location FOREIGN KEY"
            + " (storage_location_id) REFERENCES clinlims.inventory_storage_location(id);";

    private static final String DATA = "INSERT INTO clinlims.inventory_storage_location (id, fhir_uuid, name,"
            + " location_code, location_type, parent_location_id, temperature_min, temperature_max, is_active) VALUES"
            + " (1, gen_random_uuid(), 'Labo principal', 'LAB-MAIN', 'ROOM', NULL, NULL, NULL, true),"
            + " (2, gen_random_uuid(), 'Congelateur -20', 'FRZ-20', 'FREEZER', 1, -25, -15, true),"
            + " (3, gen_random_uuid(), 'Etagere 1', 'SH1', 'SHELF', 2, NULL, NULL, true),"
            + " (4, gen_random_uuid(), 'Frigo reactifs', 'FRG-A', 'REFRIGERATOR', NULL, 2, 8, true);"
            + " INSERT INTO clinlims.inventory_item (id, fhir_uuid, name, item_type, units, code) VALUES"
            + " (9001, gen_random_uuid(), 'Kit', 'REAGENT', 'tests', 'KIT9001');"
            + " INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number, initial_quantity,"
            + " current_quantity, storage_location_id) VALUES (9101, gen_random_uuid(), 9001, 'LOT-SHELF', 10, 10, 3),"
            + " (9102, gen_random_uuid(), 9001, 'LOT-FRIDGE', 10, 10, 4),"
            + " (9103, gen_random_uuid(), 9001, 'LOT-NOWHERE', 10, 10, NULL);"
            + " DELETE FROM clinlims.databasechangelog WHERE id IN ('OGC-657-migrate-inventory-storage-locations',"
            + " 'OGC-657-drop-inventory-lot-storage-location-column', 'OGC-657-drop-inventory-storage-location-table');";

    @Test
    public void storageLocationsAndLotPlacementsSurviveTheUpgrade() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims");
            postgres.withUsername("clinlims");
            postgres.withPassword("clinlims");
            postgres.start();

            try (Connection connection = postgres.createConnection("")) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("clinlims");
                ClassLoaderResourceAccessor resources = new ClassLoaderResourceAccessor();
                new Liquibase("liquibase/base-changelog.xml", resources, database).update(new Contexts("test"));
                assertFalse(
                        exists(connection, "SELECT to_regclass('clinlims.inventory_storage_location') IS NOT NULL"));
                assertFalse("a fresh install has nothing to archive", exists(connection,
                        "SELECT to_regclass('clinlims.inventory_storage_location_archive') IS NOT NULL"));

                try (Statement statement = connection.createStatement()) {
                    statement.execute(SKELETON);
                    statement.execute(DATA);
                }
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                ChangeLogHistoryServiceFactory.getInstance().resetAll();
                Database upgrade = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                upgrade.setDefaultSchemaName("clinlims");
                new Liquibase("liquibase/base-changelog.xml", resources, upgrade).update(new Contexts("test"));

                assertFalse(
                        exists(connection, "SELECT to_regclass('clinlims.inventory_storage_location') IS NOT NULL"));
                assertEquals("4 archived locations", 4,
                        count(connection, "SELECT count(*) FROM clinlims.inventory_storage_location_archive"));
                assertTrue("the room keeps its name and code",
                        exists(connection,
                                "SELECT count(*) = 1 FROM clinlims.storage_room WHERE name = 'Labo principal'"
                                        + " AND code = 'LAB-MAIN'"));
                assertTrue("the freezer sits in its room with the midpoint temperature", exists(connection,
                        "SELECT count(*) = 1 FROM clinlims.storage_device d JOIN clinlims.storage_room r"
                                + " ON r.id = d.parent_room_id WHERE d.name = 'Congelateur -20' AND d.type = 'freezer'"
                                + " AND d.temperature_setting = -20 AND r.code = 'LAB-MAIN'"));
                assertTrue("a device with no room goes under the migrated-storage room",
                        exists(connection,
                                "SELECT count(*) = 1 FROM clinlims.storage_device d JOIN clinlims.storage_room r"
                                        + " ON r.id = d.parent_room_id WHERE d.name = 'Frigo reactifs'"
                                        + " AND d.type = 'refrigerator' AND r.name = 'Migrated inventory storage'"));
                assertTrue("the lot on the shelf keeps its shelf", exists(connection,
                        "SELECT count(*) = 1 FROM clinlims.sample_storage_assignment a JOIN clinlims.storage_shelf s"
                                + " ON s.id = a.location_id WHERE a.inventory_lot_id = 9101"
                                + " AND a.occupant_type = 'INVENTORY_LOT' AND a.location_type = 'shelf'"
                                + " AND s.label = 'Etagere 1'"));
                assertTrue("the lot in the fridge keeps its device", exists(connection,
                        "SELECT count(*) = 1 FROM clinlims.sample_storage_assignment a JOIN clinlims.storage_device d"
                                + " ON d.id = a.location_id WHERE a.inventory_lot_id = 9102"
                                + " AND a.location_type = 'device' AND d.name = 'Frigo reactifs'"));
                assertEquals("a lot without a location gets no assignment", 0, count(connection,
                        "SELECT count(*) FROM clinlims.sample_storage_assignment WHERE inventory_lot_id = 9103"));
            }
        }
    }

    private boolean exists(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getBoolean(1);
        }
    }

    private int count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
