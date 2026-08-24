# OGC-1054 Analyzer Feature Roadmap

**Updated:** 2026-09-02

**Product and ownership contract:** [feature specification](../OGC-1054-analyzer-qc-config/spec.md)

**Plain-language review:** [feature map](../OGC-1054-analyzer-qc-config/feature-map.md)

This is the only OGC-1054 delivery-state document. It is deliberately short
enough to review before each implementation slice.

## Sources And Boundaries

1. This roadmap and the linked repository specification govern scope,
   architecture, execution, and acceptance.
2. Current OpenELIS, Analyzer Bridge, analyzer-mock, review-tooling code,
   `AGENTS.md`, and accepted versioned contracts determine implementation.
3. [`openelis-work@main`](https://github.com/DIGI-UW/openelis-work/tree/main/designs/analyzer-integration)
   supplies functional and visual intent only. The
   [OGC-1057 QA report](https://github.com/DIGI-UW/openelis-work/blob/qa/ogc-1057-guided-setup-report/designs/analyzer-integration/ogc-1057-qa-report.md)
   supplies review findings only. Neither defines APIs, storage, ownership, or
   tests.
4. Jira is traceability only and cannot override these sources.
5. GitHub records review and merge state. The roadmap does not copy commit
   hashes or maintain a second evidence ledger.

## Fixed Decisions

- A profile has exactly two jobs: define runtime communication for one analyzer
  type and provide defaults for a new Bridge connection.
- Bridge owns immutable profile revisions, durable connection configuration,
  protocols, listeners, parsing, probes, control recognition, FILE runtime,
  and runtime restoration.
- OpenELIS owns the lab-facing UI, local analyzer identity/lab units, local
  catalog bindings, verification/audit, activation intent, held results,
  review, alerts, result release, and operational QC.
- OpenELIS stores a Bridge connection reference, not analyzer-facing values or
  a copied profile. Its backend may transiently mediate Bridge calls.
- Analyzer Type is a composed lab-facing view, not a local plugin/profile
  authority. There is one mapping editor and one pending-result workflow.
- Operational QC never gates analyzer activation or mapping verification.
  `AnalyzerQcRule` and `QcRun` are removed.
- No OE FILE poller, raw analyzer parser, complete desired-state writer, hidden
  classifier fallback, hard-coded profile/model/code behavior, dual writer, or
  compatibility runtime survives G0.
- Existing released OE connection data receives a one-time, quiesced,
  idempotent migration. The migration is not a permanent runtime path.
- MVP publishes only GeneXpert ASTM, FluoroCycler FILE, and QuantStudio FILE.
  Other profiles return one at a time after the same contract, mock, and
  assembled-flow proof.
- The existing `Analyser Import` role authorizes analyzer profile, setup,
  activation, analyzer-result, and linked operational-QC workflows. Global
  Administrators retain their platform override. Other authenticated users
  cannot view or invoke those workflows directly.
- Multi-component mapping and Results/Validation v4 are later milestones.

## Marker Rule

- `[✓]` merged: every PR required by the checkpoint is merged.
- `[x]` review-ready: implementation and automated checkpoint evidence are
  ready for review. It remains `[x]` through review corrections.
- `[*]` active: the one checkpoint currently being implemented.
- `[ ]` future: not started.

Markers change only when a checkpoint starts, becomes review-ready, or merges.
Exactly one checkpoint is `[*]` while implementation remains. Review-ready work
may be stacked while predecessors are reviewed, but merge order is strict.
Scope, architecture, contract, or acceptance changes require an approved
roadmap amendment before production code follows them.

Every review deployment comes from an open checkpoint PR whose applicable
automated gates are green. Branch-only builds are not review targets. Preview
feedback is fixed in the owning checkpoint PR; preview deployment does not
change a roadmap marker or constitute acceptance.

Open a companion repository PR only when a failing versioned contract proves
that repository owns a required change. Do not create empty companion PRs.

Checkpoint names and order package bounded work for review; they do not prove a
code-level dependency. Derive dependencies from current code, versioned
contracts, tests, and history. Do not remove a working behavior merely because
its target replacement is named in a later checkpoint: the assembled slice that
removes it must also contain and test the replacement.

## Current Train

- [x] **R0 - Canonical roadmap and architecture.** OpenELIS
      [#4049](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4049).
- [x] **F0 - Acceptance foundation.** OpenELIS
      [#4053](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4053).
- [x] **E0 - Versioned contracts and migration boundary.** OpenELIS
      [#4055](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4055) and Bridge
      [#45](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/45).
- [x] **M1 - Bridge profiles and Analyzer Types.** OpenELIS
      [#4056](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4056), Bridge
      [#46](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/46), and mock
      [#40](https://github.com/DIGI-UW/analyzer-mock-server/pull/40).
- [x] **M2 - Local mapping and control-recognition verification.** OpenELIS
      [#4118](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4118) and Bridge
      [#47](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/47), and mock
      [#43](https://github.com/DIGI-UW/analyzer-mock-server/pull/43).
- [x] **M3 - Guided setup, durable connection, activation, and QC link.**
      OpenELIS [#4125](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4125) and
      Bridge [#48](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/48).
- [*] **M4 - Safe result traffic and integrated MVP.** OpenELIS
  [#4138](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4138), Bridge
  [#49](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/49), and mock
  [#42](https://github.com/DIGI-UW/analyzer-mock-server/pull/42).
- [ ] **G0 - Exact deployment and named human acceptance.** Review tooling
      [#17](https://github.com/DIGI-UW/openelis-review-tooling/pull/17) must be
      merged and deployed before the acceptance build is frozen.
- [ ] **R1 - Full feature operations.** Future.
- [ ] **R2 - Site rollout.** Future.

All listed PRs are stacked and unmerged. `[x]` means review-ready, not merged or
accepted. Corrections are made in the owning existing PR; do not create a
parallel remediation stack.

M4 is the active integrated checkpoint. Review corrections still land in the
earliest owning review-ready PR, from the bottom of each stack upward, and are
then carried into M4 by restacking. Those corrections do not change an `[x]`
marker. M4 becomes `[x]` only after all four remediation slices below pass.

## Execution Loop

For every bounded behavior change:

1. Select one acceptance statement and name its owning repository.
2. Audit affected tests first: retain, rewrite, move, or delete. A test for the
   superseded architecture is not evidence.
3. Record the smallest failing test at the owning layer. Cross-repository
   behavior starts with producer and consumer contract failures.
4. Implement the smallest complete behavior and remove its replaced writer,
   reader, route, field, and test in the same slice.
5. Run owning tests, then the relevant integration and assembled tests.
6. Compare code and tests to this roadmap and run `digi-uw/code-qa` alignment,
   coverage, simplicity/legacy, companion, and evidence checks.
7. For visible behavior, inspect console output, trace, screenshots, runtime
   state, and desktop/mobile design comparison before recording video.

No later UI, screenshot, or video can waive a lower-layer failure.

Acceptance tests define the allowed system positively. Do not retain
source-file, class-name, or string blacklists for deleted implementations: the
possible wrong implementations are unbounded, and such checks are not
behavioral evidence. Delete superseded code, migrations, tests, and guidance;
use closed schemas, typed provider/consumer contracts, persistence and
migration integration, and assembled behavior to prove the resulting
architecture.

## Checkpoints

### R0 - Canonical Roadmap And Architecture

Deliver:

- one concise product specification, this roadmap, and one visual feature map;
- explicit Bridge/OE/mock/review ownership and profile semantics;
- a one-time released-data migration decision;
- historical 011/014 specifications reduced to provenance pointers; and
- deletion of duplicate plan/task/checklist/evidence documents.

Exit:

- the three canonical documents agree;
- only this file contains roadmap markers;
- old documents cannot be mistaken for implementation direction; and
- the user approves this architecture before lower-stack correction resumes.

### F0 - Acceptance Foundation

Deliver closed contract fixtures and executable owner tests proving:

- the OpenELIS analyzer contract contains only LIMS-owned state and a Bridge
  connection reference;
- the Bridge profile contract retains communication behavior and
  new-connection defaults through generic consumers;
- Bridge owns connection/runtime behavior and FILE transport;
- migration has one explicit outcome for each released analyzer; and
- Playwright user stories interact only through visible UI, enforced by the
  existing syntax-aware lint rule.

Priority profile fixtures prove the same profile data drives Bridge and mock
behavior. A synthetic valid profile proves OE renders the declared Bridge
contract without a fixed connection schema.

Exit: invalid contract fixtures are demonstrated red, accepted fixtures and
owning behavior are green, and no deleted implementation remains in the diff.

### E0 - Versioned Contracts And Migration Boundary

Deliver versioned producer/consumer schemas and fixtures for:

1. the single Bridge profile revision contract;
2. generic Bridge connection create/read/update/probe/activate/deactivate;
3. the reference-only OE analyzer contract;
4. exact activation command and acknowledgment;
5. normalized patient/control/unknown traffic with raw context; and
6. migration plan/apply/verify manifests and per-analyzer outcomes.

Connection create and commands are idempotent; updates use optimistic
concurrency; secrets are masked; probes are non-mutating; restart restores the
acknowledged active revision. The migration never infers a profile from a name,
plugin class, protocol, code, or LOINC.

Exit: both repositories consume the same closed fixtures, and provider/consumer
tests accept only the reference and command contracts declared here.

### M1 - Bridge Profiles And Analyzer Types

Deliver:

- the existing profile system evolved to the E0 contract, with one Bridge
  catalog and immutable Draft/Publish/Update/Duplicate/Deactivate lifecycle;
- only the three priority profiles published and tested end to end;
- no runtime profile files or profile-serving/application path in OE;
- a composed Carbon Analyzer Types list/detail/history/authoring workflow; and
- URL-backed search, filters, selected profile/revision/tab, breadcrumbs,
  reload, browser history, and matching `Analyser Import`/Global Administrator
  route and endpoint authorization.

Exit: each priority profile passes schema, semantic, runtime, mock transport,
and visible-flow proof through generic code; a new revision never repoints a
connection implicitly; unrelated authenticated roles cannot read or mutate
Analyzer Types.

### M2 - Local Mapping And Recognition Verification

Deliver:

- one profile-revision-scoped OE site binding;
- complete Test search and Result Option selection constrained to the mapped
  active Test;
- bound, excluded, and unresolved states with deterministic suggestions only;
- human-readable Bridge recognition summary and confirmation;
- durable revision/fingerprint/actor/time audit; and
- removal of per-analyzer mapping, `AnalyzerQcRule`, copied rule arrays, and
  hidden Bridge recognition fallbacks.

Exit: every distinct priority-profile concept is visible; shared LOINC never
collapses rows; stale verification is deterministic; operational QC changes do
not stale or gate mapping/activation.

### M3 - Guided Setup, Connection, Activation, And QC Link

Work these slices in order within the active paired PRs:

1. **Connection contract correction:** rewrite current full-state sync/probe
   tests to E0's durable Bridge connection contract.
2. **Bridge persistence:** create, update, probe, activate, deactivate, restart,
   optimistic-concurrency, idempotency, and secret-handling tests and code.
3. **Upgrade migration:** test plan/apply/verify against released OE fixtures;
   add the Bridge reference, migrate each retained analyzer without guessing,
   verify restart, then remove migration-only and old runtime schema/code for
   the final candidate.
4. **OE boundary:** retain only local fields and Bridge references; mediate the
   generic API; remove fixed connection fields, profile-default copying,
   protocol decisions, full-state registration, startup replay, and obsolete
   tests.
5. **Guided Carbon UI:** preserve the inline Instrument/Verify/Connect workflow,
   lab-unit selection, summaries, URL state, breadcrumbs, and return paths;
   replace the protocol-specific form with a reusable descriptor renderer.
6. **Lifecycle:** make one OpenELIS lifecycle service the only writer that can
   move an analyzer into `ACTIVE`; show every local and Bridge blocker;
   build and synchronize one immutable candidate; require its exact Bridge
   acknowledgment; activate/deactivate that exact revision; keep draft edits
   from mutating the last active candidate; and make probe evidence visible but
   non-gating.
7. **QC link:** open the existing analyzer-scoped OE QC workflow and prove QC
   changes never alter setup verification or activation; use the same analyzer
   permission at the visible route and REST boundary.
8. **Preview:** deploy the PR-backed OE/Bridge/mock stack, sync the applicable
   Grist steps, and inspect the complete M3 visible flow.

Exit:

- Bridge restart restores the active connection exactly;
- a synthetic profile field change requires no OE production/schema change;
- the released-data migration reports every source analyzer and leaves no old
  runtime path in the final candidate;
- no OE connection value, protocol/transport branch, full-state writer,
  `AnalyzerQcRule`, duplicate create/edit route, or duplicate connection modal
  remains;
- create/update payloads and mapping, connection, error, or offline events
  cannot activate an analyzer or bypass the lifecycle service, and no hard
  delete path remains;
- analyzer setup, probe, lifecycle, and linked QC reject unrelated
  authenticated roles at the endpoint and visible-route boundaries; and
- focused backend, RTL, contract, assembled, accessibility, and visual gates
  pass.

### M4 - Safe Result Traffic And Integrated MVP

Deliver:

- known patient and recognized-control traffic through real Bridge transports;
- durable hold and visible attention for unknown tests and values;
- valid local resolution and deterministic handling of the next message;
- analyzer result review and resolution use the same established analyzer
  permission at both page and endpoint boundaries;
- priority ASTM and FILE mock stories plus a generic HL7 contract fixture;
- outbound orders addressed only by Bridge connection ID plus clinical order;
  and
- removal of superseded OE plugin routing, raw import/parser paths, local
  `AnalyzerType` registry, and direct-to-OE mock acceptance modes after parity.

Work these Grist-driven remediation slices in order. Start each behavior with a
failing test at the owning layer, make the smallest complete change, remove any
superseded behavior in the same slice, and carry the correction forward through
the existing stack.

1. **Profile lifecycle.** In M1 and M2, complete Create Profile through an
   editable, publishable Bridge draft; prove duplicate is single-submit,
   preserves Bridge lineage, leaves its source unchanged, and initializes a
   separate unconfirmed OE site binding from the source mapping decisions,
   including observed unresolved concepts. Prove Bridge lifecycle behavior in
   contract/persistence tests and OE behavior in service/integration tests plus
   real-router RTL.
2. **Mapping and attention.** In M2, make completeness and Needs Attention
   include both profile-declared concepts and observed unresolved tests/values,
   and recompute them after traffic or site-binding changes. Give unknown tests
   and unknown values one catalog-backed resolution workflow; allow only valid
   active local targets, audit the decision, leave the original held result
   unchanged, and apply it deterministically to the next matching message.
   Prove domain behavior in OE integration tests and visible behavior in
   real-router RTL before the assembled story.
3. **Connection and QC.** In M3, restore the Bridge-provided latest probe after
   reload; make the canonical New Control Lot action submit successfully;
   restrict its Test choices to active tests mapped for the selected analyzer;
   and show profile display names rather than raw identifiers. Prove probe
   persistence in Bridge/OE consumer tests and QC behavior in OE integration
   tests plus real-router RTL. Operational QC remains separate and never gates
   verification or activation.
4. **Result review.** In M4, show source analyzer identity and raw source context
   for normal, held, control, and FILE traffic. Use Bridge and analyzer-mock
   transport tests for transmission and an external demo-operator action for
   resend; do not add an OE mock control. Finish with the UI-only patient,
   control, unknown-test, unknown-value, and FILE Playwright stories.

Exit: all four remediation slices are green; their owning PRs are restacked;
patient/control/unknown behavior is proven in owning tests and UI-only assembled
Playwright stories; focused console, trace, runtime, accessibility, and
desktop/mobile screenshot review passes; and no old analyzer runtime path
survives. The resulting M4 tip is then deployed for updated Grist review.

### G0 - Exact Deployment And Human Acceptance

Deliver:

- exact OE, Bridge, mock, profile-catalog fingerprint, and review-tooling build
  metadata on `analyzers.openelis-global.org`;
- the 17 required Grist steps against that unchanged build;
- named human product-reviewer results; and
- inspected test reports, console, trace, desktop/mobile screenshots, visual
  comparison, MP4, build metadata, checklist revision, and exported report.

Exit: all 24 MVP criteria and all required UAT steps pass. Any failed required
step blocks acceptance and is triaged; issue filing remains an explicit human
action.

### R1 And R2 - Full Feature And Rollout

R1 adds broader profile curation/distribution, profile revision diff, bulk
adoption and rollback, mature alert triage/concurrency, maintenance/fleet
health, and full-feature UAT. R2 qualifies upgrades, migration, performance,
observability, backup/restore, operator guides, and representative site rollout.

## MVP Acceptance Criteria

| ID      | Observable acceptance                                                                                                                                                                                                                                                                                           | Required proof                                                         |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| MVP-001 | Authorized users see searchable/filterable shipped and site Analyzer Types, completeness, use, lifecycle, and attention state; completeness and attention include observed unresolved tests/values and refresh after traffic or site-binding changes; unrelated authenticated roles cannot read or mutate them. | OE security/integration + real-router RTL + UI E2E                     |
| MVP-002 | Create reaches an editable, publishable draft; duplicate preserves lineage and initializes a separate unconfirmed OE site binding from the source decisions without changing the source; create, duplicate, update, publish, deactivate, and reactivate are audited and single-submit; no delete exists.        | Bridge contract/integration + OE service/integration + real-router RTL |
| MVP-003 | Published revisions are immutable and retained; update/duplicate never repoints a connection.                                                                                                                                                                                                                   | Bridge persistence/restart tests                                       |
| MVP-004 | The three priority profiles retain both profile jobs, use generic runtime code, and contain no operational-QC or site-instance values.                                                                                                                                                                          | Schema/semantic tests + Bridge/mock transport                          |
| MVP-005 | Every emitted test concept is independently visible and maps by complete active-catalog search; suggestions are uniquely deterministic.                                                                                                                                                                         | OE service/integration + RTL                                           |
| MVP-006 | Qualitative mappings target only active Result Options of the mapped Test; invalid/inactive/cross-test choices fail.                                                                                                                                                                                            | OE service/integration + RTL                                           |
| MVP-007 | Recognition is explicit `RULES` or affirmed `NONE`, evaluated only by Bridge, and shown as a plain-language confirmation.                                                                                                                                                                                       | Bridge profile/runtime + OE consumer/RTL                               |
| MVP-008 | Verification records exact profile/binding/recognition fingerprints, row states, actor, and time; relevant changes stale it, QC changes do not.                                                                                                                                                                 | OE persistence/audit integration                                       |
| MVP-009 | Mapping has one Analyzer Types editor with URL-backed state and return paths; no per-analyzer editor or duplicate queue exists.                                                                                                                                                                                 | Routing integration + real-router RTL + UI E2E                         |
| MVP-010 | Authorized users see Add Analyzer inline on `/analyzers`; Instrument, Verify, and Connect reveal in order and retain list context; unrelated authenticated roles cannot open or invoke setup.                                                                                                                   | OE security/integration + RTL + UI E2E                                 |
| MVP-011 | Meaningful routes, query state, breadcrumbs, reload, back, forward, headings, lab-unit labels, and the latest structured connection-probe evidence are deterministic.                                                                                                                                           | Bridge/OE consumer + real-router RTL + accessibility/UI E2E            |
| MVP-012 | Released OE analyzer configurations migrate once to explicit Bridge profile pins/connections with complete outcomes; old schema/code/tool is absent from G0 runtime.                                                                                                                                            | Migration integration + final-schema integration                       |
| MVP-013 | Bridge durably creates and edits a profile-pinned connection; OE renders generic fields and stores no analyzer-facing value.                                                                                                                                                                                    | Cross-repo contract + persistence + RTL                                |
| MVP-014 | Probe is structured and non-mutating; synthetic profile fields and defaults change without OE production or schema changes.                                                                                                                                                                                     | Bridge tests + OE consumer contract/RTL                                |
| MVP-015 | Analyzer-scoped Quality Control opens the canonical OE workflow under the same analyzer permission; New Control Lot saves and offers only active Tests mapped for that analyzer; profile names are user-facing; QC changes never alter verification or activation.                                              | OE security + analyzer/QC integration + real-router RTL + UI E2E       |
| MVP-016 | Activation/deactivation uses the exact connection/profile/config/runtime acknowledgment, shows each blocker, preserves history, and never depends on QC or probe success.                                                                                                                                       | OE/Bridge contract + lifecycle integration + RTL                       |
| MVP-017 | Connection commands are concurrency-safe/idempotent and Bridge restart restores the exact active revision; OE performs no full-state replay.                                                                                                                                                                    | Bridge restart/contract + OE service integration                       |
| MVP-018 | Known patient and recognized-control traffic reaches the correct OE workflow; normal, held, control, and FILE review show source analyzer identity and raw source context.                                                                                                                                      | Bridge/mock transport + OE assembled integration + UI E2E              |
| MVP-019 | Unknown tests/values are durably held, visibly flagged, included in Analyzer Type completeness/attention, and never clinically posted or dropped.                                                                                                                                                               | OE persistence/integration + real-router RTL + UI E2E                  |
| MVP-020 | Authorized analyzer users can resolve unknown tests and unknown values only to valid active local catalog targets; resolution is audited, leaves the original held result unchanged, and changes the next matching result deterministically, while unrelated roles are denied.                                  | OE security/integration + real-router RTL + UI E2E                     |
| MVP-021 | ASTM, HL7, and FILE fixtures prove patient/control/nonmatch/unknown behavior; FILE watching exists only in Bridge.                                                                                                                                                                                              | Bridge/mock suites + assembled integration                             |
| MVP-022 | New UI uses reusable Carbon components, React Intl, one semantic heading, keyboard/focus behavior, and no overlapping text at desktop/mobile sizes.                                                                                                                                                             | RTL/a11y + inspected screenshots                                       |
| MVP-023 | Analyzer dashboard, Analyzer Types, setup, mapping, and QC links form one consistent visual workflow compared with `openelis-work@main`.                                                                                                                                                                        | Desktop/mobile visual review + named human UAT                         |
| MVP-024 | One unchanged deployment identifies exact component builds and checklist revision across tests, screenshots, trace, MP4, and report.                                                                                                                                                                            | Build manifest + review-tooling report                                 |

## Test Ownership

| Layer                   | Proves                                                                                                              | Must not substitute for           |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| OE JUnit 4/integration  | Local domain, persistence, migration, audit, activation, QC independence, hold/resolution, Bridge consumer contract | Browser interaction               |
| Bridge repository tests | Profile/connection contracts, persistence, protocols, parsing, probes, commands, restart, FILE behavior             | OE clinical decisions             |
| Analyzer mock tests     | Deterministic real analyzer transport and failure cases using accepted profiles                                     | Product workflows                 |
| RTL with real router    | Carbon behavior, validation, URL state, bookmarks, history, reload, headings, breadcrumbs                           | Backend contracts                 |
| Assembled harness       | Real OE + Bridge + mock + database behavior and durable outcomes                                                    | Human usability                   |
| Playwright              | Visible user stories only                                                                                           | API assertions or backend polling |
| Grist UAT               | Named human functional and visual acceptance of an exact build                                                      | Automated regression coverage     |

Playwright user stories prohibit `page.request`, API assertions, backend
polling, forced controls, arbitrary waits, and fixture mutation during the
story. Seed/fixture loading is a precondition only. Run non-video first; inspect
console, trace, screenshots, and runtime state; record MP4 only afterward.

Every checkpoint runs focused tests first, then its affected package suites,
format/lint checks, assembled contracts where applicable, and `digi-uw/code-qa`.

## Required Grist UAT

1. `AN-MVP-001` Find and inspect a shipped Analyzer Type.
2. `AN-MVP-002` Create and publish a new Analyzer Type, then duplicate an
   existing type and inspect lineage, copied local mapping decisions,
   single-submit behavior, deactivation, reactivation, and revision history.
3. `AN-MVP-003` Review test mappings and resolve a catalog match.
4. `AN-MVP-004` Map a qualitative value using only that Test's Result Options.
5. `AN-MVP-005` Confirm the control-recognition summary.
6. `AN-MVP-006` Start Add Analyzer inline from the dashboard.
7. `AN-MVP-007` Select a type, name the analyzer, and assign lab units.
8. `AN-MVP-008` Save and reload the Bridge-owned connection through Connect.
9. `AN-MVP-009` Run a visible connection test, reload, and confirm its latest
   structured evidence remains visible.
10. `AN-MVP-010` Review blockers, activate, deactivate, and reopen the analyzer.
11. `AN-MVP-011` Send a known patient result through the mock and Bridge and
    confirm normal review shows its analyzer/source context.
12. `AN-MVP-012` Send an unknown test, confirm it is held and affects attention,
    resolve it to a valid active Test, resend externally, and confirm the next
    result follows the decision.
13. `AN-MVP-013` Send a recognized control, open linked operational QC, and
    prove a control lot can be saved using only a mapped Test.
14. `AN-MVP-014` Send an unknown qualitative value and confirm it is held.
15. `AN-MVP-015` Resolve the held value and verify the next matching result.
16. `AN-MVP-016` Repeat the visible traffic story with a priority FILE type and
    confirm row-level analyzer/source context without an OE FILE configuration
    or import path.
17. `AN-MVP-017` Review desktop/mobile clarity, breadcrumbs, bookmark/reload,
    back/forward, focus, and overall consistency.

Grist is the checklist source. The overlay must refresh current steps, preserve
answers by stable step key, show errors, and export checklist revision, route,
actual URL, mark time, notes, reviewer, and exact build metadata. Review-tooling
changes require their own PR only when a failing harness contract proves a gap.

## Final Gate

G0 completes only when R0 through G0 are `[✓]`, `MVP-001` through `MVP-024`
pass, the exact OE/Bridge/mock/profile-catalog/review-tooling revisions are
deployed, all 17 Grist steps pass under a named human product reviewer, and the
inspected evidence bundle describes one unchanged deployment.
