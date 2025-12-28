package org.openelisglobal.virology.service;

import java.util.List;
import java.util.Map;

import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.virology.valueholder.*;

/**
 * Service interface for managing the complete virus culture workflow
 */
public interface VirusCultureWorkflowService {

    // ==========================================
    // VIRUS CULTURE BATCH MANAGEMENT
    // ==========================================

    /**
     * Create a new virus culture batch from a notebook page sample
     * @param notebookPageSample the source sample from stage 1
     * @param virusStrain the virus strain
     * @param cellLine the cell line to use
     * @param createdBy the user creating the batch
     * @return created VirusCultureBatch
     */
    VirusCultureBatch createVirusCultureBatch(NotebookPageSample notebookPageSample,
                                              String virusStrain,
                                              String cellLine,
                                              SystemUser createdBy);

    /**
     * Get virus culture batch by ID
     * @param id the batch ID
     * @return VirusCultureBatch or null
     */
    VirusCultureBatch getVirusCultureBatch(Integer id);

    /**
     * Get virus culture batch by batch ID
     * @param batchId the batch ID string
     * @return VirusCultureBatch or null
     */
    VirusCultureBatch getVirusCultureBatchByBatchId(String batchId);

    /**
     * Get virus culture batches for a notebook page sample
     * @param notebookPageSampleId the notebook page sample ID
     * @return List of VirusCultureBatch
     */
    List<VirusCultureBatch> getVirusCultureBatchesBySample(Integer notebookPageSampleId);

    /**
     * Get active virus culture batches
     * @return List of active VirusCultureBatch
     */
    List<VirusCultureBatch> getActiveVirusCultureBatches();

    /**
     * Get virus culture batches requiring attention
     * @return List of VirusCultureBatch with issues
     */
    List<VirusCultureBatch> getBatchesRequiringAttention();

    /**
     * Update virus culture batch
     * @param batch the batch to update
     * @return updated VirusCultureBatch
     */
    VirusCultureBatch updateVirusCultureBatch(VirusCultureBatch batch);

    /**
     * Cancel a virus culture batch
     * @param batchId the batch ID
     * @param reason cancellation reason
     * @param cancelledBy the user cancelling
     */
    void cancelVirusCultureBatch(Integer batchId, String reason, SystemUser cancelledBy);

    // ==========================================
    // WORKFLOW STEP MANAGEMENT
    // ==========================================

    /**
     * Get workflow status for a virus culture batch
     * @param batchId the batch ID
     * @return List of VirusCultureWorkflowStatus
     */
    List<VirusCultureWorkflowStatus> getWorkflowStatus(Integer batchId);

    /**
     * Get current workflow step for a batch
     * @param batchId the batch ID
     * @return current VirusCultureWorkflowStatus or null
     */
    VirusCultureWorkflowStatus getCurrentWorkflowStep(Integer batchId);

    /**
     * Start a workflow step
     * @param batchId the batch ID
     * @param stepName the step name
     * @param assignedTo the user assigned
     * @return updated VirusCultureWorkflowStatus
     */
    VirusCultureWorkflowStatus startWorkflowStep(Integer batchId, String stepName, SystemUser assignedTo);

    /**
     * Complete a workflow step
     * @param batchId the batch ID
     * @param stepName the step name
     * @param completedBy the user completing
     * @param qualityResult the QC result
     * @param notes any notes
     * @return updated VirusCultureWorkflowStatus
     */
    VirusCultureWorkflowStatus completeWorkflowStep(Integer batchId,
                                                    String stepName,
                                                    SystemUser completedBy,
                                                    VirusCultureWorkflowStatus.QualityCheckResult qualityResult,
                                                    String notes);

    /**
     * Fail a workflow step
     * @param batchId the batch ID
     * @param stepName the step name
     * @param failedBy the user marking as failed
     * @param reason failure reason
     * @return updated VirusCultureWorkflowStatus
     */
    VirusCultureWorkflowStatus failWorkflowStep(Integer batchId,
                                                String stepName,
                                                SystemUser failedBy,
                                                String reason);

