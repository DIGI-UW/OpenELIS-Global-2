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

    public AnalyzerDeliveryIssueServiceImpl(BridgeOutboxClient outboxClient, AnalyzerService analyzerService) {
        this.outboxClient = outboxClient;
        this.analyzerService = analyzerService;
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
        // The Bridge records only OpenELIS's service account, so the person who asked
        // is recorded here.
        LogEvent.logInfo(getClass().getSimpleName(), "retry",
                "Analyzer delivery " + outboxEntryId + " retried by user " + actor);
    }

    @Override
    public void dismiss(String outboxEntryId, String actor) {
        outboxClient.dismiss(outboxEntryId);
        LogEvent.logInfo(getClass().getSimpleName(), "dismiss",
                "Analyzer delivery " + outboxEntryId + " dismissed by user " + actor);
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
