# Stage 3 - Tab 1: What Happens When You Execute Tests

## Visual Flow Diagram

```
╔════════════════════════════════════════════════════════════════════════════╗
║                    STAGE 3 - TAB 1: TEST EXECUTION                          ║
║                      (Analytical Execution Start)                           ║
╚════════════════════════════════════════════════════════════════════════════╝

                    USER INTERACTION SEQUENCE
                    ========================

Step 1: Sample Selection
┌─────────────────────────────────────────────────────────┐
│  Sample Table Visible                                   │
│  ┌───────────┬──────────┬────────┬──────────────────┐  │
│  │ ☐ Sample │ Method   │ Staff  │ Status           │  │
│  ├───────────┼──────────┼────────┼──────────────────┤  │
│  │ ☐ BIO-001 │ LC-MS/MS │ John D │ PENDING_ANALYST  │  │
│  │ ☑ BIO-002 │ LC-MS/MS │ John D │ PENDING_ANALYST  │  │
│  │ ☑ BIO-003 │ HPLC     │ Jane S │ PENDING_ANALYST  │  │
│  │ ☐ API-001 │ HPLC UV  │ John D │ PENDING_ANALYST  │  │
│  └───────────┴──────────┴────────┴──────────────────┘  │
│                                                         │
│  Selected: 2 samples (checkboxes ☑)                    │
└─────────────────────────────────────────────────────────┘

Step 2: Modal Opens Automatically
┌──────────────────────────────────────────────────────────────────┐
│ ╔════════════════════════════════════════════════════════════╗  │
│ ║  Test Execution Configuration                              ║  │
│ ║                                                              ║  │
│ ║  Analyst ID * [john.doe________________]                   ║  │
│ ║                                                              ║  │
│ ║  Instrument ID * [Select instrument...▼]                   ║  │
│ ║                  ┌─────────────────────────────┐           ║  │
│ ║                  │ LC-MS/MS System (LCMS)      │           ║  │
│ ║                  │ HPLC System (HPLC)          │           ║  │
│ ║                  │ Dissolution Tester (...)    │           ║  │
│ ║                  │ USP Apparatus II (...)      │           ║  │
│ ║                  └─────────────────────────────┘           ║  │
│ ║                                                              ║  │
│ ║  Batch Number [Batch-001_________________]                 ║  │
│ ║  Execution Date [YYYY-MM-DD____________]                   ║  │
│ ║  Execution Notes [________________]                        ║  │
│ ║                                                              ║  │
│ ║           [Close]  [Execute Tests (2)]                     ║  │
│ ║                                                              ║  │
│ ╚════════════════════════════════════════════════════════════╝  │
└──────────────────────────────────────────────────────────────────┘

Step 3: User Fills in Configuration
┌────────────────────────────────────────────────────────────┐
│ Modal Form is Filled:                                      │
│ • Analyst ID: john.doe                                     │
│ • Instrument ID: 1 (LC-MS/MS System)                       │
│ • Batch Number: Batch-001                                  │
│ • Execution Date: 2026-01-07                               │
│ • Notes: Standard analysis per SOP                         │
│                                                            │
│ Button State: "Execute Tests (2)" is now ENABLED ✓         │
│ (Because 2 samples selected + Analyst ID + Instrument ID) │
└────────────────────────────────────────────────────────────┘

Step 4: User Clicks "Execute Tests (2)"
┌────────────────────────────────────────────────────────────┐
│ BACKEND OPERATIONS:                                        │
│                                                            │
│ 1. API Call Sent:                                          │
│    POST /rest/notebook/bulk/page/{pageId}/samples/apply    │
│                                                            │
│    Payload:                                                │
│    {                                                       │
│      "sampleIds": [62, 63],                               │
│      "data": {                                             │
│        "executionStatus": "EXECUTED",                      │
│        "testExecution": {                                  │
│          "analystId": "john.doe",                          │
│          "instrumentId": "1",                              │
│          "batchNumber": "Batch-001",                       │
│          "executionDate": "2026-01-07",                    │
│          "notes": "Standard analysis per SOP"              │
│        },                                                  │
│        "executedAt": "2026-01-07T10:30:00Z",             │
│        "executedBy": "john.doe"                            │
│      },                                                    │
│      "userId": "john.doe"                                  │
│    }                                                       │
│                                                            │
│ 2. Database Updated:                                       │
│    Sample 62 → sample.data.executionStatus = "EXECUTED"   │
│    Sample 63 → sample.data.executionStatus = "EXECUTED"   │
│                                                            │
│ 3. Audit Trail Entry Created:                              │
│    {                                                       │
│      "action": "TEST_EXECUTION",                           │
│      "userId": "john.doe",                                 │
│      "timestamp": "2026-01-07T10:30:00Z",                 │
│      "sampleCount": 2,                                     │
│      "sampleIds": [62, 63],                               │
│      "instrumentId": "1",                                  │
│      "batchNumber": "Batch-001"                            │
│    }                                                       │
└────────────────────────────────────────────────────────────┘

Step 5: Auto-Navigation to Tab 2
┌────────────────────────────────────────────────────────────┐
│ Modal CLOSES                                               │
│                                                            │
│ UI AUTOMATICALLY SWITCHES TO:                              │
│ Tab 2: Raw Data Upload                                     │
│                                                            │
│ Success Notification Appears:                              │
│ ┌──────────────────────────────────┐                       │
│ │ ✓ Tests executed successfully    │                       │
│ │   for 2 samples                  │                       │
│ └──────────────────────────────────┘                        │
│                                                            │
│ Sample Table NOW SHOWS:                                    │
│ ┌───────────┬──────────┬────────┬─────────────┐            │
│ │ Sample ID │ Method   │ Staff  │ Status      │            │
│ ├───────────┼──────────┼────────┼─────────────┤            │
│ │ BIO-002   │ LC-MS/MS │ John D │ EXECUTED ✓  │            │
│ │ BIO-003   │ HPLC     │ Jane S │ EXECUTED ✓  │            │
│ └───────────┴──────────┴────────┴─────────────┘            │
└────────────────────────────────────────────────────────────┘

Step 6: Tab 2 - Raw Data Upload
┌────────────────────────────────────────────────────────────┐
│ Tab 1    Tab 2 (ACTIVE)  Tab 3  Tab 4  ...                │
│          ═══════════════════════════════════════════════  │
│                                                            │
│ RAW DATA UPLOAD                                            │
│                                                            │
│ Select Instrument: [Select instrument...▼]                │
│ • LC-MS/MS System                                          │
│ • HPLC System (currently selected from Step 1)             │
│ • Dissolution Tester                                       │
│                                                            │
│ Upload Files:                                              │
│ [Drag & drop or click to select files]                     │
│                                                            │
│ Supported Formats:                                         │
│ • For LC-MS/MS: mzML, CDF                                  │
│ • For HPLC: CSV, PDF                                       │
│ • For Dissolution: CSV                                     │
│                                                            │
│ Files Uploaded: (none yet)                                 │
│ ┌─────────────────────────────────────┐                    │
│ │ File Name        │ Size   │ Status  │                    │
│ ├─────────────────────────────────────┤                    │
│ │ (add files here) │        │         │                    │
│ └─────────────────────────────────────┘                    │
│                                                            │
│ Next Step: Upload your chromatogram files and proceed     │
│ to Tab 3 (Calibration & QC)                               │
└────────────────────────────────────────────────────────────┘
```

