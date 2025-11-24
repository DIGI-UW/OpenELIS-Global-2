# Feature Specification: [FEATURE NAME]
# Feature Specification: Patient ID Card Scanning and Management

**Feature Branch**: `001-patient-id-cards`  
**Created**: 2025-11-24  
**Status**: Draft  
**Input**: User description: "Add ability to scan and attach patient ID cards (National ID, Insurance cards) to patient records"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Attach ID during patient registration (Priority: P1)

As a registration staff member, I can scan or upload one or more identification documents when creating a new patient so that the patient's record contains verifiable ID documents at point of registration.

**Why this priority**: Capturing ID at registration improves patient verification and reduces later follow-ups.

**Independent Test**: Create a new patient, use the Add Document flow to upload two documents (National ID, Insurance Card). Verify thumbnails appear, documents saved and visible in patient summary and in FHIR resources.

**Acceptance Scenarios**:
1. Given the Add New Patient form is open, When the user clicks "Add Document" and uploads a valid image/PDF (<=10MB), Then the document appears as a thumbnail with type, timestamp and action buttons.
2. Given multiple documents uploaded during registration, When the patient is saved, Then all documents are persisted and an audit record for each upload is created.

---

### User Story 2 - Manage documents from patient search (Priority: P2)

As a registration staff member, I can see a document indicator in patient search results and preview documents from the results so that I can quickly verify identity without opening the full record.

**Why this priority**: Quick verification speeds common workflows and reduces clicks.

**Independent Test**: Search for a patient with documents; verify "Documents" column shows a count badge; click badge to open a modal preview and view metadata.

**Acceptance Scenarios**:
1. Given search results contain patients with documents, When the results render, Then a Documents column displays a document icon with a count badge for each applicable row.
2. Given the user clicks the badge, When the modal opens, Then the user can view and navigate documents and see metadata.

---

### User Story 3 - Edit and version documents (Priority: P3)

As a registration staff member or manager, I can edit document metadata, replace images, and see version history so that corrections and updates are auditable.

**Why this priority**: Ensures accuracy and maintains provenance of sensitive documents.

**Independent Test**: Open a patient, edit a document's type and replace the file; verify audit entries and that previous version is retained as a soft-deleted/archive entry.

**Acceptance Scenarios**:
1. Given a document exists, When user edits type or replaces file, Then a new version is created, previous version remains (soft-deleted/archived) and audit logs record the old→new values.

---

### Edge Cases

