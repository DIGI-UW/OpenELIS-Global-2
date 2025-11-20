# Feature Specification: Patient ID Card Scanning and Management

**Feature Branch**: `008-patient-id-cards`
**Created**: 2025-11-20
**Status**: Draft
**Input**: User description from OGC-66: "Add ability to scan and attach patient ID cards (National ID, Insurance cards) to patient records"

## Clarifications

### Session 2025-11-20

**Clarification Analysis**: Based on analysis of the Figma Make prototype (file: UrAAxfl1zhRvaCctB3WPqB/components/IDDocumentsSection.tsx and TestRequest.tsx) and the specification, the following clarifications have been resolved.

#### Question 1: Document Upload Location and Workflow Integration

The spec states documents should be added on the "Add a new patient" screen, but the Figma Make shows the ID Documents section integrated into the multi-step "Test Request" workflow at the "Patient Info" step.

**Options:**

**A) Standalone "Add/Modify Patient" screen only**
- ID Documents section appears only on dedicated patient management screen
- Not integrated into Test Request workflow
- Users must navigate to patient edit screen to add documents after test request
- Pro: Simpler implementation, clear separation of concerns
- Con: Extra navigation steps, documents can't be added during test order creation

**B) Test Request workflow only (as shown in Figma Make)**
- ID Documents section integrated into "Patient Info" step of Test Request
- Only available when creating new patient during test order
- No standalone patient management screen for documents
- Pro: Matches Figma Make, streamlined workflow
- Con: Can't add documents outside test request context

**C) Both locations (dual integration)**
- ID Documents section available on both "Add/Modify Patient" screen AND "Test Request" workflow
- Same component reused in both contexts
- Documents can be added/edited in either workflow
- Pro: Maximum flexibility, matches real-world workflows
- Con: More complex, requires careful state management

Which integration approach should be implemented?

---

#### Question 2: Collapsible Section Behavior and Default State

The Figma Make shows the "Identification Documents" section as a collapsible accordion with expand/collapse toggle, while the spec doesn't specify collapsibility.

**Options:**

**A) Always expanded (non-collapsible)**
- Documents section always visible when on patient form
- No expand/collapse controls
- Documents grid immediately visible
- Pro: Simpler UI, documents always visible
- Con: Takes vertical space even when not needed

**B) Collapsible, collapsed by default**
- Section collapsed on initial load (as shown in Figma Make)
- User must click to expand to see/add documents
- Badge shows document count when collapsed
- Pro: Cleaner initial view, reduces cognitive load
- Con: Extra click required, documents might be forgotten

**C) Collapsible, expanded by default**
- Section expanded on initial load
- User can collapse if desired
- Retains expand/collapse state in session
- Pro: Documents visible by default, user can hide if needed
- Con: Takes space by default

**D) Collapsible, with intelligent default (expanded when documents exist)**
- Collapsed when patient has 0 documents
- Expanded when patient has 1+ existing documents
- Pro: Adaptive UI based on context
- Con: Inconsistent behavior might confuse users

Which collapsible behavior should be implemented?

---

#### Question 3: Upload Button Functionality - Single vs Dual Purpose

The Figma Make shows two separate buttons: "Upload Document" and "Scan ID Card", but both trigger the same file input. The spec lists them as separate methods but doesn't clarify if they should have different behaviors.

**Options:**

**A) Single unified button ("Upload Document")**
- One button that opens file picker supporting all methods (camera, files, clipboard)
- Simpler UI with one action
- Pro: Cleaner interface, less confusion
- Con: Doesn't distinguish between scanning and uploading pre-existing files

**B) Two separate buttons with same behavior (as in Figma Make)**
- "Upload Document" and "Scan ID Card" both open file picker
- Visual distinction but identical functionality
- Pro: Clear intent even if behavior is same
- Con: Potentially confusing if both do the same thing

**C) Two separate buttons with different behaviors**
- "Upload Document" opens file picker for selecting existing files
- "Scan ID Card" directly activates camera/scanner (if available)
- Pro: Clear functional distinction, optimized workflows
- Con: More complex, requires device capability detection

**D) Single button with dropdown for method selection**
- Main button "Add Document" with dropdown showing "Upload File" and "Scan with Camera"
- User selects method from dropdown
- Pro: One button, clear method selection
- Con: Extra click, less obvious for new users

Which upload button approach should be implemented?

---

#### Question 4: Document Type Selection Timing

The Figma Make shows document type selection BEFORE file upload, while the spec doesn't clarify the workflow timing.

**Options:**

**A) Select type before upload (as in Figma Make)**
- Dropdown for document type above upload buttons
- User selects type first, then uploads file
- Document type pre-set when upload completes
- Pro: Clear intent before upload, matches Figma Make
- Con: User might forget to select type, requires validation

**B) Select type after upload**
- User uploads file first
- Modal appears asking for document type
- Pro: Ensures user is prompted for type
- Con: Interrupts upload workflow, extra modal

**C) Select type during upload**
- File picker includes document type selector
- Type and file selected together
- Pro: Single-step process
- Con: Non-standard UI pattern, may require custom file picker

