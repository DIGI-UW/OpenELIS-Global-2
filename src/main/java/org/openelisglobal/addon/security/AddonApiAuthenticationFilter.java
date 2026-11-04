package org.openelisglobal.addon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.addon.service.AddonSettings;
import org.openelisglobal.addon.service.AddonTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards the add-on API. The API is internal: it answers only requests that
 * come straight from the networks where add-on containers run
 * ({@code addons.api.allowedNetworks}) and that did not pass through a reverse
 * proxy (no {@code Forwarded} / {@code X-Forwarded-*} header). A browser that
 * holds a frame token therefore cannot use it here. Then the bearer credential
 * is checked; a request without a valid one stays anonymous and gets 401.
 */
public class AddonApiAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<AddonTokenService> tokens;
    private final ObjectProvider<AddonSettings> settings;

    public AddonApiAuthenticationFilter(ObjectProvider<AddonTokenService> tokens,
            ObjectProvider<AddonSettings> settings) {
        this.tokens = tokens;
        this.settings = settings;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!fromAllowedNetwork(request, settings.getObject().allowedNetworks())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"network\"}");
            return;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AddonToken.Claims claims = tokens.getObject().authenticate(header.substring(7).trim());
                SecurityContextHolder.getContext().setAuthentication(new AddonAuthenticationToken(claims));
            } catch (IllegalArgumentException e) {
                // stays anonymous: the entry point answers 401
            }
        }
        chain.doFilter(request, response);
    }

    static boolean fromAllowedNetwork(HttpServletRequest request, List<String> networks) {
        if (request.getHeader("Forwarded") != null || request.getHeader("X-Forwarded-For") != null
                || request.getHeader("X-Forwarded-Host") != null || request.getHeader("X-Real-IP") != null) {
            return false;
        }
        String remote = request.getRemoteAddr();
        for (String network : networks) {
            try {
                if (new IpAddressMatcher(network).matches(remote)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // a malformed range matches nothing
            }
        }
        return false;
    }
}
