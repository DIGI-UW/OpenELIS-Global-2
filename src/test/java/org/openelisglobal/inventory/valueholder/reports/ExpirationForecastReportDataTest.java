package org.openelisglobal.inventory.valueholder.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

/**
 * Unit tests for ExpirationForecastReportData bean
 * Tests all getter/setter methods for JasperReports compatibility
 */
public class ExpirationForecastReportDataTest {

    @Test
    public void testGettersAndSetters() {
        // Given - Create bean instance
        ExpirationForecastReportData data = new ExpirationForecastReportData();

        // When - Set all fields
        data.setLotId("LOT-001");
        data.setLotNumber("BATCH-2024-123");
        data.setItemName("Test Reagent XYZ");
        data.setItemType("REAGENT");
        data.setStorageLocationPath("Lab A > Freezer 1 > Shelf 2");
        data.setUnitOfMeasure("mL");
        data.setExpirationStatus("EXPIRING_SOON");
        data.setManufacturer("ACME Corp");
        data.setQuantity(new BigDecimal("150.50"));
        data.setExpirationDate(LocalDate.of(2024, 12, 31));
        data.setReceivedDate(LocalDate.of(2024, 1, 15));
        data.setDaysUntilExpiration(30);

        // Then - Verify all getters return correct values
        assertEquals("LOT-001", data.getLotId());
        assertEquals("BATCH-2024-123", data.getLotNumber());
        assertEquals("Test Reagent XYZ", data.getItemName());
        assertEquals("REAGENT", data.getItemType());
        assertEquals("Lab A > Freezer 1 > Shelf 2", data.getStorageLocationPath());
        assertEquals("mL", data.getUnitOfMeasure());
        assertEquals("EXPIRING_SOON", data.getExpirationStatus());
        assertEquals("ACME Corp", data.getManufacturer());
        assertEquals(new BigDecimal("150.50"), data.getQuantity());
        assertEquals(LocalDate.of(2024, 12, 31), data.getExpirationDate());
        assertEquals(LocalDate.of(2024, 1, 15), data.getReceivedDate());
        assertEquals(Integer.valueOf(30), data.getDaysUntilExpiration());
    }

    @Test
    public void testExpirationStatusValues() {
        // Given
        ExpirationForecastReportData data = new ExpirationForecastReportData();

        // When - Set different expiration statuses
        data.setExpirationStatus("EXPIRED");

        // Then
        assertEquals("EXPIRED", data.getExpirationStatus());
    }

    @Test
    public void testNullableFields() {
        // Given
        ExpirationForecastReportData data = new ExpirationForecastReportData();

        // When - Leave optional fields null
        data.setLotId("LOT-002");
        data.setItemName("Minimal Data");

        // Then - Should handle nulls gracefully
        assertEquals("LOT-002", data.getLotId());
        assertEquals("Minimal Data", data.getItemName());
        // Other fields remain null and accessible via getters
    }
}
