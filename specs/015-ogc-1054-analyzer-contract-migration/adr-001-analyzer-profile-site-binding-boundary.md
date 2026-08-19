# ADR 001: Analyzer Profile and Site-Binding Boundary

**Status:** Accepted

**Date:** 2026-08-18

**Checkpoint:** OE-E0 / BR-E0

## Context

Current OpenELIS code mixes three concerns: copied bootstrap profile JSON,
site-specific Test and Result Option mappings, and analyzer-instance runtime
settings. It also stores analyzer-specific control-result classification in
`AnalyzerQcRule`. Current Bridge code owns analyzer-facing protocols and
transport but does not yet provide the immutable portable profile lifecycle
required by the target workflow.

The target must let a laboratory manage an Analyzer Type independently from an
installed analyzer connection. A configured analyzer must not change behavior
because a shared type is edited, and a portable profile must not contain local
OpenELIS catalog identities. The boundary also must support deterministic
verification, synchronization, migration, and rollback without two competing
authorities.

## Decision

### Bridge-owned portable profile

Analyzer Bridge owns each portable Analyzer Type profile and its analyzer-facing
behavior: protocol, listener/parser/probe behavior, connection-field definition,
source test and result declarations, control-result recognition, and FILE
watching/transport. A published profile revision is immutable and retained while
referenced. Bridge exposes an explicit profile identity and revision; it does not
silently replace one revision with another.

Control recognition has exactly one schema-valid mode on each active revision:
explicit rules, or an author-affirmed declaration that the interface transports
no controls. Bridge evaluates only that definition. It has no OpenELIS-pushed
classifier and no hard-coded fallback.

### OpenELIS-owned site binding

OpenELIS owns a revision-scoped site binding between the selected portable
profile revision and local clinical catalog identities, including Test, Result
Option, explicit exclusion, lab-unit applicability, verification, audit, and
activation state. OpenELIS also owns analyzer-instance identity and runtime
configuration needed to register that installed connection.

An analyzer pins the profile ID and revision plus the applicable OpenELIS site
binding. It does not own an authoritative copied profile snapshot or a separate
mapping editor. Updating a shared Analyzer Type publishes a new immutable
revision; existing analyzers remain pinned until a user explicitly adopts,
re-verifies, and synchronizes the new candidate.

### Verification and synchronization

The activation candidate identifies the pinned profile ID/revision, the complete
site-binding fingerprint, the profile's recognition fingerprint, and the
analyzer-instance configuration fingerprint. Bridge registration acknowledges
the same analyzer ID, profile ID/revision, and canonical desired-state fingerprint.
A mismatch is visible and cannot activate the candidate.

Operational QC remains an OpenELIS clinical workflow based on control lots,
control results, statistics, Westgard evaluation, violations, and alerts. It is
linked from an analyzer but does not alter profile verification, the desired-state
fingerprint, or activation blockers.

### Migration of current state

`defaultConfigId`, copied plugin JSON, `analyzer_test_map`, qualitative mapping,
raw ingress, and `AnalyzerQcRule` are migration inputs, not target authority.
Every analyzer requires an explicit selected profile revision; profile assignment
is never inferred from similarity.

For each analyzer, the complete active `AnalyzerQcRule` set is compared
canonically with the recognition behavior of that explicitly selected profile:

- Exact behavior is discarded only after the analyzer is pinned, verified, and
  synchronized and the migration outcome is audited.
- Valid divergent behavior requires a new site profile identity and immutable
  Bridge revision before that analyzer can be pinned.
- Invalid or untransformable behavior blocks migration visibly. It never enables
  a fallback, silent drop, or parallel classifier.

Inactive rows remain in the migration export and audit until the migration is
accepted. They never become runtime recognition behavior.

### One authority at every phase

No dual write is permitted. Each store or runtime path has one named writer
before and after its checkpoint cutover. Readers may temporarily support
preflight and rollback, but target writes begin only after preflight succeeds.
After target writes exist, rollback restores a coordinated pre-cutover database
and profile catalog or rolls forward; it does not reverse-transform target data.

## Persistence Direction

M1 persistence must model the references and site-owned state above using the
existing OpenELIS layered architecture and current Bridge contracts. E0 fixes
ownership and invariants, not table names. M1 must demonstrate that profile
identity/revision, site binding, analyzer candidate, verification, and audit can
be queried transactionally without copying portable profile authority into
OpenELIS.

## Alternatives Rejected

1. **Mutable shared profiles referenced by installed analyzers.** A profile edit
   would change analyzer behavior without adoption, verification, or audit.
2. **An authoritative profile snapshot in each OpenELIS analyzer.** This creates
   a second profile authority and makes revision lineage and reuse ambiguous.
3. **Per-analyzer mapping or classifier overrides.** These recreate the legacy
   pathway and make a profile revision non-deterministic across analyzers.
4. **Operational-QC readiness as an activation condition.** Clinical QC policy
   is separate from connection correctness and must not stale or block setup.
5. **Heuristic migration and dual write.** Either can silently bind the wrong
   clinical concept or leave two runtime behaviors active.

## Consequences

- Bridge lifecycle work must precede profile creation, revision, and duplicate
  behavior in OpenELIS.
- OpenELIS can compose portable metadata with local completeness without owning
  analyzer-facing runtime behavior.
- Published profile updates are explicit and reviewable; configured analyzers
  remain stable until adopted.
- Migration can stop on ambiguity or invalid state without losing legacy data.
- `AnalyzerQcRule` and raw OpenELIS analyzer readers can be removed after their
  one-writer cutovers, while operational QC remains intact.
