# Feature Specification: Medical Laboratory Workflow

**Feature Branch**: `001-medical-lab-workflow` **Created**: 2024-12-14
**Status**: Draft **Input**: User description: Medical Laboratory workflow
covering patient registration, sample collection, tracking, storage, processing,
testing, result entry, validation, reporting, accreditation support, and
disposal/archiving

## Executive Summary

Medical laboratories require comprehensive workflow management to handle samples
from collection through disposal. This feature introduces a complete Laboratory
Information System (LIS) workflow module supporting:

- **Patient/Participant Management**: Registration and tracking of patients and
  research participants with lab order management
- **Sample Collection**: Multi-department sample collection (Hematology,
  Chemistry, Parasitology, Urinalysis, Serology, Microbiology) with flexible
  labeling
- **Sample Tracking**: Quality control with pass/fail criteria, IATA
  PI650-compliant transport packaging validation
- **Storage Management**: Hierarchical storage (room → device → rack → box →
  position) with environmental monitoring
- **Sample Processing**: Department-specific processing protocols
  (centrifugation, smears, staining)
- **Testing Phase**: Instrument interfacing, QC monitoring (Levey-Jennings
  charts), EQA support
- **Result Management**: Manual and automated result entry with validation and
  approval workflows
- **Reporting & Analytics**: Turnaround time monitoring, equipment usage, sample
  utilization dashboards
- **Disposal & Archiving**: Compliant disposal workflows with biorepository
  transfer support

**Target Users**: Laboratory technicians, technologists, sample collectors,
supervisors, quality managers, administrators

**Expected Impact**:

- Complete sample traceability from collection to disposal
- Regulatory compliance support (ISO 15189, CAP, CLIA)
- Reduced manual transcription errors through instrument interfacing
- Real-time visibility into laboratory operations and performance metrics

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Patient/Participant Registration and Lab Orders (Priority: P1)

Laboratory staff need to register patients or participants and create lab orders
before sample collection can occur. The system must support both clinical
patients and research participants with scheduled sample collections.

**Why this priority**: This is the entry point for all laboratory workflows.
Without patient/participant registration and lab orders, no samples can be
collected or tracked.

**Independent Test**: Register a new patient with demographic data, create a lab
order for CBC and Chemistry panel, verify patient and order appear in the
system.

**Acceptance Scenarios**:

1. **Given** the patient registration page is open, **When** the user enters
   patient name, age, sex, and contact information, **Then** a new patient
   record is created with a unique patient ID
2. **Given** a patient record exists, **When** the user creates a lab order
   selecting tests (CBC, LFT, FBS), **Then** the order is created with pending
   status and linked to the patient
3. **Given** a participant is enrolled in a study, **When** the user sets up a
   collection schedule (e.g., Day 0, Day 7, Day 14), **Then** scheduled
   collection dates are created and visible on the participant dashboard
4. **Given** lab orders exist for a patient, **When** viewing the patient
   profile, **Then** all pending and completed orders are displayed with their
   status

---

### User Story 2 - Sample Collection and Labeling (Priority: P1)

Laboratory technicians, technologists, or sample collectors need to collect
samples from patients/participants, label them appropriately, and record
collection details including sample type and container information.

**Why this priority**: Sample collection is the first physical step in the
workflow. Accurate collection and labeling is critical for downstream processes.

**Independent Test**: Create a sample collection for an existing lab order,
enter sample type (Whole Blood/EDTA), apply label, verify sample appears in
tracking queue.

**Acceptance Scenarios**:

1. **Given** a lab order exists for Hematology tests, **When** the technician
   collects a blood sample and selects container type (EDTA vacutainer),
   **Then** the sample is created with a unique barcode/ID and linked to the
   order
2. **Given** the collection form is open, **When** the user enters free-text
   alphanumeric label (e.g., "PT001-DAY0-CHEM"), **Then** the label is saved and
   can be used for tracking
3. **Given** multiple sample types are needed (blood, urine, stool), **When**
   the collector creates samples for each, **Then** each sample is assigned
   appropriate container type and storage requirements
4. **Given** a participant has a scheduled collection, **When** the technician
   opens their profile, **Then** the scheduled collection is highlighted and can
   be started with pre-filled order information
5. **Given** sample collection is complete, **When** the user records collection
   time and collector ID, **Then** the collection metadata is saved for audit
   purposes

---

### User Story 3 - Sample Reception and Quality Control (Priority: P1)

Laboratory staff need to receive samples into the tracking system, perform
quality checks, and accept or reject samples based on defined criteria specific
to each sample type.

**Why this priority**: Quality control at reception prevents downstream errors
and wasted resources on unsuitable samples.

**Independent Test**: Receive 5 samples, mark 4 as accepted (quality checks
pass), reject 1 for hemolysis, verify rejection triggers corrective action
workflow.

**Acceptance Scenarios**:

1. **Given** samples arrive at the laboratory, **When** the technician scans the
   barcode, **Then** the sample details are displayed including patient info,
   tests requested, and collection time
2. **Given** a Chemistry sample is being received, **When** the technician
   checks quality criteria, **Then** options include: volume (<3ml), hemolysis,
   lipemia, icterus, delay (>1 hour), leakage
3. **Given** a Hematology sample is being received, **When** checking quality,
   **Then** anticoagulant type (EDTA, citrate, heparin, fluoride), delay (>4
   hours), clotting, volume (<2ml) are validated
