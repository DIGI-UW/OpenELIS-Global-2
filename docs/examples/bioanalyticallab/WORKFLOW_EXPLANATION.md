# OpenELIS Bioanalytical Notebook Workflow - Stage 1-3 Explanation

## Overview

The bioanalytical notebook workflow is a multi-stage process that guides samples through reception, analytical testing configuration, and execution. Each stage builds upon the previous stage's data.

---

## Complete Workflow: Stage 1 → Stage 2 → Stage 3

### **STAGE 1: Sample Reception & Registration**

**Purpose:** Receive samples and register initial metadata

**Who Uses It:** Lab Technicians, Sample Handlers

**Key Activities:**
1. Import samples via CSV manifest (batch upload)
2. Register sample metadata:
   - Sample Type (API, Tablet, Capsule, Plasma, Serum, etc.)
   - Requested Tests (which tests/analyses are needed)
   - Source Origin (Medical Lab, External Client, Internal Research)
   - Storage Condition (Room Temp, Refrigerated, Frozen)
3. Link to bioequivalence study or project
4. Mark samples COMPLETED to advance to Stage 2

**Data Stored (in sample.data JSONB):**
```json
{
  "sampleType": "Plasma",
  "requestedTests": ["Bioavailability", "Pharmacokinetics"],
  "sourceOrigin": "Medical Laboratory",
  "storageCondition": "Frozen at -80°C",
  "timepoint": "2 hours post-administration"
}
```

**Advancement:** Mark as COMPLETED → Samples advance to Stage 2 (Test Assignment)

---

### **STAGE 2: Test Assignment & Preparation**

**Purpose:** Configure which analytical method to use and prepare for testing

**Who Uses It:** Chemical Analysts, Pharmacists, Method Scientists

**Key Activities:**
1. **Receive samples from Stage 1** with metadata (sample type, requested tests)
2. **Select Analytical Method** based on sample type:
   - HPLC / UV-Vis (chemical analysis, assay, content uniformity)
   - LC-MS/MS (bioanalytical, pharmacokinetics, metabolite ID)
   - Dissolution Testing (pharmaceutical quality, release profiles)
   - Physical Testing (hardness, friability, disintegration)
   - Identity Testing (API verification)

3. **Assign Staff Responsible:**
   - Chemical Analyst (method execution)
   - Pharmacist (review/approval)
   - Researcher (method development)

4. **Configure QC Levels:**
   - Low Concentration QC (e.g., 5 ng/mL ± 20%)
   - Medium Concentration QC (e.g., 50 ng/mL ± 20%)
   - High Concentration QC (e.g., 500 ng/mL ± 20%)

5. **Configure Acceptance Criteria:**
   - R² Minimum (≥0.995 for linear regression)
   - Slope Range (typically 0.8-1.2)
   - Intercept Maximum (typically ±20%)

6. **Document Sample Preparation:**
   - How to prepare the sample (extraction, dilution, etc.)
   - Temperature conditions
   - Timing requirements
   - Equipment settings

7. **Set Expected Analysis Date**

8. **Mark Samples COMPLETED** → Advance to Stage 3

**Data Stored (in sample.data JSONB):**
```json
{
  "sampleType": "Plasma",
  "requestedTests": ["Bioavailability", "Pharmacokinetics"],
  "sourceOrigin": "Medical Laboratory",
  "analyticalMethod": "LC_MS_MS",
  "assignedStaff": "analyst_001",
  "instrumentId": "1",
  "qcLevels": {
    "low": { "concentration": "5", "tolerance": "20" },
    "medium": { "concentration": "50", "tolerance": "20" },
    "high": { "concentration": "500", "tolerance": "20" }
  },
  "acceptanceCriteria": {
    "rSquaredMin": "0.995",
    "slopeRange": { "min": "0.8", "max": "1.2" },
    "interceptMax": "20"
  },
  "samplePreparation": "Protein precipitation with ACN, vortex 2 min, centrifuge 5 min at 4000g",
  "expectedAnalysisDate": "2026-01-15",
  "notes": "High priority bioequivalence study"
}
```

**Advancement:** Mark as COMPLETED → Samples advance to Stage 3 with ALL above data

---

### **STAGE 3: Analytical Test Execution**

**Purpose:** Execute the actual analytical testing and record results

**Who Uses It:** Analysts, Instrument Operators, QA Scientists

**What Stage 3 Should Display (Currently Fixed):**

When samples enter Stage 3, they should arrive WITH the complete configuration from Stage 2:

```
Sample Table in Stage 3:
┌─────────────────────────────────────────────────────────────────┐
│ Sample ID │ Type    │ Method      │ Staff         │ QC Config   │
├─────────────────────────────────────────────────────────────────┤
│ BIO-2024  │ Plasma  │ LC-MS/MS    │ John Doe      │ ✓ Ready     │
│ API-2024  │ API     │ HPLC UV-Vis │ Jane Smith    │ ✓ Ready     │
└─────────────────────────────────────────────────────────────────┘
```

