# Feature Specification: Pathology Laboratory Workflow

**Feature Branch**: `052-pathology-lab-workflow` **Created**: 2025-12-14
**Status**: Draft **Input**: User description: Pathology Laboratory workflow
with sample reception, QC, processing, testing, storage, monitoring, disposal,
archiving, and reference module

## Executive Summary

Pathology laboratories process diverse specimen types (histopathology,
cytopathology, hematology, research) through a multi-step workflow from
reception to archiving. This feature extends the existing Notebook/Page
architecture to support the Pathology Laboratory workflow with comprehensive
sample tracking, quality control, and regulatory compliance.

**Key Differentiators from Immunology Workflow (OGC-51)**:

- **Free-text registry format** with structured metadata fields
- **Multi-category sample types**: Clinical diagnostic (FFPE, cytology, blood)
  and Research specimens
- **Post-embedding QC** for tissue blocks with Pass/Fail criteria
- **Grossing/Aliquoting** with parent-child relationships for sectioned slides
- **Diverse testing procedures**: H&E, special stains, IHC/ICC, ISH, research
  assays
- **Temperature-varied storage**: Room temp, 4°C, -80°C, LN₂
- **SOP Reference Module** with version control
- **Project-based access control** for research samples

**Target Users**: Data clerks, laboratory technicians, pathologists, PIs, lab
managers/supervisors

**Expected Impact**:

- Achieve 100% sample traceability from reception to archiving/disposal
- Reduce manual logbook entries through digital workflow tracking
- Enable real-time progress visibility for specimen batches
- Support both clinical diagnostic and research specimen workflows

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Sample Reception & Registration (Priority: P0)

Data clerks need to receive samples from Alert Hospital or affiliated research
projects, register them with appropriate metadata, and assign unique lab
accession numbers.

**Why this priority**: This is the foundation - no downstream workflow can occur
without proper sample registration. Must support both clinical and research
sample types.

**Independent Test**: Register 5 clinical samples with patient metadata and 5
research samples with study metadata, verify all receive unique accession
numbers and correct metadata categories.

**Acceptance Scenarios**:

1. **Given** a data clerk opens the Sample Reception page, **When** they select
   "Clinical Diagnostic Specimen", **Then** the form displays fields for:
   Patient ID, requesting clinician, collection date/time, specimen type/site,
   clinical details
2. **Given** a data clerk selects "Research Specimen", **When** viewing the
   form, **Then** the form displays fields for: Study ID, PI name,
   participant/animal ID, sample type, collection date/time, ethical approval
   reference
3. **Given** a sample is registered, **When** the registration is saved,
   **Then** a unique lab accession number is auto-generated and assigned
4. **Given** a sample arrives from Alert Hospital, **When** the clerk records
   the sample source, **Then** "Alert Hospital" is captured as the source with
   receiving date/time and receiving staff name
5. **Given** a research sample arrives affiliated with a project, **When** the
   clerk registers it, **Then** the sample is automatically linked to the
   project for access control purposes
6. **Given** multiple samples arrive in a batch, **When** registering them,
   **Then** the clerk can use "Copy from Previous" to replicate common fields
   (source, collection date, PI name)

---

### User Story 2 - Sample Quality Control (Initial Inspection) (Priority: P1)

Laboratory technicians need to perform initial QC inspection on received
samples, verifying specimen-requisition matching and sample-type-specific
criteria (fixation, container integrity, consent).

**Why this priority**: QC is the first processing step after reception. Samples
failing QC must be flagged before any analysis begins to prevent wasted
resources.

**Independent Test**: Process 10 samples through QC, mark 8 as Pass and 2 as
Fail with documented reasons, verify QC status persisted in Master Accession
Ledger.

**Acceptance Scenarios**:

1. **Given** a sample is on the QC page, **When** the technician opens the QC
   checklist, **Then** specimen-type-specific criteria are displayed:
   - Histology: Fixation check (10% NBF, 10:1 ratio), duration, tissue integrity
   - Cytology: Container integrity, preservative type, volume, clot presence
   - Peripheral Blood: Check for clots in EDTA tubes
   - Research: Consent verification, sample type, storage medium
