package org.openelisglobal.notebook.service;

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
 * Implementation of Traditional Medicine Testing service.
 *
 * Per SRS Requirements - Product Development and Testing: - Preliminary
 * Phytochemical Screening: Test for alkaloids, flavonoids, etc. -
 * Safety/Toxicity Study: In vitro or animal models - Efficacy Test: Biological
 * activity assays
 *
 * After testing, approved samples proceed to Formulation.
 */
@Service
public class TraditionalMedicineTestingServiceImpl implements TraditionalMedicineTestingService {

    private static final Logger log = LoggerFactory.getLogger(TraditionalMedicineTestingServiceImpl.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Override
    @Transactional
    public TestingResponse applyTestingData(Integer pageId, TestingRequest request, String sysUserId) {
        List<String> validationErrors = validateTestingRequest(request);
        if (!validationErrors.isEmpty()) {
            return TestingResponse.error(String.join("; ", validationErrors));
        }

        if (request.sampleIds() == null || request.sampleIds().isEmpty()) {
            return TestingResponse.error("No sample IDs provided");
        }

        int updatedCount = 0;
        String timestamp = Instant.now().toString();

        // Get labels for enums
        SafetyStudyType safetyType = request.safetyStudyType() != null
                ? SafetyStudyType.fromId(request.safetyStudyType())
                : null;
        ToxicityModel toxModel = request.toxicityModel() != null ? ToxicityModel.fromId(request.toxicityModel()) : null;
        ToxicityOutcome toxOutcome = request.toxicityOutcome() != null
                ? ToxicityOutcome.fromId(request.toxicityOutcome())
                : null;
        BiologicalAssayType assayType = request.biologicalAssayType() != null
                ? BiologicalAssayType.fromId(request.biologicalAssayType())
                : null;
        EfficacyOutcome effOutcome = request.efficacyOutcome() != null
                ? EfficacyOutcome.fromId(request.efficacyOutcome())
                : null;

        for (Integer sampleId : request.sampleIds()) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                // Store phytochemical screening data
                if (request.detectedPhytochemicals() != null && !request.detectedPhytochemicals().isEmpty()) {
                    data.put("detectedPhytochemicals", request.detectedPhytochemicals());
                    // Build labels
                    List<String> phytoLabels = new ArrayList<>();
                    for (String phytoId : request.detectedPhytochemicals()) {
                        PhytochemicalType phyto = PhytochemicalType.fromId(phytoId);
                        phytoLabels.add(phyto != null ? phyto.getLabel() : phytoId);
                    }
                    data.put("detectedPhytochemicalLabels", phytoLabels);
                    data.put("testsPerformed", String.join(", ", phytoLabels));
                    // Store individual flags for backwards compatibility
                    data.put("phytochemicalAlkaloids", request.detectedPhytochemicals().contains("ALKALOIDS"));
                    data.put("phytochemicalFlavonoids", request.detectedPhytochemicals().contains("FLAVONOIDS"));
                    data.put("phytochemicalTannins", request.detectedPhytochemicals().contains("TANNINS"));
                    data.put("phytochemicalSaponins", request.detectedPhytochemicals().contains("SAPONINS"));
                    data.put("phytochemicalGlycosides", request.detectedPhytochemicals().contains("GLYCOSIDES"));
                    data.put("phytochemicalTerpenoids", request.detectedPhytochemicals().contains("TERPENOIDS"));
                    data.put("phytochemicalSteroids", request.detectedPhytochemicals().contains("STEROIDS"));
                    data.put("phytochemicalPhenols", request.detectedPhytochemicals().contains("PHENOLS"));
                }
                if (request.phytochemicalScreeningNotes() != null) {
                    data.put("phytochemicalScreeningNotes", request.phytochemicalScreeningNotes());
                }

                // Store safety/toxicity data
                if (request.safetyStudyType() != null) {
                    data.put("safetyStudyType", request.safetyStudyType());
                    data.put("safetyStudyTypeLabel",
                            safetyType != null ? safetyType.getLabel() : request.safetyStudyType());
                }
                if (request.toxicityModel() != null) {
                    data.put("toxicityModel", request.toxicityModel());
                    data.put("toxicityModelLabel", toxModel != null ? toxModel.getLabel() : request.toxicityModel());
                }
                if (request.toxicityOutcome() != null) {
                    data.put("toxicityOutcome", request.toxicityOutcome());
                    data.put("toxicityOutcomeLabel",
                            toxOutcome != null ? toxOutcome.getLabel() : request.toxicityOutcome());
                }
                if (request.ld50Value() != null) {
                    data.put("ld50Value", request.ld50Value());
                }
                if (request.safetyStudyReference() != null) {
                    data.put("safetyStudyReference", request.safetyStudyReference());
                }

                // Store efficacy data
                if (request.biologicalAssayType() != null) {
                    data.put("biologicalAssayType", request.biologicalAssayType());
                    data.put("biologicalAssayTypeLabel",
                            assayType != null ? assayType.getLabel() : request.biologicalAssayType());
                    if ("OTHER".equals(request.biologicalAssayType()) && request.otherAssayType() != null) {
                        data.put("otherAssayType", request.otherAssayType());
                        data.put("biologicalAssayTypeLabel", request.otherAssayType());
                    }
                }
                if (request.efficacyOutcome() != null) {
                    data.put("efficacyOutcome", request.efficacyOutcome());
                    data.put("efficacyOutcomeLabel",
                            effOutcome != null ? effOutcome.getLabel() : request.efficacyOutcome());
                }
                if (request.assayProtocol() != null) {
                    data.put("assayProtocol", request.assayProtocol());
                }
                if (request.ic50Value() != null) {
                    data.put("ic50Value", request.ic50Value());
                }
                if (request.efficacyStudyReference() != null) {
                    data.put("efficacyStudyReference", request.efficacyStudyReference());
                }

                // Store general info
                if (request.testedBy() != null) {
                    data.put("testedBy", request.testedBy());
                }
                if (request.testDate() != null) {
                    data.put("testDate", request.testDate());
                }
                if (request.notes() != null) {
                    data.put("testingNotes", request.notes());
                }

                // Store audit info
                data.put("testingDataAppliedAt", timestamp);
                data.put("testingDataAppliedBy", sysUserId);

                nps.setData(data);
                nps.setSysUserId(sysUserId);

                if (nps.getStatus() != NotebookPageSample.Status.COMPLETED
                        && nps.getStatus() != NotebookPageSample.Status.SKIPPED) {
                    nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                }

                notebookPageSampleService.update(nps);
                updatedCount++;

                log.info("Testing data applied to sample {}: toxicity={}, efficacy={}", sampleId,
                        request.toxicityOutcome(), request.efficacyOutcome());

            } catch (Exception e) {
                log.error("Error applying testing data to sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        String message = String.format("Applied testing data to %d sample(s).", updatedCount);
        log.info("Testing data applied: pageId={}, updated={}", pageId, updatedCount);

        return TestingResponse.success(updatedCount, 0, 0, message);
    }

    @Override
    @Transactional
    public TestingResponse approveSamplesForFormulation(Integer pageId, List<Integer> sampleIds, String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return TestingResponse.error("No sample IDs provided");
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
                    incompleteReasons.add(String.format("Sample %d: No testing data recorded", sampleId));
                    continue;
                }

                // Validate testing results exist
                String toxicityOutcome = (String) data.get("toxicityOutcome");
                String efficacyOutcome = (String) data.get("efficacyOutcome");

                if ((toxicityOutcome == null || toxicityOutcome.isBlank())
                        && (efficacyOutcome == null || efficacyOutcome.isBlank())) {
                    incompleteReasons.add(String.format("Sample %d: No toxicity or efficacy results", sampleId));
                    continue;
                }

                // Check for high toxicity - warn but allow approval
                if ("HIGH_TOXICITY".equals(toxicityOutcome)) {
                    log.warn("Sample {} has HIGH_TOXICITY but is being approved for formulation", sampleId);
                }

                // Add approval metadata
                data.put("approvedForFormulationAt", Instant.now().toString());
                data.put("approvedForFormulationBy", sysUserId);
                data.put("testingStatus", "APPROVED");
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
                return TestingResponse
                        .error("No samples could be approved. Reasons: " + String.join("; ", incompleteReasons));
            }
            return TestingResponse.error("No valid samples found to approve");
        }

