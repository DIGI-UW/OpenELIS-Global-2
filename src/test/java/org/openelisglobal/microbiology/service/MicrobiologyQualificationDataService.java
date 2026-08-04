package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

/**
 * Test-support builder for repeatable qualification workloads through
 * application services.
 */
public class MicrobiologyQualificationDataService {

    private static final int MAX_WORKLIST_CASES = 500;
    private static final int DENSE_ISOLATE_COUNT = 5;
    private static final int READINGS_PER_ISOLATE = 16;
    private static final String UAT_PANEL_NAME = "Gram negative AST panel (UAT)";

    private final MicrobiologyUatScenarioService scenarioService;
    private final MicrobiologyReferenceService referenceService;
    private final MicroBreakpointService breakpointService;
    private final MicrobiologyConfigurationService configurationService;
    private final MicroIsolateService isolateService;
    private final MicroAstService astService;
    private final MicroCaseService caseService;
    private final boolean enabled;

    public MicrobiologyQualificationDataService(MicrobiologyUatScenarioService scenarioService,
            MicrobiologyReferenceService referenceService, MicroBreakpointService breakpointService,
            MicrobiologyConfigurationService configurationService, MicroIsolateService isolateService,
            MicroAstService astService, MicroCaseService caseService, boolean enabled) {
        this.scenarioService = scenarioService;
        this.referenceService = referenceService;
        this.breakpointService = breakpointService;
        this.configurationService = configurationService;
        this.isolateService = isolateService;
        this.astService = astService;
        this.caseService = caseService;
        this.enabled = enabled;
    }

    public WorklistDataset buildWorklist(String runKey, int caseCount, String performedBy) {
        requireEnabled();
        String normalizedRunKey = requireRunKey(runKey);
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        if (caseCount < 1 || caseCount > MAX_WORKLIST_CASES) {
            throw new IllegalArgumentException("caseCount must be between 1 and " + MAX_WORKLIST_CASES);
        }

        List<String> scenarioKeys = new ArrayList<>(caseCount);
        List<String> caseIds = new ArrayList<>(caseCount);
        for (int index = 1; index <= caseCount; index++) {
            String scenarioKey = String.format(Locale.ROOT, "qualification-%s-worklist-%03d", normalizedRunKey, index);
            MicrobiologyUatScenarioForm form = provision(scenarioKey, performedBy);
            scenarioKeys.add(scenarioKey);
            caseIds.add(form.caseId);
        }
        return new WorklistDataset(normalizedRunKey, List.copyOf(scenarioKeys), List.copyOf(caseIds));
    }

    public DenseCaseDataset buildDenseCase(String runKey, String performedBy) {
        requireEnabled();
        String normalizedRunKey = requireRunKey(runKey);
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        MicrobiologyUatScenarioForm scenario = provision("qualification-" + normalizedRunKey + "-dense", performedBy);
        MicroAstPanel panel = requireUatPanel();
        MicroBreakpointStandard standard = requireUatStandard();
        List<MicroAntibiotic> antibiotics = getOrCreateQualificationAntibiotics(panel, standard);

        List<String> isolateIds = new ArrayList<>(DENSE_ISOLATE_COUNT);
        int readingCount = 0;
        for (int isolateIndex = 1; isolateIndex <= DENSE_ISOLATE_COUNT; isolateIndex++) {
            MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "QISO-" + isolateIndex, null,
                    "Qualification organism " + isolateIndex, MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                    performedBy);
            isolateIds.add(isolate.getId());
            MicroAstRun run = astService.startRun(isolate.getId(), panel.getId(), standard.getId(), performedBy);
            for (int readingIndex = 0; readingIndex < READINGS_PER_ISOLATE; readingIndex++) {
                MicroAntibiotic antibiotic = antibiotics.get(readingIndex);
                BigDecimal value = readingIndex % 2 == 0 ? new BigDecimal("4") : new BigDecimal("32");
                astService.recordReading(run.getId(), antibiotic.getId(), MicroAstMethod.MIC, value, performedBy);
                readingCount++;
            }
        }

        MicroCaseDetailForm detail = caseService.getCaseDetail(scenario.caseId);
        int timelineEventCount = detail == null || detail.activities == null ? 0 : detail.activities.size();
        if (timelineEventCount < 30) {
            throw new IllegalStateException("DENSE_CASE_TIMELINE_UNDERSIZED");
        }
        return new DenseCaseDataset(normalizedRunKey, scenario.caseId, List.copyOf(isolateIds), readingCount,
                timelineEventCount);
    }

    private MicrobiologyUatScenarioForm provision(String scenarioKey, String performedBy) {
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "MVP";
        request.scenarioKey = scenarioKey;
        return scenarioService.provision(request, performedBy);
    }

    private MicroAstPanel requireUatPanel() {
        return referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
                .filter(panel -> UAT_PANEL_NAME.equals(panel.getName())).findFirst()
                .orElseThrow(() -> new IllegalStateException("QUALIFICATION_AST_PANEL_REQUIRED"));
    }

    private MicroBreakpointStandard requireUatStandard() {
        return breakpointService.getActiveStandards().stream()
                .filter(standard -> "CLSI".equals(standard.getAuthority()) && "2026".equals(standard.getVersion()))
                .findFirst().orElseThrow(() -> new IllegalStateException("QUALIFICATION_BREAKPOINT_STANDARD_REQUIRED"));
    }

    private List<MicroAntibiotic> getOrCreateQualificationAntibiotics(MicroAstPanel panel,
            MicroBreakpointStandard standard) {
        List<MicroAntibiotic> antibiotics = new ArrayList<>(READINGS_PER_ISOLATE);
        for (int index = 1; index <= READINGS_PER_ISOLATE; index++) {
            String code = String.format(Locale.ROOT, "QAST%02d", index);
            MicroAntibiotic antibiotic = configurationService.getOrCreateAntibiotic(
                    String.format(Locale.ROOT, "Qualification antibiotic %02d", index), code, "QUALIFICATION");
            configurationService.getOrCreatePanelAntibiotic(panel.getId(), antibiotic.getId(), index + 2);
            configurationService.getOrCreateBreakpointRule(micBreakpointRule(standard.getId(), antibiotic.getId()));
            antibiotics.add(antibiotic);
        }
        return antibiotics;
    }

    private MicroBreakpointRule micBreakpointRule(String standardId, String antibioticId) {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setStandardId(standardId);
        rule.setAntibioticId(antibioticId);
        rule.setMethod(MicroAstMethod.MIC.name());
        rule.setBreakpointType(MicroAstMethod.MIC.name());
        rule.setSusceptibleValue(new BigDecimal("8"));
        rule.setIntermediateLowerValue(new BigDecimal("16"));
        rule.setIntermediateUpperValue(new BigDecimal("16"));
        rule.setResistantValue(new BigDecimal("32"));
        return rule;
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new IllegalStateException("MICROBIOLOGY_QUALIFICATION_DISABLED");
        }
    }

    private String requireRunKey(String runKey) {
        MicroCaseServiceImpl.requireText(runKey, "runKey");
        String normalized = runKey.trim();
        if (normalized.length() > 64 || !normalized.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("runKey must use 1-64 letters, numbers, dots, underscores, or dashes");
        }
        return normalized;
    }

    public record WorklistDataset(String runKey, List<String> scenarioKeys, List<String> caseIds) {
    }

    public record DenseCaseDataset(String runKey, String caseId, List<String> isolateIds, int readingCount,
            int timelineEventCount) {
    }
}
