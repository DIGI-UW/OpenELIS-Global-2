# Analyzer Profile Templates

JSON profile templates consumed by the three generic analyzer plugins
(GenericASTM, GenericHL7, GenericFile). A profile describes how an analyzer
identifies itself, which fields its messages carry, and how those fields map to
OpenELIS tests.

## Transitional source

The distro's `configs/analyzer-profiles/` directory (mounted into the webapp as
`/data/analyzer-profiles`) is the source used by the current deployed
implementation. The copy under this repo is its local-development and test
mirror.

This is a migration baseline, not the target profile authority. The target
architecture in [`AGENTS.md`](../../AGENTS.md) and the
[OGC-1054 roadmap](../../specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md)
places portable analyzer profiles and analyzer-facing runtime behavior in
Analyzer Bridge. Do not add new OpenELIS parser/runtime behavior or make this
webapp-mounted mirror a second profile authority.

## Directory layout

```
projects/analyzer-profiles/
├── astm/   — GenericASTM profiles (TCP/IP ASTM LIS2-A2)
├── hl7/    — GenericHL7 profiles (TCP/IP HL7 v2.x over MLLP)
└── file/   — GenericFile profiles (filesystem CSV / Excel / ODS drops)
```

## Current transitional consumers

- **Seed script:** `projects/analyzer-harness/seed-analyzers.sh` — creates
  analyzers via the OE REST API using these profiles as bodies.
- **Unified form:**
  `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx` — loads a
  profile when the admin picks a "Default Config".
- **Bridge registration:** On analyzer creation, the OE backend registers the
  analyzer+profile with the bridge (`tools/openelis-analyzer-bridge/`), which
  uses the profile to parse incoming messages/files.
- **Mock server:** `tools/analyzer-mock-server/templates/` has a peer template
  per profile for generating test traffic.

## Profile changes during migration

Until the Bridge profile lifecycle milestone lands, changes must keep the
current distro and this test mirror synchronized and include Bridge/mock
contract evidence. New work must be compatible with migration to the
Bridge-owned catalog; do not introduce another app-side profile consumer.
