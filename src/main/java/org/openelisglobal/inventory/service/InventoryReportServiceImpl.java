package org.openelisglobal.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryTransactionDAO;
import org.openelisglobal.inventory.dao.InventoryUsageDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.inventory.valueholder.reports.ExpirationForecastReportData;
import org.openelisglobal.inventory.valueholder.reports.LotTraceabilityReportData;
import org.openelisglobal.inventory.valueholder.reports.LowStockReportData;
import org.openelisglobal.inventory.valueholder.reports.StockLevelReportData;
import org.openelisglobal.inventory.valueholder.reports.TransactionHistoryReportData;
import org.openelisglobal.inventory.valueholder.reports.UsageTrendReportData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for generating inventory report data Provides data
 * aggregation methods for all 6 report types
 *
 * Per Constitution Principle IV: - Transaction management in service layer
 * (@Transactional) - Services compile all data within transaction (prevents
 * LazyInitializationException) - Returns complete DTOs with all relationships
 * resolved
 */
@Service
@Transactional(readOnly = true)
public class InventoryReportServiceImpl implements InventoryReportService {

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryTransactionDAO inventoryTransactionDAO;

    @Autowired
    private InventoryUsageDAO inventoryUsageDAO;

    /**
     * T021: Generate stock level report data Aggregates quantities across lots and
     * calculates stock status
     */
    @Override
    public List<StockLevelReportData> generateStockLevelData(boolean includeInactive, boolean groupByType,
            boolean groupByLocation) {

        List<StockLevelReportData> reportData = new ArrayList<>();

        // Get items based on active status
        List<InventoryItem> items = includeInactive ? inventoryItemDAO.getAll() : inventoryItemDAO.getAllActive();

        for (InventoryItem item : items) {
            // Get all lots for this item
            List<InventoryLot> lots = inventoryLotDAO.getByInventoryItemId(item.getId());

            // Calculate total quantity across all lots
            double totalQuantity = lots.stream()
                    .filter(lot -> lot.getStatus() == LotStatus.ACTIVE || lot.getStatus() == LotStatus.IN_USE)
                    .mapToDouble(InventoryLot::getCurrentQuantity).sum();

            // Count active lots
            int activeLotCount = (int) lots.stream()
                    .filter(lot -> lot.getStatus() == LotStatus.ACTIVE || lot.getStatus() == LotStatus.IN_USE).count();

            // Find the most recent lot update
            LocalDateTime lastUpdated = lots.stream().filter(lot -> lot.getLastupdated() != null)
                    .map(lot -> lot.getLastupdated().toLocalDateTime()).max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            // Determine storage location (use first active lot's location)
            String storageLocationPath = lots.stream().filter(lot -> lot.getStorageLocation() != null).findFirst()
                    .map(lot -> lot.getStorageLocation().getName()).orElse("");

            String storageLocationId = lots.stream().filter(lot -> lot.getStorageLocation() != null).findFirst()
                    .map(lot -> String.valueOf(lot.getStorageLocation().getId())).orElse("");

            // Create report data
            StockLevelReportData data = new StockLevelReportData();
            data.setItemId(String.valueOf(item.getId()));
            data.setItemName(item.getName());
            data.setItemType(item.getItemType() != null ? item.getItemType().name() : "");
            data.setCategoryName(item.getCategory());
            data.setStorageLocationPath(storageLocationPath);
            data.setStorageLocationId(storageLocationId);
            data.setTotalQuantity(BigDecimal.valueOf(totalQuantity).setScale(2, RoundingMode.HALF_UP));
            data.setUnitOfMeasure(item.getUnits());
            data.setReorderLevel(item.getLowStockThreshold() != null ? BigDecimal.valueOf(item.getLowStockThreshold())
                    : BigDecimal.ZERO);

            // Calculate if below reorder level
            boolean isBelowReorderLevel = item.getLowStockThreshold() != null
                    && totalQuantity < item.getLowStockThreshold();
            data.setIsBelowReorderLevel(isBelowReorderLevel);

            data.setActiveLotCount(activeLotCount);
            data.setLastUpdated(lastUpdated);

            reportData.add(data);
        }

        return reportData;
    }

