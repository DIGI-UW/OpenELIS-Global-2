package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for BiorepositoryQCInspection entity operations. Manages QC
 * inspection records for biorepository samples.
 */
public interface BiorepositoryQCInspectionService extends BaseObjectService<BiorepositoryQCInspection, Integer> {

    /**
     * Find all QC inspection records for a specific biosample.
     *
     * @param bioSampleId the biosample ID
     * @return list of inspections ordered by inspection date descending
     */
    List<BiorepositoryQCInspection> getByBioSampleId(Integer bioSampleId);

    /**
     * Find the most recent QC inspection for a biosample.
     *
     * @param bioSampleId the biosample ID
     * @return the most recent inspection or null if none found
     */
    BiorepositoryQCInspection getMostRecentByBioSampleId(Integer bioSampleId);

    /**
     * Find all inspections by QC result.
     *
     * @param qcResult the QC result (VERIFIED or DISCREPANCY_FOUND)
     * @return list of inspections ordered by inspection date descending
     */
    List<BiorepositoryQCInspection> getByQCResult(QCResult qcResult);

    /**
     * Find inspections by inspector name.
     *
     * @param inspectorName the inspector's name
     * @return list of inspections ordered by inspection date descending
     */
    List<BiorepositoryQCInspection> getByInspectorName(String inspectorName);

    /**
     * Count inspections by QC result.
     *
     * @param qcResult the QC result
     * @return count of matching inspections
     */
    long countByQCResult(QCResult qcResult);

    /**
     * Check if a biosample has any QC inspection records.
     *
     * @param bioSampleId the biosample ID
     * @return true if at least one inspection exists
     */
    boolean existsByBioSampleId(Integer bioSampleId);

    /**
     * Get all inspections within a date range.
     *
     * @param startDate start of the range
     * @param endDate   end of the range
     * @return list of inspections ordered by inspection date
     */
    List<BiorepositoryQCInspection> getInspectionsByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Create and save a QC inspection record.
     *
     * @param bioSampleId                the biosample ID
     * @param inspectorName              the inspector's name
     * @param inspectionDate             the inspection date/time
     * @param samplePresent              checklist item
     * @param labelIntegrity             checklist item
     * @param containerIntegrity         checklist item
     * @param volumeAppearanceAcceptable checklist item
     * @param correctPosition            checklist item
     * @param discrepancyType            type of discrepancy if any
     * @param correctiveAction           corrective action taken
     * @param remarks                    additional remarks
     * @param sysUserId                  system user ID
     * @return the created inspection record
     */
    BiorepositoryQCInspection createInspection(Integer bioSampleId, String inspectorName, Timestamp inspectionDate,
            boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String sysUserId);

    /**
     * Bulk create QC inspections for multiple biosamples.
     *
     * @param bioSampleIds               list of biosample IDs
     * @param inspectorName              the inspector's name
     * @param inspectionDate             the inspection date/time
     * @param samplePresent              checklist item
     * @param labelIntegrity             checklist item
     * @param containerIntegrity         checklist item
     * @param volumeAppearanceAcceptable checklist item
     * @param correctPosition            checklist item
     * @param discrepancyType            type of discrepancy if any
     * @param correctiveAction           corrective action taken
     * @param remarks                    additional remarks
     * @param sysUserId                  system user ID
     * @return list of created inspection records
     */
    List<BiorepositoryQCInspection> createBulkInspections(List<Integer> bioSampleIds, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String sysUserId);
}
