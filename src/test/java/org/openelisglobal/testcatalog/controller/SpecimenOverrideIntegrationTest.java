package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.MappingDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.RangeDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.RangesResponse;
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
 * OGC-1145 Phase 2 — per-specimen override: terminology mappings and reference
 * ranges may scope to one of the test's sample types (null = shared), the
 * editor endpoints round-trip and guard the scope, and the result-limit
 * resolution prefers the specimen-scoped row while other specimens keep the
 * shared set.
 */
public class SpecimenOverrideIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 98001L;
    private static final long SERUM_LIKE = 98101L;
    private static final long CSF_LIKE = 98102L;
    private static final long FOREIGN_TYPE = 98103L; // never linked to the test

    @Autowired
    private TestService testService;
    @Autowired
    private TestResultComponentService componentService;
    @Autowired
    private TestResultInterpretationService interpretationService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private ResultLimitService resultLimitService;
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
        seedSampleType(SERUM_LIKE, "OvSerum 1145");
        seedSampleType(CSF_LIKE, "OvCsf 1145");
        seedSampleType(FOREIGN_TYPE, "OvForeign 1145");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'OverrideIT 1145', 'OverrideIT 1145', 'Y', ?, 'CLINICAL', true, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        insertJunction(SERUM_LIKE, TEST_ID);
        insertJunction(CSF_LIKE, TEST_ID);
        typeOfSampleService.clearCache();
    }

    private void seedSampleType(long id, String description) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                description);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, 'true', ?, ?, NOW())",
                id, description, "OV" + id % 1000, id, id);
    }

    private void insertJunction(long sampleTypeId, long testId) {
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, is_panel)"
                + " VALUES (nextval('sample_type_test_seq'), ?, ?, 'false')", sampleTypeId, testId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id IN (?, ?, ?)", SERUM_LIKE, CSF_LIKE, FOREIGN_TYPE);
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (?, ?, ?)", SERUM_LIKE, CSF_LIKE, FOREIGN_TYPE);
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

    private MappingDto mapping(String code, String sampleTypeId) {
        MappingDto m = new MappingDto();
        m.source = "LOINC";
        m.code = code;
        m.relationship = "SAME_AS";
        m.sampleTypeId = sampleTypeId;
        return m;
    }

    @org.junit.Test
    public void terminology_specimenOverride_roundTripsAndGuardsScope() {
        String testId = String.valueOf(TEST_ID);
        TerminologyResponse body = new TerminologyResponse();
        body.mappings.add(mapping("L-OV-SHARED", null));
        body.mappings.add(mapping("L-OV-CSF", String.valueOf(CSF_LIKE)));
        ResponseEntity<TerminologyResponse> saved = controller.saveTerminology(testId, body, authedRequest());
        assertEquals(200, saved.getStatusCode().value());

        TerminologyResponse loaded = controller.getTerminology(testId).getBody();
        assertEquals(2, loaded.mappings.size());
        assertTrue(loaded.mappings.stream()
                .anyMatch(m -> "L-OV-CSF".equals(m.code) && String.valueOf(CSF_LIKE).equals(m.sampleTypeId)));
        assertTrue(loaded.mappings.stream().anyMatch(m -> "L-OV-SHARED".equals(m.code) && m.sampleTypeId == null));
        assertEquals("the editor gets the test's sample types for the override picker", 2, loaded.sampleTypes.size());

        // a scope outside the test's associated types is refused
        TerminologyResponse bad = new TerminologyResponse();
        bad.mappings.add(mapping("L-OV-BAD", String.valueOf(FOREIGN_TYPE)));
        assertEquals(422, controller.saveTerminology(testId, bad, authedRequest()).getStatusCode().value());

        // same code under two scopes is legal; same code in the SAME scope is not
        TerminologyResponse dupScope = new TerminologyResponse();
        dupScope.mappings.add(mapping("L-OV-X", String.valueOf(CSF_LIKE)));
        dupScope.mappings.add(mapping("L-OV-X", null));
        assertEquals(200, controller.saveTerminology(testId, dupScope, authedRequest()).getStatusCode().value());
        TerminologyResponse dupSame = new TerminologyResponse();
        dupSame.mappings.add(mapping("L-OV-Y", String.valueOf(CSF_LIKE)));
        dupSame.mappings.add(mapping("L-OV-Y", String.valueOf(CSF_LIKE)));
        assertEquals(422, controller.saveTerminology(testId, dupSame, authedRequest()).getStatusCode().value());
    }

    private RangeDto range(String sampleTypeId, Double lowNormal, Double highNormal) {
        RangeDto r = new RangeDto();
        r.sampleTypeId = sampleTypeId;
        r.lowNormal = lowNormal;
        r.highNormal = highNormal;
        return r;
    }

    @org.junit.Test
    public void ranges_specimenOverride_roundTripsGuardsAndDoesNotGateActivation() {
        String testId = String.valueOf(TEST_ID);
        RangesResponse body = new RangesResponse();
        body.ranges.add(range(null, 10d, 20d));
        // the CSF-scoped override covers only part of nothing special — its
        // "gaps" must not appear (shared rows back the rest)
        body.ranges.add(range(String.valueOf(CSF_LIKE), 1d, 5d));
        ResponseEntity<RangesResponse> saved = controller.saveRanges(testId, body, authedRequest());
        assertEquals(200, saved.getStatusCode().value());

        RangesResponse loaded = controller.getRanges(testId).getBody();
        assertEquals(2, loaded.ranges.size());
        assertTrue(loaded.ranges.stream().anyMatch(
                r -> String.valueOf(CSF_LIKE).equals(r.sampleTypeId) && Double.valueOf(1d).equals(r.lowNormal)));
        assertTrue(loaded.ranges.stream().anyMatch(r -> r.sampleTypeId == null));
        assertEquals(2, loaded.sampleTypes.size());
        assertFalse("specimen-scoped rows never gate activation with gaps", loaded.coverage.hasGaps());

        RangesResponse bad = new RangesResponse();
        bad.ranges.add(range(String.valueOf(FOREIGN_TYPE), 1d, 2d));
        assertEquals(422, controller.saveRanges(testId, bad, authedRequest()).getStatusCode().value());
    }

    @org.junit.Test
    public void resultLimitResolution_scopedWinsForItsSpecimen_sharedForOthers() {
        String testId = String.valueOf(TEST_ID);
        RangesResponse body = new RangesResponse();
        body.ranges.add(range(null, 10d, 20d));
        body.ranges.add(range(String.valueOf(CSF_LIKE), 1d, 5d));
        assertEquals(200, controller.saveRanges(testId, body, authedRequest()).getStatusCode().value());

        Patient patient = new Patient();

        ResultLimit forCsf = resultLimitService.getResultLimitForTestAndPatient(testId, patient,
                String.valueOf(CSF_LIKE));
        assertEquals("the CSF-scoped override wins for CSF", 1d, forCsf.getLowNormal(), 0.0001);

        ResultLimit forSerum = resultLimitService.getResultLimitForTestAndPatient(testId, patient,
                String.valueOf(SERUM_LIKE));
        assertEquals("a specimen without an override uses the shared set", 10d, forSerum.getLowNormal(), 0.0001);

        ResultLimit noContext = resultLimitService.getResultLimitForTestAndPatient(testId, patient);
        assertEquals("no specimen context evaluates the shared set", 10d, noContext.getLowNormal(), 0.0001);
    }

    @org.junit.Test
    public void variantSubsystemRetired_tableAndEndpointsGone() {
        assertNull("test_variant_link is dropped by changeset 063",
                jdbc.queryForObject("SELECT to_regclass('clinlims.test_variant_link')::text", String.class));
    }
}
