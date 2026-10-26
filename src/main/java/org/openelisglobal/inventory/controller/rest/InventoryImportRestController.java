package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.imports.InventoryImportPlan;
import org.openelisglobal.inventory.imports.InventoryImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory/import")
@PreAuthorize("hasAnyRole('RESULTS', 'ADMIN')")
public class InventoryImportRestController extends BaseRestController {

    static final int MAX_CHARACTERS = 1_000_000;

    @Autowired
    private InventoryImportService inventoryImportService;

    @GetMapping(value = "/template", produces = "text/csv")
    public ResponseEntity<String> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-items-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv")).body(inventoryImportService.template());
    }

    @PostMapping(value = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> preview(@RequestBody CsvPayload payload) {
        String refusal = refuse(payload);
        if (refusal != null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", refusal));
        }
        try {
            return ResponseEntity.ok(inventoryImportService.preview(payload.getCsv()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> apply(@RequestBody CsvPayload payload, HttpServletRequest request) {
        String refusal = refuse(payload);
        if (refusal != null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", refusal));
        }
        try {
            InventoryImportPlan plan = inventoryImportService.apply(payload.getCsv(), getSysUserId(request));
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String refuse(CsvPayload payload) {
        if (payload == null || payload.getCsv() == null || payload.getCsv().isBlank()) {
            return "No file contents were sent";
        }
        if (payload.getCsv().length() > MAX_CHARACTERS) {
            return "That file is too large to import";
        }
        return null;
    }

    public static class CsvPayload {
        private String csv;

        public String getCsv() {
            return csv;
        }

        public void setCsv(String csv) {
            this.csv = csv;
        }
    }
}
