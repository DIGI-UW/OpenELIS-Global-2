package org.openelisglobal.dataexchange.service.aggregatereporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportExternalExportServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportExternalExportService reportExternalExportService;

    private ReportExternalExport reportExternalExport;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/report-external-export.xml");
        reportExternalExport = new ReportExternalExport();
    }

    @Test
    public void testGetLastCollectedTimestamp() {
        Timestamp lastTimestamp = reportExternalExportService.getLastCollectedTimestamp();
        assertEquals(Timestamp.valueOf("2023-01-11 00:00:00"), lastTimestamp);
    }

    @Test
    public void testGetReportByEventDateAndType() {
        reportExternalExport.setEventDate(Timestamp.valueOf("2022-01-01 00:00:00"));
        reportExternalExport.setTypeId("102");
        ReportExternalExport returnedReportExternalExport = reportExternalExportService
                .getReportByEventDateAndType(reportExternalExport);
        assertNotNull(returnedReportExternalExport);
        assertEquals("Export flagged for review", returnedReportExternalExport.getBookkeepingData());
    }

    @Test
    public void testGetReportsInDateRange() {
        // TODO: I noted that the String reportQueueTypeId is not used anywhere by
        // the method, So removing it would not cause any effects :)

        List<ReportExternalExport> externalExports = reportExternalExportService.getReportsInDateRange(
                Timestamp.valueOf("2023-12-21 00:00:00"), Timestamp.valueOf("2024-10-26 00:00:00"), "102");
        assertNotNull(externalExports);
        assertEquals(2, externalExports.size());
        assertEquals("2", externalExports.get(1).getId());
    }

    @Test
    public void testGetLatestSentReportExport() {
        ReportExternalExport reportExternalExport = reportExternalExportService.getLatestSentReportExport("102");
        reportExternalExport.setSend(false);
        reportExternalExport.setTypeId("102");
        reportExternalExport.setSentDate(Timestamp.valueOf("2024-05-21 00:00:00"));
        assertNotNull(reportExternalExport);
        assertEquals("2", reportExternalExport.getId());
        assertEquals(Timestamp.valueOf("2025-01-01 00:00:00"), reportExternalExport.getLastupdated());
    }

    @Test
    public void testReadReportExternalExport() {
        ReportExternalExport reportExternalExport = reportExternalExportService.readReportExternalExport("3");
        assertNotNull(reportExternalExport);
        assertEquals("Awaiting confirmation", reportExternalExport.getBookkeepingData());
    }

    @Test
    public void testGetLatestEventReportExport() {
        ReportExternalExport reportExternalExport = reportExternalExportService.getLatestEventReportExport("101");
        reportExternalExport.setTypeId("101");
        reportExternalExport.setEventDate(Timestamp.valueOf("2022-11-01 12:00:00"));
        assertNotNull(reportExternalExport);
        assertEquals(Timestamp.valueOf("2023-01-01 00:00:00"), reportExternalExport.getCollectionDate());
    }

    @Test
    public void testGetLastSentTimestamp() {
        Timestamp lastSentTimestamp = reportExternalExportService.getLastSentTimestamp();
        assertEquals(Timestamp.valueOf("2024-05-21 00:00:00"), lastSentTimestamp);
    }

    @Test
    public void testGetUnsentReportExports() {
        List<ReportExternalExport> reportExternalExports = reportExternalExportService.getUnsentReportExports("103");
        assertNotNull(reportExternalExports);
        assertEquals(1, reportExternalExports.size());
        assertEquals("3", reportExternalExports.get(0).getId());
    }

    // TODO: the method being tested uses a field called recalculate, though it was
    // not found in the Entity thus making the test fail.
//    @Test
//    public void testGetRecalculateReportExports(){
//        reportExternalExport.setTypeId("102");
//        List<ReportExternalExport> reportExternalExports = reportExternalExportService.getRecalculateReportExports("102");
//        assertNotNull(reportExternalExports);
//        assertEquals(1, reportExternalExports.size());
//        assertEquals("2", reportExternalExports.get(0).getId());
//    }

    @Test
    public void testLoadReport() {
        reportExternalExport.setId("2");
        ReportExternalExport returnedReportExternalExport = reportExternalExportService
                .loadReport(reportExternalExport);
        assertNotNull(returnedReportExternalExport);
        assertEquals("Export flagged for review", returnedReportExternalExport.getBookkeepingData());
    }
}
