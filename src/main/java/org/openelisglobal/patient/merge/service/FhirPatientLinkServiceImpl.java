package org.openelisglobal.patient.merge.service;

import java.util.Optional;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.LinkType;
import org.hl7.fhir.r4.model.Reference;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.patient.dao.PatientDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of FHIR Patient link management for patient merge operations.
 * Handles bidirectional link relationships between merged and primary patients
 * according to FHIR R4 specification.
 */
@Service
public class FhirPatientLinkServiceImpl implements FhirPatientLinkService {

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private PatientDAO patientDAO;

    @Override
    public void updatePatientLinks(String primaryPatientId, String mergedPatientId, String primaryFhirUuid,
            String mergedFhirUuid) throws FhirLocalPersistingException {

        LogEvent.logInfo(this.getClass().getName(), "updatePatientLinks", "Updating FHIR links for primary patient: "
                + primaryPatientId + ", merged patient: " + mergedPatientId);

        // Retrieve FHIR Patient resources
        Optional<Patient> primaryFhirPatientOpt = fhirPersistenceService.getPatientByUuid(primaryFhirUuid);
        Optional<Patient> mergedFhirPatientOpt = fhirPersistenceService.getPatientByUuid(mergedFhirUuid);

        if (!primaryFhirPatientOpt.isPresent()) {
            LogEvent.logWarn(this.getClass().getName(), "updatePatientLinks",
                    "Primary patient FHIR resource not found for UUID: " + primaryFhirUuid);
            throw new FhirLocalPersistingException(
                    "Primary patient FHIR resource not found with UUID: " + primaryFhirUuid);
        }

        if (!mergedFhirPatientOpt.isPresent()) {
            LogEvent.logWarn(this.getClass().getName(), "updatePatientLinks",
                    "Merged patient FHIR resource not found for UUID: " + mergedFhirUuid);
            throw new FhirLocalPersistingException(
                    "Merged patient FHIR resource not found with UUID: " + mergedFhirUuid);
        }

        Patient primaryFhirPatient = primaryFhirPatientOpt.get();
        Patient mergedFhirPatient = mergedFhirPatientOpt.get();

        // FR-016: Update primary patient with link type "replaces"
        Patient.PatientLinkComponent primaryLink = new Patient.PatientLinkComponent();
        primaryLink.setOther(new Reference("Patient/" + mergedFhirUuid));
        primaryLink.setType(LinkType.REPLACES);
        primaryFhirPatient.getLink().add(primaryLink);

        // FR-017: Update merged patient with link type "replaced-by" and set active =
        // false
        Patient.PatientLinkComponent mergedLink = new Patient.PatientLinkComponent();
        mergedLink.setOther(new Reference("Patient/" + primaryFhirUuid));
        mergedLink.setType(LinkType.REPLACEDBY);
        mergedFhirPatient.getLink().add(mergedLink);
        mergedFhirPatient.setActive(false);

        // Persist updated FHIR resources
        try {
            fhirPersistenceService.updateFhirResourceInFhirStore(primaryFhirPatient);
            LogEvent.logInfo(this.getClass().getName(), "updatePatientLinks",
                    "Successfully updated primary patient FHIR resource: " + primaryFhirUuid);

            fhirPersistenceService.updateFhirResourceInFhirStore(mergedFhirPatient);
            LogEvent.logInfo(this.getClass().getName(), "updatePatientLinks",
                    "Successfully updated merged patient FHIR resource: " + mergedFhirUuid);

        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(this.getClass().getName(), "updatePatientLinks",
                    "Failed to persist FHIR Patient link updates: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean hasFhirResource(String patientId) {
        org.openelisglobal.patient.valueholder.Patient patient = patientDAO.getData(patientId);
        return patient != null && patient.getFhirUuid() != null;
    }
}
