# Feature Specification: Sample Storage Management

**Feature Branch**: `001-sample-storage`  
**Created**: October 30, 2025  
**Status**: Draft  
**Figma Design**:
[Sample Storage Dashboard](https://www.figma.com/design/4NkWdQf5VoIbJiiEb9EJMg/SSR?node-id=12-3&m=dev)

## Executive Summary

OpenELIS Global laboratories currently have **NO physical storage location
tracking** for biological samples (blood, serum, tissue, etc.). This causes:

- **Sample loss**: 2-5 samples per month cannot be located
- **Retrieval delays**: Lab technicians spend 15-30 minutes searching for
  samples
- **Audit failures**: Cannot prove chain-of-custody for stored samples
  (SLIPTA/ISO accreditation requirement)
- **No capacity visibility**: Cannot monitor freezer/refrigerator utilization

This feature introduces a **5-level storage hierarchy** (Room → Device → Shelf →
Rack → Position) with multi-mode location assignment (cascading dropdowns,
type-ahead search, barcode scanning), sample movement tracking, and
compliance-ready disposal workflows.

**Target Users**: Reception clerks, lab technicians, quality managers, lab
managers

**Expected Impact**:

- Reduce sample retrieval time from 15-30 minutes to <2 minutes
- Eliminate sample loss due to unknown location
- Achieve 95% SLIPTA compliance for storage documentation
- Enable data-driven storage capacity planning

## Clarifications

### Session 2025-10-30

- Q: For this POC, what type of success criteria should we prioritize? → A:
  Feature-complete workflows (can assign, search, move, dispose samples;
  dashboard displays data) with basic correctness validation
- Q: Which user stories are essential for the POC to demonstrate viability? → A:
  P1 + P2A + P2B (Assignment + Search + Movement) - Complete tracking workflow
  demonstrating core value
- Q: What performance expectations should guide the POC implementation? → A:
  Reasonable response times without specific optimization (features work
  smoothly in typical usage, no need for performance tuning or caching)
- Q: What level of testing is appropriate for this POC? → A: Standard test
  coverage per constitution (unit + integration + E2E, >70% coverage target as
  specified)
- Q: Should FHIR integration be included in the POC scope? → A: Full FHIR
  integration per constitution (map entities to FHIR Location resources, sync to
  FHIR server, support IHE mCSD queries)

### Session 2025-11-04

- Q: What are the location creation/management interfaces? → A: Two distinct
  interfaces required: (1) Inline quick creation via widget on orders/results
  pages for immediate location creation during workflow, (2) Dashboard-based
  location management form page accessible via "Add Location" button on Storage
  Dashboard (positioned to right of tabs, next to Export button)
- Q: What are the filter requirements for each dashboard tab table? → A:
  Tab-specific filtering requirements: (1) Samples tab - filter by location and by
  status, (2) Rooms tab - filter by status, (3) Devices tab - filter by type and
  room and status, (4) Shelves tab - filter by device and room and status, (5)
  Racks tab - show room column, filter by room, filter by shelf, device, and
  status

## POC Scope

**In Scope for POC**:

- ✅ **User Story 1 (P1)**: Basic Storage Assignment - All three assignment
  methods (cascading dropdowns, type-ahead search, barcode scan)
- ✅ **User Story 2A (P2)**: Sample Search and Retrieval - Search by sample ID,
  filter by location, display hierarchical paths
- ✅ **User Story 2B (P2)**: Sample Movement - Single and bulk sample movement
  with audit trail

**Deferred to Post-POC**:

- ⏸️ **User Story 3 (P3)**: Sample Disposal with Compliance - Disposal workflow,
  reason/method tracking, audit records
- ⏸️ **User Story 4 (P4)**: Storage Dashboard and Capacity Monitoring - Full
  dashboard with metrics cards, tabs, occupancy visualization, drill-down
  navigation

**Rationale**: POC focuses on demonstrating the core value proposition -
eliminating sample loss and retrieval delays through location tracking. The
assign-search-move workflow proves the concept is viable. Disposal and dashboard
features validate compliance and management needs but can be added once core
tracking is proven.

**Performance Expectations for POC**: Features should work smoothly in typical
usage with reasonable response times (a few seconds for searches/saves). No
specific performance optimization, caching, or load testing required for POC.
Performance tuning can be addressed in production iterations if concept proves
viable.

**Testing Expectations for POC**: Maintain standard OpenELIS test coverage
requirements (unit + integration + E2E tests, >70% coverage goal) even for POC
scope. This ensures quality and makes evolution to production code smoother.

**FHIR Integration for POC**: Full FHIR integration per OpenELIS constitution.
Storage entities (Room, Device, Shelf, Rack, Position) will map to FHIR Location
resources and sync to the FHIR server. Sample-to-location links will use
Specimen.container references. This validates the architectural pattern works
for storage entities and ensures external interoperability from the start.

## User Scenarios & Testing

### User Story 1 - Basic Storage Assignment (Priority: P1 - MVP)

**Actor**: Maria, a reception clerk receiving blood samples

**User Journey**:

Maria receives a blood sample for HIV viral load testing. After completing
standard sample accessioning, she needs to record where the sample will be
stored.

1. Maria completes sample entry in the existing Sample Patient Entry workflow
   (sample ID: S-2025-001, patient info, test orders)
2. In the **Storage Location selector widget** (new), she chooses ONE of three
   assignment methods:
   - **Option A - Cascading dropdowns**: Selects Room ("Main Laboratory") →
     Device ("Freezer Unit 1") → Shelf ("Shelf-A") → Rack ("Rack R1") → Position
     ("A5")
   - **Option B - Barcode scan**: Scans pre-printed rack label → system
     auto-fills "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1" → she
     enters position "A5"
   - **Option C - Type-ahead search**: Types "Freezer Unit 1" → selects from
     filtered results → continues selecting child levels
3. System displays selected path:
   `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5`
4. System validates:
   - Position A5 is not already occupied
   - Freezer Unit 1 is active (not decommissioned)
   - Displays warning if freezer at 80%, 90%, or 100% capacity (assignment still
     allowed)
5. Maria clicks Save → location recorded with her user ID and timestamp

**Why this priority**: This is the foundational capability - without the ability
to assign storage locations, no other workflows can function. This delivers
immediate value by starting to build the location database.

**Independent Test**: Create a sample and assign it to a storage location using
any of the three methods. Verify location is saved correctly with user ID and
timestamp. This standalone capability provides value even before
search/retrieval features exist.

**Acceptance Scenarios**:

1. **Given** Maria has completed sample accessioning for sample S-2025-001,
   **When** she selects storage location "Main Laboratory > Freezer Unit 1 >
   Shelf-A > Rack R1 > Position A5" using cascading dropdowns and clicks Save,
   **Then** system records location with her user ID and current timestamp
2. **Given** position A5 in Rack R1 is already occupied by sample S-2025-002,
   **When** Maria attempts to assign S-2025-001 to the same position, **Then**
   system displays error "Position A5 is already occupied by sample S-2025-002"
   and prevents assignment
3. **Given** Freezer Unit 1 is marked as inactive/decommissioned, **When** Maria
   attempts to assign a sample to any location within it, **Then** system
   displays error "Cannot assign to inactive location" and suggests active
   alternatives
4. **Given** Maria scans a rack barcode "MAIN-FRZ01-SHA-RKR1", **When** barcode
   scan completes, **Then** system auto-populates Room="Main Laboratory",
   Device="Freezer Unit 1", Shelf="Shelf-A", Rack="Rack R1" and focuses the
   Position field
5. **Given** Maria has selected a location, **When** she leaves the Position
   field blank, **Then** system allows rack-level assignment (position optional
   for shelf/rack-level storage)

---

### User Story 2A - Sample Retrieval and Search (Priority: P2)

**Actor**: David, a lab technician preparing to run HIV viral load tests

**User Journey**:

David needs to retrieve sample S-2025-001 from storage to perform viral load
testing.

1. David opens the Storage Dashboard
2. He uses the search bar and types "S-2025-001"
3. Table filters to show matching sample with full details:
   - Sample ID: S-2025-001
   - Type: Blood Serum
   - Status: Active
   - Location:
     `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5`
   - Assigned By: Maria Lopez
   - Date: 2025-01-15 14:32
4. David notes the exact freezer/rack/position from the display
5. He physically retrieves the sample from Freezer Unit 1, Shelf A, Rack R1,
   Position A5
6. Retrieval time: <2 minutes (down from 15-30 minutes manual searching)

**Alternative Flow - Location-based search**:

David knows he needs samples from "Freezer Unit 1" but doesn't have specific
sample IDs:

1. Opens Storage Dashboard, switches to Samples tab
2. Filters by Location: Device="Freezer Unit 1"
3. Views all samples currently in that freezer
4. Uses additional filters (Date Range="Last 7 days", Status="Active") to narrow
   results

**Why this priority**: Search and retrieval is the primary user pain point
(15-30 minute delays). Once samples have assigned locations (P1), this
capability immediately reduces retrieval time and frustration.

**Independent Test**: Assign several samples to different locations. Search for
a specific sample ID and verify the location displays correctly. Physically
retrieve using the displayed location path. Measure time from search to
retrieval (<2 minutes target).

**Acceptance Scenarios**:

1. **Given** sample S-2025-001 is stored at "Main Laboratory > Freezer Unit 1 >
   Shelf-A > Rack R1 > Position A5", **When** David searches for "S-2025-001" in
   the dashboard search bar, **Then** system returns the sample with full
   location details in <2 seconds
2. **Given** 50 samples are stored in Freezer Unit 1, **When** David filters by
   Device="Freezer Unit 1", **Then** system displays all 50 samples with their
   specific positions in <2 seconds
3. **Given** David filters by Date Range="Last 7 days" AND Status="Active",
   **When** results are displayed, **Then** system shows only samples matching
   both criteria (AND logic)
4. **Given** 100,000+ samples exist in the system, **When** David performs any
   search/filter operation, **Then** results appear in <2 seconds
5. **Given** David has applied multiple filters, **When** he clicks "Clear
   Filters", **Then** system resets to show all samples

---

### User Story 2B - Sample Movement Between Locations (Priority: P2)

**Actor**: David, a lab technician preparing samples for testing

**User Journey**:

David needs to move sample S-2025-001 from the -80°C freezer to a 4°C
refrigerator for viral load testing preparation.

1. From the Storage Dashboard (Samples tab) or Sample Detail view, David finds
   sample S-2025-001
2. He clicks the Actions overflow menu (⋮) → selects "Move"
3. Move dialog opens showing:
   - **Current location**:
     `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5`
   - **Target location selector**: Same widget as assignment (cascading
     dropdowns / autocomplete / barcode scan)
4. David selects new location:
   `Main Laboratory > Refrigerator 2 > Shelf-1 > Rack R3 > Position C8`
5. Optionally enters reason: "Temporary storage for viral load testing"
6. System validates:
   - Target position C8 is currently empty (not occupied)
   - Refrigerator 2 is active (not decommissioned)
   - Rack R3 has available capacity
7. David clicks "Move" → System:
   - Updates sample's current location to
     `Refrigerator 2 > Shelf-1 > Rack R3 > Position C8`
   - Frees previous position A5 in Rack R1 (now available for other samples)
   - Records audit trail: Previous location, New location, User (David),
     Timestamp, Reason
8. Dashboard immediately reflects sample under Refrigerator 2

**Bulk Move Scenario**:

David needs to move 5 samples together from Freezer Unit 1 to Refrigerator 2:

1. Selects 5 samples in the dashboard
2. Clicks Actions → "Bulk Move"
3. Selects target parent location:
   `Main Laboratory > Refrigerator 2 > Shelf-1 > Rack R3`
4. System auto-assigns sequential available positions (A1, A2, A3, A4, A5) and
   displays preview
5. David can optionally modify any position assignments before confirming
6. Clicks "Confirm Move" → Each sample receives individual audit record

**Why this priority**: Sample movement is a daily activity (samples move between
temperature zones for testing). Combined with P2A (search), this creates a
complete locate-and-move workflow that addresses the core operational pain
point.

**Independent Test**: Assign a sample to one location, then move it to a
different location. Verify audit trail records the movement with previous
location, new location, user, timestamp, and reason. Verify dashboard updates
immediately. Verify previous position is freed for reuse.

**Acceptance Scenarios**:

1. **Given** sample S-2025-001 is at position A5 in Rack R1, **When** David
   moves it to position C8 in Rack R3 with reason "Testing preparation",
   **Then** system updates current location, frees position A5, creates audit
   record with all details
2. **Given** target position C8 is already occupied by sample S-2025-003,
   **When** David attempts to move S-2025-001 there, **Then** system displays
   error "Position C8 is already occupied by sample S-2025-003" and prevents the
   move
3. **Given** Refrigerator 2 is marked inactive, **When** David attempts to move
   a sample there, **Then** system displays error "Cannot move to inactive
   location" and suggests active alternatives
4. **Given** David moves sample S-2025-001 at 14:32 on Jan 15, **When** movement
   completes, **Then** audit log shows: User=David, Timestamp=2025-01-15 14:32,
   Previous=Rack R1/A5, New=Rack R3/C8, Reason=Testing preparation
5. **Given** David selects 5 samples for bulk move to Rack R3, **When** bulk
   move completes, **Then** system auto-assigns sequential positions (A1-A5),
   David can modify any before confirming, and each sample receives individual
   audit record

---

### User Story 3 - Sample Disposal with Compliance (Priority: P3)

**Actor**: Sarah, a quality manager conducting quarterly sample disposal

**User Journey**:

Sarah needs to dispose of expired TB sputum samples following regulatory
protocols.

1. Sarah opens Storage Dashboard, switches to Samples tab
2. Filters to show expired samples: Status="Active" + Custom Filter="Expiration
   Date < Today"
3. Reviews list of 15 expired TB sputum samples
4. Selects all 15 samples for disposal (single or bulk selection)
5. Clicks Actions → "Dispose"
6. Disposal dialog requires:
   - **Reason** (dropdown): Expired, Contaminated, Patient Request, Testing
     Complete, Other
   - **Method** (dropdown): Biohazard Autoclave, Chemical Neutralization,
     Incineration, Other
   - **Date/Time**: Defaults to current, editable for backdating
   - **Authorization**: System checks Sarah has "Dispose Samples" permission
   - **Notes** (optional): "Quarterly disposal Q1 2025 - TB sputum samples
     expired >6 months"
   - **Attachment** (optional): Upload disposal certificate PDF
7. Sarah confirms disposal → System:
   - Sets sample status to "Disposed" (irreversible)
   - Clears current location (positions freed in all 15 racks)
   - Creates immutable audit record for each sample with all disposal details
   - Prevents future assignment of disposed samples
8. Disposed samples remain viewable for audit purposes but cannot be moved or
   re-assigned

**Why this priority**: While less frequent than daily retrieval/movement,
disposal is critical for regulatory compliance (SLIPTA accreditation). It builds
on P1 and P2 by providing the complete lifecycle: assign → move → dispose.

**Independent Test**: Mark samples as expired, dispose them using the disposal
workflow with reason/method/authorization. Verify samples are marked "Disposed",
positions are freed, audit records are immutable, and disposed samples cannot be
reassigned. Export disposal records to verify compliance documentation.

**Acceptance Scenarios**:

1. **Given** Sarah has "Dispose Samples" permission and selects 15 expired
   samples, **When** she completes disposal with Reason="Expired" and
   Method="Biohazard Autoclave", **Then** all 15 samples are marked "Disposed",
   their positions are freed, and immutable audit records are created
2. **Given** David (lab technician) without disposal permission attempts
   disposal, **When** he clicks "Dispose", **Then** system displays
   "Unauthorized: You do not have permission to dispose samples"
3. **Given** Sarah disposes sample S-2025-001 on Jan 15 at 16:00, **When** she
   views the audit log, **Then** log shows: User=Sarah, Timestamp=2025-01-15
   16:00, Status=Disposed, Reason=Expired, Method=Biohazard Autoclave,
   Notes=[text], Attachment=[PDF link]
4. **Given** sample S-2025-001 is disposed, **When** David attempts to assign it
   to a new location, **Then** system prevents assignment with error "Cannot
   assign disposed sample"
5. **Given** sample S-2025-001 was at position A5 before disposal, **When**
   disposal completes, **Then** position A5 becomes available for new sample
   assignment

---

### User Story 4 - Storage Dashboard and Capacity Monitoring (Priority: P3)

**Actor**: Dr. Johnson, a lab manager monitoring storage utilization

**User Journey**:

Dr. Johnson needs to monitor freezer capacity across the laboratory to plan
procurement of additional storage equipment.

1. Dr. Johnson opens the Storage Dashboard
2. Views **metric cards** at the top:
   - **Total Samples**: 2,847 (count of all samples with assigned locations)
   - **Active**: 2,654 (currently stored and available)
   - **Disposed**: 193 (disposed samples, for record-keeping)
   - **Storage Locations**: 12 rooms (or total capacity count)
3. Switches between **5 tabs** to view different hierarchy levels:

   **Rooms Tab**:

   - Shows: Name | Code | Devices (count) | Samples (count) | Status | Actions
   - Example row: "Main Laboratory" | MAIN | 8 devices | 1,234 samples | Active
     | [⋮]

   **Devices Tab**:

   - Shows: Name | Code | Room | Type (badge) | Occupancy | Status | Actions
   - Example row: "Freezer Unit 1" | FRZ01 | Main Laboratory | [freezer] |
     287/500 (57%) [progress bar] | Active | [⋮]
   - Type badges: "freezer", "fridge", "cabinet" (visual indicators)

   **Shelves Tab**:

   - Shows: Label | Device | Room | Occupancy | Status | Actions
   - Example row: "Shelf-A" | Freezer Unit 1 | Main Laboratory | 23/81 (28%)
     [progress bar] | Active | [⋮]

   **Racks Tab**:

   - Shows: Label | Shelf | Device | Room | Dimensions | Occupancy | Status |
     Actions
   - Example row: "Rack R1" | Shelf-A | Freezer Unit 1 | Main Laboratory | 9 × 9
     | 23/81 (28%) [progress bar] | Active | [⋮]

   **Samples Tab**:

   - Shows: Sample ID | Type | Status | Location (hierarchical path) | Assigned
     By | Date | Actions
   - Example row: S-2025-001 | Blood Serum | Active | Main Laboratory > Freezer
     Unit 1 > Shelf-A > Rack R1 > Pos A5 | Maria Lopez | 2025-01-15 14:32 | [⋮]

4. Uses **occupancy display pattern** to identify capacity issues:

   - Fraction (occupied/total): "287/500"
   - Percentage: "57%"
   - Visual progress bar: Shows 57% filled
   - Color coding (optional): Green (<70%), Yellow (70-90%), Red (>90%)

5. Uses **filters** to analyze specific areas:

   - Filter: Room="Main Laboratory" + Device Type="freezer" → Shows all freezers
     in main lab
   - Filter: Occupancy >80% → Identifies near-capacity devices

6. **Drill-down navigation**:

   - Clicks "Freezer Unit 1" name → Switches to Shelves tab, filtered to show
     only shelves in Freezer Unit 1
   - Clicks "Shelf-A" name → Switches to Racks tab, filtered to Shelf-A
   - Clicks "Rack R1" name → Switches to Samples tab, filtered to Rack R1 (or
     optionally shows grid view)

7. Clicks **Export** button → Downloads filtered data as CSV with all metadata
   for capacity planning report

8. Dr. Johnson identifies that Freezer Unit 1 is at 94% capacity and
   Refrigerator 3 is at 87% capacity. He initiates procurement for additional
   freezer.

**Why this priority**: Dashboard and capacity monitoring enable proactive
management and data-driven decisions. It's less urgent than daily operations
(P1-P2) but critical for long-term efficiency and preventing storage overflow.

**Independent Test**: Create storage hierarchy (rooms, devices, shelves, racks),
assign samples to various locations. Navigate between tabs, verify occupancy
calculations are correct (occupied/total = percentage), use drill-down
navigation, apply filters, and export data to CSV. Verify metrics update when
samples are assigned/moved/disposed.

**Acceptance Scenarios**:

1. **Given** 2,847 samples are assigned to storage locations (2,654 active, 193
   disposed), **When** Dr. Johnson opens the dashboard, **Then** metric cards
   display correct counts
2. **Given** Freezer Unit 1 has 287 samples assigned out of 500 total capacity,
   **When** Dr. Johnson views the Devices tab, **Then** occupancy shows "287/500
   (57%)" with proportional progress bar
3. **Given** Dr. Johnson is viewing the Devices tab, **When** he clicks "Freezer
   Unit 1" name, **Then** system switches to Shelves tab filtered to show only
   shelves within Freezer Unit 1
4. **Given** Dr. Johnson has applied filter "Occupancy >80%", **When** results
   display, **Then** only devices/shelves/racks with >80% occupancy are shown
5. **Given** Dr. Johnson clicks Export with active filters, **When** export
   completes, **Then** CSV file contains only filtered data with all table
   columns plus metadata (within 10 seconds for 10,000 records)
6. **Given** Maria assigns a new sample to Freezer Unit 1, **When** Dr. Johnson
   refreshes the dashboard, **Then** Freezer Unit 1 occupancy increments
   immediately (288/500, 58%)

---

### Edge Cases

- **Concurrent access conflict**: Two reception clerks simultaneously assign
  different samples to the same position A5 in Rack R1. Second clerk receives
  error "Position A5 was just occupied by sample S-2025-XXX. Please select
  another position."

- **Barcode disambiguation**: Lab has two racks with identical labels "R1" in
  different devices. Scanning barcode "MAIN-FRZ01-SHA-R1" vs "MAIN-FRZ02-SHB-R1"
  resolves to distinct racks. If barcode format is ambiguous, system shows
  disambiguation dialog.

- **Deactivating location with active samples**: Admin attempts to deactivate
  Freezer Unit 1 while 287 samples are stored there. System displays warning:
  "Cannot deactivate location with 287 active samples. Move or dispose samples
  first, or deactivate to prevent NEW assignments only."

- **Deleting parent location cascade**: Admin attempts to delete Device "Freezer
  Unit 1" which has 5 child shelves. System prevents deletion with error:
  "Cannot delete location with child locations. Delete or deactivate child
  shelves first."

- **Disposed sample movement attempt**: Technician accidentally attempts to move
  a disposed sample. System prevents action with error: "Cannot move disposed
  sample S-2025-001."

- **Position schema flexibility**: Lab A uses alphanumeric positions (A1, B2),
  Lab B uses numeric (1-1, 2-5), Lab C uses color-coded (RED-01, BLUE-03).
  System accepts all as free text without validation.

- **Shelf/rack-level assignment without position**: Lab uses large baskets on
  shelves (no fixed positions). Reception clerk assigns sample to "Shelf-A"
  without specifying position. System allows blank position field.

- **Capacity threshold warnings**: Rack R1 is configured with 81 total
  positions. System shows warnings at 80% (65th sample), 90% (73rd sample), and
  100% (81st sample) capacity with message "Rack R1 is [percentage]% full.
  Consider using alternative storage." Assignment always allowed (no hard block,
  even at 100%+).

