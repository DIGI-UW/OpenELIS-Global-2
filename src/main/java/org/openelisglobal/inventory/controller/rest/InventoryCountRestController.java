package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryCountService;
import org.openelisglobal.inventory.service.InventoryCountService.CountEntry;
import org.openelisglobal.inventory.service.InventoryCountService.CountResult;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Committing a physical count.
 *
 * <p>
 * One endpoint for the whole session rather than one call per lot: a count is a
 * single decision taken at the shelf, and a per-lot loop on the client would
 * leave half a shelf adjusted the first time one call failed.
 */
@RestController
@RequestMapping("/rest/inventory/count")
public class InventoryCountRestController extends BaseRestController {

    @Autowired
    private InventoryCountService inventoryCountService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> recordCount(@RequestBody CountRequest body, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            List<CountEntry> entries = new ArrayList<>();
            if (body != null && body.getEntries() != null) {
                for (CountRequest.Entry supplied : body.getEntries()) {
                    CountEntry entry = new CountEntry();
                    entry.setLotId(supplied.getLotId());
                    entry.setCountedQuantity(supplied.getCountedQuantity());
                    entries.add(entry);
                }
            }

            CountResult result = inventoryCountService.recordCount(entries, sysUserId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Getter
    @Setter
    public static class CountRequest {
        private List<Entry> entries;

        @Getter
        @Setter
        public static class Entry {
            private Long lotId;
            private Double countedQuantity;
        }
    }
}
