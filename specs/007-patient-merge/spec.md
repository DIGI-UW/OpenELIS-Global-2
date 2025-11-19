# Feature Specification: Patient Merge

**Feature Branch**: `007-patient-merge`
**Created**: 2025-11-19
**Status**: Draft
**Input**: User description: "Patient merge functionality allowing Global Administrators to consolidate duplicate patient records into a single authoritative patient identity, maintaining complete data history, FHIR R4 compliance using Patient link functionality, and comprehensive audit trail"

## Clarifications

### Session 2025-11-19

**Clarification Analysis**: Based on analysis of the Figma Make prototype (file: MhvQpxldgwlluftt3zo4oi/components/MergePatient.tsx) and the specification, the following clarifications have been resolved.

#### Question 1: Patient Search and Selection Workflow

The spec states "System MUST open the existing patient search modal when 'Select Patient' button is clicked" (FR-006), but doesn't specify constraints on which patients can be selected.

**Options:**

**A) Allow selection of any patient including inactive or EQA samples**
- The search modal shows all patients in the system regardless of status
- Validation only prevents selecting the same patient twice
- Edge case handling for EQA/inactive patients happens during merge validation
- Pro: Maximum flexibility, simpler initial selection
- Con: May allow meaningless merges (e.g., merging EQA samples)

**B) Filter patient search to exclude EQA samples and already-merged patients**
- The search modal applies filters to only show mergeable patients
- Prevents selection of patients with `is_merged = TRUE` or `is_eqa_sample = TRUE`
- Cleaner UX by preventing invalid selections upfront
- Pro: Better UX, prevents confusion
- Con: Requires search modal modification, may hide patients user expects to see

**C) Show all patients but display visual indicators for non-mergeable ones**
- Search modal shows all patients with badges/icons for EQA, merged, or inactive status
- Allows selection but provides clear warning during validation step
- Pro: Transparency, user can see why a patient isn't mergeable
- Con: More complex UI, validation still needed

Which approach should the patient selection workflow use?

---

#### Question 2: Conflicting Demographic Data Resolution

The spec states "Primary patient demographics will be used" for conflicts, but doesn't specify what happens to the non-primary patient's conflicting data.

**Options:**

**A) Discard non-primary demographics completely**
- Only primary patient's phone, email, address are retained
- Non-primary patient's contact information is lost (not stored anywhere)
- Simple, clean data model
- Pro: No data duplication, clear single source of truth
- Con: Potential data loss if wrong patient selected as primary

**B) Store non-primary demographics in audit/history table**
- Create a `patient_merge_history` table storing the non-primary patient's demographics at time of merge
- Allows recovery if wrong patient was chosen as primary
- Viewable by Global Administrators in patient detail view
- Pro: No data loss, supports error correction
- Con: Additional table, more complex queries

**C) Store conflicting demographics as alternate contact methods**
- Add non-primary phone as "Alternate Phone" with note "From merged patient PAT-XXXXX"
- Add non-primary email as alternate email
- Add non-primary address as previous address
- Pro: Preserves all contact information, may be useful clinically
- Con: Creates duplicate contact records, unclear which is current

Which approach should be used for handling conflicting demographic data?

---

#### Question 3: Inactive Patient Access and Display

The spec states merged patients become inactive, but doesn't fully specify the user experience when accessing them directly.

**Options:**

**A) Redirect immediately to primary patient**
- When a user navigates to a merged patient URL, automatically redirect to primary patient
- Display brief toast notification "This patient was merged into PAT-XXXXX"
- No ability to view the old merged patient record
- Pro: Simplest UX, prevents confusion
- Con: User might not understand why they were redirected

**B) Show interstitial page explaining the merge**
- Display a full page with: "This patient record (PAT-XXXXX) was merged into PAT-YYYYY on [DATE] by [USER]"
- Show merge reason if provided
- Provide "View Current Patient Record" button
- Pro: Clear communication, educational for users
- Con: Extra click required, slower workflow

**C) Show read-only view of merged patient with prominent banner**
- Display the merged patient's data as it was at time of merge (read-only)
- Large banner at top: "This patient was merged. View current record: PAT-YYYYY"
- Useful for understanding what data came from which original patient
- Pro: Transparency, helps with troubleshooting
- Con: May confuse users, requires additional view implementation

