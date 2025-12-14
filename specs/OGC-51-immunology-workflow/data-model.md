# Data Model: Immunology Laboratory Workflow

**Date**: 2025-12-07 **Feature**: OGC-51 Immunology Laboratory Workflow

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              EXISTING ENTITIES                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────┐          ┌──────────────┐          ┌──────────────────────┐  │
│  │   NoteBook   │ 1    N   │ NoteBookPage │ 1    N   │  NotebookPageSample  │  │
│  │              │─────────>│              │─────────>│       (NEW)          │  │
│  │ - id         │          │ - id         │          │ - notebook_page_id   │  │
│  │ - title      │          │ - order      │          │ - sample_item_id     │  │
│  │ - status     │          │ - title      │          │ - status             │  │
│  │ - isTemplate │          │ - completed  │          │ - data (JSONB)       │  │
│  └──────┬───────┘          │ - page_type  │          │ - completed_by       │  │
│         │                  └──────────────┘          │ - completed_at       │  │
│         │                                            └──────────┬───────────┘  │
│         │ 1                                                     │ N            │
│         │                                                       │              │
│         ▼ N                                                     ▼ 1            │
│  ┌──────────────┐          ┌──────────────────────┐  ┌──────────────────────┐  │
│  │ NoteBookSamp │          │    SampleRouting     │  │     SampleItem       │  │
│  │              │          │       (NEW)          │  │                      │  │
│  │ - notebook_id│          │ - sample_item_id     │  │ - id                 │  │
│  │ - sample_item│          │ - notebook_id        │  │ - external_id        │  │
│  │ - quest_resp │          │ - destination_type   │  │ - parent_sample_item │  │
│  └──────────────┘          │ - box_id, well       │  │ - quantity           │  │
│                            └──────────────────────┘  └──────────┬───────────┘  │
│                                                                  │              │
│  ┌──────────────┐          ┌──────────────────────┐             │ 1            │
│  │  Analyzer    │ 1    N   │AnalyzerResultImport  │             │              │
│  │              │─────────>│       (NEW)          │             ▼ N            │
│  │ - id         │          │ - notebook_page_id   │  ┌──────────────────────┐  │
│  │ - name       │          │ - analyzer_id        │  │SampleStorageAssignment│ │
│  └──────────────┘          │ - file_name          │  │                      │  │
│                            │ - column_mapping     │  │ - location_id        │  │
│                            │ - successful_rows    │  │ - location_type      │  │
│                            └──────────────────────┘  │ - position_coordinate│  │
│                                                      └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## New Entities

### 1. NotebookPageSample

**Purpose**: Junction entity tracking per-sample status on each notebook page.
Enables workflow progress tracking like "150/200 samples completed on Page 2".

**Table**: `notebook_page_sample`

| Column                        | Type        | Constraints                                         | Description                                             |
| ----------------------------- | ----------- | --------------------------------------------------- | ------------------------------------------------------- |
| `id`                          | SERIAL      | PRIMARY KEY                                         | Auto-increment ID                                       |
| `notebook_page_id`            | INTEGER     | NOT NULL, FK -> notebook_page(id) ON DELETE CASCADE | Parent page                                             |
| `sample_item_id`              | INTEGER     | NOT NULL, FK -> sample_item(id)                     | Linked sample                                           |
| `status`                      | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK                  | PENDING, IN_PROGRESS, COMPLETED, SKIPPED                |
| `data`                        | JSONB       | NULL                                                | Page-specific values (volume, cell_count, method, etc.) |
| `questionnaire_response_uuid` | UUID        | NULL                                                | FHIR QuestionnaireResponse reference                    |
| `completed_by`                | INTEGER     | NULL, FK -> system_user(id)                         | User who completed                                      |
| `completed_at`                | TIMESTAMP   | NULL                                                | Completion timestamp                                    |
| `last_updated`                | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP                           | Audit timestamp                                         |

**Unique Constraint**: `(notebook_page_id, sample_item_id)`

**Indexes**:

- `idx_nps_page_status` ON (notebook_page_id, status)
- `idx_nps_sample` ON (sample_item_id)

**Status Values**:

- `PENDING`: Not yet processed on this page
- `IN_PROGRESS`: Currently being processed
- `COMPLETED`: Successfully completed
- `SKIPPED`: Intentionally skipped (e.g., routed elsewhere)

**Example JSONB Data** (varies by page):

```json
// Page 2 - Initial Processing
{
  "volume": "5.2",
  "cellCount": "2.4×10⁶",
  "method": "Ficoll",
  "operator": "Dr. Smith"
}

// Page 4 - Child Sample Creation
{
  "childSampleId": "IMM-C-2024-0001",
  "extractionVolume": "500"
}
```

---

### 2. AnalyzerResultImport

**Purpose**: Audit trail for bulk result imports from laboratory analyzers.
Tracks import attempts, success/failure counts, and column mappings.

**Table**: `analyzer_result_import`

