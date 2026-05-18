package org.openelisglobal.environmentalmonitoring.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.environmentalmonitoring.form.LabEnvironmentalLogForm;
import org.openelisglobal.environmentalmonitoring.service.LabEnvironmentalLogService;
import org.openelisglobal.environmentalmonitoring.valueholder.LabEnvironmentalLog;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageRoom;
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
 * temperature and humidity tracking across different storage units. Controllers
 * are singletons - NO class-level variables for thread safety.
 */
@RestController
@RequestMapping("/rest/environmental-monitoring")
public class LabEnvironmentalLogController extends BaseRestController {

    @Autowired
    private LabEnvironmentalLogService labEnvironmentalLogService;

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private FreezerService freezerService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Log a new environmental reading.
     *
     * @param form    The environmental log form data
     * @param request HTTP servlet request for user context
     * @return ResponseEntity with created log or error
     */
    @PostMapping("/log")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logEnvironmentalReading(@Valid @RequestBody LabEnvironmentalLogForm form,
            HttpServletRequest request) {
        try {
            // Get user ID from session (NO @Transactional in controller)
            String userId = getSysUserId(request);

            // Convert form to entity
            LabEnvironmentalLog log = convertFormToEntity(form);
            if (!canAccessEnvironmentalLog(log, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Not authorized for this storage unit"));
            }

            // Service handles all business logic and transaction management
            LabEnvironmentalLog savedLog = labEnvironmentalLogService.logEnvironmentalReading(log, userId);

            return ResponseEntity.ok(Map.of("success", true, "message", "Environmental log saved successfully", "logId",
                    savedLog.getId()));

        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "logEnvironmentalReading",
                    "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Validation error: " + e.getMessage()));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "logEnvironmentalReading",
                    "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to save environmental log"));
        }
    }

    /**
     * Get environmental logs by storage unit type.
     *
     * @param storageUnitType The storage unit type filter
     * @param limit           Maximum number of results (default 100)
     * @param offset          Starting offset for pagination (default 0)
     * @return ResponseEntity with logs list
     */
    @GetMapping("/logs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEnvironmentalLogs(
            @RequestParam(required = false) String storageUnitType, @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset, HttpServletRequest request) {
        try {
            List<LabEnvironmentalLog> logs;

            if (storageUnitType != null && !storageUnitType.trim().isEmpty()) {
                LabEnvironmentalLog.StorageUnitType type = LabEnvironmentalLog.StorageUnitType
                        .valueOf(storageUnitType.toUpperCase());
                logs = labEnvironmentalLogService.getLogsByStorageUnitType(type, Integer.MAX_VALUE, 0);
            } else {
                logs = labEnvironmentalLogService.getRecentLogs(Integer.MAX_VALUE);
            }
            logs = paginate(filterAccessibleLogs(logs, request), limit, offset);

            return ResponseEntity.ok(Map.of("success", true, "logs", logs, "count", logs.size()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid storage unit type: " + storageUnitType));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getEnvironmentalLogs",
                    "Error retrieving logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to retrieve environmental logs"));
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
            @RequestParam(required = false) String storageUnitType, HttpServletRequest request) {
        try {
            LabEnvironmentalLog.StorageUnitType type = null;
            if (storageUnitType != null && !storageUnitType.trim().isEmpty()) {
                type = LabEnvironmentalLog.StorageUnitType.valueOf(storageUnitType.toUpperCase());
            }
            Map<String, Object> stats = buildAccessibleDashboardStatistics(type, request);
            stats.put("success", true);
            return ResponseEntity.ok(stats);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid storage unit type: " + storageUnitType));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getDashboardStatistics",
                    "Error compiling dashboard statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to retrieve dashboard statistics"));
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
            Map<String, Object> ranges = Map.of("success", true, "ranges", Map.of("ROOM",
                    labEnvironmentalLogService.getTemperatureRange(LabEnvironmentalLog.StorageUnitType.ROOM, "C"),
                    "FREEZER",
                    labEnvironmentalLogService.getTemperatureRange(LabEnvironmentalLog.StorageUnitType.FREEZER, "C"),
                    "EQUIPMENT_ANALYZER",
                    labEnvironmentalLogService
                            .getTemperatureRange(LabEnvironmentalLog.StorageUnitType.EQUIPMENT_ANALYZER, "C"),
                    "MOVABLE_FRIDGE", labEnvironmentalLogService
                            .getTemperatureRange(LabEnvironmentalLog.StorageUnitType.MOVABLE_FRIDGE, "C")));

            return ResponseEntity.ok(ranges);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getTemperatureRanges",
                    "Error retrieving temperature ranges: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to retrieve temperature ranges"));
        }
    }

    /**
     * Search environmental logs with multiple criteria.
     *
     * @param storageUnitType Optional storage unit type filter
     * @param storageUnitId   Optional storage unit ID filter
     * @param startDate       Optional start date filter (yyyy-MM-dd HH:mm:ss)
     * @param endDate         Optional end date filter (yyyy-MM-dd HH:mm:ss)
     * @param checkedBy       Optional checked by user filter
     * @param limit           Maximum number of results (default 50)
     * @param offset          Starting offset for pagination (default 0)
     * @return ResponseEntity with filtered logs
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchEnvironmentalLogs(
            @RequestParam(required = false) String storageUnitType,
            @RequestParam(required = false) String storageUnitId, @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(required = false) String checkedBy,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {
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
            List<LabEnvironmentalLog> logs = labEnvironmentalLogService.searchLogs(type, storageUnitId, startTimestamp,
                    endTimestamp, checkedBy, Integer.MAX_VALUE, 0);
            logs = paginate(filterAccessibleLogs(logs, request), limit, offset);

            return ResponseEntity.ok(Map.of("success", true, "logs", logs, "count", logs.size()));

        } catch (ParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid date format. Use yyyy-MM-dd HH:mm:ss"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid parameter: " + e.getMessage()));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "searchEnvironmentalLogs",
                    "Error searching logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to search environmental logs"));
        }
    }

    private List<LabEnvironmentalLog> filterAccessibleLogs(List<LabEnvironmentalLog> logs, HttpServletRequest request) {
        return logs.stream().filter(log -> canAccessEnvironmentalLog(log, request)).collect(Collectors.toList());
    }

    private List<LabEnvironmentalLog> paginate(List<LabEnvironmentalLog> logs, int limit, int offset) {
        int start = Math.min(offset, logs.size());
        int end = Math.min(start + limit, logs.size());
        return logs.subList(start, end);
    }

    private Map<String, Object> buildAccessibleDashboardStatistics(LabEnvironmentalLog.StorageUnitType filterType,
            HttpServletRequest request) {
        List<LabEnvironmentalLog> logs = labEnvironmentalLogService.getRecentLogs(Integer.MAX_VALUE).stream()
                .filter(log -> filterType == null || log.getStorageUnitType() == filterType)
                .filter(log -> canAccessEnvironmentalLog(log, request)).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        long totalLogs = logs.size();
        long todaysLogs = logs.stream().filter(this::isToday).count();
        long outOfRangeLogs = logs.stream()
                .filter(log -> !labEnvironmentalLogService.isTemperatureInRange(log.getStorageUnitType(),
                        log.getTemperatureValue(), log.getTemperatureUnit()))
                .count();

        for (LabEnvironmentalLog.StorageUnitType type : LabEnvironmentalLog.StorageUnitType.values()) {
            long typeCount = logs.stream().filter(log -> log.getStorageUnitType() == type).count();
            long typeTodayCount = logs.stream().filter(log -> log.getStorageUnitType() == type).filter(this::isToday)
                    .count();
            stats.put(type.name().toLowerCase() + "Count", typeCount);
            stats.put(type.name().toLowerCase() + "TodayCount", typeTodayCount);
        }

        stats.put("totalLogs", totalLogs);
        stats.put("todaysLogs", todaysLogs);
        stats.put("outOfRangeLogs", outOfRangeLogs);
        if (filterType != null) {
            stats.put("storageUnitType", filterType);
        }
        return stats;
    }

    private boolean isToday(LabEnvironmentalLog log) {
        return log.getCheckedDateTime() != null
                && log.getCheckedDateTime().toLocalDateTime().toLocalDate().equals(LocalDate.now());
    }

    private boolean canAccessEnvironmentalLog(LabEnvironmentalLog log, HttpServletRequest request) {
        if (log == null || log.getStorageUnitType() == null) {
            return false;
        }
        if (log.getStorageUnitType() == LabEnvironmentalLog.StorageUnitType.ROOM) {
            return canAccessRoomId(log.getStorageUnitId(), request);
        }
        if (log.getStorageUnitType() == LabEnvironmentalLog.StorageUnitType.FREEZER) {
            return canAccessFreezerId(log.getStorageUnitId(), request);
        }
        return true;
    }

    private boolean canAccessRoomId(String roomId, HttpServletRequest request) {
        try {
            StorageRoom room = storageLocationService.getRoom(Integer.parseInt(roomId));
            return room != null && departmentIsolationService.canAccessStorageRoom(room, request);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canAccessFreezerId(String freezerId, HttpServletRequest request) {
        try {
            Freezer freezer = freezerService.findById(Long.parseLong(freezerId)).orElse(null);
            StorageRoom room = freezer != null ? freezer.getStorageRoom() : null;
            return room != null && departmentIsolationService.canAccessStorageRoom(room, request);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert form DTO to entity. Helper method to transform client data to domain
     * entity.
     */
    private LabEnvironmentalLog convertFormToEntity(LabEnvironmentalLogForm form) {
        LabEnvironmentalLog log = new LabEnvironmentalLog();

        log.setStorageUnitType(LabEnvironmentalLog.StorageUnitType.valueOf(form.getStorageUnitType().toUpperCase()));
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
     * Get all storage unit types for dropdown. Similar to inventory item types
     * endpoint.
     *
     * @return ResponseEntity with list of storage unit type strings
     */
    @GetMapping("/storage-unit-types")
    @ResponseBody
    public ResponseEntity<List<String>> getStorageUnitTypes() {
        try {
            List<String> types = Arrays.stream(LabEnvironmentalLog.StorageUnitType.values()).map(Enum::name)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getStorageUnitTypes",
                    "Error retrieving storage unit types: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
