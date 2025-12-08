package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestIntent;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestPriority;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenCollectionComponent;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskPriority;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
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
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
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
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private NoteService noteService;
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
    private PersonAddressService personAddressService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private FhirUtil fhirUtil;

    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;

    @PostConstruct
    public void initializeGlobalVariables() {
        List<AddressPart> partList = addressPartService.getAll();
        for (AddressPart addressPart : partList) {
            if ("department".equals(addressPart.getPartName())) {
                ADDRESS_PART_DEPT_ID = addressPart.getId();
            } else if ("commune".equals(addressPart.getPartName())) {
                ADDRESS_PART_COMMUNE_ID = addressPart.getId();
            } else if ("village".equals(addressPart.getPartName())) {
                ADDRESS_PART_VILLAGE_ID = addressPart.getId();
            }
        }
    }

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

                Optional<Task> referringTask = getReferringTaskForSample(sample);
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
                    ServiceRequest serviceRequest = this.transformToServiceRequest(analysis);
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
                    Observation observation = this.transformResultToObservation(result);
                    if (observations.containsKey(observation.getIdElement().getIdPart())) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "transformPersistObjectsUnderSamples",
                                "observation collision with id: " + observation.getIdElement().getIdPart());
                    }
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
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistOrganization",
                "transformPersistOrganization called");

        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        org.hl7.fhir.r4.model.Organization fhirOrg = transformToFhirOrganization(organization);
        this.addToOperations(fhirOperations, tempIdGenerator, fhirOrg);
        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
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

        Optional<Task> referringTask = getReferringTaskForSample(updateData.getSample());
        if (referringTask.isPresent()) {
            updateReferringTaskWithTaskInfo(referringTask.get(), task);
            this.addToOperations(fhirOperations, tempIdGenerator, referringTask.get());
        }

        Optional<ServiceRequest> referingServiceRequest = getReferringServiceRequestForSample(updateData.getSample());
        if (referingServiceRequest.isPresent()) {
            updateReferringServiceRequestWithSampleInfo(updateData.getSample(), referingServiceRequest.get());
            this.addToOperations(fhirOperations, tempIdGenerator, referingServiceRequest.get());
        }

        // patient
        org.hl7.fhir.r4.model.Patient patient = transformToFhirPatient(patientInfo.getPatientPK());
        this.addToOperations(fhirOperations, tempIdGenerator, patient);
        orderEntryObjects.patient = patient;

        // requester
        if (ObjectUtils.isNotEmpty(updateData.getProvider())) {
            Practitioner requester = transformProviderToPractitioner(updateData.getProvider().getId());
            this.addToOperations(fhirOperations, tempIdGenerator, requester);
            orderEntryObjects.requester = requester;
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
            serviceRequest.setRequisition(
                    this.createIdentifier(fhirConfig.getOeFhirSystem() + "/samp_labNo", sample.getAccessionNumber()));
        }
    }

    private Optional<Task> getReferringTaskForSample(Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "getReferringTaskForSample",
                "getReferringTaskForSample called");

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());
        if (eOrders.size() > 0 && ElectronicOrderType.FHIR.equals(eOrders.get(0).getType())) {
            return fhirPersistanceService.getTaskBasedOnServiceRequest(sample.getReferringId());
        }
        return Optional.empty();
    }

    private Optional<ServiceRequest> getReferringServiceRequestForSample(Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "getReferringServiceRequestForSample",
                "getReferringServiceRequestForSample called");

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());
        if (eOrders.size() > 0 && ElectronicOrderType.FHIR.equals(eOrders.get(0).getType())) {
            return fhirPersistanceService.getServiceRequestByReferingId(sample.getReferringId());
        }
        return Optional.empty();
    }

    private Practitioner transformProviderToPractitioner(String providerId) {
        return transformProviderToPractitioner(providerService.get(providerId));
    }

    @Override
    public Practitioner transformProviderToPractitioner(Provider provider) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformProviderToPractitioner",
                "transformProviderToPractitioner called");

        Practitioner practitioner = new Practitioner();
        practitioner.setId(provider.getFhirUuidAsString());
        practitioner.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/provider_uuid", provider.getFhirUuidAsString()));
        practitioner.addName(new HumanName().setFamily(provider.getPerson().getLastName())
                .addGiven(provider.getPerson().getFirstName()));
        practitioner.setTelecom(transformToTelecom(provider.getPerson()));
        practitioner.setActive(provider.getActive());

        return practitioner;
    }

    private List<ContactPoint> transformToTelecom(Person person) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToTelecom", "transformToTelecom called");

        List<ContactPoint> contactPoints = new ArrayList<>();
        if (person.getPrimaryPhone() != null) {
            contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue(person.getPrimaryPhone())
                    .setUse(ContactPointUse.MOBILE));
        }

        if (person.getEmail() != null) {
            contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.EMAIL).setValue(person.getEmail()));
        }

        if (person.getFax() != null) {
            contactPoints.add(new ContactPoint().setSystem(ContactPointSystem.FAX).setValue(person.getFax()));
        }

        return contactPoints;
    }

    private Task transformToTask(String sampleId) {
        return this.transformToTask(sampleService.get(sampleId));
    }

    private Task transformToTask(Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToTask", "transformToTask called");

        Task task = new Task();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        List<Analysis> analysises = sampleService.getAnalysis(sample);
        task.setId(sample.getFhirUuidAsString());
        Optional<Task> referredTask = getReferringTaskForSample(sample);
        if (referredTask.isPresent()) {
            task.addPartOf(this.createReferenceFor(referredTask.get()));
            task.setIntent(TaskIntent.ORDER);
        } else {
            task.setIntent(TaskIntent.ORIGINALORDER);
        }
        if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Entered))) {
            task.setStatus(TaskStatus.READY);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Started))
                || sample.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            task.setStatus(TaskStatus.INPROGRESS);
        } else if (sample.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            task.setStatus(TaskStatus.FAILED);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.NonConforming_depricated))
                || sample.getStatusId().equals(statusService.getStatusID(AnalysisStatus.BiologistRejected))) {
            task.setStatus(TaskStatus.REJECTED);
        } else if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Finished))) {
            task.setStatus(TaskStatus.COMPLETED);
        } else {
            task.setStatus(TaskStatus.NULL);
        }
        task.setAuthoredOn(sample.getEnteredDate());
        task.setPriority(TaskPriority.ROUTINE);
        task.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_uuid", sample.getFhirUuidAsString()));
        task.addIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/order_accessionNumber",
                sample.getAccessionNumber()));

        for (Analysis analysis : analysises) {
            task.addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
            if (sample.getStatusId().equals(statusService.getStatusID(OrderStatus.Finished))) {
                task.addOutput() //
                        .setType(new CodeableConcept().addCoding(new Coding().setCode("reference"))) //
                        .setValue(
                                this.createReferenceFor(ResourceType.DiagnosticReport, analysis.getFhirUuidAsString()));
            }
        }
        task.setFor(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));

        return task;
    }

    private DateType transformToDateElement(String strDate) throws ParseException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToDateElement", "transformToDateElement called");

        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToDateElement", "transforming date " + strDate);
        if (GenericValidator.isBlankOrNull(strDate)) {
            return null;
        }
        boolean dayAmbiguous = false;
        boolean monthAmbiguous = false;
        // TODO look at this logic for detecting ambiguity
        if (strDate.contains(DateUtil.AMBIGUOUS_DATE_SEGMENT)) {
            strDate = strDate.replaceFirst(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
            dayAmbiguous = true;
        }
        if (strDate.contains(DateUtil.AMBIGUOUS_DATE_SEGMENT)) {
            strDate = strDate.replaceFirst(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
            monthAmbiguous = true;
        }
        Date birthDate = new SimpleDateFormat(DateUtil.getDateFormat()).parse(strDate);

        DateType dateType = new DateType();
        if (monthAmbiguous) {
            dateType.setValue(birthDate, TemporalPrecisionEnum.YEAR);
        } else if (dayAmbiguous) {
            dateType.setValue(birthDate, TemporalPrecisionEnum.MONTH);
        } else {
            dateType.setValue(birthDate, TemporalPrecisionEnum.DAY);
        }
        return dateType;
    }

    @Override
    public org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId) {
        return transformToFhirPatient(patientService.get(patientId));
    }

    private org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient", "transformToFhirPatient called");

        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient",
                "transforming patient with id: " + patient.getId());
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        String subjectNumber = patientService.getSubjectNumber(patient);
        String nationalId = patientService.getNationalId(patient);
        String guid = patientService.getGUID(patient);
        String stNumber = patientService.getSTNumber(patient);
        String uuid = patient.getFhirUuidAsString();
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirPatient",
                "transforming patient with id: " + patient.getId() + " fhirUuid: " + uuid);

        fhirPatient.setId(uuid);
        fhirPatient.setIdentifier(createPatientIdentifiers(subjectNumber, nationalId, stNumber, guid, uuid));

        HumanName humanName = new HumanName();
        List<HumanName> humanNameList = new ArrayList<>();
        humanName.setFamily(patient.getPerson().getLastName());
        humanName.addGiven(patient.getPerson().getFirstName());
        humanNameList.add(humanName);
        fhirPatient.setName(humanNameList);
        fhirPatient.getNameFirstRep().setUse(HumanName.NameUse.OFFICIAL);

        try {
            if (patient.getBirthDateForDisplay() != null) {
                fhirPatient.setBirthDateElement(transformToDateElement(patient.getBirthDateForDisplay()));
            }
        } catch (ParseException e) {
            LogEvent.logError("patient date unparseable '" + patient.getBirthDateForDisplay() + "'", e);
        }
        if (GenericValidator.isBlankOrNull(patient.getGender())) {
            fhirPatient.setGender(AdministrativeGender.UNKNOWN);
        } else if (patient.getGender().equalsIgnoreCase("M")) {
            fhirPatient.setGender(AdministrativeGender.MALE);
        } else {
            fhirPatient.setGender(AdministrativeGender.FEMALE);
        }
        fhirPatient.setTelecom(transformToTelecom(patient.getPerson()));

        fhirPatient.addAddress(transformToAddress(patient.getPerson()));

        return fhirPatient;
    }

    @Override
    public PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient) {
        PatientSearchResults patientSearchResults = new PatientSearchResults();

        if (fhirPatient.hasId()) {
            patientSearchResults.setPatientID(fhirPatient.getIdElement().getIdPart());
        }

        for (Identifier identifier : fhirPatient.getIdentifier()) {
            String system = identifier.getSystem();
            String value = identifier.getValue();

            if ("http://openelis-global.org/pat_nationalId".equals(system)) {
                patientSearchResults.setNationalId(value);
            } else if ("http://openelis-global.org/pat_guid".equals(system)) {
                patientSearchResults.setExternalId(value);
            } else if ("http://openelis-global.org/pat_uuid".equals(system)) {
                patientSearchResults.setGUID(value);
            }
        }

        if (!fhirPatient.getName().isEmpty()) {
            HumanName name = fhirPatient.getNameFirstRep();
            patientSearchResults.setFirstName(name.getGivenAsSingleString());
            patientSearchResults.setLastName(name.getFamily());
        }

        switch (fhirPatient.getGender()) {
        case MALE:
            patientSearchResults.setGender("M");
            break;
        case FEMALE:
            patientSearchResults.setGender("F");
            break;
        default:
            patientSearchResults.setGender(null);
            break;
        }

        if (fhirPatient.getBirthDate() != null) {
            patientSearchResults.setBirthdate(
                    DateUtil.convertTimestampToStringDate(new Timestamp(fhirPatient.getBirthDate().getTime())));
        }

        if (!fhirPatient.getTelecom().isEmpty()) {
            ContactPoint telecom = fhirPatient.getTelecomFirstRep();
            if (ContactPointSystem.PHONE.equals(telecom.getSystem())) {
                patientSearchResults.setContactPhone(telecom.getValue());
            }

            if (ContactPointSystem.EMAIL.equals(telecom.getSystem())) {
                patientSearchResults.setContactEmail(telecom.getValue());
            }
        }

        return patientSearchResults;
    }

    private Address transformToAddress(Person person) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToAddress", "transformToAddress called");

        @SuppressWarnings("unused")
        PersonAddress village = null;
        PersonAddress commune = null;
        @SuppressWarnings("unused")
        PersonAddress dept = null;
        List<PersonAddress> personAddressList = personAddressService.getAddressPartsByPersonId(person.getId());

        for (PersonAddress address : personAddressList) {
            if (address.getAddressPartId().equals(ADDRESS_PART_COMMUNE_ID)) {
                commune = address;
            } else if (address.getAddressPartId().equals(ADDRESS_PART_VILLAGE_ID)) {
                village = address;
            } else if (address.getAddressPartId().equals(ADDRESS_PART_DEPT_ID)) {
                dept = address;
            }
        }
        Address address = new Address() //
                .addLine(person.getStreetAddress()) //
                .setCity(person.getCity()) //
                // .setDistrict(value)
                .setState(person.getState()) //
                // .setPostalCode(value)
                .setCountry(person.getCountry()) //
        ;
        if (commune != null) {
            address.addLine("commune: " + commune.getValue());
        }
        return address;
    }

    private List<Identifier> createPatientIdentifiers(String subjectNumber, String nationalId, String stNumber,
            String guid, String fhirUuid) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToAddress", "transformToAddress called");

        List<Identifier> identifierList = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(subjectNumber)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_subjectNumber", subjectNumber));
        }
        if (!GenericValidator.isBlankOrNull(nationalId)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_nationalId", nationalId));
        }
        if (!GenericValidator.isBlankOrNull(stNumber)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_stNumber", stNumber));
        }
        if (!GenericValidator.isBlankOrNull(guid)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_guid", guid));
        }
        if (!GenericValidator.isBlankOrNull(fhirUuid)) {
            identifierList.add(createIdentifier(fhirConfig.getOeFhirSystem() + "/pat_uuid", fhirUuid));
        }
        return identifierList;
    }

    private List<ServiceRequest> transformToServiceRequests(SamplePatientUpdateData updateData,
            SampleTestCollection sampleTestCollection) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToServiceRequests",
                "transformToServiceRequests called");

        List<ServiceRequest> serviceRequestsForSampleItem = new ArrayList<>();

        for (Analysis analysis : sampleTestCollection.analysises) {
            serviceRequestsForSampleItem.add(this.transformToServiceRequest(analysis.getId()));
        }
        return serviceRequestsForSampleItem;
    }

    private ServiceRequest transformToServiceRequest(String anlaysisId) {
        return transformToServiceRequest(analysisService.get(anlaysisId));
    }

    private ServiceRequest transformToServiceRequest(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToServiceRequest",
                "transformToServiceRequest called");

        Sample sample = analysis.getSampleItem().getSample();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        Provider provider = sampleHumanService.getProviderForSample(sample);

        Organization organization = sampleService.getOrganizationRequester(sample,
                TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
        Organization organizationDepartment = sampleService.getOrganizationRequester(sample,
                TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID);

        Test test = analysis.getTest();
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(analysis.getFhirUuidAsString());
        serviceRequest.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/analysis_uuid", analysis.getFhirUuidAsString()));
        serviceRequest.setRequisition(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/samp_labNo",
                analysis.getSampleItem().getSample().getAccessionNumber()));
        if (organization != null) {
            serviceRequest.addLocationReference(
                    this.createReferenceFor(ResourceType.Location, organization.getFhirUuidAsString()));
        }
        if (organizationDepartment != null) {
            serviceRequest.addLocationReference(
                    this.createReferenceFor(ResourceType.Location, organizationDepartment.getFhirUuidAsString()));
        }

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());

        if (eOrders.size() <= 0) {
            serviceRequest.setIntent(ServiceRequestIntent.ORIGINALORDER);
        } else if (ElectronicOrderType.FHIR.equals(eOrders.get(eOrders.size() - 1).getType())) {
            serviceRequest.addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, sample.getReferringId()));
            serviceRequest.setIntent(ServiceRequestIntent.ORDER);
        } else if (ElectronicOrderType.HL7_V2.equals(eOrders.get(eOrders.size() - 1).getType())) {
            serviceRequest.setIntent(ServiceRequestIntent.ORDER);
        }

        if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Canceled))) {
            serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.SampleRejected))) {
            serviceRequest.setStatus(ServiceRequestStatus.ENTEREDINERROR);
        } else {
            serviceRequest.setStatus(ServiceRequestStatus.UNKNOWN);
        }
        ObservationHistory program = observationHistoryService.getObservationHistoriesBySampleIdAndType(sample.getId(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.PROGRAM));
        if (program != null && !GenericValidator.isBlankOrNull(program.getValue())) {
            serviceRequest.addCategory(transformSampleProgramToCodeableConcept(program));
        }
        serviceRequest.setPriority(ServiceRequestPriority.ROUTINE);
        serviceRequest.setCode(transformTestToCodeableConcept(test.getId()));
        serviceRequest.setAuthoredOn(new Date());
        for (Note note : noteService.getNotes(analysis)) {
            serviceRequest.addNote(transformNoteToAnnotation(note));
        }
        // TODO performer type?

        serviceRequest.addSpecimen(
                this.createReferenceFor(ResourceType.Specimen, analysis.getSampleItem().getFhirUuidAsString()));
        serviceRequest.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        if (provider != null && provider.getFhirUuid() != null) {
            serviceRequest
                    .setRequester(this.createReferenceFor(ResourceType.Practitioner, provider.getFhirUuidAsString()));
        }

        return serviceRequest;
    }

    private CodeableConcept transformSampleProgramToCodeableConcept(ObservationHistory program) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformSampleProgramToCodeableConcept",
                "transformSampleProgramToCodeableConcept called");

        CodeableConcept codeableConcept = new CodeableConcept();
        String programDisplay = "";
        String programCode = "";
        if ("D".equals(program.getValueType())) {
            Dictionary dictionary = dictionaryService.get(program.getValue());
            if (dictionary != null) {
                programCode = dictionary.getDictEntry();
                programDisplay = dictionary.getDictEntry();
            }
        } else {
            programCode = program.getValue();
            programDisplay = program.getValue();
        }
        codeableConcept
                .addCoding(new Coding(fhirConfig.getOeFhirSystem() + "/sample_program", programCode, programDisplay));
        return codeableConcept;
    }

    private CodeableConcept transformTestToCodeableConcept(String testId) {
        return transformTestToCodeableConcept(testService.get(testId));
    }

    private CodeableConcept transformTestToCodeableConcept(Test test) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformTestToCodeableConcept",
                "transformTestToCodeableConcept test called");

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept
                .addCoding(new Coding("http://loinc.org", test.getLoinc(), test.getLocalizedTestName().getEnglish()));
        return codeableConcept;
    }

    private Specimen transformToFhirSpecimen(SampleTestCollection sampleTest) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirSpecimen", "transformToFhirSpecimen called");

        Specimen specimen = this.transformToSpecimen(sampleTest.item.getId());
        if (sampleTest.initialSampleConditionIdList != null) {
            for (ObservationHistory initialSampleCondition : sampleTest.initialSampleConditionIdList) {
                specimen.addCondition(transformSampleConditionToCodeableConcept(initialSampleCondition));
            }
        }

        return specimen;
    }

    private Specimen transformToSpecimen(String sampleItemId) {
        return transformToSpecimen(sampleItemService.get(sampleItemId));
    }

    private Specimen transformToSpecimen(SampleItem sampleItem) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToSpecimen", "transformToSpecimen called");

        Specimen specimen = new Specimen();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());
        specimen.setId(sampleItem.getFhirUuidAsString());
        specimen.addIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_uuid",
                sampleItem.getFhirUuidAsString()));
        specimen.setAccessionIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_labNo",
                sampleItem.getSample().getAccessionNumber() + "-" + sampleItem.getSortOrder()));
        specimen.setStatus(SpecimenStatus.AVAILABLE);
        specimen.setType(transformTypeOfSampleToCodeableConcept(sampleItem.getTypeOfSample()));
        specimen.setReceivedTime(new Date());
        specimen.setCollection(transformToCollection(sampleItem.getCollectionDate(), sampleItem.getCollector()));

        for (Analysis analysis : analysisService.getAnalysesBySampleItem(sampleItem)) {
            specimen.addRequest(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        }
        specimen.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));

        return specimen;
    }

    @SuppressWarnings("unused")
    private CodeableConcept transformSampleConditionToCodeableConcept(String sampleConditionId) {
        return transformSampleConditionToCodeableConcept(observationHistoryService.get(sampleConditionId));
    }

    private CodeableConcept transformSampleConditionToCodeableConcept(ObservationHistory initialSampleCondition) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformSampleConditionToCodeableConcept",
                "transformSampleConditionToCodeableConcept called");

        String observationValue;
        String observationDisplay;
        if (ValueType.DICTIONARY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = dictionaryService.get(initialSampleCondition.getValue()).getDictEntry();
            observationDisplay = dictionaryService.get(initialSampleCondition.getValue()).getDictEntryDisplayValue();
        } else if (ValueType.KEY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = localizationService.get(initialSampleCondition.getValue()).getEnglish();
            observationDisplay = "";
        } else {
            observationValue = initialSampleCondition.getValue();
            observationDisplay = "";
        }

        CodeableConcept condition = new CodeableConcept();
        condition.addCoding(
                new Coding(fhirConfig.getOeFhirSystem() + "/sample_condition", observationValue, observationDisplay));
        return condition;
    }

    private SpecimenCollectionComponent transformToCollection(Timestamp collectionDate, String collector) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToCollection", "transformToCollection called");

        SpecimenCollectionComponent specimenCollectionComponent = new SpecimenCollectionComponent();
        specimenCollectionComponent.setCollected(new DateTimeType(collectionDate));
        // TODO create a collector from this info
        // specimenCollectionComponent.setCollector(collector);
        return specimenCollectionComponent;
    }

    private CodeableConcept transformTypeOfSampleToCodeableConcept(String typeOfSampleId) {
        return transformTypeOfSampleToCodeableConcept(typeOfSampleService.get(typeOfSampleId));
    }

    private CodeableConcept transformTypeOfSampleToCodeableConcept(TypeOfSample typeOfSample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformTypeOfSampleToCodeableConcept",
                "transformTypeOfSampleToCodeableConcept called");

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(new Coding(fhirConfig.getOeFhirSystem() + "/sampleType",
                typeOfSample.getLocalAbbreviation(), typeOfSample.getLocalizedName()));
        return codeableConcept;
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void transformPersistResultsEntryFhirObjects(ResultsUpdateDataSet actionDataSet)
            throws FhirLocalPersistingException {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformPersistResultsEntryFhirObjects",
                "transformPersistResultsEntryFhirObjects called");

        CountingTempIdGenerator tempIdGenerator = new CountingTempIdGenerator();
        FhirOperations fhirOperations = new FhirOperations();
        for (ResultSet resultSet : actionDataSet.getNewResults()) {
            Observation observation = transformResultToObservation(resultSet.result.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }
        for (ResultSet resultSet : actionDataSet.getModifiedResults()) {
            Observation observation = this.transformResultToObservation(resultSet.result.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : actionDataSet.getModifiedAnalysis()) {
            ServiceRequest serviceRequest = this.transformToServiceRequest(analysis.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
        }

        Bundle responseBundle = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
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

        for (Result result : deletableList) {
            Observation observation = transformResultToObservation(result.getId());
            observation.setStatus(ObservationStatus.CANCELLED);
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Result result : resultUpdateList) {
            Observation observation = this.transformResultToObservation(result.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, observation);
        }

        for (Analysis analysis : analysisUpdateList) {
            ServiceRequest serviceRequest = this.transformToServiceRequest(analysis.getId());
            this.addToOperations(fhirOperations, tempIdGenerator, serviceRequest);
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                DiagnosticReport diagnosticReport = this.transformResultToDiagnosticReport(analysis.getId());
                this.addToOperations(fhirOperations, tempIdGenerator, diagnosticReport);
            }
        }

        Map<String, Task> referingTaskMap = new HashMap<>();
        Map<String, ServiceRequest> referingServiceRequestMap = new HashMap<>();
        for (Sample sample : sampleUpdateList) {
            Task task = this.transformToTask(sample.getId());
            Optional<Task> referringTask = getReferringTaskForSample(sample);
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
            Optional<ServiceRequest> referingServiceRequest = getReferringServiceRequestForSample(sample);
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

        if (this.setTempIdIfMissing(resource, tempIdGenerator)) {
            if (fhirOperations.createResources.containsKey(resource.getIdElement().getIdPart())) {
                LogEvent.logWarn("", "",
                        "collision on id: " + resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
            }
            fhirOperations.createResources.put(resource.getIdElement().getIdPart(), resource);
        } else {
            if (fhirOperations.updateResources.containsKey(resource.getIdElement().getIdPart())) {
                LogEvent.logWarn("", "",
                        "collision on id: " + resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
            }
            fhirOperations.updateResources.put(resource.getIdElement().getIdPart(), resource);
        }
    }

    private DiagnosticReport transformResultToDiagnosticReport(String analysisId) {
        return transformResultToDiagnosticReport(analysisService.get(analysisId));
    }

    private DiagnosticReport transformResultToDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformResultToDiagnosticReport",
                "transformResultToDiagnosticReport called");

        List<Result> allResults = resultService.getResultsByAnalysis(analysis);
        SampleItem sampleItem = analysis.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());

        DiagnosticReport diagnosticReport = genNewDiagnosticReport(analysis);
        Test test = analysis.getTest();

        if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.FINAL);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.PRELIMINARY);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.PARTIAL);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.REGISTERED);
        } else {
            diagnosticReport.setStatus(DiagnosticReportStatus.UNKNOWN);
        }

        diagnosticReport
                .addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        diagnosticReport.addSpecimen(this.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        diagnosticReport.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        for (Result curResult : allResults) {
            diagnosticReport
                    .addResult(this.createReferenceFor(ResourceType.Observation, curResult.getFhirUuidAsString()));
        }
        diagnosticReport.setCode(transformTestToCodeableConcept(test.getId()));

        return diagnosticReport;
    }

    private DiagnosticReport genNewDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "genNewDiagnosticReport", "genNewDiagnosticReport called");

        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(analysis.getFhirUuidAsString());
        diagnosticReport.addIdentifier(this.createIdentifier(fhirConfig.getOeFhirSystem() + "/analysisResult_uuid",
                analysis.getFhirUuidAsString()));
        return diagnosticReport;
    }

    private Observation transformResultToObservation(String resultId) {
        return transformResultToObservation(resultService.get(resultId));
    }

    private Observation transformResultToObservation(Result result) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformResultToObservation",
                "transformResultToObservation called");

        Analysis analysis = result.getAnalysis();
        Test test = analysis.getTest();
        SampleItem sampleItem = analysis.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());
        Observation observation = new Observation();

        observation.setId(result.getFhirUuidAsString());
        observation.addIdentifier(
                this.createIdentifier(fhirConfig.getOeFhirSystem() + "/result_uuid", result.getFhirUuidAsString()));

        // TODO make sure these align with each other.
        // we may need to add detection for when result is changed and add those status
        // to list
        if (result.getAnalysis().getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            observation.setStatus(ObservationStatus.FINAL);
        } else if (result.getAnalysis().getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            LogEvent.logError(this.getClass().getSimpleName(), "transformResultToObservation",
                    "recording result for analysis that is not started.");
            observation.setStatus(ObservationStatus.UNKNOWN);
        } else {
            observation.setStatus(ObservationStatus.PRELIMINARY);
        }

        if (!GenericValidator.isBlankOrNull(result.getValue())) {
            // in case of Viral load test
            if (result.getAnalysis().getTest().getName().equalsIgnoreCase("Viral Load")) {
                Quantity quantity = new Quantity();
                long finalResult = result.getVLValueAsNumber();
                quantity.setValue(finalResult);
                quantity.setUnit(resultService.getUOM(result));
                observation.setValue(quantity);
            } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())
                    && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
                CodeableConcept codeableConcept = new CodeableConcept();
                if (dictionary.getLoincCode() != null && !dictionary.getLoincCode().isEmpty()) {
                    codeableConcept.addCoding(new Coding("http://loinc.org", dictionary.getLoincCode(),
                            dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                    : dictionary.getLocalizedDictionaryName().getEnglish()));
                }
                codeableConcept.addCoding(
                        new Coding(fhirConfig.getOeFhirSystem() + "/dictionary_entry", dictionary.getDictEntry(),
                                dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                        : dictionary.getLocalizedDictionaryName().getEnglish()));
                observation.setValue(codeableConcept);
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())
                    && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
                CodeableConcept codeableConcept = new CodeableConcept();
                if (dictionary.getLoincCode() != null && !dictionary.getLoincCode().isEmpty()) {
                    codeableConcept.addCoding(new Coding("http://loinc.org", dictionary.getLoincCode(),
                            dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                    : dictionary.getLocalizedDictionaryName().getEnglish()));
                }
                codeableConcept.addCoding(
                        new Coding(fhirConfig.getOeFhirSystem() + "/dictionary_entry", dictionary.getDictEntry(),
                                dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                        : dictionary.getLocalizedDictionaryName().getEnglish()));
                observation.setValue(codeableConcept);
            } else if (TypeOfTestResultServiceImpl.ResultType.isNumeric(result.getResultType())) {
                Quantity quantity = new Quantity();
                quantity.setValue(new BigDecimal(result.getValue(true)));
                quantity.setUnit(resultService.getUOM(result));
                observation.setValue(quantity);
            } else if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(result.getResultType())) {
                observation.setValue(new StringType(result.getValue()));
            }
        }
        observation.setCode(transformTestToCodeableConcept(test.getId()));
        observation.addBasedOn(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        observation.setSpecimen(this.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        observation.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        // observation.setIssued(result.getOriginalLastupdated());
        observation.setIssued(analysis.getReleasedDate()); // update to get Released Date instead of commpleted date
        // observation.setEffective(new
        // DateTimeType(result.getLastupdated()));
        if (analysis.getReleasedDate() != null) {
            observation.setEffective(new DateTimeType(analysis.getReleasedDate()));
        } else {
            observation.setEffective(new DateTimeType(analysis.getStartedDate()));
        }
        // observation.setIssued(new Date());
        return observation;
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

        org.hl7.fhir.r4.model.Organization fhirOrganization = new org.hl7.fhir.r4.model.Organization();
        fhirOrganization
                .setId(organization.getFhirUuid() == null ? organization.getId() : organization.getFhirUuidAsString());
        fhirOrganization.setName(organization.getOrganizationName());
        fhirOrganization.setActive(organization.getIsActive() == IActionConstants.YES ? true : false);
        this.setFhirOrganizationIdentifiers(fhirOrganization, organization);
        this.setFhirAddressInfo(fhirOrganization, organization);
        this.setFhirOrganizationTypes(fhirOrganization, organization);
        return fhirOrganization;
    }

    @Override
    @Transactional(readOnly = true)
    public Organization transformToOrganization(org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToOrganization", "transformToOrganization called");

        Organization organization = new Organization();
        organization.setOrganizationName(fhirOrganization.getName());
        organization.setIsActive(Boolean.FALSE == fhirOrganization.getActiveElement().getValue() ? IActionConstants.NO
                : IActionConstants.YES);

        setOeOrganizationIdentifiers(organization, fhirOrganization);
        setOeOrganizationAddressInfo(organization, fhirOrganization);
        setOeOrganizationTypes(organization, fhirOrganization);

        organization.setMlsLabFlag(IActionConstants.NO);
        organization.setMlsSentinelLabFlag(IActionConstants.NO);

        return organization;
    }

    private void setOeOrganizationIdentifiers(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationIdentifiers",
                "setOeOrganizationIdentifiers called");

        organization.setFhirUuid(UUID.fromString(fhirOrganization.getIdElement().getIdPart()));
        for (Identifier identifier : fhirOrganization.getIdentifier()) {
            if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_cliaNum")) {
                organization.setCliaNum(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_shortName")) {
                organization.setShortName(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_code")) {
                organization.setCode(identifier.getValue());
            } else if (identifier.getSystem().equals(fhirConfig.getOeFhirSystem() + "/org_uuid")) {
                organization.setFhirUuid(UUID.fromString(identifier.getValue()));
            }
        }
    }

    private void setFhirOrganizationIdentifiers(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirOrganizationIdentifiers",
                "setFhirOrganizationIdentifiers called");

        if (!GenericValidator.isBlankOrNull(organization.getCliaNum())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_cliaNum")
                    .setValue(organization.getCliaNum()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getShortName())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_shortName")
                    .setValue(organization.getShortName()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_code")
                    .setValue(organization.getCode()));
        }
        if (!GenericValidator.isBlankOrNull(organization.getCode())) {
            fhirOrganization.addIdentifier(new Identifier().setSystem(fhirConfig.getOeFhirSystem() + "/org_uuid")
                    .setValue(organization.getFhirUuidAsString()));
        }
    }

    private void setOeOrganizationTypes(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationTypes", "setOeOrganizationTypes called");

        Set<OrganizationType> orgTypes = new HashSet<>();
        OrganizationType orgType = null;
        for (CodeableConcept type : fhirOrganization.getType()) {
            for (Coding coding : type.getCoding()) {
                if (coding.getSystem() != null
                        && coding.getSystem().equals(fhirConfig.getOeFhirSystem() + "/orgType")) {
                    orgType = new OrganizationType();
                    orgType.setName(coding.getCode());
                    orgType.setDescription(type.getText());
                    orgType.setNameKey("org_type." + coding.getCode() + ".name");
                    orgType.setOrganizations(new HashSet<>());
                    orgType.getOrganizations().add(organization);
                    orgTypes.add(orgType);
                }
            }
        }
        organization.setOrganizationTypes(orgTypes);
    }

    private void setFhirOrganizationTypes(org.hl7.fhir.r4.model.Organization fhirOrganization,
            Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirOrganizationTypes",
                "setFhirOrganizationTypes called");

        Set<OrganizationType> orgTypes = organizationService.get(organization.getId()).getOrganizationTypes();
        for (OrganizationType orgType : orgTypes) {
            fhirOrganization.addType(new CodeableConcept() //
                    .setText(orgType.getDescription()) //
                    .addCoding(new Coding() //
                            .setSystem(fhirConfig.getOeFhirSystem() + "/orgType") //
                            .setCode(orgType.getName())));
        }
    }

    private void setOeOrganizationAddressInfo(Organization organization,
            org.hl7.fhir.r4.model.Organization fhirOrganization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setOeOrganizationAddressInfo",
                "setOeOrganizationAddressInfo called");

        organization.setStreetAddress(fhirOrganization.getAddressFirstRep().getLine().stream()
                .map(e -> e.asStringValue()).collect(Collectors.joining("\\n")));
        organization.setCity(fhirOrganization.getAddressFirstRep().getCity());
        organization.setState(fhirOrganization.getAddressFirstRep().getState());
        organization.setZipCode(fhirOrganization.getAddressFirstRep().getPostalCode());
    }

    private void setFhirAddressInfo(org.hl7.fhir.r4.model.Organization fhirOrganization, Organization organization) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "setFhirAddressInfo", "setFhirAddressInfo called");

        if (!GenericValidator.isBlankOrNull(organization.getStreetAddress())) {
            fhirOrganization.getAddressFirstRep().addLine(organization.getStreetAddress());
        }
        if (!GenericValidator.isBlankOrNull(organization.getCity())) {
            fhirOrganization.getAddressFirstRep().setCity(organization.getCity());
        }
        if (!GenericValidator.isBlankOrNull(organization.getState())) {
            fhirOrganization.getAddressFirstRep().setState(organization.getState());
        }
        if (!GenericValidator.isBlankOrNull(organization.getZipCode())) {
            fhirOrganization.getAddressFirstRep().setPostalCode(organization.getZipCode());
        }
    }

    private Annotation transformNoteToAnnotation(Note note) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformNoteToAnnotation",
                "transformNoteToAnnotation called");

        Annotation annotation = new Annotation();
        annotation.setText(note.getText());
        return annotation;
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
    public Reference createReferenceFor(Resource resource) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "createReferenceFor", "createReferenceFor called");

        if (resource == null) {
            return null;
        }
        Reference reference = new Reference(resource);
        reference.setReference(resource.getResourceType() + "/" + resource.getIdElement().getIdPart());
        return reference;
    }

    @Override
    public Reference createReferenceFor(ResourceType resourceType, String id) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "createReferenceFor", "createReferenceFor called");

        if (GenericValidator.isBlankOrNull(id)) {
            LogEvent.logWarn(this.getClass().getName(), "createReferenceFor",
                    "null or empty id used in resource:" + resourceType + "/" + id);
        }
        Reference reference = new Reference();
        reference.setReference(resourceType + "/" + id);
        return reference;
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

    @Override
    public Identifier createIdentifier(String system, String value) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "createIdentifier", "createIdentifier called");

        Identifier identifier = new Identifier();
        identifier.setValue(value);

        if (Objects.equals(system, fhirConfig.getOeFhirSystem() + "/pat_nationalId")) {
            identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        } else {
            identifier.setUse(Identifier.IdentifierUse.USUAL);
        }

        identifier.setSystem(system);
        return identifier;
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

    public void addHumanNameToPerson(HumanName humanName, Person person) {
        person.setFirstName(
                humanName.getGivenAsSingleString() == null ? "" : humanName.getGivenAsSingleString().strip());
        person.setLastName(humanName.getFamily() == null ? "" : humanName.getFamily().strip());
    }

    public void addTelecomToPerson(List<ContactPoint> telecoms, Person person) {
        for (ContactPoint contact : telecoms) {
            String contactValue = contact.getValue();
            if (ContactPointSystem.EMAIL.equals(contact.getSystem())) {
                person.setEmail(contactValue);
            } else if (ContactPointSystem.FAX.equals(contact.getSystem())) {
                person.setFax(contactValue);
            } else if (ContactPointSystem.PHONE.equals(contact.getSystem())
                    && ContactPointUse.MOBILE.equals(contact.getUse())) {
                person.setCellPhone(contactValue);
                person.setPrimaryPhone(contactValue);
            } else if (ContactPointSystem.PHONE.equals(contact.getSystem())
                    && ContactPointUse.HOME.equals(contact.getUse())) {
                person.setHomePhone(contactValue);
                if (GenericValidator.isBlankOrNull(person.getPrimaryPhone())) {
                    person.setPrimaryPhone(contactValue);
                }
            } else if (ContactPointSystem.PHONE.equals(contact.getSystem())
                    && ContactPointUse.WORK.equals(contact.getUse())) {
                person.setWorkPhone(contactValue);
                if (GenericValidator.isBlankOrNull(person.getPrimaryPhone())) {
                    person.setPrimaryPhone(contactValue);
                }
            }
        }
    }

    @Override
    public Provider transformToProvider(Practitioner practitioner) {
        Provider provider = new Provider();
        provider.setActive(practitioner.getActive());
        provider.setFhirUuid(UUID.fromString(practitioner.getIdElement().getIdPart()));

        provider.setPerson(new Person());
        addHumanNameToPerson(practitioner.getNameFirstRep(), provider.getPerson());
        addTelecomToPerson(practitioner.getTelecom(), provider.getPerson());

        return provider;
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
}