| Column             | Type         | Constraints                         | Description                  |
| ------------------ | ------------ | ----------------------------------- | ---------------------------- |
| `id`               | SERIAL       | PRIMARY KEY                         | Auto-increment ID            |
| `notebook_page_id` | INTEGER      | NOT NULL, FK -> notebook_page(id)   | Page where import occurred   |
| `analyzer_id`      | INTEGER      | NULL, FK -> analyzer(id)            | Selected analyzer            |
| `file_name`        | VARCHAR(255) | NOT NULL                            | Uploaded file name           |
| `import_date`      | TIMESTAMP    | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When import executed         |
| `imported_by`      | INTEGER      | NOT NULL, FK -> system_user(id)     | User who imported            |
| `total_rows`       | INTEGER      | NOT NULL                            | Total rows in file           |
| `successful_rows`  | INTEGER      | NOT NULL                            | Rows successfully imported   |
| `failed_rows`      | INTEGER      | NOT NULL                            | Rows with errors             |
| `column_mapping`   | JSONB        | NULL                                | User-defined column mappings |
| `error_details`    | JSONB        | NULL                                | Error details per failed row |

**Example column_mapping**:

```json
{
  "wellCoordinate": "Well",
  "sampleId": "Sample_ID",
  "resultColumns": ["CD4_Pct", "CD8_Pct", "Ratio"]
}
```

**Example error_details**:

```json
[
  { "row": 15, "well": "A15", "message": "No sample assigned to well A15" },
  { "row": 42, "sampleId": "UNKNOWN", "message": "Sample ID not found" }
]
```

---

### 3. SampleRouting

**Purpose**: Tracks sample destination routing for branching workflows. Links
samples to their routing destination (internal analysis, external lab, storage).

**Table**: `sample_routing`

| Column                  | Type         | Constraints                               | Description                              |
| ----------------------- | ------------ | ----------------------------------------- | ---------------------------------------- |
| `id`                    | SERIAL       | PRIMARY KEY                               | Auto-increment ID                        |
| `sample_item_id`        | INTEGER      | NOT NULL, FK -> sample_item(id)           | Routed sample                            |
| `notebook_id`           | INTEGER      | NOT NULL, FK -> notebook(id)              | Parent notebook                          |
| `destination_type`      | VARCHAR(30)  | NOT NULL, CHECK                           | INTERNAL_ANALYSIS, EXTERNAL_LAB, STORAGE |
| `external_lab_name`     | VARCHAR(255) | NULL                                      | Lab name for EXTERNAL_LAB routing        |
| `shipment_date`         | DATE         | NULL                                      | Shipment date for EXTERNAL_LAB           |
| `storage_assignment_id` | INTEGER      | NULL, FK -> sample_storage_assignment(id) | For STORAGE routing                      |
| `box_id`                | INTEGER      | NULL, FK -> storage_box(id)               | For INTERNAL_ANALYSIS routing            |
| `well_coordinate`       | VARCHAR(10)  | NULL                                      | Well position for INTERNAL_ANALYSIS      |
| `routed_by`             | INTEGER      | NULL, FK -> system_user(id)               | User who routed                          |
| `routed_at`             | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP                 | When routed                              |

**Indexes**:

- `idx_routing_notebook` ON (notebook_id)
- `idx_routing_sample` ON (sample_item_id)
- `idx_routing_box_well` ON (box_id, well_coordinate) - For analyzer result
  matching

**Destination Types**:

- `INTERNAL_ANALYSIS`: Sample goes to analysis box with well assignment
- `EXTERNAL_LAB`: Sample shipped to external laboratory
- `STORAGE`: Sample goes to storage location

---

## Extended Entities

### 4. NoteBookPage (Extended)

**Change**: Add `page_type` column for UI pattern differentiation.

**New Column**: | Column | Type | Constraints | Description |
|--------|------|-------------|-------------| | `page_type` | VARCHAR(30) | NULL
| UI pattern type |

**Page Types**:

- `BATCH_VERIFICATION`: Bulk verify/mark complete (Pages 1, 9)
- `BULK_DATA_ENTRY`: Apply common values to selected samples (Pages 2, 3)
- `BRANCHING`: Route to different destinations (Page 4)
- `INDIVIDUAL_PROCESSING`: Per-sample operations (Pages 5, 6)
- `AGGREGATION`: Compile and summarize (Pages 7, 8)

**Migration**:

```sql
ALTER TABLE notebook_page ADD COLUMN page_type VARCHAR(30);
```

---

### 5. SampleItem (Extended - Verified Existing)

**Confirmed Fields** (no changes needed):

- `external_id` (String) - Already exists for manifest ID storage
- `parent_sample_item_id` (FK) - Already exists for parent-child relationships
- `fhir_uuid` (UUID) - Already exists for FHIR compliance

---

## Validation Rules

### NotebookPageSample

