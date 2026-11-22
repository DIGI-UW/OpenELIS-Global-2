package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.builder.BenchQCCaptureFormBuilder;
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
 * (which is what makes the manual/RDT split arithmetic rather than a branch),
 * and the shipped analyzer path is untouched by widening its table.
 *
 * <p>
 * Fixture {@code bench-qc-fail-signal.xml}, shared with the three sibling bench
 * tests: test 6601 in lab unit 701, the no-analyzer lot {@code bench-lot-a} on
 * MANUFACTURER_FIXED with mean 100 / SD 5 already computed, and technician 7701
 * distinct from the automation account (id 1) so "the acting user was recorded"
 * is an assertion with teeth. Only the analyzer is seeded here — the sibling
 * tests need none, and {@code qc_result.instrument_id} carries a foreign key to
 * it.
 */
public class BenchQCResultServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private static final String TEST_ID = "6601";
    private static final String SECTION_ID = "701";
    private static final String CONTROL_LOT_ID = "bench-lot-a";
    private static final int TECHNICIAN_USER_ID = 7701;
    // Clear of the fixture's own id range; the analyzer path needs a row to point
    // at.
    private static final String ANALYZER_ID = "7901";

    private static final BigDecimal LOT_MEAN = new BigDecimal("100.00000");

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/bench-qc-fail-signal.xml");
        jdbcTemplate.update("INSERT INTO analyzer (id, name, is_active, last_updated) VALUES (?, ?, true, NOW())",
                Long.parseLong(ANALYZER_ID), "BenchIntTestAnalyzer");
    }

    @After
    public void removeSeededAnalyzer() {
        // qc_result and qc_rule_violation both carry a foreign key to analyzer, so
        // the rows pointing at it go first.
        jdbcTemplate.update("DELETE FROM qc_rule_violation WHERE instrument_id = ?", Long.parseLong(ANALYZER_ID));
        jdbcTemplate.update("DELETE FROM qc_result WHERE instrument_id = ?", Long.parseLong(ANALYZER_ID));
        jdbcTemplate.update("DELETE FROM analyzer WHERE id = ?", Long.parseLong(ANALYZER_ID));
    }

    @Test
    public void manualQuantitativeControl_earnsZScoreAndSnapshotsTarget() {
        BenchQCCaptureForm capture = manualCapture(new BigDecimal("110.00000"), QCQualitativeOutcome.PASS);

        QCResult saved = qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);

        assertEquals(QCSource.MANUAL, saved.getSource());
        assertEquals(QCQualitativeOutcome.PASS, saved.getQualitativeOutcome());
        assertEquals(0, new BigDecimal("110.00000").compareTo(saved.getResultValue()));
        // (110 - 100) / 5 = 2.0000 — the same arithmetic the analyzer path uses, which
        // is what puts this point on the Levey-Jennings chart between the +1SD and
        // +3SD bands.
        assertEquals(0, new BigDecimal("2.0000").compareTo(saved.getZScore()));
        // The target in force at capture is copied onto the row, so a later edit of a
        // configured target cannot rewrite this run's history.
        assertEquals(0, LOT_MEAN.compareTo(saved.getExpectedValue()));
        assertEquals(0, new BigDecimal("5.00000").compareTo(saved.getUncertainty()));
        assertEquals(TEST_ID, saved.getTestId());
        assertEquals(SECTION_ID, saved.getTestSectionId());
        assertEquals(CONTROL_LOT_ID, saved.getControlLotId());
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
        BenchQCCaptureForm capture = BenchQCCaptureFormBuilder.create(QCSource.RDT).withTestId(TEST_ID)
                .withTestSectionId(SECTION_ID).withControlLabel("Determine HIV-1/2 · DET-2025-1102")
                .withOutcome(QCQualitativeOutcome.INVALID).withRunDateTime(LocalDateTime.now()).build();

        QCResult saved = qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);

        assertEquals(QCSource.RDT, saved.getSource());
        assertEquals(QCQualitativeOutcome.INVALID, saved.getQualitativeOutcome());
        // No synthetic number stands in for a qualitative outcome.
        assertNull(saved.getResultValue());
        // No number means no z-score, which is exactly what keeps RDT runs out of
        // Westgard rule evaluation without a single conditional.
        assertNull(saved.getZScore());
        assertNull(saved.getControlLotId());
        assertEquals("Determine HIV-1/2 · DET-2025-1102", saved.getControlLabel());
        assertEquals("REJECTED", saved.getResultStatus());
        assertEquals(Boolean.TRUE, saved.getNonConformityFlag());
    }

    @Test
    public void rdtControlCarryingAMeasuredValue_isRefused() {
        BenchQCCaptureForm capture = BenchQCCaptureFormBuilder.create(QCSource.RDT).withTestId(TEST_ID)
                .withTestSectionId(SECTION_ID).withOutcome(QCQualitativeOutcome.VALID)
                .withResultValue(new BigDecimal("1")).build();

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected an RDT control carrying a number to be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("must not carry a measured value"));
        }
    }

    /**
     * Inversion test for the constraint itself. The service refuses this
     * combination, but the service is not the only thing that can write the table —
     * this proves the database refuses it too, which makes the shape rule
     * structural rather than a convention.
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
        // PASS/FAIL belong to manual quantitative runs; an RDT control line reads
        // VALID/INVALID. The database CHECK cannot catch this, so the enum must.
        BenchQCCaptureForm capture = BenchQCCaptureFormBuilder.create(QCSource.RDT).withTestId(TEST_ID)
                .withTestSectionId(SECTION_ID).withOutcome(QCQualitativeOutcome.PASS).build();

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected PASS to be refused on an RDT control");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("not valid for source"));
        }
    }

    @Test
    public void manualQuantitativeControlWithoutALot_isRefused() {
        BenchQCCaptureForm capture = manualCaptureBuilder(new BigDecimal("101.00000"), QCQualitativeOutcome.PASS)
                .withControlLotId(null).build();

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected a manual quantitative control with no lot to be refused — the lot carries mean/SD");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("requires a control lot"));
        }
    }

    @Test
    public void analyzerSourcedControl_isRefusedOnTheBenchPath() {
        BenchQCCaptureForm capture = BenchQCCaptureFormBuilder.create(QCSource.ASTM).withTestId(TEST_ID)
                .withTestSectionId(SECTION_ID).withControlLotId(CONTROL_LOT_ID)
                .withResultValue(new BigDecimal("100.00000")).withOutcome(QCQualitativeOutcome.PASS).build();

        try {
            qcResultService.createBenchQCResult(capture, TECHNICIAN_USER_ID);
            fail("expected ASTM to be refused on the bench capture path");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("MANUAL or RDT"));
        }
    }

    /**
     * The analyzer path writes through the same table this story widened, so assert
     * it still lands as ASTM with its instrument and z-score intact — and that it
     * still carries the automation user rather than picking up a bench-path
     * default.
     */
    @Test
    public void analyzerPath_isUnchangedByTheNewColumns() {
        QCResult saved = qcResultService.createQCResult(ANALYZER_ID, TEST_ID, CONTROL_LOT_ID, "NORMAL",
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
        return manualCaptureBuilder(measured, outcome).build();
    }

    private BenchQCCaptureFormBuilder manualCaptureBuilder(BigDecimal measured, QCQualitativeOutcome outcome) {
        return BenchQCCaptureFormBuilder.create(QCSource.MANUAL).withTestId(TEST_ID).withTestSectionId(SECTION_ID)
                .withControlLotId(CONTROL_LOT_ID).withResultValue(measured).withUnitOfMeasure("mg/dL")
                .withOutcome(outcome).withTarget(LOT_MEAN, new BigDecimal("5.00000"))
                .withRunDateTime(LocalDateTime.now());
    }
}
