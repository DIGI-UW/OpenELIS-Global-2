package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The case view opens on a Case Information section the bench reads before it
 * touches anything: the day the specimen arrived, what was received, and who
 * asked for the examination (FR-1).
 *
 * <p>
 * The specimen rows are seeded here rather than in the dataset because naming
 * type_of_sample in a dataset truncates it, and through its localization also
 * test, test_section and panel, for every later test class in the same Surefire
 * fork.
 */
public class PathologyCaseViewDisplayTest extends BaseWebContextSensitiveTest {

    private static final int CASE_WITH_TWO_SPECIMEN_ITEMS = 1;
    private static final int CASE_WITHOUT_SPECIMEN_ITEMS = 2;

    /** Ids of this class's own rows, outside the ranges the fixture uses. */
    private static final int SPECIMEN_TYPE = 9401;
    private static final int SPECIMEN_TYPE_NAME = 9401;
    private static final int FIRST_SPECIMEN_ITEM = 9401;
    private static final int SECOND_SPECIMEN_ITEM = 9402;

    private static final String TISSUE = "Tissue, biopsy";

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    private TimeZone runnersTimeZone;

    @Before
    public void init() throws Exception {
        // Set before the fixture is written, so the dataset and every read of it
        // share one zone. The zone is east of Greenwich on purpose: that is where a
        // UTC formatter reads local midnight as the day before, and a UTC runner
        // would otherwise serialize the same string either way.
        runnersTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Nairobi"));
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        removeSeededSpecimenRows();
        seedSpecimenRows();
    }

    @After
    public void cleanUp() {
        removeSeededSpecimenRows();
        TimeZone.setDefault(runnersTimeZone);
    }

    @Test
    public void caseDisplayItem_carriesTheSpecimenArrivalDate() {
        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        assertEquals("the case view shows the day the specimen reached the laboratory", LocalDate.of(2024, 6, 3),
                displayItem.getReceivedDate());
        assertEquals("the day the examination was requested is a different field and a different day",
                LocalDate.of(2024, 6, 1), displayItem.getRequestDate());
    }

    /**
     * The fixture stamps both days at local midnight, which a UTC formatter reads
     * as the day before anywhere east of Greenwich.
     */
    @Test
    public void caseDisplayItem_serialisesTheArrivalDateAsTheServersCalendarDay() throws Exception {
        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(displayItem);

        assertTrue("the arrival day the bench reads must be the server's own, not UTC's: " + json,
                json.contains("\"receivedDate\":\"2024-06-03\""));
        assertTrue("and so must the day the examination was requested: " + json,
                json.contains("\"requestDate\":\"2024-06-01\""));
    }

    @Test
    public void caseDisplayItem_listsOneSpecimenTypePerSampleItem() {
        List<String> specimenTypes = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS)
                .getSpecimenTypes();

        assertEquals("two items of one type are two specimens received, not one", Arrays.asList(TISSUE, TISSUE),
                specimenTypes);
    }

    @Test
    public void caseDisplayItem_withNoSampleItems_hasAnEmptySpecimenTypeList() {
        List<String> specimenTypes = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITHOUT_SPECIMEN_ITEMS)
                .getSpecimenTypes();

        assertNotNull("a case with nothing received still answers with a list the screen can render", specimenTypes);
        assertTrue("and that list is empty rather than carrying a specimen that was never received",
                specimenTypes.isEmpty());
    }

    @Test
    public void caseDisplayItem_withNoRequester_leavesTheRequesterUnset() {
        // Neither fixture case carries a sample_requester row, so either one shows
        // what a case ordered without a named requester renders as.
        String requester = pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS)
                .getRequester();

        assertNull("a case ordered without a requester has no requester to show, not the word null: " + requester,
                requester);
    }

    private void seedSpecimenRows() {
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, now())",
                SPECIMEN_TYPE_NAME, "PathologyCaseViewSpecimenType");
        jdbcTemplate.update(
                "INSERT INTO clinlims.localization_value (id, localization_id, locale, value,"
                        + " last_updated) VALUES (?, ?, 'en', ?, now())",
                SPECIMEN_TYPE_NAME, SPECIMEN_TYPE_NAME, TISSUE);
        jdbcTemplate.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, lastupdated,"
                        + " local_abbrev, is_active, sort_order, name_localization_id)"
                        + " VALUES (?, ?, 'H', now(), 'PathTisBx', true, 1, ?)",
                SPECIMEN_TYPE, TISSUE, SPECIMEN_TYPE_NAME);
        // status_id is a plain column on sample_item with no foreign key, and nothing
        // the case view reads looks at it.
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample_item (id, sort_order, samp_id, typeosamp_id, status_id,"
                        + " lastupdated) VALUES (?, 1, ?, ?, 1, now())",
                FIRST_SPECIMEN_ITEM, CASE_WITH_TWO_SPECIMEN_ITEMS, SPECIMEN_TYPE);
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample_item (id, sort_order, samp_id, typeosamp_id, status_id,"
                        + " lastupdated) VALUES (?, 2, ?, ?, 1, now())",
                SECOND_SPECIMEN_ITEM, CASE_WITH_TWO_SPECIMEN_ITEMS, SPECIMEN_TYPE);
    }

    private void removeSeededSpecimenRows() {
        jdbcTemplate.update("DELETE FROM clinlims.sample_item WHERE id IN (?, ?)", FIRST_SPECIMEN_ITEM,
                SECOND_SPECIMEN_ITEM);
        jdbcTemplate.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SPECIMEN_TYPE);
        jdbcTemplate.update("DELETE FROM clinlims.localization_value WHERE id = ?", SPECIMEN_TYPE_NAME);
        jdbcTemplate.update("DELETE FROM clinlims.localization WHERE id = ?", SPECIMEN_TYPE_NAME);
    }
}