1. **Status Transition**:

   - PENDING -> IN_PROGRESS (on first edit)
   - IN_PROGRESS -> COMPLETED (on mark complete)
   - IN_PROGRESS -> SKIPPED (on skip/route elsewhere)
   - COMPLETED -> IN_PROGRESS (on reopen for correction)
   - SKIPPED -> PENDING (on cancel skip)

2. **Completion Requirements**:

   - `completed_by` must be set when status = COMPLETED
   - `completed_at` must be set when status = COMPLETED

3. **Unique Constraint**:
   - One record per (notebook_page_id, sample_item_id)

### SampleRouting

1. **Destination Validation**:

   - INTERNAL_ANALYSIS requires: `box_id`, `well_coordinate`
   - EXTERNAL_LAB requires: `external_lab_name`
   - STORAGE requires: `storage_assignment_id`

2. **Well Coordinate Format**:

   - Pattern: `[A-Z][0-9]+` (e.g., A1, H12, P24)
   - Must be valid for box format (e.g., A1-H12 for 96-well)

3. **Unique Well Assignment**:
   - Prevent duplicate (box_id, well_coordinate) per notebook

### AnalyzerResultImport

1. **Row Counts**:

   - `successful_rows + failed_rows == total_rows`

2. **Required Mapping**:
   - At least one of `wellCoordinate` or `sampleId` must be mapped

---

## State Transitions

### Sample Workflow States (per page)

```
                    ┌─────────────────────────────────────────┐
                    │                                         │
                    ▼                                         │
┌─────────┐    ┌─────────────┐    ┌───────────┐    ┌─────────┴─┐
│ PENDING │───>│ IN_PROGRESS │───>│ COMPLETED │    │  SKIPPED  │
└─────────┘    └──────┬──────┘    └───────────┘    └───────────┘
                      │                 ▲
                      │                 │
                      └────────────────-┘
                      (correction workflow)
```

### Notebook Status Transitions

```
┌───────┐    ┌───────────┐    ┌───────────┐    ┌──────────┐
│ DRAFT │───>│ SUBMITTED │───>│ FINALIZED │───>│ ARCHIVED │
└───────┘    └───────────┘    └───────────┘    └──────────┘
                                     │
                                     ▼
                              ┌──────────┐
                              │  LOCKED  │
                              └──────────┘
```

---

## Performance Considerations

### Indexes for Query Optimization

1. **Page Progress Queries**:

   ```sql
   -- Count completed/pending per page
   SELECT status, COUNT(*) FROM notebook_page_sample
   WHERE notebook_page_id = ?
   GROUP BY status;
   ```

   Index: `idx_nps_page_status`

2. **Sample Location Queries**:

   ```sql
   -- Find samples in a specific page with status
   SELECT * FROM notebook_page_sample
   WHERE notebook_page_id = ? AND status = ?
   ORDER BY sample_item_id;
   ```

   Index: `idx_nps_page_status`

3. **Routing Status Queries**:

   ```sql
   -- Find unrouted samples in notebook
   SELECT si.* FROM sample_item si
   JOIN notebook_samples ns ON si.id = ns.sample_item_id
   LEFT JOIN sample_routing sr ON si.id = sr.sample_item_id AND sr.notebook_id = ?
   WHERE ns.notebook_id = ? AND sr.id IS NULL;
   ```

   Index: `idx_routing_notebook`, `idx_routing_sample`

4. **Well Coordinate Lookup** (for analyzer import):
   ```sql
   -- Find sample by box and well
   SELECT sample_item_id FROM sample_routing
   WHERE notebook_id = ? AND box_id = ? AND well_coordinate = ?;
   ```
   Index: `idx_routing_box_well`

### Batch Size Recommendations

- **Bulk status updates**: 50 samples per batch
- **CSV import**: Stream processing, 100 rows per commit
- **Grid rendering**: Virtualized, 50 visible rows, 200+ total

---

## JSONB Data Schemas

### Page 2 - Initial Processing Data

```typescript
interface InitialProcessingData {
  volume?: string; // e.g., "5.2"
  cellCount?: string; // e.g., "2.4×10⁶"
  method?: string; // e.g., "Ficoll"
  operator?: string; // e.g., "Dr. Smith"
}
```

### Page 4 - Child Sample Data

```typescript
interface ChildSampleData {
  childSampleId?: string; // e.g., "IMM-C-2024-0001"
  extractionVolume?: number; // e.g., 500 (microliters)
}
```

### Page 6 - Analysis Results Data

```typescript
interface AnalysisResultData {
  importId?: number; // Reference to analyzer_result_import
  results: {
    [columnName: string]: string | number; // e.g., { "CD4_Pct": "45.2" }
  };
  assayRunId?: string; // e.g., "RUN-2024-042"
  analyzedAt?: string; // ISO timestamp
}
```

### Page 8 - Compilation Data

```typescript
interface CompilationData {
  flag?: string; // "VALID", "INVALID", "INCONCLUSIVE"
  flagReason?: string; // e.g., "Hemolyzed sample"
  compiledBy?: string; // User who reviewed
  compiledAt?: string; // ISO timestamp
}
```
