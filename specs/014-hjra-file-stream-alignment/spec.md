# Historical Feature 014: HJRA FILE Stream Alignment

**Status:** Superseded; retained as a provenance pointer only

**Superseded:** 2026-08-24

Feature 014 identified real FILE-analyzer needs, but its former specification,
plan, tasks, data model, OpenELIS file-import API, and remediation checklists
assigned FILE configuration, parsing, and polling responsibilities to
OpenELIS. That ownership is outside the accepted analyzer architecture. The
contradictory active documents have been removed; Git preserves their history.

## Current FILE Contract

- Analyzer Bridge owns the durable FILE connection, pinned profile revision,
  connection values, watch directory, file matching, parsing, archive/error
  handling, retries, and normalized transport.
- OpenELIS owns the reference to that Bridge connection, local clinical catalog
  bindings, normalized ingestion, result processing, held-result review,
  operational Quality Control, and audit.
- Analyzer mock owns deterministic FILE fixtures and instrument-like delivery
  into Bridge.
- OpenELIS has no FILE watcher/poller, raw FILE parser, FILE configuration
  writer, compatibility endpoint, or duplicate import queue in the target.

FILE configuration is created and edited through the same lab-facing OpenELIS
setup workflow as other analyzers, but the values are described, persisted,
validated, and interpreted only by Bridge. UI location does not transfer
authority.

## Current Authority

- [OGC-1054 feature specification](../OGC-1054-analyzer-qc-config/spec.md)
- [OGC-1054 authoritative roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)
- [`AGENTS.md`](../../AGENTS.md)

Any future change to this boundary requires an explicit approved architecture
amendment before implementation.