4. **Given** a Stool sample is received delayed (>30 minutes), **When** the
   technician marks quality check as failed, **Then** the sample status changes
   to "Rejected" with reason "Delayed >30 minutes"
5. **Given** a sample is rejected, **When** the rejection is confirmed, **Then**
   corrective action options are presented: recollection or return to submitter
6. **Given** all quality checks pass, **When** the technician clicks "Accept",
   **Then** the sample moves to "Accepted" status and is ready for allocation

---

### User Story 4 - Transport Packaging Validation (Priority: P2)

Laboratory staff need to document and validate transport packaging compliance
for samples being shipped or received, including primary, secondary, and
tertiary packaging per IATA PI650 standards.

**Why this priority**: Transport compliance is critical for sample integrity and
regulatory requirements, but slightly lower priority than core sample handling.

**Independent Test**: Record transport packaging details for an incoming
shipment, validate IATA PI650 compliance, document condition on arrival.

**Acceptance Scenarios**:

1. **Given** samples are being received from external source, **When**
   inspecting primary packaging, **Then** the user can record: container type
   (vacutainer, cryovial, urine cup), seal status (intact/leaking/damaged),
   barcode presence, absorbent material
2. **Given** secondary packaging is inspected, **When** documenting status,
   **Then** options include: packaging type (biohazard bag, canister, sealed
   pouch), integrity check, watertight compliance, container quantity, inspector
   initials/timestamp
3. **Given** tertiary packaging is inspected, **When** recording details,
   **Then** system captures: transport box type, labeling status (biohazard,
   orientation arrows, sender/receiver), temperature logger ID, courier details,
   arrival condition
4. **Given** all packaging levels are documented, **When** completing
   inspection, **Then** IATA PI650 compliance is calculated and displayed
   (Yes/No with non-compliant items highlighted)
5. **Given** samples were transported, **When** recording status, **Then**
   transportation timing is captured (on time/delayed)

---

### User Story 5 - Sample Allocation to Departments (Priority: P1)

Laboratory staff need to allocate accepted samples to specific testing
departments (Hematology, Chemistry, Parasitology, Urinalysis, Serology,
Microbiology) based on the ordered tests.

**Why this priority**: Sample allocation routes samples to correct workflows;
blocking if not done correctly.

**Independent Test**: Allocate a multi-test order (CBC + LFT + Urinalysis) to
three departments, verify samples appear in each department's worklist.

**Acceptance Scenarios**:

1. **Given** an accepted sample has orders for CBC and Differential, **When**
   auto-allocation runs, **Then** the sample is allocated to Hematology
   department
2. **Given** an accepted sample has orders for LFT, KFT, and Lipid Profile,
   **When** allocated, **Then** the sample appears in Chemistry department
   worklist
3. **Given** an accepted sample has Ova & Parasite test ordered, **When**
   allocated, **Then** the sample is routed to Parasitology
4. **Given** a sample has HIV, HBsAg, and HCV tests, **When** allocated,
   **Then** the sample is routed to Serology department
5. **Given** allocation is complete, **When** viewing department worklist,
   **Then** samples show with patient ID, collection time, and ordered tests

---

### User Story 6 - Hierarchical Storage Management (Priority: P2)

Laboratory staff need to store samples in a hierarchical storage system (room →
device → rack → box/tray → position) with cryo-box mapping and well position
tracking.

**Why this priority**: Proper storage is essential for sample preservation but
can happen in parallel with other workflows.

**Independent Test**: Store 10 aliquots in a cryo-box, assign positions 1-10 in
wells, verify box map shows occupied positions.

**Acceptance Scenarios**:

1. **Given** a sample needs storage, **When** selecting a location, **Then** the
   hierarchy is: Room → Refrigerator/Freezer → Drawer/Rack → Cryo-Box → Well
   Position
2. **Given** a cryo-box is selected (e.g., A01 for aliquot 1), **When** viewing
   the box map, **Then** 81 wells are displayed (1-81) with occupied/available
   status
3. **Given** aliquot 1 samples use A01-A100 boxes and aliquot 2 uses B01-B100,
   **When** storing, **Then** the system validates correct box series for
   aliquot type
4. **Given** samples are stored, **When** searching by sample ID, **Then** the
   exact location (Room/Device/Rack/Box/Position) is returned
5. **Given** a box has samples, **When** viewing the box layout, **Then** a
   visual grid shows sample IDs in their well positions

---

### User Story 7 - Environmental Monitoring (Priority: P2)

Laboratory staff need to record and monitor environmental conditions
(temperature) for storage locations, with twice-daily recording requirements and
alarm support.

**Why this priority**: Environmental monitoring is critical for sample integrity
but can be built after core storage functionality.

**Independent Test**: Record AM and PM temperatures for a -80C freezer for 3
days, verify readings appear in monitoring dashboard, trigger alert when out of
range.

**Acceptance Scenarios**:

1. **Given** a storage device is configured, **When** recording temperature,
   **Then** room temperature, refrigerator (2-8C), and freezer (-20C, -80C) can
   be logged
2. **Given** temperature monitoring is required, **When** the technician records
   readings, **Then** AM and PM entries are captured with timestamp and user
3. **Given** temperature exceeds acceptable range, **When** logged, **Then** an
   alert/alarm is generated and documented as a temperature excursion
4. **Given** a data logger is connected, **When** configured, **Then**
   temperature readings can be automatically imported and linked to storage
   locations
