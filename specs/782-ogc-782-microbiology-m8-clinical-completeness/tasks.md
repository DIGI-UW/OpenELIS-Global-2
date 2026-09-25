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
- [x] T022 Run Slice A backend/frontend formatting and focused validation; commit as one reviewable checkpoint.

## Phase 3 - Repeat AST and lot traceability tests (Slice B, red)

- [x] T023 Add service tests for original/repeat/retest creation, required source/reason, immutable prior run, and reportable-run selection blockers.
- [x] T024 Add lot-query tests for Test Catalog role preservation, FEFO ordering, QC/status/expiry/quantity eligibility, and historical inclusion.
- [x] T025 Add a PostgreSQL-backed stale-lot test proving server-side save rejection after an eligible lot becomes invalid, with no case-stage, quantity, usage, or history mutation.
- [x] T026 Add Inventory integration tests proving culture/AST usage is recorded once, linked to the bench action, decrements through shared behavior, and rolls back quantity, transaction, usage, and provenance together on downstream failure (`87177aee2`).
- [x] T027 Add controller tests proving actor derivation, validation, and named invalid-lot errors; existing microbiology authorization remains authoritative.
- [x] T028 Extend ORM validation for Slice B mappings.

## Phase 4 - Repeat AST and lot traceability implementation (Slice B, green)

- [x] T029a Add Liquibase `061` AST-attempt metadata beyond the Slice A lifecycle link, with rollback.
- [x] T029b Add culture/AST-to-Inventory-usage linkage with rollback; do not recreate reagent, lot, or usage tables.
- [x] T030 Implement repeat/retest run creation and explicit reportable-run selection.
- [x] T031 Implement reusable lot query/validation and usage orchestration over existing Test Catalog and Inventory services.
- [x] T032 Add shared Carbon lot picker and wire it into culture setup and AST setup without duplicating host logic.
- [x] T033a Render attempt relationships and reportable selection in the case workbench with text-plus-color status.
- [x] T033b Render historical lots in the case workbench with text-plus-color status.
- [x] T034a Add focused Vitest tests for repeat AST behavior.
- [x] T034b Add focused Vitest tests for lot picker behavior.
- [x] T035a Add a registered Playwright journey for repeat AST and explicit reportable-run selection.
- [x] T035b Add a registered Playwright journey with FEFO lot selection, invalid-lot blocking, persisted usage, and historical lot display.
- [x] T036 Run Slice B backend/frontend formatting and focused validation; commit as reviewable checkpoints `307a9b3de` and `554063fc8`.

## Phase 5 - Accessibility qualification (Slice C)

- [x] T037 Add the reviewed current `@axe-core/playwright` version and register focused desktop/mobile Microbiology accessibility test projects.
- [x] T038a Add stable axe checks for worklist, case overview, isolate, AST, critical communication, reporting, and amendment states.
- [x] T038b Add the stable axe check for the policy-neutral lot-picker state.
- [x] T039a Add a keyboard-only Playwright journey through filtering, case navigation, isolate/AST entry, and amendment release.
- [x] T039b Extend the keyboard-only journey through lot selection using the Carbon radio interaction.
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

- [x] T048 Run focused JUnit, Testcontainers, ORM, Liquibase, Vitest,
      Playwright, axe, and representative-volume qualification for the M8
      behavior.
- [x] T049 Run Spotless, Prettier, focused source lint, type checking for
      M8-owned files, and `git diff --check`.
- [x] T050 Run one pinned `tools/code-qa` alignment, coverage, and simplicity
      review; resolve actionable findings without committing generated reports.
- [x] T051 Keep the M8 specification, plan, and tasks aligned with durable
      behavior and explicit exclusions.

## Dependency Notes

- Slice A depends on #3789's final lock and standard report projection.
- Slice B depends on #3789's AST/culture workflow and existing Test Catalog and Inventory foundations.
- Slice C accessibility can begin after Slice A UI stabilizes; final evidence waits for Slice B UI.
- Performance fixture work can proceed after the Slice A schema is stable but must measure the final Slice B data shape.
- Mandatory/optional/substitute lot enforcement remains blocked by the
  unresolved `PRIMARY / SECONDARY` versus
  `REQUIRED / OPTIONAL / SUBSTITUTE` product-policy contradiction. T024-T039b
  now cover the policy-neutral behavior; no requirement semantics are inferred
  from the existing role values.
