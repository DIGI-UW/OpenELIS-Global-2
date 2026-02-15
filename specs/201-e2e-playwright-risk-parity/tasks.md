# Tasks: E2E Test Battery Revamp to Playwright with Risk-Based Parity

**Feature**: `201-e2e-playwright-risk-parity`  
**Input**: Design documents from `/specs/201-e2e-playwright-risk-parity/`  
**Prerequisites**: `spec.md` and `plan.md` (present)

> **Scope Update (2026-02-15, `/speckit.clarify`)**: Feature goal is now full
> migration + full E2E battery refactor (including fixture/data stabilization
> and Cypress sunset planning). Existing tasks in this file are retained as the
> executed migration log for M1-M9 planning baseline; this file now includes an
> explicit pre-cutover full-parity side-by-side milestone (`M8a`). Cypress
> sunset execution is deferred to a follow-on feature after migration review.

**Tests**: Mandatory. This feature is itself E2E-focused; milestones include
test artifacts, parity validation checks, and CI comparison verification.

**Organization**: Tasks are grouped by **milestone** (M1-M9) per plan.md, with
bite-size PRs and explicit verification gates.

## Format: `[ID] [P?] [M#] Description`

- **[P]**: Can run in parallel (different files, no direct dependency)
- **[M#]**: Milestone tag from `plan.md`
- Include exact file paths in descriptions

---

## Milestone to Story Mapping

| Milestone | Primary Stories | Scope                                                              |
| --------- | --------------- | ------------------------------------------------------------------ |
| M1        | US1             | Coverage inventory and CI execution map                            |
| M2        | US1, US2        | Risk model + parity map skeleton                                   |
| M3        | US2             | Playwright migration foundation hardening                          |
| M4a       | US2, US3        | P0 auth/nav Playwright migration                                   |
| M4b       | US2, US3        | P0 admin-core Playwright migration                                 |
| M5        | US2, US3        | P0 clinical workflow migration                                     |
| M6        | US3             | Critical storage gap closure                                       |
| M7        | US2, US4        | Dual-run CI parity report pipeline                                 |
| M8a       | US3, US4        | Full parity side-by-side gate (current Cypress vs Playwright)      |
| M8        | US4             | Big-bang cutover after parity gate (Cypress side-by-side retained) |
| M9        | US3, US4        | Stabilization and migration signoff packet                         |

---

## M1: Inventory Baseline (Bite-size PR 1)

**Goal**: Authoritative inventory of current Cypress and Playwright coverage.

- [x] T001 [M1] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m1-inventory` from current feature
      branch (tracked on `cursor/e2e-test-revamp-investigation-e1a2` due cloud
      single-branch constraint)
- [x] T002 [M1] Create inventory artifact
      `specs/201-e2e-playwright-risk-parity/coverage-inventory.md` (spec-level
      listing, domains, counts, skip status)
- [x] T003 [P] [M1] Create CI execution mapping artifact
      `specs/201-e2e-playwright-risk-parity/ci-execution-map.md`
      (workflow/job/shard -> spec mapping)
- [x] T004 [P] [M1] Add inventory generation helper script
      `scripts/e2e/export-e2e-inventory.js` (parse Cypress/Playwright specs and
      emit normalized JSON)
- [x] T005 [P] [M1] Add inventory validation script
      `scripts/e2e/validate-e2e-inventory.js` (fail if active specs are missing
      from inventory)
- [x] T006 [M1] Add raw normalized inventory output
      `specs/201-e2e-playwright-risk-parity/artifacts/inventory.json`
- [x] T007 [M1] Run validation gate and record output in
      `specs/201-e2e-playwright-risk-parity/coverage-inventory.md`
- [x] T008 [M1] Milestone gate: verify 100% active Cypress + Playwright specs
      represented

---

## M2: Risk Model + Parity Skeleton (Bite-size PR 2)

**Goal**: Establish risk tiers and scenario-level parity mapping baseline.

- [x] T020 [M2] Create/expand risk model document in
      `specs/201-e2e-playwright-risk-parity/data-model.md` (P0/P1/P2 rubric)
- [x] T021 [M2] Create parity mapping matrix template in
      `specs/201-e2e-playwright-risk-parity/parity-matrix.csv`
- [x] T022 [P] [M2] Populate all P0/P1 legacy scenarios into parity matrix with
      initial status
- [x] T023 [P] [M2] Create critical gap register
      `specs/201-e2e-playwright-risk-parity/critical-gap-register.md`
- [x] T024 [P] [M2] Create approved exception template
      `specs/201-e2e-playwright-risk-parity/approved-exception-template.md`
- [x] T025 [M2] Add P0/P1 parity completeness validator script
      `scripts/e2e/validate-parity-mapping.js`
- [x] T026 [M2] Run parity mapping validator and append results to
      `specs/201-e2e-playwright-risk-parity/parity-matrix.csv` notes column
- [x] T027 [M2] Milestone gate: 100% P0/P1 scenarios have parity map records
- [x] T028 [M2] Define reliability SLO and baseline flaky scenario set in
      `specs/201-e2e-playwright-risk-parity/baseline-flaky-scenarios.md`
      (target: >=95% pass rate across 20 CI-equivalent runs)
- [x] T029 [M2] Link baseline flaky scenarios into `parity-matrix.csv` with
      reliability tracking identifiers

---

## M3: Playwright Foundation Hardening (Bite-size PR 3)

**Goal**: Stable foundation for large-scale migration.

- [x] T040 [M3] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m3-pw-foundation` (tracked on
      `cursor/e2e-test-revamp-investigation-e1a2` due cloud single-branch
      constraint)
- [x] T041 [P] [M3] Add shared Playwright fixture helper
      `frontend/playwright/fixtures/e2e-base.ts` (navigation/auth/standard
      waits)
- [x] T042 [P] [M3] Add parity metadata helper
      `frontend/playwright/fixtures/parity-metadata.ts` (legacy scenario IDs,
      risk tags)
- [x] T043 [M3] Add harness smoke spec
      `frontend/playwright/tests/harness-smoke.spec.ts` (auth setup + common
      navigation sanity)
- [x] T044 [M3] Update Playwright config for migration conventions in
      `frontend/playwright.config.ts` (reporter details, project annotations,
      artifact consistency)
- [x] T045 [M3] Add Playwright migration quickstart section in
      `specs/201-e2e-playwright-risk-parity/quickstart.md`
- [x] T046 [M3] Test gate: run `npm run pw:test` and record baseline outcome in
      quickstart
- [ ] T047 [M3] Milestone gate: existing Playwright suite remains green after
      foundation changes (blocked in current cloud environment: app at
      `https://localhost` not reachable; `docker` unavailable)
- [x] T048 [M3] Add assertion-quality checklist for migrated scenarios in
      `specs/201-e2e-playwright-risk-parity/assertion-quality-checklist.md`
      (user-visible assertions + real-effect expectations)
- [x] T049 [M3] Add E2E semantics guardrail checklist in
      `specs/201-e2e-playwright-risk-parity/e2e-semantics-checklist.md` (avoid
      turning real E2E into mocked-backend tests)
- [x] T050 [M3] Evaluate fixture strategy alternatives and record selection in
      `specs/201-e2e-playwright-risk-parity/fixture-strategy.md` (criteria:
      atomic/independent behavior, best-practice alignment, runtime impact)
- [x] T051 [M3] Apply selected fixture strategy defaults to
      `frontend/cypress.config.js`,
      `frontend/playwright/fixtures/storage-fixtures.ts`, and shared fixture
      helpers
- [ ] T052 [M3] Milestone gate: fixture strategy is selected, documented, and
      validated against performance + isolation acceptance criteria (runtime
      execution blocked in current cloud environment: `https://localhost`
      unavailable for Playwright/Cypress verification runs)
- [x] T053 [M3] Define explicit auth strategy contract (shared-account for
      read-only/non-mutating flows; worker/account isolation for mutating flows)
      in `specs/201-e2e-playwright-risk-parity/fixture-strategy.md`
- [x] T054 [M3] Implement auth strategy defaults in
      `frontend/playwright/fixtures/e2e-base.ts` (and related helper modules if
      needed) so mutating parallel flows can run with isolated accounts
- [ ] T055 [M3] Milestone gate: validate auth strategy with representative
      read-only and mutating flows and record evidence in
      `specs/201-e2e-playwright-risk-parity/quickstart.md` (runtime execution
      blocked in current cloud environment: `https://localhost` unavailable for
      Playwright verification runs)

---

## M4a: P0 Auth/Nav Migration (Bite-size PR 4a, parallel)

**Goal**: Port core auth/navigation critical workflows.

- [x] T060 [M4a] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m4a-core-authnav` (tracked on
      `cursor/e2e-test-revamp-investigation-e1a2` due cloud single-branch
      constraint)
- [x] T061 [P] [M4a] Add Playwright spec
      `frontend/playwright/tests/auth-login.spec.ts` (login
      success/failure/session)
- [x] T062 [P] [M4a] Add Playwright spec
      `frontend/playwright/tests/home-navigation.spec.ts` (home navbar actions,
      key menu navigation)
- [x] T063 [P] [M4a] Add Playwright spec
      `frontend/playwright/tests/dashboard-smoke.spec.ts` (critical dashboard
      tile navigation)
- [x] T064 [M4a] Update parity matrix statuses for migrated auth/nav scenarios
- [x] T065 [M4a] Add migration evidence notes in
      `specs/201-e2e-playwright-risk-parity/coverage-inventory.md`
- [x] T066 [M4a] Test gate: run new auth/nav Playwright specs and Cypress
      counterparts for parity spot-check
- [ ] T067 [M4a] Milestone gate: migrated auth/nav parity entries move to PASS
      (blocked in current cloud environment: `https://localhost` unavailable for
      both Playwright and Cypress spot-checks)