5. **Given** environmental data exists, **When** generating reports, **Then**
   temperature records can be exported for stakeholder reporting

---

### User Story 8 - Sample Processing by Department (Priority: P1)

Laboratory staff need to process samples according to department-specific
protocols (centrifugation, smear preparation, staining) and create aliquots or
derived samples.

**Why this priority**: Sample processing is required before testing can occur;
core workflow step.

**Independent Test**: Process a Chemistry sample (centrifuge, separate serum),
create 2 aliquots, verify parent-child relationship maintained.

**Acceptance Scenarios**:

1. **Given** a Chemistry sample is ready for processing, **When** the technician
   performs centrifugation and serum separation, **Then** processing steps are
   recorded with timestamps
2. **Given** a Hematology sample requires smear, **When** preparing thin smear
   with Wright stain, **Then** processing method and stain used are documented
3. **Given** a Parasitology sample needs preparation, **When** creating
   thick/thin smears with Giemsa stain, **Then** preparation details are
   captured
4. **Given** a Microbiology sample needs processing, **When** performing Gram
   stain, AFB, Indian Ink, or KOH prep, **Then** method selection and results
   are recorded
5. **Given** a sample is subdivided into aliquots, **When** creating child
   samples, **Then** parent-child relationship is maintained with common
   identifiers
6. **Given** aliquots are created (tubes, slides, plates, vials), **When**
   labeled, **Then** each bears the collection tube identifier plus aliquot
   sequence

---

### User Story 9 - Testing and Instrument Integration (Priority: P1)

Laboratory staff need to perform tests using analyzers with electronic worklist
generation, automatic result upload, and reference range flagging.

**Why this priority**: Testing is the core function of the laboratory;
instrument integration eliminates transcription errors.

**Independent Test**: Generate worklist for Chemistry analyzer, run 10 samples,
verify results auto-upload with reference range flags.

**Acceptance Scenarios**:

1. **Given** samples are ready for testing, **When** generating a worklist,
   **Then** an electronic worklist is created for the analyzer with sample IDs
   and tests
2. **Given** an analyzer is interfaced (Hematology/Chemistry), **When** results
   are produced, **Then** values are automatically uploaded to the sample
   records
3. **Given** results are uploaded, **When** compared to reference ranges,
   **Then** abnormal values are automatically flagged (H=High, L=Low,
   C=Critical)
4. **Given** a sample has screening eligibility criteria, **When** testing
   complete, **Then** eligibility status is flagged in the system
5. **Given** Chemistry testing is performed, **When** documenting method,
   **Then** options include: spectrophotometry, ISE, colorimetry, potentiometry,
   enzymatic reactions
6. **Given** Hematology testing is performed, **When** documenting method,
   **Then** options include: flow-cytometry, photometry, electrical impedance,
   light scatter

---

### User Story 10 - Quality Control and Calibration (Priority: P2)

Laboratory staff need to run QC controls (dual level: normal and pathologic),
track calibration, monitor QC with Levey-Jennings charts, and participate in
External Quality Assessment.

**Why this priority**: QC is essential for result reliability but can be built
as enhancement to core testing.

**Independent Test**: Run normal and pathologic QC for Chemistry, record
results, verify Levey-Jennings chart updates, flag when out of control.

**Acceptance Scenarios**:

1. **Given** daily QC is required, **When** running controls, **Then**
   dual-level (Normal and Pathologic) results are recorded with pass/fail status
2. **Given** calibration is needed, **When** performing monthly calibration (or
   as needed), **Then** calibration date, values, and technician are documented
3. **Given** QC results are logged over time, **When** viewing Levey-Jennings
   chart, **Then** control values are plotted against mean +/-1SD, +/-2SD,
   +/-3SD lines
4. **Given** QC fails (Westgard rule violation), **When** reviewing, **Then**
   deviation is captured with root cause analysis and corrective action (rerun,
   recalibrate, maintenance)
5. **Given** EQA participation is required, **When** quarterly EQA is performed,
   **Then** results and comparison scores are documented
6. **Given** qualitative tests are run, **When** performing QC, **Then**
   positive and negative control results are recorded

---

### User Story 11 - Result Entry (Manual and Automated) (Priority: P1)

Laboratory staff need to enter results manually for tests without instrument
interfacing, or verify automatically uploaded results.

**Why this priority**: Result entry is required to complete the testing phase.

**Independent Test**: Enter manual results for urinalysis microscopy (WBC count,
RBC count), verify results saved to sample record.

**Acceptance Scenarios**:

1. **Given** a manual test is completed (e.g., microscopy), **When** the
   technician enters results, **Then** values are saved with entry timestamp and
   user
2. **Given** machine results are uploaded, **When** reviewing, **Then** the
   technician can verify or edit values before acceptance
3. **Given** results are entered, **When** validating, **Then** system checks
   for values outside panic/critical ranges and alerts
4. **Given** multiple tests on one sample, **When** entering results, **Then**
   all test results for the sample are displayed on one screen

---

### User Story 12 - Result Validation and Approval (Priority: P1)

Laboratory supervisors need to validate and approve results before release, with
department-specific approval authority (Chemistry, Hematology, Parasitology,
etc.).

**Why this priority**: Result validation ensures quality and is required before
patient reporting.

**Independent Test**: Submit 5 results for validation, approve 4, reject 1 with
reason, verify approved results are ready for reporting.

**Acceptance Scenarios**:

