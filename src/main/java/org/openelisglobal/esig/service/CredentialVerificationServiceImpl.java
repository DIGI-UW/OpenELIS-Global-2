package org.openelisglobal.esig.service;

import java.util.Optional;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.esig.valueholder.AuthMethod;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of credential verification for electronic signatures.
 *
 * <p>
 * Currently supports local OpenELIS authentication only. Keycloak support is
 * planned for a future release.
 */
@Service
public class CredentialVerificationServiceImpl implements CredentialVerificationService {

    @Autowired
    private LoginUserService loginUserService;

    @Autowired
    private SystemUserService systemUserService;

    @Value("${org.itech.login.saml:false}")
    private boolean samlEnabled;

    @Value("${org.itech.login.oauth:false}")
    private boolean oauthEnabled;

    // ========================
    // Public Methods
    // ========================

    @Override
    public AuthMethod verifyCredentials(Long userId, String password) {
        // Get the login name for this user ID
        SystemUser systemUser = systemUserService.get(userId.toString());
        if (systemUser == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        String loginName = systemUser.getLoginName();
        if (loginName == null || loginName.isEmpty()) {
            throw new IllegalArgumentException("User has no login name: " + userId);
        }

        return verifyCredentialsByLoginName(loginName, password);
    }

    @Override
    public AuthMethod verifyCredentialsByLoginName(String loginName, String password) {
        if (loginName == null || loginName.isEmpty()) {
            throw new IllegalArgumentException("Login name is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Check if Keycloak is enabled - not supported yet
        if (isKeycloakEnabled()) {
            throw new UnsupportedOperationException(
                    "Keycloak credential verification for electronic signatures is not yet implemented. "
                            + "Please use local authentication or disable Keycloak SSO.");
        }

        // Verify against local authentication
        if (verifyLocalCredentials(loginName, password)) {
            return AuthMethod.LOCAL;
        }

        throw new IllegalArgumentException("Invalid credentials");
    }

    @Override
    public boolean isKeycloakEnabled() {
        return samlEnabled || oauthEnabled;
    }

    @Override
    public boolean isLocalAuthEnabled() {
        // Local auth is always available
        return true;
    }

    // ========================
    // Private Methods
    // ========================

    /**
     * Verify credentials against local OpenELIS database.
     */
    private boolean verifyLocalCredentials(String loginName, String password) {
        try {
            Optional<LoginUser> loginUser = loginUserService.getValidatedLogin(loginName, password);

            if (loginUser.isPresent()) {
                LoginUser user = loginUser.get();

                // Check account status
                if ("Y".equals(user.getAccountDisabled())) {
                    LogEvent.logWarn(getClass().getSimpleName(), "verifyLocalCredentials",
                            "E-signature auth failed: account disabled for user " + loginName);
                    return false;
                }

                if ("Y".equals(user.getAccountLocked())) {
                    LogEvent.logWarn(getClass().getSimpleName(), "verifyLocalCredentials",
                            "E-signature auth failed: account locked for user " + loginName);
                    return false;
                }

                return true;
            }

            return false;

        } catch (Exception e) {
            LogEvent.logError(e);
            return false;
        }
    }
}
