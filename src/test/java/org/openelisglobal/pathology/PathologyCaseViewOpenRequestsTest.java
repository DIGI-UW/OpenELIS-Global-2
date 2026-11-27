package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.RequestView;
import org.openelisglobal.systemuser.service.SystemUserService;
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
 * Each request reaches the screen with every name already rendered (FR-10.1):
 * its instruction, the block or slide it names and the one that fulfilled it as
 * the bench prints them, the stain by name, and who raised it and when. A
 * request on file from before has none of those but its instruction, and is
 * listed all the same.
 *
 * <p>
 * These tests read the requests through the service that builds the display
 * items, against the fixture's two cases: case 1 holds an open text request and
 * case 2 holds only a completed one.
 */
public class PathologyCaseViewOpenRequestsTest extends BaseWebContextSensitiveTest {

    private static final int CASE_WITH_AN_OPEN_REQUEST = 1;
    private static final int CASE_WITH_ONLY_A_CLOSED_REQUEST = 2;
    private static final int THE_OPEN_REQUEST = 301;
    private static final int THE_CLOSED_REQUEST = 302;

    /** This class's own rows, outside the ranges the fixture uses. */
    private static final int REQUESTED_EXAMINATION = 9501;
    private static final String REQUESTED_EXAMINATION_NAME = "Histology review, by name";
    private static final int STAIN_CATEGORY = 9502;
    private static final int STAIN = 9503;
    private static final String STAIN_NAME = "Periodic acid-Schiff";
    private static final int BLOCK_A1 = 9511;
    private static final int SLIDE_A1_1 = 9512;
    private static final int RETIRED_SLIDE_A1_2 = 9513;
    private static final int STAIN_REQUEST = 9521;
    private static final int SECOND_OPEN_REQUEST = 9522;

    /** Every field a request carries to the screen, by name. */
    private static final Set<String> REQUEST_VIEW_FIELDS = new TreeSet<>(List.of("id", "kind", "targetBlockId",
            "targetSlideId", "targetName", "stainId", "stainName", "priority", "instruction", "status", "cancelReason",
            "resultBlockId", "resultSlideId", "resultName", "raisedAt", "raisedBy"));

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    @Autowired
    private SystemUserService systemUserService;

