# Research: OGC-285 Barcode Labels v2 — Configurable Label Preset Management

**Last updated:** 2026-05-19 (after `/speckit.clarify` + `/speckit.plan`) **FRS
source of truth pin:**
[`7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5`](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md)

## 1. Canonical FRS pin (snapshot discipline)

The OGC-285 engineering contract pins the upstream FRS at a specific commit SHA
in the `DIGI-UW/openelis-work` repository. This is deliberate: the FRS is a
separate, design-owned artifact that evolves on its own schedule, and the
engineering spec needs an unambiguous source of truth that cannot drift
silently.

**Current pin:**

| Field                          | Value                                                                            |
| ------------------------------ | -------------------------------------------------------------------------------- |
| Repository                     | `DIGI-UW/openelis-work`                                                          |
| Path                           | `designs/admin-config/barcode-labels.md`                                         |
| SHA                            | `7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5`                                       |
| FRS version                    | v2.5                                                                             |
| Date pinned                    | 2026-05-19 17:45 UTC                                                             |
| Commit message                 | `chore(admin-config): rename barcode-labels-v2 → barcode-labels, update to v2.5` |
| openelis-work HEAD at pin time | `49de90d807b0ef44865202acb54aa18f126ff299`                                       |

URL:
https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md

### Pin discipline protocol

The pin discipline is the only authorized path for FRS changes to enter this
engineering contract. The protocol:

1. **Detect drift.** On every spec PR, the reviewer confirms whether the
   upstream `barcode-labels.md` has moved beyond the pinned SHA. Use:

   ```bash
   gh api 'repos/DIGI-UW/openelis-work/commits?path=designs/admin-config/barcode-labels.md&per_page=1' \
     --jq '.[0].sha'
   ```

2. **Bump the SHA.** If a newer SHA exists, update the SHA in this research.md,
   [spec.md](./spec.md) frontmatter, [plan.md](./plan.md) and References.

3. **Diff the FRS.** Generate a diff between old and new SHA:

   ```bash
   gh api 'repos/DIGI-UW/openelis-work/compare/{OLD_SHA}...{NEW_SHA}' \
     --jq '.files[] | select(.filename == "designs/admin-config/barcode-labels.md") | .patch'
   ```

4. **Reconcile.** Walk the diff against the in-repo spec.md FRs and acceptance
   criteria; update any FR whose source FRS clause changed.

5. **Commit atomically.** The SHA bump + reconciliation lands in a single PR.

This is intentionally a manual process. Automating it would risk silent spec
drift — the opposite of the discipline OGC-285 was designed to avoid.

## 2. Resolved FRS Open Questions

The FRS v2.5 records four open questions in §11. Resolutions locked in spec.md;
full rationale below.

### Q1 — Snapshot vs current preset for reprint

**FRS text** (§11 Q1):

> Do we need a "label-template-version" column on `order_label_request` so
> reprint can choose between snapshot-at-save vs current preset? Snapshot is the
> safer default; some sites may want "always use current".

**Decision:** Snapshot only for v2. **Rationale:**

- AC-20 is the canonical statement; making it configurable would invalidate
  AC-20.
- Audit integrity outweighs operational convenience. Pathology, regulatory
  environments, and chain-of-custody tracking depend on label-as-printed
  matching order-as-shown.
- Implementation simplicity: one code path. Adding a per-site or per-reprint
  toggle doubles the reprint test matrix and adds an
  `order_label_request.template_version` column.
- No real customer has asked for "use current preset on reprint" — this is a
  hypothetical Q from the design author, not a stakeholder request.

**Alternatives considered:**

- Per-site toggle (`site_information.barcode.reprint_mode` = `SNAPSHOT` |
  `CURRENT`). Rejected: same code path doubling, no demand.
- Per-reprint user choice. Rejected: surfaces too many decisions at the reprint
  moment; admin should set policy, not technician.

