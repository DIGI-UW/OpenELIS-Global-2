# Sample Collection Workflow Implementation Guide

> **Source:** >
> [FRS v1.0 - Sample Collection Redesign](https://github.com/DIGI-UW/openelis-work/blob/main/designs/sample-collection/sample-collection-redesign.md)
>
> **Last Updated:** 2026-03-09

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Order Dashboard (DSH-\*)](#order-dashboard-dsh)
4. [Step 1: Enter Order (ORD-\*)](#step-1-enter-order-ord)
5. [Step 2: Collect Sample (COL-\*)](#step-2-collect-sample-col)
6. [Step 3: Label & Store (LBL-\*)](#step-3-label--store-lbl)
7. [Step 4: QA Review (QA-\*)](#step-4-qa-review-qa)
8. [Edit Order Workflow (EDT-\*)](#edit-order-workflow-edt)
9. [External/Incoming Orders (INC-\*)](#externalincoming-orders-inc)
10. [Cross-Cutting Concerns (XC-\*)](#cross-cutting-concerns-xc)
11. [API Endpoints](#api-endpoints)
12. [Implementation Phases](#implementation-phases)

---

## Overview

The FRS redesigns OpenELIS's monolithic order-entry form into **four decoupled
workflow steps**, unifying clinical and environmental sample tracking while
enabling future physician-facing integrations.

### Core Workflow

| Step | Route            | Actor           | Primary Function                                   |
| ---- | ---------------- | --------------- | -------------------------------------------------- |
| 1    | `/order/enter`   | Physician/Clerk | Patient/subject info, requester, tests, priority   |
| 2    | `/order/collect` | Phlebotomist    | Sample type, timing, conditions, NCE reporting     |
| 3    | `/order/label`   | Lab Tech        | Lab number assignment, barcode generation, storage |
| 4    | `/order/qa`      | QA Officer      | Completeness verification, rejection handling      |

### Navigation Model

- **Default:** Sequential wizard flow (Step 1 → 2 → 3 → 4)
- **Shortcut:** Experienced users jump directly to any step via sidebar menu
- **Edit:** Barcode scan loads existing orders in read-only mode with "Edit"
  button
- **Dashboard:** "Add Order" parent menu routes to order management dashboard

---

## Architecture

### OrderContext Shared State

A centralized React context holds order state across all 4 steps:

```typescript
interface OrderContextState {
  // Identification
  orderId: string | null;
  labNumber: string | null;

  // Workflow
  workflowType: "Clinical" | "Environmental" | "Both";
  currentStep: 0 | 1 | 2 | 3;
  orderStatus: "Draft" | "InProgress" | "AwaitingQA" | "Complete" | "Rejected";

  // Patient/Subject
  patientProperties: PatientData | null; // Clinical
  locationHierarchy: LocationData | null; // Environmental

  // Order Data
  requesterInfo: RequesterData | null;
  selectedTests: Test[];
  selectedPanels: Panel[];
  program: Program | null;
  additionalQuestions: Record<string, any>;

  // Samples
  samples: Sample[];

  // Step Progress
  stepProgress: {
    enter: boolean;
    collect: boolean;
    label: boolean;
    qa: boolean;
  };

  // State Management
  isReadOnly: boolean;
  isEditMode: boolean;
  isDirty: boolean;
  saveStatus: "saved" | "saving" | "unsaved" | "error";

  // Audit
  auditTrail: AuditEvent[];
}
```

### File Structure

```
frontend/src/components/order/
├── index.js                    # Module exports
├── OrderContext.js             # Shared state provider
├── OrderDashboard.jsx          # DSH-1 to DSH-9
├── OrderContextCard.jsx        # NAV-5: Persistent context card
├── OrderStepper.jsx            # NAV-3: Progress indicator
├── BarcodeScannerBar.jsx       # NAV-6, NAV-8: Scan + feedback
├── SaveNavigationButtons.jsx   # NAV-4: Save & Next
├── OrderWorkflowLayout.jsx     # Shared layout wrapper
├── order-workflow.scss         # WCAG 2.1 AA styles
└── steps/
    ├── index.js
    ├── OrderEnter.jsx          # Step 1: ORD-*
    ├── OrderCollect.jsx        # Step 2: COL-*
    ├── OrderLabel.jsx          # Step 3: LBL-*
    └── OrderQA.jsx             # Step 4: QA-*
```

---

## Order Dashboard (DSH-\*)

**Route:** `/order`

The Order Dashboard is the default landing page when clicking "Add Order" in the
sidebar.

### Requirements

| ID    | Priority | Description                                                                                    |
| ----- | -------- | ---------------------------------------------------------------------------------------------- |
| DSH-1 | P0       | Default filter: "My In-Progress Orders" (user's orders not through QA)                         |
| DSH-2 | P0       | Search bar: patient name, lab number, national ID, referring lab number                        |
| DSH-3 | P0       | "Include external sources" checkbox: shows incoming EMR/referral orders (purple left border)   |
| DSH-4 | P0       | Table columns: Lab No, Patient/Subject, Facility, Priority, Current Step, Last Updated, Action |
| DSH-5 | P0       | "+ New Order" button navigates to Step 1 blank form                                            |
| DSH-6 | P0       | Barcode scan/lab number quick-lookup routes to order's current step                            |
| DSH-7 | P1       | Filter dropdowns: Status, date range, Priority                                                 |
| DSH-8 | P0       | Returned-from-QA orders: yellow row, "Returned from QA" status, "Fix Issue" button             |
| DSH-9 | P1       | Pagination: 25/50/100 items per page (default 100)                                             |

### Implementation

**Component:** `OrderDashboard.jsx`

```jsx
// Key features to implement:
// 1. DataTable with columns per DSH-4
// 2. Search bar (DSH-2) with debounced search
// 3. "Include external sources" toggle (DSH-3)
// 4. Progress bar in "Current Step" column showing step status:
//    - Green: Complete
//    - Blue: In-progress
//    - Yellow: Awaiting
//    - Red: Issue (returned from QA)
//    - Gray: Pending
// 5. Action buttons: "Continue" | "Accept" | "Fix Issue"
// 6. Filter dropdowns (DSH-7)
// 7. Pagination controls (DSH-9)
```

**Progress Bar Visual States:**

| State       | Color  | Meaning               |
| ----------- | ------ | --------------------- |
| Complete    | Green  | Step finished         |
| In-progress | Blue   | Currently active step |
| Awaiting    | Yellow | Waiting for action    |
| Issue       | Red    | Returned from QA      |
| Pending     | Gray   | Not yet started       |

---

## Step 1: Enter Order (ORD-\*) — Clinical Workflow

**Route:** `/order/enter`

This step captures patient/subject information, requester details, and test
selection.

> **UI Reference:** See `ordering-screenshots/` folder for visual mockups

### Page Header

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Test Request                                                               │
│  ┌──────────────────────────────────────────────────────────┐ ┌──────────┐  │
│  │ 📊 Scan or enter Lab Number to look up an existing order │ │  Search  │  │
│  └──────────────────────────────────────────────────────────┘ └──────────┘  │
│                                               or start a new order below    │
│                                                                             │
│    ● Enter Order  ──────  ○ Collect Sample  ──────  ○ Label & Store  ────── │
│         1                      2                        3                   │
│                                                      ○ QA Review            │
│                                                           4                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Section 1: Lab Number

```
┌─ Lab Number ────────────────────────────────────────────────────────────────┐
│  Lab Number *                                                               │
│  ┌────────────────────────────────────────────────────┐                     │
│  │ 26-03-003847                                       │  Generate           │
│  └────────────────────────────────────────────────────┘                     │
│  Auto-generated per existing lab number rules. Assigned here to enable      │
│  tracking across all steps.                                                 │
│                                                                             │
│  🖨️ Print Labels  [collapsed by default]                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Lab number field with "Generate" link to auto-populate
- Helper text explains auto-generation rules
- Print Labels section collapsed (accordion)

### Section 2: Sample Category Toggle

```
┌─ Sample Category ───────────────────────────────────────────────────────────┐
│  ┌─────────────┐ ┌─────────────────────┐                                    │
│  │  Clinical   │ │ Environmental/Other │                                    │
│  └─────────────┘ └─────────────────────┘                                    │
│  This toggle appears only when the lab unit is configured for "Both"        │
│  workflow types. If the lab unit supports only one workflow type            │
│  (e.g., Clinical only), that type is used automatically and this toggle     │
│  is hidden.                                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Carbon `ContentSwitcher` component
- Only visible when lab unit config = "Both"
- Default selection based on lab unit default

### Section 3: Patient Search (Clinical)

```
┌─ Patient ───────────────────────────────────────────────────────────────────┐
│  ┌───────────────────────┐  ┌─────────────┐                                 │
│  │  Search for Patient   │  │ New Patient │                                 │
│  └───────────────────────┘  └─────────────┘                                 │
│                                                                             │
│  Patient Id                          Previous Lab Number                    │
│  ┌─────────────────────────┐         ┌─────────────────────────┐            │
│  │ Enter Patient Id        │         │ Enter Previous Lab Number│            │
│  └─────────────────────────┘         └─────────────────────────┘            │
│                                                                             │
│  Last Name                           First Name                             │
│  ┌─────────────────────────┐         ┌─────────────────────────┐            │
│  │ Enter Patient's Last... │         │ Enter Patient's First...│            │
│  └─────────────────────────┘         └─────────────────────────┘            │
│                                                                             │
│  Date of Birth                       Gender                                 │
│  ┌─────────────────────────┐         ○ Male  ○ Female                       │
│  │ dd/mm/yyyy           📅 │                                                │
│  └─────────────────────────┘                                                │
│                                                                             │
│  ┌──────────┐  ┌──────────────────┐                                         │
│  │  Search  │  │ External Search  │                                         │
│  └──────────┘  └──────────────────┘                                         │
│                                                                             │
│  Patient Results                                                            │
│  ┌───────────┬────────────┬────────┬──────────────┬───────────┬─────────┬──┐│
│  │ Last Name │ First Name │ Gender │ Date of Birth│ Unique ID │ Nat. ID │  ││
│  ├───────────┼────────────┼────────┼──────────────┼───────────┼─────────┼──┤│
│  │           │            │        │    0-0 of 0 items        │         │  ││
│  └───────────┴────────────┴────────┴──────────────┴───────────┴─────────┴──┘│
│  Items per page: [100 ▼]                                        1 of 1 page │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Two tabs: "Search for Patient" (default) and "New Patient"
- Search fields: Patient Id, Previous Lab Number, Last Name, First Name, DOB,
  Gender
- "Search" button queries local database
- "External Search" queries Client Registry (CR)
- Results table with pagination (100 items default)
- Each row has "Select" button
- Selected patient shows in summary card below

### Section 4: Program Selection

```
┌─ Program ───────────────────────────────────────────────────────────────────┐
│  Program                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐            │
│  │ VL Program                                                 ▼│            │
│  └─────────────────────────────────────────────────────────────┘            │
│  Type to filter or select from the list. Selecting a program displays      │
│  its specific Additional Order Information fields below.                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Carbon `ComboBox` with typeahead filtering
- Program list from `/rest/program/list`
- Selecting program triggers dynamic field loading

### Section 5: Additional Order Information (Program-Specific)

When "VL Program" is selected:

```
┌─ Additional Order Information — VL Program ─────────────────────────────────┐
│  These fields are specific to the selected program and provide additional   │
│  context needed for this workflow.                                          │
│                                                                             │
│  ARV Regimen                Duration on ARV (months)  Indication for VL Test│
│  ┌───────────────────┐      ┌───────────────────┐     ┌───────────────────┐ │
│  │ Select Regimen... ▼│      │ Enter months...   │     │ Select...        ▼│ │
│  └───────────────────┘      └───────────────────┘     └───────────────────┘ │
│                                                                             │
│  Pregnancy / Breastfeeding Status         Date of Last VL Result            │
│  ┌─────────────────────────────┐          ┌─────────────────────────┐       │
│  │ Not Applicable             ▼│          │ dd/mm/yyyy           📅 │       │
│  └─────────────────────────────┘          └─────────────────────────┘       │
│                                                                             │
│  Last VL Result (copies/mL)                                                 │
│  ┌─────────────────────────────┐                                            │
│  │ e.g., 150                   │                                            │
│  └─────────────────────────────┘                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Fields dynamically loaded based on selected program
- VL Program fields: ARV Regimen, Duration on ARV, Indication for VL Test,
  Pregnancy/Breastfeeding Status, Date of Last VL Result, Last VL Result
- Other programs (EID, TB, etc.) have different field sets
- Use existing `OrderEntryAdditionalQuestions` component pattern

### Section 6: Clinical Information

```
┌─ Clinical Information ──────────────────────────────────────────────────────┐
│  Provisional Diagnosis                    Payment Status                    │
│  ┌─────────────────────────────┐          ┌─────────────────────────┐       │
│  │ Search ICD-10 codes...      │          │ Select...              ▼│       │
│  └─────────────────────────────┘          └─────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Provisional Diagnosis: ICD-10/ICD-11 typeahead search
- Payment Status: Dropdown (e.g., Insurance, Cash, Free, etc.)

### Section 7: Requester / Ordering Provider

```
┌─ Requester / Ordering Provider ─────────────────────────────────────────────┐
│                                                                             │
│  Site Search                                                                │
│  Site Name *              Department / Ward / Unit        Priority          │
│  ┌───────────────────┐    ┌────────────────────────┐     ┌────────────────┐ │
│  │ Central           │    │ Select facility first..│     │ Urgent        ▼│ │
│  └───────────────────┘    └────────────────────────┘     └────────────────┘ │
│                           Available after selecting a                       │
│  ┌──────────┐ ┌─────────┐ facility with subunits                            │
│  │  Search  │ │  Clear  │                                                   │
│  └──────────┘ └─────────┘                                                   │
│                                                                             │
│  2 results found for "Central":                                             │
│  ┌───────────────────────┬──────────┬──────────┬────────┐                   │
│  │ Site Name             │ Location │ Type     │ Select │                   │
│  ├───────────────────────┼──────────┼──────────┼────────┤                   │
│  │ Central Hospital      │ Kampala  │ Hospital │[Select]│                   │
│  │ Central Clinic Nakasero│ Nakasero│ Clinic   │[Select]│                   │
│  └───────────────────────┴──────────┴──────────┴────────┘                   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ ✓ Central Hospital                                      Selected  Clear ││
│  │   Location: Kampala · Type: Hospital                                    ││
│  │   Department / Ward / Unit: [Inpatient Ward ▼]  6 subunits available    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                             │
│  Provider Search                                                            │
│  Provider Last Name        Provider First Name        Provider Phone        │
│  ┌───────────────────┐    ┌───────────────────┐      ┌───────────────────┐  │
│  │ Smith             │    │ Enter first name..│      │ +1 (555) 000-0000 │  │
│  └───────────────────┘    └───────────────────┘      └───────────────────┘  │
│                                                                             │
│  ┌──────────┐ ┌─────────┐                                                   │
│  │  Search  │ │  Clear  │                                                   │
│  └──────────┘ └─────────┘                                                   │
│                                                                             │
│  2 results found for "Smith":                                               │
│  ┌───────────────┬────────────┬─────────────────┬─────────────────┬────────┐│
│  │ Last Name     │ First Name │ Phone           │ Source          │ Select ││
│  ├───────────────┼────────────┼─────────────────┼─────────────────┼────────┤│
│  │ Smith         │ Dr. Robert │ +256 700 555 001│ Local           │[Select]││
│  │ Smith         │ Dr. Rebecca│ +256 700 555 042│ Provider Registry│[Select]││
│  └───────────────┴────────────┴─────────────────┴─────────────────┴────────┘│
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ ✓ Dr. Robert Smith                                      Selected  Clear ││
│  │   Phone: +256 700 555 001 · Source: Local                               ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  Enter part of a last name, first name, or phone and press Search.         │
│  Provider search queries the provider registry if configured, otherwise    │
│  searches previously entered providers. Environmental requestors use the   │
│  same search mechanism.                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- **Site Search:** Site Name field + Search button
- **Department/Ward/Unit:** Disabled until facility selected; populates from
  facility subunits
- **Priority:** Dropdown (Routine, Urgent, STAT)
- **Selected Site Card:** Shows after selection with department dropdown
- **Provider Search:** Last Name, First Name, Phone fields + Search button
- **Provider Results:** Shows Source column (Local vs Provider Registry)
- **Selected Provider Card:** Shows after selection

### Section 8: Sample & Test Selection

```
┌─ Sample ────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ ⓘ Sample and test selection is optional at this step. Tests and sample ││
│  │   type can be specified later during collection.                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                             │
│  Sample 1                                              Remove Sample        │
│  Sample Type                          Filter by Lab Unit                    │
│  ┌─────────────────────────────┐      ┌─────────────────────────────┐       │
│  │ Plasma                     ▼│      │ All Lab Units              ▼│       │
│  └─────────────────────────────┘      └─────────────────────────────┘       │
│                                                                             │
│  Order Panels                                                               │
│  ┌──────────────────┐                                                       │
│  │ Serologie VIH  ✕ │                                                       │
│  └──────────────────┘                                                       │
│  🔍 Choose Available panel                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ ☐ Bilan Biochimique                                                     ││
│  │ ☑ Serologie VIH                                                         ││
│  │ ☐ Complete Blood Count                                                  ││
│  │ ☐ Basic Metabolic Panel                                                 ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                             │
│  Order Tests                                                                │
│  ┌──────────────────────┐ ┌───────────────────┐                             │
│  │ COVID-19 ANTIBODY IgG ✕│ │ Western blot HIV ✕│                             │
│  └──────────────────────┘ └───────────────────┘                             │
│  🔍 Choose Available Test                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ ☑ COVID-19 ANTIBODY IgG                                                 ││
│  │ ☐ COVID-19 ANTIBODY IgM                                                 ││
│  │ ☐ DENGUE PCR                                                            ││
│  │ ☐ HEPATITIS B VIRAL LOAD                                                ││
│  │ ☐ HEPATITIS C VIRAL LOAD                                                ││
│  │ ☐ HIV VIRAL LOAD                                                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  Showing 1-11 of 47 tests                          ◀ 1  2  3  4  5  ▶       │
│                                                                             │
│  ☐ Refer test to a reference lab                                           │
│                                                                             │
│  ┌────────────────┐                                                         │
│  │  Add Sample +  │                                                         │
│  └────────────────┘                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- **Sample Type:** Carbon `Dropdown` with sample types from test catalog
- **Filter by Lab Unit:** Dropdown to filter tests/panels by lab unit
- **Order Panels:** Selected panels shown as tags; checkbox list below
- **Order Tests:** Selected tests shown as tags; paginated checkbox list
- **Pagination:** Shows "1-11 of 47 tests" with page controls
- **Refer to Reference Lab:** Checkbox per sample
- **Add Sample:** Button to add another sample card

### Footer: Auto-Save & Navigation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ● Auto-saved as draft at 10:32 AM     Changes are saved automatically     │
│                                                         every 30 seconds    │
│                                                                             │
│                            ┌───────────────┐  ┌──────────┐  ┌─────────────┐ │
│                            │ Save as Draft │  │   Save   │  │ Save & Next │ │
│                            └───────────────┘  └──────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**

- Auto-save indicator with timestamp
- Three buttons: "Save as Draft", "Save", "Save & Next"
- "Save & Next" navigates to Step 2 (Collect Sample)

---

### Requirements Summary

| ID     | Priority | Description                                                                                         |
| ------ | -------- | --------------------------------------------------------------------------------------------------- |
| ORD-1  | P0       | Lab number auto-generated or manually entered; displayed prominently                                |
| ORD-1a | P0       | Print Labels section (collapsed): Order, Sample, Slide, Block, Freezer labels with configurable qty |
| ORD-1b | P0       | Capture requester, site, priority, subject info; tests/panels optional at this step                 |
| ORD-2  | P0       | Clinical: patient search (local + Client Registry); Environmental: location hierarchy, GPS          |
| ORD-3  | P0       | Lab unit 'Both' mode: sample category toggle (Clinical/Environmental)                               |
| ORD-4  | P1       | Save as Draft; drafts appear in 'My Drafts' list                                                    |
| ORD-5  | P0       | Use existing FHIR R4 order API endpoint; returns order ID on creation                               |
| ORD-6  | P1       | Clinical diagnosis field: ICD-10/ICD-11 typeahead lookup                                            |
| ORD-7  | P0       | Test/panel selection: paginated lists, filters by lab unit + sample type                            |
| ORD-8  | P1       | Provider search: inline disambiguation table if multiple matches                                    |
| ORD-8a | P0       | Department/Ward/Unit field: disabled until facility selected                                        |
| ORD-9  | P1       | Selected patient summary card: photo, demographics, identifiers                                     |
| ORD-10 | P0       | Program field: typeahead ComboBox; program-specific additional fields display dynamically           |
| ORD-11 | P0       | New Patient form: matches existing Add/Modify Patient form                                          |

### Unified Search Pattern (XC-2)

All search fields (Patient, Provider, Site) follow this pattern:

1. **Search Fields:** Relevant identifiers (ID, name, phone)
2. **Search Button:** Triggers async query
3. **Clear Button:** Resets search fields
4. **Inline Results Table:** Shows matches with metadata columns
5. **Select Button per Row:** User selects; confirmation inline
6. **Selected Entity Card:** Read-only display with "Selected" badge and "Clear"
   link

### Lab Unit Workflow Type (CFG-\*)

| Type          | Shows                                         | Hides                         |
| ------------- | --------------------------------------------- | ----------------------------- |
| Clinical      | Patient demo, diagnosis, provider, insurance  | Environmental fields          |
| Environmental | Location hierarchy, GPS, site description     | Patient/clinical fields       |
| Both          | All fields; sample category toggle per sample | Nothing (contextual collapse) |

---

## Step 2: Collect Sample (COL-\*)

**Route:** `/order/collect`

This step handles sample collection, including type selection, timing,
conditions, and NCE reporting.

### Requirements

| ID     | Priority | Description                                                                                          |
| ------ | -------- | ---------------------------------------------------------------------------------------------------- |
| COL-1  | P0       | Requested Tests table: panels as group headers; '+ [Type] (all)' buttons; test checkboxes            |
| COL-2  | P0       | Sample card: type dropdown, quantity (value + UOM), conditions; Collection Data; Received at Lab     |
| COL-3  | P0       | Environmental: GPS capture, environmental conditions                                                 |
| COL-4  | P0       | NCE reporting via collapsed inline section per sample card                                           |
| COL-5  | P1       | Multiple samples per order with independent tracking                                                 |
| COL-6  | P0       | Downloadable CSV templates: Standard (flat), 10×10 Box (grid), 96-Well Plate                         |
| COL-7  | P0       | CSV upload: drag-drop or file picker (max 5MB); preview with validation                              |
| COL-8  | P0       | CSV preview summary: counts of valid/warning/error; 'Import N Valid' button                          |
| COL-9  | P1       | Imported CSV samples treated identically to manual entries                                           |
| COL-10 | P2       | CSV position mapping (A1-H12, 1-1 to 10-10) saved to sample record                                   |
| COL-11 | P0       | Sample type button click: no match → create; match exists → popover with options                     |
| COL-12 | P0       | Print additional labels: per-sample 'Print Labels' button + section-level 'Print More Sample Labels' |

### Implementation

**Component:** `OrderCollect.jsx`

```jsx
// Layout Structure:
// 1. Requested Tests Table (COL-1)
//    - Panels as group headers with '+ [Type] (all)' buttons
//    - Individual test rows with checkboxes
//    - Click behavior per COL-11
//
// 2. Samples Section
//    - Sample Cards (one per sample)
//      - Sample type dropdown
//      - Quantity (value + UOM from catalog)
//      - Collection conditions
//      - Collection Data (date/time/collector) - collapsible
//      - Received at Lab (date/time) - auto-populate, editable
//      - Tests assigned to this sample
//      - NCE Section (COL-4) - collapsed
//      - Print Labels button (COL-12)
//
// 3. Add Sample Button
//    - Creates new sample card
//
// 4. CSV Import Section (COL-6 to COL-10)
//    - Download template buttons (Standard, 10×10, 96-Well)
//    - Drag-drop zone / file picker
//    - Preview table with validation indicators
//    - Summary counts + Import button
//
// 5. Environmental Fields (if applicable)
//    - GPS capture (manual or geolocation API)
//    - Environmental conditions
//
// 6. SaveNavigationButtons
```

### Sample Type Click Behavior (COL-11)

```
User clicks sample type button:
├── No existing sample of this type
│   └── Create new sample directly
└── Existing sample(s) of this type
    └── Show inline popover:
        ├── List existing samples with "Add to this sample" option
        └── "New [Type] (separate draw)" option
```

### NCE (Non-Conforming Event) Section (COL-4)

Collapsed inline section per sample card:

| Field         | Type     | Required |
| ------------- | -------- | -------- |
| NCE Type      | Dropdown | Yes      |
| Severity      | Dropdown | Yes      |
| Description   | TextArea | Yes      |
| Reject Sample | Checkbox | No       |

---

## Step 3: Label & Store (LBL-\*)

**Route:** `/order/label`

This step handles lab number assignment, barcode/label generation, and storage
location assignment.

### Requirements

| ID    | Priority | Description                                                                       |
| ----- | -------- | --------------------------------------------------------------------------------- |
| LBL-1 | P0       | Display lab number (read-only); collected samples listed                          |
| LBL-2 | P0       | Print Labels section: Order, Sample, Slide, Block, Freezer; adjustable quantities |
| LBL-3 | P0       | Storage assignment inline (not modal); auto-expanded interface                    |
| LBL-4 | P2       | Position field validates against storage location configuration                   |

### Implementation

**Component:** `OrderLabel.jsx`

```jsx
// Layout Structure:
// 1. Lab Number Display (LBL-1)
//    - Read-only, prominently displayed
//    - Barcode visualization
//
// 2. Collected Samples List
//    - Table showing all samples from Step 2
//    - Sample type, quantity, collection info
//
// 3. Print Labels Section (LBL-2)
//    - Label type cards:
//      - Order Label
//      - Sample Label (per specimen)
//      - Slide Label
//      - Block Label
//      - Freezer Label
//    - Quantity inputs per type
//    - Individual Print + Bulk Print All buttons
//
// 4. Storage Assignment Section (LBL-3)
//    For each sample:
//    - Sample info card (read-only summary)
//    - Quick barcode scan field
//    - Location search with "Location +" button
//    - Position field (optional): A5, 1-1, RED-12
//    - Condition notes (optional)
//
// 5. SaveNavigationButtons
```

### Position Field Format (LBL-4)

| Format      | Example    | Use Case        |
| ----------- | ---------- | --------------- |
| Well plates | A5, B12    | 96-well plates  |
| Grid boxes  | 1-1, 10-10 | 10×10 boxes     |
| Named       | RED-12     | Named positions |

---

## Step 4: QA Review (QA-\*)

**Route:** `/order/qa`

Final quality assurance review before order submission.

### Requirements

| ID   | Priority | Description                                                                          |
| ---- | -------- | ------------------------------------------------------------------------------------ |
| QA-1 | P0       | Completeness dashboard: green/yellow/red indicators per step per sample              |
| QA-2 | P0       | Sample review table: type, tests, collection status, NCE reports, action buttons     |
| QA-3 | P0       | 'Report NCE' collapsed section: scope, category, type, severity, description, reject |
| QA-4 | P0       | Auto-reject order if ALL samples rejected                                            |
| QA-5 | P0       | Approve or reject back to specific step via dropdown                                 |
| QA-6 | P1       | Full audit trail: all actions with timestamps, actor IDs; filterable                 |

### Implementation

**Component:** `OrderQA.jsx`

```jsx
// Layout Structure:
// 1. Completeness Dashboard (QA-1)
//    - Grid showing status per step per sample
//    - Color indicators:
//      - Green: Complete/valid
//      - Yellow: Warning/partial
//      - Red: Error/missing required
//
// 2. Order Summary Section
//    - Accordions for Patient, Samples, Order Details
//    - Read-only display of all entered data
//
// 3. Sample Review Table (QA-2)
//    Columns:
//    - Sample Type
//    - Assigned Tests
//    - Collection Status
//    - NCE Reports (if any)
//    - Actions: "Report NCE" button
//
// 4. Report NCE Section (QA-3) - Collapsed
//    - Scope: Specific Sample | Entire Order
//    - Category: Pre-analytical | Analytical | Post-analytical | Documentation | Safety
//    - Type dropdown
//    - Severity dropdown
//    - Description textarea
//    - Reject checkbox
//
// 5. QA Checklist
//    - Patient info verified
//    - Samples verified
//    - Labels verified
//    - Storage verified
//
// 6. Action Buttons (QA-5)
//    - "Approve for Testing" (primary)
//    - "Reject to Step" dropdown:
//      - Enter Order
//      - Collect Sample
//      - Label & Store
//
// 7. Audit Trail Section (QA-6) - Collapsible
//    - Filterable table of all actions
//    - Columns: Timestamp, Actor, Action, Step, Details
```

### Reject Workflow (QA-5)

When QA rejects an order:

1. User selects target step from dropdown
2. System marks order as "Returned from QA"
3. Order appears in target step's queue with:
   - Yellow row highlight (DSH-8)
   - "Returned from QA" status indicator
   - "Fix Issue" action button

---

## Edit Order Workflow (EDT-\*)

Editing existing orders follows specific rules to protect data integrity.

### Requirements

| ID    | Priority | Description                                                                          |
| ----- | -------- | ------------------------------------------------------------------------------------ |
| EDT-1 | P0       | Loaded orders render read-only by default; 'Edit' button enables modification        |
| EDT-2 | P0       | Test status indicators: Results Entered (blue), Validated (green), No Results (gray) |
| EDT-3 | P0       | Tests with results: Cancel disabled for non-admin (tooltip: 'Admin role required')   |
| EDT-4 | P0       | Admin cancelling test with results: mandatory NCE form required                      |
| EDT-5 | P1       | Tests without results: any edit-permitted user can cancel                            |
| EDT-6 | P0       | Edit Order uses same 4-step screens with existing data pre-populated                 |

### Implementation

```jsx
// Read-Only Mode (EDT-1):
// - All form fields disabled/greyed
// - "Edit" button in header
// - Clicking "Edit" enables fields

// Test Status Indicators (EDT-2):
const TEST_STATUS = {
  RESULTS_ENTERED: { label: "Results Entered", color: "blue" },
  VALIDATED: { label: "Validated", color: "green", icon: "✓" },
  NO_RESULTS: { label: "No Results", color: "gray", icon: "-" },
  CANCELLED: {
    label: "Cancelled",
    color: "red",
    icon: "✗",
    strikethrough: true,
  },
};

// Cancel Button Logic (EDT-3, EDT-4, EDT-5):
// if (test.hasResults && !user.isAdmin) {
//   disabled + tooltip "Admin role required"
// } else if (test.hasResults && user.isAdmin) {
//   show mandatory NCE form before allowing cancel
// } else {
//   allow cancel, record in audit trail
// }
```

---

## External/Incoming Orders (INC-\*)

External orders from EMR or referrals are accessed via the dashboard toggle.

### Requirements

| ID    | Priority | Description                                                                          |
| ----- | -------- | ------------------------------------------------------------------------------------ |
| INC-1 | P0       | External orders via dashboard toggle (not separate screen)                           |
| INC-2 | P0       | External order row: Referring Lab No, Patient, Facility, Source, "Accept" button     |
| INC-3 | P0       | "Accept" → Step 1 with external order data pre-populated                             |
| INC-4 | P0       | Lab number options: scan barcode, enter manually, generate new, use referring number |
| INC-5 | P1       | Pre-populated data indicators ("Pre-filled from [source]")                           |
| INC-6 | P1       | Dashboard toggle shows count badge of pending external orders                        |

### Visual Treatment

```scss
// External order row styling
tr.external-order {
  border-left: 4px solid #8a3ffc; // Purple left border

  .external-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    color: #8a3ffc;
    font-size: 0.75rem;
    font-weight: 600;
  }
}
```

### Accept Workflow (INC-3, INC-4)

1. User clicks "Accept" on external order
2. Navigate to Step 1 with pre-populated data
3. Lab number options dialog:
   - Scan barcode
   - Enter manually
   - Generate new (per lab rules)
   - Use Current (keep referring lab number)
4. Referring lab number stored as reference field

---

## Cross-Cutting Concerns (XC-\*)

### Auto-Save (XC-1)

| Feature          | Implementation                             |
| ---------------- | ------------------------------------------ |
| Interval         | Every 30 seconds on dirty forms            |
| Status indicator | "Saved" / "Saving..." / "Unsaved changes"  |
| Browser warning  | `beforeunload` event listener when isDirty |
| Non-blocking     | User can continue editing during save      |

```javascript
// Auto-save implementation in OrderContext.js
useEffect(() => {
  if (isDirty && !isReadOnly) {
    const timer = setInterval(() => {
      if (isDirty && !isSubmitting) {
        saveOrder(true); // silent save
      }
    }, 30000);
    return () => clearInterval(timer);
  }
}, [isDirty, isReadOnly, isSubmitting]);
```

### WCAG 2.1 AA Compliance (XC-3 to XC-6)

| Requirement         | Implementation                                   |
| ------------------- | ------------------------------------------------ |
| Contrast ratio      | Body text ≥ 4.5:1; helper text uses #6f6f6f min  |
| Touch targets       | All buttons min-height 32px                      |
| Focus indicators    | Visible focus ring (Carbon blue-60)              |
| Keyboard navigation | All elements via Tab; Enter/Space activates      |
| ARIA attributes     | Progress stepper, collapsibles, popovers labeled |

```scss
// WCAG 2.1 AA compliant styles
.cds--btn:focus-visible,
.cds--search-input:focus-visible {
  outline: 2px solid var(--cds-focus);
  outline-offset: 2px;
}

// Touch target compliance
.cds--btn,
.cds--checkbox-wrapper,
.cds--search-input {
  min-height: 32px;
}

// Disabled states (no opacity)
.disabled-field {
  text-decoration: line-through;
  // or
  &::after {
    content: " (unavailable)";
  }
}
```

---

## API Endpoints

### Required REST Endpoints

| Endpoint                             | Method | Purpose                                  |
| ------------------------------------ | ------ | ---------------------------------------- |
| `/rest/order`                        | POST   | Create order; returns lab number         |
| `/rest/order/{orderId}`              | GET    | Retrieve order data                      |
| `/rest/order/{orderId}`              | PUT    | Update order state per step              |
| `/rest/order/dashboard`              | GET    | Fetch in-progress orders for user        |
| `/rest/order/search`                 | GET    | Search by lab number (barcode scan)      |
| `/rest/order/external/incoming`      | GET    | Fetch incoming external orders           |
| `/rest/order/external/accept`        | POST   | Accept external order; assign lab number |
| `/rest/order/{orderId}/audit`        | GET    | Fetch order audit trail                  |
| `/rest/order/{orderId}/nce`          | POST   | Report Non-Conforming Event              |
| `/rest/order/{orderId}/csv/template` | GET    | Download CSV template                    |
| `/rest/order/{orderId}/csv/import`   | POST   | Upload & validate CSV samples            |
| `/rest/labNumber/generate`           | GET    | Auto-generate lab number                 |
| `/rest/provider/search`              | GET    | Search providers                         |
| `/rest/site/search`                  | GET    | Search facilities/sites                  |
| `/rest/location/hierarchy`           | GET    | Fetch Region/District/Town-Village       |
| `/rest/test/catalog`                 | GET    | Fetch available tests + sample types     |
| `/rest/program/list`                 | GET    | Fetch programs + additional fields       |
| `/rest/patient/search`               | GET    | Search local/Client Registry             |
| `/rest/labUnit/config`               | GET    | Fetch lab unit workflow type + config    |
| `/rest/storage/location/search`      | GET    | Search storage locations                 |
| `/rest/storage/assign`               | POST   | Assign sample to storage location        |
| `/rest/label/print`                  | POST   | Trigger label print                      |

---

## Implementation Phases

### Phase 1: Foundation (Weeks 1-4)

**Goal:** Core infrastructure and Step 1

| Task                             | Status  | Files                                    |
| -------------------------------- | ------- | ---------------------------------------- |
| OrderContext shared state        | ✅ Done | `OrderContext.js`                        |
| Routing infrastructure           | ✅ Done | `App.js`, Liquibase menus                |
| Order Dashboard (DSH-1 to DSH-9) | ✅ Done | `OrderDashboard.jsx`                     |
| Progress stepper (NAV-3)         | ✅ Done | `OrderStepper.jsx`                       |
| Barcode scanner (NAV-6, NAV-8)   | ✅ Done | `BarcodeScannerBar.jsx`                  |
| Order context card (NAV-5)       | ✅ Done | `OrderContextCard.jsx`                   |
| Save & Next navigation (NAV-4)   | ✅ Done | `SaveNavigationButtons.jsx`              |
| Step 1 - Enter Order (clinical)  | 🔲 Todo | `OrderEnter.jsx` - integrate PatientInfo |
| Lab unit workflow type config    | 🔲 Todo | Admin settings, CFG-1 to CFG-3           |

### Phase 2: Collection & Storage (Weeks 5-8)

**Goal:** Steps 2 and 3 with environmental support

| Task                            | Status  | Files                                    |
| ------------------------------- | ------- | ---------------------------------------- |
| Step 2 - Collect Sample         | 🔲 Todo | `OrderCollect.jsx` - integrate AddSample |
| Sample cards with NCE reporting | 🔲 Todo | COL-2, COL-4                             |
| CSV import for batch samples    | 🔲 Todo | COL-6 to COL-10                          |
| Environmental workflow fields   | 🔲 Todo | ORD-2, COL-3                             |
| Step 3 - Label & Store          | 🔲 Todo | `OrderLabel.jsx`                         |
| Print labels section            | 🔲 Todo | LBL-2                                    |
| Storage assignment inline       | 🔲 Todo | LBL-3, LBL-4                             |

### Phase 3: QA & Polish (Weeks 9-12)

**Goal:** Step 4, edit workflow, and external orders

| Task                                 | Status   | Files                                        |
| ------------------------------------ | -------- | -------------------------------------------- |
| Step 4 - QA Review                   | ✅ Basic | `OrderQA.jsx` - needs completeness dashboard |
| Completeness dashboard (QA-1)        | 🔲 Todo  | Green/yellow/red indicators                  |
| NCE reporting (QA-3)                 | 🔲 Todo  | Scope, category, severity, description       |
| Reject to step workflow (QA-5)       | 🔲 Todo  | Target step dropdown                         |
| Audit trail (QA-6)                   | 🔲 Todo  | Filterable audit table                       |
| Edit order workflow (EDT-1 to EDT-6) | 🔲 Todo  | Test status indicators, admin NCE            |
| External orders (INC-1 to INC-6)     | 🔲 Todo  | Dashboard toggle, accept workflow            |
| FHIR API integration testing         | 🔲 Todo  | ORD-5                                        |

---

## Verification Checklist

### Navigation (NAV-\*)

- [ ] Sidebar shows "Add Order" with 4 expandable children
- [ ] Each step independently routable
- [ ] Progress stepper shows correct states
- [ ] Save & Next advances; Save stays
- [ ] Order context card visible on all steps
- [ ] Barcode scan loads order in read-only mode
- [ ] Inline feedback within 500ms, auto-dismiss after 5s

### Dashboard (DSH-\*)

- [ ] Default filter shows user's in-progress orders
- [ ] Search by patient name, lab number, national ID
- [ ] "Include external sources" shows EMR/referral orders
- [ ] Table columns match spec
- [ ] Progress bar shows step status colors
- [ ] Returned-from-QA orders have yellow highlight
- [ ] Pagination works (25/50/100)

### Order Entry (ORD-\*)

- [ ] Lab number auto-generates or manual entry
- [ ] Print Labels section collapsed by default
- [ ] Patient search works (local + Client Registry)
- [ ] Environmental location hierarchy works
- [ ] Program selection shows dynamic fields
- [ ] Test/panel selection paginated

### Cross-Cutting (XC-\*)

- [ ] Auto-save every 30 seconds
- [ ] Save status indicator visible
- [ ] Browser warning on navigation with unsaved changes
- [ ] All touch targets ≥ 32px
- [ ] Focus indicators visible
- [ ] Keyboard navigation works

---

## References

- [FRS v1.0 - Sample Collection Redesign](https://github.com/DIGI-UW/openelis-work/blob/main/designs/sample-collection/sample-collection-redesign.md)
- [Sample Collection Mockup](https://github.com/DIGI-UW/openelis-work/blob/main/designs/sample-collection/sample-collection-redesign-mockup.html)
- [Design Critique](https://github.com/DIGI-UW/openelis-work/blob/main/designs/sample-collection/sample-collection-design-critique.md)
