package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface PatientContactService extends BaseObjectService<PatientContact, String> {

    List<PatientContact> getForPatient(String patientId);
}
