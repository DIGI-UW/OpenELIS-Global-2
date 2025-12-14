package org.openelisglobal.inventory.service;

import java.time.LocalDate;
import java.util.List;

import org.openelisglobal.inventory.valueholder.reports.ExpirationForecastReportData;
import org.openelisglobal.inventory.valueholder.reports.LotTraceabilityReportData;
import org.openelisglobal.inventory.valueholder.reports.LowStockReportData;
import org.openelisglobal.inventory.valueholder.reports.StockLevelReportData;
import org.openelisglobal.inventory.valueholder.reports.TransactionHistoryReportData;
import org.openelisglobal.inventory.valueholder.reports.UsageTrendReportData;

/**
 * Service interface for generating inventory report data
 * Provides data aggregation methods for all 6 report types
 *
 * Per Constitution Principle IV:
 * - Transaction management in service layer (@Transactional)
 * - Services compile all data within transaction (prevents LazyInitializationException)
 * - Returns complete DTOs with all relationships resolved
 */
public interface InventoryReportService {

    /**
     * Generate stock level report data
     * Shows current inventory status for all (or active) items
     *
     * @param includeInactive Include inactive items (default: false)
     * @param groupByType Organize by item type (default: false)
     * @param groupByLocation Organize by storage location (default: false)
     * @return List of stock level data, fully populated
     */
    List<StockLevelReportData> generateStockLevelData(
        boolean includeInactive,
        boolean groupByType,
        boolean groupByLocation
    );

    /**
     * Generate low stock alert report data
     * Shows only items below reorder threshold
     *
     * @param includeInactive Include inactive items (default: false)
     * @return List of low stock data, fully populated
     */
    List<LowStockReportData> generateLowStockData(boolean includeInactive);

    /**
     * Generate expiration forecast report data
     * Shows lots expiring within date range
     *
     * @param startDate Start of forecast window (required)
     * @param endDate End of forecast window (required)
     * @param includeExpired Include already-expired lots (default: true)
     * @return List of expiration data, fully populated
     */
    List<ExpirationForecastReportData> generateExpirationForecastData(
        LocalDate startDate,
        LocalDate endDate,
        boolean includeExpired
    );

    /**
     * Generate transaction history report data
     * Shows all inventory transactions within date range
     *
     * @param startDate Start of date range (required)
     * @param endDate End of date range (required)
     * @return List of transaction data, fully populated
     */
    List<TransactionHistoryReportData> generateTransactionHistoryData(
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Generate usage trends report data
     * Shows consumption patterns and analytics for date range
     *
     * @param startDate Start of analysis period (required)
     * @param endDate End of analysis period (required)
     * @return List of usage trend data, fully populated
     */
    List<UsageTrendReportData> generateUsageTrendData(
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Generate lot traceability report data
     * Links specific lots to test results and patient identifiers
     *
     * @param lotNumber Specific lot number to trace (optional - if null, returns all lots)
     * @return List of lot traceability data, fully populated
     */
    List<LotTraceabilityReportData> generateLotTraceabilityData(String lotNumber);
}