---

## M4b: P0 Admin-Core Migration (Bite-size PR 4b, parallel)

**Goal**: Port critical admin workflows.

- [x] T070 [M4b] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m4b-core-admin` (tracked on
      `cursor/e2e-test-revamp-investigation-e1a2` due cloud single-branch
      constraint)
- [x] T071 [P] [M4b] Add Playwright spec
      `frontend/playwright/tests/admin-user-management.spec.ts`
- [x] T072 [P] [M4b] Add Playwright spec
      `frontend/playwright/tests/admin-organization-provider.spec.ts`
- [x] T073 [P] [M4b] Add Playwright spec
      `frontend/playwright/tests/admin-barcode-core.spec.ts`
- [x] T074 [M4b] Update parity matrix for admin-core scenario mappings
- [x] T075 [M4b] Test gate: run migrated admin Playwright specs and target
      Cypress admin specs
- [ ] T076 [M4b] Milestone gate: migrated admin-core parity entries move to PASS
      (blocked in current cloud environment: `https://localhost` unavailable for
      both Playwright and Cypress spot-checks)

---

## M5: P0 Clinical Migration (Bite-size PR 5)

**Goal**: Port critical clinical workflows (patient/order/result/report).

- [x] T080 [M5] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m5-core-clinical` (tracked on
      `cursor/e2e-test-revamp-investigation-e1a2` due cloud single-branch
      constraint)
- [x] T081 [P] [M5] Add Playwright spec
      `frontend/playwright/tests/clinical-patient-order.spec.ts`
- [x] T082 [P] [M5] Add Playwright spec
      `frontend/playwright/tests/clinical-result-validation.spec.ts`
- [x] T083 [P] [M5] Add Playwright spec
      `frontend/playwright/tests/clinical-report-workplan.spec.ts`
- [x] T084 [P] [M5] Add Playwright spec
      `frontend/playwright/tests/clinical-nonconform.spec.ts`
- [x] T085 [M5] Update parity matrix for clinical P0 scenarios
- [x] T086 [M5] Test gate: run clinical Playwright specs + targeted Cypress
      references
- [ ] T087 [M5] Milestone gate: no unresolved blocking P0 clinical gaps (blocked
      in current cloud environment: `https://localhost` unavailable for both
      Playwright and Cypress spot-checks)

