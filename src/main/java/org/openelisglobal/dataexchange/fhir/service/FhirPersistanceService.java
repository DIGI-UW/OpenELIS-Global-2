package org.openelisglobal.dataexchange.fhir.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;

public interface FhirPersistanceService {

    Bundle createFhirResourceInFhirStore(Resource resource) throws FhirLocalPersistingException;

    Bundle updateFhirResourceInFhirStore(Resource resource) throws FhirLocalPersistingException;

    Bundle createUpdateFhirResourcesInFhirStore(FhirOperations fhirOperations) throws FhirLocalPersistingException;

    Bundle createUpdateFhirResourcesInFhirStore(List<FhirOperations> fhirOperationsList)
            throws FhirLocalPersistingException;

    List<ServiceRequest> getAllServiceRequestByAccessionNumber(String accessionNumber);

    Optional<Organization> getFhirOrganizationByName(String orgName);

    Optional<Patient> getPatientByUuid(String guid);

    Optional<ServiceRequest> getServiceRequestByAnalysisUuid(String uuid);

    Optional<Specimen> getSpecimenBySampleItemUuid(String uuid);

    Optional<DiagnosticReport> getDiagnosticReportByAnalysisUuid(String uuid);

    Optional<Task> getTaskBasedOnServiceRequest(String referringId);

    Optional<ServiceRequest> getServiceRequestByReferingId(String referringId);

    Optional<Task> getTaskBasedOnTask(String taskId);

    Bundle createUpdateFhirResourcesInFhirStore(Map<String, Resource> createResources,
            Map<String, Resource> updateResources) throws FhirLocalPersistingException;

    Bundle updateFhirResourcesInFhirStore(Map<String, Resource> resources) throws FhirLocalPersistingException;

    Bundle makeTransactionBundleForCreate(Map<String, Resource> resources);

    Bundle createFhirResourcesInFhirStore(Map<String, Resource> resources) throws FhirLocalPersistingException;

    // Optional<ServiceRequest> getTaskBasedOnServiceRequests(List<ServiceRequest>
    // serviceRequests);

}
