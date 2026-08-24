# Feature Specification: Analyzer Types, Mapping, Setup, QC, And Safe Traffic

**Feature:** OGC-1054 analyzer management
**Roadmap:** [OGC-1054 authoritative roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
**Updated:** 2026-08-19

## Source Contract

This specification and its sibling roadmap, plan, tasks, checklist, and
acceptance matrix are the repository sources of truth for OGC-1054 delivery.
Current `DIGI-UW/openelis-work` analyzer requirements, prototypes, and mocks are
the functional and visual product source. They define what a laboratory user
should accomplish and what the experience should communicate; they do not
define implementation.

Jira is traceability only. It cannot supply or override requirements,
acceptance, status, architecture, persistence, repository ownership, or tests.
Technical-looking content in `openelis-work` is also non-normative.

## Problem

Laboratories can connect generic ASTM, HL7, and FILE analyzers, but the current
Bridge-owned profile system is packaged and managed through developer-oriented
files, and the experience still has incomplete mapping surfaces, copied
configuration, and fragmented setup pages. The profile-driven GeneXpert and
FluoroCycler flows prove the underlying model; this feature must make that model
manageable without replacing it. A laboratory administrator cannot yet
reliably establish what an analyzer code means, verify qualitative values and
control-result recognition, activate an instrument from one coherent workflow,
or safely resolve new traffic without developer intervention.

## Personas

- **Laboratory administrator:** creates reusable Analyzer Types, maps them to
  the local catalog, configures analyzer instances, verifies setup, and
  activates/deactivates them.
- **Laboratory reviewer:** inspects held/unknown analyzer traffic and resolves
  it using valid local catalog choices.
- **Product reviewer:** performs exact-build remote UAT and decides whether the
  complete MVP is accepted.

## MVP Outcome

The MVP is one complete laboratory-admin workflow, not the presence of selected
routes or APIs. A user can:

1. find shipped and site-created Analyzer Types and understand their source,
   protocol, lifecycle, use, completeness, and attention state;
2. create a reusable type or duplicate an existing type from the separate
   Analyzer Types management workflow without developer fields or file edits;
3. review every analyzer test and qualitative result value, bind each applicable
   item safely to the local catalog, and confirm how the selected Analyzer Type
   recognizes control results;
4. create an analyzer inline by selecting an existing type, assign readable lab
   units, apply and verify its defaults, configure supported connectivity, and
   understand every analyzer-setup blocker;
5. reach analyzer-scoped operational QC in the existing OpenELIS QC workflow
   without making QC-program readiness an analyzer-activation prerequisite;
6. activate when current binding/control-recognition verification and the same
   pinned Analyzer Type revision are synchronized to Bridge;
7. receive known patient and QC traffic through the Bridge, hold unknown
   traffic without clinical posting, and resolve it visibly and auditably; and
8. reload, bookmark, navigate, and review the same durable state in a coherent
   Carbon interface.

## MVP User Stories

### US-1 - Manage Reusable Analyzer Types

As a laboratory administrator, I can search, filter, inspect, create,
duplicate, deactivate, and reactivate reusable Analyzer Types in a dedicated
profile-management workflow so multiple analyzer connections can reference a
safe configuration without hidden per-connection profile copies.

**Verification:** M1 closes through `MVP-001` through `MVP-004`; corresponding
reviewer steps `AN-MVP-001` and `AN-MVP-002` run with the complete story at G0.

### US-2 - Bind Every Analyzer Concept Safely

As a laboratory administrator, I can use the Analyzer Types mapping editor to
see every independent source row, map tests and qualitative values using the
complete active local catalog, and confirm the Analyzer Type's human-readable
control-recognition behavior, so no unmatched or aliased row is hidden or
falsely completed and analyzer setup does not need a second editor.

**Verification:** M2 closes through `MVP-005` through `MVP-009`; corresponding
reviewer steps `AN-MVP-003` through `AN-MVP-005` run with the complete story at
G0.

### US-3 - Configure An Analyzer In One Guided Flow

As a laboratory administrator, I can identify the instrument, assign its name
and lab units, verify mappings, configure connectivity, review the resulting
state, and activate it without leaving a fragmented or developer-facing setup
path.

**Verification:** M3 closes through `MVP-010` through `MVP-016`; corresponding
reviewer steps `AN-MVP-006` through `AN-MVP-010` run with the complete story at
G0.

### US-4 - Process Known Patient And QC Traffic

As a laboratory administrator, I can observe a known patient result and a
recognized control result sent by a reproducible analyzer through the Bridge
into their correct OpenELIS workflows.

**Verification:** M4 proves the assembled known-traffic behavior through
`MVP-018`; reviewer steps `AN-MVP-011` and `AN-MVP-013` run at G0.

### US-5 - Hold And Resolve Unknown Traffic

As a laboratory reviewer, I can see that an unknown test or result value was
held rather than posted or lost, resolve it using a valid local catalog choice,
and observe the next matching message resolve deterministically.

**Verification:** M4 proves the hold-and-resolve behavior through `MVP-019` and
`MVP-020`; reviewer steps `AN-MVP-012`, `AN-MVP-014`, and `AN-MVP-015` run at
G0.

### US-6 - Use One Experience Across Protocols

As a laboratory administrator, I use the same setup and mapping concepts for
ASTM, HL7, and FILE analyzers while protocol-specific runtime behavior remains
outside the laboratory UI.

**Verification:** `MVP-021` closes through automated Bridge/mock contracts;
reviewer step `AN-MVP-016` runs at G0.

### US-7 - Preserve Navigable, Accessible State

As a laboratory administrator, I can bookmark and revisit each meaningful
state, follow breadcrumbs, use browser history, and complete the workflow on
desktop or mobile without overlapping or inaccessible controls.

**Verification:** `MVP-011`, `MVP-022`, and `MVP-023` close through real-router
RTL, accessibility, and inspected browser gates; reviewer step `AN-MVP-017`
runs at G0.

### US-8 - Review The Exact Release Candidate

As a product reviewer, I can identify the exact application and checklist
revision under review, complete all required steps, and produce an auditable
report and MP4 for the unchanged release candidate.

**Verification:** Generated target metadata, the 17-step Grist report,
screenshots, trace, console review, and MP4 all identify one G0 deployment.

## Functional Rules

### Analyzer Profiles

- The established Bridge-owned analyzer profile model is the feature baseline.
  A profile has exactly two jobs: define communication with one analyzer type,
  and supply the defaults used to create a new OpenELIS instance of that type.
- Communication includes the supported protocol/version, transport and
  direction, analyzer identity, emitted test/result vocabulary, parsing or
  extraction behavior, and control-result recognition required for that type.
- Instance defaults include the applicable connection/file choices and
  portable catalog-binding hints. Analyzer name, lab units, site address,
  credentials, and watch directory remain site-entered instance values.
- Profile lifecycle and revisioning extend this model. They do not create a
  second profile shape, remove established defaults, or move analyzer runtime
  behavior into OpenELIS.
- Published revisions are immutable. A configured analyzer remains pinned until
  a user explicitly reviews and adopts another revision.
- Existing profile content is curated from instrument evidence. Rows are not
  retained merely because they exist or share a LOINC; the accepted revision
  contains only valid emitted concepts and proven aliases.

### Analyzer Types

- An Analyzer Type is the lab-facing reusable configuration concept. The UI
  does not expose plugin classes, regular expressions, raw JSON, repository
  paths, or internal profile identifiers.
- Shipped and site-created types are searchable and distinguishable.
- The list includes a short plain-language explanation plus aggregate Total,
  In Use, Has Unmapped Results, and Deactivated counts.
- Profile management is distinct from analyzer-connection setup. An analyzer
  connection selects and references one existing Analyzer Type; it does not
  create, own, or silently modify that type.
- A type may be referenced by several analyzer connections. **Duplicate
  Profile** creates a new, independently editable type from an existing one,
  with a unique suggested name and visible source lineage.
- Each analyzer references one specific Analyzer Type revision. Publishing a
  newer revision does not change an existing analyzer until a user explicitly
  selects that revision, verifies it, and synchronizes the new candidate.
- Published revisions are immutable and retained while referenced. Update
  shared publishes a new revision under the same profile identity; Duplicate
  Profile creates a new identity and initial revision. Existing analyzers show
  Update available but stay on their pinned revision. OpenELIS does not keep an
  authoritative copied profile snapshot.
- Types and analyzers are deactivated/reactivated, never hard-deleted through
  the lab workflow.

### Mapping And Verification

- Analyzer Types contains the sole reusable mapping editor. Analyzer setup
  Verify reviews and confirms the selected revision; Resolve/Edit actions open
  that same editor with a return URL. There is no analyzer-specific or duplicate
  mapping editor.
- Every accepted profile result definition remains independently visible when
  it is a distinct emitted concept, even when two definitions share normalized
  coding or one local Test. Proven alternate spellings are presented as aliases,
  not duplicate rows.
- Test search covers the complete active catalog by name, code, or LOINC.
- A deterministic suggestion is allowed only when there is exactly one valid
  candidate. Ambiguous or absent candidates remain explicit.
- A row may be explicitly excluded from receipt. An unresolved or excluded row
  never hides or blocks verification work on independent rows; activation
  requires each row to be validly bound or explicitly excluded and confirmed.
- Qualitative values may target only active Result Options belonging to the
  mapped Test. Empty option sets provide a return-safe Test Catalog path.
- Control-result recognition is reviewed separately from operational QC. The
  reviewer sees either one or more plain-language control identifiers/conditions
  or an explicit statement that the type does not support automatic control
  recognition. Structured Analyzer Types authoring and analyzer setup expose no
  regular expressions, raw JSON, or raw matcher fields.
- Explicit `NONE` is publishable only when the profile author affirms that the
  analyzer interface transports no control results. Unknown or undocumented
  recognition remains invalid; `NONE` is never a default or fallback.
- Human verification records actor, time, Analyzer Type revision, binding
  fingerprint, recognition fingerprint, and confirmed/excluded source rows. It
  becomes stale on the draft candidate after selecting another type revision or
  changing a binding, exclusion, or recognition definition. The last active
  candidate is not silently mutated. Operational-QC and connection-test changes
  do not stale either candidate.
- Editing a type identifies every referencing analyzer. A user may update that
  type or use Duplicate Profile first; analyzer setup never creates a hidden
  per-analyzer override or copy.
- Analyzer-reported internal-control targets are mapped as test/result concepts;
  they are not automatically whole-run control recognition or operational QC.

### Guided Setup And Readiness

- Add Analyzer expands inline while the analyzer list remains available.
- The canonical sequence is Instrument, Verify, and Connect. Each section has
  linkable URL/query state and a breadcrumb path; completion has a readable
  summary, not an invented fourth setup section. Analyzer-list setup and
  connection actions deep-link to that state. No standalone create/edit route,
  redirect, compatibility route, or separate connection-test modal remains.
  The only analyzer-administration surfaces are the Analyzer dashboard and the
  distinct reusable Analyzer Types manager; linked Quality Control is not a
  third analyzer setup surface.
- Instrument selection is searchable and selects an existing Analyzer Type.
  An unlisted instrument links to the separate Analyzer Types create/duplicate
  workflow and returns to analyzer setup with the new type selectable.
- For network/socket profiles, setup begins with the selected revision's
  declared communication/data-flow default and explicit LIS-initiated
  capability. Missing or invalid capability is a profile-contract error, not a
  false value. For FILE profiles, setup uses declared FILE behavior and the
  site-entered directory without inventing network data-flow, address, or port
  values. Only modes supported by the profile are offered; OpenELIS does not
  infer capability from a default, profile identity, protocol, or application
  constant. A failed round-trip probe is shown visibly and may support an
  explicit user choice to use a supported results-only mode; it never silently
  rewrites the profile or instance configuration.
- Inline Connect reports Bridge's structured success, failure, timeout, and
  missing-configuration evidence in plain language and shows the endpoint the
  lab must configure. It does not replace that evidence with a simulated
  activity log or generic status-only result. Bridge validates and probes the
  exact draft candidate transiently through the pinned-profile contract. A
  connection test does not run desired-state synchronization or create,
  replace, start, stop, or otherwise mutate the active Bridge runtime
  registration.
- Binding/control-recognition verification and operational-QC state are distinct.
  Operational QC never blocks creating, connecting, or activating an analyzer.
- Operational QC is configured and reviewed in the existing OpenELIS Quality
  Control workflow, reached through an analyzer-scoped link. Its state governs
  QC evaluation and patient-result release/hold policy, not interface
  activation, and changing it does not stale mapping verification.
- Every transition into `ACTIVE` applies the same exact predicate. One candidate
  must have an existing active schema-valid pinned profile revision; a nonblank
  analyzer name; at least one active lab unit; supported connection/data-flow
  modes and every profile-required instance field; every declared test/result
  row validly bound or explicitly excluded where offered with matching
  confirmed row IDs; current recognition confirmation for the same revision and
  fingerprint, including explicit `NONE` (no automatic recognition); and a Bridge
  acknowledgment matching analyzer ID, profile ID/revision, and canonical
  desired-state fingerprint. Each false predicate is one visible blocker. A QC
  rule, control lot, QC result, Westgard status, and connection-test outcome are
  never activation prerequisites.
- Verify offers visible live capture. Every transmitted item reconciles as
  verified, new, or not seen; unknown items remain held. A draft type created
  in Analyzer Types may be populated from received rows, but analyzer setup
  never silently creates or mutates a profile.

### Traffic Safety

- Known patient and QC traffic enters OpenELIS through the Bridge-normalized
  path and appears in the correct workflow.
- Unknown tests and values retain raw context, are held, and create visible
  attention without clinical posting or data loss.
- For MVP, hold-and-review is the only enabled behavior for an unmapped value.
  Raw pass-through or default substitution cannot clinically post and requires
  a later explicit clinical-safety specification before it can be offered.
- Resolution uses valid local catalog choices, is audited, and changes future
  matching deterministically.
- FILE behavior is equivalent from the lab user's perspective while the Bridge
  alone watches directories, parses files, and transports normalized traffic.

### Navigation, Carbon, And Localization

- Every page has one semantic `h1`, linkable breadcrumbs, canonical URL/query
  state, and tested reload/back/forward behavior.
- New UI uses current reusable Carbon components and tokens. Controls use the
  appropriate Carbon pattern and remain usable at `1440x900` and `390x844`.
- All visible copy is externalized through React Intl.

## Out Of MVP

The post-MVP train owns mature alert triage and concurrency, profile revision
diff/bulk adoption/rollback beyond MVP's explicit one-analyzer adoption, backup
export and distribution hardening, scale, and representative-site rollout.

Multi-component target-to-result-component mapping, Results/Validation v4,
patient-report integration, broad maintenance/fleet health, and per-instrument
vendor validation are separate feature work. They must not be smuggled into an
MVP checkpoint or used to weaken MVP acceptance.

## Acceptance Contract

The normative acceptance IDs are `MVP-001` through `MVP-024` in the
[roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md). Their preconditions,
actions, observable outcomes, automated layers, and UAT steps are defined in
the [acceptance matrix](./contracts/acceptance-matrix.md). The
[UAT mapping](./contracts/uat-mapping.md) defines the 17 required human steps.

No criterion is accepted from an endpoint, route, mocked assertion, screenshot,
or video that does not exercise the assigned observable outcome. The complete
MVP is accepted only at G0 against one exact deployment.
