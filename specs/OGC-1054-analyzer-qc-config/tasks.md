# Tasks: OGC-1054 Analyzer Management

**Control:** The [roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
selects the only active iteration. This file defines dependency order inside an
iteration; it does not maintain a second progress ledger.

## R0 - Authoritative Roadmap

1. Inspect the current OE, Bridge, mock, review-tooling, design, deployment,
   Grist, branch, PR, and worktree state directly from their authoritative
   systems without creating a duplicate status ledger.
2. Preserve dirty M1 work in Git without publishing or accepting it.
3. Establish this canonical spec/plan/tasks/checklist/acceptance/UAT set.
4. Remove Jira as a requirements/status authority and freeze the
   specs-plus-`openelis-work` source boundary.
5. Mark superseded analyzer implementation plans as historical and point them
   to this artifact set.
6. Reconcile the two roadmap lineages so descendant branches inherit R0 and no
   historical branch can merge a competing roadmap; Git history retains the
   prior versions.
7. Run the requirements checklist, cross-artifact consistency analysis,
   formatting, link checks, and PR review.
8. Rebase R0 on current `develop`, rerun checks, merge R0, then change R0 to
   `[x]` and F0 to `[*]` in the canonical landed lineage.

## F0 - Foundation Salvage

1. Select any #3792 behavior directly from current code and Git history, and
   explain the decision in the F0 PR without creating a duplicate manifest.
2. Write or verify characterization tests before retaining behavior.
3. Retain only behavior compatible with Bridge runtime ownership and one
   lab-facing workflow.
4. Guard Playwright against API-driven acceptance and remove stale evidence
   claims.
5. Validate, review, merge, then advance the roadmap marker.

## E0 - Contracts And Migration

1. Complete the ownership/persistence ADR from current OE/Bridge code.
2. Write failing producer/consumer fixtures for profile, registration, known,
   unknown, QC, and FILE messages.
3. Characterize all legacy readers/writers and existing data shapes.
4. Define no-loss migration, anomaly handling, rollback, and one-writer cutover.
5. Validate paired Bridge/OE contracts, review, merge, then advance the marker.

## M1 - Analyzer Types

**Acceptance:** `MVP-001` through `MVP-004`, plus the M1 portions of
`MVP-011` and `MVP-022`.

1. Complete Bridge profile lifecycle tests before lifecycle implementation.
2. Complete OE composition, persistence, and migration tests before production
   changes.
3. Prove distinct source rows never collapse during migration.
4. Implement lab-safe create/fork, lineage, lifecycle, completeness, usage, and
   attention state, including the explainer and aggregate counts.
5. Implement URL-backed list/detail state and breadcrumbs using reusable Carbon
   components.
6. Remove or disable authoritative OpenELIS filesystem/copy writers.
7. Run focused, broad, runtime, RTL real-router, and visible browser gates.
8. Validate paired Bridge/OE PRs, review, merge, then advance the marker.

## M2 - Mapping

**Acceptance:** `MVP-005` through `MVP-009`, `MVP-012`, plus the M2 portions of
`MVP-011` and `MVP-022`.

1. Write failing contract and service tests for independent source rows and
   complete catalog lookup.
2. Write failing ownership tests for qualitative Result Options.
3. Write failing tests for QC-identification confirmation and stale mapping
   verification.
4. Write RTL real-router tests for add/edit/remove/repoint, catalog return, and
   fork/update scope.
5. Implement one protocol-neutral editor and remove duplicate editors/queues.
6. Prove an explicit exclusion does not block independent rows or pretend to be
   a mapped result.
7. Validate ASTM, HL7, and FILE criteria, review, merge, then advance the marker.

## M3 - Guided Setup And QC

**Acceptance:** `MVP-010` through `MVP-017`, completing the M3 portions of
cross-cutting `MVP-011`, `MVP-012`, and `MVP-022`.

1. Resolve `AMB-M3-001` in the functional spec and engineering contract.
2. Write failing backend tests for setup verification, readiness, audit,
   operational QC, activation, and Bridge synchronization.
3. Write failing Bridge probe/capability tests.
4. Write RTL real-router tests for Instrument, Verify, Connect, the completion
   summary, breadcrumbs, URL state, history, and reload.
5. Implement the unified Carbon workflow with no developer fields or duplicate
   setup path.
6. Run a focused visible browser story and inspect desktop/mobile captures.
7. Validate, review, merge, then advance the marker.

## M4 - Safe Traffic

**Acceptance:** `MVP-018` through `MVP-023`, plus final assembled proof for
`MVP-017` and every earlier criterion.

1. Write failing producer/consumer tests for known patient, QC, unknown test,
   unknown value, and FILE traffic with preserved raw context.
2. Add deterministic real-transport mock fixtures.
3. Write failing OE integration tests for hold, alert, catalog-safe resolution,
   audit, and deterministic next-message behavior.
4. Write failing integration and UI tests for live Verify reconciliation and
   blank-type population from held traffic.
5. Implement live capture plus the visible Alerts/Needs attention flow.
6. Remove or disable every legacy raw reader, copied-profile writer, dual
   writer, duplicate editor, and duplicate queue.
7. Write and audit the complete UI-only Playwright story.
8. Validate all prior MVP criteria, review, merge, then advance the marker.

## G0 - Remote MVP Acceptance

**Acceptance:** `MVP-024` and all 17 required `AN-MVP-*` steps for the
unchanged release candidate.

1. Deploy one immutable candidate whose target metadata and Git/submodule state
   identify every selected component.
2. Load fixtures as a separate precondition.
3. Publish and verify all 17 stable Grist steps.
4. Run non-video Playwright; inspect console, trace, runtime, and screenshots.
5. Compare desktop/mobile output with the current functional/visual design.
6. Fix failures and restart the acceptance run whenever the candidate changes.
7. Record MP4 only after the inspected non-video run is clean.
8. Obtain the completed human Grist report for the same deployment.
9. Merge the accepted candidate and advance the roadmap to R1.

## R1.1 - Mature Alert Operations

1. Write failing tests for triage, assignment, acknowledgement, concurrency,
   and durable navigation.
2. Implement the queue and conflict-safe resolution workflow.
3. Prove no held item is lost or double-resolved, review, merge, then advance
   the marker.

## R1.2 - Profile Revision And Distribution Operations

1. Write failing Bridge/OE contracts for diff, selective update, rollback,
   stale verification, and affected-analyzer reporting.
2. Implement the revision workflow, faithful backup/support export, and
   migration protections.
3. Validate export, distribution, and rollback, review, merge, then advance the
   marker.

## R1-G - Full-Feature Acceptance

Deploy one exact candidate, publish the separately versioned Grist story, run
and inspect the complete non-video UI story, then record MP4 and obtain human
acceptance for the unchanged build.

## R2 - Operational Rollout

Execute the scale, resilience, security, monitoring, documentation, site, and
per-instrument validation blocks defined by the roadmap after R1-G is finished.
