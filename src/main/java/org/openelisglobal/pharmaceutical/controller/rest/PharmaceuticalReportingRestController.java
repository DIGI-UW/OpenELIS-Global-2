package org.openelisglobal.pharmaceutical.controller.rest;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.pharmaceutical.service.PharmaceuticalReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/pharmaceutical/reports")
public class PharmaceuticalReportingRestController extends BaseRestController {

    @Autowired
    private PharmaceuticalReportingService reportingService;

    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        try {
            Map<String, Object> summary = reportingService.getDashboardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/intake", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getIntakeMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getIntakeMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/qc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getQCMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getQCMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/assays", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAssayMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getAssayMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/oos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getOOSMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getOOSMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/tat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTATMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getTATMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/storage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStorageMetrics() {
        try {
            Map<String, Object> metrics = reportingService.getStorageMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/disposal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDisposalMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getDisposalMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/excursions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getExcursionMetrics(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            Map<String, Object> metrics = reportingService.getExcursionMetrics(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample-status-distribution", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getSampleStatusDistribution() {
        try {
            List<Map<String, Object>> distribution = reportingService.getSampleStatusDistribution();
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/assay-type-distribution", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getAssayTypeDistribution(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            List<Map<String, Object>> distribution = reportingService.getAssayTypeDistribution(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/excursion-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getExcursionHistory(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            List<Map<String, Object>> history = reportingService.getExcursionHistory(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/disposal-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getDisposalHistory(
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            List<Map<String, Object>> history = reportingService.getDisposalHistory(
                    new Timestamp(startDate),
                    new Timestamp(endDate));
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/export/{reportType}/csv")
    public ResponseEntity<byte[]> exportReportAsCSV(
            @PathVariable String reportType,
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            byte[] csvData = reportingService.exportReportAsCSV(
                    reportType,
                    new Timestamp(startDate),
                    new Timestamp(endDate));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "pharmaceutical-" + reportType + "-report.csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/export/{reportType}/pdf")
    public ResponseEntity<byte[]> exportReportAsPDF(
            @PathVariable String reportType,
            @RequestParam Long startDate,
            @RequestParam Long endDate) {
        try {
            byte[] pdfData = reportingService.exportReportAsPDF(
                    reportType,
                    new Timestamp(startDate),
                    new Timestamp(endDate));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "pharmaceutical-" + reportType + "-report.pdf");

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
