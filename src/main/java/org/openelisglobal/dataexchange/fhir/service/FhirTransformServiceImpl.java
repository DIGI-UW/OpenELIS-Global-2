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
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.address.service.AddressPartService;
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
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.service.DeviceTransformService;
import org.openelisglobal.fhir.service.DiagnosticReportTransformService;
import org.openelisglobal.fhir.service.ObservationTransformService;
import org.openelisglobal.fhir.service.OrganizationTransformService;
import org.openelisglobal.fhir.service.PatientTransformService;
import org.openelisglobal.fhir.service.PractitionerTransformService;
import org.openelisglobal.fhir.service.ServiceRequestTransformService;
import org.openelisglobal.fhir.service.SpecimenTransformService;
import org.openelisglobal.fhir.service.TaskTransformService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private ReferralSetService referralSetService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private PractitionerTransformService practitionerTransformService;
    @Autowired
    private PatientTransformService patientTransformService;
    @Autowired
    private ServiceRequestTransformService serviceRequestTransformService;
    @Autowired
    private SpecimenTransformService specimenTransformService;
    @Autowired
    private ObservationTransformService observationTransformService;
    @Autowired
    private OrganizationTransformService organizationTransformService;
    @Autowired
    private DiagnosticReportTransformService diagnosticReportTransformService;

    @Autowired
    private DeviceTransformService deviceTransformService;
    @Autowired
    private TaskTransformService taskTransformService;

    @Autowired
    private FHIRTransformUtil fhirTransformUtil;

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
            org.hl7.fhir.r4.model.Patient fhirPatient = this.transformToFhirPatient(patient);
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
                Task task = this.transformToTask(sample);
                if (tasks.containsKey(task.getIdElement().getIdPart())) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                            "task collision with id: " + task.getIdElement().getIdPart());
                }
                tasks.put(task.getIdElement().getIdPart(), task);

                Optional<Task> referringTask = taskTransformService.getReferringTaskForSample(sample);
                if (referringTask.isPresent()) {
                    updateReferringTaskWithTaskInfo(referringTask.get(), task);
                    if (tasks.containsKey(referringTask.get().getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "referring task collision with id: " + referringTask.get().getIdElement().getIdPart());
                    }
                }
            }

            if (patient != null) {
                org.hl7.fhir.r4.model.Patient fhirPatient = this.transformToFhirPatient(patient);
                if (fhirPatients.containsKey(fhirPatient.getIdElement().getIdPart())) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                            "patient collision with id: " + fhirPatient.getIdElement().getIdPart());
                }
                fhirPatients.put(fhirPatient.getIdElement().getIdPart(), fhirPatient);
            }

            if (provider != null) {
                Practitioner requester = transformProviderToPractitioner(provider);
                if (requesters.containsKey(requester.getIdElement().getIdPart())) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                            "practitioner collision with id: " + requester.getIdElement().getIdPart());
                }
                requesters.put(requester.getIdElement().getIdPart(), requester);
            }

            if (sampleItems != null) {
                for (SampleItem sampleItem : sampleItems) {
                    Specimen specimen = this.transformToSpecimen(sampleItem);
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
                    if (serviceRequests.containsKey(serviceRequest.getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "serviceRequest collision with id: " + serviceRequest.getIdElement().getIdPart());
                    }
                    serviceRequests.put(serviceRequest.getIdElement().getIdPart(), serviceRequest);
                    if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                        DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis);
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
        org.hl7.fhir.r4.model.Patient patient = transformToFhirPatient(patientInfo.getPatientPK());
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
        org.hl7.fhir.r4.model.Organization fhirOrg = transformToFhirOrganization(organization);
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
        Task task = transformToTask(updateData.getSample().getId());
        this.addToOperations(fhirOperations, tempIdGenerator, task);

        Optional<Task> referringTask = taskTransformService.getReferringTaskForSample(updateData.getSample());
        if (referringTask.isPresent()) {
            updateReferringTaskWithTaskInfo(referringTask.get(), task);
            this.addToOperations(fhirOperations, tempIdGenerator, referringTask.get());
        }

        Optional<ServiceRequest> referingServiceRequest = serviceRequestTransformService
                .getReferringServiceRequestForSample(updateData.getSample());
        if (referingServiceRequest.isPresent()) {
            updateReferringServiceRequestWithSampleInfo(updateData.getSample(), referingServiceRequest.get());
            this.addToOperations(fhirOperations, tempIdGenerator, referingServiceRequest.get());
        }

        // patient - OGC-356: Environmental samples don't have a patient
        org.hl7.fhir.r4.model.Patient patient = null;
        if (patientInfo != null && !GenericValidator.isBlankOrNull(patientInfo.getPatientPK())) {
            patient = transformToFhirPatient(patientInfo.getPatientPK());
            this.addToOperations(fhirOperations, tempIdGenerator, patient);
            orderEntryObjects.patient = patient;
        }

        // requester
        if (ObjectUtils.isNotEmpty(updateData.getProvider())) {
            Practitioner requester = transformProviderToPractitioner(updateData.getProvider().getId());
            this.addToOperations(fhirOperations, tempIdGenerator, requester);
            orderEntryObjects.requester = requester;
        }

        // new organization created during order entry (free-text site)
        if (updateData.getNewOrganization() != null) {
            org.hl7.fhir.r4.model.Organization fhirOrg = transformToFhirOrganization(updateData.getNewOrganization());
            this.addToOperations(fhirOperations, tempIdGenerator, fhirOrg);
        }

        // Specimens and service requests
        for (SampleTestCollection sampleTest : updateData.getSampleItemsTests()) {
            FhirSampleEntryObjects fhirSampleEntryObjects = new FhirSampleEntryObjects();
            fhirSampleEntryObjects.specimen = transformToFhirSpecimen(sampleTest);

            // TODO collector
            // fhirSampleEntryObjects.collector =
            // transformCollectorToPractitioner(sampleTest.item.getCollector());
            fhirSampleEntryObjects.serviceRequests = transformToServiceRequests(updateData, sampleTest);

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

        if (useReferral) {
            referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);
        }
    }

    private void updateReferringTaskWithTaskInfo(Task referringTask, Task task) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "updateReferringTaskWithTaskInfo",
                "updateReferringTaskWithTaskInfo called");

        if (TaskStatus.COMPLETED.equals(task.getStatus())) {
            referringTask.setStatus(TaskStatus.COMPLETED);
            task.getOutput().forEach(outPut -> {
                referringTask.addOutput(outPut);
            });
        }
    }

    private void updateReferringServiceRequestWithSampleInfo(Sample sample, ServiceRequest serviceRequest) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "updateReferringServiceRequestWithSampleInfo",
                "updateReferringServiceRequestWithSampleInfo called");

        if (!serviceRequest.hasRequisition()) {
            serviceRequest.setRequisition(fhirTransformUtil
                    .createIdentifier(fhirConfig.getOeFhirSystem() + "/samp_labNo", sample.getAccessionNumber()));
        }
    }

    private Practitioner transformProviderToPractitioner(String providerId) {
        return transformProviderToPractitioner(providerService.get(providerId));
    }

    @Override
    public Practitioner transformProviderToPractitioner(Provider provider) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformProviderToPractitioner",
                "transformProviderToPractitioner called");
        return practitionerTransformService.transformProviderToPractitioner(provider);
    }

    private Task transformToTask(String sampleId) {
        return this.transformToTask(sampleService.get(sampleId));
    }

    private Task transformToTask(Sample sample) {
        return taskTransformService.transformToTask(sample);
    }

    @Override
    public org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId) {
        return transformToFhirPatient(patientService.get(patientId));
    }

    @Override
    public PatientManagementInfo createOePatientManagementInfo(org.hl7.fhir.r4.model.Patient fhirPatient) {

        return patientTransformService.createOePatientManagementInfo(fhirPatient);

    }

    private org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient", "transformToFhirPatient called");

        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient",
                "transforming patient with id: " + patient.getId());

        return patientTransformService.transformToFhirPatient(patient);
    }

    @Override
    public PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient) {

        return patientTransformService.transformToOpenElisPatientSearchResults(fhirPatient);
    }

    private List<ServiceRequest> transformToServiceRequests(SamplePatientUpdateData updateData,
            SampleTestCollection sampleTestCollection) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToServiceRequests",
                "transformToServiceRequests called");

        List<ServiceRequest> serviceRequestsForSampleItem = new ArrayList<>();

        for (Analysis analysis : sampleTestCollection.analysises) {
            serviceRequestsForSampleItem.add(transformToServiceRequest(analysis.getId()));
        }
        return serviceRequestsForSampleItem;
    }

    @Override
    public ServiceRequest transformToServiceRequest(String analysisId) {
        return serviceRequestTransformService.transformToServiceRequest(analysisService.get(analysisId));
    }

    private Specimen transformToFhirSpecimen(SampleTestCollection sampleTest) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirSpecimen", "transformToFhirSpecimen called");

        Specimen specimen = this.transformToSpecimen(sampleTest.item.getId());
        if (sampleTest.initialSampleConditionIdList != null) {
            for (ObservationHistory initialSampleCondition : sampleTest.initialSampleConditionIdList) {
                specimen.addCondition(
                        fhirTransformUtil.transformSampleConditionToCodeableConcept(initialSampleCondition));
            }
        }

        return specimen;
    }

    @Override
    public SampleItem createSampleItemFromSpecimen(Specimen specimen, String sysuserId) {

        return specimenTransformService.createSampleItemFromSpecimen(specimen, sysuserId);
    }

    @Override
    public Specimen transformToSpecimen(String sampleItemId) {
        return transformToSpecimen(sampleItemService.get(sampleItemId));
    }

    @SuppressWarnings("unused")
    private CodeableConcept transformSampleConditionToCodeableConcept(String sampleConditionId) {
        return fhirTransformUtil
                .transformSampleConditionToCodeableConcept(observationHistoryService.get(sampleConditionId));
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
            Observation observation = transformResultToObservation(resultSet.result.getId());
            setDeviceReferenceAndInclude(observation, resultSet.result.getAnalysis(), fhirOperations, tempIdGenerator,
                    analyzerCache, includedAnalyzerIds);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }
        for (ResultSet resultSet : actionDataSet.getModifiedResults()) {
            Observation observation = this.transformResultToObservation(resultSet.result.getId());
            setDeviceReferenceAndInclude(observation, resultSet.result.getAnalysis(), fhirOperations, tempIdGenerator,
                    analyzerCache, includedAnalyzerIds);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : actionDataSet.getModifiedAnalysis()) {
            ServiceRequest serviceRequest = this.transformToServiceRequest(analysis.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
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

    @Override
    public TestResultItem createResultFromObservation(org.hl7.fhir.r4.model.Observation observation) {

        return observationTransformService.createResultFromObservation(observation);
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
            Observation observation = transformResultToObservation(result.getId());
            observation.setStatus(ObservationStatus.CANCELLED);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Result result : resultUpdateList) {
            Observation observation = this.transformResultToObservation(result.getId());
            setDeviceReferenceAndInclude(observation, result.getAnalysis(), fhirOperations, tempIdGenerator,
                    analyzerCache, includedAnalyzerIds);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : analysisUpdateList) {
            ServiceRequest serviceRequest = this.transformToServiceRequest(analysis.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
            includeDeviceIfNeeded(analysis, fhirOperations, tempIdGenerator, analyzerCache, includedAnalyzerIds);
        }

        Map<String, Task> referingTaskMap = new HashMap<>();
        Map<String, ServiceRequest> referingServiceRequestMap = new HashMap<>();
        for (Sample sample : sampleUpdateList) {
            Task task = this.transformToTask(sample.getId());
            Optional<Task> referringTask = taskTransformService.getReferringTaskForSample(sample);
            if (referringTask.isPresent()) {
                if (referingTaskMap.containsKey(referringTask.get().getIdElement().getIdPart())) {
                    Task existingReferingTask = referingTaskMap.get(referringTask.get().getIdElement().getIdPart());
                    updateReferringTaskWithTaskInfo(existingReferingTask, task);
                    referingTaskMap.put(existingReferingTask.getIdElement().getIdPart(), existingReferingTask);
                    this.addToOperations(fhirOperations, tempIdGenerator, existingReferingTask);
                } else {
                    updateReferringTaskWithTaskInfo(referringTask.get(), task);
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
                    updateReferringServiceRequestWithSampleInfo(sample, existingServiceRequest);
                    referingServiceRequestMap.put(existingServiceRequest.getIdElement().getIdPart(),
                            existingServiceRequest);
                    this.addToOperations(fhirOperations, tempIdGenerator, existingServiceRequest);
                } else {
                    updateReferringServiceRequestWithSampleInfo(sample, referingServiceRequest.get());
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

        if (this.setTempIdIfMissing(resource, tempIdGenerator)) {
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
        observation.setDevice(fhirTransformUtil.createReferenceFor(ResourceType.Device, fhirUuid));
        if (!includedAnalyzerIds.contains(analysis.getAnalyzerId())) {
            Device device = this.transformAnalyzerToDevice(analyzer);
            this.addToOperations(fhirOperations, tempIdGenerator, device);
            includedAnalyzerIds.add(analysis.getAnalyzerId());
        }
    }

    private Device transformAnalyzerToDevice(Analyzer analyzer) {
        return deviceTransformService.transformAnalyzerToDevice(analyzer);
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
            Device device = this.transformAnalyzerToDevice(analyzer);
            this.addToOperations(fhirOperations, tempIdGenerator, device);
            includedAnalyzerIds.add(analysis.getAnalyzerId());
        }
    }

    private DiagnosticReport transformResultToDiagnosticReport(String analysisId) {
        return transformResultToDiagnosticReport(analysisService.get(analysisId));
    }

    @Override
    public DiagnosticReport transformResultToDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformResultToDiagnosticReport",
                "transformResultToDiagnosticReport called");

        return diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);
    }

    private Observation transformResultToObservation(String resultId) {
        return observationTransformService.transformResultToObservation(resultService.get(resultId));
    }

    @Override
    public Practitioner transformNameToPractitioner(String practitionerName) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformNameToPractitioner",
                "transformNameToPractitioner called");

        Practitioner practitioner = new Practitioner();
        HumanName name = practitioner.addName();

        if (practitionerName.contains(",")) {
            String[] names = practitionerName.split(",", 2);
            name.setFamily(names[0]);
            for (int i = 1; i < names.length; ++i) {
                name.addGiven(names[i]);
            }
        } else {
            String[] names = practitionerName.split(" ");
            if (names.length >= 1) {
                name.setFamily(names[names.length - 1]);
                for (int i = 0; i < names.length - 1; ++i) {
                    name.addGiven(names[i]);
                }
            }
        }
        return practitioner;
    }

    @Override
    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Organization transformToFhirOrganization(Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirOrganization",
                "transformToFhirOrganization called");

        return organizationTransformService.transformToFhirOrganization(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToOrganization", "transformToOrganization called");
        return organizationTransformService.transformToOrganization(fhirOrganization);
    }

    @Override
    public boolean setTempIdIfMissing(Resource resource, TempIdGenerator tempIdGenerator) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setTempIdIfMissing", "setTempIdIfMissing called");

        if (GenericValidator.isBlankOrNull(resource.getId())) {
            resource.setId(tempIdGenerator.getNextId());
            return true;
        }
        return false;
    }

    @Override
    public String getIdFromLocation(String location) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "getIdFromLocation", "getIdFromLocation called");

        String id = location.substring(location.indexOf("/") + 1);
        while (id.lastIndexOf("/") > 0) {
            id = id.substring(0, id.lastIndexOf("/"));
        }
        return id;
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
    public Provider transformToProvider(Practitioner practitioner) {
        return practitionerTransformService.transformToProvider(practitioner);
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
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);

            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }

        }

        fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
    }

    @Override
    public <T extends BaseObject<?>> T getItemByFhirId(String fhirUuid, BaseObjectService<T, ?> service) {
        return fhirTransformUtil.getItemByFhirId(fhirUuid, service);
    }

    @Override
    public SampleOrderItem buildSampleOrderItemFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception {
        return serviceRequestTransformService.buildSampleOrderItemFromServiceRequest(serviceRequest, sysUserId);
    }

    @Override
    public List<SampleEditItem> buildSampleEditItemsListFromServiceRequest(ServiceRequest serviceRequest,
            String sysUserId) throws Exception {
        return serviceRequestTransformService.buildSampleEditItemsListFromServiceRequest(serviceRequest, sysUserId);
    }

    @Override
    public Specimen transformToSpecimen(SampleItem sampleItem) {
        return specimenTransformService.transformToSpecimen(sampleItem);
    }

    @Override
    public Observation transformResultToObservation(Result result) throws FhirTransformationException {
        return observationTransformService.transformResultToObservation(result);

    }
}
