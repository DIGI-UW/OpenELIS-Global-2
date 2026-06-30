# Tasks: Analyzer-Independent QC, Results Entry, and Validation Roadmap

**Organization**: by milestone. Each milestone is one PR and one reviewable unit.
Do not implement a later milestone until the prior milestone's PR is merged or
explicitly approved as a stacked dependency.

**Task format**:
`- [ ] T### [P?] [Mx] [OGC-###] task -- path`

`[P]` means parallelizable within the milestone.

---

## M0: Roadmap and Spec Baseline

Status: this branch.

- [x] T001 [M0] Create clean branch `codex/qc-results-entry-roadmap` from
      `origin/develop`.
- [x] T002 [M0] Confirm OGC-1054 is adjacent/in-progress and not the base for
      this roadmap.
- [x] T003 [M0] Pin mock baseline to `openelis-work`
      `6c24b6ee04aab4ecef96e8daa57dc88ae8821356`.
- [ ] T004 [M0] Review this spec/plan/tasks/checklist with the product owner.
- [ ] T005 [M0] After review, open a documentation PR to `develop`.

## M1: Batch Workplan Foundation

Branch: `feat/427-811-qc-results-m1-batch-foundation`
Primary Jira: OGC-427

Independent proof: user can open Workplan, see Pending Tests and Batches, create
a batch, persist it, and move through lifecycle states without reagent/QC yet.

- [ ] T100 [M1] Create a clean worktree from `origin/develop`.
- [ ] T101 [M1] Resolve current legacy workplan route inventory and removal or
      redirect plan -- `frontend/src/**`, `src/main/java/**`.
- [ ] T102 [M1] Draft milestone evidence plan and mock capture list against
      `designs/quality/batch-workplan-reagent-qc.*`.
- [ ] T103 [M1] RED backend tests for batch lifecycle service.
- [ ] T104 [M1] RED controller tests for pending tests, batch create, batch
      update, and archive endpoints.
- [ ] T105 [M1] Liquibase changesets for batch persistence tables with rollback.
- [ ] T106 [M1] Valueholder, DAO, service, controller, and DTO layers for batch
      persistence.
- [ ] T107 [M1] React route and Carbon UI shell for Pending Tests and Batches.
- [ ] T108 [M1] React Intl keys in `frontend/src/languages/en.json` only.
- [ ] T109 [M1] Focused frontend/component tests for route shell and lifecycle
      controls.
- [ ] T110 [M1] Playwright demo-video story for create batch and lifecycle.
- [ ] T111 [M1] Capture mock and implementation screenshots at required
      viewports.
- [ ] T112 [M1] Complete `code-qa.md` and `mock-comparison.md`.
- [ ] T113 [M1] Run relevant backend/frontend/Playwright validation and record
      commands in PR body.

## M2: Reagent Lot and Shared QC Contract

Branch: `feat/427-811-qc-results-m2-reagent-qc-contract`
Primary Jira: OGC-427; consumer Jira: OGC-811

Independent proof: batch can assign a reagent lot and receive a server-computed
QC state through a contract that Results Entry and Validation can also consume.

- [ ] T200 [M2] Create a clean worktree from `origin/develop` after M1 lands.
- [ ] T201 [M2] Inventory existing reagent lot, inventory, QC, NCE, and audit
      entities; document reuse-first decisions.
- [ ] T202 [M2] Define shared QC/reagent DTOs and state enum: valid, missing,
      overdue, failed, overridden.
- [ ] T203 [M2] RED service tests for QC validity rules and frequency behavior.
- [ ] T204 [M2] RED controller tests for reagent lot assignment and QC state
      response.
- [ ] T205 [M2] Add or adapt Liquibase schema only where existing model cannot
      support the contract.
- [ ] T206 [M2] Implement reagent lot assignment service and shared QC signal
      service.
- [ ] T207 [M2] Add batch UI lot assignment and QC state summary.
- [ ] T208 [M2] Add contract fixtures for Results Entry future consumers.
- [ ] T209 [M2] Capture mock/implementation screenshots for lot assignment and
      QC state.
- [ ] T210 [M2] Complete code-qa and mock comparison notes.

## M3: Inline QC, NCE Override, and Printable Workplan

Branch: `feat/427-811-qc-results-m3-inline-qc-nce-print`
Primary Jira: OGC-427

Independent proof: user can enter inline QC, override invalid QC only through
NCE, and print/preview a workplan with reagent/QC state.

- [ ] T300 [M3] Create a clean worktree after M2 lands.
- [ ] T301 [M3] RED backend tests for inline QC entry, NCE creation, and audit.
- [ ] T302 [M3] RED frontend tests for overdue/failed auto-expand behavior.
- [ ] T303 [M3] Implement inline QC entry service/controller/UI.
- [ ] T304 [M3] Implement QC override flow using NCE service; no modal shortcut
      if the mock requires inline behavior.
- [ ] T305 [M3] Implement printable workplan preview/export.
- [ ] T306 [M3] Add permissions checks for workplan view/create/print and QC
      result entry/override.
