# Feature Specification: ASTM Analyzer Field Mapping

**Feature Branch**: `OGC-49-astm-analyzer-mapping`  
**Created**: 2025-11-14  
**Status**: Draft  
**Input**: User description: "ASTM Analyzer Mapping Feature based on OGC-49
analyzer mapping specification and Figma Analyzer Field Mapping designs."

Primary reference artifacts (non-exhaustive):

- OGC-49 analyzer mapping specification
  (`.dev-docs/OGC-49/OpenELIS_ASTM_Analyzer_Mapping_Specification.*`)
- Figma Analyzer Field Mapping Make:
  [`Analyzer-Field-Mapping-Feature`](https://www.figma.com/make/QseQZxQyOWsqciEpLjwkxb/Analyzer-Field-Mapping-Feature?node-id=0-1&t=wPkVXZVaIRyqh4SR-1)
- Figma OGC-49 design pages:
  [`OGC-49`](https://www.figma.com/design/i63dxlyfZE8tvdoAibH55M/OGC-49?node-id=0-1&m=dev)
  and related analyzer mapping flows

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Configure field mappings for a new ASTM analyzer (Priority: P1)

An OpenELIS system/laboratory interface administrator needs to configure an ASTM
analyzer so that all test orders and results sent by the instrument are
correctly mapped to OpenELIS tests, analytes, and result fields, without manual
re-entry.

**Why this priority**: Without correct mappings, analyzer results cannot safely
flow into OpenELIS; this is a go-live blocker for any automated analyzer
integration.

**Independent Test**: A tester can start from a clean system where an ASTM
analyzer has no mappings configured, follow the mapping UI to link analyzer test
codes, units, and qualitative values to OpenELIS fields, and then send sample
ASTM messages to confirm they are automatically interpreted into correct
OpenELIS orders/results without manual intervention.

**Acceptance Scenarios**:

1. **Given** an ASTM analyzer is registered in OpenELIS but has no field
   mappings, **When** the administrator opens the analyzer mapping UI and maps
   each analyzer test code to an OpenELIS test/analyte and saves, **Then**
   subsequent ASTM messages using those test codes are accepted and routed to
   the correct OpenELIS test context.
2. **Given** an analyzer test includes quantitative results with specific units,
   **When** the administrator maps analyzer units to OpenELIS canonical units
   (and, if required, a conversion rule) and saves, **Then** incoming results
   display in OpenELIS with the expected unit and value in the patient record.

---

### User Story 2 - Maintain mappings as instruments and test menus change (Priority: P2)

An OpenELIS system/lab administrator needs to safely update mappings when the
analyzer vendor adds new tests, changes test codes, or modifies result formats,
while preserving auditability and minimizing disruption to live message
processing.

**Why this priority**: Analyzer configurations evolve over time; the system must
support controlled changes without breaking production workflows or compromising
data integrity.

**Independent Test**: A tester can simulate a change in analyzer test codes or
result formats, update mappings in the UI, and verify that existing mapped tests
continue to work while new/changed tests are routed correctly, with all changes
reflected in an audit trail.

**Acceptance Scenarios**:

1. **Given** an analyzer test code has been remapped by an administrator,
   **When** new ASTM messages using the new configuration arrive, **Then** they
   are processed according to the updated mapping and the audit log shows who
   changed what and when.
2. **Given** an analyzer vendor adds a new test, **When** the administrator
   imports or manually defines the new analyzer field and maps it to an OpenELIS
   test, **Then** messages for the new test are accepted and appear in the
   correct worklists without impacting existing mappings.

---

### User Story 3 - Resolve unmapped or failed analyzer messages (Priority: P3)

An interface administrator or senior technologist needs to see ASTM messages
that could not be processed due to missing or ambiguous mappings, correct the
mappings, and then reprocess the affected messages.

**Why this priority**: Real-world analyzers will occasionally send messages with
new or unexpected codes; the lab must be able to resolve these quickly to
prevent result delays.

**Independent Test**: A tester can generate ASTM messages with unmapped test
codes or result values, observe that they are held for review rather than
silently failing, configure the necessary mappings, and then confirm that the
impacted messages can be reprocessed successfully.

**Acceptance Scenarios**:

1. **Given** an ASTM message contains a test code that has no mapping, **When**
   the system receives the message, **Then** it is held in an error queue and
   clearly surfaced in the Error Dashboard with enough context (raw code,
   analyzer, date/time, message segment) for the administrator to open a mapping
   interface modal and create the necessary mapping directly from the error
   context.
2. **Given** a set of previously unmapped messages displayed in the Error
   Dashboard, **When** the administrator opens the unmapped item modal, creates
   the missing mappings using the provided mapping interface, and triggers
   reprocessing, **Then** those messages are converted into OpenELIS
   orders/results according to the new mappings and are removed from the
   unresolved list.

---

### Edge Cases

- Analyzer sends the same test code for different configurations (e.g.,
  different specimen types or panels) and a single code could map to multiple
  OpenELIS tests—system must guide the user to avoid ambiguous or unsafe
  mappings.
- Analyzer units differ from OpenELIS canonical units (e.g., mg/dL vs mmol/L)
  and the mapping requires conversion factors or explicit rejection if safe
  conversion is not defined.
- Analyzer sends new qualitative values (e.g., "Reactive", "+", "Trace") that
  are not yet mapped to OpenELIS coded results.
- Analyzer messages arrive while a mapping edit is in progress (unsaved); the
  system must use the last approved mapping and clearly indicate when new
  mappings become active.
- An analyzer is decommissioned or temporarily disabled; existing mappings must
  not be applied to messages from a different instrument that happens to share
  similar codes.

## Requirements _(mandatory)_

### UI Component Patterns

The following UI patterns MUST be used consistently across all analyzer-related
pages:

**Statistics Cards Pattern**: Statistics cards MUST use Carbon Grid with
full-width layout, equal-width columns (3-4 columns depending on metric count),
aligned with table/content edges for visual consistency. Each card MUST use
color-coding via Carbon design tokens (e.g., `$blue-60`, `$green-60`, `$red-60`,
`$gray-60`) and thematic icons from `@carbon/icons-react` (e.g., Analytics,
CheckmarkFilled, ErrorFilled, WarningAltFilled, Time). Cards MUST span the same
width as the data table below using single-row layout. This pattern is
referenced in FR-001 (Analyzers List statistics) and FR-016 (Error Dashboard
statistics).

### Functional Requirements

- **FR-001**: System MUST provide an Analyzers List page with a searchable,
  filterable data table displaying all analyzers (navigation integration
  specified in FR-020). The page MUST include: a page header with "Analyzers"
  title and "Add Analyzer" primary action button, a statistics section with
  3-column grid displaying cards for Total Analyzers, Active Analyzers, and
  Inactive Analyzers. The statistics cards MUST span the same width as the
  table/content below, using a single-row layout with equal-width cards (3
  columns), aligned with table edges for visual consistency. Each card MUST use
  color-coding and thematic icons: Total Analyzers (blue Carbon token +
  Analytics icon from @carbon/icons-react), Active (green Carbon token +
  CheckmarkFilled icon), Inactive (gray Carbon token + WarningAlt icon). A
  search bar with filter dropdowns (Status, Test Unit, Analyzer Type) with 300ms
  debounce, active filter pills (dismissible tags) below the search bar,
  sortable columns (Name, Type, Connection (IP:Port), Test Units, Status, Last
  Modified, Actions), default sort by Last Modified (descending), overflow menu
  for row actions (Field Mappings, Test Connection, Copy Mappings, Edit,
  Delete), and pagination controls (25, 50, 100 items per page). **Unified
  Status Field**: The Status column MUST display a single unified status value
  (replacing separate "Active/Inactive" and "Lifecycle Stage" columns) with the
  following values: INACTIVE (manually set), SETUP (no mappings), VALIDATION
  (mappings in progress), ACTIVE (fully operational), ERROR_PENDING
  (unacknowledged errors), OFFLINE (connection failed). Status transitions MUST
  occur automatically based on analyzer state. Users can manually set status to
  INACTIVE at any time via the analyzer edit form or status dropdown in the
  table row. **Test Unit Filter**: The Test Unit filter MUST be a multi-select
  dropdown showing test unit names (loaded from existing test unit
  configuration). The filter MUST operate on test unit IDs (not names) - filters
  analyzers where any selected test unit ID matches any value in the analyzer's
  `test_unit_ids` array using PostgreSQL array overlap operator
  (`WHERE analyzer.test_unit_ids && ARRAY[selected_unit_ids]`). The UI displays
  test unit names for user selection, but filtering logic uses IDs for database
  queries. System MUST allow authorized users to register and manage ASTM
  analyzers via an Add/Edit Analyzer modal (ComposedModal, small size
  ~400-480px) with the following attributes: dialog header with title "Add New
  Analyzer" / "Edit Analyzer" and subtitle "Configure analyzer connection
  settings and test units", form fields including analyzer name (unique,
  required, 1-100 characters, placeholder: "e.g., Hematology Analyzer 1"),
  analyzer type/model (required, dropdown with "Other" option, placeholder:
  "Select analyzer type"), IP address (required, IPv4 format validation,
  half-width, placeholder: "192.168.1.10"), port number (required, 1-65535
  range, half-width, placeholder: "5000"), protocol version (read-only
  TextInput, default value: "ASTM LIS2-A2"), test unit assignment (required,
  multi-select, minimum 1, placeholder: "Select test units...", helper text:
  "Select one or more test units for this analyzer"), status dropdown (required,
  label "Status", options: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING,
  OFFLINE, with description "Analyzer operational status"), a connection test
  button (ghost style) in the connection fieldset that opens a Test Connection
  modal, and audit fields (created date/time, last modified date/time, created
  by user, all auto-generated). The modal MUST have scrollable content with
  fixed action buttons (Cancel secondary, Save primary). System MUST validate
  analyzer name uniqueness and MUST generate a warning (but not block) if
  IP:Port combination is duplicate. System MUST provide a Test Connection modal
  (ComposedModal, small size ~400-480px) with three states: (1) Initial state
  showing analyzer information section (read-only display of analyzer name,
  type, connection IP:Port, protocol version) with "Test Connection" button in
  footer, (2) Progress state with loading indicator, "Testing connection..."
  text, progress bar showing percentage completion (e.g., "50% complete"), and
  collapsible connection logs section displaying log entries with timestamps in
  [HH:MM:SS.mmm] format, log levels visually distinguished with icons (info ℹ,
  debug ◆, warning ⚠, error ✗), and log messages showing connection steps (e.g.,
  "Starting connection test", "Resolving IP address...", "TCP handshake
  initiated"), with footer showing "Testing..." button (disabled) and Cancel
  button, (3) Success state with success icon, "Connection Successful" heading,
  descriptive success message, expanded connection logs section (20+ entries)
  showing full connection sequence including DNS lookup, TCP connection
  establishment, ASTM protocol handshake (ENQ 0x05 / ACK 0x06), protocol
  verification, data transfer testing with checksum calculation, and frame
  acknowledgment, with footer showing "Close" and "Test Again" buttons. The Test
  Connection modal MUST validate TCP handshake only with 30-second timeout and
  display latency and timestamp. System MUST provide a Delete Confirmation modal
  (Alert Dialog) with title "Delete Analyzer" and warning message "Are you sure
  you want to delete {analyzer name}? This action cannot be undone and will
  remove all associated data." with Cancel and Delete (destructive) buttons. The
  delete operation MUST validate no recent results exist, require explicit
  confirmation, and implement a 90-day soft delete window (analyzers with recent
  results cannot be hard deleted). Only analyzers with status ACTIVE MUST
  receive orders and process messages automatically. Analyzers with status
  INACTIVE, SETUP, VALIDATION, ERROR_PENDING, or OFFLINE MUST retain their
  configuration but MUST NOT process incoming messages. Status changes MUST be
  logged in the audit trail with user ID, timestamp, previous status, and new
  status.
- **FR-002**: For a given analyzer, system MUST provide a "Query Analyzer"
  button in the mapping interface that sends ASTM query messages to retrieve
  available data fields. The query operation MUST execute asynchronously using a
  background job pattern: clicking the button triggers a background job, returns
  a job ID immediately (keeping UI responsive), and the UI polls a status
  endpoint (e.g., `/rest/analyzer/query/{analyzerId}/status/{jobId}`) at regular
  intervals (e.g., every 2-3 seconds) to track progress. The status endpoint
  MUST return job status (pending, in-progress, completed, failed), progress
  percentage, connection logs, and retrieved fields (when completed). The UI
  MUST display progress indication (progress bar showing percentage completion,
  collapsible connection logs section with timestamps and log levels) while the
  query executes. Users MUST be able to cancel in-progress queries. The system
  MUST parse response records, extract all field identifiers from ASTM message
  segments, and display available fields in the source panel with field type
  indicators using color-coded tags (Numeric, Qualitative, Control Test, Melting
  Point, Date/Time, Text, Custom). **Query Timeout**: Query timeout is 5 minutes
  (configurable via SystemConfiguration key `analyzer.query.timeout.minutes`,
  default: 5). If the SystemConfiguration key is missing or contains an invalid
  value (non-numeric, negative, or zero), the system MUST use 5 minutes as the
  default timeout. The timeout value MUST be stored in the `SystemConfiguration`
  table and can be adjusted per deployment. Maximum 500 fields per query, rate
  limit 1 query per minute per analyzer. The query MUST handle timeout/error
  states gracefully (display error message, allow retry, show connection logs
  for debugging).
- **FR-003**: System MUST allow users to map each analyzer test code to one or
  more OpenELIS test/analyte definitions using either drag-and-drop or
  click-to-map interaction patterns. The system MUST provide visual connection
  lines between mapped fields, validate type compatibility (blocking
  text-to-numeric mappings, warning on numeric-to-text with confirmation), and
  allow specification of specimen-type or panel constraints when needed. The
  mapping interface MUST use an OpenELIS Field Selector component (searchable
  dropdown with category filtering) that categorizes fields by 8 entity types:
  Tests, Panels, Results, Order, Sample, QC, Metadata, Units. The selector MUST
  include: main dropdown trigger, search input (in popover) that searches across
  name, entity, LOINC code, and description, category filter button, category
  filter popover with checkboxes for all 8 categories and Select All/None
  buttons, field list grouped by category with field items displaying Name,
  LOINC code, Entity, Field Type, Accepted Units, and color-coded category
  badges, type filtering (only shows compatible field types), and active filter
  pills display when categories are filtered. The selector MUST persist category
  filtering during search and enforce type compatibility (numeric→numeric,
  qualitative→qualitative).
- **FR-004**: System MUST allow users to map analyzer quantitative result fields
  and units to OpenELIS result fields via a dedicated unit mapping modal
  interface. The system MUST display the analyzer unit, allow selection of
  OpenELIS canonical unit from a dropdown, flag unit mismatches with warnings,
  and optionally support configuration of conversion behavior when units differ
  (e.g., mg/dL to mmol/L conversion factors).
- **FR-005**: System MUST support mapping of analyzer qualitative result values
  (e.g., "POS", "NEG", "+", "Trace") to OpenELIS coded results via a dedicated
  qualitative mapping modal interface. The system MUST allow multiple analyzer
  values to map to the same OpenELIS code (many-to-one mapping) and MUST allow
  users to specify a default OpenELIS code for unmapped qualitative values.
- **FR-006**: System MUST provide a Copy Mappings modal (ComposedModal, small
  size ~400-480px) that allows users to copy all field mappings from a source
  analyzer to a target analyzer. The modal MUST include: dialog header with
  title "Copy Field Mappings" and subtitle "Copy field mappings from {source
  analyzer} to {target analyzer}", source analyzer section (read-only display
  showing analyzer name and type), target analyzer section with label "Target
  Analyzer \*" (required) and dropdown selector with placeholder "Select target
  analyzer" (searchable, filters to analyzers with active mappings only),
  mapping summary section displaying "X mappings will be copied" with breakdown
  by type (field mappings, unit conversions, qualitative mappings), warning note
  section displaying "This will copy all field mappings including unit
  conversions and qualitative value mappings. Existing mappings will be
  overwritten.", and dialog footer with Cancel button and "Copy Mappings" button
  (with Copy icon from @carbon/icons-react). **Copy Operation Workflow**: (1)
  User selects source analyzer from dropdown (auto-populated based on context or
  user selection), (2) System validates source analyzer has active mappings
  (displays error if none exist: "Source analyzer has no active mappings to
  copy"), (3) User selects target analyzer from filtered dropdown (excludes
  source analyzer and analyzers with incompatible types), (4) System displays
  mapping count summary showing total mappings to be copied, (5) User clicks
  "Copy Mappings" button, (6) System displays confirmation dialog "Are you sure
  you want to copy X mappings? Existing mappings will be overwritten." with
  Confirm/Cancel buttons, (7) User confirms action, (8) System performs copy
  operation with conflict resolution (see below), (9) System displays success
  notification "Successfully copied X mappings" or error notification with
  details. **Conflict Resolution Rules**: (1) **Existing mapping overwrite**: If
  target analyzer has existing mapping with same analyzerFieldId (e.g., both
  analyzers have "GLU" test code), the source mapping overwrites the target
  mapping completely (including unit conversions and qualitative mappings), (2)
  **Type incompatibility handling**: If source field type is incompatible with
  target field type (e.g., NUMERIC source → QUALITATIVE target), system
  generates warning "Mapping skipped for field '{fieldName}' due to type
  incompatibility" and user can choose: Skip (exclude from copy), Force (copy
  anyway with warning badge on resulting mapping, mark as
  `type_incompatible=true` in database, exclude from automatic processing until
  manually reviewed), Cancel (abort entire operation), (3) **Qualitative value
  merging**: For qualitative mappings, if target already has qualitative value
  mappings for the same OpenELIS field, system merges values (does not
  replace) - source values added to existing target values, duplicate values
  deduplicated, (4) **Partial failure rollback**: If any error occurs during
  copy (database constraint violation, validation failure), entire operation
  rolls back - no partial state left in database. **Success/Error States**:
  Success state displays notification "Successfully copied X field mappings, Y
  unit conversions, Z qualitative mappings" with option to "View Target
  Analyzer" (navigates to target analyzer field mapping page); Error state
  displays notification with specific error details and "View Error Log" button
  (opens modal with detailed error information and affected mappings).
