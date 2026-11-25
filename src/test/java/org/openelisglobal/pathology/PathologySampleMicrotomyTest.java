package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * Microtomy cuts slides from the case's blocks, each slide carrying the level
 * it was cut at and the stain it is to take (FR-7.1, FR-7.2).
 *
 * <p>
 * The level and stain columns were on every slide row, but no save wrote them,
 * the snapshot the audit trail compares against left them out, and a slide cut
 * left no audit entry of its own. A slide could also be cut from a cassette
 * nobody had embedded. These tests pin a level that fits its column, a stain
 * checked against the stain category only when it changes, one case history row
 * per edit carrying the old value, no entry for a slide posted back unchanged,
 * one creation entry per slide even when two pages cut at once, a slide cut
 * only from an embedded block wherever the laboratory tracks Embedding, and no
 * new slide on a signed-out case.
 *
 * <p>
 * The stain entries are seeded here rather than in a dataset, because naming
 * dictionary or dictionary_category in a dataset truncates them for every later
 * test class in the same Surefire fork.
 */
public class PathologySampleMicrotomyTest extends PathologyCaseTestSupport {

    /** Ids of this class's own dictionary rows, outside the ranges fixtures use. */
    private static final int OWN_CATEGORY = 9600;
    private static final int HE = 9601;
    private static final int RETIRED_STAIN = 9602;

    /** An entry in use, under a category of its own that is not the stain one. */
    private static final int OTHER_CATEGORY = 9599;
    private static final int FIXATIVE = 9603;

    private StageSwitches switches;

    private boolean insertedTheCategory;

    /**
     * Slides are cut once the case stands at Microtomy, with every stage tracked;
     * how a case is sent there is pinned by the stage event tests.
     */
    @Before
    public void init() {
        switches = StageSwitches.allOn();
        removeStainEntries();
        seedStainEntries();
        storeAt(PathologyStatus.MICROTOMY);
    }

