package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/**
 * Covers the join of Bridge outbox rows to OpenELIS analyzers. The Bridge HTTP
 * contract is covered by BridgeOutboxClientTest and the assembled path by the
 * analyzer harness delivery-issues story.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerDeliveryIssueServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private BridgeOutboxClient outboxClient;

    @Mock
    private AnalyzerService analyzerService;

    private AnalyzerDeliveryIssueService service;

    @Before
    public void setUp() {
        service = new AnalyzerDeliveryIssueServiceImpl(outboxClient, analyzerService);
    }

    @Test
    public void namesTheOpenElisAnalyzerAndOffersActionsOnlyForDeadLetteredRows() throws Exception {
        Analyzer genexpert = analyzer("12", "GeneXpert bench 1", "conn-7");
        when(analyzerService.getAllWithBindings()).thenReturn(List.of(genexpert));
        when(outboxClient.list("DMQ")).thenReturn(List.of(row("""
                {"id":"ob-1","state":"DMQ","connectionId":"conn-7","sourceId":"10.1.2.3","protocol":"ASTM",
                "accession":"DEV0126100001","attempts":5,"receivedAt":"2026-09-24T01:00:00Z",
                "failureReason":"OE_REJECTED","lastHttpStatus":422,"lastError":"Unknown test code"}""")));
        when(outboxClient.list("RETRYING")).thenReturn(List.of(row("""
                {"id":"ob-2","state":"RETRYING","connectionId":"conn-7","attempts":2,
                "receivedAt":"2026-09-24T02:00:00Z","lastHttpStatus":503}""")));

        List<AnalyzerDeliveryIssue> issues = service.getOpenIssues();

        assertEquals(2, issues.size());
        AnalyzerDeliveryIssue deadLettered = issues.get(0);
        assertEquals("ob-1", deadLettered.id());
        assertEquals("12", deadLettered.analyzerId());
        assertEquals("GeneXpert bench 1", deadLettered.analyzerName());
        assertEquals("OE_REJECTED", deadLettered.failureReason());
        assertEquals(Integer.valueOf(422), deadLettered.lastHttpStatus());
        assertEquals("Unknown test code", deadLettered.lastError());
        assertTrue(deadLettered.actionable());
        AnalyzerDeliveryIssue retrying = issues.get(1);
        assertEquals("RETRYING", retrying.state());
        assertEquals("GeneXpert bench 1", retrying.analyzerName());
        assertFalse(retrying.actionable());
    }

    @Test
    public void keepsRowsFromUnrecognizedSendersWithoutAnAnalyzer() throws Exception {
        when(analyzerService.getAllWithBindings()).thenReturn(List.of(analyzer("12", "GeneXpert bench 1", "conn-7")));
        when(outboxClient.list("DMQ")).thenReturn(List.of(row("""
                {"id":"ob-9","state":"DMQ","sourceId":"10.9.9.9","failureReason":"UNREGISTERED_SOURCE",
                "attempts":0,"receivedAt":"2026-09-24T03:00:00Z"}""")));
        when(outboxClient.list("RETRYING")).thenReturn(List.of());

        List<AnalyzerDeliveryIssue> issues = service.getOpenIssues();

        assertEquals(1, issues.size());
        assertNull(issues.get(0).analyzerId());
        assertNull(issues.get(0).analyzerName());
        assertEquals("10.9.9.9", issues.get(0).sourceId());
        assertEquals("UNREGISTERED_SOURCE", issues.get(0).failureReason());
    }

    @Test
    public void retriesAndDismissesThroughTheBridge() {
        service.retry("ob-1", "17");
        service.dismiss("ob-2", "17");

        verify(outboxClient).retry("ob-1");
        verify(outboxClient).dismiss("ob-2");
    }

    private static Analyzer analyzer(String id, String name, String connectionId) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName(name);
        analyzer.setBridgeConnectionId(connectionId);
        return analyzer;
    }

    private static JsonNode row(String json) throws Exception {
        return JSON.readTree(json);
    }
}
