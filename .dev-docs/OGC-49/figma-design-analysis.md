# Figma Design Analysis: Analyzer Field Mapping Feature

**Date**: 2025-11-14  
**Sources**: 
- Figma Make - [Analyzer Field Mapping Feature](https://www.figma.com/make/QseQZxQyOWsqciEpLjwkxb/Analyzer-Field-Mapping-Feature?node-id=0-4&t=obsy3bJKKnPocoSp-1)
- Figma Design - [OGC-49](https://www.figma.com/design/i63dxlyfZE8tvdoAibH55M/OGC-49) (11 design pages analyzed)
**Status**: Design Reference Analysis (Complete)

## Executive Summary

This document summarizes findings from analyzing the Figma Make prototype for the Analyzer Field Mapping feature. The Make file contains a functional React/TypeScript prototype that demonstrates UI behavior, component structure, and user workflows. However, as a Make file (code prototype), it emphasizes implementation details rather than visual design metadata.

**Key Finding**: The prototype uses shadcn/ui components (Tailwind-based), which will need to be converted to Carbon Design System components per OpenELIS constitution requirements.

## Design Artifacts Retrieved

### Code Files Retrieved
- **Main Application**: `App.tsx` - Routing and navigation structure
- **Pages**: 
  - `AnalyzersPage.tsx` - Analyzer list and management
  - `FieldMappingPage.tsx` - Dual-panel mapping interface
  - `ErrorDashboardPage.tsx` - Error monitoring
  - `QualityControlPage.tsx` - QC management
- **Components**: 12 feature components + 50+ shadcn/ui base components
- **Type Definitions**: `types/analyzer.ts` - Complete TypeScript data model
- **Mock Data**: `data/mockData.ts` and `data/openELISFieldsData.ts`

### Image Assets Retrieved
11 PNG screenshots documented in `screenshots/README.md`:
- `01-analyzers-page.png` - Main Analyzers list page
- `02-analyzers-with-overflow-menu.png` - Analyzers page showing overflow menu
- `03-test-connection-modal.png` - Test Connection modal (initial state)
- `04-test-connection-progress.png` - Test Connection modal (progress state)
- `05-test-connection-success.png` - Test Connection modal (success state)
- `06-copy-mappings-modal.png` - Copy Field Mappings modal
- `07-delete-analyzer-modal.png` - Delete Analyzer confirmation modal
- `08-add-analyzer-modal.png` - Add New Analyzer modal
- `09-quality-control-dashboard.png` - Quality Control dashboard (out of scope)
- `10-error-dashboard.png` - Error Dashboard page
- `11-error-details-modal.png` - Error Details modal

**Note**: Screenshots were captured from Figma Design file nodes. Metadata analysis from design pages provides structural details that complement the visual screenshots.

## Application Hierarchy & Structure

### High-Level Page Hierarchy

```
App (Router)
├── Header Navigation
│   ├── OpenELIS Logo + Title
│   └── Navigation Links
│       ├── Analyzers (default route)
│       ├── Quality Control
│       └── Error Dashboard
│
├── AnalyzersPage (/)
│   ├── Page Header
│   │   ├── Title: "Analyzers"
│   │   └── Primary Action: "Add Analyzer" button
│   │
│   ├── Statistics Section (3-column grid)
│   │   ├── Total Analyzers card
│   │   ├── Active Analyzers card
│   │   └── Inactive Analyzers card
│   │
│   ├── Filter Bar
│   │   ├── Search Input (debounced)
│   │   ├── Status Filter (dropdown)
│   │   └── Type Filter (dropdown)
│   │
│   ├── Analyzers DataTable
│   │   ├── Columns: Name, Type, Connection, Test Units, Status, Last Modified, Actions
│   │   ├── Row Actions (OverflowMenu)
│   │   │   ├── Field Mappings → Navigate to FieldMappingPage
│   │   │   ├── Test Connection → Open TestConnectionModal
│   │   │   ├── Copy Mappings → Open CopyMappingsModal
│   │   │   ├── Edit → Open AnalyzerModal (edit mode)
│   │   │   └── Delete → Open DeleteConfirmationModal
│   │   └── Inline Status Toggle
│   │
│   └── Modals (conditionally rendered)
│       ├── AnalyzerModal (Add/Edit)
│       ├── DeleteConfirmationModal
│       ├── TestConnectionModal
│       └── CopyMappingsModal
│
├── FieldMappingPage (/analyzers/:id/mappings)
│   ├── Page Header
│   │   ├── Back Button → Navigate to AnalyzersPage
│   │   ├── Analyzer Name (title)
│   │   └── Primary Action: "Save Mappings" button
│   │
│   ├── Validation Warning Card (conditional)
│   │   └── Missing Required Mappings alert
│   │
│   ├── Statistics Section (3-column grid)
│   │   ├── Total Mappings card
│   │   ├── Required Mappings card
│   │   └── Unmapped Fields card
│   │
│   └── Dual-Panel Interface (50/50 grid, stacks on mobile)
│       ├── Left Panel: Analyzer Fields (Source)
│       │   ├── Panel Header
│       │   │   ├── Title: "Analyzer Fields ({type})"
│       │   │   └── Search Input (with icon)
│       │   │
│       │   ├── Fields Table (scrollable)
│       │   │   ├── Columns: Field Name, ASTM Ref, Type, Unit, Action
│       │   │   └── FieldItem rows (clickable)
│       │   │       ├── Visual mapping indicator
│       │   │       ├── Field type badge
│       │   │       └── Click handler → Select field
│       │   │
│       │   └── Status Footer: "{count} fields available"
│       │
│       └── Right Panel: Mapping Context (conditional)
│           ├── State A: Mappings Summary (no selection)
│           │   └── Card with list of existing mappings
│           │       └── Clickable mapping items → Select field
│           │
│           └── State B: MappingPanel (field selected)
│               ├── View Mode (existing mapping)
│               │   ├── Header with Edit/Remove buttons
│               │   ├── Source/Target field cards
│               │   ├── Unit conversion display (if numeric)
│               │   ├── Qualitative mappings list (if qualitative)
│               │   └── Required/Optional indicator
│               │
│               └── Edit Mode (create/edit)
│                   ├── Source field info (read-only)
│                   ├── OpenELISFieldSelector
│                   ├── Unit Mapping section (if numeric)
│                   ├── Qualitative Mapping section (if qualitative)
│                   └── Action buttons (Save/Cancel)
│
├── ErrorDashboardPage (/errors)
│   ├── Page Header
│   │   ├── Title: "Error Dashboard"
│   │   └── Primary Action: "Acknowledge All" button (with icon)
│   │
│   ├── Statistics Section (4-column grid)
│   │   ├── Total Errors card (large number display)
│   │   ├── Unacknowledged card (large number display)
│   │   ├── Critical card (large number display)
│   │   └── Last 24 Hours card (large number display)
│   │
│   ├── Filter Bar
│   │   ├── Search Input (placeholder: "Search errors...")
│   │   ├── Error Type Filter dropdown ("All Types")
│   │   ├── Severity Filter dropdown ("All Severities")
│   │   └── Analyzer Filter dropdown ("All")
│   │
│   ├── Errors DataTable
│   │   ├── Columns: Timestamp, Analyzer, Type, Severity, Message, Status, Actions
│   │   ├── Error Type badges: connection, mapping, validation, timeout, protocol
│   │   ├── Severity badges: critical, error, warning
│   │   ├── Status badges: Unacknowledged (with icon), Acknowledged (with checkmark icon)
│   │   └── Row Actions (OverflowMenu)
│   │       ├── View Details → Open ErrorDetailsModal
│   │       └── Acknowledge (if unacknowledged)
│   │
│   └── Modals
│       └── ErrorDetailsModal
│           ├── Error context display
│           ├── Analyzer logs section
│           └── Recommended actions section
│
└── QualityControlPage (/quality-control)
    └── [Not detailed in prototype - out of scope for this feature]
```

### Component Hierarchy

#### Feature Components (Application-Specific)

```
Feature Components
│
├── AnalyzerModal
│   ├── Dialog Header
│   │   ├── Title: "Add New Analyzer" / "Edit Analyzer"
│   │   └── Subtitle: "Configure analyzer connection settings and test units"
│   ├── Form Fields
│   │   ├── Analyzer Name * (TextInput)
│   │   │   └── Placeholder: "e.g., Hematology Analyzer 1"
│   │   ├── Analyzer Type * (Select)
│   │   │   └── Placeholder: "Select analyzer type"
│   │   ├── IP Address * (TextInput, half-width)
│   │   │   └── Placeholder: "192.168.1.10"
│   │   ├── Port * (NumberInput, half-width)
│   │   │   └── Placeholder: "5000"
│   │   ├── Protocol Version (TextInput, read-only)
│   │   │   └── Default value: "ASTM LIS2-A2"
│   │   ├── Test Units * (MultiSelect)
│   │   │   ├── Placeholder: "Select test units..."
│   │   │   └── Helper text: "Select one or more test units for this analyzer"
│   │   └── Active Status (Toggle)
│   │       ├── Label: "Active Status"
│   │       └── Description: "Enable this analyzer for data collection"
│   └── Dialog Footer
│       ├── Cancel button
│       └── Add Analyzer / Save button
│
├── TestConnectionModal
│   ├── Dialog Header
│   │   ├── Title: "Test Connection"
│   │   └── Subtitle: "Testing connection to {analyzer name}"
│   ├── Analyzer Information Section (read-only)
│   │   ├── Analyzer: {name}
│   │   ├── Type: {type}
│   │   ├── Connection: {IP:Port}
│   │   └── Protocol: {protocol version}
│   ├── Connection Status Section (conditional)
│   │   ├── Initial State: "Test Connection" button
│   │   ├── Progress State: 
│   │   │   ├── Loading icon with "Testing connection..." text
│   │   │   ├── Progress bar with percentage (e.g., "50% complete")
│   │   │   └── Connection Logs section (collapsible)
│   │   │       ├── Log entries with timestamps [HH:MM:SS.mmm]
│   │   │       ├── Log levels: info (ℹ), debug (◆), warning (⚠), error (✗)
│   │   │       └── Log messages (e.g., "Starting connection test", "Resolving IP address...", "TCP handshake initiated")
│   │   └── Success State:
│   │       ├── Success icon with "Connection Successful" message
│   │       ├── Success description text
│   │       ├── Connection Logs section (expanded, 20+ entries)
│   │       │   ├── Detailed connection steps with timestamps
│   │       │   ├── DNS lookup entries
│   │       │   ├── TCP connection establishment
│   │       │   ├── ASTM protocol verification (ENQ/ACK handshake)
│   │       │   ├── Data transfer testing
│   │       │   └── Checksum calculation and verification
│   │       └── Footer buttons: "Close", "Test Again"
│   └── Dialog Footer
│       ├── Cancel button (initial state)
│       ├── Test Connection button (initial state)
│       ├── Testing... button (progress state, disabled)
│       └── Close / Test Again buttons (success state)
│
├── DeleteConfirmationModal
│   ├── Alert Dialog Header
│   │   ├── Title: "Delete Analyzer"
│   │   └── Warning Message: "Are you sure you want to delete {analyzer name}? This action cannot be undone and will remove all associated data."
│   └── Alert Dialog Footer
│       ├── Cancel button
│       └── Delete button (destructive)
│
├── CopyMappingsModal
│   ├── Dialog Header
│   │   ├── Title: "Copy Field Mappings"
│   │   └── Subtitle: "Copy field mappings from {source analyzer} to {target analyzer}"
│   ├── Source Analyzer Section (read-only)
│   │   ├── Label: "Source Analyzer"
│   │   ├── Analyzer name display
│   │   └── Analyzer type display
│   ├── Target Analyzer Section
│   │   ├── Label: "Target Analyzer *" (required)
│   │   └── Dropdown selector: "Select target analyzer"
│   ├── Warning Note Section
│   │   └── Note: "This will copy all field mappings including unit conversions and qualitative value mappings. Existing mappings will be overwritten."
│   └── Dialog Footer
│       ├── Cancel button
│       └── Copy Mappings button (with icon)
│
├── FieldItem
│   ├── Field Name
│   ├── ASTM Reference (optional)
│   ├── Field Type Badge
│   ├── Unit Badge (optional)
│   ├── Mapping Indicator (visual)
│   └── Click Handler
│
├── MappingPanel
│   ├── View Mode Components
│   │   ├── Source Field Card
│   │   ├── Target Field Card
│   │   ├── Unit Mapping Display (conditional)
│   │   ├── Qualitative Mappings Display (conditional)
│   │   └── Action Buttons (Edit, Remove)
│   │
│   └── Edit Mode Components
│       ├── Source Field Info (read-only)
│       ├── OpenELISFieldSelector
│       ├── Unit Mapping Form (conditional)
│       ├── Qualitative Mapping Form (conditional)
│       └── Action Buttons (Save, Cancel)
│
├── OpenELISFieldSelector
│   ├── Main Dropdown Trigger
│   ├── Search Input (in popover)
│   ├── Category Filter Button
│   ├── Category Filter Popover
│   │   ├── Category Checkboxes (8 categories)
│   │   └── Select All/None buttons
│   ├── Field List (grouped by category)
│   │   └── Field Items with metadata
│   └── Active Filter Pills (conditional)
│
├── QualitativeMappingModal
│   ├── Analyzer Values List
│   ├── Target Value Dropdowns (per analyzer value)
│   └── Action Buttons (Save, Cancel)
│
├── UnitMappingModal
│   ├── Source Unit Input
│   ├── Target Unit Select
│   ├── Conversion Factor Input
│   └── Action Buttons (Save, Cancel)
│
└── ErrorDetailsModal
    ├── Dialog Header
    │   ├── Title: "Error Details"
    │   └── Subtitle: "Detailed information and analyzer logs for error {errorId}"
    ├── Error Information Section
    │   ├── Error ID: {id}
    │   ├── Timestamp: {formatted date/time}
    │   ├── Analyzer: {name}
    │   ├── Analyzer ID: {id}
    │   ├── Error Type: {badge} (connection, mapping, validation, timeout, protocol)
    │   ├── Severity: {badge} (critical, error, warning)
    │   └── Error Message: {full message text}
    ├── Acknowledgment Status Section (conditional)
    │   ├── Acknowledged icon
    │   ├── Status: "Acknowledged"
    │   └── Details: "By {user} on {date/time}"
    ├── Analyzer Logs Section (collapsible)
    │   ├── Header: "Analyzer Logs ({count} entries)" with expand/collapse icon
    │   ├── Action Buttons: "Copy", "Download"
    │   └── Log Entries (scrollable)
    │       ├── Timestamp: [HH:MM:SS.mmm]
    │       ├── Log Level Icon: ℹ (info), ◆ (debug), ⚠ (warning), ✗ (error)
    │       ├── Log Level Label: info, debug, warning, error
    │       └── Log Message: {detailed message}
    │       └── Example entries:
    │           ├── "Analyzer {name} online and operational"
    │           ├── "Last successful data transmission: Sample ID {id}"
    │           ├── "TCP connection established: {IP:Port}"
    │           ├── "Network latency detected: {ms}ms (threshold: {ms}ms)"
    │           ├── "Connection retry attempt {n}/3"
    │           ├── "Connection timeout after {n} seconds"
    │           └── "TCP socket closed unexpectedly"
    ├── Recommended Actions Section
    │   ├── Heading: "Recommended Actions"
    │   └── Action Items (bullet list with icons):
    │       ├── "Verify analyzer is powered on and network cable is connected"
    │       ├── "Check IP address and port configuration in analyzer settings"
    │       └── "Test connection using the 'Test Connection' feature"
    └── Dialog Footer
        └── Close button
```

#### Base UI Components (shadcn/ui - to be converted to Carbon)

```
Base UI Components (shadcn/ui → Carbon)
│
├── Button → Carbon Button
├── Card → Carbon Tile / StructuredList
├── Table → Carbon DataTable
│   ├── TableHeader
│   ├── TableBody
│   ├── TableRow
│   ├── TableHead
│   └── TableCell
├── Input → Carbon TextInput
├── Select → Carbon Select
│   ├── SelectTrigger
│   ├── SelectContent
│   └── SelectItem
├── Badge → Carbon Tag
├── Modal → Carbon ComposedModal
│   ├── ModalHeader
│   ├── ModalBody
│   └── ModalFooter
├── DropdownMenu → Carbon OverflowMenu
├── Accordion → Carbon Accordion
├── Checkbox → Carbon Checkbox
├── Toggle → Carbon Toggle
├── Label → Carbon Label
└── [50+ additional base components]
```

### Navigation Flow

```
Navigation Flow
│
├── Entry Point: AnalyzersPage (/)
│   └── Default landing page
│
├── From AnalyzersPage:
│   ├── "Add Analyzer" → AnalyzerModal → Save → Refresh list
│   ├── Row Action "Field Mappings" → FieldMappingPage (/analyzers/:id/mappings)
│   ├── Row Action "Test Connection" → TestConnectionModal
│   ├── Row Action "Copy Mappings" → CopyMappingsModal
│   ├── Row Action "Edit" → AnalyzerModal (edit mode) → Save → Refresh
│   ├── Row Action "Delete" → DeleteConfirmationModal → Confirm → Refresh
│   └── Navigation Link → ErrorDashboardPage (/errors)
│
├── From FieldMappingPage:
│   ├── Back Button → AnalyzersPage (/)
│   ├── "Save Mappings" → Save all → Stay on page
│   ├── Click Analyzer Field → Select field → MappingPanel appears
│   ├── MappingPanel "Edit" → Switch to edit mode
│   ├── MappingPanel "Remove" → Remove mapping → Return to summary
│   └── OpenELISFieldSelector → Select field → Update mapping
│
├── From ErrorDashboardPage:
│   ├── Navigation Link → AnalyzersPage (/)
│   ├── Row Action "View Details" → ErrorDetailsModal
│   ├── ErrorDetailsModal "Create Mapping" → Mapping interface (contextual)
│   └── Row Action "Acknowledge" → Mark acknowledged → Refresh
│
└── Modal Workflows (non-navigational):
    ├── AnalyzerModal → Test Connection → TestConnectionModal
    ├── CopyMappingsModal → Confirm → Apply mappings → Close
    └── ErrorDetailsModal → Create Mapping → Mapping interface → Save → Close
```

### Behavioral State Machine

```
Field Mapping Page States
│
├── Initial State: No field selected
│   ├── Left Panel: All analyzer fields visible
│   └── Right Panel: Mappings summary list
│
├── Field Selected State: User clicks analyzer field
│   ├── Left Panel: Selected field highlighted
│   └── Right Panel: MappingPanel appears
│       ├── If mapping exists → View mode
│       └── If no mapping → Edit mode (create)
│
├── Edit Mode State: User clicks "Edit" or creates new mapping
│   ├── MappingPanel switches to edit form
│   ├── OpenELISFieldSelector enabled
│   ├── Unit/Qualitative sections appear (if applicable)
│   └── Save button enabled when target field selected
│
└── Save State: User clicks "Save" or "Create Mapping"
    ├── Validation runs
    ├── If valid → Save → Return to view mode
    ├── If invalid → Show errors → Stay in edit mode
    └── Mapping stats update
```

### Data Flow Hierarchy

```
Data Flow
│
├── AnalyzersPage
│   ├── Load: GET /api/analyzers → Display in table
│   ├── Create: POST /api/analyzers → Refresh list
│   ├── Update: PUT /api/analyzers/:id → Refresh list
│   ├── Delete: DELETE /api/analyzers/:id → Refresh list
│   └── Test: POST /api/analyzers/:id/test-connection → Show result
│
├── FieldMappingPage
│   ├── Load Analyzer: GET /api/analyzers/:id → Display header
│   ├── Load Fields: POST /api/analyzers/:id/query-fields → Populate left panel
│   ├── Load Mappings: GET /api/analyzers/:id/mappings → Display in summary/panel
│   ├── Load OpenELIS Fields: GET /api/openelis-fields → Populate selector
│   ├── Save Mappings: POST /api/analyzers/:id/mappings → Update state
│   └── Update Mapping: PUT /api/analyzers/:id/mappings/:mid → Refresh panel
│
├── ErrorDashboardPage
│   ├── Load Errors: GET /api/analyzer-errors → Display in table
│   ├── Acknowledge: POST /api/analyzer-errors/:id/acknowledge → Update row
│   └── Bulk Acknowledge: POST /api/analyzer-errors/acknowledge-all → Refresh
│
└── CopyMappingsModal
    ├── Load Source Mappings: GET /api/analyzers/:sourceId/mappings
    └── Copy: POST /api/analyzers/:targetId/mappings/copy → Refresh target page
```

## Page Structure & Layout Patterns

### 1. Analyzers List Page (`AnalyzersPage.tsx`)

**Layout Structure**:
- Header section with title "Analyzers" and "Add Analyzer" primary button
- Statistics cards (3-column grid): Total Analyzers, Active, Inactive
- Filter bar: Search input + Status dropdown + Type dropdown
- Data table with columns: Name, Type, Connection (IP:Port), Test Units, Status, Last Modified, Actions
- Row actions via dropdown menu: Field Mappings, Test Connection, Copy Mappings, Edit, Delete

**Key Behaviors**:
- Search debounced across Name, Type, IP fields
- Status filter: All, Active, Inactive
- Type filter: Dynamic list from analyzer types
- Inline status badges (Active/Inactive)
- Test units displayed as badge chips
- Modals: AnalyzerModal, DeleteConfirmationModal, TestConnectionModal, CopyMappingsModal

**Component Pattern**: Card-based layout with DataTable, filter controls, and modal dialogs

### 2. Field Mapping Page (`FieldMappingPage.tsx`)

**Layout Structure**:
- Header with back button, analyzer name/title, "Save Mappings" button
- Validation warning card (yellow) if required mappings missing
- Statistics cards (3-column): Total Mappings, Required Mappings, Unmapped Fields
- **Dual-panel interface** (50/50 grid, stacks on mobile):
  - **Left Panel**: Analyzer Fields (Source)
    - Searchable field list in table format
    - Columns: Field Name, ASTM Ref, Type, Unit, Action
    - Scrollable with max-height
    - Status footer: "X fields available"
  - **Right Panel**: Mapping Panel or Summary
    - Shows current mappings summary when no field selected
    - Shows detailed mapping panel when field selected

**Key Behaviors**:
- Click analyzer field → opens mapping panel on right
- Visual indicators for mapped vs unmapped fields
- Required mappings validation (Sample ID, Test Code, Result Value)
- Mapping stats update dynamically
- Field search filters analyzer fields in real-time

**Component Pattern**: Two-column responsive grid with interactive field selection and contextual mapping panel

### 3. Mapping Panel Component (`MappingPanel.tsx`)

**Two States**:

**State 1: View Existing Mapping**
- Header with mapping title and Edit/Remove buttons
- Source and Target field cards (side-by-side)
- Unit conversion display (if numeric)
- Qualitative value mappings list (if qualitative)
- Required/Optional indicator

**State 2: Create/Edit Mapping**
- Source field info card (read-only, blue background)
- OpenELIS Field Selector (searchable, categorized dropdown)
- Unit Mapping section (if numeric):
  - Source unit input
  - Target unit dropdown (from OpenELIS field accepted units)
  - Conversion factor input
- Qualitative Value Mapping section (if qualitative):
  - List of analyzer values with target value dropdowns
  - Test-specific filtering (if test code mapped)
- Action buttons: Create/Update Mapping, Cancel

**Key Behaviors**:
- Field type filtering (only compatible types shown)
- Unit dropdown populated from target field's accepted units
- Qualitative mappings initialized from analyzer field values
- Conversion factor defaults to 1.0
- Validation: Target field required before save

**Component Pattern**: Card-based form with conditional sections based on field types

### 4. OpenELIS Field Selector (`OpenELISFieldSelector.tsx`)

**Complex Component Features**:
- Searchable dropdown with category filtering
- 8 categories: Tests, Panels, Results, Order, Sample, QC, Metadata, Units
- Category filter popover with checkboxes (Select All/None)
- Color-coded category badges
- Field display shows: Name, LOINC code, Entity, Field Type, Accepted Units
- Type filtering (only shows compatible field types)
- Active filter pills display when categories filtered

**Key Behaviors**:
- Search across name, entity, LOINC code, description
- Category filtering persists during search
- Type filtering enforced (numeric→numeric, qualitative→qualitative)
- Visual grouping by category in dropdown
- Field count per category displayed

**Component Pattern**: Advanced combobox with multi-level filtering and categorization

## Data Model (From TypeScript Types)

### Core Entities

**Analyzer**:
```typescript
{
  id: string;
  name: string;
  analyzerType: string;
  ipAddress: string;
  port: number;
  protocolVersion: string;
  isActive: boolean;
  testUnits: string[];
  createdAt: string;
  lastModified: string;
  createdBy: string;
}
```

**AnalyzerField** (Source):
```typescript
{
  id: string;
  name: string;
  fieldType: 'numeric' | 'qualitative' | 'control' | 'meltingPoint' | 'dateTime' | 'text' | 'custom';
  astmReference?: string;
  unit?: string;
  description?: string;
  messageContext?: {
    recordType: string;
    sequencePosition: number;
    exampleValue: string;
    fullMessage: string;
  };
}
```

**OpenELISField** (Target):
```typescript
{
  id: string;
  name: string;
  entity: string;
  fieldType: FieldType;
  loincCode?: string;
  acceptedUnits?: string[];
  acceptedValues?: string[];
  description?: string;
  category: 'test' | 'panel' | 'result' | 'order' | 'sample' | 'qc' | 'metadata' | 'unit';
}
```

**FieldMapping**:
```typescript
{
  id: string;
  analyzerId: string;
  sourceField: AnalyzerField;
  targetField: OpenELISField;
  unitMapping?: {
    sourceUnit: string;
    targetUnit: string;
    conversionFactor?: number;
  };
  qualitativeMapping?: Array<{
    sourceValue: string;
    targetValue: string;
    isDefault?: boolean;
  }>;
  isRequired: boolean;
  createdAt: string;
  lastModified: string;
}
```

**AnalyzerError**:
```typescript
{
  id: string;
  analyzerId: string;
  analyzerName: string;
  errorType: 'connection' | 'mapping' | 'validation' | 'timeout' | 'protocol';
  errorMessage: string;
  timestamp: string;
  severity: 'critical' | 'error' | 'warning';
  isAcknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  metadata?: Record<string, any>;
}
```

## User Workflows Identified

### Workflow 1: Register and Configure New Analyzer
1. Navigate to Analyzers page
2. Click "Add Analyzer"
3. Fill form: Name, Type, IP, Port, Test Units
4. Optionally test connection
5. Save analyzer
6. Navigate to Field Mappings
7. Query analyzer for fields (or manually enter)
8. Map each field to OpenELIS field
9. Configure unit mappings (if numeric)
10. Configure qualitative value mappings (if qualitative)
11. Save mappings
12. Validate required mappings present

### Workflow 2: Copy Mappings from Similar Analyzer
1. Open analyzer in list
2. Click "Copy Mappings" from actions menu
3. Select source analyzer from searchable list
4. Review mapping preview
5. Confirm copy (with overwrite warning)
6. Review copied mappings in mapping interface
7. Adjust as needed

### Workflow 3: Resolve Unmapped Field Error
1. Navigate to Error Dashboard
2. View unmapped field error
3. Click error to open details
4. Open mapping interface from error context
5. Select unmapped analyzer field
6. Map to appropriate OpenELIS field
7. Save mapping
8. Trigger reprocessing of affected messages

### Workflow 4: Edit Existing Mapping
1. Navigate to analyzer's Field Mapping page
2. Click mapped field in source panel
3. Mapping panel shows existing mapping
4. Click "Edit" button
5. Modify target field, units, or qualitative values
6. Save changes
7. Changes apply to new messages only

## Design Patterns & UI Behaviors

### Visual Indicators
- **Field Type Colors**: Color-coded badges for field types (though specific colors not defined in Make file)
- **Mapping Status**: Visual distinction between mapped/unmapped fields
- **Required Mappings**: Warning card when required mappings missing
- **Status Badges**: Active (green), Inactive (gray), Required (checkmark icon)

### Interaction Patterns
- **Click-to-Map**: Click source field → mapping panel opens on right
- **Search & Filter**: Real-time filtering with debounced search
- **Modal Workflows**: Multi-step workflows in modals (Analyzer, Test Connection, Copy Mappings)
- **Inline Editing**: Edit mappings without leaving page
- **Contextual Actions**: Actions menu per row, contextual buttons in panels

### Responsive Behavior
- Two-column grid stacks vertically on mobile (<1024px)
- Table scrolls horizontally on small screens
- Modals adapt to viewport size
- Filter pills wrap on narrow screens

### Validation & Feedback
- Required field validation before save
- Type compatibility checking (numeric→numeric, etc.)
- Unit mismatch warnings
- Missing required mappings warning card
- Success/error messages (implied, not shown in code)

## Limitations of Figma Make Files

### What Make Files Provide
✅ Functional code structure  
✅ Component relationships  
✅ Data model definitions  
✅ User workflow logic  
✅ Interaction patterns  
✅ Mock data examples  

### What Make Files Don't Provide
❌ Visual design tokens (colors, spacing, typography values)  
❌ Layout metadata (exact positions, sizes)  
❌ Design system variables  
❌ Visual hierarchy information  
❌ Animation/transition specifications  
❌ Accessibility annotations  
❌ Responsive breakpoint details  

**Note**: The `get_metadata` and `get_screenshot` tools do not work with Make files—they only work with traditional Figma Design files. The screenshots retrieved are static image assets, not interactive design metadata.

## Alignment with OGC-49 Specification

### Matches Specification
✅ Dual-panel mapping interface (50/50 grid)  
✅ Analyzer list with search/filter  
✅ Field type detection and categorization  
✅ Unit mapping for numeric fields  
✅ Qualitative value mapping  
✅ Copy mappings functionality  
✅ Error dashboard structure  
✅ Required mappings validation  
✅ Test connection capability  

### Deviations from Specification
⚠️ **Design System**: Uses shadcn/ui (Tailwind) instead of Carbon Design System  
⚠️ **Component Library**: Uses shadcn/ui components instead of Carbon components  
⚠️ **Styling Approach**: Tailwind utility classes instead of Carbon tokens  
⚠️ **Internationalization**: Hardcoded English strings (needs React Intl)  

### Missing from Prototype (Per Spec)
- Pagination controls (mentioned in spec, not visible in code)
- Audit trail UI (data model exists, UI not shown)
- Test mapping/preview capability (FR-007)
- Custom field type creation UI (FR-018)
- Inline OpenELIS field creation (FR-019)
- Message reprocessing UI
- Date range filter in Error Dashboard (filter bar shows "All" but no date picker visible)

### Additional Details from Figma Design Pages

#### Test Connection Modal States
- **Initial State**: Shows analyzer information (name, type, connection, protocol) with "Test Connection" button
- **Progress State**: 
  - Loading indicator with "Testing connection..." text
  - Progress bar showing percentage completion (e.g., "50% complete")
  - Collapsible connection logs section with detailed step-by-step connection process
  - Log entries include timestamps in [HH:MM:SS.mmm] format
  - Log levels visually distinguished: info (ℹ), debug (◆), warning (⚠), error (✗)
- **Success State**:
  - Success icon with "Connection Successful" heading
  - Descriptive success message
  - Expanded connection logs (20+ entries) showing full connection sequence:
    - DNS lookup and resolution
    - TCP connection establishment
    - ASTM protocol handshake (ENQ 0x05 / ACK 0x06)
    - Protocol verification
    - Data transfer testing with checksum calculation
    - Frame acknowledgment
  - Footer buttons: "Close" and "Test Again"

#### Error Dashboard Enhancements
- **Statistics Cards**: Four cards in grid layout (Total Errors, Unacknowledged, Critical, Last 24 Hours)
- **Filter Bar**: Three filter dropdowns (Error Type, Severity, Analyzer) plus search input
- **Error Table Columns**: Timestamp (formatted as "MM/DD/YYYY, HH:MM:SS AM/PM"), Analyzer, Type (badge), Severity (badge), Message (truncated), Status (badge with icon), Actions
- **Status Badges**: 
  - Unacknowledged: Badge with warning icon
  - Acknowledged: Badge with checkmark icon
- **Bulk Action**: "Acknowledge All" button in page header

#### Error Details Modal Enhancements
- **Error Metadata Grid**: Two-column layout showing Error ID, Timestamp, Analyzer, Analyzer ID, Error Type (badge), Severity (badge)
- **Acknowledgment Display**: Shows acknowledgment status with user name and timestamp when acknowledged
- **Analyzer Logs**:
  - Collapsible section with entry count in header
  - Copy and Download action buttons
  - Scrollable log list with formatted entries
  - Log levels: info, debug, warning, error (with visual icons)
  - Timestamps in [HH:MM:SS.mmm] format
  - Detailed messages showing connection state, retry attempts, errors
- **Recommended Actions**: Bulleted list with icon indicators providing troubleshooting guidance

## Key Observations for Implementation

### 1. Component Conversion Required
The prototype uses shadcn/ui components that must be converted to Carbon:
- `Button` → Carbon `Button`
- `Card` → Carbon `Tile` or `StructuredList`
- `Table` → Carbon `DataTable`
- `Select` → Carbon `Select`
- `Input` → Carbon `TextInput`
- `Modal` → Carbon `ComposedModal`
- `Badge` → Carbon `Tag`
- `Accordion` → Carbon `Accordion`
- `Dropdown` → Carbon `Dropdown` or `OverflowMenu`

### 2. Layout Patterns to Preserve
- Two-column responsive grid for mapping interface
- Card-based statistics display
- Search + filter bar pattern
- Modal-based workflows
- Inline editing pattern

### 3. Data Model Alignment
The TypeScript types align well with the OGC-49 specification entities:
- `Analyzer` matches spec requirements
- `FieldMapping` includes unit and qualitative mappings
- `AnalyzerError` supports error dashboard
- Field types match spec definitions

### 4. Workflow Completeness
The prototype demonstrates core workflows but may need:
- Audit trail UI components
- Message reprocessing interface
- Test mapping/preview capability
- Bulk operations UI

### 5. Internationalization Gap
All user-facing strings are hardcoded in English. Implementation must:
- Extract all strings to React Intl message files
- Support at least English and French (per constitution)
- Use `intl.formatMessage()` for all labels, messages, tooltips

## Recommendations

### Immediate Actions
1. **Review Screenshots**: Examine the 10 downloaded screenshots to understand visual design intent
2. **Carbon Component Mapping**: Create mapping document from shadcn/ui → Carbon components
3. **Design Token Extraction**: Work with design team to extract visual tokens if traditional Figma Design file available
4. **Workflow Validation**: Validate that prototype workflows match OGC-49 specification requirements

### Implementation Considerations
1. **Progressive Enhancement**: Start with core mapping interface, add advanced features incrementally
2. **Component Library**: Build Carbon-based component library before full implementation
3. **Data Layer**: Backend entities should align with TypeScript types (with Java naming conventions)
4. **Testing Strategy**: Use prototype workflows as basis for E2E test scenarios

### Design System Integration
1. **Carbon Theme**: Apply OpenELIS Carbon theme tokens
2. **Spacing**: Use Carbon spacing scale ($spacing-05, $spacing-07)
3. **Typography**: Use Carbon typography tokens ($heading-04, $body-01, etc.)
4. **Color System**: Use Carbon color tokens for field type indicators
5. **Accessibility**: Ensure WCAG 2.1 AA compliance with Carbon components

## Next Steps

1. **Design Review**: Review screenshots with design team to clarify visual intent
2. **Component Planning**: Create detailed component breakdown using Carbon equivalents
3. **Spec Alignment**: Cross-reference prototype with OGC-49 spec to identify gaps
4. **Implementation Planning**: Use this analysis to inform implementation plan and tasks

## References

- **Figma Make**: [Analyzer Field Mapping Feature](https://www.figma.com/make/QseQZxQyOWsqciEpLjwkxb/Analyzer-Field-Mapping-Feature?node-id=0-4&t=obsy3bJKKnPocoSp-1)
- **OGC-49 Specification**: `.dev-docs/OGC-49/docs.md`
- **Feature Spec**: `specs/004-astm-analyzer-mapping/spec.md`
- **Screenshots**: `figma-screenshots/` directory (10 PNG files)
- **OpenELIS Constitution**: `.specify/memory/constitution.md` (v1.7.0)

