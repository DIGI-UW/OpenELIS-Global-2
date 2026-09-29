package org.openelisglobal.patientidentitytype.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface PatientIdentityTypeService extends BaseObjectService<PatientIdentityType, String> {
    PatientIdentityType getNamedIdentityType(String name);

    List<PatientIdentityType> getAllPatientIdenityTypes();
}