- [ ] T307 [M3] Playwright MP4 for full Batch Workplan MVP.
- [ ] T308 [M3] Screenshot comparison against batch workplan mock.
- [ ] T309 [M3] Complete code-qa and PR evidence package.

## M4: Results Entry v4 Worklist and Edit State

Branch: `feat/427-811-qc-results-m4-results-worklist-edit-state`
Primary Jira: OGC-811

Independent proof: `/Results` route renders a narrow worklist slice, expands a
row, and enforces saved-result read-only until row-level Edit.

- [ ] T400 [M4] Create a clean worktree after M3 lands.
- [ ] T401 [M4] Resolve legacy Results routes and redirect/feature-flag plan.
- [ ] T402 [M4] Pin Results Entry v4 and decisions mock screenshots.
- [ ] T403 [M4] RED backend tests for worklist query DTO and save conflict
      guard.
- [ ] T404 [M4] RED frontend tests for row expansion and edit-state behavior.
- [ ] T405 [M4] Implement `/Results` route foundation using Carbon components.
- [ ] T406 [M4] Implement work zone: result value, method, optional analyzer,
      notes, row actions.
- [ ] T407 [M4] Implement saved-result read-only until Edit, then Save relocks.
- [ ] T408 [M4] Add e-signature integration where required by existing service.
- [ ] T409 [M4] Playwright MP4 for worklist/edit-state story.
- [ ] T410 [M4] Screenshot comparison against Results Entry v4 mock.
- [ ] T411 [M4] Complete code-qa and PR evidence package.

## M5: Results Entry Bench Controls

Branch: `feat/427-811-qc-results-m5-results-bench-controls`
Primary Jira: OGC-811

Independent proof: user can enter reagent/QC/control/sample-use/NCE data in
Results Entry using the same contract introduced in M2.

- [ ] T500 [M5] Create a clean worktree after M4 lands.
- [ ] T501 [M5] RED service/controller tests for Results Entry consuming shared
      QC/reagent/NCE signals.
- [ ] T502 [M5] RED frontend tests for combined Reagents, QC, and Controls
      section.
- [ ] T503 [M5] Implement combined Reagents, QC, and Controls section.
- [ ] T504 [M5] Implement inline NCE form and supported dispositions:
      Cancel, Reject plus reason, Retest.
- [ ] T505 [M5] Keep refer-out as a separate row action.
- [ ] T506 [M5] Implement sample partial-use and mark-used-up workflow.
- [ ] T507 [M5] Surface aliquoting from the expanded row.
- [ ] T508 [M5] Implement this-analysis inline paginated history.
- [ ] T509 [M5] Playwright MP4 for full Results Entry bench-controls MVP.
- [ ] T510 [M5] Screenshot comparison against Results Entry v4, reagent-usage,
      and NCE mocks.
- [ ] T511 [M5] Complete code-qa and PR evidence package.

## M6: Validation Risk Triage and Guarded Release

Branch: `feat/427-811-qc-results-m6-validation-risk-release`
Primary Jira: OGC-817

Independent proof: validator can distinguish clear rows from needs-review rows
and release or reject using the v4 rules.

- [ ] T600 [M6] Create a clean worktree after M5 lands.
- [ ] T601 [M6] RED backend tests for risk signal aggregation.
- [ ] T602 [M6] RED frontend tests for Check before release chips and filters.
- [ ] T603 [M6] Implement Validation queue risk chips and counts.
- [ ] T604 [M6] Implement Needs review filters.
- [ ] T605 [M6] Implement guarded release-all-clear with scannable confirm list.
- [ ] T606 [M6] Implement per-row release for flagged results.
- [ ] T607 [M6] Implement Reject through inline NCE.
- [ ] T608 [M6] Implement Retest with configured note requirement.
- [ ] T609 [M6] Integrate e-signature on final release.
- [ ] T610 [M6] Playwright MP4 for Validation v4 MVP.
- [ ] T611 [M6] Screenshot comparison against Validation v4 mock.
- [ ] T612 [M6] Complete code-qa and PR evidence package.

## M7: PNG/CPHL Cross-Domain Evidence and Hardening

Branch: `feat/427-811-qc-results-m7-png-cross-domain-evidence`
Primary Jira: OGC-899; context Jira: OGC-527

Independent proof: clinical, environmental, and vector representative rows
exercise the same analyzer-independent workflows without country-specific code.

- [ ] T700 [M7] Create clean worktree after M6 lands.
- [ ] T701 [M7] Define seeded clinical, environmental, and vector demo rows.
- [ ] T702 [M7] Verify PII masking and domain-specific context behavior.
- [ ] T703 [M7] Verify regulatory limit/reference range behavior per domain.
- [ ] T704 [M7] Verify program/EQA fields where present.
- [ ] T705 [M7] Record cross-domain Playwright MP4s.
- [ ] T706 [M7] Produce final PNG/CPHL evidence bundle and rollup note.
- [ ] T707 [M7] Complete final code-qa and roadmap status update.
