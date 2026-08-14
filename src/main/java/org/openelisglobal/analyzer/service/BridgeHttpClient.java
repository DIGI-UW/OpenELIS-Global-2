package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * The single HTTP client for every OE2 → analyzer-bridge call.
 *
 * <p>
 * The connection + TLS setup used to be hand-rolled in five places —
 * registration, drift sync, query, test-connectivity/health, and order dispatch
 * — each opening its own {@code HttpURLConnection}/{@code HttpClient} and (in
 * four of them) pasting the same ~13-line trust-all {@code SSLContext} block.
 * The fifth, order dispatch, silently omitted that block and so failed PKIX
 * against the bridge's self-signed cert while every other path worked. Routing
 * all bridge traffic through this one component is what makes that class of
 * "one path forgot the TLS config" bug impossible to reintroduce.
 *
 * <p>
 * TLS uses the configured OpenELIS truststore and normal hostname verification.
 * A configured truststore that cannot be loaded fails component construction;
 * an absent truststore uses the JVM defaults. Neither path silently trusts an
 * unknown Bridge certificate.
 */
@Component
public class BridgeHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    @Value("${analyzer.bridge.username:}")
    private String username;

    @Value("${analyzer.bridge.password:}")
    private String password;

    public BridgeHttpClient(@Value("${server.ssl.trust-store:}") String trustStoreLocation,
            @Value("${server.ssl.trust-store-password:}") String trustStorePassword,
            @Value("${server.ssl.trust-store-type:PKCS12}") String trustStoreType, ResourceLoader resourceLoader) {
        this.httpClient = buildClient(trustStoreLocation, trustStorePassword, trustStoreType, resourceLoader);
    }

    private static HttpClient buildClient(String trustStoreLocation, String trustStorePassword, String trustStoreType,
            ResourceLoader resourceLoader) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT);
        if (trustStoreLocation == null || trustStoreLocation.isBlank()) {
            return builder.build();
        }

        try {
            Resource trustStoreResource = resourceLoader.getResource(trustStoreLocation.trim());
            KeyStore trustStore = KeyStore
                    .getInstance(trustStoreType == null || trustStoreType.isBlank() ? "PKCS12" : trustStoreType);
            try (InputStream input = trustStoreResource.getInputStream()) {
                trustStore.load(input, trustStorePassword == null ? new char[0] : trustStorePassword.toCharArray());
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return builder.sslContext(sslContext).build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize Bridge TLS truststore " + trustStoreLocation, e);
        }
    }

    /** Status + body of a bridge call. Callers interpret the body themselves. */
    public static final class BridgeResponse {
        public final int status;
        public final String body;

        public BridgeResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }

    public BridgeResponse get(String url, Duration readTimeout) throws IOException {
        return send("GET", url, null, readTimeout);
    }

    public BridgeResponse post(String url, String jsonBody, Duration readTimeout) throws IOException {
        return send("POST", url, jsonBody, readTimeout);
    }

    public BridgeResponse put(String url, String jsonBody, Duration readTimeout) throws IOException {
        return send("PUT", url, jsonBody, readTimeout);
    }

    public BridgeResponse delete(String url, Duration readTimeout) throws IOException {
        return send("DELETE", url, null, readTimeout);
    }

    /**
     * Issue a request to the bridge. {@code jsonBody == null} sends no body (GET /
     * DELETE); otherwise the body is sent as {@code application/json}. Returns the
     * status and body regardless of status code — the caller decides what counts as
     * success — so error responses are returned, not thrown (matching the prior
     * read-the-error-stream behavior of the call sites this replaces).
     */
    public BridgeResponse send(String method, String url, String jsonBody, Duration readTimeout) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).timeout(readTimeout);
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            String token = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
        HttpRequest.BodyPublisher publisher;
        if (jsonBody == null) {
            publisher = HttpRequest.BodyPublishers.noBody();
        } else {
            publisher = HttpRequest.BodyPublishers.ofString(jsonBody);
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, publisher);
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new BridgeResponse(response.statusCode(), response.body() != null ? response.body() : "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(method + " " + url + " interrupted", e);
        }
    }
}