---

## M6: Critical Storage Gap Closure (Bite-size PR 6)

**Goal**: Close high-risk legacy storage gaps (including skipped legacy cases).

- [x] T090 [M6] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m6-storage-gaps` (tracked on
      `cursor/e2e-test-revamp-investigation-e1a2` due cloud single-branch
      constraint)
- [x] T091 [P] [M6] Add Playwright spec
      `frontend/playwright/tests/storage-assignment-critical.spec.ts`
      (cascading, typeahead, barcode, capacity warning)
- [x] T092 [P] [M6] Add Playwright spec
      `frontend/playwright/tests/storage-box-crud-critical.spec.ts`
      (edit/delete/constraint behavior)
- [x] T093 [P] [M6] Add Playwright spec
      `frontend/playwright/tests/storage-samples-visibility.spec.ts`
- [x] T094 [P] [M6] Add Playwright spec
      `frontend/playwright/tests/storage-view-edit-critical.spec.ts`
- [x] T095 [M6] Integrate fixture-loading workflow into Playwright tests using
      existing loader scripts
- [x] T096 [M6] Update `critical-gap-register.md` and `parity-matrix.csv`
      statuses
- [ ] T097 [M6] Test gate: validate critical storage gap list reduced to zero
      P0/P1 open items or approved exceptions (execution checks blocked in
      current cloud environment: `https://localhost` unavailable for both
      Playwright and Cypress spot-checks)

---

