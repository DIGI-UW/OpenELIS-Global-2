package org.openelisglobal.environmentalmonitoring.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.environmentalmonitoring.valueholder.LabEnvironmentalLog;

/**
 * Service interface for LabEnvironmentalLog business operations.
 *
 * Handles business logic for lab-wide environmental monitoring including
 * temperature and humidity tracking, validation, and dashboard data compilation.
 */
public interface LabEnvironmentalLogService extends BaseObjectService<LabEnvironmentalLog, Long> {

    /**
     * Log a new environmental reading with validation.
     *
     * @param log The environmental log to save
     * @param loggedByUserId The ID of the user creating the log
     * @return The saved environmental log
     * @throws IllegalArgumentException if validation fails
     */
    LabEnvironmentalLog logEnvironmentalReading(LabEnvironmentalLog log, String loggedByUserId);

    /**
     * Get environmental logs by storage unit type with pagination.
     *
     * @param storageUnitType The type of storage unit
     * @param limit Maximum number of results (default 100)
     * @param offset Starting offset for pagination (default 0)
     * @return List of environmental logs ordered by date descending
     */
    List<LabEnvironmentalLog> getLogsByStorageUnitType(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        int limit,
        int offset
    );

    /**
     * Get environmental logs by specific storage unit ID.
     *
     * @param storageUnitId The storage unit identifier
     * @param limit Maximum number of results
     * @param offset Starting offset for pagination
     * @return List of environmental logs for the unit ordered by date descending
     */
    List<LabEnvironmentalLog> getLogsByStorageUnitId(String storageUnitId, int limit, int offset);

    /**
     * Get environmental logs within date range.
     *
     * @param storageUnitType Optional storage unit type filter
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @return List of logs within the date range
     */
    List<LabEnvironmentalLog> getLogsInDateRange(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        Timestamp startDate,
        Timestamp endDate
    );

    /**
     * Get dashboard statistics for environmental monitoring.
     * Compiles total counts, today's counts, and out-of-range counts.
     *
     * @return Map with dashboard statistics (totalLogs, todaysLogs, outOfRangeLogs, etc.)
     */
    Map<String, Object> getDashboardStatistics();

    /**
     * Get dashboard statistics for a specific storage unit type.
     *
     * @param storageUnitType The storage unit type to analyze
     * @return Map with statistics for the specific storage unit type
     */
    Map<String, Object> getDashboardStatisticsForType(LabEnvironmentalLog.StorageUnitType storageUnitType);

    /**
     * Validate temperature reading against acceptable ranges for storage unit type.
     *
     * @param storageUnitType The type of storage unit
     * @param temperatureValue The temperature reading
     * @param temperatureUnit The temperature unit (C or F)
     * @return True if temperature is within acceptable range, false otherwise
     */
    boolean isTemperatureInRange(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        Double temperatureValue,
        String temperatureUnit
    );

    /**
     * Get acceptable temperature range for a storage unit type.
     *
     * @param storageUnitType The storage unit type
     * @param temperatureUnit The temperature unit (C or F)
     * @return Map with "min" and "max" temperature values
     */
    Map<String, Double> getTemperatureRange(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        String temperatureUnit
    );

    /**
     * Get logs with out-of-range temperature readings.
     *
     * @param storageUnitType The storage unit type to check
     * @param limit Maximum number of results
     * @return List of logs with temperature outside acceptable range
     */
    List<LabEnvironmentalLog> getOutOfRangeLogs(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        int limit
    );

    /**
     * Get recent logs for all storage units (lab-wide overview).
     *
     * @param limit Maximum number of results (default 50)
     * @return List of recent logs across all storage units
     */
    List<LabEnvironmentalLog> getRecentLogs(int limit);

    /**
     * Get today's logs across all storage units.
     *
     * @return List of today's environmental logs
     */
    List<LabEnvironmentalLog> getTodaysLogs();

    /**
     * Search environmental logs by multiple criteria.
     *
     * @param storageUnitType Optional storage unit type filter
     * @param storageUnitId Optional storage unit ID filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param checkedBy Optional checked by user filter
     * @param limit Maximum number of results
     * @param offset Starting offset for pagination
     * @return List of logs matching the search criteria
     */
    List<LabEnvironmentalLog> searchLogs(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        String storageUnitId,
        Timestamp startDate,
        Timestamp endDate,
        String checkedBy,
        int limit,
        int offset
    );
}