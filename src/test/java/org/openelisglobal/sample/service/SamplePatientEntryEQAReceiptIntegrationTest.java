package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.openelisglobal.eqa.EQASpineTestBase;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OGC-610 [EQA V2.2] — saving an EQA order routes it to a cycle and records the
 * panel receipt in the same transaction (FR-V2.2-12): the order and its receipt
 * either both land or neither does.
 */
public class SamplePatientEntryEQAReceiptIntegrationTest extends EQASpineTestBase {

    private static final long SAMPLE_ID = 9970L;
    private static final long ENROLLMENT = 9903L;
    /** Never seeded — lab_enrollment_id is a real FK on eqa_panel_receipt. */
    private static final long UNKNOWN_ENROLLMENT = 9989L;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * The receipt wiring is package-private on the impl, and the bean is behind a
     * transactional proxy, so unwrap to reach it.
     */
    private SamplePatientEntryServiceImpl impl() {
        return AopTestUtils.getTargetObject(samplePatientEntryService);
    }

    @Override
    protected void cleanEqaTables() {
        // sample_eqa points at eqa_cycle, so it goes before the spine tables; the
        // sample itself goes after, being sample_eqa's own parent.
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id = ?", SAMPLE_ID);
        super.cleanEqaTables();
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.shipping_box WHERE id = 9955");
        jdbc.update("DELETE FROM clinlims.organization WHERE id = '9950'");
    }

    private void seedSample() {
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, lastupdated)"
                + " VALUES (?, 'EQAT15001', now(), now(), now()) ON CONFLICT (id) DO NOTHING", SAMPLE_ID);
    }

    private SamplePatientUpdateData eqaOrder(Long cycleId, long enrollmentId) {
        Sample sample = new Sample();
        sample.setId(String.valueOf(SAMPLE_ID));

        SamplePatientUpdateData updateData = new SamplePatientUpdateData(USER);
        updateData.setSample(sample);
        updateData.setEqaSample(true);
        updateData.setEqaProgramId(String.valueOf(enrollmentId));
        updateData.setEqaCycleId(cycleId == null ? null : String.valueOf(cycleId));
        updateData.setEqaReceivedTempC("4.5");
        updateData.setEqaIntegrityOk(Boolean.TRUE);
        updateData.setEqaIntegrityNotes("Cool box intact");
        return updateData;
    }

    private Long seedCycle(String schemeName) {
        seedSample();
        seedEnrollment(ENROLLMENT, schemeName + " enrollment");
        EQAProgram scheme = insertScheme(schemeName, EQASchemeType.INTERNATIONAL_PT, "NHLS");
        return insertCycle(scheme, 1);
    }

    @Test
    public void orderSave_linksTheCycleAndRecordsTheReceipt() {
        Long cycleId = seedCycle("Order receipt");

        impl().persistSampleEQAData(eqaOrder(cycleId, ENROLLMENT));

        assertEquals("the order must carry the cycle link the V2 screens read", cycleId, jdbc
                .queryForObject("SELECT cycle_id FROM clinlims.sample_eqa WHERE sample_id = ?", Long.class, SAMPLE_ID));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_panel_receipt WHERE cycle_id = ?", Integer.class, cycleId));
        assertEquals(0,
                new java.math.BigDecimal("4.5").compareTo(
                        jdbc.queryForObject("SELECT received_temp_c FROM clinlims.eqa_panel_receipt WHERE cycle_id = ?",
                                java.math.BigDecimal.class, cycleId)));
        assertEquals(EQACycleStatus.PANEL_RECEIVED, readBack(cycleId).getStatus());
    }

    /**
     * The receipt on Add Order takes delivery of the imported consignment it names.
     */
    @Test
    public void orderSaveWithAConsignment_receivesTheBoxAndRecordsItOnTheReceipt() {
        Long cycleId = seedCycle("Order receipt with consignment");
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES ('9950', 'Receipt Test Lab', 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING");
        jdbc.update(
                "INSERT INTO clinlims.shipping_box (id, box_id, fhir_uuid, destination_facility_id, state,"
                        + " created_date, archived, sys_user_id, lastupdated)"
                        + " VALUES (9955, 'BOX-9955', gen_random_uuid(), 9950, 'IN_TRANSIT', now(), false, ?, now())",
                Integer.parseInt(USER));
        SamplePatientUpdateData order = eqaOrder(cycleId, ENROLLMENT);
        order.setEqaShippingBoxId("9955");

        impl().persistSampleEQAData(order);

        assertEquals(Integer.valueOf(9955), jdbc.queryForObject(
                "SELECT shipping_box_id FROM clinlims.eqa_panel_receipt WHERE cycle_id = ?", Integer.class, cycleId));
        assertEquals("RECEIVED",
                jdbc.queryForObject("SELECT state FROM clinlims.shipping_box WHERE id = 9955", String.class));
        assertEquals(EQACycleStatus.PANEL_RECEIVED, readBack(cycleId).getStatus());
    }

    /** FR-V2.1-03: no cycle picked is a legal order, not a rejected one. */
    @Test
    public void orderSaveWithoutACycle_recordsNoReceipt() {
        seedCycle("Uncycled order");

        impl().persistSampleEQAData(eqaOrder(null, ENROLLMENT));

        assertNull(jdbc.queryForObject("SELECT cycle_id FROM clinlims.sample_eqa WHERE sample_id = ?", Long.class,
                SAMPLE_ID));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_panel_receipt", Integer.class));
    }

    /**
     * FR-V2.2-12 atomicity: a receipt that cannot be recorded takes the order row
     * down with it rather than leaving a half-saved EQA order behind.
     */
    @Test
    public void receiptFailure_rollsBackTheWholeOrderSave() {
        Long cycleId = seedCycle("Rollback order");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(Exception.class, () -> transaction.execute(status -> {
            impl().persistSampleEQAData(eqaOrder(cycleId, UNKNOWN_ENROLLMENT));
            return null;
        }));

        assertEquals("the sample_eqa row must not survive a failed receipt", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample_eqa WHERE sample_id = ?", Integer.class, SAMPLE_ID));
        assertEquals(EQACycleStatus.PLANNED, readBack(cycleId).getStatus());
    }
}
