# Tasks: Referred Sample Container Management System (Shipment Support)

**Input**: Design documents from `/specs/002-shipment-support/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: This project includes comprehensive test tasks following TDD workflow
and OpenELIS Constitution requirements (>70% coverage, E2E tests per
Constitution V.5).

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a web application with backend and frontend:

- **Backend**: `src/main/java/org/openelisglobal/shipment/`
- **Frontend**: `frontend/src/components/shipment/`
- **Tests**: `src/test/java/org/openelisglobal/shipment/` (backend),
  `frontend/cypress/e2e/` (E2E)
- **Liquibase**: `src/main/resources/liquibase/shipment/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and module structure

- [ ] T001 Create shipment module package structure in
      `src/main/java/org/openelisglobal/shipment/` with subpackages:
      valueholder, dao, service, controller, form
- [ ] T002 [P] Create frontend shipment module in
      `frontend/src/components/shipment/` with subdirectories for components
- [ ] T003 [P] Create Liquibase migration directory at
      `src/main/resources/liquibase/shipment/`
- [ ] T004 [P] Create test directory structure in
      `src/test/java/org/openelisglobal/shipment/` with subdirectories: service,
      dao, controller
- [ ] T005 [P] Create Cypress E2E test directory in `frontend/cypress/e2e/` for
      shipment tests
- [ ] T006 [P] Add shipment internationalization keys skeleton in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can
be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Schema Foundation

- [ ] T007 Create Liquibase changeset
      `shipment-001-create-shipping-box-table.xml` in
      `src/main/resources/liquibase/shipment/` with columns: id, box_id
      (unique), fhir_uuid, destination_facility_id, state (ENUM), created_date,
      created_by, sent_date, received_date, archived (boolean), sys_user_id,
      lastupdated
- [ ] T008 [P] Create Liquibase changeset
      `shipment-002-create-shipment-table.xml` with columns: id, shipment_id
      (unique), fhir_uuid, destination_facility_id, tracking_number, courier,
      shipment_date, expected_delivery_date, status (ENUM), sys_user_id,
      lastupdated
- [ ] T009 [P] Create Liquibase changeset
      `shipment-003-create-box-sample-table.xml` with columns: id, box_id (FK),
      sample_item_id (FK), sample_accession_number, status (ENUM), added_date,
      received_date, sys_user_id, lastupdated
- [ ] T010 [P] Create Liquibase changeset
      `shipment-004-create-unassigned-sample-table.xml` with columns: id,
      sample_item_id (FK), referral_date, destination_facility_id,
      referral_reason, days_unassigned (calculated), lost_status (boolean),
      assigned_to_user_id, sys_user_id, lastupdated
- [ ] T011 [P] Create Liquibase changeset `shipment-005-add-indexes.xml` for
      performance-critical columns: box_id, shipment_id,
      sample_accession_number, destination_facility_id, sent_date,
      received_date, state
- [ ] T012 [P] Create Liquibase changeset `shipment-006-add-foreign-keys.xml`
      for relationships: shipping_box.shipment_id → shipment.id,
      box_sample.box_id → shipping_box.id, etc.

### Base Entities (Valueholders)

- [ ] T013 [P] Create `ShippingBox.java` valueholder in
      `src/main/java/org/openelisglobal/shipment/valueholder/` extending
      BaseObject, with JPA annotations (@Entity, @Table, @Column), state enum,
      relationships (@ManyToOne for Shipment and Facility), @PrePersist for
      fhir_uuid generation
- [ ] T014 [P] Create `Shipment.java` valueholder with @OneToMany relationship
      to ShippingBox, JPA annotations for all fields
- [ ] T015 [P] Create `BoxSample.java` valueholder with @ManyToOne relationships
      to ShippingBox and SampleItem
- [ ] T016 [P] Create `UnassignedSample.java` valueholder with @ManyToOne
      relationships to SampleItem and Facility
- [ ] T017 [P] Create `BoxState` enum in
      `src/main/java/org/openelisglobal/shipment/valueholder/` with values:
      Draft, ReadyToSend, Sent, InTransit, PartiallyReceived, Received,
      Reconciled, Cancelled, LostInTransit
- [ ] T018 [P] Create `SampleStatus` enum with values: Pending, Sent, Received,
      Missing, Damaged, Rejected
- [ ] T019 [P] Create `ShipmentStatus` enum with values: InTransit, Delivered,
      Reconciled

### ORM Validation Tests (Constitution V.4)

> **NOTE**: These tests MUST pass before proceeding to user story implementation

- [ ] T020 Create `HibernateMappingValidationTest.java` in
      `src/test/java/org/openelisglobal/shipment/` that builds SessionFactory
      using config.addAnnotatedClass() for all shipment entities, validates
      mappings load without errors, executes in <5 seconds, NO database required

### Base DAOs

- [ ] T021 [P] Create `ShippingBoxDAO.java` interface in
      `src/main/java/org/openelisglobal/shipment/dao/` extending BaseDAO
- [ ] T022 [P] Create `ShippingBoxDAOImpl.java` in
      `src/main/java/org/openelisglobal/shipment/dao/` extending BaseDAOImpl,
      with methods: findByState(String state, Pageable pageable),
      findByStateWithRelationships(String state), countByState(String state)
- [ ] T023 [P] Create `ShipmentDAO.java` interface and `ShipmentDAOImpl.java`
      with methods: findByDestinationFacility, findActive
- [ ] T024 [P] Create `BoxSampleDAO.java` interface and `BoxSampleDAOImpl.java`
      with methods: findByBoxId, findBySampleAccessionNumber
- [ ] T025 [P] Create `UnassignedSampleDAO.java` interface and
      `UnassignedSampleDAOImpl.java` with methods: findAll(Pageable),
      findByDestinationFacility, findByAssignedUser, countUnassigned

### Base Services

- [ ] T026 [P] Create `ShippingBoxService.java` interface in
      `src/main/java/org/openelisglobal/shipment/service/` with methods: insert,
      update, get, getBoxesForDashboard (returns complete DTOs with JOIN FETCH)
- [ ] T027 [P] Create `ShippingBoxServiceImpl.java` annotated with @Service and
      @Transactional, implementing all interface methods with data compilation
      within transactions (NO lazy loading issues)
- [ ] T028 [P] Create `ShipmentService.java` interface and
      `ShipmentServiceImpl.java` with shipment management methods
- [ ] T029 [P] Create `UnassignedSampleService.java` interface and
      `UnassignedSampleServiceImpl.java` with unassigned sample query methods
      returning complete DTOs

