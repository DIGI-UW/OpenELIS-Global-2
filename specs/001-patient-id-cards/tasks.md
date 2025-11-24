# Tasks: Patient ID Card Scanning and Management

**Input**: Design documents from `/specs/001-patient-id-cards/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`)
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included as they are standard practice for OpenELIS features (TDD workflow ENCOURAGED per Constitution Principle V).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/org/openelisglobal/document/`
- **Frontend**: `frontend/src/components/patient/documents/`
- **Tests**: `src/test/java/org/openelisglobal/document/` and `frontend/src/components/patient/documents/__tests__/`
- **Migrations**: `src/main/resources/liquibase/id-documents/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 [P] Create Liquibase changeset directory structure at `src/main/resources/liquibase/id-documents/`
- [X] T002 [P] Add backend dependencies for document handling (Apache PDFBox, Thumbnailator, ImageIO) in `pom.xml` at project root
- [X] T003 [P] Add frontend dependencies for file upload and PDF viewing (`react-dropzone`, `pdfjs-dist`) to `frontend/package.json`
- [ ] T004 [P] Verify Java 21 is configured and Maven build passes with `mvn clean install -DskipTests -Dmaven.test.skip=true` (verification, no file change)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create Liquibase changeset for `id_document` table in `src/main/resources/liquibase/id-documents/001-create-id-documents.xml`
- [X] T006 Create Liquibase changeset for `document_version` table in `src/main/resources/liquibase/id-documents/001-create-id-documents.xml`
- [X] T007 Create Liquibase changeset for `document_audit` table in `src/main/resources/liquibase/id-documents/001-create-id-documents.xml`
- [X] T008 [P] Create JPA entity `IDDocument` extending `BaseObject<String>` in `src/main/java/org/openelisglobal/document/valueholder/IDDocument.java`
- [X] T009 [P] Create JPA entity `DocumentVersion` extending `BaseObject<String>` in `src/main/java/org/openelisglobal/document/valueholder/DocumentVersion.java`
- [X] T010 [P] Create JPA entity `DocumentAudit` extending `BaseObject<String>` in `src/main/java/org/openelisglobal/document/valueholder/DocumentAudit.java`
- [X] T011 [P] Create DAO interface `IDDocumentDAO` in `src/main/java/org/openelisglobal/document/dao/IDDocumentDAO.java`
- [X] T012 [P] Create DAO interface `DocumentVersionDAO` in `src/main/java/org/openelisglobal/document/dao/DocumentVersionDAO.java`
- [X] T013 [P] Create DAO interface `DocumentAuditDAO` in `src/main/java/org/openelisglobal/document/dao/DocumentAuditDAO.java`
- [X] T014 Create DAO implementation `IDDocumentDAOImpl` extending `BaseDAOImpl<IDDocument, String>` in `src/main/java/org/openelisglobal/document/daoimpl/IDDocumentDAOImpl.java`
- [X] T015 Create DAO implementation `DocumentVersionDAOImpl` extending `BaseDAOImpl<DocumentVersion, String>` in `src/main/java/org/openelisglobal/document/daoimpl/DocumentVersionDAOImpl.java`
- [X] T016 Create DAO implementation `DocumentAuditDAOImpl` extending `BaseDAOImpl<DocumentAudit, String>` in `src/main/java/org/openelisglobal/document/daoimpl/DocumentAuditDAOImpl.java`
- [X] T017 [P] Create storage abstraction interface `DocumentStorageService` in `src/main/java/org/openelisglobal/document/storage/DocumentStorageService.java`
- [X] T018 [P] Create local filesystem storage implementation `LocalFileSystemStorageService` in `src/main/java/org/openelisglobal/document/storage/LocalFileSystemStorageService.java`
- [X] T019 [P] Create service interface `DocumentService` in `src/main/java/org/openelisglobal/document/service/DocumentService.java`
- [X] T020 [P] Create validation service interface `DocumentValidationService` in `src/main/java/org/openelisglobal/document/service/DocumentValidationService.java`
- [X] T021 [P] Create malware scanner interface `MalwareScanner` in `src/main/java/org/openelisglobal/document/service/MalwareScanner.java`
- [X] T022 [P] Create no-op malware scanner implementation `NoopMalwareScanner` in `src/main/java/org/openelisglobal/document/service/NoopMalwareScanner.java`
- [X] T023 [P] Create thumbnail service interface `ThumbnailService` in `src/main/java/org/openelisglobal/document/service/ThumbnailService.java`
- [X] T024 [P] Create rate limit service interface `RateLimitService` in `src/main/java/org/openelisglobal/document/service/RateLimitService.java`
- [X] T025 Add configuration properties for document storage in `src/main/resources/application-documents.yml`
- [X] T026 [P] Create ORM validation test `HibernateMappingValidationTest` in `src/test/java/org/openelisglobal/document/valueholder/HibernateMappingValidationTest.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Attach ID during patient registration (Priority: P1) 🎯 MVP

**Goal**: Enable scanning/uploading of one or more ID documents during new patient registration and persist them with metadata and thumbnails.

**Independent Test**: Create a new patient, use the Add Document flow to upload two documents (National ID, Insurance Card). Verify thumbnails appear, documents saved and visible in patient summary and in FHIR resources.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T027 [P] [US1] Create unit test for `DocumentService.upload()` in `src/test/java/org/openelisglobal/document/service/DocumentServiceTest.java`
- [ ] T028 [P] [US1] Create unit test for `DocumentValidationService` in `src/test/java/org/openelisglobal/document/service/DocumentValidationServiceTest.java`
- [ ] T029 [P] [US1] Create controller test for upload endpoint in `src/test/java/org/openelisglobal/document/controller/IDDocumentRestControllerTest.java`
- [ ] T030 [P] [US1] Create DAO test for `IDDocumentDAO` in `src/test/java/org/openelisglobal/document/dao/IDDocumentDAOTest.java`
- [ ] T031 [P] [US1] Create DAO test for `DocumentVersionDAO` in `src/test/java/org/openelisglobal/document/dao/DocumentVersionDAOTest.java`
- [ ] T032 [P] [US1] Create integration test for upload workflow in `src/test/java/org/openelisglobal/document/integration/DocumentServiceIntegrationTest.java`
- [ ] T033 [P] [US1] Create frontend unit test for `DocumentUploader` component in `frontend/src/components/patient/documents/__tests__/DocumentUploader.test.jsx`

### Implementation for User Story 1

- [X] T034 [P] [US1] Create DTO `UploadDocumentResponse` in `src/main/java/org/openelisglobal/document/dto/UploadDocumentResponse.java`
- [X] T035 [P] [US1] Create DTO `DocumentMetadataResponse` in `src/main/java/org/openelisglobal/document/dto/DocumentMetadataResponse.java`
- [X] T036 [US1] Implement `DocumentValidationServiceImpl` with MIME type and file size validation in `src/main/java/org/openelisglobal/document/service/DocumentValidationServiceImpl.java`
- [X] T037 [US1] Implement `ThumbnailServiceImpl` for JPEG/PNG thumbnail generation in `src/main/java/org/openelisglobal/document/service/ThumbnailServiceImpl.java`
- [X] T038 [US1] Implement `RateLimitServiceImpl` for per-patient upload rate limiting in `src/main/java/org/openelisglobal/document/service/RateLimitServiceImpl.java`
- [X] T039 [US1] Implement `DocumentServiceImpl` with upload method (wires DAOs, storage, validation, malware scanner, rate limiting) in `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [X] T040 [US1] Implement `IDDocumentRestController` with `POST /rest/patients/{patientId}/documents` endpoint in `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [X] T041 [US1] Implement `GET /rest/patients/{patientId}/documents` endpoint in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [X] T042 [US1] Implement `GET /rest/documents/{documentId}/versions/{versionId}/content` endpoint for document download in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [X] T043 [US1] Add audit logging for upload actions in `DocumentServiceImpl` at `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [X] T044 [US1] Create frontend service `documentService.js` using SWR for API calls in `frontend/src/services/documentService.js`
- [X] T045 [US1] Create React component `DocumentUploader` using Carbon FileUploader in `frontend/src/components/patient/documents/DocumentUploader.jsx`
- [X] T046 [US1] Create React component `DocumentThumbnail` using Carbon Image in `frontend/src/components/patient/documents/DocumentThumbnail.jsx`
- [X] T047 [US1] Create React component `DocumentList` using Carbon DataTable in `frontend/src/components/patient/documents/DocumentList.jsx`
- [X] T048 [US1] Integrate `DocumentUploader` and `DocumentList` into patient registration form in `frontend/src/components/patient/CreatePatientForm.js`
- [X] T049 [US1] Add React Intl message keys for document upload UI in `frontend/src/languages/en.json`
- [X] T050 [US1] Add React Intl message keys for French translation in `frontend/src/languages/fr.json`
- [ ] T051 [US1] Implement FHIR DocumentReference transformation in `src/main/java/org/openelisglobal/document/fhir/DocumentFhirTransform.java`
- [ ] T052 [US1] Integrate FHIR sync on document upload in `DocumentServiceImpl` at `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T053 [US1] Create Liquibase changeset to add `fhir_uuid` columns to document tables in `src/main/resources/liquibase/id-documents/003-add-fhir-uuid-columns.xml`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Manage documents from patient search (Priority: P2)

**Goal**: Display document count indicator in patient search results and enable quick preview from search.

**Independent Test**: Search for a patient with documents; verify "Documents" column shows a count badge; click badge to open a modal preview and view metadata.

### Tests for User Story 2

- [ ] T054 [P] [US2] Create unit test for document count aggregation in `src/test/java/org/openelisglobal/document/service/DocumentCountServiceTest.java`
- [ ] T055 [P] [US2] Create controller test for document preview endpoint in `src/test/java/org/openelisglobal/document/controller/IDDocumentRestControllerTest.java`
- [ ] T056 [P] [US2] Create frontend unit test for `DocumentSearchBadge` component in `frontend/src/components/patient/documents/__tests__/DocumentSearchBadge.test.jsx`
- [ ] T057 [P] [US2] Create frontend unit test for `DocumentViewer` component in `frontend/src/components/patient/documents/__tests__/DocumentViewer.test.jsx`
- [ ] T058 [P] [US2] Create Cypress E2E test for search preview flow in `frontend/cypress/e2e/patientIdDocuments.cy.js`

### Implementation for User Story 2

- [ ] T059 [US2] Add method to `DocumentService` to get document counts for multiple patient IDs in `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T060 [US2] Implement `GET /rest/documents/patients/counts` endpoint for batch document counts in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [ ] T061 [US2] Modify patient search service to include document counts in `src/main/java/org/openelisglobal/patient/service/PatientServiceImpl.java`
- [ ] T062 [US2] Create React component `DocumentSearchBadge` using Carbon Badge in `frontend/src/components/patient/documents/DocumentSearchBadge.jsx`
- [ ] T063 [US2] Create React component `DocumentViewer` using Carbon Modal with zoom controls in `frontend/src/components/patient/documents/DocumentViewer.jsx`
- [ ] T064 [US2] Integrate `DocumentSearchBadge` into patient search results in `frontend/src/components/search/PatientSearchResults.jsx`
- [ ] T065 [US2] Add click handler to open `DocumentViewer` modal from search badge in `frontend/src/components/search/PatientSearchResults.jsx`
- [ ] T066 [US2] Add React Intl message keys for document search UI in `frontend/src/languages/en.json`
- [ ] T067 [US2] Add React Intl message keys for French translation in `frontend/src/languages/fr.json`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Edit and version documents (Priority: P3)

