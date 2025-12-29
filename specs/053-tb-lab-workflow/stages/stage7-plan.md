# Stage 6: Isolate Storage

**Branch**: `feat/053-tb-lab-workflow` | **Date**: 2025-12-24 **Parent Spec**:
`specs/053-tb-lab-workflow/spec.md`

## Overview

Stage 6 handles storage tracking for positive culture isolates. This is distinct
from Stage 3 (raw sample storage) - Stage 6 specifically manages isolates from
culture-positive samples for biorepository compliance and future retrieval.

**Key Workflow:** Culture Positive → Isolate Prepared → Storage Assignment →
Temperature Monitoring → Retrieval (if needed)

## Distinction from Stage 3

| Aspect      | Stage 3: Raw Sample Storage         | Stage 6: Isolate Storage                          |
| ----------- | ----------------------------------- | ------------------------------------------------- |
| Purpose     | Temporary storage before processing | Long-term preservation of positive cultures       |
| Samples     | Unprocessed specimens               | Culture isolates (positive)                       |
| Duration    | Hours to days                       | Months to years                                   |
| Temperature | 2-8°C (refrigerated)                | -20°C, -80°C, or LN2 (cryopreservation)           |
| Tracking    | Simple location assignment          | Full biorepository tracking with temperature logs |

## User Stories

### US6.1 - Isolate Storage Assignment (Priority: P2)

Lab technicians assign culture-positive isolates to cryostorage locations.

**Acceptance Scenarios:**

1. **Given** culture positive sample, **When** assigning storage, **Then**
   hierarchical location (Room/Fridge/Compartment/Rack/Box) selected
2. **Given** location assigned, **Then** storage date, stored by, storage
   conditions (temp range) recorded
3. **Given** isolate stored, **When** viewing sample, **Then** full storage path
   displayed (e.g., "Room A > Fridge-2 > Compartment 1 > Rack 3 > Box 5")
4. **Given** storage conditions selected, **Then** temperature range
   (-20°C/-80°C/LN2) recorded

### US6.2 - Temperature Monitoring (Priority: P2)

Lab technicians log temperature readings for storage units.

**Acceptance Scenarios:**

1. **Given** storage unit (fridge), **When** temperature logged, **Then**
   timestamp + temperature + logged_by captured
2. **Given** temperature out of range, **Then** alert/warning flag set on
   affected isolates
3. **Given** temperature log history, **When** viewing, **Then** last 30 days
   displayed with chart
4. **Given** excursion detected, **Then** affected isolates flagged for review

### US6.3 - Isolate Retrieval (Priority: P3)

Lab technicians record retrieval of stored isolates for additional testing.

**Acceptance Scenarios:**

1. **Given** isolate retrieval requested, **When** recording, **Then** retrieval
   date, retrieved by, reason captured
2. **Given** isolate retrieved, **Then** storage status updated to "Retrieved"
3. **Given** partial retrieval (aliquot), **Then** original isolate remains in
   storage
4. **Given** complete retrieval, **Then** storage position marked as available

---

## Key Entities

### Existing: TbIsolateStorage (enhance)

```java
@Entity
@Table(name = "tb_isolate_storage")
public class TbIsolateStorage extends BaseObject<Integer> {
    private SampleItem sampleItem;            // FK to SampleItem (matches codebase pattern)
    private TbCultureReading cultureReading;  // Link to positive culture (via sample_item_id)

    // Location hierarchy
    private String room;
    private String fridgeId;
    private String compartment;
    private String rack;
    private String box;
    private String position;              // e.g., "A1", "B3"

    // Storage details
    private StorageCondition condition;   // MINUS_20, MINUS_80, LIQUID_N2
    private Date storageDate;
    private SystemUser storedBy;

    // Retrieval
    private Date retrievalDate;
    private SystemUser retrievedBy;
    private String retrievalReason;
    private StorageStatus status;         // STORED, RETRIEVED, ALIQUOTED

    private Timestamp lastupdated;
}
```

### New: TbTemperatureLog

```java
@Entity
@Table(name = "tb_temperature_log")
public class TbTemperatureLog extends BaseObject<Integer> {
    private String fridgeId;              // References storage unit
    private String room;
    private Date logDate;
    private LocalTime logTime;
    private BigDecimal temperature;       // In Celsius
    private BigDecimal targetMin;         // Expected min (-82°C for -80 freezer)
    private BigDecimal targetMax;         // Expected max (-78°C for -80 freezer)
    private Boolean excursion;            // True if out of range
    private SystemUser loggedBy;
    private String notes;
    private Timestamp lastupdated;
}
```

---

## Database Schema

### Enhance tb_isolate_storage

