package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.CassetteState;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A cassette, a block and a slide are objects on a bench, not counts on a case.
 *
 * <p>
 * Saving the case emptied both collections and cleared every posted id before
 * adding the rows back, so each save deleted and re-inserted every block and
 * slide under a new id. Nothing printed on a label could be trusted to still
 * resolve, and a laboratory could not show which slide came from which tissue,
 * which an accredited laboratory has to be able to produce for years after the
 * case closes.
 *
 * <p>
 * These tests pin the rules that replace that: a posted row is matched by its
 * id and keeps the identity the server gave it, a row with no id is created and
 * named by the server, a row the client did not post is left alone, and a
 * retirement keeps both the row and its designation and leaves one audit entry
 * behind.
 */
public class PathologySampleSaveIdentityTest extends BaseWebContextSensitiveTest {

    private static final int CASE_ID = 1;

    /**
     * The block the fixture seeds, which carries a legacy number and no
     * designation.
     */
    private static final int LEGACY_BLOCK_ID = 101;

    /**
     * technician1, the fixture's own user, so audit attribution names a real one.
     */
    private static final String TECHNICIAN_ID = "1001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private String accessionNumber;
    private String blockReferenceTableId;
    private String slideReferenceTableId;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        accessionNumber = jdbcTemplate.queryForObject(
                "SELECT s.accession_number FROM clinlims.sample s"
                        + " JOIN clinlims.pathology_sample ps ON ps.sample_id = s.id WHERE ps.id = ?",
                String.class, CASE_ID);
        blockReferenceTableId = referenceTableId("PATHOLOGY_BLOCK");
        slideReferenceTableId = referenceTableId("PATHOLOGY_SLIDE");
        clearPathologyAudit();
    }

    /**
     * Audit entries survive the fixture reload, so they are cleared here as well as
     * before each test; otherwise a count asserted in one test would include the
     * retirements another test wrote.
     */
    @After
    public void tearDown() {
        clearPathologyAudit();
    }

    @Test
    public void savingTwice_keepsTheSameBlockRow() {
        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench A"));
        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench B"));

        List<Map<String, Object>> blocks = blocksOnCase();

        assertEquals("posting a block the case already holds applies to that row, rather than deleting the"
                + " case's blocks and inserting them again", 1, blocks.size());
        assertEquals("the block keeps the id its label was printed from", Integer.valueOf(LEGACY_BLOCK_ID),
                blocks.get(0).get("id"));
        assertEquals("the location posted last is the one stored", "Bench B", blocks.get(0).get("location"));
    }

    @Test
    public void savingANewCassette_namesItOnTheServer() {
        PathologyBlock posted = postedBlock(null, "Bench A");
        posted.setPartDesignation("Z");
        posted.setTissueTypeId("424242");

        saveBlocks(posted);

        Map<String, Object> cassette = blockDesignated("A1");

        assertEquals("the case holds the block it started with and the cassette just cut", 2, blocksOnCase().size());
        assertNotNull("the first cassette of the first part is designated A1", cassette);
        assertEquals("the part a cassette is numbered within is the server's own, not the client's", "A",
                cassette.get("part_designation"));
        assertNull("and the tissue it holds is left to the work that validates it against the dictionary",
                cassette.get("tissue_type_id"));
        assertEquals("the barcode resolves the case and then the cassette within it", accessionNumber + ".A1",
                cassette.get("barcode"));
        assertEquals("a cassette is cut before it is embedded", CassetteState.CASSETTE.name(),
                cassette.get("cassette_state"));
        assertEquals("a cassette is in use from the moment it is cut", Boolean.TRUE, cassette.get("active"));
        assertNotEquals("the cassette is its own row, not the block that was already on the case",
                Integer.valueOf(LEGACY_BLOCK_ID), cassette.get("id"));
    }

    @Test
    public void aRowTheClientDidNotPost_isLeftAlone() {
        saveBlocks(postedBlock(null, "Bench A"));

        Map<String, Object> untouched = blockRow(LEGACY_BLOCK_ID);

        assertNotNull("a block absent from the post is still on the case", untouched);
        assertEquals("and keeps the location it was stored with", "Lab 1", untouched.get("location"));
        assertEquals("and its legacy number", Integer.valueOf(11), untouched.get("block_number"));
        assertEquals("and is still in use", Boolean.TRUE, untouched.get("active"));
    }

    @Test
    public void theClientCannotRenameOrRetireARow() {
        PathologyBlock posted = postedBlock(LEGACY_BLOCK_ID, "Bench A");
        posted.setDesignation("HACK");
        posted.setBarcode("HACK");
        posted.setCassetteState(CassetteState.BLOCK);
        posted.setActive(false);
        posted.setPartDesignation("Z");
        posted.setTissueTypeId("424242");

        saveBlocks(posted);

        Map<String, Object> block = blockRow(LEGACY_BLOCK_ID);
        assertNull("a designation is assigned when the row is created and is not the client's to change",
                block.get("designation"));
        assertNull("nor is the barcode printed from it", block.get("barcode"));
        assertNull("nor is the point the row has reached on the bench", block.get("cassette_state"));
        assertNull("the part a block was cut from is recorded by the work that captures it, not by a case save",
                block.get("part_designation"));
        assertNull("and neither is the tissue it holds, which is a dictionary entry that has to be validated",
                block.get("tissue_type_id"));
        assertEquals("and a row is retired through its own action, not by a field on a case save", Boolean.TRUE,
                block.get("active"));
    }

    @Test
    public void aRetiredRow_isNotEditedByASave() {
        pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID, "cassette cracked", TECHNICIAN_ID);

        saveBlocks(postedBlock(LEGACY_BLOCK_ID, "Bench Z"));

        Map<String, Object> retired = blockRow(LEGACY_BLOCK_ID);
        assertEquals("a retired block is a retained record, so a case save does not move it", "Lab 1",
                retired.get("location"));
        assertEquals("and it stays out of use", Boolean.FALSE, retired.get("active"));
    }

    @Test
    public void aRetiredDesignation_isNeverHandedOutAgain() {
        saveBlocks(postedBlock(null, "Bench A"));
        int retiredId = idOfBlockDesignated("A1");
        pathologySampleService.deactivateBlock(retiredId, "cassette cracked", TECHNICIAN_ID);

        saveBlocks(postedBlock(null, "Bench B"));

        assertNotNull("the next cassette takes the next number, not the one just retired", blockDesignated("A2"));
        Map<String, Object> retired = blockRow(retiredId);
        assertEquals("a slide cut from the retired cassette must still resolve to it, so it keeps its" + " designation",
                "A1", retired.get("designation"));
        assertEquals("the retired cassette is out of use but still on the case", Boolean.FALSE, retired.get("active"));
    }

    @Test
    public void aNewSlide_mustNameABlockOnTheCase() {
        saveBlocks(postedBlock(null, "Bench A"));
        int retiredId = idOfBlockDesignated("A1");
        pathologySampleService.deactivateBlock(retiredId, "cassette cracked", TECHNICIAN_ID);
        int slidesBefore = slidesOnCase().size();

        PathologyCaseRuleException withoutABlock = assertThrows(PathologyCaseRuleException.class,
                () -> saveSlides(postedSlide(null)));
        PathologyCaseRuleException withAForeignBlock = assertThrows(PathologyCaseRuleException.class,
                () -> saveSlides(postedSlide(424242)));
        PathologyCaseRuleException withARetiredBlock = assertThrows(PathologyCaseRuleException.class,
                () -> saveSlides(postedSlide(retiredId)));

        assertTrue("the refusal names what is missing", withoutABlock.getMessage().contains("block"));
        assertTrue("and so does the refusal of a block that belongs to another case",
                withAForeignBlock.getMessage().contains("424242"));
        assertTrue("a block taken out of use cannot be cut from, and the refusal says why",
                withARetiredBlock.getMessage().contains("retired"));
        assertEquals("a slide that cannot be traced to a block in use is not stored at all", slidesBefore,
                slidesOnCase().size());
    }

    @Test
    public void aNewSlide_isNamedUnderItsBlock() {
        saveBlocks(postedBlock(null, "Bench A"));
        int blockId = idOfBlockDesignated("A1");

        saveSlides(postedSlide(blockId));

        Map<String, Object> slide = slidesCutFrom(blockId).get(0);
        assertEquals("the first slide cut from a block is its slide 1", "1", slide.get("designation"));
        assertEquals("and its barcode resolves the case, the block and then the slide", accessionNumber + ".A1.1",
                slide.get("barcode"));
        assertEquals("a slide names the block it was cut from", Integer.valueOf(blockId), slide.get("block_id"));
        assertEquals("a slide is in use from the moment it is cut", Boolean.TRUE, slide.get("active"));

        saveSlides(postedSlide(blockId));

        assertEquals("slides are numbered within their own block", Arrays.asList("1", "2"),
                designationsOfSlidesCutFrom(blockId));

        saveSlides(postedSlide(LEGACY_BLOCK_ID));

        Map<String, Object> underLegacyBlock = slidesCutFrom(LEGACY_BLOCK_ID).get(0);
        assertEquals("a block that predates designations still numbers its own slides from one", "1",
                underLegacyBlock.get("designation"));
        assertEquals(
                "and the barcode names that block by its legacy number, so it never drops the block"
                        + " segment and collides with another block's slide",
                accessionNumber + ".11.1", underLegacyBlock.get("barcode"));
    }

    @Test
    public void deactivateBlock_retiresTheRowAndWritesOneHistoryRow() {
        Optional<PathologyBlock> retired = pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID,
                "cross-contamination suspected", TECHNICIAN_ID);

        assertTrue("a block that exists can be retired", retired.isPresent());
        assertFalse("and is out of use afterwards", retired.get().isActive());
        assertEquals("the row is still on the case, because a block is kept for years after it closes", Boolean.FALSE,
                blockRow(LEGACY_BLOCK_ID).get("active"));

        List<History> audit = historyService.getHistoryByRefIdAndRefTableId(String.valueOf(LEGACY_BLOCK_ID),
                blockReferenceTableId);
        assertEquals("retiring a block leaves exactly one audit entry", 1, audit.size());
        assertEquals("history.activity is a one-character code, so the retirement is recorded as an update", "U",
                audit.get(0).getActivity());
        assertEquals("the operator is taken from the session, not from the caller's payload", TECHNICIAN_ID,
                audit.get(0).getSysUserId());

        Map<String, Object> payload = payloadOf(audit.get(0));
        assertEquals("the verb the one-character activity code cannot carry", "PATHOLOGY_BLOCK_DEACTIVATED",
                payload.get("verb"));
        assertEquals("a block cut before designations existed is named by its legacy number", "11",
                payload.get("designation"));
        assertEquals("the reason the bench gave is kept with the event", "cross-contamination suspected",
                payload.get("reason"));

        Optional<PathologyBlock> again = pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID, "second try",
                TECHNICIAN_ID);

        assertTrue("retiring a retired block answers with the block rather than failing", again.isPresent());
        assertFalse("which is still out of use", again.get().isActive());
        assertEquals("and nothing happened, so nothing is audited a second time", 1, historyService
                .getHistoryByRefIdAndRefTableId(String.valueOf(LEGACY_BLOCK_ID), blockReferenceTableId).size());
        assertTrue("an id no block carries retires nothing",
                pathologySampleService.deactivateBlock(424242, null, TECHNICIAN_ID).isEmpty());
    }

    @Test
    public void deactivateSlide_retiresTheRowAndWritesOneHistoryRow() {
        saveBlocks(postedBlock(null, "Bench A"));
        int blockId = idOfBlockDesignated("A1");
        saveSlides(postedSlide(blockId));
        int slideId = ((Number) slidesCutFrom(blockId).get(0).get("id")).intValue();

        Optional<PathologySlide> retired = pathologySampleService.deactivateSlide(slideId, "section folded",
                TECHNICIAN_ID);

        assertTrue("a slide that exists can be retired", retired.isPresent());
        assertFalse("and is out of use afterwards", retired.get().isActive());
        assertEquals("the block it was cut from is a separate object and keeps working", Boolean.TRUE,
                blockRow(blockId).get("active"));

        List<History> audit = historyService.getHistoryByRefIdAndRefTableId(String.valueOf(slideId),
                slideReferenceTableId);
        assertEquals("retiring a slide leaves exactly one audit entry", 1, audit.size());
        assertEquals("history.activity is a one-character code, so the retirement is recorded as an update", "U",
                audit.get(0).getActivity());
        assertEquals("the operator is taken from the session, not from the caller's payload", TECHNICIAN_ID,
                audit.get(0).getSysUserId());

        Map<String, Object> payload = payloadOf(audit.get(0));
        assertEquals("the verb the one-character activity code cannot carry", "PATHOLOGY_SLIDE_DEACTIVATED",
                payload.get("verb"));
        assertEquals("named by the designation written on its label", "1", payload.get("designation"));
        assertEquals("and by the block it came from, so the tissue is still traceable", Integer.valueOf(blockId),
                payload.get("blockId"));
        assertEquals("the reason the bench gave is kept with the event", "section folded", payload.get("reason"));

        assertTrue("an id no slide carries retires nothing",
                pathologySampleService.deactivateSlide(424242, null, TECHNICIAN_ID).isEmpty());
    }

    @Test
    public void retiringABlock_leavesTheSlidesCutFromItInUse() {
        saveBlocks(postedBlock(null, "Bench A"));
        int blockId = idOfBlockDesignated("A1");
        saveSlides(postedSlide(blockId));
        int slideId = ((Number) slidesCutFrom(blockId).get(0).get("id")).intValue();

        pathologySampleService.deactivateBlock(blockId, "cassette cracked", TECHNICIAN_ID);

        assertEquals("the block is out of use", Boolean.FALSE, blockRow(blockId).get("active"));
        assertEquals("a slide already cut from it is a separate object and is still readable", Boolean.TRUE,
                slideRow(slideId).get("active"));
        assertTrue("retiring the block is not a retirement of the slide, so the slide has no audit entry",
                historyService.getHistoryByRefIdAndRefTableId(String.valueOf(slideId), slideReferenceTableId)
                        .isEmpty());
    }

    // helpers

    private void saveBlocks(PathologyBlock... blocks) {
        PathologySampleForm form = caseViewForm();
        form.setBlocks(Arrays.asList(blocks));
        pathologySampleService.updateWithFormValues(CASE_ID, form);
    }

    private void saveSlides(PathologySlideForm... slides) {
        PathologySampleForm form = caseViewForm();
        form.setSlides(Arrays.asList(slides));
        pathologySampleService.updateWithFormValues(CASE_ID, form);
    }

    /**
     * The case view posts every collection on every save, so a form that changes
     * one of them still carries the others; an empty list means the client added
     * nothing, not that the case should lose what it holds.
     */
    private PathologySampleForm caseViewForm() {
        PathologySampleForm form = new PathologySampleForm();
        form.setStatus(pathologySampleService.get(CASE_ID).getStatus());
        form.setSystemUserId(TECHNICIAN_ID);
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        return form;
    }

    private PathologyBlock postedBlock(Integer id, String location) {
        PathologyBlock block = new PathologyBlock();
        block.setId(id);
        block.setLocation(location);
        return block;
    }

    private PathologySlideForm postedSlide(Integer blockId) {
        PathologySlideForm slide = new PathologySlideForm();
        slide.setBlockId(blockId);
        slide.setLocation("Slide Storage 1");
        return slide;
    }

    private String referenceTableId(String name) {
        return referenceTablesService.getReferenceTableByName(name).getId();
    }

    private Map<String, Object> payloadOf(History history) {
        try {
            return objectMapper.readValue(history.getChanges(), new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new LIMSRuntimeException("the audit payload is not readable as JSON", e);
        }
    }

    private List<Map<String, Object>> blocksOnCase() {
        return jdbcTemplate.queryForList("SELECT id, block_number, designation, barcode, cassette_state,"
                + " part_designation, tissue_type_id, location, active FROM clinlims.pathology_block WHERE"
                + " pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    private List<Map<String, Object>> slidesOnCase() {
        return jdbcTemplate.queryForList("SELECT id, block_id, designation, barcode, location, active FROM"
                + " clinlims.pathology_slide WHERE pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    private List<Map<String, Object>> slidesCutFrom(int blockId) {
        return jdbcTemplate.queryForList("SELECT id, block_id, designation, barcode, active FROM"
                + " clinlims.pathology_slide WHERE block_id = ? ORDER BY id", blockId);
    }

    private List<String> designationsOfSlidesCutFrom(int blockId) {
        return jdbcTemplate.queryForList(
                "SELECT designation FROM clinlims.pathology_slide WHERE block_id = ?" + " ORDER BY id", String.class,
                blockId);
    }

    private Map<String, Object> blockRow(int blockId) {
        for (Map<String, Object> block : blocksOnCase()) {
            if (Integer.valueOf(blockId).equals(block.get("id"))) {
                return block;
            }
        }
        return null;
    }

    private Map<String, Object> slideRow(int slideId) {
        for (Map<String, Object> slide : slidesOnCase()) {
            if (Integer.valueOf(slideId).equals(slide.get("id"))) {
                return slide;
            }
        }
        return null;
    }

    private Map<String, Object> blockDesignated(String designation) {
        for (Map<String, Object> block : blocksOnCase()) {
            if (designation.equals(block.get("designation"))) {
                return block;
            }
        }
        return null;
    }

    private int idOfBlockDesignated(String designation) {
        Map<String, Object> block = blockDesignated(designation);
        assertNotNull("no block on the case is designated " + designation, block);
        return ((Number) block.get("id")).intValue();
    }

    private void clearPathologyAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table IN (?, ?)",
                Integer.parseInt(blockReferenceTableId), Integer.parseInt(slideReferenceTableId));
    }
}
