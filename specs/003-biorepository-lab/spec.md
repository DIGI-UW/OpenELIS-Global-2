# Feature Specification: Biorepository Laboratory Module

**Feature Branch**: `003-biorepository-lab` **Created**: 2026-01-07 **Status**:
Clarified **UAT Release ETA**: January 16th, 2026 **Input**: SRS Document -
Biorepository Laboratory Module v2.0 (AHRI OpenELIS)

---

## Executive Summary

The Biorepository Laboratory Module provides comprehensive sample lifecycle
management for AHRI's research biorepository. This module enables complete
tracking of biological specimens from intake through disposal, supporting ISO
20387:2018 compliance, environmental monitoring, quality control inspections,
and digital request/retrieval workflows.

**Business Value**:

- Protect $2M+ investment in irreplaceable biological specimens
- Reduce sample retrieval time by 60% (from 15-30 min to <5 min)
- Enable ISO 20387:2018 biobanking accreditation
- Eliminate "lost" samples through digital tracking with <0.1% error rate
- Provide real-time environmental monitoring with immediate alerting

---

## Scope Boundaries

### In Scope

- Complete sample lifecycle management (intake through disposal)
- Hierarchical storage location tracking (Room → Freezer → Shelf → Rack → Box →
  Position)
- Environmental monitoring integration (temperature sensors, alerts)
- Digital request and approval workflows
- Systematic QC inspection management
- Comprehensive audit trail and reporting
- Role-based access control
- Barcode generation and scanning

### Out of Scope

- **Laboratory testing and result management** - Handled by core OpenELIS
  pathology/clinical modules
- **Patient demographic management** - Handled by core OpenELIS patient module
- **Financial/billing operations** - Not part of biorepository tracking
- **Procurement and inventory of consumables** - Separate inventory management
  concern
- **System backup and disaster recovery** - System-wide OpenELIS infrastructure
  concern
- **Physical security systems** - External building access control systems

---

## User Scenarios & Testing

### User Story 1 - Sample Intake & Registration (Priority: P1)

As a **Biorepository Technician**, I need to receive shipments, register
incoming samples (individually or in bulk via manifest), verify documentation,
and generate barcodes so that every sample has traceable provenance from the
moment it enters the biorepository.

**Why this priority**: Sample intake is the critical entry point that determines
downstream traceability. Without proper registration, samples cannot be located,
provenance cannot be established, and regulatory compliance fails. This is the
foundation for all other biorepository operations.

**Independent Test**: Can be fully tested by: (1) logging shipment reception,
(2) registering samples via single entry or bulk manifest import, (3) completing
documentation verification checklist, (4) generating barcodes, and (5) verifying
complete audit trail.

**Acceptance Scenarios**:

**Shipment Reception:**

1. **Given** a shipment arrives at the biorepository, **When** the technician
   logs reception, **Then** the system records delivery reference, sender,
   receiver, reception timestamp, packaging condition, and transport
   temperature.

2. **Given** a shipment has damaged packaging, **When** the technician documents
   the condition, **Then** the system requires photo attachment and flags all
   samples for enhanced inspection.

3. **Given** a shipment is logged, **When** proceeding to sample registration,
   **Then** the system pre-populates reception metadata (date, transport
   conditions) for all samples in the shipment.

**Single Sample Registration:**

4. **Given** a new biological sample arrives at the biorepository, **When** the
   technician scans/enters sample information and completes all mandatory
   fields, **Then** the system generates a unique immutable barcode and creates
   a complete sample record with timestamp.

5. **Given** a sample is a derivative (aliquot) of an existing parent sample,
   **When** registering the aliquot, **Then** the system maintains the
   parent-child lineage relationship automatically.

6. **Given** a sample arrives with damaged labeling, **When** the technician
   selects manual entry fallback, **Then** the system allows registration while
   triggering a relabeling workflow.

**Bulk Manifest Import:**

7. **Given** multiple samples arrive together, **When** the technician uploads a
   CSV manifest file, **Then** the system parses the file, validates all rows,
   and displays a preview with any errors before committing.

8. **Given** a manifest contains validation errors, **When** the preview is
   displayed, **Then** the system highlights specific rows and columns with
   error messages, allowing correction before import.

9. **Given** a manifest passes validation, **When** the technician confirms
   import, **Then** the system creates all sample records in a single
   transaction with unique barcodes generated for each.

10. **Given** a user needs to prepare a manifest, **When** they request the
    template, **Then** the system provides a downloadable CSV template with all
    required and optional columns documented.

**Documentation Verification:**

11. **Given** a sample is registered, **When** the documentation verification
    step is reached, **Then** the system displays the 7-point checklist with
    auto-verified items (project linkage, biosafety match) pre-checked.

12. **Given** required documentation is missing (ethics approval, consent for
    human samples), **When** the technician cannot provide it, **Then** the
    system places the sample in quarantine status with specific missing items
    noted.

13. **Given** a sample is in quarantine, **When** documentation is later
    provided, **Then** the technician can complete verification and release the
    sample from quarantine with audit trail.

14. **Given** documentation items are not applicable (MTA for internal samples),
    **When** the technician marks them N/A, **Then** the system records the N/A
    status with justification.

**Barcode Generation:**

15. **Given** a sample passes documentation verification, **When** the barcode
    is generated, **Then** the system creates a unique DataMatrix 2D barcode
    containing no PII.

16. **Given** barcodes are generated, **When** the technician requests labels,
    **Then** the system generates a PDF with labels in configurable formats
    (single label or Avery sheet layouts).

**Condition Documentation:**

17. **Given** sample arrival condition has issues, **When** documenting the
    condition, **Then** the system supports standardized condition codes and
    photo attachment for evidence.

---

### User Story 2 - Storage Location Assignment (Priority: P1)

As a **Biorepository Technician**, I need to assign samples to precise storage
locations with environmental matching so that samples are stored correctly and
can be located instantly.

**Why this priority**: Precise location tracking is fundamental to biorepository
operations. Without it, samples become effectively lost among thousands of
similar tubes. This directly enables the 60% reduction in retrieval time.

**Independent Test**: Can be tested by assigning a sample to a storage location,
scanning barcodes to confirm placement, and verifying the hierarchical location
(Room → Freezer → Shelf → Rack → Box → Position) is recorded.

**Acceptance Scenarios**:

