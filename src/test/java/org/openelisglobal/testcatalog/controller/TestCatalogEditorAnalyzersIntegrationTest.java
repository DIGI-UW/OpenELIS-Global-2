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
    private static final long ANALYZER_TYPE_ID = 95414L;

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
    private org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;

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
                analyzerTestMappingService, typeOfSampleService, typeOfSampleTestService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "AnalyzersIT", "AnalyzersIT desc", UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID_NOMAP, "AnalyzersIT-nomap", "no mappings", UUID.randomUUID().toString());
        // Column set mirrors testdata/analyzer-test-mapping.xml (proven sufficient).
        jdbc.update("INSERT INTO clinlims.analyzer_type (id, name, plugin_class_name) VALUES (?, ?, ?)",
                ANALYZER_TYPE_ID, "AnalyzersIT Type", "oe.plugin.analyzer.AnalyzersIT");
        jdbc.update(
                "INSERT INTO clinlims.analyzer (id, name, analyzer_type, analyzer_type_id, description, location,"
                        + " is_active, has_setup_page, last_updated)"
                        + " VALUES (?, ?, 'CHEMISTRY', ?, ?, ?, true, false, NOW())",
                ANALYZER_ID, "Cobalt 9000", ANALYZER_TYPE_ID, "AnalyzersIT analyzer", "Lab A");
        jdbc.update("INSERT INTO clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, last_updated)"
                + " VALUES (?, ?, ?, NOW())", ANALYZER_ID, "Cobalt Glucose", TEST_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.analyzer_test_map WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_type WHERE id = ?", ANALYZER_TYPE_ID);
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
