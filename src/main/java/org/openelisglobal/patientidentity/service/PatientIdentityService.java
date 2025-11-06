package org.openelisglobal.patientidentity.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;

public interface PatientIdentityService extends BaseObjectService<PatientIdentity, String> {

    List<PatientIdentity> getPatientIdentitiesForPatient(String id);

    PatientIdentity getPatitentIdentityForPatientAndType(String patientId, String identityTypeId);

    List<PatientIdentity> getPatientIdentitiesByValueAndType(String value, String identityType);
}