**D) Optional type before, required after if not set**
- User can optionally select type before upload
- If not selected, system prompts after upload
- Pro: Flexible, prevents missing type
- Con: Inconsistent workflow, more complex logic

Which document type selection workflow should be implemented?

---

#### Question 5: Document Grid Layout and Responsiveness

The Figma Make shows a 3-column grid for document thumbnails, but the spec mentions "responsive grid layout with 3 columns on desktop, 2 on tablet, 1 on mobile". The spec doesn't clarify maximum visible documents before pagination.

**Options:**

**A) Unlimited grid with scrolling**
- All documents displayed in scrollable grid
- No pagination, just vertical scroll
- Pro: Simple, all documents visible
- Con: Performance issues with many documents

**B) Paginated grid (10 documents per page)**
- Maximum 10 documents visible at once
- Pagination controls below grid
- Pro: Better performance, matches spec mention of FR-027
- Con: Extra clicks to see all documents

**C) Virtual scrolling**
- Render only visible documents in viewport
- Smooth infinite scroll
- Pro: Best performance with many documents
- Con: More complex implementation

**D) Collapsible pagination (show first 6, "Show more" button)**
- Initial display shows first 6 documents
- "Show 4 more" button expands to show next batch
- Pro: Progressive disclosure, good UX
- Con: Requires batch loading logic

Which grid layout and pagination approach should be implemented?

---

#### Question 6: Document Viewer Modal Features

The Figma Make shows a basic modal with image preview and close button, while the spec mentions "zoom controls, document metadata, and navigation controls" but doesn't show these in the Make.

**Options:**

**A) Basic modal (as in Figma Make)**
- Image display with close button
- Document metadata (type, filename) in header
- No zoom or navigation controls
- Pro: Simple, matches Figma Make
- Con: Limited functionality for detailed viewing

**B) Full-featured modal with zoom**
- Zoom in/out buttons (+/-), fit-to-screen button
- Pan image when zoomed
- Document metadata displayed
- Pro: Better for detailed inspection
- Con: More complex, not shown in Make

**C) Full-featured modal with multi-document navigation**
- All features from Option B
- Previous/Next buttons to navigate between patient's documents
- Document count indicator (e.g., "2 of 3")
- Pro: Efficient multi-document viewing
- Con: Most complex, deviates from Make

**D) External viewer link**
- Modal shows thumbnail with "Open in new tab" button
- Full document opens in separate browser tab
- Browser provides native zoom controls
- Pro: Leverages browser capabilities
- Con: Loses modal context, multiple tabs

Which document viewer features should be implemented?

---

#### Question 7: Document Search Results Integration

The spec mentions adding a "Documents" column to patient search results, but the Figma Make's TestRequest.tsx doesn't show this column in the "Patient Results" table.

**Options:**

**A) Add new "Documents" column to existing table**
- New column added between existing columns
- Shows document count badge (e.g., "📄 3")
- Clickable to open document modal
- Pro: Highly visible, matches spec
- Con: Table becomes wider, may require horizontal scroll

**B) Add "Documents" column at end of table**
- Last column in table
- Always visible without horizontal scroll
- Pro: Doesn't disrupt existing columns
- Con: Less visible, requires scrolling to see

**C) Integrate into "Actions" column**
- Add document icon button to existing Actions dropdown/buttons
- Hovering shows document count
- Pro: No new column needed
- Con: Less discoverable, hidden in actions

**D) Add row expansion for documents**
- Clicking row expands to show documents below
- No new column needed
- Pro: Clean table, detailed view on demand
- Con: Requires row expansion implementation

Which search results integration approach should be implemented?

---

#### Question 8: "Other" Document Type Description Requirement

The Figma Make shows an optional "Description" field when "Other" document type is selected, but the spec states "Other (with optional text field for description)" without clarifying if it should be required or truly optional.

**Options:**

**A) Optional description for "Other" type**
- Text field appears when "Other" selected
- Can be left empty
- Document saved as "Other" with no description
- Pro: Flexible, matches "optional" in spec
- Con: Unclear what document is if no description provided

**B) Required description for "Other" type**
- Text field appears when "Other" selected
- Must provide description before upload
- Validation prevents empty description
- Pro: Ensures clarity for "Other" documents
- Con: Forces user input, deviates from "optional"

**C) Optional during upload, required for searching/filtering**
- Can upload without description
- System prompts for description when document is viewed/edited
- Pro: Doesn't block upload workflow
- Con: Incomplete metadata initially

Which approach should be used for "Other" document type descriptions?

---

### Resolved Clarifications

(No clarifications resolved yet - awaiting user input)

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Upload ID Documents During Patient Registration (Priority: P1)

A laboratory registration staff member is registering a new patient who presents a National ID card and an insurance card. They need to scan or upload these documents and attach them to the patient record to maintain digital copies for verification and record-keeping.

**Why this priority**: Digital ID card storage is essential for patient identity verification, regulatory compliance, and reducing manual record-keeping errors. This is the foundational capability required before any document viewing or management features can be used. Many countries (including Madagascar) have ID card requirements in their workplans.

**Given** I am a registration staff member on the "Add a new patient" screen
**When** I fill in patient demographics and reach the "Identification Documents" section
**Then** I should see an "Add Document" button and a "Scan ID Card" button

