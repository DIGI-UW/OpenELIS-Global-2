# Feature Specification: Viral and Vaccine Laboratory Workflow

**Feature Branch**: `282-viral-vaccine-workflow`  
**Created**: 2025-12-14  
**Status**: Draft  
**Input**: User description: Viral and Vaccine laboratory workflow including
viral sample processing, PCR/serology testing, vaccine efficacy studies, cold
chain monitoring, biosafety tracking, and cryogenic storage management

## Executive Summary

Viral and vaccine laboratories conduct specialized testing for viral diagnostics
(PCR, serology, viral culture) and vaccine efficacy studies (immunogenicity,
potency, sterility). These workflows require stringent temperature control,
biosafety compliance, and comprehensive tracking from sample receipt through
cryogenic storage and archiving.

This feature extends the existing Notebook/Page architecture (from OGC-51) to
support viral and vaccine laboratory workflows with specialized quality checks,
cold chain monitoring, and biosafety documentation.

**Key Differentiators**:

- **Ultra-cold storage requirements**: -80°C freezers, liquid nitrogen dewars
  for viral isolates and serum banks
- **Cold chain monitoring**: Continuous temperature logging with alarm
  documentation from collection to storage
- **Biosafety level tracking**: BSL-2/BSL-3 compliance documentation for viral
  specimens
- **Multiple testing modalities**: RT-PCR (COVID-19, Influenza, Dengue, HIV),
  ELISA serology, viral culture, neutralization assays
- **Vaccine-specific workflows**: Potency testing, sterility checks,
  immunogenicity studies (pre/post vaccination sera)
- **Batch-based processing**: Vaccine lots and serum panels processed as cohorts
  with pooled controls
- **Aliquoting for biobanking**: Primary samples divided into multiple cryovials
  for long-term storage
- **Dewar/Canister hierarchy**: Liquid nitrogen storage with dewar → canister →
  rack → box → position tracking

**Target Users**: Virologists, vaccine researchers, laboratory technicians,
biosafety officers, cold chain coordinators, quality managers

**Expected Impact**:

- Achieve 100% cold chain compliance from sample collection to cryostorage
- Enable real-time temperature excursion alerts to prevent sample degradation
- Support both diagnostic virology and vaccine research workflows
- Track viral isolates and serum repositories with complete provenance
- Ensure biosafety protocol adherence through documented checks

## Assumptions

Based on viral/vaccine laboratory domain knowledge:

1. **Sample Types**: Viral swabs (nasopharyngeal, throat), blood/serum, viral
   culture specimens, vaccine vials, CSF, tissue
2. **Temperature Zones**: Room temp (20-25°C), refrigerated (2-8°C), frozen
   (-20°C), ultra-cold (-80°C), cryogenic (-196°C LN₂)
3. **Biosafety Levels**: BSL-2 for most viral diagnostics, BSL-3 for select
   agents (requires documentation)
4. **Testing Platforms**: Real-time PCR machines, ELISA readers, flow
   cytometers, viral culture hoods
5. **User Authentication**: Uses existing OpenELIS authentication
6. **Role-Based Access**: Lab Tech, Senior Virologist, Biosafety Officer, Cold
   Chain Manager, Quality Manager roles
7. **Data Retention**: Viral and vaccine data retained per regulatory
   requirements (5-10 years)
8. **Language**: UI supports existing OpenELIS internationalization (English,
   French primary)
9. **Existing Services**: Leverages Notebook, SampleStorage, QualityControl
   services from OGC-51
10. **Analyzer Integration**: PCR instruments may export results; serology
    results are manual entry

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Sample Reception with Cold Chain Documentation (Priority: P0)

Laboratory technicians need to receive viral/vaccine samples with complete cold
chain documentation, recording temperature conditions from collection through
transport, and flagging any excursions.

**Why this priority**: Cold chain integrity is critical for viral specimen
viability and vaccine sample validity. This is the entry point - all downstream
workflows depend on proper cold chain documentation.

**Independent Test**: Register 5 viral samples arriving in different conditions
(dry ice, refrigerated, ambient), document transport temperatures, verify cold
chain status (compliant/excursion/break) is captured.

**Acceptance Scenarios**:

1. **Given** viral samples arrive from a field site, **When** the technician
   opens "Viral Sample Reception", **Then** the form displays fields for: Sample
   ID, patient/participant info, specimen type, collection date/time, biosafety
   level, transport method, temperature log
2. **Given** the sample arrived in a cold box with data logger, **When** the
   technician enters temperature readings, **Then** the system accepts: Dispatch
   temp, arrival temp, transport duration, excursion events (yes/no)
3. **Given** the transport temperature shows 10°C for a sample requiring 2-8°C,
   **When** the technician marks "Temperature Excursion", **Then** the sample is
   flagged with alert icon and excursion details (duration, temp range,
   corrective action)
