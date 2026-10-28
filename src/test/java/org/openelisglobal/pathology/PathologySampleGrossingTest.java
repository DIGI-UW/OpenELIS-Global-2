package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Grossing records what the specimen looked like, whether it has to be
 * decalcified, and which tissue each cassette holds (FR-3.1 to FR-3.3).
 *
 * <p>
 * The description column held 255 characters, so a full macroscopic description
 * could not be stored, and a cassette's tissue type was dropped on every save
 * because nothing validated it. These tests pin the description's length, the
 * decalcification flag, which a move onto that stage also sets and a save past
 * it no longer takes, a tissue type checked against the laboratory's own
 * dictionary category only when it changes, and one audit entry per cassette
 * cut.
 *
 * <p>
 * The tissue entries are seeded here rather than in a dataset, because naming
 * dictionary or dictionary_category in a dataset truncates them for every later
 * test class in the same Surefire fork.
 */
public class PathologySampleGrossingTest extends PathologyCaseTestSupport {

    private static final String TISSUE_TYPE_CATEGORY = "pathology_tissue_types";

    /** Ids of this class's own dictionary rows, outside the ranges fixtures use. */
    private static final int OWN_CATEGORY = 9500;
    private static final int SKIN = 9501;
    private static final int RETIRED_TISSUE = 9502;

    /** An entry in use, under a category of its own that is not the tissue one. */
    private static final int OTHER_CATEGORY = 9499;
    private static final int FIXATIVE = 9503;

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    private boolean insertedTheCategory;

    @Before
    public void init() {
        removeTissueEntries();
        seedTissueEntries();
    }

    @After
    public void tearDown() {
        jdbcTemplate.update(
                "UPDATE clinlims.pathology_block SET tissue_type_id = NULL WHERE tissue_type_id IN (?, ?, ?)", SKIN,
                RETIRED_TISSUE, FIXATIVE);
        removeTissueEntries();
        if (insertedTheCategory) {
            jdbcTemplate.update("DELETE FROM clinlims.dictionary_category WHERE id = ?", OWN_CATEGORY);
        }
    }

    // ---- decalcification flag and description (FR-3.1, FR-3.2) ----

    @Test
    public void savingTheDecalcificationFlag_storesItAndTheCaseViewCarriesIt() {
        save(flagged(true));

        assertTrue("a ticked flag is stored on the case", storedFlag());
        assertTrue("and the case view reads it back, which is what opens the decalcification stage",
                pathologyDisplayService.convertToCaseDisplayItem(CASE_ID).isDecalcificationRequired());

        save(flagged(false));

        assertFalse("an untick is a save like any other", storedFlag());
        assertFalse("and the case view follows it",
                pathologyDisplayService.convertToCaseDisplayItem(CASE_ID).isDecalcificationRequired());
    }

    @Test
    public void aSaveThatOmitsTheFlag_leavesItAsStored() {
        save(flagged(true));

        save(caseViewForm());

        assertTrue("a client that does not post the flag has not changed it, so the stored value stands", storedFlag());
    }

    @Test
    public void aDescriptionLongerThanTheOldColumn_isStored() {
        String description = StringUtils.repeat("Received in formalin, a skin ellipse. ", 60).substring(0, 2000);
        PathologySampleForm form = caseViewForm();
        form.setGrossExam(description);

        save(form);

        assertEquals(
                "a macroscopic description runs to paragraphs and is stored whole, where the old column"
                        + " held 255 characters",
                description, jdbcTemplate.queryForObject(
                        "SELECT gross_exam FROM clinlims.pathology_sample WHERE id = ?", String.class, CASE_ID));
    }

    @Test
    public void togglingTheDecalcificationFlag_isRecordedInTheCaseAuditTrail() {
        save(flagged(true));
        clearCaseAudit();

        save(flagged(false));

        List<History> audit = caseAudit();
        assertEquals("unticking the flag changes the case, so the save leaves one audit entry", 1, audit.size());
        assertTrue("which keeps the value the flag held before: " + changesOf(audit.get(0)),
                changesOf(audit.get(0)).contains("<decalcificationRequired>true</decalcificationRequired>"));
    }

    @Test
    public void movingACaseIntoDecalcification_flagsTheSpecimen() {
        PathologySampleForm form = caseViewForm();
        form.setStatus(PathologyStatus.DECALCIFICATION);
        form.setDecalcificationRequired(false);

        save(form);

        assertTrue("a case sent to decalcification needs it, even when the box was left unticked, so it never"
                + " reads as not required once it moves on", storedFlag());
    }

    @Test
    public void aSaveAtDecalcification_respectsAnUntickedFlag() {
        PathologySampleForm movedIn = caseViewForm();
        movedIn.setStatus(PathologyStatus.DECALCIFICATION);
        save(movedIn);
        assertTrue("the move onto the stage flagged the specimen", storedFlag());

        save(flagged(false));

        assertFalse("a case already standing at the stage keeps whatever the bench posts, so a flag set in"
                + " error can be cleared there", storedFlag());
    }

