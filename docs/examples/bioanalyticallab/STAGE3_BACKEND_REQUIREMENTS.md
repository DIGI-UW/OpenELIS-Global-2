# Stage 3 (Analytical Execution) - Backend Requirements & Audit

## Overview

This document outlines all backend requirements for Stage 3 of the bioanalytical notebook workflow, including API endpoints, data persistence requirements, error handling, and validation rules.

---

## Frontend to Backend Data Flow

### **Tab 1: Test Execution Configuration**

#### **API Endpoint: POST /rest/notebook/bulk/page/{pageId}/samples/apply**

**Frontend sends:**
```json
{
  "sampleIds": [62, 63, 64],
  "data": {
    "executionStatus": "EXECUTED",
    "testExecution": {
      "analystId": "john.doe",
      "instrumentId": "1",
      "batchNumber": "Batch-001",
      "executionDate": "2026-01-07",
      "testParameters": {},
      "notes": "Standard analysis per SOP"
    },
    "rawDataFiles": [],
    "executedAt": "2026-01-07T10:30:00Z",
    "executedBy": "john.doe"
  },
  "userId": "john.doe"
}
```

**Backend should:**
1. ✅ Validate all required fields are present
2. ✅ Persist data to `NotebookPageSample.data` JSONB field
3. ✅ Update sample status to "EXECUTED"
4. ✅ Create audit trail entry
5. ✅ Return success response with updated sample data

**Current Implementation Status:** ✅ **Exists** (bulk/page/{pageId}/samples/apply endpoint)

---

## Complete Backend Requirements Matrix

### **Requirement 1: Test Execution Persistence**

**What:** Store execution configuration when user fills and submits Test Execution modal

**Endpoint:** POST `/rest/notebook/bulk/page/{pageId}/samples/apply`

**Request Body:**
```json
{
  "sampleIds": [62, 63],
  "data": {
    "executionStatus": "EXECUTED",
    "testExecution": {
      "analystId": "john.doe",
      "instrumentId": "1",
      "batchNumber": "Batch-001",
      "executionDate": "2026-01-07",
      "notes": "..."
    },
    "executedAt": "2026-01-07T10:30:00Z",
    "executedBy": "john.doe"
  }
}
```

**Requirements:**
- ✅ Persist to sample.data JSONB
- ✅ Maintain existing data from Stage 2
- ✅ Merge new execution data with existing data
- ✅ Update timestamp
- ✅ Return merged data in response

**Validation Rules:**
- ✅ `analystId` must be non-empty
- ✅ `instrumentId` must match valid analyzer (1-11)
- ✅ `executionDate` must be valid ISO date or today's date
- ✅ `batchNumber` optional
- ✅ `notes` optional

---

### **Requirement 2: Raw Data File Upload & Persistence**

**What:** Upload chromatogram/spectrum data files for analysis

**Tab:** Tab 2: Raw Data Upload

**Current Implementation Status:** ⚠️ **Needs Verification**

**Frontend sends:** FileUploader with file selection

**Backend should:**
1. Accept file uploads (multipart/form-data)
2. Validate file format matches analyzer:
   - LC-MS/MS: mzML, CDF
   - HPLC: CSV, PDF
   - Dissolution: CSV
   - etc.
3. Store file metadata:
   ```json
   {
     "fileName": "LC-MS-MS_Sample_BIO-2024-001.csv",
     "fileType": "CSV",
     "instrumentId": "1",
     "uploadedAt": "2026-01-07T10:45:00Z",
     "fileSize": 15240,
     "status": "PENDING_VALIDATION"
   }
   ```
4. Persist to sample.data.rawDataFiles array
5. Return file metadata with upload confirmation

**Missing Endpoints to Check:**
- [ ] POST `/rest/notebook/page/{pageId}/samples/{sampleId}/upload-file`
- [ ] POST `/rest/notebook/bulk/page/{pageId}/upload-file`

---

### **Requirement 3: Calibration & QC Data Recording**

**What:** Store calibration curve validation and QC sample results

