/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.document.service;

import org.openelisglobal.document.dao.DocumentAuditDAO;
import org.openelisglobal.document.dao.DocumentVersionDAO;
import org.openelisglobal.document.dao.IDDocumentDAO;
import org.openelisglobal.document.dto.DocumentMetadataResponse;
import org.openelisglobal.document.dto.DocumentVersionResponse;
import org.openelisglobal.document.dto.UploadDocumentResponse;
import org.openelisglobal.document.storage.DocumentStorageService;
import org.openelisglobal.document.validation.DocumentValidationService;
import org.openelisglobal.document.valueholder.DocumentAudit;
import org.openelisglobal.document.valueholder.DocumentVersion;
import org.openelisglobal.document.valueholder.IDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for document management.
 * Handles upload, validation, malware scanning, rate limiting, thumbnail generation, and audit logging.
 */
@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private IDDocumentDAO idDocumentDAO;

    @Autowired
    private DocumentVersionDAO documentVersionDAO;

    @Autowired
    private DocumentAuditDAO documentAuditDAO;

    @Autowired
    private DocumentStorageService storageService;

    @Autowired
    private DocumentValidationService validationService;

    @Autowired
    private MalwareScanner malwareScanner;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ThumbnailService thumbnailService;

    @Override
    public UploadDocumentResponse upload(String patientId, String documentType, String description,
            InputStream data, String filename, String contentType, long size, String createdBy) throws Exception {
        
        // Rate limiting check
        if (!rateLimitService.isAllowed(patientId)) {
            long waitSeconds = rateLimitService.getTimeUntilNextAllowed(patientId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Please wait " + waitSeconds + " seconds before uploading again.");
        }

        // Read stream into byte array for multiple passes (validation, malware scan, storage)
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] dataBytes = new byte[8192];
        int bytesRead;
        while ((bytesRead = data.read(dataBytes)) != -1) {
            buffer.write(dataBytes, 0, bytesRead);
        }
        byte[] documentData = buffer.toByteArray();

        // Validate file format and size
        validationService.validate(new ByteArrayInputStream(documentData), contentType, size);

        // Malware scanning
        try {
            if (!malwareScanner.scan(new ByteArrayInputStream(documentData), filename)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Document failed malware scan and was rejected. Please retry after ensuring the file is safe.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Malware scanning service unavailable. Please retry after the service recovers: " + e.getMessage());
        }

        // Check for existing document of same type (enforce uniqueness)
        IDDocument existingDoc = idDocumentDAO.findByPatientIdAndDocumentType(patientId, documentType);
        String documentId;
        Integer versionNumber = 1;

        if (existingDoc != null && !existingDoc.getIsDeleted()) {
            // Replace existing document: soft-delete old version, create new version
            documentId = existingDoc.getId();
            
            // Soft-delete current version
            DocumentVersion currentVersion = documentVersionDAO.getLatestVersion(documentId);
            if (currentVersion != null) {
                currentVersion.setIsDeleted(true);
                documentVersionDAO.saveVersion(currentVersion);
            }

            // Get next version number
            List<DocumentVersion> allVersions = documentVersionDAO.listByDocumentId(documentId);
            versionNumber = allVersions.stream()
                    .mapToInt(DocumentVersion::getVersionNumber)
                    .max()
                    .orElse(0) + 1;
        } else {
            // Create new document
            documentId = UUID.randomUUID().toString();
            IDDocument doc = new IDDocument();
            doc.setId(documentId);
            doc.setPatientId(patientId);
            doc.setDocumentType(documentType);
            doc.setDescription(description);
            doc.setCreatedAt(OffsetDateTime.now());
            doc.setCreatedBy(createdBy == null ? "system" : createdBy);
            idDocumentDAO.save(doc);
        }

        // Store document
        String targetPath = String.format("documents/%s/%s/%s", patientId, documentId, filename);
        String storageKey = storageService.store(new ByteArrayInputStream(documentData), targetPath);

        // Generate thumbnail (async retry on failure - for now, we'll try synchronously)
        String thumbnailPath = null;
        try {
            InputStream thumbnailStream;
            if (contentType.startsWith("image/")) {
                thumbnailStream = thumbnailService.generateImageThumbnail(
                        new ByteArrayInputStream(documentData), contentType);
            } else if (contentType.equals("application/pdf")) {
                thumbnailStream = thumbnailService.generatePdfThumbnail(
                        new ByteArrayInputStream(documentData));
            } else {
                thumbnailStream = null;
            }

            if (thumbnailStream != null) {
                String thumbnailTargetPath = String.format("documents/%s/%s/thumbnails/%s.jpg",
                        patientId, documentId, filename);
                thumbnailPath = storageService.store(thumbnailStream, thumbnailTargetPath);
            }
        } catch (Exception e) {
            // Thumbnail generation failed - log but don't fail upload
            // TODO: Queue for async retry
            System.err.println("Thumbnail generation failed for " + filename + ": " + e.getMessage());
        }

        // Create version
        String versionId = UUID.randomUUID().toString();
        DocumentVersion v = new DocumentVersion();
        v.setVersionId(versionId);
        v.setIdDocumentId(documentId);
        v.setVersionNumber(versionNumber);
        v.setFilename(filename);
        v.setContentType(contentType);
        v.setSizeBytes(size);
        v.setStorageKey(storageKey);
        v.setThumbnailPath(thumbnailPath);
        v.setCreatedAt(OffsetDateTime.now());
        v.setCreatedBy(createdBy == null ? "system" : createdBy);
        documentVersionDAO.saveVersion(v);

        // Update document current version
        IDDocument doc = idDocumentDAO.getById(documentId);
        doc.setCurrentVersionId(versionId);
        idDocumentDAO.save(doc);

        // Record upload in audit log
        DocumentAudit audit = new DocumentAudit();
        audit.setId(UUID.randomUUID().toString());
        audit.setAction("UPLOAD");
        audit.setUserId(createdBy == null ? "system" : createdBy);
        audit.setPatientId(patientId);
        audit.setIdDocumentId(documentId);
        audit.setDocumentVersionId(versionId);
        audit.setFileName(filename);
        audit.setFileSize(size);
        audit.setCreatedAt(OffsetDateTime.now());
        documentAuditDAO.save(audit);

        // Record upload for rate limiting
        rateLimitService.recordUpload(patientId);

        return new UploadDocumentResponse(documentId, versionId);
    }

    @Override
    public List<DocumentMetadataResponse> listForPatient(String patientId) {
        List<IDDocument> docs = idDocumentDAO.listByPatientId(patientId);
        List<DocumentMetadataResponse> out = new ArrayList<>();
        for (IDDocument d : docs) {
            DocumentMetadataResponse m = new DocumentMetadataResponse();
            m.setDocumentId(d.getId());
            m.setDocumentType(d.getDocumentType());
            m.setDescription(d.getDescription());
            m.setCreatedAt(d.getCreatedAt());
            
            // Get thumbnail path from current version
            if (d.getCurrentVersionId() != null) {
                DocumentVersion currentVersion = documentVersionDAO.getLatestVersion(d.getId());
                if (currentVersion != null && currentVersion.getThumbnailPath() != null) {
                    // TODO: Convert to URL for frontend
                    m.setThumbnailUrl(currentVersion.getThumbnailPath());
                }
            }
            
            out.add(m);
        }
        return out;
    }

    @Override
    public InputStream downloadLatest(String documentId) throws Exception {
        DocumentVersion v = documentVersionDAO.getLatestVersion(documentId);
        if (v == null) {
            throw new IllegalArgumentException("No versions found for document: " + documentId);
        }
        return storageService.retrieve(v.getStorageKey());
    }

    @Override
    public List<DocumentVersionResponse> listVersions(String documentId) {
        List<DocumentVersion> versions = documentVersionDAO.listByDocumentId(documentId);
        List<DocumentVersionResponse> out = new ArrayList<>();
        for (DocumentVersion v : versions) {
            DocumentVersionResponse r = new DocumentVersionResponse();
            r.setVersionId(v.getVersionId());
            r.setVersionNumber(v.getVersionNumber() != null ? v.getVersionNumber() : -1);
            r.setStorageReference(v.getStorageKey());
            r.setSizeBytes(v.getSizeBytes() != null ? v.getSizeBytes() : 0);
            r.setUploadedAt(v.getCreatedAt());
            out.add(r);
        }
        return out;
    }

    @Override
    public void softDelete(String documentId, String deletedBy) throws Exception {
        IDDocument doc = idDocumentDAO.getById(documentId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        
        idDocumentDAO.softDelete(documentId);

        // Audit log
        DocumentAudit audit = new DocumentAudit();
        audit.setId(UUID.randomUUID().toString());
        audit.setAction("DELETE");
        audit.setUserId(deletedBy);
        audit.setPatientId(doc.getPatientId());
        audit.setIdDocumentId(documentId);
        audit.setCreatedAt(OffsetDateTime.now());
        documentAuditDAO.save(audit);
    }

    @Override
    public void restore(String documentId, String restoredBy) throws Exception {
        IDDocument doc = idDocumentDAO.getById(documentId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        
        idDocumentDAO.restore(documentId);

        // Audit log
        DocumentAudit audit = new DocumentAudit();
        audit.setId(UUID.randomUUID().toString());
        audit.setAction("RESTORE");
        audit.setUserId(restoredBy);
        audit.setPatientId(doc.getPatientId());
        audit.setIdDocumentId(documentId);
        audit.setCreatedAt(OffsetDateTime.now());
        documentAuditDAO.save(audit);
    }
}