1. **Given** a registered sample requiring -80°C storage, **When** the
   technician attempts to assign it to a -20°C location, **Then** the system
   prevents the assignment and displays an environmental mismatch error.

2. **Given** a sample is ready for storage, **When** the technician assigns a
   location, **Then** the system displays a visual box layout showing
   occupied/available positions with the target position highlighted.

3. **Given** a sample is being placed in storage, **When** the technician scans
   both box and sample barcodes, **Then** the system verifies placement matches
   the registered location and records timestamp.

4. **Given** a high-value/irreplaceable sample, **When** storage is assigned,
   **Then** the system requires dual verification (operator placement +
   supervisor confirmation scan).

5. **Given** a freezer reaches 85% capacity, **When** capacity is calculated,
   **Then** the system generates an alert for proactive procurement planning.

---

### User Story 3 - Environmental Monitoring & Logging (Priority: P1)

As a **Biorepository Technician**, I need to record temperature readings with
systematic logging so that environmental conditions are documented for
compliance and anomalies are identified during data entry.

**Why this priority**: Temperature documentation is essential for ISO 20387
compliance and GLP requirements. While automated alerts are deferred (DD-002),
systematic manual logging ensures audit trail integrity and enables trend
analysis.

**Independent Test**: Can be tested by entering temperature readings, verifying
out-of-range values are flagged, and confirming complete audit trail with
timestamps.

**Acceptance Scenarios**:

1. **Given** temperature monitoring is required, **When** readings are entered
   manually, **Then** the system logs temperatures with timestamp at minimum
   twice daily per BR-MON-001.

2. **Given** a temperature reading is outside the acceptable range for the
   storage unit, **When** the technician enters the value, **Then** the system
   flags the excursion, requires acknowledgment, and prompts for corrective
   action notes.

3. **Given** a storage unit has not had a temperature reading in 24 hours,
   **When** the dashboard is viewed, **Then** the system highlights the unit as
   "overdue for reading."

4. **Given** temperature readings are logged, **When** generating reports,
   **Then** the system produces temperature trend charts and excursion summaries
   for any date range.

5. **Given** an excursion is recorded, **When** it is acknowledged, **Then** the
   system requires the technician to document the response action taken.

---

### User Story 4 - Sample Request & Retrieval Workflow (Priority: P2)

As a **Researcher/Principal Investigator**, I need to request samples through a
self-service digital workflow so that I can access samples for research
efficiently with proper authorization.

**Why this priority**: Digital workflows replace informal requests (emails,
verbal) with auditable, validated processes. This enables same-day retrieval for
routine requests versus 3-5 days with manual processes.

**Independent Test**: Can be tested by submitting a sample request, verifying
automated validation, processing approval, and confirming retrieval with barcode
scan and custody update.

**Acceptance Scenarios**:

1. **Given** I am an authorized researcher, **When** I submit a sample request
   through the system, **Then** the system automatically validates against
   authorization, project linkage, ethics approval, consent scope, and
   availability.

2. **Given** my request fails validation (e.g., expired ethics approval),
   **When** the system checks criteria, **Then** the request is returned with
   specific deficiency notification.

3. **Given** my request passes validation, **When** the approver reviews it,
   **Then** they can approve or reject with documented reasoning.

4. **Given** my request is approved, **When** retrieval is scheduled, **Then**
   the system generates a work order with precise storage coordinates and box
   position maps.

5. **Given** a sample is being retrieved, **When** the technician scans the
   barcode, **Then** the system confirms identity, updates status to "Checked
   Out" with custodian information, and logs the complete chain-of-custody.

6. **Given** a sample is checked out, **When** 24 hours pass without return or
   status update, **Then** the system generates overdue alerts with escalation.

---

### User Story 5 - Quality Control Inspections (Priority: P2)

As a **QA Officer**, I need to schedule and execute systematic QC inspections so
that sample integrity is verified and discrepancies are caught before they
impact research.

**Why this priority**: Without systematic QC, discrepancies accumulate
undetected (3-8% typical). Systematic QC reduces error rates below 1%, essential
for ISO 20387 accreditation.

**Independent Test**: Can be tested by generating a QC subset, executing
inspections with barcode scanning, recording results (pass/discrepancy), and
verifying CAPA ticket generation for discrepancies.

**Acceptance Scenarios**:

1. **Given** QC scheduling is configured, **When** the schedule triggers,
   **Then** the system generates QC subsets using calendar-based, random
   (5-20%), or risk-based approaches.

2. **Given** a QC inspection is scheduled, **When** the technician accesses it,
   **Then** the system generates QC layout sheets with freezer/shelf/rack/box
   coordinates, position maps, and sample IDs.

3. **Given** the technician is performing QC, **When** they verify a sample,
   **Then** they can record pass status (present, label intact, condition
   acceptable) or discrepancy (missing, misplaced, damaged label, physical
   damage).

4. **Given** a discrepancy is recorded, **When** the type is specified, **Then**
   the system automatically creates a CAPA ticket for root cause analysis and
   corrective action.

5. **Given** every sample must be verified annually per BR-QC-001, **When** a
   year passes without verification, **Then** the system flags the sample and
   escalates for required inspection.

---

### User Story 6 - Retention & Disposal Management (Priority: P2)

As a **Biorepository Manager**, I need to track retention policies and manage
disposal workflows so that samples are kept for required periods and disposed of
properly with full documentation.

**Why this priority**: Prevents indefinite accumulation (30-40% "orphan" samples
typical without management), recovers storage capacity, and ensures
ethical/regulatory compliance for disposal.

**Independent Test**: Can be tested by setting retention policies, triggering
expiry alerts, processing disposal authorization workflow, and verifying
disposal documentation.

**Acceptance Scenarios**:

1. **Given** multiple retention policies apply (regulatory, ethical, project),
   **When** calculating retention, **Then** the system applies the longest
   required period.

2. **Given** a sample is 90 days from retention expiry, **When** the system
   checks retention dates, **Then** it generates alerts to project PI and
   Biorepository Manager for disposition decision.

3. **Given** a human biological sample is approved for disposal, **When** the
   disposal workflow is generated, **Then** it requires authorization from both
   Biorepository Manager AND QA Officer.

4. **Given** disposal is completed, **When** documenting the disposal, **Then**
   the system records sample ID, disposal method, date/time, responsible
   personnel, authorizing personnel, and certificate reference.