**Tab:** Tab 3: Calibration & QC

**Current Implementation Status:** ⚠️ **Needs Verification**

**Data Structure:**
```json
{
  "calibrationData": {
    "rSquared": 0.9987,
    "slope": 8945.23,
    "intercept": 5234.12,
    "acceptancePassed": true,
    "notes": "Excellent linearity"
  },
  "qcResults": [
    {
      "level": "LOW",
      "expectedConcentration": 5,
      "measuredConcentration": 4.95,
      "accuracy": 99.0,
      "cv": 1.5,
      "passed": true
    },
    {
      "level": "MEDIUM",
      "expectedConcentration": 50,
      "measuredConcentration": 50.2,
      "accuracy": 100.4,
      "cv": 1.2,
      "passed": true
    },
    {
      "level": "HIGH",
      "expectedConcentration": 500,
      "measuredConcentration": 505,
      "accuracy": 101.0,
      "cv": 1.3,
      "passed": true
    }
  ],
  "westgardViolations": []
}
```

**Validation Rules:**
- R² minimum (from Stage 2 acceptance criteria, typically ≥0.99)
- QC accuracy: 80-120% (or from Stage 2)
- QC precision: ≤15% CV (or from Stage 2)
- Westgard rule detection (1-2s, 1-3s, R-4s, 4-1s, 10x)

**Required Endpoint:**
- [ ] POST `/rest/notebook/page/{pageId}/samples/{sampleId}/calibration-qc`

---

### **Requirement 4: Sample Results Calculation**

**What:** Display calculated sample concentrations after data processing

**Tab:** Tab 4: Automated Processing & Tab 5: Results & Approval

**Current Implementation Status:** ⚠️ **Needs Verification**

**Data Structure:**
```json
{
  "sampleResults": [
    {
      "sampleId": "BIO-2024-001",
      "peakArea": 875000,
      "calculatedConcentration": 247.5,
      "unit": "ng/mL",
      "dilutionFactor": 1,
      "recoveryPercent": 98.5,
      "status": "PASSED",
      "qcRecovery": "ACCEPTABLE",
      "notes": "Within QC limits"
    }
  ]
}
```

**Calculation Logic:**
- `calculatedConcentration = (peakArea - calibrationIntercept) / calibrationSlope`
- `recoveryPercent = (calculatedConcentration / expectedConcentration) × 100`
- `status = "PASSED"` if within acceptance criteria, else "FAILED"

**Required Endpoint:**
- [ ] GET `/rest/notebook/page/{pageId}/samples/{sampleId}/calculate-results`

---

### **Requirement 5: Deviation Recording**

**What:** Document analytical failures, QC issues, and method deviations

**Tab:** Tab 6: Deviations

**Current Implementation Status:** ⚠️ **Partially Implemented** (see handleRecordDeviation function)

**Data Structure:**
```json
{
  "deviations": [
    {
      "id": "DEV-001",
      "type": "QC_FAILURE",
      "severity": "MAJOR",
      "description": "High QC level outside acceptance limits",
      "rootCause": "Instrument column degradation",
      "correctiveAction": "Replace column and re-validate",
      "preventiveAction": "Monthly column maintenance",
      "recordedBy": "john.doe",
      "recordedAt": "2026-01-07T11:15:00Z",
      "status": "OPEN",
      "resolution": null
    }
  ]
}
```

**Valid Deviation Types:**
- QC_FAILURE
- OUT_OF_SPEC_RESULT
- INSTRUMENT_MALFUNCTION
- METHOD_DEVIATION
- DATA_INTEGRITY_ISSUE

**Valid Severities:**
- CRITICAL (blocks all results)
- MAJOR (affects specific samples)
- MINOR (documentation only)

**Required Endpoint:**
- ✅ POST `/rest/notebook/bulk/page/{pageId}/samples/apply` (append to deviations array)

---

### **Requirement 6: Multi-Level Review & Approval**

**What:** Three-level approval workflow (Analyst → QA → Manager)

