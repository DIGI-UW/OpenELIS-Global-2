package org.openelisglobal.dataexchange.service.aggregatereporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalImport;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportExternalImportServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportExternalImportService reportExternalImportService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/report-external-import.xml");
    }

    @Test
    public void getReportsInDateRangeSortedForSite_ShouldReturnSortedReportExternalImportThatLieBetweenParameterDates() {
        List<ReportExternalImport> reportExternalImports = reportExternalImportService
                .getReportsInDateRangeSortedForSite(Timestamp.valueOf("2022-10-19 12:00:00"),
                        Timestamp.valueOf("2023-01-21 12:00:00"), "LAB001");
        assertNotNull(reportExternalImports);
        assertEquals(1, reportExternalImports.size());
        assertEquals("1", reportExternalImports.get(0).getId());
    }

    @Test
    public void getReportsInDateRangeSorted_ShouldReturnSortedReportExternalImportThatLieBetweenParameterDates() {
        List<ReportExternalImport> reportExternalImports = reportExternalImportService.getReportsInDateRangeSorted(
                Timestamp.valueOf("2022-08-19 12:00:00"), Timestamp.valueOf("2023-01-21 12:00:00"));
        assertNotNull(reportExternalImports);
        assertEquals(2, reportExternalImports.size());
        assertEquals("2", reportExternalImports.get(0).getId());
    }

    @Test
    public void getReportByEventDateSiteType_ShouldReturnAReportExternalImport() {
        ReportExternalImport importReport = new ReportExternalImport();
        importReport.setEventDate(Timestamp.valueOf("2022-09-01 00:00:00"));
        importReport.setSendingSite("CLINIC45");
        importReport.setReportType("VACCINATION");
        ReportExternalImport returnedImportReport = reportExternalImportService
                .getReportByEventDateSiteType(importReport);
        assertNotNull(returnedImportReport);
        assertEquals("2", returnedImportReport.getId());
        assertEquals(Timestamp.valueOf("2025-01-01 00:00:00"), returnedImportReport.getLastupdated());
    }

    @Test
    public void getUniqueSites_ShouldReturnAListOfUniqueSitesAsStrings() {
        List<String> uniqueSites = reportExternalImportService.getUniqueSites();
        assertNotNull(uniqueSites);
        assertEquals(3, uniqueSites.size());
        assertEquals("MOBILE_UNIT", uniqueSites.get(1));
    }
}
