# Implementation Plan: Configurable Reporting MVP

**Branch**: `spec/479-ogc-479-reporting-mvp`  
**Date**: 2026-09-13  
**Status**: Canonical frontend, Sample & Testing and queue recovery are publicly deployed.
Full-suite recovery qualification, remaining M1 checks and the other source mappings are in progress;
the complete MVP remains pending.

**Specification**: [spec.md](spec.md)  
**Inspected code baseline**: `e57a53399c2134fe3ff58009119cc05906c61e5e`

## Summary

Build one native reporting capability: instance-aware fields and filters, a
default spreadsheet with configured tests as columns, a detailed result list,
shared named report definitions and reliable CSV delivery. Preserve every repeat
result through additional spreadsheet rows. Reuse existing OpenELIS access so
normal report users do not need another setup or approval journey.

Sample & Testing is the first complete proof. Referrals and Non-Conformance use
source definitions and mappings in the same feature. They do not get separate
builders, saved-report stores, job queues or export implementations. Both
milestones are required for the complete mock-derived MVP.

## Technical Context

| Area          | Approach                                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Platform      | Existing Java 21/Spring MVC application, React and Carbon; no new application or queue service                            |
| Interface     | Existing Reports navigation; React Intl and current shared TanStack Query utilities                                       |
| Data          | Parameterized JPA/Hibernate queries over existing records; a source mapping supplies normalized records to common layouts |
| Configuration | Reuse existing configuration-loading conventions for versioned report-source definitions and instance metadata            |
| Persistence   | Existing PostgreSQL through Liquibase for shared definitions and immutable jobs; protected persistent CSV storage         |
| Execution     | One bounded background generation path; inline ready-file action and queue for longer work/return visits                  |
| Testing       | JUnit 4, ORM validation, database integration, Vitest/Testing Library, Playwright `core-app`                              |

The [research](research.md), [logical model](data-model.md),
[application contract](contracts/export-api.md) and
[acceptance plan](quickstart.md) support this plan. File and class names below
identify ownership, not a mandatory class-by-class design.

## Constitution Check

Reviewed against constitution v1.11.1. These are design checks, not
implementation test results.

- [x] Configuration-driven variation; no hardcoded country, lab or fixture
      behavior.
- [x] Carbon, keyboard-accessible controls and React Intl; English source keys
      only.
- [x] Existing internal user-download workflow; no new external interoperability
      interface.
- [x] Five-layer architecture, annotated new entities, service transactions and
      response-data compilation inside the transaction.
- [x] Parameterized HQL/JPA, supported source mappings and field definitions; no
      arbitrary database-column or raw-SQL export interface.
- [x] Registered versioned Liquibase migrations with rollback; empty/populated
      database and ORM validation planned.
- [x] Existing access and ordinary audit mechanisms reused in the background.
- [x] Meaningful tests before complex implementation; repository coverage goals
      (>80% backend, >70% frontend new code) and applicable CI checks retained.
- [x] Current Playwright guidance and `core-app`; no new Cypress tests.
- [x] Two validation milestones, one PR each. Do not extend the legacy Routine
      CSV implementation; address any actual superseded legacy path with removal
      or a tracked removal plan.

No constitution exception, new framework or shared agent-context change is
needed.

## Frontend State and Interaction Standards

The supplied mock remains the interface authority. Its implementation must also
behave like a native application:

- React Router owns the view, builder step, report type/layout, saved-report link,
  queue page and selected job in the query string. Preserve unrelated query
  parameters; reload and browser Back/Forward restore the intended screen.
- Session drafts retain unsaved columns, filters and dates through navigation.
  Loading a shared report or re-running expired output deliberately asks for fresh
  dates. Late server responses cannot replace a newer draft.
- The shared query library owns catalog, saved-report and job data. Mutations
  update or invalidate the affected records. Loading, empty and failure states
  remain distinct; an unanswered catalog request is not evidence of removed fields.
- Use Carbon controls, React Intl, visible focus, keyboard operation, meaningful
  labels and responsive layouts. Prevent duplicate submission while a request is
  pending and retain user choices when it fails.
