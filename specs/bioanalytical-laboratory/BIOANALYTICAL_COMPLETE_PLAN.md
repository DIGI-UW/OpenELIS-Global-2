# Bioanalytical & Bioequivalence Laboratory - Complete Implementation Plan v2.0

**Version:** 2.0 (Updated with Role-Based Access, Analyzer Integration, System
Requirements, Metrics) **Date:** 2026-01-06 **Status:** Comprehensive
Specification Ready for Implementation

---

## PART 1: WORKFLOW ARCHITECTURE & DATA STRUCTURE

(Reference: BIOANALYTICAL_IMPLEMENTATION_PLAN.md Sections 1-6)

### Summary of Stages:

1. **Stage 1:** Sample Reception & Registration (BATCH_VERIFICATION)
2. **Stage 2:** Test Assignment & Preparation (BULK_DATA_ENTRY)
3. **Stage 3:** Analytical Test Execution (BULK_DATA_ENTRY)
4. **Stage 4:** Reporting & Release (AGGREGATION)
5. **Stage 5:** Post-Test Storage & Archiving (BATCH_VERIFICATION)

---

## PART 2: ROLE-BASED ACCESS CONTROL

### Role Matrix (9 Roles)

| Role                  | Registration | Test Assignment | Analysis | Result Entry | Validation     | Reporting        | Sample Transfer  |
| --------------------- | ------------ | --------------- | -------- | ------------ | -------------- | ---------------- | ---------------- |
| **Sample Receivers**  | View         | No              | No       | No           | No             | No               | No               |
| **Chemical Analysts** | Update       | View            | Full     | Full         | Validate       | Limited          | No               |
| **Pharmacists**       | Update       | Full            | Full     | Full         | Validate       | Full             | No               |
| **Researchers**       | View         | View            | View     | View         | Review         | Project-specific | No               |
| **Lab Supervisors**   | Full         | Full            | Full     | Full         | Final Approval | Full             | Approve          |
| **Study Directors**   | View         | View            | View     | View         | Final Approval | Full             | Approve          |
| **QA Officers**       | View         | View            | View     | View         | Final Approval | Full             | No               |
| **Data Managers**     | No           | No              | No       | View         | No             | Full Analytics   | No               |
| **Biorepository**     | No           | No              | No       | No           | No             | No               | Approve Transfer |

### Access Control Implementation Strategy

**Cascading Access Control (BOTH Page-Level AND Field-Level):**

**Level 1: Page-Level Access**

- `notebook_page.allowed_roles` determines if entire page is visible
- Checked in `usePageAccessControl()` hook
- Pages hidden if user role not in allowed_roles

**Level 2: Field-Level Access (within visible pages)**

- Individual data points have conditional visibility/editability by role
- Example: "Validation" sections only editable by Pharmacist, Lab Supervisor, QA
- Configuration in `fieldAccessControl` section of page metadata JSON

**Example Implementation in Page JSON:**

```json
{
  "pageDescription": "Sample Reception & Registration",
  "allowedRoles": [
    "Sample Receivers",
    "Chemical Analysts",
    "Pharmacists",
    "Lab Supervisors"
  ],
  "dataPoints": {
    "sampleMetadata": {
      "uniqueSampleId": {
        "type": "text",
        "label": "Sample ID",
        "requiredForRoles": ["Sample Receivers", "Chemical Analysts"],
        "readonlyForRoles": ["Researchers"],
        "hiddenForRoles": ["QA Officers", "Data Managers"]
      },
      "manifestVerificationStatus": {
        "type": "choice",
        "label": "Manifest Verification",
        "visibleToRoles": [
          "Sample Receivers",
          "Pharmacists",
          "Lab Supervisors"
        ],
        "editableByRoles": ["Sample Receivers", "Pharmacists"],
        "validateByRoles": ["Pharmacists", "Lab Supervisors"]
      }
    }
  }
}
```

### Stage-Specific Role Permissions

**Stage 1: Sample Reception & Registration**

- Sample Receivers: Register samples, enter metadata
- Chemical Analysts: Update metadata, assist with manifest verification
- Pharmacists: Review, approve manifest verification
- Lab Supervisors: Full control, final approval

**Stage 2: Test Assignment & Preparation**

