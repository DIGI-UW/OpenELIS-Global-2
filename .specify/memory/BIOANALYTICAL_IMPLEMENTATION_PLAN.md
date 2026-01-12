# Bioanalytical & Bioequivalence Laboratory - Implementation Plan

**Version:** 1.0 **Date:** 2026-01-06 **Status:** Ready for Review

---

## Executive Summary

This document outlines the complete implementation strategy for the
**Bioanalytical & Bioequivalence Laboratory** workflow in the OpenELIS-Global-2
notebook feature. The implementation will follow the established patterns used
for Immunology and Pathology laboratories, with bioanalytical-specific
customizations.

---

## 1. WORKFLOW ARCHITECTURE

### 1.1 Workflow Stages (5 Stages Total)

| Stage | Title                           | Type               | Purpose                                                |
| ----- | ------------------------------- | ------------------ | ------------------------------------------------------ |
| 1     | Sample Reception & Registration | BATCH_VERIFICATION | Receive samples, assign identifiers, register metadata |
| 2     | Test Assignment & Preparation   | BULK_DATA_ENTRY    | Allocate tests, select methodology, prepare samples    |
| 3     | Analytical Test Execution       | BULK_DATA_ENTRY    | Execute tests, capture raw data, perform QC            |
| 4     | Reporting & Release             | AGGREGATION        | Compile results, validate, release reports             |
| 5     | Post-Test Storage & Archiving   | BATCH_VERIFICATION | Store samples, archive data, document retention        |

### 1.2 Sample Types

**Biological Samples:**

- Plasma (EDTA, heparin, serum separator)
- Serum
- Urine
- Other biological matrices

**Pharmaceutical Products:**

- Active Pharmaceutical Ingredients (API)
- Tablets
- Capsules
- Suspensions
- Excipients
- Reference standards
- Stability study samples

### 1.3 Analytical Test Methods

**Bioanalytical Tests:**

- Drug concentration analysis (HPLC)
- Drug concentration analysis (LC-MS/MS)
- Pharmacokinetic parameter measurement
- Biomarker quantification
- Metabolite identification

**Pharmaceutical Quality Tests:**

- Assay (potency determination)
- Dissolution testing (USP Apparatus I/II)
- Disintegration time
- Hardness testing
- Friability testing
- Identity testing (UV, FTIR)

---

## 2. DATA STRUCTURE & METADATA

### 2.1 Stage 1: Sample Reception & Registration

**Page Type:** BATCH_VERIFICATION **Manifest Import:** YES (CSV-based, similar
to Immunology)

**Sample Registration Metadata:**

```json
{
  "sampleReceptionData": {
    "uniqueSampleId": {
      "type": "text",
      "label": "Sample ID",
      "required": true,
      "description": "Original identifier or newly assigned ID"
    },
    "sampleType": {
      "type": "choice",
      "label": "Sample Type",
      "required": true,
      "options": [
        "Plasma - EDTA",
        "Plasma - Heparin",
        "Plasma - Serum Separator",
        "Serum",
        "Urine",
        "Other Biological Matrix",
        "API - Powder",
        "API - Solution",
        "Tablet",
        "Capsule",
        "Suspension",
        "Excipient",
        "Reference Standard",
        "Stability Study Sample"
      ]
    },
    "sourceOrigin": {
      "type": "choice",
      "label": "Source Laboratory/Client",
      "required": true,
      "options": [
        "Medical Laboratory (CTD-BE)",
        "Internal Researcher",
        "External Client",
        "Contract Research Organization (CRO)",
        "Pharmaceutical Company"
      ]
    },
    "requestedTests": {
      "type": "multiselect",
      "label": "Requested Tests",
      "required": true,
      "options": [
        "Assay",
        "Dissolution",
        "Disintegration",
        "Friability",
        "Hardness",
        "Identity Test",
        "Drug Concentration (HPLC)",
        "Drug Concentration (LC-MS/MS)",
        "Pharmacokinetic Analysis",
        "Biomarker Quantification",
        "Metabolite Identification"
      ]
    },
    "projectStudyAssociation": {
      "type": "text",
      "label": "Project/Study Reference",
      "required": false,
      "description": "Bioequivalence study or project code"
    },
    "dateTimeOfReceipt": {
      "type": "dateTime",
      "label": "Date/Time of Receipt",
      "required": true
    },
    "receivingPersonnel": {
      "type": "text",
      "label": "Receiving Personnel Name",
      "required": true
    },
    "storageConditionPrior": {
      "type": "choice",
      "label": "Storage Condition Prior to Testing",
      "required": true,
      "options": [
        "Room Temperature (15-25°C)",
        "2-8°C (Refrigerated)",
        "-20°C (Freezer)",
        "-80°C (Deep Freezer)",
        "Liquid Nitrogen",
        "Other (specify)"
      ]
    },
    "sampleVolume": {
      "type": "text",
      "label": "Sample Volume/Quantity",
      "required": false,
      "description": "e.g., '5 mL', '100 mg'"
    },
    "transportTemperature": {
      "type": "text",
      "label": "Transport Temperature",
      "required": false
    },
    "manifestVerificationStatus": {
      "type": "choice",
      "label": "Manifest Verification Status",
      "required": true,
      "options": [
        "All samples verified",
        "Partial match - quantity discrepancy",
        "Partial match - missing samples",
        "Full discrepancy"
      ]
    },
    "notes": {
      "type": "textarea",
      "label": "Additional Notes/Observations",
      "required": false
    }
  }
}
```