    @After
    public void tearDown() {
        switches.close();
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = NULL WHERE stain_id IN (?, ?, ?)", HE,
                RETIRED_STAIN, FIXATIVE);
        removeStainEntries();
        if (insertedTheCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, OWN_CATEGORY);
        }
    }

    // ---- level and stain (FR-7.2) ----

    @Test
    public void aNewSlide_takesTheLevelAndStainPosted() {
        int blockId = embeddedBlock();

        saveSlides(slide(null, blockId, "2", String.valueOf(HE)));

        Map<String, Object> slide = slidesCutFrom(blockId).get(0);
        assertEquals("the slide records the level the section was cut at", "2", slide.get("level"));
        assertEquals("and the stain the bench picked", Integer.valueOf(HE), stainOf(slide));
    }

    @Test
    public void aRetiredStain_isRefusedAndTheSlideIsNotCut() {
        int blockId = embeddedBlock();

        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveSlides(slide(null, blockId, "1", String.valueOf(RETIRED_STAIN))));

        assertTrue(
                "a stain taken out of use cannot be recorded, and the refusal names it and the category: "
                        + refused.getMessage(),
                refused.getMessage().contains(String.valueOf(RETIRED_STAIN))
                        && refused.getMessage().contains(PathologySampleService.STAIN_CATEGORY));
        assertTrue("and the slide is not cut at all", slidesCutFrom(blockId).isEmpty());
    }

    @Test
    public void aStainOfAnotherCategory_isRefused() {
        int slideId = cutSlide("1", null);

        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveSlides(slide(slideId, null, "1", String.valueOf(FIXATIVE))));

        assertTrue("an entry in use elsewhere in the dictionary is not a stain: " + refused.getMessage(),
                refused.getMessage().contains(String.valueOf(FIXATIVE)));
        assertNull("and the slide holds no stain", slideRow(slideId).get("stain_id"));
    }

    @Test
    public void aStainThatIsNotAnId_isRefusedRatherThanFailing() {
        int slideId = cutSlide("1", null);

        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveSlides(slide(slideId, null, "1", "H&E")));

        assertTrue("a value that cannot be a dictionary id is the client's mistake, answered as a refusal that"
                + " names it: " + refused.getMessage(), refused.getMessage().contains("H&E"));
        assertNull("and the slide holds no stain", slideRow(slideId).get("stain_id"));
    }

    @Test
    public void aStoredStainRetiredLater_doesNotBlockTheSave() {
        int slideId = cutSlide("1", String.valueOf(HE));
        jdbcTemplate.update("UPDATE clinlims.dictionary SET is_active = 'N' WHERE id = ?", HE);

        PathologySlideForm moved = slide(slideId, null, "1", String.valueOf(HE));
        moved.setLocation("Slide Archive");
        saveSlides(moved);

        Map<String, Object> slide = slideRow(slideId);
        assertEquals("the save goes through, because the stain the slide already carried is not a new choice",
                "Slide Archive", slide.get("location"));
        assertEquals("and the slide keeps its stain", Integer.valueOf(HE), stainOf(slide));
    }

    @Test
    public void clearingTheStainAndTheLevel_isASave() {
        int slideId = cutSlide("1", String.valueOf(HE));

        saveSlides(slide(slideId, null, "  ", "  "));

        Map<String, Object> slide = slideRow(slideId);
        assertNull("a blank level clears the one recorded", slide.get("level"));
        assertNull("and a blank stain the stain", slide.get("stain_id"));
    }

    @Test
    public void aPaddedLevel_isTrimmed() {
        int slideId = cutSlide(" 3 ", null);

        assertEquals("the level is stored as typed, without the spaces around it", "3", slideRow(slideId).get("level"));
    }

    @Test
    public void aLevelLongerThanItsColumn_isRefused() {
        int slideId = cutSlide(StringUtils.repeat("L", 16), null);
        assertEquals("a level that fills the column is stored", StringUtils.repeat("L", 16),
                slideRow(slideId).get("level"));

        LocalizedValidationException refused = assertThrows(LocalizedValidationException.class,
                () -> saveSlides(slide(slideId, null, StringUtils.repeat("L", 17), null)));

        assertEquals("one character more is refused with the message the screen translates",
                "pathology.error.levelTooLong", refused.getErrorCode());
        assertEquals("and the stored level is unchanged", StringUtils.repeat("L", 16), slideRow(slideId).get("level"));
    }

    @Test
    public void changingALevelOrAStain_isRecordedInTheCaseAuditTrail() {
        int slideId = cutSlide("1", String.valueOf(HE));
        clearCaseAudit();

        saveSlides(slide(slideId, null, "2", String.valueOf(HE)));

        List<History> afterLevel = caseAudit();
        assertEquals("changing only a slide's level changes the case, so the save leaves one audit entry", 1,
                afterLevel.size());
        assertTrue("which keeps the level the slide was cut at before: " + changesOf(afterLevel.get(0)),
                changesOf(afterLevel.get(0)).contains("Level: 1"));

        clearCaseAudit();
        saveSlides(slide(slideId, null, "2", null));

        List<History> afterStain = caseAudit();
        assertEquals("clearing its stain is audited the same way", 1, afterStain.size());
        assertTrue("naming the stain it carried, not its dictionary id: " + changesOf(afterStain.get(0)),
                changesOf(afterStain.get(0)).contains("Stain: Haematoxylin and eosin"));
    }

    // ---- one audit entry per slide cut (FR-17) ----

    @Test
    public void cuttingSlides_writesOneCreatedRowPerSlide() {
        int blockId = embeddedBlock();

        saveSlides(slide(null, blockId, "1", null), slide(null, blockId, "2", null));

        List<Map<String, Object>> slides = slidesCutFrom(blockId);
        assertEquals("two slides are cut from the block", 2, slides.size());
        for (Map<String, Object> slide : slides) {
            String designation = (String) slide.get("designation");
            List<History> audit = historyService.getHistoryByRefIdAndRefTableId(String.valueOf(slide.get("id")),
                    slideReferenceTableId);
            assertEquals("cutting slide " + designation + " leaves exactly one audit entry against its own row", 1,
                    audit.size());
            assertEquals("recorded as an insert, which is what the one-character activity code can say", "I",
                    audit.get(0).getActivity());
            assertEquals("the operator is the one the save was made by", TECHNICIAN_ID, audit.get(0).getSysUserId());

            Map<String, Object> payload = payloadOf(audit.get(0));
            assertEquals("the entry records the creation and nothing the slide later changes",
                    List.of("verb", "designation", "barcode", "blockId"), List.copyOf(payload.keySet()));
            assertEquals("the verb the activity code cannot carry", "PATHOLOGY_SLIDE_CREATED", payload.get("verb"));
            assertEquals("the designation written on the slide", designation, payload.get("designation"));
            assertEquals("the barcode printed from it", accessionNumber + ".A1." + designation, payload.get("barcode"));
            assertEquals("and the block it was cut from", Integer.valueOf(blockId), payload.get("blockId"));
        }

        save(caseViewForm());

        assertEquals("a save that cuts nothing writes no creation entry", 2, auditEntriesOnEverySlide());
    }

    /**
     * The screen posts every slide back on every save. A slide posted as it is
     * stored is no edit: the snapshot the audit trail compares against carries the
     * level and the stain, so neither the case nor the slide gains an entry.
     */
    @Test
    public void postingASlideBackUnchanged_writesNoHistoryRow() {
        int slideId = cutSlide("1", String.valueOf(HE));
        int blockId = ((Number) slideRow(slideId).get("block_id")).intValue();
        clearCaseAudit();
        clearSlideAudit();

        saveSlides(slide(slideId, blockId, "1", String.valueOf(HE)));

        assertTrue("a save that changes nothing on the case leaves no case audit entry: " + caseAudit(),
                caseAudit().isEmpty());
        assertEquals("and no slide audit entry", 0, auditEntriesOnEverySlide());
    }

    // ---- two cuts at once ----

    /**
     * Two pages cut a slide under the same block at the same moment. When the two
     * saves overlap, both designate their slide "1": one ends 200 and the other is
     * refused on the slide designation index or the case's version check, leaving
     * one slide. A save that arrives after the other has committed reads the first
     * slide and cuts "2". Both outcomes are correct; either way every surviving
     * slide has exactly one creation entry and a refused save writes nothing.
     */
    @Test
    public void concurrentCuts_leaveOneCreatedRowPerSurvivingSlideAndRefuseTheLoser() throws Exception {
        int blockId = embeddedBlock();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2));
        List<Future<?>> cuts = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                Callable<Void> cut = () -> {
                    startGate.await();
                    saveSlides(slide(null, blockId, "1", null));
                    return null;
                };
                cuts.add(pool.submit(cut));
            }
            startGate.countDown();

            int won = 0;
            for (Future<?> cut : cuts) {
                try {
                    cut.get(60, TimeUnit.SECONDS);
                    won++;
                } catch (ExecutionException e) {
                    assertTrue(
                            "a refused cut lost the case's version check or the slide designation index, not "
                                    + e.getCause(),
                            StaleSaveGuard.isOptimisticLockFailure(e.getCause()) || namesTheSlideIndex(e.getCause()));
                }
            }
            assertTrue("at least one cut is saved", won >= 1);

            List<Map<String, Object>> slides = slidesCutFrom(blockId);
            assertEquals("one slide under the block per saved cut", won, slides.size());
            assertEquals("designated in the order they were cut", won == 1 ? List.of("1") : List.of("1", "2"),
                    slides.stream().map(row -> String.valueOf(row.get("designation"))).sorted().toList());
            assertEquals("one slide audit entry per surviving slide", won, auditEntriesOnEverySlide());
            for (Map<String, Object> row : slides) {
                List<History> audit = historyService.getHistoryByRefIdAndRefTableId(String.valueOf(row.get("id")),
                        slideReferenceTableId);
                assertEquals("each slide has one entry of its own", 1, audit.size());
                assertEquals("recording its creation", PathologySampleService.PATHOLOGY_SLIDE_CREATED,
                        payloadOf(audit.get(0)).get("verb"));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- what a slide is cut from (FR-7.1) ----

    @Test
    public void aSlideFromACassette_isRefusedWhileEmbeddingIsTracked() {
        saveBlocks(postedBlock(null, "Bench A"));
        int cassetteId = idOfBlockDesignated("A1");

        LocalizedValidationException refused = assertThrows(LocalizedValidationException.class,
                () -> saveSlides(slide(null, cassetteId, "1", null)));

        assertEquals("the refusal carries the message the screen translates", "pathology.error.slideFromCassette",
                refused.getErrorCode());
        assertEquals("naming the cassette still to embed", Map.of("designation", "A1"), refused.getParams());
        assertTrue("and no slide is cut from it", slidesCutFrom(cassetteId).isEmpty());
    }

    @Test
    public void aSlideFromACassette_isTakenWithEmbeddingSwitchedOff() {
        saveBlocks(postedBlock(null, "Bench A"));
        int cassetteId = idOfBlockDesignated("A1");
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_EMBEDDING_ENABLED, "false");

        saveSlides(slide(null, cassetteId, "1", null));

        assertEquals("a laboratory that does not track embedding never has a block, so it cuts from the cassette", 1,
                slidesCutFrom(cassetteId).size());
    }

    @Test
    public void theLegacyBlockWithNoState_countsAsACassette() {
        assertNull("the fixture's legacy block carries no state", blockRow(LEGACY_BLOCK_ID).get("cassette_state"));

        LocalizedValidationException refused = assertThrows(LocalizedValidationException.class,
                () -> saveSlides(slide(null, LEGACY_BLOCK_ID, "1", null)));

        assertEquals("a row nobody embedded is not a block", "pathology.error.slideFromCassette",
                refused.getErrorCode());
        assertEquals("and is named by its legacy number", Map.of("designation", "11"), refused.getParams());
    }

    // ---- a signed-out case ----

    @Test
    public void aNewSlideOnACompletedCase_isRefusedWhileAnEditStillSaves() {
        int slideId = cutSlide("1", null);
        int blockId = ((Number) slideRow(slideId).get("block_id")).intValue();
        storeAt(PathologyStatus.COMPLETED);

        LocalizedValidationException refused = assertThrows(LocalizedValidationException.class,
                () -> saveSlides(slide(null, blockId, "2", null)));

        assertEquals("a signed-out case takes no new slide", "pathology.error.caseCompleted", refused.getErrorCode());
        assertEquals("and none is cut", 1, slidesCutFrom(blockId).size());

        PathologySlideForm moved = slide(slideId, null, "1", null);
        moved.setLocation("Slide Archive");
        saveSlides(moved);

        assertEquals("moving a slide already cut is a save like any other field of the case", "Slide Archive",
                slideRow(slideId).get("location"));
    }

    // helpers

    /** Whether a failed save was refused by the slide designation index. */
    private static boolean namesTheSlideIndex(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("pathology_slide_designation_uk")) {
                return true;
            }
        }
        return false;
    }

    /** A cassette cut and embedded, so slides can be cut from it. */
    private int embeddedBlock() {
        saveBlocks(postedBlock(null, "Bench A"));
        int blockId = idOfBlockDesignated("A1");
        storeAsBlock(blockId);
        return blockId;
    }

    /** One slide cut from a fresh embedded block, returning its id. */
    private int cutSlide(String level, String stainId) {
        int blockId = embeddedBlock();
        saveSlides(slide(null, blockId, level, stainId));
        List<Map<String, Object>> slides = slidesCutFrom(blockId);
        assertEquals("setup: one slide is cut", 1, slides.size());
        return ((Number) slides.get(0).get("id")).intValue();
    }

    private PathologySlideForm slide(Integer id, Integer blockId, String level, String stainId) {
        PathologySlideForm slide = postedSlide(blockId);
        slide.setId(id);
        slide.setLevel(level);
        slide.setStainId(stainId);
        return slide;
    }

    /** stain_id is numeric(10), which JDBC answers as a BigDecimal. */
    private Integer stainOf(Map<String, Object> slide) {
        Object stainId = slide.get("stain_id");
        return stainId == null ? null : ((Number) stainId).intValue();
    }

    /** The audit entries on every slide, of this case or any other. */
    private int auditEntriesOnEverySlide() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.history WHERE reference_table = ?",
                Integer.class, Integer.parseInt(slideReferenceTableId));
    }

    /**
     * The configuration loader ships the category, and tests do not run the loader,
     * so it is added only when absent and removed afterwards only if this class
     * added it.
     */
    private void seedStainEntries() {
        insertedTheCategory = DictionaryRows.insertCategory(jdbcTemplate, OWN_CATEGORY,
                PathologySampleService.STAIN_CATEGORY, PathologySampleService.STAIN_CATEGORY, "PathStain");
        int categoryId = DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.STAIN_CATEGORY);
        DictionaryRows.insertEntry(jdbcTemplate, HE, categoryId, "Haematoxylin and eosin", "PS" + HE, true);
        DictionaryRows.insertEntry(jdbcTemplate, RETIRED_STAIN, categoryId, "Retired stain", "PS" + RETIRED_STAIN,
                false);
        DictionaryRows.insertCategory(jdbcTemplate, OTHER_CATEGORY, "pathology_test_fixatives",
                "Pathology test fixatives", "PathFix");
        DictionaryRows.insertEntry(jdbcTemplate, FIXATIVE, OTHER_CATEGORY, "Formalin", "PS" + FIXATIVE, true);
    }

    private void removeStainEntries() {
        DictionaryRows.removeEntries(jdbcTemplate, HE, RETIRED_STAIN, FIXATIVE);
        DictionaryRows.removeCategory(jdbcTemplate, OTHER_CATEGORY);
    }
}
