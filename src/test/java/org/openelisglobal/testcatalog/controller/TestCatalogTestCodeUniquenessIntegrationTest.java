package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
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
 * OGC-1464: test codes are trimmed and unique, compared without case, on create
 * and when Basic Info changes a code; two creates with one code at the same
 * moment make one test. A code legacy variants already share stays saveable as
 * long as it is not changed.
 */
public class TestCatalogTestCodeUniquenessIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_A = 95641L;
    private static final long TEST_B = 95642L;
    private static final long TEST_C = 95643L;
    private static final long SAMPLE_TYPE = 95644L;
    private static final long SAMPLE_TYPE_LOCALIZATION = 95645L;
    private static final String PREFIX = "UQ1464";

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
        ReflectionTestUtils.setField(controller, "testCatalogCreationService", creationService);
        cleanup();
        insertTest(TEST_A, PREFIX + "LEGACY", PREFIX + " A");
        insertTest(TEST_B, PREFIX + "LEGACY", PREFIX + " B");
        insertTest(TEST_C, PREFIX + "OTHER", PREFIX + " C");
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE_LOCALIZATION, "sampleType name");
        jdbc.update(
                "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                        + " VALUES (?, ?, 'en', 'UQ1464 type', NOW())",
                SAMPLE_TYPE_LOCALIZATION, SAMPLE_TYPE_LOCALIZATION);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, 'UQ1464 type', 'H', true, 1, ?, NOW())",
                SAMPLE_TYPE, SAMPLE_TYPE_LOCALIZATION);
        typeOfSampleService.clearCache();
    }

    private void insertTest(long id, String code, String description) {
        jdbc.update("INSERT INTO clinlims.test (id, name, description, local_code, is_active, guid, domain,"
                + " antimicrobial_resistance, orderable, lastupdated) VALUES (?, ?, ?, ?, 'Y', ?, 'CLINICAL',"
                + " false, true, NOW())", id, description, description, code, UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        String created = "SELECT id FROM clinlims.test WHERE upper(trim(local_code)) LIKE 'UQ1464%'";
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (" + created + ")");
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id IN (" + created + ")");
        jdbc.update("DELETE FROM clinlims.test WHERE upper(trim(local_code)) LIKE 'UQ1464%' OR id IN (?, ?, ?)", TEST_A,
                TEST_B, TEST_C);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", SAMPLE_TYPE_LOCALIZATION);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", SAMPLE_TYPE_LOCALIZATION);
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

    private int create(String code, String name) {
        CreateTestRequest body = new CreateTestRequest();
        body.name = name;
        body.reportingName = name;
        body.code = code;
        body.domain = "CLINICAL";
        body.sampleTypeId = String.valueOf(SAMPLE_TYPE);
        return controller.createTest(body, authedRequest()).getStatusCode().value();
    }

    private int testsWithCode(String code) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.test WHERE lower(trim(local_code)) = lower(?)",
                Integer.class, code);
    }

    private ResponseEntity<BasicInfo> putCode(long testId, String code) {
        BasicInfo body = new BasicInfo();
        body.code = code;
        return controller.saveBasicInfo(String.valueOf(testId), body, authedRequest());
    }

    @org.junit.Test
    public void aCodeThatDiffersOnlyBySpacesOrCaseIsRefused_andCodesAreStoredTrimmed() {
        assertEquals(201, create("  " + PREFIX + "NEW  ", PREFIX + " new"));
        assertEquals("stored trimmed", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.test WHERE local_code = ?", Integer.class, PREFIX + "NEW"));

        assertEquals(409, create(" " + PREFIX + "NEW ", PREFIX + " new 2"));
        assertEquals(409, create((PREFIX + "NEW").toLowerCase(), PREFIX + " new 3"));
        assertEquals(1, testsWithCode(PREFIX + "NEW"));
    }

    @org.junit.Test
    public void aNameThatDiffersOnlyByALeadingSpaceIsRefused() {
        assertEquals(201, create(PREFIX + "N1", PREFIX + " named"));

        assertEquals(409, create(PREFIX + "N2", " " + PREFIX + " named"));
        assertEquals(0, testsWithCode(PREFIX + "N2"));
    }

    @org.junit.Test
    public void basicInfoCannotGiveATestAnotherTestsCode_butALegacySharedCodeStillSaves() {
        ResponseEntity<BasicInfo> taken = putCode(TEST_B, " " + PREFIX.toLowerCase() + "other ");
        assertEquals(409, taken.getStatusCode().value());
        assertEquals("code", taken.getBody().conflict);
        assertEquals(PREFIX + "LEGACY", testService.getTestById(String.valueOf(TEST_B)).getLocalCode());

        ResponseEntity<BasicInfo> unchanged = putCode(TEST_B, PREFIX + "LEGACY");
        assertEquals("a code legacy variants share is not a change", 200, unchanged.getStatusCode().value());
        assertNull(unchanged.getBody().conflict);
        assertEquals(200, putCode(TEST_B, " " + PREFIX + "FRESH ").getStatusCode().value());
        assertEquals(PREFIX + "FRESH", testService.getTestById(String.valueOf(TEST_B)).getLocalCode());
    }

    /**
     * A legacy code stored with surrounding spaces is found by its trimmed code, as
     * the duplicate check finds it, so the catalog loader updates that test instead
     * of skipping the row as a code in use.
     */
    @org.junit.Test
    public void aStoredCodeWithSpacesIsFoundByItsTrimmedCode() {
        jdbc.update("UPDATE clinlims.test SET local_code = ? WHERE id = ?", " " + PREFIX + "OTHER  ", TEST_C);

        assertEquals(1, testService.getTestsByLocalCode(PREFIX.toLowerCase() + "other").size());
        assertEquals(String.valueOf(TEST_C), testService.getTestByLocalCode(PREFIX + "OTHER").getId());
    }

    @org.junit.Test
    public void twoCreatesWithOneCodeAtTheSameMomentMakeOneTest() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (String name : new String[] { PREFIX + " race a", PREFIX + " race b" }) {
                Callable<Integer> call = () -> {
                    start.await();
                    return create(PREFIX + "RACE", name);
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            statuses.sort(null);
            assertEquals(List.of(201, 409), statuses);
            assertEquals(1, testsWithCode(PREFIX + "RACE"));
        } finally {
            pool.shutdownNow();
        }
    }
}
