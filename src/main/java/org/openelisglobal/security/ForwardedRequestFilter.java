package org.openelisglobal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives the request the scheme, host and port the browser used when a reverse
 * proxy reports them, so the absolute URLs the application builds (the login
 * redirect, the page to return to after login, SAML and OAuth callbacks, FHIR
 * links) keep a non-standard port such as 8443.
 *
 * <p>
 * Reads the RFC 7239 {@code Forwarded} header, or {@code X-Forwarded-Proto},
 * {@code X-Forwarded-Host} and {@code X-Forwarded-Port}, only from a connection
 * on a loopback or private network address, where the proxy of a Docker or
 * on-premises install sits. A port in the {@code Host} header is kept when only
 * the protocol is forwarded. A forwarded host that is not a host name is
 * ignored: the openelis-docker nginx sent {@code X-Forwarded-Host: __} up to
 * release 3.3.0. The client address stays the connection's address, which the
 * audit records rely on.
 *
 * <p>
 * A redirect to a path (after logout, on an expired session) is resolved
 * against the forwarded address too: Tomcat would otherwise make it absolute
 * from its own view of the request, without the forwarded port.
 */
public class ForwardedRequestFilter extends OncePerRequestFilter {

    private static final List<IpAddressMatcher> TRUSTED_PROXIES = Stream.of("127.0.0.0/8", "::1/128", "10.0.0.0/8",
            "172.16.0.0/12", "192.168.0.0/16", "169.254.0.0/16", "100.64.0.0/10", "fc00::/7", "fe80::/10")
            .map(IpAddressMatcher::new).toList();

    private static final int MAX_HOST_LENGTH = 253;

    private static final Pattern HOST = Pattern
            .compile("(\\[[0-9A-Fa-f:.]+\\]|[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)(?::(\\d{1,5}))?");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest forwarded = fromTrustedProxy(request) ? forwarded(request) : request;
        chain.doFilter(forwarded, forwarded == request ? response
                : new ForwardedResponse(response, forwarded.getRequestURL().toString()));
    }

    private static boolean fromTrustedProxy(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return address != null && TRUSTED_PROXIES.stream().anyMatch(proxy -> proxy.matches(address));
    }

    private static HttpServletRequest forwarded(HttpServletRequest request) {
        Map<String, String> element = firstForwardedElement(request.getHeader("Forwarded"));
        String protocol = protocol(element.get("proto"));
        if (protocol == null) {
            protocol = protocol(firstValue(request.getHeader("X-Forwarded-Proto")));
        }
        Matcher host = hostAndPort(element.get("host"));
        Integer port = null;
        if (host == null) {
            host = hostAndPort(firstValue(request.getHeader("X-Forwarded-Host")));
            port = port(firstValue(request.getHeader("X-Forwarded-Port")));
        }
        if (protocol == null && host == null && port == null) {
            return request;
        }

        String scheme = protocol != null ? protocol : request.getScheme();
        if (port == null) {
            if (host != null) {
                port = port(host.group(2));
            } else if (request.getServerPort() != defaultPort(request.getScheme())) {
                port = request.getServerPort();
            }
        }
        return new ForwardedRequest(request, scheme, host != null ? host.group(1) : request.getServerName(),
                port != null ? port : defaultPort(scheme));
    }

    private static Map<String, String> firstForwardedElement(String header) {
        Map<String, String> element = new HashMap<>();
        String first = firstValue(header);
        if (first != null) {
            for (String pair : first.split(";")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    element.put(pair.substring(0, equals).trim().toLowerCase(Locale.ROOT),
                            pair.substring(equals + 1).trim().replace("\"", ""));
                }
            }
        }
        return element;
    }

    private static String firstValue(String header) {
        return header == null || header.isBlank() ? null : header.split(",")[0].trim();
    }

    private static String protocol(String value) {
        return "http".equalsIgnoreCase(value) || "https".equalsIgnoreCase(value) ? value.toLowerCase(Locale.ROOT)
                : null;
    }

    private static Matcher hostAndPort(String value) {
        if (value == null) {
            return null;
        }
        Matcher host = HOST.matcher(value);
        return host.matches() && host.group(1).length() <= MAX_HOST_LENGTH
                && (host.group(2) == null || port(host.group(2)) != null) ? host : null;
    }

    private static Integer port(String value) {
        if (value == null || !value.matches("\\d{1,5}")) {
            return null;
        }
        int port = Integer.parseInt(value);
        return port > 0 && port <= 65535 ? port : null;
    }

    private static int defaultPort(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }

    private static final class ForwardedRequest extends HttpServletRequestWrapper {

        private final String scheme;
        private final String serverName;
        private final int serverPort;

        ForwardedRequest(HttpServletRequest request, String scheme, String serverName, int serverPort) {
            super(request);
            this.scheme = scheme;
            this.serverName = serverName;
            this.serverPort = serverPort;
        }

        @Override
        public String getScheme() {
            return scheme;
        }

        @Override
        public boolean isSecure() {
            return "https".equals(scheme);
        }

        @Override
        public String getServerName() {
            return serverName;
        }

        @Override
        public int getServerPort() {
            return serverPort;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer(scheme).append("://").append(serverName);
            if (serverPort != defaultPort(scheme)) {
                url.append(':').append(serverPort);
            }
            return url.append(getRequestURI());
        }
    }

    private static final class ForwardedResponse extends HttpServletResponseWrapper {

        private final String requestUrl;

        ForwardedResponse(HttpServletResponse response, String requestUrl) {
            super(response);
            this.requestUrl = requestUrl;
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            String absolute = location;
            try {
                if (location != null && !URI.create(location).isAbsolute()) {
                    absolute = URI.create(requestUrl).resolve(location).toString();
                }
            } catch (IllegalArgumentException e) {
                absolute = location;
            }
            super.sendRedirect(absolute);
        }
    }
}
