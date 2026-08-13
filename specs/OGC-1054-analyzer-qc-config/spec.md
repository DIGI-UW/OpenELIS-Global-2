# OGC-1054 Analyzer QC and Configuration Foundation

**Status:** Implemented foundation; not OGC-1054 MVP acceptance
**Branch:** `codex/ogc-1054-analyzer-qc-mvp`
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)
**Authoritative feature roadmap:**
[OGC-1054 Analyzer Feature](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)

## Purpose

Record the bounded behavior implemented by PR #3792: a shipped-profile
bootstrap/catalog, URL-addressable setup shell, safer local result-value
bindings, verification/readiness, and operational analyzer QC configuration.
This SpecKit set is retained for branch provenance and is not the full OGC-1054
product specification.

## Source Boundary

`DIGI-UW/openelis-work` is used only to compare functional user outcomes and
visual intent. No persistence, entity, API, route, repository-boundary,
transport, or implementation decision in this specification comes from that
repository. Current code, engineering specifications, `AGENTS.md`, and an
approved ADR/contract govern implementation.

## Fixed Architecture

- Bridge owns portable profiles and analyzer runtime: listeners, parsing,
  connection probes, protocol execution, FILE watching/transport, and
  normalized FHIR delivery.
- OpenELIS owns lab-facing setup, local catalog binding, analyzer instance/lab
  assignment, audit, operational QC, activation, held results, and review.
- The analyzer mock proves real protocol behavior through Bridge.
- OpenELIS must not gain a raw protocol runtime or FILE poller.

## Foundation User Stories

### FUS1 - Inspect a shipped bootstrap profile

A laboratory administrator can search/filter shipped profile files, inspect
protocol and summary counts, and start analyzer setup.

### FUS2 - Use a bookmarkable setup shell

A laboratory administrator can move through Instrument, Verify, Connect, and
Review routes with linked breadcrumbs and saved-analyzer state.

### FUS3 - Apply local mapping safeguards

A laboratory administrator can review copied test mappings, bind qualitative
values only to valid options for the mapped Test, resolve an already-pending
code/value, and record verification state.

### FUS4 - Configure operational analyzer QC

A laboratory administrator can configure existing analyzer QC rules and control
lots, see readiness blockers, and trigger Bridge registration resynchronization.

## Foundation Requirements

- **FFR-001:** `/analyzers` is the branch's primary setup entry and
  `/analyzers/new` redirects to it.
- **FFR-002:** Branch setup/list state uses canonical routes/query parameters,
  safe same-origin `returnTo`, semantic headings, and linked breadcrumbs.
- **FFR-003:** Creating with `defaultConfigId` applies a shipped bootstrap
  profile exactly once; edit does not reapply it.
- **FFR-004:** Result-value selection requires an active Result Option owned by
  the mapped Test; server derives label/value.
- **FFR-005:** Legacy free-text values remain readable as `LEGACY_UNBOUND` and
  do not satisfy verification.
- **FFR-006:** Mapping/QC verification records fingerprints, actor, and time and
  writes durable audit evidence.
- **FFR-007:** Relevant changes make verification stale; activation is blocked
  while branch-defined mapping or operational QC readiness is incomplete.
- **FFR-008:** Existing `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and
  Westgard paths remain the operational QC model; no `QcRun` is added.
- **FFR-009:** Bridge registration collections are deterministic, including
  explicit empty `qcRules`, `controlLots`, and `testCodeLoinc` collections.
- **FFR-010:** FILE watching, polling, and transport remain Bridge-owned.

## Foundation Acceptance Criteria

| ID | Criterion | Proof |
| --- | --- | --- |
| FAC-001 | Search/filter/setup URL state restores the same visible branch state after reload. | RTL + foundation UI E2E |
| FAC-002 | Touched analyzer setup pages have one `h1` and valid linked breadcrumbs. | RTL + foundation UI E2E |
| FAC-003 | Create applies bootstrap defaults once and navigates using the returned analyzer ID. | JUnit 4 + RTL |
| FAC-004 | Wrong-test/inactive Result Options are rejected without resolving pending state. | JUnit 4 |
| FAC-005 | Verification/audit/readiness becomes stale after relevant branch-owned changes. | JUnit 4 |
| FAC-006 | Operational QC changes recompute readiness and resync Bridge registration. | JUnit 4 |
| FAC-007 | The branch's connection probe result is visibly presented. | RTL + foundation UI E2E |
| FAC-008 | The foundation Playwright story uses visible UI rather than API-focused shortcuts. | Playwright guard/audit |

## Explicitly Not Delivered

- reusable, versioned Analyzer Type lifecycle and a living analyzer/profile
  association;
- site-created/forked types, lineage, completeness, usage, or lifecycle;
- full add/edit/remove/repoint mapping and unmatched profile-row handling;
- Bridge profile QC-identification-code confirmation;
- capability-aware Results only/Two-way behavior;
- production creation, hold, alert, and resolution of unknown Bridge traffic;
- integrated analyzer-mock -> Bridge -> FHIR -> OpenELIS MVP evidence;
- full OGC-1054 acceptance.

Those outcomes are specified only by the authoritative roadmap and future
milestone specs.
