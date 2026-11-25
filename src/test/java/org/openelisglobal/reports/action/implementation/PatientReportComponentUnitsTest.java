package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportComponentLine;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * The patient report prints each part of a multi-part result with that part's
 * own unit, as Results Entry and Validation show it. Fixture:
 * {@code testdata/component-units.xml} — a viral load in copies/ml and its Ct,
 * which has no unit.
 */
@Transactional
public class PatientReportComponentUnitsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private TestResultComponentService testResultComponentService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/component-units.xml");
    }

    @Test
    public void eachPrintedPartCarriesItsOwnUnit() {
        PatientReport report = new PatientCILNSPClinical_vreduit();
        Analysis analysis = analysisService.get("100");
        ReflectionTestUtils.setField(report, "currentAnalysis", analysis);
        ReflectionTestUtils.setField(report, "currentPatient", new Patient());
        ClinicalPatientData row = new ClinicalPatientData();

        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", resultService.getResultsByAnalysis(analysis),
                testResultComponentService.getActiveComponentsByTestId("1"), row);

        List<String> parts = List.of(row.getResult().split("\n")).stream().map(line -> line.split(":")[0]).toList();
        assertEquals(List.of("Viral load", "Ct"), parts);
        assertEquals(List.of("cp/ml VCU", ""), List.of(row.getUom().split("\n", -1)));
    }

    /**
     * A part the report carries but that has no result yet is printed as still in
     * progress, not dropped as if it had never been ordered.
     */
    @Test
    public void aPartWithoutAResultIsPrintedAsInProgress() {
        PatientReport report = report();
        Analysis analysis = analysisService.get("100");
        ReflectionTestUtils.setField(report, "currentAnalysis", analysis);
        ClinicalPatientData row = new ClinicalPatientData();
        List<Result> loadOnly = resultService.getResultsByAnalysis(analysis).stream()
                .filter(result -> "vcu-load".equals(result.getTestResult().getComponentId())).toList();

        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", loadOnly,
                testResultComponentService.getActiveComponentsByTestId("1"), row);

        List<String> lines = List.of(row.getResult().split("\n"));
        assertEquals(2, lines.size());
        assertEquals("Viral load", lines.get(0).split(":")[0]);
        assertEquals("Ct: In Progress", lines.get(1));
        assertEquals(List.of("cp/ml VCU", ""), List.of(row.getUom().split("\n", -1)));
        assertEquals("In Progress", row.getComponents().get(1).value());
    }

    /**
     * A test whose parts are validated one by one is not released until its last
     * part is, but the report still lists every part on the test's row: a validated
     * part with its value, a part awaiting validation with that status.
     */
    @Test
    public void anUnreleasedTestPrintsEachPartWithItsOwnStatus() {
        jdbcTemplate.update("UPDATE clinlims.result SET validated_date = NOW() WHERE id = 100");

        ClinicalPatientData row = reportRowFor(analysisService.get("100"));

        assertEquals(List.of("1010", "Awaiting validation"),
                row.getComponents().stream().map(ReportComponentLine::value).toList());
        assertEquals("Ct: Awaiting validation", List.of(row.getResult().split("\n")).get(1));
    }

    /**
     * Until one of its parts is validated, no value of an unreleased test is
     * printed: every entered part awaits validation.
     */
    @Test
    public void anUnreleasedTestPrintsNoUnvalidatedValue() {
        ClinicalPatientData row = reportRowFor(analysisService.get("100"));

        assertEquals(List.of("Awaiting validation", "Awaiting validation"),
                row.getComponents().stream().map(ReportComponentLine::value).toList());
    }

    private ClinicalPatientData reportRowFor(Analysis analysis) {
        PatientReport report = report();
        ReflectionTestUtils.setField(report, "currentAnalysis", analysis);
        ClinicalPatientData row = new ClinicalPatientData();
        ReflectionTestUtils.invokeMethod(report, "reportResultAndConclusion", row);
        return row;
    }

    private PatientReport report() {
        PatientReport report = new PatientCILNSPClinical_vreduit();
        ReflectionTestUtils.setField(report, "currentPatient", new Patient());
        ReflectionTestUtils.setField(report, "messages", UiMessages.forLocale(Locale.ENGLISH));
        return report;
    }
}
