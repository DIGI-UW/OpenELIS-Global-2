# Stage 5: Incubation & Monitoring

**Branch**: `feat/053-tb-lab-workflow` | **Date**: 2025-12-25 **Parent Spec**:
`specs/053-tb-lab-workflow/spec.md`

## Overview

Stage 5 handles weekly monitoring of inoculated TB cultures for up to 8 weeks,
tracking growth observations and determining final culture results
(Positive/Negative/Contaminated).

**Key Workflow:** Inoculated Sample → Weekly Readings (Week 1-8) → Final Result
Determination

**Reference Pattern:** Similar to Bacteriology temperature logging
(`NotebookEntryTemperatureLog`)

---

## User Stories

### US5.1 - Weekly Growth Monitoring (Priority: P1)

Lab technicians check inoculated cultures weekly for up to 8 weeks, recording
growth observations.

**Acceptance Scenarios:**

1. **Given** a sample is inoculated, **When** viewing Incubation Monitoring
   page, **Then** sample appears with inoculation date and current week
2. **Given** an incubating sample, **When** technician clicks on sample row,
   **Then** row expands to show reading history
3. **Given** expanded row, **When** clicking "Add Reading", **Then** modal opens
   with week selection (1-8)
4. **Given** reading modal, **When** submitting, **Then** reading date, growth
   observation (No Growth/Growth Detected/Contaminated), read by, and notes are
   saved
5. **Given** reading saved, **When** viewing expanded row, **Then** new reading
   appears in history table

### US5.2 - Culture Result Determination (Priority: P1)

System auto-prompts for final result based on observations.

**Acceptance Scenarios:**

1. **Given** growth detected at any week, **When** reading saved, **Then**
   system prompts "Mark as Culture Positive?"
2. **Given** Week 8 with no growth, **When** reading saved, **Then** system
   prompts "Mark as Culture Negative?"
3. **Given** contamination detected, **When** reading saved, **Then** system
   prompts to mark as Contaminated or re-process
4. **Given** final result set, **When** viewing sample, **Then** status badge
   shows POSITIVE (red), NEGATIVE (green), or CONTAMINATED (orange)

### US5.3 - Incubation Summary Dashboard (Priority: P2)

Summary tiles show incubation status counts.

**Acceptance Scenarios:**

1. **Given** Incubation Monitoring page loads, **Then** summary tiles display:
   Total Incubating, Week 1-4 count, Week 5-8 count, Positive count, Negative
   count
2. **Given** sample result changes, **When** page refreshes, **Then** counts
   update automatically

---

## Key Components

### Frontend (Existing)

**TBIncubationMonitoringPage.js** (Page 5 in workflow)

- Carbon DataTable with `TableExpandRow` pattern
- Summary tiles for incubation statistics
- Search/filter by sample ID
- Expandable rows showing reading history

**RecordReadingModal.js**

- Week number dropdown (1-8)
- Reading date picker
- Growth observation radio buttons (No Growth / Growth Detected / Contaminated)
- Read By text input
- Notes textarea

**WeeklyReadingTable.js**

- StructuredList showing reading history
- Columns: Week, Date, Observation (color-coded tags), Read By, Notes
- Action buttons: Add Reading, Mark Positive, Mark Negative

### Backend (Existing)

**TbIncubationMonitoringController.java**

Endpoints:

- `GET /rest/tb/incubation/samples` - List incubating samples
- `GET /rest/tb/incubation/samples/{sampleItemId}/readings` - Get reading
  history
- `POST /rest/tb/incubation/reading` - Record new reading
- `PUT /rest/tb/incubation/result/{id}/positive` - Mark positive
- `PUT /rest/tb/incubation/result/{id}/negative` - Mark negative
- `PUT /rest/tb/incubation/result/{id}/contaminated` - Mark contaminated
- `GET /rest/tb/incubation/summary` - Get summary statistics

**TbCultureReadingService**

Uses existing `TbCultureReading` entity enhanced in Stage 4 with:

- `inoculationDate`, `cultureResult`, `positiveWeek`, `finalResultDate`
- `weekNumber`, `growthObservation` fields for readings

---

## Database Schema

Uses existing `tb_culture_reading` table (enhanced in Stage 4):

```sql
-- Stage 4 enhancements already applied:
ALTER TABLE tb_culture_reading
    ADD COLUMN inoculation_date DATE,
    ADD COLUMN culture_result VARCHAR(20),
    ADD COLUMN positive_week INTEGER,
    ADD COLUMN final_result_date DATE;
```

Weekly readings are stored as individual `TbCultureReading` records per week.

---

## API Endpoints

### List Incubating Samples

