# Feature Specification: Patient Merge (Full Stack)

**Feature Branch**: `feat/008-m3-rest-controller` (backend complete),
`feat/008-frontend` (pending) **Created**: 2025-12-05 **Updated**: 2025-12-11
**Status**: Backend Complete ✅ (44/44 tests) | Frontend Pending **Scope**:
Full-stack patient merge functionality including:

- Backend: Database schema, services, DAO, REST API endpoints, FHIR integration
- Frontend: UI components, patient comparison, validation workflow, merge
  execution

## Clarifications

### Session 2025-12-05

- Q: What should happen when a merge operation times out or exceeds the
  30-second performance target? → A: Allow operation to continue beyond 30
  seconds until completion (no timeout)

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Database Schema Support for Patient Merge (Priority: P1)

As a system administrator, I need the database to track merged patient records
and maintain data integrity so that all clinical data is preserved and properly
linked after a merge operation.

**Why this priority**: Foundation for all merge functionality - nothing else can
work without proper database structure.

**Independent Test**: Can be fully tested by executing Liquibase migrations,
verifying table structures, indexes, and foreign key constraints exist, and
validates that merged patient data is stored correctly.

**Acceptance Scenarios**:

1. **Given** database is at current version, **When** Liquibase migrations
   execute, **Then** `patient_merge_audit` table is created with all required
   columns and indexes
2. **Given** `patient` table exists, **When** migrations execute, **Then**
   `merged_into_patient_id`, `is_merged`, and `merge_date` columns are added
3. **Given** a patient record is marked as merged, **When** querying related
   tables, **Then** all foreign key relationships remain valid

---

### User Story 2 - Patient Merge Service Layer (Priority: P1)

As a backend developer, I need a service layer that orchestrates the complete
merge process so that all data consolidation happens transactionally and safely.

**Why this priority**: Core business logic that ensures data integrity and
proper merge execution.

**Independent Test**: Can be fully tested with unit and integration tests that
validate merge logic, transaction management, and rollback on failure without
requiring UI.

**Acceptance Scenarios**:

1. **Given** two valid patient IDs, **When** merge service executes, **Then**
   all related data (samples, orders, results, identities) is consolidated to
   primary patient within a single transaction
2. **Given** merge service execution fails mid-process, **When** error occurs,
   **Then** entire transaction rolls back and no partial data changes persist
3. **Given** two patients with conflicting data, **When** merge executes,
   **Then** primary patient demographics are preserved and secondary patient
   identifiers are added
4. **Given** patient has 500+ test results, **When** merge executes, **Then**
   operation completes within 30 seconds using batch updates
5. **Given** merge completes successfully, **When** querying merged patient,
   **Then** `is_merged=true`, `merged_into_patient_id` points to primary, and
   all audit entries are created

---

### User Story 3 - Merge Validation Logic (Priority: P1)

As a system, I need to validate merge eligibility before execution so that
invalid merge operations are prevented.

**Why this priority**: Prevents data corruption and ensures business rules are
enforced.

**Independent Test**: Can be tested independently via unit tests that validate
all business rule checks without database interaction.

**Acceptance Scenarios**:

1. **Given** user lacks Global Administrator permission, **When** attempting
   merge validation, **Then** validation fails with permission error
2. **Given** same patient ID provided twice, **When** validation executes,
   **Then** validation fails with "cannot merge patient with itself" error
3. **Given** one patient is already merged, **When** validation executes,
   **Then** validation fails with "already merged" error
4. **Given** circular merge reference detected, **When** validation executes,
   **Then** validation fails with "circular reference" error
5. **Given** two valid, unmergeable patients, **When** validation executes,
   **Then** validation passes and returns data summary

---

### User Story 4 - RESTful Merge API Endpoints (Priority: P2)

As a frontend developer, I need RESTful API endpoints to retrieve merge details,
validate merges, and execute merges so that the UI can orchestrate the merge
workflow.

**Why this priority**: Enables frontend integration after core service layer is
complete.

**Independent Test**: Can be tested independently via API integration tests
(Postman/RestAssured) that verify endpoint contracts, authentication,
authorization, and response formats.

**Acceptance Scenarios**:

