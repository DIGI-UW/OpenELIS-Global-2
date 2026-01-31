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
import org.openelisglobal.common.util.ConfigurationProperties;
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

@RestController
@RequestMapping("/fhir/Patient")
public class PatientResourceProvider {

    private static final String FHIR_JSON = "application/fhir+json";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    private PatientService patientService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @GetMapping(value = "/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> read(@PathVariable String id) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "read", "Reading Patient with ID: " + id);

        if (id == null || id.trim().isEmpty()) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Patient ID cannot be null or empty");
        }

        try {
            org.openelisglobal.patient.valueholder.Patient oePatient = patientService.get(id);

            if (oePatient == null) {
                oePatient = patientService.getPatientForGuid(id);
            }

            if (oePatient == null) {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Patient/" + id + " not found");
            }

            Patient fhirPatient = fhirTransformService.transformToFhirPatient(oePatient.getId());
            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(fhirPatient);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "read", "Error reading Patient: " + e.getMessage());
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading Patient: " + e.getMessage());
        }
    }

    @GetMapping(produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> search(@RequestParam(value = "identifier", required = false) String identifier,
            @RequestParam(value = "family", required = false) String family,
            @RequestParam(value = "given", required = false) String given,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "_count", required = false, defaultValue = "20") int count) {

        try {
            List<Patient> results = new ArrayList<>();
            List<org.openelisglobal.patient.valueholder.Patient> oePatients;

            if (identifier != null && !identifier.trim().isEmpty()) {
                oePatients = searchByIdentifier(identifier);
            } else if (family != null || given != null || name != null) {
                oePatients = searchByName(family, given, name);
            }
            // Return all patients (with pagination) - use 1-based page number
            else {
                oePatients = patientService.getPageOfPatients(1);
            }

            int limit = Math.min(count, 100);
            int processed = 0;

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

            Bundle bundle = createSearchBundle(results);
            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(bundle);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "search", "Error searching patients: " + e.getMessage());
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching patients: " + e.getMessage());
        }
    }

    private List<org.openelisglobal.patient.valueholder.Patient> searchByIdentifier(String identifier) {
        List<org.openelisglobal.patient.valueholder.Patient> results = new ArrayList<>();

        List<org.openelisglobal.patient.valueholder.Patient> byNationalId = patientService
                .getPatientsByNationalId(identifier);
        if (byNationalId != null && !byNationalId.isEmpty()) {
            results.addAll(byNationalId);
        }

        org.openelisglobal.patient.valueholder.Patient byExternalId = patientService.getPatientByExternalId(identifier);
        if (byExternalId != null && !containsPatient(results, byExternalId)) {
            results.add(byExternalId);
        }

        org.openelisglobal.patient.valueholder.Patient byGuid = patientService.getPatientForGuid(identifier);
        if (byGuid != null && !containsPatient(results, byGuid)) {
            results.add(byGuid);
        }

        return results;
    }

    private List<org.openelisglobal.patient.valueholder.Patient> searchByName(String family, String given,
            String name) {
        // Use 1-based page number for pagination
        List<org.openelisglobal.patient.valueholder.Patient> allPatients = patientService.getPageOfPatients(1);
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

    private List<org.openelisglobal.patient.valueholder.Patient> getPageOfPatientsSafe(int pageNumber) {
        try {
            String pageSizeStr = ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize");
            if (pageSizeStr == null || pageSizeStr.trim().isEmpty()) {
                return getLimitedPatients(DEFAULT_PAGE_SIZE);
            }
            return patientService.getPageOfPatients(pageNumber);
        } catch (Exception e) {
            return getLimitedPatients(DEFAULT_PAGE_SIZE);
        }
    }

    private List<org.openelisglobal.patient.valueholder.Patient> getLimitedPatients(int limit) {
        List<org.openelisglobal.patient.valueholder.Patient> all = patientService.getAllPatients();
        if (all == null) {
            return new ArrayList<>();
        }
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }

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

    private ResponseEntity<String> createErrorResponse(HttpStatus status, String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(status.is4xxClientError() ? IssueSeverity.WARNING : IssueSeverity.ERROR)
                .setCode(status == HttpStatus.NOT_FOUND ? IssueType.NOTFOUND : IssueType.EXCEPTION)
                .setDiagnostics(message);

        String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(outcome);
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);
    }

    private boolean containsPatient(List<org.openelisglobal.patient.valueholder.Patient> results,
            org.openelisglobal.patient.valueholder.Patient patient) {
        return results.stream().anyMatch(p -> p.getId().equals(patient.getId()));
    }
}
