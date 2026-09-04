package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
public class BridgeProfileManagementServiceImpl implements BridgeProfileManagementService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final BridgeHttpClient bridgeHttpClient;
    private final String profilesUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BridgeProfileManagementServiceImpl(BridgeHttpClient bridgeHttpClient,
            @Value("${analyzer.bridge.url:}") String bridgeUrl) {
        this.bridgeHttpClient = bridgeHttpClient;
        String normalizedBridgeUrl = stripTrailingSlashes(bridgeUrl);
        this.profilesUrl = normalizedBridgeUrl.isBlank() ? "" : normalizedBridgeUrl + "/api/profiles";
    }

    @Override
    public JsonNode createDraft(String displayName, String actor) {
        ObjectNode request = actorRequest(actor);
        request.put("displayName", requireText(displayName, "Display name"));
        return post(profilesUrl + "/drafts", request);
    }

    @Override
    public JsonNode getDraft(String draftId) {
        return get(draftUrl(draftId));
    }

    @Override
    public JsonNode updateDraft(String draftId, JsonNode profile, String actor) {
        requireProfile(profile);
        ObjectNode request = actorRequest(actor);
        request.set("profile", profile);
        return put(draftUrl(draftId), request);
    }

    @Override
    public JsonNode publishDraft(String draftId, String actor) {
        return post(draftUrl(draftId) + "/publish", actorRequest(actor));
    }

    @Override
    public JsonNode updateShared(String profileId, int sourceRevision, String actor) {
        if (sourceRevision < 1) {
            throw new BridgeProfileManagementException(400, "Source revision must be at least 1");
        }
        ObjectNode request = actorRequest(actor);
        request.put("sourceRevision", sourceRevision);
        return post(profileUrl(profileId) + "/update", request);
    }

    @Override
    public JsonNode duplicate(String profileId, int sourceRevision, String displayName, String actor) {
        if (sourceRevision < 1) {
            throw new BridgeProfileManagementException(400, "Source revision must be at least 1");
        }
        ObjectNode request = actorRequest(actor);
        request.put("sourceRevision", sourceRevision);
        request.put("displayName", requireText(displayName, "Display name"));
        return post(profileUrl(profileId) + "/duplicate", request);
    }

    @Override
    public JsonNode deactivate(String profileId, String actor) {
        return post(profileUrl(profileId) + "/deactivate", actorRequest(actor));
    }

    @Override
    public JsonNode reactivate(String profileId, String actor) {
        return post(profileUrl(profileId) + "/reactivate", actorRequest(actor));
    }

    @Override
    public JsonNode history(String profileId) {
        return get(profileUrl(profileId) + "/history");
    }

    private JsonNode get(String url) {
        requireConfigured();
        try {
            return parse(bridgeHttpClient.get(url, REQUEST_TIMEOUT));
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    private JsonNode post(String url, JsonNode body) {
        requireConfigured();
        try {
            return parse(bridgeHttpClient.post(url, body.toString(), REQUEST_TIMEOUT));
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    private JsonNode put(String url, JsonNode body) {
        requireConfigured();
        try {
            return parse(bridgeHttpClient.put(url, body.toString(), REQUEST_TIMEOUT));
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    private JsonNode parse(BridgeHttpClient.BridgeResponse response) {
        JsonNode body;
        try {
            body = objectMapper.readTree(response.body);
        } catch (IOException e) {
            throw new BridgeProfileManagementException(502, "Bridge returned an invalid profile response", e);
        }
        if (!response.isSuccess()) {
            String message = body.path("error").asText();
            throw new BridgeProfileManagementException(response.status,
                    message.isBlank() ? "Bridge profile request failed" : message);
        }
        return body;
    }

    private ObjectNode actorRequest(String actor) {
        return objectMapper.createObjectNode().put("actor", requireText(actor, "Actor"));
    }

    private String profileUrl(String profileId) {
        requireConfigured();
        String normalized = requireText(profileId, "Profile ID");
        return profilesUrl + "/" + UriUtils.encodePathSegment(normalized, StandardCharsets.UTF_8);
    }

    private String draftUrl(String draftId) {
        requireConfigured();
        String normalized = requireText(draftId, "Draft ID");
        return profilesUrl + "/drafts/" + UriUtils.encodePathSegment(normalized, StandardCharsets.UTF_8);
    }

    private void requireProfile(JsonNode profile) {
        if (profile == null || !profile.isObject()) {
            throw new BridgeProfileManagementException(400, "Profile is required");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BridgeProfileManagementException(400, fieldName + " is required");
        }
        return value.trim();
    }

    private BridgeProfileManagementException unavailable(IOException cause) {
        return new BridgeProfileManagementException(502, "Bridge profile service is unavailable", cause);
    }

    private void requireConfigured() {
        if (profilesUrl.isBlank()) {
            throw new BridgeProfileManagementException(503, "Bridge URL is not configured");
        }
    }

    private static String stripTrailingSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