---

## What Really Happens Inside

### Frontend Side (What You See)

1. **Tab 1 Displays**
   - Sample selection table with checkboxes
   - Stage 2 configuration data shown (Method, Staff, QC levels)
   - Sample status badges (PENDING → EXECUTED after button click)

2. **Modal Opens on First Selection**
   - Automatically shows when you check the first sample
   - Form inputs for Analyst ID, Instrument ID, Batch Number, Date, Notes
   - "Execute Tests (N)" button (where N = count of selected samples)

3. **On Button Click**
   - Loading spinner appears briefly
   - Button becomes disabled during submission
   - Modal stays open briefly, then closes
   - Success notification appears

4. **Automatic Navigation**
   - Selected tab index changes from 0 → 1
   - Page scrolls to Tab 2
   - Tab 2 content becomes visible

### Backend Side (What Happens Behind the Scenes)

1. **Data Persistence**
   - Updates `sample.data` JSONB with execution config
   - Creates audit trail entry with timestamp and user ID
   - Logs file metadata if any files were mentioned

2. **Validation**
   - Checks analyst ID is not empty
   - Checks instrument ID is selected
   - Validates sample IDs match samples in database

3. **Audit Logging**
   - Records who executed the tests
   - Records when execution occurred
   - Records which instrument was selected
   - Records batch number for traceability

