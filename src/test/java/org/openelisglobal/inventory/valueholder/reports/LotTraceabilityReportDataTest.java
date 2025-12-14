package org.openelisglobal.inventory.valueholder.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Unit tests for LotTraceabilityReportData bean Tests all getter/setter methods
 * for JasperReports compatibility
 */
public class LotTraceabilityReportDataTest {

    @Test
    public void testGettersAndSetters() {
        // Given - Create bean instance
        LotTraceabilityReportData data = new LotTraceabilityReportData();

        // When - Set all fields
        data.setLotId("LOT-001");
        data.setLotNumber("BATCH-2024-123");
        data.setItemName("Test Reagent XYZ");
        data.setManufacturer("ACME Corp");
        data.setTestResultId("TEST-001");
        data.setAccessionNumber("ACC-2024-456");
        data.setPatientId("PAT-789");
        data.setTestName("Blood Chemistry Panel");
        data.setPerformedBy("Jane Smith");
        data.setUnitOfMeasure("mL");
        data.setExpirationDate(LocalDate.of(2024, 12, 31));
        data.setReceivedDate(LocalDate.of(2024, 1, 15));
        data.setTestDate(LocalDateTime.now());
        data.setQuantityUsed(new BigDecimal("5.50"));

        // Then - Verify all getters return correct values
        assertEquals("LOT-001", data.getLotId());
        assertEquals("BATCH-2024-123", data.getLotNumber());
        assertEquals("Test Reagent XYZ", data.getItemName());
        assertEquals("ACME Corp", data.getManufacturer());
        assertEquals("TEST-001", data.getTestResultId());
        assertEquals("ACC-2024-456", data.getAccessionNumber());
        assertEquals("PAT-789", data.getPatientId());
        assertEquals("Blood Chemistry Panel", data.getTestName());
        assertEquals("Jane Smith", data.getPerformedBy());
        assertEquals("mL", data.getUnitOfMeasure());
        assertEquals(LocalDate.of(2024, 12, 31), data.getExpirationDate());
        assertEquals(LocalDate.of(2024, 1, 15), data.getReceivedDate());
        assertNotNull(data.getTestDate());
        assertEquals(new BigDecimal("5.50"), data.getQuantityUsed());
    }

    @Test
    public void testTraceabilityChain() {
        // Given
        LotTraceabilityReportData data = new LotTraceabilityReportData();

        // When - Set traceability fields
        data.setLotNumber("BATCH-2024-123");
        data.setAccessionNumber("ACC-2024-456");
        data.setTestResultId("TEST-001");

        // Then - Verify traceability chain
        assertEquals("BATCH-2024-123", data.getLotNumber());
        assertEquals("ACC-2024-456", data.getAccessionNumber());
        assertEquals("TEST-001", data.getTestResultId());
    }

    @Test
    public void testNullableFields() {
        // Given
        LotTraceabilityReportData data = new LotTraceabilityReportData();

        // When - Leave optional fields null
        data.setLotId("LOT-002");
        data.setItemName("Minimal Data");

        // Then - Should handle nulls gracefully
        assertEquals("LOT-002", data.getLotId());
        assertEquals("Minimal Data", data.getItemName());
        // Other fields remain null and accessible via getters
    }
}