**Tabs:** Tab 7-9: Review & Approval

**Current Implementation Status:** ⚠️ **Structure Exists, Logic Needs Verification**

**Tab 7: Analyst Review (Level 1)**
```json
{
  "analystReview": {
    "reviewerId": "analyst_001",
    "reviewerName": "John Doe",
    "reviewDate": "2026-01-07T12:00:00Z",
    "comments": "All data valid, QC passed, method followed",
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

**Tab 8: QA Review (Level 2)** - Only enabled if ANALYST_APPROVED
```json
{
  "qaReview": {
    "reviewerId": "qa_002",
    "reviewerName": "Jane Smith",
    "reviewDate": "2026-01-07T13:30:00Z",
    "comments": "Method validated, acceptance criteria met",
    "approved": true,
    "electronicSignature": "jane.smith@lab.com@2026-01-07T13:30:00Z",
    "dataIntegrityVerified": true,
    "methodComplianceVerified": true,
    "qcAcceptable": true
  },
  "reviewStatus": "QA_APPROVED"
}
```

**Tab 9: Manager Approval (Final)** - Only enabled if QA_APPROVED
```json
{
  "managerReview": {
    "reviewerId": "manager_001",
    "reviewerName": "Dr. Robert Johnson",
    "reviewDate": "2026-01-07T14:00:00Z",
    "comments": "Excellent analysis, ready for submission",
    "approved": true,
    "electronicSignature": "robert.johnson@lab.com@2026-01-07T14:00:00Z",
    "regulatoryCompliance": true,
    "studyImpact": "Supports bioequivalence claim",
    "finalDisposition": "APPROVED_FOR_SUBMISSION"
  },
  "reviewStatus": "MANAGER_APPROVED",
  "executionStatus": "COMPLETED"
}
```

**Review State Transitions:**
- PENDING_ANALYST_REVIEW → ANALYST_APPROVED → QA_APPROVED → MANAGER_APPROVED → COMPLETED

**Required Endpoint:**
- [ ] POST `/rest/notebook/page/{pageId}/samples/{sampleId}/review`

---

### **Requirement 7: Audit Trail & Data Integrity**

**What:** Complete audit log for regulatory compliance (21 CFR Part 11, ALCOA+)

**Tab:** Tab 10: Audit Trail & Data Integrity

**Data Structure:**
```json
{
  "auditTrail": [
    {
      "timestamp": "2026-01-07T10:30:00Z",
      "userId": "john.doe",
      "action": "TEST_EXECUTION",
      "details": {
        "sampleCount": 3,
        "instrumentId": "1",
        "batchNumber": "Batch-001"
      },
      "changeType": "INSERT"
    },
    {
      "timestamp": "2026-01-07T10:45:00Z",
      "userId": "john.doe",
      "action": "FILE_UPLOAD",
      "details": {
        "fileName": "LC-MS-MS_Sample_BIO-2024-001.csv",
        "fileSize": 15240,
        "fileType": "CSV"
      },
      "changeType": "INSERT"
    }
  ],
  "dataIntegrity": {
    "checksumVerified": true,
    "timestampVerified": true,
    "originalityVerified": true,
    "userAttribution": {
      "john.doe": "TEST_EXECUTION, FILE_UPLOAD",
      "analyst_001": "ANALYST_REVIEW"
    },
    "contemporaneousRecord": true,
    "lastModified": "2026-01-07T14:00:00Z",
    "modificationCount": 0
  }
}
```

**Audit Trail Events to Log:**
- TEST_EXECUTION (when Tab 1 form submitted)
- FILE_UPLOAD (when file uploaded in Tab 2)
- CALIBRATION_RECORDED (when Tab 3 calibration/QC saved)
- RESULTS_CALCULATED (when Tab 4 processes data)
- SAMPLE_RESULTS_RECORDED (when Tab 5 results approved)
- DEVIATION_RECORDED (when Tab 6 deviation logged)
- ANALYST_REVIEW_APPROVED (when Tab 7 approved)
- QA_REVIEW_APPROVED (when Tab 8 approved)
- MANAGER_APPROVED (when Tab 9 approved)

**Compliance Requirements:**
- ✅ Timestamp all actions
- ✅ Record user ID for attribution
- ✅ Preserve original data (no overwrites, append only)
- ✅ Track modification count
- ✅ Immutable audit records

---

## Critical Backend Issues to Address

### **Issue 1: File Upload Endpoint**

**Status:** ⚠️ **MISSING**

**What's needed:**
- Endpoint to handle multipart/form-data file uploads
- File format validation based on analyzer type
- File storage (database or S3)
- File metadata persistence to sample.data.rawDataFiles

**Recommended Implementation:**
```java
@PostMapping("/rest/notebook/page/{pageId}/samples/{sampleId}/upload-file")
public ResponseEntity<?> uploadFile(
    @PathVariable Integer pageId,
    @PathVariable String sampleId,
    @RequestParam("file") MultipartFile file,
    @RequestParam("instrumentId") String instrumentId
) {
    // Validate file format
    // Store file
    // Update sample.data.rawDataFiles
    // Return file metadata
}
```

---

### **Issue 2: Calibration/QC Endpoint**

**Status:** ⚠️ **MISSING**

**What's needed:**
- Endpoint to record calibration curve and QC results
- Validation against acceptance criteria from Stage 2
- Westgard rule detection
- Data persistence to sample.data.calibrationData and sample.data.qcResults

**Recommended Implementation:**
```java
@PostMapping("/rest/notebook/page/{pageId}/samples/{sampleId}/calibration-qc")
public ResponseEntity<?> recordCalibrationQC(
    @PathVariable Integer pageId,
    @PathVariable String sampleId,
    @RequestBody CalibrationQCRequest request
) {
    // Validate against Stage 2 acceptance criteria
    // Perform Westgard rule detection
    // Persist data
    // Return validation results
}
```

---

### **Issue 3: Results Calculation Endpoint**

**Status:** ⚠️ **MISSING**

**What's needed:**
- Endpoint to calculate sample concentrations from calibration and peak data
- QC recovery calculation
- Pass/fail determination
- Persistence to sample.data.sampleResults

**Recommended Implementation:**
```java
@PostMapping("/rest/notebook/page/{pageId}/samples/{sampleId}/calculate-results")
public ResponseEntity<?> calculateResults(
    @PathVariable Integer pageId,
    @PathVariable String sampleId,
    @RequestBody ResultsCalculationRequest request
) {
    // Load calibration curve from sample.data
    // Calculate concentration = (peakArea - intercept) / slope
    // Calculate recovery % = (calculated / expected) × 100
    // Determine pass/fail
    // Persist results
}
```

---

### **Issue 4: Review Approval Endpoint**

**Status:** ⚠️ **MISSING**

**What's needed:**
- Endpoint to record analyst, QA, and manager reviews
- Role-based permission checking (analyst, QA scientist, manager)
- State transition validation (ANALYST_APPROVED → QA_APPROVED → MANAGER_APPROVED)
- Electronic signature capture
- Audit trail entry creation

**Recommended Implementation:**
```java
@PostMapping("/rest/notebook/page/{pageId}/samples/{sampleId}/review")
public ResponseEntity<?> submitReview(
    @PathVariable Integer pageId,
    @PathVariable String sampleId,
    @RequestBody ReviewRequest request
) {
    // Verify user role (analyst/qa/manager)
    // Validate state transition
    // Capture electronic signature
    // Update reviewStatus
    // Create audit trail entry
}
```

---

### **Issue 5: Error Handling & Validation**

**Status:** ⚠️ **Needs Improvement**

**Required:**
- ✅ All API responses should follow standard format
- ✅ Error messages should be user-friendly
- ✅ Validation errors should list all violations
- ✅ HTTP status codes should be appropriate:
  - 200/201 for success
  - 400 for validation errors
  - 401 for authentication errors
  - 403 for authorization errors
  - 404 for not found
  - 500 for server errors
- ✅ All exceptions should be caught and logged

**Standard Response Format:**
```json
{
  "success": true,
  "data": {...},
  "message": "Operation completed successfully",
  "timestamp": "2026-01-07T10:30:00Z"
}
```

**Error Response Format:**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      "analystId cannot be empty",
      "instrumentId must be between 1-11",
      "executionDate must be valid ISO date"
    ]
  },
  "timestamp": "2026-01-07T10:30:00Z"
}
```

