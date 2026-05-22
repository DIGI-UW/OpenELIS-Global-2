# Feature Specification: HJRA HL7 Stream Coordination

**Feature Branch**: `spec/013-hjra-hl7-stream-alignment`  
**Created**: 2026-03-10  
**Updated**: 2026-04-20 (status reckoning)  
**Status**: **In Progress** — coordination artifacts complete; M1 ready to
launch when HJRA networking + submodule sync are in place.  
**Input**: User description: "Create the HL7 coordination artifacts needed
before coding begins, establish stream boundaries and issue sequencing for
`OGC-325`, `OGC-326`, `OGC-327`, optional `OGC-336`, preserve the recommended
implementation branches, and keep this stage planning-only."

## Current Status (2026-04-20)

This spec coordinates the **HL7 stream** (Pattern A2) — HL7 v2.x over MLLP,
handled by the `GenericHL7` plugin against profile JSONs in
[`projects/analyzer-profiles/hl7/`](../../projects/analyzer-profiles/hl7/).
Integrating any new HL7 analyzer is a **profile-JSON drop** on top of this
stack; this spec's remaining work is about **stream infrastructure** and
**coordinated Jira-tracked milestones**, not per-instrument integration details.

### Generic HL7 support — capabilities

- **Transport**: MLLP listener owned by the bridge submodule (OGC-325). Accepts
  multiple concurrent analyzer connections routed by `MSH-3` + `MSH-4` + the
  `X-Analyzer-Id` forwarding header.
- **Plugin**: `GenericHL7` handles `ORU^R01` results via profile-driven
  `OBR`/`OBX` field mapping. `ORM^O01` outbound and `QRY^Q02` host queries are
  **post-MVP** and not yet shipped.
- **Communication mode**: `ANALYZER_INITIATED` only for MVP; `LIS_INITIATED` +
  `BOTH` are post-MVP (bridge outbound MLLP client needed).

### Remaining work (this stream, architecture-level)

