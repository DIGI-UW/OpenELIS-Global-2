# `/speckit.specify` Prompt - Sample Storage Management

**USE THIS PROMPT**: Copy everything below the line into `/speckit.specify`
command

---

## Feature Request

Create a functional specification for **Sample Storage Management** in OpenELIS
Global - a laboratory information system used by public health laboratories in
low- and middle-income countries.

## Problem Statement

Lab staff receive, process, and store thousands of biological samples (blood,
serum, tissue, etc.) for disease surveillance. Currently, OpenELIS has **NO
physical storage location tracking**, causing:

- **Sample loss**: 2-5 samples per month cannot be located
- **Retrieval delays**: Lab technicians spend 15-30 minutes searching for
  samples
- **Audit failures**: Cannot prove chain-of-custody for stored samples
  (SLIPTA/ISO accreditation requirement)
- **No capacity visibility**: Cannot monitor freezer/fridge utilization

**Users Affected**:

- Reception clerks: No way to record WHERE samples placed after accessioning
- Lab technicians: Waste time searching freezers manually
- Quality managers: Cannot produce audit documentation
- Lab managers: Cannot make data-driven storage decisions

## Visual Design Reference

**Figma**:
https://www.figma.com/design/4NkWdQf5VoIbJiiEb9EJMg/SSR?node-id=12-3&m=dev

**Layout Structure** (functional components, NOT design specifics):

**1. Page Header**

- System title and subtitle for context
- Consistent with existing OpenELIS navigation patterns

**2. Metrics Cards (4 cards in horizontal row)**

- Total Samples: Count of all samples in storage
- Active: Currently stored and available samples
- Disposed: Samples that have been disposed
- Storage Locations: Count of rooms (or total capacity)

**3. Main Dashboard Card**

- **Card Header**: Title + description + action buttons (Add Location, Export)
- **Tabbed Navigation**: 5 tabs - Samples | Rooms | Devices | Shelves | Racks
- **Tab Content**: Data table specific to selected tab

**4. Tab-Specific Tables** (suggested columns - exact order and formatting to be
refined in design):

**Rooms Tab**: Shows storage facilities

- Columns: Name | Code | Devices (count) | Samples (count) | Status | Actions

**Devices Tab**: Shows equipment (freezers, refrigerators, cabinets)

- Columns: Name | Code | Room | Type (freezer/fridge badge) | Occupancy | Status
  | Actions
- Type badges: "freezer", "fridge", "cabinet", etc.

**Shelves Tab**: Shows shelves within devices

- Columns: Label | Device | Room | Occupancy | Status | Actions

**Racks Tab**: Shows racks on shelves

- Columns: Label | Shelf | Device | Dimensions | Occupancy | Status | Actions
- Dimensions format: "9 × 9", "10 × 10", "8 × 12" (rows × columns)

**Samples Tab**: Shows samples with storage locations (design not shown in
Figma, infer from requirements)

- Columns: Sample ID | Type | Status | Location (hierarchical path) | Assigned
  By | Date | Actions

**5. Occupancy Display Pattern** (used across Devices/Shelves/Racks tabs)

- Shows three elements together: fraction + percentage + progress bar
- Example: "23/81" (occupied/total), "28%" (percentage), horizontal bar showing
  visual percentage
- Provides quick capacity recognition at a glance

**6. Hierarchical Location Path Display**

- Format: `Room > Device > Shelf > Rack > Position` (breadcrumb-style with `>`
  separator)
- Example: `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Pos A1`
- Used in tables, sample details, movement logs

**7. Actions**

- Overflow menu (⋮) on each table row for context-specific actions
- Primary actions: Add Location, Export buttons

## Integration with Existing OpenELIS Workflows

### Widget Integration Points (from existing codebase)

**1. Sample Reception Workflow** (SamplePatientEntry view):

- Add **Storage Location Selector Widget** after sample collection fields
- Allows reception clerk to assign storage location during sample entry
- Optional assignment (can be assigned later if needed)

**2. Sample Search/Results Workflow** (LogbookResults expanded view):

- Add **Storage Location Widget** in expanded sample details
- Shows current location if assigned, allows updating/moving sample
- Integrated below existing referral/test result fields

**3. Existing Data Model to Leverage**:

- Sample entity already exists (with sample ID, type, status, etc.)
- User/role management already exists for permissions
- Audit logging infrastructure already exists (sys_user_id, lastupdated pattern)