1. **Given** results are entered, **When** submitted for validation, **Then**
   they appear in the validation queue for the appropriate department
2. **Given** validation queue is displayed, **When** a supervisor reviews
   results, **Then** they can approve, reject (with reason), or request retest
3. **Given** approval is department-specific, **When** a Chemistry supervisor
   logs in, **Then** they only see Chemistry results for approval
4. **Given** reference levels need adjustment, **When** configuring, **Then**
   reference ranges can be modified by authorized personnel
5. **Given** results are approved, **When** status changes, **Then** results are
   marked ready for reporting and cannot be modified without audit trail

---

### User Story 13 - Reporting and Result Delivery (Priority: P1)

Laboratory staff need to generate and print result reports, with controlled
release only after validation approval.

**Why this priority**: Result delivery is the final output of testing workflow.

**Independent Test**: Generate PDF report for approved results, verify report
includes patient info, test results, reference ranges, and validation signature.

**Acceptance Scenarios**:

1. **Given** results are approved, **When** generating report, **Then** a
   formatted report is created with patient demographics, test results,
   reference ranges, and flags
2. **Given** report is generated, **When** printing, **Then** the print includes
   approval signature/electronic verification
3. **Given** results are not yet approved, **When** attempting to print,
   **Then** system blocks printing with message "Results pending validation"
4. **Given** report is delivered, **When** documenting, **Then** delivery
   method, recipient, and timestamp are recorded

---

### User Story 14 - Laboratory Performance Dashboards (Priority: P2)

Laboratory leadership needs dashboards showing key performance metrics:
turnaround time, sample acceptance rate, QC pass rates, equipment usage, and
corrective actions.

**Why this priority**: Analytics and dashboards provide operational visibility
but are not blocking for core workflows.

**Independent Test**: View TAT dashboard showing average time from collection to
result for each test type, verify data updates in real-time.

**Acceptance Scenarios**:

1. **Given** the dashboard is accessed, **When** viewing TAT metrics, **Then**
   turnaround time by test type is displayed with targets and actual performance
2. **Given** sample reception data exists, **When** viewing acceptance metrics,
   **Then** sample acceptance rate (accepted vs. rejected) is shown with
   rejection reasons breakdown
3. **Given** QC data is logged, **When** viewing QC dashboard, **Then** QC
   pass/fail rates are displayed: how often, when, by whom
4. **Given** equipment is used, **When** viewing usage report, **Then**
   instrument uptime, usage hours, and maintenance records are shown
5. **Given** corrective actions are recorded, **When** viewing deviation report,
   **Then** summary shows: action type, when taken, by whom, for whom (analyst)
6. **Given** assay tracking is enabled, **When** viewing completion metrics,
   **Then** rate of assays completed without interruption is displayed

---

### User Story 15 - Sample Utilization and Depletion Tracking (Priority: P2)

Laboratory staff need to track sample utilization (used/remaining) and mark
samples as depleted when exhausted.

**Why this priority**: Inventory tracking is important but secondary to core
testing workflows.

**Independent Test**: Mark a sample as partially used (2ml of 5ml), later mark
as depleted, verify status update and audit trail.

**Acceptance Scenarios**:

1. **Given** a sample is used for testing, **When** recording usage, **Then**
   volume/quantity used is deducted from available amount
2. **Given** a sample is exhausted, **When** marking as depleted, **Then**
   status changes to "Depleted" with timestamp and user
3. **Given** sample utilization is tracked, **When** viewing sample details,
   **Then** history shows all usage events with volumes and purposes
4. **Given** generating reports, **When** selecting utilization report, **Then**
   summary shows total samples, utilized, remaining, and depleted counts

---

### User Story 16 - Bioequivalence Study Support (Priority: P3)

Laboratory staff need to support bioequivalence (BE) studies by storing samples
in deep freezer (-80C) for two years and transferring to bioanalytical lab.

**Why this priority**: Specialized BE workflow is lower priority than general
laboratory functions.

**Independent Test**: Store BE sample with 2-year retention flag, transfer to
bioanalytical lab, verify chain of custody recorded.

**Acceptance Scenarios**:

1. **Given** a sample is designated for BE study, **When** storing, **Then**
   retention period is set to 2 years with -80C requirement
2. **Given** a BE sample needs transfer, **When** shipping to bioanalytical lab,
   **Then** transfer record captures: destination lab, shipment date, courier,
   conditions
3. **Given** samples after testing, **When** determining disposition, **Then**
   options include: store for retention period or discard (if not BE)

---

### User Story 17 - Sample Disposal (Priority: P2)

Laboratory staff need to dispose of samples following international guidelines,
with proper documentation of disposal method, date, time, and authorized
personnel.

**Why this priority**: Disposal is end-of-lifecycle; important for compliance
but not blocking active testing.

**Independent Test**: Dispose 10 blood samples by incineration, record disposal
details, verify audit trail complete.

**Acceptance Scenarios**:

1. **Given** samples are ready for disposal, **When** checking criteria,
   **Then** disposal reasons include: expiry, exhaustion, failed QC, safety
   concerns
2. **Given** blood/stool samples require incineration, **When** disposing,
   **Then** disposal method is recorded as "Incineration" with licensed facility
   details
3. **Given** urine/analyzer waste requires chemical treatment, **When**
   disposing, **Then** method is recorded as "Chemical Treatment" before sewage
   disposal
