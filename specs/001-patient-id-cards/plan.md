# Implementation Plan: Patient ID Card Scanning and Management

**Branch**: `001-patient-id-cards` | **Date**: 2025-01-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-patient-id-cards/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature adds the ability to scan, upload, and manage patient identification documents (National ID cards, Insurance cards) attached to patient records. The system supports multiple upload methods (file picker, camera/scanner, clipboard), document versioning, soft-delete for audit compliance, thumbnail generation, malware scanning interface, rate limiting, and FHIR DocumentReference integration. The implementation follows OpenELIS's strict 5-layer architecture (Valueholder → DAO → Service → Controller → Form) with full internationalization support via React Intl and Carbon Design System UI components.

## Technical Context

**Language/Version**: Java 21 LTS (MANDATORY - OpenJDK/Temurin)  
**Primary Dependencies**: Spring Boot 3.x (Spring Framework 6.2.2), Hibernate 6.x, Jakarta EE 9 (jakarta.persistence.*), React 17, Carbon Design System v1.15, HAPI FHIR R4 (v6.6.2)  
**Storage**: PostgreSQL 14+ (via Hibernate/JPA), Liquibase 4.8.0 for schema migrations, abstracted document storage (LocalFileSystemStorageService for dev, S3-compatible for production)  
**Testing**: JUnit 4 (NOT JUnit 5), Mockito 2.21.0, Spring Test (@WebMvcTest, @DataJpaTest, @SpringBootTest), Jest + React Testing Library, Cypress 12.17.3 for E2E  
**Target Platform**: Web application (React frontend + Spring Boot REST API), deployed via Docker Compose, Nginx reverse proxy  
**Project Type**: Web application (frontend + backend)  
**Performance Goals**: 
- Document upload completes in <2 minutes for 90% of attempts (SC-001)
- Thumbnail generation within 5 seconds for 95% of image uploads (SC-003)
- Patient search results with document counts render without perceptible lag for patients with <=20 documents (SC-005)
- Rate limiting: 5 uploads per minute per patient record (FR-020)

**Constraints**: 
- Maximum file size: 10MB per document (FR-004)
- Supported formats: JPEG, PNG, PDF only (FR-003)
- Transactions MUST start in service layer (NOT controllers) - architectural requirement
- All data needed for response MUST be eagerly fetched within service transaction (prevents LazyInitializationException)
- NO hardcoded strings - React Intl required for all user-facing text (Constitution Principle VII)
- Carbon Design System ONLY - NO Bootstrap/Tailwind (Constitution Principle II)

**Scale/Scope**: 
- Supports multiple documents per patient (uniqueness: one active document per type per patient)
- Soft-deleted documents retained for 7 years (configurable)
- Document versioning for replacements (previous versions retained as soft-deleted)
- FHIR DocumentReference resources synchronized to consolidated FHIR server
- Role-based access control: View (registration staff, lab techs, managers), Upload/Edit (registration staff, managers), Delete (managers, admins only)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Configuration-Driven Variation ✅
- **Status**: PASS
- **Compliance**: Document retention period (7 years default) is configurable via `documents.retention.years` property. Document type enum (NATIONAL_ID, INSURANCE_CARD, OTHER) can be extended via configuration without code changes. Storage backend abstraction (LocalFileSystemStorageService vs S3) is deployment-configurable.
- **Justification**: No country-specific code branches required. All customization via configuration.

### II. Carbon Design System First ✅
- **Status**: PASS
- **Compliance**: All UI components MUST use Carbon Design System v1.15 exclusively. Document upload UI uses Carbon FileUploader, thumbnails use Carbon Image, modals use Carbon Modal, search results use Carbon DataTable. NO Bootstrap or Tailwind CSS.
- **Justification**: Ensures UI/UX consistency and WCAG 2.1 AA accessibility compliance.

### III. FHIR/IHE Standards Compliance ✅
- **Status**: PASS
- **Compliance**: Documents MUST be represented as FHIR DocumentReference resources with required metadata (contentType, creation date, title, URL/embedded data). Sync to consolidated FHIR server via FhirPersistanceService. All entities with external exposure include `fhir_uuid UUID` column.
- **Justification**: Required for interoperability with national health information exchanges.

### IV. Layered Architecture Pattern ✅
- **Status**: PASS
- **Compliance**: Strict 5-layer structure:
  1. **Valueholders**: `IDDocument`, `DocumentVersion`, `DocumentAudit` entities (extend `BaseObject<String>`)
  2. **DAOs**: `IDDocumentDAO`, `DocumentVersionDAO`, `DocumentAuditDAO` (extend `BaseDAOImpl<Entity, String>`, HQL only)
  3. **Services**: `DocumentService`, `DocumentValidationService`, `DocumentStorageService`, `MalwareScanner` (interface), `ThumbnailService` (@Service, @Transactional, transactions START here)
  4. **Controllers**: `IDDocumentRestController` (extend `BaseRestController`, NO @Transactional, delegate to services)
  5. **Forms/DTOs**: `UploadDocumentResponse`, `DocumentMetadataResponse`, `DocumentVersionResponse`
