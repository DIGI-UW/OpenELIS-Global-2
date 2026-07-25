package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Proves pending-code resolution through the real catalog, ORM, and transaction
 * path. Only the bridge HTTP boundary is mocked; bridge payload contracts are
 * covered separately.
 */
public class AnalyzerPendingCodeServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long ANALYZER_ID = 95201L;
    private static final long CATALOG_TEST_ID = 995201L;
    private static final long CATALOG_ANALYTE_ID = 995201L;
    private static final long TEST_ANALYTE_ID = 995201L;
    private static final String PENDING_CODE_ID = "95201000-0000-0000-0000-000000000001";

    @Autowired
    private AnalyzerPendingCodeService analyzerPendingCodeService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private AnalyzerPendingCodeServiceImpl serviceTarget;
    private AnalyzerBridgeSyncService originalBridgeSyncService;
    private AnalyzerBridgeSyncService bridgeSyncService;

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, 'Pending Code Catalog Test', 'Pending Code Catalog Test', 'Y', ?, NOW())",
                CATALOG_TEST_ID, "99520100-0000-0000-0000-000000000001");
        jdbc.update(
                "INSERT INTO clinlims.analyte"
                        + " (id, analyte_id, name, is_active, external_id, local_abbrev, lastupdated)"
                        + " VALUES (?, ?, 'Pending Code Catalog Analyte', 'Y', 'PC-CAT', 'PCCAT', NOW())",
                CATALOG_ANALYTE_ID, CATALOG_ANALYTE_ID);
        jdbc.update("INSERT INTO clinlims.test_analyte"
                + " (id, test_id, analyte_id, result_group, sort_order, testalyt_type, is_reportable, lastupdated)"
                + " VALUES (?, ?, ?, 1, 1, 'T', 'Y', NOW())", TEST_ANALYTE_ID, CATALOG_TEST_ID, CATALOG_ANALYTE_ID);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, analyzer_type, is_active, last_updated)"
                + " VALUES (?, ?, ?, true, NOW())", ANALYZER_ID, "Pending Code Catalog Analyzer", "MOLECULAR");
        jdbc.update("INSERT INTO clinlims.analyzer_pending_code"
                + " (id, analyzer_id, analyzer_test_name, first_seen_at, last_seen_at, seen_count, status,"
                + " sys_user_id, last_updated) VALUES (?, ?, 'NEW-MTB-CODE', NOW(), NOW(), 1, 'PENDING', '1', NOW())",
                PENDING_CODE_ID, ANALYZER_ID);

        serviceTarget = (AnalyzerPendingCodeServiceImpl) AopTestUtils
                .getUltimateTargetObject(analyzerPendingCodeService);
        originalBridgeSyncService = (AnalyzerBridgeSyncService) ReflectionTestUtils.getField(serviceTarget,
                "analyzerBridgeSyncService");
        bridgeSyncService = mock(AnalyzerBridgeSyncService.class);
        ReflectionTestUtils.setField(serviceTarget, "analyzerBridgeSyncService", bridgeSyncService);
    }

    @After
    public void tearDown() {
        if (serviceTarget != null && originalBridgeSyncService != null) {
            ReflectionTestUtils.setField(serviceTarget, "analyzerBridgeSyncService", originalBridgeSyncService);
        }
        cleanup();
    }

    @Test
    public void resolvePersistsMappingAndMappedStateForConfiguredCatalogTest() {
        String openelisTestId = String.valueOf(CATALOG_TEST_ID);

        AnalyzerPendingCode resolved = analyzerPendingCodeService.resolve(String.valueOf(ANALYZER_ID), PENDING_CODE_ID,
                openelisTestId, TEST_SYS_USER_ID);

        assertEquals(AnalyzerPendingCode.Status.MAPPED, resolved.getStatus());
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM clinlims.analyzer_test_map"
                                + " WHERE analyzer_id = ? AND analyzer_test_name = 'NEW-MTB-CODE' AND test_id = ?",
                        Integer.class, ANALYZER_ID, Long.valueOf(openelisTestId)));
        assertEquals("MAPPED", jdbc.queryForObject("SELECT status FROM clinlims.analyzer_pending_code WHERE id = ?",
                String.class, PENDING_CODE_ID));
        verify(bridgeSyncService).pushAnalyzer(String.valueOf(ANALYZER_ID));
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.analyzer_test_map WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_pending_code WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE_ID);
        jdbc.update("DELETE FROM clinlims.analyte WHERE id = ?", CATALOG_ANALYTE_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", CATALOG_TEST_ID);
    }
}
