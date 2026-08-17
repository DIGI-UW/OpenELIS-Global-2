# OGC-1054 Analyzer Feature Authoritative Roadmap

**Updated:** 2026-08-17
**Status:** Authoritative implementation roadmap; MVP not yet delivered
**Epic:** [OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054)
**Roadmap branch:** `codex/ogc-1054-r0-roadmap`
**Historical foundation pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)
**First human product-acceptance gate:** Full MVP deployed to
[`https://analyzers.openelis-global.org`](https://analyzers.openelis-global.org/login)
with current Grist UAT and MP4 evidence

This is the single engineering roadmap for Analyzer Types, mapping, guided
setup, analyzer QC/configuration, and safe analyzer traffic. It supersedes the
product-scope claims in the older Analyzer QC/config roadmap and in the
PR #3792 SpecKit set. Those files remain foundation records for git provenance.

The implementation is a coordinated three-repository PR train, not one large
cross-cutting PR. OpenELIS, Analyzer Bridge, and analyzer mock each have a
linear stack. Companion PRs at one checkpoint share a contract revision and
must pass together before the checkpoint can be accepted. A descendant branch
may be prepared and tested while its predecessor is under review, but it cannot
be accepted, merged, or deployed before the predecessor.

## Non-Negotiable Source Boundary

`DIGI-UW/openelis-work` is a **non-technical product and design source**. The
same rule applies to technical-looking prose copied into Jira product stories.

It may define:

- user goals and lab-facing workflows;
- visible information, actions, states, and terminology;
- functional acceptance behavior;
- visual composition and interaction intent.

It may not define or imply:

- Java, JavaScript, database, entity, table, JSON, or persistence design;
- API, route, event, message, or payload shape;
- repository, service, process, or runtime ownership;
- migration, synchronization, polling, parsing, or transport design;
- implementation sequencing or test-layer ownership.

Any technical language found in a product brief, prototype, annotation, or gap
analysis is explicitly non-normative. It must not enter an implementation plan
unless engineering independently derives and records the decision from current
code, repository-owned engineering specifications, and an ADR or contract.
The names used by a mock are product labels, not instructions to create
same-named code or persistence objects.

For example, the entity, table, annotation, route, and persistence suggestions
currently embedded in some OGC-1054 child descriptions are not engineering
decisions. Jira remains authoritative for product outcome, priority, status,
and dependency only.

## Authority Order

| Question                                           | Authoritative evidence                                                                                                   |
| -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| What should a lab user accomplish?                 | Current Jira stories plus current `openelis-work` functional requirements and mocks, used only within the boundary above |
| What does the visual workflow need to communicate? | Current `openelis-work` mocks plus OpenELIS Carbon conventions                                                           |
| Which repository owns behavior?                    | This roadmap, [`AGENTS.md`](../../AGENTS.md), engineering specs, and current OpenELIS/Bridge code                        |
| How is it persisted or transported?                | An approved engineering ADR/contract grounded in current code                                                            |
| What exists today?                                 | Current repository code and git history                                                                                  |
| What is accepted?                                  | Passing automated evidence and exact-build remote UAT; never a route, API shape, old screenshot, or old video alone      |

Git is the provenance layer. Revisions to scope or architecture amend this file
and retain earlier decisions in history; they do not rewrite product documents
to fit an implementation shortcut.

## Reference Index

### Functional and visual references only

These references answer what a reviewer should be able to accomplish and what
the experience should communicate. They must not be cited as technical
evidence in an ADR, migration plan, API contract, or code review.

- [OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054): product epic.
- [OGC-1055](https://uwdigi.atlassian.net/browse/OGC-1055),
  [OGC-1056](https://uwdigi.atlassian.net/browse/OGC-1056),
  [OGC-1057](https://uwdigi.atlassian.net/browse/OGC-1057), and
  [OGC-1058](https://uwdigi.atlassian.net/browse/OGC-1058):
  dependency-ordered product outcomes.
- [Analyzer Types & Mapping functional specification](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/analyzer-profile-mapping.md).
- [Analyzer Types & Mapping functional prototype](https://digi-uw.github.io/openelis-work/designs/analyzer-integration/analyzer-profile-mapping.html).
- [Analyzer Types & Mapping functional gap review](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/analyzer-profile-mapping-gap-analysis.md).
- [OGC-1057 guided setup QA report at reviewed revision](https://github.com/DIGI-UW/openelis-work/blob/d4ad271d4e0acd2b612418ac05f0d1e067b88621/designs/analyzer-integration/ogc-1057-qa-report.md),
  used as a functional observation snapshot of the 2026-08-12 demo, not as
  implementation direction or final acceptance evidence.
- [Published OpenELIS design catalog](https://digi-uw.github.io/openelis-work/catalog.html),
  used for current visual comparison and neighboring workflow context.

### Engineering and implementation references

These references own architecture, repository boundaries, current behavior,
contracts, migration, and tests.

- [`AGENTS.md`](../../AGENTS.md): analyzer boundary, FILE ownership,
  constitutional architecture, TDD, Carbon, and legacy-removal rules.
- [Generic analyzer integration engineering spec](../011-madagascar-analyzer-integration/spec.md).
- [FILE stream ownership engineering spec](../014-hjra-file-stream-alignment/spec.md).
- [Bridge registration runtime](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/AnalyzerRegistrationController.java),
  [Bridge connectivity runtime](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/TestConnectivityController.java),
  and [Bridge FHIR normalization](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/fhir/FhirBundleBuilder.java).
- [OpenELIS Bridge synchronization](../../src/main/java/org/openelisglobal/analyzer/service/BridgeRegistrationService.java)
  and [unified FHIR import](../../src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerFhirImportController.java).
- [Analyzer mock server](https://github.com/DIGI-UW/analyzer-mock-server) and its
  deterministic protocol/QC tests.
- [Analyzer profile bootstrap assets](../../projects/analyzer-profiles/README.md),
  explicitly treated as the current transitional path rather than target
  profile authority.
- [OpenELIS review tooling](https://github.com/DIGI-UW/openelis-review-tooling)
  for build-bound Grist UAT and evidence.

## Terminology

| Term                     | Meaning                                                                                                                                                                                                          |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Generic analyzer runtime | Existing ASTM, HL7, FILE, FHIR, and Bridge transport foundation                                                                                                                                                  |
| Analyzer profile         | A versioned, portable, instrument-facing definition used by Bridge runtime                                                                                                                                       |
| Analyzer Type            | The lab-facing composed view of a Bridge profile plus site-specific OpenELIS catalog bindings and readiness                                                                                                      |
| Site binding             | OpenELIS-owned association from normalized analyzer test/result concepts to the local Test and Result Option catalog                                                                                             |
| Analyzer instance        | A configured instrument at a lab, associated with one profile revision, lab units, operational QC, status, and Bridge runtime registration                                                                       |
| PR #3792 foundation      | Historical source for selected route, mapping, QC, and test behavior; it is not in the new PR train and is not the OGC-1054 MVP                                                                                  |
| Coordinated PR train     | Three linear, cross-linked repository stacks: OpenELIS, Analyzer Bridge, and analyzer mock                                                                                                                       |
| OGC-1054 MVP             | A complete lab-admin workflow to manage an Analyzer Type, map and verify it, configure and activate an analyzer, and safely receive and resolve known and unknown traffic without developer-edited configuration |
| Full OGC-1054 rollout    | All remaining OGC-1054 workflow refinements, including live capture/reconciliation and mature lifecycle operations                                                                                               |
| Full analyzer program    | OGC-1054 plus multi-component ingestion, Results/Validation integration, per-instrument validation, maintenance, access control, and site rollout                                                                |

## Fixed Architecture

The analyzer architecture is already split. This roadmap does not reopen that
decision.

### Bridge owns analyzer runtime

The Analyzer Bridge owns:

- the versioned portable profile catalog and profile validation contract;
- ASTM, HL7/MLLP, FILE, and serial-facing listeners and transport;
- FILE directory watching, retries, delivery, and transport dead-letter state;
- protocol parsing, framing, analyzer identification, and connection probes;
- analyzer-code to normalized-code translation used by runtime;
- QC-sample identification from instrument messages;
- normalized FHIR output, including preserved raw code/value context;
- runtime analyzer registration and idempotent full-state reconciliation;
- bidirectional protocol execution where a profile supports it.

### OpenELIS owns laboratory and clinical decisions

OpenELIS owns:

- the lab-facing Analyzer Types, mapping, setup, review, and alert workflows;
- analyzer instance identity, assigned lab units, lifecycle, and permissions;
- local Test and Result Option catalog bindings;
- mapping confirmation, verification fingerprints, actor/time, and audit;
- operational analyzer QC rules, control lots, QC results, Westgard evaluation,
  and activation readiness;
- durable staging/holding of known and unknown clinical results;
- user-visible resolution, review, alerts, and downstream clinical processing;
- desired analyzer-instance configuration and synchronization requests to
  Bridge.

OpenELIS does not own raw analyzer protocol parsing, instrument listeners,
runtime FILE polling, protocol-specific connection logic, or a second portable
profile authority.

### Analyzer mock owns reproducible instruments

`tools/analyzer-mock-server` owns deterministic test instruments and fixtures.
It must exercise the Bridge through real ASTM, HL7, and FILE transports for
integration acceptance. Its legacy direct-to-OpenELIS HTTP modes may be used
only to characterize or retire legacy behavior; they cannot prove the target
runtime architecture.

### Review tooling owns review provenance

`DIGI-UW/openelis-review-tooling` owns the Grist-backed checklist overlay,
checklist revision, build manifest, and downloadable review report. It does not
own application behavior or seed data.

## Target Runtime Contract

The contract is directional and versioned:

1. OpenELIS selects a Bridge profile revision and sends desired instance
   configuration and active operational QC context.
2. Bridge validates and applies the desired runtime registration idempotently.
3. The analyzer mock or a real instrument sends raw traffic to Bridge.
4. Bridge parses and emits normalized FHIR with analyzer identity, profile
   revision, raw analyzer code/value, normalized code where known, QC
   classification, and source metadata.
5. OpenELIS binds normalized/raw concepts to its local catalog, stages the
   result, evaluates operational QC, and either proceeds or holds it with a
   visible reason.
6. Resolving an unknown local catalog binding updates durable OpenELIS site
   binding state. A portable profile change, when required, is made through the
   Bridge profile lifecycle contract and produces a new revision.

Unknown test codes or values must cross the Bridge boundary with enough raw
context to resolve them. Bridge must not drop them, and OpenELIS must not post
them as patient results until clinically bound.

## Current Code Baseline

Baseline reconciled on 2026-08-17:

- OpenELIS `develop` is `2017f491b7cbf1b7b6b06cc47a048b4298fd55cd`.
- OE-R0 [#4049](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4049)
  is open at `6c3f001e2d74be92ee91b9b810d1581d03710845`; its last checks
  are green, but it is behind current `develop` and review is required.
- OE-F0 [#4053](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4053)
  is open and green at `ec80c9e9d1490dbbc6709ad5a8bebd58589e0079`.
- OE-E0 [#4055](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4055)
  is open and green at `41f2091f4f92485f747a388203406207e0242282`.
- OE-M1 [#4056](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4056)
  is open at `074f4d1468ac873128631c0496d70ea87ad30c99`; frontend and
  translation checks pass, while backend CI fails because two new database
  integration tests collide with pre-seeded analyzer primary keys in the full
  suite. Focused tests alone do not close that failure.
- BR-E0 [#45](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/45)
  is open and green at `e17b021ad0687b455d01d1cc6f4702e045ea0fe3`.
- BR-M1 [#46](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/46)
  is open and green at `42cb4bc4d2181ac578d0e3d9d5f589e85b3b4fa1`; OE-M1 pins
  that exact Bridge revision.
- OE-M1 pins analyzer mock
  `d063356e5a8f82ca6a44cf809be1874a7d704f8e` and code-qa
  `30528d176bd128b4765242d130f38ca9fb85d7b8`. Current analyzer-mock
  `main` is `573308f6bc994ba585cb1cedb8f932b79ce6b215`.
- `openelis-work` current `main` is
  `5490a45f5d3775c0ee31a7baf691f64a66e02151`; it remains functional and
  visual evidence only.
- Review-tooling `main` is
  `21bf2515a2b4de3aad6dde8b61b17d3c74b3f772`, which supplies stable step
  keys, deterministic checklist revisions, refresh/stale-answer behavior,
  exact-SHA targeted deployment, and verified target metadata.
- PR #3792 was retitled as historical and closed on 2026-08-13 at
  `d985e6ce727b555c414b7db1129b3b1eeaf664cc`. It is immutable provenance,
  not a stack member or delivery candidate.
- The live analyzer host still serves historical deployment
  `20260728T205914Z-2c840a55b03b` from OE
  `2c840a55b03b238a2ad00c987181504c2bef6ef6` and review overlay
  `f3deb02e6e45cacbe9a7ad77159c2aaf3fea8e2c`. Its published checklist is
  schema 2, revision
  `9164a26d8c71f27ef72cd2452643f97d146b8e4f377bd2816c021776edbf239c`,
  with eight historical `AN-QC-*` steps. It is not current MVP acceptance
  evidence.
- The canonical analyzer review host remains
  `analyzers.openelis-global.org`.

### Present on OE-F0

- Standalone analyzer list, create/edit, type, field-mapping, QC-rule, and
  control-lot routes. There is no current inline Instrument/Verify/Connect
  story.
- Transitional OpenELIS filesystem profile assets and create-time bootstrap
  behavior. They are migration inputs, not the target profile authority.
- Existing local analyzer mappings, pending-code infrastructure, Bridge desired
  registration, and operational QC entities (`AnalyzerQcRule`, `QCControlLot`,
  `QCResult`, and Westgard).
- Legacy OpenELIS protocol-reader/import paths. F0 does not treat them as the
  target runtime; E0-M4 own migration and removal under the fixed Bridge
  boundary.
- Static direct and dependency-aware guards for analyzer `harness-demo`
  stories, plus 13 transport/setup cases honestly classified as foundational
  integration coverage.

### Historical #3792 provenance, not present on OE-F0

The frozen #3792 branch contains iterations of an inline setup shell,
URL-backed guided state, catalog-bound result-option work, verification
metadata, readiness blockers, and deterministic registration payload changes.
Those behaviors are inputs to the F0 salvage manifest only. They are not current
OE-F0 implementation, are not cherry-picked, and do not satisfy any product
acceptance criterion until reimplemented and accepted at the owning checkpoint.

### Partial or absent product behavior

- Bridge-owned reusable profile lifecycle and site-created profile flow.
- A living analyzer-to-profile-revision association rather than a transient
  create hint and copied per-analyzer snapshot.
- Completeness, usage, source, lineage, deactivate/reactivate, fork, and update
  impact in Analyzer Types.
- A complete add/edit/remove/repoint mapping editor showing unmatched profile
  rows instead of skipping them.
- Explicit QC-identification-code confirmation, separate from operational QC.
- Capability-aware Results only/Two-way selection.
- Production creation of pending result values from Bridge traffic.
- Durable hold plus Alerts/Needs attention for unknown traffic.
- Live result capture/reconciliation and blank-profile population.
- Current integrated remote acceptance against current OpenELIS, Bridge, mock,
  profile, and review-tooling revisions.

Therefore neither PR #3792 nor the July deployment is the OGC-1054 MVP.

The baseline claims above are grounded in current OpenELIS route definitions,
analyzer forms/valueholders, profile-loading code, status-transition services,
and tests. `openelis-work` contributes only the functional and visual outcomes
crosswalked below; it does not supply these implementation conclusions.

| Current-code claim                       | Reproducible code evidence                                                                                                                                                                                                                                                                                                                 |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Setup remains separate routed pages      | [`frontend/src/App.jsx`](../../frontend/src/App.jsx) and [`AnalyzersList.tsx`](../../frontend/src/components/analyzers/AnalyzersList/AnalyzersList.tsx) route Add, Edit, Mappings, QC Rules, and Control Lots separately.                                                                                                                  |
| Qualitative binding is not catalog-safe  | [`QualitativeResultMapping.java`](../../src/main/java/org/openelisglobal/analyzer/valueholder/QualitativeResultMapping.java) and [`QualitativeResultMappingForm.java`](../../src/main/java/org/openelisglobal/analyzer/form/QualitativeResultMappingForm.java) persist free-text `openelisCode`.                                           |
| Activation is not complete MVP readiness | [`AnalyzerStatusTransitionServiceImpl.java`](../../src/main/java/org/openelisglobal/analyzer/service/AnalyzerStatusTransitionServiceImpl.java) checks current status and at least one active QC rule, not current mapping verification plus profile-applicable QC readiness.                                                               |
| OpenELIS still loads bootstrap profiles  | [`AnalyzerRestController.java`](../../src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java) reads filesystem profiles and applies `defaultConfigId` at create time. E0 owns migration away from profile authority in core OpenELIS.                                                                            |
| Operational QC foundation exists         | [`AnalyzerQcRule.java`](../../src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerQcRule.java), [`QCControlLot.java`](../../src/main/java/org/openelisglobal/qc/valueholder/QCControlLot.java), and [`QCResult.java`](../../src/main/java/org/openelisglobal/qc/valueholder/QCResult.java) are the retained operational QC path. |

## Scope

### Full MVP

The MVP is reached only when a laboratory administrator can:

1. find a shipped or site Analyzer Type, understand readiness and usage, and
   create or fork a type without editing files;
2. map every analyzer test, qualitative result value, and QC identification
   code to valid local concepts through one protocol-neutral editor;
3. create an analyzer inline, select readable lab units, verify the mappings,
   configure Bridge-owned connectivity, and see all activation blockers;
4. configure required operational QC with existing OpenELIS QC entities;
5. activate the analyzer only after current mapping/QC verification;
6. receive a known patient result and a QC result through Bridge from the
   analyzer mock;
7. hold and visibly flag an unknown test/value, resolve it safely, and process
   the next matching result deterministically;
8. reload, bookmark, navigate by breadcrumb, and review the same durable state.

The MVP includes a discoverable Alerts/Needs attention path. A resolver hidden
inside an analyzer page is not enough for safe operation.

### Full OGC-1054 rollout

After MVP acceptance, complete:

- live “send a result” capture during setup;
- matched/seen/not-seen reconciliation for every mapping;
- blank profile population from live traffic;
- mature alert triage, acknowledgement, concurrency, and navigation;
- profile revision diff, update impact, rollback, and distribution hardening;
- scale and accessibility validation for large catalogs and profile libraries.

### Outside OGC-1054

- multi-component target-to-component ingestion
  ([OGC-1136](https://uwdigi.atlassian.net/browse/OGC-1136));
- Results Entry/Validation v4 and patient-report behavior;
- broad analyzer maintenance and fleet health;
- instrument-by-instrument vendor validation and country rollout;
- a core OpenELIS FILE poller or raw protocol reader.

## Requirement Crosswalk

The `openelis-work` references in this table supply functional and visual intent
only. They supply no implementation instructions.

| Product slice                                                             | Functional/visual reference                                               | Current code state                                                               | Delivery checkpoint |
| ------------------------------------------------------------------------- | ------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------- |
| [OGC-1055](https://uwdigi.atlassian.net/browse/OGC-1055) Analyzer Types   | Reuse, create/fork, completeness, usage, lifecycle, and list presentation | Transitional shipped-profile/type page; site lifecycle absent                    | M1                  |
| [OGC-1056](https://uwdigi.atlassian.net/browse/OGC-1056) mapping          | Complete test/result/QC-code editor and safe save scope                   | Legacy standalone mapping/pending paths; accepted catalog-bound editor absent    | M2                  |
| [OGC-1057](https://uwdigi.atlassian.net/browse/OGC-1057) guided setup     | Inline instrument, verify, connect, and readable review workflow          | Standalone routes only; current activation does not implement full readiness     | M3                  |
| [OGC-1058](https://uwdigi.atlassian.net/browse/OGC-1058) traffic learning | Hold, alert, resolve, and reconcile unknown traffic                       | Pending/error infrastructure exists; production hold/alert/reconciliation absent | M4 and R1           |
| PR #3792 QC/config extension                                              | Historical behavior provenance only                                       | Frozen divergent branch; every considered behavior is classified in F0           | F0 classification   |

Current Jira status was rechecked on 2026-08-13: OGC-1054 and OGC-1055 through
OGC-1058 are `Ready`; the separate multi-component story OGC-1136 is `Backlog`.
Only product outcomes and dependencies from those stories are carried forward.

## Functional Acceptance Crosswalk

This table paraphrases the current product acceptance behavior without carrying
over any proposed data model, endpoint, route, class, annotation, or repository
ownership from a product artifact.

| Product AC | Functional outcome                                                                          | Current branch                                                     | Target  |
| ---------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ | ------- |
| AC-1       | Add Analyzer starts inline while the analyzer list remains available.                       | Absent; `/analyzers/new` is a standalone route.                    | M3      |
| AC-2       | Instrument, Verify, and Connect form one progressive, understandable setup story.           | Absent; current setup concerns are separate routes.                | M3      |
| AC-3       | Instrument/type selection is searchable.                                                    | Partial standalone selectors; no accepted integrated type search.  | M3      |
| AC-4       | A not-listed instrument can start creation of a reusable site type.                         | Absent.                                                            | M1 + M3 |
| AC-5       | Verify shows every profile test, normalized identity, and match state.                      | Absent; create-time bootstrap does not retain/display every row.   | M2 + M3 |
| AC-6       | Human mapping confirmation is mandatory and auditable.                                      | Absent on OE-F0.                                                   | M2 + M3 |
| AC-7       | QC identification codes are reviewed and confirmed during Verify.                           | Absent; operational QC is a different capability.                  | M2 + M3 |
| AC-8       | A non-match can map to an existing Test, detour to Test Catalog, or be explicitly excluded. | Partial legacy pending-code resolver; no complete source-row flow. | M2      |
| AC-9       | One unresolved test does not hide or block independent mapping work.                        | Absent; unmatched bootstrap rows are not retained visibly.         | M2      |
| AC-10      | Results only is the safe default; Two-way appears only when supported and probed.           | Absent; initiator mode is exposed instead.                         | M3      |
| AC-11      | Every test mapping can be added, edited, removed, or repointed.                             | Absent for profile-applied rows.                                   | M2      |
| AC-12      | A qualitative result can target only an option belonging to its mapped Test.                | Absent; current mapping stores free-text `openelisCode`.           | M2      |
| AC-13      | Saving shared changes requires explicit new-type or update-shared scope.                    | Absent.                                                            | M2      |
| AC-14      | A fork has a unique name and visible lineage.                                               | Absent.                                                            | M1 + M2 |
| AC-15      | Unknown traffic is held and visibly flags the analyzer/type and Alerts.                     | Pending/error infrastructure only; hold/alert path absent.         | M4      |
| AC-16      | Analyzer Types shows completeness/usage and the required search/filter states.              | Partial type/profile list; completeness and usage absent.          | M1      |
| AC-17      | Types can be deactivated/reactivated without deleting history.                              | Absent.                                                            | M1      |
| AC-18      | ASTM, HL7, and FILE share one complete editor with only protocol labels varying.            | Absent; current standalone editor is not the accepted workflow.    | M2      |
| AC-19      | Normal lab setup contains no developer-only fields.                                         | Absent; current standalone form still exposes technical fields.    | M3      |
| AC-20      | All visible copy is localized.                                                              | Partial; raw status/fallback strings remain.                       | M1-M4   |
| AC-21      | Setup can request a live result and reconcile what was received.                            | Absent.                                                            | R1      |
| AC-22      | Unknown data is never lost; resolution changes future matching behavior.                    | Absent end to end.                                                 | M4      |
| AC-23      | Live traffic can populate a newly created blank type.                                       | Absent.                                                            | R1      |

### OGC-1057 QA finding disposition

The 2026-08-12 QA report was reviewed at `openelis-work`
`d4ad271d4e0acd2b612418ac05f0d1e067b88621`. Its browser observations are
functional input. Its REST checks, named endpoints, implementation diagnosis,
and proposed technical remedies are not imported as engineering authority. The
report contains no screenshot or video assets, so it does not replace the M3/G0
visual comparison and evidence gates.

| QA finding  | Deterministic roadmap disposition                                                                                                                                             | Gate                                  |
| ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| 1           | Every source row can be added, edited, removed, or repointed through complete active-Test catalog search.                                                                     | M2; MVP-005/006; `AN-MVP-003`         |
| 2           | Applying or migrating a profile never drops, hides, collapses, or falsely marks an unmatched source row complete.                                                             | E0 + M2; MVP-005/006                  |
| 3           | Changing an applicable result option enables save, persists, and survives reload; invalid and empty-option rows remain explicit.                                              | M2; MVP-007; `AN-MVP-004`             |
| 4           | Test selection searches the complete active local catalog by name, code, or LOINC; no fixed legacy subset can satisfy acceptance.                                             | M2; MVP-006; `AN-MVP-003`             |
| 5           | Source aliases and local LOINC cardinality are characterized; ambiguous candidates never auto-bind and source rows remain independently confirmable.                          | E0 + M2; `AMB-M2-001`                 |
| 6           | Mapping/QC-identification confirmation is independent of operational QC readiness; each has its own visible completion and blocker state.                                     | M2 + M3; MVP-008/012/015/016          |
| 7           | Instrument not listed creates a reusable site type through the Bridge-owned lifecycle without developer fields.                                                               | M1 + M3; MVP-003/010; `AN-MVP-002`    |
| 8           | Method-dependent control-lot requirements are visible before submit and exact validation is actionable; valid save recomputes readiness.                                      | M3; MVP-015/022; `AN-MVP-009`         |
| 9           | Type lifecycle uses deactivate/reactivate in M1; M2 removes Copy Mappings; M3 replaces analyzer-instance hard delete with audited deactivate/reactivate.                      | M1 + M2 + M3; MVP-004/009             |
| Deferred    | Real mock-to-Bridge probes cover role-appropriate settings, success, failure, timeout, supported direction, and visible Results-only degradation.                             | BR-M2 + M3 + MOCK-M4; MVP-013/014/021 |
| Untested    | Live capture/reconciliation and blank-type population are R1; M4 owns hold/alert/resolve and deterministic next-message behavior for unknown traffic.                         | M4 + R1; MVP-019/020 and rollout      |
| Preserve    | Inline setup, searchable selection, readable summaries, lab units, live blockers, catalog-safe result choices, and absence of developer fields remain functional regressions. | M1-M3; MVP-001/007/010/016/022        |
| Withdrawn   | The report's withdrawn picker-search and direction-default observations add no defect requirement; later UI automation must use focused visible controls.                     | M3/G0; MVP-010/014/023                |
| Environment | G0 starts from a deterministic reset/fixture state; artifacts left by the 2026-08-12 review are preconditions to remove, not product history to hard-delete through the UI.   | G0 deployment preflight               |

## Execution Contract

These are engineering validation milestones, not partial product acceptance.
The first product review requested from the user is G0, the full deployed MVP.
There is one next **acceptance** checkpoint at a time and no
architecture-selection prompt in the workstream. Descendant implementation may
continue while an earlier checkpoint awaits review, but its status cannot
advance past `IN_PROGRESS` until every earlier checkpoint is `MERGED`.

### Checkpoint lifecycle and authority

Every checkpoint uses exactly these states. A commit, passing focused command,
open PR, deployed route, or old UAT report cannot imply a transition.

| State | Deterministic condition | Who records or controls the transition |
| ----- | ----------------------- | -------------------------------------- |
| `NOT_STARTED` | No checkpoint implementation or red test has begun. | Roadmap owner records the initial state. |
| `IN_PROGRESS` | At least one checkpoint task or red test has begun, or any required test, evidence item, review thread, or predecessor gate remains open. | Implementer records it; CI failure or a changed accepted SHA returns the checkpoint here. |
| `READY_FOR_REVIEW` | The exact PR head has the required acceptance record, assigned criteria, targeted and broad tests, formatting, CI, runtime evidence, and no unresolved blocking ambiguity; every predecessor is `MERGED`. | Implementer may record it only from cited evidence. |
| `READY_FOR_UAT` | G0 only: the exact release-candidate SHAs are deployed, all machine gates pass, the 15-step Grist revision is served, and the inspected non-video/video evidence bundle is complete. | Delivery owner records it from deployment and evidence manifests. An agent or Playwright run cannot mark human UAT results. |
| `ACCEPTED` | Engineering checkpoint: required repository reviewer approval applies to the unchanged ready SHA. G0: a human product reviewer marks every required Grist step `pass` against the unchanged ready deployment and approves the evidence bundle. | Required GitHub reviewer for R0-M4; human product reviewer for G0. Implementers and agents cannot self-accept. |
| `MERGED` | The accepted SHA has landed on its canonical target and every dependent PR has been rebased/retargeted to that landed identity. | Repository maintainer performs the merge; implementer records the landed SHA and updates descendants. |

Any production or evidence change after `READY_FOR_REVIEW`, `READY_FOR_UAT`, or
`ACCEPTED` invalidates that status. The checkpoint returns to `IN_PROGRESS`, is
redeployed where applicable, and repeats every affected gate. Documentation-only
changes may retain approval only when the required reviewer explicitly confirms
that no accepted behavior or evidence identity changed.

An executor given the goal “execute this roadmap through deployed MVP” must:

1. fetch all three repository bases and read their local `AGENTS.md` files;
2. resume the first checkpoint whose status is neither `ACCEPTED` nor `MERGED`;
3. create or reuse exactly the branch and base named below;
4. record a failing test before production implementation, implement to green,
   refactor, and attach the red/green/refactor commands and commits to the PR;
5. update this roadmap's status ledger, acceptance record, issue/ambiguity
   register, and evidence links in the same checkpoint PR;
6. continue preparing the next stacked checkpoint without waiting for a product
   review, while leaving it `IN_PROGRESS` until its predecessors merge;
7. stop only for a permission/credential boundary, a required external review
   or merge, or evidence that contradicts this fixed architecture.

### Roadmap provenance invariant

This path has one active lineage. R0 establishes the authority; each later
OpenELIS checkpoint may update status, evidence, and newly discovered issues
only as a Git descendant of the canonical predecessor SHA. Before the
predecessor merges, that SHA is its reviewed remote branch head. After merge, it
is the commit that landed the predecessor on the target branch, including a
squash or merge commit. Rebase the dependent branch onto the new canonical SHA
when that identity changes. A sibling or historical branch may retain an older
blob as provenance, but it is never an active roadmap and is never merged as a
competing version. Before publishing or retargeting a checkpoint PR, prove the
canonical SHA is an ancestor and compare this file against that SHA so every
change is an intentional descendant update. Record both the canonical SHA and
roadmap blob in the checkpoint acceptance record. PR #3792 remains immutable
historical input and receives no roadmap edits.

If a prerequisite PR has not merged, the next PR targets its branch. After the
prerequisite merges, rebase the dependent branch on current `develop`/`main`,
retarget its PR, rerun its gates, and update the recorded base SHA. Never merge
a PR automatically, force a review conclusion, or mark a checkpoint accepted
because code merely exists.

Each checkpoint is a manageable implementation and review unit. Work within a
checkpoint proceeds one independently testable behavior at a time. Start at the
lowest layer that owns the rule, add integration or contract coverage only when
the behavior crosses a persistence or repository boundary, add RTL coverage for
user interaction and route state, and reserve Playwright for an assembled
visible user story. Repeating a mocked assertion at every layer is not evidence
of integration.

### Required checkpoint acceptance record

Every checkpoint PR must update this roadmap or link a committed checkpoint
record containing all of the following. PR prose alone is not the record of
truth.

| Record field           | Required content                                                                                                  |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Scope                  | Checkpoint ID, assigned criteria, exclusions, repository, branch, base SHA, and red/green/refactor/evidence SHAs  |
| Current-code baseline  | Current paths and tests that establish what exists before the increment                                           |
| Red                    | Test name, owning layer, exact command, observed failure, and commit containing the failing test                  |
| Green                  | Smallest implementation commit and exact targeted command that makes the same test pass                           |
| Refactor               | Refactor commit or explicit `NO_REFACTOR_NEEDED`, followed by the targeted command                                |
| Layer validation       | Unit/service, persistence, contract, RTL, harness, Playwright, and UAT marked `RUN`, `NOT_APPLICABLE`, or `LATER` |
| Acceptance crosswalk   | Criterion ID to automated test, visible workflow where applicable, and evidence artifact                          |
| Visible route scope    | Exact routes/query states, semantic `h1`, breadcrumb targets, reload/back/forward assertions, and screenshot names |
| Viewport evidence      | `1440x900` desktop and `390x844` mobile for every changed lab-facing route; inspected result recorded, not merely generated |
| Legacy-path audit      | Touched superseded paths removed, guarded, or linked to a priority removal issue                                  |
| Decisions              | Engineering decisions grounded in current code/ADR/contract; product references remain functional/visual only     |
| Issues and ambiguities | Stable ID, evidence, impact, owner, resolution gate, status, and decision or conservative interim behavior        |
| Final gate             | Formatting, focused regression, required broader suite, CI, review threads, and resulting status transition       |
| Status authority       | Exact previous/new lifecycle state, immutable head/deployment identity, evidence, and person or role authorized to make the transition |

An ambiguity is blocking when resolving it could change clinical safety,
repository ownership, durable data semantics, a cross-repository contract, or
an acceptance criterion. No production implementation proceeds through that
ambiguity. A non-blocking ambiguity may use a conservative interim behavior only
when that behavior and its removal/review gate are recorded. Failed tests,
missing evidence, or unavailable credentials are issues, not assumptions to
silently route around.

### Current issue and ambiguity register

| ID             | Kind       | Status     | Impact / next deterministic action                                                                                                                                                                                                                      |
| -------------- | ---------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ISSUE-R0-001` | Review     | `OPEN`     | OE-R0 is green but requires external approval and merge before it can become `ACCEPTED`; F0 may be prepared but cannot be accepted first.                                                                                                               |
| `ISSUE-R0-002` | Validation | `RESOLVED` | The original command block retained `frontend` as its working directory; commands now run in repository-rooted subshells.                                                                                                                               |
| `ISSUE-R0-003` | Validation | `RESOLVED` | The final Playwright story is absent on the R0 base; OE-M4 owns its creation and earlier checkpoint records must mark that full-story gate `LATER`.                                                                                                     |
| `ISSUE-R0-004` | Provenance | `RESOLVED` | A commit cannot name its own SHA; committed records name implementation/evidence commits and CI records the final immutable PR-head SHA.                                                                                                                |
| `ISSUE-R0-005` | Provenance | `RESOLVED` | PR #3792 was retitled and closed as historical after OE-F0 #4053 opened; it links OE-R0/OE-F0 and its branch remains immutable history.                                                                                                                 |
| `ISSUE-R0-006` | Provenance | `RESOLVED` | Code audit found #3792-only behavior described as current. R0 now distinguishes current-code facts from historical provenance and enforces one descendant roadmap lineage.                                                                              |
| `ISSUE-R0-007` | Provenance | `RESOLVED` | The 2026-08-14 reconciliation replaced stale #3792, PR-head, CI, deployment, checklist, Bridge, mock, review-tooling, and `openelis-work` facts with exact current revisions and states.                                                                  |
| `AMB-R0-008`   | Acceptance | `RESOLVED` | Checkpoints now use explicit `NOT_STARTED`, `IN_PROGRESS`, `READY_FOR_REVIEW`, `READY_FOR_UAT`, `ACCEPTED`, and `MERGED` states with immutable-SHA rules and named transition authority.                                                                  |
| `AMB-F0-001`   | Scope      | `RESOLVED` | The F0 salvage manifest classifies every considered #3792 behavior group before production reimplementation and prohibits commit-level cherry-picks.                                                                                                    |
| `ISSUE-F0-001` | Test scope | `RESOLVED` | Three analyzer transport/setup specs used backend helpers while labeled as demo evidence; F0 reclassified all three as foundational and retained 13 integration cases.                                                                                  |
| `ISSUE-F0-002` | Validation | `OPEN`     | Repo-wide `npm run typecheck` has 1,589 baseline TypeScript errors and none name the three changed analyzer specs; targeted Playwright compilation passes. Fix before G0.                                                                               |
| `AMB-E0-001`   | Contract   | `RESOLVED` | ADR-001 fixes Bridge profile/OpenELIS shared site-binding ownership, fingerprint grouping, explicit forks, one-writer cutovers, and rollback from current code/contracts.                                                                               |
| `ISSUE-E0-001` | Runtime    | `OPEN`     | Isolated ASTM traffic exposed normalized coding/raw-map mismatch and contradictory zero mapping counts; BR-M4/OE-M2 own correction against v1 preserved-raw contracts.                                                                                  |
| `ISSUE-E0-002` | Harness    | `OPEN`     | Mock dynamic networks use global names/fixed 10.42 pools; MOCK-M4 must add tested per-stack namespace/pool configuration before two analyzer stacks run concurrently.                                                                                   |
| `ISSUE-E0-003` | Harness    | `RESOLVED` | Explicit fixture targets were validated after generation and reset rediscovered global containers; red/green tests now bind every operation to one validated DB_CONTAINER.                                                                              |
| `ISSUE-E0-004` | Test scope | `RESOLVED` | A controller behavior test borrowed a security filter from another test's scanned nested config; behavior and dedicated security-slice coverage are now independent.                                                                                    |
| `ISSUE-E0-005` | Test scope | `RESOLVED` | The unavailable-Docker regression exposed the runner's system Docker through its test PATH. The test now supplies only deterministic `dirname`/fixture stubs, so Docker is genuinely absent.                                                            |
| `ISSUE-E0-006` | Contract   | `RESOLVED` | BR-M1 secures analyzer APIs but OE sent no service credentials. Red `5e633a9d0` and green `c34131393` make the shared OE Bridge client satisfy the accepted Basic-auth contract.                                                                        |
| `ISSUE-E0-007` | Security   | `RESOLVED` | Red `366aa2e8c` proved that `BridgeHttpClient` accepted an untrusted certificate; green `3e2e88325` now uses the configured truststore or JVM defaults, preserves hostname verification, and fails closed when configured trust material is unreadable. |
| `ISSUE-E0-008` | Harness    | `OPEN`     | Focused UI Playwright stories pass against the isolated exact-WAR stack, but browser review still reports Vite HMR WebSocket failures and a recurring 404. Resolve or explicitly classify these before G0 console-clean acceptance.                     |
| `ISSUE-M1-001` | Runtime    | `RESOLVED` | BR-M1 red `f9cd2f6` and green `b7001b1` add one active-only registry view and prove inactive FILE analyzers are neither discoverable nor routable while remaining available for desired-state reconciliation.                                           |
| `ISSUE-M1-002` | Test scope | `RESOLVED` | BR-M1 red `468fde2` and green `c516d79` make ASTM listener activation configurable, disable it in shared test configuration, and prove default-on startup plus shutdown. The prior 34-second forced-fork run now completes in 3.8 seconds.              |
| `AMB-M1-003`   | Migration  | `RESOLVED` | ADR-001 requires explicit administrator selection when legacy profile identity is absent, exact unique source-row matching, durable blocking anomalies, atomic per-analyzer reference switch, read-only legacy evidence, and no legacy mapping writes after OE-M1 cutover. No heuristic profile inference is permitted. |
| `AMB-M1-004`   | Identity   | `RESOLVED` | ADR-001 fixes public Analyzer Type identity as the URL-safe Bridge `profileId`; Bridge revision is explicit/query-addressable and local binding IDs remain internal. A mapping fork pairs a Bridge profile fork with a new OpenELIS binding aggregate, preserving one public identity and repository ownership. |
| `ISSUE-M1-005` | Test scope | `RESOLVED` | PR #4056 CI and the exact local regression proved that the pre-existing analyzer-wide ORM validator did not register the new site-binding aggregate. The migration-anomaly checkpoint extends that repository gate instead of relying only on a narrower new ORM test. |
| `ISSUE-M1-006` | Product    | `OPEN`     | The current fork UI exposes a technical `Profile ID`, while the functional requirement requires a suggested next unique fork name and no developer identity field. BR-M1 must own deterministic profile identity generation; OE-M1 must submit only lab-facing input. |
| `ISSUE-M1-007` | Security   | `OPEN`     | Analyzer Type routes are gated by frontend `ANALYSER_IMPORT` while the composed REST API requires `ROLE_ADMIN`. Align and prove one explicit permission contract before M1 runtime acceptance; do not infer equivalence from the default admin fixture. |
| `ISSUE-M1-008` | Validation | `OPEN`     | Current list/detail RTL tests mock router hooks, so they prove emitted strings but not reload/back/forward restoration. Replace the routing seam with a real test router and add the focused visible M1 browser flow required by MVP-001 through MVP-004 and MVP-011. |
| `ISSUE-M1-009` | CI         | `OPEN`     | OE-M1 #4056 backend CI at `074f4d146` runs 4,970 tests and fails two new PostgreSQL integration tests because the analyzer ID sequence is behind seeded rows. Fix fixture/sequence isolation and prove the focused tests plus the full backend job; do not dismiss the full-suite collision because focused runs pass. |
| `ISSUE-M1-010` | Migration  | `RESOLVED` | A local migration test briefly treated two explicit source rows sharing normalized coding or one local Test as a blocker. ADR-001 and the E0 no-loss report require both rows to remain independent; corrected tests now prove both explicit bindings migrate without collapse, while ambiguous aliases still block. |
| `AMB-M2-001`   | Product    | `RESOLVED` | Multiple source rows that share LOINC or one local Test remain independently visible, bindable, verifiable, and fingerprinted for MVP. No grouping or alias inference may collapse them; a later explicit product revision may add presentation grouping without changing identity.                         |
| `AMB-M3-001`   | Safety     | `OPEN`     | The source of profile-applicable operational-QC requirements must be fixed by BR-M2/OE-M3 contracts before activation readiness is implemented.                                                                                                         |
| `AMB-M3-002`   | Product    | `RESOLVED` | MVP uses four linkable setup sections: Instrument, Verify, Connect, and Review. Review is a summary/activation section with its own canonical query state and breadcrumb position; it does not duplicate editors from the first three sections.                                                    |
| `ISSUE-G0-001` | Operations | `OPEN`     | Live preflight on 2026-08-13 found historical OE `2c840a55b03b`, harness `f3deb02e`, and only eight `AN-QC-*` steps. G0 must deploy the exact RC with current review tooling and publish/verify all 15 `AN-MVP-*` steps.                                |

Resolved rows remain in the table for provenance. New evidence updates a row in
the checkpoint that discovers or resolves it; IDs are never reused.

### Status ledger

| Order | Checkpoint                | Status on 2026-08-17 | Exact next transition gate |
| ----- | ------------------------- | -------------------- | -------------------------- |
| 0     | R0 roadmap                | `IN_PROGRESS`        | Rebase #4049 onto current `develop`, rerun required checks, then record `READY_FOR_REVIEW` |
| 1     | F0 foundation salvage     | `IN_PROGRESS`        | R0 merged; reconcile the acceptance record and CI on rebased #4053 head |
| 2     | E0 contract and migration | `IN_PROGRESS`        | F0 merged; rebase paired BR-E0/OE-E0, rerun contracts/CI, complete review |
| 3     | M1 Analyzer Types         | `IN_PROGRESS`        | Fix #4056 backend CI, finish M1-AC-001 through M1-AC-010, runtime/browser evidence, and predecessor merges |
| 4     | M2 mapping                | `NOT_STARTED`        | M1 and BR-M2 merged |
| 5     | M3 guided setup and QC    | `NOT_STARTED`        | M2 merged and `AMB-M3-001` resolved in a versioned contract |
| 6     | M4 safe traffic           | `NOT_STARTED`        | M3, BR-M4, and MOCK-M4 merged |
| 7     | G0 deployed acceptance    | `NOT_STARTED`        | M4 merged and exact RC manifest committed |
| 8     | R1 full feature train     | `NOT_STARTED`        | G0 human-accepted and merged |
| 9     | R2 operational rollout    | `NOT_STARTED`        | R1-G1 human-accepted and merged |

A failed required test, open blocking review thread, missing artifact, or
failed required UAT step keeps a checkpoint `IN_PROGRESS` or returns it there.
It cannot be relabeled as a follow-up. Only the transition authority in the
lifecycle table can record `ACCEPTED`.

BR-M1 is prepared and pushed at `42cb4bc` in
[PR #46](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/46). Its 643-test
local suite and GitHub `Run Tests` check are green, including the resolved
`ISSUE-M1-001` and `ISSUE-M1-002` cases. OE-M1 has begun but is not complete.
Its pushed #4056 head contains the Bridge catalog consumer, revisioned
site-binding persistence, catalog composition, Carbon list/detail and lifecycle
UI, imported-binding reuse, and durable migration-anomaly lifecycle. Backend CI
is red at that exact head, and local work beyond it is not remote evidence.
Migration/cutover, lab-safe fork identity, permission alignment, real-router
coverage, isolated runtime/browser evidence, and every predecessor merge remain
open. M1 is therefore `IN_PROGRESS`, not `READY_FOR_REVIEW` or `ACCEPTED`.

## Pull Request Train

PR #3792 is not a stack. It was renamed and closed as frozen historical
provenance after F0 opened. It is not rebased, merged, edited, or used as a
branch base; its branch remains available only for the committed F0 behavior
classification.

### OpenELIS stack

| Order | ID    | Fixed branch                           | Initial PR base | Scope                                                                         |
| ----- | ----- | -------------------------------------- | --------------- | ----------------------------------------------------------------------------- |
| 0     | OE-R0 | `codex/ogc-1054-r0-roadmap`            | `develop`       | This roadmap and current engineering ownership amendments only                |
| 1     | OE-F0 | `codex/ogc-1054-f0-foundation`         | OE-R0 branch    | Characterize and cleanly salvage compatible #3792 foundation behavior         |
| 2     | OE-E0 | `codex/ogc-1054-e0-contract-migration` | OE-F0 branch    | ADR, consumer contracts, migration fixtures/report, red cross-repo tests      |
| 3     | OE-M1 | `codex/ogc-1054-m1-analyzer-types`     | OE-E0 branch    | OpenELIS Analyzer Types composition, site bindings, lifecycle UI, migration   |
| 4     | OE-M2 | `codex/ogc-1054-m2-mapping`            | OE-M1 branch    | Complete protocol-neutral mapping and QC-identification confirmation          |
| 5     | OE-M3 | `codex/ogc-1054-m3-setup-qc`           | OE-M2 branch    | Guided setup, Bridge connectivity, operational QC, activation                 |
| 6     | OE-M4 | `codex/ogc-1054-m4-safe-traffic`       | OE-M3 branch    | Hold/alert/resolve, integrated harness, legacy removal, full UI story         |
| 7     | OE-G0 | `codex/ogc-1054-g0-acceptance`         | OE-M4 branch    | RC manifest, Grist manifest, inspected evidence index, acceptance corrections |

### Analyzer Bridge stack

| Order | ID    | Fixed branch                          | Initial PR base  | Scope                                                                                                                  | Required by  |
| ----- | ----- | ------------------------------------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------ |
| 1     | BR-E0 | `codex/ogc-1054-e0-contracts`         | Bridge `develop` | Versioned profile/registration/normalized-traffic contracts and compatibility fixtures, without lifecycle feature code | OE-E0        |
| 2     | BR-M1 | `codex/ogc-1054-m1-profile-lifecycle` | BR-E0 branch     | Bridge-owned portable profile catalog, validation, revision, fork, and lifecycle implementation                        | OE-M1        |
| 3     | BR-M2 | `codex/ogc-1054-m2-mapping-qc`        | BR-M1 branch     | Mapping identity, QC-identification, capability, and connection evidence contract                                      | OE-M2, OE-M3 |
| 4     | BR-M4 | `codex/ogc-1054-m4-safe-traffic`      | BR-M2 branch     | Known/unknown/QC/FILE normalized traffic with preserved raw context                                                    | OE-M4        |

### Analyzer mock stack

| Order | ID      | Fixed branch                 | Initial PR base | Scope                                                                                                                    | Required by |
| ----- | ------- | ---------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------ | ----------- |
| 1     | MOCK-M4 | `codex/ogc-1054-m4-fixtures` | mock `main`     | Deterministic ASTM, HL7, and FILE known, unknown, QC, connection, failure, and supported two-way fixtures sent to Bridge | OE-M4       |

OpenELIS PRs that depend on a companion pin its exact submodule SHA and link its
PR. A companion merges before the first OpenELIS consumer that requires it. The
consumer is then rebased, updates the submodule pointer, and reruns contracts.
No empty companion PR is created: if a repository's current implementation
already passes the checkpoint contract, the OpenELIS evidence record names the
tested SHA and marks it `NO_CHANGE`.

The one global acceptance order is:

1. OE-R0
2. OE-F0
3. BR-E0
4. OE-E0
5. BR-M1
6. OE-M1
7. BR-M2
8. OE-M2
9. OE-M3
10. BR-M4
11. MOCK-M4
12. OE-M4
13. OE-G0

Branches may be prepared and tested while their predecessor is under review,
but a later item cannot become `READY_FOR_REVIEW` before every earlier item is
`MERGED`. It cannot become `ACCEPTED`, merge, or become a deployment candidate
out of order. This order, not PR creation time, selects the next acceptance
checkpoint.

Review tooling `main` is an acceptance dependency, not an implementation lane.
Its current contract already satisfies this roadmap. A review-tooling PR is
opened only if a failing contract test against its current `main` proves an
acceptance requirement is absent.

## Delivery Checkpoints

### R0 - Authoritative roadmap

1. Merge this document and the engineering source-boundary/ownership amendments
   from a clean current-`develop` PR containing no feature code.
2. Link #3792 as historical provenance, not as an implementation base.
3. Confirm all product/mock links are labeled functional/visual only.

**Exit:** OE-R0 is green and reviewable; this file is the one execution source
for OGC-1054 and names the exact next branch, OE-F0.

### F0 - Deterministic foundation salvage

1. Create OE-F0 from OE-R0, never from #3792.
2. Add `specs/roadmaps/ogc-1054-foundation-salvage-manifest.md`. For every
   #3792 behavior considered, record origin commit/path, current-`develop`
   equivalent, characterization test, action, and rationale.
3. Use only these actions: `PROVIDED_BY_DEVELOP`, `REIMPLEMENT_WITH_TDD`, or
   `DROP_INCOMPATIBLE`. Do not cherry-pick a multi-purpose feature commit.
4. A behavior is reimplemented only when a failing characterization test ties
   it to an MVP criterion and it obeys the fixed Bridge/OpenELIS boundary.
5. Reject copied-profile authority, app-owned analyzer runtime, and duplicate
   mapping/pending editors from F0 salvage, and assign their migration/removal
   to the owning E0-M4 checkpoint. In F0, guard API-focused Playwright, stale
   acceptance claims, and any new extension of a legacy path.
6. Update and close #3792 as superseded after OE-F0 is open.

**Exit:** OE-F0 is a small green replacement foundation; every retained behavior
has a test and every rejected behavior has a reason or legacy guard. #3792 is no
longer an open delivery candidate.

**Evidence:** [F0 foundation salvage manifest](./ogc-1054-foundation-salvage-manifest.md).

### E0 - Engineering contract and migration characterization

1. In OE-E0, record an ADR for the Bridge profile/OpenELIS site-binding boundary
   and derive persistence from current code constraints only.
2. In BR-E0, version portable profile, registration, normalized traffic, and
   compatibility contracts without implementing the M1 lifecycle.
3. Characterize `defaultConfigId`, copied plugin JSON, `analyzer_test_map`, raw
   import endpoints, and existing analyzers in migration tests.
4. Characterize active local Test/Result Option catalog coverage, missing and
   duplicate LOINCs, source aliases, and zero/one/multiple-candidate behavior.
5. Define no-loss migration, rollback, anomaly reporting, and the one-writer
   cutover for each legacy store/path.
6. Add failing producer/consumer contract fixtures for known test, unknown test,
   unknown value, QC, FILE, and registration reconciliation.

**Exit:** OE-E0 and BR-E0 are green; approved ADR/contracts, migration
fixture/report, rollback, and contract tests exist. No M1 production code starts
against an unresolved boundary.

**Evidence:** [E0 contract and migration checkpoint](./ogc-1054-e0-contract-migration-evidence.md).

### M1 - Bridge profile lifecycle and Analyzer Types (OGC-1055)

1. In BR-M1, implement the Bridge-owned versioned profile catalog and lifecycle
   against the accepted BR-E0 contract.
2. In OE-M1, compose one lab-facing Analyzer Types view from Bridge profile
   metadata plus OpenELIS local completeness/readiness/usage.
3. Support shipped and site-created types, fork, unique naming, lineage,
   deactivate/reactivate, and audit/history.
4. Make search and filters URL-backed and use reusable Carbon page, breadcrumb,
   status, table, empty-state, and notification components.
5. Migrate existing analyzers to a profile revision plus explicit local site
   binding without silent remapping.

**Exit:** MVP-001 through MVP-004, MVP-011, and applicable MVP-022 criteria pass;
a type is reusable by multiple analyzers and no OpenELIS filesystem catalog or
create-only copied snapshot remains authoritative.

**Prepared Bridge evidence:** [BR-M1 PR #46](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/46)
is non-draft and green at `42cb4bc`. Its local suite passed 643 tests with zero
failures or errors and three expected serial-environment skips; its GitHub
`Run Tests` check also passed after BR-E0 `e17b021` enabled CI for stacked
pull-request bases. This does not accept M1; OE-M1 and predecessor gates remain
required.

**OpenELIS implementation evidence:** [M1 Analyzer Types checkpoint](./ogc-1054-m1-analyzer-types-evidence.md).

### M2 - Safe mapping editor (OGC-1056)

1. In BR-M2, expose the normalized identities, raw codes, QC-identification
   codes, capabilities, and revision needed by the consumer contract.
2. In OE-M2, show every profile test row, including unmatched rows, with raw
   code, normalized identity, match state, and local Test selection. Shared
   normalized identities never collapse distinct source rows.
3. Add/edit/remove/repoint test bindings using complete active Test catalog
   search by name, code, or LOINC. Suggest a binding only for one unique active
   candidate; zero or multiple candidates remain visibly unresolved.
4. Bind qualitative values only to active Result Options owned by the mapped
   Test; derive value and label server-side; prove edit, enabled save, persisted
   state, and reload behavior for every applicable row.
5. Confirm Bridge QC-identification codes separately from operational QC.
6. Validate Test Catalog return URLs and expose explicit fork/update scope plus
   affected-analyzer warning.
7. Recompute completeness and stale verification after every relevant change.

**Exit:** MVP-005 through MVP-009, MVP-012, and applicable MVP-022 criteria pass
for ASTM, HL7, and FILE; one complete editor remains and invalid bindings are
rejected server-side.

### M3 - Guided setup, connectivity, and QC (OGC-1057)

1. Complete one inline Instrument -> Verify -> Connect -> Review story in OE-M3
   with canonical URL/query state, linkable breadcrumbs, reload, back, and
   forward behavior.
2. Provide searchable type selection and an instrument-not-listed path through
   the BR-M1 lifecycle contract.
3. Persist and display readable lab-unit assignments.
4. Require audited mapping/QC-identification confirmation and make it stale on
   relevant profile, site-binding, or QC-identification change. Operational QC
   changes recompute their own readiness and do not invalidate mapping sign-off.
5. Execute probes in Bridge and show protocol-appropriate evidence; separate
   connection initiator from Results only/Two-way capability. Collect only
   role-applicable settings, show the endpoint a lab must configure, and degrade
   an unreachable Two-way probe visibly to Results only without blocking
   supported one-way setup.
6. Configure existing `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard
   readiness without adding `QcRun`. Show method-dependent required fields and
   actionable server validation before a control-lot save can fail generically.
7. Block activation with a complete visible list of current blockers.

**Exit:** MVP-010 through MVP-016 and applicable MVP-022 criteria pass; a lab
administrator can activate a complete analyzer without developer fields or file
edits, and runtime setup occurs in Bridge.

### M4 - Safe traffic and integrated MVP (OGC-1058 safety scope)

1. In BR-M4, preserve raw context and normalized identity for known, unknown,
   QC, and FILE messages.
2. In MOCK-M4, supply deterministic real-transport fixtures; direct
   mock-to-OpenELIS delivery cannot satisfy a target-architecture test.
3. In OE-M4, stage known patient and QC results through the unified FHIR path.
4. Hold unknown tests/values; never discard or clinically post them.
5. Create durable Alerts/Needs attention linked to analyzer, profile revision,
   held result, and mapping action.
6. Resolve only through valid local catalog choices, audit the decision, and
   prove the next matching message maps deterministically.
7. Add one full UI-only Playwright story named
   `frontend/playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts` and
   register focused non-video/video scripts.
8. Remove or priority-track every superseded raw reader, copied-profile writer,
   duplicate editor, and duplicate pending queue; no dual-write remains.

**Exit:** MVP-017 through MVP-023 pass, all prior criteria remain green, and the
top of the three repository stacks forms one reproducible release candidate.

### G0 - Full MVP deployment and first product review

1. Create OE-G0 from OE-M4 and commit an RC manifest containing exact OpenELIS,
   Bridge, mock, profile-catalog, and review-tooling revisions plus all PR URLs.
2. Push the exact 40-character OE-G0 SHA and deploy it only to
   `analyzers.openelis-global.org` with the merged review-tooling targeted
   deployment command.
3. Load deterministic fixtures as a deployment precondition. No Playwright user
   story may seed or mutate state through an API.
4. Create/publish the Grist story and steps below through native Grist MCP or
   the Grist UI, verify the served revision, and keep old foundation steps
   unpublished rather than deleting or renaming them.
5. Run the focused non-video remote UI story first. Inspect test output, browser
   console, screenshots, trace, runtime state, and desktop/mobile captures.
6. Compare captures with current `openelis-work` functional/visual intent only;
   record its exact reference revision without treating it as implementation
   authority.
7. Fix every required failure and repeat from step 2 when the app SHA changes.
8. Run the matching video project only after non-video evidence is clean.
9. Download Markdown/JSON UAT reports and retain MP4, screenshots, trace, target
   metadata, image digests, checksums, CI links, and `code-qa` output in one
   deployment-ID evidence index.

The exact deployment sequence from a clean
`DIGI-UW/openelis-review-tooling@main` checkout is:

```bash
./deploy.sh app deploy analyzers --ref <OE_G0_40_CHAR_SHA> --scope app
./deploy.sh app status analyzers --deployment <DEPLOYMENT_ID>
./deploy.sh app verify analyzers
curl -fsS https://analyzers.openelis-global.org/__review/target.json
```

Run the remote visible story from the OE-G0 frontend checkout after the deploy
and fixture precondition:

```bash
cd frontend
BASE_URL=https://analyzers.openelis-global.org npm run pw:test -- \
  --project=harness-demo --workers=1 \
  playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts
BASE_URL=https://analyzers.openelis-global.org \
PLAYWRIGHT_VIDEO=on PLAYWRIGHT_SLOWMO=500 npm run pw:test -- \
  --project=harness-demo-video --workers=1 \
  playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts
```

Do not run the second command until the first run and its console/screenshots/
trace inspection are clean. If either implementation SHA changes, redeploy and
restart the evidence sequence; evidence never transfers between deployments.

**Exit:** MVP-024 passes; every required Grist step is `pass` against the exact
ready deployment; the MP4 shows the complete visible story; all three PR stacks
are green and reviewable. This is the first point labeled **OGC-1054 MVP
accepted**.

### R1 - Full OGC-1054 feature rollout

Create a new milestone stack after G0 for live capture, mapping reconciliation,
blank-profile learning, mature alert triage, profile revision diff/update/
rollback, and all remaining functional behavior. Repeat the exact-build UAT
gate with a separately versioned full-rollout story and MP4.

### R2 - Operational rollout

Validate scale, disaster recovery, security, monitoring, documentation, and
representative site deployments. Track each instrument with its vendor-grounded
integration spec, companion guide, Bridge profile, mock fixture, contract tests,
and site validation.

## Deterministic MVP Acceptance Criteria

| ID      | Criterion                                                                                                                                                                                                                                                             | Primary proof                                   |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| MVP-001 | Analyzer Types lists shipped and site types with source, status, completeness, usage, and attention state.                                                                                                                                                            | Service/integration + RTL + UI E2E              |
| MVP-002 | Search and filters round-trip through the URL and restore identical visible state after reload/back/forward.                                                                                                                                                          | RTL with router + UI E2E                        |
| MVP-003 | A user can create a site type or fork a shared type; lineage, unique name, actor, and revision are durable.                                                                                                                                                           | Bridge contract + OpenELIS integration + UI E2E |
| MVP-004 | Deactivation prevents new use but preserves existing history; reactivation is audited; hard delete is unavailable.                                                                                                                                                    | Service/integration + RTL                       |
| MVP-005 | The editor displays every independent profile source row, including unmatched/aliased rows; local lookup never skips, collapses, or falsely completes one.                                                                                                            | Contract + service + RTL                        |
| MVP-006 | Test selection searches the complete active catalog by name/code/LOINC; only a unique candidate may be suggested, while unresolved/ignored choices remain independent per row.                                                                                        | JUnit 4 + RTL + UI E2E                          |
| MVP-007 | Every applicable qualitative row can select only an active Result Option owned by its Test; selection enables save, server-derived value/label persist, and reload restores the binding.                                                                              | JUnit 4 + RTL + UI E2E                          |
| MVP-008 | QC-identification codes are shown and confirmed separately from operational QC rules/lots.                                                                                                                                                                            | Bridge/OpenELIS contract + RTL + UI E2E         |
| MVP-009 | Saving a shared mapping requires explicit fork/update scope and names affected analyzers; no copy/clone action bypasses that lifecycle.                                                                                                                               | Service + RTL + UI E2E                          |
| MVP-010 | Inline setup supports type selection, instrument-not-listed, name, readable lab units, Verify, Connect, and Review; profile selection visibly confirms the loaded profile plus mapping/QC/result counts, and saved analyzer name/lab-unit assignments survive reload. | RTL + UI E2E                                    |
| MVP-011 | Every page has one semantic `h1`, linkable breadcrumbs, and canonical URL/query state.                                                                                                                                                                                | RTL + UI E2E                                    |
| MVP-012 | Mapping/QC-identification confirmation records actor/time/revision/fingerprint and becomes stale only after relevant profile, binding, or identification change, not operational QC.                                                                                  | JUnit 4 integration + audit assertion           |
| MVP-013 | Bridge connection testing uses only role-applicable settings and returns visible success, failure, missing-configuration, and timeout evidence plus the endpoint to configure.                                                                                        | Bridge test + RTL + UI E2E                      |
| MVP-014 | Results only is the default; Two-way appears only when supported and a failed round-trip visibly degrades to Results only without blocking one-way setup.                                                                                                             | Bridge contract + RTL + UI E2E                  |
| MVP-015 | Operational QC uses existing QC entities only; applicable required fields and exact validation are visible, valid saves immediately recompute independent QC readiness.                                                                                               | JUnit 4 analyzer/QC + RTL + UI E2E              |
| MVP-016 | Activation is rejected until profile/bindings are current, required QC is ready, and runtime registration is synchronized.                                                                                                                                            | JUnit 4 + contract + UI E2E                     |
| MVP-017 | Bridge registration/profile sync is versioned, idempotent, deterministic, and emits explicit empty collections.                                                                                                                                                       | Cross-repo contract tests                       |
| MVP-018 | A known patient result and QC result travel mock -> Bridge -> FHIR -> OpenELIS and become visible in the correct workflow.                                                                                                                                            | Mock + Bridge + harness integration             |
| MVP-019 | Unknown test/value traffic retains raw context, is held, creates visible attention/alert state, and is not clinically posted.                                                                                                                                         | Contract + OpenELIS integration + UI E2E        |
| MVP-020 | Resolving unknown traffic uses valid local catalog choices, is audited, and makes the next matching message deterministic.                                                                                                                                            | Integration + UI E2E                            |
| MVP-021 | ASTM, HL7, and FILE each have known, unknown, QC, and connection fixtures; FILE runtime watching occurs only in Bridge.                                                                                                                                               | Mock/Bridge suites + repository guard           |
| MVP-022 | All user copy is localized; Carbon components/tokens are used; desktop/mobile layouts have no overlap or unreachable action.                                                                                                                                          | RTL/a11y + inspected screenshots                |
| MVP-023 | Playwright performs the complete visible story without `page.request`, API assertions, backend polling, forced controls, or arbitrary waits.                                                                                                                          | Playwright guard + test audit                   |
| MVP-024 | Evidence identifies OpenELIS, Bridge, mock, profile, and review-tooling SHAs/revisions plus deployment time, checklist revision, routes, mark times, screenshots, trace, and MP4.                                                                                     | Build manifest + UAT report                     |

## Test Strategy

Every checkpoint records the first failing test, passing implementation,
refactor result, and final evidence. A route or endpoint existing is never a
functional proof.

| Layer                     | Owns                                                                                                  | Required approach                                                                    |
| ------------------------- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Bridge unit/service       | Profile validation/versioning, parsing, transport, QC identification, probes, idempotent registration | Bridge repository test conventions; real protocol fixtures                           |
| OpenELIS unit/service/DAO | Local catalog constraints, site binding, audit, stale verification, readiness, hold/resolve           | JUnit 4; ORM validation for new mappings; real Postgres where query behavior matters |
| Cross-repo contracts      | Registration, profile revision, normalized FHIR, raw unknown context, QC, FILE delivery               | Versioned fixtures run by both producer and consumer                                 |
| Analyzer mock             | Reproducible ASTM/HL7/FILE known, unknown, QC, failure, and two-way scenarios                         | `pytest`; deterministic IDs and values; transport to Bridge                          |
| Harness integration       | OpenELIS + Bridge + mock + database assembled behavior                                                | Real containers and transport; assert durable outcomes, not internal mocks           |
| Frontend                  | Carbon composition, accessibility, validation, routing/query/breadcrumb state                         | Vitest/RTL with real router context and minimal network stubs at component boundary  |
| Playwright                | Complete lab-facing story                                                                             | Visible UI only; seed may establish preconditions but cannot perform the story       |
| Remote UAT                | Human acceptance and visual coherence                                                                 | Grist overlay against exact build, inspected evidence, final MP4                     |

These commands are the full G0 acceptance contract. Each checkpoint runs and
records its currently applicable subset. OE-M4 owns creation of the named final
Playwright story; earlier records mark that command `LATER`, never substitute an
API-level test or unrelated E2E. If a script or project name changes, amend this
roadmap in the PR that changes it; do not silently substitute a different gate.

```bash
# OpenELIS
mvn -Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.analyzerimport.**,org.openelisglobal.qc.** test
mvn spotless:check

# Frontend
(
  cd frontend
  npm test
  npm run typecheck
  npm run check-format
  npm run lint
  npm run build
  npm run pw:guard
  npm run pw:test -- --project=harness-demo --workers=1 playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts
  PLAYWRIGHT_VIDEO=on PLAYWRIGHT_SLOWMO=500 npm run pw:test -- --project=harness-demo-video --workers=1 playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts
)

# Bridge
(
  cd tools/openelis-analyzer-bridge
  mvn test
  mvn verify
)

# Analyzer mock
(
  cd tools/analyzer-mock-server
  python3 -m pytest
)
```

For remote G0, the same focused Playwright file runs with
`BASE_URL=https://analyzers.openelis-global.org`; fixture loading is a separate
precondition. The existing local `ci-parity-test.sh` remains the assembled-stack
gate and is not mislabeled as the remote runner.

The final code-quality gate uses `digi-uw/code-qa` for spec/code alignment,
meaningful coverage, simplicity and legacy removal, cross-repository companion
status, and the evidence bundle.

## Full MVP Grist Checklist

Create one published Grist story for instance `analyzers`, host
`analyzers.openelis-global.org`, Jira `OGC-1054`, and the full MVP PR/evidence
links. Create these stable, required steps only when OE-G0 is deployed. Keep old
foundation stories unpublished; do not rename their keys or reuse their answers.

| Step key     | Reviewer action                                                                                 | Expected result                                                                                           |
| ------------ | ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `AN-MVP-001` | Open Analyzer Types, search/filter, and inspect one shipped type.                               | Source, status, completeness, usage, protocol, mapping, QC, and attention information is understandable.  |
| `AN-MVP-002` | Create a site type through Instrument not listed, or fork a shared type.                        | A uniquely named audited revision is created without developer fields or file edits.                      |
| `AN-MVP-003` | Open the mapping editor and resolve an unmatched test.                                          | Every source row remains visible; the selected active Test persists and completeness updates.             |
| `AN-MVP-004` | Map a qualitative analyzer value.                                                               | Only active options for that mapped Test are selectable; reload preserves the binding.                    |
| `AN-MVP-005` | Review QC-identification codes and save with explicit fork/update scope.                        | QC recognition is confirmed separately from operational QC; affected analyzers are clear.                 |
| `AN-MVP-006` | Start inline analyzer setup, choose the type, name, and lab units.                              | The URL, breadcrumb, visible section, and saved context remain coherent through reload/history.           |
| `AN-MVP-007` | Verify all mappings and inspect actor/time/revision.                                            | Confirmation is audited and all incomplete/stale items remain explicit blockers.                          |
| `AN-MVP-008` | Configure connectivity and run the connection test.                                             | The probe runs from Bridge; visible success/failure and supported Results only/Two-way choices are clear. |
| `AN-MVP-009` | Select/configure an active operational QC rule and control lot.                                 | QC readiness updates without replacing QC-identification confirmation.                                    |
| `AN-MVP-010` | Review blockers, complete requirements, and activate the analyzer.                              | Activation is blocked before completion and succeeds only after current verification and runtime sync.    |
| `AN-MVP-011` | Use the visible demo control to emit a known patient and QC result through the mock instrument. | Both travel through Bridge and appear in the correct OpenELIS result/QC workflow.                         |
| `AN-MVP-012` | Emit an unknown test/value through the visible demo control.                                    | The result is held, not posted or lost, and the analyzer plus Alerts show Needs attention.                |
| `AN-MVP-013` | Resolve the unknown item and emit the same value again.                                         | Resolution is catalog-safe and audited; the next result maps without another unknown alert.               |
| `AN-MVP-014` | Configure and exercise a FILE profile scenario.                                                 | The lab-facing outcome matches other protocols while Bridge owns watching/transport.                      |
| `AN-MVP-015` | Review the completed analyzer on desktop and mobile and revisit bookmarked routes.              | The summary, breadcrumbs, query state, actions, and responsive Carbon layout remain coherent.             |

The fixture loader may prepare catalog/sample data. The reviewer and Playwright
must execute every user action above through visible controls. Protocol path and
delivery guarantees are proven separately by harness contracts.

## Deployment and Evidence Contract

`/__review/target.json` (with `/__review/build.json` as its compatibility alias)
must identify the verified application repository/ref/SHA, review-tooling SHA,
instance, deployment ID, deployment time, scope, and verification state.

OE-G0's committed RC manifest must add the full transitive build identity:

- OpenELIS image digests and database migration version;
- Analyzer Bridge repository SHA and image digest;
- analyzer-mock repository SHA and image digest;
- profile catalog revision;
- every implementation PR URL and the matching `target.json` deployment ID.

The evidence index contains both documents and verifies that the Bridge/mock
SHAs equal the gitlinks pinned by the deployed OpenELIS SHA. The widget need not
duplicate transitive component metadata that git already identifies.

The report must include schema version, checklist revision, stable step key,
required flag, status, note, marked time, route, actual URL, and the complete
build manifest. Reordering Grist rows must not move answers between step keys.

Run non-video first. Inspect console errors, screenshots, trace, network/runtime
diagnostics, and desktop/mobile images against the current functional/visual
mock intent. This comparison evaluates user outcome, information hierarchy,
Carbon coherence, and responsive behavior only; it supplies no technical
implementation directive. Record the MP4 only after defects are resolved.

## Legacy Removal and Migration Gates

- `defaultConfigId` and copied plugin JSON may be read during migration, but
  cannot remain the final reusable profile authority.
- Existing per-analyzer mappings are fingerprinted and grouped only when their
  effective behavior is identical; divergent snapshots become explicit site
  forks. No silent merge or remap is permitted.
- Legacy free-text result mappings remain readable as `LEGACY_UNBOUND` and
  block verification until catalog-bound.
- After cutover, one writer owns each capability. No dual-write to old and new
  profile/mapping/pending stores is allowed.
- Raw OpenELIS ASTM/HL7/FILE runtime routes and direct mock-to-OpenELIS
  acceptance paths are removed or covered by a priority removal issue before
  MVP acceptance.
- Repository guards prove there is no enabled OpenELIS FILE watcher/poller.

## Resolved Delivery Decisions

The canonical MVP review host is `analyzers.openelis-global.org`. The previously
written `analyzers.openelis-work.org` value is retired and is not a deployment or
Grist-publication target.

No architecture question remains open: Bridge is the analyzer runtime and
portable profile owner; OpenELIS owns local clinical bindings, audit, QC, held
results, and lab-facing orchestration; the mock proves real Bridge transports;
and `openelis-work` remains functional/visual only.
