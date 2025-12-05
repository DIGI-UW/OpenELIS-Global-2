# Feature Specification: Referred Sample Container Management System

**Feature Branch**: `002-shipment-support` **Created**: 2025-01-15 **Last
Updated**: 2025-01-15 (Figma Design Alignment Clarification) **Status**: Draft -
Ready for Planning **Input**: User description: "I want to create a new feature
based on the following JIRA ticket: https://uwdigi.atlassian.net/browse/OGC-62"

**Change Log**:

- 2025-01-15: Figma design alignment clarification (Session 2025-01-15) -
  Clarified receiving workflow UX (auto-mark on scan, no acceptance modal),
  unified rejection terminology ('Reject Sample' triggers non-conformity
  recording via inline table actions), confirmed non-conformity types read from
  existing OpenELIS configuration, mandated consistent state terminology across
  all layers (no simplified aliases)
- 2025-11-14: OGC-62 alignment remediation - Added missing box states (Partially
  Received, Cancelled, Lost in Transit) with detailed state definitions,
  expanded NonConformity entity with full data model, added 3 new functional
  requirements (FR-030b, FR-030c, FR-030d) for state transitions, added 3 edge
  cases for new states, enhanced FR-027 with sample disposition, updated SC-004
  with state progression, added batch import to out of scope, confirmed exact
  match for manual search (no auto-complete)
- 2025-01-14: Integrated Figma design review findings - Added 13 new functional
  requirements (FR-042 to FR-053), comprehensive UI/UX requirements section
  (UI-001 to UI-029), expanded Key Entities with design-based fields
  (temperature, capacity, priority, tracking number, courier, patient name,
  sample type, Reconciled state), added 7 new edge cases, and enhanced
  assumptions with design clarifications

## Clarifications

### Session 2025-11-14

- Q: How should the manual sample search/lookup handle partial matches or
  multiple results when a user types an accession number? → A: Require exact
  full accession number match - show error if not found, no partial matching
- Q: Can a receiving technician pause the receiving workflow and resume it
  later, or must it be completed in one session? → A: Auto-save draft state,
  allow resume anytime - system remembers which samples were checked
- Q: What is the relationship between a "shipment" and a "box"? Are they the
  same entity or different? → A: Shipment contains multiple boxes - one shipment
  can have many boxes with shared tracking/courier
- Q: Are "packing list" and "manifest" the same document, or are they different
  documents with different purposes? → A: Same document - just different names
  for the same box contents listing

### Session 2025-01-15 (Figma Design Alignment)

- Q: Should we add explicit acceptance modal after scanning a sample during
  receiving? → A: No - Auto-mark as received on scan (keep existing FR-025
  behavior)
- Q: Are 'rejecting a sample' and 'recording a non-conformity' the same action,
  or different workflows? → A: Same action - 'Reject' triggers non-conformity
  recording workflow
- Q: Which rejection UX approach should take precedence during receiving - modal
  after scan or inline table actions? → A: Inline table actions only - No modal
  rejection option
- Q: Should non-conformity types be configurable by administrators or
  hard-coded? → A: Read from existing OpenELIS non-conformity configuration
  (list exists in app already)
- Q: Should Figma TypeScript types use simplified UI labels ('ready', 'shipped')
  or match spec states exactly ('Ready to Send', 'Sent')? → A: Align to spec -
  Update Figma types to match spec exactly for consistency

### Session 2025-12-04

- Q: How should shipment non-conformities integrate with the existing OpenELIS
  quality system? → A: Fully integrate with OpenELIS quality system - all
  shipment non-conformities are created as standard quality incidents in the
  existing quality module
- Q: What is the maximum acceptable response time for individual system
  operations (page loads, searches, API calls)? → A: 5 seconds max response time
- Q: What level of observability is required beyond audit trails (logging
  levels, metrics, alerting)? → A: Standard logging plus alerting
- Q: What is the minimum data retention period for shipment records (boxes,
  manifests, audit logs, non-conformities)? → A: 7 years retention minimum
- Q: What encryption requirements apply to sensitive data (patient names, sample
  information) in storage and transmission? → A: Encrypted at rest and transit

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Create and Manage Sample Boxes (Priority: P1)

**As a** shipping coordinator  
**I want to** create boxes with unique identifiers and add samples to them  
**So that** I can organize samples for shipment to reference laboratories

**Why this priority**: This is the foundational capability that enables all
other workflows. Without box creation and sample management, the system cannot
function.

**Independent Test**: Can be fully tested by creating a box, adding samples via
barcode scan and manual lookup, viewing the manifest, and verifying all samples
are correctly associated with the box. This delivers immediate value by
replacing manual tracking methods.

**Acceptance Scenarios**:

1. **Given** I am a shipping coordinator with Shipping role permissions,
   **When** I click "Create New Box" and select a destination facility, **Then**
   the system creates a box with a unique auto-generated ID and saves it in
   Draft state
2. **Given** I have a box in Draft state, **When** I scan a sample barcode,
   **Then** the sample is added to the box manifest with status "Pending" and I
   see a green checkmark confirmation
3. **Given** I have a box with samples, **When** I try to add a sample that is
   already in another active box, **Then** the system rejects it with an error
   message showing which box contains the sample
