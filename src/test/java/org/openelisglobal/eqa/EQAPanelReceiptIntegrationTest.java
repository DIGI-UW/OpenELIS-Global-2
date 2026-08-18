package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAPanelReceiptService;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-609 [EQA V2.1] — receipt intake side-effects are one transaction
 * (FR-V2.1-20): receipt row + shipment delivery + cycle transition, and a
 * double receipt is a read, not a write.
 */
public class EQAPanelReceiptIntegrationTest extends EQASpineTestBase {

    private static final long ENROLLMENT = 9901L;

    @Autowired
    private EQAPanelReceiptService receiptService;

    @Override
    protected void cleanEqaTables() {
        // Receipts reference shipments, so the EQA tables go first.
        super.cleanEqaTables();
        jdbc.update("DELETE FROM clinlims.shipment WHERE id BETWEEN 9950 AND 9959");
        jdbc.update("DELETE FROM clinlims.shipping_box WHERE id BETWEEN 9950 AND 9959");
        jdbc.update("DELETE FROM clinlims.organization WHERE id = '9950'");
    }

    private Integer seedShipment(int id) {
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES ('9950', 'Receipt Test Lab', 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING");
        jdbc.update(
                "INSERT INTO clinlims.shipping_box (id, box_id, fhir_uuid, destination_facility_id, state,"
                        + " created_date, archived, sys_user_id, lastupdated)"
                        + " VALUES (?, 'BOX-' || ?, gen_random_uuid(), 9950, 'IN_TRANSIT', now(), false, ?, now())",
                id, id, Integer.parseInt(USER));
        jdbc.update(
                "INSERT INTO clinlims.shipment (id, shipping_box_id, courier, tracking_number, status,"
                        + " shipped_date, sys_user_id, lastupdated)"
                        + " VALUES (?, ?, 'DHL', 'TRK-' || ?, 'IN_TRANSIT', now(), ?, now())",
                id, id, id, Integer.parseInt(USER));
        return id;
    }

    @Test
    public void receipt_flipsShipmentAndCycleAtomically() {
        seedEnrollment(ENROLLMENT, "Receipt program");
        EQAProgram scheme = insertScheme("Receipt scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        Long cycleId = insertCycle(scheme, 1);
        Integer shipmentId = seedShipment(9951);

        EQAPanelReceipt receipt = receiptService.recordReceipt(cycleId, ENROLLMENT, shipmentId, new BigDecimal("4.50"),
                true, null, ADMIN_USER_ID, USER);

        // All three rows changed: receipt, shipment, cycle (+ its audit row).
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_panel_receipt WHERE id = ?", Integer.class, receipt.getId()));
        assertEquals("DELIVERED",
                jdbc.queryForObject("SELECT status FROM clinlims.shipment WHERE id = ?", String.class, shipmentId));
        Timestamp delivered = jdbc.queryForObject("SELECT actual_delivery_date FROM clinlims.shipment WHERE id = ?",
                Timestamp.class, shipmentId);
        assertEquals("actual_delivery_date must be stamped", false, delivered == null);
        assertEquals(EQACycleStatus.PANEL_RECEIVED, readBack(cycleId).getStatus());
        assertEquals("PANEL_RECEIPT",
                jdbc.queryForObject("SELECT trigger_event FROM clinlims.eqa_cycle_state_transition"
                        + " WHERE cycle_id = ? ORDER BY id DESC LIMIT 1", String.class, cycleId));
        assertEquals("AUTO", jdbc.queryForObject("SELECT trigger_type FROM clinlims.eqa_cycle_state_transition"
                + " WHERE cycle_id = ? ORDER BY id DESC LIMIT 1", String.class, cycleId));
    }

    @Test
    public void doubleReceipt_isIdempotentAndReadOnly() {
        seedEnrollment(ENROLLMENT, "Double receipt");
        EQAProgram scheme = insertScheme("Double scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        Long cycleId = insertCycle(scheme, 1);

        EQAPanelReceipt first = receiptService.recordReceipt(cycleId, ENROLLMENT, null, null, true, null, ADMIN_USER_ID,
                USER);
        int transitionsAfterFirst = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ?", Integer.class, cycleId);

        EQAPanelReceipt second = receiptService.recordReceipt(cycleId, ENROLLMENT, seedShipment(9952),
                new BigDecimal("9.99"), false, "should be ignored", ADMIN_USER_ID, USER);

        assertEquals("second call returns the existing receipt", first.getId(), second.getId());
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_panel_receipt WHERE cycle_id = ?", Integer.class, cycleId));
        assertEquals("no second transition fires", Integer.valueOf(transitionsAfterFirst), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ?", Integer.class, cycleId));
        assertEquals("the ignored shipment stays untouched", "IN_TRANSIT",
                jdbc.queryForObject("SELECT status FROM clinlims.shipment WHERE id = 9952", String.class));
    }

    @Test
    public void receiptWithoutShipment_stillAdvancesTheCycle() {
        seedEnrollment(ENROLLMENT, "No shipment");
        EQAProgram scheme = insertScheme("No-shipment scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        Long cycleId = insertCycle(scheme, 1);

        EQAPanelReceipt receipt = receiptService.recordReceipt(cycleId, ENROLLMENT, null, null, null, null,
                ADMIN_USER_ID, USER);

        assertNull(receipt.getShipmentId());
        assertEquals(Boolean.TRUE, receipt.getIntegrityOk());
        assertEquals(EQACycleStatus.PANEL_RECEIVED, readBack(cycleId).getStatus());
    }

    @Test
    public void unknownShipment_rollsBackTheWholeReceipt() {
        seedEnrollment(ENROLLMENT, "Rollback");
        EQAProgram scheme = insertScheme("Rollback scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        Long cycleId = insertCycle(scheme, 1);

        try {
            receiptService.recordReceipt(cycleId, ENROLLMENT, 424242, null, true, null, ADMIN_USER_ID, USER);
        } catch (IllegalArgumentException expected) {
            // the receipt insert that preceded the failure must not survive
        }

        assertEquals("nothing may persist when a side-effect fails", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_panel_receipt WHERE cycle_id = ?", Integer.class, cycleId));
        assertEquals(EQACycleStatus.PLANNED, readBack(cycleId).getStatus());
    }
}
