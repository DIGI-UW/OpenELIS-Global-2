package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1147 — bench control capture against a real PostgreSQL instance.
 *
 * <p>
 * The three claims that matter and cannot be checked any other way: a manual
 * quantitative control earns a z-score (so it plots on Levey-Jennings and
 * reaches the Westgard engine with no new wiring), an RDT control never does
 * (which is what makes decision D4's split arithmetic rather than a branch),
 * and the shipped analyzer path is untouched by widening its table (NFR-1).
 */
public class BenchQCResultServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbcTemplate;

    // High ids to stay clear of seeded reference data.
    private static final String TEST_ID = "90101";
    private static final String ANALYZER_ID = "90101";
    private static final String SECTION_ID = "90101";
    private static final String ORG_ID = "90101";
    // test_section needs its own localization row, distinct from the test's.
    private static final long SECTION_LOCALIZATION_ID = 90102L;
    // Distinct from SYSTEM_AUTOMATION_USER_ID (1) on purpose: the whole point of
    // the bench
    // path is that the acting technician is recorded, not the automation account.
    private static final int TECHNICIAN_USER_ID = 90101;

    private static final BigDecimal LOT_MEAN = new BigDecimal("100.00000");
    private static final BigDecimal LOT_SD = new BigDecimal("5.00000");

    private String controlLotId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        entityManager.clear();
        cleanTestData();
        seedParentRows();
        controlLotId = seedActiveFixedValueLot();
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    @Test
    public void manualQuantitativeControl_earnsZScoreAndSnapshotsTarget() {
        BenchQCCaptureForm capture = manualCapture(new BigDecimal("110.00000"), QCQualitativeOutcome.PASS);

        QCResult saved = qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);

        assertEquals(QCSource.MANUAL, saved.getSource());
        assertEquals(QCQualitativeOutcome.PASS, saved.getQualitativeOutcome());
        assertEquals(0, new BigDecimal("110.00000").compareTo(saved.getResultValue()));
        // (110 - 100) / 5 = 2.0000 — the same arithmetic the analyzer path uses, which
        // is
        // what puts this point on the Levey-Jennings chart between the +1SD and +3SD
        // bands.
        assertEquals(0, new BigDecimal("2.0000").compareTo(saved.getZScore()));
        // FR-B2: the target in force at capture is copied onto the row, so a later edit
        // of a
        // configured target cannot rewrite this run's history.
        assertEquals(0, LOT_MEAN.compareTo(saved.getExpectedValue()));
        assertEquals(0, new BigDecimal("5.00000").compareTo(saved.getUncertainty()));
        assertEquals(TEST_ID, saved.getTestId());
        assertEquals(SECTION_ID, saved.getTestSectionId());
        assertEquals(controlLotId, saved.getControlLotId());
        assertEquals("ACCEPTED", saved.getResultStatus());
        assertEquals(Boolean.FALSE, saved.getNonConformityFlag());
        assertEquals(Integer.valueOf(TECHNICIAN_USER_ID), saved.getSystemUserId());
        assertEquals(Integer.valueOf(TECHNICIAN_USER_ID), saved.getTechnicianId());
        // A bench run has no instrument — the reason instrument_id had to become
        // nullable.
        assertNull(saved.getInstrumentId());
    }

    @Test
    public void failingManualControl_isRecordedAsRejectedAndNonConforming() {
        BenchQCCaptureForm capture = manualCapture(new BigDecimal("140.00000"), QCQualitativeOutcome.FAIL);

        QCResult saved = qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);

        assertEquals(QCQualitativeOutcome.FAIL, saved.getQualitativeOutcome());
        assertEquals("REJECTED", saved.getResultStatus());
        assertEquals(Boolean.TRUE, saved.getNonConformityFlag());
        // Still quantitative, so it still plots: (140 - 100) / 5 = 8.0000.
        assertEquals(0, new BigDecimal("8.0000").compareTo(saved.getZScore()));
    }

    @Test
    public void rdtControl_storesOutcomeWithNoNumberAndNoZScore() {
        BenchQCCaptureForm capture = new BenchQCCaptureForm();
        capture.setSource(QCSource.RDT);
        capture.setTestId(TEST_ID);
        capture.setTestSectionId(SECTION_ID);
        capture.setControlLabel("Determine HIV-1/2 · DET-2025-1102");
        capture.setQualitativeOutcome(QCQualitativeOutcome.INVALID);
        capture.setRunDateTime(LocalDateTime.now());

        QCResult saved = qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);

        assertEquals(QCSource.RDT, saved.getSource());
        assertEquals(QCQualitativeOutcome.INVALID, saved.getQualitativeOutcome());
        // FR-A3: no synthetic number stands in for a qualitative outcome.
        assertNull(saved.getResultValue());
        // No number means no z-score, which is exactly what keeps RDT runs out of
        // Westgard
        // rule evaluation without a single conditional (decision D4).
        assertNull(saved.getZScore());
        assertNull(saved.getControlLotId());
        assertEquals("Determine HIV-1/2 · DET-2025-1102", saved.getControlLabel());
        assertEquals("REJECTED", saved.getResultStatus());
        assertEquals(Boolean.TRUE, saved.getNonConformityFlag());
    }

    @Test
    public void rdtControlCarryingAMeasuredValue_isRefused() {
        BenchQCCaptureForm capture = new BenchQCCaptureForm();
        capture.setSource(QCSource.RDT);
        capture.setTestId(TEST_ID);
        capture.setTestSectionId(SECTION_ID);
        capture.setQualitativeOutcome(QCQualitativeOutcome.VALID);
        capture.setResultValue(new BigDecimal("1"));

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected an RDT control carrying a number to be refused (FR-A3)");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("must not carry a measured value"));
        }
    }

    /**
     * Inversion test for the constraint itself. The service refuses this
     * combination, but the service is not the only thing that can write the table —
     * this proves the database refuses it too, which is what makes FR-A3 structural
     * rather than a convention.
     */
    @Test
    public void database_refusesQualitativeRowCarryingAValue() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO qc_result (id, source, qualitative_outcome, test_id, result_value, "
                            + "run_date_time, result_status, sys_user_id, last_updated) "
                            + "VALUES (?, 'RDT', 'VALID', ?, 42, NOW(), 'ACCEPTED', ?, NOW())",
                    UUID.randomUUID().toString(), Long.parseLong(TEST_ID), TECHNICIAN_USER_ID);
            fail("expected chk_qc_result_source_shape to reject an RDT row carrying result_value");
        } catch (DataIntegrityViolationException e) {
            assertTrue(String.valueOf(e.getMessage()), String.valueOf(e.getMessage()).contains("chk_qc_result_source"));
        }
    }

    /**
     * The other half of the constraint: relaxing {@code result_value} must not let
     * a quantitative row through without a number, because every existing reader of
     * that column assumes one is present.
     */
    @Test
    public void database_refusesQuantitativeRowWithoutAValue() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO qc_result (id, source, test_id, run_date_time, result_status, "
                            + "sys_user_id, last_updated) " + "VALUES (?, 'MANUAL', ?, NOW(), 'ACCEPTED', ?, NOW())",
                    UUID.randomUUID().toString(), Long.parseLong(TEST_ID), TECHNICIAN_USER_ID);
            fail("expected chk_qc_result_source_shape to reject a MANUAL row with no result_value");
        } catch (DataIntegrityViolationException e) {
            assertTrue(String.valueOf(e.getMessage()), String.valueOf(e.getMessage()).contains("chk_qc_result_source"));
        }
    }

    @Test
    public void database_refusesAnUnknownSource() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO qc_result (id, source, test_id, result_value, run_date_time, "
                            + "result_status, sys_user_id, last_updated) "
                            + "VALUES (?, 'WORKPLAN', ?, 12, NOW(), 'ACCEPTED', ?, NOW())",
                    UUID.randomUUID().toString(), Long.parseLong(TEST_ID), TECHNICIAN_USER_ID);
            fail("expected chk_qc_result_source to reject a source outside the QCSource enum");
        } catch (DataIntegrityViolationException e) {
            assertTrue(String.valueOf(e.getMessage()), String.valueOf(e.getMessage()).contains("chk_qc_result_source"));
        }
    }

    @Test
    public void outcomeFromTheWrongVocabulary_isRefused() {
        BenchQCCaptureForm capture = new BenchQCCaptureForm();
        capture.setSource(QCSource.RDT);
        capture.setTestId(TEST_ID);
        capture.setTestSectionId(SECTION_ID);
        // PASS/FAIL belong to manual quantitative runs; an RDT control line reads
        // VALID/INVALID. The database CHECK cannot catch this, so the enum must.
        capture.setQualitativeOutcome(QCQualitativeOutcome.PASS);

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected PASS to be refused on an RDT control");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("not valid for source"));
        }
    }

    @Test
    public void manualQuantitativeControlWithoutALot_isRefused() {
        BenchQCCaptureForm capture = manualCapture(new BigDecimal("101.00000"), QCQualitativeOutcome.PASS);
        capture.setControlLotId(null);

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected a manual quantitative control with no lot to be refused — the lot carries mean/SD");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("requires a control lot"));
        }
    }

    @Test
    public void analyzerSourcedControl_isRefusedOnTheBenchPath() {
        BenchQCCaptureForm capture = manualCapture(new BigDecimal("100.00000"), QCQualitativeOutcome.PASS);
        capture.setSource(QCSource.ASTM);

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected ASTM to be refused on the bench capture path");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("MANUAL or RDT"));
        }
    }

    /**
     * NFR-1. The analyzer path writes through the same table this story widened, so
     * assert it still lands as ASTM with its instrument and z-score intact — and
     * that it still carries the automation user rather than picking up a bench-path
     * default.
     */
    @Test
    public void analyzerPath_isUnchangedByTheNewColumns() {
        QCResult saved = qcResultService.createQCResult(ANALYZER_ID, TEST_ID, controlLotId, "NORMAL",
                new BigDecimal("95.00000"), "mg/dL", LocalDateTime.now());

        assertEquals(QCSource.ASTM, saved.getSource());
        assertEquals(ANALYZER_ID, saved.getInstrumentId());
        assertEquals(0, new BigDecimal("95.00000").compareTo(saved.getResultValue()));
        // (95 - 100) / 5 = -1.0000
        assertEquals(0, new BigDecimal("-1.0000").compareTo(saved.getZScore()));
        assertNull(saved.getQualitativeOutcome());
        assertEquals(Integer.valueOf(1), saved.getSystemUserId());
    }

    // ---------------- helpers ----------------

    private BenchQCCaptureForm manualCapture(BigDecimal measured, QCQualitativeOutcome outcome) {
        BenchQCCaptureForm capture = new BenchQCCaptureForm();
        capture.setSource(QCSource.MANUAL);
        capture.setTestId(TEST_ID);
        capture.setTestSectionId(SECTION_ID);
        capture.setControlLotId(controlLotId);
        capture.setResultValue(measured);
        capture.setUnitOfMeasure("mg/dL");
        capture.setQualitativeOutcome(outcome);
        capture.setExpectedValue(LOT_MEAN);
        capture.setUncertainty(new BigDecimal("5.00000"));
        capture.setRunDateTime(LocalDateTime.now());
        return capture;
    }

    /**
     * An ACTIVE lot on the MANUFACTURER_FIXED strategy with statistics already
     * present. That is decision D3 in practice: manual methods get their mean/SD
     * from tech-entered or configured targets, not from a 20-run establishment
     * protocol.
     */
    private String seedActiveFixedValueLot() {
        String lotId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO qc_control_lot (id, product_name, lot_number, control_level, test_id, "
                        + "calculation_method, manufacturer_mean, manufacturer_std_dev, status, sys_user_id, "
                        + "last_updated) VALUES (?, ?, ?, ?, ?, 'MANUFACTURER_FIXED', ?, ?, 'ACTIVE', ?, NOW())",
                lotId, "IntTest Control", "QC-2026-031", "NORMAL", Long.parseLong(TEST_ID), LOT_MEAN, LOT_SD,
                TECHNICIAN_USER_ID);
        jdbcTemplate.update(
                // last_updated, not lastupdated: renamed across the qc_* tables by qc-012.
                "INSERT INTO qc_statistics (id, control_lot_id, calculation_date, mean, standard_deviation, "
                        + "num_values, calculation_method, validity_start, sys_user_id, last_updated) "
                        + "VALUES (?, ?, NOW(), ?, ?, 1, 'MANUFACTURER_FIXED', NOW(), ?, NOW())",
                UUID.randomUUID().toString(), lotId, LOT_MEAN, LOT_SD, TECHNICIAN_USER_ID);
        return lotId;
    }

    private void seedParentRows() {
        long testIdNum = Long.parseLong(TEST_ID);
        long analyzerIdNum = Long.parseLong(ANALYZER_ID);
        long sectionIdNum = Long.parseLong(SECTION_ID);
        long orgIdNum = Long.parseLong(ORG_ID);

        // The automation user (id 1) is required by the analyzer path; the technician
        // is a
        // separate row so "the acting user was recorded" is an assertion with teeth.
        insertUserIfAbsent(1, "automationUser");
        insertUserIfAbsent(TECHNICIAN_USER_ID, "benchTech");

        if (count("SELECT COUNT(*) FROM analyzer WHERE id = ?", analyzerIdNum) == 0) {
            jdbcTemplate.update("INSERT INTO analyzer (id, name, is_active, last_updated) VALUES (?, ?, ?, NOW())",
                    analyzerIdNum, "IntTestAnalyzer-" + ANALYZER_ID, true);
        }

        if (count("SELECT COUNT(*) FROM organization WHERE id = ?", orgIdNum) == 0) {
            jdbcTemplate.update("INSERT INTO organization (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', NOW())",
                    orgIdNum, "IntTestOrg-" + ORG_ID);
        }

        // Both test and test_section carry a NOT NULL name_localization_id, so each
        // needs
        // its own localization row seeded first.
        if (count("SELECT COUNT(*) FROM test_section WHERE id = ?", sectionIdNum) == 0) {
            seedLocalization(SECTION_LOCALIZATION_ID, "IntTestSect");
            jdbcTemplate.update(
                    "INSERT INTO test_section (id, name, description, org_id, is_active, "
                            + "name_localization_id, lastupdated) VALUES (?, ?, ?, ?, 'Y', ?, NOW())",
                    sectionIdNum, "IntTestSect", "IntegrationTest section", orgIdNum, SECTION_LOCALIZATION_ID);
        }

        if (count("SELECT COUNT(*) FROM test WHERE id = ?", testIdNum) == 0) {
            seedLocalization(testIdNum, "IntTest-" + TEST_ID);
            jdbcTemplate.update(
                    "INSERT INTO test (id, name, description, is_active, guid, name_localization_id, lastupdated) "
                            + "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    testIdNum, "IntTest-" + TEST_ID, "IntegrationTest-" + TEST_ID, "Y", UUID.randomUUID().toString(),
                    testIdNum);
        }
    }

    private void seedLocalization(long id, String englishValue) {
        jdbcTemplate.update("INSERT INTO localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                englishValue);
        jdbcTemplate.update("INSERT INTO localization_value (id, localization_id, locale, value) VALUES (?, ?, ?, ?)",
                id, id, "en", englishValue);
    }

    private void insertUserIfAbsent(int id, String loginName) {
        if (count("SELECT COUNT(*) FROM system_user WHERE id = ?", (long) id) == 0) {
            jdbcTemplate
                    .update("INSERT INTO system_user (id, login_name, first_name, last_name, is_active, is_employee, "
                            + "lastupdated) VALUES (?, ?, ?, ?, 'Y', 'Y', NOW())", id, loginName, "Int", "Test");
        }
    }

    private int count(String sql, Long arg) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, arg);
        return n == null ? 0 : n;
    }

    private void cleanTestData() {
        String testId = TEST_ID;
        try {
            // qc_alert FKs qc_rule_violation without CASCADE; an undelete-able violation
            // used to abort this whole cleanup silently, leaving the seeded lot behind —
            // harmless until uq_qc_control_lot_active made leftovers a duplicate key.
            jdbcTemplate.update("DELETE FROM qc_alert WHERE violation_id IN "
                    + "(SELECT id FROM qc_rule_violation WHERE test_id = " + testId + ")");
            jdbcTemplate.update("DELETE FROM qc_rule_violation WHERE test_id = " + testId);
            jdbcTemplate.update("DELETE FROM qc_statistics WHERE control_lot_id IN "
                    + "(SELECT id FROM qc_control_lot WHERE test_id = " + testId + ")");
            jdbcTemplate.update("DELETE FROM qc_result WHERE test_id = " + testId);
            jdbcTemplate.update("DELETE FROM qc_control_lot WHERE test_id = " + testId);
            jdbcTemplate.update("DELETE FROM test WHERE id = " + testId);
            jdbcTemplate.update("DELETE FROM localization_value WHERE localization_id = " + testId);
            jdbcTemplate.update("DELETE FROM localization WHERE id = " + testId);
            jdbcTemplate.update("DELETE FROM test_section WHERE id = " + SECTION_ID);
            jdbcTemplate.update("DELETE FROM localization_value WHERE localization_id = " + SECTION_LOCALIZATION_ID);
            jdbcTemplate.update("DELETE FROM localization WHERE id = " + SECTION_LOCALIZATION_ID);
            jdbcTemplate.update("DELETE FROM organization WHERE id = " + ORG_ID);
            jdbcTemplate.update("DELETE FROM analyzer WHERE id = " + ANALYZER_ID);
            jdbcTemplate.update("DELETE FROM system_user WHERE id = " + TECHNICIAN_USER_ID);
        } catch (Exception e) {
            System.out.println("Failed to clean bench QC test data: " + e.getMessage());
        }
    }
}
