package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
        int effectiveBoxes = boxesPerRound > 0 ? boxesPerRound : 10;
        int effectiveSamplesPerBox = samplesPerBox > 0 ? samplesPerBox : 3;
        Random random = randomSeed != null ? new Random(randomSeed) : new Random();
        String qcBatchId = "QCBATCH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, List<Map<String, Object>>> candidatesByBox = new HashMap<>();
        for (BioSample bioSample : bioSampleService.getAll()) {
            if (!BioSample.WorkflowStatus.STORED.equals(bioSample.getWorkflowStatus())) {
                continue;
            }
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem == null || sampleItem.getId() == null) {
                continue;
            }

            Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
            if (location == null || location.isEmpty()) {
                continue;
            }

            String locationPath = asString(location.get("hierarchicalPath"));
            String coordinate = asString(location.get("positionCoordinate"));
            String boxKey = extractBoxKey(locationPath, coordinate);
            if (boxKey == null) {
                continue;
            }

            Map<String, Object> candidate = new HashMap<>();
            candidate.put("bioSampleId", bioSample.getId());
            candidate.put("sampleItemId", sampleItem.getId());
            candidate.put("externalId", sampleItem.getExternalId());
            candidate.put("accessionNumber",
                    sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber() : null);
            candidate.put("locationPath", locationPath);
            candidate.put("expectedCoordinate", coordinate);
            candidate.put("boxKey", boxKey);

            candidatesByBox.computeIfAbsent(boxKey, k -> new ArrayList<>()).add(candidate);
        }

        List<String> boxKeys = new ArrayList<>(candidatesByBox.keySet());
        Collections.shuffle(boxKeys, random);
        if (boxKeys.size() > effectiveBoxes) {
            boxKeys = boxKeys.subList(0, effectiveBoxes);
        }

        List<Map<String, Object>> selectedSamples = new ArrayList<>();
        for (String boxKey : boxKeys) {
            List<Map<String, Object>> candidates = new ArrayList<>(candidatesByBox.get(boxKey));
            Collections.shuffle(candidates, random);
            int limit = Math.min(effectiveSamplesPerBox, candidates.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> selected = new HashMap<>(candidates.get(i));
                selected.put("qcBatchId", qcBatchId);
                selectedSamples.add(selected);
            }
        }

        selectedSamples.sort(
                Comparator.comparing(m -> asString(m.get("boxKey")) + ":" + asString(m.get("expectedCoordinate"))));

        Map<String, Object> result = new HashMap<>();
        result.put("qcBatchId", qcBatchId);
        result.put("boxesSelected", boxKeys.size());
        result.put("samplesSelected", selectedSamples.size());
        result.put("boxesPerRound", effectiveBoxes);
        result.put("samplesPerBox", effectiveSamplesPerBox);
        result.put("seed", randomSeed);
        result.put("generatedBy", sysUserId);
        result.put("samples", selectedSamples);
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

    private String extractBoxKey(String locationPath, String coordinate) {
        if (locationPath == null || locationPath.trim().isEmpty()) {
            return null;
        }

        String[] segments = locationPath.split("\\s*>\\s*");
        if (segments.length < 2) {
            return null;
        }

        String normalizedCoordinate = coordinate != null ? coordinate.trim() : "";
        int boxIndex = segments.length - 1;
        if (!normalizedCoordinate.isEmpty() && normalizedCoordinate.equalsIgnoreCase(segments[segments.length - 1])) {
            boxIndex = segments.length - 2;
        }
        if (boxIndex < 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= boxIndex; i++) {
            if (segments[i] == null || segments[i].trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" > ");
            }
            builder.append(segments[i].trim());
        }
        return builder.length() > 0 ? builder.toString() : null;
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
