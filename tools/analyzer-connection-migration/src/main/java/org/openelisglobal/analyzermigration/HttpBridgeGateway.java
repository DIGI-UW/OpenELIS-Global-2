package org.openelisglobal.analyzermigration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpBridgeGateway implements AnalyzerMigrationRun.BridgeGateway {

    private final String bridgeBaseUrl;
    private final MigrationHttpJsonClient http = new MigrationHttpJsonClient();

    public HttpBridgeGateway(String bridgeBaseUrl) {
        this.bridgeBaseUrl = normalizedBaseUrl(bridgeBaseUrl);
    }

    @Override
    public ObjectNode createConnection(ObjectNode request) {
        return http.request("POST", bridgeBaseUrl + "/api/connections", request, Map.of());
    }

    @Override
    public ObjectNode getConnection(String connectionId) {
        return http.request("GET", bridgeBaseUrl + "/api/connections/" + pathSegment(connectionId), null, Map.of());
    }

    private static String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String normalizedBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Bridge base URL is required");
        }
        return value.replaceAll("/+$", "");
    }
}