5. **Given** disposal records are created, **When** retention requirements are
   applied, **Then** the system retains records for minimum 25 years per
   regulatory requirements.

---

### User Story 7 - Reporting & Audit Trail (Priority: P3)

As a **Biorepository Manager**, I need comprehensive reporting and immutable
audit trails so that I have operational visibility and audit-ready
documentation.

**Why this priority**: Regulatory audits center on documentation review.
Complete records demonstrate operational maturity and significantly improve
audit outcomes.

**Independent Test**: Can be tested by performing sample operations and
verifying complete audit trail generation, then generating standard reports with
configurable filters.

**Acceptance Scenarios**:

1. **Given** any sample operation occurs, **When** it completes, **Then** the
   system creates an immutable audit trail entry with timestamp, user ID, action
   type, affected records, and before/after values.

2. **Given** I need operational visibility, **When** I access the dashboard,
   **Then** I see real-time freezer status, active alerts, pending requests,
   checked-out samples, and today's scheduled activities.

3. **Given** I need a standard report, **When** I select report type (Inventory
   Summary, Intake Log, Retrieval Log, Disposal Log, QC Summary, Temperature
   Log, Chain of Custody), **Then** I can configure date ranges and filters.

4. **Given** I generate a report, **When** exporting, **Then** the system
   supports CSV, Excel, JSON, and PDF formats with generation timestamp and user
   attribution.

5. **Given** I am a researcher, **When** I access reports, **Then** I can only
   see data from my own projects (role-based access).

---

### User Story 8 - Dashboard & Operational Visibility (Priority: P3)

As a **Biorepository Technician**, I need a real-time operational dashboard so
that I can see my daily tasks, alerts, and system status at a glance.

**Why this priority**: Provides technicians with efficient workflow management
and immediate awareness of critical situations requiring attention.

**Independent Test**: Can be tested by verifying dashboard displays all required
operational metrics and updates in real-time.

**Acceptance Scenarios**:

1. **Given** I log into the system, **When** I view the operational dashboard,
   **Then** I see real-time freezer status with temperature indicators
   (normal/warning/critical).

2. **Given** active alerts exist, **When** viewing the dashboard, **Then** I see
   alerts with acknowledgment status and escalation countdown.

3. **Given** requests are pending, **When** viewing the dashboard, **Then** I
   see pending approvals, retrieval work orders, and samples due for return.

4. **Given** QC is scheduled, **When** viewing the dashboard, **Then** I see
   today's QC tasks with completion status.

---

### Edge Cases

- **Power Failure**: What happens when UPS activates? System must generate
  immediate alerts to Manager + Facilities.
- **Network Outage**: How does the system handle offline operations?
  Mobile/tablet QC entry should support offline mode with sync on reconnection.
- **Barcode Scanner Failure**: Manual entry fallback must be available for all
  barcode operations.
- **Duplicate Sample ID**: System must prevent duplicate barcode generation; if
  external sample has duplicate ID, generate unique internal barcode.
- **Storage Unit Decommissioning**: How are samples relocated when a freezer is
  removed? Bulk transfer workflow with audit trail.
- **Concurrent Access**: What happens if two technicians try to assign the same
  position? System must enforce position locking.
- **Expired Ethics Approval**: How are samples with expired ethics handled? Flag
  for review, prevent new retrievals until renewed.
- **Emergency Sample Access**: How does urgent access work outside normal
  workflow? Emergency override with enhanced audit logging.

---

## Requirements

### Functional Requirements - Shipment Reception (Stage 1a)

- **FR-REC-001**: System MUST record shipment reception with delivery reference,
  sender identification, receiving personnel, and reception timestamp.
- **FR-REC-002**: System MUST capture packaging condition (intact/damaged) with
  mandatory photo attachment for damaged shipments.
- **FR-REC-003**: System MUST record transport temperature and expected sample
  count for each shipment.
- **FR-REC-004**: System MUST pre-populate reception metadata (date, transport
  conditions) when registering samples from a logged shipment.
- **FR-REC-005**: System MUST flag all samples from damaged shipments for
  enhanced inspection during registration.

### Functional Requirements - Sample Intake (Stage 1b)

- **FR-INT-001**: System MUST capture all mandatory metadata fields during
  sample registration (source, temporal, classification, storage, condition,
  retention).
- **FR-INT-002**: System MUST generate unique, immutable barcodes containing no
  personally identifiable information (PII).
- **FR-INT-003**: System MUST preserve parent-child relationships for aliquots
  and derived samples with complete lineage tracking.
- **FR-INT-004**: System MUST support barcode scanning for sample identification
  with manual entry fallback for damaged labels.
- **FR-INT-005**: System MUST record arrival condition with standardized
  condition codes and photo attachment support.
- **FR-INT-006**: System MUST link intake records to scanned/uploaded
  documentation and archive per QMS retention requirements.
- **FR-INT-007**: System MUST support quarantine status for samples with
  incomplete documentation, generating daily alerts.

### Functional Requirements - Bulk Manifest Import (Stage 1c)

- **FR-MAN-001**: System MUST support CSV file upload for bulk sample
  registration with configurable column mapping.
- **FR-MAN-002**: System MUST provide downloadable manifest template with all
  required and optional columns documented.
- **FR-MAN-003**: System MUST parse and validate all manifest rows before
  import, displaying preview with row-level error highlighting.
- **FR-MAN-004**: System MUST support multiple date formats in manifest
  (yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy, dd-MM-yyyy).
- **FR-MAN-005**: System MUST create all samples from a valid manifest in a
  single atomic transaction.
- **FR-MAN-006**: System MUST generate unique barcodes for each sample created
  via manifest import.
- **FR-MAN-007**: System MUST validate sample types against the
  biorepository-approved sample type list.
- **FR-MAN-008**: System MUST report import results including total requested,
  total created, and any errors.

**Manifest Template Columns**:

| Column                 | Required    | Description                            |
| ---------------------- | ----------- | -------------------------------------- |
| project_id             | Yes         | Project/study identifier               |
| sample_type            | Yes         | Sample type (must match approved list) |
| biosafety_level        | Yes         | BSL-1, BSL-2, or BSL-3                 |
| collection_date        | Yes         | Date/time of sample collection         |
| origin_lab             | Yes         | Source laboratory                      |
| principal_investigator | Yes         | PI name                                |
| consent_id             | Conditional | Required for human samples             |
| ethics_approval_ref    | Conditional | Required for human samples             |
| mta_reference          | Conditional | Required for external samples          |
| preservation_medium    | No          | Preservation method                    |
| arrival_condition      | No          | Condition at receipt                   |
| notes                  | No          | Additional notes                       |

