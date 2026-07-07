package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.MappingDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.TerminologyResponse;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M10 / OGC-957..958 — Terminology Mappings API, round-tripped against
 * a real DB. Exercises the new TestTerminologyMapping entity stack: the
 * load/save round-trip, in-request (source, code) dedupe → 422, the source enum
 * guard, soft-delete on removal, and — critically — that re-adding a removed
 * (source, code) reactivates its row instead of colliding with the
 * {@code (test_id, source, code)} unique constraint.
 */
public class TestCatalogEditorTerminologyIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95441L;

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
                analyzerTestMappingService, typeOfSampleService, typeOfSampleTestService, terminologyService,
                panelService, panelItemService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "TerminologyIT", "TerminologyIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private static MappingDto mapping(String source, String code, String relationship) {
        MappingDto m = new MappingDto();
        m.source = source;
        m.code = code;
        m.relationship = relationship;
        return m;
    }

    private TerminologyResponse put(MappingDto... mappings) {
        TerminologyResponse body = new TerminologyResponse();
        for (MappingDto m : mappings) {
            body.mappings.add(m);
        }
        return controller.saveTerminology(testId(), body, authedRequest()).getBody();
    }

    private Long rowCount(String source, String code) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test_terminology_mapping WHERE test_id = ? AND source = ? AND code = ?",
                Long.class, TEST_ID, source, code);
    }

    @org.junit.Test
    public void getTerminology_emptyWhenNoMappings() {
        ResponseEntity<TerminologyResponse> resp = controller.getTerminology(testId());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().mappings.isEmpty());
    }

    @org.junit.Test
    public void saveAndGet_roundTripsTheMapping() {
        put(mapping("LOINC", "1558-6", "SAME_AS"));
        TerminologyResponse loaded = controller.getTerminology(testId()).getBody();
        assertEquals(1, loaded.mappings.size());
        assertEquals("LOINC", loaded.mappings.get(0).source);
        assertEquals("1558-6", loaded.mappings.get(0).code);
        assertEquals("SAME_AS", loaded.mappings.get(0).relationship);
    }

    @org.junit.Test
    public void save_duplicateSourceCodeInRequestReturns422() {
        ResponseEntity<TerminologyResponse> resp = controller.saveTerminology(testId(), dup(), authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    private TerminologyResponse dup() {
        TerminologyResponse body = new TerminologyResponse();
        body.mappings.add(mapping("LOINC", "1558-6", "SAME_AS"));
        body.mappings.add(mapping("LOINC", "1558-6", "BROADER_THAN"));
        return body;
    }

    @org.junit.Test
    public void save_invalidSourceReturns422() {
        TerminologyResponse body = new TerminologyResponse();
        body.mappings.add(mapping("BOGUS", "X", null));
        assertEquals(422, controller.saveTerminology(testId(), body, authedRequest()).getStatusCode().value());
    }

    @org.junit.Test
    public void removingMapping_softDeletes_andReAddReactivatesWithoutCollision() {
        put(mapping("LOINC", "1558-6", "SAME_AS"));
        // Remove it (empty desired set) → soft-deleted, no longer returned.
        put();
        assertTrue(controller.getTerminology(testId()).getBody().mappings.isEmpty());
        // Re-add the same (source, code): must reactivate, not violate the unique key.
        put(mapping("LOINC", "1558-6", "NARROWER_THAN"));
        TerminologyResponse loaded = controller.getTerminology(testId()).getBody();
        assertEquals(1, loaded.mappings.size());
        assertEquals("NARROWER_THAN", loaded.mappings.get(0).relationship);
        // Exactly one physical row for (test, LOINC, 1558-6) — reactivated, not
        // duplicated.
        assertEquals(Long.valueOf(1L), rowCount("LOINC", "1558-6"));
    }

    private String legacyLoinc() {
        return jdbc.queryForObject("SELECT loinc FROM clinlims.test WHERE id = ?", String.class, TEST_ID);
    }

    @org.junit.Test
    public void saveTerminology_mirrorsLoincToLegacyTestColumn() {
        // New editor → legacy: the SAME_AS LOINC mapping populates test.loinc.
        put(mapping("LOINC", "1558-6", "SAME_AS"));
        assertEquals("1558-6", legacyLoinc());
        // Clearing the LOINC mapping clears the legacy column.
        put();
        assertEquals(null, legacyLoinc());
    }

    @org.junit.Test
    public void syncLegacyLoinc_mirrorsLegacyLoincIntoMappings_andLeavesOtherSourcesAlone() {
        // Seed a non-LOINC mapping that legacy LOINC edits must not disturb.
        put(mapping("SNOMED", "271649006", "SAME_AS"));

        // Legacy → new editor: a legacy LOINC edit surfaces as a LOINC/SAME_AS row.
        terminologyService.syncLegacyLoinc(testId(), "4548-4", "1");
        TerminologyResponse loaded = controller.getTerminology(testId()).getBody();
        assertEquals(2, loaded.mappings.size());
        MappingDto loinc = loaded.mappings.stream().filter(m -> "LOINC".equals(m.source)).findFirst().get();
        assertEquals("4548-4", loinc.code);
        assertEquals("SAME_AS", loinc.relationship);
        assertTrue(loaded.mappings.stream().anyMatch(m -> "SNOMED".equals(m.source) && "271649006".equals(m.code)));

        // Changing the legacy LOINC retires the old code and surfaces the new one.
        terminologyService.syncLegacyLoinc(testId(), "1558-6", "1");
        TerminologyResponse changed = controller.getTerminology(testId()).getBody();
        assertEquals(1L, changed.mappings.stream().filter(m -> "LOINC".equals(m.source)).count());
        assertEquals("1558-6", changed.mappings.stream().filter(m -> "LOINC".equals(m.source)).findFirst().get().code);

        // Clearing the legacy LOINC soft-deletes the LOINC mapping but keeps SNOMED.
        terminologyService.syncLegacyLoinc(testId(), null, "1");
        TerminologyResponse cleared = controller.getTerminology(testId()).getBody();
        assertTrue(cleared.mappings.stream().noneMatch(m -> "LOINC".equals(m.source)));
        assertTrue(cleared.mappings.stream().anyMatch(m -> "SNOMED".equals(m.source)));
    }

    @org.junit.Test
    public void terminology_unknownTestReturns404() {
        assertEquals(404, controller.getTerminology("99999999").getStatusCode().value());
        assertEquals(404, controller.saveTerminology("99999999", new TerminologyResponse(), authedRequest())
                .getStatusCode().value());
    }
}
