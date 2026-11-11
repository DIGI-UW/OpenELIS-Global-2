package org.openelisglobal.reports.dataexport.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.form.SavedReportMutation;
import org.openelisglobal.reports.dataexport.service.ReportingAccess;
import org.openelisglobal.reports.dataexport.service.ReportingAudit;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.reports.dataexport.service.ReportingJobService;
import org.openelisglobal.reports.dataexport.service.ReportingSavedConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/reports/data-export")
public class ReportingExportController extends BaseRestController {
    @Autowired
    private UserContextHolder user;
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    private ReportingAccess access;
    @Autowired
    private ReportingJobService jobs;
    @Autowired
    private ReportingSavedConfigService savedReports;

    private String owner() {
        String id = user.getCurrentSysUserId();
        if (id == null) {
            ReportingAudit.denied("anonymous", null);
            throw new ReportingException(401, "reporting.access.signIn");
        }
        return id;
    }

    @GetMapping("/report-types")
    public Object types() {
        access.requireReports(owner());
        return catalog.definitions();
    }

    @GetMapping("/variables")
    public Object variables(@RequestParam String reportType,
            @RequestParam(defaultValue = "SPREADSHEET") String layout) {
        return catalog.catalog(owner(), reportType, layout);
    }

    @GetMapping("/saved-configs")
    public Object savedReports(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search) {
        return savedReports.list(owner(), page, size, search);
    }

    @PostMapping("/saved-configs")
    public Object createSavedReport(@RequestBody SavedReportMutation request) {
        return ResponseEntity.status(201).body(savedReports.create(owner(), request));
    }

    @GetMapping("/saved-configs/{id}")
    public Object savedReport(@PathVariable String id) {
        return savedReports.detail(owner(), id);
    }

    @PutMapping("/saved-configs/{id}")
    public Object updateSavedReport(@PathVariable String id, @RequestBody SavedReportMutation request) {
        return savedReports.update(owner(), id, request);
    }

    @DeleteMapping("/saved-configs/{id}")
    public Object deleteSavedReport(@PathVariable String id, @RequestParam String expectedVersion) {
        savedReports.remove(owner(), id, expectedVersion);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs")
    public Object submit(@RequestBody ExportSubmission request) {
        return ResponseEntity.accepted().body(jobs.submit(owner(), request));
    }

    @GetMapping("/jobs")
    public Object list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return jobs.list(owner(), page, size);
    }

    @GetMapping("/jobs/{id}")
    public Object job(@PathVariable String id) {
        return jobs.detail(owner(), id);
    }

    public record RetryRequest(String clientRequestId) {
    }

    @PostMapping("/jobs/{id}/retry")
    public Object retry(@PathVariable String id, @RequestBody RetryRequest request) {
        return ResponseEntity.accepted().body(jobs.retry(owner(), id, request.clientRequestId()));
    }

    @PostMapping("/jobs/{id}/cancel")
    public Object cancel(@PathVariable String id) {
        return jobs.cancel(owner(), id);
    }

    @GetMapping("/jobs/{id}/download")
    public void download(@PathVariable String id, HttpServletResponse response) throws IOException {
        var download = jobs.download(owner(), id);
        var job = download.job();
        try (var input = download.input()) {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Content-Disposition", "attachment; filename=\"report-" + job.id() + ".csv\"");
            response.setContentLengthLong(job.fileSize());
            input.transferTo(response.getOutputStream());
        }
    }

    @ExceptionHandler(ReportingException.class)
    public ResponseEntity<?> reportingError(ReportingException error) {
        return ResponseEntity.status(error.status()).body(Map.of("code", error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException error) {
        return ResponseEntity.unprocessableEntity().body(Map.of("code", "reporting.request.invalid"));
    }
}
