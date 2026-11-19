package org.openelisglobal.pathology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.function.ThrowingRunnable;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.historyservices.ProgramSampleHistoryService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologySampleService.LabelVerification;
import org.openelisglobal.program.service.RequestDraft;
import org.openelisglobal.program.service.RequestStatusChange;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Saves the fixture's pathology case the way the case view does and reads back
 * what the save stored, so each test states only the rule it pins.
 *
 * <p>
 * Audit entries and stage events survive the fixture reload, so they are
 * cleared before and after each test; otherwise a count asserted in one test
 * would include the entries another test wrote.
 */
public abstract class PathologyCaseTestSupport extends BaseWebContextSensitiveTest {

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

    /**
     * The fixture's request on case 1: open, text, with no kind, target, author or
     * date, as a request on file from before bench requests had them.
     */
    protected static final int FIXTURE_REQUEST_ID = 301;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected PathologySampleService pathologySampleService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ReferenceTablesService referenceTablesService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    protected String accessionNumber;
    protected String blockReferenceTableId;
    protected String slideReferenceTableId;
    protected String requestReferenceTableId;

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
        requestReferenceTableId = referenceTableId("PATHOLOGY_REQUEST");
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

    /**
     * A cassette A1 cut at grossing and stored as a block, so slides are cut from
     * it.
     */
    protected int cutEmbeddedBlock() {
        saveBlocks(postedBlock(null, "Bench A"));
        int blockId = idOfBlockDesignated("A1");
        storeAsBlock(blockId);
        return blockId;
    }

    /**
     * Checks a slide's label with the barcodes stored on it and its block, as a
     * scanner would read them.
     */
    protected LabelVerification verifyStoredLabel(int slideId) {
        Map<String, Object> slide = slideRow(slideId);
        Assert.assertNotNull("no slide on the case has id " + slideId, slide);
        String blockBarcode = (String) blockRow(((Number) slide.get("block_id")).intValue()).get("barcode");
        return pathologySampleService
                .verifySlideLabel(slideId, blockBarcode, (String) slide.get("barcode"), TECHNICIAN_ID)
                .orElseThrow(() -> new AssertionError("no slide carries id " + slideId));
    }

    /** The call is refused with the message id the screen translates. */
    protected LocalizedValidationException assertRefusedWith(String errorCode, ThrowingRunnable call) {
        LocalizedValidationException refusal = Assert.assertThrows(LocalizedValidationException.class, call);
        Assert.assertEquals("the refusal carries the message the screen translates", errorCode, refusal.getErrorCode());
        return refusal;
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

    protected PathologySlideForm postedSlide(Integer blockId, String stainId) {
        PathologySlideForm slide = postedSlide(blockId);
        slide.setStainId(stainId);
        return slide;
    }

    /**
     * Cuts slides from one embedded block A1 in one save, each carrying the stain
     * given, and answers their ids in the order they were cut (A1.1, A1.2, ...).
     */
    protected List<Integer> cutSlidesCarrying(int count, String stainId) {
        int blockId = cutEmbeddedBlock();
        PathologySlideForm[] slides = new PathologySlideForm[count];
        for (int i = 0; i < count; i++) {
            slides[i] = postedSlide(blockId, stainId);
        }
        saveSlides(slides);
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> slide : slidesCutFrom(blockId)) {
            ids.add(((Number) slide.get("id")).intValue());
        }
        Assert.assertEquals("setup: every slide is cut", count, ids.size());
        return ids;
    }

    /**
     * What staining and the coverslip check recorded on a slide, who recorded it
     * and when, and the stain it carries.
     */
    protected Map<String, Object> slideWork(int slideId) {
        return jdbcTemplate.queryForMap(
                "SELECT stain_id, stain_status, stain_status_at, stain_status_by,"
                        + " coverslip_qc, coverslip_qc_at, coverslip_qc_by FROM clinlims.pathology_slide WHERE id = ?",
                slideId);
    }

    /** The audit entries with that verb on the slide, oldest first. */
    protected List<Map<String, Object>> slideEntries(int slideId, String verb) {
        List<Map<String, Object>> entries = new ArrayList<>();
        historyService.getHistoryByRefIdAndRefTableId(String.valueOf(slideId), slideReferenceTableId).stream()
                .sorted((first, second) -> Long.compare(Long.parseLong(first.getId()), Long.parseLong(second.getId())))
                .map(this::payloadOf).filter(payload -> verb.equals(payload.get("verb"))).forEach(entries::add);
        return entries;
    }