---

### **Issue 6: Data Validation Rules**

**Status:** ⚠️ **Needs Implementation**

**Validation Rules to Implement:**

**Test Execution (Tab 1):**
- analystId: Required, non-empty string
- instrumentId: Required, must exist in ANALYZERS (1-11)
- executionDate: Optional, must be valid ISO date
- batchNumber: Optional, string
- notes: Optional, string

**Calibration (Tab 3):**
- R² minimum: Check against Stage 2 acceptance criteria
- Slope: Check against Stage 2 range (typically 0.8-1.2)
- Intercept: Check against Stage 2 max (typically ±20%)
- QC accuracy: Check against limits (85-115% or Stage 2 value)
- QC precision: CV ≤15% (or Stage 2 value)

**Sample Results (Tab 5):**
- Calculated concentration must be within calibration range
- Recovery % must be within acceptance limits
- Must pass all QC checks before approval

---

## Data Persistence Checklist

- [ ] Execution Configuration persists to database
- [ ] File metadata persists to sample.data.rawDataFiles
- [ ] Calibration data persists to sample.data.calibrationData
- [ ] QC results persist to sample.data.qcResults
- [ ] Sample results persist to sample.data.sampleResults
- [ ] Deviations persist to sample.data.deviations
- [ ] Review data persists to appropriate review fields
- [ ] Audit trail entries are created for each action
- [ ] Timestamps are generated on backend (not frontend)
- [ ] Electronic signatures are immutable
- [ ] Original data is never overwritten (append-only audit trail)