**Deferred to v3+:** If a real site requests "use current preset on reprint",
add an `order_label_request.template_version` column with values
`SNAPSHOT_FROZEN` (default) and `LIVE_FROM_PRESET`.

### Q2 — Allow-override toggle home

**FRS text** (§11 Q2):

> What's the right home for the "Allow label count override at order entry"
> master toggle — per-test (current proposal), per-lab, or both?

**Decision:** Per-test only (no per-lab layer in v2). **Rationale:**

- The 2-layer precedence (per-link + per-test master) handles common
  - gradient cases without a third layer.
- Lab-wide lockdown is rare-but-real; the right tool today is a SQL bulk update
  across `test_label_config.allow_order_entry_override`.
- Three-layer precedence increases bug surface (6 interaction modes vs 3).
- YAGNI: no deployed site has requested a per-lab toggle.

**Alternatives considered:**

- Per-lab master toggle stored in `site_information.barcode.override_disabled`.
  Rejected: most-restrictive-wins logic across 3 layers is harder to reason
  about; no demand.
- Per-link only (no per-test master). Rejected: makes test-level lockdown
  require N edits (one per linked preset); the per-test master is the right
  granularity for compliance scenarios.

**Deferred to v3+:** If a real stakeholder asks for lab-wide lockdown, a third
layer slots above `test_label_config` with the same most- restrictive precedence
rule.

### Q3 — A11y drag-drop for content fields

**FRS text** (§11 Q3):

> How do we handle accessibility for the drag-handle on content fields when the
> user is dragging? Carbon doesn't ship a fully a11y drag-drop; do we use
> react-aria's useDrag or roll our own keyboard-only fallback?

**Decision:** Keyboard Arrow Up/Down + native HTML5 drag for mouse.
**Rationale:**

- Keyboard navigation is a hard requirement (FR-032). Arrow Up / Arrow Down on
  the focused row covers screen-reader-only users.
- Native HTML5 drag works for mouse users; the visual affordance (drag handle
  icon) plus native browser behavior covers pointer users.
- react-aria adds bundle size + dependency risk for one component.

**Alternatives considered:**

- react-aria `useDrag` / `useDrop`. Rejected: dependency cost, learning curve,
  only used in this one component.
- Custom React hook for keyboard reorder. Same approach as Decision but written
  from scratch. Rejected: re-invents standard browser behavior.

**Deferred to v3+:** If a WCAG 2.2 AA audit flags drag-handle issues, revisit
with react-aria or similar a11y drag library.

### Q4 — Future custom-field shape (v3+ planning)

**FRS text** (§11 Q4):

> When v3+ reintroduces user-defined custom fields, do we ship them as
> `CUSTOM_FREETEXT` / `CUSTOM_FIXED` source types under the existing
> `label_preset_field.source_type` column, or as a separate
> `label_preset_custom_field` table? Affects the v2 schema shape we lock in
> here.

**Decision:** Single `source_type` column (v2 schema unchanged). **Rationale:**

- The v2 `label_preset_field.source_type` column is already constrained to
  `'SYSTEM'`; the column exists to allow v3+ to introduce additional source
  types without an `ALTER TYPE` round trip.
- Single-table read path is simpler than join.
- Storage cost is negligible.

**Alternatives considered:**

- Separate `label_preset_custom_field` table. Rejected: forces a join on every
  read; doesn't add safety; doubles the schema surface.
- Remove `source_type` for v2 and add later. Rejected: future migration would
  need backfill; cheaper to keep the column now.

**Deferred to v3+:** Migration adds new values to the
`label_preset_field_source_type_check` constraint and a new `field_value` JSONB
column for custom-field payloads.

## 3. Deliberate divergences from the upstream FRS

Each divergence is documented for audit purposes. Divergences require either (a)
constitution compliance or (b) explicit stakeholder acknowledgment (e.g., Jira
comment).

### Divergence 1 — Editable post-save quantities (LOCKED YES)

**FRS markdown** (§4.6, §1.5): silent on editability. §1.5 lists "Manual-confirm
per-preset mode" as out-of-scope; §4.6 specifies dynamic preset listing +
per-preset Print buttons but does not address editability.

