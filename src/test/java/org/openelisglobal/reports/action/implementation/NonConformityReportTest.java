package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        assertLine(lines, "Non-Conforming Events 01/01/2024 - 12/12/2024");
        assertContains(lines,
                "Laboratory Number (Accession Number): DEV0124000000000911 Date of reception: 03/06/2024 08:00");
        assertContains(lines, "Section Date Reason for rejection Type of sample Biologist Note");
        assertLine(lines, "25/06/2024 Hemolysed Serum");
        assertContains(lines,
                "Laboratory Number (Accession Number): DEV0124000000000912 Date of reception: 04/06/2024 08:00");
        assertLine(lines, "25/06/2024 Clotted Serum");
        assertLine(lines, "26/06/2024 Hemolysed Serum");
        assertEquals("one comments line per order: " + lines, 2,
                lines.stream().filter(line -> line.startsWith("Comments:")).count());
        assertFalse("the routine report leaves out the site subject number: " + lines,
                lines.stream().anyMatch(line -> line.contains("Site Subject No.")));
    }

    @Test
    public void studyByDate_listsTheSiteSubjectNumberUnderTheSiteHeader() throws Exception {
        List<String> lines = render(new RetroCINonConformityByDate());

        assertContains(lines, "Site Subject No.:");
        assertFalse("the header comes from site information: " + lines,
                lines.stream().anyMatch(line -> line.contains("CIRBA") || line.contains("Dr TONI")));
    }

    @Test
    public void bySectionAndReason_countsEachReasonWithinItsSection() throws Exception {
        List<String> lines = render(new HaitiNonConformityBySectionReason());

        assertLine(lines, "Non Conformity Report by Unit and Reason");
        assertLine(lines, "01/01/2024 - 12/12/2024");
        assertLine(lines, "Not specified");
        assertLine(lines, "Clotted 1");
        assertLine(lines, "Hemolysed 2");
        assertLine(lines, "Total: Not specified 3");
        assertFalse("a single section has no grand total: " + lines, lines.contains("Total 3"));
    }

    private List<String> render(Report report) throws Exception {
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2024");
        form.setUpperDateRange("12/12/2024");
        report.initializeReport(form);
        report.setReportPath(reportDir);
        return Arrays.asList(PdfText.of(report.runReport()).split("\n"));
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }

    private void assertContains(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.stream().anyMatch(line -> line.contains(expected)));
    }
}
