package org.openelisglobal.analyzer.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Bridge registration client for analyzer transport metadata. */
@Service
public class BridgeRegistrationService {

    private static final String CLASS_NAME = "BridgeRegistrationService";

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${analyzer.bridge.url:}")
    private String bridgeBaseUrl;

    private final HttpClient httpClient;

    public BridgeRegistrationService() {
        HttpClient client;
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
            client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).sslContext(sslContext).build();
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "BridgeRegistrationService",
                    "SSL context init failed, using default client: " + e.getMessage());
            client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        }
        this.httpClient = client;
    }

    /** Register a TCP analyzer (ASTM/HL7) with the bridge. */
    public boolean registerTcp(String oeAnalyzerId, String name, String ip, Integer port, String protocol) {
        if (!isBridgeConfigured()) {
            return false;
        }

        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("oeAnalyzerId", oeAnalyzerId);
            payload.put("sourceId", ip);
            payload.put("name", name);
            payload.put("protocol", protocol != null ? protocol : "ASTM");
            String json = objectMapper.writeValueAsString(payload);
            return callRegister(json, oeAnalyzerId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LogEvent.logError(CLASS_NAME, "registerTcp", "Failed to build registration JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Register a FILE analyzer with the bridge, including column mappings for FHIR
     * parsing.
     */
    public boolean registerFile(String oeAnalyzerId, String name, String watchDir, String filePattern,
            java.util.Map<String, String> columnMappings) {
        if (!isBridgeConfigured()) {
            return false;
        }

        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("oeAnalyzerId", oeAnalyzerId);
            payload.put("sourceId", watchDir);
            payload.put("name", name);
            payload.put("protocol", "FILE");
            payload.put("filePattern", filePattern != null ? filePattern : "");
            if (columnMappings != null && !columnMappings.isEmpty()) {
                payload.put("columnMappings", columnMappings);
            }
            String json = objectMapper.writeValueAsString(payload);
            return callRegister(json, oeAnalyzerId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LogEvent.logError(CLASS_NAME, "registerFile", "Failed to build registration JSON: " + e.getMessage());
            return false;
        }
    }

    /** Unregister an analyzer from the bridge. */
    public boolean unregister(String oeAnalyzerId) {
        if (!isBridgeConfigured()) {
            return false;
        }

        try {
            String endpoint = bridgeBaseUrl.replaceAll("/+$", "") + "/api/analyzers/" + oeAnalyzerId;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).DELETE()
                    .timeout(Duration.ofSeconds(10)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LogEvent.logInfo(CLASS_NAME, "unregister", "Unregistered analyzer " + oeAnalyzerId + " from bridge");
                return true;
            } else {
                LogEvent.logWarn(CLASS_NAME, "unregister",
                        "Bridge unregister returned " + response.statusCode() + " for analyzer " + oeAnalyzerId);
                return false;
            }
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "unregister",
                    "Failed to unregister analyzer " + oeAnalyzerId + " from bridge: " + e.getMessage());
            return false;
        }
    }

    private boolean callRegister(String json, String oeAnalyzerId) {
        try {
            String endpoint = bridgeBaseUrl.replaceAll("/+$", "") + "/api/analyzers/register";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LogEvent.logInfo(CLASS_NAME, "callRegister", "Registered analyzer " + oeAnalyzerId + " with bridge");
                return true;
            } else {
                LogEvent.logWarn(CLASS_NAME, "callRegister",
                        "Bridge register returned " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "callRegister",
                    "Failed to register analyzer " + oeAnalyzerId + " with bridge: " + e.getMessage());
            return false;
        }
    }

    private boolean isBridgeConfigured() {
        if (bridgeBaseUrl == null || bridgeBaseUrl.isBlank()) {
            LogEvent.logDebug(CLASS_NAME, "isBridgeConfigured",
                    "No analyzer.bridge.url configured — skipping bridge registration");
            return false;
        }
        return true;
    }

}