## M7: Dual-Run CI Parity Report (Bite-size PR 7)

**Goal**: Produce comparable, per-run Cypress vs Playwright parity output.

- [x] T100 [M7] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m7-parity-ci` (tracked on
      `cursor/e2e-test-revamp-investigation-e1a2` due cloud single-branch
      constraint)
- [x] T101 [P] [M7] Add normalized Playwright result exporter
      `scripts/e2e/export-playwright-results.js`
- [x] T102 [P] [M7] Add normalized Cypress result exporter
      `scripts/e2e/export-cypress-results.js`
- [x] T103 [M7] Add comparison script `scripts/e2e/compare-e2e-results.js`
- [x] T104 [M7] Add parity report schema
      `specs/201-e2e-playwright-risk-parity/contracts/parity-report.schema.json`
- [x] T105 [M7] Update Playwright workflow to emit normalized artifact
      `.github/workflows/playwright-e2e.yml`
- [x] T106 [M7] Update Cypress workflow to emit normalized artifact
      `.github/workflows/frontend-qa.yml`
- [x] T107 [M7] Add CI parity-report job/workflow that publishes markdown/json
      parity report `.github/workflows/e2e-parity-report.yml`
- [x] T108 [M7] Add parity report output location
      `specs/201-e2e-playwright-risk-parity/artifacts/parity-report.md`
- [ ] T109 [M7] Milestone gate: CI emits classified parity report with risk
      labels on each run (pending CI execution confirmation)
- [x] T110 [M7] Extend parity comparison script to classify failures by
      `failure_class` (setup/infra, assertion, parity divergence)
- [x] T111 [M7] Add runtime metric exporter
      `scripts/e2e/export-runtime-metrics.js` (capture suite wall-clock per run)
- [x] T112 [M7] Define runtime budget in
      `specs/201-e2e-playwright-risk-parity/runtime-budget.md` (Playwright +
      Cypress dual-run target/bounds)
- [x] T113 [M7] Include failure classification and runtime budget status in
      `artifacts/parity-report.md` for each CI run

---

## M8a: Full Parity Side-by-Side Gate (Bite-size PR 8a)

**Goal**: Reach full parity for the current non-skipped Cypress cutoff while
both frameworks run side by side.

- [ ] T128 [M8a] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m8a-full-parity`
- [ ] T129 [M8a] Freeze cutoff scope for parity gate using inventory artifact +
      run IDs and record reference in
      `specs/201-e2e-playwright-risk-parity/signoff-summary.md`
- [ ] T130 [M8a] Execute full side-by-side parity run using CI artifacts from
      Playwright and Cypress workflows for the frozen cutoff scope
- [ ] T131 [M8a] Update `parity-matrix.csv` so all cutoff in-scope non-skipped
      Cypress rows are `PASS` (no `EXCEPTION_APPROVED` allowed for this gate)
- [ ] T132 [M8a] Record side-by-side full parity evidence in
      `specs/201-e2e-playwright-risk-parity/artifacts/parity-report.md`
- [ ] T133 [M8a] Milestone gate: no cutoff in-scope non-skipped Cypress parity
      rows remain in `LEGACY_ONLY`, `GAP`, or `PARTIAL`
- [ ] T134 [M8a] Attach run IDs/artifact links and parity gate confirmation in
      `specs/201-e2e-playwright-risk-parity/signoff-summary.md`
- [ ] T135 [M8a] Confirm skipped legacy coverage remains explicitly tracked in
      `critical-gap-register.md` for post-migration review context

---

## M8: Big-Bang Cutover (Primary Playwright, Cypress Retained) (Bite-size PR 8b)

**Goal**: Make Playwright primary E2E gate while keeping full Cypress
comparison.

