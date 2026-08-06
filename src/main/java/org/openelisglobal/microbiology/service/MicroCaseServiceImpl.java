package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailForm;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseServiceImpl implements MicroCaseService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroCaseOrderDetailDAO orderDetailDAO;
    private final SampleItemService sampleItemService;
    private final SampleHumanService sampleHumanService;
    private final PatientService patientService;

    public MicroCaseServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroIsolateDAO isolateDAO,
            MicroCaseOrderDetailDAO orderDetailDAO, SampleItemService sampleItemService,
            SampleHumanService sampleHumanService, PatientService patientService) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.isolateDAO = isolateDAO;
        this.orderDetailDAO = orderDetailDAO;
        this.sampleItemService = sampleItemService;
        this.sampleHumanService = sampleHumanService;
        this.patientService = patientService;
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
        compileSpecimenContext(form, microCase.getSampleItemId());
        for (MicroCaseActivity activity : activityDAO.getByCaseId(caseId)) {
            form.activities.add(toActivityForm(activity));
        }
        List<MicroIsolate> isolates = isolateDAO.getByCaseId(caseId);
        for (MicroIsolate isolate : isolates) {
            form.isolates.add(toIsolateForm(isolate));
        }
        form.workflowChangeRequiresConfirmation = !MicroCaseStage.RECEIVED.name().equals(microCase.getStage())
                || !isolates.isEmpty();
        for (MicroCase sibling : caseDAO.getBySampleItem(microCase.getSampleItemId())) {
            if (!microCase.getId().equals(sibling.getId())) {
                form.siblingCases.add(toLookupForm(sibling));
            }
        }
        MicroCaseOrderDetail orderDetail = orderDetailDAO.getByCaseId(caseId);
        if (orderDetail != null) {
            form.orderDetail = toOrderDetailForm(orderDetail);
        }
        return form;
    }

    private void compileSpecimenContext(MicroCaseDetailForm form, String sampleItemId) {
        if (sampleItemId == null) {
            return;
        }
        SampleItem sampleItem = sampleItemService.getData(sampleItemId);
        if (sampleItem == null) {
            return;
        }
        Sample sample = sampleItem.getSample();
        if (sample != null) {
            form.accessionNumber = sample.getAccessionNumber();
            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient != null) {
                form.patientId = patient.getId();
                form.patientName = patientService.getLastFirstName(patient);
            }
        }
        if (sampleItem.getTypeOfSample() != null) {
            form.specimenType = sampleItem.getTypeOfSample().getDescription();
        }
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

    private MicroCaseOrderDetailForm toOrderDetailForm(MicroCaseOrderDetail orderDetail) {
        MicroCaseOrderDetailForm form = new MicroCaseOrderDetailForm();
        form.caseId = orderDetail.getCaseId();
        form.patientOrigin = orderDetail.getPatientOrigin();
        form.numberOfSets = orderDetail.getNumberOfSets();
        form.clinicalHistory = orderDetail.getClinicalHistory();
        form.antibioticExposure = orderDetail.getAntibioticExposure();
        form.criticalNotificationPreference = orderDetail.getCriticalNotificationPreference();
        return form;
    }

    private MicroIsolateForm toIsolateForm(MicroIsolate isolate) {
        MicroIsolateForm form = new MicroIsolateForm();
        form.id = isolate.getId();
        form.caseId = isolate.getCaseId();
        form.isolateLabel = isolate.getIsolateLabel();
        form.organismId = isolate.getOrganismId();
        form.preliminaryOrganismText = isolate.getPreliminaryOrganismText();
        form.gramStain = isolate.getGramStain();
        form.colonyMorphology = isolate.getColonyMorphology();
        form.identificationMethod = isolate.getIdentificationMethod();
        form.identificationConfidence = isolate.getIdentificationConfidence();
        form.significance = isolate.getSignificance();
        form.identificationStatus = isolate.getIdentificationStatus();
        form.createdAt = isolate.getCreatedAt();
        return form;
    }

    private MicroCaseLookupForm toLookupForm(MicroCase microCase) {
        MicroCaseLookupForm form = new MicroCaseLookupForm();
        form.id = microCase.getId();
        form.sampleItemId = microCase.getSampleItemId();
        form.workflowType = microCase.getWorkflowType();
        form.stage = microCase.getStage();
        form.priority = microCase.getPriority();
        return form;
    }
}
