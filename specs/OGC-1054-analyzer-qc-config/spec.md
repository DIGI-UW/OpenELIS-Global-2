# OGC-1054 Analyzer Management Specification

**Updated:** 2026-08-24

**Execution:** [authoritative roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)

**Review aid:** [feature map and remediation report](./feature-map.md)

## Authority

This specification defines the product and ownership contract. The roadmap is
the only delivery-state document.

- Repository specifications and current OE, Bridge, mock, and review-tooling
  code govern engineering.
- [`openelis-work@main`](https://github.com/DIGI-UW/openelis-work/tree/main/designs/analyzer-integration)
  supplies lab-facing workflow and visual intent only. Its data-model, API,
  persistence, and repository suggestions are not implementation direction.
- Jira is traceability only.

## Product Outcome

A laboratory administrator can manage reusable analyzer types, bind their
reported concepts to the local catalog, create and activate a physical analyzer
through one guided OpenELIS workflow, and safely receive known and unknown
patient and control traffic without developer-edited configuration.

The user sees one Analyzer dashboard, one separate Analyzer Types manager, and
links to existing Quality Control and Alerts. The user does not need to
understand plugins, JSON, regular expressions, protocol listeners, or Bridge
internals.

## The Three Objects

| Object | Owner | Purpose |
| --- | --- | --- |
| Analyzer profile revision | Bridge | Immutable analyzer-type definition with exactly two jobs: define analyzer communication/runtime behavior and provide defaults for a new connection |
| Analyzer connection | Bridge | Durable configured instrument with a profile pin, entered values, configuration revision, probe evidence, and runtime lifecycle |
| OpenELIS analyzer | OpenELIS | Local instrument identity, lab units, Bridge connection reference, local catalog binding, verification/audit, activation intent, results, and QC links |

`Analyzer Type` is the lab-facing composed view of a Bridge profile revision and
its OpenELIS site binding. It is not a second profile entity or plugin registry.

## Ownership

### Bridge

Bridge owns and persists:

- the profile catalog, immutable revisions, lifecycle, and validation;
- connection fields, defaults, entered values, secrets, revisions, and
  fingerprints;
- protocol and transport choices, listeners, parsers, probes, FILE watching,
  retries, and runtime restoration after restart;
- analyzer test/result vocabulary and control-result recognition from the
  pinned profile revision;
- normalized output with raw source context; and
- connection create, read, update, probe, activate, and deactivate operations.

### OpenELIS

OpenELIS owns and persists:

- analyzer name, lab units, permissions, and Bridge connection ID;
- local Test and Result Option bindings;
- mapping and recognition confirmation, fingerprints, actor/time, and audit;
- activation intent and the exact Bridge acknowledgment;
- held results, resolution, clinical review, alerts, and result release; and
- operational QC: control materials/lots, QC results, statistics, Westgard
  evaluation, violations, alerts, and corrective action.

OpenELIS may mediate Bridge calls for its Carbon UI. It must not persist or log
analyzer-facing connection values, interpret protocols, construct complete
Bridge runtime state, watch files, or parse analyzer messages.

The analyzer mock owns only deterministic instrument behavior and real ASTM,
HL7, and FILE traffic. Review tooling owns checklist delivery and evidence
provenance only.

## Profile Contract

The established profile system is evolved, not replaced. After cutover there is
one profile contract and one runtime catalog, both in Bridge.

Every publishable profile revision contains:

1. stable identity and display metadata;
2. complete protocol-conditional runtime behavior, including framing,
   transport, direction, identification, parsing/extraction, aggregation, and
   supported capabilities;
3. complete safe defaults for a new Bridge connection;
4. all known emitted test/result concepts and portable coding hints supported
   by evidence; and
5. explicit control-result recognition as `RULES` or affirmed `NONE`.

A profile never contains an OpenELIS database ID, lab unit, concrete connection
ID, site address, credential, watch directory, operational-QC policy, or hidden
fallback. Production code never branches on a profile ID, manufacturer, model,
display name, analyzer code, or fixture name.

Profile defaults are applied once when Bridge creates a connection. Later
connection edits do not mutate the profile. Published revisions are immutable
and retained while referenced. Updating a shared profile publishes a successor
revision; duplicating creates a new profile identity. Neither action moves an
existing connection.

Adopting a successor revision is explicit. Bridge preserves compatible entered
values, requests newly required values, removes no historical revision, and
creates a new connection-configuration revision. OpenELIS then re-verifies only
the local bindings and recognition summary affected by the new profile
revision.

For MVP, the runtime catalog publishes only the evidence-backed priority
profiles:

- GeneXpert ASTM;
- FluoroCycler FILE; and
- QuantStudio FILE.

Other source profiles are removed from runtime packaging and return only through
a later profile-by-profile curation and transport test. Git history preserves
their provenance; no placeholder, legacy profile, compatibility reader, or
minimum profile-count gate exists.

## Bridge Connection Contract

A Bridge connection has a stable ID and exposes a generic view containing:

- exact profile ID, revision, and fingerprint;
- stable field keys, input kinds, choices, conditions, requiredness, defaults,
  masked current values, and validation errors;
- configuration revision and fingerprint;
- readiness and explicit blockers;
- latest probe evidence; and
- desired and actual runtime state.

Bridge derives the field description from the selected profile and its generic
protocol adapter. OpenELIS renders that description with reusable Carbon form
components and returns edits unchanged. A synthetic valid profile must be able
to change fields or defaults without an OE production-code or schema change.

Updates require the expected configuration revision. Conflicts return the
current revision without overwriting either user's work. Probe is non-mutating.
An active connection keeps running its acknowledged revision until an explicit
activation applies a newer saved revision. Secrets are write-only or masked and
never returned to browser logs, audit payloads, or OE persistence.

Connection creation is idempotent by the stable OE analyzer identity, so a
retry cannot create duplicates. Activation/deactivation commands are also
idempotent and return the exact connection, profile, configuration, and runtime
references acknowledged by Bridge.

## Lab Workflows

### Analyzer Types

`/analyzers/types` is the reusable type manager. It supports search, filters,
profile detail, mappings, revision history, create, duplicate, publish,
deactivate, and reactivate. Meaningful list, profile, revision, tab, filter, and
return state is represented in the URL and every page has a linkable breadcrumb.

The page composes Bridge profile data with OpenELIS mapping completeness and
usage. It never exposes the superseded local `AnalyzerType` plugin registry.

### Mapping And Verification

The Analyzer Types workflow contains the sole mapping editor. Test selection
searches the complete active local catalog by name, code, or LOINC. Qualitative
values may target only active Result Options belonging to the selected Test.
Each distinct emitted concept remains visible; aliases require evidence.

Every row is recorded as bound, intentionally excluded, or unresolved. A human
confirms the exact binding revision and the profile's human-readable control
recognition summary. Unresolved rows remain visible and incoming matching
traffic is held; they do not prevent unrelated mapped traffic from being used.
Operational-QC state never changes or invalidates this verification.

Analyzer setup reviews this shared binding. Resolve/Edit opens the same Analyzer
Types editor with a return URL; no per-analyzer mapping editor exists.

### Guided Analyzer Setup

`/analyzers` is the only analyzer-instance administration surface. Add Analyzer
opens inline and keeps the list visible. Setup progresses through Instrument,
Verify, and Connect; completed sections collapse to summaries. Analyzer ID,
step, selected profile revision, and return state are bookmarkable.

Instrument selects an existing Analyzer Type, names the analyzer, and assigns
one or more active lab units. An unlisted instrument links to the separate
Analyzer Types authoring workflow and returns after a profile is published.

Connect creates or edits the Bridge-owned connection through generic fields.
The connection test shows structured success, failure, timeout, missing-setting,
and endpoint information. It never silently changes the selected data flow. A
failed two-way test may offer an explicit Bridge-described results-only choice;
the user must choose it.

Activation requires:

- a name and at least one active lab unit;
- current human confirmation for the selected profile and site-binding
  revisions, including acknowledged unresolved/excluded rows;
- a valid saved Bridge connection revision; and
- a matching Bridge activation acknowledgment.

Operational QC and probe success are not activation prerequisites. Every false
predicate is visible. Deactivation stops Bridge runtime but preserves the
connection, local record, audit, results, and QC history. No hard-delete path is
provided.

### Traffic And QC

Bridge sends normalized patient/control traffic with connection identity,
profile reference, classification, and raw context. Known traffic reaches the
correct OE workflow. Unknown tests or values are durably held, visibly flagged,
and never clinically posted or dropped. Resolution uses valid local catalog
choices, is audited, and deterministically affects the next matching result.

Control-result recognition is profile/runtime behavior in Bridge. Operational
QC is a separate linked OpenELIS workflow and result-release concern.
`AnalyzerQcRule` and `QcRun` are not part of the target architecture.

## One-Time Upgrade Migration

The old OE connection fields are present in released code, so removal requires a
versioned cutover rather than data loss or a permanent compatibility path.

1. Quiesce analyzer configuration and traffic.
2. Deploy Bridge connection persistence and its idempotent migration contract.
3. Apply an additive OE migration that adds the Bridge connection reference.
4. Run the repository-owned migration tool in `plan`, `apply`, then `verify`
   mode. It reads old configuration once, requires an explicit profile revision,
   creates the Bridge connection, and records migrated, needs-correction, or
   intentionally-excluded for every analyzer. It never guesses from a name,
   model, protocol, LOINC, or plugin class.
5. Correct every retained analyzer and verify Bridge restart restoration.
6. Deploy the final schema/code removal and resume traffic.

There is no dual writer. The migration tool is not loaded by the normal WAR and
is not part of the post-cutover runtime. G0 requires removal of the old analyzer
connection columns, local `AnalyzerType`/plugin registry where superseded,
`defaultConfigId`, copied profile/plugin configuration, complete desired-state
sync, `AnalyzerQcRule`, old readers/routes, and their tests. A fresh or approved
demo reset follows the new path directly.

## Scope

MVP includes reusable types, local binding and verification, guided setup,
durable Bridge connections, activation/lifecycle, linked operational QC, known
and unknown result traffic, three priority profiles, exact-build deployment,
17-step remote UAT, and inspected MP4 evidence.

Post-MVP OGC-1054 adds broader profile curation/distribution, revision diff and
bulk adoption, mature alert operations, maintenance/fleet health, and rollout
qualification.

Multi-component mapping, Results/Validation v4 integration, patient-report
changes, and broad vendor-by-vendor validation are separate milestones.

## Acceptance Rule

The roadmap's `MVP-001` through `MVP-024` and 17 `AN-MVP-*` steps are the
deterministic acceptance contract. An endpoint, mocked assertion, screenshot,
or video cannot substitute for the assigned owning-layer proof. G0 is accepted
only by a named human product reviewer against one unchanged deployed build.
