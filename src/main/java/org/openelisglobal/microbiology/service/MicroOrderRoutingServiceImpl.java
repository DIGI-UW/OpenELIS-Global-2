package org.openelisglobal.microbiology.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCulturePurpose;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroOrderRoutingServiceImpl implements MicroOrderRoutingService {

    private final MicroCaseService caseService;
    private final MicrobiologyReferenceService referenceService;
    private final MicroCaseOrderDetailService orderDetailService;
    private final MicroCaseAnalysisService caseAnalysisService;
    private final TestMethodService testMethodService;
    private final String defaultWorkflow;

    public MicroOrderRoutingServiceImpl(MicroCaseService caseService, MicrobiologyReferenceService referenceService,
            MicroCaseOrderDetailService orderDetailService, MicroCaseAnalysisService caseAnalysisService,
            TestMethodService testMethodService,
            @Value("${org.openelisglobal.microbiology.defaultWorkflow:}") String defaultWorkflow) {
        this.caseService = caseService;
        this.referenceService = referenceService;
        this.orderDetailService = orderDetailService;
        this.caseAnalysisService = caseAnalysisService;
        this.testMethodService = testMethodService;
        this.defaultWorkflow = defaultWorkflow == null ? "" : defaultWorkflow.trim();
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
        return routeAnalysesForSampleItem(sampleItem, analyses, performedBy, orderDetail, false);
    }

    @Override
    @Transactional
    public List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses,
            String performedBy, MicroCaseOrderDetailRequestForm orderDetail, boolean microbiologyProgramSelected) {
        if (sampleItem == null || sampleItem.getId() == null || analyses == null || analyses.isEmpty()) {
            return List.of();
        }

        MicroCaseOrderDetailRequestForm effectiveOrderDetail = orderDetail;
        if (effectiveOrderDetail == null && sampleItem.getSample() != null && sampleItem.getSample().getId() != null) {
            effectiveOrderDetail = orderDetailService.getOrderDraft(sampleItem.getSample().getId());
        }

        Map<MicroWorkflowType, List<Test>> testsByWorkflow = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            Test test = analysis == null ? null : analysis.getTest();
            MicroWorkflowType workflowType = workflowTypeFor(test);
            if (workflowType != null) {
                testsByWorkflow.computeIfAbsent(workflowType, ignored -> new ArrayList<>()).add(test);
            }
        }
        if (testsByWorkflow.isEmpty() && microbiologyProgramSelected) {
            testsByWorkflow.put(fallbackWorkflowType(), analyses.stream().filter(java.util.Objects::nonNull)
                    .map(Analysis::getTest).filter(java.util.Objects::nonNull).toList());
        }

        validateOrderDetail(effectiveOrderDetail, sampleItem);
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
            if (effectiveOrderDetail != null) {
                orderDetailService.saveOrderDetail(routedCase.getId(), effectiveOrderDetail, performedBy);
            }
        }
        return routedCases;
    }

    private MicroWorkflowType fallbackWorkflowType() {
        if (defaultWorkflow.isEmpty()) {
            return MicroWorkflowType.UNASSIGNED;
        }
        try {
            MicroWorkflowType configured = MicroWorkflowType.valueOf(defaultWorkflow.toUpperCase());
            if (configured == MicroWorkflowType.UNASSIGNED || configured == MicroWorkflowType.MYCOLOGY) {
                throw new IllegalStateException("Unsupported default microbiology workflow: " + defaultWorkflow);
            }
            return configured;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unsupported default microbiology workflow: " + defaultWorkflow, e);
        }
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

    private void validateOrderDetail(MicroCaseOrderDetailRequestForm orderDetail, SampleItem sampleItem) {
        if (orderDetail == null) {
            return;
        }
        if (orderDetail.culturePurpose == null || orderDetail.culturePurpose.isBlank()) {
            throw new IllegalArgumentException("Culture purpose is required for a new microbiology order");
        }
        try {
            MicroCulturePurpose.valueOf(orderDetail.culturePurpose.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported culture purpose", exception);
        }
        if (orderDetail.numberOfSets != null && (orderDetail.numberOfSets < 1 || orderDetail.numberOfSets > 10)) {
            throw new IllegalArgumentException("Number of culture sets must be between 1 and 10");
        }
        if (orderDetail.clinicalHistory != null && orderDetail.clinicalHistory.length() > 1000) {
            throw new IllegalArgumentException("Clinical history must be 1000 characters or fewer");
        }
        if (orderDetail.admissionDate != null && !orderDetail.admissionDate.isBlank()
                && !"OUTPATIENT".equalsIgnoreCase(orderDetail.patientOrigin)
                && sampleItem.getCollectionDate() != null) {
            LocalDate admissionDate;
            try {
                admissionDate = LocalDate.parse(orderDetail.admissionDate);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("Admission date must be a valid ISO date", exception);
            }
            LocalDate collectionDate = sampleItem.getCollectionDate().toLocalDateTime().toLocalDate();
            if (collectionDate.isBefore(admissionDate)) {
                throw new IllegalArgumentException("Collection date cannot be before admission date");
            }
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