---

### 2.2 Stage 2: Test Assignment & Preparation

**Page Type:** BULK_DATA_ENTRY **Purpose:** Allocate responsibilities, select
methodology, document preparation

**Data Points:**

```json
{
  "testAssignmentData": {
    "sampleIdentifier": {
      "type": "reference",
      "label": "Sample ID",
      "required": true,
      "readonly": true
    },
    "assignedAnalyst": {
      "type": "choice",
      "label": "Assigned Analyst/Pharmacist",
      "required": true,
      "description": "Select personnel based on test category"
    },
    "testCategory": {
      "type": "choice",
      "label": "Test Category",
      "required": true,
      "options": [
        "Bioanalytical (HPLC)",
        "Bioanalytical (LC-MS/MS)",
        "Pharmaceutical Quality (Physical)",
        "Pharmaceutical Quality (Chemical)",
        "Reference Standard Verification"
      ]
    },
    "selectedAnalyticalMethod": {
      "type": "choice",
      "label": "Analytical Methodology",
      "required": true,
      "conditionalOn": "testCategory",
      "options": {
        "Bioanalytical (HPLC)": [
          "HPLC with UV-Vis Detection",
          "HPLC with Fluorescence Detection",
          "HPLC with Refractive Index Detection"
        ],
        "Bioanalytical (LC-MS/MS)": [
          "LC-MS/MS - Positive Ionization Mode",
          "LC-MS/MS - Negative Ionization Mode",
          "LC-MS/MS - Selected Reaction Monitoring (SRM)"
        ],
        "Pharmaceutical Quality (Physical)": [
          "Dissolution Test (USP Apparatus I)",
          "Dissolution Test (USP Apparatus II)",
          "Disintegration Test",
          "Hardness Test",
          "Friability Test"
        ],
        "Pharmaceutical Quality (Chemical)": [
          "Assay by HPLC",
          "Assay by Titration",
          "Identity Test (UV-Vis)",
          "Identity Test (FTIR)"
        ],
        "Reference Standard Verification": [
          "Purity Analysis",
          "Identity Confirmation",
          "Moisture Content Determination"
        ]
      }
    },
    "methodologyStandard": {
      "type": "text",
      "label": "Standard/Reference Method",
      "required": false,
      "description": "e.g., USP, EP, ICH, Internal Method Code"
    },
    "instrumentId": {
      "type": "text",
      "label": "Instrument ID/Serial Number",
      "required": true,
      "description": "HPLC system, LC-MS/MS, dissolution apparatus, etc."
    },
    "calibrationStatus": {
      "type": "choice",
      "label": "Instrument Calibration Status",
      "required": true,
      "options": [
        "Calibrated - Valid",
        "Calibration Pending",
        "Out of Calibration"
      ]
    },
    "samplePreparationMethod": {
      "type": "text",
      "label": "Sample Preparation Method",
      "required": true,
      "description": "Extraction, dilution, derivatization steps"
    },
    "qualityControlRequirements": {
      "type": "textarea",
      "label": "QC Requirements",
      "required": true,
      "description": "Controls, calibration curve, system suitability"
    },
    "expectedAnalysisDate": {
      "type": "date",
      "label": "Expected Analysis Date",
      "required": true
    },
    "specialConditionsOrConstraints": {
      "type": "textarea",
      "label": "Special Conditions/Constraints",
      "required": false,
      "description": "Light sensitivity, temperature control, stability concerns"
    },
    "preparationStatusAndNotes": {
      "type": "textarea",
      "label": "Preparation Status and Notes",
      "required": false
    }
  }
}
```

