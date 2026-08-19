# OGC-1054 Clean Cutover Contract

This contract defines deletion of superseded OpenELIS and Bridge pathways after
their accepted replacements prove assembled parity. It creates no production
migration mode.

## Rules

- **No runtime adapter.** Site conversion, if real deployment data requires it,
  is an explicit offline preflight and approved one-time operation.
- **No compatibility reader.** Once a deletion gate passes, the old store or
  route is not read by product runtime.
- **No dual write.** Exactly one writer owns each capability before and after
  cutover.
- Profiles are curated as profile documents from protocol, vendor, capture, and
  mock evidence. Database rows do not create profile truth.
- The active candidate is unchanged until a new pinned candidate is explicitly
  verified, synchronized, and activated.

## Deletion Targets

| Target | Accepted replacement | Gate | Deletion checkpoint |
| --- | --- | --- | --- |
| `defaultConfigId` | Explicit Bridge profile ID/revision selection and pin | Bridge-backed GeneXpert/Fluoro setup parity | OE-M1 |
| copied plugin/profile JSON | Site-only instance configuration plus profile pin | Candidate persistence and Bridge registration contract tests | OE-M1 |
| OE profile serving/application | Bridge profile catalog/detail and defaults | Catalog/setup contract plus assembled parity | OE-M1 |
| per-analyzer copied mappings | Revision-scoped OpenELIS site binding | Binding migration is unnecessary; M2 creates the sole target editor and binding model | OE-M2 |
| `AnalyzerQcRule` | Pinned Bridge profile `controlResultRecognition` | RULES/NONE, no-fallback, activation-independence, and assembled patient/control tests | OE-M2 |
| Bridge OpenELIS-pushed classifier and hard-coded recognition fallbacks | Pinned profile recognition only | Protocol and analyzer-mock contract suites | BR-M2 |
| raw analyzer import routes | Bridge-normalized FHIR ingestion | ASTM, HL7, and FILE known/unknown/control flows plus rollback rehearsal | OE-M4 |

The `AnalyzerQcRule` target means complete deletion: editor, routes, controller,
service, DAO, entity, registration fields, activation/readiness checks, seeds,
translations, superseded tests, and schema. No row becomes a profile rule or an
analyzer override.

## Pre-Cutover Proof

1. The new owning path begins with failing tests and reaches green at its owning
   layer.
2. GeneXpert ASTM and FluoroCycler prove the selected profile still supplies the
   runtime communication and instance defaults used by setup, Bridge, and mock.
3. The configured analyzer stores an immutable profile pin, site-owned state,
   verification/audit, and exact Bridge acknowledgement without a profile copy.
4. Repository guards prove the superseded path has no remaining caller, writer,
   UI, schema, fixture seed, or acceptance test.
5. Assembled runtime tests prove one writer and one result path before deletion.

## Real Deployment Data

The feature stack is new and does not presume deployed analyzer records. If an
environment does contain configured analyzers, operators run a read-only offline
inventory against that environment and approve explicit profile pins and local
bindings before deployment. Ambiguous data blocks that deployment. It does not
add a generic adapter, inference rule, copied profile, or fallback to the
application.

## Rollback

Before merge, rollback is ordinary Git provenance. Before a deployment cutover,
capture a coordinated OpenELIS database backup and Bridge catalog backup. A
failed cutover restores the coordinated pre-deployment pair or rolls forward;
it does not re-enable deleted product pathways or reverse-write target state.
