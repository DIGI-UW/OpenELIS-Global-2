package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.StorageDto;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M8 / OGC-977..979 — Sample Storage API, round-tripped against a real
 * DB. Exercises the singleton upsert (insert then update-in-place, no second
 * row), the empty-config GET, and the 404 guards.
 */
public class TestCatalogEditorStorageIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95401L;

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

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                analyzerTestMappingService, typeOfSampleService, typeOfSampleTestService, terminologyService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "StorageIT", "StorageIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_sample_handling WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
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

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    @org.junit.Test
    public void getStorage_noConfigReturnsEmptyDtoWithFalseFlags() {
        ResponseEntity<StorageDto> resp = controller.getStorage(testId());
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(Boolean.FALSE, resp.getBody().protectFromLight);
        assertEquals(Boolean.FALSE, resp.getBody().overrideRestricted);
        assertEquals(null, resp.getBody().storageCondition);
    }

    @org.junit.Test
    public void saveStorage_insertsConfig_andGetReturnsIt() {
        StorageDto put = new StorageDto();
        put.storageCondition = "REFRIGERATED";
        put.storageDuration = 7;
        put.storageDurationUnit = "days";
        put.protectFromLight = true;
        put.disposalMethod = "INCINERATION";
        put.overrideRestricted = true;
        ResponseEntity<StorageDto> saved = controller.saveStorage(testId(), put, authedRequest());
        assertEquals(200, saved.getStatusCode().value());

        StorageDto loaded = controller.getStorage(testId()).getBody();
        assertEquals("REFRIGERATED", loaded.storageCondition);
        assertEquals(Integer.valueOf(7), loaded.storageDuration);
        assertEquals("days", loaded.storageDurationUnit);
        assertTrue(loaded.protectFromLight);
        assertEquals("INCINERATION", loaded.disposalMethod);
        assertTrue(loaded.overrideRestricted);
    }

    @org.junit.Test
    public void saveStorage_isSingleton_secondSaveUpdatesInPlaceAndBumpsVersion() {
        StorageDto first = new StorageDto();
        first.storageCondition = "FROZEN";
        first.doNotRefrigerate = true;
        controller.saveStorage(testId(), first, authedRequest());

        StorageDto second = new StorageDto();
        second.storageCondition = "AMBIENT";
        second.doNotRefrigerate = false;
        controller.saveStorage(testId(), second, authedRequest());

        // Exactly one row survives — the upsert updated in place, did not insert.
        Long rows = jdbc.queryForObject("SELECT count(*) FROM clinlims.test_sample_handling WHERE test_id = ?",
                Long.class, TEST_ID);
        assertEquals(Long.valueOf(1L), rows);
        // The latest values win, and the app-level config version advanced past 1.
        StorageDto loaded = controller.getStorage(testId()).getBody();
        assertEquals("AMBIENT", loaded.storageCondition);
        assertFalse(loaded.doNotRefrigerate);
        Integer version = jdbc.queryForObject("SELECT version FROM clinlims.test_sample_handling WHERE test_id = ?",
                Integer.class, TEST_ID);
        assertEquals(Integer.valueOf(2), version);
    }

    @org.junit.Test
    public void storage_unknownTestReturns404() {
        assertEquals(404, controller.getStorage("99999999").getStatusCode().value());
        assertEquals(404,
                controller.saveStorage("99999999", new StorageDto(), authedRequest()).getStatusCode().value());
    }
}