**Jira description** (updated 2026-05-19 10:42 PT):

> "**Editable post-save quantities**: technicians can adjust counts in the
> post-save dialog before pressing Print."

**Locked engineering decision** (further tightened in `/speckit.clarify` Q1,
2026-05-19): **Decrease-only**, audit-bound at saved qty.

- Carbon `<NumberInput min=0 max=order_label_request.qty>`.
- The Print action overwrites `order_label_request.qty` to the new (lower)
  value.
- To print MORE labels, re-open the order and re-save with a higher qty before
  printing (separable workflow).
- No separate `actual_printed_qty` audit column needed (saved qty IS the audit
  ceiling).

**Rationale:**

- Jira is the more recent source; tightens the FRS markdown.
- Closes the OGC-284 dialog gap (currently quantity is a static `<p>`).
- Audit-bound max prevents accidentally exceeding the saved quantity;
  decrease-only respects max-limit enforcement semantics.

**Follow-up:** Suggest the design author update FRS markdown §4.6 to match the
Jira description in a future FRS revision.

### Divergence 2 — OGC-284 Order Entry quantity UI gap is ABSORBED, not deferred

**FRS markdown** (§1.5 Out of Scope):

> "The OGC-284 Order Entry quantity UI gap. Tracked separately — see
> [OGC-284 cohesive FRS §10](./barcode-config.md)."

**Locked engineering decision:** ABSORBED into US4 (Migration & continuity).

M5's LabelsSection rewrite replaces
[`frontend/src/components/barcodeWorkflow/LabelsSection.jsx`](/frontend/src/components/barcodeWorkflow/LabelsSection.jsx)
entirely. The current file (lines 30–42) hardcodes
`applicableLabelTypes: ["specimen"]`; the M5 rewrite makes the file render two
dynamic-column tables driven by linked presets. The hardcode disappears as a
side effect.

**Rationale:**

- Same code is rewritten anyway. Deferring the OGC-284 fix to a separate ticket
  means writing a temporary patch on a file the M5 rewrite is about to replace.
  Throwaway code is worse than no code.
- One reviewable rewrite beats two patches.
- OGC-284 retro closure is faster (M5 merge, not a follow-up ticket).

The OGC-284 Gap Closure Matrix at
[../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix](../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix)
records this absorption row-by-row.

### Divergence 3 — Legacy BarcodeConfiguration page DELETED in M3

**FRS markdown** (§5):

> "**This release does NOT move, rebuild, or supersede that surface.** Label
> Presets is the new per-preset admin surface; the existing Barcode
> Configuration page continues to host site-wide barcode settings unchanged."

**Constitution constraint:** Principle X (Legacy Code Removal) — "no dual-write,
no legacy-first". Leaving a parallel BarcodeConfiguration page (even one trimmed
to just the Preprinted Accession Number controls) violates this principle.

**Locked engineering decision** (`/speckit.clarify` Q2, 2026-05-19):

- M3 deletes
  [`frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.jsx`](/frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.jsx)
  entirely (1396 LOC).
- Backend `BarcodeConfigurationRestController.java` qty/dim/element endpoints
  are removed in the same PR.
- The Preprinted Accession Number controls (`prePrintDontUseAltAccession`
  toggle + `prePrintAltAccessionPrefix` input) move into the new Master Lists →
  Label Presets surface as a "Site-wide Barcode Settings" section above the
  preset list.
- `site_information.barcode.preprinted.*` keys keep their existing semantics;
  only the UI host moves.
- A redirect from `/MasterListsPage#barcodeConfiguration` →
  `/MasterListsPage#labelPresets` preserves bookmark continuity for one release
  cycle.

**Rationale:** Constitution Principle X mandates removal of the legacy surface
when superseded. The single consolidated admin page matches the "single clear
place for a single clear workflow" principle the user articulated.

