package org.openelisglobal.inventory.valueholder.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

/**
 * Unit tests for TransactionHistoryReportData bean
 * Tests all getter/setter methods for JasperReports compatibility
 */
public class TransactionHistoryReportDataTest {

    @Test
    public void testGettersAndSetters() {
        // Given - Create bean instance
        TransactionHistoryReportData data = new TransactionHistoryReportData();

        // When - Set all fields
        data.setTransactionId("TXN-001");
        data.setTransactionType("RECEIPT");
        data.setItemName("Test Reagent XYZ");
        data.setLotNumber("BATCH-2024-123");
        data.setStorageLocationPath("Lab A > Freezer 1 > Shelf 2");
        data.setUnitOfMeasure("mL");
        data.setPerformedBy("John Doe");
        data.setReason("Initial stock");
        data.setReferenceType("PURCHASE_ORDER");
        data.setReferenceId("PO-12345");
        data.setTransactionTimestamp(LocalDateTime.now());
        data.setQuantityChange(new BigDecimal("150.50"));
        data.setQuantityAfterTransaction(new BigDecimal("150.50"));

        // Then - Verify all getters return correct values
        assertEquals("TXN-001", data.getTransactionId());
        assertEquals("RECEIPT", data.getTransactionType());
        assertEquals("Test Reagent XYZ", data.getItemName());
        assertEquals("BATCH-2024-123", data.getLotNumber());
        assertEquals("Lab A > Freezer 1 > Shelf 2", data.getStorageLocationPath());
        assertEquals("mL", data.getUnitOfMeasure());
        assertEquals("John Doe", data.getPerformedBy());
        assertEquals("Initial stock", data.getReason());
        assertEquals("PURCHASE_ORDER", data.getReferenceType());
        assertEquals("PO-12345", data.getReferenceId());
        assertNotNull(data.getTransactionTimestamp());
        assertEquals(new BigDecimal("150.50"), data.getQuantityChange());
        assertEquals(new BigDecimal("150.50"), data.getQuantityAfterTransaction());
    }

    @Test
    public void testTransactionTypes() {
        // Given
        TransactionHistoryReportData data = new TransactionHistoryReportData();

        // When - Set different transaction types
        data.setTransactionType("CONSUMPTION");
        data.setQuantityChange(new BigDecimal("-50.00"));

        // Then
        assertEquals("CONSUMPTION", data.getTransactionType());
        assertEquals(new BigDecimal("-50.00"), data.getQuantityChange());
    }

    @Test
    public void testNullableFields() {
        // Given
        TransactionHistoryReportData data = new TransactionHistoryReportData();

        // When - Leave optional fields null
        data.setTransactionId("TXN-002");
        data.setTransactionType("ADJUSTMENT");

        // Then - Should handle nulls gracefully
        assertEquals("TXN-002", data.getTransactionId());
        assertEquals("ADJUSTMENT", data.getTransactionType());
        // Other fields remain null and accessible via getters
    }
}