**4. Existing FHIR Infrastructure to Leverage**:

- HAPI FHIR R4 server already running (local FHIR store)
- FhirPersistanceService and FhirTransformService already implemented
- Specimen resource handling already exists
- Follow same pattern: Create storage entities → Transform to FHIR Location →
  Sync to FHIR store

## User Scenarios (Prioritized)

### Priority 1: Basic Storage Assignment (MVP)

**User**: Reception clerk receives blood sample, needs to record storage
location

**Flow**:

1. Clerk completes standard sample accessioning (existing workflow)
2. Sees "Storage Location" selector widget (NEW)
3. Assigns location via ONE of three methods:
   - **Cascading dropdowns**: Select Room → Device → Shelf → Rack → Position
     (each selection filters next level)
   - **Barcode scan**: Scan pre-printed rack label → auto-fills hierarchy →
     enter position
   - **Type-ahead search**: Search for location by name/code
4. System shows current selection as hierarchical path:
   `Room > Device > Shelf > Rack > Position`
5. Clicks Save → location recorded with user ID and timestamp

**Validations**:

- Cannot assign to already-occupied position (unless rack allows duplicates)
- Cannot assign to inactive/decommissioned location
- Optional capacity warnings if device/rack approaching limits [NEEDS
  CLARIFICATION: Warning threshold percentage (e.g., >80%?), hard block
  threshold (e.g., 100%?), or configurable per device?]

**Success**: Sample has recorded location, can be found quickly later

---

### Priority 2A: Sample Retrieval and Search

**User**: Lab technician David needs to find sample "S-2025-001" for HIV viral
load testing

**Flow**:

1. Opens Storage Dashboard
2. Uses search bar: types "S-2025-001"
3. Table filters to show matching sample with full details:
   - Sample ID: S-2025-001
   - Type: Blood Serum
   - Status: Active
   - Location: `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Pos A5`
   - Assigned By: Maria Lopez
   - Date: 2025-01-15 14:32
4. David notes exact freezer/rack/position, physically retrieves sample
5. Alternatively: Uses filters to show all samples in "Freezer Unit 1", then
   scans list

**Alternative Search Methods**:

- Filter by location: Room="Main Laboratory", Device="Freezer Unit 1" → shows
  all samples
- Filter by date: Last 7 days → shows recently stored samples
- Combined filters: Room + Status + Date Range

**Success**: Retrieval time reduced from 15-30 minutes to <2 minutes

**Independent Test**: Search for sample by ID, verify location displays
correctly, physically retrieve using shown location

---

### Priority 2B: Sample Movement Between Locations

**User**: Lab technician David needs to move sample from -80°C freezer to 4°C
refrigerator for testing

**Flow**:

1. From Storage Dashboard or Sample Detail view, clicks Actions menu (⋮) →
   "Move"
2. Move dialog opens showing:
   - Current location:
     `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Pos A5`
   - Target location selector (same widget as assignment - cascading dropdowns /
     autocomplete / barcode scan)
3. Selects new location:
   `Main Laboratory > Refrigerator 2 > Shelf-1 > Rack R3 > Position C8`
4. Optionally enters reason: "Temporary storage for viral load testing"
5. System validates:
   - Target position C8 is empty (not occupied)
   - Refrigerator 2 is active
   - Rack R3 has available capacity
6. Clicks "Move" → system:
   - Updates sample's current location to new position
   - Frees previous position A5 (now available)
   - Records audit trail: Previous location, New location, User, Timestamp,
     Reason
7. Dashboard immediately shows sample under Refrigerator 2

**Bulk Move**: Select 5 samples, move all to same parent location (Refrigerator
2 > Shelf-1 > Rack R3) [NEEDS CLARIFICATION: Bulk position assignment - system
auto-assigns sequential positions (A1, A2, A3...), or prompts user for each
position?]

**Success**: Sample relocated with full audit trail, previous position freed for
reuse

**Independent Test**: Move sample from one location to another, verify audit
trail records movement, verify dashboard updates

---

### Priority 3: Sample Disposal with Compliance

**User**: Quality manager disposes expired samples following regulatory
protocols

**Flow**:

