# Feature Specification: Tuberculosis Laboratory Workflow

**Feature Branch**: `053-tb-lab-workflow` **Created**: 2025-12-14 **Status**:
Draft **Input**: User description for TB laboratory multi-page workflow
including sample accession, QC, culture/DST testing, and result reporting

## Executive Summary

Tuberculosis (TB) laboratories process samples through a complex, multi-week
workflow from sample reception through culture, identification, drug
susceptibility testing (DST), and final result reporting. Currently, OpenELIS
Global has a Notebook system (extended in OGC-51 for Immunology) that can be
adapted for TB workflows, but TB laboratories have unique requirements:

- **Extended incubation tracking**: TB cultures require up to 8 weeks of weekly
  growth monitoring
- **Multiple testing methods**: Smear microscopy (ZN, Fluorescent), Culture (LJ,
  MGIT), GeneXpert, DST (1st/2nd line drugs)
- **TB-specific quality checks**: Triple packaging, temperature monitoring, leak
  detection
- **Sequential identification workflow**: Culture positive -> Smear -> Species
  identification -> DST
- **Unique sample ID format**: Sequential number per year (e.g., 345/25 for
  sample 345 of 2025)
- **Isolate storage tracking**: Room, fridge, compartment, rack, box location
  hierarchy

This feature extends the existing Notebook/Page architecture (from OGC-51) to
support the complete TB Laboratory workflow with TB-specific pages, result
formats, and weekly tracking capabilities.

**Target Users**: TB laboratory technicians, microbiologists, laboratory
supervisors, quality managers, data management team

**Expected Impact**:

- Standardize TB workflow across 8+ week culture incubation periods
- Track weekly culture readings with complete audit trail
- Support all TB testing methods (Smear, Culture, GeneXpert, DST)
- Enable isolate storage location tracking for biorepository compliance
- Integrate with REDCap for research data export

## Assumptions

Based on the requirements and TB laboratory domain knowledge:

1. **Sample Types**: Sputum is the primary specimen type, but system supports
   body fluids, swabs, tissue, and others
2. **User Authentication**: Users are already authenticated via existing
   OpenELIS authentication
3. **Role-Based Access**: Standard OpenELIS RBAC applies (Lab Tech, Supervisor,
   Quality Manager roles)
4. **Data Retention**: TB sample and result data retained per national TB
   program guidelines (typically 5+ years)
5. **Language**: UI supports existing OpenELIS internationalization (English primary)
6. **Existing Services**: Leverages existing Notebook, SampleStorage, and
   Patient services from OGC-51
7. **Analyzer Integration**: GeneXpert results may be imported; other results
   are manual entry
8. **Regulatory**: System tracks "Reported by" and "Reviewed by" for TB
   reporting requirements

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Sample Accession and Registration (Priority: P0)

Laboratory technicians need to register incoming TB samples with complete
metadata from the test request form, generating a unique sample ID in the TB
laboratory format (sequential/year).

**Why this priority**: This is the entry point - no workflow can proceed without
sample registration. The unique TB ID format (345/25) is mandatory for
laboratory operations.

**Independent Test**: Register a new TB sample with all required metadata,
verify the generated ID follows the format XXX/YY, confirm sample appears in the
reception queue.

**Acceptance Scenarios**:

1. **Given** a new sample arrives with test request form, **When** the
   technician opens "TB Sample Registration", **Then** a form displays all
   required metadata fields (patient info, specimen details, consent status)
2. **Given** the registration form is open, **When** the technician enters
   Document Number "TB-REQ-2025-001", **Then** the free text field accepts the
   value
3. **Given** the form is being completed, **When** the technician selects
   specimen type "Sputum" from dropdown, **Then** the options include: Sputum,
   Body Fluids, Swab, Tissue, Others
4. **Given** all required fields are completed, **When** the technician clicks
   "Register Sample", **Then** a unique Sample ID is generated in format NNN/YY
   (e.g., 001/25 for first sample of 2025)
5. **Given** it's the 345th sample registered in 2025, **When** registration
   completes, **Then** the generated ID is 345/25
6. **Given** patient metadata is entered, **When** saving, **Then** all fields
   are stored: patient name, age, sex, patient ID/code, study ID, referring
   facility, treatment history, specimen type, specimen quality, received
   site/date/time, patient address, physician phone, patient phone, consent
   status, test requested, method used
7. **Given** a sample is registered, **When** viewing sample details, **Then**
   the "Received Date/Time" shows when the sample was registered in the system

---

### User Story 2 - Quality Check (QC) - Raw Sample Integrity (Priority: P0)

Laboratory technicians must perform initial quality checks on registered samples
before processing to ensure samples meet TB laboratory standards.

**Why this priority**: QC is mandatory before any processing. Failed samples
must be documented with rejection reasons, and decisions about proceeding with
compromised samples require documentation.

**Independent Test**: Submit a sample through QC, mark as passed, verify it
moves to processing queue. Submit another sample, mark as failed with reason,
verify notification capability.

**Acceptance Scenarios**:

1. **Given** a sample is registered, **When** it appears in the QC queue,
   **Then** the QC checklist is displayed with TB-specific criteria: leak
   detection, transportation conditions (temperature, triple packaging),
   labeling completeness, request-sample matching, volume adequacy
2. **Given** the QC checklist is displayed, **When** the technician marks all
   items as "Pass", **Then** the sample status changes to "QC Passed" and
   proceeds to processing queue
3. **Given** a QC item fails (e.g., leak detected), **When** the technician
   marks "Fail", **Then** a mandatory rejection reason field appears
4. **Given** a sample fails QC, **When** rejection reason is entered
   (mislabeling, insufficient volume, contaminated, etc.), **Then** the sample
   is flagged and notification can be sent to physician/researcher
5. **Given** a failed sample, **When** the technician selects "Proceed with
   Remarks", **Then** the sample can continue to processing with the QC failure
   and remarks documented
6. **Given** a failed sample, **When** the technician selects "Discard",
   **Then** the sample is marked as discarded with reason and date recorded
7. **Given** a sample passes QC, **When** viewing sample status, **Then** the
   destination is shown: Processing, Temporary Storage, Shipment, or Long-term
   Storage

---

### User Story 3 - System Registration and Labeling (Priority: P1)

Once a sample passes QC and is registered in the system, a label is generated
for physical identification during collection and processing.

**Why this priority**: Labels are essential for physical sample tracking and
must be generated immediately after registration to prevent mix-ups.

**Independent Test**: Complete sample registration, generate label, verify label
contains barcode and sample ID in TB format.

**Acceptance Scenarios**:

