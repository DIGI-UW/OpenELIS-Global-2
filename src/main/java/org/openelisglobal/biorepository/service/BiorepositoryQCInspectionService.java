package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Find the most recent QC inspections for multiple biosamples in a single
     * call.
     *
     * @param bioSampleIds list of biosample IDs
     * @return map keyed by biosample ID containing the latest inspection
     */
    default Map<Integer, BiorepositoryQCInspection> getMostRecentByBioSampleIds(List<Integer> bioSampleIds) {
        java.util.HashMap<Integer, BiorepositoryQCInspection> results = new java.util.HashMap<>();
        if (bioSampleIds == null) {
            return results;
        }
        for (Integer bioSampleId : bioSampleIds) {
            if (bioSampleId != null) {
                results.put(bioSampleId, getMostRecentByBioSampleId(bioSampleId));
            }
        }
        return results;
    }

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
     * Find all inspections in a generated QC batch.
     */
    List<BiorepositoryQCInspection> getByQcBatchId(String qcBatchId);

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
     * Check if a biosample has at least one inspection with inspection time in
     * {@code [start, end]} (inclusive).
     */
    boolean hasInspectionBetween(Integer bioSampleId, java.sql.Timestamp start, java.sql.Timestamp end);

    /**
     * Biosample IDs from {@code bioSampleIds} that have at least one QC inspection.
     */
    Set<Integer> getBioSampleIdsWithAnyInspection(List<Integer> bioSampleIds);

    /**
     * Biosample IDs from {@code bioSampleIds} with an inspection in {@code [start, end]}.
     */
    Set<Integer> getBioSampleIdsInspectedBetween(List<Integer> bioSampleIds, java.sql.Timestamp start,
            java.sql.Timestamp end);

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

    default BiorepositoryQCInspection createInspection(Integer bioSampleId, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String qcBatchId, String expectedCoordinateSnapshot,
            String sysUserId) {
        return createInspection(bioSampleId, inspectorName, inspectionDate, samplePresent, labelIntegrity,
                containerIntegrity, volumeAppearanceAcceptable, correctPosition, discrepancyType, correctiveAction,
                remarks, sysUserId);
    }

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
    default List<BiorepositoryQCInspection> createBulkInspections(List<Integer> bioSampleIds, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String sysUserId) {
        return createBulkInspections(bioSampleIds, inspectorName, inspectionDate, samplePresent, labelIntegrity,
                containerIntegrity, volumeAppearanceAcceptable, correctPosition, discrepancyType, correctiveAction,
                remarks, null, null, sysUserId);
    }

    List<BiorepositoryQCInspection> createBulkInspections(List<Integer> bioSampleIds, String inspectorName,
            Timestamp inspectionDate, boolean samplePresent, boolean labelIntegrity, boolean containerIntegrity,
            boolean volumeAppearanceAcceptable, boolean correctPosition, String discrepancyType,
            String correctiveAction, String remarks, String qcBatchId, String expectedCoordinateSnapshot,
            String sysUserId);

    /**
     * Generate a randomized QC round from currently stored samples.
     *
     * @param boxesPerRound number of boxes to sample
     * @param samplesPerBox number of samples to sample per selected box
     * @param randomSeed    optional seed for deterministic generation
     * @param sysUserId     request user ID
     * @return round metadata and selected samples
     */
    default Map<String, Object> generateRandomQCRound(int boxesPerRound, int samplesPerBox, Long randomSeed,
            String sysUserId) {
        throw new UnsupportedOperationException("Random QC round generation is not implemented by this service");
    }

    /**
     * Generate a randomized QC round scoped by optional location filters.
     *
     * Supported filter keys: freezer, shelf, rack, box
     */
    default Map<String, Object> generateRandomQCRound(int boxesPerRound, int samplesPerBox, Long randomSeed,
            String sysUserId, Map<String, String> locationFilters) {
        return generateRandomQCRound(boxesPerRound, samplesPerBox, randomSeed, sysUserId);
    }

    /**
     * Build a storage location overview for QC planning UI.
     *
     * @return summary counts and location breakdowns by freezer/shelf/rack/box
     */
    default Map<String, Object> getLocationOverview() {
        throw new UnsupportedOperationException("Location overview is not implemented by this service");
    }

    /**
     * Record a corrective action for a failed QC inspection and keep an auditable
     * trace of coordinate changes.
     *
     * @param inspectionId        target QC inspection ID
     * @param observedCoordinate  observed coordinate during QC
     * @param correctedCoordinate corrected coordinate saved in system
     * @param correctiveReason    reason for correction
     * @param sysUserId           user making the change
     * @return operation summary
     */
    default Map<String, Object> applyCorrectiveAction(Integer inspectionId, String observedCoordinate,
            String correctedCoordinate, String correctiveReason, String sysUserId) {
        throw new UnsupportedOperationException("Legacy corrective action endpoint is not implemented by this service");
    }

    /**
     * Apply mandatory correction workflow for a failed QC inspection.
     *
     * Supports UPDATE_LOCATION, REASSIGN_POSITION, and MARK_MISSING.
     * Persists correction audit details onto the inspection record.
     *
     * @return correction details suitable for API response
     */
        default Map<String, Object> applyCorrectionWorkflow(BiorepositoryQCInspection inspection,
            String correctionActionType, String correctionLocationId, String correctionLocationType,
            String correctionPositionCoordinate, String correctionReason, String correctiveAction, String remarks,
            String correctedByUserId) {
        throw new UnsupportedOperationException(
            "Correction workflow is handled at controller/service integration level in this branch");
        }
}
