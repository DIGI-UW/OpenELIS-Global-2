package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.TraditionalMedicineAuthenticationService;
import org.openelisglobal.notebook.service.TraditionalMedicineAuthenticationService.AuthenticationMethod;
import org.openelisglobal.notebook.service.TraditionalMedicineAuthenticationService.AuthenticationRequest;
import org.openelisglobal.notebook.service.TraditionalMedicineAuthenticationService.AuthenticationResponse;
import org.openelisglobal.notebook.service.TraditionalMedicineAuthenticationService.AuthenticationResult;
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
 * REST controller for Traditional Medicine sample authentication.
 *
 * Per SRS Requirements - Sample Intake and Registration: - Authentication:
 * Botanical verification or expert identification - LMS logs authentication
 * method and result
 *
 * Endpoints: - POST /rest/notebook/tradmed/page/{pageId}/authenticate - Apply
 * authentication to samples - GET
 * /rest/notebook/tradmed/page/{pageId}/authentication-status - Get
 * authentication status - GET /rest/notebook/tradmed/authentication/options -
 * Get available authentication options
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineAuthenticationController extends BaseRestController {

    @Autowired
    private TraditionalMedicineAuthenticationService authenticationService;

    /**
     * Apply authentication to selected samples on a page. POST
     * /rest/notebook/tradmed/page/{pageId}/authenticate
     *
     * When authentication result is "confirmed", samples are automatically marked
     * as COMPLETED and advanced to the next workflow stage.
     *
     * @param pageId      the notebook page ID (Page 1 - Sample Creation)
     * @param request     authentication request with method, result, and metadata
     * @param httpRequest for getting user session
     * @return authentication response with counts and status
     */
    @PostMapping(value = "/page/{pageId}/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> authenticateSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody AuthenticationRequestDTO request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Convert DTO to service request
        AuthenticationRequest serviceRequest = new AuthenticationRequest(request.sampleIds,
                request.authenticationMethod, request.authenticationResult, request.verifiedBy,
                request.verificationDate, request.authenticationNotes);

        // Validate first
        List<String> validationErrors = authenticationService.validateAuthenticationRequest(serviceRequest);
        if (!validationErrors.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", String.join("; ", validationErrors));
            error.put("validationErrors", validationErrors);
            return ResponseEntity.badRequest().body(error);
        }

        // Process authentication
        AuthenticationResponse response = authenticationService.authenticateSamples(pageId, serviceRequest, sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("updatedCount", response.updatedCount());
        result.put("advancedCount", response.advancedCount());

        if (response.success()) {
            result.put("message", response.message());
            return ResponseEntity.ok(result);
        } else {
            result.put("error", response.error());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get authentication status for all samples on a page. GET
     * /rest/notebook/tradmed/page/{pageId}/authentication-status
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to authentication data
     */
    @GetMapping(value = "/page/{pageId}/authentication-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuthenticationStatus(@PathVariable("pageId") Integer pageId) {

        Map<Integer, Map<String, Object>> statusMap = authenticationService.getAuthenticationStatus(pageId);

        // Calculate summary counts
        int totalCount = statusMap.size();
        int authenticatedCount = 0;
        int confirmedCount = 0;
        int pendingCount = 0;

        for (Map<String, Object> authData : statusMap.values()) {
            String authResult = (String) authData.get("authenticationResult");
            if (authResult != null) {
                authenticatedCount++;
                if ("confirmed".equals(authResult)) {
                    confirmedCount++;
                }
            } else {
                pendingCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("samples", statusMap);
        result.put("summary", Map.of("total", totalCount, "authenticated", authenticatedCount, "confirmed",
                confirmedCount, "pending", pendingCount));

        return ResponseEntity.ok(result);
    }

    /**
     * Get available authentication method and result options. GET
     * /rest/notebook/tradmed/authentication/options
     *
     * @return available authentication options
     */
    @GetMapping(value = "/authentication/options", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuthenticationOptions() {
        Map<String, Object> result = new HashMap<>();

        // Authentication methods
        List<Map<String, String>> methods = new java.util.ArrayList<>();
        for (AuthenticationMethod method : AuthenticationMethod.values()) {
            methods.add(Map.of("id", method.getId(), "label", method.getLabel()));
        }
        result.put("methods", methods);

        // Authentication results
        List<Map<String, String>> results = new java.util.ArrayList<>();
        for (AuthenticationResult authResult : AuthenticationResult.values()) {
            results.add(Map.of("id", authResult.getId(), "label", authResult.getLabel()));
        }
        result.put("results", results);

        return ResponseEntity.ok(result);
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
     * DTO for authentication request from frontend.
     */
    public static class AuthenticationRequestDTO {
        private List<Integer> sampleIds;
        private String authenticationMethod;
        private String authenticationResult;
        private String verifiedBy;
        private String verificationDate;
        private String authenticationNotes;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getAuthenticationMethod() {
            return authenticationMethod;
        }

        public void setAuthenticationMethod(String authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
        }

        public String getAuthenticationResult() {
            return authenticationResult;
        }

        public void setAuthenticationResult(String authenticationResult) {
            this.authenticationResult = authenticationResult;
        }

        public String getVerifiedBy() {
            return verifiedBy;
        }

        public void setVerifiedBy(String verifiedBy) {
            this.verifiedBy = verifiedBy;
        }

        public String getVerificationDate() {
            return verificationDate;
        }

        public void setVerificationDate(String verificationDate) {
            this.verificationDate = verificationDate;
        }

        public String getAuthenticationNotes() {
            return authenticationNotes;
        }

        public void setAuthenticationNotes(String authenticationNotes) {
            this.authenticationNotes = authenticationNotes;
        }
    }
}
