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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read side of the inventory items board: one row per active item, already
 * sorted so the items needing attention come first.
 */
@RestController
@RequestMapping("/rest/inventory/board")
/**
 * Server-side authorization for the whole inventory module lives on these
 * annotations, not in {@code system_module}. Two things rule that out: the
 * interceptor matches {@code system_module_url.url_path} by exact string, so no
 * row can cover the paths carrying an {@code {id}}, and an unmatched
 * {@code /rest} path is allowed rather than refused
 * ({@code ModuleAuthenticationInterceptor}). The roles mirror the {@code
 * /inventory} route's own guard, so the API admits exactly who the screen does.
 */
@PreAuthorize("hasAnyRole('RESULTS', 'ADMIN')")
public class InventoryBoardRestController extends BaseRestController {

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryProjection>> getBoard(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        try {
            return ResponseEntity.ok(inventoryProjectionService.getBoard(includeInactive));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
