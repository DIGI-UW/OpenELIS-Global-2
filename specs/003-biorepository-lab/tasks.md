# Implementation Tasks: Biorepository Laboratory Module

**Branch**: `feat/003-biorepository-lab` | **Date**: 2026-01-07 **Spec**:
[spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) **UAT Release ETA**:
January 16th, 2026

---

## Task Legend

- `[ ]` - Not started
- `[~]` - In progress
- `[x]` - Completed
- `[!]` - Blocked
- `[S]` - Skipped (with reason)

**Priority**: P1 (Critical) > P2 (High) > P3 (Medium) > P4 (Low)

**Task IDs**: `M{milestone}.{sequence}` (e.g., M0.1, M1.3)

---

## Milestone M0: Biorepository Notebook Template + Workflow Pages

**Branch**: `feat/003-biorepository-lab-m0-notebook` **Dependencies**: None
**Verification**: Notebook appears in dashboard, pages navigate correctly

### M0.1 - Liquibase: Add Biorepository Dictionary Type [P1]

- [ ] **M0.1.1** Create changeset file
      `src/main/resources/liquibase/3.5.x.x/001-biorepository-notebook-template.xml`
- [ ] **M0.1.2** Add "Biorepository Laboratory" entry to
      `notebook_experiment_type` dictionary category
- [ ] **M0.1.3** Add precondition to check dictionary entry doesn't already
      exist
- [ ] **M0.1.4** Add rollback statement for dictionary entry

### M0.2 - Liquibase: Create Notebook Template [P1]

- [ ] **M0.2.1** Insert notebook template record with `is_template=true`,
      `status=ACTIVE`
- [ ] **M0.2.2** Set objective: "Manage biological specimen lifecycle from
      intake through disposal with ISO 20387:2018 compliance"
- [ ] **M0.2.3** Set protocol: "AHRI Biorepository Standard Operating
      Procedures"
- [ ] **M0.2.4** Add precondition to check template doesn't already exist
- [ ] **M0.2.5** Add rollback statement for template

### M0.3 - Liquibase: Create Workflow Pages [P1]

- [ ] **M0.3.1** Create Page 1: "Sample Intake & Registration" (order=1)
  - Instructions: Document arrival condition, verify required documentation,
    generate barcode, assign biosafety classification
- [ ] **M0.3.2** Create Page 2: "Storage Assignment" (order=2)
  - Instructions: Select hierarchical location, scan barcodes for verification,
    confirm placement
- [ ] **M0.3.3** Create Page 3: "Environmental Monitoring" (order=3)
  - Instructions: Record temperature readings, acknowledge excursions, log
    maintenance events
- [ ] **M0.3.4** Create Page 4: "Sample Request & Retrieval" (order=4)
  - Instructions: Process sample requests, approve/reject, generate work orders,
    document retrieval
- [ ] **M0.3.5** Create Page 5: "QC Inspection" (order=5)
  - Instructions: Execute scheduled QC, verify sample presence and condition,
    record discrepancies
- [ ] **M0.3.6** Create Page 6: "Retention & Disposal" (order=6)
  - Instructions: Review expiring samples, obtain authorizations, document
    disposal
- [ ] **M0.3.7** Create Page 7: "Reporting & Audit" (order=7)
  - Instructions: Generate reports, review audit trail, export compliance
    documentation
- [ ] **M0.3.8** Add rollback statements for all pages

### M0.4 - Frontend: Create BiorepositoryWorkflowTab Component [P1]

- [ ] **M0.4.1** Create
      `frontend/src/components/notebook/workflow/BiorepositoryWorkflowTab.js`
- [ ] **M0.4.2** Define `DEFAULT_BIOREPOSITORY_WORKFLOW_PAGES` constant with 7
      pages
- [ ] **M0.4.3** Implement page navigation (next/previous/jump)
- [ ] **M0.4.4** Implement `renderPageComponent()` switch for 7 page types
- [ ] **M0.4.5** Add loading and error states
- [ ] **M0.4.6** Register component in notebook workflow registry

### M0.5 - Frontend: Create Placeholder Page Components [P2]

- [ ] **M0.5.1** Create `frontend/src/components/notebook/pages/biorepository/`
      directory
- [ ] **M0.5.2** Create `BiorepositoryIntakePage.js` (placeholder with props
      interface)
- [ ] **M0.5.3** Create `BiorepositoryStorageAssignmentPage.js`
- [ ] **M0.5.4** Create `BiorepositoryEnvironmentalMonitoringPage.js`
- [ ] **M0.5.5** Create `BiorepositorySampleRequestPage.js`
- [ ] **M0.5.6** Create `BiorepositoryQCInspectionPage.js`
- [ ] **M0.5.7** Create `BiorepositoryRetentionDisposalPage.js`
- [ ] **M0.5.8** Create `BiorepositoryReportingPage.js`
- [ ] **M0.5.9** Create `index.js` with exports

### M0.6 - Internationalization: Add Message Keys [P2]

- [ ] **M0.6.1** Add `biorepository.notebook.title` to
      `frontend/src/languages/en.json`
- [ ] **M0.6.2** Add page title keys: `biorepository.page.intake`,
      `biorepository.page.storage`, etc.
- [ ] **M0.6.3** Add page instruction keys for all 7 pages
- [ ] **M0.6.4** Add French translations to `frontend/src/languages/fr.json`

### M0.7 - Verification [P1]

- [ ] **M0.7.1** Run `mvn clean install -DskipTests` to verify Liquibase
      changeset applies
- [ ] **M0.7.2** Start application and verify notebook appears in Notebook
      Dashboard
- [ ] **M0.7.3** Create new entry from template and verify pages load
- [ ] **M0.7.4** Navigate through all 7 pages and verify navigation works
- [ ] **M0.7.5** Document any issues in PR description

---

## Milestone M1: Database Schema + Core Entities

**Branch**: `feat/003-biorepository-lab-m1-db-entities` **Dependencies**: M0
**Verification**: `mvn clean install` passes, ORM validation test passes for all
6 entities

### M1.0 - Liquibase: Create shipment Table [P1]

- [ ] **M1.0.1** Create changeset `002-create-shipment-table.xml`
- [ ] **M1.0.2** Define sequence `shipment_seq`
- [ ] **M1.0.3** Create table with columns: id, delivery_reference, sender_name,
      sender_organization, receiver_user_id, reception_timestamp,
      packaging_condition, packaging_condition_notes, packaging_photo_path,
      transport_temperature, expected_sample_count, actual_sample_count, status,
      sys_user_id, lastupdated
- [ ] **M1.0.4** Add foreign key constraint (receiver_user_id → sys_user)
- [ ] **M1.0.5** Add index on (reception_timestamp, status)
- [ ] **M1.0.6** Add rollback statements

### M1.1 - Liquibase: Create bio_sample Table [P1]

- [ ] **M1.1.1** Create changeset `003-create-biosample-table.xml`
- [ ] **M1.1.2** Define sequence `bio_sample_seq`
- [ ] **M1.1.3** Create table with all columns per plan.md schema (including
      shipment_id FK)
- [ ] **M1.1.4** Add foreign key constraints (shipment_id, sample_type_id,
      project_id, parent_sample_id, retention_policy_id)
- [ ] **M1.1.5** Add unique constraint on barcode
- [ ] **M1.1.6** Add indexes on commonly queried columns (barcode, status,
      project_id, biosafety_level, shipment_id)
- [ ] **M1.1.7** Add rollback statements

### M1.1b - Liquibase: Create documentation_verification Table [P1]

- [ ] **M1.1b.1** Create changeset
      `004-create-documentation-verification-table.xml`
- [ ] **M1.1b.2** Define sequence `documentation_verification_seq`
- [ ] **M1.1b.3** Create table with columns for 7-point checklist:
      check_sample_identifiers, check_project_linkage, check_ethics_approval,
      check_consent_record, check_mta_documented, check_biosafety_match,
      check_packaging_integrity
- [ ] **M1.1b.4** Add status columns for each check item (VERIFIED, PENDING,
      N_A)
- [ ] **M1.1b.5** Add overall_status column (PENDING, VERIFIED, QUARANTINE)
- [ ] **M1.1b.6** Add N/A justification text fields for conditional items
- [ ] **M1.1b.7** Add foreign key constraint (bio_sample_id,
      verified_by_user_id)
- [ ] **M1.1b.8** Add rollback statements

### M1.2 - Liquibase: Create temperature_reading Table [P1]

- [ ] **M1.2.1** Create changeset `005-create-temperature-reading-table.xml`
- [ ] **M1.2.2** Define sequence `temperature_reading_seq`
- [ ] **M1.2.3** Create table with columns per plan.md schema
- [ ] **M1.2.4** Add foreign key to storage_device
- [ ] **M1.2.5** Add index on (storage_device_id, reading_timestamp)
- [ ] **M1.2.6** Add rollback statements

### M1.3 - Liquibase: Create retention_policy Table [P1]

- [ ] **M1.3.1** Create changeset `006-create-retention-policy-table.xml`
- [ ] **M1.3.2** Define sequence `retention_policy_seq`
- [ ] **M1.3.3** Create table with columns per plan.md schema
- [ ] **M1.3.4** Add check constraint for policy_level values
- [ ] **M1.3.5** Add rollback statements

### M1.4 - Liquibase: Create disposal_record Table [P1]

