package org.openelisglobal.inventory.valueholder.reports;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import org.junit.Test;

/**
 * Unit tests for UsageTrendReportData bean Tests all getter/setter methods for
 * JasperReports compatibility
 */
public class UsageTrendReportDataTest {

    @Test
    public void testGettersAndSetters() {
        // Given - Create bean instance
        UsageTrendReportData data = new UsageTrendReportData();

        // When - Set all fields
        data.setItemId("ITEM-001");
        data.setItemName("Test Reagent XYZ");
        data.setItemType("REAGENT");
        data.setUnitOfMeasure("mL");
        data.setTrendIndicator("INCREASING");
        data.setPeakUsageMonthName("March");
        data.setTotalConsumed(new BigDecimal("500.00"));
        data.setAverageDailyUsage(new BigDecimal("5.50"));
        data.setAverageWeeklyUsage(new BigDecimal("38.50"));
        data.setAverageMonthlyUsage(new BigDecimal("166.67"));
        data.setPeakUsageMonth(new BigDecimal("250.00"));
        data.setTransactionCount(45);

        // Then - Verify all getters return correct values
        assertEquals("ITEM-001", data.getItemId());
        assertEquals("Test Reagent XYZ", data.getItemName());
        assertEquals("REAGENT", data.getItemType());
        assertEquals("mL", data.getUnitOfMeasure());
        assertEquals("INCREASING", data.getTrendIndicator());
        assertEquals("March", data.getPeakUsageMonthName());
        assertEquals(new BigDecimal("500.00"), data.getTotalConsumed());
        assertEquals(new BigDecimal("5.50"), data.getAverageDailyUsage());
        assertEquals(new BigDecimal("38.50"), data.getAverageWeeklyUsage());
        assertEquals(new BigDecimal("166.67"), data.getAverageMonthlyUsage());
        assertEquals(new BigDecimal("250.00"), data.getPeakUsageMonth());
        assertEquals(Integer.valueOf(45), data.getTransactionCount());
    }

    @Test
    public void testTrendIndicators() {
        // Given
        UsageTrendReportData data = new UsageTrendReportData();

        // When - Set different trend indicators
        data.setTrendIndicator("DECREASING");

        // Then
        assertEquals("DECREASING", data.getTrendIndicator());
    }

    @Test
    public void testNullableFields() {
        // Given
        UsageTrendReportData data = new UsageTrendReportData();

        // When - Leave optional fields null
        data.setItemId("ITEM-002");
        data.setItemName("Minimal Data");

        // Then - Should handle nulls gracefully
        assertEquals("ITEM-002", data.getItemId());
        assertEquals("Minimal Data", data.getItemName());
        // Other fields remain null and accessible via getters
    }
}
