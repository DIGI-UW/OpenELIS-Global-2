package org.openelisglobal.notebook.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * Implementation of Traditional Medicine extraction service.
 *
 * Per SRS Requirements - Extraction, Filtration & Concentration: - Extraction
 * Process: Use of solvents based on target compounds - Techniques: maceration,
 * Soxhlet, ultrasonic, distillation, etc. - Filtration: Remove plant debris and
 * impurities - Concentration: Evaporation or distillation to reduce volume and
 * enrich extract
 *
 * When extraction is complete, samples are marked as COMPLETED and can proceed
 * to the next workflow stage (Analytical Pathway or Testing).
 */
@Service
public class TraditionalMedicineExtractionServiceImpl implements TraditionalMedicineExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TraditionalMedicineExtractionServiceImpl.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Override
    @Transactional
    public ExtractionResponse extractSamples(Integer pageId, ExtractionRequest request, String sysUserId) {
        List<String> validationErrors = validateExtractionRequest(request);
        if (!validationErrors.isEmpty()) {
            return ExtractionResponse.error(String.join("; ", validationErrors));
        }

        if (request.sampleIds() == null || request.sampleIds().isEmpty()) {
            return ExtractionResponse.error("No sample IDs provided");
        }

        int updatedCount = 0;
        BigDecimal totalYield = BigDecimal.ZERO;
        int yieldCount = 0;
        String timestamp = Instant.now().toString();

        // Get labels for solvent and technique
        Solvent solvent = Solvent.fromId(request.solvent());
        ExtractionTechnique technique = ExtractionTechnique.fromId(request.extractionTechnique());
        FiltrationMethod filtration = request.filtrationMethod() != null
                ? FiltrationMethod.fromId(request.filtrationMethod())
                : null;
        ConcentrationMethod concentration = request.concentrationMethod() != null
                ? ConcentrationMethod.fromId(request.concentrationMethod())
                : null;

        for (Integer sampleId : request.sampleIds()) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                // Store extraction parameters
                data.put("solvent", request.solvent());
                data.put("solventLabel", solvent != null ? solvent.getLabel() : request.solvent());
                if ("OTHER".equals(request.solvent()) && request.otherSolvent() != null) {
                    data.put("otherSolvent", request.otherSolvent());
                    data.put("solventLabel", request.otherSolvent());
                }
                if (request.solventConcentration() != null) {
                    data.put("solventConcentration", request.solventConcentration());
                }

                data.put("extractionTechnique", request.extractionTechnique());
                data.put("techniqueLabel", technique != null ? technique.getLabel() : request.extractionTechnique());
                if ("OTHER".equals(request.extractionTechnique()) && request.otherTechnique() != null) {
                    data.put("otherTechnique", request.otherTechnique());
                    data.put("techniqueLabel", request.otherTechnique());
                }

                data.put("extractionDate", request.extractionDate());
                data.put("operator", request.operator());

                // Store solvent ratio tracking
                if (request.materialWeight() != null) {
                    data.put("materialWeight", request.materialWeight().toString());
                    data.put("materialWeightUnit",
                            request.materialWeightUnit() != null ? request.materialWeightUnit() : "g");
                }
                if (request.solventVolume() != null) {
                    data.put("solventVolume", request.solventVolume().toString());
                    data.put("solventVolumeUnit",
                            request.solventVolumeUnit() != null ? request.solventVolumeUnit() : "mL");
                }

                // Calculate and store solvent ratio
                if (request.materialWeight() != null && request.solventVolume() != null) {
                    String ratio = calculateSolventRatio(request.materialWeight(), request.solventVolume());
                    if (ratio != null) {
                        data.put("solventRatio", ratio);
                    }
                }

                // Store extraction conditions
                if (request.extractionTemperature() != null) {
                    data.put("extractionTemperature", request.extractionTemperature().toString());
                    data.put("temperatureUnit", request.temperatureUnit() != null ? request.temperatureUnit() : "°C");
                }
                if (request.extractionDurationMinutes() != null) {
                    data.put("extractionDurationMinutes", request.extractionDurationMinutes());
                    // Also store formatted duration
                    int hours = request.extractionDurationMinutes() / 60;
                    int mins = request.extractionDurationMinutes() % 60;
                    String formatted = hours > 0 ? hours + "h " + mins + "m" : mins + "m";
                    data.put("extractionDurationFormatted", formatted);
                }
                if (request.numberOfCycles() != null) {
                    data.put("numberOfCycles", request.numberOfCycles());
                }

                // Store filtration data
                if (request.filtrationMethod() != null) {
                    data.put("filtrationMethod", request.filtrationMethod());
                    data.put("filtrationLabel",
                            filtration != null ? filtration.getLabel() : request.filtrationMethod());
                }
                if (request.filterPoreSize() != null) {
                    data.put("filterPoreSize", request.filterPoreSize());
                }
                if (request.debrisRemoved() != null) {
                    data.put("debrisRemoved", request.debrisRemoved());
                }

                // Store concentration data
                if (request.concentrationMethod() != null) {
                    data.put("concentrationMethod", request.concentrationMethod());
                    data.put("concentrationLabel",
                            concentration != null ? concentration.getLabel() : request.concentrationMethod());
                }
                if (request.concentrationTemperature() != null) {
                    data.put("concentrationTemperature", request.concentrationTemperature().toString());
                }
                if (request.finalVolume() != null) {
                    data.put("finalVolume", request.finalVolume().toString());
                    data.put("finalVolumeUnit", request.finalVolumeUnit() != null ? request.finalVolumeUnit() : "mL");
                }

                // Store extract output data
                if (request.extractId() != null && !request.extractId().isBlank()) {
                    data.put("extractId", request.extractId());
                }
                if (request.extractWeight() != null) {
                    data.put("extractWeight", request.extractWeight().toString());
                    data.put("extractWeightUnit",
                            request.extractWeightUnit() != null ? request.extractWeightUnit() : "g");

                    // Calculate yield if material weight is available
                    if (request.materialWeight() != null) {
                        BigDecimal yield = calculateYield(request.materialWeight(), request.extractWeight());
                        if (yield != null) {
                            data.put("extractYieldPercentage", yield.toString());
                            totalYield = totalYield.add(yield);
                            yieldCount++;
                        }
                    }
                }
                if (request.extractAppearance() != null) {
                    data.put("extractAppearance", request.extractAppearance());
                }
                if (request.extractColor() != null) {
                    data.put("extractColor", request.extractColor());
                }

                // Store notes
                if (request.notes() != null) {
                    data.put("extractionNotes", request.notes());
                }

                // Store audit info
                data.put("extractedAt", timestamp);
                data.put("extractedByUserId", sysUserId);

                nps.setData(data);
                nps.setSysUserId(sysUserId);

                if (nps.getStatus() != NotebookPageSample.Status.COMPLETED) {
                    nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                }

                notebookPageSampleService.update(nps);
                updatedCount++;

                log.info("Sample {} extracted: solvent={}, technique={}, yield={}%", sampleId, request.solvent(),
                        request.extractionTechnique(), data.get("extractYieldPercentage"));

            } catch (Exception e) {
                log.error("Error extracting sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        BigDecimal avgYield = yieldCount > 0
                ? totalYield.divide(BigDecimal.valueOf(yieldCount), 2, RoundingMode.HALF_UP)
                : null;

        String message = String.format("Applied extraction to %d sample(s).", updatedCount);
        if (avgYield != null) {
            message += String.format(" Average yield: %.1f%%", avgYield);
        }

        log.info("Extraction complete: pageId={}, updated={}, avgYield={}", pageId, updatedCount, avgYield);

        return ExtractionResponse.success(updatedCount, 0, avgYield, message);
    }

    @Override
    @Transactional
    public ExtractionResponse markExtractionComplete(Integer pageId, List<Integer> sampleIds, String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return ExtractionResponse.error("No sample IDs provided");
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
                    incompleteReasons.add(String.format("Sample %d: No extraction data recorded", sampleId));
                    continue;
                }

                // Validate required extraction data
                String solvent = (String) data.get("solvent");
                if (solvent == null || solvent.isBlank()) {
                    incompleteReasons.add(String.format("Sample %d: No solvent specified", sampleId));
                    continue;
                }

                String technique = (String) data.get("extractionTechnique");
                if (technique == null || technique.isBlank()) {
                    incompleteReasons.add(String.format("Sample %d: No extraction technique specified", sampleId));
                    continue;
                }

                // Add completion metadata
                data.put("completedAt", Instant.now().toString());
                data.put("completedByUserId", sysUserId);
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
                return ExtractionResponse
                        .error("No samples could be completed. Reasons: " + String.join("; ", incompleteReasons));
            }
            return ExtractionResponse.error("No valid samples found to complete");
        }

        // Use bulkUpdateStatus to mark as COMPLETED and advance to next page
        int completedCount = notebookPageSampleService.bulkUpdateStatus(pageId, validSampleIds,
                NotebookPageSample.Status.COMPLETED, sysUserId);

        log.info("Extraction complete: pageId={}, validated={}, completed={}", pageId, validSampleIds.size(),
                completedCount);

        String message = String.format("%d sample(s) marked as extracted and advanced to next step.", completedCount);
        if (!incompleteReasons.isEmpty()) {
            message += String.format(" %d sample(s) could not be completed.", incompleteReasons.size());
        }

        return ExtractionResponse.success(0, completedCount, null, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Object>> getExtractionStatus(Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = new HashMap<>();

        List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);
        for (NotebookPageSample nps : samples) {
            Map<String, Object> extData = new HashMap<>();

            if (nps.getData() != null) {
                Map<String, Object> data = nps.getData();
                extData.put("solvent", data.get("solvent"));
                extData.put("solventLabel", data.get("solventLabel"));
                extData.put("solventConcentration", data.get("solventConcentration"));
                extData.put("extractionTechnique", data.get("extractionTechnique"));
                extData.put("techniqueLabel", data.get("techniqueLabel"));
                extData.put("extractionDate", data.get("extractionDate"));
                extData.put("operator", data.get("operator"));
                extData.put("materialWeight", data.get("materialWeight"));
                extData.put("solventVolume", data.get("solventVolume"));
                extData.put("solventRatio", data.get("solventRatio"));
                extData.put("extractionTemperature", data.get("extractionTemperature"));
                extData.put("extractionDurationMinutes", data.get("extractionDurationMinutes"));
                extData.put("extractionDurationFormatted", data.get("extractionDurationFormatted"));
                extData.put("numberOfCycles", data.get("numberOfCycles"));
                extData.put("filtrationMethod", data.get("filtrationMethod"));
                extData.put("filtrationLabel", data.get("filtrationLabel"));
                extData.put("debrisRemoved", data.get("debrisRemoved"));
                extData.put("concentrationMethod", data.get("concentrationMethod"));
                extData.put("concentrationLabel", data.get("concentrationLabel"));
                extData.put("finalVolume", data.get("finalVolume"));
                extData.put("finalVolumeUnit", data.get("finalVolumeUnit"));
                extData.put("extractId", data.get("extractId"));
                extData.put("extractWeight", data.get("extractWeight"));
                extData.put("extractYieldPercentage", data.get("extractYieldPercentage"));
                extData.put("extractAppearance", data.get("extractAppearance"));
                extData.put("extractColor", data.get("extractColor"));
                extData.put("extractedAt", data.get("extractedAt"));
            }

            extData.put("status", nps.getStatus());
            String sampleItemIdStr = nps.getSampleItemId();
            extData.put("sampleItemId", sampleItemIdStr);

            if (sampleItemIdStr != null && !sampleItemIdStr.isBlank()) {
                try {
                    Integer sampleId = Integer.parseInt(sampleItemIdStr);
                    statusMap.put(sampleId, extData);
                } catch (NumberFormatException e) {
                    log.warn("Invalid sampleItemId format: {}", sampleItemIdStr);
                }
            }
        }

        return statusMap;
    }

    @Override
    public BigDecimal calculateYield(BigDecimal materialWeight, BigDecimal extractWeight) {
        if (materialWeight == null || extractWeight == null) {
            return null;
        }
        if (materialWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        // Yield = (extract / material) × 100
        return extractWeight.divide(materialWeight, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2,
                RoundingMode.HALF_UP);
    }

    @Override
    public String calculateSolventRatio(BigDecimal materialWeight, BigDecimal solventVolume) {
        if (materialWeight == null || solventVolume == null) {
            return null;
        }
        if (materialWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        // Calculate ratio as 1:X where X = solvent/material
        BigDecimal ratio = solventVolume.divide(materialWeight, 1, RoundingMode.HALF_UP);
        return "1:" + ratio.intValue();
    }

    @Override
    public List<String> validateExtractionRequest(ExtractionRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.solvent() == null || request.solvent().isBlank()) {
            errors.add("Solvent is required");
        } else if ("OTHER".equals(request.solvent())
                && (request.otherSolvent() == null || request.otherSolvent().isBlank())) {
            errors.add("Please specify the solvent name");
        }

        if (request.extractionTechnique() == null || request.extractionTechnique().isBlank()) {
            errors.add("Extraction technique is required");
        } else if ("OTHER".equals(request.extractionTechnique())
                && (request.otherTechnique() == null || request.otherTechnique().isBlank())) {
            errors.add("Please specify the extraction technique");
        }

        // Validate weights/volumes are positive if provided
        if (request.materialWeight() != null && request.materialWeight().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Material weight must be positive");
        }
        if (request.solventVolume() != null && request.solventVolume().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Solvent volume must be positive");
        }
        if (request.extractWeight() != null && request.extractWeight().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Extract weight cannot be negative");
        }

        // Validate temperature range
        if (request.extractionTemperature() != null) {
            if (request.extractionTemperature().compareTo(new BigDecimal("-50")) < 0
                    || request.extractionTemperature().compareTo(new BigDecimal("300")) > 0) {
                errors.add("Extraction temperature should be between -50 and 300°C");
            }
        }

        return errors;
    }

    @Override
    public Map<String, Object> getExtractionOptions() {
        Map<String, Object> options = new HashMap<>();

        List<Map<String, String>> solvents = new ArrayList<>();
        for (Solvent s : Solvent.values()) {
            solvents.add(Map.of("id", s.getId(), "label", s.getLabel()));
        }
        options.put("solvents", solvents);

        List<Map<String, String>> techniques = new ArrayList<>();
        for (ExtractionTechnique t : ExtractionTechnique.values()) {
            techniques.add(Map.of("id", t.getId(), "label", t.getLabel()));
        }
        options.put("techniques", techniques);

        List<Map<String, String>> filtrationMethods = new ArrayList<>();
        for (FiltrationMethod m : FiltrationMethod.values()) {
            filtrationMethods.add(Map.of("id", m.getId(), "label", m.getLabel()));
        }
        options.put("filtrationMethods", filtrationMethods);

        List<Map<String, String>> concentrationMethods = new ArrayList<>();
        for (ConcentrationMethod m : ConcentrationMethod.values()) {
            concentrationMethods.add(Map.of("id", m.getId(), "label", m.getLabel()));
        }
        options.put("concentrationMethods", concentrationMethods);

        return options;
    }
}
