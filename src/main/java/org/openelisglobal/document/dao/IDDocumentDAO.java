package org.openelisglobal.document.dao;

import org.openelisglobal.document.valueholder.IDDocument;
import java.util.List;

public interface IDDocumentDAO {
    IDDocument save(IDDocument doc);
    IDDocument getById(String id);
    List<IDDocument> listByPatientId(String patientId);
    IDDocument findByPatientIdAndDocumentType(String patientId, String documentType);
    void softDelete(String id);
    void restore(String id);
}
