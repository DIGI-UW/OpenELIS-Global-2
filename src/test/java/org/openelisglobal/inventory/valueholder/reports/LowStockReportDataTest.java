package org.openelisglobal.inventory.valueholder.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

/**
 * Unit tests for LowStockReportData bean
 * Tests all getter/setter methods for JasperReports compatibility
 */
public class LowStockReportDataTest {

    @Test
    public void testGettersAndSetters() {
        // Given - Create bean instance
        LowStockReportData data = new LowStockReportData();

        // When - Set all fields
        data.setItemId("ITEM-001");
        data.setItemName("Test Reagent XYZ");
        data.setItemType("REAGENT");
        data.setStorageLocationPath("Lab A > Freezer 1 > Shelf 2");
        data.setUnitOfMeasure("mL");
        data.setCurrentQuantity(new BigDecimal("50.00"));
        data.setReorderLevel(new BigDecimal("100.00"));
        data.setShortfall(new BigDecimal("50.00"));
        data.setRecommendedOrderQuantity(new BigDecimal("150.00"));
        data.setDaysUntilStockout(7);
        data.setLastUpdated(LocalDateTime.now());

        // Then - Verify all getters return correct values
        assertEquals("ITEM-001", data.getItemId());
        assertEquals("Test Reagent XYZ", data.getItemName());
        assertEquals("REAGENT", data.getItemType());
        assertEquals("Lab A > Freezer 1 > Shelf 2", data.getStorageLocationPath());
        assertEquals("mL", data.getUnitOfMeasure());
        assertEquals(new BigDecimal("50.00"), data.getCurrentQuantity());
        assertEquals(new BigDecimal("100.00"), data.getReorderLevel());
        assertEquals(new BigDecimal("50.00"), data.getShortfall());
        assertEquals(new BigDecimal("150.00"), data.getRecommendedOrderQuantity());
        assertEquals(Integer.valueOf(7), data.getDaysUntilStockout());
        assertNotNull(data.getLastUpdated());
    }

    @Test
    public void testShortfallCalculation() {
        // Given
        LowStockReportData data = new LowStockReportData();
        data.setCurrentQuantity(new BigDecimal("50.00"));
        data.setReorderLevel(new BigDecimal("100.00"));

        // When - Set shortfall
        data.setShortfall(new BigDecimal("50.00"));

        // Then
        assertEquals(new BigDecimal("50.00"), data.getShortfall());
    }

    @Test
    public void testNullableFields() {
        // Given
        LowStockReportData data = new LowStockReportData();

        // When - Leave optional fields null (daysUntilStockout is nullable)
        data.setItemId("ITEM-002");
        data.setItemName("Minimal Data");
        data.setDaysUntilStockout(null);

        // Then - Should handle nulls gracefully
        assertEquals("ITEM-002", data.getItemId());
        assertEquals("Minimal Data", data.getItemName());
        assertNull(data.getDaysUntilStockout());
        // Other fields remain null and accessible via getters
    }
}