- [ ] **M1.4.1** Create changeset `007-create-disposal-record-table.xml`
- [ ] **M1.4.2** Define sequence `disposal_record_seq`
- [ ] **M1.4.3** Create table with columns per plan.md schema
- [ ] **M1.4.4** Add foreign keys to bio_sample and sys_user
- [ ] **M1.4.5** Add index on disposal_date
- [ ] **M1.4.6** Add rollback statements

### M1.4b - Backend: Create Shipment Entity [P1]

- [ ] **M1.4b.1** Create package `org.openelisglobal.biorepository.valueholder`
- [ ] **M1.4b.2** Create `Shipment.java` extending `BaseObject<Integer>`
- [ ] **M1.4b.3** Add JPA annotations (@Entity, @Table, @Id, @GeneratedValue)
- [ ] **M1.4b.4** Add all fields: deliveryReference, senderName,
      senderOrganization, receiverUserId, receptionTimestamp,
      packagingCondition, packagingConditionNotes, packagingPhotoPath,
      transportTemperature, expectedSampleCount, actualSampleCount, status
- [ ] **M1.4b.5** Add enum `PackagingCondition` (INTACT, DAMAGED)
- [ ] **M1.4b.6** Add enum `ShipmentStatus` (RECEIVED, PROCESSING, COMPLETED)
- [ ] **M1.4b.7** Add @ManyToOne relationship to SystemUser (receiver)
- [ ] **M1.4b.8** Add audit fields (sysUserId, lastupdated)

### M1.5 - Backend: Create BioSample Entity [P1]

- [ ] **M1.5.1** Create `BioSample.java` extending `BaseObject<Integer>`
- [ ] **M1.5.2** Add JPA annotations (@Entity, @Table, @Id, @GeneratedValue)
- [ ] **M1.5.3** Add all fields with appropriate JPA annotations
- [ ] **M1.5.4** Add `fhirUuid` field with @PrePersist UUID generation
- [ ] **M1.5.5** Add enum `BioSampleStatus` (ACTIVE, QUARANTINE, DISPOSED,
      CHECKED_OUT)
- [ ] **M1.5.6** Add enum `DocumentationStatus` (PENDING, VERIFIED, QUARANTINE)
- [ ] **M1.5.7** Add @ManyToOne relationships (shipment, sampleType, project,
      parentSample, retentionPolicy)
- [ ] **M1.5.8** Add audit fields (sysUserId, lastupdated)

### M1.5b - Backend: Create DocumentationVerification Entity [P1]

- [ ] **M1.5b.1** Create `DocumentationVerification.java` extending
      `BaseObject<Integer>`
- [ ] **M1.5b.2** Add JPA annotations (@Entity, @Table, @Id, @GeneratedValue)
- [ ] **M1.5b.3** Add boolean check fields for 7 verification items
- [ ] **M1.5b.4** Add enum `VerificationItemStatus` (VERIFIED, PENDING, N_A)
- [ ] **M1.5b.5** Add status fields for each verification item
- [ ] **M1.5b.6** Add overall_status field (PENDING, VERIFIED, QUARANTINE)
- [ ] **M1.5b.7** Add N/A justification text fields (naJustificationMta,
      naJustificationConsent)
- [ ] **M1.5b.8** Add @ManyToOne relationship to BioSample
- [ ] **M1.5b.9** Add verifiedByUserId and verifiedTimestamp fields
- [ ] **M1.5b.10** Add audit fields (sysUserId, lastupdated)

### M1.6 - Backend: Create TemperatureReading Entity [P1]

- [ ] **M1.6.1** Create `TemperatureReading.java` extending
      `BaseObject<Integer>`
- [ ] **M1.6.2** Add JPA annotations for table mapping
- [ ] **M1.6.3** Add @ManyToOne relationship to StorageDevice
- [ ] **M1.6.4** Add enum `ExcursionType` (WARNING, CRITICAL)
- [ ] **M1.6.5** Add enum `EntryMethod` (MANUAL, SENSOR)
- [ ] **M1.6.6** Implement excursion detection helper method

### M1.7 - Backend: Create RetentionPolicy Entity [P1]

- [ ] **M1.7.1** Create `RetentionPolicy.java` extending `BaseObject<Integer>`
- [ ] **M1.7.2** Add JPA annotations for table mapping
- [ ] **M1.7.3** Add enum `PolicyLevel` (REGULATORY, ETHICAL, PROJECT,
      SAMPLE_TYPE, DEFAULT)
- [ ] **M1.7.4** Implement `isIndefinite()` helper method

### M1.8 - Backend: Create DisposalRecord Entity [P1]

- [ ] **M1.8.1** Create `DisposalRecord.java` extending `BaseObject<Integer>`
- [ ] **M1.8.2** Add JPA annotations for table mapping
- [ ] **M1.8.3** Add @ManyToOne relationship to BioSample
- [ ] **M1.8.4** Add enum `DisposalMethod` (AUTOCLAVING, INCINERATION,
      CHEMICAL_TREATMENT)
- [ ] **M1.8.5** Implement dual authorization tracking

### M1.9 - Backend: Create DAO Layer [P1]

- [ ] **M1.9.1** Create package `org.openelisglobal.biorepository.dao`
- [ ] **M1.9.2** Create `ShipmentDAO.java` interface extending `BaseDAO`
- [ ] **M1.9.3** Create `ShipmentDAOImpl.java` with query methods
      (findByDeliveryReference, findByStatus, findByDateRange)
- [ ] **M1.9.4** Create `BioSampleDAO.java` interface extending `BaseDAO`
- [ ] **M1.9.5** Create `BioSampleDAOImpl.java` with query methods
      (findByBarcode, findByShipmentId, findByStatus)
- [ ] **M1.9.6** Create `DocumentationVerificationDAO.java` interface
- [ ] **M1.9.7** Create `DocumentationVerificationDAOImpl.java` with query
      methods (findBySampleId, findPending)
- [ ] **M1.9.8** Create `TemperatureReadingDAO.java` interface
- [ ] **M1.9.9** Create `TemperatureReadingDAOImpl.java`
- [ ] **M1.9.10** Create `RetentionPolicyDAO.java` interface
- [ ] **M1.9.11** Create `RetentionPolicyDAOImpl.java`
- [ ] **M1.9.12** Create `DisposalRecordDAO.java` interface
- [ ] **M1.9.13** Create `DisposalRecordDAOImpl.java`

### M1.10 - Backend: Create ORM Validation Test [P1]

- [ ] **M1.10.1** Create package `org.openelisglobal.biorepository`
- [ ] **M1.10.2** Create `HibernateMappingValidationTest.java`
- [ ] **M1.10.3** Test Shipment entity mapping loads correctly
- [ ] **M1.10.4** Test BioSample entity mapping loads correctly
- [ ] **M1.10.5** Test DocumentationVerification entity mapping loads correctly
- [ ] **M1.10.6** Test TemperatureReading entity mapping loads correctly
- [ ] **M1.10.7** Test RetentionPolicy entity mapping loads correctly
- [ ] **M1.10.8** Test DisposalRecord entity mapping loads correctly
- [ ] **M1.10.9** Verify test executes in <5 seconds

### M1.11 - Verification [P1]

- [ ] **M1.11.1** Run `mvn clean install` (full build with tests)
- [ ] **M1.11.2** Verify all Liquibase changesets apply successfully (11
      changesets)
- [ ] **M1.11.3** Verify ORM validation test passes for all 6 entities
- [ ] **M1.11.4** Verify no regressions in existing tests

---

## Milestone M2: Storage Entity Extensions

**Branch**: `feat/003-biorepository-lab-m2-storage-ext` **Dependencies**: M1
**Verification**: Existing storage tests pass, new fields accessible via API

### M2.1 - Liquibase: Extend storage_device Table [P1]

- [ ] **M2.1.1** Create changeset `008-extend-storage-entities.xml`
- [ ] **M2.1.2** Add column `biosafety_zone` VARCHAR(10)
- [ ] **M2.1.3** Add column `qualification_date` DATE
- [ ] **M2.1.4** Add column `last_calibration_date` DATE
- [ ] **M2.1.5** Add column `next_calibration_due` DATE
- [ ] **M2.1.6** Add column `maintenance_notes` TEXT
- [ ] **M2.1.7** Add column `temp_range_min` DECIMAL(5,2)
- [ ] **M2.1.8** Add column `temp_range_max` DECIMAL(5,2)
- [ ] **M2.1.9** Add rollback statements

### M2.2 - Liquibase: Extend storage_room Table [P1]

- [ ] **M2.2.1** Add column `access_control_zone` VARCHAR(50)
- [ ] **M2.2.2** Add column `biosafety_level` VARCHAR(10)
- [ ] **M2.2.3** Add rollback statements

### M2.3 - Backend: Update StorageDevice Entity [P1]

- [ ] **M2.3.1** Add `biosafetyZone` field to `StorageDevice.java`
- [ ] **M2.3.2** Add `qualificationDate` field
- [ ] **M2.3.3** Add `lastCalibrationDate` field
- [ ] **M2.3.4** Add `nextCalibrationDue` field
- [ ] **M2.3.5** Add `maintenanceNotes` field
- [ ] **M2.3.6** Add `tempRangeMin` and `tempRangeMax` fields
- [ ] **M2.3.7** Add getter/setter methods
- [ ] **M2.3.8** Implement `isTemperatureInRange(BigDecimal temp)` helper method

### M2.4 - Backend: Update StorageRoom Entity [P1]

- [ ] **M2.4.1** Add `accessControlZone` field to `StorageRoom.java`
- [ ] **M2.4.2** Add `biosafetyLevel` field
- [ ] **M2.4.3** Add getter/setter methods

