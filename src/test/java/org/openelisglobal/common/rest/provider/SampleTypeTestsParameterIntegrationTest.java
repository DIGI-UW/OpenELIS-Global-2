package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.rest.provider.SampleEntryTestsForTypeProviderRestController.SampleEntryTests;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-1120 — {@code /rest/sample-type-tests} answered 500 when the sampleType
 * parameter was missing or not a number, and 500 for a whole sample type once
 * one of its active tests had no lab unit. A bad parameter is the caller's
 * mistake and is answered as one; a test without a lab unit is listed like the
 * others instead of taking the sample type down with it.
 */
public class SampleTypeTestsParameterIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95601L;
    private static final long LINK_ID = 95601L;
    private static final long SAMPLE_TYPE_ID = 95602L;

    @Autowired
    private SampleEntryTestsForTypeProviderRestController controller;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String sampleTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        // The shared seed is truncated by other fixtures (and the order differs per
        // runner), so the test owns its specimen type and its name localization.
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE_ID, "SectionlessIT specimen");
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, 'true', ?, ?, NOW())",
                SAMPLE_TYPE_ID, "SectionlessIT specimen", "SL" + SAMPLE_TYPE_ID % 1000, SAMPLE_TYPE_ID, SAMPLE_TYPE_ID);
        sampleTypeId = String.valueOf(SAMPLE_TYPE_ID);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", TEST_ID,
                "SectionlessIT");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, orderable, guid, lastupdated,"
                        + " name_localization_id) VALUES (?, ?, ?, 'Y', true, ?, NOW(), ?)",
                TEST_ID, "SectionlessIT", "sectionless IT", UUID.randomUUID().toString(), TEST_ID);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)", LINK_ID,
                SAMPLE_TYPE_ID, TEST_ID);
        typeOfSampleService.clearCache();
    }

    @After
    public void tearDown() {
        cleanup();
        typeOfSampleService.clearCache();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE_ID);
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (?, ?)", SAMPLE_TYPE_ID, TEST_ID);
    }

    private static MockHttpServletRequest request(String sampleType) {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        if (sampleType != null) {
            request.setParameter("sampleType", sampleType);
        }
        return request;
    }

    private ResponseEntity<Object> call(String sampleType) throws Exception {
        return controller.processRequest(request(sampleType), new MockHttpServletResponse());
    }

    private static boolean lists(SampleEntryTests body, long testId) {
        return body.getTests().stream().anyMatch(t -> String.valueOf(testId).equals(t.getId()));
    }

    @Test
    public void missingParameter_isAClientError_thatNamesTheParameter() throws Exception {
        ResponseEntity<Object> resp = call(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(String.valueOf(resp.getBody()).contains("sampleType"));
    }

    @Test
    public void nonNumericParameter_isAClientError() throws Exception {
        assertEquals(HttpStatus.BAD_REQUEST, call("abc").getStatusCode());
        ResponseEntity<Object> literalNull = call("null");
        assertEquals(HttpStatus.BAD_REQUEST, literalNull.getStatusCode());
        assertTrue(String.valueOf(literalNull.getBody()).contains("numeric"));
    }

    @Test
    public void unknownNumericId_isStillAnEmptySuccess() throws Exception {
        ResponseEntity<Object> resp = call("999999");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        SampleEntryTests body = (SampleEntryTests) resp.getBody();
        assertTrue(body.getTests().isEmpty());
        assertTrue(body.getPanels().isEmpty());
    }

    @Test
    public void activeTestWithoutLabUnit_isListed_insteadOfFailingTheSampleType() throws Exception {
        List<org.openelisglobal.test.valueholder.Test> filtered = typeOfSampleService
                .getActiveTestsBySampleTypeIdAndTestUnit(sampleTypeId, true, List.of("-1"));
        assertTrue("the unit filter must keep a test that has no lab unit",
                filtered.stream().anyMatch(t -> String.valueOf(TEST_ID).equals(t.getId())));

        ResponseEntity<Object> resp = call(sampleTypeId);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue("the order-entry list must include the sectionless test",
                lists((SampleEntryTests) resp.getBody(), TEST_ID));
    }
}
