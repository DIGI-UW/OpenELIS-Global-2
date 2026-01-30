package org.openelisglobal.fhir.facade.provider;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.facade.FhirFacadeServlet;
import org.openelisglobal.patient.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FHIR REST Controller for Patient resources.
 *
 * <p>
 * This controller implements FHIR R4 Patient resource operations using Spring
 * MVC:
 * <ul>
 * <li>GET /fhir/Patient/{id} - Read a single patient</li>
 * <li>GET /fhir/Patient - Search patients</li>
 * </ul>
 *
 * <p>
 * The controller delegates to existing OpenELIS services:
 * <ul>
 * <li>{@link PatientService} - Patient data access</li>
 * <li>{@link FhirTransformService} - Entity to FHIR transformation</li>
 * </ul>
 *
 * <p>
 * Supported search parameters:
 * <ul>
 * <li>identifier - Patient identifier (subject number, national ID, GUID)</li>
 * <li>family - Family name (partial match)</li>
 * <li>given - Given name (partial match)</li>
 * <li>name - Any name part</li>
 * </ul>
 *
 * @author OpenELIS Global Team
 * @since 3.0
 */
@RestController
@RequestMapping("/fhir/Patient")
public class PatientResourceProvider {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private PatientService patientService;

    @Autowired
    private FhirTransformService fhirTransformService;

    /**
     * FHIR Read operation - retrieve a single Patient by ID.
     *
     * <p>
     * Endpoint: GET /fhir/Patient/{id}
     *
     * <p>
     * The ID can be either:
     * <ul>
     * <li>The patient's internal database ID</li>
     * <li>The patient's FHIR UUID (fhir_uuid column)</li>
     * </ul>
     *
     * @param id the logical ID of the Patient
     * @return the Patient resource as FHIR JSON
     */
    @GetMapping(value = "/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> read(@PathVariable String id) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "read", "Reading Patient with ID: " + id);

        if (id == null || id.trim().isEmpty()) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Patient ID cannot be null or empty");
        }

        try {
            // First, try to get by internal ID
            org.openelisglobal.patient.valueholder.Patient oePatient = patientService.get(id);

            if (oePatient == null) {
                // Try to get by FHIR UUID
                oePatient = patientService.getPatientForGuid(id);
            }

            if (oePatient == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "read", "Patient not found with ID: " + id);
                return createErrorResponse(HttpStatus.NOT_FOUND, "Patient/" + id + " not found");
            }

