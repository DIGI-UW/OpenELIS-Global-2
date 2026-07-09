package org.openelisglobal.reports.amendment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentEvent;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.service.AmendmentReportService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration test for the Amendment Rate compute (OGC-698). Fixture
 * testdata/result.xml provides two released analyses (released 2025-07-07) with
 * one result each. An amendment is simulated exactly as production does it:
 * update the result value (real audit trail -> 'U' history row with the prior
 * value) and write the corrected-result EXTERNAL note.
 */
public class AmendmentReportServiceTest extends BaseWebContextSensitiveTest {

    private static final LocalDate WINDOW_FROM = LocalDate.of(2025, 7, 1);
    private static final LocalDate RELEASE_ONLY_TO = LocalDate.of(2025, 7, 31);

    @Autowired
    private AmendmentReportService amendmentReportService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Before
    public void setUp() throws Exception {
        // Replace the mocked AuditTrailService with a real one so result
        // updates write genuine history rows (same wiring as
        // SystemAuditTrailIntegrationTest)
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);
        Object target = AopTestUtils.getUltimateTargetObject(resultService);
        ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrailService);

        executeDataSetWithStateManagement("testdata/result.xml");
    }

    private void amendResult(String resultId, String newValue) {
        Result result = resultService.get(resultId);
        result.setValue(newValue);
        result.setSysUserId("1");
        resultService.update(result);
    }

    private void writeCorrectedNote(String analysisId) {
        Analysis analysis = analysisService.get(analysisId);
        Note note = noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                MessageUtil.getMessage("note.corrected.result"), "Result Note", "1");
        noteService.insert(note);
    }

    @Test
    public void summaryAndDetail_countCorrectedResultWithPriorAndNewValues() {
        // Analysis 2 / result 4: fixture test 2 ("Urinalysis") has matching
        // physical and localized names, so the join assertion is unambiguous
        // (fixture test 1 localizes its name to "GPT/ALAT").
        amendResult("4", "15.9");
        writeCorrectedNote("2");

        AmendmentSummaryResponse summary = amendmentReportService.getSummary(WINDOW_FROM, LocalDate.now());
        assertEquals(1, summary.getAmendedCount());
        assertEquals(2, summary.getReleasedCount());
        assertEquals(50.0, summary.getRatePercent(), 0.001);

        AmendmentDetailResponse detail = amendmentReportService.getDetail(WINDOW_FROM, LocalDate.now(), 0, 25);
        assertEquals(1, detail.getTotalCount());
        assertEquals(1, detail.getItems().size());

        AmendmentEvent event = detail.getItems().get(0);
        assertEquals("2", event.getAnalysisId());
        assertEquals("13333", event.getLabNumber());
        assertEquals("Urinalysis", event.getTestName());
        assertEquals("14.5", event.getPriorValue());
        assertEquals("15.9", event.getCurrentValue());
        assertEquals("John Doe", event.getAmendedBy());
        assertNotNull(event.getAmendedAt());
        assertNotNull(event.getReleasedAt());
        // released 2025-07-07 14:00, amended now -> positive and > 1 day
        assertNotNull(event.getMinutesToAmend());
        assertTrue("time to amend should be positive", event.getMinutesToAmend() > 24 * 60);
    }

    @Test
    public void resultUpdateWithoutCorrectedNote_isNotCounted() {
        // Pre-report edit: value changes (history 'U' row exists) but the
        // correction flow never wrote the corrected-result note
        amendResult("3", "92.0");

        AmendmentSummaryResponse summary = amendmentReportService.getSummary(WINDOW_FROM, LocalDate.now());
        assertEquals(0, summary.getAmendedCount());
        assertEquals(2, summary.getReleasedCount());
        assertEquals(0.0, summary.getRatePercent(), 0.001);
        assertEquals(0, amendmentReportService.getDetail(WINDOW_FROM, LocalDate.now(), 0, 25).getTotalCount());
    }

    @Test
    public void noteOutsideWindow_isNotCounted_andDenominatorTracksReleases() {
        amendResult("4", "15.9");
        writeCorrectedNote("2");

        // Window covers the releases but ends before the note was written
        AmendmentSummaryResponse summary = amendmentReportService.getSummary(WINDOW_FROM, RELEASE_ONLY_TO);
        assertEquals(0, summary.getAmendedCount());
        assertEquals(2, summary.getReleasedCount());
        assertEquals(0.0, summary.getRatePercent(), 0.001);
    }

    @Test
    public void emptyWindow_hasNullRate() {
        AmendmentSummaryResponse summary = amendmentReportService.getSummary(LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31));
        assertEquals(0, summary.getAmendedCount());
        assertEquals(0, summary.getReleasedCount());
        assertNull(summary.getRatePercent());
    }
}
