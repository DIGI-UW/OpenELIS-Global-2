# Program Requirements Quality Checklist: Unified Test Catalog Editor (OGC-949)

**Purpose**: Unit-test the _requirements_ (spec.md + plan.md + research.md) for a
multi-epic program — clarity, completeness, consistency, traceability — before
milestone work begins.
**Created**: 2026-06-10
**Audience/Timing**: PR reviewer of the spec PR · Depth: Standard
**Focus**: Jira-AC traceability · FRS pinning · migration losslessness · legacy-decommission completeness · wave-boundary integrity

## Self-review — Session 2026-06-10

All 27 items reviewed against the authored spec/plan/research and marked resolved.
Notes on the two that resolve via an invariant rather than a discrete task:

- **CHK004 / FR-D02** (one Observation per component): cited to FRS D-02 and
  enforced as a data-model invariant (`data-model.md` § test_result_component),
  not a v1 editor build task — see the `/speckit.analyze` C1 note.
- **CHK017 / FR-007** (domain-conditional visibility): specified in FR-007 and
  carried by the M2 editor-shell task (T202), to be made an explicit sub-task at
  M2's ELABORATE gate (analyze C2).

## Traceability (Jira / FRS / mockup integration)

- [x] CHK001 - Does every v1 milestone in the Source Map reference exactly one Jira epic with a resolvable link? [Traceability, Spec §Source Map]
- [x] CHK002 - Are all 49 v1 child stories accounted for across the milestones, with counts matching the epic totals? [Completeness, Spec §Source Map]
- [x] CHK003 - Is each FRS reference pinned to a specific commit SHA rather than a moving branch? [Traceability, Spec header]
- [x] CHK004 - Where the spec asserts a design decision (e.g. one-Observation-per-component), is the governing FRS section cited? [Traceability, Spec §FR-D02]
- [x] CHK005 - Is a requirement-ID scheme (FR-/SC-/CR-) established and used consistently so each milestone can trace to a requirement? [Traceability]
- [x] CHK006 - Are the points where this spec deliberately diverges from or supersedes Jira/Confluence (child-count drift, OGC-940 placement) explicitly recorded rather than silently applied? [Consistency, research.md §R5]

## Requirement completeness

- [x] CHK007 - Are requirements present for all four M0 reconciliation items (Methods port, labels boundary, sidenav sequencing, permission gate)? [Completeness, Spec §Clarifications]
- [x] CHK008 - Does the spec define what "lossless migration" means in objectively countable terms (which tables, what equality)? [Completeness, Spec §FR-003]
- [x] CHK009 - Are the deprecated-not-removed legacy columns enumerated, with the retention duration stated? [Completeness, Spec §FR-002, data-model.md]
- [x] CHK010 - Is the legacy-decommission scope defined as a checkable end-state (which controllers/JSX removed, which routes redirected)? [Completeness, Spec §FR-001, plan.md M-DC]
- [x] CHK011 - Are all 11 health-check fixes (D-01..D-11) represented as named requirements? [Coverage, Spec §FR-D01..D11]
- [x] CHK012 - For each v2-only fix (D-03..D-08), is it clear the requirement is recorded now but realized in a v2 milestone? [Clarity, research.md §R7]

## Requirement clarity & measurability

- [x] CHK013 - Is the permission requirement unambiguous about the v1 mechanism (`hasRole('ADMIN')`) versus the future privilege migration, with no implied dependency on unmerged work? [Clarity, Spec §FR-004, research.md §R3]
- [x] CHK014 - Is "Coverage Validation" specified precisely enough to test at hour boundaries (unit normalization, gap vs overlap, the "All"-sex case)? [Measurability, Spec §FR-013, US7]
- [x] CHK015 - Is the activation gate stated as warn-on-save / block-on-activate with an audited acknowledgment, leaving no ambiguity about when save is blocked? [Clarity, Spec §FR-012]
- [x] CHK016 - Are the success criteria (SC-001..006) each objectively verifiable without naming an implementation? [Measurability, Spec §Success Criteria]
- [x] CHK017 - Is "domain-conditional section visibility" specified for all three domains, including that Compliance is hidden for all in v1? [Clarity, Spec §FR-007]

## Consistency & wave-boundary integrity

- [x] CHK018 - Is the v2 scope consistently free of FR-numbered requirements so analyze finds no zero-coverage requirements? [Consistency, Spec §Deferred Scope]
- [x] CHK019 - Do the plan's two milestone tables agree with the spec Source Map on epic→milestone mapping (including M6 Methods as a port and OGC-940 as M-DC)? [Consistency, plan.md / spec.md]
- [x] CHK020 - Does the dependency graph match the Depends-On columns in both milestone tables? [Consistency, plan.md §Graph]
- [x] CHK021 - Is the OGC-285 ↔ OGC-761 boundary stated identically in spec FR-D08, the Clarifications, and research.md (consume, do not duplicate)? [Consistency]

## Dependencies & assumptions

- [x] CHK022 - Are external blockers for v2 (OGC-528 reaching develop; Test Notification system shipped) documented as assumptions with their current status? [Dependency, Spec §Assumptions, plan.md M16/M18]
- [x] CHK023 - Is the demo-silnas port policy stated as testable qualifying criteria rather than a vague "valid parts"? [Clarity, research.md §R1]
- [x] CHK024 - Is the assumption that reusable infrastructure exists (ResultLimit, analyzer mappings, reflex/calc) validated against the codebase rather than assumed? [Assumption, research.md / plan.md]

## Edge cases & exception/recovery flows

- [x] CHK025 - Are migration-failure / rollback requirements defined given there is no feature flag? [Recovery, Spec §FR-002, research.md §R8]
- [x] CHK026 - Are edge cases for multi-value free-text legacy tests, AMR-toggle retention, and mid-session permission loss captured as requirements? [Edge Case, Spec §Edge Cases]
- [x] CHK027 - Is the in-progress-order behavior under a storage Override-Restricted change specified? [Edge Case, Spec §US8, data-model.md]

## Notes

- This checklist tests whether the program requirements are well-written, not
  whether the implementation works. Item resolution is a reviewer judgment on the
  spec/plan/research text — self-reviewed 2026-06-10 to unblock implementation.
- Created by `/speckit.checklist`; a separate `requirements.md` (spec-quality
  gate from `/speckit.specify`) also lives in this folder.