1. **Given** valid patient ID and Global Admin token, **When** calling
   `GET /rest/patient/merge/details/{id}`, **Then** receive patient
   demographics, data summary, identifiers, and potential conflicts
2. **Given** two patient IDs and valid token, **When** calling
   `POST /rest/patient/merge/validate`, **Then** receive validation result with
   errors/warnings and consolidated data counts
3. **Given** valid merge request with confirmation, **When** calling
   `POST /rest/patient/merge/execute`, **Then** merge executes successfully and
   returns merge audit ID and success message
4. **Given** user without Global Admin permission, **When** calling any merge
   endpoint, **Then** receive 403 Forbidden response
5. **Given** invalid patient ID, **When** calling merge endpoints, **Then**
   receive 404 Not Found with descriptive error message

---

### User Story 5 - FHIR R4 Compliance and Synchronization (Priority: P2)

As a system integrator, I need FHIR R4 Patient resources to reflect merge
operations using proper link relationships so that external systems understand
merged patient relationships.

**Why this priority**: Required for interoperability and FHIR compliance, but
depends on core merge logic completing first.

**Independent Test**: Can be tested independently by validating FHIR resource
transformations and link relationships without full merge workflow.

**Acceptance Scenarios**:

1. **Given** patients are merged, **When** FHIR Patient resource is retrieved
   for primary patient, **Then** resource includes `link` array with
   `type: "replaces"` pointing to merged patient
2. **Given** patients are merged, **When** FHIR Patient resource is retrieved
   for merged patient, **Then** `active: false` and `link` array with
   `type: "replaced-by"` pointing to primary patient
3. **Given** merged patient had unique identifiers, **When** FHIR resource is
   retrieved for primary patient, **Then** all identifiers from both patients
   are present with appropriate `use` and `period` values
4. **Given** merge completes, **When** FHIR resources referencing merged patient
   (ServiceRequest, Specimen, Observation) are queried, **Then** `subject`
   references are updated to primary patient
5. **Given** FHIR sync fails after database merge succeeds, **When** error is
   detected, **Then** critical error is logged for manual intervention and user
   is notified

---

### User Story 6 - Comprehensive Audit Trail (Priority: P2)

As a compliance officer, I need a complete audit trail of all merge operations
so that I can track who performed merges, when, why, and what data was affected.

**Why this priority**: Required for compliance and troubleshooting, but can be
implemented after core merge logic works.

**Independent Test**: Can be tested independently by verifying audit entries are
created correctly during merge operations.

**Acceptance Scenarios**:

1. **Given** merge operation starts, **When** service begins execution, **Then**
   audit entry is created with user, timestamp, both patient IDs, and "merge
   initiated" status
2. **Given** merge updates each table, **When** updates complete, **Then**
   separate audit entry is created for each table modification
3. **Given** merge completes, **When** operation finishes, **Then**
   `patient_merge_audit` record is created with full data summary (orders count,
   results count, samples count) stored as JSONB
4. **Given** user views historical audit entries, **When** audit entry
   references merged patient, **Then** indicator shows original patient ID with
   notation "(merged → PAT-XXXXX)"
5. **Given** merge operation fails, **When** rollback occurs, **Then** audit
   entry captures failure reason and rollback timestamp

---

### User Story 7 - Permission Enforcement and Security (Priority: P1)

As a security administrator, I need merge operations restricted to Global
Administrators only so that unauthorized users cannot corrupt patient data.

**Why this priority**: Critical security requirement that must be enforced from
the start.

**Independent Test**: Can be tested independently via unit tests that verify
permission checks at service and controller layers.

**Acceptance Scenarios**:

1. **Given** user without Global Administrator role, **When** attempting to call
   merge service methods, **Then** PermissionError is thrown
2. **Given** API endpoint receives request, **When** user lacks Global
   Administrator permission, **Then** 403 Forbidden response is returned before
   any service logic executes
3. **Given** unauthorized merge attempt, **When** request is blocked, **Then**
   security audit log captures user ID, IP address, timestamp, and attempted
   action
4. **Given** user has Global Administrator permission, **When** calling merge
   endpoints, **Then** permission check passes and merge logic proceeds
5. **Given** merge operation is in progress, **When** CSRF token is missing or
   invalid, **Then** request is rejected with 403 Forbidden