    @Test
    public void aFlagPostedAfterTheStage_isIgnored() {
        storeAtProcessing(true);

        save(flagged(false));

        assertTrue("a case past decalcification went through it, so an untick posted later does not make the"
                + " specimen read as never needing it", storedFlag());

        storeAtProcessing(false);

        save(flagged(true));

        assertFalse("and a tick posted after the stage does not claim a decalcification the case never had",
                storedFlag());
    }

    // ---- tissue type a cassette holds (FR-3.3) ----

    @Test
    public void aNewCassette_takesTheTissueTypePosted() {
        saveBlocks(postedBlock(null, "Bench A", String.valueOf(SKIN)));

        Map<String, Object> cassette = blockDesignated("A1");
        assertNotNull("the cassette is cut and named", cassette);
        assertEquals("and holds the tissue type the bench picked", Integer.valueOf(SKIN), tissueTypeOf(cassette));
    }

    @Test
    public void aChangedTissueType_isValidatedAgainstTheCategory() {
        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench Z", "9999")));

        assertTrue("the refusal names the id and the category it is not an entry of: " + refused.getMessage(),
                refused.getMessage().contains("9999") && refused.getMessage().contains(TISSUE_TYPE_CATEGORY));
        Map<String, Object> block = blockRow(LEGACY_BLOCK_ID);
        assertNull("the block holds no tissue type it was never given", block.get("tissue_type_id"));
        assertEquals("and a refused save moves nothing else on the row either", "Lab 1", block.get("location"));
    }

    @Test
    public void aRetiredTissueType_isRefused() {
        int blocksBefore = blocksOnCase().size();

        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveBlocks(postedBlock(null, "Bench A", String.valueOf(RETIRED_TISSUE))));