- [ ] T120 [M8] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m8-bigbang`
- [ ] T121 [M8] Update E2E workflow orchestration to make Playwright the primary
      gate check `.github/workflows/playwright-e2e.yml`
- [ ] T122 [M8] Keep Cypress full-suite workflow active and explicit as
      comparison suite `.github/workflows/frontend-qa.yml`
- [ ] T123 [M8] Add/adjust aggregate check naming and documentation for primary
      vs comparison roles `specs/201-e2e-playwright-risk-parity/quickstart.md`
- [ ] T124 [M8] Add explicit note in migration docs that `M8a` full parity gate
      was satisfied before enabling cutover
      `specs/201-e2e-playwright-risk-parity/quickstart.md`
- [ ] T125 [M8] Milestone gate: Playwright check is primary while Cypress still
      fully runs
- [ ] T126 [M8] Verify required checks policy via GitHub CLI and record evidence
      in `specs/201-e2e-playwright-risk-parity/signoff-summary.md` (confirm
      Playwright primary check + Cypress comparison checks)
- [ ] T127 [M8] Add cutover checklist artifact
      `specs/201-e2e-playwright-risk-parity/cutover-checklist.md` (checks,
      owners, rollback criteria)

---

## M9: Stabilization + Signoff Packet (Bite-size PR 9)

**Goal**: Run stabilization window and produce decision-ready outputs.

- [ ] T140 [M9] Create milestone branch
      `feat/201-e2e-playwright-risk-parity-m9-stabilization`
- [ ] T141 [M9] Create stabilization tracker
      `specs/201-e2e-playwright-risk-parity/stabilization-report.md`
- [ ] T142 [P] [M9] Add divergence triage ledger
      `specs/201-e2e-playwright-risk-parity/divergence-triage.md`
- [ ] T143 [P] [M9] Update parity matrix with stabilization outcomes and owner
      assignment
- [ ] T144 [M9] Create final signoff summary
      `specs/201-e2e-playwright-risk-parity/signoff-summary.md`
- [ ] T145 [M9] Confirm explicit migration closure statement and Cypress sunset
      recommendation handoff in
      `specs/201-e2e-playwright-risk-parity/signoff-summary.md`
- [ ] T146 [M9] Milestone gate: no untriaged P0/P1 divergences at end of
      stabilization window
- [ ] T147 [M9] Execute 20-run CI-equivalent reliability evaluation for baseline
      flaky scenarios and record pass rates in
      `specs/201-e2e-playwright-risk-parity/stabilization-report.md`
- [ ] T148 [M9] Evaluate runtime budget compliance and record >=90% in-budget
      result in `stabilization-report.md`
- [ ] T149 [M9] Verify all divergence entries include owner, risk tier, and
      `failure_class` in `divergence-triage.md`
- [ ] T150 [M9] Create legacy Cypress artifact retention policy
      `specs/201-e2e-playwright-risk-parity/artifact-retention-policy.md`
      (artifact scope, storage location, retention duration, and owner)
- [ ] T151 [M9] Link and confirm retention policy adoption in
      `specs/201-e2e-playwright-risk-parity/signoff-summary.md`

---

## Cross-Cutting Validation Tasks

- [ ] T160 [P] [M1-M9] Run required formatting before each commit:
      `mvn spotless:apply` and `cd frontend && npm run format`
- [ ] T161 [P] [M1-M9] Run mandatory pre-push full-suite validation before each
      PR: `cd frontend && npm run pw:test` and
      `cd frontend && npm run cy:failfast`
- [ ] T162 [P] [M1-M9] Keep `parity-matrix.csv` and `critical-gap-register.md`
      current
- [ ] T163 [P] [M1-M9] Capture CI artifact links in milestone PR descriptions
- [ ] T164 [P] [M1-M9] Attach pre-push validation evidence (commands, run IDs,
      parity artifact links) in milestone PR description/checklist

---

## Dependencies & Execution Order

### Milestone Dependencies

- M1 -> M2 -> M3
- M3 -> (M4a + M4b in parallel) -> M5 -> M6 -> M7 -> M8a -> M8 -> M9

### Parallel Opportunities

- M4a and M4b are parallel milestone tracks
- Within most milestones, `[P]` tasks can run concurrently
- Script/artifact tasks can proceed in parallel with test authoring where no
  file overlap exists

### Within Each Milestone

- Create milestone branch first
- Write/adjust tests and validation checks before broad implementation where
  feasible
- Run milestone gate checks before PR creation
- Keep parity artifacts updated in the same PR

---

## Implementation Strategy

1. **Evidence first**: complete inventory and risk model before migration waves
2. **P0 first**: migrate highest-risk scenarios before long-tail coverage
3. **Gap closure over raw parity count**: skipped/weak legacy areas are upgraded
4. **Full parity before cutover**: satisfy side-by-side parity gate (`M8a`)
5. **Controlled migration completion**: keep Cypress side-by-side in this
   feature and hand off sunset recommendation (execution deferred)

---

## Notes

- This task plan intentionally prioritizes small, testable milestones and
  explicit stop/go gates.
- Branch naming in milestone tasks follows the plan’s `feat/201-...-mN-*`
  pattern.
- Milestone branch names are logical targets for parallel development; in
  constrained single-branch environments, preserve this naming in documentation
  and PR titles/checklists for traceability.
- `/speckit.implement` should execute milestones in dependency order and avoid
  skipping gates.