**Goal**: Allow editing document metadata, replacing files to create versions, and show version history with soft-delete/restore.

**Independent Test**: Open a patient, edit a document's type and replace the file; verify audit entries and that previous version is retained as a soft-deleted/archive entry.

### Tests for User Story 3

- [ ] T068 [P] [US3] Create unit test for document versioning logic in `src/test/java/org/openelisglobal/document/service/DocumentVersioningTest.java`
- [ ] T069 [P] [US3] Create unit test for soft delete and restore in `src/test/java/org/openelisglobal/document/service/DocumentSoftDeleteTest.java`
- [ ] T070 [P] [US3] Create controller test for metadata update endpoint in `src/test/java/org/openelisglobal/document/controller/IDDocumentRestControllerTest.java`
- [ ] T071 [P] [US3] Create controller test for file replace endpoint in `src/test/java/org/openelisglobal/document/controller/IDDocumentRestControllerTest.java`
- [ ] T072 [P] [US3] Create frontend unit test for `DocumentEditor` component in `frontend/src/components/patient/documents/__tests__/DocumentEditor.test.jsx`
- [ ] T073 [P] [US3] Create frontend unit test for `DocumentVersionHistory` component in `frontend/src/components/patient/documents/__tests__/DocumentVersionHistory.test.jsx`

