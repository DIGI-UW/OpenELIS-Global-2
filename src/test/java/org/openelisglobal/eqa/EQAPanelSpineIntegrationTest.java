package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.eqa.dao.EQAAnalystCompetencyEventDAO;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantFollowupDAO;
import org.openelisglobal.eqa.dao.EQASchemeAnalystDAO;
import org.openelisglobal.eqa.service.EQAProgramService;
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
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-609 [EQA V2.1 / T-09] — panels, sealed samples, receipts, follow-ups and
 * competency events against a real DB (qa/017 + qa/018).
 *
 * <p>
 * One deliberate gap, worth stating so nobody reads more assurance into this
 * class than it offers: the {@code test} Spring profile replaces
 * {@code TextEncryptor} with a pass-through mock, so
 * {@code eqa_panel_sample.target_value} is stored as plaintext here. These
 * tests therefore prove the converter is *wired* and the column is *wide
 * enough*, but only the live stack can prove the value is actually ciphertext
 * at rest. That check belongs to UAT, and the column width below is what makes
 * it survivable: measured against jasypt 1.9.3, a 143-byte plaintext is the
 * largest whose ciphertext (236 chars) still fits VARCHAR(255); 144 bytes
 * yields 256 chars and overflows it.
 */
public class EQAPanelSpineIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String USER = "1";
    private static final long ADMIN_USER_ID = 1L;
    private static final long ENROLLMENT_ID = 9903L;
    private static final long ANALYTE_HIV_VL = 9802L;
    private static final long ORG_ID = 9904L;

    @Autowired
    private EQAProgramService eqaProgramService;

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private EQASchemeAnalystDAO eqaSchemeAnalystDAO;

    @Autowired
    private EQAPanelReceiptDAO eqaPanelReceiptDAO;

    @Autowired
    private EQAParticipantFollowupDAO eqaParticipantFollowupDAO;

    @Autowired
    private EQAAnalystCompetencyEventDAO eqaAnalystCompetencyEventDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/eqa-cycle-spine.xml");
        clean();
        jdbc.update("INSERT INTO clinlims.eqa_lab_program_enrollment"
                + " (id, program_name, provider, is_active, created_date, sys_user_id, lastupdated)"
                + " VALUES (?, 'Panel spine enrollment', 'NHLS', true, now(), ?, now())", ENROLLMENT_ID, USER);
        jdbc.update("INSERT INTO clinlims.organization (id, name, lastupdated)"
                + " VALUES (?, 'Panel spine participant', now())", ORG_ID);
    }

    @After
    public void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("DELETE FROM clinlims.eqa_analyst_competency_event");
        jdbc.update("DELETE FROM clinlims.eqa_participant_followup");
        jdbc.update("DELETE FROM clinlims.eqa_panel_receipt");
        jdbc.update("DELETE FROM clinlims.eqa_panel_sample");
        jdbc.update("DELETE FROM clinlims.eqa_panel");
        jdbc.update("DELETE FROM clinlims.eqa_scheme_analyst");
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.eqa_cycle_state_transition");
        jdbc.update("DELETE FROM clinlims.eqa_round");
        jdbc.update("DELETE FROM clinlims.eqa_cycle");
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE id = ?", ENROLLMENT_ID);
        jdbc.update("DELETE FROM clinlims.eqa_program_test");
        jdbc.update("DELETE FROM clinlims.eqa_program");
        jdbc.update("DELETE FROM clinlims.organization WHERE id = ?", ORG_ID);
    }

    // ---- FR-V2.1-11/12/17 ----

    @Test
    public void panelAndSealedSampleRoundTripWithExactValues() {
        EQAPanel panel = insertPanel(insertScheme("Panel round-trip", EQASchemeType.INTER_LAB_SPLIT), p -> {
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
        // jasypt AES256 expands plaintext to 4*ceil((32+16*(floor(bytes/16)+1))/3)
        // characters, so VARCHAR(255) would silently cap plaintext at 143 bytes.
        // This is the guard that a later "tidy up to 255" cannot pass.
        Integer length = jdbc.queryForObject(
                "SELECT character_maximum_length FROM information_schema.columns WHERE table_schema = 'clinlims'"
                        + " AND table_name = 'eqa_panel_sample' AND column_name = 'target_value'",
                Integer.class);
        assertEquals(Integer.valueOf(512), length);
    }

    @Test
    public void inventoryInvariantIsEnforcedByTheDatabase() {
        EQAProgram scheme = insertScheme("Inventory", EQASchemeType.INTER_LAB_SPLIT);
        try {
            insertPanel(scheme, p -> {
                p.setPanelName("Oversold panel");
                p.setAliquotsProduced(10);
                p.setAliquotsReserved(5);
                p.setAliquotsShipped(6);
            });
            fail("produced must be >= reserved + shipped (FR-V2.1-17)");
        } catch (Exception expected) {
            assertConstraint(expected, "eqa_panel_aliquots_chk");
        }
    }

    @Test
    public void unknownPanelStatusIsRejectedByTheDatabase() {
        EQAProgram scheme = insertScheme("Bad panel status", EQASchemeType.INTER_LAB_SPLIT);
        try {
            jdbc.update(
                    "INSERT INTO clinlims.eqa_panel (id, fhir_uuid, scheme_id, panel_name, status,"
                            + " aliquots_produced, aliquots_reserved, aliquots_shipped, homogeneity_qc_passed,"
                            + " sys_user_id, last_updated)"
                            + " VALUES (9981, gen_random_uuid(), ?, 'Bogus', 'ARCHIVED', 0, 0, 0, false, ?, now())",
                    scheme.getId(), USER);
            fail("the panel status CHECK should reject an unknown state");
        } catch (Exception expected) {
            assertConstraint(expected, "eqa_panel_status_chk");
        }
    }

    // ---- FR-V2.1-08 ----

    @Test
    public void anAnalystMayBeListedOncePerScheme() {
        EQAProgram scheme = insertScheme("Analyst list", EQASchemeType.INTERNATIONAL_PT);
        insertAnalyst(scheme, ADMIN_USER_ID);
        try {
            insertAnalyst(scheme, ADMIN_USER_ID);
            fail("the eligible-analyst list is a set, not a bag");
        } catch (Exception expected) {
            assertConstraint(expected, "uq_eqa_scheme_analyst_scheme_user");
        }
    }

    // ---- FR-V2.1-20 ----

    @Test
    public void oneReceiptPerCyclePerLab() {
        EQACycle cycle = insertCycle(insertScheme("Receipts", EQASchemeType.INTERNATIONAL_PT));
        insertReceipt(cycle);
        try {
            insertReceipt(cycle);
            fail("a lab confirms receipt of a cycle's panel exactly once");
        } catch (Exception expected) {
            assertConstraint(expected, "uq_eqa_panel_receipt_cycle_enrollment");
        }
    }

    @Test
    public void receiptWithoutAShipmentIsAllowed() {
        // Walk-in pickup and legacy imports have no tracked shipment.
        EQACycle cycle = insertCycle(insertScheme("Walk-in", EQASchemeType.INTERNATIONAL_PT));
        EQAPanelReceipt receipt = eqaPanelReceiptDAO.get(insertReceipt(cycle)).orElseThrow(AssertionError::new);
        assertEquals(null, receipt.getShipmentId());
        assertTrue("integrity defaults to ok", receipt.getIntegrityOk());
    }

    // ---- FR-V2.1-13 ----

    @Test
    public void oneOpenFollowupPerCyclePerOrganisation() {
        EQACycle cycle = insertCycle(insertScheme("Follow-ups", EQASchemeType.INTERNATIONAL_PT));
        insertFollowup(cycle);
        try {
            insertFollowup(cycle);
            fail("AC-V2.1-09: duplicate follow-up must be refused");
        } catch (Exception expected) {
            assertConstraint(expected, "uq_eqa_participant_followup_cycle_org");
        }
    }

    // ---- FR-V2.1-22 ----

    @Test
    public void competencyEventPersistsWithItsVocabulary() {
        EQAProgram scheme = insertScheme("Competency", EQASchemeType.INTERNATIONAL_PT);

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
        EQAProgram scheme = insertScheme("Bad event", EQASchemeType.INTERNATIONAL_PT);
        try {
            jdbc.update(
                    "INSERT INTO clinlims.eqa_analyst_competency_event (id, analyst_id, event_type, event_date,"
                            + " scheme_id, sys_user_id, last_updated)"
                            + " VALUES (9982, ?, 'unknown', current_date, ?, ?, now())",
                    ADMIN_USER_ID, scheme.getId(), USER);
            fail("AC-V2.1-22: an undefined event_type must be refused");
        } catch (Exception expected) {
            assertConstraint(expected, "eqa_competency_event_type_chk");
        }
    }

    // ---- helpers ----

    private EQAProgram insertScheme(String name, EQASchemeType type) {
        EQAProgram scheme = new EQAProgram();
        scheme.setName(name);
        scheme.setSchemeType(type);
        scheme.setProvider(type == EQASchemeType.IN_HOUSE ? null : "NHLS");
        scheme.setSysUserId(USER);
        scheme.setId(eqaProgramService.insert(scheme));
        return scheme;
    }

    private EQACycle insertCycle(EQAProgram scheme) {
        EQACycle cycle = new EQACycle();
        cycle.setScheme(scheme);
        cycle.setCycleNumber(1);
        cycle.setCreatedBy(systemUserService.get(String.valueOf(ADMIN_USER_ID)));
        cycle.setSysUserId(USER);
        return eqaCycleDAO.get(eqaCycleDAO.insert(cycle)).orElseThrow(AssertionError::new);
    }

    private EQAPanel insertPanel(EQAProgram scheme, java.util.function.Consumer<EQAPanel> customise) {
        EQAPanel panel = new EQAPanel();
        panel.setScheme(scheme);
        panel.setPanelName("Panel");
        panel.setSysUserId(USER);
        customise.accept(panel);
        panel.setId(eqaPanelDAO.insert(panel));
        return panel;
    }

    private void insertAnalyst(EQAProgram scheme, long userId) {
        EQASchemeAnalyst analyst = new EQASchemeAnalyst();
        analyst.setScheme(scheme);
        analyst.setSystemUserId(userId);
        analyst.setSysUserId(USER);
        eqaSchemeAnalystDAO.insert(analyst);
    }

    private Long insertReceipt(EQACycle cycle) {
        EQAPanelReceipt receipt = new EQAPanelReceipt();
        receipt.setCycle(cycle);
        receipt.setLabEnrollmentId(ENROLLMENT_ID);
        receipt.setReceivedDate(Date.valueOf("2026-08-14"));
        receipt.setReceivedBy(ADMIN_USER_ID);
        receipt.setSysUserId(USER);
        return eqaPanelReceiptDAO.insert(receipt);
    }

    private void insertFollowup(EQACycle cycle) {
        EQAParticipantFollowup followup = new EQAParticipantFollowup();
        followup.setScheme(cycle.getScheme());
        followup.setCycle(cycle);
        followup.setParticipantOrgId(ORG_ID);
        followup.setSysUserId(USER);
        eqaParticipantFollowupDAO.insert(followup);
    }

    private void assertConstraint(Exception e, String constraintName) {
        StringBuilder messages = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append(' ');
        }
        assertTrue("expected " + constraintName + ", got: " + messages, messages.toString().contains(constraintName));
    }
}
