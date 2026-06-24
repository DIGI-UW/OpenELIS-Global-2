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
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.DictionaryOption;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.InterpretationDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.OptionDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.ResultComponentDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.SampleResults;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M5 / OGC-749 (OGC-962) — Sample & Results result-components API,
 * round-tripped against a real DB. Exercises the diff-based save: insert new,
 * update by id, soft-delete on omit; plus the 422 validation and 404 guards.
 * Controller is instantiated directly (security slice is covered separately).
 */
public class TestCatalogEditorSampleResultsIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95201L;

    private static final long SOURCE_ID = 95202L;

    private static final long UOM_ID = 95299L;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService;

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
    private javax.sql.DataSource dataSource;

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
        // dictionaryService is field-injected in production; set it here so the
        // option-labeling path can be exercised under direct construction.
        java.lang.reflect.Field f = TestCatalogEditorRestController.class.getDeclaredField("dictionaryService");
        f.setAccessible(true);
        f.set(controller, dictionaryService);
        cleanup();
        jdbc.update("INSERT INTO clinlims.unit_of_measure (id, name, lastupdated) VALUES (?, ?, NOW())", UOM_ID,
                "SampleResultsITUOM");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "SampleResultsIT", "SampleResultsIT desc", UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                SOURCE_ID, "SampleResultsCopySrc", "copy source", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupTest(TEST_ID);
        cleanupTest(SOURCE_ID);
        jdbc.update("DELETE FROM clinlims.unit_of_measure WHERE id = ?", UOM_ID);
    }

    private void cleanupTest(long id) {
        // FK order: interpretation -> component -> test; options are TEST_RESULT rows.
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", id);
        try {
            jdbc.update("DELETE FROM clinlims.test_result_interpretation i USING clinlims.test_result_component c"
                    + " WHERE i.component_id = c.id AND c.test_id = ?", id);
        } catch (Exception ignored) {
            // tables absent before changeset 041
        }
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", id);
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", id);
        try {
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", id);
        } catch (Exception ignored) {
            // table absent before changeset 041
        }
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", id);
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

    private static ResultComponentDto comp(String id, String code, String label, Integer order) {
        ResultComponentDto c = new ResultComponentDto();
        c.id = id;
        c.code = code;
        c.label = label;
        c.displayOrder = order;
        return c;
    }

    private SampleResults body(ResultComponentDto... components) {
        SampleResults sr = new SampleResults();
        for (ResultComponentDto c : components) {
            sr.components.add(c);
        }
        return sr;
    }

    @org.junit.Test
    public void getSampleResults_emptyReturns200WithNoComponents() {
        ResponseEntity<SampleResults> resp = controller.getSampleResults(String.valueOf(TEST_ID));
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().components.isEmpty());
    }

    @org.junit.Test
    public void saveSampleResults_insertsComponents_andGetReturnsThemOrderedByDisplayOrder() {
        // Submit out of order (DIA before SYS) to prove the read orders by
        // display_order.
        SampleResults put = body(comp(null, "DIA", "Diastolic", 2), comp(null, "SYS", "Systolic", 1));
        ResponseEntity<SampleResults> saved = controller.saveSampleResults(String.valueOf(TEST_ID), put,
                authedRequest());
        assertEquals(200, saved.getStatusCode().value());

        SampleResults loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals(2, loaded.components.size());
        assertEquals("SYS", loaded.components.get(0).code);
        assertEquals("Systolic", loaded.components.get(0).label);
        assertEquals(Integer.valueOf(1), loaded.components.get(0).displayOrder);
        assertEquals("DIA", loaded.components.get(1).code);
    }

    @org.junit.Test
    public void saveSampleResults_updatesExistingComponentById_withoutCreatingARow() {
        controller.saveSampleResults(String.valueOf(TEST_ID), body(comp(null, "SYS", "Systolic", 1)), authedRequest());
        SampleResults loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        String id = loaded.components.get(0).id;

        // Re-PUT with the captured id and a new label.
        ResultComponentDto edit = comp(id, "SYS", "Systolic BP", 1);
        controller.saveSampleResults(String.valueOf(TEST_ID), body(edit), authedRequest());

        SampleResults after = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals("must update in place, not add a row", 1, after.components.size());
        assertEquals(id, after.components.get(0).id);
        assertEquals("Systolic BP", after.components.get(0).label);
    }

    @org.junit.Test
    public void saveSampleResults_omittingAComponentSoftDeletesIt() {
        controller.saveSampleResults(String.valueOf(TEST_ID),
                body(comp(null, "SYS", "Systolic", 1), comp(null, "DIA", "Diastolic", 2)), authedRequest());
        SampleResults loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        String sysId = loaded.components.stream().filter(c -> "SYS".equals(c.code)).findFirst().get().id;

        // Re-PUT with only SYS → DIA is dropped.
        controller.saveSampleResults(String.valueOf(TEST_ID), body(comp(sysId, "SYS", "Systolic", 1)), authedRequest());

        SampleResults after = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals(1, after.components.size());
        assertEquals("SYS", after.components.get(0).code);
        // DIA soft-deleted, not hard-deleted: the row remains with is_active='N'.
        Long inactive = jdbc
                .queryForObject("SELECT count(*) FROM clinlims.test_result_component WHERE test_id = ? AND code = 'DIA'"
                        + " AND is_active = 'N'", Long.class, TEST_ID);
        assertEquals(Long.valueOf(1L), inactive);
    }

    @org.junit.Test
    public void saveSampleResults_duplicateCodeReturns422() {
        SampleResults put = body(comp(null, "SYS", "First", 1), comp(null, "SYS", "Dup", 2));
        ResponseEntity<SampleResults> resp = controller.saveSampleResults(String.valueOf(TEST_ID), put,
                authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void saveSampleResults_blankCodeOrLabelReturns422() {
        assertEquals(422,
                controller.saveSampleResults(String.valueOf(TEST_ID), body(comp(null, "", "Label", 1)), authedRequest())
                        .getStatusCode().value());
        assertEquals(422,
                controller
                        .saveSampleResults(String.valueOf(TEST_ID), body(comp(null, "CODE", "  ", 1)), authedRequest())
                        .getStatusCode().value());
    }

    @org.junit.Test
    public void sampleResults_unknownTestReturns404() {
        assertEquals(404, controller.getSampleResults("99999999").getStatusCode().value());
        assertEquals(404, controller.saveSampleResults("99999999", body(comp(null, "X", "X", 1)), authedRequest())
                .getStatusCode().value());
    }

    @org.junit.Test
    public void saveSampleResults_persistsInterpretationsPerComponent_andSoftDeletesOnOmit() {
        ResultComponentDto sys = comp(null, "SYS", "Systolic", 1);
        sys.interpretations.add(interp(null, "<90", "Low", "ABNORMAL"));
        sys.interpretations.add(interp(null, ">140", "High", "CRITICAL"));
        controller.saveSampleResults(String.valueOf(TEST_ID), body(sys), authedRequest());

        SampleResults loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        ResultComponentDto loadedSys = loaded.components.get(0);
        assertEquals(2, loadedSys.interpretations.size());
        InterpretationDto low = loadedSys.interpretations.stream().filter(i -> "Low".equals(i.text)).findFirst().get();
        assertEquals("<90", low.valueMatch);
        assertEquals("ABNORMAL", low.severity);

        // Re-PUT the component (by id) keeping only the "High" interpretation (by id)
        // → "Low" is soft-deleted.
        ResultComponentDto edit = comp(loadedSys.id, "SYS", "Systolic", 1);
        InterpretationDto keep = loadedSys.interpretations.stream().filter(i -> "High".equals(i.text)).findFirst()
                .get();
        edit.interpretations.add(interp(keep.id, keep.valueMatch, keep.text, keep.severity));
        controller.saveSampleResults(String.valueOf(TEST_ID), body(edit), authedRequest());

        SampleResults after = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals(1, after.components.get(0).interpretations.size());
        assertEquals("High", after.components.get(0).interpretations.get(0).text);
    }

    private static InterpretationDto interp(String id, String valueMatch, String text, String severity) {
        InterpretationDto i = new InterpretationDto();
        i.id = id;
        i.valueMatch = valueMatch;
        i.text = text;
        i.severity = severity;
        i.displayOrder = 0;
        return i;
    }

    @org.junit.Test
    public void saveSampleResults_persistsOptionsPerComponent_andSoftDeletesOnOmit() {
        ResultComponentDto sex = comp(null, "SEX", "Sex", 1);
        sex.resultType = "D";
        sex.options.add(opt(null, "Male", 1));
        sex.options.add(opt(null, "Female", 2));
        controller.saveSampleResults(String.valueOf(TEST_ID), body(sex), authedRequest());

        SampleResults loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        ResultComponentDto loadedSex = loaded.components.get(0);
        assertEquals(2, loadedSex.options.size());
        // ordered by sortOrder
        assertEquals("Male", loadedSex.options.get(0).value);
        assertEquals(Integer.valueOf(1), loadedSex.options.get(0).sortOrder);
        assertEquals("Female", loadedSex.options.get(1).value);

        // Re-PUT keeping only "Male" (by id) → "Female" is soft-deleted.
        ResultComponentDto edit = comp(loadedSex.id, "SEX", "Sex", 1);
        edit.resultType = "D";
        OptionDto keep = loadedSex.options.stream().filter(o -> "Male".equals(o.value)).findFirst().get();
        edit.options.add(opt(keep.id, keep.value, keep.sortOrder));
        controller.saveSampleResults(String.valueOf(TEST_ID), body(edit), authedRequest());

        SampleResults after = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals(1, after.components.get(0).options.size());
        assertEquals("Male", after.components.get(0).options.get(0).value);
        // "Female" soft-deleted, not hard-deleted: a TEST_RESULT row remains
        // is_active=false.
        Long inactive = jdbc
                .queryForObject("SELECT count(*) FROM clinlims.test_result WHERE test_id = ? AND value = 'Female'"
                        + " AND is_active = false", Long.class, TEST_ID);
        assertEquals(Long.valueOf(1L), inactive);
    }

    @org.junit.Test
    public void copySampleResults_copiesComponentsOptionsAndInterpretations() {
        // Seed the source test's config via the controller.
        ResultComponentDto srcComp = comp(null, "SEX", "Sex", 1);
        srcComp.resultType = "D";
        srcComp.options.add(opt(null, "Male", 1));
        srcComp.interpretations.add(interp(null, "Male", "Boy", "NORMAL"));
        controller.saveSampleResults(String.valueOf(SOURCE_ID), body(srcComp), authedRequest());

        // Copy onto the (empty) target test.
        ResponseEntity<SampleResults> resp = controller.copySampleResults(String.valueOf(TEST_ID),
                String.valueOf(SOURCE_ID), authedRequest());
        assertEquals(200, resp.getStatusCode().value());

        SampleResults target = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals(1, target.components.size());
        assertEquals("SEX", target.components.get(0).code);
        assertEquals(1, target.components.get(0).options.size());
        assertEquals("Male", target.components.get(0).options.get(0).value);
        assertEquals(1, target.components.get(0).interpretations.size());
        assertEquals("Boy", target.components.get(0).interpretations.get(0).text);
    }

    @org.junit.Test
    public void copySampleResults_unknownTargetReturns404() {
        assertEquals(404, controller.copySampleResults("99999999", String.valueOf(SOURCE_ID), authedRequest())
                .getStatusCode().value());
    }

    @org.junit.Test
    public void saveSampleResults_reAddingASoftDeletedCode_reactivatesInsteadOfColliding() {
        // Add SYS, then remove it (soft-delete leaves the row with is_active='N',
        // still occupying the (test_id, code) UNIQUE slot).
        controller.saveSampleResults(String.valueOf(TEST_ID), body(comp(null, "SYS", "Systolic", 1)), authedRequest());
        controller.saveSampleResults(String.valueOf(TEST_ID), body(), authedRequest());

        // Re-add the same code — must reactivate the dead row, not insert a colliding
        // one (which would violate uq_test_result_component_test_code → 500).
        ResponseEntity<SampleResults> resp = controller.saveSampleResults(String.valueOf(TEST_ID),
                body(comp(null, "SYS", "Systolic BP", 1)), authedRequest());
        assertEquals(200, resp.getStatusCode().value());

        SampleResults after = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals(1, after.components.size());
        assertEquals("SYS", after.components.get(0).code);
        assertEquals("Systolic BP", after.components.get(0).label);
        // Exactly one physical row for (test_id, SYS) — reactivated, not duplicated.
        Long rows = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test_result_component WHERE test_id = ? AND code = 'SYS'", Long.class,
                TEST_ID);
        assertEquals(Long.valueOf(1L), rows);
    }

    @org.junit.Test
    public void saveSampleResults_syncsPrimaryUomAndSignificantDigitsToLegacyColumns() {
        // The legacy Test Modify page reads UOM from test.uom_id and significant
        // digits from test_result.significant_digits; the new editor must mirror the
        // PRIMARY component's values back onto those columns.
        ResultComponentDto primary = comp(null, "PRIMARY", "Result", 0);
        primary.resultType = "N";
        primary.uomId = String.valueOf(UOM_ID);
        primary.significantDigits = 3;
        primary.options.add(opt(null, "value", 1));
        controller.saveSampleResults(String.valueOf(TEST_ID), body(primary), authedRequest());

        Long uom = jdbc.queryForObject("SELECT uom_id FROM clinlims.test WHERE id = ?", Long.class, TEST_ID);
        assertEquals(Long.valueOf(UOM_ID), uom);

        String sig = jdbc.queryForObject(
                "SELECT significant_digits FROM clinlims.test_result WHERE test_id = ? AND is_active = true LIMIT 1",
                String.class, TEST_ID);
        assertEquals("3", sig);
    }

    @org.junit.Test
    public void syncPrimaryComponentFromLegacy_pullsUomAndSignificantDigitsIntoTheComponent() {
        // Editor creates a PRIMARY component carrying no UOM / significant digits.
        ResultComponentDto primary = comp(null, "PRIMARY", "Result", 0);
        primary.resultType = "N";
        controller.saveSampleResults(String.valueOf(TEST_ID), body(primary), authedRequest());

        // Simulate a legacy Test Modify save: it writes test.uom_id and a
        // test_result.significant_digits, but never touches test_result_component.
        jdbc.update("UPDATE clinlims.test SET uom_id = ? WHERE id = ?", UOM_ID, TEST_ID);
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, is_active,"
                + " lastupdated) VALUES (?, ?, 'N', 4, true, NOW())", 952010L, TEST_ID);

        componentService.syncPrimaryComponentFromLegacy(String.valueOf(TEST_ID), "1");

        ResultComponentDto loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody().components.get(0);
        assertEquals("legacy UOM must surface on the PRIMARY component", String.valueOf(UOM_ID), loaded.uomId);
        assertEquals("legacy significant digits must surface on the PRIMARY component", Integer.valueOf(4),
                loaded.significantDigits);
    }

    @org.junit.Test
    public void syncPrimaryComponentFromLegacy_createsPrimary_syncsResultType_andRepointsOptionsAndRanges() {
        // The test has NO component yet (mimics a test created on the legacy Add
        // page). Legacy wrote uom_id on the test, a dictionary option (test_result)
        // and a range (result_limits), both with component_id = NULL.
        jdbc.update("UPDATE clinlims.test SET uom_id = ? WHERE id = ?", UOM_ID, TEST_ID);
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, significant_digits,"
                + " is_active, lastupdated) VALUES (?, ?, 'D', 'Positive', 2, true, NOW())", 952020L, TEST_ID);
        Long resultTypeId = jdbc.queryForObject("SELECT min(id) FROM clinlims.type_of_test_result", Long.class);
        jdbc.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                + " lastupdated) VALUES (?, ?, ?, 0, 120, NOW())", 952030L, TEST_ID, resultTypeId);

        componentService.syncPrimaryComponentFromLegacy(String.valueOf(TEST_ID), "1");

        SampleResults sr = controller.getSampleResults(String.valueOf(TEST_ID)).getBody();
        assertEquals("a PRIMARY component is created for the legacy test", 1, sr.components.size());
        ResultComponentDto primary = sr.components.get(0);
        assertEquals("PRIMARY", primary.code);
        assertEquals("legacy UOM surfaces", String.valueOf(UOM_ID), primary.uomId);
        assertEquals("legacy result type surfaces", "D", primary.resultType);
        assertEquals("legacy significant digits surface", Integer.valueOf(2), primary.significantDigits);
        assertEquals("legacy option is repointed onto PRIMARY and shows as an option", 1, primary.options.size());
        assertEquals("Positive", primary.options.get(0).value);

        Long boundRanges = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.result_limits WHERE test_id = ? AND component_id IS NOT NULL",
                Long.class, TEST_ID);
        assertEquals("legacy range is repointed onto the PRIMARY component", Long.valueOf(1L), boundRanges);
    }

    @org.junit.Test
    public void syncPrimaryComponentFromLegacy_picksLatestSignificantDigits_whenLegacyLeavesStaleActiveRows() {
        ResultComponentDto primary = comp(null, "PRIMARY", "Result", 0);
        primary.resultType = "N";
        controller.saveSampleResults(String.valueOf(TEST_ID), body(primary), authedRequest());

        // The legacy numeric modify inserts a fresh test_result WITHOUT deactivating
        // the old one, so two active rows coexist: an older row with stale significant
        // digits (2) and a newer row with the edited value (5). The sync must take the
        // newest (highest id), not whichever the query returns first.
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, is_active,"
                + " lastupdated) VALUES (?, ?, 'N', 2, true, NOW())", 952040L, TEST_ID);
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, is_active,"
                + " lastupdated) VALUES (?, ?, 'N', 5, true, NOW())", 952041L, TEST_ID);

        componentService.syncPrimaryComponentFromLegacy(String.valueOf(TEST_ID), "1");

        ResultComponentDto loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody().components.get(0);
        assertEquals("latest legacy significant digits must win over stale active rows", Integer.valueOf(5),
                loaded.significantDigits);
    }

    @org.junit.Test
    public void syncPrimaryComponentFromLegacy_doesNotExposeNumericResultRowsAsOptions() {
        ResultComponentDto primary = comp(null, "PRIMARY", "Result", 0);
        primary.resultType = "N";
        controller.saveSampleResults(String.valueOf(TEST_ID), body(primary), authedRequest());

        // Two saves on a numeric test → legacy leaves two active 'N' result rows.
        // These are result definitions, not select-list options, and must never show
        // as options no matter how many accumulate.
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, is_active,"
                + " lastupdated) VALUES (?, ?, 'N', 2, true, NOW())", 952050L, TEST_ID);
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, is_active,"
                + " lastupdated) VALUES (?, ?, 'N', 2, true, NOW())", 952051L, TEST_ID);
        componentService.syncPrimaryComponentFromLegacy(String.valueOf(TEST_ID), "1");

        ResultComponentDto loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody().components.get(0);
        assertTrue("numeric result rows must not appear as select-list options", loaded.options.isEmpty());
    }

    @org.junit.Test
    public void listTests_appendsSampleTypeToTheName() {
        // Reuse a seeded sample type (type_of_sample has a non-null localization FK).
        java.util.Map<String, Object> st = jdbc.queryForMap(
                "SELECT id, description FROM clinlims.type_of_sample WHERE description IS NOT NULL ORDER BY id LIMIT 1");
        String sampleTypeId = String.valueOf(st.get("id"));
        String sampleDesc = (String) st.get("description");
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)", 952060L,
                Long.parseLong(sampleTypeId), TEST_ID);

        TestCatalogEditorRestController.TestListPage page = controller.listTests(null, "all", null, "SampleResultsIT",
                1, 25);
        TestCatalogEditorRestController.TestListRow row = page.rows.stream()
                .filter(r -> r.testId.equals(String.valueOf(TEST_ID))).findFirst().orElseThrow();
        assertTrue("test name must be disambiguated by its sample type, got: " + row.name,
                row.name.endsWith(" (" + sampleDesc + ")"));
    }

    @org.junit.Test
    public void sampleResults_labelsDictionaryOptionsWithTheirEntryName() {
        // Reuse a seeded dictionary entry rather than insert one (it has a category
        // FK).
        java.util.Map<String, Object> dict = jdbc.queryForMap(
                "SELECT id, dict_entry FROM clinlims.dictionary WHERE dict_entry IS NOT NULL ORDER BY id LIMIT 1");
        String dictId = String.valueOf(dict.get("id"));
        String dictEntry = (String) dict.get("dict_entry");

        ResultComponentDto primary = comp(null, "PRIMARY", "Result", 0);
        primary.resultType = "D";
        controller.saveSampleResults(String.valueOf(TEST_ID), body(primary), authedRequest());
        String componentId = controller.getSampleResults(String.valueOf(TEST_ID)).getBody().components.get(0).id;

        // A dictionary-backed option stores the dictionary id in value.
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, component_id, tst_rslt_type, value, is_active,"
                + " lastupdated) VALUES (?, ?, ?, 'D', ?, true, NOW())", 952061L, TEST_ID, componentId, dictId);

        OptionDto option = controller.getSampleResults(String.valueOf(TEST_ID)).getBody().components.get(0).options
                .stream().filter(o -> dictId.equals(o.value)).findFirst().orElseThrow();
        assertEquals("the raw dictionary id is preserved for save round-trip", dictId, option.value);
        assertEquals("the dictionary entry name is exposed for display", dictEntry, option.valueName);
    }

    @org.junit.Test
    public void dictionaryOptionSavedInNewEditor_persistsAndIsVisibleToBothLegacyAndNewReads() {
        java.util.Map<String, Object> dict = jdbc.queryForMap("SELECT id, dict_entry FROM clinlims.dictionary"
                + " WHERE dict_entry IS NOT NULL AND is_active = 'Y' ORDER BY id LIMIT 1");
        String dictId = String.valueOf(dict.get("id"));
        String dictEntry = (String) dict.get("dict_entry");

        // Save through the real new-editor path: a dictionary component carrying one
        // dictionary-backed option (value = dictionary id), exactly as the ComboBox
        // adds.
        ResultComponentDto primary = comp(null, "PRIMARY", "Result", 0);
        primary.resultType = "D";
        primary.options.add(opt(null, dictId, 1));
        controller.saveSampleResults(String.valueOf(TEST_ID), body(primary), authedRequest());

        // Backend: persisted as an active dictionary-type test_result row on the test.
        Long persisted = jdbc.queryForObject("SELECT count(*) FROM clinlims.test_result WHERE test_id = ? AND value = ?"
                + " AND tst_rslt_type = 'D' AND is_active = true", Long.class, TEST_ID, dictId);
        assertEquals("option must persist as an active dictionary test_result row", Long.valueOf(1L), persisted);

        // New UI: surfaced as an option labeled with the dictionary name.
        OptionDto loaded = controller.getSampleResults(String.valueOf(TEST_ID)).getBody().components.get(0).options
                .stream().filter(o -> dictId.equals(o.value)).findFirst().orElseThrow();
        assertEquals(dictEntry, loaded.valueName);

        // Legacy UI: the row is returned by the legacy read (by test_id + active), is a
        // dictionary variant, and its value resolves to the dictionary name.
        org.openelisglobal.test.valueholder.Test test = testService.getTestById(String.valueOf(TEST_ID));
        boolean legacyWouldShow = testResultService.getAllActiveTestResultsPerTest(test).stream()
                .anyMatch(tr -> dictId.equals(tr.getValue()) && "D".equals(tr.getTestResultType())
                        && Boolean.TRUE.equals(tr.getIsActive()));
        assertTrue("legacy Test Modify read must include the new dictionary option", legacyWouldShow);
        assertEquals("legacy resolves the option value to the dictionary name", dictEntry,
                dictionaryService.getDataForId(dictId).getDictEntry());
    }

    @org.junit.Test
    public void searchDictionaryOptions_findsActiveEntriesByNamePrefix_andBlankReturnsNothing() {
        // A seeded active dictionary entry with no abbreviation (so the autocomplete
        // matches on the entry name prefix).
        java.util.Map<String, Object> dict = jdbc.queryForMap("SELECT id, dict_entry FROM clinlims.dictionary"
                + " WHERE dict_entry IS NOT NULL AND local_abbrev IS NULL AND is_active = 'Y' ORDER BY id LIMIT 1");
        String dictId = String.valueOf(dict.get("id"));
        String dictEntry = (String) dict.get("dict_entry");
        String prefix = dictEntry.substring(0, Math.min(3, dictEntry.length()));

        java.util.List<DictionaryOption> results = controller.searchDictionaryOptions(prefix);
        assertTrue("typeahead must return the matching dictionary entry",
                results.stream().anyMatch(o -> dictId.equals(o.id) && dictEntry.equals(o.name)));
        assertTrue("blank search must return nothing", controller.searchDictionaryOptions("  ").isEmpty());
    }

    private static OptionDto opt(String id, String value, Integer sortOrder) {
        OptionDto o = new OptionDto();
        o.id = id;
        o.value = value;
        o.sortOrder = sortOrder;
        o.normal = true;
        o.resultType = "D";
        return o;
    }
}