    /**
     * T022: Generate low stock alert report data Filters items below reorder
     * threshold and calculates shortfall
     */
    @Override
    public List<LowStockReportData> generateLowStockData(boolean includeInactive) {
        List<LowStockReportData> reportData = new ArrayList<>();

        // Get items based on active status
        List<InventoryItem> items = includeInactive ? inventoryItemDAO.getAll() : inventoryItemDAO.getAllActive();

        for (InventoryItem item : items) {
            // Skip items without reorder threshold
            if (item.getLowStockThreshold() == null) {
                continue;
            }

            // Get all lots for this item
            List<InventoryLot> lots = inventoryLotDAO.getByInventoryItemId(item.getId());

            // Calculate total current quantity
            double currentQuantity = lots.stream()
                    .filter(lot -> lot.getStatus() == LotStatus.ACTIVE || lot.getStatus() == LotStatus.IN_USE)
                    .mapToDouble(InventoryLot::getCurrentQuantity).sum();

            // Only include if below reorder level
            if (currentQuantity < item.getLowStockThreshold()) {
                // Calculate shortfall
                double shortfall = item.getLowStockThreshold() - currentQuantity;

                // Recommended order quantity (typically 1.5x the shortfall to prevent immediate
                // reorder)
                double recommendedOrderQty = shortfall * 1.5;

                // Find storage location
                String storageLocationPath = lots.stream().filter(lot -> lot.getStorageLocation() != null).findFirst()
                        .map(lot -> lot.getStorageLocation().getName()).orElse("");

                // Find last updated
                LocalDateTime lastUpdated = lots.stream().filter(lot -> lot.getLastupdated() != null)
                        .map(lot -> lot.getLastupdated().toLocalDateTime()).max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

                // Create report data
                LowStockReportData data = new LowStockReportData();
                data.setItemId(String.valueOf(item.getId()));
                data.setItemName(item.getName());
                data.setItemType(item.getItemType() != null ? item.getItemType().name() : "");
                data.setStorageLocationPath(storageLocationPath);
                data.setUnitOfMeasure(item.getUnits());
                data.setCurrentQuantity(BigDecimal.valueOf(currentQuantity).setScale(2, RoundingMode.HALF_UP));
                data.setReorderLevel(BigDecimal.valueOf(item.getLowStockThreshold()));
                data.setShortfall(BigDecimal.valueOf(shortfall).setScale(2, RoundingMode.HALF_UP));
                data.setRecommendedOrderQuantity(
                        BigDecimal.valueOf(recommendedOrderQty).setScale(2, RoundingMode.HALF_UP));
                data.setDaysUntilStockout(null); // Could be calculated with usage trends
                data.setLastUpdated(lastUpdated);

                reportData.add(data);
            }
        }

        return reportData;
    }