```sql
-- If table exists, alter; otherwise create
CREATE TABLE IF NOT EXISTS tb_isolate_storage (
    id SERIAL PRIMARY KEY,
    notebook_page_sample_id INTEGER NOT NULL REFERENCES notebook_page_sample(id),
    culture_reading_id INTEGER REFERENCES tb_culture_reading(id),

    room VARCHAR(50) NOT NULL,
    fridge_id VARCHAR(50) NOT NULL,
    compartment VARCHAR(50),
    rack VARCHAR(50),
    box VARCHAR(50),
    position VARCHAR(10),

    storage_condition VARCHAR(20) NOT NULL CHECK (storage_condition IN ('MINUS_20', 'MINUS_80', 'LIQUID_N2')),
    storage_date DATE NOT NULL,
    stored_by INTEGER REFERENCES system_user(id),

    retrieval_date DATE,
    retrieved_by INTEGER REFERENCES system_user(id),
    retrieval_reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'STORED' CHECK (status IN ('STORED', 'RETRIEVED', 'ALIQUOTED')),

    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_isolate_storage_nps ON tb_isolate_storage(notebook_page_sample_id);
CREATE INDEX idx_isolate_storage_location ON tb_isolate_storage(room, fridge_id);
CREATE INDEX idx_isolate_storage_status ON tb_isolate_storage(status);
```

### tb_temperature_log

```sql
CREATE TABLE tb_temperature_log (
    id SERIAL PRIMARY KEY,
    room VARCHAR(50) NOT NULL,
    fridge_id VARCHAR(50) NOT NULL,
    log_date DATE NOT NULL,
    log_time TIME NOT NULL,
    temperature DECIMAL(5,2) NOT NULL,
    target_min DECIMAL(5,2),
    target_max DECIMAL(5,2),
    excursion BOOLEAN DEFAULT FALSE,
    logged_by INTEGER REFERENCES system_user(id),
    notes TEXT,
    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_temp_log_fridge ON tb_temperature_log(room, fridge_id);
CREATE INDEX idx_temp_log_date ON tb_temperature_log(log_date);
CREATE INDEX idx_temp_log_excursion ON tb_temperature_log(excursion);
```

---

## Implementation Tasks

### Backend Tasks

### T6-01: Liquibase for tb_isolate_storage enhancements

**File**: `src/main/resources/liquibase/3.4.x.x/037-tb-isolate-storage.xml`
_Note: Stage 5 uses 035-036, Stage 6 starts at 037_

### T6-02: Liquibase for tb_temperature_log

**File**: `src/main/resources/liquibase/3.4.x.x/038-tb-temperature-log.xml`

### T6-03: Update base.xml

**File**: `src/main/resources/liquibase/3.4.x.x/base.xml`

### T6-04: TbEnums enhancement

**File**: `src/main/java/org/openelisglobal/tb/valueholder/TbEnums.java`

Add enums:

- `StorageCondition` (MINUS_20, MINUS_80, LIQUID_N2)
- `StorageStatus` (STORED, RETRIEVED, ALIQUOTED)

### T6-05: TbIsolateStorage valueholder

**File**:
`src/main/java/org/openelisglobal/tb/valueholder/TbIsolateStorage.java`

Full entity with:

- All location fields
- Storage condition
- Retrieval fields
- Helper method `getLocationPath()` → "Room A > Fridge-2 > ..."

### T6-06: TbTemperatureLog valueholder

**File**:
`src/main/java/org/openelisglobal/tb/valueholder/TbTemperatureLog.java`

Entity with:

- Auto-compute `excursion` based on temperature vs target range
- `@PrePersist` to validate temperature logic

### T6-07: TbIsolateStorageDAO

**Files**:

- `src/main/java/org/openelisglobal/tb/dao/TbIsolateStorageDAO.java`
- `src/main/java/org/openelisglobal/tb/daoimpl/TbIsolateStorageDAOImpl.java`

Methods:

- `getByNotebookPageSampleId(Integer npsId)`
- `getByLocation(String room, String fridgeId)`
- `getStoredIsolates(Integer entryId)`
- `getAvailablePositions(String room, String fridgeId, String box)`

### T6-08: TbTemperatureLogDAO

**Files**:

- `src/main/java/org/openelisglobal/tb/dao/TbTemperatureLogDAO.java`
- `src/main/java/org/openelisglobal/tb/daoimpl/TbTemperatureLogDAOImpl.java`

Methods:

- `getLogsByFridge(String room, String fridgeId, Date from, Date to)`
- `getExcursions(Date from, Date to)`
- `getLatestLog(String room, String fridgeId)`

### T6-09: TbIsolateStorageService

**Files**:

- `src/main/java/org/openelisglobal/tb/service/TbIsolateStorageService.java`
- `src/main/java/org/openelisglobal/tb/service/TbIsolateStorageServiceImpl.java`

Methods:

- `assignStorage(TbIsolateStorage storage)`
- `retrieveIsolate(Integer storageId, Date retrievalDate, String reason)`
- `getStorageLocationSummary()` - counts by location
- `getAffectedByExcursion(String room, String fridgeId, Date excursionDate)`

### T6-10: TbTemperatureLogService

**Files**:

- `src/main/java/org/openelisglobal/tb/service/TbTemperatureLogService.java`
- `src/main/java/org/openelisglobal/tb/service/TbTemperatureLogServiceImpl.java`

Methods:

- `logTemperature(TbTemperatureLog log)`
- `getTemperatureChart(String fridgeId, int days)` - for chart display
- `checkExcursion(TbTemperatureLog log)` - returns affected isolates

### T6-11: TbIsolateStorageController

**File**:
`src/main/java/org/openelisglobal/tb/controller/rest/TbIsolateStorageController.java`

Endpoints:

- `POST /rest/tb/isolate-storage` - assign storage
- `GET /rest/tb/isolate-storage/{npsId}` - get storage details
- `PUT /rest/tb/isolate-storage/{id}/retrieve` - record retrieval
- `GET /rest/tb/isolate-storage/summary` - location summary

### T6-12: TbTemperatureLogController

**File**:
`src/main/java/org/openelisglobal/tb/controller/rest/TbTemperatureLogController.java`

Endpoints:

- `POST /rest/tb/temperature-log` - log temperature
- `GET /rest/tb/temperature-log/{fridgeId}` - get logs
- `GET /rest/tb/temperature-log/excursions` - get excursion alerts

### T6-13: ORM Validation Tests

**Files**:

- `src/test/java/org/openelisglobal/tb/TbIsolateStorageOrmTest.java`
- `src/test/java/org/openelisglobal/tb/TbTemperatureLogOrmTest.java`

### T6-14: Service Unit Tests

**Files**:

- `src/test/java/org/openelisglobal/tb/service/TbIsolateStorageServiceTest.java`
- `src/test/java/org/openelisglobal/tb/service/TbTemperatureLogServiceTest.java`

### T6-14b: Controller Integration Tests (GAP FIX)

**Files**:

- `src/test/java/org/openelisglobal/tb/controller/TbIsolateStorageControllerTest.java`
- `src/test/java/org/openelisglobal/tb/controller/TbTemperatureLogControllerTest.java`

Test scenarios for IsolateStorageController:

- POST /rest/tb/isolate-storage - assign storage
- GET /rest/tb/isolate-storage/{sampleItemId} - get storage details
- PUT /rest/tb/isolate-storage/{id}/retrieve - record retrieval

Test scenarios for TemperatureLogController:

- POST /rest/tb/temperature-log - log temperature
- GET /rest/tb/temperature-log/{fridgeId} - get logs
- GET /rest/tb/temperature-log/excursions - verify excursion detection

---

### Frontend Tasks

### T6-15: Create TBIsolateStoragePage

**File**: `frontend/src/components/notebook/pages/tb/TBIsolateStoragePage.js`

Carbon page with:

- Sample list (culture positive, not yet stored)
- Storage assignment form with hierarchy selector
- Storage condition dropdown (-20°C/-80°C/LN2)
- Stored isolates table with retrieval action

### T6-16: Create StorageHierarchySelector component

**File**:
`frontend/src/components/notebook/pages/tb/components/StorageHierarchySelector.js`

Reusable hierarchical dropdown:

- Room → Fridge → Compartment → Rack → Box → Position
- Each level filters based on parent selection
- Shows available/occupied counts

### T6-17: Create TemperatureLogPanel component

**File**:
`frontend/src/components/notebook/pages/tb/components/TemperatureLogPanel.js`

Carbon panel with:

- Temperature input form
- Last 7 days chart (using Carbon Charts)
- Excursion warnings

### T6-18: Create IsolateRetrievalModal component

**File**:
`frontend/src/components/notebook/pages/tb/components/IsolateRetrievalModal.js`

Carbon modal with:

- Retrieval date picker
- Reason text area
- Confirm button

### T6-19: Update TBWorkflowTab

**File**: `frontend/src/components/notebook/workflow/TBWorkflowTab.js`

Add Stage 6 page to workflow navigation.

### T6-20: i18n keys

**Files**:

- `frontend/src/languages/en.json`
- `frontend/src/languages/fr.json`

Add keys for:

- Isolate storage labels
- Storage conditions
- Temperature log labels
- Retrieval form labels

### T6-21: SWR hooks

**File**: `frontend/src/components/notebook/pages/tb/hooks/useStage6.js`

Hooks:

- `useIsolateStorage(npsId)`
- `useStoredIsolates(entryId)`
- `useTemperatureLogs(fridgeId)`
- `useAssignStorage()`
- `useRetrieveIsolate()`
- `useLogTemperature()`

### T6-22: Jest tests

**Files**:

- `frontend/src/components/notebook/pages/tb/TBIsolateStoragePage.test.js`
- `frontend/src/components/notebook/pages/tb/components/StorageHierarchySelector.test.js`
- `frontend/src/components/notebook/pages/tb/components/TemperatureLogPanel.test.js`

---

## API Endpoints

### Isolate Storage

```
POST /rest/tb/isolate-storage
Request: {
    notebookPageSampleId: 1,
    cultureReadingId: 10,
    room: "Room A",
    fridgeId: "Fridge-2",
    compartment: "Compartment 1",
    rack: "Rack 3",
    box: "Box 5",
    position: "A1",
    storageCondition: "MINUS_80",
    storageDate: "2025-02-20"
}
Response: {
    id: 1,
    locationPath: "Room A > Fridge-2 > Compartment 1 > Rack 3 > Box 5 > A1",
    status: "STORED"
}

GET /rest/tb/isolate-storage/{npsId}
Response: {
    id: 1,
    locationPath: "Room A > Fridge-2 > ...",
    storageCondition: "MINUS_80",
    storageDays: 45,
    status: "STORED"
}

PUT /rest/tb/isolate-storage/{id}/retrieve
Request: {
    retrievalDate: "2025-04-05",
    retrievalReason: "Additional DST testing required"
}
Response: {
    id: 1,
    status: "RETRIEVED",
    retrievalDate: "2025-04-05"
}
```

### Temperature Log

```
POST /rest/tb/temperature-log
Request: {
    room: "Room A",
    fridgeId: "Fridge-2",
    logDate: "2025-02-20",
    logTime: "09:30",
    temperature: -79.5,
    targetMin: -82,
    targetMax: -78
}
Response: {
    id: 1,
    excursion: false
}

GET /rest/tb/temperature-log/{fridgeId}?days=30
Response: {
    fridgeId: "Fridge-2",
    logs: [
        { date: "2025-02-20", time: "09:30", temperature: -79.5, excursion: false },
        ...
    ],
    excursionCount: 0
}

GET /rest/tb/temperature-log/excursions?from=2025-01-01&to=2025-02-28
Response: [
    {
        room: "Room B",
        fridgeId: "Fridge-4",
        logDate: "2025-02-15",
        temperature: -72.0,
        targetRange: "-82 to -78",
        affectedIsolates: 12
    }
]
```

---

## Success Criteria

- [ ] SC6-01: Isolate storage assigned in <1 minute with full location path
- [ ] SC6-02: Storage location hierarchy displays correctly
- [ ] SC6-03: Temperature logging with excursion detection works
- [ ] SC6-04: Excursion alert shows affected isolates count
- [ ] SC6-05: Retrieval updates storage status correctly
- [ ] SC6-06: Temperature chart displays last 30 days
- [ ] SC6-07: Backend tests achieve >80% coverage

---

## Files Summary

| Category  | File                                                 | Action        |
| --------- | ---------------------------------------------------- | ------------- |
| Liquibase | `liquibase/3.4.x.x/037-tb-isolate-storage.xml`       | CREATE        |
| Liquibase | `liquibase/3.4.x.x/038-tb-temperature-log.xml`       | CREATE        |
| Backend   | `tb/valueholder/TbEnums.java`                        | MODIFY        |
| Backend   | `tb/valueholder/TbIsolateStorage.java`               | CREATE/MODIFY |
| Backend   | `tb/valueholder/TbTemperatureLog.java`               | CREATE        |
| Backend   | `tb/dao/TbIsolateStorageDAO.java`                    | CREATE        |
| Backend   | `tb/dao/TbTemperatureLogDAO.java`                    | CREATE        |
| Backend   | `tb/service/TbIsolateStorageService.java`            | CREATE        |
| Backend   | `tb/service/TbTemperatureLogService.java`            | CREATE        |
| Backend   | `tb/controller/rest/TbIsolateStorageController.java` | CREATE        |
| Backend   | `tb/controller/rest/TbTemperatureLogController.java` | CREATE        |
| Test      | `tb/TbIsolateStorageOrmTest.java`                    | CREATE        |
| Test      | `tb/TbTemperatureLogOrmTest.java`                    | CREATE        |
| Frontend  | `pages/tb/TBIsolateStoragePage.js`                   | CREATE        |
| Frontend  | `pages/tb/components/StorageHierarchySelector.js`    | CREATE        |
| Frontend  | `pages/tb/components/TemperatureLogPanel.js`         | CREATE        |
| Frontend  | `pages/tb/components/IsolateRetrievalModal.js`       | CREATE        |
| Frontend  | `pages/tb/hooks/useStage6.js`                        | CREATE        |

---

## Dependencies

- Requires Stage 4-5 (culture positive samples)
- Uses existing StorageLocation patterns from OGC-68
- May integrate with existing SampleStorageService for location hierarchy