Which approach should be used for displaying inactive merged patients?

---

#### Question 4: Real-Time Notification Mechanism

The spec requires real-time notifications via WebSocket (FR-054) but doesn't specify fallback behavior.

**Options:**

**A) WebSocket only - no fallback**
- Implement WebSocket notification system
- If WebSocket connection fails, user gets no notification
- User discovers merge only when they try to save/submit data
- Pro: Simpler implementation, single code path
- Con: Unreliable in poor network conditions

**B) WebSocket with polling fallback**
- Primary: WebSocket notifications (instant)
- Fallback: If WebSocket unavailable, poll every 30 seconds for patient changes
- Display same notification banner when polling detects change
- Pro: Reliable in all network conditions
- Con: More complex implementation, additional server load

**C) WebSocket with page-level validation on actions**
- WebSocket for instant notifications when available
- Before any save/submit action, validate patient hasn't been merged
- Show modal if merge detected: "This patient was merged while you were working. Please reload."
- Pro: Guaranteed detection at critical moments
- Con: Notification might come late (when user tries to save)

Which notification mechanism should be implemented?

---

#### Question 5: Merge Operation Transaction and Rollback

The spec requires transaction-based merging with rollback on failure, but doesn't specify granularity of error handling.

**Options:**

**A) All-or-nothing atomic transaction**
- Entire merge (database + FHIR) in one transaction
- If FHIR sync fails, entire database transaction rolls back
- System remains in consistent state but merge fails completely
- Pro: Perfect data consistency, no partial merges
- Con: FHIR failures prevent database merge even if DB operation succeeded

**B) Two-phase commit: Database first, then FHIR**
- Phase 1: Complete database merge in transaction (commit)
- Phase 2: Attempt FHIR sync as separate operation
- If FHIR fails, database merge remains, alert created for manual FHIR reconciliation
- Pro: Database merge succeeds even with FHIR issues
- Con: Temporary inconsistency between database and FHIR

**C) Best-effort FHIR with async retry**
- Complete database merge in transaction (commit)
- Attempt FHIR sync synchronously
- If FHIR sync fails, create async job to retry periodically
- Log warning but don't block user, show "Merge complete, FHIR sync pending"
- Pro: Best UX, automatic recovery
- Con: Most complex, requires job queue infrastructure

Which transaction and error handling approach should be used?

---

### Resolved Clarifications

- **Q1: Patient Search Workflow** → **A: Reuse existing patient search from patient entry (Modified Option A)**
  - **Finding**: Figma Make shows inline search forms as a prototype demonstration
  - **Finding**: OpenELIS already has patient search implemented as part of patient entry workflow
  - **Finding**: No filtering for EQA/merged patients shown in Make - validation only prevents selecting same patient twice
  - **Decision**: Reuse the existing patient search mechanism from patient entry. Allow selection of any patient with validation at merge time. This maintains consistency with existing OpenELIS workflows and avoids duplicate implementation.
  - **Impact**: FR-006 correctly references "existing patient search modal"; no changes needed. Validation prevents same patient selection twice.

- **Q2: Conflicting Demographic Data Resolution** → **A: Store non-primary demographics in audit/history table (Option B)**
  - **Finding**: Figma Make displays conflicts to user during confirmation step but doesn't show where non-primary data goes
  - **Finding**: Clinical systems require audit trail and potential recovery of all patient data
  - **Decision**: Create `patient_merge_history` table storing complete snapshot of non-primary patient's demographics at time of merge. Viewable by Global Administrators for audit and error correction.
  - **Impact**: Add new database table and query requirements; update data model section

- **Q3: Inactive Patient Access and Display** → **A: Show interstitial page explaining the merge (Option B)**
  - **Finding**: Figma Make consistently uses clear warnings and explanations throughout workflow
  - **Finding**: User education about merge status is important for understanding redirect
  - **Decision**: Display full interstitial page showing: "This patient record (PAT-XXXXX) was merged into PAT-YYYYY on [DATE] by [USER]", merge reason, and "View Current Patient Record" button
  - **Impact**: Add new view/page for merged patient display; clearer than immediate redirect

