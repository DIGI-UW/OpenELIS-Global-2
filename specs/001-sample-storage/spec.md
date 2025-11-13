# Feature Specification: Sample Storage Management

**Feature Branch**: `001-sample-storage`  
**Created**: October 30, 2025  
**Status**: Draft  
**Figma Design**:
[Sample Storage Dashboard](https://www.figma.com/design/4NkWdQf5VoIbJiiEb9EJMg/SSR?node-id=12-3&m=dev)

## Executive Summary

OpenELIS Global laboratories currently have **NO physical storage location
tracking** for biological sample items (blood tubes, serum aliquots, tissue
sections, etc.). This causes:

- **Sample loss**: 2-5 sample items per month cannot be located
- **Retrieval delays**: Lab technicians spend 15-30 minutes searching for sample
  items
- **Audit failures**: Cannot prove chain-of-custody for stored sample items
  (SLIPTA/ISO accreditation requirement)
- **No capacity visibility**: Cannot monitor freezer/refrigerator utilization

This feature introduces a **5-level storage hierarchy** (Room → Device → Shelf →
Rack → Position) with multi-mode location assignment (cascading dropdowns,
type-ahead search, barcode scanning), sample item movement tracking, and
compliance-ready disposal workflows. **Storage tracking operates at the
SampleItem level** (physical specimens), not at the Sample level (orders). Each
SampleItem can be stored independently, even when multiple SampleItems belong to
the same parent Sample.

**Target Users**: Reception clerks, lab technicians, quality managers, lab
managers

**Expected Impact**:

- Reduce sample item retrieval time from 15-30 minutes to <2 minutes
- Eliminate sample item loss due to unknown location
- Achieve 95% SLIPTA compliance for storage documentation
- Enable data-driven storage capacity planning

**Storage Granularity**: Storage locations are tracked at the **SampleItem
level** (physical specimens), not at the Sample level (orders). This allows
different SampleItems from the same Sample to be stored in different locations
when needed (e.g., blood tube in freezer, serum aliquot in refrigerator). The
dashboard and assignment workflows are SampleItem-specific, with parent Sample
information displayed for context and sorting/grouping capabilities.

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
  Tab-specific filtering requirements: (1) Samples tab - filter by location and
  by status, (2) Rooms tab - filter by status, (3) Devices tab - filter by type
  and room and status, (4) Shelves tab - filter by device and room and status,
  (5) Racks tab - show room column, filter by room, filter by shelf, device, and
  status

### Session 2025-11-05

- Q: When a user selects a location in the single location dropdown (e.g.,
  "Freezer Unit 1"), what should the filter behavior be? → A: Show all samples
  within that location's hierarchy (downward inclusive) - selecting "Freezer
  Unit 1" shows all samples in any shelf/rack/position within that freezer
- Q: What UI pattern should the location dropdown use for hierarchical browsing?
  → A: Combination: tree view for browsing, flat autocomplete list for search
  results
- Q: How should the autocomplete search work in the location dropdown? → A:
  Search matches location names/codes at any hierarchy level, displays full path
  (e.g., typing "Freezer" matches "Freezer Unit 1" and shows "Main Laboratory >
  Freezer Unit 1")
- Q: Should inactive/decommissioned locations appear in the location dropdown? →
  A: Show inactive locations but visually distinguish them (grayed out,
  disabled, or with "Inactive" badge) - users can select to filter samples that
  were in those locations
- Q: Should Position-level locations be included in the location dropdown? → A:
  Exclude Position-level - dropdown only includes Room, Device, Shelf, and Rack
  levels (users can filter by rack, then manually find position in table)
- Q: How should the Storage Locations metric card display the breakdown by type?
  → A: Display as formatted text list with counts (e.g., "12 rooms, 45 devices,
  89 shelves, 156 racks")
- Q: How should the color-coding be applied to the Storage Locations breakdown
  and tabs? → A: Color-code metric card text AND apply matching subtle accent
  colors to tab labels/backgrounds (e.g., Rooms tab has blue accent matching "12
  rooms" blue)
- Q: Which color scheme should be used for the location type color-coding? → A:
  Carbon Design System color tokens (e.g., blue-70 for Rooms, teal-70 for
  Devices, purple-70 for Shelves, orange-70 for Racks) - colorblind-friendly and
  accessible
- Q: Should the Storage Locations breakdown include inactive/decommissioned
  locations, or only active locations? → A: Only active locations - breakdown
  shows counts for active rooms/devices/shelves/racks only

### Session 2025-11-05

- Q: When a Device or Shelf has no capacity_limit set and cannot calculate
  capacity from children (because some children lack defined capacities), how
  should the occupancy be displayed in the dashboard? → A: Show "N/A" or
  "Unlimited" with a tooltip explaining why capacity cannot be determined
- Q: When a Sample has multiple SampleItems (e.g., blood tube + serum aliquot),
  can they be stored in different storage locations? → A: Yes, each SampleItem
  can be stored independently in different locations
- Q: When assigning storage location, how should users identify which SampleItem
  to assign? → A: Hybrid approach - dashboard and assignment are
  SampleItem-specific, with parent Sample info displayed and sortable by Sample
  to easily see sample items together
- Q: In the Storage Dashboard "Samples" tab, what should be displayed as the
  primary identifier for each row? → A: SampleItem ID/External ID (with Sample
  accession number as secondary info)
- Q: When searching for storage locations, should users search by SampleItem
  ID/External ID, Sample accession number, or both? → A: Both (search matches
  either SampleItem ID/External ID or Sample accession number)
- Q: For FHIR Specimen resource mapping, should the Specimen.container reference
  point to the SampleItem's storage location? → A: Yes, each SampleItem's
  storage location maps to its Specimen.container reference

### Session 2025-11-05

- Q: What behavior should "live-searched" use for samples? → A: Debounced
  real-time search (300-500ms delay after typing stops) - balances
  responsiveness with performance and reduces API calls
- Q: For samples, should the search field match across all three fields
  simultaneously (sample ID, sample type, assigned location), or should it
  search each field separately? → A: Combined search - single search field
  matches any of sample ID, sample type, or location (OR logic)
- Q: How should the search field match text across all tabs? → A:
  Case-insensitive partial match (substring) - "freezer" matches "Freezer Unit
  1"
- Q: For samples, what field should "sample type" refer to in the search? → A:
  Accession number type/prefix (e.g., "S-2025", "TB-001")
- Q: For samples, when searching "any of the assigned locations," should it
  match the full hierarchical path string or individual location components? →
  A: Full hierarchical path string (e.g., "Main Laboratory > Freezer Unit 1 >
  Shelf-A > Rack R1 > Position A5")
- Q: What are the minimum required levels for a valid sample location? → A: A
  valid location for a sample must have at least 2 levels set: Room and Device
  MUST be selected. Shelf, Rack, and Position levels are optional

### Session 2025-11-06

- Q: Which menu items should appear in the samples table row overflow menu? → A:
  All four items: Move, Dispose, View Audit (placeholder), View Storage
- Q: How should position hierarchy work - can positions have varying hierarchy
  depths? → A: Positions can have only room+device, or room+device+shelf+rack,
  etc. There's no auto-resolve needed - just require that at least two levels
  (room + device) are part of a position. The position entity itself maintains
  the full hierarchy path, but the minimum requirement for assignment is
  room+device.
- Q: What is the exact structure of position hierarchy levels? → A: A position
  can have at most room>device>shelf>rack>position (5 levels), but at least room
  and device (2 levels minimum). A sample can be associated with a position that
  represents the lowest level in the hierarchy for that assignment. The position
  can be at: device level (2 levels: room+device), shelf level (3 levels:
  room+device+shelf), rack level (4 levels: room+device+shelf+rack), or position
  level (5 levels: room+device+shelf+rack+position). The requirement is that it
  must be at least at the device level (cannot be just a room).
- Q: What should the View Storage modal display? → A: Based on Figma design
  (sample row menu - location modal page): Modal titled "Storage Location
  Assignment" showing sample information (ID, Type, Status) in highlighted box,
  current location hierarchical path in highlighted gray box, separator, and
  full location assignment form (barcode scan, Room/Device/Shelf/Rack/Position
  selectors, condition notes) - allows viewing current location details and
  editing assignment
- Q: Should move and dispose modals be reviewed against Figma designs? → A:
  Yes - review Figma move and dispose modal pages to capture any missing UI
  details and ensure alignment
- Q: What are the exact UI details for the move modal? → A: Based on Figma
  design (node 1-481): Modal titled "Move Sample" with subtitle, current
  location in gray box, downward arrow icon, new location selector in bordered
  box (barcode scan, Room/Device/Shelf/Rack dropdowns), "Selected Location"
  preview box showing "Not selected" until location chosen, optional reason
  textarea, Cancel and "Confirm Move" buttons
- Q: What are the exact UI details for the dispose modal? → A: Based on Figma
  design (node 1-782): Modal titled "Dispose Sample" with subtitle, red warning
  alert at top ("This action cannot be undone"), sample info box
  (ID/Type/Status), current location section with note, disposal instructions
  info box, separator, required Reason and Method dropdowns, optional Notes
  textarea, confirmation checkbox ("I confirm..."), Cancel and "Confirm
  Disposal" button (red/destructive styling, disabled until checkbox checked)
- Q: How should the location selector widget be structured for orders and
  results workflows? → A: Both workflows use same widget: compact inline view
  (shows selected location path or "Not assigned") with "Expand" or "Edit"
  button that opens full location modal (same structure as View Storage modal
  with sample info, current location, full assignment form)
- Q: What should the quick-add inline input do in the results workflow compact
  view? → A: Simple quick find/search input for any location level that already
  exists (type-ahead autocomplete matching Room, Device, Shelf, or Rack
  levels) - barcode workflows are delayed to later stage

### Session 2025-11-06

- Q: What action menu structure should location tabs (Rooms, Devices, Shelves,
  Racks) have? → A: Full CRUD overflow menu: Edit, Delete, View Details (similar
  to Samples tab pattern)
- Q: How should the Edit operation UI work for location entities? → A: Modal
  dialog with full form (all fields editable in modal, similar to View Storage
  modal pattern)
- Q: What validation and confirmation should occur for Delete operations on
  locations? → A: Validation with confirmation dialog: Check for child locations
  and active samples, show warning/error if constraints exist, require
  confirmation dialog before deletion
- Q: What should the View Details operation display? → A: View Details option
  should not be included - all important details should be visible in the table
  columns
- Q: Which fields should be editable vs read-only in the Edit modal? → A: Code
  and Parent are read-only (only name/description/attributes editable, prevents
  structural changes)

### Session 2025-11-06

- Q: How should the Move and View Storage menu items be consolidated? → A:
  Replace both "Move" and "View Storage" with a single "Manage Location" menu
  item that opens the consolidated modal
- Q: What wording should be used for the consolidated modal title and button? →
  A: Dynamic wording based on location existence: If no location assigned →
  "Assign Storage Location" (title) / "Assign" (button). If location exists →
  "Move Sample" (title) / "Confirm Move" (button) - keep movement terminology
  when location exists
- Q: When should the "Reason for Move" field appear and be required? → A: Show
  "Reason for Move" field only when sample has existing location AND user
  selects a different location. Field is optional (not required)
- Q: What sample details should be displayed in the consolidated modal? → A:
  Show Sample ID, Type, Status, plus additional fields like Date Collected,
  Patient ID, Test Orders (comprehensive details beyond basic ID/Type/Status)
- Q: What does "dashboard sample table options" refer to that needs
  clarification? → A: "Options" refers to action menu items (overflow menu) -
  addressed by consolidating Move/View Storage into single menu item

### Session 2025-11-07

- Q: What UI pattern should be used for expanding location table rows to view
  additional fields? → A: Expandable row with inline content below the row
  (Carbon DataTable expandable row pattern)
- Q: How should row expansion be triggered? → A: Click chevron/expand icon in a
  dedicated column (Carbon DataTable standard)
- Q: What content should be displayed in the expanded row? → A: All entity
  fields not visible in table columns, formatted as key-value pairs
- Q: Can multiple rows be expanded simultaneously? → A: Only one row can be
  expanded at a time (expanding another collapses the previous)
- Q: Should the expanded view allow editing fields directly? → A: Read-only
  display (Edit action remains in overflow menu)

### Session 2025-11-06 (Barcode Workflows)

- Q: What barcode formats/standards should be supported for storage location
  scanning? → A: Support industry-standard 1D barcodes (Code 128, Code 39,
  EAN-13) and 2D barcodes (QR Code, Data Matrix). Code 128 is the primary format
  for storage location labels due to high data density and alphanumeric support,
  barcode settings are inherited from the barcode setup in system
  administration. System should auto-detect format.

- Q: What is the expected barcode data structure for location labels? → A:
  Hierarchical path encoded with delimiters. Format:
  `{ROOM_CODE}-{DEVICE_CODE}-{SHELF_CODE}-{RACK_CODE}` (e.g.,
  "MAIN-FRZ01-SHA-RKR1"). Position is NOT encoded in barcode - user enters
  manually after scan. Use hyphen (-) as delimiter for easy parsing.

- Q: What hardware requirements should be specified for barcode scanners? → A:
  Support standard USB HID barcode scanners (keyboard wedge mode) - most common
  in lab settings. Scanner input should be processed as keyboard input with
  automatic Enter/Return at end. No special drivers or software required.
  Bluetooth scanners acceptable if they support HID profile.

- Q: How should the barcode input field behave and provide feedback? → A:
  Dedicated barcode input field with icon indicator. On focus, show "Ready to
  scan" state with animation/pulse. On successful scan, show green checkmark
  with decoded path preview. On error, show red X with error message. Field
  should auto-clear after successful population of location fields.

- Q: What validation should occur when a barcode is scanned? → A: (1) Verify
  barcode format is valid and parseable, (2) Verify all encoded location codes
  exist in database, (3) Verify location hierarchy is correct (e.g., Shelf-A is
  actually child of Freezer Unit 1), (4) Verify location is active (not
  decommissioned), (5) Show clear error messages for each failure type.

- Q: What should happen if a scanned barcode contains invalid or non-existent
  location codes? → A: Display error message specifying which component failed
  validation (e.g., "Rack 'RKR1' not found in Shelf 'SHA'"). Allow user to
  either rescan correct barcode OR switch to manual selection mode to correct
  the error. Do not partially populate fields with invalid data.

- Q: How should barcode scanning integrate with the existing cascading dropdown
  and type-ahead modes? → A: Provide mode toggle buttons/tabs: "Manual Select"
  (cascading dropdowns) or the user can also use a "Enter / Scan" field that
  will allow a type ahead search or scan, the format will be the same for either
  entry. Both types of entry should be visible at the same time, though the user
  can fill either one.

- Q: Should barcode scanning support sample barcodes in addition to location
  barcodes? → A: Yes - support dual barcode types with auto-detection. Sample
  barcodes (format: accession number like "S-2025-001") trigger sample lookup
  and load sample details. Location barcodes populate location hierarchy. System
  distinguishes by format pattern matching. If sample barcode scanned, pre-fill
  sample context; if location barcode scanned, populate location selectors.

- Q: What audio/visual feedback should occur on successful vs failed barcode
  scans? → A: Successful scan: Green flash + smooth transition to populated
  fields. Failed scan: Red flash + error message with retry instruction.

- Q: How should the barcode scanner handle rapid successive scans or accidental
  double-scans? → A: Implement debouncing with 500ms cooldown period after each
  scan. If duplicate barcode scanned within cooldown, ignore silently (no
  error). If different barcode scanned within cooldown, show warning "Please
  wait before next scan" and ignore input. Prevents accidental double-entry.

- Q: What should happen when a user manually edits location fields after a
  successful barcode scan? → A: They will be separate but be on the screen at
  the same time, so if the user scans a barcode, it should run that search, it
  should update the drop downs to reflect what was scanned, and can be modified
  by the user, e.g., if the user is moving a sample to a different box on the
  same rack, they might scan the wrong rack, then fix it in the dropdown and
  save. User can rescan to overwrite manual changes.

- Q: Should barcode functionality be available in all location assignment
  contexts (Order Entry, Results, Move, Dashboard)? → A: Yes - barcode scanning
  should be available consistently across all workflows where location
  assignment occurs: Order Entry widget, Results workflow, Move Sample modal,
  Manage Location modal, and Dashboard Add/Edit Location forms. Same barcode
  input component and behavior in all contexts.

- Q: What error recovery options should be provided when barcode scanning fails?
  → A: Provide two recovery paths: (1) prompt user to scan again, stay in the
  same text input to allow a second scan without intervention, (2) allow the
  user to enter manually using the cascading dropdown mode with scanned code
  visible for reference, or to use the type-ahead search, which is the same as
  the barcode field. Show last scanned code in error message to help user
  identify issue.

- Q: How should barcode printing/generation be addressed for new storage
  locations? → A: When clicking the action button on devices, shelves, and
  racks, it should say label management, and show a modal with 2 fields: short
  code - which must be unique within its context, e.g., one shelf1 in the same
  device - and print label, which will allow the user to print a label for that
  shelf. It will be the same label type and size as is specified in the system
  administration, barcode configuration. It will give a warning before changing
  the short code that alerts the user that changing the short code will
  invalidate any already printed labels, and those will need to be re-generated
  for any labels that belong to this level or lower.

### Session 2025-11-06 (Barcode Implementation Details)

- Q: When a user clicks "Label Management" on a device/shelf/rack, how should
  the system handle the "Print Label" action? → A: Send directly to the
  configured default barcode printer from system administration settings. If no
  default printer is configured, show printer selection dialog. Generate label
  using the barcode format and size specified in system admin settings. Show the
  preview of the PDF label in a new tab.

- Q: What should the short code format/validation be? → A: Maximum 10
  characters. Alphanumeric only (A-Z, 0-9, no special characters except hyphen
  and underscore). Auto-uppercase all input for consistency. Manual entry only
  (no auto-generate). Must start with a letter or number (not
  hyphen/underscore).

- Q: For the warning about invalidating printed labels when changing short
  codes, what user actions should be available? → A: Show blocking confirmation
  dialog with two options: (1) "Cancel" - abort the change and keep existing
  short code, (2) "Proceed" - save the new short code with warning acknowledged.
  After proceeding, display informational message listing all affected location
  levels that need label reprinting (e.g., "Labels need reprinting for: Shelf-A,
  Rack R1, Rack R2, Position A1-A10"). No automatic re-print trigger.

- Q: How should the system handle barcode scans that include Position level data
  (even though spec says Position NOT encoded)? → A: Accept and parse all levels
  including Position if present in barcode. If 5th level detected, auto-populate
  Position field. This provides flexibility for labs that choose to encode full
  5-level hierarchy despite recommendation. System validates the parsed position
  exists and is valid for that rack.

- Q: What happens if a scanned barcode has ONLY 2 levels (e.g., "MAIN-FRZ01")
  instead of the expected 4? → A: Valid - accept 2-level barcodes since
  Room+Device is the minimum requirement. Auto-populate only Room and Device
  fields, leave Shelf, Rack, Position empty for user to optionally fill
  manually. This supports device-level storage assignments where shelves/racks
  aren't used.

- Q: Should the delimiter be configurable or always hyphen? → A: Fixed as hyphen
  (-) for consistency and simplicity. All location barcodes must use hyphen
  delimiter. This ensures predictable parsing and reduces configuration
  complexity. If system admin settings include delimiter configuration, it
  applies to other barcode types (samples, specimens) but location barcodes
  always use hyphen.

- Q: How should the system distinguish between a barcode scanner input vs manual
  typing in the "Enter / Scan" field? → A: No distinction needed - treat
  equally. Field accepts both scanner input (fast keyboard wedge entry with
  automatic Enter) and manual typing. Validation occurs on Enter key or field
  blur. If input matches location barcode format (contains hyphens, valid
  codes), parse as barcode. If input matches location name/code without
  delimiters, treat as type-ahead search query. Format-based logic, not
  input-method detection.

- Q: When both "Manual Select" dropdowns and "Enter / Scan" field are visible,
  what happens if user fills BOTH? → A: Last-modified wins. If user selects from
  dropdowns then scans/types in Enter field, the scan/type overwrites dropdown
  selections. If user scans/types then uses dropdowns, dropdown selections
  overwrite the scan/type values. Provide visual feedback showing which method
  is currently active (highlight border or icon). No error - seamless switching
  between methods.

- Q: What is the exact format pattern matching logic to distinguish sample
  barcodes from location barcodes? → A: If the format matches a lab accession
  number (like defined in the admin section), it will be treated as a lab/sample
  number, but there are no fields where there are both location barcodes and
  sample barcodes both being scanned. The search can look for strings that match
  either, if the search can be for a sample or a location.

- Q: If auto-detection fails (ambiguous barcode), how should the system respond?
  → A: Display error message "Unable to identify barcode type. Please verify the
  barcode format." Show the scanned value and provide option to manually enter
  using cascading dropdowns or type-ahead search. Log ambiguous barcode for
  admin review. This should be rare given clear format patterns defined in admin
  settings.

- Q: When showing "last scanned code in error message," should it show the raw
  barcode string or the parsed/interpreted components? → A: Show both for
  maximum clarity. Format: "Scanned code: MAIN-FRZ01-SHA-RKR1 (Room: MAIN,
  Device: FRZ01, Shelf: SHA, Rack: RKR1)" with the specific error below (e.g.,
  "Rack 'RKR1' not found in Shelf 'SHA'"). If parsing fails completely, show
  only raw string.

- Q: When a scan fails and user switches to cascading dropdown mode, should the
  failed barcode components be pre-populated in the dropdowns (if some
  components were valid)? → A: Yes - pre-fill valid components automatically. If
  Room code "MAIN" is valid but Device code "FRZ01" doesn't exist, pre-select
  Room="Main Laboratory" in dropdown and leave Device dropdown ready for manual
  selection. Show informational message: "Room pre-filled from scan. Please
  select Device." This reduces re-entry work and guides user to fix only the
  problematic component.

- Q: Should "Label Management" be a separate menu item or integrated into the
  existing "Edit" modal for devices/shelves/racks? → A: Separate menu item in
  the overflow menu. Menu items for devices/shelves/racks: Edit, Delete, Label
  Management. This keeps the Edit modal focused on entity properties (name,
  description, attributes) while Label Management handles barcode-specific
  operations (short code, printing). Clear separation of concerns.

- Q: After printing a label, should the system track print history? → A: Yes -
  record basic print audit trail: who printed, when (timestamp), for which
  location entity. Store in audit/history table. Display print history in Label
  Management modal as read-only list showing "Last printed: 2025-11-06 14:32 by
  John Smith" with optional "View History" link for full print log. Useful for
  compliance and troubleshooting label issues.

- Q: Can users print labels in bulk (e.g., select multiple racks and print all
  labels)? → A: Future enhancement - defer to post-POC. For POC, support
  one-at-a-time printing only through Label Management modal. Document
  requirement for bulk printing in future phase: select multiple
  devices/shelves/racks from dashboard table, right-click or bulk actions menu →
  "Print Labels", generate PDF with all labels for batch printing.

- Q: What specific settings are "inherited from the barcode setup in system
  administration"? → A: All of the above: (1) Label size/dimensions (e.g.,
  2"x1", 4"x2"), (2) Barcode format preference (Code 128, Code 39, QR Code -
  Code 128 default for locations), (3) Label template layout (barcode position,
  text size, margins). System admin defines these globally; Label Management
  inherits and applies them to generated labels.

- Q: Can users override inherited settings at print time, or are they fixed? →
  A: Fixed from system admin - no override at print time. This ensures
  consistency across all printed labels in the lab. If users need different
  label formats, system admin must create multiple barcode configurations and
  users select which configuration to use. Simplifies user workflow and
  maintains standardization for compliance.

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

### User Story 4 (P4) POC Scope Breakdown

**Included in POC**:

- ✅ Metrics cards (Total Samples, Active, Disposed counts)
- ✅ Storage Locations metric card (breakdown by type with color-coding)
- ✅ 5 tabs (Rooms, Devices, Shelves, Racks, Samples)
- ✅ Basic data tables (columns per tab as specified)
- ✅ Basic filters per tab
- ✅ Expandable rows (per Constitution V.7 amendment)

**Deferred to Post-POC**:

- ⏸️ Drill-down navigation (clicking location name to filter child levels)
- ⏸️ CSV export functionality
- ⏸️ Advanced occupancy color-coding (green/yellow/red)
- ⏸️ Visual grid view for racks/positions

**Rationale**: POC includes basic dashboard to validate that location data is
captured correctly and can be displayed for management review. Advanced features
(drill-down, export, visualization) deferred to ensure POC focuses on core
tracking workflows.

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
4. **Given** Maria attempts to assign a sample without selecting Room or Device,
   **When** she clicks Save with only Room selected (missing Device) or with
   neither Room nor Device selected, **Then** system displays validation error
   "A valid location requires at least Room and Device to be selected" and
   prevents assignment
5. **Given** Maria scans a rack barcode "MAIN-FRZ01-SHA-RKR1", **When** barcode
   scan completes, **Then** system auto-populates Room="Main Laboratory",
   Device="Freezer Unit 1", Shelf="Shelf-A", Rack="Rack R1" and focuses the
   Position field
6. **Given** Maria has selected Room and Device (and optionally Shelf/Rack),
   **When** she leaves the Position field blank, **Then** system allows
   rack-level assignment (position optional for shelf/rack-level storage)

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
2. He clicks the Actions overflow menu (⋮) → selects "Manage Location"
3. Location management modal opens (titled "Move Sample" since location exists)
   showing:
   - **Sample information**: Sample ID, Type, Status, Date Collected, Patient
     ID, Test Orders
   - **Current location**:
     `Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5`
   - **Location selector widget**: Same widget as assignment (cascading
     dropdowns / autocomplete / barcode scan) with Room, Device, Shelf, Rack,
     Position fields
   - **Condition Notes** field (optional)
4. David selects new location:
   `Main Laboratory > Refrigerator 2 > Shelf-1 > Rack R3 > Position C8`
5. "Reason for Move" field appears (since location exists and different location
   selected). David optionally enters reason: "Temporary storage for viral load
   testing"
6. System validates:
   - Target position C8 is currently empty (not occupied)
   - Refrigerator 2 is active (not decommissioned)
   - Rack R3 has available capacity
7. David clicks "Confirm Move" → System:
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
   - **Storage Locations**: "12 rooms, 45 devices, 89 shelves, 156 racks"
     (formatted text list showing breakdown by active location types,
     color-coded: rooms in blue-70, devices in teal-70, shelves in purple-70,
     racks in orange-70, with matching subtle accent colors on corresponding
     tabs)
3. Switches between **5 tabs** to view different hierarchy levels:

   **Rooms Tab**:

   - Shows: [Expand] | Name | Code | Devices (count) | Sample Items (count) |
     Status | Actions
   - Example row: [▶] | "Main Laboratory" | MAIN | 8 devices | 1,234 sample
     items | Active | [⋮]
   - Expandable row: Clicking expand icon (▶) reveals additional fields below
     row: Description, Created Date, Created By, Last Modified Date, Last
     Modified By (formatted as key-value pairs, read-only)

   **Devices Tab**:

   - Shows: [Expand] | Name | Code | Room | Type (badge) | Occupancy | Status |
     Actions
   - Example row: [▶] | "Freezer Unit 1" | FRZ01 | Main Laboratory | [freezer] |
     287/500 (57%) [progress bar] | Active | [⋮]
   - Type badges: "freezer", "fridge", "cabinet" (visual indicators)
   - Expandable row: Clicking expand icon reveals additional fields: Temperature
     Setting, Capacity Limit, Description, Created Date, Created By, Last
     Modified Date, Last Modified By (formatted as key-value pairs, read-only)

   **Shelves Tab**:

   - Shows: [Expand] | Label | Device | Room | Occupancy | Status | Actions
   - Example row: [▶] | "Shelf-A" | Freezer Unit 1 | Main Laboratory | 23/81
     (28%) [progress bar] | Active | [⋮]
   - Expandable row: Clicking expand icon reveals additional fields: Capacity
     Limit, Description, Created Date, Created By, Last Modified Date, Last
     Modified By (formatted as key-value pairs, read-only)

   **Racks Tab**:

   - Shows: [Expand] | Label | Shelf | Device | Room | Dimensions | Occupancy |
     Status | Actions
   - Example row: [▶] | "Rack R1" | Shelf-A | Freezer Unit 1 | Main Laboratory |
     9 × 9 | 23/81 (28%) [progress bar] | Active | [⋮]
   - Expandable row: Clicking expand icon reveals additional fields: Position
     Schema Hint, Description, Created Date, Created By, Last Modified Date,
     Last Modified By (formatted as key-value pairs, read-only)

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

## E2E Test Requirements

**Purpose**: E2E tests validate complete user workflows end-to-end. Tests focus
on happy path user journeys, NOT edge cases or validation errors (those are
unit/integration tests).

**Execution**: Per Constitution V.5, run tests individually during development
(max 5-10 per execution). Full suite only in CI/CD.

### User Story P1 - Basic Storage Assignment (3 tests)

- **E2E Test**: "Should assign sample via cascading dropdowns" (happy path)
- **E2E Test**: "Should assign sample via type-ahead autocomplete" (happy path)
- **E2E Test**: "Should assign sample via barcode scan" (happy path)

**Edge Cases** (unit/integration tests, NOT E2E):

- Inline location creation → unit test in StorageLocationServiceTest
- Capacity warnings → unit test for capacity calculation logic
- Position occupied errors → unit test in SampleStorageServiceTest
- Inactive location errors → integration test in
  StorageLocationRestControllerTest
- Validation errors → unit tests for each validation rule

### User Story P2A - Sample Search and Retrieval (2 tests)

- **E2E Test**: "Should search samples by accession number" (happy path)
- **E2E Test**: "Should filter samples by storage location" (happy path)

**Edge Cases** (unit/integration tests, NOT E2E):

- Search performance with 100k+ samples → integration test with database seeding
- Empty search results → unit test
- Multiple filter criteria → unit test for filter composition logic
- Clear filters functionality → unit test

### User Story P2B - Sample Movement (2 tests)

- **E2E Test**: "Should move single sample between locations" (happy path)
- **E2E Test**: "Should move multiple samples with auto-assigned positions"
  (happy path)

**Edge Cases** (unit/integration tests, NOT E2E):

- Concurrent access conflicts → integration test with transaction isolation
- Occupied position errors → unit test in SampleStorageServiceTest
- Disposed sample movement → unit test in SampleStorageServiceTest
- Insufficient capacity for bulk move → unit test
- Manual position editing during bulk move → unit test for UI validation logic

**Execution Command** (development):

```bash
# Run individual test file
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

# Full suite (CI/CD only)
npm run cy:run
```

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
- **FR-009**: **Position** entity MUST include: Free-text coordinate (optional,
  only required for 5-level positions), optional row/column integers for grid
  visualization, occupancy state (empty/occupied), parent device reference
  (required), optional parent shelf reference, optional parent rack reference. A
  position can have at most 5 levels (Room → Device → Shelf → Rack → Position)
  but at least 2 levels (Room → Device). The position represents the lowest
  level in the hierarchy for a sample assignment. Minimum requirement is device
  level (room
  - device); cannot be just a room. Position can be at: device level (2 levels),
    shelf level (3 levels), rack level (4 levels), or position level (5 levels).

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
  grid configured). Rack capacity is ALWAYS calculated (never uses static
  `capacity_limit` field - racks do not have this field)
- **FR-018**: Rack dimensions MUST be used for: (a) calculating total capacity,
  (b) optional grid visualization, (c) suggesting position coordinates (user can
  override)

#### Storage Location Selector Widget Structure

- **FR-018a**: Storage Location Selector Widget MUST support two-tier design:
  - **Compact inline view**: Displays selected location hierarchical path (or
    "Not assigned" if no location selected) with "Expand" or "Edit" button
  - **Expanded modal view**: Full location assignment form matching View Storage
    modal structure (sample info box, current location display, full assignment
    form with all selectors)
- **FR-018b**: Compact inline view MUST be used in both SamplePatientEntry
  (orders workflow) and LogbookResults (results workflow) - same widget
  component
- **FR-018c**: Compact inline view MUST display the selected location as
  hierarchical path text: `Room > Device > Shelf > Rack > Position` (or "Not
  assigned" if empty)
- **FR-018d**: Compact inline view MUST include an "Expand" or "Edit" button
  that opens the full location modal
- **FR-018e**: Results workflow compact inline view MUST include a quick-find
  search input field (type-ahead autocomplete) for rapidly finding existing
  locations
- **FR-018f**: Quick-find search MUST match location names/codes at any
  hierarchy level (Room, Device, Shelf, or Rack) using case-insensitive
  partial/substring matching
- **FR-018g**: Quick-find search results MUST display as flat list with full
  hierarchical path (e.g., "Main Laboratory > Freezer Unit 1 > Shelf-A")
- **FR-018h**: Selecting a location from quick-find results MUST populate the
  compact view with the selected location path and allow expanding to modal for
  position selection or notes
- **FR-018i**: Expanded modal view MUST match View Storage modal structure:
  sample information section, current location display, visual separator, full
  assignment form (barcode scan input, Room/Device/Shelf/Rack/Position
  selectors, condition notes), Cancel and "Assign Storage Location" buttons
- **FR-018j**: Barcode scanning functionality in expanded modal view MUST follow
  same specifications as FR-021, FR-021a, FR-021b, FR-021c (unified input field
  supporting scan/type-ahead with manual dropdown fallback)

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
- **FR-021a**: System MUST provide unified input field that supports both
  barcode scanning and type-ahead search. The same field accepts either scanned
  barcode input or manual type-ahead text entry. Format is the same for either
  entry method.
- **FR-021b**: System MUST display both input modes simultaneously: "Manual
  Select" mode (cascading dropdowns) and "Enter / Scan" field (unified
  barcode/type-ahead input). Both types of entry MUST be visible at the same
  time, though the user can fill either one. User can switch between modes or
  use both.
- **FR-021c**: System MUST allow manual dropdown fallback: If barcode scan fails
  or user prefers manual selection, user can enter manually using the cascading
  dropdown mode. Scanned code (if any) remains visible for reference. User can
  rescan to overwrite manual changes.
- **FR-022**: System MUST display current selection as hierarchical path below
  selector: `Room > Device > Shelf > Rack > Position`

#### Barcode Format and Handling

- **FR-023**: System MUST support hierarchical barcode format (see FR-023c for
  detailed data structure):
  - Device: `{room}-{device}` (e.g., "MAIN-FRZ01")
  - Shelf: `{room}-{device}-{shelf}` (e.g., "MAIN-FRZ01-SHA")
  - Rack: `{room}-{device}-{shelf}-{rack}` (e.g., "MAIN-FRZ01-SHA-RKR1")
- **FR-023a**: System MUST support USB HID barcode scanners operating in
  keyboard wedge mode (see Session 2025-11-06 for hardware requirements).
  Scanners emit rapid keyboard events (typically 30-50ms between characters)
  that are captured as standard keyboard input. No special hardware drivers or
  browser extensions required.
- **FR-023b**: System MUST support multiple barcode formats: Code 128, Code 39,
  EAN-13, QR codes, and Data Matrix (see Session 2025-11-06 for format details).
  Format detection is automatic based on scanned data structure and pattern
  matching.
- **FR-023c**: Location barcodes MUST use hierarchical path with delimiter
  format: Device format `{room}-{device}`, Shelf format
  `{room}-{device}-{shelf}`, Rack format `{room}-{device}-{shelf}-{rack}`.
  Delimiters are fixed as hyphens (-) for all location barcodes (not
  configurable). System MUST accept 2-level barcodes (Room+Device minimum) and
  5-level barcodes (including Position if encoded). Sample barcodes use
  accession number format (e.g., "S-2025-001"). System MUST parse barcode format
  and extract hierarchical components automatically (see FR-024a for validation
  process, Session 2025-11-06 for delimiter and level flexibility details).
- **FR-024**: Scanning rack barcode MUST auto-populate Room, Device, Shelf, Rack
  fields and focus Position field for manual entry (see FR-024a for validation
  process, FR-024b for dual barcode support, FR-024c for debouncing, FR-024d for
  visual feedback)
- **FR-024a**: System MUST implement 5-step validation process when barcode is
  scanned (see Session 2025-11-06 for validation details): (1) Parse barcode
  format and extract hierarchical components, (2) Validate barcode structure
  matches expected pattern (location or sample), (3) Lookup location/sample in
  database, (4) Verify location is active and accessible, (5) Check for
  conflicts (e.g., occupied position). Each step MUST provide specific error
  messages if validation fails.
- **FR-024b**: System MUST support dual barcode types with auto-detection (see
  Session 2025-11-06 for auto-detection details): Sample barcodes (format:
  accession number like "S-2025-001") trigger sample lookup and load sample
  details. Location barcodes populate location hierarchy. System distinguishes
  by format pattern matching. If sample barcode scanned, pre-fill sample
  context; if location barcode scanned, populate location selectors.
- **FR-024c**: System MUST implement debouncing with 500ms cooldown period after
  each scan (see Session 2025-11-06 for debouncing details). If duplicate
  barcode scanned within cooldown, ignore silently (no error). If different
  barcode scanned within cooldown, show warning "Please wait before next scan"
  and ignore input. Prevents accidental double-entry.
- **FR-024d**: System MUST provide visual feedback for barcode scans (see
  Session 2025-11-06 for feedback details): Dedicated barcode input field with
  icon indicator. On focus, show "Ready to scan" state with animation/pulse.
  Successful scan displays green checkmark with decoded path preview + smooth
  transition to populated fields. Failed scan displays red X with error message.
  Field MUST auto-clear after successful population of location fields. No audio
  feedback (visual only).
- **FR-024e**: System MUST handle input method detection: No distinction between
  barcode scanner input vs manual typing in "Enter / Scan" field - treat
  equally. Field accepts both scanner input (fast keyboard wedge entry with
  automatic Enter) and manual typing. Validation occurs on Enter key or field
  blur. If input matches location barcode format (contains hyphens, valid
  codes), parse as barcode. If input matches location name/code without
  delimiters, treat as type-ahead search query. Format-based logic, not
  input-method detection (see Session 2025-11-06 for input handling details).
- **FR-024f**: System MUST implement "last-modified wins" behavior when both
  "Manual Select" dropdowns and "Enter / Scan" field are visible: If user
  selects from dropdowns then scans/types in Enter field, the scan/type
  overwrites dropdown selections. If user scans/types then uses dropdowns,
  dropdown selections overwrite the scan/type values. Provide visual feedback
  showing which method is currently active (highlight border or icon). No
  error - seamless switching between methods (see Session 2025-11-06 for mode
  interaction details).
- **FR-024g**: System MUST display error messages with both raw barcode string
  and parsed components: Format "Scanned code: MAIN-FRZ01-SHA-RKR1 (Room: MAIN,
  Device: FRZ01, Shelf: SHA, Rack: RKR1)" with the specific error below (e.g.,
  "Rack 'RKR1' not found in Shelf 'SHA'"). If parsing fails completely, show
  only raw string (see Session 2025-11-06 for error message format details).
