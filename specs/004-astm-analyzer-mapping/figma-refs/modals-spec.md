# Modals Design Specifications

**Figma File**: LKzqNAGc3MMQlJTF4JaPBC  
**Nodes**: 1-410 (Test Connection), 1-1031 (Edit Analyzer), 1-1488 (Add
Analyzer), 1-1489 (Delete Confirmation)  
**Date**: 2025-11-19

## Test Connection Modal (Node 1-410)

### Modal Structure

- **Component**: ComposedModal
- **Size**: Medium (default Carbon modal size)
- **Title**: "Test Connection"
- **Subtitle**: "Testing connection to {Analyzer Name}"

### Content Sections

1. **Analyzer Details** (read-only):

   - Analyzer: {name}
   - Type: {type}
   - Connection: {ipAddress}:{port}
   - Protocol: {protocolVersion}

2. **Connection Status**:

   - Error state: Red InlineNotification with error icon
   - Success state: Green checkmark with success message
   - Testing state: Loading spinner with progress message

3. **Connection Logs** (collapsible):
   - Title: "Connection Logs ({count})"
   - Dark terminal-style log viewer
   - Scrollable content area
   - Timestamps and log messages in monospace font
   - Copy/Download buttons at bottom

### Action Buttons

- **Close**: Secondary button (left)
- **Test Again**: Primary button (right)

## Edit Analyzer Modal (Node 1-1031)

### Modal Structure

- **Component**: ComposedModal
- **Size**: Medium
- **Title**: "Edit Analyzer"
- **Subtitle**: "Configure analyzer connection settings and test units"

### Form Fields

1. **Analyzer Name** \* (required)

   - TextInput
   - Pre-filled with existing name

2. **Analyzer Type** \* (required)

   - Dropdown
   - Options: Hematology, Chemistry, Immunology, Microbiology, Other
   - Pre-selected with current type

3. **IP Address** \* (required)

   - TextInput
   - Pre-filled with existing IP
   - Validation: IPv4 format

4. **Port** \* (required)

   - TextInput
   - Pre-filled with existing port
   - Validation: 1-65535

5. **Protocol Version**

   - TextInput (read-only or dropdown)
   - Default: "ASTM LIS2-A2"

6. **Test Units** \* (required)

   - MultiSelect or TagInput
   - Shows selected units as tags
   - Helper text: "Select one or more test units for this analyzer"

7. **Active Status**
   - Toggle switch
   - Label: "Enable this analyzer for data collection"
   - Default: Current status

### Action Buttons

- **Cancel**: Ghost/secondary button (left)
- **Save Changes**: Primary button (right)

**Note**: No "Test Connection" button in this modal - moved to action dropdown

## Add Analyzer Modal (Node 1-1488)

### Modal Structure

- **Component**: ComposedModal
- **Size**: Medium
- **Title**: "Add New Analyzer"
- **Subtitle**: "Configure analyzer connection settings and test units"

### Form Fields

Same as Edit Analyzer Modal, but:

- All fields empty (no pre-filled values)
- IP Address placeholder: "192.168.1.10"
- Port placeholder: "5000"
- Protocol Version: "ASTM LIS2-A2" (default)

### Action Buttons

- **Cancel**: Ghost/secondary button (left)
- **Add Analyzer**: Primary button (right)

**Note**: No "Test Connection" button in this modal - moved to action dropdown

## Delete Analyzer Modal (Node 1-1489)

### Modal Structure

- **Component**: ComposedModal
- **Size**: Small/Medium
- **Title**: "Delete Analyzer"
- **Variant**: Danger/destructive styling

### Content

- **Message**: "Are you sure you want to delete '{Analyzer Name}'? This action
  cannot be undone and will remove all associated field mappings."
- Warning icon (optional)
- Message emphasizes data loss

### Action Buttons

- **Cancel**: Secondary button (left, safe action)
- **Delete**: Danger/destructive button (right, red background)
  - Text: "Delete" (not "Confirm" or "OK")
  - Uses Carbon danger button variant

## Action Dropdown Menu (from Node 0-1)

### Menu Items (in order)

1. **Field Mappings** (with icon)

   - Navigates to `/analyzers/{id}/mappings`

2. **Test Connection** (with icon)

   - Opens Test Connection modal
   - **NEW**: Moved from inline form button

3. **Copy Mappings** (with icon)

   - Opens copy mappings modal

4. **Edit** (with icon)

   - Opens Edit Analyzer modal

5. **Delete** (with icon, red text)
   - Opens Delete Analyzer confirmation modal
   - Destructive action styling

## Implementation Notes

- All modals use Carbon ComposedModal component
- Test Connection accessible ONLY from action dropdown (not in form)
- Delete modal uses danger variant for destructive action
- All modals have proper close button (X in top right)
- Form validation shown inline with error messages
- Required fields marked with asterisk (\*)
