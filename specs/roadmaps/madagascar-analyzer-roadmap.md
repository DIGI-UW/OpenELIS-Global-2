# Madagascar Analyzer Integration Roadmap (Superseded)

**Status:** Superseded on 2026-08-24

**Current authority:**
[OGC-1054 Analyzer Feature Authoritative Roadmap](./ogc-1054-analyzer-feature-roadmap.md)

This file is a non-executable pointer. Git history preserves the former April
2026 roadmap, including its shipped-status claims, OpenELIS-owned connection
fields, copied profile configuration, FILE parsing path, Jira inventory, and
outdated remaining-work list. Those directions must not be used for current
implementation or acceptance.

The durable product outcome remains support for Madagascar laboratories using
reusable analyzer types across representative ASTM, HL7, and FILE instruments.
Current implementation follows these boundaries:

- Analyzer Bridge owns profiles and analyzer-facing runtime behavior,
  including protocols, listeners, parsing, probes, control recognition, FILE
  watching/transport, and outbound analyzer communication.
- OpenELIS owns analyzer records, lab units, Bridge connection references,
  local catalog bindings, verification/audit, activation intent, held results,
  review, alerts, and separate operational Quality Control.
- A Bridge profile defines analyzer-type communication behavior and the
  configuration fields/defaults for a new Bridge connection.
- Bridge pins the immutable profile revision and durably owns entered
  connection values. OpenELIS uses an abstract Bridge interface and does not
  interpret, store, or reproduce analyzer runtime behavior.
- The analyzer mock provides deterministic representative instrument traffic;
  it does not own product configuration or workflow behavior.

Use the canonical roadmap for checkpoint state and MVP acceptance. Use the
current protocol specifications only where they are explicitly aligned with
that roadmap. Use `DIGI-UW/openelis-work@main` solely for functional and visual
intent, never for implementation direction.
