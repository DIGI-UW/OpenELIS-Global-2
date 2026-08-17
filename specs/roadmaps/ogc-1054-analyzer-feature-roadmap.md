# OGC-1054 Analyzer Feature Authoritative Roadmap

**Updated:** 2026-08-17
**Epic:** [OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054)
**Historical foundation pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)
**First product-review gate:** Full MVP deployed to
[`https://analyzers.openelis-global.org`](https://analyzers.openelis-global.org/login)
with current Grist UAT and MP4 evidence

This document establishes the single engineering roadmap for Analyzer Types,
mapping, guided setup, analyzer QC/configuration, and safe analyzer traffic.
Its iteration markers are the only delivery state. It supersedes the
product-scope claims in the older Analyzer QC/config roadmap and in the PR
#3792 SpecKit set. Those files remain foundation records for git provenance.

The canonical feature artifact set is:

- [functional specification](../OGC-1054-analyzer-qc-config/spec.md);
- [engineering plan](../OGC-1054-analyzer-qc-config/plan.md);
- [dependency-ordered tasks](../OGC-1054-analyzer-qc-config/tasks.md);
- [requirements-quality checklist](../OGC-1054-analyzer-qc-config/checklists/requirements.md);
- [acceptance traceability matrix](../OGC-1054-analyzer-qc-config/contracts/acceptance-matrix.md); and
- [remote UAT mapping](../OGC-1054-analyzer-qc-config/contracts/uat-mapping.md).

This roadmap is the execution control document. The linked feature artifacts
elaborate it and may not override its scope, architecture, checkpoint order, or
acceptance IDs. A conflict is fixed in the owning artifact and this roadmap
before implementation continues.

The implementation is a coordinated three-repository PR train, not one large
cross-cutting PR. OpenELIS, Analyzer Bridge, and analyzer mock each have a
linear stack. Companion PRs at one checkpoint share a contract revision and
must pass together before the next checkpoint starts.

## Non-Negotiable Source Boundary

`DIGI-UW/openelis-work` is a **non-technical product and design source**.
Repository-owned specifications under `specs/` and the functional/visual
artifacts in `openelis-work` are the product sources for this work. Jira is
traceability only: its descriptions, status, and technical-looking prose cannot
supply or override scope, acceptance, architecture, implementation, or
checkpoint state.

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
decisions.

## Operating Principles

1. **Specs and product design first.** Before selecting work, read this roadmap,
   the canonical `specs/OGC-1054-analyzer-qc-config/` artifacts, applicable
   protocol specs under `specs/`, and the current `openelis-work` functional
   requirements and mocks. Jira cannot fill a missing requirement.
2. **Product and engineering stay distinct.** `openelis-work` determines
   user-visible goals, workflow, terminology, states, and visual intent only.
   Repository ownership, persistence, APIs, events, payloads, migration, and
   test-layer ownership come only from repository engineering specs, current
   OE/Bridge/mock code, and approved ADRs or versioned contracts.
3. **Roadmap changes precede implementation changes.** New scope, a changed
   acceptance outcome, a changed ownership boundary, or a changed checkpoint
   dependency requires an approved roadmap/spec amendment before production
   code proceeds. Test results, reviews, and implementation history remain in
   Git and GitHub instead of becoming roadmap bookkeeping.
4. **Git is provenance.** Superseded branches and documents remain in history;
   active branches carry one descendant roadmap lineage. Historical content is
   labeled, not silently rewritten into current truth.
5. **Acceptance is observable.** A route, API response, database row, mocked
   component, old screenshot, or old video cannot prove a lab-facing outcome.
   Each criterion names its proper automated layer and visible UAT check.
6. **Volatile run details stay out of the roadmap.** Branch-head SHAs, CI run
   IDs, deployment IDs, and test timestamps remain in Git/GitHub. The only
   standalone acceptance bundle is G0, where the deployed build, Grist
   revision, screenshots, trace, report, and MP4 must describe one candidate.
7. **TDD is checkpoint-local.** Every behavior starts with a failing test at
   the layer that owns the rule, proceeds to the smallest passing change, is
   refactored, and then gains only the integration/UI coverage needed by the
   acceptance matrix.
8. **No target-architecture bypass.** Bridge remains the portable profile and
   analyzer-runtime owner. OpenELIS remains the lab/clinical orchestration
   owner. The mock proves real analyzer transports. No OpenELIS FILE poller,
   duplicate profile authority, `QcRun`, dual writer, or API-driven Playwright
   acceptance path may be introduced.

## Authority Order

| Question                                           | Authoritative source                                                                                                    |
| -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| What should a lab user accomplish?                 | Repository feature specs plus current `openelis-work` functional requirements, within the source boundary above         |
| What does the visual workflow need to communicate? | Current `openelis-work` mocks plus OpenELIS Carbon conventions                                                          |
| Which repository owns behavior?                    | This roadmap, [`AGENTS.md`](../../AGENTS.md), repository engineering specs, and current OE/Bridge/mock code             |
| How is it persisted or transported?                | An approved repository ADR or versioned contract grounded in current code                                               |
| What exists today?                                 | Current repository code, tests, open PRs, and git history                                                               |
| What is accepted?                                  | Criterion-specific automated verification plus exact-build remote UAT where assigned; never an old route or video alone |

Git is the provenance layer. Revisions to scope or architecture amend the
owning repository specification and this file while retaining earlier decisions
in history; they do not rewrite product documents to fit an implementation
shortcut.

### Relationship to earlier analyzer specifications

The following documents remain useful historical or protocol foundations, but
they do not form a second OGC-1054 plan. Their current top-level notices and the
fixed architecture in this roadmap control whenever older body text conflicts.

| Artifact                              | OGC-1054 role                                                                                                                                        |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `004-astm-analyzer-mapping`           | Historical ASTM mapping foundation; characterize reusable behavior in F0/E0, but do not extend its OpenELIS-owned raw runtime or standalone UI paths |
| `011-madagascar-analyzer-integration` | Historical deployment and protocol inventory; current Bridge/OpenELIS ownership amendment controls                                                   |
| `012-generic-astm-plugin-profiles`    | Historical plugin-config/profile work and migration input; not the target portable-profile authority                                                 |
| `013-hjra-hl7-stream-alignment`       | Historical HL7 coordination and site-readiness record; Bridge owns the target runtime/profile lifecycle                                              |
| `014-hjra-file-stream-alignment`      | FILE protocol foundation; its current ownership amendment controls and older OpenELIS watcher/parser plans are non-executable                        |
| PR #3792 documents and videos         | Historical behavior provenance only; F0 must re-prove any retained behavior under this roadmap                                                       |

## Reference Index

### Functional and visual references only

These references answer what a reviewer should be able to accomplish and what
the experience should communicate. They must not be cited as technical
evidence in an ADR, migration plan, API contract, or code review.

- [Analyzer Types & Mapping functional specification](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/analyzer-profile-mapping.md).
- [Analyzer Types & Mapping functional prototype](https://digi-uw.github.io/openelis-work/designs/analyzer-integration/analyzer-profile-mapping.html).
- [Analyzer Types & Mapping functional gap review](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/analyzer-profile-mapping-gap-analysis.md).
- [OGC-1057 guided setup QA report](https://github.com/DIGI-UW/openelis-work/blob/qa/ogc-1057-guided-setup-report/designs/analyzer-integration/ogc-1057-qa-report.md),
  used as a functional observation of the reviewed demo, not as implementation
  direction or final acceptance proof.
- [Published OpenELIS design catalog](https://digi-uw.github.io/openelis-work/catalog.html),
  used for current visual comparison and neighboring workflow context.

The current analyzer prototype provides a clear desktop information hierarchy
but does not produce a usable layout at the required `390x844` viewport. It is
therefore the functional/desktop visual reference, not a pixel target for
mobile. Mobile acceptance requires the same information and actions in a
coherent Carbon-responsive layout with no overlap, clipping, or unreachable
control; a later mobile mock may refine visual intent without weakening that
gate.

### Engineering and implementation references

These references own architecture, repository boundaries, current behavior,
contracts, migration, and tests.

- [`AGENTS.md`](../../AGENTS.md): analyzer boundary, FILE ownership,
  constitutional architecture, TDD, Carbon, and legacy-removal rules.
- [Canonical OGC-1054 feature specification](../OGC-1054-analyzer-qc-config/spec.md),
  [plan](../OGC-1054-analyzer-qc-config/plan.md), and
  [tasks](../OGC-1054-analyzer-qc-config/tasks.md).
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

### Traceability references only

[OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054) and child identifiers
[OGC-1055](https://uwdigi.atlassian.net/browse/OGC-1055),
[OGC-1056](https://uwdigi.atlassian.net/browse/OGC-1056),
[OGC-1057](https://uwdigi.atlassian.net/browse/OGC-1057), and
[OGC-1058](https://uwdigi.atlassian.net/browse/OGC-1058) connect commits and
pull requests to project tracking. Their descriptions and workflow status are
not inputs to this roadmap.

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
| Full OGC-1054 rollout    | The accepted MVP plus mature alert operations, profile revision/update/rollback, distribution hardening, and exact-build full-feature acceptance                                                                 |
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

This baseline records durable code findings needed to select work. It does not
copy branch heads, CI runs, deployment IDs, or other volatile facts out of Git
and GitHub.

The durable baseline classification is:

- current `develop` supplies analyzer, mapping, pending-code, Bridge sync, and
  operational-QC foundations but not the accepted OGC-1054 workflow;
- PR #3792 is closed historical provenance and is not a merge candidate;
- F0, E0, and M1 branches contain prepared or implemented-but-unaccepted work;
- the analyzer demo and its eight `AN-QC-*` steps are historical evidence, not
  current MVP acceptance; and
- the canonical remote review host is `analyzers.openelis-global.org`.

### Present on OE-R0

- Standalone analyzer list, create/edit, type, field-mapping, QC-rule, and
  control-lot routes. There is no current inline Instrument/Verify/Connect
  story.
- Transitional OpenELIS filesystem profile assets and create-time bootstrap
  behavior. They are migration inputs, not the target profile authority.
- Existing local analyzer mappings, pending-code infrastructure, Bridge desired
  registration, and operational QC entities (`AnalyzerQcRule`, `QCControlLot`,
  `QCResult`, and Westgard).
- Legacy OpenELIS protocol-reader/import paths. R0 does not treat them as the
  target runtime; E0-M4 own migration and removal under the fixed Bridge
  boundary.

### Historical #3792 provenance, not present on OE-R0

The frozen #3792 branch contains iterations of an inline setup shell,
URL-backed guided state, catalog-bound result-option work, verification
metadata, readiness blockers, and deterministic registration payload changes.
Those behaviors may be evaluated directly during F0. They are not current OE-R0
implementation, are not cherry-picked, and do not satisfy any product
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

The baseline claims above are grounded in current OpenELIS, Bridge, and analyzer
mock routes, services, runtime code, fixtures, and tests. `openelis-work`
contributes only the functional and visual outcomes crosswalked below; it does
not supply these implementation conclusions.

| Current-code claim                             | Reproducible code evidence                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ---------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Setup remains separate routed pages            | [`frontend/src/App.jsx`](../../frontend/src/App.jsx) and [`AnalyzersList.tsx`](../../frontend/src/components/analyzers/AnalyzersList/AnalyzersList.tsx) route Add, Edit, Mappings, QC Rules, and Control Lots separately.                                                                                                                                                                                                                                                                                                                                   |
| Qualitative binding is not catalog-safe        | [`QualitativeResultMapping.java`](../../src/main/java/org/openelisglobal/analyzer/valueholder/QualitativeResultMapping.java) and [`QualitativeResultMappingForm.java`](../../src/main/java/org/openelisglobal/analyzer/form/QualitativeResultMappingForm.java) persist free-text `openelisCode`.                                                                                                                                                                                                                                                            |
| Activation is not complete MVP readiness       | [`AnalyzerStatusTransitionServiceImpl.java`](../../src/main/java/org/openelisglobal/analyzer/service/AnalyzerStatusTransitionServiceImpl.java) checks current status and at least one active QC rule, not current mapping verification plus profile-applicable QC readiness.                                                                                                                                                                                                                                                                                |
| OpenELIS still loads bootstrap profiles        | [`AnalyzerRestController.java`](../../src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java) reads filesystem profiles and applies `defaultConfigId` at create time. E0 owns migration away from profile authority in core OpenELIS.                                                                                                                                                                                                                                                                                             |
| Operational QC foundation exists               | [`AnalyzerQcRule.java`](../../src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerQcRule.java), [`QCControlLot.java`](../../src/main/java/org/openelisglobal/qc/valueholder/QCControlLot.java), and [`QCResult.java`](../../src/main/java/org/openelisglobal/qc/valueholder/QCResult.java) are the retained operational QC path.                                                                                                                                                                                                                  |
| Bridge already owns runtime concerns           | Bridge [`AnalyzerRegistrationController`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/AnalyzerRegistrationController.java), [`TestConnectivityController`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/TestConnectivityController.java), and [`FileWatcher`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/file/FileWatcher.java) own registration, probes, and FILE watching today. |
| Bridge profile lifecycle is still absent       | Current Bridge `develop` registers analyzer entries and OE-pushed mappings but has no portable catalog/revision/fork lifecycle; BR-E0/BR-M1 own the contract and implementation rather than recreating it in OpenELIS.                                                                                                                                                                                                                                                                                                                                      |
| Mock is multi-protocol but has legacy delivery | Current analyzer-mock [templates](https://github.com/DIGI-UW/analyzer-mock-server/tree/main/templates) cover ASTM, HL7, and FILE, while its [README](https://github.com/DIGI-UW/analyzer-mock-server/blob/main/README.md) still documents direct-to-OpenELIS delivery. M4 uses real transport to Bridge and retires direct delivery as acceptance proof.                                                                                                                                                                                                    |

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
6. request a live result during Verify, reconcile every seen/not-seen/new item,
   and populate a blank site type from held traffic without losing anything;
7. receive a known patient result and a QC result through Bridge from the
   analyzer mock;
8. hold and visibly flag an unknown test/value, resolve it safely, and process
   the next matching result deterministically; and
9. reload, bookmark, navigate by breadcrumb, and review the same durable state.

The MVP includes a discoverable Alerts/Needs attention path. A resolver hidden
inside an analyzer page is not enough for safe operation.

### Full OGC-1054 rollout

After MVP acceptance, complete:

- mature alert triage, acknowledgement, assignment, concurrency, and
  navigation;
- profile revision diff, update impact, rollback, backup export, and
  distribution hardening;
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

| Product slice                                                             | Functional/visual reference                                                        | Current code state                                                               | Delivery checkpoint      |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------ |
| [OGC-1055](https://uwdigi.atlassian.net/browse/OGC-1055) Analyzer Types   | Reuse, create/fork, completeness, usage, lifecycle, and list presentation          | Transitional shipped-profile/type page; site lifecycle absent                    | M1                       |
| [OGC-1056](https://uwdigi.atlassian.net/browse/OGC-1056) mapping          | Complete test/result/QC-code editor and safe save scope                            | Legacy standalone mapping/pending paths; accepted catalog-bound editor absent    | M2                       |
| [OGC-1057](https://uwdigi.atlassian.net/browse/OGC-1057) guided setup     | Inline Instrument, Verify, and Connect sections plus a readable completion summary | Standalone routes only; current activation does not implement full readiness     | M3                       |
| [OGC-1058](https://uwdigi.atlassian.net/browse/OGC-1058) traffic learning | Hold, alert, resolve, and reconcile unknown traffic                                | Pending/error infrastructure exists; production hold/alert/reconciliation absent | M4 core; R1.1 operations |
| PR #3792 QC/config extension                                              | Historical behavior provenance only                                                | Frozen divergent branch; F0 evaluates behavior directly before reuse             | F0 review                |

Issue identifiers in this table are traceability labels only. Scope and
dependency are defined by the repository specifications and this roadmap, not
by external workflow status.

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
| AC-6       | Human mapping confirmation is mandatory and auditable.                                      | Absent on OE-R0.                                                   | M2 + M3 |
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
| AC-21      | Setup can request a live result and reconcile what was received.                            | Absent.                                                            | M4      |
| AC-22      | Unknown data is never lost; resolution changes future matching behavior.                    | Absent end to end.                                                 | M4      |
| AC-23      | Live traffic can populate a newly created blank type.                                       | Absent.                                                            | M4      |

### OGC-1057 QA finding disposition

The OGC-1057 QA report was reviewed from its current `openelis-work` QA branch.
Its browser observations are functional input. Its REST checks, named
endpoints, implementation diagnosis, and proposed technical remedies are not
imported as engineering authority. The report contains no screenshot or video
assets, so it does not replace the M3/G0 visual comparison and acceptance
gates.

| QA finding  | Deterministic roadmap disposition                                                                                                                                             | Gate                                  |
| ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| 1           | Every source row can be added, edited, removed, or repointed through complete active-Test catalog search.                                                                     | M2; MVP-005/006; `AN-MVP-003`         |
| 2           | Applying or migrating a profile never drops, hides, collapses, or falsely marks an unmatched source row complete.                                                             | E0 + M2; MVP-005/006                  |
| 3           | Changing an applicable result option enables save, persists, and survives reload; invalid and empty-option rows remain explicit.                                              | M2; MVP-007; `AN-MVP-004`             |
| 4           | Test selection searches the complete active local catalog by name, code, or LOINC; no fixed legacy subset can satisfy acceptance.                                             | M2; MVP-006; `AN-MVP-003`             |
| 5           | Source aliases and local LOINC cardinality are characterized; ambiguous candidates never auto-bind and source rows remain independently confirmable.                          | E0 + M2                               |
| 6           | Mapping/QC-identification confirmation is independent of operational QC readiness; each has its own visible completion and blocker state.                                     | M2 + M3; MVP-008/012/015/016          |
| 7           | Instrument not listed creates a reusable site type through the Bridge-owned lifecycle without developer fields.                                                               | M1 + M3; MVP-003/010; `AN-MVP-002`    |
| 8           | Method-dependent control-lot requirements are visible before submit and exact validation is actionable; valid save recomputes readiness.                                      | M3; MVP-015/022; `AN-MVP-009`         |
| 9           | Type lifecycle uses deactivate/reactivate in M1; M2 removes Copy Mappings; M3 replaces analyzer-instance hard delete with audited deactivate/reactivate.                      | M1 + M2 + M3; MVP-004/009             |
| Deferred    | Real mock-to-Bridge probes cover role-appropriate settings, success, failure, timeout, supported direction, and visible Results-only degradation.                             | BR-M2 + M3 + MOCK-M4; MVP-013/014/021 |
| Untested    | M4 owns live capture/reconciliation, blank-type population, hold/alert/resolve, and deterministic next-message behavior for unknown traffic.                                  | M4; MVP-018/019/020                   |
| Preserve    | Inline setup, searchable selection, readable summaries, lab units, live blockers, catalog-safe result choices, and absence of developer fields remain functional regressions. | M1-M3; MVP-001/007/010/016/022        |
| Withdrawn   | The report's withdrawn picker-search and direction-default observations add no defect requirement; later UI automation must use focused visible controls.                     | M3/G0; MVP-010/014/023                |
| Environment | G0 starts from a deterministic reset/fixture state; artifacts left by the 2026-08-12 review are preconditions to remove, not product history to hard-delete through the UI.   | G0 deployment preflight               |

## Execution Contract

These are engineering validation milestones, not partial product acceptance.
The first product review requested from the user is G0, the full deployed MVP.
There is one next acceptance checkpoint at a time and no architecture-selection
prompt in the workstream. Descendant branches may be prepared while a
predecessor is reviewed, but acceptance and merge remain strictly ordered.

### Prime iteration-marker rule

The roadmap has exactly three state markers and no other workflow state:

- `[x]` **finished**: the iteration's full exit gate is satisfied, required
  review is complete, and its PR is merged on the canonical target;
- `[*]` **active**: the one iteration currently allowed to change; and
- `[ ]` **future**: every iteration that has not started, including a branch
  that was prepared early or contains unaccepted code.

Exactly one iteration is `[*]` until the roadmap is fully finished. A marker
changes only in two cases: `[ ]` to `[*]` when the preceding iteration becomes
`[x]` and the next iteration formally starts, or `[*]` to `[x]` when its entire
exit gate is proven and merged. A commit, passing test, open PR, review request,
deployment, or prepared descendant branch does not change a marker.

All finer-grained facts, including implementation progress, red/green commits,
CI, review readiness, and branch heads, already live in Git and GitHub. They are
deliberately not copied into roadmap state or a parallel checkpoint ledger.

An executor given the goal “execute this roadmap through deployed MVP” must:

1. fetch all three repository bases and read their local `AGENTS.md` files;
2. resume the single checkpoint marked `[*]`;
3. create or reuse exactly the branch and base named below;
4. record a failing test before production implementation, implement to green,
   refactor, and keep that provenance in the checkpoint's commits and PR;
5. change this roadmap's marker only when the iteration formally starts or
   finishes;
6. do not begin production work for a `[ ]` checkpoint; preserving already
   prepared descendant work does not make it active;
7. stop only for a permission/credential boundary, a required external review
   or merge, or current code/contract behavior that contradicts this fixed
   architecture.

### Roadmap provenance invariant

This path has one active lineage. R0 establishes the authority; each later
OpenELIS checkpoint may update stable scope, acceptance, and the three-state
iteration marker only as a Git descendant of the merged predecessor. A sibling
or historical branch may retain an older blob as provenance, but it is never an
active roadmap and is never merged as a competing version. Git history
preserves PR #3792 as historical input; it receives no roadmap edits and is
never an implementation base.

After a prerequisite merges, rebase the dependent branch on current
`develop`/`main`, retarget its PR, and rerun its gates. Never mark an iteration
finished because code merely exists.

Each checkpoint is a manageable implementation and review unit. Work within a
checkpoint proceeds one independently testable behavior at a time. Start at the
lowest layer that owns the rule, add integration or contract coverage only when
the behavior crosses a persistence or repository boundary, add RTL coverage for
user interaction and route state, and reserve Playwright for an assembled
visible user story. Repeating a mocked assertion at every layer is not evidence
of integration.

### Acceptance proof

No separate checkpoint files are required. The roadmap defines the exit gate;
tests, commits, CI, review, and merge remain where they naturally live in Git
and GitHub. A checkpoint PR links its assigned acceptance IDs and contains the
red-green-refactor history needed for review. Only G0 creates a standalone
bundle because remote human UAT and MP4 must be bound to one exact deployment.

An ambiguity is blocking when resolving it could change clinical safety,
repository ownership, durable data semantics, a cross-repository contract, or
an acceptance criterion. Resolve it in the owning specification or contract
before the affected iteration starts.

### Blocking decision

`AMB-M3-001` blocks the start of M3: the repository specification must identify
the source of profile-applicable operational-QC obligations and whether an
active operational QC rule, active control lot, or both are activation
requirements. No implementation may infer that policy from copied profile
defaults or the historical demo. QC-identification confirmation remains a
separate mapping concern regardless of that decision.

### Iterations

- [*] **R0 - Authoritative roadmap.** Finish and merge the governing artifact
  set from current `develop`.
- [ ] **F0 - Deterministic foundation salvage.** Prepared branch/PR exists but
      remains future until R0 is `[x]`.
- [ ] **E0 - Engineering contract and migration characterization.** Prepared
      OE/Bridge branches exist but remain future until F0 is `[x]`.
- [ ] **M1 - Bridge profile lifecycle and Analyzer Types.** Prepared work is
      preserved but remains future until E0 is `[x]`.
- [ ] **M2 - Safe mapping editor.** Future.
- [ ] **M3 - Guided setup, connectivity, and QC.** Future; cannot start until
      `AMB-M3-001` is resolved in the owning specification/contract.
- [ ] **M4 - Safe traffic and integrated MVP.** Future.
- [ ] **G0 - Full MVP deployment and human acceptance.** Future.
- [ ] **R1.1 - Mature alert operations.** Future.
- [ ] **R1.2 - Profile revision and distribution operations.** Future.
- [ ] **R1-G - Full-feature deployment and human acceptance.** Future.
- [ ] **R2 - Operational rollout.** Future.

Prepared descendants do not create parallel active roadmap blocks. Their
actual state remains visible in Git and their pull requests.

## Pull Request Train

PR #3792 is not a stack. It is frozen historical provenance and a behavior
source for F0. It is not rebased, merged, or used as a branch base. Once the F0
replacement PR is open, rename #3792 to “OGC-1054 historical analyzer QC/config
foundation (superseded)”, link R0/F0, and close it as superseded without deleting
its branch.

### OpenELIS stack

| Order | ID    | Fixed branch                           | Initial PR base | Scope                                                                       |
| ----- | ----- | -------------------------------------- | --------------- | --------------------------------------------------------------------------- |
| 0     | OE-R0 | `codex/ogc-1054-r0-roadmap`            | `develop`       | This roadmap and current engineering ownership amendments only              |
| 1     | OE-F0 | `codex/ogc-1054-f0-foundation`         | OE-R0 branch    | Characterize and cleanly salvage compatible #3792 foundation behavior       |
| 2     | OE-E0 | `codex/ogc-1054-e0-contract-migration` | OE-F0 branch    | ADR, consumer contracts, migration fixtures/report, red cross-repo tests    |
| 3     | OE-M1 | `codex/ogc-1054-m1-analyzer-types`     | OE-E0 branch    | OpenELIS Analyzer Types composition, site bindings, lifecycle UI, migration |
| 4     | OE-M2 | `codex/ogc-1054-m2-mapping`            | OE-M1 branch    | Complete protocol-neutral mapping and QC-identification confirmation        |
| 5     | OE-M3 | `codex/ogc-1054-m3-setup-qc`           | OE-M2 branch    | Guided setup, Bridge connectivity, operational QC, activation               |
| 6     | OE-M4 | `codex/ogc-1054-m4-safe-traffic`       | OE-M3 branch    | Hold/alert/resolve, integrated harness, legacy removal, full UI story       |
| 7     | OE-G0 | `codex/ogc-1054-g0-acceptance`         | OE-M4 branch    | Exact-build deployment, Grist UAT, MP4, and acceptance corrections          |

### Post-MVP OpenELIS stack

| Order | ID      | Fixed branch                                | Initial PR base | Scope                                                          |
| ----- | ------- | ------------------------------------------- | --------------- | -------------------------------------------------------------- |
| 8     | OE-R1.1 | `codex/ogc-1054-r1-alert-operations`        | merged OE-G0    | Triage, assignment, concurrency, and navigation                |
| 9     | OE-R1.2 | `codex/ogc-1054-r1-profile-revisions`       | merged OE-R1.1  | Diff, update impact, rollback, backup export, and distribution |
| 10    | OE-R1-G | `codex/ogc-1054-r1-full-feature-acceptance` | merged OE-R1.2  | Exact-build full-feature UAT and acceptance fixes              |

Bridge and mock companions for R1 are opened only when the first failing
versioned contract proves that repository must change. The owning OpenELIS PR
links and pins each required companion before merge; an unneeded repository
does not get an empty placeholder PR.

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

OpenELIS PRs that depend on a companion update the ordinary submodule pointer
and link its PR. A companion merges before the first OpenELIS consumer that
requires it. The consumer is then rebased and reruns contracts. No empty
companion PR is created: if a repository's current implementation already
passes the checkpoint contract, no repository change is needed.

The MVP acceptance order is:

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

Prepared branches do not alter roadmap state. Only the item marked `[*]` is
active, and a later item cannot merge or become the deployment candidate before
every earlier item is `[x]`. This order, not PR creation time, selects work.

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
2. Select behavior directly from current code and PR #3792 history. Retain it
   only when a failing characterization test ties it to an MVP criterion and it
   obeys the fixed architecture. Do not create a parallel salvage ledger or
   cherry-pick a multi-purpose feature commit.
3. Reject copied-profile authority, app-owned analyzer runtime, and duplicate
   mapping/pending editors from F0 salvage, and assign their migration/removal
   to the owning E0-M4 checkpoint. In F0, guard API-focused Playwright, stale
   acceptance claims, and any new extension of a legacy path.
4. Explain retained and rejected behavior in the PR review itself, backed by
   the tests and commits that implement the decision.
5. Update and close #3792 as superseded after OE-F0 is open.

**Exit:** OE-F0 is a small green replacement foundation; every retained behavior
has a test and every rejected behavior has a reason or legacy guard. #3792 is no
longer an open delivery candidate.

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

**Exit:** OE-E0 and BR-E0 are green; approved ADR/contracts, migration fixtures,
rollback behavior, and contract tests exist. No M1 production code starts
against an unresolved boundary.

### M1 - Bridge profile lifecycle and Analyzer Types (OGC-1055)

1. In BR-M1, implement the Bridge-owned versioned profile catalog and lifecycle
   against the accepted BR-E0 contract.
2. In OE-M1, compose one lab-facing Analyzer Types view from Bridge profile
   metadata plus OpenELIS local completeness/readiness/usage, with the
   plain-language explainer and aggregate counts shown by the functional mock.
3. Support shipped and site-created types, fork, unique naming, lineage,
   deactivate/reactivate, and audit/history.
4. Make search and filters URL-backed and use reusable Carbon page, breadcrumb,
   status, table, empty-state, and notification components.
5. Migrate existing analyzers to a profile revision plus explicit local site
   binding without silent remapping.

**Exit:** MVP-001 through MVP-004, MVP-011, and applicable MVP-022 criteria pass;
a type is reusable by multiple analyzers and no OpenELIS filesystem catalog or
create-only copied snapshot remains authoritative.

### M2 - Safe mapping editor (OGC-1056)

1. In BR-M2, expose the normalized identities, raw codes, QC-identification
   codes, capabilities, and revision needed by the consumer contract.
2. In OE-M2, show every profile test row, including unmatched rows, with raw
   code, normalized identity, match state, and local Test selection. Shared
   normalized identities never collapse distinct source rows.
3. Add/edit/remove/repoint test bindings using complete active Test catalog
   search by name, code, or LOINC. Suggest a binding only for one unique active
   candidate; zero or multiple candidates remain visibly unresolved. A row may
   be explicitly marked “do not receive”; one unresolved row never hides or
   blocks confirmation work on independent rows.
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

1. Complete one inline Instrument -> Verify -> Connect story in OE-M3 with
   canonical URL/query state, linkable breadcrumbs, a readable completion
   summary, reload, back, and forward behavior. Do not add a fourth setup
   section unless the functional specification is amended.
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
7. Block activation with a complete visible list of current blockers. A source
   row is ready when it is validly bound or explicitly excluded and confirmed;
   the workflow does not require a false 100% mapping claim.

**Exit:** MVP-010 through MVP-016 and applicable MVP-022 criteria pass; a lab
administrator can activate a complete analyzer without developer fields or file
edits, and runtime setup occurs in Bridge.

### M4 - Safe traffic and integrated MVP (OGC-1058 safety scope)

1. In BR-M4, preserve raw context and normalized identity for known, unknown,
   QC, and FILE messages.
2. In MOCK-M4, supply deterministic real-transport fixtures; direct
   mock-to-OpenELIS delivery cannot satisfy a target-architecture test.
3. In OE-M4, stage known patient and QC results through the unified FHIR path.
4. Make Verify's visible “send a result” action capture real mock-to-Bridge
   traffic, reconcile every transmitted item as verified/new/not-seen, and
   keep independent source rows intact.
5. Populate a blank site type from received test/value/QC-identification rows;
   require explicit catalog-safe binding and confirmation before use.
6. Hold unknown tests/values; never discard or clinically post them.
7. Create durable Alerts/Needs attention linked to analyzer, profile revision,
   held result, and mapping action.
8. Resolve only through valid local catalog choices, audit the decision, and
   prove the next matching message maps deterministically.
9. Add one full UI-only Playwright story named
   `frontend/playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts` and
   register focused non-video/video scripts.
10. Remove or disable every superseded raw reader, copied-profile writer,
    duplicate editor, and duplicate pending queue; no dual-write or alternate
    acceptance path remains enabled.

**Exit:** MVP-017 through MVP-023 pass, all prior criteria remain green, and the
top of the three repository stacks forms one reproducible release candidate.

### G0 - Full MVP deployment and first product review

1. Create OE-G0 from OE-M4 and verify its ordinary Git/submodule state selects
   the intended OpenELIS, Bridge, mock, and profile-catalog versions.
2. Deploy the immutable OE-G0 commit only to
   `analyzers.openelis-global.org` with the merged review-tooling targeted
   deployment command.
3. Load deterministic fixtures as a deployment precondition. No Playwright user
   story may seed or mutate state through an API.
4. Create/publish the Grist story and steps below through native Grist MCP or
   the Grist UI, verify the served revision, and keep old foundation steps
   unpublished rather than deleting or renaming them.
5. Run the focused non-video remote UI story first. Inspect test output, browser
   console, screenshots, trace, runtime state, and desktop/mobile captures.
6. Compare captures with the current `openelis-work` functional/visual intent
   only, without treating it as implementation authority.
7. Fix every required failure and repeat from step 2 when the application build
   changes.
8. Run the matching video project only after the inspected non-video run is
   clean.
9. Retain the generated target metadata, Markdown/JSON UAT reports, MP4,
   screenshots, and trace together as the final UAT deliverable.

The exact deployment sequence from a clean
`DIGI-UW/openelis-review-tooling@main` checkout is:

```bash
./deploy.sh app deploy analyzers --ref <OE_G0_COMMIT> --scope app
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
trace inspection are clean. If any implementation build changes, redeploy and
restart the acceptance run; acceptance never transfers between deployments.

**Exit:** MVP-024 passes; every required Grist step is `pass` against the exact
ready deployment; the MP4 shows the complete visible story; all three PR stacks
are green and reviewable. This is the first point labeled **OGC-1054 MVP
accepted**.

### R1 - Full OGC-1054 feature rollout

R1 is a second linear stack after the accepted MVP. These blocks are delivered
in order and remain `[ ]` while R0-G0 is active:

1. **R1.1 - Mature alert operations.** Add queue triage, acknowledgement,
   assignment, concurrent-edit protection, and durable navigation from an alert
   to its analyzer/type/mapping context.
   **Exit:** integration and UI tests prove no alert is lost, double-resolved,
   or detached from its held result.
2. **R1.2 - Profile revision and distribution operations.** Show revision
   differences and impact, apply updates to selected analyzers, preserve an
   audited rollback path, provide backup/support export, and harden distribution
   of shipped/site profiles. Import/community sharing remains outside this
   roadmap unless the functional specification is amended.
   **Exit:** Bridge/OE contract and migration tests prove deterministic update,
   rollback, faithful export, stale verification, and affected-analyzer
   reporting.
3. **R1-G - Full-feature acceptance.** Deploy one exact candidate, publish a
   separately versioned full-rollout Grist story, run the complete UI-only
   Playwright flow, inspect desktop/mobile output, and record MP4 after the
   non-video run is clean.
   **Exit:** every required full-rollout step passes for the unchanged build.

### R2 - Operational rollout

Validate scale, disaster recovery, security, monitoring, documentation, and
representative site deployments. Track each instrument with its vendor-grounded
integration spec, companion guide, Bridge profile, mock fixture, contract tests,
and site validation.

## Deterministic MVP Acceptance Criteria

| ID      | Criterion                                                                                                                                                                                                                                                                           | Primary proof                                   |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| MVP-001 | Analyzer Types lists shipped and site types with a plain-language explainer, aggregate counts, source, status, completeness, usage, and attention state.                                                                                                                            | Service/integration + RTL + UI E2E              |
| MVP-002 | Search and filters round-trip through the URL and restore identical visible state after reload/back/forward.                                                                                                                                                                        | RTL with router + UI E2E                        |
| MVP-003 | A user can create a site type or fork a shared type; lineage, unique name, actor, and revision are durable.                                                                                                                                                                         | Bridge contract + OpenELIS integration + UI E2E |
| MVP-004 | Deactivation prevents new use but preserves existing history; reactivation is audited; hard delete is unavailable.                                                                                                                                                                  | Service/integration + RTL                       |
| MVP-005 | The editor displays every independent profile source row, including unmatched/aliased rows; local lookup never skips, collapses, or falsely completes one.                                                                                                                          | Contract + service + RTL                        |
| MVP-006 | Test selection searches the complete active catalog by name/code/LOINC; only a unique candidate may be suggested, while unresolved/explicitly-excluded choices remain independent per row and never block work on other rows.                                                       | JUnit 4 + RTL + UI E2E                          |
| MVP-007 | Every applicable qualitative row can select only an active Result Option owned by its Test; selection enables save, server-derived value/label persist, and reload restores the binding.                                                                                            | JUnit 4 + RTL + UI E2E                          |
| MVP-008 | QC-identification codes are shown and confirmed separately from operational QC rules/lots.                                                                                                                                                                                          | Bridge/OpenELIS contract + RTL + UI E2E         |
| MVP-009 | Saving a shared mapping requires explicit fork/update scope and names affected analyzers; no copy/clone action bypasses that lifecycle.                                                                                                                                             | Service + RTL + UI E2E                          |
| MVP-010 | Inline setup supports type selection, instrument-not-listed, name, readable lab units, Verify, Connect, and a completion summary; profile selection visibly confirms the loaded profile plus mapping/QC/result counts, and saved analyzer name/lab-unit assignments survive reload. | RTL + UI E2E                                    |
| MVP-011 | Every page has one semantic `h1`, linkable breadcrumbs, and canonical URL/query state.                                                                                                                                                                                              | RTL + UI E2E                                    |
| MVP-012 | Mapping/QC-identification confirmation records actor/time/revision/fingerprint and becomes stale only after relevant profile, binding, or identification change, not operational QC.                                                                                                | JUnit 4 integration + audit assertion           |
| MVP-013 | Bridge connection testing uses only role-applicable settings and returns visible success, failure, missing-configuration, and timeout evidence plus the endpoint to configure.                                                                                                      | Bridge test + RTL + UI E2E                      |
| MVP-014 | Results only is the default; Two-way appears only when supported and a failed round-trip visibly degrades to Results only without blocking one-way setup.                                                                                                                           | Bridge contract + RTL + UI E2E                  |
| MVP-015 | Operational QC uses existing QC entities only; applicable required fields and exact validation are visible, valid saves immediately recompute independent QC readiness.                                                                                                             | JUnit 4 analyzer/QC + RTL + UI E2E              |
| MVP-016 | Activation is rejected until every source row is currently confirmed as validly bound or explicitly excluded, required QC is ready, and runtime registration is synchronized.                                                                                                       | JUnit 4 + contract + UI E2E                     |
| MVP-017 | Bridge registration/profile sync is versioned, idempotent, deterministic, and emits explicit empty collections.                                                                                                                                                                     | Cross-repo contract tests                       |
| MVP-018 | During Verify, visible live capture reconciles transmitted items as verified, new, or not seen; known patient and QC results travel mock -> Bridge -> FHIR -> OpenELIS and become visible in the correct workflow.                                                                  | Mock + Bridge + harness integration + UI E2E    |
| MVP-019 | Unknown test/value traffic retains raw context, is held, creates visible attention/alert state, populates an unbound row during setup where applicable, and is not clinically posted.                                                                                               | Contract + OpenELIS integration + UI E2E        |
| MVP-020 | A blank site type can be populated from held live traffic; resolution uses valid local catalog choices, is audited, and makes the next matching message deterministic.                                                                                                              | Integration + UI E2E                            |
| MVP-021 | ASTM, HL7, and FILE each have known, unknown, QC, and connection fixtures; FILE runtime watching occurs only in Bridge.                                                                                                                                                             | Mock/Bridge suites + repository guard           |
| MVP-022 | All user copy is localized; Carbon components/tokens are used; desktop/mobile layouts have no overlap or unreachable action.                                                                                                                                                        | RTL/a11y + inspected screenshots                |
| MVP-023 | Playwright performs the complete visible story without `page.request`, API assertions, backend polling, forced controls, or arbitrary waits.                                                                                                                                        | Playwright guard + test audit                   |
| MVP-024 | Generated target metadata and Git/submodule state identify the exact OpenELIS, Bridge, mock, profile, and review-tooling builds plus deployment time, checklist revision, routes, mark times, screenshots, trace, and MP4.                                                          | Target verifier + UAT report                    |

## Test Strategy

Each checkpoint's Git history and PR checks must show the first failing test,
passing implementation, and refactor result. A route or endpoint existing is
never a functional proof.

| Layer                     | Owns                                                                                                  | Required approach                                                                    |
| ------------------------- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Bridge unit/service       | Profile validation/versioning, parsing, transport, QC identification, probes, idempotent registration | Bridge repository test conventions; real protocol fixtures                           |
| OpenELIS unit/service/DAO | Local catalog constraints, site binding, audit, stale verification, readiness, hold/resolve           | JUnit 4; ORM validation for new mappings; real Postgres where query behavior matters |
| Cross-repo contracts      | Registration, profile revision, normalized FHIR, raw unknown context, QC, FILE delivery               | Versioned fixtures run by both producer and consumer                                 |
| Analyzer mock             | Reproducible ASTM/HL7/FILE known, unknown, QC, failure, and two-way scenarios                         | `pytest`; deterministic IDs and values; transport to Bridge                          |
| Harness integration       | OpenELIS + Bridge + mock + database assembled behavior                                                | Real containers and transport; assert durable outcomes, not internal mocks           |
| Frontend                  | Carbon composition, accessibility, validation, routing/query/breadcrumb state                         | Vitest/RTL with real router context and minimal network stubs at component boundary  |
| Playwright                | Complete lab-facing story                                                                             | Visible UI only; seed may establish preconditions but cannot perform the story       |
| Remote UAT                | Human acceptance and visual coherence                                                                 | Grist overlay against exact build, inspected outputs, final MP4                      |

These commands are the full G0 acceptance contract. Each checkpoint runs its
currently applicable subset and leaves the result in its ordinary PR/CI. OE-M4
owns creation of the named final
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
status, and the final acceptance package.

## Full MVP Grist Checklist

Create one published Grist story for instance `analyzers`, host
`analyzers.openelis-global.org`, traceability key `OGC-1054`, and the full MVP
PR links. Create these stable, required steps only when OE-G0 is deployed. Keep
old foundation stories unpublished; do not rename their keys or reuse their
answers.

| Step key     | Reviewer action                                                                    | Expected result                                                                                           |
| ------------ | ---------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `AN-MVP-001` | Open Analyzer Types, search/filter, and inspect one shipped type.                  | Source, status, completeness, usage, protocol, mapping, QC, and attention information is understandable.  |
| `AN-MVP-002` | Create a site type through Instrument not listed, or fork a shared type.           | A uniquely named audited revision is created without developer fields or file edits.                      |
| `AN-MVP-003` | Open the mapping editor and resolve an unmatched test.                             | Every source row remains visible; the selected active Test persists and completeness updates.             |
| `AN-MVP-004` | Map a qualitative analyzer value.                                                  | Only active options for that mapped Test are selectable; reload preserves the binding.                    |
| `AN-MVP-005` | Review QC-identification codes and save with explicit fork/update scope.           | QC recognition is confirmed separately from operational QC; affected analyzers are clear.                 |
| `AN-MVP-006` | Start inline analyzer setup, choose the type, name, and lab units.                 | The URL, breadcrumb, visible section, and saved context remain coherent through reload/history.           |
| `AN-MVP-007` | Verify all mappings and inspect actor/time/revision.                               | Confirmation is audited and all incomplete/stale items remain explicit blockers.                          |
| `AN-MVP-008` | Configure connectivity and run the connection test.                                | The probe runs from Bridge; visible success/failure and supported Results only/Two-way choices are clear. |
| `AN-MVP-009` | Select/configure an active operational QC rule and control lot.                    | QC readiness updates without replacing QC-identification confirmation.                                    |
| `AN-MVP-010` | Review blockers, complete requirements, and activate the analyzer.                 | Activation is blocked before completion and succeeds only after current verification and runtime sync.    |
| `AN-MVP-011` | In Verify, request a live known result and inspect reconciliation.                 | Seen items verify, absent items stay not seen, and new items remain explicit without data loss.           |
| `AN-MVP-012` | Start a blank site type, request live traffic, and bind the populated rows.        | Received rows populate visibly, remain held, and require explicit valid catalog choices before use.       |
| `AN-MVP-013` | Emit a known patient result and QC result through the visible mock control.        | Both travel through Bridge and appear in the correct OpenELIS result/QC workflow.                         |
| `AN-MVP-014` | Emit an unknown test/value through the visible mock control.                       | The result is held, not posted or lost, and the analyzer plus Alerts show Needs attention.                |
| `AN-MVP-015` | Resolve the unknown item and emit the same value again.                            | Resolution is catalog-safe and audited; the next result maps without another unknown alert.               |
| `AN-MVP-016` | Configure and exercise a FILE profile scenario.                                    | The lab-facing outcome matches other protocols while Bridge owns watching/transport.                      |
| `AN-MVP-017` | Review the completed analyzer on desktop and mobile and revisit bookmarked routes. | The summary, breadcrumbs, query state, actions, and responsive Carbon layout remain coherent.             |

The fixture loader may prepare catalog/sample data. The reviewer and Playwright
must execute every user action above through visible controls. Protocol path and
delivery guarantees are proven separately by harness contracts.

## Final Deployment And UAT Contract

`/__review/target.json` (with `/__review/build.json` as its compatibility alias)
must identify the verified application repository/ref/commit, review-tooling
commit, instance, deployment ID, deployment time, scope, and verification
state. The deployed OpenELIS commit and its ordinary submodule pointers identify
the selected Bridge and mock source; deployment status supplies image digests
and database migration version; Bridge supplies the active profile-catalog
version. No duplicate build ledger is needed.

The report must include schema version, checklist revision, stable step key,
required flag, status, note, marked time, route, actual URL, and the target
metadata. Reordering Grist rows must not move answers between step keys.

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
  paths are removed or disabled before M4 exits. A follow-up issue cannot waive
  this gate.
- A read-only migration adapter may exist only while its owning migration is
  incomplete; it cannot accept new writes, appear in lab workflows, or remain
  enabled at G0.
- Repository guards prove there is no enabled OpenELIS FILE watcher/poller.

## Resolved Delivery Decisions

The canonical MVP review host is `analyzers.openelis-global.org`. The previously
written `analyzers.openelis-work.org` value is retired and is not a deployment or
Grist-publication target.

No architecture question remains open: Bridge is the analyzer runtime and
portable profile owner; OpenELIS owns local clinical bindings, audit, QC, held
results, and lab-facing orchestration; the mock proves real Bridge transports;
and `openelis-work` remains functional/visual only.
