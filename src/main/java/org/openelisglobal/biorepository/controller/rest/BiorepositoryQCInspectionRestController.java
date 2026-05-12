package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.biorepository.service.BioSampleService;
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
            validateCorrectionWorkflowRequest(request);

            // Use current timestamp if not provided
            Timestamp inspectionDate = request.getInspectionDate() != null ? request.getInspectionDate()
                    : new Timestamp(System.currentTimeMillis());

            List<BiorepositoryQCInspection> inspections = qcInspectionService.createBulkInspections(
                    request.getBioSampleIds(), request.getInspectorName(), inspectionDate, request.isSamplePresent(),
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
            @RequestParam(required = false) String box,
            @RequestParam(required = false, defaultValue = "true") Boolean includeInspected) {
        return ResponseEntity
                .ok(buildStorageOverviewMap(normalizeFilter(freezer), normalizeFilter(shelf), normalizeFilter(rack),
                        normalizeFilter(box), Boolean.TRUE.equals(includeInspected)));
    }

    private static final int MAX_BOXES_PER_ROUND = 200;
    private static final int MAX_SAMPLES_PER_BOX = 200;

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
        int samplesPerBox = request.getSamplesPerBox() > 0 ? request.getSamplesPerBox() : 3;
        if (boxesPerRound > MAX_BOXES_PER_ROUND || samplesPerBox > MAX_SAMPLES_PER_BOX) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Parameters exceed allowed limits for this environment"));
        }
        boolean includeAll = request.getIncludeInspected() == null || Boolean.TRUE.equals(request.getIncludeInspected());
        Map<String, Object> overview = buildStorageOverviewMap(
                normalizeFilter(request.getFreezer()), normalizeFilter(request.getShelf()), normalizeFilter(request.getRack()),
                normalizeFilter(request.getBox()), includeAll);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligible = (List<Map<String, Object>>) overview.get("eligibleSamples");
        if (eligible == null || eligible.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("qcBatchId", null);
            empty.put("boxesSelected", 0);
            empty.put("samplesSelected", 0);
            empty.put("filteredSamplePool", 0);
            empty.put("includeInspected", includeAll);
            empty.put("samples", List.of());
            return ResponseEntity.ok(empty);
        }
        String qcBatchId = "QCBATCH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Random random = request.getSeed() != null ? new Random(request.getSeed()) : new Random();
        Map<String, List<Map<String, Object>>> candidatesByBox = new HashMap<>();
        for (Map<String, Object> row : eligible) {
            String boxKey = asString(row.get("boxKey"));
            if (boxKey == null) {
                continue;
            }
            candidatesByBox.computeIfAbsent(boxKey, k -> new ArrayList<>()).add(row);
        }
        if (candidatesByBox.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("qcBatchId", null);
            empty.put("boxesSelected", 0);
            empty.put("samplesSelected", 0);
            empty.put("filteredSamplePool", eligible.size());
            empty.put("includeInspected", includeAll);
            empty.put("samples", List.of());
            return ResponseEntity.ok(empty);
        }
        List<String> selectedBoxKeys = selectDistributedBoxKeys(candidatesByBox, Math.min(boxesPerRound, candidatesByBox.size()), random);
        List<Map<String, Object>> selectedSamples = new ArrayList<>();
        for (String boxKey : selectedBoxKeys) {
            List<Map<String, Object>> inBox = new ArrayList<>(candidatesByBox.getOrDefault(boxKey, List.of()));
            Collections.shuffle(inBox, random);
            int limit = Math.min(samplesPerBox, inBox.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> row = new HashMap<>(inBox.get(i));
                row.put("qcBatchId", qcBatchId);
                selectedSamples.add(row);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("qcBatchId", qcBatchId);
        result.put("boxesSelected", selectedBoxKeys.size());
        result.put("samplesSelected", selectedSamples.size());
        result.put("boxesPerRound", boxesPerRound);
        result.put("samplesPerBox", samplesPerBox);
        result.put("seed", request.getSeed());
        result.put("includeInspected", includeAll);
        result.put("filteredSamplePool", eligible.size());
        result.put("samples", selectedSamples);
        return ResponseEntity.ok(result);
    }

    /**
     * When true: all STORED samples in scope with valid path are in the pool (including
     * re-QC the same quarter). When false: exclude samples that already have a QC
     * inspection in the current calendar quarter.
     */
    private Map<String, Object> buildStorageOverviewMap(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits) {
        CalendarQuarter currentQuarter = resolveCurrentCalendarQuarter(ZoneId.systemDefault());

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

            boolean anyPriorInspection = qcInspectionService.existsByBioSampleId(bioSample.getId());
            boolean inspectedThisQuarter = qcInspectionService.hasInspectionBetween(bioSample.getId(), currentQuarter.start,
                    currentQuarter.end);
            if (!includeAllQcVisits && inspectedThisQuarter) {
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
            boolean hierarchyMatches = validShelfKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf))
                    && validRackKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack));
            if (!hierarchyMatches) {
                String[] roomPrefixedRackLevels = parseRoomPrefixedRackLevels(locationPath);
                if (validShelfKeys.contains(buildHierarchyKey(roomPrefixedRackLevels[0], roomPrefixedRackLevels[1]))
                        && validRackKeys.contains(buildHierarchyKey(roomPrefixedRackLevels[0], roomPrefixedRackLevels[1],
                                roomPrefixedRackLevels[2]))) {
                    levels = roomPrefixedRackLevels;
                    parsedFreezer = levels[0];
                    parsedShelf = levels[1];
                    parsedRack = levels[2];
                    parsedBox = levels[3];
                }
            }

            if (!validShelfKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf))) {
                continue;
            }
            if (!validRackKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack))) {
                continue;
            }
            boolean hasParsedBox = parsedBox != null && !parsedBox.isBlank() && !"Unknown".equals(parsedBox);
            if (hasParsedBox && !validBoxKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack, parsedBox))) {
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
            sampleSummary.put("anyPriorInspection", anyPriorInspection);
            sampleSummary.put("hasInspectionHistory", anyPriorInspection);
            sampleSummary.put("inspectedThisQuarter", inspectedThisQuarter);
            sampleSummary.put("shelfKey", buildHierarchyKey(parsedFreezer, parsedShelf));
            sampleSummary.put("boxKey", hasParsedBox ? buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack, parsedBox)
                    : buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack));
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
        response.put("qcExclusionWindow", Map.of("mode", "CALENDAR_QUARTER", "label", currentQuarter.label, "start",
                currentQuarter.start.toString(), "end", currentQuarter.end.toString(),
                "poolIncludesRepeatInspectionThisQuarter", includeAllQcVisits,
                "whenPoolExcludesCompletedThisQuarter", !includeAllQcVisits));
        return response;
    }

    private static List<String> selectDistributedBoxKeys(Map<String, List<Map<String, Object>>> candidatesByBox,
            int boxesToSelect, Random random) {
        Map<String, List<String>> boxesByShelf = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : candidatesByBox.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String shelfKey = asStringHelper(entry.getValue().get(0).get("shelfKey"));
            String bucket = shelfKey != null ? shelfKey : "Unknown Shelf";
            boxesByShelf.computeIfAbsent(bucket, key -> new ArrayList<>()).add(entry.getKey());
        }
        for (List<String> boxList : boxesByShelf.values()) {
            Collections.shuffle(boxList, random);
        }
        List<String> shelfKeys = new ArrayList<>(boxesByShelf.keySet());
        Collections.shuffle(shelfKeys, random);
        List<String> selected = new ArrayList<>();
        while (selected.size() < boxesToSelect) {
            boolean addedInCycle = false;
            for (String shelfKey : shelfKeys) {
                List<String> boxes = boxesByShelf.get(shelfKey);
                if (boxes == null || boxes.isEmpty()) {
                    continue;
                }
                selected.add(boxes.remove(0));
                addedInCycle = true;
                if (selected.size() >= boxesToSelect) {
                    break;
                }
            }
            if (!addedInCycle) {
                break;
            }
        }
        return selected;
    }

    private static String asStringHelper(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? null : s;
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
        if (structural.size() >= 4) {
            int boxIdx = structural.size() - 1;
            int rackIdx = structural.size() - 2;
            int shelfIdx = structural.size() - 3;
            int freezerIdx = structural.size() - 4;
            return new String[] { structural.get(freezerIdx), structural.get(shelfIdx), structural.get(rackIdx),
                    structural.get(boxIdx) };
        }
        if (structural.size() == 3) {
            return new String[] { structural.get(0), structural.get(1), structural.get(2), "Unknown" };
        }
        if (structural.size() == 2) {
            return new String[] { structural.get(0), structural.get(1), "Unknown", "Unknown" };
        }
        return new String[] { structural.get(0), "Unknown", "Unknown", "Unknown" };
    }

    private String[] parseRoomPrefixedRackLevels(String locationPath) {
        String[] defaults = new String[] { "Unknown", "Unknown", "Unknown", "Unknown" };
        if (locationPath == null || locationPath.isBlank()) {
            return defaults;
        }
        List<String> segments = Arrays.stream(locationPath.split("\\s*>\\s*")).map(String::trim).filter(s -> !s.isBlank())
                .toList();
        if (segments.size() < 4) {
            return defaults;
        }
        int end = segments.size();
        String tail = segments.get(end - 1);
        if (tail.matches("^[A-Za-z]+\\d+$")) {
            end = end - 1;
        }
        List<String> structural = segments.subList(0, Math.max(end, 0));
        if (structural.size() == 4) {
            return new String[] { structural.get(1), structural.get(2), structural.get(3), "Unknown" };
        }
        return defaults;
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
