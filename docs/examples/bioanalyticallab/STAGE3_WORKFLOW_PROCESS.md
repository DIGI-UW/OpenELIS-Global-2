# Stage 3 (Analytical Execution) - Complete Workflow Process

## Overview

Stage 3 is divided into **10 sequential tabs**, each representing a step in the analytical execution workflow. The user must complete these steps in order to fully execute and document analytical tests according to FDA bioanalytical compliance standards (21 CFR Part 11, ALCOA+).

---

## Complete Stage 3 Workflow (Tab-by-Tab)

### **Tab 1: Test Execution** ✅ (Starting Point)

**What Happens When You Execute Tests:**

When you click "Execute Tests (N)" in the modal:

1. **Modal Closes** - The configuration modal disappears
2. **Test Execution Data Saved** - Backend stores:
   - `executionStatus: "EXECUTED"`
   - Analyst ID, Instrument ID, Batch Number
   - Execution Date
   - Execution Notes
   - Audit trail entry created
3. **Auto-Navigate to Tab 2** - The UI automatically moves to the next tab
4. **Success Notification** - Confirmation message: "Tests executed successfully for N samples"
5. **Sample Status Updates** - Selected samples now show `EXECUTED` status

**After Execution in Tab 1:**
- You CAN NOT modify execution configuration
- You MUST proceed to Tab 2 to upload raw instrument data

---

### **Tab 2: Raw Data Upload** 📤 (Next Step)

**Purpose:** Capture actual chromatograms, spectra, and raw analytical data from instruments

**Activities:**
1. **Select Instrument Type** - Choose which analyzer/instrument produced the data
2. **Upload Raw Data Files** - Add files in supported formats:
   - **LC-MS/MS**: mzML, CDF
   - **HPLC**: CSV, PDF
   - **Dissolution**: CSV
   - **Other**: PDF

3. **File Validation** - System validates file format matches selected instrument
4. **Upload Records** - Each file is logged with:
   - File name
   - Instrument ID
   - Upload timestamp
   - File type
   - Validation status

**What Gets Stored:**
```json
{
  "rawDataFiles": [
    {
      "fileName": "LC-MS-MS_Sample_BIO-2024-001.csv",
      "instrumentId": "1",
      "fileType": "CSV",
      "uploadedAt": "2026-01-07T10:30:00Z",
      "status": "PENDING_VALIDATION"
    }
  ]
}
```

**Audit Trail Entry**: `FILE_UPLOAD` with file size, type, and validation status

---

### **Tab 3: Calibration & QC** ⚖️ (Data Analysis)

**Purpose:** Record calibration curve validation and QC sample results

**Activities:**

1. **Calibration Curve Validation**
   - Record R² (coefficient of determination)
   - Minimum acceptable: 0.99 (or from Stage 2 config)
   - Record Slope (linear regression)
   - Record Intercept (y-axis offset)
   - Validate all parameters meet acceptance criteria

2. **QC Sample Results** - For each QC level (Low, Medium, High):
   - Measured Concentration
   - % Accuracy: `(Measured / Expected) × 100`
   - % Coefficient of Variation (CV): `(Std Dev / Mean) × 100`
   - Pass/Fail against acceptance criteria

3. **Westgard Rules Check** - System automatically detects:
   - 1-2s rule violations (2 consecutive results >2SD)
   - 1-3s rule violations (1 result >3SD)
   - R-4s rule violations (range >4SD)
   - 4-1s rule violations (4 consecutive >1SD same side)
   - 10x rule violations (10 consecutive same side of mean)

**Example Acceptance Criteria** (from Stage 2):
```json
{
  "rSquaredMin": 0.995,
  "slopeRange": { "min": 0.8, "max": 1.2 },
  "interceptMax": 20,
  "qcAccuracy": { "min": 85, "max": 115 },
  "qcPrecision": 15  // ≤15% CV
}
```

**What Gets Stored:**
```json
{
  "calibrationData": {
    "rSquared": 0.9987,
    "slope": 8945.23,
    "intercept": 5234.12,
    "acceptancePassed": true
  },
  "qcResults": [
    {
      "level": "LOW",
      "concentration": 5,
      "measured": 4.95,
      "accuracy": 99.0,
      "cv": 1.5,
      "passed": true
    },
    {
      "level": "MEDIUM",
      "concentration": 50,
      "measured": 50.2,
      "accuracy": 100.4,
      "cv": 1.2,
      "passed": true
    },
    {
      "level": "HIGH",
      "concentration": 500,
      "measured": 505,
      "accuracy": 101.0,
      "cv": 1.3,
      "passed": true
    }
  ],
  "westgardViolations": []
}
```