**Given** I click "Add Document" or "Scan ID Card"
**When** I select a document type (National ID, Insurance Card, Other) and upload a JPEG/PNG/PDF file under 10MB
**Then** The system should accept the upload, generate a thumbnail preview, and display it in the documents grid with document type, upload date, and action buttons (View, Edit, Delete)

**Given** I upload multiple documents (e.g., National ID and Insurance Card)
**When** I save the new patient record
**Then** All uploaded documents should be stored in the FHIR Patient resource and associated with the patient

**Independent Test**: Create a new patient with 2 ID card scans (National ID + Insurance Card). Verify both documents appear in the patient search results and can be viewed from the patient detail page.

---

### User Story 2 - View and Manage ID Documents in Patient Search (Priority: P1)

A laboratory technician is searching for a patient to add a new test order. They need to quickly verify the patient's identity by viewing their ID card documents directly from the search results to ensure they're selecting the correct patient.

**Why this priority**: Quick access to ID documents from search results reduces identity verification time and prevents selecting the wrong patient. This is critical for patient safety and operational efficiency in high-volume registration environments.

**Given** I am on the "Patient Info" tab of "Test Request"
**When** I search for a patient and view the results table
**Then** I should see a new "Documents" column showing an indicator with the count of attached documents (e.g., "📄 3")

**Given** I see a patient with ID documents in the search results
**When** I click on the document count indicator
**Then** A modal should open displaying thumbnail previews of all documents with their types and upload dates

**Given** The document modal is open
**When** I click "View" on any document
**Then** The full document should open in a lightbox with zoom controls, document metadata, and navigation controls

**Independent Test**: Search for a patient with 3 ID documents. Click the document indicator in search results. Verify modal opens with 3 document thumbnails. Click "View" on each document and verify full resolution display.

---

### User Story 3 - Edit and Delete ID Documents (Priority: P2)

A laboratory manager discovers that a document was uploaded with the wrong type (marked as "Other" instead of "Insurance Card"). They need to edit the document type and, if necessary, delete outdated documents while maintaining a full audit trail of all changes.

**Why this priority**: Document management features ensure data accuracy and allow correction of human errors. Audit trail compliance is required for regulatory purposes in many jurisdictions. While not as critical as viewing documents, these capabilities are essential for maintaining data quality.

**Given** I am a lab manager editing an existing patient record
**When** I navigate to the "Identification Documents" section
**Then** I should see all existing documents with their current types, upload dates, and action buttons

**Given** I click "Edit" on a document marked as "Other"
**When** I change the document type to "Insurance Card" and add an optional description
**Then** The system should update the document type, log the change in the audit trail, and display the updated information

**Given** I click "Delete" on an outdated document
**When** I confirm the deletion in the confirmation dialog
**Then** The system should soft-delete the document (mark as deleted but retain in database), log the deletion in the audit trail, and remove it from the visible document list

**Given** I have "Registration staff" role without delete permissions
**When** I attempt to view the delete button
**Then** The delete button should not be visible or should be disabled

**Independent Test**: Edit a document type from "Other" to "Insurance Card". Verify audit trail entry shows "User: [username] | Action: EDIT | Document: Changed type from 'Other' to 'Insurance Card' | Timestamp: [datetime]". Delete a document as Lab Manager. Verify soft delete and audit trail entry.

---

## Requirements _(mandatory)_

### Functional Requirements

#### Document Upload and Storage

- **FR-001**: System MUST provide an "Add Document" button and a "Scan ID Card" button in the "Identification Documents" section of the "Add a new patient" screen
- **FR-002**: System MUST support multiple upload methods including file upload (drag-and-drop or file picker), camera/scanner integration for direct capture, and paste from clipboard
- **FR-003**: System MUST support common image formats: JPEG, PNG, and PDF
- **FR-004**: System MUST enforce maximum file size limit of 10MB per document
- **FR-005**: System MUST automatically compress and resize images for storage efficiency while maintaining readable quality
- **FR-006**: System MUST allow multiple documents per patient with no fixed limit
- **FR-007**: System MUST require document type selection from dropdown or radio buttons: National ID, Insurance Card, or Other (with optional text field for description)
- **FR-008**: System MUST make document type a required field when uploading
- **FR-009**: System MUST allow editing of document type after upload
- **FR-010**: System MUST generate thumbnail previews for all uploaded documents
- **FR-011**: System MUST validate file type before upload and reject unsupported formats with user-friendly error message
- **FR-012**: System MUST validate file size and reject files exceeding 10MB limit with error message indicating maximum allowed size
- **FR-013**: System MUST scan uploaded files for malware/viruses before storing
- **FR-014**: System MUST validate image quality including minimum resolution and corruption detection
- **FR-015**: System MUST implement file naming convention: `patient_{patientId}_doc_{timestamp}_{documentType}.{ext}`

#### Document Display and Viewing