**Stakeholder communication:** Divergence announced via
[Jira comment #28885](https://uwdigi.atlassian.net/browse/OGC-285?focusedCommentId=28885)
on OGC-285 (2026-05-19). Design team can flag in that thread if they have a
reason to keep parallel pages; otherwise the lock takes effect in the M3 PR.

### Divergence 4 — Legacy `BarcodeConfigurationRestController` DELETED in M3; new `SiteWideBarcodeSettingsRestController` hosts preprinted endpoints

**FRS markdown** (§5): silent on backend controller structure.

**Constitution constraint:** Principle X (Legacy Code Removal). Leaving a
parallel `BarcodeConfigurationRestController` after the new
`LabelPresetRestController` ships violates "no dual-write, no legacy-first".

**Locked engineering decision** (remediation pass 2026-05-19):

- M3 PR **deletes**
  `src/main/java/org/openelisglobal/barcode/controller/rest/BarcodeConfigurationRestController.java`
  entirely.
- A new
  `src/main/java/org/openelisglobal/labelpreset/controller/rest/SiteWideBarcodeSettingsRestController.java`
  hosts the `barcode.preprinted.use_order_entry_format` toggle +
  `barcode.preprinted.prefix` endpoints (the ONLY surviving site-wide barcode
  setting).
- `site_information.barcode.preprinted.*` keys keep their existing semantics;
  only the controller (and the UI host inside Master Lists → Label Presets)
  move.
- Scope: `admin.barcode.manage` on the new endpoints.

**Rationale:** preset CRUD ≠ site-wide settings — separate concerns deserve
separate controllers. Mixing them in `LabelPresetRestController` would conflate
the surfaces a future engineer reads.

### Divergence 5 — `BarcodeWorkflowPrintServiceImpl` DELETED in M5a

**FRS markdown** (§4): describes the v2 aggregation logic but does not
explicitly call out the fate of the v1 service.

**Constitution constraint:** Principle X. After M5a introduces
`OrderEntryLabelRequestService` as the authoritative aggregator, keeping
`BarcodeWorkflowPrintServiceImpl.java` alive as a thin wrapper (or a parallel
path) violates "no dual-write".

**Locked engineering decision** (remediation pass 2026-05-19):

- M5a PR **deletes**
  `src/main/java/org/openelisglobal/barcode/service/BarcodeWorkflowPrintServiceImpl.java`
  entirely along with its interface `BarcodeWorkflowPrintService.java` if
  present.
- Any remaining callers in the codebase are updated to call
  `OrderEntryLabelRequestService` directly.
- Grep gate:
  `grep -rE 'BarcodeWorkflowPrintService' src/main/java/ && exit 1 || exit 0`
  MUST pass in the M5a PR.

**Rationale:** v1's `applicableLabelTypes: ["specimen"]` hardcode lived at
`BarcodeWorkflowPrintServiceImpl.java:43`. Deleting the file in M5a is the only
way to guarantee that hardcode does not survive M5 in any form.

### Divergence 6 — Phase A legacy modernization bundled into M2

**FRS markdown:** silent on Hibernate mapping format of legacy entities.

**Constitution constraint:** Principle X — "Mandate to address legacy/deprecated
code when touched". CLAUDE.md is explicit:

> "Valueholders MUST use JPA/Hibernate annotations (NO XML mapping files -
> legacy exempt until refactored)"

OGC-285 touches the `sample`, `sample_item`, and `test` tables via FK
relationships from the new `order_label_request`, `test_label_config`, and
`test_label_preset_link` tables. Their valueholders (`Sample.java`,
`SampleItem.java`, `Test.java`) are currently XML-mapped via `.hbm.xml` files
(`src/main/resources/hibernate/hbm/{Sample,SampleItem,Test}.hbm.xml`, 365 lines
total). M2 is the natural opportunity to retire these XML mappings.

**Locked engineering decision** (remediation pass round 2, 2026-05-19):

M2 PR bundles **Phase A modernization**:

1. Re-annotate `src/main/java/org/openelisglobal/sample/valueholder/Sample.java`
   with JPA annotations (`@Entity`, `@Table`, `@Id` with the existing
   `StringSequenceGenerator` referenced via `@GenericGenerator`, `@Column` for
   each field, `@ManyToOne` for each relationship matching the existing
   `<many-to-one>` mappings, `@Version` already inherited from
   `BaseObject<String>`).
2. Same for
   `src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`.
3. Same for `src/main/java/org/openelisglobal/test/valueholder/Test.java`.
4. Keep `String id` + `@Type(LIMSStringNumberUserType)` for the bridge — Phase A
   is annotation format only, NOT schema change.
5. Delete `src/main/resources/hibernate/hbm/Sample.hbm.xml`,
   `SampleItem.hbm.xml`, `Test.hbm.xml`.
6. Update `src/main/resources/hibernate/hibernate.cfg.xml` to remove the
   now-deleted `<mapping resource>` entries.

**Verification:**

- Grep gate:
  `find src/main/resources/hibernate/hbm -name "{Sample,SampleItem,Test}.hbm.xml" && exit 1 || exit 0`.
- Hibernate boot validates JPA mappings at startup; broken annotations fail
  loudly.
- Run full existing test suite (`mvn test`) — passes green.
- Spot-check a few critical queries against the modernized entities (e.g.,
  `Sample` load + lazy-load a relationship).

**Rationale:** CLAUDE.md explicitly requires this when legacy is touched;
OGC-285 is the touching event; the 3 .hbm.xml files are tractable (~15-20
fields/relationships each, all with modern JPA equivalents); 61 entities already
use JPA annotations as templates.

**Phase B (deferred to follow-up ticket; not yet filed):** the `numeric(10,0)` →
`INTEGER` schema modernization for `sample.id` / `sample_item.id` / `test.id` is
a separate epic. Cost: ALTER COLUMN on 3 parents + 9 FK referencing columns +
migrate hundreds of consumers from `String id` to `Integer id`. **Filed as a
precursor for the next major schema simplification effort (Jira ticket TBD;
create post-OGC-285 M2 merge).** This is the single canonical reference; other
mentions in spec.md / plan.md point here.

## 4. Clarifications integrated from `/speckit.clarify` Session 2026-05-19

Three additional decisions locked in spec.md beyond the FRS Open Questions:

### Clarification — Post-save quantity edit bounds

**Decision** (folded into Divergence 1 above): Decrease-only, audit-bound at
saved qty. See spec.md FR-022 + Clarifications section.

### Clarification — Legacy BarcodeConfiguration page lifecycle after M3

**Decision** (folded into Divergence 3 above): Consolidate (constitution
Principle X).

### Clarification — Preset name uniqueness scope

**Decision:** Case-insensitive AND trim leading/trailing whitespace.

- Service layer normalizes via `name.trim().toLowerCase()` before uniqueness
  check.
- DDL `UNIQUE` constraint on `label_preset.name` serves as defense-in-depth
  (catches anything bypassing the service).
- "Cryo Vial", "cryo vial", "CRYO VIAL", " Cryo Vial " are all treated as the
  same name and collide on save.

**Rationale:** Prevents the most common admin error (typo/ capitalization
creating silent duplicates in pickers); standard pattern for human-facing admin
names (test names, user logins, etc.). FR-002 / AC-4 acceptance test must cover
all three normalization cases (case difference, leading whitespace, trailing
whitespace).

**Alternatives considered:**

- Exact-match only (DDL UNIQUE). Rejected: high typo-duplicate risk.
- Case-insensitive only (no whitespace trim). Rejected: leading/ trailing spaces
  are invisible to admin eyes.

## 5. Source-code-truth verifications (2026-05-19)

These are facts about the develop branch state at planning time. They inform M2
/ M4 scope and replace conditional language in spec.md Assumptions &
Constraints.

### Verification — OGC-761 `test_label_preset_link` table

