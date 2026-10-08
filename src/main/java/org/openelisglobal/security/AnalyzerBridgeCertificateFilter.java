package org.openelisglobal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.openelisglobal.analyzer.service.AnalyzerBridgePairingService;
import org.openelisglobal.analyzer.service.BridgeTls;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request whose TLS client certificate is the paired Analyzer
 * Bridge's. Tomcat accepts any client certificate in the handshake; this is
 * where OpenELIS decides whether it is the Bridge.
 */
public class AnalyzerBridgeCertificateFilter extends OncePerRequestFilter {

    private static final String CERTIFICATES = "jakarta.servlet.request.X509Certificate";

    private final ObjectProvider<AnalyzerBridgePairingService> pairing;

    public AnalyzerBridgeCertificateFilter(ObjectProvider<AnalyzerBridgePairingService> pairing) {
        this.pairing = pairing;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getAttribute(CERTIFICATES) instanceof X509Certificate[] certificates && certificates.length > 0
                && pairing.getObject().isPairedBridge(certificates[0])) {
            try {
                SecurityContextHolder.getContext()
                        .setAuthentication(new AnalyzerBridgeAuthenticationToken(BridgeTls.sha256(certificates[0])));
            } catch (CertificateException e) {
                throw new ServletException(e);
            }
        }
        chain.doFilter(request, response);
    }
}
