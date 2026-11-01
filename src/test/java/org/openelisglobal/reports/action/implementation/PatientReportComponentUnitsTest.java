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

    @Test
    public void logDisplayFollowsTheExplicitComponentSettingAndUsesTheClinicalMeasurement() {
        PatientReport report = report();
        Analysis analysis = analysisService.get("100");
        Result result = resultService.getResultsByAnalysis(analysis).stream()
                .filter(r -> "vcu-load".equals(r.getTestResult().getComponentId())).findFirst().orElseThrow();
        result.setValue("0.0064");
        result.setReportingDecimalPlaces(2);
        var component = testResultComponentService.get("vcu-load");
        component.setSysUserId("1");
        component.setDisplayLog10(true);
        testResultComponentService.update(component);
        String display = ReflectionTestUtils.invokeMethod(report, "formatPatientReportResult", result);
        assertEquals("0.01\nCalculated log10: −2.19\n(Value used for clinical decisions: 0.0064)", display);
        assertEquals("0.0064", result.getValue());
        assertEquals("0.01", resultService.formatResultForDisplay(result, false));
        component.setDisplayLog10(false);
        testResultComponentService.update(component);
        String withoutLog = ReflectionTestUtils.invokeMethod(report, "formatPatientReportResult", result);
        assertEquals("0.01\n(Value used for clinical decisions: 0.0064)", withoutLog);
    }

    @Test
    public void logarithmsNeverInventExactValuesForBoundsOrNonPositiveMeasurements() {
        for (String value : List.of("<0.0064", "<=0.0064", ">0.0064", "0", "-0.0064")) {
            Result result = new Result();
            result.setResultType("N");
            result.setValue(value);
            result.clearReportingDecimalPlaces();
            assertEquals(value, org.openelisglobal.reports.action.implementation.reportBeans.NumericReportDisplay
                    .format(result, true));
            assertEquals(value, result.getEnteredValue());
        }
    }

    @Test
    public void receivedLogWinsAndOnlyAMissingLogUsesTheMeasurementFallback() {
        PatientReport report = report();
        Analysis analysis = analysisService.get("100");
        ReflectionTestUtils.setField(report, "currentAnalysis", analysis);
        var load = testResultComponentService.get("vcu-load");
        var source = testResultComponentService.get("vcu-ct");
        source.setCode("INSTRUMENT_LOG");
        source.setLabel("Received log10");
        source.setSysUserId("1");
        testResultComponentService.update(source);
        load.setDisplayLog10(true);
        load.setLog10SourceComponentCode(source.getCode());
        load.setSysUserId("1");
        testResultComponentService.update(load);
        var results = resultService.getResultsByAnalysis(analysis);
        var measurement = results.stream().filter(r -> "vcu-load".equals(r.getTestResult().getComponentId()))
                .findFirst().orElseThrow();
        var received = results.stream().filter(r -> "vcu-ct".equals(r.getTestResult().getComponentId())).findFirst()
                .orElseThrow();
        measurement.setValue("1009.64");
        measurement.clearReportingDecimalPlaces();
        received.setValue("3.0040");
        received.clearReportingDecimalPlaces();
        ClinicalPatientData data = new ClinicalPatientData();
        var components = testResultComponentService.getActiveComponentsByTestId("1");
        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", results, components, data);
        assertEquals(List.of("1009.64", "3.0040"),
                data.getComponents().stream().map(ReportComponentLine::value).toList());
        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", List.of(measurement), components, data);
        assertEquals("1009.64\nCalculated log10: 3.00", data.getComponents().get(0).value());
        assertEquals("In Progress", data.getComponents().get(1).value());
        assertEquals("1009.64", measurement.getEnteredValue());
        assertEquals("3.0040", received.getEnteredValue());
        received.setValue(" ");
        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", results, components, data);
        assertEquals("1009.64\nCalculated log10: 3.00", data.getComponents().get(0).value());
        assertEquals("In Progress", data.getComponents().get(1).value());
        received.setValue("<2");
        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", results, components, data);
        assertEquals(List.of("1009.64", "<2"), data.getComponents().stream().map(ReportComponentLine::value).toList());
        // A present but unvalidated received log is pending, not permission to
        // substitute a calculated number or publish that received value.
        measurement.setValidatedDate(new java.sql.Timestamp(System.currentTimeMillis()));
        ReflectionTestUtils.invokeMethod(report, "buildMultiComponentRow", results, components, data, true);
        assertEquals(List.of("1009.64", "Awaiting validation"),
                data.getComponents().stream().map(ReportComponentLine::value).toList());
    }

    @Test
    public void legacyAlphaMeasurementsCanOptIntoLogDisplayWithoutChangingTypeOrText() {
        var component = testResultComponentService.get("vcu-load");
        component.setResultType("A");
        component.setDisplayLog10(true);
        component.setSysUserId("1");
        testResultComponentService.update(component);
        Result result = resultService.get("100");
        result.setResultType("A");
        result.setValue("1000.00");
        assertEquals("1000.00\nCalculated log10: 3.00",
                ReflectionTestUtils.invokeMethod(report(), "formatPatientReportResult", result));
        for (String value : List.of("<1000", "Not detected", "0", "-1")) {
            result.setValue(value);
            assertEquals(value, ReflectionTestUtils.invokeMethod(report(), "formatPatientReportResult", result));
            assertEquals(value, result.getEnteredValue());
            assertEquals("A", result.getResultType());
        }
    }

    @Test
    public void whitespaceDoesNotHideTheClinicalValueOrConfiguredLogarithm() {
        Result result = new Result();
        result.setResultType("N");
        result.setValue(" 6.004 ");
        result.setReportingDecimalPlaces(0);
        assertEquals("6\nCalculated log10: 0.78\n(Value used for clinical decisions:  6.004 )",
                org.openelisglobal.reports.action.implementation.reportBeans.NumericReportDisplay.format(result, true));
        assertEquals(" 6.004 ", result.getEnteredValue());
    }

    @Test
    public void logarithmsSupportScientificNotationWithoutChangingItsRepresentation() {
        for (String[] example : new String[][] { { "1.5×10⁵", "5.18" }, { "1e-400", "−400.00" },
                { "1e400", "400.00" } }) {
            Result result = new Result();
            result.setResultType("N");
            result.setValue(example[0]);
            result.clearReportingDecimalPlaces();
            assertEquals(example[0] + "\nCalculated log10: " + example[1],
                    org.openelisglobal.reports.action.implementation.reportBeans.NumericReportDisplay.format(result,
                            true));
            assertEquals(example[0], result.getEnteredValue());
        }
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