- **FR-024h**: System MUST pre-fill valid components automatically when scan
  fails and user switches to cascading dropdown mode: If Room code "MAIN" is
  valid but Device code "FRZ01" doesn't exist, pre-select Room="Main Laboratory"
  in dropdown and leave Device dropdown ready for manual selection. Show
  informational message: "Room pre-filled from scan. Please select Device." This
  reduces re-entry work and guides user to fix only the problematic component
  (see Session 2025-11-06 for pre-population details).
- **FR-025**: System MUST handle duplicate barcode labels with disambiguation
  dialog (e.g., two racks labeled "R1" in different devices)
- **FR-026**: System MUST generate printable labels for Device, Shelf, Rack
  levels including human-readable text and barcode
- **FR-027**: System MUST support printing individual or batch labels
- **FR-027a**: System MUST provide label management modal accessible as separate
  menu item in overflow menu for Devices, Shelves, and Racks (see Session
  2025-11-06 for menu structure). Modal MUST be titled "Label Management" and
  display two fields: Short Code (text input) and Print Label (button/action).
- **FR-027b**: Short Code field MUST be unique within its context (e.g., one
  "shelf1" per device). System MUST validate uniqueness before allowing save.
  Short code format MUST be: maximum 10 characters, alphanumeric only (A-Z, 0-9,
  hyphen and underscore allowed), auto-uppercase all input for consistency,
  manual entry only (no auto-generate), must start with a letter or number (not
  hyphen/underscore). Short code is used in barcode generation and MUST follow
  same format constraints as location codes (see Session 2025-11-06 for format
  details).
