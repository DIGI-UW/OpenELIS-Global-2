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
 * Implementation of Traditional Medicine sample preparation service.
 *
 * Per SRS Requirements - Sample Preparation for Analysis: - Physical
 * Processing: Grinding, chopping, drying, or powdering - Freshly processed vs
 * samples that need drying - Yield tracking (initial weight → final weight →
 * yield percentage) - Drying-specific parameters (temperature, duration,
 * method) - Quality control checks (moisture content validation)
 *
 * When preparation is complete and QC passes, samples are marked as COMPLETED
 * and can proceed to the next workflow stage (Extraction).
 */
@Service
public class TraditionalMedicineSamplePreparationServiceImpl implements TraditionalMedicineSamplePreparationService {

    private static final Logger log = LoggerFactory.getLogger(TraditionalMedicineSamplePreparationServiceImpl.class);

    // Default moisture content threshold for traditional medicine samples
    private static final BigDecimal DEFAULT_MOISTURE_THRESHOLD = new BigDecimal("10.0");

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Override
    @Transactional
    public PreparationResponse processSamples(Integer pageId, PreparationRequest request, String sysUserId) {
        // Validate request
        List<String> validationErrors = validatePreparationRequest(request);
        if (!validationErrors.isEmpty()) {
            return PreparationResponse.error(String.join("; ", validationErrors));
        }

        if (request.sampleIds() == null || request.sampleIds().isEmpty()) {
            return PreparationResponse.error("No sample IDs provided");
        }

        int updatedCount = 0;
        int completedCount = 0;
        BigDecimal totalYield = BigDecimal.ZERO;
        int yieldCount = 0;
        String timestamp = Instant.now().toString();

        for (Integer sampleId : request.sampleIds()) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                // Get or create data map
                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                // Store processing types
                data.put("processingTypes", request.processingTypes());
                List<String> processingLabels = new ArrayList<>();
                for (String typeId : request.processingTypes()) {
                    ProcessingType type = ProcessingType.fromId(typeId);
                    if (type != null) {
                        processingLabels.add(type.getLabel());
                    }
                }
                data.put("processingTypeLabels", processingLabels);

                // Store material state
                data.put("materialState", request.materialState());
                MaterialState state = MaterialState.fromId(request.materialState());
                if (state != null) {
                    data.put("materialStateLabel", state.getLabel());
                }

                // Store basic processing info
                data.put("processingDate", request.processingDate());
                data.put("processedBy", request.processedBy());
                data.put("equipment", request.equipment());
                data.put("particleSize", request.particleSize());

                // Store weight/yield tracking
                if (request.initialWeight() != null) {
                    data.put("initialWeight", request.initialWeight().toString());
                }
                if (request.finalWeight() != null) {
                    data.put("finalWeight", request.finalWeight().toString());
                }
                data.put("weightUnit", request.weightUnit() != null ? request.weightUnit() : "g");

                // Calculate and store yield if both weights provided
                if (request.initialWeight() != null && request.finalWeight() != null) {
                    BigDecimal yield = calculateYield(request.initialWeight(), request.finalWeight());
                    if (yield != null) {
                        data.put("yieldPercentage", yield.toString());
                        totalYield = totalYield.add(yield);
                        yieldCount++;
                    }
                }

                // Store drying-specific parameters if applicable
                boolean hasDrying = request.processingTypes().stream().map(ProcessingType::fromId)
                        .anyMatch(t -> t != null && t.isDryingMethod());

                if (hasDrying) {
                    if (request.dryingTemperature() != null) {
                        data.put("dryingTemperature", request.dryingTemperature().toString());
                        data.put("temperatureUnit",
                                request.temperatureUnit() != null ? request.temperatureUnit() : "°C");
                    }
                    if (request.dryingDurationHours() != null) {
                        data.put("dryingDurationHours", request.dryingDurationHours());
                    }
                    if (request.dryingMethod() != null) {
                        data.put("dryingMethod", request.dryingMethod());
                    }
                }

                // Store QC data
                if (request.moistureContent() != null) {
                    data.put("moistureContent", request.moistureContent().toString());
                }
                if (request.targetMoistureContent() != null) {
                    data.put("targetMoistureContent", request.targetMoistureContent().toString());
                }

                // Validate moisture content if both provided
                boolean qcPassed = true;
                if (request.moistureContent() != null) {
                    BigDecimal target = request.targetMoistureContent() != null ? request.targetMoistureContent()
                            : DEFAULT_MOISTURE_THRESHOLD;
                    qcPassed = validateMoistureContent(request.moistureContent(), target);
                    data.put("moistureQCPassed", qcPassed);
                }

                // Store derived material info
                if (request.derivedMaterialId() != null && !request.derivedMaterialId().isBlank()) {
                    data.put("derivedMaterialId", request.derivedMaterialId());
                }
                if (request.aliquotNotes() != null) {
                    data.put("aliquotNotes", request.aliquotNotes());
                }

                // Store general notes
                if (request.notes() != null) {
                    data.put("preparationNotes", request.notes());
                }

                // Store audit info
                data.put("preparedAt", timestamp);
                data.put("preparedByUserId", sysUserId);

                nps.setData(data);
                nps.setSysUserId(sysUserId);

                // Update status to IN_PROGRESS (processing has started)
                if (nps.getStatus() != NotebookPageSample.Status.COMPLETED) {
                    nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                }

                notebookPageSampleService.update(nps);
                updatedCount++;

                log.info("Sample {} prepared with types: {}, yield: {}%", sampleId, request.processingTypes(),
                        data.get("yieldPercentage"));

            } catch (Exception e) {
                log.error("Error processing sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        // Calculate average yield
        BigDecimal avgYield = yieldCount > 0
                ? totalYield.divide(BigDecimal.valueOf(yieldCount), 2, RoundingMode.HALF_UP)
                : null;

        String message = String.format("Applied processing to %d sample(s).", updatedCount);
        if (avgYield != null) {
            message += String.format(" Average yield: %.1f%%", avgYield);
        }

        log.info("Preparation complete: pageId={}, updated={}, avgYield={}", pageId, updatedCount, avgYield);

        return PreparationResponse.success(updatedCount, completedCount, 0, avgYield, message);
    }

    @Override
    @Transactional
    public PreparationResponse markPreparationComplete(Integer pageId, List<Integer> sampleIds, String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return PreparationResponse.error("No sample IDs provided");
        }

        List<Integer> validSampleIds = new ArrayList<>();
        List<String> incompleteReasons = new ArrayList<>();

        // First pass: validate all samples have required data
        for (Integer sampleId : sampleIds) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                // Validate that required processing data is present
                Map<String, Object> data = nps.getData();
                if (data == null) {
                    incompleteReasons.add(String.format("Sample %d: No processing data recorded", sampleId));
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<String> processingTypes = (List<String>) data.get("processingTypes");
                if (processingTypes == null || processingTypes.isEmpty()) {
                    incompleteReasons.add(String.format("Sample %d: No processing types specified", sampleId));
                    continue;
                }

                String materialState = (String) data.get("materialState");
                if (materialState == null || materialState.isBlank()) {
                    incompleteReasons.add(String.format("Sample %d: Material state not specified", sampleId));
                    continue;
                }

                // Check QC if moisture content was measured
                Object moistureQC = data.get("moistureQCPassed");
                if (moistureQC != null && Boolean.FALSE.equals(moistureQC)) {
                    incompleteReasons.add(String.format("Sample %d: Moisture content exceeds threshold", sampleId));
                    continue;
                }

                // Add completion metadata to the data
                data.put("completedAt", Instant.now().toString());
                data.put("completedByUserId", sysUserId);
                nps.setData(data);
                nps.setSysUserId(sysUserId);
                notebookPageSampleService.update(nps);

                // Sample is valid for completion
                validSampleIds.add(sampleId);

            } catch (Exception e) {
                log.error("Error validating sample {}: {}", sampleId, e.getMessage(), e);
                incompleteReasons.add(String.format("Sample %d: Error during validation", sampleId));
            }
        }

