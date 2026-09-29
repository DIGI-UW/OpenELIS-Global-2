package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads and acts on the Bridge's outbox of results not yet delivered to
 * OpenELIS.
 */
@Service
public class BridgeOutboxClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    // The Bridge caps one page at 1000 rows; MAX_PAGES bounds the loop if a
    // Bridge ever ignores the offset and keeps returning full pages.
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 10;
    // Bridge receipt IDs look like recv-v1:<sha256>; anything else that could
    // change the request path is refused.
    private static final Pattern ENTRY_ID = Pattern.compile("^[A-Za-z0-9_:-]+$");

    private final BridgeHttpClient httpClient;
    private final String bridgeBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BridgeOutboxClient(BridgeHttpClient httpClient, @Value("${analyzer.bridge.url:}") String bridgeBaseUrl) {
        this.httpClient = httpClient;
        this.bridgeBaseUrl = bridgeBaseUrl == null ? "" : bridgeBaseUrl.trim().replaceAll("/+$", "");
    }

    public List<JsonNode> list(String state) {
        List<JsonNode> rows = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            JsonNode body = send("GET",
                    outboxUrl() + "?state=" + state + "&limit=" + PAGE_SIZE + "&offset=" + page * PAGE_SIZE);
            JsonNode pageRows = body.path("rows");
            pageRows.forEach(rows::add);
            if (pageRows.size() < PAGE_SIZE) {
                return rows;
            }
        }
        LogEvent.logWarn(getClass().getSimpleName(), "list",
                "Stopped reading the Bridge outbox for state " + state + " after " + MAX_PAGES * PAGE_SIZE + " rows");
        return rows;
    }

    public void retry(String outboxEntryId) {
        send("POST", entryUrl(outboxEntryId) + "/retry");
    }

    public void dismiss(String outboxEntryId) {
        send("POST", entryUrl(outboxEntryId) + "/dismiss");
    }

    private JsonNode send(String method, String url) {
        if (bridgeBaseUrl.isEmpty()) {
            throw new BridgeAnalyzerConnectionException("analyzer.deliveryIssues.error.bridgeNotConfigured");
        }
        BridgeHttpClient.BridgeResponse response;
        try {
            response = "GET".equals(method) ? httpClient.get(url, REQUEST_TIMEOUT)
                    : httpClient.post(url, "{}", REQUEST_TIMEOUT);
        } catch (IOException exception) {
            throw new BridgeAnalyzerConnectionException("analyzer.deliveryIssues.error.bridgeUnreachable",
                    Map.of("detail", String.valueOf(exception.getMessage())), exception);
        }
        JsonNode body = parse(response.body);
        if (!response.isSuccess()) {
            throw new BridgeAnalyzerConnectionException("analyzer.deliveryIssues.error.bridgeRefused",
                    Map.of("status", response.status, "reason", body.path("error").asText("")));
        }
        return body;
    }

    private JsonNode parse(String body) {
        try {
            return body == null || body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        } catch (IOException exception) {
            throw new BridgeAnalyzerConnectionException("analyzer.deliveryIssues.error.bridgeInvalidResponse", Map.of(),
                    exception);
        }
    }

    private String outboxUrl() {
        return bridgeBaseUrl + "/admin/outbox";
    }

    private String entryUrl(String outboxEntryId) {
        if (outboxEntryId == null || !ENTRY_ID.matcher(outboxEntryId).matches()) {
            throw new IllegalArgumentException("Invalid outbox entry ID");
        }
        return outboxUrl() + "/" + outboxEntryId;
    }
}