4. **Given** I have a box with samples, **When** I search for a sample manually
   and add it, **Then** the sample appears in the manifest with all required
   metadata displayed
5. **Given** I have a box in Draft state, **When** I remove a sample from the
   box, **Then** the sample is removed from the manifest and the action is
   logged in the audit trail

---

### User Story 2 - Track Unassigned Referred Samples (Priority: P1)

**As a** shipping coordinator  
**I want to** view all samples marked for referral but not yet assigned to any
shipment box  
**So that** I can ensure no referred samples are forgotten or lost in the
referral process

**Why this priority**: This is a critical accountability feature that prevents
sample loss. Samples marked for referral must be tracked from the moment they're
flagged until they're shipped, ensuring complete traceability and preventing
samples from being forgotten.

**Independent Test**: Can be fully tested by marking samples for referral in the
order entry system, viewing them in the Unassigned Tests dashboard tab,
verifying aging indicators (7-day and 30-day thresholds), and adding samples to
boxes from the unassigned list. This delivers immediate value by providing
visibility into all pending referrals.

**Acceptance Scenarios**:

1. **Given** I have samples marked for referral in the order entry system,
   **When** I navigate to the "Unassigned Tests" tab in the dashboard, **Then**
   I see all referred samples that are not yet in any active box, displayed in a
   searchable table
2. **Given** I have unassigned samples, **When** I view the table, **Then**
   samples unassigned for 7-30 days are highlighted in yellow and samples
   unassigned for >30 days are highlighted in red
3. **Given** I have an unassigned sample, **When** I click "Add to Box" from the
   action menu, **Then** I can select an existing box or create a new box, and
   the sample is removed from the unassigned list
4. **Given** I have an unassigned sample that cannot be located, **When** I mark
   it as lost with a required reason, **Then** the sample is removed from the
   unassigned list, marked as lost in the system, and appears in the Lost
   Samples report
5. **Given** I have an unassigned sample that no longer needs referral, **When**
   I cancel the referral with a required reason, **Then** the referral flag is
   removed, the sample returns to normal workflow, and it is removed from all
   shipping tracking

---

### User Story 3 - Generate Labels and Manifests (Priority: P1)

**As a** shipping coordinator **I want to** print box labels and generate
manifests (also called packing lists) **So that** boxes can be physically
identified and reference labs know what samples to expect

**Why this priority**: Labels and manifests are essential for physical shipment
tracking and regulatory compliance. Reference labs require documentation of what
samples are being sent.

**Independent Test**: Can be fully tested by marking a box as Ready to Send,
printing a box label with barcode, generating a manifest with all sample
details, and verifying the manifest includes all required information (box ID,
destination, sample list, dates). This delivers immediate value by providing
physical and electronic documentation for shipments.

**Acceptance Scenarios**:

1. **Given** I have a box in Ready to Send state, **When** I click "Print
   Label", **Then** a label is generated with the box ID as a scannable barcode
   and all configured label fields (destination, date, sample count)
2. **Given** I have a box in Ready to Send state, **When** I click "Generate
   Manifest" (or "Print Manifest"), **Then** a manifest is created with box
   metadata, complete sample list with accession numbers, test types, collection
   dates, and special requirements
3. **Given** I have generated a manifest, **When** I view the manifest, **Then**
   I can see barcodes for the box and optionally for each sample, and I can
   export it as PDF or print it
4. **Given** I have sent a box, **When** I need to resend the manifest, **Then**
   I can resend it electronically (FHIR/API/Email) or regenerate it within 24
   hours without approval

---

### User Story 4 - Send Boxes with Confirmation (Priority: P1)

**As a** shipping coordinator **I want to** mark boxes as sent with mandatory
confirmation **So that** I don't accidentally send boxes prematurely and the
manifest is locked

**Why this priority**: Sending a box is a critical action that locks the
manifest and triggers electronic notifications. The confirmation prevents
accidental sends and ensures proper workflow.

**Independent Test**: Can be fully tested by marking a box as Ready to Send,
clicking "Send Box", confirming the warning modal, verifying the box state
changes to Sent, and confirming the electronic manifest is automatically sent to
the destination. This delivers immediate value by ensuring shipments are
properly tracked and documented.

**Acceptance Scenarios**:

1. **Given** I have a box in Ready to Send state, **When** I click "Send Box",
   **Then** a warning modal appears showing box summary (ID, destination, sample
   count, date/time) requiring explicit confirmation
2. **Given** I confirm sending a box, **When** the box is marked as Sent,
   **Then** the manifest is locked (samples cannot be edited), the box state
   changes to Sent, and an electronic manifest is automatically sent based on
   destination configuration
3. **Given** I have sent a box, **When** I view the box details, **Then** I can
   see the send timestamp, sending user, and transmission status of the
   electronic manifest

---

### User Story 5 - Receive and Reconcile Boxes (Priority: P1)

**As a** receiving technician  
**I want to** scan boxes and samples upon arrival and record any discrepancies  
**So that** I can confirm receipt and document quality issues

**Why this priority**: Receiving workflow ensures samples are properly accounted
for at the destination and any issues are documented for quality management and
compliance.

**Independent Test**: Can be fully tested by scanning a box ID at receiving,
scanning each sample barcode to mark as received, recording non-conformities for
damaged samples, marking missing samples, and completing the receipt. This
delivers immediate value by providing accurate reconciliation and quality
documentation.