### M2.5 - Backend: Update StorageLocationService [P2]

- [ ] **M2.5.1** Update `getDevicesForAPI()` to include new biorepository fields
- [ ] **M2.5.2** Update `getRoomsForAPI()` to include new fields
- [ ] **M2.5.3** Add validation for biosafety zone values

### M2.6 - Backend: Update StorageDeviceForm [P2]

- [ ] **M2.6.1** Add biorepository fields to `StorageDeviceForm.java`
- [ ] **M2.6.2** Add validation annotations
- [ ] **M2.6.3** Update form-to-entity mapping

### M2.7 - Verification [P1]

- [ ] **M2.7.1** Run existing storage module tests
- [ ] **M2.7.2** Verify backward compatibility (existing storage operations
      work)
- [ ] **M2.7.3** Test new fields via REST API
- [ ] **M2.7.4** Verify no regressions

---

## Milestone M3: Temperature Monitoring (Parallel)

**Branch**: `feat/003-biorepository-lab-m3-temp-monitor` **Dependencies**: M1
**Verification**: Can enter temperature, excursions flagged, dashboard shows
overdue devices

### M3.1 - Backend: Create TemperatureMonitoringService [P1]

- [ ] **M3.1.1** Create package `org.openelisglobal.biorepository.service`
- [ ] **M3.1.2** Create `TemperatureMonitoringService.java` interface
- [ ] **M3.1.3** Create `TemperatureMonitoringServiceImpl.java`
- [ ] **M3.1.4** Implement
      `recordReading(Integer deviceId, BigDecimal temperature, String sysUserId)`
- [ ] **M3.1.5** Implement excursion detection logic based on device temp range
- [ ] **M3.1.6** Implement
      `getReadingsForDevice(Integer deviceId, Date startDate, Date endDate)`
- [ ] **M3.1.7** Implement `getUnacknowledgedExcursions()`
- [ ] **M3.1.8** Implement
      `acknowledgeExcursion(Integer readingId, String notes, String sysUserId)`
- [ ] **M3.1.9** Implement `getDevicesOverdueForReading(int hoursThreshold)`
- [ ] **M3.1.10** Add @Transactional annotations at service level

### M3.2 - Backend: Create TemperatureRestController [P1]

- [ ] **M3.2.1** Create `TemperatureRestController.java` in controller package
- [ ] **M3.2.2** Implement `POST /rest/biorepository/temperature`
- [ ] **M3.2.3** Implement
      `GET /rest/biorepository/temperature/device/{deviceId}`
- [ ] **M3.2.4** Implement `GET /rest/biorepository/temperature/excursions`
- [ ] **M3.2.5** Implement
      `POST /rest/biorepository/temperature/{id}/acknowledge`
- [ ] **M3.2.6** Implement `GET /rest/biorepository/temperature/overdue`
- [ ] **M3.2.7** Add input validation and error handling

### M3.3 - Backend: Create TemperatureReadingForm [P2]

- [ ] **M3.3.1** Create `TemperatureReadingForm.java` in form package
- [ ] **M3.3.2** Add validation annotations (@NotNull, @DecimalMin, @DecimalMax)
- [ ] **M3.3.3** Add form-to-entity conversion method

### M3.4 - Backend: Unit Tests [P1]

- [ ] **M3.4.1** Create `TemperatureMonitoringServiceImplTest.java`
- [ ] **M3.4.2** Test `recordReading()` with normal temperature
- [ ] **M3.4.3** Test `recordReading()` with warning excursion (±3°C)
- [ ] **M3.4.4** Test `recordReading()` with critical excursion (±5°C)
- [ ] **M3.4.5** Test `getUnacknowledgedExcursions()` returns correct list
- [ ] **M3.4.6** Test `acknowledgeExcursion()` updates record correctly
- [ ] **M3.4.7** Test `getDevicesOverdueForReading()` filters correctly

### M3.5 - Frontend: Create TemperatureEntryForm Component [P1]

- [ ] **M3.5.1** Create
      `frontend/src/components/biorepository/TemperatureMonitoring/` directory
- [ ] **M3.5.2** Create `TemperatureEntryForm.jsx` with Carbon Form components
- [ ] **M3.5.3** Add device selector dropdown
- [ ] **M3.5.4** Add temperature input with validation
- [ ] **M3.5.5** Implement form submission with API call
- [ ] **M3.5.6** Show success/error notifications
- [ ] **M3.5.7** Add React Intl message keys

### M3.6 - Frontend: Create ExcursionAlert Component [P1]

- [ ] **M3.6.1** Create `ExcursionAlert.jsx` component
- [ ] **M3.6.2** Display excursion type (WARNING/CRITICAL) with appropriate
      styling
- [ ] **M3.6.3** Show expected vs actual temperature
- [ ] **M3.6.4** Add acknowledgment form with corrective action notes
- [ ] **M3.6.5** Implement acknowledgment API call

### M3.7 - Frontend: Create TemperatureLogTable Component [P2]

- [ ] **M3.7.1** Create `TemperatureLogTable.jsx` using Carbon DataTable
- [ ] **M3.7.2** Display historical readings with device, timestamp, value,
      excursion status
- [ ] **M3.7.3** Add date range filter
- [ ] **M3.7.4** Add device filter
- [ ] **M3.7.5** Highlight excursion rows

### M3.8 - Frontend: Create TemperatureTrendChart Component [P3]

- [ ] **M3.8.1** Create `TemperatureTrendChart.jsx`
- [ ] **M3.8.2** Implement line chart visualization
- [ ] **M3.8.3** Show temperature readings over time
- [ ] **M3.8.4** Display acceptable range boundaries
- [ ] **M3.8.5** Highlight excursion points

### M3.9 - Frontend: Dashboard Integration [P2]

- [ ] **M3.9.1** Create `FreezerStatusCard.jsx` for dashboard
- [ ] **M3.9.2** Display device temperature status (normal/warning/critical)
- [ ] **M3.9.3** Show last reading timestamp
- [ ] **M3.9.4** Highlight devices overdue for reading
- [ ] **M3.9.5** Integrate with BiorepositoryEnvironmentalMonitoringPage

### M3.10 - Frontend: Custom Hook [P2]

- [ ] **M3.10.1** Create `useTemperatureReadings.js` hook
- [ ] **M3.10.2** Implement SWR-based data fetching
- [ ] **M3.10.3** Add mutation functions for recording/acknowledging

### M3.11 - E2E Tests [P1]

- [ ] **M3.11.1** Create `frontend/cypress/e2e/temperatureMonitoring.cy.js`
- [ ] **M3.11.2** Test recording normal temperature reading
- [ ] **M3.11.3** Test recording excursion and seeing alert
- [ ] **M3.11.4** Test acknowledging excursion with notes
- [ ] **M3.11.5** Test viewing temperature history

### M3.12 - Verification [P1]

- [ ] **M3.12.1** Run backend unit tests
- [ ] **M3.12.2** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/temperatureMonitoring.cy.js"`
- [ ] **M3.12.3** Manual verification of excursion workflow

---

## Milestone M4: Sample Intake Workflow (4 Sub-stages)

**Branch**: `feat/003-biorepository-lab-m4-sample-intake` **Dependencies**: M1,
M2 **Verification**: Complete intake workflow works - shipment reception,
single/bulk registration, documentation verification, barcode generation

### M4.0 - Backend: Create ShipmentService [P1] (Sub-stage 1a: Shipment Reception)

- [ ] **M4.0.1** Create `ShipmentService.java` interface
- [ ] **M4.0.2** Create `ShipmentServiceImpl.java`
- [ ] **M4.0.3** Implement `logShipmentReception(ShipmentForm form)` - creates
      shipment record
- [ ] **M4.0.4** Implement `getShipment(Integer id)`
- [ ] **M4.0.5** Implement `getShipments(ShipmentFilter filter)` - list with
      date range, status filters
- [ ] **M4.0.6** Implement `updateShipment(Integer id, ShipmentForm form)` - add
      notes, photo
- [ ] **M4.0.7** Implement `getSamplesForShipment(Integer shipmentId)`
- [ ] **M4.0.8** Implement `completeShipment(Integer id)` - mark processing
      complete
- [ ] **M4.0.9** Add @Transactional annotations at service level

### M4.0b - Backend: Create ShipmentRestController [P1]

- [ ] **M4.0b.1** Create `ShipmentRestController.java`
- [ ] **M4.0b.2** Implement `POST /rest/biorepository/shipments` - log new
      shipment
- [ ] **M4.0b.3** Implement `GET /rest/biorepository/shipments` - list with
      filters
- [ ] **M4.0b.4** Implement `GET /rest/biorepository/shipments/{id}` - get
      details
- [ ] **M4.0b.5** Implement `PUT /rest/biorepository/shipments/{id}` - update
- [ ] **M4.0b.6** Implement `GET /rest/biorepository/shipments/{id}/samples` -
      samples in shipment
- [ ] **M4.0b.7** Implement `POST /rest/biorepository/shipments/{id}/complete` -
      complete processing
- [ ] **M4.0b.8** Add input validation and error handling

### M4.0c - Backend: Create ShipmentForm [P2]

- [ ] **M4.0c.1** Create `ShipmentForm.java` with validation annotations
- [ ] **M4.0c.2** Add fields: deliveryReference, senderName, senderOrganization,
      packagingCondition, transportTemperature, expectedSampleCount
- [ ] **M4.0c.3** Add @NotNull for required fields
- [ ] **M4.0c.4** Add form-to-entity conversion method

