package org.openelisglobal.dashboard.management.kpi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * testdata/result.xml: two samples received 2024-06-03 and 2024-06-04, whose
 * analyses started and were released 2025-07-07.
 */
public class KpiServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ScopeRange JUNE_2024 = ScopeRange.of("30d", LocalDate.of(2024, 6, 30));

    private static final ScopeRange JULY_2025 = new ScopeRange(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31),
            LocalDate.of(2025, 5, 31), LocalDate.of(2025, 6, 30));

    @Autowired
    private KpiService kpiService;

    @Autowired
    private TATReportService tatReportService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private QiConfigService qiConfigService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbcTemplate.execute("TRUNCATE TABLE clinlims.note, clinlims.qc_rule_violation, clinlims.qc_result CASCADE");
        jdbcTemplate.update("DELETE FROM clinlims.qi_config");
        qiConfigService.saveIndicator("TAT", config(24, 48), "1");
        qiConfigService.saveIndicator("REJECTION", config(2, 5), "1");
    }

    @Test
    public void samplesCountTheScopeAndThePriorPeriodSeparately() {
        ManagementKpis kpis = kpiService.getKpis(JUNE_2024);

        assertEquals(0, BigDecimal.valueOf(2).compareTo(kpis.samples().value()));
        assertEquals(0, BigDecimal.ZERO.compareTo(kpis.samples().prior()));
        assertEquals(2, kpis.samples().spark().stream().mapToLong(BigDecimal::longValue).sum());
        assertEquals(kpis.periods().size(), kpis.samples().spark().size());
    }

    @Test
    public void medianTatIsTheTatReportsMedianAndNothingIsOnTimeAfterAYear() {
        ManagementKpis kpis = kpiService.getKpis(JUNE_2024);

        assertEquals(
                tatReportService
                        .getSummary(JUNE_2024.from(), JUNE_2024.to(), TATSegment.RECEIPT_TO_VALIDATION,
                                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null)
                        .getMedian(),
                kpis.medianTat().value());
        assertEquals(0, BigDecimal.ZERO.compareTo(kpis.onTime().value()));
        assertNull(kpis.onTime().prior());
    }

    @Test
    public void onTimeByLabUnitKeysLikeTheTatBreakdown() {
        Map<String, BigDecimal> byUnit = kpiService.onTimeByLabUnit(JUNE_2024);

        assertEquals(
                tatReportService
                        .getSummary(JUNE_2024.from(), JUNE_2024.to(), TATSegment.RECEIPT_TO_VALIDATION,
                                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "LAB_UNIT")
                        .getBreakdown().stream().map(row -> row.getDimensionValue()).collect(Collectors.toSet()),
                byUnit.keySet());
        byUnit.values().forEach(percent -> assertEquals(0, BigDecimal.ZERO.compareTo(percent)));
    }

    @Test
    public void rejectionRateUsesTheQiDefinition() {
        Note note = noteService.createSavableNote(analysisService.get("2"), NoteType.REJECTION_REASON,
                "Hemolyzed specimen", "Rejection Reason", "1");
        noteService.insert(note);

        ManagementKpis kpis = kpiService.getKpis(JULY_2025);

        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(kpis.rejection().value()));
        assertNull(kpis.rejection().prior());
    }

    @Test
    public void qcRunsFailOnAnOutcomeOrAWestgardRejectionButNotAWarning() {
        insertQcRun("9701", "MANUAL", "1.0", "PASS", "2025-07-07 08:00");
        insertQcRun("9702", "MANUAL", "1.0", "FAIL", "2025-07-08 08:00");
        insertQcRun("9703", "ASTM", "2.0", null, "2025-07-09 08:00");
        insertQcRun("9704", "ASTM", "2.0", null, "2025-07-10 08:00");
        insertQcRun("9705", "RDT", null, "INVALID", "2025-07-11 08:00");
        insertQcRun("9706", "ASTM", "2.0", null, "2025-06-15 08:00");
        insertViolation("9801", "9703", "REJECTION");
        insertViolation("9802", "9704", "WARNING");

        ManagementKpis kpis = kpiService.getKpis(JULY_2025);

        assertEquals(0, BigDecimal.valueOf(40.0).compareTo(kpis.qcPass().value()));
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(kpis.qcPass().prior()));
    }

    private static QiConfigView config(int target, int action) {
        QiConfigView view = new QiConfigView();
        view.setEnabled(true);
        view.setTarget(BigDecimal.valueOf(target));
        view.setAction(BigDecimal.valueOf(action));
        return view;
    }

    private void insertQcRun(String id, String source, String value, String outcome, String runAt) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.qc_result (id, test_id, result_value, qualitative_outcome,"
                        + " run_date_time, result_status, sys_user_id, last_updated, source)"
                        + " VALUES (?, '1', CAST(? AS numeric), ?, CAST(? AS timestamp), 'FINAL', '1', now(), ?)",
                id, value, outcome, runAt, source);
    }

    private void insertViolation(String id, String resultId, String severity) {
        jdbcTemplate.update("INSERT INTO clinlims.qc_rule_violation (id, triggering_result_id, rule_code,"
                + " violation_date_time, severity, test_id, resolution_status, sys_user_id, last_updated)"
                + " VALUES (?, ?, '1_3S', now(), ?, '1', 'UNRESOLVED', '1', now())", id, resultId, severity);
    }
}
