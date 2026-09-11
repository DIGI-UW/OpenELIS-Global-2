# Feature Specification: Analyzer-Independent QC, Reagent, Results Entry, and Validation Roadmap

**Feature Branch**: `codex/qc-results-entry-roadmap`
**Created**: 2026-06-28
**Status**: Draft roadmap/specification
**Jira Scope**: OGC-427, OGC-428 context only where shared QC applies, OGC-811, OGC-817, OGC-899
**Out-of-scope reference**: OGC-1054 analyzer profiles/mapping is already in progress and is not a dependency for this roadmap.

## Summary

Deliver an analyzer-independent sequence for the PNG/CPHL QC, reagent
traceability, Results Entry v4, and Validation v4 work. The sequence starts from
the concrete OGC-427 Batch Workplan with Reagent QC story, extracts the shared
QC/reagent/NCE contract that Results Entry v4 needs, then implements Results
Entry and Validation slices against that contract.

The roadmap explicitly avoids depending on analyzer profile work, bridge
transport, FILE polling, ASTM/HL7 ingestion, or OGC-1054. Analyzer-derived data
may populate these workflows later, but the MVP must work for manual bench
workflows and regular OpenELIS worklists.

## Current Authority

- OGC-811 is the current Results Entry authority; its v4 block supersedes
  conflicting OGC-517 details.
- OGC-517 remains historical design input only.
- OGC-427 is the first implementation anchor because it is assigned, concrete,
  and due on 2026-07-10.
- OGC-817 is the Validation companion epic and must consume the same QC/NCE
  signals after Results Entry establishes them.
- OGC-899 is the PNG/CPHL program umbrella and evidence rollup target.

## Mock and Design Baseline

Pin mock validation to `digi-uw/openelis-work` commit:

`6c24b6ee04aab4ecef96e8daa57dc88ae8821356`

Baseline artifacts:

- Batch Workplan with Reagent QC:
  `designs/quality/batch-workplan-reagent-qc.md`
  `designs/quality/batch-workplan-reagent-qc.jsx`
- Results Entry v4:
  `designs/results-validation/results-entry-v4.md`
  `designs/results-validation/results-entry-v4.html`
  `designs/results-validation/results-entry-v4-decisions.md`
- Results reagent usage detail:
  `designs/results-validation/results-page-reagent-usage-v2.1-preview.html`
  `designs/results-validation/results-page-reagent-usage-v2.1.jsx`
- Inline NCE reference:
  `designs/nce/nce-results-entry.md`
  `designs/nce/nce-results-entry.jsx`
- Validation v4:
  `designs/results-validation/validation-page-v4.md`
  `designs/results-validation/validation-page-v4.html`

Every UI milestone must capture baseline screenshots from those mock artifacts
and compare them against implementation screenshots before the PR is marked
ready.

## Goals

- Replace fragmented batch workplan behavior with a persisted, unified batch
  workplan flow that supports reagent lot assignment and QC verification.
- Define a shared analyzer-independent QC/reagent/NCE contract that Batch
  Workplan, Results Entry, and Validation consume consistently.
- Deliver a Results Entry v4 MVP with the v4 edit-state model, inline NCE,
  reagent/QC/control capture, sample usage, aliquoting, and scoped history.
- Deliver a Validation v4 MVP that consumes those same signals for release
  triage and guarded release.
- Produce reviewer-ready evidence for every milestone: code-qa notes, focused
  tests, MP4 user-story video, implementation screenshots, and mock comparison.

## Non-Goals

- No analyzer profile, analyzer type, or mapping GUI work.
- No analyzer bridge, mock server, ASTM, HL7, or FILE transport work.
- No OpenELIS application-side FILE poller.
- No dependency on OGC-1054 landing first.
- No full rewrite of every legacy Results route in the first MVP.
- No new custom CSS framework; Carbon only.
- No non-English locale edits; add new keys to `frontend/src/languages/en.json`
  only.

## Users and Jobs

### Lab Technician

Needs one work surface to group pending tests into batches, assign reagent lots,
verify QC state, enter QC when required, print a workplan, and later enter
results with the same reagent/QC/NCE context visible.

### Validator

Needs a queue that separates clear results from results requiring review because
of NCE, QC fail, modification, nonconforming status, critical acknowledgment, or
similar release risk.

### Lab Supervisor

Needs traceable proof that reagent lot use, QC overrides, NCE creation, result
modification, and release decisions are audited consistently.

## MVP Scope