- Validate the rendered workflow against the pinned mock at matched desktop and
  narrow widths, then exercise navigation, recovery and actual CSV downloads.
  A passing component test or a ready badge alone does not establish the experience.

Implementation inspection found existing `ReportDefinition` storage with JSON,
report type, shared visibility and optimistic versioning. Reuse it for
configured CSV sources and shared saved definitions under distinct report types,
retaining the existing patient-report type behavior. Only export jobs need a new
table. See [execution.md](execution.md) for current implementation and
deployment evidence.

## Milestone Plan

| ID  | Branch suffix       | Scope                                                                                                                                  | Verification                                                                                             | Depends on        |
| --- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ----------------- |
| M1  | `m1-result-export`  | Common configured engine, Sample & Testing, instance-aware fields, both layouts, shared saved definitions, inline download/basic queue | US1–US3 with real records; repeat preservation; catalog changes without code edits; shared report rerun  | Spec/contract     |
| M2  | `m2-queue-recovery` | Referral/non-conformance source definitions and mappings using the same engine; recovery, retention and qualification                  | All three configured report types; US4; same-engine configuration test; restart and workload checks      | M1                |
| UAT | exact qualified SHA | Public Catalyst reporting target, stable review fixtures, Grist checklist, review overlay and deployment provenance                    | Live automated preflight plus executable critical-workflow UAT; human results remain separately recorded | Each usable stage |

```mermaid
graph LR
    S[Specification and clarification] --> M1[One engine: real Sample and Testing export]
    M1 --> U[Publish current stage to the shared UAT target]
    M1 --> M2[Additional source definitions and recovery]
    M2 --> U
    U --> R[Automated and human review of the same workflows]
    R --> M1
    R --> M2
```

Create the spec PR first. The constitution allows implementation during spec
review. Use `feat/479-ogc-479-reporting-mvp-m1-result-export` and
`feat/479-ogc-479-reporting-mvp-m2-queue-recovery`, targeting `develop` with one
PR per milestone. If M2 begins before M1 merges, keep its review diff isolated
from M1. No integration branch is necessary. Merge and deployment are separate
actions from creating the implementation and its evidence.

Publication is part of each usable implementation stage. The public target must
reflect ongoing work; do not wait for M2 or the full MVP. Automated end-to-end
checks and human UAT share workflows, stable fixtures and expected outcomes.
Record known failures and unfinished capabilities on the current stage while
retaining the full MVP completion criteria.

This sequence is about proving a useful workflow early, not separate projects or
a multi-week discovery phase. AI-assisted implementation can accelerate tasks;
working exports and focused acceptance evidence determine completion.

## Implementation Approach

### One Engine, Configured Sources

A report-source definition describes its label, source mapping, fields and
filters, date anchor, row identity, grouping and available layouts. The shared
builder consumes this definition and the instance's configured metadata. The
engine resolves the request, reads bounded batches, produces the selected layout
and creates one CSV artifact. Source definitions use supported mapping
identifiers and typed filters, not executable user-supplied SQL.

Common attributes remain application-defined. Local tests, result components and
additional questions come from existing configuration when their source mapping
is supported. Adding/renaming such an item must not require reporting-code
edits. Adding a report definition over an existing mapping is configuration
work. A genuinely different data source may need a small mapping; configuration
does not infer unknown relationships or clinical calculations by itself.

Use the existing configuration handler/initialization pattern where suitable. Do
not prescribe JSON versus YAML or a new configuration-management screen here.
Ship definitions for the mock's three report types and validate them at load
time.

### Values and Layouts

For Sample & Testing, start from stable result identity, its analysis and
specimen. Filter collection dates using `SampleItem.collectionDate`, not
order-entry date. Resolve statuses and configured names through existing
services. Default to finalized results; supported status filters remain
available. Prove corrections, dictionary/multi-valued results, result components
and missing numeric precision with fixtures before accepting the mapping.

