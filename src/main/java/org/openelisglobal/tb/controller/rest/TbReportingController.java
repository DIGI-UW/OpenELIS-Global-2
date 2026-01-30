package org.openelisglobal.tb.controller.rest;

import java.time.LocalDate;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.tb.service.TbReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for TB Reporting metrics. Provides endpoints for dashboard
 * summary, sample intake, QC, culture, smear, GeneXpert, DST, and turnaround
 * time metrics.
 */
@RestController
@RequestMapping(value = "/rest/tb/reporting")
public class TbReportingController extends BaseRestController {

    @Autowired
    private TbReportingService tbReportingService;

    /**
     * Get comprehensive dashboard summary with key metrics from all pages.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboardSummary(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> summary = tbReportingService.getDashboardSummary(notebookId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get sample intake metrics from Page 1 (Registration). Includes breakdown by
     * specimen type, facility, and treatment history.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/intake", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSampleIntakeMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getSampleIntakeMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get QC metrics from Page 2 (Quality Check). Includes pass/fail rates and
     * rejection reason breakdown.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/qc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getQcMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getQcMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get culture metrics from Page 4 (Incubation & Monitoring). Includes
     * positivity rate, contamination rate, and average positive week.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/culture", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCultureMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getCultureMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get smear microscopy metrics from Page 5 (Test Execution). Includes
     * positivity rate and AFB grading breakdown.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/smear", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSmearMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getSmearMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get GeneXpert metrics from Page 5 (Test Execution). Includes MTB detection
     * rate and RIF resistance rate.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/genexpert", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getGeneXpertMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getGeneXpertMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get DST (Drug Susceptibility Testing) metrics from Page 5 (Test Execution).
     * Includes MDR/XDR rates and classification breakdown.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/dst", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDstMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getDstMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get turnaround time (TAT) metrics. Includes average, min, and max culture TAT
     * in days.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/tat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTurnaroundTimeMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getTurnaroundTimeMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get resistance trends (MDR/XDR) over time. Alias for DST metrics focused on
     * resistance patterns.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/resistance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getResistanceTrends(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Resistance trends are derived from DST metrics
        Map<String, Object> metrics = tbReportingService.getDstMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get disposal and archiving metrics from Page 7 (Disposal & Archiving).
     * Includes disposed/archived counts, breakdown by reason and method, and
     * biorepository transfer counts.
     *
     * @param notebookId the TB notebook ID
     * @param startDate  start of date range (YYYY-MM-DD)
     * @param endDate    end of date range (YYYY-MM-DD)
     */
    @GetMapping(value = "/{notebookId}/disposal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDisposalMetrics(@PathVariable Integer notebookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> metrics = tbReportingService.getDisposalMetrics(notebookId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
}