### Functional Requirements - Documentation Verification (Stage 1d)

- **FR-DOC-001**: System MUST display 7-point documentation verification
  checklist after sample registration.
- **FR-DOC-002**: System MUST auto-verify checkable items (project linkage
  validity, biosafety classification match).
- **FR-DOC-003**: System MUST require manual verification for items that cannot
  be auto-checked (ethics approval currency, consent scope).
- **FR-DOC-004**: System MUST support "Not Applicable" status with justification
  for conditional items (MTA for internal samples).
- **FR-DOC-005**: System MUST place samples in QUARANTINE status when required
  documentation items are incomplete.
- **FR-DOC-006**: System MUST record verification status, verifier identity, and
  timestamp for each checklist item.
- **FR-DOC-007**: System MUST allow quarantine release when missing
  documentation is provided, with complete audit trail.
- **FR-DOC-008**: System MUST generate daily alerts for samples in quarantine
  until documentation is resolved.

**Metadata Field Specifications**:

| Field Category | Fields                                  | Mandatory   | Business Purpose            |
| -------------- | --------------------------------------- | ----------- | --------------------------- |
| Source         | Origin lab, Project ID, PI              | Yes         | Traceability, authorization |
| Temporal       | Collection date/time, Receipt date/time | Yes         | Timeline, freshness         |
| Classification | Sample type, Category, Biosafety level  | Yes         | Storage rules, handling     |
| Storage        | Required temp, Preservation medium      | Yes         | Environment matching        |
| Compliance     | Ethics ref, Consent ID, MTA ref         | Conditional | Legal/ethical compliance    |
| Condition      | Arrival condition, Integrity notes      | Yes         | Quality baseline            |
| Retention      | Policy reference, Expiration date       | Yes         | Lifecycle management        |

**Documentation Verification Checklist** (at intake):

- [ ] Sample identifiers and labeling match paperwork
- [ ] Project/study linkage is valid and active
- [ ] Ethical approval reference is on file and current
- [ ] Informed consent record ID exists (for human samples)
- [ ] Material Transfer Agreement documented (for external samples)
- [ ] Biosafety classification matches sample type
- [ ] Packaging integrity verified (damaged packaging photo documented)

**Barcode Specifications**:

- **FR-BAR-001**: Barcodes SHALL be unique across the entire system with no
  reuse
- **FR-BAR-002**: Barcode format SHALL support 2D codes (DataMatrix or QR) for
  space efficiency
- **FR-BAR-003**: Barcodes SHALL contain no PII—only system-generated
  identifiers
- **FR-BAR-004**: Parent-child relationships SHALL be maintained through
  database linkages, not barcode encoding

### Functional Requirements - Storage Location (Stage 2)

- **FR-STO-001**: System MUST enforce storage environment matching (samples
  requiring -80°C cannot be assigned to -20°C locations).
- **FR-STO-002**: System MUST generate visual box layout displays showing
  occupied/available positions with target position highlighted.
- **FR-STO-003**: System MUST require scanning of both box and sample barcodes
  to confirm placement with timestamp recording.
- **FR-STO-004**: System MUST support configurable dual verification
  requirements based on sample value/criticality classification.
- **FR-STO-005**: System MUST maintain equipment records for each storage unit
  (ID, model, capacity, qualification history, calibration records, maintenance
  logs).
- **FR-STO-006**: System MUST track real-time capacity utilization per
  freezer/room with configurable threshold alerts (default 85%).
- **FR-STO-007**: System MUST enforce hierarchical storage structure: Room →
  Freezer → Shelf → Rack → Box → Position.

### Functional Requirements - Environmental Monitoring (Stage 3)

- **FR-MON-001**: System MUST support manual temperature entry with timestamp
  (minimum twice daily per BR-MON-001). _(Note: Automated sensor integration
  deferred per DD-002)_
- **FR-MON-002**: _(DEFERRED)_ System SHOULD generate automated alerts via
  email, SMS, and in-app notification for temperature excursions, power events,
  and sensor failures. _(Deferred per DD-002—manual excursion review during data
  entry)_
- **FR-MON-003**: System MUST maintain continuous temperature logs with data
  retained for minimum 5 years per GLP requirements.
- **FR-MON-004**: System MUST log all sample movements with timestamp, user ID,
  origin location, destination location, and reason code.
- **FR-MON-005**: System MUST record equipment maintenance events linked to
  affected storage units with impact assessment.
- **FR-MON-006**: _(DEFERRED)_ System SHOULD support configurable escalation
  paths with automatic escalation if alerts are not acknowledged. _(Deferred per
  DD-002)_
- **FR-MON-007**: _(DEFERRED)_ System SHOULD detect and alert when freezer door
  remains open > 5 minutes. _(Requires sensor integration—deferred per DD-002)_
- **FR-MON-008**: System MUST flag temperature readings outside acceptable range
  during manual entry and require acknowledgment.

**Alert Configuration Matrix** _(Note: Automated alerts deferred per DD-002;
manual review during data entry)_:

| Alert Type           | Trigger Condition       | Current Handling                          | Future (Automated)             |
| -------------------- | ----------------------- | ----------------------------------------- | ------------------------------ |
| Temperature Warning  | ±3°C from setpoint      | Flagged during manual entry               | Technician alert < 30 min      |
| Temperature Critical | ±5°C from setpoint      | Flagged during manual entry, requires ack | Manager escalation < 15 min    |
| Power Interruption   | UPS activation detected | _(Deferred—requires sensor)_              | Manager + Facilities immediate |
| Sensor Failure       | No reading for 30 min   | _(Deferred—no sensors)_                   | Technician + IT < 1 hour       |
| Door Open            | Open > 5 minutes        | _(Deferred—requires sensor)_              | Technician < 10 min            |
| Overdue Reading      | No manual entry 24 hrs  | Dashboard highlight                       | -                              |

### Functional Requirements - Retention & Disposal (Stage 4)

- **FR-RET-001**: System MUST support retention policy definition at regulatory,
  project, and sample-type levels with automatic application of longest required
  period.
