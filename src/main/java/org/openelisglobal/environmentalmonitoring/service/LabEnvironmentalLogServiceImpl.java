package org.openelisglobal.environmentalmonitoring.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.environmentalmonitoring.dao.LabEnvironmentalLogDAO;
import org.openelisglobal.environmentalmonitoring.valueholder.LabEnvironmentalLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for LabEnvironmentalLog business operations.
 *
 * Handles validation, business logic, and data compilation for environmental monitoring.
 * All transactions start here (NOT in controllers).
 */
@Service
@Transactional
public class LabEnvironmentalLogServiceImpl extends AuditableBaseObjectServiceImpl<LabEnvironmentalLog, Long>
        implements LabEnvironmentalLogService {

    @Autowired
    private LabEnvironmentalLogDAO labEnvironmentalLogDAO;

    public LabEnvironmentalLogServiceImpl() {
        super(LabEnvironmentalLog.class);
    }

    @Override
    protected LabEnvironmentalLogDAO getBaseObjectDAO() {
        return labEnvironmentalLogDAO;
    }

    @Override
    @Transactional
    public LabEnvironmentalLog logEnvironmentalReading(LabEnvironmentalLog log, String loggedByUserId) {
        try {
            // Set system fields
            log.setLoggedBy(loggedByUserId);
            if (log.getLoggedAt() == null) {
                log.setLoggedAt(new Timestamp(System.currentTimeMillis()));
            }
            if (log.getCheckedDateTime() == null) {
                log.setCheckedDateTime(new Timestamp(System.currentTimeMillis()));
            }

            // Validate required fields
            if (log.getStorageUnitType() == null) {
                throw new IllegalArgumentException("Storage unit type is required");
            }
            if (log.getStorageUnitId() == null || log.getStorageUnitId().trim().isEmpty()) {
                throw new IllegalArgumentException("Storage unit ID is required");
            }
            if (log.getTemperatureValue() == null) {
                throw new IllegalArgumentException("Temperature value is required");
            }

            // Validate Movable Fridge specific fields
            if (log.getStorageUnitType() == LabEnvironmentalLog.StorageUnitType.MOVABLE_FRIDGE) {
                if (log.getSampleType() == null || log.getSampleType().trim().isEmpty()) {
                    throw new IllegalArgumentException("Sample type is required for Movable Fridge");
                }
                if (log.getProjectName() == null || log.getProjectName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Project name is required for Movable Fridge");
                }
                if (log.getSampleId() == null || log.getSampleId().trim().isEmpty()) {
                    throw new IllegalArgumentException("Sample ID is required for Movable Fridge");
                }
            }

            // Set default temperature unit if not provided
            if (log.getTemperatureUnit() == null || log.getTemperatureUnit().trim().isEmpty()) {
                log.setTemperatureUnit("C"); // Default to Celsius
            }

            // Save the log
            LabEnvironmentalLog savedLog = save(log);

            LogEvent.logInfo(this.getClass().getSimpleName(), "logEnvironmentalReading",
                    "Environmental log created for " + log.getStorageUnitType() +
                    " unit: " + log.getStorageUnitId());

            return savedLog;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "logEnvironmentalReading",
                    "Error logging environmental reading: " + e.getMessage());
            throw new RuntimeException("Failed to log environmental reading", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> getLogsByStorageUnitType(
            LabEnvironmentalLog.StorageUnitType storageUnitType,
            int limit,
            int offset) {
        try {
            List<LabEnvironmentalLog> logs = labEnvironmentalLogDAO.findByStorageUnitType(storageUnitType);

            // Apply pagination
            int start = Math.min(offset, logs.size());
            int end = Math.min(start + limit, logs.size());
            return logs.subList(start, end);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getLogsByStorageUnitType",
                    "Error retrieving logs by storage unit type: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve logs by storage unit type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> getLogsByStorageUnitId(String storageUnitId, int limit, int offset) {
        try {
            List<LabEnvironmentalLog> logs = labEnvironmentalLogDAO.findByStorageUnitId(storageUnitId);

            // Apply pagination
            int start = Math.min(offset, logs.size());
            int end = Math.min(start + limit, logs.size());
            return logs.subList(start, end);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getLogsByStorageUnitId",
                    "Error retrieving logs by storage unit ID: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve logs by storage unit ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> getLogsInDateRange(
            LabEnvironmentalLog.StorageUnitType storageUnitType,
            Timestamp startDate,
            Timestamp endDate) {
        try {
            if (storageUnitType != null) {
                return labEnvironmentalLogDAO.findByStorageUnitTypeAndDateRange(storageUnitType, startDate, endDate);
            } else {
                // Return logs for all storage unit types within date range
                return labEnvironmentalLogDAO.findAllOrderedByDate().stream()
                    .filter(log -> !log.getCheckedDateTime().before(startDate) &&
                                  !log.getCheckedDateTime().after(endDate))
                    .toList();
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getLogsInDateRange",
                    "Error retrieving logs in date range: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve logs in date range", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Get counts for each storage unit type
            long totalLogs = 0;
            long todaysLogs = 0;
            long outOfRangeLogs = 0;

            for (LabEnvironmentalLog.StorageUnitType type : LabEnvironmentalLog.StorageUnitType.values()) {
                long typeCount = labEnvironmentalLogDAO.countByStorageUnitType(type);
                long typeTodayCount = labEnvironmentalLogDAO.countTodayByStorageUnitType(type);

                totalLogs += typeCount;
                todaysLogs += typeTodayCount;

                // Calculate out-of-range for each type
                Map<String, Double> tempRange = getTemperatureRange(type, "C");
                if (tempRange.get("min") != null && tempRange.get("max") != null) {
                    long typeOutOfRange = labEnvironmentalLogDAO.countOutOfRangeTemperature(
                        type, tempRange.get("min"), tempRange.get("max"));
                    outOfRangeLogs += typeOutOfRange;
                }

                // Store per-type statistics
                stats.put(type.name().toLowerCase() + "Count", typeCount);
                stats.put(type.name().toLowerCase() + "TodayCount", typeTodayCount);
            }

            stats.put("totalLogs", totalLogs);
            stats.put("todaysLogs", todaysLogs);
            stats.put("outOfRangeLogs", outOfRangeLogs);

            return stats;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getDashboardStatistics",
                    "Error compiling dashboard statistics: " + e.getMessage());
            throw new RuntimeException("Failed to compile dashboard statistics", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatisticsForType(LabEnvironmentalLog.StorageUnitType storageUnitType) {
        try {
            Map<String, Object> stats = new HashMap<>();

            long totalCount = labEnvironmentalLogDAO.countByStorageUnitType(storageUnitType);
            long todayCount = labEnvironmentalLogDAO.countTodayByStorageUnitType(storageUnitType);

            stats.put("totalLogs", totalCount);
            stats.put("todaysLogs", todayCount);
            stats.put("storageUnitType", storageUnitType);

            // Calculate out-of-range count
            Map<String, Double> tempRange = getTemperatureRange(storageUnitType, "C");
            if (tempRange.get("min") != null && tempRange.get("max") != null) {
                long outOfRange = labEnvironmentalLogDAO.countOutOfRangeTemperature(
                    storageUnitType, tempRange.get("min"), tempRange.get("max"));
                stats.put("outOfRangeLogs", outOfRange);
            } else {
                stats.put("outOfRangeLogs", 0L);
            }

            return stats;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getDashboardStatisticsForType",
                    "Error compiling statistics for storage unit type: " + e.getMessage());
            throw new RuntimeException("Failed to compile statistics for storage unit type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTemperatureInRange(
            LabEnvironmentalLog.StorageUnitType storageUnitType,
            Double temperatureValue,
            String temperatureUnit) {
        try {
            Map<String, Double> range = getTemperatureRange(storageUnitType, temperatureUnit);
            if (range.get("min") == null || range.get("max") == null) {
                return true; // No range defined, assume valid
            }
            return temperatureValue >= range.get("min") && temperatureValue <= range.get("max");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "isTemperatureInRange",
                    "Error validating temperature range: " + e.getMessage());
            return true; // Default to valid if error occurs
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getTemperatureRange(
            LabEnvironmentalLog.StorageUnitType storageUnitType,
            String temperatureUnit) {
        Map<String, Double> range = new HashMap<>();

        // Temperature ranges in Celsius (converted to Fahrenheit if needed)
        double minC, maxC;

        switch (storageUnitType) {
            case FREEZER:
                minC = -25.0;
                maxC = -15.0;
                break;
            case ROOM:
                minC = 15.0;
                maxC = 25.0;
                break;
            case EQUIPMENT_ANALYZER:
                minC = 2.0;
                maxC = 8.0;
                break;
            case MOVABLE_FRIDGE:
                minC = 2.0;
                maxC = 8.0;
                break;
            default:
                range.put("min", null);
                range.put("max", null);
                return range;
        }

        // Convert to Fahrenheit if needed
        if ("F".equalsIgnoreCase(temperatureUnit)) {
            double minF = (minC * 9.0 / 5.0) + 32.0;
            double maxF = (maxC * 9.0 / 5.0) + 32.0;
            range.put("min", minF);
            range.put("max", maxF);
        } else {
            range.put("min", minC);
            range.put("max", maxC);
        }

        return range;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> getOutOfRangeLogs(
            LabEnvironmentalLog.StorageUnitType storageUnitType,
            int limit) {
        try {
            Map<String, Double> tempRange = getTemperatureRange(storageUnitType, "C");
            if (tempRange.get("min") == null || tempRange.get("max") == null) {
                return List.of(); // No range defined
            }

            List<LabEnvironmentalLog> logs = labEnvironmentalLogDAO.findOutOfRangeTemperature(
                storageUnitType, tempRange.get("min"), tempRange.get("max"));

            // Apply limit
            return logs.subList(0, Math.min(limit, logs.size()));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getOutOfRangeLogs",
                    "Error retrieving out-of-range logs: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve out-of-range logs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> getRecentLogs(int limit) {
        try {
            List<LabEnvironmentalLog> logs = labEnvironmentalLogDAO.findAllOrderedByDate();
            return logs.subList(0, Math.min(limit, logs.size()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getRecentLogs",
                    "Error retrieving recent logs: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve recent logs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> getTodaysLogs() {
        try {
            return labEnvironmentalLogDAO.findTodaysLogs();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getTodaysLogs",
                    "Error retrieving today's logs: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve today's logs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabEnvironmentalLog> searchLogs(
            LabEnvironmentalLog.StorageUnitType storageUnitType,
            String storageUnitId,
            Timestamp startDate,
            Timestamp endDate,
            String checkedBy,
            int limit,
            int offset) {
        try {
            // For now, use basic filtering. In a production environment,
            // this would be implemented as a proper search query in the DAO.
            List<LabEnvironmentalLog> logs = labEnvironmentalLogDAO.findAllOrderedByDate();

            // Apply filters
            if (storageUnitType != null) {
                logs = logs.stream()
                    .filter(log -> log.getStorageUnitType() == storageUnitType)
                    .toList();
            }

            if (storageUnitId != null && !storageUnitId.trim().isEmpty()) {
                logs = logs.stream()
                    .filter(log -> log.getStorageUnitId().contains(storageUnitId))
                    .toList();
            }

            if (startDate != null && endDate != null) {
                logs = logs.stream()
                    .filter(log -> !log.getCheckedDateTime().before(startDate) &&
                                  !log.getCheckedDateTime().after(endDate))
                    .toList();
            }

            if (checkedBy != null && !checkedBy.trim().isEmpty()) {
                logs = logs.stream()
                    .filter(log -> log.getCheckedBy() != null &&
                                  log.getCheckedBy().toLowerCase().contains(checkedBy.toLowerCase()))
                    .toList();
            }

            // Apply pagination
            int start = Math.min(offset, logs.size());
            int end = Math.min(start + limit, logs.size());
            return logs.subList(start, end);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "searchLogs",
                    "Error searching logs: " + e.getMessage());
            throw new RuntimeException("Failed to search logs", e);
        }
    }
}