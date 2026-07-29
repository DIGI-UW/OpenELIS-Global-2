package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.openelisglobal.testcatalog.service.CatalogHealthService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-1145 P1c/P1d — the Basic Info sample-type write path as a true m:n:
 * reconcile-to-N junction rows (FR-2), the D-030 domain guard (FR-3), the
 * at-least-one rule for active/orderable tests (FR-1), the multi-type create
 * path, the domain-filtered sample-type reference list, the list row's
 * sampleTypes payload (FR-9), and the FR-10 health-finding inversion (zero
 * links is the only error; multiple links raise nothing).
 */
public class TestCatalogEditorSampleTypesIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 96001L;
    private static final long HUMAN_TYPE_A = 96101L;
    private static final long HUMAN_TYPE_B = 96102L;
    private static final long ENV_TYPE = 96103L;
    private static final long NULL_DOMAIN_TYPE = 96104L;

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
    private CatalogHealthService catalogHealthService;
    @Autowired
    private org.openelisglobal.testcatalog.service.TestCatalogCreationService creationService;
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
        // create-in-place is field-injected (optional) in production; wire it here so
        // the createTest endpoint is exercisable.
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "testCatalogCreationService",
                creationService);
        cleanup();
        seedSampleType(HUMAN_TYPE_A, "Human A 1145", "H");
        seedSampleType(HUMAN_TYPE_B, "Human B 1145", "H");
        seedSampleType(ENV_TYPE, "Env 1145", "E");
        seedSampleType(NULL_DOMAIN_TYPE, "NoDomain 1145", null);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'SampleTypesIT', 'SampleTypesIT desc', 'Y', ?, 'CLINICAL', true, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        typeOfSampleService.clearCache();
    }

    private void seedSampleType(long id, String description, String domain) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                description);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, ?, ?, 'true', ?, ?, NOW())",
                id, description, domain, "ST" + id % 1000, id, id);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ? OR sample_type_id IN (?, ?, ?, ?)", TEST_ID,
                HUMAN_TYPE_A, HUMAN_TYPE_B, ENV_TYPE, NULL_DOMAIN_TYPE);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN"
                + " (SELECT id FROM clinlims.test WHERE name = 'SampleTypesCreateIT')");
        jdbc.update("DELETE FROM clinlims.test WHERE id = ? OR name = 'SampleTypesCreateIT'", TEST_ID);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id IN (?, ?, ?, ?)", HUMAN_TYPE_A, HUMAN_TYPE_B,
                ENV_TYPE, NULL_DOMAIN_TYPE);
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (?, ?, ?, ?)", HUMAN_TYPE_A, HUMAN_TYPE_B, ENV_TYPE,
                NULL_DOMAIN_TYPE);
        typeOfSampleService.clearCache();
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

    private List<String> junctionTypeIds() {
        return jdbc.queryForList("SELECT sample_type_id::text FROM clinlims.sampletype_test WHERE test_id = ?"
                + " ORDER BY sample_type_id", String.class, TEST_ID);
    }

    private BasicInfo saveTypes(String... typeIds) {
        BasicInfo body = new BasicInfo();
        body.sampleTypeIds = List.of(typeIds);
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), body, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        return resp.getBody();
    }

    @org.junit.Test
    public void sampleTypes_reconcileToN_addAndRemove() {
        BasicInfo saved = saveTypes(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_B));
        assertEquals(2, saved.sampleTypeIds.size());
        assertEquals(List.of(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_B)), junctionTypeIds());

        // reconcile: drop A, keep B, add the domainless type
        saveTypes(String.valueOf(HUMAN_TYPE_B), String.valueOf(NULL_DOMAIN_TYPE));
        assertEquals(List.of(String.valueOf(HUMAN_TYPE_B), String.valueOf(NULL_DOMAIN_TYPE)), junctionTypeIds());

        // duplicates in the request collapse to one junction row (FR-2)
        saveTypes(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_A));
        assertEquals(List.of(String.valueOf(HUMAN_TYPE_A)), junctionTypeIds());
    }

    @org.junit.Test
    public void sampleTypes_domainGuard_refusesMismatchAndAllowsDomainless() {
        BasicInfo bad = new BasicInfo();
        bad.sampleTypeIds = List.of(String.valueOf(ENV_TYPE));
        assertEquals("an E-domain sample type on a CLINICAL test is refused (D-030)", 422,
                controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest()).getStatusCode().value());
        assertTrue("a refused save leaves the junction untouched", junctionTypeIds().isEmpty());

        // legacy domainless sample types stay usable everywhere
        saveTypes(String.valueOf(NULL_DOMAIN_TYPE));
        assertEquals(List.of(String.valueOf(NULL_DOMAIN_TYPE)), junctionTypeIds());
    }

    @org.junit.Test
    public void sampleTypes_atLeastOneRequiredWhileActiveOrOrderable() {
        saveTypes(String.valueOf(HUMAN_TYPE_A));
        BasicInfo clear = new BasicInfo();
        clear.sampleTypeIds = List.of();
        assertEquals("clearing every sample type on an active test is refused (FR-1)", 422,
                controller.saveBasicInfo(String.valueOf(TEST_ID), clear, authedRequest()).getStatusCode().value());
        assertEquals(List.of(String.valueOf(HUMAN_TYPE_A)), junctionTypeIds());

        // deactivating + un-ordering in the same request may clear the links
        clear.active = false;
        clear.orderable = false;
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), clear, authedRequest()).getStatusCode().value());
        assertTrue(junctionTypeIds().isEmpty());
    }

    @org.junit.Test
    public void sampleTypes_legacyScalarStillReconciles() {
        BasicInfo body = new BasicInfo();
        body.sampleTypeId = String.valueOf(HUMAN_TYPE_B);
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), body, authedRequest()).getStatusCode().value());
        assertEquals(List.of(String.valueOf(HUMAN_TYPE_B)), junctionTypeIds());
    }

    @org.junit.Test
    public void createTest_linksEverySampleType_andGuardsDomain() {
        TestCatalogEditorRestController.CreateTestRequest create = new TestCatalogEditorRestController.CreateTestRequest();
        create.name = "SampleTypesCreateIT";
        create.reportingName = "SampleTypesCreateIT";
        create.code = "STCIT1145";
        create.domain = "CLINICAL";
        create.sampleTypeIds = List.of(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_B));
        ResponseEntity<TestCatalogEditorRestController.CreatedTest> created = controller.createTest(create,
                authedRequest());
        assertEquals(201, created.getStatusCode().value());
        List<String> linked = jdbc.queryForList(
                "SELECT sample_type_id::text FROM clinlims.sampletype_test"
                        + " WHERE test_id = ?::numeric ORDER BY sample_type_id",
                String.class, created.getBody().testId);
        assertEquals(List.of(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_B)), linked);

        TestCatalogEditorRestController.CreateTestRequest badDomain = new TestCatalogEditorRestController.CreateTestRequest();
        badDomain.name = "SampleTypesCreateIT2";
        badDomain.reportingName = "SampleTypesCreateIT2";
        badDomain.code = "STCIT1146";
        badDomain.domain = "CLINICAL";
        badDomain.sampleTypeIds = List.of(String.valueOf(ENV_TYPE));
        assertEquals(422, controller.createTest(badDomain, authedRequest()).getStatusCode().value());
    }

    @org.junit.Test
    public void listSampleTypes_domainParamFiltersOffers() {
        List<TestCatalogEditorRestController.SampleTypeOption> clinical = controller.listSampleTypes("CLINICAL");
        assertTrue(clinical.stream().anyMatch(o -> String.valueOf(HUMAN_TYPE_A).equals(o.id)));
        assertTrue("domainless sample types are offered everywhere",
                clinical.stream().anyMatch(o -> String.valueOf(NULL_DOMAIN_TYPE).equals(o.id)));
        assertFalse("E-domain types are not offered for CLINICAL",
                clinical.stream().anyMatch(o -> String.valueOf(ENV_TYPE).equals(o.id)));

        List<TestCatalogEditorRestController.SampleTypeOption> unfiltered = controller.listSampleTypes(null);
        assertTrue(unfiltered.stream().anyMatch(o -> String.valueOf(ENV_TYPE).equals(o.id)));
    }

    @org.junit.Test
    public void listTests_rowCarriesEverySampleType() {
        saveTypes(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_B));
        TestCatalogEditorRestController.TestListPage page = controller.listTests(null, "all", null, null,
                "SampleTypesIT", false, 1, 10);
        assertEquals(1, page.rows.size());
        assertEquals("FR-9: the row lists every associated specimen", 2, page.rows.get(0).sampleTypes.size());
        assertEquals(page.rows.get(0).sampleTypes.get(0), page.rows.get(0).sampleType);
    }

    @org.junit.Test
    public void catalogHealth_flagsZeroLinksOnly() {
        // multiple links: the supported m:n shape raises nothing (FR-10)
        saveTypes(String.valueOf(HUMAN_TYPE_A), String.valueOf(HUMAN_TYPE_B));
        catalogHealthService.invalidate();
        List<CatalogHealthService.Finding> multi = catalogHealthService.getAll().getOrDefault(String.valueOf(TEST_ID),
                List.of());
        assertFalse("multiple sample-type links are not a finding",
                multi.stream().anyMatch(f -> "SAMPLE_TYPE_LINKS".equals(f.code)));

        // zero links: the only sample-type-link error
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", TEST_ID);
        typeOfSampleService.clearCache();
        catalogHealthService.invalidate();
        List<CatalogHealthService.Finding> zero = catalogHealthService.getAll().getOrDefault(String.valueOf(TEST_ID),
                List.of());
        assertTrue("a test with no sample-type link is flagged", zero.stream().anyMatch(
                f -> "SAMPLE_TYPE_LINKS".equals(f.code) && f.severity == CatalogHealthService.Severity.ERROR));
    }
}
