package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1200 FR-B3, FR-D7: a result saved on a row in no run is a run of one, and
 * a control recorded on that row before the save opens it, holds the row while
 * it fails and is completed by the save.
 */
public class ResultRunOfOneIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACTOR = "1";

    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private RunQcService runQcService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;

    private String testId;
    private String testSectionId;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/panel-item.xml");
        org.openelisglobal.test.valueholder.Test test = testService.getAllActiveTests(false).stream()
                .filter(candidate -> candidate.getTestSection() != null).findFirst().orElseThrow();
        testId = test.getId();
        testSectionId = test.getTestSection().getId();
    }

    @Test
    public void aFirstValueSavedOnARowInNoRunIsARunOfOne() {
        String analysisId = createAnalysis();
        Result saved = storeResult(analysisId, "81");

        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));

        Long runId = runOf(saved);
        assertNotNull("the saved result records into a run of one", runId);
        Map<String, Object> run = jdbcTemplate.queryForMap(
                "SELECT source, state, CAST(analysis_id AS VARCHAR) AS analysis, CAST(test_section_id AS VARCHAR)"
                        + " AS unit, row_count FROM clinlims.result_run WHERE id = ?",
                runId);
        assertEquals("RESULT_ENTRY", run.get("source"));
        assertEquals("the run is completed in the same save", "COMPLETE", run.get("state"));
        assertEquals(analysisId, run.get("analysis"));
        assertEquals(testSectionId, run.get("unit"));
        assertEquals(1, ((Number) run.get("row_count")).intValue());
        assertEquals(Map.of(saved.getId(), runId), resultRunService.runsOfResults(List.of(saved.getId())));
    }

    @Test
    public void anEditOfAStoredResultMakesNoRun() {
        Result edited = storeResult(createAnalysis(), "81");

        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(), List.of(edited)));

        assertNull(runOf(edited));
    }

    @Test
    public void aControlRecordedOnARowOpensItsRunOfOneWhichHoldsTheRowUntilDecidedAndTheSaveCompletes() {
        String analysisId = createAnalysis();

        QCResult control = resultRunService.recordRowControl(analysisId, invalidControl(), viewer(), ACTOR);

        Long runId = control.getRunId();
        assertNotNull("the control opens the row's run of one", runId);
        assertEquals("HELD", stateOf(runId));
        assertEquals("a failed control holds the row at entry", Map.of(analysisId, runId),
                runQcService.entryHoldingRuns(List.of(analysisId)));
        assertEquals("a second control goes to the same run", runId,
                resultRunService.recordRowControl(analysisId, invalidControl(), viewer(), ACTOR).getRunId());

        runQcService.decide(runId, testId, RunTestAction.ACCEPT_DESPITE, "Control repeated and passed", true, ACTOR);
        assertTrue(runQcService.entryHoldingRuns(List.of(analysisId)).isEmpty());
        Result saved = storeResult(analysisId, "82");
        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));

        assertEquals("the save records into the run the control opened", runId, runOf(saved));
        assertEquals("COMPLETE", stateOf(runId));
    }

    @Test
    public void aRowWhoseResultIsStoredOrWhoseTestIsNotTheViewersOpensNoRun() {
        String resulted = createAnalysis();
        storeResult(resulted, "81");
        try {
            resultRunService.recordRowControl(resulted, invalidControl(), viewer(), ACTOR);
            fail("a stored row opens no run");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("already saved"));
        }
        try {
            resultRunService.recordRowControl(createAnalysis(), invalidControl(),
                    new RunViewer(Set.of(), Set.of(testSectionId)), ACTOR);
            fail("another unit's row opens no run");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not one of your lab units"));
        }
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*)::int FROM clinlims.result_run WHERE CAST(analysis_id AS VARCHAR) = ?",
                        Integer.class, resulted));
    }

    private BenchQCCaptureForm invalidControl() {
        return new BenchQCCaptureForm(QCSource.RDT, testId, testSectionId, null, "KIT-" + UUID.randomUUID(), null, null,
                QCQualitativeOutcome.INVALID, null, null, LocalDateTime.now().minusMinutes(1), "OGC-1200 row control");
    }

    private RunViewer viewer() {
        return new RunViewer(Set.of(testId), Set.of(testSectionId));
    }

    private Long runOf(Result result) {
        return jdbcTemplate.queryForObject("SELECT run_id FROM clinlims.result WHERE id = CAST(? AS numeric)",
                Long.class, result.getId());
    }

    private String stateOf(Long runId) {
        return jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId);
    }

    private Result storeResult(String analysisId, String value) {
        String id = jdbcTemplate.queryForObject("SELECT CAST(nextval('clinlims.result_seq') AS VARCHAR)", String.class);
        jdbcTemplate.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated)"
                + " VALUES (CAST(? AS numeric), CAST(? AS numeric), ?, 'N', now())", id, analysisId, value);
        return resultService.get(id);
    }

    private IResultSaveService saving(List<Result> created, List<Result> modified) {
        return new IResultSaveService() {
            @Override
            public String getCurrentUserId() {
                return ACTOR;
            }

            @Override
            public List<ResultSet> getNewResults() {
                return created.stream().map(result -> new ResultSet(result, null, null, null, null, null, false))
                        .toList();
            }

            @Override
            public List<ResultSet> getModifiedResults() {
                return modified.stream().map(result -> new ResultSet(result, null, null, null, null, null, false))
                        .toList();
            }
        };
    }

    private String createAnalysis() {
        String statusId = jdbcTemplate.queryForObject("SELECT id FROM clinlims.status_of_sample ORDER BY id LIMIT 1",
                String.class);
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());

        Sample sample = new Sample();
        sample.setAccessionNumber("OGC1200" + UUID.randomUUID().toString().substring(0, 6));
        sample.setDomain("H");
        sample.setEnteredDate(today);
        sample.setReceivedDate(today);
        sample.setSysUserId(ACTOR);
        String sampleId = sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusId);
        sampleItem.setSysUserId(ACTOR);
        String sampleItemId = sampleItemService.insert(sampleItem);

        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItemService.get(sampleItemId));
        analysis.setTest(testService.get(testId));
        analysis.setStatusId(statusId);
        analysis.setAnalysisType("MANUAL");
        analysis.setRevision("1");
        analysis.setIsReportable("Y");
        analysis.setSysUserId(ACTOR);
        return analysisService.insert(analysis);
    }
}