1. **Given** a sample is registered, **When** the user clicks "Generate Label",
   **Then** a printable label is created with: barcode, sample ID (NNN/YY
   format), patient name (optional based on settings)
2. **Given** a label is generated, **When** printing, **Then** the barcode
   encodes the full sample identifier for scanner compatibility
3. **Given** multiple samples are registered from a batch, **When** the user
   selects "Print All Labels", **Then** labels for all selected samples are
   generated in a single print job

---

### User Story 4 - Initial Processing and Media Preparation (Priority: P1)

Laboratory technicians need to document the initial processing steps including
media preparation, reagent/instrument selection, and inoculation to culture
media (LJ/MGIT/Both).

**Why this priority**: Initial processing is the first laboratory step after QC.
Documentation of media type, reagents, and equipment is required for
traceability and quality assurance.

**Independent Test**: Record processing for 5 samples including processing date,
operator initials, media type, reagents used, verify all data saved.

**Acceptance Scenarios**:

1. **Given** Page 3 "Initial Processing" is open, **When** the technician
   selects samples for processing, **Then** a form displays fields for:
   processing date, operator initials, media type, reagents, instruments
2. **Given** a sample is being processed, **When** the technician selects Media
   Type, **Then** options include: LJ (Lowenstein-Jensen), MGIT (Mycobacteria
   Growth Indicator Tube), Both
3. **Given** reagents are being documented, **When** the technician clicks
   "Select Reagents", **Then** a list of available reagents is displayed for
   selection
4. **Given** instruments are being documented, **When** the technician clicks
   "Select Instruments", **Then** a list of available equipment is displayed for
   selection
5. **Given** processing is complete, **When** the technician clicks "Mark
   Processed", **Then** the sample moves to incubation tracking with processing
   details recorded
6. **Given** media is prepared and inoculated, **When** samples are placed in
   incubator, **Then** incubator storage location can be recorded for the sample

---

### User Story 5 - Culture Incubation and Weekly Growth Monitoring (Priority: P1)

Laboratory technicians must check inoculated cultures weekly for up to 8 weeks,
recording growth observations each week until either positive growth is detected
or 8 weeks pass (negative result).

**Why this priority**: Weekly monitoring over 8 weeks is the core of TB culture
testing. Missing a reading or not tracking this properly compromises the entire
workflow.

**Independent Test**: Create culture for a sample, record weekly readings for 3
weeks showing no growth, then week 4 showing growth, verify all readings stored
with dates and results.

**Acceptance Scenarios**:

1. **Given** a sample is inoculated to culture media, **When** viewing the
   Culture Tracking page, **Then** a grid displays Week 1 through Week 8 columns
   for recording growth observations
2. **Given** it's Week 1 after inoculation, **When** the technician checks the
   culture, **Then** they can record: reading date, growth observation (No
   Growth, Growth Detected, Contaminated), and initials
3. **Given** Week 3 reading shows no growth, **When** recorded, **Then** the
   system indicates the sample needs continued monitoring and Week 4 reading
   becomes due
4. **Given** Week 4 reading shows growth detected, **When** recorded, **Then**
   the sample status changes to "Culture Positive" and proceeds to
   identification workflow
5. **Given** all 8 weeks show no growth, **When** Week 8 is recorded as "No
   Growth", **Then** the sample result is marked as "Culture Negative" with
   final reading date
6. **Given** a culture shows contamination, **When** recorded, **Then** the
   sample is flagged as "Contaminated" with option to re-process or report as
   contaminated result
7. **Given** the culture method is MGIT, **When** the MGIT instrument flags
   positive, **Then** the technician can record the positive detection with
   instrument-provided date

---

### User Story 6 - Smear Microscopy and AFB Results (Priority: P1)

Laboratory technicians perform smear microscopy on culture-positive samples (or
direct specimens) using ZN or Fluorescent methods, recording AFB (Acid-Fast
Bacilli) quantification results.

**Why this priority**: Smear microscopy is a primary TB diagnostic method.
Results must follow standardized grading (Scanty, 1+, 2+, 3+, Negative).

**Independent Test**: Record smear result for a sample using ZN method with
result "2+", verify result stored with method, operator, reviewer, and date.

**Acceptance Scenarios**:

1. **Given** Page 5 "Smear Microscopy" is open, **When** selecting a sample for
   smear, **Then** a result entry form appears
2. **Given** the smear form is open, **When** selecting Method, **Then** options
   include: ZN (Ziehl-Neelsen), Concentrated, Fluorescent, Others
3. **Given** method is selected, **When** entering AFB Result, **Then** options
   are: Negative, Scanty, 1+, 2+, 3+
4. **Given** AFB result is entered, **When** submitting, **Then** the following
   fields are recorded: Method used, AFB result, Result reported date, "Test
   done by" name, "Reviewed by" name
5. **Given** a smear is completed, **When** viewing sample results, **Then** the
   smear section shows method, result, dates, and personnel

---

### User Story 7 - Species Identification (Priority: P1)

Laboratory technicians perform species identification on culture-positive
samples to differentiate MTB (Mycobacterium tuberculosis) from NTM
(Non-tuberculous Mycobacteria).

**Why this priority**: Species identification determines the clinical
significance and subsequent testing (DST is only relevant for MTB).

**Independent Test**: Record identification result as "MTB" using rapid test
kit, verify result stored with method and personnel.

**Acceptance Scenarios**:

1. **Given** Page 6 "Species Identification" is open, **When** selecting a
   culture-positive sample, **Then** identification options are displayed
2. **Given** identification form is open, **When** selecting Identification
   Result, **Then** options include: MTB (M. tuberculosis complex), NTM
   (Non-tuberculous Mycobacteria), Negative, Contaminated
3. **Given** identification method is needed, **When** selecting Method,
   **Then** options include: Smear morphology, BHI/BA (Brain Heart
   Infusion/Blood Agar), MTB Rapid Test Kit
4. **Given** identification is complete, **When** submitting, **Then** the
   following are recorded: Result, Method, Result reported date, "Test done by",
   "Reviewed by"
5. **Given** result is MTB, **When** identification is saved, **Then** the
   sample becomes eligible for Drug Susceptibility Testing (DST)
6. **Given** result is NTM, **When** identification is saved, **Then** the
   sample is flagged for NTM-specific reporting and may skip DST

---

### User Story 8 - GeneXpert Testing (Priority: P1)

Laboratory technicians record GeneXpert (rapid molecular) test results including
MTB detection and Rifampicin resistance status.

**Why this priority**: GeneXpert provides rapid TB diagnosis with rifampicin
resistance detection, often run in parallel with culture for faster clinical
decision-making.

**Independent Test**: Record GeneXpert result as "MTB Detected, Rif Resistant",
verify all fields including method stored correctly.

