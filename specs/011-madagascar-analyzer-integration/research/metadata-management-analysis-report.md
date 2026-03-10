# Metadata Management & Liquibase Analysis Report

## OpenELIS-Global-2 — Summary

> **Purpose**: Architectural analysis of metadata management in OpenELIS —
> Liquibase role, existing mechanisms, gaps, and recommended tier model.
>
> **Date**: January 29, 2026

---

## 1. Executive Summary

OpenELIS-Global-2 uses Liquibase for **both** schema management **and**
reference data seeding. The codebase has **~170 changelog XML files** containing
an estimated **1,200–1,400 changesets**, with roughly **41% being pure
data/metadata inserts** rather than schema changes.

The project already has the seeds of a better approach:

- A **plugin system** that self-registers analyzer metadata at startup
- A **`ConfigurationInitializationService`** that loads domain data from
  CSV/JSON files (8 handlers for tests, samples, dictionaries, roles, OCL,
  questionnaires)
- An **admin UI** for runtime configuration of analyzers, field mappings, and
  test connections

**Core problem**: These mechanisms coexist without clear ownership boundaries.
Legacy reference data still lives in Liquibase alongside schema DDL. The
`ConfigurationInitializationService` infrastructure is fully built but **no
default configuration files exist on the classpath** — fresh installs rely
entirely on Liquibase for reference data.

> **Key Finding**: The project is already transitioning toward the right
> architecture. What's needed is a clear policy codifying which data belongs
> where, and a gradual migration of legacy reference data out of Liquibase.

---

## 2. Current Liquibase Anti-Patterns

1. **Massive single files**: `new_tests.xml` has 236 changesets (2,173 lines);
   `add_tb_tests.xml` has 126 changesets (2,075 lines)
2. **Mixed schema + data**: Localization inserts and table creation in single
   changesets (e.g., `immunohistochemistry.xml`)
3. **Hardcoded IDs**: Test data uses `value="1"` with fixed identifiers
4. **Defensive SQL guards**: Heavy `<preConditions onFail="MARK_RAN">` — a
   workaround indicating data shouldn't be in migrations at all
5. **Dual-source DB init**: PostgreSQL dump (`OpenELIS-Global.sql`, ~864KB)
   provides baseline schema+data; Liquibase applies changes on top, risking
   conflicts

---

## 3. Application Startup Flow

```
1. DATABASE → PostgreSQL runs init scripts (baseline schema dump)
2. WEB APP → Tomcat starts → Spring Boot bootstraps
3. LIQUIBASE → Applies pending changesets (schema + legacy data)
4. HIBERNATE → Maps entities to tables (@DependsOn("liquibase"))
5. PLUGINS → PluginLoader scans /var/lib/openelis-global/plugins/
6. CONFIG INIT → ConfigurationInitializationService loads CSV/JSON
7. CACHE → AnalyzerTestNameCache lazy-loads on first access
8. APPLICATION READY
```

---

## 4. Gap Analysis

| Aspect                    | Current State                  | Best Practice              | Status |
| ------------------------- | ------------------------------ | -------------------------- | ------ |
| Schema management         | Liquibase (DDL)                | Liquibase (DDL only)       | ✅     |
| Analyzer metadata         | Plugin self-registration       | Plugin self-registration   | ✅     |
| Configuration loading     | ConfigurationInitializationSvc | File-based config loading  | ✅     |
| Admin UI management       | React + REST controllers       | Runtime admin UI           | ✅     |
| Test definitions          | Liquibase changesets (legacy)  | File-based or API-driven   | ⚠️     |
| Dictionary/lookup values  | Liquibase changesets (legacy)  | File-based or API-driven   | ⚠️     |
| Database initialization   | Dual source (dump + Liquibase) | Single source              | ⚠️     |
| Site-specific data        | Not cleanly separated          | Deployment bundles         | ⚠️     |
| E2E test fixtures         | 4 competing mechanisms         | Single composable strategy | ⚠️     |
| Classpath default configs | Empty (no files)               | Bundled defaults           | ⚠️     |
| Handler coverage          | 8 handlers                     | Handler for every domain   | ⚠️     |