**Stage 3 Activities:**
1. **Receive samples with complete test configuration** from Stage 2
2. **Setup Instrumentation:**
   - Select instrument/analyzer (LC-MS/MS, HPLC, etc.)
   - Verify calibration status
   - Record instrument parameters
3. **Upload Raw Data:**
   - Chromatograms (mzML, CDF, CSV formats)
   - Spectra data
   - Raw instrument files
4. **Perform Quality Control Checks:**
   - Verify calibration curve (r², slope, intercept)
   - Record QC sample results (Low/Medium/High)
   - Check against acceptance criteria
5. **Record Test Results:**
   - Calculated concentrations
   - Units and calculations
   - Data integrity verification
6. **Detect Deviations:**
   - Use Westgard rules to detect QC failures
   - Document any method deviations
   - Justify out-of-spec results
7. **Multi-Level Reviews:**
   - Analyst Review (data validity check)
   - QA/Senior Scientist Review (methodology check)
   - Final Manager/Study Director Approval (regulatory compliance)
8. **Document ALCOA+ Compliance:**
   - Attributable: Who performed the test
   - Legible: Clear documentation
   - Contemporaneous: Real-time recording
   - Original: No alterations
   - Accurate: Verified calculations
   - Complete: All required information
   - Plus: Audit trail, signatures, timestamps

**Data Received from Stage 2 (Used in Stage 3):**
- analyticalMethod
- assignedStaff
- qcLevels (Low/Medium/High configurations)
- acceptanceCriteria (R², slope, intercept limits)
- samplePreparation (method guide)
- expectedAnalysisDate
- All Stage 1 metadata (sampleType, requestedTests, etc.)

---

## The Critical Fix: Data Preservation During Advancement

### **The Problem (Before Fix)**

When samples advanced from Stage 2 to Stage 3:

```
Stage 2 Sample Record (Database):
{
  id: 62,
  pageId: 116,        ← Stage 2
  sampleItemId: "62",
  data: {
    analyticalMethod: "LC_MS_MS",
    assignedStaff: "analyst_001",
    qcLevels: {...},
    acceptanceCriteria: {...}
  }
}

  ↓ API Call: POST /rest/notebook/{notebookId}/samples/advance-string
  ↓ (Contains: sampleIds, fromPageId, toPageIndex)

Stage 3 Sample Record (Database) - BEFORE FIX:
{
  id: NULL,
  pageId: 117,        ← Stage 3
  sampleItemId: "62",
  data: {}            ← EMPTY! Data was lost
}
```

### **Root Cause**

The `advanceSamplesToNextPageString()` endpoint was:
1. Creating a NEW `NotebookPageSample` record on Stage 3
2. Never copying the `data` field from the Stage 2 record
3. Method `createPageSampleForPageString()` had no parameter to accept data

### **The Solution (After Fix)**

**Three Changes Made:**

1. **Service Interface** (`NotebookPageSampleService.java`):
```java
// Added overloaded method
void createPageSampleForPageString(
  Integer pageId,
  String sampleItemId,
  Status status,
  Map<String, Object> data  // ← NEW: Accept data
);
```

2. **Service Implementation** (`NotebookPageSampleServiceImpl.java`):
```java
public void createPageSampleForPageString(Integer pageId, String sampleItemId,
                                         Status status, Map<String, Object> data) {
  NotebookPageSample nps = new NotebookPageSample();
  nps.setNotebookPage(page);
  nps.setSampleItemId(sampleItemId);
  nps.setStatus(status);

  // ← NEW: Copy data from source page
  if (data != null && !data.isEmpty()) {
    nps.setData(data);
  }
  insert(nps);
}
```

3. **Controller Endpoint** (`NoteBookRestController.java`):
```java
for (String sampleId : request.getSampleIds()) {
  // ← NEW: Fetch data from source page
  Map<String, Object> dataToPreserve = null;
  if (request.getFromPageId() != null) {
    var sourcePageSample = notebookPageSampleService
      .getBySampleItemIdAndPageId(sampleId, request.getFromPageId());
    if (sourcePageSample != null && sourcePageSample.getData() != null) {
      dataToPreserve = new HashMap<>(sourcePageSample.getData());
    }
  }

  // ← NEW: Pass data when creating Stage 3 record
  notebookPageSampleService.createPageSampleForPageString(
    targetPageId, sampleId, Status.PENDING, dataToPreserve
  );
}
```

### **Result (After Fix)**

```
Stage 2 Sample Record (Database):
{
  data: {
    analyticalMethod: "LC_MS_MS",
    assignedStaff: "analyst_001",
    ...
  }
}

  ↓ advanceSamplesToNextPageString()

Stage 3 Sample Record (Database) - AFTER FIX:
{
  data: {
    analyticalMethod: "LC_MS_MS",  ← PRESERVED!
    assignedStaff: "analyst_001",   ← PRESERVED!
    qcLevels: {...},                ← PRESERVED!
    acceptanceCriteria: {...}       ← PRESERVED!
  }
}
```