- **FR-007**: System MUST provide an inline "test mapping" capability accessible
  from the mapping interface that lets users submit sample ASTM messages or
  example field/value combinations and see a preview of how they would be
  interpreted into OpenELIS entities before going live. **UI Location**: The
  "Test Mapping" button MUST be located in the FieldMapping page header,
  positioned between the "Back" button (left) and the "Save Mappings" button
  (right). The button MUST use ghost style (secondary action) to distinguish it
  from the primary "Save Mappings" action. Clicking the button opens the
  TestMappingModal (ComposedModal, medium size ~600-700px) with the following
  structure: (1) **Dialog header** with title "Test Field Mappings" and subtitle
  "Preview how sample ASTM messages will be interpreted with current mappings",
  (2) **Analyzer information section** displaying read-only analyzer name,
  analyzer type, and active mappings count (e.g., "15 active mappings"), (3)
  **Sample message input section** with TextArea labeled "Sample ASTM Message
  \*" (required), placeholder showing example ASTM message format (e.g.,
  "H|\^&||| PSM^Micro^2.0|..."), character counter showing "0 / 10,240
  characters" with visual warning at 90% capacity, and validation indicators
  (format validation, checksum verification, message structure), (4) **Preview
  options section** with checkboxes "Show detailed parsing steps" (displays
  intermediate parsing state) and "Validate all mappings" (runs full validation
  including type compatibility, required fields, unit conversions), (5) **Result
  display section** (appears after preview button clicked) containing Parsed
  Fields table with columns (Field Name, ASTM Ref, Raw Value, Mapped To,
  Interpretation Status), Applied Mappings section showing which mappings were
  used with mapping IDs and confidence indicators, OpenELIS Entity Preview
  displaying structured JSON or formatted display of resulting
  Test/Result/Sample entities with syntax highlighting, and Warnings/Errors
  section listing mapping warnings (type mismatches, missing conversions,
  ambiguous values), validation errors (required fields missing, invalid
  formats), and unmapped fields with suggested actions, (6) **Action buttons**
  in footer: "Close" button (secondary), "Test Another Message" button (ghost,
  clears form and results), and "Save as Test Case" button (optional, stores
  message for regression testing). The modal MUST validate ASTM message format
  (header/patient/order/result/terminator record structure), enforce 10KB
  message size limit, verify checksums if present, and display validation errors
  inline with specific line/field references. The preview operation MUST execute
  synchronously with <2s response time target and MUST NOT persist any data
  (stateless preview only). **Figma Reference**: Test mapping modal design
  available at
  https://www.figma.com/design/LKzqNAGc3MMQlJTF4JaPBC/004?node-id=1-3200
  (placeholder - update with actual Figma node when wireframe is created).
