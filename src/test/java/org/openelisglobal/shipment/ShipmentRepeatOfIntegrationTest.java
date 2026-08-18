package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-613 [EQA V2.5] — shipment.repeat_of_shipment_id (FR-V2.5-15): the
 * reprovisioning trail persists, and deleting an original that a repeat still
 * points at is refused (RESTRICT), so the register cannot lose its history.
 */
public class ShipmentRepeatOfIntegrationTest extends BaseWebContextSensitiveTest {

    private static final int ORIGINAL = 9950;
    private static final int REPEAT = 9951;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void seed() {
        jdbc = new JdbcTemplate(dataSource);
        cleanRows();
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES ('9950', 'Repeat Test Lab', 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING");
        for (int id : new int[] { ORIGINAL, REPEAT }) {
            jdbc.update(
                    "INSERT INTO clinlims.shipping_box (id, box_id, fhir_uuid, destination_facility_id, state,"
                            + " created_date, archived, sys_user_id, lastupdated)"
                            + " VALUES (?, 'BOX-' || ?, gen_random_uuid(), 9950, 'IN_TRANSIT', now(), false, 1, now())",
                    id, id);
            jdbc.update(
                    "INSERT INTO clinlims.shipment (id, shipping_box_id, courier, tracking_number, status,"
                            + " sys_user_id, lastupdated) VALUES (?, ?, 'DHL', 'TRK-' || ?, 'PENDING', 1, now())",
                    id, id, id);
        }
    }

    @After
    public void cleanRows() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.shipment WHERE id IN (9950, 9951)");
        jdbc.update("DELETE FROM clinlims.shipping_box WHERE id IN (9950, 9951)");
        jdbc.update("DELETE FROM clinlims.organization WHERE id = '9950'");
    }

    @Test
    public void repeatOf_persistsAndReadsBack() {
        jdbc.update("UPDATE clinlims.shipment SET repeat_of_shipment_id = ? WHERE id = ?", ORIGINAL, REPEAT);

        assertEquals(Integer.valueOf(ORIGINAL), jdbc.queryForObject(
                "SELECT repeat_of_shipment_id FROM clinlims.shipment WHERE id = ?", Integer.class, REPEAT));
        assertNull("the original carries no repeat pointer", jdbc.queryForObject(
                "SELECT repeat_of_shipment_id FROM clinlims.shipment WHERE id = ?", Integer.class, ORIGINAL));
    }

    @Test
    public void unknownRepeatTarget_isRefusedByTheFk() {
        try {
            jdbc.update("UPDATE clinlims.shipment SET repeat_of_shipment_id = 424242 WHERE id = ?", REPEAT);
            fail("fk_shipment_repeat_of must refuse a repeat pointer at a shipment that does not exist");
        } catch (DataIntegrityViolationException expected) {
            assertNull(jdbc.queryForObject("SELECT repeat_of_shipment_id FROM clinlims.shipment WHERE id = ?",
                    Integer.class, REPEAT));
        }
    }

    @Test
    public void deletingAReferencedOriginal_isRefused() {
        jdbc.update("UPDATE clinlims.shipment SET repeat_of_shipment_id = ? WHERE id = ?", ORIGINAL, REPEAT);

        try {
            jdbc.update("DELETE FROM clinlims.shipment WHERE id = ?", ORIGINAL);
            fail("deleting an original a repeat points at must be refused (RESTRICT keeps the trail)");
        } catch (DataIntegrityViolationException expected) {
            assertEquals("the original must survive the refused delete", Integer.valueOf(1), jdbc
                    .queryForObject("SELECT count(*) FROM clinlims.shipment WHERE id = ?", Integer.class, ORIGINAL));
        }
        // Clearing the pointer makes the delete legal again — the rollback path.
        jdbc.update("UPDATE clinlims.shipment SET repeat_of_shipment_id = NULL WHERE id = ?", REPEAT);
        jdbc.update("DELETE FROM clinlims.shipment WHERE id = ?", ORIGINAL);
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.shipment WHERE id = ?", Integer.class, ORIGINAL));
    }
}