---

## State of Data After Execution

### Sample Data in Database

**Before Step 4 (Before Execution):**
```json
{
  "id": 62,
  "sampleType": "Plasma",
  "data": {
    "analyticalMethod": "LC_MS_MS",
    "assignedStaff": "analyst_001",
    "qcLevels": { "low": {...}, "medium": {...}, "high": {...} },
    "acceptanceCriteria": {...},
    "executionStatus": "PENDING"
  }
}
```

**After Step 4 (After Execution):**
```json
{
  "id": 62,
  "sampleType": "Plasma",
  "data": {
    "analyticalMethod": "LC_MS_MS",
    "assignedStaff": "analyst_001",
    "qcLevels": { "low": {...}, "medium": {...}, "high": {...} },
    "acceptanceCriteria": {...},
    "executionStatus": "EXECUTED",           ← NEW
    "testExecution": {                        ← NEW
      "analystId": "john.doe",
      "instrumentId": "1",
      "batchNumber": "Batch-001",
      "executionDate": "2026-01-07",
      "notes": "Standard analysis per SOP"
    },
    "executedAt": "2026-01-07T10:30:00Z",    ← NEW
    "executedBy": "john.doe"                  ← NEW
  }
}
```

### Audit Trail Entry Created

```json
{
  "timestamp": "2026-01-07T10:30:00Z",
  "userId": "john.doe",
  "action": "TEST_EXECUTION",
  "details": {
    "sampleCount": 2,
    "sampleIds": [62, 63],
    "instrumentId": "1",
    "batchNumber": "Batch-001",
    "executionDate": "2026-01-07",
    "rawDataFiles": 0
  }
}
```

---

## Complete Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│  User Selects Samples (Checkboxes)                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  Modal Opens Automatically (First Selection)                │
│  - Shows execution configuration form                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  User Fills Form                                            │
│  - Analyst ID: john.doe                                     │
│  - Instrument: LC-MS/MS System                              │
│  - Batch/Date/Notes (optional)                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  User Clicks "Execute Tests (2)"                            │
│  - Modal shows loading state                                │
│  - Button disabled                                          │
└────────────────────┬────────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
    FRONTEND              BACKEND
    ────────              ────────
    Modal closes    →     Validate inputs
    Success msg    →      Save to database
    setSelectedTab → 1    Create audit entry
    Table updates  →      Return success


          │                     │
          └──────────┬──────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  Tab 2 Active (Raw Data Upload)                             │
│  - Sample status now shows "EXECUTED ✓"                     │
│  - Ready to upload chromatograms/raw data                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Points

✅ **Automatic Modal** - Opens on first sample selection
✅ **Auto-Navigation** - Moves to Tab 2 after successful execution
✅ **Data Preserved** - Stage 2 config data (method, QC levels) stays intact
✅ **Audit Trail** - All execution details recorded with timestamp
✅ **Status Updates** - Sample status changes to "EXECUTED"
✅ **Sequential Workflow** - Tab 1 → Tab 2 → Tab 3... enforced by UI logic

