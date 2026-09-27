package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
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
 * a pathology_request at OPENED instead (FR-2.1, AC-6), and the case keeps its
 * real stage.
 *
 * <p>
 * These tests pin the two counting rules the tile depends on: a case is counted
 * once however many of its requests are open, and a case whose requests have
 * all been closed is not counted at all.
 */
public class PathologySampleOpenRequestsTest extends BaseWebContextSensitiveTest {

    /**
     * Case 2's requests are all closed, so it is counted on no tile to begin with.
     */
    private static final int CASE_WITH_NO_OPEN_REQUESTS = 2;

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
     * The case view shows a status control as soon as a request is picked, but it
     * only displays OPENED as a fallback and never writes it into the posted form,
     * so a request the operator does not touch is posted with no status. Before
     * this, that null was written straight to the row and the request was stored as
     * neither open nor closed: a case with a plainly outstanding request was
     * counted on no tile.
     */
    @Test
    public void aRequestRaisedFromTheCaseView_isOpen() {
        long before = pathologySampleService.getCountWithOpenRequests();

        raiseRequestThroughTheCaseView(CASE_WITH_NO_OPEN_REQUESTS);

        assertEquals("a request carries no status until it is answered, so one just raised is open", "OPENED",
                statusOfTheOnlyRequestOn(CASE_WITH_NO_OPEN_REQUESTS));
        assertEquals("the case now holds an outstanding request, so the tile counts it", Long.valueOf(before + 1),
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

    /**
     * Saves the case the way the case view does when an operator picks a request
     * and saves: every other collection is posted empty, and the request carries a
     * value but no status.
     */
    private void raiseRequestThroughTheCaseView(int pathologySampleId) {
        PathologySampleForm form = new PathologySampleForm();
        form.setStatus(pathologySampleService.get(pathologySampleId).getStatus());
        form.setSystemUserId("1");
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        PathologySampleForm.PathologyRequestForm request = new PathologySampleForm.PathologyRequestForm();
        request.setValue("a second opinion on the margins");
        form.setRequests(List.of(request));

        pathologySampleService.updateWithFormValues(pathologySampleId, form);
    }

    private String statusOfTheOnlyRequestOn(int pathologySampleId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM clinlims.pathology_request WHERE pathology_sample_id = ?", String.class,
                pathologySampleId);
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
