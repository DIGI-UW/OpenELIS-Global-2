# Stage 4: Initial Processing & Incubation

**Branch**: `feat/053-tb-lab-workflow` | **Date**: 2025-12-24 **Parent Spec**:
`specs/053-tb-lab-workflow/spec.md`

## Overview

Stage 4 handles laboratory processing of TB samples after QC/storage assignment
(Stage 3), through inoculation and weekly growth monitoring.

**ARCHITECTURE: TWO SEPARATE PAGES**

1. **Page 4: TBInitialProcessingPage** - Media Prep → Sample Processing →
   Inoculation
2. **Page 5: TBIncubationMonitoringPage** - Weekly readings with expandable
   sample rows

**Key Workflow:** Media Preparation → Sample Processing → Inoculation → [Move to
Incubation Page] → Weekly Monitoring (8 weeks)

---

## Page 1: Initial Processing (TBInitialProcessingPage)

**Reference Pattern:** `BacteriologyProcessingQCPage.js`

### UX Structure

```
┌─────────────────────────────────────────────────────────────────┐
│ [Flask Icon] TB Initial Processing                               │
│ Process samples through decontamination and inoculation          │
├─────────────────────────────────────────────────────────────────┤
│ Progress Tiles:                                                  │
│ [Total] [Pending] [Processed] [Inoculated] [Ready for Incub]    │
├─────────────────────────────────────────────────────────────────┤
│ Actions: [Assign Media Prep] [Record Processing] [Inoculate]    │
├─────────────────────────────────────────────────────────────────┤
│ Sample Grid (with selection, status filter, search)             │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ □ Sample ID | Type | Status | Prep Info | Processing Info   │ │
│ │ □ TB-001    | Sputum | PENDING | - | -                       │ │
│ │ □ TB-002    | Sputum | PROCESSED | LJ Batch-01 | NALC-NaOH  │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Modal Workflows

**Modal 1: Media Preparation**

- Media Type (LJ/MGIT/Both)
- Batch ID (auto-generated or manual)
- Preparation Date, Expiry Date
- QC Status (Pass/Fail)
- Prepared By

**Modal 2: Sample Processing**

- Decontamination Method (NALC-NaOH, NaOH Only, Other)
- Processing Date, Processed By
- Notes
- → Mark as "Ready for Inoculation"

**Modal 3: Inoculation**

- Select Media Batch (filtered by type, QC passed, not expired)
- Media Type (LJ/MGIT/Both)
- Inoculation Date, Inoculated By
- → Move to Incubation Queue

---

## Page 2: Incubation Monitoring (TBIncubationMonitoringPage) - NEW

**Reference Pattern:** Temperature logging from
`BacteriologyTemporaryStoragePage.js` **Key Component:** Carbon `ExpandableRow`
DataTable

### UX Structure with Expandable Rows

```
┌─────────────────────────────────────────────────────────────────┐
│ [Thermometer Icon] TB Incubation Monitoring                      │
│ Track weekly culture readings for inoculated samples             │
├─────────────────────────────────────────────────────────────────┤
│ Summary Tiles:                                                   │
│ [Incubating] [Week 1-4] [Week 5-8] [Positive] [Negative]        │
├─────────────────────────────────────────────────────────────────┤
│ Actions: [Record Reading] [Mark Positive] [Mark Negative]       │
├─────────────────────────────────────────────────────────────────┤
│ Sample Grid with Expandable Rows:                                │
│ │ ▶ TB-001 | LJ | Week 3 | No Growth | 2025-01-15               │
│ │ ▼ TB-002 | MGIT | Week 5 | POSITIVE | 2025-01-10              │
│ │   ┌─────────────────────────────────────────────────────┐     │
│ │   │ Weekly Readings (Accordion Content)                  │     │
│ │   │ │ Wk │ Date  │ Observation │ Read By │ Notes       │     │
│ │   │ │ 1  │ 01-03 │ No Growth   │ JD      │             │     │
│ │   │ │ 5  │ 01-31 │ GROWTH      │ AB      │ Colonies    │     │
│ │   │ [+ Add Reading] [Mark as Positive] [View Details]  │     │
│ │   └─────────────────────────────────────────────────────┘     │
│ │ ▶ TB-003 | Both | Week 2 | No Growth | 2025-01-20             │
└─────────────────────────────────────────────────────────────────┘
```

### Reading Log Modal

```
┌─────────────────────────────────────────────────────────────────┐
│ Record Weekly Reading                                    [X]    │
├─────────────────────────────────────────────────────────────────┤
│ Sample: TB-002 (LJ) | Current Week: 5                           │
├─────────────────────────────────────────────────────────────────┤
│ Week Number: [5 ▼]                                              │
│ Reading Date: [2025-01-31]                                      │
│ Observation: (○) No Growth  (○) Growth Detected  (○) Contaminated│
│ Read By (Initials): [AB        ]                                │
│ Notes: [Colony morphology observed...                    ]      │
├─────────────────────────────────────────────────────────────────┤
│                              [Cancel]  [Save Reading]           │
└─────────────────────────────────────────────────────────────────┘
```

### Auto-Determination Logic

- **Week 8 + No Growth** → Auto-prompt to mark as "Culture Negative"
- **Any Week + Growth Detected** → Auto-prompt to mark as "Culture Positive" →
  Stage 5
- **Contaminated** → Option to re-process or mark failed

---

## User Stories

### US4.1 - Media Preparation (Priority: P1)

Lab technicians prepare culture media (LJ slants, MGIT tubes) with batch
tracking.

**Acceptance Scenarios:**

1. Given media preparation starts, When technician records batch info, Then
   batch ID, media type (LJ/MGIT), preparation date, expiry date, technician
   initials captured
2. Given media is prepared, When QC check done, Then sterility/contamination
   status recorded
3. Given media fails QC, When flagged, Then batch marked unusable with reason

### US4.2 - Sample Processing/Decontamination (Priority: P1)

Samples undergo decontamination (NALC-NaOH) before inoculation.

**Acceptance Scenarios:**

1. Given sample arrives for processing, When decontamination starts, Then
   processing date, method, technician recorded
2. Given decontamination complete, When proceeding, Then sample marked ready for
   inoculation
3. Given multiple samples processed, When batch processing, Then same
   method/date applied to selected samples

### US4.3 - Inoculation (Priority: P1)

Processed samples inoculated to culture media.

**Acceptance Scenarios:**

1. Given sample ready, When inoculating, Then media type (LJ/MGIT/Both),
   inoculation date, media batch ID, technician recorded
2. Given inoculation complete, When saved, Then sample moves to Incubation
   Monitoring page
3. Given media batch selected, When linking, Then sample linked to specific
   media preparation batch

### US4.4 - Weekly Growth Monitoring (Priority: P1) - ON SEPARATE PAGE

Weekly checks for up to 8 weeks recording growth observations.

**Acceptance Scenarios:**

1. Given sample in incubation, When week N arrives, Then technician records
   reading date, growth status (No Growth/Growth Detected/Contaminated),
   initials
2. Given 8 weeks no growth, When Week 8 recorded as "No Growth", Then sample
   marked "Culture Negative"
3. Given growth detected at any week, When recorded, Then sample marked "Culture
   Positive" → proceeds to Stage 5
4. Given MGIT method, When instrument flags positive, Then reading can be
   recorded with instrument date

---

## Key Entities

### New: TbMediaPreparation

```java
@Entity
@Table(name = "tb_media_preparation")
public class TbMediaPreparation extends BaseObject<Integer> {
    private String batchId;
    private MediaType mediaType;      // LJ, MGIT
    private Date preparationDate;
    private Date expiryDate;
    private MediaQcStatus qcStatus;   // PENDING, PASSED, FAILED
    private String qcNotes;
    private SystemUser preparedBy;
}
```

### New: TbSampleProcessing

```java
@Entity
@Table(name = "tb_sample_processing")
public class TbSampleProcessing extends BaseObject<Integer> {
    private SampleItem sampleItem;        // FK to SampleItem
    private Date processingDate;
    private DecontaminationMethod method;  // NALC_NAOH, NAOH_ONLY, OTHER
    private String methodNotes;
    private SystemUser processedBy;
    private ProcessingStatus status;  // PENDING, PROCESSED, READY_FOR_INOCULATION
}
```

### Modified: TbCultureReading

Add fields:

- `inoculationDate` (Date)
- `mediaBatchId` (FK to TbMediaPreparation)
- `sampleProcessingId` (FK to TbSampleProcessing)
- `cultureResult` (enum: POSITIVE/NEGATIVE/CONTAMINATED)
- `positiveWeek` (Integer, null if negative)
- `finalResultDate` (Date)

---

## Database Schema

### tb_media_preparation

```sql
CREATE TABLE tb_media_preparation (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL UNIQUE,
    media_type VARCHAR(10) NOT NULL CHECK (media_type IN ('LJ', 'MGIT')),
    preparation_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    qc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (qc_status IN ('PENDING', 'PASSED', 'FAILED')),
    qc_notes TEXT,
    prepared_by INTEGER REFERENCES system_user(id),
    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_media_prep_batch ON tb_media_preparation(batch_id);
CREATE INDEX idx_media_prep_type ON tb_media_preparation(media_type);
CREATE INDEX idx_media_prep_expiry ON tb_media_preparation(expiry_date);
```

### tb_sample_processing

```sql
CREATE TABLE tb_sample_processing (
    id SERIAL PRIMARY KEY,
    sample_item_id VARCHAR(10) NOT NULL REFERENCES sample_item(id),
    processing_date DATE NOT NULL,
    method VARCHAR(20) NOT NULL CHECK (method IN ('NALC_NAOH', 'NAOH_ONLY', 'OTHER')),
    method_notes TEXT,
    processed_by INTEGER REFERENCES system_user(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'READY_FOR_INOCULATION')),
    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sample_proc_sample ON tb_sample_processing(sample_item_id);
CREATE INDEX idx_sample_proc_status ON tb_sample_processing(status);
```

### Alter tb_culture_reading

```sql
ALTER TABLE tb_culture_reading
    ADD COLUMN IF NOT EXISTS inoculation_date DATE,
    ADD COLUMN IF NOT EXISTS media_batch_id INTEGER REFERENCES tb_media_preparation(id),
    ADD COLUMN IF NOT EXISTS sample_processing_id INTEGER REFERENCES tb_sample_processing(id),
    ADD COLUMN IF NOT EXISTS culture_result VARCHAR(20) CHECK (culture_result IN ('POSITIVE', 'NEGATIVE', 'CONTAMINATED')),
    ADD COLUMN IF NOT EXISTS positive_week INTEGER CHECK (positive_week BETWEEN 1 AND 8),
    ADD COLUMN IF NOT EXISTS final_result_date DATE;

CREATE INDEX idx_culture_reading_batch ON tb_culture_reading(media_batch_id);
CREATE INDEX idx_culture_reading_proc ON tb_culture_reading(sample_processing_id);
CREATE INDEX idx_culture_reading_result ON tb_culture_reading(culture_result);
```

---

## Implementation Tasks

### Backend (T4-01 to T4-11)

- T4-01: Liquibase for `tb_media_preparation` (032-tb-media-preparation.xml)
- T4-02: Liquibase for `tb_sample_processing` (033-tb-sample-processing.xml)
- T4-03: Alter `tb_culture_reading` with new fields
  (034-tb-culture-reading-enhance.xml)
- T4-04: Update base.xml with new changesets
- T4-05: TbEnums enhancement (MediaQcStatus, DecontaminationMethod,
  ProcessingStatus)
- T4-06: TbMediaPreparation valueholder + DAO + Service
- T4-07: TbSampleProcessing valueholder + DAO + Service
- T4-08: Update TbCultureReading valueholder with new fields
- T4-09: TbInitialProcessingController REST endpoints
- T4-10: TbIncubationMonitoringController REST endpoints (NEW)
- T4-11: TbCultureReadingService enhancements (getIncubatingSamples,
  recordReading, determineFinalResult)

### Backend Tests (T4-12 to T4-17)

- T4-12: TbMediaPreparationOrmTest
- T4-13: TbSampleProcessingOrmTest
- T4-14: TbMediaPreparationServiceTest
- T4-15: TbSampleProcessingServiceTest
- T4-16: TbCultureReadingInoculationTest
- T4-17: TbInitialProcessingControllerTest

### Frontend - Page 1: Initial Processing (T4-18 to T4-23)

- T4-18: REWRITE TBInitialProcessingPage (BacteriologyProcessingQCPage pattern)
- T4-19: Progress tiles component
- T4-20: MediaPreparationModal component
- T4-21: SampleProcessingModal component
- T4-22: InoculationModal component
- T4-23: Sample grid with custom columns

### Frontend - Page 2: Incubation Monitoring (T4-24 to T4-29)

- T4-24: CREATE TBIncubationMonitoringPage (NEW - ExpandableRow DataTable)
- T4-25: ExpandableRow DataTable with sample readings
- T4-26: WeeklyReadingTable component (accordion content)
- T4-27: RecordReadingModal component
- T4-28: Auto-determination prompts component
- T4-29: DELETE TBCultureTrackingPage (replaced)

### Frontend Shared (T4-30 to T4-35)

- T4-30: i18n keys for both pages (en.json)
- T4-31: i18n keys for both pages (fr.json)
- T4-32: SWR hooks (useMediaBatches, useProcessedSamples, useIncubatingSamples,
  useReadings)
- T4-33: Jest tests for TBInitialProcessingPage
- T4-34: Jest tests for TBIncubationMonitoringPage
- T4-35: Jest tests for modal components

---

## API Endpoints

### Media Preparation

```
POST /rest/tb/media-preparation
Request: {
    batchId: "LJ-2025-001",
    mediaType: "LJ",
    preparationDate: "2025-01-15",
    expiryDate: "2025-04-15",
    preparedBy: "tech_user"
}
Response: { id: 1, batchId: "LJ-2025-001", ... }

GET /rest/tb/media-preparation?mediaType=LJ&qcStatus=PASSED&includeExpired=false
Response: [{ id: 1, batchId: "LJ-2025-001", ... }, ...]

PUT /rest/tb/media-preparation/{id}/qc
Request: { qcStatus: "PASSED", qcNotes: "Sterility check passed" }
Response: { id: 1, qcStatus: "PASSED", ... }
```

### Sample Processing

```
POST /rest/tb/processing/samples
Request: {
    sampleItemIds: ["1", "2", "3"],
    processingDate: "2025-01-16",
    method: "NALC_NAOH",
    methodNotes: "Standard protocol",
    processedBy: "tech_user"
}
Response: [{ id: 1, status: "PROCESSED", ... }, ...]

PUT /rest/tb/processing/{id}/ready
Response: { id: 1, status: "READY_FOR_INOCULATION" }
```

### Inoculation

```
POST /rest/tb/processing/inoculate
Request: {
    sampleProcessingId: 1,
    mediaType: "MGIT",
    mediaBatchId: 5,
    inoculationDate: "2025-01-17"
}
Response: {
    cultureReadingId: 10,
    inoculationDate: "2025-01-17",
    mediaType: "MGIT",
    week1DueDate: "2025-01-24"
}
```

### Incubation Monitoring

```
GET /rest/tb/incubation/samples?entryId={entryId}&status=INCUBATING
Response: [{ id: 10, sampleId: "TB-001", mediaType: "MGIT", currentWeek: 3, ... }, ...]

POST /rest/tb/incubation/{cultureReadingId}/reading
Request: {
    weekNumber: 5,
    readingDate: "2025-01-31",
    observation: "GROWTH_DETECTED",
    readBy: "AB",
    notes: "Colony morphology observed"
}
Response: { readingId: 15, ... }

PUT /rest/tb/incubation/{cultureReadingId}/result
Request: {
    cultureResult: "POSITIVE",
    positiveWeek: 5
}
Response: { id: 10, cultureResult: "POSITIVE", ... }
```

---

## Files Summary

| Category  | File                                                       | Action  |
| --------- | ---------------------------------------------------------- | ------- |
| Liquibase | `liquibase/3.4.x.x/032-tb-media-preparation.xml`           | CREATE  |
| Liquibase | `liquibase/3.4.x.x/033-tb-sample-processing.xml`           | CREATE  |
| Liquibase | `liquibase/3.4.x.x/034-tb-culture-reading-enhance.xml`     | CREATE  |
| Liquibase | `liquibase/3.4.x.x/base.xml`                               | MODIFY  |
| Backend   | `tb/valueholder/TbEnums.java`                              | MODIFY  |
| Backend   | `tb/valueholder/TbMediaPreparation.java`                   | CREATE  |
| Backend   | `tb/valueholder/TbSampleProcessing.java`                   | CREATE  |
| Backend   | `tb/valueholder/TbCultureReading.java`                     | MODIFY  |
| Backend   | `tb/dao/TbMediaPreparationDAO.java`                        | CREATE  |
| Backend   | `tb/daoimpl/TbMediaPreparationDAOImpl.java`                | CREATE  |
| Backend   | `tb/service/TbMediaPreparationService.java`                | CREATE  |
| Backend   | `tb/service/TbMediaPreparationServiceImpl.java`            | CREATE  |
| Backend   | `tb/dao/TbSampleProcessingDAO.java`                        | CREATE  |
| Backend   | `tb/daoimpl/TbSampleProcessingDAOImpl.java`                | CREATE  |
| Backend   | `tb/service/TbSampleProcessingService.java`                | CREATE  |
| Backend   | `tb/service/TbSampleProcessingServiceImpl.java`            | CREATE  |
| Backend   | `tb/controller/rest/TbInitialProcessingController.java`    | CREATE  |
| Backend   | `tb/controller/rest/TbIncubationMonitoringController.java` | CREATE  |
| Test      | `tb/TbMediaPreparationOrmTest.java`                        | CREATE  |
| Test      | `tb/TbSampleProcessingOrmTest.java`                        | CREATE  |
| Test      | `tb/service/TbMediaPreparationServiceTest.java`            | CREATE  |
| Test      | `tb/service/TbSampleProcessingServiceTest.java`            | CREATE  |
| Test      | `tb/controller/TbInitialProcessingControllerTest.java`     | CREATE  |
| Frontend  | `pages/tb/TBInitialProcessingPage.js`                      | REWRITE |
| Frontend  | `pages/tb/TBIncubationMonitoringPage.js`                   | CREATE  |
| Frontend  | `pages/tb/TBCultureTrackingPage.js`                        | DELETE  |
| Frontend  | `pages/tb/components/MediaPreparationModal.js`             | CREATE  |
| Frontend  | `pages/tb/components/SampleProcessingModal.js`             | CREATE  |
| Frontend  | `pages/tb/components/InoculationModal.js`                  | CREATE  |
| Frontend  | `pages/tb/components/RecordReadingModal.js`                | CREATE  |
| Frontend  | `pages/tb/components/WeeklyReadingTable.js`                | CREATE  |
| Frontend  | `pages/tb/hooks/useStage4.js`                              | CREATE  |
| Frontend  | `languages/en.json`                                        | MODIFY  |
| Frontend  | `languages/fr.json`                                        | MODIFY  |
| Frontend  | `pages/tb/TBInitialProcessingPage.test.js`                 | CREATE  |
| Frontend  | `pages/tb/TBIncubationMonitoringPage.test.js`              | CREATE  |

---

## Carbon Components Required

**Initial Processing Page:**

- Grid, Column, Button, Modal (3), InlineNotification
- DataTable, TableHead, TableRow, TableCell, TableToolbar, TableToolbarSearch
- Checkbox, Select, MultiSelect, DatePicker, TextInput, TextArea, Tag, Tile

**Incubation Monitoring Page:**

- DataTable with **ExpandableRow** (key component)
- Modal, RadioButtonGroup, DatePicker, TextInput, TextArea
- Tag (green=negative, red=positive, yellow=incubating)
- InlineNotification (auto-determination prompts)

---

## Success Criteria

- [ ] SC4-01: Lab tech can create media batch in <1 minute
- [ ] SC4-02: Sample processing with batch mode handles 10 samples in <2 minutes
- [ ] SC4-03: Inoculation links sample to media batch with full traceability
- [ ] SC4-04: After inoculation, sample appears on Incubation Monitoring page
- [ ] SC4-05: Weekly readings recorded with expandable row view
- [ ] SC4-06: Auto-determination prompts work for Positive/Negative
- [ ] SC4-07: Backend tests achieve >80% coverage
- [ ] SC4-08: Frontend tests achieve >70% coverage

---

## Dependencies

- Requires Stage 3 (Sample Storage Assignment) samples in "Stored" or
  "Processing" status
- Uses existing `TbCultureReading` entity (from M1)
- Reuses `SampleItem` for sample tracking (consistent with existing TB entities)
