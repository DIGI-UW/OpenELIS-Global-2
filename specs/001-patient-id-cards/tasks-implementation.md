---
description: "Granular implementation tasks for Patient ID Card Scanning and Management (file-level)
This complements `tasks.md` with concrete, small tasks developers can pick up directly."
---

# Implementation Tasks (Granular)

These tasks break Phase 2 (Foundational) and Phase 3 (US1) into small, file-level steps so work can be picked up by individual developers.

Notes:
- All tasks include exact file paths where code should be added or modified.
- Task IDs continue from `T051` upwards to avoid colliding with `tasks.md`.

## Foundational / Backend (Phase 2) - Entities, DAOs, Storage

- [ ] T051 [P] Create Liquibase changelog `src/main/resources/liquibase/id-documents/001-create-id-documents.xml` with tables `id_document` and `document_version` and basic indexes
- [ ] T052 Create JPA entity `src/main/java/org/openelisglobal/document/valueholder/IDDocument.java` with fields: `id, patientId, documentType, description, createdAt, createdBy, isDeleted, currentVersionId, fhirUuid`
- [ ] T053 Create JPA entity `src/main/java/org/openelisglobal/document/valueholder/DocumentVersion.java` with fields: `versionId, idDocumentId, filename, contentType, sizeBytes, storagePath, thumbnailPath, createdAt, createdBy`
- [ ] T054 Create DAO interface `src/main/java/org/openelisglobal/document/dao/IDDocumentDAO.java` with methods: `save, getById, listByPatientId, softDelete, restore`
- [ ] T055 Create DAO interface `src/main/java/org/openelisglobal/document/dao/DocumentVersionDAO.java` with methods: `saveVersion, listByDocumentId, getLatestVersion`
- [ ] T056 [P] Implement simple DAO implementations (skeleton) in `src/main/java/org/openelisglobal/document/dao/impl/` using Spring Data JPA or BaseDAOImpl patterns
- [ ] T057 Create storage interface `src/main/java/org/openelisglobal/document/storage/DocumentStorageService.java` with methods: `store(InputStream, path) -> storagePath`, `retrieve(storagePath) -> InputStream`, `delete(storagePath)`
- [ ] T058 Implement local filesystem adapter `src/main/java/org/openelisglobal/document/storage/LocalFileSystemStorageService.java` that writes to `document.storage.path` config
- [ ] T059 Add S3 adapter stub `src/main/java/org/openelisglobal/document/storage/S3StorageService.java` with TODOs for later implementation
- [ ] T060 Add config properties in `src/main/resources/application.yml` (or application-*.yml): `document.storage.path`, `document.max-size-bytes: 10485760`, `document.thumbnail.width`

## Foundational / Backend (Phase 2) - Services & Utilities

- [ ] T061 Create service interface `src/main/java/org/openelisglobal/document/service/DocumentService.java` declaring core methods: `upload`, `getMetadata`, `listForPatient`, `replaceVersion`, `softDelete`, `restore`, `listVersions`
- [ ] T062 Create service implementation skeleton `src/main/java/org/openelisglobal/document/service/impl/DocumentServiceImpl.java` with method stubs and injected DAOs/storage
- [ ] T063 Create `src/main/java/org/openelisglobal/document/audit/DocumentAuditService.java` with `logUpload`, `logView`, `logEdit`, `logDelete` methods that write to existing audit table/DAO
- [ ] T064 Add `src/main/java/org/openelisglobal/document/util/ThumbnailGenerator.java` with a `generateThumbnail(InputStream, targetPath)` method using ImageIO (image fallback for PDFs can be TODO)
- [ ] T065 Create malware scanner interface `src/main/java/org/openelisglobal/document/security/MalwareScanner.java` and dev no-op impl `NoopMalwareScanner` in `src/main/java/org/openelisglobal/document/security/NoopMalwareScanner.java`

## API Layer (Controller + DTOs) - US1

- [ ] T066 Create REST controller `src/main/java/org/openelisglobal/document/controller/IDDocumentRestController.java` with endpoints:
  - `POST /rest/documents/patient/{patientId}` (multipart upload)
  - `GET /rest/documents/patient/{patientId}` (list metadata)
  - `GET /rest/documents/{id}/download` (download latest)
  - `GET /rest/documents/{id}/versions` (list versions)
  - `DELETE /rest/documents/{id}` (soft delete)
- [ ] T067 Add request/response DTOs in `src/main/java/org/openelisglobal/document/dto/`:
  - `UploadDocumentResponse.java`, `DocumentMetadataResponse.java`, `DocumentVersionResponse.java`, `ErrorResponse.java`
- [ ] T068 Implement multipart validation helper `src/main/java/org/openelisglobal/document/api/UploadHandler.java` to validate MIME types and file size and call `MalwareScanner`
- [ ] T069 Wire controller to `DocumentService` and return appropriate HTTP status codes and JSON responses

## Backend Tests (Unit + Integration)

- [ ] T070 Add unit test for `DocumentServiceImpl.upload` in `src/test/java/org/openelisglobal/document/service/DocumentServiceUploadTest.java` (use Mockito)
- [ ] T071 Add DAO integration test (in-memory DB) for `IDDocumentDAO` in `src/test/java/org/openelisglobal/document/dao/IDDocumentDaoTest.java`
- [ ] T072 Add controller slice test `src/test/java/org/openelisglobal/document/controller/IDDocumentRestControllerTest.java` using `@WebMvcTest` and `MockMvc` for upload endpoint

## Frontend (US1) - Components & Tests

- [ ] T073 Create `frontend/src/components/Patient/IdDocuments/IdDocumentUploader.jsx` (React, Carbon) with drag-and-drop and camera capture UI using `react-dropzone`
- [ ] T074 Create `frontend/src/components/Patient/IdDocuments/IdDocumentList.jsx` to display thumbnails and action buttons (View/Edit/Delete)
- [ ] T075 Integrate uploader into `frontend/src/components/Patient/PatientForm/PatientForm.jsx` - add `Identification Documents` section and hook to backend
- [ ] T076 Create i18n keys in `frontend/src/languages/en.json` for labels/messages used by uploader and list
- [ ] T077 Add Jest unit tests for `IdDocumentUploader` in `frontend/src/__tests__/IdDocumentUploader.test.jsx`
- [ ] T078 Add Cypress E2E spec `frontend/cypress/e2e/uploadDocument.cy.js` covering upload flow (happy path + validation error)

## US1 Polish & CI

- [ ] T079 Add logging and metrics in `DocumentServiceImpl` for upload durations and thumbnail generation times
- [ ] T080 Ensure `mvn spotless:apply` formats Java changes and `frontend` runs `npm run format` before commit
- [ ] T081 Update `README.md` or `specs/001-patient-id-cards/quickstart.md` with curl examples for `POST /rest/documents/patient/{patientId}`

## Dependencies & Ordering

- Tasks T051-T060 (DB + entities + storage config) should be done first.
- Tasks T061-T069 (services + controllers) depend on entities and storage.
- Frontend tasks T073-T078 can start in parallel once API contract (T066-T067) is available (stubbed responses accepted for initial UI work).
- Tests (T070-T072, T077-T078) should be added as tasks are implemented and should run in CI.

## Estimated task count (new granular list): 31

Pick tasks to start with or tell me to create skeleton files for T051-T058 and run a local build to validate. I will commit each small change and update the todo list as I progress.
