package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

public class NonConformityReportTest extends BaseWebContextSensitiveTest {

    private final String reportDir = getClass().getClassLoader().getResource("reports/").getPath();

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/non-conformity-report.xml");
    }

    @Test
    public void byDate_listsEachOrdersNonConformities() throws Exception {
        List<String> lines = render(new HaitiNonConformityByDate());

        assertContains(lines, "DEV0124000000000911");
        assertContains(lines, "DEV0124000000000912");
    }

    private List<String> render(Report report) throws Exception {
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2024");
        form.setUpperDateRange("12/12/2024");
        report.initializeReport(form);
        report.setReportPath(reportDir);
        return Arrays.asList(PdfText.of(report.runReport()).split("\n"));
    }

    private void assertContains(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.stream().anyMatch(line -> line.contains(expected)));
    }
}