2. **Given** a technician completes QC checks, **When** all criteria pass,
   **Then** they can mark the sample as "QC Pass" with their initials and
   timestamp
3. **Given** a sample fails any QC criterion, **When** the technician marks it
   as "QC Fail", **Then** they must enter a failure reason and the system
   prompts to notify the submitter (clinician/PI) and lab head
4. **Given** a QC failure is recorded, **When** selecting the action type,
   **Then** options include: "Recollection requested", "Processed with
   limitations", "Awaiting PI decision"
5. **Given** a research sample fails QC, **When** viewing the failure, **Then**
   a note indicates "PI decides usability for research samples"
6. **Given** QC is complete, **When** viewing the Master Accession Ledger,
   **Then** the QC status (Pass/Fail), remarks, staff initials, and date are
   recorded

---

### User Story 3 - Tissue Block QC (Post-Embedding) (Priority: P1)

Laboratory technicians need to perform quality control on embedded tissue
blocks, checking surface quality, depth, orientation, and paraffin integrity.

**Why this priority**: Block QC is essential for histopathology workflow -
poor-quality blocks cannot produce usable slides for diagnosis.

**Independent Test**: Evaluate 10 tissue blocks, mark 8 as Pass and 2 as Fail,
document remediation actions for failed blocks.

**Acceptance Scenarios**:

1. **Given** a tissue block is ready for QC, **When** the technician opens the
   Block QC form, **Then** criteria checklist shows: surface smoothness, correct
   depth, correct orientation, no paraffin overflow
2. **Given** a block passes all criteria, **When** marked as Pass, **Then** the
   block is cleared for microtomy and sectioning
3. **Given** a block has cracks, bubbles, shallow/deep tissue, or paraffin
   obscuring tissue, **When** marked as Fail, **Then** remediation options are
   presented: "Re-embed", "Melt and re-embed"
4. **Given** a block fails QC multiple times, **When** viewing history, **Then**
   all QC attempts are documented with actions taken
5. **Given** a block is deemed unusable after remediation attempts, **When**
   escalating, **Then** the pathologist is notified via system notification
6. **Given** any block QC action, **When** saving, **Then** the action is
   recorded in the logbook with date and staff initials

---

### User Story 4 - Sample Processing (Grossing & Aliquoting) (Priority: P1)

Laboratory technicians and pathologists need to process samples through grossing
(tissue) or aliquoting (fluids) and create child samples/slides while
maintaining parent-child traceability.

**Why this priority**: Processing transforms raw specimens into analyzable
samples. Parent-child tracking is critical for traceability and result
correlation.

**Independent Test**: Gross 5 tissue samples into blocks, aliquot 5 fluid
samples, verify parent-child relationships are correctly recorded.

**Acceptance Scenarios**:

1. **Given** a histopathology sample is ready for grossing, **When** the
   pathologist/assistant performs grossing, **Then** they can record: gross exam
   findings, description, number of sections/blocks
2. **Given** grossing is complete, **When** creating blocks, **Then** each block
   receives a unique ID linked to parent accession number (e.g., A24-001-A,
   A24-001-B)
3. **Given** tissue blocks are processed, **When** recording processing steps,
   **Then** the system captures: alcohols, xylene, paraffin embedding stages
   with timestamps
4. **Given** microtomy is performed, **When** creating slides, **Then** slide
   IDs are linked to parent block ID with section thickness (3-5 µm) recorded
5. **Given** a cytology fluid sample, **When** aliquoting, **Then** child
   samples can be created for: LBC (ThinPrep), cell block, molecular testing,
   biobanking
6. **Given** an FNA sample, **When** processing, **Then** direct smears and
   needle rinse cell blocks can be created with parent linkage
7. **Given** any child sample is created, **When** viewing the parent sample,
   **Then** all children are listed with their processing purpose
8. **Given** the processing logbook, **When** a step is completed, **Then**
   accession number, step name (e.g., "Grossing done"), date, and staff initials
   are recorded

---

### User Story 5 - Testing & Microscopy (Priority: P1)

Laboratory technicians and pathologists need to perform staining procedures and
record test results with appropriate controls and validation.

