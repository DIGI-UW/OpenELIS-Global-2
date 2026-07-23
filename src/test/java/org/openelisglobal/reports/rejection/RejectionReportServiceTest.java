package org.openelisglobal.reports.rejection;

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
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the Rejection Rate compute (OGC-710, C.3 gap #4).
 * Fixture testdata/result.xml provides two analyses started 2025-07-07. A
 * rejection is simulated exactly as production does it: a REJECTION_REASON
 * ('R') note on the analysis whose text is the rejection-reason dictionary
 * value (LogbookResultsController / ResultUtil).
 */
public class RejectionReportServiceTest extends BaseWebContextSensitiveTest {

    private static final LocalDate WINDOW_FROM = LocalDate.of(2025, 7, 1);
    private static final LocalDate STARTS_ONLY_TO = LocalDate.of(2025, 7, 31);

    @Autowired
    private RejectionReportService rejectionReportService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private NoteService noteService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
    }

    private void writeRejectionNote(String analysisId, String reason) {
        Analysis analysis = analysisService.get(analysisId);
        Note note = noteService.createSavableNote(analysis, NoteType.REJECTION_REASON, reason, "Rejection Reason", "1");
        noteService.insert(note);
    }

    @Test
    public void summaryAndDetail_countRejectionWithReasonAndUser() {
        writeRejectionNote("2", "Hemolyzed specimen");

        RejectionSummaryResponse summary = rejectionReportService.getSummary(WINDOW_FROM, LocalDate.now());
        assertEquals(1, summary.getRejectedCount());
        assertEquals(2, summary.getTotalCount());
        assertEquals(50.0, summary.getRatePercent(), 0.001);

        RejectionDetailResponse detail = rejectionReportService.getDetail(WINDOW_FROM, LocalDate.now(), 0, 25);
        assertEquals(1, detail.getTotalCount());
        RejectionDetailResponse.RejectionEvent event = detail.getItems().get(0);
        assertEquals("2", event.getAnalysisId());
        assertEquals("13333", event.getLabNumber());
        assertEquals("Urinalysis", event.getTestName());
        assertEquals("Hemolyzed specimen", event.getReason());
        assertEquals("John Doe", event.getRejectedBy());
        assertNotNull(event.getRejectedAt());
    }

    @Test
    public void nonRejectionNotes_areNotCounted() {
        Analysis analysis = analysisService.get("2");
        Note note = noteService.createSavableNote(analysis, NoteType.EXTERNAL, "Result corrected", "Result Note", "1");
        noteService.insert(note);

        RejectionSummaryResponse summary = rejectionReportService.getSummary(WINDOW_FROM, LocalDate.now());
        assertEquals(0, summary.getRejectedCount());
        assertEquals(2, summary.getTotalCount());
        assertEquals(0.0, summary.getRatePercent(), 0.001);
    }

    @Test
    public void noteOutsideWindow_isNotCounted_andDenominatorTracksStarts() {
        writeRejectionNote("2", "Hemolyzed specimen");

        // Window covers the started analyses but ends before the note was written
        RejectionSummaryResponse summary = rejectionReportService.getSummary(WINDOW_FROM, STARTS_ONLY_TO);
        assertEquals(0, summary.getRejectedCount());
        assertEquals(2, summary.getTotalCount());
        assertEquals(0.0, summary.getRatePercent(), 0.001);
    }

    @Test
    public void emptyWindow_hasNullRate() {
        RejectionSummaryResponse summary = rejectionReportService.getSummary(LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31));
        assertEquals(0, summary.getRejectedCount());
        assertEquals(0, summary.getTotalCount());
        assertNull(summary.getRatePercent());
    }

    private static RejectionTrendResponse.TrendPoint pointFor(RejectionTrendResponse trend, String period) {
        return trend.getPoints().stream().filter(p -> period.equals(p.getPeriod())).findFirst().orElse(null);
    }

    @Test
    public void trend_bucketsRejectionsAndStartsByPeriod() {
        // Rejection note lands "now", starts are 2025-07-07 — two buckets
        writeRejectionNote("2", "Hemolyzed specimen");

        RejectionTrendResponse trend = rejectionReportService.getTrend(WINDOW_FROM, LocalDate.now(), "MONTHLY");

        RejectionTrendResponse.TrendPoint startMonth = pointFor(trend, "2025-07");
        assertNotNull(startMonth);
        assertEquals(0, startMonth.getRejectedCount());
        assertEquals(2, startMonth.getTotalCount());
        assertEquals(0.0, startMonth.getRatePercent(), 0.001);

        LocalDate now = LocalDate.now();
        RejectionTrendResponse.TrendPoint noteMonth = pointFor(trend,
                now.getYear() + "-" + String.format("%02d", now.getMonthValue()));
        assertNotNull("rejection must appear in a bucket even with no starts there", noteMonth);
        assertEquals(1, noteMonth.getRejectedCount());
        assertEquals(0, noteMonth.getTotalCount());
        assertNull(noteMonth.getRatePercent());
    }

    @Test
    public void breakdown_reasonParetoIsCumulativeAndTestsGroupWithRates() {
        writeRejectionNote("1", "Hemolyzed specimen");
        writeRejectionNote("2", "Hemolyzed specimen");
        writeRejectionNote("2", "Insufficient volume");

        RejectionBreakdownResponse breakdown = rejectionReportService.getBreakdown(WINDOW_FROM, LocalDate.now());

        assertEquals(2, breakdown.getReasons().size());
        RejectionBreakdownResponse.ReasonRow top = breakdown.getReasons().get(0);
        assertEquals("Hemolyzed specimen", top.getReason());
        assertEquals(2, top.getCount());
        assertEquals(66.67, top.getPercentOfRejections(), 0.001);
        assertEquals(66.67, top.getCumulativePercent(), 0.001);
        RejectionBreakdownResponse.ReasonRow second = breakdown.getReasons().get(1);
        assertEquals("Insufficient volume", second.getReason());
        assertEquals(1, second.getCount());
        assertEquals(33.33, second.getPercentOfRejections(), 0.001);
        assertEquals(100.0, second.getCumulativePercent(), 0.001);

        // Both tests rejected once each of one started. Assert by the test whose
        // fixture and catalog names agree ("Urinalysis") — the harness seed keeps
        // its own name for fixture test 1, same caveat as AmendmentReportServiceTest.
        assertEquals(2, breakdown.getTests().size());
        RejectionBreakdownResponse.TestRow urinalysis = breakdown.getTests().stream()
                .filter(t -> "Urinalysis".equals(t.getTestName())).findFirst().orElse(null);
        assertNotNull(urinalysis);
        assertEquals(1, urinalysis.getRejectedCount());
        assertEquals(1, urinalysis.getTotalCount());
        assertEquals(100.0, urinalysis.getRatePercent(), 0.001);
    }

    @Test
    public void breakdown_emptyWindow_returnsNoRows() {
        RejectionBreakdownResponse breakdown = rejectionReportService.getBreakdown(LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31));
        assertTrue(breakdown.getReasons().isEmpty());
        assertTrue(breakdown.getTests().isEmpty());
    }
}