4. **Given** disposal is performed, **When** documenting, **Then** date, time,
   initials of disposing personnel, and facility accreditation are captured
5. **Given** disposal party must be licensed, **When** recording, **Then**
   license/accreditation of disposing entity is verified and documented

---

### User Story 18 - Data Archiving and Biobanking Transfer (Priority: P3)

Laboratory staff need to permanently archive key data, metadata, and analysis
results, and transfer samples to biobank when required.

**Why this priority**: Archiving is long-term retention; lowest priority for
initial implementation.

**Independent Test**: Archive complete results for a closed study, transfer
remaining samples to biobank, verify all data accessible in archive.

**Acceptance Scenarios**:

1. **Given** a project is completed, **When** archiving, **Then** all results,
   QC data, and audit trails are permanently stored
2. **Given** samples need biobank transfer, **When** transferring, **Then**
   chain of custody, storage conditions, and recipient details are documented
3. **Given** retention policies are defined, **When** configuring, **Then**
   retention periods can be set by sample type, project, or regulation
4. **Given** archived data is needed, **When** searching, **Then** historical
   records are retrievable with complete traceability

---

### User Story 19 - Accreditation Support (Priority: P3)

The system needs to support laboratory accreditation requirements (ISO 15189,
CAP, CLIA) through comprehensive audit trails, document control, and compliance
reporting.

**Why this priority**: Accreditation support is valuable but can be enhanced
iteratively after core functions work.

**Independent Test**: Generate audit trail report for a sample from collection
to result, verify all actions documented with user, timestamp, and action type.

**Acceptance Scenarios**:

1. **Given** audit is required, **When** reviewing sample history, **Then**
   complete chain of custody is displayed from collection to disposition
2. **Given** accreditation body requires documentation, **When** generating
   compliance reports, **Then** QC records, calibration logs, and corrective
   actions are exportable
3. **Given** document control is needed, **When** managing SOPs, **Then**
   version control, approval workflows, and training records are maintained

---

### Edge Cases

- What happens when a scheduled collection is missed? **System MUST mark
  collection as "Missed" and allow rescheduling with documented reason.**
- How does system handle partial sample collection (insufficient volume)?
  **System MUST allow acceptance with "Insufficient Volume" flag if minimum
  testable amount is present; otherwise reject.**
- What happens if QC fails during a batch run? **System MUST hold all results
  from that run pending investigation; only release after corrective action
  documented.**
- How does system handle analyzer communication failure? **System MUST log
  communication error, allow manual result entry as fallback, flag as "Manual
  Entry - Analyzer Offline".**
- What happens when storage location reaches capacity? **System MUST warn user
  and suggest alternative locations; block assignment if no capacity
  available.**
- How does system handle concurrent users on same sample? **System MUST use
  optimistic locking; second user sees "Sample modified by another user - please
  refresh".**
- What happens if temperature excursion affects stored samples? **System MUST
  flag all affected samples, trigger investigation workflow, and document
  resolution before samples can be used.**

## Requirements _(mandatory)_

### Functional Requirements

#### Patient/Participant Management

- **FR-001**: System MUST support patient registration with name, age, sex, and
  contact information (reuse existing patient services)
- **FR-002**: System MUST support participant registration for research studies
  with enrollment status
- **FR-003**: System MUST allow creation of lab orders linked to
  patients/participants
- **FR-004**: System MUST support scheduled sample collections for participants
  with predefined visit schedules
- **FR-005**: System MUST distinguish between patient samples and participant
  samples

#### Sample Collection

- **FR-010**: System MUST support sample collection for multiple sample types:
  Hematology (CBC, Diff), Chemistry (LFT, KFT, Lipid Profile, FBS), Urinalysis,
  Stool, Serology (HIV, HBsAg, HCV), and others (ECG integration, Pregnancy
  test)
- **FR-011**: System MUST support free-text alphanumeric labeling for samples
- **FR-012**: System MUST assign unique barcodes/identifiers to each sample
- **FR-013**: System MUST record collector identity and collection timestamp
- **FR-014**: System MUST capture container type (vacutainer, cryovial, urine
  cup, stool jar, swab tube)

#### Sample Reception and Quality Control

- **FR-020**: System MUST support sample reception with barcode scanning
- **FR-021**: System MUST enforce sample-type-specific quality criteria:
  - Chemistry: delay (>1 hour), leak, volume (<3ml), hemolysis, lipemia, icterus
  - Hematology: anticoagulant type (EDTA, citrate, heparin, fluoride), delay (>4
    hours), clotting, volume (<2ml), hemolysis
  - Stool: delay (>30 minutes), volume (<pea grain), leak, urine contamination,
    improper container
  - Urine: delay (>30 minutes), volume (<10ml), leak, stool contamination
  - Microbiology: visible contamination
- **FR-022**: System MUST record Accept (A) or Reject (R) status for each sample
- **FR-023**: System MUST capture corrective action for rejected samples:
  recollection or return to submitter
- **FR-024**: System MUST verify lab orders match collected samples

#### Transport Packaging Validation

- **FR-030**: System MUST capture primary packaging details: container type,
  seal status, barcode presence, absorbent material
- **FR-031**: System MUST capture secondary packaging: type, integrity,
  watertight compliance, container count, inspector initials, timestamp, receipt
  condition
- **FR-032**: System MUST capture tertiary packaging: box type, labeling status,
  temperature logger ID, courier details, arrival condition
