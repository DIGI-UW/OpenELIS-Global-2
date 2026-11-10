package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Component;

@Component
public class MicroOrganismGroupConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroOrganismDAO organisms;
    private final MicroAntibioticDAO antibiotics;
    private final MicroReportingRuleService reportingRules;
    private final MicroGroupRuleConflictFinder conflicts;
    private final MicroAstPanelConfigurationHandler panelDefaults;
    private final CatalogReferenceResolver resolver;

    public MicroOrganismGroupConfigurationHandler(MicroOrganismDAO organisms, MicroAntibioticDAO antibiotics,
            MicroReportingRuleService reportingRules, MicroGroupRuleConflictFinder conflicts,
            MicroAstPanelConfigurationHandler panelDefaults, CatalogReferenceResolver resolver) {
        this.organisms = organisms;
        this.antibiotics = antibiotics;
        this.reportingRules = reportingRules;
        this.conflicts = conflicts;
        this.panelDefaults = panelDefaults;
        this.resolver = resolver;
    }

    @Override
    public String getDomainName() {
        return "microbiology-organism-groups";
    }

    @Override
    public int getLoadOrder() {
        return 315;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "organism", "group" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        List<String> removals = new ArrayList<>();
        List<LoadedRow<String>> outcomes = transaction.run(() -> {
            List<String> errors = new ArrayList<>();
            List<MembershipRow> memberships = new ArrayList<>();
            Map<String, Map<String, Integer>> linesByOrganismAndGroup = new HashMap<>();
            Set<String> groupNames = new LinkedHashSet<>();
            for (CsvRow row : rows) {
                try {
                    MicroOrganism organism = organism(row);
                    String group = required(row, "group");
                    if (group.length() > 100) {
                        throw new IllegalArgumentException(
                                "Line " + row.lineNumber() + ": group is longer than 100 characters");
                    }
                    groupNames.add(group);
                    linesByOrganismAndGroup.computeIfAbsent(organism.getId(), ignored -> new HashMap<>()).put(group,
                            row.lineNumber());
                    memberships.add(new MembershipRow(organism, !organism.getLabGroups().contains(group)));
                } catch (RuntimeException e) {
                    errors.add(CsvLoadSummary.reason(e));
                    flushUnresolvedReferences(fileName, row.lineNumber());
                }
            }
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("; ", errors));
            }

            for (MicroOrganism organism : organisms.getAll()) {
                Set<String> wanted = linesByOrganismAndGroup.getOrDefault(organism.getId(), Map.of()).keySet();
                boolean changed = false;
                for (String group : groupNames) {
                    if (wanted.contains(group)) {
                        changed |= organism.getLabGroups().add(group);
                    } else if (organism.getLabGroups().remove(group)) {
                        removals.add(organism.getDisplayName() + " removed from group " + group);
                        changed = true;
                    }
                }
                if (changed) {
                    organisms.update(organism);
                }
            }

            Map<String, Set<String>> groupsByOrganismId = new HashMap<>();
            for (MicroOrganism organism : organisms.getAll()) {
                groupsByOrganismId.put(organism.getId(), Set.copyOf(organism.getLabGroups()));
            }
            List<String> conflictErrors = new ArrayList<>();
            for (MicroGroupRuleConflictFinder.Conflict conflict : conflicts.find(groupsByOrganismId,
                    reportingRules.getRules())) {
                Map<String, Integer> lines = linesByOrganismAndGroup.getOrDefault(conflict.organismId(), Map.of());
                Integer firstLine = lines.get(conflict.first().organismGroup);
                Integer secondLine = lines.get(conflict.second().organismGroup);
                Integer lineNumber = firstLine == null ? secondLine
                        : secondLine == null ? firstLine : Math.max(firstLine, secondLine);
                if (lineNumber == null) {
                    continue;
                }
                String organismName = organisms.get(conflict.organismId()).map(MicroOrganism::getDisplayName)
                        .orElse("");
                String agentCode = antibiotics.get(conflict.first().antibioticId).map(MicroAntibiotic::getWhonetCode)
                        .orElse("");
                conflictErrors.add("Line " + lineNumber + ": " + organismName + " would match conflicting " + agentCode
                        + " rules for groups " + conflict.first().organismGroup + " and "
                        + conflict.second().organismGroup);
            }
            if (!conflictErrors.isEmpty()) {
                throw new IllegalArgumentException(String.join("; ", conflictErrors));
            }
            panelDefaults.assignDefaults(Map.of());

            List<LoadedRow<String>> results = new ArrayList<>();
            for (MembershipRow membership : memberships) {
                results.add(membership.created() ? LoadedRow.created(membership.organism().getId())
                        : LoadedRow.unchanged("Membership unchanged"));
            }
            return results;
        });
        for (int i = 0; i < outcomes.size(); i++) {
            summary.record(outcomes.get(i), getClass().getSimpleName(), rows.get(i).lineNumber());
        }
        removals.forEach(removal -> summary.removed(getClass().getSimpleName(), removal));
    }

    private MicroOrganism organism(CsvRow row) {
        if (!row.isBlank("organism_source") && !row.isBlank("organism_code")) {
            String source = row.get("organism_source");
            String code = row.get("organism_code");
            return organisms.findByReference(source, code).orElseThrow(() -> new IllegalArgumentException(
                    "Line " + row.lineNumber() + ": unknown organism " + row.get("organism")));
        }
        String name = required(row, "organism");
        MicroOrganism organism = resolver.resolveOrganism(name, getDomainName());
        if (organism == null) {
            throw new IllegalArgumentException("Line " + row.lineNumber() + ": unknown organism " + name);
        }
        return organism;
    }

    private static String required(CsvRow row, String column) {
        if (row.isBlank(column)) {
            throw new IllegalArgumentException("Line " + row.lineNumber() + ": " + column + " is required");
        }
        return row.get(column).trim();
    }

    private record MembershipRow(MicroOrganism organism, boolean created) {
    }
}
