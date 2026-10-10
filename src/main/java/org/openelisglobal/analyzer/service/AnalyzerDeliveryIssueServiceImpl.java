package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerDeliveryIssueServiceImpl implements AnalyzerDeliveryIssueService {

    private static final String DEAD_LETTERED = "DMQ";
    private static final String RETRYING = "RETRYING";

    private final BridgeOutboxClient outboxClient;
    private final AnalyzerService analyzerService;
    private final AnalyzerDeliveryActionService deliveryActionService;

    public AnalyzerDeliveryIssueServiceImpl(BridgeOutboxClient outboxClient, AnalyzerService analyzerService,
            AnalyzerDeliveryActionService deliveryActionService) {
        this.outboxClient = outboxClient;
        this.analyzerService = analyzerService;
        this.deliveryActionService = deliveryActionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerDeliveryIssue> getOpenIssues() {
        Map<String, Analyzer> byConnection = analyzerService.getAllWithBindings().stream()
                .filter(analyzer -> analyzer.getBridgeConnectionId() != null).collect(
                        Collectors.toMap(Analyzer::getBridgeConnectionId, Function.identity(), (first, next) -> first));
        List<AnalyzerDeliveryIssue> issues = new ArrayList<>();
        outboxClient.list(DEAD_LETTERED).forEach(row -> issues.add(toIssue(row, byConnection)));
        outboxClient.list(RETRYING).forEach(row -> issues.add(toIssue(row, byConnection)));
        return issues;
    }

    @Override
    public void retry(String outboxEntryId, String actor) {
        outboxClient.retry(outboxEntryId);
        LogEvent.logInfo(getClass().getSimpleName(), "retry",
                "Analyzer delivery " + outboxEntryId + " retried by user " + actor);
        // The Bridge records only OpenELIS's service account, so the person who asked
        // is recorded here.
        deliveryActionService.retain(outboxEntryId, AnalyzerDeliveryActionService.RETRY,
                resolveAnalyzerId(outboxEntryId), actor);
    }

    @Override
    public void dismiss(String outboxEntryId, String actor) {
        outboxClient.dismiss(outboxEntryId);
        LogEvent.logInfo(getClass().getSimpleName(), "dismiss",
                "Analyzer delivery " + outboxEntryId + " dismissed by user " + actor);
        deliveryActionService.retain(outboxEntryId, AnalyzerDeliveryActionService.DISMISS,
                resolveAnalyzerId(outboxEntryId), actor);
    }

    /**
     * The Bridge action endpoints do not echo the entry, so the analyzer is read
     * from the entry itself once the Bridge has accepted the action. Attribution of
     * the actor does not depend on this, so a failed lookup leaves the analyzer
     * unresolved rather than losing the record.
     */
    private String resolveAnalyzerId(String outboxEntryId) {
        try {
            String connectionId = text(outboxClient.get(outboxEntryId), "connectionId");
            return connectionId == null ? null
                    : analyzerService.findByBridgeConnectionId(connectionId).map(Analyzer::getId).orElse(null);
        } catch (RuntimeException exception) {
            LogEvent.logWarn(getClass().getSimpleName(), "resolveAnalyzerId",
                    "Cannot resolve the analyzer for delivery " + outboxEntryId + ": " + exception.getMessage());
        }
        return null;
    }

    private static AnalyzerDeliveryIssue toIssue(JsonNode row, Map<String, Analyzer> byConnection) {
        String connectionId = text(row, "connectionId");
        Analyzer analyzer = connectionId == null ? null : byConnection.get(connectionId);
        String state = text(row, "state");
        return new AnalyzerDeliveryIssue(text(row, "id"), state, analyzer == null ? null : analyzer.getId(),
                analyzer == null ? null : analyzer.getName(), connectionId, text(row, "sourceId"),
                text(row, "protocol"), text(row, "accession"), row.path("attempts").asInt(0), text(row, "receivedAt"),
                text(row, "failureReason"),
                row.path("lastHttpStatus").isNumber() ? row.path("lastHttpStatus").asInt() : null,
                text(row, "lastError"), DEAD_LETTERED.equals(state));
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row.path(field);
        return value.isNull() || value.isMissingNode() || value.asText().isBlank() ? null : value.asText();
    }
}