- **FR-033**: System MUST calculate and display IATA PI650 compliance status
- **FR-034**: System MUST record transportation timing status (on time/delayed)
- **FR-035**: System MUST capture storage temperature at collection: room
  temperature (2-8C), frozen, N/A

#### Sample Allocation and Routing

- **FR-040**: System MUST allocate samples to departments based on ordered
  tests: Hematology, Chemistry, Parasitology, Urinalysis, Serology, Microbiology
- **FR-041**: System MUST display department-specific worklists with pending
  samples
- **FR-042**: System MUST support routing samples to external labs
  (bioanalytical, reference labs)

#### Storage Management

- **FR-050**: System MUST support hierarchical storage: room → device
  (refrigerator/freezer) → drawer/rack → cryo-box → well position
- **FR-051**: System MUST support cryo-box labeling convention: A01-A100 for
  aliquot 1, B01-B100 for aliquot 2
- **FR-052**: System MUST support cryo-box mapping with 81 wells (1-81)
- **FR-053**: System MUST display visual box layout showing occupied/available
  positions
- **FR-054**: System MUST support sample lookup by ID returning exact storage
  location

#### Environmental Monitoring

- **FR-060**: System MUST support temperature recording for storage locations
  (room, refrigerator, freezer)
- **FR-061**: System MUST require twice-daily temperature readings (AM and PM)
- **FR-062**: System MUST generate alerts for temperature excursions outside
  acceptable ranges
- **FR-063**: System MUST support integration with external data loggers
- **FR-064**: System MUST generate temperature reports for stakeholder reporting

#### Sample Processing

- **FR-070**: System MUST support department-specific processing methods:
  - Chemistry/Serology: centrifugation, serum/plasma separation
  - Hematology: thin smear, Wright stain
  - Parasitology: thin/thick smear, Giemsa stain
  - Microbiology: Gram stain, AFB, Indian Ink, KOH
  - Urinalysis: centrifugation/sedimentation, wet smear
  - Stool: wet smear, concentration (sedimentation/floatation)
- **FR-071**: System MUST support aliquot creation with parent-child
  relationships
- **FR-072**: System MUST maintain common identifiers between parent samples and
  aliquots/derivatives
- **FR-073**: System MUST support labeling of derived samples: tubes, slides,
  plates, vials

#### Testing and Instrument Integration

- **FR-080**: System MUST generate electronic worklists for analyzers
- **FR-081**: System MUST support bidirectional instrument interfacing for
  result upload
- **FR-082**: System MUST apply automatic reference range checks and flag
  abnormal results
- **FR-083**: System MUST support test-specific methodologies documentation
- **FR-084**: System MUST support screening eligibility flagging

#### Quality Control

- **FR-090**: System MUST support dual-level QC (Normal and Pathologic) with
  daily recording
- **FR-091**: System MUST support monthly calibration recording (or as needed)
- **FR-092**: System MUST generate Levey-Jennings charts for QC monitoring
- **FR-093**: System MUST support method comparison and verification recording
- **FR-094**: System MUST support quarterly External Quality Assessment (EQA)
  documentation
- **FR-095**: System MUST capture deviations with root cause analysis and
  corrective actions
- **FR-096**: System MUST support positive/negative control recording for
  qualitative tests

#### Result Entry and Validation

- **FR-100**: System MUST support manual result entry with technician ID and
  timestamp
- **FR-101**: System MUST support automatic result upload from interfaced
  instruments
- **FR-102**: System MUST support result validation workflow with
  department-specific queues
- **FR-103**: System MUST support configurable reference ranges
- **FR-104**: System MUST restrict result release until validated by authorized
  personnel
- **FR-105**: System MUST support result rejection with reason and retest
  request

#### Reporting and Analytics

- **FR-110**: System MUST calculate and display turnaround time (TAT) by test
  type
- **FR-111**: System MUST track sample acceptance rates with rejection reason
  breakdown
- **FR-112**: System MUST display QC pass/fail rates with operator and timing
  details
- **FR-113**: System MUST track equipment usage: uptime, duration, operator
- **FR-114**: System MUST track corrective actions: when, by whom, for whom
- **FR-115**: System MUST provide dashboards for laboratory leadership
- **FR-116**: System MUST support sample utilization and depletion tracking
- **FR-117**: System MUST report temperature excursions and storage incidents to
  stakeholders

#### Disposal and Archiving

- **FR-120**: System MUST enforce disposal criteria: expiry, exhaustion, failed
  QC, safety concerns
- **FR-121**: System MUST document disposal: date, time, personnel initials,
  method
- **FR-122**: System MUST enforce disposal methods by sample type:
  - Blood, stool: incineration
  - Urine, analyzer waste: chemical treatment before sewage disposal
- **FR-123**: System MUST verify disposing party license/accreditation
- **FR-124**: System MUST support permanent data archiving with metadata
  preservation
- **FR-125**: System MUST support biobank transfer with chain of custody
  documentation
- **FR-126**: System MUST enforce retention policies (2 years at -80C for BE
  samples)

#### Accreditation Support

- **FR-130**: System MUST maintain complete audit trails for all sample
  operations
- **FR-131**: System MUST support accreditation standards (ISO 15189, CAP, CLIA)
- **FR-132**: System MUST generate compliance reports on demand

### Key Entities

#### Core Entities (New or Extended)

- **MedLabSample**: Extended sample entity with medical lab-specific attributes:
  sample category (patient/participant), collection temperature, transport
  packaging details, QC status, allocation department
