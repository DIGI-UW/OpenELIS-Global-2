package org.openelisglobal.addon.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.addon.security.AddonToken;

/**
 * Credentials of add-ons: short-lived tokens issued to UI frames for one user
 * and the orders on that user's screen, and the per-add-on service API key.
 * Every check reads the registry, so disabling an add-on or removing a scope
 * takes effect on the next request.
 */
public interface AddonTokenService {

    record IssuedToken(String token, long expiresIn, List<String> scopes, Map<String, Object> context) {
    }

    /** Why a token was not issued. */
    enum Refusal {
        NOT_FOUND, SLOT, ROLE
    }

    class TokenRefusedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final Refusal refusal;

        public TokenRefusedException(Refusal refusal) {
            super(refusal.name());
            this.refusal = refusal;
        }

        public Refusal getRefusal() {
            return refusal;
        }
    }

    /**
     * A token for an add-on frame in {@code slot}. The context is filtered to what
     * the add-on's scopes allow; the token carries the remaining lab numbers.
     *
     * @param userRoles role names of the signed-in user (without ROLE_)
     * @throws TokenRefusedException if the add-on is missing or disabled, does not
     *                               contribute to the slot, or the user lacks a
     *                               role the slot needs
     */
    IssuedToken issue(String addonId, String slot, Map<String, ?> context, String userId, Collection<String> userRoles);

    /**
     * Authenticates a bearer credential (a token, or the service API key) of an
     * enabled add-on. The effective scopes are the token's scopes that the add-on
     * still holds.
     *
     * @throws IllegalArgumentException if refused, without saying why
     */
    AddonToken.Claims authenticate(String bearer);

    /**
     * A stable pseudonym of {@code value} (for example a patient id) for one
     * add-on: the same value gives the same pseudonym to the same add-on, cannot be
     * reversed, and differs between add-ons.
     */
    String opaqueId(String addonId, String value);
}