- **FR-008**: Mapping UI MUST provide a dual-panel interface with equal-width
  panels (50/50) using CSS Grid, stacking vertically on mobile devices
  (<1024px). The page header MUST include a back button (navigates to Analyzers
  page), analyzer name as title, and "Save Mappings" primary action button. A
  validation warning card (yellow background) MUST be displayed if required
  mappings are missing. A statistics section with 3-column grid MUST display
  cards for Total Mappings, Required Mappings, and Unmapped Fields. The left
  panel (Source) MUST display analyzer fields in a table format with columns:
  Field Name, ASTM Ref, Type, Unit, Action. The panel MUST include a panel
  header with title "Analyzer Fields ({type})" and search input (with icon), a
  scrollable fields table with max-height, field type indicators using
  color-coded tags, ASTM segment references (displayed in light gray), visual
  mapping indicators for mapped vs unmapped fields, and a status footer showing
  "{count} fields available". The right panel (Target) MUST display either: (1)
  Mappings Summary state (when no field selected) showing a card with list of
  existing mappings (clickable mapping items that select the field), or (2)
  MappingPanel state (when field selected) with View Mode (existing mapping)
  showing header with mapping title and Edit/Remove buttons, source and target
  field cards (side-by-side), unit conversion display (if numeric), qualitative
  value mappings list (if qualitative), and Required/Optional indicator, or Edit
  Mode (create/edit) showing source field info card (read-only, blue
  background), OpenELIS Field Selector (searchable, categorized dropdown), unit
  mapping section (if numeric) with source unit input, target unit dropdown
  (from OpenELIS field accepted units), and conversion factor input (defaults to
  1.0), qualitative value mapping section (if qualitative) with list of analyzer
  values and target value dropdowns, and action buttons (Save/Cancel). The
  interface MUST support filtering, sorting, and searching across analyzer
  fields and mappings (e.g., by test code, OpenELIS test name, unit, or status).
  Clicking an analyzer field MUST highlight it in the left panel and open the
  mapping panel on the right.
- **FR-009**: System MUST record an audit trail for all mapping-related changes,
  including who made the change, when it was made, which analyzer/mapping was
  affected, and previous vs new values, in support of ISO 15189 and SLIPTA
  requirements.
- **FR-010**: System MUST ensure that mapping changes are applied in a
  controlled manner (e.g., with explicit save/confirm flows and clear indication
  of "draft" vs "active" mappings) so that in-flight messages are processed
  using a consistent configuration. For active analyzers, mapping changes MUST
  require explicit confirmation before activation, and changes MUST apply to new
  results only (existing results remain unchanged). The system MUST validate
  that required mappings (Sample ID, Test Code, Result Value) are present before
  allowing analyzer activation, and MUST prevent activation if any required
  mappings are missing. **Activation Confirmation Modal**: When a user attempts
  to activate mapping changes for an active analyzer, the system MUST display an
  Activation Confirmation Modal (ComposedModal, warning variant) with the
  following structure: (1) Dialog header with title "Activate Mapping Changes"
  and subtitle "Confirm activation of mapping changes for analyzer '{analyzer
  name}'", (2) Warning message section displaying "You are about to activate
  mapping changes for analyzer '{analyzer name}'. These changes will apply to
  all new messages received after activation. Existing results will not be
  affected." with warning icon, (3) Additional warning for active analyzers:
  "This analyzer is currently active. Activating changes may affect incoming
  results." displayed prominently, (4) Confirmation checkbox with label "I
  understand these changes will apply to new messages only" (required before
  activation), (5) Dialog footer with Cancel button (secondary) and "Activate
  Changes" button (primary, destructive style). The modal MUST prevent
  activation until the confirmation checkbox is checked. **Draft/Active State
  Indicators**: The mapping interface MUST display visual indicators for mapping
  state: "Draft" badge (Tag component, gray color) on mappings with
  `is_active=false`, "Active" badge (Tag component, green color) on mappings
  with `is_active=true`. **Save Actions**: The MappingPanel Edit Mode MUST
  provide two save action buttons: "Save as Draft" button (always available,
  secondary style) saves mapping with `is_active=false`, and "Save and Activate"
  button (primary style) saves mapping with `is_active=true` but requires
  confirmation modal for active analyzers (as specified above). **Edge Cases and
  Validation**: (1) **Analyzer with pending messages in queue**: If analyzer has
  unprocessed messages in the error queue (status = UNACKNOWLEDGED or
  PENDING_RETRY), activation modal MUST display additional warning "This
  analyzer has {count} pending messages in the error queue. Activating mapping
  changes may affect how these messages are reprocessed. Consider resolving
  errors first." with option to "View Pending Messages" (opens Error Dashboard
  filtered to this analyzer), (2) **Required mappings missing**: If Sample ID,
  Test Code, or Result Value mappings are missing, activation MUST be blocked -
  modal displays error notification "Cannot activate: Required mappings missing"
  with list of missing required fields and "Close" button only (Activate button
  disabled), (3) **Concurrent edits (optimistic locking)**: If another user has
  modified mappings since current user loaded the page, activation MUST fail
  with error modal "Mapping changes detected. Another user has modified mappings
  for this analyzer. Please reload the page to see latest changes." with "Reload
  Page" button (refreshes field mapping page) and "Cancel" button, (4) **State
  transition diagram**: Draft state → Pending Activation (when user clicks Save
  and Activate) → Active (after confirmation), with rollback path: Pending
  Activation → Draft (if user cancels confirmation or validation fails).
  **Activation Validation Checks**: Before displaying activation modal, system
  MUST perform validation: Check required mappings present (Sample ID, Test
  Code, Result Value), Check for pending messages in error queue, Check for
  concurrent edits (compare mapping lastUpdated timestamps), Validate all active
  mappings have compatible types, Verify analyzer connection is operational
  (optional warning if last connection test failed).
