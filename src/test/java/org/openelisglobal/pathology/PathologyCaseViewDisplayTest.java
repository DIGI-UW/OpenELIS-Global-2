package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.StageEventView;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
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

    /** The block the fixture seeds on the first case. */
    private static final int LEGACY_BLOCK_ID = 101;

    private static final String TISSUE_TYPE_CATEGORY = "pathology_tissue_types";
    private static final int TISSUE_CATEGORY = 9403;
    private static final int RETIRED_TISSUE = 9403;
    private static final String RETIRED_TISSUE_NAME = "Bone, retired";

    private boolean insertedTheCategory;

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private IStatusService statusService;

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
        removeSeededTissueRows();
        seedSpecimenRows();
    }

    @After
    public void cleanUp() {
        removeSeededSpecimenRows();
        removeSeededTissueRows();
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

    @Test
    public void caseDisplayItem_namesARetiredTissueTypeACassetteHolds() {
        int categoryId = ensureTissueCategory();
        jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, 'N', ?, 'PTRet', ?, 1, now())",
                RETIRED_TISSUE, RETIRED_TISSUE_NAME, categoryId);
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET tissue_type_id = ? WHERE id = ?", RETIRED_TISSUE,
                LEGACY_BLOCK_ID);

        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        assertEquals(
                "an entry taken out of use after a cassette recorded it is missing from the category list"
                        + " the picker reads, so the case names it itself",
                RETIRED_TISSUE_NAME, displayItem.getTissueTypeNames().get(String.valueOf(RETIRED_TISSUE)));
    }

    /**
     * The case view lists every stage the case entered, with the operator as a name
     * and the time as text, and keeps the processor run readable once the case has
     * left Processing (FR-5.1, AC-8).
     */
    @Test
    public void caseDisplayItem_carriesTheStageEntriesAndTheProcessorRun() {
        try (StageSwitches switches = StageSwitches.allOn()) {
            pathologySampleService.advance(CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.GROSSING, "1001");
            pathologySampleService.updateWithFormValues(CASE_WITH_TWO_SPECIMEN_ITEMS, formWithRunReference("RUN-7"));

            PathologyCaseViewDisplayItem atProcessing = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

            assertEquals("one move, one entry", 1, atProcessing.getStageEvents().size());
            StageEventView entry = atProcessing.getStageEvents().get(0);
            assertEquals("onto processing", "PROCESSING", entry.stage());
            assertEquals("going forward", "FORWARD", entry.direction());
            assertNotNull("timed by the server, as text", entry.enteredAt());
            assertTrue("by the user who moved it, as a name: " + entry.enteredBy(), entry.enteredBy().contains("Doe"));
            assertNull("still open", entry.completedAt());
            assertEquals("the run is read from the processing entry", "RUN-7", atProcessing.getRunReference());
            assertFalse("and, written once, it is no longer editable", atProcessing.isRunReferenceEditable());

            pathologySampleService.advance(CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.PROCESSING, "1001");

            PathologyCaseViewDisplayItem movedOn = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

            assertEquals("two moves, two entries, oldest first", Arrays.asList("PROCESSING", "EMBEDDING"),
                    movedOn.getStageEvents().stream().map(StageEventView::stage).toList());
            assertNotNull("the processing entry is completed", movedOn.getStageEvents().get(0).completedAt());
            assertEquals("and the run still reads from it once the case has moved on", "RUN-7",
                    movedOn.getRunReference());
        } finally {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS);
        }
    }

    /**
     * At Processing on an entry that holds no run, the case view offers the run
     * field; a case that reached Processing before entries were recorded has no
     * entry to hold one, so it does not.
     */
    @Test
    public void caseDisplayItem_offersTheRunOnlyWhereASaveWouldTakeIt() {
        try (StageSwitches switches = StageSwitches.allOn()) {
            pathologySampleService.advance(CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.GROSSING, "1001");

            assertTrue("an open processing entry with no run takes one", pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).isRunReferenceEditable());

            PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITHOUT_SPECIMEN_ITEMS, PathologyStatus.PROCESSING);

            assertFalse("a case with no entry at processing does not", pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITHOUT_SPECIMEN_ITEMS).isRunReferenceEditable());
        } finally {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS);
        }
    }

    /**
     * The case view reads where the case would be sent on or back to from the same
     * rule the moves use, so the label on the button is the move the server makes.
     */
    @Test
    public void caseDisplayItem_carriesTheNextAndPreviousStage() {
        try (StageSwitches switches = StageSwitches.allOn()) {
            PathologyCaseViewDisplayItem atGrossing = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

            assertEquals("an unflagged specimen at grossing goes on to processing", "PROCESSING",
                    atGrossing.getNextStage());
            assertEquals("and back to accessioning", "ACCESSIONED", atGrossing.getPreviousStage());

            ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_PROCESSING_ENABLED,
                    "false");

            assertEquals("a stage the deployment switched off is skipped", "EMBEDDING",
                    pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).getNextStage());

            PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.COMPLETED);
            PathologyCaseViewDisplayItem completed = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

            assertNull("a signed-out case goes nowhere on", completed.getNextStage());
            assertNull("nor back", completed.getPreviousStage());
        }
    }

    /**
     * The case view enables Release by the rule the release is refused on: in the
     * pathologist's queue, or at Completed with no release on record. A new result
     * that sets the order back to Started does not undo the release.
     */
    @Test
    public void caseDisplayItem_saysWhetherAReleaseWouldBeTaken() {
        PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS);
        try {
            assertFalse("a case at a bench stage is not released",
                    pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).isReleasable());

            PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.READY_PATHOLOGIST);
            assertTrue("a case in the pathologist's queue is",
                    pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).isReleasable());

            PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.COMPLETED);
            assertTrue("so is a case set to Completed without a release",
                    pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).isReleasable());

            PathologySampleForm release = formWithRunReference(null);
            release.setRelease(true);
            pathologySampleService.updateWithFormValues(CASE_WITH_TWO_SPECIMEN_ITEMS, release);
            jdbcTemplate.update(
                    "UPDATE clinlims.sample SET status_id = ?"
                            + " WHERE id = (SELECT sample_id FROM clinlims.pathology_sample WHERE id = ?)",
                    Integer.parseInt(statusService.getStatusID(OrderStatus.Started)), CASE_WITH_TWO_SPECIMEN_ITEMS);
            assertFalse("but not once it is released, though a later result started its order again",
                    pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).isReleasable());
        } finally {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS);
        }
    }

    /** A case on file before stage entries were recorded has none, and no run. */
    @Test
    public void caseDisplayItem_ofACaseNeverMoved_hasNoEntriesAndNoRun() {
        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITHOUT_SPECIMEN_ITEMS);

        assertTrue("no entries are invented", displayItem.getStageEvents().isEmpty());
        assertNull("nor a run", displayItem.getRunReference());
    }

    /** What the screen posts at Processing: the case as it stands, plus the run. */
    private PathologySampleForm formWithRunReference(String runReference) {
        PathologySampleForm form = new PathologySampleForm();
        form.setSystemUserId("1001");
        form.setGrossExam(pathologySampleService.get(CASE_WITH_TWO_SPECIMEN_ITEMS).getGrossExam());
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        form.setRunReference(runReference);
        return form;
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

    /**
     * Liquibase seeds the category, but an earlier dataset naming
     * dictionary_category may have truncated it, so it is added only when absent.
     */
    private int ensureTissueCategory() {
        insertedTheCategory = jdbcTemplate.update("INSERT INTO clinlims.dictionary_category (id, description,"
                + " local_abbrev, name, lastupdated) SELECT ?, 'Pathology - Tissue Types', 'PathTissue', ?, now()"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.dictionary_category WHERE name = ?)", TISSUE_CATEGORY,
                TISSUE_TYPE_CATEGORY, TISSUE_TYPE_CATEGORY) == 1;
        return jdbcTemplate.queryForObject("SELECT id FROM clinlims.dictionary_category WHERE name = ?", Integer.class,
                TISSUE_TYPE_CATEGORY);
    }

    private void removeSeededTissueRows() {
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET tissue_type_id = NULL WHERE tissue_type_id = ?",
                RETIRED_TISSUE);
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id = ?", RETIRED_TISSUE);
        if (insertedTheCategory) {
            jdbcTemplate.update("DELETE FROM clinlims.dictionary_category WHERE id = ?", TISSUE_CATEGORY);
            insertedTheCategory = false;
        }
    }

    private void removeSeededSpecimenRows() {
        jdbcTemplate.update("DELETE FROM clinlims.sample_item WHERE id IN (?, ?)", FIRST_SPECIMEN_ITEM,
                SECOND_SPECIMEN_ITEM);
        jdbcTemplate.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SPECIMEN_TYPE);
        jdbcTemplate.update("DELETE FROM clinlims.localization_value WHERE id = ?", SPECIMEN_TYPE_NAME);
        jdbcTemplate.update("DELETE FROM clinlims.localization WHERE id = ?", SPECIMEN_TYPE_NAME);
    }
}