- **FR-RET-002**: System MUST generate automated alerts at 90, 60, and 30 days
  before retention expiry to designated personnel.
- **FR-RET-003**: System MUST generate disposal workflows requiring appropriate
  authorizations based on sample type and biosafety classification.
- **FR-RET-004**: System MUST record disposal details including sample ID,
  method, date/time, responsible personnel, authorizing personnel, and
  certificate reference.
- **FR-RET-005**: System MUST retain disposal records for minimum 25 years per
  regulatory requirements.

**Retention Policy Hierarchy**:

| Policy Level | Example                    | Typical Duration         | Authority                     |
| ------------ | -------------------------- | ------------------------ | ----------------------------- |
| Regulatory   | GLP study samples          | 15 years minimum         | Ethiopian FDA, ICH guidelines |
| Ethical      | Consent expiration         | Per consent document     | IRB/Ethics committee          |
| Project      | Study protocol requirement | Study duration + 5 years | Principal Investigator        |
| Sample Type  | DNA reference standards    | Indefinite while viable  | Biorepository Manager         |
| Default      | Unspecified samples        | 10 years from collection | Institutional policy          |

**Disposal Methods**:

| Method             | Sample Types                 | Verification Required       | Documentation                   |
| ------------------ | ---------------------------- | --------------------------- | ------------------------------- |
| Autoclaving        | BSL-1/2 biological           | Autoclave cycle validation  | Cycle record, witness signature |
| Incineration       | BSL-3, chemical, all tissues | Incineration certificate    | Chain of custody to incinerator |
| Chemical Treatment | Specific reagents            | Neutralization verification | Treatment log, disposal record  |

### Functional Requirements - Sample Request & Retrieval (Stage 5)

- **FR-RTV-001**: System MUST provide self-service request interface for
  authorized users to submit, track, and manage sample requests.
- **FR-RTV-002**: System MUST automatically validate requests against
  authorization, project, ethics, consent, and availability criteria.
- **FR-RTV-003**: System MUST support configurable approval workflows based on
  sample type, project, and request characteristics.
- **FR-RTV-004**: System MUST generate retrieval work orders with precise
  storage coordinates and box position maps.
- **FR-RTV-005**: System MUST require barcode scan confirmation during retrieval
  to verify correct sample identity.
- **FR-RTV-006**: System MUST update sample custody status in real-time and
  maintain complete chain-of-custody records.
- **FR-RTV-007**: System MUST generate digital chain-of-custody forms for
  inter-laboratory and external transfers.
- **FR-RTV-008**: System MUST send automated notifications at key workflow
  stages.

### Functional Requirements - Quality Control (Stage 6)

- **FR-QC-001**: System MUST support configurable QC scheduling (calendar-based,
  percentage-based random, risk-adaptive, event-triggered).
- **FR-QC-002**: System MUST automatically generate statistically valid random
  sample subsets for QC inspection.
- **FR-QC-003**: System MUST generate printable and digital QC layout sheets
  with storage coordinates and box maps.
- **FR-QC-004**: System MUST support mobile/tablet QC data entry with barcode
  scanning confirmation.
- **FR-QC-005**: System MUST flag/escalate unverified samples and require
  documented resolution for all discrepancies.
- **FR-QC-006**: System MUST integrate with CAPA workflow, automatically
  creating tickets for discrepancies.
- **FR-QC-007**: System MUST generate digitally signed QC audit reports with
  summary statistics and electronic signatures.
- **FR-QC-008**: System MUST provide QC dashboard with verification coverage
  rates, discrepancy trends, and CAPA status.

**QC Scheduling Approaches**:

| Approach        | Method                             | Coverage        | Best For                                      |
| --------------- | ---------------------------------- | --------------- | --------------------------------------------- |
| Scheduled       | Calendar-based (monthly/quarterly) | 100% over cycle | Baseline comprehensive coverage               |
| Random          | Statistical sampling each period   | 5-20% per cycle | Ongoing verification between full inspections |
| Risk-Based      | Adaptive based on history          | Variable        | Focus resources on problem areas              |
| Event-Triggered | After equipment issues, moves      | Affected units  | Verify integrity after disruptions            |

**Discrepancy Management**:

| Discrepancy     | Immediate Action                                   | Root Cause Investigation                      |
| --------------- | -------------------------------------------------- | --------------------------------------------- |
| Missing Sample  | Search adjacent positions, check recent retrievals | Required - escalate if not located in 48 hrs  |
| Misplaced       | Return to correct position, update system          | Review recent handling, retrain if pattern    |
| Damaged Label   | Relabel with verification, photo document          | Check storage conditions, label supplier      |
| Physical Damage | Quarantine, assess viability                       | Check freezer conditions, handling procedures |

### Functional Requirements - Reporting & Audit (Stage 7)

- **FR-AUD-001**: System MUST maintain immutable audit trail for all sample
  lifecycle events with timestamp, user attribution, and before/after values.
- **FR-AUD-002**: System MUST provide real-time operational dashboards
  accessible to authorized personnel based on role.
- **FR-AUD-003**: System MUST generate standard reports with configurable date
  ranges and filter criteria.
- **FR-AUD-004**: System MUST export data in CSV, Excel, JSON, and PDF formats
  with appropriate access controls.
- **FR-AUD-005**: System MUST restrict report access based on user role
  (researchers limited to own project data).
- **FR-AUD-006**: System MUST retain audit trail data for minimum 25 years with
  automated archival after 5 years.

**Audit Event Categories**:

| Event Category   | Specific Events Logged                                                  |
| ---------------- | ----------------------------------------------------------------------- |
| Sample Lifecycle | Registration, storage assignment, movement, retrieval, return, disposal |
| Environmental    | Temperature readings, excursions, alerts, acknowledgments               |
| QC Activities    | Inspection scheduling, execution, results, discrepancy resolution       |
| Requests         | Submission, validation, approval/rejection, fulfillment                 |
| Administrative   | User access, role changes, configuration changes                        |

**Standard Reports**:

| Report            | Description                                        | Key Filters                             |
| ----------------- | -------------------------------------------------- | --------------------------------------- |
| Inventory Summary | Current sample counts by type, project, location   | Sample type, project, storage unit      |
| Intake Log        | Samples received within date range                 | Date range, source, project             |
| Retrieval Log     | Samples retrieved with requester and purpose       | Date range, requester, project          |
| Disposal Log      | Samples disposed with method and authorization     | Date range, disposal method, authorizer |
| QC Summary        | Inspection results, discrepancy rates, CAPA status | Date range, QC type, storage unit       |
| Temperature Log   | Environmental readings and excursion events        | Date range, freezer, excursion only     |
| Chain of Custody  | Complete history for specified samples             | Sample ID(s), date range                |

**Management Dashboard Items**:

- Storage capacity utilization by room/freezer
- Sample aging analysis (approaching retention limits)
- QC compliance rates and trends
- Discrepancy rates by type and location
- Request fulfillment metrics (time to approval, time to retrieval)
- CAPA status summary

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks (Constitution II)
- **CR-002**: All UI strings MUST be internationalized via React Intl - no
  hardcoded text (Constitution VII)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form) with:
  - JPA/Hibernate annotations on entities (NO XML mappings)
  - @Transactional in services ONLY (NOT controllers)
  - Services must compile ALL data within transaction (prevent
    LazyInitializationException)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
  (Constitution VI)
- **CR-005**: External data integration MUST use FHIR R4 (if specimen
  integration with external systems required) (Constitution III)
- **CR-006**: Configuration-driven variation for site-specific requirements
  (Constitution I)
- **CR-007**: Security: RBAC via existing role system, audit trail
  (sys_user_id + lastupdated), input validation (Constitution VIII)
- **CR-008**: Tests MUST be included per Testing Roadmap - unit tests (JUnit 4 +
  Mockito), E2E tests (Cypress), ORM validation tests (Constitution V)
- **CR-009**: Spec-Driven Iteration: Feature broken into Validation Milestones
  with 1 PR per milestone (Constitution IX)

### Non-Functional Requirements

**Performance**:

- **NFR-001**: Sample registration response time < 3 seconds
- **NFR-002**: Barcode scan validation < 1 second
- **NFR-003**: QC layout generation (1000 samples) < 10 seconds
- **NFR-004**: Dashboard refresh < 5 seconds
- **NFR-005**: Support 50 concurrent users

**Security**:

- **NFR-006**: Integration with existing OpenELIS authentication (Spring
  Security)
- **NFR-007**: Role-based access control per permission matrix
- **NFR-008**: Session timeout: 30 minutes inactivity
- **NFR-009**: All transmissions via TLS 1.3
- **NFR-010**: Sensitive data (donor linkages) encrypted at rest

**Availability**:

- **NFR-011**: System availability: 99.5% during business hours
- **NFR-012**: Environmental alerts: 24/7 operation
- **NFR-013**: Planned maintenance outside business hours with 48hr notice

### Key Entities

- **Shipment**: Incoming delivery record with reference, sender, receiver,
  packaging condition, transport temperature, and expected sample count
- **BioSample**: Core sample entity with barcode, type, classification,
  biosafety level, collection date, project linkage, consent reference, shipment
  reference, and lifecycle status
- **DocumentationVerification**: Per-sample checklist record with 7 verification
  items, status (verified/pending/N-A), verifier, and timestamp
- **StorageRoom**: Physical room containing freezers, with access control zone
  designation
- **StorageDevice**: Freezer/refrigerator with model, capacity, temperature
  setpoint, and equipment qualification records
- **StorageShelf**: Shelf within device (vertical organization)
- **StorageRack**: Rack within shelf (removable unit for batch access)
- **StorageBox**: Box containing sample positions (81 or 100 positions per box)
- **StoragePosition**: Individual position within box (coordinate-based: A1-J10)
- **BioSampleAssignment**: Links sample to position with placement timestamp and
  verification status
- **TemperatureReading**: Environmental monitoring data with device reference
  and excursion flags
- **SampleRequest**: Request workflow entity with requester, samples, approval
  status, and fulfillment tracking
- **QCInspection**: QC batch with schedule type, sample subset, and completion
  status
- **QCResult**: Individual sample verification result with pass/discrepancy
  status
- **RetentionPolicy**: Policy definition with level
  (regulatory/project/sample-type), duration, and authority
- **DisposalRecord**: Disposal documentation with method, authorizations, and
  certificate reference

### Business Rules Summary

| Rule ID    | Description                                                                                            |
| ---------- | ------------------------------------------------------------------------------------------------------ |
| BR-SAM-001 | Every sample must be assigned a classification at intake                                               |
| BR-INT-001 | Samples must not be placed in storage until registration is complete                                   |
| BR-INT-002 | Samples with incomplete documentation enter quarantine with daily alerts                               |
| BR-STO-001 | Samples of different biosafety levels must be stored in segregated areas                               |
| BR-STO-002 | Project-specific samples should be co-located when possible                                            |
| BR-STO-003 | High-value samples require dual verification for placement                                             |
| BR-MON-001 | Temperature readings minimum every 15 minutes (automated) or twice daily (manual)                      |
| BR-MON-002 | Samples removed from storage must be returned or status updated within 24 hours                        |
| BR-RET-001 | Samples approaching retention expiry (90 days) generate alerts to PI and Manager                       |
| BR-RET-002 | Human biological samples require dual authorization for disposal                                       |
| BR-RTV-001 | Requests failing validation returned to requester with specific deficiency notification                |
| BR-QC-001  | Every sample must be physically verified at least once per calendar year                               |
| BR-QC-002  | Discrepancies must be recorded with specific type and description                                      |
| BR-AUD-001 | All audit trail entries include timestamp, user ID, action type, affected records, before/after values |

---

## User Roles & Permissions

| Role                     | Description                                              | Access Level              |
| ------------------------ | -------------------------------------------------------- | ------------------------- |
| Biorepository Technician | Daily operations: intake, storage, retrieval, QC         | Operational functions     |
| Biorepository Manager    | Supervision, approvals, full oversight                   | Full module access        |
| QA Officer               | Quality oversight, QC management, disposal authorization | QC + compliance functions |
| Researcher               | Request samples, view own project data                   | Self-service requests     |
| Lab Supervisor           | Approve own lab requests, view reports                   | Department-scoped         |
| System Administrator     | Configuration, user management, full reporting           | Technical administration  |

### Permission Matrix

