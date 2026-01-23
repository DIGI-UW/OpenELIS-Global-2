package org.openelisglobal.virology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.virology.valueholder.VirusCultureBatch;

/**
 * DAO interface for VirusCultureBatch entity
 */
public interface VirusCultureBatchDAO extends BaseDAO<VirusCultureBatch, Integer> {

    /**
     * Find virus culture batch by batch ID
     * 
     * @param batchId the batch ID
     * @return VirusCultureBatch or null if not found
     */
    VirusCultureBatch findByBatchId(String batchId);

    /**
     * Find virus culture batches by notebook page sample
     * 
     * @param notebookPageSample the notebook page sample
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findByNotebookPageSample(NotebookPageSample notebookPageSample);

    /**
     * Find virus culture batches by notebook page sample ID
     * 
     * @param notebookPageSampleId the notebook page sample ID
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findByNotebookPageSampleId(Integer notebookPageSampleId);

    /**
     * Find virus culture batches by status
     * 
     * @param status the batch status
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findByStatus(VirusCultureBatch.BatchStatus status);

    /**
     * Find active virus culture batches (not failed, cancelled, or complete)
     * 
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findActiveBatches();

    /**
     * Find virus culture batches by virus strain
     * 
     * @param virusStrain the virus strain
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findByVirusStrain(String virusStrain);

    /**
     * Find virus culture batches by cell line
     * 
     * @param cellLine the cell line used
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findByCellLine(String cellLine);

    /**
     * Find virus culture batches created by user
     * 
     * @param userId the user ID
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findByCreatedBy(Integer userId);

    /**
     * Count virus culture batches by status
     * 
     * @param status the batch status
     * @return count of batches
     */
    long countByStatus(VirusCultureBatch.BatchStatus status);

    /**
     * Find batches requiring attention (failed QC, on hold, etc.)
     * 
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findBatchesRequiringAttention();

    /**
     * Find recently created batches (within last N days)
     * 
     * @param days number of days to look back
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> findRecentBatches(int days);
}