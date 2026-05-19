# Feature Specification: Barcode Labels v2 — Configurable Label Preset Management

**Feature Branch**: `feat/ogc-285-spec-cleanup` (M1) → `feat/ogc-285-*` (M2..M6)
**Created**: 2026-05-19
**Status**: Draft (M1 spec authoring; awaiting non-Copilot human review)
**Input**: `/speckit.specify` invocation 2026-05-19 from the kickoff prompt at
`~/.claude/plans/toasty-hugging-sparkle.md` § "SpecKit kickoff prompt".
**Issue**: [OGC-285](https://uwdigi.atlassian.net/browse/OGC-285)
**Supersedes**: [OGC-284](https://uwdigi.atlassian.net/browse/OGC-284) ·
[../OGC-284-barcode-label-quantity-management/](../OGC-284-barcode-label-quantity-management/)
**Canonical FRS**: [DIGI-UW/openelis-work · designs/admin-config/barcode-labels.md @ `7cf6f65`](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md)
(v2.5 · 2026-05-19 17:45 UTC · Casey Iiams-Hauser)

## Overview

Extend OpenELIS Global's barcode label system from a fixed set of five
hardcoded system label types (Order / Specimen / Block / Slide / Freezer) to
an **admin-configurable label preset system**. Lab administrators define
their own label types — including custom dimensions, barcode style, and
content fields — and tests in the Test Catalog declare which presets they
require and at what quantity. Order Entry aggregates label requirements
across all tests in an order and produces a single, deterministic label
workload. Historical orders reprint from a JSONB snapshot frozen at save
time, so admin edits to a preset do not retroactively change labels on
saved orders.

This release also closes the OGC-284 acceptance criteria that did not ship
in v1 (block/slide/freezer per-sample inputs, editable post-save
quantities, Skip — Print Later, max-limit override semantics). The
unshipped FRs and their absorption mapping are recorded in
[../OGC-284-barcode-label-quantity-management/spec.md § Gap Closure Matrix](../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix).

This specification is the engineering contract for OGC-285. The FRS
(linked above) is the authoritative design source; this spec transforms
that prose narrative into testable user stories, acceptance criteria, and
functional requirements. Prose stays in the FRS; requirements live here.

## Clarifications

### Session 2026-05-19 — FRS Open Questions resolved

The FRS v2.5 records four open questions in §11. Resolutions locked in this
spec, with rationale captured in [research.md](./research.md):

- **Q1 (snapshot vs current preset for reprint)** → **Snapshot only.** AC-20
  is the canonical statement. Reprint reads the JSONB snapshot frozen at
  order save time. The "always-current" alternative is deferred to v3+
  Future Considerations to avoid label drift from admin edits.
- **Q2 (allow-override toggle home)** → **Per-test only.** The existing
  combination of `test_label_preset_link.allow_override` (per-link) plus
  `test_label_config.allow_order_entry_override` (per-test master) is
  sufficient. No per-lab layer in v2.
- **Q3 (a11y drag-drop for content fields)** → **Keyboard Arrow Up/Down +
  native HTML5 drag for mouse.** No react-aria dependency.
- **Q4 (future custom-field shape)** → **Single `source_type` column on
  `label_preset_field`.** Schema unchanged for v2; v3+ migration decision
  deferred.

### Session 2026-05-19 — /speckit.clarify

- Q: Post-save quantity edit bounds — can the technician INCREASE beyond
  saved qty, or only DECREASE (audit-bound)? → A: **Decrease-only**,
  audit-bound at saved qty. `<NumberInput min=0 max=order_label_request.qty>`.
  The Print action overwrites `order_label_request.qty` to the new (lower)
  value. To print MORE labels, re-open the order and re-save with a higher
  qty before printing (separable workflow). Rationale: preserves audit
  trail (saved qty = ceiling); aligns with OGC-284 max-limit enforcement
  philosophy; avoids needing a separate `actual_printed_qty` column.
- Q: Legacy BarcodeConfiguration page lifecycle after M3 — keep
  parallel admin surfaces or consolidate? → A: **Consolidate.** M3
  deletes `frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.jsx`
  entirely along with its backend `BarcodeConfigurationRestController.java`
  qty/dim/element endpoints. The Preprinted Accession Number controls
  (`prePrintDontUseAltAccession` toggle + `prePrintAltAccessionPrefix`
  input, currently inside BarcodeConfiguration.jsx) move into the new
  Master Lists → Label Presets surface as a small "Site-wide Barcode
  Settings" section above the preset list. Single admin page for all
  barcode admin. Legacy `site_information.barcode.preprinted.*` keys
  retain their semantics (no rename); the M3 PR rewires the new section
  to the same keys. Rationale: enforced by constitution Principle X
  (Legacy Code Removal — "no dual-write, no legacy-first"); also
  satisfies "no parallel legacy admin surfaces" principle. This is a
  3rd deliberate divergence from Casey's FRS — announced via Jira
  comment on [OGC-285](https://uwdigi.atlassian.net/browse/OGC-285).
- Q: Preset name uniqueness — exact-match, case-insensitive, or with
  whitespace trim? → A: **Case-insensitive AND trim leading/trailing
  whitespace.** Service layer normalizes via
  `name.trim().toLowerCase()` before uniqueness check; DDL UNIQUE
  constraint serves as defense-in-depth (catches anything bypassing the
  service). "Cryo Vial", "cryo vial", "CRYO VIAL", "  Cryo Vial  " are
  all treated as the same name and collide on save. FR-002 / AC-4
  acceptance test must cover all three normalization cases (case,
  leading whitespace, trailing whitespace). Rationale: prevents the
  most common admin error (typo/capitalization creating silent
  duplicates in pickers); standard pattern for human-facing admin
  names.

### Session 2026-05-19 — Deliberate divergences from Casey's FRS

Two locks intentionally tighten or invert the FRS markdown:

- **Editable post-save quantities = YES** (per Casey's 2026-05-19 Jira
  edit, which postdates the FRS markdown). The post-save dialog renders
  quantities as Carbon `<NumberInput>` with `min=0` and `max` from
  `order_label_request.qty` at save time. This closes the OGC-284 dialog
  gap that currently renders quantity as a `<p>` element. The FRS markdown
  §4.6 is silent on editability; the Jira description's "Locked design
  decisions (May 2026)" section is the tightening source.
- **OGC-284 Order Entry quantity UI gap is NOT deferred** (contra FRS §1.5
  which lists it as out-of-scope). The gap is absorbed by US4 because the
  M5 LabelsSection rewrite removes the
  `applicableLabelTypes: ["specimen"]` hardcode as a side effect.
  Deferring it would create throwaway code; M5 must rewrite that file
  anyway.

## User Scenarios & Testing _(mandatory)_

### User Story 1 — Configure Label Presets in Admin (Priority: P1)

A laboratory administrator opens **Administration → Master Lists → Label
Presets**, sees the five system presets pre-seeded from the legacy v1
configuration, creates a custom preset (e.g., "Cryo Vial Label" at
25 × 25 mm with QR code, containing Lab Number + Storage Location +
Expiry Date), assigns dimensions and per-scope default/max quantities,
and saves. The preset is then available for linking from the Test Catalog
Labels tab.

**Why this priority**: This is the foundational surface — without admin
CRUD on presets, every downstream surface (Test Catalog linking, Order
Entry aggregation, post-save print) has no preset data to work with.

**Independent Test**: Can be fully tested by opening the Master Lists →
Label Presets page on a fresh installation, observing the 5 seeded system
presets, creating one custom preset, deactivating it, reactivating it,
and confirming the preset appears in subsequent lookups.

**Acceptance Scenarios** (FRS AC-1..AC-7):

1. **Given** an admin user lands on Master Lists → Label Presets on a
   fresh upgrade from v1, **When** the page loads, **Then** the user sees
   exactly five system presets (Order Label, Specimen Label, Block Label,
   Slide Label, Freezer Label) with `is_system = true`, named, dimensioned,
   and quantity-populated from the legacy `site_information.barcode.*`
   keys.
2. **Given** an admin user opens the "+ Add Label Preset" editor, **When**
   the user enters name "Cryo Vial Label", height 25 mm, width 25 mm,
   barcode type QR, content fields Lab Number + Storage Location +
   Expiry Date, scope per-sample with default 1 and max 4, and clicks
   Save, **Then** the preset persists and is returned by the next list
   request with the supplied values.
3. **Given** an admin user opens a system preset (e.g., Order Label),
   **When** the user attempts to deactivate it or rename it, **Then** the
   system blocks the save with an inline field-level error referencing
   `is_system = true`.
4. **Given** an admin user tries to save a preset with a name that
   matches an existing preset (case-insensitive), **When** they click
   Save, **Then** the system blocks the save with a field-level
   "name must be unique" error.
5. **Given** an admin user deactivates a custom preset that is currently
   linked to a test, **When** the deactivation completes, **Then** the
   preset disappears from the Test Catalog Labels tab "+ Add Label Type"
   picker, but historical orders that referenced it still print
   correctly via snapshot.
6. **Given** an admin user clicks "Save as new" (Duplicate), **When** they
   keep the original name and click Save, **Then** the system blocks save
   with a uniqueness error and requires entering a new name before saving.
7. **Given** an admin user enters `max_per_order = 3` and
   `default_per_order = 5` (or saves with both scope flags off), **When**
   they click Save, **Then** the system blocks save with a field-level
   validation error explaining the constraint.

---

### User Story 2 — Link presets to tests in Test Catalog (Priority: P1)

A laboratory administrator opens the **Test Catalog → Test Editor →
Labels tab** for a specific test (e.g., CBC with Differential), links one
or more per-sample label presets, sets per-test default and max
quantities, and chooses whether order-entry users can override the
quantity. The Order Entry Preview card on the page shows how the test's
labels will surface during Order Entry.

**Why this priority**: Without test-level binding, Order Entry has no
test-driven default and falls back entirely on preset-level defaults —
which removes the whole purpose of per-test configuration. P1 because
US3 depends on this.

**Independent Test**: Can be fully tested by linking two presets to one
test, saving, reloading the test editor, confirming the linked presets
appear with the saved quantities + allow-override flag, and observing
the Order Entry Preview render correctly.

**Acceptance Scenarios** (FRS AC-8..AC-12):

1. **Given** a Test Editor for CBC, **When** the user opens the Labels
   SideNav entry, **Then** the Labels tab loads with a "Linked Label
   Presets" empty table and the "+ Add Label Type" button.
2. **Given** the Labels tab is open, **When** the admin links Specimen
   Label (default 1, max 5, allow override on) and Slide Label (default
   4, max 12, allow override off) and clicks Save, **Then** both links
   persist and reload identically.
3. **Given** the Labels tab shows two linked presets, **When** the user
   views the Order Entry Preview, **Then** the preview accurately reflects
   the linked presets, their default quantities, and lock indicators for
   `allow_override = false`.
4. **Given** the user has Specimen Label already linked to CBC,
   **When** they open the "+ Add Label Type" picker, **Then** Specimen
   Label is excluded from the picker (preventing duplicate links).
5. **Given** the test-level `allow_order_entry_override` toggle above the
   table is off, **When** the page renders, **Then** all per-preset
   Allow Override checkboxes are forced off, disabled, and visually
   muted to indicate the test-level lockdown.

---

### User Story 3 — Aggregated, dynamic Order Entry Labels section (Priority: P1)

A laboratory technician creates an order with one or more tests (e.g.,
CBC + Tissue Biopsy). The Order Entry **Labels section** (step 4)
renders two Carbon DataTables — Order Labels (per-order presets) and
Sample Labels (per-sample presets, one row per sample) — with dynamic
columns built from the union of presets linked to the order's tests.
Each cell pre-populates from per-test or preset defaults, shows a
source tag explaining where the default came from, locks if any linked
test forbids override, and contributes to a live total row.

**Why this priority**: This is the user-facing payoff of the whole
feature. Without it, admins have configured presets that no order
consumes.

**Independent Test**: Can be fully tested by selecting two tests with
known preset links, observing the columns + cells render per the
aggregation rules, adjusting an unlocked cell, observing the total
update, saving the order, and confirming `order_label_request` rows
are persisted with the JSONB snapshot.

**Acceptance Scenarios** (FRS AC-13..AC-19):

1. **Given** a user adds CBC + Tissue Biopsy tests to an order, **When**
   they reach the Labels section, **Then** the Order Labels table shows
   all active per-order presets (system-first, then alphabetical) and
   the Sample Labels table shows the union of per-sample presets
   linked to either test, system-first then alphabetical, with one row
   per sample.
2. **Given** the Sample Labels table is rendered, **When** the user
   inspects a cell, **Then** a small `<Tag>` chip below the
   `<NumberInput>` indicates the source — "from Test B" if a linked
   test drove the value, "system default" if no test in the order
   linked the preset.
3. **Given** the Slide Label preset is linked to Tissue Biopsy with
   `allow_override = false`, **When** the cell renders for the Tissue
   Biopsy sample row, **Then** the `<NumberInput>` is read-only with a
   lock icon and a tooltip "Quantity locked by test catalog".
4. **Given** two tests in the order link the same preset with
   different `allow_override` values (one true, one false), **When** the
   cell renders, **Then** the most-restrictive rule wins: cell is
   read-only with a lock icon, tooltip names the locking test.
5. **Given** Test A overrides Specimen Label default to 2 and Test B
   overrides it to 3, **When** the Specimen Label cell renders for a
   sample where both tests apply, **Then** the cell pre-populates with
   `3` (highest wins) and the source `<Tag>` reads "from Test B".
6. **Given** the user modifies an unlocked cell, **When** the value
   commits, **Then** the live Total row at the bottom recalculates
   immediately to reflect the new sum.
7. **Given** the user clicks "Save order & queue labels", **When** the
   order saves, **Then** the system writes one `order_label_request`
   row per `(sample, preset)` pair (and one for each Order-Labels cell)
   including a JSONB `preset_snapshot` matching the canonical shape in
   data-model.md §7.3.1 of the FRS.

---

### User Story 4 — Migration & continuity (Priority: P1)

A site upgrading from OGC-284 (v1) to OGC-285 (v2) sees its existing
barcode configuration preserved: the 5 legacy system label types become
5 seeded `is_system = true` Label Presets with dimensions and quantities
copied from `site_information.barcode.*` keys; legacy keys remain
readable for one release cycle; existing v1 orders continue to print
correctly; and the M5 LabelsSection rewrite closes the unshipped OGC-284
acceptance criteria as a side effect.

**Why this priority**: P1 because every deployed site is currently a v1
deployment. Migration must be transparent and reversible; otherwise the
feature cannot ship.

**Independent Test**: Can be fully tested by running the migration
against a known v1 database snapshot, confirming the 5 system presets
are created with values matching the legacy keys, confirming legacy
orders reprint correctly, and confirming the LabelsSection.jsx
applicableLabelTypes hardcode is removed.

**Acceptance Scenarios** (FRS AC-21..AC-24, plus OGC-284 gap closure):

1. **Given** a v1 deployment with populated `site_information.barcode.*`
   keys (order/specimen/block/slide/freezer × default/max/height/width),
   **When** the v2 Liquibase migration runs, **Then** exactly 5
   `label_preset` rows are created with `is_system = true`, scope flags
   per FRS §2.7 (Order Label = per-order; the other 4 = per-sample),
   dimensions copied, and quantities mapped into the correct per-scope
   columns (Order Label → `default_per_order` / `max_per_order`; others
   → `default_per_sample` / `max_per_sample`).
2. **Given** a v1 deployment with legacy `site_information.barcode.*`
   keys, **When** the migration completes, **Then** the legacy keys
   remain readable in `site_information` (read-only mirror, one release
   cycle), and a follow-up migration in v2.x will remove them.
3. **Given** existing v1 orders in the database with rows in
   `sample_barcode_info` and `sample_item_barcode_info`, **When** a
   user opens an existing v1 order's View page, **Then** labels print
   correctly using the v1 schema fallback path until the v2 reprint
   path is fully wired (M6).
4. **Given** existing OGC-761 `test_label_preset_link` rows (if the
   table exists on develop at M2 start), **When** the v2 migration
   adds the `allow_override` column with default `true`, **Then** all
   existing rows persist and remain valid, with `allow_override`
   populated to its schema default.
5. **Given** the M5 LabelsSection rewrite lands, **When** a user creates
   a new order on Add Order, **Then** the Labels section renders the
   two-table v2 layout instead of the v1 single-table specimen-only
   layout, closing OGC-284 FR-005b and FR-010a as a side effect (see
   [Gap Closure Matrix](../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix)).

---

### User Story 5 — Reprint integrity via JSONB snapshot (Priority: P2)

A laboratory technician reprints labels for a previously saved order
from the Order View page. The labels render exactly as they did at
original print time — including content fields, dimensions, and
barcode type — even if the admin has subsequently edited the linked
label preset. The snapshot JSONB stored at order save time is the
authoritative source for reprint rendering.

**Why this priority**: P2 because the main user-facing flow (US3)
delivers value without reprint-from-snapshot. But P2-not-P3 because
without snapshot integrity, admin preset edits cause label drift on
historical orders — a chain-of-custody risk.

**Independent Test**: Can be fully tested by saving an order, mutating
the linked preset (e.g., add a content field, change dimensions),
reprinting the saved order, and confirming the labels render with
the original snapshot configuration, not the current preset.

**Acceptance Scenarios** (FRS AC-19..AC-20):

1. **Given** a user saves an order on 2026-06-01 with Specimen Label
   preset configured as `[Lab Number, Patient Name]`, **When** the
   save completes, **Then** the `order_label_request.preset_snapshot`
   JSONB column captures the preset config + per-test link settings +
   content field list at that moment.
2. **Given** an admin edits the Specimen Label preset on 2026-07-15 to
   add a Collection Date content field, **When** the technician
   reprints the 2026-06-01 order, **Then** the reprinted labels use
   the snapshot (no Collection Date), not the current preset.
3. **Given** the preset's dimensions are modified post-save (e.g.,
   25 × 76 → 25 × 50), **When** the order is reprinted, **Then** the
   PDF is sized to the snapshot's 25 × 76 dimensions.

---

### Edge Cases

- A v1 deployment has a malformed `site_information.barcode.X.default`
  value (non-numeric). Migration applies the canonical fallback (`1`)
  per OGC-284 FR-004 and logs a warning, but does not fail.
- A test catalog row has no `test_label_preset_link` rows. Order
  Entry's Sample Labels table falls back to the active system per-sample
  presets (Specimen / Block / Slide / Freezer) and shows "system
  default" source tags.
- A user adds two tests where the same preset is linked to both with
  conflicting allow-override settings. Most-restrictive wins (cell
  locked), tooltip names the locking test.
- A preset is deactivated mid-order-entry session (admin deactivates
  while another user has an order in progress). The in-progress order's
  Labels section continues to show the preset for that session; refresh
  pulls the deactivated preset out.
- A user submits a label save request that exceeds a per-preset max
  quantity for a cell where `allow_override = true`. Server-side
  validation rejects the save with a 422; client renders the field-level
  error.
- An order has zero tests selected. Order Labels table still renders
  (showing all active per-order presets); Sample Labels falls back to
  system per-sample presets.
- A snapshot JSONB references a preset id that has been hard-deleted.
  Reprint uses the snapshot content; the missing preset is not
  re-fetched.
- A preset has both `prints_per_order = true` and
  `prints_per_sample = true`. The preset appears as a column in BOTH
  Order Labels and Sample Labels tables, with independent default
  quantities per scope.

## Requirements _(mandatory)_

### Functional Requirements

Renumbered from FRS LP-x, TL-x, OE-x, MG-x with original IDs preserved
in parentheses for traceability.

#### Label Presets — Master Lists Admin

- **FR-001 (LP-1)**: System MUST display a Label Presets list view at
  `/MasterListsPage#labelPresets`.
- **FR-002 (LP-2)**: Admins MUST be able to create a new preset with a
  unique name, integer-mm dimensions (height + width, 5..200), barcode
  type (Code 128 / QR / DataMatrix), and a selection of system content
  fields from the 15-field set in [data-model.md](./data-model.md).
- **FR-003 (LP-3)**: Admins MUST be able to select content fields from
  the system field set and arrange them in display order. Lab Number is
  always required, always present, and locked at position 1.
- **FR-004 (LP-4)**: Admins MUST be able to duplicate a preset via
  "Save as new", clearing the name field for required entry.
- **FR-005 (LP-5)**: Admins MUST be able to deactivate a preset.
  Deactivated presets persist on historical orders but disappear from
  "+ Add Label Type" pickers.
- **FR-006 (LP-6)**: System MUST prevent renaming or deactivating any
  `is_system = true` preset.
- **FR-007 (LP-7)**: System MUST validate that `max_per_order ≥
  default_per_order`, `max_per_sample ≥ default_per_sample`, all
  values are non-negative integers, and at least one scope
  (`prints_per_order` or `prints_per_sample`) is true.
- **FR-008 (LP-8)**: System MUST persist Lab Number as a required,
  locked, first-position field on every preset.

#### Test Catalog — Labels Tab

- **FR-009 (TL-1)**: System MUST display a Labels section in the Test
  Editor SideNav.
- **FR-010 (TL-2)**: Admins MUST be able to link any active per-sample
  preset to a test with default qty, max qty, and `allow_override`
  flag. The Preset dropdown MUST exclude presets where
  `prints_per_sample = false`.
- **FR-011 (TL-3)**: System MUST prevent linking the same preset to the
  same test twice.
- **FR-012 (TL-4)**: System MUST display an Order Entry Preview (Carbon
  `<StructuredList>`) summarizing the configuration.
- **FR-013 (TL-5)**: System MUST aggregate per-sample label
  requirements across all tests in an order by taking the highest
  `test_label_preset_link.default_qty` per preset.
- **FR-014 (TL-6)**: System MUST fall back to
  `label_preset.default_per_sample` when an order contains no tests
  linked to that preset.

#### Order Entry

- **FR-015 (OE-1)**: System MUST dynamically construct Labels-section
  columns from the union of presets across all tests in the order,
  rendered as two tables (Order Labels for per-order presets, Sample
  Labels for per-sample presets).
- **FR-016 (OE-2)**: System MUST display, per cell, a source
  `<Tag>` indicating whether the value came from a test ("from Test X")
  or system default.
- **FR-017 (OE-3)**: Cells whose driving test has `allow_override =
  false` MUST be read-only with a lock icon. When multiple tests link
  the same preset and any one has `allow_override = false`, the cell
  MUST be locked (most-restrictive wins). Tooltip names the locking
  test.
- **FR-018 (OE-4)**: System MUST display a live Total row summing each
  column.
- **FR-019 (OE-5)**: System MUST persist the entered label quantities
  with the order upon Save, including a JSONB `preset_snapshot` per
  data-model.md.
- **FR-020 (OE-6)**: Reprinting from Order View MUST use the snapshot
  stored with the order, not the current `label_preset` config.

#### Post-Save Print Dialog

- **FR-021**: After successful order save and accession-number
  assignment, system MUST show a post-save print dialog listing every
  preset with a non-zero count in the saved order, dynamic across all
  active presets (not the v1 5-fixed enum).
- **FR-022**: Each preset row in the dialog MUST provide a Carbon
  `<NumberInput>` showing the editable quantity (locked to v2 design
  per the 2026-05-19 Jira tightening). `min = 0`,
  `max = order_label_request.qty` at save time.
- **FR-023**: Each preset row in the dialog MUST provide a Print
  button that opens a PDF in a new browser tab, generated from the
  preset's snapshot.
- **FR-024**: The dialog MUST provide a "Skip — Print Later" button
  that closes the dialog without printing and preserves reprint
  capability from the Order View page.
- **FR-025**: PDF labels MUST be sized to the snapshot dimensions, not
  the current preset dimensions.

#### Migration & Backward Compatibility

- **FR-026 (MG-1)**: At v2 release, system MUST create exactly one
  `label_preset` row per existing v1 system label type, populated from
  `site_information.barcode.*` keys per FRS §2.7.
- **FR-027 (MG-2)**: System MUST set `is_system = true` on each
  migrated preset.
- **FR-028 (MG-3)**: Existing OGC-761 `test_label_preset_link` rows
  MUST continue to function unchanged after the v2 schema additions;
  new `allow_override` column populates to `true` by default.
- **FR-029 (MG-4)**: Existing v1 orders MUST continue to print labels
  against their existing v1 schema until snapshot rewrite of historical
  orders is complete (not required for v2; v1 orders retain v1
  behavior).
- **FR-030 (MG-5)**: Legacy `site_information.barcode.*` keys MUST be
  retained as read-only mirrors for one release cycle and removed in
  the subsequent maintenance migration.

#### Accessibility

- **FR-031 (AC-25)**: All Carbon components used MUST pass screen-reader
  smoke tests (NVDA + JAWS).
- **FR-032 (AC-26)**: Content-field reordering MUST work via keyboard
  (Arrow Up / Arrow Down on focused row).
- **FR-033 (AC-27)**: Color MUST NEVER be the sole indicator of status;
  system Tag, lock icon, and text labels MUST accompany color cues.

### Constitution Compliance Requirements (OpenELIS Global)

Derived from `.specify/memory/constitution.md`:

- **CR-001** (Principle II): UI components MUST use Carbon Design
  System (`@carbon/react`) exclusively. NO Bootstrap, NO Tailwind, NO
  custom CSS frameworks. Modal, DataTable, NumberInput, Dropdown,
  FilterableMultiSelect, Tag, StructuredList, Tabs, Checkbox, Toggle
  are the primary components used.
- **CR-002** (Principle VII): All user-facing strings MUST be
  internationalized via React Intl. NO hardcoded text. New keys land in
  `frontend/src/languages/en.json` ONLY; non-English locale files are
  Transifex-managed. Key prefixes:
  - `admin.labelPresets.*` for Master Lists → Label Presets
  - `admin.testCatalog.labels.*` for Test Catalog Labels tab
  - `orderEntry.labels.*` for Order Entry Labels section
  - `barcode.print.dialog.*` for the post-save print dialog
- **CR-003** (Principle IV): Backend MUST follow the 5-layer pattern:
  Valueholder → DAO → Service → Controller → Form. Services own
  `@Transactional`; controllers MUST NOT have `@Transactional`.
  Valueholders use JPA/Hibernate annotations (Jakarta EE 9 `jakarta.*`,
  NOT `javax.*`). New package: `org.openelisglobal.labelpreset.*`.
- **CR-004** (Principle VI): All schema changes MUST use Liquibase
  changesets. NO direct DDL/DML in production. New tables land in
  `src/main/resources/liquibase/3.3.x.x/0NN-label-preset-tables.xml`
  (NN = next available); migration data is in
  `src/main/resources/liquibase/3.3.x.x/0NN-seed-system-presets.xml`.
- **CR-005** (Principle III): Label Preset entities are NOT externally
  exposed (no FHIR R4 representation). The exception is the order
  label rendering on physical labels, which uses internal data only.
  No `fhir_uuid` required.
- **CR-006** (Principle I): Country-specific behavior MUST be
  configuration-driven (label preset content). No code branching on
  country/site.
- **CR-007** (Principle VIII): Security — admin endpoints require the
  `admin.barcode.manage` scope; test catalog endpoints require
  `admin.testCatalog.manage`; order entry endpoints require
  `order.create`; reprint endpoints require `order.read`. All input
  values validated server-side (dimensions, quantities, name
  uniqueness, scope constraint).
- **CR-008** (Principle V): Tests MUST precede implementation (TDD
  Red-Green-Refactor). JUnit 4 for backend (NOT JUnit 5); Vitest +
  React Testing Library for frontend; Playwright for E2E. Coverage
  target ≥ 70% for new modules.

### Key Entities

- **LabelPreset**: A reusable label template defining dimensions,
  barcode type, content fields, and per-scope default/max quantities.
  System presets (`is_system = true`) cannot be renamed or deactivated;
  custom presets are fully mutable.
- **LabelPresetField**: A row on a label preset selecting one of 15
  system content fields with a position and required flag. Lab Number
  is locked at position 1.
- **TestLabelPresetLink**: An association between a Test and a
  LabelPreset (per-sample only) with per-test override of default qty,
  max qty, and `allow_override` flag. Reused from OGC-761; this release
  adds the `allow_override` column.
- **TestLabelConfig**: A per-test master toggle
  (`allow_order_entry_override`) that overrides all per-link
  `allow_override` flags when off.
- **OrderLabelRequest**: A per-`(parent_sample, sample_item, preset)` row
  capturing the entered quantity + a JSONB snapshot of the preset config
  at save time. The authoritative source for reprint rendering. Naming
  note: OpenELIS has no separate "order" table — the order identity is
  rooted in the parent `sample` row. Per-order labels have
  `sample_item_id IS NULL`; per-sample-item labels reference both the
  parent sample and the specific sample item.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 100% of fresh v2 installations show exactly 5 seeded
  system presets matching the FRS §2.7 mapping after migration.
- **SC-002**: 100% of upgraded v1 sites preserve their
  `site_information.barcode.*` configured values in the migrated
  system preset rows.
- **SC-003**: Admins can create a custom label preset (Cryo Vial
  example from FRS AC-2) in under 90 seconds from opening the editor
  to seeing the row appear in the list view.
- **SC-004**: 100% of Order Entry sessions on saved orders persist
  one `order_label_request` row per non-zero cell with a JSONB
  snapshot conforming to the FRS §7.3.1 shape.
- **SC-005**: 100% of reprints from Order View use the snapshot, not
  the current preset config (verifiable by post-save preset edit +
  reprint smoke test).
- **SC-006**: 100% of test catalog → labels-tab → order-entry round
  trips reflect the same default/max/lock state without manual
  intervention.
- **SC-007**: 100% of attempts to deactivate a system preset are
  blocked with a field-level error.
- **SC-008**: 100% of attempts to save a duplicate preset name (case-
  insensitive) are blocked with a field-level error.
- **SC-009**: All 27 FRS acceptance criteria (AC-1..AC-27) have at
  least one passing automated test before the corresponding milestone
  PR merges.
- **SC-010**: 0 hardcoded English strings introduced in new code;
  100% of new user-facing strings reference an i18n key.
- **SC-011**: 0 instances of `applicableLabelTypes: ["specimen"]`
  remain in `frontend/src/components/barcodeWorkflow/` after M5
  merges.

## Assumptions & Constraints

- **OGC-285 supersedes OGC-284 in scope.** The OGC-284 in-repo SpecKit
  is closed and superseded; unshipped OGC-284 FRs are absorbed by
  US4 per the [Gap Closure Matrix](../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix).
- **Editable post-save quantities = YES.** The Jira description's
  "Locked design decisions (May 2026)" supersedes the FRS markdown
  silence on this point; the post-save dialog renders quantities as
  Carbon `<NumberInput>` (audit-bound, `min = 0`,
  `max = order_label_request.qty` at save).
- **Snapshot is frozen.** Reprint always renders from snapshot.
  Future support for "use current preset on reprint" is deferred to
  v3+ Future Considerations.
- **OGC-746 (Test Editor scaffold) is NOT on develop** (verified by
  source-code-truth check 2026-05-19: only `ManageMethod.jsx` and
  `ViewTestCatalog.jsx` exist under `frontend/src/components/admin/testManagement/`).
  M4 frontend ships a temporary `<Tabs>` host in
  [ViewTestCatalog.jsx](../../frontend/src/components/admin/testManagement/ViewTestCatalog.jsx);
  migrate to the OGC-746 scaffold once it lands. M4 backend is
  unblocked.
- **OGC-761 `test_label_preset_link` table is NOT on develop**
  (verified by source-code-truth check 2026-05-19: no Liquibase
  changeset references the table). The M2 Liquibase changeset creates
  the full table from scratch with all v2 columns including
  `allow_override`; the FRS §3.5 ALTER reduces to a CREATE.
- **Legacy Barcode Configuration page is deleted in M3** (3rd
  deliberate divergence from FRS §5). The
  `frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.jsx`
  file and its backend `BarcodeConfigurationRestController.java`
  qty/dim/element endpoints are removed in the same PR that ships the
  new Master Lists → Label Presets surface. The Preprinted Accession
  Number controls move into the new Label Presets page as a small
  "Site-wide Barcode Settings" section above the preset list, backed
  by the same `site_information.barcode.preprinted.*` keys (no rename,
  no semantics change). A redirect from the legacy
  `/MasterListsPage#barcodeConfiguration` URL to the new
  `/MasterListsPage#labelPresets` URL preserves bookmark continuity
  for one release cycle. Rationale: "no half-refactors" +
  "no parallel legacy admin surfaces" — single clear place for the
  single clear barcode-admin workflow. This divergence is documented
  in research.md (when authored by /speckit.plan) and announced via a
  comment on Jira [OGC-285](https://uwdigi.atlassian.net/browse/OGC-285).
- **Five hardcoded label types disappear from new code.** They survive
  as 5 seeded `is_system = true` rows in `label_preset`. The frontend
  Barcode Configuration page may keep its current shape for one
  release cycle (defining seeded preset values via Master Lists →
  Label Presets is the v2 path).
- **Existing v1 orders use v1 print path.** The migration does not
  retroactively write `order_label_request` rows for historical
  orders. New orders (created after M5 lands) use the v2 path
  exclusively. Old orders reprint via the v1 `sample_barcode_info`
  fallback until phased out.
- **i18n key budget.** New keys land in `en.json` only; French and
  other locales are Transifex-managed. Layout budget per surface
  accommodates ~30% text expansion.
- **Postmortem guardrails apply to every milestone PR.** Per
  [../OGC-284-barcode-label-quantity-management/POSTMORTEM.md](../OGC-284-barcode-label-quantity-management/POSTMORTEM.md):
  no self-merge without non-Copilot human review; AC checklist in PR
  body; no mid-stream rescoping; no Jira self-resolve; ≤30 files /
  ≤2,500 LOC net per milestone PR; tests precede implementation; open
  PR as draft early.

## Out of Scope

Mirroring FRS §1.5, minus the OGC-284 deferral (absorbed by US4):

- **User-defined custom content fields** (free text or fixed-value
  rows on a preset). v2 supports only the 15-field system set; the
  `label_preset_field.source_type` column reserves the seat for v3+
  expansion (CR-006: configuration-driven, not code-driven).
- **Live preview pane** in the preset editor. v2 has no in-editor
  rendering of the label; admins validate visually at print time.
- **Manual-confirm per-preset mode.** All presets auto-generate; the
  post-save print dialog's per-preset Print buttons are sufficient
  control.
- **Full WYSIWYG canvas designer.** v2 supports content-field
  selection + ordering, not pixel-perfect placement.
- **Per-preset font/typography controls.** Fields render in the
  system label font.
- **Conditional content fields** (e.g., "show Stain Type only when
  test is histology"). Deferred to v3+.
- **Multi-language labels.** Labels render in the system locale at
  print time.
- **Reprint audit trail beyond the snapshot itself.** Snapshot records
  what was printed; surfacing the reprint history in Order View is
  deferred to v3+.

## References

- [Canonical FRS v2.5 (DIGI-UW/openelis-work @ 7cf6f65)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md)
- [Jira OGC-285](https://uwdigi.atlassian.net/browse/OGC-285)
- [research.md](./research.md) — FRS pin discipline, Q1–Q4 resolutions, divergence rationale
- [plan.md](./plan.md) — M1–M6 milestone breakdown
- [tasks.md](./tasks.md) — `[T###]` task-level breakdown with AC traceability
- [data-model.md](./data-model.md) — DDL, CHECK constraints, JSONB snapshot shape, Hibernate notes
- [contracts/openapi.yaml](./contracts/openapi.yaml) — 10 REST endpoints
- [quickstart.md](./quickstart.md) — per-milestone verification recipes
- [../OGC-284-barcode-label-quantity-management/POSTMORTEM.md](../OGC-284-barcode-label-quantity-management/POSTMORTEM.md) — the OGC-284 ship-with-gaps story; guardrails inform OGC-285 PR discipline
- [../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix](../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix) — unshipped OGC-284 FRs → OGC-285 milestone mapping