- **Q4: Real-Time Notification Mechanism** → **A: WebSocket with page-level validation on actions (Option C)**
  - **Finding**: Figma Make is static prototype and doesn't show notification mechanism
  - **Finding**: Clinical environments may have unreliable WebSocket support
  - **Decision**: Implement WebSocket notifications when available, but also validate patient hasn't been merged before any save/submit action. Shows modal if merge detected: "This patient was merged while you were working. Please reload."
  - **Impact**: Dual-layer notification system; guaranteed detection at critical moments

- **Q5: Merge Operation Transaction and Rollback** → **A: Two-phase commit: Database first, then FHIR (Option B)**
  - **Finding**: Figma Make cannot demonstrate backend transaction behavior
  - **Finding**: FHIR sync failures should not block critical database merge operation
  - **Decision**: Phase 1 - Complete database merge in transaction (commit); Phase 2 - Attempt FHIR sync as separate operation. If FHIR fails, database merge remains successful, alert created for manual FHIR reconciliation by technical staff.
  - **Impact**: Separate transaction boundaries; add alert/queue system for failed FHIR sync

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Merge Duplicate Patient Records (Priority: P1)

A Global Administrator discovers two patient records that represent the same person due to duplicate registration. They need to consolidate all clinical and administrative data into a single authoritative patient record while preserving complete data history and maintaining FHIR compliance.

**Why this priority**: Duplicate patient records compromise data integrity, fragment clinical history, and can lead to medical errors. The ability to merge duplicates is essential for maintaining accurate patient records and ensuring complete clinical context for diagnosis and treatment decisions.

**Independent Test**: Can be fully tested by creating two patient records with associated orders, results, and samples, executing the merge workflow through all steps, and verifying that all data is consolidated under the primary patient with proper FHIR linking and complete audit trail.

**Acceptance Scenarios**:

1. **Given** I am a Global Administrator on the Merge Patient screen, **When** I click "Select Patient" buttons, **Then** the existing patient search modal opens and allows me to search and select patients
2. **Given** I have selected two different patients, **When** I proceed to the next step, **Then** I see the primary patient selection screen with full demographic and clinical summaries for both patients in expandable accordions
3. **Given** I am on the primary patient selection screen, **When** I select one patient as primary via radio button, **Then** the system highlights which demographics will be preserved and shows data consolidation summary
4. **Given** I proceed to the confirmation screen, **When** I review the merge summary, **Then** I see total counts of orders, results, samples, documents, and audit entries being consolidated, plus any conflicting demographic information
5. **Given** I am on the confirmation screen, **When** I check the confirmation checkbox and provide a reason for merge, **Then** the "Confirm Merge" button becomes enabled
6. **Given** I click "Confirm Merge", **When** the merge operation completes successfully, **Then** I see a success message with the primary patient ID and options to view the patient or merge another pair
7. **Given** the merge is complete, **When** I view the primary patient record, **Then** I see all consolidated data (orders, results, samples) and both patient identifiers preserved
8. **Given** the merge is complete, **When** I attempt to access the merged patient record directly, **Then** the system shows it is inactive and provides a link to the primary patient record

---

### User Story 2 - Real-Time Notification for Active Sessions (Priority: P1)

A laboratory technician is actively viewing a patient record or entering results when a Global Administrator merges that patient with another record. The technician needs immediate notification that the displayed data has changed so they can reload to see current information.

**Why this priority**: Without real-time notification, users could continue working with stale patient data after a merge, potentially entering results or orders against an inactive patient record. This is P1 because it prevents data integrity issues and user confusion.

**Independent Test**: Can be fully tested by having one user view a patient record while another Global Administrator merges that patient, verifying that the viewing user receives an immediate notification banner with reload option.

**Acceptance Scenarios**:

1. **Given** I am viewing a patient record or results entry screen, **When** a Global Administrator merges this patient with another, **Then** I immediately see a warning notification banner at the top of the page
2. **Given** the merge notification appears, **When** I read the message, **Then** it clearly states "This patient record has been merged" and provides a "Reload Page" button
3. **Given** I click the "Reload Page" button, **When** the page refreshes, **Then** I am redirected to the primary patient record with current consolidated data
4. **Given** I was viewing the merged (non-primary) patient, **When** I reload, **Then** I see the primary patient record instead with all merged data visible

