package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An outstanding pathologist request is a fact about the case's requests, not a
 * stage of the bench.
 *
 * <p>
 * The retired ADDITIONAL_REQUEST status parked a case in a stage that does not
 * exist on the bench purely to record that a request was open, so for as long
 * as the request stood the case no longer reported where its tissue actually
 * was. The dashboard's "additional requests" tile now counts the cases holding
 * a pathology_request at OPENED instead (OGC-264, FR-2.2, AC-6), and the case
 * keeps its real stage.
 *
 * <p>
 * These tests pin the two counting rules the tile depends on: a case is counted
 * once however many of its requests are open, and a case whose requests have
 * all been closed is not counted at all.
 */
public class PathologySampleOpenRequestsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PathologySampleService pathologySampleService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        cleanup();

        // Case 1 already holds the fixture's open request 301, so a second open one
        // proves the case is still counted only once.
        insertRequest(9301, 1, "OPENED");
        insertRequest(9302, 1, "COMPLETED");
        // Case 2 holds requests, but none of them are still open.
        insertRequest(9303, 2, "COMPLETED");
        insertRequest(9304, 2, "CANCELLED");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void getCountWithOpenRequests_countsACaseOnceHoweverManyRequestsAreOpen() {
        Long count = pathologySampleService.getCountWithOpenRequests();

        assertEquals("a case with two open requests is one case on the tile", Long.valueOf(1L), count);
    }

    @Test
    public void getCountWithOpenRequests_ignoresACaseWhoseRequestsAreAllClosed() {
        Long count = pathologySampleService.getCountWithOpenRequests();

        assertEquals("only the case holding an open request is counted", Long.valueOf(1L), count);
        assertNotEquals("a case whose requests are all COMPLETED or CANCELLED is not counted", Long.valueOf(2L), count);
    }

    @Test
    public void getCountWithOpenRequests_isZeroWhenNothingIsOpen() {
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = 'COMPLETED' WHERE status = 'OPENED'");

        Long count = pathologySampleService.getCountWithOpenRequests();

        assertEquals("no open request anywhere empties the tile", Long.valueOf(0L), count);
    }

    @Test
    public void getCountWithOpenRequests_countsEachCaseWithAnOpenRequest() {
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = 'OPENED' WHERE id = 9303");

        Long count = pathologySampleService.getCountWithOpenRequests();

        assertEquals("each case holding an open request adds one", Long.valueOf(2L), count);
    }

    // helpers

    /**
     * The id is the table's only NOT NULL column, but the entity reads status and
     * type as enum names, so both are supplied with names those enums declare.
     */
    private void insertRequest(int id, int pathologySampleId, String status) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.pathology_request (id, pathology_sample_id, status, type, value, last_updated)"
                        + " VALUES (?, ?, ?, 'TEXT', ?, now())",
                id, pathologySampleId, status, "Request " + id);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_request WHERE id BETWEEN 9301 AND 9304");
    }
}