- **QualityCheck**: Quality control check results per sample with
  criteria-specific pass/fail and rejection reasons
- **TransportPackaging**: Primary, secondary, tertiary packaging details with
  IATA PI650 compliance tracking
- **EnvironmentalReading**: Temperature readings for storage locations with
  timestamp, operator, and alert status
- **ProcessingRecord**: Sample processing details: method, operator, timestamp,
  derived samples
- **TestResult**: Test result with value, unit, reference range, flags, and
  validation status
- **QCResult**: Quality control result with level (normal/pathologic), value,
  target, acceptable range, Westgard rule status
- **ValidationRecord**: Result validation with approver, timestamp, action
  (approve/reject/retest), comments
- **DisposalRecord**: Disposal documentation: date, time, method, operator,
  facility, compliance verification
- **EquipmentUsageLog**: Equipment/instrument usage tracking: start time, end
  time, operator, purpose

#### Existing Entities to Reuse

- **Patient**: Existing patient registration (extend for participant support)
- **LabOrder/TestRequest**: Existing lab order system
- **SampleItem**: Existing sample tracking (extend with medical lab attributes)
- **StorageLocation**: Existing hierarchical storage (extend for cryo-box well
  mapping)
- **Analyzer**: Existing analyzer configuration for interfacing
- **SystemUser**: Existing user management for audit trails

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Sample collection to result reporting TAT meets defined targets
  per test type (e.g., <4 hours for STAT, <24 hours for routine)
- **SC-002**: 100% of samples have complete chain of custody from collection to
  disposal/archiving
- **SC-003**: Sample rejection rate <5% with documented corrective actions for
  all rejections
- **SC-004**: QC pass rate >95% with all failures documented and resolved within
  24 hours
- **SC-005**: Instrument-interfaced result entry eliminates 100% of
  transcription errors vs. manual entry
- **SC-006**: Temperature excursions detected within 1 recording cycle (AM/PM)
  with 100% documentation
- **SC-007**: User can locate any stored sample's exact position within 10
  seconds via search
- **SC-008**: Dashboard displays real-time laboratory performance metrics with
  <30 second refresh
- **SC-009**: Disposal compliance: 100% of disposed samples have documented
  method, date, and authorized personnel
- **SC-010**: System supports laboratory accreditation audits with complete
  audit trail retrieval within 2 minutes for any sample

### Quality Gates

- **QG-001**: All sample operations complete within 3 seconds for individual
  actions
- **QG-002**: Bulk operations (up to 100 samples) complete within 30 seconds
- **QG-003**: Report generation completes within 60 seconds for standard reports
- **QG-004**: System supports 20 concurrent users without degradation
- **QG-005**: All data operations maintain ACID compliance for audit trail
  integrity

## Dependencies

### Existing Services to Reuse (Backend)

The following existing backend services will be reused directly or extended:

| Service                     | Package                    | Reuse Strategy | Notes                                    |
| --------------------------- | -------------------------- | -------------- | ---------------------------------------- |
| **PatientService**          | `patient.service`          | REUSE AS-IS    | Patient registration and lookup          |
| **SampleService**           | `sample.service`           | REUSE AS-IS    | Core sample tracking                     |
| **SampleItemService**       | `sampleitem.service`       | EXTEND         | Add collection_temperature, custom_label |
| **SampleManagementService** | `sampleitem.service`       | REUSE AS-IS    | Aliquoting, test management              |
| **StorageLocationService**  | `storage.service`          | REUSE AS-IS    | Full hierarchy support exists            |
| **SampleStorageService**    | `storage.service`          | REUSE AS-IS    | Assignment, movement, disposal           |
| **AnalyzerService**         | `analyzer.service`         | REUSE AS-IS    | Instrument integration                   |
| **AnalysisService**         | `analysis.service`         | REUSE AS-IS    | Test order lifecycle                     |
| **ResultService**           | `result.service`           | EXTEND         | Add panic value alerting                 |
| **ResultValidationService** | `resultvalidation.service` | REUSE AS-IS    | Approval workflow                        |
| **NoteBookService**         | `notebook.service`         | REUSE          | Workflow templating framework            |
| **QaEventService**          | `qaevent.service`          | REUSE          | Adapt for sample QC events               |
| **NoteService**             | `note.service`             | REUSE AS-IS    | Documentation attachment                 |
| **AuditTrailService**       | `audittrail.dao`           | REUSE AS-IS    | Compliance logging                       |

### Existing UI Components to Reuse (Frontend)

The following existing React components will be reused or extended:

| Component                         | Path                 | Reuse Strategy | Notes                           |
| --------------------------------- | -------------------- | -------------- | ------------------------------- |
| **StorageDashboard.jsx**          | `storage/`           | REUSE AS-IS    | Complete storage management     |
| **SampleReceptionPage.js**        | `notebook/pages/`    | EXTEND         | Add QC checklist for medlab     |
| **InitialProcessingPage.js**      | `notebook/pages/`    | EXTEND         | Add department-specific methods |
| **ChildSampleCreationPage.js**    | `notebook/pages/`    | REUSE AS-IS    | Aliquot creation                |
| **ResultCompilationPage.js**      | `notebook/pages/`    | REUSE AS-IS    | Result export/delivery          |
| **FreezerMonitoringDashboard.js** | `coldStorage/`       | EXTEND         | Add twice-daily schedule        |
| **SearchResultForm.js**           | `resultPage/`        | REUSE AS-IS    | Result entry/viewing            |
| **Validation.js**                 | `validation/`        | EXTEND         | Add department queue filtering  |
| **Dashboard.tsx**                 | `home/`              | EXTEND         | Add medlab-specific metrics     |
| **Workplan.js**                   | `workplan/`          | REUSE AS-IS    | Worklist management             |
| **SampleGrid.js**                 | `notebook/workflow/` | REUSE AS-IS    | Sample display grid             |
| **StorageHierarchySelector.js**   | `notebook/workflow/` | REUSE AS-IS    | Location picker                 |
| **BoxLayoutViewer.js**            | `notebook/workflow/` | REUSE AS-IS    | Box visualization               |