---

### User Story 3 - View Audit Trail with Merge Indicators (Priority: P2)

A laboratory supervisor is reviewing historical audit entries for quality assurance or compliance purposes. When viewing entries related to a patient who was later merged, they need clear indication of the merge relationship to maintain proper context.

**Why this priority**: Audit trail clarity is important for compliance and investigation, but the core merge functionality (P1) can operate without enhanced audit display. This is P2 because it improves usability and compliance reporting without blocking basic merge operations.

**Independent Test**: Can be fully tested by merging two patients with existing audit entries, then viewing the audit trail and verifying that entries for the merged patient show an indicator linking to the primary patient.

**Acceptance Scenarios**:

1. **Given** I am viewing the audit trail, **When** I see an entry for a patient who was later merged, **Then** the patient ID is displayed with a merge indicator icon (ⓘ)
2. **Given** I see a merge indicator on an audit entry, **When** I hover over the icon, **Then** a tooltip appears stating "This patient record was merged into [PRIMARY-ID] on [DATE]"
3. **Given** I click on a merged patient ID with indicator, **When** the link activates, **Then** I navigate to the primary patient record
4. **Given** I am viewing audit entries for the primary patient after a merge, **When** I look at the timeline, **Then** I see entries from both original patients in chronological order with their original patient IDs preserved

---

### Edge Cases

- **What happens when attempting to merge a patient with itself?** The system validates that the two selected patients must have different IDs and displays an error message "Cannot merge a patient with itself" if the same patient is selected twice.

- **How does the system handle merging patients who have already been merged?** The system checks the `is_merged` flag on both patients and displays an error "One or both patients have already been merged" if either patient has `is_merged = TRUE`, preventing cascading merges.

- **What happens if two Global Administrators attempt to merge the same patients simultaneously?** The system uses database row locking (`SELECT ... FOR UPDATE`) on both patient records at the start of the merge transaction, causing the second administrator's attempt to wait until the first completes, then displaying "A merge operation is already in progress for one of these patients."

- **How does the system handle patients with conflicting demographic information?** The system preserves the demographics of the selected primary patient and displays all conflicts (phone, email, address) in the confirmation screen with a note that "Primary patient demographics will be used." All identifiers from both patients are preserved.

- **What happens to active lab orders and pending results during a merge?** All active orders, pending results, and in-progress samples are reassigned to the primary patient via foreign key updates. Users with these items open receive real-time notifications to reload their screens.

- **How does the system handle performance with patients who have 500+ results?** For merges involving patients with combined total >200 results, the system displays a performance warning "This merge involves a large amount of data (500+ results). The operation may take up to 30 seconds to complete" and shows a progress indicator during the operation.

- **What happens if the FHIR synchronization fails after a successful database merge?** The system logs a critical error, notifies the Global Administrator with "Patients were merged but FHIR synchronization encountered an error. Please contact support", and creates an alert for technical staff to manually reconcile FHIR resources.

- **How does the system handle external system integrations after a merge?** If FHIR messaging is configured for external systems, the system sends a Patient merge notification following FHIR R4 specifications with both the `replaces` and `replaced-by` link relationships to inform downstream systems.

- **What happens to patient relationships (e.g., family links, guarantors) during a merge?** The system updates both `patient_id` and `related_patient_id` columns in the `patient_relations` table to point to the primary patient, preserving all relationship mappings with audit trail logging.

- **How does the system prevent circular merge references?** The validation logic checks that neither patient already has a `merged_into_patient_id` value pointing to the other patient, preventing circular references and ensuring a clean merge chain.

## Requirements _(mandatory)_

### Functional Requirements

#### Access Control and Navigation

- **FR-001**: System MUST restrict patient merge functionality to users with Global Administrator permission only
- **FR-002**: System MUST display "Merge Patient" menu item under Main Menu → Patient only for users with Global Administrator permission
- **FR-003**: System MUST hide "Merge Patient" menu item for users without Global Administrator permission
- **FR-004**: System MUST verify Global Administrator permission on all merge-related API endpoints regardless of frontend checks

#### Patient Selection