1. Opens Storage Dashboard, filters to expired samples
2. Selects samples for disposal (single or bulk)
3. Clicks "Dispose" action
4. Disposal dialog requires:
   - Reason (dropdown: Expired, Contaminated, Patient Request, Testing Complete,
     Other)
   - Method (dropdown: Biohazard Autoclave, Chemical Neutralization,
     Incineration, Other)
   - Date/time (default current, editable)
   - Authorization (role-based permission check)
   - Notes (optional free text)
   - Attachment (optional: disposal certificate PDF)
5. For high-risk samples, requires dual authorization (second user approval)
   [NEEDS CLARIFICATION: What defines "high-risk" - pathogen type (TB, HIV), BSL
   level, sample volume/count, or configurable criteria?]
6. Confirms disposal → samples marked "Disposed", locations freed, immutable
   audit record created
7. Disposed samples prevented from future assignment

**Success**: Regulatory compliance, proper audit documentation, freed storage
positions

---

### Priority 4: Storage Dashboard and Capacity Monitoring

**User**: Lab manager monitors storage utilization across facilities

**Flow**:

1. Opens Storage Dashboard
2. Views metric cards showing total samples, active, disposed, locations
3. Switches between tabs to view different levels (Rooms, Devices, Shelves,
   Racks)
4. Each tab shows table with:
   - Entity details (name, code, parent hierarchy)
   - Occupancy (fraction + percentage + visual bar for capacity)
   - Status (Active/Inactive)
   - Actions (Edit, Deactivate, View Details)
5. Uses filters to narrow view (by room, device type, status, date range)
6. Clicks on entity name to drill down (Room → Devices in that room)
7. Exports filtered data to CSV for capacity planning

**Success**: Proactive capacity management, prevents overflow, data-driven
procurement

---

## Functional Requirements (From Original Spec)

### Storage Hierarchy (5 Levels - Per Original Spec)

**FR-001: Five-Level Structure**

- System MUST support hierarchy: **Room → Device → Shelf → Rack → Position**
- Each level references parent (child-parent relationship)
- Location codes unique within parent scope

**FR-002: Entity Metadata**

- **Room**: Name, code, description, active status
- **Device**: Type (freezer/refrigerator/cabinet/other), device ID, optional
  temperature, capacity, status, parent room
- **Shelf**: Label/number, optional capacity, status, parent device
- **Rack**: Label/ID, dimensions (rows, columns as integers), optional position
  schema hint, status, parent shelf
- **Position**: Free-text coordinate (NO enforced schema), optional row/column
  integers for grid visualization, occupancy state, parent rack

**FR-003: Flexible Position Schema** (CRITICAL - Per Original Spec)

- Position is **free text** (NO validation, NO enforced format)
- Different labs use different conventions: A1, 1-1, RED-12, ZONE-A-03, etc.
- Optional row/column integers stored for grid visualization (NOT for
  validation)
- Support "shelf-level" or "rack-level" assignment if position not needed (leave
  position blank)

**FR-004: Rack Dimensions Configuration**

- Users configure rows and columns as integers (e.g., 9×9, 10×10, 8×12)
- Dimensions used for:
  - Calculating total capacity (rows × columns)
  - Optional grid visualization
  - Suggested position coordinates (user can override)

### Multi-Mode Location Selection

**FR-005: Cascading Dropdown Selection**

- Select Room → Device dropdown populates with devices in that room
- Select Device → Shelf dropdown populates
- Select Shelf → Rack dropdown populates
- Select Rack → Position field enables (free text entry)
- Current path displays below: `Room > Device > Shelf > Rack > Position`

**FR-006: Type-Ahead Autocomplete**

- Alternative to dropdowns: search locations by name/code
- Results filtered by parent selection
- Keyboard navigation supported

**FR-007: Barcode Scanning**

- Scan pre-printed barcode label → auto-populates hierarchy
- Barcode formats (hierarchical):
  - Device: `{room}-{device}` (e.g., "MAIN-FRZ01")
  - Shelf: `{room}-{device}-{shelf}` (e.g., "MAIN-FRZ01-SHA")
  - Rack: `{room}-{device}-{shelf}-{rack}` (e.g., "MAIN-FRZ01-SHA-RKR1")
- Scanning rack barcode fills Room/Device/Shelf/Rack, focuses Position field
- Handle duplicates with disambiguation dialog

**FR-008: Barcode Generation**

- System generates printable labels for Device, Shelf, Rack
- Labels include human-readable text + barcode
- Print individual or batch labels

