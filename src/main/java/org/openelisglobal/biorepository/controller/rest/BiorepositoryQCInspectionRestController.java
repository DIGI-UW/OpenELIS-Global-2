package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryQcRoundGenerationService;
import org.openelisglobal.biorepository.service.BiorepositoryQcSamplePoolService;
import org.openelisglobal.biorepository.service.BiorepositoryQCInspectionService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for Biorepository QC Inspection operations.
 *
 * Provides endpoints for: - Fetching STORED samples with storage locations -
 * Creating QC inspection records - Bulk creating QC inspections - Retrieving QC
 * inspection history
 */
@RestController
@RequestMapping(value = "/rest/biorepository/qc-inspection")
public class BiorepositoryQCInspectionRestController extends BaseRestController {

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private BiorepositoryQcRoundGenerationService qcRoundGenerationService;

    @Autowired
    private BiorepositoryQcSamplePoolService qcSamplePoolService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleStorageService storageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private TestSectionService testSectionService;

    /**
     * Active stored sample-items in biorepository scope (same source as Storage Management).
     */
    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getStoredSamplesForQC(
            @RequestParam(required = false) Integer notebookId) {
        return ResponseEntity.ok(qcSamplePoolService.listSamplesForQcTable(notebookId));
    }

    /**
     * Bulk create QC inspections for multiple samples.
     */
    @PostMapping(value = "/bulk-apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> bulkApplyQC(@RequestBody BulkQCInspectionRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            // Validate required fields
            if (request.getBioSampleIds() == null || request.getBioSampleIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one biosample ID is required"));
            }
            if (request.getInspectorName() == null || request.getInspectorName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Inspector name is required"));
            }

            Set<Integer> uniqueBioSampleIds = new LinkedHashSet<>();
            for (Integer bioSampleId : request.getBioSampleIds()) {
                if (bioSampleId == null) {
                    continue;
                }
                if (!uniqueBioSampleIds.add(bioSampleId)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Duplicate biosample IDs are not allowed in bulk QC request"));
                }
            }
            if (uniqueBioSampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one biosample ID is required"));
            }

            String qcBatchId = trimToNull(request.getQcBatchId());
            if (qcBatchId != null) {
                for (Integer bioSampleId : uniqueBioSampleIds) {
                    if (hasInspectionInBatch(qcBatchId, bioSampleId)) {
                        return ResponseEntity.status(409).body(Map.of("error",
                                "Sample already has a QC inspection in batch " + qcBatchId
                                        + ". Re-inspection for the same round is not allowed."));
                    }
                }
            }

            if (requiresCorrectionWorkflow(request) && request.getBioSampleIds().size() != 1) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Correction workflow currently supports exactly one sample per request"));
            }

            if (requiresCorrectionWorkflow(request) && allChecklistPassed(request)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Correction workflow can only be used when QC result is discrepancy"));
            }
            validateCorrectionWorkflowRequest(request);

            // Use current timestamp if not provided
            Timestamp inspectionDate = request.getInspectionDate() != null ? request.getInspectionDate()
                    : new Timestamp(System.currentTimeMillis());

            List<BiorepositoryQCInspection> inspections = qcInspectionService.createBulkInspections(
                    new ArrayList<>(uniqueBioSampleIds), request.getInspectorName(), inspectionDate,
                    request.isSamplePresent(),
                    request.isLabelIntegrity(), request.isContainerIntegrity(), request.isVolumeAppearanceAcceptable(),
                    request.isCorrectPosition(), request.getDiscrepancyType(), request.getCorrectiveAction(),
                    request.getRemarks(), request.getQcBatchId(), request.getExpectedCoordinateSnapshot(), sysUserId);

            List<Map<String, Object>> result = new ArrayList<>();
            for (BiorepositoryQCInspection inspection : inspections) {
                Map<String, Object> correctionDetails = null;
                if (requiresCorrectionWorkflow(request)) {
                    correctionDetails = applyCorrectionWorkflow(inspection, request);
                }

                Map<String, Object> inspectionMap = mapQCInspection(inspection, correctionDetails);
                if (correctionDetails != null) {
                    inspectionMap.put("correction", correctionDetails);
                }

                result.add(inspectionMap);
            }

            return ResponseEntity.ok(Map.of("inspections", result, "count", inspections.size()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create QC inspections: " + e.getMessage()));
        }
    }

