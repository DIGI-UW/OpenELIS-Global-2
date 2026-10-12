package org.openelisglobal.dashboard.management.surveillance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.surveillance.SurveillanceResponse.Metric;
import org.openelisglobal.dashboard.management.surveillance.SurveillanceResponse.Program;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService.FirstIsolate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveillanceServiceImpl implements SurveillanceService {

    static final int METRICS_PER_CARD = 3;
    static final int MIN_ISOLATES = 10;

    @Autowired
    private SurveillanceDAO surveillanceDAO;
    @Autowired
    private MicroWhonetDatasetService microWhonetDatasetService;

    private record Ranked(int order, Metric metric) {
    }

    private static final class Card {
        final List<Ranked> metrics = new ArrayList<>();
        final Set<String> missingCodes = new TreeSet<>();
        int testCount;
    }

    @Override
    @Transactional(readOnly = true)
    public SurveillanceResponse getSurveillance(ScopeRange scope) {
        Map<Long, Card> cards = new HashMap<>();
        addPositivity(scope, cards);
        addResistance(scope, cards);
        List<Program> programs = new ArrayList<>();
        for (Object[] row : surveillanceDAO.getActivePrograms()) {
            Card card = cards.get(((Number) row[0]).longValue());
            if (card == null || card.metrics.isEmpty()) {
                continue;
            }
            programs.add(new Program((String) row[1], (String) row[2], card.testCount, card.metrics.stream()
                    .sorted(Comparator.comparingInt(Ranked::order).thenComparing(ranked -> ranked.metric().label()))
                    .limit(METRICS_PER_CARD).map(Ranked::metric).toList(), List.copyOf(card.missingCodes)));
        }
        return new SurveillanceResponse(programs);
    }

    private void addPositivity(ScopeRange scope, Map<Long, Card> cards) {
        Map<String, int[]> counts = new HashMap<>();
        for (Object[] row : surveillanceDAO.countPositivity(scope)) {
            long programId = ((Number) row[0]).longValue();
            int[] testedPositive = { ((Number) row[3]).intValue(), ((Number) row[4]).intValue() };
            if (row[1] == null) {
                if ("C".equals(row[2])) {
                    cards.computeIfAbsent(programId, id -> new Card()).testCount += testedPositive[0];
                }
            } else {
                counts.put(programId + "|" + row[1] + "|" + row[2], testedPositive);
            }
        }
        for (Object[] row : surveillanceDAO.getPositivityMetrics()) {
            long programId = ((Number) row[0]).longValue();
            String key = (String) row[1];
            cards.computeIfAbsent(programId, id -> new Card()).metrics
                    .add(new Ranked(((Number) row[3]).intValue(), metric(key, (String) row[2],
                            counts.get(programId + "|" + key + "|C"), counts.get(programId + "|" + key + "|P"), 1)));
        }
    }

    // Share of first isolates of the metric's organisms, tested against any of its
    // antibiotics, that read R to at least one of them.
    private void addResistance(ScopeRange scope, Map<Long, Card> cards) {
        List<Object[]> metrics = surveillanceDAO.getResistanceMetrics();
        if (metrics.isEmpty()) {
            return;
        }
        List<Object[]> organisms = surveillanceDAO.getActiveOrganisms();
        List<Object[]> antibiotics = surveillanceDAO.getActiveAntibiotics();
        List<FirstIsolate> current = microWhonetDatasetService.selectFirstIsolates(scope.from(), scope.to());
        List<FirstIsolate> prior = microWhonetDatasetService.selectFirstIsolates(scope.priorFrom(), scope.priorTo());
        Map<Long, Set<String>> testedIsolates = new HashMap<>();
        for (Object[] row : metrics) {
            long programId = ((Number) row[0]).longValue();
            Card card = cards.computeIfAbsent(programId, id -> new Card());
            Set<String> organismCodes = codes((String) row[3]);
            String group = row[4] == null ? null : ((String) row[4]).toLowerCase(Locale.ROOT);
            Set<String> antibioticCodes = codes((String) row[5]);
            Set<String> organismIds = new HashSet<>();
            for (Object[] organism : organisms) {
                if (organismCodes.contains(lower(organism[1])) || (group != null && group.equals(lower(organism[2])))) {
                    organismIds.add((String) organism[0]);
                }
            }
            Set<String> antibioticIds = new HashSet<>();
            for (Object[] antibiotic : antibiotics) {
                if (antibioticCodes.contains(lower(antibiotic[1]))) {
                    antibioticIds.add((String) antibiotic[0]);
                }
            }
            if (organismIds.isEmpty()) {
                card.missingCodes.addAll(organismCodes);
                if (row[4] != null) {
                    card.missingCodes.add((String) row[4]);
                }
            }
            if (antibioticIds.isEmpty()) {
                card.missingCodes.addAll(Arrays.stream(((String) row[5]).split(",")).map(String::trim).toList());
            }
            Set<String> tested = testedIsolates.computeIfAbsent(programId, id -> new HashSet<>());
            card.metrics.add(new Ranked(((Number) row[6]).intValue(),
                    metric((String) row[1], (String) row[2], resistance(current, organismIds, antibioticIds, tested),
                            resistance(prior, organismIds, antibioticIds, new HashSet<>()), MIN_ISOLATES)));
        }
        testedIsolates.forEach((programId, tested) -> cards.get(programId).testCount += tested.size());
    }

    private static int[] resistance(List<FirstIsolate> isolates, Set<String> organismIds, Set<String> antibioticIds,
            Set<String> tested) {
        int[] testedResistant = new int[2];
        for (FirstIsolate isolate : isolates) {
            if (!organismIds.contains(isolate.organismId())) {
                continue;
            }
            List<String> readings = isolate.results().stream()
                    .filter(result -> antibioticIds.contains(result.antibioticId()))
                    .map(result -> String.valueOf(result.interpretation())).toList();
            if (!readings.isEmpty()) {
                tested.add(isolate.isolateId());
                testedResistant[0]++;
                if (readings.contains("R")) {
                    testedResistant[1]++;
                }
            }
        }
        return testedResistant;
    }

    private static Metric metric(String key, String label, int[] current, int[] prior, int minimum) {
        int[] now = current == null ? new int[2] : current;
        Double value = percent(now, minimum);
        Double before = percent(prior == null ? new int[2] : prior, minimum);
        return new Metric(key, label, value, now[1], now[0],
                value == null || before == null ? null : Math.round(10 * (value - before)) / 10.0);
    }

    private static Double percent(int[] testedPositive, int minimum) {
        return testedPositive[0] == 0 || testedPositive[0] < minimum ? null
                : Math.round(1000.0 * testedPositive[1] / testedPositive[0]) / 10.0;
    }

    private static Set<String> codes(String commaSeparated) {
        Set<String> codes = new HashSet<>();
        if (commaSeparated != null) {
            for (String code : commaSeparated.split(",")) {
                if (!code.isBlank()) {
                    codes.add(code.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return codes;
    }

    private static String lower(Object value) {
        return value == null ? null : ((String) value).toLowerCase(Locale.ROOT);
    }
}
