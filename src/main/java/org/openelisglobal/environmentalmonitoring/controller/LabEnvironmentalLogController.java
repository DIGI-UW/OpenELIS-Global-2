package org.openelisglobal.environmentalmonitoring.controller;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.environmentalmonitoring.form.LabEnvironmentalLogForm;
import org.openelisglobal.environmentalmonitoring.service.LabEnvironmentalLogService;
import org.openelisglobal.environmentalmonitoring.valueholder.LabEnvironmentalLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Lab Environmental Monitoring.
 * <p>
 * Handles HTTP requests for lab-wide environmental logging including
 * temperature and humidity tracking across different storage units.
 * Controllers are singletons - NO class-level variables for thread safety.
 */
@RestController
@RequestMapping("/rest/environmental-monitoring")
public class LabEnvironmentalLogController extends BaseRestController {

    @Autowired
    private LabEnvironmentalLogService labEnvironmentalLogService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Log a new environmental reading.
     *
     * @param form The environmental log form data
     * @param request HTTP servlet request for user context
     * @return ResponseEntity with created log or error
     */
    @PostMapping("/log")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logEnvironmentalReading(
            @Valid @RequestBody LabEnvironmentalLogForm form,
            HttpServletRequest request) {
        try {
            // Get user ID from session (NO @Transactional in controller)
            String userId = getSysUserId(request);

            // Convert form to entity
            LabEnvironmentalLog log = convertFormToEntity(form);

            // Service handles all business logic and transaction management
            LabEnvironmentalLog savedLog = labEnvironmentalLogService.logEnvironmentalReading(log, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Environmental log saved successfully",
                "logId", savedLog.getId()
            ));

        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "logEnvironmentalReading",
                    "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Validation error: " + e.getMessage()
            ));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "logEnvironmentalReading",
                    "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to save environmental log"
            ));
        }
    }

    /**
     * Get environmental logs by storage unit type.
     *
     * @param storageUnitType The storage unit type filter
     * @param limit Maximum number of results (default 100)
     * @param offset Starting offset for pagination (default 0)
     * @return ResponseEntity with logs list
     */
    @GetMapping("/logs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEnvironmentalLogs(
            @RequestParam(required = false) String storageUnitType,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            List<LabEnvironmentalLog> logs;

            if (storageUnitType != null && !storageUnitType.trim().isEmpty()) {
                LabEnvironmentalLog.StorageUnitType type =
                    LabEnvironmentalLog.StorageUnitType.valueOf(storageUnitType.toUpperCase());
                logs = labEnvironmentalLogService.getLogsByStorageUnitType(type, limit, offset);
            } else {
                logs = labEnvironmentalLogService.getRecentLogs(limit);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "logs", logs,
                "count", logs.size()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid storage unit type: " + storageUnitType
            ));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getEnvironmentalLogs",
                    "Error retrieving logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to retrieve environmental logs"
            ));
        }
    }

    /**
     * Get dashboard statistics for environmental monitoring.
     *
     * @param storageUnitType Optional storage unit type filter
     * @return ResponseEntity with dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStatistics(
            @RequestParam(required = false) String storageUnitType) {
        try {
            Map<String, Object> stats;

            if (storageUnitType != null && !storageUnitType.trim().isEmpty()) {
                LabEnvironmentalLog.StorageUnitType type =
                    LabEnvironmentalLog.StorageUnitType.valueOf(storageUnitType.toUpperCase());
                stats = labEnvironmentalLogService.getDashboardStatisticsForType(type);
            } else {
                stats = labEnvironmentalLogService.getDashboardStatistics();
            }

            stats.put("success", true);
            return ResponseEntity.ok(stats);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid storage unit type: " + storageUnitType
            ));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getDashboardStatistics",
                    "Error compiling dashboard statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to retrieve dashboard statistics"
            ));
        }
    }

    /**
     * Get temperature ranges for storage unit types.
     *
     * @return ResponseEntity with temperature ranges for all storage unit types
     */
    @GetMapping("/temperature-ranges")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTemperatureRanges() {
        try {
            Map<String, Object> ranges = Map.of(
                "success", true,
                "ranges", Map.of(
                    "ROOM", labEnvironmentalLogService.getTemperatureRange(
                        LabEnvironmentalLog.StorageUnitType.ROOM, "C"),
                    "FREEZER", labEnvironmentalLogService.getTemperatureRange(
                        LabEnvironmentalLog.StorageUnitType.FREEZER, "C"),
                    "EQUIPMENT_ANALYZER", labEnvironmentalLogService.getTemperatureRange(
                        LabEnvironmentalLog.StorageUnitType.EQUIPMENT_ANALYZER, "C"),
                    "MOVABLE_FRIDGE", labEnvironmentalLogService.getTemperatureRange(
                        LabEnvironmentalLog.StorageUnitType.MOVABLE_FRIDGE, "C")
                )
            );

            return ResponseEntity.ok(ranges);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getTemperatureRanges",
                    "Error retrieving temperature ranges: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to retrieve temperature ranges"
            ));
        }
    }

    /**
     * Search environmental logs with multiple criteria.
     *
     * @param storageUnitType Optional storage unit type filter
     * @param storageUnitId Optional storage unit ID filter
     * @param startDate Optional start date filter (yyyy-MM-dd HH:mm:ss)
     * @param endDate Optional end date filter (yyyy-MM-dd HH:mm:ss)
     * @param checkedBy Optional checked by user filter
     * @param limit Maximum number of results (default 50)
     * @param offset Starting offset for pagination (default 0)
     * @return ResponseEntity with filtered logs
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchEnvironmentalLogs(
            @RequestParam(required = false) String storageUnitType,
            @RequestParam(required = false) String storageUnitId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String checkedBy,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            // Parse parameters
            LabEnvironmentalLog.StorageUnitType type = null;
            if (storageUnitType != null && !storageUnitType.trim().isEmpty()) {
                type = LabEnvironmentalLog.StorageUnitType.valueOf(storageUnitType.toUpperCase());
            }

            Timestamp startTimestamp = null;
            Timestamp endTimestamp = null;
            if (startDate != null && !startDate.trim().isEmpty()) {
                startTimestamp = new Timestamp(DATE_FORMAT.parse(startDate).getTime());
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                endTimestamp = new Timestamp(DATE_FORMAT.parse(endDate).getTime());
            }

            // Service handles all search logic
            List<LabEnvironmentalLog> logs = labEnvironmentalLogService.searchLogs(
                type, storageUnitId, startTimestamp, endTimestamp, checkedBy, limit, offset);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "logs", logs,
                "count", logs.size()
            ));

        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid date format. Use yyyy-MM-dd HH:mm:ss"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid parameter: " + e.getMessage()
            ));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "searchEnvironmentalLogs",
                    "Error searching logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to search environmental logs"
            ));
        }
    }

    /**
     * Convert form DTO to entity.
     * Helper method to transform client data to domain entity.
     */
    private LabEnvironmentalLog convertFormToEntity(LabEnvironmentalLogForm form) {
        LabEnvironmentalLog log = new LabEnvironmentalLog();

        log.setStorageUnitType(LabEnvironmentalLog.StorageUnitType.valueOf(
            form.getStorageUnitType().toUpperCase()));
        log.setStorageUnitId(form.getStorageUnitId());
        log.setIntervalType(form.getIntervalType());
        log.setTemperatureValue(form.getTemperatureValue());
        log.setTemperatureUnit(form.getTemperatureUnit());
        log.setHumidityValue(form.getHumidityValue());
        log.setCheckedBy(form.getCheckedBy());
        log.setNotes(form.getNotes());

        // Set checked date/time from form or current time
        if (form.getCheckedDateTime() != null) {
            log.setCheckedDateTime(form.getCheckedDateTime());
        }

        // Movable Fridge specific fields
        log.setSampleType(form.getSampleType());
        log.setProjectName(form.getProjectName());
        log.setSampleId(form.getSampleId());
        log.setAdditionalDetails(form.getAdditionalDetails());

        return log;
    }

    /**
     * Get all storage unit types for dropdown.
     * Similar to inventory item types endpoint.
     *
     * @return ResponseEntity with list of storage unit type strings
     */
    @GetMapping("/storage-unit-types")
    @ResponseBody
    public ResponseEntity<List<String>> getStorageUnitTypes() {
        try {
            List<String> types = Arrays.stream(LabEnvironmentalLog.StorageUnitType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getStorageUnitTypes",
                    "Error retrieving storage unit types: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}