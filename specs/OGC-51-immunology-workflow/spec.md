# Feature Specification: Immunology Laboratory Workflow

**Feature Branch**: `OGC-51` **Created**: 2025-12-07 **Status**: Draft
**Input**: User description from `laboratory.md` - Immunology Laboratory 9-step
workflow

## Executive Summary

Immunology laboratories process hundreds of samples per batch through a
multi-step workflow from reception to archiving. Currently, OpenELIS Global has
a Notebook system with Pages that can represent workflow steps, but lacks:

- **Per-sample-per-page tracking**: Cannot track which samples have completed
  each workflow step
- **Bulk operations UI**: No efficient way to process 100+ samples
  simultaneously
- **Branching workflows**: No support for routing samples to different
  destinations (internal analysis, external lab, storage)
- **Parent-child sample linking**: Limited traceability between parent samples
  and derived child samples
- **Analyzer result import**: No bulk import of results from laboratory
  analyzers

This feature extends the existing Notebook/Page architecture to support the
complete Immunology Laboratory workflow with bulk sample processing
capabilities.

**Target Users**: Laboratory technicians, supervisors, quality managers, data
management team

**Expected Impact**:

- Reduce per-sample processing time from 5+ minutes to <30 seconds (via bulk
  operations)
- Achieve 100% sample traceability from reception to archiving
- Eliminate manual transcription errors through analyzer result import
- Enable real-time progress visibility across 200+ sample batches

## Clarifications

### Session 2025-12-08

- Q: How should the NotebookEntry entity be handled - is it new or existing? →
  A: NotebookEntry already exists in the codebase; spec documents existing
  entity, no new entity tasks needed (Option A).

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Create Notebook Instance from Template (Priority: P0)

Laboratory supervisors need to create a new workflow instance from the
Immunology template when a new batch of samples arrives, linking all samples to
the notebook for tracking.

**Why this priority**: This is the foundation - without creating a notebook
instance with linked samples, no workflow tracking can occur. Must work before
any other feature.

**Independent Test**: Create notebook instance from template, add 5 samples by
accession number, verify all samples appear in the notebook's sample list.

**Acceptance Scenarios**:

1. **Given** an "Immunology Laboratory Workflow" template exists with 9 pages,
   **When** a supervisor clicks "New Instance" and enters batch information,
   **Then** a new notebook instance is created with all 9 pages copied from
   template
2. **Given** a new notebook instance is created, **When** the user searches for
   samples by accession number and selects 200 samples, **Then** all 200 samples
   are linked to the notebook
3. **Given** a notebook instance with linked samples, **When** viewing the
   WORKFLOW tab, **Then** each page shows "0/200 samples completed" progress
   indicator
4. **Given** a user imports a CSV manifest file, **When** the import completes,
   **Then** all samples from the CSV are added to the notebook with their
   manifest IDs preserved

---

### User Story 2 - Sample Reception & Creation (Priority: P1)

Laboratory technicians need to either (a) link existing samples to the notebook,
or (b) create new project samples from a manifest CSV when samples arrive in
bulk from research projects that don't have clinical orders.

**Why this priority**: Sample reception is the first workflow step. All
subsequent steps depend on samples being properly received and identified. Must
support both clinical (pre-existing) and research (new) sample workflows.

**Independent Test**: Import manifest CSV with 4 rows (num_of_samples: 10, 15,
20, 5), verify 50 total SampleItem records created with correct external_id
pattern (GRP-001-001 through GRP-001-010 for first row, etc.), sample_type, and
collection_date from CSV.

**Acceptance Scenarios**:

1. **Given** Page 1 "Sample Reception" is open, **When** the user selects "Link
   Existing Samples" and searches by accession number prefix, **Then** matching
   samples are displayed and can be added to the notebook
2. **Given** Page 1 is open, **When** the user selects "Create New Project
   Samples" and uploads a manifest CSV, **Then** a preview shows columns mapped
   to fields
3. **Given** a manifest CSV contains columns (group_id, sample_type,
   collection_date, volume, num_of_samples, notes), **When** the user maps
   columns and clicks "Create Samples", **Then** SampleItem records are created:
   - num_of_samples SampleItems created per row (e.g., row with
     num_of_samples=50 creates 50 SampleItems)
   - external_id = group_id + sequential suffix (e.g., GRP-001-001, GRP-001-002,
     ...)
   - All samples in group share: type_of_sample, collection_date, volume from
     row
4. **Given** a manifest has 4 rows with num_of_samples of 50, 50, 50, 50,
   **When** import completes, **Then** 200 total SampleItems are created (50 per
   group)
5. **Given** 200 samples are created from manifest, **When** viewing the sample
   grid, **Then** all samples appear linked to the notebook with "Pending
   Verification" status, grouped by their group_id