---

### **Tab 4: Automated Processing** 🤖 (Data Integration)

**Purpose:** Automatic calculation of sample concentrations using validated calibration

**Activities:**

1. **Peak Integration** - System processes chromatogram data:
   - Identifies peaks in raw data
   - Calculates peak areas/heights
   - Matches retention times

2. **Calibration Point Mapping** - Applies validated calibration curve:
   - Maps measured peak area to concentration
   - Uses linear regression equation: `Concentration = (PeakArea - Intercept) / Slope`

3. **Sample Concentration Calculation**
   - Calculates concentration for each sample
   - Applies dilution factors (if applicable)
   - Validates against calibration range

4. **Automatic QC Evaluation**
   - Compares sample results to QC recovery limits
   - Flags out-of-range results for manual review

**What Gets Stored:**
```json
{
  "automatedResults": [
    {
      "sampleId": "BIO-2024-001",
      "peakArea": 875000,
      "calculatedConcentration": 247.5,
      "unit": "ng/mL",
      "withinRange": true,
      "qcRecovery": 98.5
    }
  ],
  "processingStatus": "COMPLETED",
  "processedAt": "2026-01-07T10:45:00Z"
}
```

---

### **Tab 5: Results & Approval** 📊 (Final Sample Data)

**Purpose:** Display calculated sample results and manage approvals

**Activities:**

1. **View Sample Results**
   - Sample ID
   - Calculated Concentration
   - Units (ng/mL, %, etc.)
   - Pass/Fail status
   - % Recovery vs. QC

2. **Results Validation**
   - Check all samples are within expected ranges
   - Identify any outliers or suspicious results
   - Flag samples requiring additional testing

3. **Record Approval Status**
   - Analyst comment (if needed)
   - Initial approval by executing analyst
   - Mark samples as "RESULTS_APPROVED"

**What Gets Stored:**
```json
{
  "sampleResults": [
    {
      "sampleId": "BIO-2024-001",
      "concentration": 247.5,
      "unit": "ng/mL",
      "recoveryPercent": 98.5,
      "status": "PASSED",
      "approvedBy": "analyst_001",
      "approvedAt": "2026-01-07T11:00:00Z"
    }
  ]
}
```

---

### **Tab 6: Deviations** ⚠️ (Exceptions & Corrections)

**Purpose:** Document any analytical failures or method deviations

**Activities:**

1. **Record Deviations** - If QC failed or results were unexpected:
   - **Type**: QC Failure, Out-of-Spec Result, Instrument Malfunction, Method Deviation
   - **Severity**: Critical, Major, Minor
   - **Description**: What went wrong and why
   - **Corrective Action**: How to fix it
   - **Preventive Action**: How to prevent future occurrence

2. **Deviation Example**:
   ```
   Type: QC Failure
   Severity: Major
   Description: High QC level showed -15% accuracy (415 ng/mL vs. 500 expected)
   Root Cause: Instrument column degradation
   Corrective Action: Replace analytical column, re-run calibration and QC
   Preventive Action: Implement monthly column performance check
   Reported By: analyst_001
   Status: OPEN (pending resolution)
   ```

3. **Impact Assessment**
   - Does deviation invalidate all results?
   - Which samples are affected?
   - Can affected samples be re-analyzed?

**What Gets Stored:**
```json
{
  "deviations": [
    {
      "id": "DEV-001",
      "type": "QC_FAILURE",
      "severity": "MAJOR",
      "description": "High QC accuracy outside limits",
      "correctiveAction": "Column replacement and recalibration",
      "preventiveAction": "Monthly column performance audit",
      "recordedBy": "analyst_001",
      "recordedAt": "2026-01-07T11:15:00Z",
      "status": "OPEN"
    }
  ]
}
```

---

### **Tab 7: Analyst Review (ALCOA+)** 👤 (First Level Review)

**Purpose:** Analyst validates data integrity and method compliance (Level 1 Review)

**Activities:**

1. **Contemporaneous Record Check**
   - Were all results recorded in real-time?
   - Are there any gaps or missing data points?
   - Verify timestamps are in chronological order

2. **Data Integrity Verification**
   - Check for alterations or corrections
   - Verify all original data is preserved
   - Confirm audit trail is complete

3. **Method Compliance**
   - Did analysis follow validated method from Stage 2?
   - Were all instrument parameters within specs?
   - Were QC samples analyzed correctly?