- **Justification**: Maintains architectural consistency and prevents LazyInitializationException by ensuring services eagerly fetch all data within transactions.

### V. Test-Driven Development ✅
- **Status**: PASS
- **Compliance**: TDD workflow ENCOURAGED for complex logic. Test pyramid:
  - Unit tests (JUnit 4 + Mockito): Business logic validation (75%)
  - ORM validation tests: Hibernate SessionFactory build validation (<5s, no database) (5%)
  - Integration tests (@SpringBootTest): Full stack with database (15%)
  - E2E tests (Cypress): User workflow validation (5%)
- **Coverage Goal**: >70% for new code (JaCoCo)
- **Justification**: Ensures code quality and catches bugs early.

### VI. Database Schema Management ✅
- **Status**: PASS
- **Compliance**: All schema changes via Liquibase changesets in `src/main/resources/liquibase/id-documents/`. NO direct DDL/DML in production. Rollback scripts provided for structural changes.
- **Justification**: Ensures version-controlled, reproducible database migrations.

### VII. Internationalization First ✅
- **Status**: PASS
- **Compliance**: ALL user-facing strings MUST use React Intl. Message files in `frontend/src/languages/{locale}.json`. Supported locales: en, fr, ar, es, hi, pt, sw. NO hardcoded English text.
- **Justification**: OpenELIS serves 30+ countries with multilingual requirements.

### VIII. Security & Compliance ✅
- **Status**: PASS
- **Compliance**: 
  - Role-based access control (FR-012): View/Upload/Edit/Delete permissions enforced
  - Audit trail: All actions (UPLOAD/VIEW/EDIT/DELETE) logged with user, timestamp, patient ID, document type, filename, file size, session/IP (FR-011)
  - Malware scanning interface: `MalwareScanner` with `NoopMalwareScanner` for dev, production implementations integrate ClamAV/cloud services (FR-013)
  - Rate limiting: Per-patient upload rate limit (5 uploads/minute) to prevent abuse (FR-020)
  - HTTPS enforced, encryption at rest (deployment detail)
- **Justification**: Meets SLIPTA and ISO 15189 requirements for healthcare data security.

**GATE RESULT**: ✅ **PASS** - All 8 constitution principles satisfied. No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/001-patient-id-cards/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command) - COMPLETE
├── data-model.md        # Phase 1 output (/speckit.plan command) - COMPLETE
├── quickstart.md        # Phase 1 output (/speckit.plan command) - COMPLETE
├── contracts/           # Phase 1 output (/speckit.plan command) - COMPLETE
│   └── openapi.yaml     # REST API contract
├── checklists/          # Quality validation checklists
│   └── requirements.md
├── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
└── spec.md              # Feature specification
```

### Source Code (repository root)

```text
# Backend (Java/Spring Boot)
src/main/java/org/openelisglobal/document/
├── valueholder/
│   ├── IDDocument.java              # JPA entity (extends BaseObject<String>)
│   ├── DocumentVersion.java         # JPA entity (extends BaseObject<String>)
│   └── DocumentAudit.java          # JPA entity (extends BaseObject<String>)
├── dao/
│   ├── IDDocumentDAO.java          # Interface
│   ├── DocumentVersionDAO.java     # Interface
│   └── DocumentAuditDAO.java       # Interface
├── daoimpl/
│   ├── IDDocumentDAOImpl.java      # Extends BaseDAOImpl<IDDocument, String>
│   ├── DocumentVersionDAOImpl.java # Extends BaseDAOImpl<DocumentVersion, String>
│   └── DocumentAuditDAOImpl.java   # Extends BaseDAOImpl<DocumentAudit, String>
├── service/
│   ├── DocumentService.java        # Interface
│   ├── DocumentServiceImpl.java   # @Service, @Transactional (transactions START here)
│   ├── DocumentValidationService.java
│   ├── DocumentValidationServiceImpl.java
│   ├── ThumbnailService.java
│   ├── ThumbnailServiceImpl.java
│   ├── MalwareScanner.java         # Interface (no-op for dev, ClamAV/cloud for prod)
│   ├── NoopMalwareScanner.java     # Development implementation
│   └── RateLimitService.java       # Per-patient upload rate limiting
├── storage/
│   ├── DocumentStorageService.java # Interface
│   ├── LocalFileSystemStorageService.java  # Dev implementation
│   └── S3DocumentStorageService.java      # Production implementation (future)
├── controller/
│   └── IDDocumentRestController.java  # Extends BaseRestController, NO @Transactional
├── dto/
│   ├── UploadDocumentResponse.java
│   ├── DocumentMetadataResponse.java
│   └── DocumentVersionResponse.java
└── fhir/
    ├── DocumentFhirTransform.java  # Entity ↔ FHIR DocumentReference transformation
    └── DocumentFhirTransformService.java

