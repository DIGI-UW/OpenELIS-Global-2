# Feature Specification: Barcode Label Quantity Management

**Feature Branch**: `OGC-284-barcode-label-quantity-management`  
**Created**: 2026-02-14  
**Status**: Draft  
**Input**: User description: "This is a PR for https://uwdigi.atlassian.net/browse/OGC-284. We need to generate specs properly for this feature, using the issue number as the feature id."  
**Issue**: [OGC-284](https://uwdigi.atlassian.net/browse/OGC-284)

## Overview

Improve barcode label management so laboratories can reliably configure label
quantities and persist print quantity choices at sample creation time.

This feature addresses two related operational needs:

1. Barcode administrators must manage default and maximum print quantities for
   multiple label categories in one place.
2. Sample entry workflows must persist order/specimen label quantity choices so
   downstream printing behavior is consistent and traceable.

## User Scenarios & Testing

### User Story 1 - Configure barcode label quantities in Admin (Priority: P1)

A laboratory administrator updates barcode configuration values so staff can
print the right number of labels for order, specimen, slide, block, and freezer
workflows.

**Why this priority**: Incorrect default or maximum label counts create
immediate operational friction and wasted consumables across all laboratories.

**Independent Test**: Can be fully tested by editing barcode quantity values in
the admin screen, saving, then reloading configuration to verify the same
values are returned.

**Acceptance Scenarios**:

1. **Given** an authenticated admin user on Barcode Configuration, **When** the
   user updates default label counts and saves, **Then** the system persists the
   values and returns them unchanged on the next load.
2. **Given** an authenticated admin user on Barcode Configuration, **When** the
   user updates maximum label counts and saves, **Then** the system persists the
   values and returns them unchanged on the next load.
3. **Given** a configuration value is missing or malformed in persisted config,
   **When** the page is loaded, **Then** the page still loads and safe fallback
   values are applied.

---

### User Story 2 - Persist per-sample label quantities during sample creation (Priority: P1)

A laboratory user creates a generic sample order and expects order/specimen
label quantity selections to be retained with the created sample and sample
items.

**Why this priority**: This ensures each sample has durable print quantity
metadata for consistent label handling and future reprint workflows.

**Independent Test**: Can be fully tested by creating a sample order with
explicit quantities and verifying that associated sample-level and
sample-item-level barcode quantity records are created or updated.

**Acceptance Scenarios**:

1. **Given** a generic sample order request includes order and specimen label
   quantities, **When** the order is saved, **Then** the system stores the order
   quantity for the sample and the specimen quantity for each sample item.
2. **Given** a generic sample order request omits one or both label quantities,
   **When** the order is saved, **Then** the system stores default quantity
   values.
3. **Given** barcode quantity metadata already exists for a sample or sample
   item, **When** a new save occurs for that same record, **Then** the existing
   metadata is updated rather than duplicated.

---

### User Story 3 - Maintain resilient label generation behavior (Priority: P2)

A laboratory user prints labels and expects printing to remain available even
when configuration values are incomplete or invalid.

**Why this priority**: Print interruptions block specimen processing and can
delay clinical workflows.

**Independent Test**: Can be fully tested by attempting label generation with
normal and malformed configuration states and confirming no unhandled failures
occur.

**Acceptance Scenarios**:

1. **Given** barcode configuration contains valid numeric values, **When** a
   label generation workflow runs, **Then** configured quantities and limits are
   applied.
2. **Given** barcode configuration contains malformed numeric values, **When** a
   label generation workflow runs, **Then** safe defaults are applied and
   processing continues.
3. **Given** maximum label quantity has been reached for a label category,
   **When** additional labels are requested, **Then** the user is prevented from
   exceeding the configured limit unless an explicit override path is used.

---

### Edge Cases

- Configuration values are blank, non-numeric, or partially missing.
- Sample has no sample items at save time.
- Existing barcode metadata exists for some sample items but not others.
- Quantity values are omitted from incoming sample order payload.
- Label request attempts to exceed configured maximum values.
- Label request exceeds max values while override is disabled.
- Label request exceeds max values while override is explicitly enabled.
- Concurrent updates occur to barcode configuration while users are printing.

## Requirements

### Functional Requirements

- **FR-001**: System MUST provide admin-configurable default label quantity
  values for order, specimen, slide, block, and freezer label categories.
- **FR-002**: System MUST provide admin-configurable maximum label quantity
  values for order, specimen, slide, block, and freezer label categories.
- **FR-003**: System MUST persist barcode configuration updates and return the
  persisted values via the barcode configuration retrieval endpoint.
- **FR-004**: System MUST tolerate missing or malformed numeric configuration
  values by applying safe fallback defaults rather than failing requests.
- **FR-005**: Generic sample order payloads MUST support order/specimen label
  quantity fields.
- **FR-006**: If generic sample order label quantity fields are not provided,
  the system MUST apply default quantity values.
- **FR-007**: When a generic sample order is saved, the system MUST persist
  sample-level order label quantity metadata.
- **FR-008**: When a generic sample order creates sample items, the system MUST
  persist sample-item-level specimen label quantity metadata.
- **FR-009**: For existing barcode metadata records tied to the same sample or
  sample item, the system MUST update existing records instead of creating
  duplicates.
- **FR-010**: Pathology-specific barcode metadata persistence MUST support
  storing specimen, block, slide, and freezer quantities per sample item when
  provided by workflow.
- **FR-011**: Label generation workflows MUST continue to function when
  optional barcode fields are disabled or unset.
- **FR-012**: User-facing labels and descriptions for newly exposed barcode
  quantity settings MUST be localized.
- **FR-013**: When a requested label quantity exceeds the configured maximum for
  a label type, the system MUST prevent printing and return a validation error
  unless an explicit override flag is enabled for that print operation.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI updates MUST use Carbon Design System components only
  (Principle II).
- **CR-002**: All user-facing UI strings MUST use React Intl and be externalized
  (Principle VII).
- **CR-003**: Backend changes MUST follow 5-layer architecture and keep
  transactions in services only (Principle IV).
- **CR-004**: Database changes MUST be implemented using Liquibase changesets
  (Principle VI).
- **CR-005**: Country-specific behavior MUST remain configuration-driven and not
  introduced via code branching (Principle I).
- **CR-006**: Automated tests MUST cover new behavior (unit/integration minimum)
  under existing project testing standards (Principle V).
- **CR-007**: Input values for configurable quantities MUST be validated within
  accepted ranges to reduce operational and security risk (Principle VIII).
- **CR-008**: Feature implementation and delivery MUST remain aligned with
  Spec-driven iteration standards for issue-linked development (Principle IX).

### Key Entities

- **Barcode Configuration**: Administrative settings controlling default and
  maximum label quantities and optional field toggles for each label category.
- **SampleBarcodeInfo**: Sample-level metadata record storing order label print
  count preferences.
- **SampleItemBarcodeInfo**: Sample-item-level metadata storing specimen (and
  pathology-related) label print count preferences.
- **GenericSampleOrderForm.DefaultFields**: Input contract carrying sample-entry
  fields, including order/specimen label quantity values.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of successful admin saves for barcode quantity configuration
  return the same saved values on subsequent retrieval.
- **SC-002**: 100% of generic sample orders created through this workflow
  persist sample-level and sample-item-level barcode quantity metadata.
- **SC-003**: Label generation flows complete without unhandled numeric parsing
  errors when persisted configuration values are missing or malformed.
- **SC-004**: 100% of newly introduced barcode quantity labels in the UI are
  available through localization message keys (including en and fr).
- **SC-005**: New automated tests covering barcode quantity persistence and
  configuration behavior pass in CI for this feature scope.
- **SC-006**: 100% of requests exceeding configured maximum label quantities are
  blocked when override is disabled, and accepted when override is enabled.

## Assumptions & Constraints

- OGC-284 scope includes barcode quantity configuration and persistence of label
  quantity metadata, not a redesign of barcode printing UX.
- Existing barcode printing endpoints and route patterns remain the integration
  path for this feature.
- Existing print workflow override controls (if enabled) are the authorized
  mechanism for exceeding configured max-label limits.
- Sample and sample item barcode quantity records are operational metadata and
  are not external-facing FHIR entities in this scope.
- Any schema changes required by this feature are implemented via Liquibase and
  are backward compatible with existing barcode configuration data.
- This feature remains compatible with existing multilingual deployments and
  does not introduce hardcoded locale-specific behavior.

## Out of Scope

- New barcode printer hardware integration.
- New barcode symbology standards beyond current platform support.
- Full redesign of barcode template/layout editor UX.
- Cross-module refactoring unrelated to barcode quantity configuration or
  persistence.
