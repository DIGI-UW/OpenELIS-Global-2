package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.dao.BiorepositoryQCInspectionDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.DiscrepancyType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
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
    private SampleStorageService storageService;

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
            String correctiveAction, String remarks, String sysUserId) {

        BioSample bioSample = bioSampleService.get(bioSampleId);
        if (bioSample == null) {
            throw new IllegalArgumentException("BioSample not found: " + bioSampleId);
        }

        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setBioSample(bioSample);
        inspection.setInspectorName(inspectorName);
        inspection.setInspectionDate(inspectionDate);
        inspection.setSamplePresent(samplePresent);
        inspection.setLabelIntegrity(labelIntegrity);
        inspection.setContainerIntegrity(containerIntegrity);
        inspection.setVolumeAppearanceAcceptable(volumeAppearanceAcceptable);
        inspection.setCorrectPosition(correctPosition);

        // Snapshot expected location at time of QC record creation.
        SampleItem sampleItem = bioSample.getSampleItem();
        if (sampleItem != null && sampleItem.getId() != null) {
            Map<String, Object> currentLocation = storageService.getSampleItemLocation(sampleItem.getId().toString());
            inspection.setExpectedLocationPath(trimToNull(asString(currentLocation.get("hierarchicalPath"))));
            inspection.setExpectedPositionCoordinate(trimToNull(asString(currentLocation.get("positionCoordinate"))));
        }

        // Auto-calculate QC result based on checklist
        inspection.updateQcResult();

        // Set discrepancy details if QC failed
        if (inspection.getQcResult() == QCResult.DISCREPANCY_FOUND) {
            DiscrepancyType type = DiscrepancyType.fromString(discrepancyType);
            if (type == null) {
                throw new IllegalArgumentException("Discrepancy type is required when QC result is DISCREPANCY_FOUND");
            }
            if (trimToNull(correctiveAction) == null) {
                throw new IllegalArgumentException("Corrective action is required when QC result is DISCREPANCY_FOUND");
            }
            if (trimToNull(remarks) == null) {
                throw new IllegalArgumentException("Comment/remarks is required when QC result is DISCREPANCY_FOUND");
            }
            inspection.setDiscrepancyType(type);
            inspection.setCorrectiveAction(trimToNull(correctiveAction));
            inspection.setRemarks(trimToNull(remarks));
        } else {
            inspection.setDiscrepancyType(null);
            inspection.setCorrectiveAction(null);
            inspection.setRemarks(trimToNull(remarks));
        }

        inspection.setSysUserId(sysUserId);

        return save(inspection);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    @Override
    @Transactional
    public List<BiorepositoryQCInspection> createBulkInspections(List<Integer> bioSampleIds, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String sysUserId) {

        List<BiorepositoryQCInspection> inspections = new ArrayList<>();

        for (Integer bioSampleId : bioSampleIds) {
            BiorepositoryQCInspection inspection = createInspection(bioSampleId, inspectorName, inspectionDate,
                    samplePresent, labelIntegrity, containerIntegrity, volumeAppearanceAcceptable, correctPosition,
                    discrepancyType, correctiveAction, remarks, sysUserId);
            inspections.add(inspection);
        }

        return inspections;
    }

    @Override
    @Transactional
    public Map<String, Object> applyCorrectionWorkflow(BiorepositoryQCInspection inspection, String correctionActionType,
            String correctionLocationId, String correctionLocationType, String correctionPositionCoordinate,
            String correctionReason, String correctiveAction, String remarks, String correctedByUserId) {
        if (inspection == null) {
            throw new IllegalArgumentException("Inspection is required for correction workflow");
        }
        if (inspection.getQcResult() != BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND) {
            throw new IllegalArgumentException("Correction workflow requires discrepancy QC result");
        }

        BioSample bioSample = inspection.getBioSample();
        if (bioSample == null || bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            throw new IllegalArgumentException("Sample item is required for correction workflow");
        }

        String actionType = trimToNull(correctionActionType);
        if (actionType == null) {
            throw new IllegalArgumentException("Correction action type is required");
        }

        String normalizedActionType = actionType.trim().toUpperCase();
        String sampleItemId = bioSample.getSampleItem().getId();

        String reason = trimToNull(correctionReason);
        if (reason == null) {
            reason = trimToNull(correctiveAction);
        }
        if (reason == null) {
            reason = "QC discrepancy correction";
        }

        String oldCoordinate = composeCoordinate(inspection.getExpectedLocationPath(), inspection.getExpectedPositionCoordinate());
        Timestamp correctionTimestamp = new Timestamp(System.currentTimeMillis());
        String normalizedActionReason = normalizedActionType + ": " + reason;

        Map<String, Object> correction = new HashMap<>();
        correction.put("actionType", normalizedActionType);
        correction.put("reason", normalizedActionReason);
        correction.put("correctionTimestamp", correctionTimestamp.toString());

        if ("MARK_MISSING".equals(normalizedActionType)) {
            if (!isSampleMissingDiscrepancy(inspection)) {
                throw new IllegalArgumentException("MARK_MISSING requires discrepancy type SAMPLE_MISSING");
            }
            if (trimToNull(correctionLocationId) != null || trimToNull(correctionLocationType) != null
                    || trimToNull(correctionPositionCoordinate) != null) {
                throw new IllegalArgumentException("MARK_MISSING does not allow correction location/position fields");
            }

            Map<String, Object> missingUpdate = storageService.markSampleItemMissing(sampleItemId, normalizedActionReason,
                    trimToNull(remarks));
            @SuppressWarnings("unchecked")
            Map<String, Object> updatedLocation = missingUpdate.get("updatedLocation") instanceof Map
                    ? (Map<String, Object>) missingUpdate.get("updatedLocation")
                    : Map.of("hierarchicalPath", "Missing (not found during QC)", "positionCoordinate", null,
                            "status", "MISSING");

            String newCoordinate = composeCoordinate(asString(updatedLocation.get("hierarchicalPath")),
                    asString(updatedLocation.get("positionCoordinate")));

            inspection.setCorrectiveAction(normalizedActionReason);
            inspection.setCorrectionActionType(normalizedActionType);
            inspection.setCorrectionOldCoordinate(oldCoordinate);
            inspection.setCorrectionNewCoordinate(trimToNull(newCoordinate) != null ? newCoordinate
                    : "Missing (not found during QC)");
            inspection.setCorrectionReason(normalizedActionReason);
            inspection.setCorrectionByUser(trimToNull(correctedByUserId) != null ? trimToNull(correctedByUserId)
                    : inspection.getSysUserId());
            inspection.setCorrectionTimestamp(correctionTimestamp);
            update(inspection);

            correction.put("movementId", asString(missingUpdate.get("movementId")));
            correction.put("updatedLocation", updatedLocation);
            correction.put("auditTrail", buildAuditTrail(inspection));
            return correction;
        }

        if (!"UPDATE_LOCATION".equals(normalizedActionType) && !"REASSIGN_POSITION".equals(normalizedActionType)) {
            throw new IllegalArgumentException("Unsupported correction action type: " + correctionActionType);
        }

        String locationId = trimToNull(correctionLocationId);
        String locationType = trimToNull(correctionLocationType);
        if (locationId == null || locationType == null) {
            throw new IllegalArgumentException(
                    "Correction locationId and locationType are required for UPDATE_LOCATION/REASSIGN_POSITION");
        }
        if ("REASSIGN_POSITION".equals(normalizedActionType) && trimToNull(correctionPositionCoordinate) == null) {
            throw new IllegalArgumentException("REASSIGN_POSITION requires correctionPositionCoordinate");
        }

        String movementId = storageService.moveSampleItemWithLocation(sampleItemId, locationId, locationType,
                trimToNull(correctionPositionCoordinate), normalizedActionReason, trimToNull(remarks));

        Map<String, Object> updatedLocation = storageService.getSampleItemLocation(sampleItemId);
        String newCoordinate = composeCoordinate(asString(updatedLocation.get("hierarchicalPath")),
                asString(updatedLocation.get("positionCoordinate")));

        inspection.setCorrectiveAction(normalizedActionReason);
        inspection.setCorrectionActionType(normalizedActionType);
        inspection.setCorrectionOldCoordinate(oldCoordinate);
        inspection.setCorrectionNewCoordinate(newCoordinate);
        inspection.setCorrectionReason(normalizedActionReason);
        inspection.setCorrectionByUser(trimToNull(correctedByUserId) != null ? trimToNull(correctedByUserId)
                : inspection.getSysUserId());
        inspection.setCorrectionTimestamp(correctionTimestamp);
        update(inspection);

        correction.put("movementId", movementId);
        correction.put("updatedLocation", updatedLocation);
        correction.put("auditTrail", buildAuditTrail(inspection));
        return correction;
    }

    private boolean isSampleMissingDiscrepancy(BiorepositoryQCInspection inspection) {
        if (inspection == null || inspection.getDiscrepancyType() == null) {
            return false;
        }
        return BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING.equals(inspection.getDiscrepancyType())
                || BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE.equals(inspection.getDiscrepancyType());
    }

    private Map<String, Object> buildAuditTrail(BiorepositoryQCInspection inspection) {
        Map<String, Object> auditTrail = new HashMap<>();
        String oldCoordinate = trimToNull(inspection.getCorrectionOldCoordinate()) != null
                ? inspection.getCorrectionOldCoordinate()
                : composeCoordinate(inspection.getExpectedLocationPath(), inspection.getExpectedPositionCoordinate());
        String newCoordinate = trimToNull(inspection.getCorrectionNewCoordinate()) != null
                ? inspection.getCorrectionNewCoordinate()
                : oldCoordinate;
        String timestamp = inspection.getCorrectionTimestamp() != null ? inspection.getCorrectionTimestamp().toString()
                : (inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : null);
        String user = trimToNull(inspection.getCorrectionByUser()) != null ? inspection.getCorrectionByUser()
                : inspection.getSysUserId();
        String reason = trimToNull(inspection.getCorrectionReason()) != null ? inspection.getCorrectionReason()
                : inspection.getCorrectiveAction();

        auditTrail.put("oldCoordinate", oldCoordinate);
        auditTrail.put("newCoordinate", newCoordinate);
        auditTrail.put("fromCoordinates", oldCoordinate);
        auditTrail.put("toCoordinates", newCoordinate);
        auditTrail.put("user", user);
        auditTrail.put("correctedBy", user);
        auditTrail.put("timestamp", timestamp);
        auditTrail.put("correctedAt", timestamp);
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
}