    private DictionaryRows.StainEntries stains;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        removeOwnRows();
    }

    @After
    public void cleanUp() {
        removeOwnRows();
    }

    @Test
    public void caseDisplayItem_carriesTheStatusOfEachRequestItHolds() {
        List<RequestView> open = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST)
                .getRequests();
        List<RequestView> closed = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_ONLY_A_CLOSED_REQUEST)
                .getRequests();

        assertEquals("the case holds the one request the fixture gave it", 1, open.size());
        assertEquals("named by the row's own id, so the screen can act on it", Integer.valueOf(THE_OPEN_REQUEST),
                open.get(0).id());
        assertEquals("a text request is listed with its instruction as written", "Histology review",
                open.get(0).instruction());
        assertEquals("a request still waiting for an answer reaches the screen as OPENED", "OPENED",
                open.get(0).status());
        assertEquals("the other case holds the one request the fixture gave it", 1, closed.size());
        assertEquals(Integer.valueOf(THE_CLOSED_REQUEST), closed.get(0).id());
        assertEquals("holding a request is not the same as holding an open one, and only the row says which",
                "COMPLETED", closed.get(0).status());
    }

    /**
     * A request on file is named by the dictionary entry it was picked by, or by
     * the value itself when no entry carries it, and has nothing else to show.
     */
    @Test
    public void aRequestOnFile_isNamedByItsDictionaryEntry_orByItsValueWhenNoEntryCarriesIt() {
        jdbcTemplate.update("INSERT INTO clinlims.dictionary (id, is_active, dict_entry, lastupdated, sort_order)"
                + " VALUES (?, 'Y', ?, now(), 1)", REQUESTED_EXAMINATION, REQUESTED_EXAMINATION_NAME);
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET type = 'DICTIONARY', value = ? WHERE id = ?",
                String.valueOf(REQUESTED_EXAMINATION), THE_OPEN_REQUEST);
        jdbcTemplate.update(
                "UPDATE clinlims.pathology_request SET type = 'DICTIONARY', value = '424242'" + " WHERE id = ?",
                THE_CLOSED_REQUEST);

        RequestView picked = onlyRequestOf(CASE_WITH_AN_OPEN_REQUEST);
        RequestView unnamed = onlyRequestOf(CASE_WITH_ONLY_A_CLOSED_REQUEST);

        assertEquals("a pick is named by its entry", REQUESTED_EXAMINATION_NAME, picked.instruction());
        assertEquals("a value no entry carries stands for itself", "424242", unnamed.instruction());
        assertNull("a request on file names no kind", picked.kind());
        assertNull("no target", picked.targetName());
        assertNull("no priority", picked.priority());
        assertNull("and no author or date", picked.raisedAt());
        assertNull(picked.raisedBy());
    }

    /**
     * A raised request carries every name the table shows, rendered here: the slide
     * it names and the one that fulfilled it as the bench prints them, a retired
     * one included, the stain by name, and who raised it and when.
     */
    @Test
    public void aRaisedRequest_carriesItsTargetStainResultAndAuthorByName() {
        seedAStainRequestFulfilledByARetiredSlide();

        RequestView view = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST).getRequests()
                .stream().filter(request -> request.id() == STAIN_REQUEST).findFirst().orElseThrow();

        assertEquals("STAIN", view.kind());
        assertEquals(Integer.valueOf(SLIDE_A1_1), view.targetSlideId());
        assertNull("a stain request names a slide, not a block", view.targetBlockId());
        assertEquals("the slide it names, as its label prints it", "A1.1", view.targetName());
        assertEquals(String.valueOf(STAIN), view.stainId());
        assertEquals("the stain by name", STAIN_NAME, view.stainName());
        assertEquals("URGENT", view.priority());
        assertEquals("PAS on A1.1", view.instruction());
        assertEquals("COMPLETED", view.status());
        assertEquals(Integer.valueOf(RETIRED_SLIDE_A1_2), view.resultSlideId());
        assertEquals("the slide that fulfilled it is named although it was retired since", "A1.2", view.resultName());
        assertNotNull("when it was raised, as text", view.raisedAt());
        assertEquals("by whom, by name", systemUserService.get("1002").getDisplayName(), view.raisedBy());
    }

    /**
     * Newest first, by id, whatever order the rows come back in. The second request
     * is open like the fixture's and written after it, so the database hands them
     * back oldest first and only the sort puts them the other way round.
     */
    @Test
    public void requests_comeNewestFirst() {
        jdbcTemplate.update(
                "INSERT INTO clinlims.pathology_request (id, pathology_sample_id, last_updated, status,"
                        + " type, value) VALUES (?, ?, now(), 'OPENED', 'TEXT', 'Iron stain')",
                SECOND_OPEN_REQUEST, CASE_WITH_AN_OPEN_REQUEST);

        List<Integer> ids = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST).getRequests()
                .stream().map(RequestView::id).toList();

        assertEquals("the request raised last comes first", List.of(SECOND_OPEN_REQUEST, THE_OPEN_REQUEST), ids);
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
        assertEquals("each of which states its own status", "OPENED",
                caseItem.get("requests").get(0).get("status").asText());
    }

    /** The names the screen reads each request by, and no others. */
    @Test
    public void eachRequest_reachesTheScreenUnderTheNamesItReads() throws Exception {
        seedAStainRequestFulfilledByARetiredSlide();

        JsonNode request = mapper()
                .valueToTree(pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_AN_OPEN_REQUEST))
                .get("requests").get(0);

        Set<String> names = new TreeSet<>();
        request.fieldNames().forEachRemaining(names::add);
        assertEquals(REQUEST_VIEW_FIELDS, names);
        assertEquals("ids are numbers", STAIN_REQUEST, request.get("id").asInt());
        assertTrue("and the time is text, not an instant to format", request.get("raisedAt").isTextual());
    }

    // helpers

    private RequestView onlyRequestOf(int caseId) {
        List<RequestView> requests = pathologyDisplayService.convertToCaseDisplayItem(caseId).getRequests();
        assertEquals(1, requests.size());
        return requests.get(0);
    }

    /**
     * Block A1 on case 1 with slide A1.1 in use and A1.2 retired, and a stain
     * request on A1.1 raised by pathologist1 and fulfilled by A1.2.
     */
    private void seedAStainRequestFulfilledByARetiredSlide() {
        stains = DictionaryRows.stainEntries(jdbcTemplate, STAIN_CATEGORY).with(STAIN, STAIN_NAME, true);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_block (id, pathology_sample_id, last_updated, designation,"
                + " active) VALUES (?, ?, now(), 'A1', true)", BLOCK_A1, CASE_WITH_AN_OPEN_REQUEST);
        jdbcTemplate.update(
                "INSERT INTO clinlims.pathology_slide (id, pathology_sample_id, block_id, designation, last_updated,"
                        + " active) VALUES (?, ?, ?, '1', now(), true), (?, ?, ?, '2', now(), false)",
                SLIDE_A1_1, CASE_WITH_AN_OPEN_REQUEST, BLOCK_A1, RETIRED_SLIDE_A1_2, CASE_WITH_AN_OPEN_REQUEST,
                BLOCK_A1);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_request (id, pathology_sample_id, last_updated, status,"
                + " type, value, request_kind, target_slide_id, stain_id, priority, result_slide_id, raised_at,"
                + " raised_by) VALUES (?, ?, now(), 'COMPLETED', 'TEXT', 'PAS on A1.1', 'STAIN', ?, ?, 'URGENT', ?,"
                + " now(), 1002)", STAIN_REQUEST, CASE_WITH_AN_OPEN_REQUEST, SLIDE_A1_1, STAIN, RETIRED_SLIDE_A1_2);
    }

    private void removeOwnRows() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_request WHERE id IN (?, ?)", STAIN_REQUEST,
                SECOND_OPEN_REQUEST);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id IN (?, ?)", SLIDE_A1_1, RETIRED_SLIDE_A1_2);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_block WHERE id = ?", BLOCK_A1);
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET type = 'TEXT', value = 'Histology review'"
                + " WHERE id IN (?, ?)", THE_OPEN_REQUEST, THE_CLOSED_REQUEST);
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id = ?", REQUESTED_EXAMINATION);
        if (stains != null) {
            stains.close();
            stains = null;
        }
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
