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
package org.openelisglobal.document.controller;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.document.dto.DocumentMetadataResponse;
import org.openelisglobal.document.dto.DocumentVersionResponse;
import org.openelisglobal.document.dto.UploadDocumentResponse;
import org.openelisglobal.document.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

/**
 * REST Controller for Patient ID Document management.
 * 
 * NO @Transactional annotations - transactions belong in service layer.
 * Controllers are singletons - NO class-level variables.
 */
@RestController
@RequestMapping("/rest/patients/{patientId}/documents")
public class IDDocumentRestController extends BaseRestController {

    @Autowired
    private DocumentService documentService;

    /**
     * Upload a new document for a patient.
     * POST /rest/patients/{patientId}/documents
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentMetadataResponse> uploadDocument(
            @PathVariable String patientId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "documentGroupId", required = false) String documentGroupId,
            Principal principal) throws Exception {

        String user = principal == null ? "system" : principal.getName();

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        if (documentType == null || documentType.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentType is required");
        }

        UploadDocumentResponse resp = documentService.upload(
                patientId,
                documentType,
                description,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                user);

        // Return document metadata
        List<DocumentMetadataResponse> docs = documentService.listForPatient(patientId);
        DocumentMetadataResponse created = docs.stream()
                .filter(d -> d.getDocumentId().equals(resp.getDocumentId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Document created but not found in list"));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * List all documents for a patient.
     * GET /rest/patients/{patientId}/documents
     */
    @GetMapping
    public ResponseEntity<List<DocumentMetadataResponse>> listForPatient(@PathVariable String patientId) {
        List<DocumentMetadataResponse> list = documentService.listForPatient(patientId);
        return ResponseEntity.ok(list);
    }

    /**
     * Get document metadata.
     * GET /rest/patients/{patientId}/documents/{documentId}
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentMetadataResponse> getDocument(
            @PathVariable String patientId,
            @PathVariable String documentId) {
        List<DocumentMetadataResponse> docs = documentService.listForPatient(patientId);
        DocumentMetadataResponse doc = docs.stream()
                .filter(d -> d.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found: " + documentId));
        return ResponseEntity.ok(doc);
    }

    /**
     * Update document metadata (type, description).
     * PUT /rest/patients/{patientId}/documents/{documentId}/metadata
     */
    @PutMapping("/{documentId}/metadata")
    public ResponseEntity<DocumentMetadataResponse> updateMetadata(
            @PathVariable String patientId,
            @PathVariable String documentId,
            @RequestBody UpdateMetadataRequest request,
            Principal principal) throws Exception {
        // TODO: Implement metadata update in DocumentService
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Metadata update not yet implemented");
    }

    /**
     * Soft-delete a document.
     * DELETE /rest/patients/{patientId}/documents/{documentId}
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> softDelete(
            @PathVariable String patientId,
            @PathVariable String documentId,
            Principal principal) throws Exception {
        String user = principal == null ? "system" : principal.getName();
        documentService.softDelete(documentId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Download document binary content.
     * GET /rest/patients/{patientId}/documents/{documentId}/versions/{versionId}/content
     */
    @GetMapping("/{documentId}/versions/{versionId}/content")
    public ResponseEntity<byte[]> downloadContent(
            @PathVariable String patientId,
            @PathVariable String documentId,
            @PathVariable String versionId) throws Exception {
        // For now, download latest version (versionId parameter ignored)
        // TODO: Add method to download specific version
        InputStream in = documentService.downloadLatest(documentId);
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        byte[] content = buffer.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=document")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    /**
     * List all versions of a document.
     * GET /rest/documents/{documentId}/versions
     */
    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> listVersions(
            @PathVariable String patientId,
            @PathVariable String documentId) {
        List<DocumentVersionResponse> versions = documentService.listVersions(documentId);
        return ResponseEntity.ok(versions);
    }

    /**
     * Request DTO for metadata update.
     */
    public static class UpdateMetadataRequest {
        private String documentType;
        private String description;

        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
