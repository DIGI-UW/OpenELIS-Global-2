package org.openelisglobal.biorepository.controller.rest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.BiorepositoryDashboardService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for biorepository dashboard metrics and reporting.
 *
 * Provides aggregated statistics for: - Storage capacity and utilization -
 * Sample aging and expiration warnings - QC compliance and inspection trends -
 * Retrieval and disposal statistics - Environmental monitoring (temperature,
 * O2, humidity)
 *
 * All endpoints are read-only and optimized for dashboard performance.
 */
@RestController
@RequestMapping(value = "/rest/biorepository/dashboard")
public class BiorepositoryDashboardRestController extends BaseRestController {

    @Autowired
    private BiorepositoryDashboardService dashboardService;

    /**
     * Get storage capacity metrics.
     *
     * Returns: - totalSamplesStored: Count of STORED samples - pendingStorage:
     * Count of PENDING_STORAGE samples - totalDevices: Count of storage devices -
     * averageUtilization: Average utilization percentage
     *
     * @return ResponseEntity with storage metrics map
     */
    @GetMapping(value = "/storage-capacity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStorageCapacity() {
        try {
            Map<String, Object> metrics = dashboardService.getStorageCapacityMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load storage capacity metrics: " + e.getMessage()));
        }
    }

    /**
     * Get storage utilization breakdown by device.
     *
     * Returns list of devices with: - deviceId, deviceName - currentUsage,
     * totalCapacity - utilizationPercent
     *
     * @return ResponseEntity with device utilization list
     */
    @GetMapping(value = "/storage-utilization", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStorageUtilization() {
        try {
            Map<String, Object> utilization = dashboardService.getStorageUtilizationByDevice();
            return ResponseEntity.ok(utilization);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load storage utilization: " + e.getMessage()));
        }
    }

    /**
     * Get sample aging metrics.
     *
     * Returns: - expired: Samples past retention expiry - expiring30Days: Expiring
     * within 30 days - expiring60Days: Expiring within 31-60 days - expiring90Days:
     * Expiring within 61-90 days - total: Total active samples - averageAgeYears:
     * Average sample age
     *
     * @return ResponseEntity with aging metrics map
     */
    @GetMapping(value = "/sample-aging", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSampleAging() {
        try {
            Map<String, Object> metrics = dashboardService.getSampleAgingMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load sample aging metrics: " + e.getMessage()));
        }
    }

    /**
     * Get samples expiring within a threshold.
     *
     * @param daysThreshold Number of days threshold (default: 30)
     * @return ResponseEntity with list of expiring samples
     */
    @GetMapping(value = "/expiration-warnings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getExpirationWarnings(
            @RequestParam(required = false, defaultValue = "30") Integer daysThreshold) {
        try {
            List<Map<String, Object>> warnings = dashboardService.getExpirationWarnings(daysThreshold);
            return ResponseEntity.ok(warnings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get QC compliance metrics.
     *
     * Returns: - totalInspections, passedInspections, failedInspections -
     * complianceRate (percentage) - Individual checkpoint pass rates
     * (samplePresentPassRate, labelIntegrityPassRate, etc.)
     *
     * @return ResponseEntity with QC metrics map
     */
    @GetMapping(value = "/qc-metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getQCMetrics() {
        try {
            Map<String, Object> metrics = dashboardService.getQCComplianceMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load QC metrics: " + e.getMessage()));
        }
    }

    /**
     * Get QC discrepancy breakdown by type.
     *
     * Returns map with discrepancy type as key and count as value.
     *
     * @return ResponseEntity with discrepancy breakdown map
     */
    @GetMapping(value = "/qc-discrepancies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getQCDiscrepancies() {
        try {
            Map<String, Object> breakdown = dashboardService.getQCDiscrepancyBreakdown();
            return ResponseEntity.ok(breakdown);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load QC discrepancy breakdown: " + e.getMessage()));
        }
    }

    /**
     * Get retrieval statistics within date range.
     *
     * Returns: - totalRequests, completedRequests, pendingRequests,
     * rejectedRequests - totalItemsRetrieved, returnedItems, consumedItems -
     * overdueReturns
     *
     * @param startDate Start date in YYYY-MM-DD format (optional, default: 30 days
     *                  ago)
     * @param endDate   End date in YYYY-MM-DD format (optional, default: today)
     * @return ResponseEntity with retrieval stats map
     */
    @GetMapping(value = "/retrieval-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getRetrievalStats(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE) : null;

            Map<String, Object> stats = dashboardService.getRetrievalStatistics(start, end);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load retrieval statistics: " + e.getMessage()));
        }
    }

    /**
     * Get disposal statistics within date range.
     *
     * Returns: - totalDisposed: Count of disposed samples - disposalsByProject:
     * Breakdown by project ID
     *
     * @param startDate Start date in YYYY-MM-DD format (optional, default: 30 days
     *                  ago)
     * @param endDate   End date in YYYY-MM-DD format (optional, default: today)
     * @return ResponseEntity with disposal stats map
     */
    @GetMapping(value = "/disposal-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDisposalStats(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE) : null;

            Map<String, Object> stats = dashboardService.getDisposalStatistics(start, end);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load disposal statistics: " + e.getMessage()));
        }
    }

    /**
     * Get temperature excursions within date range.
     *
     * Returns list of excursions with: - freezerId, excursionTimestamp -
     * temperature, temperatureUnit - checkedBy, notes
     *
     * @param startDate Start date in YYYY-MM-DD format (optional, default: 7 days
     *                  ago)
     * @param endDate   End date in YYYY-MM-DD format (optional, default: today)
     * @param freezerId Optional freezer ID filter
     * @return ResponseEntity with excursion list
     */
    @GetMapping(value = "/temperature-trends", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getTemperatureTrends(
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String freezerId) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE) : null;

            List<Map<String, Object>> excursions = dashboardService.getTemperatureExcursions(start, end);

            // Filter by freezerId if provided
            if (freezerId != null && !freezerId.trim().isEmpty()) {
                excursions = excursions.stream().filter(exc -> freezerId.equals(exc.get("freezerId"))).toList();
            }

            return ResponseEntity.ok(excursions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get environmental compliance metrics (temperature, O2, humidity).
     *
     * Returns: - temperatureCompliance, oxygenCompliance, humidityCompliance
     * (percentages) - totalTemperatureReadings, totalOxygenReadings,
     * totalHumidityReadings
     *
     * @return ResponseEntity with environmental metrics map
     */
    @GetMapping(value = "/environmental-compliance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getEnvironmentalCompliance() {
        try {
            Map<String, Object> metrics = dashboardService.getEnvironmentalComplianceMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load environmental compliance metrics: " + e.getMessage()));
        }
    }
}
