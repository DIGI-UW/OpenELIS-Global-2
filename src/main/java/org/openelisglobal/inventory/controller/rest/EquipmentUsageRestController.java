package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EquipmentUsageRestController
 *
 * REST endpoints for equipment usage tracking without reducing inventory
 * quantities. Provides endpoints for recording equipment usage, retrieving
 * usage history, and viewing usage metrics and analytics.
 */
@RestController
@RequestMapping("/rest/equipment/usage")
public class EquipmentUsageRestController extends BaseRestController {

    @Autowired
    private InventoryUsageService usageService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private TestSectionService testSectionService;

    /**
     * Record equipment usage without reducing inventory quantities. Request body
     * should contain: { itemId, lotId, quantity, labUnitId (optional) }
     *
     * @param request Equipment usage request
     * @return InventoryUsageDTO with enriched data
     */
    @PostMapping(value = "/record", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryUsageDTO> recordEquipmentUsage(@RequestBody EquipmentUsageRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Get current user from session
            UserSessionData userSession = (UserSessionData) httpRequest.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
            if (userSession == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String sysUserId = String.valueOf(userSession.getSystemUserId());

            // Record usage (without deducting quantity)
            InventoryUsage usage = usageService.recordEquipmentUsage(request.getLotId(), request.getItemId(),
                    request.getQuantity(), sysUserId, null);

            // Convert to enriched DTO
            InventoryUsageDTO dto = convertToDTO(usage);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Record complete equipment usage entry with all form fields (activities,
     * operator name, login/logout times, etc.). This endpoint captures the full
     * submission from the Usage Log form.
     *
     * @param request Equipment usage entry request with all form data
     * @return EquipmentUsageEntryDTO with enriched data for dashboard display
     */
    @PostMapping(value = "/submit", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipmentUsageEntryDTO> submitEquipmentUsageEntry(
            @RequestBody EquipmentUsageEntryRequest request, HttpServletRequest httpRequest) {
        try {
            // Get current user from session
            UserSessionData userSession = (UserSessionData) httpRequest.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
            if (userSession == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String sysUserId = String.valueOf(userSession.getSystemUserId());

            // Record usage (without deducting quantity)
            InventoryUsage usage = usageService.recordEquipmentUsage(request.getLotId(), request.getItemId(),
                    request.getQuantity(), sysUserId, null);

            // Populate form fields in usage record (persists to database)
            usage.setOperatorName(request.getOperatorName());
            usage.setLoginTime(request.getLoginTime());
            usage.setLogoutTime(request.getLogoutTime());
            usage.setActivities(request.getActivities());
            usage.setEquipmentStatus(request.getEquipmentStatus());
            usage.setApprovedBy(request.getApprovedBy());
            usage.setApprovalDate(request.getApprovalDate());
            usageService.save(usage);

            // Convert to extended DTO with all form fields
            EquipmentUsageEntryDTO dto = convertToEntryDTO(usage, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all equipment usage submissions with all form fields from the database.
     * Returns entries created via the /submit endpoint, persisted to database. Used
     * by dashboard to fetch submissions on page load (source of truth).
     *
     * @param startDate Optional start date (ISO 8601 format)
     * @param endDate   Optional end date (ISO 8601 format)
     * @return List of equipment usage entries with all form fields
     */
    @GetMapping(value = "/submissions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EquipmentUsageEntryDTO>> getEquipmentUsageSubmissions(
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        try {
            List<InventoryUsage> usageList;

            if (startDate != null && endDate != null) {
                Timestamp start = Timestamp.valueOf(startDate.replace("T", " "));
                Timestamp end = Timestamp.valueOf(endDate.replace("T", " "));
                usageList = usageService.getByDateRange(start, end);
            } else {
                usageList = usageService.getAll();
            }

            // Convert to extended DTO with form fields from database
            List<EquipmentUsageEntryDTO> dtoList = usageList.stream().map(usage -> {
                String userName = "Unknown User";
                if (usage.getPerformedByUser() != null) {
                    try {
                        SystemUser user = systemUserService.get(usage.getPerformedByUser().toString());
                        if (user != null) {
                            userName = user.getFirstName() + " " + user.getLastName();
                        }
                    } catch (Exception e) {
                        LogEvent.logError("Error retrieving user: " + usage.getPerformedByUser(), e);
                    }
                }

                return EquipmentUsageEntryDTO.builder().id(usage.getId())
                        .inventoryItemId(usage.getInventoryItem().getId())
                        .inventoryItemName(usage.getInventoryItem().getName()).lotId(usage.getLot().getId())
                        .lotNumber(usage.getLot().getLotNumber()).quantityUsed(usage.getQuantityUsed())
                        .usageDate(usage.getUsageDate()).performedByUserId(usage.getPerformedByUser())
                        .performedByUserName(userName).testResultId(usage.getTestResultId())
                        .analysisId(usage.getAnalysisId())
                        // Form fields from database
                        .operatorName(usage.getOperatorName())
                        .date(usage.getUsageDate() != null ? usage.getUsageDate().toString() : null)
                        .loginTime(usage.getLoginTime()).activities(usage.getActivities())
                        .equipmentStatus(usage.getEquipmentStatus()).logoutTime(usage.getLogoutTime())
                        .approvedBy(usage.getApprovedBy()).approvalDate(usage.getApprovalDate()).build();
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);

        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get equipment usage history for all items with optional date range filter.
     *
     * @param startDate Optional start date (ISO 8601 format)
     * @param endDate   Optional end date (ISO 8601 format)
     * @return List of enriched usage records for all items
     */
    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsageDTO>> getAllEquipmentUsageHistory(
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        try {
            List<InventoryUsage> usageList;

            if (startDate != null && endDate != null) {
                Timestamp start = Timestamp.valueOf(startDate.replace("T", " "));
                Timestamp end = Timestamp.valueOf(endDate.replace("T", " "));
                usageList = usageService.getByDateRange(start, end);
            } else {
                usageList = usageService.getAll();
            }

            List<InventoryUsageDTO> dtoList = usageList.stream().map(this::convertToDTO).collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);

        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get equipment usage history for a specific item with optional date range
     * filter.
     *
     * @param itemId    The equipment item ID
     * @param startDate Optional start date (ISO 8601 format)
     * @param endDate   Optional end date (ISO 8601 format)
     * @return List of enriched usage records
     */
    @GetMapping(value = "/item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsageDTO>> getEquipmentUsageHistory(@PathVariable Long itemId,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        try {
            List<InventoryUsage> usageList;

            if (startDate != null && endDate != null) {
                Timestamp start = Timestamp.valueOf(startDate.replace("T", " "));
                Timestamp end = Timestamp.valueOf(endDate.replace("T", " "));
                usageList = usageService.getByItemIdAndDateRange(itemId, start, end);
            } else {
                usageList = usageService.getByInventoryItemId(itemId);
            }

            List<InventoryUsageDTO> dtoList = usageList.stream().map(this::convertToDTO).collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);

        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get aggregated equipment usage metrics. Includes total equipment count, usage
     * statistics by equipment, and by lab unit.
     *
     * @param startDate Optional start date (ISO 8601 format)
     * @param endDate   Optional end date (ISO 8601 format)
     * @return Aggregated metrics DTO
     */
    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipmentUsageMetricsDTO> getEquipmentUsageMetrics(
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        try {
            List<InventoryUsage> usageList;

            if (startDate != null && endDate != null) {
                Timestamp start = Timestamp.valueOf(startDate.replace("T", " "));
                Timestamp end = Timestamp.valueOf(endDate.replace("T", " "));
                usageList = usageService.getByDateRange(start, end);
            } else {
                // Get all usage records if no date filter provided
                usageList = usageService.getAll();
            }

            // Get total equipment count (CARTRIDGE items)
            Integer totalEquipmentCount = inventoryItemService.getAllActive().stream()
                    .filter(item -> "CARTRIDGE".equals(item.getItemType().name())).map(item -> 1)
                    .reduce(0, Integer::sum);

            // Aggregate by equipment
            Map<Long, EquipmentUsageStat> equipmentStats = new HashMap<>();
            usageList.forEach(usage -> {
                Long itemId = usage.getInventoryItem().getId();
                EquipmentUsageStat stat = equipmentStats.computeIfAbsent(itemId,
                        k -> EquipmentUsageStat.builder().equipmentId(itemId)
                                .equipmentName(usage.getInventoryItem().getName()).usageCount(0).totalQuantityUsed(0.0)
                                .build());
                stat.setUsageCount(stat.getUsageCount() + 1);
                stat.setTotalQuantityUsed(stat.getTotalQuantityUsed() + usage.getQuantityUsed());
            });

            EquipmentUsageMetricsDTO metrics = EquipmentUsageMetricsDTO.builder()
                    .totalEquipmentCount(totalEquipmentCount).totalUsageRecords(usageList.size())
                    .usageByEquipment(new ArrayList<>(equipmentStats.values())).build();

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert InventoryUsage entity to enriched DTO with user and lab unit names.
     *
     * @param usage The InventoryUsage entity
     * @return Enriched DTO
     */
    private InventoryUsageDTO convertToDTO(InventoryUsage usage) {
        String userName = "Unknown User";
        if (usage.getPerformedByUser() != null) {
            try {
                SystemUser user = systemUserService.get(usage.getPerformedByUser().toString());
                if (user != null) {
                    userName = user.getFirstName() + " " + user.getLastName();
                }
            } catch (Exception e) {
                LogEvent.logError("Error retrieving user: " + usage.getPerformedByUser(), e);
            }
        }

        return InventoryUsageDTO.builder().id(usage.getId()).inventoryItemId(usage.getInventoryItem().getId())
                .inventoryItemName(usage.getInventoryItem().getName()).lotId(usage.getLot().getId())
                .lotNumber(usage.getLot().getLotNumber()).quantityUsed(usage.getQuantityUsed())
                .usageDate(usage.getUsageDate()).performedByUserId(usage.getPerformedByUser())
                .performedByUserName(userName).testResultId(usage.getTestResultId()).analysisId(usage.getAnalysisId())
                .build();
    }

    /**
     * Convert InventoryUsage entity and entry request to extended DTO with all form
     * fields.
     *
     * @param usage   The InventoryUsage entity
     * @param request The equipment usage entry request with form fields
     * @return Extended DTO with all fields for dashboard display
     */
    private EquipmentUsageEntryDTO convertToEntryDTO(InventoryUsage usage, EquipmentUsageEntryRequest request) {
        String userName = "Unknown User";
        if (usage.getPerformedByUser() != null) {
            try {
                SystemUser user = systemUserService.get(usage.getPerformedByUser().toString());
                if (user != null) {
                    userName = user.getFirstName() + " " + user.getLastName();
                }
            } catch (Exception e) {
                LogEvent.logError("Error retrieving user: " + usage.getPerformedByUser(), e);
            }
        }

        return EquipmentUsageEntryDTO.builder().id(usage.getId()).inventoryItemId(usage.getInventoryItem().getId())
                .inventoryItemName(usage.getInventoryItem().getName()).lotId(usage.getLot().getId())
                .lotNumber(usage.getLot().getLotNumber()).quantityUsed(usage.getQuantityUsed())
                .usageDate(usage.getUsageDate()).performedByUserId(usage.getPerformedByUser())
                .performedByUserName(userName).testResultId(usage.getTestResultId()).analysisId(usage.getAnalysisId())
                // Add form fields from request
                .operatorName(request.getOperatorName()).date(request.getDate()).loginTime(request.getLoginTime())
                .activities(request.getActivities()).equipmentStatus(request.getEquipmentStatus())
                .logoutTime(request.getLogoutTime()).approvedBy(request.getApprovedBy())
                .approvalDate(request.getApprovalDate()).build();
    }

    /**
     * Request body for recording complete equipment usage entry with all form
     * fields.
     */
    @Setter
    @Getter
    public static class EquipmentUsageEntryRequest {
        public Long itemId;
        public Long lotId;
        public Double quantity;
        public String labUnitId;
        public String operatorName;
        public String date;
        public String loginTime;
        public String activities;
        public String equipmentStatus;
        public String logoutTime;
        public String approvedBy;
        public String approvalDate;

    }

    /**
     * Request body for recording equipment usage.
     */
    @Setter
    @Getter
    public static class EquipmentUsageRequest {
        public Long itemId;
        public Long lotId;
        public Double quantity;
        public String labUnitId;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentUsageMetricsDTO {

        private Integer totalEquipmentCount;
        private Integer totalUsageRecords;
        private List<EquipmentUsageStat> usageByEquipment;
    }

    /**
     * Per-equipment usage statistics
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentUsageStat {
        private Long equipmentId;
        private String equipmentName;
        private Integer usageCount;
        private Double totalQuantityUsed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentUsageEntryDTO {

        // Base inventory usage fields
        private Long id;
        private Long inventoryItemId;
        private String inventoryItemName;
        private Long lotId;
        private String lotNumber;
        private Double quantityUsed;
        private Timestamp usageDate;

        // User information
        private Integer performedByUserId;
        private String performedByUserName;

        // Optional linked records
        private Long testResultId;
        private Long analysisId;

        // Equipment usage form fields
        private String operatorName;
        private String date;
        private String loginTime;
        private String activities;
        private String equipmentStatus;
        private String logoutTime;

        // Approval fields
        private String approvedBy;
        private String approvalDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryUsageDTO {

        private Long id;
        private Long inventoryItemId;
        private String inventoryItemName;
        private Long lotId;
        private String lotNumber;
        private Double quantityUsed;
        private Timestamp usageDate;

        // Enriched user data (not just ID)
        private Integer performedByUserId;
        private String performedByUserName;

        // Optional linked records
        private Long testResultId;
        private Long analysisId;
    }
}
