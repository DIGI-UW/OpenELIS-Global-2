package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BiorepositoryExportServicePdfTest {

    @Mock
    private BiorepositoryDashboardService dashboardService;

    @Mock
    private ChainOfCustodyService custodyService;

    @InjectMocks
    private BiorepositoryExportServiceImpl exportService;

    @Test
    public void exportDashboardToPDF_ReturnsPdfDocumentBytes() throws Exception {
        when(dashboardService.getStorageCapacityMetrics(any())).thenReturn(
                Map.of("totalDevices", 2, "totalSamplesStored", 20, "pendingStorage", 3, "averageUtilization", 45.5));
        when(dashboardService.getSampleAgingMetrics(any()))
                .thenReturn(Map.of("total", 20, "expiring30Days", 2, "expiring60Days", 1, "expiring90Days", 0, "expired", 1));
        when(dashboardService.getQCComplianceMetrics(any()))
                .thenReturn(Map.of("totalInspections", 10, "passedInspections", 9, "failedInspections", 1, "complianceRate", 90.0));
        when(dashboardService.getRetrievalStatistics(any(), any(), any()))
                .thenReturn(Map.of("totalRequests", 5, "pendingRequests", 1, "rejectedRequests", 0, "completedRequests", 4));

        byte[] bytes = exportService.exportDashboardToPDF();
        assertNotNull(bytes);
        assertTrue(bytes.length > 32);
        String prefix = new String(bytes, 0, 5, StandardCharsets.ISO_8859_1);
        assertEqualsPdfSignature(prefix);
    }

    @Test
    public void exportAuditTrailToPDF_ReturnsPdfDocumentBytes() throws Exception {
        ChainOfCustodyLog log = new ChainOfCustodyLog();
        log.setActionTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setCustodyAction(ChainOfCustodyLog.CustodyAction.CHECKOUT_RETRIEVED);
        log.setFromLocation("Freezer-A > Shelf-1");
        log.setToLocation("Bench-1");
        log.setTemperature(new BigDecimal("-70.5"));
        log.setNotes("Operator transfer");

        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-123");
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1001");
        sampleItem.setExternalId("BAR-001");
        sampleItem.setSample(sample);
        log.setSampleItem(sampleItem);

        when(custodyService.searchCustodyLogs(anyString(), any(), anyInt(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(log));

        byte[] bytes = exportService.exportAuditTrailToPDF("BAR-001", null, null, null, null);
        assertNotNull(bytes);
        assertTrue(bytes.length > 32);
        String prefix = new String(bytes, 0, 5, StandardCharsets.ISO_8859_1);
        assertEqualsPdfSignature(prefix);
        String contentProbe = new String(bytes, StandardCharsets.ISO_8859_1);
        assertFalse(contentProbe.trim().startsWith("{"));
    }

    private void assertEqualsPdfSignature(String prefix) {
        assertTrue("Expected PDF signature but got: " + prefix, "%PDF-".equals(prefix));
    }
}
