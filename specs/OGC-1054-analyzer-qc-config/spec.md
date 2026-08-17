# Feature Specification: Analyzer Types, Mapping, Setup, QC, And Safe Traffic

**Feature:** OGC-1054 analyzer management
**Roadmap:** [OGC-1054 authoritative roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
**Updated:** 2026-08-17

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
configuration experience still depends on developer-oriented profile files,
incomplete mapping surfaces, copied analyzer configuration, and fragmented
setup pages. A laboratory administrator cannot reliably establish what an
analyzer code means, verify qualitative values and QC identifiers, activate an
instrument from one coherent workflow, or safely resolve new traffic without
developer intervention.

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
2. create or fork a reusable type without developer fields or file edits;
3. review every analyzer test, qualitative result value, and QC-identification
   code and bind each applicable item safely to the local catalog;
4. create an analyzer inline, assign readable lab units, verify mappings,
   configure supported connectivity, and understand every readiness blocker;
5. configure profile-applicable operational QC through the existing OpenELIS
   QC workflow;
6. activate only when current verification, required QC, and Bridge runtime
   synchronization are ready;
7. receive known patient and QC traffic through the Bridge, hold unknown
   traffic without clinical posting, and resolve it visibly and auditably; and
8. reload, bookmark, navigate, and review the same durable state in a coherent
   Carbon interface.

## MVP User Stories

### US-1 - Manage Reusable Analyzer Types

As a laboratory administrator, I can search, filter, inspect, create, fork,
deactivate, and reactivate reusable Analyzer Types so multiple instruments can
share a safe configuration without hidden per-instrument copies.

**Verification:** M1 closes through `MVP-001` through `MVP-004`; corresponding
reviewer steps `AN-MVP-001` and `AN-MVP-002` run with the complete story at G0.

### US-2 - Bind Every Analyzer Concept Safely

As a laboratory administrator, I can see every independent source row and map
tests, qualitative values, and QC-identification codes using the complete
active local catalog, so no unmatched or aliased row is hidden or falsely
completed.

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

As a laboratory administrator, I can observe a known patient result and a QC
result sent by a reproducible analyzer through the Bridge into their correct
OpenELIS workflows.

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

### Analyzer Types

- An Analyzer Type is the lab-facing reusable configuration concept. The UI
  does not expose plugin classes, regular expressions, raw JSON, repository
  paths, or internal profile identifiers.
- Shipped and site-created types are searchable and distinguishable.
- The list includes a short plain-language explanation plus aggregate Total,
  In Use, Has Unmapped Results, and Deactivated counts.
- A type may be shared by several analyzers. A variation is an explicit fork
  with a unique suggested name and visible lineage.
- Types and analyzers are deactivated/reactivated, never hard-deleted through
  the lab workflow.

### Mapping And Verification

- Every profile source row remains independently visible even when two rows
  share normalized coding or one local Test.
- Test search covers the complete active catalog by name, code, or LOINC.
- A deterministic suggestion is allowed only when there is exactly one valid
  candidate. Ambiguous or absent candidates remain explicit.
- A row may be explicitly excluded from receipt. An unresolved or excluded row
  never hides or blocks verification work on independent rows; activation
  requires each row to be validly bound or explicitly excluded and confirmed.
- Qualitative values may target only active Result Options belonging to the
  mapped Test. Empty option sets provide a return-safe Test Catalog path.
- QC-identification codes are reviewed separately from operational QC rules and
  control lots.
- Human verification records actor, time, revision, and fingerprint and becomes
  stale after a relevant type/binding/identification change.
- Shared changes require explicit fork or update-shared scope and identify
  affected analyzers.

### Guided Setup And Readiness

- Add Analyzer expands inline while the analyzer list remains available.
- The canonical sequence is Instrument, Verify, and Connect. Each section has
  linkable URL/query state and a breadcrumb path; completion has a readable
  summary, not an invented fourth setup section.
- Instrument selection is searchable. An unlisted instrument starts a
  reusable site-type flow using lab-facing fields only.
- Results only is the safe default. Two-way is offered only when supported and
  visibly degrades to Results only when a round-trip probe fails.
- Connection testing reports success, failure, timeout, and missing
  configuration in plain language and shows the endpoint the lab must configure.
- Mapping/QC-identification verification and operational-QC readiness are
  distinct states. Activation shows every current blocker.
- Verify offers visible live capture. Every transmitted item reconciles as
  verified, new, or not seen; unknown items remain held. A blank site type is
  populated from received rows and still requires explicit valid catalog
  binding and confirmation.
- `AMB-M3-001` in the roadmap blocks M3 acceptance until the source of
  profile-applicable operational-QC obligations is explicitly defined.

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
diff/update/rollback, backup export and distribution hardening, scale, and
representative-site rollout.

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
