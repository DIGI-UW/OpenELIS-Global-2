package org.openelisglobal.biorepository.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for BiorepositoryQCInspection entity operations. Manages QC
 * inspection records for biorepository samples.
 */
public interface BiorepositoryQCInspectionDAO extends BaseDAO<BiorepositoryQCInspection, Integer> {

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
     * query.
     *
     * @param bioSampleIds list of biosample IDs
     * @return map keyed by biosample ID containing the latest inspection
     */
    Map<Integer, BiorepositoryQCInspection> getMostRecentByBioSampleIds(List<Integer> bioSampleIds);

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
     * Find inspections by QC batch ID.
     *
     * @param qcBatchId QC batch identifier
     * @return list of inspections ordered by inspection date ascending
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
     * Get all inspections within a date range.
     *
     * @param startDate start of the range
     * @param endDate   end of the range
     * @return list of inspections ordered by inspection date
     */
    List<BiorepositoryQCInspection> getInspectionsByDateRange(java.sql.Timestamp startDate, java.sql.Timestamp endDate);

    /**
     * True if the biosample has at least one inspection with inspection date in
     * {@code [startDate, endDate]} (inclusive).
     */
    boolean hasInspectionBetween(Integer bioSampleId, java.sql.Timestamp startDate, java.sql.Timestamp endDate);

    /**
     * Biosample IDs from {@code bioSampleIds} that have at least one QC inspection.
     */
    Set<Integer> getBioSampleIdsWithAnyInspection(List<Integer> bioSampleIds);

    /**
     * Biosample IDs from {@code bioSampleIds} with an inspection in {@code [startDate, endDate]}.
     */
    Set<Integer> getBioSampleIdsInspectedBetween(List<Integer> bioSampleIds, java.sql.Timestamp startDate,
            java.sql.Timestamp endDate);
}
