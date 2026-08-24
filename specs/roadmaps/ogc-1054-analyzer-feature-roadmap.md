# OGC-1054 Analyzer Feature Authoritative Roadmap

**Updated:** 2026-08-24
**Epic:** [OGC-1054](https://uwdigi.atlassian.net/browse/OGC-1054)
**Historical foundation pull request:** [#3792](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3792)
**Review target:** CI-green checkpoint PR builds are published to
[`https://analyzers.openelis-global.org`](https://analyzers.openelis-global.org/login)
for iterative product review; only G0 can accept the full MVP

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
- [remote UAT mapping](../OGC-1054-analyzer-qc-config/contracts/uat-mapping.md); and
- [grounded profile-system remediation report](./ogc-1054-profile-system-remediation-report-2026-08-19.md).

This roadmap is the execution control document. The linked feature artifacts
elaborate it and may not override its scope, architecture, checkpoint order, or
acceptance IDs. A conflict is fixed in the owning artifact and this roadmap
before implementation continues.

The implementation is a coordinated, ownership-based PR train, not one large
cross-cutting PR. OpenELIS, Analyzer Bridge, and analyzer mock each keep a
linear stack when that repository must change. Companion PRs at one checkpoint
share a contract revision and must pass together before the next checkpoint
starts; a repository whose existing code passes the contract gets no empty PR.

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
9. **Evolve the established profile system.** The working Bridge-owned profile
   model is the implementation baseline. Profile ownership, strict validation,
   immutable revisioning, and management UX are additive hardening around that
   system. They never justify a second profile schema or loss of established
   communication/default behavior. GeneXpert ASTM and FluoroCycler are blocking
   compatibility fixtures for every profile-contract change.
10. **Profiles are data, never production special cases.** No production OE,
    Bridge, or mock code may special-case a hard-coded profile ID/revision,
    manufacturer, model, display name, analyzer code, fixture name, or vendor-
    specific value, or duplicate profile-owned defaults in source constants.
    Generic lookup by profile data/pins is expected. Named profiles appear only
    as profile data and parameterized fixtures; all validators, consumers,
    handlers, and UI composition are generic.
11. **Reviewable work is published before merge.** Once a coherent checkpoint
    candidate is pushed in its owning PRs and its applicable automated gates are
    green, deploy that exact stacked candidate to the analyzer review instance
    and sync its applicable Grist steps. Every active checkpoint opens its
    owning PR before its first preview deployment; a branch-only build is never
    a review target. Review feedback is fixed in the same checkpoint. A preview
    deployment is not checkpoint acceptance and never changes a roadmap marker.

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
| `012-generic-astm-plugin-profiles`    | Established generic-plugin/profile foundation whose working profile semantics must be evolved and moved without behavioral replacement               |
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
- [Westgard/QC functional specification](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)
  and [Analyzer Manual QC functional specification](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/analyzer-manual-qc.md),
  used only for the separate operational-QC workflow and analyzer-context/return
  behavior. Their entity, route, and `QcRun` suggestions are non-normative and
  explicitly do not override this roadmap's no-`QcRun` decision.
- [Analyzer result-review functional design](https://github.com/DIGI-UW/openelis-work/blob/main/designs/system/analyzer-import-redesign-v2.md)
  and [Results Validation v4 functional design](https://github.com/DIGI-UW/openelis-work/blob/main/designs/results-validation/validation-page-v4.md),
  used only to bound the post-OGC-1054 patient-result review/release work.
- [Published OpenELIS design catalog](https://digi-uw.github.io/openelis-work/catalog.html),
  used for current visual comparison and neighboring workflow context.

The current analyzer product artifact still uses the older labels “fork” and an
inline instrument-not-listed profile form. The newer repository-owned feature
specification records the approved product behavior: **Duplicate Profile** is a
separate Analyzer Types action, and analyzer setup links to that profile manager
then returns to the selected profile. Those wording differences are known
product-artifact follow-ups, not implementation ambiguities.

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
- [OpenELIS PR #3390](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3390),
  which introduced both the retained operational-QC foundation and the
  superseded OpenELIS-owned analyzer classifier.
- [Bridge PR #33](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/33),
  which introduced OE-pushed QC classifiers and current fallback behavior, and
  prepared [Bridge PR #46](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/46),
  whose lifecycle infrastructure is retained but whose replacement profile
  contract must be rewired to the established system before acceptance.
- [HL7 v2.9 Clinical Laboratory Automation, transfer of QC results](https://hl7.eu/HL7v2x/v29/std29/ch13.html),
  which identifies multiple valid control-specimen fields and supports an
  instrument-specific recognition contract rather than one protocol-wide guess.
- [Analyzer mock server](https://github.com/DIGI-UW/analyzer-mock-server) and its
  deterministic protocol/QC tests.
- [Established analyzer profile assets](../../projects/analyzer-profiles/README.md),
  which are the current working contract/content baseline. Their OE repository
  packaging is transitional; their Bridge-owned profile semantics are not.
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

| Term                       | Meaning                                                                                                                                                                                                          |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Generic analyzer runtime   | Existing ASTM, HL7, FILE, FHIR, and Bridge transport foundation                                                                                                                                                  |
| Analyzer profile           | The Bridge-owned analyzer-type definition that (1) defines communication/runtime behavior and (2) supplies defaults for creating a new OpenELIS analyzer instance                                                |
| Profile revision           | An immutable, retained version of one Bridge profile; an analyzer keeps using its pinned revision until a user explicitly adopts another                                                                         |
| Analyzer Type              | The lab-facing composed view of a Bridge profile plus site-specific OpenELIS catalog bindings and readiness                                                                                                      |
| Site binding               | OpenELIS-owned association from normalized analyzer test/result concepts to the local Test and Result Option catalog                                                                                             |
| Control-result recognition | Bridge profile behavior that identifies an incoming control specimen/run before normalized delivery; it is not a Westgard rule, control lot, or activation prerequisite                                          |
| Operational QC             | OpenELIS control materials/lots, QC results/statistics, Westgard evaluation, violations, alerts, corrective action, and result-release policy; it is linked to an analyzer but separate from analyzer activation |
| Analyzer instance          | A configured instrument at a lab, associated with one pinned profile revision, lab units, status, Bridge runtime registration, and a link to operational QC                                                      |
| Activation candidate       | The exact profile revision, site-binding/recognition fingerprints, and analyzer-instance/runtime configuration proposed for verification and Bridge synchronization                                              |
| PR #3792 foundation        | Historical source for selected route, mapping, QC, and test behavior; it is not in the new PR train and is not the OGC-1054 MVP                                                                                  |
| Coordinated PR train       | Linear, cross-linked OpenELIS, Analyzer Bridge, and analyzer-mock stacks, with a checkpoint PR only where an owning test proves that repository must change                                                      |
| OGC-1054 MVP               | A complete lab-admin workflow to manage an Analyzer Type, map and verify it, configure and activate an analyzer, and safely receive and resolve known and unknown traffic without developer-edited configuration |
| Full OGC-1054 rollout      | The accepted MVP plus mature alert operations, profile revision diff/bulk adoption/rollback, distribution hardening, and exact-build full-feature acceptance                                                     |
| Full analyzer program      | OGC-1054 plus multi-component ingestion, Results/Validation integration, per-instrument validation, maintenance, access control, and site rollout                                                                |

## Fixed Architecture

The analyzer architecture is already split. This roadmap does not reopen that
decision.

### Established profile baseline and contract

The existing profile system is the foundation for this feature, not a legacy
model to replace. Its current files, OE setup consumer, Bridge runtime
registration, and profile-backed mock flows already prove the intended model.
The temporary fact that profile files are packaged through the OpenELIS
distribution does not transfer their architectural ownership to OpenELIS.

Every profile has exactly two product jobs:

1. define communication with one analyzer type, including protocol/version,
   supported transport and direction, analyzer identity, parser/extraction
   behavior, emitted test/result vocabulary, and control-result recognition;
2. define safe defaults used when OpenELIS creates a new instance of that type,
   including applicable connection/file settings and portable catalog-binding
   hints. Site name, lab unit, network address, credentials, and watch directory
   remain instance values supplied by the lab.

The accepted Bridge contract MUST be a strict additive evolution of the
established `analyzer-defaults` contract. It adds generated publication
metadata, strict protocol-discriminated validation, immutable revisions, and
lifecycle without dropping either job. It forbids OpenELIS database IDs,
instance-specific site values, operational-QC policy/data, arbitrary copied
plugin JSON, and hidden analyzer-specific fallbacks.

The contract is the only source of profile-specific runtime/default values.
Production code has no hard-coded profile/model/manufacturer/analyzer-code
switch, named special case, or selected-profile fallback constant. Generic
lookup tables populated from the selected profile are required. GeneXpert and
FluoroCycler are named only as parameterized compatibility inputs; passing their
tests must exercise the same generic code path used by every other profile.

Published revisions are immutable and retained while referenced. Duplicate
Profile creates a new draft identity; Update shared creates a draft successor
revision under the same identity; Publish creates an immutable selectable
revision. A configured analyzer pins profile ID and revision and never moves
implicitly. OpenELIS stores site instance values and local catalog bindings,
not an authoritative profile copy.

Existing profile rows are evidence to curate, not records to preserve
mechanically. Each row is retained, corrected, represented as a proven alias,
split, or removed according to vendor/capture/mock evidence. Equal LOINC values
alone never establish alias equivalence, and the target system has no
`LEGACY_UNBOUND` profile or mapping concept.

The evolved contract classifies content deterministically:

| Class                | Profile content                                                                                                                                                                                                                                                            |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Always required      | Stable profile identity/display/manufacturer/model/category/contract version; typed `protocol`; complete safe `configDefaults`; all known fixed-vocabulary result definitions; and explicit `controlResultRecognition` as `RULES` or affirmed `NONE`                       |
| Protocol-conditional | ASTM/HL7 framing, version, transport, direction/role, identity, parser/extraction, aggregation, and capabilities; or complete FILE format/extensions, filename, encoding, delimiter/header/sheet/row, column, locale/date/unit, watching, and transport semantics          |
| Field-conditional    | Unit and raw qualitative vocabulary where the emitted concept supplies them; aliases only when evidence proves alternate encodings of one concept                                                                                                                          |
| Catalog-generated    | Immutable revision, content fingerprint, lifecycle status, lineage, publication actor/time, and retirement metadata                                                                                                                                                        |
| Optional             | Human authoring notes and evidence links that do not change runtime behavior; publication policy may require evidence when confidence is not already validated                                                                                                             |
| Forbidden            | OpenELIS database IDs, site analyzer name, lab units, network address, credentials, watch directory, operational-QC data/policy, arbitrary plugin JSON, hidden fallback behavior, authored publication metadata, copied-profile authority, and undeclared extension fields |

A publishable fixed-vocabulary profile cannot have an empty result-definition
list. Dynamic-vocabulary support is not inferred from missing content and
requires an approved future contract amendment.

The established `configDefaults.qcRules` field is removed as part of curation,
not retained as a legacy array. Only valid analyzer-message identification
semantics are rewritten into `controlResultRecognition`; invalid, duplicate, or
unsupported entries are corrected or removed with evidence. No compatibility
reader, `AnalyzerQcRule`, or operational-QC configuration is created from it.

### Bridge owns analyzer runtime

The Analyzer Bridge owns:

- the versioned portable profile catalog and profile validation contract;
- ASTM, HL7/MLLP, FILE, and serial-facing listeners and transport;
- FILE directory watching, retries, delivery, and transport dead-letter state;
- protocol parsing, framing, analyzer identification, and connection probes;
- analyzer-code to normalized-code translation used by runtime;
- control-result recognition from instrument messages using only the pinned
  profile revision;
- normalized FHIR output, including preserved raw code/value context;
- runtime analyzer registration and idempotent full-state reconciliation;
- bidirectional protocol execution where a profile supports it.

### OpenELIS owns laboratory and clinical decisions

OpenELIS owns:

- the lab-facing Analyzer Types, mapping, setup, review, and alert workflows;
- analyzer instance identity, assigned lab units, lifecycle, and permissions;
- local Test and Result Option catalog bindings;
- mapping confirmation, verification fingerprints, actor/time, and audit;
- operational QC control lots, QC results/statistics, Westgard configuration
  and evaluation, violations, alerts, corrective action, and result-release
  policy;
- durable staging/holding of known and unknown clinical results;
- user-visible resolution, review, alerts, and downstream clinical processing;
- desired analyzer-instance configuration and synchronization requests to
  Bridge.

OpenELIS does not own raw analyzer protocol parsing, instrument listeners,
runtime FILE polling, protocol-specific connection logic, or a second portable
profile authority.

OpenELIS also does not own a per-analyzer control-recognition rule store or send
operational-QC configuration to Bridge. `AnalyzerQcRule` is a superseded
classifier path, not part of the target operational-QC domain.

### Control-result recognition contract

Control-result recognition is a required, versioned part of every Bridge
profile because Bridge must decide whether an incoming transmission represents
a patient specimen or a control before normalized delivery. The BR-E0 portable
profile contract MUST expose one object named `controlResultRecognition` with
exactly one of these modes:

1. `RULES`: `rules` contains at least one schema-valid matcher. Rules have
   stable unique keys and OR semantics: one match classifies the transmission
   as a control. Field-based rules require an explicit target field; every rule
   requires a nonblank operand. Optional level/type output is recognition
   metadata only and cannot carry a local lot, assigned mean, standard
   deviation, Westgard limit, or release policy.
2. `NONE`: the profile explicitly does not support automated control-result
   recognition and contains no rules. Publishing `NONE` requires the profile
   author to affirm that this interface does not transport control results; an
   unknown or undocumented recognition scheme remains invalid rather than
   becoming `NONE`. `NONE` does not mean “use defaults,” does not create an
   operational-QC blocker, and must be shown for human confirmation in Verify.

Missing mode, unknown mode, `RULES` without rules, `NONE` with rules, an invalid
matcher, or a matcher that cannot be evaluated by the profile protocol makes
that profile revision invalid. An invalid revision cannot become active or be
selected for a new analyzer. No `UNKNOWN`, implicit-empty, or best-effort mode
exists.

Bridge evaluates only the rules in the analyzer's pinned profile revision. It
must not fall back to ASTM field guesses, FILE prefixes/tasks, an OE-pushed rule
array, or any other hidden classifier when mode is `NONE` or rules do not match.
The normalized result carries the resulting patient/control classification and
any extracted control identifier, level, lot, and raw source context available
from the message; OpenELIS owns local interpretation and operational QC.

OpenELIS displays a human-readable summary such as “specimen ID starts with
CNEG” or “control flag is Q” and records confirmation against the profile ID,
revision, and recognition fingerprint. Analyzer Types may expose structured,
protocol-aware authoring controls, but no normal lab workflow exposes a regular
expression, raw JSON, or raw matcher field. Analyzer connection setup only
reviews and confirms the summary. A recognition change creates a new profile
revision and makes confirmation stale only for analyzers that explicitly move
to that revision. Operational-QC changes never stale this confirmation.

Analyzer-reported internal-control targets such as SPC, PCC, or IPC remain test
or result-component mapping concepts. They are not automatically whole-run
control recognition and are not operational QC.

Published profile revisions are immutable and retained while any analyzer
references them. OpenELIS stores the profile ID and revision rather than an
authoritative copied profile snapshot. Site bindings and their verification
fingerprints are scoped to that profile revision. **Update shared** publishes a
new revision under the same profile identity; **Duplicate Profile** creates a
new profile identity and initial revision. Neither action moves an existing
analyzer. The Analyzer Types manager shows which analyzers use the profile and
which have an update available. MVP supports explicit one-analyzer adoption,
followed by re-verification and synchronization; R1.2 adds richer diff, bulk
update, and rollback operations.

The Analyzer Types manager is the sole authoring surface for reusable profile
behavior and shared site bindings. Analyzer setup Verify is a review and
confirmation surface. Any Resolve or Edit action leaves setup for that same
Analyzer Types editor with a return URL; it never opens a second or
analyzer-specific editor.

The lab-facing navigation has one Analyzer dashboard and one distinct Analyzer
Types manager. `/analyzers` is the sole analyzer-instance list and inline
create/edit/setup entry. `/analyzers/types` owns reusable profile lifecycle and
its subordinate mapping view. Operational QC and analyzer attention are linked
workflows, not additional analyzer-configuration dashboards. Once inline setup
replaces `/analyzers/new` and `/analyzers/:id/edit`, those standalone routes and
components are removed; G0 contains no compatibility redirect or duplicate
navigation entry for them.

### Analyzer mock owns reproducible instruments

`tools/analyzer-mock-server` owns deterministic test instruments and fixtures.
It must exercise the Bridge through real ASTM, HL7, and FILE transports for
integration acceptance. Its legacy direct-to-OpenELIS HTTP modes may be used
only to characterize or retire legacy behavior; they cannot prove the target
runtime architecture. Keep the mock deliberately thin: it emulates only the
instrument transport, messages, connection behavior, and failure cases needed
by an accepted scenario. It does not own profile lifecycle, OpenELIS business
rules, or a duplicate product workflow. Add mock code only when a failing
transport-level test proves an accepted scenario is missing.

### Review tooling owns review provenance

`DIGI-UW/openelis-review-tooling` owns the Grist-backed checklist overlay,
checklist revision, build manifest, and downloadable review report. It does not
own application behavior or seed data.

## Target Runtime Contract

The contract is directional and versioned:

1. OpenELIS pins a Bridge profile ID and revision and sends desired analyzer
   instance identity, lab-owned connection choices, and runtime configuration.
   The registration contract does not contain `AnalyzerQcRule`, control lots,
   Westgard configuration, or other operational-QC state.
2. Bridge validates and applies the desired runtime registration idempotently.
3. The analyzer mock or a real instrument sends raw traffic to Bridge.
4. Bridge parses and emits normalized FHIR with analyzer identity, pinned
   profile revision, raw analyzer code/value, normalized code where known,
   patient/control classification from `controlResultRecognition`, and source
   metadata.
5. OpenELIS binds normalized/raw concepts to its local catalog, stages the
   result, evaluates operational QC, and either proceeds or holds it with a
   visible reason.
6. Resolving an unknown local catalog binding updates durable OpenELIS site
   binding state. A portable profile change, when required, is made through the
   Bridge profile lifecycle contract and produces a new revision. Existing
   analyzers remain pinned to their current revision until a user explicitly
   selects the update and re-verifies it. Duplicate Profile creates a new
   profile identity rather than silently changing the source profile.

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
- Established Bridge-owned profile assets currently packaged through the
  OpenELIS distribution, plus working profile-driven setup, mock, and Bridge
  runtime behavior. Their packaging and copied-configuration path must be
  replaced after parity; their two-job profile semantics are the target
  baseline.
- Existing local analyzer mappings, pending-code infrastructure, Bridge desired
  registration, the superseded per-analyzer `AnalyzerQcRule` classifier, and
  the retained operational-QC foundation (`QCControlLot`, `QCResult`,
  `QCStatistics`, Westgard configuration/evaluation, violations, and alerts).
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
- Completeness, usage, source, lineage, deactivate/reactivate, Duplicate Profile,
  and update impact in Analyzer Types.
- A complete add/edit/remove/repoint mapping editor showing unmatched profile
  rows instead of skipping them.
- Explicit control-result-recognition confirmation, separate from operational QC.
- Capability-aware Results only/Two-way selection.
- Production creation of pending result values from Bridge traffic.
- Durable hold plus Alerts/Needs attention for unknown traffic.
- Live result capture/reconciliation and draft-profile population through the
  separate Analyzer Types workflow.
- Current integrated remote acceptance against current OpenELIS, Bridge, mock,
  profile, and review-tooling revisions.

Therefore neither PR #3792 nor the July deployment is the OGC-1054 MVP.

The table below records the pre-execution `develop` baseline established during
R0. It is historical context, not a live status ledger; checkpoint markers and
acceptance criteria govern current status. Named paths may disappear when their
owning removal checkpoint becomes review-ready. `openelis-work` contributes only
functional and visual outcomes; it does not supply these implementation
conclusions.

| R0 baseline claim                               | Reproducible code evidence                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ----------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Setup remains separate routed pages             | [`frontend/src/App.jsx`](../../frontend/src/App.jsx) and [`AnalyzersList.tsx`](../../frontend/src/components/analyzers/AnalyzersList/AnalyzersList.tsx) route Add, Edit, Mappings, QC Rules, and Control Lots separately.                                                                                                                                                                                                                                                                                                                                   |
| Qualitative binding is not catalog-safe         | [`QualitativeResultMapping.java`](../../src/main/java/org/openelisglobal/analyzer/valueholder/QualitativeResultMapping.java) and [`QualitativeResultMappingForm.java`](../../src/main/java/org/openelisglobal/analyzer/form/QualitativeResultMappingForm.java) persist free-text `openelisCode`.                                                                                                                                                                                                                                                            |
| Activation has an inconsistent legacy QC gate   | [`AnalyzerStatusTransitionServiceImpl.java`](../../src/main/java/org/openelisglobal/analyzer/service/AnalyzerStatusTransitionServiceImpl.java) requires one active `AnalyzerQcRule` only for the initial `VALIDATION -> ACTIVE` transition, while return transitions from error/offline bypass it. The target gate is current binding/recognition verification plus synchronized pinned runtime state; operational QC never participates.                                                                                                                   |
| Profile packaging and provenance are incomplete | [`AnalyzerRestController.java`](../../src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java) reads the established filesystem profiles and applies `defaultConfigId`; the running setup/Bridge flow proves those profile semantics work, but the analyzer does not retain an authoritative revision pin. E0/M1 move catalog packaging and add lifecycle without replacing the contract.                                                                                                                                          |
| PR #3390 mixed two QC concerns                  | [`AnalyzerQcRule.java`](../../src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerQcRule.java) is the superseded classifier. [`QCControlLot.java`](../../src/main/java/org/openelisglobal/qc/valueholder/QCControlLot.java), [`QCResult.java`](../../src/main/java/org/openelisglobal/qc/valueholder/QCResult.java), QC statistics, Westgard evaluation, violations, and alerts are the retained operational-QC foundation.                                                                                                                       |
| Bridge already owns runtime concerns            | Bridge [`AnalyzerRegistrationController`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/AnalyzerRegistrationController.java), [`TestConnectivityController`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/TestConnectivityController.java), and [`FileWatcher`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/file/FileWatcher.java) own registration, probes, and FILE watching today. |
| Bridge retains hidden QC classifier fallbacks   | Current Bridge ASTM and FILE parsers use hard-coded field/prefix/task detection when no pushed rules exist. BR-E0/BR-M2 replace this with the explicit pinned-profile contract and tests proving `NONE` and non-match never invoke a fallback.                                                                                                                                                                                                                                                                                                              |
| Bridge profile lifecycle is still absent        | Current Bridge `develop` executes profile-derived registration/runtime behavior but has no durable catalog/revision/Duplicate Profile lifecycle. BR-E0/BR-M1 add that lifecycle around the established contract rather than introducing another profile model.                                                                                                                                                                                                                                                                                              |
| Mock is multi-protocol but has legacy delivery  | Current analyzer-mock [templates](https://github.com/DIGI-UW/analyzer-mock-server/tree/main/templates) cover ASTM, HL7, and FILE, while its [README](https://github.com/DIGI-UW/analyzer-mock-server/blob/main/README.md) still documents direct-to-OpenELIS delivery. M4 uses real transport to Bridge and retires direct delivery as acceptance proof.                                                                                                                                                                                                    |

## Scope

### Full MVP

The MVP is reached only when a laboratory administrator can:

1. find a shipped or site Analyzer Type, understand readiness and usage, and
   create a type or use Duplicate Profile without editing files;
2. bind every analyzer test and qualitative result value to valid local
   concepts and confirm the profile's human-readable control-result recognition
   through one protocol-neutral editor;
3. create an analyzer inline, select readable lab units, verify the mappings,
   configure Bridge-owned connectivity, and see all activation blockers;
4. open the analyzer-linked OpenELIS Quality Control workflow without making
   its rule, lot, result, or readiness state an analyzer-activation prerequisite;
5. activate the analyzer only after current binding/control-recognition
   verification and synchronized pinned Bridge runtime state;
6. request a live result during Verify, reconcile every seen/not-seen/new item,
   and populate a draft site type created in Analyzer Types from held traffic
   without losing anything;
7. receive a known patient result and a recognized control result through Bridge
   from the analyzer mock;
8. hold and visibly flag an unknown test/value, resolve it safely, and process
   the next matching result deterministically; and
9. reload, bookmark, navigate by breadcrumb, and review the same durable state.

The MVP includes a discoverable Alerts/Needs attention path. A resolver hidden
inside an analyzer page is not enough for safe operation.

MVP operational-QC scope is the linked canonical workflow, control-result
ingestion/evaluation, and proof that QC state is independent of analyzer
activation. Patient Results/Validation release-screen integration remains
outside OGC-1054; the architectural rule is nevertheless fixed that any
QC-based release/hold decision belongs in OpenELIS, never Bridge or analyzer
activation.

### Full OGC-1054 rollout

After MVP acceptance, complete:

- mature alert triage, acknowledgement, assignment, concurrency, and
  navigation;
- profile revision diff, bulk adoption impact, rollback, backup export, and
  distribution hardening beyond MVP's explicit one-analyzer adoption;
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

| Product slice                                                             | Functional/visual reference                                                            | Current code state                                                               | Delivery checkpoint      |
| ------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------ |
| [OGC-1055](https://uwdigi.atlassian.net/browse/OGC-1055) Analyzer Types   | Reuse, create/Duplicate Profile, completeness, usage, lifecycle, and list presentation | Transitional shipped-profile/type page; site lifecycle absent                    | M1                       |
| [OGC-1056](https://uwdigi.atlassian.net/browse/OGC-1056) mapping          | Complete test/result editor, control-recognition review, and safe revision scope       | Legacy standalone mapping/pending paths; accepted catalog-bound editor absent    | M2                       |
| [OGC-1057](https://uwdigi.atlassian.net/browse/OGC-1057) guided setup     | Inline Instrument, Verify, and Connect sections plus a readable completion summary     | Standalone routes only; current activation does not implement full readiness     | M3                       |
| [OGC-1058](https://uwdigi.atlassian.net/browse/OGC-1058) traffic learning | Hold, alert, resolve, and reconcile unknown traffic                                    | Pending/error infrastructure exists; production hold/alert/reconciliation absent | M4 core; R1.1 operations |
| PR #3792 QC/config extension                                              | Historical behavior provenance only                                                    | Frozen divergent branch; F0 evaluates behavior directly before reuse             | F0 review                |

Issue identifiers in this table are traceability labels only. Scope and
dependency are defined by the repository specifications and this roadmap, not
by external workflow status.

## Functional Acceptance Crosswalk

This table paraphrases the current product acceptance behavior without carrying
over any proposed data model, endpoint, route, class, annotation, or repository
ownership from a product artifact.

| Product AC | Functional outcome                                                                                                                                                          | Current branch                                                                                                                                                             | Target  |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| AC-1       | Add Analyzer starts inline while the analyzer list remains available.                                                                                                       | Absent; `/analyzers/new` is a standalone route.                                                                                                                            | M3      |
| AC-2       | Instrument, Verify, and Connect form one progressive, understandable setup story.                                                                                           | Absent; current setup concerns are separate routes.                                                                                                                        | M3      |
| AC-3       | Instrument/type selection is searchable.                                                                                                                                    | Partial standalone selectors; no accepted integrated type search.                                                                                                          | M3      |
| AC-4       | A not-listed instrument links to separate Analyzer Types creation and can return with the new reusable type selected.                                                       | Absent.                                                                                                                                                                    | M1 + M3 |
| AC-5       | Verify shows every profile test, normalized identity, and match state.                                                                                                      | Absent; create-time bootstrap does not retain/display every row.                                                                                                           | M2 + M3 |
| AC-6       | Human mapping confirmation is mandatory and auditable.                                                                                                                      | Absent on OE-R0.                                                                                                                                                           | M2 + M3 |
| AC-7       | Profile-owned control-result recognition is summarized and confirmed during Verify; explicit `NONE` is visible and no operational-QC state is shown as an analyzer blocker. | Absent; the OE classifier is mislabeled as a QC rule and gates initial activation, while the separate operational-QC foundation does not define this recognition behavior. | M2 + M3 |
| AC-8       | A non-match can map to an existing Test, detour to Test Catalog, or be explicitly excluded.                                                                                 | Partial legacy pending-code resolver; no complete source-row flow.                                                                                                         | M2      |
| AC-9       | One unresolved test does not hide or block independent mapping work.                                                                                                        | Absent; unmatched bootstrap rows are not retained visibly.                                                                                                                 | M2      |
| AC-10      | Setup uses the selected profile's communication/data-flow default; only profile-supported modes appear, and probe failure never silently changes the choice.                | Partial; current setup exposes initiator mode and does not consistently apply complete profile defaults.                                                                   | M1 + M3 |
| AC-11      | Every test mapping can be added, edited, removed, or repointed.                                                                                                             | Absent for profile-applied rows.                                                                                                                                           | M2      |
| AC-12      | A qualitative result can target only an option belonging to its mapped Test.                                                                                                | Absent; current mapping stores free-text `openelisCode`.                                                                                                                   | M2      |
| AC-13      | Saving shared changes requires explicit Duplicate Profile or update-shared scope.                                                                                           | Absent.                                                                                                                                                                    | M2      |
| AC-14      | Duplicate Profile creates a unique profile identity and visible source lineage.                                                                                             | Absent.                                                                                                                                                                    | M1 + M2 |
| AC-15      | Unknown traffic is held and visibly flags the analyzer/type and Alerts.                                                                                                     | Pending/error infrastructure only; hold/alert path absent.                                                                                                                 | M4      |
| AC-16      | Analyzer Types shows completeness/usage and the required search/filter states.                                                                                              | Partial type/profile list; completeness and usage absent.                                                                                                                  | M1      |
| AC-17      | Types can be deactivated/reactivated without deleting history.                                                                                                              | Absent.                                                                                                                                                                    | M1      |
| AC-18      | ASTM, HL7, and FILE share one complete editor with only protocol labels varying.                                                                                            | Absent; current standalone editor is not the accepted workflow.                                                                                                            | M2      |
| AC-19      | Normal lab setup contains no developer-only fields.                                                                                                                         | Absent; current standalone form still exposes technical fields.                                                                                                            | M3      |
| AC-20      | All visible copy is localized.                                                                                                                                              | Partial; raw status/fallback strings remain.                                                                                                                               | M1-M4   |
| AC-21      | Setup can request a live result and reconcile what was received.                                                                                                            | Absent.                                                                                                                                                                    | M4      |
| AC-22      | Unknown data is never lost; resolution changes future matching behavior.                                                                                                    | Absent end to end.                                                                                                                                                         | M4      |
| AC-23      | Live traffic can populate a draft type created in the separate Analyzer Types workflow.                                                                                     | Absent.                                                                                                                                                                    | M4      |

### OGC-1057 QA finding disposition

The OGC-1057 QA report was reviewed from its current `openelis-work` QA branch.
Its browser observations are functional input. Its REST checks, named
endpoints, implementation diagnosis, and proposed technical remedies are not
imported as engineering authority. The report contains no screenshot or video
assets, so it does not replace the M3/G0 visual comparison and acceptance
gates.

| QA finding  | Deterministic roadmap disposition                                                                                                                                                                      | Gate                                     |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------- |
| 1           | Every accepted distinct result definition can be bound, excluded, or repointed through complete active-Test catalog search; proven aliases are not duplicate rows.                                     | M2; MVP-005/006; `AN-MVP-003`            |
| 2           | Profile curation records retain/correct/alias/split/remove dispositions from evidence; applying an accepted revision never hides or falsely completes one of its definitions.                          | E0 + M2; MVP-005/006                     |
| 3           | Changing an applicable result option enables save, persists, and survives reload; invalid and empty-option rows remain explicit.                                                                       | M2; MVP-007; `AN-MVP-004`                |
| 4           | Test selection searches the complete active local catalog by name, code, or LOINC; no fixed legacy subset can satisfy acceptance.                                                                      | M2; MVP-006; `AN-MVP-003`                |
| 5           | Proven aliases and local LOINC cardinality are characterized; equal LOINCs never create aliases, ambiguous candidates never auto-bind, and accepted distinct results remain independently confirmable. | E0 + M2                                  |
| 6           | Control-result-recognition confirmation is independent of operational QC. Operational rules/lots/results never disable mapping confirmation or analyzer activation.                                    | E0 + M2 + M3; MVP-008/012/015/016        |
| 7           | Instrument not listed links to the separate Analyzer Types create/Duplicate Profile workflow without developer fields, then returns to setup.                                                          | M1 + M3; MVP-003/010; `AN-MVP-002`       |
| 8           | Method-dependent control-lot validation remains visible and actionable inside the separate OpenELIS Quality Control workflow; it never becomes an analyzer readiness blocker.                          | M3; MVP-015/022; `AN-MVP-009`            |
| 9           | Type lifecycle uses deactivate/reactivate in M1; M2 removes Copy Mappings; M3 replaces analyzer-instance hard delete with audited deactivate/reactivate.                                               | M1 + M2 + M3; MVP-004/009                |
| Deferred    | Real mock-to-Bridge probes cover role-appropriate settings, success, failure, timeout, supported direction, and an explicit supported Results-only choice after Two-way failure.                       | BR-M3 + OE-M3 + MOCK-M4; MVP-013/014/021 |
| Untested    | M4 owns live capture/reconciliation, draft-type population, hold/alert/resolve, and deterministic next-message behavior for unknown traffic.                                                           | M4; MVP-018/019/020                      |
| Preserve    | Inline setup, searchable selection, readable summaries, lab units, live blockers, catalog-safe result choices, and absence of developer fields remain functional regressions.                          | M1-M3; MVP-001/007/010/016/022           |
| Withdrawn   | The report's withdrawn picker-search and direction-default observations add no defect requirement; later UI automation must use focused visible controls.                                              | M3/G0; MVP-010/014/023                   |
| Environment | G0 starts from a deterministic reset/fixture state; artifacts left by the 2026-08-12 review are preconditions to remove, not product history to hard-delete through the UI.                            | G0 deployment preflight                  |

## Execution Contract

These are engineering validation milestones, not partial product acceptance.
Review happens against CI-green checkpoint PR deployments throughout the train;
G0 is the first and only full-MVP human acceptance gate.
There is one active implementation checkpoint at a time and no
architecture-selection prompt in the workstream. Once that checkpoint is
review-ready, the next stacked checkpoint may start while review continues;
acceptance and merge remain strictly ordered.

### Prime iteration-marker rule

The roadmap has exactly four state markers and no other workflow state:

- `[✓]` **merged**: every required PR for the iteration is merged on its
  canonical target;
- `[x]` **review-ready**: implementation and the iteration's full automated
  exit gate are complete, and every required PR is ready for review but not
  necessarily merged;
- `[*]` **active**: the one iteration currently being implemented; and
- `[ ]` **future**: an iteration that has not started.

Exactly one iteration is `[*]` while any implementation iteration remains.
The ordinary transition is `[ ]` to `[*]` when its review-ready predecessor is
`[x]`, `[*]` to `[x]` when its implementation and automated exit gate are
complete, and `[x]` to `[✓]` when GitHub records every required checkpoint PR
as merged. Starting the next stacked iteration and changing its predecessor to
`[x]` happen in the same roadmap amendment, so the roadmap never invents a
second active implementation state.

GitHub is authoritative for review and merge state. An iteration remains `[x]`
through review and any review corrections; review does not change roadmap
markers. The active descendant records a predecessor's `[✓]` after the
predecessor merges and the stack is rebased or retargeted.

All finer-grained facts, including implementation progress, red/green commits,
CI, review readiness, and branch heads, already live in Git and GitHub. They are
deliberately not copied into roadmap state or a parallel checkpoint ledger.

An executor given the goal “execute this roadmap through deployed MVP” must:

1. fetch all three repository bases and read their local `AGENTS.md` files;
2. resume the single checkpoint marked `[*]`;
3. create or reuse exactly the branch and base named below;
4. record a failing test before production implementation, implement to green,
   refactor, and keep that provenance in the checkpoint's commits and PR;
5. change this roadmap's markers only for a formal start, review-ready exit, or
   completed merge;
6. do not begin production work for a `[ ]` checkpoint; start only the immediate
   successor of an `[x]` or `[✓]` checkpoint;
7. stop only for a permission/credential boundary, a required external review
   or merge, or current code/contract behavior that contradicts this fixed
   architecture.

### Roadmap provenance invariant

This path has one active lineage. R0 establishes the authority; each later
OpenELIS checkpoint may update stable scope, acceptance, and the four-state
iteration marker only as a Git descendant of its review-ready predecessor. A
sibling or historical branch may retain an older blob as provenance, but it is
never an active roadmap and is never merged as a competing version. Git history
preserves PR #3792 as historical input; it receives no roadmap edits and is
never an implementation base.

After a prerequisite merges, rebase the dependent branch on current
`develop`/`main`, retarget its PR, rerun its gates, and record the prerequisite
as `[✓]`. Never mark an iteration `[x]` because code merely exists.

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
red-green-refactor history needed for review. Each UI checkpoint also publishes
its exact review candidate and verifies the live overlay/checklist; those
ordinary deployment and review facts remain in the PR. Only G0 creates a
standalone bundle because final human UAT and MP4 must be bound to one unchanged
deployment.

An ambiguity is blocking when resolving it could change clinical safety,
repository ownership, durable data semantics, a cross-repository contract, or
an acceptance criterion. Resolve it in the owning specification or contract
before the affected iteration starts.

### Resolved QC and activation decision

The earlier blocking QC question is removed because it conflated control-result
recognition with operational QC. The governing decisions are:

1. Bridge profile `controlResultRecognition` is analyzer-type behavior and is
   confirmed with the profile mappings.
2. OpenELIS operational QC is a separate linked workflow and a QC/result-release
   concern. No operational rule, control lot, QC result, Westgard state, or
   connection-test outcome is an analyzer-activation prerequisite.
3. `AnalyzerQcRule` is not retained. Its applicable recognition behavior moves
   to Bridge profiles; the OpenELIS entity/table/service/controller/UI,
   registration field, readiness checks, seed data, translations, and runtime
   callers are removed through E0-M4.
4. Activation is evaluated against one immutable candidate and succeeds if and
   only if all of these predicates are true:
   - the pinned profile ID/revision exists, is active, and passes the accepted
     Bridge profile schema;
   - the analyzer name is nonblank, at least one active lab unit is assigned,
     the selected connection/data-flow modes are declared by that revision,
     and every profile-declared required instance field is valid;
   - every declared test/result source row is currently bound to an active,
     valid local catalog target or explicitly excluded where exclusion is
     offered, and the confirmed row IDs exactly match that candidate;
   - control-result recognition is confirmed for the same profile revision and
     recognition fingerprint, including an explicit `NONE`; and
   - Bridge has acknowledged the same analyzer ID, profile ID/revision, and
     canonical desired-state fingerprint.
5. A visible connection test supplies setup evidence but does not persist as an
   activation gate.
6. The same server-side predicate applies to every transition into `ACTIVE`,
   including reactivation from error/offline. Editing a profile, binding, or
   runtime field creates a draft candidate; it does not mutate the last active
   candidate or silently deploy it. The UI shows the pending update until the
   new candidate is verified and synchronized.

No QC policy question blocks M3. Only its ordinary predecessor and the accepted
E0/Bridge contracts control when it starts.

### Iterations

- [x] **R0 - Authoritative roadmap.** Review-ready on PR #4049; this correction
      remains part of R0 review rather than opening a second roadmap lineage.
- [x] **F0 - Deterministic foundation salvage.** Review-ready on PR #4053;
      remediation adds the missing established-profile parity guards.
- [x] **E0 - Engineering contract and clean replacement boundary.** Bridge #45
      and OE #4055 are review-ready only after the profile contract and parity
      fixtures are corrected under this amendment.
- [x] **M1 - Established Bridge profile lifecycle and Analyzer Types.**
      Review-ready on Bridge #46, analyzer-mock #40, and OE #4056 using the
      corrected E0 contract and exact priority-profile revisions.
- [x] **M2 - Safe mapping editor.** Review-ready on Bridge #47 and OpenELIS
      #4118.
- [*] **M3 - Guided setup, connectivity, and linked operational QC.** Active
  on Analyzer Bridge PR #48 and OpenELIS PR #4125. The accepted analyzer-mock
  implementation remains the fixture source unless a failing transport
  contract proves that it must change.
- [ ] **M4 - Safe traffic and integrated MVP.** Future.
- [ ] **G0 - Full MVP deployment and human acceptance.** Future.
- [ ] **R1.1 - Mature alert operations.** Future.
- [ ] **R1.2 - Profile revision and distribution operations.** Future.
- [ ] **R1-G - Full-feature deployment and human acceptance.** Future.
- [ ] **R2 - Operational rollout.** Future.

Review-ready predecessors and merged checkpoints do not create parallel active
implementation blocks. Their actual review and merge state remains visible in
Git and GitHub.

## Pull Request Train

PR #3792 is not a stack. It is frozen historical provenance and a behavior
source for F0. It is not rebased, merged, or used as a branch base. Once the F0
replacement PR is open, rename #3792 to “OGC-1054 historical analyzer QC/config
foundation (superseded)”, link R0/F0, and close it as superseded without deleting
its branch.

### OpenELIS stack

| Order | ID    | Fixed branch                           | Initial PR base | Scope                                                                                             |
| ----- | ----- | -------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------- |
| 0     | OE-R0 | `codex/ogc-1054-r0-roadmap`            | `develop`       | This roadmap and current engineering ownership amendments only                                    |
| 1     | OE-F0 | `codex/ogc-1054-f0-foundation`         | OE-R0 branch    | Characterize and cleanly salvage compatible #3792 foundation behavior                             |
| 2     | OE-E0 | `codex/ogc-1054-e0-contract-migration` | OE-F0 branch    | ADR, consumer contracts, profile curation/removal boundary, red cross-repo tests                  |
| 3     | OE-M1 | `codex/ogc-1054-m1-analyzer-types`     | OE-E0 branch    | OpenELIS Analyzer Types composition, site bindings, lifecycle UI, profile-driven setup            |
| 4     | OE-M2 | `codex/ogc-1054-m2-mapping`            | OE-M1 branch    | Complete protocol-neutral mapping and control-recognition confirmation; remove OE classifier path |
| 5     | OE-M3 | `codex/ogc-1054-m3-setup-qc`           | OE-M2 branch    | Guided setup, Bridge connectivity, linked operational QC, activation                              |
| 6     | OE-M4 | `codex/ogc-1054-m4-safe-traffic`       | OE-M3 branch    | Hold/alert/resolve, integrated harness, legacy removal, full UI story                             |
| 7     | OE-G0 | `codex/ogc-1054-g0-acceptance`         | OE-M4 branch    | Exact-build deployment, Grist UAT, MP4, and acceptance corrections                                |

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

| Order | ID    | Fixed branch                          | Initial PR base  | Scope                                                                                                                                                    | Required by |
| ----- | ----- | ------------------------------------- | ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| 1     | BR-E0 | `codex/ogc-1054-e0-contracts`         | Bridge `develop` | Strict additive evolution of the established profile contract plus registration/normalized-traffic contracts and GeneXpert/Fluoro compatibility fixtures | OE-E0       |
| 2     | BR-M1 | `codex/ogc-1054-m1-profile-lifecycle` | BR-E0 branch     | Catalog, validation, revision, Duplicate/Update/Publish lifecycle, and curated shipped profiles around the established system                            | OE-M1       |
| 3     | BR-M2 | `codex/ogc-1054-m2-mapping-qc`        | BR-M1 branch     | Mapping identity, control-result recognition, and capability contracts; no classifier fallback                                                           | OE-M2       |
| 4     | BR-M3 | `codex/ogc-1054-m3-connect-probes`    | BR-M2 branch     | Profile-pinned, role-aware connection probes with structured endpoint/outcome evidence; remove the raw generic probe path                                | OE-M3       |
| 5     | BR-M4 | `codex/ogc-1054-m4-safe-traffic`      | BR-M3 branch     | Patient/recognized-control/unknown/FILE normalized traffic with preserved raw context                                                                    | OE-M4       |

### Analyzer mock stack

| Order | ID      | Fixed branch                       | Initial PR base | Scope                                                                                                                                                | Required by |
| ----- | ------- | ---------------------------------- | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| 1     | MOCK-M1 | `codex/ogc-1054-m1-profile-parity` | mock `main`     | Exact priority-profile revision pins and deterministic GeneXpert/Fluoro transport parity; QuantStudio only when it passes the same publication gate  | OE-M1       |
| 2     | MOCK-M4 | `codex/ogc-1054-m4-fixtures`       | merged MOCK-M1  | Deterministic ASTM, HL7, and FILE patient, recognized-control, nonmatch, unknown, connection, failure, and supported two-way fixtures sent to Bridge | OE-M4       |

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
6. MOCK-M1
7. OE-M1
8. BR-M2
9. OE-M2
10. BR-M3
11. OE-M3
12. BR-M4
13. MOCK-M4
14. OE-M4
15. OE-G0

Only the item marked `[*]` is under active implementation. Its immediate
successor cannot start until it reaches `[x]`, and a later item cannot merge or
become the deployment candidate before every earlier item is `[✓]`. This order,
not PR creation time, selects work.

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
4. Add compatibility fixtures for the established GeneXpert ASTM and
   FluoroCycler profiles. The fixtures require both profile jobs and fail if a
   proposed contract loses a runtime communication field or OE instance
   default used by the working flow.
5. Explain retained and rejected behavior in the PR review itself, backed by
   the tests and commits that implement the decision.
6. Update and close #3792 as superseded after OE-F0 is open.

**Exit:** OE-F0 is a small green replacement foundation; every retained behavior
has a test and every rejected behavior has a reason or legacy guard. #3792 is no
longer an open delivery candidate.

### E0 - Engineering contract and clean cutover characterization

1. In OE-E0, record an ADR for the established Bridge profile/OpenELIS
   instance-and-site-binding boundary and derive persistence from current code
   constraints only.
2. In BR-E0, evolve the established `analyzer-defaults` semantics into one
   strict, versioned, protocol-discriminated contract without implementing M1
   lifecycle. It MUST retain communication/runtime definition and OE instance
   defaults, add generated revision/publication metadata, and contain the
   discriminated `controlResultRecognition` contract above.
3. Use the current GeneXpert ASTM and FluoroCycler profiles as blocking
   compatibility fixtures. Each must retain every communication/default field
   used by current OE setup, Bridge registration/runtime, and mock traffic.
4. Produce a profile-by-profile curation disposition for the 20 established
   profiles. A row is retained, corrected, represented as a proven alias,
   split, or removed from evidence; neither LOINC equality nor current storage
   creates a preservation requirement.
5. Define the clean OE consumer: persisted profile ID/revision, site instance
   values, local Test/Result Option bindings, verification/audit, and desired
   Bridge registration. No copied profile, `LEGACY_UNBOUND`, heuristic profile
   inference, or compatibility writer is part of the target.
6. Characterize and plan one-way deletion of `defaultConfigId`, copied plugin
   JSON, old profile serving/apply, per-analyzer copied mappings, raw import
   routes, `AnalyzerQcRule`, and hidden Bridge fallbacks after assembled parity.
   Existing deployment data, if any, receives an explicit offline preflight and
   approved conversion; it does not justify a production legacy path.
7. Add failing producer/consumer fixtures for profile defaults, known/unknown
   traffic, recognized control, explicit `NONE`, non-match/no-fallback, FILE,
   immutable pinning, and registration reconciliation.

**Exit:** OE-E0 and BR-E0 are green; the approved ADR/contracts preserve both
profile jobs, GeneXpert/Fluoro compatibility passes, all 20 profiles have a
curation disposition, and clean replacement/removal tests exist. No M1 code is
accepted against a second or incomplete profile contract.

### M1 - Bridge profile lifecycle and Analyzer Types (OGC-1055)

1. In BR-M1, put the versioned profile catalog and lifecycle around the
   established profile system using the accepted BR-E0 contract. Ship only
   priority profiles whose complete contract, analyzer-mock transport, Bridge
   behavior, and assembled visible result flow are proven together; OE is not
   the catalog source. The 20-file E0 disposition is not a 20-profile M1
   publication gate, and an unproven source file is never loaded as a runtime
   profile or served through an OE fallback.
2. In OE-M1, compose one lab-facing Analyzer Types view from Bridge profile
   metadata plus OpenELIS local completeness/readiness/usage, with the
   plain-language explainer and aggregate counts shown by the functional mock.
3. Support shipped and site-created types, create, Duplicate Profile, unique
   naming, source lineage, deactivate/reactivate, and audit/history in the
   separate Analyzer Types workflow. Create reserves a unique, incomplete
   Bridge-owned draft that is not selectable until it is complete and
   published. Duplicate Profile starts a separate copied draft with source
   lineage. Publishing either kind creates immutable revision 1; creating,
   duplicating, or publishing never moves an existing analyzer.
4. Make search and filters URL-backed and use reusable Carbon page, breadcrumb,
   status, table, empty-state, and notification components.
5. Persist a selected profile ID/revision plus explicit local site binding and
   site-entered instance values. Profile selection MUST fetch and apply that
   revision's actual defaults; frontend protocol/communication constants cannot
   substitute for profile data. A later revision changes no analyzer until the
   user explicitly selects it, re-verifies, and synchronizes. Retain every
   referenced revision and never persist an authoritative copied profile.

**Exit:** MVP-001 through MVP-004, MVP-011, and applicable MVP-022 criteria pass;
GeneXpert/Fluoro setup and runtime parity pass, every profile published in the
M1 seed catalog passes the same-version contract/mock/assembled-flow gate, a
type is reusable by multiple analyzers, and no OpenELIS filesystem catalog,
hardcoded selected-profile default, or create-only copied snapshot remains
authoritative. No profile-count target can substitute for this evidence.

### M2 - Safe mapping editor (OGC-1056)

1. In BR-M2, expose normalized identities, raw codes, the human-readable
   `controlResultRecognition` summary, capabilities, and revision needed by the
   consumer contract. Implement `RULES`/`NONE` validation and remove every
   hard-coded ASTM/HL7/FILE classifier fallback.
2. In OE-M2, implement the one reusable mapping editor in Analyzer Types. Show
   every profile test row, including unmatched rows, with raw code, normalized
   identity, match state, and local Test selection. Shared normalized identities
   never collapse distinct source rows. Verify links to this editor with a
   return URL and does not implement a second editor.
3. Add/edit/remove/repoint test bindings using complete active Test catalog
   search by name, code, or LOINC. Suggest a binding only for one unique active
   candidate; zero or multiple candidates remain visibly unresolved. A row may
   be explicitly marked “do not receive”; one unresolved row never hides or
   blocks confirmation work on independent rows.
4. Bind qualitative values only to active Result Options owned by the mapped
   Test; derive value and label server-side; prove edit, enabled save, persisted
   state, and reload behavior for every applicable row.
5. Confirm Bridge control-result recognition separately from operational QC.
   `RULES` confirms every human-readable matcher; `NONE` confirms the explicit
   absence of automatic recognition. Raw fields/regexes never appear in analyzer
   connection setup.
6. Validate Test Catalog/setup return URLs and expose explicit Duplicate Profile
   or Update shared scope plus an affected-analyzer/update-available warning.
   Publishing either result never moves an analyzer implicitly.
7. Recompute completeness and stale verification after every relevant change.
8. Remove the OpenELIS `AnalyzerQcRule` editor, routes, controller, service/DAO
   runtime callers, profile seeding, Bridge payload, readiness use, translations,
   writes, entity, tests for superseded behavior, and schema. No migration
   reader or compatibility path remains.

**Exit:** MVP-005 through MVP-009, MVP-012, and applicable MVP-022 criteria pass
for ASTM, HL7, and FILE; one complete editor remains and invalid bindings are
rejected server-side.

### M3 - Guided setup, connectivity, and linked operational QC (OGC-1057)

Resume M3 from current branch evidence, not from narrative assumptions. Select
the first slice below whose acceptance is not proven in the M3 pull request,
and complete it before starting the next:

1. consume the pinned profile's connection capability, complete URL-backed
   inline setup/Connect, and remove the superseded standalone interfaces;
2. link the canonical operational-QC workflow without coupling it to analyzer
   verification or activation;
3. make one activation service the only `ACTIVE` writer and persist durable,
   exact verification evidence;
4. build and retain an immutable activation candidate, synchronize that exact
   desired state, and require the matching Bridge acknowledgement;
5. apply the complete non-QC activation predicate and expose the same precise
   blocker set through the lab-facing workflow;
6. replace hard delete and implicit recovery with audited
   deactivate/reactivate through the same lifecycle boundary;
7. prove profile-pinned, role-appropriate connectivity for the currently
   published priority set: GeneXpert ASTM, FluoroCycler FILE, and QuantStudio
   FILE. Change Bridge or mock production code only when a failing owning
   contract requires it; and
8. run the integrated, legacy-removal, Carbon/URL, desktop/mobile, and
   cross-repository gates, then publish the exact analyzer-only candidate and
   applicable Grist steps for review.

These are execution slices, not additional roadmap states or a second progress
ledger. Test and review evidence stays in the owning commits, pull requests,
and CI. M3 remains the sole `[*]` block until its complete exit gate passes.

1. Complete one inline Instrument -> Verify -> Connect story in OE-M3 with
   canonical URL/query state, linkable breadcrumbs, a readable completion
   summary, reload, back, and forward behavior. Do not add a fourth setup
   section unless the functional specification is amended.
   `/analyzers` remains the sole analyzer-instance dashboard and setup entry;
   analyzer-list setup and connection actions deep-link to that inline state.
   Remove the superseded standalone create/edit routes, `AnalyzerForm`, the
   separate connection-test modal, their navigation/actions, and their tests
   when the replacement is green. Do not retain redirects or compatibility
   routes for those superseded interfaces.
2. Provide searchable type selection. Instrument not listed links to the
   separate Analyzer Types create/Duplicate Profile workflow and returns with
   the new type selectable; analyzer setup never creates or silently mutates a
   profile.
3. Persist and display readable lab-unit assignments.
4. Require audited binding/control-recognition confirmation and make it stale
   only when the pinned profile revision, site binding, explicit exclusion, or
   recognition fingerprint changes. Operational-QC changes do not invalidate
   analyzer verification.
5. In BR-M3, execute profile-pinned probes and return protocol-appropriate
   structured evidence; separate connection initiator from Results only/Two-way
   capability. For a network/socket profile, OE-M3 consumes the pinned
   revision's declared communication mode and explicit LIS-initiated capability;
   omission or invalidity is a contract error, not `false`. For a FILE profile,
   Connect uses the declared FILE behavior plus the site directory and does not
   invent network communication, data-flow, address, or port values. OpenELIS
   never infers supported modes from the selected default, profile identity,
   protocol name, or frontend/server constants. Collect only role-applicable
   settings and show the endpoint a lab must configure. When a Two-way probe
   fails and the pinned profile supports Results only, offer that as an explicit
   user choice; never rewrite the saved mode automatically. For a Bridge
   receiver/listener, OpenELIS neither asks for nor registers a per-analyzer
   listen address or port: Bridge derives the advertised endpoint from the
   pinned profile's protocol/lower-layer behavior and its live listener
   configuration. Analyzer host/port settings apply only when the selected role
   requires Bridge to connect to the instrument. Probe the exact draft
   candidate transiently: a setup connection test neither runs desired-state
   synchronization nor creates, replaces, starts, stops, or otherwise mutates
   the analyzer's active Bridge runtime registration. Bridge validates and
   materializes the candidate through the same pinned-profile contract used by
   registration, then discards that transient runtime after the probe.
6. Render Connect as part of the URL-backed setup workflow. Save applicable
   instance settings before probing, then show Bridge's structured outcome,
   checks, missing configuration, timeout/failure reason, and configure endpoint
   in plain language. Do not substitute a progress animation, synthetic activity
   log, generic success tag, or a second modal workflow for that evidence.
7. Provide an analyzer-scoped link into the canonical OpenELIS Quality Control
   workflow, preserving analyzer context and a breadcrumb/back path. Retain
   `QCControlLot`, `QCResult`, QC statistics, Westgard configuration/evaluation,
   violations, and alerts; add no `QcRun`. Method-dependent required fields and
   exact validation remain visible in that QC workflow. Its state is reported
   separately and never changes analyzer activation or verification.
8. Apply the exact predicate in the resolved decision above on every transition
   into `ACTIVE`. Present one visible blocker for each false predicate and no
   others. In particular, no operational rule, control lot, QC result, Westgard
   status, or connection-test outcome appears in the list. A source row is
   ready when it is validly bound or explicitly excluded where offered and its
   ID is part of the current confirmation; the workflow does not display a
   false 100% mapping claim.
9. Make one OpenELIS activation service the only writer that can move an
   analyzer into `ACTIVE`. Analyzer create/update payloads cannot set `ACTIVE`
   directly, and mapping completion, connection-test success, error
   acknowledgement, or return from offline cannot activate automatically or
   bypass the predicate. Activation synchronizes the exact immutable draft
   candidate and requires the matching Bridge acknowledgement before promoting
   it; editing or synchronizing a draft cannot replace or mutate the last active
   candidate. Every reactivation uses this same path and produces the same
   blockers for the same candidate.
10. Replace analyzer-instance hard delete with audited deactivate/reactivate.
    Deactivation prevents new runtime use while preserving the pinned profile,
    site binding, history, held results, and operational-QC links. No hidden
    delete endpoint, direct status writer, event-driven activation path, or
    compatibility lifecycle route remains enabled.
11. Exercise GeneXpert ASTM, FluoroCycler FILE, and QuantStudio FILE
    role-appropriate connectivity against the existing analyzer-mock transport
    fixtures as part of the Bridge/OE contract gate. Resolve each mock
    template's exact `profileRef` through the accepted profile catalog/adapter;
    mock templates contain only simulation/fixture inputs and do not duplicate
    profile-owned communication behavior or OpenELIS instance defaults. These
    cross-repository fixture tests must run under the repository's ordinary
    test command; a test excluded by its filename or runner configuration is no
    evidence. M3 mock scope is limited to deterministic instrument traffic and
    connection behavior required by those priority fixtures. The mock owns no
    profile lifecycle, mapping, activation, QC, review, or duplicate product
    workflow. Reuse the accepted mock implementation when it satisfies the
    contract; change the mock only when a failing versioned transport test
    proves missing analyzer behavior.
12. Correct the `QCResultCreatedEventListenerTest` fixture so it supplies
    `QCResultDAO`, reloads the persisted result, verifies that the reloaded result
    reaches Westgard evaluation and violation handling, and fails on an
    unintended listener exception. A test that passes only because the listener
    catches and logs a missing-dependency failure is not acceptance evidence.
13. Make local checkpoint assembly exact and reproducible. The supported local
    build must rebuild OpenELIS, its frontend, Bridge, and analyzer mock from the
    checked-out candidate and verify the resulting image/profile revisions before
    tests run. A healthy container that was built from older companion code is a
    failed precondition, not checkpoint evidence.
14. Make analyzer fixture preparation deterministic and rerunnable in the
    isolated analyzer stack. It must not call a removed product hard-delete
    route, restore hard delete for test convenience, silently continue after a
    reset/schema error, or alter the shared AMR deployment. Harness-only fixture
    reset remains a test precondition and is not an analyzer lifecycle path.
15. Add a focused UI-only Playwright checkpoint story for the assembled M3
    workflow. It uses visible controls to exercise inline setup, canonical URL
    state and navigation, profile-appropriate connection evidence, the linked
    independent Quality Control workflow, activation blockers, activation,
    deactivation, and reactivation. Run it without video first and inspect its
    console output, trace, runtime state, and desktop/mobile screenshots before
    publishing the checkpoint preview. It does not replace the complete M4/G0
    story or final MP4.

**Exit:** MVP-010 through MVP-016 and applicable MVP-022 criteria pass in OE-M3
and BR-M3; a lab administrator can activate a complete analyzer without
developer fields or file edits, and runtime setup/probes occur in Bridge. The
only analyzer-administration surfaces are the `/analyzers` instance dashboard
with inline setup and the distinct `/analyzers/types` reusable-type manager;
the linked Quality Control workflow is not a third analyzer setup surface. No
standalone create/edit or connection-test workflow remains. GeneXpert,
FluoroCycler, and QuantStudio fixture gates run in the ordinary owning suites.
The supported isolated stack proves that all four candidate images are current,
fixture preparation is rerunnable without a product delete path, and the
inspected M3 UI-only story passes on desktop and mobile.
The exact green analyzer-only candidate is published to the review host without
changing the AMR deployment. Stable Grist steps `AN-MVP-006` through
`AN-MVP-010` and `AN-MVP-017` are served by the Review overlay against that
exact candidate before M3 changes from `[*]` to `[x]`; this preview does not
substitute for G0 human acceptance.

### M4 - Safe traffic and integrated MVP (OGC-1058 safety scope)

1. In BR-M4, preserve raw context and normalized identity for known, unknown,
   recognized-control, and FILE messages.
2. In MOCK-M4, supply deterministic real-transport fixtures; direct
   mock-to-OpenELIS delivery cannot satisfy a target-architecture test.
3. In OE-M4, stage known patient and QC results through the unified FHIR path.
4. Make Verify's visible “send a result” action capture real mock-to-Bridge
   traffic, reconcile every transmitted item as verified/new/not-seen, and
   keep independent source rows intact.
5. Populate a draft site type from received test/value/control-recognition rows;
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
11. Repository guards re-prove that M2 left no `AnalyzerQcRule`, OE-pushed
    classifier payload, Bridge hard-coded classifier fallback, or migration
    adapter and that M4 introduced none while assembling traffic.

**Exit:** MVP-017 through MVP-023 pass, all prior criteria remain green, and the
top of the three repository stacks forms one reproducible release candidate.

### G0 - Full MVP deployment and human acceptance

1. Create OE-G0 from OE-M4 and verify its ordinary Git/submodule state selects
   the intended OpenELIS, Bridge, mock, and profile-catalog versions.
2. Deploy the immutable OE-G0 commit only to
   `analyzers.openelis-global.org` with the merged review-tooling targeted
   deployment command.
3. Load deterministic fixtures as a deployment precondition. No Playwright user
   story may seed or mutate state through an API.
4. Freeze and verify the already-synchronized Grist story and 17 stable steps
   below through native Grist MCP or the Grist UI. Confirm the served revision,
   and keep old foundation stories unpublished rather than deleting or renaming
   them.
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
2. **R1.2 - Profile catalog, revision, and distribution operations.** Curate the
   remaining established source profiles in bounded profile-data PRs and add
   each to the Bridge catalog only after its contract, mock transport, Bridge
   behavior, and assembled visible result flow pass together. Show revision
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

| ID      | Criterion                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Primary proof                                         |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------- |
| MVP-001 | Analyzer Types lists shipped and site types with a plain-language explainer, aggregate counts, source, status, completeness, usage, and attention state.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Service/integration + RTL + UI E2E                    |
| MVP-002 | Search and filters round-trip through the URL and restore identical visible state after reload/back/forward.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | RTL with router + UI E2E                              |
| MVP-003 | Analyzer Types provides Create and Duplicate Profile as separate profile-management actions. Create reserves a unique incomplete draft that cannot be selected by analyzer setup before publication. Duplicate Profile starts a separate complete copied draft with a unique name and visible source lineage. Publishing either kind creates immutable revision 1 with actor and time; draft changes and publication cannot mutate the source profile or move analyzers pinned to another revision.                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Bridge contract + OpenELIS integration + UI E2E       |
| MVP-004 | Profile/type and analyzer-instance deactivation prevent new use while preserving references and history; reactivation is audited and revalidates the applicable current candidate; hard delete is unavailable through UI, API, service, and repository paths.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Service/integration + RTL + repository guard          |
| MVP-005 | Analyzer Types has the sole reusable mapping editor. It displays every accepted distinct profile result, including unmatched results, while proven aliases remain one definition; local lookup never skips or falsely completes one. Verify links to that editor with a return URL and contains no second/per-analyzer editor.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Contract + service + RTL + UI E2E                     |
| MVP-006 | Test selection searches the complete active catalog by name/code/LOINC; only a unique candidate may be suggested, while unresolved/explicitly-excluded choices remain independent per row and never block work on other rows.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | JUnit 4 + RTL + UI E2E                                |
| MVP-007 | Every applicable qualitative row can select only an active Result Option owned by its Test; selection enables save, server-derived value/label persist, and reload restores the binding.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | JUnit 4 + RTL + UI E2E                                |
| MVP-008 | Every active profile revision has exactly one valid `controlResultRecognition` mode. `RULES` has one or more valid OR matchers. `NONE` has no rules and a required author affirmation that the interface transports no control results; unknown/undocumented behavior cannot publish as `NONE`. Missing/invalid combinations are not selectable. Structured Analyzer Types authoring and Verify render human-readable behavior separately from operational QC; neither exposes regex, raw JSON, or raw matcher fields.                                                                                                                                                                                                                                                                                                                                                                                                                                             | Bridge schema/unit + OpenELIS contract + RTL + UI E2E |
| MVP-009 | Editing a referenced Analyzer Type requires explicit Update shared or Duplicate Profile scope and identifies affected analyzers. Update shared publishes a new immutable revision under the same identity; Duplicate Profile creates a new identity/revision. Existing analyzers remain on their pinned revision and show Update available until one is explicitly moved, re-verified, and synchronized. No Copy Mappings, clone, per-analyzer override, copied snapshot, or implicit bulk update bypass exists.                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Bridge/OpenELIS integration + RTL + UI E2E            |
| MVP-010 | `/analyzers` is the sole analyzer-instance dashboard and inline create/edit/setup entry. Setup supports searchable existing-type selection, name, readable lab units, Verify, Connect, and a completion summary; analyzer-list setup and connection actions deep-link to that state. Instrument not listed links to the separate Analyzer Types manager and returns with the created type selectable. Loaded profile/mapping/recognition/result counts and saved analyzer fields survive reload. No standalone create/edit screen, separate connection-test modal, duplicate navigation/action, redirect, or compatibility route remains.                                                                                                                                                                                                                                                                                                                          | RTL + UI E2E                                          |
| MVP-011 | Every page has one semantic `h1`, linkable breadcrumbs, and canonical URL/query state.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | RTL + UI E2E                                          |
| MVP-012 | Confirmation records actor, time, profile ID/revision, binding fingerprint, recognition fingerprint, and every confirmed/excluded source-row ID. Selecting another profile revision or changing a binding, exclusion, or recognition definition creates a draft candidate and stales only that candidate; the last active candidate remains unchanged until verified/synchronized. Operational-QC and connection-test changes do not stale either candidate.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | JUnit 4 integration + audit assertion                 |
| MVP-013 | Bridge connection testing uses only role-applicable settings and returns visible structured success, failure, missing-configuration, and timeout evidence plus the endpoint to configure. Connect displays that evidence inline without a synthetic activity log, generic status-only result, or second modal. For Bridge receiver/listener modes, the endpoint is derived from the pinned profile and live Bridge listener configuration; OpenELIS neither collects nor registers a per-analyzer listen address/port. Analyzer host/port fields appear only for roles where Bridge connects to the instrument. The exact draft candidate is validated and probed transiently through the pinned-profile contract without desired-state synchronization or any mutation of the active Bridge runtime registration.                                                                                                                                                 | Bridge contract + OpenELIS consumer + RTL + UI E2E    |
| MVP-014 | For network/socket profiles, setup uses the pinned revision's declared communication/data-flow default and explicit LIS-initiated capability and offers only supported modes; omission or invalidity is a contract error. For FILE profiles, setup uses declared FILE behavior plus the site directory and displays no invented network data-flow, address, or port. OpenELIS never infers capability from a default, profile identity, protocol, or application constant. A failed round-trip is visible and may lead to an explicit supported results-only choice; it never silently rewrites the candidate.                                                                                                                                                                                                                                                                                                                                                     | Bridge contract + RTL + UI E2E                        |
| MVP-015 | An analyzer-scoped Quality Control link opens the canonical OpenELIS QC workflow with analyzer context and a return path. `QCControlLot`, `QCResult`, QC statistics, Westgard configuration/evaluation, violations, and alerts remain the operational path; `AnalyzerQcRule` and `QcRun` do not. Valid/invalid QC changes never alter analyzer verification or activation blockers.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | JUnit 4 analyzer/QC + RTL + UI E2E                    |
| MVP-016 | Every transition into `ACTIVE` succeeds if and only if one immutable candidate has: an existing active schema-valid pinned profile revision; a nonblank analyzer name; at least one active lab unit; supported connection/data-flow modes and all profile-required instance fields; every declared test/result row validly bound or explicitly excluded where offered with exactly matching confirmed row IDs; current recognition confirmation for the same revision/fingerprint, including explicit `NONE`; and a Bridge acknowledgment matching analyzer ID, profile ID/revision, and canonical desired-state fingerprint. One service owns every activation/reactivation. Create/update status fields and mapping, connection, error, or offline events cannot set `ACTIVE` or bypass it. Each false predicate produces one visible blocker. Draft edits/sync never mutate the last active candidate. Operational QC and connection-test outcomes never block. | JUnit 4 + contract + UI E2E                           |
| MVP-017 | Bridge desired-state synchronization is versioned, idempotent, and deterministic for the pinned profile revision and instance runtime configuration. Its schema contains no OE classifier rules, control lots, Westgard configuration, or other operational-QC state, and repeated identical sync produces no behavioral change.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Cross-repo contract tests                             |
| MVP-018 | During Verify, visible live capture reconciles transmitted items as verified, new, or not seen. Known patient and control fixtures travel mock -> Bridge -> normalized FHIR -> OpenELIS; Bridge classifies the control only from the pinned profile and OpenELIS displays it in the QC workflow rather than as a patient result.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Mock + Bridge + harness integration + UI E2E          |
| MVP-019 | Unknown test/value traffic retains raw context, is held, creates visible attention/alert state, populates an unbound row during setup where applicable, and is not clinically posted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Contract + OpenELIS integration + UI E2E              |
| MVP-020 | A draft site type created in Analyzer Types can be populated from held live traffic; resolution uses valid local catalog choices, is audited, and makes the next matching message deterministic. Analyzer setup never creates or silently mutates a profile.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Integration + UI E2E                                  |
| MVP-021 | ASTM, HL7, and FILE each have patient, recognized-control, nonmatching-control, unknown-code/value, and connection fixtures. Contract tests prove multiple-rule OR behavior, explicit `NONE`, no hard-coded classifier fallback, and FILE watching/transport only in Bridge. Repository guards find no OpenELIS FILE poller or `AnalyzerQcRule` runtime/schema/UI path.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Mock/Bridge suites + repository guard                 |
| MVP-022 | All user copy is localized; Carbon components/tokens are used; desktop/mobile layouts have no overlap or unreachable action.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | RTL/a11y + inspected screenshots                      |
| MVP-023 | Playwright performs the complete visible story without `page.request`, API assertions, backend polling, forced controls, or arbitrary waits.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Playwright guard + test audit                         |
| MVP-024 | Generated target metadata and Git/submodule state identify the exact OpenELIS, Bridge, mock, profile, and review-tooling builds plus deployment time, checklist revision, routes, mark times, screenshots, trace, and MP4.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Target verifier + UAT report                          |

## Test Strategy

Each checkpoint's Git history and PR checks must show the first failing test,
passing implementation, and refactor result. A route or endpoint existing is
never a functional proof.

| Layer                     | Owns                                                                                                                                                    | Required approach                                                                    |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Bridge unit/service       | Profile revision validation, `RULES`/`NONE`, rule OR semantics, no fallback, parsing, transport, probes, idempotent registration                        | Bridge repository test conventions; real protocol fixtures                           |
| OpenELIS unit/service/DAO | Local catalog constraints, site binding, audit, exact stale-verification triggers, exact activation predicate, independent operational QC, hold/resolve | JUnit 4; ORM validation for new mappings; real Postgres where query behavior matters |
| Cross-repo contracts      | Pinned profile revision, runtime registration without operational QC, normalized patient/control FHIR, raw unknown/control context, FILE delivery       | Versioned fixtures run by both producer and consumer                                 |
| Analyzer mock             | Reproducible ASTM/HL7/FILE patient, recognized-control, nonmatch, explicit-`NONE`, unknown, failure, and supported two-way scenarios                    | `pytest`; deterministic IDs and values; transport to Bridge                          |
| Harness integration       | OpenELIS + Bridge + mock + database classification, QC routing, activation independence, and hold/resolve                                               | Real containers and transport; assert durable outcomes, not internal mocks           |
| Frontend                  | Carbon composition, accessibility, validation, routing/query/breadcrumb state                                                                           | Vitest/RTL with real router context and minimal network stubs at component boundary  |
| Playwright                | Complete lab-facing story                                                                                                                               | Visible UI only; seed may establish preconditions but cannot perform the story       |
| Remote UAT                | Human acceptance and visual coherence                                                                                                                   | Grist overlay against exact build, inspected outputs, final MP4                      |

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

Until the repository-wide TypeScript baseline is green, every OpenELIS
checkpoint still runs and reports `npm run typecheck` with the same toolchain as
its immediate predecessor. New or changed OGC-1054 files must have zero
diagnostics, and the total diagnostic count must not increase from that
predecessor. Do not suppress diagnostics, omit the command, or claim a global
pass. Repairing unrelated baseline diagnostics belongs in a separate PR and is
not silently absorbed into this feature stack.

## Full MVP Grist Checklist

Maintain one published Grist review for instance `analyzers`, host
`analyzers.openelis-global.org`, traceability key `OGC-1054`, and the current PR
links. Synchronize these 17 stable, required steps before the next checkpoint
preview and keep them visible in the Review overlay throughout implementation.
An interim reviewer executes the steps supported by that exact checkpoint and
leaves future steps unmarked; an interim report is feedback, not acceptance.
G0 requires a fresh pass on all 17 steps by the named human product reviewer
against one unchanged final deployment. Keep old foundation reviews unpublished;
do not rename these keys or reuse answers from another deployment/checklist
revision.

| Step key     | Reviewer action                                                                                                                                                                                        | Expected result                                                                                                                                                                                                                                                                                                                           |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AN-MVP-001` | Open Analyzer Types, search/filter, and inspect one shipped type.                                                                                                                                      | Source, status, completeness, usage, protocol, mapping, control-recognition mode, and attention information are understandable.                                                                                                                                                                                                           |
| `AN-MVP-002` | In Analyzer Types, start Create and observe the incomplete draft state; then Duplicate Profile, publish the copied draft, return to analyzer setup, and select it.                                     | Create does not expose an incomplete type to setup. The published duplicate has a unique profile identity/revision and source lineage without developer fields or file edits; the source and existing analyzers remain unchanged.                                                                                                         |
| `AN-MVP-003` | Open the Analyzer Types mapping editor, resolve one unmatched test, explicitly exclude another, then return to Verify.                                                                                 | The same sole editor is used; every accepted distinct result remains visible and proven aliases are not duplicated; complete catalog search works; selections persist independently; no per-analyzer editor appears.                                                                                                                      |
| `AN-MVP-004` | Map a qualitative analyzer value.                                                                                                                                                                      | Only active options for that mapped Test are selectable; reload preserves the binding.                                                                                                                                                                                                                                                    |
| `AN-MVP-005` | Inspect structured authoring and Verify for `RULES`/affirmed `NONE`; publish with Update shared and Duplicate Profile; explicitly adopt one new revision.                                              | Recognition is human-readable and separate from operational QC; `NONE` states that no controls are transported; no raw/fallback state appears; analyzers remain pinned/show Update available until explicit adoption, re-verification, and sync; duplicate identity/lineage are clear.                                                    |
| `AN-MVP-006` | Start inline analyzer setup, choose the type, name, and lab units.                                                                                                                                     | The URL, breadcrumb, visible section, and saved context remain coherent through reload/history.                                                                                                                                                                                                                                           |
| `AN-MVP-007` | Confirm mappings/recognition, inspect signer metadata, change a binding, then change only operational QC.                                                                                              | The binding change stales only the draft and leaves the active candidate unchanged; the operational-QC change stales neither.                                                                                                                                                                                                             |
| `AN-MVP-008` | Configure connectivity and run the connection test in inline Connect, then bookmark and reload it.                                                                                                     | Profile-declared defaults and supported Results only/Two-way choices are clear; only role-applicable fields appear; Bridge's structured success/failure/missing/timeout evidence and configure endpoint remain visible without a separate modal or simulated activity log.                                                                |
| `AN-MVP-009` | Follow the analyzer-scoped Quality Control link, inspect or complete a valid control-lot/Westgard configuration, and return.                                                                           | The canonical QC workflow retains analyzer context and validates its own fields; changing QC does not change mapping confirmation or analyzer activation blockers.                                                                                                                                                                        |
| `AN-MVP-010` | Review the seeded incomplete candidate's blocker list, complete its visible setup requirements, leave operational QC incomplete and the connection test failed, activate, deactivate, then reactivate. | Visible blockers match the applicable MVP-016 predicates and clear as their fields are completed; QC/test outcomes never appear; the synchronized exact candidate activates; deactivation preserves configuration/history while preventing new runtime use; explicit reactivation uses the same exact boundary; no Delete action appears. |
| `AN-MVP-011` | In Verify, request a live known result and inspect reconciliation.                                                                                                                                     | Seen items verify, absent items stay not seen, and new items remain explicit without data loss.                                                                                                                                                                                                                                           |
| `AN-MVP-012` | Create a draft site type in Analyzer Types, return to setup, request live traffic, and bind the populated rows.                                                                                        | Received rows populate visibly, remain held, and require explicit valid catalog choices before use; setup does not create or silently mutate the profile.                                                                                                                                                                                 |
| `AN-MVP-013` | Emit a known patient result and recognized control result through the visible mock control.                                                                                                            | Both travel through Bridge; the pinned profile alone classifies the control, which appears in OpenELIS QC and never as a patient result.                                                                                                                                                                                                  |
| `AN-MVP-014` | Emit an unknown test/value through the visible mock control.                                                                                                                                           | The result is held, not posted or lost, and the analyzer plus Alerts show Needs attention.                                                                                                                                                                                                                                                |
| `AN-MVP-015` | Resolve the unknown item and emit the same value again.                                                                                                                                                | Resolution is catalog-safe and audited; the next result maps without another unknown alert.                                                                                                                                                                                                                                               |
| `AN-MVP-016` | Exercise representative ASTM, HL7, and FILE patient/control/nonmatch scenarios through the visible demo controls.                                                                                      | The lab-facing outcomes are consistent; automated contracts separately prove `RULES`/`NONE`, no fallback, and Bridge-only FILE watching/transport.                                                                                                                                                                                        |
| `AN-MVP-017` | Review the completed analyzer on desktop and mobile and revisit bookmarked routes.                                                                                                                     | The summary, breadcrumbs, query state, actions, and responsive Carbon layout remain coherent.                                                                                                                                                                                                                                             |

The fixture loader may prepare catalog/sample data. The reviewer and Playwright
must execute every user action above through visible controls. Protocol path and
delivery guarantees are proven separately by harness contracts.

## Final Deployment And UAT Contract

### Checkpoint preview deployment

Once a UI checkpoint's owning PRs contain a coherent candidate and all
applicable automated gates are green, publish the exact pushed top OpenELIS
commit with its required Bridge/mock revisions to
`analyzers.openelis-global.org`. The candidate may be unmerged; deployment is
how the product reviewer inspects a PR before merge. Record the deployment link
and observed findings in the owning PR, then fix findings in that same
checkpoint.

Use only the review-tooling targeted command
`./deploy.sh app deploy analyzers --ref <commit> --scope app`. Before and after
deployment, verify the analyzer target/health and verify that the AMR target
metadata and health are unchanged. Do not run a full-host deployment, restart
the shared router/Grist services, reseed AMR, or alter AMR containers while
publishing an analyzer checkpoint. A Grist content edit should appear through
the live checklist service without a widget deployment; verify both the served
checklist revision and the visible Review overlay. Change review-tooling code
only in its own PR when a failing tooling contract proves that is necessary.

Preview review never changes `[x]`, `[*]`, or `[✓]` and never carries acceptance
forward to a later deployment. G0 repeats the complete review against the final
candidate and is the only source of full-MVP human acceptance and MP4 evidence.

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

- The established analyzer profiles are the target content/behavior baseline,
  not legacy rows. Their current OE packaging and copied application paths are
  temporary implementation debt.
- Existing profile files are curated into the strict Bridge contract one by
  one. Current rows survive only when semantically valid; proven alternate raw
  spellings may become aliases, distinct emitted results remain distinct, and
  unsupported/incorrect/duplicate rows are corrected or removed. LOINC equality
  alone never decides the disposition.
- `defaultConfigId`, copied plugin JSON, per-analyzer copied profile mappings,
  and the OE filesystem profile endpoints are deleted after Bridge-backed
  GeneXpert/Fluoro parity. They do not receive a compatibility writer or a
  permanent reader.
- No `LEGACY_UNBOUND` domain, legacy profile row, or preserve-every-row rule
  exists in the target. Unknown runtime codes/values are held as current raw
  traffic and resolved through the canonical pending workflow, not represented
  as legacy configuration.
- After cutover, one writer owns each capability. No dual-write to old and new
  profile/mapping/pending stores is allowed.
- `AnalyzerQcRule` is deleted rather than promoted into a target migration
  domain. Valid control-recognition behavior is authored and validated in the
  corresponding Bridge profile from protocol/vendor/capture evidence. Existing
  rows may inform that curation but never become authoritative or create a site
  profile automatically.
- By M2 exit, no caller, route, editor, writer, registration field,
  activation/readiness check, seed, translation, entity, DAO/service/controller
  class, superseded test, or schema references `AnalyzerQcRule`.
- Bridge contains no hard-coded ASTM, HL7, FILE, prefix, task, or field fallback
  for control recognition. `RULES` match or do not match; `NONE` never guesses.
- Raw OpenELIS ASTM/HL7/FILE runtime routes and direct mock-to-OpenELIS
  paths are removed or disabled before M4 exits. A follow-up issue cannot waive
  this gate.
- If a deployment contains real configured analyzer instances, E0 reports the
  legitimate site-specific facts requiring explicit one-time conversion. No
  generic runtime migration adapter is added solely because obsolete tables or
  demo fixtures exist.
- Repository guards prove there is no enabled OpenELIS FILE watcher/poller.

## Resolved Delivery Decisions

The canonical MVP review host is `analyzers.openelis-global.org`. The previously
written `analyzers.openelis-work.org` value is retired and is not a deployment or
Grist-publication target.

The analyzer instance model has one dashboard (`/analyzers`), one separate
reusable Analyzer Types manager (`/analyzers/types`), and links to canonical
operational-QC and attention workflows. Specialized linked workflows are not
alternate analyzer setup/admin screens, and superseded standalone create/edit
routes do not survive G0.

No architecture question remains open: Bridge is the analyzer runtime,
established-profile catalog, and control-result-recognition owner; OpenELIS owns local
clinical bindings, audit, operational QC, result-release policy, held results,
and lab-facing orchestration; the mock proves real Bridge transports; and
`openelis-work` remains functional/visual only.

PR #3390 is retained selectively: its OpenELIS operational-QC foundation
continues, while its `AnalyzerQcRule` classifier is superseded. Bridge PR #33's
OE-pushed classifier and hard-coded fallback behavior are superseded by the
versioned profile contract. Prepared Bridge PR #46 must use the additive
established-profile contract, preserve both profile jobs, adopt
`controlResultRecognition` with explicit `RULES`/`NONE`, ship the priority
evidence-backed catalog, and remove fallback before its checkpoint can be
accepted. The remaining established files are later curation inputs, not a
legacy runtime catalog and not an M1 publication-count gate.
