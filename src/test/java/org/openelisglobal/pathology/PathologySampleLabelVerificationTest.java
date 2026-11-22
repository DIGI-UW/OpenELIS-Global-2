package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologySampleService.LabelVerification;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * A slide's printed label is checked against the block it was cut from before
 * the slide is sectioned (FR-7.3 to FR-7.5).
 *
 * <p>
 * Nothing checked a label before: a slide could be sectioned under a label
 * printed for another block, and nobody would know. These tests pin a match on
 * the two barcodes, stamped with the session user and the server clock and
 * audited once; a mismatch that changes nothing on the slide but is audited all
 * the same; the identifiers as printed taken only where the laboratory has no
 * scanner; a second matching check of a verified slide as a no-op, and a wrong
 * one as a mismatch; the refusals; three checks at once ending in one stamp;
 * and the stamp kept out of the reach of a case save.
 */
public class PathologySampleLabelVerificationTest extends PathologyCaseTestSupport {

    private StageSwitches switches;

    private int blockId;
    private String blockBarcode;
    private int slideId;
    private String slideBarcode;

    /**
     * One slide cut from an embedded block A1, at Microtomy, with the scanner
     * setting on; the entries its cutting left are cleared so each test counts only
     * what the check writes.
     */
    @Before
    public void cutOneSlide() {
        switches = StageSwitches.allOn();
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_SCAN_VERIFICATION_REQUIRED, "true");
        storeAt(PathologyStatus.MICROTOMY);
        blockId = cutEmbeddedBlock();
        blockBarcode = (String) blockRow(blockId).get("barcode");
        saveSlides(postedSlide(blockId));
        Map<String, Object> slide = slidesCutFrom(blockId).get(0);
        slideId = ((Number) slide.get("id")).intValue();
        slideBarcode = (String) slide.get("barcode");
        assertEquals("setup: the slide is barcoded under its block", accessionNumber + ".A1.1", slideBarcode);
        clearSlideAudit();
        clearCaseAudit();
    }

    @After
    public void restoreTheSwitches() {
        switches.close();
    }

    // ---- a match ----

    @Test
    public void verifying_withTheBlockAndSlideBarcodes_stampsTheSlideAndWritesOneVerifiedRow() {
        LabelVerification outcome = verify(blockBarcode, slideBarcode);

        assertTrue("the two barcodes belong together, so the label is verified", outcome.match());
        assertNotNull("the slide carries when it was verified", verifiedAt());
        assertEquals("and by the signed-in user, never a typed name", TECHNICIAN_ID, verifiedBy());

        List<History> audit = slideAudit();
        assertEquals("the check leaves exactly one entry against the slide", 1, audit.size());
        assertEquals("by the operator who checked it", TECHNICIAN_ID, audit.get(0).getSysUserId());
        Map<String, Object> payload = payloadOf(audit.get(0));
        assertEquals("the entry records the event's facts and nothing else",
                List.of("verb", "blockBarcode", "slideBarcode", "match", "mode"), List.copyOf(payload.keySet()));
        assertEquals(PathologySampleService.PATHOLOGY_SLIDE_LABEL_VERIFIED, payload.get("verb"));
        assertEquals("the block as it was scanned", blockBarcode, payload.get("blockBarcode"));
        assertEquals("the slide label as it was scanned", slideBarcode, payload.get("slideBarcode"));
        assertEquals(Boolean.TRUE, payload.get("match"));
        assertEquals("checked with a scanner", "scan", payload.get("mode"));
    }

    @Test
    public void aScanThatDiffersOnlyInCaseAndSpaces_stillMatches() {
        LabelVerification outcome = verify("  " + blockBarcode.toLowerCase() + " ", slideBarcode.toLowerCase());

        assertTrue("a scanner's case and stray spaces say nothing about which slide it is", outcome.match());
        assertNotNull(verifiedAt());
        assertEquals("the entry keeps the identifier as given, trimmed", blockBarcode.toLowerCase(),
                payloadOf(slideAudit().get(0)).get("blockBarcode"));
    }

    // ---- a mismatch ----

    /**
     * With a scanner the designations alone are not the label: anybody can type
     * "A1" without the block in front of them.
     */
    @Test
    public void aTypedDesignation_withTheScannerOn_isAMismatch() {
        LabelVerification outcome = verify("A1", "A1.1");

        assertFalse("the designations are not the barcodes the scanner reads", outcome.match());
        assertNull("so the slide stays unverified", verifiedAt());
        List<History> audit = slideAudit();
        assertEquals("the mismatch is still recorded once", 1, audit.size());
        Map<String, Object> payload = payloadOf(audit.get(0));
        assertEquals("naming both identifiers as given", List.of("verb", "blockBarcode", "slideBarcode", "mode"),
                List.copyOf(payload.keySet()));
        assertEquals(PathologySampleService.PATHOLOGY_SLIDE_LABEL_MISMATCH, payload.get("verb"));
        assertEquals("A1", payload.get("blockBarcode"));
        assertEquals("A1.1", payload.get("slideBarcode"));
        assertEquals("scan", payload.get("mode"));
    }

    @Test
    public void anotherBlocksBarcode_isAMismatchNamingBothAsGiven() {
        saveBlocks(postedBlock(null, "Bench B"));
        String otherBlockBarcode = (String) blockRow(idOfBlockDesignated("A2")).get("barcode");
        clearSlideAudit();

        LabelVerification outcome = verify(otherBlockBarcode, slideBarcode);

        assertFalse("a slide of A1 scanned against A2 is the error the check exists for", outcome.match());
        assertNull("the slide stays unverified", verifiedAt());
        Map<String, Object> payload = payloadOf(slideAudit().get(0));
        assertEquals(PathologySampleService.PATHOLOGY_SLIDE_LABEL_MISMATCH, payload.get("verb"));
        assertEquals(otherBlockBarcode, payload.get("blockBarcode"));
        assertEquals(slideBarcode, payload.get("slideBarcode"));
    }

    // ---- without a scanner ----

    @Test
    public void withoutAScanner_theIdentifiersAsPrintedMatch() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_SCAN_VERIFICATION_REQUIRED, "false");

        LabelVerification outcome = verify("a1", " A1.1 ");

        assertTrue("where there is no scanner, the block and slide as printed confirm the label", outcome.match());
        assertNotNull(verifiedAt());
        Map<String, Object> payload = payloadOf(slideAudit().get(0));
        assertEquals(PathologySampleService.PATHOLOGY_SLIDE_LABEL_VERIFIED, payload.get("verb"));
        assertEquals("the entry says the label was confirmed by hand", "confirm", payload.get("mode"));
    }

    @Test
    public void withoutAScanner_theWrongSlideIsStillAMismatch() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_SCAN_VERIFICATION_REQUIRED, "false");

        LabelVerification outcome = verify("A1", "A1.2");

        assertFalse("the confirmation still has to name this slide", outcome.match());
        assertNull(verifiedAt());
        assertEquals("confirm", payloadOf(slideAudit().get(0)).get("mode"));
    }

    // ---- a verified slide ----

    @Test
    public void aSecondMatchingScan_ofAVerifiedSlide_changesNothingAndWritesNoRow() {
        verify(blockBarcode, slideBarcode);
        Timestamp first = verifiedAt();

        LabelVerification again = verify(blockBarcode, slideBarcode);

        assertTrue("the same label scanned again still matches", again.match());
        assertEquals("its stamp is the first one", first, verifiedAt());
        assertEquals("and no second entry is written", 1, slideAudit().size());
    }

    /**
     * A page that still shows the slide unverified may scan it again. A wrong scan
     * then is the error the check exists for, so it is answered and recorded as a
     * mismatch, and the earlier stamp stands.
     */
    @Test
    public void aWrongScan_ofAVerifiedSlide_isAMismatchAndKeepsTheStamp() {
        verify(blockBarcode, slideBarcode);
        Timestamp first = verifiedAt();
        clearSlideAudit();

        LabelVerification again = verify("A1", "A1.1");

        assertFalse("a wrong scan is never answered as a match, verified slide or not", again.match());
        assertEquals("the stamp from the earlier match stands", first, verifiedAt());
        List<History> audit = slideAudit();
        assertEquals("the wrong scan is recorded once", 1, audit.size());
        assertEquals(PathologySampleService.PATHOLOGY_SLIDE_LABEL_MISMATCH, payloadOf(audit.get(0)).get("verb"));
    }

    // ---- refusals ----

    @Test
    public void aBlankField_isRefused() {
        assertRefusedWith("pathology.error.scanBothFields", () -> verify("  ", slideBarcode));
        assertRefusedWith("pathology.error.scanBothFields", () -> verify(blockBarcode, null));

        assertNull("the slide stays unverified", verifiedAt());
        assertTrue("and nothing is recorded", slideAudit().isEmpty());
    }

    @Test
    public void aRetiredSlide_isRefused() {
        pathologySampleService.deactivateSlide(slideId, "section folded", TECHNICIAN_ID);
        clearSlideAudit();

        assertRefusedWith("pathology.error.slideRetired", () -> verify(blockBarcode, slideBarcode));

        assertNull(verifiedAt());
        assertTrue(slideAudit().isEmpty());
    }

    @Test
    public void aSlideWithNoBlockOnRecord_isRefused() {
        LocalizedValidationException refusal = assertThrows(LocalizedValidationException.class,
                () -> pathologySampleService.verifySlideLabel(LEGACY_SLIDE_ID, blockBarcode, "anything",
                        TECHNICIAN_ID));

        assertEquals("a slide that names no block has nothing to be checked against", "pathology.error.slideHasNoBlock",
                refusal.getErrorCode());
        assertNull("and it stays unverified", PathologyCaseRows.labelVerifiedAt(jdbcTemplate, LEGACY_SLIDE_ID));
    }

    @Test
    public void aSlideOfASignedOutCase_isRefused() {
        storeAt(PathologyStatus.COMPLETED);

        assertRefusedWith("pathology.error.caseCompleted", () -> verify(blockBarcode, slideBarcode));

        assertNull(verifiedAt());
        assertTrue(slideAudit().isEmpty());
    }

    @Test
    public void anIdNoSlideCarries_isEmpty() {
        assertEquals(Optional.empty(), pathologySampleService.verifySlideLabel(424242, "x", "y", TECHNICIAN_ID));
    }

    // ---- three checks at once ----

    /**
     * Three pages verify the same slide at once. Whichever commits first stamps it;
     * a check that overlaps loses on the slide's version check and writes nothing,
     * and a check that arrives after the first committed finds the slide verified
     * and writes nothing either. Either way one stamp and one entry remain.
     */
    @Test
    public void threeConcurrentVerifications_leaveOneStampAndOneVerifiedRow() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(3));
        List<Future<LabelVerification>> checks = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Callable<LabelVerification> check = () -> {
                    startGate.await();
                    return verify(blockBarcode, slideBarcode);
                };
                checks.add(pool.submit(check));
            }
            startGate.countDown();

            int answered = 0;
            for (Future<LabelVerification> check : checks) {
                try {
                    assertTrue("a check that is answered reads the slide as verified",
                            check.get(60, TimeUnit.SECONDS).match());
                    answered++;
                } catch (ExecutionException e) {
                    assertTrue("a check that is refused lost the slide's version check, not " + e.getCause(),
                            StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                }
            }
            assertTrue("at least one check is answered", answered >= 1);
            assertNotNull("the slide is verified", verifiedAt());
            List<History> audit = slideAudit();
            assertEquals("exactly one entry records it", 1, audit.size());
            assertEquals(PathologySampleService.PATHOLOGY_SLIDE_LABEL_VERIFIED, payloadOf(audit.get(0)).get("verb"));
            assertEquals("and exactly one entry in the case's history", 1, caseAudit().size());
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- the stamp and the case save ----

    /**
     * The stamp is a change to the case's slides, so the case history records it
     * once; the save that posts the slide back afterwards finds nothing changed and
     * leaves the stamp alone.
     */
    @Test
    public void theStamp_leavesOneCaseHistoryRow_andAPlainSaveAfterItNone() {
        verify(blockBarcode, slideBarcode);
        Timestamp stamped = verifiedAt();

        List<History> afterStamp = caseAudit();
        assertEquals("the check leaves one entry in the case's history", 1, afterStamp.size());
        assertTrue("which records the label check: " + changesOf(afterStamp.get(0)),
                changesOf(afterStamp.get(0)).contains("Label verified"));

        PathologySlideForm postedBack = postedSlide(blockId);
        postedBack.setId(slideId);
        saveSlides(postedBack);

        assertEquals("a save that posts the slide back as it is adds nothing to the case's history", 1,
                caseAudit().size());
        assertEquals("and keeps the stamp", stamped, verifiedAt());
    }

    @Test
    public void aStampPostedWithTheSave_isIgnored() {
        PathologySlideForm postedBack = postedSlide(blockId);
        postedBack.setId(slideId);
        postedBack.setLabelVerifiedAt(new Timestamp(System.currentTimeMillis()));
        postedBack.setLabelVerifiedBy(TECHNICIAN_ID);
        PathologySlideForm newSlide = postedSlide(blockId);
        newSlide.setLabelVerifiedAt(new Timestamp(System.currentTimeMillis()));
        newSlide.setLabelVerifiedBy(TECHNICIAN_ID);

        saveSlides(postedBack, newSlide);

        assertNull("only the check stamps a slide; a save posting a stamp leaves it unverified", verifiedAt());
        assertNull("and so does cutting a new slide",
                jdbcTemplate.queryForObject(
                        "SELECT max(label_verified_by) FROM clinlims.pathology_slide WHERE block_id = ?", Object.class,
                        blockId));
        assertEquals("setup: the new slide was cut", 2, slidesCutFrom(blockId).size());
    }

    // ---- the dashboard search ----

    /**
     * A block or slide label now scans to the row's own barcode, not the bare lab
     * number. Scanned into the Pathology dashboard search, it still finds its case,
     * whatever separator the laboratory's scheme uses.
     */
    @Test
    public void aScannedBlockOrSlideLabel_findsItsCaseInTheDashboardSearch() {
        assertEquals("a scanned slide label finds the case it was cut for", List.of(CASE_ID), search(slideBarcode));
        assertEquals("and so does a scanned block label", List.of(CASE_ID), search(" " + blockBarcode + " "));

        // Barcodes as a scheme with a dash separator writes them: the lab number
        // cannot be read off the front, so the label is found by its barcode.
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET barcode = ? WHERE id = ?", accessionNumber + "-A1",
                blockId);
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET barcode = ? WHERE id = ?", accessionNumber + "-A1-1",
                slideId);
        assertEquals("a slide label under another separator finds its case", List.of(CASE_ID),
                search(accessionNumber + "-A1-1"));
        assertEquals("and so does a block label", List.of(CASE_ID), search(accessionNumber + "-A1"));
        assertTrue("a barcode no order, block or slide carries finds nothing", search("NOSUCHLAB-Z9-9").isEmpty());
    }

    // helpers

    /** The ids of the cases at Microtomy the dashboard search finds for a term. */
    private List<Integer> search(String term) {
        return pathologySampleService.searchWithStatusAndTerm(List.of(PathologyStatus.MICROTOMY), term).stream()
                .map(PathologySample::getId).collect(Collectors.toList());
    }

    private LabelVerification verify(String block, String slide) {
        return pathologySampleService.verifySlideLabel(slideId, block, slide, TECHNICIAN_ID)
                .orElseThrow(() -> new AssertionError("the slide was not found"));
    }

    private Timestamp verifiedAt() {
        return PathologyCaseRows.labelVerifiedAt(jdbcTemplate, slideId);
    }

    private String verifiedBy() {
        Object verifiedBy = jdbcTemplate.queryForObject(
                "SELECT label_verified_by FROM clinlims.pathology_slide WHERE id = ?", Object.class, slideId);
        return verifiedBy == null ? null : String.valueOf(((Number) verifiedBy).intValue());
    }

    private List<History> slideAudit() {
        return historyService.getHistoryByRefIdAndRefTableId(String.valueOf(slideId), slideReferenceTableId);
    }

}
