package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestReflexCalcRestController;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService.ReflexCalcView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-764 — read-only Reflex &amp; Calc endpoint. A test with no
 * reflex rules or calculations returns empty cross-link lists (not an error),
 * and an unknown test 404s.
 */
public class TestReflexCalcRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95431L;

    @Autowired
    private ReflexCalcViewService reflexCalcViewService;

    @Autowired
    private TestService testService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestReflexCalcRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestReflexCalcRestController(reflexCalcViewService, testService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "ReflexCalcIT", "ReflexCalcIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void get_noCrossLinks_returnsEmptyLists() {
        ReflexCalcView view = controller.get(String.valueOf(TEST_ID));
        assertNotNull(view);
        assertNotNull(view.reflexRules);
        assertNotNull(view.calculatedBy);
        assertNotNull(view.feedsInto);
        assertTrue(view.reflexRules.isEmpty());
    }

    @Test
    public void get_unknownTest_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.get("99999999"));
        assertEquals(404, ex.getStatusCode().value());
    }
}