6. **Given** samples are in the grid, **When** the user clicks "Select All" and
   "Mark Verified", **Then** all selected samples are marked as verified
7. **Given** a manifest row has invalid sample_type, **When** import is
   attempted, **Then** that row is flagged as error with message "Unknown sample
   type: XYZ" and no samples from that row are created
8. **Given** all samples are verified, **When** the user clicks "Mark Page
   Complete", **Then** Page 1 shows as completed and Page 2 becomes active

---

### User Story 3 - Bulk Data Entry for Initial Processing (Priority: P1)

Laboratory technicians need to record volume, cell count, and isolation method
for each sample, with the ability to apply common values to multiple samples at
once.

**Why this priority**: Initial processing generates core sample data. Bulk entry
is essential for handling 200+ samples efficiently.

**Independent Test**: Select 10 samples, apply common values (Volume: 5.0mL,
Method: Ficoll), verify all 10 samples updated in database.

**Acceptance Scenarios**:

1. **Given** Page 2 "Initial Processing" is open, **When** the user enters
   Volume "5.0", Cell Count "2.4×10⁶", Method "Ficoll" in the bulk apply form
   and clicks "Apply to Selected", **Then** all selected samples receive these
   values
2. **Given** a sample grid is displayed, **When** the user clicks on a cell
   (e.g., Volume for sample IMM-P-2024-0003), **Then** the cell becomes editable
   for inline data entry
3. **Given** 150 of 200 samples have complete data, **When** viewing Page 2,
   **Then** progress shows "150/200 processed" with a progress bar
4. **Given** the user filters by "Pending" status, **When** the filter is
   applied, **Then** only the 50 incomplete samples are displayed
5. **Given** all samples have complete processing data, **When** the user clicks
   "Mark Page Complete", **Then** Page 2 shows as completed

---

### User Story 4 - Child Sample Creation with Destination Routing (Priority: P1)

Laboratory technicians need to create child samples from parent samples and
route them to different destinations (internal analysis, external lab, or
storage). Samples routed for internal analysis need automatic box/well
assignment with coordinates to enable direct correlation with analyzer results.

**Why this priority**: This is the critical branching point where samples split
into different processing paths. Storage integration is required here. Automatic
well assignment is critical for efficient analyzer result import.

**Independent Test**: Create 96 child samples, route to internal analysis with
box assignment, verify samples are assigned to wells A1-H12 in sequential order.

**Acceptance Scenarios**:

1. **Given** parent samples exist on Page 4, **When** the user clicks "Generate
   Child IDs" with prefix "IMM-C-2024-", **Then** each parent receives a linked
   child sample with the new ID
2. **Given** child samples are created, **When** the user selects 75 samples,
   chooses an analysis box (96-well plate), and clicks "Route to Internal
   Analysis", **Then**:
   - Samples are assigned to box wells sequentially (A1, A2, ..., A12, B1, ...)
   - SampleStorageAssignment records created with box_id and position_coordinate
   - Samples marked for internal processing and will appear on Page 5
3. **Given** a 96-well plate box is selected for internal analysis, **When** 75
   samples are routed, **Then** wells A1 through G3 are assigned (row-major
   order) and wells G4 through H12 remain available
4. **Given** child samples are created, **When** the user selects 25 samples and
   clicks "Route to External Lab", **Then** those samples are marked for
   external shipment with lab destination recorded (no well assignment needed)
5. **Given** child samples are created, **When** the user selects 100 samples,
   chooses a storage location, and clicks "Route to Storage", **Then**
   SampleStorageAssignment records are created linking samples to the location
6. **Given** the routing table shows all samples, **When** filtering by
   "Unrouted", **Then** only samples without a destination assignment are shown
7. **Given** a child sample "IMM-C-2024-0001" is created from parent
   "IMM-P-2024-0001", **When** viewing either sample, **Then** the parent-child
   relationship is displayed with clickable navigation between them
8. **Given** samples are assigned to wells in a box, **When** viewing the box
   layout, **Then** a visual grid shows which wells are occupied and by which
   sample ID

---

### User Story 5 - Analyzer Result Import (Priority: P2)

Laboratory technicians need to import bulk results from ELISA or Flow Cytometry
analyzers, mapping result columns to sample IDs automatically.

**Why this priority**: Manual result entry for 200+ samples is error-prone and
time-consuming. Analyzer import eliminates transcription errors.

**Independent Test**: Upload analyzer CSV with 10 results, verify column mapping
UI works, confirm all 10 results imported to correct samples.

**Acceptance Scenarios**:

1. **Given** Page 6 "Main Analysis Execution" is open, **When** the user uploads
   an analyzer export file (CSV/Excel), **Then** the column mapping interface is
   displayed
2. **Given** an analyzer file is uploaded, **When** the user maps "Well_ID"
   column to "Child Sample ID" and selects result columns (CD4+, CD8+), **Then**
   a preview shows matched samples and their values