---

## Frontend Integration: Stage 2 Test Assignment Page

The bioanalytical test assignment page (Stage 2) now includes:

### **Features:**
1. **Sample Selection Table**
   - Shows samples from Stage 1
   - Displays Stage 1 metadata (sampleType, requestedTests)
   - Shows assigned tests in colored badges

2. **Test Assignment Configuration Modal**
   - Analytical Method selection
   - Staff assignment
   - QC level configuration
   - Acceptance criteria
   - Sample preparation documentation
   - Expected analysis date

3. **Stage Progression**
   - "Configure Tests" button (for test assignment)
   - "Mark Complete & Move to Next Stage" button (when assignments exist)
   - Automatically marks samples as COMPLETED on Stage 2
   - Automatically advances to Stage 3 with data preserved

### **Frontend Data Flow:**

```javascript
// Stage 2: Test Assignment Data is collected in assignmentConfig:
{
  analyticalMethod: "LC_MS_MS",
  assignedStaff: "analyst_001",
  instrumentId: "1",
  qcLevels: {
    low: { concentration: "5", tolerance: "20" },
    medium: { concentration: "50", tolerance: "20" },
    high: { concentration: "500", tolerance: "20" }
  },
  acceptanceCriteria: {
    rSquaredMin: "0.995",
    slopeRange: { min: "0.8", max: "1.2" },
    interceptMax: "20"
  },
  samplePreparation: "...",
  expectedAnalysisDate: "2026-01-15",
  notes: "..."
}

// Stage 2: Data is saved via bulkApplyValues endpoint
POST /rest/notebook/bulk/page/{pageId}/samples/apply
{
  sampleIds: [62, 63, 64, ...],
  data: { ...assignmentConfig }
}

// Stage 2: Frontend marks complete and advances
POST /rest/notebook/{notebookId}/samples/advance-string
{
  sampleIds: ["62", "63", "64", ...],
  fromPageId: 116,
  toPageIndex: 3
}
// Backend now copies data from page 116 to page 117!

// Stage 3: Frontend loads samples
GET /rest/notebook/page/117/samples
// Response now includes sample.data with full configuration!
```

---

## Testing the Workflow

### **Verify Data Preservation:**

1. Go to Stage 2 (Test Assignment)
2. Select some samples
3. Click "Configure Tests for X Sample(s)"
4. Fill in the form (method, staff, QC levels, etc.)
5. Click "Assign Tests to X Sample(s)"
6. Click "Mark Complete & Move to Next Stage"
7. Go to Stage 3 (Analytical Execution)
8. Check the browser console or network tab
9. Verify response includes `sample.data` with:
   - analyticalMethod
   - assignedStaff
   - qcLevels
   - acceptanceCriteria
   - All other Stage 2 data

### **Before Fix (What You Saw):**
```json
[
  {
    "id": "62",
    "data": {}  // ← Empty!
  }
]
```

### **After Fix (What You Should See):**
```json
[
  {
    "id": "62",
    "data": {
      "analyticalMethod": "LC_MS_MS",
      "assignedStaff": "analyst_001",
      "qcLevels": {...},
      "acceptanceCriteria": {...},
      ...
    }
  }
]
```

---

## Key Architectural Insights

### **JSONB Data Strategy**
- Instead of separate database tables for each stage's configuration, we use JSONB
- Each sample carries forward its historical data as it progresses
- This allows flexibility without schema changes
- Audit trail is maintained naturally as data accumulates

### **Page Advancement Pattern**
- Samples have multiple records (one per page they visit)
- `NotebookPageSample` entity links a sample to a specific page
- Advancement creates a NEW record on the target page
- The fix ensures data is copied to the new record

### **Transactional Consistency**
- All operations marked with `@Transactional`
- Data copying happens within a transaction
- Failures are logged but don't block other samples

---

## Related Commits

- **dbe3070f9**: Fixed form controls (NumberInput, DatePicker) and implemented data persistence on page refresh
- **417412475**: Added stage progression button to move samples from Stage 2 to Stage 3
- **5917de33d**: Fixed data preservation during page advancement (CRITICAL FIX)

---

## Testing Checklist for Stage 3

- [ ] Stage 2 samples have test assignments
- [ ] Click "Mark Complete & Move to Next Stage"
- [ ] Advance to Stage 3
- [ ] Check browser network tab for GET /samples
- [ ] Verify response has `sample.data` with analyticalMethod, assignedStaff, etc.
- [ ] Frontend filtering on lines 259-262 of `BioanalyticalAnalyticalExecutionPage.js` should pass
- [ ] Samples should appear in Stage 3 datatable
- [ ] Sample details should show Stage 2 configuration

