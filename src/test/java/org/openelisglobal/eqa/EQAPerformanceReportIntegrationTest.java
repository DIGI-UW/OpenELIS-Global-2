package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQACycleRestController;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.service.EQAParticipantResultService;
import org.openelisglobal.eqa.service.EQAPerformanceReportPDFService;
import org.openelisglobal.eqa.service.EQAReportCommentService;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * OGC-933 — the printed CPHL-format performance report against the real schema:
 * the summaries, the scoring table and the cycle identifiers a CPHL reviewer
 * signs off on.
 *
 * <p>
 * Z-scores are written straight to the row here. The report only reads
 * {@code z_score}; the production writers are participant-result scoring and
 * the external score intake, not this report.
 *
 * <p>
 * Assertions run against the rendered page, including the labels:
 * {@code AppTestConfig} wires the real {@code /languages/message} bundle into
 * {@code MessageUtil}, so a key that never got a translation reaches the page
 * as itself and {@link #reportPrintsTranslatedLabelsNotRawKeysOrEnumNames}
 * fails.
 */
public class EQAPerformanceReportIntegrationTest extends EQASpineTestBase {

    private static final long SECTION_ID = 9812L;
    private static final String SECTION_NAME = "EQA Report Section";
    private static final long HIV_ANALYTE = 9820L;
    private static final long CD4_ANALYTE = 9821L;
    private static final long TB_ANALYTE = 9822L;
    private static final long PENDING_ANALYTE = 9823L;
    private static final long DRAFT_ANALYTE = 9824L;
    private static final long BENCH_ANALYTE = 9825L;
    private static final long ENROLLMENT = 9920L;
    private static final long SEALED_ANALYTE = 9830L;
    private static final long UNBLINDED_ANALYTE = 9831L;
    private static final long OTHER_ENROLLMENT = 9921L;

    // The analysis-linked lane: its own section, so a row resolved through the
    // analysis cannot be confused with the scheme's fallback section.
    private static final long BENCH_SECTION_ID = 9813L;
    private static final String BENCH_SECTION_NAME = "Molecular Bench";
    private static final long TEST_ID = 9814L;
    private static final long SAMPLE_ID = 9815L;
    private static final long SAMPLE_ITEM_ID = 9816L;
    private static final long ANALYSIS_ID = 9817L;

    private static final String PROGRAMME_SUMMARY = "Programme summary";
    private static final String SECTION_SUMMARY = "Section summary";
    private static final String SCORING_DETAIL = "Scoring detail";
    private static final String SIGN_OFF = "Review and sign-off";
    private static final String COMMENTS_TITLE = "Interpretive comments";
    private static final String COMMENT_ONE = "Cycle reviewed against the scheme's acceptance limits.";
    private static final String COMMENT_TWO = "Repeat the unacceptable analyte in the next cycle.";
    private static final String UNRELATED_COMMENT = "Wording from another dictionary category.";
    private static final String OTHER_CATEGORY = "EQA Report Comment Test Foil";

    @Autowired
    private EQAPerformanceReportPDFService reportService;

    @Autowired
    private EQAReportCommentService reportCommentService;
    @Autowired
    private EQALabProgramEnrollmentService enrollmentService;

    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;
    @Autowired
    private EQAParticipantResultService participantResultService;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private SampleEQAService sampleEQAService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    // eqa.controller.* is excluded from the test component scan — construct it
    private EQACycleRestController controller;

    private EQAProgram scheme;
    private EQACycle cycle;
    private EQARound round;

    @Before
    public void seedCycleWithScores() {
        controller = new EQACycleRestController(cycleService, sampleEQAService, sampleService, analysisService,
                resultService, reportService, reportCommentService, systemUserService, enrollmentService);

        jdbc.update("INSERT INTO clinlims.localization (id, description)"
                + " SELECT ?, 'EQA Report Section' WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.localization WHERE id = ?)", SECTION_ID, SECTION_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                        + " name_localization_id) SELECT ?, ?, ?, 'N', ?, ? WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.test_section WHERE id = ?)",
                SECTION_ID, SECTION_NAME, SECTION_NAME, SECTION_ID, SECTION_ID, SECTION_ID);
        seedAnalyte(HIV_ANALYTE, "HIV Viral Load");
        seedAnalyte(CD4_ANALYTE, "CD4 Count");
        seedAnalyte(TB_ANALYTE, "TB Smear Grade");
        seedAnalyte(PENDING_ANALYTE, "Syphilis RPR");
        seedAnalyte(DRAFT_ANALYTE, "Hepatitis B sAg");
        seedAnalyte(BENCH_ANALYTE, "HIV p24 Antigen");
        seedEnrollment(ENROLLMENT, "CPHL HIV Programme");
        seedEnrollment(OTHER_ENROLLMENT, "Another Lab Enrollment");

        scheme = insertScheme("CPHL HIV Viral Load PT", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        // The section FK is set in SQL: the base test runs each service call in its
        // own transaction, so a section loaded through the ORM here is not managed.
        jdbc.update("UPDATE clinlims.eqa_program SET test_section_id = ? WHERE id = ?", SECTION_ID, scheme.getId());
        scheme = eqaProgramService.get(scheme.getId());

        cycle = readBack(insertCycle(scheme, 3));
        cycle.setCycleName("Q3 2026");
        cycle.setPlannedStartDate(Date.valueOf("2026-07-01"));
        cycle.setPlannedEndDate(Date.valueOf("2026-09-30"));
        cycle.setSysUserId(USER);
        eqaCycleDAO.update(cycle);
        round = eqaRoundDAO.get(insertRound(cycle, 1, "OPEN")).orElseThrow(AssertionError::new);

        scored(HIV_ANALYTE, "48000", "copies/mL", new BigDecimal("0.8"), EQAPerformanceStatus.ACCEPTABLE, ENROLLMENT);
        scored(CD4_ANALYTE, "410", "cells/uL", new BigDecimal("2.4"), EQAPerformanceStatus.QUESTIONABLE, ENROLLMENT);
        scored(TB_ANALYTE, "Scanty", null, new BigDecimal("-3.6"), EQAPerformanceStatus.UNACCEPTABLE, ENROLLMENT);
        insertParticipantResult(cycle, round, ENROLLMENT, PENDING_ANALYTE, EQASubmissionStatus.SUBMITTED, "Negative");
        scored(HIV_ANALYTE, "51000", "copies/mL", new BigDecimal("1.1"), EQAPerformanceStatus.ACCEPTABLE,
                OTHER_ENROLLMENT);
    }

    /**
     * Runs before the spine's own cleanup, so release the participant result's
     * reference to the analysis before dropping the row it points at.
     */
    /** Comment attachments and the library rows this suite created. */
    @After
    public void dropCommentSeed() {
        jdbc.update("DELETE FROM clinlims.note WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE LOWER(name) = 'eqa_cycle')");
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dict_entry IN (?, ?, ?)", COMMENT_ONE, COMMENT_TWO,
                UNRELATED_COMMENT);
        jdbc.update("DELETE FROM clinlims.dictionary_category WHERE name = ?", OTHER_CATEGORY);
    }

    @After
    public void dropAnalysisLinkedSeed() {
        // Panel-backed results outlive the spine's delete order, which drops
        // eqa_panel_sample before eqa_participant_result.
        jdbc.update("UPDATE clinlims.eqa_participant_result SET panel_sample_id = NULL");
        jdbc.update("UPDATE clinlims.eqa_participant_result SET analysis_id = NULL WHERE analysis_id = ?", ANALYSIS_ID);
        jdbc.update("DELETE FROM clinlims.analysis WHERE id = ?", ANALYSIS_ID);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", SAMPLE_ITEM_ID);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void reportCarriesTheCycleIdentifiersAndSchemeHeader() throws IOException {
        String text = reportText();

        assertTrue("the scheme name identifies the programme", text.contains("CPHL HIV Viral Load PT"));
        assertTrue("provider is printed for an external scheme", text.contains("NHLS"));
        assertTrue("the cycle number and name identify the round", text.contains("#3 — Q3 2026"));
        assertTrue("the planned period is printed", text.contains("2026-07-01 — 2026-09-30"));
        assertTrue("scheme type is printed in the reader's language", text.contains("International PT"));
    }

    @Test
    public void programmeSummaryCountsEveryStatusAndRatesOnlyScoredResults() throws IOException {
        // 5 rows, 4 scored (one SUBMITTED row is not scored); 2 acceptable,
        // 1 questionable, 1 unacceptable, so 2/4 = 50.0% of scored results.
        String programme = block(reportText(), PROGRAMME_SUMMARY, SECTION_SUMMARY);

        assertTrue("the programme table reads 5 results, 4 scored, 2/1/1 and 50%",
                rowIn(programme, "5", "4", "2", "1", "1", "50%"));
    }

    @Test
    public void sectionSummaryGroupsByTheSchemeSection() throws IOException {
        String sections = block(reportText(), SECTION_SUMMARY, SCORING_DETAIL);

        assertTrue("results with no analysis link fall back to the scheme's section", sections.contains(SECTION_NAME));
        assertTrue("the section summary tallies this section's four scored results",
                rowIn(sections, SECTION_NAME, "5", "4", "2", "1", "1", "50%"));
    }

    @Test
    public void scoringTableCarriesEveryAnalyteWithItsZScoreAndVerdict() throws IOException {
        String detail = block(reportText(), SCORING_DETAIL, SIGN_OFF);
        String analyst = systemUserService.get(String.valueOf(ADMIN_USER_ID)).getNameForDisplay();

        assertTrue("acceptable row names the analyst who ran it",
                rowIn(detail, "HIV Viral Load", "48000", "copies/mL", "0.8", "Acceptable", analyst));
        assertTrue("questionable row", rowIn(detail, "CD4 Count", "410", "cells/uL", "2.4", "Questionable"));
        assertTrue("unacceptable row keeps the sign of the z-score",
                rowIn(detail, "TB Smear Grade", "Scanty", "-3.6", "Unacceptable"));
        assertTrue("an unscored submission is shown, not dropped",
                rowIn(detail, "Syphilis RPR", "Negative", "Not scored"));
    }

    /**
     * A DRAFT row is the lab's unsubmitted working value. Printing it would put an
     * uncommitted number on a signed document, and counting it would inflate the
     * denominator behind every rate on the page.
     */
    @Test
    public void unsubmittedDraftResultsAreNeitherPrintedNorCounted() throws IOException {
        insertParticipantResult(cycle, round, ENROLLMENT, DRAFT_ANALYTE, EQASubmissionStatus.DRAFT, "9999");

        String text = reportText();

        assertFalse("the draft value is not printed", text.contains("9999"));
        assertFalse("the draft analyte gets no row", text.contains("Hepatitis B sAg"));
        assertTrue("the totals still count the five submitted results, not six",
                rowIn(block(text, PROGRAMME_SUMMARY, SECTION_SUMMARY), "5", "4", "2", "1", "1", "50%"));
    }

    /**
     * The same analyte is reported once per round. Without the round column the two
     * rows are indistinguishable on the page.
     */
    @Test
    public void everyRoundOfTheCycleIsPrintedAndKeptApartByItsRoundNumber() throws IOException {
        EQARound second = eqaRoundDAO.get(insertRound(cycle, 2, "OPEN")).orElseThrow(AssertionError::new);
        insertParticipantResult(cycle, second, ENROLLMENT, HIV_ANALYTE, EQASubmissionStatus.SUBMITTED, "52000");

        String detail = block(reportText(), SCORING_DETAIL, SIGN_OFF);

        assertTrue("round 1 carries its own HIV row", rowIn(detail, "1", "HIV Viral Load", "48000"));
        assertTrue("round 2 carries its own HIV row", rowIn(detail, "2", "HIV Viral Load", "52000"));
        assertEquals("both rounds' HIV rows are on the page", 3, occurrences(detail, "HIV Viral Load"));
    }

    /**
     * A result entered through standard result entry carries an analysis link, and
     * the bench that ran it — not the scheme's own section — is what belongs in the
     * section column.
     */
    @Test
    public void aResultLinkedToAnAnalysisReportsTheBenchThatRanIt() throws IOException {
        seedAnalysisLinkedResult();

        String text = reportText();
        String detail = block(text, SCORING_DETAIL, SIGN_OFF);

        assertTrue("the analysis's own section wins over the scheme fallback",
                rowIn(under(detail, BENCH_SECTION_NAME), "HIV p24 Antigen", "395"));
        assertTrue("the scheme's section still groups the results with no analysis link",
                rowIn(block(text, SECTION_SUMMARY, SCORING_DETAIL), SECTION_NAME, "5"));
        assertTrue("the bench gets its own section summary row",
                rowIn(block(text, SECTION_SUMMARY, SCORING_DETAIL), BENCH_SECTION_NAME, "1"));
    }

    @Test
    public void everyEnrollmentInTheCycleIsReported() throws IOException {
        String detail = block(reportText(), SCORING_DETAIL, SIGN_OFF);

        assertTrue("this enrollment's result", detail.contains("48000"));
        assertTrue("the other enrollment's result — the report is cycle-wide", detail.contains("51000"));
    }

    /**
     * The inversion this guards: a label that never reached the bundle renders as
     * its own key, and a raw enum constant renders as {@code INTERNATIONAL_PT}.
     * Neither belongs on a document a reviewer signs.
     */
    @Test
    public void reportPrintsTranslatedLabelsNotRawKeysOrEnumNames() throws IOException {
        String text = reportText();

        for (String label : List.of("External Quality Assessment - Performance Report", "Scheme", "Planned period",
                PROGRAMME_SUMMARY, SECTION_SUMMARY, SCORING_DETAIL, SIGN_OFF, "Acceptable %", "Z-score", "Round",
                "Scored on", "Not scored")) {
            assertTrue(label + " is printed as text, not as a key", text.contains(label));
        }
        assertFalse("no unresolved message key reaches the page", text.contains("eqa.report."));
        assertFalse("no unresolved enum key reaches the page", text.contains("eqa.performanceStatus."));
        assertFalse("the performance verdict is not a raw enum constant", text.contains("ACCEPTABLE"));
        assertFalse("the scheme type is not a raw enum constant", text.contains("INTERNATIONAL_PT"));
    }

    @Test
    public void aCycleWithNoPlannedDatesPrintsOneDashNotThree() throws IOException {
        // Re-read: the seed already wrote this row, so the field still holds the
        // version it had before that update.
        EQACycle undated = readBack(cycle.getId());
        undated.setPlannedStartDate(null);
        undated.setPlannedEndDate(null);
        undated.setSysUserId(USER);
        eqaCycleDAO.update(undated);

        String text = reportText();

        String periodLine = text.lines().filter(line -> line.contains("Planned period")).findFirst()
                .orElseThrow(() -> new AssertionError("the report prints no planned period at all"));
        assertFalse("three dashes in a row read as a rendering fault: " + periodLine, periodLine.contains("— — —"));
        assertTrue("the period label still prints with a single placeholder", rowIn(periodLine, "Planned period", "—"));
    }

    @Test
    public void everyPageCarriesItsNumberAndTheTotal() throws IOException {
        assertTrue("a filed report has to show whether a page is missing", reportText().contains("Page 1 of 1"));
    }

    @Test
    public void submittedAndScoredTimestampsArePrinted() throws IOException {
        EQAParticipantResult result = eqaParticipantResultDAO
                .getAllMatching(java.util.Map.of("cycle.id", cycle.getId(), "analyteId", CD4_ANALYTE)).get(0);
        result.setSubmittedAt(java.sql.Timestamp.valueOf("2026-09-02 08:15:00"));
        result.setScoreReceivedAt(java.sql.Timestamp.valueOf("2026-09-20 16:40:00"));
        result.setSysUserId(USER);
        eqaParticipantResultDAO.update(result);

        String detail = block(reportText(), SCORING_DETAIL, SIGN_OFF);

        assertTrue("the row carries the day it was submitted and the day it was scored",
                rowIn(detail, "CD4 Count", "2026-09-02", "2026-09-20"));
    }

    @Test
    public void anUnacceptableScorePrintsTheNonConformityItRaised() throws IOException {
        // Scored through the service, so the FR-V2.3-01 adapter raises the NCE the
        // way production does rather than the test inserting one.
        Long resultId = insertParticipantResult(cycle, round, ENROLLMENT, BENCH_ANALYTE, EQASubmissionStatus.SUBMITTED,
                "Reactive");
        participantResultService.recordScore(resultId, EQAPerformanceStatus.UNACCEPTABLE, null, USER);

        String nceNumber = jdbc.queryForObject(
                "SELECT nce_number FROM clinlims.nc_event WHERE trigger_source_type = 'EQA_UNACCEPTABLE'"
                        + " AND trigger_source_id = ?",
                String.class, String.valueOf(resultId));
        String detail = block(reportText(), SCORING_DETAIL, SIGN_OFF);

        assertTrue("the failing row names the investigation it is evidence for",
                rowIn(detail, "HIV p24 Antigen", "Unacceptable", nceNumber));
    }

    @Test
    public void aSealedPanelNeverPrintsItsTargetAndAnUnblindedOneDoes() throws IOException {
        Long sealedResult = resultAgainstPanel(EQAPanelStatus.SEALED, null, "9000", "8800");
        Long unblindedResult = resultAgainstPanel(EQAPanelStatus.SCORED,
                java.sql.Timestamp.valueOf("2026-09-21 09:00:00"), "120", "118");

        String detail = block(reportText(), SCORING_DETAIL, SIGN_OFF);

        assertFalse("a target still under seal must never reach a page served on the read permission",
                detail.contains("8800"));
        assertTrue("the unblinded panel's target is what the reviewer compares against", detail.contains("118"));
        assertTrue("and it sits on its own result's row", rowIn(detail, "120", "118"));
        assertTrue("both results are still listed", detail.contains("9000"));
        assertEquals("two panel-backed results were seeded", 2,
                (sealedResult == null ? 0 : 1) + (unblindedResult == null ? 0 : 1));
    }

    /**
     * A result scored against a blinded panel sample, so the report has to decide
     * whether the sealed target may be printed.
     */
    private Long resultAgainstPanel(EQAPanelStatus status, java.sql.Timestamp unblindedAt, String reported,
            String target) {
        EQAPanel panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Panel " + status);
            p.setStatus(status);
            p.setUnblindedAt(unblindedAt);
        });
        long analyteId = status == EQAPanelStatus.SEALED ? SEALED_ANALYTE : UNBLINDED_ANALYTE;
        seedAnalyte(analyteId, "Panel analyte " + analyteId);

        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode("SMP-" + analyteId);
        sample.setBlindCode("BLIND-" + analyteId);
        sample.setAnalyteId(analyteId);
        sample.setTargetValue(target);
        sample.setSysUserId(USER);
        Long sampleId = eqaPanelSampleDAO.insert(sample);

        Long resultId = insertParticipantResult(cycle, round, ENROLLMENT, analyteId, EQASubmissionStatus.SUBMITTED,
                reported);
        EQAParticipantResult result = eqaParticipantResultDAO.get(resultId).orElseThrow(AssertionError::new);
        result.setPanelSampleId(sampleId);
        result.setPerformanceStatus(EQAPerformanceStatus.ACCEPTABLE);
        result.setSubmissionStatus(EQASubmissionStatus.SCORED);
        result.setSysUserId(USER);
        eqaParticipantResultDAO.update(result);
        return resultId;
    }

    @Test
    public void anUnknownCycleIsRejected() {
        try {
            reportService.generatePerformanceReport(987654L);
            fail("expected an unknown cycle to be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("987654"));
        }
    }

    @Test
    public void theEndpointStreamsAPdfTheBrowserCanOpenInline() {
        ResponseEntity<byte[]> response = controller.performanceReport(cycle.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("inline; filename=\"eqa-performance-report-cycle-" + cycle.getId() + ".pdf\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertEquals("the body is a PDF, not an error page", "%PDF-",
                new String(response.getBody(), 0, 5, StandardCharsets.US_ASCII));
    }

    /** A path variable that names no cycle is a missing resource, not bad input. */
    @Test
    public void theEndpointAnswers404ForAnUnknownCycle() {
        ResponseEntity<byte[]> response = controller.performanceReport(987654L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- OGC-934 interpretive comments ----

    @Test
    public void attachedCommentsPrintUnderTheScoringTableWithTheirAttribution() throws IOException {
        seedCommentLibrary();
        String analyst = systemUserService.get(String.valueOf(ADMIN_USER_ID)).getNameForDisplay();

        List<Map<String, Object>> added = controller.attachReportComments(requestForAdmin(), cycle.getId(),
                Map.of("commentIds", List.of(libraryId(COMMENT_ONE), libraryId(COMMENT_TWO))));

        assertEquals("both selections are attached", 2, added.size());
        assertEquals(COMMENT_ONE, added.get(0).get("text"));
        assertEquals(COMMENT_TWO, added.get(1).get("text"));
        assertEquals("the comment is attributed to the user who attached it", analyst, added.get(0).get("attachedBy"));
        assertEquals("the library entry stays traceable", libraryId(COMMENT_ONE), added.get(0).get("libraryEntryId"));

        String comments = block(reportText(), COMMENTS_TITLE, SIGN_OFF);
        assertTrue("the first comment prints", comments.contains(COMMENT_ONE));
        assertTrue("the second comment prints", comments.contains(COMMENT_TWO));
        assertTrue("the reviewer's name prints beside the comment", rowIn(comments, COMMENT_ONE, analyst));
        assertTrue("comments print in the order they were attached",
                comments.indexOf(COMMENT_ONE) < comments.indexOf(COMMENT_TWO));
    }

    /**
     * The report carries no comment heading until a comment exists: an empty
     * section reads as commentary that went missing.
     */
    @Test
    public void aCycleWithNoCommentsPrintsNoCommentSection() throws IOException {
        seedCommentLibrary();

        String text = reportText();

        assertFalse("no heading without a comment", text.contains(COMMENTS_TITLE));
        assertEquals("nothing is attached", 0, reportCommentService.getComments(cycle.getId()).size());
    }

    /**
     * FR: the picker is the only way in. The endpoint takes ids, and an id outside
     * the pre-approved category is refused, so free text cannot reach the page.
     */
    @Test
    public void onlyIdsFromTheApprovedLibraryCanBeAttached() throws IOException {
        seedCommentLibrary();
        String foreignId = dictionaryIdByEntry(UNRELATED_COMMENT);

        try {
            controller.attachReportComments(requestForAdmin(), cycle.getId(), Map.of("commentIds", List.of(foreignId)));
            fail("an entry from another category is not pre-approved wording");
        } catch (IllegalArgumentException expected) {
            assertTrue("the message names the library", expected.getMessage().contains("EQA Report Comment"));
        }

        assertEquals("nothing was attached", 0, reportCommentService.getComments(cycle.getId()).size());
        assertFalse("the foreign wording never reaches the page", reportText().contains(UNRELATED_COMMENT));
    }

    /**
     * Deactivating a library entry stops new use without rewriting a report that
     * already printed it — the reason the attachment stores the wording rather than
     * a live reference to the dictionary row.
     */
    @Test
    public void retiringALibraryEntryLeavesAlreadyAttachedTextIntact() throws IOException {
        seedCommentLibrary();
        String entryId = libraryId(COMMENT_ONE);
        controller.attachReportComments(requestForAdmin(), cycle.getId(), Map.of("commentIds", List.of(entryId)));

        jdbc.update("UPDATE clinlims.dictionary SET is_active = 'N' WHERE id = ?", Long.valueOf(entryId));

        assertFalse("the retired entry is no longer offered",
                controller.reportCommentLibrary().stream().anyMatch(row -> entryId.equals(row.get("id"))));
        assertTrue("the wording already attached still prints",
                block(reportText(), COMMENTS_TITLE, SIGN_OFF).contains(COMMENT_ONE));
        try {
            controller.attachReportComments(requestForAdmin(), cycle.getId(), Map.of("commentIds", List.of(entryId)));
            fail("a retired entry cannot be attached again");
        } catch (IllegalArgumentException expected) {
            assertTrue("the message names the entry", expected.getMessage().contains(entryId));
        }
    }

    @Test
    public void reattachingAnAttachedCommentDoesNotPrintItTwice() throws IOException {
        seedCommentLibrary();
        String entryId = libraryId(COMMENT_ONE);
        controller.attachReportComments(requestForAdmin(), cycle.getId(), Map.of("commentIds", List.of(entryId)));

        List<Map<String, Object>> second = controller.attachReportComments(requestForAdmin(), cycle.getId(),
                Map.of("commentIds", List.of(entryId)));

        assertEquals("the resent id adds nothing", 0, second.size());
        assertEquals("one attachment is stored", 1, reportCommentService.getComments(cycle.getId()).size());
        assertEquals("the sentence prints once", 1, occurrences(reportText(), COMMENT_ONE));
    }

    @Test
    public void detachedCommentsLeaveThePrintedReport() throws IOException {
        seedCommentLibrary();
        List<Map<String, Object>> added = controller.attachReportComments(requestForAdmin(), cycle.getId(),
                Map.of("commentIds", List.of(libraryId(COMMENT_ONE), libraryId(COMMENT_TWO))));
        String firstCommentId = String.valueOf(added.get(0).get("id"));

        controller.detachReportComment(cycle.getId(), firstCommentId);

        String text = reportText();
        assertFalse("the removed comment is gone from the page", text.contains(COMMENT_ONE));
        assertTrue("the comment left in place still prints", text.contains(COMMENT_TWO));
        assertEquals("one attachment remains", 1, reportCommentService.getComments(cycle.getId()).size());
        try {
            controller.detachReportComment(cycle.getId(), firstCommentId);
            fail("a comment that is not attached cannot be detached");
        } catch (IllegalArgumentException expected) {
            assertTrue("the message names the cycle", expected.getMessage().contains(String.valueOf(cycle.getId())));
        }
    }

    // ---- helpers ----
    /**
     * The library the service reads, ensured rather than assumed: other suites
     * truncate dictionary, and a full-suite run must not depend on whether
     * liquibase qa/032's seed survived. Rows are matched on wording, so this is
     * idempotent whether or not the shipped category is already present.
     */
    private void seedCommentLibrary() {
        jdbc.update("INSERT INTO clinlims.reference_tables (id, name, keep_history, lastupdated)"
                + " SELECT nextval('clinlims.reference_tables_seq'), 'eqa_cycle', 'Y', now() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.reference_tables WHERE LOWER(name) = 'eqa_cycle')");
        ensureCategory(EQAReportCommentService.CATEGORY_NAME, "EQARC");
        ensureCategory(OTHER_CATEGORY, "EQARCF");
        ensureEntry(EQAReportCommentService.CATEGORY_NAME, COMMENT_ONE, 910);
        ensureEntry(EQAReportCommentService.CATEGORY_NAME, COMMENT_TWO, 920);
        ensureEntry(OTHER_CATEGORY, UNRELATED_COMMENT, 930);
    }

    private void ensureCategory(String name, String abbreviation) {
        jdbc.update(
                "INSERT INTO clinlims.dictionary_category (id, name, description, local_abbrev, lastupdated)"
                        + " SELECT nextval('clinlims.dictionary_category_seq'), ?, ?, ?, now() WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.dictionary_category WHERE name = ?)",
                name, name, abbreviation, name);
    }

    private void ensureEntry(String category, String text, int sortOrder) {
        jdbc.update(
                "INSERT INTO clinlims.dictionary (id, dictionary_category_id, dict_entry, is_active,"
                        + " sort_order, lastupdated) SELECT nextval('clinlims.dictionary_seq'),"
                        + " (SELECT id FROM clinlims.dictionary_category WHERE name = ?), ?, 'Y', ?, now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.dictionary WHERE dict_entry = ?)",
                category, text, sortOrder, text);
        jdbc.update("UPDATE clinlims.dictionary SET is_active = 'Y' WHERE dict_entry = ?", text);
    }

    /** The id the picker would send for this wording. */
    private String libraryId(String text) {
        for (Map<String, Object> entry : controller.reportCommentLibrary()) {
            if (text.equals(entry.get("text"))) {
                return String.valueOf(entry.get("id"));
            }
        }
        throw new AssertionError("the library does not offer: " + text);
    }

    private String dictionaryIdByEntry(String text) {
        return jdbc.queryForObject("SELECT id::text FROM clinlims.dictionary WHERE dict_entry = ?", String.class, text);
    }

    private MockHttpServletRequest requestForAdmin() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId((int) ADMIN_USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    private void scored(long analyteId, String value, String unit, BigDecimal zScore, EQAPerformanceStatus performance,
            long enrollmentId) {
        Long id = insertParticipantResult(cycle, round, enrollmentId, analyteId, EQASubmissionStatus.SUBMITTED, value);
        EQAParticipantResult result = eqaParticipantResultDAO.get(id).orElseThrow(AssertionError::new);
        result.setResultUnit(unit);
        result.setZScore(zScore);
        result.setPerformanceStatus(performance);
        result.setSubmissionStatus(EQASubmissionStatus.SCORED);
        result.setAssignedAnalystId(ADMIN_USER_ID);
        result.setSysUserId(USER);
        eqaParticipantResultDAO.update(result);
    }

    /**
     * A participant result reached through standard result entry: an analysis on a
     * test that belongs to a different section than the scheme's.
     */
    private void seedAnalysisLinkedResult() {
        jdbc.update(
                "INSERT INTO clinlims.localization (id, description) SELECT ?, ? WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.localization WHERE id = ?)",
                BENCH_SECTION_ID, BENCH_SECTION_NAME, BENCH_SECTION_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                        + " name_localization_id) SELECT ?, ?, ?, 'N', ?, ? WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.test_section WHERE id = ?)",
                BENCH_SECTION_ID, BENCH_SECTION_NAME, BENCH_SECTION_NAME, BENCH_SECTION_ID, BENCH_SECTION_ID,
                BENCH_SECTION_ID);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, test_section_id, guid, lastupdated)"
                        + " SELECT ?, 'EQA Report CD4', 'EQA Report CD4', 'Y', ?, ?, now() WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST_ID, BENCH_SECTION_ID, UUID.randomUUID().toString(), TEST_ID);
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date,"
                + " is_confirmation, lastupdated) SELECT ?, ?, now(), now(), false, now() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.sample WHERE id = ?)", SAMPLE_ID, "EQARPT" + SAMPLE_ID, SAMPLE_ID);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " SELECT ?, ?, 1, ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.sample_item WHERE id = ?)",
                SAMPLE_ITEM_ID, SAMPLE_ID, anyAnalysisStatusId(), SAMPLE_ITEM_ID);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, status_id, lastupdated)"
                        + " SELECT ?, 'MANUAL', ?, ?, ?, now() WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.analysis WHERE id = ?)",
                ANALYSIS_ID, TEST_ID, SAMPLE_ITEM_ID, anyAnalysisStatusId(), ANALYSIS_ID);

        Long id = insertParticipantResult(cycle, round, ENROLLMENT, BENCH_ANALYTE, EQASubmissionStatus.SUBMITTED,
                "395");
        EQAParticipantResult result = eqaParticipantResultDAO.get(id).orElseThrow(AssertionError::new);
        result.setAnalysisId(ANALYSIS_ID);
        result.setSysUserId(USER);
        eqaParticipantResultDAO.update(result);
    }

    /**
     * Other suites truncate status_of_sample, so take whatever ANALYSIS status the
     * database holds and restore one when it holds none.
     */
    private long anyAnalysisStatusId() {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM clinlims.status_of_sample WHERE status_type = 'ANALYSIS' LIMIT 1", Long.class);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " VALUES (9604, 1, 'ANALYSIS', 'Not Tested', 'restored by EQAPerformanceReportIntegrationTest')");
        return 9604L;
    }

    private void seedAnalyte(long id, String name) {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated)"
                + " SELECT ?, ?, 'Y', now() WHERE NOT EXISTS" + " (SELECT 1 FROM clinlims.analyte WHERE id = ?)", id,
                name, id);
    }

    private String reportText() throws IOException {
        byte[] pdf = reportService.generatePerformanceReport(cycle.getId());
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(reader, page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * The slice of the page under one heading. Every table on this report is a run
     * of digits, so an unsliced search lets the section summary satisfy an
     * assertion written for the programme summary.
     */
    private String block(String text, String fromHeading, String toHeading) {
        int from = text.indexOf(fromHeading);
        assertTrue(fromHeading + " is on the page", from >= 0);
        int to = text.indexOf(toHeading, from + fromHeading.length());
        assertTrue(toHeading + " follows " + fromHeading, to > from);
        return text.substring(from, to);
    }

    /** True when one line of the extracted text holds every token, in order. */
    /** The detail rows printed under a section's banner. */
    private String under(String detail, String sectionName) {
        int at = detail.indexOf(sectionName);
        return at < 0 ? "" : detail.substring(at + sectionName.length());
    }

    private boolean rowIn(String text, String... tokens) {
        for (String line : text.split("\n")) {
            int cursor = 0;
            boolean matched = true;
            for (String token : tokens) {
                int found = line.indexOf(token, cursor);
                if (found < 0) {
                    matched = false;
                    break;
                }
                cursor = found + token.length();
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private int occurrences(String text, String token) {
        int count = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            count++;
        }
        return count;
    }
}
