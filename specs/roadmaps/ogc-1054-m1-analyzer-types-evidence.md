# OGC-1054 M1 Analyzer Types Evidence

- **Recorded:** 2026-08-13
- **OpenELIS branch:** `codex/ogc-1054-m1-analyzer-types`
- **Bridge branch:** `codex/ogc-1054-m1-profile-lifecycle`
- **Bridge tested head:** `42cb4bc`
- **Checkpoint status:** `IN_PROGRESS`

This record covers the first M1 transport-contract slice. It is not M1
acceptance, an Analyzer Types UI claim, or deployed-UAT evidence.

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

## Remaining M1 Scope

- Implement the shared, revisioned OpenELIS site-binding aggregate and
  deterministic legacy migration from ADR-001.
- Compose Bridge profile metadata with OpenELIS completeness, usage, and
  attention state.
- Replace the lab-facing legacy plugin registry with the Carbon Analyzer Types
  workflow, including URL-backed filters and linkable breadcrumbs.
- Implement audited create/fork/deactivate/reactivate interactions through the
  Bridge lifecycle API.
- Validate the complete M1 behavior in the isolated OpenELIS + BR-M1 stack with
  desktop/mobile screenshot and console inspection.
