package org.openelisglobal.fhir.service;

import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;

/**
 * OpenELIS Patient to and from FHIR Patient.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface PatientTransformService {

    org.hl7.fhir.r4.model.Patient transformToFhirPatient(String patientId);

    PatientManagementInfo createOePatientManagementInfo(org.hl7.fhir.r4.model.Patient fhirPatient);

    org.hl7.fhir.r4.model.Patient transformToFhirPatient(Patient patient);

    PatientSearchResults transformToOpenElisPatientSearchResults(org.hl7.fhir.r4.model.Patient fhirPatient);
}
