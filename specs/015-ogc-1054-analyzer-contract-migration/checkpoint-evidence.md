# OE-E0 Checkpoint Evidence

**Checkpoint:** OE-E0 contract and migration characterization

**Status:** In progress pending PR CI, review, and predecessor acceptance

## Scope

- Repository: `DIGI-UW/OpenELIS-Global-2`
- Branch: `codex/ogc-1054-e0-contract-migration`
- Base: OE-F0 `7115ea696632f78ef1f28b61e7c5fa08a59bb4c2`
- Base roadmap blob: `012a4934aea5862ce50b4c576302c07044ad15a3`
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

Commit: `b81ab63cf` (`docs(OGC-1054): fix analyzer migration boundary`)

The green commit adds ADR-001, deterministic one-writer phases and anomaly
actions, the migration/rollback report, and this checkpoint record.

```bash
scripts/run-java21 mvn \
  -Dtest=org.openelisglobal.analyzer.migration.AnalyzerMigrationCharacterizationTest,org.openelisglobal.analyzer.contract.AnalyzerBridgeContractConsumerTest,org.openelisglobal.testsupport.dbunit.PostgresqlJsonbDataTypeFactoryTest \
  test
```

Observed: 11 tests ran with zero failures, errors, or skips.

## Refactor

The refactor removed an incidental assertion against inherited service sorting
and corrected the first ADR draft, which would have evolved the per-analyzer
map in place. The accepted decision uses a shared, revisioned site-binding
aggregate and groups legacy analyzers only by a complete identical fingerprint;
divergent snapshots become explicit forks.

The broader regression also exposed two harness/test-isolation defects:

- `ISSUE-E0-003`: red `5a129644d` proved an explicit fixture database was
  checked only after XML generation and that reset rediscovered a global
  database container. Green `66f9e963b` validates one explicit target before
  any generation/reset/load work and passes that exact target through reset.
  The two shell integration tests pass, and a real fixture load completed
  against only `ogc1054-database`.
- `ISSUE-E0-004`: the 869-test package gate exposed a behavior test that applied
  Spring Security while depending on a filter chain accidentally scanned from
  another test's nested configuration. `b72aadfb6` leaves 401/403/admin
  coverage in the dedicated security slice and makes the five controller
  behavior tests independent of cross-test configuration pollution.

## Layer validation

| Layer | Status | Evidence |
| --- | --- | --- |
| OpenELIS persistence/current-state | `RUN` | Real PostgreSQL through `AnalyzerMigrationCharacterizationTest` |
| Cross-repository contract | `RUN` | Bridge schemas/fixtures through `AnalyzerBridgeContractConsumerTest` |
| JSONB test support | `RUN` | `PostgresqlJsonbDataTypeFactoryTest` |
| Bridge producer | `RUN` | Bridge PR #45: 610 passed, 3 serial-hardware skips |
| Harness integration | `RUN` | Isolated OE/Bridge/mock; three visible connection and ASTM result cases passed |
| Fixture target safety | `RUN` | Two inversion-backed shell integration tests plus live load against named database |
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
- `ISSUE-E0-002` remains open: Compose containers, ports, volumes, and networks
  are stack-scoped, while the mock's dynamic `mock-analyzer-*` networks and
  `10.42.x.0/24` pools are global. MOCK-M4 owns tested namespace/pool inputs
  before two analyzer-enabled stacks may run concurrently.
- `ISSUE-E0-003` and `ISSUE-E0-004` are resolved by their committed red/green
  evidence and the passing broader package suite.
- OE-R0 still requires review and has an unrelated E2E failure; OE-F0 and BR-E0
  are green/mergeable. OE-E0 cannot become accepted out of global order.

## Isolated runtime record

- Compose project: `ogc1054`
- OpenELIS runtime branch state: contract/migration through `b81ab63cf`; the
  reusable isolation layer is committed at `901d4dfb9`