3. **Given** the preview shows 72 valid matches and 3 errors, **When** the user
   clicks "Import All Valid", **Then** 72 results are saved and 3 are flagged
   for review
4. **Given** a sample has an import error, **When** the user clicks "Review
   Errors", **Then** the error details are shown (e.g., "Sample ID not found",
   "Value out of range")
5. **Given** all results are imported, **When** viewing the sample grid,
   **Then** each sample shows its imported values with the assay run ID and
   timestamp

---

### User Story 6 - Post-Analysis Storage Assignment (Priority: P2)

Laboratory technicians need to assign processed samples to storage locations
with defined conditions and retention periods.

**Why this priority**: Proper storage assignment is required for sample
preservation and future retrieval. Integrates with existing storage system.

**Independent Test**: Select 10 samples, assign to storage location with
retention period, verify SampleStorageAssignment and SampleStorageMovement
records created.

**Acceptance Scenarios**:

1. **Given** Page 7 "Post-Analysis Handling" is open, **When** the user selects
   a storage condition (Frozen -20°C), retention period (6 months), and location
   (Freezer-2 > Shelf-B > Rack-1), **Then** the configuration is ready to apply
2. **Given** storage configuration is set, **When** the user selects 75 samples
   and clicks "Apply to Selected", **Then** all 75 samples receive storage
   assignments with movement audit records
3. **Given** samples are assigned to storage, **When** viewing the sample grid,
   **Then** each sample shows its storage location, condition, and retention
   date
4. **Given** a sample already has a storage assignment from Page 4, **When**
   assigning a new location on Page 7, **Then** a SampleStorageMovement record
   is created showing the location change

---

### User Story 7 - Result Compilation and Export (Priority: P2)

Laboratory staff need to compile results, flag invalid samples, and export
reports for the data management team.

**Why this priority**: Results must be compiled and delivered to complete the
workflow. Flagging ensures data quality.

**Independent Test**: Flag 3 samples as invalid with reasons, generate Excel
report, verify flagged samples are marked in report.

**Acceptance Scenarios**:

1. **Given** Page 8 "Result Compilation" is open, **When** viewing the summary,
   **Then** total analyzed, valid, and invalid/inconclusive counts are displayed
2. **Given** a sample has inconclusive results, **When** the user selects the
   sample and enters a flag reason, **Then** the sample is marked as flagged
   with the reason recorded
3. **Given** results are ready for export, **When** the user selects "Excel"
   format and clicks "Generate Report", **Then** an Excel file is created with
   all sample results, flags, and metadata
4. **Given** a report is generated, **When** the user clicks "Attach to
   Notebook", **Then** the file is saved to the notebook's file attachments
5. **Given** a report is delivered, **When** the user marks delivery complete,
   **Then** the delivery timestamp and recipient are recorded

---

### User Story 8 - End of Project Archiving (Priority: P3)

Laboratory supervisors need to transfer all samples to the biorepository and
verify complete traceability before closing the project.

**Why this priority**: Archiving is the final step. Lower priority because it
happens once at project end, not during active processing.

**Independent Test**: Transfer 10 samples to biorepository location, verify
traceability checklist items, mark page complete.

**Acceptance Scenarios**:

1. **Given** Page 9 "End of Project Archiving" is open, **When** the user marks
   "Project Concluded" with a date, **Then** the archiving workflow is initiated
2. **Given** samples need archiving, **When** the user selects biorepository
   destination and clicks "Transfer All", **Then** SampleStorageMovement records
   are created for all samples with new location
3. **Given** the traceability checklist is displayed, **When** the user verifies
   each item (parent-child links, movement history, permanent storage), **Then**
   checkboxes are recorded in the notebook
4. **Given** all samples are archived and traceability verified, **When** the
   user clicks "Mark Page Complete", **Then** Page 9 shows complete and notebook
   status can be changed to FINALIZED
5. **Given** all 9 pages are complete, **When** the supervisor clicks "Submit
   for Final Review", **Then** notebook status changes to SUBMITTED for approval

---

### Edge Cases

- What happens when a sample fails verification but other samples proceed?
  **System MUST allow partial page completion - failed samples remain pending
  while others advance. Page shows "195/200 complete" with 5 pending.**
- How does the system handle samples routed to external labs? **System MUST
  track external routing as a destination type. External samples skip Pages 5-7
  and rejoin at Page 8 when results are received.**
- What happens if analyzer import has duplicate sample IDs? **System MUST reject
  duplicates with error message "Duplicate Sample ID: IMM-C-2024-0001 appears on
  rows 15 and 42".**
- How should the system handle partial analyzer results? **System MUST allow
  partial import - samples with valid results are imported, others flagged for
  manual entry.**
