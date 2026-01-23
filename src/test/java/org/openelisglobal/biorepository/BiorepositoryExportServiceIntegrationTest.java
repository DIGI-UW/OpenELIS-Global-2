package org.openelisglobal.biorepository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BiorepositoryExportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for BiorepositoryExportService.
 *
 * Verifies that export service correctly maps field names from dashboard
 * service. Tests CSV export format to ensure no null values for valid metrics.
 */
public class BiorepositoryExportServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BiorepositoryExportService exportService;

    @Test
    public void testExportDashboardToCSV_ContainsCorrectFieldNames() throws Exception {

        // Export dashboard to CSV
        byte[] csvBytes = exportService.exportDashboardToCSV();
        assertNotNull("CSV export should not be null", csvBytes);
        assertTrue("CSV export should not be empty", csvBytes.length > 0);

        // Parse CSV content
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\n");

        assertTrue("CSV should have at least header row", lines.length > 1);
        assertTrue("First line should be header", lines[0].contains("Metric Category,Metric Name,Value"));

        // Verify Storage Capacity metrics use correct field names
        boolean hasTotalSamplesStored = false;
        boolean hasPendingStorage = false;

        // Verify Sample Aging metrics use correct field names
        boolean hasTotalActiveSamples = false;
        boolean hasExpiredSamples = false;

        // Verify Retrieval Statistics use correct field names
        boolean hasRejectedRequests = false;

        // Verify QC Compliance metrics are present
        boolean hasTotalInspections = false;
        boolean hasComplianceRate = false;

        for (String line : lines) {
            if (line.contains("Storage Capacity,Total Samples Stored")) {
                hasTotalSamplesStored = true;
                // Verify value is not null
                String[] parts = line.split(",");
                assertTrue("Total Samples Stored should have a value", parts.length == 3 && !parts[2].equals("null"));
            }
            if (line.contains("Storage Capacity,Pending Storage")) {
                hasPendingStorage = true;
                String[] parts = line.split(",");
                assertTrue("Pending Storage should have a value", parts.length == 3 && !parts[2].equals("null"));
            }
            if (line.contains("Sample Aging,Total Active Samples")) {
                hasTotalActiveSamples = true;
                String[] parts = line.split(",");
                assertTrue("Total Active Samples should have a value", parts.length == 3 && !parts[2].equals("null"));
            }
            if (line.contains("Sample Aging,Expired Samples")) {
                hasExpiredSamples = true;
                String[] parts = line.split(",");
                assertTrue("Expired Samples should have a value", parts.length == 3 && !parts[2].equals("null"));
            }
            if (line.contains("Retrieval Statistics,Rejected Requests")) {
                hasRejectedRequests = true;
                String[] parts = line.split(",");
                assertTrue("Rejected Requests should have a value", parts.length == 3 && !parts[2].equals("null"));
            }
            if (line.contains("QC Compliance,Total Inspections")) {
                hasTotalInspections = true;
                String[] parts = line.split(",");
                assertTrue("Total Inspections should have a value", parts.length == 3 && !parts[2].equals("null"));
            }
            if (line.contains("QC Compliance,Compliance Rate %")) {
                hasComplianceRate = true;
                String[] parts = line.split(",");
                assertTrue("Compliance Rate should have a value", parts.length == 3 && !parts[2].equals("null"));
            }

            // Verify no old incorrect field names are present
            assertFalse("Should not contain old field name 'Total Capacity'", line.contains(",Total Capacity,"));
            assertFalse("Should not contain old field name 'Total Occupied'", line.contains(",Total Occupied,"));
            assertFalse("Should not contain old field name 'Total Samples,'", line.contains(",Total Samples,"));
            assertFalse("Should not contain old field name 'Approved Requests'", line.contains(",Approved Requests,"));
        }

        // Assert all expected metrics are present with correct field names
        assertTrue("CSV should contain 'Total Samples Stored'", hasTotalSamplesStored);
        assertTrue("CSV should contain 'Pending Storage'", hasPendingStorage);
        assertTrue("CSV should contain 'Total Active Samples'", hasTotalActiveSamples);
        assertTrue("CSV should contain 'Expired Samples'", hasExpiredSamples);
        assertTrue("CSV should contain 'Rejected Requests'", hasRejectedRequests);
        assertTrue("CSV should contain 'Total Inspections'", hasTotalInspections);
        assertTrue("CSV should contain 'Compliance Rate %'", hasComplianceRate);
    }

    @Test
    public void testExportDashboardToExcel_GeneratesValidXLSX() throws Exception {
        byte[] excelBytes = exportService.exportDashboardToExcel();
        assertNotNull("Excel export should not be null", excelBytes);
        assertTrue("Excel export should not be empty", excelBytes.length > 0);

        // Verify Excel magic bytes (XLSX file signature)
        byte[] xlsxSignature = new byte[] { 0x50, 0x4B, 0x03, 0x04 }; // "PK" ZIP signature
        byte[] fileHeader = new byte[4];
        System.arraycopy(excelBytes, 0, fileHeader, 0, 4);

        for (int i = 0; i < 4; i++) {
            assertTrue("Excel file should have valid XLSX signature", fileHeader[i] == xlsxSignature[i]);
        }
    }

    @Test
    public void testExportDashboardToJSON_ContainsAllMetricCategories() throws Exception {
        byte[] jsonBytes = exportService.exportDashboardToJSON();
        assertNotNull("JSON export should not be null", jsonBytes);
        assertTrue("JSON export should not be empty", jsonBytes.length > 0);

        String jsonContent = new String(jsonBytes, StandardCharsets.UTF_8);

        // Verify JSON contains all expected metric categories
        assertTrue("JSON should contain storageCapacity", jsonContent.contains("\"storageCapacity\""));
        assertTrue("JSON should contain sampleAging", jsonContent.contains("\"sampleAging\""));
        assertTrue("JSON should contain qcCompliance", jsonContent.contains("\"qcCompliance\""));
        assertTrue("JSON should contain retrievalStats", jsonContent.contains("\"retrievalStats\""));

        // Verify correct field names are used (not old ones)
        assertTrue("JSON should contain totalSamplesStored", jsonContent.contains("\"totalSamplesStored\""));
        assertTrue("JSON should contain pendingStorage", jsonContent.contains("\"pendingStorage\""));
        assertTrue("JSON should contain total (active samples)", jsonContent.contains("\"total\""));
        assertTrue("JSON should contain expired", jsonContent.contains("\"expired\""));
        assertTrue("JSON should contain rejectedRequests", jsonContent.contains("\"rejectedRequests\""));

        // Verify old incorrect field names are NOT present
        assertFalse("JSON should not contain old field totalCapacity", jsonContent.contains("\"totalCapacity\""));
        assertFalse("JSON should not contain old field totalOccupied", jsonContent.contains("\"totalOccupied\""));
        assertFalse("JSON should not contain old field totalSamples", jsonContent.contains("\"totalSamples\""));
        assertFalse("JSON should not contain old field expiredSamples", jsonContent.contains("\"expiredSamples\""));
        assertFalse("JSON should not contain old field approvedRequests", jsonContent.contains("\"approvedRequests\""));
    }

    @Test
    public void testExportAuditTrailToCSV_GeneratesValidOutput() throws Exception {
        // Export audit trail with no filters
        byte[] csvBytes = exportService.exportAuditTrailToCSV(null, null, null, null, null);
        assertNotNull("Audit trail CSV export should not be null", csvBytes);
        assertTrue("Audit trail CSV export should not be empty", csvBytes.length > 0);

        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\n");

        assertTrue("CSV should have at least header row", lines.length >= 1);

        // Verify header contains expected columns
        String header = lines[0];
        assertTrue("Header should contain Timestamp", header.contains("Timestamp"));
        assertTrue("Header should contain Sample Barcode", header.contains("Sample Barcode"));
        assertTrue("Header should contain Custody Action", header.contains("Custody Action"));
        assertTrue("Header should contain From Location", header.contains("From Location"));
        assertTrue("Header should contain To Location", header.contains("To Location"));
        assertTrue("Header should contain Temperature", header.contains("Temperature"));
    }

    @Test
    public void testExportAuditTrailToExcel_GeneratesValidXLSX() throws Exception {
        byte[] excelBytes = exportService.exportAuditTrailToExcel(null, null, null, null, null);
        assertNotNull("Audit trail Excel export should not be null", excelBytes);
        assertTrue("Audit trail Excel export should not be empty", excelBytes.length > 0);

        // Verify Excel magic bytes (XLSX file signature)
        byte[] xlsxSignature = new byte[] { 0x50, 0x4B, 0x03, 0x04 };
        byte[] fileHeader = new byte[4];
        System.arraycopy(excelBytes, 0, fileHeader, 0, 4);

        for (int i = 0; i < 4; i++) {
            assertTrue("Excel file should have valid XLSX signature", fileHeader[i] == xlsxSignature[i]);
        }
    }

    @Test
    public void testExportAuditTrailToJSON_ContainsValidStructure() throws Exception {
        byte[] jsonBytes = exportService.exportAuditTrailToJSON(null, null, null, null, null);
        assertNotNull("Audit trail JSON export should not be null", jsonBytes);
        assertTrue("Audit trail JSON export should not be empty", jsonBytes.length > 0);

        String jsonContent = new String(jsonBytes, StandardCharsets.UTF_8);

        // Verify JSON is valid array structure
        assertTrue("JSON should start with array bracket", jsonContent.trim().startsWith("["));
        assertTrue("JSON should end with array bracket", jsonContent.trim().endsWith("]"));
    }
}
