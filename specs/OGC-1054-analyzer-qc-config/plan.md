# Implementation Plan: OGC-1054 Analyzer Management

**Control:** Governed by the single active marker in the
[roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
**Spec:** [spec.md](./spec.md)
**Updated:** 2026-08-19

## Planning Rules

- The roadmap selects the one active iteration using `[✓]`, `[x]`, `[*]`, and
  `[ ]`.
- This plan contains no independent status, branch-head, CI, or deployment
  ledger.
- Requirements come from repository specs and `openelis-work` functional and
  visual artifacts. Jira is traceability only.
- `openelis-work` cannot determine repositories, APIs, persistence, events,
  migrations, or test layers.
- Production work for a future `[ ]` iteration does not start. The immediate
  successor may become `[*]` when its predecessor is `[x]` review-ready, without
  waiting for that predecessor to merge.

## Fixed Architecture

### Analyzer Bridge

Bridge owns portable versioned analyzer profiles and analyzer-facing runtime:
listeners, parsing/framing, protocol execution, connection probes, FILE
watching and transport, pinned-profile control-result recognition, normalized
FHIR output, and idempotent runtime registration. It has no hidden classifier
fallback.

### OpenELIS

OpenELIS owns laboratory-facing orchestration, analyzer instances and lab-unit
assignment, local Test/Result Option bindings, verification/audit, operational
QC, activation, held results, alerts, resolution, and downstream clinical
processing. Operational QC is independent of analyzer activation.

### Analyzer Mock

The mock owns deterministic ASTM, HL7, and FILE instruments and fixtures. Target
architecture integration tests send real protocol traffic to Bridge, never
directly to OpenELIS.

### Review Tooling

Review tooling owns the exact-build target manifest, Grist checklist overlay,
and downloadable UAT report. It does not own application fixtures or behavior.

## Durable Engineering Decisions

1. Bridge is the only portable profile authority and analyzer runtime.
2. The established Bridge-owned profile system is the implementation baseline.
   A profile defines communication/runtime for one analyzer type and the
   defaults used to create an OpenELIS analyzer instance. The strict versioned
   contract and lifecycle evolve those semantics; they do not replace them.
3. OpenELIS composes the selected profile revision with site instance values,
   local catalog bindings, verification, and operational state. It stores a pin
   and never becomes a second profile authority.
4. The existing profile corpus is curated profile by profile. A semantically
   valid emitted result remains distinct; a proven alternate spelling becomes
   an alias; incorrect, unsupported, or duplicate content is corrected or
   removed. Current rows and equal LOINCs do not create preservation rules, and
   no `LEGACY_UNBOUND` configuration concept exists.
5. Qualitative mappings bind to active Result Options belonging to the mapped
   local Test.
6. Every active Bridge profile revision declares explicit control-result
   recognition: `RULES` with one or more OR matchers, or `NONE` with no rules.
   `NONE` also requires an author affirmation that the interface transports no
   control results. Missing, undocumented, or invalid behavior is not
   selectable and no runtime fallback exists.
7. Control-recognition confirmation is distinct from operational QC and becomes
   stale only with the pinned profile/binding/recognition state.
8. Unknown traffic is held with raw context and never silently posted or lost.
9. `QCControlLot`, `QCResult`, QC statistics, Westgard evaluation, violations,
   and alerts remain the OpenELIS operational-QC path. `AnalyzerQcRule` is a
   superseded classifier and is removed; no `QcRun` is introduced.
10. Activation requires current binding/control-recognition confirmation,
    required instance values, and a Bridge acknowledgment matching the pinned
    profile and desired-state fingerprint. The same predicate governs every
    transition into `ACTIVE`; operational QC and connection-test outcomes are
    not prerequisites.
11. No OpenELIS FILE poller, raw protocol parser, dual profile writer, duplicate
    editor, or duplicate pending queue survives the MVP cutover.
12. Published profile revisions are immutable and retained while referenced.
    Update shared creates a new revision, Duplicate Profile creates a new
    identity/revision, and no analyzer moves until explicit adoption,
    re-verification, and synchronization. OpenELIS stores a pin, not an
    authoritative copied-profile snapshot.

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
contracts as a strict additive evolution of the established profile system.
Use GeneXpert ASTM and FluoroCycler as blocking compatibility fixtures for both
profile jobs. Record an evidence-based disposition for all 20 source profiles,
but publish only priority profiles whose contract, mock transport, Bridge
behavior, and assembled visible result flow pass together. Define
`controlResultRecognition`, revision retention, revision-scoped site bindings,
activation-candidate fingerprints, exact Bridge acknowledgment, and the one-way
removal of copied configuration, `defaultConfigId`, OE profile
serving/application, `AnalyzerQcRule`, and hidden Bridge fallbacks. Do not add a
runtime legacy adapter, mechanical row preservation, unproven runtime profile,
or product-mock-derived persistence.

### M1 - Profile Lifecycle And Analyzer Types

Implement Bridge profile lifecycle and the composed OpenELIS Analyzer Types
experience around the accepted established contract. Ship the evidence-backed
priority catalog from Bridge, fetch complete profile defaults in OE setup, and
persist explicit revision pins plus site values without copied profile
authority. Complete
URL-backed search/filter state, breadcrumbs, lifecycle, usage, completeness,
audit, and lab-safe Create/Duplicate/Update/Publish behavior. GeneXpert and
Fluoro form defaults, Bridge registration/runtime, and mock traffic must retain
assembled parity before the OE-hosted path is removed. Remaining source profiles
are standardized in later bounded profile-data iterations and never remain as
an OE runtime fallback.

### M2 - Safe Mapping

Implement one protocol-neutral mapping editor for every test, qualitative
value, and human-readable control-recognition row in Analyzer Types. Verify
links to this editor with a return URL and does not duplicate it. Enforce
complete active-catalog search, catalog ownership constraints, independent
source-row identity, explicit Duplicate Profile/Update shared scope, exact
candidate staleness, and removal of the OpenELIS `AnalyzerQcRule` runtime/UI
path.

### M3 - Guided Setup And Linked Operational QC

Implement the linkable Instrument, Verify, and Connect sequence with a readable
completion summary. Consume the pinned profile's declared default and
LIS-initiated capability without application inference, and render Bridge's
structured probe evidence inline. Remove the standalone create/edit and
connection-modal paths once the replacement is green. Keep Bridge probes and
capabilities separate from OpenELIS operational-QC policy. Link to the canonical
QC workflow, but permit activation only from the exact non-QC predicates in
`MVP-016`, applied to every transition into `ACTIVE`.

### M4 - Safe Traffic

Assemble known patient/recognized-control and unknown traffic across mock,
Bridge, and OpenELIS. Complete visible setup capture/reconciliation and
draft-type learning, hold and resolve unknown traffic, expose Alerts/Needs
attention, remove legacy writers/readers/classifiers and schema, and add the
complete UI-only Playwright story.

### G0 - Deployed MVP Acceptance

Deploy one exact release candidate, publish the 17-step Grist story, run and
inspect the non-video UI run first, then record MP4 and obtain human UAT for
the unchanged build.

### R1 - Full Feature Rollout

Deliver these separately reviewable iterations in order: R1.1 mature alert
triage, assignment, concurrency, and navigation; R1.2 profile revision
diff/bulk adoption/rollback beyond MVP's explicit one-analyzer adoption, backup
export, and distribution hardening; then R1-G exact-build human acceptance.

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

| Layer                      | Owns                                                                                                        |
| -------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Bridge unit/service        | Profile lifecycle, `RULES`/`NONE`, rule OR semantics, no fallback, protocol runtime, probes, registration   |
| OpenELIS unit/service/DAO  | Catalog constraints, bindings, audit, migration, exact activation, independent operational QC, hold/resolve |
| Producer/consumer contract | Pinned profile, registration without operational QC, normalized patient/control FHIR, raw context, FILE     |
| Analyzer mock              | Deterministic patient, recognized-control, nonmatch, `NONE`, unknown, failure, and two-way fixtures         |
| Harness integration        | Assembled OE/Bridge/mock behavior through real transport and persistence                                    |
| RTL with real router       | Carbon interactions, validation, breadcrumbs, URL/query state, reload/history                               |
| Playwright                 | Complete visible user story only; no API-driven acceptance                                                  |
| Grist UAT                  | Human functional and visual acceptance of one exact remote build                                            |

## PR And Merge Rules

- Each roadmap iteration is one manageable PR per repository that owns a
  required change.
- Companion PRs are created only when a failing contract proves work is needed
  in that repository.
- A future branch may be preserved but does not become active before its
  predecessor is `[x]` review-ready.
- A checkpoint may not merge before every predecessor is `[✓]` merged.
- After a predecessor merges, descendants rebase on the landed target and rerun
  applicable tests, then record that predecessor as `[✓]`.
- The roadmap markers change only for a formal start, review-ready exit, or
  completed merge. GitHub remains authoritative for review and merge state.
- An iteration remains `[x]` throughout review and review corrections until it
  merges; review does not change roadmap markers or the active child.
