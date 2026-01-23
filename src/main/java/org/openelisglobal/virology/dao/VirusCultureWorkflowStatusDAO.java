package org.openelisglobal.virology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.virology.valueholder.VirusCultureBatch;
import org.openelisglobal.virology.valueholder.VirusCultureWorkflowStatus;

/**
 * DAO interface for VirusCultureWorkflowStatus entity
 */
public interface VirusCultureWorkflowStatusDAO extends BaseDAO<VirusCultureWorkflowStatus, Integer> {

    /**
     * Find workflow statuses for a virus culture batch
     * 
     * @param cultureBatch the culture batch
     * @return List of VirusCultureWorkflowStatus ordered by step order
     */
    List<VirusCultureWorkflowStatus> findByCultureBatch(VirusCultureBatch cultureBatch);

    /**
     * Find workflow statuses for a culture batch ID
     * 
     * @param cultureBatchId the culture batch ID
     * @return List of VirusCultureWorkflowStatus ordered by step order
     */
    List<VirusCultureWorkflowStatus> findByCultureBatchId(Integer cultureBatchId);

    /**
     * Find workflow status for specific step
     * 
     * @param cultureBatch the culture batch
     * @param stepName     the step name
     * @return VirusCultureWorkflowStatus or null
     */
    VirusCultureWorkflowStatus findByCultureBatchAndStepName(VirusCultureBatch cultureBatch, String stepName);

    /**
     * Find workflow statuses by status
     * 
     * @param status the step status
     * @return List of VirusCultureWorkflowStatus
     */
    List<VirusCultureWorkflowStatus> findByStatus(VirusCultureWorkflowStatus.StepStatus status);

    /**
     * Find current step for a culture batch
     * 
     * @param cultureBatch the culture batch
     * @return VirusCultureWorkflowStatus of the current step or null
     */
    VirusCultureWorkflowStatus findCurrentStep(VirusCultureBatch cultureBatch);

    /**
     * Find next pending step for a culture batch
     * 
     * @param cultureBatch the culture batch
     * @return VirusCultureWorkflowStatus of the next pending step or null
     */
    VirusCultureWorkflowStatus findNextPendingStep(VirusCultureBatch cultureBatch);

    /**
     * Find workflow statuses assigned to a user
     * 
     * @param userId the user ID
     * @return List of VirusCultureWorkflowStatus
     */
    List<VirusCultureWorkflowStatus> findByAssignedTo(Integer userId);

    /**
     * Find in-progress workflow statuses
     * 
     * @return List of VirusCultureWorkflowStatus
     */
    List<VirusCultureWorkflowStatus> findInProgress();

    /**
     * Find workflow statuses that failed quality check
     * 
     * @return List of VirusCultureWorkflowStatus
     */
    List<VirusCultureWorkflowStatus> findFailedQualityChecks();
}