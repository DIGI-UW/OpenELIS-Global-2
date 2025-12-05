package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import javax.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory-usage")
public class InventoryUsageRestController extends BaseRestController {

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryUsage> getInventoryUsageById(@PathVariable String id) {
        InventoryUsage usage = inventoryUsageService.get(id);
        if (usage == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(usage);
    }

    @GetMapping(value = "/by-lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getUsageByLot(@PathVariable String lotId) {
        List<InventoryUsage> usageList = inventoryUsageService.findByLotId(lotId);
        return ResponseEntity.ok(usageList);
    }

    @GetMapping(value = "/by-test-result/{testResultId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryUsage>> getUsageByTestResult(@PathVariable String testResultId) {
        List<InventoryUsage> usageList = inventoryUsageService.findByTestResultId(testResultId);
        return ResponseEntity.ok(usageList);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryUsage> createInventoryUsage(HttpServletRequest httpRequest,
            @Valid @RequestBody InventoryUsage usage) {
        usage.setSysUserId(getSysUserId(httpRequest));
        String id = inventoryUsageService.insert(usage);
        InventoryUsage created = inventoryUsageService.get(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