- Bridge SHA: `5ce6d3842e908cb442ce616c06346526a343af80`
- Analyzer mock SHA: `d063356e5a8f82ca6a44cf809be1874a7d704f8e`
- UI: `https://localhost:29443`
- Bridge: `https://localhost:28452`
- Mock API: `http://localhost:28085`
- Database: `localhost:25432`, selected explicitly as
  `DB_CONTAINER=ogc1054-database`
- Health at execution: OpenELIS, PostgreSQL, FHIR, and analyzer mock healthy;
  Bridge, frontend, proxy, and virtual serial services running

```bash
cd frontend
BASE_URL=https://localhost:29443 \
MOCK_SIMULATOR_URL=http://localhost:28085 \
  npm run pw:test -- --project=harness-foundational --workers=1 \
  analyzer-test-connection-foundational.spec.ts \
  analyzer-astm-results.spec.ts
```

Observed: three tests passed in 15 seconds. The tests used the visible OpenELIS
workflow for accepting the ASTM result and testing the analyzer connection;
simulator traffic remains foundational harness setup, not final demo evidence.
The successful run produced no automatic screenshot because the foundational
project captures screenshots on failure. Earlier desktop screenshots were
manually inspected and exposed the contradictory mapping counters recorded as
`ISSUE-E0-001`.

After committing the explicit fixture-target contract, the named harness
fixtures were reloaded and the same three cases passed again in 14.4 seconds.

Reviewed browser output still contains Vite development HMR WebSocket failures,
one missing-resource 404, Carbon `aria-label` deprecation warnings, and one
navigation-aborted Analyzer Results POST. These did not fail the focused flow
and are not accepted as clean final evidence; M4/G0 must rerun on the release
build and require a clean or explicitly dispositioned console/trace review.

## Final validation

```bash
scripts/run-java21 mvn \
  '-Dtest=org.openelisglobal.analyzer.**,org.openelisglobal.analyzerimport.**,org.openelisglobal.qc.**' \
  test
```

Observed: 869 tests ran with zero failures, errors, or skips. The first run found
five shared-context errors in `AnalyzerPluginConfigRestControllerTest`; after
`b72aadfb6`, the same command passed. Spotless, frontend Prettier, frontend lint,
Playwright bucket/dependency guards, shell syntax, Java 21 selection, and
resolved Compose validation also pass.

## Code-QA disposition

| Gate | Finding | Disposition |
| --- | --- | --- |
| Meaningful coverage | Fixture/reset scripts could target a different stack despite an explicit name | Red `5a129644d`, green `66f9e963b`, plus a live named-database load prove the inversion |
| Meaningful coverage | A behavior test borrowed another test's security filter | `b72aadfb6` separates five behavior assertions from the existing three-test security slice; all 14 migration/behavior/security tests pass together |
| Spec/code alignment | E0 had to fix ownership without copying implementation directions from product mocks | ADR-001 and the executable Bridge consumer contract derive from current OE/Bridge code and schemas; `openelis-work` remains functional/visual only |
| Simplicity/legacy | E0 could have introduced target tables, dual writes, or an OE file watcher | E0 adds no product persistence/runtime path; it records one-writer cutovers and keeps Bridge as FILE/runtime owner |
| Cross-repository | OE consumer claims could drift from Bridge | Bridge PR #45 is pinned at `5ce6d38`; schemas and canonical fixtures execute in OE tests |
| Evidence | A passing Compose startup could be mistaken for product acceptance | Evidence records exact SHAs, ports, health, focused flows, inspected screenshots, console debt, and explicitly leaves UI/UAT/video to M4/G0 |

**Verdict:** lean for an engineering-boundary checkpoint. The custom JSONB
DBUnit type, schema validator dependency, Java wrapper, and isolation override
each have a demonstrated caller/failure and remove no product-facing work from
later milestones. E0 is implementation-complete but remains `IN_PROGRESS`
pending PR CI/review and predecessor acceptance.

## Final gate

Local publication gates are complete. After push, CI and review threads
determine the final transition; passing local tests do not mark E0 accepted,
and the fixed global PR order still blocks acceptance before R0, F0, and BR-E0.