**Acceptance Scenarios**:

1. **Given** I am at the receiving location, **When** I scan a box ID, **Then**
   the system displays the box details, expected sample list, and a receiving
   checklist
2. **Given** I am receiving a box, **When** I scan each sample barcode, **Then**
   the corresponding sample is marked as received with a green checkmark, and a
   running count shows "X of Y samples received"
3. **Given** I encounter a sample with a damaged barcode, **When** I manually
   check it off, **Then** the sample is marked as received with a note
   indicating manual confirmation
4. **Given** I receive a damaged sample, **When** I record a non-conformity with
   type and optional notes/photos, **Then** the non-conformity is logged, the
   sample is flagged but remains available for testing, and the issue appears in
   reports
5. **Given** I am in the middle of receiving a box, **When** I exit the
   receiving workflow, **Then** the system auto-saves my progress (which samples
   were checked) and I can resume later from the same point
6. **Given** I have accounted for all expected samples (received, missing, or
   damaged), **When** I complete the receipt, **Then** the box state changes to
   Received or Closed, a receipt summary is generated, and the completion is
   logged with timestamp and user

---

### User Story 6 - View Dashboard and Generate Reports (Priority: P2)

**As a** shipping coordinator or administrator  
**I want to** view key metrics about shipments and generate reports  
**So that** I can track shipping activity and analyze performance

**Why this priority**: While not critical for day-to-day operations, dashboard
metrics and reporting provide visibility into shipping operations and support
management decision-making.

**Independent Test**: Can be fully tested by viewing the dashboard with metric
cards (boxes ready to send, in transit, awaiting receipt, received this week),
filtering the box list table by date, destination, and status, and generating a
report with export to PDF/Excel. This delivers value by providing operational
insights and audit capabilities.

**Acceptance Scenarios**:

1. **Given** I access the dashboard, **When** I view the main screen, **Then** I
   see metric cards showing counts of boxes in key states (Ready to Send, In
   Transit, Awaiting Receipt, Received This Week) with trend indicators
2. **Given** I want to find a specific box, **When** I use the search and
   filters (date range, destination, status), **Then** the box list table
   updates to show only matching boxes
3. **Given** I need to generate a shipping report, **When** I select filters
   (date range, destination, status) and click "Generate Report", **Then** a
   report is created with all matching boxes and their details, and I can export
   it as PDF or Excel

---

### User Story 7 - Configure System Settings (Priority: P3)

**As an** administrator  
**I want to** configure label formats, facility destinations, and business
rules  
**So that** the system meets our operational and regulatory requirements

**Why this priority**: Configuration is necessary for system setup and
customization but is not required for initial MVP functionality. Administrators
can configure these settings during implementation and as needed.

**Independent Test**: Can be fully tested by creating label prefix templates,
configuring barcode types and dimensions, managing the facility registry
(reference lab destinations), and setting up FHIR integration options. This
delivers value by allowing customization to meet specific operational needs.

**Acceptance Scenarios**:

1. **Given** I am an administrator, **When** I configure a label prefix template
   (e.g., "REF-BOX-"), **Then** new boxes use this prefix in their
   auto-generated IDs
2. **Given** I am managing facilities, **When** I add or edit a reference
   laboratory destination, **Then** the facility appears in the destination
   dropdown when creating boxes
3. **Given** I am configuring unassigned test alerts, **When** I set aging
   thresholds (warning at 7 days, alert at 30 days) and enable email
   notifications, **Then** the system sends alerts when samples exceed these
   thresholds

---

### Edge Cases

- What happens when a sample is added to a box, then the box is cancelled? The
  sample should return to unassigned status if it still has a referral flag
- How does the system handle a box that is sent but never received? The box can
  be marked as "Lost in Transit" after a configurable time period
- What happens when a receiving technician scans a sample that's not in the
  manifest? The system offers to add it as an "unexpected sample" with required
  documentation
- How does the system handle bulk operations when some samples fail validation?
  The entire bulk operation is rolled back (all-or-nothing transaction)
- What happens when a box is marked as sent but needs to be recalled? Within 24
  hours, regeneration is allowed; after 24 hours up to 7 days, admin approval is
  required
- How does the system handle samples that are marked as lost but later found?
  Administrators can reverse the lost status, returning the sample to unassigned
  if it still has a referral flag
- What happens when electronic manifest transmission fails? The system retries
  automatically (3 attempts with exponential backoff), and users can manually
  retry after automatic attempts fail
- How does the system handle a sample that needs to be referred again after
  referral was cancelled? The sample can be marked for referral again in the
  order entry system and will appear in unassigned tests
- What is the source of sample priority (Critical/Urgent/Normal)? Priority is
  derived from test type configuration in OpenELIS, where each test type has an
  assigned priority level that maps to the display priority