```
GET /rest/tb/incubation/samples
Response: [
  {
    id: 10,
    sampleItemId: "123",
    accessionNumber: "TB-001/25",
    cultureMethod: "MGIT",
    inoculationDate: "2025-01-15",
    weekNumber: 3,
    growthObservation: "NO_GROWTH",
    cultureResult: null
  }
]
```

### Get Sample Readings

```
GET /rest/tb/incubation/samples/{sampleItemId}/readings
Response: [
  { weekNumber: 1, readingDate: "2025-01-22", observation: "NO_GROWTH", readBy: "JD" },
  { weekNumber: 2, readingDate: "2025-01-29", observation: "NO_GROWTH", readBy: "JD" },
  { weekNumber: 3, readingDate: "2025-02-05", observation: "GROWTH_DETECTED", readBy: "AB" }
]
```

### Record Reading

```
POST /rest/tb/incubation/reading
Request: {
  cultureReadingId: 10,
  weekNumber: 5,
  observation: "GROWTH_DETECTED",
  notes: "Colony morphology observed"
}
Response: {
  id: 10,
  weekNumber: 5,
  observation: "GROWTH_DETECTED",
  autoDetermination: "POSITIVE",
  prompt: "Growth detected. Mark as Culture Positive?"
}
```

### Mark Result

```
PUT /rest/tb/incubation/result/{id}/positive
Request: { positiveWeek: 5 }
Response: { id: 10, result: "POSITIVE", positiveWeek: 5, message: "Culture marked as positive" }
```

---

## UI Wireframe: Expandable DataTable

```
┌─────────────────────────────────────────────────────────────────┐
│ [Thermometer] TB Incubation Monitoring                          │
│ Track weekly culture readings for inoculated samples            │
├─────────────────────────────────────────────────────────────────┤
│ Summary Tiles:                                                  │
│ [Total: 25] [Week 1-4: 18] [Week 5-8: 7] [Positive: 3] [Neg: 2]│
├─────────────────────────────────────────────────────────────────┤
│ [Search samples...]                              [Refresh]      │
├─────────────────────────────────────────────────────────────────┤
│   │ Sample ID │ Method │ Week │ Status        │ Inoculation   │
│───┼───────────┼────────┼──────┼───────────────┼───────────────│
│ ▶ │ TB-001/25 │ LJ     │ 3    │ Incubating    │ 2025-01-10    │
│ ▼ │ TB-002/25 │ MGIT   │ 5    │ POSITIVE      │ 2025-01-05    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ Weekly Readings History                                  │   │
│   ├──────┬────────────┬────────────────┬─────────┬──────────┤   │
│   │ Week │ Date       │ Observation    │ Read By │ Notes    │   │
│   ├──────┼────────────┼────────────────┼─────────┼──────────┤   │
│   │ 1    │ 2025-01-12 │ No Growth      │ JD      │          │   │
│   │ 2    │ 2025-01-19 │ No Growth      │ JD      │          │   │
│   │ 5    │ 2025-02-09 │ GROWTH ⚠️      │ AB      │ Colonies │   │
│   ├──────┴────────────┴────────────────┴─────────┴──────────┤   │
│   │ [+ Add Reading]  [✓ Mark Positive]  [✗ Mark Negative]   │   │
│   └─────────────────────────────────────────────────────────┘   │
│ ▶ │ TB-003/25 │ Both   │ 2    │ Incubating    │ 2025-01-15    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Files Summary

| Category | File                                        | Status |
| -------- | ------------------------------------------- | ------ |
| Frontend | `TBIncubationMonitoringPage.js`             | EXISTS |
| Frontend | `RecordReadingModal.js`                     | EXISTS |
| Frontend | `WeeklyReadingTable.js`                     | EXISTS |
| Backend  | `TbIncubationMonitoringController.java`     | EXISTS |
| Backend  | `TbCultureReadingService[Impl].java`        | EXISTS |
| Test     | `TbIncubationMonitoringControllerTest.java` | NEEDED |
| Test     | `TBIncubationMonitoringPage.test.js`        | NEEDED |
| Test     | `RecordReadingModal.test.js`                | NEEDED |
| Test     | `WeeklyReadingTable.test.js`                | NEEDED |

---

## Success Criteria

- [ ] SC5-01: Expandable DataTable shows incubating samples with reading history
- [ ] SC5-02: Weekly readings can be logged via modal
- [ ] SC5-03: Auto-determination prompts appear (Week 8 no growth → Negative)
- [ ] SC5-04: Final results display with color-coded status badges
- [ ] SC5-05: Summary tiles show accurate counts
- [ ] SC5-06: Backend tests achieve >80% coverage
- [ ] SC5-07: Frontend tests achieve >70% coverage

---

## Dependencies

- Requires Stage 4 (Initial Processing) - samples must be inoculated first
- Uses existing `TbCultureReading` entity (enhanced in Stage 4)
- Registered as Page 5 in `TBWorkflowTab.js`
