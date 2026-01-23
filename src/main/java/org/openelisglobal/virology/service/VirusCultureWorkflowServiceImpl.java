package org.openelisglobal.virology.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.virology.dao.VirusCultureBatchDAO;
import org.openelisglobal.virology.dao.VirusCultureWorkflowStatusDAO;
import org.openelisglobal.virology.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing the complete virus culture workflow
 */
@Service
@Transactional
public class VirusCultureWorkflowServiceImpl implements VirusCultureWorkflowService {

    @Autowired
    private VirusCultureBatchDAO virusCultureBatchDAO;

    @Autowired
    private VirusCultureWorkflowStatusDAO workflowStatusDAO;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @PersistenceContext
    private EntityManager entityManager;

    // ==========================================
    // VIRUS CULTURE BATCH MANAGEMENT
    // ==========================================

    @Override
    public VirusCultureBatch createVirusCultureBatch(NotebookPageSample notebookPageSample, String virusStrain,
            String cellLine, SystemUser createdBy) {
        // Generate unique batch ID
        String batchId = generateBatchId(virusStrain);

        // Create new batch
        VirusCultureBatch batch = new VirusCultureBatch(batchId, notebookPageSample);
        batch.setVirusStrain(virusStrain);
        batch.setCellLineUsed(cellLine);
        batch.setCreatedBy(createdBy);
        batch.setStatus(VirusCultureBatch.BatchStatus.MEDIA_PREP_PENDING);

        // Save the batch
        virusCultureBatchDAO.insert(batch);
        return batch;
    }

    @Override
    public VirusCultureBatch getVirusCultureBatch(Integer id) {
        return virusCultureBatchDAO.get(id).orElse(null);
    }

    @Override
    public VirusCultureBatch getVirusCultureBatchByBatchId(String batchId) {
        return virusCultureBatchDAO.findByBatchId(batchId);
    }

    @Override
    public List<VirusCultureBatch> getVirusCultureBatchesBySample(Integer notebookPageSampleId) {
        NotebookPageSample sample = notebookPageSampleService.get(notebookPageSampleId);
        if (sample == null) {
            return new ArrayList<>();
        }
        return virusCultureBatchDAO.findByNotebookPageSample(sample);
    }

    @Override
    public List<VirusCultureBatch> getActiveVirusCultureBatches() {
        return virusCultureBatchDAO.findActiveBatches();
    }

    @Override
    public List<VirusCultureBatch> getBatchesRequiringAttention() {
        return virusCultureBatchDAO.findBatchesRequiringAttention();
    }

    @Override
    public VirusCultureBatch updateVirusCultureBatch(VirusCultureBatch batch) {
        return virusCultureBatchDAO.update(batch);
    }

    @Override
    public void cancelVirusCultureBatch(Integer batchId, String reason, SystemUser cancelledBy) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            VirusCultureBatch batch = batchOpt.get();
            batch.setStatus(VirusCultureBatch.BatchStatus.CANCELLED);
            virusCultureBatchDAO.update(batch);

