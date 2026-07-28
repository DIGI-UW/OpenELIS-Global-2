# Feature 004: Analyzer Mapping

**Status:** Split into current configuration and result-processing milestones
**Current MVP owner:** [OGC-1054 Analyzer QC/config](../OGC-1054-analyzer-qc-config/spec.md)

## Purpose

Provide deterministic mappings between analyzer identifiers and OpenELIS tests
and result options. The configuration slice is protocol-neutral: ASTM, HL7, and
FILE analyzers use shipped profiles and the same analyzer-specific review
workflow.

## Current Configuration Scope

- Shipped profile defaults create analyzer test mappings exactly once.
- Administrators review profile-applied mappings and pending analyzer codes.
- Qualitative analyzer values bind to an active result option belonging to the
  mapped OpenELIS test.
- Legacy free-text values remain visible as `LEGACY_UNBOUND` and are incomplete.
- Administrators explicitly verify current mapping IDs/fingerprints.
- Mapping changes make prior verification stale.
- Mapping configuration participates in analyzer activation readiness.

The route, UI, service, audit, and acceptance requirements are normative in
[OGC-1054](../OGC-1054-analyzer-qc-config/spec.md).

## Deferred Result-Processing Scope

A subsequent milestone owns:

- ingesting analyzer result traffic into the pending-value queue;
- mapping analyzer targets to multi-component OpenELIS results;
- applying configured mappings during result import;
- routing imported results into Results/Validation v4;
- reprocessing held results after mapping resolution;
- conversions and ambiguity rules that require clinical result context.

That milestone must use stable component codes and preserve the primary
component as the default. Bridge changes are justified only by a failing
OpenELIS contract proving target identity is lost in transport.

## Invariants

1. A profile-expressible analyzer does not receive a custom OpenELIS adapter.
2. The configuration MVP does not claim production learn-from-traffic capture.
3. Verification is explicit and auditable; saving a mapping alone does not
   activate an analyzer.
4. Result options are server-validated against the mapped test.
5. No duplicate mapping editor or pending-value queue is introduced.
6. Human UAT validates visible workflow; service/contract tests validate payload
   and persistence internals.

## Acceptance

Configuration acceptance is AC-1054-06 through AC-1054-09 and AC-1054-13 in
[the OGC-1054 spec](../OGC-1054-analyzer-qc-config/spec.md). Result-processing
acceptance will be specified in its own milestone before implementation.
