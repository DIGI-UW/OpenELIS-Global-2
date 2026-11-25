package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deleting an analyzer against the real schema: its own mappings go with it, a
 * reference elsewhere refuses.
 */
public class AnalyzerLifecycleIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long ANALYZER_ID = 98451L;
    private static final long MAPPING_ID = 98452L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private AnalyzerLifecycleService lifecycleService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, status, last_updated)"
                + " VALUES (?, 'Never used bench', false, 'SETUP', NOW())", ANALYZER_ID);
        jdbc.update(
                "INSERT INTO clinlims.analyzer_mapping"
                        + " (id, analyzer_id, revision_number, profile_id, profile_revision, profile_fingerprint,"
                        + " mapping_fingerprint, created_by, created_at, last_updated)"
                        + " VALUES (?, ?, 1, 'site.never-used', 1, ?, ?, '1', NOW(), NOW())",
                MAPPING_ID, ANALYZER_ID, "sha256:" + "1".repeat(64), "sha256:" + "2".repeat(64));
        jdbc.update("INSERT INTO clinlims.analyzer_mapping_test (mapping_id, source_row_key, mapping_state,"
                + " last_updated) VALUES (?, 'GLU', 'UNRESOLVED', NOW())", MAPPING_ID);
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = ? WHERE id = ?", MAPPING_ID, ANALYZER_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void anAnalyzerNeverUsedIsDeletedWithItsMappingRevisionsAndRows() {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> lifecycleService.delete(String.valueOf(ANALYZER_ID), "1"));

        assertEquals(Integer.valueOf(0), count("SELECT COUNT(*) FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID));
        assertEquals(Integer.valueOf(0),
                count("SELECT COUNT(*) FROM clinlims.analyzer_mapping WHERE analyzer_id = ?", ANALYZER_ID));
        assertEquals(Integer.valueOf(0),
                count("SELECT COUNT(*) FROM clinlims.analyzer_mapping_test WHERE mapping_id = ?", MAPPING_ID));
    }

    @Test
    public void anAnalyzerThatReceivedADeliveryIsKeptWhole() {
        jdbc.update("INSERT INTO clinlims.analyzer_delivery_receipt (id, connection_id, message_id, analyzer_id,"
                + " profile_id, profile_revision, results_staged, results_held, controls_processed, accepted_by,"
                + " accepted_at, bundle_json) VALUES ('receipt-98451', 'bridge-98451', 'msg-98451', ?,"
                + " 'site.never-used', 1, 0, 0, 0, '1', NOW(), '{}')", ANALYZER_ID);

        AnalyzerRequestException refused = assertThrows(AnalyzerRequestException.class,
                () -> new TransactionTemplate(transactionManager)
                        .executeWithoutResult(status -> lifecycleService.delete(String.valueOf(ANALYZER_ID), "1")));

        assertEquals("analyzer.lifecycle.error.inUse", refused.messageKey());
        assertEquals(Integer.valueOf(1), count("SELECT COUNT(*) FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID));
        assertEquals(Integer.valueOf(1),
                count("SELECT COUNT(*) FROM clinlims.analyzer_mapping_test WHERE mapping_id = ?", MAPPING_ID));
    }

    private Integer count(String sql, long id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = NULL WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping_test WHERE mapping_id = ?", MAPPING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
    }
}