        // Use bulkUpdateStatus to mark as COMPLETED and advance to next page
        // (Formulation)
        int approvedCount = notebookPageSampleService.bulkUpdateStatus(pageId, validSampleIds,
                NotebookPageSample.Status.COMPLETED, sysUserId);

        log.info("Samples approved for formulation: pageId={}, validated={}, approved={}", pageId,
                validSampleIds.size(), approvedCount);

        String message = String.format("%d sample(s) approved and advanced to Formulation.", approvedCount);
        if (!incompleteReasons.isEmpty()) {
            message += String.format(" %d sample(s) could not be approved.", incompleteReasons.size());
        }

        return TestingResponse.success(0, approvedCount, 0, message);
    }

    @Override
    @Transactional
    public TestingResponse rejectSamples(Integer pageId, List<Integer> sampleIds, String rejectReason,
            String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return TestingResponse.error("No sample IDs provided");
        }

        int rejectedCount = 0;

        for (Integer sampleId : sampleIds) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();
                data.put("rejectedAt", Instant.now().toString());
                data.put("rejectedBy", sysUserId);
                data.put("rejectReason", rejectReason != null ? rejectReason : "Failed safety/efficacy testing");
                data.put("testingStatus", "REJECTED");
                nps.setData(data);
                nps.setStatus(NotebookPageSample.Status.SKIPPED); // SKIPPED is used for rejected samples that won't
                                                                  // proceed
                nps.setSysUserId(sysUserId);
                notebookPageSampleService.update(nps);
                rejectedCount++;

                log.info("Sample {} rejected: reason={}", sampleId, rejectReason);

            } catch (Exception e) {
                log.error("Error rejecting sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        String message = String.format("Rejected %d sample(s) - failed safety/efficacy testing.", rejectedCount);
        log.info("Samples rejected: pageId={}, rejected={}", pageId, rejectedCount);

        return TestingResponse.success(0, 0, rejectedCount, message);
    }

    @Override
    @Transactional
    public TestingResponse markForFurtherTesting(Integer pageId, List<Integer> sampleIds, String sysUserId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return TestingResponse.error("No sample IDs provided");
        }

        int updatedCount = 0;

        for (Integer sampleId : sampleIds) {
            try {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps == null) {
                    log.warn("NotebookPageSample not found for pageId={}, sampleId={}", pageId, sampleId);
                    continue;
                }

                Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();
                data.put("markedForFurtherTestingAt", Instant.now().toString());
                data.put("markedForFurtherTestingBy", sysUserId);
                data.put("testingStatus", "FURTHER_TESTING_REQUIRED");
                nps.setData(data);
                nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                nps.setSysUserId(sysUserId);
                notebookPageSampleService.update(nps);
                updatedCount++;

                log.info("Sample {} marked for further testing", sampleId);

            } catch (Exception e) {
                log.error("Error marking sample {} for further testing: {}", sampleId, e.getMessage(), e);
            }
        }

        String message = String.format("Marked %d sample(s) for further testing.", updatedCount);
        log.info("Samples marked for further testing: pageId={}, count={}", pageId, updatedCount);

        return TestingResponse.success(updatedCount, 0, 0, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Object>> getTestingStatus(Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = new HashMap<>();

        List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);
        for (NotebookPageSample nps : samples) {
            Map<String, Object> testingData = new HashMap<>();

            if (nps.getData() != null) {
                Map<String, Object> data = nps.getData();
                // Copy all testing-related fields
                testingData.put("detectedPhytochemicals", data.get("detectedPhytochemicals"));
                testingData.put("detectedPhytochemicalLabels", data.get("detectedPhytochemicalLabels"));
                testingData.put("testsPerformed", data.get("testsPerformed"));
                testingData.put("safetyStudyType", data.get("safetyStudyType"));
                testingData.put("safetyStudyTypeLabel", data.get("safetyStudyTypeLabel"));
                testingData.put("toxicityModel", data.get("toxicityModel"));
                testingData.put("toxicityModelLabel", data.get("toxicityModelLabel"));
                testingData.put("toxicityOutcome", data.get("toxicityOutcome"));
                testingData.put("toxicityOutcomeLabel", data.get("toxicityOutcomeLabel"));
                testingData.put("ld50Value", data.get("ld50Value"));
                testingData.put("biologicalAssayType", data.get("biologicalAssayType"));
                testingData.put("biologicalAssayTypeLabel", data.get("biologicalAssayTypeLabel"));
                testingData.put("efficacyOutcome", data.get("efficacyOutcome"));
                testingData.put("efficacyOutcomeLabel", data.get("efficacyOutcomeLabel"));
                testingData.put("ic50Value", data.get("ic50Value"));
                testingData.put("testedBy", data.get("testedBy"));
                testingData.put("testDate", data.get("testDate"));
                testingData.put("testingStatus", data.get("testingStatus"));
                testingData.put("rejectReason", data.get("rejectReason"));
            }

            testingData.put("status", nps.getStatus());
            String sampleItemIdStr = nps.getSampleItemId();
            testingData.put("sampleItemId", sampleItemIdStr);

            if (sampleItemIdStr != null && !sampleItemIdStr.isBlank()) {
                try {
                    Integer sampleId = Integer.parseInt(sampleItemIdStr);
                    statusMap.put(sampleId, testingData);
                } catch (NumberFormatException e) {
                    log.warn("Invalid sampleItemId format: {}", sampleItemIdStr);
                }
            }
        }

        return statusMap;
    }

    @Override
    public List<String> validateTestingRequest(TestingRequest request) {
        List<String> errors = new ArrayList<>();

        // At least one type of test data should be provided
        boolean hasPhytochemical = request.detectedPhytochemicals() != null
                && !request.detectedPhytochemicals().isEmpty();
        boolean hasSafety = request.safetyStudyType() != null && !request.safetyStudyType().isBlank();
        boolean hasEfficacy = request.biologicalAssayType() != null && !request.biologicalAssayType().isBlank();

        if (!hasPhytochemical && !hasSafety && !hasEfficacy) {
            errors.add(
                    "At least one type of testing data must be provided (phytochemical screening, safety study, or efficacy test)");
        }

        return errors;
    }

    @Override
    public Map<String, Object> getTestingOptions() {
        Map<String, Object> options = new HashMap<>();

        List<Map<String, String>> phytochemicalTypes = new ArrayList<>();
        for (PhytochemicalType t : PhytochemicalType.values()) {
            phytochemicalTypes.add(Map.of("id", t.getId(), "label", t.getLabel()));
        }
        options.put("phytochemicalTypes", phytochemicalTypes);

        List<Map<String, String>> safetyStudyTypes = new ArrayList<>();
        for (SafetyStudyType t : SafetyStudyType.values()) {
            safetyStudyTypes.add(Map.of("id", t.getId(), "label", t.getLabel()));
        }
        options.put("safetyStudyTypes", safetyStudyTypes);

        List<Map<String, String>> toxicityModels = new ArrayList<>();
        for (ToxicityModel m : ToxicityModel.values()) {
            toxicityModels.add(Map.of("id", m.getId(), "label", m.getLabel()));
        }
        options.put("toxicityModels", toxicityModels);

        List<Map<String, String>> toxicityOutcomes = new ArrayList<>();
        for (ToxicityOutcome o : ToxicityOutcome.values()) {
            toxicityOutcomes.add(Map.of("id", o.getId(), "label", o.getLabel()));
        }
        options.put("toxicityOutcomes", toxicityOutcomes);

        List<Map<String, String>> biologicalAssayTypes = new ArrayList<>();
        for (BiologicalAssayType t : BiologicalAssayType.values()) {
            biologicalAssayTypes.add(Map.of("id", t.getId(), "label", t.getLabel()));
        }
        options.put("biologicalAssayTypes", biologicalAssayTypes);

        List<Map<String, String>> efficacyOutcomes = new ArrayList<>();
        for (EfficacyOutcome o : EfficacyOutcome.values()) {
            efficacyOutcomes.add(Map.of("id", o.getId(), "label", o.getLabel()));
        }
        options.put("efficacyOutcomes", efficacyOutcomes);

        return options;
    }
}