            // Update workflow status
            VirusCultureWorkflowStatus currentStep = getCurrentWorkflowStep(batchId);
            if (currentStep != null) {
                currentStep.setStatus(VirusCultureWorkflowStatus.StepStatus.FAILED);
                currentStep.setNotes(reason);
                currentStep.setCompletedBy(cancelledBy);
                currentStep.setCompletedDate(new Timestamp(System.currentTimeMillis()));
                workflowStatusDAO.update(currentStep);
            }
        }
    }

    // ==========================================
    // WORKFLOW STEP MANAGEMENT
    // ==========================================

    @Override
    public List<VirusCultureWorkflowStatus> getWorkflowStatus(Integer batchId) {
        return workflowStatusDAO.findByCultureBatchId(batchId);
    }

    @Override
    public VirusCultureWorkflowStatus getCurrentWorkflowStep(Integer batchId) {
        List<VirusCultureWorkflowStatus> statuses = workflowStatusDAO.findByCultureBatchId(batchId);
        return statuses.stream()
                .filter(status -> status.getStatus() == VirusCultureWorkflowStatus.StepStatus.IN_PROGRESS
                        || status.getStatus() == VirusCultureWorkflowStatus.StepStatus.PENDING)
                .min(Comparator.comparing(VirusCultureWorkflowStatus::getStepOrder)).orElse(null);
    }

    @Override
    public VirusCultureWorkflowStatus startWorkflowStep(Integer batchId, String stepName, SystemUser assignedTo) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            VirusCultureBatch batch = batchOpt.get();
            VirusCultureWorkflowStatus step = workflowStatusDAO.findByCultureBatchAndStepName(batch, stepName);
            if (step != null && step.getStatus() == VirusCultureWorkflowStatus.StepStatus.PENDING) {
                step.setStatus(VirusCultureWorkflowStatus.StepStatus.IN_PROGRESS);
                step.setAssignedTo(assignedTo);
                step.setStartedDate(new Timestamp(System.currentTimeMillis()));
                return workflowStatusDAO.update(step);
            }
            return step;
        }
        return null;
    }

    @Override
    public VirusCultureWorkflowStatus completeWorkflowStep(Integer batchId, String stepName, SystemUser completedBy,
            VirusCultureWorkflowStatus.QualityCheckResult qualityResult, String notes) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            VirusCultureBatch batch = batchOpt.get();
            VirusCultureWorkflowStatus step = workflowStatusDAO.findByCultureBatchAndStepName(batch, stepName);
            if (step != null && step.getStatus() == VirusCultureWorkflowStatus.StepStatus.IN_PROGRESS) {
                step.setStatus(VirusCultureWorkflowStatus.StepStatus.COMPLETED);
                step.setCompletedBy(completedBy);
                step.setCompletedDate(new Timestamp(System.currentTimeMillis()));
                step.setQualityCheckResult(qualityResult);
                step.setNotes(notes);

                VirusCultureWorkflowStatus updatedStep = workflowStatusDAO.update(step);

                // Auto-advance to next step if possible
                autoAdvanceWorkflow(batchId);

                return updatedStep;
            }
            return step;
        }
        return null;
    }

    @Override
    public VirusCultureWorkflowStatus failWorkflowStep(Integer batchId, String stepName, SystemUser failedBy,
            String reason) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            VirusCultureBatch batch = batchOpt.get();
            VirusCultureWorkflowStatus step = workflowStatusDAO.findByCultureBatchAndStepName(batch, stepName);
            if (step != null) {
                step.setStatus(VirusCultureWorkflowStatus.StepStatus.FAILED);
                step.setCompletedBy(failedBy);
                step.setCompletedDate(new Timestamp(System.currentTimeMillis()));
                step.setNotes(reason);

                // Mark the batch as failed
                batch.setStatus(VirusCultureBatch.BatchStatus.FAILED);
                virusCultureBatchDAO.update(batch);

                return workflowStatusDAO.update(step);
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getWorkflowProgress(Integer batchId) {
        Map<String, Object> progress = new HashMap<>();
        List<VirusCultureWorkflowStatus> statuses = workflowStatusDAO.findByCultureBatchId(batchId);

        long completedSteps = statuses.stream()
                .filter(s -> s.getStatus() == VirusCultureWorkflowStatus.StepStatus.COMPLETED).count();

        progress.put("totalSteps", statuses.size());
        progress.put("completedSteps", completedSteps);
        progress.put("progressPercentage", statuses.size() > 0 ? (completedSteps * 100.0 / statuses.size()) : 0);
        progress.put("currentStep", getCurrentWorkflowStep(batchId));
        progress.put("allSteps", statuses);

        return progress;
    }

    // ==========================================
    // PROCESS STEP OPERATIONS (STUB IMPLEMENTATIONS)
    // ==========================================

    @Override
    public VirusCultureMediaPreparation recordMediaPreparation(Integer batchId,
            VirusCultureMediaPreparation mediaPreparation) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            mediaPreparation.setCultureBatch(batchOpt.get());
            mediaPreparation.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(mediaPreparation);
            entityManager.flush();
        }
        return mediaPreparation;
    }

    @Override
    public VirusCultureSterilization recordSterilization(Integer batchId, VirusCultureSterilization sterilization) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            sterilization.setCultureBatch(batchOpt.get());
            sterilization.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(sterilization);
            entityManager.flush();
        }
        return sterilization;
    }

    @Override
    public VirusCultureCellCulture recordCellCulture(Integer batchId, VirusCultureCellCulture cellCulture) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            cellCulture.setCultureBatch(batchOpt.get());
            cellCulture.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(cellCulture);
            entityManager.flush();
        }
        return cellCulture;
    }

    @Override
    public VirusCultureQualityControl recordQualityControl(Integer batchId, VirusCultureQualityControl qualityControl) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            qualityControl.setCultureBatch(batchOpt.get());
            qualityControl.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(qualityControl);
            entityManager.flush();
        }
        return qualityControl;
    }

    @Override
    public VirusCultureVirusInoculation recordVirusInoculation(Integer batchId,
            VirusCultureVirusInoculation virusInoculation) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            virusInoculation.setCultureBatch(batchOpt.get());
            virusInoculation.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(virusInoculation);
            entityManager.flush();
        }
        return virusInoculation;
    }

    @Override
    public VirusCultureImaging recordImaging(Integer batchId, VirusCultureImaging imaging) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            imaging.setCultureBatch(batchOpt.get());
            imaging.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(imaging);
            entityManager.flush();
        }
        return imaging;
    }

    @Override
    public VirusCultureFormulation recordFormulation(Integer batchId, VirusCultureFormulation formulation) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            formulation.setCultureBatch(batchOpt.get());
            formulation.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(formulation);
            entityManager.flush();
        }
        return formulation;
    }

    @Override
    public VirusCultureFeeding recordFeeding(Integer batchId, VirusCultureFeeding feeding) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            feeding.setCultureBatch(batchOpt.get());
            feeding.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(feeding);
            entityManager.flush();
        }
        return feeding;
    }

    @Override
    public VirusCulturePackaging recordPackaging(Integer batchId, VirusCulturePackaging packaging) {
        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (batchOpt.isPresent()) {
            packaging.setCultureBatch(batchOpt.get());
            packaging.setLastupdated(new Timestamp(System.currentTimeMillis()));
            entityManager.persist(packaging);
            entityManager.flush();
        }
        return packaging;
    }

    // ==========================================
    // VALIDATION AND BUSINESS LOGIC
    // ==========================================

    @Override
    public Map<String, Object> validateStepCanStart(Integer batchId, String stepName) {
        Map<String, Object> validation = new HashMap<>();
        validation.put("canStart", true);
        validation.put("reasons", new ArrayList<String>());

        Optional<VirusCultureBatch> batchOpt = virusCultureBatchDAO.get(batchId);
        if (!batchOpt.isPresent()) {
            validation.put("canStart", false);
            ((List<String>) validation.get("reasons")).add("Batch not found");
            return validation;
        }

        VirusCultureBatch batch = batchOpt.get();
        if (!batch.isActive()) {
            validation.put("canStart", false);
            ((List<String>) validation.get("reasons")).add("Batch is not active");
            return validation;
        }

        // Check if previous steps are completed
        List<VirusCultureWorkflowStatus> statuses = workflowStatusDAO.findByCultureBatchId(batchId);
        VirusCultureWorkflowStatus targetStep = statuses.stream().filter(s -> s.getStepName().equals(stepName))
                .findFirst().orElse(null);

        if (targetStep == null) {
            validation.put("canStart", false);
            ((List<String>) validation.get("reasons")).add("Step not found in workflow");
            return validation;
        }

        // Check if any previous steps are incomplete
        boolean previousStepsComplete = statuses.stream().filter(s -> s.getStepOrder() < targetStep.getStepOrder())
                .allMatch(s -> s.getStatus() == VirusCultureWorkflowStatus.StepStatus.COMPLETED);

        if (!previousStepsComplete) {
            validation.put("canStart", false);
            ((List<String>) validation.get("reasons")).add("Previous steps must be completed first");
        }

        return validation;
    }

    @Override
    public Map<String, Object> validateBatchCreation(Integer notebookPageSampleId) {
        Map<String, Object> validation = new HashMap<>();
        validation.put("canCreate", true);
        validation.put("reasons", new ArrayList<String>());

        NotebookPageSample sample = notebookPageSampleService.get(notebookPageSampleId);
        if (sample == null) {
            validation.put("canCreate", false);
            ((List<String>) validation.get("reasons")).add("Sample not found");
            return validation;
        }

        // Check if there are already active batches for this sample
        List<VirusCultureBatch> existingBatches = virusCultureBatchDAO.findByNotebookPageSample(sample);
        boolean hasActiveBatch = existingBatches.stream().anyMatch(VirusCultureBatch::isActive);

        if (hasActiveBatch) {
            validation.put("canCreate", false);
            ((List<String>) validation.get("reasons"))
                    .add("There is already an active virus culture batch for this sample");
        }

        return validation;
    }

    @Override
    public Map<String, Object> getWorkflowStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<VirusCultureBatch> allBatches = virusCultureBatchDAO.getAll();

        stats.put("totalBatches", allBatches.size());
        stats.put("activeBatches", allBatches.stream().filter(VirusCultureBatch::isActive).count());
        stats.put("completedBatches", allBatches.stream().filter(VirusCultureBatch::isWorkflowComplete).count());
        stats.put("failedBatches", allBatches.stream().filter(VirusCultureBatch::isFailed).count());
        stats.put("cancelledBatches", allBatches.stream().filter(VirusCultureBatch::isCancelled).count());

        return stats;
    }

    @Override
    public String generateBatchId(String virusStrain) {
        String strainPrefix = virusStrain != null && virusStrain.length() >= 3
                ? virusStrain.substring(0, 3).toUpperCase()
                : "VCB";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return strainPrefix + "-" + timestamp;
    }

    @Override
    public boolean autoAdvanceWorkflow(Integer batchId) {
        VirusCultureWorkflowStatus currentStep = getCurrentWorkflowStep(batchId);
        if (currentStep == null) {
            return false;
        }

        // Get next step
        List<VirusCultureWorkflowStatus> allSteps = workflowStatusDAO.findByCultureBatchId(batchId);
        Optional<VirusCultureWorkflowStatus> nextStep = allSteps.stream()
                .filter(s -> s.getStepOrder() == currentStep.getStepOrder() + 1).findFirst();

        if (nextStep.isPresent() && nextStep.get().getStatus() == VirusCultureWorkflowStatus.StepStatus.PENDING) {
            // Auto-start next step
            nextStep.get().setStatus(VirusCultureWorkflowStatus.StepStatus.PENDING);
            workflowStatusDAO.update(nextStep.get());
            return true;
        }

        return false;
    }

    @Override
    public List<VirusCultureBatch> checkFeedingDue() {
        // Get active batches that are in VIRUS_CULTURE or later stages
        List<VirusCultureBatch> activeBatches = virusCultureBatchDAO.findActiveBatches();
        List<VirusCultureBatch> feedingDue = new ArrayList<>();

        // Feeding is typically required during virus culture phase (24-72 hour
        // intervals)
        long feedingInterval = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
        long currentTime = System.currentTimeMillis();

        for (VirusCultureBatch batch : activeBatches) {
            // Check if batch is in a stage that requires feeding
            if (batch.getStatus() == VirusCultureBatch.BatchStatus.VIRUS_CULTURE_PENDING
                    || batch.getStatus() == VirusCultureBatch.BatchStatus.VIRUS_CULTURE_IN_PROGRESS
                    || batch.getStatus() == VirusCultureBatch.BatchStatus.DARK_ROOM_IMAGING_PENDING
                    || batch.getStatus() == VirusCultureBatch.BatchStatus.DARK_ROOM_IMAGING_IN_PROGRESS) {

                // Get last feeding record for this batch
                // For now, we'll use a simple time-based check from batch creation
                long timeSinceCreation = currentTime - batch.getCreatedDate().getTime();
                if (timeSinceCreation > feedingInterval) {
                    feedingDue.add(batch);
                }
            }
        }

        return feedingDue;
    }

    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        // Get basic statistics
        List<VirusCultureBatch> allBatches = virusCultureBatchDAO.getAll();
        List<VirusCultureBatch> activeBatches = virusCultureBatchDAO.findActiveBatches();
        List<VirusCultureBatch> batchesRequiringAttention = getBatchesRequiringAttention();

        // Calculate today's completed batches
        long todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        long completedToday = allBatches.stream().filter(batch -> batch.isWorkflowComplete()
                && batch.getLastupdated() != null && batch.getLastupdated().getTime() >= todayStart).count();

        // Calculate success rate (completed vs total non-cancelled batches)
        long totalNonCancelled = allBatches.stream().filter(batch -> !batch.isCancelled()).count();
        double successRate = totalNonCancelled > 0
                ? (double) allBatches.stream().filter(VirusCultureBatch::isWorkflowComplete).count() / totalNonCancelled
                        * 100
                : 0;

        // Frontend-expected fields
        dashboard.put("activeBatches", activeBatches.size());
        dashboard.put("completedToday", completedToday);
        dashboard.put("requiresAttention", batchesRequiringAttention.size());
        dashboard.put("successRate", Math.round(successRate));

        // Additional useful data
        dashboard.put("totalBatches", allBatches.size());
        dashboard.put("recentBatches", virusCultureBatchDAO.findRecentBatches(10));
        dashboard.put("batchesRequiringAttention", batchesRequiringAttention);
        dashboard.put("feedingDue", checkFeedingDue());

        return dashboard;
    }

    @Override
    public Map<String, Object> validateQualityControlResults(VirusCultureQualityControl qualityControl) {
        Map<String, Object> validation = new HashMap<>();
        List<String> recommendations = new ArrayList<>();

        validation.put("isValid", true);
        validation.put("recommendations", recommendations);

        // QC validation logic based on actual entity properties

        // Viability percentage validation (optimal: 80%+, acceptable: 70%+)
        if (qualityControl.getViabilityPercentage() != null) {
            if (qualityControl.getViabilityPercentage().compareTo(new BigDecimal("70.0")) < 0) {
                validation.put("isValid", false);
                recommendations
                        .add("CRITICAL: Cell viability below minimum threshold (70%). Batch should be terminated.");
            } else if (qualityControl.getViabilityPercentage().compareTo(new BigDecimal("80.0")) < 0) {
                recommendations.add("WARNING: Cell viability below optimal threshold (80%). Monitor closely.");
            }
        }

        // Sterility validation - critical for virus culture
        if (qualityControl.getSterilityResult() == VirusCultureQualityControl.SterilityResult.FAIL
                || qualityControl.getSterilityResult() == VirusCultureQualityControl.SterilityResult.CONTAMINATED) {
            validation.put("isValid", false);
            recommendations
                    .add("CRITICAL: Sterility test failed. Batch contaminated and must be terminated immediately.");
        }

        // Mycoplasma test validation
        if (qualityControl.getMycoplasmaTestResult() == VirusCultureQualityControl.SterilityResult.FAIL
                || qualityControl
                        .getMycoplasmaTestResult() == VirusCultureQualityControl.SterilityResult.CONTAMINATED) {
            validation.put("isValid", false);
            recommendations.add("CRITICAL: Mycoplasma contamination detected. Batch must be terminated.");
        }

        // Endotoxin level validation (EU/ml should be <5.0 for most cell culture
        // applications)
        if (qualityControl.getEndotoxinLevelEuMl() != null
                && qualityControl.getEndotoxinLevelEuMl().compareTo(new BigDecimal("5.0")) > 0) {
            validation.put("isValid", false);
            recommendations.add("CRITICAL: Endotoxin level too high (>5.0 EU/ml). Media may be contaminated.");
        }

        // pH validation (optimal range: 7.2-7.4 for most cell culture)
        if (qualityControl.getPhMeasurement() != null) {
            if (qualityControl.getPhMeasurement().compareTo(new BigDecimal("6.8")) < 0
                    || qualityControl.getPhMeasurement().compareTo(new BigDecimal("7.8")) > 0) {
                validation.put("isValid", false);
                recommendations.add("CRITICAL: pH outside acceptable range (6.8-7.8). Check medium composition.");
            } else if (qualityControl.getPhMeasurement().compareTo(new BigDecimal("7.2")) < 0
                    || qualityControl.getPhMeasurement().compareTo(new BigDecimal("7.4")) > 0) {
                recommendations.add("WARNING: pH outside optimal range (7.2-7.4). Monitor culture conditions.");
            }
        }

        // Overall result interpretation validation
        if (qualityControl.getResultInterpretation() == VirusCultureQualityControl.ResultInterpretation.FAILED
                || qualityControl
                        .getResultInterpretation() == VirusCultureQualityControl.ResultInterpretation.OUT_OF_SPECIFICATION) {
            validation.put("isValid", false);
            recommendations
                    .add("Quality control assessment failed. Review all test results and consider batch termination.");
        } else if (qualityControl
                .getResultInterpretation() == VirusCultureQualityControl.ResultInterpretation.REQUIRES_RETEST) {
            recommendations.add("Quality control requires retesting. Verify test procedures and repeat analysis.");
        }

        return validation;
    }
}