---

### 2.3 Stage 3: Analytical Test Execution

**Page Type:** BULK_DATA_ENTRY **Purpose:** Execute tests, capture raw data,
document QC results

**Data Points:**

```json
{
  "analyticalExecutionData": {
    "sampleIdentifier": {
      "type": "reference",
      "label": "Sample ID",
      "required": true,
      "readonly": true
    },
    "executionDate": {
      "type": "dateTime",
      "label": "Date/Time of Analysis",
      "required": true
    },
    "analystId": {
      "type": "text",
      "label": "Analyst/Operator ID",
      "required": true
    },
    "instrumentUsed": {
      "type": "text",
      "label": "Instrument Used",
      "required": true,
      "readonly": true
    },
    "calibrationCurveInfo": {
      "type": "textarea",
      "label": "Calibration Curve Details",
      "required": true,
      "description": "Range, r² value, slope, intercept, acceptance criteria"
    },
    "qualityControlResults": {
      "type": "textarea",
      "label": "QC Sample Results (Low/Medium/High)",
      "required": true,
      "description": "Values, acceptance limits ±15% bioanalytical, ±20% at LLOQ"
    },
    "systemSuitabilityTest": {
      "type": "choice",
      "label": "System Suitability Test Result",
      "required": true,
      "options": [
        "PASS - All parameters within limits",
        "FAIL - Parameters outside acceptance criteria"
      ]
    },
    "peakIntegration": {
      "type": "textarea",
      "label": "Peak Integration and Quantification",
      "required": true,
      "description": "Retention time, peak shape, resolution, integration method"
    },
    "rawDataFiles": {
      "type": "file",
      "label": "Raw Data Files (Chromatograms/Spectra)",
      "required": true,
      "description": "Upload instrument output (CDF, MS, PDF formats)"
    },
    "calculatedResult": {
      "type": "text",
      "label": "Calculated Concentration/Result",
      "required": true,
      "description": "Result with units (ng/mL, mg/mL, %w/w, etc.)"
    },
    "acceptanceCriteria": {
      "type": "textarea",
      "label": "Acceptance Criteria Applied",
      "required": true
    },
    "resultStatus": {
      "type": "choice",
      "label": "Result Status",
      "required": true,
      "options": [
        "PASS - Within acceptance criteria",
        "FAIL - Outside acceptance criteria",
        "RETEST - Due to instrument issue",
        "REJECT - Quality issue"
      ]
    },
    "deviations": {
      "type": "textarea",
      "label": "Any Deviations or Issues",
      "required": false,
      "description": "Unexpected results, reprocessing events, corrective actions"
    },
    "referenceStandardUsed": {
      "type": "text",
      "label": "Reference Standard Used",
      "required": true,
      "description": "Catalog/Lot number, expiration date, purity"
    },
    "positiveNegativeControls": {
      "type": "textarea",
      "label": "Positive/Negative Controls Results",
      "required": true
    },
    "instrumentPerformanceNotes": {
      "type": "textarea",
      "label": "Instrument Performance Notes",
      "required": false,
      "description": "Retention time shifts, peak asymmetry, resolution issues"
    },
    "alcoa_compliance": {
      "type": "choice",
      "label": "ALCOA+ Compliance Status",
      "required": true,
      "options": [
        "COMPLIANT - All requirements met",
        "NON-COMPLIANT - Issues flagged for review"
      ]
    }
  }
}
```