4. **Given** the sample cold chain was maintained, **When** the technician
   selects "Cold Chain Compliant", **Then** the sample status shows green
   checkmark for cold chain integrity
5. **Given** a sample arrives frozen on dry ice, **When** the technician selects
   transport method "Dry Ice", **Then** the expected temperature range is
   pre-filled as -78°C to -80°C
6. **Given** sample metadata is entered, **When** saving, **Then** all fields
   are stored: specimen type (NP swab, serum, viral culture, vaccine vial, CSF),
   collection site, collector ID, study/protocol number, biosafety level
   (BSL-2/BSL-3), consent status
7. **Given** a batch of 50 samples arrives together, **When** the technician
   uses "Batch Reception", **Then** common fields (transport conditions, arrival
   date/time, receiving staff) are applied to all samples

---

### User Story 2 - Biosafety Level Assignment and PPE Documentation (Priority: P0)

Biosafety officers need to assign biosafety levels to incoming specimens and
document required personal protective equipment (PPE) and containment measures
before processing begins.

**Why this priority**: Biosafety compliance is mandatory before any sample
handling. Incorrect biosafety classification can endanger laboratory personnel.

**Independent Test**: Register a suspected tuberculosis sample, assign BSL-3,
verify downstream processing requires BSL-3 hood documentation before
proceeding.

**Acceptance Scenarios**:

1. **Given** a viral sample is registered, **When** the biosafety officer
   reviews sample details, **Then** the system displays biosafety level options:
   BSL-1, BSL-2, BSL-3, BSL-4 (if facility supports)
2. **Given** the sample is identified as influenza H5N1, **When** the officer
   assigns BSL-3, **Then** the required PPE is auto-populated: N95 respirator,
   double gloves, gown, face shield, work in biosafety cabinet
3. **Given** a sample is marked BSL-3, **When** a technician attempts to open
   the processing page, **Then** the system prompts for biosafety checklist
   confirmation: Appropriate PPE worn, BSL-3 hood operational, spill kit
   available, trained personnel only
4. **Given** the biosafety checklist is incomplete, **When** the technician
   tries to proceed, **Then** the system blocks processing and displays missing
   requirements
5. **Given** a sample is marked BSL-2, **When** viewing required precautions,
   **Then** the system displays: Gloves, lab coat, work on bench or hood
6. **Given** processing is complete, **When** the technician logs out of BSL-3
   work, **Then** the system records: Work start time, end time, personnel ID,
   any incidents (spills, exposures)

---

### User Story 3 - Quality Check (QC) - Viral Specimen Integrity (Priority: P0)

Laboratory technicians must perform initial quality checks on received viral
samples, verifying specimen-type-specific criteria (volume, container integrity,
transport media, hemolysis for serum).

**Why this priority**: QC is the first processing checkpoint. Failed samples
must be documented before reagents and testing resources are consumed.

**Independent Test**: Process 10 viral samples through QC, mark 8 as Pass and 2
as Fail (one for insufficient volume, one for hemolysis), verify QC status is
recorded.

**Acceptance Scenarios**:

1. **Given** a viral sample is in the QC queue, **When** the technician opens
   the QC checklist, **Then** specimen-type-specific criteria are displayed:
   - **Nasopharyngeal swab**: Swab in viral transport medium (VTM), container
     sealed, label legible, volume >1ml
   - **Serum**: No hemolysis, no lipemia, volume ≥0.5ml, clot-free
   - **Viral culture specimen**: Transport medium appropriate, sterility seal
     intact, no leakage
   - **Vaccine vial**: Vial integrity (no cracks), label matches documentation,
     within expiry date, stored at correct temp
   - **CSF**: Sterile container, volume recorded, cell count noted (if
     available)
2. **Given** all QC criteria pass, **When** the technician clicks "QC Pass",
   **Then** the sample status changes to "QC Passed" and is queued for
   aliquoting or testing
3. **Given** a serum sample shows hemolysis, **When** the technician marks "QC
   Fail - Hemolysis", **Then** the sample is flagged, rejection reason is
   recorded, and notification option appears
4. **Given** a swab has insufficient VTM (<0.5ml), **When** marked "QC Fail -
   Insufficient Volume", **Then** the system suggests: Request recollection or
   Proceed with caution (documented)
5. **Given** a vaccine vial label is illegible, **When** marked "QC Fail -
   Labeling Issue", **Then** the sample is quarantined pending clarification
   from sender
6. **Given** a sample fails QC but the principal investigator approves
   proceeding, **When** the technician selects "Proceed with Remarks", **Then**
   the QC failure and PI approval are documented, and the sample continues to
   processing

---

### User Story 4 - Sample Aliquoting for Biobanking (Priority: P1)

Laboratory technicians need to divide primary samples into multiple aliquots
(cryovials) for storage, testing, and future use, creating parent-child
relationships with full traceability.

