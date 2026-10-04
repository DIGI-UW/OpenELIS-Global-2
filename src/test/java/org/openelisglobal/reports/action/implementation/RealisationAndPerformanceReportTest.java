package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.SectionPerformanceData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testsupport.PdfText;

public class RealisationAndPerformanceReportTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/indicator-reports.xml");
        SpringContext.getBean(TestSectionService.class).refreshNames();
    }

    @Test
    public void realisation_countsRequestedPerformedAndNotPerformedTestsBySection() throws Exception {
        IPCIRealisationReport report = new IPCIRealisationReport();
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2026");
        form.setUpperDateRange("12/12/2026");
        report.initializeReport(form);
        report.setReportPath(getClass().getClassLoader().getResource("reports/").getPath());

        List<String> lines = Arrays.asList(PdfText.of(report.runReport()).split("\n"));

        assertLine(lines, "Rapport sur la realisation des tests");
        assertTrue(String.valueOf(lines), lines.stream().anyMatch(line -> line.startsWith("01/01/2026 - 12/12/2026")));
        assertLine(lines, "Test Demande Effectue Non effectue");
        assertLine(lines, "Hematology");
        assertLine(lines, "Hemoglobin 3 1 2");
        assertLine(lines, "Total 3 1 2");
        assertLine(lines, "Totaux 3 1 2");
        assertTrue("the site comes from site information: " + lines,
                lines.stream().noneMatch(line -> line.contains("IPCI")));
    }

    @Test
    public void sectionPerformance_chartsTheWaitingDaysOfEachCategory() throws Exception {
        SectionPerformanceData biochemistry = new SectionPerformanceData();
        biochemistry.setCategoryLabel("Biochimie");
        biochemistry.setCategoryValue(5);
        SectionPerformanceData hematology = new SectionPerformanceData();
        hematology.setCategoryLabel("Hematologie");
        hematology.setCategoryValue(2);

        String text = PdfText.of(SectionPerformancePdf.render(List.of(biochemistry, hematology)));

        assertTrue(text, text.contains("Durée d'attente des demandes en jours"));
        assertTrue("each category is on the chart: " + text,
                text.contains("Biochimie") && text.contains("Hematologie"));
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
