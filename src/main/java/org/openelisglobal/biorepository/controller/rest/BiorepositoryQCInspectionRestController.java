package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryQcOutcomeDerivation;
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
import org.openelisglobal.storage.valueholder.StorageDevice.DeviceType;
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
    private BioSampleService bioSampleService;

    @Autowired
    private SampleStorageService storageService;

    @Autowired
    private StorageLocationService storageLocationService;

    /**
     * Get all BioSamples with workflowStatus = STORED along with their storage
     * locations. This endpoint provides samples ready for QC inspection.
     */
    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getStoredSamplesForQC() {
        List<BioSample> storedSamples = bioSampleService.getAll().stream()
                .filter(bs -> WorkflowStatus.STORED.equals(bs.getWorkflowStatus())).toList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (BioSample bioSample : storedSamples) {
            Map<String, Object> sampleData = new HashMap<>();

            // BioSample basic info
            sampleData.put("bioSampleId", bioSample.getId());
            sampleData.put("workflowStatus", bioSample.getWorkflowStatus().name());
            sampleData.put("biosafetyLevel", bioSample.getBiosafetyLevel().name());

            if (bioSample.getProjectId() != null) {
                sampleData.put("projectId", bioSample.getProjectId());
            }

            // SampleItem info
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem != null) {
                sampleData.put("sampleItemId", sampleItem.getId());
                sampleData.put("externalId", sampleItem.getExternalId());

                if (sampleItem.getTypeOfSample() != null) {
                    sampleData.put("sampleType", sampleItem.getTypeOfSample().getDescription());
                    sampleData.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
                }

                if (sampleItem.getSample() != null) {
                    sampleData.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
                }

                // Get storage location
                Map<String, Object> location = storageService.getSampleItemLocation(sampleItem.getId().toString());
                if (location != null && !location.isEmpty()) {
                    sampleData.put("storageLocation", location);
                    // Extract hierarchical path for display
                    if (location.containsKey("hierarchicalPath")) {
                        sampleData.put("locationPath", location.get("hierarchicalPath"));
                    }
                }
            }

            // Check if sample has existing QC inspection
            BiorepositoryQCInspection mostRecent = qcInspectionService.getMostRecentByBioSampleId(bioSample.getId());
            if (mostRecent != null) {
                Map<String, Object> mappedInspection = mapQCInspection(mostRecent);
                sampleData.put("lastQCInspection", mappedInspection);
                sampleData.put("lastQCDate", mappedInspection.get("lastQCDate"));
                sampleData.put("qcStatus", mappedInspection.get("qcStatus"));
                sampleData.put("sampleFlag", mappedInspection.get("sampleFlag"));
                sampleData.put("qcFailed", mappedInspection.get("qcFailed"));
            } else {
                sampleData.put("qcStatus", "NOT_CHECKED");
                sampleData.put("sampleFlag", "PENDING_QC");
                sampleData.put("qcFailed", false);
            }

            result.add(sampleData);
        }

        return ResponseEntity.ok(result);
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

            if (requiresCorrectionWorkflow(request) && request.getBioSampleIds().size() != 1) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Correction workflow currently supports exactly one sample per request"));
            }

            if (requiresCorrectionWorkflow(request) && allChecklistPassed(request)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Correction workflow can only be used when QC result is discrepancy"));
            }

            String correctionValidationError = validateCorrectionWorkflowRequest(request);
            if (correctionValidationError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", correctionValidationError));
            }

            // Use current timestamp if not provided
            Timestamp inspectionDate = request.getInspectionDate() != null ? request.getInspectionDate()
                    : new Timestamp(System.currentTimeMillis());

            List<BiorepositoryQCInspection> inspections = qcInspectionService.createBulkInspections(
                    request.getBioSampleIds(), request.getInspectorName(), inspectionDate, request.isSamplePresent(),
                    request.isLabelIntegrity(), request.isContainerIntegrity(), request.isVolumeAppearanceAcceptable(),
                    request.isCorrectPosition(), request.getDiscrepancyType(), request.getCorrectiveAction(),
                    request.getRemarks(), sysUserId);

            List<Map<String, Object>> result = new ArrayList<>();
            for (BiorepositoryQCInspection inspection : inspections) {
                Map<String, Object> correctionDetails = null;
                if (requiresCorrectionWorkflow(request)) {
                    correctionDetails = qcInspectionService.applyCorrectionWorkflow(inspection,
                            request.getCorrectionActionType(), request.getCorrectionLocationId(),
                            request.getCorrectionLocationType(), request.getCorrectionPositionCoordinate(),
                            request.getCorrectionReason(), request.getCorrectiveAction(), request.getRemarks(),
                            sysUserId);
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
     * Uses active storage hierarchy entities for freezer/shelf/rack/box options and
     * counts, plus current STORED biosample assignments for eligible sample scope.
     *
     * Note: This worktree has no explicit device-level biorepository scope flag
     * (for example biorepositoryStorage), so hierarchy scope is not
     * biorepository-only.
     */
    @GetMapping(value = "/storage-overview", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getQCStorageOverview(@RequestParam(required = false) String freezer,
            @RequestParam(required = false) String shelf, @RequestParam(required = false) String rack,
            @RequestParam(required = false) String box) {
        String freezerFilter = normalizeFilter(freezer);
        String shelfFilter = normalizeFilter(shelf);
        String rackFilter = normalizeFilter(rack);
        String boxFilter = normalizeFilter(box);

        List<BioSample> storedSamples = bioSampleService.getAll().stream()
                .filter(bs -> WorkflowStatus.STORED.equals(bs.getWorkflowStatus())).toList();

        // Hierarchy scope in this branch is all active freezer entities.
        List<StorageDevice> allDevices = storageLocationService.getAllDevices().stream()
            .filter(this::isActive)
            .filter(this::isFreezer)
            .toList();

        Set<Integer> activeFreezerIds = allDevices.stream().map(StorageDevice::getId)
            .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());

        List<StorageShelf> allShelves = storageLocationService.getAllShelves().stream()
            .filter(this::isActive)
            .filter(shelfEntity -> shelfEntity.getParentDevice() != null
                && activeFreezerIds.contains(shelfEntity.getParentDevice().getId()))
            .toList();

        Set<Integer> activeShelfIds = allShelves.stream().map(StorageShelf::getId)
            .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());

        List<StorageRack> allRacks = storageLocationService.getAllRacks().stream()
            .filter(this::isActive)
            .filter(rackEntity -> rackEntity.getParentShelf() != null
                && activeShelfIds.contains(rackEntity.getParentShelf().getId()))
            .toList();

        Set<Integer> activeRackIds = allRacks.stream().map(StorageRack::getId)
            .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());

        List<StorageBox> allBoxes = storageLocationService.getAllBoxes().stream()
            .filter(this::isActive)
            .filter(boxEntity -> boxEntity.getParentRack() != null
                && activeRackIds.contains(boxEntity.getParentRack().getId()))
            .toList();

        List<StorageDevice> scopedDevices = allDevices.stream()
                .filter(device -> matches(deviceName(device), freezerFilter)).toList();

        List<StorageShelf> scopedShelves = allShelves.stream().filter(s -> {
            String deviceName = s.getParentDevice() != null ? deviceName(s.getParentDevice()) : null;
            return matches(deviceName, freezerFilter) && matches(s.getLabel(), shelfFilter);
        }).toList();

        List<StorageRack> scopedRacks = allRacks.stream().filter(r -> {
            StorageShelf parentShelf = r.getParentShelf();
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String deviceName = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            return matches(deviceName, freezerFilter) && matches(shelfLabel, shelfFilter)
                    && matches(r.getLabel(), rackFilter);
        }).toList();

        List<StorageBox> scopedBoxes = allBoxes.stream().filter(b -> {
            StorageRack parentRack = b.getParentRack();
            StorageShelf parentShelf = parentRack != null ? parentRack.getParentShelf() : null;
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String deviceName = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            String rackLabel = parentRack != null ? parentRack.getLabel() : null;
            return matches(deviceName, freezerFilter) && matches(shelfLabel, shelfFilter)
                    && matches(rackLabel, rackFilter) && matches(b.getLabel(), boxFilter);
        }).toList();

        Set<String> freezerOptions = new LinkedHashSet<>();
        for (StorageDevice d : allDevices) {
            freezerOptions.add(deviceName(d));
        }
        Set<String> shelfOptions = new LinkedHashSet<>();
        for (StorageShelf s : allShelves) {
            String parentFreezer = s.getParentDevice() != null ? deviceName(s.getParentDevice()) : null;
            if (matches(parentFreezer, freezerFilter)) {
                shelfOptions.add(s.getLabel());
            }
        }
        Set<String> rackOptions = new LinkedHashSet<>();
        for (StorageRack r : allRacks) {
            StorageShelf parentShelf = r.getParentShelf();
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String parentFreezer = parentDevice != null ? deviceName(parentDevice) : null;
            String parentShelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            if (matches(parentFreezer, freezerFilter) && matches(parentShelfLabel, shelfFilter)) {
                rackOptions.add(r.getLabel());
            }
        }
        Set<String> boxOptions = new LinkedHashSet<>();
        for (StorageBox b : allBoxes) {
            StorageRack parentRack = b.getParentRack();
            StorageShelf parentShelf = parentRack != null ? parentRack.getParentShelf() : null;
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String parentFreezer = parentDevice != null ? deviceName(parentDevice) : null;
            String parentShelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            String parentRackLabel = parentRack != null ? parentRack.getLabel() : null;
            if (matches(parentFreezer, freezerFilter) && matches(parentShelfLabel, shelfFilter)
                    && matches(parentRackLabel, rackFilter)) {
                boxOptions.add(b.getLabel());
            }
        }

        Set<String> validShelfKeys = new LinkedHashSet<>();
        for (StorageShelf shelfEntity : allShelves) {
            StorageDevice parentDevice = shelfEntity.getParentDevice();
            String freezerName = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = shelfEntity.getLabel();
            if (freezerName != null && shelfLabel != null && !shelfLabel.isBlank()) {
                validShelfKeys.add(buildHierarchyKey(freezerName, shelfLabel));
            }
        }

        Set<String> validRackKeys = new LinkedHashSet<>();
        for (StorageRack rackEntity : allRacks) {
            StorageShelf parentShelf = rackEntity.getParentShelf();
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String freezerName = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            String rackLabel = rackEntity.getLabel();
            if (freezerName != null && shelfLabel != null && !shelfLabel.isBlank()
                    && rackLabel != null && !rackLabel.isBlank()) {
                validRackKeys.add(buildHierarchyKey(freezerName, shelfLabel, rackLabel));
            }
        }

        Set<String> validBoxKeys = new LinkedHashSet<>();
        for (StorageBox boxEntity : allBoxes) {
            StorageRack parentRack = boxEntity.getParentRack();
            StorageShelf parentShelf = parentRack != null ? parentRack.getParentShelf() : null;
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String freezerName = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            String rackLabel = parentRack != null ? parentRack.getLabel() : null;
            String boxLabel = boxEntity.getLabel();
            if (freezerName != null && shelfLabel != null && !shelfLabel.isBlank()
                    && rackLabel != null && !rackLabel.isBlank()
                    && boxLabel != null && !boxLabel.isBlank()) {
                validBoxKeys.add(buildHierarchyKey(freezerName, shelfLabel, rackLabel, boxLabel));
            }
        }

        List<Map<String, Object>> eligibleSamples = new ArrayList<>();
        for (BioSample bioSample : storedSamples) {
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem == null || sampleItem.getId() == null) {
                continue;
            }
            Map<String, Object> location = storageService.getSampleItemLocation(sampleItem.getId().toString());
            if (location == null || location.isEmpty()) {
                continue;
            }
            String locationPath = asString(location.get("hierarchicalPath"));
            String[] levels = parseHierarchyLevels(locationPath);

            String parsedFreezer = levels[0];
            String parsedShelf = levels[1];
            String parsedRack = levels[2];
            String parsedBox = levels[3];

            if (!validShelfKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf))) {
                continue;
            }
            if (!validRackKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack))) {
                continue;
            }
            if (!validBoxKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack, parsedBox))) {
                continue;
            }

            if (!matches(levels[0], freezerFilter) || !matches(levels[1], shelfFilter) || !matches(levels[2], rackFilter)
                    || !matches(levels[3], boxFilter)) {
                continue;
            }
            Map<String, Object> sampleSummary = new HashMap<>();
            sampleSummary.put("bioSampleId", bioSample.getId());
            sampleSummary.put("sampleItemId", sampleItem.getId());
            sampleSummary.put("freezer", levels[0]);
            sampleSummary.put("shelf", levels[1]);
            sampleSummary.put("rack", levels[2]);
            sampleSummary.put("box", levels[3]);
            sampleSummary.put("locationPath", locationPath);
            sampleSummary.put("positionCoordinate", asString(location.get("positionCoordinate")));
            sampleSummary.put("accessionNumber",
                    sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);
            eligibleSamples.add(sampleSummary);
        }

        Map<String, Object> counts = new HashMap<>();
        counts.put("freezers", scopedDevices.size());
        counts.put("shelves", scopedShelves.size());
        counts.put("racks", scopedRacks.size());
        counts.put("boxes", scopedBoxes.size());
        counts.put("eligibleSamples", eligibleSamples.size());

        Map<String, Object> filterOptions = new HashMap<>();
        filterOptions.put("freezers", freezerOptions.stream().filter(v -> v != null && !v.isBlank()).sorted().toList());
        filterOptions.put("shelves", shelfOptions.stream().filter(v -> v != null && !v.isBlank()).sorted().toList());
        filterOptions.put("racks", rackOptions.stream().filter(v -> v != null && !v.isBlank()).sorted().toList());
        filterOptions.put("boxes", boxOptions.stream().filter(v -> v != null && !v.isBlank()).sorted().toList());

        Map<String, Object> response = new HashMap<>();
        response.put("counts", counts);
        response.put("filters", filterOptions);
        response.put("eligibleSamples", eligibleSamples);
        response.put("biorepositoryScope", Map.of(
            "deviceHierarchyBiorepositoryOnly", false,
            "reason", "No explicit device biorepository scope field exists in this branch"));
        return ResponseEntity.ok(response);
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

    private boolean matches(String value, String filter) {
        if (filter == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return filter.equals(value);
    }

    private String[] parseHierarchyLevels(String locationPath) {
        String[] defaults = new String[] { "Unknown", "Unknown", "Unknown", "Unknown" };
        if (locationPath == null || locationPath.isBlank()) {
            return defaults;
        }
        List<String> segments = Arrays.stream(locationPath.split("\\s*>\\s*")).map(String::trim).filter(s -> !s.isBlank())
                .toList();
        if (segments.isEmpty()) {
            return defaults;
        }
        int end = segments.size();
        String tail = segments.get(end - 1);
        if (tail.matches("^[A-Za-z]+\\d+$")) {
            end = end - 1;
        }
        List<String> structural = segments.subList(0, Math.max(end, 0));
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

    private String deviceName(StorageDevice device) {
        if (device == null) {
            return null;
        }
        if (device.getName() != null && !device.getName().isBlank()) {
            return device.getName().trim();
        }
        if (device.getCode() != null && !device.getCode().isBlank()) {
            return device.getCode().trim();
        }
        return null;
    }

    private boolean isActive(StorageDevice device) {
        return device != null && Boolean.TRUE.equals(device.getActive());
    }

    private boolean isActive(StorageShelf shelfEntity) {
        return shelfEntity != null && Boolean.TRUE.equals(shelfEntity.getActive());
    }

    private boolean isActive(StorageRack rackEntity) {
        return rackEntity != null && Boolean.TRUE.equals(rackEntity.getActive());
    }

    private boolean isActive(StorageBox boxEntity) {
        return boxEntity != null && Boolean.TRUE.equals(boxEntity.getActive());
    }

    private boolean isFreezer(StorageDevice device) {
        return device != null
                && (DeviceType.FREEZER.getValue().equalsIgnoreCase(asString(device.getType()))
                        || DeviceType.FREEZER.getValue().equalsIgnoreCase(asString(device.getTypeAsString())));
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
        map.put("qcResult", inspection.getQcResult().name());
        String qcStatus = BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection);
        map.put("qcStatus", qcStatus);
        map.put("sampleFlag", "VALID".equals(qcStatus) ? "QC_VALID" : "QC_FAILED");
        map.put("qcFailed", !"VALID".equals(qcStatus));
        map.put("lifecycleOutcome", BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection));
        map.put("expectedLocationPath", inspection.getExpectedLocationPath());
        map.put("expectedPositionCoordinate", inspection.getExpectedPositionCoordinate());
        map.put("correctionActionType", inspection.getCorrectionActionType());
        map.put("correctionReason", inspection.getCorrectionReason());
        map.put("correctionByUser", inspection.getCorrectionByUser());
        map.put("correctionTimestamp",
                inspection.getCorrectionTimestamp() != null ? inspection.getCorrectionTimestamp().toString() : null);

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

    private String validateCorrectionWorkflowRequest(BulkQCInspectionRequest request) {
        String correctionActionType = trimToNull(request.getCorrectionActionType());
        if (correctionActionType == null) {
            return null;
        }

        String normalizedActionType = correctionActionType.toUpperCase();
        boolean hasCorrectionLocationId = trimToNull(request.getCorrectionLocationId()) != null;
        boolean hasCorrectionLocationType = trimToNull(request.getCorrectionLocationType()) != null;
        boolean hasCorrectionLocation = hasCorrectionLocationId || hasCorrectionLocationType;
        boolean hasCorrectionPosition = trimToNull(request.getCorrectionPositionCoordinate()) != null;

        if ("MARK_MISSING".equals(normalizedActionType)) {
            BiorepositoryQCInspection.DiscrepancyType discrepancyType = BiorepositoryQCInspection.DiscrepancyType
                    .fromString(request.getDiscrepancyType());
            if (!isSampleMissingDiscrepancy(discrepancyType)) {
                return "MARK_MISSING requires discrepancy type SAMPLE_MISSING";
            }
            if (hasCorrectionLocation || hasCorrectionPosition) {
                return "MARK_MISSING does not allow correction location/position fields";
            }
            return null;
        }

        if ("UPDATE_LOCATION".equals(normalizedActionType) || "REASSIGN_POSITION".equals(normalizedActionType)) {
            if (!hasCorrectionLocationId || !hasCorrectionLocationType) {
                return "UPDATE_LOCATION/REASSIGN_POSITION require correctionLocationId and correctionLocationType";
            }
            if ("REASSIGN_POSITION".equals(normalizedActionType) && !hasCorrectionPosition) {
                return "REASSIGN_POSITION requires correctionPositionCoordinate";
            }
            return null;
        }

        return "Unsupported correction action type: " + correctionActionType;
    }

    private boolean isSampleMissingDiscrepancy(BiorepositoryQCInspection.DiscrepancyType discrepancyType) {
        if (discrepancyType == null) {
            return false;
        }
        return BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.equals(discrepancyType)
                || BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE.equals(discrepancyType);
    }

    private Map<String, Object> buildAuditTrail(BiorepositoryQCInspection inspection,
            Map<String, Object> correctionDetails) {
        Map<String, Object> auditTrail = new HashMap<>();

        String oldCoordinate = trimToNull(inspection.getCorrectionOldCoordinate());
        if (oldCoordinate == null) {
            oldCoordinate = composeCoordinate(inspection.getExpectedLocationPath(), inspection.getExpectedPositionCoordinate());
        }
        String newCoordinate = trimToNull(inspection.getCorrectionNewCoordinate());
        String reason = trimToNull(inspection.getCorrectionReason());
        if (reason == null) {
            reason = trimToNull(inspection.getCorrectiveAction());
        }
        String auditTimestamp = inspection.getCorrectionTimestamp() != null ? inspection.getCorrectionTimestamp().toString()
                : (inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
        String auditUser = trimToNull(inspection.getCorrectionByUser()) != null ? inspection.getCorrectionByUser()
                : inspection.getSysUserId();
        if (correctionDetails != null) {
            if (reason == null) {
                reason = trimToNull(asString(correctionDetails.get("reason")));
            }
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

        if (newCoordinate == null && "MISSING".equals(BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection))) {
            newCoordinate = "Missing (not found during QC)";
        }

        boolean correctionApplied = BiorepositoryQcOutcomeDerivation.hasAppliedCorrectionWorkflow(inspection);
        if (!correctionApplied
                && BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
            SampleItem sampleItem = inspection.getBioSample() != null ? inspection.getBioSample().getSampleItem() : null;
            if (sampleItem != null && sampleItem.getId() != null) {
                Map<String, Object> currentLocation = storageService.getSampleItemLocation(sampleItem.getId().toString());
                String currentCoordinate = composeCoordinate(asString(currentLocation.get("hierarchicalPath")),
                        asString(currentLocation.get("positionCoordinate")));
                if (trimToNull(currentCoordinate) != null) {
                    newCoordinate = currentCoordinate;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ========== Request DTOs ==========

    public static class BulkQCInspectionRequest {
        private List<Integer> bioSampleIds;
        private String inspectorName;
        private Timestamp inspectionDate;
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
