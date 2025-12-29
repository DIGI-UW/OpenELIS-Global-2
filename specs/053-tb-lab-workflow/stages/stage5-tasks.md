# Stage 5: Incubation & Monitoring - Tasks

**Branch**: `feat/053-tb-lab-workflow` | **Date**: 2025-12-25 **Plan**:
`stage5-plan.md`

---

## Task Summary

Stage 5 focuses on validating existing implementation and adding tests. The
frontend page and backend controller were created during Stage 4 implementation.

| Phase                  | Tasks | Status      |
| ---------------------- | ----- | ----------- |
| 1. File Reorganization | 3     | ✅ Complete |
| 2. Backend Validation  | 2     | Pending     |
| 3. Frontend Validation | 3     | Pending     |
| 4. Backend Tests       | 2     | Pending     |
| 5. Frontend Tests      | 3     | Pending     |
| 6. i18n Verification   | 2     | Pending     |

---

## Phase 1: File Reorganization ✅

- [x] T5-01: Rename `stage5-plan.md` → `stage6-plan.md`
- [x] T5-02: Rename `stage6-plan.md` → `stage7-plan.md`
- [x] T5-03: Rename `stage7-plan.md` → `stage8-plan.md`
- [x] T5-04: Create new `stage5-plan.md` for Incubation & Monitoring
- [x] T5-05: Create `stage5-tasks.md`

---

## Phase 2: Backend Validation

### T5-06: Verify TbCultureReadingService Methods

**File**:
`src/main/java/org/openelisglobal/tb/service/TbCultureReadingService.java`

Verify these methods exist:

- [ ] `findIncubatingSamples()` - Get samples with inoculation but no final
      result
- [ ] `findBySampleItemId(String sampleItemId)` - Get readings for a sample
- [ ] `recordReading(Integer id, Integer weekNumber, GrowthObservation obs, String notes, String sysUserId)`
- [ ] `determineFinalResult(Integer id, CultureResult result, Integer positiveWeek)`
- [ ] `getIncubationSummary()` - Return summary statistics
- [ ] `findCulturePositiveSamples()` - For downstream Stage 6

### T5-07: Test Controller Endpoints

**File**:
`src/main/java/org/openelisglobal/tb/controller/rest/TbIncubationMonitoringController.java`

Test each endpoint responds correctly:

- [ ] `GET /rest/tb/incubation/samples` - Returns list of incubating samples
- [ ] `GET /rest/tb/incubation/samples/{sampleItemId}/readings` - Returns
      reading history
- [ ] `POST /rest/tb/incubation/reading` - Creates new reading with
      auto-determination
- [ ] `PUT /rest/tb/incubation/result/{id}/positive` - Sets positive result
- [ ] `PUT /rest/tb/incubation/result/{id}/negative` - Sets negative result
- [ ] `PUT /rest/tb/incubation/result/{id}/contaminated` - Sets contaminated
      result
- [ ] `GET /rest/tb/incubation/summary` - Returns summary stats

---

## Phase 3: Frontend Validation

### T5-08: Validate TBIncubationMonitoringPage.js

**File**:
`frontend/src/components/notebook/pages/tb/TBIncubationMonitoringPage.js`

- [ ] Page renders without errors
- [ ] Summary tiles display correctly
- [ ] DataTable shows sample list
- [ ] Expandable rows work (click to expand/collapse)
- [ ] Search/filter functions
- [ ] Refresh button reloads data

### T5-09: Validate RecordReadingModal.js

**File**:
`frontend/src/components/notebook/pages/tb/components/RecordReadingModal.js`

- [ ] Modal opens when "Add Reading" clicked
- [ ] Week number dropdown works (1-8)
- [ ] Growth observation radio buttons work
- [ ] Reading date picker works
- [ ] Save button submits correctly
- [ ] Modal closes after save

### T5-10: Validate WeeklyReadingTable.js

**File**:
`frontend/src/components/notebook/pages/tb/components/WeeklyReadingTable.js`

