package org.openelisglobal.fhir.service;

import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;

/**
 * OpenELIS Patient to and from FHIR Patient.
 */
public interface PatientTransformService {

    org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId);

    PatientManagementInfo createOePatientManagementInfo(org.hl7.fhir.r4.model.Patient fhirPatient);

    org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient);

    PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient);
}
