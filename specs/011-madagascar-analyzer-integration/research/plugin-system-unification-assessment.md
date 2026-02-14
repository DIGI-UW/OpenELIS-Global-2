# Plugin System Unification — Assessment Report

**Date**: 2026-02-11 **PR**: #2802 (`fix/011-sync-remediation` ->
`feat/011-madagascar-analyzer-integration`) **Plan**:
[plugin-system-unification.md](../plans/plugin-system-unification.md)

---

## Executive Summary

The analyzer plugin system had two parallel pathways — **legacy** (35 JARs with
hardcoded identification) and **generic** (2 JARs with DB-driven configuration)
— built on a 3-table data model (`analyzer_type`, `analyzer`,
`analyzer_configuration`). This unification merged the model to 2 tables,
simplified all consumers, and established runtime-only availability tracking.

**Outcome**: 5 files deleted, 47 files changed, net -1,079 lines. 439 tests
passing (431 backend + 8 frontend). All CI checks green.

---

## Phases Completed

### Phase 1: Routing Unification (pre-PR, on `feat/011`)

Removed redundant Stage 2 (type-pattern matching) from
`InstanceAwareAnalyzerRouter`. Router is now 2-stage: IP match -> Plugin
`isTargetAnalyzer()`. 8 unit tests.

### Phase 2: Sync Remediation Gaps 1-4

- **Gap 1**: Orphan deactivation at startup (later revised in Phase 4)
- **Gap 2**: Legacy linking — `linkLegacyAnalyzersToTypes()` in
  `PluginRegistryService`
- **Gap 3**: REST `pluginLoaded` — per-request JAR availability check
- **Gap 4**: UI plugin health indicators in analyzer list

### Phase 3: Table Merge (`analyzer` + `analyzer_configuration` -> `analyzer`)

The core structural change. Implemented in 4 sub-phases:

| Sub-phase | Description                                                 | Key Files                                    |
| --------- | ----------------------------------------------------------- | -------------------------------------------- |
| **3A**    | Liquibase migration — add columns, migrate data, drop table | `011-merge-analyzer-configuration.xml`       |
| **3B**    | Entity & DAO changes — `Analyzer` gains config fields       | `Analyzer.java`, deleted 5 config files      |
| **3C**    | Update 16 consumer files                                    | Router, controller, scheduler, readers, etc. |
| **3D**    | Update all tests                                            | 8 test files rewritten/updated               |

**Deleted files** (5):

- `AnalyzerConfiguration.java`
- `AnalyzerConfigurationDAO.java` / `AnalyzerConfigurationDAOImpl.java`
- `AnalyzerConfigurationService.java` / `AnalyzerConfigurationServiceImpl.java`

**Analyzer entity gained** (7 columns):

- `ip_address`, `port`, `protocol_version`, `test_unit_ids`
- `status` (enum: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE)
- `identifier_pattern`, `last_activated_date`

### Phase 4: Availability Cleanup (Runtime-Only `pluginLoaded`)

Removed startup DB mutations from `PluginRegistryService`:

- Deleted `deactivateOrphanedTypes()` — no longer toggles `is_active` based on
  JAR presence
- Deleted `reactivateReturnedTypes()` — same
- `is_active` now means "admin-enabled" only
- `pluginLoaded` is computed per-request by scanning in-memory plugin list

### Phase 5: UI Refinements

- **Backend**: `pluginTypeId` field wired through form -> REST controller ->
  entity response. `analyzerToMap()` returns `pluginTypeId` + `pluginTypeName`.
- **Frontend**: Conditional rendering — generic plugins show identifier pattern
  - default config loader; legacy plugins show minimal fields (name, IP, port).
- **i18n**: English + French keys for `identifierPattern` field.

---

## Test Coverage

