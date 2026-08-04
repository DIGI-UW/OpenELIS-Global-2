# Tasks: M8 Clinical Completeness and Qualification

Tasks are dependency ordered. Tests are written before production behavior in
each slice. A checked task requires committed evidence, not intent.

## Phase 0 - Grounding and branch isolation

- [x] T001 Create the clean `cc8c` worktree at PR #3789 head.
- [x] T002 Create branch `feat/782-ogc-782-microbiology-m8-clinical-completeness`.
- [x] T003 Reconcile OGC-783/784/790/791, current OpenELIS Work sources, and repo state in `research.md`.
- [x] T004 Define behavior-only acceptance criteria in `spec.md` and keep engineering decisions in `plan.md`.
- [x] T005 Run SpecKit cross-artifact analysis and resolve every ERROR before the first Slice A commit.

## Phase 1 - Amendment lifecycle tests (Slice A, red)

- [x] T006 Add JUnit 4 service tests: only final cases can open amendments, reason required, one open amendment, authenticated actor retained, and non-amendment writes remain locked.
- [x] T007 Add service tests for release and cancellation: original version preserved, amended version appended, case relocked, named blockers returned.
- [x] T008 Add report-projection tests proving amended release creates a new standard Analysis revision and Result without changing the prior finalized Analysis/Result.
- [x] T009 Add isolate tests proving a reported organism change requires an open amendment and reason and records immutable before/after identification history.
- [x] T010 Add controller tests for amendment/history endpoints, actor spoof rejection, authorization, validation, and 409 conflict responses.
- [x] T011 Add Testcontainers integration tests for unique open amendment, report-version ordering/correction link, append-only identification history, and transaction rollback.
- [x] T012 Extend ORM validation with all Slice A mappings.

## Phase 2 - Amendment lifecycle implementation (Slice A, green)

- [x] T013 Add Liquibase `060` amendment/report-version/identification-history schema with constraints, indexes, and rollback; include it in the versioned base changelog.
- [x] T014 Add amendment, report-version, and identification-event valueholders plus DAO/service layers.
- [x] T015 Replace the absolute final mutation guard with an active-amendment-aware service guard while preserving the named lock outside an amendment.
- [x] T016 Orchestrate baseline capture, new Analysis revision/Result creation, amended release, cancellation, and final relock through services.
- [x] T017 Require and persist re-identification reason and before/after values; project a readable event into the existing case timeline.
- [x] T018 Add REST forms/controllers with server-derived actors and stable blocker codes.
- [x] T019 Add Carbon amendment/history UI in the case workbench with canonical `section=amendment` state and React Intl strings.
- [x] T020 Add focused Vitest tests for amendment reason, history, focus management, errors, and relock behavior.
- [x] T021 Add registered Playwright amendment journey proving original/amended patient-report content and post-release lock.
- [ ] T022 Run Slice A backend/frontend formatting and focused validation; commit as one reviewable checkpoint.

## Phase 3 - Repeat AST and lot traceability tests (Slice B, red)

- [x] T023 Add service tests for original/repeat/retest creation, required source/reason, immutable prior run, and reportable-run selection blockers.
- [ ] T024 Add lot-query tests for Test Catalog linkage semantics, FIFO ordering, QC/status/expiry/quantity eligibility, and historical inclusion.
- [ ] T025 Add stale-lot integration test proving server-side save rejection after an eligible lot becomes invalid.
- [ ] T026 Add Inventory integration tests proving culture/AST usage is recorded once, linked to the bench action, decrements through shared behavior, and is transactionally rolled back on failure.
- [ ] T027 Add controller tests proving actor derivation, validation, authorization, and named invalid-lot errors.
- [ ] T028 Extend ORM validation for Slice B mappings.

## Phase 4 - Repeat AST and lot traceability implementation (Slice B, green)

