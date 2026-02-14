# Data Model: OGC-284 Barcode Label Quantity Management

## Scope

This model captures the persisted and runtime data structures relevant to
OGC-284 and its assessment-driven remediation.

---

## Persisted Entities

## 1) SampleBarcodeInfo

**Purpose**: Stores sample-level order label quantity metadata.

| Field | Type | Required | Notes |
| ----- | ---- | -------- | ----- |
| id | Integer | Yes | PK (sequence) |
| sample_id | FK -> Sample.id | Yes | Unique per sample |
| print_order_num | Integer | No | Requested order-label quantity |
| last_updated | Timestamp | No | Audit timestamp |

**Business Rules**:

- One `SampleBarcodeInfo` per `Sample`.
- On save, upsert behavior is used (update existing, else insert).
- If quantity input is omitted, default quantity is applied before persistence.

---

## 2) SampleItemBarcodeInfo

**Purpose**: Stores sample-item-level label quantity metadata.

| Field | Type | Required | Notes |
| ----- | ---- | -------- | ----- |
| id | Integer | Yes | PK (sequence) |
| sample_item_id | FK -> SampleItem.id | Yes | Unique per sample item |
| print_specimen_num | Integer | No | Requested specimen-label quantity |
| print_block_num | Integer | No | Pathology block-label quantity |
| print_slide_num | Integer | No | Pathology slide-label quantity |
| print_freezer_num | Integer | No | Pathology freezer-label quantity |
| last_updated | Timestamp | No | Audit timestamp |

**Business Rules**:

- One `SampleItemBarcodeInfo` per `SampleItem`.
- Upsert logic matches sample-level behavior.
- Non-pathology workflows use specimen quantity; pathology workflows may populate
  additional quantities.

---

## 3) SiteInformation (labels domain subset)

**Purpose**: Stores barcode label defaults, limits, dimensions, and field
toggles under the `labels` domain.

Representative key groups:

- Quantity limits/defaults:
  - `numMaxOrderLabels`, `numMaxSpecimenLabels`, `numMaxSlideLabels`,
    `numMaxBlockLabels`, `numMaxFreezerLabels`
  - `numDefaultOrderLabels`, `numDefaultSpecimenLabels`,
    `numDefaultSlideLabels`, `numDefaultBlockLabels`,
    `numDefaultFreezerLabels`
- Dimensions:
  - `height*Labels`, `width*Labels` (by label type)
- Field toggles:
  - `orderLabel*`, `specimenLabel*`, `slideLabel*`, `blockLabel*`,
    `freezerLabel*`

**Business Rules**:

- Numeric values are parsed via safe parsing with defaults on malformed input.
- Boolean values are normalized to `true/false`.
- Runtime behavior must remain consistent with exposed admin toggles.

---

## Runtime Structures

## 4) GenericSampleOrderForm.DefaultFields

**Purpose**: Request payload for generic sample order creation and default-field
capture.

Relevant fields for OGC-284:

| Field | Type | Required | Notes |
| ----- | ---- | -------- | ----- |
| numOrderLabels | Integer | No | Defaults to 1 when omitted |
| numSpecimenLabels | Integer | No | Defaults to 1 when omitted |

---

## 5) Pathology Label Context (runtime assembly)

**Purpose**: Service-layer aggregation of pathology-related values required by
label rendering (for example specimen type).

Expected attributes:

- sample / sample item linkage context
- resolved specimen type text
- optional slide/block metadata used by configurable fields

**Design Intent**:

- Context is assembled before label creation.
- Label classes consume context and render; they do not run unscoped FHIR
  searches directly.

---

## Relationships

```text
Sample (1) ─────── (0..1) SampleBarcodeInfo
  │
  └──── (1..n) SampleItem
             │
             └── (0..1) SampleItemBarcodeInfo

SiteInformation (labels domain) supplies configuration values used by:
- Barcode configuration forms/controllers
- Label classes (render-time behavior)
- Barcode quantity defaults/limits
```

---

## State Transitions

## A) Barcode configuration lifecycle

1. GET `/rest/BarcodeConfiguration` loads values from `SiteInformation`.
2. Missing/invalid numeric values are normalized to safe defaults.
3. POST updates configuration values in labels domain via upsert semantics.

## B) Generic sample order persistence lifecycle

1. `GenericSampleOrderForm` received.
2. Sample and sample items are created.
3. Barcode info service persists:
   - sample-level order quantity
   - sample-item-level specimen quantity
4. Existing records are updated; missing records are inserted.

## C) Pathology label rendering lifecycle (target remediation)

1. Service resolves required pathology metadata once per request/work unit.
2. Label classes render fields based on configuration toggles and passed context.
3. No per-label unscoped FHIR search in rendering classes.
