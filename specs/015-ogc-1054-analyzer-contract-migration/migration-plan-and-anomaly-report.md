# OGC-1054 E0 Migration Plan and Anomaly Report

**Date:** 2026-08-13

**OpenELIS base:** `7115ea696632f78ef1f28b61e7c5fa08a59bb4c2`

**Bridge contract:** `5ce6d3842e908cb442ce616c06346526a343af80`

This report is engineering evidence derived from current OpenELIS and Bridge
code, database fixtures, executable contracts, and the isolated runtime. It
does not derive implementation from `openelis-work`; that repository is used
only for later functional and visual comparison.

## No-loss invariants

- Preserve every analyzer ID, name, lab-unit assignment, lifecycle status, and
  audit relationship.
- Preserve the exact copied plugin JSON until each represented concern has a
  target owner or is reported as an unresolved anomaly.
- Preserve every independent `analyzer_test_map` source row. Two rows may bind
  to the same local Test and must not be collapsed.
- Group analyzers only when their selected profile revision and complete local
  mapping fingerprints are identical; preserve divergent snapshots as explicit
  site forks.
- Preserve raw analyzer codes, aliases, raw result values, normalized coding,
  profile revision, and source metadata across the Bridge boundary.
- Bind automatically only when exactly one active local Test or Result Option
  satisfies all ownership constraints.
- Preserve `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard history;
  no `QcRun` or replacement operational-QC path is introduced.
- Preserve unresolved traffic in a held state. Unknown tests or values are not
  discarded or posted as clinical results.

## One-writer cutovers

| Concern | Before cutover | Cutover | After cutover |
| --- | --- | --- | --- |
| Portable profiles | OpenELIS filesystem files are migration input | BR-M1 | Bridge versioned profile catalog writes; OpenELIS stores only a reference |
| Analyzer profile association and test bindings | `defaultConfigId`, copied JSON, and current `analyzer_test_map` write | OE-M1 | OpenELIS versioned association and source-row site binding write |
| Result-value bindings | Free-text `qualitative_result_mapping` writes | OE-M2 | OpenELIS catalog-backed Result Option binding writes |
| Analyzer traffic | Deprecated raw import compatibility paths may receive characterized traffic | BR-M4 | Bridge normalized FHIR is the only analyzer traffic writer |

Each database cutover runs as a transaction: inventory, migrate unambiguous
records, persist anomalies, validate row counts and fingerprints, then switch
the application reader/writer. A validation failure rolls back the transaction
and leaves the prior writer active. No application release dual-writes old and
target state.

The OE-M1 inventory computes a canonical fingerprint over profile ID/revision,
sorted source-row identities, raw codes/aliases, local Test/component targets,
and unresolved states. Identical fingerprints may share one imported
site-binding revision. A changed row, missing row, copied transform, or result
mapping produces a different fingerprint and therefore an explicit fork. The
generic plugin `analyzer_type` is not part of this equivalence decision.

## Anomaly handling

The machine-readable inventory is
`fixtures/current-state-characterization.json`. Every anomaly has a stable code,
condition, and deterministic action. Unresolved profile identity, missing
source rows, inactive or ambiguous catalog targets, unbound qualitative values,
and post-cutover raw ingress all prevent current verification and activation.

The migration report for a deployment must include counts and identifiers for:

- analyzers scanned, migrated, and blocked;
- source rows scanned, migrated, preserved unresolved, and rejected;
- zero/one/multiple active Test candidates by normalized identity;
- qualitative values bound to one active Result Option or marked
  `LEGACY_UNBOUND`;
- deprecated ingress activity observed before and after the traffic cutover.

No anomaly is silently dropped, assigned the first database match, or hidden by
a readiness count of zero.

## Rollback

Rollback restores the pre-cutover PostgreSQL backup and the matching
pre-cutover Bridge release. It does not reverse-transform target state into
legacy stores. Deployment verifies backup identity and restoration before the
writer switch; a failed verification blocks cutover. Once target-only writes
begin, rollback is a coordinated restore, never a downgrade that continues to
write legacy state.

## Runtime characterization

An isolated local stack ran on 2026-08-13 with Compose project `ogc1054`:

- OpenELIS from OE-E0 base `7115ea696632f78ef1f28b61e7c5fa08a59bb4c2`
  plus the uncommitted E0 characterization changes;
- Bridge contract/runtime `5ce6d3842e908cb442ce616c06346526a343af80`;
- analyzer mock `d063356e5a8f82ca6a44cf809be1874a7d704f8e`;
- dedicated containers, volumes, networks, subnets, and host ports, running
  alongside the unrelated OGC-782 stack.

The focused visible connection workflow passed for the seeded GeneXpert ASTM
analyzer. The foundational ASTM transport workflow also passed from mock to
Bridge to normalized FHIR to OpenELIS and through the visible result-acceptance
screen. This is integration characterization, not final UI-only acceptance.

The runtime exposed two important current-state gaps:

1. The GeneXpert mapping page displayed a missing-mappings blocker while its
   total, required, and unmapped counters all read zero, despite twelve seeded
   `analyzer_test_map` rows. Current copied configuration and current mapping UI
   do not form a trustworthy target site-binding view.
2. A mock ASTM result arrived as normalized code `94500-6`, while the current
   analyzer-code cache contained raw analyzer codes and had no matching entry.
   The result was staged, but this demonstrates why the v1 bundle must preserve
   both normalized and raw source identity and why local binding cannot infer a
   match from coding order.

Browser inspection also recorded development-only Vite websocket failures, one
404, and Carbon deprecation warnings. They do not alter E0 persistence or
contract decisions and remain separate UI validation debt for their owning
checkpoint.

## Checkpoint evidence

- Red: `AnalyzerMigrationCharacterizationTest` failed because the ADR,
  one-writer phases, and anomaly inventory were absent.
- Green target: the migration characterization, PostgreSQL JSONB fixture
  support, and Bridge consumer contract tests pass together.
- Runtime target: isolated OpenELIS, Bridge, PostgreSQL, and analyzer mock remain
  independently addressable; no direct mock-to-OpenELIS path is used as target
  architecture evidence.
