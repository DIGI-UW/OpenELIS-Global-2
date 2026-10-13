package org.openelisglobal.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The paired Analyzer Bridge, authenticated by its TLS client certificate. It
 * acts as the system user: no person is behind a delivery.
 */
public class AnalyzerBridgeAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public static final String ROLE = "ANALYZER_BRIDGE";

    private final String certificateSha256;

    public AnalyzerBridgeAuthenticationToken(String certificateSha256) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
        this.certificateSha256 = certificateSha256;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "analyzer-bridge";
    }

    public String getCertificateSha256() {
        return certificateSha256;
    }
}
