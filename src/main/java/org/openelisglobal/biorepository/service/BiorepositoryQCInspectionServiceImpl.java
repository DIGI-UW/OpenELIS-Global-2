package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.biorepository.dao.BiorepositoryQCInspectionDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.DiscrepancyType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for BiorepositoryQCInspection entity operations.
 */
@Service
public class BiorepositoryQCInspectionServiceImpl extends
        AuditableBaseObjectServiceImpl<BiorepositoryQCInspection, Integer> implements BiorepositoryQCInspectionService {

    @Autowired
    protected BiorepositoryQCInspectionDAO baseObjectDAO;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private ChainOfCustodyService chainOfCustodyService;

    BiorepositoryQCInspectionServiceImpl() {
        super(BiorepositoryQCInspection.class);
    }

    @Override
    protected BiorepositoryQCInspectionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiorepositoryQCInspection> getByBioSampleId(Integer bioSampleId) {
        return baseObjectDAO.getByBioSampleId(bioSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public BiorepositoryQCInspection getMostRecentByBioSampleId(Integer bioSampleId) {
        return baseObjectDAO.getMostRecentByBioSampleId(bioSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, BiorepositoryQCInspection> getMostRecentByBioSampleIds(List<Integer> bioSampleIds) {
        return baseObjectDAO.getMostRecentByBioSampleIds(bioSampleIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiorepositoryQCInspection> getByQCResult(QCResult qcResult) {
        return baseObjectDAO.getByQCResult(qcResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiorepositoryQCInspection> getByInspectorName(String inspectorName) {
        return baseObjectDAO.getByInspectorName(inspectorName);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByQCResult(QCResult qcResult) {
        return baseObjectDAO.countByQCResult(qcResult);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBioSampleId(Integer bioSampleId) {
        return baseObjectDAO.existsByBioSampleId(bioSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiorepositoryQCInspection> getInspectionsByDateRange(Timestamp startDate, Timestamp endDate) {
        return baseObjectDAO.getInspectionsByDateRange(startDate, endDate);
    }

    @Override
    @Transactional
    public BiorepositoryQCInspection createInspection(Integer bioSampleId, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String qcBatchId, String expectedCoordinateSnapshot,
            String sysUserId) {

        BioSample bioSample = bioSampleService.get(bioSampleId);
        if (bioSample == null) {
            throw new IllegalArgumentException("BioSample not found: " + bioSampleId);
        }
        if (!BioSample.WorkflowStatus.STORED.equals(bioSample.getWorkflowStatus())) {
            throw new IllegalArgumentException("QC inspection can only be recorded for STORED samples. Current "
                    + "status: " + bioSample.getWorkflowStatus());
        }
        if (inspectorName == null || inspectorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Inspector name is required");
        }

        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setBioSample(bioSample);
        inspection.setInspectorName(inspectorName.trim());
        inspection
                .setInspectionDate(inspectionDate != null ? inspectionDate : new Timestamp(System.currentTimeMillis()));
        inspection.setSamplePresent(samplePresent);
        inspection.setLabelIntegrity(labelIntegrity);
        inspection.setContainerIntegrity(containerIntegrity);
        inspection.setVolumeAppearanceAcceptable(volumeAppearanceAcceptable);
        inspection.setCorrectPosition(correctPosition);
        inspection.setQcBatchId(qcBatchId);
        inspection.setExpectedCoordinateSnapshot(
                resolveExpectedCoordinateSnapshot(bioSample, expectedCoordinateSnapshot));

        // Auto-calculate QC result based on checklist
        inspection.updateQcResult();

        // Set discrepancy details if QC failed
        if (inspection.getQcResult() == QCResult.DISCREPANCY_FOUND) {
            DiscrepancyType type = DiscrepancyType.fromString(discrepancyType);
            if (type == null) {
                throw new IllegalArgumentException("Discrepancy type is required when QC fails");
            }
            if (correctiveAction == null || correctiveAction.trim().isEmpty()) {
                throw new IllegalArgumentException("Corrective action is required when QC fails");
            }
            if (remarks == null || remarks.trim().isEmpty()) {
                throw new IllegalArgumentException("Comment/remarks are required when QC fails");
            }
            inspection.setDiscrepancyType(type);
            inspection.setCorrectiveAction(correctiveAction.trim());
        } else {
            inspection.setDiscrepancyType(null);
            inspection.setCorrectiveAction(null);
        }

        inspection.setRemarks(remarks);
        inspection.setSysUserId(sysUserId);

        return save(inspection);
    }

    @Override
    @Transactional
    public List<BiorepositoryQCInspection> createBulkInspections(List<Integer> bioSampleIds, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String qcBatchId, String expectedCoordinateSnapshot,
            String sysUserId) {

        List<BiorepositoryQCInspection> inspections = new ArrayList<>();

        for (Integer bioSampleId : bioSampleIds) {
            BiorepositoryQCInspection inspection = createInspection(bioSampleId, inspectorName, inspectionDate,
                    samplePresent, labelIntegrity, containerIntegrity, volumeAppearanceAcceptable, correctPosition,
                    discrepancyType, correctiveAction, remarks, qcBatchId, expectedCoordinateSnapshot, sysUserId);
            inspections.add(inspection);
        }

        return inspections;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateRandomQCRound(int boxesPerRound, int samplesPerBox, Long randomSeed,
            String sysUserId) {
        return generateRandomQCRound(boxesPerRound, samplesPerBox, randomSeed, sysUserId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateRandomQCRound(int boxesPerRound, int samplesPerBox, Long randomSeed,
            String sysUserId, Map<String, String> locationFilters) {
        int effectiveBoxes = boxesPerRound > 0 ? boxesPerRound : 10;
        int effectiveSamplesPerBox = samplesPerBox > 0 ? samplesPerBox : 3;
        Random random = randomSeed != null ? new Random(randomSeed) : new Random();
        String qcBatchId = "QCBATCH-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8);

        List<Map<String, Object>> candidates = collectStoredSampleCandidates(locationFilters);

        Map<String, List<Map<String, Object>>> candidatesByBox = new HashMap<>();
        for (Map<String, Object> candidate : candidates) {
            String boxKey = asString(candidate.get("boxKey"));
            if (boxKey == null) {
                continue;
            }
            candidatesByBox.computeIfAbsent(boxKey, k -> new ArrayList<>()).add(candidate);
        }

        List<String> selectedBoxes = selectDistributedBoxesByShelf(candidatesByBox, effectiveBoxes, random);

        List<Map<String, Object>> selectedSamples = new ArrayList<>();
        for (String boxKey : selectedBoxes) {
            List<Map<String, Object>> boxCandidates = new ArrayList<>(candidatesByBox.get(boxKey));
            Collections.shuffle(boxCandidates, random);
            int limit = Math.min(effectiveSamplesPerBox, boxCandidates.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> selected = new HashMap<>(boxCandidates.get(i));
                selected.put("qcBatchId", qcBatchId);
                selectedSamples.add(selected);
            }
        }

        selectedSamples.sort(
                Comparator.comparing(m -> asString(m.get("boxKey")) + ":" + asString(m.get("expectedCoordinate"))));

        Map<String, Object> result = new HashMap<>();
        result.put("qcBatchId", qcBatchId);
        result.put("boxesSelected", selectedBoxes.size());
        result.put("samplesSelected", selectedSamples.size());
        result.put("boxesPerRound", effectiveBoxes);
        result.put("samplesPerBox", effectiveSamplesPerBox);
        result.put("seed", randomSeed);
        result.put("filters", locationFilters != null ? locationFilters : Collections.emptyMap());
        result.put("generatedBy", sysUserId);
        result.put("samples", selectedSamples);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLocationOverview() {
        List<Map<String, Object>> candidates = collectStoredSampleCandidates(null);
        List<Map<String, Object>> freezerDevices = new ArrayList<>();
        for (Map<String, Object> device : storageLocationService.getDevicesForAPI(null)) {
            if (isBiorepositoryFreezer(device)) {
                freezerDevices.add(device);
            }
        }

        Set<String> freezerNames = new HashSet<>();
        Set<String> configuredShelfKeys = new HashSet<>();
        Set<String> configuredRackKeys = new HashSet<>();
        Set<String> configuredBoxKeys = new HashSet<>();
        List<Map<String, Object>> freezerOptions = new ArrayList<>();
        List<Map<String, Object>> shelfOptions = new ArrayList<>();
        List<Map<String, Object>> rackOptions = new ArrayList<>();
        List<Map<String, Object>> boxOptions = new ArrayList<>();

        for (Map<String, Object> device : freezerDevices) {
            String freezerName = asString(device.get("name"));
            Integer deviceId = asInteger(device.get("id"));
            if (freezerName == null || deviceId == null) {
                continue;
            }

            freezerNames.add(freezerName);
            freezerOptions.add(optionRow(freezerName, freezerName, null, null, null));

            for (Map<String, Object> shelf : storageLocationService.getShelvesForAPI(deviceId)) {
                if (!Boolean.TRUE.equals(shelf.get("biorepositoryStorage")) || !Boolean.TRUE.equals(shelf.get("active"))) {
                    continue;
                }
                String shelfLabel = asString(shelf.get("label"));
                if (shelfLabel == null) {
                    continue;
                }
                String shelfKey = freezerName + " > " + shelfLabel;
                configuredShelfKeys.add(shelfKey);
                shelfOptions.add(optionRow(shelfLabel, shelfLabel, freezerName, null, null));

                Integer shelfId = asInteger(shelf.get("id"));
                if (shelfId == null) {
                    continue;
                }

                for (Map<String, Object> rack : storageLocationService.getRacksForAPI(shelfId)) {
                    if (!Boolean.TRUE.equals(rack.get("biorepositoryStorage")) || !Boolean.TRUE.equals(rack.get("active"))) {
                        continue;
                    }
                    String rackLabel = asString(rack.get("rackLabel"));
                    if (rackLabel == null) {
                        rackLabel = asString(rack.get("label"));
                    }
                    if (rackLabel == null) {
                        continue;
                    }
                    String rackKey = shelfKey + " > " + rackLabel;
                    configuredRackKeys.add(rackKey);
                    rackOptions.add(optionRow(rackLabel, rackLabel, freezerName, shelfLabel, null));
                }
            }
        }

        for (Map<String, Object> box : storageLocationService.getBoxesForAPI(null)) {
            if (!Boolean.TRUE.equals(box.get("biorepositoryStorage")) || !Boolean.TRUE.equals(box.get("active"))) {
                continue;
            }
            String deviceName = asString(box.get("deviceName"));
            String shelfLabel = asString(box.get("shelfLabel"));
            String rackLabel = asString(box.get("rackLabel"));
            String boxLabel = asString(box.get("label"));
            if (deviceName == null || shelfLabel == null || rackLabel == null || boxLabel == null) {
                continue;
            }
            if (!freezerNames.contains(deviceName)) {
                continue;
            }
            configuredBoxKeys.add(deviceName + " > " + shelfLabel + " > " + rackLabel + " > " + boxLabel);
            boxOptions.add(optionRow(boxLabel, boxLabel, deviceName, shelfLabel, rackLabel));
        }

        Map<String, Integer> freezerSampleCount = new HashMap<>();
        Map<String, Set<String>> freezerShelfSet = new HashMap<>();
        Map<String, Set<String>> freezerRackSet = new HashMap<>();
        Map<String, Set<String>> freezerBoxSet = new HashMap<>();
        Map<String, Integer> shelfSampleCount = new HashMap<>();
        Map<String, Set<String>> shelfRackSet = new HashMap<>();
        Map<String, Set<String>> shelfBoxSet = new HashMap<>();
        Map<String, String> shelfToFreezer = new HashMap<>();
        Map<String, String> shelfLabelMap = new HashMap<>();

        for (Map<String, Object> candidate : candidates) {
            String freezer = asString(candidate.get("freezer"));
            String shelf = asString(candidate.get("shelf"));
            String shelfKey = asString(candidate.get("shelfKey"));
            String rackKey = asString(candidate.get("rackKey"));
            String boxKey = asString(candidate.get("boxKey"));
            if (freezer == null) {
                continue;
            }

            freezerSampleCount.merge(freezer, 1, Integer::sum);
            freezerShelfSet.computeIfAbsent(freezer, key -> new HashSet<>());
            freezerRackSet.computeIfAbsent(freezer, key -> new HashSet<>());
            freezerBoxSet.computeIfAbsent(freezer, key -> new HashSet<>());

            if (shelfKey != null) {
                freezerShelfSet.get(freezer).add(shelfKey);
            }
            if (rackKey != null) {
                freezerRackSet.get(freezer).add(rackKey);
            }
            if (boxKey != null) {
                freezerBoxSet.get(freezer).add(boxKey);
            }

            if (shelfKey != null) {
                shelfToFreezer.putIfAbsent(shelfKey, freezer);
                shelfLabelMap.putIfAbsent(shelfKey, shelf != null ? shelf : "Unknown Shelf");
                shelfSampleCount.merge(shelfKey, 1, Integer::sum);
                shelfRackSet.computeIfAbsent(shelfKey, key -> new HashSet<>());
                shelfBoxSet.computeIfAbsent(shelfKey, key -> new HashSet<>());
                if (rackKey != null) {
                    shelfRackSet.get(shelfKey).add(rackKey);
                }
                if (boxKey != null) {
                    shelfBoxSet.get(shelfKey).add(boxKey);
                }
            }
        }

        List<Map<String, Object>> freezerSummary = new ArrayList<>();
        for (String freezerName : freezerNames) {
            Map<String, Object> summaryRow = new HashMap<>();
            summaryRow.put("freezer", freezerName);
            summaryRow.put("sampleCount", freezerSampleCount.getOrDefault(freezerName, 0));
            summaryRow.put("shelfCount", countMatchingPrefix(configuredShelfKeys, freezerName + " > "));
            summaryRow.put("rackCount", countMatchingPrefix(configuredRackKeys, freezerName + " > "));
            summaryRow.put("boxCount", countMatchingPrefix(configuredBoxKeys, freezerName + " > "));
            freezerSummary.add(summaryRow);
        }

        List<Map<String, Object>> shelfSummary = new ArrayList<>();
        for (String shelfKey : configuredShelfKeys) {
            Map<String, Object> summaryRow = new HashMap<>();
            summaryRow.put("freezer", shelfToFreezer.getOrDefault(shelfKey, prefixBeforeLastSeparator(shelfKey)));
            summaryRow.put("shelf", shelfLabelMap.getOrDefault(shelfKey, suffixAfterLastSeparator(shelfKey)));
            summaryRow.put("shelfKey", shelfKey);
            summaryRow.put("sampleCount", shelfSampleCount.getOrDefault(shelfKey, 0));
            summaryRow.put("rackCount", countMatchingPrefix(configuredRackKeys, shelfKey + " > "));
            summaryRow.put("boxCount", countMatchingPrefix(configuredBoxKeys, shelfKey + " > "));
            shelfSummary.add(summaryRow);
        }

        freezerSummary.sort(Comparator.comparing(row -> asString(row.get("freezer"))));
        shelfSummary.sort(Comparator.comparing(row -> asString(row.get("shelfKey"))));
        freezerOptions.sort(Comparator.comparing(row -> asString(row.get("label"))));
        shelfOptions.sort(Comparator.comparing(row -> asString(row.get("freezer")) + ":" + asString(row.get("label"))));
        rackOptions.sort(Comparator.comparing(
                row -> asString(row.get("freezer")) + ":" + asString(row.get("shelf")) + ":" + asString(row.get("label"))));
        boxOptions.sort(Comparator.comparing(row -> asString(row.get("freezer")) + ":" + asString(row.get("shelf"))
                + ":" + asString(row.get("rack")) + ":" + asString(row.get("label"))));

        Map<String, Object> result = new HashMap<>();
        result.put("totalStoredSamples", candidates.size());
        result.put("freezerCount", freezerNames.size());
        result.put("shelfCount", configuredShelfKeys.size());
        result.put("rackCount", configuredRackKeys.size());
        result.put("boxCount", configuredBoxKeys.size());
        result.put("freezerSummary", freezerSummary);
        result.put("shelfSummary", shelfSummary);
        result.put("freezerOptions", freezerOptions);
        result.put("shelfOptions", shelfOptions);
        result.put("rackOptions", rackOptions);
        result.put("boxOptions", boxOptions);
        result.put("generatedAt", System.currentTimeMillis());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> applyCorrectiveAction(Integer inspectionId, String observedCoordinate,
            String correctedCoordinate, String correctiveReason, String sysUserId) {
        BiorepositoryQCInspection inspection = get(inspectionId);
        if (inspection == null) {
            throw new IllegalArgumentException("QC inspection not found: " + inspectionId);
        }
        if (!QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
            throw new IllegalStateException("Corrective action is only allowed for failed QC inspections");
        }
        if (correctiveReason == null || correctiveReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Corrective reason is required");
        }
        if (correctedCoordinate == null || correctedCoordinate.trim().isEmpty()) {
            throw new IllegalArgumentException("Corrected coordinate is required");
        }

        SampleItem sampleItem = inspection.getBioSample() != null ? inspection.getBioSample().getSampleItem() : null;
        if (sampleItem == null || sampleItem.getId() == null) {
            throw new IllegalStateException("QC inspection sample is missing sample item linkage");
        }

        Map<String, Object> before = sampleStorageService.getSampleItemLocation(sampleItem.getId());
        String previousCoordinate = before != null ? asString(before.get("positionCoordinate")) : null;
        String previousPath = before != null ? asString(before.get("hierarchicalPath")) : null;

        sampleStorageService.updateAssignmentMetadata(sampleItem.getId(), correctedCoordinate, correctiveReason);

        Map<String, Object> after = sampleStorageService.getSampleItemLocation(sampleItem.getId());
        String newCoordinate = after != null ? asString(after.get("positionCoordinate")) : null;
        String newPath = after != null ? asString(after.get("hierarchicalPath")) : null;

        String note = "QC corrective action. InspectionId=" + inspectionId + ", batchId="
                + asString(inspection.getQcBatchId()) + ", observed=" + asString(observedCoordinate) + ", old="
                + asString(previousCoordinate) + ", new=" + asString(newCoordinate) + ", reason="
                + correctiveReason.trim();

        chainOfCustodyService.logCustodyAction(sampleItem, CustodyAction.RETURN_INSPECTED, null, null, newCoordinate,
                null, previousPath, newPath, null, note, sysUserId);

        inspection.setCorrectiveAction(correctiveReason.trim());
        inspection.setRemarks((inspection.getRemarks() == null ? "" : inspection.getRemarks() + " | ")
                + "Corrected coordinate from " + asString(previousCoordinate) + " to " + asString(newCoordinate));
        inspection.setSysUserId(sysUserId);
        update(inspection);

        Map<String, Object> result = new HashMap<>();
        result.put("inspectionId", inspectionId);
        result.put("sampleItemId", sampleItem.getId());
        result.put("oldCoordinate", previousCoordinate);
        result.put("newCoordinate", newCoordinate);
        result.put("oldPath", previousPath);
        result.put("newPath", newPath);
        result.put("reason", correctiveReason.trim());
        return result;
    }

    private List<String> selectDistributedBoxesByShelf(Map<String, List<Map<String, Object>>> candidatesByBox,
            int boxesToSelect, Random random) {
        Map<String, List<String>> boxesByShelf = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : candidatesByBox.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }

            String shelfKey = asString(entry.getValue().get(0).get("shelfKey"));
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

    private List<Map<String, Object>> collectStoredSampleCandidates(Map<String, String> locationFilters) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        List<BioSample> storedSamples = bioSampleService.getAllWithRelationships(50000);
        List<String> sampleItemIds = new ArrayList<>();

        for (BioSample bioSample : storedSamples) {
            if (!BioSample.WorkflowStatus.STORED.equals(bioSample.getWorkflowStatus())) {
                continue;
            }
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem == null || sampleItem.getId() == null || sampleItem.getId().trim().isEmpty()) {
                continue;
            }
            sampleItemIds.add(sampleItem.getId().trim());
        }

        Map<String, Map<String, Object>> locationsBySampleItemId = sampleStorageService.getSampleItemLocations(sampleItemIds);

        for (BioSample bioSample : storedSamples) {
            if (!BioSample.WorkflowStatus.STORED.equals(bioSample.getWorkflowStatus())) {
                continue;
            }
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem == null || sampleItem.getId() == null || sampleItem.getId().trim().isEmpty()) {
                continue;
            }

            Map<String, Object> location = locationsBySampleItemId.get(sampleItem.getId().trim());
            if (!isBiorepositoryFreezerLocation(location)) {
                continue;
            }

            Map<String, String> parsedLocation = normalizeFreezerLocation(location);

            Map<String, Object> candidate = new HashMap<>();
            candidate.put("bioSampleId", bioSample.getId());
            candidate.put("sampleItemId", sampleItem.getId());
            candidate.put("externalId", sampleItem.getExternalId());
            candidate.put("accessionNumber",
                    sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);
            candidate.put("locationPath", asString(location.get("hierarchicalPath")));
            candidate.put("expectedCoordinate", asString(location.get("positionCoordinate")));
            candidate.put("freezer", parsedLocation.get("freezer"));
            candidate.put("shelf", parsedLocation.get("shelf"));
            candidate.put("rack", parsedLocation.get("rack"));
            candidate.put("box", parsedLocation.get("box"));
            candidate.put("shelfKey", parsedLocation.get("shelfKey"));
            candidate.put("rackKey", parsedLocation.get("rackKey"));
            candidate.put("boxKey", parsedLocation.get("boxKey"));

            if (matchesLocationFilters(candidate, locationFilters)) {
                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private boolean isBiorepositoryFreezer(Map<String, Object> device) {
        return device != null
                && Boolean.TRUE.equals(device.get("active"))
                && Boolean.TRUE.equals(device.get("biorepositoryStorage"))
                && "freezer".equalsIgnoreCase(asString(device.get("deviceType")));
    }

    private boolean isBiorepositoryFreezerLocation(Map<String, Object> location) {
        return location != null
                && !location.isEmpty()
                && Boolean.TRUE.equals(location.get("deviceBiorepositoryStorage"))
                && "freezer".equalsIgnoreCase(asString(location.get("deviceType")));
    }

    private Map<String, String> normalizeFreezerLocation(Map<String, Object> location) {
        String freezer = asString(location.get("deviceName"));
        String shelf = asString(location.get("shelfLabel"));
        String rack = asString(location.get("rackLabel"));
        String box = asString(location.get("boxLabel"));

        if (freezer != null && shelf != null && rack != null && box != null) {
            Map<String, String> parsed = new HashMap<>();
            parsed.put("freezer", freezer);
            parsed.put("shelf", shelf);
            parsed.put("rack", rack);
            parsed.put("box", box);
            parsed.put("shelfKey", freezer + " > " + shelf);
            parsed.put("rackKey", freezer + " > " + shelf + " > " + rack);
            parsed.put("boxKey", freezer + " > " + shelf + " > " + rack + " > " + box);
            return parsed;
        }

        return parseLocationParts(asString(location.get("hierarchicalPath")), asString(location.get("positionCoordinate")));
    }

    private Map<String, String> parseLocationParts(String locationPath, String coordinate) {
        String freezer = null;
        String shelf = null;
        String rack = null;
        String box = null;

        String[] segments = locationPath != null ? locationPath.split("\\s*>\\s*") : new String[0];
        List<String> normalizedSegments = new ArrayList<>();
        for (String segment : segments) {
            if (segment == null) {
                continue;
            }
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalizedSegments.add(trimmed);

            String lower = trimmed.toLowerCase();
            if (freezer == null && lower.contains("freezer")) {
                freezer = trimmed;
                continue;
            }
            if (shelf == null && lower.contains("shelf")) {
                shelf = trimmed;
                continue;
            }
            if (rack == null && lower.contains("rack")) {
                rack = trimmed;
                continue;
            }
            if (box == null && (lower.contains("box") || lower.startsWith("bx"))) {
                box = trimmed;
            }
        }

        if (freezer == null && !normalizedSegments.isEmpty()) {
            freezer = normalizedSegments.get(0);
        }
        if (shelf == null && normalizedSegments.size() > 1) {
            shelf = normalizedSegments.get(1);
        }
        if (rack == null && normalizedSegments.size() > 2) {
            rack = normalizedSegments.get(2);
        }
        if (box == null) {
            if (normalizedSegments.size() > 3) {
                box = normalizedSegments.get(3);
            } else if (!normalizedSegments.isEmpty()) {
                box = normalizedSegments.get(normalizedSegments.size() - 1);
            }
        }

        String normalizedFreezer = freezer != null ? freezer : "Unknown Freezer";
        String normalizedShelf = shelf != null ? shelf : "Unknown Shelf";
        String normalizedRack = rack != null ? rack : "Unknown Rack";
        String normalizedBox = box != null ? box : "Unknown Box";

        Map<String, String> parsed = new HashMap<>();
        parsed.put("freezer", normalizedFreezer);
        parsed.put("shelf", normalizedShelf);
        parsed.put("rack", normalizedRack);
        parsed.put("box", normalizedBox);
        parsed.put("shelfKey", normalizedFreezer + " > " + normalizedShelf);
        parsed.put("rackKey", normalizedFreezer + " > " + normalizedShelf + " > " + normalizedRack);
        parsed.put("boxKey", normalizedFreezer + " > " + normalizedShelf + " > " + normalizedRack + " > "
                + normalizedBox);
        parsed.put("position", coordinate != null ? coordinate : "");
        return parsed;
    }

    private boolean matchesLocationFilters(Map<String, Object> candidate, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        String freezerFilter = normalizeFilter(filters.get("freezer"));
        String shelfFilter = normalizeFilter(filters.get("shelf"));
        String rackFilter = normalizeFilter(filters.get("rack"));
        String boxFilter = normalizeFilter(filters.get("box"));

        if (!matchesSingleFilter(asString(candidate.get("freezer")), freezerFilter)) {
            return false;
        }
        if (!matchesSingleFilter(asString(candidate.get("shelf")), shelfFilter)) {
            return false;
        }
        if (!matchesSingleFilter(asString(candidate.get("rack")), rackFilter)) {
            return false;
        }

        if (boxFilter != null) {
            String box = asString(candidate.get("box"));
            String boxKey = asString(candidate.get("boxKey"));
            boolean boxMatches = matchesSingleFilter(box, boxFilter) || matchesSingleFilter(boxKey, boxFilter);
            if (!boxMatches) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesSingleFilter(String actual, String normalizedFilter) {
        if (normalizedFilter == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return actual.toLowerCase().contains(normalizedFilter);
    }

    private String normalizeFilter(String filterValue) {
        if (filterValue == null || filterValue.trim().isEmpty()) {
            return null;
        }
        return filterValue.trim().toLowerCase();
    }

    private Map<String, Object> optionRow(String value, String label, String freezer, String shelf, String rack) {
        Map<String, Object> row = new HashMap<>();
        row.put("value", value);
        row.put("label", label);
        if (freezer != null) {
            row.put("freezer", freezer);
        }
        if (shelf != null) {
            row.put("shelf", shelf);
        }
        if (rack != null) {
            row.put("rack", rack);
        }
        return row;
    }

    private int countMatchingPrefix(Set<String> values, String prefix) {
        int count = 0;
        for (String value : values) {
            if (value != null && value.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private String prefixBeforeLastSeparator(String value) {
        if (value == null || !value.contains(" > ")) {
            return value;
        }
        return value.substring(0, value.lastIndexOf(" > "));
    }

    private String suffixAfterLastSeparator(String value) {
        if (value == null || !value.contains(" > ")) {
            return value;
        }
        return value.substring(value.lastIndexOf(" > ") + 3);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        String str = String.valueOf(value);
        return str.isBlank() ? null : str;
    }

    private String resolveExpectedCoordinateSnapshot(BioSample bioSample, String providedSnapshot) {
        if (providedSnapshot != null && !providedSnapshot.trim().isEmpty()) {
            return providedSnapshot.trim();
        }
        if (bioSample == null || bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            return null;
        }

        Map<String, Object> location = sampleStorageService.getSampleItemLocation(bioSample.getSampleItem().getId());
        if (location == null || location.isEmpty()) {
            return null;
        }

        String path = asString(location.get("hierarchicalPath"));
        String coordinate = asString(location.get("positionCoordinate"));

        if (path == null && coordinate == null) {
            return null;
        }
        if (path == null) {
            return coordinate;
        }
        if (coordinate == null) {
            return path;
        }
        return path + " @ " + coordinate;
    }
}