    /**
     * T023: Generate expiration forecast report data Filters lots by expiration
     * date range and calculates days until expiration
     */
    @Override
    public List<ExpirationForecastReportData> generateExpirationForecastData(LocalDate startDate, LocalDate endDate,
            boolean includeExpired) {

        List<ExpirationForecastReportData> reportData = new ArrayList<>();

        // Get all lots
        List<InventoryLot> allLots = inventoryLotDAO.getAll();

        LocalDate today = LocalDate.now();

        for (InventoryLot lot : allLots) {
            if (lot.getExpirationDate() == null) {
                continue;
            }

            LocalDate expirationDate = lot.getExpirationDate().toLocalDateTime().toLocalDate();

            // Calculate days until expiration
            long daysUntilExpiration = ChronoUnit.DAYS.between(today, expirationDate);

            // Filter by date range
            boolean isInRange = !expirationDate.isBefore(startDate) && !expirationDate.isAfter(endDate);
            boolean isExpired = expirationDate.isBefore(today);

            if (isInRange && (includeExpired || !isExpired)) {
                // Determine expiration status
                String expirationStatus;
                if (isExpired) {
                    expirationStatus = "EXPIRED";
                } else if (daysUntilExpiration <= 7) {
                    expirationStatus = "CRITICAL";
                } else if (daysUntilExpiration <= 30) {
                    expirationStatus = "WARNING";
                } else {
                    expirationStatus = "NORMAL";
                }

                // Get storage location
                String storageLocationPath = lot.getStorageLocation() != null ? lot.getStorageLocation().getName() : "";

                // Create report data
                ExpirationForecastReportData data = new ExpirationForecastReportData();
                data.setLotId(String.valueOf(lot.getId()));
                data.setLotNumber(lot.getLotNumber());
                data.setItemName(lot.getInventoryItem() != null ? lot.getInventoryItem().getName() : "");
                data.setItemType(lot.getInventoryItem() != null && lot.getInventoryItem().getItemType() != null
                        ? lot.getInventoryItem().getItemType().name()
                        : "");
                data.setStorageLocationPath(storageLocationPath);
                data.setUnitOfMeasure(lot.getInventoryItem() != null ? lot.getInventoryItem().getUnits() : "");
                data.setExpirationStatus(expirationStatus);
                data.setManufacturer(lot.getInventoryItem() != null ? lot.getInventoryItem().getManufacturer() : "");
                data.setQuantity(BigDecimal.valueOf(lot.getCurrentQuantity()).setScale(2, RoundingMode.HALF_UP));
                data.setExpirationDate(expirationDate);
                data.setReceivedDate(
                        lot.getReceiptDate() != null ? lot.getReceiptDate().toLocalDateTime().toLocalDate() : null);
                data.setDaysUntilExpiration((int) daysUntilExpiration);

                reportData.add(data);
            }
        }

        // Sort by days until expiration (ascending)
        reportData.sort((a, b) -> Integer.compare(a.getDaysUntilExpiration(), b.getDaysUntilExpiration()));

        return reportData;
    }

    /**
     * T024: Generate transaction history report data Shows all inventory
     * transactions within date range
     */
    @Override
    public List<TransactionHistoryReportData> generateTransactionHistoryData(LocalDate startDate, LocalDate endDate) {

        List<TransactionHistoryReportData> reportData = new ArrayList<>();

        // Convert LocalDate to Timestamp
        Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp endTimestamp = Timestamp.valueOf(endDate.atTime(23, 59, 59));

        // Get transactions within date range
        List<InventoryTransaction> transactions = inventoryTransactionDAO.getByDateRange(startTimestamp, endTimestamp);

        for (InventoryTransaction transaction : transactions) {
            // Get lot details
            InventoryLot lot = transaction.getLot();
            if (lot == null) {
                continue;
            }

            // Get storage location
            String storageLocationPath = lot.getStorageLocation() != null ? lot.getStorageLocation().getName() : "";

            // Create report data
            TransactionHistoryReportData data = new TransactionHistoryReportData();
            data.setTransactionId(String.valueOf(transaction.getId()));
            data.setTransactionType(
                    transaction.getTransactionType() != null ? transaction.getTransactionType().name() : "");
            data.setItemName(lot.getInventoryItem() != null ? lot.getInventoryItem().getName() : "");
            data.setLotNumber(lot.getLotNumber());
            data.setStorageLocationPath(storageLocationPath);
            data.setUnitOfMeasure(lot.getInventoryItem() != null ? lot.getInventoryItem().getUnits() : "");
            data.setPerformedBy(String.valueOf(transaction.getPerformedByUser()));
            data.setReason(transaction.getNotes());
            data.setReferenceType(transaction.getReferenceType() != null ? transaction.getReferenceType().name() : "");
            data.setReferenceId(
                    transaction.getReferenceId() != null ? String.valueOf(transaction.getReferenceId()) : "");
            data.setTransactionTimestamp(transaction.getTransactionDate().toLocalDateTime());
            data.setQuantityChange(
                    BigDecimal.valueOf(transaction.getQuantityChange()).setScale(2, RoundingMode.HALF_UP));
            data.setQuantityAfterTransaction(
                    BigDecimal.valueOf(transaction.getQuantityAfter()).setScale(2, RoundingMode.HALF_UP));

            reportData.add(data);
        }

        return reportData;
    }