- **Bulk move with insufficient capacity**: User selects 20 samples to bulk move
  to Rack R3 which has only 15 available positions. System displays error:
  "Target rack has only 15 available positions, but 20 samples selected. Reduce
  selection or choose different target."

- **Audit log retention**: System retains disposal audit logs for 7+ years.
  After 7 years, logs remain immutable but may be archived to cold storage (not
  deleted).

- **Barcode generation offline**: Network is down. User generates barcode labels
  for new racks. System queues labels for printing and syncs barcode registry
  when network restored.

- **Search performance with 100,000+ samples**: Dashboard search bar query
  "S-2025" matches 50,000 samples. System paginates results and displays first
  100 in <2 seconds. User can page through or narrow search.

- **Export large dataset**: User exports all 100,000 samples to CSV. System
  processes export in background and provides download link when complete
  (within reasonable time, <1 minute for 100k records).

## Requirements

### Functional Requirements

#### Storage Hierarchy (5 Levels)

- **FR-001**: System MUST support a 5-level storage hierarchy: **Room → Device →
  Shelf → Rack → Position**
- **FR-002**: Each level MUST reference its parent (child-parent relationship
  enforced)
- **FR-003**: Location codes MUST be unique within parent scope (e.g., two
  devices in different rooms can both be "FRZ-01", but two devices in the same
  room cannot)
