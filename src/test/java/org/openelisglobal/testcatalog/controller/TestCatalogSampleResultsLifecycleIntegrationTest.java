package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.InterpretationDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.OptionDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.ResultComponentDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.SampleResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-1458: what a test measures stays what the admin configured.
 *
 * <ul>
 * <li>A free-text option typed as a number is that number, never the dictionary
 * entry that happens to have that id; an interpretation on a new free-text
 * option points at the option as stored.</li>
 * <li>Removing a component frees its code: removing Primary while another
 * component exists saves, and a component added again with a removed code is a
 * new component, with none of the old one's ranges.</li>
 * <li>Removing a component retires what was scoped to it: its ranges, alert
 * rules, terminology mappings and QC targets.</li>
 * <li>An active test keeps at least one component; every component has a known
 * result type; the old copy-from endpoint is gone.</li>
 * </ul>
 */
public class TestCatalogSampleResultsLifecycleIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95481L;

    private static final long DICT_ID = 954870L;

    private static final long COPY_TARGET_ID = 95482L;

    private static final String DICT_ENTRY = "LifecycleIT Positive";

    @Autowired
    private org.openelisglobal.test.service.TestService testService;
    @Autowired
    private org.openelisglobal.testresultcomponent.service.TestResultComponentService componentService;
    @Autowired
    private org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService interpretationService;
    @Autowired
    private org.openelisglobal.testresult.service.TestResultService testResultService;
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
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;
    private Long testResultCategoryId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        java.lang.reflect.Field dictionary = TestCatalogEditorRestController.class
                .getDeclaredField("dictionaryService");
        dictionary.setAccessible(true);
        dictionary.set(controller, dictionaryService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'N', ?, NOW())",
                TEST_ID, "LifecycleIT", "LifecycleIT desc", UUID.randomUUID().toString());
        testResultCategoryId = jdbc.queryForObject("SELECT min(id) FROM clinlims.dictionary_category WHERE name = ?",
                Long.class, "Test Result");
        if (testResultCategoryId == null) {
            jdbc.update("INSERT INTO clinlims.dictionary_category (id, name, description, lastupdated)"
                    + " VALUES (954880, 'Test Result', 'General test result', NOW())");
            testResultCategoryId = 954880L;
        }
        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, dictionary_category_id, lastupdated)"
                + " VALUES (?, ?, 'Y', ?, NOW())", DICT_ID, DICT_ENTRY, testResultCategoryId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_result_interpretation i USING clinlims.test_result_component c"
                + " WHERE i.component_id = c.id AND c.test_id = ?", COPY_TARGET_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", COPY_TARGET_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", COPY_TARGET_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", COPY_TARGET_ID);
        jdbc.update("DELETE FROM clinlims.test_alert_rule WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_qc_target WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_result_interpretation i USING clinlims.test_result_component c"
                + " WHERE i.component_id = c.id AND c.test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
        jdbc.update("UPDATE clinlims.test SET default_test_result_id = NULL WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", DICT_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dict_entry IN (?, ?, ?, ?)", String.valueOf(DICT_ID), "1",
                "LifecycleIT Reactive", "LifecycleIT Two");
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

    private static ResultComponentDto comp(String id, String code, String label, boolean primary, int order) {
        ResultComponentDto c = new ResultComponentDto();
        c.id = id;
        c.code = code;
        c.label = label;
        c.isPrimary = primary;
        c.displayOrder = order;
        c.resultType = "N";
        return c;
    }

    private static OptionDto option(String value, String valueName, Boolean freeText, int sortOrder) {
        OptionDto o = new OptionDto();
        o.value = value;
        o.valueName = valueName;
        o.freeText = freeText;
        o.sortOrder = sortOrder;
        o.resultType = "D";
        o.normal = false;
        return o;
    }

    private int save(ResultComponentDto... components) {
        SampleResults body = new SampleResults();
        body.testId = testId();
        body.components.addAll(List.of(components));
        return controller.saveSampleResults(testId(), body, authedRequest()).getStatusCode().value();
    }

    private SampleResults load() {
        return controller.getSampleResults(testId()).getBody();
    }

    private ResultComponentDto byCode(String code) {
        return load().components.stream().filter(c -> code.equals(c.code)).findFirst().orElse(null);
    }

    private String primaryId() {
        if (load().components.isEmpty()) {
            save(comp(null, "PRIMARY", "Result", true, 0));
        }
        return byCode("PRIMARY").id;
    }

    // -- 1, 2: free-text options and their interpretations --------------------

    @org.junit.Test
    public void aFreeTextOptionTypedAsANumberIsThatNumber_notTheEntryWithThatId() {
        ResultComponentDto coded = comp(primaryId(), "PRIMARY", "Result", true, 0);
        coded.resultType = "D";
        coded.options.add(option(String.valueOf(DICT_ID), null, Boolean.TRUE, 1));
        coded.options.add(option(String.valueOf(DICT_ID + 1), String.valueOf(DICT_ID + 1), null, 2));

        assertEquals(200, save(coded));

        List<OptionDto> stored = byCode("PRIMARY").options;
        assertEquals(String.valueOf(DICT_ID), stored.get(0).valueName);
        assertNotEquals("the typed number is not the dictionary entry with that id", String.valueOf(DICT_ID),
                stored.get(0).value);
        assertEquals("a name that is the typed value marks free text too", String.valueOf(DICT_ID + 1),
                stored.get(1).valueName);
    }

    @org.junit.Test
    public void aDictionaryPickStaysThatEntry_withOrWithoutItsName() {
        ResultComponentDto coded = comp(primaryId(), "PRIMARY", "Result", true, 0);
        coded.resultType = "D";
        coded.options.add(option(String.valueOf(DICT_ID), DICT_ENTRY, null, 1));
        assertEquals(200, save(coded));
        assertEquals(String.valueOf(DICT_ID), byCode("PRIMARY").options.get(0).value);

        coded = byCode("PRIMARY");
        coded.options.clear();
        coded.options.add(option(String.valueOf(DICT_ID), null, null, 1));
        assertEquals(200, save(coded));
        assertEquals("an API client sending the bare id still picks that entry", DICT_ENTRY,
                byCode("PRIMARY").options.get(0).valueName);
    }

    @org.junit.Test
    public void anInterpretationOnANewFreeTextOptionPointsAtTheStoredOption() {
        ResultComponentDto coded = comp(primaryId(), "PRIMARY", "Result", true, 0);
        coded.resultType = "D";
        coded.options.add(option("LifecycleIT Reactive", null, Boolean.TRUE, 1));
        InterpretationDto interpretation = new InterpretationDto();
        interpretation.valueMatch = "LifecycleIT Reactive";
        interpretation.text = "Refer";
        interpretation.severity = "ABNORMAL";
        interpretation.displayOrder = 1;
        coded.interpretations.add(interpretation);

        assertEquals(200, save(coded));

        ResultComponentDto stored = byCode("PRIMARY");
        assertTrue(stored.options.get(0).value.matches("\\d+"));
        assertEquals(stored.options.get(0).value, stored.interpretations.get(0).valueMatch);
    }

    // -- 3, 4: removing a component frees its code ----------------------------

    @org.junit.Test
    public void removingPrimaryWhileAnotherComponentExists_promotesTheOther() {
        String primary = primaryId();
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), comp(null, "SECOND", "Second", false, 1)));
        String second = byCode("SECOND").id;

        assertEquals(200, save(comp(second, "PRIMARY", "Second", true, 0)));

        SampleResults after = load();
        assertEquals(1, after.components.size());
        assertEquals(second, after.components.get(0).id);
        assertTrue(after.components.get(0).isPrimary);
    }

    @org.junit.Test
    public void aComponentAddedAgainWithARemovedCode_isNewAndHasNoOldRanges() {
        String primary = primaryId();
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), comp(null, "SECOND", "Second", false, 1)));
        String removed = byCode("SECOND").id;
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                        + " low_normal, high_normal, component_id, lastupdated)"
                        + " VALUES (nextval('clinlims.result_limits_seq'), ?, 4, 0, 'Infinity', 3, 5, ?, NOW())",
                TEST_ID, removed);
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0)));

        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), comp(null, "SECOND", "Again", false, 1)));

        String fresh = byCode("SECOND").id;
        assertNotEquals("a new component, not the removed one", removed, fresh);
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.result_limits WHERE component_id = ?", Integer.class, fresh));
        assertEquals("the removed row is kept for the record, inactive", "N", jdbc.queryForObject(
                "SELECT is_active FROM clinlims.test_result_component WHERE id = ?", String.class, removed));
    }

    // -- 5: what was scoped to a removed component is retired -----------------

    @org.junit.Test
    public void removingAComponentRetiresItsRangesRulesMappingsAndQcTargets() {
        String primary = primaryId();
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), comp(null, "SECOND", "Second", false, 1)));
        String second = byCode("SECOND").id;
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                        + " low_normal, high_normal, component_id, lastupdated)"
                        + " VALUES (nextval('clinlims.result_limits_seq'), ?, 4, 0, 'Infinity', 3, 5, ?, NOW())",
                TEST_ID, second);
        jdbc.update(
                "INSERT INTO clinlims.test_alert_rule (id, test_id, component_id, name, is_enabled, trigger_type,"
                        + " last_updated) VALUES ('lifecycle-rule', ?, ?, 'LifecycleIT rule', true, 'CRITICAL', NOW())",
                TEST_ID, second);
        jdbc.update("INSERT INTO clinlims.test_terminology_mapping (id, test_id, component_id, source, code,"
                + " relationship, is_active, last_updated)"
                + " VALUES ('lifecycle-map', ?, ?, 'LOINC', '5678-9', 'SAME_AS', 'Y', NOW())", TEST_ID, second);
        jdbc.update("INSERT INTO clinlims.test_qc_target (id, test_id, component_id, control_level, is_active,"
                + " last_updated) VALUES ('lifecycle-qc', ?, ?, 'LOW', true, NOW())", TEST_ID, second);

        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0)));

        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.result_limits WHERE component_id = ?", Integer.class, second));
        assertEquals("the rule is switched off and no longer scoped to the removed component", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.test_alert_rule WHERE id = 'lifecycle-rule'"
                        + " AND is_enabled = false AND component_id IS NULL", Integer.class));
        assertEquals("N", jdbc.queryForObject(
                "SELECT is_active FROM clinlims.test_terminology_mapping WHERE id = 'lifecycle-map'", String.class));
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT is_active FROM clinlims.test_qc_target WHERE id = 'lifecycle-qc'", Boolean.class));
    }

    /**
     * A removed select-list component takes its options and interpretations with
     * it.
     */
    @org.junit.Test
    public void removingASelectListComponentRetiresItsOptionsAndInterpretations() {
        String primary = primaryId();
        ResultComponentDto second = comp(null, "SECOND", "Second", false, 1);
        second.resultType = "D";
        second.options.add(option("LifecycleIT Two", null, true, 1));
        InterpretationDto bucket = new InterpretationDto();
        bucket.valueMatch = "LifecycleIT Two";
        bucket.text = "LifecycleIT grade";
        bucket.severity = "ABNORMAL";
        bucket.displayOrder = 1;
        second.interpretations.add(bucket);
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), second));
        String secondId = byCode("SECOND").id;

        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0)));

        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.test_result WHERE component_id = ? AND is_active = true",
                        Integer.class, secondId));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test_result_interpretation WHERE component_id = ? AND is_active = 'Y'",
                Integer.class, secondId));
    }

    /**
     * Creating a test as a copy fills its blank pre-seeded PRIMARY from the source
     * and adds the source's other components with their options and
     * interpretations.
     */
    @org.junit.Test
    public void aCopyFillsTheBlankPrimaryAndAddsTheOtherComponents() {
        String primary = primaryId();
        ResultComponentDto second = comp(null, "SECOND", "Second", false, 1);
        second.resultType = "D";
        second.options.add(option("LifecycleIT Two", null, true, 1));
        InterpretationDto bucket = new InterpretationDto();
        bucket.valueMatch = "LifecycleIT Two";
        bucket.text = "LifecycleIT grade";
        bucket.severity = "ABNORMAL";
        bucket.displayOrder = 1;
        second.interpretations.add(bucket);
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), second));
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, 'LifecycleIT copy', 'LifecycleIT copy desc', 'N', ?, NOW())",
                COPY_TARGET_ID, UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order,"
                        + " allow_multiple_readings, is_primary, show_on_report, is_active, lastupdated, last_updated)"
                        + " VALUES (?, ?, 'PRIMARY', 'Result', 0, false, true, true, 'Y', NOW(), NOW())",
                UUID.randomUUID().toString(), COPY_TARGET_ID);

        componentService.copyComponentsFromTest(testId(), String.valueOf(COPY_TARGET_ID), "1");

        assertEquals("N",
                jdbc.queryForObject(
                        "SELECT result_type FROM clinlims.test_result_component"
                                + " WHERE test_id = ? AND code = 'PRIMARY' AND is_active = 'Y'",
                        String.class, COPY_TARGET_ID));
        String copied = jdbc.queryForObject("SELECT id FROM clinlims.test_result_component"
                + " WHERE test_id = ? AND code = 'SECOND' AND is_active = 'Y'", String.class, COPY_TARGET_ID);
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.test_result WHERE component_id = ? AND is_active = true",
                        Integer.class, copied));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test_result_interpretation WHERE component_id = ? AND is_active = 'Y'",
                Integer.class, copied));
    }

    // -- 6, 7, 8: the rulings of 11 Oct 2026 ---------------------------------

    @org.junit.Test
    public void anActiveTestKeepsAtLeastOneComponent_anInactiveOneMayHaveNone() {
        primaryId();
        jdbc.update("UPDATE clinlims.test SET is_active = 'Y' WHERE id = ?", TEST_ID);
        assertEquals(422, save());
        assertEquals(1, load().components.size());

        jdbc.update("UPDATE clinlims.test SET is_active = 'N' WHERE id = ?", TEST_ID);
        assertEquals(200, save());
        assertEquals(0, load().components.size());
    }

    @org.junit.Test
    public void aComponentWithoutAKnownResultTypeIsRefused() {
        String primary = primaryId();
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0)));
        for (String type : new String[] { null, "Z", "NN", "" }) {
            ResultComponentDto c = comp(primary, "PRIMARY", "Result", true, 0);
            c.resultType = type;
            assertEquals("result type " + type, 422, save(c));
        }
        assertEquals("N", byCode("PRIMARY").resultType);
    }

    @org.junit.Test
    public void theCopyFromEndpointIsRetired() throws Exception {
        primaryId();
        int status = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/rest/test-catalog/tests/" + testId() + "/sample-results/copy-from/" + testId())
                .session((MockHttpSession) authedRequest().getSession())).andReturn().getResponse().getStatus();
        assertTrue("got " + status, status == 404 || status == 405 || status == 410);
    }

    // -- OGC-1463: two editors ----------------------------------------------

    @org.junit.Test
    public void anOldCopyNamingARemovedComponentIsRefusedAsStale_evenWithoutAVersion() {
        String primary = primaryId();
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0), comp(null, "SECOND", "Second", false, 1)));
        SampleResults pageA = load();
        assertEquals(200, save(comp(primary, "PRIMARY", "Result", true, 0)));

        pageA.version = null;
        pageA.components.forEach(c -> c.label = "A " + c.label);
        ResponseEntity<SampleResults> stale = controller.saveSampleResults(testId(), pageA, authedRequest());

        assertEquals(409, stale.getStatusCode().value());
        assertEquals("stale", stale.getBody().conflict);
        assertNull("the removal stands", byCode("SECOND"));
        assertEquals("Result", byCode("PRIMARY").label);
    }

    @org.junit.Test
    public void aSaveSentWithAnOlderVersionIsRefused_andTheCurrentOneSaves() {
        String primary = primaryId();
        SampleResults pageA = load();
        assertEquals(200, save(comp(primary, "PRIMARY", "Renamed by B", true, 0)));

        pageA.components.get(0).label = "Renamed by A";
        assertEquals(409, controller.saveSampleResults(testId(), pageA, authedRequest()).getStatusCode().value());
        assertEquals("Renamed by B", byCode("PRIMARY").label);

        SampleResults reloaded = load();
        reloaded.components.get(0).label = "Renamed by A";
        assertEquals(200, controller.saveSampleResults(testId(), reloaded, authedRequest()).getStatusCode().value());
        assertEquals("Renamed by A", byCode("PRIMARY").label);
    }
}