### New Components Required

Only the following genuinely new components are needed:

**Backend (New Entities/Services):**

- **QualityCheck** entity + service - Sample-type-specific QC criteria
- **TransportPackaging** entity + service - IATA PI650 compliance tracking
- **QCResult** entity + service - Levey-Jennings QC data
- **EquipmentUsageLog** entity - Instrument usage tracking

**Frontend (New Pages):**

- **PatientOrderEntryPage.js** - Simplified inline patient registration + lab
  order form
- **SampleCollectionPage.js** - Create samples from orders
- **MedLabQCDashboard.js** - Levey-Jennings charting (Carbon Charts)
- **TransportPackagingForm.js** - IATA PI650 compliance form
- **QualityCheckForm.js** - Sample-type-specific QC criteria checklist

## Out of Scope

- Billing and invoicing integration
- Electronic Medical Records (EMR) integration beyond lab results
- Mobile application (future enhancement)
- Real-time instrument monitoring (beyond result upload)
- Multi-site/network laboratory coordination
- Advanced analytics/machine learning predictions

## UI Pages Summary

The Medical Lab workflow is implemented as a **Notebook workflow** using the
existing notebook framework. All pages are embedded within the Notebook - users
never navigate away from the workflow context.

### Architecture

- **Patient-centric workflow**: Patient → Order → Sample → Processing → Results
- **Sample tracking**: Cross-cutting concern visible in every page via
  `SampleGrid.js`
- **Status progression**:
  `COLLECTED → RECEIVED → ACCEPTED → ALLOCATED → PROCESSING → TESTED → VALIDATED → REPORTED → STORED/DISPOSED`

### Medical Lab Notebook Pages

| #   | Page                         | Description                                                       | Strategy | Base Component                  |
| --- | ---------------------------- | ----------------------------------------------------------------- | -------- | ------------------------------- |
| 1   | **Patient & Lab Order**      | Register patient (with inline search), create lab order           | NEW      | `PatientOrderEntryPage.js`      |
| 2   | **Sample Collection**        | Record specimen collection: container, label, collector, time     | NEW      | `SampleCollectionPage.js`       |
| 3   | **Sample Reception**         | Receive at lab: scan barcode, verify order, check transport       | EXTEND   | `SampleReceptionPage.js`        |
| 4   | **Quality Control**          | Sample-type-specific QC (hemolysis, volume, delay), accept/reject | NEW      | `QualityCheckPage.js`           |
| 5   | **Transport Packaging**      | IATA PI650 compliance documentation (P2)                          | NEW      | `TransportPackagingPage.js`     |
| 6   | **Sample Allocation**        | Route to departments based on ordered tests                       | EXTEND   | `SampleRoutingPage.js`          |
| 7   | **Sample Processing**        | Centrifugation, smears, staining, aliquot creation                | EXTEND   | `InitialProcessingPage.js`      |
| 8   | **Storage Assignment**       | Assign samples to storage locations                               | EMBED    | `StorageHierarchySelector.js`   |
| 9   | **Environmental Monitoring** | Temperature recording, alerts, excursions                         | EMBED    | `FreezerMonitoringDashboard.js` |
| 10  | **Testing & Worklist**       | Analyzer worklist, result import, manual entry                    | EXTEND   | `AnalysisPage.js`               |
| 11  | **QC Dashboard**             | Levey-Jennings charts, Westgard rules, calibration, EQA           | NEW      | `QCDashboardPage.js`            |
| 12  | **Result Validation**        | Department queues, approve/reject/retest workflow                 | EMBED    | `Validation.js`                 |
| 13  | **Result Reporting**         | Generate reports, track delivery                                  | REUSE    | `ResultCompilationPage.js`      |
| 14  | **Lab Dashboard**            | TAT metrics, acceptance rates, equipment usage, KPIs              | EMBED    | `Dashboard.tsx` components      |
| 15  | **Sample Utilization**       | Track usage, mark depletion                                       | EXTEND   | `SampleGrid.js` + actions       |
| 16  | **Disposal/Archiving**       | Dispose or archive with compliance documentation                  | REUSE    | `EndOfProjectArchivingPage.js`  |

### Strategy Legend

- **NEW**: Create new page component for medlab
- **EXTEND**: Modify existing notebook page to add medlab-specific features
- **EMBED**: Wrap existing component to display within notebook page
- **REUSE**: Use existing notebook page as-is

### Workflow Context

The notebook maintains patient context throughout all pages:

```javascript
workflowContext = {
  patientId: "12345",
  patientName: "John Doe",
  orderId: "ORD-2024-001",
  orderedTests: ["CBC", "LFT", "FBS"],
  samples: [...] // All samples linked to this patient/order
}
```