**Why this priority**: Testing produces the diagnostic/research results. Control
validation ensures result quality.

**Independent Test**: Perform H&E staining on 10 slides, IHC on 5 slides with
controls, verify control status and results recorded.

**Acceptance Scenarios**:

1. **Given** slides are ready for staining, **When** selecting stain type,
   **Then** options include:
   - Routine: H&E (histology), Papanicolaou, Romanowsky (cytology/blood)
   - Special: AFB, GMS, PAS, Gram stains
   - Advanced: IHC/ICC, ISH
   - Research: Laser microdissection, multiplex IHC, immunofluorescent, RNAscope
2. **Given** an IHC/ICC/ISH run is set up, **When** configuring the run,
   **Then** positive and negative controls must be specified
3. **Given** a staining run completes, **When** recording results, **Then** the
   system captures: accession number, block/slide ID, test name, control results
   (Pass/Fail), stain quality assessment
4. **Given** controls fail, **When** attempting to accept results, **Then** the
   system warns that assay results may be invalid
5. **Given** clinical results are ready, **When** a certified pathologist
   reviews them, **Then** they can sign off with their credentials and timestamp
6. **Given** results are recorded, **When** viewing the Test Requisition &
   Result Logbook, **Then** all entries show accession number, block/slide ID,
   test name, controls, results, and signatures

---

### User Story 6 - Storage & Inventory Management (Priority: P2)

Laboratory technicians need to assign samples to appropriate storage locations
based on sample type and record all storage movements with environmental
monitoring.

**Why this priority**: Proper storage preserves sample integrity. Location
tracking enables retrieval for future analysis.

**Independent Test**: Store 10 samples across different temperature conditions,
record retrieval of 2 samples, verify location logbook updated.

**Acceptance Scenarios**:

1. **Given** a sample type is determined, **When** assigning storage, **Then**
   appropriate conditions are suggested:
   - FFPE blocks & slides: Room temperature, labeled cabinets
   - Cell blocks: Room temperature
   - Residual cytology material: 4°C, 2-4 weeks retention
   - Frozen research samples: -80°C or LN₂ vapor
   - Blood smears: Room temperature, slide boxes