- What happens when box capacity is exceeded? System prevents adding samples
  beyond capacity limit with error message showing current capacity (e.g., "Box
  is full (25/25 samples). Please select a different box or create a new one.")
- What is the difference between "Received" and "Reconciled" states? "Received"
  means the box has been scanned at destination; "Reconciled" means all samples
  have been accounted for (received, missing, or damaged) and checked into the
  local system
- How does the system handle patient names for HIPAA compliance? Patient names
  are displayed in Unassigned Tests ONLY if user has "View Patient Names"
  permission; otherwise, only accession numbers are shown
- What happens when removing a sample from a Ready to Send box? The box state
  automatically reverts to Draft state, requiring re-validation before it can be
  marked Ready to Send again
- How does the system handle samples with different temperature requirements in
  the same box? The box temperature requirement is set at box level; users
  should only add samples with matching temperature requirements to avoid
  conflicts
- What happens during receiving if a sample's barcode is completely illegible?
  User can manually check off the sample by selecting it from the list, and the
  audit trail logs this as manual confirmation (distinct from scanned
  confirmation)
- What happens to in-progress receiving workflows that are never completed? The
  system retains in-progress receiving state indefinitely (no automatic
  timeout), and administrators can view and manage incomplete receiving
  workflows via dashboard filters
- How are boxes grouped into shipments? When boxes are marked as Sent, the
  system either creates a new shipment or allows adding them to an existing
  shipment going to the same destination; shipment provides shared
  tracking/courier information and shipment-level reporting
- Are "packing list" and "manifest" different documents? No, they are the same
  document listing all samples in a box; the terms are used interchangeably, but
  "manifest" is the preferred term for consistency
- What is the difference between "Received" and "Partially Received" states?
  "Partially Received" means receiving workflow has started (first sample
  scanned) but not all samples are accounted for yet; "Received" means the
  receiving workflow shows all expected samples as accounted for (received,
  missing, or damaged)
- How long before a box can be marked "Lost in Transit"? After a configurable
  time period (default 14 days from ship date) if box state is still "In
  Transit" and no receiving activity has occurred
- Can a cancelled box be reactivated? No - cancellation is permanent. Samples
  must be added to a new box if referral is still needed

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST allow shipping coordinators to create boxes with
  unique identifiers (auto-generated or manually entered/scanned)
- **FR-002**: System MUST require destination facility selection from a facility
  registry when creating boxes
- **FR-003**: System MUST allow users to add samples to boxes via barcode
  scanning (USB keyboard wedge input)
- **FR-004**: System MUST allow users to add samples to boxes via manual
  search/lookup by accession number or sample ID, requiring exact full match (no
  partial matching) and showing error if not found
- **FR-005**: System MUST validate that each sample exists in OpenELIS before
  allowing addition to a box
- **FR-006**: System MUST prevent duplicate samples within the same box
- **FR-007**: System MUST prevent samples from being added to multiple active
  boxes simultaneously
- **FR-008**: System MUST display all samples in a box in a manifest table with
  required metadata (accession number, test type, collection date, status,
  temperature/storage requirements)
- **FR-009**: System MUST allow removal of samples from boxes in Draft, Ready to
  Send, or In Transit states
- **FR-010**: System MUST track and display all samples marked for referral but
  not yet assigned to any active box (unassigned tests)
- **FR-011**: System MUST calculate and display "days unassigned" for referred
  samples (from referral date to current date)
- **FR-012**: System MUST highlight unassigned samples based on aging thresholds
  (7 days = warning/yellow, 30 days = alert/red)
- **FR-013**: System MUST allow users to add unassigned samples directly to
  boxes from the unassigned tests list
- **FR-014**: System MUST allow users to mark unassigned samples as lost with a
  required reason
- **FR-015**: System MUST allow users to cancel referrals for unassigned samples
  with a required reason
- **FR-016**: System MUST support bulk selection and bulk actions on unassigned
  samples (add to box, export)
- **FR-017**: System MUST allow users to mark boxes as "Ready to Send" after
  validating required fields (destination, at least one sample, all samples have
  required metadata)
- **FR-018**: System MUST generate printable box labels with scannable barcodes
  using configurable templates
- **FR-019**: System MUST generate manifests (also called packing lists) with
  box metadata (ID, destination, temperature requirement, date/time), complete
  sample list with accession numbers and test types, and optional sample
  barcodes
- **FR-020**: System MUST allow regeneration of manifests within 24 hours of
  sending without approval
- **FR-021**: System MUST require confirmation via warning modal before marking
  a box as Sent
- **FR-022**: System MUST lock manifests (prevent sample edits) when a box is
  marked as Sent
- **FR-023**: System MUST automatically send electronic manifests
  (FHIR/API/Email) when a box is marked as Sent, based on destination
  configuration
- **FR-023a**: System MUST allow grouping multiple boxes with the same
  destination into a single shipment with shared tracking number and courier
  information
- **FR-023b**: System MUST automatically create a shipment when one or more
  boxes are marked as Sent, or allow adding boxes to existing shipments
- **FR-023c**: System MUST display shipment-level information in the Shipments
  dashboard tab, showing tracking, courier, total boxes, and total samples per
  shipment
- **FR-024**: System MUST allow receiving technicians to scan box IDs to
  initiate receiving workflow
- **FR-025**: System MUST allow receiving technicians to scan sample barcodes to
  mark samples as received
- **FR-026**: System MUST allow manual check-off of samples when barcodes are
  unreadable
- **FR-027**: System MUST allow recording of non-conformities via inline "Reject
  Sample" action button in receiving workflow table with type selection from
  existing OpenELIS non-conformity configuration, required notes for type
  "Other", optional photo/document attachments (JPG, PNG, PDF, max 10MB per
  file, max 5 files per non-conformity), and required sample disposition
  selection (Accepted for Testing, Rejected, Recollection Requested). System
  MUST create quality incidents directly in the existing OpenELIS quality module
  with full integration, ensuring all shipment non-conformities appear in
  standard quality reports and tracking workflows. Note: "Reject Sample" is the
  user-facing term; the action triggers the non-conformity recording workflow
- **FR-028**: System MUST allow marking samples as missing during receiving
  workflow
- **FR-029**: System MUST allow documenting unexpected samples (not in manifest)
  with required explanation
- **FR-030**: System MUST allow completion of receiving workflow when all
  expected samples are accounted for (received, missing, damaged, or rejected)
- **FR-030a**: System MUST auto-save receiving progress after each sample is
  checked (scanned or manually confirmed) and allow technicians to exit and
  resume the workflow later from the same point
- **FR-030b**: System MUST transition box to "Partially Received" state when
  receiving workflow is initiated (first sample scanned) and to "Received" state
  when all expected samples are accounted for
- **FR-030c**: System MUST allow marking boxes as "Lost in Transit" after a
  configurable time period (default 14 days) if box has not been received
- **FR-030d**: System MUST allow administrators to cancel boxes in Draft, Ready
  to Send, or In Transit states with required cancellation reason, transitioning
  box to "Cancelled" state
- **FR-031**: System MUST display a dashboard with key metrics (boxes ready to
  send, in transit, awaiting receipt, received this week)
- **FR-032**: System MUST provide search and filter capabilities for box lists
  (by date range, destination, status, Box ID)
- **FR-033**: System MUST allow generation of reports with filters (Box ID, date
  range, destination, status) and export to PDF, Excel, or CSV
- **FR-034**: System MUST allow administrators to configure label prefixes,
  barcode types, label dimensions, and label templates
- **FR-035**: System MUST allow administrators to manage the facility registry
  (reference lab destinations)
- **FR-036**: System MUST allow administrators to configure unassigned test
  aging thresholds and alert settings
- **FR-037**: System MUST log all actions (box creation, sample
  additions/removals, state changes, sending, receiving) with user ID,
  timestamp, and relevant details in an audit trail
- **FR-038**: System MUST enforce role-based access control (Shipping role for
  day-to-day operations, Administrator role for configuration and post-send
  edits)
- **FR-039**: System MUST integrate with OpenELIS order entry system to read
  sample data and update referral flags
- **FR-040**: System MUST support FHIR R4 SupplyDelivery resources for
  electronic manifest exchange
- **FR-041**: System MUST handle non-referral samples by prompting for referral
  reason and automatically setting referral flag in OpenELIS
- **FR-042**: System MUST allow optional entry of courier name and tracking
  number when creating or editing boxes
- **FR-043**: System MUST enforce box capacity limits based on selected capacity
  template (25, 50, or 100 samples), preventing addition of samples beyond
  capacity
- **FR-044**: System MUST require temperature requirement selection at box level
  from configured temperature ranges (2-8°C, -20°C, Ambient, -80°C)
- **FR-045**: System MUST derive sample priority (Critical/Urgent/Normal) from
  test type configuration in OpenELIS and display it in unassigned tests list
  with color-coded indicators (red=Critical, orange=Urgent, gray=Normal)
- **FR-046**: System MUST display patient names in Unassigned Tests list ONLY if
  user has "View Patient Names" permission for HIPAA compliance; otherwise show
  only accession numbers
- **FR-047**: System MUST allow filtering unassigned tests by "My cases"
  (samples assigned to current user)
- **FR-048**: System MUST provide "Simulate Scan" functionality for testing
  barcode workflows without physical scanners (for training and testing
  purposes)
- **FR-049**: System MUST support "Reconciled" state for boxes when receiving is
  complete and all samples are checked into local system (distinct from
  "Received" which indicates box arrival)
- **FR-050**: System MUST display non-conformity type selection when recording
  quality issues, reading available types from existing OpenELIS non-conformity
  configuration (typical types include: Damaged, Insufficient Volume,
  Mislabeled, Expired, Contaminated, Wrong Sample Type, Other with required
  explanation)
- **FR-051**: System MUST include temperature designation on packing
  lists/manifests to ensure proper handling during transport
- **FR-052**: System MUST support bulk actions on unassigned samples including:
  bulk add to box, bulk export, bulk mark as lost (with confirmation), bulk
  cancel referral (with confirmation)
- **FR-053**: System MUST distinguish in audit trail between samples received
  via barcode scan versus manual check-off
- **FR-054**: System MUST implement structured logging (INFO, WARN, ERROR
  levels) for all critical operations and provide alerting for system errors,
  failed manifest transmissions, and boxes stuck in transit beyond expected
  timeframes
- **FR-055**: System MUST retain all shipment records (boxes, manifests, audit
  logs, non-conformities) for a minimum of 7 years to meet healthcare regulatory
  compliance requirements, with configurable archival strategies for records
  older than the retention period

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - include only relevant
principles for this feature:_

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files -
    legacy exempt until refactored)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