- **Shared infrastructure shipped**: MLLP listener + `GenericHL7` code merged to
  develop (#3035). `X-Analyzer-Id` / `MSH-3+MSH-4` pattern matching (#3191).
  Cross-cutting test-connection parity + `CommunicationMode` enum in review as
  PR #3195.
- **Pre-M1 readiness** (T009/T010 in [tasks.md](./tasks.md)): sync `develop`,
  `tools/openelis-analyzer-bridge` and `plugins` submodule pins; document
  paired-PR team agreement.
- **M1 (OGC-325)** — MLLP listener foundation branch (paired bridge + main-repo
  PR), Gate 1 evidence.
- **M2 (OGC-327)** — first proving-target HL7 analyzer branch, Gate 2.
- **M3 (OGC-326)** — follow-on HL7 analyzer branch, Gate 3.
- **Post-MVP**: OGC-336 GeneXpert HL7 mode (QBP queries); HL7 bidirectional
  (ORM^O01 worklist, QRY^Q02 order download); LIS-initiated outbound MLLP from
  the bridge.

### Where per-analyzer HL7 status lives

- **Live tracker (canonical)**: [OpenELIS Global — Analyzer Integration
  Tracker][tracker] on Confluence — current spec confidence, deployment status,
  site-validation notes for each HL7 analyzer.
- **Protocol-level technical detail** (captures, mock flows, bridge specifics):
  `projects/analyzer-mock-server/` + `tools/openelis-analyzer-bridge`.

[tracker]: https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396

## Clarifications

### Session 2026-03-10

- Q: Which evidence gate should mark `OGC-325` ready before `OGC-327` can start?
  → A: End-to-end bridge + ACK + `/analyzer/hl7` ingestion proof required.
- Q: How should `BS-300` be handled within `feat/013-ogc-326-bs-series-hl7`
  while evidence is still incomplete? → A: Commit the branch to deliver both
  `BS-200` and `BS-300` together in the same branch, with early BS-300
  validation still required inside that branch.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Freeze The HL7 Stream Boundary (Priority: P1)

As a stream owner or reviewer, I need one coordination specification that
defines what belongs in the HJRA HL7 lane so that implementation does not begin
with conflicting assumptions about issue ownership, protocol lane, or branch
scope.

**Why this priority**: If the stream boundary is wrong, every downstream branch
can start from the wrong protocol lane or the wrong repository boundary.

**Independent Test**: Can be fully tested by reviewing the coordination spec and
confirming it identifies the `013` bundle, keeps `013` as a coordination
identifier rather than a Jira surrogate, states the ownership split between
bridge-managed transport work and analyzer-specific HL7 work, and preserves the
first implementation bundle boundary.

**Acceptance Scenarios**:

1. **Given** the HJRA HL7 stream is defined across multiple Jira issues,
   **When** a reviewer reads the coordination spec, **Then** the document
   identifies `OGC-325`, `OGC-326`, and `OGC-327` as the core HL7 bundle and
   treats `OGC-336` as optional rather than primary.
2. **Given** `OGC-325` covers HL7 over MLLP transport, **When** a reviewer
   checks the stream boundary, **Then** the document states that MLLP listener
   ownership belongs to the bridge submodule while remaining in scope for `013`
   through paired bridge and main-repository delivery.
3. **Given** the stream cannot validate analyzer-specific HL7 behavior without
   both transport and GenericHL7 readiness, **When** a reviewer checks the first
   bundle definition, **Then** the document treats `OGC-325` and GenericHL7
   completion as one practical readiness bundle rather than two unrelated
   starts.

---

### User Story 2 - Start With The Right Validation Target (Priority: P2)

As a technical lead, I need the coordination spec to define the first proving
target and the follow-on sequence so that implementation starts with the
narrowest useful validation path instead of expanding scope too early.

**Why this priority**: Even with the correct issue bundle, the wrong proving
target can delay validation, obscure transport problems, and widen the first
implementation slice unnecessarily.

**Independent Test**: Can be fully tested by checking that the spec sets one
prerequisite gate for the listener path, then identifies one first validation
target and one next committed BS-series branch scope.

**Acceptance Scenarios**:

1. **Given** the shared listener path is a prerequisite for analyzer-specific
   work, **When** the sequence is reviewed, **Then** `OGC-325` is treated as the
   prerequisite gate before analyzer validation begins and is not considered
   ready until the end-to-end bridge-to-OpenELIS ingestion path is proven.
2. **Given** BC-5380 has the clearest narrow proving scope in the current
   evidence, **When** the sequence is reviewed, **Then** BC-5380 is designated
   as the first validation target and the BS-series branch follows only after
   that path is proven.
3. **Given** BS-300 is not backed by the same level of evidence as BS-200,
   **When** the BS-series branch scope is reviewed, **Then** the branch is still
   committed to both analyzers while requiring early evidence validation for
   BS-300 before equivalence assumptions are treated as proven.

---

### User Story 3 - Keep Evidence Gaps Visible (Priority: P3)

As a reviewer preparing future implementation branches, I need the coordination
spec to preserve uncertainty where evidence is incomplete so that the team does
not overstate what has been confirmed.

**Why this priority**: The most expensive planning mistakes here are false
certainty and silent assumption carryover, especially around stale ASTM examples
and BS-300 equivalence.

**Independent Test**: Can be fully tested by confirming that the spec explicitly
names the remaining evidence gaps and does not convert them into settled facts.

**Acceptance Scenarios**:

1. **Given** BS-200 is explicitly supported by the current HL7 evidence but
   BS-300 is not equally supported, **When** the reviewer checks the spec,
   **Then** BS-200 is treated as confirmed and BS-300 remains a documented
   evidence gap.
2. **Given** older repo material contains stale ASTM examples, **When** the
   reviewer checks the guardrails, **Then** the spec explicitly prevents BC-5380
   and BS-series analyzers from being pulled into the wrong protocol lane.

---

### Edge Cases

- What happens when the bridge submodule and the main repository are not ready
  at the same time for `OGC-325`? The coordination spec must treat the work as a
  paired delivery gate rather than allowing one side to be declared complete in
  isolation.
- How does the stream handle missing source inputs such as absent GenericHL7
  documentation or an unavailable HL7 design reference? The coordination spec
  must record these as evidence gaps rather than filling them with invented
  behavior.
- What happens when repository references disagree on protocol lane because of
  stale ASTM examples? The HL7 coordination artifacts must defer to current
  Jira-backed HL7 scope and explicitly call out the stale references as
  contamination risk.
- How does the stream handle BS-series expansion if BS-300 equivalence is still
  unproven when implementation begins? The sequence must allow the combined
  BS-series branch to carry both analyzers while treating BS-300 evidence
  validation as an explicit early branch gate.
- What happens when the coordination branch is mistaken for a disposable
  document branch? The spec must state that the current branch is planning-only
  at this stage while still serving as the launch point for later issue-specific
  implementation branches.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: The `013` coordination specification MUST define the HL7 stream as
  a multi-issue bundle built around `OGC-325`, `OGC-326`, and `OGC-327`, with
  `OGC-336` documented as an optional alternative rather than a primary
  dependency.
- **FR-002**: The specification MUST state that `013` is a coordination
  identifier for planning and branch orchestration, not a replacement for a
  single umbrella Jira issue.
- **FR-003**: The specification MUST define a clear ownership boundary between
  bridge-managed HL7 transport work and main-repository or plugin-managed HL7
  parsing, routing, mapping, and validation work.
- **FR-004**: The specification MUST keep `OGC-325` in scope for the HL7 stream
  while describing it as bridge-submodule-owned MLLP foundation work that
  normally moves through paired bridge and main-repository pull requests.
- **FR-005**: The specification MUST require the listener foundation to be
  treated as the first readiness gate before analyzer-specific HL7 validation
  branches are considered ready to start.
- **FR-005a**: The specification MUST define `OGC-325` readiness as end-to-end
  proof that the bridge listener accepts MLLP traffic, emits the expected
  acknowledgment behavior, routes traffic into `/analyzer/hl7`, and completes
  one representative bridge-to-OpenELIS ingestion path.
- **FR-006**: The specification MUST treat `OGC-325` readiness and GenericHL7
  completion as one practical first implementation bundle for planning purposes,
  even though transport and analyzer parsing have different repository ownership
  boundaries.
- **FR-007**: The specification MUST designate BC-5380 as the first validation
  target after the shared listener path is ready.
- **FR-008**: The specification MUST designate the BS-series branch as the next
  committed profile wave after BC-5380 proves the listener and ingestion flow.
- **FR-009**: The specification MUST define `feat/013-ogc-326-bs-series-hl7` as
  a branch that is committed to delivering both BS-200 and BS-300 together.
- **FR-009a**: The specification MUST require early validation inside the
  BS-series branch to confirm whether BS-300 can safely share the BS-200 path
  before equivalence assumptions are treated as proven.
- **FR-010**: The specification MUST preserve the recommended implementation
  branch names `feat/013-ogc-325-hl7-listener-foundation`,
  `feat/013-ogc-327-bc5380-hl7`, and `feat/013-ogc-326-bs-series-hl7`.
- **FR-011**: The specification MUST define review and evidence gates that
  determine when the stream can move from coordination work into issue-specific
  implementation branches.
- **FR-012**: The specification MUST explicitly prevent stale ASTM examples or
  legacy documentation from reclassifying BC-5380 or BS-series analyzers into
  the wrong protocol lane.
- **FR-013**: The specification MUST treat the existing HL7 profile JSON files
  as seed inputs for planning and validation, not as proof that every downstream
  analyzer path is already complete.
- **FR-014**: The specification MUST record missing or incomplete planning
  inputs, including absent GenericHL7 documentation and unavailable design
  references, as named evidence limitations rather than silent assumptions.
- **FR-015**: The specification MUST state that the current `013` branch remains
  planning-only at this stage while also serving as the launch point for later
  issue-specific implementation branches.
- **FR-016**: The specification MUST exclude implementation on the coordination
  branch, profile library or sharing features, and any invented umbrella Jira
  that does not exist in the validated source material.
  - **Exception**: The current consolidation branch
    `fix/013-hl7-test-connection` (PR #3195) intentionally includes shared
    implementation for CommunicationMode enum, test-connectivity, and Liquibase
    migration. These are prerequisites shared across M1/M2/M3 milestones.
- **FR-017**: The HL7 stream MUST ensure test-connection parity between ASTM and
  HL7 analyzers. HL7 analyzers with IP/port configured MUST receive genuine TCP
  connectivity testing rather than hardcoded success. The test-connection
  response MUST be contextualized based on the analyzer's CommunicationMode
  (ANALYZER_INITIATED, LIS_INITIATED, BOTH).

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: Any future UI changes that come out of this stream MUST use Carbon
  Design System components exclusively and MUST NOT introduce other CSS
  frameworks.
- **CR-002**: Any future user-facing strings produced by HL7 stream work MUST
  use React Intl and include at least English and French translations.
- **CR-003**: Any future backend implementation spawned from this stream MUST
  follow the 5-layer architecture pattern, with transactions owned by services
  rather than controllers.
- **CR-004**: Any future schema changes required by HL7 stream work MUST be
  delivered through Liquibase changesets rather than direct DDL or DML.
- **CR-005**: Any future external-facing healthcare data introduced by this
  stream MUST preserve FHIR R4 and IHE alignment where applicable.
- **CR-006**: Analyzer variation across sites or models MUST remain
  configuration-driven rather than hardcoded into country-specific or
  site-specific code branches.
- **CR-007**: Future HL7 stream work MUST preserve security, RBAC, auditability,
  and input-validation requirements across both bridge and main-repository
  changes.
- **CR-008**: Future implementation work spawned from this stream MUST include
  automated tests and evidence gates across the relevant bridge and
  main-repository paths.
- **CR-009**: Because the HL7 stream is larger than a single small change,
  downstream implementation MUST remain milestone-based with one pull request
  per implementation slice.

### Assumptions & Constraints

- `013` is a planning-only coordination branch. It does not become a generic
  implementation branch and does not authorize implementation work by itself.
- The `013` coordination branch is not disposable. It exists to freeze the issue
  bundle and launch the later issue-specific implementation branches once the
  planning gates are satisfied.
- The bridge submodule is part of the project scope for planning purposes. MLLP
  listener work is therefore in scope for the HL7 stream even though its
  transport ownership lives outside the main repository tree.
- `OGC-325` normally requires paired bridge and main-repository pull requests
  because message transport readiness and OpenELIS-side ingestion readiness are
  operationally coupled.
- `OGC-325` is not considered implementation-ready based on bridge startup or
  configuration alone; the readiness gate requires one representative end-to-end
  bridge-to-OpenELIS ingestion proof with correct acknowledgment handling.
- The first practical implementation bundle combines `OGC-325` readiness with
  GenericHL7 completion because analyzer validation is not meaningful until both
  transport and OpenELIS-side ingestion behavior are aligned.
- BC-5380 is the preferred first proving target because it gives the stream a
  narrow validation path after the shared listener gate, without broadening
  immediately into the full BS-series surface area.
- The BS-series follow-on branch is committed to both BS-200 and BS-300, but
  BS-300 still requires explicit early validation inside that branch because
  current evidence is weaker than for BS-200.
- Optional GeneXpert HL7 work in `OGC-336` remains outside the primary proving
  path for this stage unless later review explicitly elevates it.
- Existing HL7 profile JSON files are seed evidence for branch planning and
  validation order, not proof that the later implementation branches are already
  fully specified.
- Missing GenericHL7 documentation and unavailable HL7 mapping reference
  material do not authorize invented requirements. They remain documented
  planning limitations until replaced by stronger evidence.

### Key Entities _(include if feature involves data)_

- **HL7 Stream Coordination Bundle**: The set of Jira-backed HL7 work items and
  planning decisions that define what belongs in the `013` stream and in what
  order those items become implementation-ready.
- **Bridge Listener Foundation**: The shared transport prerequisite that covers
  HL7 over MLLP listener readiness and its paired handoff into OpenELIS-side
  ingestion.
- **Analyzer Validation Target**: A named analyzer-specific proving slice used
  to validate the shared foundation before the stream expands to broader profile
  work.
- **Evidence Gate**: A documented readiness checkpoint that determines whether
  the stream can move from coordination to issue-specific implementation.
- **Implementation Branch Recommendation**: A branch name and scope statement
  that tells reviewers which future slice should start next and what it is
  allowed to include.

## Success Criteria _(mandatory)_

### Measurable Outcomes

_Note: "within 5 minutes" in SC-001 through SC-003 means one short reviewer
session; no formal timing measurement._

- **SC-001**: A reviewer can identify the complete in-scope HL7 issue bundle and
  each issue's role from the coordination spec within 5 minutes, without
  reopening the roadmap or Jira bundle notes.

- **SC-002**: A reviewer can determine from the coordination spec, within 5
  minutes, that the first required gate is the `OGC-325` listener foundation and
  that BC-5380 is the first analyzer validation target after that gate.
- **SC-002a**: A reviewer can determine from the coordination spec, within 5
  minutes, that `OGC-325` is not complete until MLLP listener behavior,
  acknowledgments, routing to `/analyzer/hl7`, and one representative ingestion
  path have all been demonstrated together.
- **SC-003**: A reviewer can determine from the coordination spec, within 5
  minutes, that `OGC-325` readiness and GenericHL7 completion are treated as the
  first practical implementation bundle for the HL7 lane.
- **SC-004**: The coordination artifacts contain zero conflicting statements
  about BC-5380 protocol lane, MLLP ownership, the BS-series branch being
  committed to both BS-200 and BS-300, or the need for early BS-300 evidence
  validation inside that branch.
- **SC-005**: The coordination artifacts preserve all three requested
  implementation branch recommendations and state when each becomes eligible to
  start.
- **SC-006**: The coordination spec leaves no unresolved ambiguity about whether
  `OGC-325` is out of scope, bridge-only, or paired work; it is clearly
  documented as in-scope bridge-owned foundation work with paired delivery
  expectations.