- What happens when a child sample is created but parent has no remaining
  volume? **System MUST warn but allow creation - volume tracking is
  informational for this workflow, not blocking.**
- How does the system handle concurrent users on the same notebook? **System
  MUST use optimistic locking on notebook_page_sample records. Conflicting
  updates show "Data modified by another user - please refresh".**
- What happens if storage location is at capacity when assigning samples?
  **System MUST show warning but allow assignment per existing storage behavior
  (soft capacity limits).**

## Requirements _(mandatory)_

### Functional Requirements

#### Notebook & Page Management

- **FR-001**: System MUST support creating notebook instances from templates
  with all pages copied
- **FR-002**: System MUST allow linking up to 500 samples to a single notebook
  instance
- **FR-003**: System MUST track page completion status (completed flag) per page
- **FR-004**: System MUST display progress indicators showing samples
  completed/total per page
- **FR-005**: System MUST support 9 configurable pages for the Immunology
  workflow

#### Per-Sample-Per-Page Tracking

- **FR-010**: System MUST create a `notebook_page_sample` record for each
  sample-page combination when samples are linked to notebook
- **FR-011**: System MUST track status per sample-page (PENDING, IN_PROGRESS,
  COMPLETED, SKIPPED)
- **FR-012**: System MUST store page-specific data per sample (e.g., volume,
  cell count) in JSONB field
- **FR-013**: System MUST record which user completed each sample-page and when
- **FR-014**: System MUST support questionnaire responses per sample-page via
  FHIR QuestionnaireResponse UUID

#### Sample Entry (Page 1)

- **FR-020**: System MUST support two sample entry modes: "Link Existing
  Samples" and "Create New Project Samples"
- **FR-021**: For "Link Existing", system MUST allow searching samples by
  accession number or external_id and adding them to the notebook
- **FR-022**: For "Create New", system MUST accept manifest CSV with columns for
  sample group data (group_id, sample_type, collection_date, volume,
  num_of_samples, notes, etc.)
- **FR-023**: System MUST create multiple SampleItem records per manifest row
  based on num_of_samples column:
  - If num_of_samples=50, create 50 SampleItems from that row
  - external_id = group_id + "-" + sequential number (e.g., GRP-001-001,
    GRP-001-002)
  - All samples in group share: type_of_sample, collection_date, volume from row
- **FR-024**: System MUST support num_of_samples values from 1 to 500 per row
- **FR-025**: System MUST validate sample_type values against existing
  TypeOfSample records and flag unknown types as errors (entire row rejected if
  invalid)
- **FR-026**: System MUST link all created/linked samples to the notebook via
  notebook_samples relationship
- **FR-027**: System MUST store the group_id on each SampleItem to enable
  grouping in the UI (samples from same manifest row can be displayed together)

#### Bulk Operations

- **FR-030**: System MUST support "Select All" to select all visible samples in
  grid
- **FR-031**: System MUST support "Apply to Selected" to update common values
  for multiple samples in single transaction
- **FR-032**: System MUST support CSV/Excel import for analyzer results with
  column mapping
- **FR-033**: System MUST process bulk operations in batches of 50 to prevent
  timeout