- **CR-005**: External data integration MUST use FHIR R4 + IHE profiles for
  electronic manifest exchange
- **CR-006**: Configuration-driven variation for country-specific requirements
  (NO code branching) - label formats, aging thresholds, notification settings
  must be configurable
- **CR-007**: Security: RBAC (Shipping role, Administrator role), audit trail
  (user ID + timestamp for all actions), input validation, encryption at rest
  (database-level encryption for sensitive fields including patient names) and
  in transit (TLS 1.2+ for all API communications and FHIR exchanges)
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal)
- **CR-009**: Performance: All individual operations (page loads, searches, API
  calls, barcode scans) MUST complete within 5 seconds maximum response time
  under normal load conditions

### UI/UX Requirements

_Based on Carbon Design System implementation in approved designs:_

**Dashboard Structure:**

- **UI-001**: Dashboard MUST display two tabs with count badges: "Shipments"
  (showing count of active shipments) and "Unassigned Tests" (showing count of
  unassigned samples)
- **UI-002**: Shipments tab MUST display 4 metric cards at the top:
  - In Transit (count of boxes currently in transit)
  - Delivered/Received (count of boxes received but not reconciled)
  - Reconciled (count of boxes fully reconciled)
  - Total Samples (count of samples in all active boxes)
- **UI-003**: Unassigned Tests tab MUST display alert banner when unassigned
  count > 0 with message: "X Unassigned Tests Requiring Attention - These
  samples are marked for referral but have not been assigned to a shipping box.
  Please assign them to ensure proper tracking and prevent sample loss."
