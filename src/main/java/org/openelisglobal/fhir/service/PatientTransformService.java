package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;

public interface PatientTransformService {

    Patient transformToFhirPatient(org.openelisglobal.patient.valueholder.Patient patient);

    PatientSearchResults transformToOpenElisPatientSearchResults(Patient fhirPatient);

    PatientManagementInfo createOePatientManagementInfo(Patient fhirPatient);

}