### Inline Location Creation

**FR-009: Add New Location Without Leaving Workflow**

- "Add new" option available at each hierarchy level (Room, Device, Shelf, Rack)
- Quick-add dialog requires minimum information to create location:
  - Identifying name/label (required)
  - Parent relationship (auto-selected from current context)
  - Type-specific attributes (e.g., device type, rack dimensions)
  - Unique code (auto-generated or manually entered)
- System validates uniqueness of code within parent scope
- New location appears immediately in selector without page refresh
- Failed validation shows error inline (e.g., "Code already exists in this
  room")

### Sample Assignment and Movement

**FR-010: Assign Sample to Position**

- Record: Sample ID, Location (room/device/shelf/rack/position), Assigned By
  (user), Timestamp, Notes
- Validation: Position not occupied (unless rack allows duplicates), location is
  active

**FR-011: Move Sample Workflow**

- Initiate from dashboard or sample detail
- Select target location using same selector widget (dropdown/autocomplete/scan)
- Optional reason for move
- Validation: Target location active, position not occupied, has capacity
- Records: Previous location, New location, User, Timestamp, Reason
- Updates dashboard immediately

**FR-012: Bulk Move**

- Select multiple samples, move together to same parent location
  (device/shelf/rack)
- Individual audit record per sample

### Sample Disposal Workflow

**FR-013: Disposal Required Fields**

- Reason (dropdown): Expired, Contaminated, Patient Request, Testing Complete,
  Other
- Method (dropdown): Biohazard Autoclave, Chemical Neutralization, Incineration,
  Other
- Date/Time (default current, editable)
- Authorization (role-based permission)
- Notes (optional free text)
- Attachment (optional: disposal certificate)

**FR-014: Disposal Effects**

- Sample status → "Disposed"
- Current location cleared (position becomes available)
- Immutable audit record created
- Disposed samples prevented from future assignment

**FR-015: Dual Authorization**

- High-risk sample types require second user approval for disposal
- Configurable by sample type/category [NEEDS CLARIFICATION: How configured -
  admin UI screen, config file, or database table? Who can configure - only
  admins?]

### Dashboard and Reporting

**FR-016: Tabbed Views**

- Five tabs: Samples | Rooms | Devices | Shelves | Racks
- Each tab shows table appropriate for that entity level
- Tab selection state visually distinct

**FR-017: Metrics Display**

- Four metrics shown at top: Total Samples, Active, Disposed, Storage Locations
- Updates immediately when user performs actions (assign, move, dispose)

**FR-018: Occupancy Display**

- Shows capacity utilization as: fraction (occupied/total) + percentage + visual
  indicator
- Used in Devices, Shelves, Racks tabs
- Example: "287/500" + "57%" + progress bar

**FR-019: Filters and Search**

- Search by sample ID, location name/code
- Filter by: Room, Device, Status, Date Range, Sample Type
- Filters combine with AND logic
- Clear filters option resets to show all

**FR-020: Drill-Down Navigation**

- Click Room name → Devices tab filtered to that room
- Click Device name → Shelves tab filtered to that device
- Click Shelf → Racks tab filtered to that shelf
- Click Rack → Grid view OR Samples tab filtered to that rack

**FR-021: Export to CSV**

- Export current filtered/visible data
- Columns include all table data + additional metadata
- Completes in reasonable time for large datasets

**FR-022: Grid Visualization (Optional)**

- For racks with dimensions configured, provide visual grid view
- Shows row/column labels, cell states (empty/occupied/reserved)
- Click empty cell to assign sample, click occupied cell for sample details
- Hover shows sample information

### Validation and Safety

**FR-023: Location Validation**

- Prevent assignment to inactive location
- Prevent double-occupancy unless rack allows
- Validate capacity limits if configured
- Clear error messages with suggested alternatives

**FR-024: Concurrent Access**

- Handle simultaneous assignments to same position gracefully
- Show error to second user with current state

### Audit Trail

**FR-025: Audit Log Requirements**

- Record ALL actions: assign, move, dispose
- Log includes: User ID, Timestamp, Action, Previous State, New State, Reason
  (if provided)
- Audit records are immutable (cannot edit or delete)
- Viewable by users with appropriate permission

### Permissions and Roles

**FR-026: Role-Based Access**

- **Lab Technicians**: Assign samples, Move samples
- **Quality Managers**: All technician permissions + Dispose samples, View audit
  logs, Deactivate locations
- **Administrators**: All permissions + Create/Edit/Delete locations, Configure
  capacity rules
- Permissions enforced on backend (not just UI)

### Data Validation Rules

**FR-027: Uniqueness Constraints**

- Hierarchical barcode combinations MUST be unique: room-device,
  room-device-shelf, room-device-shelf-rack
- Location codes MUST be unique within parent scope (two devices in different
  rooms can both be "FRZ-01")
- Position codes can be duplicated across different racks (position "A1" can
  exist in multiple racks)

**FR-028: Dimension Validation**

- Rack rows and columns MUST be positive integers (1 or greater)
- Allow zero rows/columns to indicate "no grid" (shelf-level or rack-level
  assignment only)
- Calculated capacity = rows × columns (or 0 if no grid)

**FR-029: Position Field Validation**

- Position accepts free text up to configurable max length (e.g., 50 characters)
- Disallow control characters (tabs, newlines)
- Warning (not error) if position duplicated within same rack [NEEDS
  CLARIFICATION: Should system prevent duplicate positions in same rack, or only
  warn?]

**FR-030: State Transition Rules**

- Disposal action: Set sample status to "Disposed" AND clear current location
  (position freed)
- Disposed samples MUST be prevented from future location assignment
- Deactivating location with active samples: Prompt user to move/dispose samples
  first, OR allow deactivation but prevent new assignments

**FR-031: Referential Integrity**

- Prevent deleting location if samples currently assigned (show error with
  sample count)
- Deactivation allowed but blocks new assignments
- Deleting parent location (e.g., Device) requires all child locations (Shelves)
  to be empty or deactivated

### Non-Functional Requirements

**NFR-001: Performance**

- Location selector cascading dropdown response: <1 second per level with
  50,000+ location nodes
- Dashboard table load: <2 seconds with 100,000+ samples
- Search/filter operations: <2 seconds response time
- Barcode scan resolution: <1 second to populate fields
- CSV export: <10 seconds for 10,000 records

**NFR-002: Reliability**

- Barcode generation works offline if needed (queue sync)
- Prevent duplicate barcode codes through validation before save
- Concurrent access conflicts handled gracefully (optimistic locking)

**NFR-003: Auditability**

- Immutable audit logs for ALL actions (assign, move, dispose)
- Logs include: User ID, Timestamp, Action, Previous State, New State, Reason
- Audit log retention: 7+ years (compliance requirement)
- Disposal certificate attachments encrypted at rest [NEEDS CLARIFICATION:
  Encryption requirements - at-rest only, or in-transit too? Algorithm
  requirements?]

**NFR-004: Security**

- Role-based access control enforced on backend (not just UI hiding)
- Prevent unauthorized moves/disposals (permission checks)
- Barcode payloads contain system IDs only (NO patient data)
- Session timeout applies (existing OpenELIS security settings)

**NFR-005: Internationalization**

- All UI labels, tooltips, validation messages, error messages translatable
- Support right-to-left layouts (Arabic support)
- Date/time/number formatting respects user locale
- Minimum language support: English, French, Swahili (OpenELIS standard)

**NFR-006: Usability**

- Minimize clicks: Type-ahead autocomplete, default selections, recent locations
  shortcut
- Keyboard navigation: Full operation without mouse
- Clear error messages with suggested actions
- Consistent interaction patterns across all workflows

## Leverage Existing OpenELIS Patterns (Integration Requirements)

**CRITICAL**: This feature integrates into existing OpenELIS workflows and
infrastructure. Implementation should follow established patterns, NOT reinvent
wheels.

### Technical Constraints (OpenELIS-Specific)

> ⚠️ **SPEC KIT EXCEPTION**: Normally specifications are technology-agnostic,
> but OpenELIS has **non-negotiable architectural constraints** (Carbon Design
> System, FHIR R4, layered backend architecture) defined in its project
> constitution. These constraints affect functional design and are stated here
> to prevent incompatible approaches during specification.

OpenELIS has **mandatory architectural patterns** defined in its constitution.
While implementation details belong in the planning phase, these constraints
affect functional design:

**Backend Architecture**:

- All entities follow **layered architecture pattern**: Data Model → Data Access
  → Business Logic → API → Transfer Objects
- All entities include **audit fields**: user who created/modified, timestamps
- All entities with external interoperability include **unique identifier for
  FHIR mapping**
- Schema changes via **database migration tool only** (NOT direct SQL)

**FHIR/Interoperability**:

- OpenELIS has **FHIR R4 server** already running (local store for healthcare
  data exchange)
- Existing services handle FHIR resource creation/updates and entity↔FHIR
  transformation
- Storage entities MUST map to **FHIR Location resources** (standard resource
  for physical locations)
- Sample-to-location link via **Specimen.container** reference (existing
  Specimen resource structure)
- Support **IHE mCSD queries** for care services discovery (existing facility
  registry pattern)

**Frontend Architecture**:

- All UI follows **Carbon Design System** (official framework adopted
  August 2024)
- All text must be **localizable** via message keys (NOT hardcoded strings)
- Existing component patterns: Tabs, data tables, forms, modals, overflow menus
- Existing utilities for API calls, form validation, date formatting

### Integration Points in Existing Workflows

**INT-001: Sample Reception Workflow**

- Add **Storage Location Selector Widget** in existing SamplePatientEntry screen
- Placement: After collector/sample collection fields
- Behavior: Optional assignment (can assign later if needed)
- Reuses existing form validation and save mechanism

**INT-002: Sample Search/Results Workflow**

- Add **Storage Location Widget** in LogbookResults expanded view
- Shows current location if assigned (read-only or editable based on
  permissions)
- Placement: Below existing referral/test result fields
- Actions: Move sample, View location details

**INT-003: Reusable Widget Component**

- Single component used in BOTH workflows above (and future workflows)
- Modes: View-only, Edit, Quick-assign
- Same selector mechanism (cascading dropdowns / type-ahead / barcode scan) in
  all contexts
- Adapts behavior based on context and permissions

### Leverage Existing Infrastructure

**Data Model**:

- Sample entity already exists (ID, type, status, patient link, etc.)
- User and role management already exists
- Audit logging infrastructure already exists
- Organizational hierarchy pattern already exists (sites, facilities,
  departments)

**FHIR**:

- HAPI FHIR R4 server running (co-habitant with OpenELIS)
- Persistence service for creating/updating FHIR resources exists
- Transformation service for entity↔FHIR conversion exists
- Specimen resource sync already implemented
- **NEW**: Map storage to FHIR Location, link Specimen to Location

**UI/UX**:

- Tab navigation pattern exists (used in multiple screens)
- Data table pattern exists (with pagination, sorting, filtering)
- Modal dialog pattern exists (for confirmations, forms)
- Form validation pattern exists
- Internationalization infrastructure exists (multi-language support)

### What NOT to Build (Already Exists)

- ❌ Authentication/authorization framework (use existing role system)
- ❌ Audit logging infrastructure (use existing audit tables and patterns)
- ❌ FHIR server (already running)
- ❌ FHIR sync mechanism (use existing persistence/transform services)
- ❌ Localization framework (use existing message key system)
- ❌ Base UI components (use existing component library)
- ❌ Database connection/migration framework (already configured)

## Key Requirements from Original Spec

### From "Storage Hierarchy and Data Model"

- 5-level hierarchy: Room → Device → Shelf → Rack/Tray → Position
- Parent-child relationships enforced
- Uniqueness within parent scope
- Active/inactive states at all levels
- Optional capacity limits at device/shelf/rack

### From "Functional Requirements"

- **Location Selection**: Multi-mode (autocomplete, dropdowns, barcode scan)
- **Inline Add**: Create Room/Device/Shelf/Rack from selector
- **Barcode**: Hierarchical format (room-device, room-device-shelf,
  room-device-shelf-rack)
- **Rack Configuration**: Rows and columns as integers, flexible position free
  text
- **Sample Assignment**: Store to position or shelf/rack level, prevent double
  occupancy
- **Move Workflow**: Target selection, validation, audit trail, bulk support
- **Disposal Workflow**: Reason, method, authorization, immutable record,
  chain-of-custody

### From "Reusable UI Component"

- Single component for all workflows
- Modes: Create, select, scan barcode
- Cascading filters at each level
- Keyboard navigation support
- Inline add at each level
- Localized labels/tooltips

### From "Dashboard and Reporting"

- Views: Samples by location, Locations by hierarchy, Capacity summary
- Filters: Room, Device, Date range, Status
- Actions: Move, Dispose (single/bulk)
- Metrics: Total samples, Active, Disposed, Capacity utilization
- Drill-down: Room → Devices → Shelves → Racks → Positions

### From "Permissions and Roles"

- Three role levels defined above (Technicians, Quality Managers,
  Administrators)
- Disposal requires specific permission
- Audit log viewing requires permission
- Location management requires admin permission

## Acceptance Criteria (Simplified)

**AC-001: Location Assignment**

- User can assign sample to storage location via dropdown/autocomplete/barcode
- System prevents double-occupancy
- Assignment includes user and timestamp

**AC-002: Sample Retrieval** (Priority 2A)

- Dashboard shows samples with storage locations
- User can search by sample ID in <2 seconds
- User can filter by room, device, status, date range
- Location displayed as hierarchical path format

**AC-003: Sample Movement** (Priority 2B)

- User can move sample to new location from dashboard or sample detail
- Movement validated (target active, not occupied, has capacity)
- Audit trail records previous location, new location, user, timestamp, reason
- Dashboard updates immediately after move

**AC-004: Sample Disposal**

- User can dispose samples with reason/method
- Disposed samples become non-assignable
- Disposal record immutable with full details

**AC-005: Dashboard Navigation**

- Five tabs show appropriate data tables
- Drill-down from Room → Device → Shelf → Rack works
- Occupancy displayed with fraction + percentage + visual indicator

**AC-006: Inline Location Creation**

- User can create new location from selector
- New location appears immediately
- Duplicate codes prevented

**AC-007: Barcode Workflow**

- System generates printable labels (device, shelf, rack)
- Scanning label auto-populates hierarchy
- Duplicate barcodes handled with disambiguation

**AC-008: Permissions Enforced**

- Only authorized users can dispose samples
- Only administrators can create/deactivate locations
- Permissions checked on backend

## Success Metrics (Measurable)

**SM-001: Sample Retrieval Time**

- Baseline: 15-30 minutes average
- Target: <2 minutes average
- Measurement: Time from search to physical retrieval

**SM-002: Sample Loss Rate**

- Baseline: 2-5 samples/month cannot be located
- Target: Zero samples lost due to unknown location
- Measurement: Monthly "cannot locate" count

**SM-003: Location Coverage**

- Baseline: 0% samples have recorded location
- Target: 100% newly received samples have location within 6 months
- Measurement: % of active samples with non-null location

**SM-004: Audit Compliance**

- Baseline: 60% SLIPTA compliance (storage items)
- Target: 95% SLIPTA compliance
- Measurement: Annual SLIPTA audit results

## Out of Scope (Future Enhancements)

- Automated temperature monitoring/alerts (requires IoT sensors)
- RFID tag support (barcode only for now)
- Predictive analytics for sample expiration
- Mobile native app for scanning (web-based sufficient)
- Integration with external freezer management systems
- Laboratory automation/robotics integration

---

**KEY POINTS FOR SPECIFICATION**:

1. **5-level hierarchy** (Room > Device > Shelf > Rack > Position), NOT 6
   levels - per original spec
2. **Position is free text** (NO validation, supports any naming convention) -
   CRITICAL requirement
3. **Reusable widget** integrated into existing workflows (SamplePatientEntry,
   LogbookResults)
4. **Three input methods**: Cascading dropdowns, Type-ahead autocomplete,
   Barcode scanning
5. **Inline creation**: Add locations without leaving workflow
6. **Audit everything**: Assign, move, dispose with immutable logs (7+ year
   retention)
7. **Dashboard with tabs**: 5 tabs (Samples, Rooms, Devices, Shelves, Racks)
   with tab-specific tables
8. **Occupancy visualization**: Fraction + percentage + visual indicator for
   capacity
9. **Leverage existing**: FHIR infrastructure, data model patterns, UI component
   library, localization, audit logging
10. **Permissions**: Three role levels (Technicians, Quality Managers,
    Administrators)
11. **5 user scenarios** (Priority 1=MVP): Assignment → Retrieval → Movement →
    Disposal → Dashboard
12. **Performance targets**: <2 sec search/filter, <1 sec barcode scan, <10 sec
    CSV export
13. **NEEDS CLARIFICATION markers**: High-risk criteria, capacity thresholds,
    bulk move behavior, encryption requirements
