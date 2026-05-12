package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory/usage")
public class InventoryUsageRestController extends BaseRestController {

    @Autowired
    private InventoryUsageService usageService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryUsage> getById(@PathVariable String id, HttpServletRequest request) {
        try {
            InventoryUsage usage = usageService.get(Long.valueOf(id));
            if (usage == null) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessUsage(usage, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(usage);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/test-result/{testResultId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByTestResultId(@PathVariable String testResultId,
            HttpServletRequest request) {
        try {
            List<InventoryUsage> usageList = filterAccessible(usageService.getByTestResultId(Long.valueOf(testResultId)),
                    request);
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByLotId(@PathVariable String lotId, HttpServletRequest request) {
        try {
            List<InventoryUsage> usageList = filterAccessible(usageService.getByLotId(Long.valueOf(lotId)), request);
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByItemId(@PathVariable String itemId, HttpServletRequest request) {
        try {
            if (!departmentIsolationService.canAccessInventoryItem(inventoryItemService.get(Long.valueOf(itemId)),
                    request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            List<InventoryUsage> usageList = usageService.getByInventoryItemId(Long.valueOf(itemId));
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/analysis/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByAnalysisId(@PathVariable String analysisId,
            HttpServletRequest request) {
        try {
            List<InventoryUsage> usageList = filterAccessible(usageService.getByAnalysisId(Long.valueOf(analysisId)),
                    request);
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<InventoryUsage> filterAccessible(List<InventoryUsage> usageList, HttpServletRequest request) {
        return usageList.stream().filter(usage -> canAccessUsage(usage, request)).toList();
    }

    private boolean canAccessUsage(InventoryUsage usage, HttpServletRequest request) {
        return usage != null
                && departmentIsolationService.canAccessInventoryItem(usage.getInventoryItem(), request);
    }
}