4. **ALCOA+ Elements**
   - **Attributable**: Who performed the analysis? (Analyst ID recorded)
   - **Legible**: All records clear and readable? ✓
   - **Contemporaneous**: Real-time recording? ✓
   - **Original**: No alterations? ✓
   - **Accurate**: Calculations verified? ✓
   - **Complete**: All required data present? ✓
   - **Plus**: Signatures, timestamps, audit trail? ✓

5. **Analyst Signs Off**
   - Analyst Name
   - Electronic Signature (timestamp)
   - Comments or Observations
   - Approval Checkbox

**What Gets Stored:**
```json
{
  "analystReview": {
    "reviewerId": "analyst_001",
    "reviewerName": "John Doe",
    "reviewDate": "2026-01-07T12:00:00Z",
    "comments": "All data valid. Calibration R² 0.9987, all QC passed. Method followed per validated SOP.",
    "approved": true,
    "electronicSignature": "john.doe@lab.com@2026-01-07T12:00:00Z",
    "alcoa": {
      "attributable": true,
      "legible": true,
      "contemporaneous": true,
      "original": true,
      "accurate": true,
      "complete": true,
      "plus": true
    }
  },
  "reviewStatus": "ANALYST_APPROVED"
}
```

**IMPORTANT**: After Analyst Review is APPROVED:
- Tab 8 (QA Review) becomes ENABLED ✓
- User can proceed to multi-level review

---

### **Tab 8: QA Review (Level 2)** 🔍 (Second Level Review)

**Purpose:** Quality Assurance specialist validates methodology and regulatory compliance

**Enabled Only After:** Analyst Review is APPROVED

**Activities:**

1. **Method Validation Check**
   - Is the analytical method validated per USP/USP guidelines?
   - Were all acceptance criteria met?
   - Are QC results within established ranges?

2. **Data Integrity Audit**
   - Verify no unauthorized modifications
   - Check audit trail for completeness
   - Confirm original data preservation

3. **Regulatory Compliance**
   - 21 CFR Part 11 compliance? ✓
   - System suitability test results acceptable?
   - Reference standard properly verified?

4. **QA Approval Elements**
   - Data Integrity Verified: ✓
   - Method Compliance Verified: ✓
   - QC Acceptable: ✓
   - Comments/Observations
   - Electronic Signature

**What Gets Stored:**
```json
{
  "qaReview": {
    "reviewerId": "qa_002",
    "reviewerName": "Jane Smith",
    "reviewDate": "2026-01-07T13:30:00Z",
    "comments": "Method properly validated. All acceptance criteria met. R² 0.9987 exceeds requirement of 0.99. QC recovery 98-101%, excellent precision.",
    "approved": true,
    "electronicSignature": "jane.smith@lab.com@2026-01-07T13:30:00Z",
    "dataIntegrityVerified": true,
    "methodComplianceVerified": true,
    "qcAcceptable": true
  },
  "reviewStatus": "QA_APPROVED"
}
```

**IMPORTANT**: After QA Review is APPROVED:
- Tab 9 (Manager Approval) becomes ENABLED ✓
- Final approval step unlocked

---

### **Tab 9: Manager Approval (Final)** 👔 (Final Authorization)

**Purpose:** Study Director/Manager gives final regulatory approval

**Enabled Only After:** QA Review is APPROVED

**Activities:**

1. **Overall Compliance Assessment**
   - Are all requirements met?
   - Is the study data suitable for regulatory submission?
   - Does data support the study conclusions?

2. **Regulatory Submission Check**
   - Can this report be submitted to FDA?
   - Does it meet GLP/cGLP standards?
   - Is documentation complete?

3. **Final Disposition Decision**
   - **APPROVED**: Release for regulatory submission
   - **APPROVED_WITH_CONDITIONS**: Minor issues noted but acceptable
   - **REJECTED**: Requires re-analysis or investigation

4. **Manager Signs Off**
   - Study Director Name
   - Electronic Signature
   - Final Comments
   - Study Impact Assessment

**What Gets Stored:**
```json
{
  "managerReview": {
    "reviewerId": "manager_001",
    "reviewerName": "Dr. Robert Johnson",
    "reviewDate": "2026-01-07T14:00:00Z",
    "comments": "Excellent analytical work. All requirements met. Ready for FDA submission. Bioequivalence study demonstrates equivalent exposure profiles.",
    "approved": true,
    "electronicSignature": "robert.johnson@lab.com@2026-01-07T14:00:00Z",
    "regulatoryCompliance": true,
    "studyImpact": "Study supports bioequivalence claim",
    "finalDisposition": "APPROVED_FOR_SUBMISSION"
  },
  "reviewStatus": "MANAGER_APPROVED",
  "executionStatus": "COMPLETED"
}
```

