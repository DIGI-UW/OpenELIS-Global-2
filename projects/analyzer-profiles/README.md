# Analyzer Profile Templates

JSON profile templates consumed by the three generic analyzer plugins
(GenericASTM, GenericHL7, GenericFile). A profile describes how an analyzer
identifies itself, which fields its messages carry, and how those fields map to
OpenELIS tests.

## Authoritative source

The distro's `configs/analyzer-profiles/` directory (mounted into the webapp as
`/data/analyzer-profiles`) is the **source of truth** for deployed environments.
The copy under this repo exists as a mirror for local development and unit
tests. When they drift, the distro wins.

## Directory layout

```
projects/analyzer-profiles/
├── astm/   — GenericASTM profiles (TCP/IP ASTM LIS2-A2)
├── hl7/    — GenericHL7 profiles (TCP/IP HL7 v2.x over MLLP)
└── file/   — GenericFile profiles (filesystem CSV / Excel / ODS drops)
```

## Consumers

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

## Authoring a profile

Profiles are read-only runtime assets — the schema is documented in the live
profile files themselves. When adding a new profile, place it in the
authoritative distro location first, then sync to this repo mirror.
