package org.openelisglobal.biorepository.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

                                List<Map<String, Object>> breakdownByFreezer = buildDimensionBreakdown(inspections,
                                                                this::extractFreezerFromPath);
                                List<Map<String, Object>> breakdownByRack = buildDimensionBreakdown(inspections, this::extractRackFromPath);
                                List<Map<String, Object>> breakdownByTechnician = buildDimensionBreakdown(inspections,
                                                                this::extractTechnicianKey);

                                metrics.put("breakdownByFreezer", breakdownByFreezer);
                                metrics.put("breakdownByRack", breakdownByRack);
                                metrics.put("breakdownByTechnician", breakdownByTechnician);

                                List<Map<String, Object>> underInvestigationBoxes = buildUnderInvestigationBoxes(inspections);
                                List<Map<String, Object>> frequentlyProblematicLocations = buildFrequentlyProblematicLocations(
                                                                underInvestigationBoxes, breakdownByRack);

                                metrics.put("failureResolutionSummary", buildFailureResolutionSummary(inspections));
                                metrics.put("underInvestigationBoxes", underInvestigationBoxes);
                                metrics.put("frequentlyProblematicLocations", frequentlyProblematicLocations);
                                metrics.put("escalationSignals",
                                                                buildEscalationSignals(inspections, breakdownByFreezer, breakdownByRack,
                                                                                                underInvestigationBoxes));

        return metrics;
    }

        @Override
        @Transactional(readOnly = true)
        public Map<String, Object> getQCHistory(Integer limit) {
                int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 500));

                List<BiorepositoryQCInspection> inspections = qcInspectionService.getAll();
                inspections.sort(Comparator.comparing(BiorepositoryQCInspection::getInspectionDate,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

                List<Map<String, Object>> items = inspections.stream().limit(safeLimit).map(this::toQCHistoryItem)
                                .collect(Collectors.toList());
                decorateQCHistoryFlags(items, inspections);

                Map<String, Object> result = new HashMap<>();
                result.put("source", "biorepository_qc_inspection");
                result.put("recordType", "completed_qc_checks");
                result.put("count", items.size());
                result.put("items", items);
                result.put("failureResolutionSummary", buildFailureResolutionSummary(inspections));
                return result;
        }

        private void decorateQCHistoryFlags(List<Map<String, Object>> items,
                        List<BiorepositoryQCInspection> allInspections) {
                List<Map<String, Object>> underInvestigationBoxes = buildUnderInvestigationBoxes(allInspections);
                List<Map<String, Object>> breakdownByFreezer = buildDimensionBreakdown(allInspections,
                                this::extractFreezerFromPath);
                List<Map<String, Object>> breakdownByRack = buildDimensionBreakdown(allInspections, this::extractRackFromPath);
                Map<String, Object> escalationSignals = buildEscalationSignals(allInspections, breakdownByFreezer,
                                breakdownByRack, underInvestigationBoxes);

                java.util.Set<String> underInvestigationBoxKeys = underInvestigationBoxes.stream()
                                .map(row -> String.valueOf(row.get("key"))).collect(Collectors.toSet());
                @SuppressWarnings("unchecked")
                java.util.Set<String> flaggedFreezerKeys = ((List<Map<String, Object>>) escalationSignals
                                .getOrDefault("flaggedFreezers", List.of())).stream()
                                                .map(row -> String.valueOf(row.get("key"))).collect(Collectors.toSet());

                for (Map<String, Object> item : items) {
                        boolean boxUnderInvestigation = underInvestigationBoxKeys.contains(String.valueOf(item.get("boxKey")));
                        boolean freezerFlagged = flaggedFreezerKeys.contains(String.valueOf(item.get("freezer")));
                        if (boxUnderInvestigation) {
                                item.put("boxFlag", "UNDER_INVESTIGATION");
                        }
                        if (freezerFlagged) {
                                item.put("freezerFlag", "THRESHOLD_EXCEEDED");
                        }
                        item.put("escalationTriggered", boxUnderInvestigation || freezerFlagged
                                        || Boolean.TRUE.equals(escalationSignals.get("criticalSamplesMissing"))
                                        || Boolean.TRUE.equals(escalationSignals.get("batchFailRateExceeded")));
                }
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

        private Map<String, Object> toQCHistoryItem(BiorepositoryQCInspection inspection) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", inspection.getId());
                item.put("bioSampleId", inspection.getBioSample() != null ? inspection.getBioSample().getId() : null);
                item.put("qcBatchId", inspection.getQcBatchId());
                item.put("inspectionDate", inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
                item.put("lastQcDate", inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
                item.put("qcResult", inspection.getQcResult() != null ? inspection.getQcResult().name() : null);
                String qcStatus = deriveQcStatus(inspection);
                item.put("qcStatus", qcStatus);
                item.put("sampleFlag", "VALID".equals(qcStatus) ? "QC_VALID" : "QC_FAILED");
                item.put("qcFailed", !"VALID".equals(qcStatus));
                item.put("lifecycleOutcome", deriveLifecycleOutcome(inspection));
                item.put("inspectorName", inspection.getInspectorName());
                item.put("technicianId", inspection.getSysUserId());
                item.put("expectedLocationPath", inspection.getExpectedLocationPath());
                item.put("expectedPositionCoordinate", inspection.getExpectedPositionCoordinate());
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
                String boxKey = normalizeLabel(levels[0]) + " > " + normalizeLabel(levels[1]) + " > "
                                + normalizeLabel(levels[2]) + " > " + normalizeLabel(levels[3]);
                item.put("boxKey", boxKey);
                item.put("failureResolution", toFailureResolution(inspection));

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

        private Map<String, Object> buildFailureResolutionSummary(List<BiorepositoryQCInspection> inspections) {
                long failedPendingCorrection = inspections.stream()
                                .filter(insp -> "FAILED_PENDING_CORRECTION".equals(deriveLifecycleOutcome(insp))).count();
                long failedCorrected = inspections.stream()
                                .filter(insp -> "FAILED_CORRECTED".equals(deriveLifecycleOutcome(insp))).count();
                long failedMarkedMissing = inspections.stream()
                                .filter(insp -> "FAILED_MARKED_MISSING".equals(deriveLifecycleOutcome(insp))).count();

                Map<String, Object> correctedVsUnresolved = new HashMap<>();
                correctedVsUnresolved.put("corrected", failedCorrected + failedMarkedMissing);
                correctedVsUnresolved.put("unresolved", failedPendingCorrection);

                Map<String, Object> summary = new HashMap<>();
                summary.put("failedPendingCorrection", failedPendingCorrection);
                summary.put("failedCorrected", failedCorrected);
                summary.put("failedMarkedMissing", failedMarkedMissing);
                summary.put("correctedVsUnresolved", correctedVsUnresolved);
                return summary;
        }

        private String toFailureResolution(BiorepositoryQCInspection inspection) {
                String lifecycle = deriveLifecycleOutcome(inspection);
                if ("FAILED_CORRECTED".equals(lifecycle) || "FAILED_MARKED_MISSING".equals(lifecycle)) {
                        return "RESOLVED";
                }
                if ("FAILED_PENDING_CORRECTION".equals(lifecycle)) {
                        return "UNRESOLVED";
                }
                return "NOT_REQUIRED";
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
                boolean batchFailRateExceeded = totalInspections > 0 && batchFailRate >= 5.0;

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
                        return failed > 0 && failRate >= 5.0;
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
                signals.put("supervisorNotificationRequired", !triggeredRules.isEmpty());
                signals.put("supervisorNotificationMessage",
                                buildSupervisorNotificationMessage(batchFailRate, batchFailRateExceeded,
                                                repeatedFailureInSameBoxOrRack, criticalMissingSamples, flaggedFreezers));

                return signals;
        }

        private String buildSupervisorNotificationMessage(double batchFailRate, boolean batchFailRateExceeded,
                        boolean repeatedFailureInSameBoxOrRack, long criticalMissingSamples,
                        List<Map<String, Object>> flaggedFreezers) {
                List<String> reasons = new ArrayList<>();
                if (batchFailRateExceeded) {
                        reasons.add(String.format("QC batch fail rate is %.1f%% (threshold 5.0%%)", batchFailRate));
                }
                if (repeatedFailureInSameBoxOrRack) {
                        reasons.add("Repeated failure detected in the same box/rack");
                }
                if (criticalMissingSamples > 0) {
                        reasons.add(String.format("Critical samples missing: %d", criticalMissingSamples));
                }

                if (reasons.isEmpty()) {
                        return null;
                }

                StringBuilder message = new StringBuilder();
                message.append("Biorepository QC escalation alert: ");
                message.append(String.join("; ", reasons));
                if (flaggedFreezers != null && !flaggedFreezers.isEmpty()) {
                        String freezerList = flaggedFreezers.stream()
                                        .map(row -> String.valueOf(row.get("key")))
                                        .filter(value -> value != null && !"null".equalsIgnoreCase(value))
                                        .collect(Collectors.joining(", "));
                        if (!freezerList.isEmpty()) {
                                message.append(". Flagged freezers: ").append(freezerList);
                        }
                }
                message.append(".");
                return message.toString();
        }

        private String deriveQcStatus(BiorepositoryQCInspection inspection) {
                if (inspection == null || inspection.getQcResult() == null) {
                        return "UNKNOWN";
                }
                if (BiorepositoryQCInspection.QCResult.VERIFIED.equals(inspection.getQcResult())) {
                        return "VALID";
                }
                if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                        if (isMissingDiscrepancy(inspection) && isMarkedMissingAction(inspection)) {
                                return "MISSING";
                        }
                        return "QC_FAILED";
                }
                return "UNKNOWN";
        }

        private String deriveLifecycleOutcome(BiorepositoryQCInspection inspection) {
                if (inspection == null || inspection.getQcResult() == null) {
                        return "UNKNOWN";
                }
                if (BiorepositoryQCInspection.QCResult.VERIFIED.equals(inspection.getQcResult())) {
                        return "PASSED";
                }
                if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                        if ("MISSING".equals(deriveQcStatus(inspection))) {
                                return "FAILED_MARKED_MISSING";
                        }
                        if (trimToNull(inspection.getCorrectionActionType()) != null) {
                                return "FAILED_CORRECTED";
                        }
                        return "FAILED_PENDING_CORRECTION";
                }
                return "UNKNOWN";
        }

        private boolean isMissingDiscrepancy(BiorepositoryQCInspection inspection) {
                if (inspection == null || inspection.getDiscrepancyType() == null) {
                        return false;
                }
                return BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.equals(inspection.getDiscrepancyType())
                                || BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE
                                                .equals(inspection.getDiscrepancyType());
        }

        private boolean isMarkedMissingAction(BiorepositoryQCInspection inspection) {
                String correctionActionType = trimToNull(inspection.getCorrectionActionType());
                return correctionActionType != null && correctionActionType.equalsIgnoreCase("MARK_MISSING");
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

                boolean hasPersistedNewCoordinate = trimToNull(inspection.getCorrectionNewCoordinate()) != null;
                if ("MISSING".equals(deriveQcStatus(inspection))) {
                        newCoordinate = "Missing (not found during QC)";
                } else if (!hasPersistedNewCoordinate
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
                String auditUser = trimToNull(inspection.getCorrectionByUser()) != null
                                ? trimToNull(inspection.getCorrectionByUser())
                                : inspection.getSysUserId();
                String auditTimestamp = inspection.getCorrectionTimestamp() != null
                                ? inspection.getCorrectionTimestamp().toString()
                                : inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null;
                auditTrail.put("user", auditUser);
                auditTrail.put("correctedBy", auditUser);
                auditTrail.put("timestamp", auditTimestamp);
                auditTrail.put("correctedAt", auditTimestamp);
                auditTrail.put("reason", trimToNull(inspection.getCorrectionReason()) != null
                                ? trimToNull(inspection.getCorrectionReason())
                                : trimToNull(inspection.getCorrectiveAction()));
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
}
