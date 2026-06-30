# Feature Specification: Microbiology MVP Workflow

**Feature Branch**: `spec/782-ogc-782-microbiology-mvp-spec`
**Created**: 2026-06-27
**Status**: Draft
**Input**: User description: "Create a microbiology-specific feature spec that distills the important features from Jira, Confluence, openelis-work specs/mockups, and repo research; reference the proper files; make the product behavior crystal clear for planning and implementation without letting product specs mandate technical implementation details."

## Source References

This feature spec is the product-facing distillation for planning. The detailed
source artifacts remain useful references, but table names, service names,
routes, schemas, and ownership assumptions inside those artifacts are not
binding product requirements.

- Confluence narrative: [Microbiology Module - Workflow Walk-through](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256)
- Public design bundle:
  [openelis-work/designs/microbiology](https://github.com/DIGI-UW/openelis-work/tree/main/designs/microbiology)
- M-00 parent:
  [m-00-micro-module-parent.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-00-micro-module-parent.md)
- M-01 reference data:
  [m-01-amr-reference-data.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-01-amr-reference-data.md)
- M-02 breakpoint catalog:
  [m-02-breakpoint-catalog.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-02-breakpoint-catalog.md)
- M-03 order entry hook:
  [m-03-order-entry-micro-hook.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-03-order-entry-micro-hook.md)
- M-04 case workbench:
  [m-04-case-workbench-core.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-04-case-workbench-core.md)
- M-05 AST entry and interpretation:
  [m-05-ast-entry-and-interpretation.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-05-ast-entry-and-interpretation.md)
- M-07 worklist:
  [m-07-worklists.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-07-worklists.md)
- M-09 WHONET export:
  [m-09-whonet-export.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-09-whonet-export.md)
- M-11 critical notification:
  [m-11-critical-result-acknowledgment.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-11-critical-result-acknowledgment.md)
- M-12 reagent linkage:
  [m-12-test-reagent-linkage.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-12-test-reagent-linkage.md)
- M-14 TB workflow:
  [m-14-mycobacteriology-tb.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-14-mycobacteriology-tb.md)
- M-NFR:
  [m-nfr-non-functional-requirements.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-nfr-non-functional-requirements.md)
- Local engineering crosswalk:
  `specs/roadmaps/analyzer-microbiology-engineering-crosswalk.md`
- Local product cleanup list:
  `specs/roadmaps/microbiology-spec-health-cleanup-list.md`
- Primary Jira references: OGC-782, OGC-786, OGC-787, OGC-789, OGC-790,
  OGC-791, OGC-792, OGC-794, OGC-785, OGC-925.

## Interpretation Rules for Planning

- This spec defines product behavior and acceptance expectations.
- Detailed M-* source files provide workflow detail and mockup context, but
  implementation-heavy language in them must be filtered through the local
  engineering crosswalk.
- Planning and tasks may choose table names, service boundaries, API shapes,
  routes, migrations, and reuse points, but those choices should not be treated
  as Casey-owned product requirements.
- Mockups are visual workflow references, not implementation contracts.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Route a Microbiology Order (Priority: P1)

As an order-entry user, I need a culture-capable ordered test to route itself
into the correct microbiology workflow so that the lab does not miss creating a
case when the clerk forgets to choose a program manually.

**Why this priority**: Without reliable routing, no downstream microbiology
workflow is dependable.

**Independent Test**: Can be tested by entering an order that includes a
microbiology culture-capable test and confirming that the microbiology work
appears in the worklist with the correct workflow context.

**Acceptance Scenarios**:

1. **Given** a test that is configured to start routine bacteriology work,
   **When** the user places an order for that test, **Then** the system
   indicates that a microbiology case will be created and captures the
   microbiology order details needed by the lab.
2. **Given** a test that is not configured for microbiology culture work,
   **When** the user places an order for that test, **Then** the microbiology
   order fields do not appear and no microbiology case is created.
3. **Given** one physical specimen with both routine bacteriology and TB work
   ordered, **When** the order is saved, **Then** the system represents those as
   separate microbiology workflows for the same specimen without requiring
   duplicate accessioning.

---

### User Story 2 - Work a Bacteriology Case (Priority: P1)

As a microbiology technologist, I need one case surface for a specimen's
bacteriology workup so that I can record culture setup, growth observations,
organism identification, AST readiness, and reporting progress without paper
logs or duplicate transcription.

**Why this priority**: This is the core MVP workflow for running a bacterial
culture from receipt through finalization.

**Independent Test**: Can be tested by opening a newly routed case, recording
setup and culture progress, adding an isolate, identifying the organism, and
confirming the case shows the next required action.

**Acceptance Scenarios**:

1. **Given** a newly created bacteriology case, **When** a technologist opens
   the case, **Then** the case displays specimen, patient, workflow, and next
   action information in a way that supports immediate bench work.
2. **Given** a case in culture setup, **When** the technologist records media or
   bottle setup, **Then** the case moves forward and records who performed the
   action and when.
3. **Given** growth or a positive signal is observed, **When** the technologist
   records the observation, **Then** the case prompts isolate workup and makes
   the event visible in the case history.
4. **Given** an organism has been identified, **When** the technologist marks it
   as clinically significant, **Then** the case supports AST setup for that
   isolate.

---

### User Story 3 - Enter and Review AST (Priority: P1)

As a technologist or supervisor, I need to enter AST readings and see their
susceptibility interpretation so that antibiotic results are calculated,
reviewed, and corrected with an audit trail before reporting.

**Why this priority**: AST is the central clinical value of the bacteriology
MVP.

**Independent Test**: Can be tested by entering AST readings for an identified
isolate and confirming interpretations, review state, override behavior, and
report readiness.

**Acceptance Scenarios**:

1. **Given** an identified significant isolate, **When** the technologist starts
   AST entry, **Then** the system offers the expected antibiotic panel and
   breakpoint context for that organism and specimen.
2. **Given** MIC or zone values are entered, **When** the values are saved,
   **Then** the system displays susceptibility interpretations and clearly
   marks readings that need manual judgment.
3. **Given** a user overrides an interpretation, **When** the override is saved,
   **Then** the original reading remains reviewable and the override requires a
   reason.
4. **Given** AST results have not been reviewed, **When** a supervisor attempts
   final release, **Then** the case is not considered ready for final reporting.

---

### User Story 4 - Work from a Shared Microbiology Queue (Priority: P2)

As an on-shift technologist, I need a shared worklist organized by what needs
action next so that the team can safely work multi-day microbiology cases
without assigning ownership to one person.

**Why this priority**: Microbiology work is shift-based and event-driven; a
shared state-driven queue prevents cases from being overlooked.

**Independent Test**: Can be tested by creating cases in different states and
confirming the worklist groups, filters, highlights, and navigates to the right
case action.

**Acceptance Scenarios**:

1. **Given** multiple in-flight cases, **When** the user opens the microbiology
   worklist, **Then** cases are visible by current state and due action.
2. **Given** a case receives a positive signal or AST results become ready for
   review, **When** the worklist refreshes, **Then** that case is visibly
   prioritized for action.
3. **Given** a specimen has sibling bacteriology and TB workflows, **When** the
   user views the worklist or either case, **Then** the relationship is visible
   without merging their separate workflows.

---

### User Story 5 - Release Reports and Log Critical Communications (Priority: P2)

As a technologist or supervisor, I need preliminary/final release readiness and
critical communication logging so that urgent findings are communicated and
reports are released only when required clinical work is complete.

**Why this priority**: Reporting and critical communication are patient-safety
requirements for microbiology.

**Independent Test**: Can be tested by logging a critical communication,
releasing a preliminary report from an eligible case, and confirming final
release is blocked until readiness requirements are met.

**Acceptance Scenarios**:

1. **Given** a critical finding such as a positive sterile-site culture,
   **When** the user logs a critical communication, **Then** the system records
   the target, recipient, message, time, and acknowledgment state.
2. **Given** preliminary information is available, **When** the user releases a
   preliminary report, **Then** the case records the release and keeps the case
   open for later results.
3. **Given** final release is attempted, **When** required isolate, AST, review,
   or critical follow-up work is incomplete, **Then** the system blocks final
   release and explains what remains.

---

### User Story 6 - Prepare Surveillance Outputs (Priority: P3)

As a surveillance officer or lab manager, I need finalized microbiology results
to be export-ready for WHONET so that AMR surveillance reporting can be
performed without rebuilding the same result data manually.

**Why this priority**: Surveillance is a planned extension of the MVP workflow
and must be protected by the core data captured during cases and AST.

**Independent Test**: Can be tested by finalizing cases with organism and AST
data and confirming the system can identify which finalized results are ready
or missing mapping for export.

**Acceptance Scenarios**:

1. **Given** finalized bacteriology cases with AST results, **When** export
   readiness is checked, **Then** the system identifies included organisms,
   antibiotics, specimen types, and missing mappings.
2. **Given** export is not part of the current release slice, **When** a user
   encounters export actions, **Then** the system makes the planned status clear
   without pretending the capability is complete.

### Edge Cases

- A clerk manually selects Microbiology without a culture-capable test.
- A culture-capable test is ordered without a default culture setup.
- One specimen has both routine bacteriology and TB ordered.
- A case is routed to the wrong workflow and must be corrected before or after
  bench work begins.
- A specimen is lost, contaminated, mislabeled, or rejected after a case exists.
- A case has no growth and needs final negative reporting.
- An isolate is later reidentified after preliminary or final release.
- AST has no matching breakpoint for an organism, drug, specimen, or method.
- AST readings fail QC or require repeat testing.
- Critical communication goes to a clinician not found in the provider
  directory.
- WHONET readiness finds unmapped organisms, antibiotics, specimen types, or
  patient origin codes.
- Large worklists include many open cases and must remain usable by keyboard
  and screen reader users.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: The system MUST provide a reliable way for ordered tests to start
  the appropriate microbiology workflow without relying on clerk memory.
- **FR-002**: Users MUST be able to capture microbiology order details needed
  for culture setup, including culture setup default, patient origin, number of
  sets, clinical history, antibiotic exposure, and critical notification
  preference.
- **FR-003**: The system MUST support separate bacteriology and TB workflows for
  the same physical specimen without duplicate accessioning.
- **FR-004**: Users MUST be able to open a microbiology case and understand the
  current workflow, specimen context, stage, next action, and prior activity.
- **FR-005**: Users MUST be able to record culture setup, incubation progress,
  positive or growth observations, no-growth finalization, and specimen-loss or
  rejection events.
- **FR-006**: Users MUST be able to create and update isolates, record organism
  identification, record clinical significance, and distinguish preliminary
  from final organism identification.
- **FR-007**: Users MUST be able to set up AST only when the isolate context is
  sufficient to support interpretation.
- **FR-008**: Users MUST be able to enter manual AST readings and see
  susceptibility interpretation, including clear guidance when no standard
  breakpoint is available.
- **FR-009**: Users MUST be able to review analyzer-ingested AST results when
  available in later slices, and analyzer-ingested results MUST require human
  review before final reporting.
- **FR-010**: Users MUST be able to override AST interpretations with a reason
  while preserving the original reading for audit and review.
- **FR-011**: Users MUST be able to repeat or retest AST without overwriting the
  prior run or hiding the reason for repeat testing.
- **FR-012**: The system MUST provide a shared microbiology worklist organized
  by case state, due action, urgency, and review need rather than per-case
  ownership.
- **FR-013**: The worklist MUST allow users to filter, sort, and open cases or
  AST work that needs action.
- **FR-014**: The system MUST make sibling workflows on the same specimen
  visible in the worklist and case view while keeping their lifecycles and
  reports separate.
- **FR-015**: Users MUST be able to release preliminary reports when eligible
  and final reports only when readiness checks pass.
- **FR-016**: The system MUST preserve report history when a case is amended or
  reidentified after release.
- **FR-017**: Users MUST be able to log critical communications for case,
  isolate, sample, or result context, including recipient, message, time,
  method, acknowledgment state, and follow-up.
- **FR-018**: Critical communications MUST surface through the existing
  operational alerts workflow rather than a parallel alerts experience.
- **FR-019**: The system MUST support reference data needed for MVP
  microbiology work: organisms, antibiotics, AST panels, culture setup recipes,
  breakpoint standards, patient origin, specimen mapping, and reagent/lot
  linkage.
- **FR-020**: The system MUST prepare finalized microbiology data for WHONET
  surveillance export by tracking export-relevant organism, specimen,
  antibiotic, breakpoint, and interpretation information.
- **FR-021**: Phase 1A MUST support manual bacteriology workflow end-to-end;
  analyzer automation, expert rules, full WHONET export, TB workflow, and GLASS
  reporting may be planned extensions unless included in an explicit later
  slice.
- **FR-022**: Product specs and Jira tickets for this feature MUST describe
  workflow behavior and acceptance outcomes, not required table names, service
  names, route names, or storage layout.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System v1.15+ exclusively.
- **CR-002**: All user-facing strings MUST be internationalized via React Intl;
  new source strings go in `en.json`.
- **CR-003**: Backend implementation MUST follow the OpenELIS 5-layer
  architecture and keep transaction ownership in services.
- **CR-004**: Database changes MUST use Liquibase changesets with rollback for
  structural changes.
- **CR-005**: External healthcare interoperability MUST use FHIR R4/IHE-aligned
  patterns where applicable.
- **CR-006**: Country or deployment variation MUST be configuration-driven, not
  forked in code.
- **CR-007**: Security, audit trail, role-based access control, and input
  validation are required for clinical and administrative actions.
- **CR-008**: Tests MUST be included for new backend behavior, frontend flows,
  and migration/ORM validity; backend tests use JUnit 4.
- **CR-009**: Because the full module is larger than three days, implementation
  MUST be split into validation milestones, each suitable for one PR.

### Key Entities

These are product concepts. Engineering may choose the final storage and API
shape during planning.

- **Microbiology Case**: One microbiology workflow for one physical specimen,
  from receipt through final or amended report.
- **Workflow Type**: The kind of microbiology work being performed, such as
  routine bacteriology or mycobacteriology/TB.
- **Culture Setup Recipe**: The lab recipe for media, incubation, atmosphere,
  and related setup defaults for a culture test.
- **Case Activity / Timeline**: The chronological record of actions,
  observations, notes, stage changes, and analyzer events associated with a
  case.
- **Isolate**: A distinct organism identified during the case workup, with
  significance and identification history.
- **AST Work**: Susceptibility testing for one isolate, including panel,
  method, readings, interpretations, overrides, review, and repeat testing.
- **Breakpoint Standard**: The versioned rule set used to interpret AST
  readings.
- **Critical Communication**: A clinically required notification and
  acknowledgment record for urgent findings.
- **Microbiology Worklist**: The shared operational queue of microbiology cases
  and AST work requiring attention.
- **Surveillance Export Readiness**: The finalized result and mapping state
  needed to produce WHONET-compatible surveillance output.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: A user can enter a qualifying routine bacteriology order and see
  the resulting microbiology work on the worklist without manual program
  routing.
- **SC-002**: A technologist can complete the MVP bacteriology path from order
  routing through culture setup, isolate identification, manual AST entry,
  review readiness, and report release using one case workflow.
- **SC-003**: A case with incomplete required work cannot be final-released, and
  the user can identify the blocking item without consulting paper logs.
- **SC-004**: A user can distinguish sibling bacteriology and TB workflows for
  the same specimen in both the worklist and case context.
- **SC-005**: AST override history preserves the original reading, the changed
  interpretation, actor, time, and justification.
- **SC-006**: A user can log a critical communication without being blocked by
  incomplete provider directory data.
- **SC-007**: Worklist users can find urgent positive/growth/AST-review work
  within 30 seconds in a seeded test dataset of at least 200 in-flight items.
- **SC-008**: All MVP user-facing microbiology strings are represented by i18n
  keys and can be rendered in English.
- **SC-009**: Engineering planning can produce implementation tasks without
  needing Casey artifacts to decide schema, API, route, or service ownership.

## Planning Notes

The following technical directions are currently favored by evidence, but they
belong to planning and task generation rather than this product spec:

- The ordered test should be the source of workflow routing.
- Case identity should be anchored at the physical specimen plus workflow
  level.
- Culture setup should reuse existing method/configuration concepts where
  appropriate.
- AST should reuse existing result/reporting infrastructure where feasible.
- WHONET should extend the existing export path where feasible.
- Critical communication should reuse existing alert surfaces while preserving
  a clinical call/read-back log.

See `specs/roadmaps/analyzer-microbiology-engineering-crosswalk.md` for the
current engineering interpretation and verification gates.