# Frontend (React/Carbon Design System)
frontend/src/
├── components/
│   └── patient/
│       └── documents/
│           ├── DocumentUploader.jsx        # Carbon FileUploader component
│           ├── DocumentThumbnail.jsx      # Carbon Image with metadata
│           ├── DocumentViewer.jsx         # Carbon Modal with zoom/PDF viewer
│           ├── DocumentList.jsx           # Carbon DataTable with document list
│           └── DocumentSearchBadge.jsx    # Badge indicator in search results
├── services/
│   └── documentService.js          # SWR-based API client
└── languages/
    └── {locale}.json               # React Intl messages (en, fr, ar, es, hi, pt, sw)

# Database Migrations (Liquibase)
src/main/resources/liquibase/id-documents/
├── 001-create-id-documents.xml     # Creates id_document, document_version, document_audit tables
├── 002-add-indexes.xml              # Performance indexes
└── 003-add-fhir-uuid-columns.xml    # FHIR UUID columns for DocumentReference sync

# Tests
src/test/java/org/openelisglobal/document/
├── valueholder/
│   └── HibernateMappingValidationTest.java  # ORM validation (<5s, no DB)
├── service/
│   ├── DocumentServiceTest.java             # Unit tests (JUnit 4 + Mockito)
│   ├── DocumentValidationServiceTest.java
│   └── ThumbnailServiceTest.java
├── dao/
│   ├── IDDocumentDAOTest.java              # @DataJpaTest
│   └── DocumentVersionDAOTest.java
├── controller/
│   └── IDDocumentRestControllerTest.java   # @WebMvcTest
└── integration/
    └── DocumentServiceIntegrationTest.java  # @SpringBootTest

frontend/src/components/patient/documents/
└── __tests__/
    ├── DocumentUploader.test.jsx           # Jest + React Testing Library
    ├── DocumentThumbnail.test.jsx
    └── DocumentViewer.test.jsx