            // Transform to FHIR Patient
            Patient fhirPatient = fhirTransformService.transformToFhirPatient(oePatient.getId());
            LogEvent.logInfo(this.getClass().getSimpleName(), "read",
                    "Successfully retrieved Patient: " + fhirPatient.getId());

            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(fhirPatient);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "read", "Error reading Patient: " + e.getMessage());
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading Patient: " + e.getMessage());
        }
    }

    /**
     * FHIR Search operation - search for Patients.
     *
     * <p>
     * Endpoint: GET /fhir/Patient?[params]
     *
     * <p>
     * Examples:
     * <ul>
     * <li>GET /fhir/Patient - Returns all patients (paginated)</li>
     * <li>GET /fhir/Patient?identifier=ABC123 - Search by identifier</li>
     * <li>GET /fhir/Patient?family=Smith - Search by family name</li>
     * <li>GET /fhir/Patient?given=John - Search by given name</li>
     * <li>GET /fhir/Patient?name=John - Search by any name part</li>
     * </ul>
     *
     * @param identifier patient identifier (optional)
     * @param family     family name (optional)
     * @param given      given name (optional)
     * @param name       any name part (optional)
     * @param count      maximum results to return (optional, default 20)
     * @return Bundle of matching Patient resources
     */
    @GetMapping(produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> search(@RequestParam(value = "identifier", required = false) String identifier,
            @RequestParam(value = "family", required = false) String family,
            @RequestParam(value = "given", required = false) String given,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "_count", required = false, defaultValue = "20") int count) {

        LogEvent.logInfo(this.getClass().getSimpleName(), "search", "Searching Patients - identifier: " + identifier
                + ", family: " + family + ", given: " + given + ", name: " + name);

        try {
            List<Patient> results = new ArrayList<>();
            List<org.openelisglobal.patient.valueholder.Patient> oePatients;

            // Search by identifier (subject number, national ID, or external ID)
            if (identifier != null && !identifier.trim().isEmpty()) {
                oePatients = searchByIdentifier(identifier);
            }
            // Search by name
            else if (family != null || given != null || name != null) {
                oePatients = searchByName(family, given, name);
            }
            // Return all patients (with pagination)
            else {
                oePatients = patientService.getPageOfPatients(0);
            }

            // Limit results
            int limit = Math.min(count, 100);
            int processed = 0;

            // Transform results to FHIR
            for (org.openelisglobal.patient.valueholder.Patient oePatient : oePatients) {
                if (processed >= limit) {
                    break;
                }
                try {
                    Patient fhirPatient = fhirTransformService.transformToFhirPatient(oePatient.getId());
                    if (fhirPatient != null) {
                        results.add(fhirPatient);
                        processed++;
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "search",
                            "Error transforming patient " + oePatient.getId() + ": " + e.getMessage());
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "search",
                    "Search returned " + results.size() + " patients");

            // Build FHIR Bundle
            Bundle bundle = createSearchBundle(results);
            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(bundle);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "search", "Error searching patients: " + e.getMessage());
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching patients: " + e.getMessage());
        }
    }

    /**
     * Search patients by identifier (subject number, national ID, external ID).
     *
     * @param identifier the identifier value
     * @return list of matching patients
     */
    private List<org.openelisglobal.patient.valueholder.Patient> searchByIdentifier(String identifier) {
        List<org.openelisglobal.patient.valueholder.Patient> results = new ArrayList<>();

        // Try national ID
        List<org.openelisglobal.patient.valueholder.Patient> byNationalId = patientService
                .getPatientsByNationalId(identifier);
        if (byNationalId != null && !byNationalId.isEmpty()) {
            results.addAll(byNationalId);
        }

        // Try external ID
        org.openelisglobal.patient.valueholder.Patient byExternalId = patientService.getPatientByExternalId(identifier);
        if (byExternalId != null && !containsPatient(results, byExternalId)) {
            results.add(byExternalId);
        }

        // Try GUID
        org.openelisglobal.patient.valueholder.Patient byGuid = patientService.getPatientForGuid(identifier);
        if (byGuid != null && !containsPatient(results, byGuid)) {
            results.add(byGuid);
        }

        return results;
    }

    /**
     * Search patients by name components.
     *
     * @param family family name (optional)
     * @param given  given name (optional)
     * @param name   any name part (optional)
     * @return list of matching patients
     */
    private List<org.openelisglobal.patient.valueholder.Patient> searchByName(String family, String given,
            String name) {
        List<org.openelisglobal.patient.valueholder.Patient> allPatients = patientService.getPageOfPatients(0);
        List<org.openelisglobal.patient.valueholder.Patient> results = new ArrayList<>();

        for (org.openelisglobal.patient.valueholder.Patient patient : allPatients) {
            String patientFirstName = patientService.getFirstName(patient);
            String patientLastName = patientService.getLastName(patient);

            boolean matches = true;

            if (family != null && !family.trim().isEmpty()) {
                matches = patientLastName != null && patientLastName.toLowerCase().contains(family.toLowerCase());
            }

            if (matches && given != null && !given.trim().isEmpty()) {
                matches = patientFirstName != null && patientFirstName.toLowerCase().contains(given.toLowerCase());
            }

            if (matches && name != null && !name.trim().isEmpty()) {
                String nameLower = name.toLowerCase();
                matches = (patientFirstName != null && patientFirstName.toLowerCase().contains(nameLower))
                        || (patientLastName != null && patientLastName.toLowerCase().contains(nameLower));
            }

            if (matches) {
                results.add(patient);
            }
        }

        return results;
    }

    /**
     * Create a FHIR Bundle for search results.
     *
     * @param patients list of Patient resources
     * @return a searchset Bundle
     */
    private Bundle createSearchBundle(List<Patient> patients) {
        Bundle bundle = new Bundle();
        bundle.setType(BundleType.SEARCHSET);
        bundle.setTotal(patients.size());

        for (Patient patient : patients) {
            BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(patient);
            entry.setFullUrl("Patient/" + patient.getId());
        }

        return bundle;
    }

    /**
     * Create an error response with OperationOutcome.
     *
     * @param status  HTTP status
     * @param message error message
     * @return ResponseEntity with OperationOutcome
     */
    private ResponseEntity<String> createErrorResponse(HttpStatus status, String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(status.is4xxClientError() ? IssueSeverity.WARNING : IssueSeverity.ERROR)
                .setCode(status == HttpStatus.NOT_FOUND ? IssueType.NOTFOUND : IssueType.EXCEPTION)
                .setDiagnostics(message);

        String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(outcome);
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);
    }

    /**
     * Check if a patient is already in the results list.
     *
     * @param results the results list
     * @param patient the patient to check
     * @return true if patient is already in the list
     */
    private boolean containsPatient(List<org.openelisglobal.patient.valueholder.Patient> results,
            org.openelisglobal.patient.valueholder.Patient patient) {
        return results.stream().anyMatch(p -> p.getId().equals(patient.getId()));
    }
}
