package org.openelisglobal.reports.vectorsurveillance.manualentry.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntrySubmissionService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.service.ManualEntryViewService;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryViewDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual Entry Helper endpoints (US4). Permission module
 * {@code VectorManualEntryHelper} (enforced by the
 * ModuleAuthenticationInterceptor via system_module_url mapping).
 */
@RestController
@RequestMapping("/rest/reports/vector-surveillance/manual-entry")
public class ManualEntryRestController extends BaseRestController {

    @Autowired
    private ManualEntryViewService viewService;

    @Autowired
    private ManualEntrySubmissionService submissionService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManualEntryViewDTO> getManualEntryView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) Integer siteId) {
        try {
            return ResponseEntity.ok(viewService.getView(periodStart, periodEnd, siteId));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManualEntrySubmissionAudit> submit(@RequestBody ManualEntrySubmitRequest request,
            HttpServletRequest httpRequest) {
        try {
            ManualEntrySubmissionAudit audit = submissionService.submit(request.getPeriodStart(),
                    request.getPeriodEnd(), request.getSiteId(), request.getValueSnapshot(), getSysUserId(httpRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(audit);
        } catch (LIMSRuntimeException validation) {
            // Bad period / empty snapshot / missing user — a client error, not a 500.
            LogEvent.logError(validation);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
