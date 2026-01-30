package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Traditional Medicine sample authentication.
 *
 * Per SRS Requirements - Sample Intake and Registration: - Authentication:
 * Botanical verification or expert identification - LMS logs authentication
 * method and result
 *
 * When authentication is confirmed, samples are marked as COMPLETED on the
 * current page, advancing them to the next workflow stage.
 */
public interface TraditionalMedicineAuthenticationService {

    /**
     * Authentication method options per SRS.
     */
    enum AuthenticationMethod {
        MORPHOLOGICAL_EXAMINATION("morphological_examination", "Morphological examination"),
        MICROSCOPIC_ANALYSIS("microscopic_analysis", "Microscopic analysis"), MOLECULAR_IDENTIFICATION(
                "molecular_identification", "Molecular identification (DNA barcoding: ITS, rbcL, matK genes)");

        private final String id;
        private final String label;

        AuthenticationMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static AuthenticationMethod fromId(String id) {
            for (AuthenticationMethod method : values()) {
                if (method.id.equals(id)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * Authentication result options.
     */
    enum AuthenticationResult {
        CONFIRMED("confirmed", "Confirmed / Authenticated"), NOT_CONFIRMED("not_confirmed", "Not Confirmed"),
        INCONCLUSIVE("inconclusive", "Inconclusive - Further Testing Required"),
        PARTIAL("partial", "Partially Confirmed");

        private final String id;
        private final String label;

        AuthenticationResult(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static AuthenticationResult fromId(String id) {
            for (AuthenticationResult result : values()) {
                if (result.id.equals(id)) {
                    return result;
                }
            }
            return null;
        }
    }

    /**
     * Request object for authentication operation.
     */
    record AuthenticationRequest(List<Integer> sampleIds, String authenticationMethod, String authenticationResult,
            String verifiedBy, String verificationDate, String authenticationNotes) {
    }

    /**
     * Result of authentication operation.
     */
    record AuthenticationResponse(boolean success, int updatedCount, int advancedCount, String message, String error) {

        public static AuthenticationResponse success(int updatedCount, int advancedCount, String message) {
            return new AuthenticationResponse(true, updatedCount, advancedCount, message, null);
        }

        public static AuthenticationResponse error(String error) {
            return new AuthenticationResponse(false, 0, 0, null, error);
        }
    }

    /**
     * Apply authentication to samples on a notebook page.
     *
     * @param pageId    the notebook page ID (Page 1 - Sample Creation)
     * @param request   the authentication request with method, result, and metadata
     * @param sysUserId the system user ID performing the authentication
     * @return authentication response with counts and status
     */
    AuthenticationResponse authenticateSamples(Integer pageId, AuthenticationRequest request, String sysUserId);

    /**
     * Get authentication status for samples on a page.
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to authentication data
     */
    Map<Integer, Map<String, Object>> getAuthenticationStatus(Integer pageId);

    /**
     * Validate authentication request data.
     *
     * @param request the authentication request
     * @return list of validation errors (empty if valid)
     */
    List<String> validateAuthenticationRequest(AuthenticationRequest request);
}