- Chemical Analysts: View assignments, document preparation steps
- Pharmacists: Assign tests, select analytical methods, approve methodology
- Lab Supervisors: Full control, authorize preparation

**Stage 3: Analytical Test Execution**

- Chemical Analysts: Execute tests, capture results, upload raw data
- Pharmacists: Validate QC results, approve methodology execution
- Lab Supervisors: Full control, final result validation

**Stage 4: Reporting & Release**

- Chemical Analysts: View results, enter limited data
- Pharmacists: Compile results, primary review (analyst validation)
- Lab Supervisors: Secondary review (senior scientist validation)
- QA Officers: Final approval
- Study Directors: Final approval for studies
- Data Managers: View full analytics/dashboards

**Stage 5: Post-Test Storage & Archiving**

- Chemical Analysts: View storage documentation
- Pharmacists: Document storage, track retention
- Lab Supervisors: Approve storage, authorize transfers
- Biorepository: View, approve long-term storage transfers

---

## PART 3: ANALYZER/INSTRUMENT INTEGRATION

### 11 Supported Instruments

| #   | Machine                      | Data Output | Integration Type | Manual Entry     | Status |
| --- | ---------------------------- | ----------- | ---------------- | ---------------- | ------ |
| 1   | LC-MS/MS                     | Yes         | **Automatic**    | Bidirectional    | Active |
| 2   | HPLC                         | Yes         | **Automatic**    | Bidirectional    | Active |
| 3   | Dissolution Apparatus        | Yes         | Both             | Both             | Active |
| 4   | Disintegration Tester        | Yes         | Manual           | Manual           | Active |
| 5   | Hardness Tester              | Yes         | Manual           | Manual           | Active |
| 6   | Friability Tester            | Yes         | Manual           | Manual           | Active |
| 7   | Stability Chamber            | Yes         | **Automatic**    | Bidirectional    | Active |
| 8   | UV-Vis Spectrophotometer     | Yes         | Both             | Both             | Active |
| 9   | FTIR                         | Yes         | Both             | Both             | Active |
| 10  | Freezers (-20°C, -80°C)      | Yes         | Manual           | Automated Logger | Active |
| 11  | Millipore Water Purification | Yes         | **Automatic**    | Automated Logger | Active |

### Integration Architecture

**Selected Approach: Extend AnalyzerResultImportService with Bioanalytical
Adapter**

1. **Automatic Data Integration (3 instruments: LC-MS/MS, HPLC, Stability
   Chamber)**

   - Extend existing `AnalyzerResultImportService` to support bioanalytical
     formats
   - Create `BioanalyticalAnalyzerDataAdapter` interface
   - Support file formats:
     - CDF (chromatograms from HPLC)
     - mzML (mass spectra from LC-MS/MS)
     - CSV/Excel (physical test results, stability data)
   - Automatic file parsing and import to notebook page

2. **Bidirectional Integration Capability**

   - System can IMPORT instrument output AND EXPORT parameters to instruments
   - Example: Write calibration curve to HPLC before analytical run

3. **Manual Data Entry (6 instruments: Disintegration, Hardness, Friability,
   UV-Vis, FTIR, Physical)**

   - Operators enter results manually in Stage 3 "Analytical Test Execution"
   - Stored in same schema as automatic imports for consistency

4. **Environmental Monitoring (Freezers, Water Purification)**
   - Automated data loggers (if available) export monitoring data
   - Manual entry fallback for legacy equipment
   - Data linked to notebook entry via `environmental_monitoring` table

### Backend Implementation

**New Java Classes to Create:**

