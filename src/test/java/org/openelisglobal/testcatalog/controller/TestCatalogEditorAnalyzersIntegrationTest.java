package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.AnalyzersResponse;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-949 M11 / OGC-959..960 — read-only Analyzers section, round-tripped
 * against a real DB. Verifies the reverse test→analyzers lookup, analyzer-name
 * resolution, the empty state, and the 404 guard.
 */
public class TestCatalogEditorAnalyzersIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95411L;
    private static final long TEST_ID_NOMAP = 95412L;
    private static final long ANALYZER_ID = 95413L;
    private static final long MAPPING_ID = 95416L;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private TestResultInterpretationService interpretationService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;

    @Autowired
    private org.openelisglobal.testcatalog.service.RangeCoverageValidationService coverageService;

    @Autowired
    private org.openelisglobal.testsamplehandling.service.TestSampleHandlingService handlingService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerService analyzerService;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private org.openelisglobal.testterminology.service.TestTerminologyMappingService terminologyService;

    @Autowired
    private org.openelisglobal.panel.service.PanelService panelService;

    @Autowired
    private org.openelisglobal.panelitem.service.PanelItemService panelItemService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "AnalyzersIT", "AnalyzersIT desc", UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID_NOMAP, "AnalyzersIT-nomap", "no mappings", UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.analyzer" + " (id, name, is_active, status, bridge_connection_id, last_updated)"
                        + " VALUES (?, ?, true, 'ACTIVE', ?, NOW())",
                ANALYZER_ID, "Cobalt 9000", "bridge-test-catalog-95413");
        jdbc.update("INSERT INTO clinlims.analyzer_mapping"
                + " (id, analyzer_id, revision_number, profile_id, profile_revision, profile_fingerprint,"
                + " mapping_fingerprint, created_by)" + " VALUES (?, ?, 1, 'test-catalog-fixture', 1, ?, ?, '1')",
                MAPPING_ID, ANALYZER_ID, "sha256:" + "a".repeat(64), "sha256:" + "b".repeat(64));
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = ? WHERE id = ?", MAPPING_ID, ANALYZER_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_mapping_test"
                + " (mapping_id, source_row_key, mapping_state, test_id, last_updated)"
                + " VALUES (?, ?, 'BOUND', ?, NOW())", MAPPING_ID, "Cobalt Glucose", TEST_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = NULL WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping_test WHERE mapping_id = ?", MAPPING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping WHERE id = ?", MAPPING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TEST_ID, TEST_ID_NOMAP);
    }

    @org.junit.Test
    public void getAnalyzers_listsMappedAnalyzerWithResolvedName() {
        ResponseEntity<AnalyzersResponse> resp = controller.getAnalyzers(String.valueOf(TEST_ID));
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().analyzers.size());
        TestCatalogEditorRestController.AnalyzerRow row = resp.getBody().analyzers.get(0);
        assertEquals(String.valueOf(ANALYZER_ID), row.analyzerId);
        assertEquals("Cobalt 9000", row.analyzerName);
        assertEquals("Cobalt Glucose", row.analyzerTestName);
    }

    @org.junit.Test
    public void getAnalyzers_emptyWhenTestHasNoMappings() {
        ResponseEntity<AnalyzersResponse> resp = controller.getAnalyzers(String.valueOf(TEST_ID_NOMAP));
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().analyzers.isEmpty());
    }

    @org.junit.Test
    public void getAnalyzers_unknownTestReturns404() {
        assertEquals(404, controller.getAnalyzers("99999999").getStatusCode().value());
    }
}
