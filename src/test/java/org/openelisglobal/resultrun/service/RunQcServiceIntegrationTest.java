package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QcHoldService;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.qc.valueholder.RunControlVerdict;
import org.openelisglobal.qc.valueholder.RunVerdictSource;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.openelisglobal.resultrun.valueholder.ResultRunSource;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1200 slice 4: a control on a run gives each test it covers a verdict, a
 * failed test holds only that test's rows, and a hold is not cleared by a later
 * passing control (FR-C2, FR-C3, FR-C4, FR-C6).
 */
@Transactional
public class RunQcServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UNIT = "97790";
    private static final String TEST = "97791";
    private static final String OTHER_TEST = "97792";
    private static final String ANALYZER = "97793";
    private static final String LOT = "ogc1200-s4-lot-l1";

    @Autowired
    private RunQcService runQcService;
    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private ResultRunEventDAO resultRunEventDAO;
    @Autowired
    private QCResultDAO qcResultDAO;
    @Autowired
    private QcHoldService qcHoldService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private TestService testService;
    @Autowired
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;
    private ResultRun run;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?::numeric, ?, NOW())",
                UNIT, "Run QC unit");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, name_localization_id, is_active,"
                + " domain, lastupdated) VALUES (?::numeric, 'Run QC unit', 'Run QC unit', ?::numeric, 'Y',"
                + " 'CLINICAL', NOW())", UNIT, UNIT);
        for (String test : List.of(TEST, OTHER_TEST)) {
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable,"
                            + " test_section_id, lastupdated) VALUES (?::numeric, ?, ?, 'Y', ?, 'CLINICAL', true,"
                            + " ?::numeric, NOW())",
                    test, "Run QC " + test, "Run QC " + test, UUID.randomUUID().toString(), UNIT);
        }
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, test_unit_ids, last_updated)"
                + " VALUES (?::numeric, 'Run QC analyzer', true, ?, NOW())", ANALYZER, UNIT);
        jdbc.update("INSERT INTO clinlims.qc_control_lot (id, product_name, lot_number, control_level, test_id,"
                + " instrument_id, status, sys_user_id) VALUES (?, 'Level normal', 'LOT-N', 'NORMAL', ?::int, ?::int,"
                + " 'ACTIVE', 1)", LOT, TEST, ANALYZER);
        jdbc.update(
                "INSERT INTO clinlims.test_qc_target (id, test_id, control_level, expected_value, uncertainty,"
                        + " is_active) VALUES (?, ?::numeric, 'NORMAL', 100, 5, true)",
                UUID.randomUUID().toString(), TEST);

        run = new ResultRun();
        run.setSource(ResultRunSource.ANALYZER);
        run.setAnalyzerId(ANALYZER);
        run.setOpenedAt(new Timestamp(System.currentTimeMillis()));
        run.setLastActivityAt(new Timestamp(System.currentTimeMillis()));
        run.setSysUserId("1");
        resultRunService.insert(run);
        entityManager.flush();
    }

    private QCResult analyzerControl(String testId, String value) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO clinlims.qc_result (id, control_lot_id, test_id, instrument_id, source, result_value,"
                + " run_date_time, result_status, sys_user_id) VALUES (?, ?, ?::numeric, ?::numeric, 'ASTM',"
                + " ?::numeric, NOW(), 'PENDING', 1)", id, LOT, testId, ANALYZER, value);
        QCResult control = qcResultDAO.get(id).orElseThrow();
        runQcService.attachAnalyzerControl(control, run.getId(), null);
        entityManager.flush();
        return qcResultDAO.get(id).orElseThrow();
    }

    private List<String> events() {
        return resultRunEventDAO.getEvents(run.getId()).stream().map(ResultRunEvent::getEvent).toList();
    }

    @Test
    public void aControlOutsideItsTargetFailsAndHoldsOnlyItsTest() {
        QCResult control = analyzerControl(TEST, "120");

        assertEquals(RunControlVerdict.FAIL, control.getRunVerdict());
        assertEquals(RunVerdictSource.COMPUTED, control.getVerdictSource());
        assertEquals(0, new BigDecimal("100").compareTo(control.getExpectedValue()));
        assertEquals(run.getId(), control.getRunId());
        assertEquals(Set.of(TEST), runQcService.heldTestIds(run.getId()));
        assertEquals(RunTestVerdict.FAIL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(ResultRunState.HELD, resultRunService.get(run.getId()).getState());
        assertEquals(List.of(ResultRunEvent.CONTROL_RECORDED, ResultRunEvent.TEST_HELD), events());
        assertEquals("HELD", runQcService.qcBlock(run.getId()).overall());
    }

    @Test
    public void aControlWithinItsTargetPassesAndHoldsNothing() {
        QCResult control = analyzerControl(TEST, "103");

        assertEquals(RunControlVerdict.PASS, control.getRunVerdict());
        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertEquals(RunTestVerdict.PASS, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());
        assertEquals(List.of(ResultRunEvent.CONTROL_RECORDED), events());
    }

    @Test
    public void aLaterPassingControlDoesNotClearTheHold() {
        analyzerControl(TEST, "120");
        analyzerControl(TEST, "101");

        assertEquals(RunTestVerdict.FAIL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(ResultRunState.HELD, resultRunService.get(run.getId()).getState());
        assertEquals(
                List.of(ResultRunEvent.CONTROL_RECORDED, ResultRunEvent.TEST_HELD, ResultRunEvent.CONTROL_RECORDED),
                events());
    }

    @Test
    public void aControlTheTechnicianRecordsCarriesTheirVerdict() {
        analyzerControl(OTHER_TEST, "5");
        QCResult control = runQcService.recordControl(run.getId(), new BenchQCCaptureForm(QCSource.RDT, OTHER_TEST,
                UNIT, null, "Kit A", null, null, QCQualitativeOutcome.INVALID, null, null, null, null), 1);
        entityManager.flush();

        QCResult stored = qcResultDAO.get(control.getId()).orElseThrow();
        assertEquals(RunControlVerdict.FAIL, stored.getRunVerdict());
        assertEquals(RunVerdictSource.DECLARED, stored.getVerdictSource());
        assertEquals(Set.of(OTHER_TEST), runQcService.heldTestIds(run.getId()));
    }

    @Test
    public void aControlForATestTheRunDoesNotCoverIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> runQcService.recordControl(run.getId(), new BenchQCCaptureForm(QCSource.RDT, OTHER_TEST, UNIT,
                        null, "Kit A", null, null, QCQualitativeOutcome.INVALID, null, null, null, null), 1));
        assertTrue(runQcService.controlsOf(run.getId()).isEmpty());
    }

    @Test
    public void aMeasuredControlWhoseOutcomeContradictsItsNumbersIsCaught() {
        assertTrue(RunQcServiceImpl.contradictsItsNumbers(
                new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT, null, new BigDecimal("50"), null,
                        QCQualitativeOutcome.PASS, new BigDecimal("10"), BigDecimal.ONE, null, null)));
        assertFalse(RunQcServiceImpl.contradictsItsNumbers(
                new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT, null, new BigDecimal("50"), null,
                        QCQualitativeOutcome.FAIL, new BigDecimal("10"), BigDecimal.ONE, null, null)));
        assertFalse(RunQcServiceImpl.contradictsItsNumbers(
                new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT, null, new BigDecimal("10.5"), null,
                        QCQualitativeOutcome.PASS, new BigDecimal("10"), BigDecimal.ONE, null, null)));
    }

    @Test
    public void aResultStoredFromARunThatHoldsItsTestIsNotReleasedWhateverTheSiteSetting() {
        SampleItem item = fixtures.createSampleWithSampleItem("RQC");
        Analysis held = fixtures.createAnalysis(item, testService.get(TEST));
        Analysis other = fixtures.createAnalysis(item, testService.get(OTHER_TEST));
        for (Analysis analysis : List.of(held, other)) {
            jdbc.update(
                    "INSERT INTO clinlims.result (id, analysis_id, value, run_id, lastupdated) VALUES"
                            + " (nextval('clinlims.result_seq'), ?::numeric, '42', ?, now())",
                    analysis.getId(), run.getId());
        }
        entityManager.flush();
        assertEquals("a test whose result was already accepted is still covered by the run",
                RunTestVerdict.NOT_REQUIRED, runQcService.testVerdicts(run.getId()).get(TEST));
        analyzerControl(TEST, "120");

        assertEquals(Set.of(held.getId()),
                qcHoldService.analysisIdsBlockedFromRelease(List.of(held.getId(), other.getId())));

        TestResultItem heldRow = new TestResultItem();
        heldRow.setAnalysisId(held.getId());
        heldRow.setTestId(TEST);
        heldRow.setResultValue("42");
        TestResultItem otherRow = new TestResultItem();
        otherRow.setAnalysisId(other.getId());
        otherRow.setTestId(OTHER_TEST);
        otherRow.setResultValue("42");
        resultRunService.markWaiting(List.of(heldRow, otherRow));
        assertTrue("the worklist shows the stored result held, so it is not edited",
                heldRow.getWaitingRun() != null && heldRow.getWaitingRun().held());
        assertEquals(run.getId(), heldRow.getWaitingRun().runId());
        assertEquals(null, otherRow.getWaitingRun());
    }
}
