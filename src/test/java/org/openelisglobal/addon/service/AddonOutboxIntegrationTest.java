package org.openelisglobal.addon.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.addon.AddonTestPackages;
import org.openelisglobal.addon.AddonTestPackages.Publisher;
import org.openelisglobal.addon.dao.AddonOutboxDAO;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.security.WebhookSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Events end to end on a real database: the Liquibase triggers write the
 * outbox, the fan-out queues one delivery per subscribed add-on, the dispatcher
 * POSTs signed CloudEvents to a local HTTP server, retries, and drops what a
 * disabled add-on would have received. The service key reads only orders its
 * add-on was told about.
 */
public class AddonOutboxIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ADMIN = "1";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private AddonRegistryService registry;
    @Autowired
    private AddonTokenService tokens;
    @Autowired
    private AddonDataService data;
    @Autowired
    private AddonOutboxDAO outboxDAO;
    @Autowired
    private AddonEventDispatcher dispatcher;
    @Autowired
    private AddonSettings settings;

    private final Publisher example = new Publisher("example");
    private String originalTrust;
    private HttpServer server;
    private final List<Map<String, String>> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger answer = new AtomicInteger(204);

    @Before
    public void setUp() throws IOException {
        originalTrust = (String) ReflectionTestUtils.getField(settings, "trustedPublishers");
        ReflectionTestUtils.setField(settings, "trustedPublishers", example.trustSetting());
        cleanUp();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/events", exchange -> {
            received.add(Map.of("id", exchange.getRequestHeaders().getFirst("webhook-id"), "timestamp",
                    exchange.getRequestHeaders().getFirst("webhook-timestamp"), "signature",
                    exchange.getRequestHeaders().getFirst("webhook-signature"), "contentType",
                    exchange.getRequestHeaders().getFirst("Content-Type"), "body",
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            exchange.sendResponseHeaders(answer.get(), -1);
            exchange.close();
        });
        server.start();
    }

    @After
    public void tearDown() {
        server.stop(0);
        ReflectionTestUtils.setField(settings, "trustedPublishers", originalTrust);
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM clinlims.addon_event");
        jdbcTemplate.update("DELETE FROM clinlims.addon WHERE id LIKE 'obx-%'");
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 1 WHERE status_id = 990");
        jdbcTemplate.update("DELETE FROM clinlims.status_of_sample WHERE id = 990");
    }

    /** Installs, points at the local server, enables. Returns the credentials. */
    private AddonRegistryService.Credentials enabledAddon(String id) {
        AddonRegistryService.Credentials credentials = registry
                .install(AddonTestPackages.signedPackage(example, id), ADMIN).credentials();
        registry.setEndpoint(id, "http://127.0.0.1:" + server.getAddress().getPort() + "/events");
        registry.setEnabled(id, true);
        return credentials;
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    @Test
    public void triggersWriteEventsOnlyWhileAnEnabledAddonSubscribes() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
        assertEquals("no add-on, no event", 0, count("SELECT count(*) FROM clinlims.addon_event"));

        enabledAddon("obx-trigger");
        executeDataSetWithStateManagement("testdata/analysis.xml");
        assertEquals(1, count("SELECT count(*) FROM clinlims.addon_event WHERE type = 'order.created'"
                + " AND accession_number = '12345'"));

        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, lastupdated,"
                + " name, display_key, is_active) VALUES (990, 'Finalized', 9, 'ANALYSIS', now(), 'Finalized',"
                + " 'status.finalized', 'Y')");
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 990 WHERE id = 1");
        String data = jdbcTemplate
                .queryForObject("SELECT data FROM clinlims.addon_event WHERE type = 'result.validated'", String.class);
        JsonNode event = JSON.readTree(data);
        assertEquals("12345", event.get("accessionNumber").asText());
        assertEquals("1", event.get("analysisId").asText());

        // the same status again is not a new validation
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 990 WHERE id = 1");
        assertEquals(1, count("SELECT count(*) FROM clinlims.addon_event WHERE type = 'result.validated'"));

        registry.setEnabled("obx-trigger", false);
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 990 WHERE id = 2");
        assertEquals("disabled add-on, no event", 1,
                count("SELECT count(*) FROM clinlims.addon_event WHERE type = 'result.validated'"));
    }

    @Test
    public void theDispatcherPostsASignedCloudEvent() throws Exception {
        AddonRegistryService.Credentials credentials = enabledAddon("obx-send");
        outboxDAO.emit("result.validated", "DEV0001", "{\"accessionNumber\":\"DEV0001\",\"analysisId\":\"7\"}");

        dispatcher.runOnce();

        assertEquals(1, received.size());
        Map<String, String> request = received.get(0);
        assertEquals("application/cloudevents+json", request.get("contentType"));
        assertEquals(WebhookSignature.sign(credentials.webhookSecret(), request.get("id"),
                Long.parseLong(request.get("timestamp")), request.get("body")), request.get("signature"));
        JsonNode body = JSON.readTree(request.get("body"));
        assertEquals("result.validated", body.get("type").asText());
        assertEquals("DEV0001", body.get("data").get("accessionNumber").asText());
        assertEquals(Map.of("DELIVERED", 1), registry.get("obx-send").orElseThrow().deliveries());

        dispatcher.runOnce();
        assertEquals("delivered once", 1, received.size());
    }

    @Test
    public void aFailedDeliveryIsRetriedLater() {
        enabledAddon("obx-retry");
        answer.set(500);
        outboxDAO.emit("order.created", "DEV0002", "{\"accessionNumber\":\"DEV0002\"}");

        dispatcher.runOnce();
        dispatcher.runOnce();

        assertEquals("not retried before its backoff", 1, received.size());
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, attempts, last_status_code,"
                + " next_attempt_at > now() AS later FROM clinlims.addon_delivery WHERE addon_id = 'obx-retry'");
        assertEquals("PENDING", row.get("status"));
        assertEquals(1, ((Number) row.get("attempts")).intValue());
        assertEquals(500, ((Number) row.get("last_status_code")).intValue());
        assertEquals(Boolean.TRUE, row.get("later"));

        answer.set(200);
        jdbcTemplate.update("UPDATE clinlims.addon_delivery SET next_attempt_at = now() WHERE addon_id = 'obx-retry'");
        dispatcher.runOnce();
        assertEquals(2, received.size());
        assertEquals("DELIVERED", jdbcTemplate.queryForObject(
                "SELECT status FROM clinlims.addon_delivery WHERE addon_id = 'obx-retry'", String.class));
    }

    @Test
    public void disablingDropsWhatWasQueuedAndNothingIsSent() {
        enabledAddon("obx-drop");
        answer.set(500);
        outboxDAO.emit("order.created", "DEV0003", "{\"accessionNumber\":\"DEV0003\"}");
        dispatcher.runOnce();
        assertEquals(1, received.size());

        registry.setEnabled("obx-drop", false);
        jdbcTemplate.update("UPDATE clinlims.addon_delivery SET next_attempt_at = now() WHERE addon_id = 'obx-drop'");
        answer.set(200);
        dispatcher.runOnce();

        assertEquals("nothing sent after disable", 1, received.size());
        assertEquals("DROPPED", jdbcTemplate.queryForObject(
                "SELECT status FROM clinlims.addon_delivery WHERE addon_id = 'obx-drop'", String.class));
    }

    @Test
    public void eachSubscribedAddonGetsItsOwnDelivery() {
        enabledAddon("obx-a");
        enabledAddon("obx-b");
        outboxDAO.emit("order.created", "DEV0004", "{\"accessionNumber\":\"DEV0004\"}");
        dispatcher.runOnce();
        assertEquals(2, received.size());
        assertEquals(1, count("SELECT count(*) FROM clinlims.addon_event WHERE fanned"));
    }

    @Test
    public void theServiceKeyReadsOnlyOrdersItsAddonWasToldAbout() {
        String apiKey = enabledAddon("obx-key").apiKey();
        AddonToken.Claims service = tokens.authenticate(apiKey);
        assertFalse(data.mayRead(service, "DEV0005"));

        outboxDAO.emit("order.created", "DEV0005", "{\"accessionNumber\":\"DEV0005\"}");
        dispatcher.runOnce();

        assertTrue(data.mayRead(service, "DEV0005"));
        assertFalse(data.mayRead(service, "DEV0006"));
        assertFalse(data.mayRead(service, "bad accession!"));
    }
}
