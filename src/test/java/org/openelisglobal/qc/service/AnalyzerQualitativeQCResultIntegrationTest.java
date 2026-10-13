package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * An analyzer control that reports an answer rather than a number, judged by
 * OpenELIS against the Test Catalog QC target, against a real PostgreSQL
 * instance.
 *
 * <p>
 * Fixture {@code bench-qc-fail-signal.xml} supplies test 6601; the analyzer and
 * its control lot are seeded here because {@code qc_result.instrument_id} and
 * the lot both point at an analyzer.
 */
public class AnalyzerQualitativeQCResultIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_ID = "6601";
    private static final String ANALYZER_ID = "7902";
    private static final String LOT_ID = "analyzer-qualitative-lot";
    private static final LocalDateTime RUN = LocalDateTime.of(2026, 10, 5, 9, 0);

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/bench-qc-fail-signal.xml");
        jdbcTemplate.update("INSERT INTO analyzer (id, name, is_active, last_updated) VALUES (?, ?, true, NOW())",
                Long.parseLong(ANALYZER_ID), "QualitativeQcAnalyzer");
        jdbcTemplate.update("INSERT INTO qc_control_lot (id, fhir_uuid, product_name, lot_number, control_level,"
                + " test_id, instrument_id, calculation_method, activation_date, status, sys_user_id, last_updated)"
                + " VALUES (?, ?::uuid, 'Positive control', 'POS-LOT-1', 'HIGH', ?, ?, 'MANUFACTURER_FIXED', NOW(),"
                + " 'ACTIVE', 1, NOW())", LOT_ID, UUID.randomUUID().toString(), Long.parseLong(TEST_ID),
                Long.parseLong(ANALYZER_ID));
    }

    @After
    public void removeSeededAnalyzer() {
        // A failing control raises its signal after commit: a violation, its alert and
        // an NCE keyed by the violation.
        String violations = "(SELECT id FROM qc_rule_violation WHERE instrument_id = ?)";
        jdbcTemplate.update("DELETE FROM qc_alert WHERE violation_id IN " + violations, Long.parseLong(ANALYZER_ID));
        jdbcTemplate.update("DELETE FROM clinlims.nc_event WHERE trigger_source_id IN " + violations,
                Long.parseLong(ANALYZER_ID));
        jdbcTemplate.update("DELETE FROM qc_rule_violation WHERE instrument_id = ?", Long.parseLong(ANALYZER_ID));
        jdbcTemplate.update("DELETE FROM qc_result WHERE instrument_id = ?", Long.parseLong(ANALYZER_ID));
        jdbcTemplate.update("DELETE FROM qc_control_lot WHERE id = ?", LOT_ID);
        jdbcTemplate.update("DELETE FROM analyzer WHERE id = ?", Long.parseLong(ANALYZER_ID));
    }

    @Test
    public void aPassingAnalyzerControlIsStoredWithItsOutcomeAndNoNumber() {
        QCResult saved = qcResultService.createAnalyzerQualitativeQCResult(ANALYZER_ID, TEST_ID, LOT_ID,
                QCQualitativeOutcome.PASS, RUN);

        assertEquals(QCSource.ASTM, saved.getSource());
        assertEquals(QCQualitativeOutcome.PASS, saved.getQualitativeOutcome());
        assertNull(saved.getResultValue());
        assertNull("no number, so nothing to plot or evaluate", saved.getZScore());
        assertEquals(ANALYZER_ID, saved.getInstrumentId());
        assertEquals(LOT_ID, saved.getControlLotId());
        assertEquals("ACCEPTED", saved.getResultStatus());
        assertEquals(Boolean.FALSE, saved.getNonConformityFlag());
    }

    @Test
    public void aFailingAnalyzerControlIsRejectedNonConformingAndRaisesAViolation() throws Exception {
        QCResult saved = qcResultService.createAnalyzerQualitativeQCResult(ANALYZER_ID, TEST_ID, LOT_ID,
                QCQualitativeOutcome.FAIL, RUN);

        assertEquals(QCQualitativeOutcome.FAIL, saved.getQualitativeOutcome());
        assertEquals("REJECTED", saved.getResultStatus());
        assertEquals(Boolean.TRUE, saved.getNonConformityFlag());
        assertEquals("the wrong answer enters corrective action like a manual Fail", "QUALITATIVE_FAIL",
                awaitViolationRuleCode(saved.getId()));
    }

    private String awaitViolationRuleCode(String resultId) throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT rule_code FROM qc_rule_violation WHERE triggering_result_id = ?", String.class, resultId);
            if (!codes.isEmpty()) {
                return codes.get(0);
            }
            Thread.sleep(100);
        }
        return null;
    }

    @Test
    public void anAnalyzerControlRefusesAnOutcomeThatOnlyARapidTestCanHave() {
        assertThrows(IllegalArgumentException.class, () -> qcResultService
                .createAnalyzerQualitativeQCResult(ANALYZER_ID, TEST_ID, LOT_ID, QCQualitativeOutcome.VALID, RUN));
    }

    /**
     * The database still refuses an analyzer row with neither a number nor an
     * outcome, so relaxing the shape rule for qualitative analyzer controls lets
     * through only the row it was meant to.
     */
    @Test
    public void database_refusesAnAnalyzerRowWithNeitherANumberNorAnOutcome() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO qc_result (id, source, test_id, instrument_id, control_lot_id, run_date_time,"
                            + " result_status, sys_user_id, last_updated)"
                            + " VALUES (?, 'ASTM', ?, ?, ?, NOW(), 'PENDING', 1, NOW())",
                    UUID.randomUUID().toString(), Long.parseLong(TEST_ID), Long.parseLong(ANALYZER_ID), LOT_ID);
            fail("expected chk_qc_result_source_shape to reject an analyzer row with no value and no outcome");
        } catch (DataIntegrityViolationException e) {
            assertTrue(String.valueOf(e.getMessage()), String.valueOf(e.getMessage()).contains("chk_qc_result_source"));
        }
    }

    @Test
    public void lotHistoryUsedForStatisticsAndRulesHoldsOnlyMeasuredRuns() {
        jdbcTemplate.update(
                "INSERT INTO qc_result (id, source, test_id, instrument_id, control_lot_id, result_value,"
                        + " run_date_time, result_status, sys_user_id, last_updated)"
                        + " VALUES ('measured-run', 'ASTM', ?, ?, ?, 101.5, NOW(), 'PENDING', 1, NOW())",
                Long.parseLong(TEST_ID), Long.parseLong(ANALYZER_ID), LOT_ID);
        qcResultService.createAnalyzerQualitativeQCResult(ANALYZER_ID, TEST_ID, LOT_ID, QCQualitativeOutcome.PASS, RUN);

        assertEquals(List.of("measured-run"), ids(resultDAO.findByControlLot(LOT_ID)));
        assertEquals(List.of("measured-run"), ids(resultDAO.findByControlLotIdOrderByRunDateTime(LOT_ID)));
        assertEquals(List.of("measured-run"), ids(resultDAO.findHistoricalForRule(LOT_ID, 10)));
        assertEquals(0, new BigDecimal("101.5").compareTo(resultDAO.findByControlLot(LOT_ID).get(0).getResultValue()));
    }

    private static List<String> ids(List<QCResult> results) {
        return results.stream().map(QCResult::getId).toList();
    }
}