### M4.0d - Frontend: Create ShipmentReception Components [P1]

- [ ] **M4.0d.1** Create
      `frontend/src/components/biorepository/ShipmentReception/` directory
- [ ] **M4.0d.2** Create `ShipmentReceptionForm.jsx` with Carbon Form components
- [ ] **M4.0d.3** Add delivery reference input
- [ ] **M4.0d.4** Add sender name and organization inputs
- [ ] **M4.0d.5** Add transport temperature input
- [ ] **M4.0d.6** Add expected sample count input
- [ ] **M4.0d.7** Create `PackagingConditionPanel.jsx` with INTACT/DAMAGED radio
      buttons
- [ ] **M4.0d.8** Add photo upload for damaged shipments (mandatory when
      DAMAGED)
- [ ] **M4.0d.9** Create `ShipmentSampleList.jsx` to show samples linked to
      shipment
- [ ] **M4.0d.10** Add React Intl message keys for all shipment UI strings

### M4.1 - Backend: Create BioSampleService [P1] (Sub-stage 1b: Sample Registration)

- [ ] **M4.1.1** Create `BioSampleService.java` interface
- [ ] **M4.1.2** Create `BioSampleServiceImpl.java`
- [ ] **M4.1.3** Implement `registerSample(BioSampleIntakeForm form)` - single
      sample entry
- [ ] **M4.1.4** Implement barcode generation logic (unique, no PII, DataMatrix
      format)
- [ ] **M4.1.5** Implement `getSample(Integer id)`
- [ ] **M4.1.6** Implement `getSampleByBarcode(String barcode)`
- [ ] **M4.1.7** Implement `getSamplesByShipment(Integer shipmentId)`
- [ ] **M4.1.8** Implement `updateSample(Integer id, BioSampleForm form)`
- [ ] **M4.1.9** Implement `quarantineSample(Integer id, String reason)`
- [ ] **M4.1.10** Implement `releaseFromQuarantine(Integer id)`
- [ ] **M4.1.11** Implement `getAuditTrail(Integer sampleId)`

### M4.2 - Backend: Create BioSampleIntakeService [P1]

- [ ] **M4.2.1** Create `BioSampleIntakeService.java` interface
- [ ] **M4.2.2** Create `BioSampleIntakeServiceImpl.java`
- [ ] **M4.2.3** Implement documentation checklist validation
- [ ] **M4.2.4** Implement auto-quarantine for incomplete documentation
- [ ] **M4.2.5** Implement parent-child lineage for aliquots
- [ ] **M4.2.6** Implement retention policy auto-application

### M4.3 - Backend: Create BioSampleRestController [P1]

- [ ] **M4.3.1** Create `BioSampleRestController.java`
- [ ] **M4.3.2** Implement `POST /rest/biorepository/samples` (intake)
- [ ] **M4.3.3** Implement `GET /rest/biorepository/samples` (list with filters)
- [ ] **M4.3.4** Implement `GET /rest/biorepository/samples/{id}`
- [ ] **M4.3.5** Implement `PUT /rest/biorepository/samples/{id}`
- [ ] **M4.3.6** Implement `POST /rest/biorepository/samples/{id}/quarantine`
- [ ] **M4.3.7** Implement
      `POST /rest/biorepository/samples/{id}/release-quarantine`
- [ ] **M4.3.8** Implement `GET /rest/biorepository/samples/{id}/audit-trail`
- [ ] **M4.3.9** Implement
      `POST /rest/biorepository/samples/{id}/barcode/generate`

### M4.4 - Backend: Create Forms [P2]

- [ ] **M4.4.1** Create `BioSampleForm.java` with validation annotations
- [ ] **M4.4.2** Create `BioSampleIntakeForm.java` with intake-specific fields
- [ ] **M4.4.3** Add validation for mandatory metadata fields per spec

### M4.4b - Backend: Create BiorepositoryManifestImportService [P1] (Bulk Manifest Import)

- [ ] **M4.4b.1** Create `BiorepositoryManifestImportService.java` interface
- [ ] **M4.4b.2** Create `BiorepositoryManifestImportServiceImpl.java`
- [ ] **M4.4b.3** Implement
      `previewManifest(MultipartFile file, Integer shipmentId)` - parse CSV,
      validate, return preview
- [ ] **M4.4b.4** Implement
      `createSamplesFromManifest(ManifestImportForm form, Integer shipmentId)` -
      atomic creation
- [ ] **M4.4b.5** Implement CSV column mapping (project_id, sample_type,
      biosafety_level, collection_date, etc.)
- [ ] **M4.4b.6** Implement row-level validation with error collection
- [ ] **M4.4b.7** Return validation errors with row numbers for display
- [ ] **M4.4b.8** Implement rollback on any row failure (atomic transaction)

### M4.4c - Backend: Create BiorepositoryManifestImportController [P1]

- [ ] **M4.4c.1** Create `BiorepositoryManifestImportController.java`
- [ ] **M4.4c.2** Implement `GET /rest/biorepository/manifest/template` -
      download CSV template
- [ ] **M4.4c.3** Implement
      `POST /rest/biorepository/shipments/{shipmentId}/samples/preview-manifest` -
      preview with validation
- [ ] **M4.4c.4** Implement
      `POST /rest/biorepository/shipments/{shipmentId}/samples/create-from-manifest` -
      create samples
- [ ] **M4.4c.5** Handle MultipartFile upload
- [ ] **M4.4c.6** Return structured validation errors in response

### M4.4d - Backend: Create ManifestImportForm [P2]

- [ ] **M4.4d.1** Create `ManifestImportForm.java`
- [ ] **M4.4d.2** Add field for column mapping overrides
- [ ] **M4.4d.3** Add validation annotations

### M4.4e - Backend: Create DocumentationVerificationService [P1] (Sub-stage 1c: Documentation Verification)

- [ ] **M4.4e.1** Create `DocumentationVerificationService.java` interface
- [ ] **M4.4e.2** Create `DocumentationVerificationServiceImpl.java`
- [ ] **M4.4e.3** Implement `initializeVerification(Integer sampleId)` - create
      checklist with auto-verified items
- [ ] **M4.4e.4** Implement auto-verification logic for project linkage validity
- [ ] **M4.4e.5** Implement auto-verification logic for biosafety classification
      match
- [ ] **M4.4e.6** Implement
      `submitVerification(DocumentationVerificationForm form)` - save manual
      checks
- [ ] **M4.4e.7** Implement
      `markItemNA(Integer verificationId, String item, String justification)` -
      N/A with reason
- [ ] **M4.4e.8** Implement
      `quarantineForDocumentation(Integer sampleId, String reason)`
- [ ] **M4.4e.9** Implement `releaseFromQuarantine(Integer sampleId)` - after
      documentation resolved
- [ ] **M4.4e.10** Implement `getVerificationStatus(Integer sampleId)` - get
      checklist state

### M4.4f - Backend: Create DocumentationVerificationRestController [P1]

- [ ] **M4.4f.1** Create `DocumentationVerificationRestController.java`
- [ ] **M4.4f.2** Implement
      `GET /rest/biorepository/samples/{sampleId}/documentation` - get status
- [ ] **M4.4f.3** Implement
      `POST /rest/biorepository/samples/{sampleId}/documentation/verify` -
      submit verification
- [ ] **M4.4f.4** Implement
      `POST /rest/biorepository/samples/{sampleId}/documentation/quarantine` -
      place in quarantine
- [ ] **M4.4f.5** Implement
      `POST /rest/biorepository/samples/{sampleId}/documentation/release` -
      release from quarantine
- [ ] **M4.4f.6** Add input validation and error handling

### M4.4g - Backend: Create DocumentationVerificationForm [P2]

- [ ] **M4.4g.1** Create `DocumentationVerificationForm.java`
- [ ] **M4.4g.2** Add fields for 7 verification items with status
- [ ] **M4.4g.3** Add N/A justification fields for conditional items
- [ ] **M4.4g.4** Add validation annotations

### M4.5 - Backend: PDF Barcode Generation [P1] (Sub-stage 1d: Barcode Generation)

- [ ] **M4.5.1** Create barcode PDF generation service (using iText or similar)
- [ ] **M4.5.2** Support DataMatrix 2D barcode format
- [ ] **M4.5.3** Generate Avery-style label layout
- [ ] **M4.5.4** Include sample ID, barcode, and minimal non-PII metadata

### M4.6 - Backend: FHIR Integration [P3]

- [ ] **M4.6.1** Create `BioSampleFhirTransform.java`
- [ ] **M4.6.2** Implement mapping to FHIR Specimen resource
- [ ] **M4.6.3** Add @PostPersist and @PostUpdate hooks

### M4.7 - Backend: Unit Tests [P1]

- [ ] **M4.7.1** Create `ShipmentServiceImplTest.java`
- [ ] **M4.7.2** Test `logShipmentReception()` creates shipment correctly
- [ ] **M4.7.3** Test `completeShipment()` updates status and sample count
- [ ] **M4.7.4** Create `BioSampleServiceImplTest.java`
- [ ] **M4.7.5** Test `registerSample()` with complete documentation
- [ ] **M4.7.6** Test `registerSample()` with incomplete documentation
      (quarantine)
