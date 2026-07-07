package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.RangeDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.RangesResponse;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService.Status;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M7 / OGC-969..973 — reference-ranges API + coverage validation,
 * round-tripped against a real DB. Exercises the diff-based save (insert /
 * update-by-id / delete-on-omit), the per-sex coverage report computed on every
 * load (the activation gate's input), and the 404 / 422 guards.
 *
 * Ages are in DAYS — the unit the legacy result_limits schema stores.
 */
public class TestCatalogEditorRangesIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95301L;

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
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'N', ?, NOW())",
                TEST_ID, "RangesIT", "RangesIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
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

    private static RangeDto range(String id, String gender, Double minAge, Double maxAge) {
        RangeDto r = new RangeDto();
        r.id = id;
        r.gender = gender;
        r.minAge = minAge;
        r.maxAge = maxAge;
        return r;
    }

    private static RangesResponse body(RangeDto... ranges) {
        RangesResponse resp = new RangesResponse();
        for (RangeDto r : ranges) {
            resp.ranges.add(r);
        }
        return resp;
    }

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    @org.junit.Test
    public void getRanges_emptyReturns200WithEmptyCoverage() {
        ResponseEntity<RangesResponse> resp = controller.getRanges(testId());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().ranges.isEmpty());
        // No ranges at all → neither sex is covered (EMPTY), and the gate is open
        // (no gaps, because there is nothing claiming to cover anything).
        assertEquals(Status.EMPTY, resp.getBody().coverage.male.status);
        assertEquals(Status.EMPTY, resp.getBody().coverage.female.status);
        assertTrue(!resp.getBody().coverage.hasGaps());
    }

    @org.junit.Test
    public void saveRanges_insertsRanges_andGetRoundTripsThem() {
        RangesResponse put = body(range(null, "M", 0d, 30d), range(null, "F", 0d, 30d));
        ResponseEntity<RangesResponse> saved = controller.saveRanges(testId(), put, authedRequest());
        assertEquals(200, saved.getStatusCode().value());

        RangesResponse loaded = controller.getRanges(testId()).getBody();
        assertEquals(2, loaded.ranges.size());
        RangeDto male = loaded.ranges.stream().filter(r -> "M".equals(r.gender)).findFirst().get();
        assertEquals(Double.valueOf(0d), male.minAge);
        assertEquals(Double.valueOf(30d), male.maxAge);
    }

    @org.junit.Test
    public void saveRanges_openEndedMaxAgeRoundTripsAsNull_andCoversToInfinity() {
        // maxAge null → open-ended; a single 0..∞ all-sex range fully covers both.
        controller.saveRanges(testId(), body(range(null, null, 0d, null)), authedRequest());

        RangesResponse loaded = controller.getRanges(testId()).getBody();
        assertEquals(1, loaded.ranges.size());
        assertNull("an open-ended max age must serialize as null, not Infinity", loaded.ranges.get(0).maxAge);
        assertEquals(Status.COMPLETE, loaded.coverage.male.status);
        assertEquals(Status.COMPLETE, loaded.coverage.female.status);
    }

    @org.junit.Test
    public void saveRanges_updatesExistingRangeById_withoutCreatingARow() {
        controller.saveRanges(testId(), body(range(null, "M", 0d, 30d)), authedRequest());
        RangeDto existing = controller.getRanges(testId()).getBody().ranges.get(0);

        // Re-PUT with the captured id and a widened max age.
        controller.saveRanges(testId(), body(range(existing.id, "M", 0d, 60d)), authedRequest());

        RangesResponse after = controller.getRanges(testId()).getBody();
        assertEquals("must update in place, not add a row", 1, after.ranges.size());
        assertEquals(existing.id, after.ranges.get(0).id);
        assertEquals(Double.valueOf(60d), after.ranges.get(0).maxAge);
    }

    @org.junit.Test
    public void saveRanges_omittingARangeDeletesIt() {
        controller.saveRanges(testId(), body(range(null, "M", 0d, 30d), range(null, "F", 0d, 30d)), authedRequest());
        RangeDto maleRange = controller.getRanges(testId()).getBody().ranges.stream().filter(r -> "M".equals(r.gender))
                .findFirst().get();

        // Re-PUT with only the male range → the female range is removed.
        controller.saveRanges(testId(), body(range(maleRange.id, "M", 0d, 30d)), authedRequest());

        RangesResponse after = controller.getRanges(testId()).getBody();
        assertEquals(1, after.ranges.size());
        assertEquals("M", after.ranges.get(0).gender);
        Long remaining = jdbc.queryForObject("SELECT count(*) FROM clinlims.result_limits WHERE test_id = ?",
                Long.class, TEST_ID);
        assertEquals(Long.valueOf(1L), remaining);
    }

    @org.junit.Test
    public void saveRanges_withAnUncoveredAgeWindow_reportsAMaleGap() {
        // Male covered 0–30d and 60d–open-ended, leaving days 30–60 UNCOVERED — the
        // kind of gap the activation safety gate must catch. The top band is
        // open-ended so the only gap under test is [30,60] (a finite top would also
        // leave an uncovered tail — see the coverage unit tests). No female ranges →
        // female EMPTY.
        RangesResponse put = body(range(null, "M", 0d, 30d), range(null, "M", 60d, null));
        RangesResponse saved = controller.saveRanges(testId(), put, authedRequest()).getBody();

        assertEquals(Status.GAP, saved.coverage.male.status);
        assertEquals(1, saved.coverage.male.gaps.size());
        assertEquals(30d, saved.coverage.male.gaps.get(0).fromAge, 1e-9);
        assertEquals(60d, saved.coverage.male.gaps.get(0).toAge, 1e-9);
        assertTrue("a male gap must open the activation gate", saved.coverage.hasGaps());
        assertEquals(Status.EMPTY, saved.coverage.female.status);

        // The gap survives a reload (coverage is recomputed from the persisted rows).
        assertTrue(controller.getRanges(testId()).getBody().coverage.hasGaps());
    }

    @org.junit.Test
    public void saveRanges_preservesDictionaryLimitsAndReportingBounds() {
        // The Ranges editor manages only NUMERIC ranges. Seed a non-numeric
        // (dictionary) limit via the service — it must survive a ranges save.
        Long dictTypeId = jdbc
                .queryForObject("SELECT id FROM clinlims.type_of_test_result WHERE test_result_type = 'D'", Long.class);
        org.openelisglobal.resultlimits.valueholder.ResultLimit dict = new org.openelisglobal.resultlimits.valueholder.ResultLimit();
        dict.setTestId(testId());
        dict.setResultTypeId(String.valueOf(dictTypeId));
        dict.setSysUserId("1");
        resultLimitService.insert(dict);

        // Save a numeric range, then set a reporting bound the editor never edits.
        controller.saveRanges(testId(), body(range(null, "M", 0d, 30d)), authedRequest());
        RangeDto numeric = controller.getRanges(testId()).getBody().ranges.get(0);
        jdbc.update("UPDATE clinlims.result_limits SET low_reporting_range = 1.5 WHERE id = ?",
                Long.valueOf(numeric.id));

        // Edit the numeric range again through the editor.
        controller.saveRanges(testId(), body(range(numeric.id, "M", 0d, 60d)), authedRequest());

        // The dictionary limit was not deleted by the numeric diff-save.
        Long dictRows = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.result_limits WHERE test_id = ? AND test_result_type_id = ?", Long.class,
                TEST_ID, dictTypeId);
        assertEquals("dictionary limit must be preserved", Long.valueOf(1L), dictRows);
        // The reporting bound on the edited numeric range was preserved, not wiped.
        Double lowReporting = jdbc.queryForObject("SELECT low_reporting_range FROM clinlims.result_limits WHERE id = ?",
                Double.class, Long.valueOf(numeric.id));
        assertEquals(1.5d, lowReporting, 1e-9);
    }

    @org.junit.Test
    public void saveRanges_unknownTestReturns404() {
        assertEquals(404, controller.getRanges("99999999").getStatusCode().value());
        assertEquals(404, controller.saveRanges("99999999", body(range(null, "M", 0d, 30d)), authedRequest())
                .getStatusCode().value());
    }

    @org.junit.Test
    public void saveRanges_invalidGenderOrAgeWindowReturns422() {
        // Unknown gender code.
        assertEquals(422, controller.saveRanges(testId(), body(range(null, "X", 0d, 30d)), authedRequest())
                .getStatusCode().value());
        // maxAge not strictly greater than minAge.
        assertEquals(422, controller.saveRanges(testId(), body(range(null, "M", 30d, 30d)), authedRequest())
                .getStatusCode().value());
        // Negative minAge.
        assertEquals(422, controller.saveRanges(testId(), body(range(null, "M", -1d, 30d)), authedRequest())
                .getStatusCode().value());
    }
}
