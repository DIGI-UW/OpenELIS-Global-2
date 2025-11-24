# data-model.md — Patient ID Document Model

## Entities

### IDDocument
- Purpose: Logical container representing an identification document attached to a patient.
- Table: `id_document`
- Fields:
  - `id` (PK, uuid or serial)
  - `patient_id` (FK -> patient.id)
  - `document_type` (enum: NATIONAL_ID, INSURANCE_CARD, OTHER)
  - `description` (text, optional)
  - `document_group_id` (nullable, uuid) — groups related docs (front/back)
  - `created_at` (timestamp)
  - `created_by` (username/id)
  - `is_deleted` (boolean) — soft delete marker for the logical document
  - `deleted_at` (timestamp, nullable)

### DocumentVersion
- Purpose: Stores each uploaded binary/version of a document. Keeps history for replacements.
- Table: `document_version`
- Fields:
  - `id` (PK, uuid)
  - `id_document_id` (FK -> id_document.id)
  - `version_number` (int)
  - `filename` (string)
  - `content_type` (string / mime)
  - `size_bytes` (long)
  - `storage_key` (string) — location or reference (e.g., S3 key or filesystem hash)
  - `data_hash` (string) — optional, for duplicate detection
  - `ocr_status` (enum: NOT_RUN, PENDING, COMPLETED, FAILED)
  - `ocr_extracted` (jsonb, nullable) — holds OCR results if/when available
  - `created_at` (timestamp)
  - `created_by` (username)
  - `is_deleted` (boolean) — soft delete at version level
  - `deleted_at` (timestamp, nullable)

### AuditLogEntry
- Purpose: Record all actions (UPLOAD, VIEW, EDIT, DELETE) for documents
- Table: `document_audit`
- Fields:
  - `id` (PK, uuid)
  - `action` (enum: UPLOAD, VIEW, EDIT, DELETE, PURGE)
  - `user_id` (string)
  - `patient_id` (string)
  - `id_document_id` (nullable)
  - `document_version_id` (nullable)
  - `details` (jsonb) — e.g., {"oldType":"Other","newType":"Insurance Card"}
  - `file_name` (string, optional)
  - `file_size` (long, optional)
  - `ip_address` (string, optional)
  - `session_id` (string, optional)
  - `created_at` (timestamp)

## Indexes & Queries
- Index on `id_document.patient_id` for listing by patient
- Index on `document_version.storage_key` or `data_hash` for duplicate detection
- Index on `id_document.document_group_id` for grouping front/back

## Liquibase / Migration Notes
- Add new changeSet files under `src/main/resources/liquibase/{module}/` with new tables and rollback scripts.
- Always include rollback for schema changes.

## Storage Abstraction
- Implement `DocumentStorageService` interface with two implementations:
  - `S3DocumentStorageService` (production)
  - `LocalFileSystemDocumentStorageService` (dev/test)

API: `store(byte[] data, String contentType) -> storageKey`, `retrieve(storageKey) -> InputStream`, `delete(storageKey)`

## FHIR Mapping (DocumentReference)
- When creating DocumentReference for a document version:
  - `DocumentReference.type.text` = document_type (e.g., "National ID")
  - `DocumentReference.subject.reference` = `Patient/{patientId}`
  - `DocumentReference.date` = `documentVersion.created_at`
  - `DocumentReference.author` = practitioner/user reference
  - `DocumentReference.content[0].attachment` = { contentType, url (signed or public), title, creation }