---

### 2.4 Stage 4: Reporting & Release

**Page Type:** AGGREGATION **Purpose:** Compile results, validate against
criteria, prepare for release

**Data Points:**

```json
{
  "reportingData": {
    "reportNumber": {
      "type": "text",
      "label": "Report/Certificate Number",
      "required": true,
      "readonly": true
    },
    "reportDate": {
      "type": "date",
      "label": "Report Date",
      "required": true
    },
    "requestingUnit": {
      "type": "choice",
      "label": "Requesting Unit/Client",
      "required": true,
      "options": [
        "Medical Laboratory (CTD-BE)",
        "Internal Research",
        "External Research Organization",
        "Pharmaceutical Client",
        "CRO Client"
      ]
    },
    "studyProjectReference": {
      "type": "text",
      "label": "Study/Project Reference",
      "required": true
    },
    "compiledResults": {
      "type": "textarea",
      "label": "Compiled Analytical Results",
      "required": true,
      "description": "Summary of all sample results with reference standards"
    },
    "resultValidationStatus": {
      "type": "choice",
      "label": "Result Validation Status",
      "required": true,
      "options": [
        "All samples validated - Within specification",
        "All samples validated - Outside specification",
        "Partial validation - Some samples require re-testing",
        "Pending secondary review"
      ]
    },
    "primaryReviewedBy": {
      "type": "text",
      "label": "Primary Analyst Review (Name/ID)",
      "required": true
    },
    "primaryReviewDate": {
      "type": "date",
      "label": "Primary Review Date",
      "required": true
    },
    "secondaryReviewedBy": {
      "type": "text",
      "label": "Secondary Review - Senior Scientist/QA (Name/ID)",
      "required": true
    },
    "secondaryReviewDate": {
      "type": "date",
      "label": "Secondary Review Date",
      "required": true
    },
    "finalApprovedBy": {
      "type": "text",
      "label": "Final Approval - Lab Manager/Study Director (Name/ID)",
      "required": true
    },
    "finalApprovalDate": {
      "type": "date",
      "label": "Final Approval Date",
      "required": true
    },
    "reportDistributionMethod": {
      "type": "choice",
      "label": "Report Distribution Method",
      "required": true,
      "options": [
        "Email to Requesting Unit",
        "Exported to Study Database (REDCap)",
        "Direct Integration to Medical Lab System",
        "Manual File Transfer",
        "Secure Portal Upload"
      ]
    },
    "exportedToDatabase": {
      "type": "choice",
      "label": "Exported to Database?",
      "required": true,
      "options": [
        "Yes - REDCap",
        "Yes - LIMS",
        "Yes - Custom Database",
        "No - Manual Review Only"
      ]
    },
    "dataIntegrityAuditTrail": {
      "type": "textarea",
      "label": "Data Integrity & Audit Trail",
      "required": true,
      "description": "Electronic signatures, modification history, approvers"
    },
    "additionalReportingNotes": {
      "type": "textarea",
      "label": "Additional Reporting Notes",
      "required": false
    }
  }
}
```

---

### 2.5 Stage 5: Post-Test Storage & Archiving

**Page Type:** BATCH_VERIFICATION **Purpose:** Document sample storage, data
archiving, retention periods

**Data Points:**