- [ ] **M4.7.7** Test `registerSample()` links to shipment correctly
- [ ] **M4.7.8** Test barcode uniqueness
- [ ] **M4.7.9** Test parent-child lineage creation
- [ ] **M4.7.10** Create `BioSampleIntakeServiceImplTest.java`
- [ ] **M4.7.11** Test documentation checklist validation
- [ ] **M4.7.12** Create `BiorepositoryManifestImportServiceImplTest.java`
- [ ] **M4.7.13** Test `previewManifest()` with valid CSV
- [ ] **M4.7.14** Test `previewManifest()` with invalid rows (returns errors)
- [ ] **M4.7.15** Test `createSamplesFromManifest()` creates all samples
      atomically
- [ ] **M4.7.16** Test `createSamplesFromManifest()` rollback on failure
- [ ] **M4.7.17** Create `DocumentationVerificationServiceImplTest.java`
- [ ] **M4.7.18** Test `initializeVerification()` auto-verifies checkable items
- [ ] **M4.7.19** Test `submitVerification()` updates checklist
- [ ] **M4.7.20** Test `quarantineForDocumentation()` updates sample status
- [ ] **M4.7.21** Test N/A status requires justification

### M4.8 - Frontend: Create SampleIntakeForm Component [P1]

- [ ] **M4.8.1** Create `frontend/src/components/biorepository/SampleIntake/`
      directory
- [ ] **M4.8.2** Create `SampleIntakeForm.jsx` with Carbon Form components
- [ ] **M4.8.3** Implement all mandatory metadata fields per spec
- [ ] **M4.8.4** Add dropdown for sample type, biosafety level
- [ ] **M4.8.5** Add date pickers for collection/receipt dates
- [ ] **M4.8.6** Add project selector
- [ ] **M4.8.7** Add conditional consent/ethics fields
- [ ] **M4.8.8** Add shipment selector dropdown (link sample to shipment)
- [ ] **M4.8.9** Implement form validation
- [ ] **M4.8.10** Add React Intl message keys

### M4.8b - Frontend: Create Manifest Upload Components [P1]

- [ ] **M4.8b.1** Create `ManifestUploadPanel.jsx` with Carbon FileUploader
- [ ] **M4.8b.2** Add CSV file type validation
- [ ] **M4.8b.3** Create `ManifestTemplateDownload.jsx` with download button
- [ ] **M4.8b.4** Create `ManifestColumnMapper.jsx` for column mapping UI
- [ ] **M4.8b.5** Display detected columns with mapping dropdowns
- [ ] **M4.8b.6** Create `ManifestPreviewTable.jsx` using Carbon DataTable
- [ ] **M4.8b.7** Display parsed rows with validation status
- [ ] **M4.8b.8** Highlight rows with errors in red
- [ ] **M4.8b.9** Display row-level error messages
- [ ] **M4.8b.10** Add "Import All" button (disabled when errors exist)
- [ ] **M4.8b.11** Add React Intl message keys for manifest UI

### M4.8c - Frontend: Create useManifestImport Hook [P2]

- [ ] **M4.8c.1** Create `useManifestImport.js` hook
- [ ] **M4.8c.2** Implement template download function
- [ ] **M4.8c.3** Implement preview upload with validation
- [ ] **M4.8c.4** Implement create-from-manifest function
- [ ] **M4.8c.5** Handle loading and error states

### M4.9 - Frontend: Create DocumentationChecklist Component [P1]

- [ ] **M4.9.1** Create `DocumentationChecklist.jsx`
- [ ] **M4.9.2** Display 7 verification items as checkboxes with labels
- [ ] **M4.9.3** Show auto-verified items (project linkage, biosafety match) as
      pre-checked and disabled
- [ ] **M4.9.4** Show conditional items (consent, MTA) with N/A option
- [ ] **M4.9.5** Add justification text field when N/A is selected
- [ ] **M4.9.6** Show warning for unchecked mandatory items
- [ ] **M4.9.7** Indicate items that will trigger quarantine if unchecked

### M4.9b - Frontend: Create DocumentationVerificationModal [P1]

- [ ] **M4.9b.1** Create `DocumentationVerificationModal.jsx` using Carbon Modal
- [ ] **M4.9b.2** Display current verification status for sample
- [ ] **M4.9b.3** Show auto-verified items with checkmarks
- [ ] **M4.9b.4** Allow manual verification of remaining items
- [ ] **M4.9b.5** Add N/A toggle with justification input
- [ ] **M4.9b.6** Show overall status (PENDING/VERIFIED/QUARANTINE)
- [ ] **M4.9b.7** Add "Verify" button to submit verification
- [ ] **M4.9b.8** Add "Quarantine" button for incomplete documentation
- [ ] **M4.9b.9** Add React Intl message keys

### M4.10 - Frontend: Create BarcodeGenerator Component [P1]

- [ ] **M4.10.1** Create `BarcodeGenerator.jsx`
- [ ] **M4.10.2** Display generated barcode value
- [ ] **M4.10.3** Add "Generate PDF" button
- [ ] **M4.10.4** Implement PDF download via API call

### M4.11 - Frontend: Create QuarantineAlert Component [P2]

- [ ] **M4.11.1** Create `QuarantineAlert.jsx`
- [ ] **M4.11.2** Display quarantine status and reason
- [ ] **M4.11.3** Add "Release from Quarantine" action for authorized users

### M4.12 - Frontend: Custom Hook [P2]

- [ ] **M4.12.1** Create `useBioSamples.js` hook
- [ ] **M4.12.2** Implement SWR-based data fetching
- [ ] **M4.12.3** Add mutation functions for CRUD operations

### M4.13 - Frontend: Integrate with Notebook Page [P1]

- [ ] **M4.13.1** Implement `BiorepositoryIntakePage.js` with SampleIntakeForm
- [ ] **M4.13.2** Connect to notebook entry data
- [ ] **M4.13.3** Save page state to notebook page data

### M4.14 - E2E Tests [P1]

- [ ] **M4.14.1** Create `frontend/cypress/e2e/shipmentReception.cy.js`
- [ ] **M4.14.2** Test logging new shipment reception
- [ ] **M4.14.3** Test adding photo for damaged shipment
- [ ] **M4.14.4** Test completing shipment processing
- [ ] **M4.14.5** Create `frontend/cypress/e2e/bioSampleIntake.cy.js`
- [ ] **M4.14.6** Test registering single sample linked to shipment
- [ ] **M4.14.7** Test registering sample with complete documentation
- [ ] **M4.14.8** Test registering sample with incomplete documentation
      (quarantine)
- [ ] **M4.14.9** Create `frontend/cypress/e2e/manifestImport.cy.js`
- [ ] **M4.14.10** Test downloading manifest template
- [ ] **M4.14.11** Test uploading valid manifest with preview
- [ ] **M4.14.12** Test uploading manifest with validation errors (row
      highlighting)
- [ ] **M4.14.13** Test creating samples from valid manifest
- [ ] **M4.14.14** Create `frontend/cypress/e2e/documentationVerification.cy.js`
- [ ] **M4.14.15** Test documentation verification modal displays auto-verified
      items
- [ ] **M4.14.16** Test manual verification submission
- [ ] **M4.14.17** Test N/A status with justification
- [ ] **M4.14.18** Test quarantine workflow for incomplete documentation
- [ ] **M4.14.19** Test barcode PDF generation
- [ ] **M4.14.20** Test releasing sample from quarantine

### M4.15 - Verification [P1]

- [ ] **M4.15.1** Run backend unit tests for all M4 services
- [ ] **M4.15.2** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/shipmentReception.cy.js"`
- [ ] **M4.15.3** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/bioSampleIntake.cy.js"`
- [ ] **M4.15.4** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/manifestImport.cy.js"`
- [ ] **M4.15.5** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/documentationVerification.cy.js"`
- [ ] **M4.15.6** Manual verification of complete intake workflow (4 sub-stages)

---

## Milestone M5: Retention & Disposal (Parallel)

**Branch**: `feat/003-biorepository-lab-m5-retention` **Dependencies**: M1
**Verification**: Can create policies, receive expiry alerts, complete disposal
with authorization

### M5.1 - Backend: Create RetentionPolicyService [P1]

- [ ] **M5.1.1** Create `RetentionPolicyService.java` interface
- [ ] **M5.1.2** Create `RetentionPolicyServiceImpl.java`
- [ ] **M5.1.3** Implement CRUD operations for retention policies
- [ ] **M5.1.4** Implement `getApplicablePolicies(BioSample sample)`
- [ ] **M5.1.5** Implement `calculateRetentionExpiry(BioSample sample)` (longest
      period)
- [ ] **M5.1.6** Implement `getSamplesExpiringWithin(int days)`

### M5.2 - Backend: Create DisposalService [P1]

- [ ] **M5.2.1** Create `DisposalService.java` interface
- [ ] **M5.2.2** Create `DisposalServiceImpl.java`
- [ ] **M5.2.3** Implement
      `initiateDisposal(Integer sampleId, DisposalForm form)`
- [ ] **M5.2.4** Implement dual authorization for human samples
- [ ] **M5.2.5** Implement
      `authorizeDisposal(Integer recordId, Integer authorizerId)`
- [ ] **M5.2.6** Implement
      `completeDisposal(Integer recordId, String certificateRef)`
- [ ] **M5.2.7** Update BioSample status on disposal completion

### M5.3 - Backend: Create Controllers [P1]

- [ ] **M5.3.1** Create `RetentionPolicyRestController.java`
- [ ] **M5.3.2** Implement `GET/POST /rest/biorepository/retention-policies`
- [ ] **M5.3.3** Implement `GET /rest/biorepository/samples/expiring`
- [ ] **M5.3.4** Create `DisposalRestController.java`
- [ ] **M5.3.5** Implement `POST /rest/biorepository/samples/{id}/dispose`
- [ ] **M5.3.6** Implement `POST /rest/biorepository/disposal/{id}/authorize`
- [ ] **M5.3.7** Implement `GET /rest/biorepository/disposal/records`

