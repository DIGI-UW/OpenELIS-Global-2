# Tasks: Storage Location Management & Configuration

**Feature**: OGC-68-storage-location-crud  
**Jira**: [OGC-68](https://uwdigi.atlassian.net/browse/OGC-68)  
**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)  
**Date Generated**: 2025-12-11

## User Story Mapping

| User Story | Priority | Description                                                | Milestones                  |
| ---------- | -------- | ---------------------------------------------------------- | --------------------------- |
| US1        | P1       | Manage Storage Locations (CRUD for Room/Device/Shelf/Rack) | M1 (backend), M2 (frontend) |
| US2        | P2       | Configure Device Connectivity (IP/Port/Protocol fields)    | M1 (backend), M2 (frontend) |

## Milestone Dependency Graph

```mermaid
graph LR
    M1[M1: Backend<br/>Entity/Service/API<br/>20 tasks] --> DONE[Feature Complete]
    M2[M2: Frontend [P]<br/>CRUD Modals/i18n<br/>22 tasks] --> DONE

    style M1 fill:#e1f5fe
    style M2 fill:#f3e5f5
    style DONE fill:#c8e6c9
```

**Parallel Execution**: M1 and M2 can be developed simultaneously (no
dependencies between them).

---

## Milestone 1: Backend (Sequential)

**Branch**: `feat/OGC-68-storage-location-crud/m1-backend`  
**Target**: `develop`  
**User Stories**: US1 (backend), US2 (backend)  
**Verification**: Unit tests + Integration tests MUST pass  
**Task Count**: 20 tasks

### M1.1 - Branch Setup

- [ ] T001 Create milestone branch:
      `git checkout -b feat/OGC-68-storage-location-crud/m1-backend` from
      `develop`

### M1.2 - Tests First (TDD) [US1, US2]

> **TDD Rule**: Write failing tests BEFORE implementation

- [ ] T002 [P] [US1] Write unit test for `canDeleteRoom()` validation in
      `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceTest.java`
- [ ] T003 [P] [US1] Write unit test for `canDeleteDevice()` validation in
      `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceTest.java`
- [ ] T004 [P] [US1] Write unit test for `canDeleteShelf()` validation in
      `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceTest.java`
- [ ] T005 [P] [US1] Write unit test for `canDeleteRack()` validation in
      `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceTest.java`
- [ ] T006 [P] [US2] Write unit test for StorageDevice connectivity field
      persistence in
      `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceTest.java`
- [ ] T007 [P] [US1] Write controller test for DELETE `/rest/storage/rooms/{id}`
      endpoint in
      `src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java`
- [ ] T008 [P] [US2] Write controller test for POST `/rest/storage/devices` with
      connectivity fields in
      `src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java`
- [ ] T008a [P] [US1] Write controller test verifying non-admin users receive
      403 Forbidden for CRUD operations in
      `src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java`

**Checkpoint**: All tests should FAIL (Red phase)

### M1.3 - Database Schema [US2]

- [ ] T009 [US2] Create Liquibase changeset for device connectivity columns in
      `src/main/resources/liquibase/3.3.x.x/023-storage-device-connectivity.xml`
- [ ] T010 [US2] Add changeset include to main Liquibase changelog in
      `src/main/resources/liquibase/liquibase-changelog.xml`

### M1.4 - Entity Layer [US2]

- [ ] T011 [US2] Add `ipAddress`, `port`, `communicationProtocol` fields to
      `src/main/java/org/openelisglobal/storage/valueholder/StorageDevice.java`
- [ ] T012 [US2] Update `StorageDeviceForm.java` DTO with new connectivity
      fields in
      `src/main/java/org/openelisglobal/storage/form/StorageDeviceForm.java`

### M1.5 - Service Layer [US1]

- [ ] T013 [US1] Create `DeletionValidationResult` class in
      `src/main/java/org/openelisglobal/storage/service/DeletionValidationResult.java`
- [ ] T014 [US1] Implement `canDeleteRoom()` method in
      `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java`
- [ ] T015 [US1] Implement `canDeleteDevice()` method in
      `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java`
- [ ] T016 [US1] Implement `canDeleteShelf()` method in
      `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java`
- [ ] T017 [US1] Implement `canDeleteRack()` method in
      `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java`
- [ ] T017a [US1] Implement uniqueness validation for Location Names within
      parent scope in
      `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java`

### M1.6 - Controller Layer [US1]

- [ ] T018 [US1] Add DELETE `/rest/storage/rooms/{id}` endpoint with validation
      in
      `src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java`
- [ ] T019 [US1] Add DELETE `/rest/storage/devices/{id}` endpoint with
      validation in
      `src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java`
- [ ] T020 [US1] Add DELETE `/rest/storage/shelves/{id}` endpoint with
      validation in
      `src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java`
- [ ] T021 [US1] Add DELETE `/rest/storage/racks/{id}` endpoint with validation
      in
      `src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java`

