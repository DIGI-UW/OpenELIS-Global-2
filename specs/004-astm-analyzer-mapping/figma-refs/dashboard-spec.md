# Dashboard Design Specifications

**Figma File**: LKzqNAGc3MMQlJTF4JaPBC  
**Nodes**: 0-1 (Main Dashboard), 1-410 (Test Connection Modal)  
**Date**: 2025-11-19

## Analyzers Dashboard (Node 0-1)

### Page Title

- Text: "Analyzers"
- Subtitle: "Manage laboratory analyzers and field mappings"
- Font: Carbon heading-04
- No breadcrumb navigation on main dashboard

### Add Analyzer Button

- **Icon**: "+" (Add icon from @carbon/icons-react)
- **Style**: `kind="primary"` (black background in Figma)
- **Position**: Top right of page header
- **Text**: "Add Analyzer"
- **Component**: `<Button renderIcon={Add} kind="primary">`

### Statistics Tiles

- **Layout**: 3-column grid on desktop
  - Column 1 (lg={5}): "Total Analyzers" - Value: 4, Icon: Activity monitor
  - Column 2 (lg={6}): "Active" - Value: 3, Icon: Checkmark filled (green)
  - Column 3 (lg={5}): "Inactive" - Value: 1, Icon: Close filled (gray)
- **Styling**:
  - Background: `var(--cds-layer-01)` (white/light gray depending on theme)
  - Padding: Standard Carbon Tile padding
  - Icon position: Top right of tile
  - Value: Large bold number (2rem, font-weight 600)
  - Label: Small secondary text (0.875rem, --cds-text-secondary)
- **Grid**: Full width, equal spacing between tiles

### Search and Filters

- Search input: Full width in first row
- Filter dropdowns: "All Status", "All Types" in second row
- Uses Carbon Search and Dropdown components

### Data Table

- Columns: Name, Type, Connection, Test Units, Status, Last Modified, Actions
- Status: Tag component (green for Active, gray for Inactive)
- Actions: OverflowMenu with dropdown items

### Action Dropdown (from overflow menu visible in screenshot)

- Items:
  1. "Field Mappings" (with icon)
  2. "Test Connection" (with icon)
  3. "Copy Mappings" (with icon)
  4. "Edit" (with icon)
  5. "Delete" (with icon, red text for destructive action)
- Menu appears on click of overflow icon (three dots)
- Position: Aligned to right of clicked button

## Test Connection Modal (Node 1-410)

### Modal Structure

- **Component**: ComposedModal
- **Size**: Medium (default)
- **Title**: "Test Connection"
- **Subtitle**: "Testing connection to {Analyzer Name}"

### Content Sections

1. **Analyzer Details** (read-only fields):

   - Analyzer name
   - Type
   - Connection (IP:Port)
   - Protocol

2. **Connection Status**:

   - Error state: Red inline notification with error message
   - Success state: Green checkmark with success message
   - Testing state: Loading indicator with progress message

3. **Connection Logs**:
   - Collapsible accordion section
   - Title: "Connection Logs (25)" (count in parentheses)
   - Content: Scrollable log viewer with dark background
   - Timestamps and log messages in monospace font

### Action Buttons

- **Close**: Secondary button (left)
- **Test Again**: Primary button (right)
- Both buttons enabled at all times

## Color Tokens Used

- Tile background: `--cds-layer-01`
- Text primary: `--cds-text-primary`
- Text secondary: `--cds-text-secondary`
- Success green: `--cds-support-success`
- Error red: `--cds-support-error`
- Warning yellow: `--cds-support-warning`

## Spacing

- Tiles: `margin-bottom: 1rem` between rows
- Grid columns: `padding: 0 0.5rem` for spacing
- Page padding: `1rem` standard

## Implementation Notes

- Add icon must be imported from `@carbon/icons-react`
- Tiles should use 3-column layout for dashboard (lg={5}, lg={6}, lg={5} = 16
  total)
- Test Connection modal should be accessible from action dropdown, not inline in
  form
- All colors use Carbon design tokens (no hardcoded hex values)
