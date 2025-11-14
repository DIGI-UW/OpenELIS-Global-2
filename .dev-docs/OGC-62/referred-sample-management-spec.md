# Referred Sample Container Management System
## Comprehensive Functional Specification

**Version:** 1.0  
**Date:** November 7, 2025  
**System:** OpenELIS Global  
**UI Framework:** Carbon Design System (React)

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [User Roles & Permissions](#user-roles--permissions)
3. [System States & Workflow](#system-states--workflow)
4. [User Stories](#user-stories)
   - 4.1 [Box Creation & Management](#41-box-creation--management)
   - 4.2 [Labeling & Documentation](#42-labeling--documentation)
   - 4.3 [Sending Workflow](#43-sending-workflow)
   - 4.4 [Receiving Workflow](#44-receiving-workflow)
   - 4.5 [Dashboard & Reporting](#45-dashboard--reporting)
   - 4.6 [Administration](#46-administration)
5. [Business Rules](#business-rules)
   - 5.1 [Box Management Rules](#51-box-management-rules)
   - 5.2 [Sample Management Rules](#52-sample-management-rules)
   - 5.3 [Manifest & Documentation Rules](#53-manifest--documentation-rules)
   - 5.4 [Receiving & Reconciliation Rules](#54-receiving--reconciliation-rules)
   - 5.5 [Security & Audit Rules](#55-security--audit-rules)
   - 5.6 [Integration & Interoperability Rules](#56-integration--interoperability-rules)
   - 5.7 [Unassigned Tests Tracking Rules](#57-unassigned-tests-tracking-rules)
6. [Functional Requirements](#functional-requirements)
   - 6.1 [Box Management Functions](#61-box-management-functions)
   - 6.2 [Sample Management Functions](#62-sample-management-functions)
   - 6.3 [Label & Manifest Functions](#63-label--manifest-functions)
   - 6.4 [Receiving Functions](#64-receiving-functions)
   - 6.5 [Dashboard & Reporting Functions](#65-dashboard--reporting-functions)
   - 6.5A [Unassigned Tests Tracking Functions](#65a-unassigned-tests-tracking-functions)
   - 6.6 [Administration Functions](#66-administration-functions)
   - 6.7 [Integration Functions](#67-integration-functions)
   - 6.8 [Audit & Compliance Functions](#68-audit--compliance-functions)
7. [UI Specifications](#ui-specifications)
8. [Integration Requirements](#integration-requirements)
9. [Non-Functional Requirements](#non-functional-requirements)
10. [Data Model](#data-model)

---

## 1. Executive Summary

The Referred Sample Container Management System enables laboratory staff to build, track, and manage boxes of samples sent to reference laboratories. The system provides end-to-end tracking from box creation through receiving and reconciliation, with full audit trails and electronic manifest exchange capabilities.

### Key Capabilities
- Box creation and sample manifest management
- **Unassigned tests tracking - ensures all referred samples are accounted for**
- Barcode scanning integration for samples and boxes
- Configurable label printing (box labels and packing lists)
- Electronic manifest exchange via FHIR and API
- Receipt workflow with discrepancy tracking
- Comprehensive dashboard and reporting
- Non-conformity recording with optional documentation

### Unassigned Tests Feature
A critical accountability feature that tracks all samples marked for referral but not yet assigned to a shipment. This prevents samples from being forgotten or lost in the referral process by:
- Displaying all unassigned referred samples in a dedicated dashboard tab
- Highlighting samples based on how long they've been unassigned (7-day and 30-day thresholds)
- Providing quick actions to add samples to boxes, mark as lost, or cancel referrals
- Supporting bulk operations for efficient management
- Optional alerting for aging unassigned samples

---

## 2. User Roles & Permissions

### 2.1 Shipping Role (New Global Role)
**Permission Name:** `ROLE_SHIPPING`

**Capabilities:**
- Create and manage boxes
- Add/remove samples from boxes
- Mark boxes as ready to send
- Generate and print labels and manifests
- Send boxes
- Receive boxes
- Record non-conformities
- View reports

**Access Restriction:**
- Can edit boxes in Draft, Ready to Send, and In Transit states
- Cannot edit boxes after they are marked as Sent (except Admin)

### 2.2 Administrator Role
**Additional Capabilities:**
- Configure label prefixes and formats
- Manage facility registry (reference lab destinations)
- Define label dimensions and barcode types
- Edit boxes in any state (including after sent)
- Configure business rules
- Manage FHIR mapping options

### 2.3 Lab Technician (Receiving)
**Note:** Uses Shipping role permissions for receiving workflow

---

## 3. System States & Workflow

### 3.1 Box States

| State | Description | Allowed Transitions | User Actions Available |
|-------|-------------|---------------------|------------------------|
| **Draft** | Box created, samples being added | → Ready to Send<br>→ Cancelled | Add/remove samples, edit metadata, delete box |
| **Ready to Send** | All required fields complete, ready for shipment | → Sent<br>→ Draft<br>→ Cancelled | Print labels, send box, edit samples |
| **Sent** | Box dispatched to reference lab | → In Transit<br>→ Cancelled | View only, resend manifest |
| **In Transit** | Box confirmed en route | → Partially Received<br>→ Received<br>→ Lost in Transit | View only, resend manifest |
| **Partially Received** | Some samples confirmed at destination | → Received<br>→ Lost in Transit | Continue receiving workflow |
| **Received** | All expected samples confirmed | → Closed/Archived | View, generate reports |
| **Closed/Archived** | Box reconciled and closed | None (terminal state) | View only, reports |
| **Cancelled/Abandoned** | Box not sent or shipment cancelled | None (terminal state) | View only |
| **Lost in Transit** | Box not received, marked as lost | None (terminal state) | View only, incident reports |

### 3.2 Sample States (within a Box)

| State | Description | Visual Indicator |
|-------|-------------|------------------|
| **Pending** | Added to manifest, not yet sent | Gray dot |
| **Sent** | Included in sent box | Blue dot |
| **Received** | Confirmed at destination via scan | Green checkmark |
| **Missing** | Expected but not received | Yellow warning |
| **Damaged** | Received but damaged | Red exclamation |
| **Rejected** | Not acceptable for testing | Red X |

### 3.3 Core Workflow Diagram

```
┌─────────────┐
│   Create    │
│     Box     │
└──────┬──────┘
       │
       ▼
┌─────────────┐     Add samples via:
│    Draft    │◄─── • Barcode scan
└──────┬──────┘     • Manual lookup
       │            • Batch import
       │
       ▼ (Checkbox: Mark as Ready to Send + validation)
┌─────────────┐
│  Ready to   │     • Print box label
│    Send     │     • Print packing list
└──────┬──────┘     • Generate manifest
       │
       ▼ (Warning modal confirmation)
┌─────────────┐
│    Sent     │──→ Electronic manifest sent
└──────┬──────┘     (FHIR/API/Email)
       │
       ▼
┌─────────────┐
│ In Transit  │
└──────┬──────┘
       │
       ▼ (Scan box ID at receiving lab)
┌─────────────┐
│  Receiving  │     Scan each sample to:
│   Workflow  │     • Mark as received
└──────┬──────┘     • Record non-conformities
       │
       ▼
┌─────────────┐
│  Received/  │
│   Closed    │
└─────────────┘
```

---

## 4. User Stories

### 4.1 Box Creation & Management

#### US-001: Create a New Box
**As a** shipping coordinator  
**I want to** create a new box with a unique identifier  
**So that** I can start building a manifest of samples to send to a reference lab

**Acceptance Criteria:**
- System auto-generates Box ID based on configured prefix (or accepts scanned existing Box ID)
- User must select destination from facility registry dropdown
- Box is saved in "Draft" state
- Box creation timestamp and creator user ID are recorded
- User can add optional notes/comments to the box

**UI Notes:**
- "Create New Box" button prominently displayed on dashboard
- Modal or full-page form for box creation
- Required fields marked with asterisk

---

#### US-002: Add Samples to Box via Barcode Scan
**As a** shipping coordinator  
**I want to** scan sample barcodes to add them to a box  
**So that** I can quickly build the manifest without manual typing

**Acceptance Criteria:**
- USB barcode scanner input accepted (keyboard wedge)
- Each scanned sample appears in manifest table with status "Pending"
- System validates sample exists in OpenELIS
- Green checkmark displayed on successful scan
- Red X displayed on failed scan with error message
- Duplicate samples rejected with error: "Sample already in this box"
- Sample already in another active box rejected with error: "Sample [ID] is already in box [Box ID]"
- System prompts user to try again after failed scan

**UI Notes:**
- Dedicated scan input field with focus indicator
- Real-time feedback (green/red visual indicators)
- Audio beep option for scan feedback
- Sample added to table immediately upon successful scan

---

#### US-003: Lookup and Add Samples Manually
**As a** shipping coordinator  
**I want to** search for and add samples manually  
**So that** I can include samples when barcode scanning is not available

**Acceptance Criteria:**
- Search by accession number or sample ID
- Auto-complete suggestions as user types
- Sample details displayed before adding (accession #, test type, collection date)
- User can confirm addition from search results
- Same validation rules as barcode scan apply

**UI Notes:**
- Search field with Carbon ComboBox component
- Sample preview card before adding
- "Add to Box" button in preview

---

#### US-004: Add Non-Referral Sample with Reason
**As a** shipping coordinator  
**I want to** add a sample that wasn't already marked for referral  
**So that** I can refer additional samples as needed with proper documentation

**Acceptance Criteria:**
- When adding a non-referral sample, system displays additional step
- User must select/enter reason for referral (dropdown + free text)
- System automatically marks sample with referral flag
- Referral destination set to box destination
- Referral reason captured in sample metadata
- Sample now shows as referred in order entry system

**UI Notes:**
- Modal dialog appears after sample selection
- "Reason for Referral" required field
- Link to view/edit order details if needed

---

#### US-005: View Sample Details in Manifest
**As a** shipping coordinator  
**I want to** see key details for each sample in the box  
**So that** I can verify I'm sending the correct samples

**Acceptance Criteria:**
- Table displays: Accession #, Test Type, Collection Date, Status
- Temperature/storage requirements shown if specified in order
- Sample disposal requirements shown if applicable
- Row highlighting on hover
- Ability to expand row for additional details

**UI Notes:**
- Carbon DataTable component
- Expandable rows for additional metadata
- Icons for temperature requirements (frozen ❄️, refrigerated 🧊, room temp 🌡️)

---

#### US-006: Remove Sample from Box
**As a** shipping coordinator  
**I want to** remove a sample from a box at any time  
**So that** I can correct mistakes or respond to changed requirements

**Acceptance Criteria:**
- Samples can be removed from boxes in Draft, Ready to Send, or In Transit states
- Confirmation dialog before removal: "Remove [Sample ID] from box?"
- Removal logged in audit trail with user ID and timestamp
- Removed sample is unlinked from box (referral flag remains if it was set)
- If box is in Ready to Send state, it reverts to Draft after removal

**UI Notes:**
- Remove icon (trash can) in each table row
- Confirmation modal with sample details
- Undo option for accidental removal (5-second toast notification)

---

#### US-007: Mark Box as Ready to Send
**As a** shipping coordinator  
**I want to** mark a box as ready to send  
**So that** it can go through final validation before shipment

**Acceptance Criteria:**
- Checkbox displayed on manifest screen: "Mark as Ready to Send"
- System validates all required fields:
  - Box has at least 1 sample
  - Destination is selected
  - All samples have required metadata (test type, collection date)
  - Temperature requirements are documented for all samples
- If validation fails, display error messages for each issue
- If validation passes, box state changes to "Ready to Send"
- "Send Box" button becomes enabled

**UI Notes:**
- Checkbox at top of manifest table
- Validation messages displayed inline with specific errors
- Success notification when ready state achieved

---

### 4.2 Labeling & Documentation

#### US-008: Configure Box Label Format (Admin)
**As an** administrator  
**I want to** configure the box label format and specifications  
**So that** labels meet our operational and regulatory requirements

**Acceptance Criteria:**
- Admin can define label prefix (e.g., "REF-BOX-")
- Admin can select barcode type (Code 39, Code 128, QR Code, Data Matrix)
- Admin can specify label dimensions (width x height in mm or inches)
- Admin can customize label template with fields:
  - Box ID (barcode)
  - Destination
  - Number of samples
  - Sender
  - Date
  - Temperature requirements
  - Custom logo/header
- Preview of label shown before saving
- Multiple label templates can be created for different destinations

**UI Notes:**
- Admin panel section: "Label Configuration"
- Visual label designer with drag-and-drop fields
- Template selector dropdown
- Live preview pane

---

#### US-009: Print Box Label
**As a** shipping coordinator  
**I want to** print a label for the box  
**So that** it can be physically identified and tracked

**Acceptance Criteria:**
- "Print Label" button available when box is in Ready to Send or later state
- Label generated using configured template
- Box ID rendered as scannable barcode
- All configured fields populated from box metadata
- Print dialog allows selection of printer and number of copies
- Label generation logged in audit trail

**UI Notes:**
- Print button in box details view
- Print preview modal before printing
- "Print Another Copy" option

---

#### US-010: Generate and Print Packing List/Manifest
**As a** shipping coordinator  
**I want to** generate a packing list with all samples  
**So that** the reference lab knows what to expect

**Acceptance Criteria:**
- "Generate Packing List" button available when box is Ready to Send or later
- Packing list includes:
  - Box ID and barcode
  - Destination details
  - Sender details
  - Date and time generated
  - Total sample count
  - Table of all samples with: Accession #, Patient ID, Test Type, Collection Date, Special Requirements
  - Barcode for each sample (optional toggle)
- Summary sheet includes barcode for entire shipment
- PDF format for electronic storage and email
- Printable format for physical copy
- Optional: Individual sample labels with barcodes (checked by user)

**UI Notes:**
- "Generate Manifest" button with dropdown:
  - "Packing List Only"
  - "Packing List + Sample Labels"
  - "View Previous Manifest"
- PDF preview before printing
- "Email Copy" button

---

#### US-011: Regenerate or Resend Manifest
**As a** shipping coordinator  
**I want to** regenerate or resend a packing list  
**So that** I can provide updated information or resend lost documentation

**Acceptance Criteria:**
- Regeneration allowed within 24 hours of sending
- After 24 hours, recall option available for 7 days (requires admin approval)
- "Resend Manifest" button sends electronic copy without regenerating
- Regeneration creates new version number
- All versions retained in audit trail
- Electronic resend options:
  - FHIR message
  - API call (for OpenELIS instances)
  - Email with PDF attachment

**UI Notes:**
- "Resend Manifest" button in box details
- Version history dropdown
- Confirmation modal with resend options

---

### 4.3 Sending Workflow

#### US-012: Send Box with Confirmation
**As a** shipping coordinator  
**I want to** mark a box as sent with mandatory confirmation  
**So that** I don't accidentally send boxes prematurely

**Acceptance Criteria:**
- "Send Box" button only enabled when box is in Ready to Send state
- Warning modal displayed: "Are you sure you want to send this box? This action will lock the packing list."
- Modal shows summary: Box ID, Destination, # Samples, Date/Time
- User must explicitly click "Confirm Send"
- Box state changes to "Sent"
- Packing list locked (cannot edit samples)
- Electronic manifest automatically sent based on destination configuration
- Send timestamp and user recorded

**UI Notes:**
- Warning modal with amber/yellow theme
- "Cancel" and "Confirm Send" buttons
- Summary of box contents in modal
- Option to print label again before sending

---

#### US-013: Electronic Manifest Exchange
**As a** shipping coordinator  
**I want to** automatically send electronic manifests  
**So that** the reference lab receives advance notification

**Acceptance Criteria:**
- When box is marked as Sent, system checks destination configuration
- If FHIR enabled: Send SupplyDelivery resource with status "in-progress"
- If API enabled: POST manifest JSON to configured endpoint
- If email enabled: Send confirmation email with PDF attachment to configured recipients
- System retries failed transmissions (3 attempts with exponential backoff)
- Transmission log shows status (success/failed) with timestamp
- User can manually retry transmission

**UI Notes:**
- Transmission status indicator in box details
- "View Transmission Log" expandable section
- "Resend" button for failed transmissions

---

### 4.4 Receiving Workflow

#### US-014: Scan Box at Receiving Location
**As a** receiving technician  
**I want to** scan a box ID when it arrives  
**So that** I can start the receiving process

**Acceptance Criteria:**
- Receiving screen with "Scan Box ID" input field
- When box scanned, system displays:
  - Box details (ID, origin, sent date)
  - Expected sample list
  - Receiving checklist
- Box state transitions from "In Transit" to "Partially Received"
- If box ID not found, error message: "Box not found or not yet sent"
- If box already fully received, warning: "Box already received on [Date]"

**UI Notes:**
- Prominent scan input at top of receiving screen
- Box details card displayed after successful scan
- Sample checklist in table format below

---

#### US-015: Scan Samples During Receipt
**As a** receiving technician  
**I want to** scan each sample as I remove it from the box  
**So that** I can confirm receipt and identify discrepancies

**Acceptance Criteria:**
- "Scan Sample" input field in receiving screen
- As each sample scanned:
  - Corresponding row in checklist gets green checkmark
  - Sample status changes to "Received"
  - Visual and audio feedback (beep) on successful scan
  - Sample row highlighted in green
- If scanned sample not in manifest:
  - Red X displayed
  - Error message: "Sample [ID] not in this manifest"
  - Option to "Add as unexpected sample" or "Ignore"
- Running count displayed: "X of Y samples received"
- Manual checkbox available if barcode cannot be scanned

**UI Notes:**
- Scan input field with autofocus
- Live progress indicator (e.g., "5 of 12 samples received")
- Filter: "Show only unreceived samples"
- Color-coded rows (gray=pending, green=received, yellow=issue)

---

#### US-016: Manual Check-off for Damaged Barcodes
**As a** receiving technician  
**I want to** manually check off samples when barcodes are unreadable  
**So that** I can still complete the receiving process

**Acceptance Criteria:**
- Each sample row has checkbox for manual confirmation
- Checking box marks sample as "Received"
- System logs that sample was manually confirmed (not scanned)
- Manual confirmation requires additional confirmation dialog
- Audit trail distinguishes between scanned and manual receipt

**UI Notes:**
- Checkbox in each row
- Confirmation dialog: "Manually confirm receipt of [Sample ID]?"
- Visual indicator (icon) showing scan vs. manual receipt method

---

#### US-017: Record Non-Conformities
**As a** receiving technician  
**I want to** record non-conformities for samples  
**So that** quality issues are documented

**Acceptance Criteria:**
- "Report Issue" button/icon in each sample row
- Non-conformity types available (from OpenELIS order entry system):
  - Damaged container
  - Insufficient volume
  - Mislabeled
  - Leaked
  - Wrong sample type
  - Temperature deviation
  - Hemolyzed
  - Clotted
  - Other (with free text)
- User can select multiple non-conformity types
- Optional free text notes field
- Optional photo/document upload (JPG, PNG, PDF)
- Non-conformity flag applied to sample (existing flag in OpenELIS)
- Sample marked as "optionally available for testing" (can still be tested but with flag)
- "Apply to All Samples" checkbox for batch issues (e.g., temperature deviation)

**UI Notes:**
- Modal dialog for non-conformity recording
- Multi-select checkboxes for types
- File upload component with preview
- Warning icon (⚠️) next to samples with non-conformities
- Non-conformity summary in box details

---

#### US-018: Complete Box Receipt
**As a** receiving technician  
**I want to** complete and close the receiving process  
**So that** the box is fully reconciled

**Acceptance Criteria:**
- "Complete Receipt" button enabled when all expected samples accounted for
- System checks:
  - All expected samples marked as Received, Missing, Damaged, or Rejected
  - Any non-conformities have been documented
- Confirmation dialog shows summary:
  - Samples received: X
  - Samples missing: Y
  - Samples with issues: Z
- User must confirm completion
- Box state changes to "Received"
- If all samples received without issues, state changes to "Closed/Archived"
- Receipt timestamp and user recorded
- FHIR SupplyDelivery status updated to "completed"

**UI Notes:**
- "Complete Receipt" button at bottom of checklist
- Summary modal with statistics
- Option to print receiving report

---

#### US-019: Handle Missing Samples
**As a** receiving technician  
**I want to** mark samples as missing  
**So that** discrepancies are tracked

**Acceptance Criteria:**
- "Mark as Missing" button for unreceived samples
- Missing samples remain in "Missing" state
- Missing count displayed in box summary
- User can add notes explaining missing sample
- Box can be completed with missing samples
- Missing samples can later be marked as received if they arrive late

**UI Notes:**
- "Missing" status with yellow warning icon
- Notes field for each missing sample
- Filter to show only missing samples

---

#### US-020: Handle Unexpected Samples
**As a** receiving technician  
**I want to** document unexpected samples in the box  
**So that** they can be followed up outside this system

**Acceptance Criteria:**
- When scanning unexpected sample, option to "Add as Unexpected"
- Unexpected sample added to receiving log (not to manifest)
- Note field required: "Why is this sample unexpected?"
- Unexpected samples logged but not processed in this workflow
- Receiving report includes unexpected samples section
- Follow-up handled outside this feature (entered as new samples in OpenELIS later)

**UI Notes:**
- Unexpected samples shown in separate section
- Orange/amber color coding
- "Requires Follow-up" flag

---

### 4.5 Dashboard & Reporting

#### US-021: View Dashboard with Metrics
**As a** shipping coordinator  
**I want to** see key metrics about boxes  
**So that** I can understand the current state of shipments

**Acceptance Criteria:**
- Dashboard displays metric cards:
  1. "Boxes Ready to Send" - count of boxes in Ready to Send state
  2. "Boxes In Transit" - count of boxes in Sent or In Transit states
  3. "Awaiting Receipt Confirmation" - count of boxes in Partially Received state
  4. "Received This Week" - count of boxes received in current week (with date range shown)
- Cards clickable to filter main table
- Cards show trend indicators if applicable (e.g., "+2 since yesterday")
- Cards styled similar to Pathology dashboard (screenshot reference)

**UI Notes:**
- Carbon Card components (4 cards in row)
- Large number display in each card
- Subtle background colors per state
- Date range shown for "Received This Week" card (e.g., "Week 31/10/2025 - 07/11/2025")

---

#### US-021A: View Unassigned Referred Tests
**As a** shipping coordinator  
**I want to** view all samples marked as referred but not yet assigned to a shipment  
**So that** I can ensure no referred samples are forgotten or lost

**Acceptance Criteria:**
- Dashboard has tab navigation: "Shipments" | "Unassigned Tests"
- "Unassigned Tests" tab displays all samples where:
  - Sample has referral flag set to true
  - Sample is not in any active box (Draft, Ready to Send, Sent, In Transit, Partially Received)
  - Sample is not marked as Lost or Referral Cancelled
- Table displays columns:
  - Accession Number
  - Patient Name/ID
  - Test Type
  - Collection Date
  - Referral Date
  - Referral Destination
  - Referral Reason
  - Days Unassigned (calculated from referral date)
- Search functionality across all columns
- Filter options:
  - Date range (referral date)
  - Destination
  - Test type
  - Days unassigned (0-7, 7-14, 14-30, >30)
- Sort capability on all columns
- Highlight rows >7 days unassigned in yellow
- Highlight rows >30 days unassigned in red
- Action menu for each sample: "Add to Box" | "Mark as Lost" | "Cancel Referral"

**UI Notes:**
- Carbon Tabs component for tab navigation
- Carbon DataTable with search and filters
- Warning badges for aging unassigned samples
- Bulk selection capability for adding multiple samples to a box

---

#### US-021B: Add Unassigned Sample to Box
**As a** shipping coordinator  
**I want to** add an unassigned sample directly to a box from the unassigned list  
**So that** I can quickly resolve unassigned samples

**Acceptance Criteria:**
- "Add to Box" action opens modal or side panel
- User can select existing Draft or Ready to Send box
- User can create new box and add sample
- After adding, sample is removed from unassigned list
- Action logged in audit trail
- Success notification displayed

**UI Notes:**
- Modal with box selection dropdown
- "Create New Box" option in dropdown
- Quick add without leaving unassigned view

---

#### US-021C: Mark Unassigned Sample as Lost
**As a** shipping coordinator  
**I want to** mark an unassigned sample as lost  
**So that** it no longer appears in the unassigned list and is properly tracked

**Acceptance Criteria:**
- "Mark as Lost" action requires confirmation dialog
- Confirmation shows sample details and warning message
- User must provide reason for marking as lost (required text field)
- Sample marked with "Lost" status in OpenELIS
- Sample removed from unassigned list
- Sample appears in "Lost Samples" report
- Referral flag remains set, but sample is no longer tracked for shipping
- Action logged in audit trail with reason
- Cannot be undone (except by administrator)

**UI Notes:**
- Warning modal with amber/red theme
- Required reason text field
- "This action cannot be undone" warning
- Confirmation requires typing "CONFIRM" or clicking acknowledge checkbox

---

#### US-021D: Cancel Referral for Unassigned Sample
**As a** shipping coordinator  
**I want to** cancel the referral for an unassigned sample  
**So that** samples no longer needed for referral are properly tracked

**Acceptance Criteria:**
- "Cancel Referral" action requires confirmation dialog
- Confirmation shows sample details
- User must provide reason for cancellation (required text field)
- Sample referral flag is removed in OpenELIS
- Sample removed from unassigned list
- Sample returns to normal order workflow in OpenELIS
- Action logged in audit trail with reason
- Can only be done by user with Shipping role or Admin role
- Cancellation date and user recorded

**UI Notes:**
- Confirmation modal
- Required reason text field
- Warning: "Sample will return to normal processing workflow"
- Success notification after cancellation

---

#### US-021E: Bulk Actions on Unassigned Samples
**As a** shipping coordinator  
**I want to** perform actions on multiple unassigned samples at once  
**So that** I can efficiently manage large numbers of unassigned samples

**Acceptance Criteria:**
- Checkbox column in unassigned tests table for multi-select
- "Select All" checkbox in header (with intelligent pagination handling)
- Bulk actions toolbar appears when samples selected
- Bulk actions available:
  - "Add Selected to Box" - opens box selection modal
  - "Export Selected" - exports to CSV/Excel
- Bulk action count displayed (e.g., "5 samples selected")
- Individual actions still available per row
- "Clear Selection" button in toolbar

**UI Notes:**
- Carbon DataTable batch actions pattern
- Toolbar slides in from top when selection active
- Action buttons disabled until samples selected

---

#### US-022: View and Filter Box List Table
**As a** shipping coordinator  
**I want to** view and filter the list of boxes  
**So that** I can find specific shipments

**Acceptance Criteria:**
- Table displays columns:
  - Box ID
  - Destination
  - Created Date
  - Status
  - # of Samples
  - Created By
  - Sent Date
  - Received Date
- Filters available:
  - Date range picker (created date)
  - Destination dropdown
  - Status multi-select
  - Search by Box ID
- Sort available on all columns
- Pagination (25, 50, 100 items per page)
- Status shown with color-coded badges
- Default sort: Most recent created date first

**UI Notes:**
- Carbon DataTable component
- Filter row above table
- Status badges with semantic colors (blue, yellow, green, red)
- Search input with debounce

---

#### US-023: Quick Actions in Table
**As a** shipping coordinator  
**I want to** perform quick actions from the table  
**So that** I don't have to navigate to detail pages

**Acceptance Criteria:**
- Actions column in table with overflow menu (⋮)
- Actions vary based on box state:
  - **Draft/Ready to Send:** View, Edit, Print Label, Delete
  - **Sent/In Transit:** View, Print Manifest, Resend Manifest
  - **Received/Closed:** View, Generate Report, Archive
- Actions disabled/hidden based on user permissions
- Action results shown via toast notifications

**UI Notes:**
- Carbon OverflowMenu component
- Icons for each action
- Disabled actions grayed out with tooltip explaining why

---

#### US-024: Generate Reports
**As a** user  
**I want to** generate reports on shipments  
**So that** I can track and analyze shipping activity

**Acceptance Criteria:**
- Report filters:
  - By Box ID (specific or multiple)
  - By Date Range (sent date or received date)
  - By Destination
  - By Reference Lab
  - By Status
  - By Created By user
- Report columns:
  - Box ID
  - Contents (sample count)
  - Sent Date/Time
  - Sender (user)
  - Received Status
  - Received Date/Time
  - Receiving User
  - Non-conformities Count
  - Missing Samples Count
- Export options:
  - PDF (formatted report)
  - Excel (.xlsx)
  - CSV
- Report can be scheduled (future enhancement noted)

**UI Notes:**
- Reports section in navigation
- Filter sidebar on left
- "Generate Report" button
- Export dropdown menu
- Report preview before export

---

### 4.6 Administration

#### US-025: Manage Label Prefixes
**As an** administrator  
**I want to** define and manage label prefixes  
**So that** boxes are consistently identified

**Acceptance Criteria:**
- Admin can create multiple prefix templates
- Each prefix has:
  - Name/Description
  - Prefix text (e.g., "REF-BOX-")
  - Counter start number
  - Counter format (e.g., 5 digits with leading zeros)
  - Active/Inactive status
- One prefix can be set as default
- Example preview shown (e.g., "REF-BOX-00001")
- Cannot delete prefix if boxes exist using it

**UI Notes:**
- Admin table with prefix list
- "Add Prefix" button
- Inline editing
- Preview column

---

#### US-026: Manage Facility Registry
**As an** administrator  
**I want to** manage reference lab destinations  
**So that** users can select the correct shipping destination

**Acceptance Criteria:**
- Admin can add/edit/deactivate facilities
- Each facility has:
  - Facility Name
  - Facility Code
  - Address
  - Contact Information
  - FHIR endpoint URL (optional)
  - API endpoint URL (optional)
  - Email addresses for notifications (comma-separated)
  - Default label template
  - Active/Inactive status
- Facilities can be grouped/categorized
- Cannot delete facility if boxes exist using it

**UI Notes:**
- Facility management table
- "Add Facility" modal
- Integration settings collapsible section
- Validation for URLs and email formats

---

#### US-027: Configure FHIR Mappings
**As an** administrator  
**I want to** configure FHIR resource mappings  
**So that** electronic manifests conform to standards

**Acceptance Criteria:**
- Admin can configure:
  - SupplyDelivery.status mappings for box states
  - Specimen.container.type codes for sample containers
  - SNOMED CT codes for non-conformity types
  - Organization identifiers for sender/receiver
- Mapping table with source value and target FHIR value
- Validation that FHIR codes are valid
- Test connection button for FHIR endpoints

**UI Notes:**
- FHIR Configuration section in admin
- Mapping table with inline editing
- "Test FHIR Connection" button
- Documentation links to FHIR specs

---

## 5. Business Rules

### 5.1 Box Management Rules

**BR-001: Unique Box ID**
- Each Box ID must be unique across the entire system
- Box IDs can be auto-generated using configured prefix + sequential number
- Box IDs can be manually scanned (e.g., from pre-printed labels)
- If scanned Box ID already exists, system rejects and prompts for different ID

**BR-002: Box State Transitions**
- Boxes must progress through states in logical order (see state diagram)
- Cannot skip required states (e.g., cannot go from Draft directly to Sent)
- State transitions are logged with timestamp and user ID
- Only administrators can reverse state transitions

**BR-003: Box Editing Restrictions**
- Draft boxes: Fully editable
- Ready to Send boxes: Can edit samples and metadata, but state resets to Draft if samples changed
- Sent/In Transit boxes: Cannot edit samples (manifest locked), can only edit notes
- After Sent: Only administrators can edit
- Received/Closed boxes: Read-only except for notes/documentation

**BR-004: Box Deletion**
- Only Draft boxes can be deleted
- Deletion requires confirmation
- Deleted boxes are soft-deleted (marked inactive, audit trail retained)
- Box IDs from deleted boxes cannot be reused

**BR-005: Minimum Box Requirements**
- Box must have at least 1 sample to be marked as Ready to Send
- Box must have destination selected
- Box must have all required metadata fields filled

---

### 5.2 Sample Management Rules

**BR-006: Sample Uniqueness**
- Each sample can belong to only one active box at a time
- Active boxes are those in Draft, Ready to Send, Sent, or In Transit states
- If sample is in Received/Closed box, it can be added to a new box (re-referral scenario)
- System checks sample status before allowing addition to box

**BR-007: Sample Eligibility**
- Samples already marked for referral in OpenELIS can be added directly
- Non-referral samples require additional step:
  - Reason for referral must be provided
  - System automatically applies referral flag to sample
  - Referral destination set to box destination
- System validates that sample exists in OpenELIS before adding

**BR-008: Sample Metadata Requirements**
- Each sample must have:
  - Accession Number (required)
  - Test Type (required)
  - Collection Date (required)
  - Temperature/Storage Requirements (if applicable from order)
- Missing required fields prevent box from being marked Ready to Send

**BR-009: Sample Removal**
- Samples can be removed from boxes at any time before receipt is completed
- Sample removal logs user ID, timestamp, and reason (optional)
- If sample had referral flag set by this box, flag is not removed (referral intention remains)
- Removing sample from Ready to Send box reverts box to Draft state

**BR-010: Duplicate Prevention**
- Scanning same sample twice in same box is rejected with error message
- System performs real-time duplicate check on scan/add

---

### 5.3 Manifest & Documentation Rules

**BR-011: Manifest Generation**
- Manifest can be generated at any point after box creation
- Final manifest is locked when box is marked as Sent
- Manifest includes:
  - Box metadata (ID, destination, sender, date)
  - Complete sample list with all metadata
  - Barcode for box and optionally for each sample
  - Temperature and handling requirements
  - Sender signature line (for printed copy)

**BR-012: Manifest Versioning**
- Each manifest regeneration creates a new version
- Version number increments (v1, v2, v3...)
- All versions retained in system
- Current version always displayed by default
- Users can view previous versions in history

**BR-013: Manifest Regeneration Time Limits**
- Regeneration allowed freely within 24 hours of sending
- After 24 hours up to 7 days: Recall option (requires admin approval)
- After 7 days: No regeneration (contact support for special cases)
- Reasoning: Prevents confusion with multiple manifests in circulation

**BR-014: Electronic Manifest Exchange**
- Electronic manifest sent automatically when box marked as Sent
- Transmission method based on destination configuration:
  - FHIR: SupplyDelivery resource with status "in-progress"
  - API: JSON POST to configured endpoint
  - Email: PDF attachment to configured recipients
- Multiple transmission methods can be active simultaneously
- Failed transmissions are retried automatically (3 attempts)
- Users can manually retry after automatic attempts fail

**BR-015: Label Printing**
- Box labels can be printed multiple times (reprints allowed)
- Each print logged in audit trail
- Label format determined by:
  1. Destination-specific template (if configured)
  2. Default template (if no destination-specific template)
- Label must include barcode that matches Box ID exactly

---

### 5.4 Receiving & Reconciliation Rules

**BR-016: Receiving Initiation**
- Box must be in Sent or In Transit state to start receiving
- Scanning box ID initiates receiving workflow
- Box transitions to Partially Received state when first sample is confirmed
- Receiving session can be paused and resumed

**BR-017: Sample Receipt Validation**
- Each scanned sample is checked against manifest
- If sample not in manifest: "Unexpected sample" option available
- If sample already marked as received: Warning displayed but scan allowed (for verification)
- Sample status changes to Received only after successful validation

**BR-018: Receipt Completion Criteria**
- Box can be marked as "Receipt Complete" when:
  - All expected samples are accounted for (Received, Missing, Damaged, or Rejected)
  - Any non-conformities have been documented
- Partial receipts are allowed (box can be completed with missing samples)
- Completion requires confirmation with summary of discrepancies

**BR-019: Missing Sample Handling**
- Samples not received can be marked as Missing
- Missing status is permanent but can be updated to Received if sample arrives late
- Missing samples do not prevent completion of receiving workflow
- Missing samples are included in reconciliation reports

**BR-020: Non-Conformity Recording**
- Non-conformities can be recorded at any point during receiving
- Non-conformity does not automatically reject sample
- Sample with non-conformity is flagged but remains "optionally available for testing"
- Non-conformity flag integrates with existing OpenELIS non-conformity system
- Photos/documents uploaded with non-conformities are linked to sample record

**BR-021: Unexpected Sample Handling**
- Unexpected samples are logged in receiving record but not added to manifest
- Unexpected samples must be followed up outside this workflow
- Receiving can be completed with unexpected samples present
- Unexpected samples are entered into OpenELIS as new samples after investigation

---

### 5.5 Security & Audit Rules

**BR-022: User Authentication & Authorization**
- All actions require authenticated user session
- User must have Shipping role permission to access feature
- Administrator role required for:
  - Configuration changes
  - Editing boxes after Sent state
  - Approving manifest recalls after 24 hours
- Permission checks enforced on both client and server side

**BR-023: Audit Trail Requirements**
- All actions logged with:
  - User ID
  - Timestamp (date and time)
  - Action type
  - Entity affected (Box ID, Sample ID)
  - Previous and new values (for edits)
  - IP address
- Audit records are immutable (cannot be edited or deleted)
- Audit trail retained indefinitely for compliance
- Audit logs queryable for reporting and investigation

**BR-024: Data Retention**
- Active boxes and samples: Retained indefinitely
- Closed/Archived boxes: Retained per organizational policy (recommend 7 years minimum)
- Deleted boxes: Soft-deleted, audit trail retained
- Uploaded documents: Retained with associated sample/box record

---

### 5.6 Integration & Interoperability Rules

**BR-025: FHIR Compliance**
- SupplyDelivery resource used for box tracking
- Status mappings:
  - Draft → SupplyDelivery.status = "in-progress" (preparing)
  - Sent/In Transit → SupplyDelivery.status = "in-progress"
  - Received → SupplyDelivery.status = "completed"
  - Cancelled → SupplyDelivery.status = "abandoned"
- Specimen.container.type used for sample container classification
- Organization references used for sender and receiver facilities

**BR-026: Sample Referral Integration**
- Sample referral flag in OpenELIS synchronized with box inclusion
- Adding non-referral sample to box automatically sets referral flag in order entry
- Referral destination in order entry updated to match box destination
- Removing sample from box does not remove referral flag

**BR-027: Non-Conformity Integration**
- Non-conformities recorded in this system sync with order entry non-conformity system
- Non-conformity types from order entry system available in receiving workflow
- Additional types specific to shipping can be added
- SNOMED CT codes mapped for interoperability (configurable by admin)

---

### 5.7 Unassigned Tests Tracking Rules

**BR-028: Unassigned Test Definition**
- A sample is considered "unassigned" when:
  - Referral flag is set to true in OpenELIS
  - Sample is not in any active box (Draft, Ready to Send, Sent, In Transit, Partially Received states)
  - Sample status is not "Lost" or "Referral Cancelled"
- Once a sample is added to a box, it immediately becomes "assigned" and is removed from unassigned list
- Samples in Received/Closed boxes are considered "assigned" (already shipped)

**BR-029: Unassigned Test Aging**
- Days unassigned calculated from referral date to current date
- Visual indicators based on aging:
  - 0-7 days: Normal (no highlighting)
  - 7-30 days: Warning (yellow highlight)
  - >30 days: Alert (red highlight)
- Aging thresholds configurable by administrator

**BR-030: Mark as Lost Workflow**
- Only unassigned samples can be marked as lost
- Samples in boxes cannot be marked as lost (must be removed from box first)
- Marking as lost is permanent and cannot be undone by regular users
- Administrator can reverse "Lost" status if needed
- Lost samples:
  - Removed from unassigned list
  - Retain referral flag
  - Marked with "Lost" status in separate field
  - Included in Lost Samples report
  - Cannot be added to future boxes unless status reversed

**BR-031: Cancel Referral Workflow**
- Only unassigned samples can have referral cancelled
- Samples in boxes cannot have referral cancelled (must be removed from box first)
- Cancelling referral:
  - Removes referral flag from sample
  - Removes referral destination
  - Clears referral reason
  - Returns sample to normal OpenELIS workflow
  - Sample removed from all shipping tracking
- Cancellation requires reason (audit trail)
- Cancellation can be done by Shipping role or Administrator

**BR-032: Unassigned Test Notifications**
- Optional: System can send alerts for samples unassigned >7 days (configurable)
- Weekly summary report can be generated showing all unassigned samples
- Reports include aging statistics and destination breakdown

**BR-033: Bulk Operations**
- Multiple unassigned samples can be added to same box in bulk operation
- Bulk operations require all selected samples to pass validation
- If any sample fails validation, entire bulk operation is rolled back
- Bulk operations logged as single audit entry with list of affected samples

---

## 6. Functional Requirements

### 6.1 Box Management Functions

**FR-001: Box Creation**
- System shall provide UI to create new box
- System shall auto-generate Box ID using configured prefix + sequential number
- System shall allow manual entry/scan of Box ID
- System shall validate Box ID uniqueness
- System shall require destination selection from facility registry
- System shall save box in Draft state with creator user ID and timestamp

**FR-002: Box Editing**
- System shall provide UI to edit box metadata (destination, notes)
- System shall enforce editing restrictions based on box state
- System shall log all edits in audit trail
- System shall prevent editing after Sent state unless user is Administrator

**FR-003: Box Deletion**
- System shall allow deletion of Draft boxes only
- System shall require confirmation before deletion
- System shall perform soft delete (mark inactive, retain audit trail)
- System shall prevent reuse of deleted Box IDs

**FR-004: Box State Management**
- System shall manage box state transitions per workflow diagram
- System shall validate state transition rules
- System shall update FHIR SupplyDelivery status when state changes
- System shall log all state changes with user ID and timestamp

---

### 6.2 Sample Management Functions

**FR-005: Add Sample via Barcode Scan**
- System shall accept barcode input from USB keyboard wedge scanners
- System shall validate sample exists in OpenELIS
- System shall check for duplicate samples in box
- System shall check if sample already in another active box
- System shall display green checkmark on successful scan
- System shall display red X with error message on failed scan
- System shall add sample to manifest table with status "Pending"

**FR-006: Add Sample via Manual Lookup**
- System shall provide search/auto-complete for sample lookup
- System shall search by accession number or sample ID
- System shall display sample details before adding
- System shall apply same validation as barcode scan
- System shall add confirmed sample to manifest

**FR-007: Handle Non-Referral Samples**
- System shall detect if sample is not marked for referral
- System shall prompt user for reason for referral
- System shall require reason before allowing sample addition
- System shall automatically set referral flag on sample in OpenELIS
- System shall set referral destination to box destination

**FR-008: Display Sample Details**
- System shall display sample list in table format
- System shall show: Accession #, Test Type, Collection Date, Status
- System shall show temperature/storage requirements from order entry
- System shall allow row expansion for additional details
- System shall provide visual status indicators (color-coded)

**FR-009: Remove Sample from Box**
- System shall allow sample removal from boxes in Draft, Ready to Send, In Transit states
- System shall require confirmation before removal
- System shall log removal in audit trail
- System shall revert box to Draft state if removed from Ready to Send box
- System shall not remove referral flag from sample in OpenELIS

**FR-010: Validate Ready to Send**
- System shall provide checkbox to mark box as Ready to Send
- System shall validate:
  - Box has at least 1 sample
  - Destination is selected
  - All samples have required metadata
- System shall display validation errors if requirements not met
- System shall change box state to Ready to Send upon successful validation

---

### 6.3 Label & Manifest Functions

**FR-011: Configure Label Templates (Admin)**
- System shall allow admin to define label templates
- System shall support configuration of:
  - Label dimensions (width x height)
  - Barcode type (Code 39, Code 128, QR, Data Matrix)
  - Layout fields (Box ID, destination, samples, sender, date, temp)
- System shall provide live preview of label template
- System shall allow multiple templates for different destinations

**FR-012: Generate Box Label**
- System shall generate printable label using configured template
- System shall render Box ID as scannable barcode
- System shall populate all configured fields from box metadata
- System shall generate label in printable format (PDF or ZPL)
- System shall log each label print in audit trail

**FR-013: Generate Packing List**
- System shall generate packing list/manifest when requested
- System shall include:
  - Box metadata (ID, destination, sender, date/time)
  - Complete sample list with all metadata
  - Box barcode
  - Optional: Individual sample barcodes
- System shall generate in PDF format
- System shall generate printable format
- System shall support option to include individual sample labels

**FR-014: Manifest Versioning**
- System shall assign version number to each manifest generation
- System shall increment version on regeneration
- System shall retain all previous versions
- System shall display current version by default
- System shall allow viewing previous versions

**FR-015: Manifest Regeneration & Recall**
- System shall allow manifest regeneration within 24 hours of sending
- System shall allow recall request after 24 hours (requires admin approval)
- System shall block regeneration after 7 days
- System shall log all regeneration actions

**FR-016: Electronic Manifest Sending**
- System shall send electronic manifest when box marked as Sent
- System shall check destination configuration for transmission methods
- System shall support:
  - FHIR SupplyDelivery message
  - API POST to configured endpoint
  - Email with PDF attachment
- System shall retry failed transmissions (3 attempts)
- System shall log all transmission attempts
- System shall allow manual retry by user

---

### 6.4 Receiving Functions

**FR-017: Initiate Receiving**
- System shall provide receiving screen with box scan input
- System shall validate box is in Sent or In Transit state
- System shall display box details and expected sample list
- System shall transition box to Partially Received on first sample scan
- System shall allow receiving session to be paused and resumed

**FR-018: Scan Samples During Receipt**
- System shall accept sample barcode scans
- System shall validate sample against manifest
- System shall mark sample as Received on successful scan
- System shall display green checkmark and row highlighting
- System shall provide audio feedback (beep)
- System shall display running count (X of Y received)
- System shall show error for samples not in manifest
- System shall offer "Add as Unexpected" option for unmanifested samples

**FR-019: Manual Sample Check-off**
- System shall provide manual checkbox for each sample
- System shall require confirmation for manual check-off
- System shall mark sample as Received when manually checked
- System shall log manual check-off differently from scan in audit trail
- System shall display visual indicator for manual vs. scanned receipt

**FR-020: Record Non-Conformities**
- System shall provide UI to record non-conformities for samples
- System shall offer non-conformity types from OpenELIS order entry system
- System shall support additional shipping-specific types
- System shall allow multiple non-conformity types per sample
- System shall support free text notes
- System shall support photo/document upload (JPG, PNG, PDF)
- System shall apply non-conformity flag to sample in OpenELIS
- System shall mark sample as "optionally available for testing"
- System shall provide "Apply to All Samples" option for batch issues

**FR-021: Handle Missing Samples**
- System shall allow marking samples as Missing
- System shall require confirmation for missing status
- System shall allow optional notes explaining missing sample
- System shall display missing count in box summary
- System shall allow later update if missing sample arrives

**FR-022: Handle Unexpected Samples**
- System shall detect samples not in manifest
- System shall log unexpected samples in receiving record
- System shall require note explaining unexpected sample
- System shall not add unexpected samples to manifest
- System shall flag unexpected samples for follow-up
- System shall include unexpected samples in receiving report

**FR-023: Complete Receipt**
- System shall provide "Complete Receipt" button
- System shall validate all samples accounted for
- System shall display summary of:
  - Samples received
  - Samples missing
  - Samples with non-conformities
- System shall require confirmation to complete
- System shall transition box to Received state
- System shall update FHIR SupplyDelivery status to "completed"
- System shall record completion timestamp and user

---

### 6.5 Dashboard & Reporting Functions

**FR-024: Display Dashboard Metrics**
- System shall display metric cards:
  - Boxes Ready to Send (count)
  - Boxes In Transit (count)
  - Awaiting Receipt Confirmation (count)
  - Received This Week (count with date range)
- System shall make cards clickable to filter table
- System shall update counts in real-time
- System shall calculate date range for "This Week" dynamically

**FR-025: Display Box List Table**
- System shall display table with columns:
  - Box ID, Destination, Created Date, Status, # Samples, Created By, Sent Date, Received Date
- System shall support sorting on all columns
- System shall provide pagination (25, 50, 100 per page)
- System shall display status with color-coded badges
- System shall default sort to most recent created date first

**FR-026: Filter Box List**
- System shall provide filters:
  - Date range picker (created date)
  - Destination dropdown (from facility registry)
  - Status multi-select
  - Search by Box ID (with auto-complete)
- System shall apply filters in combination (AND logic)
- System shall show active filters with clear indicators
- System shall allow clearing all filters

**FR-027: Provide Quick Actions**
- System shall display actions overflow menu in table rows
- System shall show context-appropriate actions based on box state
- System shall disable/hide actions based on user permissions
- System shall execute actions and display toast notifications
- System shall refresh table after actions complete

**FR-028: Generate Reports**
- System shall provide report generation UI
- System shall support filter criteria:
  - Box ID (specific or multiple)
  - Date Range (sent or received date)
  - Destination
  - Status
  - Created By user
- System shall generate report with columns:
  - Box ID, Contents, Sent Date/Time, Sender, Received Status, Received Date/Time, Receiving User, Non-conformities Count, Missing Samples Count
- System shall support export formats:
  - PDF (formatted report)
  - Excel (.xlsx)
  - CSV
- System shall allow report preview before export
- System shall log report generation in audit trail

---

### 6.5A Unassigned Tests Tracking Functions

**FR-028A: Display Unassigned Tests List**
- System shall provide tab navigation on dashboard: "Shipments" | "Unassigned Tests"
- System shall query all samples where:
  - referral_flag = true
  - sample is not in any shipping_box_sample record with active box
  - sample status ≠ "Lost" and ≠ "Referral Cancelled"
- System shall display table with columns:
  - Accession Number, Patient Name/ID, Test Type, Collection Date, Referral Date, Referral Destination, Referral Reason, Days Unassigned
- System shall calculate "Days Unassigned" as current_date - referral_date
- System shall apply visual highlighting:
  - Yellow background for samples 7-30 days unassigned
  - Red background for samples >30 days unassigned
- System shall refresh list in real-time when samples added to boxes

**FR-028B: Search and Filter Unassigned Tests**
- System shall provide global search across all table columns
- System shall provide filters:
  - Date range picker (referral date)
  - Destination dropdown (from facility registry)
  - Test type dropdown (from OpenELIS test catalog)
  - Days unassigned ranges (0-7, 7-14, 14-30, >30)
- System shall apply filters in combination (AND logic)
- System shall show active filter count and clear option
- System shall support column sorting (ascending/descending)

**FR-028C: Add Unassigned Sample to Box**
- System shall provide "Add to Box" action in row overflow menu
- System shall open modal/side panel with options:
  - Select existing Draft or Ready to Send box (dropdown)
  - Create new box (opens box creation flow)
- System shall validate sample can be added to selected box
- System shall add sample to box and remove from unassigned list
- System shall log action in audit trail
- System shall display success notification with link to box

**FR-028D: Mark Unassigned Sample as Lost**
- System shall provide "Mark as Lost" action in row overflow menu
- System shall display confirmation dialog with:
  - Sample details (accession #, test type, patient)
  - Warning message
  - Required reason text field
- System shall update sample record:
  - Set lost_flag = true
  - Set lost_reason = user input
  - Set lost_date = current timestamp
  - Set lost_by = current user
- System shall keep referral_flag = true
- System shall remove sample from unassigned list
- System shall log action in audit trail
- System shall prevent undo (except by administrator)

**FR-028E: Cancel Referral for Unassigned Sample**
- System shall provide "Cancel Referral" action in row overflow menu
- System shall display confirmation dialog with:
  - Sample details
  - Warning that sample will return to normal workflow
  - Required reason text field
- System shall update sample record in OpenELIS:
  - Set referral_flag = false
  - Clear referral_destination
  - Clear referral_reason
  - Set referral_cancelled_date = current timestamp
  - Set referral_cancelled_by = current user
  - Set referral_cancellation_reason = user input
- System shall remove sample from unassigned list
- System shall log action in audit trail

**FR-028F: Bulk Selection and Actions**
- System shall provide checkbox column for multi-select
- System shall provide "Select All" option (current page or all pages)
- System shall display bulk actions toolbar when samples selected
- System shall show selection count in toolbar
- System shall support bulk actions:
  - "Add Selected to Box" - opens box selection modal
  - "Export Selected" - generates CSV/Excel with selected samples
- System shall validate all selected samples before bulk operation
- System shall execute bulk operation as single transaction (all or nothing)
- System shall log bulk actions with list of affected samples

**FR-028G: Export Unassigned Tests**
- System shall provide "Export" button in unassigned tests view
- System shall export current filtered/searched results
- System shall support export formats:
  - CSV (for Excel compatibility)
  - Excel (.xlsx) with formatting
  - PDF (formatted report)
- System shall include all visible columns in export
- System shall include filter criteria in export header
- System shall log export action

**FR-028H: Unassigned Tests Aging Alerts (Optional)**
- System shall check for samples unassigned >7 days daily
- System shall generate alert list for review
- System shall send email digest to configured users (optional)
- System shall display alert banner on dashboard if aged samples exist
- Alert banner shall show count and link to filtered unassigned list

---

### 6.6 Administration Functions

**FR-029: Manage Label Prefixes**
- System shall allow admin to create/edit/deactivate label prefixes
- System shall support configuration of:
  - Prefix text
  - Counter start number
  - Counter format (digit count, leading zeros)
- System shall allow setting default prefix
- System shall show example preview
- System shall prevent deletion of prefixes in use

**FR-030: Manage Facility Registry**
- System shall allow admin to add/edit/deactivate facilities
- System shall store facility data:
  - Name, Code, Address, Contact Info
  - FHIR endpoint URL
  - API endpoint URL
  - Email addresses for notifications
  - Default label template
- System shall validate URL and email formats
- System shall prevent deletion of facilities in use
- System shall support facility grouping/categorization

**FR-031: Configure FHIR Mappings**
- System shall allow admin to configure FHIR mappings:
  - SupplyDelivery.status for box states
  - Specimen.container.type for container types
  - SNOMED CT codes for non-conformity types
- System shall validate FHIR codes
- System shall provide test connection functionality
- System shall store mapping configuration persistently

**FR-032: Manage User Permissions**
- System shall provide UI to assign Shipping role to users
- System shall integrate with OpenELIS user management
- System shall enforce role-based access control
- System shall log permission changes

---

### 6.7 Integration Functions

**FR-033: OpenELIS Sample Integration**
- System shall query OpenELIS for sample details when adding to box
- System shall retrieve:
  - Accession number, Test type, Collection date
  - Temperature/storage requirements
  - Referral status
- System shall update sample referral flag in OpenELIS when appropriate
- System shall sync non-conformities with OpenELIS order entry system

**FR-034: FHIR Message Exchange**
- System shall generate FHIR SupplyDelivery resource for each box
- System shall update SupplyDelivery.status on box state changes
- System shall POST FHIR messages to configured endpoints
- System shall handle FHIR response codes appropriately
- System shall log all FHIR transactions

**FR-035: API Integration**
- System shall expose API endpoint to receive incoming boxes (FHIR-based)
- System shall expose API endpoint for other OpenELIS instances
- System shall POST manifest JSON to configured API endpoints
- System shall implement authentication for API calls
- System shall retry failed API calls with exponential backoff

**FR-036: Email Notifications**
- System shall send email with PDF manifest when configured
- System shall support multiple recipient addresses per destination
- System shall include box summary in email body
- System shall attach PDF packing list
- System shall log email send status

---

### 6.8 Audit & Compliance Functions

**FR-037: Audit Logging**
- System shall log all user actions with:
  - User ID and username
  - Timestamp (ISO 8601 format with timezone)
  - Action type (CREATE, UPDATE, DELETE, SEND, RECEIVE, etc.)
  - Entity type and ID (Box, Sample)
  - Previous and new values (for updates)
  - IP address
  - Session ID
- System shall make audit logs immutable
- System shall retain audit logs indefinitely
- System shall provide audit log query interface for admins

**FR-038: Compliance Reporting**
- System shall generate audit reports for compliance purposes
- System shall support queries by:
  - User, Date range, Action type, Entity
- System shall export audit logs in standard formats (CSV, JSON)
- System shall track data access for privacy compliance

---

## 7. UI Specifications

### 7.1 Design System Guidelines

**DS-001: Carbon Design System Adherence**
- Use Carbon Design System React components exclusively
- Follow Carbon spacing, typography, and color tokens
- Implement Carbon's grid system for layouts
- Use Carbon icons from `@carbon/icons-react`

**DS-002: Color Coding**
- **Status Colors:**
  - Draft: Gray (`$ui-03`)
  - Ready to Send: Blue (`$support-02`)
  - Sent: Purple (`$purple-60`)
  - In Transit: Cyan (`$cyan-50`)
  - Partially Received: Yellow (`$yellow-30`)
  - Received: Green (`$green-50`)
  - Closed: Green (`$green-60`)
  - Cancelled: Red (`$red-50`)
  - Lost: Red (`$red-60`)
- **Sample Status Colors:**
  - Pending: Gray
  - Received: Green
  - Missing: Yellow
  - Damaged: Orange
  - Rejected: Red

**DS-003: Typography**
- Headings: IBM Plex Sans
- Body: IBM Plex Sans
- Code/IDs: IBM Plex Mono
- Use Carbon type tokens for consistency

---

### 7.2 Layout & Navigation

**UI-001: Main Navigation**
- Add "Sample Shipping" item to OpenELIS main navigation
- Icon: Package/Box icon from Carbon
- Sub-menu items:
  - Dashboard
  - Manage Boxes
  - Receiving
  - Reports
  - Administration (admin only)

**UI-002: Page Structure**
- Header: Breadcrumbs + Page Title + Primary Actions
- Content: Main content area
- Carbon Grid: 16-column layout
- Responsive breakpoints per Carbon specs

---

### 7.3 Dashboard Screen (Based on Pathology Screenshot)

**UI-003: Dashboard Layout**

```
┌─────────────────────────────────────────────────────────────┐
│ Home / Sample Shipping                                       │
│ Dashboard                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ [Shipments] [Unassigned Tests]    <-- Tab Navigation       │
│                                                              │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────┐│
│ │ Boxes Ready  │ │ Boxes In     │ │ Awaiting     │ │ Recv ││
│ │ to Send      │ │ Transit      │ │ Receipt Conf │ │ This ││
│ │              │ │              │ │              │ │ Week ││
│ │      4       │ │      7       │ │      2       │ │      │
│ └──────────────┘ └──────────────┘ └──────────────┘ │  12  │
│                                                      │ (Wk) │
│                                                      └──────┘
│                                                              │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ 🔍 Search by Box ID or Destination   Filters: ☰       │ │
│ ├────────────────────────────────────────────────────────┤ │
│ │ Box ID  │ Destination │ Created │ Status │ # │ ... │ ⋮ ││
│ ├────────────────────────────────────────────────────────┤ │
│ │ REF-001 │ Lab A       │ 10/31   │ 🔵Sent │ 5 │ ... │ ⋮ ││
│ │ REF-002 │ Lab B       │ 11/01   │ 🟢Recv │ 8 │ ... │ ⋮ ││
│ │ REF-003 │ Lab A       │ 11/02   │ 🔵Send │ 3 │ ... │ ⋮ ││
│ │ ...                                                      │ │
│ └────────────────────────────────────────────────────────┘ │
│ Items per page: 25 ▾     1-5 of 25 items    1 ▾  [< >]   │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Carbon Tabs for "Shipments" | "Unassigned Tests" navigation
- Metric Cards: Carbon `Tile` or custom cards styled like pathology cards
- Search: Carbon `Search` component
- Table: Carbon `DataTable` with sorting, pagination, overflow menus
- Filters: Carbon `Dropdown`, `MultiSelect`, `DatePicker`

---

**UI-003A: Unassigned Tests Tab Layout**

```
┌─────────────────────────────────────────────────────────────┐
│ Home / Sample Shipping                                       │
│ Dashboard                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ [Shipments] [Unassigned Tests] ← Active Tab                │
│                                                              │
│ ⚠️ 23 samples have been referred but not assigned to a box │
│    15 samples have been unassigned for >7 days             │
│                                                              │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ 🔍 Search unassigned samples...                        │ │
│ │                                                         │ │
│ │ Filters:                                                │ │
│ │ Date Range: [Last 30 days ▾]  Destination: [All ▾]    │ │
│ │ Test Type: [All ▾]  Days Unassigned: [All ▾]          │ │
│ │                                       [Clear Filters]  │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ <!-- When samples selected: -->                             │
│ ⚡ 3 samples selected  [Add to Box] [Export] [Clear]       │
│                                                              │
│ ┌────────────────────────────────────────────────────────┐ │
│ │☐│Acc#│Patient│Test│Collected│Referred│Dest│Days│ ⋮   ││
│ ├────────────────────────────────────────────────────────┤ │
│ │☐│001 │Smith  │CBC │ 10/15   │ 10/16  │LabA│ 22 │ ⋮  ││ Yellow
│ │☐│002 │Jones  │Chem│ 10/28   │ 10/29  │LabB│  9 │ ⋮  ││ Yellow
│ │☐│003 │Brown  │Sero│ 11/05   │ 11/05  │LabA│  2 │ ⋮  ││ Normal
│ │☐│004 │Davis  │Path│ 09/15   │ 09/16  │LabC│ 52 │ ⋮  ││ Red!
│ │ ...                                                      │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ Items per page: 25 ▾     1-23 of 23 items      [Export]   │
└─────────────────────────────────────────────────────────────┘
```

**Row Actions Menu (⋮):**
- Add to Box...
- Mark as Lost
- Cancel Referral

**Visual Indicators:**
- Normal rows (0-7 days): Standard white background
- Warning rows (7-30 days): Yellow/amber background tint (#FFF3CD)
- Alert rows (>30 days): Red/pink background tint (#FFE6E6)

**Components:**
- Carbon Tabs component
- Alert banner: Carbon `InlineNotification` (warning type)
- Search: Carbon `Search` with full-text capability
- Filters: Carbon `Dropdown`, `DatePicker` with range
- Batch actions toolbar: Carbon DataTable batch actions pattern
- Table: Carbon `DataTable` with checkboxes, sorting, pagination
- Row actions: Carbon `OverflowMenu`

---

**UI-003B: Add Unassigned Sample to Box Modal**

```
┌─────────────────────────────────────────────────────────────┐
│ Add Sample to Shipment                                  [X] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Sample Details                                              │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ Accession #: 24TST00001                              │   │
│ │ Patient: Smith, John                                 │   │
│ │ Test Type: CBC                                       │   │
│ │ Referred: 10/16/2025 (22 days ago)                  │   │
│ │ Destination: Lab A Reference Laboratory              │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Select Box*                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [Select destination box...                       ▾] │   │
│ │                                                      │   │
│ │ Options:                                             │   │
│ │ • Create New Box                                     │   │
│ │ • REF-BOX-00042 (Draft, Lab A, 3 samples)          │   │
│ │ • REF-BOX-00043 (Ready to Send, Lab A, 8 samples)  │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ ℹ️ Only boxes for the same destination (Lab A) are shown   │
│                                                              │
│                                          [Cancel] [Add]     │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Modal: Carbon `Modal` (small size)
- Sample details: Carbon `Tile` with read-only fields
- Dropdown: Carbon `Dropdown` with filtered options
- Info notification: Carbon `InlineNotification` (info type)

---

**UI-003C: Mark Sample as Lost Modal**

```
┌─────────────────────────────────────────────────────────────┐
│ ⚠️ Mark Sample as Lost                                  [X] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Are you sure you want to mark this sample as lost?         │
│                                                              │
│ Sample: 24TST00001 - Smith, John - CBC                     │
│                                                              │
│ This action will:                                           │
│ • Remove the sample from the unassigned list               │
│ • Mark the sample as permanently lost                       │
│ • Generate an incident report                               │
│ • This action CANNOT be undone by regular users            │
│                                                              │
│ Reason for marking as lost*                                │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [Select reason...                                 ▾] │   │
│ │   • Lost in laboratory                               │   │
│ │   • Damaged beyond use                               │   │
│ │   • Improperly stored/disposed                       │   │
│ │   • Unknown location                                 │   │
│ │   • Other (specify below)                            │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Additional Notes                                            │
│ ┌──────────────────────────────────────────────────────┐   │
│ │                                                      │   │
│ │                                                      │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│                                [Cancel] [Mark as Lost]      │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Modal: Carbon `Modal` (medium size) with danger theme
- Warning icon and styling
- Dropdown: Carbon `Dropdown` for reason selection
- Text area: Carbon `TextArea` for additional notes
- Danger button styling for "Mark as Lost"

---

**UI-003D: Cancel Referral Modal**

```
┌─────────────────────────────────────────────────────────────┐
│ Cancel Referral                                         [X] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Sample: 24TST00001 - Smith, John - CBC                     │
│ Current Referral Destination: Lab A Reference Laboratory    │
│                                                              │
│ Cancelling this referral will:                              │
│ • Remove the sample from all shipping tracking             │
│ • Clear the referral flag in OpenELIS                      │
│ • Return the sample to normal processing workflow          │
│                                                              │
│ Reason for cancellation*                                    │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [Select reason...                                 ▾] │   │
│ │   • Test can now be performed in-house               │   │
│ │   • Patient cancelled request                        │   │
│ │   • Referral no longer needed                        │   │
│ │   • Duplicate referral                               │   │
│ │   • Administrative error                             │   │
│ │   • Other (specify below)                            │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Additional Notes                                            │
│ ┌──────────────────────────────────────────────────────┐   │
│ │                                                      │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│                           [Cancel] [Cancel Referral]        │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Modal: Carbon `Modal` (medium size)
- Info section with current referral details
- Dropdown: Carbon `Dropdown` for reason selection
- Text area: Carbon `TextArea` for notes
- Primary button for confirmation

---

**UI-003E: Bulk Add to Box Modal**

```
┌─────────────────────────────────────────────────────────────┐
│ Add Multiple Samples to Shipment                       [X] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ 5 samples selected                                          │
│                                                              │
│ Selected Samples:                                           │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ • 24TST00001 - CBC - Lab A                          │   │
│ │ • 24TST00002 - Chemistry - Lab A                    │   │
│ │ • 24TST00005 - Serology - Lab A                     │   │
│ │ • 24TST00008 - Hematology - Lab A                   │   │
│ │ • 24TST00012 - Microbiology - Lab A                 │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ ✓ All samples are for the same destination (Lab A)         │
│                                                              │
│ Select Box*                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [Select destination box...                       ▾] │   │
│ │                                                      │   │
│ │ • Create New Box                                     │   │
│ │ • REF-BOX-00042 (Draft, Lab A, 3 samples)          │   │
│ │ • REF-BOX-00043 (Ready to Send, Lab A, 8 samples)  │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│                                 [Cancel] [Add All]          │
└─────────────────────────────────────────────────────────────┘

<!-- If samples have different destinations: -->
┌─────────────────────────────────────────────────────────────┐
│ Add Multiple Samples to Shipment                       [X] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ 5 samples selected                                          │
│                                                              │
│ ⚠️ Warning: Selected samples have different destinations    │
│                                                              │
│ • 3 samples → Lab A                                         │
│ • 2 samples → Lab B                                         │
│                                                              │
│ You can either:                                             │
│ • Add samples to separate boxes by destination              │
│ • Deselect samples to have matching destinations            │
│                                                              │
│                    [Add to Separate Boxes] [Cancel]         │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Modal: Carbon `Modal` (medium size)
- Sample list: Read-only list with scrolling if needed
- Validation: Carbon `InlineNotification` for destination matching
- Dropdown: Carbon `Dropdown` for box selection

---

### 7.4 Create/Edit Box Screen

**UI-004: Box Creation Form**

```
┌─────────────────────────────────────────────────────────────┐
│ ← Back to Dashboard                                         │
│ Create New Box                              [Save Draft] [X]│
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Box Information                                             │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ Box ID*        [REF-BOX-00042          ]  (auto)    │   │
│ │ Destination*   [Select destination...   ▾]           │   │
│ │ Notes          [Optional notes...                    ]   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Add Samples                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ 🔍 Scan or enter Sample ID [________________] 🔍     │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Manifest (3 samples)                                        │
│ ☐ Mark as Ready to Send                                    │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Accession # │ Test Type │ Collected │ Status │ ❌     ││
│ ├────────────────────────────────────────────────────────┤ │
│ │ 24TST00010  │ CBC       │ 10/31     │ Pending │ ❌    ││
│ │ 24TST00013  │ Chemistry │ 11/01     │ Pending │ ❌    ││
│ │ 25TST00019  │ Serology  │ 11/02     │ Pending │ ❌    ││
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ [🖨️ Print Label]  [📄 Generate Manifest]  [✉️ Send Box]    │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Form: Carbon `Form`, `TextInput`, `Dropdown`, `TextArea`
- Scan Input: Autofocus, keyboard event listener for scanner
- Checkbox: Carbon `Checkbox` for "Mark as Ready to Send"
- Table: Carbon `DataTable` with inline delete actions
- Buttons: Carbon `Button` (primary, secondary, danger)

**Validation:**
- Real-time validation on "Mark as Ready to Send"
- Inline error messages for missing fields
- Toast notification on successful save

---

### 7.5 Scanning Feedback UI

**UI-005: Scan Success Feedback**
- Green checkmark overlay (✓)
- Fade-in animation (200ms)
- Success beep sound (if enabled)
- Toast notification: "Sample [ID] added"
- Auto-clear input field for next scan

**UI-006: Scan Error Feedback**
- Red X overlay (✗)
- Shake animation (300ms)
- Error beep sound (if enabled)
- Inline error message below input
- Error types:
  - "Sample not found in OpenELIS"
  - "Sample already in this box"
  - "Sample already in box [Box ID]"
  - "Invalid barcode format"
- "Try Again" prompt with input cleared

**Implementation:**
```jsx
// Pseudo-code for scan feedback
const [scanStatus, setScanStatus] = useState(null); // null | 'success' | 'error'
const [scanMessage, setScanMessage] = useState('');

const handleScan = async (barcode) => {
  try {
    await addSampleToBox(barcode);
    setScanStatus('success');
    setScanMessage(`Sample ${barcode} added`);
    playSound('success');
    // Auto-clear after 2s
    setTimeout(() => setScanStatus(null), 2000);
  } catch (error) {
    setScanStatus('error');
    setScanMessage(error.message);
    playSound('error');
  }
};

// Visual feedback component
{scanStatus === 'success' && (
  <CheckmarkFilled className="scan-success-icon" />
)}
{scanStatus === 'error' && (
  <ErrorFilled className="scan-error-icon" />
)}
```

---

### 7.6 Receiving Screen

**UI-007: Receiving Workflow**

```
┌─────────────────────────────────────────────────────────────┐
│ ← Back to Dashboard                                         │
│ Receive Shipment                                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Scan Box ID to Start                                        │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ 🔍 [________________] 📦                             │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ OR                                                          │
│ [Select from In Transit Boxes ▾]                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘

// After box scanned:

┌─────────────────────────────────────────────────────────────┐
│ ← Back                                                       │
│ Receiving: REF-BOX-00042                                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Box Details                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ From: Lab A     Sent: 11/05/2025    Samples: 12     │   │
│ │ Status: 🟡 Partially Received (8 of 12 received)     │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Scan Sample ID                                              │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ 🔍 [________________] ✓ [Progress: 8/12]            │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Filters: [Show Only Unreceived ☐]                          │
│                                                              │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ☐ │ Acc# │ Test │ Status │ Non-Conf │ Actions │      ││
│ ├────────────────────────────────────────────────────────┤ │
│ │ ✅│ 001  │ CBC  │ ✓Recv  │ -        │ 📝      │       ││
│ │ ✅│ 002  │ Chem │ ✓Recv  │ ⚠️Leak   │ 📝      │       ││
│ │ ☐ │ 003  │ Sero │ Pend   │ -        │ 📝 ❌   │       ││
│ │ ...                                                      │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ [⚠️ Report Non-Conformity]  [Complete Receipt]             │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Box Details Card: Carbon `Tile` with key metrics
- Scan Input: Large, prominent, autofocus
- Progress Indicator: Carbon `ProgressBar` or custom component
- Checklist Table: Carbon `DataTable` with checkboxes
- Row Actions: Report issue, Mark missing
- Filter: Carbon `Checkbox` for unreceived only

**Color Coding:**
- Received rows: Green background tint
- Pending rows: Gray
- Non-conformity icon: Yellow/Red warning

---

### 7.7 Non-Conformity Modal

**UI-008: Record Non-Conformity**

```
┌─────────────────────────────────────────────────────────────┐
│ Record Non-Conformity - Sample 24TST00010              [X] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Select Issue Type(s)*                                       │
│ ☑️ Damaged container                                        │
│ ☐ Insufficient volume                                       │
│ ☐ Mislabeled                                                │
│ ☐ Leaked                                                    │
│ ☐ Wrong sample type                                         │
│ ☐ Temperature deviation                                     │
│ ☐ Hemolyzed                                                 │
│ ☐ Clotted                                                   │
│ ☑️ Other                                                    │
│                                                              │
│ Notes                                                        │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ Container was cracked on arrival...                  │   │
│ │                                                      │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ Attach Photo/Documentation (Optional)                       │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [📎 Choose File] or drag and drop                    │   │
│ │ Accepted: JPG, PNG, PDF (max 10MB)                   │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                              │
│ ☐ Apply to all samples in this box                         │
│                                                              │
│ ⚠️ This sample will be flagged but remain available for    │
│    testing with non-conformity noted.                       │
│                                                              │
│                                    [Cancel]  [Record Issue] │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Modal: Carbon `Modal` (medium size)
- Checkboxes: Carbon `CheckboxGroup`
- Text Area: Carbon `TextArea`
- File Upload: Carbon `FileUploader`
- Warning: Carbon `InlineNotification`

---

### 7.8 Reports Screen

**UI-009: Reports Interface**

```
┌─────────────────────────────────────────────────────────────┐
│ Home / Sample Shipping / Reports                            │
│ Shipment Reports                                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ ┌─────────────┐ ┌──────────────────────────────────────┐   │
│ │             │ │ Report Parameters                    │   │
│ │ Filters     │ │                                       │   │
│ │             │ │ Date Range*                          │   │
│ │ Date Range  │ │ ┌──────────┐ to ┌──────────┐        │   │
│ │             │ │ │ 11/01/25 │    │ 11/07/25 │        │   │
│ │ Destination │ │ └──────────┘    └──────────┘        │   │
│ │             │ │                                       │   │
│ │ Status      │ │ Destination                          │   │
│ │             │ │ [All Destinations ▾]                 │   │
│ │ Created By  │ │                                       │   │
│ │             │ │ Status                               │   │
│ │ Box ID      │ │ ☑️ Sent  ☑️ In Transit  ☐ Received   │   │
│ │             │ │                                       │   │
│ │             │ │ Created By                           │   │
│ │             │ │ [All Users ▾]                        │   │
│ │             │ │                                       │   │
│ │             │ │                                       │   │
│ │   [Clear]   │ │ [Preview Report]  [Export ▾]        │   │
│ └─────────────┘ └──────────────────────────────────────┘   │
│                                                              │
│ Report Preview (15 boxes match criteria)                    │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Box ID │ Samples │ Sent Date │ Status │ Issues │ ... ││
│ ├────────────────────────────────────────────────────────┤ │
│ │ REF-001│ 5       │ 11/01     │ Recv   │ 0      │ ... ││
│ │ REF-002│ 8       │ 11/02     │ Recv   │ 2      │ ... ││
│ │ ...                                                      │ │
│ └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Components:**
- Filter Sidebar: Carbon `Accordion` or separate panel
- Date Picker: Carbon `DatePicker` with range selection
- Dropdowns: Carbon `Dropdown`, `MultiSelect`
- Preview Table: Carbon `DataTable`
- Export Menu: Carbon `OverflowMenu` with PDF, Excel, CSV options

---

### 7.9 Administration Screens

**UI-010: Label Configuration (Admin)**

```
┌─────────────────────────────────────────────────────────────┐
│ Administration / Label Configuration                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Label Templates                              [+ New Template]│
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Name      │ Prefix  │ Barcode │ Dimensions │ Default │││
│ ├────────────────────────────────────────────────────────┤ │
│ │ Standard  │ REF-BOX-│ Code128 │ 4x6 in    │ ✓       │⋮││
│ │ Lab A     │ LABA-   │ QR Code │ 3x5 in    │ -       │⋮││
│ │ Frozen    │ FROZ-   │ Code128 │ 4x6 in    │ -       │⋮││
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ Edit Template: Standard                                     │
│ ┌─────────────────────────┐ ┌─────────────────────────┐   │
│ │                         │ │ Template Details        │   │
│ │  ┌─────────────────┐   │ │                         │   │
│ │  │  REF-BOX-00042  │   │ │ Name*                   │   │
│ │  │                 │   │ │ [Standard           ]   │   │
│ │  │  █████████      │   │ │                         │   │
│ │  │                 │   │ │ Prefix*                 │   │
│ │  │  Lab A          │   │ │ [REF-BOX-           ]   │   │
│ │  │  12 samples     │   │ │                         │   │
│ │  │  11/07/2025     │   │ │ Barcode Type*           │   │
│ │  │  Sender: User   │   │ │ [Code 128          ▾]   │   │
│ │  └─────────────────┘   │ │                         │   │
│ │                         │ │ Dimensions*             │   │
│ │  Live Preview           │ │ Width  [4] [inches ▾]   │   │
│ │                         │ │ Height [6] [inches ▾]   │   │
│ └─────────────────────────┘ │                         │   │
│                              │ [Save] [Cancel]        │   │
│                              └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**UI-011: Facility Registry (Admin)**

```
┌─────────────────────────────────────────────────────────────┐
│ Administration / Facility Registry                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Reference Lab Destinations                   [+ Add Facility]│
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Name       │ Code │ City │ Active │ Integration │ ⋮   ││
│ ├────────────────────────────────────────────────────────┤ │
│ │ Lab A      │ LABA │ NYC  │ ✓      │ FHIR+API   │ ⋮   ││
│ │ Lab B      │ LABB │ LA   │ ✓      │ Email      │ ⋮   ││
│ │ Ref Lab C  │ REFC │ CHI  │ -      │ None       │ ⋮   ││
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ Edit Facility: Lab A                                        │
│ ┌───────────────────────── ──────────────────────────────┐ │
│ │ Basic Information                Integration Settings  │ │
│ │                                                         │ │
│ │ Name*     [Lab A Reference Laboratory            ]     │ │
│ │ Code*     [LABA    ]                                   │ │
│ │ Address   [123 Main St                           ]     │ │
│ │           [New York, NY 10001                    ]     │ │
│ │ Contact   [contact@laba.com                      ]     │ │
│ │           [+1-555-0100                           ]     │ │
│ │                                                         │ │
│ │ ☑️ FHIR Integration                                     │ │
│ │   Endpoint [https://laba.com/fhir               ]     │ │
│ │   [Test Connection]                                    │ │
│ │                                                         │ │
│ │ ☑️ API Integration                                      │ │
│ │   Endpoint [https://api.laba.com/shipments      ]     │ │
│ │   API Key  [••••••••••••••••                     ]     │ │
│ │   [Test Connection]                                    │ │
│ │                                                         │ │
│ │ ☑️ Email Notifications                                  │ │
│ │   To: [receiving@laba.com, qa@laba.com           ]     │ │
│ │                                                         │ │
│ │ Default Label Template [Standard ▾]                    │ │
│ │                                                         │ │
│ │                                    [Save] [Cancel]     │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

### 7.10 Responsive Design

**UI-012: Breakpoints**
- Large Desktop (≥1584px): Full 16-column grid
- Desktop (1056px-1583px): Full features
- Tablet (672px-1055px): Collapsed sidebar, stacked metric cards
- Mobile (≤671px): Single column, bottom navigation, simplified tables

**UI-013: Mobile Adaptations**
- Dashboard: Stack metric cards vertically
- Table: Horizontal scroll or card view
- Forms: Full-width inputs
- Scanning: Large scan button, camera option (if implemented later)

---

## 8. Integration Requirements

### 8.1 OpenELIS Integration

**INT-001: Sample Data Retrieval**
- API endpoint: `GET /rest/sample/{accessionNumber}`
- Response includes:
  - Accession number, Sample ID
  - Patient information (ID, name)
  - Test type(s)
  - Collection date
  - Temperature/storage requirements
  - Current status
  - Referral flag

**INT-002: Referral Flag Update**
- API endpoint: `PATCH /rest/sample/{accessionNumber}/referral`
- Request body:
  ```json
  {
    "referralFlag": true,
    "referralReason": "string",
    "referralDestination": "facility_code",
    "referralDate": "ISO8601 datetime"
  }
  ```

**INT-003: Non-Conformity Sync**
- API endpoint: `POST /rest/sample/{accessionNumber}/non-conformity`
- Request body:
  ```json
  {
    "types": ["type1", "type2"],
    "notes": "string",
    "recordedBy": "user_id",
    "recordedAt": "ISO8601 datetime",
    "attachments": ["file_id1", "file_id2"]
  }
  ```
- Response: Non-conformity ID and flag applied to sample

---

### 8.2 FHIR Integration

**INT-004: SupplyDelivery Resource**

Create SupplyDelivery when box is sent:
```json
{
  "resourceType": "SupplyDelivery",
  "id": "box-123",
  "identifier": [{
    "system": "http://openelis.org/box-id",
    "value": "REF-BOX-00042"
  }],
  "status": "in-progress",
  "patient": null,
  "type": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/supply-item-type",
      "code": "specimen",
      "display": "Specimen Container"
    }]
  },
  "suppliedItem": {
    "quantity": {
      "value": 12,
      "unit": "specimen"
    },
    "itemReference": {
      "reference": "#manifest-123"
    }
  },
  "destination": {
    "reference": "Organization/lab-a",
    "display": "Lab A Reference Laboratory"
  },
  "supplier": {
    "reference": "Organization/sending-facility"
  },
  "occurrenceDateTime": "2025-11-07T10:30:00Z",
  "receiver": [{
    "reference": "Practitioner/receiving-tech"
  }]
}
```

**INT-005: Status Updates**
- Update SupplyDelivery.status on box state changes:
  - Sent → `in-progress`
  - Received → `completed`
  - Cancelled → `abandoned`
  - Lost → `abandoned` (with note)

**INT-006: Specimen References**
- Each sample in manifest linked as contained Specimen resources
- Specimen.identifier = sample accession number
- Specimen.container.type = container type code

---

### 8.3 API Integration (OpenELIS-to-OpenELIS)

**INT-007: Send Manifest API**

Endpoint: `POST {configured_url}/api/v1/shipments/incoming`

Request:
```json
{
  "shipment": {
    "boxId": "REF-BOX-00042",
    "origin": {
      "facilityCode": "SENDER",
      "facilityName": "Sending Facility",
      "address": "..."
    },
    "destination": {
      "facilityCode": "LABA",
      "facilityName": "Lab A"
    },
    "sentDate": "2025-11-07T10:30:00Z",
    "sentBy": {
      "userId": "user123",
      "userName": "John Doe"
    },
    "samples": [
      {
        "accessionNumber": "24TST00010",
        "testType": "CBC",
        "collectionDate": "2025-10-31",
        "temperatureRequirement": "refrigerated",
        "containerType": "EDTA tube"
      },
      ...
    ]
  }
}
```

Response:
```json
{
  "success": true,
  "shipmentId": "INCOMING-789",
  "message": "Shipment received and recorded"
}
```

**INT-008: Receive Incoming API**

Endpoint: `POST /api/v1/shipments/incoming` (on receiving system)

This endpoint is exposed by the receiving OpenELIS instance to accept incoming manifests from other instances.

---

### 8.4 Email Integration

**INT-009: Email Notification**

When box is sent (if configured):
- To: Configured email addresses for destination
- CC: Sender email
- Subject: `Shipment [Box ID] Sent - [# Samples] Samples`
- Body:
  ```
  A new shipment has been sent to your facility.
  
  Box ID: REF-BOX-00042
  From: Sending Facility
  Sent Date: November 7, 2025 10:30 AM
  Number of Samples: 12
  Sent By: John Doe
  
  Please see attached packing list for complete manifest.
  
  [View in OpenELIS] (link if available)
  ```
- Attachment: PDF packing list

**INT-010: Email Configuration**
- Use OpenELIS email configuration (SMTP settings)
- Support multiple recipients (comma-separated)
- Track email send status (success/failure)
- Retry logic for failed sends (3 attempts)

---

### 8.5 Barcode Scanner Integration

**INT-011: Keyboard Wedge Scanners**
- Accept input from USB barcode scanners
- Scanner emulates keyboard input
- Detect barcode input patterns:
  - Rapid keystroke sequence
  - Terminated by Enter/Tab key
  - Longer than typical keyboard input
- Parse barcode data:
  - Strip prefixes/suffixes if configured
  - Validate format (alphanumeric, specific patterns)

**INT-012: Barcode Formats**
- Support common formats:
  - Code 39
  - Code 128
  - QR Code
  - Data Matrix
  - PDF417
- Configure expected format per facility
- Validate barcode checksum if applicable

---

## 9. Non-Functional Requirements

### 9.1 Performance

**NFR-001: Response Time**
- Box creation: < 1 second
- Sample scan processing: < 500ms
- Dashboard load: < 2 seconds
- Report generation (< 100 boxes): < 5 seconds
- Report generation (> 100 boxes): < 30 seconds

**NFR-002: Throughput**
- Support 10,000+ samples/day across all boxes
- Support 500+ concurrent users
- Support 100+ simultaneous barcode scans/minute

**NFR-003: Scalability**
- Horizontal scaling for web servers
- Database optimization for large datasets (millions of samples)
- Efficient indexing on Box ID, Sample ID, dates

---

### 9.2 Security

**NFR-004: Authentication**
- Integrate with OpenELIS authentication system
- Support single sign-on (SSO) if implemented
- Session timeout after 30 minutes of inactivity
- Require re-authentication for sensitive actions

**NFR-005: Authorization**
- Role-based access control (Shipping role, Admin role)
- Permission checks on all API endpoints
- Client-side and server-side authorization enforcement
- Audit all permission checks

**NFR-006: Data Protection**
- Encrypt data in transit (TLS 1.2+)
- Encrypt sensitive data at rest (database encryption)
- Redact sensitive data in logs
- Follow HIPAA/PHI guidelines for patient data

**NFR-007: Audit Trail**
- Immutable audit logs
- Log all user actions with timestamp, user ID, IP address
- Retain audit logs per compliance requirements (minimum 7 years)
- Support audit log export for compliance reporting

---

### 9.3 Reliability

**NFR-008: Availability**
- 99.9% uptime during business hours
- Scheduled maintenance windows communicated in advance
- Graceful degradation if external integrations fail

**NFR-009: Data Integrity**
- Database transactions for all critical operations
- Foreign key constraints enforced
- Data validation on input (client and server)
- Backup strategy: Daily full backup, hourly incremental

**NFR-010: Error Handling**
- User-friendly error messages
- Technical details logged server-side
- Automatic retry for transient failures (with exponential backoff)
- Fallback mechanisms for critical workflows

---

### 9.4 Usability

**NFR-011: Accessibility**
- WCAG 2.1 Level AA compliance
- Keyboard navigation support
- Screen reader compatible
- Sufficient color contrast
- Focus indicators visible

**NFR-012: User Experience**
- Consistent with OpenELIS UI patterns
- Carbon Design System adherence
- Responsive design (desktop, tablet, mobile)
- Loading indicators for async operations
- Inline help text and tooltips

**NFR-013: Training & Documentation**
- User manual with screenshots
- Video tutorials for key workflows
- Contextual help within application
- FAQ section

---

### 9.5 Maintainability

**NFR-014: Code Quality**
- React components following best practices
- TypeScript for type safety
- ESLint and Prettier for code style
- Unit test coverage > 80%
- Integration tests for critical paths

**NFR-015: Monitoring & Logging**
- Application performance monitoring (APM)
- Error tracking (e.g., Sentry)
- Usage analytics (without PHI)
- Server and database health monitoring

**NFR-016: Deployment**
- CI/CD pipeline for automated testing and deployment
- Blue-green deployment for zero-downtime updates
- Database migration scripts version-controlled
- Rollback capability for failed deployments

---

## 10. Data Model

### 10.1 Core Entities

**Box**
```sql
CREATE TABLE shipping_box (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  box_id VARCHAR(50) UNIQUE NOT NULL,
  destination_facility_id BIGINT NOT NULL,
  status ENUM('DRAFT', 'READY_TO_SEND', 'SENT', 'IN_TRANSIT', 
              'PARTIALLY_RECEIVED', 'RECEIVED', 'CLOSED', 
              'CANCELLED', 'LOST') NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  sent_by BIGINT,
  sent_at TIMESTAMP,
  received_by BIGINT,
  received_at TIMESTAMP,
  completed_by BIGINT,
  completed_at TIMESTAMP,
  notes TEXT,
  FOREIGN KEY (destination_facility_id) REFERENCES facility(id),
  FOREIGN KEY (created_by) REFERENCES user(id),
  FOREIGN KEY (sent_by) REFERENCES user(id),
  FOREIGN KEY (received_by) REFERENCES user(id),
  FOREIGN KEY (completed_by) REFERENCES user(id),
  INDEX idx_box_id (box_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at),
  INDEX idx_destination (destination_facility_id)
);
```

**BoxSample** (Junction table)
```sql
CREATE TABLE shipping_box_sample (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  box_id BIGINT NOT NULL,
  sample_id BIGINT NOT NULL,
  accession_number VARCHAR(50) NOT NULL,
  status ENUM('PENDING', 'SENT', 'RECEIVED', 'MISSING', 
              'DAMAGED', 'REJECTED') NOT NULL,
  added_by BIGINT NOT NULL,
  added_at TIMESTAMP NOT NULL,
  received_by BIGINT,
  received_at TIMESTAMP,
  receipt_method ENUM('SCANNED', 'MANUAL') DEFAULT 'SCANNED',
  removed_by BIGINT,
  removed_at TIMESTAMP,
  notes TEXT,
  FOREIGN KEY (box_id) REFERENCES shipping_box(id),
  FOREIGN KEY (sample_id) REFERENCES sample(id),
  FOREIGN KEY (added_by) REFERENCES user(id),
  FOREIGN KEY (received_by) REFERENCES user(id),
  FOREIGN KEY (removed_by) REFERENCES user(id),
  UNIQUE KEY unique_sample_per_box (box_id, sample_id),
  INDEX idx_accession (accession_number),
  INDEX idx_box_status (box_id, status)
);
```

**BoxManifest**
```sql
CREATE TABLE shipping_box_manifest (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  box_id BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  generated_by BIGINT NOT NULL,
  generated_at TIMESTAMP NOT NULL,
  manifest_pdf_path VARCHAR(500),
  is_current BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (box_id) REFERENCES shipping_box(id),
  FOREIGN KEY (generated_by) REFERENCES user(id),
  INDEX idx_box_version (box_id, version)
);
```

**SampleNonConformity**
```sql
CREATE TABLE shipping_sample_non_conformity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  box_sample_id BIGINT NOT NULL,
  non_conformity_type_id BIGINT NOT NULL,
  notes TEXT,
  recorded_by BIGINT NOT NULL,
  recorded_at TIMESTAMP NOT NULL,
  FOREIGN KEY (box_sample_id) REFERENCES shipping_box_sample(id),
  FOREIGN KEY (non_conformity_type_id) REFERENCES non_conformity_type(id),
  FOREIGN KEY (recorded_by) REFERENCES user(id),
  INDEX idx_box_sample (box_sample_id)
);
```

**NonConformityAttachment**
```sql
CREATE TABLE shipping_non_conformity_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  non_conformity_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_type VARCHAR(50),
  file_size BIGINT,
  uploaded_by BIGINT NOT NULL,
  uploaded_at TIMESTAMP NOT NULL,
  FOREIGN KEY (non_conformity_id) REFERENCES shipping_sample_non_conformity(id),
  FOREIGN KEY (uploaded_by) REFERENCES user(id)
);
```

**Facility** (extends existing OpenELIS facility table)
```sql
CREATE TABLE facility_integration_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  facility_id BIGINT NOT NULL,
  fhir_enabled BOOLEAN DEFAULT FALSE,
  fhir_endpoint VARCHAR(500),
  api_enabled BOOLEAN DEFAULT FALSE,
  api_endpoint VARCHAR(500),
  api_key_encrypted VARCHAR(500),
  email_enabled BOOLEAN DEFAULT FALSE,
  email_recipients TEXT,
  default_label_template_id BIGINT,
  FOREIGN KEY (facility_id) REFERENCES facility(id),
  FOREIGN KEY (default_label_template_id) REFERENCES label_template(id),
  UNIQUE KEY unique_facility_config (facility_id)
);
```

**LabelTemplate**
```sql
CREATE TABLE label_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  prefix VARCHAR(20) NOT NULL,
  counter_format VARCHAR(20) DEFAULT '%05d',
  barcode_type ENUM('CODE39', 'CODE128', 'QR', 'DATAMATRIX') NOT NULL,
  label_width_mm INT NOT NULL,
  label_height_mm INT NOT NULL,
  template_json TEXT,
  is_default BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (created_by) REFERENCES user(id)
);
```

**AuditLog**
```sql
CREATE TABLE shipping_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  old_value TEXT,
  new_value TEXT,
  ip_address VARCHAR(45),
  session_id VARCHAR(100),
  timestamp TIMESTAMP NOT NULL,
  FOREIGN KEY (user_id) REFERENCES user(id),
  INDEX idx_entity (entity_type, entity_id),
  INDEX idx_user_timestamp (user_id, timestamp),
  INDEX idx_timestamp (timestamp)
);
```

**TransmissionLog**
```sql
CREATE TABLE shipping_transmission_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  box_id BIGINT NOT NULL,
  transmission_type ENUM('FHIR', 'API', 'EMAIL') NOT NULL,
  status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL,
  attempt_number INT DEFAULT 1,
  request_payload TEXT,
  response_payload TEXT,
  error_message TEXT,
  transmitted_at TIMESTAMP NOT NULL,
  FOREIGN KEY (box_id) REFERENCES shipping_box(id),
  INDEX idx_box_status (box_id, status)
);
```

**Sample** (extends existing OpenELIS sample table)
```sql
-- Add columns to existing sample table for referral tracking
ALTER TABLE sample ADD COLUMN referral_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE sample ADD COLUMN referral_date TIMESTAMP NULL;
ALTER TABLE sample ADD COLUMN referral_destination_facility_id BIGINT NULL;
ALTER TABLE sample ADD COLUMN referral_reason TEXT NULL;
ALTER TABLE sample ADD COLUMN referral_cancelled_date TIMESTAMP NULL;
ALTER TABLE sample ADD COLUMN referral_cancelled_by BIGINT NULL;
ALTER TABLE sample ADD COLUMN referral_cancellation_reason TEXT NULL;
ALTER TABLE sample ADD COLUMN lost_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE sample ADD COLUMN lost_date TIMESTAMP NULL;
ALTER TABLE sample ADD COLUMN lost_by BIGINT NULL;
ALTER TABLE sample ADD COLUMN lost_reason TEXT NULL;

-- Indexes for unassigned tracking
CREATE INDEX idx_referral_flag ON sample(referral_flag);
CREATE INDEX idx_referral_date ON sample(referral_date);
CREATE INDEX idx_lost_flag ON sample(lost_flag);

-- Foreign keys
ALTER TABLE sample ADD FOREIGN KEY (referral_destination_facility_id) REFERENCES facility(id);
ALTER TABLE sample ADD FOREIGN KEY (referral_cancelled_by) REFERENCES user(id);
ALTER TABLE sample ADD FOREIGN KEY (lost_by) REFERENCES user(id);
```

**UnassignedSampleView** (Database view for performance)
```sql
-- View to efficiently query unassigned samples
CREATE VIEW unassigned_samples_view AS
SELECT 
  s.id,
  s.accession_number,
  s.patient_id,
  s.test_type,
  s.collection_date,
  s.referral_date,
  s.referral_destination_facility_id,
  s.referral_reason,
  f.name AS referral_destination_name,
  DATEDIFF(CURRENT_DATE, s.referral_date) AS days_unassigned,
  CASE 
    WHEN DATEDIFF(CURRENT_DATE, s.referral_date) > 30 THEN 'RED'
    WHEN DATEDIFF(CURRENT_DATE, s.referral_date) > 7 THEN 'YELLOW'
    ELSE 'NORMAL'
  END AS alert_level
FROM sample s
LEFT JOIN facility f ON s.referral_destination_facility_id = f.id
WHERE s.referral_flag = TRUE
  AND s.lost_flag = FALSE
  AND s.referral_cancelled_date IS NULL
  AND s.id NOT IN (
    SELECT DISTINCT sample_id 
    FROM shipping_box_sample sbs
    INNER JOIN shipping_box sb ON sbs.box_id = sb.id
    WHERE sb.status IN ('DRAFT', 'READY_TO_SEND', 'SENT', 'IN_TRANSIT', 'PARTIALLY_RECEIVED', 'RECEIVED')
      AND sbs.removed_at IS NULL
  );
```

**LostSampleLog**
```sql
CREATE TABLE lost_sample_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sample_id BIGINT NOT NULL,
  accession_number VARCHAR(50) NOT NULL,
  lost_reason_category ENUM('LOST_IN_LAB', 'DAMAGED', 'IMPROPERLY_STORED', 
                            'UNKNOWN_LOCATION', 'OTHER') NOT NULL,
  lost_reason_notes TEXT,
  marked_lost_by BIGINT NOT NULL,
  marked_lost_at TIMESTAMP NOT NULL,
  reversed_by BIGINT,
  reversed_at TIMESTAMP,
  reversal_reason TEXT,
  FOREIGN KEY (sample_id) REFERENCES sample(id),
  FOREIGN KEY (marked_lost_by) REFERENCES user(id),
  FOREIGN KEY (reversed_by) REFERENCES user(id),
  INDEX idx_sample (sample_id),
  INDEX idx_marked_lost_at (marked_lost_at)
);
```

**ReferralCancellationLog**
```sql
CREATE TABLE referral_cancellation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sample_id BIGINT NOT NULL,
  accession_number VARCHAR(50) NOT NULL,
  previous_destination_facility_id BIGINT NOT NULL,
  cancellation_reason_category ENUM('TEST_IN_HOUSE', 'PATIENT_CANCELLED', 
                                     'NO_LONGER_NEEDED', 'DUPLICATE', 
                                     'ADMIN_ERROR', 'OTHER') NOT NULL,
  cancellation_reason_notes TEXT,
  cancelled_by BIGINT NOT NULL,
  cancelled_at TIMESTAMP NOT NULL,
  FOREIGN KEY (sample_id) REFERENCES sample(id),
  FOREIGN KEY (previous_destination_facility_id) REFERENCES facility(id),
  FOREIGN KEY (cancelled_by) REFERENCES user(id),
  INDEX idx_sample (sample_id),
  INDEX idx_cancelled_at (cancelled_at)
);
```

**UnassignedAlertConfig** (Admin configuration)
```sql
CREATE TABLE unassigned_alert_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_enabled BOOLEAN DEFAULT FALSE,
  warning_threshold_days INT DEFAULT 7,
  alert_threshold_days INT DEFAULT 30,
  email_notification_enabled BOOLEAN DEFAULT FALSE,
  email_recipients TEXT,
  notification_frequency ENUM('DAILY', 'WEEKLY') DEFAULT 'WEEKLY',
  last_notification_sent TIMESTAMP,
  created_by BIGINT NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  FOREIGN KEY (created_by) REFERENCES user(id)
);
```

---

### 10.2 Entity Relationships

```
sample (1) ────── (M) shipping_box_sample (M) ────── (1) shipping_box
   │                         │                               │
   │                         │                               ├──────< (M) shipping_box_manifest
   │                         │                               │
   │                         └──────< (M) shipping_sample_non_conformity
   │                                           │             ├──────< (M) shipping_transmission_log
   │                                           │             │
   │                                           │             └────── (1) facility
   │                                           └──────< (M) shipping_non_conformity_attachment
   │                                                                   │
   │                                                                   └────── (1) facility_integration_config
   ├────── (1) lost_sample_log                                                      │
   │                                                                                 └────── (1) label_template
   ├────── (1) referral_cancellation_log
   │
   └────── (1) facility (referral_destination)


unassigned_samples_view (derived from sample + facility + shipping_box_sample)
```

**Key Relationships:**
- Sample can be in multiple boxes over time (historical tracking)
- Sample can have 0 or 1 lost_sample_log entry
- Sample can have 0 or 1 referral_cancellation_log entry  
- Sample with referral_flag=true and not in active box appears in unassigned_samples_view
- Facility referenced by both box destination and sample referral destination

---

### 10.3 Sample Data

**Example Box:**
```json
{
  "id": 123,
  "boxId": "REF-BOX-00042",
  "destinationFacility": {
    "id": 5,
    "name": "Lab A Reference Laboratory",
    "code": "LABA"
  },
  "status": "SENT",
  "createdBy": {
    "id": 10,
    "name": "John Doe"
  },
  "createdAt": "2025-11-05T08:00:00Z",
  "sentBy": {
    "id": 10,
    "name": "John Doe"
  },
  "sentAt": "2025-11-07T10:30:00Z",
  "samples": [
    {
      "id": 1001,
      "accessionNumber": "24TST00010",
      "testType": "CBC",
      "collectionDate": "2025-10-31",
      "status": "SENT",
      "addedAt": "2025-11-05T08:15:00Z"
    },
    ...
  ],
  "notes": "Urgent samples for processing"
}
```

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| **Accession Number** | Unique identifier for a laboratory sample/specimen |
| **Box** | Physical container used to ship multiple samples |
| **Carbon Design System** | IBM's open-source design system and component library |
| **FHIR** | Fast Healthcare Interoperability Resources - HL7 standard |
| **Keyboard Wedge** | Barcode scanner that emulates keyboard input |
| **Manifest** | Document listing all samples in a shipment |
| **Non-conformity** | Quality issue or deviation from expected sample condition |
| **OpenELIS** | Open source Laboratory Information System |
| **Packing List** | Same as manifest - printed list of box contents |
| **Referral** | Process of sending sample to external lab for testing |
| **SupplyDelivery** | FHIR resource type for tracking shipments |
| **Unassigned Sample** | Sample marked for referral but not yet added to any shipment box |
| **Days Unassigned** | Number of days since a sample was marked for referral but remains unassigned |
| **Lost Sample** | Sample that cannot be located and is permanently marked as lost |
| **Referral Cancellation** | Removal of referral flag from a sample, returning it to normal workflow |

---

## Appendix B: FHIR Status Mappings

| Box Status | SupplyDelivery.status | Description |
|------------|----------------------|-------------|
| Draft | in-progress | Box being prepared |
| Ready to Send | in-progress | Box ready for shipment |
| Sent | in-progress | Box dispatched |
| In Transit | in-progress | Box en route |
| Partially Received | in-progress | Receiving in progress |
| Received | completed | All samples confirmed |
| Closed/Archived | completed | Reconciliation complete |
| Cancelled | abandoned | Shipment cancelled |
| Lost in Transit | abandoned | Box not received |

---

## Appendix C: User Permissions Matrix

| Action | Shipping Role | Admin Role |
|--------|--------------|-----------|
| View dashboard | ✓ | ✓ |
| View unassigned tests | ✓ | ✓ |
| Add unassigned sample to box | ✓ | ✓ |
| Mark sample as lost | ✓ | ✓ |
| Reverse lost status | ✗ | ✓ |
| Cancel referral | ✓ | ✓ |
| Export unassigned tests | ✓ | ✓ |
| Configure aging thresholds | ✗ | ✓ |
| Configure unassigned alerts | ✗ | ✓ |
| Create box | ✓ | ✓ |
| Edit box (Draft) | ✓ | ✓ |
| Edit box (Ready to Send) | ✓ | ✓ |
| Edit box (after Sent) | ✗ | ✓ |
| Delete box | ✓ | ✓ |
| Add/remove samples | ✓ | ✓ |
| Send box | ✓ | ✓ |
| Receive box | ✓ | ✓ |
| Record non-conformities | ✓ | ✓ |
| Generate reports | ✓ | ✓ |
| Configure labels | ✗ | ✓ |
| Manage facilities | ✗ | ✓ |
| Configure FHIR | ✗ | ✓ |
| View audit logs | ✗ | ✓ |
| Approve manifest recall | ✗ | ✓ |

---

**End of Specification**

---

## Document Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-07 | System Analyst | Initial comprehensive specification |
| 1.1 | 2025-11-07 | System Analyst | Added Unassigned Tests tracking feature: new dashboard tab, search/filter capabilities, mark as lost and cancel referral workflows, bulk actions, data model extensions, and business rules |

---

## Next Steps

1. **Review & Approval:** Stakeholder review of this specification
2. **Technical Design:** Database schema finalization, API design
3. **UI/UX Design:** Detailed mockups and prototypes
4. **Development Sprint Planning:** Break into epics and user stories
5. **Development:** Iterative development with sprint demos
6. **Testing:** Unit, integration, UAT
7. **Deployment:** Staged rollout to production
8. **Training:** User training and documentation
9. **Go-Live:** Production release with monitoring
10. **Post-Launch:** Gather feedback, iterate on improvements