### M1.7 - FHIR Integration [US2]

- [ ] T022 [US2] Add FHIR extension constants for connectivity fields in
      `src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java`
- [ ] T023 [US2] Update `transformToFhirLocation()` to include connectivity
      extensions in
      `src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java`

### M1.8 - Verification & PR

- [ ] T024 Run `mvn spotless:apply` and verify all tests pass with `mvn test`
- [ ] T025 Create PR: `feat/OGC-68-storage-location-crud/m1-backend` → `develop`
      with title "feat(OGC-68): Add storage location CRUD backend and device
      connectivity"

**Milestone 1 Checkpoint**: All unit + integration tests pass, DELETE endpoints
work, connectivity fields persist

---

## Milestone 2: Frontend [P] (Parallel)

**Branch**: `feat/OGC-68-storage-location-crud/m2-frontend`  
**Target**: `develop`  
**User Stories**: US1 (frontend), US2 (frontend)  
**Verification**: Jest tests + Cypress E2E tests MUST pass  
**Task Count**: 22 tasks

### M2.1 - Branch Setup

- [ ] T026 Create milestone branch:
      `git checkout -b feat/OGC-68-storage-location-crud/m2-frontend` from
      `develop`

### M2.2 - Internationalization [US1, US2]

- [ ] T027 [P] [US1] Add CRUD action strings to `frontend/src/languages/en.json`
      (add.room, edit.room, delete.room, etc.)
- [ ] T028 [P] [US1] Add CRUD action strings to `frontend/src/languages/fr.json`
- [ ] T029 [P] [US2] Add device connectivity field labels to
      `frontend/src/languages/en.json` (ipAddress, port, communicationProtocol)
- [ ] T030 [P] [US2] Add device connectivity field labels to
      `frontend/src/languages/fr.json`
- [ ] T031 [P] [US1] Add deletion error messages to
      `frontend/src/languages/en.json` (cannotDelete.hasChildren,
      cannotDelete.hasAssignments)
- [ ] T032 [P] [US1] Add deletion error messages to
      `frontend/src/languages/fr.json`

### M2.3 - Tests First (TDD) [US1, US2]

> **TDD Rule**: Write failing tests BEFORE implementation

- [ ] T033 [P] [US1] Write Jest test for StorageLocationModal component in
      `frontend/src/components/storage/StorageLocationModal.test.jsx`
- [ ] T034 [P] [US1] Write Jest test for DeleteLocationModal component in
      `frontend/src/components/storage/DeleteLocationModal.test.jsx`
- [ ] T035 [P] [US2] Write Jest test for device connectivity form validation in
      `frontend/src/components/storage/StorageLocationModal.test.jsx`

**Checkpoint**: All Jest tests should FAIL (Red phase)

### M2.4 - Modal Components [US1, US2]

- [ ] T036 [US1] Create shared `StorageLocationModal.jsx` component with dynamic
      fields by entity type in
      `frontend/src/components/storage/StorageLocationModal.jsx`
- [ ] T037 [US2] Add IP Address, Port, Communication Protocol fields to device
      form in `frontend/src/components/storage/StorageLocationModal.jsx`
- [ ] T038 [US2] Implement IP address validation (IPv4/IPv6 regex) in
      `frontend/src/components/storage/StorageLocationModal.jsx`
- [ ] T039 [US2] Implement port validation (1-65535) in
      `frontend/src/components/storage/StorageLocationModal.jsx`
- [ ] T039a [US1] Implement uniqueness validation for Location Names within
      parent scope (display error from backend 409 response) in
      `frontend/src/components/storage/StorageLocationModal.jsx`
- [ ] T040 [US1] Create `DeleteLocationModal.jsx` confirmation component in
      `frontend/src/components/storage/DeleteLocationModal.jsx`

### M2.5 - Tab Updates [US1]

- [ ] T041 [US1] Add OverflowMenu with Edit/Delete actions to
      `frontend/src/components/storage/StorageRoomsTab.jsx`
- [ ] T042 [US1] Add "Add Room" button and modal integration to
      `frontend/src/components/storage/StorageRoomsTab.jsx`
- [ ] T043 [US1] Add OverflowMenu with Edit/Delete actions to
      `frontend/src/components/storage/StorageDevicesTab.jsx`
- [ ] T044 [US1] Add "Add Device" button and modal integration to
      `frontend/src/components/storage/StorageDevicesTab.jsx`
- [ ] T045 [US1] Add OverflowMenu with Edit/Delete actions to
      `frontend/src/components/storage/StorageShelvesTab.jsx`
- [ ] T046 [US1] Add "Add Shelf" button and modal integration to
      `frontend/src/components/storage/StorageShelvesTab.jsx`
- [ ] T047 [US1] Add OverflowMenu with Edit/Delete actions to
      `frontend/src/components/storage/StorageRacksTab.jsx`
- [ ] T048 [US1] Add "Add Rack" button and modal integration to
      `frontend/src/components/storage/StorageRacksTab.jsx`