```java
// Interface for bioanalytical analyzer data handling
public interface BioanalyticalAnalyzerDataAdapter {
  ParsedAnalyzerData parseInstrumentOutput(File dataFile, String instrumentType);
  void writeCalibrationToInstrument(Instrument instrument, CalibrationCurve curve);
  List<RawDataFile> importChromatograms(NotebookEntry entry);
  List<RawDataFile> importMassSpectra(NotebookEntry entry);
  AnalyzerIntegrationResult validateInstrumentData(ParsedAnalyzerData data);
}

// Implementation class
@Service
public class BioanalyticalAnalyzerDataAdapterImpl
    extends AnalyzerResultImportService
    implements BioanalyticalAnalyzerDataAdapter {

  @Transactional
  @Override
  public ParsedAnalyzerData parseInstrumentOutput(File dataFile, String instrumentType) {
    // Parse CDF, mzML, CSV based on instrumentType
    // Return structured data ready for storage
  }

  @Transactional
  @Override
  public List<RawDataFile> importChromatograms(NotebookEntry entry) {
    // Import HPLC chromatogram files
  }

  @Transactional
  @Override
  public List<RawDataFile> importMassSpectra(NotebookEntry entry) {
    // Import LC-MS/MS raw spectral data
  }
}
```

### Database Schema for Analyzer Integration

**New Tables:**

```sql
-- Raw data files from instruments
CREATE TABLE analytical_raw_file (
  id SERIAL PRIMARY KEY,
  notebook_entry_id INTEGER NOT NULL REFERENCES notebook_entry(id),
  instrument_id INTEGER REFERENCES instrument(id),
  filename VARCHAR(255) NOT NULL,
  file_type VARCHAR(20), -- CDF, mzML, CSV, PDF
  file_path VARCHAR(500) NOT NULL,
  upload_date TIMESTAMP NOT NULL,
  uploaded_by_id INTEGER NOT NULL,
  file_size_bytes BIGINT,
  data_format VARCHAR(100),
  date_created TIMESTAMP NOT NULL,
  FOREIGN KEY (uploaded_by_id) REFERENCES systemuser(id)
);

-- Calibration curves per analytical run
CREATE TABLE calibration_curve (
  id SERIAL PRIMARY KEY,
  notebook_entry_id INTEGER NOT NULL REFERENCES notebook_entry(id),
  instrument_id INTEGER REFERENCES instrument(id),
  curve_equation VARCHAR(500),
  r_squared DECIMAL(5,4), -- e.g., 0.9995
  slope DECIMAL(15,8),
  intercept DECIMAL(15,8),
  concentration_range_low DECIMAL(15,8),
  concentration_range_high DECIMAL(15,8),
  acceptance_criteria VARCHAR(500),
  curve_status VARCHAR(20), -- PASS, FAIL
  created_date TIMESTAMP NOT NULL,
  analyst_id INTEGER NOT NULL REFERENCES systemuser(id)
);

-- Environmental monitoring (temperature, humidity)
CREATE TABLE environmental_monitoring (
  id SERIAL PRIMARY KEY,
  notebook_entry_id INTEGER REFERENCES notebook_entry(id),
  storage_location_id INTEGER,
  measurement_type VARCHAR(50), -- TEMPERATURE, HUMIDITY
  measured_value DECIMAL(10,2),
  measurement_unit VARCHAR(10), -- C, F, %RH
  measurement_date TIMESTAMP NOT NULL,
  logged_manually BOOLEAN DEFAULT false,
  alert_triggered BOOLEAN DEFAULT false,
  corrective_action_taken VARCHAR(500)
);
```

---

## PART 4: KEY SYSTEM REQUIREMENTS

### 4.1 Sample Source Tracking

**Requirement:** Distinguish samples from Medical Laboratory (CTD-BE) vs.
external clients

**Implementation:**

- Sample metadata includes `sourceOrigin` (Medical Lab vs. External)
- For Medical Lab samples: Link to originating CTD-BE sample via
  `sourceLabSampleId`
- Maintain chain of custody with timestamp and receiving personnel
- Enable tracking of sample transfer events

**Database Support:**

```sql
-- Extend sample table
ALTER TABLE sample ADD COLUMN source_laboratory VARCHAR(100);
ALTER TABLE sample ADD COLUMN source_lab_sample_id VARCHAR(255);
ALTER TABLE sample ADD COLUMN chain_of_custody_path TEXT; -- JSON array of transfer events
```

### 4.2 Test Method Selection

**Requirement:** Dropdown selection of analytical methodology with conditional
options

**Implementation in Stage 2:**

```
User selects Test Category → System shows appropriate Analytical Methods

  Example Flow:
  Test Category: "Bioanalytical (HPLC)"
    → Analytical Methods: [HPLC UV-Vis, HPLC Fluorescence, HPLC RI]

  Test Category: "Pharmaceutical Quality (Physical)"
    → Analytical Methods: [Dissolution USP I, Dissolution USP II, Disintegration, Hardness, Friability]
```