### M5.4 - Backend: Create Forms [P2]

- [ ] **M5.4.1** Create `RetentionPolicyForm.java`
- [ ] **M5.4.2** Create `DisposalForm.java`

### M5.5 - Backend: Unit Tests [P1]

- [ ] **M5.5.1** Test retention policy hierarchy (longest period wins)
- [ ] **M5.5.2** Test `getSamplesExpiringWithin()` filters correctly
- [ ] **M5.5.3** Test dual authorization requirement for human samples
- [ ] **M5.5.4** Test disposal record creation

### M5.6 - Frontend: Create RetentionPolicyManager Component [P1]

- [ ] **M5.6.1** Create
      `frontend/src/components/biorepository/RetentionDisposal/` directory
- [ ] **M5.6.2** Create `RetentionPolicyManager.jsx`
- [ ] **M5.6.3** Display existing policies in DataTable
- [ ] **M5.6.4** Add create/edit policy modal
- [ ] **M5.6.5** Add policy level selection (REGULATORY, ETHICAL, etc.)

### M5.7 - Frontend: Create ExpiryAlertPanel Component [P1]

- [ ] **M5.7.1** Create `ExpiryAlertPanel.jsx`
- [ ] **M5.7.2** Display samples approaching expiry (90, 60, 30 days)
- [ ] **M5.7.3** Group by urgency level
- [ ] **M5.7.4** Add action buttons (Extend, Dispose)

### M5.8 - Frontend: Create DisposalWorkflowModal Component [P1]

- [ ] **M5.8.1** Create `DisposalWorkflowModal.jsx`
- [ ] **M5.8.2** Display disposal method selection
- [ ] **M5.8.3** Display reason input
- [ ] **M5.8.4** Show authorization requirements based on sample type
- [ ] **M5.8.5** Track authorization status

### M5.9 - Frontend: Create DisposalRecordView Component [P2]

- [ ] **M5.9.1** Create `DisposalRecordView.jsx`
- [ ] **M5.9.2** Display disposal history in DataTable
- [ ] **M5.9.3** Include all disposal details per spec

### M5.10 - Frontend: Integrate with Notebook Page [P1]

- [ ] **M5.10.1** Implement `BiorepositoryRetentionDisposalPage.js`
- [ ] **M5.10.2** Include policy management and expiry alerts

### M5.11 - E2E Tests [P1]

- [ ] **M5.11.1** Create `frontend/cypress/e2e/retentionDisposal.cy.js`
- [ ] **M5.11.2** Test creating retention policy
- [ ] **M5.11.3** Test viewing expiring samples
- [ ] **M5.11.4** Test initiating and completing disposal

### M5.12 - Verification [P1]

- [ ] **M5.12.1** Run backend unit tests
- [ ] **M5.12.2** Run E2E tests
- [ ] **M5.12.3** Manual verification of disposal workflow

---

## Milestone M6: Sample Request Workflow (Backend)

**Branch**: `feat/003-biorepository-lab-m6-request-workflow` **Dependencies**:
M1, M2 **Verification**: Request state transitions work, validation catches
invalid requests

### M6.1 - Liquibase: Create Request Tables [P1]

- [ ] **M6.1.1** Create changeset `006-create-sample-request-tables.xml`
- [ ] **M6.1.2** Create `sample_request` table per plan.md schema
- [ ] **M6.1.3** Create `sample_request_item` table
- [ ] **M6.1.4** Add sequences and indexes
- [ ] **M6.1.5** Add rollback statements

### M6.2 - Backend: Create Request Entities [P1]

- [ ] **M6.2.1** Create `SampleRequest.java` entity
- [ ] **M6.2.2** Add enum `SampleRequestStatus` (SUBMITTED, MANAGER_REVIEW,
      APPROVED, REJECTED, ASSIGNED_TO_TECHNICIAN, RETRIEVED)
- [ ] **M6.2.3** Create `SampleRequestItem.java` entity
- [ ] **M6.2.4** Add enum `SampleRequestItemStatus` (PENDING, RETRIEVED,
      RETURNED, CONSUMED)
- [ ] **M6.2.5** Add @OneToMany relationship between Request and Items

### M6.3 - Backend: Create SampleRequestDAO [P1]

- [ ] **M6.3.1** Create `SampleRequestDAO.java` interface
- [ ] **M6.3.2** Create `SampleRequestDAOImpl.java`
- [ ] **M6.3.3** Implement query by status, requester, project

### M6.4 - Backend: Create SampleRequestService [P1]

- [ ] **M6.4.1** Create `SampleRequestService.java` interface
- [ ] **M6.4.2** Create `SampleRequestServiceImpl.java`
- [ ] **M6.4.3** Implement `submitRequest(SampleRequestForm form)` with
      auto-validation
- [ ] **M6.4.4** Implement validation: authorization, project linkage, ethics
      approval, consent, availability
- [ ] **M6.4.5** Implement
      `approveRequest(Integer requestId, String notes, Integer reviewerId)`
- [ ] **M6.4.6** Implement
      `rejectRequest(Integer requestId, String reason, Integer reviewerId)`
- [ ] **M6.4.7** Implement
      `assignToTechnician(Integer requestId, Integer technicianId)`
- [ ] **M6.4.8** Implement
      `completeRetrieval(Integer requestId, List<RetrievalConfirmation> confirmations)`
- [ ] **M6.4.9** Implement `returnSample(Integer itemId, String condition)`
- [ ] **M6.4.10** Implement fixed workflow state machine: SUBMITTED →
      MANAGER_REVIEW → APPROVED/REJECTED → ASSIGNED → RETRIEVED

### M6.5 - Backend: Request Number Generation [P2]

- [ ] **M6.5.1** Implement unique request number generation (e.g.,
      REQ-YYYYMMDD-XXXX)

### M6.6 - Backend: Create SampleRequestRestController [P1]

- [ ] **M6.6.1** Create `SampleRequestRestController.java`
- [ ] **M6.6.2** Implement `POST /rest/biorepository/requests`
- [ ] **M6.6.3** Implement `GET /rest/biorepository/requests` (role-filtered)
- [ ] **M6.6.4** Implement `GET /rest/biorepository/requests/{id}`
- [ ] **M6.6.5** Implement `POST /rest/biorepository/requests/{id}/approve`
- [ ] **M6.6.6** Implement `POST /rest/biorepository/requests/{id}/reject`
- [ ] **M6.6.7** Implement `POST /rest/biorepository/requests/{id}/assign`
- [ ] **M6.6.8** Implement
      `POST /rest/biorepository/requests/{id}/complete-retrieval`
- [ ] **M6.6.9** Implement
      `POST /rest/biorepository/requests/items/{itemId}/return`

### M6.7 - Backend: Create SampleRequestForm [P2]

- [ ] **M6.7.1** Create `SampleRequestForm.java`
- [ ] **M6.7.2** Add validation annotations for required fields

### M6.8 - Backend: Unit Tests [P1]

- [ ] **M6.8.1** Create `SampleRequestServiceImplTest.java`
- [ ] **M6.8.2** Test `submitRequest()` with valid data
- [ ] **M6.8.3** Test `submitRequest()` with expired ethics approval (validation
      fail)
- [ ] **M6.8.4** Test state transitions through complete workflow
- [ ] **M6.8.5** Test invalid state transitions are rejected

### M6.9 - Backend: ORM Validation [P1]

- [ ] **M6.9.1** Add SampleRequest and SampleRequestItem to ORM validation test

### M6.10 - Verification [P1]

- [ ] **M6.10.1** Run all backend tests
- [ ] **M6.10.2** Test API endpoints via curl/Postman
- [ ] **M6.10.3** Verify state machine logic works correctly

---

## Milestone M7: Sample Request UI

**Branch**: `feat/003-biorepository-lab-m7-request-ui` **Dependencies**: M6
**Verification**: Full request workflow from submission to retrieval works

### M7.1 - Frontend: Create RequestSubmissionForm Component [P1]

- [ ] **M7.1.1** Create `frontend/src/components/biorepository/SampleRequest/`
      directory
- [ ] **M7.1.2** Create `RequestSubmissionForm.jsx`
- [ ] **M7.1.3** Add project selector
- [ ] **M7.1.4** Add sample search/selection interface
- [ ] **M7.1.5** Add purpose/justification text area
- [ ] **M7.1.6** Add ethics approval reference field
- [ ] **M7.1.7** Display validation results before submission
- [ ] **M7.1.8** Implement form submission

### M7.2 - Frontend: Create RequestApprovalPanel Component [P1]

- [ ] **M7.2.1** Create `RequestApprovalPanel.jsx` (for Biorepository Manager)
- [ ] **M7.2.2** Display pending requests in DataTable
- [ ] **M7.2.3** Show request details on selection
- [ ] **M7.2.4** Add Approve button with notes field
- [ ] **M7.2.5** Add Reject button with reason field
- [ ] **M7.2.6** Implement role-based visibility

### M7.3 - Frontend: Create RetrievalWorkOrder Component [P1]

- [ ] **M7.3.1** Create `RetrievalWorkOrder.jsx` (for Technician)
- [ ] **M7.3.2** Display assigned retrievals
- [ ] **M7.3.3** Show storage coordinates and box position map
- [ ] **M7.3.4** Add barcode scan confirmation
- [ ] **M7.3.5** Update sample status on retrieval

