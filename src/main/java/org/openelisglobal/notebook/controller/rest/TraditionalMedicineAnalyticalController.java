package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.AnalyticalRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.AnalyticalResponse;
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
 * REST controller for Traditional Medicine Analytical Pathway.
 *
 * Per SRS Requirements - Advanced Analysis (Optional): - Fractionation:
 * Separate compounds, record method and fractions - Identification/Isolation:
 * Detect active constituents, link to compound database - Purification: Remove
 * impurities, log purity level - Characterization: Determine structure (NMR,
 * MS, IR spectral data)
 *
 * Endpoints: - POST /rest/notebook/tradmed/page/{pageId}/analytical - Apply
 * analytical data - POST
 * /rest/notebook/tradmed/page/{pageId}/analytical/complete - Mark as
 * characterized - POST /rest/notebook/tradmed/page/{pageId}/analytical/skip -
 * Skip to Testing - GET /rest/notebook/tradmed/page/{pageId}/analytical-status
 * - Get status - GET /rest/notebook/tradmed/analytical/options - Get dropdown
 * options
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineAnalyticalController extends BaseRestController {

    @Autowired
    private TraditionalMedicineAnalyticalService analyticalService;

    /**
     * Apply analytical data to selected samples on a page. POST
     * /rest/notebook/tradmed/page/{pageId}/analytical
     */
    @PostMapping(value = "/page/{pageId}/analytical", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyAnalyticalData(@PathVariable("pageId") Integer pageId,
            @RequestBody AnalyticalRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        AnalyticalRequest serviceRequest = new AnalyticalRequest(request.sampleIds, request.fractionationMethod,
                request.otherFractionationMethod, request.numberOfFractions, request.fractionIds,
                request.mobilePhaseSolvent, request.stationaryPhase, request.activeConstituentsIdentified,
                request.compoundDatabaseLinks, request.isolatedCompoundId,
                parseBigDecimal(request.isolatedCompoundWeight), request.isolatedCompoundWeightUnit,
                request.purificationMethod, request.otherPurificationMethod, parseBigDecimal(request.purityLevel),
                request.purityAssessmentMethod, request.characterizationTechniques, request.spectralFileReference,
                request.molecularFormula, parseBigDecimal(request.molecularWeight), request.structureDescription,
                request.analyst, request.analysisDate, request.notes);

        List<String> validationErrors = analyticalService.validateAnalyticalRequest(serviceRequest);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                    String.join("; ", validationErrors), "validationErrors", validationErrors));
        }

        AnalyticalResponse response = analyticalService.applyAnalyticalData(pageId, serviceRequest, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("updatedCount", response.updatedCount());

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Mark samples as characterized and complete. POST
     * /rest/notebook/tradmed/page/{pageId}/analytical/complete
     */
    @PostMapping(value = "/page/{pageId}/analytical/complete", produces = MediaType.APPLICATION_JSON_VALUE)
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

        AnalyticalResponse response = analyticalService.markAnalyticalComplete(pageId, request.sampleIds, sysUserId);

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
     * Skip analytical pathway and proceed directly to Testing. POST
     * /rest/notebook/tradmed/page/{pageId}/analytical/skip
     */
    @PostMapping(value = "/page/{pageId}/analytical/skip", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> skipAnalytical(@PathVariable("pageId") Integer pageId,
            @RequestBody CompletionRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        if (request.sampleIds == null || request.sampleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No sample IDs provided"));
        }

        AnalyticalResponse response = analyticalService.skipAnalyticalPathway(pageId, request.sampleIds, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("skippedCount", response.skippedCount());

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get analytical status for all samples on a page. GET
     * /rest/notebook/tradmed/page/{pageId}/analytical-status
     */
    @GetMapping(value = "/page/{pageId}/analytical-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalyticalStatus(@PathVariable("pageId") Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = analyticalService.getAnalyticalStatus(pageId);

        int totalCount = statusMap.size();
        int analyzedCount = 0;
        int completedCount = 0;
        int skippedCount = 0;
        int pendingCount = 0;

        for (Map<String, Object> data : statusMap.values()) {
            String status = (String) data.get("status");
            String fractionationMethod = (String) data.get("fractionationMethod");

            if ("COMPLETED".equals(status)) {
                completedCount++;
            } else if ("SKIPPED".equals(status)) {
                skippedCount++;
            } else if (fractionationMethod != null && !fractionationMethod.isBlank()) {
                analyzedCount++;
            } else {
                pendingCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("samples", statusMap);
        result.put("summary", Map.of("total", totalCount, "analyzed", analyzedCount, "completed", completedCount,
                "skipped", skippedCount, "pending", pendingCount));

        return ResponseEntity.ok(result);
    }

    /**
     * Get available analytical options. GET
     * /rest/notebook/tradmed/analytical/options
     */
    @GetMapping(value = "/analytical/options", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalyticalOptions() {
        return ResponseEntity.ok(analyticalService.getAnalyticalOptions());
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

    public static class AnalyticalRequestDTO {
        public List<Integer> sampleIds;
        // Fractionation
        public String fractionationMethod;
        public String otherFractionationMethod;
        public Integer numberOfFractions;
        public String fractionIds;
        public String mobilePhaseSolvent;
        public String stationaryPhase;
        // Identification/Isolation
        public String activeConstituentsIdentified;
        public String compoundDatabaseLinks;
        public String isolatedCompoundId;
        public String isolatedCompoundWeight;
        public String isolatedCompoundWeightUnit;
        // Purification
        public String purificationMethod;
        public String otherPurificationMethod;
        public String purityLevel;
        public String purityAssessmentMethod;
        // Characterization
        public List<String> characterizationTechniques;
        public String spectralFileReference;
        public String molecularFormula;
        public String molecularWeight;
        public String structureDescription;
        // General
        public String analyst;
        public String analysisDate;
        public String notes;
    }

    public static class CompletionRequestDTO {
        public List<Integer> sampleIds;
    }
}
