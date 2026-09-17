package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.GroupRangesUpdate;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.RangeDto;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-1118 — "Edit related tests together" writes one range set to every
 * selected test's own rows. The set is seeded from the first test, so its
 * component and specimen ids belong to that test; each other test must receive
 * its own equivalent, and every bound the editor offers must survive.
 */
public class TestCatalogEditorGroupRangesIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long SEED_TEST_ID = 95311L;
    private static final long SIBLING_TEST_ID = 95312L;

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

    private static final long SAMPLE_TYPE_ID = 95913L;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;
    private String seedComponentId;
    private String siblingComponentId;
    private String sampleTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        cleanup();
        seedTest(SEED_TEST_ID, "GroupIT seed");
        seedTest(SIBLING_TEST_ID, "GroupIT sibling");
        seedComponentId = seedPrimaryComponent(SEED_TEST_ID);
        siblingComponentId = seedPrimaryComponent(SIBLING_TEST_ID);
        // The shared specimen seed is truncated by other fixtures, so the test owns
        // the specimen type it scopes the range to.
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE_ID, "GroupIT specimen");
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, 'true', ?, ?, NOW())",
                SAMPLE_TYPE_ID, "GroupIT specimen", "GR" + SAMPLE_TYPE_ID % 1000, SAMPLE_TYPE_ID, SAMPLE_TYPE_ID);
        sampleTypeId = String.valueOf(SAMPLE_TYPE_ID);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)", SEED_TEST_ID,
                SAMPLE_TYPE_ID, SEED_TEST_ID);
        // The test-to-specimen map is cached; the link above must be visible to it.
        typeOfSampleService.clearCache();
    }

    @After
    public void tearDown() {
        cleanup();
        typeOfSampleService.clearCache();
    }

    private void cleanup() {
        for (long id : new long[] { SEED_TEST_ID, SIBLING_TEST_ID }) {
            jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE_ID);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", SAMPLE_TYPE_ID);
    }

    private void seedTest(long id, String name) {
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                + " VALUES (?, ?, ?, 'Y', ?, NOW())", id, name, name + " desc", UUID.randomUUID().toString());
    }

    private String seedPrimaryComponent(long testId) {
        String componentId = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO clinlims.test_result_component"
                        + " (id, test_id, code, label, display_order, result_type, allow_multiple_readings,"
                        + " is_primary, show_on_report, is_active, lastupdated)"
                        + " VALUES (?, ?, 'PRIMARY', 'Result', 0, 'N', false, true, true, 'Y', NOW())",
                componentId, testId);
        return componentId;
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

    private RangeDto sharedRange() {
        RangeDto r = new RangeDto();
        r.componentId = seedComponentId;
        r.sampleTypeId = sampleTypeId;
        r.gender = null;
        r.minAge = 0d;
        r.maxAge = null;
        r.lowNormal = 1d;
        r.highNormal = 5d;
        r.lowCritical = 0.5d;
        r.highCritical = 8d;
        r.lowValid = 0d;
        r.highValid = 10d;
        return r;
    }

    private List<RangeDto> rangesOf(long testId) {
        return controller.getRanges(String.valueOf(testId)).getBody().ranges;
    }

    @org.junit.Test
    public void groupSave_writesEachTestsOwnComponent_notTheSeedTests() {
        GroupRangesUpdate body = new GroupRangesUpdate();
        body.testIds = List.of(String.valueOf(SEED_TEST_ID), String.valueOf(SIBLING_TEST_ID));
        body.ranges = List.of(sharedRange());

        assertEquals(200, controller.saveGroupRanges(body, authedRequest()).getStatusCode().value());

        assertEquals(1, rangesOf(SEED_TEST_ID).size());
        assertEquals("the seed test keeps its own component", seedComponentId,
                rangesOf(SEED_TEST_ID).get(0).componentId);
        assertEquals(1, rangesOf(SIBLING_TEST_ID).size());
        assertEquals("the sibling gets its own PRIMARY, not the seed test's", siblingComponentId,
                rangesOf(SIBLING_TEST_ID).get(0).componentId);
    }

    @org.junit.Test
    public void groupSave_rejectsAComponentIdThatExistsNowhere_andWritesNothing() {
        GroupRangesUpdate body = new GroupRangesUpdate();
        body.testIds = List.of(String.valueOf(SEED_TEST_ID), String.valueOf(SIBLING_TEST_ID));
        RangeDto foreign = sharedRange();
        foreign.componentId = UUID.randomUUID().toString();
        body.ranges = List.of(foreign);

        assertEquals(422, controller.saveGroupRanges(body, authedRequest()).getStatusCode().value());
        assertEquals(0, rangesOf(SEED_TEST_ID).size());
        assertEquals(0, rangesOf(SIBLING_TEST_ID).size());
    }

    @org.junit.Test
    public void groupSave_keepsEveryBound_andScopesTheSpecimenPerTest() {
        GroupRangesUpdate body = new GroupRangesUpdate();
        body.testIds = List.of(String.valueOf(SEED_TEST_ID), String.valueOf(SIBLING_TEST_ID));
        body.ranges = List.of(sharedRange());
        controller.saveGroupRanges(body, authedRequest());

        RangeDto seed = rangesOf(SEED_TEST_ID).get(0);
        RangeDto sibling = rangesOf(SIBLING_TEST_ID).get(0);
        for (RangeDto written : List.of(seed, sibling)) {
            assertEquals(Double.valueOf(1d), written.lowNormal);
            assertEquals(Double.valueOf(5d), written.highNormal);
            assertEquals(Double.valueOf(0.5d), written.lowCritical);
            assertEquals(Double.valueOf(8d), written.highCritical);
            assertEquals(Double.valueOf(0d), written.lowValid);
            assertEquals(Double.valueOf(10d), written.highValid);
        }
        assertEquals("the seed test has that specimen, so the scope stands", sampleTypeId, seed.sampleTypeId);
        assertNull("the sibling does not have that specimen, so its range is shared across its own",
                sibling.sampleTypeId);
    }
}
