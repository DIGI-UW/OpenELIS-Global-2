package org.openelisglobal.patientidentitytype.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;

public interface PatientIdentityTypeService extends BaseObjectService<PatientIdentityType, String> {
    PatientIdentityType getNamedIdentityType(String name);

    List<PatientIdentityType> getAllPatientIdenityTypes();
}