---

### Edge Cases

- What happens when a patient with 500+ test results is merged? (Performance
  target: should complete within 30 seconds, but operation continues until
  successful completion if longer)
- What happens if merge operation exceeds 30-second performance target?
  (Operation continues until completion - no hard timeout enforced)
- How does the system handle concurrent merge attempts on the same patient?
  (Lock mechanism prevents duplicate processing)
- What happens if FHIR synchronization fails after database merge succeeds?
  (Critical error logged, manual intervention required)
- How are circular merge references prevented? (Pre-merge validation checks
  merged_into_patient_id chains)
- What happens if a patient has active lab orders during merge? (Orders are
  consolidated to primary patient, remain active)
- How are audit entries for the merged patient displayed historically? (Show
  original patient ID with merged indicator)
- What happens when trying to merge a patient that's already been merged?
  (Validation fails, error returned to user)
- How does rollback work if merge fails mid-transaction? (Database transaction
  rollback, all changes reverted)

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST create `patient_merge_audit` table with fields: id,
  primary_patient_id, merged_patient_id, merge_date, performed_by_user_id,
  reason, data_summary (JSONB)
- **FR-002**: System MUST add columns to `patient` table:
  merged_into_patient_id, is_merged (boolean), merge_date
- **FR-003**: System MUST create indexes on patient_merge_audit
  (primary_patient_id, merged_patient_id, merge_date) and patient (is_merged,
  merged_into_patient_id)
- **FR-004**: System MUST update all foreign key references in related tables
  (sample_human, patient_identity, patient_contact, external_patient_id,
  patient_relations, electronic_order) to point to primary patient
- **FR-005**: System MUST preserve all identifiers from both patients in the
  primary patient record
- **FR-006**: System MUST execute entire merge within a single database
  transaction with rollback on failure
- **FR-007**: System MUST validate merge eligibility: user has Global
  Administrator permission, patients are different, neither patient is already
  merged, no circular references
- **FR-008**: System MUST calculate and return data summary before merge: total
  orders, active orders, total results, total samples, documents count,
  identifiers list
- **FR-009**: System MUST detect conflicting demographic data (phone, email,
  address) and preserve primary patient values
- **FR-010**: System MUST create audit trail entries for: merge initiation, each
  table update, FHIR updates, merge completion
- **FR-011**: System MUST expose RESTful API endpoint
  `GET /rest/patient/merge/details/{patientId}` returning patient demographics,
  data summary, identifiers, conflicts
- **FR-012**: System MUST expose RESTful API endpoint
  `POST /rest/patient/merge/validate` accepting two patient IDs and returning
  validation results with data summary
- **FR-013**: System MUST expose RESTful API endpoint
  `POST /rest/patient/merge/execute` accepting patient IDs, primary selection,
  reason, confirmation flag
- **FR-014**: System MUST enforce Global Administrator permission on all merge
  API endpoints, returning 403 Forbidden if unauthorized
- **FR-015**: System MUST implement CSRF protection on merge execution endpoint
- **FR-016**: System MUST update FHIR Patient resource for primary patient with
  `link` array containing `type: "replaces"` reference to merged patient
- **FR-017**: System MUST update FHIR Patient resource for merged patient with
  `active: false` and `link` array containing `type: "replaced-by"` reference to
  primary patient
- **FR-018**: System MUST update all FHIR resources (ServiceRequest, Specimen,
  Observation, DiagnosticReport) to reference primary patient instead of merged
  patient
- **FR-019**: System MUST add historical identifiers from merged patient to
  primary patient's FHIR resource with `use: "old"` and appropriate `period.end`
- **FR-020**: System MUST use batch UPDATE statements for performance when
  consolidating data
- **FR-021**: System SHOULD complete merge operations involving 500+ results
  within 30 seconds (performance target, not hard timeout - operation continues
  until completion)
- **FR-022**: System MUST log unauthorized merge attempts with user ID, IP
  address, timestamp, and action
- **FR-023**: System MUST log critical errors when FHIR synchronization fails
  after successful database merge

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - includes only backend-relevant
principles:_

