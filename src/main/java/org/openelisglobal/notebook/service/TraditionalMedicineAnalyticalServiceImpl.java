package org.openelisglobal.notebook.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of Traditional Medicine Analytical Pathways service.
 *
 * Per SRS Requirements - STAGE 6: Analytical Pathways (Page 6)
 *
 * Branching Workflow:
 * PATH A: Advanced Analysis (Mandatory all 4 steps)
 *   - Step 1: Fractionation
 *   - Step 2: Identification/Isolation
 *   - Step 3: Purification
 *   - Step 4: Characterization
 * PATH B: Direct to Production (skips this page)
 *
 * Pathway is selected at end of Page 5 and LOCKED (cannot be changed).
 */
@Service
public class TraditionalMedicineAnalyticalServiceImpl
    implements TraditionalMedicineAnalyticalService {

  private static final Logger log =
      LoggerFactory.getLogger(TraditionalMedicineAnalyticalServiceImpl.class);

  @Autowired private NotebookPageSampleService notebookPageSampleService;

  @Override
  @Transactional
  public AnalyticalResponse selectPathway(
      Integer pageId, PathwaySelectionRequest request, String sysUserId) {
    if (request == null || request.sampleIds() == null || request.sampleIds().isEmpty()) {
      return AnalyticalResponse.error("No sample IDs provided");
    }

    if (request.analyticalPathway() == null || request.analyticalPathway().isBlank()) {
      return AnalyticalResponse.error("Analytical pathway is required");
    }

    AnalyticalPathway pathway = AnalyticalPathway.fromId(request.analyticalPathway());
    if (pathway == null) {
      return AnalyticalResponse.error("Invalid analytical pathway specified");
    }

    int updatedCount = 0;
    String timestamp = Instant.now().toString();

    for (Integer sampleId : request.sampleIds()) {
      try {
        NotebookPageSample nps =
            notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
        if (nps == null) {
          log.warn(
              "NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
          continue;
        }

        Map<String, Object> data =
            nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Store pathway selection (LOCKED - cannot be changed after selection)
        data.put("selectedPath", pathway.getId());
        data.put("selectedPathLabel", pathway.getLabel());
        data.put("pathwaySelectedAt", timestamp);
        data.put("pathwaySelectedBy", sysUserId);

        nps.setData(data);
        nps.setSysUserId(sysUserId);

        if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
            && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
          nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
        }

        notebookPageSampleService.update(nps);
        updatedCount++;

        log.info("Pathway selected for sample {}: pathway={}", sampleId, pathway.getLabel());

      } catch (Exception e) {
        log.error(
            "Error selecting pathway for sample {}: {}",
            sampleId,
            e.getMessage(),
            e);
      }
    }

    String message = String.format("Pathway selected for %d sample(s).", updatedCount);
    log.info("Pathway selection complete: pageId={}, pathway={}, updated={}", pageId,
        pathway.getLabel(), updatedCount);

    return AnalyticalResponse.success(updatedCount, message);
  }

  @Override
  @Transactional
  public AnalyticalResponse recordFractionation(
      Integer pageId, FractionationRequest request, String sysUserId) {
    if (request == null || request.sampleIds() == null || request.sampleIds().isEmpty()) {
      return AnalyticalResponse.error("No sample IDs provided");
    }

    if (request.chromatographyMethod() == null || request.chromatographyMethod().isBlank()) {
      return AnalyticalResponse.error("Chromatography method is required");
    }

    ChromatographyMethod method = ChromatographyMethod.fromId(request.chromatographyMethod());
    if (method == null) {
      return AnalyticalResponse.error("Invalid chromatography method specified");
    }

    int updatedCount = 0;
    String timestamp = Instant.now().toString();

    for (Integer sampleId : request.sampleIds()) {
      try {
        NotebookPageSample nps =
            notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
        if (nps == null) {
          log.warn(
              "NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
          continue;
        }

        Map<String, Object> data =
            nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Store fractionation data
        data.put("chromatographyMethod", method.getId());
        data.put("chromatographyMethodLabel", method.getLabel());

        if (request.numberOfFractions() != null) {
          data.put("numberOfFractions", request.numberOfFractions());
        }
        if (request.fractionLabels() != null && !request.fractionLabels().isBlank()) {
          data.put("fractionLabels", request.fractionLabels());
        }
        if (request.fractionDescription() != null && !request.fractionDescription().isBlank()) {
          data.put("fractionDescription", request.fractionDescription());
        }
        if (request.fractionationNotes() != null && !request.fractionationNotes().isBlank()) {
          data.put("fractionationNotes", request.fractionationNotes());
        }

        // Mark step as complete
        data.put("fractionationComplete", true);
        data.put("fractionationCompletedAt", timestamp);
        data.put("fractionationCompletedBy", sysUserId);

        nps.setData(data);
        nps.setSysUserId(sysUserId);

        if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
            && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
          nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
        }

        notebookPageSampleService.update(nps);
        updatedCount++;

        log.info(
            "Fractionation recorded for sample {}: method={}, fractions={}",
            sampleId,
            method.getLabel(),
            request.numberOfFractions());

      } catch (Exception e) {
        log.error(
            "Error recording fractionation for sample {}: {}",
            sampleId,
            e.getMessage(),
            e);
      }
    }

    String message = String.format("Recorded fractionation for %d sample(s).", updatedCount);
    log.info("Fractionation recorded: pageId={}, updated={}", pageId, updatedCount);

    return AnalyticalResponse.success(updatedCount, message);
  }

  @Override
  @Transactional
  public AnalyticalResponse recordIdentification(
      Integer pageId, IdentificationRequest request, String sysUserId) {
    if (request == null || request.sampleIds() == null || request.sampleIds().isEmpty()) {
      return AnalyticalResponse.error("No sample IDs provided");
    }

    if (request.detectionMethod() == null || request.detectionMethod().isBlank()) {
      return AnalyticalResponse.error("Detection method is required");
    }

    DetectionMethod method = DetectionMethod.fromId(request.detectionMethod());
    if (method == null) {
      return AnalyticalResponse.error("Invalid detection method specified");
    }

    int updatedCount = 0;
    String timestamp = Instant.now().toString();

    for (Integer sampleId : request.sampleIds()) {
      try {
        NotebookPageSample nps =
            notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
        if (nps == null) {
          log.warn(
              "NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
          continue;
        }

        Map<String, Object> data =
            nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Store identification data
        data.put("detectionMethod", method.getId());
        data.put("detectionMethodLabel", method.getLabel());

        if (request.activeConstituentsFound() != null
            && !request.activeConstituentsFound().isBlank()) {
          data.put("activeConstituentsFound", request.activeConstituentsFound());
        }
        if (request.knownCompoundsIdentified() != null
            && !request.knownCompoundsIdentified().isBlank()) {
          data.put("knownCompoundsIdentified", request.knownCompoundsIdentified());
        }
        if (request.identificationNotes() != null && !request.identificationNotes().isBlank()) {
          data.put("identificationNotes", request.identificationNotes());
        }

        // Mark step as complete
        data.put("identificationComplete", true);
        data.put("identificationCompletedAt", timestamp);
        data.put("identificationCompletedBy", sysUserId);

        nps.setData(data);
        nps.setSysUserId(sysUserId);

        if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
            && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
          nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
        }

        notebookPageSampleService.update(nps);
        updatedCount++;

        log.info("Identification recorded for sample {}: method={}", sampleId, method.getLabel());

      } catch (Exception e) {
        log.error(
            "Error recording identification for sample {}: {}",
            sampleId,
            e.getMessage(),
            e);
      }
    }

    String message = String.format("Recorded identification for %d sample(s).", updatedCount);
    log.info("Identification recorded: pageId={}, updated={}", pageId, updatedCount);

    return AnalyticalResponse.success(updatedCount, message);
  }

  @Override
  @Transactional
  public AnalyticalResponse recordPurification(
      Integer pageId, PurificationRequest request, String sysUserId) {
    if (request == null || request.sampleIds() == null || request.sampleIds().isEmpty()) {
      return AnalyticalResponse.error("No sample IDs provided");
    }

    if (request.purificationMethod() == null || request.purificationMethod().isBlank()) {
      return AnalyticalResponse.error("Purification method is required");
    }

    PurificationMethod method = PurificationMethod.fromId(request.purificationMethod());
    if (method == null) {
      return AnalyticalResponse.error("Invalid purification method specified");
    }

    int updatedCount = 0;
    String timestamp = Instant.now().toString();

    for (Integer sampleId : request.sampleIds()) {
      try {
        NotebookPageSample nps =
            notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
        if (nps == null) {
          log.warn(
              "NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
          continue;
        }

        Map<String, Object> data =
            nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Store purification data
        data.put("purificationMethod", method.getId());
        data.put("purificationMethodLabel", method.getLabel());

        if (request.purityLevel() != null) {
          data.put("purityLevel", request.purityLevel().toString());
        }
        if (request.purityAssessmentMethod() != null
            && !request.purityAssessmentMethod().isBlank()) {
          PurityAssessmentMethod assessmentMethod =
              PurityAssessmentMethod.fromId(request.purityAssessmentMethod());
          data.put("purityAssessmentMethod", request.purityAssessmentMethod());
          if (assessmentMethod != null) {
            data.put("purityAssessmentMethodLabel", assessmentMethod.getLabel());
          }
        }
        if (request.purificationNotes() != null && !request.purificationNotes().isBlank()) {
          data.put("purificationNotes", request.purificationNotes());
        }

        // Mark step as complete
        data.put("purificationComplete", true);
        data.put("purificationCompletedAt", timestamp);
        data.put("purificationCompletedBy", sysUserId);

        nps.setData(data);
        nps.setSysUserId(sysUserId);

        if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
            && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
          nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
        }

        notebookPageSampleService.update(nps);
        updatedCount++;

        log.info(
            "Purification recorded for sample {}: method={}, purity={}%",
            sampleId,
            method.getLabel(),
            request.purityLevel());

      } catch (Exception e) {
        log.error(
            "Error recording purification for sample {}: {}",
            sampleId,
            e.getMessage(),
            e);
      }
    }

    String message = String.format("Recorded purification for %d sample(s).", updatedCount);
    log.info("Purification recorded: pageId={}, updated={}", pageId, updatedCount);

    return AnalyticalResponse.success(updatedCount, message);
  }

  @Override
  @Transactional
  public AnalyticalResponse recordCharacterization(
      Integer pageId, CharacterizationRequest request, String sysUserId) {
    if (request == null || request.sampleIds() == null || request.sampleIds().isEmpty()) {
      return AnalyticalResponse.error("No sample IDs provided");
    }

    if (request.spectroscopyTechniques() == null
        || request.spectroscopyTechniques().isEmpty()) {
      return AnalyticalResponse.error("At least one spectroscopy technique is required");
    }

    int updatedCount = 0;
    String timestamp = Instant.now().toString();

    for (Integer sampleId : request.sampleIds()) {
      try {
        NotebookPageSample nps =
            notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
        if (nps == null) {
          log.warn(
              "NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
          continue;
        }

        Map<String, Object> data =
            nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Store characterization data
        data.put("spectroscopyTechniques", request.spectroscopyTechniques());

        // Build labels for techniques
        List<String> techniqueLabels = new ArrayList<>();
        for (String techId : request.spectroscopyTechniques()) {
          SpectroscopyTechnique technique = SpectroscopyTechnique.fromId(techId);
          techniqueLabels.add(
              technique != null ? technique.getLabel() : techId);
        }
        data.put("spectroscopyTechniqueLabels", techniqueLabels);

        if (request.structureDetermination() != null
            && !request.structureDetermination().isBlank()) {
          data.put("structureDetermination", request.structureDetermination());
        }
        if (request.propertiesIdentified() != null
            && !request.propertiesIdentified().isBlank()) {
          data.put("propertiesIdentified", request.propertiesIdentified());
        }
        if (request.characterizationNotes() != null
            && !request.characterizationNotes().isBlank()) {
          data.put("characterizationNotes", request.characterizationNotes());
        }

        // Mark step as complete
        data.put("characterizationComplete", true);
        data.put("characterizationCompletedAt", timestamp);
        data.put("characterizationCompletedBy", sysUserId);

        nps.setData(data);
        nps.setSysUserId(sysUserId);

        if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
            && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
          nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
        }

        notebookPageSampleService.update(nps);
        updatedCount++;

        log.info(
            "Characterization recorded for sample {}: techniques={}",
            sampleId,
            String.join(",", request.spectroscopyTechniques()));

      } catch (Exception e) {
        log.error(
            "Error recording characterization for sample {}: {}",
            sampleId,
            e.getMessage(),
            e);
      }
    }

    String message =
        String.format("Recorded characterization for %d sample(s).", updatedCount);
    log.info("Characterization recorded: pageId={}, updated={}", pageId, updatedCount);

    return AnalyticalResponse.success(updatedCount, message);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Integer, Map<String, Object>> getAnalyticalStatus(Integer pageId) {
    Map<Integer, Map<String, Object>> statusMap = new HashMap<>();

    List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);
    for (NotebookPageSample nps : samples) {
      Map<String, Object> analyticalData = new HashMap<>();

      if (nps.getData() != null) {
        Map<String, Object> data = nps.getData();
        // Copy all analytical-related fields
        analyticalData.put("selectedPath", data.get("selectedPath"));
        analyticalData.put("selectedPathLabel", data.get("selectedPathLabel"));
        analyticalData.put(
            "fractionationComplete", data.get("fractionationComplete"));
        analyticalData.put(
            "identificationComplete", data.get("identificationComplete"));
        analyticalData.put("purificationComplete", data.get("purificationComplete"));
        analyticalData.put(
            "characterizationComplete", data.get("characterizationComplete"));
        analyticalData.put(
            "chromatographyMethod", data.get("chromatographyMethod"));
        analyticalData.put(
            "chromatographyMethodLabel",
            data.get("chromatographyMethodLabel"));
        analyticalData.put("detectionMethod", data.get("detectionMethod"));
        analyticalData.put("detectionMethodLabel", data.get("detectionMethodLabel"));
        analyticalData.put("purificationMethod", data.get("purificationMethod"));
        analyticalData.put(
            "purificationMethodLabel", data.get("purificationMethodLabel"));
        analyticalData.put("purityLevel", data.get("purityLevel"));
        analyticalData.put(
            "spectroscopyTechniques", data.get("spectroscopyTechniques"));
      }

      analyticalData.put("status", nps.getStatus());
      String sampleItemIdStr = nps.getSampleItemId();
      analyticalData.put("sampleItemId", sampleItemIdStr);

      if (sampleItemIdStr != null && !sampleItemIdStr.isBlank()) {
        try {
          Integer sampleId = Integer.parseInt(sampleItemIdStr);
          statusMap.put(sampleId, analyticalData);
        } catch (NumberFormatException e) {
          log.warn("Invalid sampleItemId format: {}", sampleItemIdStr);
        }
      }
    }

    return statusMap;
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> validateAllStepsComplete(Integer pageId, List<Integer> sampleIds) {
    List<String> errors = new ArrayList<>();

    if (sampleIds == null || sampleIds.isEmpty()) {
      errors.add("No sample IDs provided");
      return errors;
    }

    for (Integer sampleId : sampleIds) {
      try {
        NotebookPageSample nps =
            notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
        if (nps == null) {
          errors.add(String.format("Sample %d not found", sampleId));
          continue;
        }

        Map<String, Object> data = nps.getData();
        if (data == null) {
          errors.add(String.format("Sample %d: No analytical data recorded", sampleId));
          continue;
        }

        // Check all 4 steps are complete
        if (!Boolean.TRUE.equals(data.get("fractionationComplete"))) {
          errors.add(String.format("Sample %d: Fractionation not complete", sampleId));
        }
        if (!Boolean.TRUE.equals(data.get("identificationComplete"))) {
          errors.add(String.format("Sample %d: Identification not complete", sampleId));
        }
        if (!Boolean.TRUE.equals(data.get("purificationComplete"))) {
          errors.add(String.format("Sample %d: Purification not complete", sampleId));
        }
        if (!Boolean.TRUE.equals(data.get("characterizationComplete"))) {
          errors.add(
              String.format("Sample %d: Characterization not complete", sampleId));
        }

      } catch (Exception e) {
        log.error(
            "Error validating sample {}: {}",
            sampleId,
            e.getMessage(),
            e);
        errors.add(String.format("Sample %d: Validation error", sampleId));
      }
    }

    return errors;
  }

  @Override
  @Transactional
  public AnalyticalResponse markAnalysisComplete(
      Integer pageId, List<Integer> sampleIds, String sysUserId) {
    if (sampleIds == null || sampleIds.isEmpty()) {
      return AnalyticalResponse.error("No sample IDs provided");
    }

    List<String> validationErrors = validateAllStepsComplete(pageId, sampleIds);
    if (!validationErrors.isEmpty()) {
      return AnalyticalResponse.error(String.join("; ", validationErrors));
    }

    // Use bulkUpdateStatus to mark complete and route to next page
    // This ensures samples are properly advanced to Page 7
    int updatedCount =
        notebookPageSampleService.bulkUpdateStatus(
            pageId, sampleIds, NotebookPageSample.Status.COMPLETED, sysUserId);

    String message = String.format("Marked %d sample(s) analysis as complete.", updatedCount);
    log.info(
        "Analysis marked complete: pageId={}, updated={}, routing to next page", pageId, updatedCount);

    return AnalyticalResponse.success(updatedCount, message);
  }
}
