package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.AnalyticalResponse;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.CharacterizationRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.FractionationRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.IdentificationRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.PathwaySelectionRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineAnalyticalService.PurificationRequest;
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
 * REST controller for Traditional Medicine Analytical Pathways (Page 6).
 *
 * Per SRS Requirements - STAGE 6: Analytical Pathways
 *
 * Branching workflow:
 * - PATH A: Advanced Analysis (mandatory all 4 steps on Page 6)
 * - PATH B: Direct to Production (skip Page 6)
 *
 * Endpoints:
 * - POST /rest/notebook/tradmed/page/{pageId}/select-pathway - Select pathway at end of Page 5
 * - POST /rest/notebook/tradmed/page/{pageId}/record-fractionation - Step 1 for PATH A
 * - POST /rest/notebook/tradmed/page/{pageId}/record-identification - Step 2 for PATH A
 * - POST /rest/notebook/tradmed/page/{pageId}/record-purification - Step 3 for PATH A
 * - POST /rest/notebook/tradmed/page/{pageId}/record-characterization - Step 4 for PATH A
 * - GET /rest/notebook/tradmed/page/{pageId}/analytical-status - Get status
 * - POST /rest/notebook/tradmed/page/{pageId}/mark-analysis-complete - Mark complete and advance
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineAnalyticalController extends BaseRestController {

    @Autowired
    private TraditionalMedicineAnalyticalService analyticalService;

    /**
     * Select analysis pathway (PATH A or PATH B) at end of Page 5.
     * Pathway is LOCKED after selection and cannot be changed.
     * POST /rest/notebook/tradmed/page/{pageId}/select-pathway
     */
    @PostMapping(value = "/page/{pageId}/select-pathway", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectPathway(
            @PathVariable("pageId") Integer pageId,
            @RequestBody PathwaySelectionRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        AnalyticalResponse response = analyticalService.selectPathway(pageId, request, sysUserId);

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
     * Record fractionation - MANDATORY STEP 1 for PATH A.
     * POST /rest/notebook/tradmed/page/{pageId}/record-fractionation
     */
    @PostMapping(value = "/page/{pageId}/record-fractionation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordFractionation(
            @PathVariable("pageId") Integer pageId,
            @RequestBody FractionationRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        AnalyticalResponse response = analyticalService.recordFractionation(pageId, request, sysUserId);

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
     * Record identification/isolation - MANDATORY STEP 2 for PATH A.
     * POST /rest/notebook/tradmed/page/{pageId}/record-identification
     */
    @PostMapping(value = "/page/{pageId}/record-identification", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordIdentification(
            @PathVariable("pageId") Integer pageId,
            @RequestBody IdentificationRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        AnalyticalResponse response = analyticalService.recordIdentification(pageId, request, sysUserId);

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
     * Record purification - MANDATORY STEP 3 for PATH A.
     * POST /rest/notebook/tradmed/page/{pageId}/record-purification
     */
    @PostMapping(value = "/page/{pageId}/record-purification", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordPurification(
            @PathVariable("pageId") Integer pageId,
            @RequestBody PurificationRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        AnalyticalResponse response = analyticalService.recordPurification(pageId, request, sysUserId);

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
     * Record characterization - MANDATORY STEP 4 for PATH A.
     * POST /rest/notebook/tradmed/page/{pageId}/record-characterization
     */
    @PostMapping(value = "/page/{pageId}/record-characterization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordCharacterization(
            @PathVariable("pageId") Integer pageId,
            @RequestBody CharacterizationRequest request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        AnalyticalResponse response = analyticalService.recordCharacterization(pageId, request, sysUserId);

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
     * Get analytical status for all samples on a page.
     * GET /rest/notebook/tradmed/page/{pageId}/analytical-status
     */
    @GetMapping(value = "/page/{pageId}/analytical-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getAnalyticalStatus(@PathVariable("pageId") Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = analyticalService.getAnalyticalStatus(pageId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", statusMap);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark samples as analysis complete and ready for next stage (Page 7).
     * For PATH A: All 4 steps must be complete before marking.
     * For PATH B: Samples skip this page entirely.
     * POST /rest/notebook/tradmed/page/{pageId}/mark-analysis-complete
     */
    @PostMapping(value = "/page/{pageId}/mark-analysis-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAnalysisComplete(
            @PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, List<Integer>> request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        List<Integer> sampleIds = request.get("sampleIds");
        if (sampleIds == null || sampleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No sample IDs provided"));
        }

        AnalyticalResponse response = analyticalService.markAnalysisComplete(pageId, sampleIds, sysUserId);

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

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        return usd != null ? String.valueOf(usd.getSystemUserId()) : null;
    }
}