### Frontend Foundation

- [ ] T030 [P] Create `ShipmentDashboard.jsx` skeleton in
      `frontend/src/components/shipment/` with Carbon Tabs component (Shipments,
      Unassigned Tests tabs)
- [ ] T031 [P] Create `shipmentApi.js` in `frontend/src/components/shipment/`
      with SWR hooks for API calls: useBoxes(), useUnassignedSamples(),
      useShipments()
- [ ] T032 [P] Add routing for /shipment path in `frontend/src/routes/` pointing
      to ShipmentDashboard

**Checkpoint**: Foundation ready - user story implementation can now begin in
parallel

---

## Phase 3: User Story 1 - Create and Manage Sample Boxes (Priority: P1) 🎯 MVP

**Goal**: Enable shipping coordinators to create boxes with unique IDs,
add/remove samples, view manifest, and manage box lifecycle (Draft → Ready to
Send)

**Independent Test**: Create a box, add 3 samples, remove 1 sample, view
manifest with 2 samples, mark box as Ready to Send - verify box state
transitions and manifest accuracy

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T033 [P] [US1] Unit test for ShippingBoxService.createBox() in
      `src/test/java/org/openelisglobal/shipment/service/ShippingBoxServiceTest.java` -
      verify box created with unique box_id, Draft state, empty samples list
- [ ] T034 [P] [US1] Unit test for ShippingBoxService.addSampleToBox() - verify
      sample added, status Pending, audit trail recorded
- [ ] T035 [P] [US1] Unit test for ShippingBoxService.removeSampleFromBox() -
      verify sample removed, audit trail recorded
- [ ] T036 [P] [US1] Unit test for ShippingBoxService.markBoxAsReadyToSend() -
      verify state transition Draft → ReadyToSend, validation for non-empty box
- [ ] T037 [P] [US1] Integration test for POST /rest/shipment/boxes endpoint in
      `src/test/java/org/openelisglobal/shipment/controller/ShipmentRestControllerTest.java` -
      verify 201 Created response, box persisted to database
- [ ] T038 [P] [US1] Integration test for PUT
      /rest/shipment/boxes/{id}/samples - verify sample added to box
- [ ] T039 [P] [US1] Integration test for GET /rest/shipment/boxes/{id} - verify
      box details with samples returned
- [ ] T040 [P] [US1] Cypress E2E test in
      `frontend/cypress/e2e/boxCreation.cy.js` - full workflow: create box, add
      samples via search, remove sample, mark Ready to Send (per Constitution
      V.5: run individually, review console logs)

### Implementation for User Story 1

#### Backend - Controllers & Forms

- [ ] T041 [US1] Create `ShippingBoxForm.java` in
      `src/main/java/org/openelisglobal/shipment/form/` with fields:
      destinationFacilityId, temperatureRequirement, notes
- [ ] T042 [US1] Create `AddSampleForm.java` with fields: boxId,
      sampleAccessionNumber
- [ ] T043 [US1] Create `ShipmentRestController.java` in
      `src/main/java/org/openelisglobal/shipment/controller/` extending
      BaseRestController, annotated @RestController
      @RequestMapping("/rest/shipment")
- [ ] T044 [US1] Implement POST /rest/shipment/boxes endpoint in
      ShipmentRestController (creates box, calls service, returns 201 Created)
- [ ] T045 [US1] Implement GET /rest/shipment/boxes/{id} endpoint (calls
      service.getBoxWithSamples - returns complete DTO)
- [ ] T046 [US1] Implement PUT /rest/shipment/boxes/{id}/samples endpoint (add
      sample to box)
- [ ] T047 [US1] Implement DELETE /rest/shipment/boxes/{id}/samples/{sampleId}
      endpoint (remove sample)
- [ ] T048 [US1] Implement PUT /rest/shipment/boxes/{id}/ready endpoint (mark
      box as Ready to Send, validate non-empty)

#### Frontend - Components

- [ ] T049 [P] [US1] Create `CreateBoxModal.jsx` in
      `frontend/src/components/shipment/` with Carbon Modal, TextInput for
      destination, Dropdown for temperature, notes TextArea (all
      internationalized)
- [ ] T050 [P] [US1] Create `SampleSearchInput.jsx` with Carbon ComboBox for
      sample lookup by accession number (debounced search)
- [ ] T051 [P] [US1] Create `BoxManifestTable.jsx` with Carbon DataTable showing
      samples in box (columns: accession, type, status, actions with
      OverflowMenu for remove)
- [ ] T052 [US1] Integrate CreateBoxModal into ShipmentDashboard - add "Create
      Box" Carbon Button that opens modal, handle form submission, call API
- [ ] T053 [US1] Implement box details view in ShipmentDashboard - display box
      metadata, embedded BoxManifestTable, SampleSearchInput for adding samples
- [ ] T054 [US1] Add "Mark Ready to Send" Carbon Button to box details view -
      calls API, shows success notification

#### Internationalization

- [ ] T055 [P] [US1] Add all User Story 1 i18n keys to
      `frontend/src/languages/en.json`: shipment.box.create,
      shipment.box.addSample, shipment.box.manifest, shipment.box.readyToSend,
      etc.
- [ ] T056 [P] [US1] Add French translations to `frontend/src/languages/fr.json`
      for all User Story 1 keys

**Checkpoint**: At this point, User Story 1 should be fully functional - can
create boxes, manage samples, mark Ready to Send

---

## Phase 4: User Story 2 - Track Unassigned Referral Samples (Priority: P1)

**Goal**: Enable coordinators to view samples marked for referral but not yet
assigned to boxes, filter by destination/priority/days-unassigned, assign to
user, mark as lost

**Independent Test**: Mark 5 samples for referral (different destinations,
priorities), view Unassigned Tests tab filtered by destination, assign 2 samples
to user, mark 1 as lost - verify filtering, assignment, and lost status

### Tests for User Story 2 ⚠️

- [ ] T057 [P] [US2] Unit test for
      UnassignedSampleService.getUnassignedSamples() - verify pagination,
      filtering by destination, sorting by days_unassigned
- [ ] T058 [P] [US2] Unit test for
      UnassignedSampleService.assignSampleToUser() - verify assigned_to_user_id
      updated, audit trail
- [ ] T059 [P] [US2] Unit test for UnassignedSampleService.markSampleAsLost() -
      verify lost_status = true, confirmation required
- [ ] T060 [P] [US2] Integration test for GET /rest/shipment/unassigned - verify
      pagination (page, size params), filtering (destination, priority query
      params)