        if (validSampleIds.isEmpty()) {
            if (!incompleteReasons.isEmpty()) {
                return PreparationResponse
                        .error("No samples could be completed. Reasons: " + String.join("; ", incompleteReasons));
            }
            return PreparationResponse.error("No valid samples found to complete");
        }

        // Use bulkUpdateStatus to mark samples as COMPLETED and auto-advance to next
        // page
        // This method handles creating NotebookPageSample records on the next page
        // (Extraction)
        int completedCount = notebookPageSampleService.bulkUpdateStatus(pageId, validSampleIds,
                NotebookPageSample.Status.COMPLETED, sysUserId);

        log.info("Preparation complete: pageId={}, validated={}, completed={}, advancedToNextPage={}", pageId,
                validSampleIds.size(), completedCount, completedCount);

        String message = String.format("%d sample(s) marked as prepared and advanced to Extraction.", completedCount);
        if (!incompleteReasons.isEmpty()) {
            message += String.format(" %d sample(s) could not be completed.", incompleteReasons.size());
        }

        return PreparationResponse.success(0, completedCount, 0, null, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Object>> getPreparationStatus(Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = new HashMap<>();

        List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);
        for (NotebookPageSample nps : samples) {
            Map<String, Object> prepData = new HashMap<>();

            if (nps.getData() != null) {
                Map<String, Object> data = nps.getData();
                // Copy all preparation-related fields
                prepData.put("processingTypes", data.get("processingTypes"));
                prepData.put("processingTypeLabels", data.get("processingTypeLabels"));
                prepData.put("materialState", data.get("materialState"));
                prepData.put("materialStateLabel", data.get("materialStateLabel"));
                prepData.put("processingDate", data.get("processingDate"));
                prepData.put("processedBy", data.get("processedBy"));
                prepData.put("equipment", data.get("equipment"));
                prepData.put("particleSize", data.get("particleSize"));
                prepData.put("initialWeight", data.get("initialWeight"));
                prepData.put("finalWeight", data.get("finalWeight"));
                prepData.put("weightUnit", data.get("weightUnit"));
                prepData.put("yieldPercentage", data.get("yieldPercentage"));
                prepData.put("dryingTemperature", data.get("dryingTemperature"));
                prepData.put("temperatureUnit", data.get("temperatureUnit"));
                prepData.put("dryingDurationHours", data.get("dryingDurationHours"));
                prepData.put("dryingMethod", data.get("dryingMethod"));
                prepData.put("moistureContent", data.get("moistureContent"));
                prepData.put("targetMoistureContent", data.get("targetMoistureContent"));
                prepData.put("moistureQCPassed", data.get("moistureQCPassed"));
                prepData.put("derivedMaterialId", data.get("derivedMaterialId"));
                prepData.put("preparationNotes", data.get("preparationNotes"));
                prepData.put("preparedAt", data.get("preparedAt"));
            }

            prepData.put("status", nps.getStatus());
            String sampleItemIdStr = nps.getSampleItemId();
            prepData.put("sampleItemId", sampleItemIdStr);

            if (sampleItemIdStr != null && !sampleItemIdStr.isBlank()) {
                try {
                    Integer sampleId = Integer.parseInt(sampleItemIdStr);
                    statusMap.put(sampleId, prepData);
                } catch (NumberFormatException e) {
                    log.warn("Invalid sampleItemId format: {}", sampleItemIdStr);
                }
            }
        }

        return statusMap;
    }

