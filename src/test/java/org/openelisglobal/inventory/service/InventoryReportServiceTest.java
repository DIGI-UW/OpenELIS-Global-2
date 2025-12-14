package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryTransactionDAO;
import org.openelisglobal.inventory.dao.InventoryUsageDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.inventory.valueholder.reports.ExpirationForecastReportData;
import org.openelisglobal.inventory.valueholder.reports.LotTraceabilityReportData;
import org.openelisglobal.inventory.valueholder.reports.LowStockReportData;
import org.openelisglobal.inventory.valueholder.reports.StockLevelReportData;
import org.openelisglobal.inventory.valueholder.reports.TransactionHistoryReportData;
import org.openelisglobal.inventory.valueholder.reports.UsageTrendReportData;

/**
 * Unit tests for InventoryReportService using Mockito Tests business logic for
 * all 6 report types: 1. Stock Level Report 2. Low Stock Report 3. Expiration
 * Forecast Report 4. Transaction History Report 5. Usage Trends Report 6. Lot
 * Traceability Report
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryReportServiceTest {

    @Mock
    private InventoryItemDAO inventoryItemDAO;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private InventoryTransactionDAO inventoryTransactionDAO;

    @Mock
    private InventoryUsageDAO inventoryUsageDAO;

    @InjectMocks
    private InventoryReportServiceImpl inventoryReportService;

    private InventoryItem testItem1;
    private InventoryItem testItem2;
    private InventoryLot testLot1;
    private InventoryLot testLot2;
    private InventoryLot testLot3;
    private InventoryStorageLocation testLocation;

    @Before
    public void setUp() {
        // Setup test storage location
        testLocation = new InventoryStorageLocation();
        testLocation.setId(100L);
        testLocation.setName("Refrigerator 1");

        // Setup test items
        testItem1 = new InventoryItem();
        testItem1.setId(1L);
        testItem1.setName("Test Reagent A");
        testItem1.setItemType(ItemType.REAGENT);
        testItem1.setUnits("mL");
        testItem1.setLowStockThreshold(50);
        testItem1.setCategory("Chemistry");
        testItem1.setIsActive("Y");

        testItem2 = new InventoryItem();
        testItem2.setId(2L);
        testItem2.setName("Test RDT Kit");
        testItem2.setItemType(ItemType.RDT);
        testItem2.setUnits("tests");
        testItem2.setLowStockThreshold(100);
        testItem2.setCategory("Malaria");
        testItem2.setIsActive("Y");

        // Setup test lots
        testLot1 = new InventoryLot();
        testLot1.setId(10L);
        testLot1.setLotNumber("LOT001");
        testLot1.setInventoryItem(testItem1);
        testLot1.setStorageLocation(testLocation);
        testLot1.setCurrentQuantity(80.0);
        testLot1.setInitialQuantity(100.0);
        testLot1.setQcStatus(QCStatus.PASSED);
        testLot1.setStatus(LotStatus.ACTIVE);
        testLot1.setExpirationDate(Timestamp.valueOf(LocalDate.now().plusDays(60).atStartOfDay()));
        testLot1.setReceiptDate(Timestamp.valueOf(LocalDate.now().minusDays(10).atStartOfDay()));

        testLot2 = new InventoryLot();
        testLot2.setId(11L);
        testLot2.setLotNumber("LOT002");
        testLot2.setInventoryItem(testItem1);
        testLot2.setStorageLocation(testLocation);
        testLot2.setCurrentQuantity(20.0);
        testLot2.setInitialQuantity(50.0);
        testLot2.setQcStatus(QCStatus.PASSED);
        testLot2.setStatus(LotStatus.ACTIVE);
        testLot2.setExpirationDate(Timestamp.valueOf(LocalDate.now().plusDays(30).atStartOfDay()));
        testLot2.setReceiptDate(Timestamp.valueOf(LocalDate.now().minusDays(20).atStartOfDay()));

        testLot3 = new InventoryLot();
        testLot3.setId(12L);
        testLot3.setLotNumber("LOT003");
        testLot3.setInventoryItem(testItem2);
        testLot3.setStorageLocation(testLocation);
        testLot3.setCurrentQuantity(30.0);
        testLot3.setInitialQuantity(100.0);
        testLot3.setQcStatus(QCStatus.PASSED);
        testLot3.setStatus(LotStatus.ACTIVE);
        testLot3.setExpirationDate(Timestamp.valueOf(LocalDate.now().plusDays(10).atStartOfDay()));
        testLot3.setReceiptDate(Timestamp.valueOf(LocalDate.now().minusDays(5).atStartOfDay()));
    }

    /**
     * T015: Test generateStockLevelData - verify aggregation across lots
     */
    @Test
    public void testGenerateStockLevelData_shouldAggregateQuantitiesAcrossLots() {
        // Arrange
        List<InventoryItem> items = Arrays.asList(testItem1, testItem2);
        when(inventoryItemDAO.getAllActive()).thenReturn(items);

        when(inventoryLotDAO.getByInventoryItemId(1L)).thenReturn(Arrays.asList(testLot1, testLot2));
        when(inventoryLotDAO.getByInventoryItemId(2L)).thenReturn(Arrays.asList(testLot3));

        // Act
        List<StockLevelReportData> result = inventoryReportService.generateStockLevelData(false, false, false);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 2 items", 2, result.size());

        // Verify item 1 aggregation (80 + 20 = 100)
        StockLevelReportData item1Data = result.get(0);
        assertEquals("Item name should match", "Test Reagent A", item1Data.getItemName());
        assertEquals("Total quantity should be sum of lots", new BigDecimal("100.00"), item1Data.getTotalQuantity());
        assertEquals("Should have 2 active lots", Integer.valueOf(2), item1Data.getActiveLotCount());
        assertFalse("Should not be below reorder level (100 >= 50)", item1Data.getIsBelowReorderLevel());

        // Verify item 2
        StockLevelReportData item2Data = result.get(1);
        assertEquals("Item name should match", "Test RDT Kit", item2Data.getItemName());
        assertEquals("Total quantity should be 30", new BigDecimal("30.00"), item2Data.getTotalQuantity());
        assertTrue("Should be below reorder level (30 < 100)", item2Data.getIsBelowReorderLevel());

        // Verify DAO interactions
        verify(inventoryItemDAO).getAllActive();
        verify(inventoryLotDAO, times(2)).getByInventoryItemId(anyLong());
    }

    /**
     * T016: Test generateLowStockData - verify threshold filtering
     */
    @Test
    public void testGenerateLowStockData_shouldFilterItemsBelowReorderLevel() {
        // Arrange
        List<InventoryItem> items = Arrays.asList(testItem1, testItem2);
        when(inventoryItemDAO.getAllActive()).thenReturn(items);

        when(inventoryLotDAO.getByInventoryItemId(1L)).thenReturn(Arrays.asList(testLot1, testLot2));
        when(inventoryLotDAO.getByInventoryItemId(2L)).thenReturn(Arrays.asList(testLot3));

        // Act
        List<LowStockReportData> result = inventoryReportService.generateLowStockData(false);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return only 1 low stock item", 1, result.size());

        LowStockReportData lowStockItem = result.get(0);
        assertEquals("Should be item 2", "Test RDT Kit", lowStockItem.getItemName());
        assertEquals("Current quantity should be 30", new BigDecimal("30.00"), lowStockItem.getCurrentQuantity());
        assertEquals("Reorder level should be 100", new BigDecimal("100"), lowStockItem.getReorderLevel());
        assertEquals("Shortfall should be 70", new BigDecimal("70.00"), lowStockItem.getShortfall());

        // Recommended order quantity should be at least the shortfall
        assertTrue("Recommended order quantity should be at least 70",
                lowStockItem.getRecommendedOrderQuantity().compareTo(new BigDecimal("70.00")) >= 0);

        verify(inventoryItemDAO).getAllActive();
    }

    /**
     * T017: Test generateExpirationForecastData - verify date range filtering
     */
    @Test
    public void testGenerateExpirationForecastData_shouldFilterByDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(45);

        List<InventoryLot> allLots = Arrays.asList(testLot1, testLot2, testLot3);
        when(inventoryLotDAO.getAll()).thenReturn(allLots);

        // Act
        List<ExpirationForecastReportData> result = inventoryReportService.generateExpirationForecastData(startDate,
                endDate, true);

        // Assert
        assertNotNull("Result should not be null", result);
        // LOT002 (30 days) and LOT003 (10 days) should be in range, LOT001 (60 days)
        // should be excluded
        assertEquals("Should return 2 lots expiring within 45 days", 2, result.size());

        // Verify lot with shortest expiration (LOT003 - 10 days)
        ExpirationForecastReportData expiringLot = result.stream().filter(lot -> "LOT003".equals(lot.getLotNumber()))
                .findFirst().orElse(null);

        assertNotNull("LOT003 should be in results", expiringLot);
        assertEquals("Item name should match", "Test RDT Kit", expiringLot.getItemName());
        assertTrue("Days until expiration should be around 10",
                expiringLot.getDaysUntilExpiration() >= 9 && expiringLot.getDaysUntilExpiration() <= 11);

        verify(inventoryLotDAO).getAll();
    }

    /**
     * T018: Test generateTransactionHistoryData - verify date range queries
     */
    @Test
    public void testGenerateTransactionHistoryData_shouldQueryTransactionsWithinDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        InventoryTransaction transaction1 = new InventoryTransaction();
        transaction1.setId(100L);
        transaction1.setLot(testLot1);
        transaction1.setTransactionType(TransactionType.CONSUMPTION);
        transaction1.setQuantityChange(-20.0);
        transaction1.setQuantityAfter(80.0);
        transaction1.setTransactionDate(Timestamp.valueOf(LocalDate.now().minusDays(3).atStartOfDay()));
        transaction1.setPerformedByUser(1);

        InventoryTransaction transaction2 = new InventoryTransaction();
        transaction2.setId(101L);
        transaction2.setLot(testLot2);
        transaction2.setTransactionType(TransactionType.RECEIPT);
        transaction2.setQuantityChange(50.0);
        transaction2.setQuantityAfter(50.0);
        transaction2.setTransactionDate(Timestamp.valueOf(LocalDate.now().minusDays(5).atStartOfDay()));
        transaction2.setPerformedByUser(1);

        List<InventoryTransaction> transactions = Arrays.asList(transaction1, transaction2);
        when(inventoryTransactionDAO.getByDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(transactions);

        // Act
        List<TransactionHistoryReportData> result = inventoryReportService.generateTransactionHistoryData(startDate,
                endDate);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 2 transactions", 2, result.size());

        TransactionHistoryReportData txData1 = result.get(0);
        assertEquals("Transaction type should be CONSUMPTION", "CONSUMPTION", txData1.getTransactionType());
        assertEquals("Quantity change should be -20", new BigDecimal("-20.00"), txData1.getQuantityChange());
        assertEquals("Item name should match", "Test Reagent A", txData1.getItemName());

        verify(inventoryTransactionDAO).getByDateRange(any(Timestamp.class), any(Timestamp.class));
    }

    /**
     * T019: Test generateUsageTrendData - verify consumption calculations
     */
    @Test
    public void testGenerateUsageTrendData_shouldCalculateAverageConsumption() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Create consumption transactions for item 1
        List<InventoryTransaction> consumptionTransactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            InventoryTransaction tx = new InventoryTransaction();
            tx.setId((long) i);
            tx.setLot(testLot1);
            tx.setTransactionType(TransactionType.CONSUMPTION);
            tx.setQuantityChange(-10.0); // 10 units consumed per transaction
            tx.setTransactionDate(Timestamp.valueOf(LocalDate.now().minusDays(i * 3).atStartOfDay()));
            consumptionTransactions.add(tx);
        }

        when(inventoryTransactionDAO.getByDateRange(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(consumptionTransactions);

        List<InventoryItem> items = Arrays.asList(testItem1);
        when(inventoryItemDAO.getAllActive()).thenReturn(items);

        // Act
        List<UsageTrendReportData> result = inventoryReportService.generateUsageTrendData(startDate, endDate);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should return at least 1 item", result.size() >= 1);

        UsageTrendReportData trendData = result.stream().filter(data -> "Test Reagent A".equals(data.getItemName()))
                .findFirst().orElse(null);

        assertNotNull("Should find trend data for Test Reagent A", trendData);
        assertEquals("Total consumed should be 100 (10 tx * 10 units)", new BigDecimal("100.00"),
                trendData.getTotalConsumed());
        assertTrue("Average daily usage should be positive",
                trendData.getAverageDailyUsage().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("Transaction count should be 10", Integer.valueOf(10), trendData.getTransactionCount());
    }

    /**
     * T020: Test generateLotTraceabilityData - verify lot-to-test linkage
     */
    @Test
    public void testGenerateLotTraceabilityData_shouldLinkLotsToTestResults() {
        // Arrange
        InventoryUsage usage1 = new InventoryUsage();
        usage1.setId(200L);
        usage1.setLot(testLot1);
        usage1.setInventoryItem(testItem1);
        usage1.setTestResultId(5000L);
        usage1.setAnalysisId(6000L);
        usage1.setQuantityUsed(5.0);
        usage1.setUsageDate(Timestamp.valueOf(LocalDateTime.now().minusDays(2)));
        usage1.setPerformedByUser(1);

        InventoryUsage usage2 = new InventoryUsage();
        usage2.setId(201L);
        usage2.setLot(testLot1);
        usage2.setInventoryItem(testItem1);
        usage2.setTestResultId(5001L);
        usage2.setAnalysisId(6001L);
        usage2.setQuantityUsed(3.0);
        usage2.setUsageDate(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
        usage2.setPerformedByUser(1);

        when(inventoryUsageDAO.getByLotId(10L)).thenReturn(Arrays.asList(usage1, usage2));
        when(inventoryLotDAO.getByLotNumber("LOT001")).thenReturn(testLot1);

        // Act
        List<LotTraceabilityReportData> result = inventoryReportService.generateLotTraceabilityData("LOT001");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 2 usage records", 2, result.size());

        LotTraceabilityReportData traceData1 = result.get(0);
        assertEquals("Lot number should match", "LOT001", traceData1.getLotNumber());
        assertEquals("Item name should match", "Test Reagent A", traceData1.getItemName());
        assertEquals("Test result ID should match", "5000", traceData1.getTestResultId());
        assertEquals("Quantity used should be 5", new BigDecimal("5.00"), traceData1.getQuantityUsed());

        verify(inventoryLotDAO).getByLotNumber("LOT001");
        verify(inventoryUsageDAO).getByLotId(10L);
    }

    /**
     * Test empty result scenarios
     */
    @Test
    public void testGenerateStockLevelData_shouldReturnEmptyListWhenNoItems() {
        // Arrange
        when(inventoryItemDAO.getAllActive()).thenReturn(new ArrayList<>());

        // Act
        List<StockLevelReportData> result = inventoryReportService.generateStockLevelData(false, false, false);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void testGenerateLowStockData_shouldReturnEmptyListWhenNoLowStock() {
        // Arrange
        testItem1.setLowStockThreshold(10); // Set very low threshold
        List<InventoryItem> items = Arrays.asList(testItem1);
        when(inventoryItemDAO.getAllActive()).thenReturn(items);
        when(inventoryLotDAO.getByInventoryItemId(1L)).thenReturn(Arrays.asList(testLot1, testLot2));

        // Act
        List<LowStockReportData> result = inventoryReportService.generateLowStockData(false);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty when no items below threshold", result.isEmpty());
    }

    @Test
    public void testGenerateLotTraceabilityData_shouldReturnAllLotsWhenLotNumberIsNull() {
        // Arrange
        when(inventoryLotDAO.getAll()).thenReturn(Arrays.asList(testLot1, testLot2, testLot3));
        when(inventoryUsageDAO.getByLotId(anyLong())).thenReturn(new ArrayList<>());

        // Act
        List<LotTraceabilityReportData> result = inventoryReportService.generateLotTraceabilityData(null);

        // Assert
        assertNotNull("Result should not be null", result);
        verify(inventoryLotDAO).getAll();
    }
}