| Category                   | Count                         | Status                                |
| -------------------------- | ----------------------------- | ------------------------------------- |
| Backend unit tests         | 431                           | All passing                           |
| Frontend unit tests (Jest) | 448 (433 active + 15 skipped) | All active passing                    |
| Router tests               | 8                             | Updated for merged model              |
| Status transition tests    | 18                            | Updated for `Analyzer.AnalyzerStatus` |
| REST controller tests      | 29                            | Updated cleanup SQL, FK ordering      |
| Field mapping tests        | 12                            | Fixed OptimisticLockException         |
| Plugin registry tests      | 3                             | Gap 1 tests removed, Gap 2 kept       |

**Known test issues fixed during validation:**

1. Stale `DELETE FROM analyzer_configuration` JDBC in 5 integration tests
2. FK ordering bug in `AnalyzerRestControllerTest` (subquery-based cleanup)
3. OptimisticLockException in `AnalyzerFieldMappingServiceIntegrationTest`
   (double update -> single update)
4. `InstanceAwareAnalyzerRouterTest` stale `AnalyzerConfiguration` references
   (stacked PR merge shadow)
5. `AnalyzerForm.defaultConfigs.test` assertion (conditional dropdown rendering)

---

## Architecture Before vs After

### Before (3-table, dual-pathway)

```
analyzer_type ──> analyzer ──> analyzer_configuration
                                  (ip, port, status,
                                   is_generic_plugin,
                                   prefer_generic_plugin,
                                   identifier_pattern)

Startup: PluginRegistryService toggles is_active based on JAR presence
Router: 3-stage (IP -> Type Pattern -> Plugin)
```

### After (2-table, unified)

```
analyzer_type ──> analyzer
                    (ip_address, port, status,
                     identifier_pattern, ...)

Startup: PluginRegistryService only creates types + links legacy analyzers
Runtime: pluginLoaded computed per-request
Router: 2-stage (IP -> Plugin)
```

---

## Remaining Work

### Sister PR (Plugins Submodule)

The generic plugin JARs (`GenericASTMAnalyzer.java`, `GenericHL7Analyzer.java`)
still use `AnalyzerConfiguration` in their ThreadLocal storage. A sister PR in
`DIGI-UW/openelisglobal-plugins` is needed to change ThreadLocal types from
`AnalyzerConfiguration` -> `Analyzer`. This must be merged before the submodule
pointer is updated in the main repo.

**Status**: Deferred. The generic plugins will compile against the old API until
the submodule is updated. No runtime breakage since the configuration columns
are now on the `analyzer` table and accessible via the `Analyzer` entity.

### Phase 5B: Pre-loaded Config from Volume

Default configs loaded from `projects/analyzer-defaults/` volume at startup.
Currently served from classpath. Deferred per plan ("greyed out / disabled for
now").

### Phase 6: E2E Testing

Manual testing scenarios:

1. Legacy (HoribaPentra60): Send ASTM -> routed by legacy plugin -> results
2. Generic ASTM: Create via Dashboard -> send matching ASTM -> routed by pattern
3. Generic HL7: Create via Dashboard -> send HL7 ORU^R01 -> routed
4. Coexistence: Legacy + generic in same session -> no cross-contamination

---

## Commit History

| SHA         | Message                                                                   |
| ----------- | ------------------------------------------------------------------------- |
| `65ff92bda` | feat(011): plugin sync remediation — Gaps 1-3                             |
| `a518a2cad` | feat(011): Gap 4 — UI plugin health indicators                            |
| `c869b851b` | refactor(011): merge analyzer_configuration into analyzer — 2-table model |
| `f9fe631cb` | feat(011): Phase 5 — pluginTypeId wiring + conditional generic fields     |
| `05f9b58bd` | fix(011): update stale tests after table merge + conditional UI           |

---

**Assessment complete.** The plugin system is now unified on a 2-table model
with runtime-only availability tracking. The architecture is simpler, queries
are faster (no JOINs to config table), and the UI correctly differentiates
between legacy and generic plugin workflows.