- [ ] T061 [P] [US2] Integration test for PUT
      /rest/shipment/unassigned/{id}/assign - verify user assignment
- [ ] T062 [P] [US2] Cypress E2E test in
      `frontend/cypress/e2e/unassignedTracking.cy.js` - filter unassigned
      samples by destination, assign to user, mark as lost (run individually,
      review console logs)

### Implementation for User Story 2

#### Backend

- [ ] T063 [P] [US2] Create `UnassignedSampleForm.java` in
      `src/main/java/org/openelisglobal/shipment/form/` with fields for filters:
      destinationFacilityId, priority, assignedToUserId, daysUnassignedMin
- [ ] T064 [US2] Implement GET /rest/shipment/unassigned endpoint in
      ShipmentRestController - supports pagination (page, size), filtering
      (destination, priority, assignedUser), sorting (daysUnassigned DESC)
- [ ] T065 [US2] Implement PUT /rest/shipment/unassigned/{id}/assign endpoint -
      assign sample to user
- [ ] T066 [US2] Implement PUT /rest/shipment/unassigned/{id}/lost endpoint -
      mark sample as lost (requires confirmation via request param)
- [ ] T067 [US2] Implement GET /rest/shipment/unassigned/count endpoint - return
      count of unassigned samples for dashboard badge

#### Frontend

- [ ] T068 [P] [US2] Create `UnassignedTestsTable.jsx` in
      `frontend/src/components/shipment/` with Carbon DataTable, columns:
      accession, lab number, patient name (permission-based), type, priority,
      destination, days unassigned, assigned to, actions
- [ ] T069 [P] [US2] Create `UnassignedTestsFilters.jsx` with Carbon Dropdown
      for destination filter, Checkbox for "My cases" filter, NumberInput for
      days-unassigned threshold
- [ ] T070 [US2] Integrate UnassignedTestsTable into ShipmentDashboard
      Unassigned Tests tab - fetch data with SWR, handle pagination (Carbon
      Pagination component)
- [ ] T071 [US2] Add row actions to UnassignedTestsTable - OverflowMenu with
      options: Assign to me, Assign to user (opens modal), Mark as lost
      (confirmation modal)
- [ ] T072 [US2] Implement patient name permission check - use SWR to fetch user
      permissions, conditionally render patient name column based on "View
      Patient Names" permission

#### Internationalization

- [ ] T073 [P] [US2] Add User Story 2 i18n keys to
      `frontend/src/languages/en.json`: shipment.unassigned.title,
      shipment.unassigned.filter, shipment.unassigned.assignToMe,
      shipment.unassigned.markLost, etc.
- [ ] T074 [P] [US2] Add French translations for User Story 2 keys

**Checkpoint**: User Stories 1 AND 2 should both work independently - can create
boxes AND track unassigned samples

---

## Phase 5: User Story 3 - Generate Labels and Manifests (Priority: P1)

**Goal**: Enable coordinators to generate printable labels (box barcodes) and
packing lists (manifests with sample details) for boxes marked Ready to Send

**Independent Test**: Mark box as Ready to Send with 3 samples, generate box
label PDF (with QR code), generate manifest PDF (with box barcode + sample list
table) - verify PDFs download and print correctly

### Tests for User Story 3 ⚠️

- [ ] T075 [P] [US3] Unit test for ShipmentLabelService.generateBoxLabel() -
      verify PDF stream generated, QR code contains box_id, label dimensions
      correct
- [ ] T076 [P] [US3] Unit test for ShipmentLabelService.generateManifest() -
      verify PDF contains box barcode, sample list table with all samples,
      destination info
- [ ] T077 [P] [US3] Integration test for GET /rest/shipment/boxes/{id}/label -
      verify PDF response with Content-Type: application/pdf,
      Content-Disposition: attachment
- [ ] T078 [P] [US3] Integration test for GET
      /rest/shipment/boxes/{id}/manifest - verify PDF generated for box in
      ReadyToSend state
- [ ] T079 [P] [US3] Cypress E2E test in
      `frontend/cypress/e2e/manifestGeneration.cy.js` - create box with samples,
      mark Ready to Send, download label, download manifest (verify downloads)

### Implementation for User Story 3

#### Backend - Label/Manifest Generation

- [ ] T080 [P] [US3] Create `ShipmentLabelService.java` interface in
      `src/main/java/org/openelisglobal/shipment/service/` with methods:
      generateBoxLabel(String boxId), generateManifest(String boxId)
- [ ] T081 [US3] Create `ShipmentLabelServiceImpl.java` implementing
      ShipmentLabelService - use iText 5.5.13.4 for PDF generation, ZXing 3.5.2
      for QR codes (follow BarcodeLabelMaker pattern from existing codebase)
- [ ] T082 [US3] Implement generateBoxLabel() method - create PDF with QR code
      (box_id), destination name, box metadata (size: 4" x 2" label)
- [ ] T083 [US3] Implement generateManifest() method - create PDF with box
      barcode (Code 128), sample list table (accession, type, status, individual
      barcodes), destination info, temperature designation
- [ ] T084 [US3] Implement GET /rest/shipment/boxes/{id}/label endpoint in
      ShipmentRestController - return PDF ByteArrayOutputStream as
      ResponseEntity with headers: Content-Type: application/pdf,
      Content-Disposition: attachment; filename="box-{id}-label.pdf"
- [ ] T085 [US3] Implement GET /rest/shipment/boxes/{id}/manifest endpoint -
      return manifest PDF

#### Frontend

- [ ] T086 [P] [US3] Add "Generate Label" Carbon Button to box details view -
      calls label endpoint, triggers download
- [ ] T087 [P] [US3] Add "Generate Manifest" Carbon Button to box details view -
      calls manifest endpoint, triggers download
- [ ] T088 [US3] Handle PDF download in browser - use fetch with blob response,
      create download link, auto-click to trigger download

#### Internationalization

- [ ] T089 [P] [US3] Add User Story 3 i18n keys: shipment.box.generateLabel,
      shipment.box.generateManifest, shipment.box.labelDownloaded,
      shipment.box.manifestDownloaded
- [ ] T090 [P] [US3] Add French translations for User Story 3 keys

**Checkpoint**: User Stories 1, 2, AND 3 should all work independently - can
create boxes, track unassigned samples, AND generate labels/manifests

---

## Phase 6: User Story 4 - Send Boxes to Reference Lab (Priority: P1)