- **FR-004**: Hierarchical barcode combinations MUST be globally unique across
  the system (e.g., "MAIN-FRZ01-SHA-RKR1" cannot exist twice)

#### Entity Metadata

- **FR-005**: **Room** entity MUST include: Name, unique code, optional
  description, active/inactive status
- **FR-006**: **Device** entity MUST include: Name, unique code (within parent
  room), type (freezer/refrigerator/cabinet/other), optional temperature
  setting, optional capacity limit, active/inactive status, parent room
  reference
- **FR-007**: **Shelf** entity MUST include: Label/number, optional capacity
  limit, active/inactive status, parent device reference
- **FR-008**: **Rack** entity MUST include: Label/ID, dimensions (rows and
  columns as positive integers), optional position schema hint, active/inactive
  status, parent shelf reference
- **FR-009**: **Position** entity MUST include: Free-text coordinate (NO
  enforced format/validation), optional row/column integers for grid
  visualization, occupancy state (empty/occupied), parent rack reference

#### Navigation and Access

- **FR-009a**: Storage management link MUST appear in main side navigation menu
- **FR-009b**: Storage link MUST be positioned directly below "Patients" menu
  item
- **FR-009c**: Storage link MUST be accessible to users with Technician, Lab
  Manager, or Admin roles
- **FR-009d**: Clicking storage link MUST navigate to Storage Dashboard (for P4)
  or storage management page