- **FR-005**: System MUST provide two "Select Patient" buttons on the initial merge screen for selecting first and second patients
- **FR-006**: System MUST open the existing patient search modal when "Select Patient" button is clicked
- **FR-007**: System MUST display selected patient information in a Carbon Tile with patient ID, name, DOB, gender, national ID, phone, address, and summary counts (active orders, total results, total samples)
- **FR-008**: System MUST prevent selection of the same patient twice with validation error "Cannot merge a patient with itself"
- **FR-009**: System MUST provide a "Remove X" button on each selected patient card to deselect and choose a different patient
- **FR-010**: System MUST disable the "Next Step" button until both patients are selected

#### Primary Patient Selection

- **FR-011**: System MUST display both patients side-by-side with radio button selection for choosing the primary patient
- **FR-012**: System MUST use Carbon Accordion components to show expandable sections for Demographics, Clinical Summary, and Identifiers for each patient
- **FR-013**: System MUST calculate and display clinical summary including total orders, total results, total samples, active orders, and date range of clinical data
- **FR-014**: System MUST display a warning notification "All future data will be linked to the primary patient. The non-selected patient will be marked as merged and inactive"
- **FR-015**: System MUST disable the "Next Step" button until a primary patient is selected via radio button

#### Merge Confirmation and Validation

- **FR-016**: System MUST display a confirmation screen with complete merge summary including primary patient ID, merging-from patient ID, and data consolidation counts
- **FR-017**: System MUST calculate and display data consolidation summary showing total orders (with active count), test results, samples, documents, and audit entries
- **FR-018**: System MUST list all identifiers being preserved from both patients including patient IDs, national IDs, external IDs, and passport numbers
- **FR-019**: System MUST detect and display conflicting demographic information including differing phone numbers, emails, and addresses with note that primary patient demographics will be used
- **FR-020**: System MUST require a mandatory text input for "Reason for Merge" with minimum 10 characters
- **FR-021**: System MUST require a confirmation checkbox "I understand this action cannot be undone and have verified the correct patients are being merged"
- **FR-022**: System MUST disable the "Confirm Merge" button until both reason is provided and confirmation checkbox is checked
- **FR-023**: System MUST display critical warning "This action CANNOT be undone. All data will be permanently consolidated" in prominent InlineNotification

#### Pre-Merge Validation

- **FR-024**: System MUST validate that both patients exist and are not already merged (is_merged = FALSE) before allowing merge
- **FR-025**: System MUST prevent merges if either patient has `is_merged = TRUE` with error "One or both patients have already been merged"
- **FR-026**: System MUST validate that neither patient has a `merged_into_patient_id` pointing to the other to prevent circular references
- **FR-027**: System MUST check for concurrent merge operations on the same patients and display error "A merge operation is already in progress for one of these patients"

#### Merge Execution

- **FR-028**: System MUST execute the entire merge operation within a single database transaction to ensure atomicity
- **FR-029**: System MUST acquire database row locks (SELECT ... FOR UPDATE) on both patient records at the start of the transaction
- **FR-030**: System MUST create an entry in `patient_merge_audit` table with primary patient ID, merged patient ID, merge date, performing user ID, reason, and data summary in JSONB format
- **FR-031**: System MUST update the merged patient record with `is_merged = TRUE`, `merged_into_patient_id = primary patient ID`, and `merge_date = CURRENT_TIMESTAMP`
- **FR-032**: System MUST update `sample_human` table setting `patient_id = primary patient ID` for all samples where `patient_id = merged patient ID`
- **FR-033**: System MUST update `patient_identity` table setting `patient_id = primary patient ID` for all identity records where `patient_id = merged patient ID`
- **FR-034**: System MUST update `patient_contact` table setting `patient_id = primary patient ID` for all contact records where `patient_id = merged patient ID`
- **FR-035**: System MUST update `external_patient_id` table setting `patient_id = primary patient ID` for all external IDs where `patient_id = merged patient ID`
- **FR-036**: System MUST update `patient_relations` table setting both `patient_id = primary patient ID` and `related_patient_id = primary patient ID` where either column references the merged patient
- **FR-037**: System MUST update `electronic_order` table setting `patient_id = primary patient ID` where `patient_id = merged patient ID`
- **FR-038**: System MUST preserve all historical audit entries with their original patient references (no updates to audit tables)
- **FR-039**: System MUST add all identifiers from the merged patient to the primary patient as historical identifiers with `period.end` timestamp