**Goal**: Enable coordinators to mark boxes as Sent, automatically transmit
electronic manifests via FHIR/API/Email, group multiple boxes into shipments,
track transmission status

**Independent Test**: Mark 2 boxes (same destination) as Sent, verify shipment
created with both boxes, electronic manifest transmitted via FHIR (or Email
fallback), tracking number and courier recorded, transmission status SUCCESS

### Tests for User Story 4 ⚠️

- [ ] T091 [P] [US4] Unit test for
      ShipmentTransmissionService.transmitManifestAsync() - verify FHIR
      SupplyDelivery resource created, retry logic (3 attempts with exponential
      backoff), Email fallback
- [ ] T092 [P] [US4] Unit test for ShipmentService.createShipmentForBoxes() -
      verify multiple boxes grouped into single shipment, shared
      tracking/courier
- [ ] T093 [P] [US4] Integration test for POST /rest/shipment/boxes/{id}/send -
      verify box state transition ReadyToSend → Sent → InTransit, shipment
      created, manifest locked
- [ ] T094 [P] [US4] Integration test for FHIR SupplyDelivery creation - verify
      resource structure matches R4 spec, identifier system, destination
      reference
- [ ] T095 [P] [US4] Cypress E2E test in
      `frontend/cypress/e2e/boxSending.cy.js` - mark box as Sent, enter
      tracking/courier, verify shipment created, transmission status displayed
      (run individually)

### Implementation for User Story 4

#### Backend - FHIR Integration

- [ ] T096 [P] [US4] Create `ShipmentFhirTransform.java` in
      `src/main/java/org/openelisglobal/shipment/fhir/` with method
      transformToFhirSupplyDelivery(ShippingBox box, Shipment shipment) -
      follows StorageLocationFhirTransform pattern
- [ ] T097 [US4] Implement transformToFhirSupplyDelivery() - map ShippingBox →
      SupplyDelivery: identifier (box_id), status (mapBoxStateToSupplyStatus),
      suppliedItem (samples), destination (facility FHIR reference), extensions
      (temperature, courier, tracking)
- [ ] T098 [US4] Create `ShipmentTransmissionService.java` interface with method
      transmitManifestAsync(ShippingBox box, Shipment shipment) returning
      Future<TransmissionStatus>
- [ ] T099 [US4] Create `ShipmentTransmissionServiceImpl.java` annotated
      @Service, implement transmitManifestAsync() with @Async - try FHIR
      transmission first, fallback to Email, retry logic (3 attempts, 5s/10s/15s
      delays), persist transmission attempt status
- [ ] T100 [US4] Create Liquibase changeset
      `shipment-007-create-transmission-attempt-table.xml` with columns: id,
      box_id, shipment_id, attempt_number, transmission_method (FHIR/API/Email),
      status (IN_PROGRESS/SUCCESS/FAILED), error_message, started_at,
      completed_at

#### Backend - Shipment Management

- [ ] T101 [US4] Create `SendBoxForm.java` with fields: boxId, trackingNumber,
      courier, expectedDeliveryDate
- [ ] T102 [US4] Implement POST /rest/shipment/boxes/{id}/send endpoint -
      validate box in ReadyToSend state, lock manifest (prevent sample edits),
      transition state to Sent, create/update shipment, trigger async
      transmission, return transmission_attempt_id
- [ ] T103 [US4] Implement ShipmentService.createOrUpdateShipment() - if boxes
      with same destination sent on same day exist, add to existing shipment,
      else create new shipment
- [ ] T104 [US4] Implement GET /rest/shipment/transmission/{attemptId}/status
      endpoint - poll transmission status (allows frontend to show progress)

#### Frontend

- [ ] T105 [P] [US4] Create `SendBoxModal.jsx` with Carbon Modal, TextInput for
      tracking number, Dropdown for courier (configurable list), DatePicker for
      expected delivery
- [ ] T106 [US4] Add "Send Box" Carbon Button to box details view (enabled only
      for ReadyToSend state) - opens SendBoxModal
- [ ] T107 [US4] Implement send box flow - submit form, show loading state, poll
      transmission status, display success/failure notification with retry
      option
- [ ] T108 [US4] Add transmission status indicator to box details - shows
      FHIR/API/Email transmission status, timestamp, error message if failed

#### Internationalization

- [ ] T109 [P] [US4] Add User Story 4 i18n keys: shipment.box.send,
      shipment.box.trackingNumber, shipment.box.courier,
      shipment.transmission.success, shipment.transmission.failed,
      shipment.transmission.retrying
- [ ] T110 [P] [US4] Add French translations for User Story 4 keys

**Checkpoint**: User Stories 1-4 should all work - can create boxes, track
unassigned samples, generate labels/manifests, AND send boxes with electronic
transmission

---

## Phase 7: User Story 5 - Receive and Reconcile Boxes (Priority: P1)

**Goal**: Enable receiving technicians to scan box IDs to initiate receiving,
scan sample barcodes to mark received, record non-conformities via inline
actions (integrated with quality system), complete receiving when all samples
accounted for

**Independent Test**: Scan box barcode to initiate receiving, scan 2 sample
barcodes (auto-mark received), manually check 1 sample, record non-conformity
for 1 sample (damaged) with photo attachment, mark box complete - verify box
state transitions InTransit → PartiallyReceived → Received, non-conformity
created in quality system

### Tests for User Story 5 ⚠️

- [ ] T111 [P] [US5] Unit test for ShippingBoxService.initiateReceiving() -
      verify state transition InTransit → PartiallyReceived,
      receiving_started_at timestamp set
- [ ] T112 [P] [US5] Unit test for ShippingBoxService.markSampleReceived() -
      verify sample status updated, received_date set, auto-save progress
- [ ] T113 [P] [US5] Unit test for
      ShipmentReceivingService.recordNonConformity() - verify
      NCEventService.insert() called with correct params, box_id context field
      populated, disposition saved
- [ ] T114 [P] [US5] Unit test for ShippingBoxService.completeReceiving() -
      verify state transition PartiallyReceived → Received when all samples
      accounted for (received/missing/damaged)
- [ ] T115 [P] [US5] Integration test for POST
      /rest/shipment/boxes/{id}/receive - verify receiving initiated
- [ ] T116 [P] [US5] Integration test for PUT
      /rest/shipment/boxes/{id}/receive/samples/{sampleId} - verify sample
      marked received
- [ ] T117 [P] [US5] Integration test for POST
      /rest/shipment/boxes/{id}/receive/nonconformities - verify non-conformity
      created in `org.openelisglobal.qaevent` NcEvent table
