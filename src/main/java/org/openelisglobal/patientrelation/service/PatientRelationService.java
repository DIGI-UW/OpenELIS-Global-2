package org.openelisglobal.patientrelation.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patientrelation.valueholder.PatientRelation;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface PatientRelationService extends BaseObjectService<PatientRelation, String> {
}