- Upload interrupted by network error: system shows error, does not create incomplete record, and allows retry.
- Upload of corrupted or unreadable file: validation fails with clear message and file is rejected.
- Thumbnail generation failure: upload succeeds, thumbnail generation retried asynchronously, UI updated when thumbnail becomes available (placeholder icon shown until then).
- Malware scanning failure (timeout, service unavailable, or error): upload is rejected with error message requiring retry after scan service recovers.
- Rate limit exceeded: per-patient upload rate limit (e.g., 5 uploads per minute) exceeded returns HTTP 429 with clear error message indicating when retry is allowed.
- Storage backend unavailable: upload completes (metadata persisted), storage write queued for retry when storage recovers, UI shows "processing" status until storage write completes.
- Patient has large number (100+) of documents: thumbnails are paginated or lazily loaded to avoid UI slowdown.
- Concurrent edits: last-write-wins with audit trail capturing concurrent attempts.
- Attempting to upload a document type that already exists for the patient: system MUST automatically replace the existing document by creating a new version (previous version retained as soft-deleted). No confirmation dialog is required.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The UI MUST provide an "Identification Documents" section on the Add New Patient and Edit Patient screens with an "Add Document" / "Scan ID Card" action.
- **FR-002**: The system MUST support multiple upload methods: file picker (drag-and-drop), camera/scanner capture, and paste-from-clipboard.
- **FR-003**: The system MUST accept JPEG, PNG and PDF file formats and validate MIME type prior to upload.
- **FR-004**: The system MUST enforce a maximum file size of 10MB per document and reject larger files with an explanatory error.
- **FR-005**: The UI MUST display thumbnail previews for JPEG/PNG image files (generated automatically on upload) and 200x200px preview images from the first page of PDF documents. Thumbnails/previews MUST include document type, upload datetime, and action buttons (View, Edit, Delete). If thumbnail generation fails during upload, the upload MUST succeed and thumbnail generation MUST be retried asynchronously, with the UI updated when the thumbnail becomes available (placeholder icon shown until then).
- **FR-006**: The upload flow MUST require the user to select a document type (National ID, Insurance Card, Other) and, if "Other" is selected, allow entering a short description.
- **FR-019**: The system MUST enforce uniqueness: each patient can have at most one active (non-deleted) document per document type. If a document of the same type already exists, the system MUST automatically replace it by creating a new version of the existing IDDocument (no confirmation dialog required). The previous version is retained as soft-deleted for audit purposes.
- **FR-007**: The system MUST allow editing document metadata (type, description), replacing the document file, and maintain version history for replaced documents.
- **FR-008**: The system MUST implement soft delete for documents: deleted documents are marked deleted but retained for audit and possible restore.
- **FR-009**: Patient search results MUST display an "ID Documents" column showing a document-count indicator which is clickable to preview documents in a modal or open a document viewer.
- **FR-010**: Viewing a document MUST open a modal/lightbox with zoom controls for images and page navigation for PDFs, and display document metadata (type, upload date, uploaded by).
- **FR-011**: The system MUST record audit events for Upload, View, Edit, Delete actions including user, timestamp, patient ID, document type, filename, file size, and session/IP context where available.
- **FR-012**: Role-based access control MUST restrict actions: View (registration staff, lab techs, managers), Upload/Edit (registration staff, managers), Delete (managers, admins only).
- **FR-013**: The system MUST provide a malware/virus scanning interface (`MalwareScanner`) with a no-op implementation (`NoopMalwareScanner`) for development. Production deployments MUST integrate a malware scanning service (e.g., ClamAV, cloud-based scanning services) via this interface prior to making files available to users. If malware scanning fails (timeout, service unavailable, or error), the upload MUST be rejected with an error message requiring retry after the scan service recovers.
- **FR-014**: The system MUST validate file format integrity (detect corrupted/unreadable files) and provide clear feedback on failures. No minimum resolution or image quality thresholds are required.
- **FR-020**: The system MUST enforce per-patient rate limiting on document uploads (e.g., 5 uploads per minute per patient record) to prevent abuse and DoS attacks. Rate limit violations MUST return an HTTP 429 (Too Many Requests) response with a clear error message indicating when retry is allowed.
- **FR-021**: If the storage backend (filesystem, S3, etc.) is unavailable during upload, the system MUST allow the upload to complete (metadata persisted to database) and queue the storage write operation for automatic retry when storage recovers. The UI MUST show a "processing" status indicator until the storage write completes successfully.
- **FR-015**: Documents attached to patients MUST be represented in FHIR resources (DocumentReference or Patient extensions) with metadata: contentType, creation date, title, and either a URL or embedded data per interoperability needs.
-
- *Unclear / decision required*: **FR-016**: Retention policy for soft-deleted and archived documents: Retain soft-deleted and archived documents for 7 years (available for audit/restore), after which an automated purge MAY run unless overridden by local/regulatory policy.
-
- *Unclear / decision required*: **FR-017**: Support for multi-page documents: Store front and back of ID cards as two linked documents (front/back relation). This keeps per-page metadata, thumbnails and versioning simple while allowing an on-demand combined PDF if required.

- **FR-018**: OCR (Optical Character Recognition) extraction is out of scope for initial delivery. The system focuses on document storage, metadata management, and viewing. OCR functionality may be added as a future enhancement without breaking changes to the core document management system.
### Key Entities *(include if feature involves data)*

- **Patient**: Existing patient record; documents are associated to this entity via IDs and FHIR linkage.
- **IDDocument**: Represents a logical document attached to a patient. Key attributes: id, patientId, documentType, description, createdAt, createdBy, isDeleted, currentVersionId. Uniqueness constraint: (patientId, documentType) must be unique for active (non-deleted) documents.
- **DocumentVersion**: Binary artifact and metadata for a specific version. Attributes: versionId, idDocumentId, filename, contentType, sizeBytes, storageUrlOrHash, createdAt, createdBy.
- **AuditLogEntry**: Records actions on documents. Attributes: actionType (UPLOAD/VIEW/EDIT/DELETE), userId, timestamp, patientId, documentId/version, details (old→new values), ipAddress, sessionId.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can attach at least one document during new patient registration in under 2 minutes in 90% of observed successful attempts (measured in QA tests).
- **SC-002**: System accepts valid files (JPEG/PNG/PDF) and rejects invalid types or files >10MB with a clear error message 100% of the time during automated tests.
- **SC-003**: Thumbnail previews generate successfully for 95% of image uploads within 5 seconds of upload under normal test environment conditions.
- **SC-004**: For any Upload/Edit/Delete/View action on ID documents, an audit log entry exists with required metadata in 100% of sampled events during QA.
- **SC-005**: The patient search results display document counts and allow previewing documents for 95% of patients with <=20 documents without perceptible UI lag (manual performance check).

## Assumptions

- Default authentication and session management are provided by the existing system; document actions will use the current user identity.
- FHIR mapping choices (Patient extensions vs DocumentReference) will be finalized during design; both approaches are supported conceptually and the spec describes required metadata rather than implementation details.
- Storage location (database BLOB, file storage, or cloud object storage) is a deployment decision. The feature must support either pattern via an abstraction.
- OCR (Optical Character Recognition) is out of scope for initial delivery. The system focuses on document storage, metadata management, and viewing capabilities.

## Questions / NEEDS CLARIFICATION (max 3)

1. Document retention period for soft-deleted/archived documents: Retain for 7 years (recorded in Clarifications).
2. Multi-page documents: Store front and back as two linked documents (front/back relation) (recorded in Clarifications).
3. OCR extraction: OCR is out of scope for initial delivery (recorded in Clarifications).