- [x] T029a Add Liquibase `061` AST-attempt metadata beyond the Slice A lifecycle link, with rollback.
- [ ] T029b Add culture/AST-to-Inventory-usage linkage with rollback; do not recreate reagent, lot, or usage tables.
- [x] T030 Implement repeat/retest run creation and explicit reportable-run selection.
- [ ] T031 Implement reusable lot query/validation and usage orchestration over existing Test Catalog and Inventory services.
- [ ] T032 Add shared Carbon lot picker and wire it into culture setup and AST setup without duplicating host logic.
- [x] T033a Render attempt relationships and reportable selection in the case workbench with text-plus-color status.
- [ ] T033b Render historical lots in the case workbench with text-plus-color status.
- [x] T034a Add focused Vitest tests for repeat AST behavior.
- [ ] T034b Add focused Vitest tests for lot picker behavior.
- [x] T035a Add a registered Playwright journey for repeat AST and explicit reportable-run selection.
- [ ] T035b Extend the registered Playwright journey with FIFO lot selection, invalid-lot rejection, persisted usage, and historical lot display.
- [ ] T036 Run Slice B backend/frontend formatting and focused validation; commit as one reviewable checkpoint.

## Phase 5 - Accessibility qualification (Slice C)

- [x] T037 Add the reviewed current `@axe-core/playwright` version and register focused desktop/mobile Microbiology accessibility test projects.
- [x] T038a Add stable axe checks for worklist, case overview, isolate, AST, critical communication, reporting, and amendment states.
- [ ] T038b Add the stable axe check for the lot-picker state after lot policy is resolved.
- [x] T039a Add a keyboard-only Playwright journey through filtering, case navigation, isolate/AST entry, and amendment release.
- [ ] T039b Extend the keyboard-only journey through lot selection after lot policy is resolved.
- [x] T040 Fix detected semantic, labeling, status, focus, and contrast defects using Carbon patterns; add Vitest regressions at the owning component level.
- [x] T041 Record desktop/mobile screenshots and machine-readable axe output tied to the commit.

## Phase 6 - Scale and performance qualification (Slice C)

- [x] T042 Add property-gated service-layer qualification builders for 200 worklist cases and one dense case with 5 isolates, 80 readings, and at least 30 events; no SQL, fixed IDs, DAO bypass, or production endpoint.
- [x] T043 Add cleanup and isolation tests proving qualification data cannot leak across tests or ordinary deployments.
- [x] T044 Add API measurements for worklist load/search, case load, isolate save, AST save, and timeline save with fixed warm-up/iteration/p95 rules.
- [x] T045 Add browser measurements for initial worklist, case render, and filter/page interaction using stable app-ready marks.
- [x] T046a Remove the evidenced worklist N+1 relationship loading and verify existing foreign-key indexes cover the new batch predicates; do not add a speculative migration.
- [x] T046b Capture formal query-plan evidence; add an index only where the plan demonstrates need and validate any resulting Liquibase update/rollback.
- [x] T047 Emit JSON and Markdown evidence containing raw samples, percentiles, environment, data volume, commit, and pass/fail.

## Phase 7 - Completion and artifact reconciliation

- [ ] T048 Run all focused JUnit, Testcontainers, ORM, Liquibase, Vitest, Playwright, axe, and qualification commands named in evidence.
- [ ] T049 Run Spotless, Prettier, source lint, typecheck, and `git diff --check`.
- [ ] T050 Run pinned `tools/code-qa` alignment, coverage, simplicity, and evidence reviews; resolve actionable findings.
- [ ] T051 Update the M8 spec/plan/tasks and the original MVP V2 list without changing #3789's historical acceptance claims.
- [ ] T052 Reconcile OGC-783/784/790/791 delivery status with product-safe comments; do not close parent epics wholesale.
- [ ] T053 Push the branch and open/update the stacked PR with exact base, migrations, exclusions, test commands, screenshots, video, evidence, and current one-shot check state.

## Dependency Notes

- Slice A depends on #3789's final lock and standard report projection.
- Slice B depends on #3789's AST/culture workflow and existing Test Catalog and Inventory foundations.
- Slice C accessibility can begin after Slice A UI stabilizes; final evidence waits for Slice B UI.
- Performance fixture work can proceed after the Slice A schema is stable but must measure the final Slice B data shape.
- Human UAT of #3789 remains independent and cannot be replaced by M8 automation.
- T024-T035b lot work remains blocked by the unresolved `PRIMARY / SECONDARY`
  versus `REQUIRED / OPTIONAL / SUBSTITUTE` product-policy contradiction. No
  requirement semantics are inferred from the existing role values.