---

### **Tab 10: Audit Trail & Data Integrity** 📋 (Compliance Record)

**Purpose:** Show complete audit trail and data integrity verification for regulatory compliance

**Activities:**

1. **View Audit Trail Log**
   - All actions recorded chronologically
   - Who made changes and when
   - What data was modified

2. **Data Integrity Summary**
   - Checksum verification: ✓
   - Timestamp verification: ✓
   - Originality verification: ✓
   - User attribution: Complete ✓

3. **System Events**
   - File uploads: 1 LC-MS/MS chromatogram
   - Data modifications: 0 alterations
   - Reviews completed: 3 (Analyst, QA, Manager)
   - Approvals recorded: 3

**Audit Trail Entries**:
```
10:30:00 - TEST_EXECUTION - analyst_001 - Executed tests for 3 samples
10:35:00 - FILE_UPLOAD - analyst_001 - Uploaded LC-MS-MS_Sample_BIO-2024-001.csv (245 KB)
10:45:00 - CALIBRATION_RECORDED - analyst_001 - Calibration R² 0.9987
11:00:00 - QC_RESULTS_RECORDED - analyst_001 - All QC passed
11:15:00 - SAMPLE_RESULTS_CALCULATED - System - 3 samples processed
12:00:00 - ANALYST_REVIEW_APPROVED - analyst_001 - John Doe approved
13:30:00 - QA_REVIEW_APPROVED - qa_002 - Jane Smith approved
14:00:00 - MANAGER_APPROVED - manager_001 - Dr. Johnson approved
```

---

## Complete Workflow Summary

```
Tab 1: Test Execution
   ↓ (Click "Execute Tests") → Saves execution config & moves to Tab 2
Tab 2: Raw Data Upload
   ↓ (Upload chromatograms)
Tab 3: Calibration & QC
   ↓ (Record curve & QC results)
Tab 4: Automated Processing
   ↓ (System calculates concentrations)
Tab 5: Results & Approval
   ↓ (Review sample results)
Tab 6: Deviations
   ↓ (Document any issues)
Tab 7: Analyst Review (ALCOA+)
   ↓ (Analyst approves) → ANALYST_APPROVED ✓
Tab 8: QA Review (Level 2) [ENABLED ONLY AFTER ANALYST APPROVAL]
   ↓ (QA approves) → QA_APPROVED ✓
Tab 9: Manager Approval (Final) [ENABLED ONLY AFTER QA APPROVAL]
   ↓ (Manager approves) → MANAGER_APPROVED ✓
Tab 10: Audit Trail & Data Integrity
   ↓
COMPLETE! Ready for regulatory submission.
```

---

## What Your Frontend Currently Has

✅ **Tab 1: Test Execution** - Modal implementation COMPLETE
   - Selects samples (checkboxes)
   - Opens configuration modal
   - Saves execution data
   - Auto-navigates to Tab 2

⏳ **Tabs 2-10** - Structure exists but content needs implementation:
   - Tab 2: Raw Data Upload - FileUploader component
   - Tab 3: Calibration & QC - Form fields need data binding
   - Tab 4: Automated Processing - Logic needs implementation
   - Tab 5: Results & Approval - Display & signing
   - Tab 6: Deviations - Form for recording issues
   - Tab 7-9: Review tabs - Review forms with approval flows
   - Tab 10: Audit Trail - Log display

---

## Next Steps for Your Implementation

1. **Tab 2: Raw Data Upload**
   - Bind FileUploader to state
   - Implement file validation
   - Save uploaded files to backend

2. **Tab 3: Calibration & QC**
   - Add form inputs for R², slope, intercept
   - Add QC result form (Low, Medium, High)
   - Implement Westgard rule detection
   - Show pass/fail against acceptance criteria

3. **Tabs 4-10**
   - Implement calculated results display
   - Add deviation recording
   - Build review forms (Analyst, QA, Manager)
   - Show audit trail with timestamps

---

## Key Points to Remember

- **Tab 1 → Tab 2 Automatic**: When you click "Execute Tests", execution status is saved and UI moves to Tab 2 automatically
- **Progressive Disclosure**: Tabs 8-9 are disabled until previous reviews are approved
- **ALCOA+ Compliance**: All steps record who, when, and what
- **Audit Trail**: Every action is logged with timestamp and user attribution
- **Regulatory Ready**: After Manager Approval (Tab 9), data is ready for FDA submission

