package org.openelisglobal.biorepository.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service for biorepository dashboard metrics and reporting.
 *
 * Aggregates data from multiple sources (BioSample, SampleStorageAssignment,
 * QCInspection, Temperature logs, Retrieval requests) to provide comprehensive
 * dashboard statistics.
 *
 * All methods return Maps with well-defined keys for frontend consumption. All
 * queries are read-only and optimized for dashboard performance.
 */
public interface BiorepositoryDashboardService {

    /**
     * Get storage capacity metrics including total devices, utilization
     * percentages, and sample distribution.
     *
     * @return Map with keys: - totalSamplesStored (Long): Count of samples with
     *         status STORED - pendingStorage (Long): Count of samples pending
     *         storage assignment - totalDevices (Long): Count of storage devices -
     *         averageUtilization (Double): Average utilization percentage across
     *         all devices
     */
    default Map<String, Object> getStorageCapacityMetrics() {
        return getStorageCapacityMetrics(null);
    }

    Map<String, Object> getStorageCapacityMetrics(HttpServletRequest request);

    /**
     * Get storage utilization breakdown by individual devices.
     *
     * @return List of Maps, each containing: - deviceId (Integer): Storage device
     *         ID - deviceName (String): Device name - currentUsage (Long): Current
     *         sample count - totalCapacity (Long): Device capacity -
     *         utilizationPercent (Double): (currentUsage / totalCapacity) * 100
     */
    default Map<String, Object> getStorageUtilizationByDevice() {
        return getStorageUtilizationByDevice(null);
    }

    Map<String, Object> getStorageUtilizationByDevice(HttpServletRequest request);

    /**
     * Get sample aging metrics including expiration warnings.
     *
     * @return Map with keys: - expired (Long): Samples past retention expiry date -
     *         expiring30Days (Long): Samples expiring within 30 days -
     *         expiring60Days (Long): Samples expiring within 31-60 days -
     *         expiring90Days (Long): Samples expiring within 61-90 days - total
     *         (Long): Total active samples (STORED + IN_USE) - averageAgeYears
     *         (Double): Average age in years
     */
    default Map<String, Object> getSampleAgingMetrics() {
        return getSampleAgingMetrics(null);
    }

    Map<String, Object> getSampleAgingMetrics(HttpServletRequest request);

    /**
     * Get samples expiring within a specific threshold.
     *
     * @param daysThreshold Number of days threshold (e.g., 30 for samples expiring
     *                      in next 30 days)
     * @return List of Maps with sample details (barcode, expiryDate, projectId,
     *         etc.)
     */
    default List<Map<String, Object>> getExpirationWarnings(Integer daysThreshold) {
        return getExpirationWarnings(null, daysThreshold);
    }

    List<Map<String, Object>> getExpirationWarnings(HttpServletRequest request, Integer daysThreshold);

    /**
     * Get QC compliance metrics including pass rates and checkpoint breakdown.
     *
     * @return Map with keys: - totalInspections (Long): Total QC inspections -
     *         passedInspections (Long): Inspections with VERIFIED result -
     *         failedInspections (Long): Inspections with DISCREPANCY_FOUND -
     *         complianceRate (Double): (passedInspections / totalInspections) * 100
     *         - samplePresentPassRate (Double): Percentage of samples present -
     *         labelIntegrityPassRate (Double): Percentage with good labels -
     *         containerIntegrityPassRate (Double): Percentage with intact
     *         containers - volumeAcceptablePassRate (Double): Percentage with
     *         acceptable volume - correctPositionPassRate (Double): Percentage in
     *         correct position
     */
    default Map<String, Object> getQCComplianceMetrics() {
        return getQCComplianceMetrics(null);
    }

    Map<String, Object> getQCComplianceMetrics(HttpServletRequest request);

    /**
     * Get breakdown of QC discrepancies by type.
     *
     * @return Map with discrepancy type as key and count as value: MISSING_SAMPLE,
     *         DAMAGED_LABEL, MISPLACED_ITEM, CONTAINER_DAMAGE, VOLUME_DISCREPANCY,
     *         OTHER
     */
    default Map<String, Object> getQCDiscrepancyBreakdown() {
        return getQCDiscrepancyBreakdown(null);
    }

