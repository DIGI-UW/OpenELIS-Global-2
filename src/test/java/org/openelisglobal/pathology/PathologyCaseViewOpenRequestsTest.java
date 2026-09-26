package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.RequestDisplayBean;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The case view has to show that a pathologist request is outstanding while
 * still showing the case at the stage its tissue is actually at. The retired
 * ADDITIONAL_REQUEST status could only do the first by giving up the second, so
 * the case view reads the fact from the case's own pathology_request rows
 * (FR-2.1, AC-6).
 *
 * <p>
 * A boolean saying only whether any row was open came off the dashboard list
 * item first, because drawing nothing from it still cost one lazy SELECT over
 * pathology_request for every row of an unpaginated list. It then came off the
 * case view's item too: the screen names the requests it is waiting on and
 * counts them, and only the rows carry the names, so a second answer to the
 * same question could disagree with them but never add anything.
 *
 * <p>
 * These tests read the requests through the service that builds the display
 * items, against the fixture's two cases: case 1 holds an open request and case
 * 2 holds only a closed one.
 */
public class PathologyCaseViewOpenRequestsTest extends BaseWebContextSensitiveTest {

    private static final int CASE_WITH_AN_OPEN_REQUEST = 1;
    private static final int CASE_WITH_ONLY_A_CLOSED_REQUEST = 2;
    private static final int THE_OPEN_REQUEST = 301;
    private static final int THE_CLOSED_REQUEST = 302;

    /** This class's own dictionary row, outside the ranges the fixture uses. */
    private static final int REQUESTED_EXAMINATION = 9501;

    private static final String REQUESTED_EXAMINATION_NAME = "Histology review";

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        removeSeededDictionaryRow();
        seedDictionaryBackedRequests();
    }

    @After
    public void cleanUp() {
        removeSeededDictionaryRow();
    }

    @Test
    public void caseDisplayItem_carriesTheStatusOfEachRequestItHolds() {
        List<RequestDisplayBean> open = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST)
                .getRequests();
        List<RequestDisplayBean> closed = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_ONLY_A_CLOSED_REQUEST).getRequests();

        assertEquals("the case holds the one request the fixture gave it", 1, open.size());
        assertEquals("the request the case view names is the one that was asked for", REQUESTED_EXAMINATION_NAME,
                open.get(0).getValue());
        assertEquals("a request still waiting for an answer reaches the screen as OPENED", RequestStatus.OPENED,
                open.get(0).getStatus());
        assertEquals("the other case holds the one request the fixture gave it", 1, closed.size());
        assertEquals("holding a request is not the same as holding an open one, and only the row says which",
                RequestStatus.COMPLETED, closed.get(0).getStatus());
    }

    @Test
    public void listItem_doesNotCarryTheFlagOnTheWire() throws Exception {
        // A mapper carrying the same date module the application registers; the
        // shape asserted below is what the dashboard is sent.
        JsonNode listItem = mapper()
                .valueToTree(pathologyDisplayService.convertToDisplayItem(CASE_WITH_AN_OPEN_REQUEST));

        assertFalse("the dashboard list draws nothing from the flag, so it is not on the list item's"
                + " wire contract and the query behind it is not run per row", listItem.has("hasOpenRequests"));
    }

    @Test
    public void caseDisplayItem_doesNotCarryTheFlagOnTheWireEither() throws Exception {
        JsonNode caseItem = mapper()
                .valueToTree(pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST));

        assertFalse("a second answer to a question the rows already answer could only disagree with them",
                caseItem.has("hasOpenRequests"));
        assertTrue("so the wire contract is the request rows themselves", caseItem.get("requests").isArray());
        assertEquals("each of which states its own status", RequestStatus.OPENED.name(),
                caseItem.get("requests").get(0).get("status").asText());
    }

    // helpers

    /**
     * The case view names a dictionary-backed request, so the fixture's two text
     * requests are repointed at one dictionary row this class owns.
     */
    private void seedDictionaryBackedRequests() {
        jdbcTemplate.update("INSERT INTO clinlims.dictionary (id, is_active, dict_entry, lastupdated, sort_order)"
                + " VALUES (?, 'Y', ?, now(), 1)", REQUESTED_EXAMINATION, REQUESTED_EXAMINATION_NAME);
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET type = 'DICTIONARY', value = ? WHERE id IN (?, ?)",
                String.valueOf(REQUESTED_EXAMINATION), THE_OPEN_REQUEST, THE_CLOSED_REQUEST);
    }

    private void removeSeededDictionaryRow() {
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id = ?", REQUESTED_EXAMINATION);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