        assertTrue("an entry taken out of use cannot be recorded on a cassette: " + refused.getMessage(),
                refused.getMessage().contains(String.valueOf(RETIRED_TISSUE)));
        assertEquals("and the cassette is not cut at all", blocksBefore, blocksOnCase().size());
    }

    @Test
    public void anEntryOfAnotherCategory_isRefused() {
        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench Z", String.valueOf(FIXATIVE))));

        assertTrue("an entry in use elsewhere in the dictionary is not a tissue type: " + refused.getMessage(),
                refused.getMessage().contains(String.valueOf(FIXATIVE)));
        assertNull("and the block holds no tissue type", blockRow(LEGACY_BLOCK_ID).get("tissue_type_id"));
    }

    @Test
    public void aTissueTypeThatIsNotAnId_isRefusedRatherThanFailing() {
        PathologyCaseRuleException refused = assertThrows(PathologyCaseRuleException.class,
                () -> saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench Z", "Skin")));

        assertTrue("a value that cannot be a dictionary id is the client's mistake, answered as a refusal that"
                + " names it: " + refused.getMessage(), refused.getMessage().contains("Skin"));
        assertNull("and the block holds no tissue type", blockRow(LEGACY_BLOCK_ID).get("tissue_type_id"));
    }

    @Test
    public void aStoredTissueTypeRetiredLater_doesNotBlockTheSave() {
        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", String.valueOf(SKIN)));
        jdbcTemplate.update("UPDATE clinlims.dictionary SET is_active = 'N' WHERE id = ?", SKIN);

        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench B", String.valueOf(SKIN)));

        Map<String, Object> block = blockRow(LEGACY_BLOCK_ID);
        assertEquals("the save goes through, because the tissue the cassette already held is not a new choice",
                "Bench B", block.get("location"));
        assertEquals("and the cassette keeps the tissue it was cut from", Integer.valueOf(SKIN), tissueTypeOf(block));
    }

    @Test
    public void clearingATissueType_isASave() {
        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", String.valueOf(SKIN)));
        assertEquals("the block holds a tissue type before it is cleared", Integer.valueOf(SKIN),
                tissueTypeOf(blockRow(LEGACY_BLOCK_ID)));

        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", "  "));

        assertNull("a blank tissue type clears the one recorded", blockRow(LEGACY_BLOCK_ID).get("tissue_type_id"));

        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", String.valueOf(SKIN)));
        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", null));

        assertNull("and so does an absent one, since the screen posts the whole row",
                blockRow(LEGACY_BLOCK_ID).get("tissue_type_id"));
    }

    @Test
    public void changingACassettesTissueType_isRecordedInTheCaseAuditTrail() {
        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", String.valueOf(SKIN)));
        clearCaseAudit();

        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A", null));

        List<History> audit = caseAudit();
        assertEquals("changing only the tissue a cassette holds still changes the case, so the save leaves one"
                + " audit entry", 1, audit.size());
        assertTrue("which names the tissue the cassette held before, not its dictionary id: " + changesOf(audit.get(0)),
                changesOf(audit.get(0)).contains("Tissue: Skin"));
    }

    // ---- one audit entry per cassette cut (FR-17) ----

    @Test
    public void cuttingCassettes_writesOneCreatedRowPerCassette() {
        saveBlocks(postedBlock(null, "Bench A", null), postedBlock(null, "Bench B", null));

        for (String designation : Arrays.asList("A1", "A2")) {
            Map<String, Object> cassette = blockDesignated(designation);
            assertNotNull("cassette " + designation + " is cut", cassette);
            String blockId = String.valueOf(cassette.get("id"));

            List<History> audit = historyService.getHistoryByRefIdAndRefTableId(blockId, blockReferenceTableId);
            assertEquals("cutting cassette " + designation + " leaves exactly one audit entry against its own row", 1,
                    audit.size());
            assertEquals("recorded as an insert, which is what the one-character activity code can say", "I",
                    audit.get(0).getActivity());
            assertEquals("the operator is the one the save was made by", TECHNICIAN_ID, audit.get(0).getSysUserId());

            Map<String, Object> payload = payloadOf(audit.get(0));
            assertEquals("the verb the activity code cannot carry", "PATHOLOGY_BLOCK_CREATED", payload.get("verb"));
            assertEquals("the designation written on the cassette", designation, payload.get("designation"));
            assertEquals("and the barcode printed from it", accessionNumber + "." + designation,
                    payload.get("barcode"));
        }

        save(caseViewForm());

        assertEquals("a save that cuts nothing writes no creation entry", 2, blockAuditCount());
    }

    // helpers

    private PathologySampleForm flagged(boolean decalcificationRequired) {
        PathologySampleForm form = caseViewForm();
        form.setDecalcificationRequired(decalcificationRequired);
        return form;
    }

    private void storeAtProcessing(boolean decalcificationRequired) {
        jdbcTemplate.update(
                "UPDATE clinlims.pathology_sample SET status = 'PROCESSING', decalcification_required = ? WHERE id = ?",
                decalcificationRequired, CASE_ID);
    }

    private boolean storedFlag() {
        return jdbcTemplate.queryForObject(
                "SELECT decalcification_required FROM clinlims.pathology_sample WHERE id = ?", Boolean.class, CASE_ID);
    }

    /** tissue_type_id is numeric(10), which JDBC answers as a BigDecimal. */
    private Integer tissueTypeOf(Map<String, Object> block) {
        Object tissueTypeId = block.get("tissue_type_id");
        return tissueTypeId == null ? null : ((Number) tissueTypeId).intValue();
    }

    private int blockAuditCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.history WHERE reference_table = ?",
                Integer.class, Integer.parseInt(blockReferenceTableId));
    }

    /**
     * Liquibase seeds the category, but an earlier dataset naming
     * dictionary_category may have truncated it, so it is added only when absent
     * and removed afterwards only if this class added it.
     */
    private void seedTissueEntries() {
        insertedTheCategory = jdbcTemplate.update("INSERT INTO clinlims.dictionary_category (id, description,"
                + " local_abbrev, name, lastupdated) SELECT ?, 'Pathology - Tissue Types', 'PathTissue', ?, now()"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.dictionary_category WHERE name = ?)", OWN_CATEGORY,
                TISSUE_TYPE_CATEGORY, TISSUE_TYPE_CATEGORY) == 1;
        int categoryId = jdbcTemplate.queryForObject("SELECT id FROM clinlims.dictionary_category WHERE name = ?",
                Integer.class, TISSUE_TYPE_CATEGORY);
        insertTissueEntry(SKIN, "Skin", "Y", categoryId);
        insertTissueEntry(RETIRED_TISSUE, "Retired tissue", "N", categoryId);
        jdbcTemplate.update("INSERT INTO clinlims.dictionary_category (id, description, local_abbrev, name,"
                + " lastupdated) VALUES (?, 'Pathology test fixatives', 'PathFix', 'pathology_test_fixatives', now())",
                OTHER_CATEGORY);
        insertTissueEntry(FIXATIVE, "Formalin", "Y", OTHER_CATEGORY);
    }

    private void insertTissueEntry(int id, String name, String isActive, int categoryId) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, ?, ?, ?, ?, ?, now())",
                id, isActive, name, "PT" + id, categoryId, id);
    }

    private void removeTissueEntries() {
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id IN (?, ?, ?)", SKIN, RETIRED_TISSUE, FIXATIVE);
        jdbcTemplate.update("DELETE FROM clinlims.dictionary_category WHERE id = ?", OTHER_CATEGORY);
    }
}