**Method:**
`grep -rE "test_label_preset_link|test_label_config|label_preset" src/main/resources/liquibase/`
returns no matches. **Conclusion:** Table does NOT exist on develop. The FRS
§3.5 assumption "introduced by OGC-761" is not yet realized. **Impact:** M2
Liquibase changeset creates the full table from scratch with all v2 columns
including `allow_override`. The FRS §3.5 `ALTER` becomes a `CREATE`.

### Verification — OGC-746 Test Editor scaffold

**Method:** `find frontend/src/components/admin/testManagement -name "*.jsx"`
returns only `ManageMethod.jsx` and `ViewTestCatalog.jsx`. No `TestEditor.jsx`
or similar scaffold exists. **Conclusion:** OGC-746 scaffold not yet on develop.
**Impact:** M4 frontend ships a temporary Carbon `<Tabs>` host in
`ViewTestCatalog.jsx` hosting the Labels tab; migrate to the OGC-746 scaffold
when it ships. M4 backend is unblocked.

### Verification — BarcodeConfiguration.jsx hosts Preprinted controls

**Method:**
`grep -iE "prePrint|preprint" frontend/src/components/admin/barcodeConfiguration/BarcodeConfiguration.jsx`
returns `prePrintDontUseAltAccession`, `setPrePrintDontUseAltAccession`,
`prePrintAltAccessionPrefix`, `handleSitePrefixPrePrintedValue`. **Conclusion:**
The 1396-LOC `BarcodeConfiguration.jsx` does contain the Preprinted Accession
Number toggle + prefix controls (FRS §5 is correct about that). M3 migration
plan must extract these controls into the new Label Presets surface BEFORE
deleting the file. **Impact:** M3 PR scope includes the controlled migration of
the Preprinted controls to the new surface, not just deletion.

### Verification — Locale files inventory

**Method:** `ls frontend/src/languages/`. **Conclusion:** 20 locale JSON files
exist (`am_ET`, `ar`, `bg`, `de`, `en`, `en_GB`, `en_LK`, `en_US`, `es`, `fr`,
`id`, `id_ID`, `mg`, `ro`, `ru`, `si`, `si_LK`, `sw`, `ta`). Per durable memory
rule "Transifex manages translations", only `en.json` may be edited in PRs; the
other 19 are Transifex-managed. **Impact:** All milestone PRs MUST edit ONLY
`en.json` for new keys. Reviewer enforces; no CI gate currently catches
accidental edits.

### Verification (NEW, 2026-05-19) — Entity ID convention: `BaseObject<Integer>` for new entities

**Method:** Read
`src/main/java/org/openelisglobal/barcode/valueholder/SampleBarcodeInfo.java`
(the canonical OGC-284 new-pattern entity). **Conclusion:** New JPA-annotated
entities use `BaseObject<Integer>` + `Integer id` +
`@SequenceGenerator(sequenceName = "{table}_seq", allocationSize = 1)` +
`INTEGER` DB column. Legacy entities (e.g., `Sample`) use `String id` +
`numeric(10,0)` + `LIMSStringNumberUserType` bridge. The `Sample.java` file at
`src/main/java/org/openelisglobal/sample/valueholder/Sample.java` has NO
`@Entity` or `@Id` annotation (legacy XML mapping pattern). **Impact:** All 5
OGC-285 entities follow the SampleBarcodeInfo pattern: `BaseObject<Integer>` +
`Integer id` with sequence-driven generation. No exceptions; no
`BaseObject<String>` carve-outs. For FK references to legacy tables (`sample`,
`sample_item`, `test`), declare a `@ManyToOne` (or `@OneToOne` for 1:1
associations) JPA relationship to the legacy entity — Hibernate routes through
the legacy `LIMSStringNumberUserType` automatically at the relationship
boundary. For 1:1 associations like `TestLabelConfig` ↔ `Test`, the entity has
its own Integer surrogate PK with `@JoinColumn(name = "test_id", unique = true)`
on the FK column (mirrors how `SampleBarcodeInfo` associates 1:1 with `Sample`).
See [data-model.md §5](./data-model.md) for the full pattern.