- **FR-034**: System MUST display progress during bulk operations ("Processing
  50/200...")

#### Sample Grid UI

- **FR-040**: System MUST display paginated sample grid with 25 samples per page
  (configurable: 10, 25, 50, 100)
- **FR-041**: System MUST support inline editing for sample-specific values
- **FR-042**: System MUST support filtering by status (Pending, Completed, All)
- **FR-043**: System MUST support sorting by any column
- **FR-044**: System MUST highlight samples with errors or warnings
- **FR-045**: System MUST use virtualized rendering for grids with 200+ samples

#### Child Sample & Routing

- **FR-050**: System MUST generate child sample IDs with configurable prefix and
  sequential numbering
- **FR-051**: System MUST maintain parent-child relationship in sample_item
  table
- **FR-052**: System MUST support three routing destinations: Internal Analysis,
  External Lab, Storage
- **FR-053**: System MUST integrate with SampleStorageService for storage
  routing
- **FR-054**: System MUST track external lab destination name and shipment date
- **FR-055**: System MUST display routing status in sample grid
  (Routed/Unrouted)

#### Internal Analysis Box/Well Assignment

- **FR-056**: For Internal Analysis routing, system MUST require selection of an
  analysis box (e.g., 96-well plate, 384-well plate)
- **FR-057**: System MUST auto-assign well coordinates sequentially in row-major
  order (A1, A2, ..., A12, B1, B2, ..., H12 for 96-well)
- **FR-058**: System MUST create SampleStorageAssignment with box_id and
  position_coordinate for each sample routed to Internal Analysis
- **FR-059**: System MUST prevent assignment to already-occupied wells
- **FR-060**: System MUST display visual box layout showing occupied/available
  wells
- **FR-061**: System MUST support common plate formats: 96-well (8x12), 384-well
  (16x24), 48-well (6x8), 24-well (4x6)
- **FR-062**: Well coordinates MUST use standard format: row letter + column
  number (e.g., A1, B12, H6)

#### Analyzer Integration

- **FR-070**: System MUST accept CSV and Excel file uploads for analyzer results
- **FR-071**: System MUST display column mapping interface with preview
- **FR-072**: System MUST match samples by well coordinate (e.g., A1, B12) to
  correlate analyzer output with sample IDs via box assignment
- **FR-073**: System MUST support matching by sample external_id as fallback
- **FR-074**: System MUST validate result values against expected ranges
- **FR-075**: System MUST import valid results and flag errors for review
- **FR-076**: System MUST record assay run ID, analyzer, operator, and timestamp

#### Result Compilation

- **FR-080**: System MUST calculate summary statistics (total, valid, invalid)
- **FR-081**: System MUST support flagging samples as invalid/inconclusive with
  reason
- **FR-082**: System MUST generate Excel/PDF/CSV export reports
- **FR-083**: System MUST attach generated reports to notebook files
- **FR-084**: System MUST record delivery recipient and timestamp

#### Archiving & Traceability

- **FR-090**: System MUST support bulk transfer to biorepository location
- **FR-091**: System MUST verify parent-child links for all samples
- **FR-092**: System MUST verify complete movement history for all samples
- **FR-093**: System MUST display traceability verification checklist
- **FR-094**: System MUST support notebook status transition to FINALIZED

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
- **CR-005**: @Transactional annotations MUST only be used in service layer (NOT
  in controllers)
- **CR-006**: Services MUST compile all data within transaction boundaries to
  prevent LazyInitializationException
- **CR-007**: Security: RBAC for notebook operations, audit trail (sys_user_id +
  lastupdated), input validation
- **CR-008**: Tests MUST be included (unit + integration + E2E per feature, >70%
  coverage goal)
- **CR-009**: Individual E2E tests MUST be runnable independently during
  development (NOT full suite)

### Key Entities _(mandatory)_

#### New Entities

- **NotebookPageSample**: Junction entity linking notebook pages to samples with
  per-sample status tracking. Key attributes: notebook_page_id (FK),
  sample_item_id (FK), status (enum: PENDING, IN_PROGRESS, COMPLETED, SKIPPED),
  data (JSONB for page-specific values), questionnaire_response_uuid (UUID),
  completed_by (FK to system_user), completed_at (timestamp). Unique constraint
  on (notebook_page_id, sample_item_id).

- **AnalyzerResultImport**: Tracks bulk result imports from analyzers. Key
  attributes: id, notebook_page_id (FK), analyzer_id (FK), file_name,
  import_date, imported_by (FK), total_rows, successful_rows, failed_rows,
  column_mapping (JSONB). The column_mapping JSONB structure:

  ```json
  {
    "wellCoordinate": "Well", // CSV column name for well coordinate (A1, B2)
    "sampleId": "Sample_ID", // CSV column name for sample external_id (fallback)
    "resultColumns": ["CD4_Pct", "CD8_Pct", "Ratio"], // Result column names to import
    "boxId": 42 // Target box ID for well coordinate matching
  }
  ```

- **SampleRouting**: Tracks sample destination routing for branching workflows.
  Key attributes: id, sample_item_id (FK), notebook_id (FK), destination_type
  (enum: INTERNAL_ANALYSIS, EXTERNAL_LAB, STORAGE), external_lab_name,
  shipment_date, storage_assignment_id (FK to sample_storage_assignment).

- **NotebookEntry** _(EXISTING - already implemented)_: Represents an active
  workflow instance created from a NoteBook template. Key attributes: id,
  notebook_id (FK to template), title (optional custom title), status (enum:
  DRAFT, IN_PROGRESS, COMPLETED, CANCELLED), date_created, date_completed,
  technician_id (FK to system_user), creator_id (FK to system_user). One
  NoteBook template can have many NotebookEntry instances. Samples are linked to
  entries via notebook_entry_id. **Note**: Entity, DAO, Service, and Controller
  already exist in codebase - this feature extends/integrates with existing
  implementation.

#### Extended Entities

- **NoteBookPage**: Add `page_type` enum (BATCH_VERIFICATION, BULK_DATA_ENTRY,
  INDIVIDUAL_PROCESSING, BRANCHING, AGGREGATION) to support different UI
  patterns.

- **SampleItem**: Parent-child tracking uses existing
  `SampleItemAliquotRelationship` entity (already implemented in
  001-sample-management feature). This junction table provides rich metadata:
  sequence numbers, quantity transferred, notes, and FHIR UUID. The direct FK
  `parent_sample_item_id` in sample_item provides navigational convenience. Add
  external_id field if not present for manifest ID storage.

### Database Schema Changes

```sql
-- New table: notebook_page_sample
CREATE TABLE notebook_page_sample (
    id SERIAL PRIMARY KEY,
    notebook_page_id INTEGER NOT NULL REFERENCES notebook_page(id) ON DELETE CASCADE,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    data JSONB,
    questionnaire_response_uuid UUID,
    completed_by INTEGER REFERENCES system_user(id),
    completed_at TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_page_sample UNIQUE (notebook_page_id, sample_item_id),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'))
);

CREATE INDEX idx_nps_page_status ON notebook_page_sample(notebook_page_id, status);
CREATE INDEX idx_nps_sample ON notebook_page_sample(sample_item_id);

-- New table: analyzer_result_import
CREATE TABLE analyzer_result_import (
    id SERIAL PRIMARY KEY,
    notebook_page_id INTEGER NOT NULL REFERENCES notebook_page(id),
    analyzer_id INTEGER REFERENCES analyzer(id),
    file_name VARCHAR(255) NOT NULL,
    import_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    imported_by INTEGER NOT NULL REFERENCES system_user(id),
    total_rows INTEGER NOT NULL,
    successful_rows INTEGER NOT NULL,
    failed_rows INTEGER NOT NULL,
    column_mapping JSONB,
    error_details JSONB
);

-- New table: sample_routing
CREATE TABLE sample_routing (
    id SERIAL PRIMARY KEY,
    sample_item_id INTEGER NOT NULL REFERENCES sample_item(id),
    notebook_id INTEGER NOT NULL REFERENCES notebook(id),
    destination_type VARCHAR(30) NOT NULL,
    external_lab_name VARCHAR(255),
    shipment_date DATE,
    storage_assignment_id INTEGER REFERENCES sample_storage_assignment(id),
    routed_by INTEGER REFERENCES system_user(id),
    routed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dest_type CHECK (destination_type IN ('INTERNAL_ANALYSIS', 'EXTERNAL_LAB', 'STORAGE'))
);

CREATE INDEX idx_routing_notebook ON sample_routing(notebook_id);
CREATE INDEX idx_routing_sample ON sample_routing(sample_item_id);

-- Add page_type to notebook_page
ALTER TABLE notebook_page ADD COLUMN page_type VARCHAR(30);
```

## API Endpoints _(mandatory)_

### Sample Entry (Page 1)

```
POST /rest/notebook/{notebookId}/samples/create-from-manifest
  - Create new SampleItem records from manifest CSV (multiple per row)
  - Request: multipart/form-data with file + columnMapping (JSON)
  - Column mapping: {
      groupId: "group_id",           // Used as prefix for external_id
      sampleType: "sample_type",     // Must match existing TypeOfSample
      collectionDate: "collection_date",
      volume: "volume",
      numOfSamples: "num_of_samples" // How many SampleItems to create per row
    }
  - Example CSV row: GRP-001, Whole Blood, 2024-01-15, 5.0, 50, "Batch notes"
    → Creates 50 SampleItems: GRP-001-001, GRP-001-002, ..., GRP-001-050
  - Response: {
      rowsProcessed: 4,
      samplesCreated: 200,
      samplesLinked: 200,
      errors: [{ row: 3, message: "Unknown sample type: InvalidType" }]
    }

POST /rest/notebook/{notebookId}/samples/link
  - Link existing samples to notebook
  - Request: { sampleItemIds: [1, 2, 3] } or { accessionNumbers: ["2024-0001", "2024-0002"] }
  - Response: { linked: 3 }

GET /rest/notebook/{notebookId}/samples/search
  - Search for existing samples to link
  - Query params: ?query=2024-00&limit=50
  - Response: [{ sampleItemId: 1, accessionNumber: "2024-0001", externalId: "...", sampleType: "..." }]
```

### Bulk Operations

```
POST /rest/notebook/bulk/{notebookId}/samples/import
  - Import sample data updates from CSV (for existing linked samples)
  - Request: multipart/form-data with file
  - Response: { updated: 200, errors: [] }

POST /rest/notebook/bulk/page/{pageId}/samples/status
  - Update status for multiple samples on a page
  - Request: { sampleIds: [1,2,3], status: "COMPLETED" }
  - Response: { updated: 3 }

POST /rest/notebook/bulk/page/{pageId}/samples/apply
  - Apply common values to multiple samples
  - Request: { sampleIds: [1,2,3], data: { volume: "5.0", method: "Ficoll" } }
  - Response: { updated: 3 }

GET /rest/notebook/bulk/page/{pageId}/progress
  - Get page completion progress
  - Response: { total: 200, completed: 150, pending: 45, skipped: 5, percentage: 75 }
```

### Analyzer Integration

```
POST /rest/notebook/bulk/page/{pageId}/analyzer-import
  - Import analyzer results (matches by well coordinate or sample ID)
  - Request: multipart/form-data with file, analyzerId, boxId, columnMapping (JSON)
  - Column mapping: {
      wellCoordinate: "Well",      // Primary match: A1, A2, etc. (from box assignment)
      sampleId: "Sample_ID",       // Fallback match: external_id
      resultColumns: ["CD4_Pct", "CD8_Pct", "Ratio"]
    }
  - Example analyzer CSV:
      Well, Sample_ID, CD4_Pct, CD8_Pct, Ratio
      A1, IMM-C-2024-0001, 45.2, 22.1, 2.04
      A2, IMM-C-2024-0002, 38.7, 28.3, 1.37
  - System matches A1 → sample assigned to well A1 in boxId
  - Response: {
      importId: 42,
      total: 75,
      matchedByWell: 72,
      matchedBySampleId: 2,
      failed: 1,
      errors: [{ row: 45, well: "F9", message: "No sample assigned to well F9" }]
    }

GET /rest/notebook/bulk/page/{pageId}/analyzer-import/{importId}
  - Get import details and errors
  - Response: { id: 42, fileName: "...", errors: [...] }
```

### Sample Routing

```
POST /rest/notebook/{notebookId}/samples/route
  - Route samples to destinations
  - Request varies by destinationType:

  For INTERNAL_ANALYSIS (with auto well assignment):
  {
    sampleIds: [1, 2, 3, ...],
    destinationType: "INTERNAL_ANALYSIS",
    boxId: 42,                    // Required: target analysis box/plate
    startingWell: "A1"            // Optional: default is first available well
  }
  - Response: {
      routed: 75,
      assignments: [
        { sampleId: 1, well: "A1" },
        { sampleId: 2, well: "A2" },
        ...
      ]
    }

  For EXTERNAL_LAB:
  {
    sampleIds: [1, 2, 3],
    destinationType: "EXTERNAL_LAB",
    externalLabName: "LabCorp",
    shipmentDate: "2024-01-20"
  }
  - Response: { routed: 3 }

  For STORAGE:
  {
    sampleIds: [1, 2, 3],
    destinationType: "STORAGE",
    locationId: 5,
    locationType: "rack"
  }
  - Response: { routed: 3 }

GET /rest/notebook/{notebookId}/samples/routing
  - Get routing status for all samples
  - Response: [
      { sampleId: 1, destination: "INTERNAL_ANALYSIS", boxId: 42, well: "A1" },
      { sampleId: 2, destination: "STORAGE", location: "Freezer-1 > Rack-2" },
      { sampleId: 3, destination: "EXTERNAL_LAB", labName: "LabCorp" }
    ]

GET /rest/notebook/{notebookId}/box/{boxId}/layout
  - Get visual box layout with sample assignments
  - Response: {
      boxId: 42,
      format: "96-well",
      rows: 8,
      columns: 12,
      wells: [
        { coordinate: "A1", sampleId: 1, externalId: "IMM-C-2024-0001", status: "occupied" },
        { coordinate: "A2", sampleId: 2, externalId: "IMM-C-2024-0002", status: "occupied" },
        { coordinate: "A3", sampleId: null, status: "available" },
        ...
      ]
    }
```

### Child Sample Creation

```
POST /rest/notebook/{notebookId}/samples/create-children
  - Create child samples from parents
  - Request: { parentSampleIds: [1,2,3], idPrefix: "IMM-C-2024-", extractionVolume: 500 }
  - Response: { created: 3, children: [{ parentId: 1, childId: 101, childExternalId: "IMM-C-2024-0001" }, ...] }
```

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory technicians can verify 200 samples in under 5 minutes
  using bulk verification (compared to 30+ minutes individually)
- **SC-002**: Bulk data entry for 200 samples completes in under 10 minutes
  using "Apply to Selected" pattern
- **SC-003**: Analyzer result import for 200 samples completes in under 2
  minutes with zero transcription errors
- **SC-004**: 100% of samples have complete traceability from reception to
  archiving (parent-child links, movement history, page completion records)
- **SC-005**: Progress visibility: supervisors can see real-time completion
  status for any page within 2 seconds
- **SC-006**: System handles 500-sample batches without timeout or performance
  degradation
- **SC-007**: Storage integration: samples routed to storage have valid
  SampleStorageAssignment records in 100% of cases
- **SC-008**: All 9 workflow pages can be completed for a 200-sample batch in
  under 4 hours (compared to 1-2 days with manual tracking)

### Quality Gates

- **QG-001**: All bulk operations complete within 30 seconds for 200 samples
- **QG-002**: Grid rendering remains responsive (<100ms) with 200+ samples using
  virtualization
- **QG-003**: CSV/Excel import handles files up to 10MB without memory issues
- **QG-004**: Concurrent users (up to 5) can work on same notebook without data
  loss
- **QG-005**: All database operations use appropriate indexes (no full table
  scans)

## UI Wireframes _(mandatory)_

### Page Layout - Workflow Tab

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  NOTEBOOK: Immunology Batch #2024-001                          Status: DRAFT   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  [ENTRY DETAILS] [METADATA] [ATTACHMENTS] [WORKFLOW] [COMMENTS] [AUDIT TRAIL]  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  PAGE NAVIGATION (9 pages)                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ [✓ 1. Reception] [● 2. Processing] [○ 3. Assays] [○ 4. Child Samples]  │   │
│  │ [○ 5. Prep] [○ 6. Analysis] [○ 7. Storage] [○ 8. Results] [○ 9. Archive]│   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  CURRENT PAGE: Initial Processing                         Progress: 150/200    │
│  ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                 │
│  Instructions: Perform volume determination, cell count & isolation            │
│  ───────────────────────────────────────────────────────────────────────────── │
│                                                                                 │
│  BULK APPLY                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ Operator: [Dr. Smith ▼]  Volume: [    ]mL  Method: [Ficoll ▼]           │   │
│  │                                                    [Apply to Selected]  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  SAMPLE GRID                        Filter: [All ▼]  Search: [____________]    │
│  [☑ Select All]  Selected: 25                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ ☑ │ Sample ID       │ Volume  │ Cell Count │ Method │ Operator │ Status│   │
│  │ ☑ │ IMM-P-2024-0001 │ 5.2     │ 2.4×10⁶    │ Ficoll │ Dr.Smith │ ✓     │   │
│  │ ☑ │ IMM-P-2024-0002 │ 4.8     │ 2.1×10⁶    │ Ficoll │ Dr.Smith │ ✓     │   │
│  │ ☐ │ IMM-P-2024-0003 │ [     ] │ [        ] │ [    ] │          │ ○     │   │
│  │   │ ...             │         │            │        │          │       │   │
│  │   │ Showing 1-50 of 200                    │        │  [<] [>] │       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  [Save Progress]                                       [☑ Mark Page Complete]  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Analyzer Import Modal

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  IMPORT ANALYZER RESULTS                                              [X]      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Step 1: Upload File                                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  [📎 Choose File]  FACSCanto_Run042.csv ✓                               │   │
│  │  Analyzer: [BD FACSCanto II ▼]   Assay Run ID: [RUN-2024-042        ]  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  Step 2: Map Columns                                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Sample ID Column:  [Well_ID ▼]  →  Match to: [Child Sample ID]        │   │
│  │  Result Columns:    [☑ CD4_Percent] [☑ CD8_Percent] [☑ Ratio]          │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  Step 3: Preview (first 10 rows)                                                │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Sample ID       │ CD4+ %   │ CD8+ %   │ Ratio  │ Status               │   │
│  │  IMM-C-2024-0001 │ 45.2     │ 22.1     │ 2.04   │ ✓ Ready              │   │
│  │  IMM-C-2024-0002 │ 38.7     │ 28.3     │ 1.37   │ ✓ Ready              │   │
│  │  IMM-C-2024-XXX  │ —        │ —        │ —      │ ⚠ Sample not found   │   │
│  │  ...             │          │          │        │                      │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  Summary: 72 valid, 3 errors                                                    │
│                                                                                 │
│  [Cancel]                              [Review Errors]  [Import 72 Valid]      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Sample Routing Panel (Page 4)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  DESTINATION ROUTING                                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────────┐  │
│  │ INTERNAL ANALYSIS│  │ EXTERNAL LAB     │  │ STORAGE                      │  │
│  │                  │  │                  │  │                              │  │
│  │   75 samples     │  │   25 samples     │  │   100 samples                │  │
│  │                  │  │                  │  │   Location: [Freezer-1 ▼]    │  │
│  │ [+ Add Selected] │  │ [+ Add Selected] │  │   [+ Add Selected]           │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────────────────┘  │
│                                                                                 │
│  Selected: 15 samples                                                           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Dependencies

- **Existing**: NoteBook, NoteBookPage, NoteBookSample entities and services
- **Existing**: SampleStorageService for storage integration
- **Existing**: SampleItem, Sample entities
- **Existing**: Analyzer entity for analyzer selection
- **New**: NotebookPageSample entity and DAO/Service
- **New**: Bulk operation endpoints
- **New**: Frontend grid components with virtualization

## Out of Scope

- FHIR Questionnaire integration (use existing questionnaireFhirUuid field)
- Barcode scanning (future enhancement)
- Real-time notifications/websockets
- Mobile-optimized UI
- Multi-language questionnaire content
- Automated analyzer file polling
