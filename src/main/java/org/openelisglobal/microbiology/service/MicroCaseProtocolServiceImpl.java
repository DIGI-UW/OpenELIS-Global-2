package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseProtocolOptionForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseProtocolServiceImpl implements MicroCaseProtocolService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final AnalysisService analysisService;
    private final TestMethodService testMethodService;
    private final MethodService methodService;
    private final MicrobiologyReferenceService referenceService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroCaseProtocolServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            MicroCaseAnalysisDAO caseAnalysisDAO, AnalysisService analysisService, TestMethodService testMethodService,
            MethodService methodService, MicrobiologyReferenceService referenceService) {
        this(caseDAO, activityDAO, caseAnalysisDAO, analysisService, testMethodService, methodService, referenceService,
                new ObjectMapper());
    }

    MicroCaseProtocolServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            MicroCaseAnalysisDAO caseAnalysisDAO, AnalysisService analysisService, TestMethodService testMethodService,
            MethodService methodService, MicrobiologyReferenceService referenceService, ObjectMapper objectMapper) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.analysisService = analysisService;
        this.testMethodService = testMethodService;
        this.methodService = methodService;
        this.referenceService = referenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseProtocolOptionForm> getProtocolOptions(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCase microCase = requireCase(caseId);
        MicroWorkflowType workflowType = supportedWorkflow(microCase.getWorkflowType());
        Map<String, MicroCaseProtocolOptionForm> byMethodId = new LinkedHashMap<>();

        for (var link : caseAnalysisDAO.getByCaseId(caseId)) {
            Analysis analysis = analysisService.getAnalysisById(link.getAnalysisId());
            Test test = analysis == null ? null : analysisService.getTest(analysis);
            if (test == null || !workflowType.name().equals(test.getCultureWorkflowType())) {
                continue;
            }
            for (TestMethodDto linkedMethod : testMethodService.getLinkedMethodDtos(test.getId())) {
                Method method = methodService.findById(linkedMethod.methodId);
                MicroCultureSetup setup = referenceService.getActiveCultureSetupForMethod(linkedMethod.methodId,
                        workflowType);
                if (method != null && IActionConstants.YES.equals(method.getIsActive())) {
                    byMethodId.putIfAbsent(linkedMethod.methodId, toOption(linkedMethod.methodId, method, setup,
                            linkedMethod.methodId.equals(microCase.getCultureMethodId()), true));
                }
            }
        }

        String currentMethodId = microCase.getCultureMethodId();
        if (currentMethodId != null && !currentMethodId.isBlank() && !byMethodId.containsKey(currentMethodId)) {
            Method incumbent = methodService.findById(currentMethodId);
            MicroCultureSetup setup = referenceService.getActiveCultureSetupForMethod(currentMethodId, workflowType);
            byMethodId.put(currentMethodId, toOption(currentMethodId, incumbent, setup, true, false));
        }

        List<MicroCaseProtocolOptionForm> options = new ArrayList<>(byMethodId.values());
        options.sort(Comparator.comparing((MicroCaseProtocolOptionForm option) -> !option.current)
                .thenComparing(option -> option.label == null ? "" : option.label, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    @Override
    @Transactional
    public MicroCase changeProtocol(String caseId, String cultureMethodId, String reason, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(cultureMethodId, "cultureMethodId");
        MicroCaseServiceImpl.requireText(reason, "reason");
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        MicroCase microCase = requireCase(caseId);
        MicroCaseMutationGuard.requireMutable(microCase);
        String previousMethodId = microCase.getCultureMethodId();
        if (cultureMethodId.equals(previousMethodId)) {
            throw new IllegalArgumentException("MICROBIOLOGY_PROTOCOL_UNCHANGED");
        }

        List<MicroCaseProtocolOptionForm> options = getProtocolOptions(caseId);
        MicroCaseProtocolOptionForm next = options.stream()
                .filter(option -> cultureMethodId.equals(option.id) && option.active).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_PROTOCOL_METHOD_INCOMPATIBLE"));
        MicroCaseProtocolOptionForm previous = options.stream()
                .filter(option -> previousMethodId != null && previousMethodId.equals(option.id)).findFirst()
                .orElse(null);
        String previousLabel = previous == null ? "Not set" : previous.label;

        microCase.setCultureMethodId(cultureMethodId);
        MicroCase updated = caseDAO.update(microCase);

        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(MicroCaseActivityType.CULTURE_PROTOCOL_CHANGED.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote("Culture protocol changed from " + previousLabel + " to " + next.label + ": " + reason.trim());
        activity.setStructuredData(
                toStructuredData(previousMethodId, cultureMethodId, previousLabel, next.label, reason.trim()));
        activityDAO.insert(activity);
        return updated;
    }

    private MicroCase requireCase(String caseId) {
        return caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
    }

    private MicroWorkflowType supportedWorkflow(String workflowType) {
        MicroWorkflowType parsed;
        try {
            parsed = MicroWorkflowType.valueOf(workflowType);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("MICROBIOLOGY_PROTOCOL_WORKFLOW_REQUIRED", exception);
        }
        if (parsed != MicroWorkflowType.BACTERIOLOGY && parsed != MicroWorkflowType.MYCOBACTERIOLOGY_TB) {
            throw new IllegalArgumentException("MICROBIOLOGY_PROTOCOL_WORKFLOW_REQUIRED");
        }
        return parsed;
    }

    private MicroCaseProtocolOptionForm toOption(String methodId, Method method, MicroCultureSetup setup,
            boolean current, boolean active) {
        MicroCaseProtocolOptionForm option = new MicroCaseProtocolOptionForm();
        option.id = methodId;
        option.label = method == null ? methodId : method.getLocalizedValue();
        option.code = method == null ? null : method.getCode();
        option.active = active;
        option.current = current;
        if (setup != null) {
            option.mediaDefaults = setup.getMediaDefaults();
            option.incubationDefaults = setup.getIncubationDefaults();
            option.atmosphereDefaults = setup.getAtmosphereDefaults();
        }
        return option;
    }

    private String toStructuredData(String fromMethodId, String toMethodId, String fromMethodName, String toMethodName,
            String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of("fromMethodId", valueOrEmpty(fromMethodId), "toMethodId",
                    toMethodId, "fromMethodName", valueOrEmpty(fromMethodName), "toMethodName",
                    valueOrEmpty(toMethodName), "reason", reason));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("MICROBIOLOGY_PROTOCOL_AUDIT_SERIALIZATION_FAILED", exception);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
