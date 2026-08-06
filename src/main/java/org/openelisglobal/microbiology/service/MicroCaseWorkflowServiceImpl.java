package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseWorkflowServiceImpl implements MicroCaseWorkflowService {

    private static final Set<MicroWorkflowType> CLASSIFIABLE_WORKFLOWS = EnumSet.of(MicroWorkflowType.BACTERIOLOGY,
            MicroWorkflowType.MYCOBACTERIOLOGY_TB);

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicrobiologyReferenceService referenceService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroCaseWorkflowServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            MicroIsolateDAO isolateDAO, MicrobiologyReferenceService referenceService) {
        this(caseDAO, activityDAO, isolateDAO, referenceService, new ObjectMapper());
    }

    MicroCaseWorkflowServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroIsolateDAO isolateDAO,
            MicrobiologyReferenceService referenceService, ObjectMapper objectMapper) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.isolateDAO = isolateDAO;
        this.referenceService = referenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MicroCase changeWorkflow(String caseId, MicroWorkflowType workflowType, String cultureMethodId,
            String reason, boolean preserveExistingWorkConfirmed, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(cultureMethodId, "cultureMethodId");
        MicroCaseServiceImpl.requireText(reason, "reason");
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        if (workflowType == null || !CLASSIFIABLE_WORKFLOWS.contains(workflowType)) {
            throw new IllegalArgumentException("MICROBIOLOGY_WORKFLOW_NOT_SUPPORTED");
        }

        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
        String previousWorkflow = microCase.getWorkflowType();
        String previousMethodId = microCase.getCultureMethodId();
        if (workflowType.name().equals(previousWorkflow) && cultureMethodId.equals(previousMethodId)) {
            throw new IllegalArgumentException("MICROBIOLOGY_WORKFLOW_UNCHANGED");
        }
        if (referenceService.getActiveCultureSetupForMethod(cultureMethodId, workflowType) == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_WORKFLOW_METHOD_INCOMPATIBLE");
        }

        MicroCase sibling = caseDAO.getBySampleItemAndWorkflow(microCase.getSampleItemId(), workflowType.name());
        if (sibling != null && !microCase.getId().equals(sibling.getId())) {
            throw new MicroCaseWorkflowConflictException("MICROBIOLOGY_WORKFLOW_SIBLING_EXISTS");
        }
        boolean existingWork = hasExistingWork(microCase);
        if (existingWork && !preserveExistingWorkConfirmed) {
            throw new MicroCaseWorkflowConflictException("MICROBIOLOGY_WORKFLOW_CHANGE_CONFIRMATION_REQUIRED");
        }

        microCase.setWorkflowType(workflowType.name());
        microCase.setCultureMethodId(cultureMethodId);
        MicroCase updated = caseDAO.update(microCase);

        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(MicroCaseActivityType.WORKFLOW_CHANGED.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(reason.trim());
        activity.setStructuredData(toStructuredData(previousWorkflow, workflowType.name(), previousMethodId,
                cultureMethodId, existingWork));
        activityDAO.insert(activity);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean requiresPreservationConfirmation(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        return hasExistingWork(microCase);
    }

    private boolean hasExistingWork(MicroCase microCase) {
        return !MicroCaseStage.RECEIVED.name().equals(microCase.getStage())
                || !isolateDAO.getByCaseId(microCase.getId()).isEmpty();
    }

    private String toStructuredData(String fromWorkflow, String toWorkflow, String fromMethodId, String toMethodId,
            boolean preservedExistingWork) {
        try {
            return objectMapper.writeValueAsString(Map.of("fromWorkflow", valueOrEmpty(fromWorkflow), "toWorkflow",
                    toWorkflow, "fromMethodId", valueOrEmpty(fromMethodId), "toMethodId", toMethodId,
                    "preservedExistingWork", preservedExistingWork));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("MICROBIOLOGY_WORKFLOW_AUDIT_SERIALIZATION_FAILED", exception);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