### M7.4 - Frontend: Create RequestStatusTracker Component [P2]

- [ ] **M7.4.1** Create `RequestStatusTracker.jsx`
- [ ] **M7.4.2** Display request status timeline
- [ ] **M7.4.3** Show current status with timestamp
- [ ] **M7.4.4** Display next required action

### M7.5 - Frontend: Create Return Sample Modal [P2]

- [ ] **M7.5.1** Create modal for returning checked-out samples
- [ ] **M7.5.2** Add condition assessment field
- [ ] **M7.5.3** Update sample status on return

### M7.6 - Frontend: Custom Hook [P2]

- [ ] **M7.6.1** Create `useSampleRequests.js` hook
- [ ] **M7.6.2** Implement SWR-based data fetching
- [ ] **M7.6.3** Add mutation functions for workflow actions

### M7.7 - Frontend: Integrate with Notebook Page [P1]

- [ ] **M7.7.1** Implement `BiorepositorySampleRequestPage.js`
- [ ] **M7.7.2** Include request submission (for researchers)
- [ ] **M7.7.3** Include approval panel (for managers)
- [ ] **M7.7.4** Include work orders (for technicians)

### M7.8 - Frontend: Notifications [P2]

- [ ] **M7.8.1** Add toast notifications for status changes
- [ ] **M7.8.2** Display pending request count in dashboard

### M7.9 - E2E Tests [P1]

- [ ] **M7.9.1** Create `frontend/cypress/e2e/sampleRequest.cy.js`
- [ ] **M7.9.2** Test submitting sample request
- [ ] **M7.9.3** Test manager approving request
- [ ] **M7.9.4** Test technician completing retrieval
- [ ] **M7.9.5** Test returning sample

### M7.10 - Verification [P1]

- [ ] **M7.10.1** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/sampleRequest.cy.js"`
- [ ] **M7.10.2** Manual end-to-end workflow test with multiple roles

---

## Milestone M8: QC Inspection Backend (Parallel)

**Branch**: `feat/003-biorepository-lab-m8-qc-backend` **Dependencies**: M1
**Verification**: Can schedule QC, generate sample subsets, create NcEvent on
discrepancy

### M8.1 - Liquibase: Create QC Tables [P1]

- [ ] **M8.1.1** Create changeset `007-create-qc-inspection-tables.xml`
- [ ] **M8.1.2** Create `qc_inspection` table per plan.md schema
- [ ] **M8.1.3** Create `qc_result` table
- [ ] **M8.1.4** Add sequences and indexes
- [ ] **M8.1.5** Add foreign key to nc_event for discrepancies
- [ ] **M8.1.6** Add rollback statements

### M8.2 - Backend: Create QC Entities [P1]

- [ ] **M8.2.1** Create `QCInspection.java` entity
- [ ] **M8.2.2** Add enum `ScheduleType` (SCHEDULED, RANDOM, RISK_BASED,
      EVENT_TRIGGERED)
- [ ] **M8.2.3** Add enum `InspectionStatus` (SCHEDULED, IN_PROGRESS, COMPLETED)
- [ ] **M8.2.4** Create `QCResult.java` entity
- [ ] **M8.2.5** Add enum `QCResultStatus` (PASS, MISSING, MISPLACED,
      DAMAGED_LABEL, PHYSICAL_DAMAGE)

### M8.3 - Backend: Create QCDAO [P1]

- [ ] **M8.3.1** Create `QCInspectionDAO.java` interface
- [ ] **M8.3.2** Create `QCInspectionDAOImpl.java`
- [ ] **M8.3.3** Create `QCResultDAO.java` interface
- [ ] **M8.3.4** Create `QCResultDAOImpl.java`

### M8.4 - Backend: Create QCInspectionService [P1]

- [ ] **M8.4.1** Create `QCInspectionService.java` interface
- [ ] **M8.4.2** Create `QCInspectionServiceImpl.java`
- [ ] **M8.4.3** Implement
      `scheduleInspection(ScheduleType type, Date scheduledDate, Integer storageUnitId)`
- [ ] **M8.4.4** Implement
      `generateRandomSampleSubset(Integer inspectionId, int percentage)`
- [ ] **M8.4.5** Implement
      `startInspection(Integer inspectionId, Integer userId)`
- [ ] **M8.4.6** Implement
      `recordResult(Integer inspectionId, QCResultForm result)`
- [ ] **M8.4.7** Implement
      `completeInspection(Integer inspectionId, String notes)`
- [ ] **M8.4.8** Implement CAPA integration: create NcEvent on discrepancy (per
      DD-004)
- [ ] **M8.4.9** Implement `getSamplesNotVerifiedInYear()` for BR-QC-001
      compliance

### M8.5 - Backend: QC Layout Sheet Generation [P2]

- [ ] **M8.5.1** Implement PDF generation for QC layout sheets
- [ ] **M8.5.2** Include freezer/shelf/rack/box coordinates
- [ ] **M8.5.3** Include position maps with sample IDs

### M8.6 - Backend: Create QCInspectionRestController [P1]

- [ ] **M8.6.1** Create `QCInspectionRestController.java`
- [ ] **M8.6.2** Implement `POST /rest/biorepository/qc/schedule`
- [ ] **M8.6.3** Implement `GET /rest/biorepository/qc/inspections`
- [ ] **M8.6.4** Implement `GET /rest/biorepository/qc/inspections/{id}`
- [ ] **M8.6.5** Implement `POST /rest/biorepository/qc/inspections/{id}/start`
- [ ] **M8.6.6** Implement
      `POST /rest/biorepository/qc/inspections/{id}/results`
- [ ] **M8.6.7** Implement
      `POST /rest/biorepository/qc/inspections/{id}/complete`
- [ ] **M8.6.8** Implement
      `GET /rest/biorepository/qc/inspections/{id}/layout-sheet`

### M8.7 - Backend: Create Forms [P2]

- [ ] **M8.7.1** Create `QCInspectionForm.java`
- [ ] **M8.7.2** Create `QCResultForm.java`

### M8.8 - Backend: Unit Tests [P1]

- [ ] **M8.8.1** Create `QCInspectionServiceImplTest.java`
- [ ] **M8.8.2** Test random sample subset generation
- [ ] **M8.8.3** Test inspection workflow (schedule → start → record → complete)
- [ ] **M8.8.4** Test NcEvent creation on discrepancy
- [ ] **M8.8.5** Test result aggregation (pass count, discrepancy count)

### M8.9 - Backend: ORM Validation [P1]

- [ ] **M8.9.1** Add QCInspection and QCResult to ORM validation test

### M8.10 - Verification [P1]

- [ ] **M8.10.1** Run all backend tests
- [ ] **M8.10.2** Test API endpoints
- [ ] **M8.10.3** Verify NcEvent integration

---

## Milestone M9: QC Inspection UI

**Branch**: `feat/003-biorepository-lab-m9-qc-ui` **Dependencies**: M8
**Verification**: Full QC workflow from scheduling through completion works

### M9.1 - Frontend: Create QCSchedulingPanel Component [P1]

- [ ] **M9.1.1** Create `frontend/src/components/biorepository/QCInspection/`
      directory
- [ ] **M9.1.2** Create `QCSchedulingPanel.jsx`
- [ ] **M9.1.3** Display scheduled inspections calendar view
- [ ] **M9.1.4** Add "Schedule New Inspection" modal
- [ ] **M9.1.5** Allow selection of schedule type and target

### M9.2 - Frontend: Create QCLayoutSheet Component [P1]

- [ ] **M9.2.1** Create `QCLayoutSheet.jsx`
- [ ] **M9.2.2** Display printable QC checklist
- [ ] **M9.2.3** Show box position maps
- [ ] **M9.2.4** Add PDF download button

### M9.3 - Frontend: Create QCExecutionForm Component [P1]

- [ ] **M9.3.1** Create `QCExecutionForm.jsx`
- [ ] **M9.3.2** Display sample list with verification checkboxes
- [ ] **M9.3.3** Add barcode scan integration
- [ ] **M9.3.4** Allow status selection (PASS or discrepancy types)
- [ ] **M9.3.5** Track verification progress

### M9.4 - Frontend: Create DiscrepancyModal Component [P1]

- [ ] **M9.4.1** Create `DiscrepancyModal.jsx`
- [ ] **M9.4.2** Allow discrepancy type selection
- [ ] **M9.4.3** Add notes field
- [ ] **M9.4.4** Show expected vs actual location
- [ ] **M9.4.5** Display CAPA ticket creation confirmation

### M9.5 - Frontend: QC Dashboard Panel [P2]

- [ ] **M9.5.1** Create QC summary panel for dashboard
- [ ] **M9.5.2** Display verification coverage rates
- [ ] **M9.5.3** Show discrepancy trends
- [ ] **M9.5.4** Display upcoming inspections

### M9.6 - Frontend: Custom Hook [P2]

- [ ] **M9.6.1** Create `useQCInspections.js` hook
- [ ] **M9.6.2** Implement SWR-based data fetching
- [ ] **M9.6.3** Add mutation functions

### M9.7 - Frontend: Integrate with Notebook Page [P1]

- [ ] **M9.7.1** Implement `BiorepositoryQCInspectionPage.js`
- [ ] **M9.7.2** Include scheduling, execution, and results view

### M9.8 - E2E Tests [P1]

- [ ] **M9.8.1** Create `frontend/cypress/e2e/qcInspection.cy.js`
- [ ] **M9.8.2** Test scheduling QC inspection
- [ ] **M9.8.3** Test executing QC verification
- [ ] **M9.8.4** Test recording discrepancy
- [ ] **M9.8.5** Test completing inspection

