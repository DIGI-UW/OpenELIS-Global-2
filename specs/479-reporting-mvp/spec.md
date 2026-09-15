# Feature Specification: Configurable Reporting MVP

**Feature Branch**: `spec/479-ogc-479-reporting-mvp`  
**Created**: 2026-09-13  
**Status**: Agreed MVP scope; implementation in progress

**Phase checkpoint**: Specification and clarification check-ins completed. The
accepted direction is recorded below and reconciled with the plan and tasks.  
**Input**: Deliver the smallest useful configurable CSV reporting workflow
inside OpenELIS, using the existing mock and product specification and an
implementation-focused SpecKit approach.

The outcome is a laboratory user choosing or reopening a report, selecting
instance-appropriate columns and a period, and getting a useful CSV from real
OpenELIS records. Completed output is available directly from the reporting flow
and remains accessible through the report queue. Generative AI is a development
tool; using this feature does not require AI.

The MVP must be useful for actual reporting with little setup. Demonstrate
finding the instance's data, selecting and arranging columns, filtering and
downloading. Reuse existing access controls as background application behavior;
a new permission-administration workflow is not the centerpiece or prerequisite
for the functional demonstration.

## Scope and Source Relationship

The product source is the
[Custom Data Export specification](https://github.com/DIGI-UW/openelis-work/blob/5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f/designs/reports/custom-data-export.md)
and
[interactive mock](https://github.com/DIGI-UW/openelis-work/blob/5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f/designs/reports/custom-data-export.html).
The mock is the interface source of truth. This MVP specification scopes which
functions are connected to real OpenELIS data first; it does not authorize a
simplified or replacement interface. Port the supplied layout and interaction
code directly where practical. Preserve its polished overview, staged builder,
catalog, ordering, review, saved reports, queue and responsive presentation.
Controls beyond the connected stage can remain visible with explicit pending
behavior; they must not pretend to generate, save or recover real data.

User decisions in this conversation govern intentional adaptations: both CSV
layouts with spreadsheet default, every repeated result, per-test turnaround,
instance-aware catalogs, shared definitions and reuse of existing access. These
do not authorize replacing Add/Added controls with checkboxes, expanding the
whole catalog, preselecting every test or flattening the design's flow.
Use [design-parity.md](design-parity.md) for the mandatory direct comparison.
Engineering choices are recorded separately in [plan.md](plan.md).

| Product capability        | First release                                                                                                                                      | Follow-on                                                                                   |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Native builder            | One configurable builder for the mock’s Sample & Testing, Referrals and Non-Conformance definitions                                                | New source kinds outside the mock                                                           |
| Columns                   | Common reporting attributes plus fields supplied by the instance’s supported configuration; no fixed count                                         | Additional source types requiring a new export mapping                                      |
| Row meaning               | Spreadsheet layout by default, with configured tests as columns and additional rows for repeated tests; detailed result-list layout also available | Additional presentation formats                                                             |
| Filters                   | Date range using the selected definition’s date meaning; instance-populated filters and useful defaults                                            | Additional source-specific filters outside supported mappings                               |
| Delivery                  | Create CSV and download from the current flow when ready; queue for return visits and longer work                                                  | Separate estimation and synchronous-generation optimizations                                |
| Recovery                  | Re-download, retry failure, cancel queued work, restart recovery, expiry and rerun                                                                 | Queue preferences, estimated wait, cross-page completion notifications, manual job deletion |
| Reuse                     | Shared named report definitions: save layout/columns/filters and rerun with fresh dates; retain the current draft                                  | Personal libraries, sharing administration and schedules                                    |
| Administration and naming | Existing deployment configuration; generated download name                                                                                         | Export settings controls and custom file names                                              |

Both layouts are required for Sample & Testing: a spreadsheet with sample
information and configured test columns, and a detailed list with one row per
individual result. The spreadsheet is the default. Repeated tests use additional
rows; the spreadsheet must not silently select only the latest result. Report
types are configurations of one feature, not three independently built
interfaces or export systems. Preserve the mock's three report types in the MVP,
prove the common capability with Sample & Testing first, and add the other
source definitions through the same engine. Flat referral/event definitions use
the common tabular projection without an artificial test pivot. This sequencing
is an engineering plan, not a field-count reduction.

The catalog combines common OpenELIS reporting attributes with fields from the
instance's supported configuration. Common attributes include accession, dates,
patient information, test/result details and turnaround measures. Configured
items include local tests, result components and additional patient/sample
questions where their source can be mapped to the chosen reporting workflow. The
available count and labels therefore vary by instance. No fixed limit of seven,
38 or 50 defines the product.

The seven example columns remain one useful demonstration, not the catalog or an
acceptance ceiling. New items within a supported configuration type must become
available without changing reporting code. A wholly new data source may need a
new mapping; the builder must not claim it can export arbitrary database columns
merely because they exist. Useful supported fields are not excluded solely to
avoid dealing with patient information.

Personal report libraries, sharing administration, QC exports, Catalyst
integration, dashboards, SSO changes and replacement of Routine CSV/Jasper
reports are outside this first release. Builder work relates to
[OGC-479](https://uwdigi.atlassian.net/browse/OGC-479); the bounded queue subset
relates to [OGC-481](https://uwdigi.atlassian.net/browse/OGC-481). Reusable
definitions relate to [OGC-483](https://uwdigi.atlassian.net/browse/OGC-483),
with instance-wide sharing replacing its personal-library assumption. The mock’s
report types share one configurable implementation. This specification does not
change ticket assignments.

## Clarifications

### Session 2026-09-13

- Q: Should MVP field coverage be a fixed subset of seven, 38 or 50 fields? → A:
  No. Prioritize functional usefulness and little friction; field availability
  should reflect the instance. Reuse existing data-access controls rather than
  making new security workflows the focus of the MVP.

- Q: How should configured tests appear in the CSV? → A: Both layouts, with the
  spreadsheet layout as the default for routine reporting.
- Q: How should the spreadsheet handle repeated finalized results for a test? →
  A: Preserve every result, using additional rows for repeated tests.
- Q: How should turnaround appear when a spreadsheet contains several tests? →
  A: Show turnaround beside each test. Repeated results keep their own durations;
  collection-to-receipt remains a common specimen field.

- Q: Should the MVP include personal named reports? → A: Include reusable
  definitions without making per-user scoping a prerequisite; use shared
  definitions within the instance for the MVP.

- Q: Do the mock's report types require independent efforts? → A: Generalize
  reporting through configuration; do not treat the types as separate builds.

- Q: What is the delivery goal? → A: A complete MVP deployed to a public
  OpenELIS UAT target with the established review overlay and executable UAT for
  the critical reporting workflows. A local implementation or draft PR is only
  an intermediate checkpoint. Automated deployment checks and human UAT results
  remain separate evidence.
- Q: Should public UAT wait until the MVP is finished? → A: No. It must reflect
  each usable development stage and help find issues as work proceeds.
  Automated end-to-end validation and human UAT use the same workflows,
  fixtures and expected results; they are not separate late-stage scopes.

The mock includes Sample & Testing, Referrals and Non-Conformance; its complete
fictional worked example is Sample & Testing. The implementation interpretation
is one engine with source definitions for those types, using Sample & Testing as
its first proof. The field-count question is superseded by instance-aware
coverage. The unrelated query-panel UI request was withdrawn and is outside
scope.

## User Scenarios & Testing

### User Story 1 — Export Selected Reporting Information (Priority: P1)

As a laboratory user, I can choose the columns and period I need and download
the matching results without asking a developer to create a report.

**Why this priority**: This is the first complete, useful reporting journey.

**Independent test**: Seed known results in two lab sections, complete the
builder, and compare the downloaded CSV with an independently defined expected
record set and column sequence.

**Acceptance scenarios**:

1. **Given** export access, **when** I open Custom Data Export from Reports,
   **then** I can select supported columns, reorder them, set filters and review
   their labels and order before submitting.
2. **Given** configured tests and repeated results for a sample, **when** I
   export the default spreadsheet, **then** the selected tests appear as columns
   and repeated tests create additional rows without losing any eligible result.
3. **Given** the same request, **when** I switch to the detailed result list,
   **then** each eligible individual result appears once; both layouts preserve
   the same result identities and values even when their row counts differ.
4. **Given** finalized and unfinished results, **when** I export using the
   default finalized filter, **then** only finalized results appear, including
   corrected results that are finalized; their current stored values and
   correction status are represented accurately.
5. **Given** records on both date boundaries and outside them, **when** I apply
   the period, **then** both whole boundary dates are included in the laboratory
   timezone using the selected report’s stated date meaning.
6. **Given** a successful submission, **when** the export completes, **then** I
   can download it without navigating away and find it later in my queue; no
   matching records produces a valid header-only CSV and an explicit zero-row
   result.
7. **Given** tests or repeated results with different turnaround times, **when**
   I select turnaround columns in the spreadsheet, **then** each duration
   appears in a column for its own test beside the chosen result column and
   remains with that result's row. Missing timestamps leave empty duration
   cells without losing results. Collection-to-receipt remains specimen-level.

### User Story 2 — Refine a Report Without Repeated Setup (Priority: P1)

As a laboratory user, I can start with useful defaults, find the instance’s
fields and tests, and revise my report without losing choices or repeating
setup.

**Why this priority**: Useful defaults and retained choices make self-service
reporting practical for routine work.

**Independent test**: Use two fixture configurations with different tests and
additional fields, build and revise reports in each, and verify that normal
report users can complete the flow with their existing access.

**Acceptance scenarios**:

1. **Given** an edited draft, **when** I move backward or visit the queue and
   return in the same signed-in browser session, **then** columns, order, dates,
   filters and current step remain available.
2. **Given** missing, reversed or excessive dates, **when** I try to continue,
   **then** the relevant fields explain the error and submission is unavailable.
3. **Given** lost export or lab-section access, **when** I submit, generate or
   download, **then** access is denied without silently reducing the request or
   releasing data; my draft remains available for review.
4. **Given** another user's job identifier, **when** I try to inspect, download,
   retry or cancel it, **then** it is not disclosed or modified.

### User Story 3 — Reuse a Shared Report (Priority: P1)

As a laboratory user, I can save a useful report definition for the instance and
rerun it for a new period without reconstructing its layout, columns and
filters.

**Why this priority**: Routine reporting should require little repeated setup.

**Independent test**: Save a definition, reopen it as another existing report
user, choose fresh dates and compare the export with the stored layout and
filters.

**Acceptance scenarios**:

1. **Given** a configured report, **when** I save it with a name, **then** it is
   available in the instance's shared report list with its layout, ordered
   fields and non-date filters.
2. **Given** a shared definition, **when** another report user opens it,
   **then** the definition is reusable without a sharing invitation or
   personal-library setup, and running it uses that user's existing access.
3. **Given** a saved report, **when** I rerun it, **then** I choose a fresh
   period; changing the definition does not change any previously submitted job
   or file.
4. **Given** an edited definition, **when** I save changes, **then** I can
   update it with explicit overwrite confirmation or save a separate copy;
   concurrent changes are surfaced rather than silently overwritten.

### User Story 4 — Recover a Submitted Export (Priority: P2)

As a laboratory user, I can leave the page, return to my export, and recover
from a failure without reconstructing my choices.

**Why this priority**: This makes the first export usable during routine work.
P2 orders delivery; this story is still required for the complete MVP.

**Independent test**: Use persisted jobs in each lifecycle state to verify
return visits, retry, cancellation, application restart and expiry.

**Acceptance scenarios**:

1. **Given** a queued or generating job, **when** I leave and return, **then**
   its state remains available and the queue updates while work is active.
2. **Given** a failed job, **when** I retry, **then** a new linked job uses the
   original column order, dates and resolved scope; the original remains
   unchanged.
3. **Given** an application restart, **when** processing resumes, **then**
   queued work is recovered and interrupted work becomes visibly failed and
   retryable; no incomplete output is offered for download.
4. **Given** a ready file within retention, **when** I download it again,
   **then** I receive the same file. After expiry I can restore its choices, but
   must enter a new reporting period before rerunning it.
5. **Given** queued work, **when** I confirm cancellation, **then** it will not
   run. If processing has already started, cancellation is refused clearly.

### Edge Cases

- For Sample & Testing, a specimen's collection date differs from its
  order-entry date or another specimen under the same accession; filter by the
  result's own specimen date.
- Missing specimen collection date cannot match a bounded collection-date query;
  other sources use their own declared date anchor, without a silent fallback.
- Null cells, non-ASCII names, quotes, commas and newlines remain valid CSV.
- Numeric, dictionary, text and multi-valued result representations must be
  checked against real OpenELIS fixtures; rendered HTML is not a CSV value.
- A correction still awaiting finalization is not included as a finalized
  result.
- A lab section granted after submission does not widen an existing job; a
  revoked section prevents execution/download of that requested scope.
- A repeated submission after a lost response must not create duplicate work.
- Concurrent submissions must not bypass the active-job limit; cancel/start and
  expiry/download races must not expose incomplete or expired files.
- Logging out or changing user clears the previous user's browser draft and
  queue.

## Requirements

### Functional Requirements

- **FR-001**: Provide Custom Data Export and My Report Queue within the existing
  Reports experience for users with export access. If a separate base export
  capability is required, existing roles authorized for Reports receive it
  during migration; this does not grant additional lab-section or
  identifying-data access.
- **FR-002**: Offer the instance's supported reporting fields in meaningful
  searchable groups, with a separate ordered selection, add/remove actions and
  an ordered header preview. Allow any nonempty compatible selection. Do not
  impose a fixed catalog count or show unsupported fields as working options.
  Preserve the mock's initially collapsed groups, field-and-group search,
  automatic expansion of matches, restoration of browsing folds after clearing
  search, Expand all/Collapse all, group counts and Add/Added actions. Catalog
  availability does not imply default selection of every configured test.
- **FR-003**: Support direct reordering and accessible keyboard/button ordering,
  with named controls, retained focus and position feedback. The displayed order
  must remain authoritative through review, submission, download and
  restoration.
  Preserve the mock's visible drag grip, insertion feedback, compact stacked
  ordering controls, separate remove action and narrow-screen pane switching.
- **FR-004**: Require Date From and Date To; include whole boundary dates in the
  laboratory timezone. Reject missing/reversed periods and periods exceeding the
  configured maximum, initially 90 inclusive calendar days.
- **FR-005**: Filter by the result's specimen collection date and currently
  permitted lab sections for Sample & Testing; populate optional tests and
  status choices from the instance. Default to finalized results, allow
  supported status choices and show the effective filter. Other definitions use
  their declared referral/event date and applicable filters. An empty test
  selection means all eligible tests within the requested scope.
- **FR-006**: For Sample & Testing, support a default spreadsheet with
  configured tests as columns and a detailed list with one row per individual
  result. Preserve the same eligible results in both layouts. Repeated tests
  create additional spreadsheet rows; do not select only the latest value,
  collapse duplicate-looking results, or create combinations of unrelated
  repeats. Make layout and row meaning clear.
- **FR-007**: Show the reporting period, named filters, row meaning and ordered
  columns on review. Use defaults so a routine export can be completed without
  configuring access rules or reconstructing metadata. Generation/queue details
  must not dominate the reporting workflow.
- **FR-008**: Retain the draft during builder navigation, a queue visit and
  failed requests within the current signed-in browser session. Clear it on
  sign-out or user change. Cross-device and post-session draft recovery are
  outside scope.
- **FR-009**: Reuse the existing user's reporting, patient-data and lab-section
  access rules on the server. Keep ownership and current-access checks for jobs
  and files, without adding per-export approval steps or a separate permission
  setup journey. Explain genuine access failures and preserve the draft; never
  silently change requested fields or scope.
- **FR-010**: Persist submitted parameters and their order immutably with their
  owner. A repeated delivery of the same submission must identify the same job;
  edits to the builder must not change an existing job.
- **FR-011**: Show only the current user's jobs, newest first, with submitted
  time, reporting period, readable status, completed row count and available
  action. Show QUEUED, GENERATING, READY, FAILED, EXPIRED and CANCELLED states
  accurately. Update active jobs without a page reload; stop automatic polling
  when idle.
- **FR-012**: Generate CSV with UTF-8 BOM, comma separation and double-quote
  escaping, ISO dates and empty cells for nulls. Use stable canonical headers
  for common fields and the instance's configured labels for configured fields;
  capture those headers with the job. Header and cell order must match the
  selection. A READY job offers only its complete stored file.
- **FR-013**: Allow download and re-download only by the owner while the file is
  ready, unexpired and currently authorized. Job operations must not disclose
  another user's resource. Generation errors must not expose clinical content.
- **FR-014**: Retry a failed job as a new linked job with the same immutable
  request and resolved lab scope, subject to current authorization and limits.
  Retry reads current data; it does not promise a historical or byte-identical
  recreation of a failed run.
- **FR-015**: Recover queued work after restart and make interrupted generation
  visibly failed and retryable. Bound execution and memory use so a large export
  does not consume all worker capacity or buffer the entire dataset in memory.
- **FR-016**: Expire completed files after the configured retention, initially
  seven days from completion, remove their output and retain job/audit history.
  Rerun restores columns and non-date filters but requires fresh dates.
- **FR-017**: Enforce the configured per-user active-job limit, initially five
  queued/generating jobs, including concurrent submissions. Explain rejection
  without losing the draft. Allow confirmed cancellation only while queued.
- **FR-018**: Audit submission, execution outcome, download, retry, cancellation
  and expiry with actor/job/time as appropriate; record access denials without
  recording result contents. Queue visibility is not a substitute for an audit.

- **FR-019**: Build catalog choices from common supported attributes and current
  instance configuration. Adding or renaming a configured item within a
  supported field type updates the catalog without a reporting-code change. Use
  stable identities so matching names do not merge distinct items and renames do
  not silently retarget an existing job. Explain stale selections when reopening
  a draft; retained completed files remain unchanged.
- **FR-020**: Prioritize a low-friction functional journey: useful initial
  column choices, instance-populated filters, retained edits and a clear
  download action. A user who already has the necessary OpenELIS access must not
  complete an additional access-configuration or approval workflow to
  demonstrate reporting.

- **FR-021**: Support shared named report definitions within the instance. Save
  the chosen layout, ordered stable field identities and non-date filters; allow
  report users to find, reopen, update with confirmation, save a copy and remove
  definitions with confirmation. Require a fresh reporting period for each run.
  Reuse existing Reports access without personal-library or invitation
  machinery. Definitions contain configuration, not generated result data; jobs
  and files remain bound to the user who runs them. Preserve creator/change
  metadata for traceability and detect concurrent edits.

- **FR-022**: Provide one configurable reporting experience for the mock's
  Sample & Testing, Referrals and Non-Conformance definitions. A definition
  supplies labels, supported fields/filters, date meaning, row meaning and
  layout options; common builder, saved-report, execution and download behavior
  is reused. Adding a definition over an already supported source must not
  require another report-specific frontend or queue. New source kinds can
  require a data mapping.

- **FR-023**: Deploy the complete MVP at
  `reporting.catalyst.openelis-global.org` from an exact reviewed revision and
  expose verified deployment identity. Integrate the established OpenELIS UAT
  review overlay with a `reporting` checklist in Grist. The checklist must give
  reviewers executable, stable-fixture workflows for both Sample & Testing
  layouts, shared report reuse, Referrals, Non-Conformance and user-visible
  queue/recovery actions. Validate the live checklist, overlay, routes,
  downloaded files and review submission path. Checklist availability is UAT
  readiness; only submitted reviewer results establish human acceptance.

### Constitution Compliance Requirements

- **CR-001**: Follow the current repository constitution and existing platform:
  Java 21, traditional Spring MVC, the five-layer pattern, service transactions
  and JPA/Hibernate queries; use Liquibase for new persistence.
- **CR-002**: Use Carbon and accessible controls. Externalize interface strings
  through React Intl and add translation source keys to `en.json` only.
- **CR-003**: Use configuration for deployment-specific limits and existing
  authentication, role and lab-section access conventions. This CSV workflow
  adds no external system integration or alternative authentication scheme.
- **CR-004**: Add meaningful unit, ORM, database integration and Playwright
  coverage; deliver one reviewable PR per validation milestone.
- **CR-005**: Build on the target application architecture. Do not extend the
  legacy Routine CSV implementation; track any legacy path actually superseded
  by this implementation with a removal plan.

### Key Entities

- **Report source definition**: Instance-enabled reporting type, field/filter
  definitions, date/row meaning and supported layouts using a known data source.
- **Reporting catalog**: Common attributes and instance-configured field
  definitions, each with a stable identity, display label and supported source.
- **Export draft**: The user's current columns, filters and step; separate from
  a saved report definition.
- **Saved report definition**: Named instance-wide layout, columns and non-date
  filters; changes do not mutate past jobs.
- **Export job**: An owner-bound immutable request with a processing lifecycle.
- **Export file**: Completed CSV belonging to a job and subject to
  retention/access.
- **Export source record**: A projection of one eligible result, referral or
  event with its stable identity and related reporting values; the selected
  layout determines the final CSV rows.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A permitted user completes the native builder → download workflow
  and can revisit output in the queue using real OpenELIS records without
  developer intervention.
- **SC-002**: The acceptance dataset has exactly the independently expected
  result identities, layout-appropriate row count, values and ordered headers,
  including corrections, duplicate-looking rows, nulls and inclusive date
  boundaries.
- **SC-003**: Denied clinical-access cases and attempts to access another user's
  job/file release zero clinical rows/file bytes and preserve the user's
  reviewable draft where applicable. Authorized reuse of shared definitions
  works.
- **SC-004**: Failed submission, retry, return visit, cancellation, restart,
  retention and rerun checks pass without lost parameters or partial downloads.
- **SC-005**: Keyboard and narrow-layout checks complete the workflow. Automated
  tests and applicable repository checks pass for the submitted implementation.
- **SC-006**: A 50,000-result qualification run completes with the expected
  count and bounded query batches, while the application remains usable. Record
  runtime and peak memory with environment details; this is a test workload, not
  a claim about production capacity or a universal response-time guarantee.

- **SC-007**: Two fixture configurations expose their own configured fields and
  labels through the same reporting implementation. Adding or renaming a
  supported configured item changes the available catalog without code edits,
  and a normal report user can select it and complete the export with existing
  access.

- **SC-008**: A report user saves a shared definition; another report user can
  reopen it, select fresh dates and generate the same configured layout using
  existing access. Definition edits never change previously submitted
  jobs/files.

- **SC-009**: Sample & Testing, Referrals and Non-Conformance run through the
  same builder and job/download path with fixtures proving each source's date
  and row meaning. An additional definition over an existing source appears from
  configuration without adding report-specific frontend or queue code.

- **SC-010**: The exact qualified revision is reachable at
  `reporting.catalyst.openelis-global.org`; `/__review/target.json` identifies
  that revision as ready; the live `reporting` overlay presents all critical
  stories; and an authenticated reviewer can execute the steps, inspect the
  real CSV files and submit or download a revision-bound UAT report. Automated
  preflight passes before the target is handed to a human reviewer.
