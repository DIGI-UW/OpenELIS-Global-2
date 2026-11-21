package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.RequestDraft;
import org.openelisglobal.program.service.RequestStatusChange;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
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
 * a pathology_request at OPENED instead (FR-2.1, AC-6), and the case keeps its
 * real stage.
 *
 * <p>
 * These tests pin the two counting rules the tile depends on: a case is counted
 * once however many of its requests are open, and a case whose requests have
 * all been closed is not counted at all.
 */
@org.springframework.transaction.annotation.Transactional
public class PathologySampleOpenRequestsTest extends BaseWebContextSensitiveTest {

    /**
     * Case 2's requests are all closed, so it is counted on no tile to begin with.
     */
    private static final int CASE_WITH_NO_OPEN_REQUESTS = 2;

    /** technician1, the fixture's own user. */
    private static final String TECHNICIAN = "1001";

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
        // Case 1's requests, previously open, are closed here so it stands alongside
        // case 2 as a second case whose requests are all COMPLETED or CANCELLED.
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = 'COMPLETED' WHERE pathology_sample_id = 1");

        Long count = pathologySampleService.getCountWithOpenRequests();

        assertEquals("case 2, which still holds only COMPLETED and CANCELLED requests, is not counted",
                Long.valueOf(0L), count);
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

    /**
     * A request is raised on its own now, never by a save of the case, and one just
     * raised is open. Before, the case view posted a picked request with no status
     * and the save wrote that null straight to the row, so a plainly outstanding
     * request was counted on no tile.
     */
    @Test
    public void aRequestRaisedThroughTheService_isOpenAndCounted() {
        long before = pathologySampleService.getCountWithOpenRequests();

        PathologyRequest raised = raiseOn(CASE_WITH_NO_OPEN_REQUESTS, "a second opinion on the margins");

        assertEquals("a request just raised is open", "OPENED", statusOf(raised.getId()));
        assertEquals("the case now holds an outstanding request, so the tile counts it", Long.valueOf(before + 1),
                pathologySampleService.getCountWithOpenRequests());
    }

    /**
     * The tile counts a case with a raised request once, however many are raised,
     * and stops counting it once each is completed or cancelled.
     */
    @Test
    public void aCaseWithRaisedRequests_isCountedOnceUntilEachIsClosed() {
        long before = pathologySampleService.getCountWithOpenRequests();
        PathologyRequest first = raiseOn(CASE_WITH_NO_OPEN_REQUESTS, "another block from the margin");
        PathologyRequest second = raiseOn(CASE_WITH_NO_OPEN_REQUESTS, "a further block from the base");

        assertEquals("two raised requests count their case once", Long.valueOf(before + 1),
                pathologySampleService.getCountWithOpenRequests());

        pathologySampleService.changeRequestStatus(CASE_WITH_NO_OPEN_REQUESTS, first.getId(),
                new RequestStatusChange("OPENED", "COMPLETED", null, null, null), TECHNICIAN);
        assertEquals("one still open keeps the case on the tile", Long.valueOf(before + 1),
                pathologySampleService.getCountWithOpenRequests());

        pathologySampleService.changeRequestStatus(CASE_WITH_NO_OPEN_REQUESTS, second.getId(),
                new RequestStatusChange("OPENED", "CANCELLED", "not needed", null, null), TECHNICIAN);
        assertEquals("with both closed the case leaves the tile", Long.valueOf(before),
                pathologySampleService.getCountWithOpenRequests());
    }

    /**
     * The same rule applied to the rows already in the table. The case view has
     * always shown a status-less request as open and has always let an operator
     * answer it, so the backfill changeset makes the stored value agree with what
     * the screen was showing rather than changing any behaviour.
     */
    @Test
    public void migration_makesAStatuslessRequestOpen() {
        insertRequest(9305, CASE_WITH_NO_OPEN_REQUESTS, null);
        assertEquals("the row starts with no status, which is the state the changeset exists to remove", 0,
                openRequestCountOn(CASE_WITH_NO_OPEN_REQUESTS));

        runBackfill();

        assertEquals("a request that was stored without a status reads as open afterwards", 1,
                openRequestCountOn(CASE_WITH_NO_OPEN_REQUESTS));
        assertEquals("the case holding it is now counted on the tile", Long.valueOf(2L),
                pathologySampleService.getCountWithOpenRequests());
    }

    // helpers

    /** The SQL equivalent of 002-pathology-request-status-backfill. */
    private void runBackfill() {
        jdbcTemplate.execute("UPDATE clinlims.pathology_request SET status = 'OPENED' WHERE status IS NULL");
    }

    private int openRequestCountOn(int pathologySampleId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.pathology_request"
                + " WHERE pathology_sample_id = ? AND status = 'OPENED'", Integer.class, pathologySampleId);
    }

    private PathologyRequest raiseOn(int pathologySampleId, String instruction) {
        return pathologySampleService.raiseRequest(pathologySampleId,
                new RequestDraft("BLOCK", null, null, null, null, instruction), TECHNICIAN);
    }

    private String statusOf(int requestId) {
        return jdbcTemplate.queryForObject("SELECT status FROM clinlims.pathology_request WHERE id = ?", String.class,
                requestId);
    }

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

    /**
     * Only the ids this class inserts are removed here. A test that mutates the
     * fixture row (id 301) leaves that mutation in place; the next
     * {@code executeDataSetWithStateManagement} load, run by every consumer of
     * testdata/pathology-sample.xml in its own {@code @Before}, restores it.
     */
    private void cleanup() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_request WHERE id BETWEEN 9301 AND 9399");
    }
}