    @Override
    public BigDecimal calculateYield(BigDecimal initialWeight, BigDecimal finalWeight) {
        if (initialWeight == null || finalWeight == null) {
            return null;
        }
        if (initialWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        // Yield = (final / initial) * 100
        return finalWeight.divide(initialWeight, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2,
                RoundingMode.HALF_UP);
    }

    @Override
    public boolean validateMoistureContent(BigDecimal moistureContent, BigDecimal targetMoistureContent) {
        if (moistureContent == null) {
            return true; // If not measured, assume OK
        }
        BigDecimal target = targetMoistureContent != null ? targetMoistureContent : DEFAULT_MOISTURE_THRESHOLD;
        // Moisture content should be at or below target
        return moistureContent.compareTo(target) <= 0;
    }

    @Override
    public List<String> validatePreparationRequest(PreparationRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.processingTypes() == null || request.processingTypes().isEmpty()) {
            errors.add("At least one processing type is required");
        } else {
            for (String typeId : request.processingTypes()) {
                ProcessingType type = ProcessingType.fromId(typeId);
                if (type == null) {
                    errors.add("Invalid processing type: " + typeId);
                }
            }
        }

        if (request.materialState() == null || request.materialState().isBlank()) {
            errors.add("Material state is required");
        } else {
            MaterialState state = MaterialState.fromId(request.materialState());
            if (state == null) {
                errors.add("Invalid material state: " + request.materialState());
            }
        }

        // Validate weight values are positive if provided
        if (request.initialWeight() != null && request.initialWeight().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Initial weight must be positive");
        }
        if (request.finalWeight() != null && request.finalWeight().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Final weight must be positive");
        }

        // Validate final weight doesn't exceed initial (yield can't be > 100% for most
        // processes)
        if (request.initialWeight() != null && request.finalWeight() != null
                && request.finalWeight().compareTo(request.initialWeight()) > 0) {
            // This is a warning, not an error - some processes may add mass
            log.warn("Final weight exceeds initial weight - yield > 100%");
        }

        // Validate moisture content range
        if (request.moistureContent() != null) {
            if (request.moistureContent().compareTo(BigDecimal.ZERO) < 0
                    || request.moistureContent().compareTo(new BigDecimal("100")) > 0) {
                errors.add("Moisture content must be between 0 and 100%");
            }
        }

        // Validate drying parameters if drying method selected
        if (request.processingTypes() != null) {
            boolean hasDrying = request.processingTypes().stream().map(ProcessingType::fromId)
                    .anyMatch(t -> t != null && t.isDryingMethod());

            if (hasDrying && request.dryingTemperature() != null) {
                if (request.dryingTemperature().compareTo(BigDecimal.ZERO) < 0
                        || request.dryingTemperature().compareTo(new BigDecimal("200")) > 0) {
                    errors.add("Drying temperature should be between 0 and 200°C");
                }
            }
        }

        return errors;
    }