**Why this priority**: Aliquoting enables biobanking and prevents freeze-thaw
cycles. Each aliquot must be independently trackable with storage location.

**Independent Test**: Aliquot a 2ml serum sample into 4x 0.5ml cryovials, assign
unique IDs to each aliquot, verify all link back to parent sample, assign
storage locations.

**Acceptance Scenarios**:

1. **Given** a QC-passed serum sample (volume 2ml), **When** the technician
   opens "Aliquot Sample", **Then** the form displays: Parent sample ID, total
   volume available, number of aliquots to create, volume per aliquot, aliquot
   type (cryovial, EDTA tube, etc.)
2. **Given** the technician enters "Create 4 aliquots of 0.5ml each", **When**
   clicking "Generate Aliquots", **Then** 4 child sample IDs are created with
   naming convention: PARENT-ID-A1, PARENT-ID-A2, PARENT-ID-A3, PARENT-ID-A4
3. **Given** aliquots are created, **When** viewing aliquot details, **Then**
   each shows: Parent sample ID, aliquot volume, creation date/time, technician
   ID, storage location (to be assigned)
4. **Given** a viral culture sample is being aliquoted, **When** creating
   aliquots, **Then** the technician can select aliquot purpose: Testing,
   Backup, Biobank, Shipment
5. **Given** aliquots are ready for storage, **When** assigning locations,
   **Then** the system allows batch location assignment for aliquots going to
   the same box
6. **Given** an aliquot is used for testing, **When** marking it consumed,
   **Then** the aliquot status changes to "Used", remaining volume is updated to
   0, and parent sample audit log is updated
7. **Given** viewing a parent sample, **When** clicking "View Aliquots",
   **Then** all child aliquots are listed with their current status (Available,
   Used, Discarded) and locations

---

### User Story 5 - PCR/Molecular Testing Workflow (Priority: P1)

Laboratory technicians need to perform RT-PCR or PCR testing for viral
diagnostics (COVID-19, Influenza, Dengue, HIV viral load), recording extraction
details, amplification results, and Ct values.

**Why this priority**: PCR is the primary diagnostic method for viral detection.
Results must be traceable to specific runs with quality control metrics.

**Independent Test**: Run a PCR batch of 20 samples for COVID-19, include
positive/negative controls, enter Ct values, verify results are linked to run ID
and controls are within acceptable ranges.

**Acceptance Scenarios**:

1. **Given** samples are queued for PCR testing, **When** the technician creates
   a "PCR Run", **Then** the form displays: Run ID (auto-generated), run date,
   instrument ID, technician, assay type (COVID-19, Influenza A/B, Dengue, HIV
   VL), kit lot number
2. **Given** the run form is open, **When** the technician adds samples,
   **Then** the system enforces: Maximum 96 wells (if 96-well plate), at least 2
   controls (positive, negative, NTC)
3. **Given** the PCR run is complete, **When** entering results, **Then** for
   each sample the system captures: Target gene (N, ORF1ab, S for COVID-19), Ct
   value, result interpretation (Detected, Not Detected, Indeterminate)
4. **Given** a sample has Ct values for multiple targets, **When** entering
   results, **Then** the system allows entry for each target separately: N gene
   Ct=18.5, ORF1ab Ct=19.2, S gene Ct=20.1
5. **Given** the positive control Ct is outside expected range (Ct <30),
   **When** the system validates, **Then** an alert is displayed: "Positive
   control Ct out of range, review run validity"
6. **Given** the negative control shows amplification (Ct detected), **When**
   entering results, **Then** the system flags: "Negative control
   contamination - run invalid" and suggests repeat
7. **Given** a sample result is "Detected" with Ct <40, **When** saving,
   **Then** the result status is "Positive" and is queued for review/reporting
8. **Given** a sample result is "Not Detected" (Ct >40 or no amplification),
   **When** saving, **Then** the result status is "Negative"
9. **Given** amplification is unclear (weak signal, Ct 38-40 borderline),
   **When** the technician selects "Indeterminate", **Then** the system
   suggests: Repeat test or Request new sample

---

### User Story 6 - Serology/ELISA Testing Workflow (Priority: P1)

Laboratory technicians need to perform ELISA serology tests for antibody
detection (IgG, IgM, IgA), recording optical density (OD) values, calculating
titers, and interpreting results based on cutoff thresholds.

**Why this priority**: Serology is essential for vaccine efficacy studies and
seroprevalence surveys. Results must include quantitative data (OD, titer) and
qualitative interpretation.

**Independent Test**: Run an ELISA plate for anti-SARS-CoV-2 IgG, include
calibrators and controls, enter OD values, verify system calculates titers using
standard curve.

**Acceptance Scenarios**:

