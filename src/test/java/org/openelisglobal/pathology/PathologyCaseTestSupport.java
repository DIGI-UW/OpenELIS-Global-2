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
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saves the fixture's pathology case the way the case view does and reads back
 * what the save stored, so each test states only the rule it pins.
 *
 * <p>
 * Audit entries survive the fixture reload, so they are cleared before and
 * after each test; otherwise a count asserted in one test would include the
 * entries another test wrote.
 */
abstract class PathologyCaseTestSupport extends BaseWebContextSensitiveTest {

    protected static final int CASE_ID = 1;

    /**
     * The block the fixture seeds, which carries a legacy number, no designation
     * and no tissue type.
     */
    protected static final int LEGACY_BLOCK_ID = 101;

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
     * changes one still carries the others, and no decalcification flag unless a
     * test sets one. An empty list means the client added nothing, not that the
     * case should lose what it holds.
     */
    protected PathologySampleForm caseViewForm() {
        PathologySampleForm form = new PathologySampleForm();
        form.setStatus(pathologySampleService.get(CASE_ID).getStatus());
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

    /** Every audit entry on a block or slide, and the case's own. */
    private void clearPathologyAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table IN (?, ?)",
                Integer.parseInt(blockReferenceTableId), Integer.parseInt(slideReferenceTableId));
        clearCaseAudit();
    }
}
