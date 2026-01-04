package org.openelisglobal.environmentalmonitoring.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.environmentalmonitoring.valueholder.LabEnvironmentalLog;

/**
 * DAO interface for LabEnvironmentalLog entities.
 *
 * Provides data access methods for lab-wide environmental monitoring logs
 * including temperature and humidity tracking across different storage unit types.
 */
public interface LabEnvironmentalLogDAO extends BaseDAO<LabEnvironmentalLog, Long> {

    /**
     * Find all environmental logs by storage unit type.
     *
     * @param storageUnitType The type of storage unit (ROOM, FREEZER, etc.)
     * @return List of logs for the specified storage unit type, ordered by checked date/time descending
     */
    List<LabEnvironmentalLog> findByStorageUnitType(LabEnvironmentalLog.StorageUnitType storageUnitType);

    /**
     * Find all environmental logs by storage unit ID.
     *
     * @param storageUnitId The specific storage unit identifier
     * @return List of logs for the specified storage unit, ordered by checked date/time descending
     */
    List<LabEnvironmentalLog> findByStorageUnitId(String storageUnitId);

    /**
     * Find environmental logs by storage unit type and date range.
     *
     * @param storageUnitType The type of storage unit
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @return List of logs within the date range, ordered by checked date/time descending
     */
    List<LabEnvironmentalLog> findByStorageUnitTypeAndDateRange(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        Timestamp startDate,
        Timestamp endDate
    );

    /**
     * Find environmental logs by storage unit ID and date range.
     *
     * @param storageUnitId The specific storage unit identifier
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @return List of logs within the date range, ordered by checked date/time descending
     */
    List<LabEnvironmentalLog> findByStorageUnitIdAndDateRange(
        String storageUnitId,
        Timestamp startDate,
        Timestamp endDate
    );

    /**
     * Count environmental logs by storage unit type.
     *
     * @param storageUnitType The type of storage unit
     * @return Total count of logs for the storage unit type
     */
    long countByStorageUnitType(LabEnvironmentalLog.StorageUnitType storageUnitType);

    /**
     * Count environmental logs by storage unit type for today.
     *
     * @param storageUnitType The type of storage unit
     * @return Count of logs created today for the storage unit type
     */
    long countTodayByStorageUnitType(LabEnvironmentalLog.StorageUnitType storageUnitType);

    /**
     * Find environmental logs with temperature values outside specified range.
     *
     * @param storageUnitType The type of storage unit
     * @param minTemp Minimum acceptable temperature
     * @param maxTemp Maximum acceptable temperature
     * @return List of logs with out-of-range temperatures, ordered by checked date/time descending
     */
    List<LabEnvironmentalLog> findOutOfRangeTemperature(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        Double minTemp,
        Double maxTemp
    );

    /**
     * Count environmental logs with temperature values outside specified range.
     *
     * @param storageUnitType The type of storage unit
     * @param minTemp Minimum acceptable temperature
     * @param maxTemp Maximum acceptable temperature
     * @return Count of logs with out-of-range temperatures
     */
    long countOutOfRangeTemperature(
        LabEnvironmentalLog.StorageUnitType storageUnitType,
        Double minTemp,
        Double maxTemp
    );

    /**
     * Find all environmental logs ordered by checked date/time descending.
     * Used for lab-wide overview dashboard.
     *
     * @return All environmental logs ordered by most recent first
     */
    List<LabEnvironmentalLog> findAllOrderedByDate();

    /**
     * Find environmental logs for today across all storage units.
     *
     * @return List of today's logs ordered by checked date/time descending
     */
    List<LabEnvironmentalLog> findTodaysLogs();
}