- [ ] T118 [P] [US5] Cypress E2E test in
      `frontend/cypress/e2e/boxReceiving.cy.js` - scan box barcode, scan sample
      barcodes, record non-conformity with inline action, complete receiving
      (run individually, review console logs, verify auto-save)

### Implementation for User Story 5

#### Backend - Quality System Integration

- [ ] T119 [US5] Explore existing `org.openelisglobal.qaevent` package -
      identify NcEvent entity structure, NCEventService methods, required fields
      (RESEARCH from plan.md R2)
- [ ] T120 [US5] Create `ShipmentReceivingService.java` interface with methods:
      recordNonConformity(String boxId, String sampleId, String type, String
      notes, List<MultipartFile> attachments, String disposition)
- [ ] T121 [US5] Create `ShipmentReceivingServiceImpl.java` implementing
      ShipmentReceivingService - inject NCEventService, map shipment
      non-conformity data to NcEvent entity, set nce_category_id = "Shipment",
      add box_id to description/context, handle photo uploads
- [ ] T122 [US5] Create Liquibase changeset
      `shipment-008-add-nce-category-shipment.xml` to insert "Shipment" category
      into nce_category table (if not exists)

#### Backend - Receiving Workflow

- [ ] T123 [US5] Create `ReceivingProgressForm.java` with fields: boxId,
      receivedSamples (List<String> accessionNumbers), receivingMethod
      (SCAN/MANUAL)
- [ ] T124 [US5] Create `NonConformityForm.java` with fields: boxId, sampleId,
      nonConformityType, notes, sampleDisposition, attachments (MultipartFile[])
- [ ] T125 [US5] Implement POST /rest/shipment/boxes/{id}/receive endpoint -
      initiate receiving, transition to PartiallyReceived
- [ ] T126 [US5] Implement PUT
      /rest/shipment/boxes/{id}/receive/samples/{sampleId} endpoint - mark
      sample received (scan or manual), auto-save progress, distinguish method
      in audit trail (FR-053)
- [ ] T127 [US5] Implement POST
      /rest/shipment/boxes/{id}/receive/nonconformities endpoint - record
      non-conformity via ShipmentReceivingService, handle multipart form data
      (photo uploads)
- [ ] T128 [US5] Implement PUT
      /rest/shipment/boxes/{id}/receive/samples/{sampleId}/missing endpoint -
      mark sample as missing
- [ ] T129 [US5] Implement POST /rest/shipment/boxes/{id}/receive/complete
      endpoint - complete receiving, transition to Received state when all
      samples accounted for

#### Frontend - Barcode Scanning

- [ ] T130 [P] [US5] Create `BarcodeInput.jsx` in
      `frontend/src/components/shipment/` - reuse UnifiedBarcodeInput pattern
      from existing storage module, detect rapid input (<50ms), debounce
      (500ms), distinguish box vs sample barcodes
- [ ] T131 [P] [US5] Add "Simulate Scan" button to BarcodeInput for testing
      without physical scanner (FR-048)

#### Frontend - Receiving Workflow

- [ ] T132 [P] [US5] Create `ReceivingWorkflow.jsx` with Carbon
      ProgressIndicator (steps: Scan Box, Check Samples, Complete), embedded
      BarcodeInput for box and samples
- [ ] T133 [US5] Create `ReceivingSampleTable.jsx` with Carbon DataTable showing
      expected samples, columns: accession, type, status
      (Pending/Received/Missing), received_date, actions (inline OverflowMenu:
      Manual check, Mark missing, Reject sample)
- [ ] T134 [US5] Create `NonConformityModal.jsx` with Carbon Modal, Dropdown for
      non-conformity type (load from config), TextArea for notes, FileUploader
      for photos (max 5 files, 10MB each), RadioButtonGroup for sample
      disposition
- [ ] T135 [US5] Integrate ReceivingWorkflow into ShipmentDashboard - add
      "Receive Boxes" section, handle box barcode scan to initiate
- [ ] T136 [US5] Implement inline "Reject Sample" action in
      ReceivingSampleTable - opens NonConformityModal (NOT modal after scan, per
      spec clarification)
- [ ] T137 [US5] Implement auto-save on sample check - PUT request after each
      scan/manual check, show toast notification on save
- [ ] T138 [US5] Add "Complete Receiving" Carbon Button - enabled when all
      samples accounted for, calls complete endpoint, transitions box to
      Received state

#### Internationalization

- [ ] T139 [P] [US5] Add User Story 5 i18n keys: shipment.receiving.scanBox,
      shipment.receiving.scanSample, shipment.receiving.rejectSample,
      shipment.nonconformity.type, shipment.nonconformity.notes,
      shipment.nonconformity.attachments, shipment.nonconformity.disposition
- [ ] T140 [P] [US5] Add French translations for User Story 5 keys

**Checkpoint**: All P1 user stories (1-5) should be functional - complete
end-to-end workflow from box creation to receiving

---

## Phase 8: User Story 6 - View Shipment Dashboard (Priority: P2)

**Goal**: Enable users to view dashboard with key metrics (boxes ready to send,
in transit, awaiting receipt, received this week), search/filter boxes, view
shipment-level information

**Independent Test**: View dashboard metrics (4 cards show correct counts),
filter boxes by state (In Transit), search by box ID, view shipment details
(tracking, courier, total boxes/samples)

### Tests for User Story 6 ⚠️

- [ ] T141 [P] [US6] Unit test for
      ShipmentDashboardService.getDashboardMetrics() - verify counts calculated
      correctly for each metric: readyToSend, inTransit, awaitingReceipt,
      receivedThisWeek
- [ ] T142 [P] [US6] Integration test for GET /rest/shipment/dashboard/metrics -
      verify response JSON structure with all 4 metrics
- [ ] T143 [P] [US6] Integration test for GET /rest/shipment/boxes with query
      params (state, search, page, size) - verify filtering and pagination
- [ ] T144 [P] [US6] Cypress E2E test in
      `frontend/cypress/e2e/shipmentDashboard.cy.js` - view dashboard, verify
      metrics displayed, filter boxes, search by ID (run individually)

### Implementation for User Story 6

#### Backend

- [ ] T145 [P] [US6] Create `ShipmentDashboardService.java` interface with
      method getDashboardMetrics() returning Map<String, Long>
- [ ] T146 [US6] Create `ShipmentDashboardServiceImpl.java` - use DAO queries
      with countByState() to calculate metrics
