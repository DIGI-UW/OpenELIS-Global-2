package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.biorepository.controller.rest.BiorepositoryQcHierarchyParser;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BiorepositoryQcSamplePoolServiceImpl implements BiorepositoryQcSamplePoolService {

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private IStatusService statusService;

    @Override
    @Transactional
    public Map<String, Object> buildStorageOverview(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits, Integer notebookId) {
        final String freezer = normalizeFilter(freezerFilter);
        final String shelf = normalizeFilter(shelfFilter);
        final String rack = normalizeFilter(rackFilter);
        final String box = normalizeFilter(boxFilter);

        CalendarQuarter currentQuarter = resolveCurrentCalendarQuarter(ZoneId.systemDefault());
        BiorepositoryHierarchyContext hierarchy = resolveBiorepositoryHierarchy(notebookId);
        PoolBuildResult poolResult = buildPool(notebookId, hierarchy);

        List<StorageDevice> allDevices = hierarchy.devices;
        List<StorageShelf> allShelves = hierarchy.shelves;
        List<StorageRack> allRacks = hierarchy.racks;
        List<StorageBox> allBoxes = hierarchy.boxes;
        Set<String> validShelfKeys = hierarchy.validShelfKeys;
        Set<String> validRackKeys = hierarchy.validRackKeys;
        Set<String> validBoxKeys = hierarchy.validBoxKeys;

        List<StorageDevice> scopedDevices = allDevices.stream()
                .filter(device -> matches(deviceName(device), freezer)).toList();

        List<StorageShelf> scopedShelves = allShelves.stream().filter(s -> {
            String device = s.getParentDevice() != null ? deviceName(s.getParentDevice()) : null;
            return matches(device, freezer) && matches(s.getLabel(), shelf);
        }).toList();

        List<StorageRack> scopedRacks = allRacks.stream().filter(r -> {
            StorageShelf parentShelf = r.getParentShelf();
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String device = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            return matches(device, freezer) && matches(shelfLabel, shelf)
                    && matches(r.getLabel(), rack);
        }).toList();

        List<StorageBox> scopedBoxes = allBoxes.stream().filter(b -> {
            StorageRack parentRack = b.getParentRack();
            StorageShelf parentShelf = parentRack != null ? parentRack.getParentShelf() : null;
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String device = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            String rackLabel = parentRack != null ? parentRack.getLabel() : null;
            return matches(device, freezer) && matches(shelfLabel, shelf)
                    && matches(rackLabel, rack) && matches(b.getLabel(), box);
        }).toList();

        Set<String> freezerOptions = new LinkedHashSet<>();
        for (StorageDevice d : allDevices) {
            freezerOptions.add(deviceName(d));
        }
        Set<String> shelfOptions = new LinkedHashSet<>();
        for (StorageShelf s : allShelves) {
            String parentFreezer = s.getParentDevice() != null ? deviceName(s.getParentDevice()) : null;
            if (matches(parentFreezer, freezer)) {
                shelfOptions.add(s.getLabel());
            }
        }
        Set<String> rackOptions = new LinkedHashSet<>();
        for (StorageRack r : allRacks) {
            StorageShelf parentShelf = r.getParentShelf();
            StorageDevice parentDevice = parentShelf != null ? parentShelf.getParentDevice() : null;
            String parentFreezer = parentDevice != null ? deviceName(parentDevice) : null;
            String parentShelfLabel = parentShelf != null ? parentShelf.getLabel() : null;
            if (matches(parentFreezer, freezer) && matches(parentShelfLabel, shelf)) {
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
            if (matches(parentFreezer, freezer) && matches(parentShelfLabel, shelf)
                    && matches(parentRackLabel, rack)) {
                boxOptions.add(b.getLabel());
            }
        }

        List<Map<String, Object>> eligibleSamples = new ArrayList<>();
        for (PooledSampleEntry entry : poolResult.inScope) {
            if (!includeAllQcVisits && entry.inspectedThisQuarter) {
                continue;
            }
            String[] levels = entry.levels;
            if (!matches(levels[0], freezer) || !matches(levels[1], shelf)
                    || !matches(levels[2], rack) || !matches(levels[3], box)) {
                continue;
            }
            eligibleSamples.add(entry.toEligibleSummary());
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
                "deviceHierarchyBiorepositoryOnly", true,
                "includesAllActiveDeviceTypes", false,
                "usedDepartmentFallback", hierarchy.usedDepartmentFallback,
                "reason", "QC scope limited to biorepository storage devices"));
        response.put("qcExclusionWindow", Map.of("mode", "CALENDAR_QUARTER", "label", currentQuarter.label, "start",
                currentQuarter.start.toString(), "end", currentQuarter.end.toString(),
                "poolIncludesRepeatInspectionThisQuarter", includeAllQcVisits,
                "whenPoolExcludesCompletedThisQuarter", !includeAllQcVisits));
        response.put("scopeStats", buildScopeStats(poolResult.inScope, freezer, shelf, rack, box));
        response.put("diagnostics", poolResult.diagnostics);
        return response;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> listSamplesForQcTable(Integer notebookId) {
        BiorepositoryHierarchyContext hierarchy = resolveBiorepositoryHierarchy(notebookId);
        PoolBuildResult poolResult = buildPool(notebookId, hierarchy);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PooledSampleEntry entry : poolResult.inScope) {
            Map<String, Object> sampleData = new HashMap<>();
            BioSample bioSample = entry.bioSample;
            SampleItem sampleItem = entry.sampleItem;

            sampleData.put("bioSampleId", bioSample.getId());
            sampleData.put("workflowStatus", bioSample.getWorkflowStatus().name());
            sampleData.put("biosafetyLevel",
                    bioSample.getBiosafetyLevel() != null ? bioSample.getBiosafetyLevel().name() : null);
            if (bioSample.getProjectId() != null) {
                sampleData.put("projectId", bioSample.getProjectId());
            }

            sampleData.put("sampleItemId", sampleItem.getId());
            sampleData.put("externalId", sampleItem.getExternalId());
            if (sampleItem.getTypeOfSample() != null) {
                sampleData.put("sampleType", sampleItem.getTypeOfSample().getDescription());
                sampleData.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
            }
            if (sampleItem.getSample() != null) {
                sampleData.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
            }

            Map<String, Object> storageLocation = new HashMap<>();
            storageLocation.put("hierarchicalPath", entry.locationPath);
            storageLocation.put("positionCoordinate", entry.positionCoordinate);
            sampleData.put("storageLocation", storageLocation);
            sampleData.put("locationPath", entry.locationPath);

            BiorepositoryQCInspection mostRecent = qcInspectionService.getMostRecentByBioSampleId(bioSample.getId());
            if (mostRecent != null) {
                Map<String, Object> mappedInspection = mapQCInspectionSummary(mostRecent);
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
        return result;
    }

    private PoolBuildResult buildPool(Integer notebookId, BiorepositoryHierarchyContext hierarchy) {
        Set<Integer> departmentIds = resolveQcDepartmentScope(notebookId);
        String departmentNameHint = resolveQcDepartmentNameHint(notebookId);
        Set<String> hierarchyDeviceNames = new HashSet<>();
        for (StorageDevice device : hierarchy.devices) {
            String name = deviceName(device);
            if (name != null && !name.isBlank()) {
                hierarchyDeviceNames.add(name.toLowerCase(Locale.ROOT));
            }
        }

        CalendarQuarter currentQuarter = resolveCurrentCalendarQuarter(ZoneId.systemDefault());
        List<Map<String, Object>> assignmentRows = sampleStorageService.getAllSamplesWithAssignments();

        int storageManagementActiveInScope = 0;
        int excludedNotInScope = 0;
        int excludedNoBioSample = 0;
        int bioSamplesLazyLinked = 0;
        List<PooledSampleEntry> inScope = new ArrayList<>();

        for (Map<String, Object> row : assignmentRows) {
            String locationPath = asString(row.get("location"));
            if (locationPath == null || locationPath.isBlank()) {
                continue;
            }
            if (!isActiveAssignmentRow(row)) {
                continue;
            }
            if (!isInBiorepositoryScope(row, departmentIds, departmentNameHint, hierarchy, hierarchyDeviceNames)) {
                excludedNotInScope++;
                continue;
            }
            storageManagementActiveInScope++;

            String sampleItemIdStr = asString(row.get("id"));
            if (sampleItemIdStr == null || sampleItemIdStr.isBlank()) {
                excludedNoBioSample++;
                continue;
            }
            SampleItem sampleItem = sampleStorageService.resolveSampleItemByIdentifier(sampleItemIdStr);
            if (sampleItem == null) {
                excludedNoBioSample++;
                continue;
            }

            BioSample bioSample = bioSampleService.getBySampleItemId(parseSampleItemId(sampleItemIdStr));
            if (bioSample == null) {
                bioSample = bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, null);
                if (bioSample != null) {
                    bioSamplesLazyLinked++;
                }
            }
            if (bioSample == null) {
                excludedNoBioSample++;
                continue;
            }

            String locationPathForParse = locationPath;
            String[] levels = resolveHierarchyLevels(locationPathForParse, hierarchy.validShelfKeys,
                    hierarchy.validRackKeys, hierarchy.validBoxKeys);

            boolean anyPriorInspection = qcInspectionService.existsByBioSampleId(bioSample.getId());
            boolean inspectedThisQuarter = qcInspectionService.hasInspectionBetween(bioSample.getId(),
                    currentQuarter.start, currentQuarter.end);

            String parsedFreezer = levels[0];
            String parsedShelf = levels[1];
            String parsedRack = levels[2];
            String parsedBox = levels[3];
            boolean hasParsedBox = parsedBox != null && !parsedBox.isBlank() && !"Unknown".equals(parsedBox);

            PooledSampleEntry entry = new PooledSampleEntry();
            entry.bioSample = bioSample;
            entry.sampleItem = sampleItem;
            entry.locationPath = locationPath;
            entry.positionCoordinate = asString(row.get("positionCoordinate"));
            entry.levels = levels;
            entry.anyPriorInspection = anyPriorInspection;
            entry.inspectedThisQuarter = inspectedThisQuarter;
            entry.shelfKey = buildHierarchyKey(parsedFreezer, parsedShelf);
            entry.boxKey = hasParsedBox
                    ? buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack, parsedBox)
                    : buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack);
            inScope.add(entry);
        }

        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("storageManagementActiveInScope", storageManagementActiveInScope);
        diagnostics.put("qcPoolTotal", inScope.size());
        diagnostics.put("qcEligibleCount", inScope.size());
        diagnostics.put("excludedNoBioSample", excludedNoBioSample);
        diagnostics.put("excludedNotInScope", excludedNotInScope);
        diagnostics.put("bioSamplesLazyLinked", bioSamplesLazyLinked);

        PoolBuildResult result = new PoolBuildResult();
        result.inScope = inScope;
        result.diagnostics = diagnostics;
        return result;
    }

    private boolean isActiveAssignmentRow(Map<String, Object> row) {
        String sampleStatusId = asString(row.get("status"));
        if (sampleStatusId == null || sampleStatusId.isEmpty()) {
            return true;
        }
        return !statusService.matches(sampleStatusId, SampleStatus.Disposed);
    }

    private boolean isInBiorepositoryScope(Map<String, Object> row, Set<Integer> departmentIds,
            String departmentNameHint, BiorepositoryHierarchyContext hierarchy,
            Set<String> hierarchyDeviceNames) {
        String locationPath = asString(row.get("location"));
        boolean departmentMatch = matchesDepartmentScope(row, departmentIds, departmentNameHint, locationPath);
        boolean requiresDepartment = !departmentIds.isEmpty()
                || (departmentNameHint != null && !departmentNameHint.isBlank());
        if (requiresDepartment && !departmentMatch) {
            return false;
        }
        if (departmentMatch && hierarchy.devices.isEmpty()) {
            return true;
        }
        String deviceName = asString(row.get("deviceName"));
        if (deviceName != null && hierarchyDeviceNames.contains(deviceName.toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (locationPath != null && matchesBiorepositoryHierarchy(locationPath, hierarchy.validShelfKeys,
                hierarchy.validRackKeys, hierarchy.validBoxKeys)) {
            return true;
        }
        if (departmentMatch && departmentNameHint != null && locationPath != null
                && locationPath.toLowerCase(Locale.ROOT).contains(departmentNameHint.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return departmentMatch && !requiresDepartment;
    }

    private boolean matchesDepartmentScope(Map<String, Object> row, Set<Integer> departmentIds,
            String departmentNameHint, String locationPath) {
        if (departmentIds.isEmpty() && (departmentNameHint == null || departmentNameHint.isBlank())) {
            return false;
        }
        Object sampleDepartment = row.get("departmentTestSectionId");
        if (sampleDepartment != null && !departmentIds.isEmpty()) {
            try {
                int deptId = Integer.parseInt(String.valueOf(sampleDepartment));
                if (departmentIds.contains(deptId)) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        String departmentName = asString(row.get("departmentName"));
        if (departmentName != null && !departmentIds.isEmpty()) {
            String normalizedName = departmentName.toLowerCase(Locale.ROOT);
            if (normalizedName.contains("biorepository")) {
                return true;
            }
        }
        if (departmentNameHint != null && !departmentNameHint.isBlank()) {
            if (departmentName != null
                    && departmentName.toLowerCase(Locale.ROOT).contains(departmentNameHint.toLowerCase(Locale.ROOT))) {
                return true;
            }
            if (locationPath != null
                    && locationPath.toLowerCase(Locale.ROOT).contains(departmentNameHint.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        if (locationPath != null && locationPath.toLowerCase(Locale.ROOT).contains("biorepository")) {
            return true;
        }
        return false;
    }

    private String[] resolveHierarchyLevels(String locationPath, Set<String> validShelfKeys, Set<String> validRackKeys,
            Set<String> validBoxKeys) {
        if (locationPath == null || locationPath.isBlank()) {
            return null;
        }
        String[] levels = BiorepositoryQcHierarchyParser.parseHierarchyLevels(locationPath);
        if (matchesBiorepositoryHierarchy(locationPath, validShelfKeys, validRackKeys, validBoxKeys)) {
            return levels;
        }
        String[] roomPrefixed = BiorepositoryQcHierarchyParser.parseRoomPrefixedRackLevels(locationPath);
        String parsedFreezer = roomPrefixed[0];
        String parsedShelf = roomPrefixed[1];
        String parsedRack = roomPrefixed[2];
        if (validShelfKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf))
                && validRackKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack))) {
            return roomPrefixed;
        }
        // Department-only scope: still parse coordinates for filters/display
        return levels;
    }

    private Map<String, Object> buildScopeStats(List<PooledSampleEntry> inScope, String freezerFilter,
            String shelfFilter, String rackFilter, String boxFilter) {
        int totalStoredInScope = 0;
        int verified = 0;
        int discrepancies = 0;
        int pending = 0;

        for (PooledSampleEntry entry : inScope) {
            String[] levels = entry.levels;
            if (!matches(levels[0], freezerFilter) || !matches(levels[1], shelfFilter)
                    || !matches(levels[2], rackFilter) || !matches(levels[3], boxFilter)) {
                continue;
            }
            totalStoredInScope++;
            BiorepositoryQCInspection mostRecent =
                    qcInspectionService.getMostRecentByBioSampleId(entry.bioSample.getId());
            if (mostRecent == null) {
                pending++;
            } else if (BiorepositoryQCInspection.QCResult.VERIFIED.equals(mostRecent.getQcResult())) {
                verified++;
            } else if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(mostRecent.getQcResult())) {
                discrepancies++;
            } else {
                pending++;
            }
        }

        int inspected = verified + discrepancies;
        double passRate = inspected > 0 ? (verified * 100.0 / inspected) : 0.0;

        Map<String, Object> scopeStats = new HashMap<>();
        scopeStats.put("totalStored", totalStoredInScope);
        scopeStats.put("verified", verified);
        scopeStats.put("discrepancies", discrepancies);
        scopeStats.put("pending", pending);
        scopeStats.put("passRatePercent", passRate);
        return scopeStats;
    }

    private Map<String, Object> mapQCInspectionSummary(BiorepositoryQCInspection inspection) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("id", inspection.getId());
        mapped.put("lastQCDate", inspection.getInspectionDate());
        String qcStatus = BiorepositoryQCInspection.QCResult.VERIFIED.equals(inspection.getQcResult()) ? "VALID"
                : "INVALID";
        mapped.put("qcStatus", qcStatus);
        mapped.put("sampleFlag", "VALID".equals(qcStatus) ? "QC_VALID" : "QC_FAILED");
        mapped.put("qcFailed", !"VALID".equals(qcStatus));
        return mapped;
    }

    private Integer parseSampleItemId(String sampleItemIdStr) {
        try {
            return Integer.valueOf(sampleItemIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * QC scope always includes the Biorepository Laboratory department (this service is biorepository-only),
     * unioned with notebook-linked departments when notebookId is present.
     */
    private Set<Integer> resolveQcDepartmentScope(Integer notebookId) {
        Set<Integer> ids = new HashSet<>();
        if (notebookId != null) {
            ids.addAll(resolveNotebookDepartmentIds(notebookId));
        }
        TestSection biorepositoryDepartment = testSectionService.getTestSectionByName("Biorepository Laboratory");
        if (biorepositoryDepartment == null) {
            biorepositoryDepartment = resolveTestSectionByTemplateTitle("Biorepository Laboratory");
        }
        Integer biorepositoryDepartmentId = parseDepartmentId(biorepositoryDepartment);
        if (biorepositoryDepartmentId != null) {
            ids.add(biorepositoryDepartmentId);
        }
        return ids;
    }

    private String resolveQcDepartmentNameHint(Integer notebookId) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        hints.add("Biorepository Laboratory");
        String notebookHint = resolveNotebookDepartmentNameHint(notebookId);
        if (notebookHint != null && !notebookHint.isBlank()) {
            hints.add(notebookHint.trim());
        }
        return hints.iterator().next();
    }

    private String resolveNotebookDepartmentNameHint(Integer notebookId) {
        if (notebookId == null) {
            return null;
        }
        Set<TestSection> departments = noteBookService.getNoteBookDepartments(notebookId);
        if (departments != null) {
            for (TestSection department : departments) {
                if (department.getTestSectionName() != null && !department.getTestSectionName().isBlank()) {
                    return department.getTestSectionName().trim();
                }
                if (department.getLocalizedName() != null && !department.getLocalizedName().isBlank()) {
                    return department.getLocalizedName().trim();
                }
            }
        }
        NoteBook notebook = noteBookService.get(notebookId);
        return normalizeNotebookDepartmentTitle(notebook != null ? notebook.getTitle() : null);
    }

    // --- Hierarchy resolution (aligned with BiorepositoryQCInspectionRestController) ---

    private BiorepositoryHierarchyContext resolveBiorepositoryHierarchy(Integer notebookId) {
        List<StorageDevice> eligibleDevices = storageLocationService.getAllDevices().stream()
                .filter(this::isActive)
                .filter(this::isQCEligibleDevice)
                .toList();
        boolean hasFlaggedDevices = eligibleDevices.stream().anyMatch(this::isBiorepositoryStorageDevice);

        List<StorageDevice> devices = resolveBiorepositoryDevices(notebookId);
        boolean usedDepartmentFallback = !hasFlaggedDevices && notebookId != null && !devices.isEmpty();

        Set<Integer> activeDeviceIds = devices.stream().map(StorageDevice::getId)
                .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());

        List<StorageShelf> shelves = storageLocationService.getAllShelves().stream()
                .filter(this::isActive)
                .filter(shelfEntity -> shelfEntity.getParentDevice() != null
                        && activeDeviceIds.contains(shelfEntity.getParentDevice().getId()))
                .toList();

        Set<Integer> activeShelfIds = shelves.stream().map(StorageShelf::getId)
                .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());

        List<StorageRack> racks = storageLocationService.getAllRacks().stream()
                .filter(this::isActive)
                .filter(rackEntity -> rackEntity.getParentShelf() != null
                        && activeShelfIds.contains(rackEntity.getParentShelf().getId()))
                .toList();

        Set<Integer> activeRackIds = racks.stream().map(StorageRack::getId)
                .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());

        List<StorageBox> boxes = storageLocationService.getAllBoxes().stream()
                .filter(this::isActive)
                .filter(boxEntity -> boxEntity.getParentRack() != null
                        && activeRackIds.contains(boxEntity.getParentRack().getId()))
                .toList();

        Set<String> validShelfKeys = new LinkedHashSet<>();
        for (StorageShelf shelfEntity : shelves) {
            StorageDevice parentDevice = shelfEntity.getParentDevice();
            String freezerName = parentDevice != null ? deviceName(parentDevice) : null;
            String shelfLabel = shelfEntity.getLabel();
            if (freezerName != null && shelfLabel != null && !shelfLabel.isBlank()) {
                validShelfKeys.add(buildHierarchyKey(freezerName, shelfLabel));
            }
        }

        Set<String> validRackKeys = new LinkedHashSet<>();
        for (StorageRack rackEntity : racks) {
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
        for (StorageBox boxEntity : boxes) {
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

        BiorepositoryHierarchyContext context = new BiorepositoryHierarchyContext();
        context.devices = devices;
        context.shelves = shelves;
        context.racks = racks;
        context.boxes = boxes;
        context.validShelfKeys = validShelfKeys;
        context.validRackKeys = validRackKeys;
        context.validBoxKeys = validBoxKeys;
        context.usedDepartmentFallback = usedDepartmentFallback;
        return context;
    }

    private List<StorageDevice> resolveBiorepositoryDevices(Integer notebookId) {
        List<StorageDevice> eligible = storageLocationService.getAllDevices().stream()
                .filter(this::isActive)
                .filter(this::isQCEligibleDevice)
                .toList();

        List<StorageDevice> flagged = eligible.stream()
                .filter(this::isBiorepositoryStorageDevice)
                .toList();
        if (!flagged.isEmpty()) {
            return flagged;
        }

        if (notebookId == null) {
            return List.of();
        }

        Set<Integer> departmentIds = resolveQcDepartmentScope(notebookId);
        if (departmentIds.isEmpty()) {
            return List.of();
        }

        return eligible.stream()
                .filter(device -> {
                    if (device.getParentRoom() == null) {
                        return false;
                    }
                    Integer deptId = device.getParentRoom().getDepartmentTestSectionId();
                    return deptId != null && departmentIds.contains(deptId);
                })
                .toList();
    }

    private Set<Integer> resolveNotebookDepartmentIds(Integer notebookId) {
        Set<Integer> ids = new HashSet<>();
        Set<TestSection> departments = noteBookService.getNoteBookDepartments(notebookId);
        if (departments != null) {
            for (TestSection department : departments) {
                Integer id = parseDepartmentId(department);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        if (!ids.isEmpty()) {
            return ids;
        }

        NoteBook notebook = noteBookService.get(notebookId);
        String title = notebook != null ? notebook.getTitle() : null;
        TestSection resolvedFromTitle = resolveTestSectionByTemplateTitle(normalizeNotebookDepartmentTitle(title));
        Integer resolvedId = parseDepartmentId(resolvedFromTitle);
        if (resolvedId != null) {
            ids.add(resolvedId);
        }
        return ids;
    }

    private String normalizeNotebookDepartmentTitle(String title) {
        if (title == null) {
            return null;
        }
        return title.trim().replaceFirst("\\s+-\\s+Lab\\s+\\d+.*$", "").replaceFirst("\\s+-\\s+Entry\\s+#?\\d+.*$", "");
    }

    private TestSection resolveTestSectionByTemplateTitle(String notebookTitle) {
        if (notebookTitle == null || notebookTitle.isBlank()) {
            return null;
        }
        TestSection byName = testSectionService.getTestSectionByName(notebookTitle.trim());
        if (byName != null) {
            return byName;
        }
        List<TestSection> activeSections = testSectionService.getAllActiveTestSections();
        if (activeSections == null || activeSections.isEmpty()) {
            return null;
        }
        return activeSections.stream()
                .filter(section -> templateTitleMatchesDepartment(notebookTitle, section))
                .findFirst()
                .orElse(null);
    }

    private boolean templateTitleMatchesDepartment(String notebookTitle, TestSection department) {
        if (notebookTitle == null || department == null) {
            return false;
        }
        String normalizedTitle = notebookTitle.trim();
        if (department.getTestSectionName() != null
                && normalizedTitle.equalsIgnoreCase(department.getTestSectionName().trim())) {
            return true;
        }
        return department.getLocalizedName() != null
                && normalizedTitle.equalsIgnoreCase(department.getLocalizedName().trim());
    }

    private Integer parseDepartmentId(TestSection department) {
        if (department == null || department.getId() == null) {
            return null;
        }
        try {
            return Integer.valueOf(department.getId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean matchesBiorepositoryHierarchy(String locationPath, Set<String> validShelfKeys,
            Set<String> validRackKeys, Set<String> validBoxKeys) {
        if (locationPath == null || locationPath.isBlank()) {
            return false;
        }
        String[] levels = BiorepositoryQcHierarchyParser.parseHierarchyLevels(locationPath);
        String parsedFreezer = levels[0];
        String parsedShelf = levels[1];
        String parsedRack = levels[2];
        String parsedBox = levels[3];

        if (!validShelfKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf))
                || !validRackKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack))) {
            String[] roomPrefixedRackLevels = BiorepositoryQcHierarchyParser.parseRoomPrefixedRackLevels(locationPath);
            if (!validShelfKeys.contains(buildHierarchyKey(roomPrefixedRackLevels[0], roomPrefixedRackLevels[1]))
                    || !validRackKeys.contains(buildHierarchyKey(roomPrefixedRackLevels[0], roomPrefixedRackLevels[1],
                            roomPrefixedRackLevels[2]))) {
                return false;
            }
            levels = roomPrefixedRackLevels;
            parsedFreezer = levels[0];
            parsedShelf = levels[1];
            parsedRack = levels[2];
            parsedBox = levels[3];
        }

        if (!validShelfKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf))) {
            return false;
        }
        if (!validRackKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack))) {
            return false;
        }
        boolean hasParsedBox = parsedBox != null && !parsedBox.isBlank() && !"Unknown".equals(parsedBox);
        return !hasParsedBox
                || validBoxKeys.contains(buildHierarchyKey(parsedFreezer, parsedShelf, parsedRack, parsedBox));
    }

    private boolean isQCEligibleDevice(StorageDevice device) {
        return isActive(device) && deviceName(device) != null;
    }

    private boolean isBiorepositoryStorageDevice(StorageDevice device) {
        return device != null && Boolean.TRUE.equals(device.getBiorepositoryStorage());
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

    private boolean matches(String value, String filter) {
        if (filter == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return filter.equals(value);
    }

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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private static final class BiorepositoryHierarchyContext {
        List<StorageDevice> devices = List.of();
        List<StorageShelf> shelves = List.of();
        List<StorageRack> racks = List.of();
        List<StorageBox> boxes = List.of();
        Set<String> validShelfKeys = Set.of();
        Set<String> validRackKeys = Set.of();
        Set<String> validBoxKeys = Set.of();
        boolean usedDepartmentFallback;
    }

    private static final class PoolBuildResult {
        List<PooledSampleEntry> inScope = List.of();
        Map<String, Object> diagnostics = Map.of();
    }

    private static final class PooledSampleEntry {
        BioSample bioSample;
        SampleItem sampleItem;
        String locationPath;
        String positionCoordinate;
        String[] levels;
        boolean anyPriorInspection;
        boolean inspectedThisQuarter;
        String shelfKey;
        String boxKey;

        Map<String, Object> toEligibleSummary() {
            Map<String, Object> sampleSummary = new HashMap<>();
            sampleSummary.put("bioSampleId", bioSample.getId());
            sampleSummary.put("sampleItemId", sampleItem.getId());
            sampleSummary.put("freezer", levels[0]);
            sampleSummary.put("shelf", levels[1]);
            sampleSummary.put("rack", levels[2]);
            sampleSummary.put("box", levels[3]);
            sampleSummary.put("locationPath", locationPath);
            sampleSummary.put("positionCoordinate", positionCoordinate);
            sampleSummary.put("accessionNumber",
                    sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);
            sampleSummary.put("anyPriorInspection", anyPriorInspection);
            sampleSummary.put("hasInspectionHistory", anyPriorInspection);
            sampleSummary.put("inspectedThisQuarter", inspectedThisQuarter);
            sampleSummary.put("shelfKey", shelfKey);
            sampleSummary.put("boxKey", boxKey);
            return sampleSummary;
        }
    }
}