#### Flexible Position Schema (CRITICAL)

- **FR-010**: Position coordinate MUST accept free text up to 50 characters
  without format validation (supports any naming convention: A1, 1-1, RED-12,
  ZONE-A-03, etc.)
- **FR-011**: System MUST support "shelf-level" or "rack-level" assignment by
  allowing blank position field (position is optional)
- **FR-012**: System MUST store optional row/column integers for grid
  visualization (NOT for validation purposes)
- **FR-013**: System MUST allow duplicate position coordinates across different
  racks (position "A1" can exist in multiple racks)
- **FR-014**: System MUST allow duplicate position coordinates within same rack
  (for flexible storage scenarios like baskets)

#### Rack Dimensions Configuration

- **FR-015**: Users MUST be able to configure rack dimensions as positive
  integers (rows and columns ≥ 1)
- **FR-016**: System MUST allow zero rows/columns to indicate "no grid"
  (shelf-level or rack-level assignment only)
- **FR-017**: System MUST calculate rack capacity as rows × columns (or 0 if no
  grid configured)
- **FR-018**: Rack dimensions MUST be used for: (a) calculating total capacity,
  (b) optional grid visualization, (c) suggesting position coordinates (user can
  override)

#### Multi-Mode Location Selection

- **FR-019**: System MUST provide **cascading dropdown selection**: Select Room
  → Device dropdown populates with devices in that room → Select Device → Shelf
  dropdown populates → Select Shelf → Rack dropdown populates → Select Rack →
  Position field enables (free text entry)
