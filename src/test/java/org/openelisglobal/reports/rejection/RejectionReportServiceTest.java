package org.openelisglobal.reports.rejection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionHeatmapResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the Rejection Rate compute (OGC-710). Fixture
 * testdata/result.xml provides two analyses started 2025-07-07. A rejection is
 * simulated exactly as production does it: a REJECTION_REASON ('R') note on the
 * analysis whose text is the rejection-reason dictionary value
 * (LogbookResultsController / ResultUtil).
 */
public class RejectionReportServiceTest extends BaseWebContextSensitiveTest {

    private static final LocalDate WINDOW_FROM = LocalDate.of(2025, 7, 1);
    private static final LocalDate START_MONTH_TO = LocalDate.of(2025, 7, 31);

    @Autowired
    private RejectionReportService rejectionReportService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SampleRequesterService sampleRequesterService;

    @Autowired
    private RequesterTypeService requesterTypeService;

    @PersistenceContext
    private EntityManager entityManager;

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
    public void windowSelectsByAnalysisStart_noteDateIsIrrelevant() {
        writeRejectionNote("2", "Hemolyzed specimen");

        // Window covers the starts (2025-07-07) but ends before the note was
        // written (now): the rejection still counts — cohort semantics key on
        // when the analysis started, so rejected ⊆ started and rates can never
        // exceed 100%.
        RejectionSummaryResponse summary = rejectionReportService.getSummary(WINDOW_FROM, START_MONTH_TO);
        assertEquals(1, summary.getRejectedCount());
        assertEquals(2, summary.getTotalCount());
        assertEquals(50.0, summary.getRatePercent(), 0.001);

        // The reverse direction: a window covering the note date but not the
        // start date sees nothing — the analysis isn't in the cohort.
        LocalDate now = LocalDate.now();
        RejectionSummaryResponse noteWindow = rejectionReportService.getSummary(now.minusDays(3), now);
        assertEquals(0, noteWindow.getRejectedCount());
        assertEquals(0, noteWindow.getTotalCount());
        assertNull(noteWindow.getRatePercent());

        // Detail follows the same window: the row appears in the start window
        // (with its out-of-window rejection timestamp), not the note window.
        assertEquals(1, rejectionReportService.getDetail(WINDOW_FROM, START_MONTH_TO, 0, 25).getTotalCount());
        assertEquals(0, rejectionReportService.getDetail(now.minusDays(3), now, 0, 25).getTotalCount());
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
    public void trend_bucketsByAnalysisStart_rejectedNeverExceedsStarted() {
        // Rejection note lands "now", starts are 2025-07-07 — under cohort
        // semantics both counts land in the single start-month bucket.
        writeRejectionNote("2", "Hemolyzed specimen");

        RejectionTrendResponse trend = rejectionReportService.getTrend(WINDOW_FROM, LocalDate.now(), "MONTHLY");

        RejectionTrendResponse.TrendPoint startMonth = pointFor(trend, "2025-07");
        assertNotNull(startMonth);
        assertEquals(1, startMonth.getRejectedCount());
        assertEquals(2, startMonth.getTotalCount());
        assertEquals(50.0, startMonth.getRatePercent(), 0.001);

        // No phantom bucket in the note's month, and no bucket anywhere can
        // have more rejections than starts (the old note-dated numerator
        // rendered a 200% point).
        LocalDate now = LocalDate.now();
        assertNull(pointFor(trend, now.getYear() + "-" + String.format("%02d", now.getMonthValue())));
        trend.getPoints().forEach(p -> assertTrue("rejected must be ⊆ started in every bucket",
                p.getRejectedCount() <= p.getTotalCount()));
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
    public void heatmap_groupsByRequesterOrgAndSection_nullLocationWhenNoRequester() {
        writeRejectionNote("2", "Hemolyzed specimen");

        // No requesting site captured: the fixture's rejection sits in the
        // null-location bucket (the UI labels it, not the backend). Asserted on
        // that bucket only — the heatmap is a window-wide query, so cells from
        // other suites' leftovers can legitimately coexist in the window and an
        // allMatch over them is order-fragile.
        RejectionHeatmapResponse before = rejectionReportService.getHeatmap(WINDOW_FROM, START_MONTH_TO);
        long beforeNullRejected = before.getCells().stream().filter(cell -> cell.getLocation() == null)
                .mapToLong(RejectionHeatmapResponse.Cell::getRejectedCount).sum();
        assertTrue("analyses with no requester must land in the null-location bucket", beforeNullRejected >= 1);

        // Fixture rows carry explicit ids the sequence doesn't know about —
        // advance it past them so the insert doesn't collide.
        entityManager.createNativeQuery("SELECT setval('organization_seq',"
                + " COALESCE((SELECT CAST(MAX(id) AS bigint) FROM organization), 1) + 100)").getSingleResult();

        // Attach an ordering organization to the rejected analysis's sample
        // (fixture: analysis 2 -> sample_item 602 -> sample 2).
        Organization org = new Organization();
        org.setOrganizationName("Inpatient Ward");
        org.setIsActive("Y");
        org.setMlsSentinelLabFlag("N");
        org.setSysUserId("1");
        String orgId = organizationService.insert(org);

        SampleRequester requester = new SampleRequester();
        requester.setSampleId(2L);
        requester.setRequesterId(Long.parseLong(orgId));
        requester.setRequesterTypeId(
                Long.parseLong(requesterTypeService.getRequesterTypeByName("organization").getId()));
        requester.setSysUserId("1");
        sampleRequesterService.insert(requester);

        RejectionHeatmapResponse after = rejectionReportService.getHeatmap(WINDOW_FROM, START_MONTH_TO);
        RejectionHeatmapResponse.Cell cell = after.getCells().stream()
                .filter(c -> "Inpatient Ward".equals(c.getLocation())).findFirst().orElse(null);
        assertNotNull(cell);
        assertEquals(1, cell.getRejectedCount());
        assertEquals(1, cell.getTotalCount());
        assertEquals(100.0, cell.getRatePercent(), 0.001);

        // The fixture's rejection moved from the null-location bucket to the
        // Inpatient Ward cell — exactly one fewer null-located rejection than
        // before. Relative rather than absolute, for the same window-wide reason
        // as above.
        long afterNullRejected = after.getCells().stream().filter(c -> c.getLocation() == null)
                .mapToLong(RejectionHeatmapResponse.Cell::getRejectedCount).sum();
        assertEquals(beforeNullRejected - 1, afterNullRejected);

        // The same join labels the detail rows.
        RejectionDetailResponse detail = rejectionReportService.getDetail(WINDOW_FROM, START_MONTH_TO, 0, 25);
        assertTrue("the requester's organization labels its detail row",
                detail.getItems().stream().anyMatch(i -> "Inpatient Ward".equals(i.getLocation())));
    }

    @Test
    public void breakdown_emptyWindow_returnsNoRows() {
        RejectionBreakdownResponse breakdown = rejectionReportService.getBreakdown(LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31));
        assertTrue(breakdown.getReasons().isEmpty());
        assertTrue(breakdown.getTests().isEmpty());
    }
}
