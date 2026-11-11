package org.openelisglobal.patient.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.patient.valueholder.PatientIdDocument;

public interface PatientIdDocumentDAO extends BaseDAO<PatientIdDocument, Integer> {

    List<PatientIdDocument> getByPatientId(String patientId);
}
