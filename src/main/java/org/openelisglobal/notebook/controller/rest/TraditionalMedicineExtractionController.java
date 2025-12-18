package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.TraditionalMedicineExtractionService;
import org.openelisglobal.notebook.service.TraditionalMedicineExtractionService.ExtractionRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineExtractionService.ExtractionResponse;
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
 * REST controller for Traditional Medicine extraction, filtration, and
 * concentration.
 *
 * Per SRS Requirements - Extraction, Filtration & Concentration: - Extraction
 * Process: Use of solvents based on target compounds - Techniques: maceration,
 * Soxhlet, ultrasonic, distillation, etc. - Filtration: Remove plant debris and
 * impurities - Concentration: Evaporation or distillation to reduce volume and
 * enrich extract
 *
 * Endpoints: - POST /rest/notebook/tradmed/page/{pageId}/extract - Apply
 * extraction to samples - POST
 * /rest/notebook/tradmed/page/{pageId}/extract/complete - Mark samples as
 * extract ready - GET /rest/notebook/tradmed/page/{pageId}/extraction-status -
 * Get extraction status - GET /rest/notebook/tradmed/extraction/options - Get
 * available extraction options
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineExtractionController extends BaseRestController {

    @Autowired
    private TraditionalMedicineExtractionService extractionService;

    /**
     * Apply extraction to selected samples on a page. POST
     * /rest/notebook/tradmed/page/{pageId}/extract
     */
    @PostMapping(value = "/page/{pageId}/extract", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> extractSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody ExtractionRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        ExtractionRequest serviceRequest = new ExtractionRequest(request.sampleIds, request.solvent,
                request.otherSolvent, request.solventConcentration, request.extractionTechnique, request.otherTechnique,
                request.extractionDate, request.operator, parseBigDecimal(request.materialWeight),
                request.materialWeightUnit, parseBigDecimal(request.solventVolume), request.solventVolumeUnit,
                parseBigDecimal(request.extractionTemperature), request.temperatureUnit,
                request.extractionDurationMinutes, request.numberOfCycles, request.filtrationMethod,
                request.filterPoreSize, request.debrisRemoved, request.concentrationMethod,
                parseBigDecimal(request.concentrationTemperature), parseBigDecimal(request.finalVolume),
                request.finalVolumeUnit, request.extractId, parseBigDecimal(request.extractWeight),
                request.extractWeightUnit, request.extractAppearance, request.extractColor, request.notes);

        List<String> validationErrors = extractionService.validateExtractionRequest(serviceRequest);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                    String.join("; ", validationErrors), "validationErrors", validationErrors));
        }

        ExtractionResponse response = extractionService.extractSamples(pageId, serviceRequest, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("updatedCount", response.updatedCount());
        result.put("completedCount", response.completedCount());
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
     * Mark samples as extraction complete and ready for next step. POST
     * /rest/notebook/tradmed/page/{pageId}/extract/complete
     */
    @PostMapping(value = "/page/{pageId}/extract/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markComplete(@PathVariable("pageId") Integer pageId,
            @RequestBody CompletionRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        if (request.sampleIds == null || request.sampleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No sample IDs provided"));
        }

        ExtractionResponse response = extractionService.markExtractionComplete(pageId, request.sampleIds, sysUserId);

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
     * Get extraction status for all samples on a page. GET
     * /rest/notebook/tradmed/page/{pageId}/extraction-status
     */
    @GetMapping(value = "/page/{pageId}/extraction-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExtractionStatus(@PathVariable("pageId") Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = extractionService.getExtractionStatus(pageId);

        int totalCount = statusMap.size();
        int extractedCount = 0;
        int completedCount = 0;
        int pendingCount = 0;

        for (Map<String, Object> data : statusMap.values()) {
            String solvent = (String) data.get("solvent");
            String status = (String) data.get("status");

            if ("COMPLETED".equals(status)) {
                completedCount++;
            } else if (solvent != null && !solvent.isBlank()) {
                extractedCount++;
            } else {
                pendingCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("samples", statusMap);
        result.put("summary", Map.of("total", totalCount, "extracted", extractedCount, "completed", completedCount,
                "pending", pendingCount));

        return ResponseEntity.ok(result);
    }

    /**
     * Get available extraction options. GET
     * /rest/notebook/tradmed/extraction/options
     */
    @GetMapping(value = "/extraction/options", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExtractionOptions() {
        return ResponseEntity.ok(extractionService.getExtractionOptions());
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        return usd != null ? String.valueOf(usd.getSystemUserId()) : null;
    }

    public static class ExtractionRequestDTO {
        public List<Integer> sampleIds;
        public String solvent;
        public String otherSolvent;
        public String solventConcentration;
        public String extractionTechnique;
        public String otherTechnique;
        public String extractionDate;
        public String operator;
        public String materialWeight;
        public String materialWeightUnit;
        public String solventVolume;
        public String solventVolumeUnit;
        public String extractionTemperature;
        public String temperatureUnit;
        public Integer extractionDurationMinutes;
        public Integer numberOfCycles;
        public String filtrationMethod;
        public String filterPoreSize;
        public Boolean debrisRemoved;
        public String concentrationMethod;
        public String concentrationTemperature;
        public String finalVolume;
        public String finalVolumeUnit;
        public String extractId;
        public String extractWeight;
        public String extractWeightUnit;
        public String extractAppearance;
        public String extractColor;
        public String notes;
    }

    public static class CompletionRequestDTO {
        public List<Integer> sampleIds;
    }
}