    @Override
    public Map<String, Object> getProcessingOptions() {
        Map<String, Object> options = new HashMap<>();

        // Processing types grouped by category
        List<Map<String, Object>> processingTypes = new ArrayList<>();
        Map<String, List<Map<String, String>>> typesByCategory = new HashMap<>();
        typesByCategory.put("physical", new ArrayList<>());
        typesByCategory.put("drying", new ArrayList<>());

        for (ProcessingType type : ProcessingType.values()) {
            Map<String, String> typeMap = Map.of("id", type.getId(), "label", type.getLabel());
            if (type.isDryingMethod()) {
                typesByCategory.get("drying").add(typeMap);
            } else {
                typesByCategory.get("physical").add(typeMap);
            }
            processingTypes
                    .add(Map.of("id", type.getId(), "label", type.getLabel(), "isDrying", type.isDryingMethod()));
        }

        options.put("processingTypes", processingTypes);
        options.put("processingTypesByCategory", typesByCategory);

        // Material states
        List<Map<String, String>> materialStates = new ArrayList<>();
        for (MaterialState state : MaterialState.values()) {
            materialStates.add(Map.of("id", state.getId(), "label", state.getLabel()));
        }
        options.put("materialStates", materialStates);

        // Weight units
        List<Map<String, String>> weightUnits = new ArrayList<>();
        for (WeightUnit unit : WeightUnit.values()) {
            weightUnits.add(Map.of("id", unit.getId(), "label", unit.getLabel()));
        }
        options.put("weightUnits", weightUnits);

        // Default moisture threshold
        options.put("defaultMoistureThreshold", DEFAULT_MOISTURE_THRESHOLD.toString());

        return options;
    }
}
