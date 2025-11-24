package org.openelisglobal.document.dao;

import org.openelisglobal.document.valueholder.DocumentVersion;
import java.util.List;

public interface DocumentVersionDAO {
    DocumentVersion saveVersion(DocumentVersion v);
    List<DocumentVersion> listByDocumentId(String documentId);
    DocumentVersion getLatestVersion(String documentId);
}