### M9.9 - Verification [P1]

- [ ] **M9.9.1** Run E2E tests:
      `npm run cy:run -- --spec "cypress/e2e/qcInspection.cy.js"`
- [ ] **M9.9.2** Manual end-to-end QC workflow test

---

## Milestone M10: Reporting & Dashboard

**Branch**: `feat/003-biorepository-lab-m10-reporting` **Dependencies**: M4, M6,
M8 **Verification**: All reports generate correctly, dashboard displays
real-time metrics

### M10.1 - Backend: Create BiorepositoryReportService [P1]

- [ ] **M10.1.1** Create `BiorepositoryReportService.java` interface
- [ ] **M10.1.2** Create `BiorepositoryReportServiceImpl.java`
- [ ] **M10.1.3** Implement `getInventorySummary(filters)`
- [ ] **M10.1.4** Implement `getIntakeLog(dateRange, filters)`
- [ ] **M10.1.5** Implement `getRetrievalLog(dateRange, filters)`
- [ ] **M10.1.6** Implement `getDisposalLog(dateRange, filters)`
- [ ] **M10.1.7** Implement `getQCSummary(dateRange, filters)`
- [ ] **M10.1.8** Implement `getTemperatureLog(dateRange, filters)`
- [ ] **M10.1.9** Implement `getChainOfCustody(sampleId)`
- [ ] **M10.1.10** Implement `getDashboardMetrics()`

### M10.2 - Backend: Create BiorepositoryReportRestController [P1]

- [ ] **M10.2.1** Create `BiorepositoryReportRestController.java`
- [ ] **M10.2.2** Implement all report endpoints per plan.md
- [ ] **M10.2.3** Implement `GET /rest/biorepository/dashboard/metrics`
- [ ] **M10.2.4** Add role-based access filtering (researchers see own projects
      only)

### M10.3 - Backend: Export Formats [P2]

- [ ] **M10.3.1** Implement CSV export
- [ ] **M10.3.2** Implement Excel export
- [ ] **M10.3.3** Implement PDF export
- [ ] **M10.3.4** Implement JSON export

### M10.4 - Frontend: Create ReportGenerator Component [P1]

- [ ] **M10.4.1** Create `frontend/src/components/biorepository/Reporting/`
      directory
- [ ] **M10.4.2** Create `ReportGenerator.jsx`
- [ ] **M10.4.3** Add report type selector
- [ ] **M10.4.4** Add date range picker
- [ ] **M10.4.5** Add filter controls per report type
- [ ] **M10.4.6** Add export format selector
- [ ] **M10.4.7** Display report preview
- [ ] **M10.4.8** Implement export download

### M10.5 - Frontend: Create AuditTrailViewer Component [P1]

- [ ] **M10.5.1** Create `AuditTrailViewer.jsx`
- [ ] **M10.5.2** Display audit events in timeline format
- [ ] **M10.5.3** Show user, timestamp, action, before/after values
- [ ] **M10.5.4** Add filtering by event type, date, user

### M10.6 - Frontend: Create BiorepositoryDashboard Component [P1]

- [ ] **M10.6.1** Create
      `frontend/src/components/biorepository/BiorepositoryDashboard/` directory
- [ ] **M10.6.2** Create `BiorepositoryDashboard.jsx`
- [ ] **M10.6.3** Add FreezerStatusCard panel (from M3)
- [ ] **M10.6.4** Add AlertsPanel for active alerts
- [ ] **M10.6.5** Add PendingRequestsPanel
- [ ] **M10.6.6** Add QC summary panel
- [ ] **M10.6.7** Add capacity utilization charts
- [ ] **M10.6.8** Implement auto-refresh

### M10.7 - Frontend: Integrate with Notebook Page [P1]

- [ ] **M10.7.1** Implement `BiorepositoryReportingPage.js`
- [ ] **M10.7.2** Include report generator and audit viewer

### M10.8 - E2E Tests [P1]

- [ ] **M10.8.1** Create `frontend/cypress/e2e/biorepositoryDashboard.cy.js`
- [ ] **M10.8.2** Test dashboard loads with all panels
- [ ] **M10.8.3** Test generating inventory report
- [ ] **M10.8.4** Test exporting report to CSV

### M10.9 - Verification [P1]

- [ ] **M10.9.1** Run E2E tests
- [ ] **M10.9.2** Verify all report types generate correctly
- [ ] **M10.9.3** Verify dashboard displays real-time data

---

## Milestone M11: Integration & Polish

**Branch**: `feat/003-biorepository-lab-m11-integration` **Dependencies**: All
previous milestones **Verification**: All tests pass, >80% backend
coverage, >70% frontend coverage

### M11.1 - Full E2E Test Suite [P1]

- [ ] **M11.1.1** Run all E2E tests together
- [ ] **M11.1.2** Fix any flaky tests
- [ ] **M11.1.3** Ensure no test isolation issues

### M11.2 - Constitution Compliance Verification [P1]

- [ ] **M11.2.1** Verify Carbon Design System usage (no Bootstrap/Tailwind)
- [ ] **M11.2.2** Verify React Intl for all UI strings
- [ ] **M11.2.3** Verify 5-layer architecture adherence
- [ ] **M11.2.4** Verify @Transactional only in services
- [ ] **M11.2.5** Verify Liquibase for all schema changes
- [ ] **M11.2.6** Verify RBAC implementation
- [ ] **M11.2.7** Verify audit trail completeness

### M11.3 - Internationalization Audit [P2]

- [ ] **M11.3.1** Scan for hardcoded strings in frontend
- [ ] **M11.3.2** Verify all message keys have en.json entries
- [ ] **M11.3.3** Verify all message keys have fr.json entries
- [ ] **M11.3.4** Test UI in French locale

### M11.4 - Code Coverage Reports [P1]

- [ ] **M11.4.1** Run JaCoCo for backend coverage
- [ ] **M11.4.2** Verify >80% backend coverage
- [ ] **M11.4.3** Run Jest coverage for frontend
- [ ] **M11.4.4** Verify >70% frontend coverage
- [ ] **M11.4.5** Add tests for uncovered critical paths

### M11.5 - Performance Testing [P2]

- [ ] **M11.5.1** Test sample registration response time <3 seconds
- [ ] **M11.5.2** Test barcode scan validation <1 second
- [ ] **M11.5.3** Test QC layout generation (1000 samples) <10 seconds
- [ ] **M11.5.4** Test dashboard refresh <5 seconds
- [ ] **M11.5.5** Test 50 concurrent users

### M11.6 - Security Audit [P1]

- [ ] **M11.6.1** Verify RBAC enforcement on all endpoints
- [ ] **M11.6.2** Verify input validation on all forms
- [ ] **M11.6.3** Test for XSS vulnerabilities
- [ ] **M11.6.4** Test for SQL injection
- [ ] **M11.6.5** Verify audit logging for security events

### M11.7 - Documentation Updates [P3]

- [ ] **M11.7.1** Update API documentation
- [ ] **M11.7.2** Update user guide for biorepository module
- [ ] **M11.7.3** Add troubleshooting section

### M11.8 - Final Verification [P1]

- [ ] **M11.8.1** Complete end-to-end user acceptance testing
- [ ] **M11.8.2** Verify all 8 user stories work correctly
- [ ] **M11.8.3** Verify success criteria from spec.md
- [ ] **M11.8.4** Create release notes

---

## Task Summary

| Milestone | Total Tasks | P1 (Critical) | P2 (High) | P3 (Medium) |
| --------- | ----------- | ------------- | --------- | ----------- |
| M0        | 24          | 16            | 7         | 1           |
| M1        | 56          | 48            | 8         | 0           |
| M2        | 18          | 14            | 4         | 0           |
| M3        | 37          | 22            | 11        | 4           |
| M4        | 115         | 88            | 23        | 4           |
| M5        | 30          | 20            | 8         | 2           |
| M6        | 28          | 24            | 4         | 0           |
| M7        | 24          | 16            | 7         | 1           |
| M8        | 29          | 24            | 5         | 0           |
| M9        | 23          | 16            | 6         | 1           |
| M10       | 27          | 19            | 7         | 1           |
| M11       | 25          | 16            | 6         | 3           |
| **Total** | **436**     | **323**       | **96**    | **17**      |

**Note**: M1 and M4 task counts increased to include Shipment,
DocumentationVerification entities, manifest import functionality, and 4
sub-stages of the intake workflow.

---

## Execution Order

**Critical Path** (sequential):

```
M0 → M1 → M2 → M4 → M6 → M7 → M10 → M11
```

**Parallel Tracks** (can run concurrently after M1):

```
Track A: M3 (Temperature Monitoring)
Track B: M5 (Retention & Disposal)
Track C: M8 → M9 (QC Inspection)
```

**Recommended Execution**:

1. Start with M0 (Notebook template)
2. Complete M1 (Database + Entities)
3. Start M2, M3, M5, M8 in parallel
4. Complete M4 after M2
5. Complete M6 after M2
6. Complete M7 after M6
7. Complete M9 after M8
8. Complete M10 after M4, M6, M8
9. Complete M11 after all others

---

## Notes

- All tasks follow TDD workflow: write test first, then implementation
- Each milestone should have its own PR
- Run `mvn spotless:apply` before every commit
- Run `cd frontend && npm run format` before every commit
- Use `-DskipTests -Dmaven.test.skip=true` for quick builds during development
- Run individual E2E tests during development, not full suite