#### FHIR R4 Compliance

- **FR-040**: System MUST update the primary patient's FHIR Patient resource by adding a `link` element with `type = "replaces"` referencing the merged patient
- **FR-041**: System MUST add the merged patient's ID to the primary patient's FHIR identifier array with `use = "old"` and `period.end` set to merge timestamp
- **FR-042**: System MUST update the merged patient's FHIR Patient resource by setting `active = false` and adding a `link` element with `type = "replaced-by"` referencing the primary patient
- **FR-043**: System MUST update all FHIR ServiceRequest resources referencing the merged patient by changing `subject.reference` from `Patient/[MERGED-ID]` to `Patient/[PRIMARY-ID]`
- **FR-044**: System MUST update all FHIR Specimen resources referencing the merged patient by changing `subject.reference` to the primary patient
- **FR-045**: System MUST update all FHIR Observation resources (lab results) referencing the merged patient by changing `subject.reference` to the primary patient
- **FR-046**: System MUST update all FHIR DiagnosticReport resources referencing the merged patient by changing `subject.reference` to the primary patient
- **FR-047**: System MUST preserve all merged patient's identifiers in the primary patient's FHIR resource with appropriate `period.end` timestamps

#### Audit Trail and Logging

- **FR-048**: System MUST create audit trail entries for merge initiation including user ID, timestamp, both patient IDs, and IP address
- **FR-049**: System MUST create audit trail entries for each database table updated during merge with before/after patient ID values
- **FR-050**: System MUST create audit trail entries for all FHIR resource updates including resource type, resource ID, and changed fields
- **FR-051**: System MUST create audit trail entry for merge completion with success/failure status and total duration
- **FR-052**: System MUST log any errors or warnings that occur during merge execution with full stack traces for troubleshooting
- **FR-053**: System MUST store the merge reason provided by the Global Administrator in both the audit trail and `patient_merge_audit` table

#### Real-Time Notifications

- **FR-054**: System MUST broadcast a real-time notification via WebSocket to all active user sessions when a patient merge completes
- **FR-055**: System MUST include both the primary patient ID and merged patient ID in the notification payload
- **FR-056**: System MUST display a warning InlineNotification at the top of any page showing patient data when that patient is involved in a merge
- **FR-057**: System MUST provide a "Reload Page" button in the merge notification banner
- **FR-058**: System MUST redirect users viewing the merged patient record to the primary patient record after reload
- **FR-059**: System MUST update any open patient lists, work queues, or search results to reflect the merged patient's new inactive status

#### Post-Merge Display

- **FR-060**: System MUST display a success message after merge completion showing primary patient ID and merged patient ID with inactive status
- **FR-061**: System MUST provide "View Patient" button to navigate to the primary patient record
- **FR-062**: System MUST provide "Merge Another Patient" button to return to patient selection for a new merge operation
- **FR-063**: System MUST show merge indicators (ⓘ icon) next to patient IDs in audit trail entries when the patient was later merged
- **FR-064**: System MUST display tooltip on merge indicators showing "This patient record was merged into [PRIMARY-ID] on [DATE]"
- **FR-065**: System MUST allow clicking on merged patient IDs in audit trails to navigate to the primary patient record
- **FR-066**: System MUST display both original patient IDs in the primary patient's detail view under an "Historical Identifiers" or "Merged From" section

#### Performance Requirements

- **FR-067**: System MUST complete merge operations for patients with combined total <200 results within 5 seconds
- **FR-068**: System MUST display a performance warning for merges involving patients with combined total >200 results stating "This merge involves a large amount of data. The operation may take up to 30 seconds"
- **FR-069**: System MUST display a progress indicator (Carbon Loading component) during merge execution
- **FR-070**: System MUST use batch UPDATE statements with WHERE clauses rather than individual row updates for performance

#### Error Handling

