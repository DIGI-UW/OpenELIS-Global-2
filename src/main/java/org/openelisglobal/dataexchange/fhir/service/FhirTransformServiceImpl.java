package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.service.DeviceTransformService;
import org.openelisglobal.fhir.service.DiagnosticReportTransformService;
import org.openelisglobal.fhir.service.FhirCommonTransformService;
import org.openelisglobal.fhir.service.ObservationTransformService;
import org.openelisglobal.fhir.service.OrganizationTransformService;
import org.openelisglobal.fhir.service.PatientTransformService;
import org.openelisglobal.fhir.service.PractitionerTransformService;
import org.openelisglobal.fhir.service.ServiceRequestTransformService;
import org.openelisglobal.fhir.service.SpecimenTransformService;
import org.openelisglobal.fhir.service.TaskTransformService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.service.ReferralSetService;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates FHIR persistence for OpenELIS workflows. Resource-level mapping
 * lives in the per-resource transform services under
 * {@code org.openelisglobal.fhir.service}; this class assembles them into
 * bundles and keeps the historical {@link FhirTransformService} contract by
 * delegating.
 */
@Service
public class FhirTransformServiceImpl implements FhirTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private PatientService patientService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ReferralSetService referralSetService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private FhirCommonTransformService common;
    @Autowired
    private PatientTransformService patientTransformService;
    @Autowired
    private PractitionerTransformService practitionerTransformService;
    @Autowired
    private OrganizationTransformService organizationTransformService;
    @Autowired
    private TaskTransformService taskTransformService;
    @Autowired
    private ServiceRequestTransformService serviceRequestTransformService;
    @Autowired
    private SpecimenTransformService specimenTransformService;
    @Autowired
    private ObservationTransformService observationTransformService;
    @Autowired
    private DiagnosticReportTransformService diagnosticReportTransformService;
    @Autowired
    private DeviceTransformService deviceTransformService;

    @Transactional
    @Async
    @Override
    public AsyncResult<Bundle> transformPersistPatients(List<String> patientIds) throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistPatients",
                "transformPersistPatients called");

        FhirOperations fhirOperations = new FhirOperations();
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();

        Map<String, org.hl7.fhir.r4.model.Patient> fhirPatients = new HashMap<>();
        for (String patientId : patientIds) {
            Patient patient = patientService.get(patientId);
            if (patient.getFhirUuid() == null) {
                patient.setFhirUuid(UUID.randomUUID());
            }
            org.hl7.fhir.r4.model.Patient fhirPatient = patientTransformService.transformToFhirPatient(patient);
            if (fhirPatients.containsKey(fhirPatient.getIdElement().getIdPart())) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistPatients",
                        "patient collision with id: " + fhirPatient.getIdElement().getIdPart());
            }
            fhirPatients.put(fhirPatient.getIdElement().getIdPart(), fhirPatient);
        }

        for (org.hl7.fhir.r4.model.Patient fhirPatient : fhirPatients.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, fhirPatient);
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        return new AsyncResult<>(responseBundle);
    }

    @Transactional
    @Async
    @Override
    public AsyncResult<Bundle> transformPersistObjectsUnderSamples(List<String> sampleIds)
            throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                "transformPersistObjectsUnderSamples called");

        FhirOperations fhirOperations = new FhirOperations();
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();

        Map<String, Task> tasks = new HashMap<>();
        Map<String, org.hl7.fhir.r4.model.Patient> fhirPatients = new HashMap<>();
        Map<String, Specimen> specimens = new HashMap<>();
        Map<String, ServiceRequest> serviceRequests = new HashMap<>();
        Map<String, DiagnosticReport> diagnosticReports = new HashMap<>();
        Map<String, Observation> observations = new HashMap<>();
        Map<String, Practitioner> requesters = new HashMap<>();
        Set<String> includedAnalyzerIds = new HashSet<>();
        Map<String, Analyzer> analyzerCache = new HashMap<>();
        for (String sampleId : sampleIds) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                    "transforming sampleId: " + sampleId);
            Sample sample = sampleService.get(sampleId);
            Patient patient = sampleHumanService.getPatientForSample(sample);
            Provider provider = sampleHumanService.getProviderForSample(sample);
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sampleId);
            List<Analysis> analysises = analysisService.getAnalysesBySampleId(sampleId);
            List<Result> results = resultService.getResultsForSample(sample);

            if (sample != null && sample.getFhirUuid() == null) {
                sample.setFhirUuid(UUID.randomUUID());
            }
            if (patient != null && patient.getFhirUuid() == null) {
                patient.setFhirUuid(UUID.randomUUID());
            }
            if (provider != null && provider.getFhirUuid() == null) {
                provider.setFhirUuid(UUID.randomUUID());
            }

            if (sampleItems != null) {
                sampleItems.stream().forEach((e) -> {
                    if (e.getFhirUuid() == null) {
                        e.setFhirUuid(UUID.randomUUID());
                    }
                });
            }

            if (analysises != null) {
                analysises.stream().forEach((e) -> {
                    if (e.getFhirUuid() == null) {
                        e.setFhirUuid(UUID.randomUUID());
                    }
                });
            }

            if (results != null) {
                results.stream().forEach((e) -> {
                    if (e.getFhirUuid() == null) {
                        e.setFhirUuid(UUID.randomUUID());
                    }
                });
            }

            if (sample != null) {
                Task task = taskTransformService.transformToTask(sample);
                if (tasks.containsKey(task.getIdElement().getIdPart())) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                            "task collision with id: " + task.getIdElement().getIdPart());
                }
                tasks.put(task.getIdElement().getIdPart(), task);

                Optional<Task> referringTask = taskTransformService.getReferringTaskForSample(sample);
                if (referringTask.isPresent()) {
                    taskTransformService.updateReferringTaskWithTaskInfo(referringTask.get(), task);
                    if (tasks.containsKey(referringTask.get().getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "referring task collision with id: " + referringTask.get().getIdElement().getIdPart());
                    }
                }
            }

            if (patient != null) {
                org.hl7.fhir.r4.model.Patient fhirPatient = patientTransformService.transformToFhirPatient(patient);
                if (fhirPatients.containsKey(fhirPatient.getIdElement().getIdPart())) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                            "patient collision with id: " + fhirPatient.getIdElement().getIdPart());
                }
                fhirPatients.put(fhirPatient.getIdElement().getIdPart(), fhirPatient);
            }

            if (provider != null) {
                Practitioner requester = practitionerTransformService.transformProviderToPractitioner(provider);
                if (requesters.containsKey(requester.getIdElement().getIdPart())) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                            "practitioner collision with id: " + requester.getIdElement().getIdPart());
                }
                requesters.put(requester.getIdElement().getIdPart(), requester);
            }

            if (sampleItems != null) {
                for (SampleItem sampleItem : sampleItems) {
                    Specimen specimen = specimenTransformService.transformToSpecimen(sampleItem);
                    if (specimens.containsKey(specimen.getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "specimen collision with id: " + specimen.getIdElement().getIdPart());
                    }
                    specimens.put(specimen.getIdElement().getIdPart(), specimen);
                }
            }
            if (analysises != null) {
                for (Analysis analysis : analysises) {
                    ServiceRequest serviceRequest = serviceRequestTransformService.transformToServiceRequest(analysis);
                    if (serviceRequest == null) {
                        // transformToServiceRequest already logged the skip reason
                        // (no resolvable Sample via sample_item or vector_pool).
                        continue;
                    }
                    if (serviceRequests.containsKey(serviceRequest.getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "serviceRequest collision with id: " + serviceRequest.getIdElement().getIdPart());
                    }
                    serviceRequests.put(serviceRequest.getIdElement().getIdPart(), serviceRequest);
                    if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                        DiagnosticReport diagnosticReport = diagnosticReportTransformService
                                .transformResultToDiagnosticReport(analysis);
                        if (diagnosticReports.containsKey(analysis.getFhirUuidAsString())) {
                            LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                    "diagnosticReport collision with id: "
                                            + diagnosticReport.getIdElement().getIdPart());
                        }
                        diagnosticReports.put(analysis.getFhirUuidAsString(), diagnosticReport);
                    }
                }
            }
            if (results != null) {
                for (Result result : results) {
                    Observation observation = observationTransformService.transformResultToObservation(result);
                    if (observations.containsKey(observation.getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "observation collision with id: " + observation.getIdElement().getIdPart());
                    }
                    setDeviceReferenceAndInclude(observation, result.getAnalysis(), fhirOperations, tempIdGenerator,
                            analyzerCache, includedAnalyzerIds);
                    observations.put(observation.getIdElement().getIdPart(), observation);
                }
            }
        }

        for (Task task : tasks.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, task);
        }
        for (org.hl7.fhir.r4.model.Patient fhirPatient : fhirPatients.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, fhirPatient);
        }
        for (Specimen specimen : specimens.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, specimen);
        }
        for (ServiceRequest serviceRequest : serviceRequests.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
        }
        for (Observation observation : observations.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }
        for (DiagnosticReport diagnosticReport : diagnosticReports.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
        }
        for (Practitioner requester : requesters.values()) {
            this.addToOperations(fhirOperations, tempIdGenerator, requester);
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        return new AsyncResult<>(responseBundle);
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistPatient(PatientManagementInfo patientInfo, boolean isCreate)
            throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistPatient", "transformPersistPatient called");

        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        org.hl7.fhir.r4.model.Patient patient = patientTransformService
                .transformToFhirPatient(patientInfo.getPatientPK());
        this.addToOperations(fhirOperations, tempIdGenerator, patient);

        if (ConfigurationProperties.getInstance().getPropertyValue(Property.ENABLE_CLIENT_REGISTRY).equals("true")) {
            if (!GenericValidator.isBlankOrNull(fhirConfig.getClientRegistryServerUrl())
                    && !GenericValidator.isBlankOrNull(fhirConfig.getClientRegistryUserName())
                    && !GenericValidator.isBlankOrNull(fhirConfig.getClientRegistryPassword())) {
                IGenericClient clientRegistry = fhirUtil.getFhirClient(fhirConfig.getClientRegistryServerUrl(),
                        fhirConfig.getClientRegistryUserName(), fhirConfig.getClientRegistryPassword());
                try {
                    if (isCreate) {
                        clientRegistry.create().resource(patient).execute();
                    } else {
                        clientRegistry.update().resource(patient).execute();
                    }
                } catch (FhirClientConnectionException e) {
                    handleException(e, patientInfo.getPatientUpdateStatus());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    @Transactional
    @Async
    @Override
    public void transformPersistOrganization(Organization organization) throws FhirLocalPersistingException {
        String method = "transformPersistOrganization";
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistOrganization",
                "transformPersistOrganization called");

        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        org.hl7.fhir.r4.model.Organization fhirOrg = organizationTransformService
                .transformToFhirOrganization(organization);
        this.addToOperations(fhirOperations, tempIdGenerator, fhirOrg);
        try {
            Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Local fhirStore current unavalable");
        }
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistOrderEntryFhirObjects(SamplePatientUpdateData updateData,
            PatientManagementInfo patientInfo, boolean useReferral, List<ReferralItem> referralItems)
            throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistOrderEntryFhirObjects",
                "transformPersistOrderEntryFhirObjects called");
        LogEvent.logTrace(this.getClass().getSimpleName(), "createFhirFromSamplePatient",
                "accessionNumber - " + updateData.getAccessionNumber());
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();

        FhirOrderEntryObjects orderEntryObjects = new FhirOrderEntryObjects();
        // TODO should we create a task per service request that is part of this task so
        // we can have the ServiceRequest as the focus in those tasks?
        // task for entering the order
        Task task = taskTransformService.transformToTask(updateData.getSample().getId());
        this.addToOperations(fhirOperations, tempIdGenerator, task);

        Optional<Task> referringTask = taskTransformService.getReferringTaskForSample(updateData.getSample());
        if (referringTask.isPresent()) {
            taskTransformService.updateReferringTaskWithTaskInfo(referringTask.get(), task);
            this.addToOperations(fhirOperations, tempIdGenerator, referringTask.get());
        }

        Optional<ServiceRequest> referingServiceRequest = serviceRequestTransformService
                .getReferringServiceRequestForSample(updateData.getSample());
        if (referingServiceRequest.isPresent()) {
            serviceRequestTransformService.updateReferringServiceRequestWithSampleInfo(updateData.getSample(),
                    referingServiceRequest.get());
            this.addToOperations(fhirOperations, tempIdGenerator, referingServiceRequest.get());
        }

        // patient - OGC-356: Environmental samples don't have a patient
        org.hl7.fhir.r4.model.Patient patient = null;
        if (patientInfo != null && !GenericValidator.isBlankOrNull(patientInfo.getPatientPK())) {
            patient = patientTransformService.transformToFhirPatient(patientInfo.getPatientPK());
            this.addToOperations(fhirOperations, tempIdGenerator, patient);
            orderEntryObjects.patient = patient;
        }

        // requester
        if (ObjectUtils.isNotEmpty(updateData.getProvider())) {
            Practitioner requester = practitionerTransformService
                    .transformProviderToPractitioner(updateData.getProvider().getId());
            this.addToOperations(fhirOperations, tempIdGenerator, requester);
            orderEntryObjects.requester = requester;
        }

        // new organization created during order entry (free-text site)
        if (updateData.getNewOrganization() != null) {
            org.hl7.fhir.r4.model.Organization fhirOrg = organizationTransformService
                    .transformToFhirOrganization(updateData.getNewOrganization());
            this.addToOperations(fhirOperations, tempIdGenerator, fhirOrg);
        }

        // Specimens and service requests
        for (SampleTestCollection sampleTest : updateData.getSampleItemsTests()) {
            // Skip items removed between updateData capture and async transform —
            // notably vector_pool fan-out hard-deletes the parent SampleItem after
            // creating per-organism children.
            if (sampleTest.item == null || sampleTest.item.getId() == null
                    || sampleItemService.getMatch("id", sampleTest.item.getId()).isEmpty()) {
                continue;
            }
            FhirSampleEntryObjects fhirSampleEntryObjects = new FhirSampleEntryObjects();
            fhirSampleEntryObjects.specimen = specimenTransformService.transformToFhirSpecimen(sampleTest);

            // TODO collector
            // fhirSampleEntryObjects.collector =
            // transformCollectorToPractitioner(sampleTest.item.getCollector());
            fhirSampleEntryObjects.serviceRequests = serviceRequestTransformService
                    .transformToServiceRequests(updateData, sampleTest);

            this.addToOperations(fhirOperations, tempIdGenerator, fhirSampleEntryObjects.specimen);
            // this.addToOperations(fhirOperations, tempIdGenerator,
            // fhirSampleEntryObjects.collector);

            for (ServiceRequest serviceRequest : fhirSampleEntryObjects.serviceRequests) {
                this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            }

            orderEntryObjects.sampleEntryObjectsList.add(fhirSampleEntryObjects);
        }

        if (updateData.getProgramQuestionnaireResponse() != null) {
            updateData.getProgramQuestionnaireResponse()
                    .setId(updateData.getProgramSample().getQuestionnaireResponseUuid().toString());
            this.addToOperations(fhirOperations, tempIdGenerator, updateData.getProgramQuestionnaireResponse());
        }

        // TODO location?
        // TODO create encounter?

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);

        // S-14 / OGC-624: env/vector "Refer Out" already persisted its referrals
        // synchronously inside SamplePatientEntryServiceImpl.persistData. Guard
        // prevents the legacy save-and-FHIR-push path from running a second time
        // for those workflows.
        if (useReferral && !updateData.isReferralsPersistedSynchronously()) {
            referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);
        }
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistResultsEntryFhirObjects(ResultsUpdateDataSet actionDataSet)
            throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistResultsEntryFhirObjects",
                "transformPersistResultsEntryFhirObjects called");
        String method = "transformPersistResultsEntryFhirObjects";

        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        Set<String> includedAnalyzerIds = new HashSet<>();
        Map<String, Analyzer> analyzerCache = new HashMap<>();

        for (ResultSet resultSet : actionDataSet.getNewResults()) {
            Observation observation = observationTransformService
                    .transformResultToObservation(resultSet.result.getId());
            setDeviceReferenceAndInclude(observation, resultSet.result.getAnalysis(), fhirOperations, tempIdGenerator,
                    analyzerCache, includedAnalyzerIds);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }
        for (ResultSet resultSet : actionDataSet.getModifiedResults()) {
            Observation observation = observationTransformService
                    .transformResultToObservation(resultSet.result.getId());
            setDeviceReferenceAndInclude(observation, resultSet.result.getAnalysis(), fhirOperations, tempIdGenerator,
                    analyzerCache, includedAnalyzerIds);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : actionDataSet.getModifiedAnalysis()) {
            ServiceRequest serviceRequest = serviceRequestTransformService.transformToServiceRequest(analysis.getId());
            if (serviceRequest != null) {
                serviceRequestTransformService.preserveTerminalServiceRequestStatus(analysis, serviceRequest);
                this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            }
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = diagnosticReportTransformService
                        .transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
            includeDeviceIfNeeded(analysis, fhirOperations, tempIdGenerator, analyzerCache, includedAnalyzerIds);
        }
        try {
            Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        } catch (FhirPersistanceException e) {
            LogEvent.logError(getClass().getSimpleName(), method, "Fhir store currently un avalable");
        }
    }

    @Async
    @Override
    @Transactional(readOnly = true)
    public void transformPersistResultValidationFhirObjects(List<Result> deletableList,
            List<Analysis> analysisUpdateList, ArrayList<Result> resultUpdateList, List<AnalysisItem> resultItemList,
            ArrayList<Sample> sampleUpdateList, ArrayList<Note> noteUpdateList) throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistResultValidationFhirObjects",
                "transformPersistResultValidationFhirObjects called");

        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        Set<String> includedAnalyzerIds = new HashSet<>();
        Map<String, Analyzer> analyzerCache = new HashMap<>();

        for (Result result : deletableList) {
            Observation observation = observationTransformService.transformResultToObservation(result.getId());
            observation.setStatus(ObservationStatus.CANCELLED);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Result result : resultUpdateList) {
            Observation observation = observationTransformService.transformResultToObservation(result.getId());
            setDeviceReferenceAndInclude(observation, result.getAnalysis(), fhirOperations, tempIdGenerator,
                    analyzerCache, includedAnalyzerIds);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : analysisUpdateList) {
            ServiceRequest serviceRequest = serviceRequestTransformService.transformToServiceRequest(analysis.getId());
            if (serviceRequest != null) {
                this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            }
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = diagnosticReportTransformService
                        .transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
            includeDeviceIfNeeded(analysis, fhirOperations, tempIdGenerator, analyzerCache, includedAnalyzerIds);
        }

        Map<String, Task> referingTaskMap = new HashMap<>();
        Map<String, ServiceRequest> referingServiceRequestMap = new HashMap<>();
        for (Sample sample : sampleUpdateList) {
            Task task = taskTransformService.transformToTask(sample.getId());
            Optional<Task> referringTask = taskTransformService.getReferringTaskForSample(sample);
            if (referringTask.isPresent()) {
                if (referingTaskMap.containsKey(referringTask.get().getIdElement().getIdPart())) {
                    Task existingReferingTask = referingTaskMap.get(referringTask.get().getIdElement().getIdPart());
                    taskTransformService.updateReferringTaskWithTaskInfo(existingReferingTask, task);
                    referingTaskMap.put(existingReferingTask.getIdElement().getIdPart(), existingReferingTask);
                    this.addToOperations(fhirOperations, tempIdGenerator, existingReferingTask);
                } else {
                    taskTransformService.updateReferringTaskWithTaskInfo(referringTask.get(), task);
                    referingTaskMap.put(referringTask.get().getIdElement().getIdPart(), referringTask.get());
                    this.addToOperations(fhirOperations, tempIdGenerator, referringTask.get());
                }
            }
            Optional<ServiceRequest> referingServiceRequest = serviceRequestTransformService
                    .getReferringServiceRequestForSample(sample);
            if (referingServiceRequest.isPresent()) {
                if (referingServiceRequestMap.containsKey(referingServiceRequest.get().getIdElement().getIdPart())) {
                    ServiceRequest existingServiceRequest = referingServiceRequestMap
                            .get(referingServiceRequest.get().getIdElement().getIdPart());
                    serviceRequestTransformService.updateReferringServiceRequestWithSampleInfo(sample,
                            existingServiceRequest);
                    referingServiceRequestMap.put(existingServiceRequest.getIdElement().getIdPart(),
                            existingServiceRequest);
                    this.addToOperations(fhirOperations, tempIdGenerator, existingServiceRequest);
                } else {
                    serviceRequestTransformService.updateReferringServiceRequestWithSampleInfo(sample,
                            referingServiceRequest.get());
                    referingServiceRequestMap.put(referingServiceRequest.get().getIdElement().getIdPart(),
                            referingServiceRequest.get());
                    this.addToOperations(fhirOperations, tempIdGenerator, referingServiceRequest.get());
                }
            }
            this.addToOperations(fhirOperations, tempIdGenerator, task);
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    private void addToOperations(FhirOperations fhirOperations, TempIdGenerator tempIdGenerator, Resource resource) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "addToOperations", "addToOperations called");

        // Use composite key (resourceType/id) to prevent collisions between different
        // resource types
        String compositeKey = resource.getResourceType() + "/" + resource.getIdElement().getIdPart();

        if (common.setTempIdIfMissing(resource, tempIdGenerator)) {
            if (fhirOperations.createResources.containsKey(compositeKey)) {
                LogEvent.logWarn("", "", "collision on id: " + compositeKey);
            }
            fhirOperations.createResources.put(compositeKey, resource);
        } else {
            if (fhirOperations.updateResources.containsKey(compositeKey)) {
                LogEvent.logWarn("", "", "collision on id: " + compositeKey);
            }
            fhirOperations.updateResources.put(compositeKey, resource);
        }
    }

    /**
     * Resolves the analyzer for an analysis, sets Observation.device reference, and
     * ensures the corresponding Device resource is included in the bundle (once per
     * analyzer). Single DB lookup per unique analyzerId.
     */
    private void setDeviceReferenceAndInclude(Observation observation, Analysis analysis, FhirOperations fhirOperations,
            TempIdGenerator tempIdGenerator, Map<String, Analyzer> analyzerCache, Set<String> includedAnalyzerIds) {
        if (analysis == null || GenericValidator.isBlankOrNull(analysis.getAnalyzerId())) {
            return;
        }
        Analyzer analyzer = analyzerCache.computeIfAbsent(analysis.getAnalyzerId(), id -> analyzerService.get(id));
        if (analyzer == null) {
            return;
        }
        String fhirUuid = analyzer.ensureFhirUuid();
        observation.setDevice(common.createReferenceFor(ResourceType.Device, fhirUuid));
        if (!includedAnalyzerIds.contains(analysis.getAnalyzerId())) {
            Device device = deviceTransformService.transformAnalyzerToDevice(analyzer);
            this.addToOperations(fhirOperations, tempIdGenerator, device);
            includedAnalyzerIds.add(analysis.getAnalyzerId());
        }
    }

    /**
     * Ensures the Device resource for an analysis's analyzer is included in the
     * bundle. Use when no Observation is available (e.g., DiagnosticReport paths).
     */
    private void includeDeviceIfNeeded(Analysis analysis, FhirOperations fhirOperations,
            TempIdGenerator tempIdGenerator, Map<String, Analyzer> analyzerCache, Set<String> includedAnalyzerIds) {
        if (analysis == null || GenericValidator.isBlankOrNull(analysis.getAnalyzerId())) {
            return;
        }
        if (includedAnalyzerIds.contains(analysis.getAnalyzerId())) {
            return;
        }
        Analyzer analyzer = analyzerCache.computeIfAbsent(analysis.getAnalyzerId(), id -> analyzerService.get(id));
        if (analyzer != null) {
            Device device = deviceTransformService.transformAnalyzerToDevice(analyzer);
            this.addToOperations(fhirOperations, tempIdGenerator, device);
            includedAnalyzerIds.add(analysis.getAnalyzerId());
        }
    }

    private void handleException(FhirClientConnectionException e, IPatientUpdate.PatientUpdateStatus status)
            throws FhirClientConnectionException {
        Throwable cause = e.getCause();
        if (cause instanceof DataFormatException) {
            LogEvent.logWarn(e.getMessage(), status.name().toLowerCase(),
                    "Client Registry responds with unsupported data format!");
        } else {
            throw e;
        }
    }

    @Async
    @Override
    @Transactional(readOnly = true)
    public void transformAnalysisByIds(List<String> analysisIds)
            throws FhirTransformationException, FhirPersistanceException {
        FhirOperations fhirOperations = new FhirOperations();
        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();

        for (String analysisId : analysisIds) {
            Analysis analysis = analysisService.get(analysisId);
            ServiceRequest serviceRequest = serviceRequestTransformService.transformToServiceRequest(analysis);
            if (serviceRequest != null) {
                this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            }

            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = diagnosticReportTransformService
                        .transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }

        }

        fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    private class FhirOrderEntryObjects {
        @SuppressWarnings("unused")
        public org.hl7.fhir.r4.model.Patient patient;

        public Practitioner requester;
        List<FhirSampleEntryObjects> sampleEntryObjectsList = new ArrayList<>();
    }

    private class FhirSampleEntryObjects {
        public Practitioner collector;
        public Specimen specimen;
        public List<ServiceRequest> serviceRequests = new ArrayList<>();
    }

    @Override
    public Practitioner transformProviderToPractitioner(Provider provider) {
        return practitionerTransformService.transformProviderToPractitioner(provider);
    }

    @Override
    public org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId) {
        return patientTransformService.transformToFhirPatient(patientId);
    }

    @Override
    public PatientManagementInfo createOePatientManagementInfo(org.hl7.fhir.r4.model.Patient fhirPatient) {
        return patientTransformService.createOePatientManagementInfo(fhirPatient);
    }

    @Override
    public PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient) {
        return patientTransformService.transformToOpenElisPatientSearchResults(fhirPatient);
    }

    @Override
    public ServiceRequest transformToServiceRequest(String anlaysisId) {
        return serviceRequestTransformService.transformToServiceRequest(anlaysisId);
    }

    @Override
    public SampleItem createSampleItemFromSpecimen(Specimen specimen, String sysuserId) {
        return specimenTransformService.createSampleItemFromSpecimen(specimen, sysuserId);
    }

    @Override
    public Specimen transformToSpecimen(String sampleItemId) {
        return specimenTransformService.transformToSpecimen(sampleItemId);
    }

    @Override
    public Specimen transformToSpecimen(SampleItem sampleItem) {
        return specimenTransformService.transformToSpecimen(sampleItem);
    }

    @Override
    public TestResultItem createResultFromObservation(org.hl7.fhir.r4.model.Observation observation) {
        return observationTransformService.createResultFromObservation(observation);
    }

    @Override
    public <T extends BaseObject<?>> T getItemByFhirId(String fhirUuid, BaseObjectService<T, ?> service) {
        return common.getItemByFhirId(fhirUuid, service);
    }

    @Override
    public DiagnosticReport transformResultToDiagnosticReport(Analysis analysis) {
        return diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);
    }

    @Override
    public Analyzer transformDeviceToAnalyzer(Device device) {
        return deviceTransformService.transformDeviceToAnalyzer(device);
    }

    @Override
    public Device transformAnalyzerToDevice(Analyzer analyzer) {
        return deviceTransformService.transformAnalyzerToDevice(analyzer);
    }

    @Override
    public Observation transformResultToObservation(Result result) {
        return observationTransformService.transformResultToObservation(result);
    }

    @Override
    public Practitioner transformNameToPractitioner(String practitionerName) {
        return practitionerTransformService.transformNameToPractitioner(practitionerName);
    }

    @Override
    public org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization) {
        return organizationTransformService.transformToFhirOrganization(organization);
    }

    @Override
    public Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization) {
        return organizationTransformService.transformToOrganization(fhirOrganization);
    }

    @Override
    public boolean setTempIdIfMissing(Resource resource, TempIdGenerator tempIdGenerator) {
        return common.setTempIdIfMissing(resource, tempIdGenerator);
    }

    @Override
    public Reference createReferenceFor(Resource resource) {
        return common.createReferenceFor(resource);
    }

    @Override
    public Reference createReferenceFor(ResourceType resourceType, String id) {
        return common.createReferenceFor(resourceType, id);
    }

    @Override
    public String getIdFromLocation(String location) {
        return common.getIdFromLocation(location);
    }

    @Override
    public Identifier createIdentifier(String system, String value) {
        return common.createIdentifier(system, value);
    }

    @Override
    public void addHumanNameToPerson(HumanName humanName, Person person) {
        common.addHumanNameToPerson(humanName, person);
    }

    @Override
    public void addTelecomToPerson(List<ContactPoint> telecoms, Person person) {
        common.addTelecomToPerson(telecoms, person);
    }

    @Override
    public Provider transformToProvider(Practitioner practitioner) {
        return practitionerTransformService.transformToProvider(practitioner);
    }

    @Override
    public List<SampleEditItem> buildSampleEditItemsListFromServiceRequest(ServiceRequest serviceRequest,
            String sysUserId) throws Exception {
        return serviceRequestTransformService.buildSampleEditItemsListFromServiceRequest(serviceRequest, sysUserId);
    }

    @Override
    public SampleOrderItem buildSampleOrderItemFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception {
        return serviceRequestTransformService.buildSampleOrderItemFromServiceRequest(serviceRequest, sysUserId);
    }

    @Override
    public List<Test> resolveTestsFromCodeableConcept(CodeableConcept codeableConcept) {
        return serviceRequestTransformService.resolveTestsFromCodeableConcept(codeableConcept);
    }
}
