package org.openelisglobal.esig.valueholder;

/**
 * Identifies the authentication provider used to verify credentials during an
 * electronic signature ceremony.
 */
public enum AuthMethod {

    /**
     * Credentials verified against OpenELIS local user database.
     */
    LOCAL,

    /**
     * Credentials verified against Keycloak SSO.
     */
    KEYCLOAK
}
