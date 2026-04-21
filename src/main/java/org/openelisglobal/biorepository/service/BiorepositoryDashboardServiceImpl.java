package org.openelisglobal.biorepository.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService;
import org.openelisglobal.notebook.service.NotebookEntryTemperatureLogService;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;
import org.openelisglobal.notebook.valueholder.NotebookEntryTemperatureLog;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
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

        @Autowired
        private SampleStorageService storageService;

    @Autowired
    private StorageLocationService storageLocationService;

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
        Map<String, Object> utilizationSummary = buildStorageUtilizationSummary();
        metrics.put("totalDevices", utilizationSummary.get("totalDevices"));
        metrics.put("averageUtilization", utilizationSummary.get("averageUtilization"));
        metrics.put("capacityDefinedDevices", utilizationSummary.get("capacityDefinedDevices"));
        metrics.put("capacityUndefinedDevices", utilizationSummary.get("capacityUndefinedDevices"));
        metrics.put("totalConfiguredCapacity", utilizationSummary.get("totalConfiguredCapacity"));
        metrics.put("totalCurrentUsage", utilizationSummary.get("totalCurrentUsage"));

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStorageUtilizationByDevice() {
        return buildStorageUtilizationSummary();
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

        // Calculate average age using collection date first, then received timestamp.
        double averageAgeYears = activeSamples.stream().map(this::resolveSampleReferenceDate).filter(date -> date != null)
                .mapToLong(referenceDate -> Math.max(0, ChronoUnit.DAYS.between(referenceDate, today))).average()
                .orElse(0.0) / 365.0;

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
                metrics.put("failCount", failedInspections);
        metrics.put("complianceRate", complianceRate);
                metrics.put("passRate", complianceRate);
        metrics.put("samplePresentPassRate", samplePresentPassRate);
        metrics.put("labelIntegrityPassRate", labelIntegrityPassRate);
        metrics.put("containerIntegrityPassRate", containerIntegrityPassRate);
        metrics.put("volumeAcceptablePassRate", volumeAcceptablePassRate);
        metrics.put("correctPositionPassRate", correctPositionPassRate);

                metrics.put("failTrend", buildDailyFailTrend(inspections, 30));

                Map<String, Object> failTrendBasis = new HashMap<>();
                failTrendBasis.put("source", "biorepository_qc_inspection.inspection_date");
                failTrendBasis.put("granularity", "day");
                failTrendBasis.put("windowDays", 30);
                failTrendBasis.put("failureCriteria", BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.name());
                failTrendBasis.put("completedCheckCriteria", "persisted QC inspection records");
                metrics.put("failTrendBasis", failTrendBasis);

        metrics.put("volumeAppearancePassRate", volumeAcceptablePassRate);

        QcEscalationSnapshot snapshot = buildQcEscalationSnapshot(inspections);
        metrics.put("breakdownByFreezer", snapshot.breakdownByFreezer);
        metrics.put("breakdownByRack", snapshot.breakdownByRack);
        metrics.put("breakdownByTechnician", snapshot.breakdownByTechnician);
        metrics.put("underInvestigationBoxes", snapshot.underInvestigationBoxes);
        metrics.put("frequentlyProblematicLocations", snapshot.frequentlyProblematicLocations);
        metrics.put("escalationSignals", snapshot.escalationSignals);

        Map<String, Object> failureResolution = buildFailureResolutionSummary(inspections);
        metrics.put("failureResolutionSummary", failureResolution);
        metrics.put("failedCorrected", failureResolution.get("failedCorrected"));
        metrics.put("failedPendingCorrection", failureResolution.get("failedPendingCorrection"));
        metrics.put("failedMarkedMissing", failureResolution.get("failedMarkedMissing"));

        return metrics;
    }

        @Override
        @Transactional(readOnly = true)
        public Map<String, Object> getQCHistory(Integer limit) {
                int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 500));

                List<BiorepositoryQCInspection> inspections = qcInspectionService.getAll();
                inspections.sort(Comparator.comparing(BiorepositoryQCInspection::getInspectionDate,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

                QcEscalationSnapshot snapshot = buildQcEscalationSnapshot(inspections);
                List<Map<String, Object>> items = inspections.stream().limit(safeLimit)
                                .map(inspection -> toQCHistoryItem(inspection, snapshot)).collect(Collectors.toList());

                Map<String, Object> result = new HashMap<>();
                result.put("source", "biorepository_qc_inspection");
                result.put("recordType", "completed_qc_checks");
                result.put("count", items.size());
                result.put("items", items);
                result.put("failureResolutionSummary", buildFailureResolutionSummary(inspections));
                result.put("escalationSignals", snapshot.escalationSignals);
                return result;
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

        private List<Map<String, Object>> buildDailyFailTrend(List<BiorepositoryQCInspection> inspections,
                        int windowDays) {
                LocalDate today = LocalDate.now();
                LocalDate start = today.minusDays(Math.max(windowDays - 1, 0));

                Map<LocalDate, long[]> daily = new HashMap<>();
                for (BiorepositoryQCInspection inspection : inspections) {
                        if (inspection.getInspectionDate() == null) {
                                continue;
                        }
                        LocalDate inspectionDay = inspection.getInspectionDate().toLocalDateTime().toLocalDate();
                        if (inspectionDay.isBefore(start) || inspectionDay.isAfter(today)) {
                                continue;
                        }
                        long[] counts = daily.computeIfAbsent(inspectionDay, k -> new long[2]);
                        counts[0]++;
                        if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                                counts[1]++;
                        }
                }

                List<Map<String, Object>> trend = new ArrayList<>();
                for (int i = 0; i < windowDays; i++) {
                        LocalDate day = start.plusDays(i);
                        long[] counts = daily.getOrDefault(day, new long[2]);
                        long total = counts[0];
                        long failed = counts[1];
                        Map<String, Object> row = new HashMap<>();
                        row.put("date", day.toString());
                        row.put("totalInspections", total);
                        row.put("failedInspections", failed);
                        row.put("passRate", total > 0 ? ((total - failed) * 100.0 / total) : 0.0);
                        trend.add(row);
                }
                return trend;
        }

        private List<Map<String, Object>> buildDimensionBreakdown(List<BiorepositoryQCInspection> inspections,
                        Function<BiorepositoryQCInspection, String> keyExtractor) {
                Map<String, long[]> grouped = new HashMap<>();

                for (BiorepositoryQCInspection inspection : inspections) {
                        String key = normalizeLabel(keyExtractor.apply(inspection));
                        long[] counts = grouped.computeIfAbsent(key, k -> new long[2]);
                        counts[0]++;
                        if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                                counts[1]++;
                        }
                }

                return grouped.entrySet().stream().map(entry -> {
                        long total = entry.getValue()[0];
                        long failed = entry.getValue()[1];
                        Map<String, Object> row = new HashMap<>();
                        row.put("key", entry.getKey());
                        row.put("totalInspections", total);
                        row.put("failedInspections", failed);
                        row.put("passRate", total > 0 ? ((total - failed) * 100.0 / total) : 0.0);
                        return row;
                }).sorted((a, b) -> {
                        long totalA = ((Number) a.get("totalInspections")).longValue();
                        long totalB = ((Number) b.get("totalInspections")).longValue();
                        int totalCompare = Long.compare(totalB, totalA);
                        if (totalCompare != 0) {
                                return totalCompare;
                        }
                        return String.valueOf(a.get("key")).compareToIgnoreCase(String.valueOf(b.get("key")));
                }).collect(Collectors.toList());
        }

        private Map<String, Object> toQCHistoryItem(BiorepositoryQCInspection inspection, QcEscalationSnapshot snapshot) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", inspection.getId());
                item.put("bioSampleId", inspection.getBioSample() != null ? inspection.getBioSample().getId() : null);
                item.put("inspectionDate", inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
                item.put("lastQcDate", inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
                item.put("qcResult", inspection.getQcResult() != null ? inspection.getQcResult().name() : null);
                String qcStatus = BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection);
                item.put("qcStatus", qcStatus);
                item.put("sampleFlag", "VALID".equals(qcStatus) ? "QC_VALID" : "QC_FAILED");
                item.put("qcFailed", !"VALID".equals(qcStatus));
                item.put("lifecycleOutcome", BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection));
                item.put("inspectorName", inspection.getInspectorName());
                item.put("technicianId", inspection.getSysUserId());
                item.put("expectedLocationPath", inspection.getExpectedLocationPath());
                item.put("expectedPositionCoordinate", inspection.getExpectedPositionCoordinate());
                item.put("correctionActionType", inspection.getCorrectionActionType());
                item.put("correctionReason", inspection.getCorrectionReason());
                item.put("correctionByUser", inspection.getCorrectionByUser());
                item.put("correctionTimestamp",
                                inspection.getCorrectionTimestamp() != null ? inspection.getCorrectionTimestamp().toString() : null);
                item.put("discrepancyType",
                                inspection.getDiscrepancyType() != null ? inspection.getDiscrepancyType().name() : null);
                item.put("correctiveAction", inspection.getCorrectiveAction());
                item.put("remarks", inspection.getRemarks());
                item.put("failureComment", inspection.getRemarks());
                item.put("auditTrail", buildQCAuditTrail(inspection));

                String[] levels = parseLocationLevels(inspection.getExpectedLocationPath());
                item.put("freezer", levels[0]);
                item.put("shelf", levels[1]);
                item.put("rack", levels[2]);
                item.put("box", levels[3]);
                String boxKey = normalizeLabel(levels[0]) + " > " + normalizeLabel(levels[1]) + " > " + normalizeLabel(levels[2])
                                + " > " + normalizeLabel(levels[3]);
                item.put("boxFlag", snapshot.underInvestigationBoxKeys.contains(boxKey) ? "UNDER_INVESTIGATION" : "NORMAL");
                item.put("freezerFlag", snapshot.flaggedFreezerKeys.contains(normalizeLabel(levels[0])) ? "QC_FLAGGED" : "NORMAL");
                item.put("escalationTriggered",
                                snapshot.batchFailRateExceeded || snapshot.underInvestigationBoxKeys.contains(boxKey)
                                                || snapshot.flaggedFreezerKeys.contains(normalizeLabel(levels[0]))
                                                || isMissingDiscrepancy(inspection));
                item.put("failureResolution", mapFailureResolutionStatus(inspection));

                return item;
        }

        private String extractFreezerFromPath(BiorepositoryQCInspection inspection) {
                return parseLocationLevels(inspection.getExpectedLocationPath())[0];
        }

        private String extractRackFromPath(BiorepositoryQCInspection inspection) {
                String[] levels = parseLocationLevels(inspection.getExpectedLocationPath());
                if ("Unknown".equals(levels[2])) {
                        return "Unknown";
                }
                return levels[0] + " > " + levels[1] + " > " + levels[2];
        }

        private String extractTechnicianKey(BiorepositoryQCInspection inspection) {
                String inspectorName = trimToNull(inspection.getInspectorName());
                String technicianId = trimToNull(inspection.getSysUserId());

                if (inspectorName != null && technicianId != null) {
                        return inspectorName + " (" + technicianId + ")";
                }
                if (inspectorName != null) {
                        return inspectorName;
                }
                if (technicianId != null) {
                        return technicianId;
                }
                return "Unknown";
        }

        private String[] parseLocationLevels(String locationPath) {
                String[] defaults = new String[] { "Unknown", "Unknown", "Unknown", "Unknown" };
                String raw = trimToNull(locationPath);
                if (raw == null) {
                        return defaults;
                }

                List<String> segments = java.util.Arrays.stream(raw.split("\\s*>\\s*")).map(String::trim)
                                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
                if (segments.isEmpty()) {
                        return defaults;
                }

                int structuralEnd = segments.size();
                String tail = segments.get(structuralEnd - 1);
                if (tail.matches("^[A-Za-z]+\\d+$")) {
                        structuralEnd = structuralEnd - 1;
                }

                List<String> structural = segments.subList(0, Math.max(structuralEnd, 0));
                if (structural.isEmpty()) {
                        return defaults;
                }

                int boxIdx = structural.size() - 1;
                int rackIdx = structural.size() - 2;
                int shelfIdx = structural.size() - 3;
                int freezerIdx = structural.size() - 4;

                return new String[] { freezerIdx >= 0 ? structural.get(freezerIdx) : "Unknown",
                                shelfIdx >= 0 ? structural.get(shelfIdx) : "Unknown",
                                rackIdx >= 0 ? structural.get(rackIdx) : "Unknown",
                                boxIdx >= 0 ? structural.get(boxIdx) : "Unknown" };
        }

        private String trimToNull(String value) {
                if (value == null) {
                        return null;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        private String normalizeLabel(String label) {
                String normalized = trimToNull(label);
                return normalized != null ? normalized : "Unknown";
        }

    private LocalDate resolveSampleReferenceDate(BioSample sample) {
        if (sample == null || sample.getSampleItem() == null) {
            return null;
        }
        if (sample.getSampleItem().getCollectionDate() != null) {
            return sample.getSampleItem().getCollectionDate().toLocalDateTime().toLocalDate();
        }
        if (sample.getSampleItem().getSample() != null && sample.getSampleItem().getSample().getReceivedTimestamp() != null) {
            return sample.getSampleItem().getSample().getReceivedTimestamp().toLocalDateTime().toLocalDate();
        }
        return null;
    }

    private Map<String, Object> buildStorageUtilizationSummary() {
        List<Map<String, Object>> deviceRows = buildDeviceUtilizationRows();
        long totalCurrentUsage = 0;
        long totalConfiguredCapacity = 0;
        int capacityDefinedDevices = 0;
        for (Map<String, Object> row : deviceRows) {
            totalCurrentUsage += ((Number) row.getOrDefault("currentUsage", 0)).longValue();
            long deviceCapacity = ((Number) row.getOrDefault("totalCapacity", 0)).longValue();
            if (deviceCapacity > 0) {
                totalConfiguredCapacity += deviceCapacity;
                capacityDefinedDevices++;
            }
        }

        double averageUtilization = totalConfiguredCapacity > 0 ? (totalCurrentUsage * 100.0 / totalConfiguredCapacity)
                : 0.0;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("devices", deviceRows);
        summary.put("totalDevices", deviceRows.size());
        summary.put("capacityDefinedDevices", capacityDefinedDevices);
        summary.put("capacityUndefinedDevices", Math.max(deviceRows.size() - capacityDefinedDevices, 0));
        summary.put("totalConfiguredCapacity", totalConfiguredCapacity);
        summary.put("totalCurrentUsage", totalCurrentUsage);
        summary.put("averageUtilization", averageUtilization);
        return summary;
    }

    private List<Map<String, Object>> buildDeviceUtilizationRows() {
        List<StorageDevice> devices = storageLocationService.getAllDevices().stream().filter(this::isActive).toList();
        List<StorageShelf> shelves = storageLocationService.getAllShelves().stream().filter(this::isActive).toList();
        List<StorageRack> racks = storageLocationService.getAllRacks().stream().filter(this::isActive).toList();
        List<StorageBox> boxes = storageLocationService.getAllBoxes().stream().filter(this::isActive).toList();
        Set<Integer> activeShelfIds = shelves.stream().map(StorageShelf::getId).filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Integer, Integer> rackToDevice = new HashMap<>();
        for (StorageRack rack : racks) {
            StorageShelf shelf = rack.getParentShelf();
            StorageDevice device = shelf != null ? shelf.getParentDevice() : null;
            if (rack.getId() != null && shelf != null && activeShelfIds.contains(shelf.getId()) && device != null
                    && device.getId() != null) {
                rackToDevice.put(rack.getId(), device.getId());
            }
        }

        Map<Integer, Long> derivedCapacityByDevice = new HashMap<>();
        for (StorageBox box : boxes) {
            StorageRack parentRack = box.getParentRack();
            Integer rackId = parentRack != null ? parentRack.getId() : null;
            Integer deviceId = rackId != null ? rackToDevice.get(rackId) : null;
            if (deviceId == null) {
                continue;
            }
            long boxCapacity = box.getCapacity() != null ? box.getCapacity().longValue() : 0L;
            if (boxCapacity <= 0) {
                continue;
            }
            derivedCapacityByDevice.merge(deviceId, boxCapacity, Long::sum);
        }

        return devices.stream().map(device -> {
            long currentUsage = 0L;
            if (device.getId() != null) {
                currentUsage = storageLocationService.countOccupiedInDevice(device.getId());
            }
            long configuredDeviceCapacity = device.getCapacityLimit() != null ? device.getCapacityLimit().longValue() : 0L;
            long derivedCapacity = device.getId() != null ? derivedCapacityByDevice.getOrDefault(device.getId(), 0L) : 0L;
            long totalCapacity = configuredDeviceCapacity > 0 ? configuredDeviceCapacity : derivedCapacity;
            String capacitySource = configuredDeviceCapacity > 0 ? "DEVICE_CAPACITY_LIMIT"
                    : (derivedCapacity > 0 ? "BOX_GRID_SUM" : "UNDEFINED");
            double utilizationPercent = totalCapacity > 0 ? (currentUsage * 100.0 / totalCapacity) : 0.0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deviceId", device.getId());
            row.put("deviceName", trimToNull(device.getName()) != null ? device.getName() : device.getCode());
            row.put("deviceCode", device.getCode());
            row.put("deviceType", device.getType());
            row.put("active", Boolean.TRUE.equals(device.getActive()));
            row.put("currentUsage", currentUsage);
            row.put("totalCapacity", totalCapacity);
            row.put("capacitySource", capacitySource);
            row.put("utilizationPercent", utilizationPercent);
            return row;
        }).sorted((a, b) -> String.valueOf(a.getOrDefault("deviceName", ""))
                .compareToIgnoreCase(String.valueOf(b.getOrDefault("deviceName", "")))).collect(Collectors.toList());
    }

    private boolean isActive(StorageDevice device) {
        return device != null && Boolean.TRUE.equals(device.getActive());
    }

    private boolean isActive(StorageShelf shelf) {
        return shelf != null && Boolean.TRUE.equals(shelf.getActive());
    }

    private boolean isActive(StorageRack rack) {
        return rack != null && Boolean.TRUE.equals(rack.getActive());
    }

    private boolean isActive(StorageBox box) {
        return box != null && Boolean.TRUE.equals(box.getActive());
    }

        private List<Map<String, Object>> buildUnderInvestigationBoxes(List<BiorepositoryQCInspection> inspections) {
                Map<String, long[]> grouped = new HashMap<>();

                for (BiorepositoryQCInspection inspection : inspections) {
                        String[] levels = parseLocationLevels(inspection.getExpectedLocationPath());
                        String boxKey = normalizeLabel(levels[0]) + " > " + normalizeLabel(levels[1]) + " > "
                                        + normalizeLabel(levels[2]) + " > " + normalizeLabel(levels[3]);
                        long[] counts = grouped.computeIfAbsent(boxKey, k -> new long[2]);
                        counts[0]++;
                        if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                                counts[1]++;
                        }
                }

                return grouped.entrySet().stream().filter(entry -> entry.getValue()[1] >= 2).map(entry -> {
                        long total = entry.getValue()[0];
                        long failed = entry.getValue()[1];
                        Map<String, Object> row = new HashMap<>();
                        row.put("key", entry.getKey());
                        row.put("totalInspections", total);
                        row.put("failedInspections", failed);
                        row.put("failRate", total > 0 ? (failed * 100.0 / total) : 0.0);
                        row.put("investigationFlag", true);
                        return row;
                }).sorted((a, b) -> {
                        long failedA = ((Number) a.get("failedInspections")).longValue();
                        long failedB = ((Number) b.get("failedInspections")).longValue();
                        int failCompare = Long.compare(failedB, failedA);
                        if (failCompare != 0) {
                                return failCompare;
                        }
                        return String.valueOf(a.get("key")).compareToIgnoreCase(String.valueOf(b.get("key")));
                }).collect(Collectors.toList());
        }

        private List<Map<String, Object>> buildFrequentlyProblematicLocations(List<Map<String, Object>> boxCandidates,
                        List<Map<String, Object>> rackBreakdown) {
                List<Map<String, Object>> rows = new ArrayList<>();

                boxCandidates.stream().limit(10).forEach(box -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("locationType", "BOX");
                        row.put("key", box.get("key"));
                        row.put("failedInspections", box.get("failedInspections"));
                        row.put("totalInspections", box.get("totalInspections"));
                        row.put("failRate", box.get("failRate"));
                        rows.add(row);
                });

                if (rows.size() < 10) {
                        rackBreakdown.stream()
                                        .filter(row -> ((Number) row.getOrDefault("failedInspections", 0)).longValue() > 0)
                                        .limit(10 - rows.size()).forEach(rack -> {
                                                Map<String, Object> row = new HashMap<>();
                                                row.put("locationType", "RACK");
                                                row.put("key", rack.get("key"));
                                                row.put("failedInspections", rack.get("failedInspections"));
                                                row.put("totalInspections", rack.get("totalInspections"));
                                                row.put("failRate", rack.get("passRate") != null
                                                                ? (100.0 - ((Number) rack.get("passRate")).doubleValue())
                                                                : 0.0);
                                                rows.add(row);
                                        });
                }

                return rows;
        }

        private Map<String, Object> buildEscalationSignals(List<BiorepositoryQCInspection> inspections,
                        List<Map<String, Object>> breakdownByFreezer, List<Map<String, Object>> breakdownByRack,
                        List<Map<String, Object>> underInvestigationBoxes) {
                Map<String, Object> signals = new HashMap<>();

                long totalInspections = inspections.size();
                long failedInspections = inspections.stream()
                                .filter(insp -> BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND
                                                .equals(insp.getQcResult()))
                                .count();
                double batchFailRate = totalInspections > 0 ? (failedInspections * 100.0 / totalInspections) : 0.0;
                boolean batchFailRateExceeded = totalInspections > 0 && batchFailRate > 5.0;

                long repeatedRacks = breakdownByRack.stream()
                                .filter(row -> ((Number) row.getOrDefault("failedInspections", 0)).longValue() >= 2)
                                .count();
                boolean repeatedFailureInSameBoxOrRack = !underInvestigationBoxes.isEmpty() || repeatedRacks > 0;

                long criticalMissingSamples = inspections.stream()
                                .filter(insp -> BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND
                                                .equals(insp.getQcResult()))
                                .filter(this::isMissingDiscrepancy)
                                .count();

                List<Map<String, Object>> flaggedFreezers = breakdownByFreezer.stream().filter(row -> {
                        long failed = ((Number) row.getOrDefault("failedInspections", 0)).longValue();
                        double passRate = ((Number) row.getOrDefault("passRate", 0.0)).doubleValue();
                        double failRate = 100.0 - passRate;
                        return failed > 0 && failRate > 5.0;
                }).map(row -> {
                        Map<String, Object> freezer = new HashMap<>();
                        freezer.put("key", row.get("key"));
                        freezer.put("failedInspections", row.get("failedInspections"));
                        freezer.put("totalInspections", row.get("totalInspections"));
                        freezer.put("failRate", 100.0 - ((Number) row.getOrDefault("passRate", 0.0)).doubleValue());
                        freezer.put("thresholdPercent", 5.0);
                        return freezer;
                }).collect(Collectors.toList());

                List<String> triggeredRules = new ArrayList<>();
                if (batchFailRateExceeded) {
                        triggeredRules.add("BATCH_FAIL_RATE_OVER_5_PERCENT");
                }
                if (repeatedFailureInSameBoxOrRack) {
                        triggeredRules.add("REPEATED_FAILURE_IN_SAME_BOX_OR_RACK");
                }
                if (criticalMissingSamples > 0) {
                        triggeredRules.add("CRITICAL_SAMPLES_MISSING");
                }

                List<String> recommendedActions = new ArrayList<>();
                if (!triggeredRules.isEmpty()) {
                        recommendedActions.add("Notify supervisor");
                }
                if (repeatedFailureInSameBoxOrRack) {
                        recommendedActions.add("Trigger full box audit or targeted rack review");
                }
                if (!flaggedFreezers.isEmpty()) {
                        recommendedActions.add("Trigger freezer-wide QC review for flagged freezers");
                }

                signals.put("totalInspections", totalInspections);
                signals.put("failedInspections", failedInspections);
                signals.put("batchFailRatePercent", batchFailRate);
                signals.put("batchFailRateThresholdPercent", 5.0);
                signals.put("batchFailRateExceeded", batchFailRateExceeded);
                signals.put("repeatedFailureInSameBoxOrRack", repeatedFailureInSameBoxOrRack);
                signals.put("repeatedFailureBoxesCount", underInvestigationBoxes.size());
                signals.put("repeatedFailureRacksCount", repeatedRacks);
                signals.put("criticalMissingSamples", criticalMissingSamples);
                signals.put("criticalSamplesMissing", criticalMissingSamples > 0);
                signals.put("flaggedFreezers", flaggedFreezers);
                signals.put("triggeredRules", triggeredRules);
                signals.put("recommendedActions", recommendedActions);

                return signals;
        }

        private boolean isMissingDiscrepancy(BiorepositoryQCInspection inspection) {
                if (inspection == null || inspection.getDiscrepancyType() == null) {
                        return false;
                }
                return BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.equals(inspection.getDiscrepancyType())
                                || BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE
                                                .equals(inspection.getDiscrepancyType());
        }

        private Map<String, Object> buildQCAuditTrail(BiorepositoryQCInspection inspection) {
                Map<String, Object> auditTrail = new HashMap<>();

                String oldCoordinate = trimToNull(inspection.getCorrectionOldCoordinate());
                if (oldCoordinate == null) {
                        oldCoordinate = composeCoordinate(inspection.getExpectedLocationPath(),
                                        inspection.getExpectedPositionCoordinate());
                }
                String newCoordinate = trimToNull(inspection.getCorrectionNewCoordinate());
                if (newCoordinate == null) {
                        newCoordinate = oldCoordinate;
                }
                String auditUser = trimToNull(inspection.getCorrectionByUser()) != null ? inspection.getCorrectionByUser()
                                : inspection.getSysUserId();
                String auditTimestamp = inspection.getCorrectionTimestamp() != null
                                ? inspection.getCorrectionTimestamp().toString()
                                : (inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
                String reason = trimToNull(inspection.getCorrectionReason()) != null ? inspection.getCorrectionReason()
                                : trimToNull(inspection.getCorrectiveAction());
                boolean correctionApplied = BiorepositoryQcOutcomeDerivation.hasAppliedCorrectionWorkflow(inspection);

                if ("MISSING".equals(BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection))
                                && trimToNull(inspection.getCorrectionNewCoordinate()) == null) {
                        newCoordinate = "Missing (not found during QC)";
                } else if (!correctionApplied
                                && BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                        SampleItem sampleItem = inspection.getBioSample() != null ? inspection.getBioSample().getSampleItem()
                                        : null;
                        if (sampleItem != null && sampleItem.getId() != null) {
                                Map<String, Object> currentLocation = storageService
                                                .getSampleItemLocation(sampleItem.getId().toString());
                                String currentCoordinate = composeCoordinate(asString(currentLocation.get("hierarchicalPath")),
                                                asString(currentLocation.get("positionCoordinate")));
                                if (trimToNull(currentCoordinate) != null) {
                                        newCoordinate = currentCoordinate;
                                }
                        }
                }

                auditTrail.put("oldCoordinate", oldCoordinate);
                auditTrail.put("newCoordinate", newCoordinate);
                auditTrail.put("fromCoordinates", oldCoordinate);
                auditTrail.put("toCoordinates", newCoordinate);
                auditTrail.put("user", auditUser);
                auditTrail.put("correctedBy", auditUser);
                auditTrail.put("timestamp", auditTimestamp);
                auditTrail.put("correctedAt", auditTimestamp);
                auditTrail.put("reason", reason);
                return auditTrail;
        }

        private String composeCoordinate(String locationPath, String positionCoordinate) {
                String path = trimToNull(locationPath);
                String coordinate = trimToNull(positionCoordinate);
                if (path == null && coordinate == null) {
                        return null;
                }
                if (path == null) {
                        return coordinate;
                }
                if (coordinate == null) {
                        return path;
                }
                return path + " > " + coordinate;
        }

        private String asString(Object value) {
                if (value == null) {
                        return null;
                }
                return String.valueOf(value);
        }

        private String mapFailureResolutionStatus(BiorepositoryQCInspection inspection) {
                String lifecycleOutcome = BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection);
                if ("FAILED_CORRECTED".equals(lifecycleOutcome) || "FAILED_MARKED_MISSING".equals(lifecycleOutcome)) {
                        return "RESOLVED";
                }
                if ("FAILED_PENDING_CORRECTION".equals(lifecycleOutcome)) {
                        return "UNRESOLVED";
                }
                return "N/A";
        }

        private Map<String, Object> buildFailureResolutionSummary(List<BiorepositoryQCInspection> inspections) {
                long failedCorrected = 0;
                long failedPendingCorrection = 0;
                long failedMarkedMissing = 0;
                for (BiorepositoryQCInspection inspection : inspections) {
                        String outcome = BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection);
                        if ("FAILED_CORRECTED".equals(outcome)) {
                                failedCorrected++;
                        } else if ("FAILED_PENDING_CORRECTION".equals(outcome)) {
                                failedPendingCorrection++;
                        } else if ("FAILED_MARKED_MISSING".equals(outcome)) {
                                failedMarkedMissing++;
                        }
                }
                Map<String, Object> summary = new HashMap<>();
                summary.put("failedCorrected", failedCorrected);
                summary.put("failedPendingCorrection", failedPendingCorrection);
                summary.put("failedMarkedMissing", failedMarkedMissing);
                summary.put("correctedVsUnresolved", Map.of("corrected", failedCorrected + failedMarkedMissing, "unresolved",
                                failedPendingCorrection));
                return summary;
        }

        private QcEscalationSnapshot buildQcEscalationSnapshot(List<BiorepositoryQCInspection> inspections) {
                List<Map<String, Object>> breakdownByFreezer = buildDimensionBreakdown(inspections, this::extractFreezerFromPath);
                List<Map<String, Object>> breakdownByRack = buildDimensionBreakdown(inspections, this::extractRackFromPath);
                List<Map<String, Object>> breakdownByTechnician = buildDimensionBreakdown(inspections, this::extractTechnicianKey);
                List<Map<String, Object>> underInvestigationBoxes = buildUnderInvestigationBoxes(inspections);
                List<Map<String, Object>> frequentlyProblematicLocations = buildFrequentlyProblematicLocations(
                                underInvestigationBoxes, breakdownByRack);
                Map<String, Object> escalationSignals = buildEscalationSignals(inspections, breakdownByFreezer, breakdownByRack,
                                underInvestigationBoxes);

                List<Map<String, Object>> flaggedFreezers = escalationSignals.get("flaggedFreezers") instanceof List
                                ? castMapList(escalationSignals.get("flaggedFreezers"))
                                : List.of();
                double batchFailRatePercent = ((Number) escalationSignals.getOrDefault("batchFailRatePercent", 0.0))
                                .doubleValue();
                boolean batchFailRateExceeded = Boolean.TRUE.equals(escalationSignals.get("batchFailRateExceeded"));

                return new QcEscalationSnapshot(breakdownByFreezer, breakdownByRack, breakdownByTechnician,
                                underInvestigationBoxes, frequentlyProblematicLocations, escalationSignals,
                                toKeySet(underInvestigationBoxes), toKeySet(flaggedFreezers), batchFailRatePercent,
                                batchFailRateExceeded);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> castMapList(Object value) {
                return (List<Map<String, Object>>) value;
        }

        private java.util.Set<String> toKeySet(List<Map<String, Object>> rows) {
                return rows.stream().map(row -> normalizeLabel(asString(row.get("key")))).collect(Collectors.toSet());
        }

        private static class QcEscalationSnapshot {
                private final List<Map<String, Object>> breakdownByFreezer;
                private final List<Map<String, Object>> breakdownByRack;
                private final List<Map<String, Object>> breakdownByTechnician;
                private final List<Map<String, Object>> underInvestigationBoxes;
                private final List<Map<String, Object>> frequentlyProblematicLocations;
                private final Map<String, Object> escalationSignals;
                private final java.util.Set<String> underInvestigationBoxKeys;
                private final java.util.Set<String> flaggedFreezerKeys;
                private final double batchFailRatePercent;
                private final boolean batchFailRateExceeded;

                private QcEscalationSnapshot(List<Map<String, Object>> breakdownByFreezer,
                                List<Map<String, Object>> breakdownByRack, List<Map<String, Object>> breakdownByTechnician,
                                List<Map<String, Object>> underInvestigationBoxes,
                                List<Map<String, Object>> frequentlyProblematicLocations,
                                Map<String, Object> escalationSignals, java.util.Set<String> underInvestigationBoxKeys,
                                java.util.Set<String> flaggedFreezerKeys, double batchFailRatePercent,
                                boolean batchFailRateExceeded) {
                        this.breakdownByFreezer = breakdownByFreezer;
                        this.breakdownByRack = breakdownByRack;
                        this.breakdownByTechnician = breakdownByTechnician;
                        this.underInvestigationBoxes = underInvestigationBoxes;
                        this.frequentlyProblematicLocations = frequentlyProblematicLocations;
                        this.escalationSignals = escalationSignals;
                        this.underInvestigationBoxKeys = underInvestigationBoxKeys;
                        this.flaggedFreezerKeys = flaggedFreezerKeys;
                        this.batchFailRatePercent = batchFailRatePercent;
                        this.batchFailRateExceeded = batchFailRateExceeded;
                }
        }
}