### M2.6 - E2E Tests [US1, US2]

- [ ] T049 [US1] Create Cypress E2E test file
      `frontend/cypress/e2e/storageLocationCrud.cy.js`
- [ ] T050 [US1] Write E2E test: Create new Room via modal in
      `frontend/cypress/e2e/storageLocationCrud.cy.js`
- [ ] T051 [US1] Write E2E test: Edit existing Device in
      `frontend/cypress/e2e/storageLocationCrud.cy.js`
- [ ] T052 [US1] Write E2E test: Delete location blocked when children exist in
      `frontend/cypress/e2e/storageLocationCrud.cy.js`
- [ ] T053 [US2] Write E2E test: Create Device with IP/Port configuration in
      `frontend/cypress/e2e/storageLocationCrud.cy.js`

### M2.7 - Verification & PR

- [ ] T054 Run `npm run format` and verify all Jest tests pass with `npm test`
- [ ] T055 Run individual Cypress test:
      `npm run cy:run -- --spec "cypress/e2e/storageLocationCrud.cy.js"`
- [ ] T056 Create PR: `feat/OGC-68-storage-location-crud/m2-frontend` →
      `develop` with title "feat(OGC-68): Add storage location CRUD frontend
      modals"

**Milestone 2 Checkpoint**: All Jest + Cypress tests pass, CRUD modals work for
all entity types

---

## Dependencies & Execution Order

### Milestone Dependencies

```
M1 (Backend) ──┬──► develop (can merge independently)
               │
M2 (Frontend) ─┴──► develop (can merge independently)
```

**Key Points**:

- M1 and M2 are fully parallel - no dependencies between them
- Both milestones target `develop` directly
- Order of merging doesn't matter
- E2E tests (T049-T053) require backend to be deployed to test environment

### Within Milestone 1 (Backend)

```
T001 (branch) → T002-T008 (tests) → T009-T010 (schema) → T011-T012 (entity)
    → T013-T017 (service) → T018-T021 (controller) → T022-T023 (FHIR) → T024-T025 (PR)
```

### Within Milestone 2 (Frontend)

```
T026 (branch) → T027-T032 (i18n, parallel) → T033-T035 (tests) → T036-T040 (modals)
    → T041-T048 (tabs) → T049-T053 (E2E) → T054-T056 (PR)
```

### Parallel Opportunities

**Within M1**:

- T002-T008: All test tasks can run in parallel (different test methods)
- T009-T010: Schema tasks are sequential

**Within M2**:

- T027-T032: All i18n tasks can run in parallel (different files/languages)
- T033-T035: All Jest test tasks can run in parallel
- T041-T048: Tab updates can run in parallel (different files)

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete M1 tasks T001-T021 (skip T022-T023 FHIR for MVP)
2. Complete M2 tasks T026-T048 (skip T049-T053 E2E for MVP)
3. **VALIDATE**: Test CRUD operations manually
4. Deploy/demo basic CRUD capability

### Full Feature (US1 + US2)

1. Complete ALL M1 tasks (including FHIR integration)
2. Complete ALL M2 tasks (including E2E tests)
3. Run full test suite: `mvn test && cd frontend && npm test && npm run cy:run`
4. Merge both PRs to `develop`

### Parallel Team Strategy

With 2 developers:

- **Developer A**: M1 (Backend) - T001-T025
- **Developer B**: M2 (Frontend) - T026-T056

Both can work simultaneously and merge independently.

---

## Verification Checklists

### M1 Backend Verification

- [ ] All unit tests pass: `mvn test -Dtest=StorageLocationServiceTest`
- [ ] All controller tests pass:
      `mvn test -Dtest=StorageLocationRestControllerTest`
- [ ] DELETE endpoints return 409 for locations with children
- [ ] StorageDevice connectivity fields persist via API
- [ ] FHIR Location includes connectivity extensions

### M2 Frontend Verification

- [ ] All Jest tests pass: `npm test`
- [ ] All Cypress E2E tests pass:
      `npm run cy:run -- --spec "cypress/e2e/storageLocationCrud.cy.js"`
- [ ] "Add" buttons appear on all tabs
- [ ] OverflowMenu shows Edit/Delete on all rows
- [ ] Modals validate IP address format
- [ ] Deletion blocked message displays for locations with children
- [ ] All strings are localized (no hardcoded English)

---

## Notes

- **TDD**: Tests MUST fail before implementation (Red-Green-Refactor)
- **[P]** tasks can run in parallel (different files, no dependencies)
- **[US1]** / **[US2]** labels map tasks to user stories for traceability
- **Spec Reference**: `specs/OGC-68-storage-location-crud/spec.md FR-XXX` for
  requirement tracing
- **Pre-commit**: Always run `mvn spotless:apply` (backend) and `npm run format`
  (frontend)
- **Constitution V.5**: Run Cypress tests individually during development, not
  full suite
