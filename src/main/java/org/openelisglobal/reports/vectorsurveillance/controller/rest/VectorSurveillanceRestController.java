package org.openelisglobal.reports.vectorsurveillance.controller.rest;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceService;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only surveillance reporting endpoints. Permission-gated by the
 * {@code VectorSurveillanceDashboard} system module (Liquibase 042) via the
 * ModuleAuthenticationInterceptor URL mapping. All figures come from OpenELIS's
 * own data (FR-011) — no external system.
 */
@RestController
@RequestMapping("/rest/reports/vector-surveillance")
public class VectorSurveillanceRestController {

    @Autowired
    private VectorSurveillanceService vectorSurveillanceService;

    @GetMapping(value = "/indices", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurveillanceIndicesDTO> getIndices(@RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo, @RequestParam(required = false) Integer siteId) {
        try {
            return ResponseEntity.ok(vectorSurveillanceService.getIndices(parse(dateFrom), parse(dateTo), siteId));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sites", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SiteOption>> getSites() {
        try {
            return ResponseEntity.ok(vectorSurveillanceService.getSites());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private LocalDate parse(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