**Implementation:**

- Metadata JSON includes conditional field options based on testCategory value
- Frontend handles dynamic dropdown updates
- Backend validates selected method matches test category

### 4.3 Bioequivalence Study Support

**Requirements:**

- Link samples to BE study protocols
- Track pharmacokinetic sampling timepoints
- Calculate PK parameters (optional, future integration)
- Auto-flag samples for 2-year retention at -80°C
- Generate regulatory-compliant exports

**Implementation:**

Database Tables:

```sql
CREATE TABLE bioequivalence_study (
  id SERIAL PRIMARY KEY,
  study_code VARCHAR(100) UNIQUE NOT NULL,
  study_title VARCHAR(500),
  sponsor VARCHAR(255),
  protocol_version VARCHAR(50),
  active_from_date DATE,
  active_to_date DATE,
  target_sample_count INTEGER,
  retention_period_days INTEGER DEFAULT 730, -- 2 years
  regulatory_submission_required BOOLEAN,
  date_created TIMESTAMP
);

CREATE TABLE be_sample_assignment (
  id SERIAL PRIMARY KEY,
  sample_id INTEGER NOT NULL REFERENCES sample(id),
  study_id INTEGER NOT NULL REFERENCES bioequivalence_study(id),
  subject_id VARCHAR(100),
  sampling_timepoint VARCHAR(100), -- "0h", "0.5h", "1h", etc.
  sampling_time TIMESTAMP,
  expected_retention_until DATE,
  regulatory_submission_included BOOLEAN
);
```

**Stage 1 Addition:**

- Bioequivalence Study Reference dropdown (optional field)
- Subject ID and Sampling Timepoint fields (if BE study selected)

**Stage 5 Automation:**

- If sample linked to BE study → Auto-flag for biorepository transfer
- Calculate expiration date based on study retention period
- Flag for regulatory document archiving

### 4.4 Analytical Data Management

**Requirements:**

- Upload/store chromatograms, spectra, raw instrument files
- Store calibration curves with metadata for each analytical run
- Auto-calculate concentrations from calibration curves
- Auto-flag out-of-range results

**Implementation:**

- Use existing `analytical_raw_file` table (created above)
- Calibration curve storage in `calibration_curve` table
- Concentration calculation service:
  ```java
  CalculatedResult calculateConcentration(CalibrationCurve curve, PeakArea peakArea) {
    concentration = (peakArea - curve.intercept) / curve.slope;
    if (concentration < curve.concentrationRangeLow) flag = "BELOW_LLOQ";
    if (concentration > curve.concentrationRangeHigh) flag = "ABOVE_RANGE";
    return CalculatedResult(concentration, flag);
  }
  ```
- Validation rules in Stage 3 Analytical Execution page

### 4.5 QC Sample Management

**Requirements:**

- Track QC sample preparation (Low, Medium, High concentrations)
- Record QC results with acceptance criteria
- Generate QC trending charts (Levey-Jennings plots)
- Trigger alerts if QC fails

**Implementation:**

Database Table:

```sql
CREATE TABLE qc_result (
  id SERIAL PRIMARY KEY,
  notebook_entry_id INTEGER NOT NULL REFERENCES notebook_entry(id),
  instrument_id INTEGER REFERENCES instrument(id),
  qc_level VARCHAR(20), -- LOW, MEDIUM, HIGH
  expected_concentration DECIMAL(15,8),
  measured_concentration DECIMAL(15,8),
  percent_recovery DECIMAL(10,2),
  acceptance_criteria_lower DECIMAL(10,2), -- e.g., 85%
  acceptance_criteria_upper DECIMAL(10,2), -- e.g., 115%
  pass_fail VARCHAR(10), -- PASS, FAIL
  batch_run_number VARCHAR(100),
  created_date TIMESTAMP
);
```

**Service for QC Trending:**

