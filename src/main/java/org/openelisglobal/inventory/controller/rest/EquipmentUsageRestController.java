package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.controller.rest.dto.EquipmentUsageEntryDTO;
import org.openelisglobal.inventory.controller.rest.dto.EquipmentUsageMetricsDTO;
import org.openelisglobal.inventory.controller.rest.dto.EquipmentUsageMetricsDTO.EquipmentUsageStat;
import org.openelisglobal.inventory.controller.rest.dto.InventoryUsageDTO;
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
                EquipmentUsageStat stat = equipmentStats.computeIfAbsent(itemId, k -> {
                    return EquipmentUsageStat.builder().equipmentId(itemId)
                            .equipmentName(usage.getInventoryItem().getName()).usageCount(0).totalQuantityUsed(0.0)
                            .build();
                });
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

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getLotId() {
            return lotId;
        }

        public void setLotId(Long lotId) {
            this.lotId = lotId;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public String getLabUnitId() {
            return labUnitId;
        }

        public void setLabUnitId(String labUnitId) {
            this.labUnitId = labUnitId;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getLoginTime() {
            return loginTime;
        }

        public void setLoginTime(String loginTime) {
            this.loginTime = loginTime;
        }

        public String getLogoutTime() {
            return logoutTime;
        }

        public void setLogoutTime(String logoutTime) {
            this.logoutTime = logoutTime;
        }

        public String getActivities() {
            return activities;
        }

        public void setActivities(String activities) {
            this.activities = activities;
        }

        public String getEquipmentStatus() {
            return equipmentStatus;
        }

        public void setEquipmentStatus(String equipmentStatus) {
            this.equipmentStatus = equipmentStatus;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }

        public String getApprovalDate() {
            return approvalDate;
        }

        public void setApprovalDate(String approvalDate) {
            this.approvalDate = approvalDate;
        }
    }

    /**
     * Request body for recording equipment usage.
     */
    public static class EquipmentUsageRequest {
        public Long itemId;
        public Long lotId;
        public Double quantity;
        public String labUnitId;

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getLotId() {
            return lotId;
        }

        public void setLotId(Long lotId) {
            this.lotId = lotId;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public String getLabUnitId() {
            return labUnitId;
        }

        public void setLabUnitId(String labUnitId) {
            this.labUnitId = labUnitId;
        }
    }
}
