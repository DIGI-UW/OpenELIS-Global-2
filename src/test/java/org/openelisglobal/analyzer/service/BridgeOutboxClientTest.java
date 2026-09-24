package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BridgeOutboxClientTest {

    private static final String BASE_URL = "https://bridge.example";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String RECEIPT_ID = "recv-v1:72468791ea1a5109de4fba64eb61ff380b1c49883acdfc492559fc331e55b674";

    @Mock
    private BridgeHttpClient httpClient;

    private BridgeOutboxClient client;

    @Before
    public void setUp() {
        client = new BridgeOutboxClient(httpClient, BASE_URL + "/");
    }

    @Test
    public void listsOneOutboxStateWithTheBridgeFilter() throws Exception {
        when(httpClient.get(eq(BASE_URL + "/admin/outbox?state=DMQ&limit=200"), eq(TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, """
                        {"limit":200,"offset":0,"count":1,"rows":[{"id":"ob-1","state":"DMQ","connectionId":"conn-7",
                        "failureReason":"OE_REJECTED","lastHttpStatus":422,"attempts":5}]}"""));

        List<JsonNode> rows = client.list("DMQ");

        assertEquals(1, rows.size());
        assertEquals("ob-1", rows.get(0).path("id").asText());
        assertEquals("OE_REJECTED", rows.get(0).path("failureReason").asText());
    }

    @Test
    public void retriesAndDismissesOneEntryById() throws Exception {
        when(httpClient.post(eq(BASE_URL + "/admin/outbox/" + RECEIPT_ID + "/retry"), eq("{}"), eq(TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "{\"id\":\"ob-1\",\"state\":\"PENDING\"}"));
        when(httpClient.post(eq(BASE_URL + "/admin/outbox/ob-2/dismiss"), eq("{}"), eq(TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "{\"id\":\"ob-2\"}"));

        client.retry(RECEIPT_ID);
        client.dismiss("ob-2");

        verify(httpClient).post(BASE_URL + "/admin/outbox/" + RECEIPT_ID + "/retry", "{}", TIMEOUT);
        verify(httpClient).post(BASE_URL + "/admin/outbox/ob-2/dismiss", "{}", TIMEOUT);
    }

    @Test
    public void reportsTheBridgeReasonWhenAnEntryCannotBeRetried() throws Exception {
        when(httpClient.post(eq(BASE_URL + "/admin/outbox/" + RECEIPT_ID + "/retry"), eq("{}"), eq(TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(409,
                        "{\"error\":\"not_dead_lettered\",\"id\":\"ob-1\",\"state\":\"DELIVERED\"}"));

        BridgeAnalyzerConnectionException failure = assertThrows(BridgeAnalyzerConnectionException.class,
                () -> client.retry(RECEIPT_ID));

        assertEquals("analyzer.deliveryIssues.error.bridgeRefused", failure.messageKey());
        assertEquals("not_dead_lettered", failure.messageArgs().get("reason"));
        assertEquals(409, failure.messageArgs().get("status"));
    }

    @Test
    public void reportsAnUnreachableBridge() throws Exception {
        when(httpClient.get(eq(BASE_URL + "/admin/outbox?state=RETRYING&limit=200"), eq(TIMEOUT)))
                .thenThrow(new IOException("Connection refused"));

        BridgeAnalyzerConnectionException failure = assertThrows(BridgeAnalyzerConnectionException.class,
                () -> client.list("RETRYING"));

        assertEquals("analyzer.deliveryIssues.error.bridgeUnreachable", failure.messageKey());
    }

    @Test
    public void rejectsEntryIdsThatWouldChangeThePath() {
        assertThrows(IllegalArgumentException.class, () -> client.retry("../stats"));
        assertThrows(IllegalArgumentException.class, () -> client.retry("recv-v1:abc/../../stats"));
        assertThrows(IllegalArgumentException.class, () -> client.dismiss("ob-1?x=1"));
    }
}