    Map<String, Object> getQCDiscrepancyBreakdown(HttpServletRequest request);

    /**
     * Get completed QC inspection history for reporting dashboards.
     *
     * @param limit maximum number of most-recent records to return (optional,
     *              defaults to 50)
     * @return Map with keys: - source (String): underlying source table/entity -
     *         count (Integer): number of returned records - items (List<Map>):
     *         completed inspection records with date/result/technician/location
     *         context
     */
    default Map<String, Object> getQCHistory(Integer limit) {
        return getQCHistory(null, limit);
    }

    Map<String, Object> getQCHistory(HttpServletRequest request, Integer limit);

    /**
     * Get retrieval statistics within a date range.
     *
     * @param startDate Start date (optional, defaults to 30 days ago)
     * @param endDate   End date (optional, defaults to today)
     * @return Map with keys: - totalRequests (Long): Total retrieval requests -
     *         completedRequests (Long): Requests with COMPLETED status -
     *         pendingRequests (Long): Requests pending/in progress -
     *         rejectedRequests (Long): Rejected requests - totalItemsRetrieved
     *         (Long): Total items retrieved - returnedItems (Long): Items returned
     *         - consumedItems (Long): Items consumed - overdueReturns (Long): Items
     *         past expected return date
     */
    default Map<String, Object> getRetrievalStatistics(LocalDate startDate, LocalDate endDate) {
        return getRetrievalStatistics(null, startDate, endDate);
    }

    Map<String, Object> getRetrievalStatistics(HttpServletRequest request, LocalDate startDate, LocalDate endDate);

    /**
     * Get disposal statistics within a date range.
     *
     * @param startDate Start date (optional, defaults to 30 days ago)
     * @param endDate   End date (optional, defaults to today)
     * @return Map with keys: - totalDisposed (Long): Total samples with DISPOSED
     *         status - disposalsByProject (Map<String, Long>): Breakdown by project
     *         ID Note: For MVP, returns basic counts. Enhanced in Phase 4 with
     *         BioSampleDisposal entity.
     */
    default Map<String, Object> getDisposalStatistics(LocalDate startDate, LocalDate endDate) {
        return getDisposalStatistics(null, startDate, endDate);
    }

    Map<String, Object> getDisposalStatistics(HttpServletRequest request, LocalDate startDate, LocalDate endDate);

    /**
     * Get temperature excursions (readings outside acceptable range) within a date
     * range.
     *
     * @param startDate Start date (optional, defaults to 7 days ago)
     * @param endDate   End date (optional, defaults to today)
     * @return List of Maps with: - freezerId (String): Device identifier -
     *         excursionTimestamp (Timestamp): When excursion occurred - temperature
     *         (Double): Temperature value - temperatureUnit (String): C or F -
     *         checkedBy (String): Who recorded - notes (String): Any notes
     */
    default List<Map<String, Object>> getTemperatureExcursions(LocalDate startDate, LocalDate endDate) {
        return getTemperatureExcursions(null, startDate, endDate);
    }

    List<Map<String, Object>> getTemperatureExcursions(HttpServletRequest request, LocalDate startDate,
            LocalDate endDate);

    /**
     * Get environmental compliance metrics (temperature, O2, humidity).
     *
     * @return Map with keys: - temperatureCompliance (Double): % readings in range
     *         - oxygenCompliance (Double): % readings >= 19.5% - humidityCompliance
     *         (Double): % readings in 30-60% range - totalTemperatureReadings
     *         (Long): Total readings - totalOxygenReadings (Long): Total O2
     *         readings - totalHumidityReadings (Long): Total humidity readings
     */
    default Map<String, Object> getEnvironmentalComplianceMetrics() {
        return getEnvironmentalComplianceMetrics(null);
    }

    Map<String, Object> getEnvironmentalComplianceMetrics(HttpServletRequest request);
}
