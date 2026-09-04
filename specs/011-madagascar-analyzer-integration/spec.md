# Historical Feature 011: Madagascar Analyzer Integration

**Status:** Superseded as an engineering specification and delivery plan

**Superseded:** 2026-08-24

Feature 011 established important analyzer-program evidence: generic ASTM,
HL7, and FILE transport work; early profile files; analyzer fixtures; and
Madagascar instrument research. It also accumulated implementation directions
that no longer match the accepted analyzer boundary. Those directions are not
kept in the active specification tree merely for history; Git preserves the
original specification, plan, tasks, data model, and implementation plans.

## Current Authority

Current OGC-1054 engineering and acceptance are governed by:

- [OGC-1054 feature specification](../OGC-1054-analyzer-qc-config/spec.md)
- [OGC-1054 authoritative roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
- [`AGENTS.md`](../../AGENTS.md)

The fixed boundary is:

- Analyzer Bridge owns profiles, durable analyzer connections and their
  configuration, analyzer-facing runtime, parsing, probes, control-result
  recognition, and FILE watching/transport.
- OpenELIS owns the lab-facing workflow, a reference to the Bridge connection,
  lab units, local Test/Result Option bindings, verification/audit, activation
  intent, operational Quality Control, held results, alerts, and review.
- Analyzer mock owns deterministic instrument behavior and real protocol/file
  traffic used to test Bridge.

A profile retains exactly two jobs: define communication/runtime behavior for
one analyzer type and supply defaults when Bridge creates a new connection of
that type through OpenELIS setup. OpenELIS does not persist analyzer-facing
connection values or reconstruct Bridge runtime state.

## Retained Evidence

The `research/`, `templates/`, and `contracts/` directories may still provide
historical instrument evidence or independently scoped future-program inputs.
They do not set OGC-1054 architecture, implementation, checkpoint status, or
acceptance. Validate any retained fact against current code and the governing
specification before use.

Per-analyzer product intent and visual design come from
[`DIGI-UW/openelis-work@main`](https://github.com/DIGI-UW/openelis-work/tree/main/designs/analyzer-integration)
only as functional/visual input, never as implementation direction.
