# Research: HJRA HL7 Stream Coordination

**Feature**: `013-hjra-hl7-stream-alignment`  
**Date**: 2026-03-10

## Decision 1: Treat `013` as a coordination ID, not an umbrella Jira

**Decision**: Keep `013` as the coordination identifier for the HL7 lane and
continue to anchor real implementation scope to the Jira-backed issues
`OGC-325`, `OGC-326`, `OGC-327`, with `OGC-336` optional.

**Rationale**: The roadmaps and Atlassian-backed notes consistently treat `013`
as the lane identifier used to organize the stream, not as a replacement issue.
Preserving that distinction prevents fake scope expansion and keeps later branch
naming practical without inventing a nonexistent umbrella ticket.

**Alternatives considered**:

- Treat `013` as a surrogate umbrella issue. Rejected because it would overstate
  authority and break traceability back to the actual Jira work.
- Ignore the coordination number entirely. Rejected because the repository
  roadmaps and SpecKit folders already rely on `013` as the planning anchor.

## Decision 2: Keep `OGC-325` in scope even though transport ownership lives in the bridge submodule

**Decision**: Model `OGC-325` as in-scope bridge-owned foundation work that must
be reviewed and promoted through paired bridge and main-repository readiness,
not as a bridge-only side concern.

**Rationale**: The main repository already exposes HL7 ingestion codepaths, but
the MLLP listener ownership belongs in `tools/openelis-analyzer-bridge`. The
stream therefore cannot honestly say the listener is out of scope, and it also
cannot honestly claim main-repository code alone proves readiness.

**Alternatives considered**:

- Treat `OGC-325` as entirely out of scope because the bridge owns transport.
  Rejected because analyzer validation cannot begin without transport readiness.
- Pull MLLP listener ownership into the main repo. Rejected because it conflicts
  with the current bridge architecture and would invent a new ownership model.

## Decision 3: Treat `OGC-325` readiness and GenericHL7 completion as one practical first implementation bundle

**Decision**: Downstream planning should not let transport readiness and
GenericHL7 readiness start as unrelated workstreams. The first implementation
bundle is only meaningful when both are aligned enough to prove one
bridge-to-OpenELIS ingestion path.

**Rationale**: A listener that accepts framed messages but cannot complete a
representative ingestion path does not unlock analyzer-specific validation.
Likewise, plugin-side parsing work without the transport path in place does not
prove the lane is real.

**Alternatives considered**:

- Mark bridge listener completion alone as sufficient to unlock BC-5380.
  Rejected because it leaves the critical handoff to `/analyzer/hl7` unproven.
- Delay all GenericHL7 work until after analyzer-specific branches begin.
  Rejected because that would push core ingestion uncertainty into the analyzer
  branches.

## Decision 4: Use BC-5380 as the first validation target, then move to a combined BS-series branch

**Decision**: Sequence validation as `OGC-325` readiness bundle first, `OGC-327`
BC-5380 second, and `OGC-326` BS-series third.

**Rationale**: BC-5380 provides the narrowest proving slice after the shared
listener path is ready. The BS-series branch is explicitly committed to both
BS-200 and BS-300, but current evidence is stronger for BS-200 than BS-300, so
the combined branch must start with an early BS-300 validation checkpoint. The
existing `mindray-bs360e.json` profile covers the BS-360E model, not BS-200 or
BS-300; dedicated profiles for BS-200/BS-300 are part of the evidence gap.

**Alternatives considered**:

- Start with BS-series first. Rejected because it broadens the first proving
  slice and risks hiding transport defects inside a wider analyzer surface area.
- Exclude BS-300 from the BS-series branch. Rejected because the chosen
  clarification committed the branch to both analyzers.

## Decision 5: Record evidence gaps instead of filling them with invented behavior

**Decision**: Missing GenericHL7 docs in the current worktree and the
unavailable external HL7 mapping reference remain named planning limitations.

**Rationale**: The current branch has enough evidence to freeze boundaries and
sequencing, but not enough to pretend every downstream mapping detail is fully
specified. Keeping those gaps visible protects the stream from false confidence
and from stale ASTM contamination.

**Alternatives considered**:

- Reconstruct missing documentation from memory or stale examples. Rejected
  because it would create hidden assumptions.
- Block all planning until every source is restored. Rejected because current
  evidence is already sufficient for stream-boundary and milestone decisions.

## Decision 6: Use planning contracts instead of inventing runtime API contracts

**Decision**: The `contracts/` output for this feature documents branch scope,
ownership boundaries, and readiness gates as markdown contracts rather than
OpenAPI or GraphQL artifacts.

**Rationale**: This branch does not introduce a new runtime API surface.
Inventing one would add false detail and undermine the planning-only constraint.
What needs to be locked down here is the handoff contract between bridge work,
main-repository readiness, and analyzer branch promotion.

**Alternatives considered**:

- Create placeholder OpenAPI specs. Rejected because they would look
  authoritative while describing functionality not yet designed.
- Leave `contracts/` empty. Rejected because the planning handoff rules are
  central enough to deserve explicit contractual form.

## Decision 7: Require explicit evidence before each branch can start

**Decision**: Each future implementation branch should have a clear promotion
rule:

- `feat/013-ogc-325-hl7-listener-foundation`: paired bridge/main-repository
  proof completed
- `feat/013-ogc-327-bc5380-hl7`: foundation proof completed
- `feat/013-ogc-326-bs-series-hl7`: BC-5380 proving path accepted, with BS-300
  early validation still pending inside the branch

**Rationale**: The main risk in this stream is not missing branch names; it is
starting the next branch with the wrong assumptions already baked in. Explicit
promotion rules prevent that.

**Alternatives considered**:

- Let branches start as soon as code owners are available. Rejected because
  availability is not evidence.
- Use only Jira status changes as gates. Rejected because ticket status alone
  does not prove cross-repo readiness.
