package org.openelisglobal.patienttype.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;

public interface PatientPatientTypeDAO extends BaseDAO<PatientPatientType, String> {

    // public boolean insertData(PatientPatientType patientType) throws
    // LIMSRuntimeException;

    // public void updateData(PatientPatientType patientType) throws
    // LIMSRuntimeException;

    // public PatientType getPatientTypeForPatient(String id);

    public PatientPatientType getPatientPatientTypeForPatient(String patientId) throws LIMSRuntimeException;
}
