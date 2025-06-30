package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.dataexchange.fhir.service.TaskWorker.TaskResult;
import org.openelisglobal.dataexchange.order.action.DBOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FhirApiWorkFlowServiceImpl implements FhirApiWorkflowService {

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private FhirReferralService fhirReferralService;
    @Autowired
    private ReferralService referralService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private OrganizationService organizationService;

    @Value("${org.openelisglobal.fhirstore.uri}")
    private String localFhirStorePath;

    @Value("${org.openelisglobal.remote.source.updateStatus}")
    private Optional<Boolean> remoteStoreUpdateStatus;

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedRateString = "${org.openelisglobal.remote.poll.frequency:120000}")
    public void pollForRemoteTasks() {
        processWorkflow(ResourceType.Task);
    }

    @Override
    @Async
    public void processWorkflow(ResourceType resourceType) {
        for (String remoteStorePath : fhirConfig.getRemoteStorePaths()) {
            switch (resourceType) {
            case Task:
                try {
                    beginTaskImportOrderPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow",
                            "could not process Task import workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow", e.getMessage());
                }
                try {
                    beginTaskCheckIfAcceptedPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow",
                            "could not process Task accepted workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow", e.getMessage());
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow",
                            "could not process Task result import workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow", e.getMessage());
                }
                try {
                    beginTaskImportResultsPath(remoteStorePath);
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow",
                            "could not process Task workflow using remote address: " + remoteStorePath);
                    LogEvent.logError(this.getClass().getSimpleName(), "processWorkflow", e.getMessage());
                }
            default:
            }
        }
    }

    private void beginTaskCheckIfAcceptedPath(String remoteStorePath) throws FhirLocalPersistingException {
        if (fhirConfig.getRemoteStoreIdentifier().isEmpty()) {
            return;
        }

        Map<String, Resource> updateResources = new HashMap<>();

        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        for (UUID referralTaskUuid : referralService.getSentReferralUuids()) {
            try {
                IQuery<Bundle> searchQuery = sourceFhirClient.search() //
                        .forResource(Task.class) //
                        .returnBundle(Bundle.class) //
                        .include(Task.INCLUDE_BASED_ON) // serviceRequest
                        .where(Task.STATUS.exactly().codes(TaskStatus.REQUESTED.toCode(), TaskStatus.RECEIVED.toCode())) //
                        .where(Task.RES_ID.exactly().identifier(referralTaskUuid.toString()));
                Bundle originalTasksBundle = searchQuery.execute();
                if (originalTasksBundle.hasEntry()) {
                    LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskCheckIfAcceptedPath",
                            "received bundle with " + originalTasksBundle.getEntry().size() + " entries");
                } else {
                    LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskCheckIfAcceptedPath",
                            "received bundle with 0 entries");
                }
                Map<String, Task> originalTasksById = new HashMap<>();
                for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
                    if (bundleEntry.hasResource()
                            && bundleEntry.getResource().getResourceType().equals(ResourceType.Task)) {
                        Task originalTask = (Task) bundleEntry.getResource();
                        originalTasksById.put(originalTask.getIdElement().getIdPart(), originalTask);
                    }
                }
                if (originalTasksById.size() <= 0) {
                    return;
                }

                List<Task> receivedTasks = new ArrayList<>();
                List<Task> acceptedTasks = new ArrayList<>();
                List<Task> rejectedTasks = new ArrayList<>();
                for (Entry<String, Task> taskEntry : originalTasksById.entrySet()) {
                    Optional<Task> task = fhirPersistanceService.getTaskBasedOnTask(taskEntry.getKey());
                    if (task.isPresent()) {
                        LogEvent.logTrace(FhirApiWorkFlowServiceImpl.class.getName(), "beginTaskCheckIfAcceptedPath",
                                "task " + task.get().getIdElement().getIdPart() + " has been detected as "
                                        + task.get().getStatus());
                        LogEvent.logTrace(FhirApiWorkFlowServiceImpl.class.getName(), "beginTaskCheckIfAcceptedPath",
                                "changing task " + taskEntry.getKey() + " to " + task.get().getStatus());
                        if (TaskStatus.RECEIVED.equals(task.get().getStatus())) {
                            Task taskBasedOnOrginalTask = task.get();
                            Task originalTask = taskEntry.getValue();
                            originalTask.setStatus(TaskStatus.RECEIVED);
                            updateResources.put(originalTask.getIdElement().getIdPart(), originalTask);
                            receivedTasks.add(originalTasksById.get(
                                    taskBasedOnOrginalTask.getBasedOnFirstRep().getReferenceElement().getIdPart()));
                        }
                        if (TaskStatus.ACCEPTED.equals(task.get().getStatus())) {
                            Task taskBasedOnOrginalTask = task.get();
                            Task originalTask = taskEntry.getValue();
                            originalTask.setStatus(TaskStatus.ACCEPTED);
                            updateResources.put(originalTask.getIdElement().getIdPart(), originalTask);
                            acceptedTasks.add(originalTasksById.get(
                                    taskBasedOnOrginalTask.getBasedOnFirstRep().getReferenceElement().getIdPart()));
                        }
                        if (TaskStatus.REJECTED.equals(task.get().getStatus())) {
                            Task taskBasedOnOrginalTask = task.get();
                            Task originalTask = taskEntry.getValue();
                            originalTask.setStatus(TaskStatus.REJECTED);
                            updateResources.put(originalTask.getIdElement().getIdPart(), originalTask);
                            rejectedTasks.add(originalTasksById.get(
                                    taskBasedOnOrginalTask.getBasedOnFirstRep().getReferenceElement().getIdPart()));
                        }
                    }
                }
            } catch (RuntimeException e) {
                LogEvent.logError("could not check/update state of referral with UUID: " + referralTaskUuid, e);
            }
        }

        fhirPersistanceService.updateFhirResourcesInFhirStore(updateResources);
    }

    private void beginTaskImportResultsPath(String remoteStorePath) {
        if (fhirConfig.getRemoteStoreIdentifier().isEmpty()) {
            return;
        }

        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        // IQuery<Bundle> searchQuery = sourceFhirClient.search()//
        // .forResource(Task.class)//
        // .returnBundle(Bundle.class)//
        // .include(Task.INCLUDE_BASED_ON) // serviceRequest
        // .where(Task.STATUS.exactly().code(TaskStatus.ACCEPTED.toCode()))//
        // .where(Task.REQUESTER.hasAnyOfIds(remoteStoreIdentifier));
        for (UUID referralTaskUuid : referralService.getSentReferralUuids()) {
            LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportResultsPath",
                    "searching for results for Task ID " + referralTaskUuid);
            try {
                IQuery<Bundle> searchQuery = sourceFhirClient.search() //
                        .forResource(Task.class) //
                        .returnBundle(Bundle.class) //
                        .include(Task.INCLUDE_BASED_ON) // serviceRequest
                        .include(ServiceRequest.INCLUDE_REQUESTER.asRecursive()) // serviceRequest
                        .where(Task.STATUS.exactly().code(TaskStatus.ACCEPTED.toCode())) //
                        .where(Task.RES_ID.exactly().identifier(referralTaskUuid.toString()));
                Bundle originalTasksBundle = searchQuery.execute();
                if (originalTasksBundle.hasEntry()) {
                    LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportResultsPath",
                            "received bundle with " + originalTasksBundle.getEntry().size() + " entries for Task ID "
                                    + referralTaskUuid);
                } else {
                    LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportResultsPath",
                            "received bundle with 0 entries for Task ID " + referralTaskUuid);
                }
                Map<String, OriginalReferralObjects> originalReferralObjectsByServiceRequest = new HashMap<>();
                for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
                    if (bundleEntry.hasResource()) {
                        try {
                            addOriginalReferralObject(bundleEntry, originalReferralObjectsByServiceRequest);
                        } catch (RuntimeException e) {
                            LogEvent.logError("could not import result for: " + bundleEntry.getResource().getId(), e);
                        }
                    }
                }
                if (originalReferralObjectsByServiceRequest.size() > 0) {
                    searchQuery = sourceFhirClient.search() //
                            .forResource(ServiceRequest.class) //
                            .returnBundle(Bundle.class) //
                            // .revInclude(Task.INCLUDE_BASED_ON)//
                            // .include(ServiceRequest.INCLUDE_SPECIMEN) // specimen
                            .revInclude(Observation.INCLUDE_BASED_ON.asRecursive()) //
                            .revInclude(DiagnosticReport.INCLUDE_BASED_ON.asRecursive()) //
                            .where(ServiceRequest.STATUS.exactly().code(ServiceRequestStatus.COMPLETED.toCode()))
                            .where(ServiceRequest.BASED_ON
                                    .hasAnyOfIds(originalReferralObjectsByServiceRequest.keySet()));
                    originalTasksBundle = searchQuery.execute();

                    Map<String, ReferralResultsImportObjects> resultImportByServiceRequest = new HashMap<>();
                    for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
                        if (bundleEntry.hasResource()) {
                            try {
                                addResultImportObject(bundleEntry, resultImportByServiceRequest,
                                        originalReferralObjectsByServiceRequest);
                            } catch (RuntimeException e) {
                                LogEvent.logError(e);
                                LogEvent.logError("could not import result for: " + bundleEntry.getResource().getId(),
                                        e);
                            }
                        }
                    }

                    for (Entry<String, ReferralResultsImportObjects> resultsImportEntry : resultImportByServiceRequest
                            .entrySet()) {
                        try {
                            fhirReferralService.setReferralResult(resultsImportEntry.getValue());
                        } catch (RuntimeException e) {
                            LogEvent.logError(e);
                            LogEvent.logError(
                                    "could not import result for ServiceRequest: " + resultsImportEntry.getKey(), e);
                        }
                    }
                }

            } catch (RuntimeException e) {
                LogEvent.logError(e);
                LogEvent.logError("could not import result for referral with UUID: " + referralTaskUuid, e);
            }
        }
    }

    private void addOriginalReferralObject(BundleEntryComponent bundleEntry,
            Map<String, OriginalReferralObjects> originalReferralObjectsByServiceRequest) {
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.ServiceRequest)) {
            ServiceRequest originalServiceRequest = (ServiceRequest) bundleEntry.getResource();
            String serviceRequestId = originalServiceRequest.getIdElement().getIdPart();
            OriginalReferralObjects referralObjects = originalReferralObjectsByServiceRequest
                    .getOrDefault(serviceRequestId, new OriginalReferralObjects());
            referralObjects.serviceRequests = Arrays.asList(originalServiceRequest);
            originalReferralObjectsByServiceRequest.put(serviceRequestId, referralObjects);
        }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.QuestionnaireResponse)) {
            QuestionnaireResponse originalQResponse = (QuestionnaireResponse) bundleEntry.getResource();
            String serviceRequestId = originalQResponse.getBasedOnFirstRep().getReferenceElement().getIdPart();
            OriginalReferralObjects referralObjects = originalReferralObjectsByServiceRequest
                    .getOrDefault(serviceRequestId, new OriginalReferralObjects());
            referralObjects.questionnaireResponses = Arrays.asList(originalQResponse);
            originalReferralObjectsByServiceRequest.put(serviceRequestId, referralObjects);
        }
        // used wrong id to match so commented out
        // if
        // (bundleEntry.getResource().getResourceType().equals(ResourceType.Practitioner))
        // {
        // Practitioner originalPractitioner = (Practitioner) bundleEntry.getResource();
        // String practitionerId = originalPractitioner.getIdElement().getIdPart();
        // OriginalReferralObjects referralObjects =
        // originalReferralObjectsByServiceRequest
        // .getOrDefault(practitionerId, new OriginalReferralObjects());
        // referralObjects.requestors = Arrays.asList(originalPractitioner);
        // originalReferralObjectsByServiceRequest.put(practitionerId, referralObjects);
        // }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.Task)) {
            Task referralTask = (Task) bundleEntry.getResource();
            String serviceRequestId = referralTask.getBasedOnFirstRep().getReferenceElement().getIdPart();
            OriginalReferralObjects referralObjects = originalReferralObjectsByServiceRequest
                    .getOrDefault(serviceRequestId, new OriginalReferralObjects());
            referralObjects.task = referralTask;
            originalReferralObjectsByServiceRequest.put(serviceRequestId, referralObjects);
        }
    }

    private void addResultImportObject(BundleEntryComponent bundleEntry,
            Map<String, ReferralResultsImportObjects> resultImportByServiceRequest,
            Map<String, OriginalReferralObjects> originalReferralObjectsByServiceRequest) {
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.ServiceRequest)) {
            ServiceRequest srBasedOnOriginalSR = (ServiceRequest) bundleEntry.getResource();
            String srId = srBasedOnOriginalSR.getIdElement().getIdPart();
            ReferralResultsImportObjects importObjects = resultImportByServiceRequest.getOrDefault(srId,
                    new ReferralResultsImportObjects());
            importObjects.serviceRequest = srBasedOnOriginalSR;
            importObjects.originalReferralObjects = originalReferralObjectsByServiceRequest
                    .get(srBasedOnOriginalSR.getBasedOnFirstRep().getReferenceElement().getIdPart());

            resultImportByServiceRequest.putIfAbsent(srId, importObjects);
        }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.Observation)) {
            Observation observation = (Observation) bundleEntry.getResource();

            String srId = observation.getBasedOnFirstRep().getReferenceElement().getIdPart();
            ReferralResultsImportObjects importObjects = resultImportByServiceRequest.getOrDefault(srId,
                    new ReferralResultsImportObjects());
            importObjects.observations.add(observation);
            resultImportByServiceRequest.putIfAbsent(srId, importObjects);
        }
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.DiagnosticReport)) {
            DiagnosticReport diagnosticReport = (DiagnosticReport) bundleEntry.getResource();

            String srId = diagnosticReport.getBasedOnFirstRep().getReferenceElement().getIdPart();
            ReferralResultsImportObjects importObjects = resultImportByServiceRequest.getOrDefault(srId,
                    new ReferralResultsImportObjects());
            importObjects.diagnosticReport = diagnosticReport;
            resultImportByServiceRequest.putIfAbsent(srId, importObjects);
        }
    }

    private void beginTaskImportOrderPath(String remoteStorePath) {
        if (fhirConfig.getRemoteStoreIdentifier().isEmpty()) {
            return;
        }

        List<Bundle> importBundles = new ArrayList<>();
        LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportOrderPath", "searching for Tasks");
        IGenericClient sourceFhirClient = fhirUtil.getFhirClient(remoteStorePath);
        IQuery<Bundle> searchQuery = sourceFhirClient.search() //
                .forResource(Task.class) //
                .returnBundle(Bundle.class) //
                // TODO use include
                // .include(Task.INCLUDE_PATIENT)//
                // .include(Task.INCLUDE_BASED_ON)//
                .where(Task.STATUS.exactly().code(TaskStatus.REQUESTED.toCode())) //
                .where(Task.OWNER.hasAnyOfIds(fhirConfig.getRemoteStoreIdentifier()));
        Bundle importBundle = searchQuery.execute();
        importBundles.add(importBundle);
        if (importBundle.hasEntry()) {
            LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportOrderPath",
                    "received bundle with " + importBundle.getEntry().size() + " entries");
        } else {
            LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportOrderPath",
                    "received bundle with 0 entries");
        }

        while (importBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportOrderPath", "following next link");
            importBundle = sourceFhirClient.loadPage().next(importBundle).execute();
            importBundles.add(importBundle);
            if (importBundle.hasEntry()) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportOrderPath",
                        "received bundle with " + importBundle.getEntry().size() + " entries");
            } else {
                LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskImportOrderPath",
                        "received bundle with 0 entries");
            }
        }

        for (Bundle bundle : importBundles) {
            for (BundleEntryComponent bundleComponent : bundle.getEntry()) {
                if (bundleComponent.hasResource()
                        && ResourceType.Task.equals(bundleComponent.getResource().getResourceType())) {

                    Task remoteTask = (Task) bundleComponent.getResource();
                    try {
                        processTaskImportOrder(remoteTask, remoteStorePath, sourceFhirClient, bundle);
                    } catch (RuntimeException | FhirLocalPersistingException e) {
                        LogEvent.logError(e);
                        LogEvent.logError(this.getClass().getSimpleName(), "beginTaskImportOrderPath",
                                "could not process Task with identifier : " + remoteTask.getId());
                    }
                }
            }
        }
    }

    private void processTaskImportOrder(Task remoteTask, String remoteStorePath, IGenericClient sourceFhirClient,
            Bundle bundle) throws FhirLocalPersistingException {
        // TODO use fhirPersistenceService
        // should contain the Patient, the ServiceRequest, and the Task
        OriginalReferralObjects localObjects = saveRemoteTaskAsLocalTask(sourceFhirClient, remoteTask, bundle,
                remoteStorePath);

        Task taskBasedOnRemoteTask = getLocalTaskBasedOnTask(remoteTask, remoteStorePath);
        if (taskBasedOnRemoteTask == null) {
            taskBasedOnRemoteTask = saveTaskBasedOnRemoteTask(sourceFhirClient, remoteTask, bundle, remoteStorePath);
        }

        List<ServiceRequest> serviceRequestList = localObjects.serviceRequests;
        Patient patient = localObjects.patient;

        if (patient == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processTaskImportOrder",
                    "no local patients found for Task " + localObjects.task.getId());
        }
        if (serviceRequestList.size() == 0) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processTaskImportOrder",
                    "no local serviceRequests found for Task " + localObjects.task.getId());
        }

        TaskResult taskResult = null;
        if (localObjects.task.getStatus() == null || localObjects.task.getStatus().equals(TaskStatus.REQUESTED)) {
            Boolean taskOrderAcceptedFlag = false;
            for (ServiceRequest serviceRequest : serviceRequestList) {

                TaskWorker worker = new TaskWorker(remoteTask,
                        fhirContext.newJsonParser().encodeResourceToString(remoteTask), serviceRequest, patient);

                worker.setInterpreter(SpringContext.getBean(TaskInterpreter.class));
                worker.setExistanceChecker(SpringContext.getBean(DBOrderExistanceChecker.class));
                worker.setPersister(SpringContext.getBean(IOrderPersister.class));

                taskResult = worker.handleOrderRequest();
                if (taskResult == TaskResult.OK) {
                    taskOrderAcceptedFlag = true; // at least one order was accepted per Piotr 5/14/2020
                }
            }

            TaskStatus taskStatus = taskOrderAcceptedFlag ? TaskStatus.ACCEPTED : TaskStatus.REJECTED;
            localObjects.task.setStatus(taskStatus);
            if (remoteStoreUpdateStatus.isPresent() && remoteStoreUpdateStatus.get()) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "beginTaskPath",
                        "updating remote status to " + taskStatus);
                remoteTask.setStatus(taskStatus);
                sourceFhirClient.update().resource(remoteTask).execute();
            }
            IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
            localFhirClient.update().resource(localObjects.task).execute();
            // taskBasedOnRemoteTask.setStatus(taskStatus);
            // localFhirClient.update().resource(taskBasedOnRemoteTask).execute();
        }
    }

    private Task getLocalTaskBasedOnTask(Task remoteTask, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search().forResource(Task.class)
                .where(Task.BASED_ON
                        .hasAnyOfIds(ResourceType.Task.toString() + "/" + remoteTask.getIdElement().getIdPart()))
                .returnBundle(Bundle.class).execute();
        return (Task) localBundle.getEntryFirstRep().getResource();
    }

    private Task saveTaskBasedOnRemoteTask(IGenericClient fhirClient, Task remoteTask, Bundle bundle,
            String remoteStorePath) {
        Task taskBasedOnRemoteTask = new Task();
        taskBasedOnRemoteTask.setId(UUID.randomUUID().toString());
        Reference reference = new Reference();
        String referenceString = remoteStorePath;
        if (!referenceString.endsWith("/")) {
            referenceString = referenceString + "/";
        }
        referenceString = referenceString + ResourceType.Task.toString() + "/" + remoteTask.getIdElement().getIdPart();
        if (referenceString.endsWith("null")) {
            LogEvent.logWarn(this.getClass().getName(), "saveTaskBasedOnRemoteTask",
                    "remote task has a null identifier: " + remoteTask.getId());
        }
        reference.setReference(referenceString);
        taskBasedOnRemoteTask.addBasedOn(reference);

        MethodOutcome outcome = fhirUtil.getFhirClient(localFhirStorePath).update().resource(taskBasedOnRemoteTask)
                .execute();

        return (Task) outcome.getResource();
    }

    private OriginalReferralObjects saveRemoteTaskAsLocalTask(IGenericClient sourceFhirClient, Task remoteTask,
            Bundle bundle, String remoteStorePath) throws FhirLocalPersistingException {
        FhirOperations fhirOperations = new FhirOperations();
        OriginalReferralObjects objects = new OriginalReferralObjects();

        List<ServiceRequest> remoteServiceRequests = getBasedOnServiceRequestsFromServer(sourceFhirClient, remoteTask);
        List<QuestionnaireResponse> remoteQResponses = getQuestionnaireResponsesForServiceRequestsFromServer(
                sourceFhirClient, remoteServiceRequests);
        List<Specimen> remoteSpecimens = getSpecimenForServiceRequestsFromServer(sourceFhirClient,
                remoteServiceRequests);
        List<Practitioner> remoteRequesters = getRequestorsForServiceRequestsFromServer(sourceFhirClient,
                remoteServiceRequests);
        Patient remotePatientForTask = getForPatientFromServer(sourceFhirClient, remoteTask);
        Location remoteTaskLocation = getTaskLocationFromServer(sourceFhirClient, remoteTask);
        if (remoteTaskLocation != null) {
            Organization localOrganization = organizationService
                    .getOrganizationByFhirId(remoteTaskLocation.getIdElement().getIdPart());
            if (localOrganization == null) {
                Organization newOrg = new Organization();
                if (remoteTaskLocation.hasName()) {
                    newOrg.setOrganizationName(remoteTaskLocation.getName());
                }
                newOrg.setFhirUuid(UUID.fromString(remoteTaskLocation.getIdElement().getIdPart()));
                newOrg.setIsActive(IActionConstants.YES);
                newOrg.setMlsLabFlag(IActionConstants.NO);
                newOrg.setMlsSentinelLabFlag(IActionConstants.NO);
                organizationService.save(newOrg);
                organizationService.linkOrganizationAndType(newOrg, TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
            }
        }
        if (remotePatientForTask == null) {
            remotePatientForTask = getForPatientFromServer(sourceFhirClient, remoteServiceRequests);
        }
        Practitioner remotePractitionerForTask = getPractitionerFromServer(sourceFhirClient, remoteTask);
        String originalRemoteTaskId = remoteTask.getIdElement().getIdPart();
        Optional<Task> existingLocalTask = getTaskWithSameIdentifier(remoteTask, remoteStorePath);
        if (existingLocalTask.isEmpty()) {
            objects.task = remoteTask.addIdentifier(createIdentifierToRemoteResource(remoteTask, remoteStorePath));
            // localTask.setStatus(TaskStatus.ACCEPTED);
            // localTask.setId(UUID.randomUUID().toString());
            fhirOperations.updateResources.put(objects.task.getIdElement().getIdPart(), objects.task);
        } else {
            objects.task = existingLocalTask.get();
        }

        // if (localTask.hasEncounter()) {
        // replaceLocalReferenceWithAbsoluteReference(remoteStorePath,
        // localTask.getEncounter());
        // }
        // if (localTask.hasOwner()) {
        // replaceLocalReferenceWithAbsoluteReference(remoteStorePath,
        // localTask.getOwner());
        // }

        // ServiceRequests
        objects.serviceRequests = new ArrayList<>();
        for (ServiceRequest remoteBasedOn : remoteServiceRequests) {
            ServiceRequest localServiceRequest;
            Optional<ServiceRequest> existingLocalBasedOn = getServiceRequestWithSameIdentifier(remoteBasedOn,
                    remoteStorePath);
            if (existingLocalBasedOn.isEmpty()) {
                localServiceRequest = remoteBasedOn
                        .addIdentifier(createIdentifierToRemoteResource(remoteBasedOn, remoteStorePath));
                // fhirOperations.createResources.add(localServiceRequest);
                fhirOperations.updateResources.put(localServiceRequest.getIdElement().getIdPart(), localServiceRequest);
            } else {
                localServiceRequest = existingLocalBasedOn.get();
            }

            //
            // localTask.addBasedOn(fhirTransformService.createReferenceFor(localServiceRequest));
            objects.serviceRequests.add(localServiceRequest);
        }

        // QuestionnaireResponse
        objects.questionnaireResponses = new ArrayList<>();
        for (QuestionnaireResponse qResponse : remoteQResponses) {
            QuestionnaireResponse localQResponse = qResponse;
            fhirOperations.updateResources.put(localQResponse.getIdElement().getIdPart(), localQResponse);
            // questionnaireResponse only has one identifier so we can't add identifiers to
            // the
            // list like other objects
            // Optional<QuestionnaireResponse> existingLocalBasedOn =
            // getQuestionnaireResponseWithSameIdentifier(qResponse,
            // remoteStorePath);
            // if (existingLocalBasedOn.isEmpty()) {
            // localQResponse = qResponse
            // .addIdentifier(createIdentifierToRemoteResource(qResponse,
            // remoteStorePath));
            //// fhirOperations.createResources.add(localServiceRequest);
            //
            // fhirOperations.updateResources.put(localQResponse.getIdElement().getIdPart(),
            // localQResponse);
            // } else {
            // localQResponse = existingLocalBasedOn.get();
            // }

            //
            // localTask.addBasedOn(fhirTransformService.createReferenceFor(localServiceRequest));
            objects.questionnaireResponses.add(localQResponse);
        }

        // Specimen
        objects.specimens = new ArrayList<>();
        for (Specimen remoteSpecimen : remoteSpecimens) {
            Specimen localSpecimen;
            Optional<Specimen> existingLocalSpecimen = getSpecimenWithSameIdentifier(remoteSpecimen, remoteStorePath);
            if (existingLocalSpecimen.isEmpty()) {
                localSpecimen = remoteSpecimen
                        .addIdentifier(createIdentifierToRemoteResource(remoteSpecimen, remoteStorePath));
                // fhirOperations.createResources.add(localSpecimen);
                fhirOperations.updateResources.put(localSpecimen.getIdElement().getIdPart(), localSpecimen);
            } else {
                localSpecimen = existingLocalSpecimen.get();
            }
            objects.specimens.add(localSpecimen);
        }

        // Patient
        // Patient forPatient = getForPatientFromBundle(bundle, remoteTask);
        Optional<Patient> existingLocalPatient = getPatientWithSameServiceIdentifier(remotePatientForTask,
                remoteStorePath);
        if (existingLocalPatient.isEmpty()) {
            objects.patient = remotePatientForTask
                    .addIdentifier(createIdentifierToRemoteResource(remotePatientForTask, remoteStorePath));
            // localForPatient.addLink(new
            // PatientLinkComponent().setType(LinkType.SEEALSO).setOther(patient));
            // fhirOperations.createResources.add(localPatient);
            fhirOperations.updateResources.put(objects.patient.getIdElement().getIdPart(), objects.patient);
        } else {
            objects.patient = existingLocalPatient.get();
            // patient already exists so we should update the reference to ours
        }
        objects.task.setFor(fhirTransformService.createReferenceFor(objects.patient));
        for (ServiceRequest serviceRequest : objects.serviceRequests) {
            serviceRequest.setSubject(fhirTransformService.createReferenceFor(objects.patient));
        }

        // Task Practitioner
        // Patient forPatient = getForPatientFromBundle(bundle, remoteTask);
        if (remotePractitionerForTask != null) {
            Optional<Practitioner> existingLocalPractitioner = getPractitionerWithSameServiceIdentifier(
                    remotePractitionerForTask, remoteStorePath);
            if (existingLocalPractitioner.isEmpty()) {
                objects.practitioner = remotePractitionerForTask
                        .addIdentifier(createIdentifierToRemoteResource(remotePractitionerForTask, remoteStorePath));
                // fhirOperations.createResources.add(localPractitioner);
                fhirOperations.updateResources.put(objects.practitioner.getIdElement().getIdPart(),
                        objects.practitioner);
            } else {
                objects.practitioner = existingLocalPractitioner.get();
                // Practitioner already exists so we should update the reference to ours
            }
            objects.task.setRequester(fhirTransformService.createReferenceFor(objects.practitioner));
        }

        // ServiceRequest Requester
        objects.requestors = new ArrayList<>();
        for (Practitioner remotePractitioner : remoteRequesters) {
            Practitioner localPractitioner;
            Optional<Practitioner> existingLocalRequester = getProviderWithSameIdentifier(remotePractitioner,
                    remoteStorePath);
            if (existingLocalRequester.isEmpty()) {
                localPractitioner = remotePractitioner
                        .addIdentifier(createIdentifierToRemoteResource(remotePractitioner, remoteStorePath));
                // fhirOperations.createResources.add(localSpecimen);
                fhirOperations.updateResources.put(localPractitioner.getIdElement().getIdPart(), localPractitioner);
            } else {
                localPractitioner = existingLocalRequester.get();
            }
            objects.requestors.add(localPractitioner);
        }

        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "creating local copies of the remote fhir objects relating to Task: " + originalRemoteTaskId);
        // Run the transaction
        fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        return objects;
    }

    private Optional<Specimen> getSpecimenWithSameIdentifier(Specimen specimen, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search() //
                .forResource(Specimen.class) //
                .where(Specimen.IDENTIFIER.exactly().systemAndIdentifier(remoteStorePath,
                        specimen.getIdElement().getIdPart())) //
                .returnBundle(Bundle.class).execute();
        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.Specimen.equals(entry.getResource().getResourceType())) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "",
                        "found specimen with same identifier as " + specimen.getIdElement().getIdPart());
                return Optional.of((Specimen) localBundle.getEntryFirstRep().getResource());
            }
        }
        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "no specimen with same identifier " + specimen.getIdElement().getIdPart());
        return Optional.empty();
    }

    private Optional<Practitioner> getProviderWithSameIdentifier(Practitioner provider, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search() //
                .forResource(Practitioner.class) //
                .where(Practitioner.IDENTIFIER.exactly().systemAndIdentifier(remoteStorePath,
                        provider.getIdElement().getIdPart())) //
                .returnBundle(Bundle.class).execute();
        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.Practitioner.equals(entry.getResource().getResourceType())) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "",
                        "found provider with same identifier as " + provider.getIdElement().getIdPart());
                return Optional.of((Practitioner) localBundle.getEntryFirstRep().getResource());
            }
        }
        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "no provider with same identifier " + provider.getIdElement().getIdPart());
        return Optional.empty();
    }

    private List<QuestionnaireResponse> getQuestionnaireResponsesForServiceRequestsFromServer(IGenericClient fhirClient,
            List<ServiceRequest> remoteServiceRequests) {
        List<QuestionnaireResponse> questionnaireResponses = new ArrayList<>();
        for (ServiceRequest serviceRequest : remoteServiceRequests) {
            Bundle searchBundle = fhirClient.search() //
                    .forResource(QuestionnaireResponse.class) //
                    .where(QuestionnaireResponse.BASED_ON
                            .hasId(ResourceType.ServiceRequest + "/" + serviceRequest.getIdElement().getIdPart()))
                    .returnBundle(Bundle.class).execute();
            for (BundleEntryComponent entry : searchBundle.getEntry()) {
                if (entry.hasResource()
                        && ResourceType.QuestionnaireResponse.equals(entry.getResource().getResourceType())) {
                    questionnaireResponses.add((QuestionnaireResponse) entry.getResource());
                }
            }
        }
        return questionnaireResponses;
    }

    private List<Specimen> getSpecimenForServiceRequestsFromServer(IGenericClient fhirClient,
            List<ServiceRequest> remoteServiceRequests) {
        List<Specimen> specimens = new ArrayList<>();
        for (ServiceRequest serviceRequest : remoteServiceRequests) {
            for (Reference specimenReference : serviceRequest.getSpecimen()) {
                specimens.add(fhirClient.read().resource(Specimen.class)
                        .withId(specimenReference.getReferenceElement().getIdPart()).execute());
            }
        }
        return specimens;
    }

    private List<Practitioner> getRequestorsForServiceRequestsFromServer(IGenericClient fhirClient,
            List<ServiceRequest> remoteServiceRequests) {
        List<Practitioner> specimens = new ArrayList<>();
        for (ServiceRequest serviceRequest : remoteServiceRequests) {
            if (!GenericValidator.isBlankOrNull(serviceRequest.getRequester().getReferenceElement().getIdPart())
                    && serviceRequest.getRequester().getReference().contains(ResourceType.Practitioner.toString())) {
                Reference requesterReference = serviceRequest.getRequester();
                specimens.add(fhirClient.read().resource(Practitioner.class)
                        .withId(requesterReference.getReferenceElement().getIdPart()).execute());
            }
        }
        return specimens;
    }

    private Patient getForPatientFromServer(IGenericClient fhirClient, List<ServiceRequest> serviceRequests) {
        for (ServiceRequest serviceRequest : serviceRequests) {
            if (serviceRequest.getSubject() != null && serviceRequest.getSubject().getReference() != null) {
                return fhirClient.read().resource(Patient.class)
                        .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()).execute();
            }
        }
        return null;
    }

    private Identifier createIdentifierToRemoteResource(IDomainResource remoteResource, String remoteStorePath) {
        Identifier identifier = new Identifier();
        identifier.setSystem(remoteStorePath);
        identifier.setType(new CodeableConcept()
                .addCoding(new Coding().setCode("externalId").setSystem(fhirConfig.getOeFhirSystem() + "/genIdType")));
        identifier.setValue(remoteResource.getIdElement().getIdPart());
        return identifier;
    }

    private Optional<Task> getTaskWithSameIdentifier(Task remoteTask, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search() //
                .forResource(Task.class) //
                .where(Task.IDENTIFIER.exactly().systemAndIdentifier(remoteStorePath,
                        remoteTask.getIdElement().getIdPart())) //
                .returnBundle(Bundle.class).execute();
        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.Task.equals(entry.getResource().getResourceType())) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "",
                        "found task with same identifier as " + remoteTask.getIdElement().getIdPart());
                return Optional.of((Task) localBundle.getEntryFirstRep().getResource());
            }
        }
        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "no task with same identifier " + remoteTask.getIdElement().getIdPart());

        return Optional.empty();
    }

    private Optional<ServiceRequest> getServiceRequestWithSameIdentifier(ServiceRequest basedOn,
            String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);
        Bundle localBundle = localFhirClient.search() //
                .forResource(ServiceRequest.class) //
                .where(ServiceRequest.IDENTIFIER.exactly().systemAndIdentifier(remoteStorePath,
                        basedOn.getIdElement().getIdPart())) //
                .returnBundle(Bundle.class).execute();
        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.ServiceRequest.equals(entry.getResource().getResourceType())) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "",
                        "found serviceRequest with same identifier as " + basedOn.getIdElement().getIdPart());
                return Optional.of((ServiceRequest) localBundle.getEntryFirstRep().getResource());
            }
        }
        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "no serviceRequest with same identifier " + basedOn.getIdElement().getIdPart());
        return Optional.empty();
    }

    private Optional<Patient> getPatientWithSameServiceIdentifier(Patient remotePatient, String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);

        Bundle localBundle = localFhirClient.search() //
                .forResource(Patient.class) //
                .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(remoteStorePath,
                        remotePatient.getIdElement().getIdPart())) //
                .returnBundle(Bundle.class).execute();
        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.Patient.equals(entry.getResource().getResourceType())) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "",
                        "found patient with same identifier as " + remotePatient.getIdElement().getIdPart());
                return Optional.of((Patient) localBundle.getEntryFirstRep().getResource());
            }
        }
        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "no patient with same identifier " + remotePatient.getIdElement().getIdPart());
        return Optional.empty();
    }

    private Optional<Practitioner> getPractitionerWithSameServiceIdentifier(Practitioner remotePractitioner,
            String remoteStorePath) {
        IGenericClient localFhirClient = fhirUtil.getFhirClient(localFhirStorePath);

        Bundle localBundle = localFhirClient.search() //
                .forResource(Practitioner.class) //
                .where(Practitioner.IDENTIFIER.exactly().systemAndIdentifier(remoteStorePath,
                        remotePractitioner.getIdElement().getIdPart())) //
                .returnBundle(Bundle.class).execute();
        for (BundleEntryComponent entry : localBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.Practitioner.equals(entry.getResource().getResourceType())) {
                LogEvent.logTrace(this.getClass().getSimpleName(), "",
                        "found Practitioner with same identifier as " + remotePractitioner.getIdElement().getIdPart());
                return Optional.of((Practitioner) localBundle.getEntryFirstRep().getResource());
            }
        }
        LogEvent.logTrace(this.getClass().getSimpleName(), "",
                "no Practitioner with same identifier " + remotePractitioner.getIdElement().getIdPart());
        return Optional.empty();
    }

    private List<ServiceRequest> getBasedOnServiceRequestsFromServer(IGenericClient fhirClient, Task remoteTask) {
        List<ServiceRequest> basedOn = new ArrayList<>();
        for (Reference basedOnElement : remoteTask.getBasedOn()) {
            basedOn.add(fhirClient.read().resource(ServiceRequest.class)
                    .withId(basedOnElement.getReferenceElement().getIdPart()).execute());
        }
        return basedOn;
    }

    private Patient getForPatientFromServer(IGenericClient fhirClient, Task remoteTask) {
        Patient forPatient = null;
        if (!(remoteTask.getFor() == null || remoteTask.getFor().getReference() == null)) {
            forPatient = fhirClient.read().resource(Patient.class)
                    .withId(remoteTask.getFor().getReferenceElement().getIdPart()).execute();
        }

        if (forPatient == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getForPatientFromServer",
                    "remoteTask doesn't reference a patient, or referenced patient returned null");
        }
        return forPatient;
    }

    private Location getTaskLocationFromServer(IGenericClient fhirClient, Task remoteTask) {
        Location taskLocation = null;
        if (!(remoteTask.getLocation() == null || remoteTask.getLocation().getReference() == null)) {
            taskLocation = fhirClient.read().resource(Location.class)
                    .withId(remoteTask.getLocation().getReferenceElement().getIdPart()).execute();
        }

        if (taskLocation == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getTaskLoctionFromServer",
                    "remoteTask doesn't reference a Location");
        }
        return taskLocation;
    }

    private Practitioner getPractitionerFromServer(IGenericClient fhirClient, Task remoteTask) {
        Practitioner practitioner = null;
        if (!GenericValidator.isBlankOrNull(remoteTask.getRequester().getReferenceElement().getIdPart())
                && remoteTask.getRequester().getReference().contains(ResourceType.Practitioner.toString())) {
            practitioner = fhirClient.read().resource(Practitioner.class)
                    .withId(remoteTask.getRequester().getReferenceElement().getIdPart()).execute();
        }

        if (practitioner == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getForPractitionerFromServer",
                    "remoteTask doesn't reference a Practitioner, or referenced Practitioner returned null");
        }
        return practitioner;
    }

    // may be incomplete
    public class OriginalReferralObjects {
        public List<QuestionnaireResponse> questionnaireResponses;
        public Practitioner practitioner;
        public Task task;
        public List<ServiceRequest> serviceRequests;
        public List<Practitioner> requestors;
        public List<Specimen> specimens;
        public Patient patient;
        public Location location;
    }

    public class ReferralResultsImportObjects {
        public OriginalReferralObjects originalReferralObjects = new OriginalReferralObjects();
        public ServiceRequest serviceRequest;
        public DiagnosticReport diagnosticReport;
        public List<Observation> observations = new ArrayList<>();
    }
}
