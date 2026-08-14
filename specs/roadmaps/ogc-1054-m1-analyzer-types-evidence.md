# OGC-1054 M1 Analyzer Types Evidence

- **Recorded:** 2026-08-13
- **OpenELIS branch:** `codex/ogc-1054-m1-analyzer-types`
- **Bridge branch:** `codex/ogc-1054-m1-profile-lifecycle`
- **Bridge tested head:** `42cb4bc`
- **Checkpoint status:** `IN_PROGRESS`

This record covers the M1 transport-contract and site-binding persistence
foundation. It is not M1 acceptance, an Analyzer Types UI claim, or
deployed-UAT evidence.

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

## Remaining M1 Scope

- Implement the deterministic legacy inventory/migration and one-writer cutover
  from ADR-001.
- Compose Bridge profile metadata with OpenELIS completeness, usage, and
  attention state.
- Replace the lab-facing legacy plugin registry with the Carbon Analyzer Types
  workflow, including URL-backed filters and linkable breadcrumbs.
- Implement audited create/fork/deactivate/reactivate interactions through the
  Bridge lifecycle API.
- Validate the complete M1 behavior in the isolated OpenELIS + BR-M1 stack with
  desktop/mobile screenshot and console inspection.
