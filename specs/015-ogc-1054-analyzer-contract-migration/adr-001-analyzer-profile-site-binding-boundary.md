# ADR-001: Analyzer Profile and Site-Binding Boundary

**Status:** Accepted

**Date:** 2026-08-13

**Checkpoint:** OE-E0 / BR-E0

## Context

OpenELIS currently accepts `defaultConfigId` as a create-time hint, reads a
filesystem profile, copies configuration into `analyzer_plugin_config`, and
creates analyzer-specific rows in `analyzer_test_map`. Qualitative result
mappings retain a free-text OpenELIS code through `qualitative_result_mapping`.
Existing analyzers therefore have durable local identity and mappings, but no
durable association with a versioned portable profile and no catalog-backed
Result Option identity.

The accepted Bridge v1 contract gives each profile a stable `profileId` and
integer `revision`; each profile test has an immutable `sourceRowKey`, raw
analyzer code and aliases, optional normalized coding, and result-value
definitions. Desired registration names a profile revision and an opaque
`siteBindingRevision`. Normalized FHIR preserves raw and normalized context.
Those contracts contain no OpenELIS database identifiers.

Current database behavior also proves that distinct analyzer source rows may
bind to the same local Test. Any migration that keys a row only by LOINC, raw
code, or Test ID can collapse valid information. Current catalog queries can
return zero, one, or multiple active Tests for one LOINC; only the one-candidate
case is safe to suggest automatically.

## Decision

### Bridge-owned portable profile

Analyzer Bridge is the sole authority for portable profile identity, revision,
protocol behavior, source-row identity, raw codes and aliases, normalization,
QC identification, connection capabilities, parsing, and runtime transport.
Bridge owns FILE watching and transport. OpenELIS will not maintain a second
portable profile catalog or an analyzer runtime.

### OpenELIS-owned site binding

OpenELIS is the sole authority for analyzer instances, lab units, local Test and
Result Option bindings, verification/audit, operational QC, activation
readiness, held results, and downstream clinical processing. An analyzer
instance records the selected Bridge `profileId` and `revision`, plus an opaque
site-binding revision used in desired registration.

OpenELIS introduces a shared, revisioned site-binding aggregate associated with
exactly one Bridge profile revision. The aggregate owns immutable revision
identity, lifecycle/audit metadata, and a deterministic fingerprint. Its test
rows are keyed by `(site_binding_revision_id, source_row_key)`. The raw analyzer
code remains evidence and display data; it is not the target row identity. Test
and optional component foreign keys remain OpenELIS-owned. Distinct source rows
are preserved even when they share a raw alias, normalized coding, or local
Test.

An analyzer instance references the selected Bridge profile revision and one
site-binding revision. Multiple analyzer instances may reference the same
revision; changing a shared mapping creates a new revision and requires the
explicit update/fork decision assigned to M1/M2. The current generic
`analyzer_type` remains a plugin adapter/capability record and cannot own local
bindings: current Liquibase history proves that instruments sharing a generic
ASTM, HL7, or FILE adapter may still need different mappings.

At OE-M1 migration, complete effective legacy snapshots are fingerprinted from
the selected profile revision, every independent `analyzer_test_map` row, and
relevant mapping state. Analyzers with identical fingerprints may reference
one shared imported site-binding revision. Divergent snapshots become explicit
site forks. Missing or ambiguous profile/source-row identity remains unresolved
and blocks activation. The per-analyzer map is not mutated into the target
authority.

OE-M2 adds catalog-backed result-value rows to the site-binding revision, keyed
by site-binding revision, source-row key, and raw value. Their target is an
active `TestResult` belonging to the bound Test. The legacy
`qualitative_result_mapping.openelis_code` free text is migration input only;
it is never considered complete without a unique Result Option identity. The
owning milestones supply the exact Liquibase tables, ORM validation, audit
events, and deletion of superseded writable paths; E0 fixes the aggregate and
identity semantics without prematurely implementing that schema.

`analyzer_plugin_config` remains readable during migration for analyzer
instance connection overrides and copied legacy evidence. It cannot own a
profile, mapping, result binding, verification, or operational QC after the
respective cutover.

### No dual write

Each capability has one writer before and after its named cutover:

- before OE-M1, OpenELIS reads and writes the legacy analyzer/profile and
  per-analyzer test mapping state;
- after OE-M1, Bridge writes portable profiles and OpenELIS writes only analyzer
  associations plus shared, revisioned local site bindings;
- after OE-M2, OpenELIS writes only catalog-backed result-value bindings;
- before BR-M4, deprecated raw ingress remains characterization-only
  compatibility behavior;
- after BR-M4, Bridge-to-OpenELIS normalized FHIR is the only analyzer traffic
  writer.

The application does not write old and target stores in parallel. Cutover is a
transactional migration followed by a reader/writer switch. Legacy rows remain
read-only evidence until the milestone's verified removal step.

## Migration and rollback consequences

Migration preserves analyzer IDs, names, lab assignments, status, copied
configuration, every independent source mapping row, operational QC records,
and unresolved values. It never invents a profile association, merges source
rows, selects among duplicate local candidates, or converts free text into a
Result Option without an exact catalog match.

Unsafe records are emitted in a deterministic anomaly report and block current
verification/activation. They are resolved through the lab-facing workflow.

Rollback restores the pre-cutover database backup and the pre-cutover Bridge
release together. Target writes are not reverse-transformed into legacy rows;
doing so would lose profile revision, source-row identity, or Result Option
identity. A failed migration leaves the previous writer active.

## Rejected alternatives

- Persisting portable profiles in OpenELIS duplicates Bridge authority.
- Keeping copied plugin JSON as mapping authority makes revisions and audit
  ambiguous.
- Using LOINC, analyzer code, or local Test ID as source-row identity can merge
  independent rows.
- Extending free-text qualitative mappings cannot prove Result Option ownership.
- Dual writing legacy and target models creates conflicting authorities and an
  unverifiable rollback state.

## Verification

`AnalyzerMigrationCharacterizationTest` executes the current database inputs,
catalog edge cases, deprecated ingress inventory, one-writer phases, rollback
rule, and required engineering artifacts. `AnalyzerBridgeContractConsumerTest`
executes the exact BR-E0 schemas and canonical fixtures pinned by the Bridge
submodule SHA.
