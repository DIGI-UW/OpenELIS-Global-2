# OGC-1054 M1 Analyzer Types Evidence

- **Recorded:** 2026-08-13
- **OpenELIS branch:** `codex/ogc-1054-m1-analyzer-types`
- **Bridge branch:** `codex/ogc-1054-m1-profile-lifecycle`
- **Bridge tested head:** `42cb4bc`
- **Checkpoint status:** `IN_PROGRESS`

This record covers the implemented M1 foundation and local descendant work. It
is not M1 acceptance or deployed-UAT evidence. A behavior is listed as local
implementation evidence only when the owning automated checkpoint passed; no
local evidence substitutes for the remaining isolated runtime and browser
gates.

## M1.1 Bridge Profile Catalog Consumer

| Stage | Evidence | Result |
| ----- | -------- | ------ |
| Producer red | `AnalyzerContractArtifactsTest#profileCatalogEntryFixtureConforms` | Failed because the Bridge catalog envelope schema and fixture did not exist. |
| Producer green | BR-M1 `42cb4bc` | Versioned `profile-catalog-entry` schema and canonical fixture pass the producer contract and profile-controller tests; PR #46 `Run Tests` is green. |
| Consumer red | OE-M1 `7c2dc68ab` | The pinned producer artifact and consumer tests compile-failed because no OpenELIS Bridge profile catalog client existed. |
| Consumer green | OE-M1 `2fcc3ddeb` | The shared authenticated/TLS Bridge client lists catalog entries with encoded filters and preserves profile revision, lifecycle audit, and fingerprint. Missing configuration and non-success responses fail closed. |
| Refactor | Constructor-injected `AnalyzerProfileCatalogClient` boundary | No OpenELIS filesystem fallback, profile persistence, or Bridge lifecycle duplication was introduced. |

## Validation

| Gate | Result |
| ---- | ------ |
| Bridge `AnalyzerContractArtifactsTest,PortableProfileControllerTest` | 15 passed |
| OpenELIS `BridgeAnalyzerProfileCatalogClientTest,AnalyzerBridgeContractConsumerTest` | 7 passed |
| Bridge changed-file Prettier check | Passed |
| OpenELIS `mvn spotless:check` | Passed |

## M1.2 Revisioned Site-Binding Persistence

| Stage | Evidence | Result |
| ----- | -------- | ------ |
| Liquibase red | OE-M1 `2af4226ad` | The version changelog had no shared site-binding tables, immutable revisions, source-row identity, or analyzer profile/binding reference. |
| Liquibase green | OE-M1 `1f1c5af03` | Adds the shared binding, immutable revision, independently keyed source rows, referential/check constraints, analyzer reference tuple, and explicit rollback without deleting legacy evidence. |
| ORM red | OE-M1 `bbbb6afc8` | The ORM validation compile-failed because the accepted site-binding aggregate did not exist. |
| ORM green | OE-M1 `1d30d5e48` | Maps the aggregate and analyzer reference; the database-free SessionFactory validation passes. |
| Integration red | PR #4056 backend CI and focused Spring context | The catalog client required an unpublished `ObjectMapper` bean; after that was isolated, the explicit persistence units lacked the new entities. |
| Integration green | OE-M1 `6a0880ffe` and `5bee5c1dd` | The client follows the repository's context-safe mapper pattern, both persistence units register the aggregate, one real Spring context loads, and both Liquibase changesets execute against PostgreSQL. |
| Audit red | OE-M1 `93489b296` | The accepted audit contract failed because `analyzer_site_binding_revision` was absent from the existing `reference_tables` registry. |
| Audit green | OE-M1 `99d5b0ea9` | Registers immutable site-binding revisions for durable insert history with an idempotent changeset and explicit rollback. |
| Service red | OE-M1 `10d5bdc37` | Service tests compile-failed because no immutable revision service, entity-specific DAOs, draft, snapshot, or deterministic fingerprint implementation existed. |
| Service green | OE-M1 `6e8f30b11` | Adds transactional create/revise behavior, aggregate locking, source-row preservation, a versioned canonical SHA-256 fingerprint, validation-before-write, and one durable audit event per immutable revision. |
| Service refactor | OE-M1 `6e8f30b11` | Canonical inputs are normalized and sorted before hashing; the pinned digest is independently reproducible and ignores input/alias order while detecting target changes. |

