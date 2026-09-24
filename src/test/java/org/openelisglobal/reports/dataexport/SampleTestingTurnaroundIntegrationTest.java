package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.StringWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SampleTestingTurnaroundIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    private AnalysisService analyses;
    @Autowired
    private ResultService results;
    @Autowired
    private TestResultService options;
    @Autowired
    private TestResultComponentService components;
    @Autowired
    private IStatusService statuses;
    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-sample-testing.xml");
        var specimen = analyses.get("1").getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-20 10:00:00"));
        specimen.setReceivedDate(Timestamp.valueOf("2026-08-20 11:00:00"));
        specimen.getSample().setEnteredDate(Date.valueOf("2026-08-20"));
    }

    private Analysis timed(Analysis analysis, String completed, String released) {
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analysis.setCompletedDate(completed == null ? null : Timestamp.valueOf("2026-08-20 " + completed));
        analysis.setReleasedDate(released == null ? null : Timestamp.valueOf("2026-08-20 " + released));
        analysis.setSysUserId(TEST_SYS_USER_ID);
        analyses.update(analysis);
        return analysis;
    }

    private Analysis repeat(Analysis first, String completed, String released) {
        Analysis repeat = new Analysis();
        repeat.setSampleItem(first.getSampleItem());
        repeat.setTest(first.getTest());
        repeat.setTestSection(first.getTestSection());
        repeat.setRevision("1");
        repeat.setAnalysisType("ROUTINE");
        repeat.setSysUserId(TEST_SYS_USER_ID);
        analyses.insert(repeat);
        return timed(repeat, completed, released);
    }

    private void reading(Analysis analysis, TestResult option, String value) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setTestResult(option);
        result.setResultType("N");
        result.setValue(value);
        result.setGrouping(0);
        result.setIsReportable("Y");
        result.setSysUserId(TEST_SYS_USER_ID);
        results.insert(result);
    }

    private String csv(String layout, List<String> ids, long expectedRows) throws Exception {
        entityManager.flush();
        entityManager.clear();
        var definition = catalog.definition("SAMPLE_TESTING");
        Map<String, ReportingVariable> available = catalog.variables(definition, layout).stream()
                .collect(Collectors.toMap(ReportingVariable::id, Function.identity()));
        assertTrue("Selected duration fields must be in the requested layout's catalog",
                available.keySet().containsAll(ids));
        var filter = new ExportFilter("2026-08-20", "2026-08-20", List.of("1", "2"), List.of(), List.of("FINALIZED"));
        var snapshot = new ExportSnapshot(definition, layout, ids.stream().map(available::get).toList(), filter,
                ZoneId.systemDefault().getId(), List.of(statuses.getStatusID(AnalysisStatus.Finalized)));
        StringWriter csv = new StringWriter();
        assertEquals(expectedRows, catalog.source(definition.source()).write(csv, snapshot));
        return csv.toString().substring(1);
    }

    @Test
    public void differentTestsShareASpecimenRowWithTheirOwnDurations() throws Exception {
        var blood = timed(analyses.get("1"), "11:30:00", "12:00:00");
        var urine = analyses.get("2");
        urine.setSampleItem(blood.getSampleItem());
        timed(urine, "12:30:00", "13:00:00");
        reading(blood, options.get("1"), "12");
        reading(urine, options.get("2"), "7");
        assertEquals(
                "Specimen ID,Collection to Received (min),Blood Test,Blood Test — Received to Validated (min),Urine Test,Urine Test — Received to Validated (min)\r\n"
                        + "1,60,12,60,7,120\r\n",
                csv("SPREADSHEET",
                        List.of("specimenId", "collectionToReceivedMinutes", "test:1",
                                "test:1:receivedToValidatedMinutes", "test:2", "test:2:receivedToValidatedMinutes"),
                        1));
    }

    @Test
    public void repeatedAnalysesKeepDifferentTimesWithoutPairingTheOtherTest() throws Exception {
        var blood = timed(analyses.get("1"), "11:30:00", "12:00:00");
        var urine = analyses.get("2");
        urine.setSampleItem(blood.getSampleItem());
        timed(urine, "12:30:00", "13:00:00");
        var repeated = repeat(blood, "14:00:00", "15:00:00");
        reading(blood, options.get("1"), "12");
        reading(repeated, options.get("1"), "12");
        reading(urine, options.get("2"), "7");
        assertEquals(
                "Specimen ID,Blood Test,Blood Test — Order to Result (min),Blood Test — Received to Validated (min),Blood Test — Resulted to Validated (min),Urine Test,Urine Test — Received to Validated (min)\r\n"
                        + "1,12,690,60,30,,\r\n1,12,840,240,60,,\r\n1,,,,,7,120\r\n",
                csv("SPREADSHEET",
                        List.of("specimenId", "test:1", "test:1:orderToResultMinutes",
                                "test:1:receivedToValidatedMinutes", "test:1:resultedToValidatedMinutes", "test:2",
                                "test:2:receivedToValidatedMinutes"),
                        3));
        assertEquals(
                "Test Name,Result Value,Order to Result (min),Received to Validated (min),Resulted to Validated (min)\r\n"
                        + "Blood Test,12,690,60,30\r\nUrine Test,7,750,120,30\r\nBlood Test,12,840,240,60\r\n",
                csv("RESULT_LIST", List.of("testName", "resultValue", "orderToResultMinutes",
                        "receivedToValidatedMinutes", "resultedToValidatedMinutes"), 3));
    }

    @Test
    public void durationOnlySelectionPreservesMissingRepeatedResultsAndNegativeIntervals() throws Exception {
        var first = timed(analyses.get("1"), null, null);
        reading(first, options.get("1"), "12");
        reading(first, options.get("1"), "12");
        var repeated = repeat(first, "11:00:00", "10:00:00");
        reading(repeated, options.get("1"), "13");
        assertEquals(
                "Specimen ID,Blood Test — Order to Result (min),Blood Test — Received to Validated (min),Blood Test — Resulted to Validated (min)\r\n"
                        + "1,,,\r\n1,,,\r\n1,660,-60,-60\r\n",
                csv("SPREADSHEET", List.of("specimenId", "test:1:orderToResultMinutes",
                        "test:1:receivedToValidatedMinutes", "test:1:resultedToValidatedMinutes"), 3));
    }

    @Test
    public void componentDurationUsesItsOwnRepeatedAnalysisAndSurvivesRename() throws Exception {
        TestResultComponent primary = new TestResultComponent();
        primary.setCode("PRIMARY");
        primary.setLabel("Primary value");
        primary.setDisplayOrder(0);
        primary.setResultType("N");
        TestResultComponent secondary = new TestResultComponent();
        secondary.setCode("SECONDARY");
        secondary.setLabel("Secondary value");
        secondary.setDisplayOrder(1);
        secondary.setResultType("N");
        var configured = components.saveSampleResults("1", List.of(primary, secondary), null, null, TEST_SYS_USER_ID);
        var component = configured.stream().filter(c -> c.getCode().equals("SECONDARY")).findFirst().orElseThrow();
        var option = options.getAllMatching("componentId", component.getId()).get(0);
        var analysis = timed(analyses.get("1"), "11:30:00", "12:00:00");
        reading(analysis, option, "6");
        reading(repeat(analysis, "14:00:00", "15:00:00"), option, "8");
        component.setLabel("Renamed secondary");
        components.saveComponentsForTest("1", configured, TEST_SYS_USER_ID);
        String id = "component:" + component.getId();
        assertEquals(
                "Blood Test — Renamed secondary,Blood Test — Renamed secondary — Received to Validated (min)\r\n6,60\r\n8,240\r\n",
                csv("SPREADSHEET", List.of(id, id + ":receivedToValidatedMinutes"), 2));
    }

    @Test
    public void catalogSeparatesPerTestIntervalsFromGenericDetailIntervalsWithoutSelectingFields() {
        var definition = catalog.definition("SAMPLE_TESTING");
        var spreadsheet = catalog.variables(definition, "SPREADSHEET").stream().map(ReportingVariable::id).toList();
        var detail = catalog.variables(definition, "RESULT_LIST").stream().map(ReportingVariable::id).toList();
        assertFalse(spreadsheet.contains("receivedToValidatedMinutes"));
        assertTrue(spreadsheet.contains("test:1:receivedToValidatedMinutes"));
        assertTrue(spreadsheet.contains("collectionToReceivedMinutes"));
        assertTrue(detail.contains("receivedToValidatedMinutes"));
        assertFalse(detail.contains("test:1:receivedToValidatedMinutes"));
        assertEquals(List.of(), catalog.defaultColumns(definition, "SPREADSHEET"));
        assertEquals(List.of(), catalog.defaultColumns(definition, "RESULT_LIST"));
    }

    @Test
    public void obsoleteCommonSpreadsheetDurationsRequireReviewBeforeSavingOrRunning() {
        for (String field : List.of("orderToResultMinutes", "receivedToValidatedMinutes",
                "resultedToValidatedMinutes")) {
            var filter = new ExportFilter("2026-08-20", "2026-08-20", List.of(), List.of(), List.of());
            var request = new ExportSubmission(1, "SAMPLE_TESTING", "SPREADSHEET", "old-duration-selection",
                    List.of("specimenId", field), filter);
            var submission = assertThrows(ReportingException.class, () -> catalog.freeze(request, TEST_SYS_USER_ID));
            assertEquals(422, submission.status());
            assertEquals("reporting.columns.stale", submission.getMessage());
            var definition = new SavedReportDefinition(1, "SAMPLE_TESTING", "SPREADSHEET", request.selectedVariables(),
                    null);
            var saved = assertThrows(ReportingException.class,
                    () -> catalog.validateSaved(TEST_SYS_USER_ID, definition));
            assertEquals(422, saved.status());
            assertEquals("reporting.columns.stale", saved.getMessage());
        }
    }
}
