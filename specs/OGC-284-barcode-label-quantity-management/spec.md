# Feature Specification: Barcode Label Quantity Management

**Feature Branch**: `feat/ogc-284-expand-barcode`  
**Created**: 2026-02-14  
**Status**: In Progress (remediation + artifact alignment)  
**Input**: User description: "This is a PR for
https://uwdigi.atlassian.net/browse/OGC-284. We need to generate specs properly
for this feature, using the issue number as the feature id."  
**Issue**: [OGC-284](https://uwdigi.atlassian.net/browse/OGC-284)

## Overview

Improve barcode label management so laboratories can reliably configure label
quantities, capture print quantity choices during sample creation, and complete
printing workflows after save.

This feature addresses two related operational needs:

1. Barcode administrators must manage default and maximum print quantities for
   multiple label categories in one place.
2. Sample creation workflows that support barcode printing must let users review
   and save label quantity choices so downstream printing behavior is consistent
   and traceable.

This specification is intended to fully capture the OGC-284 Jira ticket and
design intent. Ticket-described workflow and UX elements are in scope unless
explicitly marked out of scope below.

## Clarifications

### Session 2026-03-11

- Q: Which workflows must include the OGC-284 labels UI and post-save printing
  flow? → A: All relevant sample-creation workflows that support barcode
  printing.
- Q: What post-save printing behavior is required after the accession number is
  assigned? → A: Show a post-save dialog with per-label-type Print buttons that
  open PDFs in new browser tabs, plus a Done button that closes without printing
  and returns to Order View.
- Q: What must the sample-creation labels UI include before save? → A: One order
  row, one row per sample, editable applicable label counts, and a running
  total.
- Q: Should the spec require full parity with the original Jira/design intent or
  allow deferred follow-up scope? → A: Require full parity with Jira/design in
  this spec.
- Q: What is the primary workflow for initial implementation and validation? →
  A: The primary workflow is Add Order (`/SamplePatientEntry`), with rollout to
  all other relevant workflows after shared-foundation completion.

## User Scenarios & Testing

### User Story 1 - Configure barcode label quantities in Admin (Priority: P1)

A laboratory administrator updates barcode configuration values so staff can
print the right number of labels for order, specimen, slide, block, and freezer
workflows.

**Why this priority**: Incorrect default or maximum label counts create
immediate operational friction and wasted consumables across all laboratories.

**Independent Test**: Can be fully tested by editing barcode quantity values in
the admin screen, saving, then reloading configuration to verify the same values
are returned.

**Acceptance Scenarios**:

1. **Given** an authenticated admin user on Barcode Configuration, **When** the
   user updates default label counts and saves, **Then** the system persists the
   values and returns them unchanged on the next load.
2. **Given** an authenticated admin user on Barcode Configuration, **When** the
   user updates maximum label counts and saves, **Then** the system persists the
   values and returns them unchanged on the next load.
3. **Given** a configuration value is missing or malformed in persisted config,
   **When** the page is loaded, **Then** the page still loads and canonical
   fallback values are applied (default label count `1`, max label count `10`).
4. **Given** an authenticated admin user manages barcode label element settings,
   **When** optional fields are toggled for each label type and saved, **Then**
   the system persists those toggles while keeping Lab Number mandatory on all
   label types.
5. **Given** an authenticated admin user edits label dimensions, **When**
   positive numeric dimensions are saved for order/specimen/block/slide/freezer
   label types, **Then** the values persist and are used by print outputs.

---

### User Story 2 - Capture and persist label quantities during sample creation (Priority: P1)

A laboratory user creates a sample in any workflow that supports barcode
printing and expects label quantity selections to be available in the workflow,
retained with the created sample and sample items, and used by subsequent
printing steps.

**Why this priority**: This ensures each sample has durable print quantity
metadata for consistent label handling and future reprint workflows.

**Independent Test**: Can be fully tested by creating a sample in an applicable
workflow with explicit quantities, verifying those quantities are saved, and
confirming the saved order drives the next printing step.

**Acceptance Scenarios**:

1. **Given** a sample-creation workflow supports barcode printing, **When** the
   user reaches the labels step/section, **Then** the workflow shows one order
   row, one row per sample, editable applicable label quantities pre-populated
   from barcode configuration defaults, and a running total of labels to be
   printed.
2. **Given** a sample save request includes label quantities, **When** the order
   is saved, **Then** the system stores the order quantity for the sample and
   the applicable per-sample-item label quantities for the created records.
3. **Given** a sample save request omits one or more label quantities, **When**
   the order is saved, **Then** the system stores default quantity values of `1`
   or configured defaults, as appropriate for that label type.
4. **Given** barcode quantity metadata already exists for a sample or sample
   item, **When** a new save occurs for that same record, **Then** the existing
   metadata is updated rather than duplicated.

---

### User Story 3 - Complete post-save printing with resilient label generation (Priority: P2)

A laboratory user saves a sample and expects the system to guide printing
through a post-save dialog while keeping label generation available even when
configuration values are incomplete or invalid.

**Why this priority**: Print interruptions block specimen processing and can
delay clinical workflows.

**Independent Test**: Can be fully tested by saving a sample, confirming the
post-save dialog offers applicable label types with Print buttons and a Done
button, then attempting label generation with normal and malformed configuration
states and confirming no unhandled failures occur.

**Acceptance Scenarios**:

1. **Given** a supported sample-creation workflow saves successfully and an
   accession number is assigned, **When** save completes, **Then** the system
   shows a post-save print dialog listing the applicable label types available
   for printing.
2. **Given** the post-save print dialog lists applicable label types with
   per-row Print buttons, **When** the user clicks Print for a label type,
   **Then** a PDF for that label type opens in a new browser tab and is sized to
   the configured dimensions.
3. **Given** the user does not want to print immediately, **When** the user
   clicks Done, **Then** the sample is saved without printing, the dialog
   closes, and the user can reprint later from the Order View page.
4. **Given** barcode configuration contains malformed numeric values, **When** a
   label generation workflow runs, **Then** safe defaults are applied and
   processing continues.
5. **Given** maximum label quantity has been reached for a label category,
   **When** additional labels are requested, **Then** the request is blocked
   unless an explicit print-operation override flag (`override=true`) is
   provided.

---

### Edge Cases

- Configuration values are blank, non-numeric, or partially missing.
- Sample has no sample items at save time.
- Existing barcode metadata exists for some sample items but not others.
- Quantity values are omitted from incoming sample order payload.
- A supported sample-creation workflow has order labels but no applicable
  specimen/block/slide/freezer labels for the current sample.
- A sample-creation workflow includes multiple samples, so the labels UI must
  scale to one row per sample while preserving the order row and running total.
- A saved sample has only a subset of label types available, so the post-save
  print dialog must present only applicable label types.
- The user clicks Done on the post-save print dialog, so no print job is started
  at save time.
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
- **FR-002a**: System MUST provide admin-configurable label dimensions for
  order, specimen, block, slide, and freezer label types.
- **FR-002b**: Label dimension values MUST be validated as positive numbers.
- **FR-002c**: Barcode label element configuration MUST enforce Lab Number as a
  mandatory element for all label types and allow optional element toggles per
  label type (including freezer label optional elements).
- **FR-003**: System MUST persist barcode configuration updates and return the
  persisted values via the barcode configuration retrieval endpoint.
- **FR-004**: System MUST tolerate missing or malformed numeric configuration
  values by applying canonical fallback defaults rather than failing requests
  (default quantity fallback `1`, max quantity fallback `10`).
- **FR-004a**: System MUST validate that each label type's default count does
  not exceed its configured maximum count; if violated, save MUST be rejected
  with a validation error.
- **FR-005**: Generic sample order payloads MUST support order/specimen label
  quantity fields.
- **FR-005a**: All relevant sample-creation workflows that support barcode
  printing MUST expose a labels step or section before save so users can review
  and edit applicable label quantities; on Add Order, the labels section MUST be
  positioned between the ORDER and RESULT REPORTING sections.
- **FR-005b**: The labels step or section MUST show one order row, one row per
  sample, editable applicable label quantity fields, and a running total of all
  labels selected for printing.
- **FR-006**: If generic sample order label quantity fields are not provided,
  the system MUST apply default quantity values.
- **FR-007**: When a generic sample order is saved, the system MUST persist
  sample-level order label quantity metadata.
- **FR-008**: When a generic sample order creates sample items, the system MUST
  persist sample-item-level specimen label quantity metadata.
- **FR-009**: For existing barcode metadata records tied to the same sample or
  sample item, the system MUST update existing records instead of creating
  duplicates.
- **FR-010**: Backend pathology barcode metadata persistence MUST support
  storing specimen, block, slide, and freezer quantities per sample item when
  those values are supplied by pathology workflow/service inputs.
- **FR-010a**: For workflows that support additional label categories beyond
  order/specimen, the system MUST persist the applicable block, slide, and
  freezer quantities supplied by the workflow for the created sample items.
- **FR-011**: After a supported sample is saved and an accession number is
  assigned, the system MUST show a post-save print dialog that lists the
  applicable label types available for printing.
- **FR-011a**: Each applicable label type in the post-save print dialog MUST
  provide a Print button that opens a PDF in a new browser tab.
- **FR-011b**: The system MUST NOT offer label printing until the order is saved
  and an accession number is assigned.
- **FR-012**: PDFs generated from post-save print actions MUST be sized to the
  configured dimensions for the selected label type.
- **FR-012a**: The system MUST track cumulative labels printed per order and per
  sample item, by label type, so max-limit enforcement applies across multiple
  printing sessions.
- **FR-013**: The post-save print dialog MUST provide a Done button that closes
  without printing, returns the user to the Order View page, and preserves
  reprint capability from the Order View page.
- **FR-013a**: Existing Preprinted Bar Code Accession Number settings behavior
  MUST remain supported and unchanged by OGC-284 label quantity enhancements.
- **FR-014**: Label generation workflows MUST continue to function when optional
  barcode fields are disabled or unset.
- **FR-015**: User-facing labels and descriptions for newly exposed barcode
  quantity settings MUST be localized.
- **FR-016**: When a requested label quantity exceeds the configured maximum for
  a label type, the system MUST prevent printing and return a validation error
  unless explicit `override=true` is enabled for that print operation.

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
- **Sample Creation Workflow**: Any sample-creation workflow that supports
  barcode printing and must expose the labels UI and post-save printing flow.
- **Workflow-specific label inputs**: Input contracts carrying order/specimen
  and, where applicable, block/slide/freezer label quantity values.
- **Labels UI row model**: A pre-save labels presentation composed of one order
  row plus one row per sample, each showing only the applicable label types for
  that row and contributing to a running total.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of successful admin saves for barcode quantity configuration
  return the same saved values on subsequent retrieval.
- **SC-001a**: 100% of successful admin saves for label element toggles and
  dimensions return the same values on subsequent retrieval.
- **SC-002**: 100% of generic sample orders created through this workflow and
  other supported sample-creation workflows persist sample-level barcode
  quantity metadata, and persist applicable sample-item-level metadata whenever
  sample items are created.
- **SC-003**: Label generation flows complete without unhandled numeric parsing
  errors when persisted configuration values are missing or malformed.
- **SC-004**: 100% of successful saves in supported sample-creation workflows
  present a post-save print dialog with per-label-type Print buttons and a Done
  button.
- **SC-005**: 100% of newly introduced barcode quantity labels in the UI are
  available through localization message keys (including en and fr).
- **SC-006**: New automated tests covering barcode quantity persistence and
  configuration behavior pass in CI for this feature scope.
- **SC-007**: 100% of requests exceeding configured maximum label quantities are
  blocked when `override` is disabled/absent, and accepted only when
  `override=true` is enabled.

## Assumptions & Constraints

- OGC-284 scope includes barcode quantity configuration and persistence of label
  quantity metadata and required sample-creation/printing workflow updates for
  all supported barcode-printing sample-creation flows.
- This specification requires full parity with OGC-284 Jira acceptance criteria
  and does not treat major workflow/UI elements as optional follow-up scope.
- The Jira OGC-284 FRS attachment (`OpenELIS_Barcode_Labels_v1_FRS.md`) and
  companion mockup (`OpenELIS_BarcodeConfig_Mockup.jsx`) are the governing
  design artifacts for this feature.
- Existing barcode printing endpoints and route patterns remain the integration
  path for this feature.
- Post-save printing behavior is in scope, including per-label-type Print
  buttons that open PDFs in new tabs and a Done button to close without
  printing; a full template editor redesign remains out of scope.
- Existing print workflow override controls remain the authorized mechanism for
  exceeding configured max-label limits; this feature hardens behavior to use
  explicit `override=true` semantics for over-max requests.
- Sample and sample item barcode quantity records are operational metadata and
  are not external-facing FHIR entities in this scope.
- Any schema changes required by this feature are implemented via Liquibase and
  are backward compatible with existing barcode configuration data.
- Existing OGC-284 schema additions for barcode info tables are already present
  on the active branch and are validated/hardened as part of this remediation.
- This feature remains compatible with existing multilingual deployments and
  does not introduce hardcoded locale-specific behavior.

## Out of Scope

- New barcode printer hardware integration.
- New barcode symbology standards beyond current platform support.
- Full redesign of barcode template/layout editor UX.
- Cross-module refactoring unrelated to barcode quantity configuration or
  persistence.
