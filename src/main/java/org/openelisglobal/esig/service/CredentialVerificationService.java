package org.openelisglobal.esig.service;

import org.openelisglobal.esig.valueholder.AuthMethod;

/**
 * Service for verifying user credentials during electronic signature
 * ceremonies.
 *
 * <p>
 * Supports both local OpenELIS authentication and Keycloak SSO. Used by
 * {@link ElectronicSignatureService} to verify the signer's identity before
 * creating a signature record.
 */
public interface CredentialVerificationService {

    /**
     * Verify credentials for a user by user ID.
     *
     * @param userId   the system_user ID
     * @param password the user's password
     * @return the authentication method used (LOCAL or KEYCLOAK)
     * @throws IllegalArgumentException if credentials are invalid
     * @throws IllegalStateException    if authentication system is unavailable
     */
    AuthMethod verifyCredentials(Long userId, String password);

    /**
     * Verify credentials for a user by login name.
     *
     * @param loginName the user's login name
     * @param password  the user's password
     * @return the authentication method used (LOCAL or KEYCLOAK)
     * @throws IllegalArgumentException if credentials are invalid
     * @throws IllegalStateException    if authentication system is unavailable
     */
    AuthMethod verifyCredentialsByLoginName(String loginName, String password);

    /**
     * Check if Keycloak authentication is enabled for this site.
     *
     * @return true if Keycloak is the configured auth provider
     */
    boolean isKeycloakEnabled();

    /**
     * Check if local authentication is enabled for this site.
     *
     * @return true if local auth is available
     */
    boolean isLocalAuthEnabled();
}
