package org.openelisglobal.biorepository.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService;
import org.openelisglobal.notebook.service.NotebookEntryTemperatureLogService;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;
import org.openelisglobal.notebook.valueholder.NotebookEntryTemperatureLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BiorepositoryDashboardService for dashboard metrics
 * aggregation.
 *
 * All methods are @Transactional(readOnly = true) for performance. Eagerly
 * loads relationships to prevent LazyInitializationException. Uses efficient
 * queries and stream processing for large datasets.
 */
@Service
public class BiorepositoryDashboardServiceImpl implements BiorepositoryDashboardService {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private SampleRetrievalService retrievalService;

    @Autowired
    private NotebookEntryTemperatureLogService temperatureLogService;

    @Autowired
    private NotebookEntryRoomEnvironmentLogService roomEnvironmentLogService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStorageCapacityMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get all biosamples
        List<BioSample> allSamples = bioSampleService.getAll();

        // Count by workflow status
        long totalStored = allSamples.stream()
                .filter(bs -> BioSample.WorkflowStatus.STORED.equals(bs.getWorkflowStatus())).count();

        long pendingStorage = allSamples.stream()
                .filter(bs -> BioSample.WorkflowStatus.PENDING_STORAGE.equals(bs.getWorkflowStatus())).count();

        metrics.put("totalSamplesStored", totalStored);
        metrics.put("pendingStorage", pendingStorage);

