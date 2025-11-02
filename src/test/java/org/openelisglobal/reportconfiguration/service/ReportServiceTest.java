package org.openelisglobal.reportconfiguration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportService reportService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/reportconfiguration-service.xml");
    }

    @Test
    public void getReports_ShouldReturnAllReports() {
        List<Report> reports = reportService.getReports();

        assertNotNull("Reports list should not be null", reports);
        assertFalse("Reports list should not be empty", reports.isEmpty());
        assertTrue("Reports list should contain at least 4 test reports", reports.size() >= 4);
    }

    @Test
    public void getReports_ShouldReturnReportsWithLocalizedNames() {
        List<Report> reports = reportService.getReports();

        assertNotNull("Reports list should not be null", reports);
        for (Report report : reports) {
            assertNotNull("Report name should not be null", report.getName());
            assertNotNull("Report display key should not be null", report.getDisplayKey());
        }
    }

    @Test
    public void get_ShouldReturnSpecificReport_WhenValidIdProvided() {
        Report report = reportService.get("100");

        assertNotNull("Report should be found", report);
        assertEquals("Report ID should match", "100", report.getId());
        assertEquals("Report category should be 'Lab Reports'", "Lab Reports", report.getCategory());
        assertTrue("Report should be visible", report.getIsVisible());
        assertEquals("Report sort order should be 1", Integer.valueOf(1), report.getSortOrder());
    }

    @Test
    public void get_ShouldReturnInvisibleReport_WhenReportIsNotVisible() {
        Report report = reportService.get("102");

        assertNotNull("Report should be found", report);
        assertEquals("Report ID should match", "102", report.getId());
        assertEquals("Report category should be 'Financial Reports'", "Financial Reports", report.getCategory());
        assertFalse("Report should not be visible", report.getIsVisible());
    }

    @Test
    public void getAll_ShouldReturnAllReportsFromDatabase() {
        List<Report> allReports = reportService.getAll();

        assertNotNull("Reports list should not be null", allReports);
        assertTrue("Should have at least 4 reports", allReports.size() >= 4);
    }

    @Test
    public void getReports_ShouldReturnReportsFromDifferentCategories() {
        List<Report> reports = reportService.getReports();

        assertNotNull("Reports list should not be null", reports);

        boolean hasLabReports = reports.stream().anyMatch(r -> "Lab Reports".equals(r.getCategory()));
        boolean hasFinancialReports = reports.stream().anyMatch(r -> "Financial Reports".equals(r.getCategory()));
        boolean hasPatientReports = reports.stream().anyMatch(r -> "Patient Reports".equals(r.getCategory()));

        assertTrue("Should have Lab Reports", hasLabReports);
        assertTrue("Should have Financial Reports", hasFinancialReports);
        assertTrue("Should have Patient Reports", hasPatientReports);
    }

    @Test
    public void get_ShouldReturnNull_WhenInvalidIdProvided() {
        Report report = reportService.get("999999");

        assertEquals("Report should not be found for invalid ID", null, report);
    }
}