- **FR-020**: System MUST provide **type-ahead autocomplete** as alternative to
  dropdowns: Search locations by name/code, filter results by parent selection,
  support keyboard navigation
- **FR-021**: System MUST provide **barcode scanning** workflow: Scan
  pre-printed barcode label → auto-populate hierarchy fields → focus Position
  field for manual entry
- **FR-022**: System MUST display current selection as hierarchical path below
  selector: `Room > Device > Shelf > Rack > Position`

#### Barcode Format and Handling

- **FR-023**: System MUST support hierarchical barcode format:
  - Device: `{room}-{device}` (e.g., "MAIN-FRZ01")
  - Shelf: `{room}-{device}-{shelf}` (e.g., "MAIN-FRZ01-SHA")
  - Rack: `{room}-{device}-{shelf}-{rack}` (e.g., "MAIN-FRZ01-SHA-RKR1")
- **FR-024**: Scanning rack barcode MUST auto-populate Room, Device, Shelf, Rack
  fields and focus Position field for manual entry
- **FR-025**: System MUST handle duplicate barcode labels with disambiguation
  dialog (e.g., two racks labeled "R1" in different devices)
- **FR-026**: System MUST generate printable labels for Device, Shelf, Rack
  levels including human-readable text and barcode
- **FR-027**: System MUST support printing individual or batch labels

#### Inline Location Creation (Widget-Based)

**Context**: Available within the Storage Location Selector widget on Sample
Patient Entry (orders) and Logbook Results pages to enable quick location
creation during sample assignment workflow.

- **FR-028**: Users MUST be able to create new locations (Room, Device, Shelf,
  Rack) inline from the location selector widget without leaving current
  workflow (Sample Patient Entry or Logbook Results page)
- **FR-029**: Quick-add dialog MUST require minimum information: Identifying
  name/label (required), parent relationship (auto-selected from current
  context), type-specific attributes (e.g., device type, rack dimensions),
  unique code (auto-generated or manually entered)
- **FR-030**: System MUST validate code uniqueness within parent scope before
  saving
- **FR-031**: New location MUST appear immediately in selector dropdown without
  page refresh