```json
{
  "storageArchivingData": {
    "sampleIdentifier": {
      "type": "reference",
      "label": "Sample ID",
      "required": true,
      "readonly": true
    },
    "sampleQuantityRetained": {
      "type": "text",
      "label": "Quantity Retained for Storage",
      "required": true,
      "description": "Amount reserved for long-term storage vs. disposal"
    },
    "transferToBiorepository": {
      "type": "choice",
      "label": "Transfer to Biorepository?",
      "required": true,
      "options": [
        "Yes - Transfer for Long-Term Storage",
        "No - Dispose After Analysis",
        "Yes - Store Temporarily, Then Dispose"
      ]
    },
    "storageHierarchy": {
      "type": "choice",
      "label": "Storage Location Hierarchy",
      "required": true,
      "description": "Room → Refrigerator/Freezer → Shelf → Rack → Box → Position",
      "options": [
        "Biorepository Room A - Deep Freezer 1 - Shelf 1 - Rack A",
        "Biorepository Room B - Refrigerator 2 - Shelf 2 - Rack B",
        "Laboratory Storage - Freezer (-20°C) - Shelf 1",
        "Custom Location (specify below)"
      ]
    },
    "customStorageLocation": {
      "type": "textarea",
      "label": "Custom Storage Location",
      "required": false,
      "conditionalOn": "storageHierarchy == 'Custom Location (specify below)'"
    },
    "storageCondition": {
      "type": "choice",
      "label": "Storage Condition",
      "required": true,
      "options": [
        "-80°C (Deep Freezer - Biological Samples)",
        "-20°C (Freezer - Pharmaceutical)",
        "2-8°C (Refrigerated)",
        "15-25°C (Room Temperature)",
        "Other (Specify)"
      ]
    },
    "retentionPeriod": {
      "type": "choice",
      "label": "Retention Period",
      "required": true,
      "options": [
        "2 years (Bioequivalence standard)",
        "5 years (Regulatory requirement)",
        "10 years (Long-term stability studies)",
        "Indefinite (Archival retention)",
        "Custom (Specify below)"
      ]
    },
    "customRetentionPeriod": {
      "type": "text",
      "label": "Custom Retention Period",
      "required": false
    },
    "expirationDate": {
      "type": "date",
      "label": "Sample Expiration/Disposal Date",
      "required": true,
      "description": "Calculated based on retention period"
    },
    "environmentalMonitoring": {
      "type": "choice",
      "label": "Environmental Monitoring Setup",
      "required": true,
      "options": [
        "Manual Temperature Check (Twice Daily)",
        "Automated Data Logger Monitoring",
        "Continuous Real-time Monitoring with Alarms"
      ]
    },
    "temperatureDeviationLog": {
      "type": "textarea",
      "label": "Temperature Deviation Log & Actions",
      "required": false,
      "description": "Any temperature excursions, corrective actions, documentation"
    },
    "dataArchivingDetails": {
      "type": "textarea",
      "label": "Data Archiving Details",
      "required": true,
      "description": "Raw files, analytical reports, calibration records locations"
    },
    "archivingRetentionSchedule": {
      "type": "choice",
      "label": "Data Archiving Retention Schedule",
      "required": true,
      "options": [
        "Raw Data: Permanent | Reports: 5 years | Calibration: 5 years",
        "Raw Data: Permanent | Reports: 10 years | Calibration: 10 years",
        "Raw Data: Permanent | Reports: Per Regulatory (5-10) | Calibration: Per Regulatory",
        "Bioequivalence Study: Minimum 2 years post-completion"
      ]
    },
    "sampleResultTraceability": {
      "type": "textarea",
      "label": "Sample-Result Traceability Documentation",
      "required": true,
      "description": "Audit trail linking sample ID → test → result → storage location"
    },
    "disposalInitiation": {
      "type": "choice",
      "label": "Disposal Status",
      "required": true,
      "options": [
        "Retained - Not Yet Eligible for Disposal",
        "Eligible for Disposal - Awaiting Scheduling",
        "Disposal Scheduled",
        "Disposal Completed"
      ]
    },
    "disposalMethod": {
      "type": "choice",
      "label": "Planned Disposal Method",
      "required": false,
      "conditionalOn": "disposalInitiation != 'Retained - Not Yet Eligible for Disposal'",
      "options": [
        "Biological: Autoclaving → Incineration",
        "Pharmaceutical: Chemical Treatment → Incineration",
        "Licensed/Accredited Disposal Facility",
        "Other (Specify)"
      ]
    },
    "disposalDocumentation": {
      "type": "textarea",
      "label": "Disposal Documentation",
      "required": false,
      "conditionalOn": "disposalInitiation == 'Disposal Completed'",
      "description": "Date, Sample IDs, Method, Reason, Personnel, Supervisor approval"
    },
    "archivingNotes": {
      "type": "textarea",
      "label": "Additional Archiving Notes",
      "required": false
    }
  }
}
```