        // Note: Device-level utilization requires SampleStorageAssignment integration
        // For MVP, provide basic counts
        metrics.put("totalDevices", 0L); // Placeholder for Phase 2
        metrics.put("averageUtilization", 0.0); // Placeholder for Phase 2

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStorageUtilizationByDevice() {
        Map<String, Object> utilization = new HashMap<>();

        // Note: Full implementation requires SampleStorageAssignment service
        // integration
        // Placeholder for MVP
        utilization.put("devices", new ArrayList<>());

        return utilization;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleAgingMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        LocalDate today = LocalDate.now();

        // Get active samples (STORED + IN_USE)
        List<BioSample> activeSamples = bioSampleService.getAll().stream()
                .filter(bs -> BioSample.WorkflowStatus.STORED.equals(bs.getWorkflowStatus())
                        || BioSample.WorkflowStatus.IN_USE.equals(bs.getWorkflowStatus()))
                .collect(Collectors.toList());

        long totalActive = activeSamples.size();

        // Count expiration warnings
        long expired = activeSamples.stream().filter(bs -> bs.getRetentionExpiryDate() != null)
                .filter(bs -> bs.getRetentionExpiryDate().toLocalDate().isBefore(today)).count();

        long expiring30Days = activeSamples.stream().filter(bs -> bs.getRetentionExpiryDate() != null)
                .filter(bs -> !bs.getRetentionExpiryDate().toLocalDate().isBefore(today))
                .filter(bs -> ChronoUnit.DAYS.between(today, bs.getRetentionExpiryDate().toLocalDate()) <= 30).count();

        long expiring60Days = activeSamples.stream().filter(bs -> bs.getRetentionExpiryDate() != null)
                .filter(bs -> !bs.getRetentionExpiryDate().toLocalDate().isBefore(today)).filter(bs -> {
                    long daysUntil = ChronoUnit.DAYS.between(today, bs.getRetentionExpiryDate().toLocalDate());
                    return daysUntil > 30 && daysUntil <= 60;
                }).count();

        long expiring90Days = activeSamples.stream().filter(bs -> bs.getRetentionExpiryDate() != null)
                .filter(bs -> !bs.getRetentionExpiryDate().toLocalDate().isBefore(today)).filter(bs -> {
                    long daysUntil = ChronoUnit.DAYS.between(today, bs.getRetentionExpiryDate().toLocalDate());
                    return daysUntil > 60 && daysUntil <= 90;
                }).count();

        // Calculate average age (placeholder - would need collection dates)
        double averageAgeYears = 0.0; // Placeholder for more complex calculation

        metrics.put("expired", expired);
        metrics.put("expiring30Days", expiring30Days);
        metrics.put("expiring60Days", expiring60Days);
        metrics.put("expiring90Days", expiring90Days);
        metrics.put("total", totalActive);
        metrics.put("averageAgeYears", averageAgeYears);

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getExpirationWarnings(Integer daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold != null ? daysThreshold : 30);

        List<BioSample> activeSamples = bioSampleService.getAll().stream()
                .filter(bs -> BioSample.WorkflowStatus.STORED.equals(bs.getWorkflowStatus())
                        || BioSample.WorkflowStatus.IN_USE.equals(bs.getWorkflowStatus()))
                .filter(bs -> bs.getRetentionExpiryDate() != null)
                .filter(bs -> !bs.getRetentionExpiryDate().toLocalDate().isBefore(today))
                .filter(bs -> bs.getRetentionExpiryDate().toLocalDate().isBefore(thresholdDate))
                .collect(Collectors.toList());

        return activeSamples.stream().map(bs -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("bioSampleId", bs.getId());
            detail.put("barcode", bs.getBarcode());
            detail.put("expiryDate", bs.getRetentionExpiryDate());
            detail.put("projectId", bs.getProjectId());
            detail.put("daysUntilExpiry", ChronoUnit.DAYS.between(today, bs.getRetentionExpiryDate().toLocalDate()));
            return detail;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getQCComplianceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get all QC inspections
        List<BiorepositoryQCInspection> inspections = qcInspectionService.getAll();

        long totalInspections = inspections.size();
        long passedInspections = inspections.stream()
                .filter(insp -> BiorepositoryQCInspection.QCResult.VERIFIED.equals(insp.getQcResult())).count();
        long failedInspections = totalInspections - passedInspections;

        double complianceRate = totalInspections > 0 ? (passedInspections * 100.0 / totalInspections) : 0.0;

        // Calculate individual checkpoint pass rates
        double samplePresentPassRate = calculateCheckpointPassRate(inspections,
                BiorepositoryQCInspection::isSamplePresent);
        double labelIntegrityPassRate = calculateCheckpointPassRate(inspections,
                BiorepositoryQCInspection::isLabelIntegrity);
        double containerIntegrityPassRate = calculateCheckpointPassRate(inspections,
                BiorepositoryQCInspection::isContainerIntegrity);
        double volumeAcceptablePassRate = calculateCheckpointPassRate(inspections,
                BiorepositoryQCInspection::isVolumeAppearanceAcceptable);
        double correctPositionPassRate = calculateCheckpointPassRate(inspections,
                BiorepositoryQCInspection::isCorrectPosition);

        metrics.put("totalInspections", totalInspections);
        metrics.put("passedInspections", passedInspections);
        metrics.put("failedInspections", failedInspections);
        metrics.put("complianceRate", complianceRate);
        metrics.put("samplePresentPassRate", samplePresentPassRate);
        metrics.put("labelIntegrityPassRate", labelIntegrityPassRate);
        metrics.put("containerIntegrityPassRate", containerIntegrityPassRate);
        metrics.put("volumeAcceptablePassRate", volumeAcceptablePassRate);
        metrics.put("correctPositionPassRate", correctPositionPassRate);

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getQCDiscrepancyBreakdown() {
        Map<String, Object> breakdown = new HashMap<>();

        List<BiorepositoryQCInspection> failedInspections = qcInspectionService.getAll().stream()
                .filter(insp -> BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(insp.getQcResult()))
                .collect(Collectors.toList());

        // Group by discrepancy type
        Map<BiorepositoryQCInspection.DiscrepancyType, Long> typeCounts = failedInspections.stream()
                .filter(insp -> insp.getDiscrepancyType() != null)
                .collect(Collectors.groupingBy(BiorepositoryQCInspection::getDiscrepancyType, Collectors.counting()));

        // Convert to string keys for JSON serialization
        typeCounts.forEach((type, count) -> breakdown.put(type.name(), count));

        return breakdown;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRetrievalStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        // Default date range: last 30 days
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        // Get all retrieval requests
        List<SampleRetrievalRequest> allRequests = retrievalService.getAll();

        // Filter by date range
        List<SampleRetrievalRequest> filteredRequests = allRequests.stream().filter(req -> {
            LocalDate requestDate = req.getRequestedTimestamp().toLocalDateTime().toLocalDate();
            return !requestDate.isBefore(effectiveStartDate) && !requestDate.isAfter(effectiveEndDate);
        }).collect(Collectors.toList());

        long totalRequests = filteredRequests.size();
        long completedRequests = filteredRequests.stream()
                .filter(req -> SampleRetrievalRequest.RequestStatus.COMPLETED.equals(req.getStatus())).count();
        long pendingRequests = filteredRequests.stream()
                .filter(req -> SampleRetrievalRequest.RequestStatus.PENDING_APPROVAL.equals(req.getStatus())
                        || SampleRetrievalRequest.RequestStatus.APPROVED.equals(req.getStatus())
                        || SampleRetrievalRequest.RequestStatus.IN_PROGRESS.equals(req.getStatus()))
                .count();
        long rejectedRequests = filteredRequests.stream()
                .filter(req -> SampleRetrievalRequest.RequestStatus.REJECTED.equals(req.getStatus())).count();

        // Aggregate item-level statistics
        long totalItemsRetrieved = 0;
        long returnedItems = 0;
        long consumedItems = 0;
        long overdueReturns = 0;

        for (SampleRetrievalRequest request : filteredRequests) {
            List<SampleRetrievalItem> items = request.getItems();
            totalItemsRetrieved += items.stream()
                    .filter(item -> item.getStatus() != SampleRetrievalItem.ItemStatus.PENDING).count();
            returnedItems += items.stream()
                    .filter(item -> SampleRetrievalItem.ItemStatus.RETURNED.equals(item.getStatus())).count();
            consumedItems += items.stream()
                    .filter(item -> SampleRetrievalItem.ItemStatus.CONSUMED.equals(item.getStatus())).count();
            overdueReturns += items.stream().filter(SampleRetrievalItem::isOverdue).count();
        }

        stats.put("totalRequests", totalRequests);
        stats.put("completedRequests", completedRequests);
        stats.put("pendingRequests", pendingRequests);
        stats.put("rejectedRequests", rejectedRequests);
        stats.put("totalItemsRetrieved", totalItemsRetrieved);
        stats.put("returnedItems", returnedItems);
        stats.put("consumedItems", consumedItems);
        stats.put("overdueReturns", overdueReturns);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDisposalStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        // Get all disposed samples (MVP: count by workflow status)
        List<BioSample> disposedSamples = bioSampleService.getAll().stream()
                .filter(bs -> BioSample.WorkflowStatus.DISPOSED.equals(bs.getWorkflowStatus()))
                .collect(Collectors.toList());

        long totalDisposed = disposedSamples.size();

        // Breakdown by project
        Map<String, Long> disposalsByProject = disposedSamples.stream().filter(bs -> bs.getProjectId() != null)
                .collect(Collectors.groupingBy(BioSample::getProjectId, Collectors.counting()));

        stats.put("totalDisposed", totalDisposed);
        stats.put("disposalsByProject", disposalsByProject);

        // Note: Enhanced disposal tracking (method, reason, approver) available in
        // Phase 4
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTemperatureExcursions(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> excursions = new ArrayList<>();

        // Default date range: last 7 days
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        // Get all temperature logs
        List<NotebookEntryTemperatureLog> tempLogs = temperatureLogService.getAll();

        // Filter by date range and detect excursions (basic threshold: outside -80 to
        // -65°C)
        tempLogs.stream().filter(log -> {
            if (log.getCheckedDateTime() == null) {
                return false;
            }
            LocalDate logDate = log.getCheckedDateTime().toLocalDateTime().toLocalDate();
            return !logDate.isBefore(effectiveStartDate) && !logDate.isAfter(effectiveEndDate);
        }).filter(log -> {
            // Excursion if temperature outside typical -80 to -65°C range
            Double temp = log.getTemperatureValue();
            return temp != null && (temp < -80.0 || temp > -65.0);
        }).forEach(log -> {
            Map<String, Object> excursion = new HashMap<>();
            excursion.put("freezerId", log.getFreezerId());
            excursion.put("excursionTimestamp", log.getCheckedDateTime());
            excursion.put("temperature", log.getTemperatureValue());
            excursion.put("temperatureUnit", log.getTemperatureUnit());
            excursion.put("checkedBy", log.getCheckedBy());
            excursion.put("notes", log.getNotes());
            excursions.add(excursion);
        });

        return excursions;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEnvironmentalComplianceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Temperature compliance
        List<NotebookEntryTemperatureLog> tempLogs = temperatureLogService.getAll();
        long totalTemperatureReadings = tempLogs.size();
        long tempInRange = tempLogs.stream().filter(log -> {
            Double temp = log.getTemperatureValue();
            return temp != null && temp >= -80.0 && temp <= -65.0;
        }).count();
        double temperatureCompliance = totalTemperatureReadings > 0 ? (tempInRange * 100.0 / totalTemperatureReadings)
                : 0.0;

        // Oxygen compliance (alert if < 19.5%)
        List<NotebookEntryRoomEnvironmentLog> envLogs = roomEnvironmentLogService.getAll();
        long totalOxygenReadings = envLogs.stream().filter(log -> log.getOxygenLevel() != null).count();
        long oxygenInRange = envLogs.stream()
                .filter(log -> log.getOxygenLevel() != null && log.getOxygenLevel() >= 19.5).count();
        double oxygenCompliance = totalOxygenReadings > 0 ? (oxygenInRange * 100.0 / totalOxygenReadings) : 0.0;

        // Humidity compliance (optimal 30-60%)
        long totalHumidityReadings = envLogs.stream().filter(log -> log.getHumidity() != null).count();
        long humidityInRange = envLogs.stream()
                .filter(log -> log.getHumidity() != null && log.getHumidity() >= 30.0 && log.getHumidity() <= 60.0)
                .count();
        double humidityCompliance = totalHumidityReadings > 0 ? (humidityInRange * 100.0 / totalHumidityReadings) : 0.0;

        metrics.put("temperatureCompliance", temperatureCompliance);
        metrics.put("oxygenCompliance", oxygenCompliance);
        metrics.put("humidityCompliance", humidityCompliance);
        metrics.put("totalTemperatureReadings", totalTemperatureReadings);
        metrics.put("totalOxygenReadings", totalOxygenReadings);
        metrics.put("totalHumidityReadings", totalHumidityReadings);

        return metrics;
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Calculate pass rate for a specific QC checkpoint.
     */
    private double calculateCheckpointPassRate(List<BiorepositoryQCInspection> inspections,
            java.util.function.Predicate<BiorepositoryQCInspection> checkpointPredicate) {

        if (inspections.isEmpty()) {
            return 0.0;
        }

        long passed = inspections.stream().filter(checkpointPredicate).count();

        return (passed * 100.0 / inspections.size());
    }
}
