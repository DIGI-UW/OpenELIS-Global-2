package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.TraditionalMedicineSamplePreparationService;
import org.openelisglobal.notebook.service.TraditionalMedicineSamplePreparationService.PreparationRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineSamplePreparationService.PreparationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Traditional Medicine sample preparation.
 *
 * Per SRS Requirements - Sample Preparation for Analysis: - Physical
 * Processing: Grinding, chopping, drying, or powdering - Freshly processed vs
 * samples that need drying - Yield tracking (initial weight → final weight →
 * yield percentage) - Drying-specific parameters (temperature, duration,
 * method) - Quality control checks (moisture content validation)
 *
 * Endpoints: - POST /rest/notebook/tradmed/page/{pageId}/prepare - Apply
 * preparation processing to samples - POST
 * /rest/notebook/tradmed/page/{pageId}/prepare/complete - Mark samples as
 * preparation complete - GET
 * /rest/notebook/tradmed/page/{pageId}/preparation-status - Get preparation
 * status - GET /rest/notebook/tradmed/preparation/options - Get available
 * processing options
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineSamplePreparationController extends BaseRestController {

    @Autowired
    private TraditionalMedicineSamplePreparationService preparationService;

    /**
     * Apply preparation processing to selected samples on a page. POST
     * /rest/notebook/tradmed/page/{pageId}/prepare
     *
     * @param pageId      the notebook page ID (Page 3 - Sample Preparation)
     * @param request     preparation request with processing details
     * @param httpRequest for getting user session
     * @return preparation response with counts and status
     */
    @PostMapping(value = "/page/{pageId}/prepare", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody PreparationRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Convert DTO to service request
        PreparationRequest serviceRequest = new PreparationRequest(request.sampleIds, request.processingTypes,
                request.materialState, request.processingDate, request.processedBy, request.equipment,
                request.particleSize, request.initialWeight != null ? new BigDecimal(request.initialWeight) : null,
                request.finalWeight != null ? new BigDecimal(request.finalWeight) : null, request.weightUnit,
                request.dryingTemperature != null ? new BigDecimal(request.dryingTemperature) : null,
                request.temperatureUnit, request.dryingDurationHours, request.dryingMethod,
                request.moistureContent != null ? new BigDecimal(request.moistureContent) : null,
                request.targetMoistureContent != null ? new BigDecimal(request.targetMoistureContent) : null,
                request.passedQC, request.derivedMaterialId, request.createAliquot, request.aliquotNotes,
                request.notes);

        // Validate first
        List<String> validationErrors = preparationService.validatePreparationRequest(serviceRequest);
        if (!validationErrors.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", String.join("; ", validationErrors));
            error.put("validationErrors", validationErrors);
            return ResponseEntity.badRequest().body(error);
        }

        // Process preparation
        PreparationResponse response = preparationService.processSamples(pageId, serviceRequest, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("updatedCount", response.updatedCount());
        result.put("completedCount", response.completedCount());
        result.put("aliquotsCreated", response.aliquotsCreated());

        if (response.averageYieldPercentage() != null) {
            result.put("averageYieldPercentage", response.averageYieldPercentage().toString());
        }

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Mark samples as preparation complete and ready for extraction. POST
     * /rest/notebook/tradmed/page/{pageId}/prepare/complete
     *
     * @param pageId      the notebook page ID
     * @param request     completion request with sample IDs
     * @param httpRequest for getting user session
     * @return completion response with counts
     */
    @PostMapping(value = "/page/{pageId}/prepare/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markComplete(@PathVariable("pageId") Integer pageId,
            @RequestBody CompletionRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.sampleIds == null || request.sampleIds.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        PreparationResponse response = preparationService.markPreparationComplete(pageId, request.sampleIds, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("completedCount", response.completedCount());

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get preparation status for all samples on a page. GET
     * /rest/notebook/tradmed/page/{pageId}/preparation-status
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to preparation data
     */
    @GetMapping(value = "/page/{pageId}/preparation-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPreparationStatus(@PathVariable("pageId") Integer pageId) {

        Map<Integer, Map<String, Object>> statusMap = preparationService.getPreparationStatus(pageId);

        // Calculate summary counts
        int totalCount = statusMap.size();
        int processedCount = 0;
        int completedCount = 0;
        int pendingCount = 0;
        int qcFailedCount = 0;

        for (Map<String, Object> prepData : statusMap.values()) {
            @SuppressWarnings("unchecked")
            List<String> processingTypes = (List<String>) prepData.get("processingTypes");
            String status = (String) prepData.get("status");
            Boolean qcPassed = (Boolean) prepData.get("moistureQCPassed");

            if ("COMPLETED".equals(status)) {
                completedCount++;
            } else if (processingTypes != null && !processingTypes.isEmpty()) {
                processedCount++;
            } else {
                pendingCount++;
            }

            if (qcPassed != null && !qcPassed) {
                qcFailedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("samples", statusMap);
        result.put("summary", Map.of("total", totalCount, "processed", processedCount, "completed", completedCount,
                "pending", pendingCount, "qcFailed", qcFailedCount));

        return ResponseEntity.ok(result);
    }

    /**
     * Get available processing options. GET
     * /rest/notebook/tradmed/preparation/options
     *
     * @return available processing options
     */
    @GetMapping(value = "/preparation/options", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProcessingOptions() {
        Map<String, Object> options = preparationService.getProcessingOptions();
        return ResponseEntity.ok(options);
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }

    /**
     * DTO for preparation request from frontend.
     */
    public static class PreparationRequestDTO {
        private List<Integer> sampleIds;
        private List<String> processingTypes;
        private String materialState;
        private String processingDate;
        private String processedBy;
        private String equipment;
        private String particleSize;
        // Weight tracking
        private String initialWeight;
        private String finalWeight;
        private String weightUnit;
        // Drying parameters
        private String dryingTemperature;
        private String temperatureUnit;
        private Integer dryingDurationHours;
        private String dryingMethod;
        // QC
        private String moistureContent;
        private String targetMoistureContent;
        private Boolean passedQC;
        // Derived material
        private String derivedMaterialId;
        private Boolean createAliquot;
        private String aliquotNotes;
        // Notes
        private String notes;

        // Getters and setters
        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public List<String> getProcessingTypes() {
            return processingTypes;
        }

        public void setProcessingTypes(List<String> processingTypes) {
            this.processingTypes = processingTypes;
        }

        public String getMaterialState() {
            return materialState;
        }

        public void setMaterialState(String materialState) {
            this.materialState = materialState;
        }

        public String getProcessingDate() {
            return processingDate;
        }

        public void setProcessingDate(String processingDate) {
            this.processingDate = processingDate;
        }

        public String getProcessedBy() {
            return processedBy;
        }

        public void setProcessedBy(String processedBy) {
            this.processedBy = processedBy;
        }

        public String getEquipment() {
            return equipment;
        }

        public void setEquipment(String equipment) {
            this.equipment = equipment;
        }

        public String getParticleSize() {
            return particleSize;
        }

        public void setParticleSize(String particleSize) {
            this.particleSize = particleSize;
        }

        public String getInitialWeight() {
            return initialWeight;
        }

        public void setInitialWeight(String initialWeight) {
            this.initialWeight = initialWeight;
        }

        public String getFinalWeight() {
            return finalWeight;
        }

        public void setFinalWeight(String finalWeight) {
            this.finalWeight = finalWeight;
        }

        public String getWeightUnit() {
            return weightUnit;
        }

        public void setWeightUnit(String weightUnit) {
            this.weightUnit = weightUnit;
        }

        public String getDryingTemperature() {
            return dryingTemperature;
        }

        public void setDryingTemperature(String dryingTemperature) {
            this.dryingTemperature = dryingTemperature;
        }

        public String getTemperatureUnit() {
            return temperatureUnit;
        }

        public void setTemperatureUnit(String temperatureUnit) {
            this.temperatureUnit = temperatureUnit;
        }

        public Integer getDryingDurationHours() {
            return dryingDurationHours;
        }

        public void setDryingDurationHours(Integer dryingDurationHours) {
            this.dryingDurationHours = dryingDurationHours;
        }

        public String getDryingMethod() {
            return dryingMethod;
        }

        public void setDryingMethod(String dryingMethod) {
            this.dryingMethod = dryingMethod;
        }

        public String getMoistureContent() {
            return moistureContent;
        }

        public void setMoistureContent(String moistureContent) {
            this.moistureContent = moistureContent;
        }

        public String getTargetMoistureContent() {
            return targetMoistureContent;
        }

        public void setTargetMoistureContent(String targetMoistureContent) {
            this.targetMoistureContent = targetMoistureContent;
        }

        public Boolean getPassedQC() {
            return passedQC;
        }

        public void setPassedQC(Boolean passedQC) {
            this.passedQC = passedQC;
        }

        public String getDerivedMaterialId() {
            return derivedMaterialId;
        }

        public void setDerivedMaterialId(String derivedMaterialId) {
            this.derivedMaterialId = derivedMaterialId;
        }

        public Boolean getCreateAliquot() {
            return createAliquot;
        }

        public void setCreateAliquot(Boolean createAliquot) {
            this.createAliquot = createAliquot;
        }

        public String getAliquotNotes() {
            return aliquotNotes;
        }

        public void setAliquotNotes(String aliquotNotes) {
            this.aliquotNotes = aliquotNotes;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * DTO for completion request.
     */
    public static class CompletionRequestDTO {
        private List<Integer> sampleIds;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }
    }
}