1. **Given** serum samples are queued for ELISA, **When** the technician creates
   an "ELISA Run", **Then** the form displays: Run ID, run date, plate ID,
   reader instrument, assay type (anti-spike IgG, anti-nucleocapsid IgG, dengue
   IgM/IgG), kit lot number, expiry date
2. **Given** the ELISA plate is set up, **When** adding samples, **Then** the
   system allows: Up to 96 wells, calibrators (for quantitative assays),
   positive/negative controls, blank wells
3. **Given** the plate is read, **When** entering OD values, **Then** for each
   sample the system captures: OD at 450nm (primary), OD at 630nm (reference,
   optional), net OD (450nm - 630nm)
4. **Given** a quantitative assay uses a standard curve, **When** calibrators
   are entered, **Then** the system calculates: 4-parameter logistic curve fit,
   displays R² value, calculates sample concentrations
5. **Given** sample OD values are entered, **When** the system interprets
   results, **Then** comparison to cutoff value determines: Positive (OD >
   cutoff × 1.1), Negative (OD < cutoff × 0.9), Equivocal (between 0.9-1.1
   cutoff)
6. **Given** the positive control OD is <0.8 (expected >1.0), **When**
   validation occurs, **Then** the system alerts: "Positive control OD below
   threshold - review plate validity"
7. **Given** a sample is "Equivocal", **When** the system interprets, **Then**
   the recommendation is: Repeat in duplicate or Request follow-up sample
8. **Given** a vaccine immunogenicity study sample, **When** results show
   anti-spike IgG = 250 BAU/ml (above protective threshold 15 BAU/ml), **Then**
   the interpretation is "Seroprotective"

---

### User Story 7 - Viral Culture and Isolation (Priority: P2)

Virologists need to isolate and propagate viruses from clinical specimens using
cell culture systems, recording culture initiation, passage history, cytopathic
effect (CPE) observations, and viral titer.

**Why this priority**: Viral culture is required for virus characterization,
sequencing, and production of viral stocks. Each passage must be documented for
traceability.

**Independent Test**: Inoculate a Vero cell culture with a viral swab specimen,
observe CPE over 5 days, harvest supernatant, calculate TCID₅₀, verify culture
lineage is tracked.

**Acceptance Scenarios**:

1. **Given** a viral specimen is selected for culture, **When** the virologist
   opens "Initiate Viral Culture", **Then** the form displays: Specimen ID, cell
   line (Vero, MDCK, A549, etc.), culture medium, inoculation date, inoculation
   volume
2. **Given** the culture is initiated, **When** recording setup, **Then** the
   system generates a Culture ID (e.g., VCU-2025-001) and records: Passage
   number (P0 for primary isolation), flask/well ID, incubation conditions
   (37°C, 5% CO₂)
3. **Given** the culture is incubated, **When** the virologist performs daily
   observations, **Then** for each observation day the system captures: Date,
   time, CPE assessment (None, Mild, Moderate, Severe), confluence (%),
   morphology notes
4. **Given** CPE is observed at day 3, **When** the virologist records "Moderate
   CPE - rounding and detachment", **Then** the culture status changes to "CPE
   Positive - Harvest Ready"
5. **Given** the culture is harvested, **When** recording harvest details,
   **Then** the system captures: Harvest date, volume collected, storage
   aliquots created, next passage planned (yes/no)
6. **Given** a passage is performed, **When** creating the next passage (P1),
   **Then** the system links: Parent culture ID (P0), passage ratio (1:5
   dilution), inoculation volume, new Culture ID (VCU-2025-001-P1)
7. **Given** a viral titer is determined (TCID₅₀ or PFU/ml), **When** entering
   titer, **Then** the system records: Titer value, method (TCID₅₀, plaque
   assay), calculation date, operator
8. **Given** viewing culture lineage, **When** the virologist clicks "Culture
   History", **Then** the system displays: P0 → P1 → P2 chain with dates,
   operators, and CPE observations for each passage

---

### User Story 8 - Vaccine Potency and Sterility Testing (Priority: P2)

Laboratory technicians and quality controllers need to perform potency tests
(antigen content, immunogenicity in animal models) and sterility tests
(bacterial/fungal contamination) on vaccine batches.

**Why this priority**: Potency and sterility are regulatory release criteria for
vaccine lots. Results must be documented per GMP standards.

**Independent Test**: Test a vaccine lot for sterility by inoculating culture
media, incubate for 14 days, observe for growth, verify "Pass/Fail" result is
recorded with lot release decision.

**Acceptance Scenarios**:

1. **Given** a vaccine lot arrives for testing, **When** the quality controller
   opens "Vaccine Lot Testing", **Then** the form displays: Lot number,
   manufacturer, vaccine type (inactivated, subunit, mRNA, viral vector), expiry
   date, storage temp, test type (potency, sterility, endotoxin)
2. **Given** a sterility test is initiated, **When** recording setup, **Then**
   the system captures: Media type (thioglycollate, soybean-casein digest),
   inoculation volume, incubation temperature (20-25°C, 30-35°C), incubation
   duration (14 days)
