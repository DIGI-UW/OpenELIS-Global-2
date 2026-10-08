package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.microbiology.form.MicroCaseSpecimenForm;
import org.openelisglobal.microbiology.form.MicroCultureSetWarningForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Evaluates recorded assignments without changing grouping or blocking saves.
 */
@Service
public class MicroCultureSetWarningService {
    private final int intervalMinutes;

    public MicroCultureSetWarningService(@Value("${amr.cultureSetIntervalMinutes:30}") int intervalMinutes) {
        if (intervalMinutes < 1) {
            throw new IllegalArgumentException("Culture set interval must be positive");
        }
        this.intervalMinutes = intervalMinutes;
    }

    public List<MicroCultureSetWarningForm> evaluate(List<MicroCaseSpecimenForm> specimens) {
        Map<Integer, List<MicroCaseSpecimenForm>> sets = new LinkedHashMap<>();
        for (MicroCaseSpecimenForm specimen : specimens) {
            if (specimen.collectedInSets && specimen.cultureSetNumber != null) {
                sets.computeIfAbsent(specimen.cultureSetNumber, ignored -> new ArrayList<>()).add(specimen);
            }
        }
        List<MicroCultureSetWarningForm> warnings = new ArrayList<>();
        sets.forEach((number, bottles) -> {
            if (bottles.size() == 1) {
                warnings.add(new MicroCultureSetWarningForm(number, "SINGLE_BOTTLE", intervalMinutes));
            }
            List<String> containers = bottles.stream().map(bottle -> normalize(bottle.containerType))
                    .filter(Objects::nonNull).toList();
            if (containers.stream().distinct().count() < containers.size()) {
                warnings.add(new MicroCultureSetWarningForm(number, "REPEATED_CONTAINER", intervalMinutes));
            }
            if (bottles.stream().anyMatch(b -> "ADULT".equals(b.containerPopulation))
                    && bottles.stream().anyMatch(b -> "PAEDIATRIC".equals(b.containerPopulation))) {
                warnings.add(new MicroCultureSetWarningForm(number, "MIXED_POPULATIONS", intervalMinutes));
            }
            if (bottles.stream().map(bottle -> normalize(bottle.bodySite)).filter(Objects::nonNull).distinct()
                    .count() > 1) {
                warnings.add(new MicroCultureSetWarningForm(number, "DIFFERENT_SITES", intervalMinutes));
            }
            var times = bottles.stream().map(bottle -> bottle.collectionDate).filter(Objects::nonNull)
                    .mapToLong(time -> time.getTime()).summaryStatistics();
            if (times.getCount() > 1 && times.getMax() - times.getMin() > intervalMinutes * 60_000L) {
                warnings.add(new MicroCultureSetWarningForm(number, "COLLECTION_INTERVAL", intervalMinutes));
            }
        });
        return List.copyOf(warnings);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
