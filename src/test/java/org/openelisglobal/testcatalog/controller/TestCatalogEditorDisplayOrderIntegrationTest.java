package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.DisplayOrderResponse;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.DisplayOrderUpdate;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.SampleTypeOption;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.TestOrderItem;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M12 / OGC-983..985 — Display Order API, round-tripped against a real
 * DB. Verifies the per-sample-type ordered read, the auto-save reorder
 * (persisted to {@code sampletype_test.display_order}), and the 404 guard.
 *
 * <p>
 * Uses an existing (Liquibase-seeded) sample type — the established pattern,
 * and cleaner than seeding the generic-localization stack a new sample type
 * needs. My two tests are seeded with high display orders so they sit after any
 * pre-existing tests; assertions are on the persisted column + the relative
 * order of my two tests, so pre-existing rows under the type don't matter.
 */
public class TestCatalogEditorDisplayOrderIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_A = 95432L;
    private static final long TEST_B = 95433L;
    private static final long JUNCTION_A = 95434L;
    private static final long JUNCTION_B = 95435L;
    private static final int ORDER_HIGH_A = 9002;
    private static final int ORDER_HIGH_B = 9001;

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
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;
    private String sampleTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                analyzerTestMappingService, typeOfSampleService, typeOfSampleTestService, terminologyService);
        // Use the first sort-ordered sample type — guaranteed to appear in the picker.
        List<TypeOfSample> types = typeOfSampleService.getAllTypeOfSamplesSortOrdered();
        Assume.assumeFalse("needs a Liquibase-seeded sample type", types.isEmpty());
        sampleTypeId = types.get(0).getId();
        cleanup();
        for (long id : new long[] { TEST_A, TEST_B }) {
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                            + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                    id, "DOIT-Test-" + id, "DOIT test " + id, UUID.randomUUID().toString());
        }
        // A at the higher position (9002), B lower (9001) → the read returns B before
        // A.
        long sampleTypeIdNum = Long.parseLong(sampleTypeId);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, display_order)"
                + " VALUES (?, ?, ?, ?)", JUNCTION_A, sampleTypeIdNum, TEST_A, ORDER_HIGH_A);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, display_order)"
                + " VALUES (?, ?, ?, ?)", JUNCTION_B, sampleTypeIdNum, TEST_B, ORDER_HIGH_B);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE id IN (?, ?)", JUNCTION_A, JUNCTION_B);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TEST_A, TEST_B);
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

    private int indexOf(DisplayOrderResponse resp, long testId) {
        for (int i = 0; i < resp.tests.size(); i++) {
            if (String.valueOf(testId).equals(resp.tests.get(i).testId)) {
                return i;
            }
        }
        return -1;
    }

    private Integer persistedOrder(long testId) {
        return jdbc.queryForObject(
                "SELECT display_order FROM clinlims.sampletype_test WHERE sample_type_id = ? AND test_id = ?",
                Integer.class, Long.parseLong(sampleTypeId), testId);
    }

    @org.junit.Test
    public void getTestOrder_returnsMyTestsSortedByDisplayOrder() {
        ResponseEntity<DisplayOrderResponse> resp = controller.getTestOrder(sampleTypeId);
        assertEquals(200, resp.getStatusCode().value());
        int idxA = indexOf(resp.getBody(), TEST_A);
        int idxB = indexOf(resp.getBody(), TEST_B);
        assertTrue("both seeded tests present", idxA >= 0 && idxB >= 0);
        // B (9001) sorts before A (9002).
        assertTrue("B before A by display order", idxB < idxA);
        assertEquals(Integer.valueOf(ORDER_HIGH_A), resp.getBody().tests.get(idxA).displayOrder);
        assertNotNull(resp.getBody().tests.get(idxA).testName);
    }

    @org.junit.Test
    public void saveTestOrder_persistsNewOrder_andReReadReflectsIt() {
        // Flip: A→9001, B→9002.
        DisplayOrderUpdate update = new DisplayOrderUpdate();
        TestOrderItem a = new TestOrderItem();
        a.testId = String.valueOf(TEST_A);
        a.displayOrder = ORDER_HIGH_B;
        TestOrderItem b = new TestOrderItem();
        b.testId = String.valueOf(TEST_B);
        b.displayOrder = ORDER_HIGH_A;
        update.items.add(a);
        update.items.add(b);

        ResponseEntity<DisplayOrderResponse> saved = controller.saveTestOrder(sampleTypeId, update, authedRequest());
        assertEquals(200, saved.getStatusCode().value());
        // Column actually written (catches an unmapped hbm property).
        assertEquals(Integer.valueOf(ORDER_HIGH_B), persistedOrder(TEST_A));
        assertEquals(Integer.valueOf(ORDER_HIGH_A), persistedOrder(TEST_B));
        // Re-read reflects the flip: A now before B.
        assertTrue("A before B after flip", indexOf(saved.getBody(), TEST_A) < indexOf(saved.getBody(), TEST_B));
    }

    @org.junit.Test
    public void listSampleTypes_includesTheSampleType() {
        boolean found = false;
        for (SampleTypeOption o : controller.listSampleTypes()) {
            if (sampleTypeId.equals(o.id)) {
                found = true;
            }
        }
        assertTrue("the sample type should appear in the picker list", found);
    }

    @org.junit.Test
    public void getTestOrder_unknownSampleTypeReturns404() {
        assertEquals(404, controller.getTestOrder("99999999").getStatusCode().value());
    }
}
