# Field Mappings Page Design Specifications

**Figma File**: LKzqNAGc3MMQlJTF4JaPBC  
**Nodes**: 1-1887 (Main Page), 1-2532 (Left Panel), 1-2533 (Right Panel)  
**Date**: 2025-11-19

## Field Mappings Page (Node 1-1887) - FULL PAGE VIEW

### Page Structure

**CRITICAL**: This is a FULL-PAGE view, NOT a modal overlay

### Page Title & Navigation

- **Back Arrow**: Left arrow icon button (ArrowLeft from @carbon/icons-react)
  - Position: Top left, before title
  - Style: `kind="ghost"` button
  - Action: Returns to `/analyzers` dashboard
- **Title**: "Hematology Analyzer 1" (analyzer name, large)
  - Font: Carbon heading-04 or heading-05
- **Subtitle**: "Configure field mappings between analyzer and OpenELIS"
  - Font: Carbon body-01
  - Color: --cds-text-secondary

### Warning Banner (Conditional)

- **Display When**: Required mappings are missing
- **Component**: InlineNotification
- **Kind**: "warning" (yellow background)
- **Icon**: Warning triangle icon
- **Title**: "Required mappings missing"
- **Message**: "The following required fields need to be mapped: Result Value"
- **Dismissible**: No close button (persistent until fixed)

### Statistics Cards

- **Layout**: 3-card grid (non-equal widths for visual hierarchy)
  - Column 1 (lg={5}): "Total Mappings" - Value: 5
  - Column 2 (lg={6}): "Required Mappings" - Value: 3
  - Column 3 (lg={5}): "Unmapped Fields" - Value: 8
- **Grid Total**: 5+6+5 = 16 columns
- **Styling**:
  - Background: `var(--cds-layer-01)`
  - Same styling as dashboard tiles
  - Label above value

### Action Buttons

- **Position**: Top right of page (below title/subtitle)
- **Buttons**:
  - "Save Mappings": Primary button (black background), icon: Save icon
  - (Query Analyzer button visible in earlier design, likely tertiary)

### Dual Panel Layout

#### Left Panel: Analyzer Fields (Node 1-2532)

- **Title**: "Analyzer Fields (Sysmex XN-1000)"
  - Shows analyzer model/type in parentheses
- **Search Bar**:
  - Placeholder: "Search analyzer fields..."
  - Carbon Search component
  - Full width within panel
- **Field Count**: "13 fields available" (below table)
- **Table Columns**:

  1. Expand icon (chevron for expandable rows)
  2. Mapping indicator (star icon + checkmark for mapped fields)
  3. Field Name (with entity type shown below: "→ Accession", "→ Sample
     Received", etc.)
  4. ASTM Ref (e.g., "01|1|SampID", "01|1|TestCode")
  5. Type (badge: "text", "numeric", "qualitative", "dateTime", "control")
  6. Unit (e.g., "10^9/L", "g/dL", "%")
  7. Actions (OverflowMenu three dots)

- **Row Highlighting**:

  - Mapped fields: Green left border (3-4px solid green)
  - Selected field: Light background highlight
  - Expandable rows: Chevron rotates on expand, shows nested fields

- **Field Type Badges**:
  - text: gray/neutral badge
  - numeric: blue badge
  - qualitative: purple badge
  - dateTime: cyan badge
  - control: yellow/gold badge

#### Right Panel: Current Mappings Summary (Node 1-2533)

**Title**: "Current Mappings Summary" (View Mode) or "Create New Mapping" (Edit
Mode)

**View Mode** (when mapped field selected):

- Shows mapped OpenELIS field details
- Field name with checkmark icon
- Entity type: "→ Accession", "→ Sample Received", "→ Order", etc.
- Mapping details: Source → Target
- Unit conversion info (if applicable): "10^9/L → 10^9/L"
- Qualitative mappings: "8 value mappings configured"
- Edit button: Opens edit mode

**Edit Mode - Create New Mapping** (from screenshots 1-2532/1-2533):

- **Title**: "Create New Mapping"
- **Subtitle**: "Map analyzer field to OpenELIS data element"

- **Source Field Section**:

  - Label: "Source Field (Analyzer)"
  - Shows selected field details:
    - Field name: "Antibiotic Susceptibility" (example)
    - ASTM Ref: "R|7|SUSC" (example)
    - Field type badge: "qualitative" (purple badge)
  - Read-only display (not editable)

- **Target Field Section**:

  - Label: "Target Field (OpenELIS)"
  - Dropdown/ComboBox: "Select OpenELIS field..."
  - Filter icon button next to dropdown (for advanced filtering)
  - Searchable dropdown with all available OpenELIS fields
  - Fields grouped by entity type

- **Action Buttons**:
  - "Create Mapping" button: Full-width, primary style, positioned at bottom
  - (In update mode: "Save Changes" + "Cancel" buttons)

**Empty State** (no field selected):

- Gray placeholder text
- "Select a field from the left panel to view or create mappings"

**Panel Background**:

- Light gray background (--cds-layer-01 or --cds-layer-02)
- Distinct from left panel for visual separation

### Layout Ratio

- **Left Panel**: 50% width (lg={8} columns)
- **Right Panel**: 50% width (lg={8} columns)
- **Total**: 8+8 = 16 columns (Carbon Grid)
- **Gap**: Standard Carbon grid gap between panels

## Visual Indicators

### Mapping Status Icons

- **Mapped**: Green checkmark + star icon (left of field name)
- **Unmapped**: No icon or gray indicator
- **Required**: Special indicator or badge

### Field Type Color Coding

- Numeric: Blue (#0f62fe - Carbon blue-60)
- Qualitative: Purple (#8a3ffc - Carbon purple-60)
- Control Test: Yellow/Gold (#f1c21b - Carbon yellow)
- Melting Point: Teal
- Date/Time: Cyan (#1192e8 - Carbon cyan-60)
- Text: Gray (#6f6f6f - Carbon gray-60)

## Implementation Notes

- Page MUST be full-page view (remove FieldMappingsPage modal wrapper)
- Use PageTitle component for hierarchical navigation
- Warning banner conditional on missing required mappings
- Stats cards calculate from mappings array
- Dual panel maintains 50/50 split on desktop
- Mobile: Stack panels vertically
- All icons from @carbon/icons-react
- No hardcoded colors (use Carbon tokens only)
