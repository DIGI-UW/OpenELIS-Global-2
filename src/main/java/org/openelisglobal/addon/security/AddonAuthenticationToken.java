package org.openelisglobal.addon.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * An add-on calling the add-on API, authenticated by a token or its service
 * key. It holds the add-on role only: no OpenELIS user role is ever granted to
 * an add-on.
 */
public class AddonAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public static final String ROLE = "ADDON";

    private final transient AddonToken.Claims claims;

    public AddonAuthenticationToken(AddonToken.Claims claims) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "addon:" + claims.addonId();
    }

    public AddonToken.Claims getClaims() {
        return claims;
    }
}