- **UI-004**: Unassigned Tests tab MUST display priority breakdown metric cards:
  - Critical Priority (count with red accent)
  - Urgent Priority (count with orange accent)
  - Normal Priority (count with gray accent)
  - Avg. Wait Time (average days waiting, calculated)

**Table and List Display:**

- **UI-005**: Unassigned Tests table MUST show color-coded left borders for
  priority: Red (Critical), Orange (Urgent), Gray (Normal)
- **UI-006**: All data tables MUST support bulk selection with checkboxes and
  display selected count (e.g., "2 tests selected")
- **UI-007**: Bulk action toolbar MUST appear when items are selected, showing
  available actions (e.g., "Assign to Box")
- **UI-008**: Tables MUST include search field and filter controls (e.g., "My
  cases" checkbox, "All Priorities" dropdown)
- **UI-009**: All tables MUST show pagination controls with "Items per page"
  dropdown and current page indicator

**Forms and Modals:**

- **UI-010**: Create Box modal MUST show two-column layout: left side for input
  fields, right side for summary panel
- **UI-011**: Summary panel MUST display real-time updates of: Box Number,
  Capacity (X samples / Y total, Z remaining), Destination, Temperature
- **UI-012**: All forms MUST show real-time validation with inline error
  messages in red (e.g., "_ Select a destination facility", "_ Add at least one
  sample")
- **UI-013**: Required fields MUST be marked with asterisk (\*) in the field
  label
- **UI-014**: Action buttons MUST be disabled with visual indication until all
  validation passes, then enabled

**Progress Indicators:**

- **UI-015**: Receiving workflow MUST show progress indicator at top: "X/Y
  received" with progress bar
- **UI-016**: Box capacity MUST show visual indicator: "Samples: X / Y" with "Z
  remaining" message
- **UI-017**: All multi-step workflows MUST show current progress and total
  steps

**Empty States:**

- **UI-018**: All empty lists/tables MUST show helpful icon (box/package icon)
  and descriptive text (e.g., "No samples added yet - Scan or search for samples
  to add them to this box")
- **UI-019**: Empty states MUST include guidance on next action

**Barcode Scanning UX:**

- **UI-020**: All barcode scan fields MUST show barcode icon next to input field
  for visual clarity
- **UI-021**: All barcode scan fields MUST include "Simulate Scan" button for
  testing without hardware
- **UI-022**: Helper text under scan fields MUST read: "Position barcode in
  scanner or click 'Simulate Scan' to test"
- **UI-023**: Scan fields MUST include "Search" button for manual lookup
  alternative
- **UI-023a**: Manual search MUST require exact full accession number match; if
  not found, display inline error message: "Sample not found. Please verify the
  accession number and try again."

**Status and Feedback:**

- **UI-024**: Status tags MUST use consistent color coding: Blue (In Transit),
  Green (Delivered/Received), Gray (Draft)
- **UI-025**: All actions MUST show immediate feedback (success checkmark, error
  message, loading spinner)
- **UI-026**: Confirmation modals MUST show box/action summary before requiring
  confirmation

**Navigation:**

- **UI-027**: Top navigation bar MUST include: Dashboard link, Create Box
  button, Receive button, Settings icon, Unassigned badge (red with count)
- **UI-028**: All modals MUST include close button (X) in top-right corner
- **UI-029**: All modals MUST show clear action buttons at bottom: primary
  action (right), secondary/cancel (left)

### Key Entities _(include if feature involves data)_

- **Box (ShippingBox)**: Represents a physical container used to ship multiple
  samples to a reference laboratory. Key attributes: unique Box ID, destination
  facility, creation date/time, creator user, current state (Draft, Ready to
  Send, Sent, In Transit, Partially Received, Received, Reconciled, Cancelled,
  Lost in Transit), sent date/time, received date/time, shipment reference
  (foreign key to Shipment), **capacity (25, 50, or 100 samples)**,
  **temperature requirement (2-8°C Refrigerated, -20°C Frozen, Ambient Room
  Temperature, -80°C Ultra-low)**, notes
  - **State Progression**: Draft → Ready to Send → Sent → In Transit → Partially
    Received → Received → Reconciled
    - **Alternative paths**: Any active state → Cancelled (if box not sent or
      shipment abandoned)
    - **Alternative paths**: In Transit → Lost in Transit (if box not received
      within configurable time period)
  - **Draft**: Box created, samples being added
  - **Ready to Send**: All validation passed, ready for shipment
  - **Sent**: Box dispatched to reference lab, manifest locked
  - **In Transit**: Box confirmed en route
  - **Partially Received**: Receiving workflow initiated, some samples confirmed
    (not all)
  - **Received**: Box scanned at destination facility, receiving workflow in
    progress or complete
  - **Reconciled**: All samples accounted for (received, missing, damaged, or
    rejected) and checked into local system (terminal state for successful
    receipt)
  - **Cancelled**: Box or shipment cancelled/abandoned (terminal state)
  - **Lost in Transit**: Box not received within expected timeframe, marked as
    lost (terminal state)
  - **Note**: Individual boxes may have their own tracking numbers, but when
    grouped into a shipment, the shipment-level tracking/courier takes
    precedence
- **Shipment**: Represents a logical grouping of one or more boxes being sent
  together to the same destination on the same date with shared tracking and
  courier information. Key attributes: unique Shipment ID, list of boxes
  (one-to-many relationship), destination facility, **tracking number (shared
  across all boxes in shipment)**, **courier name (shared across all boxes in
  shipment)**, shipment date/time, expected delivery date, current status (In
  Transit, Delivered, Reconciled), total sample count (calculated from all
  boxes)
  - **Relationship**: One Shipment contains one or more Boxes; all boxes in a
    shipment must have the same destination facility
  - **Purpose**: Provides shipment-level tracking and reporting when multiple
    boxes are sent together
- **Sample (in Box)**: Represents a laboratory sample included in a shipment
  box. Key attributes: accession number, test type, collection date, status
  within box (Pending, Sent, Received, Missing, Damaged, Rejected), **priority
  (Critical/Urgent/Normal - derived from test type)**, temperature/storage
  requirements, relationship to OpenELIS sample record
- **Unassigned Sample**: Represents a sample marked for referral in OpenELIS but
  not yet assigned to any active box. Key attributes: accession number, referral
  date, referral destination, referral reason, days unassigned (calculated),
  lost status, referral cancellation status, **priority
  (Critical/Urgent/Normal)**, **sample type (Plasma, Whole Blood, Sputum,
  Nasopharyngeal Swab, etc.)**, **patient name (optional - requires "View
  Patient Names" permission for HIPAA compliance)**, **assigned to user
  (optional)**, lab number
- **Facility (Destination)**: Represents a reference laboratory or facility that
  receives shipments. Key attributes: facility name, facility code, address,
  contact information, integration configuration (FHIR/API/Email settings)
- **Non-Conformity (Quality Issue)**: Represents a quality issue recorded during
  receiving workflow when a sample is received but has problems.
  **Implementation Note**: This entity uses the existing OpenELIS quality
  incident system directly rather than creating a separate shipment-specific
  table. All shipment non-conformities are created as standard quality incidents
  in the OpenELIS quality module with additional context fields (box_id,
  shipment context). Key attributes:
  - `id` (BIGINT, PK) - Unique identifier (from existing quality system)
  - `sample_id` (BIGINT, FK) - Reference to sample with issue
  - `box_id` (BIGINT, FK) - Reference to box being received (shipment-specific
    context field)
  - `type` (VARCHAR, required) - Type of issue read from existing OpenELIS
    non-conformity configuration (typical values: Damaged, Insufficient Volume,
    Mislabeled, Expired, Contaminated, Wrong Sample Type, Other)
  - `notes` (TEXT, required for type="Other", optional otherwise) - Description
    of the issue
  - `resolution` (TEXT, optional) - How the issue was resolved
  - `recorded_date` (TIMESTAMP, required) - When issue was recorded
  - `recorded_by` (BIGINT, FK, required) - User who recorded the issue
  - `attachments` (JSON, optional) - Array of document/photo file references
    (JPG, PNG, PDF; max 10MB per file, max 5 files per non-conformity)
  - `sample_disposition` (ENUM, required) - What happened to the sample:
    Accepted for Testing (with caveats), Rejected (cannot be tested),
    Recollection Requested
  - **Relationship**: One sample can have multiple non-conformities (e.g., both
    damaged and insufficient volume)
  - **Business Rule**: Non-conformity does NOT automatically reject the sample;
    technician decides disposition based on severity
  - **Integration**: All shipment non-conformities appear in standard OpenELIS
    quality reports, tracking workflows, and quality management dashboards for
    unified quality oversight
  - **UI Note**: User-facing action button labeled "Reject Sample" in receiving
    workflow table triggers the non-conformity recording workflow; rejection
    options are accessed via inline table actions, not modal dialogs
- **Manifest**: Represents the packing list/documentation for a box. Key
  attributes: box reference, version number, generation date/time, sample list,
  electronic transmission status and history
- **Audit Log**: Represents a record of all actions performed in the system. Key
  attributes: action type, user ID, timestamp, entity affected (Box ID, Sample
  ID), previous and new values, IP address

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Shipping coordinators can create a box and add 10 samples via
  barcode scanning in under 2 minutes
- **SC-002**: All referred samples are visible in the unassigned tests list
  within 1 minute of being marked for referral in the order entry system
- **SC-003**: Shipping coordinators can generate and print a box label and
  packing list in under 30 seconds
- **SC-004**: Receiving technicians can scan a box (transitioning to Partially
  Received state), reconcile 20 samples (marking as received, recording
  non-conformities), and complete receiving (transitioning to Received then
  Reconciled state) in under 5 minutes
- **SC-005**: System supports tracking 1,000+ active boxes and 10,000+ samples
  per day without performance degradation
- **SC-006**: 95% of electronic manifest transmissions succeed on the first
  attempt, with 99% success rate after automatic retries
- **SC-007**: Users can search and filter box lists and find a specific box
  among 500+ boxes in under 10 seconds
- **SC-008**: Unassigned tests dashboard displays aging indicators (7-day and
  30-day thresholds) accurately, updating in real-time as samples are added to
  boxes
- **SC-009**: All actions (box creation, sample additions, sending, receiving)
  are logged in the audit trail with 100% accuracy (no missing audit records)
- **SC-010**: System reduces manual tracking errors (lost samples, missing
  documentation) by 80% compared to spreadsheet-based tracking methods
- **SC-011**: Shipping coordinators can complete the full workflow (create box,
  add samples, generate manifest, send box) for a 20-sample shipment in under 10
  minutes
- **SC-012**: Receiving workflow completion rate (all expected samples accounted
  for) is 95% or higher for all received boxes

## Assumptions

- Barcode scanners are USB keyboard wedge devices that input data as if typed
  from keyboard
- Reference laboratories have varying capabilities for electronic manifest
  receipt (FHIR, API, Email, or none)
- Samples can be marked for referral in the OpenELIS order entry system before
  being added to boxes
- Some samples may need referral even if not originally marked for referral
  (requires reason documentation)
- Boxes may be received partially (some samples missing) and still be completed
- Non-conformities do not automatically reject samples but flag them for quality
  review
- Administrators need ability to edit boxes after sending for error correction
  (with proper audit trail)
- Label printers are standard thermal or inkjet printers accessible from user
  workstations
- System will integrate with existing OpenELIS user authentication and role
  management
- Facility registry (reference lab destinations) is managed within OpenELIS or
  can be imported
- Sample data (accession numbers, test types, collection dates) is available
  from OpenELIS order entry system
- Electronic manifest transmission failures are expected occasionally (network
  issues, destination system downtime) and require retry capability
- **Sample priority (Critical/Urgent/Normal) is derived from test type
  configuration** in OpenELIS; each test type has a configurable priority level
  that determines display priority in unassigned tests
- **Box capacity templates are configurable** but default to 25, 50, or 100
  samples with corresponding physical grid dimensions (e.g., 5x5 for 25-sample
  boxes)
- **Temperature requirements are enforced at box level**; all samples in a box
  should have matching temperature requirements to ensure proper transport
  conditions
- **Patient name visibility is permission-based** to support HIPAA compliance;
  users without "View Patient Names" permission see only accession numbers
- **Tracking numbers are manually entered** (not integrated with courier APIs)
  and serve as reference information only
- **"Received" and "Reconciled" are distinct states**: Received = box scanned at
  destination, Reconciled = all samples accounted for and checked into local
  system
- **Simulate Scan functionality is provided for training and testing** without
  requiring physical barcode scanners
- **State terminology must be consistent across all layers**: TypeScript types,
  database enums, and UI labels must use exact spec state names (e.g., 'Ready to
  Send', 'Sent', 'In Transit', 'Partially Received') - no simplified aliases
  like 'ready' or 'shipped'

## Dependencies

- OpenELIS order entry system: For reading sample data, checking referral flags,
  updating referral status
- OpenELIS user management: For authentication and role-based access control
- OpenELIS facility registry: For reference laboratory destination data
- OpenELIS non-conformity/quality system: For reading configured non-conformity
  types (during receiving workflow) and creating quality incidents directly in
  the existing quality module. All shipment non-conformities are created as
  standard quality incidents with full integration for unified quality
  management and reporting
- FHIR server: For electronic manifest exchange via SupplyDelivery resources (if
  destination supports FHIR)
- Label printing infrastructure: Printers and drivers accessible from user
  workstations
- Barcode scanning hardware: USB keyboard wedge scanners for sample and box ID
  scanning

## Out of Scope

- Batch import of samples via CSV/Excel file upload (future enhancement - v2.0;
  v1.0 supports barcode scanning and manual lookup only)
- Courier/shipping carrier integration (automatic tracking via carrier APIs -
  tracking numbers are manually entered for reference only)
- Mobile application for receiving workflow (web interface only)
- Automated box suggestions based on destination and sample volume (manual box
  creation only)
- SMS notifications for aging unassigned samples (email notifications only)
- Predictive analytics for shipping patterns
- Integration with external laboratory information systems beyond FHIR/API/Email
- Sample storage location assignment (handled by separate storage management
  feature)
- Sample aliquoting workflow (handled by separate sample management feature)
- Order entry functionality (samples must already exist in OpenELIS)
- Patient registration or demographics management
- Test result reporting or analysis
