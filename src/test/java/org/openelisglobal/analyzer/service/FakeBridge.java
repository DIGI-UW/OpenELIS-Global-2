package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * An HTTPS server that pairs like the Analyzer Bridge: it requires a client
 * certificate, accepts one pairing code once, and then answers only the
 * certificate that paired.
 */
final class FakeBridge implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    final BridgeTls.Identity identity;
    final List<String> receivedServerFingerprints = Collections.synchronizedList(new ArrayList<>());
    private final HttpsServer server;
    private final String code;
    private volatile String pairedClient;
    private volatile String announcedFingerprint;

    FakeBridge(String code) throws Exception {
        this.code = code;
        this.identity = BridgeTls.generateIdentity();
        SSLContext context = BridgeTls.context(identity, new AnyClient());
        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters parameters = context.getDefaultSSLParameters();
                parameters.setNeedClientAuth(true);
                params.setSSLParameters(parameters);
            }
        });
        server.createContext("/pairing", this::pairing);
        server.createContext("/api/ping", this::ping);
        server.start();
    }

    String url() {
        return "https://127.0.0.1:" + server.getAddress().getPort();
    }

    String pairedClient() {
        return pairedClient;
    }

    /** Answers pairing with this fingerprint instead of its own certificate's. */
    void announce(String fingerprint) {
        announcedFingerprint = fingerprint;
    }

    private void pairing(HttpExchange exchange) throws IOException {
        JsonNode body = JSON.readTree(exchange.getRequestBody());
        if (pairedClient != null) {
            respond(exchange, 409, "{\"error\":\"pairing_closed\"}");
        } else if (!code.equals(body.path("code").asText())) {
            respond(exchange, 403, "{\"error\":\"wrong_code\"}");
        } else {
            pairedClient = client(exchange);
            if (body.hasNonNull("serverCertificateSha256")) {
                receivedServerFingerprints.add(body.get("serverCertificateSha256").asText());
            }
            String fingerprint = announcedFingerprint;
            try {
                if (fingerprint == null) {
                    fingerprint = BridgeTls.sha256(identity.certificate());
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
            respond(exchange, 200, "{\"paired\":true,\"bridgeCertificateSha256\":\"" + fingerprint + "\"}");
        }
    }

    private void ping(HttpExchange exchange) throws IOException {
        lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        respond(exchange, pairedClient != null && pairedClient.equals(client(exchange)) ? 200 : 401, "{}");
    }

    private volatile String lastAuthorization;

    String lastAuthorization() {
        return lastAuthorization;
    }

    private static String client(HttpExchange exchange) throws IOException {
        try {
            return BridgeTls
                    .sha256((X509Certificate) ((HttpsExchange) exchange).getSSLSession().getPeerCertificates()[0]);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static final class AnyClient extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            throw new UnsupportedOperationException();
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