### Implementation for User Story 3

- [ ] T074 [US3] Create DTO `DocumentVersionResponse` in `src/main/java/org/openelisglobal/document/dto/DocumentVersionResponse.java`
- [ ] T075 [US3] Add method to `DocumentService` to update document metadata in `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T076 [US3] Add method to `DocumentService` to replace document file (create new version) in `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T077 [US3] Add method to `DocumentService` to soft delete document in `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T078 [US3] Add method to `DocumentService` to list document versions in `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T079 [US3] Implement `PUT /rest/patients/{patientId}/documents/{documentId}/metadata` endpoint in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [ ] T080 [US3] Implement `POST /rest/patients/{patientId}/documents/{documentId}/replace` endpoint in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [ ] T081 [US3] Implement `DELETE /rest/patients/{patientId}/documents/{documentId}` endpoint in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [ ] T082 [US3] Implement `GET /rest/documents/{documentId}/versions` endpoint in `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [ ] T083 [US3] Add audit logging for edit, replace, and delete actions in `DocumentServiceImpl` at `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T084 [US3] Create React component `DocumentEditor` using Carbon Form components in `frontend/src/components/patient/documents/DocumentEditor.jsx`
- [ ] T085 [US3] Create React component `DocumentVersionHistory` using Carbon DataTable in `frontend/src/components/patient/documents/DocumentVersionHistory.jsx`
- [ ] T086 [US3] Integrate `DocumentEditor` and `DocumentVersionHistory` into patient detail view in `frontend/src/components/patient/PatientDetail.jsx`
- [ ] T087 [US3] Add React Intl message keys for document editing UI in `frontend/src/languages/en.json`
- [ ] T088 [US3] Add React Intl message keys for French translation in `frontend/src/languages/fr.json`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T089 [P] Implement RBAC checks in service layer for document actions in `src/main/java/org/openelisglobal/document/service/DocumentSecurityService.java`
- [ ] T090 [P] Add method-level security annotations to `IDDocumentRestController` at `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java`
- [ ] T091 [P] Implement async thumbnail generation with retry logic in `ThumbnailServiceImpl` at `src/main/java/org/openelisglobal/document/service/ThumbnailServiceImpl.java`
- [ ] T092 [P] Implement storage write queue for handling storage backend failures in `src/main/java/org/openelisglobal/document/storage/StorageWriteQueue.java`
- [ ] T093 [P] Add pagination support for document lists in `DocumentServiceImpl` at `src/main/java/org/openelisglobal/document/service/DocumentServiceImpl.java`
- [ ] T094 [P] Add lazy loading for thumbnails in `DocumentList` component at `frontend/src/components/patient/documents/DocumentList.jsx`
- [ ] T095 [P] Add accessibility attributes (ARIA labels) to all document components in `frontend/src/components/patient/documents/`
- [ ] T096 [P] Add error handling and user feedback for upload failures in `DocumentUploader` at `frontend/src/components/patient/documents/DocumentUploader.jsx`
- [ ] T097 [P] Add loading states and progress indicators for uploads in `DocumentUploader` at `frontend/src/components/patient/documents/DocumentUploader.jsx`
- [ ] T098 [P] Create Liquibase changeset for performance indexes in `src/main/resources/liquibase/id-documents/002-add-indexes.xml`
- [ ] T099 [P] Add comprehensive E2E Cypress test for full user workflow in `frontend/cypress/e2e/patientIdDocuments.cy.js`
- [ ] T100 [P] Update documentation in `docs/features/patient-id-cards.md`
- [ ] T101 [P] Run code formatting: `mvn spotless:apply` and `cd frontend && npm run format`
- [ ] T102 [P] Run quickstart.md validation to ensure all steps work correctly

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Depends on US1 for document storage/retrieval infrastructure
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on US1 for document storage/retrieval infrastructure

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: T027 [P] [US1] Create unit test for DocumentService.upload()
Task: T028 [P] [US1] Create unit test for DocumentValidationService
Task: T029 [P] [US1] Create controller test for upload endpoint
Task: T030 [P] [US1] Create DAO test for IDDocumentDAO
Task: T031 [P] [US1] Create DAO test for DocumentVersionDAO
Task: T032 [P] [US1] Create integration test for upload workflow
Task: T033 [P] [US1] Create frontend unit test for DocumentUploader component

