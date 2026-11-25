package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The single HTTP client for every OE2 → analyzer-bridge call.
 *
 * <p>
 * Calls go only to the Bridge OpenELIS paired with: OpenELIS presents its own
 * certificate and accepts only the Bridge certificate it pinned, so no password
 * is sent and no other server is trusted.
 */
@Component
public class BridgeHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final AnalyzerBridgePairingService pairing;
    private HttpClient client;
    private String clientFingerprint;

    public BridgeHttpClient(AnalyzerBridgePairingService pairing) {
        this.pairing = pairing;
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

    /** The Bridge has not been paired, so there is no Bridge to call yet. */
    public static final class BridgeNotPairedException extends IOException {
        private static final long serialVersionUID = 1L;

        BridgeNotPairedException() {
            super("The Analyzer Bridge is not paired with OpenELIS");
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
     * success — so error responses are returned, not thrown.
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
            HttpResponse<String> response = client().send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new BridgeResponse(response.statusCode(), response.body() != null ? response.body() : "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(method + " " + url + " interrupted", e);
        }
    }

    private synchronized HttpClient client() throws BridgeNotPairedException {
        Optional<AnalyzerBridgePairingService.PairedBridge> current = pairing.getPairedBridge();
        if (current.isEmpty()) {
            // Without this, the first calls after startup would race the scheduled pairing.
            pairing.pairWithConfiguredCode();
            current = pairing.getPairedBridge();
        }
        AnalyzerBridgePairingService.PairedBridge paired = current.orElseThrow(BridgeNotPairedException::new);
        if (client == null || !paired.bridgeCertificateSha256().equals(clientFingerprint)) {
            client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).sslContext(paired.sslContext()).build();
            clientFingerprint = paired.bridgeCertificateSha256();
        }
        return client;
    }
}