The detailed layout retains one row per independent result. The default
spreadsheet puts selected configured tests/components in columns and retains all
repeated results in continuation rows. Do not produce cross-products or invent
same-event pairings between unrelated repeats. Repeat specimen/sample metadata
and leave unrelated test cells blank as needed. Row identity must preserve
separate specimens under one accession. Both layouts reconcile to the same
result identities and values, although their row counts differ.

Turnaround follows the accepted per-test spreadsheet meaning. Emit the three
result-dependent intervals with their test/component measurements so the common
pivot preserves each repeat's times. Expose these optional fields through
separate `testTurnaround` and `componentTurnaround` catalogs; ordinary test
defaults therefore remain unchanged. Keep specimen intervals common and generic
result intervals in the detailed layout. Reject obsolete generic spreadsheet
interval selections through the existing stale-column handling.

Referral and non-conformance definitions declare their own row/date meanings. Do
not reinterpret an event's reported date as the event date or assume a catalog
entry is an event occurrence. The mapping tests prove those differences. Flat
event reports can use the common tabular formatter directly; they do not need an
artificial test pivot.

### Interface Authority and Frontend State

Frontend quality is part of acceptance for each usable stage. Use maintainable
React components and the application's shared routing, query and Carbon patterns.
Judge the result by a smooth user journey: predictable navigation, retained work,
responsive controls and understandable feedback throughout the workflow.

The pinned interactive mock is authoritative for layout and interactions. MVP
scope limits connected functionality; it does not reduce the supplied design.
Use the direct comparison gate in [design-parity.md](design-parity.md) for every
UI increment, with actual desktop and narrow-screen interactions.

React Router owns navigable state in the query string: overview/builder/saved/
queue, builder step, source and layout, shared-report identity, queue page/job
and saved-report search. Browser Back/Forward and reload must reproduce that
location. Preserve unrelated review parameters. Normalize invalid parameters
with replacement navigation; do not add history entries for typing or repairs.
A deep link to review without the necessary draft returns to the required step.

Draft contents (ordered selections per source/layout, filters and dates) remain
in the existing browser-session draft. A saved-report link loads its server
configuration and asks for fresh dates. The URL does not claim to serialize an
unsaved report. TanStack Query owns server catalog, saved definitions and frozen
job records. Late responses must not overwrite a newer draft, and completed jobs
remain retrievable through the queue after navigation.

Keep transient interface controls local to the current draft. Reset them for a
new export, preserve keyboard focus during ordering and move focus into a newly
opened workflow step. Render collapsed groups without mounting every field
control; filter requests must not fire on every keystroke. Include loading,
empty, error and recovery states. Use Carbon controls, tokens and localized copy.
Check real downloaded values, unexpected browser errors, accessibility and
visual fidelity separately; a passing screenshot capture alone proves none of
the others.

### Navigation Configuration Contract

Retain one navigation model: database menu definitions, explicit instance
configuration overrides, existing filtering, then the shared Carbon renderer.
The mock determines the Reporting UAT profile; its particular sections and
destinations belong in that profile. Existing menu identities, hierarchy,
ordering, translated labels, visibility and destinations remain configurable.

Extend the database and configuration layers with the same optional section and
icon metadata. The local menu persistence increment now supplies
`presentationStyle` and `icon` through both database rows and mounted JSON,
with administrative editing. Its publication and remaining runtime qualification
are tracked in `execution.md`. Add them through the existing menu service and administration
path, with a versioned, reversible migration and backward-compatible defaults.
Show when an instance override controls an edited value so a successful save
cannot misleadingly appear to have changed the effective navigation.

Only explicitly supplied configuration fields override database values. Loading
an instance profile must not persist its reorganized tree over database defaults.
Verify saved metadata after reload and application restart, explicit override and
override removal, preservation of unlisted extensions and existing filtering,
and the effective server tree in the rendered sidebar. Use two distinct instance
profiles without frontend edits, then repeat the mock, routing and retained-draft
checks before publishing the increment. Database/editor support is not a gate
for continued access to the already usable public stage.

### Low-Friction Interface and Shared Reports