- **FR-016**: System MUST display thumbnail previews of uploaded documents in a grid/list layout in the "Identification Documents" section
- **FR-017**: System MUST display for each uploaded document: thumbnail preview, document type label, upload date/time, and action buttons (View, Edit, Delete)
- **FR-018**: System MUST add a new "Documents" column to the patient search results table showing an icon/indicator with the count of attached documents (e.g., "📄 2" or document icon with count badge)
- **FR-019**: System MUST make the document count indicator clickable to preview documents in a modal/lightbox
- **FR-020**: System MUST open clicked documents in a modal/lightbox overlay with full resolution display
- **FR-021**: System MUST support zoom in/out controls for image documents in the viewer
- **FR-022**: System MUST support page navigation controls for PDF documents
- **FR-023**: System MUST include close button and navigation controls in the document viewer
- **FR-024**: System MUST display document metadata in the viewer including document type, upload date, and uploaded by user
- **FR-025**: System MUST implement lazy loading for document thumbnails in patient search results to optimize performance
- **FR-026**: System MUST use thumbnail generation for preview without loading full resolution images in grid views
- **FR-027**: System MUST implement pagination if patient has more than 10 documents

#### Document Editing

- **FR-028**: System MUST display existing ID documents when editing a patient record
- **FR-029**: System MUST allow adding new documents to existing patient records
- **FR-030**: System MUST allow changing the document type for existing documents
- **FR-031**: System MUST allow adding or editing optional description/notes for documents
- **FR-032**: System MUST allow replacing the image by uploading a new version
- **FR-033**: System MUST track version history if document is replaced
- **FR-034**: System MUST display upload date and last modified date for each document

#### Document Deletion

- **FR-035**: System MUST require confirmation dialog before deleting a document
- **FR-036**: System MUST implement soft delete: mark document as deleted but retain in database for audit purposes
- **FR-037**: System MUST only allow users with "Lab Manager" or "System Administrator" roles to delete documents
- **FR-038**: System MUST log all deletion actions in the audit trail with complete details

#### FHIR Integration

- **FR-039**: System MUST store scanned ID cards as attachments in the FHIR Patient resource or as linked DocumentReference resources
- **FR-040**: System MUST use DocumentReference resource type with link to Patient for ID card scans
- **FR-041**: System MUST include document metadata in FHIR resource: contentType (MIME type), creation date, title (document type), and url or data (base64 encoded if embedded)
- **FR-042**: System MUST set DocumentReference status to "current" for active documents
- **FR-043**: System MUST set DocumentReference status to "superseded" for replaced document versions
- **FR-044**: System MUST populate DocumentReference.type with appropriate LOINC or SNOMED code for ID document types
- **FR-045**: System MUST link DocumentReference to Patient resource via DocumentReference.subject field
- **FR-046**: System MUST populate DocumentReference.author with reference to user who uploaded the document
- **FR-047**: System MUST populate DocumentReference.date with upload timestamp
- **FR-048**: System MUST store document content in DocumentReference.content with attachment including contentType, url/data, title, and creation date

#### Audit Trail

- **FR-049**: System MUST log all document-related actions in the audit trail with action type: Upload, View, Edit (type change), Replace, Delete
- **FR-050**: System MUST capture in audit log: username/ID of person performing action, timestamp (date and time), patient ID, document details (type, file name, file size)
- **FR-051**: System MUST capture for edit actions: old value → new value
- **FR-052**: System MUST capture user's IP address in audit log when available
- **FR-053**: System MUST capture session ID in audit log for traceability
- **FR-054**: System MUST display audit log entries in format: "User: [username] | Action: [ACTION] | Patient: [patientId] | Document: [type] | Details: [details] | Timestamp: [datetime]"
- **FR-055**: System MUST log document view actions including which document was viewed and by whom

#### Security and Permissions