3. **Given** daily sterility observations are performed, **When** entering
   results, **Then** for each day the system records: Turbidity (clear, slightly
   turbid, turbid), growth observed (yes/no), date of observation
4. **Given** no growth is observed after 14 days, **When** finalizing sterility
   test, **Then** the result is "Sterile - Pass" and the lot is flagged for
   release eligibility
5. **Given** growth is observed on day 5, **When** recording contamination,
   **Then** the result is "Non-Sterile - Fail", the lot is quarantined, and an
   investigation is triggered
6. **Given** a potency test uses ELISA for antigen quantification, **When**
   entering potency results, **Then** the system records: Antigen concentration
   (µg/dose), specification range (e.g., 5-15 µg), result interpretation (Within
   Spec, Out of Spec)
7. **Given** potency is below specification, **When** the result is "Out of
   Spec - Fail", **Then** the lot is quarantined and cannot be released
8. **Given** all tests pass (sterility, potency, endotoxin), **When** the
   quality manager reviews results, **Then** the system allows lot release
   approval with digital signature and release date

---

### User Story 9 - Cryogenic Storage Management (Liquid Nitrogen Dewars) (Priority: P1)

Laboratory staff need to store viral isolates, serum banks, and vaccine samples
in liquid nitrogen dewars, tracking dewar → canister → rack → box → position
hierarchy with temperature monitoring.

**Why this priority**: Cryogenic storage is critical for long-term sample
preservation. Loss of a dewar due to liquid nitrogen depletion can destroy
irreplaceable samples.

**Independent Test**: Register a new liquid nitrogen dewar, create storage
hierarchy (canisters, racks, boxes), assign 50 samples to positions, verify
liquid nitrogen level logging is enabled.

**Acceptance Scenarios**:

1. **Given** a new liquid nitrogen dewar is installed, **When** the cold chain
   coordinator registers it, **Then** the form displays: Dewar ID, capacity
   (liters), location (building, room), temperature (-196°C), alarm system
   (yes/no), responsible personnel
2. **Given** the dewar is registered, **When** creating storage structure,
   **Then** the coordinator defines: Number of canisters (e.g., 6), racks per
   canister (e.g., 4), boxes per rack (e.g., 3), positions per box (9×9 grid =
   81 positions)
3. **Given** a storage box is defined, **When** viewing the box, **Then** the
   system displays a 9×9 grid (A1-I9) showing position status: Empty, Occupied,
   Reserved
4. **Given** a viral isolate sample is ready for cryostorage, **When** assigning
   location, **Then** the system allows selection: Dewar → Canister → Rack → Box
   → Position (e.g., LN2-01 → C2 → R3 → BOX-A → D5)
5. **Given** a position is assigned, **When** the sample is stored, **Then** the
   position status changes to "Occupied", displays sample ID, and records
   storage date/time
6. **Given** liquid nitrogen levels must be monitored, **When** the coordinator
   logs levels weekly, **Then** the system records: Date, LN₂ level (liters or
   %), refill needed (yes/no), refill date (if applicable)
7. **Given** the LN₂ level drops below 20%, **When** the system checks, **Then**
   an alert is generated: "Low liquid nitrogen in Dewar LN2-01 - Refill
   urgently"
8. **Given** a dewar alarm is triggered, **When** the coordinator logs the
   incident, **Then** the system records: Alarm date/time, cause (LN₂ depletion,
   power outage, sensor failure), corrective action taken, samples affected (if
   any)
9. **Given** viewing dewar utilization, **When** the coordinator generates a
   report, **Then** the system displays: Total capacity (positions), occupied
   positions, empty positions, % utilization

---

### User Story 10 - Ultra-Cold Freezer Storage (-80°C) (Priority: P1)

Laboratory staff need to store samples in -80°C freezers with hierarchical
storage (freezer → shelf → rack → box → position), continuous temperature
monitoring, and alarm logging.

**Why this priority**: -80°C storage is used for most biobanked samples.
Temperature excursions can compromise sample integrity, so monitoring is
critical.

**Independent Test**: Register a -80°C freezer, create storage hierarchy, assign
100 samples to positions, log a temperature excursion event (-70°C for 2 hours),
verify alarm documentation.

**Acceptance Scenarios**:

1. **Given** a -80°C freezer is installed, **When** the cold chain coordinator
   registers it, **Then** the form displays: Freezer ID, capacity (number of
   shelves/racks), location, target temperature (-80°C), alarm system type
   (email, SMS, audible), responsible personnel
2. **Given** the freezer is registered, **When** creating storage structure,
   **Then** the coordinator defines: Number of shelves (e.g., 5), racks per
   shelf (e.g., 4), boxes per rack (e.g., 6), positions per box (10×10 grid =
   100 positions)