Use one builder with configuration-driven labels, groups, filters and defaults.
Keep selected columns visible, support keyboard ordering, retain choices on
navigation/failure and preserve filters when switching layouts. Each layout can
retain its own ordered column selection while the draft is open.

Save named report definitions in an instance-wide list: source definition,
layout, ordered fields and non-date filters. Existing report users can reopen,
save a copy, update or delete with the relevant confirmation. Use version checks
for concurrent edits. Creator/change metadata provides traceability, not private
ownership. A fresh date range is chosen per run. Definitions contain
configuration; they do not grant access to another user's generated file.

Generation uses one persisted background-job path. The reporting page observes
the submitted job and offers its download as soon as it is ready, without
requiring a queue-page visit. The queue remains available for longer jobs and
return visits. Separate estimation and synchronous-generation paths are deferred
implementation optimizations, not reasons to add user steps.

### Execution, Existing Access and Recovery

Freeze source/version, layout, ordered field identities/labels, dates and
resolved scope on submission. Use idempotent admission and atomic claims. Query
in bounded batches and stage output privately; publish only a complete file.
Re-download returns that stored file. Re-read current access before
execution/download using existing OpenELIS authority; do not build a new
permission-administration product.

Start with one export worker per application instance and FIFO claims,
coordinated in the database. Recovery must distinguish abandoned work from
another live worker. Retry creates a new linked job with the original request;
rerun from expiry uses fresh dates. Cancellation applies only before processing
starts. Keep current limits configurable: initially 90 inclusive calendar days,
five active jobs per user and seven days of completed-file retention.

Wire protected persistent output storage through existing deployment
conventions. Poll active jobs using current shared query utilities, stop when
idle and refresh on return; pagination must not lose known active jobs. Ordinary
audit and existing access checks remain focused backend requirements.

## Project Structure

```text
specs/479-reporting-mvp/                              # this package
src/main/java/org/openelisglobal/reports/dataexport/  # common feature
  valueholder/ dao/ service/ controller/ form/
src/main/resources/                                 # report definitions using current configuration conventions
src/main/resources/liquibase/3.6.x.x/                # new persistence and rollback
src/test/java/org/openelisglobal/reports/dataexport/
src/test/resources/                                 # clinical/configuration fixtures
frontend/src/components/reports/CustomDataExport/   # common builder, saved reports, queue
frontend/src/components/reports/Routine.jsx         # existing report navigation
frontend/src/languages/en.json
frontend/playwright/tests/foundational/core/
```

These are planned locations. Refresh branch state and the current migration
registry at implementation start. No application files were changed by this
spec.

## Testing Strategy

Tests must establish functional output, not only HTTP success. Compare
independently specified identities and values across both layouts; test instance
metadata changes, shared definition reuse, three source definitions and an
additional configuration over an existing source without frontend changes. Test
source-specific dates, corrections, nulls and repeated records. Keep ordinary
existing-access negative tests, concurrency/recovery tests and the 50,000-result
qualification focused.

Use the [quickstart](quickstart.md) for fixtures, browser flows and commands.
Run focused tests during development, then applicable repository format/build,
coverage and CI checks. Record tested revision, actual CSV comparison, browser
console/screenshots and measured workload. Distinguish local tests, CI, deployed
behavior and user acceptance.

### UAT Delivery

Use the established `DIGI-UW/openelis-review-tooling` system. Grist is the
checklist source of truth. The public reporting deployment loads the review
overlay and publishes verified target metadata only after application health
and critical browser preflight pass. The deployment must preserve the existing
Catalyst application and its unrelated CSiM services.

The reviewer-visible stories and stable keys are defined in [uat.md](uat.md).
Before writing them to Grist, dry-run their exact prose against the deployed
target and stable synthetic fixtures. A valid checklist endpoint proves only
delivery; a submitted or downloaded review report records human acceptance.

## Complexity Tracking

The necessary additions are configurable source mappings, two reusable layouts,
shared definitions and durable jobs/files. No general SQL designer, external
queue, separate report applications, personal-library permissions or new
authentication scheme is planned.
