# OE-E0 Checkpoint Evidence

**Checkpoint:** OE-E0 contract and migration characterization

**Status:** In progress pending PR CI, review, and predecessor acceptance

## Scope

- Repository: `DIGI-UW/OpenELIS-Global-2`
- Branch: `codex/ogc-1054-e0-contract-migration`
- Base: OE-F0 `7115ea696632f78ef1f28b61e7c5fa08a59bb4c2`
- Companion: Bridge PR
  [#45](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/45) at
  `5ce6d3842e908cb442ce616c06346526a343af80`
- Included: engineering ADR, current-state migration fixtures, consumer
  contracts, anomaly/rollback report, isolated runtime characterization
- Excluded: M1 persistence/lifecycle, M2 mapping implementation, M3 setup/QC,
  M4 safe-traffic runtime, final UI story, deployment, UAT, and video

## Current-code baseline

- `AnalyzerRestController` treats `defaultConfigId` as a create-time filesystem
  hint and copies defaults into analyzer-specific state.
- `AnalyzerPluginConfig` stores per-analyzer JSONB without a durable versioned
  profile reference.
- `analyzer_test_map` is keyed per analyzer and raw analyzer test name.
- `qualitative_result_mapping.openelis_code` is free text, not a local Result
  Option identity.
- `AnalyzerFhirImportController` stages normalized FHIR and retains deprecated
  raw ASTM/HL7 routes as current migration inputs.
- Liquibase history first moved mappings to generic analyzer type, then moved
  them back per analyzer because shared generic adapters do not imply shared
  mapping behavior.

## Red

Commit: `a776758c9` (`test(OGC-1054): characterize analyzer contract migration`)

```bash
scripts/run-java21 mvn \
  -Dtest=org.openelisglobal.analyzer.migration.AnalyzerMigrationCharacterizationTest \
  test
```

Observed: six tests ran, one failed. The current-state data, JSONB, source-row,
catalog, and ingress checks passed; the failing assertion required the absent
`GROUP_IDENTICAL_BY_FINGERPRINT_PRESERVE_DIVERGENCE_AS_FORKS` migration rule.
Earlier red execution also identified absent ADR/phases/anomaly artifacts before
they were added.

## Green

The green commit adds ADR-001, deterministic one-writer phases and anomaly
actions, the migration/rollback report, and this checkpoint record. The same
targeted test must pass before the commit is published.

## Refactor

The refactor removed an incidental assertion against inherited service sorting
and corrected the first ADR draft, which would have evolved the per-analyzer
map in place. The accepted decision uses a shared, revisioned site-binding
aggregate and groups legacy analyzers only by a complete identical fingerprint;
divergent snapshots become explicit forks.

## Layer validation

| Layer | Status | Evidence |
| --- | --- | --- |
| OpenELIS persistence/current-state | `RUN` | Real PostgreSQL through `AnalyzerMigrationCharacterizationTest` |
| Cross-repository contract | `RUN` | Bridge schemas/fixtures through `AnalyzerBridgeContractConsumerTest` |
| JSONB test support | `RUN` | `PostgresqlJsonbDataTypeFactoryTest` |
| Bridge producer | `RUN` | Bridge PR #45: 610 passed, 3 serial-hardware skips |
| Harness integration | `RUN` | Isolated OE/Bridge/mock; connection and ASTM result workflows passed |
| Frontend RTL | `NOT_APPLICABLE` | E0 contains no lab-facing UI behavior |
| UI-only Playwright | `LATER` | OE-M4 owns the full visible story |
| Remote UAT/video | `LATER` | OE-G0 only |

## Acceptance crosswalk

E0 supplies prerequisites for MVP-003, MVP-005 through MVP-009, MVP-012,
MVP-017 through MVP-021, and MVP-024. It accepts none of those product criteria
by itself. Its proof is the engineering boundary, executable wire fixtures,
current-state migration inventory, and no-loss/rollback rules.

## Legacy-path audit

- `defaultConfigId`, filesystem profiles, copied plugin JSON, per-analyzer
  mappings, free-text qualitative mappings, and raw ingress are explicitly
  migration inputs, not target authorities.
- No OpenELIS FILE poller, `QcRun`, duplicate pending queue, or dual writer is
  introduced.
- BR-M1/OE-M1, OE-M2, and BR-M4/OE-M4 own their named one-writer cutovers and
  removal/guard steps.

## Decisions

- Bridge owns portable profiles and analyzer runtime.
- OpenELIS owns one shared, revisioned local site-binding aggregate and analyzer
  references to it.
- Distinct profile source rows never collapse through LOINC, alias, or local
  Test identity.
- Only one active catalog candidate may be suggested automatically.
- Rollback restores paired database/Bridge state; it never reverse-transforms
  target writes.

These decisions derive from current OpenELIS/Bridge code, Liquibase history,
the accepted v1 contract, and ADR-001. No implementation direction comes from
`openelis-work`.

## Issues and ambiguities

- `AMB-E0-001` is resolved by ADR-001.
- `ISSUE-E0-001` remains open: isolated runtime showed contradictory mapping
  counters and normalized-code/raw-map mismatch. It is assigned to M2/M4 rather
  than patched in E0.
- OE-R0 still requires review and has an unrelated E2E failure; OE-F0 and BR-E0
  are green/mergeable. OE-E0 cannot become accepted out of global order.

## Final gate

Before PR publication: targeted migration + consumer + JSONB tests, Spotless,
frontend formatting/guards for changed harness files, isolated harness status,
git diff audit, and code-qa alignment/simplicity/cross-repo checks. After push:
CI and review threads determine the final transition; passing local tests do not
mark E0 accepted.