- **FR-071**: System MUST rollback the entire transaction if any step of the merge fails, leaving both patient records unchanged
- **FR-072**: System MUST display user-friendly error messages for common failures including insufficient permission, patient not found, already merged, and validation errors
- **FR-073**: System MUST log critical errors if database merge succeeds but FHIR synchronization fails, with notification to Global Administrator and technical support
- **FR-074**: System MUST display error "You do not have permission to merge patients. Global Administrator permission is required" for unauthorized access attempts
- **FR-075**: System MUST display error "One or both patients could not be found" if either patient ID is invalid
- **FR-076**: System MUST display error "One or both patients have already been merged" if either patient has `is_merged = TRUE`

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO custom CSS frameworks. Patient merge interface must use: Carbon Button, Carbon Tile, Carbon StructuredList, Carbon RadioButtonGroup, Carbon Accordion, Carbon InlineNotification, Carbon TextArea, Carbon Checkbox, Carbon Modal (reuse existing patient search modal), Carbon Loading for progress
- **CR-002**: All UI strings MUST be internationalized via React Intl (no hardcoded text) - includes all labels, error messages, button text, warnings, tooltips, and confirmation messages
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form):
  - Valueholders: `PatientMergeAudit` entity with JPA/Hibernate annotations, extend `Patient` entity with merge-related fields
  - DAOs: `PatientMergeAuditDAO`, extend existing `PatientDAO` with merge-specific queries
  - Services: `PatientMergeService` with @Transactional annotation, `PatientMergeNotificationService` for WebSocket broadcasts
  - Controllers: `PatientMergeController` (NO @Transactional in controllers)
  - Forms: `PatientMergeForm` for data binding with validation annotations
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML) - includes new `patient_merge_audit` table, ALTER TABLE for `patient` with new columns (`merged_into_patient_id`, `is_merged`, `merge_date`), and indexes for merge-related queries
- **CR-005**: External data integration MUST use FHIR R4 for patient merge notifications to external systems using Patient link functionality with `replaces` and `replaced-by` link types
- **CR-006**: Configuration-driven variation for country-specific requirements (NO code branching) - use configuration for enabling/disabling real-time notifications, FHIR sync, and external system messaging
- **CR-007**: Security MUST implement RBAC for Global Administrator role, audit trail with sys_user_id + lastupdated for all merge entities, input validation for patient IDs and merge reason, CSRF protection on merge execution endpoint
- **CR-008**: Tests MUST be included with >70% coverage goal:
  - Unit tests: Validation logic, permission checking, data summary calculation, FHIR resource transformation
  - Integration tests: Complete merge workflow, transaction rollback on failure, concurrent merge handling, FHIR synchronization
  - E2E tests (Cypress): Full merge journey from patient selection through confirmation, real-time notification display and reload, audit trail merge indicators

### UI/UX Requirements

- **UI-001**: "Merge Patient" menu item must only appear under Main Menu → Patient for users with Global Administrator permission
- **UI-002**: "Select Patient" buttons must use existing patient search modal for consistency with sample entry workflow
- **UI-003**: Selected patient cards must use Carbon Tile component with StructuredList showing patient ID, demographics, and data summary counts
- **UI-004**: Primary patient selection must use Carbon RadioButtonGroup with clear visual distinction between the two patients
- **UI-005**: Patient demographic and clinical data must be organized in Carbon Accordion components with collapsed-by-default state
- **UI-006**: Critical warning message "This action CANNOT be undone" must use Carbon InlineNotification with type="warning" and prominent placement
- **UI-007**: Merge confirmation screen must clearly highlight which patient will remain active (primary) and which will become inactive (merged)
- **UI-008**: Conflicting demographic information must be displayed in a clearly formatted list showing both values with indication that primary patient value will be used
- **UI-009**: "Confirm Merge" button must remain disabled until both required fields (reason, confirmation checkbox) are completed
- **UI-010**: Progress indicator during merge execution must use Carbon Loading component with appropriate size for the modal/page context
- **UI-011**: Success confirmation must use Carbon InlineNotification with type="success" and include both patient IDs for reference
- **UI-012**: Real-time merge notification must appear as Carbon InlineNotification with type="warning" fixed to top of page content area
- **UI-013**: Audit trail merge indicators must use info icon (ⓘ) with Carbon Tooltip showing merge details on hover
- **UI-014**: All error messages must use Carbon InlineNotification with type="error" and user-friendly language

