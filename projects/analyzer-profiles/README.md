# Analyzer Profile Templates

JSON profile templates consumed by the three generic analyzer plugins
(GenericASTM, GenericHL7, GenericFile). A profile describes how an analyzer
identifies itself, which fields its messages carry, and how those fields map to
OpenELIS tests.

Architecturally this is the established Bridge-owned profile system. A profile
has exactly two jobs: define communication/runtime behavior for one analyzer
type and supply defaults when OpenELIS creates an instance of that type.
OGC-1054 adds strict validation, immutable revisions, and Bridge catalog
lifecycle around these semantics; it does not replace them with another profile
model.

## Transitional source

The distro's `configs/analyzer-profiles/` directory (mounted into the webapp as
`/data/analyzer-profiles`) is the source used by the current deployed
implementation. The copy under this repo is its local-development and test
mirror.

The physical mount is transitional; the working profile content and behavior are
the target baseline. The architecture in [`AGENTS.md`](../../AGENTS.md) and the
[OGC-1054 roadmap](../../specs/roadmaps/ogc-1054-analyzer-feature-roadmap.md)
places canonical catalog storage and analyzer-facing runtime behavior in
Analyzer Bridge. The cutover must preserve both profile jobs under compatibility
tests. Do not add OpenELIS parser/runtime behavior, make this webapp-mounted
copy a second authority, or introduce a second profile contract.

## Directory layout

```
projects/analyzer-profiles/
├── astm/   — GenericASTM profiles (TCP/IP ASTM LIS2-A2)
├── hl7/    — GenericHL7 profiles (TCP/IP HL7 v2.x over MLLP)
└── file/   — GenericFile profiles (filesystem CSV / Excel / ODS drops)
```

## Current consumers

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

## Profile changes before catalog cutover

Until the Bridge profile lifecycle milestone lands, changes must keep the
current distro and this test mirror synchronized and include OE setup,
Bridge/mock, GeneXpert ASTM, and FluoroCycler compatibility evidence. After
cutover, Bridge authors, validates, versions, and publishes the same profile
model, and configured analyzers remain pinned to immutable revisions.
