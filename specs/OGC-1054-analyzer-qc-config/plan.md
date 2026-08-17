# Implementation Plan: OGC-1054 Analyzer Management

**Control:** Governed by the single active marker in the
[roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
**Spec:** [spec.md](./spec.md)
**Updated:** 2026-08-17

## Planning Rules

- The roadmap selects the one active iteration using `[x]`, `[*]`, and `[ ]`.
- This plan contains no independent status, branch-head, CI, or deployment
  ledger.
- Requirements come from repository specs and `openelis-work` functional and
  visual artifacts. Jira is traceability only.
- `openelis-work` cannot determine repositories, APIs, persistence, events,
  migrations, or test layers.
- Production work for a future `[ ]` iteration does not start.

## Fixed Architecture

### Analyzer Bridge

Bridge owns portable versioned analyzer profiles and analyzer-facing runtime:
listeners, parsing/framing, protocol execution, connection probes, FILE
watching and transport, QC-sample identification, normalized FHIR output, and
idempotent runtime registration.

### OpenELIS

OpenELIS owns laboratory-facing orchestration, analyzer instances and lab-unit
assignment, local Test/Result Option bindings, verification/audit, operational
QC, activation, held results, alerts, resolution, and downstream clinical
processing.

### Analyzer Mock

The mock owns deterministic ASTM, HL7, and FILE instruments and fixtures. Target
architecture integration tests send real protocol traffic to Bridge, never
directly to OpenELIS.

### Review Tooling

Review tooling owns the exact-build target manifest, Grist checklist overlay,
and downloadable UAT report. It does not own application fixtures or behavior.

## Durable Engineering Decisions

1. Bridge is the only portable profile authority and analyzer runtime.
2. OpenELIS composes Bridge profile metadata with local catalog bindings and
   operational state; it does not copy profiles into a second authority.
3. Existing OpenELIS profile files and copied plugin JSON are migration inputs
   only.
4. Distinct source rows remain distinct even when normalized coding or the
   selected local Test is shared.
5. Qualitative mappings bind to active Result Options belonging to the mapped
   local Test.
6. QC-identification confirmation is distinct from operational-QC readiness.
7. Unknown traffic is held with raw context and never silently posted or lost.
8. Existing `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard services
   remain the operational analyzer-QC path. No `QcRun` is introduced.
9. No OpenELIS FILE poller, raw protocol parser, dual profile writer, duplicate
   editor, or duplicate pending queue survives the MVP cutover.

## Iteration Plan

The authoritative order and markers live only in the roadmap.

### R0 - Roadmap And Specification Authority

Deliver the canonical artifact set, classify historical work, preserve dirty
work safely, remove competing authority, validate consistency, and merge the
roadmap before implementation resumes.

### F0 - Foundation Salvage

Characterize selected behavior from historical PR #3792 with tests. Retain only
behavior compatible with the fixed architecture. Do not cherry-pick mixed
feature commits or preserve legacy writers as compatibility paths.

### E0 - Contracts And Migration

Complete the Bridge-profile/OpenELIS-binding ADR and versioned producer/consumer
contracts. Characterize legacy profiles, copied configuration, mappings, raw
ingestion, and existing analyzers. Define no-loss migration, rollback, anomaly
handling, and one-writer cutover before M1 production migration.

### M1 - Profile Lifecycle And Analyzer Types

Implement Bridge profile lifecycle and the composed OpenELIS Analyzer Types
experience. Migrate existing analyzers without heuristic profile inference or
silent row collapse. Complete URL-backed search/filter state, breadcrumbs,
lifecycle, usage, completeness, audit, and lab-safe create/fork behavior.

### M2 - Safe Mapping

Implement one protocol-neutral mapping editor for every test, qualitative
value, and QC-identification row. Enforce complete active-catalog search,
catalog ownership constraints, independent source-row identity, explicit
fork/update scope, and stale verification.

### M3 - Guided Setup And QC

Implement the linkable Instrument, Verify, and Connect sequence with a readable
completion summary. Keep Bridge probes and capabilities separate from OpenELIS
operational-QC policy. Resolve `AMB-M3-001` in the owning spec/contract before
starting this iteration.

### M4 - Safe Traffic

Assemble known patient/QC and unknown traffic across mock, Bridge, and
OpenELIS. Complete visible setup capture/reconciliation and blank-type learning,
hold and resolve unknown traffic, expose Alerts/Needs attention, remove legacy
writers/readers, and add the complete UI-only Playwright story.

### G0 - Deployed MVP Acceptance

Deploy one exact release candidate, publish the 17-step Grist story, run and
inspect the non-video UI run first, then record MP4 and obtain human UAT for
the unchanged build.

### R1 - Full Feature Rollout

Deliver these separately reviewable iterations in order: R1.1 mature alert
triage, assignment, concurrency, and navigation; R1.2 profile revision
diff/update/rollback, backup export, and distribution hardening; then R1-G
exact-build human acceptance.

### R2 - Operational Rollout

Validate scale, resilience, security, monitoring, documentation, representative
sites, and per-instrument vendor evidence.

## TDD And Test Ownership

For each acceptance slice:

1. Select an acceptance ID assigned to the active iteration.
2. Write the first failing test at the layer owning the rule.
3. Implement the smallest behavior that makes that test pass.
4. Refactor while the targeted test remains green.
5. Add broader coverage only where the behavior crosses a real boundary.
6. Run the active iteration's complete exit gate before review.

| Layer                      | Owns                                                                                     |
| -------------------------- | ---------------------------------------------------------------------------------------- |
| Bridge unit/service        | Profile lifecycle, validation, protocol runtime, probes, QC identification, registration |
| OpenELIS unit/service/DAO  | Catalog constraints, bindings, audit, migration, readiness, hold/resolve                 |
| Producer/consumer contract | Profile, registration, normalized FHIR, raw unknown context, FILE and QC boundaries      |
| Analyzer mock              | Deterministic real-protocol known, unknown, QC, failure, and two-way fixtures            |
| Harness integration        | Assembled OE/Bridge/mock behavior through real transport and persistence                 |
| RTL with real router       | Carbon interactions, validation, breadcrumbs, URL/query state, reload/history            |
| Playwright                 | Complete visible user story only; no API-driven acceptance                               |
| Grist UAT                  | Human functional and visual acceptance of one exact remote build                         |

## PR And Merge Rules

- Each roadmap iteration is one manageable PR per repository that owns a
  required change.
- Companion PRs are created only when a failing contract proves work is needed
  in that repository.
- A future branch may be preserved but does not become active or merge before
  its predecessor is `[x]`.
- After a predecessor merges, descendants rebase on the landed target and rerun
  applicable tests.
- The roadmap marker changes only in the PR that starts or finishes an
  iteration.
