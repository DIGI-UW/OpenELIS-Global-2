package org.openelisglobal.inventory.valueholder.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

/**
 * Unit tests for StockLevelReportData bean
 * Tests all getter/setter methods for JasperReports compatibility
 */
public class StockLevelReportDataTest {

    @Test
    public void testGettersAndSetters() {
        // Given - Create bean instance
        StockLevelReportData data = new StockLevelReportData();

        // When - Set all fields
        data.setItemId("ITEM-001");
        data.setItemName("Test Reagent XYZ");
        data.setItemType("REAGENT");
        data.setCategoryName("Chemistry");
        data.setStorageLocationPath("Lab A > Freezer 1 > Shelf 2");
        data.setStorageLocationId("LOC-123");
        data.setTotalQuantity(new BigDecimal("150.50"));
        data.setUnitOfMeasure("mL");
        data.setReorderLevel(new BigDecimal("100.00"));
        data.setIsBelowReorderLevel(false);
        data.setActiveLotCount(3);
        data.setLastUpdated(LocalDateTime.now());

        // Then - Verify all getters return correct values
        assertEquals("ITEM-001", data.getItemId());
        assertEquals("Test Reagent XYZ", data.getItemName());
        assertEquals("REAGENT", data.getItemType());
        assertEquals("Chemistry", data.getCategoryName());
        assertEquals("Lab A > Freezer 1 > Shelf 2", data.getStorageLocationPath());
        assertEquals("LOC-123", data.getStorageLocationId());
        assertEquals(new BigDecimal("150.50"), data.getTotalQuantity());
        assertEquals("mL", data.getUnitOfMeasure());
        assertEquals(new BigDecimal("100.00"), data.getReorderLevel());
        assertEquals(Boolean.FALSE, data.getIsBelowReorderLevel());
        assertEquals(Integer.valueOf(3), data.getActiveLotCount());
        assertNotNull(data.getLastUpdated());
    }

    @Test
    public void testBelowReorderLevelIndicator() {
        // Given
        StockLevelReportData data = new StockLevelReportData();
        data.setTotalQuantity(new BigDecimal("50.00"));
        data.setReorderLevel(new BigDecimal("100.00"));

        // When - Set below reorder level flag
        data.setIsBelowReorderLevel(true);

        // Then
        assertTrue(data.getIsBelowReorderLevel());
    }

    @Test
    public void testNullableFields() {
        // Given
        StockLevelReportData data = new StockLevelReportData();

        // When - Leave optional fields null
        data.setItemId("ITEM-002");
        data.setItemName("Minimal Data");

        // Then - Should handle nulls gracefully
        assertEquals("ITEM-002", data.getItemId());
        assertEquals("Minimal Data", data.getItemName());
        // Other fields remain null and accessible via getters
    }
}
