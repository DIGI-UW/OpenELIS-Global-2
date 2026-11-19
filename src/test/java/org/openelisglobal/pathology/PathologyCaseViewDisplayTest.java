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
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.LabelVerificationView;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.SlideStampView;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.StageEventView;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
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

    private static final int TISSUE_CATEGORY = 9403;
    private static final int RETIRED_TISSUE = 9403;
    private static final String RETIRED_TISSUE_NAME = "Bone, retired";

    private static final int STAIN_CATEGORY_ID = 9404;
    private static final int STAIN = 9405;
    private static final String STAIN_NAME = "Periodic acid-Schiff";
    private static final int RETIRED_STAIN = 9406;
    private static final String RETIRED_STAIN_NAME = "Mucicarmine, retired";
    private static final int LEGACY_SLIDE_ID = 201;
    private static final int SECOND_SLIDE = 9407;

    /**
     * Blocks and slides of this class's own, cut in an order that is not the one
     * shown.
     */
    private static final int FIRST_OWN_BLOCK = 9408;
    private static final int SECOND_OWN_BLOCK = 9409;
    private static final int A2_1 = 9410;
    private static final int A1_1 = 9411;
    private static final int A1_2 = 9412;

    /** An immunohistochemistry case of this class's own. */
    private static final int REFERRAL_CASE = 9413;

    /** A marker the order holds in the immunohistochemistry section. */
    private static final int REFERRAL_SECTION = 9414;
    private static final int REFERRED_MARKER = 9415;
    private static final int REFERRED_MARKER_ANALYSIS = 9416;

    private boolean insertedTheCategory;

    private DictionaryRows.StainEntries stains;

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SystemUserService systemUserService;

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
        removeSeededStainRows();
        seedSpecimenRows();
    }

    @After
    public void cleanUp() {
        removeSeededSpecimenRows();
        removeSeededTissueRows();
        removeSeededStainRows();
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
        DictionaryRows.insertEntry(jdbcTemplate, RETIRED_TISSUE, categoryId, RETIRED_TISSUE_NAME, "PTRet", false);
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET tissue_type_id = ? WHERE id = ?", RETIRED_TISSUE,
                LEGACY_BLOCK_ID);

        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        assertEquals(
                "an entry taken out of use after a cassette recorded it is missing from the category list"
                        + " the picker reads, so the case names it itself",
                RETIRED_TISSUE_NAME, displayItem.getTissueTypeNames().get(String.valueOf(RETIRED_TISSUE)));
    }

    @Test
    public void caseDisplayItem_namesTheStainsTheSlidesCarry_aRetiredOneIncluded() {
        seedStains();
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = ? WHERE id = ?", STAIN, LEGACY_SLIDE_ID);
        jdbcTemplate.update(
                "INSERT INTO clinlims.pathology_slide (id, pathology_sample_id, last_updated, slide_number,"
                        + " active, stain_id) VALUES (?, ?, now(), 2, true, ?)",
                SECOND_SLIDE, CASE_WITH_TWO_SPECIMEN_ITEMS, RETIRED_STAIN);

        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        assertEquals("a stain a slide carries is named for the picker", STAIN_NAME,
                displayItem.getStainNames().get(String.valueOf(STAIN)));
        assertEquals(
                "and so is one taken out of use after a slide recorded it, which the category list the picker"
                        + " reads leaves out",
                RETIRED_STAIN_NAME, displayItem.getStainNames().get(String.valueOf(RETIRED_STAIN)));
        assertEquals("nothing else is named", 2, displayItem.getStainNames().size());
    }

    /**
     * The case view reads a slide's label check from a map naming who verified it
     * and when; the raw row carries only the user's id, and a slide never checked
     * is absent.
     */
    @Test
    public void caseDisplayItem_namesWhoVerifiedEachSlideLabel() {
        PathologyCaseRows.stampLabel(jdbcTemplate, LEGACY_SLIDE_ID, "1001");
        jdbcTemplate.update("INSERT INTO clinlims.pathology_slide (id, pathology_sample_id, last_updated, slide_number,"
                + " active) VALUES (?, ?, now(), 2, true)", SECOND_SLIDE, CASE_WITH_TWO_SPECIMEN_ITEMS);

        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        LabelVerificationView verification = displayItem.getLabelVerifications().get(LEGACY_SLIDE_ID);
        assertNotNull("a verified slide is in the map", verification);
        assertEquals("named by the verifier's display name, not an id", systemUserService.get("1001").getDisplayName(),
                verification.verifiedBy());
        assertNotNull("with the time as text", verification.verifiedAt());
        assertFalse("a slide never checked is absent", displayItem.getLabelVerifications().containsKey(SECOND_SLIDE));
    }

    /**
     * The case view reads who stained each slide and who checked its coverslip, and
     * when, from two maps; the raw row carries the values and the user ids only,
     * and a slide with nothing recorded is absent from each map.
     */
    @Test
    public void caseDisplayItem_namesWhoStainedAndCheckedEachSlide() {
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_status = 'STAINED', stain_status_at = now(),"
                + " stain_status_by = 1001, coverslip_qc = 'FAIL', coverslip_qc_at = now(), coverslip_qc_by = 1001"
                + " WHERE id = ?", LEGACY_SLIDE_ID);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_slide (id, pathology_sample_id, last_updated, slide_number,"
                + " active) VALUES (?, ?, now(), 2, true)", SECOND_SLIDE, CASE_WITH_TWO_SPECIMEN_ITEMS);

        PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

        String technician = systemUserService.get("1001").getDisplayName();
        SlideStampView stained = displayItem.getStainStamps().get(LEGACY_SLIDE_ID);
        assertNotNull("a stained slide is in the stain map", stained);
        assertEquals("named by who stained it, not an id", technician, stained.by());
        assertNotNull("with the time as text", stained.at());
        SlideStampView checked = displayItem.getCoverslipQcStamps().get(LEGACY_SLIDE_ID);
        assertEquals("and by who checked its coverslip in the other", technician, checked.by());
        assertFalse("a slide with nothing recorded is in neither",
                displayItem.getStainStamps().containsKey(SECOND_SLIDE)
                        || displayItem.getCoverslipQcStamps().containsKey(SECOND_SLIDE));
        assertEquals("the raw row carries the values", StainStatus.STAINED, displayItem.getSlides().stream()
                .filter(slide -> slide.getId() == LEGACY_SLIDE_ID).findFirst().orElseThrow().getStainStatus());
    }

    /**
     * The coverslipping batch is offered where a save would write it and read back
     * once written, beside the processor run and apart from it (FR-8.4).
     */
    @Test
    public void caseDisplayItem_carriesTheCoverslippingBatch() {
        try (StageSwitches switches = StageSwitches.allOn()) {
            PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.STAINING);
            PathologyCaseRows.storeStainStatus(jdbcTemplate, LEGACY_SLIDE_ID, StainStatus.STAINED);
            pathologySampleService.advance(CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.STAINING, "1001");

            PathologyCaseViewDisplayItem open = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);
            assertTrue("an open coverslipping entry with no batch takes one", open.isCoverslipRunReferenceEditable());
            assertFalse("the processor run is not offered there", open.isRunReferenceEditable());

            PathologySampleForm form = formWithRunReference(null);
            form.setCoverslipRunReference("CS-3");
            pathologySampleService.updateWithFormValues(CASE_WITH_TWO_SPECIMEN_ITEMS, form);

            PathologyCaseViewDisplayItem written = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);
            assertEquals("the batch is read from the coverslipping entry", "CS-3", written.getCoverslipRunReference());
            assertFalse("and, written once, it is no longer editable", written.isCoverslipRunReferenceEditable());
            assertNull("the processor run is a different field", written.getRunReference());
        } finally {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS);
        }
    }

    /**
     * The case's collections carry no order, so the case view could list slides
     * differently on each load and every sentence naming them would change. Blocks
     * come by the order they were cut, slides under their block in the order they
     * were cut, and a slide with no block on record last.
     */
    @Test
    public void caseDisplayItem_listsSlidesByBlockThenInTheOrderTheyWereCut() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.pathology_block (id, pathology_sample_id, last_updated,"
                            + " designation, active) VALUES (?, ?, now(), 'A1', true), (?, ?, now(), 'A2', true)",
                    FIRST_OWN_BLOCK, CASE_WITH_TWO_SPECIMEN_ITEMS, SECOND_OWN_BLOCK, CASE_WITH_TWO_SPECIMEN_ITEMS);
            // A2.1 is cut first, so the rows are stored in an order that is not the
            // one wanted.
            jdbcTemplate.update(
                    "INSERT INTO clinlims.pathology_slide (id, pathology_sample_id, block_id,"
                            + " designation, last_updated, active) VALUES (?, ?, ?, '1', now(), true),"
                            + " (?, ?, ?, '1', now(), true), (?, ?, ?, '2', now(), true)",
                    A2_1, CASE_WITH_TWO_SPECIMEN_ITEMS, SECOND_OWN_BLOCK, A1_1, CASE_WITH_TWO_SPECIMEN_ITEMS,
                    FIRST_OWN_BLOCK, A1_2, CASE_WITH_TWO_SPECIMEN_ITEMS, FIRST_OWN_BLOCK);

            PathologyCaseViewDisplayItem displayItem = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

            assertEquals("A1.1, A1.2, A2.1, then the fixture's slide with no block",
                    List.of(A1_1, A1_2, A2_1, LEGACY_SLIDE_ID),
                    displayItem.getSlides().stream().map(PathologySlide::getId).toList());
            assertEquals("blocks in the order they were cut",
                    List.of(LEGACY_BLOCK_ID, FIRST_OWN_BLOCK, SECOND_OWN_BLOCK),
                    displayItem.getBlocks().stream().map(PathologyBlock::getId).toList());
        } finally {
            jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id IN (?, ?, ?)", A2_1, A1_1, A1_2);
            jdbcTemplate.update("DELETE FROM clinlims.pathology_block WHERE id IN (?, ?)", FIRST_OWN_BLOCK,
                    SECOND_OWN_BLOCK);
        }
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

    /**
     * The case says at which stage its findings are written and whether a save
     * takes them now, one row of the reading window at a time (W1 to W7).
     */
    @Test
    public void theCaseSaysItsReadingStage_andWhetherFindingsAreOpen() {
        try (StageSwitches switches = StageSwitches.allOn()) {
            for (PathologyStatus stage : List.of(PathologyStatus.ACCESSIONED, PathologyStatus.GROSSING,
                    PathologyStatus.DECALCIFICATION, PathologyStatus.PROCESSING, PathologyStatus.EMBEDDING,
                    PathologyStatus.MICROTOMY, PathologyStatus.STAINING, PathologyStatus.COVERSLIPPING)) {
                assertReadingWindow("W1 at " + stage, stage, "UNDER_REVIEW", false);
            }
            assertReadingWindow("W2", PathologyStatus.READY_PATHOLOGIST, "UNDER_REVIEW", false);
            assertReadingWindow("W4", PathologyStatus.UNDER_REVIEW, "UNDER_REVIEW", true);
            assertReadingWindow("W6", PathologyStatus.COMPLETED, "UNDER_REVIEW", true);

            PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS, PathologyStatus.UNDER_REVIEW);
            PathologySampleForm release = formWithRunReference(null);
            release.setRelease(true);
            pathologySampleService.updateWithFormValues(CASE_WITH_TWO_SPECIMEN_ITEMS, release);
            PathologyCaseViewDisplayItem released = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);
            assertFalse("W7: a signed-out case takes no findings", released.isFindingsOpen());

            ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED,
                    "false");
            assertReadingWindow("W3", PathologyStatus.READY_PATHOLOGIST, "READY_PATHOLOGIST", true);
            assertReadingWindow("W5", PathologyStatus.UNDER_REVIEW, "READY_PATHOLOGIST", true);
        } finally {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS);
        }
    }

    /** Each coded conclusion carries its code, the entry's abbreviation. */
    @Test
    public void codedConclusionsCarryTheirCode() {
        try (PathologyFindingsDictionary dictionary = PathologyFindingsDictionary.seed(jdbcTemplate)) {
            insertCaseRow("pathology_conclusion", 9421, PathologyFindingsDictionary.CHRONIC_GASTRITIS);
            insertCaseRow("pathology_conclusion", 9422, PathologyFindingsDictionary.ADENOCARCINOMA);
            insertCaseRow("pathology_conclusion", 9423, PathologyFindingsDictionary.OLD_TERM);

            assertEquals("id, name and code, in the stored order; no abbreviation is an empty code",
                    List.of(new ConclusionEntry("9102", "Chronic gastritis", "K29.5"),
                            new ConclusionEntry("9101", "Adenocarcinoma", "C80.1"),
                            new ConclusionEntry("9103", "Old term", "")),
                    pathologyDisplayService.convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS).getConclusions());
        }
    }

    /** The case says it was referred, and to which immunohistochemistry case. */
    @Test
    public void theCaseSaysItWasReferred_andNamesTheIhcCase() {
        PathologyCaseViewDisplayItem notReferred = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);
        assertFalse("a case nobody referred says so", notReferred.isReferredToIhc());
        assertNull("and names no case", notReferred.getIhcCaseId());
        assertEquals("and lists no referred marker, as a list rather than nothing", List.of(),
                notReferred.getIhcReferredTestIds());

        try {
            jdbcTemplate.update("INSERT INTO clinlims.immunohistochemistry_sample (id, last_updated, program_id,"
                    + " sample_id, status, pathology_sample_id, reffered) VALUES (?, now(), 1, 1, 'IN_PROGRESS', ?,"
                    + " true)", REFERRAL_CASE, CASE_WITH_TWO_SPECIMEN_ITEMS);
            jdbcTemplate.update(
                    "INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'Ki-67', now())",
                    REFERRAL_SECTION);
            jdbcTemplate.update("INSERT INTO clinlims.test_section (id, name, description, is_external, lastupdated,"
                    + " sort_order, domain, name_localization_id) VALUES (?, 'Immunohistochemistry',"
                    + " 'Referral section', 'N', now(), 1, 'CLINICAL', ?)", REFERRAL_SECTION, REFERRAL_SECTION);
            jdbcTemplate.update("INSERT INTO clinlims.test (id, description, name, local_code, lastupdated,"
                    + " test_section_id, is_active, orderable, sort_order, guid, name_localization_id) VALUES (?,"
                    + " 'Ki-67 marker', 'Ki-67', 'Ki67', now(), ?, 'Y', true, 1, gen_random_uuid(), ?)",
                    REFERRED_MARKER, REFERRAL_SECTION, REFERRAL_SECTION);
            jdbcTemplate.update("INSERT INTO clinlims.analysis (id, sampitem_id, test_sect_id, test_id, revision,"
                    + " analysis_type, lastupdated, fhir_uuid, referred_out, corrected, result_calculated,"
                    + " reflex_trigger) VALUES (?, ?, ?, ?, 0, 'MANUAL', now(), gen_random_uuid(), false, false,"
                    + " false, false)", REFERRED_MARKER_ANALYSIS, FIRST_SPECIMEN_ITEM, REFERRAL_SECTION,
                    REFERRED_MARKER);

            PathologyCaseViewDisplayItem referred = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);
            assertTrue("a referred case says so", referred.isReferredToIhc());
            assertEquals("and names the case", Integer.valueOf(REFERRAL_CASE), referred.getIhcCaseId());
            assertEquals("and lists the marker the order holds in the immunohistochemistry section",
                    List.of(String.valueOf(REFERRED_MARKER)), referred.getIhcReferredTestIds());
        } finally {
            jdbcTemplate.update("DELETE FROM clinlims.analysis WHERE id = ?", REFERRED_MARKER_ANALYSIS);
            jdbcTemplate.update("DELETE FROM clinlims.test WHERE id = ?", REFERRED_MARKER);
            jdbcTemplate.update("DELETE FROM clinlims.test_section WHERE id = ?", REFERRAL_SECTION);
            jdbcTemplate.update("DELETE FROM clinlims.localization WHERE id = ?", REFERRAL_SECTION);
            jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE id = ?", REFERRAL_CASE);
        }
    }

    /**
     * A conclusion or technique stored before codes were checked may name no entry;
     * the case is still read, without it.
     */
    @Test
    public void aStoredCodeThatNamesNoEntry_isLeftOut_notThrownOn() {
        try (PathologyFindingsDictionary dictionary = PathologyFindingsDictionary.seed(jdbcTemplate)) {
            insertCaseRow("pathology_conclusion", 9421, PathologyFindingsDictionary.ADENOCARCINOMA);
            insertCaseRow("pathology_conclusion", 9422, PathologyFindingsDictionary.NO_ENTRY);
            insertCaseRow("pathology_technique", 9424, PathologyFindingsDictionary.NO_ENTRY);
            insertCaseRow("pathology_technique", 9425, PathologyFindingsDictionary.FROZEN_SECTION);

            PathologyCaseViewDisplayItem read = pathologyDisplayService
                    .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);

            assertEquals("the conclusion that names no entry is left out", List.of("9101"),
                    read.getConclusions().stream().map(ConclusionEntry::id).collect(Collectors.toList()));
            assertEquals("and so is the technique", List.of("9201"),
                    read.getTechniques().stream().map(IdValuePair::getId).collect(Collectors.toList()));
        }
    }

    private void assertReadingWindow(String row, PathologyStatus stage, String readingStage, boolean open) {
        PathologyCaseRows.storeAt(jdbcTemplate, CASE_WITH_TWO_SPECIMEN_ITEMS, stage);
        PathologyCaseViewDisplayItem read = pathologyDisplayService
                .convertToCaseDisplayItem(CASE_WITH_TWO_SPECIMEN_ITEMS);
        assertEquals(row + ": the reading stage", readingStage, read.getReadingStage());
        assertEquals(row + ": whether the findings are open", open, read.isFindingsOpen());
    }

    /**
     * A coded row of the case, as a save writes it; the fixture reload clears it.
     */
    private void insertCaseRow(String table, int id, String code) {
        jdbcTemplate.update("INSERT INTO clinlims." + table + " (id, pathology_sample_id, type, value, last_updated)"
                + " VALUES (?, ?, 'DICTIONARY', ?, now())", id, CASE_WITH_TWO_SPECIMEN_ITEMS, code);
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
        insertedTheCategory = DictionaryRows.insertCategory(jdbcTemplate, TISSUE_CATEGORY,
                PathologySampleService.TISSUE_TYPE_CATEGORY, "Pathology - Tissue Types", "PathTissue");
        return DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.TISSUE_TYPE_CATEGORY);
    }

    private void removeSeededTissueRows() {
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET tissue_type_id = NULL WHERE tissue_type_id = ?",
                RETIRED_TISSUE);
        DictionaryRows.removeEntries(jdbcTemplate, RETIRED_TISSUE);
        if (insertedTheCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, TISSUE_CATEGORY);
            insertedTheCategory = false;
        }
    }

    private void seedStains() {
        stains = DictionaryRows.stainEntries(jdbcTemplate, STAIN_CATEGORY_ID).with(STAIN, STAIN_NAME, true)
                .with(RETIRED_STAIN, RETIRED_STAIN_NAME, false);
    }

    private void removeSeededStainRows() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id = ?", SECOND_SLIDE);
        PathologyCaseRows.clearLabelStamp(jdbcTemplate, LEGACY_SLIDE_ID);
        if (stains != null) {
            stains.close();
            stains = null;
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
