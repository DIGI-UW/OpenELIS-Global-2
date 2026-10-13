package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis.Kpi;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis.Thresholds;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.openelisglobal.reports.tat.service.TATReportService;
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
    private TATReportService tatReportService;

    @Autowired
    private RejectionReportService rejectionReportService;

    @Autowired
    private QiConfigService qiConfigService;

    @Override
    @Transactional(readOnly = true)
    public ManagementKpis getKpis(ScopeRange scope) {
        String interval = ChronoUnit.DAYS.between(scope.from(), scope.to()) > DAILY_SPARK_MAX_DAYS ? "WEEKLY" : "DAILY";
        List<String> periods = periods(scope.from(), scope.to(), interval);
        return new ManagementKpis(samples(scope, interval, periods), medianTat(scope, interval, periods),
                onTime(scope, interval, periods), rejection(scope, interval, periods),
                qcPass(scope, interval, periods));
    }

    private Kpi samples(ScopeRange scope, String interval, List<String> periods) {
        Map<String, Long> current = kpiDAO.countSamplesReceivedByPeriod(scope.from(), scope.to(), interval);
        Map<String, Long> prior = kpiDAO.countSamplesReceivedByPeriod(scope.priorFrom(), scope.priorTo(), interval);
        return new Kpi(BigDecimal.valueOf(current.values().stream().mapToLong(Long::longValue).sum()),
                BigDecimal.valueOf(prior.values().stream().mapToLong(Long::longValue).sum()),
                align(periods, p -> BigDecimal.valueOf(current.getOrDefault(p, 0L))), null);
    }

    private Kpi medianTat(ScopeRange scope, String interval, List<String> periods) {
        Map<String, BigDecimal> medians = new HashMap<>();
        TATTrendResponse trend = tatReportService.getTrend(scope.from(), scope.to(), TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, interval, null);
        trend.getSeries()
                .forEach(series -> series.getDataPoints().forEach(p -> medians.put(p.getPeriod(), p.getMedian())));
        return new Kpi(tatMedian(scope.from(), scope.to()), tatMedian(scope.priorFrom(), scope.priorTo()),
                align(periods, medians::get), null);
    }

    private BigDecimal tatMedian(LocalDate from, LocalDate to) {
        return tatReportService.getSummary(from, to, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR,
                null, null, null, null, null, null, false, null).getMedian();
    }

    // Validated analyses within the QI TAT target, receipt to validation.
    private Kpi onTime(ScopeRange scope, String interval, List<String> periods) {
        BigDecimal targetHours = tatTargetHours();
        if (targetHours == null) {
            return new Kpi(null, null, align(periods, p -> null), ON_TIME_THRESHOLDS);
        }
        List<TATResult> current = tatResults(scope.from(), scope.to());
        Map<String, long[]> byPeriod = onTimeCounts(current, targetHours,
                r -> QiReportSupport.periodKey(r.getReceived().toLocalDateTime().toLocalDate(), interval));
        return new Kpi(onTimePercent(onTimeCounts(current, targetHours, r -> "").get("")),
                onTimePercent(
                        onTimeCounts(tatResults(scope.priorFrom(), scope.priorTo()), targetHours, r -> "").get("")),
                align(periods, p -> onTimePercent(byPeriod.get(p))), ON_TIME_THRESHOLDS);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> onTimeByLabUnit(ScopeRange scope) {
        BigDecimal targetHours = tatTargetHours();
        Map<String, BigDecimal> percents = new HashMap<>();
        if (targetHours != null) {
            onTimeCounts(tatResults(scope.from(), scope.to()), targetHours,
                    r -> r.getLabUnit() == null || r.getLabUnit().isBlank() ? "Unknown" : r.getLabUnit())
                    .forEach((unit, counts) -> percents.put(unit, onTimePercent(counts)));
        }
        return percents;
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

    /** {validated, on time} per key; analyses not yet validated are left out. */
    private static Map<String, long[]> onTimeCounts(List<TATResult> results, BigDecimal targetHours,
            Function<TATResult, String> key) {
        Map<String, long[]> counts = new HashMap<>();
        for (TATResult result : results) {
            if (result.getSelectedSegmentTat() != null) {
                long[] count = counts.computeIfAbsent(key.apply(result), k -> new long[2]);
                count[0]++;
                count[1] += result.getSelectedSegmentTat().compareTo(targetHours) <= 0 ? 1 : 0;
            }
        }
        return counts;
    }

    private static BigDecimal onTimePercent(long[] counts) {
        return counts == null ? null : percent(counts[1], counts[0]);
    }

    private List<TATResult> tatResults(LocalDate from, LocalDate to) {
        return tatReportService.getAllResults(from, to, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR,
                null, null, null, null, null, null, false);
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