- **FR-056**: System MUST implement role-based access control for document viewing: Registration staff, Lab technicians, and Lab managers can view documents
- **FR-057**: System MUST implement role-based access control for upload/edit: Registration staff and Lab managers can upload and edit documents
- **FR-058**: System MUST implement role-based access control for deletion: Only Lab managers and System administrators can delete documents
- **FR-059**: System MUST encrypt sensitive ID card images at rest using AES-256 encryption or equivalent
- **FR-060**: System MUST use HTTPS for all document transmission
- **FR-061**: System MUST implement access logging tracking who viewed which document when
- **FR-062**: System MUST implement CSRF protection on all document upload, edit, and delete endpoints

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO custom CSS frameworks. Patient ID card interface must use: Carbon Button, Carbon FileUploader (with drag-and-drop), Carbon Tile for document thumbnails, Carbon Modal for document viewer, Carbon DataTable for search results, Carbon InlineNotification for upload status, Carbon IconButton for action buttons (View, Edit, Delete), Carbon Tooltip for document metadata display
- **CR-002**: All UI strings MUST be internationalized via React Intl (no hardcoded text) - includes all labels, error messages, button text, warnings, tooltips, document type options, and confirmation dialogs
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form):
  - Valueholders: `PatientDocument` entity with JPA/Hibernate annotations
  - DAOs: `PatientDocumentDAO` with queries for retrieving documents by patient ID, document type, and status
  - Services: `PatientDocumentService` with @Transactional annotation for upload, edit, delete operations; `PatientDocumentStorageService` for file storage and retrieval; `PatientDocumentAuditService` for audit logging
  - Controllers: `PatientDocumentController` (NO @Transactional in controllers) with endpoints for upload, view, edit, delete
  - Forms: `PatientDocumentForm` for data binding with validation annotations including file type, file size, and document type validation
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML) - includes new `patient_document` table with fields: id, patient_id (foreign key to patient.id), document_type, file_name, file_path, file_size, mime_type, thumbnail_path, upload_date, uploaded_by_user_id, last_modified_date, modified_by_user_id, is_deleted, deleted_date, deleted_by_user_id, version, description; indexes for patient_id, document_type, is_deleted, and upload_date
- **CR-005**: External data integration MUST use FHIR R4 DocumentReference resource linked to Patient resource for ID card scans with proper contentType, url/data, title, creation date, and author fields
- **CR-006**: Configuration-driven variation for country-specific requirements (NO code branching) - use configuration for enabling/disabling document types, maximum file sizes, storage locations (database/filesystem/cloud), encryption requirements, and OCR integration
- **CR-007**: Security MUST implement RBAC for view/upload/edit/delete permissions, audit trail with sys_user_id + timestamp for all document actions, input validation for file types and sizes, malware scanning, encryption at rest, HTTPS transmission, CSRF protection on upload/edit/delete endpoints
- **CR-008**: Tests MUST be included with >70% coverage goal:
  - Unit tests: File validation logic, thumbnail generation, permission checking, FHIR resource transformation, audit logging
  - Integration tests: Complete upload workflow, document viewing from search results, edit/delete operations, FHIR synchronization, role-based access control
  - E2E tests (Cypress): Upload documents during patient registration, view documents from search results, edit document type, delete document with confirmation, verify audit trail entries

### UI/UX Requirements

- **UX-001**: System MUST add "Identification Documents" section below "Additional Information" on the "Add a new patient" screen
- **UX-002**: System MUST display "Add Document" and "Scan ID Card" buttons prominently at top of "Identification Documents" section
- **UX-003**: System MUST display uploaded documents in a responsive grid layout with 3 columns on desktop, 2 on tablet, 1 on mobile
- **UX-004**: System MUST display document thumbnail preview, document type label, upload date in "YYYY-MM-DD" format, and action buttons for each document in the grid
- **UX-005**: System MUST use icon buttons for View (eye icon), Edit (pencil icon), and Delete (trash icon) actions
- **UX-006**: System MUST show upload progress indicator during file upload with percentage complete
- **UX-007**: System MUST display success notification after successful upload with message "Document uploaded successfully"
- **UX-008**: System MUST display error notification if upload fails with specific error reason (file too large, invalid format, etc.)
- **UX-009**: System MUST display document count badge on document indicator in patient search results (e.g., "📄 3" or document icon with "3" badge)
- **UX-010**: System MUST open document viewer modal on center of screen with overlay background
- **UX-011**: System MUST display zoom controls (+, -, fit to screen) in document viewer for image documents
- **UX-012**: System MUST display page navigation controls (previous, next, page X of Y) in document viewer for PDF documents
- **UX-013**: System MUST display confirmation dialog with message "Are you sure you want to delete this document? This action cannot be undone." before document deletion
- **UX-014**: System MUST work on both desktop and tablet devices with touch-friendly controls for mobile devices

### Edge Cases and Error Handling

#### File Upload Edge Cases

- **EC-001**: If user attempts to upload file exceeding 10MB limit, system MUST display error "File size exceeds maximum limit of 10MB. Please select a smaller file."
- **EC-002**: If user attempts to upload unsupported file type, system MUST display error "File type not supported. Please upload JPEG, PNG, or PDF files only."
- **EC-003**: If malware is detected in uploaded file, system MUST reject upload, log security incident, and display error "File upload rejected. Security scan detected potential threat."
- **EC-004**: If image quality validation fails (corrupted file, too low resolution), system MUST display error "Image quality insufficient. Please upload a clear, readable image."
- **EC-005**: If storage quota is exceeded, system MUST display error "Storage limit reached. Please contact administrator to increase storage capacity."
- **EC-006**: If upload fails due to network error, system MUST allow retry and display error "Upload failed due to network error. Please try again."

#### Document Viewing Edge Cases

- **EC-007**: If document file is missing from storage but database record exists, system MUST display error "Document file not found. Please contact administrator."
- **EC-008**: If user lacks permission to view documents, system MUST hide document count indicator in search results and display "Access denied" message if attempting direct access
- **EC-009**: If patient has no documents, system MUST display message "No identification documents uploaded" in the "Identification Documents" section
- **EC-010**: If thumbnail generation fails, system MUST display generic document icon placeholder

#### Document Editing Edge Cases

- **EC-011**: If user attempts to replace document with invalid file type, system MUST reject replacement and display appropriate error message
- **EC-012**: If concurrent edit occurs (two users editing same document), system MUST detect conflict and display warning "This document was modified by another user. Please refresh and try again."
- **EC-013**: If user attempts to change document type to empty/invalid value, system MUST prevent save and display error "Document type is required."

#### Document Deletion Edge Cases

