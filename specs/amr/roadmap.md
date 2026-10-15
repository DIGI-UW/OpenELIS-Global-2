# Microbiology (AMR) V2 — Implementation Roadmap

This roadmap defines the step-by-step implementation plan to deliver AMR V2 through vertical slices (MVP + Iterations), replacing the previous horizontal design.

Each milestone below represents a fully reviewable, self-contained PR (or small PR stack) that delivers a complete feature slice from database to UI.

## Foundation history (Milestone 1)

The foundational PR stack (Milestone 1) is now complete, providing the baseline infrastructure for V2:

1. **Retire V1 (PR #4646)**: Ripped out the V1 microbiology workflows, legacy routing, and retired schema.
2. **Case Structure & Migration (PR #2)**: Restructured the database schema to support independent cases, updated case membership constraints, and ensured legacy case compatibility.
3. **Routing & Case Creation (PR #3 - M5)**: Implemented order routing, case creation, set warnings, and proper handling of order edits/cancellations (including requiring a reason when dropping tests with results or dropping the last micro test).

_Note: The archived work from previous horizontal attempts remains highly useful as a reference for UI components and business logic, but will be integrated slice-by-slice._

## Current validation phase

The 14 October 2026 audit exercised the assembled M8 application and found nine
confirmed workflow and design issues. M9 below owns remediation and integrated
acceptance. The [audit](milestone-9-validation.md) distinguishes observed behavior
from untested journeys; it does not mark earlier milestones accepted or certify CI.

## Milestone 1: The Core Foundation (Completed)

**Goal:** Rip out V1, establish the V2 schema, and enable basic case routing.

- [x] Remove V1 workflow types, culture setups, and old routing.
- [x] Restructure case, sample, and program relationships.
- [x] Implement V2 routing on order save (including electronic/reflex orders).
- [x] Handle order edits (add/cancel tests, require reasons for dropping micro tests).

---

## Milestone 2: MVP Case View & Access (Implemented; validation in review)

**Goal:** Allow users to view the cases created by Milestone 1 and manage basic case access.

- [x] Build the Case View shell (header, related cases, sample list).
- [x] Implement Case-Lab-Unit access controls (read/write permissions based on lab unit).
- [x] Build the Case search and worklist listing.
- [x] Support basic case transfers between lab units.

The contract and validation scope are recorded in [milestone-2.md](milestone-2.md).
Focused checks establish implementation behavior; full local CI, GitHub
checkpoints and owner acceptance remain separate gates.

## Milestone 3: MVP Initial Testing & Results (Implemented; validation in review)

**Goal:** Enable lab technicians to enter basic results for case tests.

- [x] Implement the shared chooser for Initial/Additional testing.
- [x] Build the single result table and inline editor for multi-component results.
- [x] Implement basic result validation (Block self-validation).
- [x] Add support for "Tested elsewhere" and basic case notes.

The contract, focused evidence and remaining shared design dependencies are
recorded in [milestone-3.md](milestone-3.md). Full CI and owner acceptance
remain separate gates.

## Milestone 4: MVP Culture Rows & Media Tracking

**Goal:** Support the growth and tracking of cultures.

- [ ] Add culture row inoculation (media links, tracked media settings).
- [ ] Implement reading tracking (check due, incubation complete, extensions).
- [ ] Build the culture tree (tests on a culture, subcultures, Gram stain shortcut).

## Milestone 5: MVP Isolates & Referral

**Goal:** Support picking isolates and referring them out.

- [ ] Implement isolate picking from culture rows and recording identification history.
- [ ] Support isolate sample items and received isolates.
- [ ] Implement the referral workflow for remaining work/isolates.

## Milestone 6: AST/DST & Reporting

**Goal:** Enable antibiotic susceptibility testing and final reporting.

- [ ] Implement AST runs, panel selection, and readings/overrides.
- [ ] Add TB classification logic and NTM off-ramp.
- [ ] Implement partial/final release of cases and server-side final locks.
- [ ] Integrate with the Patient Report micro block.

## Milestone 7: Integration & Automation (Incoming Results & Labeling)

**Goal:** Connect the MVP features with instruments and physical lab workflows.

- [ ] Build the incoming results queue for instrument integrations.
- [ ] Ensure results map correctly to existing culture rows/tests.
- [ ] Implement Worklist/Bench sheet printing and culture filters.
- [ ] Add per-container label scope and scanning support.

## Milestone 8: Environmental Cases & Final Migration

**Goal:** Handle non-human environmental samples and finalize the clinical data migration.

- [ ] Implement site-subject cases and environmental fields.
- [ ] Support WHONET export for surveillance populations.
- [ ] Finalize the clinical migration (assigning legacy data to new lab units/programs).
- [ ] Supply the environmental and migration journeys for integrated acceptance in M9.

## Milestone 9: Workflow Remediation & Integrated Acceptance (Planned)

**Goal:** Make the assembled V2 workspace consistent with the pinned design and
usable from order entry through reporting, before UAT setup and workflow recording.

**Baseline:** [14 October audit](milestone-9-validation.md), nine confirmed findings
(UX-01–09), with [committed evidence](evidence/m9/2026-10-14/README.md).
**Implementation plan:** [milestone-9.md](milestone-9.md).

- [ ] Unify case entry routes, expose required clinical information, and provide
      consistent work/release state and reasoned amendment access (UX-01–02).
- [ ] Preserve applicable worklist filters, surface waiting incoming results,
      and restore sample/header context (UX-03–05).
- [ ] Correct laboratory date/time presentation and referral handoff precision;
      complete culture previews, attribution, shared notes, and actionable errors
      (UX-06–09).
- [ ] Reproduce each finding before its fix, add focused regression coverage, and
      compare the corrected journeys with the pinned mocks at recorded viewports.
- [ ] Complete the audit's remaining acceptance lanes using normal laboratory
      roles and isolated synthetic data; record failures separately from gaps.
- [ ] Pass local CI and GitHub checks on the final candidate, then obtain owner
      workflow acceptance before UAT setup and recording.

M9 uses `codex/amr-v2-ux-validation`, already in the native `gh stack` above M8.
It is one milestone PR containing ordered, usable workflow changes. The audit and
plan are the preparation commit; implementation and acceptance remain outstanding.
