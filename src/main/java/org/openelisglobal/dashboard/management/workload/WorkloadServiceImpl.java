package org.openelisglobal.dashboard.management.workload;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.kpi.KpiService;
import org.openelisglobal.dashboard.management.kpi.KpiService.SectionTat;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkloadServiceImpl implements WorkloadService {

    @Autowired
    private WorkloadDAO workloadDAO;
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
        Map<String, SectionTat> tatBySection = kpiService.tatBySection(scope);

        List<WorkloadResponse.Section> sections = countsBySection.entrySet().stream().map(e -> {
            String name = testSectionService.getTestSectionById(e.getKey()).getLocalizedName();
            SectionTat tat = tatBySection.get(e.getKey());
            return new WorkloadResponse.Section(e.getKey(), name, Arrays.stream(e.getValue()).boxed().toList(),
                    Arrays.stream(e.getValue()).sum(), tat == null ? null : tat.medianHours(),
                    tat == null ? null : tat.p90Hours(), tat == null ? null : tat.onTimePercent());
        }).sorted(Comparator.comparingLong(WorkloadResponse.Section::samples).reversed()).toList();
        return new WorkloadResponse(days, sections);
    }
}
