# Data Model: OGC-284 Barcode Label Quantity Management

## Scope

This model captures:

- the persisted barcode quantity/configuration data already present on the
  branch,
- the runtime workflow data needed for the missing Jira/design UX,
- the separation between completed baseline persistence work and remaining
  labels/printing workflow orchestration.

---

## Persisted Entities

## 1) SampleBarcodeInfo

**Purpose**: Stores sample-level order label quantity metadata.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | Integer | Yes | PK (sequence) |
| sample_id | FK -> Sample.id | Yes | Unique per sample |
| print_order_num | Integer | No | Requested order-label quantity |
| printed_order_count | Integer | No | Cumulative printed order labels |
| last_updated | Timestamp | No | Audit timestamp |

**Business Rules**:

- One `SampleBarcodeInfo` per `Sample`.
- On save, upsert behavior is used (update existing, else insert).
- If quantity input is omitted, a default quantity is applied before
  persistence.

---

## 2) SampleItemBarcodeInfo

**Purpose**: Stores sample-item-level label quantity metadata.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | Integer | Yes | PK (sequence) |
| sample_item_id | FK -> SampleItem.id | Yes | Unique per sample item |
| print_specimen_num | Integer | No | Requested specimen-label quantity |
| print_block_num | Integer | No | Block-label quantity |
| print_slide_num | Integer | No | Slide-label quantity |
| print_freezer_num | Integer | No | Freezer-label quantity |
| printed_specimen_count | Integer | No | Cumulative printed specimen labels |
| printed_block_count | Integer | No | Cumulative printed block labels |
| printed_slide_count | Integer | No | Cumulative printed slide labels |
| printed_freezer_count | Integer | No | Cumulative printed freezer labels |
| last_updated | Timestamp | No | Audit timestamp |

**Business Rules**:

- One `SampleItemBarcodeInfo` per `SampleItem`.
- Upsert logic matches sample-level behavior.
- Workflows only populate the label-type fields applicable to that sample item.

---

## 3) SiteInformation (labels domain subset)

**Purpose**: Stores barcode label defaults, limits, dimensions, and field
toggles under the `labels` domain.

Representative key groups:

- Quantity limits/defaults:
  - `numMaxOrderLabels`, `numMaxSpecimenLabels`, `numMaxSlideLabels`,
    `numMaxBlockLabels`, `numMaxFreezerLabels`
  - `numDefaultOrderLabels`, `numDefaultSpecimenLabels`,
    `numDefaultSlideLabels`, `numDefaultBlockLabels`, `numDefaultFreezerLabels`
- Dimensions:
  - `height*Labels`, `width*Labels` (by label type)
- Field toggles:
  - `orderLabel*`, `specimenLabel*`, `slideLabel*`, `blockLabel*`,
    `freezerLabel*`

**Business Rules**:

- Numeric values are parsed via safe parsing with defaults on malformed input.
- Boolean values are normalized to `true/false`.
- Runtime labels UI and print dialog must remain consistent with these settings.

---

## Runtime Structures

## 4) Workflow Label Quantity Input

**Purpose**: Workflow-specific request payload segment used before save.

Representative fields:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| numOrderLabels | Integer | No | Defaults to configured/default fallback |
| numSpecimenLabels | Integer | No | Per-sample-item or per-sample as applicable |
| numBlockLabels | Integer | No | Only when workflow supports block labels |
| numSlideLabels | Integer | No | Only when workflow supports slide labels |
| numFreezerLabels | Integer | No | Only when workflow supports freezer labels |

**Business Rules**:

- Not every workflow exposes every field.
- Missing values fall back to configured/default quantities.
- Submitted values must respect workflow applicability and configured maximums.

---

## 5) Labels UI Row Model

