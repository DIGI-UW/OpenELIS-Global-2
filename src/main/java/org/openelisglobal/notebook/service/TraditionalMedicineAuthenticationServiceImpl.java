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
 * Implementation of Traditional Medicine authentication service.
 *
 * Per SRS Requirements - Sample Intake and Registration: - Authentication:
 * Botanical verification or expert identification - LMS logs authentication
 * method and result
 *
 * When authentication result is CONFIRMED, samples are automatically marked as
 * COMPLETED on the current page, advancing them to the next workflow stage
 * (Storage/Preparation).
 */
@Service
public class TraditionalMedicineAuthenticationServiceImpl implements TraditionalMedicineAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(TraditionalMedicineAuthenticationServiceImpl.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NoteBookService noteBookService;

    @Override
    @Transactional
    public AuthenticationResponse authenticateSamples(Integer pageId, AuthenticationRequest request, String sysUserId) {
        // Validate request
        List<String> validationErrors = validateAuthenticationRequest(request);
        if (!validationErrors.isEmpty()) {
            return AuthenticationResponse.error(String.join("; ", validationErrors));
        }

        if (request.sampleIds() == null || request.sampleIds().isEmpty()) {
            return AuthenticationResponse.error("No sample IDs provided");
        }

        // Get method and result enums for labels
        AuthenticationMethod method = AuthenticationMethod.fromId(request.authenticationMethod());
        AuthenticationResult result = AuthenticationResult.fromId(request.authenticationResult());

        if (method == null) {
            return AuthenticationResponse.error("Invalid authentication method: " + request.authenticationMethod());
        }
        if (result == null) {
            return AuthenticationResponse.error("Invalid authentication result: " + request.authenticationResult());
        }

        int updatedCount = 0;
        List<Integer> confirmedSampleIds = new ArrayList<>();
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

                // Store authentication data (SRS: LMS logs method and result)
                data.put("authenticationMethod", method.getId());
                data.put("authenticationMethodLabel", method.getLabel());
                data.put("authenticationResult", result.getId());
                data.put("authenticationResultLabel", result.getLabel());
                data.put("verifiedBy", request.verifiedBy());
                data.put("verificationDate", request.verificationDate());
                data.put("authenticationNotes", request.authenticationNotes());
                data.put("authenticatedAt", timestamp);
                data.put("authenticatedBy", sysUserId);

                nps.setData(data);
                nps.setSysUserId(sysUserId);

                // If authentication is CONFIRMED, collect for bulk status update (which
                // advances to next page)
                if (result == AuthenticationResult.CONFIRMED) {
                    confirmedSampleIds.add(sampleId);
                    log.info("Sample {} authenticated as CONFIRMED, will advance to next stage", sampleId);
                } else {
                    // For other results, mark as IN_PROGRESS (authenticated but not confirmed)
                    if (nps.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                    }
                    log.info("Sample {} authenticated as {}, status: {}", sampleId, result.getId(), nps.getStatus());
                }

                notebookPageSampleService.update(nps);
                updatedCount++;

            } catch (Exception e) {
                log.error("Error authenticating sample {}: {}", sampleId, e.getMessage(), e);
            }
        }

        // Use bulkUpdateStatus to mark confirmed samples as COMPLETED and auto-advance
        // to next page
        // Data is automatically copied to next page by
        // NotebookPageSampleServiceImpl.bulkUpdateStatus
        int advancedCount = 0;
        if (!confirmedSampleIds.isEmpty()) {
            advancedCount = notebookPageSampleService.bulkUpdateStatus(pageId, confirmedSampleIds,
                    NotebookPageSample.Status.COMPLETED, sysUserId);
            log.info("Advanced {} confirmed sample(s) to next page via bulkUpdateStatus (data auto-copied)",
                    advancedCount);
        }

        String message;
        if (result == AuthenticationResult.CONFIRMED) {
            message = String.format("Authenticated %d sample(s). %d sample(s) confirmed and advanced to next stage.",
                    updatedCount, advancedCount);
        } else {
            message = String.format("Applied authentication (%s) to %d sample(s).", result.getLabel(), updatedCount);
        }

        log.info("Authentication complete: pageId={}, method={}, result={}, updated={}, advanced={}", pageId,
                method.getId(), result.getId(), updatedCount, advancedCount);

        return AuthenticationResponse.success(updatedCount, advancedCount, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Object>> getAuthenticationStatus(Integer pageId) {
        Map<Integer, Map<String, Object>> statusMap = new HashMap<>();

        List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(pageId);
        for (NotebookPageSample nps : samples) {
            Map<String, Object> authData = new HashMap<>();

            if (nps.getData() != null) {
                Map<String, Object> data = nps.getData();
                authData.put("authenticationMethod", data.get("authenticationMethod"));
                authData.put("authenticationMethodLabel", data.get("authenticationMethodLabel"));
                authData.put("authenticationResult", data.get("authenticationResult"));
                authData.put("authenticationResultLabel", data.get("authenticationResultLabel"));
                authData.put("verifiedBy", data.get("verifiedBy"));
                authData.put("verificationDate", data.get("verificationDate"));
                authData.put("authenticationNotes", data.get("authenticationNotes"));
                authData.put("authenticatedAt", data.get("authenticatedAt"));
            }

            authData.put("status", nps.getStatus());
            String sampleItemIdStr = nps.getSampleItemId();
            authData.put("sampleItemId", sampleItemIdStr);

            if (sampleItemIdStr != null && !sampleItemIdStr.isBlank()) {
                try {
                    Integer sampleId = Integer.parseInt(sampleItemIdStr);
                    statusMap.put(sampleId, authData);
                } catch (NumberFormatException e) {
                    log.warn("Invalid sampleItemId format: {}", sampleItemIdStr);
                }
            }
        }

        return statusMap;
    }

    @Override
    public List<String> validateAuthenticationRequest(AuthenticationRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.authenticationMethod() == null || request.authenticationMethod().isBlank()) {
            errors.add("Authentication method is required");
        } else {
            AuthenticationMethod method = AuthenticationMethod.fromId(request.authenticationMethod());
            if (method == null) {
                errors.add("Invalid authentication method: " + request.authenticationMethod());
            }
        }

        if (request.authenticationResult() == null || request.authenticationResult().isBlank()) {
            errors.add("Authentication result is required");
        } else {
            AuthenticationResult result = AuthenticationResult.fromId(request.authenticationResult());
            if (result == null) {
                errors.add("Invalid authentication result: " + request.authenticationResult());
            }
        }

        return errors;
    }
}
