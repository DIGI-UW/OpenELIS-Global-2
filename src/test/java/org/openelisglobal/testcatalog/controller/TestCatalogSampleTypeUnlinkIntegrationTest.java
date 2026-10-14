package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.management.controller.rest.SampleTypeManagementRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1460: the Sample Type side cannot take the last sample type away from an
 * active or orderable test, the same rule as the test's own Basic Info save.
 */
public class TestCatalogSampleTypeUnlinkIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95461L;

    private static final long SERUM = 95462L;

    private static final long PLASMA = 95463L;

    @Autowired
    private javax.sql.DataSource dataSource;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private org.openelisglobal.sampletypeterminology.service.SampleTypeTerminologyMappingService terminologyService;
    @Autowired
    private org.openelisglobal.test.service.TestService testService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;

    private SampleTypeManagementRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new SampleTypeManagementRestController();
        ReflectionTestUtils.setField(controller, "typeOfSampleService", typeOfSampleService);
        ReflectionTestUtils.setField(controller, "terminologyService", terminologyService);
        ReflectionTestUtils.setField(controller, "testService", testService);
        ReflectionTestUtils.setField(controller, "typeOfSampleTestService", typeOfSampleTestService);
        cleanup();
        for (long id : new long[] { SERUM, PLASMA }) {
            jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                    "UnlinkIT " + id);
            jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                    + " sort_order, name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, 'true', 1, ?, NOW())", id,
                    "UnlinkIT " + id, "UL" + id, id);
        }
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, orderable, guid, lastupdated)"
                        + " VALUES (?, 'UnlinkIT', 'UnlinkIT desc', 'Y', true, ?, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)", SERUM, SERUM,
                TEST_ID);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)", PLASMA,
                PLASMA, TEST_ID);
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
        for (long id : new long[] { SERUM, PLASMA }) {
            jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", id);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", id);
        }
    }

    private static MockHttpServletRequest request() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private int unlink(long sampleTypeId) {
        return controller.removeTestFromSampleType(request(), String.valueOf(sampleTypeId), String.valueOf(TEST_ID))
                .getStatusCode().value();
    }

    private int links() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.sampletype_test WHERE test_id = ?", Integer.class,
                TEST_ID);
    }

    @org.junit.Test
    public void anActiveTestKeepsItsLastSampleType() {
        assertEquals("another sample type remains, so this one may go", 200, unlink(SERUM));
        assertEquals(1, links());

        assertEquals(422, unlink(PLASMA));
        assertEquals("the last sample type is still linked", 1, links());
    }

    @org.junit.Test
    public void anInactiveTestMayLoseItsLastSampleType() {
        jdbc.update("UPDATE clinlims.test SET is_active = 'N', orderable = false WHERE id = ?", TEST_ID);

        assertEquals(200, unlink(SERUM));
        assertEquals(200, unlink(PLASMA));
        assertEquals(0, links());
    }

    @org.junit.Test
    public void aMalformedTestIdIsA404() {
        assertEquals(404,
                controller.removeTestFromSampleType(request(), String.valueOf(SERUM), "abc").getStatusCode().value());
        assertEquals(2, links());
    }
}