**Acceptance Scenarios**:

1. **Given** Page 7 "GeneXpert Testing" is open, **When** selecting a sample,
   **Then** the GeneXpert result entry form is displayed
2. **Given** GeneXpert form is open, **When** selecting Result, **Then** options
   include: MTB Not Detected, MTB Detected - Rif Sensitive, MTB Detected - Rif
   Resistant, MTB Detected - Rif Indeterminate
3. **Given** Method field is shown, **When** selecting, **Then** options
   include: GeneXpert MTB/RIF, Real-time PCR, Other molecular method
4. **Given** result is entered, **When** submitting, **Then** the following are
   recorded: GeneXpert result, Method used, Result reported date, "Test done
   by", "Reviewed by"
5. **Given** GeneXpert shows Rif Resistant, **When** viewing sample, **Then** a
   resistance alert is displayed for clinical attention

---

### User Story 9 - Drug Susceptibility Testing (DST) (Priority: P2)

Laboratory technicians perform DST on MTB-positive cultures to determine
susceptibility to first-line and second-line anti-TB drugs.

**Why this priority**: DST results guide TB treatment regimens. Complex drug
panels require structured data entry for accuracy.

**Independent Test**: Record DST results for all 5 first-line drugs with mixed
S/R results, verify each drug result stored individually.

**Acceptance Scenarios**:

1. **Given** Page 8 "Drug Susceptibility Testing" is open, **When** selecting an
   MTB-positive sample, **Then** the DST entry form displays drug panels
2. **Given** DST Method selection is shown, **When** selecting, **Then** options
   include: Phenotypic DST - 1st Line, Phenotypic DST - 2nd Line, Molecular
   DST - 1st Line
3. **Given** 1st Line Drugs panel is displayed, **When** entering results,
   **Then** each drug shows separately: INH (Isoniazid), RMP (Rifampicin), STM
   (Streptomycin), EMB (Ethambutol), PZA (Pyrazinamide)
4. **Given** 2nd Line Drugs panel is displayed, **When** entering results,
   **Then** drugs include: FLQ (Fluoroquinolones), KAN/AMK/CAP
   (Kanamycin/Amikacin/Capreomycin), KAN/CAP/VIO, KAN/AMK/CAP/VIO, Low level KAN
5. **Given** a drug is being tested, **When** entering result, **Then** options
   for each drug are: Sensitive (S), Resistant (R), Invalid
6. **Given** all drug results are entered, **When** submitting, **Then** the
   following are recorded: Method used, individual drug results, Result reported
   date, "Test done by", "Reviewed by"
7. **Given** MDR pattern detected (INH-R and RMP-R), **When** DST is saved,
   **Then** sample is flagged as "MDR-TB Suspected" for supervisor review

---

### User Story 10 - Isolate Storage (Priority: P2)

Laboratory technicians must record storage locations for TB isolates using a
hierarchical location system (Room/Fridge ID, Compartment, Rack, Box).

**Why this priority**: Proper isolate storage is required for biorepository
compliance and future retrieval for additional testing or quality assurance.

**Independent Test**: Assign an isolate to storage location "Room A, Fridge 2,
Compartment 1, Rack 3, Box 5", verify full hierarchy stored.

**Acceptance Scenarios**:

1. **Given** Page 9 "Isolate Storage" is open, **When** selecting a
   culture-positive sample, **Then** the storage assignment form is displayed
2. **Given** storage form is open, **When** selecting location, **Then**
   hierarchical dropdowns appear: Room -> Fridge ID -> Compartment -> Rack ->
   Box
3. **Given** a location is selected, **When** clicking "Assign Storage",
   **Then** the isolate is linked to the storage location with assignment date
   and operator
