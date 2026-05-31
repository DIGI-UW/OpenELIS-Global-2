# Quickstart: OGC-285 Barcode Labels v2 — Per-Milestone Verification

**Purpose:** Step-by-step verification recipes a reviewer can execute by hand
during PR review for each OGC-285 milestone. Each recipe walks the relevant FRS
acceptance criteria against running code.

**Reference:** [spec.md](./spec.md) · [plan.md](./plan.md) ·
[data-model.md](./data-model.md) ·
[contracts/openapi.yaml](./contracts/openapi.yaml) ·
[research.md](./research.md).

**Engineering practice** (applies to every milestone): the reviewer + author
walk this quickstart together at PR-ready. Visible UI verification beats "do the
tests pass" — feature existence is the bar.

---

## Common setup

Before any milestone verification, get the worktree to a known state:

```bash
# Backend build, skip tests
mvn clean install -DskipTests -Dmaven.test.skip=true

# Frontend deps
cd frontend && npm install && cd ..

# Run dev stack
./run-dev.sh   # or your local equivalent (Tomcat + PostgreSQL)
```

Open the browser at `http://localhost:8080/OpenELIS-Global/`. Log in as an admin
user. Confirm the dev DB is on the OGC-285 branch's Liquibase state (current
changelog applied + no pending).

For frontend-only iteration:

```bash
cd frontend && npm start    # localhost:3000 with hot reload
```

---

## M1 — Spec authoring (no code; documentation only)

**Verification:** PR review of the documentation.

- [ ] `specs/OGC-285-barcode-label-presets/` exists with: spec.md, plan.md,
      tasks.md (M1 also if `/speckit.tasks` ran), research.md, data-model.md,
      quickstart.md (this file), contracts/openapi.yaml,
      checklists/requirements.md.
