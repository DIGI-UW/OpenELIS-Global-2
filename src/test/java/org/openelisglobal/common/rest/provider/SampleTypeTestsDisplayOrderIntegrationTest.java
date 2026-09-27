package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.rest.provider.SampleEntryTestsForTypeProviderRestController.SampleEntryTests;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-1238 — order entry lists a sample type's tests in the order set on the
 * Test Catalog's Display Order (sampletype_test.display_order), falling back to
 * the global test sort order and then the name.
 */
public class SampleTypeTestsDisplayOrderIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE_TYPE_ID = 95611L;
    private static final long TEST_A = 95612L;
    private static final long TEST_B = 95613L;
    private static final long TEST_C = 95614L;
    private static final long[] TESTS = { TEST_A, TEST_B, TEST_C };

    @Autowired
    private SampleEntryTestsForTypeProviderRestController controller;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE_ID, "DisplayOrderIT specimen");
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, 'true', ?, ?, NOW())",
                SAMPLE_TYPE_ID, "DisplayOrderIT specimen", "DO" + SAMPLE_TYPE_ID % 1000, SAMPLE_TYPE_ID,
                SAMPLE_TYPE_ID);
        seedTest(TEST_A, "DisplayOrderIT A", 1);
        seedTest(TEST_B, "DisplayOrderIT B", 2);
        seedTest(TEST_C, "DisplayOrderIT C", 3);
        typeOfSampleService.clearCache();
    }

    @After
    public void tearDown() {
        cleanup();
        typeOfSampleService.clearCache();
    }

    private void cleanup() {
        for (long id : TESTS) {
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", id);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE_ID);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", SAMPLE_TYPE_ID);
    }

    private void seedTest(long id, String name, int sortOrder) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id, name);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, orderable, guid, lastupdated,"
                        + " name_localization_id, sort_order) VALUES (?, ?, ?, 'Y', true, ?, NOW(), ?, ?)",
                id, name, name + " desc", UUID.randomUUID().toString(), id, sortOrder);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)", id,
                SAMPLE_TYPE_ID, id);
    }

    private void setDisplayOrder(long testId, Integer displayOrder) {
        jdbc.update("UPDATE clinlims.sampletype_test SET display_order = ? WHERE test_id = ?", displayOrder, testId);
        typeOfSampleService.clearCache();
    }

    private List<String> listedTestIds() throws Exception {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.setParameter("sampleType", String.valueOf(SAMPLE_TYPE_ID));
        SampleEntryTests body = (SampleEntryTests) controller.processRequest(request, new MockHttpServletResponse())
                .getBody();
        return body.getTests().stream().map(t -> t.getId()).collect(Collectors.toList());
    }

    private static List<String> ids(long... testIds) {
        return java.util.Arrays.stream(testIds).mapToObj(String::valueOf).collect(Collectors.toList());
    }

    @Test
    public void withoutDisplayOrder_testsFollowTheGlobalSortOrder() throws Exception {
        assertEquals(ids(TEST_A, TEST_B, TEST_C), listedTestIds());
    }

    @Test
    public void displayOrder_winsOverTheGlobalSortOrder() throws Exception {
        setDisplayOrder(TEST_C, 1);
        setDisplayOrder(TEST_A, 2);
        setDisplayOrder(TEST_B, 3);

        assertEquals(ids(TEST_C, TEST_A, TEST_B), listedTestIds());
    }

    @Test
    public void testsWithoutADisplayOrder_followThoseWithOne() throws Exception {
        setDisplayOrder(TEST_C, 1);

        assertEquals(ids(TEST_C, TEST_A, TEST_B), listedTestIds());
    }
}