- **EC-014**: If user without delete permission attempts to access delete endpoint directly, system MUST return 403 Forbidden error and log unauthorized access attempt
- **EC-015**: If document is already deleted (soft deleted), system MUST prevent duplicate deletion and display message "Document already deleted"
- **EC-016**: If referenced FHIR DocumentReference resource fails to update status to "superseded" during deletion, system MUST log error but complete database soft delete to maintain internal consistency

#### FHIR Integration Edge Cases

- **EC-017**: If FHIR server is unavailable during document upload, system MUST complete database storage, create pending FHIR sync task, and display warning "Document saved locally. FHIR synchronization pending."
- **EC-018**: If FHIR DocumentReference creation fails validation, system MUST log detailed validation errors and create alert for technical staff
- **EC-019**: If patient has no FHIR Patient resource, system MUST create FHIR Patient resource before creating DocumentReference
- **EC-020**: If document encoding to base64 exceeds FHIR resource size limits, system MUST store document via URL reference instead of embedded data

## Data Model _(mandatory)_

### Database Entities

#### PatientDocument Table

New entity for storing patient ID card documents:

- `id` (BIGSERIAL, primary key)
- `patient_id` (BIGINT, NOT NULL, foreign key to patient.id) - The patient this document belongs to
- `document_type` (VARCHAR(50), NOT NULL) - Type of document: "NATIONAL_ID", "INSURANCE_CARD", "OTHER"
- `document_type_description` (VARCHAR(255), NULL) - Optional description when document_type is "OTHER"
- `file_name` (VARCHAR(255), NOT NULL) - Original file name
- `file_path` (VARCHAR(500), NOT NULL) - Path to stored file (relative or absolute depending on storage strategy)
- `file_size` (BIGINT, NOT NULL) - File size in bytes
- `mime_type` (VARCHAR(100), NOT NULL) - MIME type: "image/jpeg", "image/png", "application/pdf"
- `thumbnail_path` (VARCHAR(500), NULL) - Path to thumbnail image
- `storage_location` (VARCHAR(50), NOT NULL) - Storage type: "DATABASE", "FILESYSTEM", "CLOUD"
- `upload_date` (TIMESTAMP, NOT NULL, default CURRENT_TIMESTAMP) - When document was uploaded
- `uploaded_by_user_id` (BIGINT, NOT NULL, foreign key to system_user.id) - User who uploaded document
- `last_modified_date` (TIMESTAMP, NULL) - When document was last modified
- `modified_by_user_id` (BIGINT, NULL, foreign key to system_user.id) - User who last modified document
- `is_deleted` (BOOLEAN, NOT NULL, default FALSE) - Soft delete flag
- `deleted_date` (TIMESTAMP, NULL) - When document was deleted
- `deleted_by_user_id` (BIGINT, NULL, foreign key to system_user.id) - User who deleted document
- `version` (INTEGER, NOT NULL, default 1) - Version number for document replacement tracking
- `replaced_by_document_id` (BIGINT, NULL, foreign key to patient_document.id) - Links to newer version if replaced
- `fhir_document_reference_id` (VARCHAR(255), NULL) - FHIR DocumentReference resource ID for this document
- Indexes:
  - `idx_patient_document_patient` (patient_id) - For retrieving all documents for a patient
  - `idx_patient_document_type` (document_type) - For filtering by document type
  - `idx_patient_document_deleted` (is_deleted) - For excluding deleted documents
  - `idx_patient_document_upload_date` (upload_date) - For sorting by upload date
  - `idx_patient_document_fhir` (fhir_document_reference_id) - For FHIR synchronization

#### PatientDocumentAudit Table

New entity for audit trail of document actions:

- `id` (BIGSERIAL, primary key)
- `patient_document_id` (BIGINT, NOT NULL, foreign key to patient_document.id) - The document this audit entry relates to
- `patient_id` (BIGINT, NOT NULL, foreign key to patient.id) - The patient (denormalized for faster queries)
- `action` (VARCHAR(50), NOT NULL) - Action type: "UPLOAD", "VIEW", "EDIT", "REPLACE", "DELETE"
- `action_details` (TEXT, NULL) - JSON object with action-specific details (e.g., old value → new value for edits)
- `performed_by_user_id` (BIGINT, NOT NULL, foreign key to system_user.id) - User who performed action
- `action_timestamp` (TIMESTAMP, NOT NULL, default CURRENT_TIMESTAMP) - When action occurred
- `ip_address` (VARCHAR(45), NULL) - User's IP address (supports IPv6)
- `session_id` (VARCHAR(255), NULL) - Session identifier for traceability
- `file_name` (VARCHAR(255), NULL) - File name at time of action
- `file_size` (BIGINT, NULL) - File size at time of action
- `document_type` (VARCHAR(50), NULL) - Document type at time of action
- Indexes:
  - `idx_patient_document_audit_document` (patient_document_id) - For retrieving audit history for a document
  - `idx_patient_document_audit_patient` (patient_id) - For retrieving all document audits for a patient
  - `idx_patient_document_audit_timestamp` (action_timestamp) - For time-based queries
  - `idx_patient_document_audit_user` (performed_by_user_id) - For user activity tracking

