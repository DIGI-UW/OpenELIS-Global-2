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
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.ResultComponentDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.SampleResults;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-949 M5 / OGC-749 (OGC-962) — Sample & Results result-components API,
 * round-tripped against a real DB. Exercises the diff-based save: insert new,
 * update by id, soft-delete on omit; plus the 422 validation and 404 guards.
 * Controller is instantiated directly (security slice is covered separately).
 */
public class TestCatalogEditorSampleResultsIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95201L;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController();
        ReflectionTestUtils.setField(controller, "testService", testService);
        ReflectionTestUtils.setField(controller, "componentService", componentService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "SampleResultsIT", "SampleResultsIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        } catch (Exception ignored) {
            // table absent before changeset 041
        }
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
}
