# Analyzer QC + Configuration Implementation Roadmap

- **Date:** 2026-06-28
- **Status:** Code-rooted brief for review
- **Primary epic:** [OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054)
- **Related epics:** [OGC-1016](https://uwdigi.atlassian.net/browse/OGC-1016),
  [OGC-811](https://uwdigi.atlassian.net/browse/OGC-811),
  [OGC-817](https://uwdigi.atlassian.net/browse/OGC-817),
  [OGC-426](https://uwdigi.atlassian.net/browse/OGC-426),
  [OGC-427](https://uwdigi.atlassian.net/browse/OGC-427),
  [OGC-428](https://uwdigi.atlassian.net/browse/OGC-428)

This brief turns the current analyzer/QC direction into an implementation
roadmap. It is intentionally grounded in the current OpenELIS code, with Jira,
Slack, `digi-uw/openelis-work`, and `digi-uw/code-qa` used as planning signals.

## Executive Decision

The current implementation should build forward from the generic analyzer/profile
and `QCResult`/Westgard code already in this repo. The old per-instrument adapter
stories are no longer the implementation model for ASTM/HL7/FILE instruments that
fit the generic profile system. They remain useful only as profile/spec inputs.

Implementation center of gravity:

1. `OGC-1054` is the current analyzer setup/mapping epic. The user-facing object is
   **Analyzer Type**; the code-facing object can remain "profile" where the
   current subsystem already uses that language.
2. Analyzer-specific work should usually be a profile/catalog task, not a new
   OpenELIS adapter. Dedicated code is reserved for true non-generic patterns
   such as proprietary serial/BCI or pipeline imports that cannot be expressed as
   profiles.
3. Analyzer QC should use `QCResult`, `QCControlLot`, `AnalyzerQcRule`, and
   Westgard evaluation. Do not implement or extend a `QcRun` pathway for analyzer
   QC.
4. The bridge owns analyzer runtime transport: socket listeners, directory/file
   watching, polling, retry, parser/runtime state, and operational dead-letter
   handling. OpenELIS owns configuration, profile application, bridge
   registration/sync, the direct ingestion endpoint, QC/result persistence, and
   review UI.
5. No OpenELIS application-side FILE poller belongs in this workstream. If FILE
   runtime behavior is missing or unreliable, remediation belongs on the bridge
   side; OpenELIS remains the configuration, registration, ingest, result, and QC
   owner.

## Current Signals

**Jira as of 2026-06-28**

| Signal | Meaning for implementation |
| --- | --- |
| `OGC-1054` created 2026-06-17, Ready by 2026-06-25 | Current reset point for analyzer type/profile mapping. Its scope replaces the old developer-facing Analyzer Types page with lab-facing profile verification and mapping. |
| `OGC-1016`, `OGC-811`, `OGC-817` Ready and updated June 25-26 | Current Results/Validation v4 spine. Analyzer work must feed Method/Analyzer split, instrument flags, QC fail chips/signals, and Result & Validation Configuration rather than inventing a parallel review surface. |
| `OGC-427` and `OGC-428` still mention `QcRun` | Treat as stale wording to be cleaned up. Implementation should align those stories to `QCResult`/manual-control persistence and the v4 Results/Validation surfaces. |
| Old analyzer issues updated in June, e.g. MinION/GeneXpert/Mindray | Use them as profile/spec/design inputs only unless they are explicit non-generic implementation patterns. |

**Slack signal**

Casey's June 2026 handoff points at `OGC-1054` as the refreshed final analyzer
phase and explicitly frames the old analyzer stories as things that should be
closed or superseded. The practical read is: stop sizing per-analyzer
implementation tickets first; implement the reusable profile verification,
mapping, QC, and learn-from-traffic path.

**`digi-uw/openelis-work` signal**

Current design sources are:

- `designs/analyzer-integration/analyzer-profile-mapping.md`
- `designs/analyzer-integration/analyzer-profile-mapping-gap-analysis.md`
- `designs/results-validation/results-validation-v4-breakdown.md`
- `designs/admin-config/results-validation-config-v4.md`
- `designs/results-validation/results-entry-v4.md`
- `designs/results-validation/validation-page-v4.md`
- `designs/quality/westgard-rules.md`
- `designs/quality/analyzer-manual-qc.md`
- `designs/quality/batch-workplan-reagent-qc.md`

The design-gallery direction is clear: profile verification and mapping should be
lab-facing, deterministic, and profile-based; Results/Validation v4 is the place
where analyzer method, analyzer instance, instrument flags, and QC fail signals
surface.

**`digi-uw/code-qa` signal**

Use these skills as required gates for implementation:

- `spec-code-alignment`: code is the ground truth for what ships; update specs
  when docs lag confirmed code decisions, but do not rewrite requirements around
  shortcuts.
- `meaningful-test-coverage`: every slice must start with tests at the right
  layer, and at least one load-bearing guard must be proven to fail against the
  old behavior.
- `simplicity-review`: delete legacy/duplicated pathways in the same PR as the
  replacement, and cut speculative abstractions.
- `cross-repo-companion-pr`: bridge/config/profile changes must degrade cleanly
  and be safe to merge independently when they span repos.
- `evidence-bundle`: E2E/demo proof is packaged for review; MP4/PNG artifacts are
  not committed.

**Code-qa pass applied to this brief**

| Gate | How this roadmap applies it |
| --- | --- |
| `spec-code-alignment` | Code anchors are listed before the milestone plan, and drift is called out explicitly where Jira/design text lags current code (`QcRun`, developer-facing Analyzer Types, FILE ownership). |
| `meaningful-test-coverage` | Every milestone starts with tests at the layer where the bug would actually fail. Load-bearing guards name the old behavior they must catch. |
| `simplicity-review` | Milestones extend existing analyzer, plugin-config, bridge-registration, and QC services instead of inventing parallel subsystems. Legacy removal is an acceptance gate, not a follow-up wish. |
| `cross-repo-companion-pr` | Bridge work is isolated in M6 with merge-order safety and degradation requirements for paired PRs. |
| `evidence-bundle` | Browser-visible flows require Playwright/demo evidence packaging before review-ready status. |

## Code Baseline

The implementation already has the bones we should extend:

| Area | Current code anchor | Roadmap implication |
| --- | --- | --- |
| Profile catalog APIs | `AnalyzerRestController` exposes read-only `/rest/analyzer/profiles` and profile detail endpoints; create accepts `defaultConfigId` | Keep built-in profiles read-only. Add explicit analyzer-type/profile management over the current model instead of reintroducing developer-only type editing as the main path. |
| Profile apply | `AnalyzerServiceImpl.autoCreateTestMappings` applies `configDefaults`, `qcRules`, and `default_test_mappings` | Build deterministic verification around this path. Tests must lock down profile defaults, QC-rule creation, LOINC mapping, and missing-catalog behavior. |
| Analyzer status | `AnalyzerStatusTransitionServiceImpl` blocks generic analyzers from `VALIDATION -> ACTIVE` without active QC rules | Preserve and expose this as a setup readiness gate. If the UI says Ready/Active, it must mean mappings + QC readiness are true. |
| Plugin config | `AnalyzerPluginConfigRestController` and `AnalyzerPluginConfigServiceImpl` own JSON config and pending codes | Extend this to cover pending result values and learn-from-traffic; do not create a second pending queue. |
| QC rules | `AnalyzerQcRuleRestController` writes analyzer QC rules and syncs bridge registration | Keep QC-rule edits authoritative in OpenELIS and sync to bridge. Add tests proving bridge payloads clear stale `qcRules`/`controlLots` with empty arrays. |
| Bridge registration | `BridgeRegistrationService` registers TCP/FILE analyzers and attaches `qcRules`, `controlLots`, and test-code/LOINC data | This is the OE-owned bridge contract. Bridge work should consume this contract; OpenELIS should not own watcher/poller runtime. |
| FILE setup | `FileImportServiceImpl` persists FILE profile config and registers the bridge watch directory | Keep FILE setup/config here, but leave directory watching and file movement to bridge. |
| QC processing | `QCResultProcessingServiceImpl` sends QC observations to `QCResultService.createQCResult` | This is the analyzer QC ingestion path. Manual QC should converge here where possible, not create a `QcRun` island. |
| QC persistence/evaluation | `QCResultServiceImpl` persists z-score, stats bootstrap, and publishes `QCResultCreatedEvent`; `QCRestController`, `QCChartDataRestController`, `QCViolationRestController` expose QC APIs | Westgard/QC dashboards already exist. Analyzer roadmap should fill gaps and integrate with v4 Results/Validation. |
| Current Analyzer Types UI | `/analyzers/types` routes to `AnalyzerTypeManagement`, a developer-facing plugin registry | Replace or demote this as the primary route for lab admins. Keep plugin registry only as Advanced/implementer UI if still needed. |
| Current Add Analyzer route | `/analyzers/new` routes to a standalone analyzer form | The current design says setup is inline from `/analyzers`; once the guided flow lands, redirect or retire the old route so there is one setup authority. |
| Mapping UI | `/analyzers/:id/mappings` routes to `FieldMapping` | Rework into the verification/mapping editor rather than adding another mapping surface. |
| QC rule UI | `/analyzers/:id/qc-rules` routes to `QcRuleBuilderModal` | Stabilization needed: current routed component references an undefined `open` variable. |
| Control lot UI | `ControlLotSetup.jsx` parses analyzer/test IDs as integers | IDs should remain strings; fix before depending on this screen for analyzer QC setup. |

## Non-Negotiable Legacy Removal

These are not optional cleanup chores. They are acceptance criteria for the
roadmap.

| Legacy / drift | Required outcome |
| --- | --- |
| Developer-facing Analyzer Types as the main lab admin workflow | Replaced by lab-facing Analyzer Type/Profile list and verification. Any plugin registry remains Advanced/implementer-only, not the primary `/analyzers/types` experience. |
| `QcRun` analyzer-QC path in old tickets | Removed from implementation scope. Update Jira/spec text or file follow-up cleanup so `QCResult`/control persistence is the only analyzer QC direction. |
| OpenELIS FILE polling/watching | Not implemented. OE configures and registers; bridge watches/transports. |
| Per-instrument code paths for generic ASTM/HL7/FILE analyzers | No new adapter code when a profile can express the instrument. Add/fix profiles and test fixtures instead. |
| Duplicate pending-code/pending-value stores | Extend the existing analyzer plugin config/pending-code model or explicitly migrate it. Do not ship two unresolved-item queues. |
| Raw developer config shown as the main mapping UX | Remove from the primary admin flow once the guided verification editor exists. |
| Old route retained as a second editor | Redirect/deprecate rather than leaving two authoritative editors for the same setting. |
| Standalone `/analyzers/new` setup flow after inline setup ships | Redirect/deprecate so Add Analyzer has one authoritative implementation. |

## Ownership Boundary

| Concern | OpenELIS | Bridge |
| --- | --- | --- |
| Analyzer/profile catalog | Owns read, apply, verify, fork/export, catalog binding | No ownership |
| Test/result/QC mappings | Owns authoring, validation, persistence, audit | Consumes for classification/routing where needed |
| TCP/MLLP/ASTM listener runtime | Registers intended config only | Owns socket listener/client runtime, connection state, retry, framing |
| FILE import | Owns watched-folder config and registration | Owns watch/poll/move/archive/dead-letter runtime |
| Parsing/classification | Owns final result/QC ingestion and persistence | Owns transport/parser runtime where bridge-side profile support exists; sends OE normalized payloads |
| QC result persistence | Owns `QCResult`, control lots, stats, Westgard events | May pre-classify QC vs patient, but does not persist LIS QC state |
| Operational status | Displays bridge-reported state | Owns runtime state and reports it |

## TDD Roadmap

Each milestone is one reviewable PR unless a slice proves too large and needs to
split. Every slice follows red/green/refactor and includes the code-qa gates.

### M0. Baseline Alignment + Stabilization

**Goal:** make the current code safe enough to build on and stop obvious drift.

**Tests first**

- Frontend component test proving `QcRuleBuilderModal` can mount as a routed page
  and load QC rules without an `open` prop.
- Frontend test proving `ControlLotSetup` submits analyzer/test IDs as strings.
- Backend test around `BridgeRegistrationService` proving an analyzer with no
  active QC rules/control lots still emits empty `qcRules` and `controlLots`
  arrays during registration/sync.

**Implementation**

- Fix `QcRuleBuilderModal` routed lifecycle.
- Preserve string IDs in control-lot setup.
- Add/adjust bridge-registration tests around empty array semantics if missing.
- Add a docs/Jira cleanup note that `QcRun` wording is stale for analyzer QC.

**Legacy gate**

- No new UI or backend path is added; this milestone only removes blockers and
  locks current contracts.

**Validation**

- Run targeted frontend tests for the touched components.
- Run targeted JUnit tests for bridge registration.
- Run `digi-uw/code-qa` `meaningful-test-coverage` manually against the slice:
  each test must fail on the current buggy behavior.

### M1. Analyzer Type/Profile Verification List

**Goal:** replace the lab-facing role of `/analyzers/types` with profile
verification status rather than plugin registry maintenance.

**Tests first**

- Backend tests for profile listing shape, read-only profile endpoints, and
  completeness calculation from `default_test_mappings`, result mappings, and QC
  rules.
- Frontend tests for searchable/filterable Analyzer Types list, deactivated
  filter state, mapped-count rendering, and no Java/plugin-class fields in the
  primary lab UI.
- Route-level test that plugin registry is not the default lab admin experience.

**Implementation**

- Add/extend DTOs so profile list can report tests mapped, results mapped, QC
  readiness, used-by count, and source (shipped/site-created when available).
- Rework `AnalyzerTypeManagement` into lab-facing profile management.
- Keep plugin registry as Advanced/implementer UI only if still required.

**Legacy gate**

- The old developer registry cannot remain as the main `/analyzers/types`
  surface.

**Validation**

- JUnit DTO/service tests.
- Vitest/RTL for the route.
- `spec-code-alignment`: compare against `analyzer-profile-mapping.md`; document
  any deliberate code-first deviations.

### M2. Guided Analyzer Setup + Deterministic Profile Apply

**Goal:** instrument-first setup where verify is the normal path and hand-edit is
the exception.

**Tests first**

- Backend integration/service tests proving profile apply:
  - applies `configDefaults`;
  - creates QC rules from `configDefaults.qcRules`;
  - maps tests by deterministic active-test LOINC match;
  - records missing/unmatched tests without silently activating them;
  - keeps `ACTIVE` blocked until mapping/QC readiness is satisfied.
- Frontend tests for the Instrument -> Verify -> Connect flow, including "not in
  catalog" rows, confirm-all, per-row resolve, and connection test result states.
- Contract test for `defaultConfigId` create semantics.

**Implementation**

- Build guided setup over the existing create/profile apply endpoint where
  possible.
- Move the user-facing Add Analyzer entry into the `/analyzers` inline flow and
  redirect/deprecate `/analyzers/new` after parity is proven.
- Add missing readiness/status DTOs rather than duplicating business logic in the
  frontend.
- Wire bridge test/connectivity through existing bridge endpoints; do not open
  direct sockets from OpenELIS.

**Legacy gate**

- No app-side direct socket/path is added.
- No per-analyzer implementation branches for generic analyzers.

**Validation**

- Targeted JUnit service/integration tests.
- Frontend unit/component tests.
- Playwright smoke for "select profile -> verify mappings/QC -> connect".
- `meaningful-test-coverage`: prove the LOINC/QC readiness guard fails against
  a reverted/missing mapping path.

### M3. Result Value Mapping + Learn-From-Traffic

**Goal:** complete the hard part of qualitative analyzers: analyzer result values
map to a selected test's own result options and unmapped values never disappear.

**Tests first**

- Backend tests for pending code + pending result value creation, resolution, and
  profile update.
- Tests proving unmapped values are accepted/held and surfaced for review, not
  written as silent raw patient results.
- Frontend tests for value-map editor:
  - target options come from the matched catalog test;
  - empty result-option state points to Test Catalog;
  - completeness `X/Y` updates after resolution.
- Analyzer Results review test showing unmapped code/value flags are visible.

**Implementation**

- Extend existing pending-code/config services to also cover pending result
  values, unless code review proves a migration is needed.
- Add result-value mapping model/API over the profile/plugin config path.
- Update `FieldMapping` into the shared verification/mapping editor for analyzer
  instances and profile context.

**Legacy gate**

- Do not introduce a second unresolved-value queue.
- Remove raw config snapshots from the primary admin experience after the proper
  editor lands.

**Validation**

- Backend tests at service/integration level for unresolved-item persistence.
- Frontend component tests for editor flows.
- Playwright case that sends/loads an unmapped value and resolves it.
- `simplicity-review`: make sure the mapping model is the smallest shape that
  supports current profile needs and not a speculative profile marketplace.

### M4. Analyzer QC Setup Completion

**Goal:** make QC setup for analyzers complete and operational, using
`QCResult`/control lots/Westgard only.

**Tests first**

- Backend tests for profile-created QC rules, control-lot resolution, and
  `QCResultProcessingServiceImpl` lot disambiguation.
- Backend tests proving active analyzer activation requires active QC rules where
  configured.
- Frontend tests for analyzer QC rule setup, control lot selection, string ID
  persistence, and visible readiness.
- Westgard service tests for `QCResultCreatedEvent` evaluation after QC result
  creation.

**Implementation**

- Finish analyzer QC rule/control lot UX integration.
- Ensure bridge registration sync is triggered after QC rule/control lot changes.
- Add status/readiness fields so analyzer setup clearly says what is missing.
- Tie analyzer QC state to existing QC dashboard/chart/violation endpoints.

**Legacy gate**

- No `QcRun` table/entity/use case for analyzer QC.
- If manual QC persistence is required for `OGC-428`, model it as
  `QCResult`/control persistence or a clearly shared control-result dependency
  that feeds Results/Validation v4.

**Validation**

- Targeted QC service tests.
- Playwright smoke: create/control lot -> QC rule -> ingest QC observation ->
  violation/dashboard signal.
- `spec-code-alignment`: update `OGC-41-westgard-qc` docs if they lag the
  current `QCResult` implementation.

### M5. Results/Validation v4 Integration Hooks

**Goal:** analyzer work feeds the current Results/Validation surfaces instead of
creating another review universe.

**Tests first**

- Backend tests that imported analyzer results populate or expose
  `Analysis.method`/`Analysis.analyzerId` or the equivalent current model needed
  by v4.
- Frontend tests for Results Entry and Validation surfaces showing Method,
  Analyzer instance, instrument flags, and QC fail chips/signals.
- Validation lane tests proving QC-failed/analyzer-flagged results are not bulk
  released as "clear".

**Implementation**

- Add adapter/DTO fields needed by `OGC-811`/`OGC-817`.
- Map analyzer QC fail/instrument flags into v4 signal vocabulary.
- Wire Result & Validation Configuration flags where behavior is controlled by
  `OGC-1016`.

**Legacy gate**

- No standalone analyzer-only validation release rules if v4 owns the release
  policy.

**Validation**

- Backend DTO/service tests.
- Frontend route/component tests.
- Playwright smoke for a flagged analyzer result visible on Results and
  Validation.

### M6. Bridge Contract Lane

**Goal:** decide and implement only the bridge-side work that belongs on bridge.

**Bridge-side scope candidates**

- Durable bridge runtime state or idempotent registry recovery if current bridge
  state is still ephemeral.
- FILE watcher/poller runtime, move/archive/error directories, dead-letter
  handling, retry, and bridge-visible path validation.
- Parser/runtime use of OE-pushed `qcRules`, `controlLots`, and test/LOINC data
  for QC/patient classification.
- Operational status reporting back to OE.

**OpenELIS-side scope**

- Registration payload shape and tests.
- Sync triggers on create/update/QC config/control lot change/startup and any
  agreed periodic reconciliation.
- UI display of bridge-reported state.

**Tests first**

- Cross-repo contract tests over `/api/analyzers/register`, `/api/analyzers/sync`,
  direct ingest, and status APIs.
- Primary-without-companion and companion-without-primary checks per
  `cross-repo-companion-pr`.

**Implementation**

- Open paired PRs only when both repos need changes.
- Ensure each PR degrades safely without the other merged first.

**Legacy gate**

- No OE-side watcher is introduced as a shortcut for missing bridge runtime.

**Validation**

- Contract tests in both repos.
- End-to-end analyzer harness for at least one TCP and one FILE profile.
- `evidence-bundle` for reviewer proof when the flow reaches browser-visible
  review.

### M7. Final QA, Evidence, and Release Readiness

**Goal:** prove the implementation and documentation are coherent.

**Validation checklist**

- `spec-code-alignment`: inventory every requirement in this brief, `OGC-1054`,
  `OGC-1016`, `OGC-811`, `OGC-817`, and `OGC-41-westgard-qc` against shipped code.
- `meaningful-test-coverage`: identify the load-bearing tests per layer and prove
  at least the core guard fails on the old behavior.
- `simplicity-review`: delete leftover legacy paths, unused flags, duplicate
  queues, and speculative abstractions.
- `cross-repo-companion-pr`: verify merge-order safety for any bridge/profile
  companion PRs.
- `commit-pr-hygiene`: run repo formatters cold, stage deliberately, and keep
  PR/commit rationale out of code comments.
- `evidence-bundle`: package Playwright/demo evidence for stakeholder review.

**Minimum evidence before review-ready**

- Backend targeted tests for analyzer profile apply, QC rule/control lot sync,
  QCResult ingestion, and Results/Validation signal DTOs.
- Frontend tests for Analyzer Types/Profile list, guided setup, mapping editor,
  QC rule/control lot setup, and Results/Validation signal rendering.
- At least one Playwright happy-path from profile selection through analyzer
  result/QC signal visibility.
- At least one Playwright exception path for unmapped test or result value.
- Bridge contract tests for both TCP and FILE registration paths if bridge work is
  part of the slice.

## Ticket Cleanup / Backlog Shape

Recommended grooming:

1. Keep `OGC-1054` as the parent for Analyzer Types/Profile + Mapping work.
2. Create child stories matching M1-M3 if they do not already exist in Jira:
   Analyzer Type/Profile list, guided setup, result-value mapping/learn-from-traffic.
3. Align analyzer QC work under `OGC-426` but rewrite `OGC-428` away from
   `QcRun`; either make it a `QCResult`/control persistence story or explicitly
   mark it superseded by the analyzer QC lane.
4. Align v4 Results/Validation hooks to `OGC-1016`, `OGC-811`, and `OGC-817`.
5. Mark old per-instrument ASTM/HL7/FILE tickets as profile/spec inputs when
   their instrument can be represented by a profile.
6. Keep non-generic instrument patterns as distinct implementation stories only
   when a profile cannot express the protocol or result model.
7. Create bridge tickets only for bridge-owned runtime gaps; do not hide bridge
   scope inside OpenELIS analyzer tickets.

## Deterministic Next Steps

1. Start with M0 in the OpenELIS worktree and create a small stabilization PR.
2. In parallel, groom Jira text so `OGC-1054` child stories and analyzer QC scope
   stop pointing engineers at stale `QcRun`/per-adapter pathways.
3. After M0 is green, implement M1 and M2 as separate PRs. Do not start M3 until
   the profile/list and guided apply contracts are stable.
4. Decide bridge-lane ownership after reading the current bridge repo state, not
   from this OpenELIS worktree; `tools/openelis-analyzer-bridge` is empty here.
5. Require code-qa gates before each PR is marked ready for review.

## Review Questions

- Should the plugin registry remain reachable as an Advanced page, or should it
  be fully removed from the UI once Analyzer Types/Profile management lands?
- Should pending unmapped values resolve from both Analyzer Results review and the
  mapping editor, or only one canonical place with links from the other?
- For manual/RDT control persistence, do we represent it directly as `QCResult`
  plus typed metadata, or introduce a small shared control-result entity that
  still feeds `QCResult`/Westgard and v4 QC-fail signals?
- Which bridge repo/branch is authoritative for the next bridge contract lane?
