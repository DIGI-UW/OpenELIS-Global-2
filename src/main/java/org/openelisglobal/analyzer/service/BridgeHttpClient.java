package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * The bridge presents a self-signed cert on the internal OE2↔bridge hop, so
 * this trusts all certificates. That is acceptable for that private hop only;
 * production hardening (a real truststore / pinned CA) belongs here, in this
 * one place, rather than in five.
 */
@Component
public class BridgeHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(BridgeHttpClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    public BridgeHttpClient() {
        this.httpClient = buildTrustAllClient();
    }

    private static HttpClient buildTrustAllClient() {
        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[] { new javax.net.ssl.X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String s) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String s) {
                }
            } }, new java.security.SecureRandom());
            return HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).sslContext(sslContext).build();
        } catch (Exception e) {
            logger.warn("Bridge SSL context init failed; using default HttpClient (self-signed bridge calls will"
                    + " fail PKIX): {}", e.getMessage());
            return HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
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