### Verification (NEW, 2026-05-19) — Audit timestamp + optimistic locking inherited from `BaseObject`

**Method:** Read
`src/main/java/org/openelisglobal/common/valueholder/BaseObject.java`.
**Conclusion:** `BaseObject<PK>` declares
`@Column(name = "last_updated") @Version private Timestamp lastupdated;`. All
entities extending it inherit:

- Optimistic-locking column `last_updated TIMESTAMP` (no TZ).
- Java type `java.sql.Timestamp` (NOT `OffsetDateTime`, NOT `LocalDateTime`).
- `@Version` on `lastupdated` for Hibernate's optimistic concurrency control.
  **Impact:** OGC-285 entity declarations MUST NOT add their own `created_at` /
  `updated_at` columns or `@CreationTimestamp` / `@UpdateTimestamp` annotations.
  They inherit `lastupdated` from `BaseObject`. The Liquibase DDL DOES include
  `<column name="last_updated" type="TIMESTAMP"/>` on every new table (matches
  OGC-284 `028-barcode-info-tables.xml`).

### Verification (NEW, 2026-05-19) — Test infrastructure: `BaseWebContextSensitiveTest` + DBUnit

**Method:** Read
`src/test/java/org/openelisglobal/BaseWebContextSensitiveTest.java`.
**Conclusion:**

- Extends `AbstractTransactionalJUnit4SpringContextTests` (JUnit 4, NOT JUnit
  5).
- Uses DBUnit with `org.dbunit.ext.postgresql.PostgresqlDataTypeFactory` for
  fixture loading via `FlatXmlDataSet`.
- Class-default `@Transactional(propagation = Propagation.NOT_SUPPORTED)`
  (per-test rollback opt-in).
- `@ContextConfiguration(classes = { BaseTestConfig.class, AppTestConfig.class })`
  is the test-context pattern.
- `MockMvc` is built via `MockMvcBuilders.webAppContextSetup(...)`. **Impact:**
  OGC-285 backend integration tests MUST extend `BaseWebContextSensitiveTest`
  (not invent their own test base). Test fixtures are `.xml` DBUnit format under
  `src/test/resources/fixtures/ogc-285/`, NOT `.sql` files or Hibernate
  builders. Reuse `BaseTestConfig` + `AppTestConfig` for
  `@ContextConfiguration`; do not introduce new test config classes.

### Verification (NEW, 2026-05-19) — JSONB binding: existing `JsonBinaryType` UserType

**Method:** `find src/main -name 'JsonBinaryType.java'`; grep usage.
**Conclusion:**
`src/main/java/org/openelisglobal/hibernate/type/JsonBinaryType.java` exists and
is used by `Alert`
(`src/main/java/org/openelisglobal/alert/valueholder/Alert.java`) and
`PatientMergeAudit`. Pattern:

```java
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Alert extends BaseObject<Integer> {
    @Type(type = "jsonb")
    @Column(name = "..." columnDefinition = "jsonb")
    private MyDto data;
}
```

**Impact:** OGC-285's `OrderLabelRequest.preset_snapshot` JSONB column uses the
SAME `JsonBinaryType` pattern. We do NOT introduce Hibernate 6.x's native
`@JdbcTypeCode(SqlTypes.JSON)` — that's a different binding mechanism not yet
adopted in this codebase. Keep consistent with `Alert` + `PatientMergeAudit`.

### Verification (NEW, 2026-05-19) — `npm run pw:test` script + variants

**Method:** Read `frontend/package.json` scripts block. **Conclusion:**
`pw:test` exists as `"pw:test": "playwright test"`. 12 additional `pw:test:*`
variants for project-specific test buckets (harness-demo, core-app, etc.).
**Impact:** OGC-285 Playwright tests run via `npm run pw:test -- {spec-file}`
for one-offs, or `npm run pw:test:core-app` for the OE app project bucket.
CLAUDE.md "never raw `npx playwright test`" rule applies — always go through npm
scripts.

