package org.openelisglobal.patienttype.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patienttype.valueholder.BasePatientType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface BasePatientTypeService extends BaseObjectService<BasePatientType, String> {
}
