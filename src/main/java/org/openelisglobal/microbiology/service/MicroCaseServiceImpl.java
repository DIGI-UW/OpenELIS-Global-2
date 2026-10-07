package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseSpecimenForm;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseServiceImpl implements MicroCaseService {

    private final org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO caseAnalysisDAO;
    private final org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO requestedTestDAO;
    private final org.openelisglobal.sampletyperequest.service.SampleTypeRequestService requestService;
    private final MicroCultureSetWarningService setWarningService;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseSpecimenDAO specimenDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroCaseOrderDetailDAO orderDetailDAO;
    private final SampleItemService sampleItemService;
    private final SampleHumanService sampleHumanService;
    private final PatientService patientService;
    private final SampleOrganizationService sampleOrganizationService;
    private final SystemUserService systemUserService;
    private final NceSpecimenService nceSpecimenService;
    private final TestSectionService testSectionService;

    public MicroCaseServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroIsolateDAO isolateDAO,
            MicroCaseOrderDetailDAO orderDetailDAO, SampleItemService sampleItemService,
            SampleHumanService sampleHumanService, PatientService patientService,
            SampleOrganizationService sampleOrganizationService, SystemUserService systemUserService,
            NceSpecimenService nceSpecimenService, MicroCaseSpecimenDAO specimenDAO,
            TestSectionService testSectionService,
            org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO caseAnalysisDAO,
            MicroCultureSetWarningService setWarningService,
            org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO requestedTestDAO,
            org.openelisglobal.sampletyperequest.service.SampleTypeRequestService requestService) {
        this.requestedTestDAO = requestedTestDAO;
        this.requestService = requestService;
        this.setWarningService = setWarningService;
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.testSectionService = testSectionService;
        this.caseDAO = caseDAO;
        this.specimenDAO = specimenDAO;
        this.activityDAO = activityDAO;
        this.isolateDAO = isolateDAO;
        this.orderDetailDAO = orderDetailDAO;
        this.sampleItemService = sampleItemService;
        this.sampleHumanService = sampleHumanService;
        this.patientService = patientService;
        this.sampleOrganizationService = sampleOrganizationService;
        this.systemUserService = systemUserService;
        this.nceSpecimenService = nceSpecimenService;
    }

    @Override
    @Transactional
    public MicroCase createOrGetCase(SampleItem sampleItem, Test test, String performedBy) {
        MicroCaseRoutingKey key = MicroCaseRoutingKey.forTest(sampleItem, test);
        requireText(sampleItem.getId(), "sampleItemId");
        requireText(key.sampleId(), "sampleId");
        requireText(performedBy, "performedBy");
        caseDAO.lockOrder(key.sampleId());
        List<MicroCase> candidates = caseDAO.getRoutingCandidates(key.sampleId(), key.sampleTypeId(),
                key.testSectionId(), key.collectedInSetsTestId(), sampleItem.getId());
        MicroCase microCase;
        if (candidates.isEmpty()) {
            microCase = new MicroCase();
            microCase.setSampleId(key.sampleId());
            microCase.setSampleTypeId(sampleItem.getTypeOfSampleId());
            microCase.setTestSectionId(key.testSectionId());
            microCase.setCreatedAt(now());
            microCase.setCreatedBy(performedBy);
            microCase.setSysUserId(performedBy);
            caseDAO.insert(microCase);
            recordActivity(microCase.getId(), MicroCaseActivityType.CASE_CREATED, performedBy, "Case created", null);
        } else {
            microCase = candidates.get(0);
            if (microCase.getClosedAt() != null || "FINAL_RELEASED".equals(microCase.getFinalReleaseState())) {
                throw new IllegalStateException("A finalized case requires an amendment before adding tests");
            }
        }
        if (specimenDAO.getByCaseAndSampleItem(microCase.getId(), sampleItem.getId()) == null) {
            MicroCaseSpecimen member = new MicroCaseSpecimen();
            member.setCaseId(microCase.getId());
            member.setSampleItemId(sampleItem.getId());
            member.setCreatedAt(now());
            member.setCreatedBy(performedBy);
            member.setSysUserId(performedBy);
            specimenDAO.insert(member);
            recordActivity(microCase.getId(), MicroCaseActivityType.SPECIMEN_ADDED, performedBy, "Sample added to case",
                    "{\"sampleItemId\":\"" + sampleItem.getId() + "\"}");
        }
        return microCase;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCase getCase(String caseId) {
        return caseDAO.get(caseId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseSpecimen> getSpecimens(String caseId) {
        return specimenDAO.getByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSpecimenIds(String caseId) {
        return getSpecimens(caseId).stream().map(MicroCaseSpecimen::getSampleItemId).toList();
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
        compileRequestedContext(form);
        for (MicroCaseSpecimen member : getSpecimens(caseId)) {
            compileSpecimenContext(form, member);
        }
        Map<String, String> userDisplayById = new HashMap<>();
        for (MicroCaseActivity activity : activityDAO.getByCaseId(caseId)) {
            MicroCaseActivityForm activityForm = toActivityForm(activity, userDisplayById);
            form.activities.add(activityForm);
            if (activity.getOccurredAt() != null
                    && (form.lastActivityAt == null || activity.getOccurredAt().after(form.lastActivityAt))) {
                form.lastActivityAt = activity.getOccurredAt();
                form.lastActivityBy = activityForm.performedByDisplay;
            }
        }
        List<MicroIsolate> isolates = isolateDAO.getByCaseId(caseId);
        for (MicroIsolate isolate : isolates) {
            form.isolates.add(toIsolateForm(isolate));
        }
        for (MicroCase sibling : caseDAO.getByOrder(microCase.getSampleId())) {
            if (!microCase.getId().equals(sibling.getId())) {
                form.siblingCases.add(toLookupForm(sibling));
            }
        }
        MicroCaseOrderDetail orderDetail = orderDetailDAO.getByCaseId(caseId);
        if (orderDetail != null) {
            form.orderDetail = toOrderDetailForm(orderDetail);
        }
        List<String> setSpecimenIds = caseAnalysisDAO.getSetSpecimenIds(caseId);
        for (MicroCaseSpecimenForm specimen : form.specimens) {
            specimen.collectedInSets = setSpecimenIds.contains(specimen.sampleItemId);
        }
        if (!setSpecimenIds.isEmpty() || form.requestedSpecimens.stream().anyMatch(r -> r.collectedInSets)) {
            if (form.orderDetail == null) {
                form.orderDetail = new MicroCaseOrderDetailForm();
                form.orderDetail.caseId = caseId;
            }
            form.orderDetail.numberOfSets = (int) java.util.stream.Stream
                    .concat(form.specimens.stream().filter(s -> s.collectedInSets).map(s -> s.cultureSetNumber),
                            form.requestedSpecimens.stream().filter(s -> s.collectedInSets)
                                    .map(s -> s.cultureSetNumber))
                    .filter(java.util.Objects::nonNull).distinct().count();
        }
        List<MicroCaseSpecimenForm> warningInputs = new java.util.ArrayList<>(form.specimens);
        for (var request : form.requestedSpecimens) {
            // Warning inputs never enter the collected-specimen/result-target list.
            var input = new MicroCaseSpecimenForm();
            input.collectedInSets = request.collectedInSets;
            input.cultureSetNumber = request.cultureSetNumber;
            input.bodySite = request.bodySite;
            input.containerType = request.containerType;
            input.collectionDate = request.collectionDate;
            warningInputs.add(input);
        }
        form.setWarnings = setWarningService.evaluate(warningInputs);
        return form;
    }

    private void compileRequestedContext(MicroCaseDetailForm form) {
        Map<Integer, org.openelisglobal.microbiology.form.MicroCaseRequestedSpecimenForm> pending = new java.util.LinkedHashMap<>();
        for (var link : requestedTestDAO.getByCaseId(form.id)) {
            if (link.getCancelledAt() != null) {
                continue;
            }
            var request = requestService.get(link.getRequestId());
            if (request
                    .getStatus() != org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest.Status.REQUESTED) {
                continue;
            }
            var row = pending.get(request.getId());
            if (row == null) {
                row = new org.openelisglobal.microbiology.form.MicroCaseRequestedSpecimenForm();
                row.requestId = request.getId();
                row.sampleTypeId = request.getTypeOfSample().getId();
                row.specimenType = request.getTypeOfSample().getLocalizedName();
                row.bodySite = request.getBodySite();
                row.containerType = request.getContainer();
                row.cultureSetNumber = request.getCultureSetNumber();
                if (request.getCollectionDate() != null) {
                    row.collectionDate = Timestamp.valueOf(request.getCollectionDate().toLocalDate()
                            .atTime(request.getCollectionTime() == null ? java.time.LocalTime.MIDNIGHT
                                    : java.time.LocalTime.parse(request.getCollectionTime())));
                }
                pending.put(request.getId(), row);
                compileOrderContext(form, request.getSample());
            }
            row.collectedInSets |= link.isCollectedInSets();
        }
        form.requestedSpecimens.addAll(pending.values());
    }

    private void compileOrderContext(MicroCaseDetailForm form, Sample sample) {
        if (sample != null) {
            form.accessionNumber = sample.getAccessionNumber();
            SampleOrganization sampleOrganization = sampleOrganizationService.getDataBySample(sample);
            if (sampleOrganization != null && sampleOrganization.getOrganization() != null) {
                form.requestingLocation = sampleOrganization.getOrganization().getOrganizationName();
            }
            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient != null) {
                form.patientId = patient.getId();
                form.patientName = patientService.getLastFirstName(patient);
            }
        }
    }

    private void compileSpecimenContext(MicroCaseDetailForm form, MicroCaseSpecimen member) {
        SampleItem sampleItem = sampleItemService.getData(member.getSampleItemId());
        if (sampleItem == null) {
            return;
        }
        Sample sample = sampleItem.getSample();
        compileOrderContext(form, sample);
        if (sampleItem.getTypeOfSample() != null) {
            form.specimenType = form.specimenType == null ? sampleItem.getTypeOfSample().getDescription()
                    : form.specimenType + ", " + sampleItem.getTypeOfSample().getDescription();
        }
        MicroCaseSpecimenForm specimen = new MicroCaseSpecimenForm();
        specimen.id = member.getId();
        specimen.sampleItemId = sampleItem.getId();
        specimen.label = (sample == null ? "" : sample.getAccessionNumber()) + "-" + sampleItem.getSortOrder();
        specimen.sampleTypeId = sampleItem.getTypeOfSampleId();
        specimen.specimenType = sampleItem.getTypeOfSample() == null ? null
                : sampleItem.getTypeOfSample().getDescription();
        specimen.bodySite = sampleItem.getSourceOther();
        specimen.cultureSetNumber = sampleItem.getCultureSetNumber();
        specimen.containerType = sampleItem.getContainer();
        specimen.collectionDate = sampleItem.getCollectionDate();
        form.specimens.add(specimen);
        try {
            List<?> linkedNonconformances = nceSpecimenService
                    .getSpecimenBySampleItemId(Integer.valueOf(sampleItem.getId()));
            form.nonconformanceCount += linkedNonconformances == null ? 0 : linkedNonconformances.size();
        } catch (NumberFormatException ignored) {
            form.nonconformanceCount = 0;
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
        form.sampleId = microCase.getSampleId();
        form.testSectionId = microCase.getTestSectionId();
        var labUnit = testSectionService.getTestSectionById(microCase.getTestSectionId());
        form.testSectionName = labUnit == null ? null : labUnit.getTestSectionName();
        form.stage = microCase.getStage();
        form.priority = microCase.getPriority();
        form.programId = microCase.getProgramId();
        form.migrationReviewRequired = microCase.isMigrationReviewRequired();
        form.createdAt = microCase.getCreatedAt();
        form.createdBy = microCase.getCreatedBy();
        form.closedAt = microCase.getClosedAt();
        form.closedBy = microCase.getClosedBy();
        form.finalReleaseState = microCase.getFinalReleaseState();
        return form;
    }

    private MicroCaseActivityForm toActivityForm(MicroCaseActivity activity, Map<String, String> userDisplayById) {
        MicroCaseActivityForm form = new MicroCaseActivityForm();
        form.id = activity.getId();
        form.caseId = activity.getCaseId();
        form.activityType = activity.getActivityType();
        form.occurredAt = activity.getOccurredAt();
        form.performedBy = activity.getPerformedBy();
        form.performedByDisplay = MicrobiologyUserDisplayResolver.resolve(systemUserService, activity.getPerformedBy(),
                userDisplayById);
        form.note = activity.getNote();
        form.structuredData = activity.getStructuredData();
        form.resultSourceSampleItemId = activity.getResultSourceSampleItemId();
        return form;
    }

    private MicroCaseOrderDetailForm toOrderDetailForm(MicroCaseOrderDetail orderDetail) {
        MicroCaseOrderDetailForm form = new MicroCaseOrderDetailForm();
        form.caseId = orderDetail.getCaseId();
        form.patientOrigin = orderDetail.getPatientOrigin();
        form.culturePurpose = orderDetail.getCulturePurpose();
        form.admissionDate = orderDetail.getAdmissionDate() == null ? null : orderDetail.getAdmissionDate().toString();
        form.clinicalHistory = orderDetail.getClinicalHistory();
        form.antibioticExposure = orderDetail.getAntibioticExposure();
        return form;
    }

    private MicroIsolateForm toIsolateForm(MicroIsolate isolate) {
        MicroIsolateForm form = new MicroIsolateForm();
        form.id = isolate.getId();
        form.caseId = isolate.getCaseId();
        form.sourceSampleItemId = isolate.getSourceSampleItemId();
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
        form.sampleId = microCase.getSampleId();
        form.testSectionId = microCase.getTestSectionId();
        var labUnit = testSectionService.getTestSectionById(microCase.getTestSectionId());
        form.testSectionName = labUnit == null ? null : labUnit.getTestSectionName();
        form.stage = microCase.getStage();
        form.priority = microCase.getPriority();
        return form;
    }
}