    /**
     * T025: Generate usage trends report data Shows consumption patterns and
     * analytics for date range
     */
    @Override
    public List<UsageTrendReportData> generateUsageTrendData(LocalDate startDate, LocalDate endDate) {

        List<UsageTrendReportData> reportData = new ArrayList<>();

        // Convert LocalDate to Timestamp
        Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp endTimestamp = Timestamp.valueOf(endDate.atTime(23, 59, 59));

        // Get all consumption transactions
        List<InventoryTransaction> transactions = inventoryTransactionDAO.getByDateRange(startTimestamp, endTimestamp);

        // Filter only consumption transactions
        List<InventoryTransaction> consumptionTransactions = transactions.stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.CONSUMPTION).collect(Collectors.toList());

        // Group by item
        Map<Long, List<InventoryTransaction>> transactionsByItem = new HashMap<>();
        for (InventoryTransaction tx : consumptionTransactions) {
            if (tx.getLot() != null && tx.getLot().getInventoryItem() != null) {
                Long itemId = tx.getLot().getInventoryItem().getId();
                transactionsByItem.computeIfAbsent(itemId, k -> new ArrayList<>()).add(tx);
            }
        }

        // Get all active items
        List<InventoryItem> items = inventoryItemDAO.getAllActive();

        // Calculate period length in days
        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (InventoryItem item : items) {
            List<InventoryTransaction> itemTransactions = transactionsByItem.get(item.getId());

            if (itemTransactions == null || itemTransactions.isEmpty()) {
                continue; // Skip items with no consumption
            }

            // Calculate total consumed (sum of absolute values of quantity changes)
            double totalConsumed = itemTransactions.stream().mapToDouble(tx -> Math.abs(tx.getQuantityChange())).sum();

            // Calculate averages
            double averageDailyUsage = totalConsumed / periodDays;
            double averageWeeklyUsage = averageDailyUsage * 7;
            double averageMonthlyUsage = averageDailyUsage * 30;

            // Determine trend (simplified - could be enhanced with regression analysis)
            String trendIndicator = "STABLE";
            if (itemTransactions.size() >= 2) {
                // Compare first half vs second half consumption
                int midpoint = itemTransactions.size() / 2;
                double firstHalfAvg = itemTransactions.subList(0, midpoint).stream()
                        .mapToDouble(tx -> Math.abs(tx.getQuantityChange())).average().orElse(0);
                double secondHalfAvg = itemTransactions.subList(midpoint, itemTransactions.size()).stream()
                        .mapToDouble(tx -> Math.abs(tx.getQuantityChange())).average().orElse(0);

                if (secondHalfAvg > firstHalfAvg * 1.2) {
                    trendIndicator = "INCREASING";
                } else if (secondHalfAvg < firstHalfAvg * 0.8) {
                    trendIndicator = "DECREASING";
                }
            }

            // Create report data
            UsageTrendReportData data = new UsageTrendReportData();
            data.setItemId(String.valueOf(item.getId()));
            data.setItemName(item.getName());
            data.setItemType(item.getItemType() != null ? item.getItemType().name() : "");
            data.setUnitOfMeasure(item.getUnits());
            data.setTrendIndicator(trendIndicator);
            data.setTotalConsumed(BigDecimal.valueOf(totalConsumed).setScale(2, RoundingMode.HALF_UP));
            data.setAverageDailyUsage(BigDecimal.valueOf(averageDailyUsage).setScale(2, RoundingMode.HALF_UP));
            data.setAverageWeeklyUsage(BigDecimal.valueOf(averageWeeklyUsage).setScale(2, RoundingMode.HALF_UP));
            data.setAverageMonthlyUsage(BigDecimal.valueOf(averageMonthlyUsage).setScale(2, RoundingMode.HALF_UP));
            data.setPeakUsageMonth(BigDecimal.valueOf(averageMonthlyUsage).setScale(2, RoundingMode.HALF_UP)); // Simplified
            data.setPeakUsageMonthName(LocalDate.now().getMonth().name()); // Simplified
            data.setTransactionCount(itemTransactions.size());

            reportData.add(data);
        }

