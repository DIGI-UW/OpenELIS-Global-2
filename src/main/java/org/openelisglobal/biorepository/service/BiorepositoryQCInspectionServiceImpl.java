package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.ArrayList;
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
    public List<BiorepositoryQCInspection> getByQcBatchId(String qcBatchId) {
        return baseObjectDAO.getByQcBatchId(qcBatchId);
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
    public boolean hasInspectionBetween(Integer bioSampleId, java.sql.Timestamp start, java.sql.Timestamp end) {
        if (bioSampleId == null) {
            return false;
        }
        return baseObjectDAO.hasInspectionBetween(bioSampleId, start, end);
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
        return createInspection(bioSampleId, inspectorName, inspectionDate, samplePresent, labelIntegrity,
                containerIntegrity, volumeAppearanceAcceptable, correctPosition, discrepancyType, correctiveAction,
                remarks, null, null, sysUserId);
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
        inspection.setExpectedCoordinateSnapshot(trimToNull(expectedCoordinateSnapshot));

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
        inspection.setQcBatchId(trimToNull(qcBatchId));

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
        return createBulkInspections(bioSampleIds, inspectorName, inspectionDate, samplePresent, labelIntegrity,
                containerIntegrity, volumeAppearanceAcceptable, correctPosition, discrepancyType, correctiveAction,
                remarks, null, null, sysUserId);
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
}
