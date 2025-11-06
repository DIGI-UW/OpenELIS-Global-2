package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.PatientContact;

public interface PatientContactService extends BaseObjectService<PatientContact, String> {

    List<PatientContact> getForPatient(String patientId);
}
