package org.openelisglobal.patient.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.patient.valueholder.PatientContact;

public interface PatientContactDAO extends BaseDAO<PatientContact, String> {

    List<PatientContact> getForPatient(String patientId);
}