frontend/cypress/e2e/
└── patientIdDocuments.cy.js               # Cypress E2E tests (individual file during dev)
```

**Structure Decision**: Web application structure (frontend + backend) following OpenELIS's existing architecture. Backend follows strict 5-layer pattern (Valueholder → DAO → Service → Controller → Form). Frontend uses React 17 + Carbon Design System v1.15 with React Intl for internationalization. All database changes via Liquibase. FHIR integration via existing FhirTransformService and FhirPersistanceService patterns.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations requiring justification. All 8 constitution principles are satisfied.

## Phase 0: Research Summary

**Status**: ✅ **COMPLETE** (see [research.md](./research.md))

Key decisions resolved:
1. **Document retention**: 7 years default (configurable via `documents.retention.years`)
2. **Multi-page documents**: Support both separate images (front/back) and multi-page PDFs, linked via `documentGroupId` when needed
3. **OCR extraction**: OUT OF SCOPE for initial delivery (future enhancement)

All "NEEDS CLARIFICATION" items from spec have been resolved.

## Phase 1: Design & Contracts

**Status**: ✅ **COMPLETE**

### Data Model
- **File**: [data-model.md](./data-model.md)
- **Entities**: `IDDocument`, `DocumentVersion`, `DocumentAudit`
- **Key Relationships**: 
  - `IDDocument` → `Patient` (many-to-one)
  - `IDDocument` → `DocumentVersion` (one-to-many, versioning)
  - Uniqueness constraint: `(patientId, documentType)` for active (non-deleted) documents
- **FHIR Mapping**: `DocumentReference` resources with required metadata (contentType, creation date, title, URL/embedded data)

### API Contracts
- **File**: [contracts/openapi.yaml](./contracts/openapi.yaml)
- **Endpoints**:
  - `POST /rest/patients/{patientId}/documents` - Upload document (multipart/form-data)
  - `GET /rest/patients/{patientId}/documents` - List documents for patient
  - `GET /rest/documents/{documentId}` - Get document metadata
  - `GET /rest/documents/{documentId}/versions/{versionId}/content` - Download document binary
  - `PUT /rest/documents/{documentId}` - Update document metadata
  - `DELETE /rest/documents/{documentId}` - Soft delete document
  - `GET /rest/documents/{documentId}/versions` - List document versions

### Quickstart Guide
- **File**: [quickstart.md](./quickstart.md)
- **Includes**: Prerequisites, build commands, API test examples, frontend dev setup

### Agent Context Update
- **Status**: PENDING (will be executed after plan completion)
- **Script**: `.specify/scripts/bash/update-agent-context.sh cursor-agent`
- **Purpose**: Update agent-specific context files with new technology/patterns from this plan

## Next Steps

1. **Phase 2: Task Breakdown** - Execute `/speckit.tasks` to generate dependency-ordered tasks.md
2. **Phase 3: Implementation** - Execute `/speckit.implement` to process tasks.md using TDD workflow
3. **Quality Validation** - Execute `/speckit.checklist` to generate custom quality validation checklist

## Implementation Notes

### Critical Implementation Requirements

1. **Transaction Management**: 
   - Services MUST be annotated with `@Transactional` (transactions START in service layer)
   - Controllers MUST NOT have `@Transactional` annotations
   - Services MUST eagerly fetch ALL data needed for response using `JOIN FETCH` to prevent LazyInitializationException

2. **Data Compilation Rule**:
   - Services compile complete data structures within transaction
   - Controllers receive complete DTOs, never traverse entity relationships

3. **File Upload Flow**:
   - Validate file type (JPEG/PNG/PDF) and size (<=10MB) before storage
   - Malware scanning via `MalwareScanner` interface (no-op for dev)
   - Rate limiting: 5 uploads/minute per patient
   - Thumbnail generation: async retry on failure, placeholder icon until complete
   - Storage backend failure: queue write operation, show "processing" status

4. **Document Uniqueness**:
   - Enforce one active document per type per patient
   - Automatic replacement: create new version, soft-delete previous version (no confirmation dialog)

5. **FHIR Synchronization**:
   - Create/update `DocumentReference` resources on document upload/edit
   - Sync to consolidated FHIR server via `FhirPersistanceService`
   - Include `fhir_uuid UUID` column in entities for FHIR mapping

6. **Internationalization**:
   - ALL user-facing strings via React Intl
   - Message keys in `frontend/src/languages/{locale}.json`
   - Document type labels translatable

7. **Testing Requirements**:
   - Unit tests: JUnit 4 (NOT JUnit 5) + Mockito
   - ORM validation: Hibernate SessionFactory build test (<5s, no database)
   - Integration tests: @SpringBootTest with full application context
   - E2E tests: Cypress (run individual files during dev, NOT full suite)
   - Coverage goal: >70% for new code

8. **Code Quality**:
   - Format code: `mvn spotless:apply` (backend), `npm run format` (frontend)
   - MUST run before every commit

## Dependencies

### Existing OpenELIS Components
- Patient entity and DAO/service infrastructure
- `BaseObject<String>`, `BaseDAOImpl<Entity, String>`, `BaseRestController` base classes
- `FhirTransformService`, `FhirPersistanceService` for FHIR integration
- Spring Security for authentication/authorization
- Audit logging infrastructure (extend for document-specific audit events)

### External Libraries
- **Backend**: 
  - Apache PDFBox or iText for PDF first-page preview generation
  - ImageIO or Thumbnailator for JPEG/PNG thumbnail generation
  - ClamAV Java client (optional, for production malware scanning)
- **Frontend**:
  - Carbon Design System v1.15 components (FileUploader, Image, Modal, DataTable)
  - React Intl for internationalization
  - SWR for data fetching
  - PDF.js for PDF viewing in browser

## Risk Mitigation

1. **Storage Backend Failure**: Queue write operations, show "processing" status, retry automatically when storage recovers
2. **Thumbnail Generation Failure**: Upload succeeds, thumbnail generation retried asynchronously, placeholder icon shown until complete
3. **Malware Scanning Failure**: Reject upload with error message, require retry after scan service recovers
4. **Rate Limit Exceeded**: Return HTTP 429 with clear error message indicating when retry is allowed
5. **Large Number of Documents**: Pagination/lazy loading for thumbnails to avoid UI slowdown
6. **Concurrent Edits**: Last-write-wins with audit trail capturing concurrent attempts

## Success Metrics

- **SC-001**: Users can attach at least one document during new patient registration in under 2 minutes in 90% of observed successful attempts
- **SC-002**: System accepts valid files (JPEG/PNG/PDF) and rejects invalid types or files >10MB with a clear error message 100% of the time
- **SC-003**: Thumbnail previews generate successfully for 95% of image uploads within 5 seconds of upload
- **SC-004**: For any Upload/Edit/Delete/View action on ID documents, an audit log entry exists with required metadata in 100% of sampled events
- **SC-005**: Patient search results display document counts and allow previewing documents for 95% of patients with <=20 documents without perceptible UI lag

---

**Plan Status**: ✅ **COMPLETE** - Ready for Phase 2 (Task Breakdown via `/speckit.tasks`)
