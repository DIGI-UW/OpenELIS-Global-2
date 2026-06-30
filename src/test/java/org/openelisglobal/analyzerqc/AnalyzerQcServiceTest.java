package org.openelisglobal.analyzerqc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.QcFrequencyType;
import org.openelisglobal.analyzer.valueholder.QcStatus;
import org.openelisglobal.analyzerqc.dao.AnalyzerQcRunDAO;
import org.openelisglobal.analyzerqc.form.QcRunForm;
import org.openelisglobal.analyzerqc.service.AnalyzerQcServiceImpl;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcRun;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcStatus;

/**
 * Unit tests for AnalyzerQcServiceImpl.
 * Mocks all dependencies — no DB or Spring context needed.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerQcServiceTest {

    @Mock AnalyzerQcRunDAO analyzerQcRunDAO;
    @Mock AnalyzerService analyzerService;
    @InjectMocks AnalyzerQcServiceImpl service;

    private Analyzer analyzer;
    private static final String ANALYZER_ID = "42";

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId(ANALYZER_ID);
        when(analyzerService.get(ANALYZER_ID)).thenReturn(analyzer);
    }

    // ── BR-AQC-004: qcRequired=false → always PASS ───────────────────────────

    @Test
    public void getQcStatus_qcNotRequired_alwaysPass_evenWithNoRuns() {
        analyzer.setQcRequired(false);
        when(analyzerQcRunDAO.getLastPassForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.empty());
        when(analyzerQcRunDAO.getLastRunForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.empty());

        AnalyzerQcStatus result = service.getQcStatus(ANALYZER_ID);

        assertEquals(QcStatus.PASS, result.getStatus());
        assertFalse(result.isQcRequired());
    }

    // ── No runs at all → NOT_RUN ──────────────────────────────────────────────

    @Test
    public void getQcStatus_qcRequired_noRuns_returnsNotRun() {
        analyzer.setQcRequired(true);
        analyzer.setQcFrequencyType(QcFrequencyType.DAILY);
        when(analyzerQcRunDAO.getLastPassForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.empty());
        when(analyzerQcRunDAO.getLastRunForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.empty());

        assertEquals(QcStatus.NOT_RUN, service.getQcStatus(ANALYZER_ID).getStatus());
    }

    // ── DAILY: pass today → PASS ─────────────────────────────────────────────

    @Test
    public void getQcStatus_daily_passToday_returnsPass() {
        analyzer.setQcRequired(true);
        analyzer.setQcFrequencyType(QcFrequencyType.DAILY);

        Instant todayNine = LocalDate.now(ZoneId.systemDefault())
                .atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant();
        AnalyzerQcRun run = runWithDate(Timestamp.from(todayNine));

        when(analyzerQcRunDAO.getLastPassForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));
        when(analyzerQcRunDAO.getLastRunForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));

        assertEquals(QcStatus.PASS, service.getQcStatus(ANALYZER_ID).getStatus());
    }

    // ── DAILY: pass yesterday → OVERDUE ──────────────────────────────────────

    @Test
    public void getQcStatus_daily_passYesterday_returnsOverdue() {
        analyzer.setQcRequired(true);
        analyzer.setQcFrequencyType(QcFrequencyType.DAILY);

        AnalyzerQcRun run = runWithDate(
                Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        when(analyzerQcRunDAO.getLastPassForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));
        when(analyzerQcRunDAO.getLastRunForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));

        assertEquals(QcStatus.OVERDUE, service.getQcStatus(ANALYZER_ID).getStatus());
    }

    // ── PER_SHIFT: 5 h ago, shift=8 h → PASS ─────────────────────────────────

    @Test
    public void getQcStatus_perShift_withinWindow_returnsPass() {
        analyzer.setQcRequired(true);
        analyzer.setQcFrequencyType(QcFrequencyType.PER_SHIFT);
        analyzer.setQcFrequencyHours(8);

        AnalyzerQcRun run = runWithDate(
                Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)));
        when(analyzerQcRunDAO.getLastPassForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));
        when(analyzerQcRunDAO.getLastRunForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));

        assertEquals(QcStatus.PASS, service.getQcStatus(ANALYZER_ID).getStatus());
    }

    // ── PER_SHIFT: 10 h ago, shift=8 h → OVERDUE ─────────────────────────────

    @Test
    public void getQcStatus_perShift_pastWindow_returnsOverdue() {
        analyzer.setQcRequired(true);
        analyzer.setQcFrequencyType(QcFrequencyType.PER_SHIFT);
        analyzer.setQcFrequencyHours(8);

        AnalyzerQcRun run = runWithDate(
                Timestamp.from(Instant.now().minus(10, ChronoUnit.HOURS)));
        when(analyzerQcRunDAO.getLastPassForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));
        when(analyzerQcRunDAO.getLastRunForAnalyzer(ANALYZER_ID))
                .thenReturn(Optional.of(run));

        assertEquals(QcStatus.OVERDUE, service.getQcStatus(ANALYZER_ID).getStatus());
    }

    // ── BR-AQC-006: future run date rejected ─────────────────────────────────

    @Test
    public void recordQcRun_futureDate_throwsIllegalArgument() {
        QcRunForm form = new QcRunForm();
        form.setResult("PASS");
        form.setSource("ANALYZER_LIST");
        form.setRunDate(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        assertThrows(IllegalArgumentException.class,
                () -> service.recordQcRun(ANALYZER_ID, form, "user1"));
    }

    // ── recordQcRun: valid run → saved ───────────────────────────────────────

    @Test
    public void recordQcRun_validForm_callsSave() {
        QcRunForm form = new QcRunForm();
        form.setResult("PASS");
        form.setSource("ANALYZER_IMPORT");

        service.recordQcRun(ANALYZER_ID, form, "user1");
        verify(analyzerQcRunDAO, times(1)).insert(any(AnalyzerQcRun.class));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private AnalyzerQcRun runWithDate(Timestamp ts) {
        AnalyzerQcRun run = new AnalyzerQcRun();
        run.setResult("PASS");
        run.setRunDate(ts);
        run.setSource("ANALYZER_LIST");
        return run;
    }
}