4. **Given** an isolate is stored, **When** viewing sample details, **Then** the
   full storage path is displayed (e.g., "Room A > Fridge-2 > Compartment 1 >
   Rack 3 > Box 5")
5. **Given** an isolate needs retrieval, **When** searching by sample ID,
   **Then** the storage location is immediately visible for retrieval

---

### User Story 11 - Result Compilation and Reporting (Priority: P2)

Laboratory staff need to compile all test results for a sample and prepare the
final report with required reviewer signatures.

**Why this priority**: Final reports must be accurate, complete, and properly
reviewed before release to clinicians.

**Independent Test**: Generate final report for a sample with Culture Positive,
AFB 2+, MTB identified, GeneXpert Rif-R, DST results, verify all sections
populated with reviewer fields.

**Acceptance Scenarios**:

1. **Given** Page 10 "Result Compilation" is open, **When** selecting a sample
   with complete testing, **Then** a summary displays all results: Culture,
   Smear/AFB, Identification, GeneXpert, DST
2. **Given** results are compiled, **When** the technician enters "Reported by"
   name, **Then** the field accepts free text for the reporting person's name
3. **Given** report is prepared, **When** entering "Reviewed by" name, **Then**
   the field accepts free text for the reviewer's name
4. **Given** a Comment section is available, **When** entering clinical
   comments, **Then** free text comments are stored with the report
5. **Given** all required fields are complete, **When** clicking "Generate
   Report", **Then** a printable/exportable report is created following TB
   Result format
6. **Given** report is generated, **When** clicking "Export", **Then** the
   report can be exported to LMS (Laboratory Management System) if supported

---

### User Story 12 - Data Export and REDCap Integration (Priority: P3)

Data managers need to export validated results to REDCap database for research
purposes.

**Why this priority**: Research integration is important but lower priority than
core laboratory workflow operations.

**Independent Test**: Export 10 sample results to CSV format compatible with
REDCap, verify all data fields present.

**Acceptance Scenarios**:

1. **Given** samples have completed testing, **When** the data manager opens
   "Data Export", **Then** export options are displayed
2. **Given** export is initiated, **When** selecting samples, **Then** raw data
   can be exported in CSV or Excel format
3. **Given** data is exported, **When** validation is complete, **Then** the
   data manager can mark samples as "Exported to REDCap" with timestamp
4. **Given** export tracking is needed, **When** viewing sample history,
   **Then** REDCap export status and date are visible

---

### Edge Cases

- What happens when a sample fails QC but testing proceeds with remarks?
  **System MUST track the QC failure flag throughout the workflow and display a
  warning on all result screens.**
- How does the system handle samples where culture is contaminated but needs
  re-processing? **System MUST allow creating a "re-process" record linked to
  the original sample, maintaining full traceability.**
- What happens if GeneXpert and culture/DST results conflict? **System MUST
  allow both results to coexist, flagging for supervisor review with comment
  field for resolution.**
- How should the system handle samples where Week 8 reading is missed? **System
  MUST allow late entry with actual reading date, flagging as "delayed reading"
  in audit trail.**
- What happens when isolate storage location is full? **System MUST warn but
  allow assignment, using soft capacity limits consistent with existing storage
  behavior.**
- How does the system handle samples with only partial testing (e.g., smear
  only, no culture)? **System MUST support partial workflows where not all pages
  are required for every sample based on test requested.**

## Requirements _(mandatory)_

### Functional Requirements

#### Notebook & TB Workflow Configuration

- **FR-001**: System MUST support creating TB Laboratory notebook instances from
  a TB workflow template
- **FR-002**: System MUST support a minimum of 10 configurable pages for the TB
  workflow (Registration, QC, Labeling, Processing, Culture Tracking, Smear,
  Identification, GeneXpert, DST, Isolate Storage, Result Compilation)
- **FR-003**: System MUST allow pages to be optional based on test requested
  (not all samples need all tests)
- **FR-004**: System MUST reuse existing NotebookPageSample tracking from OGC-51

#### Sample Registration (TB-Specific)

- **FR-010**: System MUST generate unique TB sample IDs in format NNN/YY
  (sequential per year)
- **FR-011**: System MUST reset the sequential counter at the start of each
  calendar year
- **FR-012**: System MUST store Document Number as free text field
- **FR-013**: System MUST support specimen types: Sputum, Body Fluids, Swab,
  Tissue, Others
- **FR-014**: System MUST capture all required metadata: patient name, age, sex,
  patient ID/code, study ID, sample ID, referring facility, treatment history,
  specimen type, specimen quality, received site/date/time, patient address,
  physician phone, patient phone, consent status, test requested, method used
- **FR-015**: System MUST record received date and time for each sample

#### Quality Check (TB-Specific)

- **FR-020**: System MUST display TB-specific QC checklist: leak detection,
  transportation conditions (temperature, triple packaging), labeling
  completeness, request-sample matching, volume adequacy
- **FR-021**: System MUST require rejection reason when any QC item fails
- **FR-022**: System MUST support QC outcomes: Pass (proceed to processing),
  Pass to Storage (temporary/shipment/long-term), Fail - Discard, Fail - Proceed
  with Remarks
- **FR-023**: System MUST store rejection reasons from predefined list:
  mislabeling, insufficient sample, contaminated sample, temperature deviation,
  packaging issue, request mismatch, other
- **FR-024**: System MUST flag samples that proceeded with QC failures
  throughout the workflow

#### Label Generation

- **FR-030**: System MUST generate printable labels with barcode encoding sample
  ID
- **FR-031**: System MUST include sample ID in TB format (NNN/YY) on label
- **FR-032**: System MUST optionally include patient name on label based on
  laboratory settings

#### Culture Tracking (TB-Specific)

- **FR-040**: System MUST track weekly culture readings for up to 8 weeks per
  sample
- **FR-041**: System MUST record for each weekly reading: reading date, week
  number (1-8), growth observation, operator initials
- **FR-042**: System MUST support growth observations: No Growth, Growth
  Detected, Contaminated
- **FR-043**: System MUST support culture methods: LJ (Lowenstein-Jensen), MGIT,
  Both
- **FR-044**: System MUST mark sample as "Culture Positive" when growth is
  detected
- **FR-045**: System MUST mark sample as "Culture Negative" after 8 weeks of no
  growth
- **FR-046**: System MUST track which media (LJ/MGIT) showed growth when "Both"
  is used

#### Smear Microscopy Results

- **FR-050**: System MUST support smear methods: ZN (Ziehl-Neelsen),
  Concentrated, Fluorescent, Others
- **FR-051**: System MUST support AFB results: Negative, Scanty, 1+, 2+, 3+
- **FR-052**: System MUST record: Method, AFB result, Result reported date, Test
  done by, Reviewed by

#### Species Identification

- **FR-060**: System MUST support identification results: MTB, NTM, Negative,
  Contaminated
- **FR-061**: System MUST support identification methods: Smear morphology,
  BHI/BA, MTB Rapid Test Kit
- **FR-062**: System MUST record: Result, Method, Result reported date, Test
  done by, Reviewed by

#### GeneXpert Results

- **FR-070**: System MUST support GeneXpert results: MTB Not Detected, MTB
  Detected - Rif Sensitive, MTB Detected - Rif Resistant, MTB Detected - Rif
  Indeterminate
- **FR-071**: System MUST support methods: GeneXpert MTB/RIF, Real-time PCR,
  Other molecular
- **FR-072**: System MUST record: Result, Method, Result reported date, Test
  done by, Reviewed by
- **FR-073**: System MUST flag rifampicin resistance for clinical alert

#### Drug Susceptibility Testing (DST)

- **FR-080**: System MUST support DST methods: Phenotypic DST (1st line),
  Phenotypic DST (2nd line), Molecular DST (1st line)
- **FR-081**: System MUST support 1st line drugs: INH, RMP, STM, EMB, PZA with
  individual S/R/Invalid results
- **FR-082**: System MUST support 2nd line drug combinations: FLQ, KAN/AMK/CAP,
  KAN/CAP/VIO, KAN/AMK/CAP/VIO, Low level KAN
- **FR-083**: System MUST record individual drug results with overall method,
  dates, and personnel
- **FR-084**: System MUST flag MDR-TB pattern (INH-R + RMP-R) automatically

#### Isolate Storage

- **FR-090**: System MUST support hierarchical storage locations: Room -> Fridge
  ID -> Compartment -> Rack -> Box
- **FR-091**: System MUST integrate with existing SampleStorageService for
  location management
- **FR-092**: System MUST record storage assignment date and operator

#### Result Reporting

- **FR-100**: System MUST compile all test results into unified sample report
- **FR-101**: System MUST support "Reported by" and "Reviewed by" name fields
  (free text)
- **FR-102**: System MUST support comment/notes section for clinical remarks
- **FR-103**: System MUST generate printable/exportable TB result report
- **FR-104**: System MUST support data export to CSV/Excel for REDCap
  integration

#### Reuse from OGC-51

- **FR-110**: System MUST reuse NotebookPageSample entity for
  per-sample-per-page tracking
- **FR-111**: System MUST reuse bulk operations patterns (select all, apply to
  selected)
- **FR-112**: System MUST reuse sample grid UI with filtering and pagination
- **FR-113**: System MUST reuse page navigation and progress indicators

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder->DAO->Service->Controller->Form)
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

### Key Entities _(mandatory)_

#### New Entities

