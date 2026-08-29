package org.openelisglobal.referral.fhir.service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskRestrictionComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.registration.ValidationUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.service.ReferralSetService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.service.ResultValidationService;
import org.openelisglobal.resultvalidation.util.ResultValidationSaveService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirReferralServiceImpl implements FhirReferralService {

    @Autowired
    private ReferenceTablesService referenceTablesService;
    @Autowired
    private DocumentTrackService documentTrackService;
    @Autowired
    private DocumentTypeService documentTypeService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private ReferralResultService referralResultService;
    @Autowired
    private ReferralService referralService;
    @Autowired
    private ReferralSetService referralSetService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private ResultValidationService resultValidationService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private TestService testService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private AlertService alertService;

    private final String RESULT_SUBJECT = "Result Note";
    private String RESULT_TABLE_ID;
    private String RESULT_REPORT_ID;

    @PostConstruct
    public void setup() {
        RESULT_TABLE_ID = referenceTablesService.getReferenceTableByName("RESULT").getId();
        RESULT_REPORT_ID = documentTypeService.getDocumentTypeByName("resultExport").getId();
    }

    // @Override
    // @Transactional
    // public Bundle cancelReferralToOrganization(String referralOrganizationId,
    // String sampleId,
    // List<String> analysisIds) throws FhirLocalPersistingException {
    // org.openelisglobal.organization.valueholder.Organization referralOrganization
    // =
    // organizationService
    // .get(referralOrganizationId);
    // Organization fhirOrg = getFhirOrganization(referralOrganization);
    // if (fhirOrg == null) {
    // // organization doesn't exist as fhir organization, cannot cancel
    // automatically
    // return new Bundle();
    // }
    // Sample sample = sampleService.get(sampleId);
    // List<Analysis> analysises = analysisService.get(analysisIds);
    //
    // List<ServiceRequest> serviceRequests = new ArrayList<>();
    // for (Analysis analysis : analysises) {
    //
    // serviceRequests.add(fhirPersistanceService.getServiceRequestByAnalysisUuid(analysis.getFhirUuidAsString())
    // .orElseThrow());
    // }
    //
    // Task task =
    // this.fhirPersistanceService.getTaskBasedOnServiceRequests(serviceRequests).orElseThrow();
    // task.setStatus(TaskStatus.CANCELLED);
    // return fhirPersistanceService.updateFhirResourceInFhirStore(task);
    // }

    @Override
    @Transactional
    public Bundle referAnalysisesToOrganization(Referral referral) throws FhirLocalPersistingException {
        String referralOrganizationId = referral.getOrganization().getId();
        String sampleId = referral.getAnalysis().getSampleItem().getSample().getId();
        String analysisId = referral.getAnalysis().getId();

        org.openelisglobal.organization.valueholder.Organization referralOrganization = organizationService
                .get(referralOrganizationId);
        Organization fhirOrg = getFhirOrganization(referralOrganization);
        if (fhirOrg == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "referAnalysisesToOrganization",
                    "no fhir organization provided");
            // organization doesn't exist as fhir organization, cannot refer automatically
            return new Bundle();
        }
        Map<String, Resource> updateResources = new HashMap<>();
        Sample sample = sampleService.get(sampleId);
        Provider provider = sampleHumanService.getProviderForSample(sample);

        Analysis analysis = analysisService.get(analysisId);
        ServiceRequest serviceRequest = fhirPersistanceService
                .getServiceRequestByAnalysisUuid(analysis.getFhirUuidAsString()).orElseGet(() -> {
                    // env/vector path: no prior copy in the store; build a complete SR so the
                    // receiver gets the samp_domain category + Specimen reference. Falls back
                    // to the bare-SR behaviour if transformToServiceRequest returns null
                    // (e.g. pool-level analyses pre-fanout, where Sample cannot be resolved).
                    ServiceRequest fresh = fhirTransformService.transformToServiceRequest(analysis.getId());
                    if (fresh != null) {
                        return fresh;
                    }
                    ServiceRequest sr = new ServiceRequest();
                    sr.setId(analysis.getFhirUuidAsString());
                    return sr;
                });
        Optional<Practitioner> requester = Optional.empty();
        if (provider != null) {
            requester = Optional.of(fhirTransformService.transformProviderToPractitioner(provider));
            requester.get().setId(UUID.randomUUID().toString());
        }

        // OGC-356: only environmental ("E") and vector ("V") samples lack a
        // patient. For clinical (human, "H") samples require a patient object
        org.openelisglobal.patient.valueholder.Patient localPatient = sampleHumanService.getPatientForSample(sample);
        Patient fhirPatient;
        if (localPatient != null) {
            fhirPatient = fhirPersistanceService.getPatientByUuid(localPatient.getFhirUuidAsString()).orElse(null);
        } else if ("E".equals(sample.getDomain()) || "V".equals(sample.getDomain())) {
            fhirPatient = null;
        } else {
            throw new IllegalStateException("Referral on clinical sample " + sample.getId()
                    + " has no linked patient — sample_human row missing or sample created without a patient");
        }
        Task task = createReferralTask(fhirOrg, fhirPatient, serviceRequest, requester, sample);
        task.setId(referral.getFhirUuidAsString());
        if (requester.isPresent()) {
            updateResources.put(requester.get().getIdElement().getIdPart(), requester.get());
        }
        updateResources.put(task.getIdElement().getIdPart(), task);
        updateResources.put(serviceRequest.getIdElement().getIdPart(), serviceRequest);

        SampleItem sampleItem = analysis.getSampleItem();
        if (sampleItem != null) {
            Specimen specimen = fhirTransformService.transformToSpecimen(sampleItem);
            updateResources.put(specimen.getIdElement().getIdPart(), specimen);
        }

        return fhirPersistanceService.updateFhirResourcesInFhirStore(updateResources);
    }

    @Override
    @Transactional
    public void publishManualEntryCompletion(Referral referral, String actorUserId)
            throws FhirLocalPersistingException {
        if (referral.getFhirUuid() == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishManualEntryCompletion",
                    "skipping FHIR publish: referral " + referral.getId() + " has no fhir_uuid");
            return;
        }
        Analysis analysis = referral.getAnalysis();
        if (analysis == null || analysis.getFhirUuid() == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishManualEntryCompletion",
                    "skipping FHIR publish: referral " + referral.getId() + " has no analysis fhir_uuid");
            return;
        }

        Map<String, Resource> updateResources = new HashMap<>();

        // 1. DiagnosticReport + Observations from the locally-entered results. DR and
        // ServiceRequest both have IDs equal to analysis.fhir_uuid in the OE FHIR
        // model — key the map by resource-type-qualified ID so they don't collide.
        DiagnosticReport diagnosticReport;
        try {
            diagnosticReport = fhirTransformService.transformResultToDiagnosticReport(analysis);
            putByTypedId(updateResources, diagnosticReport);
            for (Result result : resultService.getResultsByAnalysis(analysis)) {
                Observation observation = fhirTransformService.transformResultToObservation(result);
                putByTypedId(updateResources, observation);
            }
        } catch (FhirTransformationException e) {
            throw new FhirLocalPersistingException(e);
        }

        // 2. ServiceRequest — fetch existing copy and flip status to COMPLETED. Falls
        // back to a freshly-built SR for env/vector samples where the dispatch path
        // wouldn't have left a prior copy in the store.
        ServiceRequest serviceRequest = fhirPersistanceService
                .getServiceRequestByAnalysisUuid(analysis.getFhirUuidAsString()).orElseGet(() -> {
                    ServiceRequest fresh = fhirTransformService.transformToServiceRequest(analysis.getId());
                    if (fresh != null) {
                        return fresh;
                    }
                    ServiceRequest sr = new ServiceRequest();
                    sr.setId(analysis.getFhirUuidAsString());
                    return sr;
                });
        serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        putByTypedId(updateResources, serviceRequest);

        // 3. Task — rebuild the same way the dispatch path does, but status=completed
        // and with an output reference to the DiagnosticReport we just published.
        org.openelisglobal.organization.valueholder.Organization referralOrganization = referral.getOrganization();
        Organization fhirOrg = referralOrganization == null ? null : getFhirOrganization(referralOrganization);
        Sample sample = analysis.getSampleItem() != null ? analysis.getSampleItem().getSample() : null;
        org.openelisglobal.patient.valueholder.Patient localPatient = sample == null ? null
                : sampleHumanService.getPatientForSample(sample);
        Patient fhirPatient = (localPatient == null) ? null
                : fhirPersistanceService.getPatientByUuid(localPatient.getFhirUuidAsString()).orElse(null);
        Provider provider = sample == null ? null : sampleHumanService.getProviderForSample(sample);
        Optional<Practitioner> requester = Optional.empty();
        if (provider != null) {
            requester = Optional.of(fhirTransformService.transformProviderToPractitioner(provider));
            requester.get().setId(UUID.randomUUID().toString());
            putByTypedId(updateResources, requester.get());
        }
        Task task = createReferralTask(fhirOrg, fhirPatient, serviceRequest, requester, sample);
        task.setId(referral.getFhirUuidAsString());
        task.setStatus(TaskStatus.COMPLETED);
        task.addOutput(
                new Task.TaskOutputComponent().setValue(fhirTransformService.createReferenceFor(diagnosticReport))
                        .setType(new CodeableConcept()
                                .addCoding(new Coding().setSystem(fhirConfig.getOeFhirSystem() + "/task_output")
                                        .setCode("DiagnosticReport"))));
        putByTypedId(updateResources, task);

        fhirPersistanceService.updateFhirResourcesInFhirStore(updateResources);
    }

    // REQUIRES_NEW so a FHIR-store outage that throws here marks ONLY this inner
    // tx rollback-only — never the caller's transaction that already committed the
    // local lost-flag write. The caller's catch then actually works (with plain
    // REQUIRED the exception poisons the shared tx before the caller's catch runs
    // and commit fails with UnexpectedRollbackException). Re-fetch by id so lazy
    // associations resolve against this inner session's live persistence context.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishReferralLost(Referral detached, String reason, String actorUserId)
            throws FhirLocalPersistingException {
        if (detached.getFhirUuid() == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishReferralLost",
                    "skipping FHIR publish: referral " + detached.getId() + " has no fhir_uuid");
            return;
        }
        Referral referral = referralService.getReferralById(detached.getId());
        if (referral == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishReferralLost",
                    "skipping FHIR publish: referral " + detached.getId() + " no longer exists");
            return;
        }
        Analysis analysis = referral.getAnalysis();
        if (analysis == null || analysis.getFhirUuid() == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishReferralLost",
                    "skipping FHIR publish: referral " + referral.getId() + " has no analysis fhir_uuid");
            return;
        }

        Map<String, Resource> updateResources = new HashMap<>();

        // ServiceRequest -> REVOKED. Fall back to a bare SR when the store has no
        // prior copy (env/vector samples where the dispatch path never pushed one).
        ServiceRequest serviceRequest = fhirPersistanceService
                .getServiceRequestByAnalysisUuid(analysis.getFhirUuidAsString()).orElseGet(() -> {
                    ServiceRequest sr = new ServiceRequest();
                    sr.setId(analysis.getFhirUuidAsString());
                    return sr;
                });
        serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
        putByTypedId(updateResources, serviceRequest);

        // Task -> CANCELLED with statusReason "lost in transit" + note carrying the
        // user-supplied reason. Built the same way the dispatch / completion paths do.
        org.openelisglobal.organization.valueholder.Organization referralOrganization = referral.getOrganization();
        Organization fhirOrg = referralOrganization == null ? null : getFhirOrganization(referralOrganization);
        Sample sample = analysis.getSampleItem() != null ? analysis.getSampleItem().getSample() : null;
        org.openelisglobal.patient.valueholder.Patient localPatient = sample == null ? null
                : sampleHumanService.getPatientForSample(sample);
        Patient fhirPatient = (localPatient == null) ? null
                : fhirPersistanceService.getPatientByUuid(localPatient.getFhirUuidAsString()).orElse(null);
        Provider provider = sample == null ? null : sampleHumanService.getProviderForSample(sample);
        Optional<Practitioner> requester = Optional.empty();
        if (provider != null) {
            requester = Optional.of(fhirTransformService.transformProviderToPractitioner(provider));
            requester.get().setId(UUID.randomUUID().toString());
            putByTypedId(updateResources, requester.get());
        }
        Task task = createReferralTask(fhirOrg, fhirPatient, serviceRequest, requester, sample);
        task.setId(referral.getFhirUuidAsString());
        task.setStatus(TaskStatus.CANCELLED);
        task.setStatusReason(new CodeableConcept().setText("lost in transit"));
        if (reason != null && !reason.isBlank()) {
            task.addNote(new Annotation().setText(reason));
        }
        putByTypedId(updateResources, task);

        fhirPersistanceService.updateFhirResourcesInFhirStore(updateResources);
    }

    // REQUIRES_NEW so a FHIR-store outage that throws here marks ONLY this inner
    // tx rollback-only — never the caller's transaction that already committed the
    // local rejection. The caller's catch then actually works (with plain REQUIRED
    // the exception poisons the shared tx before the caller's catch runs and commit
    // fails with UnexpectedRollbackException). Re-fetch by id so lazy associations
    // resolve against this inner session's live persistence context.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishReferralRejected(Referral detached, String reasonText, String actorUserId)
            throws FhirLocalPersistingException {
        if (detached.getFhirUuid() == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishReferralRejected",
                    "skipping FHIR publish: referral " + detached.getId() + " has no fhir_uuid");
            return;
        }
        Referral referral = referralService.getReferralById(detached.getId());
        if (referral == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishReferralRejected",
                    "skipping FHIR publish: referral " + detached.getId() + " no longer exists");
            return;
        }
        Analysis analysis = referral.getAnalysis();
        if (analysis == null || analysis.getFhirUuid() == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "publishReferralRejected",
                    "skipping FHIR publish: referral " + referral.getId() + " has no analysis fhir_uuid");
            return;
        }

        Map<String, Resource> updateResources = new HashMap<>();

        // ServiceRequest -> REVOKED. Fall back to a bare SR when the store has no
        // prior copy (env/vector samples where the dispatch path never pushed one).
        ServiceRequest serviceRequest = fhirPersistanceService
                .getServiceRequestByAnalysisUuid(analysis.getFhirUuidAsString()).orElseGet(() -> {
                    ServiceRequest sr = new ServiceRequest();
                    sr.setId(analysis.getFhirUuidAsString());
                    return sr;
                });
        serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
        putByTypedId(updateResources, serviceRequest);

        // Task -> REJECTED with statusReason "rejected by reference lab" + note
        // carrying the user-supplied reason. Built like the lost/dispatch paths.
        org.openelisglobal.organization.valueholder.Organization referralOrganization = referral.getOrganization();
        Organization fhirOrg = referralOrganization == null ? null : getFhirOrganization(referralOrganization);
        Sample sample = analysis.getSampleItem() != null ? analysis.getSampleItem().getSample() : null;
        org.openelisglobal.patient.valueholder.Patient localPatient = sample == null ? null
                : sampleHumanService.getPatientForSample(sample);
        Patient fhirPatient = (localPatient == null) ? null
                : fhirPersistanceService.getPatientByUuid(localPatient.getFhirUuidAsString()).orElse(null);
        Provider provider = sample == null ? null : sampleHumanService.getProviderForSample(sample);
        Optional<Practitioner> requester = Optional.empty();
        if (provider != null) {
            requester = Optional.of(fhirTransformService.transformProviderToPractitioner(provider));
            requester.get().setId(UUID.randomUUID().toString());
            putByTypedId(updateResources, requester.get());
        }
        Task task = createReferralTask(fhirOrg, fhirPatient, serviceRequest, requester, sample);
        task.setId(referral.getFhirUuidAsString());
        task.setStatus(TaskStatus.REJECTED);
        task.setStatusReason(new CodeableConcept().setText("rejected by reference lab"));
        if (reasonText != null && !reasonText.isBlank()) {
            task.addNote(new Annotation().setText(reasonText));
        }
        putByTypedId(updateResources, task);

        fhirPersistanceService.updateFhirResourcesInFhirStore(updateResources);
    }

    private void putByTypedId(Map<String, Resource> resources, Resource resource) {
        resources.put(resource.getResourceType().name() + "/" + resource.getIdElement().getIdPart(), resource);
    }

    private Organization getFhirOrganization(org.openelisglobal.organization.valueholder.Organization organization) {
        Optional<Organization> fhiOrganization = fhirPersistanceService
                .getFhirOrganizationByName(organization.getOrganizationName());
        if (fhiOrganization.isPresent()) {
            return fhiOrganization.get();
        } else {
            try {
                Organization fhirOrg = fhirTransformService.transformToFhirOrganization(organization);
                fhirPersistanceService.createFhirResourceInFhirStore(fhirOrg);
            } catch (FhirTransformationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FhirPersistanceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Best-effort: a missing/unreachable FHIR Organization degrades to a null
        // owner reference rather than throwing — the Task/SR build tolerates a null
        // org.
        return fhirPersistanceService.getFhirOrganizationByName(organization.getOrganizationName()).orElse(null);
    }

    public Task createReferralTask(Organization referralOrganization, Patient patient, ServiceRequest serviceRequest,
            Optional<Practitioner> requester, Sample sample) {
        Bundle bundle = new Bundle();
        Task task = new Task();
        // task.setGroupIdentifier(
        // new Identifier().setValue(labNumber).setSystem(fhirConfig.getOeFhirSystem() +
        // "/samp_labNumber"));
        // TODO put the referral reason into the code
        task.setReasonCode(new CodeableConcept()
                .addCoding(new Coding().setSystem(fhirConfig.getOeFhirSystem() + "/refer_reason")));
        task.setOwner(fhirTransformService.createReferenceFor(referralOrganization));
        if (requester.isPresent()) {
            task.setRequester(fhirTransformService.createReferenceFor(requester.get()));
        }
        if (!fhirConfig.getRemoteStoreIdentifier().isEmpty()) {
            task.setRestriction(new TaskRestrictionComponent()
                    .setRecipient(Arrays.asList(new Reference(fhirConfig.getRemoteStoreIdentifier().get(0)))));
        }
        task.setAuthoredOn(new Date());
        task.setStatus(TaskStatus.REQUESTED);
        // OGC-356: environmental & vector samples have no patient — omit the
        // `task.for` reference rather than NPE in createReferenceFor.
        if (patient != null) {
            task.setFor(fhirTransformService.createReferenceFor(patient));
        }
        task.setBasedOn(Arrays.asList(fhirTransformService.createReferenceFor(serviceRequest)));
        task.setFocus(fhirTransformService.createReferenceFor(serviceRequest));
        task.setDescription("referring accession number " + sample.getAccessionNumber() + " from "
                + task.getRequester().getReference() + " to " + task.getOwner().getReference());

        bundle.addEntry(new BundleEntryComponent().setResource(task));
        return task;
    }

    @Override
    @Transactional
    public void setReferralResult(ReferralResultsImportObjects resultsImport) {
        // TODO make this work for multiple service requests
        Analysis analysis = analysisService
                .getMatch("fhirUuid", UUID.fromString(
                        resultsImport.originalReferralObjects.serviceRequests.get(0).getIdElement().getIdPart()))
                .orElseThrow(() -> {
                    return new RuntimeException("no matching analysis with FhirUUID: "
                            + resultsImport.originalReferralObjects.serviceRequests.get(0).getIdElement().getIdPart());
                });

        // Refuse to apply incoming results when the referral has been marked lost
        // locally — otherwise contradictory truths land in the same row
        // (lost_status=true AND analysis=Finalized with results).
        Referral referralForAnalysis = referralService.getReferralByAnalysisId(analysis.getId());
        if (referralForAnalysis != null && Boolean.TRUE.equals(referralForAnalysis.getLostStatus())) {
            throw new IllegalStateException("Refusing to apply FHIR results for referral " + referralForAnalysis.getId()
                    + ": referral is marked lost in transit");
        }

        List<Result> currentResults = resultService.getResultsByAnalysis(analysis);

        List<IResultUpdate> updaters = ValidationUpdateRegister.getRegisteredUpdaters();
        boolean areListeners = !updaters.isEmpty();
        // wrapper object for holding modifedResultSet and newResultSet
        IResultSaveService resultSaveService = new ResultValidationSaveService();

        List<AnalysisItem> resultItemList = new ArrayList<>();
        List<Analysis> analysisUpdateList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();
        List<Result> deletableList = new ArrayList<>();
        List<ReferralSet> referralSets = new ArrayList<>();

        AnalysisItem analysisItem = new AnalysisItem();

        analysisItem.setAccessionNumber(analysis.getSampleItem().getSample().getAccessionNumber());
        resultItemList.add(analysisItem);

        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
        analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
        analysis.setReleasedDate(DateUtil.getNowAsTimestamp());
        analysisUpdateList.add(analysis);

        // createNeededNotes(analysisItem, analysis, noteUpdateList);

        List<Map.Entry<Result, Observation>> criticalResults = new ArrayList<>();
        for (Observation observation : resultsImport.observations) {
            Result result = getResultFromObservation(observation, currentResults, analysis);
            resultUpdateList.add(result);
            if (areListeners) {
                addResultSets(analysis, result, resultSaveService);
            }
            recordResultForReferral(resultsImport, analysis, result, referralSets);
            if (isCriticalOrAbnormal(observation)) {
                criticalResults.add(new java.util.AbstractMap.SimpleEntry<>(result, observation));
            }
        }

        try {
            LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                    "referralSetService.updateReferralSets");
            referralSetService.updateReferralSets(referralSets, new ArrayList<>(), new HashSet<>(), new ArrayList<>(),
                    "1");
            LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                    "resultValidationService.persistdata");
            resultValidationService.persistdata(new ArrayList<>(), analysisUpdateList, resultUpdateList, resultItemList,
                    sampleUpdateList, noteUpdateList, resultSaveService, new ArrayList<>(), "1");
            LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                    "fhirTransformService.transformPersistResultValidationFhirObjects");
            fhirTransformService.transformPersistResultValidationFhirObjects(deletableList, analysisUpdateList,
                    resultUpdateList, resultItemList, sampleUpdateList, noteUpdateList);
            // OGC-803: a Critical/Abnormal returned result raises an in-app Alert in
            // addition to posting the result (acceptance still proceeds normally).
            for (Map.Entry<Result, Observation> critical : criticalResults) {
                raiseCriticalResultAlert(analysis, critical.getKey(), critical.getValue());
            }
            resultsImport.originalReferralObjects.task.setStatus(TaskStatus.COMPLETED);
            LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                    "fhirPersistanceService.updateFhirResourceInFhirStore");
            fhirPersistanceService.updateFhirResourceInFhirStore(resultsImport.originalReferralObjects.task);
        } catch (FhirPersistanceException e) {
            LogEvent.logError(e);
        }
    }

    // HL7 v3 ObservationInterpretation: AA/HH/LL are critical, A/H/L abnormal.
    // Also honour a free-text "critical"/"abnormal" interpretation.
    private boolean isCriticalOrAbnormal(Observation observation) {
        for (CodeableConcept interpretation : observation.getInterpretation()) {
            for (Coding coding : interpretation.getCoding()) {
                String code = coding.getCode() == null ? "" : coding.getCode().toUpperCase();
                if (code.equals("AA") || code.equals("HH") || code.equals("LL") || code.equals("A") || code.equals("H")
                        || code.equals("L") || code.equals("AB")) {
                    return true;
                }
            }
            String text = interpretation.getText();
            if (text != null && (text.equalsIgnoreCase("critical") || text.equalsIgnoreCase("abnormal"))) {
                return true;
            }
        }
        return false;
    }

    // OGC-803: raise an in-app Alert for a Critical/Abnormal returned result.
    // Best-effort — an alert failure must never roll back the posted result.
    private void raiseCriticalResultAlert(Analysis analysis, Result result, Observation observation) {
        try {
            String testCode = analysis.getTest() != null ? analysis.getTest().getId() : "";
            String value = result.getValue() == null ? "" : result.getValue();
            String range = observation.hasReferenceRange() && observation.getReferenceRangeFirstRep().hasText()
                    ? observation.getReferenceRangeFirstRep().getText()
                    : "";
            String deepLink = "/result?analysisId=" + analysis.getId();
            String json = "{\"analysisId\":\"" + analysis.getId() + "\",\"testCode\":" + jsonStr(testCode)
                    + ",\"value\":" + jsonStr(value) + ",\"range\":" + jsonStr(range) + ",\"deepLink\":"
                    + jsonStr(deepLink) + "}";
            Long entityId = result.getId() != null ? Long.valueOf(result.getId()) : Long.valueOf(analysis.getId());
            alertService.createAlert(AlertType.REFERRAL_CRITICAL_RESULT, "Result", entityId, AlertSeverity.CRITICAL,
                    "Critical/abnormal reference lab result for analysis " + analysis.getId(), json);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "raiseCriticalResultAlert",
                    "failed to raise critical-result alert for analysis " + analysis.getId());
            LogEvent.logError(e);
        }
    }

    private static String jsonStr(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private Result getResultFromObservation(Observation observation, List<Result> currentResults, Analysis analysis) {
        Result result;
        LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation",
                "creating result from observation");
        if (currentResults.size() == 1) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation",
                    "previous result found, writing new result to result");
            result = currentResults.get(0);
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation",
                    "creating new result from observation");
            result = new Result();
            String testResultType = testService.getResultType(analysis.getTest());
            result.setResultType(testResultType);
            result.setAnalysis(analysis);
            currentResults.stream().forEach(e -> {
                resultService.delete(e);
            });
        }

        if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())
                || TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation",
                    "multi/dictionary result type");

            String inferredValue = ((CodeableConcept) observation.getValue()).getCodingFirstRep().getCode();
            List<TestResult> testResults = testResultService
                    .getAllActiveTestResultsPerTest(analysisService.getTest(analysis));
            String resultValue = null;
            LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation",
                    "matching result to dictionary entry");
            for (TestResult testResult : testResults) {
                if (StringUtils.equals(inferredValue, dictionaryService.get(testResult.getValue()).getDictEntry())) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                            "found a matching dictionary value for: " + inferredValue + "");
                    resultValue = dictionaryService.get(testResult.getValue()).getId();
                    result.setValue(resultValue);
                    LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                            "value set as: " + resultValue + "");
                }
            }
            if (resultValue == null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "setReferralResult",
                        "no matching dictionary value for '" + inferredValue + "'");
            }
        } else if (TypeOfTestResultServiceImpl.ResultType.isNumeric(result.getResultType())) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation", "numeric result type");
            result.setValue(observationValueAsString(observation));
        } else if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(result.getResultType())) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation", "text result type");
            result.setValue(observationValueAsString(observation));
        }

        LogEvent.logDebug(this.getClass().getSimpleName(), "getResultFromObservation", "result made from observation");
        return result;
    }

    /**
     * Coerce an Observation's value to a String, regardless of the FHIR wrapper
     * type. The local {@code result_type} dictates how we store the value, but the
     * remote OE may have published it under a different wrapper (e.g. Quantity for
     * a value the local install classifies as text-only). This drift is routine
     * across independent installations.
     */
    private String observationValueAsString(Observation observation) {
        org.hl7.fhir.r4.model.Type value = observation.getValue();
        if (value == null) {
            return null;
        }
        if (value instanceof StringType) {
            return ((StringType) value).getValueAsString();
        }
        if (value instanceof Quantity) {
            java.math.BigDecimal q = ((Quantity) value).getValue();
            return q == null ? null : q.toPlainString();
        }
        if (value instanceof CodeableConcept) {
            return ((CodeableConcept) value).getCodingFirstRep().getCode();
        }
        LogEvent.logWarn(this.getClass().getSimpleName(), "observationValueAsString",
                "unsupported Observation value type for import: " + value.getClass().getSimpleName());
        return null;
    }

    private void addResultSets(Analysis analysis, Result result, IResultSaveService resultValidationSave) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "addResultSets", "creating resultSet for referral");
        Sample sample = analysis.getSampleItem().getSample();
        org.openelisglobal.patient.valueholder.Patient patient = sampleHumanService.getPatientForSample(sample);
        LogEvent.logDebug(this.getClass().getSimpleName(), "addResultSets", "got patient for referral");
        if (finalResultAlreadySent(result)) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "addResultSets", "final results already sent");
            result.setResultEvent(Event.CORRECTION);
            resultValidationSave.getModifiedResults()
                    .add(new ResultSet(result, null, null, patient, sample, null, false));
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "addResultSets", "final results not already sent");
            result.setResultEvent(Event.FINAL_RESULT);
            resultValidationSave.getNewResults().add(new ResultSet(result, null, null, patient, sample, null, false));
        }
        LogEvent.logDebug(this.getClass().getSimpleName(), "addResultSets", "referral result added to set");
    }

    // TO DO bug falsely triggered when preliminary result is sent, fails, retries
    // and succeeds
    private boolean finalResultAlreadySent(Result result) {
        if (GenericValidator.isBlankOrNull(result.getId())) {
            return false;
        }
        List<DocumentTrack> documents = documentTrackService.getByTypeRecordAndTable(RESULT_REPORT_ID, RESULT_TABLE_ID,
                result.getId());
        return documents.size() > 0;
    }

    private void recordResultForReferral(ReferralResultsImportObjects resultsImport, Analysis analysis, Result result,
            List<ReferralSet> referralSets) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "recordResultForReferral", "recording result for referral");

        ReferralSet referralSet = new ReferralSet();

        // The poll already advanced the referral to COMPLETED when the reference lab
        // returned results (OGC-803); Accept only posts the result data here, in one
        // transaction, so there is no nested-commit version bump to reconcile.
        Referral referral = referralService.getReferralByAnalysisId(analysis.getId());
        LogEvent.logDebug(this.getClass().getSimpleName(), "recordResultForReferral", "got referral for analysis");
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral(referral.getId());
        LogEvent.logDebug(this.getClass().getSimpleName(), "recordResultForReferral",
                "got referralresults for referral");
        referralSet.setExistingReferralResults(referralResults == null ? new ArrayList<>() : referralResults);
        ReferralResult referralResult = referralSet.getNextReferralResult();
        referralResult.setReferralId(referral.getId());
        referralResult.setReferralReportDate(DateUtil.getNowAsTimestamp());
        referralResult.setTestId(analysis.getTest().getId());
        referralResult.setResult(result);

        NoteService noteService = SpringContext.getBean(NoteService.class);
        Note importNote = noteService.createSavableNote(referral.getAnalysis(), NoteServiceImpl.NoteType.INTERNAL,
                "referral result imported automatically", RESULT_SUBJECT, "1");
        // Idempotency: the note carries a uniqueness constraint, so re-importing a
        // result for an analysis that already has this note would 500. Only attach it
        // when it isn't already present (updateReferralSets skips a null note).
        if (!noteService.duplicateNoteExists(importNote)) {
            referralSet.setNote(importNote);
            LogEvent.logDebug(this.getClass().getSimpleName(), "recordResultForReferral",
                    "created referral result import note");
        }
        referralSet.setReferral(referral);

        referralSets.add(referralSet);
        LogEvent.logDebug(this.getClass().getSimpleName(), "recordResultForReferral",
                "referral result added for referral");
    }
}
