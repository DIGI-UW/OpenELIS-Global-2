# Feature Specification: Medical Laboratory Workflow

**Feature Branch**: `001-medical-lab-workflow` **Created**: 2024-12-14
**Updated**: 2026-01-07 **Status**: Draft **SRS Version**: Medical Laboratory
Workflow Documentation v1.0 (January 2026)

**Input**: Medical Laboratory Workflow Documentation SRS covering order-driven
sample lifecycle management, quality control procedures, testing protocols, and
compliance standards for clinical laboratory operations and biorepository
functions.

## Executive Summary

Medical laboratories require comprehensive workflow management to handle samples
from collection through disposal. This feature introduces a complete Laboratory
Information Management System (LIMS) workflow module with an **order-driven
architecture** where lab orders drive all downstream sample collection,
processing, and testing activities.

**Core Architecture: Order-Driven Workflow**

The system follows an order-centric model where:

- **Orders drive sample collection** - Lab orders specify required container
  types, volumes, and handling requirements
- **Orders enable QC validation** - Samples without corresponding orders are
  rejected
- **Orders determine routing** - Sample-to-test allocation and department
  routing based on ordered tests
- **Orders generate worklists** - Electronic work lists for analyzers generated
  from orders

**10-Stage Workflow**:

1. **Patient Information & Lab Orders** - Establish patient identity, capture
   test requisitions
2. **Sample Collection** - Physically fulfill orders by obtaining specimens
3. **Sample Tracking & QC** - Assess integrity, verify order matching,
   accept/reject
4. **Storage** - Hierarchical storage with environmental monitoring
5. **Sample Processing** - Department-specific preparation (centrifugation,
   smears, staining)
6. **Testing Phase** - Analyzer worklists, instrument interfacing, QC monitoring
7. **Result Entry** - Machine-generated and manual result capture
8. **Validation & Reporting** - Technical/clinical validation, dashboards, KPIs
9. **System Accreditation** - ISO 15189, SLIPTA compliance support
10. **Disposal & Archiving** - Compliant disposal, biorepository transfers

**Target Users**: Laboratory technicians, technologists, sample collectors,
supervisors, quality managers, administrators, clinical management,
bioanalytical lab staff, biorepository staff

**Expected Impact**:

- Complete sample traceability from collection to disposal
- Regulatory compliance support (ISO 15189, SLIPTA, CAP, CLIA)
- Reduced manual transcription errors through instrument interfacing
- Real-time visibility into laboratory operations and performance metrics
- ALCOA+ compliant data handling for audit readiness

## System Overview

### Organizational Roles and Responsibilities

| Role                      | Responsibilities                                                                                                                                                   |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Patient/Participant**   | Provides demographic information, receives test results                                                                                                            |
| **Clinical Management**   | Patient registration, test ordering, clinical trial enrollment, scheduling sample collections, reviewing and releasing results, archiving data                     |
| **Laboratory Operations** | Sample collection, labeling, tracking, QC assessment, storage, processing, testing, result entry, report preparation, disposal management, biorepository transfers |
| **Quality & Compliance**  | Rejection investigation, corrective action initiation, QC metrics monitoring, deviation investigation, result approval, compliance reporting                       |
| **Bioanalytical Lab**     | Receives samples for specialized bioanalysis, returns analytical results                                                                                           |
| **Biorepository**         | Long-term sample storage, biobanking operations                                                                                                                    |

### Sample Categories

The system handles two main sample categories:

- **Patient Samples**: Specimens collected for routine clinical testing and
  diagnostics
- **Participant Samples**: Specimens collected from clinical trial participants;
  participants selected on screening continue as enrolled participants with
  scheduled collection events

### Workflow Decision Points

Based on the workflow stages, the following key decision points drive workflow
branching:

**1. QC Pass/Fail Gateway (After Stage 3: Sample Tracking & QC)**

- **Pass**: Sample proceeds to allocation, storage, processing, and testing
- **Fail**: Routes to Quality & Compliance for rejection investigation and
  corrective action initiation

**2. Test Deviation Gateway (After Stage 6: Testing Phase)**

- **No Deviation**: Proceeds to result entry and report preparation
- **Deviation Detected**: Routes to Quality & Compliance for deviation
  investigation before result approval

**3. Post-Testing Sample Disposition (After Stage 6)**

- **Store**: Sample retained per storage protocols
- **Dispose**: Routes to disposal workflow
- **Transfer to Biorepository**: Long-term storage for biobanking or
  bioequivalence studies

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Patient/Participant Registration and Lab Orders (Priority: P0)