    /**
     * The case's own save diffs, which are XML; the named events that share its
     * reference row are JSON and are left out.
     */
    protected List<History> caseSaveDiffs() {
        List<History> diffs = new ArrayList<>();
        for (History history : caseAudit()) {
            if (!changesOf(history).startsWith("{")) {
                diffs.add(history);
            }
        }
        return diffs;
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

    // two calls landing together

    /** What two calls landing together each ended with, null for success. */
    protected record Outcome(Throwable held, Throwable other) {
    }

    /**
     * Runs the first call in a transaction held open while the second runs on
     * another thread, gives the second a few seconds (it may be waiting on a row
     * the first holds), commits the first, then waits for the second.
     */
    protected Outcome holdOpenWhile(Runnable held, Callable<?> other) {
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newSingleThreadExecutor());
        AtomicReference<Future<?>> otherRun = new AtomicReference<>();
        Throwable heldFailure = null;
        try {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    held.run();
                    otherRun.set(pool.submit(other));
                    try {
                        otherRun.get().get(3, TimeUnit.SECONDS);
                    } catch (TimeoutException | ExecutionException waiting) {
                        // Still waiting on a row this transaction holds, or already over.
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RuntimeException e) {
                heldFailure = e;
            }
            Throwable otherFailure = null;
            try {
                otherRun.get().get(60, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                otherFailure = e.getCause();
            } catch (InterruptedException | TimeoutException e) {
                throw new AssertionError("the second call did not finish", e);
            }
            return new Outcome(heldFailure, otherFailure);
        } finally {
            pool.shutdownNow();
        }
    }

    // bench requests

    /**
     * Closes the fixture's open request, for a test whose subject is not a request:
     * an open request holds a case back from the pathologist's queue.
     */
    protected void closeTheFixtureRequest() {
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = 'COMPLETED' WHERE id = ?",
                FIXTURE_REQUEST_ID);
    }

    protected PathologyRequest raise(RequestDraft draft) {
        return pathologySampleService.raiseRequest(CASE_ID, draft, TECHNICIAN_ID);
    }

    protected static RequestDraft blockRequest(String instruction) {
        return new RequestDraft("BLOCK", null, null, null, null, instruction);
    }

    protected static RequestDraft slideRequest(Integer blockId, String instruction) {
        return new RequestDraft("SLIDE", blockId, null, null, null, instruction);
    }

    protected static RequestDraft stainRequest(Integer slideId, String stainId, String instruction) {
        return new RequestDraft("STAIN", null, slideId, stainId, null, instruction);
    }

    protected PathologyRequest changeStatus(int requestId, String from, String status) {
        return changeStatus(requestId, new RequestStatusChange(from, status, null, null, null));
    }

    protected PathologyRequest changeStatus(int requestId, RequestStatusChange change) {
        return pathologySampleService.changeRequestStatus(CASE_ID, requestId, change, TECHNICIAN_ID);
    }

    /** Every request on the case as stored, oldest first. */
    protected List<Map<String, Object>> requestsOnCase() {
        return jdbcTemplate.queryForList("SELECT id, status, type, value, request_kind, target_block_id,"
                + " target_slide_id, stain_id, priority, result_block_id, result_slide_id, cancel_reason, raised_at,"
                + " raised_by, last_updated FROM clinlims.pathology_request WHERE pathology_sample_id = ? ORDER BY id",
                CASE_ID);
    }

    /** The requests lines of the order's audit trail, as old and new value. */
    protected List<List<String>> requestLines() {
        List<AuditTrailItem> items = new TransactionTemplate(transactionManager).execute(
                status -> new ProgramSampleHistoryService(pathologySampleService.get(CASE_ID)).getAuditTrailItems());
        return items.stream().filter(item -> "requests".equals(item.getIdentifier())).map(item -> List
                .of(StringUtils.defaultString(item.getOldValue()), StringUtils.defaultString(item.getNewValue())))
                .collect(Collectors.toList());
    }

    protected Map<String, Object> requestRow(int requestId) {
        return jdbcTemplate.queryForMap("SELECT id, pathology_sample_id, status, type, value, request_kind,"
                + " target_block_id, target_slide_id, stain_id, priority, result_block_id, result_slide_id,"
                + " cancel_reason, raised_at, raised_by, last_updated FROM clinlims.pathology_request WHERE id = ?",
                requestId);
    }

    /** The audit entries with that verb on the request, oldest first. */
    protected List<History> requestEntries(int requestId, String verb) {
        return historyService.getHistoryByRefIdAndRefTableId(String.valueOf(requestId), requestReferenceTableId)
                .stream()
                .sorted((first, second) -> Long.compare(Long.parseLong(first.getId()), Long.parseLong(second.getId())))
                .filter(history -> verb.equals(payloadOf(history).get("verb"))).collect(Collectors.toList());
    }

    /** Every audit entry on any request. */
    protected int requestAuditCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.history WHERE reference_table = ?",
                Integer.class, Integer.parseInt(requestReferenceTableId));
    }

    /**
     * Every audit entry on a block, slide or request, the case's own, and its
     * moves.
     */
    private void clearPathologyAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table IN (?, ?, ?)",
                Integer.parseInt(blockReferenceTableId), Integer.parseInt(slideReferenceTableId),
                Integer.parseInt(requestReferenceTableId));
        PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_ID);
    }
}