## FHIR Integration Notes

- Documents MUST be represented in FHIR with sufficient metadata. Options:
  - Use `DocumentReference` resources linked to the `Patient` with `content.attachment` containing `contentType`, `url` or `data`, `title`, and `creation` timestamp.
  - Alternatively, use Patient `extension` attachments or `Patient.photo` for patient photos when appropriate.
- Future OCR enhancements (if implemented) MUST store extracted identifiers in `Patient.identifier` using appropriate system URIs where available.

## Clarifications

### Session 2025-11-24

- Q: Document retention period for soft-deleted/archived documents: How long must deleted documents remain available for audit/restore? → A: 7 years
- Q: Multi-page documents—front/back handling: Should front/back be stored as single PDF or two linked docs? → A: Store as two linked documents (front/back relation)
- Q: OCR extraction / auto-population scope: What is the scope of OCR (Optical Character Recognition) extraction for ID documents? → A: OCR is out of scope for initial delivery — focus on document storage, metadata management, and viewing only
- Q: Malware/virus scanning implementation: How should malware/virus scanning be implemented for uploaded documents? → A: Interface with no-op implementation — create `MalwareScanner` interface with `NoopMalwareScanner` for dev; production implementations can integrate ClamAV, cloud services, etc.
- Q: Image quality validation requirements: What are the specific requirements for image quality validation (FR-014)? → A: No image quality validation — only validate file format and corruption detection
- Q: Document type uniqueness per patient: Can a patient have multiple documents of the same type (e.g., two National IDs)? → A: One document per type per patient (enforce uniqueness constraint)
- Q: Document replacement workflow: When uploading a document type that already exists, what should the UX flow be? → A: Automatically replace the existing document (create new version silently) — no confirmation dialog required
- Q: Thumbnail generation specifications: What are the requirements for thumbnail generation for image documents? → A: Generate thumbnails only for JPEG/PNG images; use PDF first page preview for PDFs
- Q: PDF first page preview specifications: For PDF documents, what are the requirements for generating the first page preview? → A: Generate 200x200px preview image from PDF first page (matches image thumbnail size)
- Q: Malware scanning failure handling: What should happen if malware scanning fails (timeout, service unavailable, or error) during document upload? → A: Reject upload with error message requiring retry after scan service recovers
- Q: Rate limiting/throttling for uploads: Should the system enforce rate limits on document uploads to prevent abuse or DoS? → A: Per-patient rate limit (e.g., 5 uploads per minute per patient record)
- Q: Storage quota/limits: Should there be a maximum number of documents per patient, or a total storage quota per patient? → A: No hard limits (rely on file size limits and pagination for UI performance)
- Q: Thumbnail generation failure handling: What should happen if thumbnail generation fails (e.g., corrupted image, PDF parsing error, or processing timeout)? → A: Retry thumbnail generation asynchronously and update UI when complete
- Q: Storage backend failure handling: What should happen if the storage service (filesystem, S3, etc.) is unavailable during upload or retrieval? → A: Allow upload to complete but queue storage write; show "processing" status in UI

## Security & Permissions (summary)

- Role-based access control must enforce view/upload/edit/delete permissions as defined in FR-012.
- Documents must be transmitted over HTTPS and stored encrypted at rest (deployment detail; requirement is to ensure encryption for sensitive ID images).
- All view and download actions must be logged for audit purposes.

## Testing Scenarios

1. Upload single National ID card during new patient creation — verify thumbnail, metadata, FHIR entry, and audit log.
2. Upload multiple documents (National ID + Insurance Card) for existing patient — verify list, counts and previews.
3. View document from patient search results — click badge, open preview modal, and view metadata.
4. Edit document type from "Other" to "Insurance Card" — verify audit log records change.
5. Delete document and verify audit trail entry and that document is soft-deleted (not visible in active list).
6. Attempt to upload unsupported file type (should be rejected with clear error).
7. Attempt to upload file exceeding size limit (should be rejected).
8. Verify FHIR Patient/DocumentReference contains required document metadata.
9. Verify role without delete permission cannot delete documents.
10. Test on tablet with camera capture (manual QA): capture an image and attach to patient record.

## Definition of Done

- UI components implemented and visually matching Carbon Design System standards.
- Unit tests for upload/validation logic and permission checks added.
- Integration tests verifying FHIR mapping and audit entries implemented.
- Security review completed (encryption at rest, HTTPS verified).
- Documentation updated (user manual and technical notes) and accessibility checks completed.
- Product Owner acceptance and QA verification of acceptance criteria.

## Related Issues
- OGC-59: Add the ability to capture or upload a photo to the patient (related)
- OGC-50: Add Diagnoses to the lab order (related enhancement)

## Notes
- Internationalization: Document type labels and UI text must be translatable via existing React Intl mechanism.
- Duplicate detection and automated similarity warnings are considered future enhancements unless requested.
