package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.ImportRunContext;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroInterpretationRuleForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.stereotype.Component;

@Component
public class MicroReportingRuleConfigurationHandler extends AbstractCatalogCsvHandler {
    private static final String ALWAYS_RESISTANT_SOURCE = "Configuration file";

    private final MicroReportingRuleService reportingRules;
    private final MicroInterpretationRuleService interpretationRules;
    private final MicroOrganismDAO organisms;
    private final MicroAntibioticDAO antibiotics;
    private final CatalogReferenceResolver resolver;

    public MicroReportingRuleConfigurationHandler(MicroReportingRuleService reportingRules,
            MicroInterpretationRuleService interpretationRules, MicroOrganismDAO organisms,
            MicroAntibioticDAO antibiotics, CatalogReferenceResolver resolver) {
        this.reportingRules = reportingRules;
        this.interpretationRules = interpretationRules;
        this.organisms = organisms;
        this.antibiotics = antibiotics;
        this.resolver = resolver;
    }

    @Override
    public String getDomainName() {
        return "microbiology-reporting-rules";
    }

    @Override
    public int getLoadOrder() {
        return 350;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "organism_level", "organism", "specimen_group", "agent", "report_default" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        for (CsvRow row : rows) {
            LoadedRow<String> outcome = transaction.run(() -> loadRow(row));
            summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
        }
    }

    private LoadedRow<String> loadRow(CsvRow row) {
        String level = required(row, "organism_level").toUpperCase(Locale.ROOT);
        MicroOrganism organism = null;
        String organismGroup = null;
        switch (level) {
        case "SPECIES" -> organism = species(row);
        case "GROUP" -> organismGroup = required(row, "organism");
        case "ANY" -> {
        }
        default -> throw new IllegalArgumentException("Line " + row.lineNumber()
                + ": organism_level must be SPECIES, GROUP or ANY, was '" + row.get("organism_level") + "'");
        }
        String organismId = organism == null ? null : organism.getId();
        Set<String> sampleTypeIds = sampleTypeIds(row);
        String code = required(row, "agent");
        MicroAntibiotic antibiotic = antibiotics.findByWhonetCodeIgnoreCase(code).orElseThrow(
                () -> new IllegalArgumentException("Line " + row.lineNumber() + ": unknown antibiotic " + code));
        String behavior = reportBehavior(row);
        String note = blankToNull(row.get("comment"));
        String noteWhen = note == null ? null : noteWhen(row);
        String clinicalContext = MicroBreakpointClinicalContext.validate(blankToNull(row.get("indication")));
        String cascadeCondition = cascadeCondition(row, behavior);
        boolean alwaysResistant = "YES".equalsIgnoreCase(row.get("always_report_R"));
        if (alwaysResistant && organismId == null && organismGroup == null) {
            throw new IllegalArgumentException(
                    "Line " + row.lineNumber() + ": always_report_R needs a SPECIES or GROUP organism_level");
        }
        if (behavior == null && !alwaysResistant) {
            return LoadedRow.skipped("Nothing to load");
        }

        boolean created = false;
        boolean changed = false;
        String id = null;
        if (behavior != null) {
            MicroReportingRuleForm existing = existingReportingRule(antibiotic.getId(), organismId, organismGroup,
                    sampleTypeIds, clinicalContext);
            MicroReportingRuleForm form = new MicroReportingRuleForm();
            form.id = existing == null ? null : existing.id;
            form.antibioticId = antibiotic.getId();
            form.organismId = organismId;
            form.organismGroup = organismGroup;
            form.sampleTypeIds = new ArrayList<>(sampleTypeIds);
            form.clinicalContext = clinicalContext;
            form.reportBehavior = behavior;
            form.cascadeCondition = cascadeCondition;
            form.note = note;
            form.noteWhen = noteWhen;
            form.addAutomatically = row.has("add_automatically") ? "YES".equalsIgnoreCase(row.get("add_automatically"))
                    : existing != null && existing.addAutomatically;
            form.active = true;
            created |= existing == null;
            changed |= existing != null && !sameReportingRule(existing, form);
            id = reportingRules.save(form, SYS_USER_ID).getId();
        }
        if (alwaysResistant) {
            MicroInterpretationRuleForm existing = existingInterpretationRule(antibiotic.getId(), organismId,
                    organismGroup);
            MicroInterpretationRuleForm form = new MicroInterpretationRuleForm();
            form.id = existing == null ? null : existing.id;
            form.antibioticId = antibiotic.getId();
            form.organismId = organismId;
            form.organismGroup = organismGroup;
            form.interpretation = "RESISTANT";
            form.source = ALWAYS_RESISTANT_SOURCE;
            form.active = true;
            created |= existing == null;
            changed |= existing != null && !sameInterpretationRule(existing, form);
            String savedId = interpretationRules.save(form, SYS_USER_ID).getId();
            id = id == null ? savedId : id;
        }
        if (created) {
            return LoadedRow.created(id);
        }
        return changed ? LoadedRow.updated(id) : LoadedRow.skipped("Rule unchanged");
    }

