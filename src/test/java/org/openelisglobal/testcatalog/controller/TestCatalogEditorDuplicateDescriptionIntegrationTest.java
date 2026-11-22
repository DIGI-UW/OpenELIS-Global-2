package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.CreateTestRequest;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1180 — TEST.description carries a unique index (test_desc_uk), and the
 * editor's save path used to hand a duplicate straight to the constraint: an
 * HTTP 500 with an empty body, on exactly the create-then-edit flow the editor
 * redesign steers users into. Create derives the description from the name, so
 * the create path could hit the same constraint through a duplicate name.
 *
 * <p>
 * The failure was reported as session-correlated ("a test created in this
 * session cannot be saved") because the reporting suite saved a fixed
 * description literal: the first test to claim it saved fine, every later one
 * collided. These tests pin the honest contract instead — a deliberate 409
 * naming the conflicting field, never a bare 500 — and pin exact-match
 * semantics, since the database index is case-sensitive.
 */
public class TestCatalogEditorDuplicateDescriptionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_A = 95101L;
    private static final long TEST_B = 95102L;
    private static final String DESC_A = "DupDesc A 1180";
    private static final String DESC_B = "DupDesc B 1180";
    private static final long SAMPLE_TYPE = 95103L;
    private static final long SAMPLE_TYPE_LOCALIZATION = 95104L;

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
    private org.openelisglobal.testcatalog.service.TestCatalogCreationService creationService;
    @Autowired
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService;

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
        // create-in-place is field-injected (optional) in production; wire it here
        // so the createTest endpoint is exercisable.
        ReflectionTestUtils.setField(controller, "testCatalogCreationService", creationService);
        cleanup();
        insertTest(TEST_A, "DupDescA1180", DESC_A);
        insertTest(TEST_B, "DupDescB1180", DESC_B);
        // The create path validates domain compatibility before the description
        // guard; a fixture-owned clinical type keeps that check deterministic.
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE_LOCALIZATION, "sampleType name");
        jdbc.update(
                "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                        + " VALUES (?, ?, 'en', 'DupDesc type 1180', NOW())",
                SAMPLE_TYPE_LOCALIZATION, SAMPLE_TYPE_LOCALIZATION);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, 'DupDesc type 1180', 'H', true, 1, ?, NOW())",
                SAMPLE_TYPE, SAMPLE_TYPE_LOCALIZATION);
        typeOfSampleService.clearCache();
    }

    private void insertTest(long id, String name, String description) {
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                + " antimicrobial_resistance, orderable, lastupdated) VALUES (?, ?, ?, 'Y', ?, 'CLINICAL',"
                + " false, true, NOW())", id, name, description, UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
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

    private ResponseEntity<BasicInfo> putDescription(long testId, String description) {
        BasicInfo body = new BasicInfo();
        body.description = description;
        return controller.saveBasicInfo(String.valueOf(testId), body, authedRequest());
    }

    // ── the reported failure ────────────────────────────────────────────────────

    @org.junit.Test
    public void savingAnotherTestsDescriptionAnswersANamedConflictNotABare500() {
        ResponseEntity<BasicInfo> resp = putDescription(TEST_B, DESC_A);

        assertEquals(409, resp.getStatusCode().value());
        assertEquals("the client needs to know which field conflicted", "description", resp.getBody().conflict);
        assertEquals("a rejected save leaves the test untouched", DESC_B,
                testService.getTestById(String.valueOf(TEST_B)).getDescription());
    }

    @org.junit.Test
    public void savingItsOwnDescriptionBackIsStillIdempotent() {
        // The reported repro PUT the body its own GET returned; that must stay 200.
        ResponseEntity<BasicInfo> resp = putDescription(TEST_B, DESC_B);

        assertEquals(200, resp.getStatusCode().value());
        assertNull(resp.getBody().conflict);
    }

    @org.junit.Test
    public void savingAFreshDescriptionStillPersists() {
        ResponseEntity<BasicInfo> resp = putDescription(TEST_B, "DupDesc B 1180 edited");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("DupDesc B 1180 edited", testService.getTestById(String.valueOf(TEST_B)).getDescription());
    }

    @org.junit.Test
    public void aCaseVariantIsAllowedBecauseTheIndexIsCaseSensitive() {
        // test_desc_uk is a plain btree on description: "dupdesc a 1180" and
        // "DupDesc A 1180" coexist in the database, so the guard must not be
        // stricter than the constraint it fronts.
        ResponseEntity<BasicInfo> resp = putDescription(TEST_B, DESC_A.toLowerCase());

        assertEquals(200, resp.getStatusCode().value());
    }

    // ── the same constraint through create (description defaults to the name) ──

    @org.junit.Test
    public void creatingWithADuplicateNameAnswersTheSameNamedConflict() {
        CreateTestRequest body = new CreateTestRequest();
        body.name = DESC_A;
        body.reportingName = DESC_A;
        body.code = "DUP1180";
        body.domain = "CLINICAL";
        body.sampleTypeId = String.valueOf(SAMPLE_TYPE);

        ResponseEntity<?> resp = controller.createTest(body, authedRequest());

        assertEquals(409, resp.getStatusCode().value());
        assertEquals("description", ((TestCatalogEditorRestController.CreatedTest) resp.getBody()).conflict);
    }

    // ── the fall-through the ticket flagged in the same code path ──────────────

    @org.junit.Test
    @SuppressWarnings("unchecked")
    public void refreshingDictionaryTestResultsLeavesItsNeighborsAlone() {
        // refreshList(DICTIONARY_TEST_RESULTS) used to fall through and rebuild
        // ARV_ORG_LIST, ACTIVE_ORG_LIST and RESULT_TYPE_CODES too. The context's
        // DisplayListService bean is a mock, so this drives a real instance with
        // the one collaborator the requested case needs; the neighbor cases would
        // NPE on their missing services if the fall-through were still there —
        // and the sentinel identity check catches even a rebuilt-to-equal list.
        DisplayListService real = new DisplayListService();
        ReflectionTestUtils.setField(real, "dictionaryService", dictionaryService);
        Map<DisplayListService.ListType, List<IdValuePair>> map = (Map<DisplayListService.ListType, List<IdValuePair>>) ReflectionTestUtils
                .getField(DisplayListService.class, "typeToListMap");
        if (map == null) {
            map = new HashMap<>();
            ReflectionTestUtils.setField(DisplayListService.class, "typeToListMap", map);
        }
        List<IdValuePair> sentinel = new ArrayList<>();
        map.put(DisplayListService.ListType.ARV_ORG_LIST, sentinel);
        map.put(DisplayListService.ListType.ACTIVE_ORG_LIST, sentinel);
        map.put(DisplayListService.ListType.RESULT_TYPE_CODES, sentinel);

        real.refreshList(DisplayListService.ListType.DICTIONARY_TEST_RESULTS);

        assertNotNull("the requested list itself is rebuilt",
                map.get(DisplayListService.ListType.DICTIONARY_TEST_RESULTS));
        assertSame("ARV_ORG_LIST must not be rebuilt by fall-through", sentinel,
                map.get(DisplayListService.ListType.ARV_ORG_LIST));
        assertSame("ACTIVE_ORG_LIST must not be rebuilt by fall-through", sentinel,
                map.get(DisplayListService.ListType.ACTIVE_ORG_LIST));
        assertSame("RESULT_TYPE_CODES must not be rebuilt by fall-through", sentinel,
                map.get(DisplayListService.ListType.RESULT_TYPE_CODES));
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id IN (SELECT id FROM clinlims.test"
                + " WHERE description IN (?, ?, ?))", DESC_A, DESC_B, "DupDesc B 1180 edited");
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?) OR description IN (?, ?)", TEST_A, TEST_B, DESC_A,
                "DupDesc B 1180 edited");
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", SAMPLE_TYPE_LOCALIZATION);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", SAMPLE_TYPE_LOCALIZATION);
    }
}
