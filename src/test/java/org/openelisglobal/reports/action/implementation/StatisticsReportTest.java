package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.StatisticsReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

public class StatisticsReportTest extends BaseWebContextSensitiveTest {

    private static final String JANUARY_TO_DECEMBER = "4 3 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 1";

    @Test
    public void countsEachTestsTestsAndSamplesByMonth() throws Exception {
        StatisticsReport report = new StatisticsReport();
        ReportForm form = new ReportForm();
        form.setUpperYear("2026");
        form.setPriority(List.of());
        form.setLabSections(List.of());
        form.setReceptionTime(List.of());
        report.initializeReport(form);
        StatisticsReportData hemoglobin = new StatisticsReportData();
        hemoglobin.setTestName("Hemoglobin");
        hemoglobin.setTestsJan(4);
        hemoglobin.setSamplesJan(3);
        hemoglobin.setTestsDec(2);
        hemoglobin.setSamplesDec(1);

        List<String> lines = Arrays.asList(PdfText.of(report.render(List.of(hemoglobin))).split("\n"));

        assertLine(lines, "StatisticsReport");
        assertLine(lines, "01/01/2026 - 31/12/2026 Site ID: DEV01");
        assertLine(lines, "Test Section:");
        assertLine(lines, "Priority:");
        assertTrue(String.valueOf(lines),
                lines.stream().anyMatch(line -> line.contains("January February March") && line.endsWith("December")));
        assertLine(lines, "Hemoglobin " + JANUARY_TO_DECEMBER);
        assertLine(lines, "Total " + JANUARY_TO_DECEMBER);
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
