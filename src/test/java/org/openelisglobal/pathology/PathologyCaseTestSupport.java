package org.openelisglobal.pathology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saves the fixture's pathology case the way the case view does and reads back
 * what the save stored, so each test states only the rule it pins.
 *
 * <p>
 * Audit entries and stage events survive the fixture reload, so they are
 * cleared before and after each test; otherwise a count asserted in one test
 * would include the entries another test wrote.
 */
abstract class PathologyCaseTestSupport extends BaseWebContextSensitiveTest {

    protected static final int CASE_ID = 1;

    /**
     * The block the fixture seeds, which carries a legacy number, no designation
     * and no tissue type.
     */
    protected static final int LEGACY_BLOCK_ID = 101;

    /** The slide the fixture seeds, in use and with no block recorded. */
    protected static final int LEGACY_SLIDE_ID = 201;

    /**
     * technician1, the fixture's own user, so audit attribution names a real one.
     */
    protected static final String TECHNICIAN_ID = "1001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected PathologySampleService pathologySampleService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ReferenceTablesService referenceTablesService;

    protected String accessionNumber;
    protected String blockReferenceTableId;
    protected String slideReferenceTableId;

    @Before
    public void loadPathologyCase() throws Exception {
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

    @After
    public void clearPathologyAuditAfterwards() {
        clearPathologyAudit();
    }

    protected void save(PathologySampleForm form) {
        pathologySampleService.updateWithFormValues(CASE_ID, form);
    }

    protected void saveBlocks(PathologyBlock... blocks) {
        PathologySampleForm form = caseViewForm();
        form.setBlocks(Arrays.asList(blocks));
        save(form);
    }

    /**
     * What the case view posts: every collection on every save, so a form that
     * changes one still carries the others, the stored macroscopic description, and
     * no decalcification flag unless a test sets one. An empty list means the
     * client added nothing, not that the case should lose what it holds.
     */
    protected PathologySampleForm caseViewForm() {
        PathologySampleForm form = new PathologySampleForm();
        form.setGrossExam(pathologySampleService.get(CASE_ID).getGrossExam());
        form.setSystemUserId(TECHNICIAN_ID);
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        return form;
    }

    protected PathologyBlock postedBlock(Integer id, String location) {
        return postedBlock(id, location, null);
    }

    protected PathologyBlock postedBlock(Integer id, String location, String tissueTypeId) {
        PathologyBlock block = new PathologyBlock();
        block.setId(id);
        block.setLocation(location);
        block.setTissueTypeId(tissueTypeId);
        return block;
    }

    protected PathologySampleForm flagged(boolean decalcificationRequired) {
        PathologySampleForm form = caseViewForm();
        form.setDecalcificationRequired(decalcificationRequired);
        return form;
    }

    protected boolean storedFlag() {
        return jdbcTemplate.queryForObject(
                "SELECT decalcification_required FROM clinlims.pathology_sample WHERE id = ?", Boolean.class, CASE_ID);
    }

    /** See {@link PathologyCaseRows#storeAt}. */
    protected void storeAt(PathologyStatus stage) {
        PathologyCaseRows.storeAt(jdbcTemplate, CASE_ID, stage);
    }

    protected String storedStage() {
        return PathologyCaseRows.storedStage(jdbcTemplate, CASE_ID);
    }

    private String referenceTableId(String name) {
        return referenceTablesService.getReferenceTableByName(name).getId();
    }

    protected Map<String, Object> payloadOf(History history) {
        try {
            return objectMapper.readValue(history.getChanges(), new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new LIMSRuntimeException("the audit payload is not readable as JSON", e);
        }
    }

    protected List<Map<String, Object>> blocksOnCase() {
        return jdbcTemplate.queryForList("SELECT id, block_number, designation, barcode, cassette_state,"
                + " part_designation, tissue_type_id, location, active FROM clinlims.pathology_block WHERE"
                + " pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    protected Map<String, Object> blockRow(int blockId) {
        for (Map<String, Object> block : blocksOnCase()) {
            if (Integer.valueOf(blockId).equals(block.get("id"))) {
                return block;
            }
        }
        return null;
    }

    protected Map<String, Object> blockDesignated(String designation) {
        for (Map<String, Object> block : blocksOnCase()) {
            if (designation.equals(block.get("designation"))) {
                return block;
            }
        }
        return null;
    }

    protected int idOfBlockDesignated(String designation) {
        Map<String, Object> block = blockDesignated(designation);
        Assert.assertNotNull("no block on the case is designated " + designation, block);
        return ((Number) block.get("id")).intValue();
    }

    /**
     * Stores a row as a block by SQL, for a test whose subject is a slide cut from
     * it rather than the embedding. It skips the service, so no embedding audit row
     * is written and the case's stage is not checked.
     */
    protected void storeAsBlock(int blockId) {
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET cassette_state = 'BLOCK' WHERE id = ?", blockId);
    }

    protected void saveSlides(PathologySlideForm... slides) {
        PathologySampleForm form = caseViewForm();
        form.setSlides(Arrays.asList(slides));
        save(form);
    }

    protected PathologySlideForm postedSlide(Integer blockId) {
        PathologySlideForm slide = new PathologySlideForm();
        slide.setBlockId(blockId);
        slide.setLocation("Slide Storage 1");
        return slide;
    }

    protected List<Map<String, Object>> slidesOnCase() {
        return jdbcTemplate.queryForList("SELECT id, block_id, designation, barcode, location, level, stain_id, active"
                + " FROM clinlims.pathology_slide WHERE pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    protected List<Map<String, Object>> slidesCutFrom(int blockId) {
        return jdbcTemplate.queryForList("SELECT id, block_id, designation, barcode, location, level, stain_id, active"
                + " FROM clinlims.pathology_slide WHERE block_id = ? ORDER BY id", blockId);
    }

    protected Map<String, Object> slideRow(int slideId) {
        for (Map<String, Object> slide : slidesOnCase()) {
            if (Integer.valueOf(slideId).equals(slide.get("id"))) {
                return slide;
            }
        }
        return null;
    }

    /**
     * The slide entries a test's setup left, such as the creation entry of a slide
     * it cut, so the test counts only the entries its own action writes.
     */
    protected void clearSlideAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table = ?",
                Integer.parseInt(slideReferenceTableId));
    }

    protected List<History> caseAudit() {
        return historyService.getHistoryByRefIdAndRefTableId(String.valueOf(CASE_ID),
                referenceTableId("PATHOLOGY_SAMPLE"));
    }

    protected void clearCaseAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table = ? AND reference_id = ?",
                Integer.parseInt(referenceTableId("PATHOLOGY_SAMPLE")), CASE_ID);
    }

    protected String changesOf(History history) {
        return new String(history.getChanges(), StandardCharsets.UTF_8);
    }

    /** Every audit entry on a block or slide, the case's own, and its moves. */
    private void clearPathologyAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table IN (?, ?)",
                Integer.parseInt(blockReferenceTableId), Integer.parseInt(slideReferenceTableId));
        PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_ID);
    }
}
