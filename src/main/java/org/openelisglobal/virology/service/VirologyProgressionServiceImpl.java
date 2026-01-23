package org.openelisglobal.virology.service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.virology.controller.rest.VirologyProgressionRestController.ValidationResult;
import org.openelisglobal.virology.controller.rest.VirologyProgressionRestController.ProgressionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of VirologyProgressionService.
 * Handles validation and execution of sample progression through virology workflow stages.
 */
@Service
public class VirologyProgressionServiceImpl implements VirologyProgressionService {

    private static final Log log = LogFactory.getLog(VirologyProgressionServiceImpl.class);

    @Autowired
    private NotebookPageSampleDAO notebookPageSampleDAO;

    // Valid stage transitions
    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
        "stage1_reception", List.of("stage2_culture"),
        "stage2_culture", List.of("stage3_vaccine"),
        "stage3_vaccine", List.of() // Final stage
    );


    @Override
    public ValidationResult validateProgression(Long entryId, String fromStage, String toStage, List<Long> sampleIds) {
        List<String> errors = new ArrayList<>();

        try {
            // Validate stage transition
            if (!isValidStageTransition(fromStage, toStage)) {
                errors.add("Invalid stage transition from " + fromStage + " to " + toStage);
                return new ValidationResult(false, errors);
            }

            // Validate samples exist and belong to the entry
            List<NotebookPageSample> samples = validateSamplesExist(sampleIds, entryId);
            if (samples.size() != sampleIds.size()) {
                errors.add("One or more samples not found or do not belong to the specified entry");
                return new ValidationResult(false, errors);
            }

            // Stage-specific validation
            switch (fromStage) {
                case "stage1_reception":
                    validateStage1Completion(samples, errors);
                    break;
                case "stage2_culture":
                    validateStage2Completion(samples, errors);
                    break;
                default:
                    errors.add("Unknown source stage: " + fromStage);
            }

            return new ValidationResult(errors.isEmpty(), errors);

        } catch (Exception e) {
            log.error("Error during progression validation", e);
            errors.add("Validation error: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
    }

    @Override
    @Transactional
    public ProgressionResult advanceSamples(Long entryId, String fromStage, String toStage, List<Long> sampleIds, String userId) {
        try {
            // Validate before progression
            ValidationResult validation = validateProgression(entryId, fromStage, toStage, sampleIds);
            if (!validation.isValid()) {
                return new ProgressionResult(false, "Validation failed: " + String.join(", ", validation.getErrors()), 0);
            }

            // Get samples to progress
            List<NotebookPageSample> samples = validateSamplesExist(sampleIds, entryId);

            int progressedCount = 0;
            Timestamp now = new Timestamp(System.currentTimeMillis());

            // Update each sample's stage and metadata
            for (NotebookPageSample sample : samples) {
                // Update stage assignment
                updateSampleStage(sample, toStage, userId, now);

                // Save the updated sample
                notebookPageSampleDAO.update(sample);
                progressedCount++;

                log.info("Progressed sample " + sample.getId() + " from " + fromStage + " to " + toStage);
            }

            // Create audit trail entry
            createProgressionAuditTrail(entryId, fromStage, toStage, sampleIds, userId, now);

            return new ProgressionResult(true, null, progressedCount);

        } catch (Exception e) {
            log.error("Error during sample progression", e);
            return new ProgressionResult(false, "Progression failed: " + e.getMessage(), 0);
        }
    }

    @Override
    public Map<String, Object> getProgressionStatus(Long entryId) {
        Map<String, Object> status = new HashMap<>();

        try {
            // This would be implemented to return current progression status
            // For now, return basic structure
            status.put("entryId", entryId);
            status.put("stage1_reception", getStageStatus(entryId, "stage1_reception"));
            status.put("stage2_culture", getStageStatus(entryId, "stage2_culture"));
            status.put("stage3_vaccine", getStageStatus(entryId, "stage3_vaccine"));

        } catch (Exception e) {
            log.error("Error getting progression status", e);
            status.put("error", e.getMessage());
        }

        return status;
    }

    @Override
    public boolean isValidStageTransition(String fromStage, String toStage) {
        List<String> validToStages = VALID_TRANSITIONS.get(fromStage);
        return validToStages != null && validToStages.contains(toStage);
    }

    // Helper methods

    private List<NotebookPageSample> validateSamplesExist(List<Long> sampleIds, Long entryId) {
        List<NotebookPageSample> samples = new ArrayList<>();

        for (Long sampleId : sampleIds) {
            var sampleOpt = notebookPageSampleDAO.get(sampleId.intValue());
            if (sampleOpt.isPresent()) {
                NotebookPageSample sample = sampleOpt.get();
                // For now, skip entry validation as we need to verify page relationship
                samples.add(sample);
            }
        }

        return samples;
    }

    private void validateStage1Completion(List<NotebookPageSample> samples, List<String> errors) {
        for (NotebookPageSample sample : samples) {
            // Check if sample is verified (COMPLETED status)
            if (!Status.COMPLETED.equals(sample.getStatus())) {
                String externalId = getExternalIdFromSample(sample);
                errors.add("Sample " + externalId + " is not verified (status: " + sample.getStatus() + ")");
                continue;
            }

            // Check if reception metadata is complete
            if (sample.getData() == null) {
                String externalId = getExternalIdFromSample(sample);
                errors.add("Sample " + externalId + " has no reception metadata");
                continue;
            }

            Map<String, Object> data = sample.getData();
            String externalId = getExternalIdFromSample(sample);

            // Check required metadata fields
            if (data.get("receptionDateTime") == null || data.get("receptionDateTime").toString().trim().isEmpty()) {
                errors.add("Sample " + externalId + " missing reception date/time");
            }
            if (data.get("source") == null || data.get("source").toString().trim().isEmpty()) {
                errors.add("Sample " + externalId + " missing source information");
            }
            if (data.get("testType") == null || data.get("testType").toString().trim().isEmpty()) {
                errors.add("Sample " + externalId + " missing test type");
            }
        }
    }

    private void validateStage2Completion(List<NotebookPageSample> samples, List<String> errors) {
        for (NotebookPageSample sample : samples) {
            // Check if sample has completed virus culture workflow
            if (!Status.COMPLETED.equals(sample.getStatus())) {
                String externalId = getExternalIdFromSample(sample);
                errors.add("Sample " + externalId + " virus culture workflow not completed (status: " + sample.getStatus() + ")");
                continue;
            }

            // Additional validation could be added here to check:
            // - All 9 culture steps completed
            // - QC results passed
            // - Batch marked as WORKFLOW_COMPLETE
            // This would require additional DAO methods to check culture batch status
        }
    }

    private void updateSampleStage(NotebookPageSample sample, String toStage, String userId, Timestamp now) {
        // Update the sample's stage assignment in its data
        Map<String, Object> data = sample.getData() != null ? sample.getData() : new HashMap<>();

        // Set the new stage
        data.put("workflowStage", toStage);
        data.put("stageProgressedDate", now.toString());
        data.put("stageProgressedBy", userId);

        sample.setData(data);

        // Reset status to PENDING for the new stage
        sample.setStatus(Status.PENDING);
    }

    private String getExternalIdFromSample(NotebookPageSample sample) {
        if (sample.getData() != null && sample.getData().get("externalId") != null) {
            return sample.getData().get("externalId").toString();
        }
        return sample.getSampleItemId();  // Fallback to sample item ID
    }

    private void createProgressionAuditTrail(Long entryId, String fromStage, String toStage, List<Long> sampleIds, String userId, Timestamp timestamp) {
        // This would create an audit trail entry for the progression
        // For now, just log it
        log.info("AUDIT: User " + userId + " progressed " + sampleIds.size() + " samples in entry " + entryId +
                 " from " + fromStage + " to " + toStage + " at " + timestamp);
    }

    private Map<String, Object> getStageStatus(Long entryId, String stage) {
        Map<String, Object> stageStatus = new HashMap<>();
        stageStatus.put("stage", stage);
        stageStatus.put("status", "UNKNOWN"); // Would be implemented to get actual status
        return stageStatus;
    }
}