### Key Entities

- **Patient (Extended)**: Existing entity extended with merge-related fields:
  - `merged_into_patient_id` (BIGINT, nullable, foreign key to patient.id) - References the primary patient if this patient was merged
  - `is_merged` (BOOLEAN, default FALSE) - Flag indicating if patient has been merged and is inactive
  - `merge_date` (TIMESTAMP, nullable) - When the merge occurred
  - Indexes: `idx_patient_merged` on (is_merged, merged_into_patient_id)

- **PatientMergeAudit**: New entity for comprehensive merge audit trail:
  - `id` (BIGSERIAL, primary key)
  - `primary_patient_id` (BIGINT, NOT NULL, foreign key to patient.id) - The patient who remains active
  - `merged_patient_id` (BIGINT, NOT NULL, foreign key to patient.id) - The patient who was merged and made inactive
  - `merge_date` (TIMESTAMP, NOT NULL, default CURRENT_TIMESTAMP) - When the merge was performed
  - `performed_by_user_id` (BIGINT, NOT NULL, foreign key to system_user.id) - Global Administrator who performed merge
  - `reason` (TEXT, NOT NULL) - Reason provided by user for the merge
  - `data_summary` (JSONB) - JSON object with counts: { orders: {total: N, active: N}, results: {total: N}, samples: {total: N}, documents: {total: N}, auditEntries: {total: N}, identifiers: {...}, conflicts: {...} }
  - Indexes: `idx_primary_patient` (primary_patient_id), `idx_merged_patient` (merged_patient_id), `idx_merge_date` (merge_date)

- **FHIR Patient Resource** (Primary Patient):
  - `active`: true
  - `identifier`: Array including both original patient IDs (primary with current status, merged with `use: "old"` and `period.end`)
  - `link`: Array with element `{ other: { reference: "Patient/[MERGED-ID]" }, type: "replaces" }`
  - All other patient demographics preserved from original primary patient

- **FHIR Patient Resource** (Merged Patient):
  - `active`: false
  - `identifier`: Original identifiers with `period.end` set to merge timestamp
  - `link`: Array with element `{ other: { reference: "Patient/[PRIMARY-ID]" }, type: "replaced-by" }`
  - Demographics preserved but patient is marked inactive

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Global Administrators can complete a patient merge from patient selection through confirmation in under 2 minutes for typical cases (patients with <100 results each)
- **SC-002**: Merge operation for patients with combined total <200 results completes within 5 seconds from clicking "Confirm Merge" to success message display
- **SC-003**: All foreign key relationships (sample_human, patient_identity, patient_contact, external_patient_id, patient_relations, electronic_order) are updated atomically with zero data loss
- **SC-004**: FHIR Patient resources for both primary and merged patients are updated correctly with `replaces`/`replaced-by` link types in 100% of merge operations
- **SC-005**: Real-time notifications are delivered to all active user sessions viewing affected patient data within 2 seconds of merge completion
- **SC-006**: Users viewing merged patient data can reload their screen and see updated primary patient information within 3 seconds of clicking "Reload Page"
- **SC-007**: Complete audit trail is captured for 100% of merge operations including user ID, timestamp, reason, data summary, and all table updates
- **SC-008**: System prevents unauthorized access with 100% accuracy - users without Global Administrator permission cannot access merge functionality via menu or direct API calls
- **SC-009**: Pre-merge validation catches 100% of invalid merge attempts (same patient, already merged, concurrent operations) before database transaction begins
- **SC-010**: Transaction rollback occurs successfully in 100% of failed merge attempts, leaving both patient records in their original state
- **SC-011**: Merge indicators appear correctly in audit trail displays for 100% of historical entries related to merged patients
- **SC-012**: System handles concurrent merge attempts with proper locking, ensuring only one merge proceeds while the other receives clear error message
- **SC-013**: Performance warning is displayed for merges involving >200 combined results, and these operations complete within 30 seconds
- **SC-014**: All UI strings are internationalized via React Intl with zero hardcoded English text in production code
- **SC-015**: System maintains >70% unit test coverage, >60% integration test coverage, and E2E tests covering full merge workflow and edge cases
