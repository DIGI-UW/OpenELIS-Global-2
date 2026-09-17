package org.openelisglobal.analyzermigration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpOpenElisGateway implements AnalyzerMigrationRun.OpenElisGateway {

    private final String openElisBaseUrl;
    private final Map<String, String> headers;
    private final MigrationHttpJsonClient http = new MigrationHttpJsonClient();

    public HttpOpenElisGateway(String openElisBaseUrl, String sessionCookie) {
        this.openElisBaseUrl = normalizedBaseUrl(openElisBaseUrl);
        if (sessionCookie == null || sessionCookie.isBlank()) {
            throw new IllegalArgumentException("OpenELIS session cookie is required");
        }
        this.headers = Map.of("Cookie", sessionCookie.trim());
    }

    @Override
    public void attachBridgeConnection(String sourceAnalyzerId, ObjectNode profileRef, String bridgeConnectionId,
            String actor) {
        ObjectNode request = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        request.put("profileId", profileRef.path("profileId").asText());
        request.put("profileRevision", profileRef.path("revision").asInt());
        request.put("profileFingerprint", profileRef.path("fingerprint").asText());
        request.put("bridgeConnectionId", bridgeConnectionId);
        http.request("PUT", referenceEndpoint(sourceAnalyzerId), request, headers);
    }

    @Override
    public ObjectNode getAnalyzerReference(String sourceAnalyzerId) {
        return http.request("GET", referenceEndpoint(sourceAnalyzerId), null, headers);
    }

    private String referenceEndpoint(String analyzerId) {
        return openElisBaseUrl + "/rest/analyzer/migration/analyzers/" + pathSegment(analyzerId) + "/reference";
    }

    private static String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String normalizedBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OpenELIS base URL is required");
        }
        return value.replaceAll("/+$", "");
    }
}