Laboratory staff need to register patients or participants and create lab orders
before sample collection can occur. The system must support both clinical
patients and research participants with scheduled sample collections.

**Why this priority**: This is the entry point for all laboratory workflows.
Orders are the **driving force** for all downstream activities - without orders,
samples cannot be collected, validated, routed, or tested.

**Order Significance** (per SRS Section 3.1.2):

Orders created at this stage serve multiple critical functions:

- **Drive sample collection requirements** - Specify container type, volume, and
  handling instructions
- **Enable QC validation** - Samples without corresponding orders are rejected
- **Determine sample-to-test allocation** - Route samples to appropriate
  departments
- **Generate electronic work lists** - Create analyzer worklists for testing

**Independent Test**: Register a new patient with demographic data, create a lab
order for CBC and Chemistry panel, verify patient and order appear in the
system, verify order specifies required container types.

**Acceptance Scenarios**:

1. **Given** the patient registration page is open, **When** the user enters
   patient name, age, sex, and contact information, **Then** a new patient
   record is created with a unique patient ID
2. **Given** a patient record exists, **When** the user creates a lab order
   selecting tests (CBC, LFT, FBS), **Then** the order is created with pending
   status and linked to the patient
3. **Given** a lab order is created, **When** viewing the order details,
   **Then** the system displays required container types, volumes, and handling
   requirements for each ordered test
4. **Given** a participant is enrolled in a study, **When** the user sets up a
   collection schedule (e.g., Day 0, Day 7, Day 14), **Then** scheduled
   collection dates are created and visible on the participant dashboard
5. **Given** lab orders exist for a patient, **When** viewing the patient
   profile, **Then** all pending and completed orders are displayed with their
   status
6. **Given** a participant is selected for trial participation (screening),
   **When** screening eligibility is determined, **Then** the participant status
   is updated and scheduled collections are activated

---

### User Story 2 - Sample Collection and Labeling (Priority: P1)

Laboratory technicians, technologists, or sample collectors need to **physically
fulfill orders** by collecting samples from patients/participants according to
test requirements, labeling them appropriately, and recording collection
details.

**Why this priority**: Sample collection is the first physical step that
**fulfills the lab orders** created in Stage 1. Accurate collection and labeling
is critical for downstream processes.

**Performed by**: Lab technicians, technologists, sample collectors

**Sample Types by Department** (per SRS Section 3.2.1):

| Department         | Tests/Sample Types                                  |
| ------------------ | --------------------------------------------------- |
| Hematology         | CBC (Complete Blood Count), Differential Count      |
| Clinical Chemistry | LFT, KFT, Lipid Profile, FBS, Electrolytes, Enzymes |
| Urinalysis         | Dipstick, Microscopy                                |
| Stool              | Ova, Parasite, Occult Blood                         |
| Serology           | HIV, HBsAg, HCV                                     |
| Other              | ECG (if integrated), Pregnancy Test                 |

**Labeling Requirements** (per SRS Section 3.2.2):

- Free-text alphanumeric entries supported
- Labels can be generated pre-collection or at reception
- Barcode/QR code support for automated tracking
- Label contents: Unique Sample Identifier, date, technician initials, sample
  type

**Sample Collection Workflow (Two-Step Process)**:

1. **Step 1: Import Samples from Manifest** - Bulk import collected samples from
   CSV manifest file. At this point, samples exist in the system but are not yet
   linked to orders.
2. **Step 2: Link Samples to Orders/Tests** - Associate imported samples with
   existing lab orders and assign tests. One sample can have multiple tests
   (Analysis records). Anonymous samples (NULL patient) are supported.

**Manifest File Specification**:

The sample import manifest CSV MUST contain only these fields (per FR-010 to
FR-014):

| Field              | Required | Description                                                    |
| ------------------ | -------- | -------------------------------------------------------------- |
| `sampleId`         | Yes      | Unique sample identifier (FR-012)                              |
| `sampleTypeId`     | Yes      | Sample type: Blood, Urine, Stool, Serum, etc. (FR-010)         |
| `containerType`    | Yes      | Container: vacutainer, cryovial, urine_cup, stool_jar (FR-014) |
| `customLabel`      | No       | Free-text alphanumeric label (FR-011)                          |
| `quantity`         | Yes      | Collected volume/amount                                        |
| `unitOfMeasure`    | Yes      | Unit: ml, g, etc.                                              |
| `collectionSource` | Yes      | Collection location/site                                       |
| `collector`        | Yes      | Collector identity (FR-013)                                    |
| `collectionDate`   | Yes      | Collection date YYYY-MM-DD (FR-013)                            |
| `collectionTime`   | Yes      | Collection time HH:MM (FR-013)                                 |
| `orderId`          | No       | Lab order ID to link (optional for anonymous samples)          |
| `patientId`        | No       | Patient ID (NULL for anonymous participants)                   |
| `notes`            | No       | Observations or comments                                       |