3. **Given** a storage box is defined, **When** viewing the box, **Then** the
   system displays a 10×10 grid (A1-J10) showing position status: Empty,
   Occupied, Reserved
4. **Given** a serum sample is ready for -80°C storage, **When** assigning
   location, **Then** the system allows selection: Freezer → Shelf → Rack → Box
   → Position (e.g., ULT-01 → S3 → R2 → BOX-12 → F7)
5. **Given** continuous temperature monitoring is enabled, **When** the data
   logger records temperatures, **Then** the system stores: Date/time,
   temperature reading, within range (yes/no)
6. **Given** the temperature rises to -70°C (excursion), **When** the alarm
   triggers, **Then** the system records: Alarm start time, temperature at
   alarm, alert sent (yes/no), acknowledgment by staff
7. **Given** the excursion is resolved (temperature returns to -80°C), **When**
   the coordinator logs resolution, **Then** the system records: Alarm end time,
   duration of excursion, maximum temperature reached, corrective action
   (service call, samples moved), samples potentially affected (list of sample
   IDs in freezer)
8. **Given** a freezer alarm log is reviewed, **When** viewing alarm history,
   **Then** the system displays: All past alarms with start/end times,
   durations, causes (door ajar, compressor failure, power outage), and
   corrective actions

---

### User Story 11 - Cold Chain Compliance Reporting (Priority: P2)

Cold chain coordinators and quality managers need to generate cold chain
compliance reports for regulatory audits, showing temperature logs, excursion
events, and corrective actions for specified date ranges.

**Why this priority**: Regulatory agencies require documented cold chain
compliance. Non-compliance can invalidate study results or cause sample
rejection.

**Independent Test**: Generate a cold chain report for January 2025, verify it
includes all freezers/dewars, temperature logs, excursion events with corrective
actions, and compliance percentage.

**Acceptance Scenarios**:

1. **Given** the quality manager opens "Cold Chain Compliance Reports", **When**
   selecting report parameters, **Then** the form displays: Date range, storage
   types (LN₂ dewars, -80°C freezers, -20°C freezers, 2-8°C refrigerators),
   include excursions only (checkbox), export format (PDF, Excel)
2. **Given** the report is generated, **When** viewing the summary, **Then** the
   system displays: Total storage units monitored, total temperature readings,
   number of excursions, compliance percentage (readings within range / total
   readings × 100)
3. **Given** excursions occurred, **When** viewing excursion details, **Then**
   the report lists: Storage unit ID, date/time, duration, temperature range
   during excursion, samples affected, corrective action, resolution date
4. **Given** a specific freezer had multiple excursions, **When** filtering by
   freezer ID, **Then** the report shows only that freezer's compliance history
5. **Given** the report is exported to PDF, **When** opening the PDF, **Then**
   it includes: Header (lab name, report date), summary statistics, temperature
   graphs (min/max/avg), excursion table, corrective actions table, signatures
   (quality manager, cold chain coordinator)
6. **Given** a regulatory audit is scheduled, **When** the quality manager
   generates an audit report, **Then** the system includes: All excursions in
   the past year, corrective action verification, preventive maintenance
   records, staff training documentation

---

### User Story 12 - Sample Disposal and Autoclave Documentation (Priority: P2)

Laboratory technicians need to dispose of viral specimens and used culture
materials safely through autoclaving, with documentation of autoclave cycles,
temperature/pressure validation, and disposal dates.

**Why this priority**: Biosafety regulations require proper inactivation of
viral materials before disposal. Autoclave records prove compliance with
biosafety protocols.

**Independent Test**: Mark 20 samples for disposal, batch them into an autoclave
run, record cycle parameters (temperature, pressure, duration), verify disposal
is logged with autoclave validation.

**Acceptance Scenarios**:

1. **Given** a viral sample is no longer needed, **When** the technician marks
   it for disposal, **Then** the sample status changes to "Pending Disposal" and
   a disposal reason is recorded (Study complete, QC fail, Expired)
2. **Given** samples are pending disposal, **When** creating an autoclave batch,
   **Then** the system displays: Batch ID, autoclave ID, cycle date, operator,
   list of samples to be autoclaved
3. **Given** the autoclave cycle is run, **When** recording cycle parameters,
   **Then** the system captures: Start time, end time, temperature (≥121°C
   required), pressure (≥15 psi required), duration (≥30 minutes required),
   cycle validation (Pass/Fail)
4. **Given** the autoclave cycle validation is "Pass", **When** confirming
   disposal, **Then** all samples in the batch status changes to "Disposed -
   Autoclaved" and disposal date is recorded
5. **Given** the autoclave cycle validation is "Fail" (temperature <121°C),
   **When** the technician reviews, **Then** the system alerts: "Autoclave cycle
   failed - Re-autoclave required" and samples remain in "Pending Disposal"
   status