- **FR-027c**: Print Label functionality MUST send directly to configured
  default barcode printer from system administration settings. If no default
  printer is configured, show printer selection dialog. Generate label using
  barcode format and size specified in system admin settings (inherited: label
  size/dimensions, barcode format preference with Code 128 default for
  locations, label template layout). Labels MUST include human-readable text and
  barcode encoding of the hierarchical path (or short code if configured). Show
  preview of PDF label in new tab. Settings are fixed from system admin - no
  override at print time (see Session 2025-11-06 for inheritance details).
- **FR-027d**: System MUST display blocking confirmation dialog before allowing
  short code changes with two options: (1) "Cancel" - abort the change and keep
  existing short code, (2) "Proceed" - save the new short code with warning
  acknowledged. After proceeding, display informational message listing all
  affected location levels that need label reprinting (e.g., "Labels need
  reprinting for: Shelf-A, Rack R1, Rack R2, Position A1-A10"). No automatic
  re-print trigger (see Session 2025-11-06 for dialog details).
- **FR-027e**: System MUST track print history: record basic print audit trail
  (who printed, when/timestamp, for which location entity) in audit/history
  table. Display print history in Label Management modal as read-only list
  showing "Last printed: [date] [time] by [user]" with optional "View History"
  link for full print log (see Session 2025-11-06 for audit trail details).
- **FR-027f**: Bulk label printing MUST be deferred to post-POC. For POC,
  support one-at-a-time printing only through Label Management modal. Future
  requirement: select multiple devices/shelves/racks from dashboard table, bulk
  actions menu → "Print Labels", generate PDF with all labels for batch printing
  (see Session 2025-11-06 for bulk printing details).

#### Inline Location Creation (Widget-Based)

**Context**: Available within the expanded modal view of the Storage Location
Selector widget on Sample Patient Entry (orders) and Logbook Results pages to
enable quick location creation during sample assignment workflow.

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

#### SampleItem Assignment

- **FR-033**: System MUST record sample item assignment with: SampleItem ID,
  Sample ID (parent Sample reference for context), Location
  (room/device/shelf/rack/position), Assigned By (user ID), Timestamp, Optional
  notes. Storage tracking operates at SampleItem level (physical specimens), not
  Sample level (orders).
- **FR-033a**: System MUST require that a valid location for a sample item has
  at least 2 levels set: Room and Device MUST be selected. Shelf, Rack, and
  Position levels are optional (shelf/rack/position may be left blank). A
  position can have at most 5 levels (Room → Device → Shelf → Rack → Position)
  but at least 2 levels (Room → Device). A sample item is associated with a
  position that represents the lowest level in the hierarchy for that
  assignment. The position can be at device level (2 levels), shelf level (3
  levels), rack level (4 levels), or position level (5 levels). The requirement
  is that it must be at least at the device level (cannot be just a room). When
  assigning, we select the lowest position in the hierarchy for the given sample
  item, which provides all necessary location information.
- **FR-033b**: System MUST allow users to select which SampleItem to assign when
  a Sample has multiple SampleItems. Dashboard and assignment workflows are
  SampleItem-specific, with parent Sample information displayed for context and
  sorting/grouping capabilities.
- **FR-034**: System MUST prevent assignment to already-occupied position
  (unless rack allows duplicates - see FR-014)
- **FR-035**: System MUST prevent assignment to inactive/decommissioned location
- **FR-036**: System MUST display capacity warnings at fixed thresholds: 80%,
  90%, and 100% capacity with message "[Location] is [percentage]% full.
  Consider using alternative storage." System MUST allow assignment even at or
  above 100% capacity (no hard block). Capacity warnings apply to both manual
  `capacity_limit` values and calculated capacities (per FR-062a). Warnings MUST
  NOT be displayed when capacity cannot be determined (per FR-062b)
- **FR-037**: System MUST allow assignment at shelf/rack level without
  specifying position (position field blank)

#### SampleItem Row Actions Menu

- **FR-037a**: SampleItems table rows MUST include an overflow menu button
  (triple-dot icon, ⋮) in the Actions column. Each row represents a SampleItem
  (physical specimen), with parent Sample information displayed as secondary
  context.
- **FR-037b**: Overflow menu MUST display three menu items: Manage Location,
  Dispose, View Audit (placeholder). "Manage Location" consolidates the previous
  "Move" and "View Storage" functionality into a single unified modal
- **FR-037c**: Overflow menu MUST use Carbon Design System OverflowMenu
  component
- **FR-037d**: Menu items MUST be accessible via keyboard navigation and screen
  readers
- **FR-037e**: "View Audit" menu item MUST be marked as placeholder (disabled or
  with visual indicator) until audit functionality is implemented

#### Location Row Actions Menu (Rooms, Devices, Shelves, Racks)

- **FR-037f**: Rooms, Devices, Shelves, and Racks table rows MUST include an
  overflow menu button (triple-dot icon, ⋮) in the Actions column
- **FR-037g**: Overflow menu MUST display two menu items: Edit, Delete (all
  important details are visible in table columns, no separate View Details
  needed)
- **FR-037h**: Overflow menu MUST use Carbon Design System OverflowMenu
  component
- **FR-037i**: Menu items MUST be accessible via keyboard navigation and screen
  readers

#### Location Edit Modal

- **FR-037j**: Selecting "Edit" from overflow menu MUST open a modal dialog with
  full form for editing all location entity fields
- **FR-037k**: Edit modal MUST use Carbon Design System Modal component with
  proper accessibility attributes
- **FR-037l**: Edit modal MUST display all editable fields for the location
  type:
  - **Room**: Name (editable), Code (read-only), Description (optional,
    editable), Active/Inactive status (editable)
  - **Device**: Name (editable), Code (read-only), Type (editable), Temperature
    setting (optional, editable), Capacity limit (optional, editable),
    Active/Inactive status (editable), Parent Room (read-only)
  - **Shelf**: Label (editable), Capacity limit (optional, editable),
    Active/Inactive status (editable), Parent Device (read-only)
  - **Rack**: Label (editable), Dimensions (rows, columns, editable), Position
    schema hint (optional, editable), Active/Inactive status (editable), Parent
    Shelf (read-only)
- **FR-037l1**: Code and Parent relationship fields MUST be read-only in Edit
  modal to prevent structural changes that could break references or hierarchy
  integrity
- **FR-037m**: Edit modal MUST validate code uniqueness within parent scope and
  parent-child relationships before saving
- **FR-037n**: Edit modal MUST display Cancel and "Save Changes" buttons in
  footer
- **FR-037o**: Edit modal MUST preserve table context (user remains on same tab
  after closing modal)

#### Location Delete Operation

- **FR-037p**: Selecting "Delete" from overflow menu MUST validate constraints
  before allowing deletion
- **FR-037q**: System MUST prevent deletion if location has child locations
  (e.g., cannot delete Room with Devices, cannot delete Device with Shelves,
  cannot delete Shelf with Racks)
- **FR-037r**: System MUST prevent deletion if location has active samples
  assigned (samples currently stored at that location or any child location)
- **FR-037s**: System MUST display error message if deletion is blocked due to
  constraints, indicating the specific reason (e.g., "Cannot delete Room 'Main
  Laboratory' because it contains 8 devices" or "Cannot delete Device 'Freezer
  Unit 1' because 287 active samples are stored there")
- **FR-037t**: If no constraints exist, system MUST display confirmation dialog
  before deletion with warning message (e.g., "Are you sure you want to delete
  [Location Name]? This action cannot be undone.")
- **FR-037u**: Confirmation dialog MUST use Carbon Design System Modal component
  with destructive action styling for confirm button
- **FR-037v**: After successful deletion, system MUST refresh table data and
  display success notification

#### Consolidated Location Management Modal

- **FR-038**: Users MUST be able to initiate location management from dashboard
  or sample detail view via overflow menu "Manage Location" action (consolidates
  previous "Move" and "View Storage" functionality)
- **FR-039**: Location management modal MUST use same location selector widget
  as initial assignment (dropdown/autocomplete/scan)
- **FR-040**: Location management modal MUST show current location (if assigned)
  and location selector widget for new location selection
- **FR-040a**: Location management modal title and button wording MUST be
  dynamic based on whether sample has existing location:
  - **If no location assigned**: Modal title "Assign Storage Location", button
    text "Assign"
  - **If location exists**: Modal title "Move Sample Item" with subtitle "Move
    sample item [SampleItem ID] (Sample: [Sample ID]) to a new storage
    location", button text "Confirm Move"
- **FR-040b**: Location management modal MUST display comprehensive sample item
  information section showing: SampleItem ID/External ID, Sample ID (parent
  Sample accession number), Type, Status, Date Collected, Patient ID, Test
  Orders in a highlighted/background box
- **FR-040c**: Location management modal MUST display "Current Location" section
  (if location exists) showing full hierarchical path (Room > Device > Shelf >
  Rack > Position) in a highlighted gray background box. If no location exists,
  this section MUST NOT be displayed
- **FR-040d**: Location management modal MUST display a visual separator
  (downward-pointing arrow icon if location exists, or horizontal line if no
  location) between current location and location selection form
- **FR-040e**: Location management modal MUST display location selection form in
  a bordered box containing:
  - Barcode scan input field (Quick Assign) - MUST follow specifications in
    FR-021, FR-021a, FR-021b, FR-021c (unified input field supporting
    scan/type-ahead with manual dropdown fallback)
  - Room dropdown selector (required, marked with \*)
  - Device dropdown selector
  - Shelf dropdown selector
  - Rack/Box dropdown selector
  - Position text input field (optional, with format hint)
  - Condition Notes textarea (optional)
- **FR-040f**: Location management modal MUST display "Selected Location"
  preview section showing the selected hierarchical path in gray background box
  (displays "Not selected" until location is chosen)
- **FR-040g**: Location management modal MUST display "Reason for Move" textarea
  field ONLY when: (1) sample has existing location AND (2) user selects a
  different location. Field is optional (not required) and labeled "Reason for
  Move (optional)"
- **FR-040h**: Location management modal MUST display Cancel and action button
  in footer with dynamic text based on location existence ("Assign" if no
  location, "Confirm Move" if location exists). Action button uses primary/dark
  styling
- **FR-040i**: Location management modal MUST use Carbon Design System Modal
  component with proper accessibility attributes
- **FR-041**: System MUST validate: Target location is active, target position
  is not occupied, target has available capacity
- **FR-042**: System MUST update sample item's current location to new location
  and free previous position (mark as available) when location is changed
- **FR-043**: System MUST record audit trail with: Previous location (if
  existed), New location, User, Timestamp, Reason (if provided when moving)
- **FR-044**: Dashboard MUST update immediately after location assignment or
  move completes

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

- **FR-051a**: Disposal modal MUST be titled "Dispose Sample Item" with subtitle
  "Permanently dispose of sample item [SampleItem ID] (Sample: [Sample ID])"
- **FR-051b**: Disposal modal MUST display a red warning alert box at the top
  stating "This action cannot be undone. The sample will be marked as disposed
  and removed from storage." (uses warning/error styling with icon)
- **FR-051c**: Disposal modal MUST display sample item information section in
  gray background box showing: SampleItem ID/External ID, Sample ID (parent
  Sample), Type, and Status
- **FR-051d**: Disposal modal MUST display "Current Storage Location" section
  with location pin icon showing:
  - Full hierarchical path in gray background box
  - Helper text below: "Sample will be removed from this location upon disposal"
- **FR-051e**: Disposal modal MUST display a disposal instructions info box
  (blue/info styling) showing sample-specific disposal instructions (e.g.,
  "Biohazard waste - autoclave at 121°C for 30 minutes before disposal")
- **FR-051f**: Disposal modal MUST include a horizontal separator line between
  location info and disposal form fields
- **FR-051g**: Disposal modal MUST display required fields:
  - "Disposal Reason \*" dropdown (required, marked with asterisk, initially
    shows "Select reason..." placeholder)
  - "Disposal Method \*" dropdown (required, marked with asterisk, initially
    shows "Select method..." placeholder)
- **FR-051h**: Disposal modal MUST display "Additional Notes (optional)"
  textarea field
- **FR-051i**: Disposal modal MUST require a confirmation checkbox with text: "I
  confirm that I want to permanently dispose of this sample. This action cannot
  be undone."
- **FR-051j**: Disposal modal MUST display Cancel and "Confirm Disposal" buttons
  in footer, with "Confirm Disposal" button:
  - Using red/destructive action styling (e.g., rgba(231,0,11,0.6) background)
  - Disabled (opacity 50%) until confirmation checkbox is checked
  - Enabled only when checkbox is checked
- **FR-051k**: Disposal modal MUST use Carbon Design System Modal component with
  proper accessibility attributes

- **FR-052**: System MUST set disposed sample status to "Disposed" (irreversible
  state change)
- **FR-053**: System MUST clear disposed sample's current location (position
  becomes available)
- **FR-054**: System MUST create immutable audit record with all disposal
  details
- **FR-055**: System MUST prevent future assignment/movement of disposed samples
- **FR-056**: Disposed samples MUST remain viewable for audit purposes but
  non-editable
- **FR-056a**: Disposal workflow MUST be initiated via overflow menu "Dispose"
  action

#### Dashboard and Reporting

- **FR-057**: Dashboard MUST display 4 metric cards: Total SampleItems (count of
  all sample items with locations), Active (currently stored), Disposed
  (disposed sample items), Storage Locations (formatted text list showing
  breakdown by type: "X rooms, Y devices, Z shelves, W racks" with counts for
  each active hierarchy level, color-coded using Carbon Design System tokens:
  blue-70 for rooms, teal-70 for devices, purple-70 for shelves, orange-70 for
  racks)
- **FR-057a**: Storage Locations metric card text MUST be color-coded with
  matching subtle accent colors applied to corresponding tab labels/backgrounds
  (Rooms tab has blue accent, Devices tab has teal accent, Shelves tab has
  purple accent, Racks tab has orange accent) - tab coloring must be very subtle
- **FR-058**: Dashboard MUST provide 5 tabs: SampleItems | Rooms | Devices |
  Shelves | Racks. SampleItems tab displays SampleItem-level data (physical
  specimens), with parent Sample information displayed as secondary context and
  sortable by Sample to easily see sample items together.
- **FR-059**: Each tab MUST show data table appropriate for that entity level
  with relevant columns
- **FR-059a**: Location tables (Rooms, Devices, Shelves, Racks) MUST support
  expandable rows using Carbon DataTable expandable row pattern
- **FR-059b**: Expandable rows MUST be triggered by clicking chevron/expand icon
  in a dedicated column (first column, Carbon DataTable standard)
- **FR-059c**: Expanded row content MUST display all entity fields not visible
  in table columns, formatted as key-value pairs in read-only format
- **FR-059d**: Only one row can be expanded at a time (expanding another row
  automatically collapses the previously expanded row)
- **FR-059e**: Expanded row content MUST be read-only (Edit action remains in
  overflow menu, no inline editing in expanded view)
- **FR-059f**: Expanded row MUST show entity-specific additional fields:
  - **Rooms**: Description, Created Date, Created By, Last Modified Date, Last
    Modified By
  - **Devices**: Temperature Setting, Capacity Limit, Description, Created Date,
    Created By, Last Modified Date, Last Modified By
  - **Shelves**: Capacity Limit, Description, Created Date, Created By, Last
    Modified Date, Last Modified By
  - **Racks**: Position Schema Hint, Description, Created Date, Created By, Last
    Modified Date, Last Modified By
- **FR-060**: Tab selection state MUST be visually distinct (active tab
  highlighted)
- **FR-060a**: Dashboard MUST provide action buttons positioned to the right of
  the tabs: "Add Location" button (navigates to location management form page)
  and "Export" button (exports current filtered table data to CSV), both visible
  on all tabs

#### Occupancy Display

- **FR-061**: Devices, Shelves, Racks tabs MUST display occupancy as: Fraction
  (occupied/total) + Percentage + Visual progress bar. If capacity cannot be
  determined (see FR-062b), display "N/A" or "Unlimited" with a tooltip
  explaining why capacity cannot be determined (e.g., "Capacity cannot be
  calculated: some child locations lack defined capacities")
- **FR-062**: Occupancy calculation MUST be: (count of occupied positions /
  total capacity) × 100, where total capacity is determined per FR-062a and
  FR-062b
- **FR-062a**: Capacity determination MUST follow hierarchical two-tier logic:
  - **Racks**: Capacity is ALWAYS calculated as rows × columns (per FR-017). If
    rows=0 OR columns=0, capacity=0 (no grid, rack-level assignment only)
  - **Devices and Shelves**: If `capacity_limit` is set (static/manual limit),
    use that value as total capacity. Otherwise, calculate capacity from child
    locations per FR-062b
- **FR-062b**: When `capacity_limit` is NULL for a Device or Shelf, capacity
  MUST be calculated from child locations using the following logic:
  - If ALL child locations (shelves for devices, racks for shelves) have defined
    capacities (either static `capacity_limit` set OR calculated capacity from
    their own children), sum those capacities to determine parent capacity
  - If ANY child location lacks a defined capacity (no `capacity_limit` set AND
    cannot calculate from its children), parent capacity cannot be determined
    and occupancy MUST display "N/A" or "Unlimited" per FR-061
  - Racks always have defined capacity (rows × columns), so they can always be
    summed into parent capacity if needed
- **FR-062c**: UI MUST visually distinguish between manual/static capacity
  limits and calculated capacities (e.g., badge, tooltip, or icon) to help users
  understand whether capacity is user-defined or system-calculated
- **FR-063**: Visual progress bar MUST show proportional fill (e.g., 57% filled
  bar for 287/500). If capacity cannot be determined, progress bar MUST be
  hidden and "N/A" or "Unlimited" text displayed instead

#### Filters and Search

- **FR-064**: Dashboard MUST provide tab-specific search functionality:
  - **SampleItems tab**: Live search (debounced 300-500ms) by SampleItem
    ID/External ID, Sample accession number (parent Sample), and assigned
    location (full hierarchical path string). Search matches any of these fields
    (OR logic) using case-insensitive partial/substring matching. Primary
    identifier is SampleItem ID/External ID, with Sample accession number
    displayed as secondary context.
  - **Rooms tab**: Search by name and code using case-insensitive
    partial/substring matching
  - **Devices tab**: Search by name, code, and type using case-insensitive
    partial/substring matching
  - **Shelves tab**: Search by name (label) using case-insensitive
    partial/substring matching
  - **Racks tab**: Search by name (label) using case-insensitive
    partial/substring matching
- **FR-064a**: Search operations MUST update results in real-time with debounced
  delay (300-500ms after typing stops) for samples tab, and MAY use debounced or
  submit-button search for other tabs
- **FR-065**: Dashboard MUST provide tab-specific filters:
  - **Samples tab**: Filter by location (single smart dropdown with autocomplete
    and hierarchical browsing) and by status
  - **Rooms tab**: Filter by status
  - **Devices tab**: Filter by type and room and status
  - **Shelves tab**: Filter by device and room and status
  - **Racks tab**: Filter by room, filter by shelf, device, and status
- **FR-065b**: Samples tab location filter MUST be a single dropdown that
  supports:
  - Autocomplete search across hierarchy levels Room, Device, Shelf, and Rack
    (Position-level excluded - users filter by rack then find position in table)
  - Search matches location names/codes at any included hierarchy level (e.g.,
    typing "Freezer" matches "Freezer Unit 1" regardless of which room it's in)
  - Search results displayed as flat list with full hierarchical path (e.g.,
    "Main Laboratory > Freezer Unit 1 > Shelf-A")
  - Hierarchical browsing via tree view with expand/collapse (like file
    explorer) - users can expand/collapse parent nodes to navigate children
    (Room → Device → Shelf → Rack)
  - Combination mode: tree view for browsing, flat autocomplete list for search
    results
  - Selection of any included hierarchy level (Room, Device, Shelf, or Rack)
  - When a location is selected, filter shows all samples within that location's
    hierarchy (downward inclusive, including all positions within selected rack)
  - Inactive/decommissioned locations MUST appear in dropdown but be visually
    distinguished (grayed out, disabled, or with "Inactive" badge) to allow
    filtering of historical samples
- **FR-065a**: Dashboard MUST display room column in Racks tab table (shows
  parent room for each rack)
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

- **FR-080a**: System MUST validate that Room and Device are selected before
  allowing sample assignment (minimum 2 levels required for valid location)
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
  - **Functional Impact**: SampleItem-to-location link via Specimen.container
    (each SampleItem's storage location maps to its corresponding FHIR Specimen
    resource container reference)
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

- **SampleItem Assignment**: Link between sample item and storage location.
  Attributes: SampleItem ID (reference to existing SampleItem entity), Sample ID
  (reference to parent Sample for context), Location reference
  (room/device/shelf/rack/position), Assigned by (user ID), Assignment
  timestamp, Optional notes. Represents current location of sample item
  (physical specimen). Each SampleItem can be stored independently, even when
  multiple SampleItems belong to the same parent Sample.

- **SampleItem Movement**: Audit record of sample item relocation. Attributes:
  SampleItem ID, Sample ID (parent Sample reference), Previous location (full
  hierarchy path), New location (full hierarchy path), Moved by (user ID),
  Movement timestamp, Optional reason. Immutable audit trail.

- **SampleItem Disposal**: Audit record of sample item disposal. Attributes:
  SampleItem ID, Sample ID (parent Sample reference), Location at disposal time,
  Disposed by (user ID), Disposal timestamp, Reason (dropdown value), Method
  (dropdown value), Optional notes, Optional certificate attachment. Immutable
  compliance record.

- **Storage Location Barcode**: Pre-generated barcodes for physical labels.
  Attributes: Barcode value (hierarchical format), Location reference
  (device/shelf/rack), Generated by (user ID), Generation timestamp, Print
  status. Enables barcode scanning workflow.

### Integration Points

- **INT-001**: Add **Storage Location Selector Widget** in existing
  `SamplePatientEntry` screen (sample reception workflow)

  - Placement: After sample collection fields, before Save button
  - Behavior: Optional assignment (can assign later if needed)
  - Widget structure: Compact inline view showing selected location path (or
    "Not assigned") with "Expand" or "Edit" button that opens full location
    modal
  - Modal structure: Same as Consolidated Location Management Modal - shows
    comprehensive sample info box, current location display (if exists), full
    assignment form (barcode scan, Room/Device/Shelf/Rack/Position selectors,
    condition notes, reason for move when applicable)
  - Integration: Reuses existing form validation and save mechanism

- **INT-002**: Add **Storage Location Widget** in existing `LogbookResults`
  expanded view (sample search/results workflow)

  - Placement: Below existing referral/test result fields in expanded sample
    details
  - Behavior: Shows current location (read-only or editable based on
    permissions), allows location management action
  - Widget structure: Same compact inline view as INT-001 with "Expand" or
    "Edit" button that opens full location modal
  - Modal structure: Same as Consolidated Location Management Modal - shows
    comprehensive sample info box, current location display (if exists), full
    assignment form
  - Quick-add capability: Compact inline view includes quick-find search input
    (type-ahead autocomplete) for rapidly finding and selecting existing
    locations at any hierarchy level (Room, Device, Shelf, or Rack)
  - Integration: Adapts behavior based on user permissions and context

- **INT-003**: Create **Reusable Storage Location Selector Component**

  - Used in: SamplePatientEntry (orders workflow), LogbookResults (results
    workflow), Storage Dashboard, Consolidated Location Management Modal
  - Widget structure: Two-tier design:
    - **Compact inline view**: Displays selected location path (or "Not
      assigned") with "Expand"/"Edit" button
    - **Expanded modal view**: Full location assignment form matching
      Consolidated Location Management Modal structure (comprehensive sample
      info, current location if exists, barcode scan,
      Room/Device/Shelf/Rack/Position selectors, condition notes, reason for
      move when applicable)
  - Modes: Compact inline, Expanded modal
  - Features:
    - Compact view: Quick location path display, expand button
    - Modal view: Cascading dropdowns, type-ahead autocomplete, barcode scan,
      inline location creation, condition notes
  - Results workflow enhancement: Compact view includes quick-find search input
    (type-ahead autocomplete) for rapidly finding existing locations at any
    hierarchy level (Room, Device, Shelf, or Rack) - matches location
    names/codes and displays full hierarchical path
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
  - Link sample items to locations via Specimen.container reference (each
    SampleItem's storage location maps to its corresponding FHIR Specimen
    resource container reference)
  - Use existing FhirPersistanceService for creating/updating FHIR resources
  - Use existing FhirTransformService for entity↔FHIR conversion
  - Support IHE mCSD queries for location discovery

- **INT-005**: Leverage existing **audit logging infrastructure**

  - Use existing audit table pattern (sys_user_id, lastupdated columns)
  - Extend for storage-specific actions (assign, move, dispose)
  - Retain logs for 7+ years per existing compliance configuration

- **INT-006**: Leverage existing **UI/UX patterns**
  - Tab navigation (Carbon Tabs component, used in multiple screens)
  - Data tables (Carbon DataTable with pagination, sorting, filtering,
    expandable rows)
  - Modal dialogs (Carbon Modal for confirmations, forms)
  - Overflow menu (Carbon OverflowMenu for row actions)
  - Form validation (existing validation utilities)
  - Internationalization (React Intl message key system)

### Assumptions

- **Assumption 1**: Existing OpenELIS SampleItem entity has unique SampleItem ID
  suitable for foreign key reference. Storage tracking operates at SampleItem
  level (physical specimens), not Sample level (orders). Each SampleItem can be
  stored independently.
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

- **SC-001**: **Storage Assignment Workflow** - User can assign a sample item to
  a storage location using any of three methods (cascading dropdowns, type-ahead
  search, barcode scan), and assignment is saved with correct location path,
  user ID, and timestamp. Storage tracking operates at SampleItem level
  (physical specimens), with parent Sample information displayed for context.

- **SC-002**: **SampleItem Search and Retrieval** - User can search for a sample
  item by SampleItem ID/External ID or Sample accession number, and system
  displays the complete hierarchical storage location path (Room > Device >
  Shelf > Rack > Position). Search matches either identifier.

- **SC-003**: **SampleItem Movement** - User can move a sample item from one
  storage location to another, previous location is freed, new location is
  recorded, and audit trail captures the movement with user, timestamp, and
  reason

- **SC-004**: **Bulk Movement** - User can select multiple sample items and move
  them together, system auto-assigns sequential positions with option to modify,
  and each sample item receives individual audit record

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