### MVP A: Batch Workplan and Shared QC Contract

MVP A is complete when a lab user can:

1. Open a unified workplan route.
2. Filter pending tests.
3. Create and persist a batch.
4. Move a batch through `DRAFT`, `ACTIVE`, `COMPLETED`, and `ARCHIVED`.
5. Assign a reagent lot to the batch.
6. See whether required QC is valid, missing, failed, or overdue.
7. Enter inline QC for the reagent lot.
8. Override invalid/missing QC only by creating an NCE.
9. Print or preview a workplan with reagent/QC state included.

### MVP B: Results Entry v4 Bench Flow

MVP B is complete when a lab user can:

1. Use `/Results` as the unified route for a narrow initial worklist slice.
2. Expand a row into the v4 work zone.
3. Respect the v4 edit state: saved results are read-only until row-level Edit.
4. Enter result value, method, notes, reagent/QC/control data, and sample usage.
5. Open the real inline NCE form without a modal.
6. Save with e-signature where required.
7. See this-analysis history inline and paginated.
8. Complete the same flow for at least one clinical and one non-clinical/domain
   representative row where seeded data exists.

### MVP C: Validation v4 Companion

MVP C is complete when a validator can:

1. Open a Validation queue with "check before release" signals.
2. Filter by Needs review, NCE, QC fail, Modified, Ack pending, Critical, and
   Abnormal.
3. Release clear rows through a guarded confirmation list.
4. Review and release flagged rows one at a time.
5. Reject by filing an inline NCE.
6. Retest with required note behavior controlled by configuration.
7. Use e-signature for final release.

## Core Requirements

### Functional Requirements

- **FR-001**: The Batch Workplan route must expose Pending Tests and Batches
  surfaces.
- **FR-002**: Batch creation must persist server-side and survive logout.
- **FR-003**: Batch lifecycle transitions must be service-owned and audited.
- **FR-004**: Reagent lot assignment must be explicit and visible on the batch.
- **FR-005**: QC validity must be computed server-side for the selected reagent
  lot and requirement frequency.
- **FR-006**: Invalid, failed, overdue, or missing QC must be visible before
  print or activation decisions.
- **FR-007**: QC override must create an NCE and record actor, reason, and
  affected batch/test context.
- **FR-008**: Results Entry v4 must implement saved-result read-only state until
  row-level Edit is selected.
- **FR-009**: Method and analyzer must remain separate fields; analyzer is
  optional in this analyzer-independent MVP.
- **FR-010**: Results Entry must include a combined Reagents, QC, and Controls
  section without requiring analyzer profiles.
- **FR-011**: Inline NCE must use the real inline form pattern, not a modal.
- **FR-012**: Refer-out must remain separate from NCE disposition.
- **FR-013**: Sample usage must support partial amount used and mark-used-up
  workflow where backend support exists or is added in the milestone.
- **FR-014**: Aliquoting must be available from Results Entry using existing
  aliquot behavior where possible.
- **FR-015**: History must be scoped to this analysis/result and rendered inline
  with pagination.
- **FR-016**: Validation must consume shared QC/NCE/modification signals instead
  of recomputing incompatible state.
- **FR-017**: All user-facing strings must use React Intl with new keys in
  `en.json` only.
- **FR-018**: All schema changes must be Liquibase changesets with rollback for
  structural changes.

### Evidence Requirements

- **ER-001**: Every UI milestone must include a Playwright MP4 generated by the
  repo demo-video project.
- **ER-002**: The MP4 must exercise the user story through visible UI behavior;
  API calls cannot be used as proof.
- **ER-003**: Setup/cleanup helpers may use backend fixtures only when they are
  outside the proof path and documented.
- **ER-004**: Every UI milestone must include baseline mock screenshots and
  implementation screenshots at matching viewport sizes.
- **ER-005**: Every UI milestone must include a mock comparison note describing
  alignment, intentional deviations, and blocking mismatches.
- **ER-006**: Every PR must include a code-qa note covering spec alignment,
  meaningful tests, architecture, simplicity, legacy removal, i18n, security,
  and evidence.

## Success Criteria

- Each milestone lands as a reviewable PR with one milestone's scope.
- No milestone extends analyzer-owned code to finish analyzer-independent
  workflows.
- The shared QC/reagent/NCE contract is consumed by Batch Workplan, Results
  Entry, and Validation without duplicate implementations.
- UI evidence is visually comparable to the pinned mock baseline.
- Local validation and CI evidence are included in each PR body.
