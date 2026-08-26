# Feature Specification: Microbiology Clinical Completeness and Qualification

**Feature branch**: `feat/782-ogc-782-microbiology-m8-clinical-completeness`  
**Parent feature**: OGC-782 routine bacteriology MVP  
**Related product work**: OGC-783, OGC-784, OGC-790, OGC-791  
**Status**: Implementation in progress; base lot traceability implemented,
requiredness policy awaiting product ruling
**Created**: 2026-08-03

## Purpose

Extend the accepted routine-bacteriology workflow with the clinical history,
bench traceability, accessibility, and measured capacity needed for sustained
laboratory use. This feature begins after the OGC-782 MVP implementation and
does not change that MVP's acceptance boundary.

This document defines user and laboratory outcomes. Entity names, database
tables, service boundaries, routes, and component choices belong in
`plan.md`, not in this product contract.

## Source Context

- [OGC-782](https://uwdigi.atlassian.net/browse/OGC-782), the microbiology
  module umbrella
- [OGC-783](https://uwdigi.atlassian.net/browse/OGC-783), microbiology
  non-functional requirements
- [OGC-784](https://uwdigi.atlassian.net/browse/OGC-784), reagent-lot use in
  microbiology
- [OGC-790](https://uwdigi.atlassian.net/browse/OGC-790), case workbench
- [OGC-791](https://uwdigi.atlassian.net/browse/OGC-791), AST entry and review
- [Microbiology workflow walkthrough](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256)
- [M-12 reagent-lot design](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-12-test-reagent-linkage.md)
- [M-NFR design](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-nfr-non-functional-requirements.md)
- The implemented OGC-782 MVP and its evidence under
  `specs/782-ogc-782-microbiology-mvp-spec/`

The linked source artifacts contain useful workflow intent and acceptance
targets, but also contain technical prescriptions. Those prescriptions are not
product requirements. `research.md` records which statements are product gaps,
implementation leakage, contradictions, or engineering decisions.

## Scope

### Included

1. Amendment and re-identification history after a final report.
2. Repeat/retest AST context that preserves the relationship between attempts.
3. Culture-media, AST-card, and AST-disc lot traceability at the bench action
   where each lot was used.
4. Accessibility qualification of the implemented worklist and case workflow.
5. Repeatable scale and performance qualification using service-created data.

### Excluded

- Offline save queues and conflict-resolution workflows
- Analyzer-ingested AST
- Full expert rules, WHONET export, antibiograms, TB processing, and GLASS
- Reagent inventory administration or Test Catalog reagent-link authoring
- A new browser-support policy for OpenELIS as a whole
- Rewriting the existing Inventory or Test Catalog modules

### Delivery Status

Amendment/re-identification history, repeat/retest AST, accessibility
qualification, measured capacity, and policy-neutral reagent/card-lot
traceability are implemented and evidenced on the M8 branch. Bench users can
choose eligible lots, see FEFO guidance and named ineligibility reasons, and
review the exact culture or AST action that consumed each lot. The source
artifacts still conflict on whether existing `PRIMARY / SECONDARY` Test Catalog
roles represent the requiredness policy described as
`REQUIRED / OPTIONAL / SUBSTITUTE`. The implementation does not infer that
mapping, so mandatory/optional/substitute enforcement and full US4 acceptance
remain open.

## User Stories and Acceptance Scenarios

### US1 - Amend a finalized microbiology report (Priority P1)

As an authorized laboratory user, I can correct a finalized microbiology case
without erasing what was previously reported, so the patient record and audit
trail show both the original report and the correction.

**Independent test**: Finalize a case, open an amendment with a reason, change
the identification or AST content, release the amendment, and verify that the
original and amended report versions remain distinguishable and attributable.

**Acceptance scenarios**:

1. A finalized case remains read-only until an authorized user opens an
   amendment and supplies a non-empty reason.
2. Opening an amendment does not remove or silently alter the previously
   released report.
3. While one amendment is open, a second amendment cannot be opened for the
   same case.
4. Every change made during an amendment records who changed it, when, the
   reason, and the before/after clinical values.
5. Releasing the amendment creates a new report version visibly identified as
   amended, closes the amendment, and locks the case again.
6. Cancelling an amendment preserves the prior final report, records the
   cancellation reason, and locks the case again without publishing draft
   changes as a new report.
7. A user without the required case/report permission cannot open, cancel, or
   release an amendment.

### US2 - Preserve isolate re-identification history (Priority P1)

As a microbiology user, I can correct an isolate identification with a reason
and review its identification history, so no organism determination is silently
overwritten.

**Independent test**: Identify an isolate, re-identify it during an amendment,
and verify the timeline shows the old and new identification, reason, user, and
time while the prior report remains available.

**Acceptance scenarios**:

1. Changing a previously reported organism requires an open amendment and a
   non-empty re-identification reason.
2. The case timeline shows each identification change in chronological order
   with both the prior and replacement values.
3. Identification history is append-only through supported application paths.
4. Re-identifying one isolate does not change sibling isolates or sibling
   workflows on the same specimen.

### US3 - Record repeat and retest AST attempts (Priority P1)

As a bench user, I can start a repeat or retest AST attempt from an earlier run
and state why it is being repeated, so the clinical record distinguishes each
attempt instead of overwriting readings.

**Independent test**: Review an AST run, start a repeat with a reason, record a
different set of readings, and verify both attempts and their relationship are
visible and independently auditable.

**Acceptance scenarios**:

1. A repeat/retest names its source run and requires a reason.
2. Earlier readings, interpretations, overrides, and review state remain
   unchanged.
3. The new attempt records the method, panel, breakpoint standard, start/review
   actors and times, and whether it is an original, repeat, or retest.
4. Only reviewed attempts contribute to reportable AST content; when more than
   one reviewed attempt exists, the user explicitly selects the reportable
   attempt or receives a named release blocker.
5. The case UI visually distinguishes original, repeat, and retest attempts
   without relying on color alone.

### US4 - Trace reagent and card lots to bench work (Priority P1)

As a bench user, I select the actual culture-media, AST-card, or AST-disc lots
used during setup, so a reported result can be traced to the consumables that
produced it.

**Independent test**: Configure required and optional reagents for a culture
test, use valid lots during culture and AST setup, and verify the case shows the
selected lots and inventory usage without duplicating catalog or inventory
records.

**Acceptance scenarios**:

1. Required lot selections block setup until completed; optional selections do
   not.
2. Available lots are ordered by earliest effective expiry and show lot number,
   expiry, quantity, and QC state.
3. Expired, exhausted, quarantined, consumed, or failed-QC lots cannot be used.
4. If a selected lot becomes invalid before save, the server rejects the save
   with a specific message naming the lot and reason.
5. Saving setup records usage through the shared Inventory workflow and links
   it to the culture or AST action where it was consumed.
6. Reopening a case shows the recorded lots even if those lots later become
   unavailable for new work.
7. The same lot-selection behavior is reused for culture setup and AST setup.

### US5 - Complete the workflow without accessibility barriers (Priority P1)

As a keyboard or assistive-technology user, I can complete the primary
microbiology workflow with understandable structure, status, focus, and error
feedback.

**Independent test**: Complete worklist filtering, case navigation, isolate
entry, AST entry/review, lot selection, and amendment release using only the
keyboard while automated accessibility checks report no detectable WCAG 2.1 AA
violations on the exercised surfaces.

**Acceptance scenarios**:

1. Every action is keyboard reachable and has a visible focus indicator.
2. Tables have an accessible name; fields have associated labels and helper or
   error descriptions.
3. Expanding or closing an action panel moves and restores focus predictably.
4. Status, interpretation, override, repeat, and critical indicators always
   include text or an accessible name in addition to color.
5. Automated checks cover the worklist, case overview, isolate, AST, critical
   communication, reporting, amendment, and reagent-lot states.

### US6 - Qualify expected laboratory volume (Priority P1)

As a laboratory manager, I can review repeatable performance evidence at the
defined workload, so capacity claims are measured rather than inferred from a
small demo fixture.

**Independent test**: Create the qualification datasets through application
services, run the named measurements, and produce machine-readable and human
readable evidence tied to a commit and environment description.

**Acceptance scenarios**:

1. A 200-case worklist loads in under 2 seconds.
2. A case containing 5 isolates, 80 AST readings, and at least 30 timeline
   events loads in under 1 second.
3. Worklist search returns in under 500 ms and a filter/page change completes
   in under 300 ms at the defined dataset size.
4. Saving an isolate, an AST reading, or a timeline event completes in under
   500 ms at the defined dataset size.
5. Qualification data is created through services, is property-gated, can be
   removed safely, and does not use direct SQL, fixed primary keys, or a
   production-exposed fixture endpoint.
6. Evidence records warm-up policy, iteration count, percentile calculation,
   browser/server versions, database size, commit, and pass/fail result.

## Functional Requirements

- **FR-001**: The system shall preserve every released microbiology report
  version and identify which version corrects which prior version.
- **FR-002**: The system shall require reasoned, authorized amendment lifecycle
  actions and allow at most one open amendment per case.
- **FR-003**: The system shall preserve before/after isolate identification
  history and expose it in the case timeline.
- **FR-004**: The system shall preserve original and repeat/retest AST attempts
  as distinct clinical records.
- **FR-005**: The system shall require an explicit reportable AST attempt when
  multiple reviewed attempts could produce conflicting report content.
- **FR-006**: The system shall validate reagent-lot eligibility again at save
  time and record usage through shared Inventory behavior.
- **FR-007**: The system shall retain historical lot references even after a lot
  is no longer eligible for new use.
- **FR-008**: The implemented microbiology workflow shall meet the accessibility
  scenarios in US5.
- **FR-009**: Performance claims shall be produced by a repeatable qualification
  procedure at the workloads and thresholds in US6.
- **FR-010**: Audit and clinical-history records shall not be editable or
  deletable through supported application endpoints.

## Success Criteria

- **SC-001**: The amendment Playwright journey proves original and amended
  report versions, re-identification history, and the post-release lock.
- **SC-002**: The repeat-AST journey proves two distinct attempts and an
  explicit reportable-attempt decision.
- **SC-003**: The reagent journey proves FEFO lot choice, invalid-lot rejection,
  persisted traceability, and historical display.
- **SC-004**: The registered accessibility suite reports zero detectable WCAG
  2.1 AA violations on every named Microbiology state and the keyboard journey
  passes without mouse input.
- **SC-005**: The registered qualification suite produces a JSON result in
  which every US6 threshold passes on the documented baseline environment.
- **SC-006**: Focused new backend code meets the repository coverage target and
  all schema mappings load in the ORM validation test.

## Clarifications and Defaults

- Previously released content remains valid until an amended report is
  released; opening an amendment does not retract it.
- The first follow-up qualifies the NFR-02, NFR-04, and NFR-05 obligations named
  by the user. It does not claim all ten OGC-783 NFRs are complete.
- Reagent definition and inventory ownership stay with their existing shared
  modules. Microbiology consumes those capabilities at bench-work time.
- Product mockups and design documents guide workflow intent but do not dictate
  storage, routes, services, or component structure.