## 6. Workflow inventory (M5 scope)

The M5 LabelsSection rewrite must update every barcode-printing sample-creation
workflow in the codebase. Starting list, drawn from OGC-284's quickstart
workflow inventory + a 2026-05-19 audit:

| Workflow                             | Primary file                                                                                                            | M5 update needed                                                     |
| ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| Add Order (`/SamplePatientEntry`)    | [`frontend/src/components/addOrder/AddOrder.jsx`](/frontend/src/components/addOrder/AddOrder.jsx)                       | YES — primary surface for the two-table v2 layout.                   |
| Add Order Success Message            | [`frontend/src/components/addOrder/OrderSuccessMessage.jsx`](/frontend/src/components/addOrder/OrderSuccessMessage.jsx) | YES — pass `orderLabelRequests` to PostSavePrintDialog (M6).         |
| Order View (reprint surface)         | (TBD by audit during M5/M6 planning)                                                                                    | YES — wire reprint via snapshot endpoint.                            |
| Pathology workflow (if separate)     | (Audit during M5 planning)                                                                                              | UNKNOWN — verify in M5 prep.                                         |
| Generic Sample Order (FHIR endpoint) | [`src/main/java/org/openelisglobal/genericsample/`](/src/main/java/org/openelisglobal/genericsample/)                   | YES — backend orchestration accepts new label request payload shape. |

The M5 engineer audits the full list during T040..T043 planning and updates this
table with any newly discovered workflows. M5 cannot ship until every
barcode-printing workflow is on the v2 path.

## 7. Risks & unknowns

- **OGC-746 (Test Editor scaffold) landing date.** Verified absent 2026-05-19.
  M4 frontend ships the `<Tabs>` shim in `ViewTestCatalog.jsx`; migration to
  OGC-746 scaffold becomes a follow-up task when OGC-746 lands. M4 backend is
  unblocked.
- **`site_information.barcode.*` data quality across deployed sites.** Malformed
  numeric values (non-integer, negative, missing) must apply the canonical
  fallback per OGC-284 FR-004 (default `1`, max `10`). M2 includes a
  data-integrity test against a sample of production-like configs.
- **Snapshot JSONB shape evolution.** Future fields added to `label_preset` will
  NOT retro-appear in historical snapshots. This is documented in spec.md (US5
  acceptance scenarios) and in data-model.md §4.
- **Translation drift.** Non-English locale files are Transifex- managed; all
  milestone PR engineers MUST keep `frontend/src/languages/fr.json` and other
  locale files unmodified. CI does not currently catch accidental edits;
  reviewers verify in PR.
- **Performance under high test count.** Order Entry with 20+ tests could cause
  the aggregation function to fan out heavily. M5 performance target:
  `POST /api/orderEntry/labelRequest` < 100ms p95 with 20 tests. If not met, add
  server-side caching keyed by test_id_set.

## 8. References

- [Canonical FRS v2.5](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md)
- [FRS mockup](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.jsx)
- [FRS gallery permalink](https://digi-uw.github.io/openelis-work/#/admin-config/barcode-labels)
- [Jira OGC-285](https://uwdigi.atlassian.net/browse/OGC-285)
- [Jira OGC-285 comment #28885](https://uwdigi.atlassian.net/browse/OGC-285?focusedCommentId=28885)
  (FRS §5 divergence)
- [OGC-284 Gap Closure Matrix](../OGC-284-barcode-label-quantity-management/spec.md#gap-closure-matrix)
- [Constitution](/.specify/memory/constitution.md)
- [Testing Roadmap](/.specify/guides/testing-roadmap.md)
- [spec.md](./spec.md) · [plan.md](./plan.md) · [data-model.md](./data-model.md)
  · [contracts/openapi.yaml](./contracts/openapi.yaml) ·
  [quickstart.md](./quickstart.md)
