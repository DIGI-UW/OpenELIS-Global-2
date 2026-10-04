package org.openelisglobal.inventory.controller.rest;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.projection.InventoryProjection;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read side of the inventory items board: one row per active item, already
 * sorted so the items needing attention come first.
 */
@RestController
@RequestMapping("/rest/inventory/board")
public class InventoryBoardRestController extends BaseRestController {

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryProjection>> getBoard() {
        try {
            return ResponseEntity.ok(inventoryProjectionService.getBoard());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
