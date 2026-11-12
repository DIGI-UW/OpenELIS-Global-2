package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroInterpretationRuleDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroReportingRuleDAO;
import org.openelisglobal.microbiology.dao.MicroReportingRuleHistoryDAO;
import org.openelisglobal.microbiology.form.MicroReportingRuleForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleHistoryForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleSettingsForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroInterpretationRule;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroReportingRule;
import org.openelisglobal.microbiology.valueholder.MicroReportingRuleHistory;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroReportingRuleService {
    static final Map<String, String> REPORT_DEFAULTS = Map.of("REPORT", "ALWAYS", "REPORT_IF_R",
            "SUPPRESS_UNLESS_RESISTANT", "CASCADE", "CASCADE", "DONT_REPORT", "DONT_REPORT");
    private static final Set<String> REPORT_BEHAVIORS = Set.of("ALWAYS", "SUPPRESS_UNLESS_RESISTANT", "CASCADE",
            "DONT_REPORT");
    private static final Set<String> NOTE_WHENS = Set.of("ALWAYS", "WHEN_R", "WHEN_S");
    private static final Set<String> STANDARDS = Set.of("CLSI", "EUCAST", "LOCAL");
    private static final String SETTING = "microbiologyReportingRulesStandard";
    private static final String EXPORT_HEADER = "organism_level,organism,specimen_group,agent,agent_name,"
            + "report_default,cascade_when,trigger_agents,report_as,always_report_R,comment,comment_when,"
            + "sop_section,note,indication,cascade_condition,add_automatically,active";

    private final MicroReportingRuleDAO rules;
    private final MicroReportingRuleHistoryDAO history;
    private final MicroAntibioticDAO antibiotics;
    private final MicroOrganismDAO organisms;
    private final MicroInterpretationRuleDAO interpretationRules;
    private final TypeOfSampleService sampleTypes;
    private final SystemUserService systemUsers;
    private final MicroBreakpointService breakpointService;
    private final SiteInformationService settings;
    private final MicroAstService astService;
    private final ObjectMapper json = new ObjectMapper();

    public MicroReportingRuleService(MicroReportingRuleDAO rules, MicroReportingRuleHistoryDAO history,
            MicroAntibioticDAO antibiotics, MicroOrganismDAO organisms, MicroInterpretationRuleDAO interpretationRules,
            TypeOfSampleService sampleTypes, SystemUserService systemUsers, MicroBreakpointService breakpointService,
            SiteInformationService settings, MicroAstService astService) {
        this.rules = rules;
        this.history = history;
        this.antibiotics = antibiotics;
        this.organisms = organisms;
        this.interpretationRules = interpretationRules;
        this.sampleTypes = sampleTypes;
        this.systemUsers = systemUsers;
        this.breakpointService = breakpointService;
        this.settings = settings;
        this.astService = astService;
    }

    @Transactional
    public MicroReportingRule save(MicroReportingRuleForm form, String actor) {
        MicroCaseServiceImpl.requireText(actor, "actor");
        if (form == null) {
            throw new IllegalArgumentException("REPORTING_RULE_INVALID");
        }
        String antibioticId = trimToNull(form.antibioticId);
        if (antibioticId == null || antibiotics.get(antibioticId).isEmpty()) {
            throw new IllegalArgumentException("REPORTING_RULE_ANTIBIOTIC_INVALID");
        }
        String reportBehavior = trimToNull(form.reportBehavior);
        if (reportBehavior == null || !REPORT_BEHAVIORS.contains(reportBehavior)) {
            throw new IllegalArgumentException("REPORTING_RULE_BEHAVIOR_INVALID");
        }
        String note = trimToNull(form.note);
        String noteWhen = null;
        if (note != null) {
            if (note.length() > 1000) {
                throw new IllegalArgumentException("REPORTING_RULE_NOTE_TOO_LONG");
            }
            noteWhen = trimToNull(form.noteWhen);
            if (noteWhen == null || !NOTE_WHENS.contains(noteWhen)) {
                throw new IllegalArgumentException("REPORTING_RULE_NOTE_WHEN_INVALID");
            }
        }
        String organismId = trimToNull(form.organismId);
        String organismGroup = trimToNull(form.organismGroup);
        if (organismId != null && organismGroup != null) {
            throw new IllegalArgumentException("REPORTING_RULE_TARGET_INVALID");
        }
        String clinicalContext = MicroBreakpointClinicalContext.validate(form.clinicalContext);
        Set<String> sampleTypeIds = form.sampleTypeIds == null ? new HashSet<>()
                : form.sampleTypeIds.stream().map(this::trimToNull).filter(Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new));
        MicroReportingRule rule = form.id == null ? new MicroReportingRule()
                : rules.get(form.id).orElseThrow(() -> new IllegalArgumentException("REPORTING_RULE_NOT_FOUND"));
        String previousAntibioticId = rule.getAntibioticId();
        String before = form.id == null ? null : snapshot(rule);
        rule.setAntibioticId(antibioticId);
        rule.setOrganismId(organismId);
        rule.setOrganismGroup(organismGroup);
        rule.setClinicalContext(clinicalContext);
        rule.setReportBehavior(reportBehavior);
        boolean cascade = "CASCADE".equals(reportBehavior);
        rule.setCascadeCondition(cascade ? trimToNull(form.cascadeCondition) : null);
        rule.setNote(note);
        rule.setNoteWhen(noteWhen);
        rule.setSampleTypeIds(sampleTypeIds);
        rule.setAddAutomatically(cascade && form.addAutomatically);
        rule.setActive(form.active == null || form.active);
        rule.setSysUserId(actor);
        if (form.id == null) {
            rules.insert(rule);
        } else {
            rules.update(rule);
        }
        String after = snapshot(rule);
        if (after.equals(before)) {
            return rule;
        }
        List<MicroReportingRuleHistory> revisions = history.getAllMatchingOrdered("ruleId", rule.getId(), "revision",
                true);
        int revision = revisions.isEmpty() ? 1 : revisions.get(0).getRevision() + 1;
        MicroReportingRuleHistory entry = new MicroReportingRuleHistory();
        entry.setRuleId(rule.getId());
        entry.setRevision(revision);
        entry.setChangedAt(new Timestamp(System.currentTimeMillis()));
        entry.setChangedBy(actor);
        entry.setBeforeValue(before);
        entry.setAfterValue(after);
        entry.setSysUserId(actor);
        history.insert(entry);
        astService.reapplyReportingRules(antibioticId, actor);
        if (previousAntibioticId != null && !Objects.equals(previousAntibioticId, antibioticId)) {
            astService.reapplyReportingRules(previousAntibioticId, actor);
        }
        return rule;
    }

    public List<MicroReportingRuleForm> getRules() {
        return rules.getAll().stream().map(rule -> {
            MicroReportingRuleForm form = toForm(rule);
            antibiotics.get(rule.getAntibioticId())
                    .ifPresent(antibiotic -> form.antibioticName = antibiotic.getDisplayName());
            if (rule.getOrganismId() != null) {
                organisms.get(rule.getOrganismId())
                        .ifPresent(organism -> form.organismName = organism.getDisplayName());
            }
            List<String> sampleTypeNames = new ArrayList<>();
            for (String sampleTypeId : rule.getSampleTypeIds()) {
                TypeOfSample sampleType = sampleTypes.get(sampleTypeId);
                if (sampleType != null) {
                    sampleTypeNames.add(sampleType.getLocalizedName());
                }
            }
            form.sampleTypeNames = sampleTypeNames;
            return form;
        }).toList();
    }

    public String exportCsv() {
        List<MicroInterpretationRule> interpretations = interpretationRules.getAll();
        Map<String, MicroInterpretationRule> activeInterpretationRules = new LinkedHashMap<>();
        for (MicroInterpretationRule rule : interpretations) {
            if (rule.isActive()) {
                activeInterpretationRules.putIfAbsent(
                        pairKey(rule.getAntibioticId(), rule.getOrganismId(), rule.getOrganismGroup()), rule);
            }
        }
        StringBuilder csv = new StringBuilder(EXPORT_HEADER).append('\n');
        for (MicroReportingRuleForm rule : getRules()) {
            boolean species = rule.organismId != null;
            boolean group = rule.organismGroup != null;
            String organism = species ? rule.organismName : rule.organismGroup;
            String agentName = rule.antibioticName;
            String pair = species || group ? pairKey(rule.antibioticId, rule.organismId, rule.organismGroup) : null;
            boolean alwaysResistant = pair != null && activeInterpretationRules.remove(pair) != null;
            csv.append(CSVFormat.DEFAULT.format(species ? "SPECIES" : group ? "GROUP" : "ANY", organism,
                    rule.sampleTypeNames.isEmpty() ? "ANY" : String.join(";", rule.sampleTypeNames),
                    whonetCode(rule.antibioticId), agentName, reportDefault(rule.reportBehavior), "", "", "",
                    alwaysResistant ? "YES" : "", rule.note, rule.noteWhen, "", "", rule.clinicalContext,
                    rule.cascadeCondition, rule.addAutomatically ? "YES" : "",
                    Boolean.TRUE.equals(rule.active) ? "" : "NO")).append('\n');
        }
        for (MicroInterpretationRule rule : activeInterpretationRules.values()) {
            String organism = rule.getOrganismId() != null
                    ? organisms.get(rule.getOrganismId()).map(MicroOrganism::getDisplayName).orElse("")
                    : rule.getOrganismGroup();
            String agentName = antibiotics.get(rule.getAntibioticId()).map(MicroAntibiotic::getDisplayName).orElse("");
            csv.append(CSVFormat.DEFAULT.format(rule.getOrganismId() != null ? "SPECIES" : "GROUP", organism, "ANY",
                    whonetCode(rule.getAntibioticId()), agentName, "", "", "", "", "YES", "", "", "", "", "", "", "",
                    "")).append('\n');
        }
        return csv.toString();
    }

    public List<MicroReportingRuleHistoryForm> getHistory(String ruleId) {
        Map<String, String> userDisplayById = new java.util.HashMap<>();
        return history.getAllMatchingOrdered("ruleId", ruleId, "revision", true).stream().map(entry -> {
            MicroReportingRuleHistoryForm form = new MicroReportingRuleHistoryForm();
            form.revision = entry.getRevision();
            form.changedAt = entry.getChangedAt();
            form.changedBy = entry.getChangedBy();
            form.changedByName = MicrobiologyUserDisplayResolver.resolve(systemUsers, entry.getChangedBy(),
                    userDisplayById);
            form.before = entry.getBeforeValue();
            form.after = entry.getAfterValue();
            return form;
        }).toList();
    }

    public MicroReportingRuleSettingsForm getSettings() {
        MicroReportingRuleSettingsForm form = new MicroReportingRuleSettingsForm();
        form.writtenFor = trimToNull(setting(SETTING));
        form.activeAuthority = breakpointService.getDefaultStandard().authority;
        return form;
    }

    @Transactional
    public MicroReportingRuleSettingsForm saveSettings(String writtenFor, String actor) {
        MicroCaseServiceImpl.requireText(actor, "actor");
        String normalized = trimToNull(writtenFor);
        if (normalized != null && !STANDARDS.contains(normalized)) {
            throw new IllegalArgumentException("REPORTING_RULES_STANDARD_INVALID");
        }
        saveSetting(SETTING, normalized == null ? "" : normalized, actor);
        return getSettings();
    }

    private String setting(String name) {
        SiteInformation value = settings.getSiteInformationByName(name);
        return value == null || value.getValue() == null ? "" : value.getValue().trim();
    }

    private void saveSetting(String name, String value, String actorId) {
        SiteInformation setting = settings.getSiteInformationByName(name);
        boolean isNew = setting == null;
        if (isNew) {
            setting = new SiteInformation();
            setting.setName(name);
            setting.setValueType("text");
        }
        setting.setValue(value);
        setting.setSysUserId(actorId);
        settings.persistData(setting, isNew);
    }

    public MicroReportingRuleForm toForm(MicroReportingRule rule) {
        MicroReportingRuleForm form = new MicroReportingRuleForm();
        form.id = rule.getId();
        form.antibioticId = rule.getAntibioticId();
        form.organismId = rule.getOrganismId();
        form.organismGroup = rule.getOrganismGroup();
        form.clinicalContext = rule.getClinicalContext();
        form.reportBehavior = rule.getReportBehavior();
        form.cascadeCondition = rule.getCascadeCondition();
        form.note = rule.getNote();
        form.noteWhen = rule.getNoteWhen();
        form.sampleTypeIds = new ArrayList<>(new TreeSet<>(rule.getSampleTypeIds()));
        form.addAutomatically = rule.isAddAutomatically();
        form.active = rule.isActive();
        return form;
    }

    private static String pairKey(String antibioticId, String organismId, String organismGroup) {
        return organismId != null ? "species|" + antibioticId + "|" + organismId
                : "group|" + antibioticId + "|" + Objects.toString(organismGroup, "");
    }

    private String whonetCode(String antibioticId) {
        return antibiotics.get(antibioticId).map(MicroAntibiotic::getWhonetCode).orElse("");
    }

    private static String reportDefault(String reportBehavior) {
        if (reportBehavior == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : REPORT_DEFAULTS.entrySet()) {
            if (entry.getValue().equals(reportBehavior)) {
                return entry.getKey();
            }
        }
        return "";
    }

    private String snapshot(MicroReportingRule rule) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("antibioticId", rule.getAntibioticId());
        values.put("organismId", rule.getOrganismId());
        values.put("organismGroup", rule.getOrganismGroup());
        values.put("sampleTypeIds", new TreeSet<>(rule.getSampleTypeIds()));
        values.put("clinicalContext", rule.getClinicalContext());
        values.put("reportBehavior", rule.getReportBehavior());
        values.put("cascadeCondition", rule.getCascadeCondition());
        values.put("note", rule.getNote());
        values.put("noteWhen", rule.getNoteWhen());
        values.put("addAutomatically", rule.isAddAutomatically());
        values.put("active", rule.isActive());
        try {
            return json.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("REPORTING_RULE_INVALID");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