- **FR-032**: Failed validation MUST show error inline (e.g., "Code 'FRZ01'
  already exists in room 'Main Laboratory'")

#### Dashboard-Based Location Management

**Context**: Full location management interface accessible from Storage
Dashboard for comprehensive location creation, editing, and management
operations.

- **FR-028a**: Storage Dashboard MUST provide "Add Location" button positioned
  to the right of the tabs (Samples | Rooms | Devices | Shelves | Racks),
  adjacent to the "Export" button
- **FR-028b**: Clicking "Add Location" button MUST navigate to a dedicated
  location management form page
- **FR-028c**: Location management form page MUST support creating new locations
  (Room, Device, Shelf, Rack) with full attribute editing (name, code, type,
  dimensions, capacity, active/inactive status)
- **FR-028d**: Location management form page MUST support editing existing
  locations (update attributes, deactivate/reactivate)
- **FR-028e**: Location management form page MUST validate parent-child
  relationships and code uniqueness within parent scope
- **FR-028f**: Location management form page MUST display hierarchical
  breadcrumb navigation showing current location context within the 5-level
  hierarchy

#### Sample Assignment

- **FR-033**: System MUST record sample assignment with: Sample ID, Location
  (room/device/shelf/rack/position), Assigned By (user ID), Timestamp, Optional
  notes
- **FR-034**: System MUST prevent assignment to already-occupied position
  (unless rack allows duplicates - see FR-014)
- **FR-035**: System MUST prevent assignment to inactive/decommissioned location
- **FR-036**: System MUST display capacity warnings at fixed thresholds: 80%,
  90%, and 100% capacity with message "[Location] is [percentage]% full.
  Consider using alternative storage." System MUST allow assignment even at or
  above 100% capacity (no hard block)
- **FR-037**: System MUST allow assignment at shelf/rack level without
  specifying position (position field blank)

#### Sample Movement

- **FR-038**: Users MUST be able to initiate sample move from dashboard or
  sample detail view
- **FR-039**: Move workflow MUST use same location selector widget as initial
  assignment (dropdown/autocomplete/scan)
- **FR-040**: Move dialog MUST show current location and target location
  selector
- **FR-041**: Users MUST be able to optionally enter reason for move (free text)
- **FR-042**: System MUST validate: Target location is active, target position
  is not occupied, target has available capacity
- **FR-043**: System MUST update sample's current location to new location and
  free previous position (mark as available)
- **FR-044**: System MUST record audit trail with: Previous location, New
  location, User, Timestamp, Reason (if provided)
- **FR-045**: Dashboard MUST update immediately after move completes

#### Bulk Sample Movement

- **FR-046**: Users MUST be able to select multiple samples and move them
  together to same parent location (device/shelf/rack)
- **FR-047**: System MUST auto-assign sequential available positions and display
  preview for review
- **FR-048**: Users MUST be able to manually modify any auto-assigned positions
  before confirming bulk move
- **FR-049**: System MUST create individual audit record for each sample in bulk
  move
- **FR-050**: Bulk move MUST validate capacity before starting (display warning
  if insufficient positions available)

#### Sample Disposal Workflow

- **FR-051**: Disposal dialog MUST require:

  - Reason (dropdown): Expired, Contaminated, Patient Request, Testing Complete,
    Other
  - Method (dropdown): Biohazard Autoclave, Chemical Neutralization,
    Incineration, Other
  - Date/Time (default current timestamp, editable for backdating)
  - Authorization (role-based permission check)
  - Notes (optional free text)
  - Attachment (optional: disposal certificate PDF upload)

- **FR-052**: System MUST set disposed sample status to "Disposed" (irreversible
  state change)
- **FR-053**: System MUST clear disposed sample's current location (position
  becomes available)
- **FR-054**: System MUST create immutable audit record with all disposal
  details
- **FR-055**: System MUST prevent future assignment/movement of disposed samples
- **FR-056**: Disposed samples MUST remain viewable for audit purposes but
  non-editable

#### Dashboard and Reporting

- **FR-057**: Dashboard MUST display 4 metric cards: Total Samples (count of all
  samples with locations), Active (currently stored), Disposed (disposed
  samples), Storage Locations (count of rooms or total capacity)
- **FR-058**: Dashboard MUST provide 5 tabs: Samples | Rooms | Devices | Shelves
  | Racks
- **FR-059**: Each tab MUST show data table appropriate for that entity level
  with relevant columns
- **FR-060**: Tab selection state MUST be visually distinct (active tab
  highlighted)
- **FR-060a**: Dashboard MUST provide action buttons positioned to the right of
  the tabs: "Add Location" button (navigates to location management form page)
  and "Export" button (exports current filtered table data to CSV), both
  visible on all tabs

#### Occupancy Display

- **FR-061**: Devices, Shelves, Racks tabs MUST display occupancy as: Fraction
  (occupied/total) + Percentage + Visual progress bar
- **FR-062**: Occupancy calculation MUST be: (count of occupied positions /
  total capacity) × 100
- **FR-063**: Visual progress bar MUST show proportional fill (e.g., 57% filled
  bar for 287/500)

#### Filters and Search

- **FR-064**: Dashboard MUST provide search by sample ID and location name/code
- **FR-065**: Dashboard MUST provide tab-specific filters:
  - **Samples tab**: Filter by location and by status
  - **Rooms tab**: Filter by status
  - **Devices tab**: Filter by type and room and status
  - **Shelves tab**: Filter by device and room and status
  - **Racks tab**: Filter by room, filter by shelf, device, and status
- **FR-065a**: Dashboard MUST display room column in Racks tab table (shows parent
  room for each rack)
- **FR-066**: Multiple filters MUST combine with AND logic (all criteria must
  match)
- **FR-067**: System MUST provide "Clear Filters" option to reset to show all
  records
- **FR-068**: Search and filter operations MUST return results in <2 seconds
  (even with 100,000+ samples) **[Aspirational - Not validated in POC. POC
  targets reasonable response times without specific optimization.]**

#### Drill-Down Navigation

- **FR-069**: Clicking Room name MUST switch to Devices tab filtered to that
  room
- **FR-070**: Clicking Device name MUST switch to Shelves tab filtered to that
  device
- **FR-071**: Clicking Shelf name MUST switch to Racks tab filtered to that
  shelf
- **FR-072**: Clicking Rack name MUST switch to Samples tab filtered to that
  rack (or optionally show grid view)

#### Data Export

- **FR-073**: Dashboard MUST provide CSV export of current filtered/visible data
- **FR-074**: Export MUST include all table columns plus additional metadata
  (assigned by, timestamps, etc.)
- **FR-075**: Export MUST complete in <10 seconds for 10,000 records
  **[Aspirational - Not validated in POC. Export performance optimization
  deferred to post-POC.]**
- **FR-076**: Export MUST handle large datasets (100,000+ records) via
  background processing with download link notification

#### Grid Visualization (Optional Enhancement)

- **FR-077**: For racks with dimensions configured, system MAY provide optional
  visual grid view
- **FR-078**: Grid view MUST show row/column labels and cell states
  (empty/occupied/reserved)
- **FR-079**: Clicking empty cell MUST allow sample assignment, clicking
  occupied cell MUST show sample details
- **FR-080**: Hovering over cell MUST show sample information tooltip

#### Validation and Safety

- **FR-081**: System MUST prevent assignment to inactive location with clear
  error message
- **FR-082**: System MUST prevent double-occupancy unless rack allows duplicates
  (see FR-014)
- **FR-083**: System MUST validate capacity limits (if configured) before
  allowing assignment
- **FR-084**: Error messages MUST be user-friendly and suggest alternative
  actions when possible
- **FR-085**: System MUST handle concurrent access gracefully: Two users
  attempting simultaneous assignment to same position → Second user receives
  error with current state

#### Audit Trail

- **FR-086**: System MUST record ALL actions: assign, move, dispose
- **FR-087**: Audit log MUST include: User ID, Timestamp, Action type, Previous
  state, New state, Reason (if provided)
