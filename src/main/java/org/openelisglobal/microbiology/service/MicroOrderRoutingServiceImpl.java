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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroOrderRoutingServiceImpl implements MicroOrderRoutingService {

    private final MicroCaseService caseService;
    private final MicrobiologyReferenceService referenceService;

    public MicroOrderRoutingServiceImpl(MicroCaseService caseService, MicrobiologyReferenceService referenceService) {
        this.caseService = caseService;
        this.referenceService = referenceService;
    }

    @Override
    @Transactional
    public List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses,
            String performedBy) {
        if (sampleItem == null || sampleItem.getId() == null || analyses == null || analyses.isEmpty()) {
            return List.of();
        }

        Map<MicroWorkflowType, String> cultureMethodsByWorkflow = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            Test test = analysis == null ? null : analysis.getTest();
            MicroWorkflowType workflowType = workflowTypeFor(test);
            if (workflowType == null || cultureMethodsByWorkflow.containsKey(workflowType)) {
                continue;
            }
            String methodId = methodIdFor(test);
            MicroCultureSetup setup = referenceService.getActiveCultureSetupForMethod(methodId, workflowType);
            if (setup == null) {
                throw new IllegalStateException("No active microbiology culture setup for method " + methodId
                        + " and workflow " + workflowType.name());
            }
            cultureMethodsByWorkflow.put(workflowType, methodId);
        }

        List<MicroCase> routedCases = new ArrayList<>();
        for (Map.Entry<MicroWorkflowType, String> entry : cultureMethodsByWorkflow.entrySet()) {
            routedCases.add(
                    caseService.createOrGetCase(sampleItem.getId(), entry.getKey(), entry.getValue(), performedBy));
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

    private String methodIdFor(Test test) {
        Method method = test.getMethod();
        if (method == null || method.getId() == null || method.getId().trim().isEmpty()) {
            throw new IllegalStateException("Microbiology workflow tests require a culture method");
        }
        return method.getId();
    }
}
