# Tasks: OGC-1054 Analyzer Management

**Control:** The [roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
selects the only active iteration. This file defines dependency order inside an
iteration; it does not maintain a second progress ledger.

At each checkpoint boundary, apply the roadmap's marker and merge rules: make
the completed implementation `[x]`, start its immediate successor as `[*]`, and
submit the completed PRs for review. Review leaves the predecessor `[x]`; merge
changes it to `[✓]` in the rebased active descendant. No task below repeats that
state bookkeeping.

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
7. Record the resolved QC boundary: Bridge profile control recognition,
   independent OpenELIS operational QC, exact non-QC activation predicates, and
   one-way `AnalyzerQcRule`/fallback removal.
8. Add the grounded profile-system remediation report and freeze the
   established two-job Bridge profile model, profile curation rules, and
   GeneXpert/Fluoro compatibility gates across every canonical artifact.
9. Run the requirements checklist, cross-artifact consistency analysis,
   formatting, and link checks; advance markers one checkpoint at a time under
   the roadmap rule while preserving exactly one `[*]` active checkpoint.
10. Keep R0 current and address review. After R0 merges, rebase F0 on current
    `develop`, record R0 as `[✓]`, and rerun F0 gates before F0 review.

## F0 - Foundation Salvage

1. Select any #3792 behavior directly from current code and Git history, and
   explain the decision in the F0 PR without creating a duplicate manifest.
2. Write or verify characterization tests before retaining behavior.
3. Retain only behavior compatible with Bridge runtime ownership and one
   lab-facing workflow.
4. Guard Playwright against API-driven acceptance and remove stale evidence
   claims.
5. Add established GeneXpert ASTM and FluoroCycler fixtures that fail when a
   contract loses profile-owned communication or OE instance defaults.
6. Add guards against a second profile model, selected-profile frontend
   defaults, `AnalyzerQcRule`, and an OE FILE poller.
7. Run the F0 exit gate and submit the PR for review.

## E0 - Contracts And Migration

1. Complete the ownership/persistence ADR from the working OE/Bridge/mock flow,
   explicitly preserving the established profile's communication and instance-
   default jobs.
2. Write failing producer/consumer fixtures for complete GeneXpert ASTM and
   FluoroCycler profiles, pinned revisions, `controlResultRecognition`
   `RULES`/`NONE`, registration without operational QC, known/unknown traffic,
   and FILE messages.
3. Replace the thin profile schema with a strict additive evolution of the
   established contract and semantic validation.
4. Produce evidence-based retain/correct/alias/split/remove dispositions for all
   20 profiles; do not mechanically preserve rows or create `LEGACY_UNBOUND`.
5. Define immutable revision retention, revision-scoped site bindings,
   activation fingerprints, exact Bridge acknowledgment, and a clean OE
   consumer that stores a pin rather than a profile snapshot.
6. Define and test one-way removal of `defaultConfigId`, copied plugin/profile
   state, OE profile serving/application, `AnalyzerQcRule`, and hidden Bridge
   fallbacks after parity. Add no runtime migration adapter or dual writer.
7. Run the paired Bridge/OE exit gate and submit the corrected checkpoint PRs.

## M1 - Analyzer Types

**Acceptance:** `MVP-001` through `MVP-004`, plus the M1 portions of
`MVP-011` and `MVP-022`.

1. Complete Bridge profile lifecycle tests before lifecycle implementation.
2. Complete OE composition, persistence, defaults, and removal tests before production
   changes.
3. Rewire the catalog to the corrected E0 contract and ship the curated 20
   profiles from Bridge.
4. Prove GeneXpert/Fluoro profile defaults, registration/runtime, and mock
   traffic retain parity; accepted distinct results remain distinct and proven
   aliases are not duplicated.
5. Implement lab-safe Create/Duplicate/Update/Publish, lineage, pinned revisions,
   lifecycle, completeness, usage, and attention state, including the explainer
   and aggregate counts.
6. Fetch and apply the selected revision's actual defaults in OE; frontend
   protocol/communication constants cannot act as profile defaults.
7. Implement URL-backed list/detail state and breadcrumbs using reusable Carbon
   components.
8. Remove the authoritative OE filesystem/copy/application path after assembled
   parity.
9. Run focused, broad, runtime, RTL real-router, and visible browser gates.
10. Run the paired Bridge/OE exit gate and submit the checkpoint PRs for review.

## M2 - Mapping

**Acceptance:** `MVP-005` through `MVP-009`, `MVP-012`, plus the M2 portions of
`MVP-011` and `MVP-022`.

1. Write failing contract and service tests for independent source rows and
   complete catalog lookup.
2. Write failing ownership tests for qualitative Result Options.
3. Write failing Bridge tests for `RULES`/`NONE`, multiple-rule OR behavior,
   required `NONE` author affirmation, undocumented/invalid profile
   combinations, non-match, and no hard-coded fallback; write OE tests for
   human-readable confirmation and exact stale triggers.
4. Write RTL real-router tests for add/edit/remove/repoint, catalog return, and
   Duplicate Profile/update-shared scope.
5. Implement the sole protocol-neutral editor in Analyzer Types; make Verify
   link to it with a return URL and remove duplicate/per-analyzer editors and
   queues.
6. Remove the `AnalyzerQcRule` production editor/routes/controller/service/DAO
   callers, profile seeding, registration fields, readiness checks,
   translations, writes, entity, tests for superseded behavior, and schema.
7. Prove an explicit exclusion does not block independent rows or pretend to be
   a mapped result.
8. Run the ASTM, HL7, and FILE exit gate and submit the checkpoint PRs for
   review.

## M3 - Guided Setup And Linked Operational QC

**Acceptance:** `MVP-010` through `MVP-017`, completing the M3 portions of
cross-cutting `MVP-011`, `MVP-012`, and `MVP-022`.

1. Write failing backend tests for setup verification/audit, each exact
   activation predicate on initial activation and re-entry from error/offline,
   draft-versus-active candidate isolation, operational-QC independence, and
   pinned Bridge acknowledgment matching.
2. Write failing Bridge probe/capability tests.
3. Write RTL real-router tests for Instrument, Verify, Connect, separate
   Analyzer Types create/Duplicate Profile return, the completion
   summary, breadcrumbs, URL state, history, and reload.
4. Write integration/RTL tests for the analyzer-scoped canonical Quality Control
   link and prove valid/invalid operational-QC changes do not alter verification
   or activation blockers.
5. Implement the unified Carbon workflow with no developer fields or duplicate
   setup path.
6. Run a focused visible browser story and inspect desktop/mobile captures.
7. Run the M3 exit gate and submit the checkpoint PRs for review.

## M4 - Safe Traffic

**Acceptance:** `MVP-018` through `MVP-023`, plus final assembled proof for
`MVP-017` and every earlier criterion.

1. Write failing producer/consumer tests for known patient, recognized control,
   nonmatching control, explicit `NONE`, unknown test/value, and FILE traffic
   with preserved raw context.
2. Add deterministic real-transport mock fixtures.
3. Write failing OE integration tests for hold, alert, catalog-safe resolution,
   audit, and deterministic next-message behavior.
4. Write failing integration and UI tests for live Verify reconciliation and
   draft-type population from held traffic.
5. Implement live capture plus the visible Alerts/Needs attention flow.
6. Remove every remaining raw reader, copied-profile writer, dual writer,
   duplicate editor/queue, Bridge classifier fallback, and alternate acceptance
   path. `AnalyzerQcRule` must already be absent from M2.
7. Write and audit the complete UI-only Playwright story.
8. Run the M4 exit gate and submit the checkpoint PRs for review.

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
9. Run the G0 exit gate and submit the accepted candidate for review.

## R1.1 - Mature Alert Operations

1. Write failing tests for triage, assignment, acknowledgement, concurrency,
   and durable navigation.
2. Implement the queue and conflict-safe resolution workflow.
3. Prove no held item is lost or double-resolved, run the R1.1 exit gate, and
   submit for review.

## R1.2 - Profile Revision And Distribution Operations

1. Write failing Bridge/OE contracts for diff, selective update, rollback,
   stale verification, and affected-analyzer reporting.
2. Implement the revision workflow, faithful backup/support export, and
   migration protections.
3. Validate export, distribution, and rollback, run the R1.2 exit gate, and
   submit for review.

## R1-G - Full-Feature Acceptance

Deploy one exact candidate, publish the separately versioned Grist story, run
and inspect the complete non-video UI story, then record MP4 and obtain human
acceptance for the unchanged build.

## R2 - Operational Rollout

Execute the scale, resilience, security, monitoring, documentation, site, and
per-instrument validation blocks defined by the roadmap after R1-G is finished.