- **TbSampleRegistration**: TB-specific sample registration data extending
  SampleItem. Key attributes: document_number (VARCHAR free text), specimen_type
  (enum: SPUTUM, BODY_FLUID, SWAB, TISSUE, OTHER), specimen_quality (text),
  referring_facility, treatment_history, physician_phone, patient_phone,
  consent_status (boolean), test_requested (JSONB array), received_site,
  received_datetime. Links to sample_item via FK.

- **TbQualityCheck**: QC assessment for TB samples. Key attributes:
  sample_item_id (FK), qc_date, leak_check (PASS/FAIL), temperature_check
  (PASS/FAIL), packaging_check (PASS/FAIL), labeling_check (PASS/FAIL),
  volume_check (PASS/FAIL), request_match_check (PASS/FAIL), overall_result
  (PASS/FAIL_DISCARD/FAIL_PROCEED), rejection_reason (enum), rejection_remarks
  (text), checked_by (FK to system_user).

- **TbCultureReading**: Weekly culture reading records. Key attributes:
  sample_item_id (FK), week_number (1-8), reading_date, culture_method
  (LJ/MGIT/BOTH), growth_observation (NO_GROWTH/GROWTH_DETECTED/CONTAMINATED),
  lj_observation (if using LJ), mgit_observation (if using MGIT), read_by (FK),
  notes.

- **TbSmearResult**: Smear microscopy results. Key attributes: sample_item_id
  (FK), method (ZN/CONCENTRATED/FLUORESCENT/OTHER), afb_result
  (NEGATIVE/SCANTY/PLUS1/PLUS2/PLUS3), result_date, tested_by (FK), reviewed_by
  (FK).

- **TbIdentificationResult**: Species identification results. Key attributes:
  sample_item_id (FK), result (MTB/NTM/NEGATIVE/CONTAMINATED), method
  (SMEAR_MORPHOLOGY/BHI_BA/RAPID_TEST_KIT), result_date, tested_by (FK),
  reviewed_by (FK).

- **TbGeneXpertResult**: GeneXpert/molecular results. Key attributes:
  sample_item_id (FK), result
  (MTB_NOT_DETECTED/MTB_RIF_SENSITIVE/MTB_RIF_RESISTANT/MTB_RIF_INDETERMINATE),
  method (GENEXPERT/REALTIME_PCR/OTHER), result_date, tested_by (FK),
  reviewed_by (FK).

- **TbDstResult**: Drug susceptibility testing results. Key attributes:
  sample_item_id (FK), method (PHENOTYPIC_1ST/PHENOTYPIC_2ND/MOLECULAR_1ST),
  inh_result (S/R/INVALID), rmp_result, stm_result, emb_result, pza_result,
  second_line_results (JSONB for 2nd line drugs), mdr_flag (boolean computed),
  result_date, tested_by (FK), reviewed_by (FK).

- **TbIsolateStorage**: Isolate storage assignments using hierarchical location.
  Key attributes: sample_item_id (FK), room, fridge_id, compartment, rack, box,
  storage_date, stored_by (FK), retrieval_date (nullable), retrieved_by (FK
  nullable).

#### Extended Entities

- **SampleItem**: Add tb_sample_id field for TB format ID (NNN/YY), add
  external_id if not present from OGC-51.

- **NoteBookPage**: Reuse page_type enum from OGC-51, add TB-specific page types
  if needed (CULTURE_TRACKING, DST_ENTRY).

#### Reused Entities (from OGC-51)

- **NotebookPageSample**: Junction entity for per-sample-per-page tracking (no
  changes needed)
- **NotebookEntry**: Workflow instance (no changes needed)
- **SampleStorageAssignment**: For isolate storage integration (no changes
  needed)

### Database Schema Changes

```sql
-- New table: tb_sample_registration
CREATE TABLE tb_sample_registration (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id) ON DELETE CASCADE,
    document_number VARCHAR(100),
    specimen_type VARCHAR(20) NOT NULL,
    specimen_quality VARCHAR(255),
    referring_facility VARCHAR(255),
    treatment_history TEXT,
    physician_phone VARCHAR(50),
    patient_phone VARCHAR(50),
    consent_status BOOLEAN DEFAULT false,
    test_requested JSONB,
    received_site VARCHAR(255),
    received_datetime TIMESTAMP,
    created_by INTEGER REFERENCES system_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_specimen_type CHECK (specimen_type IN ('SPUTUM', 'BODY_FLUID', 'SWAB', 'TISSUE', 'OTHER'))
);

CREATE INDEX idx_tb_reg_sample ON tb_sample_registration(sample_item_id);

-- New table: tb_quality_check
CREATE TABLE tb_quality_check (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    qc_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leak_check VARCHAR(10),
    temperature_check VARCHAR(10),
    packaging_check VARCHAR(10),
    labeling_check VARCHAR(10),
    volume_check VARCHAR(10),
    request_match_check VARCHAR(10),
    overall_result VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(50),
    rejection_remarks TEXT,
    checked_by INTEGER REFERENCES system_user(id),
    CONSTRAINT chk_overall CHECK (overall_result IN ('PASS', 'FAIL_DISCARD', 'FAIL_PROCEED'))
);

CREATE INDEX idx_tb_qc_sample ON tb_quality_check(sample_item_id);

-- New table: tb_culture_reading
CREATE TABLE tb_culture_reading (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    week_number INTEGER NOT NULL CHECK (week_number >= 1 AND week_number <= 8),
    reading_date DATE NOT NULL,
    culture_method VARCHAR(10) NOT NULL,
    growth_observation VARCHAR(20) NOT NULL,
    lj_observation VARCHAR(20),
    mgit_observation VARCHAR(20),
    read_by INTEGER REFERENCES system_user(id),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_culture_method CHECK (culture_method IN ('LJ', 'MGIT', 'BOTH')),
    CONSTRAINT chk_growth CHECK (growth_observation IN ('NO_GROWTH', 'GROWTH_DETECTED', 'CONTAMINATED')),
    CONSTRAINT uq_sample_week UNIQUE (sample_item_id, week_number)
);

CREATE INDEX idx_tb_culture_sample ON tb_culture_reading(sample_item_id);

-- New table: tb_smear_result
CREATE TABLE tb_smear_result (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    method VARCHAR(20) NOT NULL,
    afb_result VARCHAR(10) NOT NULL,
    result_date DATE NOT NULL,
    tested_by INTEGER REFERENCES system_user(id),
    reviewed_by INTEGER REFERENCES system_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_smear_method CHECK (method IN ('ZN', 'CONCENTRATED', 'FLUORESCENT', 'OTHER')),
    CONSTRAINT chk_afb CHECK (afb_result IN ('NEGATIVE', 'SCANTY', 'PLUS1', 'PLUS2', 'PLUS3'))
);

-- New table: tb_identification_result
CREATE TABLE tb_identification_result (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    result VARCHAR(20) NOT NULL,
    method VARCHAR(30) NOT NULL,
    result_date DATE NOT NULL,
    tested_by INTEGER REFERENCES system_user(id),
    reviewed_by INTEGER REFERENCES system_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_id_result CHECK (result IN ('MTB', 'NTM', 'NEGATIVE', 'CONTAMINATED')),
    CONSTRAINT chk_id_method CHECK (method IN ('SMEAR_MORPHOLOGY', 'BHI_BA', 'RAPID_TEST_KIT'))
);

-- New table: tb_genexpert_result
CREATE TABLE tb_genexpert_result (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    result VARCHAR(30) NOT NULL,
    method VARCHAR(20) NOT NULL,
    result_date DATE NOT NULL,
    tested_by INTEGER REFERENCES system_user(id),
    reviewed_by INTEGER REFERENCES system_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_gx_result CHECK (result IN ('MTB_NOT_DETECTED', 'MTB_RIF_SENSITIVE', 'MTB_RIF_RESISTANT', 'MTB_RIF_INDETERMINATE')),
    CONSTRAINT chk_gx_method CHECK (method IN ('GENEXPERT', 'REALTIME_PCR', 'OTHER'))
);

-- New table: tb_dst_result
CREATE TABLE tb_dst_result (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    method VARCHAR(20) NOT NULL,
    inh_result VARCHAR(10),
    rmp_result VARCHAR(10),
    stm_result VARCHAR(10),
    emb_result VARCHAR(10),
    pza_result VARCHAR(10),
    second_line_results JSONB,
    mdr_flag BOOLEAN DEFAULT false,
    result_date DATE NOT NULL,
    tested_by INTEGER REFERENCES system_user(id),
    reviewed_by INTEGER REFERENCES system_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dst_method CHECK (method IN ('PHENOTYPIC_1ST', 'PHENOTYPIC_2ND', 'MOLECULAR_1ST'))
);

-- New table: tb_isolate_storage
CREATE TABLE tb_isolate_storage (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    room VARCHAR(50) NOT NULL,
    fridge_id VARCHAR(50) NOT NULL,
    compartment VARCHAR(50),
    rack VARCHAR(50),
    box VARCHAR(50),
    storage_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stored_by INTEGER REFERENCES system_user(id),
    retrieval_date TIMESTAMP,
    retrieved_by INTEGER REFERENCES system_user(id)
);

CREATE INDEX idx_tb_storage_sample ON tb_isolate_storage(sample_item_id);
CREATE INDEX idx_tb_storage_location ON tb_isolate_storage(room, fridge_id);

-- Add tb_sample_id to sample_item for TB format ID
ALTER TABLE sample_item ADD COLUMN IF NOT EXISTS tb_sample_id VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_sample_tb_id ON sample_item(tb_sample_id);

-- Sequence for TB sample ID generation (resets yearly)
CREATE SEQUENCE IF NOT EXISTS tb_sample_id_seq_2025 START WITH 1;
```