The target service does not write or mutate `analyzer_test_map` or copied plugin
JSON. Writer cutover, legacy inventory, deterministic anomaly reporting, and
application composition remain unimplemented and therefore cannot be claimed
from this persistence and service foundation.

## Current Validation

| Gate | Result |
| ---- | ------ |
| Site-binding service + ORM + Liquibase + catalog/consumer + real Spring/PostgreSQL regression | 27 passed |
| OpenELIS `mvn spotless:check` | Passed at OE-M1 `6e8f30b11` |

## M1.3 Composed Catalog, Lifecycle, and Lab-Facing UI

| Stage | Evidence | Result |
| ----- | -------- | ------ |
| Composition red/green | OE-M1 `070976b80` / `7b622e625` | Composes Bridge profile metadata with OpenELIS binding completeness, usage, and attention state without copying the portable profile into OpenELIS. |
| Public API red/green | OE-M1 `71304b35b` / `581f3f848` | Adds list/detail composition at the lab-facing `/rest/analyzer/types` boundary. |
| Lifecycle client/service/controller red/green | OE-M1 `6a62afe2d` through `b1d27c582` | Proxies history, export, fork, deactivate, and reactivate through the Bridge-owned lifecycle and preserves actor/revision/lineage. No delete route exists. |
| Carbon list/detail | OE-M1 `fbfb7d6ec` | Replaces the developer registry surface with Carbon list/detail pages, semantic headings, linkable breadcrumbs, URL-backed list filters and detail tabs/actions, completeness, usage, attention, lineage, and audit history. |

### UI and API Validation

| Gate | Result |
| ---- | ------ |
| Focused catalog/lifecycle backend tests | 15 passed |
| Focused list/detail Vitest/RTL tests | 9 passed |
| Targeted Prettier and OpenELIS Spotless checks | Passed |

This UI has not yet passed the M1 browser gate. The current RTL tests mock
router hooks and therefore do not prove reload/back/forward restoration. The
fork modal also exposes a technical `Profile ID`, contrary to the current
functional requirement for a suggested unique lab-facing fork name. Those are
open M1 defects, not accepted behavior.

## M1.4 Durable Migration-Anomaly Foundation

| Stage | Evidence | Result |
| ----- | -------- | ------ |
| Schema/ORM red | `AnalyzerProfileMigrationLiquibaseTest` plus the existing analyzer-wide `HibernateMappingValidationTest` | The anomaly entity/changelog did not exist, and the repository-wide ORM validator failed on the new site-binding association even though the narrower new ORM test passed. |
| Schema/ORM green | Current migration checkpoint | Adds one durable anomaly table keyed by analyzer plus deterministic evidence key, audit registration, explicit rollback that preserves legacy evidence, entity registration in both persistence units, and registration in the existing analyzer-wide ORM gate. |
| Focused validation | Java 21 Maven run | 7 tests passed: analyzer-wide ORM, focused site-binding ORM, and migration Liquibase contracts. |

This checkpoint persists the evidence shape only. It does not inventory legacy
rows, choose a profile, create/reuse a complete site binding, switch an analyzer
reference, or reject a legacy write. Those behaviors remain red-green-refactor
work in the migration service checkpoint.

## Remaining M1 Scope

- Implement the deterministic legacy inventory/migration and one-writer cutover
  from ADR-001.
- Move deterministic fork identity generation to BR-M1 and remove the technical
  profile identifier from the OpenELIS lab-facing modal.
- Align and test the Analyzer Type frontend/backend permission contract.
- Replace mocked router hooks with real-router coverage for canonical URL,
  reload, back, and forward behavior.
- Reject legacy mapping writes after successful per-analyzer cutover without
  removing read-only migration evidence.
- Validate the complete M1 behavior in the isolated OpenELIS + BR-M1 stack with
  a focused UI browser flow, desktop/mobile screenshots, and console
  inspection.