- **FR-088**: Audit records MUST be immutable (cannot edit or delete)
- **FR-089**: Audit logs MUST be viewable by users with appropriate permission
- **FR-090**: Audit logs MUST be retained for minimum 7 years (compliance
  requirement)

#### Permissions and Roles

- **FR-091**: System MUST enforce role-based access control:

  - **Lab Technicians**: Assign samples, Move samples
  - **Quality Managers**: All technician permissions + Dispose samples, View
    audit logs, Deactivate locations
  - **Administrators**: All permissions + Create/Edit/Delete locations,
    Configure capacity rules

- **FR-092**: Permissions MUST be enforced on backend (not just UI hiding)
- **FR-093**: Unauthorized actions MUST display clear error message indicating
  missing permission

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - these constraints affect
functional design:_

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks

  - **Functional Impact**: Dashboard tabs, data tables, forms, modals, overflow
    menus will follow Carbon patterns
  - **Functional Impact**: Occupancy progress bars will use Carbon ProgressBar
    component
  - **Functional Impact**: Barcode scanner input will use Carbon TextInput with
    scan icon

- **CR-002**: All UI strings MUST be internationalized via message keys (no
  hardcoded text)

  - **Functional Impact**: All labels, tooltips, validation messages, error
    messages must be translatable
  - **Functional Impact**: Minimum language support: English, French, Swahili

- **CR-003**: Backend MUST follow 5-layer architecture (Data Model → Data Access
  → Business Logic → API → Transfer Objects)

  - **Functional Impact**: Storage entities (Room, Device, Shelf, Rack,
    Position, Assignment, Movement, Disposal) will follow this pattern
  - **Functional Impact**: All entities will include audit fields: created_by,
    created_date, modified_by, modified_date

- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)

  - **Functional Impact**: Storage schema creation will be versioned and
    reversible
  - **Functional Impact**: Migration path for existing samples without locations

- **CR-005**: External data integration MUST use FHIR R4 + IHE profiles

  - **Functional Impact**: Storage entities will map to FHIR Location resources
  - **Functional Impact**: Sample-to-location link via Specimen.container
    reference
  - **Functional Impact**: Support IHE mCSD queries for facility/location
    discovery

- **CR-006**: Configuration-driven variation for country-specific requirements
  (NO code branching)

  - **Functional Impact**: High-risk disposal criteria configurable per
    deployment
  - **Functional Impact**: Capacity thresholds configurable per deployment
  - **Functional Impact**: Position naming conventions flexible (free text, no
    validation)

- **CR-007**: Security: RBAC, audit trail (sys_user_id + lastupdated), input
  validation

  - **Functional Impact**: All storage actions (assign, move, dispose) recorded
    with user ID and timestamp
  - **Functional Impact**: All user input validated server-side (barcode format,
    position text length, etc.)

- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal)
  - **Functional Impact**: Each user scenario (P1-P4) will have corresponding
    E2E test
  - **Functional Impact**: Concurrent access scenarios will be tested

### Key Entities

- **Room**: Physical laboratory room or facility area containing storage
  devices. Attributes: Name, unique code, optional description, active/inactive
  status. Top-level parent in storage hierarchy.

- **Device**: Storage equipment (freezer, refrigerator, cabinet) within a room.
  Attributes: Name, unique code (within parent room), type
  (freezer/fridge/cabinet/other), optional temperature setting, optional
  capacity limit, active/inactive status, parent room reference. Contains
  shelves.

- **Shelf**: Storage shelf within a device. Attributes: Label/number, optional
  capacity limit, active/inactive status, parent device reference. Contains
  racks.

- **Rack**: Storage rack/tray on a shelf with grid structure. Attributes:
  Label/ID, dimensions (rows and columns as integers), optional position schema
  hint, active/inactive status, parent shelf reference. Contains positions.
  Example: 9×9 rack has 81 positions.

- **Position**: Specific storage location within a rack. Attributes: Free-text
  coordinate (flexible naming: A1, 1-1, RED-12, etc.), optional row/column
  integers for grid visualization, occupancy state (empty/occupied), parent rack
  reference. Can be left blank for shelf/rack-level assignment.

- **Sample Assignment**: Link between sample and storage location. Attributes:
  Sample ID (reference to existing Sample entity), Location reference
  (room/device/shelf/rack/position), Assigned by (user ID), Assignment
  timestamp, Optional notes. Represents current location of sample.

- **Sample Movement**: Audit record of sample relocation. Attributes: Sample ID,
  Previous location (full hierarchy path), New location (full hierarchy path),
  Moved by (user ID), Movement timestamp, Optional reason. Immutable audit
  trail.

- **Sample Disposal**: Audit record of sample disposal. Attributes: Sample ID,
  Location at disposal time, Disposed by (user ID), Disposal timestamp, Reason
  (dropdown value), Method (dropdown value), Optional notes, Optional
  certificate attachment. Immutable compliance record.

- **Storage Location Barcode**: Pre-generated barcodes for physical labels.
  Attributes: Barcode value (hierarchical format), Location reference
  (device/shelf/rack), Generated by (user ID), Generation timestamp, Print
  status. Enables barcode scanning workflow.

### Integration Points

- **INT-001**: Add **Storage Location Selector Widget** in existing
  `SamplePatientEntry` screen (sample reception workflow)

  - Placement: After sample collection fields, before Save button
  - Behavior: Optional assignment (can assign later if needed)
  - Widget modes: Cascading dropdowns, type-ahead search, barcode scan
  - Integration: Reuses existing form validation and save mechanism

- **INT-002**: Add **Storage Location Widget** in existing `LogbookResults`
  expanded view (sample search/results workflow)

  - Placement: Below existing referral/test result fields in expanded sample
    details
  - Behavior: Shows current location (read-only or editable based on
    permissions), allows Move action
  - Widget modes: View-only, Edit, Quick-assign
  - Integration: Adapts behavior based on user permissions and context

- **INT-003**: Create **Reusable Storage Location Selector Component**

  - Used in: SamplePatientEntry, LogbookResults, Storage Dashboard, Move dialog,
    Inline creation
  - Modes: View-only, Edit, Quick-assign, Inline-create
  - Features: Cascading dropdowns, type-ahead autocomplete, barcode scan, inline
    location creation
  - Localized: All labels/tooltips use message keys

