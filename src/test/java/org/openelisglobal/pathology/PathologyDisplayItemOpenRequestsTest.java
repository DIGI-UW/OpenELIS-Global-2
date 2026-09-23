package org.openelisglobal.pathology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The dashboard and the case view have to show that a pathologist request is
 * outstanding while still showing the case at the stage its tissue is actually
 * at. The retired ADDITIONAL_REQUEST status could only do the first by giving
 * up the second, so the display items now carry the request as a fact derived
 * from the case's own pathology_request rows (FR-2.1, AC-6).
 *
 * <p>
 * These tests drive the derivation through the service that builds the display
 * item, against the fixture's two cases: case 1 holds an open request and case
 * 2 holds only a closed one. The case view's display item is exercised only
 * once, since it inherits the flag from {@link PathologyDisplayItem} and the
 * service sets it through the same helper.
 */
public class PathologyDisplayItemOpenRequestsTest extends BaseWebContextSensitiveTest {

    private static final int CASE_WITH_AN_OPEN_REQUEST = 1;
    private static final int CASE_WITH_ONLY_A_CLOSED_REQUEST = 2;
    private static final int THE_OPEN_REQUEST = 301;

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
    }

    @Test
    public void displayItem_flagsACaseHoldingAnOpenRequest() {
        PathologyDisplayItem displayItem = pathologyDisplayService.convertToDisplayItem(CASE_WITH_AN_OPEN_REQUEST);

        assertTrue("a case holding a request at OPENED reports an outstanding request",
                displayItem.isHasOpenRequests());
    }

    @Test
    public void caseDisplayItem_carriesTheSameFlag() {
        assertTrue("the case view reads the same derived fact as the dashboard list",
                pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST).isHasOpenRequests());
    }

    @Test
    public void displayItem_doesNotFlagACaseWhoseOnlyRequestIsClosed() {
        PathologyDisplayItem displayItem = pathologyDisplayService
                .convertToDisplayItem(CASE_WITH_ONLY_A_CLOSED_REQUEST);

        assertFalse("holding a request is not the same as holding an open one", displayItem.isHasOpenRequests());
    }

    @Test
    public void displayItem_clearsTheFlagOnceTheLastOpenRequestIsClosed() {
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = 'COMPLETED' WHERE id = ?",
                THE_OPEN_REQUEST);

        PathologyDisplayItem displayItem = pathologyDisplayService.convertToDisplayItem(CASE_WITH_AN_OPEN_REQUEST);

        assertFalse("the flag follows the request rows, so closing the last open one clears it",
                displayItem.isHasOpenRequests());
    }
}
