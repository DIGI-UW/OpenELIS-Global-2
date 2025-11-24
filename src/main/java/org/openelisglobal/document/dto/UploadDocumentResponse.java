package org.openelisglobal.document.dto;

public class UploadDocumentResponse {
    private String documentId;
    private String versionId;

    public UploadDocumentResponse() {}

    public UploadDocumentResponse(String documentId, String versionId) {
        this.documentId = documentId;
        this.versionId = versionId;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }
}
