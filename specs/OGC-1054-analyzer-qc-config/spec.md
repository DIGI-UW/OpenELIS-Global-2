# OGC-1054 Analyzer QC and Configuration MVP

**Status:** MVP accepted on analyzer UAT; PR pending merge
**Branch:** `codex/ogc-1054-analyzer-qc-mvp`
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)
**Design baseline:** [`DIGI-UW/openelis-work@4c0e1a28`](https://github.com/DIGI-UW/openelis-work/tree/4c0e1a28/designs)

## Purpose

Give a laboratory administrator one coherent, profile-driven workflow to add an
analyzer, verify its test and qualitative-result mappings, configure its
connection and applicable QC, and review activation readiness.

Shipped ASTM, HL7, and FILE profile files are the MVP profile source of truth.
An analyzer stores its selected defaults through the existing
`defaultConfigId` flow and stores analyzer-specific overrides in plugin config
JSON. The MVP does not add an `AnalyzerProfile`, result-mapping, or `QcRun`
table.

## User Stories

### US1 - Inspect a shipped analyzer type

As a laboratory administrator, I can search and filter shipped analyzer types,
inspect protocol, connection mode, mapping/QC counts, and readiness, then start
setup from a selected type.

### US2 - Complete guided analyzer setup

As a laboratory administrator, I can complete four explicit, bookmarkable
steps: Instrument, Verify, Connect, and Review. Browser back/forward and reload
preserve the current saved-analyzer step.

### US3 - Verify mappings

As a laboratory administrator, I can review profile-applied test mappings,
pending analyzer codes, and result-value mappings in one workflow. Qualitative
targets are active catalog options belonging to the mapped OpenELIS test.

### US4 - Configure analyzer QC

As a laboratory administrator, I can create or select active analyzer QC rules
and control lots through the setup workflow. Readiness is recalculated and
bridge registration is resynchronized after relevant changes.

### US5 - Understand readiness

As a laboratory administrator, I can see every blocker before activation,
including stale mapping/QC verification. I can see who last verified each
configuration and when.

## Functional Requirements

- **FR-001:** `/analyzers` is the primary analyzer setup and management page.
- **FR-002:** `/analyzers/new` redirects to the canonical inline Instrument
  step.
- **FR-003:** `/analyzers/types` is a lab-facing shipped-profile catalog, not a
  developer plugin registry.
- **FR-004:** Creating an analyzer with `defaultConfigId` applies profile
  defaults exactly once. Editing the analyzer never reapplies the profile.
- **FR-005:** The four setup steps have canonical URLs defined in
  [contracts/frontend-routes.md](contracts/frontend-routes.md).
- **FR-006:** List search and filters are encoded in the query string and
  restored on load, reload, back, and forward navigation.
- **FR-007:** Analyzer pages use a shared Carbon page header with a semantic
  `h1`, linkable breadcrumb path, and Carbon actions.
- **FR-008:** Mapping review uses Carbon data-table patterns and exposes test
  mappings, pending codes, result-value mappings, and verification state.
- **FR-009:** Result-value resolution requires an
  `openelisResultOptionId`. The server derives the value and label.
- **FR-010:** Inactive options and options outside the pending value's mapped
  test are rejected.
- **FR-011:** Legacy free-text mappings remain readable as `LEGACY_UNBOUND`,
  but never count as complete verification.
- **FR-012:** Mapping and QC verification record IDs, fingerprints, actor, and
  time in plugin config JSON and emit a durable analyzer audit event.
- **FR-013:** Mapping or QC changes invalidate the corresponding prior
  verification through fingerprint mismatch.
- **FR-014:** `ACTIVE` is blocked until mappings are currently verified and
  profile-applicable QC requirements are ready.
- **FR-015:** Connection testing is visible saved-analyzer evidence, not a
  persisted activation prerequisite.
- **FR-016:** Bridge registration always emits deterministic `qcRules`,
  `controlLots`, and `testCodeLoinc` collections, including empty collections.
- **FR-017:** FILE directory watching and transport remain bridge-owned.
  OpenELIS configures, registers, ingests direct submissions, and processes
  results/QC.

## Deterministic Acceptance Criteria

| ID         | Acceptance criterion                                                                                                                                                        | Primary proof            |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ |
| AC-1054-01 | Profile catalog search, protocol, and readiness filters round-trip through the URL and restore identical visible rows after reload.                                         | RTL + UI E2E             |
| AC-1054-02 | Every analyzer/profile/setup page has one semantic `h1` and a breadcrumb whose links resolve to valid application routes.                                                   | RTL + UI E2E             |
| AC-1054-03 | Starting setup from a profile opens the Instrument URL with the selected profile and a safe encoded `returnTo`.                                                             | RTL + UI E2E             |
| AC-1054-04 | Submitting Instrument creates exactly one analyzer, applies defaults once, and navigates to Verify using the returned analyzer ID.                                          | JUnit + RTL + UI E2E     |
| AC-1054-05 | Reload, back, and forward retain the active saved-analyzer setup step without creating or mutating data.                                                                    | UI E2E                   |
| AC-1054-06 | Verify presents profile-applied test mappings, pending codes, qualitative mappings, and current/stale verification with Carbon table semantics.                             | RTL + UI E2E             |
| AC-1054-07 | Pending qualitative resolution offers only active options for the mapped test and persists the chosen option ID, derived value, and label.                                  | JUnit + RTL + UI E2E     |
| AC-1054-08 | Wrong-test and inactive result options return a validation error and leave the pending value unresolved.                                                                    | JUnit                    |
| AC-1054-09 | Mapping/QC verification stores fingerprints, actor, and time, emits a durable audit event, and becomes stale after relevant change.                                         | JUnit                    |
| AC-1054-10 | QC-rule and control-lot create/update/delete operations recompute readiness and invoke bridge registration sync.                                                            | JUnit                    |
| AC-1054-11 | Setup detours to QC rule/control-lot pages preserve `returnTo` and return to the originating Verify step after save or cancel.                                              | RTL + UI E2E             |
| AC-1054-12 | Connect displays protocol-appropriate fields and a visible success/failure result from a real saved-analyzer connection test.                                               | RTL + UI E2E             |
| AC-1054-13 | Review lists all readiness blockers; `ACTIVE` is rejected while mapping verification is stale or applicable QC is incomplete.                                               | JUnit + RTL + UI E2E     |
| AC-1054-14 | Bridge payload collection ordering and empty collections are deterministic; no human UAT inspects payload internals.                                                        | JUnit/contract test      |
| AC-1054-15 | The focused Playwright acceptance story uses visible UI only: no `page.request`, API assertions, response polling, forced Carbon controls, or arbitrary waits.              | Playwright guard + audit |
| AC-1054-16 | Final evidence identifies application SHA, harness SHA, deployment time, checklist revision, routes, statuses, mark times, screenshots, and MP4 for all required UAT steps. | Remote UAT report        |

## Boundaries

The following are separate milestone work:

- analyzer result import and Results/Validation v4 integration;
- multi-component target-to-component mapping;
- persisted, forkable shared profile management;
- per-instrument code where a shipped profile expresses the protocol;
- bridge changes without failing OpenELIS contract evidence;
- any OpenELIS FILE poller.

## Clarifications

- Profile catalog readiness means the shipped file can start setup. Analyzer
  activation readiness is calculated from the saved analyzer configuration.
- A missing `qcApplicable` value does not waive QC. A profile opts out only with
  explicit `qcApplicable: false`.
- Connection test state is transient user-facing evidence. It does not replace
  mapping/QC verification or activation readiness.