**Sample-Test Relationship**:

- One SampleItem can have multiple Analysis records (tests assigned)
- Aliquoting is NOT automatic - it is a manual operation in Stage 6 (Sample
  Processing) via SampleManagementService
- When assigning multiple tests to a sample, decide upfront whether the sample
  volume supports all tests or if aliquoting is needed

**Independent Test**: Import 5 samples via manifest CSV, link 3 to existing
orders, verify 2 remain unlinked (anonymous), verify samples appear in tracking
queue with correct test assignments.

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
   appropriate container type and storage requirements based on the order
4. **Given** a participant has a scheduled collection, **When** the technician
   opens their profile, **Then** the scheduled collection is highlighted and can
   be started with pre-filled order information
5. **Given** sample collection is complete, **When** the user records collection
   time and collector ID, **Then** the collection metadata is saved for audit
   purposes
6. **Given** a lab order specifies required containers, **When** collecting
   samples, **Then** the system validates the selected container matches order
   requirements

---

### User Story 3 - Sample Reception and Quality Control (Priority: P1)

Laboratory staff need to receive samples into the tracking system, perform
quality checks including **order matching**, and accept or reject samples based
on defined criteria specific to each sample type.

**Why this priority**: Quality control at reception prevents downstream errors
and wasted resources on unsuitable samples. **Samples without corresponding
orders must be rejected.**

**Common Quality Assessment Criteria (Pass/Fail)** (per SRS Section 3.3.1):

- Mislabeling or unlabeled specimens
- Inappropriate container or test tube type
- **Without corresponding test request/order** ← Order validation is mandatory
- Storage temperature at collection: room temp, 2-8°C, frozen, N/A

**Sample-Specific Quality Requirements** (per SRS Section 3.3.3):

| Sample Type  | Quality Criteria (Rejection Reasons)                                                                                |
| ------------ | ------------------------------------------------------------------------------------------------------------------- |
| Chemistry    | Delayed >1 hour; Leaked; Volume <3 ml; Hemolysis in serum; Lipemic; Icteric                                         |
| Hematology   | Wrong anticoagulant (EDTA, citrate, heparin, fluoride); Delayed >4 hours; Leaked; Volume <2 ml; Clotting; Hemolysis |
| Stool        | Delayed >30 minutes; Volume less than pea grain; Leaked; Contaminated with urine; Improper container                |
| Urine        | Delayed >30 minutes; Volume <10 ml; Leaked container; Contaminated with stool                                       |
| Microbiology | Visible contamination; Sterility breach; Inappropriate transport medium                                             |

**Independent Test**: Receive 5 samples, mark 4 as accepted (quality checks
pass), reject 1 for hemolysis, verify rejection triggers corrective action
workflow. Also test rejection of sample without matching order.

**Acceptance Scenarios**:

1. **Given** samples arrive at the laboratory, **When** the technician scans the
   barcode, **Then** the sample details are displayed including patient info,
   tests requested, and collection time
2. **Given** a sample is scanned, **When** no corresponding lab order exists,
   **Then** the sample is flagged for rejection with reason "Without
   corresponding test request/order"
3. **Given** a Chemistry sample is being received, **When** the technician
   checks quality criteria, **Then** options include: volume (<3ml), hemolysis,
   lipemia, icterus, delay (>1 hour), leakage
4. **Given** a Hematology sample is being received, **When** checking quality,
   **Then** anticoagulant type (EDTA, citrate, heparin, fluoride), delay (>4
   hours), clotting, volume (<2ml) are validated
5. **Given** a Stool sample is received delayed (>30 minutes), **When** the
   technician marks quality check as failed, **Then** the sample status changes
   to "Rejected" with reason "Delayed >30 minutes"
6. **Given** a sample is rejected, **When** the rejection is confirmed, **Then**
   corrective action options are presented: recollection or return to submitter
7. **Given** all quality checks pass AND order matching verified, **When** the
   technician clicks "Accept", **Then** the sample moves to "Accepted" status
   and is ready for allocation
8. **Given** QC is complete, **When** verifying sample-test matching, **Then**
   lab orders appropriately match collected samples before department allocation

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

The system needs to support laboratory accreditation requirements through
comprehensive audit trails, document control, and compliance reporting.

**Supported Accreditation Standards** (per SRS Section 3.9):

