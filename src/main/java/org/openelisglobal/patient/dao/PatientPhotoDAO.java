package org.openelisglobal.patient.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.patient.valueholder.PatientPhoto;

public interface PatientPhotoDAO extends BaseDAO<PatientPhoto, Integer> {

    PatientPhoto getByPatientId(String patientId);
}