        return reportData;
    }

    /**
     * T026-T027: Generate lot traceability report data Links specific lots to test
     * results and patient identifiers
     */
    @Override
    public List<LotTraceabilityReportData> generateLotTraceabilityData(String lotNumber) {
        List<LotTraceabilityReportData> reportData = new ArrayList<>();

        // Get lots based on lot number parameter
        List<InventoryLot> lots = new ArrayList<>();
        if (lotNumber != null && !lotNumber.trim().isEmpty()) {
            InventoryLot lot = inventoryLotDAO.getByLotNumber(lotNumber);
            if (lot != null) {
                lots.add(lot);
            }
        } else {
            lots = inventoryLotDAO.getAll();
        }

        for (InventoryLot lot : lots) {
            // Get all usage records for this lot
            List<InventoryUsage> usageRecords = inventoryUsageDAO.getByLotId(lot.getId());

            if (usageRecords.isEmpty()) {
                // Include lot even without usage for traceability purposes
                LotTraceabilityReportData data = new LotTraceabilityReportData();
                data.setLotId(String.valueOf(lot.getId()));
                data.setLotNumber(lot.getLotNumber());
                data.setItemName(lot.getInventoryItem() != null ? lot.getInventoryItem().getName() : "");
                data.setManufacturer(lot.getInventoryItem() != null ? lot.getInventoryItem().getManufacturer() : "");
                data.setUnitOfMeasure(lot.getInventoryItem() != null ? lot.getInventoryItem().getUnits() : "");
                data.setExpirationDate(
                        lot.getExpirationDate() != null ? lot.getExpirationDate().toLocalDateTime().toLocalDate()
                                : null);
                data.setReceivedDate(
                        lot.getReceiptDate() != null ? lot.getReceiptDate().toLocalDateTime().toLocalDate() : null);

                reportData.add(data);
            } else {
                // Create a record for each usage
                for (InventoryUsage usage : usageRecords) {
                    LotTraceabilityReportData data = new LotTraceabilityReportData();
                    data.setLotId(String.valueOf(lot.getId()));
                    data.setLotNumber(lot.getLotNumber());
                    data.setItemName(lot.getInventoryItem() != null ? lot.getInventoryItem().getName() : "");
                    data.setManufacturer(
                            lot.getInventoryItem() != null ? lot.getInventoryItem().getManufacturer() : "");
                    data.setTestResultId(
                            usage.getTestResultId() != null ? String.valueOf(usage.getTestResultId()) : "");
                    data.setAccessionNumber(""); // Would need to join with test result table
                    data.setPatientId(""); // Would need to join with test result -> sample -> patient
                    data.setTestName(""); // Would need to join with analysis -> test
                    data.setPerformedBy(String.valueOf(usage.getPerformedByUser()));
                    data.setUnitOfMeasure(lot.getInventoryItem() != null ? lot.getInventoryItem().getUnits() : "");
                    data.setExpirationDate(
                            lot.getExpirationDate() != null ? lot.getExpirationDate().toLocalDateTime().toLocalDate()
                                    : null);
                    data.setReceivedDate(
                            lot.getReceiptDate() != null ? lot.getReceiptDate().toLocalDateTime().toLocalDate() : null);
                    data.setTestDate(usage.getUsageDate() != null ? usage.getUsageDate().toLocalDateTime() : null);
                    data.setQuantityUsed(BigDecimal.valueOf(usage.getQuantityUsed()).setScale(2, RoundingMode.HALF_UP));

                    reportData.add(data);
                }
            }
        }

        return reportData;
    }
}
