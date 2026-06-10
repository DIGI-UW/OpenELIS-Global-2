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
- `TestModifyEntry.jsx` — the Methods commit mounts `MethodsSection` as a *Tab*
  inside demo-silnas's **compliance-Tabs** test-editor UI, which does not exist
  on develop (develop renders `TestStepForm` directly). The editor mount is
  therefore **deferred to M6** (wiring `MethodsSection` into the M2/OGC-927
  unified editor scaffold). `MethodsSection.jsx` ships orphaned until then.
- `SearchResultForm.jsx` — the conflict was NCE/holding-time notification logic,
  not Methods; took develop's version.

This validates the port policy: the Methods *backend* qualified cleanly, but its
*frontend mount* dragged a compliance dependency and was correctly excluded. It
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
and have `test_range` / `test_interpretation` / `test_select_list_option`
repointed; a miscount silently corrupts the catalog. Legacy per-test columns
(`result_type`, `unit_of_measure`, `significant_digits`, `default_result`) are
retained one release cycle as the data-level rollback (FR-002) since there is no
feature flag (FR-D10). Multi-value free-text legacy tests map to a single PRIMARY
component with no automated sweep report (critique H-05 declined) — manual admin
review recommended post-migration.
