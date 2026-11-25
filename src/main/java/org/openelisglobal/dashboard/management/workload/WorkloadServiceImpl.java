package org.openelisglobal.dashboard.management.workload;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.kpi.KpiService;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkloadServiceImpl implements WorkloadService {

    @Autowired
    private WorkloadDAO workloadDAO;
    @Autowired
    private TATReportService tatReportService;
    @Autowired
    private KpiService kpiService;
    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional(readOnly = true)
    public WorkloadResponse getWorkload(ScopeRange scope) {
        List<String> days = scope.from().datesUntil(scope.to().plusDays(1)).map(LocalDate::toString).toList();
        Map<String, long[]> countsBySection = new HashMap<>();
        for (Object[] row : workloadDAO.countSamplesByDayAndSection(scope.from(), scope.to())) {
            long[] counts = countsBySection.computeIfAbsent((String) row[1], id -> new long[days.size()]);
            counts[(int) ChronoUnit.DAYS.between(scope.from(), LocalDate.parse((String) row[0]))] = ((Number) row[2])
                    .longValue();
        }
        // Same segment and mode as the QI Dashboard's TAT tile.
        Map<String, TATSummaryResponse.BreakdownRow> tatByLabUnit = tatReportService
                .getSummary(scope.from(), scope.to(), TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR,
                        null, null, null, null, null, null, false, "LAB_UNIT")
                .getBreakdown().stream()
                .collect(toMap(TATSummaryResponse.BreakdownRow::getDimensionValue, Function.identity()));
        Map<String, BigDecimal> onTime = kpiService.onTimeByLabUnit(scope);

        List<WorkloadResponse.Section> sections = countsBySection.entrySet().stream().map(e -> {
            String name = testSectionService.getTestSectionById(e.getKey()).getLocalizedName();
            TATSummaryResponse.BreakdownRow tat = tatByLabUnit.get(name);
            return new WorkloadResponse.Section(e.getKey(), name, Arrays.stream(e.getValue()).boxed().toList(),
                    Arrays.stream(e.getValue()).sum(), tat == null ? null : tat.getMedian(),
                    tat == null ? null : tat.getPercentile90(), onTime.get(name));
        }).sorted(Comparator.comparingLong(WorkloadResponse.Section::samples).reversed()).toList();
        return new WorkloadResponse(days, sections);
    }
}
