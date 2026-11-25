package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Creates destination-scoped clients; internal credentials never default onto
 * partners.
 */
@Component
public class FhirClientFactory {
    private final FhirConfig config;
    private final FhirContext context;
    private final ApacheRestfulClientFactory partnerFactory;
    private final Environment environment;
    private Map<URI, String> partners = Map.of();

    public FhirClientFactory(FhirConfig config, FhirContext context,
            @Qualifier("partnerFhirHttpClient") CloseableHttpClient partnerTransport, Environment environment) {
        this.config = config;
        this.context = context;
        this.environment = environment;
        // HAPI's metadata preflight consults the context's factory. Keep the partner
        // context separate so that preflight cannot use the local client identity.
        FhirContext partnerContext = new FhirContext(context.getVersion().getVersion());
        partnerFactory = new ApacheRestfulClientFactory(partnerContext);
        partnerFactory.setHttpClient(partnerTransport);
        partnerContext.setRestfulClientFactory(partnerFactory);
    }

    @PostConstruct
    public void configurePartners() {
        Map<URI, String> configured = new HashMap<>();
        for (String name : environment.getProperty("org.openelisglobal.fhir.partners", "").split(",")) {
            name = name.trim();
            if (name.isEmpty())
                continue;
            if (!name.matches("[A-Za-z0-9_-]+"))
                throw new IllegalArgumentException("Invalid FHIR partner name");
            String prefix = "org.openelisglobal.fhir.partner." + name + ".";
            URI base = baseUri(environment.getRequiredProperty(prefix + "uri"));
            requireHttps(base);
            if (isLocal(base) || configured.containsKey(base)) {
                throw new IllegalArgumentException("FHIR partners must have distinct external base URLs");
            }
            String token = environment.getProperty(prefix + "token", "");
            String username = environment.getProperty(prefix + "username", "");
            String password = environment.getProperty(prefix + "password", "");
            if (!token.isBlank() && (!username.isBlank() || !password.isEmpty())) {
                throw new IllegalArgumentException(
                        "Configure either Basic or Bearer authentication for a FHIR partner");
            }
            configured.put(base, token.isBlank() ? basic(username, password) : bearer(token));
        }
        partners = Map.copyOf(configured);
    }

    public IGenericClient create(String destination) {
        URI base = baseUri(destination);
        return create(base,
                isLocal(base) ? basic(config.getUsername(), config.getPassword()) : partners.getOrDefault(base, ""));
    }

    public IGenericClient create(String destination, String username, String password) {
        return create(baseUri(destination), basic(username, password));
    }

    public IGenericClient createBearer(String destination, String token) {
        return create(baseUri(destination), bearer(token));
    }

    private IGenericClient create(URI base, String authorization) {
        boolean local = isLocal(base);
        if (!local && !isAnonymousTestSubscriber(base, authorization))
            requireHttps(base);
        IGenericClient client = local ? context.newRestfulGenericClient(base.toString())
                : partnerFactory.newGenericClient(base.toString());
        client.registerInterceptor(new DestinationAuthentication(base, authorization));
        return client;
    }

    private boolean isAnonymousTestSubscriber(URI base, String authorization) {
        String subscriber = environment.getProperty("org.openelisglobal.fhir.subscriber", "");
        return authorization.isEmpty()
                && environment.getProperty("org.openelisglobal.fhir.subscriber.allowHTTP", Boolean.class, false)
                && !subscriber.isBlank() && base.equals(baseUri(subscriber));
    }

    private boolean isLocal(URI base) {
        String local = config.getLocalFhirStorePath();
        return local != null && !local.isBlank() && base.equals(baseUri(local));
    }

    static void requireHttps(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("External FHIR and authentication endpoints require HTTPS");
        }
    }

    static URI baseUri(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())))
                throw new IllegalArgumentException();
            String path = safePath(uri);
            while (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
            int port = uri.getPort();
            if (port == ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80))
                port = -1;
            return new URI(uri.getScheme().toLowerCase(Locale.ROOT), null, uri.getHost().toLowerCase(Locale.ROOT), port,
                    path, null, null);
        } catch (Exception e) {
            // Do not include a supplied URL: it may contain credentials.
            throw new IllegalArgumentException(
                    "FHIR base URL must be an absolute HTTP(S) URL without credentials, query or fragment");
        }
    }

    private static String safePath(URI uri) {
        String path = uri.getPath();
        String raw = uri.getRawPath().toLowerCase(Locale.ROOT);
        if (path.contains("\\") || path.contains(";") || path.contains("//")
                || path.chars().anyMatch(Character::isISOControl) || raw.contains("%2f") || raw.contains("%5c")
                || raw.contains("%25") || java.util.Arrays.stream(path.split("/", -1))
                        .anyMatch(segment -> segment.equals(".") || segment.equals(".."))) {
            throw new IllegalArgumentException("Ambiguous FHIR request path");
        }
        return path;
    }

    private static String basic(String username, String password) {
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        if (username.isBlank()) {
            if (!password.isEmpty())
                throw new IllegalArgumentException("FHIR Basic authentication requires a username");
            return "";
        }
        if (username.contains(":"))
            throw new IllegalArgumentException("FHIR Basic username cannot contain a colon");
        return "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        if (token == null || token.isBlank() || token.contains("\r") || token.contains("\n")) {
            throw new IllegalArgumentException("FHIR Bearer authentication requires a valid token");
        }
        return "Bearer " + token;
    }

    private static class DestinationAuthentication implements IClientInterceptor {
        private final URI base;
        private final String authorization;

        DestinationAuthentication(URI base, String authorization) {
            this.base = base;
            this.authorization = authorization;
        }

        @Override
        public void interceptRequest(IHttpRequest request) {
            URI target = URI.create(request.getUri());
            URI origin = baseUri(target.getScheme() + "://" + target.getRawAuthority());
            URI expected = baseUri(base.getScheme() + "://" + base.getRawAuthority());
            String path = safePath(target);
            if (!origin.equals(expected) || target.getFragment() != null
                    || !(path.equals(base.getPath()) || path.startsWith(base.getPath() + "/"))) {
                throw new IllegalArgumentException("FHIR request escaped its configured destination");
            }
            request.removeHeaders("Authorization");
            if (!authorization.isEmpty())
                request.addHeader("Authorization", authorization);
        }

        @Override
        public void interceptResponse(IHttpResponse response) {
        }
    }
}
