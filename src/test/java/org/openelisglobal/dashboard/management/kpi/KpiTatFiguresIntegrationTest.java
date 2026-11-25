package org.openelisglobal.dashboard.management.kpi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class KpiTatFiguresIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ScopeRange SCOPE = new ScopeRange(LocalDate.of(2097, 3, 1), LocalDate.of(2097, 3, 10),
            LocalDate.of(2097, 2, 19), LocalDate.of(2097, 2, 28));

    private static final BigDecimal TARGET_HOURS = BigDecimal.valueOf(24);

    @Autowired
    private KpiService kpiService;

    @Autowired
    private TATReportService tatReportService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private QiConfigService qiConfigService;

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final String run = UUID.randomUUID().toString().substring(0, 8);
    private int samples;
    private String unitA;
    private String unitB;

    @Before
    public void seedAnalyses() {
        fixtures.ensureRequiredWorkflowStatuses();
        testSectionService.refreshNames();
        jdbcTemplate.update("DELETE FROM clinlims.qi_config");
        QiConfigView tat = new QiConfigView();
        tat.setEnabled(true);
        tat.setTarget(TARGET_HOURS);
        tat.setAction(BigDecimal.valueOf(48));
        qiConfigService.saveIndicator("TAT", tat, "1");

        List<String[]> tests = testsInTwoNamedSections();
        String testA = tests.get(0)[0];
        String sectionA = tests.get(0)[1];
        String testB = tests.get(1)[0];
        String sectionB = tests.get(1)[1];
        unitA = testSectionService.getTestSectionById(sectionA).getLocalizedName();
        unitB = testSectionService.getTestSectionById(sectionB).getLocalizedName();

        analysis(sample("2097-03-01 00:00"), testA, sectionA, "2097-03-01 01:00:18", Status.DONE);
        long late = sample("2097-03-02 08:00");
        analysis(late, testB, null, "2097-03-03 14:00", Status.DONE);
        analysis(late, testB, null, "2097-03-02 10:00", Status.CANCELED);
        analysis(sample("2097-03-02 09:00"), testA, sectionB, "2097-03-02 19:00", Status.DONE);
        analysis(rejectedSample("2097-03-03 10:00"), testA, sectionA, "2097-03-03 13:00", Status.DONE);
        analysis(sample("2097-03-03 11:00"), testA, sectionA, null, Status.DONE);
        analysis(sample("2097-03-04 12:00"), testA, sectionA, "2097-03-04 11:00", Status.DONE);
        analysis(sample("2097-03-05 13:00"), testA, sectionA, "2097-03-05 14:00:17.9999", Status.DONE);
        analysis(sample("2097-03-06 09:30"), null, null, "2097-03-07 05:30", Status.DONE);
        analysis(sample("2097-02-27 10:00"), testA, sectionA, "2097-02-27 15:00", Status.DONE);
        analysis(sample("2097-02-28 23:00"), testB, sectionB, "2097-03-02 23:00", Status.DONE);
        // Outside both periods.
        analysis(sample("2097-02-18 23:00"), testA, sectionA, "2097-02-19 01:00", Status.DONE);
        analysis(sample("2097-03-11 01:00"), testA, sectionA, "2097-03-11 03:00", Status.DONE);
    }

    @Test
    public void tatFiguresMatchTheTatReportsRows() {
        ManagementKpis kpis = kpiService.getKpis(SCOPE);
        Map<String, BigDecimal> byUnit = kpiService.onTimeByLabUnit(SCOPE);

        Expected expected = new Expected(tatReport(SCOPE.priorFrom(), SCOPE.to()));
        assertEquals(new BigDecimal("10.00"), kpis.medianTat().value());
        assertEquals(new BigDecimal("80.0"), kpis.onTime().value());
        assertEquals(expected.median(true), kpis.medianTat().value());
        assertEquals(expected.median(false), kpis.medianTat().prior());
        assertEquals(expected.medianSpark(), kpis.medianTat().spark());
        assertEquals(expected.onTime(true), kpis.onTime().value());
        assertEquals(expected.onTime(false), kpis.onTime().prior());
        assertEquals(expected.onTimeSpark(), kpis.onTime().spark());

        Map<String, BigDecimal> expectedByUnit = byUnit(tatReport(SCOPE.from(), SCOPE.to()));
        assertEquals(Set.of(unitA, unitB, "Unknown"), expectedByUnit.keySet());
        assertEquals(expectedByUnit, byUnit);
    }

    @Test
    public void tatFiguresLoadNoAnalyses() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        try {
            kpiService.getKpis(SCOPE);
            kpiService.onTimeByLabUnit(SCOPE);
            assertEquals(0, statistics.getEntityStatistics(Analysis.class.getName()).getLoadCount());
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    // The KPI figures computed in Java from the TAT report's rows.
    private class Expected {
        private final List<TATResult> current = new ArrayList<>();
        private final List<TATResult> prior = new ArrayList<>();

        Expected(List<TATResult> rows) {
            Timestamp scopeStart = QiReportSupport.startOf(SCOPE.from());
            rows.forEach(r -> (r.getReceived().before(scopeStart) ? prior : current).add(r));
        }

        BigDecimal median(boolean inScope) {
            return lowerMiddle(inScope ? current : prior);
        }

        BigDecimal onTime(boolean inScope) {
            return onTimePercent(inScope ? current : prior);
        }

        List<BigDecimal> medianSpark() {
            return spark(KpiTatFiguresIntegrationTest::lowerMiddle);
        }

        List<BigDecimal> onTimeSpark() {
            return spark(KpiTatFiguresIntegrationTest::onTimePercent);
        }

        private List<BigDecimal> spark(Function<List<TATResult>, BigDecimal> figure) {
            Map<LocalDate, List<TATResult>> byDay = current.stream()
                    .collect(Collectors.groupingBy(r -> r.getReceived().toLocalDateTime().toLocalDate()));
            return SCOPE.from().datesUntil(SCOPE.to().plusDays(1))
                    .map(day -> figure.apply(byDay.getOrDefault(day, List.of()))).toList();
        }
    }

    private Map<String, BigDecimal> byUnit(List<TATResult> rows) {
        Map<String, List<TATResult>> grouped = new LinkedHashMap<>();
        rows.stream().filter(r -> r.getSelectedSegmentTat() != null)
                .forEach(r -> grouped.computeIfAbsent(
                        r.getLabUnit() == null || r.getLabUnit().isBlank() ? "Unknown" : r.getLabUnit(),
                        k -> new ArrayList<>()).add(r));
        Map<String, BigDecimal> percents = new HashMap<>();
        grouped.forEach((unit, unitRows) -> percents.put(unit, onTimePercent(unitRows)));
        return percents;
    }

    private static BigDecimal lowerMiddle(List<TATResult> rows) {
        List<BigDecimal> tats = rows.stream().map(TATResult::getSelectedSegmentTat).filter(Objects::nonNull).sorted()
                .toList();
        return tats.isEmpty() ? null : tats.get((tats.size() - 1) / 2);
    }

    private static BigDecimal onTimePercent(List<TATResult> rows) {
        List<BigDecimal> tats = rows.stream().map(TATResult::getSelectedSegmentTat).filter(Objects::nonNull).toList();
        Double rate = QiReportSupport.ratePercent(tats.stream().filter(t -> t.compareTo(TARGET_HOURS) <= 0).count(),
                tats.size());
        return rate == null ? null : BigDecimal.valueOf(rate);
    }

    private List<TATResult> tatReport(LocalDate from, LocalDate to) {
        return tatReportService.getAllResults(from, to, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR,
                null, null, null, null, null, null, false);
    }

    private List<String[]> testsInTwoNamedSections() {
        Map<String, String[]> byName = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList("SELECT t.id, t.test_section_id FROM clinlims.test t"
                + " WHERE t.test_section_id IS NOT NULL ORDER BY t.id")) {
            String section = String.valueOf(row.get("test_section_id"));
            String name = testSectionService.getTestSectionById(section).getLocalizedName();
            if (name != null && !name.isBlank()) {
                byName.putIfAbsent(name, new String[] { String.valueOf(row.get("id")), section });
            }
        }
        assertTrue("tests in two named sections", byName.size() >= 2);
        return new ArrayList<>(byName.values()).subList(0, 2);
    }

    private enum Status {
        DONE, CANCELED
    }

    private long sample(String received) {
        return sample(received, false);
    }

    private long rejectedSample(String received) {
        return sample(received, true);
    }

    private long sample(String received, boolean rejected) {
        Timestamp at = Timestamp.valueOf(LocalDateTime.parse(received.replace(' ', 'T')));
        long sampleId = jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date)"
                        + " VALUES (nextval('clinlims.sample_seq'), ?, ?, ?) RETURNING id",
                Long.class, "KS" + run + samples++, at, at);
        return jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, rejected)"
                        + " VALUES (nextval('clinlims.sample_item_seq'), ?, 1,"
                        + " (SELECT min(id) FROM clinlims.status_of_sample), ?) RETURNING id",
                Long.class, sampleId, rejected);
    }

    private void analysis(long sampleItemId, String testId, String sectionId, String released, Status status) {
        jdbcTemplate.update("INSERT INTO clinlims.analysis"
                + " (id, sampitem_id, test_id, test_sect_id, analysis_type, status_id, released_date)"
                + " VALUES (nextval('clinlims.analysis_seq'), ?, CAST(? AS numeric), CAST(? AS numeric), 'MANUAL',"
                + " (SELECT min(id) FROM clinlims.status_of_sample WHERE status_type = 'ANALYSIS' AND name "
                + (status == Status.CANCELED ? "=" : "<>") + " 'Test Canceled'), CAST(? AS timestamp))", sampleItemId,
                testId, sectionId, released);
    }
}