| Function               | Technician | Manager | QA Officer | Researcher   | Supervisor | Admin |
| ---------------------- | ---------- | ------- | ---------- | ------------ | ---------- | ----- |
| Sample Intake          | Full       | Full    | View       | -            | Request    | -     |
| Storage Assignment     | Full       | Full    | View       | -            | -          | -     |
| QC Execution           | Full       | Full    | Full       | -            | -          | -     |
| Request Approval       | -          | Full    | View       | -            | Own Dept   | -     |
| Disposal Authorization | -          | Auth    | Auth       | -            | -          | -     |
| Reporting              | Limited    | Full    | Full       | Own Projects | Dept       | Full  |
| Configuration          | -          | Limited | -          | -            | -          | Full  |

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Sample retrieval time reduced from 15-30 minutes average to <5
  minutes (60% improvement target)
- **SC-002**: "Lost" sample incidents reduced to <0.1% (from 5-10% with manual
  tracking)
- **SC-003**: Temperature excursion detection time <15 minutes (from 12-60 hours
  with manual checks)
- **SC-004**: QC inspection coverage achieves 100% annual verification for all
  samples
- **SC-005**: Discrepancy rate reduced to <1% (from 3-8% typical without
  systematic QC)
- **SC-006**: Request-to-retrieval time for routine requests <1 day (from 3-5
  days)
- **SC-007**: Zero undetected equipment failures (no sample losses due to
  delayed detection)
- **SC-008**: Audit-ready documentation producible within minutes for any sample
- **SC-009**: System supports ISO 20387:2018 accreditation requirements

### Compliance Targets

- **SC-010**: Pass ISO 20387:2018 biobanking quality management audit
- **SC-011**: Meet Good Laboratory Practice (GLP) data integrity requirements
  (ALCOA+)
- **SC-012**: Comply with Ethiopian FDA biological material handling regulations
- **SC-013**: Maintain research ethics compliance (consent tracking, ethical
  approval linkage)

---

## Regulatory Compliance Framework

| Standard/Regulation            | Key Requirements                                        | System Impact                                    |
| ------------------------------ | ------------------------------------------------------- | ------------------------------------------------ |
| ISO 20387:2018                 | Biobanking quality management, traceability, competence | Complete audit trail, QC processes               |
| Good Laboratory Practice (GLP) | Data integrity (ALCOA+), equipment qualification        | Immutable records, equipment tracking            |
| Ethiopian FDA                  | Biological material handling, safety                    | Biosafety level tracking, disposal documentation |
| Research Ethics                | Informed consent, ethical approval linkage              | Consent tracking, approval reference fields      |
| Data Protection                | Personal data security, access control                  | Role-based access, audit logging, encryption     |

### ALCOA+ Data Integrity Compliance

All data captured within the biorepository module SHALL comply with ALCOA+
principles:

| Principle           | System Implementation                                                |
| ------------------- | -------------------------------------------------------------------- |
| **A**ttributable    | All records linked to authenticated user; no shared logins permitted |
| **L**egible         | Standardized formats; clear display; printable outputs               |
| **C**ontemporaneous | Server timestamps at time of action; no backdating                   |
| **O**riginal        | First entry preserved; corrections as new records with references    |
| **A**ccurate        | Validation rules; dropdown selections; barcode verification          |
| **+C**omplete       | Mandatory fields enforced; audit trail captures all events           |
| **+C**onsistent     | Standardized codes; controlled vocabularies; format validation       |
| **+E**nduring       | Retained per policy; protected from deletion; backup procedures      |
| **+A**vailable      | Authorized access; appropriate response times; export capabilities   |

---

## Sample Type Classifications

| Category        | Examples                   | Storage Temp    | Biosafety | Business Criticality              |
| --------------- | -------------------------- | --------------- | --------- | --------------------------------- |
| Blood-derived   | Serum, plasma, whole blood | -80°C / LN2     | BSL-2     | High - primary research material  |
| Nucleic Acids   | DNA, RNA extracts          | -80°C / LN2     | BSL-1     | Critical - irreplaceable extracts |
| Tissue          | Biopsies, FFPE blocks      | -80°C / Ambient | BSL-2     | High - limited availability       |
| Cellular        | Cell lines, PBMCs          | LN2 vapor       | BSL-2     | Critical - viable cultures        |
| Microbiological | Isolates, cultures         | -80°C / LN2     | BSL-2/3   | High - research strains           |

### Non-Biological Materials

The biorepository also stores non-biological materials essential for research
operations:

| Category            | Examples                               | Storage Requirements        |
| ------------------- | -------------------------------------- | --------------------------- |
| Pharmaceuticals     | Drug compounds under investigation     | Per compound specifications |
| Laboratory Reagents | Certified reference materials          | Per manufacturer specs      |
| Proficiency Testing | PT samples, QC standards               | Per program requirements    |
| Environmental       | Soil, water samples from field studies | Study-specific              |

### Sample Source Categories

| Source Category               | Documentation Required                      | Special Considerations                 |
| ----------------------------- | ------------------------------------------- | -------------------------------------- |
| Internal - Active Studies     | Protocol reference, consent ID              | Subject to study-specific restrictions |
| Internal - Concluded Projects | Original approval, transfer authorization   | May require re-consent for new use     |
| External Collaborators        | MTA, shipping records, origin documentation | Access restricted per MTA terms        |
| Commercial/Reference          | Certificate of analysis, purchase records   | Typically unrestricted internal use    |

---

## Storage Hierarchy Specification

| Level | Element        | Capacity            | Identifier Format | Business Purpose                            |
| ----- | -------------- | ------------------- | ----------------- | ------------------------------------------- |
| 1     | Room           | Multiple freezers   | ROOM-XXX          | Physical segregation, access control zones  |
| 2     | Freezer/Device | Multiple shelves    | FRZ-XXXX          | Temperature environment, equipment tracking |
| 3     | Shelf          | Up to 32 racks      | SHF-XX            | Vertical organization within freezer        |
| 4     | Rack           | Multiple boxes      | RCK-XX            | Removable unit for batch access             |
| 5     | Box            | 81 or 100 positions | BOX-XXXXX         | Project/study grouping, QC unit             |
| 6     | Position       | Single sample       | A1-J10            | Precise sample location                     |

---

## Glossary

