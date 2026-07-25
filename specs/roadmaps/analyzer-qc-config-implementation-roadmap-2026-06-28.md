# Analyzer QC + Configuration Implementation Roadmap

- **Date:** 2026-06-28
- **Status:** Acceptance closure in progress; the existing remote report and
  video are historical until rerun against the final PR SHA
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

## Acceptance-Closure Amendment - 2026-07-24

PR [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792) remains the
single-branch, non-draft Analyzer QC/config MVP. Its code is the implementation
foundation, not proof of final acceptance. Acceptance now requires catalog-bound
qualitative result mappings, durable and stale-aware setup verification,
deterministic QC/bridge readiness, the live Grist review contract, and a fresh
UI-only run against the exact deployed PR build.

The current design comparison is pinned to
[`DIGI-UW/openelis-work@2b590bb`](https://github.com/DIGI-UW/openelis-work/tree/2b590bb1d6ccf8a1c8217aecc8eb5662a05e72a7/designs).
The June planning dashboard and the
[June 28 evidence note](ogc-1054-analyzer-qc-config-mvp-evidence-2026-06-28.md)
are historical signals only. Neither can satisfy the July acceptance gates.

### Milestone Classification

| Milestone                       | Classification         | Acceptance position                                                                                                                                                                                           |
| ------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| M0 - stabilization              | Implemented foundation | Routed QC loading, string-safe control-lot IDs, deterministic empty bridge collections, and bridge-owned FILE runtime are implemented and covered by focused tests.                                           |
| M1 - profile verification       | Partially accepted     | The shipped-profile catalog and setup action are implemented. Richer design-gallery filters, used-by/source metadata, and list-level attention presentation are deferred and are not claimed by this MVP.     |
| M2 - guided setup               | Partially accepted     | Inline creation, lab-unit selection, old-route redirect, profile application, and saved-analyzer connection testing are implemented. The MVP uses create-then-review routes, not a single progressive wizard. |
| M3 - mappings and result values | Partially accepted     | Catalog-bound values, real pending-code resolution, and stale-aware verification pass focused and real-DB guards. Final acceptance still requires the final-SHA remote story.                                 |
| M4 - analyzer QC                | Partially accepted     | Existing QC entities, readiness gates, startup payload assembly, and all fingerprint-input guards pass locally. Final acceptance still requires the final-SHA remote story.                                   |
| C4 - remote UAT and evidence    | Not accepted           | The `fb17b576` report and MP4 are historical. A fresh UI-only non-video/video run, screenshot review, and 8/8 required report must identify the final application and harness SHAs.                           |

### Historical Evidence Position - 2026-07-24

- The prior remote run demonstrated `AN-QC-001` through `AN-QC-008` against
  application commit `fb17b57681966a31693afaedd9f9017a99a9f980` and review
  tooling commit `4d33505400ebda43d878018c7c1e0fc8f99d777c`.
- That run remains useful regression evidence but cannot accept later code or
  documentation changes.
- Its non-video run, MP4, screenshots, build manifest, checklist revision, and
  report remain packaged under `/Users/pmanko/.codex/evidence/ogc-1054-final`
  until replaced by a bundle for the final SHA.
- Review-tooling contract and widget tests pass in
  [DIGI-UW/openelis-review-tooling#2](https://github.com/DIGI-UW/openelis-review-tooling/pull/2);
  merging that companion PR remains independent of application acceptance.
- The pinned `openelis-work` mock has richer analyzer-list attention
  presentation: separate `In setup` and `Needs attention` totals, a global
  unresolved-result alert, and explicit Lab Unit/Analyzer Type columns. Those
  refinements are follow-on design scope, not hidden MVP completion claims.

### Requirement Traceability Matrix

| Requirement                                                            | Code anchor                                                                                           | Automated guard                                                                              | Current design                                                                                                                                                                   | Remote UAT                                                               | Acceptance evidence                                                                                          |
| ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| Shipped profiles are a lab-facing setup catalog                        | `AnalyzerRestController`, `AnalyzerTypeManagement`                                                    | profile DTO/controller and route component tests                                             | [`analyzer-profile-mapping.md`](https://github.com/DIGI-UW/openelis-work/blob/2b590bb1d6ccf8a1c8217aecc8eb5662a05e72a7/designs/analyzer-integration/analyzer-profile-mapping.md) | `AN-QC-001`                                                              | Final-SHA desktop/mobile screenshots and report pending                                                      |
| One inline profile-driven creation path                                | `AnalyzersList`, `AnalyzerForm`, analyzer routes                                                      | inline setup, redirect, lab-unit, and endpoint/state exact-once tests                        | same profile-mapping design; progressive wizard presentation deferred                                                                                                            | `AN-QC-002`                                                              | Final-SHA UI-only trace, screenshot, and MP4 pending                                                         |
| Deterministic mapping review records who verified what                 | `FieldMapping`, `PendingCodesPanel`, `SetupVerificationPanel`, setup/pending-code services            | real-DB pending-code mapping plus fingerprint, actor/time, audit, and all-input stale guards | same profile-mapping design and gap analysis                                                                                                                                     | `AN-QC-003`                                                              | Final-SHA UAT mark plus build-bound report pending                                                           |
| Connection testing is visible evidence, not an activation prerequisite | saved-analyzer connection-test flow                                                                   | form/component state tests and UI assertion                                                  | same profile-mapping design                                                                                                                                                      | `AN-QC-004`                                                              | Visible success or failure state in MP4                                                                      |
| Qualitative targets come from the mapped test's active catalog options | `AnalyzerResultValueOptionServiceImpl`, `AnalyzerPluginConfigServiceImpl`, `ResultValueMappingsPanel` | real-DB inactive/wrong-test inversion guard plus service/component tests                     | [`result-options.md`](https://github.com/DIGI-UW/openelis-work/blob/2b590bb1d6ccf8a1c8217aecc8eb5662a05e72a7/designs/admin-config/result-options.md)                             | `AN-QC-005`                                                              | Final-SHA reloaded UI state and downloaded report pending                                                    |
| QC uses the existing rule, lot, result, and Westgard path              | `AnalyzerQcRule`, `QCControlLot`, `QCResult`, verification and readiness services                     | create/update/delete sync, lot sync, and status-transition tests                             | [`westgard-rules.md`](https://github.com/DIGI-UW/openelis-work/blob/2b590bb1d6ccf8a1c8217aecc8eb5662a05e72a7/designs/quality/westgard-rules.md)                                  | `AN-QC-006`, `AN-QC-007`                                                 | Before/after blocker screenshots and MP4                                                                     |
| Bridge registration is deterministic                                   | `BridgeRegistrationService`                                                                           | sorted `qcRules` and `controlLots` arrays plus a sorted `testCodeLoinc` map, including empty collections | analyzer integration contract                                                                                                                                                    | `AN-QC-007` for user-visible readiness; contract tests own payload proof | JUnit output, no human payload inspection                                                                    |
| Completed setup is coherent for a lab administrator                    | `/analyzers`, `/analyzers/types`, shared mapping/QC flow                                              | focused component suite and Playwright guard                                                 | profile-mapping design                                                                                                                                                           | `AN-QC-008`                                                              | Required-step report and final screenshot                                                                    |
| Review evidence identifies exactly what was reviewed                   | `openelis-review-tooling` schema-v2 transformer, widget, router, and build manifest                   | transformer/browser/Compose/deploy checks                                                    | UAT review harness contract                                                                                                                                                      | all steps                                                                | app SHA, harness SHA, deployment time, checklist revision, route, actual URL, actor, time, status, and notes |
| FILE runtime remains bridge-owned                                      | `AGENTS.md`, bridge registration and direct ingestion code                                            | OpenELIS contract/harness tests only                                                         | analyzer integration ownership                                                                                                                                                   | not a human UAT step                                                     | Contract evidence; no OE2 poller                                                                             |

### Acceptance Checkpoints

#### C0 - Reconcile the Baseline

- Rebase `codex/ogc-1054-analyzer-qc-mvp` onto current `develop`.
- Resolve PR review comments and CI failures without widening MVP scope.
- Compare against the pinned current `openelis-work` revision and classify every
  requirement as complete, partial, absent, obsolete, or deferred.

**Gate:** the updated PR and this roadmap no longer claim acceptance from route
existence, API shape, or the historical video.

#### C1 - Make the UAT Harness Trustworthy

Deliver the review contract in a separate
[`DIGI-UW/openelis-review-tooling`](https://github.com/DIGI-UW/openelis-review-tooling)
PR:

- Grist native `/api/mcp` is the only authoring interface.
- `UAT_Steps.step_key` is stable and unique; `required` is explicit.
- Live JSON emits `schemaVersion: 2` and a deterministic
  `checklistRevision`.
- Browser answers are keyed by `step_key`, survive row reordering, and become
  stale when the reviewed instruction changes.
- Opening the panel refreshes the checklist; manual refresh and visible
  load/schema errors are available.
- `/__review/build.json` reports repository, branch, application SHA, harness
  SHA, instance, and deployment time.
- Markdown and JSON reports carry build/checklist provenance plus step key,
  route, actual URL, status, note, and marked time.
- Bootstrap preserves authored rows after initial import and deployment
  preserves host-side secrets.

**Gate:** transformer and widget tests pass, Compose/deployment definitions
validate, Grist edits appear within 30 seconds or explicit refresh, and a
fresh deployment reproduces the live harness.

#### C2 - Close Mapping and Verification Gaps

- Resolve qualitative values only through an active result option belonging to
  the mapped OpenELIS test.
- Persist `openelisResultOptionId`; derive value and label server-side.
- Keep readable free-text mappings as `LEGACY_UNBOUND`, excluded from complete
  verification.
- Record verified mapping/QC IDs, fingerprints, actor, and time in plugin config
  JSON and a durable existing audit trail.
- Make mapping or QC changes invalidate the corresponding fingerprint.
- Finish visible profile review, lab-unit selection, inline setup, mapping
  confirmation, and real saved-analyzer connection states.

**Gate:** service/component tests prove valid selection, rejection, persistence,
audit, and stale verification; `AN-QC-001` through `AN-QC-005` pass remotely.

#### C3 - Complete QC Readiness and Bridge Contracts

- Keep `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard as the only
  analyzer-QC path.
- Recompute readiness and resync registration after rule/lot changes.
- Emit deterministic `qcRules` and `controlLots` arrays plus a deterministic
  `testCodeLoinc` map, including empty collections.
- Block `ACTIVE` until mappings are currently verified and profile-applicable QC
  is ready.
- Keep connection testing visible but non-blocking.

**Gate:** backend and frontend guards pass, every blocker is explained in the
UI, and `AN-QC-006` through `AN-QC-008` pass remotely.

#### C4 - Remote UAT and Final Evidence

The required Grist steps are `AN-QC-001` through `AN-QC-008` in
`widget/examples/uat-analyzers.json` in the review-tooling repository. Run them
against [analyzers.openelis-global.org](https://analyzers.openelis-global.org/login)
using the exact PR application and harness SHAs:

1. Use fixture loading only as a precondition.
2. Execute the story through visible UI controls only.
3. Do not use `page.request`, response polling, forced controls, arbitrary waits,
   or backend assertions as substitutes for user-visible behavior.
4. Run non-video `harness-demo` first and inspect console output, screenshots,
   trace, and runtime state.
5. Compare desktop and mobile screenshots to the pinned current designs.
6. Run `harness-demo-video` and retain MP4, screenshots, results, build manifest,
   checklist revision, and downloaded Markdown/JSON report.

**Gate:** every required step passes against the recorded build. Any required
failure blocks merge and becomes a ranked Jira/GitHub draft; filing remains an
explicit action.

### Deliberate MVP Boundaries

- The shipped profile files remain the MVP source of truth. The current
  `openelis-work` full-scope design also describes shared/forkable persisted
  profiles; that is deferred and must not be inferred from this PR.
- M3 is a catalog-bound configuration and resolver milestone. The harness may
  seed an unresolved value as a test precondition, but production capture from
  analyzer result traffic belongs to the result-import follow-on. This PR does
  not claim a production learn-from-traffic ingestion path.
- Analyzer QC is applicable by default. A profile may opt out only by explicitly
  setting `qcApplicable: false`; a missing field does not silently waive QC
  readiness.
- Profile-catalog `readinessStatus` means the shipped file has enough profile
  metadata to start setup. It is not analyzer activation readiness, which is
  computed from persisted mappings, pending values, QC rules/lots, and current
  setup verification.
- `AnalyzerSetupVerifiedEvent` is the sole mapping/QC activation trigger. The
  dormant mapping-created/all-mappings-activated event path is removed; it
  cannot advance an analyzer around setup verification.
- Multi-component target-to-component mapping, analyzer result import, and
  Results/Validation v4 are the next milestone. They use stable component codes
  and preserve the primary-component default.
- `designs/quality/analyzer-manual-qc.md` still contains `QcRun` language. That
  wording is obsolete for this workstream: `AGENTS.md` and the existing QC model
  govern implementation.
- No bridge repository change is required unless an OpenELIS contract test
  demonstrates missing bridge behavior.

## MVP Branch Status

Branch `codex/ogc-1054-analyzer-qc-mvp` now carries the MVP implementation and
historical local evidence for the Analyzer QC/config slice. The branch keeps the OpenELIS
scope on analyzer configuration, profile-driven setup, mapping review, QC setup,
readiness gating, bridge registration payload contracts, and direct result/QC
processing. It does not add an OpenELIS FILE watcher/poller, does not introduce
`QcRun`, and does not require a bridge repo change for the MVP.

The required final functional evidence is a user-facing Playwright demo flow,
not an API-driven browser test. The historical recorded story verifies
`/analyzers/types`, creates
an analyzer from the shipped GeneXpert ASTM profile, reviews mappings, creates a
QC rule, creates a control lot, and returns to `/analyzers` with QC readiness
satisfied. The media bundle is generated outside the source tree at
`/Users/pmanko/.codex/evidence/ogc-1054-final`; the MP4/PNG/zip
artifacts are intentionally not committed. This historical bundle does not
satisfy C4.

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

| Signal                                                             | Meaning for implementation                                                                                                                                                                                          |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `OGC-1054` created 2026-06-17, Ready by 2026-06-25                 | Current reset point for analyzer type/profile mapping. Its scope replaces the old developer-facing Analyzer Types page with lab-facing profile verification and mapping.                                            |
| `OGC-1016`, `OGC-811`, `OGC-817` Ready and updated June 25-26      | Current Results/Validation v4 spine. Analyzer work must feed Method/Analyzer split, instrument flags, QC fail chips/signals, and Result & Validation Configuration rather than inventing a parallel review surface. |
| `OGC-427` and `OGC-428` still mention `QcRun`                      | Treat as stale wording to be cleaned up. Implementation should align those stories to `QCResult`/manual-control persistence and the v4 Results/Validation surfaces.                                                 |
| Old analyzer issues updated in June, e.g. MinION/GeneXpert/Mindray | Use them as profile/spec/design inputs only unless they are explicit non-generic implementation patterns.                                                                                                           |

**Slack signal**

Casey's June 2026 handoff points at `OGC-1054` as the refreshed final analyzer
phase and explicitly frames the old analyzer stories as things that should be
closed or superseded. The practical read is: stop sizing per-analyzer
implementation tickets first; implement the reusable profile verification,
mapping, and QC configuration path. Runtime capture from analyzer traffic stays
with the result-import follow-on.

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

| Gate                       | How this roadmap applies it                                                                                                                                                                    |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `spec-code-alignment`      | Code anchors are listed before the milestone plan, and drift is called out explicitly where Jira/design text lags current code (`QcRun`, developer-facing Analyzer Types, FILE ownership).     |
| `meaningful-test-coverage` | Every milestone starts with tests at the layer where the bug would actually fail. Load-bearing guards name the old behavior they must catch.                                                   |
| `simplicity-review`        | Milestones extend existing analyzer, plugin-config, bridge-registration, and QC services instead of inventing parallel subsystems. Legacy removal is an acceptance gate, not a follow-up wish. |
| `cross-repo-companion-pr`  | Bridge work is isolated in the follow-on Bridge Contract Lane with merge-order safety and degradation requirements for paired PRs.                                                             |
| `evidence-bundle`          | Browser-visible flows require Playwright/demo evidence packaging before review-ready status.                                                                                                   |

## Historical Code Baseline

This table records the June 28 starting point and the July acceptance-closure
result. Rows marked resolved must not be read as current implementation gaps.

| Area                       | Current code anchor                                                                                                                                                                        | Roadmap implication                                                                                                                                                           |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Profile catalog APIs       | `AnalyzerRestController` exposes read-only `/rest/analyzer/profiles` and profile detail endpoints; create accepts `defaultConfigId`                                                        | Keep built-in profiles read-only. Add explicit analyzer-type/profile management over the current model instead of reintroducing developer-only type editing as the main path. |
| Profile apply              | `AnalyzerServiceImpl.autoCreateTestMappings` applies `configDefaults`, `qcRules`, and `default_test_mappings`                                                                              | Build deterministic verification around this path. Tests must lock down profile defaults, QC-rule creation, LOINC mapping, and missing-catalog behavior.                      |
| Analyzer status            | `AnalyzerStatusTransitionServiceImpl` blocks generic analyzers from `VALIDATION -> ACTIVE` without active QC rules                                                                         | Preserve and expose this as a setup readiness gate. If the UI says Ready/Active, it must mean mappings + QC readiness are true.                                               |
| Plugin config              | `AnalyzerPluginConfigRestController` and `AnalyzerPluginConfigServiceImpl` own JSON config and pending codes                                                                               | Resolved for MVP: explicit result-mapping, option, pending-value-resolution, and verification endpoints own protected config keys; no generic write endpoint remains.         |
| QC rules                   | `AnalyzerQcRuleRestController` writes analyzer QC rules and syncs bridge registration                                                                                                      | Keep QC-rule edits authoritative in OpenELIS and sync to bridge. Add tests proving bridge payloads clear stale `qcRules`/`controlLots` with empty arrays.                     |
| Bridge registration        | `BridgeRegistrationService` registers TCP/FILE analyzers and attaches `qcRules`, `controlLots`, and test-code/LOINC data                                                                   | This is the OE-owned bridge contract. Bridge work should consume this contract; OpenELIS should not own watcher/poller runtime.                                               |
| FILE setup                 | `FileImportServiceImpl` persists FILE profile config and registers the bridge watch directory                                                                                              | Keep FILE setup/config here, but leave directory watching and file movement to bridge.                                                                                        |
| QC processing              | `QCResultProcessingServiceImpl` sends QC observations to `QCResultService.createQCResult`                                                                                                  | This is the analyzer QC ingestion path. Manual QC should converge here where possible, not create a `QcRun` island.                                                           |
| QC persistence/evaluation  | `QCResultServiceImpl` persists z-score, stats bootstrap, and publishes `QCResultCreatedEvent`; `QCRestController`, `QCChartDataRestController`, `QCViolationRestController` expose QC APIs | Westgard/QC dashboards already exist. Analyzer roadmap should fill gaps and integrate with v4 Results/Validation.                                                             |
| Current Analyzer Types UI  | `/analyzers/types` routed to a developer-facing plugin registry                                                                                                                            | Resolved: the route is a lab-facing shipped-profile verification and setup view; the duplicate registry editor is not retained.                                               |
| Current Add Analyzer route | `/analyzers/new` routed to a standalone analyzer form                                                                                                                                      | Resolved: the old route redirects to the one inline `/analyzers?add=1` setup flow.                                                                                            |
| Mapping UI                 | `/analyzers/:id/mappings` routed to a mixed legacy `FieldMapping` editor                                                                                                                   | Resolved: one deterministic review/verification surface owns profile mappings, pending codes, result values, and setup confirmation; raw and duplicate editors are removed.   |
| QC rule UI                 | `/analyzers/:id/qc-rules` routed to `QcRuleBuilderModal` with broken standalone lifecycle                                                                                                  | Resolved: the routed QC workflow loads and saves without the obsolete modal-only `open` dependency.                                                                           |
| Control lot UI             | `ControlLotSetup.jsx` parsed analyzer/test IDs as integers                                                                                                                                 | Resolved: analyzer and test IDs remain strings throughout the setup payload.                                                                                                  |

## Non-Negotiable Legacy Removal

These are not optional cleanup chores. They are acceptance criteria for the
roadmap.

| Legacy / drift                                                  | Required outcome                                                                                                                                                          |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Developer-facing Analyzer Types as the main lab admin workflow  | Replaced by lab-facing Analyzer Type/Profile list and verification. Any plugin registry remains Advanced/implementer-only, not the primary `/analyzers/types` experience. |
| `QcRun` analyzer-QC path in old tickets                         | Removed from implementation scope. Update Jira/spec text or file follow-up cleanup so `QCResult`/control persistence is the only analyzer QC direction.                   |
| OpenELIS FILE polling/watching                                  | Not implemented. OE configures and registers; bridge watches/transports.                                                                                                  |
| Per-instrument code paths for generic ASTM/HL7/FILE analyzers   | No new adapter code when a profile can express the instrument. Add/fix profiles and test fixtures instead.                                                                |
| Duplicate pending-code/pending-value stores                     | Extend the existing analyzer plugin config/pending-code model or explicitly migrate it. Do not ship two unresolved-item queues.                                           |
| Raw developer config shown as the main mapping UX               | Remove from the primary admin flow once the guided verification editor exists.                                                                                            |
| Old route retained as a second editor                           | Redirect/deprecate rather than leaving two authoritative editors for the same setting.                                                                                    |
| Standalone `/analyzers/new` setup flow after inline setup ships | Redirect/deprecate so Add Analyzer has one authoritative implementation.                                                                                                  |

## Ownership Boundary

| Concern                        | OpenELIS                                               | Bridge                                                                                               |
| ------------------------------ | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- |
| Analyzer/profile catalog       | Owns read, apply, verify, fork/export, catalog binding | No ownership                                                                                         |
| Test/result/QC mappings        | Owns authoring, validation, persistence, audit         | Consumes for classification/routing where needed                                                     |
| TCP/MLLP/ASTM listener runtime | Registers intended config only                         | Owns socket listener/client runtime, connection state, retry, framing                                |
| FILE import                    | Owns watched-folder config and registration            | Owns watch/poll/move/archive/dead-letter runtime                                                     |
| Parsing/classification         | Owns final result/QC ingestion and persistence         | Owns transport/parser runtime where bridge-side profile support exists; sends OE normalized payloads |
| QC result persistence          | Owns `QCResult`, control lots, stats, Westgard events  | May pre-classify QC vs patient, but does not persist LIS QC state                                    |
| Operational status             | Displays bridge-reported state                         | Owns runtime state and reports it                                                                    |

## MVP Single-Branch TDD Roadmap

The `OGC-1054` MVP ships as one OpenELIS branch/PR with checkpoint commits for
reviewability. The MVP scope is M0-M4 only: analyzer profile verification,
guided profile-driven setup, deterministic mapping/result-value configuration,
and analyzer QC readiness/config sync. Results/Validation v4 integration and
bridge runtime work are follow-up workstreams, not hidden scope in this branch.
Every checkpoint follows red/green/refactor and includes the code-qa gates.

### M0. Baseline Alignment + Stabilization

**Goal:** make the current code safe enough to build on and stop obvious drift.

**Tests first**

- Frontend component test proving `QcRuleBuilderModal` can mount as a routed page
  and load QC rules without an `open` prop.
- Frontend test proving `ControlLotSetup` submits analyzer/test IDs as strings.
- Backend test around `BridgeRegistrationService` proving an analyzer with no
  active QC rules/control lots still emits empty `qcRules` and `controlLots`
  collections during registration/sync (`qcRules`/`controlLots` arrays and a
  `testCodeLoinc` map).

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

**MVP tests**

- Backend tests for profile listing shape, read-only profile endpoints, and
  readiness calculation from test mappings, result mappings, and QC rules.
- Frontend tests for lab-facing profile rows, readiness/mapping counts, and setup
  actions without Java/plugin-class fields in the primary UI.
- Route-level test that plugin registry is not the default lab admin experience.

**Implementation**

- Add/extend DTOs so shipped profiles report protocol, connection mode, mapping
  counts, QC defaults, and readiness.
- Rework `AnalyzerTypeManagement` into a lab-facing shipped-profile catalog.
- Keep plugin registry as Advanced/implementer UI only if still required.

Full design-gallery filtering, used-by/source metadata, and shared/forkable
profile management remain follow-on work. They are not required to accept the
shipped-profile MVP.

**Legacy gate**

- The old developer registry cannot remain as the main `/analyzers/types`
  surface.

**Validation**

- JUnit DTO/service tests.
- Vitest/RTL for the route.
- `spec-code-alignment`: compare against `analyzer-profile-mapping.md`; document
  any deliberate code-first deviations.

### M2. Guided Analyzer Setup + Deterministic Profile Apply

**Goal:** one profile-driven analyzer creation path followed by deterministic
mapping, QC, and connection review; hand-edit remains the exception.

**Tests first**

- Backend integration/service tests proving profile apply:
  - applies `configDefaults`;
  - creates QC rules from `configDefaults.qcRules`;
  - maps tests by deterministic active-test LOINC match;
  - records missing/unmatched tests without silently activating them;
  - keeps `ACTIVE` blocked until mapping/QC readiness is satisfied.
- Frontend tests for inline profile selection/creation, lab-unit selection,
  old-route redirect, mapping/QC actions, and visible saved-analyzer connection
  result states.
- Endpoint/state integration test for `defaultConfigId` create semantics,
  exact-once defaults, and preservation of analyzer-specific overrides.
- Service/component tests proving pending analyzer codes cannot become `MAPPED`
  without a persisted catalog-backed `AnalyzerTestMapping`.

**Implementation**

- Build guided setup over the existing create/profile apply endpoint where
  possible.
- Move the user-facing Add Analyzer entry into the `/analyzers` inline flow and
  redirect/deprecate `/analyzers/new` after parity is proven.
- Continue setup through the analyzer's mapping, QC, and connection actions.
  The design-gallery's single-page `Instrument -> Verify -> Connect` wizard is
  not implemented or claimed by this MVP.
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

### M3. Catalog-Bound Result Value Mapping + Resolution

**Goal:** complete the hard part of qualitative analyzers: analyzer result values
map only to the selected test's active catalog options, and an unresolved value
can be reviewed and resolved without free-text targets.

**Tests first**

- Backend tests for result mapping read/write, catalog validation, pending value
  resolution, and verification blockers.
- Tests proving a pending value cannot be ignored or resolved without an active
  option belonging to its mapped test.
- Frontend tests for value-map editor:
  - target options come from the matched catalog test;
  - empty result-option state points to Test Catalog;
  - bound and legacy-unbound records remain readable after save/reload.

**Implementation**

- Keep result-value mappings and the resolver queue in existing analyzer plugin
  config JSON, behind explicit owned endpoints.
- Derive stored value and label server-side from `openelisResultOptionId`.
- Use `FieldMapping` as the one analyzer-instance review and verification
  surface.
- Keep runtime capture from analyzer traffic out of this PR; result import owns
  that production path in the next milestone.

**Legacy gate**

- Do not introduce a second unresolved-value queue.
- Remove raw config snapshots from the primary admin experience after the proper
  editor lands.

**Validation**

- Backend tests at service/integration level for resolver persistence and
  catalog ownership.
- Frontend component tests for editor flows.
- UI-only Playwright case that resolves a fixture-provided pending value.
- `simplicity-review`: make sure the mapping model is the smallest shape that
  supports current profile needs and not a speculative profile marketplace.

### M4. Analyzer QC Setup Completion

**Goal:** make QC setup for analyzers complete and operational, using
`QCResult`/control lots/Westgard only.

**Tests first**

- Backend tests proving analyzer QC rule create/update/delete and control lot
  create/update/activate/deactivate trigger bridge registration sync.
- Backend tests proving generic profile analyzers cannot transition to `ACTIVE`
  without at least one active QC rule.
- Backend bridge-registration tests proving active control lots are emitted with
  string-safe IDs and deterministic `controlLots` payloads.
- Frontend tests for analyzer QC rule setup, control lot setup from the analyzer
  workflow, string ID persistence, and visible analyzer QC readiness.

**Implementation**

- Finish analyzer QC rule/control lot UX integration from the analyzer workflow.
- Ensure bridge registration sync is triggered after QC rule/control lot changes
  using the existing `BridgeRegistrationService` payload builder.
- Add visible readiness fields so analyzer setup clearly says what is missing.
- Keep QC state on existing `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and
  Westgard services.

**Legacy gate**

- No `QcRun` table/entity/use case for analyzer QC.
- No OpenELIS-side FILE watcher or poller.
- If manual QC persistence is required for `OGC-428`, model it as `QCResult` /
  control persistence in a follow-up, not as a `QcRun` island in this branch.

**Validation**

- Targeted analyzer/QC backend tests.
- Targeted frontend component tests for analyzer setup, QC rules, and control
  lots.
- Playwright evidence is required before review-ready status when the full app
  stack is available; package it with `digi-uw/code-qa` `evidence-bundle`.
- `spec-code-alignment`: keep this roadmap scoped to M0-M4 and file
  Results/Validation or bridge runtime gaps as companion follow-ups.

## Follow-Up Workstreams

### Results/Validation v4 Integration Hooks

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

### Bridge Contract Lane

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

### Final QA, Evidence, and Release Readiness

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

**Minimum evidence before MVP review-ready**

- Backend targeted tests for analyzer profile apply, QC rule/control lot sync,
  result-value mapping config, and bridge registration payloads.
- Frontend tests for Analyzer Types/Profile list, guided setup, mapping editor,
  QC rule setup, control lot setup, and active-readiness visibility.
- At least one Playwright happy path from profile selection through mapping/QC
  setup when the app stack is available.
- Bridge companion evidence only if OpenELIS contract tests prove bridge behavior
  is missing; no bridge repo change is required for the MVP by default.

## Ticket Cleanup / Backlog Shape

Recommended grooming:

1. Keep `OGC-1054` as the parent for Analyzer Types/Profile + Mapping work.
2. Create child stories matching M1-M3 if they do not already exist in Jira:
   Analyzer Type/Profile list, guided setup, and catalog-bound result-value
   mapping/resolution.
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

1. Merge
   [DIGI-UW/openelis-review-tooling#2](https://github.com/DIGI-UW/openelis-review-tooling/pull/2)
   independently; its schema-v2 report contract remains backward-compatible with
   the application.
2. Deploy the final PR SHA with review-tooling commit `4d335054`, run the
   non-video UI story, inspect screenshots/trace/runtime state, then record the
   video run and download the 8/8 build-bound UAT report.
3. Complete reviewer approval and merge non-draft OpenELIS PR
   [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792) after its
   required checks and final remote evidence confirm the recorded head.
4. Groom the analyzer-list attention-summary differences from the pinned design
   as a presentation follow-up without reopening the accepted configuration/QC
   contracts.
5. Start a separate milestone PR for production analyzer result import,
   multi-component target identity, and Results/Validation v4 integration.
6. File bridge work only if cross-repository contract evidence demonstrates
   missing bridge behavior; do not add an OpenELIS FILE poller.

## Follow-Up Decisions

- For manual/RDT control persistence, do we represent it directly as `QCResult`
  plus typed metadata, or introduce a small shared control-result entity that
  still feeds `QCResult`/Westgard and v4 QC-fail signals?
- Which bridge repo/branch is authoritative for the next bridge contract lane?
