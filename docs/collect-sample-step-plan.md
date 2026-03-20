# Collect Sample Step Implementation Plan

## Overview

Based on the UI screenshots, the "Collect Sample" step (Step 2) is a
comprehensive page that allows technicians to:

1. View ordered tests/panels and assign them to samples
2. Configure collection details (date, time, collector, conditions)
3. Record when samples are received at the lab
4. Report non-conforming events (NCE)

This is significantly different from the current `OrderCollect.jsx` which simply
wraps the existing `AddSample` component.

---

## UI Analysis (from Screenshots)

### Screenshot 1: Requested Tests Section

**Header Elements:**

- Barcode scanner bar at top
- Progress stepper showing Step 2 "Collect Sample" active
- Order context card showing: Lab Number (26-03-003847), Subject (Doe, Jane),
  Tests (HIV VL, Western blot, COVID-19 IgG), Status (Collecting badge)

**Requested Tests Table:** | Column | Description | |--------|-------------| |
Test / Panel | Test or panel name with icon indicator | | Compatible Sample
Types | Tags showing valid sample types (e.g., "+ Plasma (all)", "+ DBS (all)")
| | Sample Assignment(s) | Shows which sample the test is assigned to (e.g., "✓
Plasma (Sample 1)") |

**Test Assignment Modal:**

- Title: "Assign 'HIV VIRAL LOAD' — Plasma"
- Description: "A Plasma sample already exists. Choose where to assign this
  test, or draw a new sample."
- Options:
  - Radio: "Add to Sample 1 — Plasma" (Currently has: HIV VL/DNA, LOAD, Western
    blot HIV)
  - Radio: "New Plasma sample (separate draw)" (Creates a new sample requiring
    its own collection)
- Buttons: "Assign" (primary), "Cancel" (ghost)

**Example Multi-Sample Assignment:**

- Shows how one test can be assigned to multiple samples
- Example: "✓ Plasma (Sample 1)" + "✓ Plasma (Sample 2)" = "same test on two
  separate Plasma draws"

### Screenshot 2: Samples Section

**Sample Card (repeatable):**

- Header: "Sample 1 — Plasma" with "Print Labels" button and "Remove" link
- Assigned Tests: Tags showing tests (e.g., "HIV VIRAL LOAD", "Western blot
  HIV")

**Collection Details Grid:** | Field | Type | Notes | |-------|------|-------| |
Sample Type | Dropdown | Pre-selected from assignment | | Quantity | Number
input + unit dropdown | e.g., "5 mL" (unit from test catalog) | | Collection
Conditions | Text input | e.g., "Fasting, Room temp" | | Collection Date | Date
picker | Optional - filled when specimen physically collected | | Collection
Time | Time input | Optional - filled when specimen physically collected | |
Collector | Text input | Collector ID (e.g., "COL-0042") |

**Received at Lab Section:**

- Label: "(auto-populated from server — editable)"
- Received Date: Date picker (e.g., "03/03/2026", auto-filled from server)
- Received Time: Time input (e.g., "10:15", auto-filled from server)

**NCE Link:**

- Red link: "Report Non-Conforming Event (NCE)"
- Links to NCE reporting workflow

**Footer Buttons:**

- "+ Add Another Sample" (tertiary button)
- "Print More Sample Labels" (tertiary button)
- Helper text: "Use 'Print More Sample Labels' if you draw more than expected or
  need labels for a different sample type."

---

## Component Architecture

```
OrderCollect.jsx
├── OrderWorkflowLayout
│   ├── BarcodeScannerBar
│   ├── OrderStepper (step 2 active)
│   └── OrderContextCard
├── RequestedTestsSection.jsx (NEW)
│   ├── TestsTable
│   │   ├── TestRow (with sample type tags)
│   │   └── SampleAssignmentCell
│   └── TestAssignmentModal.jsx (NEW)
├── SamplesCollectionSection.jsx (NEW)
│   ├── SampleCollectionCard.jsx (repeatable)
│   │   ├── SampleHeader (type, print labels, remove)
│   │   ├── AssignedTestsTags
│   │   ├── CollectionDetailsGrid
│   │   │   ├── SampleTypeDropdown
│   │   │   ├── QuantityInput
│   │   │   ├── CollectionConditionsInput
│   │   │   ├── CollectionDatePicker
│   │   │   ├── CollectionTimeInput
│   │   │   └── CollectorInput
│   │   ├── ReceivedAtLabSection
│   │   │   ├── ReceivedDatePicker
│   │   │   └── ReceivedTimeInput
│   │   └── NCELink
│   ├── AddSampleButton
│   └── PrintMoreLabelsButton
└── SaveNavigationButtons
```

---

## Data Model Changes

### Sample Object (Extended)

```javascript
{
  // Existing fields
  index: number,
  sampleTypeId: string,
  panels: [{ id, name, testIds }],
  tests: [{ id, name }],
  sampleRejected: boolean,
  rejectionReason: string,
  requestReferralEnabled: boolean,
  referralItems: [],

  // NEW fields for collection
  quantity: number,
  quantityUnit: string,         // "mL", "uL", etc.
  collectionConditions: string, // "Fasting, Room temp"
  collectionDate: string,       // ISO date
  collectionTime: string,       // HH:mm format
  collectorId: string,          // Collector identifier

  // NEW fields for lab receipt
  receivedDate: string,         // ISO date (auto-populated)
  receivedTime: string,         // HH:mm format (auto-populated)
  receivedBy: string,           // Who received it

  // NEW fields for NCE
  hasNCE: boolean,
  nceId: string,                // Link to NCE record
}
```

### Test Assignment Structure

```javascript
// Map test/panel to samples
testSampleAssignments: {
  [testId]: {
    testId: string,
    testName: string,
    isPanel: boolean,
    compatibleSampleTypes: [{ id, name, code }],
    assignedToSamples: [sampleIndex, ...]
  }
}
```

---

## API Endpoints

### Existing Endpoints (reuse)

- `GET /rest/user-sample-types` - Get available sample types
- `GET /rest/sample-type-tests?sampleType={id}` - Get tests for sample type

### New Endpoints Required

#### 1. Get Test Sample Type Compatibility

```
GET /rest/test-sample-types?testIds={ids}

Response:
{
  "tests": [
    {
      "testId": "123",
      "testName": "HIV VIRAL LOAD",
      "compatibleSampleTypes": [
        { "id": "1", "name": "Plasma", "code": "all" },
        { "id": "2", "name": "DBS", "code": "all" }
      ]
    }
  ]
}
```

#### 2. Get Default Quantity Units for Tests

```
GET /rest/test-quantity-units?testIds={ids}

Response:
{
  "tests": [
    {
      "testId": "123",
      "defaultQuantity": 5,
      "defaultUnit": "mL",
      "availableUnits": ["mL", "uL", "tubes"]
    }
  ]
}
```

#### 3. Get Current Server Time (for received timestamp)

```
GET /rest/server-time

Response:
{
  "date": "2026-03-03",
  "time": "10:15",
  "timezone": "Africa/Nairobi"
}
```

---

## Implementation Tasks

### Phase 1: Data Model & Context Updates

- [ ] **Task 1.1**: Update `OrderContext.js`

  - Add `testSampleAssignments` state
  - Add functions: `assignTestToSample()`, `removeTestFromSample()`,
    `createNewSampleForTest()`
  - Update sample object structure to include collection fields

- [ ] **Task 1.2**: Create `useTestAssignment` hook
  - Manage test-to-sample assignment logic
  - Handle multi-sample assignment scenarios
  - Compute compatible sample types for each test

### Phase 2: Requested Tests Section

- [ ] **Task 2.1**: Create `RequestedTestsSection.jsx`

  - Display table of ordered tests/panels from Step 1
  - Show compatible sample types as tags
  - Show current sample assignments

- [ ] **Task 2.2**: Create `TestAssignmentModal.jsx`

  - Radio buttons for existing samples of matching type
  - Option to create new sample
  - Assign/Cancel buttons

- [ ] **Task 2.3**: Implement assignment logic
  - Click on sample type tag → open modal
  - Selecting existing sample → add test to that sample
  - Selecting "New sample" → create sample with test

### Phase 3: Samples Collection Section

- [ ] **Task 3.1**: Create `SampleCollectionCard.jsx`

  - Replace current simple card with full collection form
  - Include all collection detail fields
  - Date/time pickers with proper formatting

- [ ] **Task 3.2**: Create `ReceivedAtLabSection.jsx`

  - Auto-populate from server time on load
  - Allow manual override
  - Display "auto-filled from server" helper text

- [ ] **Task 3.3**: Implement quantity with units

  - Fetch default units from test catalog
  - Number input + unit dropdown

- [ ] **Task 3.4**: Add NCE link
  - Red "Report Non-Conforming Event (NCE)" link
  - Navigate to NCE form (placeholder for now)

### Phase 4: Print Labels Integration

- [ ] **Task 4.1**: Add "Print Labels" button to sample card header

  - Trigger label print for that sample

- [ ] **Task 4.2**: Add "Print More Sample Labels" button
  - For printing additional labels
  - Helper text explaining use case

### Phase 5: Backend API

- [ ] **Task 5.1**: Create `TestSampleTypeController.java`

  - Endpoint for test-to-sample-type compatibility
  - Query test catalog for compatible sample types

- [ ] **Task 5.2**: Update `OrderWorkflowRestController.java`
  - Add server time endpoint
  - Add quantity units endpoint

### Phase 6: Styling

- [ ] **Task 6.1**: Update `order-workflow.scss`
  - Styles for RequestedTestsSection table
  - Styles for sample type tags (clickable)
  - Styles for SampleCollectionCard
  - Styles for NCE link (red)

### Phase 7: Testing

- [ ] **Task 7.1**: Unit tests for test assignment logic
- [ ] **Task 7.2**: Integration tests for collection workflow
- [ ] **Task 7.3**: E2E test: complete order → collect → verify data persists

---

## i18n Keys to Add

```json
{
  "collect.requestedTests.title": "Requested Tests",
  "collect.requestedTests.info": "Tests and panels ordered in Step 1. Click a sample type to assign. If a matching sample already exists, you choose whether to add to it or draw a new sample. If no match, a new sample is created directly. A test or panel can be assigned to multiple samples when needed.",
  "collect.table.testPanel": "Test / Panel",
  "collect.table.compatibleTypes": "Compatible Sample Types",
  "collect.table.sampleAssignments": "Sample Assignment(s)",
  "collect.assign.title": "Assign '{testName}' — {sampleType}",
  "collect.assign.description": "A {sampleType} sample already exists. Choose where to assign this test, or draw a new sample.",
  "collect.assign.addToSample": "Add to Sample {number} — {sampleType}",
  "collect.assign.newSample": "New {sampleType} sample (separate draw)",
  "collect.assign.newSampleHelp": "Creates a new sample requiring its own collection",
  "collect.assign.button": "Assign",
  "collect.noSampleYet": "No sample yet",
  "collect.multiSampleExample.title": "Example: Multi-Sample Assignment",
  "collect.multiSampleExample.description": "A test or panel can be assigned to multiple samples. The 'Sample Assignment(s)' column reflects all assignments.",
  "collect.samples.title": "Samples",
  "collect.sample.header": "Sample {number} — {sampleType}",
  "collect.sample.assignedTests": "Assigned Tests:",
  "collect.sample.quantity": "Quantity",
  "collect.sample.collectionConditions": "Collection Conditions",
  "collect.sample.collectionConditions.placeholder": "e.g., Fasting, Room temp",
  "collect.sample.collectionDate": "Collection Date",
  "collect.sample.collectionDate.helper": "(optional — filled when specimen is physically collected)",
  "collect.sample.collectionTime": "Collection Time",
  "collect.sample.collector": "Collector",
  "collect.sample.receivedAtLab": "Received at Lab",
  "collect.sample.receivedAtLab.helper": "(auto-populated from server — editable)",
  "collect.sample.receivedDate": "Received Date",
  "collect.sample.receivedTime": "Received Time",
  "collect.sample.nce.link": "Report Non-Conforming Event (NCE)",
  "collect.sample.printLabels": "Print Labels",
  "collect.sample.remove": "Remove",
  "collect.addSample.button": "+ Add Another Sample",
  "collect.printMoreLabels.button": "Print More Sample Labels",
  "collect.printMoreLabels.helper": "Use 'Print More Sample Labels' if you draw more than expected or need labels for a different sample type."
}
```

---

## Dependencies

- Carbon Design System components (DataTable, Modal, DatePicker, TimePicker,
  NumberInput)
- Existing OrderContext and OrderWorkflowLayout
- Backend test catalog for sample type compatibility

---

## Risks & Considerations

1. **Complexity**: This is a significant redesign of the collect step
2. **Data Migration**: Existing orders may not have collection fields populated
3. **NCE Integration**: NCE workflow may need separate planning
4. **Print Labels**: Requires integration with existing label printing system

---

## Success Criteria

1. User can see all ordered tests with compatible sample types
2. User can assign tests to existing or new samples
3. User can record collection details (date, time, collector, conditions)
4. System auto-populates received at lab timestamp
5. User can navigate to NCE reporting
6. All data persists correctly to backend
