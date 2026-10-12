package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.kpi.KpiDAO.TatFigures;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis.Kpi;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis.Thresholds;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KpiServiceImpl implements KpiService {

    // FRS defaults; it names no "approaching" band, so action sits ten points
    // below.
    static final Thresholds ON_TIME_THRESHOLDS = new Thresholds(true, BigDecimal.valueOf(90), BigDecimal.valueOf(80),
            "HIGHER_BETTER");
    static final Thresholds QC_PASS_THRESHOLDS = new Thresholds(true, BigDecimal.valueOf(95), BigDecimal.valueOf(85),
            "HIGHER_BETTER");

    private static final long DAILY_SPARK_MAX_DAYS = 92;

    @Autowired
    private KpiDAO kpiDAO;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private RejectionReportService rejectionReportService;

    @Autowired
    private QiConfigService qiConfigService;

    @Override
    @Transactional(readOnly = true)
    public ManagementKpis getKpis(ScopeRange scope) {
        String interval = ChronoUnit.DAYS.between(scope.from(), scope.to()) > DAILY_SPARK_MAX_DAYS ? "WEEKLY" : "DAILY";
        List<String> periods = periods(scope.from(), scope.to(), interval);
        BigDecimal targetHours = tatTargetHours();
        Map<String, TatFigures> current = kpiDAO.tatByPeriod(scope.from(), scope.to(), interval, targetHours);
        TatFigures prior = kpiDAO.tatByPeriod(scope.priorFrom(), scope.priorTo(), interval, targetHours)
                .get(KpiDAO.ALL_PERIODS);
        return new ManagementKpis(samples(scope, interval, periods), medianTat(current, prior, periods),
                onTime(current, prior, periods, targetHours), rejection(scope, interval, periods),
                qcPass(scope, interval, periods));
    }

    private Kpi samples(ScopeRange scope, String interval, List<String> periods) {
        Map<String, Long> current = kpiDAO.countSamplesReceivedByPeriod(scope.from(), scope.to(), interval);
        Map<String, Long> prior = kpiDAO.countSamplesReceivedByPeriod(scope.priorFrom(), scope.priorTo(), interval);
        return new Kpi(BigDecimal.valueOf(current.values().stream().mapToLong(Long::longValue).sum()),
                BigDecimal.valueOf(prior.values().stream().mapToLong(Long::longValue).sum()),
                align(periods, p -> BigDecimal.valueOf(current.getOrDefault(p, 0L))), null);
    }

    private Kpi medianTat(Map<String, TatFigures> current, TatFigures prior, List<String> periods) {
        return new Kpi(median(current.get(KpiDAO.ALL_PERIODS)), median(prior),
                align(periods, p -> median(current.get(p))), null);
    }

    private static BigDecimal median(TatFigures figures) {
        return figures == null ? null : figures.medianHours();
    }

    // Validated analyses within the QI TAT target, receipt to validation.
    private Kpi onTime(Map<String, TatFigures> current, TatFigures prior, List<String> periods,
            BigDecimal targetHours) {
        if (targetHours == null) {
            return new Kpi(null, null, align(periods, p -> null), ON_TIME_THRESHOLDS);
        }
        return new Kpi(onTimePercent(current.get(KpiDAO.ALL_PERIODS)), onTimePercent(prior),
                align(periods, p -> onTimePercent(current.get(p))), ON_TIME_THRESHOLDS);
    }

    private static BigDecimal onTimePercent(TatFigures figures) {
        return figures == null ? null : percent(figures.onTime(), figures.validated());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> onTimeByLabUnit(ScopeRange scope) {
        BigDecimal targetHours = tatTargetHours();
        Map<String, long[]> byUnit = new HashMap<>();
        if (targetHours != null) {
            kpiDAO.countOnTimeBySection(scope.from(), scope.to(), targetHours).forEach((section, counts) -> {
                long[] unit = byUnit.computeIfAbsent(labUnit(section), k -> new long[2]);
                unit[0] += counts[0];
                unit[1] += counts[1];
            });
        }
        Map<String, BigDecimal> percents = new HashMap<>();
        byUnit.forEach((unit, counts) -> percents.put(unit, percent(counts[1], counts[0])));
        return percents;
    }

    // Named as on the TAT report and /workload.
    private String labUnit(String sectionId) {
        String name = sectionId == null ? null : testSectionService.getTestSectionById(sectionId).getLocalizedName();
        return name == null || name.isBlank() ? "Unknown" : name;
    }

    private BigDecimal tatTargetHours() {
        ResolvedConfig tat = qiConfig("TAT");
        return tat != null && tat.isEnabled() ? tat.getTarget() : null;
    }

    /**
     * Null when the indicator has no default row, so one missing config does not
     * fail the strip.
     */
    private ResolvedConfig qiConfig(String indicatorKey) {
        try {
            return qiConfigService.resolve(indicatorKey, null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Kpi rejection(ScopeRange scope, String interval, List<String> periods) {
        Map<String, BigDecimal> rates = new HashMap<>();
        for (RejectionTrendResponse.TrendPoint point : rejectionReportService
                .getTrend(scope.from(), scope.to(), interval).getPoints()) {
            rates.put(point.getPeriod(), decimal(point.getRatePercent()));
        }
        ResolvedConfig config = qiConfig("REJECTION");
        return new Kpi(decimal(rejectionReportService.getSummary(scope.from(), scope.to()).getRatePercent()),
                decimal(rejectionReportService.getSummary(scope.priorFrom(), scope.priorTo()).getRatePercent()),
                align(periods, rates::get),
                config == null ? null
                        : new Thresholds(config.isEnabled(), config.getTarget(), config.getAction(),
                                config.getDirection() == null ? null : config.getDirection().name()));
    }

    private Kpi qcPass(ScopeRange scope, String interval, List<String> periods) {
        Map<String, long[]> current = kpiDAO.countQcRunsByPeriod(scope.from(), scope.to(), interval);
        Map<String, long[]> prior = kpiDAO.countQcRunsByPeriod(scope.priorFrom(), scope.priorTo(), interval);
        return new Kpi(passRate(current.values()), passRate(prior.values()),
                align(periods, p -> current.containsKey(p) ? passRate(List.of(current.get(p))) : null),
                QC_PASS_THRESHOLDS);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal qcPassRate(LocalDate from, LocalDate to) {
        return passRate(kpiDAO.countQcRunsByPeriod(from, to, "MONTHLY").values());
    }

    private static BigDecimal passRate(Iterable<long[]> runs) {
        long total = 0;
        long failed = 0;
        for (long[] run : runs) {
            total += run[0];
            failed += run[1];
        }
        return percent(total - failed, total);
    }

    private static List<String> periods(LocalDate from, LocalDate to, String interval) {
        return from.datesUntil(to.plusDays(1)).map(d -> QiReportSupport.periodKey(d, interval)).distinct().toList();
    }

    private static List<BigDecimal> align(List<String> periods, Function<String, BigDecimal> value) {
        return periods.stream().map(value).toList();
    }

    private static BigDecimal percent(long part, long whole) {
        return decimal(QiReportSupport.ratePercent(part, whole));
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