---

## Security & Compliance Checklist

- [ ] All endpoints require authentication (logged-in user)
- [ ] Role-based access control implemented:
  - Analyst can: Record execution, upload files, record QC, submit analyst review
  - QA can: Submit QA review (only after analyst approval)
  - Manager can: Submit final approval (only after QA approval)
- [ ] Electronic signatures include user ID and timestamp
- [ ] All data modifications logged to audit trail
- [ ] No data can be deleted (soft delete only if needed)
- [ ] Timestamps use server time (UTC preferred)
- [ ] 21 CFR Part 11 compliance: Original data preserved, audit trail immutable

---

## Testing Checklist

**For Each Endpoint:**
- [ ] Valid request succeeds
- [ ] Missing required fields returns 400 with clear error message
- [ ] Invalid instrument ID returns 400
- [ ] Invalid date format returns 400
- [ ] Unauthorized user returns 401
- [ ] Non-existent sample returns 404
- [ ] Duplicate submission is idempotent (doesn't create duplicate records)
- [ ] Data persists correctly in database
- [ ] Audit trail entry is created
- [ ] Response includes updated sample data

---

## Summary

**Currently Implemented:**
- ✅ Test Execution modal in frontend
- ✅ Instrument dropdown with 11 analyzers
- ✅ Data persistence endpoint (/rest/notebook/bulk/page/{pageId}/samples/apply)
- ✅ Audit trail logging framework

**Missing/Needs Verification:**
- ⚠️ File upload endpoint
- ⚠️ Calibration/QC recording endpoint
- ⚠️ Results calculation endpoint
- ⚠️ Review approval endpoint
- ⚠️ Validation rule enforcement
- ⚠️ Error handling robustness
- ⚠️ Role-based access control

**Priority Fixes:**
1. Verify existing endpoints handle all data correctly
2. Implement file upload endpoint
3. Implement calibration/QC endpoint
4. Implement results calculation endpoint
5. Implement review approval endpoint
6. Add comprehensive error handling and validation
7. Add role-based access control

