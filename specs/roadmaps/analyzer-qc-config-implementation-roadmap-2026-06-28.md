# Analyzer QC and Configuration Implementation Roadmap

**Updated:** 2026-07-28
**Status:** Acceptance remediation in progress
**Epic:** [OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054)
**Branch:** `codex/ogc-1054-analyzer-qc-mvp`
**Pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)

## Objective

Deliver a lab-facing, profile-driven analyzer QC/configuration MVP through one
reviewable branch and non-draft PR. The code currently provides the backend and
UI foundation. Final acceptance requires the URL-addressable Carbon workflow,
current automated validation, and build-bound remote UAT evidence.

The normative feature definition is
[OGC-1054-analyzer-qc-config](../OGC-1054-analyzer-qc-config/spec.md).
Git history preserves prior roadmap states; this file describes the current
state only.

## Governing Decisions

- Shipped ASTM, HL7, and FILE profile files are the MVP source of truth.
- Analyzer creation uses existing `defaultConfigId`; profile defaults apply
  once.
- Analyzer-specific result mappings and verification metadata use plugin config
  JSON. No new profile or mapping table is introduced.
- Analyzer QC uses `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard.
  `QcRun` is not part of this path.
- Bridge owns FILE watching, polling, transport, retries, and runtime parser
  state. OpenELIS owns configuration, registration, direct ingestion, and
  result/QC processing. No OpenELIS FILE poller is permitted.
- Multi-component mapping, result import, and Results/Validation v4 are the next
  milestone, not hidden MVP scope.
- Design comparison is pinned to
  [`DIGI-UW/openelis-work@4c0e1a28`](https://github.com/DIGI-UW/openelis-work/tree/4c0e1a28/designs),
  including Style Guide v2.3.

## Current Classification

| Milestone | State | Current position |
| --- | --- | --- |
| M0 - stabilization | Implemented foundation | Routed QC loading, string-safe lot payloads, deterministic bridge collections, and FILE ownership pass the 875-test analyzer/QC package gate. |
| M1 - profile verification | Partially accepted | Shipped-profile summaries, setup actions, URL-backed filters, shared semantic page shell, and Carbon tables have focused coverage. Responsive screenshot proof remains. |
| M2 - guided setup | Partially accepted | Inline creation, exact-once profile application, canonical four-step routes, browser state, contextual detours, and Review are implemented and covered below E2E. Remote UI proof remains. |
| M3 - mappings/result values | Implemented foundation | Catalog-bound result options, legacy-unbound state, pending resolution, fingerprints, audit, and reusable Carbon mapping tables exist. Remote UI proof remains. |
| M4 - analyzer QC | Implemented foundation | Existing QC entities, readiness, bridge sync, blocker presentation, and detour returns pass focused and package tests. Remote validation remains. |
| C4 - remote UAT/evidence | Not accepted | Previous videos and reports are historical. A fresh exact-build UI-only run is required after C1-C3. |

## Requirement Matrix

| Requirement | Code surface | Automated owner | Design reference | UAT step | State |
| --- | --- | --- | --- | --- | --- |
| Searchable shipped-profile catalog | `AnalyzerRestController`, `AnalyzerTypeManagement` | JUnit + RTL | analyzer integration/profile designs | `AN-QC-001` | Automated; remote pending |
| Inline profile-driven creation | `AnalyzersList`, `AnalyzerForm` | JUnit + RTL + Playwright | setup wizard/form patterns | `AN-QC-002` | Automated below E2E; remote pending |
| Deterministic mapping verification | `FieldMapping`, plugin-config/verification services | JUnit + RTL + Playwright | mapping/data-table patterns | `AN-QC-003` | Automated below E2E; remote pending |
| Visible connection test | `AnalyzerForm`, connection endpoint | RTL + Playwright | form/notification patterns | `AN-QC-004` | Automated below E2E; remote pending |
| Catalog-bound qualitative resolution | result-option/pending-value services and panel | JUnit + RTL + Playwright | result-option designs | `AN-QC-005` | Foundation complete |
| Existing-model QC setup | rule builder, control lots, readiness services | JUnit + RTL + Playwright | Westgard/QC designs | `AN-QC-006` | Partial |
| Explained activation readiness | verification/readiness services and Review step | JUnit + RTL + Playwright | status/notification patterns | `AN-QC-007` | Partial |
| Coherent lab-facing review | four setup routes, `AnalyzerSetupReview`, shared page shell | RTL + Playwright | Style Guide v2.3 | `AN-QC-008` | Automated below E2E; remote pending |
| Deterministic bridge contract | bridge registration service | JUnit/contract tests | ownership contract | not human UAT | Foundation complete |
| Build-bound review provenance | review-tooling schema v2/build manifest | harness tests | UAT harness contract | all | External dependency |

## Acceptance Checkpoints

### C0 - Reconcile baseline and specifications

1. Rebase the branch onto current `develop`.
2. Run focused backend/frontend baselines.
3. Make the OGC-1054 spec, route contract, checklist, and this roadmap agree.
4. Reconcile legacy Feature 004/012 contracts and QC/FILE references.
5. Replace the PR title/body with current-head status.

**Exit:** no active document or PR claim treats historical API routes, mocks,
screenshots, or video as final acceptance.

### C1 - URL state and Carbon page shell

1. Add pure, tested analyzer route/query helpers.
2. Make list/catalog search and filters bookmarkable and browser-history safe.
3. Add a shared semantic Carbon page header and linked breadcrumbs.
4. Preserve validated `returnTo` through analyzer/QC actions.
5. Remove the analyzer/QC `PageTitle` legacy pathway.

**Exit:** AC-1054-01 through AC-1054-03 and AC-1054-11 pass focused tests.

### C2 - Guided setup

1. Implement Instrument → Verify → Connect → Review as canonical URLs.
2. Navigate from creation using the server-returned analyzer ID.
3. Preserve selected-profile context without reapplying it during edit.
4. Keep QC rule/control-lot detours inside the setup story.
5. Explain current/stale verification and readiness on Review.

**Exit:** AC-1054-04 through AC-1054-06 and AC-1054-12/13 pass focused tests.

### C3 - Carbon and responsive remediation

1. Convert profile/mapping tabular surfaces to reusable Carbon composition.
2. Use accessible Carbon status, overflow/action, notification, form, and
   progress patterns.
3. Keep actions reachable and content non-overlapping at desktop and mobile
   viewports.
4. Compare implementation screenshots with the pinned designs and record
   explicit deferred differences.

**Exit:** focused accessibility/component tests pass and inspected screenshots
show no incoherent overflow or overlap.

### C4 - Acceptance closure

1. Run targeted and package-level JUnit 4 analyzer/QC suites.
2. Run focused and package-level RTL/Vitest, format, lint, and Playwright guard.
3. Run `digi-uw/code-qa` alignment, coverage, simplicity, cross-repo, and
   evidence gates.
4. Deploy the exact PR SHA to
   [analyzers.openelis-global.org](https://analyzers.openelis-global.org/login).
5. Verify the Grist overlay revision and stable required steps.
6. Run UI-only non-video evidence, inspect console/screenshots/trace/runtime,
   then record the MP4.
7. Attach build metadata, checklist revision, report, screenshots, and video to
   the PR evidence record.

**Exit:** AC-1054-07 through AC-1054-16 pass; all required Grist steps pass
against the exact recorded build; PR #3792 is green, non-draft, and mergeable.

## Required Grist Steps

1. `AN-QC-001` - Find and inspect a shipped profile.
2. `AN-QC-002` - Create an analyzer through inline Instrument setup.
3. `AN-QC-003` - Review and verify deterministic test/QC mappings.
4. `AN-QC-005` - Resolve a pending qualitative value with a valid catalog
   option.
5. `AN-QC-006` - Add/select an active QC rule and control lot.
6. `AN-QC-007` - Observe blocked then satisfied readiness.
7. `AN-QC-004` - Enter connection settings and observe a connection-test result.
8. `AN-QC-008` - Review the completed analyzer for lab-facing clarity.

This ordering follows the implemented four-step story while retaining stable
step keys. Grist is the reviewer-authored source; repository fixtures are a
tested bootstrap/example only.

## Evidence Policy

The deleted June evidence note and prior recordings remain available through
git history. They cannot accept the current branch. Current evidence will be
recorded in `ogc-1054-analyzer-qc-config-mvp-evidence-2026-07-28.md` only after
the exact deployed application/harness SHAs and checklist revision are known.

Fixture loading may establish preconditions. The acceptance story itself uses
visible UI controls and assertions only. It must not use `page.request`, API
assertions, backend polling, forced controls, or arbitrary waits.

## Follow-On Milestone

After PR #3792, create a separate result-import milestone for multi-component
target-to-component mapping and Results/Validation v4. Use stable component
codes, preserve the primary-component default, and request bridge changes only
when OpenELIS contract evidence demonstrates loss of target identity.