- [ ] T147 [US6] Implement GET /rest/shipment/dashboard/metrics endpoint -
      return JSON: {readyToSend: X, inTransit: Y, awaitingReceipt: Z,
      receivedThisWeek: W}
- [ ] T148 [US6] Implement GET /rest/shipment/boxes endpoint with query params:
      state, search (box_id or sample accession), page, size - return paginated
      results with complete DTOs (JOIN FETCH to prevent lazy loading)
- [ ] T149 [US6] Implement GET /rest/shipment/shipments/{id} endpoint - return
      shipment details with list of boxes, tracking, courier, total counts

#### Frontend

- [ ] T150 [P] [US6] Create `MetricCard.jsx` in
      `frontend/src/components/shipment/` - Carbon Tile with title, value, icon
      (Carbon icons)
- [ ] T151 [US6] Add 4 MetricCards to ShipmentDashboard Shipments tab - fetch
      from /dashboard/metrics API, display: Ready to Send, In Transit, Awaiting
      Receipt, Received This Week
- [ ] T152 [US6] Create `BoxesTable.jsx` with Carbon DataTable, columns: box_id,
      destination, state, created_date, sent_date, sample_count, actions (View,
      Download Manifest)
- [ ] T153 [US6] Add search/filter controls to Shipments tab - Carbon Search for
      box ID, Dropdown for state filter
- [ ] T154 [US6] Implement pagination for BoxesTable - Carbon Pagination
      component, fetch data on page change
- [ ] T155 [US6] Create `ShipmentDetailsModal.jsx` - show shipment info:
      tracking, courier, expected delivery, list of boxes, total sample count
- [ ] T156 [US6] Add "View Shipment" action to BoxesTable rows (for boxes with
      shipment_id) - opens ShipmentDetailsModal

#### Internationalization

- [ ] T157 [P] [US6] Add User Story 6 i18n keys:
      shipment.dashboard.metrics.readyToSend,
      shipment.dashboard.metrics.inTransit, shipment.dashboard.search,
      shipment.dashboard.filter, shipment.shipmentDetails
- [ ] T158 [P] [US6] Add French translations for User Story 6 keys

**Checkpoint**: All P1 + P2 user stories functional - complete workflow +
dashboard visibility

---

## Phase 9: User Story 7 - Generate Reports (Priority: P3)

**Goal**: Enable administrators to generate reports: boxes sent in date range,
samples in transit, aging unassigned samples, transmission success rate

**Independent Test**: Generate "Boxes Sent" report for last 30 days with 10
boxes, export as CSV, verify all columns present (box_id, destination,
sent_date, sample_count, tracking_number)

### Tests for User Story 7 ⚠️

- [ ] T159 [P] [US7] Unit test for
      ShipmentReportService.generateBoxesSentReport() - verify date range
      filtering, CSV generation
- [ ] T160 [P] [US7] Integration test for POST /rest/shipment/reports - verify
      CSV download with Content-Type: text/csv
- [ ] T161 [P] [US7] Cypress E2E test in
      `frontend/cypress/e2e/shipmentReports.cy.js` - generate report, verify CSV
      download (if Cypress supports)

### Implementation for User Story 7

#### Backend

- [ ] T162 [P] [US7] Create `ShipmentReportService.java` interface with methods:
      generateBoxesSentReport(Date startDate, Date endDate),
      generateSamplesInTransitReport(), generateAgingUnassignedSamplesReport(),
      generateTransmissionSuccessRateReport()
- [ ] T163 [US7] Create `ShipmentReportServiceImpl.java` - implement CSV
      generation using Apache Commons CSV library (add dependency to pom.xml if
      needed)
- [ ] T164 [US7] Implement generateBoxesSentReport() - query boxes sent in date
      range, compile data (box_id, destination, sent_date, sample_count,
      tracking_number), return CSV ByteArrayOutputStream
- [ ] T165 [US7] Implement generateAgingUnassignedSamplesReport() - query
      unassigned samples with days_unassigned > threshold, group by destination
- [ ] T166 [US7] Create `ReportForm.java` with fields: reportType (ENUM),
      startDate, endDate, filters (Map<String, String>)
- [ ] T167 [US7] Implement POST /rest/shipment/reports endpoint - accept
      ReportForm, call appropriate service method, return CSV with headers:
      Content-Type: text/csv, Content-Disposition: attachment;
      filename="report.csv"

#### Frontend

- [ ] T168 [P] [US7] Create `ReportsPanel.jsx` in
      `frontend/src/components/shipment/` with Carbon Dropdown for report type,
      DatePicker for date range, Dropdown for filters
- [ ] T169 [US7] Add "Generate Report" Carbon Button to ReportsPanel - submit
      form, trigger CSV download
- [ ] T170 [US7] Add ReportsPanel to ShipmentDashboard as collapsible section
      (Carbon Accordion) - accessible to Administrator role only

#### Internationalization

- [ ] T171 [P] [US7] Add User Story 7 i18n keys: shipment.reports.title,
      shipment.reports.type.boxesSent, shipment.reports.type.samplesInTransit,
      shipment.reports.dateRange, shipment.reports.generate
- [ ] T172 [P] [US7] Add French translations for User Story 7 keys

**Checkpoint**: All user stories (P1, P2, P3) complete - full feature functional

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Performance Optimization

- [ ] T173 [P] Add database indexes (if not already in foundational phase) -
      verify indexes on: box_id, shipment_id, sample_accession_number,
      destination_facility_id, sent_date, received_date, state columns for
      performance
- [ ] T174 [P] Performance test dashboard metrics query - verify <5 seconds
      response time with 1,000+ boxes and 10,000+ samples (use JMeter or
      similar)
- [ ] T175 [P] Performance test unassigned samples pagination - verify <5
      seconds response time with 1,000+ unassigned samples

### Error Handling & Validation

- [ ] T176 [P] Add comprehensive input validation to all forms - use Hibernate
      Validator (@NotNull, @Size, @Pattern) on backend, Formik + Yup on frontend
- [ ] T177 [P] Add error handling for barcode scanning - invalid format,
      duplicate scan, sample not found, box not found
- [ ] T178 [P] Add error handling for transmission failures - retry UI, manual
      retry button, error log display

### Logging & Observability (FR-054)

- [ ] T179 [P] Add structured logging (INFO, WARN, ERROR) to all service
      methods - use LogEvent.logInfo(), LogEvent.logWarn(), LogEvent.logError()
      from existing codebase
