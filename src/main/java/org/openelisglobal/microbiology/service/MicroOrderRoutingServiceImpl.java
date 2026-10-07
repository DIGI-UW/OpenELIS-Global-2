package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroOrderRoutingServiceImpl implements MicroOrderRoutingService {

    private final MicroCaseService caseService;
    private final MicrobiologyReferenceService referenceService;
    private final MicroCaseAnalysisService caseAnalysisService;
    private final TestMethodService testMethodService;

    public MicroOrderRoutingServiceImpl(MicroCaseService caseService, MicrobiologyReferenceService referenceService,
            MicroCaseAnalysisService caseAnalysisService, TestMethodService testMethodService) {
        this.caseService = caseService;
        this.referenceService = referenceService;
        this.caseAnalysisService = caseAnalysisService;
        this.testMethodService = testMethodService;
    }

    @Override
    @Transactional
    public List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses,
            String performedBy) {
        if (sampleItem == null || sampleItem.getId() == null || analyses == null || analyses.isEmpty()) {
            return List.of();
        }

        Map<MicroWorkflowType, List<Test>> testsByWorkflow = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            Test test = analysis == null ? null : analysis.getTest();
            MicroWorkflowType workflowType = workflowTypeFor(test);
            if (workflowType != null) {
                testsByWorkflow.computeIfAbsent(workflowType, ignored -> new ArrayList<>()).add(test);
            }
        }
        Map<MicroWorkflowType, RoutingConfiguration> configurationsByWorkflow = new LinkedHashMap<>();
        for (Map.Entry<MicroWorkflowType, List<Test>> entry : testsByWorkflow.entrySet()) {
            MicroWorkflowType workflowType = entry.getKey();
            String methodId = methodIdFor(entry.getValue());
            MicroCultureSetup setup = workflowType == MicroWorkflowType.UNASSIGNED || methodId == null ? null
                    : referenceService.getActiveCultureSetupForMethod(methodId, workflowType);
            if (setup == null && workflowType != MicroWorkflowType.UNASSIGNED && methodId != null) {
                throw new IllegalStateException("No active microbiology culture setup for method " + methodId
                        + " and workflow " + workflowType.name());
            }
            configurationsByWorkflow.put(workflowType, new RoutingConfiguration(methodId, setup, entry.getValue()));
        }

        List<MicroCase> routedCases = new ArrayList<>();
        for (Map.Entry<MicroWorkflowType, RoutingConfiguration> entry : configurationsByWorkflow.entrySet()) {
            RoutingConfiguration configuration = entry.getValue();
            MicroCase routedCase = caseService.createOrGetCase(sampleItem.getId(), entry.getKey(),
                    configuration.methodId(), performedBy);
            routedCases.add(routedCase);
            linkPersistedAnalyses(routedCase, configuration.tests(), configuration.cultureSetup(), analyses);
        }
        return routedCases;
    }

    private MicroWorkflowType workflowTypeFor(Test test) {
        if (test == null || test.getCultureWorkflowType() == null || test.getCultureWorkflowType().trim().isEmpty()) {
            return null;
        }
        try {
            return MicroWorkflowType.valueOf(test.getCultureWorkflowType());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unsupported microbiology workflow type: " + test.getCultureWorkflowType(),
                    e);
        }
    }

    private String methodIdFor(List<Test> tests) {
        Test test = tests.get(0);
        String defaultMethodId = testMethodService.getDefaultMethodId(test.getId());
        if (defaultMethodId != null && !defaultMethodId.trim().isEmpty()) {
            return defaultMethodId;
        }
        Method legacyMethod = test.getMethod();
        return legacyMethod == null || legacyMethod.getId() == null || legacyMethod.getId().trim().isEmpty() ? null
                : legacyMethod.getId();
    }

    private void linkPersistedAnalyses(MicroCase microCase, List<Test> routedTests, MicroCultureSetup cultureSetup,
            List<Analysis> analyses) {
        List<String> routedTestIds = routedTests.stream().map(Test::getId).toList();
        for (Analysis analysis : analyses) {
            Test test = analysis == null ? null : analysis.getTest();
            if (test == null || !routedTestIds.contains(test.getId()) || analysis.getId() == null
                    || analysis.getId().trim().isEmpty()) {
                continue;
            }
            caseAnalysisService.linkAnalysis(microCase, analysis, cultureSetup);
        }
    }

    private record RoutingConfiguration(String methodId, MicroCultureSetup cultureSetup, List<Test> tests) {
    }
}
