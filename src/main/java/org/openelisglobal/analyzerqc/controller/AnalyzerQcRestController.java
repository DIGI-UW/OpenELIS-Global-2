package org.openelisglobal.analyzerqc.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.analyzerqc.form.QcRunForm;
import org.openelisglobal.analyzerqc.service.AnalyzerQcService;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcRun;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for Analyzer Manual QC Recording (Issue #3490).
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerQcRestController {

    @Autowired
    private AnalyzerQcService analyzerQcService;

    @GetMapping(
            value = "/{id}/qc-status",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ANALYZER_VIEW')")
    public ResponseEntity<AnalyzerQcStatus> getQcStatus(@PathVariable String id) {
        try {
            return ResponseEntity.ok(analyzerQcService.getQcStatus(id));
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "getQcStatus",
                    "Analyzer not found: " + id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LogEvent.logError(getClass().getName(), "getQcStatus", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(
            value = "/{id}/qc-runs",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QC_RESULT_ENTER')")
    public ResponseEntity<AnalyzerQcStatus> recordQcRun(
            @PathVariable String id,
            @RequestBody QcRunForm form,
            HttpServletRequest request) {
        try {
            analyzerQcService.recordQcRun(id, form, getSysUserId(request));
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(analyzerQcService.getQcStatus(id));
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(getClass().getName(), "recordQcRun", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(getClass().getName(), "recordQcRun", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(
            value = "/{id}/qc-runs",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QC_HISTORY_VIEW')")
    public ResponseEntity<List<AnalyzerQcRun>> getQcHistory(@PathVariable String id) {
        try {
            return ResponseEntity.ok(analyzerQcService.getQcHistory(id));
        } catch (Exception e) {
            LogEvent.logError(getClass().getName(), "getQcHistory", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getSysUserId(HttpServletRequest request) {
        UserSessionData sessionData = (UserSessionData)
                request.getSession().getAttribute("userSessionData");
        return sessionData != null
                ? String.valueOf(sessionData.getSystemUserId())
                : null;
    }
}