### FHIR Resources

#### DocumentReference Resource (for ID Card Scans)

```json
{
  "resourceType": "DocumentReference",
  "id": "doc-patient-123-national-id-001",
  "status": "current",
  "type": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "47420-5",
        "display": "Functional status assessment note"
      }
    ],
    "text": "National ID Card"
  },
  "category": [
    {
      "coding": [
        {
          "system": "http://openelis-global.org/fhir/document-category",
          "code": "identification",
          "display": "Identification Document"
        }
      ]
    }
  ],
  "subject": {
    "reference": "Patient/patient-123",
    "display": "John Smith"
  },
  "date": "2025-11-20T10:23:45Z",
  "author": [
    {
      "reference": "Practitioner/user-jsmith",
      "display": "Jane Smith (Registration Staff)"
    }
  ],
  "description": "National ID Card - Front",
  "content": [
    {
      "attachment": {
        "contentType": "image/jpeg",
        "url": "https://storage.openelis.org/documents/patient_123_doc_20251120102345_NATIONAL_ID.jpg",
        "size": 2458624,
        "title": "National ID - Front",
        "creation": "2025-11-20T10:23:45Z"
      },
      "format": {
        "system": "http://ihe.net/fhir/ValueSet/IHE.FormatCode.codesystem",
        "code": "urn:ihe:iti:xds:2017:mimeTypeSufficient",
        "display": "mimeType Sufficient"
      }
    }
  ],
  "context": {
    "encounter": [
      {
        "reference": "Encounter/visit-123"
      }
    ],
    "period": {
      "start": "2025-11-20T10:23:45Z"
    }
  }
}
```

**For replaced documents (superseded version):**
```json
{
  "resourceType": "DocumentReference",
  "id": "doc-patient-123-national-id-001",
  "status": "superseded",
  "relatesTo": [
    {
      "code": "replaces",
      "target": {
        "reference": "DocumentReference/doc-patient-123-national-id-002"
      }
    }
  ],
  ...
}
```

**Extension for OpenELIS-specific metadata:**
```json
{
  "extension": [
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/document-version",
      "valueInteger": 2
    },
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/document-internal-id",
      "valueString": "patient-doc-456"
    }
  ]
}
```

## Success Criteria _(mandatory)_

- **SC-001**: Registration staff can upload National ID and Insurance Card documents during new patient registration with success rate >95%
- **SC-002**: Document thumbnails load in patient search results within 500ms per page of results
- **SC-003**: Document viewer modal opens and displays full resolution image within 2 seconds
- **SC-004**: All document actions (upload, view, edit, delete) are logged in audit trail with 100% accuracy including user, timestamp, and action details
- **SC-005**: System rejects invalid file types and oversized files with clear error messages in 100% of cases
- **SC-006**: Lab managers can delete documents with confirmation, and deleted documents remain in database with is_deleted=TRUE for audit purposes
- **SC-007**: FHIR DocumentReference resources are created for all uploaded documents with 100% success rate (or pending sync task created if FHIR server unavailable)
- **SC-008**: Role-based access control prevents unauthorized document deletion in 100% of test cases (users without delete permission cannot access delete endpoint)
- **SC-009**: Document count indicator displays correctly in patient search results showing accurate count of non-deleted documents
- **SC-010**: System handles concurrent uploads from 20 users without data corruption or performance degradation
- **SC-011**: Encrypted document storage prevents unauthorized file system access - documents cannot be viewed without application authentication
- **SC-012**: Document thumbnail generation completes within 3 seconds for images up to 10MB
- **SC-013**: System maintains >70% unit test coverage, >60% integration test coverage, and E2E tests for all critical user journeys
- **SC-014**: Audit log entries are formatted correctly and include all required fields (user, action, timestamp, patient ID, document details) in 100% of cases
- **SC-015**: Soft delete mechanism retains document metadata and file for minimum 7 years (configurable retention period)

## Technical Architecture _(optional)_

### Storage Strategy Options

**Option 1: Database Storage (BLOB/base64)**
- Pros: Easier backup, FHIR compliant, transactional consistency
- Cons: Larger database size, potential performance impact for large files
- Recommendation: Use for deployments with <10,000 documents or strict compliance requirements

**Option 2: File System Storage**
- Pros: Better performance for large files, lower database size
- Cons: Requires separate backup strategy, file system permissions management
- Recommendation: Use for deployments with 10,000-100,000 documents

**Option 3: Cloud Storage (S3, Azure Blob, Google Cloud Storage)**
- Pros: Scalable, cost-effective, built-in redundancy, CDN integration
- Cons: External dependency, network latency, requires cloud account
- Recommendation: Use for large deployments (>100,000 documents) or multi-site deployments

### File Naming Convention

```
patient_{patientId}_doc_{timestamp}_{documentType}.{ext}

Examples:
patient_00000123_doc_20251120102345_NATIONAL_ID.jpg
patient_00000123_doc_20251120103012_INSURANCE_CARD.png
patient_00000456_doc_20251120104521_OTHER.pdf
```

### Thumbnail Generation