- **INT-003a**: Create **Location Management Form Page**

  - Route: `/storage/locations/new` (create) and `/storage/locations/:id/edit`
    (edit)
  - Accessible from: Storage Dashboard "Add Location" button (FR-028a)
  - Features: Full CRUD form for Room, Device, Shelf, Rack entities with
    hierarchical parent selection, attribute editing, validation, breadcrumb
    navigation
  - Integration: Uses same REST endpoints as inline creation (POST/PUT
    /rest/storage/rooms, /rest/storage/devices, etc.)
  - Localized: All form labels, validation messages use React Intl message keys

- **INT-004**: Leverage existing **FHIR infrastructure**

  - Map storage entities to FHIR Location resources (Room, Device, Shelf, Rack,
    Position)
  - Link samples to locations via Specimen.container reference
  - Use existing FhirPersistanceService for creating/updating FHIR resources
  - Use existing FhirTransformService for entity↔FHIR conversion
  - Support IHE mCSD queries for location discovery

- **INT-005**: Leverage existing **audit logging infrastructure**

  - Use existing audit table pattern (sys_user_id, lastupdated columns)
  - Extend for storage-specific actions (assign, move, dispose)
  - Retain logs for 7+ years per existing compliance configuration

- **INT-006**: Leverage existing **UI/UX patterns**
  - Tab navigation (Carbon Tabs component, used in multiple screens)
  - Data tables (Carbon DataTable with pagination, sorting, filtering)
  - Modal dialogs (Carbon Modal for confirmations, forms)
  - Overflow menu (Carbon OverflowMenu for row actions)
  - Form validation (existing validation utilities)
  - Internationalization (React Intl message key system)

### Assumptions

- **Assumption 1**: Existing OpenELIS sample entity has unique sample ID
  suitable for foreign key reference
- **Assumption 2**: Existing OpenELIS user/role system supports adding new
  permissions (Assign Samples, Move Samples, Dispose Samples, etc.)
- **Assumption 3**: Existing HAPI FHIR R4 server is running and accessible for
  Location resource sync
- **Assumption 4**: Labs will print barcode labels using standard label printers
  (brother QL-series or similar)
- **Assumption 5**: Barcode scanners emit keyboard input (standard USB HID
  scanners) - no special hardware integration required
- **Assumption 6**: Network connectivity is generally available; offline barcode
  generation is edge case (queue for sync)
- **Assumption 7**: Sample retrieval time baseline (15-30 minutes) based on user
  interviews/observations
- **Assumption 8**: Capacity threshold warnings at 80%, 90%, 100% are
  appropriate for most laboratory contexts (no hard block, always allow
  assignment)
- **Assumption 9**: Auto-assignment of sequential positions for bulk moves
  provides good default behavior, with manual override available for special
  cases
- **Assumption 10**: Position text field max length 50 characters is sufficient
  for all naming conventions observed in field research

## Success Criteria

### POC Functional Completion Criteria

**Note**: This is a Proof-of-Concept implementation. Success criteria focus on
functional completeness and correctness validation for the core tracking
workflow (assign-search-move). Disposal and dashboard features are deferred to
post-POC iterations.

**In-Scope for POC (User Stories P1, P2A, P2B)**:

- **SC-001**: **Storage Assignment Workflow** - User can assign a sample to a
  storage location using any of three methods (cascading dropdowns, type-ahead
  search, barcode scan), and assignment is saved with correct location path,
  user ID, and timestamp

- **SC-002**: **Sample Search and Retrieval** - User can search for a sample by
  ID and system displays the complete hierarchical storage location path (Room >
  Device > Shelf > Rack > Position)

- **SC-003**: **Sample Movement** - User can move a sample from one storage
  location to another, previous location is freed, new location is recorded, and
  audit trail captures the movement with user, timestamp, and reason

- **SC-004**: **Bulk Movement** - User can select multiple samples and move them
  together, system auto-assigns sequential positions with option to modify, and
  each sample receives individual audit record

- **SC-005**: **Location Hierarchy Management** - User can create storage
  locations inline (Room, Device, Shelf, Rack) during assignment workflow, and
  new locations appear immediately in selector without page refresh

- **SC-006**: **Data Integrity** - Concurrent assignment attempts to the same
  position are handled correctly (second user receives error)

- **SC-007**: **Audit Trail Completeness** - All storage actions (assign, move)
  are recorded in audit log with user ID, timestamp, action type, previous
  state, new state, and optional reason

- **SC-008**: **FHIR Integration** - Storage entities (Room, Device, Shelf,
  Rack, Position) are mapped to FHIR Location resources and synced to the FHIR
  server, and sample-to-location links use Specimen.container references

**Deferred to Post-POC** (User Stories P3, P4):

- **SC-FUTURE-001**: Sample Disposal - Disposal workflow with reason/method
  tracking and immutable audit records
- **SC-FUTURE-002**: Storage Dashboard - Metrics cards and 5-tab navigation with
  data tables
- **SC-FUTURE-003**: Occupancy Calculation - Visual occupancy display (fraction,
  percentage, progress bar)
- **SC-FUTURE-004**: Disposed Sample Prevention - System prevents reassignment
  of disposed samples

## Dependencies

- Existing OpenELIS sample entity and database schema
- Existing user authentication and role management system
- Existing FHIR R4 server (HAPI FHIR) running locally
- Existing audit logging infrastructure
- Carbon Design System component library (@carbon/react)
- React Intl internationalization framework
- Barcode scanner hardware (USB HID standard scanners)
- Label printer hardware (for printing barcode labels)

## Out of Scope (Future Enhancements)

- Dual authorization for high-risk sample disposal (configure high-risk
  criteria, require second user approval)
- Automated temperature monitoring/alerts (requires IoT sensor integration)
- RFID tag support (barcode-only for now)
- Predictive analytics for sample expiration forecasting
- Mobile native app for barcode scanning (web-based sufficient for MVP)
- Integration with external freezer management systems (e.g., Thermo Fisher
  FreezerWorks)
- Laboratory automation/robotics integration
- Real-time location tracking (GPS/RFID for chain-of-custody in transit)
- Automated inventory replenishment recommendations
- Visual heatmaps of storage utilization by temperature zone
- Advanced reporting dashboards (BI/analytics beyond CSV export)
- Configurable capacity thresholds per device/rack (MVP uses fixed 80%, 90%,
  100%)