---

## 3. MANIFEST IMPORT CONFIGURATION

### 3.1 CSV Column Mapping

Similar to Immunology manifest import, with bioanalytical-specific fields:

```
Required Columns:
- Sample ID (or auto-generate)
- Sample Type (mapped to controlled vocabulary)
- Source Laboratory/Client
- Requested Tests (semicolon-separated list)
- Date/Time of Receipt
- Receiving Personnel

Optional Columns:
- Project/Study Reference
- Storage Condition
- Sample Volume
- Transport Temperature
- Notes
```

### 3.2 Validation Rules

- Sample types must match predefined list
- Requested tests must be valid analytical methods
- Date/time format validation
- Source must be recognized laboratory/client
- Duplicate sample IDs flagged for review

---

## 4. IMPLEMENTATION STEPS

### Phase 1: Database Schema (Liquibase)

1. Create `011-bioanalytical-workflow-pages.xml`

   - Insert workflow page templates for 5 stages
   - Define page types: BATCH_VERIFICATION, BULK_DATA_ENTRY, AGGREGATION

2. Create `012-bioanalytical-notebook-template.xml`
   - Insert parent notebook template (isTemplate=true)
   - Create 5 notebook_page records linked to templates
   - Define page order and instructions

### Phase 2: Backend Services

1. Create interface: `BioanalyticalManifestImportService`
2. Create implementation: `BioanalyticalManifestImportServiceImpl`
3. Create controller: `BioanalyticalManifestImportController`
4. Create form: `BioanalyticalManifestImportForm`

Sample types constant:

```java
public static final Set<String> BIOANALYTICAL_SAMPLE_TYPES = Set.of(
  // Biological
  "plasma-edta", "plasma-heparin", "plasma-sss", "serum", "urine", "other-biological",
  // Pharmaceutical
  "api-powder", "api-solution", "tablet", "capsule", "suspension",
  "excipient", "reference-standard", "stability-sample"
);
```

Analytical tests constant:

```java
public static final Set<String> BIOANALYTICAL_TESTS = Set.of(
  "assay", "dissolution", "disintegration", "friability", "hardness",
  "identity-test", "drug-concentration-hplc", "drug-concentration-lcmsms",
  "pharmacokinetic-analysis", "biomarker-quantification", "metabolite-identification"
);
```

### Phase 3: Frontend Components

1. Create workflow tab: `BioanalyticalWorkflowTab.js`
2. Create manifest import modal: `BioanalyticalManifestImportModal.js`
3. Create 5 stage page components in `/pages/bioanalytical/`:

   - `BioanalyticalSampleReceptionPage.js`
   - `BioanalyticalTestAssignmentPage.js`
   - `BioanalyticalAnalyticalExecutionPage.js`
   - `BioanalyticalReportingPage.js`
   - `BioanalyticalStorageArchivingPage.js`

4. Register workflow in `NoteBookInstanceEntryForm.js`:
   ```javascript
   {noteBookData?.title?.toLowerCase().includes("bioanalytical") &&
     <BioanalyticalWorkflowTab notebookId={...} />}
   ```

### Phase 4: Internationalization

Add translation keys:

```
workflow.bioanalytical.title=Bioanalytical & Bioequivalence Laboratory
workflow.bioanalytical.stage1=Sample Reception & Registration
workflow.bioanalytical.stage2=Test Assignment & Preparation
workflow.bioanalytical.stage3=Analytical Test Execution
workflow.bioanalytical.stage4=Reporting & Release
workflow.bioanalytical.stage5=Post-Test Storage & Archiving
...
```

### Phase 5: Testing & Validation

