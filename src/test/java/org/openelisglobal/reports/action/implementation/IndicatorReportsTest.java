package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.reports.action.implementation.reportBeans.HaitiHIVSummaryData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testsupport.PdfText;

public class IndicatorReportsTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/indicator-reports.xml");
        SpringContext.getBean(TestSectionService.class).refreshNames();
    }

    @Test
    public void labAggregate_countsEachTestsStatusesBySection() throws Exception {
        List<String> lines = render(new IndicatorAllTestClinical());

        assertLine(lines, "Summary of All Tests");
        assertLine(lines, "01/01/2026 - 12/12/2026 Site ID: DEV01");
        assertLine(lines, "Global Laboratory Report");
        assertLine(lines, "Test Not Started In Progress * Completed Total");
        assertLine(lines, "Hematology");
        assertLine(lines, "Hemoglobin 1 1 1 3");
        assertLine(lines, "Total 1 1 1 3");
        assertLine(lines, "There are no tests for this lab unit in the date range selected");
        assertLine(lines, "Total Laboratories 1 1 1 3");
    }

    @Test
    public void hivSummary_listsThePopulationAndEachTestsOutcomes() throws Exception {
        List<String> lines = render(hivReport(List.of()));

        assertLine(lines, "HIV Test Summary");
        assertLine(lines, "From 01/01/2026 To 12/12/2026 Site ID: DEV01");
        assertLine(lines, "Population Group Men Women Children Total");
        assertLine(lines, "Total 0 0 0 0");
        assertLine(lines, "Positive Negative Indeterminate Waiting Total");
        assertLine(lines, "Determine");
        assertLine(lines, "Account 3 5 1 2 11");
        assertTrue(String.valueOf(lines), lines.stream().anyMatch(line -> line.startsWith("Percentage 27.27")));
        assertLine(lines, "Account 0 4 0 0 4");
    }

    @Test
    public void hivSummary_leavesOrdersWithoutAPatientOutOfThePopulation() throws Exception {
        List<String> lines = render(hivReport(List.of(SpringContext.getBean(AnalysisService.class).get("941"))));

        assertLine(lines, "Total 0 0 0 0");
    }

    private IndicatorHIV hivReport(List<Analysis> analyses) {
        return new IndicatorHIV() {
            @Override
            protected void findAnalysis() {
                analysisList = new ArrayList<>(analyses);
            }

            @Override
            protected void setHIVByTest() {
                testData = List.of(summary("Determine", 3, 5, 1, 2), summary("CD4 en mm3", 0, 4, 0, 0));
            }
        };
    }

    private static HaitiHIVSummaryData summary(String test, int positive, int negative, int indeterminate,
            int pending) {
        HaitiHIVSummaryData data = new HaitiHIVSummaryData();
        data.setTestName(test);
        data.setPositive(positive);
        data.setNegative(negative);
        data.setIndeterminate(indeterminate);
        data.setPending(pending);
        int total = positive + negative + indeterminate + pending;
        data.setTotal(total);
        data.setPositivePer(total == 0 ? 0 : positive * 100.0 / total);
        data.setNegativePer(total == 0 ? 0 : negative * 100.0 / total);
        data.setIndeterminatePer(total == 0 ? 0 : indeterminate * 100.0 / total);
        data.setPendingPer(total == 0 ? 0 : pending * 100.0 / total);
        return data;
    }

    private List<String> render(Report report) throws Exception {
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2026");
        form.setUpperDateRange("12/12/2026");
        report.initializeReport(form);
        report.setReportPath(getClass().getClassLoader().getResource("reports/").getPath());
        return Arrays.asList(PdfText.of(report.runReport()).split("\n"));
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
