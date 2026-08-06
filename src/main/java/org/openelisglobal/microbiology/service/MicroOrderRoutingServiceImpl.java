package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
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
    private final MicroCaseOrderDetailService orderDetailService;
    private final MicroCaseAnalysisService caseAnalysisService;
    private final TestMethodService testMethodService;

    public MicroOrderRoutingServiceImpl(MicroCaseService caseService, MicrobiologyReferenceService referenceService,
            MicroCaseOrderDetailService orderDetailService, MicroCaseAnalysisService caseAnalysisService,
            TestMethodService testMethodService) {
        this.caseService = caseService;
        this.referenceService = referenceService;
        this.orderDetailService = orderDetailService;
        this.caseAnalysisService = caseAnalysisService;
        this.testMethodService = testMethodService;
    }

    @Override
    @Transactional
    public List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses,
            String performedBy) {
        return routeAnalysesForSampleItem(sampleItem, analyses, performedBy, null);
    }

    @Override
    @Transactional
    public List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses,
            String performedBy, MicroCaseOrderDetailRequestForm orderDetail) {
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

        validateSelectedMethod(orderDetail, testsByWorkflow);
        Map<MicroWorkflowType, RoutingConfiguration> configurationsByWorkflow = new LinkedHashMap<>();
        for (Map.Entry<MicroWorkflowType, List<Test>> entry : testsByWorkflow.entrySet()) {
            MicroWorkflowType workflowType = entry.getKey();
            String methodId = methodIdFor(entry.getValue(), orderDetail);
            MicroCultureSetup setup = referenceService.getActiveCultureSetupForMethod(methodId, workflowType);
            if (setup == null) {
                throw new IllegalStateException("No active microbiology culture setup for method " + methodId
                        + " and workflow " + workflowType.name());
            }
            configurationsByWorkflow.put(workflowType, new RoutingConfiguration(methodId, setup));
        }

        List<MicroCase> routedCases = new ArrayList<>();
        for (Map.Entry<MicroWorkflowType, RoutingConfiguration> entry : configurationsByWorkflow.entrySet()) {
            RoutingConfiguration configuration = entry.getValue();
            MicroCase routedCase = caseService.createOrGetCase(sampleItem.getId(), entry.getKey(),
                    configuration.methodId(), performedBy);
            routedCases.add(routedCase);
            linkPersistedAnalyses(routedCase, entry.getKey(), configuration.cultureSetup(), analyses);
            if (orderDetail != null) {
                orderDetailService.saveOrderDetail(routedCase.getId(), orderDetail, performedBy);
            }
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

    private void validateSelectedMethod(MicroCaseOrderDetailRequestForm orderDetail,
            Map<MicroWorkflowType, List<Test>> testsByWorkflow) {
        if (orderDetail == null || orderDetail.cultureMethodId == null
                || orderDetail.cultureMethodId.trim().isEmpty()) {
            return;
        }
        boolean linked = testsByWorkflow.values().stream().flatMap(List::stream)
                .anyMatch(test -> testMethodService.testMethodLinkExists(test.getId(), orderDetail.cultureMethodId));
        if (!linked) {
            throw new IllegalArgumentException("Selected culture method is not linked to an ordered culture test");
        }
    }

    private String methodIdFor(List<Test> tests, MicroCaseOrderDetailRequestForm orderDetail) {
        if (orderDetail != null && orderDetail.cultureMethodId != null
                && !orderDetail.cultureMethodId.trim().isEmpty()) {
            boolean linkedToWorkflow = tests.stream().anyMatch(
                    test -> testMethodService.testMethodLinkExists(test.getId(), orderDetail.cultureMethodId));
            if (linkedToWorkflow) {
                return orderDetail.cultureMethodId;
            }
        }
        Test test = tests.get(0);
        Method method = test.getMethod();
        if (method == null || method.getId() == null || method.getId().trim().isEmpty()) {
            throw new IllegalStateException("Microbiology workflow tests require a culture method");
        }
        return method.getId();
    }

    private void linkPersistedAnalyses(MicroCase microCase, MicroWorkflowType workflowType,
            MicroCultureSetup cultureSetup, List<Analysis> analyses) {
        for (Analysis analysis : analyses) {
            Test test = analysis == null ? null : analysis.getTest();
            if (workflowType != workflowTypeFor(test) || analysis.getId() == null
                    || analysis.getId().trim().isEmpty()) {
                continue;
            }
            caseAnalysisService.linkAnalysis(microCase, analysis, cultureSetup);
        }
    }

    private record RoutingConfiguration(String methodId, MicroCultureSetup cultureSetup) {
    }
}
