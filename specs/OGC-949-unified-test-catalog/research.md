# Research: Unified Test Catalog Management Editor (OGC-949)

Phase 0 research consolidating the session audit (2026-06-10). Format per
`/speckit.plan`: Decision / Rationale / Alternatives. This is the home for the
reconciliation findings that M0 acts on.

## R1 — `demo-silnas` divergence and port policy

**Decision**: All OGC-949 work targets `develop`. Port only test-catalog-scoped,
develop-clean commits from `demo-silnas`; defer wholesale reconciliation until
`demo-silnas` merges back.

**Findings**:

- `demo-silnas` is **~19 commits ahead** of develop (and ~5 behind), carrying
  vector surveillance, environmental/vector reception, NCE reporting, QC
  evaluation, and S-01 Compliance — a broad demo lane, not just test catalog.
- **Methods (OGC-750)** shipped there: PR
  [#3706](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3706) merged
  2026-06-10 into `demo-silnas` (commit `c9b623391`). Contents: `test_method`
  table + Liquibase `3.5.x.x/039-test-method-links.xml`, `Method.code` column,
  `TestMethod` valueholder, `TestMethodRestController` / `TestMethodService`,
  `MethodsSection.jsx`, and a `GET /rest/api/tests/{testId}/methods` endpoint on
  `DisplayListController`. This is the first qualifying **port** (M0/M6).
- **S-01 Compliance (OGC-528)** also lives on `demo-silnas` (PRs #3558/#3609).
  It does **not** qualify for porting through this feature — it reaches develop
  via its own PR [#3500](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3500).
  v2 Compliance (OGC-765 / M18) waits on that landing.

**Port policy (qualifying criteria)**: a commit may be ported iff (1) it belongs
to an OGC-949 epic, (2) it applies onto develop without dragging
vector/environmental/compliance/NCE dependencies, and (3) it carries its own
tests.

**Alternatives considered**: (a) dual-lane demo-first delivery — rejected: the
no-feature-flag, delete-legacy-at-release strategy assumes a single integration
branch; sections accruing on demo-silnas would turn the v1 release into a
cross-branch port and lag E2E coverage. (b) Merge demo-silnas to develop now —
rejected: out of OGC-949 scope and entangles unrelated demo work.

**M0 port outcome (2026-06-10)**: cherry-picking `c9b623391` onto develop
applied **15 of 17 files cleanly** (the entire `testmethod` backend package,
liquibase `039` + base.xml, `Method`/`Method.hbm.xml`, `MethodCreate*`,
`DisplayListController` endpoint, `MethodsSection.jsx`, 27 `en.json` keys). **Two
frontend files conflicted on demo-silnas entanglement and were NOT ported**:

- `TestModifyEntry.jsx` — the Methods commit mounts `MethodsSection` as a _Tab_
  inside demo-silnas's **compliance-Tabs** test-editor UI, which does not exist
  on develop (develop renders `TestStepForm` directly). The editor mount is
  therefore **deferred to M6** (wiring `MethodsSection` into the M2/OGC-927
  unified editor scaffold). `MethodsSection.jsx` ships orphaned until then.
- `SearchResultForm.jsx` — the conflict was NCE/holding-time notification logic,
  not Methods; took develop's version.

This validates the port policy: the Methods _backend_ qualified cleanly, but its
_frontend mount_ dragged a compliance dependency and was correctly excluded. It
also confirms the editor mount belongs to M6 (against the new scaffold), not to
the legacy `TestModifyEntry` that M-DC will decommission.

## R2 — OGC-285 ↔ OGC-761 (Labels) boundary

**Decision**: OGC-285 owns the label-preset data model; the v2 Labels section
(OGC-761) **consumes** it and builds only the editor tab. No parallel model.

**Findings**:

- The label-preset tables do **not** exist on develop yet.
- On `feat/ogc-285-integration` (PR
  [#3676](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3676)), Liquibase
  `3.3.x.x/029-label-preset-tables.xml` creates `label_preset`,
  `label_preset_field`, `test_label_config`, `test_label_preset_link`,
  `order_label_request`, with valueholders/DAOs/services under
  `org.openelisglobal.labelpreset` and REST controllers incl.
  `TestLabelConfigRestController` (`/rest/api/tests/{id}/labelConfig`).
- The `test_label_preset_link` table (`test_id`, `preset_id`, `default_qty`,
  `max_qty`, `allow_override`) is **exactly** what OGC-761 needs ("a test
  declares which presets it uses at what quantity"). Its changeset comment
  states it CREATEs the full table precisely because "OGC-761 has NOT landed on
  develop" — i.e. it was authored anticipating OGC-761 as the consumer.

**Implication for FR-D08**: v2 Labels is UI-only on top of OGC-285's schema. M0
records this; M14 (v2) must not introduce a second linkage model.

**Alternatives considered**: OGC-761 builds its own `test_label_preset_link` —
rejected: duplicates a table designed for it and would collide on the same name.

## R3 — Permission gate (FR-004)

**Decision**: Gate v1 on the existing `ADMIN` role
(`@PreAuthorize("hasRole('ADMIN')")` on the REST layer + UI hides the menu entry
for non-admins). `admin.testCatalog.manage` is a logical name, not a separate
grant. **No dependency** on the unmerged fine-grained RBAC work.

**Findings**:

- Every OE admin REST controller uses coarse `hasRole('ADMIN')` (8 distinct
  `@PreAuthorize` expressions exist project-wide, all role-based). REST endpoints
  bypass the DB-backed `SystemModule` page-permission machinery
  (`ModuleAuthenticationInterceptor` auto-allows authenticated REST requests
  lacking a `SystemModuleUrl` mapping).
- A fine-grained, privilege-based RBAC effort is **in flight but not merged**:
  [OGC-384](https://uwdigi.atlassian.net/browse/OGC-384) / PR
  [#3443](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3443) ("privilege-based
  RBAC with fine-grained service authorization").

**Decision rationale**: per maintainer direction (2026-06-10), v1 uses the simple
gate now and must not block on #3443. Migrating `admin.testCatalog.manage` to a
real privilege when OGC-384 lands is a **future follow-up**, tracked separately —
not v1 scope.

**Alternatives considered**: (a) new `SystemModule` + interceptor menu gate —
heavier, and REST still needs `hasRole`; deferred as unnecessary for v1. (b)
Depend on OGC-384 — rejected: unmerged; would block v1.

## R4 — PR #3546 (admin SideNav consolidation) vs OGC-927

**Decision**: Treat PR
[#3546](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3546) (mherman22,
"consolidate React admin sidenav into context-swap pattern", open → develop) as a
**sequencing dependency** for M2 (OGC-927 editor scaffold), resolved in M0.

**Rationale**: M2 mounts the editor into the admin SideNav and M-DC deletes
legacy admin entries from it; landing on a moving navigation target risks rework.
M0 confirms whether #3546 lands first (preferred — M2 builds on the consolidated
nav) or is sequenced after M2. No code in M0 beyond the decision record + a small
rebase if #3546 merges first.

**Alternatives considered**: ignore #3546 and reconcile at M-DC — rejected:
late-binding navigation conflicts are exactly what the consolidation changes.

**Resolved (2026-06-11)**: PR #3546 is **CLOSED, not merged** (abandoned). So
there is no consolidation to sequence against — M2 builds on the existing
`AdminSideNav.jsx` static component, no dependency. M2 added a Test Catalog
Editor entry under the existing Test Management SideNav menu.

## R5 — Jira structure validation (the "audit and pare" pass)

Validated the 20-epic / 72-story structure against the FRS and Confluence. Findings:

- **Child-count drift (cosmetic)**: the OGC-949 umbrella description says
  OGC-747 has "5 child stories" and OGC-927 has "4"; the Confluence delivery plan
  lists OGC-747 with 4 and OGC-927 with 5. **Ground truth from Jira**: OGC-747 has
  **5** (OGC-936..940), OGC-927 has **4** (OGC-941..944). The plan/spec use the
  Jira ground truth. → _Flag for a one-line Confluence/umbrella fix; not blocking._
- **OGC-940 placement**: filed under OGC-747 (schema/foundation) but it is
  _legacy decommission_. Executing it inside M1 would delete legacy admin before
  the replacement exists. → **Moved to the final v1 gate M-DC** in this plan.
  Jira can stay as-is (epic membership) since milestone mapping is plan-owned;
  noted for the team.
- **72-story census**: 49 v1 (epics 747/927/928/748/749/750/751/752/753/754/755/756)
  - 23 v2 (760–767) confirmed present and linked in the Source Map. All v1 stories
    appear exactly once in tasks.md.
- **Pared as not-dev-critical for v1 task elaboration**: none removed — instead,
  over-split historical epics (OGC-929/930/931/758) were already merged back into
  their parents during the June 8 restructure, so no de-duplication needed here.

**Action items for the user** (Jira-side, not done here per "no silent
divergence"): (1) reconcile the OGC-747/927 child-count line in the umbrella
description vs Confluence; (2) optionally add a comment on OGC-940 noting it
sequences at v1-release, not with the schema epic.

## R6 — FRS SHA pinning

**Decision**: Pin all FRS references to openelis-work `@f04cce54` (resolved
2026-06-10 `main` HEAD).

**Rationale**: the openelis-work design gallery updates frequently (last push
2026-06-09); pinning prevents the spec's section references (e.g. v2.5 §2.4) from
silently drifting. Re-pin deliberately if the FRS is revised.

## R7 — D-01..D-11 health-check fixes → FR mapping

| Fix  | Summary                                                 | Where honored                       |
| ---- | ------------------------------------------------------- | ----------------------------------- |
| D-01 | Drop 5 stale `editor.sidenav.*` i18n keys               | FR-D01; M2                          |
| D-02 | One Observation per component (FHIR)                    | FR-D02 / CR-005; M5 + data-model.md |
| D-03 | Alerts authoring here, delivery via Notification system | FR-D03; M16 (v2)                    |
| D-04 | `COMPLIANCE_BREACH` trigger for ENV/VECTOR only         | FR-D04; M16 (v2)                    |
| D-05 | `acknowledgment_required` flag on alert rules           | FR-D05; M16 (v2)                    |
| D-06 | Cross-ref Critical Result Acknowledgment workflow       | FR-D06; M16 (v2)                    |
| D-07 | Build test↔reagent linkage (v2 scope)                   | FR-D07; M13 (v2)                    |
| D-08 | Labels = 4 fixed presets, consume OGC-285               | FR-D08; M14 (v2) + R2               |
| D-09 | `test_sample_handling_history` created v1, inert        | FR-D09; M1 + M19                    |
| D-10 | No feature flag (direct replacement)                    | FR-D10 / FR-002; M-DC               |
| D-11 | Internal QA flag ≠ EQA participant workflow             | FR-D11; M4                          |

## R8 — Migration risk (M1, the riskiest piece)

**Decision**: M1 ships the `component_id` backfill with a dedicated losslessness
test and a dry-run against a production-like dump before merge.

**Rationale**: every existing test must gain a PRIMARY `test_result_component`
and have `RESULT_LIMITS` / `TEST_RESULT` rows repointed (see R9 for the real
table names); a miscount silently corrupts the catalog. Legacy per-test fields
(`TEST.UOM_ID`, per-`TEST_RESULT` type/significant-digits) are retained
one release cycle as the data-level rollback (FR-002) since there is no
feature flag (FR-D10). Multi-value free-text legacy tests map to a single PRIMARY
component with no automated sweep report (critique H-05 declined) — manual admin
review recommended post-migration.

## R9 — Schema grounding correction (2026-06-11)

**What happened**: the first version of data-model.md restated the FRS's
_idealized_ table names (`test_range`, `test_interpretation`,
`test_select_list_option`, new `unit_of_measure` / `panel_test` /
`test_localization` tables) without resolving them against the repository.
Caught when the OGC-937 changeset was about to target nonexistent tables.
**The spec is the translation layer between the FRS and the repo; restating
FRS names unresolved defeats its purpose.** data-model.md was re-grounded
against `hibernate/hbm/*.hbm.xml` + existing liquibase changesets.

**Material corrections** (full table in data-model.md):

| FRS name                         | Reality                                                                                         | Consequence                                        |
| -------------------------------- | ----------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| `test_range`                     | `RESULT_LIMITS` (has gender, Double ages in years, critical cols already)                       | component_id ALTER targets RESULT_LIMITS           |
| `test_select_list_option`        | `TEST_RESULT` rows (type 'D' via TEST_DICTIONARY)                                               | component_id ALTER targets TEST_RESULT             |
| `test_interpretation`            | no counterpart (only `TEST_RESULT.is_normal`)                                                   | genuinely new table `test_result_interpretation`   |
| new `unit_of_measure`            | `UNIT_OF_MEASURE` exists (TEST.UOM_ID FK)                                                       | ALTER (add code/ucum/is_active), not CREATE        |
| new `panel_test` junction        | `PANEL_ITEM` exists **with SORT_ORDER**                                                         | REUSE — no new table; M9 uses PANEL_ITEM           |
| new `test_localization`          | `LOCALIZATION`/`LOCALIZATION_VALUE` already localize test names via `TEST.name_localization_id` | REUSE — creating it would duplicate infra          |
| `test_sample_type.display_order` | junction is `SAMPLETYPE_TEST`, no order col                                                     | ALTER SAMPLETYPE_TEST                              |
| `test_section_assignment`        | TEST 1:1 section via `TEST.TEST_SECTION_ID`                                                     | decision deferred to M2; flagged to Jira (OGC-938) |

**Jira impact (flag to user)**: OGC-938's story text ("Junction tables + Sample
Storage + Unit of Measure seed") presumes the FRS names; actual scope is
narrower (handling tables + two ALTERs + reuse decisions). OGC-961..968 (M5)
and OGC-969..976 (M7) ACs that say `test_range`/`test_select_list_option`
should be read as `RESULT_LIMITS`/`TEST_RESULT`.

## R10 — REST path convention correction (2026-06-11 audit)

**What happened**: the foundation contract (`contracts/openapi.yaml`) was
authored with a `/api/v1` base — a training-data default, not this repo's
convention. **No `/api/v1` routing exists**: `SecurityConfig` routes/guards
REST exclusively under `REST_CONTROLLERS = {"/Provider/**", "/rest/**"}`;
every REST controller maps under `/rest` (`TestCatalogRestController` →
`GET /rest/TestCatalog`; the ported `TestMethodRestController` →
`/rest/test/{testId}/methods`). Contract endpoints under `/api/v1` would 404.

**Decision**: contract base corrected to `/rest`; editor endpoints are
`/rest/tests/**` (plural — avoids colliding with the existing singular
`/rest/test/{testId}/methods` namespace). Naming reconciliation between
`/rest/tests/**` and the ported singular path happens at the M2 ELABORATE
gate. Session auth (JSESSIONID) and the `hasRole('ADMIN')` 403 contract were
verified correct against SecurityConfig.

**Same root cause as R9** (ungrounded generation); both caught by the
2026-06-11 full-artifact audit. Verified-clean in that audit: tech stack
(Java 21 / Spring 6.2.17 MVC / React 17 / Liquibase 4.8 / JUnit 4 /
`npm run pw:test`), all "reusable infrastructure" classes/paths, all hbm
schema facts in R9's table, fixture loader + spotless + template paths,
branch/commit/PR facts, and the checklists' cross-references.

## R11 — AMR flag duplicate (caught in post-worktree recon, 2026-06-11)

**What happened**: changeset 040 (my OGC-936 work) added a NEW `test.is_amr_test`
boolean + `Test.amrTest`. Recon found `test.antimicrobial_resistance` ALREADY
exists (`Test.antimicrobialResistance`, liquibase
`2.8.x.x/add_antimicrobial_resistance_test_column.xml`), wired through
`TestService`, `TestAddFormValidator`, `TestModifyEntry*Controller`,
`TestCatalogBean`. The new column was a duplicate of the established AMR flag.

**Fix**: dropped `is_amr_test` / `amrTest` from changeset 040, `Test.java`,
`Test.hbm.xml`. The v2.5 Basic Info AMR toggle (M4) **reuses
`Test.antimicrobialResistance`**. `test_amr_config` (per-test WHONET detail) +
`whonet_antibiotic_codes` remain — they are genuinely new and complement the
existing flag. Safe to amend 040 in place: it had only run on ephemeral
Testcontainers/CI DBs (no durable DB recorded its checksum) pre-merge.

**Same lesson as R9/R10**: grep the existing columns before adding one. `domain`
was verified genuinely new (no pre-existing test.domain); only AMR collided.