```java
@Service
public class QCTrendingService {

  public QCTrendReport generateTrendingChart(
      Integer instrumentId,
      LocalDate startDate,
      LocalDate endDate) {
    // Generate Levey-Jennings plot data
    // Detect out-of-control patterns (Westgard rules)
  }

  public List<QCAlert> checkForFailures(QCResult qcResult) {
    // Validate against acceptance criteria
    // Flag if outside limits
  }
}
```

### 4.6 Result Export

**Requirements:**

- Export validated results to Medical Laboratory LMIS
- Export to REDCap (bioequivalence studies)
- Export to regulatory submission formats
- Support CDISC/SDTM format (optional for BE studies)

**Implementation:**

Export Service Interfaces:

```java
public interface AnalyticalResultExporter {
  ExportResult exportToLMIS(NotebookEntry entry);
  ExportResult exportToREDCap(NotebookEntry entry, String studyCode);
  ExportResult exportToRegulatoryFormat(NotebookEntry entry, String format); // CDISC, SDTM
}
```

Export Configuration:

- Field mapping stored in database for each export format
- REDCap export: Map bioanalytical result fields to REDCap project fields
- LMIS export: Structured JSON/XML matching Medical Lab schema
- Audit trail: All exports logged with timestamp, user, destination

---

## PART 5: PERFORMANCE METRICS & REPORTING

### Dashboards (Embedded in Stage 4: Reporting & Release)

**Dashboard 1: Real-Time Sample Throughput**

- Samples Received (cumulative)
- Samples Analyzed (cumulative)
- Samples Reported (cumulative)
- Average Turnaround Time (TAT) by test type
- Analytical queue status (pending, in-progress, completed)

**Dashboard 2: Quality Metrics**

- QC Pass/Fail rates (% passing QC for each instrument)
- Calibration curve acceptance rates (% curves with r² ≥ acceptance threshold)
- Instrument performance trends:
  - Uptime percentage
  - Maintenance events
  - System suitability test pass rates
- Method validation status (validated, pending validation, invalidated)

**Dashboard 3: Study-Specific Progress** (for bioequivalence studies)

- BE study progress toward sample completion target
- Sampling timepoint coverage completeness
- Sample integrity status (good, compromised, lost)
- Subjects completed vs. enrolled

**Dashboard 4: Instrument Utilization**

- Percent time in use per instrument
- Analytical run queue depth
- Failed run rates by instrument

### Frontend Implementation

**Component: BioanalyticalReportingDashboard.js** (Embedded in Stage 4)

```javascript
// Data structure for dashboard
const dashboardData = {
  throughput: {
    samplesReceived: 150,
    samplesAnalyzed: 120,
    samplesReported: 100,
    averageTAT_days: 3.5,
    tatByTestType: {
      "Drug Concentration (HPLC)": 2.1,
      "Drug Concentration (LC-MS/MS)": 3.5,
      Assay: 1.8,
      Dissolution: 4.2,
    },
  },
  quality: {
    qcPassRate: 98.5,
    calibrationAcceptanceRate: 99.2,
    instrumentMetrics: [
      { instrument: "LC-MS/MS", uptime: 97.8, maintenanceEvents: 2 },
      { instrument: "HPLC", uptime: 98.1, maintenanceEvents: 1 },
    ],
  },
  studyProgress: [
    {
      studyId: "BE-2025-001",
      studyTitle: "Bioequivalence of Formulation X",
      targetSamples: 60,
      completedSamples: 45,
      progressPercent: 75,
      samplingTimepointsCovered: "0h, 0.5h, 1h, 2h, 3h (18/20 timepoints)",
    },
  ],
};
```

### Backend Implementation

**Query Services:**

```java
@Service
public class ReportingMetricsService {

  public ThroughputMetrics calculateThroughputMetrics(LocalDate startDate, LocalDate endDate) {
    // Query notebook_page_entry for timeline data
    // Calculate averages by test type
  }

  public QualityMetrics aggregateQualityMetrics(LocalDate startDate, LocalDate endDate) {
    // Query qc_result, calibration_curve tables
    // Calculate pass rates
  }

  public StudyProgressMetrics getStudyProgress(Integer bioequivalenceStudyId) {
    // Query be_sample_assignment for completion status
  }

  public List<InstrumentMetric> getInstrumentUtilization() {
    // Query analytical_execution for run counts
  }
}
```

### External Reporting

**Recipients:**

