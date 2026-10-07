package org.openelisglobal.fhir.service;

import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;

/**
 * OpenELIS Patient to and from FHIR Patient.
 */
public interface PatientTransformService {

    org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId);

    PatientManagementInfo createOePatientManagementInfo(org.hl7.fhir.r4.model.Patient fhirPatient);

    org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient);

    PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient);

    /**
     * Writes the parts of the patient's first address that the patient form does
     * not carry (state, postal code, country) onto the person.
     */
    void addAddressToPerson(org.hl7.fhir.r4.model.Patient fhirPatient, Person person);

    /**
     * Copies onto {@code patientInfo} the stored details a FHIR Patient has no
     * element for: the identities other than national id, subject number, ST number
     * and GUID, and the GPS coordinates. Saving the form writes every one of them,
     * so without this a FHIR update would erase them.
     */
    void keepDetailsFhirDoesNotCarry(PatientManagementInfo patientInfo, Patient storedPatient);
}
