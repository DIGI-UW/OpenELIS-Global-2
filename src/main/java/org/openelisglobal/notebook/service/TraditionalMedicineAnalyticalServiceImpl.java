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
 * Implementation of Traditional Medicine Analytical Pathway service.
 *
 * Per SRS Requirements - Advanced Analysis (Optional): - Fractionation:
 * Separate compounds, record method and fractions - Identification/Isolation:
 * Detect active constituents, link to compound database - Purification: Remove
 * impurities, log purity level - Characterization: Determine structure (NMR,
 * MS, IR spectral data)
 *
 * This pathway is OPTIONAL - samples can skip directly to Testing.
 */
@Service
public class TraditionalMedicineAnalyticalServiceImpl implements TraditionalMedicineAnalyticalService {

    private static final Logger log = LoggerFactory.getLogger(TraditionalMedicineAnalyticalServiceImpl.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Override
    @Transactional
    public AnalyticalResponse applyAnalyticalData(Integer pageId, AnalyticalRequest request, String sysUserId) {
        List<String> validationErrors = validateAnalyticalRequest(request);
        if (!validationErrors.isEmpty()) {
            return AnalyticalResponse.error(String.join("; ", validationErrors));
        }

        if (request.sampleIds() == null || request.sampleIds().isEmpty()) {
            return AnalyticalResponse.error("No sample IDs provided");
        }

        int updatedCount = 0;
        String timestamp = Instant.now().toString();

        // Get labels for methods
        FractionationMethod fractionation = FractionationMethod.fromId(request.fractionationMethod());
        PurificationMethod purification = request.purificationMethod() != null
                ? PurificationMethod.fromId(request.purificationMethod())
                : null;

        for (Integer sampleId : request.sampleIds()) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                // Store fractionation data
                if (request.fractionationMethod() != null) {
                    data.put("fractionationMethod", request.fractionationMethod());
                    data.put("fractionationMethodLabel",
                            fractionation != null ? fractionation.getLabel() : request.fractionationMethod());
                    if ("OTHER".equals(request.fractionationMethod()) && request.otherFractionationMethod() != null) {
                        data.put("otherFractionationMethod", request.otherFractionationMethod());
                        data.put("fractionationMethodLabel", request.otherFractionationMethod());
                    }
                }
                if (request.numberOfFractions() != null) {
                    data.put("numberOfFractions", request.numberOfFractions());
                }
                if (request.fractionIds() != null && !request.fractionIds().isBlank()) {
                    data.put("fractionIds", request.fractionIds());
                }
                if (request.mobilePhaseSolvent() != null) {
                    data.put("mobilePhaseSolvent", request.mobilePhaseSolvent());
                }
                if (request.stationaryPhase() != null) {
                    data.put("stationaryPhase", request.stationaryPhase());
                }

                // Store identification/isolation data
                if (request.activeConstituentsIdentified() != null
                        && !request.activeConstituentsIdentified().isBlank()) {
                    data.put("activeConstituentsIdentified", request.activeConstituentsIdentified());
                }
                if (request.compoundDatabaseLinks() != null) {
                    data.put("compoundDatabaseLinks", request.compoundDatabaseLinks());
                }
                if (request.isolatedCompoundId() != null) {
                    data.put("isolatedCompoundId", request.isolatedCompoundId());
                }
                if (request.isolatedCompoundWeight() != null) {
                    data.put("isolatedCompoundWeight", request.isolatedCompoundWeight().toString());
                    data.put("isolatedCompoundWeightUnit",
                            request.isolatedCompoundWeightUnit() != null ? request.isolatedCompoundWeightUnit() : "mg");
                }

                // Store purification data
                if (request.purificationMethod() != null) {
                    data.put("purificationMethod", request.purificationMethod());
                    data.put("purificationMethodLabel",
                            purification != null ? purification.getLabel() : request.purificationMethod());
                    if ("OTHER".equals(request.purificationMethod()) && request.otherPurificationMethod() != null) {
                        data.put("otherPurificationMethod", request.otherPurificationMethod());
                        data.put("purificationMethodLabel", request.otherPurificationMethod());
                    }
                }
                if (request.purityLevel() != null) {
                    data.put("purityLevel", request.purityLevel().toString());
                }
                if (request.purityAssessmentMethod() != null) {
                    data.put("purityAssessmentMethod", request.purityAssessmentMethod());
                }

                // Store characterization data
                if (request.characterizationTechniques() != null && !request.characterizationTechniques().isEmpty()) {
                    data.put("characterizationTechniques", request.characterizationTechniques());
                    // Build labels
                    List<String> techniqueLabels = new ArrayList<>();
                    for (String techId : request.characterizationTechniques()) {
                        CharacterizationTechnique tech = CharacterizationTechnique.fromId(techId);
                        techniqueLabels.add(tech != null ? tech.getLabel() : techId);
                    }
                    data.put("characterizationTechniqueLabels", techniqueLabels);
                    // Store individual flags for backwards compatibility
                    data.put("structuralAnalysisNMR", request.characterizationTechniques().contains("NMR"));
                    data.put("structuralAnalysisMS", request.characterizationTechniques().contains("MS"));
                    data.put("structuralAnalysisIR", request.characterizationTechniques().contains("IR"));
                }
                if (request.spectralFileReference() != null) {
                    data.put("spectralFileReference", request.spectralFileReference());
                }
                if (request.molecularFormula() != null) {
                    data.put("molecularFormula", request.molecularFormula());
                }
                if (request.molecularWeight() != null) {
                    data.put("molecularWeight", request.molecularWeight().toString());
                }
                if (request.structureDescription() != null) {
                    data.put("structureDescription", request.structureDescription());
                }

                // Store general info
                if (request.analyst() != null) {
                    data.put("analyst", request.analyst());
                }
                if (request.analysisDate() != null) {
                    data.put("analysisDate", request.analysisDate());
                }
                if (request.notes() != null) {
                    data.put("analyticalNotes", request.notes());
                }

                // Store audit info
                data.put("analyticalDataAppliedAt", timestamp);
                data.put("analyticalDataAppliedBy", sysUserId);

                nps.setData(data);
                nps.setSysUserId(sysUserId);

                if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
                        && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
                    nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                }

                notebookPageSampleService.update(nps);
                updatedCount++;

                log.info("Analytical data applied to sample {}: fractionation={}, purification={}, purity={}%",
                        sampleId, request.fractionationMethod(), request.purificationMethod(), request.purityLevel());

            } catch (Exception e) {
                log.error("Error applying analytical data to sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        String message = String.format("Applied analytical data to %d sample(s).", updatedCount);
        log.info("Analytical data applied: pageId={}, updated={}", pageId, updatedCount);

        return AnalyticalResponse.success(updatedCount, 0, 0, message);
    }

    @Override
    @Transactional
    public AnalyticalResponse markAnalyticalComplete(Integer pageId, List<Integer> sampleIds, String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return AnalyticalResponse.error("No sample IDs provided");
        }

        List<Integer> validSampleIds = new ArrayList<>();
        List<String> incompleteReasons = new ArrayList<>();

        for (Integer sampleId : sampleIds) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                Map<String, Object> data = nps.getData();
                if (data == null) {
                    incompleteReasons.add(String.format("Sample %d: No analytical data recorded", sampleId));
                    continue;
                }

                // Validate required analytical data - at minimum need fractionation
                String fractionationMethod = (String) data.get("fractionationMethod");
                if (fractionationMethod == null || fractionationMethod.isBlank()) {
                    incompleteReasons.add(String.format("Sample %d: No fractionation method specified", sampleId));
                    continue;
                }

                // Add completion metadata
                data.put("analyticalCompletedAt", Instant.now().toString());
                data.put("analyticalCompletedBy", sysUserId);
                data.put("analyticalPathwayStatus", "COMPLETED");
                nps.setData(data);
                nps.setSysUserId(sysUserId);
                notebookPageSampleService.update(nps);

                validSampleIds.add(sampleId);

            } catch (Exception e) {
                log.error("Error validating sample {}: {}", sampleId, e.getMessage(), e);
                incompleteReasons.add(String.format("Sample %d: Error during validation", sampleId));
            }
        }

        if (validSampleIds.isEmpty()) {
            if (!incompleteReasons.isEmpty()) {
                return AnalyticalResponse
                        .error("No samples could be completed. Reasons: " + String.join("; ", incompleteReasons));
            }
            return AnalyticalResponse.error("No valid samples found to complete");
        }

        // Use bulkUpdateStatus to mark as COMPLETED and advance to next page (Testing)
        int completedCount = notebookPageSampleService.bulkUpdateStatus(pageId, validSampleIds,
                NotebookPageSample.Status.COMPLETED, sysUserId);

        log.info("Analytical pathway complete: pageId={}, validated={}, completed={}", pageId, validSampleIds.size(),
                completedCount);

        String message = String.format("%d sample(s) characterized and advanced to Testing.", completedCount);
        if (!incompleteReasons.isEmpty()) {
            message += String.format(" %d sample(s) could not be completed.", incompleteReasons.size());
        }

        return AnalyticalResponse.success(0, completedCount, 0, message);
    }

    @Override
    @Transactional
    public AnalyticalResponse skipAnalyticalPathway(Integer pageId, List<Integer> sampleIds, String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return AnalyticalResponse.error("No sample IDs provided");
        }

        List<Integer> validSampleIds = new ArrayList<>();

        for (Integer sampleId : sampleIds) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                // Mark as skipped with metadata
                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();
                data.put("analyticalPathwayStatus", "SKIPPED");
                data.put("analyticalSkippedAt", Instant.now().toString());
                data.put("analyticalSkippedBy", sysUserId);
                data.put("analyticalSkipReason", "Direct to Production - extract ready post-concentration");
                nps.setData(data);
                nps.setSysUserId(sysUserId);
                notebookPageSampleService.update(nps);

                validSampleIds.add(sampleId);

            } catch (Exception e) {
                log.error("Error skipping sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        if (validSampleIds.isEmpty()) {
            return AnalyticalResponse.error("No valid samples found to skip");
        }

        // Use bulkUpdateStatus with COMPLETED to advance to next page (Testing)
        // The samples will be marked as SKIPPED on this page but still advance
        int skippedCount = notebookPageSampleService.bulkUpdateStatus(pageId, validSampleIds,
                NotebookPageSample.Status.COMPLETED, // COMPLETED triggers advancement to next page
                sysUserId);

        // Update the status back to SKIPPED for record-keeping on this page
        for (Integer sampleId : validSampleIds) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps != null) {
                    nps.setStatus(NotebookPageSample.Status.SKIPPED);
                    notebookPageSampleService.update(nps);
                }
            } catch (Exception e) {
                log.warn("Could not update status to SKIPPED for sample {}: {}", sampleId, e.getMessage());
            }
        }

        log.info("Analytical pathway skipped: pageId={}, skipped={}, advancedToTesting={}", pageId,
                validSampleIds.size(), skippedCount);

        String message = String.format(
                "Skipped analytical pathway for %d sample(s). They will proceed directly to Testing.", skippedCount);

        return AnalyticalResponse.success(0, 0, skippedCount, message);
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
                analyticalData.put("fractionationMethod", data.get("fractionationMethod"));
                analyticalData.put("fractionationMethodLabel", data.get("fractionationMethodLabel"));
                analyticalData.put("numberOfFractions", data.get("numberOfFractions"));
                analyticalData.put("fractionIds", data.get("fractionIds"));
                analyticalData.put("activeConstituentsIdentified", data.get("activeConstituentsIdentified"));
                analyticalData.put("purificationMethod", data.get("purificationMethod"));
                analyticalData.put("purificationMethodLabel", data.get("purificationMethodLabel"));
                analyticalData.put("purityLevel", data.get("purityLevel"));
                analyticalData.put("characterizationTechniques", data.get("characterizationTechniques"));
                analyticalData.put("structuralAnalysisNMR", data.get("structuralAnalysisNMR"));
                analyticalData.put("structuralAnalysisMS", data.get("structuralAnalysisMS"));
                analyticalData.put("structuralAnalysisIR", data.get("structuralAnalysisIR"));
                analyticalData.put("spectralFileReference", data.get("spectralFileReference"));
                analyticalData.put("molecularFormula", data.get("molecularFormula"));
                analyticalData.put("molecularWeight", data.get("molecularWeight"));
                analyticalData.put("analyst", data.get("analyst"));
                analyticalData.put("analysisDate", data.get("analysisDate"));
                analyticalData.put("analyticalPathwayStatus", data.get("analyticalPathwayStatus"));
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
    public List<String> validateAnalyticalRequest(AnalyticalRequest request) {
        List<String> errors = new ArrayList<>();

        // Fractionation method is required if applying analytical data
        if (request.fractionationMethod() == null || request.fractionationMethod().isBlank()) {
            errors.add("Fractionation method is required");
        } else if ("OTHER".equals(request.fractionationMethod())
                && (request.otherFractionationMethod() == null || request.otherFractionationMethod().isBlank())) {
            errors.add("Please specify the fractionation method");
        }

        // Validate purity level range if provided
        if (request.purityLevel() != null) {
            if (request.purityLevel().compareTo(BigDecimal.ZERO) < 0
                    || request.purityLevel().compareTo(new BigDecimal("100")) > 0) {
                errors.add("Purity level must be between 0 and 100%");
            }
        }

        // Validate number of fractions if provided
        if (request.numberOfFractions() != null && request.numberOfFractions() < 1) {
            errors.add("Number of fractions must be at least 1");
        }

        return errors;
    }

    @Override
    public Map<String, Object> getAnalyticalOptions() {
        Map<String, Object> options = new HashMap<>();

        List<Map<String, String>> fractionationMethods = new ArrayList<>();
        for (FractionationMethod m : FractionationMethod.values()) {
            fractionationMethods.add(Map.of("id", m.getId(), "label", m.getLabel()));
        }
        options.put("fractionationMethods", fractionationMethods);

        List<Map<String, String>> purificationMethods = new ArrayList<>();
        for (PurificationMethod m : PurificationMethod.values()) {
            purificationMethods.add(Map.of("id", m.getId(), "label", m.getLabel()));
        }
        options.put("purificationMethods", purificationMethods);

        List<Map<String, String>> characterizationTechniques = new ArrayList<>();
        for (CharacterizationTechnique t : CharacterizationTechnique.values()) {
            characterizationTechniques.add(Map.of("id", t.getId(), "label", t.getLabel()));
        }
        options.put("characterizationTechniques", characterizationTechniques);

        return options;
    }
}