- Study sponsors (bioequivalence results summary)
- Regulatory agencies (detailed analysis data, CDISC format)
- Medical Laboratory (result integration)
- Principal Investigators (study outcomes)

**Report Types:**

- BE_REGULATORY: Detailed analysis report for regulatory submission
- MEDICAL_LAB_INTEGRATION: Result summary for Medical Lab LMIS
- STUDY_SUMMARY: Progress report for study coordinators
- QC_CERTIFICATION: QC pass/fail documentation for audit

**Implementation:**

- Scheduled exports (e.g., weekly reports)
- On-demand export via UI (Stage 4 Reporting page)
- Audit trail of all exports (timestamp, user, recipient, content hash)

---

## PART 6: IMPLEMENTATION PHASES

### Phase 1: Database Schema (Liquibase)

**Files to Create:**

1. `011-bioanalytical-workflow-pages.xml`

   - Create workflow_page_template records for 5 stages
   - Define page types, instructions, default content (JSON)

2. `012-bioanalytical-notebook-template.xml`

   - Create parent notebook template (isTemplate=true)
   - Create 5 notebook_page records with allowedRoles

3. `013-bioanalytical-supporting-tables.xml`
   - Create `analytical_raw_file` table
   - Create `calibration_curve` table
   - Create `qc_result` table
   - Create `environmental_monitoring` table
   - Create `bioequivalence_study` table
   - Create `be_sample_assignment` table

### Phase 2: Backend Services

**Services to Create:**

1. `BioanalyticalManifestImportService` (interface)
2. `BioanalyticalManifestImportServiceImpl` (implementation)
3. `BioanalyticalAnalyzerDataAdapter` (interface for instrument integration)
4. `BioanalyticalAnalyzerDataAdapterImpl` (extends AnalyzerResultImportService)
5. `QCTrendingService` (QC management)
6. `ReportingMetricsService` (dashboard metrics)
7. `AnalyticalResultExporter` (export to LMIS, REDCap, regulatory)

**Controllers to Create:**

1. `BioanalyticalManifestImportController` (REST endpoints for CSV import)
2. `BioanalyticalAnalysisController` (REST endpoints for test execution data)
3. `BioanalyticalReportingController` (REST endpoints for reporting/export)

**Forms to Create:**

1. `BioanalyticalManifestImportForm` (CSV data binding)

### Phase 3: Frontend Components

**Workflow Tab:**

- `BioanalyticalWorkflowTab.js` (main workflow coordinator)

**Manifest Import Modal:**

- `BioanalyticalManifestImportModal.js` (CSV upload interface)

**Stage-Specific Page Components:**

- `BioanalyticalSampleReceptionPage.js` (Stage 1)
- `BioanalyticalTestAssignmentPage.js` (Stage 2)
- `BioanalyticalAnalyticalExecutionPage.js` (Stage 3)
- `BioanalyticalReportingPage.js` (Stage 4) - **includes embedded dashboard**
- `BioanalyticalStorageArchivingPage.js` (Stage 5)
- `index.js` (exports all page components)

**Shared Dashboard Component:**

- `BioanalyticalReportingDashboard.js` (embedded in Stage 4)

### Phase 4: Role-Based Access Control Enforcement

**Updates to Existing Components:**

- Update `NoteBookInstanceEntryForm.js` to apply cascading role filters
- Enhance `usePageAccessControl()` hook to support field-level filtering
- Update notebook_page loading logic to apply field-level ACL

### Phase 5: Workflow Registration

**Updates to NoteBookInstanceEntryForm.js:**

```javascript
{noteBookData?.title?.toLowerCase().includes("bioanalytical") &&
  <BioanalyticalWorkflowTab notebookId={...} />}
```

### Phase 6: Internationalization (i18n)

**Translation Keys:**

```
workflow.bioanalytical.title=Bioanalytical & Bioequivalence Laboratory
workflow.bioanalytical.stage1=Sample Reception & Registration
workflow.bioanalytical.stage2=Test Assignment & Preparation
workflow.bioanalytical.stage3=Analytical Test Execution
workflow.bioanalytical.stage4=Reporting & Release
workflow.bioanalytical.stage5=Post-Test Storage & Archiving

role.sampleReceivers=Sample Receivers
role.chemicalAnalyst=Chemical Analysts
role.pharmacist=Pharmacists
role.researcher=Researchers
role.labSupervisor=Lab Supervisors
role.studyDirector=Study Directors
role.qaOfficer=QA Officers
role.dataManager=Data Managers
role.biorepository=Biorepository

... (additional labels for all data points across 5 stages)
```

