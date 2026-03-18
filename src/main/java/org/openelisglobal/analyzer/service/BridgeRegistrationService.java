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

    @Value("${analyzer.bridge.url:}")
    private String bridgeBaseUrl;

    private final HttpClient httpClient;

    public BridgeRegistrationService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** Register a TCP analyzer (ASTM/HL7) with the bridge. */
    public boolean registerTcp(String oeAnalyzerId, String name, String ip, Integer port, String protocol) {
        if (!isBridgeConfigured()) {
            return false;
        }

        String sourceId = ip;
        String json = String.format("{\"oeAnalyzerId\":\"%s\",\"sourceId\":\"%s\",\"name\":\"%s\",\"protocol\":\"%s\"}",
                oeAnalyzerId, sourceId, escapeJson(name), protocol != null ? protocol : "ASTM");

        return callRegister(json, oeAnalyzerId);
    }

    /** Register a FILE analyzer with the bridge. */
    public boolean registerFile(String oeAnalyzerId, String name, String watchDir, String filePattern) {
        if (!isBridgeConfigured()) {
            return false;
        }

        String json = String.format(
                "{\"oeAnalyzerId\":\"%s\",\"sourceId\":\"%s\",\"name\":\"%s\",\"protocol\":\"FILE\",\"filePattern\":\"%s\"}",
                oeAnalyzerId, escapeJson(watchDir), escapeJson(name),
                escapeJson(filePattern != null ? filePattern : ""));

        return callRegister(json, oeAnalyzerId);
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

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