## API Endpoints _(mandatory)_

### Sample Registration

```
POST /rest/tb/samples/register
  - Register new TB sample with metadata
  - Request: {
      documentNumber: "TB-REQ-2025-001",
      specimenType: "SPUTUM",
      patientName: "...",
      age: 45,
      sex: "M",
      patientId: "P-12345",
      studyId: "STUDY-001",
      referringFacility: "Hospital A",
      treatmentHistory: "...",
      specimenQuality: "Good",
      receivedDatetime: "2025-01-15T09:30:00",
      physicianPhone: "+1234567890",
      patientPhone: "+0987654321",
      consentStatus: true,
      testRequested: ["CULTURE", "SMEAR", "GENEXPERT"]
    }
  - Response: {
      sampleId: 42,
      tbSampleId: "015/25",
      status: "REGISTERED"
    }

GET /rest/tb/samples/{sampleId}
  - Get TB sample details with all results
  - Response: { registration: {...}, qc: {...}, culture: [...], smear: {...}, ... }
```

### Quality Check

```
POST /rest/tb/samples/{sampleId}/qc
  - Submit QC assessment
  - Request: {
      leakCheck: "PASS",
      temperatureCheck: "PASS",
      packagingCheck: "PASS",
      labelingCheck: "FAIL",
      volumeCheck: "PASS",
      requestMatchCheck: "PASS",
      overallResult: "FAIL_PROCEED",
      rejectionReason: "MISLABELING",
      rejectionRemarks: "Label partially damaged but readable"
    }
  - Response: { qcId: 1, overallResult: "FAIL_PROCEED" }

GET /rest/tb/samples/{sampleId}/qc
  - Get QC status for sample
  - Response: { ...qcData, qcPassed: false, proceedWithRemarks: true }
```

### Culture Tracking

```
POST /rest/tb/samples/{sampleId}/culture/reading
  - Record weekly culture reading
  - Request: {
      weekNumber: 3,
      readingDate: "2025-02-05",
      cultureMethod: "MGIT",
      growthObservation: "NO_GROWTH",
      notes: "Continue monitoring"
    }
  - Response: { readingId: 1, weekNumber: 3, nextReadingDue: "2025-02-12" }

GET /rest/tb/samples/{sampleId}/culture/readings
  - Get all culture readings for sample
  - Response: [{ weekNumber: 1, ... }, { weekNumber: 2, ... }, ...]

GET /rest/tb/samples/{sampleId}/culture/status
  - Get culture status summary
  - Response: {
      method: "MGIT",
      currentWeek: 3,
      status: "IN_PROGRESS",
      positiveWeek: null,
      readings: [...]
    }
```

### Smear Microscopy

```
POST /rest/tb/samples/{sampleId}/smear
  - Record smear result
  - Request: {
      method: "ZN",
      afbResult: "PLUS2",
      resultDate: "2025-01-20",
      testedBy: "Dr. Smith",
      reviewedBy: "Dr. Jones"
    }
  - Response: { smearId: 1, afbResult: "PLUS2" }
```

### Species Identification

```
POST /rest/tb/samples/{sampleId}/identification
  - Record identification result
  - Request: {
      result: "MTB",
      method: "RAPID_TEST_KIT",
      resultDate: "2025-02-10",
      testedBy: "Dr. Smith",
      reviewedBy: "Dr. Jones"
    }
  - Response: { identificationId: 1, result: "MTB", eligibleForDst: true }
```

### GeneXpert

