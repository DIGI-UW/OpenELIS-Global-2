package org.openelisglobal.virology.service;

import java.util.List;
import org.openelisglobal.virology.controller.rest.VirologyProgressionRestController.ProgressionResult;
import org.openelisglobal.virology.controller.rest.VirologyProgressionRestController.ValidationResult;

/**
 * Service interface for managing virology workflow progression between stages.
 * Handles validation and execution of sample advancement through the virology
 * workflow.
 */
public interface VirologyProgressionService {

    /**
     * Validates whether samples can be progressed from one stage to another.
     *
     * @param entryId   The notebook entry ID
     * @param fromStage The current stage (stage1_reception, stage2_culture,
     *                  stage3_vaccine)
     * @param toStage   The target stage (stage2_culture, stage3_vaccine)
     * @param sampleIds List of sample IDs to progress
     * @return ValidationResult containing validation status and any error messages
     */
    ValidationResult validateProgression(Long entryId, String fromStage, String toStage, List<Long> sampleIds);

    /**
     * Advances samples from one stage to another after validation.
     *
     * @param entryId   The notebook entry ID
     * @param fromStage The current stage
     * @param toStage   The target stage
     * @param sampleIds List of sample IDs to progress
     * @param userId    The user performing the progression
     * @return ProgressionResult containing success status and details
     */
    ProgressionResult advanceSamples(Long entryId, String fromStage, String toStage, List<Long> sampleIds,
            String userId);

    /**
     * Gets the current progression status for a notebook entry.
     *
     * @param entryId The notebook entry ID
     * @return Map containing progression status for each stage
     */
    java.util.Map<String, Object> getProgressionStatus(Long entryId);

    /**
     * Checks if a specific stage transition is valid.
     *
     * @param fromStage The current stage
     * @param toStage   The target stage
     * @return true if the transition is valid
     */
    boolean isValidStageTransition(String fromStage, String toStage);
}