    private MicroOrganism species(CsvRow row) {
        if (row.has("organism_source") && row.has("organism_code")) {
            String source = row.get("organism_source");
            String code = row.get("organism_code");
            if (!source.isEmpty() && !code.isEmpty()) {
                return organisms.findByReference(source, code).orElseThrow(() -> new IllegalArgumentException(
                        "Line " + row.lineNumber() + ": unknown organism " + row.get("organism")));
            }
        }
        String name = required(row, "organism");
        return organisms.findByDisplayNameIgnoreCase(name).orElseThrow(
                () -> new IllegalArgumentException("Line " + row.lineNumber() + ": unknown organism " + name));
    }

    private Set<String> sampleTypeIds(CsvRow row) {
        Set<String> ids = new HashSet<>();
        String value = row.get("specimen_group");
        if (value.isEmpty() || "ANY".equalsIgnoreCase(value)) {
            return ids;
        }
        for (String name : value.split("\\s*;\\s*")) {
            if (name.isEmpty()) {
                continue;
            }
            TypeOfSample sampleType = resolver.resolveSampleType(name, getDomainName());
            if (sampleType == null) {
                ImportRunContext.drainPending();
                throw new IllegalArgumentException("Line " + row.lineNumber() + ": unknown sample type " + name);
            }
            ids.add(sampleType.getId());
        }
        return ids;
    }

    private static String reportBehavior(CsvRow row) {
        String value = row.get("report_default");
        if (value.isEmpty()) {
            return null;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
        case "REPORT" -> "ALWAYS";
        case "REPORT_IF_R" -> "SUPPRESS_UNLESS_RESISTANT";
        case "CASCADE" -> "CASCADE";
        case "DONT_REPORT" -> "DONT_REPORT";
        default -> throw new IllegalArgumentException("Line " + row.lineNumber()
                + ": report_default must be REPORT, REPORT_IF_R, CASCADE or DONT_REPORT, was '" + value + "'");
        };
    }

    private static String noteWhen(CsvRow row) {
        String value = row.get("comment_when");
        if (value.isEmpty()) {
            return "ALWAYS";
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!Set.of("ALWAYS", "WHEN_R", "WHEN_S").contains(normalized)) {
            throw new IllegalArgumentException("Line " + row.lineNumber()
                    + ": comment_when must be ALWAYS, WHEN_R or WHEN_S, was '" + value + "'");
        }
        return normalized;
    }

    private static String cascadeCondition(CsvRow row, String behavior) {
        if (!"CASCADE".equals(behavior)) {
            return null;
        }
        String condition = blankToNull(row.get("cascade_condition"));
        if (condition == null && !row.isBlank("cascade_when")) {
            condition = (row.get("cascade_when") + " " + row.get("trigger_agents")).trim();
        }
        return condition;
    }

    private MicroReportingRuleForm existingReportingRule(String antibioticId, String organismId, String organismGroup,
            Set<String> sampleTypeIds, String clinicalContext) {
        for (MicroReportingRuleForm rule : reportingRules.getRules()) {
            if (antibioticId.equals(rule.antibioticId) && Objects.equals(organismId, rule.organismId)
                    && Objects.equals(organismGroup, rule.organismGroup)
                    && Objects.equals(clinicalContext, rule.clinicalContext)
                    && new HashSet<>(rule.sampleTypeIds).equals(sampleTypeIds)) {
                return rule;
            }
        }
        return null;
    }

    private MicroInterpretationRuleForm existingInterpretationRule(String antibioticId, String organismId,
            String organismGroup) {
        for (MicroInterpretationRuleForm rule : interpretationRules.getRules()) {
            if (antibioticId.equals(rule.antibioticId) && "RESISTANT".equals(rule.interpretation)
                    && Objects.equals(organismId, rule.organismId)
                    && Objects.equals(organismGroup, rule.organismGroup)) {
                return rule;
            }
        }
        return null;
    }

    private static boolean sameReportingRule(MicroReportingRuleForm existing, MicroReportingRuleForm wanted) {
        return Boolean.TRUE.equals(existing.active) && Objects.equals(existing.reportBehavior, wanted.reportBehavior)
                && Objects.equals(existing.cascadeCondition, wanted.cascadeCondition)
                && Objects.equals(existing.note, wanted.note) && Objects.equals(existing.noteWhen, wanted.noteWhen)
                && Objects.equals(existing.clinicalContext, wanted.clinicalContext)
                && new HashSet<>(existing.sampleTypeIds).equals(new HashSet<>(wanted.sampleTypeIds))
                && existing.addAutomatically == wanted.addAutomatically;
    }

    private static boolean sameInterpretationRule(MicroInterpretationRuleForm existing,
            MicroInterpretationRuleForm wanted) {
        return Boolean.TRUE.equals(existing.active) && Objects.equals(existing.source, wanted.source);
    }

    private static String required(CsvRow row, String column) {
        if (row.isBlank(column)) {
            throw new IllegalArgumentException("Line " + row.lineNumber() + ": " + column + " is required");
        }
        return row.get(column).trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