    /**
     * Get workflow progress summary
     * @param batchId the batch ID
     * @return Map with progress information
     */
    Map<String, Object> getWorkflowProgress(Integer batchId);

    // ==========================================
    // PROCESS STEP OPERATIONS
    // ==========================================

    /**
     * Record media preparation step
     * @param batchId the batch ID
     * @param mediaPreparation the media preparation data
     * @return created VirusCultureMediaPreparation
     */
    VirusCultureMediaPreparation recordMediaPreparation(Integer batchId, VirusCultureMediaPreparation mediaPreparation);

    /**
     * Record sterilization step
     * @param batchId the batch ID
     * @param sterilization the sterilization data
     * @return created VirusCultureSterilization
     */
    VirusCultureSterilization recordSterilization(Integer batchId, VirusCultureSterilization sterilization);

    /**
     * Record cell culture step
     * @param batchId the batch ID
     * @param cellCulture the cell culture data
     * @return created VirusCultureCellCulture
     */
    VirusCultureCellCulture recordCellCulture(Integer batchId, VirusCultureCellCulture cellCulture);

    /**
     * Record quality control step
     * @param batchId the batch ID
     * @param qualityControl the QC data
     * @return created VirusCultureQualityControl
     */
    VirusCultureQualityControl recordQualityControl(Integer batchId, VirusCultureQualityControl qualityControl);

    /**
     * Record virus inoculation step
     * @param batchId the batch ID
     * @param virusInoculation the virus inoculation data
     * @return created VirusCultureVirusInoculation
     */
    VirusCultureVirusInoculation recordVirusInoculation(Integer batchId, VirusCultureVirusInoculation virusInoculation);

    /**
     * Record imaging step
     * @param batchId the batch ID
     * @param imaging the imaging data
     * @return created VirusCultureImaging
     */
    VirusCultureImaging recordImaging(Integer batchId, VirusCultureImaging imaging);

    /**
     * Record formulation step
     * @param batchId the batch ID
     * @param formulation the formulation data
     * @return created VirusCultureFormulation
     */
    VirusCultureFormulation recordFormulation(Integer batchId, VirusCultureFormulation formulation);

    /**
     * Record feeding step
     * @param batchId the batch ID
     * @param feeding the feeding data
     * @return created VirusCultureFeeding
     */
    VirusCultureFeeding recordFeeding(Integer batchId, VirusCultureFeeding feeding);

    /**
     * Record packaging step
     * @param batchId the batch ID
     * @param packaging the packaging data
     * @return created VirusCulturePackaging
     */
    VirusCulturePackaging recordPackaging(Integer batchId, VirusCulturePackaging packaging);

    // ==========================================
    // VALIDATION AND BUSINESS LOGIC
    // ==========================================

    /**
     * Validate if a step can be started
     * @param batchId the batch ID
     * @param stepName the step name
     * @return validation result with reasons
     */
    Map<String, Object> validateStepCanStart(Integer batchId, String stepName);

    /**
     * Validate if a batch can be created from a sample
     * @param notebookPageSampleId the sample ID
     * @return validation result with reasons
     */
    Map<String, Object> validateBatchCreation(Integer notebookPageSampleId);

    /**
     * Get summary statistics for virus culture workflow
     * @return Map with statistics
     */
    Map<String, Object> getWorkflowStatistics();

    /**
     * Generate batch ID for new culture batch
     * @param virusStrain the virus strain
     * @return generated batch ID
     */
    String generateBatchId(String virusStrain);

    /**
     * Auto-advance workflow to next step if conditions are met
     * @param batchId the batch ID
     * @return true if advanced, false otherwise
     */
    boolean autoAdvanceWorkflow(Integer batchId);

    /**
     * Check if any feeding is due for active batches
     * @return List of batches needing feeding
     */
    List<VirusCultureBatch> checkFeedingDue();

    /**
     * Get dashboard data for virus culture workflow
     * @return Map with dashboard information
     */
    Map<String, Object> getDashboardData();

    /**
     * Validate quality control results and determine next actions
     * @param qualityControl the QC results
     * @return Map with validation results and recommendations
     */
    Map<String, Object> validateQualityControlResults(VirusCultureQualityControl qualityControl);
}