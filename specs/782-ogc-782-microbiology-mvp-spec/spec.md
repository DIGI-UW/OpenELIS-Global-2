# Feature Specification: Microbiology MVP Workflow

**Feature Branch**: `spec/782-ogc-782-microbiology-mvp-spec`
**Created**: 2026-06-27
**Lifecycle**: Active
**Input**: User description: "Create a microbiology-specific feature spec that
distills the important product behavior from the OpenELIS Work specs and mocks;
reference the proper files; make the behavior crystal clear
for planning and implementation without letting product artifacts mandate
technical implementation details."

## Authority And References

The durable authority chain is:

1. Repository-owned feature and engineering specifications in this directory.
2. [`tasks.md`](./tasks.md), the single execution roadmap and only delivery
   status document for OGC-782.
3. OpenELIS Work for functional requirements, workflows, mocks, and visual
   intent only.

OpenELIS Work does not define implementation architecture or delivery status.
Its table names, service names, schemas, routes, and component suggestions are
non-binding. When its functional intent changes, this specification is
reconciled before implementation proceeds; repository files do not copy source
commits, checklist revisions, or other short-lived metadata.

- Public design bundle:
  [openelis-work/designs/microbiology](https://github.com/DIGI-UW/openelis-work/tree/main/designs/microbiology)
- M-00 parent:
  [m-00-micro-module-parent.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-00-micro-module-parent.md)
- M-01 reference data:
  [m-01-amr-reference-data.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-01-amr-reference-data.md),
  [Organism Master visual mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-01-organism-master.html)
- M-02 breakpoint catalog:
  [m-02-breakpoint-catalog.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-02-breakpoint-catalog.md),
  [Breakpoint Catalog visual mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-02-breakpoint-catalog.html)
- M-03 order entry hook:
  [m-03-order-entry-micro-hook.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-03-order-entry-micro-hook.md),
  [Step 1 visual mock](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-03-order-entry-step1.html)
- M-04 case workbench:
  [m-04-case-workbench-core.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-04-case-workbench-core.md),
  [interactive prototype](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-04-case-workbench-prototype.html)
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

## Interpretation Rules for Planning

- This spec defines product behavior and acceptance expectations.
- Detailed M-\* source files and visual mocks provide the source intent for
  actors, workflow order, information shown or captured, control meaning,
  requiredness, defaults, state transitions, and observable acceptance
  behavior. This specification records the behavior adopted by the product.
- Planning and tasks may choose table names, service boundaries, API shapes,
  routes, migrations, and reuse points, but those choices should not be treated
  as Casey-owned product requirements.
- Mockups do not prescribe schema, API, route names, or component structure.
  They guide visible workflow intent; any discrepancy is reconciled into this
  specification before implementation.

## MVP Scope Ruling

This feature keeps `782` as its traceability number. The first product boundary
is a complete routine-bacteriology path across M-03, M-04, M-05, M-07, and
M-11 outcomes; it is not the complete microbiology module. Delivery state and
the ordered follow-on slices live only in `tasks.md`.

The merge-blocking MVP is routine bacteriology order routing and order details,
case work, isolate identification, manual AST, worklist navigation, critical
communication, preliminary/final report propagation, and WHONET readiness.
Amendment/re-identification history, reagent/card lot linkage, expert rules,
authoritative WHONET interoperability, operational TB processing, antibiograms,
and GLASS reporting are explicit later work.

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
   **When** the user selects that test in the supported Add Order workflow,
   **Then** Program visibly changes to Microbiology and the Microbiology Program
   Details become available before save.
2. **Given** a test that is not configured for microbiology culture work,
   **When** the user places an order for that test, **Then** the microbiology
   order fields do not appear and no microbiology case is created.
3. **Given** one physical specimen has routine bacteriology work and a sibling
   workflow reserved for TB work, **When** the worklist is reviewed, **Then**
   the system distinguishes both workflow records without requiring duplicate
   accessioning. Completing the TB laboratory workflow is not part of this MVP.
4. **Given** the user has entered microbiology details, **When** the final
   culture test is removed or the order leaves the microbiology workflow,
   **Then** the system asks for confirmation before discarding those details.
5. **Given** a qualifying order has been saved, **When** the resulting case is
   opened, **Then** the selected culture setup and order details are present
   and repeated saves have not created a duplicate case.
6. **Given** a culture-capable test has a default protocol, **When** its order
   details appear, **Then** the protocol is derived and displayed read-only;
   no order-entry user can override it.
7. **Given** no default protocol resolves, **When** the user continues the
   order, **Then** the order and case are still created and the bench is told
   that a protocol remains to be set.
8. **Given** microbiology order details are visible, **When** the user reviews
   them, **Then** Date of Admission is optional and no critical-notification
   preference control appears.
9. **Given** a referring facility has departments or wards, **When** the user
   completes the standard Requester section, **Then** they can select the
   applicable department or ward and the selection remains with the order.
   This standard requester field is not an additional microbiology-specific
   field.
10. **Given** Date of Admission is present, **When** a specimen Collection Date
    would be earlier than admission, **Then** the user sees a correctable inline
    error and cannot save or continue until the dates are chronologically
    possible.
11. **Given** the deployment's configured date locale and a date whose day is
    greater than 12, **When** the user saves and reloads the order, **Then** the
    admission and collection dates remain unchanged.
12. **Given** a saved order is reopened, **When** the user reviews its sample
    collection, **Then** the recorded collection timing is visible and
    read-only until the user deliberately chooses Edit.

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
3. **Given** a culture is incubating, **When** a technologist records a positive
   signal or the connected instrument reports one, **Then** the case preserves
   that signal as distinct from confirmed growth and prompts subculture and Gram
   stain work.
4. **Given** a positive signal, **When** growth is confirmed through the bench
   workflow, **Then** the case records the progression to observed growth and
   makes both events visible in its history.
5. **Given** growth is present, **When** the technologist records Gram stain and
   colony morphology, **Then** the case preserves a preliminary isolate that can
   contribute to preliminary reporting without presenting AST as available.
6. **Given** the preliminary isolate is identified, **When** the technologist
   records the organism, identification method, confidence, and clinical
   significance, **Then** the case marks the isolate identified and supports AST
   setup for it.
7. **Given** the case has no protocol or the resolved protocol is unsuitable,
   **When** a technologist sets or changes it from the Inoculation section with
   a reason, **Then** the workflow classification and existing culture, isolate,
   and AST work remain unchanged and the history records who changed what and
   when.
8. **Given** the case is final, **When** a technologist tries to change its
   protocol, **Then** the action is blocked and the amendment path is required.
9. **Given** incubation is complete with no observed growth, **When** a
   technologist records no growth, **Then** the system audits the bench outcome,
   makes the case ready for review, and does not publish a patient result.
10. **Given** no growth has been recorded, **When** an authorized reviewer
    separately reviews and releases the final negative report, **Then** the
    patient result is published and the case is locked against further bench
    mutation.

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
   breakpoint context and allows the technologist to select the laboratory
   technique used.
2. **Given** a laboratory technique is selected, **When** the technologist
   records its readings, **Then** the system presents the appropriate MIC or
   zone input without asking the user to classify the same measurement again,
   displays susceptibility interpretations, and clearly marks readings that
   need manual judgment.
3. **Given** a user overrides an interpretation, **When** the override is saved,
   **Then** the original reading remains reviewable and the override requires a
   reason.
4. **Given** AST results have not been reviewed, **When** a supervisor attempts
   final release, **Then** the case is not considered ready for final reporting.
5. **Given** the ordered panel needs to change, **When** the technologist adjusts
   the panel or its antibiotic set with a reason, **Then** entry and reporting
   use that retained set and later reference-data edits do not change it.

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
3. **Given** a specimen has sibling bacteriology and TB workflow records,
   **When** the user views the worklist, **Then** both are distinguishable
   without merging their lifecycles or implying that TB bench processing is
   available.
4. **Given** the user needs to work culture cases or AST attempts, **When** the
   user switches the worklist view, **Then** the same shared page presents one
   row per culture case or one row per actionable isolate attempt with
   view-specific status counts and columns.
5. **Given** culture and AST work are in flight, **When** either worklist view
   is opened, **Then** each row identifies the laboratory accession and patient;
   culture rows also identify the specimen and most recent actor, while AST rows
   identify the isolate, organism, and selected panel.
6. **Given** an instrument has not returned an AST result or has returned a
   result needing review, **When** the worklist refreshes, **Then** the user can
   distinguish awaiting-results from results-in-review without importing or
   transcribing a result on the worklist.
7. **Given** a supervisor needs situational awareness, **When** the worklist is
   opened, **Then** resistance-hit context and recent microbiology activity are
   available on the same page without implying ownership of a case.
8. **Given** a resistance classification is reported by an analyzer or
   explicitly confirmed by a user, **When** it appears in the resistance-hit
   context, **Then** its provenance is distinguishable and the system does not
   infer a classification from free-text notes.
9. **Given** the worklist remains open during a shift, **When** thirty seconds
   elapse or the user refreshes it directly, **Then** current work appears
   without changing the bookmarked queue state, keyboard focus, or horizontal
   scroll position, and the page states how recently it refreshed.
10. **Given** a user is not permitted to view or work microbiology cases,
    **When** they navigate to the worklist or request its data, **Then** access
    is denied even when that user is otherwise authenticated.
11. **Given** a positive signal first appears during refresh, **When** the
    refreshed rows render, **Then** the new positive work is briefly
    distinguishable without relying on color alone or disrupting focus.
12. **Given** a culture is incubating, **When** the worklist renders its next
    action, **Then** the action reflects the elapsed culture day when timing
    inputs are available and falls back to an accurate stage label when they
    are not.
13. **Given** an AST attempt has been reviewed, **When** the user opens the AST
    worklist, **Then** it no longer appears as work needing action but remains
    available in a bookmarkable Reviewed view with a read-only View action;
    repeat or retest setup starts from the case and requires its clinical
    reason.

---

### User Story 5 - Release Reports and Log Critical Communications (Priority: P2)

As a technologist or supervisor, I need preliminary/final release readiness and
critical communication logging so that urgent findings are communicated and
reports are released only when required clinical work is complete.

**Why this priority**: Reporting and critical communication are patient-safety
requirements for microbiology.

**Independent Test**: Can be tested by logging a critical communication,
releasing a preliminary report from an eligible case, and confirming final
release is blocked until readiness requirements are met. The no-growth path is
tested by proving the bench outcome and final negative release are separate
actions.

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
4. **Given** a technologist records no growth after incubation, **When** the
   case becomes ready for review, **Then** no patient result has been released.
5. **Given** that no-growth outcome is ready for review, **When** an authorized
   reviewer releases the final negative report, **Then** the report is visible
   in the patient result path and the finalized case is locked.

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
- **FR-002**: In the supported Add Order workflow, the system MUST derive and
  display Culture Protocol read-only from the ordered test. A missing default
  MUST NOT block order or case creation. Users MUST be able to select Patient
  Origin from the deployment's configured choices, optionally record Date of
  Admission, enter a bounded/defaulted Number of Sets, record multi-line
  Clinical History, and mark Antibiotic Exposure. No critical-notification
  preference appears in order entry; notification policy remains in the
  existing catalog and critical-result workflow. Date of Admission MUST remain
  visible but disabled for Outpatient, MUST round-trip without deriving a
  stored surveillance classification, and MUST remain optional. Patient Origin
  SHOULD default from the requesting location when that mapping is available.
  Clinical History MUST offer managed clinical macros when the separately owned
  Macro Library is enabled. The system MUST confirm before discarding entered
  details when culture routing is removed.
- **FR-002A**: A technologist MUST be able to set a missing protocol or change
  the current protocol inline from the case's Inoculation section. The action
  MUST require a reason, retain previous and new values with authenticated actor
  and time, preserve existing inoculations, isolates, AST work, and workflow
  classification, and be blocked after final release.
- **FR-003**: The system MUST distinguish sibling bacteriology and TB workflow
  records for the same physical specimen without duplicate accessioning. This
  MVP does not provide the operational TB laboratory workflow.
- **FR-004**: Users MUST be able to open a microbiology case and understand the
  current workflow, specimen context, stage, next action, and prior activity.
- **FR-005**: Users MUST be able to record culture setup, incubation progress,
  positive or growth observations, a no-growth bench outcome, and specimen-loss
  or rejection events. Recording no growth MUST be audited, MUST make the case
  ready for review, and MUST NOT publish a patient result. An authorized
  reviewer MUST separately release the final negative report; that release
  publishes the result and locks the case.
- **FR-006**: Users MUST be able to record preliminary isolate work-up from Gram
  stain and colony morphology, then identify the organism with method,
  confidence, and clinical significance while clearly distinguishing pending
  from identified state.
- **FR-007**: AST MUST remain unavailable until the isolate has a confirmed
  organism identification sufficient to support interpretation.
- **FR-008**: Users MUST be able to enter manual AST readings and see
  susceptibility interpretation, including clear guidance when no standard
  breakpoint is available.
- **FR-010**: Users MUST be able to override AST interpretations with a reason
  while preserving the original reading for audit and review.
- **FR-011**: Users MUST be able to repeat or retest AST without overwriting the
  prior run or hiding the reason for repeat testing.
- **FR-012**: The system MUST provide a shared microbiology worklist organized
  by case state, due action, urgency, and review need rather than per-case
  ownership.
- **FR-013**: The worklist MUST be discoverable from configured primary
  navigation and allow users to filter, sort, and open cases or AST work that
  needs action.
- **FR-014**: The system MUST make sibling workflow records on the same specimen
  distinguishable in the worklist while keeping their lifecycles and reports
  separate.
- **FR-014A**: The worklist MUST remain one shared page with separate Culture
  and AST views, view-specific status counts and columns, and navigation from
  an AST row to the exact isolate attempt that needs work.
- **FR-014B**: Analyzer-driven results MUST appear automatically as awaiting or
  review-ready work. The worklist MUST NOT require a manual import action, and
  unmatched instrument messages MUST direct users to the existing
  reconciliation experience.
- **FR-014C**: The worklist MUST include resistance-hit context and recent
  microbiology activity, while visibly identifying future controls that are
  unavailable in the current phase. Resistance classifications MUST retain
  their analyzer-reported or user-confirmed provenance and MUST NOT be inferred
  from free-text notes.
- **FR-014D**: Reviewed AST attempts MUST leave the default action queue and
  remain available through a bookmarkable Reviewed view with read-only case
  navigation. Repeat or retest setup MUST remain a case-scoped action that
  requires a reason.
- **FR-015**: Users MUST be able to release preliminary reports when reportable
  culture observations or isolate work-up are available, including Gram stain
  and colony morphology before organism identification, and final reports only
  when readiness checks pass. A recorded no-growth outcome is reportable only
  through the separate authorized final-negative review and release action.
- **FR-016**: Users MUST be able to report a nonconformance from the case with
  the specimen context already identified, choose whether to flag or reject
  affected work, and use a dedicated Mark Lost action that records the event,
  rejects affected open tests, and makes the terminal lost state visible.
- **FR-017**: Users MUST be able to log critical communications for case,
  isolate, sample, or result context, including recipient, message, time,
  method, acknowledgment state, and follow-up.
- **FR-018**: Critical communications MUST surface through the existing
  operational alerts workflow rather than a parallel alerts experience, and an
  acknowledgment from either workflow MUST remain synchronized.
- **FR-019**: The system MUST support reference data needed for MVP
  microbiology work: organisms, antibiotics, AST panels, culture setup recipes,
  breakpoint standards, patient origin, and specimen mapping. Phase 1A users
  with reference-data access MUST be able to view the six configured Patient
  Origin choices and their WHONET codes; adding or changing those choices is a
  later deployment need.
- **FR-020**: The system MUST prepare finalized microbiology data for WHONET
  surveillance export by tracking export-relevant organism, specimen,
  antibiotic, breakpoint, and interpretation information.
- **FR-021**: Phase 1A MUST support manual bacteriology workflow end-to-end;
  analyzer automation, expert rules, authoritative WHONET interoperability, TB
  workflow, and GLASS reporting may be planned extensions unless included in an
  explicit later slice.
- **FR-022**: Product specs and product tickets for this feature MUST describe
  workflow behavior and acceptance outcomes, not required table names, service
  names, route names, or storage layout.
- **FR-023**: Worklist and case destinations MUST preserve bookmarkable,
  refresh-stable filter, sort, and active-section context, including when a
  user opens a case and returns to the worklist.
- **FR-024**: Completion claims MUST trace each required OpenELIS Work behavior
  to an implementation task, production path, focused automated evidence, and
  human UAT. Direct navigation to a legacy route or label-only checks do not
  prove the supported user workflow.
- **FR-025**: The shared worklist MUST refresh every thirty seconds and on user
  request without changing its canonical URL, focus, scroll position, or
  current row context.
- **FR-026**: Only users permitted to view or work microbiology cases may open
  the worklist or read its data.
- **FR-027**: Every Microbiology worklist, case, order-detail, AST, and reference
  action MUST be operable without a mouse, expose its name and status to
  assistive technology, preserve a visible logical focus position, and never
  use color as the only carrier of meaning.
- **FR-028**: Worklist and case interactions MUST remain responsive at a
  small-to-medium clinical laboratory's realistic daily volume so that loading,
  filtering, and moving between bench actions do not interrupt routine work.
- **FR-029**: During intermittent connectivity, users MUST be able to read the
  last successfully loaded worklist with a clear offline state. Bench edits
  made after connectivity is lost MUST not be silently discarded, and a user
  MUST be able to resolve a conflict explicitly after reconnection.
- **FR-030**: Automated accessibility and performance evidence MUST identify
  the environment, realistic data volume, and measurement boundary used.
  Automated scans do not replace deployed review or human acceptance.
- **FR-031**: Only users authorized for microbiology bench work may view or
  change case data. Final release and amendment actions require a
  supervisor-capable user, and the identity recorded for any action MUST come
  from the signed-in session rather than submitted form data.

### Scope Boundaries

These outcomes remain valid module goals but are delivered through later
roadmap slices rather than being implied by the routine-bacteriology boundary:

- Versioned amendment and re-identification history.
- Repeat/retest AST metadata and complete reagent/card-lot traceability.
- Reference-data and mapping administration.
- Complete WHONET packaging, scheduling, and delivery.
- Analyzer-result review and QC workflow.
- Expert rules, operational TB, antibiograms, and GLASS reporting.
- Shared Macro Library integration; Macro authoring and administration remain a
  separate product capability.
- Accessibility, representative-volume performance, and shared offline/conflict
  behavior qualification.

The scope and delivery state of each item are maintained only in `tasks.md`.

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
  MUST be split into independently verifiable behavior slices. Delivery and
  review structure is defined by the engineering plan, not this product
  contract.

### Key Entities

These are product concepts. Engineering may choose the final storage and API
shape during planning.

- **Microbiology Case**: One microbiology workflow for one physical specimen,
  from receipt through final report. Amendment history is later scope.
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
- **Nonconformance**: A reported departure from expected specimen or laboratory
  handling, including its classification, severity, description, disposition,
  and visible effect on affected work.
- **Microbiology Worklist**: The shared operational queue of microbiology cases
  and AST work requiring attention.
- **Surveillance Export Readiness**: The finalized result and mapping state
  needed to produce surveillance output for WHONET import validation.

## Authoritative Alignment Stories

These stories close behavior omitted when the earlier roadmap treated the
first vertical slice as module completion.

### User Story 8 - Classify And Work Every Culture Case

As a bench technologist, I can identify cases that still need a workflow,
choose the correct bacteriology or TB workflow with a reason, and continue from
the appropriate current step without losing prior work.

**Acceptance Scenarios**:

1. A manually routed case with no safe deployment default is visibly marked as
   needing workflow classification in both the shared queue and case header.
2. Changing workflow is inline, requires a reason and compatible culture
   method, preserves prior history, and is blocked after final release.
3. Cases sharing one specimen link to each other while retaining independent
   stages, results, critical communication, and reports, except when a ruled
   physical-specimen disposition must apply to every affected sibling.
4. The case presents setup, subculture lineage, notes, isolate work, AST,
   critical communication, reporting, and amendments in the order implied by
   the current stage.
5. A user can report a nonconformance from the case without re-entering specimen
   identity and can choose flag-only or reject-affected-work disposition.
6. Marking a physical specimen lost records the nonconformance, rejects affected
   open work, and leaves a visible terminal event in each affected case history.

### User Story 9 - Review Complete And Traceable AST Work

As a microbiology technologist or supervisor, I can review manual or
instrument-provided susceptibility work with enough provenance to accept,
override, revert, repeat, or block it safely.

**Acceptance Scenarios**:

1. AST setup shows the ordered panel and its provenance, selected breakpoint
   standard/version, laboratory technique, technique-appropriate measurement,
   and reagent lot.
2. The run shows the exact ordered antibiotic set; entry cannot silently add a
   drug outside that set, and a justified adjustment changes the set used for
   entry and reporting.
3. Each reading shows the matched interpretation basis and preserves visible
   original-to-override history with a justified supervisor revert.
4. Instrument results remain pending review until accepted; mismatches,
   missing breakpoints, and QC failures are visible blockers with named next
   actions.
5. Repeating all or part of a run creates a new attempt and leaves the prior
   attempt available for review.

### User Story 10 - Work One Shared Culture And AST Queue

As a rotating bench team, we can use one shared worklist to switch between
culture cases and AST runs, understand what is due, and open the exact work
item without relying on ownership or paper logs.

**Acceptance Scenarios**:

1. Culture and AST views are distinct views of one worklist and preserve their
   selected view, filters, search, sort, and page state when bookmarked.
2. Summary actions, due-action text, linked-workflow markers, and row actions
   are deterministic for the selected view.
3. New instrument results are identified as awaiting review; no manual import
   action appears.
4. Empty, refresh, keyboard, and compact-screen behavior preserve the user’s
   position and focus.
5. The page refreshes every thirty seconds, reports its freshness, briefly
   distinguishes newly positive work, and keeps the current canonical URL.
6. Clicking a culture or AST row opens the exact case context; the same actions
   remain available through the row menu.
7. Incubating cultures show the correct day-aware next action when timing is
   known and an accurate stage fallback when it is not.

### User Story 11 - Select Safe Reagent Lots Consistently

As a bench technologist, I use the same lot-selection behavior during culture
setup and AST setup, see the oldest-expiring eligible lots first, and cannot
save a selected lot that is expired, QC-blocked, depleted, or otherwise no
longer eligible.

**Acceptance Scenarios**:

1. Eligible lots show expiration, QC state, quantity, and clear oldest-expiry
   guidance in both consuming workflows.
2. When the catalog supplies required, optional, and substitute selection
   policies, each policy enforces its distinct save rule. Until that catalog
   contract exists, the system does not infer these policies from unrelated
   reagent roles and this scenario remains blocked by an explicit dependency.
3. A lot that becomes invalid after selection produces a specific message
   naming the reagent, lot, and corrective action.

### User Story 12 - Work Reliably With Keyboard, Assistive Technology, And Intermittent Connectivity

As a bench user, I can complete microbiology work with the input and assistive
technology available to me, at normal laboratory volume, without losing my
place or silently losing work when connectivity is interrupted.

**Acceptance Scenarios**:

1. A keyboard-only user can open the worklist, choose a summary view, open a
   case, expand its current section, add and identify an isolate, enter and
   review AST, and release a preliminary report without a mouse.
2. Expanding an inline section moves focus into that section and announces the
   change; closing an inline section or dialog restores focus to the initiating
   control.
3. Status, provenance, and urgency remain understandable without color, all
   controls and tables expose meaningful names, and focus is always visible.
4. With 200 active items in either worklist view, initial display completes in
   under two seconds and filtering completes in under 300 milliseconds. A case
   with five isolates and up to 100 AST readings displays in under one second,
   defaults to its newest 30 Timeline events, and retains complete history on
   demand.
5. If connectivity is lost after a worklist has loaded, the last loaded work
   remains readable and is clearly marked offline. A bench edit made while
   disconnected is not silently lost, and a conflicting change requires an
   explicit user resolution after reconnection.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: From the supported Add Order workflow, a user can select a
  qualifying routine-bacteriology test, see Program become Microbiology,
  confirm the complete culture details, save, and find one resulting case on
  the worklist without manual routing or duplicate creation.
- **SC-002**: A technologist can complete the MVP bacteriology path from order
  routing through culture setup, isolate identification, manual AST entry,
  review readiness, and report release using one case workflow.
- **SC-003**: A case with incomplete required work cannot be final-released, and
  the user can identify the blocking item without consulting paper logs.
- **SC-004**: A user can distinguish sibling bacteriology and TB workflow
  records for the same specimen in worklist context without implying that the
  TB bench workflow is implemented.
- **SC-005**: AST override history preserves the original reading, the changed
  interpretation, actor, time, and justification.
- **SC-006**: A user can log a critical communication without being blocked by
  incomplete provider directory data.
- **SC-007**: Worklist users can identify urgent positive, growth, and
  AST-review work through visible priority, due-action, filter, and sort
  controls.
- **SC-008**: All MVP user-facing microbiology strings are represented by i18n
  keys and can be rendered in English.
- **SC-009**: Engineering planning can produce implementation tasks without
  needing Casey artifacts to decide schema, API, route, or service ownership.
- **SC-010**: A user can reach the worklist from primary navigation, bookmark a
  filtered view, open a case, refresh either page, and return without losing
  the relevant worklist or case-section context.
- **SC-011**: A keyboard-only user can complete the order-to-preliminary-report
  path with visible focus and announced inline-section changes.
- **SC-012**: Performance measurements record the environment, data volume,
  server time, browser-ready time, and interaction time so reviewers can
  distinguish a user-visible regression from harness, runtime, or hardware
  effects.
- **SC-013**: A deployed connectivity-loss exercise proves the last-loaded
  worklist remains readable, offline state is visible, an interrupted bench
  edit survives reconnection, and a conflicting edit requires an explicit
  resolution.
- **SC-014**: An authenticated user without a microbiology bench role receives
  a forbidden response; a results-entry user cannot release a final report;
  and a submitted alternate actor identifier cannot change audit attribution.
- **SC-015**: Recording no growth creates no patient result. A separate
  authorized final-negative release publishes the result and leaves the case
  locked.

OpenELIS Work's M-NFR document includes numeric timing and fixture
prescriptions. Those numbers are engineering qualification inputs, not product
acceptance requirements: "render," hardware, runtime mode, network, and
representative deployment conditions must be defined before a threshold can be
deterministic.

## Engineering Boundary

Implementation architecture and technical decisions belong in
[`plan.md`](./plan.md). Current execution status belongs only in
[`tasks.md`](./tasks.md).