```
POST /rest/tb/samples/{sampleId}/genexpert
  - Record GeneXpert result
  - Request: {
      result: "MTB_RIF_RESISTANT",
      method: "GENEXPERT",
      resultDate: "2025-01-16",
      testedBy: "Tech A",
      reviewedBy: "Dr. Jones"
    }
  - Response: { geneXpertId: 1, result: "MTB_RIF_RESISTANT", rifResistanceAlert: true }
```

### Drug Susceptibility Testing

```
POST /rest/tb/samples/{sampleId}/dst
  - Record DST results
  - Request: {
      method: "PHENOTYPIC_1ST",
      inhResult: "RESISTANT",
      rmpResult: "RESISTANT",
      stmResult: "SENSITIVE",
      embResult: "SENSITIVE",
      pzaResult: "SENSITIVE",
      resultDate: "2025-03-01",
      testedBy: "Tech B",
      reviewedBy: "Dr. Jones"
    }
  - Response: { dstId: 1, mdrFlag: true, mdrAlert: "MDR-TB pattern detected" }

POST /rest/tb/samples/{sampleId}/dst/second-line
  - Record 2nd line DST results
  - Request: {
      method: "PHENOTYPIC_2ND",
      flqResult: "SENSITIVE",
      kanAmkCapResult: "RESISTANT",
      resultDate: "2025-03-15",
      testedBy: "Tech B",
      reviewedBy: "Dr. Jones"
    }
```

### Isolate Storage

```
POST /rest/tb/samples/{sampleId}/storage
  - Assign isolate to storage
  - Request: {
      room: "Room A",
      fridgeId: "Fridge-2",
      compartment: "Compartment 1",
      rack: "Rack 3",
      box: "Box 5"
    }
  - Response: { storageId: 1, location: "Room A > Fridge-2 > Compartment 1 > Rack 3 > Box 5" }

POST /rest/tb/samples/{sampleId}/storage/retrieve
  - Record isolate retrieval
  - Request: { retrievalDate: "2025-06-01", reason: "Additional testing" }
  - Response: { storageId: 1, retrievalDate: "2025-06-01" }
```

### Result Compilation

```
GET /rest/tb/samples/{sampleId}/report
  - Get compiled result report
  - Response: {
      sampleId: "015/25",
      patient: {...},
      culture: {...},
      smear: {...},
      identification: {...},
      geneXpert: {...},
      dst: {...},
      storage: {...}
    }

POST /rest/tb/samples/{sampleId}/report/finalize
  - Finalize report with reviewer info
  - Request: {
      reportedBy: "Dr. Smith",
      reviewedBy: "Dr. Jones",
      comments: "MDR-TB confirmed, refer for treatment adjustment"
    }
  - Response: { reportId: 1, status: "FINALIZED", reportDate: "2025-03-20" }

GET /rest/tb/samples/export
  - Export samples for REDCap
  - Query params: ?fromDate=2025-01-01&toDate=2025-03-31&format=csv
  - Response: CSV file download
```

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory technicians can register a new TB sample with all
  metadata in under 3 minutes
- **SC-002**: QC assessment for a sample completes in under 1 minute with all
  checklist items
- **SC-003**: Weekly culture readings for 50 samples can be recorded in under 30
  minutes using bulk entry
- **SC-004**: Complete 8-week culture tracking has 100% data integrity (no
  missing weeks, all dates recorded)
- **SC-005**: All TB test results (Smear, Culture, GeneXpert, DST) are captured
  with required personnel fields (Tested by, Reviewed by)
- **SC-006**: MDR-TB pattern detection alerts supervisors within 5 seconds of
  DST result entry
- **SC-007**: Isolate storage location can be retrieved within 3 seconds by
  sample ID search
- **SC-008**: Final TB result report compiles all test results in a single view
  for clinician delivery
- **SC-009**: REDCap export generates valid CSV with all sample data in under 30
  seconds for 500 samples

### Quality Gates

- **QG-001**: TB sample ID generation is unique and sequential within each
  calendar year
- **QG-002**: Culture tracking grid renders 8 weeks of readings without
  performance issues
- **QG-003**: All TB-specific pages maintain Carbon Design System compliance
- **QG-004**: All database operations use appropriate indexes (culture readings
  by sample, storage by location)
- **QG-005**: Audit trail captures all result entries with user and timestamp

## UI Wireframes _(mandatory)_

### TB Sample Registration Form

```
+-----------------------------------------------------------------------------+
|  TB SAMPLE REGISTRATION                                                      |
+-----------------------------------------------------------------------------+
|                                                                              |
|  DOCUMENT INFORMATION                                                        |
|  +-----------------------------------------------------------------------+  |
|  | Document Number: [TB-REQ-2025-001                                   ] |  |
|  | Sample ID: [Auto-generated: 015/25]                                   |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  PATIENT INFORMATION                                                         |
|  +-----------------------------------------------------------------------+  |
|  | Patient Name: [                              ]  Age: [   ] Sex: [M v] |  |
|  | Patient ID/Code: [            ]  Study ID: [                        ] |  |
|  | Address: [                                                          ] |  |
|  | Patient Phone: [              ]  Physician Phone: [                 ] |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  SPECIMEN INFORMATION                                                        |
|  +-----------------------------------------------------------------------+  |
|  | Specimen Type: [Sputum v]   Specimen Quality: [Good v]                |  |
|  | Referring Facility: [                                               ] |  |
|  | Received Site: [          ]  Date/Time: [2025-01-15] [09:30]         |  |
|  | Treatment History: [                                                ] |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  CONSENT & TESTING                                                           |
|  +-----------------------------------------------------------------------+  |
|  | Consent Status: [x] Obtained  [ ] Pending                             |  |
|  | Tests Requested: [x] Culture [x] Smear [x] GeneXpert [ ] DST          |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  [Cancel]                                              [Register Sample]     |
+-----------------------------------------------------------------------------+
```

### QC Checklist Page

```
+-----------------------------------------------------------------------------+
|  QUALITY CHECK - Sample 015/25                                               |
+-----------------------------------------------------------------------------+
|                                                                              |
|  QC CHECKLIST                                                                |
|  +-----------------------------------------------------------------------+  |
|  | Item                          | Status    | Notes                     |  |
|  |-------------------------------|-----------|---------------------------|  |
|  | Leak Detection               | [Pass v]  | [                       ] |  |
|  | Temperature (2-8C)           | [Pass v]  | [                       ] |  |
|  | Triple Packaging             | [Pass v]  | [                       ] |  |
|  | Labeling Complete            | [Fail v]  | [Label partially damaged] |  |
|  | Request-Sample Match         | [Pass v]  | [                       ] |  |
|  | Volume Adequate              | [Pass v]  | [                       ] |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  OVERALL RESULT: [Fail - Proceed with Remarks v]                            |
|                                                                              |
|  Rejection Reason: [Mislabeling v] (Required when failed)                   |
|  Remarks: [Label damaged but readable, sample identity confirmed by...]     |
|                                                                              |
|  [Cancel]                                              [Submit QC]           |
+-----------------------------------------------------------------------------+
```