- **CR-001**: Backend MUST follow 5-layer architecture: Valueholder (Patient,
  PatientMergeAudit) → DAO (PatientDAO, PatientMergeAuditDAO) → Service
  (PatientMergeService) → Controller (PatientMergeController) → Form
  (PatientMergeForm)
- **CR-002**: Valueholders MUST use JPA/Hibernate annotations (NO XML mapping
  files - legacy exempt until refactored)
- **CR-003**: Database changes MUST use Liquibase changesets with proper
  rollback definitions (NO direct DDL/DML)
- **CR-004**: External data integration MUST use FHIR R4 Patient resource with
  `link` relationships per specification
- **CR-005**: Security MUST implement RBAC with Global Administrator role check
  at both service and controller layers
- **CR-006**: Audit trail MUST record sys_user_id and lastupdated timestamp for
  all merge operations
- **CR-007**: Input validation MUST occur at controller layer before service
  invocation
- **CR-008**: Service layer methods MUST be annotated with @Transactional for
  proper transaction management
- **CR-009**: Services MUST compile all data within transaction to prevent
  LazyInitializationException
- **CR-010**: Tests MUST include: unit tests (service validation logic,
  permission checks), integration tests (full merge workflow, rollback
  scenarios, API endpoints), and achieve >70% code coverage

### Key Entities

- **PatientMergeAudit**: Audit record of merge operation containing
  primary_patient_id, merged_patient_id, merge_date, performed_by_user_id,
  reason, data_summary (JSONB with counts and details)
- **Patient**: Enhanced with merge tracking fields: merged_into_patient_id
  (foreign key to self), is_merged (boolean flag), merge_date (timestamp when
  merge occurred)
- **PatientMergeRequest**: Form/DTO containing patient1_id, patient2_id,
  primary_patient_id, reason, confirmation flag for API requests
- **PatientMergeValidation**: Result object containing validation status, error
  messages, warnings, consolidated data summary
- **PatientMergeDataSummary**: Contains counts of orders (total, active),
  results, samples, documents, audit entries, identifiers from both patients,
  conflicting demographic fields

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Merge operations involving up to 500 test results complete within
  30 seconds (performance target - operations exceeding this continue until
  successful completion)
- **SC-002**: Database transaction rollback successfully reverts all changes
  when merge fails mid-process (0% partial merge states)
- **SC-003**: 100% of unauthorized merge attempts are blocked and logged before
  any service logic executes
- **SC-004**: All merge operations create complete audit trail with user,
  timestamp, reason, and data summary
- **SC-005**: FHIR Patient resources correctly reflect merge relationships with
  proper `link` arrays and `active` status for 100% of merges
- **SC-006**: Backend unit and integration tests achieve >70% code coverage for
  merge-related code
- **SC-007**: All validation rules (permission, patient eligibility, circular
  references) correctly prevent invalid merges (0% false positives)
- **SC-008**: API endpoints return appropriate HTTP status codes (200, 400, 403,
  404, 500) for all scenarios
- **SC-009**: Foreign key updates across all related tables (sample_human,
  patient_identity, patient_contact, etc.) complete within the merge transaction
  with 100% success rate
- **SC-010**: System preserves 100% of identifiers from both patients in
  consolidated primary patient record

## Dependencies

- OpenELIS Global 3.0 database schema
- Existing Patient entity and DAO layer
- FHIR integration module
- Global Administrator permission system
- Liquibase migration framework
- Transaction management infrastructure (Spring @Transactional)
- Audit logging system

## Assumptions

- Database supports nested transactions and proper rollback mechanisms
- FHIR integration module provides APIs for updating Patient resources and
  related clinical resources
- Global Administrator permission already exists and is enforced in the system
- Existing patient search and selection functionality will be reused (not part
  of backend scope)
- Real-time notification mechanism (WebSocket/polling) will be implemented in
  frontend scope
- Maximum of 500 test results per patient is a reasonable upper bound for
  performance testing
- Audit trail infrastructure exists and supports additional merge-related
  entries

## Out of Scope (Frontend Responsibility)

- Patient selection UI and search modals
- Primary patient selection interface
- Confirmation dialog and user warnings
- Real-time notification display to users
- Success/error message display
- Navigation and routing
- UI state management
- Internationalization of UI strings
