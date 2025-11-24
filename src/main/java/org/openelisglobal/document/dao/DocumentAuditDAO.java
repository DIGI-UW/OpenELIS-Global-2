package org.openelisglobal.document.dao;

import org.openelisglobal.document.valueholder.DocumentAudit;
import java.util.List;

public interface DocumentAuditDAO {
    DocumentAudit save(DocumentAudit audit);
    List<DocumentAudit> listByPatientId(String patientId);
    List<DocumentAudit> listByDocumentId(String documentId);
}

