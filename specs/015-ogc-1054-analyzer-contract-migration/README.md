# OGC-1054 E0 Contracts

This directory contains OpenELIS-owned executable contract fixtures for the E0
boundary defined by the canonical OGC-1054 specification and roadmap.

- `openelis-analyzer-reference.schema.json` permits only the Bridge connection
  reference and LIMS-owned lab units, catalog binding, verification, and
  activation acknowledgement.
- `analyzer-migration-manifest.schema.json` describes the one-time
  plan/apply/verify result. Every released source analyzer receives one explicit
  outcome, and any selected profile revision is recorded as a human selection.

Bridge-owned profile, connection, command, and normalized-traffic contracts are
consumed directly from `tools/openelis-analyzer-bridge/contracts/analyzer/v1`.
No copied profile, full-state registration, all-profile publication gate, or
runtime compatibility contract belongs here.

E0 defines and tests the boundary only. The migration executable and runtime
cutover are implemented and qualified in M3.