- [ ] T180 [P] Add alerting configuration for critical failures - failed
      manifest transmissions (>3 attempts), boxes stuck in transit beyond
      expected delivery date + 7 days

### Security

- [ ] T181 [P] Add RBAC checks to all endpoints - verify "Shipping" role
      required for coordinator actions, "Administrator" role for reports/admin
      actions
- [ ] T182 [P] Verify encryption at rest - patient names in unassigned_sample
      table encrypted (database-level encryption per CR-007)
- [ ] T183 [P] Verify TLS 1.2+ for all API communications - FHIR transmissions,
      external API calls

### Data Retention (FR-055)

- [ ] T184 [P] Implement soft-delete archival - add archived columns (archived,
      archived_date, archived_by_user_id, archive_reason) to shipping_box table
      via Liquibase changeset `shipment-009-add-archival-columns.xml`
- [ ] T185 [P] Update DAO queries to exclude archived boxes by default - add
      WHERE (archived IS NULL OR archived = false) to all findByState queries
- [ ] T186 [P] Add scheduled job (Spring @Scheduled) to auto-archive boxes older
      than 7 years - runs daily at 2 AM, only archives boxes in Closed state

### Documentation

- [ ] T187 [P] Create API documentation - OpenAPI spec in
      `specs/002-shipment-support/contracts/openapi.yaml` with all endpoints,
      request/response schemas
- [ ] T188 [P] Create developer quickstart guide - update
      `specs/002-shipment-support/quickstart.md` with step-by-step
      implementation example (User Story 1)
- [ ] T189 [P] Update CLAUDE_CONTEXT.md - run
      `.specify/scripts/bash/update-agent-context.sh claude` to add shipment
      module architecture

### Code Quality

- [ ] T190 [P] Run code formatting - execute `mvn spotless:apply` (backend) and
      `npm run format` (frontend) BEFORE commit
- [ ] T191 [P] Run Spotless check - execute `mvn spotless:check` to verify
      formatting compliance
- [ ] T192 [P] Code review checklist - verify no @Transactional in controllers,
      no entity relationship traversal in controllers, all services compile DTOs
      within transactions

---

## Phase 11: Constitution Compliance Verification (OpenELIS Global 3.0)

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

- [ ] T193 **Configuration-Driven**: Verify no country-specific code branches
      introduced - all label formats, aging thresholds, temperature ranges
      configurable via SystemConfiguration table
- [ ] T194 **Carbon Design System**: Audit UI - confirm @carbon/react used
      exclusively (NO Bootstrap/Tailwind) - check all components (Modal,
      DataTable, Button, Tabs, Dropdown, etc.)
- [ ] T195 **FHIR/IHE Compliance**: Validate FHIR SupplyDelivery resources
      against R4 profiles - use HAPI FHIR validator, verify all required fields
      present
- [ ] T196 **Layered Architecture**: Verify 5-layer pattern followed
      (Valueholder→DAO→Service→Controller→Form) - code review all packages
- [ ] T197 **Test Coverage**: Run JaCoCo coverage report - confirm >70% for new
      shipment module code (`mvn verify`, check `target/site/jacoco/`)
- [ ] T198 **ORM Validation**: Verify HibernateMappingValidationTest passes -
      SessionFactory builds without errors, <5s execution, NO database required
- [ ] T199 **Schema Management**: Verify ALL database changes use Liquibase
      changesets (NO direct SQL) - audit all changesets in
      `src/main/resources/liquibase/shipment/`
- [ ] T200 **Internationalization**: Audit UI strings - confirm React Intl used
      for ALL text (no hardcoded strings) - check en.json and fr.json have all
      keys
- [ ] T201 **Security & Compliance**: Verify RBAC implemented (Shipping +
      Administrator roles), audit trail (sys_user_id + lastupdated on all
      entities), input validation (Hibernate Validator + Formik/Yup)
- [ ] T202 **E2E Test Compliance (Constitution V.5)**: Verify E2E tests follow
      best practices - video disabled, screenshots enabled, console logging
      enabled, individual execution during development (<30s per test), post-run
      review checklist followed

**Verification Commands**:

```bash
# Backend: Code formatting (MUST run before each commit) + build + tests
mvn spotless:apply && mvn spotless:check && mvn clean install

# Frontend: Formatting (MUST run before each commit)
cd frontend && npm run format

# Run E2E tests individually (per Constitution V.5):
cd frontend && npm run cy:run -- --spec "cypress/e2e/boxCreation.cy.js"
cd frontend && npm run cy:run -- --spec "cypress/e2e/unassignedTracking.cy.js"
cd frontend && npm run cy:run -- --spec "cypress/e2e/manifestGeneration.cy.js"
cd frontend && npm run cy:run -- --spec "cypress/e2e/boxSending.cy.js"
cd frontend && npm run cy:run -- --spec "cypress/e2e/boxReceiving.cy.js"
cd frontend && npm run cy:run -- --spec "cypress/e2e/shipmentDashboard.cy.js"
# Full suite only in CI/CD: npm run cy:run

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user
  stories
- **User Stories (Phase 3-9)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 stories 1-5 → P2 story 6 → P3 story 7)
- **Polish (Phase 10)**: Depends on all desired user stories being complete
- **Constitution Compliance (Phase 11)**: Depends on Polish completion - final
  verification before merge

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No
  dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Independent of
  US1 (different entities/components)
- **User Story 3 (P1)**: Can start after US1 (depends on ShippingBox entity,
  ReadyToSend state)
- **User Story 4 (P1)**: Can start after US1 (depends on ShippingBox entity,
  Shipment entity for grouping)
- **User Story 5 (P1)**: Can start after US4 (depends on boxes being
  Sent/InTransit, quality system integration)
- **User Story 6 (P2)**: Can start after US1 (depends on ShippingBox queries,
  dashboard metrics)
- **User Story 7 (P3)**: Can start after US1-5 (depends on data existing for
  reports)

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD workflow)
- Models before services (entity classes before business logic)
- Services before controllers (business logic before REST endpoints)
- Backend before frontend (API endpoints before UI components)
- Core implementation before integration (story-specific code before cross-story
  integration)

### Parallel Opportunities

**Setup Phase (Phase 1)**: All tasks marked [P] can run in parallel

- T002 (frontend structure) + T003 (Liquibase) + T004 (test structure) + T005
  (Cypress) + T006 (i18n)

**Foundational Phase (Phase 2)**: Many tasks marked [P] can run in parallel

- Database schema changesets: T007-T012 (all [P], different files)
- Base entities: T013-T019 (all [P], different files)
- Base DAOs: T021-T025 (all [P], different files)
- Base services: T026-T029 (all [P], different files)
- Frontend foundation: T030-T032 (all [P], different files)

**User Stories**: Once Foundational phase completes, all user stories can start
in parallel (if team capacity allows)

- US1 + US2 can run in parallel (different entities, components)
- US3-5 depend on US1 but can start as soon as US1 models/services complete

**Within User Stories**: Tests marked [P] can run in parallel (different test
files)

- Example US1: T033-T040 (8 tests, all [P], different files)

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if TDD approach):
Task T033: Unit test for ShippingBoxService.createBox()
Task T034: Unit test for ShippingBoxService.addSampleToBox()
Task T035: Unit test for ShippingBoxService.removeSampleFromBox()
Task T036: Unit test for ShippingBoxService.markBoxAsReadyToSend()
Task T037: Integration test for POST /rest/shipment/boxes
Task T038: Integration test for PUT /rest/shipment/boxes/{id}/samples
Task T039: Integration test for GET /rest/shipment/boxes/{id}
Task T040: Cypress E2E test in boxCreation.cy.js

# Launch all frontend components for User Story 1 together:
Task T049: CreateBoxModal.jsx
Task T050: SampleSearchInput.jsx
Task T051: BoxManifestTable.jsx
Task T055: en.json i18n keys
Task T056: fr.json i18n keys
```

