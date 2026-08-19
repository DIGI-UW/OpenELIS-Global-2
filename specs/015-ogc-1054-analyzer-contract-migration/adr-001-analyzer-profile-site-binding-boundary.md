# ADR 001: Analyzer Profile and Site-Binding Boundary

**Status:** Accepted

**Date:** 2026-08-19

**Checkpoint:** OE-E0 / BR-E0

## Context

OpenELIS already ships an analyzer profile system used by analyzer setup, Bridge
registration/runtime, and analyzer-mock fixtures. Each established profile has
two jobs:

1. define runtime communication for one analyzer type; and
2. provide instance defaults when a laboratory creates an analyzer connection.

The target adds strict validation, immutable publication, and a lab-facing
catalog around that system. It does not replace the profile shape or treat
OpenELIS copies, mapping rows, or classifier rows as a second profile source.

## Decision

### Established Bridge-owned profile

Analyzer Bridge owns the established profile document and analyzer-facing
runtime behavior: protocols, listeners, parsing, probes, connection/default
fields, source test/result declarations, control-result recognition, and FILE
watching/transport. The contract remains protocol-discriminated: socket profiles
carry socket communication and non-empty default mappings, while FILE profiles
may be column-only. An invented model, socket field, tabular header, or mapping
row is never required merely to make a profile fit one universal template.

Published revisions are immutable and retained while referenced. Generated
revision, fingerprint, publication, and lineage metadata is catalog state; it is
not authored analyzer behavior.

Profiles are data. Production code selects behavior only from the loaded profile
and pinned revision. Analyzer IDs, names, manufacturers, models, raw codes,
mappings, recognition rules, connection values, and defaults are not embedded in
generic runtime or consumer code.

### OpenELIS-owned site binding

OpenELIS owns installed-analyzer identity, lab units, site-entered connection
values, local Test and Result Option bindings, explicit exclusions,
verification/audit, activation, held-result review, and operational state. An
analyzer stores a profile ID/revision pin and the OpenELIS-owned candidate; it
does not store an authoritative profile document.

No copied profile authority is permitted. Updating or duplicating a profile
never moves a configured analyzer. Adoption creates a new candidate that must be
reviewed, verified, synchronized, and explicitly activated while the prior
active candidate remains unchanged.

### Recognition and operational QC

Control-result recognition is profile behavior. Each publishable revision
declares evidence-backed `RULES` or an author-affirmed `NONE`; Bridge evaluates
that declaration without an OpenELIS classifier or hard-coded fallback.

`AnalyzerQcRule` is deleted from the target architecture. Its rows are not
converted into profiles, retained as overrides, or used to create site profiles.
Existing repository data may inform human curation only when independently
supported by protocol, vendor, capture, or mock evidence.

OpenELIS operational QC remains separate: control lots, control results,
statistics, Westgard evaluation, violations, alerts, and result-release policy.
Operational QC neither enters Bridge registration nor changes analyzer
verification or activation blockers.

### Verification and synchronization

The OpenELIS candidate records the pinned profile ID/revision, site-owned state,
binding fingerprint, profile revision fingerprint, recognition fingerprint,
verifier, verification time, audit event, and desired-registration fingerprint.
Bridge acknowledges the same analyzer key, profile reference, and desired-state
fingerprint. A mismatch is visible and cannot activate the candidate.

### One-way cutover

The current OE-hosted profile serving/application path, copied plugin/profile
JSON, `defaultConfigId`, per-analyzer copied mappings, `AnalyzerQcRule`, and raw
analyzer import routes are deletion targets at their roadmap gates. No runtime
adapter, compatibility reader, dual writer, or alternate acceptance path is
introduced. If a real deployment contains analyzer-specific site facts, an
offline preflight reports them for explicit approved conversion before cutover;
the product runtime remains clean.

## Consequences

- Bridge lifecycle work wraps the established two-job profile contract.
- All 20 source profiles receive an evidence-backed curation disposition before
  Bridge M1 publication; current rows are never accepted mechanically.
- OpenELIS M1 persists only the pin and site-owned candidate state.
- GeneXpert ASTM and FluoroCycler remain blocking assembled parity fixtures.
- Superseded OE and Bridge paths are removed at the named roadmap gates rather
  than maintained for compatibility.

## Alternatives Rejected

1. A second thin profile contract.
2. Mutable published profiles or implicit analyzer upgrades.
3. An authoritative copied profile in OpenELIS.
4. Per-analyzer mapping or classifier overrides.
5. `AnalyzerQcRule` conversion into Bridge profile data.
6. Operational-QC readiness as an analyzer activation condition.
7. Runtime adapters, compatibility readers, heuristic profile inference, or
   dual write.
