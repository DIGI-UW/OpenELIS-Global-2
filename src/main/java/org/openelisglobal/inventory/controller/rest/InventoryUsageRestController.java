package org.openelisglobal.inventory.controller.rest;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
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

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryUsage> getById(@PathVariable String id) {
        try {
            InventoryUsage usage = usageService.get(Long.valueOf(id));
            if (usage == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(usage);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/test-result/{testResultId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByTestResultId(@PathVariable String testResultId) {
        try {
            List<InventoryUsage> usageList = usageService.getByTestResultId(Long.valueOf(testResultId));
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByLotId(@PathVariable String lotId) {
        try {
            List<InventoryUsage> usageList = usageService.getByLotId(Long.valueOf(lotId));
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByItemId(@PathVariable String itemId) {
        try {
            List<InventoryUsage> usageList = usageService.getByInventoryItemId(Long.valueOf(itemId));
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/analysis/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getByAnalysisId(@PathVariable String analysisId) {
        try {
            List<InventoryUsage> usageList = usageService.getByAnalysisId(Long.valueOf(analysisId));
            return ResponseEntity.ok(usageList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