- **ISO 15189** - Medical laboratories - Requirements for quality and competence
- **SLIPTA** - Stepwise Laboratory Quality Improvement Process Towards
  Accreditation
- **CAP** - College of American Pathologists
- **CLIA** - Clinical Laboratory Improvement Amendments

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
4. **Given** SLIPTA assessment is needed, **When** generating reports, **Then**
   system produces documentation supporting stepwise quality improvement

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

#### Lab Order Management (Order-Driven Architecture)

- **FR-006**: Lab orders MUST specify required container types, volumes, and
  handling requirements for each ordered test
- **FR-007**: Lab orders MUST drive sample collection requirements - collectors
  see order-specified container/volume needs
- **FR-008**: Lab orders MUST determine sample-to-test allocation and department
  routing
- **FR-009**: Lab orders MUST generate electronic work lists for analyzers

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
- **FR-021**: System MUST enforce common quality criteria for ALL samples:
  - Mislabeling or unlabeled specimens
  - Inappropriate container or test tube type
  - **Without corresponding test request/order** (samples without orders MUST be
    rejected)
  - Storage temperature at collection validation
- **FR-022**: System MUST enforce sample-type-specific quality criteria:
  - Chemistry: delay (>1 hour), leak, volume (<3ml), hemolysis, lipemia, icterus
  - Hematology: anticoagulant type (EDTA, citrate, heparin, fluoride), delay (>4
    hours), clotting, volume (<2ml), hemolysis
  - Stool: delay (>30 minutes), volume (<pea grain), leak, urine contamination,
    improper container
  - Urine: delay (>30 minutes), volume (<10ml), leak, stool contamination
  - Microbiology: visible contamination
- **FR-023**: System MUST record Accept (A) or Reject (R) status for each sample
- **FR-024**: System MUST capture corrective action for rejected samples:
  recollection or return to submitter
- **FR-025**: System MUST verify lab orders match collected samples before
  acceptance
- **FR-026**: System MUST verify sample-test matching to allocate samples to
  specific test departments

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
- **FR-131**: System MUST support accreditation standards: ISO 15189 (Medical
  laboratories - Requirements for quality and competence), SLIPTA (Stepwise
  Laboratory Quality Improvement Process Towards Accreditation), CAP, CLIA
- **FR-132**: System MUST generate compliance reports on demand

#### Data Handling (ALCOA+ Compliance)

All data MUST meet ALCOA+ requirements per SRS Section 5.3:

- **FR-140**: Data MUST be **Attributable** - User identification for all
  entries
- **FR-141**: Data MUST be **Legible** - Clear and readable
- **FR-142**: Data MUST be **Contemporaneous** - Real-time entry
- **FR-143**: Data MUST be **Original** - Original data or certified copy
- **FR-144**: Data MUST be **Accurate** - Verified data
- **FR-145**: Data MUST be **Complete** - All required data captured
- **FR-146**: Data MUST be **Consistent** - Uniform data practices
- **FR-147**: Data MUST be **Enduring** - Preserved for required retention
  period
- **FR-148**: Data MUST be **Available** - Accessible when needed
- **FR-149**: System MUST support automatic result upload from analyzers via
  instrument interfacing
- **FR-150**: System MUST support Delta checks - comparison with previous
  patient results for anomaly detection

#### Sample Retrieval and Distribution

- **FR-160**: System MUST support sample retrieval requests via LIMS with
  authorization by supervisor for external requests
- **FR-161**: System MUST document retrieval: date/time, personnel, purpose,
  sample condition, remaining volume/quantity
- **FR-162**: System MUST support inter-lab transfers with complete chain of
  custody
- **FR-163**: System MUST support temperature monitoring during transfer
- **FR-164**: System MUST support Material Transfer Agreements (MTAs) for
  external distribution
- **FR-165**: System MUST support proper packaging and shipping documentation

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
- **LabOrder/TestRequest**: Existing lab order system - **CRITICAL** for
  order-driven architecture. Orders specify container types, volumes, handling
  requirements, and drive all downstream activities
- **SampleItem**: Existing sample tracking (extend with medical lab attributes
  and order linkage)
- **StorageLocation**: Existing hierarchical storage (extend for cryo-box well
  mapping)
- **Analyzer**: Existing analyzer configuration for interfacing
- **SystemUser**: Existing user management for audit trails

#### New Entities for Order-Based Architecture

- **OrderSampleLink**: Links samples to their fulfilling orders, enabling QC
  validation that samples have corresponding orders
- **SampleRetrievalRequest**: Tracks sample retrieval requests with
  authorization, purpose, and chain of custody

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
