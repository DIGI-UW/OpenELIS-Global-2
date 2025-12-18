package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.TraditionalMedicineTestingService;
import org.openelisglobal.notebook.service.TraditionalMedicineTestingService.TestingRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineTestingService.TestingResponse;
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
 * REST controller for Traditional Medicine Testing (Product Development &
 * Testing).
 *
 * Per SRS Requirements - Product Development and Testing: - Preliminary
 * Phytochemical Screening: Test for alkaloids, flavonoids, etc. -
 * Safety/Toxicity Study: In vitro or animal models - Efficacy Test: Biological
 * activity assays
 *
 * Endpoints: - POST /rest/notebook/tradmed/page/{pageId}/testing - Apply
 * testing data - POST /rest/notebook/tradmed/page/{pageId}/testing/approve -
 * Approve for formulation - POST
 * /rest/notebook/tradmed/page/{pageId}/testing/reject - Reject samples - POST
 * /rest/notebook/tradmed/page/{pageId}/testing/further - Mark for further
 * testing - GET /rest/notebook/tradmed/page/{pageId}/testing-status - Get
 * status - GET /rest/notebook/tradmed/testing/options - Get dropdown options
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineTestingController extends BaseRestController {

    @Autowired
    private TraditionalMedicineTestingService testingService;

    /**
     * Apply testing data to selected samples on a page. POST
     * /rest/notebook/tradmed/page/{pageId}/testing
     */
    @PostMapping(value = "/page/{pageId}/testing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyTestingData(@PathVariable("pageId") Integer pageId,
            @RequestBody TestingRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        TestingRequest serviceRequest = new TestingRequest(request.sampleIds, request.detectedPhytochemicals,
                request.phytochemicalScreeningNotes, request.safetyStudyType, request.toxicityModel,
                request.toxicityOutcome, request.ld50Value, request.safetyStudyReference, request.biologicalAssayType,
                request.otherAssayType, request.efficacyOutcome, request.assayProtocol, request.ic50Value,
                request.efficacyStudyReference, request.testedBy, request.testDate, request.notes);

        List<String> validationErrors = testingService.validateTestingRequest(serviceRequest);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                    String.join("; ", validationErrors), "validationErrors", validationErrors));
        }

        TestingResponse response = testingService.applyTestingData(pageId, serviceRequest, sysUserId);

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
     * Approve samples for formulation after passing testing. POST
     * /rest/notebook/tradmed/page/{pageId}/testing/approve
     */
    @PostMapping(value = "/page/{pageId}/testing/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody CompletionRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        if (request.sampleIds == null || request.sampleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No sample IDs provided"));
        }

        TestingResponse response = testingService.approveSamplesForFormulation(pageId, request.sampleIds, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("approvedCount", response.approvedCount());

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Reject samples that failed safety or efficacy testing. POST
     * /rest/notebook/tradmed/page/{pageId}/testing/reject
     */
    @PostMapping(value = "/page/{pageId}/testing/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody RejectRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        if (request.sampleIds == null || request.sampleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No sample IDs provided"));
        }

        TestingResponse response = testingService.rejectSamples(pageId, request.sampleIds, request.rejectReason,
                sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("rejectedCount", response.rejectedCount());

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Mark samples for further testing. POST
     * /rest/notebook/tradmed/page/{pageId}/testing/further
     */
    @PostMapping(value = "/page/{pageId}/testing/further", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markForFurtherTesting(@PathVariable("pageId") Integer pageId,
            @RequestBody CompletionRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "User session not found"));
        }

        if (request.sampleIds == null || request.sampleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No sample IDs provided"));
        }

        TestingResponse response = testingService.markForFurtherTesting(pageId, request.sampleIds, sysUserId);

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
     * Get testing status for all samples on a page. GET
     * /rest/notebook/tradmed/page/{pageId}/testing-status
     */
    @GetMapping(value = "/page/{pageId}/testing-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTestingStatus(@PathVariable("pageId") Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = testingService.getTestingStatus(pageId);

        int totalCount = statusMap.size();
        int testedCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int furtherTestingCount = 0;
        int pendingCount = 0;

        for (Map<String, Object> data : statusMap.values()) {
            String status = (String) data.get("status");
            String testingStatus = (String) data.get("testingStatus");

            if ("COMPLETED".equals(status) || "APPROVED".equals(testingStatus)) {
                approvedCount++;
            } else if ("REJECTED".equals(status) || "REJECTED".equals(testingStatus)) {
                rejectedCount++;
            } else if ("FURTHER_TESTING_REQUIRED".equals(testingStatus)) {
                furtherTestingCount++;
            } else if (data.get("toxicityOutcome") != null || data.get("efficacyOutcome") != null) {
                testedCount++;
            } else {
                pendingCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("samples", statusMap);
        result.put("summary", Map.of("total", totalCount, "tested", testedCount, "approved", approvedCount, "rejected",
                rejectedCount, "furtherTesting", furtherTestingCount, "pending", pendingCount));

        return ResponseEntity.ok(result);
    }

    /**
     * Get available testing options. GET /rest/notebook/tradmed/testing/options
     */
    @GetMapping(value = "/testing/options", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTestingOptions() {
        return ResponseEntity.ok(testingService.getTestingOptions());
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        return usd != null ? String.valueOf(usd.getSystemUserId()) : null;
    }

    public static class TestingRequestDTO {
        public List<Integer> sampleIds;
        // Phytochemical screening
        public List<String> detectedPhytochemicals;
        public String phytochemicalScreeningNotes;
        // Safety/Toxicity
        public String safetyStudyType;
        public String toxicityModel;
        public String toxicityOutcome;
        public String ld50Value;
        public String safetyStudyReference;
        // Efficacy
        public String biologicalAssayType;
        public String otherAssayType;
        public String efficacyOutcome;
        public String assayProtocol;
        public String ic50Value;
        public String efficacyStudyReference;
        // General
        public String testedBy;
        public String testDate;
        public String notes;
    }

    public static class CompletionRequestDTO {
        public List<Integer> sampleIds;
    }

    public static class RejectRequestDTO {
        public List<Integer> sampleIds;
        public String rejectReason;
    }
}