### Culture Tracking Grid

```
+-----------------------------------------------------------------------------+
|  CULTURE TRACKING - Sample 015/25                Method: MGIT                |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Inoculation Date: 2025-01-20                    Status: IN PROGRESS         |
|                                                                              |
|  WEEKLY READINGS                                                             |
|  +-----------------------------------------------------------------------+  |
|  | Week | Due Date   | Reading Date | Growth      | Read By  | Notes     |  |
|  |------|------------|--------------|-------------|----------|-----------|  |
|  |  1   | 2025-01-27 | 2025-01-27   | No Growth   | Tech A   |           |  |
|  |  2   | 2025-02-03 | 2025-02-03   | No Growth   | Tech A   |           |  |
|  |  3   | 2025-02-10 | 2025-02-10   | No Growth   | Tech B   |           |  |
|  |  4   | 2025-02-17 | [          ] | [Select v]  | [      ] | [       ] |  |
|  |  5   | 2025-02-24 |     --       |    --       |   --     |    --     |  |
|  |  6   | 2025-03-03 |     --       |    --       |   --     |    --     |  |
|  |  7   | 2025-03-10 |     --       |    --       |   --     |    --     |  |
|  |  8   | 2025-03-17 |     --       |    --       |   --     |    --     |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  Growth Options: No Growth | Growth Detected | Contaminated                 |
|                                                                              |
|  [Save Reading]                           [Mark Culture Negative (Week 8)]   |
+-----------------------------------------------------------------------------+
```

### DST Result Entry

```
+-----------------------------------------------------------------------------+
|  DRUG SUSCEPTIBILITY TESTING - Sample 015/25                                 |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Method: [Phenotypic DST - 1st Line v]                                      |
|                                                                              |
|  FIRST LINE DRUGS                                                            |
|  +-----------------------------------------------------------------------+  |
|  | Drug                    | Result                                       |  |
|  |-------------------------|---------------------------------------------|  |
|  | INH (Isoniazid)        | ( ) S  (x) R  ( ) Invalid                    |  |
|  | RMP (Rifampicin)       | ( ) S  (x) R  ( ) Invalid                    |  |
|  | STM (Streptomycin)     | (x) S  ( ) R  ( ) Invalid                    |  |
|  | EMB (Ethambutol)       | (x) S  ( ) R  ( ) Invalid                    |  |
|  | PZA (Pyrazinamide)     | (x) S  ( ) R  ( ) Invalid                    |  |
|  +-----------------------------------------------------------------------+  |
|                                                                              |
|  [!] MDR-TB PATTERN DETECTED (INH-R + RMP-R)                                |
|                                                                              |
|  Result Date: [2025-03-01]                                                   |
|  Test Done By: [                    ]  Reviewed By: [                    ]  |
|                                                                              |
|  [Cancel]                                              [Save DST Results]    |
+-----------------------------------------------------------------------------+
```

### Final Result Report

```
+-----------------------------------------------------------------------------+
|  TB RESULT REPORT - Sample 015/25                                            |
+-----------------------------------------------------------------------------+
|  Patient: John Doe (M, 45)   Referring: Hospital A   Received: 2025-01-15   |
+-----------------------------------------------------------------------------+
|                                                                              |
|  SMEAR MICROSCOPY                                                            |
|  Method: ZN (Ziehl-Neelsen)    AFB Result: 2+                               |
|  Date: 2025-01-20    Test By: Dr. Smith    Reviewed By: Dr. Jones           |
|                                                                              |
|  CULTURE                                                                     |
|  Method: MGIT    Result: POSITIVE (Week 4)    Date: 2025-02-17              |
|                                                                              |
|  IDENTIFICATION                                                              |
|  Method: MTB Rapid Test Kit    Result: MTB                                  |
|  Date: 2025-02-20    Test By: Dr. Smith    Reviewed By: Dr. Jones           |
|                                                                              |
|  GENEXPERT                                                                   |
|  Method: GeneXpert MTB/RIF    Result: MTB Detected, Rif RESISTANT           |
|  Date: 2025-01-16    Test By: Tech A    Reviewed By: Dr. Jones              |
|                                                                              |
|  DRUG SUSCEPTIBILITY TESTING (1st Line)                                      |
|  Method: Phenotypic DST                                                      |
|  INH: R | RMP: R | STM: S | EMB: S | PZA: S                                 |
|  [!] MDR-TB PATTERN DETECTED                                                 |
|  Date: 2025-03-01    Test By: Tech B    Reviewed By: Dr. Jones              |
|                                                                              |
|  ISOLATE STORAGE                                                             |
|  Location: Room A > Fridge-2 > Compartment 1 > Rack 3 > Box 5               |
|                                                                              |
|  -------------------------------------------------------------------------  |
|  Reported By: [Dr. Smith           ]  Reviewed By: [Dr. Jones           ]   |
|  Comments: [MDR-TB confirmed, refer for treatment adjustment             ]  |
|  Report Date: 2025-03-20                                                     |
|  -------------------------------------------------------------------------  |
|                                                                              |
|  [Print Report]  [Export PDF]  [Export to REDCap]         [Finalize Report] |
+-----------------------------------------------------------------------------+
```

## Dependencies

### Existing (Reuse from OGC-51)

- **NotebookEntry**: Workflow instance management
- **NotebookPageSample**: Per-sample-per-page tracking
- **SampleStorageService**: Integration for isolate storage
- **Bulk Operations**: Select all, apply to selected patterns
- **Sample Grid UI**: Filtering, pagination, virtualization

### Existing (Core OpenELIS)

- **SampleItem**: Core sample entity
- **Patient**: Patient information
- **SystemUser**: User authentication and audit trail
- **TypeOfSample**: Specimen type reference data

### New Components

- **TB-specific entities**: TbSampleRegistration, TbQualityCheck,
  TbCultureReading, etc.
- **TB result forms**: Smear, Identification, GeneXpert, DST entry pages
- **Culture tracking grid**: 8-week monitoring UI component
- **TB report generator**: Compiled result report

## Out of Scope

- FHIR DiagnosticReport integration (future enhancement)
- Automated GeneXpert instrument interface (manual result entry for now)
- Mobile-optimized UI for field use
- Integration with National TB Program reporting systems
- Patient notification system for results
- Barcode scanner integration (labels are print-only)
- Real-time notifications/websockets
- Multi-site sample sharing/transfer between laboratories
