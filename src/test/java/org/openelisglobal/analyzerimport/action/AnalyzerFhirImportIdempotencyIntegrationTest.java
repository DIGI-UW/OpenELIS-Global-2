package org.openelisglobal.analyzerimport.action;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The wire-level idempotency contract the Analyzer Bridge depends on.
 *
 * <p>
 * Bridge delivery is at-least-once: it keeps a received result until OpenELIS
 * accepts it, and it cannot tell an answer that was lost from a delivery that
 * failed, so it sends the result again. That is only safe if a repeated
 * delivery over HTTP is answered exactly as the first one was and creates
 * nothing new.
 *
 * <p>
 * The service-level tests alongside this one already prove the deduplication.
 * This one proves the same thing through the endpoint Bridge actually calls,
 * including the response body Bridge stores as its proof of delivery.
 */
public class AnalyzerFhirImportIdempotencyIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long MAPPING_ID = 98413L;
    private static final long ANALYZER_ID = 98414L;
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final Path FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures", "normalized-unknown-test.fhir.json");

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, bridge_connection_id, last_updated)"
                + " VALUES (?, 'Idempotency test analyzer', true, ?, NOW())", ANALYZER_ID, CONNECTION_ID);
        jdbc.update(
                "INSERT INTO clinlims.analyzer_mapping"
                        + " (id, analyzer_id, revision_number, profile_id, profile_revision, profile_fingerprint,"
                        + " mapping_fingerprint, created_by, created_at, last_updated)"
                        + " VALUES (?, ?, 1, 'site.unknown-capable', 3, ?, ?, '1', NOW(), NOW())",
                MAPPING_ID, ANALYZER_ID, "sha256:" + "3".repeat(64), "sha256:" + "4".repeat(64));
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = ? WHERE id = ?", MAPPING_ID, ANALYZER_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void aRepeatedDeliveryIsAnsweredIdenticallyAndCreatesNothingNew() throws Exception {
        String payload = Files.readString(FIXTURE);

        String first = deliver(payload);
        String second = deliver(payload);

        assertEquals("a redelivery must be answered exactly as the delivery it repeats, so the sender"
                + " can confirm rather than guess", first, second);
        assertEquals("a redelivery must not stage a second clinical result", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class, ANALYZER_ID));
        assertEquals("a redelivery must not record a second acceptance", Integer.valueOf(1),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?",
                        Integer.class, CONNECTION_ID));

        String storedReceipt = jdbc.queryForObject(
                "SELECT id FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?", String.class,
                CONNECTION_ID);
        assertEquals("the sender confirms delivery against the id of OpenELIS's durable acceptance record",
                storedReceipt, new ObjectMapper().readTree(first).path("receiptId").textValue());
    }

    private String deliver(String payload) throws Exception {
        return mockMvc.perform(post("/analyzer/fhir").contentType("application/fhir+json").content(payload))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = NULL WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping WHERE id = ?", MAPPING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
    }
}