- Generate thumbnails on upload (async job)
- Target thumbnail size: 200x200 pixels (maintain aspect ratio)
- Store thumbnails in separate directory/bucket: `/thumbnails/` prefix
- Fallback to generic document icon if thumbnail generation fails

### Security Considerations

- Encrypt documents at rest using AES-256 encryption
- Store encryption keys in secure key management system (not in application code)
- Use signed URLs for cloud storage with expiration (default: 1 hour)
- Implement rate limiting on upload endpoint (10 uploads per minute per user)
- Scan uploads with antivirus/malware scanner before storage
- Validate file headers (magic bytes) in addition to file extension

### Performance Optimization

- Use CDN for cloud-stored documents to reduce latency
- Implement document caching (cache thumbnails for 24 hours)
- Use lazy loading for document thumbnails in search results
- Paginate document list if patient has >10 documents
- Compress large images (>1MB) to reduce storage and bandwidth

---

## Future Enhancements _(optional)_

### OCR Integration (Phase 2)

- Extract text from ID cards using OCR (Optical Character Recognition)
- Auto-populate patient fields (name, DOB, ID numbers) from extracted text
- Highlight confidence scores for extracted fields
- Allow manual correction of extracted data
- Support multiple languages and ID card formats

### Duplicate Detection (Phase 2)

- Implement perceptual hashing for image comparison
- Warn user if uploading document that looks identical to existing one
- Suggest replacing existing document instead of creating duplicate
- Detect near-duplicates (same document, different resolution/quality)

### Multi-page Document Support (Phase 2)

- Support front and back of ID card as separate pages within single document
- Allow uploading multiple images for a single document
- Display page navigator in document viewer (Page 1 of 2)
- Store page order and allow reordering

### Document Expiration Tracking (Phase 3)

- Add expiration_date field for ID cards and insurance cards
- Generate alerts for expired documents (30 days before expiration)
- Display expiration status in patient search results (expired, expiring soon, valid)
- Prevent test orders for patients with expired insurance cards (configurable)

### Batch Upload (Phase 3)

- Allow uploading multiple documents at once (zip file or multiple file selection)
- Auto-detect document type from file names or content
- Display batch upload progress with individual file status
- Provide summary report of successful/failed uploads

### Document Sharing (Phase 4)

- Generate secure shareable links for documents (expiring links)
- Allow patients to access their own documents via patient portal
- Support FHIR DocumentReference exchange with other systems
- Implement document access consent management

---

## Migration and Rollout Plan _(optional)_

### Phase 1: Development and Testing (Weeks 1-4)
- Implement core upload and viewing functionality
- Implement FHIR DocumentReference integration
- Implement audit trail
- Unit and integration testing
- Security review and penetration testing

### Phase 2: Pilot Deployment (Weeks 5-6)
- Deploy to testing.openelis-global.org
- Pilot with 2-3 facilities in one country (Madagascar priority)
- Collect user feedback on UI/UX
- Monitor performance and storage usage
- Address bugs and usability issues

### Phase 3: Production Rollout (Weeks 7-8)
- Deploy to production environments
- Provide training materials (videos, documentation)
- Conduct webinar for registration staff
- Monitor adoption and usage metrics
- Provide ongoing support

### Data Migration
- No data migration required (new feature)
- Existing patients will have zero documents initially
- Staff can add documents retroactively as needed

---

## Open Questions _(to be resolved)_

1. **Document Retention Policy**: How long should deleted documents be retained in the system for audit purposes? (Recommendation: 7 years, configurable)
2. **Storage Location**: Which storage strategy should be default? (Database, filesystem, or cloud)
3. **OCR Integration**: Should Phase 2 OCR integration be prioritized, or is manual entry sufficient for initial release?
4. **Multi-page Documents**: Should front and back of ID card be separate documents or pages within single document?
5. **Internationalization**: What document type labels are needed for different countries beyond "National ID" and "Insurance Card"?
6. **Access Consent**: Do we need explicit patient consent tracking for document storage, or is implicit consent via patient registration sufficient?
7. **Document Expiration**: Should expiration date tracking be included in Phase 1, or deferred to Phase 3?
8. **Barcode/QR Code**: Should we support scanning barcodes/QR codes on ID cards to auto-populate patient identifiers?

---

## Dependencies _(optional)_

- **Carbon Design System (@carbon/react)**: Required for FileUploader, Modal, DataTable, Tile components
- **FHIR Server**: Required for DocumentReference resource storage and synchronization
- **Image Processing Library**: Required for thumbnail generation (e.g., ImageMagick, Sharp, Pillow)
- **Antivirus Scanner**: Required for malware scanning (e.g., ClamAV integration)
- **Storage Service**: Required for file storage (local filesystem or cloud provider SDK)
- **Encryption Library**: Required for at-rest encryption (e.g., OpenSSL, Java Cryptography Extension)

---

## Related Specifications

- **005-eqa-module**: EQA samples should NOT have patient ID documents (is_eqa_sample validation)
- **007-patient-merge**: Merged patients should display ID documents from both original patients in merge history
- **OGC-59**: Patient photos (related but separate feature - photo vs ID card scans)
- **OGC-50**: Diagnoses on lab orders (related patient information enhancement)