**Purpose**: Canonical pre-save labels presentation model required by the
clarified spec.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| rowType | Enum(`order`,`sample`) | Yes | One order row, one row per sample |
| rowId | String | Yes | Stable UI key |
| sampleRef | Sample/Temp ID | No | Present for sample rows |
| applicableLabelTypes | Array<LabelType> | Yes | Only label types valid for that row |
| quantities | Map<LabelType,Integer> | Yes | Editable label counts |
| sourceDefaults | Map<LabelType,Integer> | Yes | Pre-populated from barcode config |
| rowTotal | Integer | Yes | Sum for this row |

**Derived Structure**:

- `LabelsSection = { orderRow, sampleRows[], runningTotal }`

**Business Rules**:

- Exactly one order row is shown.
- Exactly one row is shown per sample in the workflow.
- Running total recomputes whenever any editable quantity changes.

---

## 6) PostSavePrintDialogModel

**Purpose**: Runtime state shown after save once accession number is assigned.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| accessionNumber | String | Yes | Assigned on successful save |
| printableLabelTypes | Array<PrintableLabelOption> | Yes | Only applicable label types |
| allowDoneWithoutPrinting | Boolean | Yes | Done button is available |
| reprintContextToken | String/Object | No | Context needed for later print entry |

### PrintableLabelOption

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| labelType | Enum | Yes | Order/specimen/block/slide/freezer |
| quantity | Integer | Yes | Saved quantity for that type |
| dimensionsMm | String | Yes | Configured `H x W mm` for display |
| printUrl | String | Yes | PDF generation endpoint for this label type |

**Business Rules**:

- Dialog appears only after a successful save with accession number assigned.
- Clicking Print for a label type opens a dimension-matched PDF in a new tab.
- Done exits without printing and preserves reprint capability from Order View.

---

## 7) Pathology Label Context

**Purpose**: Service-layer aggregation of pathology-related values required by
label rendering and print orchestration.

Expected attributes:

- sample / sample item linkage context
- resolved specimen type text
- optional slide/block/freezer metadata used by configurable fields

**Design Intent**:

- Context is assembled before label creation.
- Label classes consume context and render; they do not run unscoped lookups
  directly.

---

## 8) Workflow Applicability Matrix

**Purpose**: Defines which sample-creation workflows must implement the OGC-284
labels UI and post-save print flow.

Candidate workflow families identified in the repo:

- legacy order entry / add-order flow
- generic sample order
- notebook sample order
- batch order entry
- pathology-related case/sample workflows

**Business Rules**:

- Planning must inventory and confirm every barcode-printing sample-creation
  flow.
- All confirmed in-scope workflows must converge on the same labels-section and
  post-save print semantics.

---

## Relationships

```text
Sample (1) ─────── (0..1) SampleBarcodeInfo
  │
  └──── (1..n) SampleItem
             │
             └── (0..1) SampleItemBarcodeInfo

SiteInformation (labels domain) supplies configuration values used by:
- barcode configuration forms/controllers
- labels UI row defaults/limits
- post-save print dialog defaults
- label classes (render-time behavior)
```

---

## State Transitions

## A) Barcode configuration lifecycle

1. GET `/rest/BarcodeConfiguration` loads values from `SiteInformation`.
2. Missing/invalid numeric values are normalized to safe defaults.
3. POST updates configuration values in labels domain via upsert semantics.

## B) Sample-creation labels workflow lifecycle

1. Workflow loads barcode defaults/maximums for applicable label types.
2. User reaches labels step/section.
3. UI renders one order row plus one row per sample.
4. User edits label quantities and sees running total.
5. Save request submits quantities in the workflow-specific contract.

## C) Post-save printing lifecycle

1. Save succeeds and accession number is assigned.
2. System builds `PostSavePrintDialogModel`.
3. User either:
   - selects label types to print now, or
   - clicks Done without printing.
4. If printing now, each selected label type is dispatched as a separate print
   job.
5. If deferred, later order/sample view uses saved metadata plus reprint context
   to restart printing.

## D) Label generation resilience lifecycle

1. Service resolves required context once per request/work unit.
2. Label classes render fields based on configuration toggles and passed
   context.
3. Over-max requests are blocked unless explicit `override=true` is supplied.
