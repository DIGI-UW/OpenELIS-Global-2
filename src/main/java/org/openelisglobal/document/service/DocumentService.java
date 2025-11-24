package org.openelisglobal.document.service;

import org.openelisglobal.document.dto.DocumentMetadataResponse;
import org.openelisglobal.document.dto.UploadDocumentResponse;
import org.openelisglobal.document.dto.DocumentVersionResponse;

import java.io.InputStream;
import java.util.List;

public interface DocumentService {
    UploadDocumentResponse upload(String patientId, String documentType, String description, InputStream data, String filename, String contentType, long size, String createdBy) throws Exception;
    List<DocumentMetadataResponse> listForPatient(String patientId);
    InputStream downloadLatest(String documentId) throws Exception;
    List<DocumentVersionResponse> listVersions(String documentId);
    void softDelete(String documentId, String deletedBy) throws Exception;
    void restore(String documentId, String restoredBy) throws Exception;
}