6. **Given** disposal is complete, **When** viewing sample history, **Then** the
   audit log shows: Disposal date, disposal method (Autoclaved), autoclave batch
   ID, operator, disposal confirmation
7. **Given** a biosafety audit requires autoclave records, **When** generating
   disposal reports, **Then** the system exports: All autoclave cycles in date
   range, cycle parameters, validation status, samples disposed per cycle

---

### User Story 13 - Result Review and Reporting (Priority: P1)

Senior virologists and laboratory supervisors need to review test results (PCR,
serology, culture), approve finalized results, and generate reports for
clinicians or researchers.

**Why this priority**: Results must be reviewed by qualified personnel before
release to ensure accuracy. Reporting is the final deliverable to clinicians and
researchers.

**Independent Test**: Review a batch of 30 PCR results, approve 28 as final,
flag 2 for repeat, generate a batch report, verify approved results are locked
and available for download.

**Acceptance Scenarios**:

1. **Given** test results are entered, **When** the senior virologist opens
   "Results Review Queue", **Then** the system displays: All pending results
   with sample ID, test type, result value, entry date, technician
2. **Given** a PCR result shows "COVID-19 Detected, Ct N=18.5", **When** the
   reviewer examines it, **Then** the system displays: Sample ID, patient name
   (if not blinded), collection date, run ID, controls status, result
   interpretation
3. **Given** the result is correct and controls passed, **When** the reviewer
   clicks "Approve", **Then** the result status changes to "Approved", is locked
   from further editing, and the approval date + reviewer signature are recorded
4. **Given** a result appears incorrect (Ct value inconsistent with clinical
   picture), **When** the reviewer selects "Flag for Repeat", **Then** the
   sample is returned to testing queue with a note: "Repeat requested - Reviewer
   comments: [text]"
5. **Given** all results in a batch are approved, **When** the reviewer
   generates a report, **Then** the system creates a PDF report containing: Lab
   header, sample IDs, patient identifiers (if included), test types, results,
   interpretation, reference ranges, reviewer signature, report date
6. **Given** a research study requires batch export, **When** the researcher
   exports results to Excel, **Then** the file contains: Sample ID, study ID,
   test date, test type, quantitative results (Ct, OD, titer), qualitative
   interpretation, approval status
7. **Given** a result report is generated, **When** the clinician downloads it,
   **Then** the system logs: Report generated date, downloaded by (user ID),
   download date/time

---

## Requirements _(mandatory)_

### Functional Requirements

**Sample Management**:

- **FR-001**: System MUST register viral and vaccine samples with cold chain
  documentation (dispatch temp, arrival temp, transport duration, excursions)
- **FR-002**: System MUST assign biosafety levels (BSL-1, BSL-2, BSL-3, BSL-4)
  to specimens and enforce biosafety checklist completion before processing
- **FR-003**: System MUST support specimen-type-specific QC checks (VTM volume
  for swabs, hemolysis for serum, vial integrity for vaccines)
- **FR-004**: System MUST create parent-child relationships during aliquoting
  with unique IDs for each aliquot
- **FR-005**: System MUST track aliquot status (Available, Used, Discarded) and
  remaining volume

**Testing Workflows**:

- **FR-006**: System MUST support PCR run creation with sample assignment,
  control inclusion (positive, negative, NTC), and kit lot tracking
- **FR-007**: System MUST capture multi-target PCR results (gene name, Ct value,
  interpretation: Detected/Not Detected/Indeterminate)
- **FR-008**: System MUST validate PCR controls against expected ranges and
  alert when out of range
- **FR-009**: System MUST support ELISA run creation with calibrators, controls,
  and OD value entry
- **FR-010**: System MUST calculate ELISA results using standard curves
  (4-parameter logistic fit) and cutoff-based interpretation
  (Positive/Negative/Equivocal)
- **FR-011**: System MUST track viral culture passage history (P0 → P1 → P2...)
  with CPE observations and harvest dates
- **FR-012**: System MUST record viral titers (TCID₅₀, PFU/ml) with method and
  calculation date
- **FR-013**: System MUST support vaccine lot testing workflows (sterility,
  potency, endotoxin) with GMP documentation

**Cryogenic Storage**:

- **FR-014**: System MUST support hierarchical liquid nitrogen storage (dewar →
  canister → rack → box → position)
- **FR-015**: System MUST support hierarchical -80°C freezer storage (freezer →
  shelf → rack → box → position)
- **FR-016**: System MUST display storage box positions as grids (9×9 for LN₂
  boxes, 10×10 for freezer boxes) with occupancy status
- **FR-017**: System MUST log liquid nitrogen levels with refill tracking and
  low-level alerts (<20%)
- **FR-018**: System MUST record continuous temperature monitoring with
  date/time stamps
- **FR-019**: System MUST capture temperature excursion events (alarm start/end
  time, temperature range, duration, corrective action, affected samples)

