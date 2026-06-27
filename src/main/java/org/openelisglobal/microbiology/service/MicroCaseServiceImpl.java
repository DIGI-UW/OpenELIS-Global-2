package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseServiceImpl implements MicroCaseService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroIsolateDAO isolateDAO;

    public MicroCaseServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroIsolateDAO isolateDAO) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.isolateDAO = isolateDAO;
    }

    @Override
    @Transactional
    public MicroCase createOrGetCase(String sampleItemId, MicroWorkflowType workflowType, String cultureMethodId,
            String performedBy) {
        requireText(sampleItemId, "sampleItemId");
        if (workflowType == null) {
            throw new IllegalArgumentException("workflowType is required");
        }

        MicroCase existing = caseDAO.getBySampleItemAndWorkflow(sampleItemId, workflowType.name());
        if (existing != null) {
            return existing;
        }

        MicroCase microCase = new MicroCase();
        microCase.setSampleItemId(sampleItemId);
        microCase.setWorkflowType(workflowType.name());
        microCase.setCultureMethodId(cultureMethodId);
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        microCase.setCreatedAt(now());
        microCase.setCreatedBy(performedBy);
        caseDAO.insert(microCase);
        recordActivity(microCase.getId(), MicroCaseActivityType.CASE_CREATED, performedBy, "Case created", null);
        return microCase;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCase getCase(String caseId) {
        return caseDAO.get(caseId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCase getCaseForSampleItemWorkflow(String sampleItemId, MicroWorkflowType workflowType) {
        if (workflowType == null) {
            return null;
        }
        return caseDAO.getBySampleItemAndWorkflow(sampleItemId, workflowType.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getSiblingCases(String sampleItemId) {
        return caseDAO.getBySampleItem(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseDetailForm getCaseDetail(String caseId) {
        MicroCase microCase = getCase(caseId);
        if (microCase == null) {
            return null;
        }
        MicroCaseDetailForm form = toDetailForm(microCase);
        for (MicroCaseActivity activity : activityDAO.getByCaseId(caseId)) {
            form.activities.add(toActivityForm(activity));
        }
        for (MicroIsolate isolate : isolateDAO.getByCaseId(caseId)) {
            form.isolates.add(toIsolateForm(isolate));
        }
        return form;
    }

    void recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy, String note,
            String structuredData) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activity.setStructuredData(structuredData);
        activityDAO.insert(activity);
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private MicroCaseDetailForm toDetailForm(MicroCase microCase) {
        MicroCaseDetailForm form = new MicroCaseDetailForm();
        form.id = microCase.getId();
        form.sampleItemId = microCase.getSampleItemId();
        form.workflowType = microCase.getWorkflowType();
        form.stage = microCase.getStage();
        form.priority = microCase.getPriority();
        form.cultureMethodId = microCase.getCultureMethodId();
        form.createdAt = microCase.getCreatedAt();
        form.createdBy = microCase.getCreatedBy();
        form.closedAt = microCase.getClosedAt();
        form.closedBy = microCase.getClosedBy();
        form.finalReleaseState = microCase.getFinalReleaseState();
        return form;
    }

    private MicroCaseActivityForm toActivityForm(MicroCaseActivity activity) {
        MicroCaseActivityForm form = new MicroCaseActivityForm();
        form.id = activity.getId();
        form.caseId = activity.getCaseId();
        form.activityType = activity.getActivityType();
        form.occurredAt = activity.getOccurredAt();
        form.performedBy = activity.getPerformedBy();
        form.note = activity.getNote();
        form.structuredData = activity.getStructuredData();
        return form;
    }

    private MicroIsolateForm toIsolateForm(MicroIsolate isolate) {
        MicroIsolateForm form = new MicroIsolateForm();
        form.id = isolate.getId();
        form.caseId = isolate.getCaseId();
        form.isolateLabel = isolate.getIsolateLabel();
        form.organismId = isolate.getOrganismId();
        form.preliminaryOrganismText = isolate.getPreliminaryOrganismText();
        form.significance = isolate.getSignificance();
        form.identificationStatus = isolate.getIdentificationStatus();
        form.createdAt = isolate.getCreatedAt();
        return form;
    }
}