---

## Implementation Strategy

### MVP First (User Stories 1-3 Only)

Minimum viable product for box creation, sample assignment, and label
generation:

1. Complete Phase 1: Setup (6 tasks)
2. Complete Phase 2: Foundational (26 tasks - CRITICAL, blocks all stories)
3. Complete Phase 3: User Story 1 (25 tasks - Create/Manage Boxes)
4. Complete Phase 5: User Story 3 (15 tasks - Generate Labels/Manifests)
5. **STOP and VALIDATE**: Test US1+US3 independently (create box, add samples,
   generate label/manifest)
6. Deploy/demo if ready

**MVP Scope**: 72 tasks (Setup + Foundation + US1 + US3)

### Full P1 Implementation (User Stories 1-5)

Complete end-to-end workflow from box creation to receiving:

1. Complete Setup + Foundational → 32 tasks
2. Add User Story 1 (Create Boxes) → 25 tasks
3. Add User Story 2 (Unassigned Samples) → 18 tasks
4. Add User Story 3 (Labels/Manifests) → 15 tasks
5. Add User Story 4 (Send Boxes) → 20 tasks
6. Add User Story 5 (Receive/Reconcile) → 30 tasks
7. **Total P1**: 140 tasks

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (32 tasks)
2. Add User Story 1 → Test independently → Deploy/Demo (MVP with box creation:
   57 tasks total)
3. Add User Story 2 → Test independently → Deploy/Demo (MVP + unassigned
   tracking: 75 tasks total)
4. Add User Story 3 → Test independently → Deploy/Demo (MVP + labels: 90 tasks
   total)
5. Add User Story 4 → Test independently → Deploy/Demo (MVP + sending: 110 tasks
   total)
6. Add User Story 5 → Test independently → Deploy/Demo (Full P1 workflow: 140
   tasks total)
7. Add User Story 6 (P2 Dashboard) → Deploy/Demo (P1 + P2: 158 tasks total)
8. Add User Story 7 (P3 Reports) → Deploy/Demo (Complete feature: 172 tasks
   total)
9. Add Polish + Constitution Compliance → Final verification (202 tasks total)

Each story adds value without breaking previous stories.

### Parallel Team Strategy

With multiple developers (3+ team members):

1. **Team completes Setup + Foundational together** (Phases 1-2: 32 tasks)
2. **Once Foundational is done, split by user story**:
   - Developer A: User Story 1 (Create Boxes: 25 tasks)
   - Developer B: User Story 2 (Unassigned Samples: 18 tasks)
   - Developer C: User Story 3 (Labels/Manifests: 15 tasks, depends on US1
     models)
3. **Next iteration**:
   - Developer A: User Story 4 (Send Boxes: 20 tasks, depends on US1)
   - Developer B: User Story 5 (Receive/Reconcile: 30 tasks, depends on US4)
   - Developer C: User Story 6 (Dashboard: 18 tasks, depends on US1)
4. **Final iteration**:
   - All developers: Polish + Constitution Compliance together

Stories complete and integrate independently with minimal merge conflicts
(different packages/components).

---

## Summary

**Total Tasks**: 202

- **Setup**: 6 tasks
- **Foundational**: 26 tasks (BLOCKING)
- **User Story 1 (P1)**: 25 tasks
- **User Story 2 (P1)**: 18 tasks
- **User Story 3 (P1)**: 15 tasks
- **User Story 4 (P1)**: 20 tasks
- **User Story 5 (P1)**: 30 tasks
- **User Story 6 (P2)**: 18 tasks
- **User Story 7 (P3)**: 14 tasks
- **Polish**: 20 tasks
- **Constitution Compliance**: 10 tasks

**Parallel Opportunities**: 89 tasks marked [P] can run in parallel (44% of
total)

**Independent Test Criteria**:

- **US1**: Create box, add/remove samples, mark Ready to Send
- **US2**: View/filter unassigned samples, assign to user, mark lost
- **US3**: Generate box label PDF, generate manifest PDF
- **US4**: Mark box Sent, transmit manifest electronically, verify transmission
  status
- **US5**: Scan box/samples, record non-conformity, complete receiving
- **US6**: View dashboard metrics, filter/search boxes, view shipment details
- **US7**: Generate report, export CSV

**Suggested MVP**: User Stories 1 + 3 (72 tasks) - Box creation + Label
generation

**Constitution Compliance**: All 8 principles addressed with verification tasks
(T193-T202)

---

## Notes

- [P] tasks = different files, no dependencies - can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- TDD workflow: Write tests FIRST, ensure they FAIL, then implement, ensure
  tests PASS
- Commit after each task or logical group (atomic commits)
- Stop at any checkpoint to validate story independently before proceeding
- **Pre-commit mandatory**: `mvn spotless:apply` + `npm run format` BEFORE EVERY
  commit
- **E2E tests**: Run individually during development, review console logs +
  screenshots AFTER EVERY run
- **Lazy loading prevention**: All services compile DTOs within transactions, NO
  entity relationship traversal in controllers
- **Quality system integration**: All shipment non-conformities created in
  existing OpenELIS quality module (org.openelisglobal.qaevent.NcEvent)