# Launch all DTOs for User Story 1 together:
Task: T034 [P] [US1] Create DTO UploadDocumentResponse
Task: T035 [P] [US1] Create DTO DocumentMetadataResponse

# Launch frontend components in parallel:
Task: T045 [US1] Create React component DocumentUploader
Task: T046 [US1] Create React component DocumentThumbnail
Task: T047 [US1] Create React component DocumentList
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2 (can start after US1 core is done)
   - Developer C: User Story 3 (can start after US1 core is done)
3. Stories complete and integrate independently

---

## Task Counts & Summary

- **Total tasks**: 102
- **Tasks per phase**:
  - Setup (Phase 1): 4
  - Foundational (Phase 2): 22
  - User Story 1 (P1): 27 (7 tests + 20 implementation)
  - User Story 2 (P2): 14 (5 tests + 9 implementation)
  - User Story 3 (P3): 21 (6 tests + 15 implementation)
  - Polish & Cross-cutting (Phase 6): 14

**MVP suggestion**: Implement Phase 1 + Phase 2 and User Story 1 (T001-T053) as the initial deliverable.

**Format validation**: All tasks above follow checklist format `- [ ] T### [P]? [US#]? Description with file path` where applicable.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- All user-facing strings MUST use React Intl (Constitution Principle VII)
- All UI components MUST use Carbon Design System (Constitution Principle II)
- Transactions MUST start in service layer, NOT controllers (Constitution Principle IV)
