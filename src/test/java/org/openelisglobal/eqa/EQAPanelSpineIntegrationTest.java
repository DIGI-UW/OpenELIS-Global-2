package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAAnalystCompetencyEventDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantFollowupDAO;
import org.openelisglobal.eqa.dao.EQASchemeAnalystDAO;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelSourceType;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStorageTemp;
import org.openelisglobal.security.GeneralTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-609 [EQA V2.1] — panels, sealed samples, receipts, follow-ups and
 * competency events against a real DB (qa/017 + qa/018).
 *
 * <p>
 * The test Spring profile uses a pass-through TextEncryptor. Entity round trips
 * verify converter wiring; the capacity test explicitly encrypts with the real
 * encryptor and verifies that the complete ciphertext survives database
 * storage. Runtime converter configuration remains a live-stack acceptance
 * check.
 */
public class EQAPanelSpineIntegrationTest extends EQASpineTestBase {

    private static final long ENROLLMENT_ID = 9903L;
    private static final long ANALYTE_HIV_VL = 9802L;
    private static final long ORG_ID = 9904L;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private EQASchemeAnalystDAO eqaSchemeAnalystDAO;

    @Autowired
    private EQAParticipantFollowupDAO eqaParticipantFollowupDAO;

    @Autowired
    private EQAAnalystCompetencyEventDAO eqaAnalystCompetencyEventDAO;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        seedEnrollment(ENROLLMENT_ID, "Panel spine enrollment");
        jdbc.update("INSERT INTO clinlims.organization (id, name, lastupdated)"
                + " VALUES (?, 'Panel spine participant', now())", ORG_ID);
    }

    @Override
    protected void cleanEqaTables() {
        super.cleanEqaTables();
        jdbc.update("DELETE FROM clinlims.organization WHERE id = ?", ORG_ID);
    }

    // ---- panels, samples and inventory ----

    @Test
    public void panelAndSealedSampleRoundTripWithExactValues() {
        EQAPanel panel = insertPanel(insertScheme("Panel round-trip", EQASchemeType.INTER_LAB_SPLIT, "NHLS"), p -> {
            p.setPanelName("2026 R1 panel");
            p.setStatus(EQAPanelStatus.SEALED);
            p.setSourceType(EQAPanelSourceType.VENDOR_SOURCED);
            p.setVendorName("Acme Controls");
            p.setVendorLot("LOT-77");
            p.setStorageTemp(EQAStorageTemp.FROZEN_MINUS_20C);
            p.setExpirationDate(Date.valueOf("2027-01-31"));
            p.setAliquotsProduced(100);
            p.setAliquotsReserved(10);
            p.setAliquotsShipped(80);
            p.setHomogeneityQcPassed(true);
            p.setHomogeneityQcNotes("Checked across 10 vials");
        });

        EQAPanel read = eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new);
        assertEquals("2026 R1 panel", read.getPanelName());
        assertEquals(EQAPanelStatus.SEALED, read.getStatus());
        assertEquals(EQAPanelSourceType.VENDOR_SOURCED, read.getSourceType());
        assertEquals(EQAStorageTemp.FROZEN_MINUS_20C, read.getStorageTemp());
        assertEquals("Acme Controls", read.getVendorName());
        assertEquals(Date.valueOf("2027-01-31"), read.getExpirationDate());
        assertEquals(Integer.valueOf(100), read.getAliquotsProduced());
        assertTrue(read.getHomogeneityQcPassed());
        assertNotNull("fhirUuid is assigned on persist", read.getFhirUuid());

        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(read);
        sample.setSampleCode("SAMPLE-A01");
        sample.setBlindCode("BLIND-77");
        sample.setAnalyteId(ANALYTE_HIV_VL);
        sample.setTargetValue("4.52");
        sample.setTargetUnit("log10 c/mL");
        sample.setAcceptanceRangeLow(new BigDecimal("4.00000"));
        sample.setAcceptanceRangeHigh(new BigDecimal("5.00000"));
        sample.setSysUserId(USER);
        Long sampleId = eqaPanelSampleDAO.insert(sample);

        EQAPanelSample readSample = eqaPanelSampleDAO.get(sampleId).orElseThrow(AssertionError::new);
        assertEquals("SAMPLE-A01", readSample.getSampleCode());
        assertEquals("BLIND-77", readSample.getBlindCode());
        // The converter is wired: the value survives a write/read cycle. Whether
        // the bytes at rest are ciphertext is a live-stack check (see class doc).
        assertEquals("4.52", readSample.getTargetValue());
        assertEquals(0, new BigDecimal("4.00000").compareTo(readSample.getAcceptanceRangeLow()));
    }

    @Test
    public void targetValueColumnIsWideEnoughForCiphertext() {
        GeneralTextEncryptor encryptor = GeneralTextEncryptor.of(java.util.UUID.randomUUID().toString(), null);
        String plaintext = "x".repeat(336);
        String ciphertext = encryptor.encrypt(plaintext);
        assertTrue("versioned ciphertext exceeds the old storage limit", ciphertext.length() > 512);
        EQAPanel panel = insertPanel(insertScheme("Ciphertext capacity", EQASchemeType.INTER_LAB_SPLIT, "NHLS"),
                p -> p.setPanelName("Ciphertext capacity"));
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode("CAPACITY-A01");
        sample.setBlindCode("CAPACITY-B01");
        sample.setAnalyteId(ANALYTE_HIV_VL);
        sample.setTargetValue(ciphertext);
        sample.setSysUserId(USER);
        Long sampleId = eqaPanelSampleDAO.insert(sample);
        String stored = jdbc.queryForObject("SELECT target_value FROM clinlims.eqa_panel_sample WHERE id = ?",
                String.class, sampleId);
        assertEquals(ciphertext, stored);
        assertEquals(plaintext, encryptor.decrypt(stored));
    }

    @Test
    public void inventoryInvariantIsEnforcedByTheDatabase() {
        EQAProgram scheme = insertScheme("Inventory", EQASchemeType.INTER_LAB_SPLIT, "NHLS");
        try {
            insertPanel(scheme, p -> {
                p.setPanelName("Oversold panel");
                p.setAliquotsProduced(10);
                p.setAliquotsReserved(5);
                p.setAliquotsShipped(6);
            });
            fail("produced must be >= reserved + shipped");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_panel_aliquots_chk");
        }
    }

    @Test
    public void unknownPanelStatusIsRejectedByTheDatabase() {
        EQAProgram scheme = insertScheme("Bad panel status", EQASchemeType.INTER_LAB_SPLIT, "NHLS");
        try {
            jdbc.update(
                    "INSERT INTO clinlims.eqa_panel (id, fhir_uuid, scheme_id, panel_name, status,"
                            + " aliquots_produced, aliquots_reserved, aliquots_shipped, homogeneity_qc_passed,"
                            + " sys_user_id, last_updated)"
                            + " VALUES (9981, gen_random_uuid(), ?, 'Bogus', 'ARCHIVED', 0, 0, 0, false, ?, now())",
                    scheme.getId(), USER);
            fail("the panel status CHECK should reject an unknown state");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_panel_status_chk");
        }
    }

    // ---- scheme analysts ----

    @Test
    public void anAnalystMayBeListedOncePerScheme() {
        EQAProgram scheme = insertScheme("Analyst list", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        insertAnalyst(scheme, ADMIN_USER_ID);
        try {
            insertAnalyst(scheme, ADMIN_USER_ID);
            fail("the eligible-analyst list is a set, not a bag");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "uq_eqa_scheme_analyst_scheme_user");
        }
    }

    // ---- panel receipts ----

    @Test
    public void oneReceiptPerCyclePerLab() {
        EQACycle cycle = readBack(insertCycle(insertScheme("Receipts", EQASchemeType.INTERNATIONAL_PT, "NHLS"), 1));
        insertReceipt(cycle, ENROLLMENT_ID);
        try {
            insertReceipt(cycle, ENROLLMENT_ID);
            fail("a lab confirms receipt of a cycle's panel exactly once");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "uq_eqa_panel_receipt_cycle_enrollment");
        }
    }

    @Test
    public void receiptWithoutAShipmentIsAllowed() {
        // Walk-in pickup and legacy imports have no tracked shipment.
        EQACycle cycle = readBack(insertCycle(insertScheme("Walk-in", EQASchemeType.INTERNATIONAL_PT, "NHLS"), 1));
        EQAPanelReceipt receipt = eqaPanelReceiptDAO.get(insertReceipt(cycle, ENROLLMENT_ID))
                .orElseThrow(AssertionError::new);
        assertEquals(null, receipt.getShipmentId());
        assertTrue("integrity defaults to ok", receipt.getIntegrityOk());
    }

    // ---- follow-ups ----

    @Test
    public void oneOpenFollowupPerCyclePerOrganisation() {
        EQACycle cycle = readBack(insertCycle(insertScheme("Follow-ups", EQASchemeType.INTERNATIONAL_PT, "NHLS"), 1));
        insertFollowup(cycle);
        try {
            insertFollowup(cycle);
            fail("duplicate follow-up must be refused");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "uq_eqa_participant_followup_cycle_org");
        }
    }

    // ---- competency events ----

    @Test
    public void competencyEventPersistsWithItsVocabulary() {
        EQAProgram scheme = insertScheme("Competency", EQASchemeType.INTERNATIONAL_PT, "NHLS");

        EQAAnalystCompetencyEvent event = new EQAAnalystCompetencyEvent();
        event.setAnalystId(ADMIN_USER_ID);
        event.setEventType(EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE);
        event.setEventDate(Date.valueOf("2026-08-14"));
        event.setScheme(scheme);
        event.setNotes("Deadline passed with no validated result");
        event.setSysUserId(USER);
        Long id = eqaAnalystCompetencyEventDAO.insert(event);

        EQAAnalystCompetencyEvent read = eqaAnalystCompetencyEventDAO.get(id).orElseThrow(AssertionError::new);
        assertEquals(EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE, read.getEventType());
        assertEquals(Date.valueOf("2026-08-14"), read.getEventDate());
        assertEquals(Long.valueOf(ADMIN_USER_ID), read.getAnalystId());
    }

    @Test
    public void unknownCompetencyEventTypeIsRejectedByTheDatabase() {
        EQAProgram scheme = insertScheme("Bad event", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        try {
            jdbc.update(
                    "INSERT INTO clinlims.eqa_analyst_competency_event (id, analyst_id, event_type, event_date,"
                            + " scheme_id, sys_user_id, last_updated)"
                            + " VALUES (9982, ?, 'unknown', current_date, ?, ?, now())",
                    ADMIN_USER_ID, scheme.getId(), USER);
            fail("an undefined event_type must be refused");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_competency_event_type_chk");
        }
    }

    // ---- helpers ----

    private void insertAnalyst(EQAProgram scheme, long userId) {
        EQASchemeAnalyst analyst = new EQASchemeAnalyst();
        analyst.setScheme(scheme);
        analyst.setSystemUserId(userId);
        analyst.setSysUserId(USER);
        eqaSchemeAnalystDAO.insert(analyst);
    }

    private void insertFollowup(EQACycle cycle) {
        EQAParticipantFollowup followup = new EQAParticipantFollowup();
        followup.setScheme(cycle.getScheme());
        followup.setCycle(cycle);
        followup.setParticipantOrgId(ORG_ID);
        followup.setSysUserId(USER);
        eqaParticipantFollowupDAO.insert(followup);
    }
}
