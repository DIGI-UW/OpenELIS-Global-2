package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Proves setup verification persists through the real audit writer and
 * Liquibase-registered history table. The unit test covers fingerprint and
 * serialization logic; this guard owns audit durability.
 */
public class AnalyzerSetupVerificationAuditIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long ANALYZER_ID = 95401L;
    private static final long TEST_ID = 995401L;
    private static final String RULE_ID = "95401000-0000-0000-0000-000000000001";
    private static final String LOT_ID = "95401000-0000-0000-0000-000000000002";

    @Autowired
    private AnalyzerSetupVerificationService analyzerSetupVerificationService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private org.openelisglobal.history.service.HistoryService historyService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private AnalyzerSetupVerificationServiceImpl serviceTarget;
    private AuditTrailService originalAuditTrailService;

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        ensureSystemUser();

        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, 'Verification Audit Test', 'Verification Audit Test', 'Y', ?, NOW())",
                TEST_ID, "99540100-0000-0000-0000-000000000001");
        jdbc.update(
                "INSERT INTO clinlims.analyzer (id, name, analyzer_type, is_active, status, last_updated)"
                        + " VALUES (?, 'Verification Audit Analyzer', 'MOLECULAR', true, 'ACTIVE', NOW())",
                ANALYZER_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_test_map"
                + " (analyzer_id, analyzer_test_name, test_id, last_updated)" + " VALUES (?, 'AUDIT-CODE', ?, NOW())",
                ANALYZER_ID, TEST_ID);
        jdbc.update(
                "INSERT INTO clinlims.analyzer_qc_rule"
                        + " (id, analyzer_id, rule_type, target_field, operand, is_active, display_order, sys_user_id,"
                        + " last_updated) VALUES (?, ?, 'FIELD_EQUALS', 'Q.3', 'QC', true, 0, '1', NOW())",
                RULE_ID, ANALYZER_ID);
        jdbc.update("INSERT INTO clinlims.qc_control_lot"
                + " (id, product_name, lot_number, control_level, test_id, instrument_id, calculation_method,"
                + " initial_runs_count, status, sys_user_id, last_updated)"
                + " VALUES (?, 'Verification Control', 'AUDIT-LOT', 'L1', ?, ?, 'INITIAL_RUNS', 20,"
                + " 'ACTIVE', 1, NOW())", LOT_ID, TEST_ID, ANALYZER_ID);

        serviceTarget = (AnalyzerSetupVerificationServiceImpl) AopTestUtils
                .getUltimateTargetObject(analyzerSetupVerificationService);
        originalAuditTrailService = (AuditTrailService) ReflectionTestUtils.getField(serviceTarget,
                "auditTrailService");
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);
        ReflectionTestUtils.setField(serviceTarget, "auditTrailService", realAuditTrailService);
    }

    @After
    public void tearDown() {
        if (serviceTarget != null && originalAuditTrailService != null) {
            ReflectionTestUtils.setField(serviceTarget, "auditTrailService", originalAuditTrailService);
        }
        cleanup();
    }

    @Test
    public void verifySetupPersistsAttributedConfigHistory() {
        Map<String, Object> before = analyzerSetupVerificationService
                .getVerificationStatus(String.valueOf(ANALYZER_ID));

        Map<String, Object> verified = analyzerSetupVerificationService.verifySetup(String.valueOf(ANALYZER_ID),
                Map.of("mappingIds", before.get("mappingIds"), "qcIds", before.get("qcIds"), "mappingFingerprint",
                        before.get("mappingFingerprint"), "qcFingerprint", before.get("qcFingerprint")),
                TEST_SYS_USER_ID);

        assertTrue((Boolean) verified.get("currentlyVerified"));
        assertEquals("Y", jdbc.queryForObject(
                "SELECT keep_history FROM clinlims.reference_tables" + " WHERE LOWER(name) = 'analyzer_plugin_config'",
                String.class));
        List<Map<String, Object>> historyRows = jdbc
                .queryForList("SELECT h.activity, h.sys_user_id, convert_from(h.changes, 'UTF8') AS changes"
                        + " FROM clinlims.history h JOIN clinlims.reference_tables rt" + " ON rt.id = h.reference_table"
                        + " WHERE h.reference_id = ? AND LOWER(rt.name) = 'analyzer_plugin_config'", ANALYZER_ID);
        assertFalse("Expected durable setup-verification history", historyRows.isEmpty());
        assertTrue("Expected attributed config update history, got: " + historyRows,
                historyRows.stream()
                        .anyMatch(row -> "U".equals(row.get("activity"))
                                && TEST_SYS_USER_ID.equals(String.valueOf(row.get("sys_user_id")))
                                && String.valueOf(row.get("changes")).contains("<config>")));
    }

    private void ensureSystemUser() {
        jdbc.update("INSERT INTO clinlims.system_user"
                + " (id, login_name, last_name, first_name, is_active, is_employee, external_id, lastupdated)"
                + " VALUES (1, 'admin', 'admin', 'admin', 'Y', 'Y', 'admin', NOW())" + " ON CONFLICT (id) DO NOTHING");
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update(
                "DELETE FROM clinlims.history WHERE reference_id = ? AND reference_table IN"
                        + " (SELECT id FROM clinlims.reference_tables WHERE LOWER(name) = 'analyzer_plugin_config')",
                ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_plugin_config WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.qc_control_lot WHERE id = ?", LOT_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_qc_rule WHERE id = ?", RULE_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_test_map WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }
}
