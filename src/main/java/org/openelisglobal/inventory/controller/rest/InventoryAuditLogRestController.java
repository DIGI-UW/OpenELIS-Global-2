package org.openelisglobal.inventory.controller.rest;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryAuditService;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory/audit-logs")
public class InventoryAuditLogRestController extends BaseRestController {

    @Autowired
    private InventoryAuditService inventoryAuditService;

    @GetMapping(value = "/item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryAuditLog>> getItemAuditTrail(@PathVariable Long itemId) {
        try {
            List<InventoryAuditLog> auditLogs = inventoryAuditService.getItemAuditTrail(itemId);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryAuditLog>> getLotAuditTrail(@PathVariable Long lotId) {
        try {
            List<InventoryAuditLog> auditLogs = inventoryAuditService.getLotAuditTrail(lotId);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/entity/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryAuditLog>> getEntityAuditTrail(@PathVariable String entityType,
            @PathVariable Long entityId) {
        try {
            EntityType type = EntityType.valueOf(entityType.toUpperCase());
            List<InventoryAuditLog> auditLogs = inventoryAuditService.getAuditTrail(type, entityId);
            return ResponseEntity.ok(auditLogs);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
