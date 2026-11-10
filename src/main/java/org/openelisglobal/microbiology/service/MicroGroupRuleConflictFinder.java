package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.microbiology.form.MicroReportingRuleForm;
import org.springframework.stereotype.Component;

@Component
public class MicroGroupRuleConflictFinder {

    public record Conflict(MicroReportingRuleForm first, MicroReportingRuleForm second, String organismId) {
    }

    public List<Conflict> find(Map<String, Set<String>> groupsByOrganismId, List<MicroReportingRuleForm> rules) {
        List<MicroReportingRuleForm> active = rules.stream().filter(rule -> Boolean.TRUE.equals(rule.active)).toList();
        List<MicroReportingRuleForm> candidates = active.stream()
                .filter(rule -> rule.organismId == null && rule.organismGroup != null).toList();
        List<MicroReportingRuleForm> speciesRules = active.stream().filter(rule -> rule.organismId != null).toList();
        List<Conflict> conflicts = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                MicroReportingRuleForm first = candidates.get(i);
                MicroReportingRuleForm second = candidates.get(j);
                if (Objects.equals(first.organismGroup, second.organismGroup)
                        || !Objects.equals(first.antibioticId, second.antibioticId)
                        || !Objects.equals(first.clinicalContext, second.clinicalContext)
                        || !sampleTypesTie(first, second) || !disagree(first, second)) {
                    continue;
                }
                String organismId = firstOrganism(groupsByOrganismId, first, second, speciesRules);
                if (organismId != null) {
                    conflicts.add(new Conflict(first, second, organismId));
                }
            }
        }
        return conflicts;
    }

    private static String firstOrganism(Map<String, Set<String>> groupsByOrganismId, MicroReportingRuleForm first,
            MicroReportingRuleForm second, List<MicroReportingRuleForm> speciesRules) {
        for (Map.Entry<String, Set<String>> entry : groupsByOrganismId.entrySet()) {
            Set<String> groups = entry.getValue();
            if (groups.contains(first.organismGroup) && groups.contains(second.organismGroup)
                    && speciesRules.stream().noneMatch(species -> settles(species, entry.getKey(), first, second))) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean settles(MicroReportingRuleForm species, String organismId, MicroReportingRuleForm first,
            MicroReportingRuleForm second) {
        if (!organismId.equals(species.organismId) || !Objects.equals(species.antibioticId, first.antibioticId)
                || species.clinicalContext != null && !species.clinicalContext.equals(first.clinicalContext)) {
            return false;
        }
        List<String> types = species.sampleTypeIds == null ? List.of() : species.sampleTypeIds;
        List<String> firstTypes = first.sampleTypeIds == null ? List.of() : first.sampleTypeIds;
        List<String> secondTypes = second.sampleTypeIds == null ? List.of() : second.sampleTypeIds;
        return types.isEmpty()
                || !firstTypes.isEmpty() && firstTypes.stream().filter(secondTypes::contains).allMatch(types::contains);
    }

    private static boolean sampleTypesTie(MicroReportingRuleForm first, MicroReportingRuleForm second) {
        List<String> firstTypes = first.sampleTypeIds == null ? List.of() : first.sampleTypeIds;
        List<String> secondTypes = second.sampleTypeIds == null ? List.of() : second.sampleTypeIds;
        if (firstTypes.isEmpty() && secondTypes.isEmpty()) {
            return true;
        }
        if (firstTypes.isEmpty() || secondTypes.isEmpty()) {
            return false;
        }
        return firstTypes.stream().anyMatch(secondTypes::contains);
    }

    private static boolean disagree(MicroReportingRuleForm first, MicroReportingRuleForm second) {
        return !Objects.equals(first.reportBehavior, second.reportBehavior)
                || !Objects.equals(first.cascadeCondition, second.cascadeCondition)
                || !Objects.equals(first.note, second.note) || !Objects.equals(first.noteWhen, second.noteWhen);
    }
}