- **FR-011**: On receipt of ASTM messages that reference unmapped test codes,
  units, qualitative values, or QC fields, system MUST hold these messages in an
  error queue and surface them in the Error Dashboard (per FR-016). **Analyzer
  Identification**: Before processing messages, the system MUST identify the
  analyzer that sent the message using multi-strategy identification (ASTM
  header parsing, client IP address lookup, or plugin fallback). Analyzer
  identification is required to apply the correct field mappings and associate
  errors with the correct analyzer configuration. The Error Dashboard MUST
  display unmapped items in a modal or detail view that provides sufficient
  context (raw analyzer code, analyzer name, message timestamp, affected segment
  type) to enable administrators to create the necessary mappings directly from
  the error context. **QC Message Reprocessing**: When QC mapping errors are
  resolved (missing Control Level, Instrument ID, Lot Number, Value, or
  Timestamp mappings are created), system MUST automatically reprocess all
  queued QC messages for the affected analyzer. Reprocessing MUST create
  QCResult entities if all required QC field mappings are now complete.
  Reprocessing MUST occur asynchronously to avoid blocking Error Dashboard
  operations. System MUST notify administrators when QC messages are
  successfully reprocessed (notification: "X QC messages reprocessed
  successfully for analyzer {name}").
- **FR-012**: System MUST allow users to identify which analyzer fields and
  values remain unmapped via visual indicators including: status badges showing
  "Unmapped" state, filter options to show only unmapped fields, counts in the
  status bar (e.g., "X mapped • Y available"), and visual distinction (e.g.,
  grayed out or warning icons) for unmapped items in the mapping interface. This
  enables users to prioritize configuration work before or during go-live.
- **FR-013**: System MUST allow users to disable or retire mappings (e.g., when
  a test is discontinued) while retaining historical mappings for audit and
  historical message interpretation.
- **FR-014**: System MUST provide clear, user-facing feedback (success,
  warnings, errors) in the mapping UI, following OpenELIS internationalization
  and accessibility practices so that administrators understand the impact of
  their changes.
- **FR-015**: System MUST integrate mapping configuration into the overall
  analyzer interface lifecycle using a unified status field that combines
  lifecycle stage and operational status. **Unified Status Field**: The system
  MUST use a single `status` field (replacing separate `active` boolean and
  `lifecycle_stage` enum) with the following values: (1) **INACTIVE** - Manually
  set by user (overrides all other criteria), analyzer does not process
  messages, (2) **SETUP** - Analyzer added but no mappings configured yet,
  analyzer does not process messages, (3) **VALIDATION** - Mappings being
  created/tested, not all required mappings activated, analyzer does not process
  messages, (4) **ACTIVE**
  - All required mappings configured and activated, analyzer automatically
    processes incoming messages into results, (5) **ERROR_PENDING** - Active
    analyzer with unacknowledged errors in error queue, analyzer continues
    processing but errors require attention, (6) **OFFLINE** - Connection test
    failed, analyzer unreachable, analyzer does not process messages.
    **Automatic Status Transitions**: Status MUST transition automatically based
    on analyzer state: SETUP → VALIDATION (when first mapping created),
    VALIDATION → ACTIVE (when all required mappings activated), ACTIVE →
    ERROR_PENDING (when unacknowledged errors detected), ACTIVE → OFFLINE (when
    connection test fails), ERROR_PENDING → ACTIVE (when all errors
    acknowledged), OFFLINE → ACTIVE (when connection restored). Users can
    manually set status to INACTIVE at any time, which overrides automatic
    transitions. **Message Processing**: Only analyzers with status ACTIVE MUST
    automatically process incoming messages into results. When an analyzer
    transitions to ACTIVE, all queued messages (from previous stages) MUST be
    automatically processed using the active mappings.
- **FR-016**: System MUST provide an Error Dashboard page that displays all
  unmapped or failed analyzer messages. The page MUST include: a page header
  with title "Error Dashboard" and "Acknowledge All" primary action button (with
  icon), a statistics section with 4-column grid displaying cards for Total
  Errors (large number display), Unacknowledged (large number display), Critical
  (large number display), and Last 24 Hours (large number display). The
  statistics cards MUST span the same width as the table/content below, using a
  single-row layout with equal-width cards (4 columns), aligned with table edges
  for visual consistency. Each card MUST use color-coding and thematic icons:
  Total Errors (red Carbon token + Error icon), Unacknowledged (orange Carbon
  token + WarningAltFilled icon), Critical (red Carbon token + ErrorFilled
  icon), Last 24 Hours (blue Carbon token + Time icon). A filter bar with search
  input (placeholder: "Search errors..."), Error Type filter dropdown (default:
  "All Types"), Severity filter dropdown (default: "All Severities"), and
  Analyzer filter dropdown (default: "All"), a searchable, filterable data table
  with columns: Timestamp (formatted as "MM/DD/YYYY, HH:MM:SS AM/PM"), Analyzer,
  Type (badge showing error type: connection, mapping, validation, timeout,
  protocol), Severity (badge showing severity: critical, error, warning),
  Message (truncated), Status (badge with icon: Unacknowledged with warning
  icon, Acknowledged with checkmark icon), and Actions (OverflowMenu with "View
  Details" and "Acknowledge" options). The dashboard MUST support filtering by
  analyzer ID, error type, severity, start date, end date, and pagination. Users
  with LAB_SUPERVISOR or equivalent roles MUST be able to acknowledge individual
  errors via an Error Details modal or acknowledge multiple errors in batch. The
  Error Details modal (ComposedModal) MUST include: dialog header with title
  "Error Details" and subtitle "Detailed information and analyzer logs for error
  {errorId}", error information section with error metadata grid (2-column
  layout) displaying Error ID, Timestamp (formatted date/time), Analyzer (name),
  Analyzer ID, Error Type (badge), Severity (badge), and Error Message (full
  message text), acknowledgment status section (conditional) showing
  acknowledged icon, status "Acknowledged", and details "By {user} on
  {date/time}" when acknowledged, analyzer logs section (collapsible) with
  header "Analyzer Logs ({count} entries)" and expand/collapse icon, action
  buttons "Copy" and "Download", scrollable log entries list with formatted
  entries showing timestamp [HH:MM:SS.mmm], log level icon (ℹ info, ◆ debug, ⚠
  warning, ✗ error), log level label, and detailed log message (e.g., "Analyzer
  {name} online and operational", "Last successful data transmission: Sample ID
  {id}", "TCP connection established: {IP:Port}", "Network latency detected:
  {ms}ms (threshold: {ms}ms)", "Connection retry attempt {n}/3", "Connection
  timeout after {n} seconds", "TCP socket closed unexpectedly"), recommended
  actions section with heading "Recommended Actions" and bulleted list with icon
  indicators providing troubleshooting guidance (e.g., "Verify analyzer is
  powered on and network cable is connected", "Check IP address and port
  configuration in analyzer settings", "Test connection using the 'Test
  Connection' feature"), and dialog footer with Close button. The Error Details
  modal MUST display full error context and allow users to open a mapping
  interface modal for creating mappings directly from the error context. The
  dashboard MUST show unacknowledged error counts and provide bulk action
  capabilities.
- **FR-017**: System MUST support reprocessing of previously failed messages
  after mappings are created, allowing users to trigger reprocessing for
  individual messages or batches of messages from the Error Dashboard.
- **FR-018**: System MUST allow system administrators to add custom field types
  with validation rules, extending beyond the standard field types (Numeric,
  Qualitative, Control Test, Melting Point, Date/Time, Text). Custom field types
  MUST include validation rules (e.g., format patterns, value ranges, allowed
  characters) and MUST be available for use in field mapping configuration.
- **FR-019**: System MUST allow users to add new OpenELIS target fields directly
  from the mapping interface without navigating away to a separate configuration
  page. When a required OpenELIS field does not exist, users MUST be able to
  create it inline (e.g., via a "Create New Field" action in the OpenELIS Field
  Selector modal), with appropriate validation and confirmation before the new
  field becomes available for mapping. The inline field creation modal
  (InlineFieldCreationModal, ComposedModal, medium size ~500-600px) MUST
  include: (1) **Dialog header** with title "Create New OpenELIS Field" and
  subtitle "Add a new field for mapping to analyzer data", (2) **Entity type
  selection** (Dropdown, required) with 8 options: TEST, PANEL, RESULT, ORDER,
  SAMPLE, QC, METADATA, UNIT, (3) **Entity-specific form fields** that adapt
  based on selected entity type (see validation matrix below), (4) **Field type
  compatibility display** showing which analyzer field types can map to this
  field, (5) **Action buttons**: Cancel (secondary) and "Create Field" (primary,
  disabled until validation passes). The system MUST validate fields according
  to entity type and MUST check uniqueness constraints before creation.
  **Entity-Specific Validation Rules**: (1) **TEST** - Required: Test Name
  (1-200 chars, unique), Test Code (1-50 chars, unique, alphanumeric +
  hyphens/underscores only), Sample Type (dropdown from existing sample types);
  Optional: LOINC Code (valid LOINC format), Description (max 500 chars), Result
  Type (dropdown: Numeric, Qualitative, Text); Validation: Test code must be
  unique across all tests, Sample type must exist in system, LOINC code must
  match format \d{4,5}-\d if provided. (2) **PANEL** - Required: Panel Name
  (1-200 chars, unique), Panel Code (1-50 chars, unique); Optional: LOINC Code,
  Description, Member Tests (multi-select from existing tests); Validation:
  Panel code unique, Member tests must exist and be active. (3) **RESULT** -
  Required: Result Name (1-100 chars), Analyte Reference (dropdown from existing
  analytes, required); Optional: Result Group (dropdown), Reporting Sequence
  (integer 1-999); Validation: Analyte reference must be valid and active,
  Reporting sequence must be unique within test/panel if specified. (4)
  **ORDER** - Required: Order Type (dropdown: ROUTINE, URGENT, STAT, PRIORITY),
  Priority (integer 1-10); Optional: Requesting Provider (reference);
  Validation: Order type from enum, Priority between 1-10. (5) **SAMPLE** -
  Required: Sample Type Code (1-50 chars, unique, uppercase), Sample Type Name
  (1-100 chars); Optional: Container Type (dropdown), Collection Method;
  Validation: Code unique, uppercase letters/numbers/hyphens only. (6) **QC** -
  Required: Control Name (1-100 chars), Lot Number (1-50 chars, unique for this
  control), Control Level (dropdown: Low, Normal, High, required), Instrument ID
  (extracted from ASTM message header, required), Result Value (numeric or
  qualitative, required), Timestamp (extracted from ASTM message, required);
  Optional: Expiration Date (future date), Target Range (min/max values, if
  quantitative); Validation: Lot number unique for control name, Control level
  must be one of Low/Normal/High, Instrument ID must reference valid analyzer,
  Result value must match control type (numeric for quantitative, qualitative
  for qualitative controls), Timestamp must be valid ISO 8601 format, Expiration
  date must be future if provided. (7) **METADATA** - Required: Field Name
  (1-100 chars, unique), Data Type (dropdown: STRING, NUMBER, DATE, BOOLEAN);
  Optional: Format Pattern (regex for STRING, date format for DATE); Validation:
  Field name unique across metadata, Format pattern valid regex/date format if
  provided. (8) **UNIT** - Required: Unit Code (1-20 chars, unique, uppercase),
  Unit Name (1-50 chars); Optional: SI Equivalent (reference to SI unit),
  Conversion Factor (decimal, default 1.0); Validation: Unit code unique,
  Conversion factor > 0 if provided. **Field Type Compatibility Matrix**:
  Analyzer field types that can map to each OpenELIS field type - NUMERIC
  analyzer fields → TEST (if result type = Numeric), RESULT (if data type =
  Numeric), QC (quantitative controls), METADATA (if data type = NUMBER), UNIT;
  QUALITATIVE analyzer fields → TEST (if result type = Qualitative), RESULT (if
  data type = Qualitative/Text), QC (qualitative controls), METADATA (if data
  type = STRING); TEXT analyzer fields → All entity types (with warnings for
  type mismatches); CUSTOM analyzer fields → Compatible based on custom field
  type definition. **Error Messaging**: The system MUST display specific
  validation errors for each entity type: "Test code '{code}' already exists"
  (TEST uniqueness), "Panel '{code}' must have at least one member test" (PANEL
  validation), "Analyte '{id}' is inactive and cannot be used" (RESULT
  validation), "Sample type code must be uppercase letters, numbers, or hyphens
  only" (SAMPLE format), "Control lot number '{lot}' already exists for
  '{control}'" (QC uniqueness), "Field name '{name}' already exists in metadata"
  (METADATA uniqueness), "Unit code '{code}' already exists" (UNIT uniqueness).
  After successful field creation, the system MUST refresh the OpenELIS Field
  Selector list, auto-select the newly created field for immediate mapping, and
  display success notification "Field '{name}' created successfully and ready
  for mapping".
- **FR-020**: System MUST integrate the analyzer mapping feature into the
  OpenELIS left-hand navigation bar using Carbon SideNavMenu and SideNavMenuItem
  components. Navigation menu items MUST be backend-driven via the existing
  `/rest/menu` API endpoint. The "Analyzers" parent menu item MUST always be
  present and, when expanded, MUST display the following sub-navigation items
  (subject to role permissions):

  1. **Analyzers Dashboard** – default landing page at `/analyzers`, showing the
     analyzer list/overview defined in FR-001.
  2. **Error Dashboard** – `/analyzers/errors`, per FR-016.
  3. **Quality Control** – placeholder group that links to the Westgard QC
     experience delivered in feature `003-westgard-qc`, with three stub routes
     exposed immediately: `/analyzers/qc` (QC dashboard), `/analyzers/qc/alerts`
     ("QC Alerts & Violations"), and `/analyzers/qc/corrective-actions`
     ("Corrective Actions"). These links MAY initially point to "coming soon" or
     legacy QC content but MUST remain under the "Analyzers" hierarchy so users
     learn the unified instrumentation tree.

  **Field Mappings Access**: The Field Mappings page (`/analyzers/:id/mappings`)
  MUST NOT appear as a menu item in the left-hand navigation. Users access Field
  Mappings through the Analyzers Dashboard by clicking the "Field Mappings"
  action in the analyzer row's overflow menu (per FR-001). The side menu entries
  (Analyzers Dashboard, Error Dashboard, Quality Control + its two sub-pages)
  MUST remain visible whenever the parent node is expanded (respecting
  role-based filtering). **Unified Tab-Navigation Pattern**: Sub-navigation
  items MUST act as the sole "tab" affordance—the system MUST NOT use Carbon
  Tabs/TabList/TabPanels components on analyzer pages (explicit anti-pattern).
  Navigation is handled entirely through SideNavMenuItem components. Clicking a
  sub-nav item navigates to its route and highlights that SideNavMenuItem.
  Active highlighting MUST work for all routes listed above (e.g.,
  `/analyzers/qc/alerts` highlights "QC Alerts & Violations"). Note: Field
  Mappings page does not appear in navigation menu and is accessed via analyzer
  row actions. **State Preservation**: The system MUST maintain filters,
  pagination, selected analyzer, and other UI state through URL query
  parameters/path segments; scroll positions and unsaved form drafts MUST use
  sessionStorage so state persists when navigating among these sub-pages.
  **Role-based Visibility**: Menu API responses MUST exclude items the user
  cannot access (e.g., QC routes hidden if the user lacks quality-control
  permissions). If no child item is visible for a user, the parent “Analyzers”
  entry MUST also be hidden. This structure ensures analyzer registration,
  mapping, monitoring, and QC oversight stay discoverable under one hierarchy in
  line with the Figma navigation model.

#### QC Result Processing

- **FR-021**: System MUST process QC results from ASTM analyzer messages
  automatically. **Q-Segment Parsing**: When ASTM messages contain Q-segments
  (Quality Control result segments), the system MUST parse these segments and
  extract QC data including: instrument ID (from message header), test code,
  control lot number, control level (Low/Normal/High), result value (numeric or
  qualitative), unit of measure, and timestamp. **Field Mapping Application**:
  System MUST apply configured QC field mappings (per FR-019) to map extracted
  ASTM codes to OpenELIS entities (control lot, test, instrument).
  **Persistence**: System MUST persist parsed QC results to the QCResult entity
  (003's data model) via direct service call to QCResultService. Persistence
  MUST occur within the same transaction as patient result processing. **Error
  Handling**: If QC field mappings are incomplete or missing, system MUST queue
  the unmapped QC message in the Error Dashboard (per FR-011) with error type
  "QC_MAPPING_INCOMPLETE" and severity "ERROR". **Service Unavailability**: If
  Feature 003's `QCResultService` is unavailable (service not found, connection
  failure, or Feature 003 not deployed), system MUST queue the QC message in
  Error Dashboard with error type "QC_SERVICE_UNAVAILABLE" and severity "ERROR".
  The error message MUST indicate that QC results cannot be persisted until
  Feature 003 is available. **Reprocessing**: When QC mapping errors are
  resolved in Error Dashboard OR when Feature 003 service becomes available,
  system MUST automatically reprocess queued QC messages and create QCResult
  entities if mappings are now complete and service is available.

### Infrastructure Prerequisites

The following infrastructure components are required for ASTM analyzer
communication but are not part of the feature specification (handled as
deployment/infrastructure setup):

- **ASTM-HTTP Bridge**: The ASTM-HTTP bridge is a middleware component that
  translates between ASTM TCP protocol (used by analyzers) and HTTP POST (used
  by OpenELIS). The bridge is integrated into the standard development
  environment via `dev.docker-compose.yml` and starts automatically with the
  development Docker Compose setup. Bridge configuration is documented in
  `volume/astm-bridge/configuration.yml` and `docs/astm.md`. The bridge enables
  **bi-directional** production-like communication flows:
  - **Analyzer → Bridge → OpenELIS** (Results submission): Analyzer (TCP) →
    Bridge (TCP:5001) → OpenELIS (HTTP POST at `/analyzer/astm`)
  - **OpenELIS → Bridge → Analyzer** (Field queries per FR-002): OpenELIS (HTTP
    POST to Bridge) → Bridge forwards to Analyzer (TCP) using `forwardAddress`
    and `forwardPort` parameters For testing, direct HTTP POST to OpenELIS is
    also supported. See `research.md` Section 12 and `quickstart.md` Step 0 for
    detailed setup instructions.

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` (v1.7.0); only items relevant to
analyzer mapping are listed:_

- **CR-001**: UI components for analyzer mapping MUST follow the Carbon Design
  System exclusively. Specific components MUST include: Header for page titles,
  Search with 300ms debounce, MultiSelect for filter dropdowns, Tag for filter
  pills and field type indicators, DataTable for analyzer and error listings,
  Toggle for status controls, OverflowMenu for row actions, Pagination for
  navigation, ComposedModal for dialogs, Accordion for field categorization,
  TextInput/Dropdown/NumberInput for form fields, and visual connection lines
  using Carbon design tokens. **Navigation components**: SideNavMenu and
  SideNavMenuItem MUST be used for left-hand navigation (sub-navigation items
  function as tabs). Carbon Tabs/TabList/TabPanels components MUST NOT be used
  on analyzer pages - navigation is handled entirely through unified
  sub-navigation per FR-020. Field type color coding MUST use Carbon tokens:
  Numeric ($blue-60), Qualitative ($purple-60), Control Test
  ($green-60), Melting Point ($teal-60), Date/Time ($cyan-60), Text ($gray-60).
  Typography MUST follow Carbon standards: page titles
  ($heading-04, 32px), section headers ($heading-03, 20px), body text
  ($body-01, 14px), helper text ($label-01, 12px). Spacing MUST use Carbon
  tokens ($spacing-05 for standard, $spacing-07 for sections). The UI MUST NOT
  introduce additional custom CSS frameworks (e.g., Bootstrap, Tailwind) and
  MUST be responsive, stacking vertically on mobile devices (<1024px).
- **CR-002**: All user-facing strings in the analyzer mapping UI (labels,
  tooltips, messages) MUST be internationalized via the existing localization
  mechanism (React Intl in the current frontend), with at least English and
  French translations provided.
- **CR-003**: Backend logic for analyzer mapping MUST follow the mandated
  5-layer architecture (Valueholder→DAO→Service→Controller→Form); controllers
  MUST NOT contain business logic or access DAOs directly.
- **CR-004**: Any schema changes required to support
  analyzer/analyzer-field/mapping entities MUST be implemented via Liquibase
  changesets with appropriate rollback definitions, and NOT via direct SQL
  DDL/DML in production.
- **CR-005**: If analyzer results or mappings are exposed beyond OpenELIS (e.g.,
  to national health information exchanges), integration MUST use FHIR R4
  resources and IHE profiles as defined in the project’s interoperability
  architecture.
- **CR-006**: Country- or site-specific analyzer mapping rules (e.g., different
  standard units, preferred code systems) MUST be handled via configuration
  (e.g., database-backed configuration or properties), NOT via country-specific
  code branches.
- **CR-007**: Mapping configuration and the application of mappings to incoming
  messages MUST respect security principles: role-based access control (with
  separate permissions for editing vs activating mappings), audit trail for
  configuration changes, and input validation for analyzer-provided data.
- **CR-008**: This feature MUST be delivered with automated tests across the
  test pyramid (unit tests for mapping logic, integration tests for end-to-end
  message handling, and targeted E2E tests for mapping workflows), meeting or
  exceeding the project’s coverage goals for new code.

### Key Entities _(include if feature involves data)_

- **Analyzer**: Represents a physical or logical instrument (e.g., specific
  chemistry analyzer) configured to communicate with OpenELIS using ASTM;
  includes identifiers (name, model, serial), location, and configuration
  metadata required to associate messages with this analyzer.
- **AnalyzerField**: Represents a specific field or code emitted by the analyzer
  (e.g., test code, measurement ID, qualifier field) that can be mapped to one
  or more OpenELIS concepts.
- **OpenELISField**: Represents the relevant OpenELIS domain concept to which an
  analyzer field can map (e.g., test order, analyte component, result field,
  unit, qualitative code).
- **AnalyzerFieldMapping**: Represents the mapping configuration between an
  `AnalyzerField` and one or more `OpenELISField` entries, including mapping
  type (test-level, result-level, metadata), any transformation rules, and
  activation state.
- **QualitativeResultMapping**: Represents mapping of instrument-specific
  qualitative values (strings or codes) to canonical OpenELIS-coded results,
  including support for multiple analyzer values mapping to the same OpenELIS
  code.
- **UnitMapping**: Represents mapping of analyzer-reported units to OpenELIS
  canonical units, including optional indication of whether numeric conversion
  is supported or if mismatched units must be rejected.

## Assumptions

- The OGC-49 specification fully defines the ASTM segments/fields and expected
  mapping behavior for supported analyzers; this feature will align with those
  definitions even though not all details are restated in this spec.
- Feature 004 includes the Error Dashboard for viewing and resolving
  unmapped/failed analyzer messages, but excludes the Quality Control dashboard
  and ongoing quality control visualization flows, which will be addressed as a
  separate feature.
- Mapping configuration follows a two-tier permission model: users with "Lab
  Manager" or equivalent roles (e.g., "Maintenance Admin") can create and edit
  analyzer mappings, but only users with "System Administrator" or "Interface
  Manager" roles (e.g., "Global Administrator", "User Account Administrator")
  can approve and activate mapping changes in production environments. This
  separation ensures controlled deployment of configuration changes.
- Quality Control (Westgard) capabilities delivered in feature `003-westgard-qc`
  share the same analyzer navigation hierarchy; this spec references those
  screens as placeholders (main QC dashboard plus "QC Alerts & Violations" and
  "Corrective Actions") and expects future QC pages to reside under
  `/specs/003-westgard-qc`.

**Feature Separation with Feature 003 (Westgard QC)**:

- **Feature 004 (ASTM Analyzer Field Mapping) Scope**: ASTM message
  communication and parsing, field mapping configuration, error handling and
  dashboard, QC result parsing and persistence (via service call to Feature
  003's QCResultService). Feature 004 is the single source of truth for analyzer
  data ingestion and handles ALL ASTM message processing, including Q-segments
  (Quality Control result segments). Feature 004 parses Q-segments, extracts QC
  data, applies configured QC field mappings, and persists QC results via direct
  service call to Feature 003's QCResultService.createQCResult() method.

- **Feature 003 (Westgard QC) Scope**: QC analytics and visualization, Westgard
  rule evaluation, control charts (Levey-Jennings), automated alerts, corrective
  actions. Feature 003 depends on Feature 004 for QC result data via
  QCResultService.

- **Separation Boundary**: Feature 004 handles ASTM communication, message
  parsing, and data persistence (including QC results). Feature 003 handles all
  QC analytics, visualization, rule evaluation, and corrective actions. Feature
  004 does NOT include: Westgard rule evaluation, control charts, QC analytics,
  or corrective actions. Feature 003 does NOT include: ASTM message parsing or
  field mapping configuration.

## Clarifications

### Session 2025-11-14

- Q: Can you make sure the analysis @figma-design-analysis.md is incorporated
  into the specs fully? → A: Yes. The Figma design analysis has been fully
  incorporated into the specification. Key UI/UX details added include: (1)
  Statistics cards layout (3-column grid for Analyzers page, 4-column grid for
  Error Dashboard), (2) Test Connection modal with three detailed states
  (initial, progress with connection logs, success with expanded logs), (3)
  Error Dashboard with specific statistics cards, filter dropdowns, table
  columns, and status badges with icons, (4) Error Details modal with error
  metadata grid, analyzer logs section (collapsible with Copy/Download buttons),
  and recommended actions section, (5) Copy Mappings modal structure with
  source/target sections and warning note, (6) Delete Analyzer modal with exact
  warning message format, (7) Add Analyzer modal with specific form field
  placeholders and helper text, (8) Field Mapping page with detailed dual-panel
  interface specifications including panel states, mapping panel view/edit
  modes, and OpenELIS Field Selector component details with 8 entity categories
  and filtering capabilities. All details from the Figma design analysis
  document have been integrated into the relevant functional requirements
  (FR-001, FR-003, FR-006, FR-008, FR-016).

- Q: Navigation integration with OpenELIS left-hand navigation bar → A: The
  analyzer mapping feature MUST be integrated into the OpenELIS left-hand
  navigation bar. A top-level "Analyzers" navigation item MUST be added to the
  main navigation menu. Under this parent item, sub-navigation items MUST be
  provided for each top-level page: "Analyzers List" (default route), "Error
  Dashboard", and optionally "Field Mappings" (when accessed via direct link).
  Each page MUST have its own unique URL route. The navigation structure MUST
  follow OpenELIS navigation patterns and MUST be accessible based on user role
  permissions. Navigation state MUST be preserved when navigating between pages
  (e.g., filters, selected analyzer context).

- Q: Sub-navigation structure and URL routes → A: Option B - Expandable
  sub-navigation. The "Analyzers" parent navigation item MUST be
  expandable/collapsible (following Carbon SideNavMenu pattern). When expanded,
  it MUST show sub-navigation items: "Analyzers List" (default route
  `/analyzers`), "Error Dashboard" (route `/analyzers/errors`). The "Field
  Mappings" page MUST appear contextually in the navigation when the user is on
  that page (route `/analyzers/:id/mappings`), but need not always be visible in
  the sub-menu when not on that page. The parent "Analyzers" menu item MUST
  follow existing OpenELIS navigation patterns using Carbon SideNavMenu and
  SideNavMenuItem components.

- Q: Role-based access control for navigation items → A: Option A - Hide
  navigation items. Navigation items MUST be hidden from users who lack required
  permissions (e.g., "Error Dashboard" hidden from users without LAB_SUPERVISOR
  role, "Analyzers List" hidden from users without LAB_USER role). If a user
  lacks permission for all sub-navigation items under "Analyzers", the parent
  "Analyzers" navigation item MUST also be hidden. This follows the principle of
  not showing inaccessible options to users.

- Q: Navigation state preservation scope → A: Option A - Preserve all state.
  When navigating between pages within the analyzer mapping feature, the system
  MUST preserve all user interface state including: active filters (search
  terms, dropdown selections, filter pills), selected items (e.g., selected
  analyzer when navigating from Analyzers List to Field Mappings), scroll
  position within tables and panels, pagination settings (current page, items
  per page), and form input values in modals or inline forms that are not yet
  submitted. This state preservation MUST use appropriate mechanisms (URL query
  parameters for shareable state, sessionStorage or localStorage for
  non-shareable state) and MUST persist across browser back/forward navigation
  and page refreshes where appropriate.
- Q: Navigation menu integration mechanism → A: Option A - Backend-driven menu
  items via `/rest/menu` API. The "Analyzers" navigation menu items MUST be
  added to the database menu structure and exposed via the existing `/rest/menu`
  API endpoint. The frontend MUST render menu items dynamically based on the API
  response, consistent with existing OpenELIS navigation patterns (as
  implemented in Header.js). Role-based visibility MUST be handled server-side
  by the menu API, ensuring users only see navigation items they have permission
  to access. This approach enables dynamic menu configuration without frontend
  code changes and maintains consistency with the existing menu management
  system.
- Q: State preservation implementation details → A: Option A -
  Filters/search/pagination in URL params; scroll/form drafts in sessionStorage.
  Active filters (search terms, dropdown selections, filter pills), pagination
  settings (current page, items per page), and selected analyzer ID MUST be
  stored in URL query parameters or path parameters (e.g.,
  `/analyzers?status=active&page=2` or `/analyzers/:id/mappings`), enabling
  shareable/bookmarkable URLs. Scroll position within tables and panels, and
  unsaved form input values in modals or inline forms MUST be stored in
  sessionStorage (clears on tab close). This approach matches existing OpenELIS
  patterns (SearchResultForm.js uses URLSearchParams for filters) and balances
  shareability with user session privacy.
- Q: ASTM query analyzer execution model → A: Option B - Asynchronous with
  background job and polling. The "Query Analyzer" button MUST trigger an
  asynchronous background job that returns a job ID immediately, allowing the UI
  to remain responsive. The system MUST provide a status polling endpoint (e.g.,
  `/rest/analyzer/query/{analyzerId}/status/{jobId}`) that returns job status
  (pending, in-progress, completed, failed), progress percentage, and retrieved
  fields (when completed). The UI MUST poll the status endpoint at regular
  intervals (e.g., every 2-3 seconds) and display progress indication (progress
  bar, connection logs) while the query executes. Users MUST be able to cancel
  in-progress queries. This approach prevents UI blocking during long-running
  network operations (up to 5-minute timeout), handles analyzer offline/timeout
  scenarios gracefully, and enables real-time progress tracking consistent with
  existing OpenELIS patterns for long-running operations.
- Q: Tab-based navigation integration pattern → A: Tabs and sub-navigation are
  unified - sub-nav items act as tabs. The left-hand sub-navigation items
  ("Analyzers List", "Error Dashboard", "Field Mappings") MUST function as tabs
  without requiring a separate tab bar component on the page. Clicking a sub-nav
  item navigates to the corresponding route and highlights that item in the left
  navigation. The active sub-nav item MUST be visually highlighted to indicate
  the current page/tab. Pages MUST NOT include separate Carbon Tabs/TabList
  components - navigation is handled entirely through the left-hand
  sub-navigation. This unified approach eliminates duplicate navigation options
  and provides a single, consistent navigation pattern.

### Session 2025-11-18

- Q: How should Quality Control (Westgard) pages be organized within the
  analyzer navigation hierarchy? → A: Option A - Keep “Quality Control” as a
  sub-section under the existing “Analyzers” parent. The left-hand navigation
  MUST always show the “Analyzers” parent node expanded for analyzer tooling,
  with a dedicated “Quality Control” sub-section linking to QC Dashboard,
  Control Charts, Corrective Actions, etc., so users experience one unified
  analyzer hierarchy.
- Q: Which specific sub-navigation pages should be present under the “Analyzers”
  parent for feature 004 while accommodating future QC work? → A: Option B
  (refined) - Implement the ASTM-focused pages now (Analyzers Dashboard, Field
  Mappings, Error Dashboard) and add a “Quality Control” placeholder section
  that exposes two future sub-pages (“QC Alerts & Violations”, “Corrective
  Actions”) plus the main QC dashboard link. These QC routes point to the 003
  Westgard feature but remain visible so users see a consistent hierarchy even
  before QC is fully delivered.

### Session 2025-01-19

- Q: Modal sizes for CRUD operations and Test Connection → A: Option A - Use
  Carbon "sm" (small) size (~400-480px width) for Add/Edit Analyzer, Test
  Connection, Copy Mappings, and Delete modals. All CRUD operations and test
  connection actions MUST open in small modals to maintain a compact, consistent
  UI experience.
- Q: Dashboard tile width and layout → A: Option A - Dashboard tiles span the
  same width as the table/content below, using a single-row layout with
  equal-width cards (3 columns), aligned with table edges. The statistics cards
  MUST be better-laid out and aligned with the table width for visual
  consistency.
- Q: Color-coding and thematic icons for dashboard tiles → A: Option A -
  Color-coded tiles with thematic icons: Total Analyzers (blue Carbon token +
  Analytics icon), Active (green Carbon token + CheckmarkFilled icon), Inactive
  (gray Carbon token + WarningAlt icon). Dashboard tiles MUST use color-coding
  and thematic icons to improve visual distinction and enable quick scanning.

### Session 2025-01-27

- Q: Field Mappings menu visibility pattern → A: Field Mappings should not show
  up in the side menu ever. The Field Mappings page (`/analyzers/:id/mappings`)
  MUST NOT appear as a menu item in the left-hand navigation. Users access Field
  Mappings through the Analyzers Dashboard by clicking the "Field Mappings"
  action in the analyzer row's overflow menu. The side menu MUST only display:
  Analyzers Dashboard, Error Dashboard, and Quality Control placeholder items.
- Q: "7 days" definition for lifecycle transition → A: Option A - 7 calendar
  days from the date when `lifecycleStage` transitions to `GO_LIVE` (activation
  date). The transition from GO_LIVE to MAINTENANCE stage MUST occur
  automatically after 7 calendar days have elapsed since the analyzer's
  lifecycle stage was set to GO_LIVE, using the server's timezone for date
  calculations.
- Q: Query timeout default behavior → A: Option A - If SystemConfiguration key
  `analyzer.query.timeout.minutes` is missing or invalid, use 5 minutes as
  default timeout. The system MUST use 5 minutes as the default timeout value
  when the configuration key is absent from the database or contains an invalid
  value (non-numeric, negative, or zero).
- Q: Terminology consistency - "Analyzers Dashboard" vs "AnalyzersList" → A:
  Option A - Standardize on "Analyzers Dashboard" across all artifacts. Use
  "Analyzers Dashboard" in all user-facing references (spec, UI labels,
  navigation). The React component can be named `AnalyzersList.jsx` internally
  for code organization, but all documentation, user-facing labels, and
  navigation menu items MUST refer to it as "Analyzers Dashboard".
- Q: Copy mappings conflict resolution "Force" option behavior → A: Option A -
  When "Force" is selected for type-incompatible mappings, create the mapping
  with a warning badge, mark it as `type_incompatible=true` in the database, and
  exclude it from automatic message processing until manually reviewed and
  approved. The system MUST prevent automatic processing of type-incompatible
  mappings and require explicit user review before activation.

### Session 2025-11-20

- Q: 004 vs 003 division of concerns → A: 004 handles all ASTM backend message
  processing and mapping configuration (including QC results); 003 focuses on QC
  analytics and visualization. 004 parses ASTM messages (Q-segments included),
  applies mappings, and persists the results using 003's data model/service. 004
  is the single source of truth for analyzer data ingestion. **See FR-021 for
  detailed QC result processing requirements.**
- Q: QC Field Mapping scope in 004 → A: 004 MUST support mapping all QC fields
  required by 003 (Control Level, Instrument ID, Lot Number, Value, Timestamp).
  The 004 spec's QC field type definition (FR-019) is updated to include these
  missing fields.
- Q: Error handling for QC results → A: 004's Error Dashboard handles ALL
  unmapped messages, including QC results. There is no separate error queue for
  QC in 003. Resolving a mapping error in 004's dashboard reprocesses the
  message for both patient and QC results. **See FR-011 for QC message
  reprocessing requirements.**
- Q: Analyzer Lifecycle States → A: Analyzer lifecycle stages are defined in
  `AnalyzerConfiguration.LifecycleStage` enum: (1) **SETUP**: Analyzer added but
  no mappings configured. (2) **VALIDATION**: First mappings created, testing
  with sample messages, validating accuracy. (3) **GO_LIVE**: Mappings
  activated, analyzer receiving orders, monitoring enabled. (4) **MAINTENANCE**:
  Operational analyzer with ongoing mapping updates and error resolution. **See
  FR-015 and plan.md Section "Lifecycle Stage Management" for detailed lifecycle
  stage definitions and transition rules.**

### Session 2025-01-28

- Q: What does "active only if all pass" mean? What are the specific criteria
  that must pass for an analyzer to be considered "active"? → A: An analyzer is
  considered "Active" only if BOTH of the following criteria pass: (1) Lifecycle
  stage is GO_LIVE or MAINTENANCE, (2) All required field mappings are
  configured and activated. Users can always manually set an analyzer to
  "Inactive" regardless of these criteria, providing a manual override
  capability. **See updated FR-001 and FR-015 for merged status/lifecycle stage
  requirements.**

- Q: How should the unified status field be structured? What status values are
  needed? → A: The system MUST use a single unified `status` field (replacing
  separate `active` boolean and `lifecycle_stage` enum) with the following
  values: (1) **INACTIVE** - Manually set by user (overrides all other
  criteria), (2) **SETUP** - Analyzer added but no mappings configured yet, (3)
  **VALIDATION** - Mappings being created/tested, not all required mappings
  activated, (4) **ACTIVE** - All required mappings configured and activated,
  analyzer can automatically process results, (5) **ERROR_PENDING** - Active
  analyzer with unacknowledged errors in error queue, (6) **OFFLINE** -
  Connection test failed, analyzer unreachable. Status transitions MUST occur
  automatically: SETUP → VALIDATION (when first mapping created), VALIDATION →
  ACTIVE (when all required mappings activated), ACTIVE → ERROR_PENDING (when
  unacknowledged errors detected), ACTIVE → OFFLINE (when connection test
  fails), ERROR_PENDING → ACTIVE (when all errors acknowledged), OFFLINE →
  ACTIVE (when connection restored). Users can manually set status to INACTIVE
  at any time. **See updated FR-001, FR-015, and data-model.md for unified
  status field implementation.**

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory administrators can complete initial analyzer
  configuration (analyzer registration + field mapping for 100 test codes) in
  under 2 hours of active configuration work (excluding training time). This
  includes: registering the analyzer, querying analyzer fields, mapping all test
  codes to OpenELIS tests/analytes, configuring unit mappings, and setting up
  qualitative value mappings. _[Task Reference: T159]_

- **SC-002**: System processes 98%+ of ASTM messages successfully when mappings
  are configured. Messages with unmapped fields are queued for resolution rather
  than failing silently. Error rate calculation: (successful messages / total
  messages) >= 0.98. Includes edge cases: unmapped fields, unit mismatches,
  validation errors. _[Task Reference: T208]_

- **SC-003**: 100% of mapping changes (create, update, retire) are recorded in
  audit trail with user ID, timestamp, previous value, and new value. Audit
  trail queries complete in <1 second for 1000+ mapping changes. Test:
  Create/update/disable 100 mappings, verify 100% have audit trail entries.
  _[Task Reference: T207]_

- **SC-004**: Query analyzer operation completes within configured timeout
  (default 5 minutes, configurable via SystemConfiguration key
  `analyzer.query.timeout.minutes`) and retrieves up to 500 fields. Connection
  test validates TCP handshake within 30 seconds. Query timeout handling
  displays error message, allows retry, and shows connection logs for debugging.
  _[Task Reference: T162, T209]_

- **SC-005**: Error Dashboard displays 1000+ error records with pagination,
  filtering, and sorting in <1 second load time. Dashboard supports filtering by
  error type, analyzer, date range, and resolution status. _[Task Reference:
  T096, T097, T098]_

- **SC-006**: Unmapped messages are resolved (mapping created + message
  reprocessed) within 5 minutes of error identification for 90%+ of cases.
  Resolution workflow: administrator views error in dashboard, creates mapping
  from error context, triggers reprocessing, and verifies successful processing.
  _[Task Reference: T092, T101, T088]_

- **SC-007**: Field mapping interface supports drag-and-drop and click-to-map
  interactions with <500ms response time for field queries. Mapping save
  operations complete in <2 seconds. Visual connection lines between mapped
  fields render without performance degradation for 100+ mappings. _[Task
  Reference: T059, T060, T061, T062, T124]_

- **SC-008**: System maintains 99.9% uptime for ASTM message processing and
  mapping application. Message processing applies mappings in <100ms per message
  (non-blocking). System handles analyzer downtime gracefully without losing
  messages. _[Task Reference: T178, T179, T180, T181]_

- **SC-009**: Copy mappings operation completes in <10 seconds for analyzers
  with 100+ field mappings. Copied mappings preserve all mapping attributes
  (field type, unit mappings, qualitative mappings, validation rules) and can be
  edited before activation. _[Task Reference: T076, T192, T193, T194, T195,
  T196, T197]_

- **SC-010**: Test mapping preview validates sample ASTM messages and displays
  mapping results within 2 seconds. Preview shows: mapped OpenELIS fields, unit
  conversions (if applicable), qualitative value mappings, and any validation
  errors or warnings. _[Task Reference: T154, T155, T156, T157, T158, T160,
  T161, T162]_

### Qualitative Outcomes

- **SC-011**: Laboratory administrators report that mapping interface is
  intuitive and requires minimal training (<2 hours) to become proficient.
  Interface provides clear visual feedback, validation messages, and contextual
  help for mapping decisions. _[Task Reference: T059, T060, T061, T062, T117]_

- **SC-012**: Error resolution workflow reduces time to resolve unmapped
  messages compared to manual investigation and mapping creation. Error
  Dashboard provides sufficient context (raw code, analyzer, date/time, message
  segment) to create mappings directly from error context without additional
  investigation. _[Task Reference: T096, T097, T101]_

- **SC-013**: System prevents ambiguous or unsafe mappings (e.g.,
  text-to-numeric) through validation and user confirmation. Type compatibility
  validation blocks incompatible mappings and warns on potentially unsafe
  mappings (e.g., numeric-to-text) requiring explicit confirmation. _[Task
  Reference: T204, T205, T206]_

**Note on Success Criteria History**: SC-005 through SC-013 were derived from
the "Performance Goals" section in plan.md (lines 77-82). These performance
goals existed when plan.md was created (commit eed28598c) but were not
formalized as Success Criteria in spec.md until the remediation commit
(44e508cb2). Implementation was completed based on Functional Requirements (FRs)
and plan.md performance goals, which is why the functionality exists but the SCs
were not linked to tasks initially. The original SC-004 (support ticket
reduction - 50% reduction in 3 months) was replaced with the current SC-004
(query analyzer timeout and connection test) for better testability and
measurability.

## Reference Documentation

This specification is derived from and should be read in conjunction with:

- **OGC-49 Comprehensive Specification**: `.dev-docs/OGC-49/docs.md` and
  `.dev-docs/OGC-49/OpenELIS_ASTM_Analyzer_Mapping_Specification.pdf` - Contains
  detailed functional requirements, business rules, API endpoint specifications,
  validation rules, security/permissions model, and UI design guidance using
  Carbon Design System components.

- **Figma Design Artifacts**:
  - Analyzer Field Mapping Make:
    [`Analyzer-Field-Mapping-Feature`](https://www.figma.com/make/QseQZxQyOWsqciEpLjwkxb/Analyzer-Field-Mapping-Feature?node-id=0-1&t=wPkVXZVaIRyqh4SR-1) -
    Reference implementation showing component structure and interaction
    patterns
  - OGC-49 Design Pages:
    [`OGC-49`](https://www.figma.com/design/i63dxlyfZE8tvdoAibH55M/OGC-49?node-id=0-1&m=dev) -
    Detailed UI flows for analyzer management, field mapping, and error
    dashboard
  - Figma Design Analysis: `.dev-docs/OGC-49/figma-design-analysis.md` -
    Comprehensive analysis of Figma Make prototype and design pages, including
    application hierarchy, component structure, navigation flows, behavioral
    state machines, data models, user workflows, page layouts, and UI component
    details. This document provides detailed UI/UX specifications extracted from
    the Figma artifacts, including modal states, form field placeholders,
    statistics card layouts, table column definitions, badge styles, and
    interaction patterns.

**Note**: The OGC-49 specification document (docs.md) includes additional
implementation details such as:

- Specific Carbon Design System components to use (DataTable, ComposedModal,
  Search, MultiSelect, Tag, etc.)
- API endpoint specifications with request/response formats
- Validation rules with specific error messages
- Business rules for analyzer uniqueness, active status, mapping validation, and
  connection testing
- Field type detection and mapping patterns
- ASTM LIS2-A2 segment/field reference (Appendix A)

These details will be incorporated during the planning and implementation
phases. This specification focuses on user value and business requirements,
while the OGC-49 docs.md provides the technical implementation guidance.