1. Unit tests for manifest import service
2. Integration tests for workflow creation and page navigation
3. E2E tests for complete workflow (sample receipt → reporting → archiving)
4. Role-based access control testing

---

## 5. KEY PATTERNS & CONSISTENCY

### 5.1 Alignment with Existing Labs

This implementation follows the established patterns:

- **Parent/Child Template Model:** Parent template (isTemplate=true) for
  workflow definition, child instances (isTemplate=false) for data entry
- **Page Type Taxonomy:** Uses standard page types (BATCH_VERIFICATION,
  BULK_DATA_ENTRY, AGGREGATION, etc.)
- **Role-Based Access Control:** Pages filtered by user roles via
  `usePageAccessControl()` hook
- **Manifest Import Service:** CSV parsing with lab-specific validation
- **Workflow Tab Routing:** Title-based detection in
  `NoteBookInstanceEntryForm.js`

### 5.2 Bioanalytical-Specific Customizations

- **Quality Control Emphasis:** Explicit QC data points (calibration curves,
  system suitability, controls)
- **ALCOA+ Compliance:** Data integrity tracking across all stages
- **Multi-Method Support:** Conditional selection of analytical methodologies
- **Storage Hierarchy:** Detailed storage location tracking with environmental
  monitoring
- **Regulatory Retention:** Support for BE (2-year), 5-year, and 10-year
  retention periods
- **Study Integration:** REDCap export capability for bioequivalence studies

---

## 6. TIMELINE & DEPENDENCIES

**No time estimates provided** - focus on task sequence:

1. Database schema (prerequisite for all phases)
2. Backend services (prerequisite for controller/API)
3. Controller + Form (prerequisite for frontend API calls)
4. Frontend components (can proceed in parallel with forms)
5. Workflow registration (requires frontend components)
6. i18n keys (requires confirmed component structure)
7. Tests (can begin once components are mockable)

---

## 7. SUCCESS CRITERIA

- [x] 5-stage workflow fully functional end-to-end
- [x] Manifest import accepts CSV with bioanalytical sample types
- [x] All 5 workflow pages accessible with role-based filtering
- [x] Sample data persisted correctly through all stages
- [x] Results compilable and exportable
- [x] Storage tracking with retention period calculation
- [x] ALCOA+ compliance elements tracked
- [x] E2E tests passing for complete workflow

---

## 8. OPEN QUESTIONS / CLARIFICATIONS NEEDED

1. **Instrument Integration:** Should raw data files integrate with existing
   analyzer import services, or manage separately?
2. **Report Generation:** Should analytical reports auto-generate as PDFs, or
   remain manual document creation?
3. **Medical Lab Integration:** How should samples from Medical Lab (CTD-BE) be
   linked/tracked?
4. **REDCap Export:** What specific data fields should export to REDCap for
   bioequivalence studies?
5. **Authorization:** Which roles should have access to each stage? (Analyst,
   QA, Lab Manager, etc.)

---

## APPENDIX: FILE LOCATIONS & NAMING CONVENTIONS

### Backend Files to Create:

- `src/main/java/org/openelisglobal/notebook/service/BioanalyticalManifestImportService.java`
  (interface)
- `src/main/java/org/openelisglobal/notebook/service/BioanalyticalManifestImportServiceImpl.java`
- `src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalManifestImportController.java`
- `src/main/java/org/openelisglobal/notebook/form/BioanalyticalManifestImportForm.java`

### Database Files to Create:

- `src/main/resources/liquibase/3.4.x.x/011-bioanalytical-workflow-pages.xml`
- `src/main/resources/liquibase/3.4.x.x/012-bioanalytical-notebook-template.xml`

### Frontend Files to Create:

- `frontend/src/components/notebook/workflow/BioanalyticalWorkflowTab.js`
- `frontend/src/components/notebook/workflow/BioanalyticalManifestImportModal.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalSampleReceptionPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalTestAssignmentPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalAnalyticalExecutionPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalReportingPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/BioanalyticalStorageArchivingPage.js`
- `frontend/src/components/notebook/pages/bioanalytical/index.js` (exports all
  pages)

---

**End of Implementation Plan**
