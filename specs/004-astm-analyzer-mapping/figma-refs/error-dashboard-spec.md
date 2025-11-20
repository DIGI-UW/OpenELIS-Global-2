# Error Dashboard Design Specifications

**Figma File**: LKzqNAGc3MMQlJTF4JaPBC  
**Nodes**: 2-4800 (Error Dashboard), 2-5221 (Action Dropdown)  
**Date**: 2025-11-19

## Error Dashboard Page (Node 2-4800)

### Page Title

- Text: "Error Dashboard"
- Font: Carbon heading-04
- Position: Top left of content area
- No breadcrumb navigation

### Acknowledge All Button

- **Style**: `kind="primary"` with icon
- **Icon**: Checkmark icon from @carbon/icons-react
- **Position**: Top right of page header
- **Text**: "Acknowledge All"
- **Component**: `<Button renderIcon={Checkmark} kind="primary">`

### Statistics Tiles

- **Layout**: 4-column grid (equal width)
  - Column 1 (lg={4}): "Total Errors" - Value: 5
  - Column 2 (lg={4}): "Unacknowledged" - Value: 3 (red text)
  - Column 3 (lg={4}): "Critical" - Value: 1 (red text)
  - Column 4 (lg={4}): "Last 24 Hours" - Value: 0
- **Grid Total**: 4+4+4+4 = 16 columns (full width)
- **Styling**:
  - Background: `var(--cds-layer-01)`
  - Padding: Standard Carbon Tile padding
  - Value: Large bold number
  - Label: Small secondary text above value
  - Critical values shown in red for emphasis

### Filter Bar

- **Layout**: Horizontal row with 4 inputs
  - Search input (left, takes ~30% width)
  - Error Type dropdown: "All Types"
  - Severity dropdown: "All Severities"
  - Analyzer dropdown: "All"
- **Components**: Carbon Search + 3 Dropdown components
- **Spacing**: Even spacing between filter controls

### Data Table

#### Columns

1. **Timestamp**: Date/time format (MM/DD/YYYY, HH:MM:SS AM/PM)
2. **Analyzer**: Analyzer name text
3. **Type**: Badge component with error type
4. **Severity**: Badge component with severity level
5. **Message**: Truncated error message text
6. **Status**: Badge component with acknowledgment status
7. **Actions**: OverflowMenu (three dots icon)

#### Badge Colors

- **Error Type** (column 3):

  - connection: blue badge
  - mapping: blue badge
  - validation: blue badge
  - timeout: blue badge
  - protocol: blue badge

- **Severity** (column 4):

  - critical: red badge
  - error: magenta/pink badge
  - warning: orange/yellow badge

- **Status** (column 6):
  - Unacknowledged: red badge with warning icon
  - Acknowledged: green badge with checkmark icon

### Action Dropdown (Node 2-5221)

From metadata inspection, the dropdown menu contains:

- **Item 1**: "Acknowledge"
- **Item 2**: "View Details"

**Menu Structure**:

- Appears on click of OverflowMenu button (three dots in Actions column)
- Menu positioned below/aligned to button
- Items use standard Carbon OverflowMenuItem styling

### Error Details Modal (visible in screenshot)

- **Title**: "Error Details"
- **Subtitle**: "Detailed information and analyzer logs for error {id}"
- **Sections**:
  1. Error metadata (Error ID, Timestamp, Analyzer, Analyzer ID, Error Type,
     Severity)
  2. Error Message (highlighted in yellow background)
  3. Acknowledged status (green checkmark with user/timestamp if acknowledged)
  4. Analyzer Logs (collapsible accordion with dark terminal-style viewer)
  5. Recommended Actions (bulleted list)
  6. Action buttons: Copy, Download (for logs)
- **Close Button**: Secondary button at bottom

## Color Tokens

- Tile background: `--cds-layer-01`
- Critical/Error red: `--cds-support-error`
- Warning orange: `--cds-support-warning`
- Success green: `--cds-support-success`
- Info blue: `--cds-support-info`

## Implementation Notes

- Tiles MUST use lg={4} for each column (4 equal-width tiles)
- Action dropdown follows standard Carbon OverflowMenu pattern
- Badge colors must use Carbon type prop (red, magenta, blue, green, etc.)
- Error Details modal should be in separate component (ErrorDetailsModal)