| Term             | Definition                                                                                             |
| ---------------- | ------------------------------------------------------------------------------------------------------ |
| Aliquot          | Portion divided from original specimen                                                                 |
| BSL              | Biosafety Level (1-4 classification)                                                                   |
| CAPA             | Corrective and Preventive Action                                                                       |
| Chain of Custody | Documented trail of possession and handling                                                            |
| FFPE             | Formalin-Fixed Paraffin-Embedded tissue                                                                |
| GLP              | Good Laboratory Practice                                                                               |
| IQ/OQ/PQ         | Installation/Operational/Performance Qualification                                                     |
| IRB              | Institutional Review Board                                                                             |
| LN2              | Liquid Nitrogen                                                                                        |
| MTA              | Material Transfer Agreement                                                                            |
| PII              | Personally Identifiable Information                                                                    |
| QMS              | Quality Management System                                                                              |
| ALCOA+           | Attributable, Legible, Contemporaneous, Original, Accurate + Complete, Consistent, Enduring, Available |

---

## Complete Workflow Summary

### Biorepository Laboratory Staff Workflow

**Sample Intake Flow** (Stage 1 - 4 sub-stages):

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  STAGE 1a: SHIPMENT RECEPTION                                               │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Log Shipment Arrival → Record Sender/Receiver → Check Packaging Condition →│
│  Record Transport Temperature → Note Expected Sample Count                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  STAGE 1b: SAMPLE REGISTRATION (choose one)                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Option A: Single Entry                                                      │
│    Enter Metadata → Scan/Enter Sample ID → Link to Shipment                 │
│                                                                             │
│  Option B: Bulk Manifest Import                                             │
│    Upload CSV → Map Columns → Preview & Validate → Confirm Import           │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  STAGE 1c: DOCUMENTATION VERIFICATION                                       │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Display 7-Point Checklist → Auto-Verify Where Possible →                   │
│  Manual Verify Remaining → Mark N/A with Justification →                    │
│  [If incomplete] Quarantine Sample OR [If complete] Proceed                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  STAGE 1d: BARCODE GENERATION                                               │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Generate Unique Barcode → Download Label PDF → Print & Affix to Sample     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
                          [Proceed to Stage 2: Storage Assignment]
```

**Ongoing Management Flow**:

```
Store Sample → Monitor Temperature → Flag Retention Expiry → Schedule QC Check →
Perform QC Inspection → Record QC Results → [If expired] Generate Disposal Workflow →
Dispose Sample → Update Removal Status → End Process
```

**Retrieval Flow** (parallel to main flow):

```
Receive Request → Validate Request → Approve/Reject → Schedule Retrieval →
Log Barcode Scan → Retrieve Sample → Track Sample Location → Log Retrieval Action →
Notify Designated Personnel
```

### Sample Requester Workflow

```
Submit Sample Request → [Wait for Processing] → Receive Approval/Rejection →
[If approved] Receive Pickup Notification
```

---

## Design Decisions (Clarification Phase)

The following architectural and implementation decisions were made during the
clarification phase:

### DD-001: Storage System Integration

**Decision**: Extend existing OpenELIS storage system rather than creating
parallel entities.

**Rationale**: OpenELIS already has `StorageDevice`, `StorageRoom`,
`StorageShelf`, `StorageRack`, `StorageBox`, `SampleStorageAssignment`, and
`SampleStorageMovement` entities. Extending these avoids data duplication,
maintains a single source of truth, and leverages existing code.

**Implementation Impact**:

- Add biorepository-specific fields to existing entities (e.g., biosafety level,
  equipment qualification records)
- Create new entities only where none exist (e.g., `TemperatureReading`,
  `RetentionPolicy`, `DisposalRecord`)
- Ensure backward compatibility with existing storage usage

### DD-002: Temperature Monitoring Approach

**Decision**: Manual temperature entry only (no automated sensor integration).

**Rationale**: Automated sensor integration adds significant complexity (IoT
protocols, hardware dependencies, real-time streaming). Manual entry provides
the essential audit trail without infrastructure dependencies.

**Implementation Impact**:

- Temperature readings entered via UI form (twice daily per BR-MON-001)
- No automated alerts—temperature excursion review is manual during data entry
- Alert configuration matrix requirements (FR-MON-002, FR-MON-006, FR-MON-007)
  are **deferred** for future automated integration
- Dashboard displays most recent manual readings

**Future Consideration**: Architecture should allow for future sensor
integration without major refactoring.

### DD-003: Sample Request Workflow

**Decision**: Fixed (hardcoded) approval workflow: Researcher → Manager →
Technician.

**Rationale**: Simplifies initial implementation. AHRI's workflow is
well-established and unlikely to change frequently.

**Implementation Impact**:

- Workflow stages: `SUBMITTED` → `MANAGER_REVIEW` → `APPROVED`/`REJECTED` →
  `ASSIGNED_TO_TECHNICIAN` → `RETRIEVED`
- No workflow configuration UI needed
- Approval roles fixed: Biorepository Manager approves requests
- If workflow changes are needed later, refactor to configurable engine

### DD-004: CAPA System Integration

**Decision**: Extend existing `NcEvent` (Non-Conformance Event) entity for CAPA
tickets.

**Rationale**: OpenELIS has existing CAPA-related entities (`QaEvent`,
`NcEvent`, `CorrectiveAction`). Extending `NcEvent` provides integration with
existing QA workflows.

**Implementation Impact**:

- QC discrepancies create `NcEvent` records linked to the sample and QC
  inspection
- Add biorepository-specific fields/types to `NcEvent` if needed
- Leverage existing CAPA resolution workflows

**Future Consideration**: May need to revisit if a more comprehensive CAPA
system is implemented separately.

### DD-005: Barcode Label Printing

**Decision**: PDF generation for standard office printers.

**Rationale**: Avoids specialized hardware dependencies. Standard office
printers with label paper are widely available and cost-effective.

**Implementation Impact**:

- Generate PDF with barcode labels using existing PDF generation libraries
- Support standard label formats (Avery-style sheet labels)
- User downloads PDF and prints to any standard printer
- No direct printer integration or ZPL/EPL commands needed

---

## References

- Source Document: Biorepository_Laboratory_SRS_v2.pdf (AHRI OpenELIS)
- OpenELIS Constitution: `.specify/memory/constitution.md` v1.8.1
- ISO 20387:2018 - Biobanking requirements
- Good Laboratory Practice (GLP) guidelines
- Ethiopian FDA regulations for biological materials