Missing handlers: UoM, panels, reference ranges, analyzer field mappings.

---

## 5. Proposed Architecture: 6-Tier Metadata Ownership

```
┌──────────────────────────────────────────────────────────┐
│                   DEPLOYMENT TIME                         │
│                                                          │
│  TIER 1: Liquibase — Schema Only (DDL)                   │
│  CREATE TABLE, ALTER TABLE, CREATE INDEX, sequences       │
│                                                          │
│  TIER 2: Liquibase — Structural Data                     │
│  site_information keys, role definitions, menu items      │
│  (data the code references by constant/ID)               │
├──────────────────────────────────────────────────────────┤
│                   APPLICATION STARTUP                      │
│                                                          │
│  TIER 3: ConfigurationInitializationService              │
│  Test definitions, sample types, dictionaries (CSV/JSON)  │
│  Source: classpath + /var/lib/openelis-global/config/     │
│                                                          │
│  TIER 4: Plugin Self-Registration                        │
│  Analyzer definitions, test mappings, protocol configs    │
│  Source: /var/lib/openelis-global/plugins/                │
├──────────────────────────────────────────────────────────┤
│                   RUNTIME                                  │
│                                                          │
│  TIER 5: Admin UI + REST API                             │
│  IP/port config, field mappings, enable/disable           │
│                                                          │
│  TIER 6: Site-Specific Deployment Bundles                │
│  Madagascar-specific tests, country-specific dicts        │
│  Source: /var/lib/openelis-global/configuration/          │
└──────────────────────────────────────────────────────────┘
```

---

## 6. Recommendations

### Short-Term (No Architecture Change)

1. **Document the policy**: Add `METADATA_MANAGEMENT.md` explaining which tier
   owns which data
2. **Stop adding reference data to Liquibase**: New features use
   ConfigurationInitializationService or plugin registration
3. **Fix Docker startup race**: Use `depends_on: condition: service_healthy`
4. **Consolidate test fixture strategy**: Standardize on idempotent SQL
   (`ON CONFLICT DO NOTHING`), deprecate DBUnit XML → Python → SQL pipeline

### Medium-Term (Incremental)

5. **Create deployment bundles**: Site-specific CSV/JSON in
   `/var/lib/openelis-global/configuration/`
6. **Migrate largest data changesets**: Move `new_tests.xml` (236 changesets),
   `add_tb_tests.xml` (126), `immunohistochemistry.xml` into CSV/JSON files
7. **Eliminate dual-source DB init**: Formalize SQL dump as baseline + Liquibase
   for changes only
8. **Write missing handlers**: UoM (order 100), Panels (220), ReferenceRanges
   (220)
9. **Populate classpath defaults**: `src/main/resources/configuration/` for
   fresh installs

### Long-Term

10. **FHIR Terminology Service**: Manage test defs, code systems, concept maps
    as FHIR resources
11. **Metadata Package Import/Export**: Like OpenMRS Metadata Sharing Module
12. **Reference Data Versioning**: Audit trail + version numbers for all
    reference data changes

---

## 7. Key Files

| File                                                            | Role                          |
| --------------------------------------------------------------- | ----------------------------- |
| `liquibase/LiquibaseConfig.java`                                | Spring Liquibase config       |
| `liquibase/base-changelog.xml`                                  | Master changelog              |
| `common/services/PluginAnalyzerService.java`                    | Plugin metadata registration  |
| `configuration/service/ConfigurationInitializationService.java` | Config file loading           |
| `configuration/service/DomainConfigurationHandler.java`         | Domain handler interface      |
| `analyzerimport/util/AnalyzerTestNameCache.java`                | In-memory test name cache     |
| `db/dbInit/OpenELIS-Global.sql`                                 | Baseline schema dump (~864KB) |

---

## 8. Comparable Projects

- **OpenMRS**: Uses Liquibase for schema only; metadata managed via Initializer
  Module (CSV/JSON loading — directly comparable to
  ConfigurationInitializationService) and Metadata Sharing Module (import/export
  packages)
- **FHIR R4**: Provides CodeSystem, ValueSet, ConceptMap resources for reference
  data — aligns with OE's constitutional FHIR mandate