2. **Given** a sample is stored, **When** recording location, **Then** the
   system captures: Sample ID, exact location (e.g., "Freezer 3, Rack B, Box 4,
   Position 21"), date stored, staff signature
3. **Given** a sample is retrieved, **When** logging retrieval, **Then** the
   system records: retrieval date, recipient signature, purpose of retrieval
4. **Given** a sample is disposed, **When** marking disposal in storage logbook,
   **Then** the entry is marked as "Disposed [Date]" (crossed out in manual
   equivalent)
5. **Given** environmental monitoring is required, **When** viewing a storage
   unit, **Then** twice-daily temperature checks are recorded with date/time and
   staff initials
6. **Given** a temperature excursion occurs, **When** recording the check,
   **Then** the system flags the excursion and prompts documentation of affected
   samples

---

### User Story 7 - Tracking & Performance Monitoring (Priority: P2)

Laboratory supervisors need to generate routine reports on specimen volume,
turnaround time, rejection rates, and track key performance metrics.

**Why this priority**: Performance monitoring enables quality improvement and
identifies bottlenecks. Required for laboratory accreditation.

**Independent Test**: Generate monthly report for 100 samples, verify metrics
include volume, TAT, rejection rate, assay success rate.

**Acceptance Scenarios**:

1. **Given** a reporting period is selected, **When** generating the monthly
   report, **Then** metrics include: total specimen volume, average turnaround
   time, rejection rate, assay success rate, equipment downtime
2. **Given** samples have timestamps at each workflow step, **When** calculating
   TAT, **Then** the time from reception to result delivery is computed
3. **Given** QC failures exist in the period, **When** calculating rejection
   rate, **Then** (failed QC samples / total received samples) × 100 is computed
4. **Given** test results exist, **When** calculating assay success rate,
   **Then** (successful assays / total assays) × 100 is computed
5. **Given** equipment downtime is logged, **When** viewing the report, **Then**
   total downtime hours per equipment are displayed
6. **Given** the monthly QC meeting needs data, **When** exporting the report,
   **Then** logs, incidents, and performance trends are included

---

### User Story 8 - Disposal & Archiving (Priority: P3)

Laboratory supervisors need to manage sample disposal per retention policies and
archive physical/digital records.

**Why this priority**: Disposal ensures regulatory compliance. Archiving
preserves institutional records. Lower priority as it occurs after active
processing.

**Independent Test**: Mark 5 clinical samples for disposal following national
guidelines, archive associated logbooks, verify disposal logbook updated.

**Acceptance Scenarios**:

1. **Given** a sample exceeds retention period, **When** initiating disposal,
   **Then** the system checks retention policy:
   - Clinical samples: National guidelines (configurable)
   - Research samples: Protocol-approved retention period
2. **Given** disposal is approved, **When** recording disposal, **Then** the
   Disposal Logbook captures: date, sample IDs, disposal method
   (incineration/autoclaving), staff signature, unit head approval
3. **Given** samples are disposed, **When** updating the Storage Logbook,
   **Then** entries are marked "Disposed [Date]"
4. **Given** a project is completed, **When** archiving records, **Then**
   physical archives (logbooks, ledgers, reports) are stored in fire-proof
   cabinets with location recorded
5. **Given** digital data exists, **When** archiving, **Then** data is backed up
   to secure servers with backup verification recorded

---

### User Story 9 - Reference Module & SOP Management (Priority: P2)

All laboratory staff need access to reference materials and SOPs, while
authorized personnel (lab managers, supervisors) need to upload and maintain
protocol versions.

**Why this priority**: SOP access ensures consistent procedures. Version control
maintains audit trail for compliance.

**Independent Test**: Upload an SOP document, verify all users can view it,
upload a new version, verify previous version remains accessible.

**Acceptance Scenarios**:

1. **Given** a user accesses the Reference Module, **When** viewing available
   documents, **Then** all SOPs and reference materials are listed without
   restriction
2. **Given** a lab manager needs to upload a new SOP, **When** they have upload
   permission, **Then** they can upload the document with metadata (title,
   version, effective date)
3. **Given** an SOP needs updating, **When** uploading a new version, **Then**
   the previous version is retained and accessible with version history
4. **Given** version history exists, **When** viewing an SOP, **Then** all
   previous versions are listed with dates and change summaries
5. **Given** a user without upload permission, **When** attempting to upload,
   **Then** the action is blocked with appropriate message
6. **Given** protocols are updated, **When** a new version is uploaded, **Then**
   affected staff can be notified (optional notification system)

---

### User Story 10 - Project-Based Access Control (Priority: P2)

Project coordinators and pathologists need to restrict access to
project-associated samples to designated personnel only.

**Why this priority**: Research sample confidentiality and ethical compliance
require access restrictions. Prevents unauthorized data access.

**Independent Test**: Create a project with 10 samples, assign access to 3
users, verify other users cannot view the samples.

**Acceptance Scenarios**:

1. **Given** a sample is associated with a research project, **When** a user
   attempts to view it, **Then** access is granted only if they are: designated
   lab technician, project coordinator, or pathologist for that project
2. **Given** a project is created, **When** configuring access, **Then** the
   coordinator can add/remove designated personnel
3. **Given** a user without project access searches for samples, **When**
   results are returned, **Then** project-restricted samples are filtered out
4. **Given** a user with project access views a sample, **When** viewing audit
   trail, **Then** all access events are logged

---

### Edge Cases

- What happens when a sample fails QC but clinical urgency requires processing?
  **System MUST allow "Processed with limitations" option with documented
  limitations visible on all downstream results.**
- How does the system handle samples with multiple child samples across
  different processing paths? **System MUST display full sample hierarchy tree
  showing all parent-child relationships.**
- What happens if a tissue block fails QC after slides have already been cut?
  **System MUST allow marking existing slides as "From Failed Block" with
  warning displayed on results.**
- How should the system handle samples requiring multiple stain types? **System
  MUST support multiple test records per sample, each with its own controls and
  results.**
- What happens when storage location is at capacity? **System MUST warn but
  allow assignment per existing storage behavior (soft capacity limits).**
- How does the system handle concurrent users editing the same sample? **System
  MUST use optimistic locking. Conflicting updates show "Data modified by
  another user - please refresh".**
- What happens if ethical approval expires for a research sample? **System
  SHOULD flag samples with expired ethical approvals at the Reception page.**

## Requirements _(mandatory)_

### Functional Requirements

#### Sample Reception & Registration

- **FR-001**: System MUST support two sample categories: "Clinical Diagnostic
  Specimen" and "Research Specimen"
- **FR-002**: System MUST auto-generate unique lab accession numbers upon
  registration
- **FR-003**: System MUST capture clinical sample metadata: Patient ID,
  requesting clinician, collection date/time, specimen type/site, clinical
  details
- **FR-004**: System MUST capture research sample metadata: Study ID, PI name,
  participant/animal ID, sample type, collection date/time, ethical approval
  reference
- **FR-005**: System MUST capture common metadata for all samples: lab accession
  number, receiving date/time, receiving staff name, sample source
- **FR-006**: System MUST support "Alert Hospital" as a predefined sample source
- **FR-007**: System MUST allow free-text entry for clinical details and notes
  fields

#### Sample Types Support

- **FR-010**: System MUST support Histopathology sample types: FFPE tissue
  blocks, fresh biopsies, surgical resections
- **FR-011**: System MUST support Cytopathology sample types: FNA, body fluids
  (pleural, peritoneal, pericardial, CSF), urine, sputum, cervical smears
- **FR-012**: System MUST support Hematology sample types: EDTA tubes for
  peripheral blood smears
- **FR-013**: System MUST support Research specimen types: Human tissue biopsies
  (fresh, frozen, FFPE), animal model tissues, bacterial/cellular pellets,
  primary cell cultures

#### Quality Control

- **FR-020**: System MUST display specimen-type-specific QC criteria checklists
- **FR-021**: System MUST record QC status (Pass/Fail), remarks, staff initials,
  and date
- **FR-022**: System MUST require failure reason when marking QC Fail
- **FR-023**: System MUST support QC failure actions: "Recollection requested",
  "Processed with limitations", "Awaiting PI decision"
- **FR-024**: System MUST support tissue block QC criteria: surface smoothness,
  depth, orientation, paraffin integrity
- **FR-025**: System MUST support block QC remediation actions: "Re-embed",
  "Melt and re-embed"
- **FR-026**: System MUST allow escalation to pathologist for unusable blocks
- **FR-027**: System MUST notify submitter (clinician/PI) and lab head on QC
  failure

#### Sample Processing

- **FR-030**: System MUST support grossing with: gross exam findings,
  description, section count
- **FR-031**: System MUST create child sample IDs linked to parent accession
  number
- **FR-032**: System MUST record processing stages with timestamps
- **FR-033**: System MUST support aliquoting for fluids with purpose designation
  (LBC, cell block, molecular testing, biobanking)
- **FR-034**: System MUST maintain parent-child relationships for all derived
  samples
- **FR-035**: System MUST support Processing Logbook entries: accession number,
  step name, date, staff initials

#### Testing & Results

- **FR-040**: System MUST support routine stains: H&E, Papanicolaou, Romanowsky
- **FR-041**: System MUST support special stains: AFB, GMS, PAS, Gram
- **FR-042**: System MUST support advanced techniques: IHC/ICC, ISH
- **FR-043**: System MUST support research assays: laser microdissection,
  multiplex IHC, immunofluorescent, RNAscope
- **FR-044**: System MUST require positive and negative controls for IHC/ICC/ISH
  runs
- **FR-045**: System MUST warn when control results fail
- **FR-046**: System MUST support pathologist sign-off on clinical results
- **FR-047**: System MUST maintain Test Requisition & Result Logbook per assay

#### Storage & Inventory

- **FR-050**: System MUST suggest storage conditions based on sample type
- **FR-051**: System MUST record storage location with hierarchical path (e.g.,
  Freezer > Rack > Box > Position)
- **FR-052**: System MUST track storage movements (stored, retrieved, disposed)
- **FR-053**: System MUST record twice-daily temperature checks for storage
  units
- **FR-054**: System MUST flag temperature excursions
- **FR-055**: System MUST support multiple storage temperatures: Room temp, 4°C,
  -80°C, LN₂

#### Performance Monitoring

- **FR-060**: System MUST calculate and display: specimen volume, turnaround
  time, rejection rate, assay success rate, equipment downtime
- **FR-061**: System MUST support monthly report generation
- **FR-062**: System MUST support export of performance reports

#### Disposal & Archiving

- **FR-070**: System MUST enforce retention policies (clinical: national
  guidelines, research: protocol-specific)
- **FR-071**: System MUST maintain Disposal Logbook: date, sample IDs, method,
  staff signature, unit head approval
- **FR-072**: System MUST mark disposed samples in Storage Logbook
- **FR-073**: System MUST support physical archive location recording
- **FR-074**: System MUST support digital backup verification recording

#### Reference Module

- **FR-080**: System MUST allow all users to view SOPs and reference documents
- **FR-081**: System MUST restrict document upload/edit to authorized personnel
  (lab managers, supervisors)
- **FR-082**: System MUST maintain version history for all documents
- **FR-083**: System MUST retain previous versions when updating documents

#### Access Control

- **FR-090**: System MUST support project-based access restrictions for research
  samples
- **FR-091**: System MUST allow project coordinators to designate authorized
  personnel
- **FR-092**: System MUST filter project-restricted samples from unauthorized
  user searches
- **FR-093**: System MUST audit all sample access events

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
- **CR-005**: @Transactional annotations MUST only be used in service layer (NOT
  in controllers)
- **CR-006**: Services MUST compile all data within transaction boundaries to
  prevent LazyInitializationException
- **CR-007**: Security: RBAC for notebook operations, audit trail (sys_user_id +
  lastupdated), input validation
- **CR-008**: Tests MUST be included (unit + integration + E2E per feature, >70%
  coverage goal)
- **CR-009**: Individual E2E tests MUST be runnable independently during
  development (NOT full suite)

### Key Entities

#### Existing Entities (to be extended/reused)

- **NoteBook**: Template for pathology workflow with pages representing workflow
  steps
- **NoteBookPage**: Individual workflow step with page-type-specific UI
- **NoteBookEntry**: Active workflow instance created from NoteBook template
- **SampleItem**: Individual specimen with parent-child relationships
- **SampleStorageAssignment**: Storage location assignments
- **SampleStorageMovement**: Movement/retrieval audit records

#### New/Extended Entities

- **PathologySampleRegistration**: Extended metadata for pathology specimens.
  Key attributes: sample_item_id (FK), category (enum: CLINICAL, RESEARCH),
  patient_id, requesting_clinician, clinical_details, study_id, pi_name,
  participant_id, ethical_approval_reference, sample_source

- **QualityControlRecord**: QC inspection records. Key attributes: id,
  sample_item_id (FK), qc_type (enum: INITIAL_INSPECTION, BLOCK_QC), status
  (enum: PASS, FAIL), criteria_results (JSONB), failure_reason, action_taken,
  technician_id (FK), recorded_at, notes

- **ProcessingLogEntry**: Processing step tracking. Key attributes: id,
  sample_item_id (FK), step_name, step_details (JSONB), completed_by (FK),
  completed_at

- **TestResultRecord**: Test/assay results. Key attributes: id, sample_item_id
  (FK), block_slide_id, test_type, stain_name, positive_control_status,
  negative_control_status, result_data (JSONB), pathologist_signoff_id (FK),
  signoff_timestamp

- **StorageEnvironmentLog**: Temperature monitoring. Key attributes: id,
  storage_unit_id (FK), recorded_temperature, recorded_at, recorded_by (FK),
  is_excursion

- **ReferenceDocument**: SOP and protocol documents. Key attributes: id, title,
  document_type (enum: SOP, PROTOCOL, REFERENCE), file_path, version,
  effective_date, uploaded_by (FK), uploaded_at, replaced_by_id (FK)

- **ProjectAccess**: Project-based access control. Key attributes: id,
  project_id, user_id (FK), access_role (enum: TECHNICIAN, COORDINATOR,
  PATHOLOGIST), granted_by (FK), granted_at

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Data clerks can register a sample with all required metadata in
  under 3 minutes
- **SC-002**: Laboratory technicians can complete QC inspection for a sample in
  under 2 minutes
- **SC-003**: 100% of samples have complete traceability from reception to final
  disposition (archiving or disposal)
- **SC-004**: Parent-child sample relationships are accurately maintained with
  zero orphaned records
- **SC-005**: Monthly performance reports can be generated in under 5 minutes
  for up to 1000 samples
- **SC-006**: All temperature excursions are flagged within the same logging
  session
- **SC-007**: SOP version history maintains 100% of previous versions for audit
  purposes
- **SC-008**: Project-restricted samples are not visible to unauthorized users
  in 100% of cases
- **SC-009**: Turnaround time from reception to result delivery is trackable for
  all clinical samples

### Quality Gates

- **QG-001**: All sample registration and QC operations complete within 5
  seconds
- **QG-002**: Storage location searches return results in under 2 seconds for up
  to 10,000 stored samples
- **QG-003**: Concurrent users (up to 5) can work on the same notebook without
  data loss
- **QG-004**: All database operations use appropriate indexes (no full table
  scans)

## UI Wireframes _(mandatory)_

### Sample Reception Page

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PATHOLOGY LABORATORY - Sample Reception                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  SAMPLE CATEGORY                                                                │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐              │
│  │ (●) Clinical Diagnostic     │  │ ( ) Research Specimen       │              │
│  └─────────────────────────────┘  └─────────────────────────────┘              │
│                                                                                 │
│  SAMPLE SOURCE                              LAB ACCESSION #                     │
│  [Alert Hospital            ▼]              [ A24-0001 (auto) ]                │
│                                                                                 │
│  ══════════════════════════════════════════════════════════════════════════    │
│  CLINICAL DIAGNOSTIC SPECIMEN DETAILS                                           │
│  ──────────────────────────────────────────────────────────────────────────    │
│                                                                                 │
│  Patient ID:              [________________]   Collection Date: [2024-01-15 ▼] │
│  Requesting Clinician:    [________________]   Collection Time: [09:30     ▼]  │
│  Specimen Type:           [FFPE Tissue Block ▼]                                │
│  Specimen Site:           [________________]                                   │
│                                                                                 │
│  Clinical Details:                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Free text field for clinical notes, history, and additional details    │   │
│  │                                                                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  RECEIVING INFORMATION                                                          │
│  Receiving Date: [2024-01-15]   Time: [10:15]   Staff: [J. Smith ▼]           │
│                                                                                 │
│  [Copy from Previous]                    [Clear]  [Register Sample]             │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Quality Control Page

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PATHOLOGY LABORATORY - Quality Control                     Sample: A24-0001   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  SPECIMEN TYPE: FFPE Tissue Block                                              │
│  ══════════════════════════════════════════════════════════════════════════    │
│                                                                                 │
│  INITIAL INSPECTION CHECKLIST                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ [✓] Requisition matches labeling                                        │   │
│  │ [✓] Fixation verified (10% NBF)                                         │   │
│  │ [✓] Fixation ratio (10:1) adequate                                      │   │
│  │ [✓] Fixation duration documented                                        │   │
│  │ [✓] Tissue integrity acceptable                                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  QC RESULT                                                                      │
│  ┌──────────────────┐  ┌──────────────────┐                                   │
│  │ (●) Pass         │  │ ( ) Fail         │                                   │
│  └──────────────────┘  └──────────────────┘                                   │
│                                                                                 │
│  Remarks:                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Sample in good condition                                                │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  Technician: [J. Smith ▼]    Date: [2024-01-15]                               │
│                                                                                 │
│  [Previous Sample]                                        [Save & Next]         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Testing & Results Page

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PATHOLOGY LABORATORY - Testing & Results                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  SAMPLE: A24-0001-A (Slide)    Parent Block: A24-0001-BLK1                     │
│  ══════════════════════════════════════════════════════════════════════════    │
│                                                                                 │
│  TEST SETUP                                                                     │
│  Test Type:    [IHC                    ▼]                                      │
│  Target:       [CD20                   ▼]                                      │
│  Protocol:     [IHC-CD20-v2.1          ▼]                                      │
│                                                                                 │
│  CONTROLS                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Positive Control:  [Tonsil tissue        ]   Result: [● Pass] [○ Fail] │   │
│  │ Negative Control:  [Reagent blank        ]   Result: [● Pass] [○ Fail] │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  STAIN QUALITY: [● Acceptable] [○ Suboptimal] [○ Failed]                       │
│                                                                                 │
│  RESULTS                                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Staining pattern: Membranous                                            │   │
│  │ Intensity: 3+                                                           │   │
│  │ % Positive cells: 80%                                                   │   │
│  │ Interpretation: Positive for CD20                                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  PATHOLOGIST SIGN-OFF                                                           │
│  Reviewed by: [Dr. Anderson ▼]    Date: [2024-01-16]    [✓ Sign & Finalize]   │
│                                                                                 │
│  [Save Draft]                                             [Save & Complete]     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Reference Module Page

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PATHOLOGY LABORATORY - Reference Module                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  [+ Upload Document] (Lab Managers Only)        Search: [________________] 🔍  │
│                                                                                 │
│  DOCUMENT TYPE: [All Types ▼]                                                  │
│  ══════════════════════════════════════════════════════════════════════════    │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Title                    │ Type     │ Version │ Effective  │ Actions   │   │
│  │─────────────────────────│──────────│─────────│────────────│───────────│   │
│  │ IHC Protocol - CD20      │ PROTOCOL │ 2.1     │ 2024-01-01 │ [View][📋]│   │
│  │ H&E Staining SOP         │ SOP      │ 3.0     │ 2023-06-15 │ [View][📋]│   │
│  │ Grossing Guidelines      │ SOP      │ 1.5     │ 2023-09-01 │ [View][📋]│   │
│  │ Safety Procedures        │ REFERENCE│ 4.2     │ 2024-01-10 │ [View][📋]│   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  SELECTED: IHC Protocol - CD20 (v2.1)                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Current Version: 2.1 (2024-01-01)                                       │   │
│  │ Uploaded by: Lab Manager Smith                                          │   │
│  │                                                                         │   │
│  │ VERSION HISTORY                                                         │   │
│  │   v2.1 - 2024-01-01 - Updated incubation times                         │   │
│  │   v2.0 - 2023-07-15 - Added new antibody clone                         │   │
│  │   v1.0 - 2022-01-01 - Initial release                                  │   │
│  │                                                                         │   │
│  │ [Download Current] [View Previous Versions]                             │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Dependencies

### Existing (Reusable from OGC-51 Immunology Workflow)

- **NoteBook, NoteBookPage, NoteBookEntry**: Template and instance management
- **NotebookPageSample**: Per-sample-per-page tracking (if created in OGC-51)
- **SampleItem**: Parent-child sample relationships
- **SampleStorageAssignment, SampleStorageMovement**: Storage tracking
- **Bulk operation patterns**: Grid UI, Select All, Apply to Selected

### New for Pathology Workflow

- **PathologySampleRegistration**: Extended pathology-specific metadata
- **QualityControlRecord**: QC inspection tracking
- **ProcessingLogEntry**: Step-by-step processing records
- **TestResultRecord**: Test results with control validation
- **StorageEnvironmentLog**: Temperature monitoring
- **ReferenceDocument**: SOP management with versioning
- **ProjectAccess**: Research project access control

## Out of Scope

- Barcode/label printing integration
- Direct instrument/analyzer interfacing (manual data entry assumed)
- Billing/financial integration
- External lab result import (external lab routing is tracked, but results are
  entered manually)
- Mobile-optimized UI
- Real-time notifications/websockets
- Automated sample disposal scheduling (manual process)
- Image management for microscopy images (separate imaging system assumed)

## Assumptions

- Alert Hospital is a known, predefined sample source for this laboratory
- Retention policies for clinical samples follow national guidelines that will
  be configurable
- Research project ethical approval references are free-text (no external system
  integration)
- Temperature monitoring is manual (twice daily checks), not automated sensors
- Physical archiving location is tracked but not integrated with external
  archive management systems
- Pathologist sign-off uses existing OpenELIS user authentication (no separate
  credential system)