### Phase 7: Testing & Validation

**Test Coverage:**

1. Unit tests for manifest import service
2. Unit tests for QC trending and metrics services
3. Integration tests for analyzer data adapter
4. Integration tests for result export (LMIS, REDCap)
5. E2E tests for complete workflow (receipt → reporting → archiving)
6. Role-based access control tests (verify cascading permissions)
7. Dashboard metric calculations (verify accuracy)

---

## PART 7: KEY DECISION SUMMARY

| Decision             | Selected Approach                           | Rationale                                                                                                                              |
| -------------------- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| Analyzer Integration | Extend existing AnalyzerResultImportService | Reuse proven pattern, minimize code duplication                                                                                        |
| Access Control       | Cascading (Page + Field Level)              | Maximum control granularity for compliance-heavy bioanalytical workflows                                                               |
| Dashboard Location   | Embedded in Stage 4                         | Quick reference for reporting phase; standalone module future enhancement                                                              |
| Role Authority       | 9 roles with specific permissions           | Matches organizational structure (Receivers, Analysts, Pharmacists, Supervisors, QA, Researchers, Directors, Data Mgmt, Biorepository) |

---

## PART 8: CONSISTENCY WITH EXISTING LABS

This implementation aligns with established OpenELIS patterns:

✓ **Parent/Child Template Model** - Parent template (isTemplate=true) defines
workflow, child instances store data ✓ **Page Type Taxonomy** - Uses standard
page types (BATCH_VERIFICATION, BULK_DATA_ENTRY, AGGREGATION, BRANCHING) ✓
**Role-Based Access** - Pages filtered by user role via `usePageAccessControl()`
hook ✓ **Manifest Import Service** - CSV parsing with lab-specific validation ✓
**Workflow Tab Routing** - Title-based detection in
`NoteBookInstanceEntryForm.js` ✓ **Analyzer Integration** - Extends existing
`AnalyzerResultImportService`

---

## PART 9: FILE STRUCTURE & LOCATIONS

### Backend Files (Java)

**Services:**

- `src/main/java/org/openelisglobal/notebook/service/BioanalyticalManifestImportService.java`
- `src/main/java/org/openelisglobal/notebook/service/BioanalyticalManifestImportServiceImpl.java`
- `src/main/java/org/openelisglobal/notebook/service/BioanalyticalAnalyzerDataAdapter.java`
- `src/main/java/org/openelisglobal/notebook/service/BioanalyticalAnalyzerDataAdapterImpl.java`
- `src/main/java/org/openelisglobal/notebook/service/QCTrendingService.java`
- `src/main/java/org/openelisglobal/notebook/service/ReportingMetricsService.java`
- `src/main/java/org/openelisglobal/notebook/service/AnalyticalResultExporter.java`

**Controllers:**

- `src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalManifestImportController.java`
- `src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalAnalysisController.java`
- `src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalReportingController.java`

**Forms:**

- `src/main/java/org/openelisglobal/notebook/form/BioanalyticalManifestImportForm.java`

### Database Files (Liquibase XML)

- `src/main/resources/liquibase/3.4.x.x/011-bioanalytical-workflow-pages.xml`
- `src/main/resources/liquibase/3.4.x.x/012-bioanalytical-notebook-template.xml`
- `src/main/resources/liquibase/3.4.x.x/013-bioanalytical-supporting-tables.xml`

### Frontend Files (React/JavaScript)

**Workflow Tab:**

- `frontend/src/components/notebook/workflow/BioanalyticalWorkflowTab.js`

**Manifest Import Modal:**

- `frontend/src/components/notebook/workflow/BioanalyticalManifestImportModal.js`

**Stage Page Components:**

- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalSampleReceptionPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalTestAssignmentPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalAnalyticalExecutionPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalReportingPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalStorageArchivingPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/index.js`

**Dashboard Component:**

- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalReportingDashboard.js`