- [ ] Displays reading history in expanded row
- [ ] Color-coded observation tags (green=No Growth, red=Growth,
      orange=Contaminated)
- [ ] "Add Reading" button opens modal
- [ ] "Mark Positive" button works
- [ ] "Mark Negative" button works

---

## Phase 4: Backend Tests

### T5-11: Create Controller Integration Test

**File**:
`src/test/java/org/openelisglobal/tb/controller/TbIncubationMonitoringControllerTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
public class TbIncubationMonitoringControllerTest {
    // Test GET /rest/tb/incubation/samples
    // Test GET /rest/tb/incubation/samples/{id}/readings
    // Test POST /rest/tb/incubation/reading
    // Test PUT /rest/tb/incubation/result/{id}/positive
    // Test PUT /rest/tb/incubation/result/{id}/negative
    // Test GET /rest/tb/incubation/summary
}
```

### T5-12: Create Service Layer Tests

**File**:
`src/test/java/org/openelisglobal/tb/service/TbCultureReadingServiceIncubationTest.java`

- [ ] Test `findIncubatingSamples()` returns only samples without final result
- [ ] Test `recordReading()` creates reading with correct week
- [ ] Test `determineFinalResult()` sets result and date
- [ ] Test `getIncubationSummary()` returns correct counts

---

## Phase 5: Frontend Tests

### T5-13: Create Page Test

**File**:
`frontend/src/components/notebook/pages/tb/TBIncubationMonitoringPage.test.js`

- [ ] Renders without crashing
- [ ] Displays summary tiles
- [ ] Renders DataTable with sample rows
- [ ] Expandable row functionality

### T5-14: Create RecordReadingModal Test

**File**:
`frontend/src/components/notebook/pages/tb/components/RecordReadingModal.test.js`

- [ ] Renders when open=true
- [ ] Week dropdown has options 1-8
- [ ] Radio buttons for observations
- [ ] Calls onSave with correct data

### T5-15: Create WeeklyReadingTable Test

**File**:
`frontend/src/components/notebook/pages/tb/components/WeeklyReadingTable.test.js`

- [ ] Renders reading history
- [ ] Displays correct observation tags
- [ ] Action buttons render

---

## Phase 6: i18n Verification

### T5-16: Verify English Keys

**File**: `frontend/src/languages/en.json`

Check these keys exist:

- [ ] `notebook.page.tb.incubationMonitoring.title`
- [ ] `notebook.page.tb.incubationMonitoring.description`
- [ ] `notebook.tb.incubation.sampleId`
- [ ] `notebook.tb.incubation.method`
- [ ] `notebook.tb.incubation.week`
- [ ] `notebook.tb.incubation.status`
- [ ] `notebook.tb.incubation.inocDate`
- [ ] `notebook.tb.incubation.totalIncubating`
- [ ] `notebook.tb.incubation.positive`
- [ ] `notebook.tb.incubation.negative`
- [ ] `notebook.tb.incubation.readingSaved`
- [ ] `notebook.tb.incubation.markedPositive`
- [ ] `notebook.tb.incubation.markedNegative`

### T5-17: Verify French Keys

**File**: `frontend/src/languages/fr.json`

- [ ] All keys from T5-16 have French translations

---

## Milestone Checkpoints

### M5-1: Backend Validated

- [ ] All service methods exist and work
- [ ] All controller endpoints respond correctly

### M5-2: Frontend Validated

- [ ] Page renders and functions correctly
- [ ] Modal and table components work

### M5-3: Tests Complete

- [ ] Backend tests pass with >80% coverage
- [ ] Frontend tests pass with >70% coverage

### M5-4: Stage 5 Complete

- [ ] All tasks completed
- [ ] All success criteria met
- [ ] Ready for Stage 6 (Assay/Test Execution)

---

## Notes

- Stage 5 components were created during Stage 4 implementation work
- This stage focuses on validation, testing, and documentation
- Uses existing `TbCultureReading` entity (no new database tables needed)
- Page 5 is already registered in `TBWorkflowTab.js`
