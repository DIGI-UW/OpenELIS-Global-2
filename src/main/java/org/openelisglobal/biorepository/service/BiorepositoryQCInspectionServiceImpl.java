package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.biorepository.dao.BiorepositoryQCInspectionDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.DiscrepancyType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
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

        // Auto-calculate QC result based on checklist
        inspection.updateQcResult();

        // Set discrepancy details if QC failed
        if (inspection.getQcResult() == QCResult.DISCREPANCY_FOUND) {
            DiscrepancyType type = DiscrepancyType.fromString(discrepancyType);
            inspection.setDiscrepancyType(type);
            inspection.setCorrectiveAction(correctiveAction);
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
}