---

## PART 10: CONSTANTS & ENUMERATIONS

### Sample Types (Bioanalytical Lab)

```java
public static final Set<String> BIOANALYTICAL_SAMPLE_TYPES = Set.of(
  // Biological matrices
  "plasma-edta", "plasma-heparin", "plasma-sss", "serum", "urine",
  "cerebrospinal-fluid", "saliva", "hair", "nail",

  // Pharmaceutical
  "api-powder", "api-solution", "tablet", "capsule", "suspension",
  "powder", "liquid", "cream", "gel", "patch",

  // Standards & excipients
  "reference-standard", "excipient", "degradation-product",

  // Stability samples
  "stability-sample-initial", "stability-sample-intermediate",
  "stability-sample-final", "accelerated-sample"
);
```

### Analytical Tests

```java
public static final Set<String> BIOANALYTICAL_TESTS = Set.of(
  "assay", "dissolution", "disintegration", "friability", "hardness",
  "identity-test", "purity-test", "moisture-content",
  "drug-concentration-hplc", "drug-concentration-lcmsms",
  "pharmacokinetic-analysis", "biomarker-quantification",
  "metabolite-identification", "related-substances",
  "content-uniformity"
);
```

### Analytical Methods

```java
public static final Map<String, List<String>> TEST_CATEGORY_TO_METHODS = Map.ofEntries(
  Map.entry("Bioanalytical (HPLC)", Arrays.asList(
    "HPLC UV-Vis", "HPLC Fluorescence", "HPLC RI"
  )),
  Map.entry("Bioanalytical (LC-MS/MS)", Arrays.asList(
    "LC-MS/MS Positive Ion Mode", "LC-MS/MS Negative Ion Mode",
    "LC-MS/MS SRM"
  )),
  Map.entry("Pharmaceutical Quality (Physical)", Arrays.asList(
    "Dissolution USP I", "Dissolution USP II", "Disintegration",
    "Hardness", "Friability"
  )),
  Map.entry("Pharmaceutical Quality (Chemical)", Arrays.asList(
    "Assay by HPLC", "Assay by Titration", "Identity Test UV",
    "Identity Test FTIR"
  ))
);
```

### Role Constants

```java
public static final String ROLE_SAMPLE_RECEIVER = "Sample Receivers";
public static final String ROLE_CHEMICAL_ANALYST = "Chemical Analysts";
public static final String ROLE_PHARMACIST = "Pharmacists";
public static final String ROLE_RESEARCHER = "Researchers";
public static final String ROLE_LAB_SUPERVISOR = "Lab Supervisors";
public static final String ROLE_STUDY_DIRECTOR = "Study Directors";
public static final String ROLE_QA_OFFICER = "QA Officers";
public static final String ROLE_DATA_MANAGER = "Data Managers";
public static final String ROLE_BIOREPOSITORY = "Biorepository";

public static final Set<String> ALL_BIOANALYTICAL_ROLES = Set.of(
  ROLE_SAMPLE_RECEIVER, ROLE_CHEMICAL_ANALYST, ROLE_PHARMACIST,
  ROLE_RESEARCHER, ROLE_LAB_SUPERVISOR, ROLE_STUDY_DIRECTOR,
  ROLE_QA_OFFICER, ROLE_DATA_MANAGER, ROLE_BIOREPOSITORY
);
```

---

## SUMMARY

This comprehensive plan provides:

1. ✅ **Complete 5-stage workflow** with detailed metadata for each stage
2. ✅ **9-role access control matrix** with cascading page + field-level
   permissions
3. ✅ **11-instrument analyzer integration** using existing
   AnalyzerResultImportService pattern
4. ✅ **System requirements** (sample source tracking, test method selection, BE
   support, data management, QC, export)
5. ✅ **Performance metrics & dashboards** embedded in reporting stage
6. ✅ **Implementation roadmap** across 7 phases
7. ✅ **Consistency** with existing Immunology/Pathology patterns
8. ✅ **Complete file structure** and naming conventions

**Ready for Phase 1 implementation (Database Schema).**

---

**End of Bioanalytical & Bioequivalence Laboratory Complete Implementation Plan
v2.0**