- [ ] [spec.md Clarifications](./spec.md#clarifications) records 4 resolved FRS
      Open Questions (Q1–Q4) + 6 deliberate divergences (see research.md §3).
- [ ] [research.md §5](./research.md) records the two source-code-truth
      verifications (OGC-761 absent / OGC-746 absent).
- [ ] [../OGC-284-barcode-label-quantity-management/spec.md](../OGC-284-barcode-label-quantity-management/spec.md)
      first paragraph carries the "Superseded by OGC-285" banner.
- [ ] Jira [OGC-285](https://uwdigi.atlassian.net/browse/OGC-285) shows status
      **In Progress** + comment #28885 announcing the FRS §5 divergence.
- [ ] PR #3628 has a non-Copilot human reviewer's `APPROVED` review before
      merge.

---

## M2 — Schema, Hibernate entities, migration

**Branch:** `feat/ogc-285-m2-schema-migration`

### Tests-precede-implementation evidence

- [ ] `LabelPresetOrmValidationTest` exists and is RED before any entity
      annotation lands.
- [ ] `MigrationDataIntegrityTest` exists and is RED before the seed changeset
      lands.

### Backend (Liquibase)

```bash
# Fresh DB
dropdb clinlims && createdb clinlims
mvn liquibase:update

# Inspect new tables
psql clinlims -c "\d clinlims.label_preset"
psql clinlims -c "\d clinlims.label_preset_field"
psql clinlims -c "\d clinlims.test_label_config"
psql clinlims -c "\d clinlims.test_label_preset_link"
psql clinlims -c "\d clinlims.order_label_request"

# Confirm seed presets
psql clinlims -c "SELECT id, name, is_system, prints_per_order, prints_per_sample, default_per_order, max_per_order, default_per_sample, max_per_sample FROM clinlims.label_preset WHERE is_system = true ORDER BY id;"
```

- [ ] 5 system preset rows: Order Label, Specimen Label, Block Label, Slide
      Label, Freezer Label.
- [ ] Order Label row has `prints_per_order = true`,
      `prints_per_sample = false`, `default_per_order` / `max_per_order`
      populated from `site_information.barcode.order.{default,max}`.
- [ ] Specimen/Block/Slide/Freezer rows have `prints_per_order = false`,
      `prints_per_sample = true`, `default_per_sample` / `max_per_sample`
      populated from the respective `site_information.barcode.*` keys.
- [ ] `label_preset_field` rows: each system preset has at minimum a
      `LAB_NUMBER` row with `is_required = true` and `display_order = 1`.
      Optional v1 checkbox carry-overs appear with their own rows.

### Rollback

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=2
psql clinlims -c "\d clinlims.label_preset" 2>&1 | grep -q "Did not find any relation"
```

- [ ] Rollback cleanly removes the new tables.
- [ ] No data left over (orphaned sequences / indexes).

### ORM validation

```bash
mvn test -Dtest=LabelPresetOrmValidationTest
```

- [ ] Test runs in <5s and does NOT require a DB connection (Constitution V.4).
- [ ] Validates the @Entity-to-@Table mapping for all 5 new entities
      (LabelPreset, LabelPresetField, TestLabelConfig, TestLabelPresetLink,
      OrderLabelRequest).

### Migration against v1 fixture

```bash
psql clinlims < src/test/resources/fixtures/v1-barcode-config.sql
mvn liquibase:update

# Verify seeded values match the fixture
psql clinlims -c "SELECT name, default_per_order, max_per_order, default_per_sample, max_per_sample FROM clinlims.label_preset WHERE is_system = true;"
```

- [ ] Seeded values match the v1 fixture's `site_information.barcode.*` rows.
- [ ] Malformed v1 fixture values (intentionally injected non-numerics) get the
      canonical fallback (default `1`, max `10`) and emit warnings.

---

## M3 — Label Preset CRUD + Master Lists admin + LEGACY PAGE DELETION

**Branch:** `feat/ogc-285-m3-preset-admin-crud`

### Pre-merge code-truth gate

```bash
# BarcodeConfiguration.jsx MUST be deleted (Principle X)
[ ! -f frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.jsx ] && echo PASS || echo FAIL

# Legacy controller endpoints for qty/dim/element MUST be gone
grep -rE "/rest/barcodeConfig.*qty|barcodeLabelElement" src/main/java/ && echo FAIL || echo PASS
```

- [ ] `BarcodeConfiguration.jsx` no longer exists.
- [ ] Backend `BarcodeConfigurationRestController.java` qty/dim/element
      endpoints removed (preprinted-prefix endpoint may remain transitionally or
      move to new controller).

### Admin UI — Master Lists → Label Presets

Navigate: **Administration → Master Lists → Label Presets**.

- [ ] **AC-1**: Page loads showing exactly 5 system presets (Order Label /
      Specimen Label / Block Label / Slide Label / Freezer Label) labeled
      "System" + tagged as Active.
- [ ] **AC-2**: Click "+ Add Label Preset". Modal opens. Enter: - Name:
      `Cryo Vial Label` - Height (mm): 25 - Width (mm): 25 - Barcode type: QR -
      Content fields: Lab Number (locked, required) + Storage Location + Expiry
      Date - Scope: Per sample, default 1, max 4 Click Save. Modal closes; new
      row appears in the list.
- [ ] **AC-3**: Open the "Order Label" system preset. Attempt to deactivate via
      the Active toggle. Save fails with an inline error referencing
      `is_system = true`. Name field is read-only.
- [ ] **AC-4**: Click "+ Add Label Preset". Type `Cryo Vial Label` (exact same
      name). Tab away from the field. Inline "name must be unique" error
      displays. Try `cryo vial label` (different casing) — same error
      (per-clarify Q3, case-insensitive). Try `Cryo Vial Label` (with
      whitespace) — same error (trim).
- [ ] **AC-5**: Deactivate Cryo Vial Label. Confirm it disappears from the list
      filter "Active" but appears in "Inactive".
- [ ] **AC-6**: Open Cryo Vial Label → click "Save as new" → keep name → Save.
      Validation error requires entering a new name.
- [ ] **AC-7**: Open Cryo Vial Label → set `default_per_sample = 5`,
      `max_per_sample = 3`. Save fails with "max must be ≥ default" field error.
      Also try unchecking both scope flags — save fails with "at least one scope
      required".

### Preprinted Accession Number consolidation

- [ ] Above the preset list, a "Site-wide Barcode Settings" section renders the
      `prePrintDontUseAltAccession` toggle + `prePrintAltAccessionPrefix` input.
      Toggling and saving writes to `site_information.barcode.preprinted.*` keys
      (verify with
      `psql clinlims -c "SELECT * FROM clinlims.site_information WHERE name LIKE 'barcode.preprinted.%';"`).

### Legacy URL redirect

- [ ] Visit
      `http://localhost:8080/OpenELIS-Global/MasterListsPage#barcodeConfiguration`
      — redirects to `#labelPresets`.

### Playwright demo spec (functional + video)

```bash
# Functional (ci-safe, no video) — what CI runs
cd frontend && npm run pw:test:core-demo -- ogc-285-label-preset-admin

# Video evidence (local) — produces MP4 under frontend/test-results/
cd frontend && npm run pw:test:core-demo-video -- ogc-285-label-preset-admin
```

- [ ] All assertions pass: AC-1, AC-2, AC-3, AC-5, AC-6, AC-7.
- [ ] **US1 demo video** produced and attached to PR / Jira OGC-285 as visible
      proof.

---

## M4 — Test Catalog Labels tab

**Branch:** `feat/ogc-285-m4-test-catalog-labels`

### Admin UI — Test Editor → Labels tab

Navigate: **Administration → Test Catalog → (choose CBC with Differential) →
Edit → Labels tab**.

- [ ] **AC-8**: Labels SideNav entry appears in the Test Editor (or, until
      OGC-746 lands, in the temporary `<Tabs>` host inside ViewTestCatalog that
      this milestone introduces).
- [ ] **AC-9**: Click "+ Add Label Type". Picker shows only active per-sample
      presets (Specimen Label / Block Label / Slide Label / Freezer Label + any
      custom per-sample presets). Order Label is excluded. Link Specimen Label
      with default 1, max 5, allow_override = on. Link Slide Label with default
      4, max 12, allow_override = off. Save. Both rows persist on reload.
- [ ] **AC-10**: Order Entry Preview card below the table renders a
      `<StructuredList>` showing the two linked presets with their default qty +
      lock indicator (Slide Label shows a lock icon for
      `allow_override = false`).
- [ ] **AC-11**: Click "+ Add Label Type" again. Specimen Label is excluded from
      the picker (already linked).
- [ ] **AC-12**: Toggle the master "Allow label count override at order entry"
      switch above the table to OFF. All per-link Allow Override checkboxes go
      disabled + forced-off. Save and reload — state persists.

### Backend integration test

```bash
mvn test -Dtest=TestLabelConfigRestControllerTest
```

- [ ] PUT request with duplicate `preset_id` rejected (409 or 422 per contract).
- [ ] PUT request linking an order-only preset (`prints_per_sample = false`)
      rejected.
- [ ] Master toggle off → response shape returns `allow_override = false`
      effectively (forced-off semantics).

### Playwright demo spec (functional + video)

```bash
cd frontend && npm run pw:test:core-demo -- ogc-285-test-catalog-labels
cd frontend && npm run pw:test:core-demo-video -- ogc-285-test-catalog-labels
```

- [ ] **US2 demo video** attached as visible proof.

---

## M5a — Order Entry backend (aggregation + JSONB persistence) + `BarcodeWorkflowPrintServiceImpl` deletion

**Branch:** `feat/ogc-285-m5a-order-entry-backend`.

### Pre-merge code-truth gates (Principle X)

```bash
# BarcodeWorkflowPrintServiceImpl.java MUST be deleted
[ ! -f src/main/java/org/openelisglobal/barcode/service/BarcodeWorkflowPrintServiceImpl.java ] && echo PASS || echo FAIL

# Interface (if present) also deleted
[ ! -f src/main/java/org/openelisglobal/barcode/service/BarcodeWorkflowPrintService.java ] && echo PASS || echo FAIL

# No remaining references
grep -rE "BarcodeWorkflowPrintService" src/main/java/ && echo FAIL || echo PASS
```

### Backend aggregation test

```bash
mvn test -Dtest='OrderEntryLabelRequest*,OrderLabelRequest*'
```

Cover:

- AC-16 most-restrictive `allow_override` precedence
- AC-17 highest `default_qty` wins
- AC-19 JSONB snapshot shape (`PresetSnapshotDto` matching FRS §7.3.1)

### JSONB round-trip

`mvn test -Dtest=PresetSnapshotJsonbRoundtripTest` — real `JsonBinaryType`
UserType against the real DB.

## M5b — Order Entry frontend rewrite + workflow integration

**Branch:** `feat/ogc-285-m5-order-entry-v2`

### Pre-merge code-truth gate

```bash
# applicableLabelTypes: ["specimen"] hardcode MUST be removed
grep -rE 'applicableLabelTypes.*specimen' frontend/src/components/barcodeWorkflow/ && echo FAIL || echo PASS
```

- [ ] Grep returns no matches. OGC-284 retro gap is closed as a side effect of
      M5.

### Order Entry — Add Order step 4 (Labels)

Pre-setup (via API per the Playwright pattern):

- CBC test linked to Specimen Label (default 1, max 5, allow_override on).
- Tissue Biopsy test linked to Specimen Label (default 2, max 6, allow_override
  on) AND Slide Label (default 4, max 12, allow_override off).

Navigate: **Add Order → step 4 (Labels)**, select CBC + Tissue Biopsy, add 2
samples.

- [ ] **AC-13**: The Labels section renders TWO Carbon DataTables: - Order
      Labels: column for "Order Label" (system per-order preset). - Sample
      Labels: columns for Specimen Label, Block Label (system default fallback),
      Slide Label, Freezer Label (system default fallback). System-first order;
      alphabetical within system + within custom. Two rows (one per sample).
- [ ] **AC-14**: Each Sample Labels cell shows a small `<Tag>` chip: - Specimen
      Label cells: "from Tissue Biopsy" (since 2 > 1 of CBC). - Slide Label
      cells: "from Tissue Biopsy". - Block Label / Freezer Label cells: "system
      default".
- [ ] **AC-15**: Slide Label cells (Tissue Biopsy's `allow_override = off`)
      render as read-only `<NumberInput>` with a lock icon. Tooltip on hover
      reads "Quantity locked by test catalog (locked by Tissue Biopsy)".
- [ ] **AC-16**: Most-restrictive: link Specimen Label to a second CBC test run
      with `allow_override = false`. Cell now locks even though Tissue Biopsy's
      link has `allow_override = true`.
- [ ] **AC-17**: Specimen Label cell pre-populates at **2** (highest default
      across linked tests).
- [ ] **AC-18**: Modify an unlocked cell from 2 → 3. Total row at bottom updates
      immediately.
- [ ] **AC-19**: Click "Save order & queue labels".
      `psql clinlims -c "SELECT order_id, sample_id, preset_id, qty, preset_snapshot FROM clinlims.order_label_request ORDER BY id DESC LIMIT 10;"`
      shows new rows. `preset_snapshot` JSONB matches FRS §7.3.1 shape (preset
      block, fields array, test_link block or null).

### Backend aggregation test

```bash
mvn test -Dtest=OrderEntryLabelRequestServiceAggregationTest
```

- [ ] Conflict-resolution tests cover: highest-default (AC-17), highest-max,
      most-restrictive-allow-override (AC-16), system-default fallback, column
      ordering (system-first then alphabetical).

### Playwright demo spec (functional + video)

```bash
cd frontend && npm run pw:test:core-demo -- ogc-285-order-entry-labels
cd frontend && npm run pw:test:core-demo-video -- ogc-285-order-entry-labels
```

- [ ] **US3 + US4 demo video** attached as visible proof (also shows OGC-284
      hardcode closure).

---

## M6 — Post-save dialog + reprint via snapshot

**Branch:** `feat/ogc-285-m6-postsave-dialog-reprint`

### Post-save dialog (immediately after Save order)

After saving an order with mixed test types (continuing M5 scenario):

- [ ] Dialog appears with: accession number tag, one row per preset with
      non-zero qty, Print button per row, "Skip — Print Later" button.
- [ ] Quantity column shows Carbon `<NumberInput>` (not `<p>`) with `min = 0`,
      `max = order_label_request.qty` at save time.
- [ ] Decrease a value (e.g., 3 → 1) and click Print. PDF opens in a new tab.
- [ ] `psql clinlims -c "SELECT qty FROM clinlims.order_label_request WHERE id = N;"`
      shows the **new** qty (1), per the decrease-only / overwrite semantics (Q1
      resolution).
- [ ] Try to **increase** above the original saved qty. NumberInput rejects the
      input (cannot go above max). Cell shows the max value.
- [ ] Click "Skip — Print Later". Dialog closes; the order is saved without any
      print job firing.

### Reprint from Order View

Navigate to the saved order's View page.

- [ ] Order View renders a "Print labels" action surface using the
      `GET /api/orders/{id}/labels` endpoint.
- [ ] Click a preset's Print button. PDF opens; dimensions / content fields
      reflect the SNAPSHOT.

### Snapshot frozen-on-reprint regression (AC-20)

Steps:

1. Save an order (note the order_id).
2. As admin, edit Specimen Label preset → change `height_mm` from 25 → 50.
3. As technician, go back to Order View → reprint Specimen Labels.

- [ ] **AC-20**: The reprinted PDF has the OLD dimensions (25 mm), not the new
      ones (50 mm). The snapshot is frozen.

### Backend regression test

```bash
mvn test -Dtest=ReprintFromSnapshotRegressionTest
```

- [ ] Test mutates the linked preset post-save, calls the reprint endpoint, and
      asserts the PDF content uses the snapshot, NOT the mutated preset.

### Playwright demo spec (functional + video)

```bash
cd frontend && npm run pw:test:core-demo -- ogc-285-reprint-from-snapshot
cd frontend && npm run pw:test:core-demo-video -- ogc-285-reprint-from-snapshot
```

- [ ] **US5 demo video** attached as visible proof of snapshot-frozen reprint.

---

## Cross-milestone smoke (end-to-end)

After M6 ships, run the full smoke loop:

```bash
# Pristine build + DB
dropdb clinlims && createdb clinlims
mvn clean install -DskipTests -Dmaven.test.skip=true
mvn liquibase:update

# Run full E2E suite
cd frontend && npm run pw:test:core-demo
```

- [ ] All OGC-285 demo specs green: `ogc-285-label-preset-admin`,
      `ogc-285-test-catalog-labels`, `ogc-285-order-entry-labels`,
      `ogc-285-reprint-from-snapshot`.
- [ ] **All 4 user-story demo videos** recorded via `pw:test:core-demo-video`
      and attached to the final OGC-285 closeout (PR body / Jira). One MP4 per
      user story (US1, US2, US3+US4, US5).
- [ ] Backend test coverage ≥80% on `org.openelisglobal.labelpreset.*` (verify
      via JaCoCo report).
- [ ] Frontend test coverage ≥70% on the new admin components and rewritten
      `barcodeWorkflow/` files.
- [ ] No regressions in existing OpenELIS suites (run `mvn test` end-to-end).
- [ ] No new instances of `applicableLabelTypes` or
      `prePrintDontUseAltAccession` outside the new Label Presets admin tree
      (confirms legacy code removal per Principle X).

## References

- [spec.md](./spec.md) · [plan.md](./plan.md) · [research.md](./research.md)
- [data-model.md](./data-model.md) ·
  [contracts/openapi.yaml](./contracts/openapi.yaml)
- [Canonical FRS @ 7cf6f65](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md)
- [Constitution Principle X (Legacy Code Removal)](/.specify/memory/constitution.md)
- [Playwright Best Practices](/.specify/guides/playwright-best-practices.md)