    /**
     * Get QC inspection history for a specific biosample.
     */
    @GetMapping(value = "/history/{bioSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getQCHistory(@PathVariable("bioSampleId") Integer bioSampleId) {

        List<BiorepositoryQCInspection> inspections = qcInspectionService.getByBioSampleId(bioSampleId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (BiorepositoryQCInspection inspection : inspections) {
            result.add(mapQCInspection(inspection));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get QC statistics summary.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getQCStats() {
        long verifiedCount = qcInspectionService.countByQCResult(BiorepositoryQCInspection.QCResult.VERIFIED);
        long discrepancyCount = qcInspectionService
                .countByQCResult(BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInspections", verifiedCount + discrepancyCount);
        stats.put("verified", verifiedCount);
        stats.put("discrepanciesFound", discrepancyCount);

        return ResponseEntity.ok(stats);
    }

    /**
     * Storage overview for QC filter structure before random generation.
     * Uses biorepository-scoped storage hierarchy for device/shelf/rack/box options
     * and counts, plus current STORED biosample assignments for eligible sample scope.
     */
    @GetMapping(value = "/storage-overview", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getQCStorageOverview(@RequestParam(required = false) String freezer,
            @RequestParam(required = false) String shelf, @RequestParam(required = false) String rack,
            @RequestParam(required = false) String box,
            @RequestParam(required = false, defaultValue = "true") Boolean includeInspected,
            @RequestParam(required = false) Integer notebookId,
            @RequestParam(required = false, defaultValue = "false") Boolean summaryOnly,
            @RequestParam(required = false) Integer eligibleLimit,
            @RequestParam(required = false) Integer eligibleOffset) {
        return ResponseEntity.ok(buildStorageOverviewMap(normalizeFilter(freezer), normalizeFilter(shelf),
                normalizeFilter(rack), normalizeFilter(box), Boolean.TRUE.equals(includeInspected), notebookId,
                Boolean.TRUE.equals(summaryOnly), eligibleLimit, eligibleOffset));
    }

    /**
     * Random QC round using the same eligible pool as storage-overview (including quarter rule).
     */
    @PostMapping(value = "/generate-round", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQCRound(@RequestBody(required = false) GenerateQCRoundRequest request,
            HttpServletRequest httpRequest) {
        if (getSysUserId(httpRequest) == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }
        if (request == null) {
            request = new GenerateQCRoundRequest();
        }
        int boxesPerRound = request.getBoxesPerRound() > 0 ? request.getBoxesPerRound() : 10;
        int samplesPerBox = request.getSamplesPerBox();
        boolean includeAll = request.getIncludeInspected() == null || Boolean.TRUE.equals(request.getIncludeInspected());
        String freezerFilter = normalizeFilter(request.getFreezer());
        Map<String, Object> overview = buildStorageOverviewMap(freezerFilter, normalizeFilter(request.getShelf()),
                normalizeFilter(request.getRack()), normalizeFilter(request.getBox()), includeAll,
                request.getNotebookId());
        try {
            Map<String, Object> result = qcRoundGenerationService.generateRound(overview, boxesPerRound, samplesPerBox,
                    request.getSeed(), freezerFilter, Boolean.TRUE.equals(request.getAllowAllDevices()));
            result.put("includeInspected", includeAll);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = qcRoundGenerationService.toErrorBody(e);
            errorBody.put("includeInspected", includeAll);
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    /**
     * Escalation signals for a single QC batch (fail rate, repeated location failures, critical missing).
     */
    @GetMapping(value = "/batch-escalation", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBatchEscalation(@RequestParam String qcBatchId) {
        if (trimToNull(qcBatchId) == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "qcBatchId is required"));
        }
        List<BiorepositoryQCInspection> inspections = qcInspectionService.getByQcBatchId(qcBatchId.trim());
        Map<String, Object> escalation = buildBatchEscalationSignals(inspections);
        escalation.put("qcBatchId", qcBatchId.trim());
        escalation.put("inspectionCount", inspections.size());
        return ResponseEntity.ok(escalation);
    }

    private boolean hasInspectionInBatch(String qcBatchId, Integer bioSampleId) {
        if (qcBatchId == null || bioSampleId == null) {
            return false;
        }
        return qcInspectionService.getByQcBatchId(qcBatchId).stream()
                .anyMatch(inspection -> inspection.getBioSample() != null
                        && bioSampleId.equals(inspection.getBioSample().getId()));
    }

    /**
     * When true: all STORED samples in scope with valid path are in the pool (including
     * re-QC the same quarter). When false: exclude samples that already have a QC
     * inspection in the current calendar quarter.
     */
    private Map<String, Object> buildStorageOverviewMap(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits, Integer notebookId) {
        return buildStorageOverviewMap(freezerFilter, shelfFilter, rackFilter, boxFilter, includeAllQcVisits,
                notebookId, false, null, null);
    }

    private Map<String, Object> buildStorageOverviewMap(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits, Integer notebookId, boolean summaryOnly,
            Integer eligibleLimit, Integer eligibleOffset) {
        return qcSamplePoolService.buildStorageOverview(freezerFilter, shelfFilter, rackFilter, boxFilter,
                includeAllQcVisits, notebookId, summaryOnly, eligibleLimit, eligibleOffset);
    }

    private Map<String, Object> buildBatchEscalationSignals(List<BiorepositoryQCInspection> inspections) {
        Map<String, Object> signals = new HashMap<>();
        long totalInspections = inspections.size();
        long failedInspections = inspections.stream()
                .filter(insp -> BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(insp.getQcResult()))
                .count();
        double batchFailRate = totalInspections > 0 ? (failedInspections * 100.0 / totalInspections) : 0.0;
        boolean batchFailRateExceeded = totalInspections > 0 && batchFailRate >= 5.0;

        Map<String, Long> failuresByBox = new HashMap<>();
        Map<String, Long> failuresByRack = new HashMap<>();
        for (BiorepositoryQCInspection inspection : inspections) {
            if (!BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
                continue;
            }
            String path = inspection.getExpectedLocationPath();
            if (path == null) {
                continue;
            }
            String[] levels = BiorepositoryQcHierarchyParser.parseHierarchyLevels(path);
            String boxKey = buildHierarchyKey(levels[0], levels[1], levels[2], levels[3]);
            String rackKey = buildHierarchyKey(levels[0], levels[1], levels[2]);
            failuresByBox.merge(boxKey, 1L, Long::sum);
            failuresByRack.merge(rackKey, 1L, Long::sum);
        }

        long repeatedBoxes = failuresByBox.values().stream().filter(count -> count >= 2).count();
        long repeatedRacks = failuresByRack.values().stream().filter(count -> count >= 2).count();
        boolean repeatedFailureInSameBoxOrRack = repeatedBoxes > 0 || repeatedRacks > 0;

        long criticalMissingSamples = inspections.stream()
                .filter(insp -> BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(insp.getQcResult()))
                .filter(this::isSampleMissingDiscrepancy)
                .count();

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

        signals.put("totalInspections", totalInspections);
        signals.put("failedInspections", failedInspections);
        signals.put("batchFailRatePercent", batchFailRate);
        signals.put("batchFailRateThresholdPercent", 5.0);
        signals.put("batchFailRateExceeded", batchFailRateExceeded);
        signals.put("repeatedFailureInSameBoxOrRack", repeatedFailureInSameBoxOrRack);
        signals.put("criticalMissingSamples", criticalMissingSamples);
        signals.put("criticalSamplesMissing", criticalMissingSamples > 0);
        signals.put("triggeredRules", triggeredRules);
        signals.put("supervisorNotificationRequired", !triggeredRules.isEmpty());
        if (!triggeredRules.isEmpty()) {
            signals.put("supervisorNotificationMessage",
                    "Biorepository QC batch escalation: fail rate "
                            + String.format("%.1f", batchFailRate)
                            + "%. Rules: " + String.join(", ", triggeredRules));
        }
        return signals;
    }

    private static final class CalendarQuarter {
        final Timestamp start;
        final Timestamp end;
        final String label;

        private CalendarQuarter(Timestamp start, Timestamp end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }
    }

    static CalendarQuarter resolveCurrentCalendarQuarter(ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        int year = now.getYear();
        int month = now.getMonthValue();
        int quarter = (month - 1) / 3 + 1;
        int firstMonth = (quarter - 1) * 3 + 1;
        LocalDate firstDay = LocalDate.of(year, firstMonth, 1);
        LocalDate lastDay = firstDay.plusMonths(3).minusDays(1);
        ZonedDateTime start = firstDay.atStartOfDay(zone);
        ZonedDateTime end = lastDay.atTime(LocalTime.of(23, 59, 59, 999_000_000)).atZone(zone);
        return new CalendarQuarter(Timestamp.from(start.toInstant()), Timestamp.from(end.toInstant()),
                year + " Q" + quarter);
    }

    // ========== Helper Methods ==========

    private String normalizeFilter(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty() || "__ALL__".equals(v)) {
            return null;
        }
        return v;
    }

    private String buildHierarchyKey(String... levels) {
        if (levels == null || levels.length == 0) {
            return "";
        }
        StringBuilder keyBuilder = new StringBuilder();
        for (String level : levels) {
            if (level == null || level.isBlank()) {
                return "";
            }
            if (keyBuilder.length() > 0) {
                keyBuilder.append(" > ");
            }
            keyBuilder.append(level.trim());
        }
        return keyBuilder.toString();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }

    private Map<String, Object> mapQCInspection(BiorepositoryQCInspection inspection) {
        return mapQCInspection(inspection, null);
    }

    private Map<String, Object> mapQCInspection(BiorepositoryQCInspection inspection,
            Map<String, Object> correctionDetails) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", inspection.getId());
        map.put("bioSampleId", inspection.getBioSample().getId());
        map.put("inspectorName", inspection.getInspectorName());
        map.put("technicianId", inspection.getSysUserId());
        map.put("inspectionDate", inspection.getInspectionDate().toString());
        map.put("lastQCDate", inspection.getInspectionDate().toString());
        map.put("qcBatchId", inspection.getQcBatchId());
        map.put("qcResult", inspection.getQcResult().name());
        String qcStatus = deriveQcStatus(inspection);
        map.put("qcStatus", qcStatus);
        map.put("sampleFlag", "VALID".equals(qcStatus) ? "QC_VALID" : "QC_FAILED");
        map.put("qcFailed", !"VALID".equals(qcStatus));
        map.put("lifecycleOutcome", deriveLifecycleOutcome(inspection));
        map.put("expectedLocationPath", inspection.getExpectedLocationPath());
        map.put("expectedPositionCoordinate", inspection.getExpectedPositionCoordinate());
        map.put("expectedCoordinateSnapshot", inspection.getExpectedCoordinateSnapshot());

        // Checklist items
        map.put("samplePresent", inspection.isSamplePresent());
        map.put("labelIntegrity", inspection.isLabelIntegrity());
        map.put("containerIntegrity", inspection.isContainerIntegrity());
        map.put("volumeAppearanceAcceptable", inspection.isVolumeAppearanceAcceptable());
        map.put("correctPosition", inspection.isCorrectPosition());

        // Discrepancy details (if applicable)
        if (inspection.getDiscrepancyType() != null) {
            map.put("discrepancyType", inspection.getDiscrepancyType().name());
        }
        if (inspection.getCorrectiveAction() != null) {
            map.put("correctiveAction", inspection.getCorrectiveAction());
        }
        if (inspection.getRemarks() != null) {
            map.put("remarks", inspection.getRemarks());
            map.put("failureComment", inspection.getRemarks());
        }
        if (inspection.getCorrectionActionType() != null) {
            map.put("correctionActionType", inspection.getCorrectionActionType());
        }
        if (inspection.getCorrectionByUser() != null) {
            map.put("correctionByUser", inspection.getCorrectionByUser());
        }
        if (inspection.getCorrectionTimestamp() != null) {
            map.put("correctionTimestamp", inspection.getCorrectionTimestamp().toString());
        }

        map.put("auditTrail", buildAuditTrail(inspection, correctionDetails));

        return map;
    }

    private boolean allChecklistPassed(BulkQCInspectionRequest request) {
        return request.isSamplePresent() && request.isLabelIntegrity() && request.isContainerIntegrity()
                && request.isVolumeAppearanceAcceptable() && request.isCorrectPosition();
    }

    private boolean requiresCorrectionWorkflow(BulkQCInspectionRequest request) {
        return trimToNull(request.getCorrectionActionType()) != null;
    }

    private void validateCorrectionWorkflowRequest(BulkQCInspectionRequest request) {
        if (!requiresCorrectionWorkflow(request)) {
            return;
        }

        String correctionActionType = trimToNull(request.getCorrectionActionType());
        String discrepancyType = trimToNull(request.getDiscrepancyType());
        String locationId = trimToNull(request.getCorrectionLocationId());
        String locationType = trimToNull(request.getCorrectionLocationType());
        String positionCoordinate = trimToNull(request.getCorrectionPositionCoordinate());

        if ("MARK_MISSING".equalsIgnoreCase(correctionActionType)) {
            boolean isMissingDiscrepancy = BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.name()
                    .equals(discrepancyType)
                    || BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE.name().equals(discrepancyType);
            if (!isMissingDiscrepancy) {
                throw new IllegalArgumentException("MARK_MISSING requires discrepancy type SAMPLE_MISSING");
            }
            if (locationId != null || locationType != null || positionCoordinate != null) {
                throw new IllegalArgumentException(
                        "MARK_MISSING does not allow correction location/position fields");
            }
            return;
        }

        if ("UPDATE_LOCATION".equalsIgnoreCase(correctionActionType)
                || "REASSIGN_POSITION".equalsIgnoreCase(correctionActionType)) {
            if (locationId == null || locationType == null) {
                throw new IllegalArgumentException(
                        "UPDATE_LOCATION/REASSIGN_POSITION require correctionLocationId and correctionLocationType");
            }
            if ("REASSIGN_POSITION".equalsIgnoreCase(correctionActionType) && positionCoordinate == null) {
                throw new IllegalArgumentException("REASSIGN_POSITION requires correctionPositionCoordinate");
            }
            return;
        }

        if ("QUARANTINE_SAMPLE".equalsIgnoreCase(correctionActionType)
                || "REQUEST_SUPERVISOR_REVIEW".equalsIgnoreCase(correctionActionType)) {
            if (locationId != null || locationType != null || positionCoordinate != null) {
                throw new IllegalArgumentException(
                        "QUARANTINE_SAMPLE and REQUEST_SUPERVISOR_REVIEW do not allow correction location/position fields");
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported correction action type: " + correctionActionType);
    }

    private Map<String, Object> applyCorrectionWorkflow(BiorepositoryQCInspection inspection, BulkQCInspectionRequest request) {
        if (inspection.getQcResult() != BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND) {
            throw new IllegalArgumentException("Correction workflow requires discrepancy QC result");
        }

        BioSample bioSample = inspection.getBioSample();
        if (bioSample == null || bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            throw new IllegalArgumentException("Sample item is required for correction workflow");
        }

        String correctionActionType = trimToNull(request.getCorrectionActionType());
        String sampleItemId = bioSample.getSampleItem().getId();

        if ("MARK_MISSING".equalsIgnoreCase(correctionActionType)) {
            if (!isSampleMissingDiscrepancy(inspection)) {
                throw new IllegalArgumentException("MARK_MISSING requires discrepancy type SAMPLE_MISSING");
            }

            String reason = trimToNull(request.getCorrectionReason());
            if (reason == null) {
                reason = trimToNull(request.getCorrectiveAction());
            }
            if (reason == null) {
                reason = "Sample marked as missing during QC";
            }

            String normalizedReason = reason;
            if (!normalizedReason.toUpperCase().startsWith("MARK_MISSING")) {
                normalizedReason = "MARK_MISSING: " + normalizedReason;
            }

            Timestamp correctionTimestamp = new Timestamp(System.currentTimeMillis());
            Map<String, Object> missingUpdate = storageService.markSampleItemMissing(sampleItemId, normalizedReason,
                    trimToNull(request.getRemarks()));

            @SuppressWarnings("unchecked")
            Map<String, Object> updatedLocation = missingUpdate.get("updatedLocation") instanceof Map
                    ? (Map<String, Object>) missingUpdate.get("updatedLocation")
                    : Map.of("hierarchicalPath", "Missing (not found during QC)", "positionCoordinate", null,
                            "status", "MISSING");

            Map<String, Object> correction = new HashMap<>();
            correction.put("actionType", "MARK_MISSING");
            correction.put("movementId", asString(missingUpdate.get("movementId")));
            correction.put("reason", normalizedReason);
            correction.put("updatedLocation", updatedLocation);
            correction.put("correctionTimestamp", correctionTimestamp.toString());
            persistCorrectionAuditFields(inspection, "MARK_MISSING", normalizedReason, updatedLocation,
                    correctionTimestamp);
            correction.put("auditTrail", buildAuditTrail(inspection, correction));
            return correction;
        }

        if ("QUARANTINE_SAMPLE".equalsIgnoreCase(correctionActionType)) {
            return applyMetadataCorrectionWorkflow(inspection, request, correctionActionType,
                    "Sample quarantined pending review after QC failure");
        }

        if ("REQUEST_SUPERVISOR_REVIEW".equalsIgnoreCase(correctionActionType)) {
            return applyMetadataCorrectionWorkflow(inspection, request, correctionActionType,
                    "Supervisor review requested after QC failure");
        }

        if (!"UPDATE_LOCATION".equalsIgnoreCase(correctionActionType)
                && !"REASSIGN_POSITION".equalsIgnoreCase(correctionActionType)) {
            throw new IllegalArgumentException("Unsupported correction action type: " + correctionActionType);
        }

        String locationId = trimToNull(request.getCorrectionLocationId());
        String locationType = trimToNull(request.getCorrectionLocationType());
        if (locationId == null || locationType == null) {
            throw new IllegalArgumentException(
                    "Correction locationId and locationType are required for UPDATE_LOCATION/REASSIGN_POSITION");
        }

        String reason = trimToNull(request.getCorrectionReason());
        if (reason == null) {
            reason = trimToNull(request.getCorrectiveAction());
        }
        if (reason == null) {
            reason = "QC discrepancy correction";
        }

        String movementId = storageService.moveSampleItemWithLocation(sampleItemId, locationId, locationType,
                trimToNull(request.getCorrectionPositionCoordinate()), reason, trimToNull(request.getRemarks()));

        String normalizedActionReason = reason;
        if (!normalizedActionReason.toUpperCase().startsWith(correctionActionType.toUpperCase())) {
            normalizedActionReason = correctionActionType + ": " + reason;
        }
        Map<String, Object> updatedLocation = storageService.getSampleItemLocation(sampleItemId);
        Timestamp correctionTimestamp = new Timestamp(System.currentTimeMillis());
        persistCorrectionAuditFields(inspection, correctionActionType, normalizedActionReason, updatedLocation,
                correctionTimestamp);
        Map<String, Object> correction = new HashMap<>();
        correction.put("actionType", correctionActionType);
        correction.put("movementId", movementId);
        correction.put("reason", normalizedActionReason);
        correction.put("updatedLocation", updatedLocation);
        correction.put("correctionTimestamp", correctionTimestamp.toString());
        correction.put("auditTrail", buildAuditTrail(inspection, correction));
        return correction;
    }

    private Map<String, Object> applyMetadataCorrectionWorkflow(BiorepositoryQCInspection inspection,
            BulkQCInspectionRequest request, String correctionActionType, String defaultReason) {
        String reason = trimToNull(request.getCorrectionReason());
        if (reason == null) {
            reason = trimToNull(request.getCorrectiveAction());
        }
        if (reason == null) {
            reason = defaultReason;
        }
        String normalizedReason = reason;
        if (!normalizedReason.toUpperCase().startsWith(correctionActionType.toUpperCase())) {
            normalizedReason = correctionActionType + ": " + reason;
        }

        BioSample bioSample = inspection.getBioSample();
        Map<String, Object> currentLocation = Map.of();
        if (bioSample != null && bioSample.getSampleItem() != null && bioSample.getSampleItem().getId() != null) {
            currentLocation = storageService.getSampleItemLocation(bioSample.getSampleItem().getId());
            if (currentLocation == null) {
                currentLocation = Map.of();
            }
        }

        Timestamp correctionTimestamp = new Timestamp(System.currentTimeMillis());
        persistCorrectionAuditFields(inspection, correctionActionType, normalizedReason, currentLocation,
                correctionTimestamp);

        Map<String, Object> correction = new HashMap<>();
        correction.put("actionType", correctionActionType);
        correction.put("reason", normalizedReason);
        correction.put("updatedLocation", currentLocation);
        correction.put("correctionTimestamp", correctionTimestamp.toString());
        if ("QUARANTINE_SAMPLE".equalsIgnoreCase(correctionActionType)) {
            correction.put("quarantined", true);
        }
        if ("REQUEST_SUPERVISOR_REVIEW".equalsIgnoreCase(correctionActionType)) {
            correction.put("supervisorReviewRequested", true);
        }
        correction.put("auditTrail", buildAuditTrail(inspection, correction));
        return correction;
    }

    private void persistCorrectionAuditFields(BiorepositoryQCInspection inspection, String actionType, String reason,
            Map<String, Object> updatedLocation, Timestamp correctionTimestamp) {
        String oldCoordinate = composeCoordinate(inspection.getExpectedLocationPath(),
                inspection.getExpectedPositionCoordinate());
        String newCoordinate = null;
        if (updatedLocation != null) {
            newCoordinate = composeCoordinate(asString(updatedLocation.get("hierarchicalPath")),
                    asString(updatedLocation.get("positionCoordinate")));
        }
        if (newCoordinate == null && "MARK_MISSING".equalsIgnoreCase(actionType)) {
            newCoordinate = "Missing (not found during QC)";
        }

        inspection.setCorrectiveAction(reason);
        if (trimToNull(inspection.getRemarks()) == null && "MARK_MISSING".equalsIgnoreCase(actionType)) {
            inspection.setRemarks("Marked as missing during QC correction workflow");
        }
        inspection.setCorrectionActionType(actionType);
        inspection.setCorrectionOldCoordinate(oldCoordinate);
        inspection.setCorrectionNewCoordinate(newCoordinate != null ? newCoordinate : oldCoordinate);
        inspection.setCorrectionReason(reason);
        inspection.setCorrectionByUser(inspection.getSysUserId());
        inspection.setCorrectionTimestamp(correctionTimestamp);
        qcInspectionService.update(inspection);
    }

    private String deriveQcStatus(BiorepositoryQCInspection inspection) {
        if (inspection == null || inspection.getQcResult() == null) {
            return "UNKNOWN";
        }
        if (BiorepositoryQCInspection.QCResult.VERIFIED.equals(inspection.getQcResult())) {
            return "VALID";
        }
        if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
            if (isSampleMissingDiscrepancy(inspection)
                    && "MARK_MISSING".equalsIgnoreCase(trimToNull(inspection.getCorrectionActionType()))) {
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

    private boolean isSampleMissingDiscrepancy(BiorepositoryQCInspection inspection) {
        if (inspection == null || inspection.getDiscrepancyType() == null) {
            return false;
        }
        return BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.equals(inspection.getDiscrepancyType())
                || BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE.equals(inspection.getDiscrepancyType());
    }

    private Map<String, Object> buildAuditTrail(BiorepositoryQCInspection inspection,
            Map<String, Object> correctionDetails) {
        Map<String, Object> auditTrail = new HashMap<>();

        String oldCoordinate = composeCoordinate(inspection.getExpectedLocationPath(),
                inspection.getExpectedPositionCoordinate());

        String newCoordinate = null;
        String reason = trimToNull(inspection.getCorrectiveAction());
        String auditTimestamp = inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null;
        if (correctionDetails != null) {
            reason = trimToNull(asString(correctionDetails.get("reason")));
            String correctionTimestamp = trimToNull(asString(correctionDetails.get("correctionTimestamp")));
            if (correctionTimestamp != null) {
                auditTimestamp = correctionTimestamp;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> updatedLocation = correctionDetails.get("updatedLocation") instanceof Map
                    ? (Map<String, Object>) correctionDetails.get("updatedLocation")
                    : null;
            if (updatedLocation != null) {
                newCoordinate = composeCoordinate(asString(updatedLocation.get("hierarchicalPath")),
                        asString(updatedLocation.get("positionCoordinate")));
            }
        }

        if (newCoordinate == null && "MISSING".equals(deriveQcStatus(inspection))) {
            newCoordinate = "Missing (not found during QC)";
        }

        if (newCoordinate == null && correctionDetails == null
                && BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
            BioSample bioSample = inspection.getBioSample();
            SampleItem sampleItem = bioSample != null ? bioSample.getSampleItem() : null;
            if (sampleItem != null && sampleItem.getId() != null) {
                Map<String, Object> currentLocation = storageService.getSampleItemLocation(sampleItem.getId().toString());
                if (currentLocation != null) {
                    newCoordinate = composeCoordinate(asString(currentLocation.get("hierarchicalPath")),
                            asString(currentLocation.get("positionCoordinate")));
                }
            }
        }

        if (newCoordinate == null) {
            newCoordinate = oldCoordinate;
        }

        auditTrail.put("oldCoordinate", oldCoordinate);
        auditTrail.put("newCoordinate", newCoordinate);
        auditTrail.put("fromCoordinates", oldCoordinate);
        auditTrail.put("toCoordinates", newCoordinate);
        auditTrail.put("user", inspection.getSysUserId());
        auditTrail.put("correctedBy", inspection.getSysUserId());
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ========== Request DTOs ==========

    public static class GenerateQCRoundRequest {
        private int boxesPerRound = 10;
        private int samplesPerBox = 3;
        private Long seed;
        private String freezer;
        private String shelf;
        private String rack;
        private String box;
        /** When true (default), full eligible pool. When false, exclude if inspected this calendar quarter. */
        private Boolean includeInspected = Boolean.TRUE;
        private Integer notebookId;
        /** Lab manager / full-pool QC may generate across all devices without a freezer filter. */
        private Boolean allowAllDevices = Boolean.FALSE;

        public int getBoxesPerRound() {
            return boxesPerRound;
        }

        public void setBoxesPerRound(int boxesPerRound) {
            this.boxesPerRound = boxesPerRound;
        }

        public int getSamplesPerBox() {
            return samplesPerBox;
        }

        public void setSamplesPerBox(int samplesPerBox) {
            this.samplesPerBox = samplesPerBox;
        }

        public Long getSeed() {
            return seed;
        }

        public void setSeed(Long seed) {
            this.seed = seed;
        }

        public String getFreezer() {
            return freezer;
        }

        public void setFreezer(String freezer) {
            this.freezer = freezer;
        }

        public String getShelf() {
            return shelf;
        }

        public void setShelf(String shelf) {
            this.shelf = shelf;
        }

        public String getRack() {
            return rack;
        }

        public void setRack(String rack) {
            this.rack = rack;
        }

        public String getBox() {
            return box;
        }

        public void setBox(String box) {
            this.box = box;
        }

        public Boolean getIncludeInspected() {
            return includeInspected;
        }

        public void setIncludeInspected(Boolean includeInspected) {
            this.includeInspected = includeInspected;
        }

        public Integer getNotebookId() {
            return notebookId;
        }

        public void setNotebookId(Integer notebookId) {
            this.notebookId = notebookId;
        }

        public Boolean getAllowAllDevices() {
            return allowAllDevices;
        }

        public void setAllowAllDevices(Boolean allowAllDevices) {
            this.allowAllDevices = allowAllDevices;
        }
    }

    public static class BulkQCInspectionRequest {
        private List<Integer> bioSampleIds;
        private String inspectorName;
        private Timestamp inspectionDate;
        private String qcBatchId;
        private String expectedCoordinateSnapshot;
        private boolean samplePresent;
        private boolean labelIntegrity;
        private boolean containerIntegrity;
        private boolean volumeAppearanceAcceptable;
        private boolean correctPosition;
        private String discrepancyType;
        private String correctiveAction;
        private String remarks;
        private String correctionActionType;
        private String correctionLocationId;
        private String correctionLocationType;
        private String correctionPositionCoordinate;
        private String correctionReason;

        // Getters and setters
        public List<Integer> getBioSampleIds() {
            return bioSampleIds;
        }

        public void setBioSampleIds(List<Integer> bioSampleIds) {
            this.bioSampleIds = bioSampleIds;
        }

        public String getInspectorName() {
            return inspectorName;
        }

        public void setInspectorName(String inspectorName) {
            this.inspectorName = inspectorName;
        }

        public Timestamp getInspectionDate() {
            return inspectionDate;
        }

        public void setInspectionDate(Timestamp inspectionDate) {
            this.inspectionDate = inspectionDate;
        }

        public String getQcBatchId() {
            return qcBatchId;
        }

        public void setQcBatchId(String qcBatchId) {
            this.qcBatchId = qcBatchId;
        }

        public String getExpectedCoordinateSnapshot() {
            return expectedCoordinateSnapshot;
        }

        public void setExpectedCoordinateSnapshot(String expectedCoordinateSnapshot) {
            this.expectedCoordinateSnapshot = expectedCoordinateSnapshot;
        }

        public boolean isSamplePresent() {
            return samplePresent;
        }

        public void setSamplePresent(boolean samplePresent) {
            this.samplePresent = samplePresent;
        }

        public boolean isLabelIntegrity() {
            return labelIntegrity;
        }

        public void setLabelIntegrity(boolean labelIntegrity) {
            this.labelIntegrity = labelIntegrity;
        }

        public boolean isContainerIntegrity() {
            return containerIntegrity;
        }

        public void setContainerIntegrity(boolean containerIntegrity) {
            this.containerIntegrity = containerIntegrity;
        }

        public boolean isVolumeAppearanceAcceptable() {
            return volumeAppearanceAcceptable;
        }

        public void setVolumeAppearanceAcceptable(boolean volumeAppearanceAcceptable) {
            this.volumeAppearanceAcceptable = volumeAppearanceAcceptable;
        }

        public boolean isCorrectPosition() {
            return correctPosition;
        }

        public void setCorrectPosition(boolean correctPosition) {
            this.correctPosition = correctPosition;
        }

        public String getDiscrepancyType() {
            return discrepancyType;
        }

        public void setDiscrepancyType(String discrepancyType) {
            this.discrepancyType = discrepancyType;
        }

        public String getCorrectiveAction() {
            return correctiveAction;
        }

        public void setCorrectiveAction(String correctiveAction) {
            this.correctiveAction = correctiveAction;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getCorrectionActionType() {
            return correctionActionType;
        }

        public void setCorrectionActionType(String correctionActionType) {
            this.correctionActionType = correctionActionType;
        }

        public String getCorrectionLocationId() {
            return correctionLocationId;
        }

        public void setCorrectionLocationId(String correctionLocationId) {
            this.correctionLocationId = correctionLocationId;
        }

        public String getCorrectionLocationType() {
            return correctionLocationType;
        }

        public void setCorrectionLocationType(String correctionLocationType) {
            this.correctionLocationType = correctionLocationType;
        }

        public String getCorrectionPositionCoordinate() {
            return correctionPositionCoordinate;
        }

        public void setCorrectionPositionCoordinate(String correctionPositionCoordinate) {
            this.correctionPositionCoordinate = correctionPositionCoordinate;
        }

        public String getCorrectionReason() {
            return correctionReason;
        }

        public void setCorrectionReason(String correctionReason) {
            this.correctionReason = correctionReason;
        }
    }
}