**Biosafety & Disposal**:

- **FR-020**: System MUST document PPE requirements per biosafety level and
  enforce checklist completion
- **FR-021**: System MUST log biosafety incidents (spills, exposures) with
  date/time, personnel, and corrective actions
- **FR-022**: System MUST batch samples for autoclave disposal with cycle
  parameter recording (temperature, pressure, duration)
- **FR-023**: System MUST validate autoclave cycles against requirements
  (≥121°C, ≥15 psi, ≥30 minutes) and flag failed cycles

**Result Review & Reporting**:

- **FR-024**: System MUST provide result review queues with
  approve/reject/repeat actions
- **FR-025**: System MUST lock approved results from further editing and record
  reviewer signature + approval date
- **FR-026**: System MUST generate result reports (PDF, Excel) with lab header,
  results, interpretation, reviewer signature
- **FR-027**: System MUST generate cold chain compliance reports with
  temperature logs, excursion events, and compliance percentage

### Key Entities _(include if feature involves data)_

- **ViralSample**: Represents a viral specimen with attributes: Sample ID,
  specimen type (swab, serum, culture, vaccine, CSF), collection date, biosafety
  level, cold chain status, QC status
- **ColdChainLog**: Records temperature events during transport and storage:
  Dispatch temp, arrival temp, excursion events, corrective actions
- **BiosafetyClearance**: Documents biosafety level assignment and PPE
  checklist: Biosafety level, required PPE, checklist completion, incidents
- **SampleAliquot**: Child samples created from parent: Parent ID, aliquot ID,
  volume, purpose (testing/backup/biobank), status (available/used), location
- **PCRRun**: RT-PCR or PCR test batch: Run ID, instrument, assay type, kit lot,
  technician, samples, controls
- **PCRResult**: Individual sample result: Sample ID, run ID, target genes, Ct
  values, interpretation (Detected/Not Detected/Indeterminate)
- **ELISARun**: ELISA test batch: Run ID, plate ID, assay type, kit lot,
  calibrators, controls, samples
- **ELISAResult**: Individual sample result: Sample ID, run ID, OD values,
  calculated concentration/titer, interpretation (Positive/Negative/Equivocal)
- **ViralCulture**: Virus isolation and propagation: Culture ID, specimen ID,
  cell line, passage number, CPE observations, harvest dates, viral titer
- **VaccineLot**: Vaccine batch for testing: Lot number, manufacturer, vaccine
  type, sterility result, potency result, release status
- **CryogenicDewar**: Liquid nitrogen storage: Dewar ID, capacity, canisters,
  LN₂ levels, alarms
- **UltraColdFreezer**: -80°C freezer storage: Freezer ID, shelves, temperature
  logs, alarms
- **StoragePosition**: Hierarchical storage location: Dewar/Freezer →
  Canister/Shelf → Rack → Box → Position, occupancy status, sample ID
- **TemperatureLog**: Continuous monitoring records: Storage unit ID, date/time,
  temperature, within range (yes/no)
- **TemperatureExcursion**: Alarm events: Storage unit ID, start time, end time,
  temperature range, affected samples, corrective action
- **AutoclaveCycle**: Disposal documentation: Batch ID, autoclave ID, cycle
  date, temperature, pressure, duration, validation (Pass/Fail), disposed
  samples
- **ResultReview**: Result approval tracking: Result ID, reviewer, approval
  status (pending/approved/rejected/repeat), approval date, reviewer signature

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory staff can register viral samples with complete cold
  chain documentation in under 3 minutes per sample
- **SC-002**: 100% of viral specimens have biosafety level assigned and
  biosafety checklist completed before processing
- **SC-003**: PCR and ELISA results are traceable to specific run IDs with
  control validation within acceptable ranges 95% of the time
- **SC-004**: Cryogenic storage locations are tracked with 100% position
  accuracy (no lost samples)
- **SC-005**: Temperature excursions in -80°C freezers and LN₂ dewars are
  detected and logged within 5 minutes of alarm trigger
- **SC-006**: Cold chain compliance reports show >95% temperature readings
  within acceptable range for all storage units
- **SC-007**: Sample aliquoting creates complete parent-child traceability with
  100% of aliquots linked to parent sample
- **SC-008**: Viral culture passage history is documented with 100% lineage
  traceability from P0 to final passage
- **SC-009**: Autoclave cycle validation flags failed cycles 100% of the time
  (temperature <121°C or duration <30 minutes)
- **SC-010**: Result review and approval workflow reduces result release time
  from 48 hours to 24 hours
- **SC-011**: Laboratory supervisors can generate cold chain compliance reports
  for regulatory audits in under 10 minutes
- **SC-012**: Sample retrieval time from cryostorage reduced from 20 minutes
  (paper logbook search) to 2 minutes (system lookup)
