# Metadata Management & Liquibase Analysis Report

## OpenELIS-Global-2 — Deep Dive Analysis

> **Purpose**: Foundation document for future architectural discussions about
> how metadata is managed in OpenELIS, the role of Liquibase, Docker
> complications, and recommended best practices.
>
> **Date**: January 29, 2026 **Context**: Generated from comprehensive codebase
> analysis and industry research

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State: Liquibase Usage in OE](#2-current-state-liquibase-usage-in-oe)
3. [Current State: Metadata Management Beyond Liquibase](#3-current-state-metadata-management-beyond-liquibase)
4. [Docker Complications](#4-docker-complications)
5. [Test Data Management: The Full Picture](#5-test-data-management-the-full-picture)
6. [Industry Best Practices & Research](#6-industry-best-practices--research)
7. [Comparable Projects: OpenMRS & FHIR](#7-comparable-projects-openmrs--fhir)
8. [Gap Analysis: Where OE Stands vs. Best Practice](#8-gap-analysis-where-oe-stands-vs-best-practice)
9. [Recommendations](#9-recommendations)
10. [Proposed Architecture: Metadata Management Tiers](#10-proposed-architecture-metadata-management-tiers)
11. [Migration Path](#11-migration-path)
12. [Key References](#12-key-references)

---

## 1. Executive Summary

OpenELIS-Global-2 uses Liquibase for **both** schema management **and**
reference data seeding — a common pattern that creates increasing friction as
the project grows. The codebase has **170 changelog XML files** containing an
estimated **1,200–1,400 changesets**, with roughly **41% being pure
data/metadata inserts** rather than schema changes.

The project already has the seeds of a better approach:

- A **plugin system** that self-registers analyzer metadata at startup
- A **`ConfigurationInitializationService`** that loads domain data from
  CSV/JSON files
- An **admin UI** for runtime configuration of analyzers, field mappings, and
  test connections
- **Liquibase contexts** (`test`, `default`) for environment separation

The core problem: **these mechanisms coexist without a clear delineation of
responsibility**, and legacy reference data still lives in Liquibase changesets
alongside schema DDL. Docker containerization amplifies these issues by adding
startup ordering, volume persistence, and context management complexity.

**Test data management compounds the problem further.** E2E tests require
specific fixtures (providers, organizations, storage hierarchies, patients,
samples), and these are loaded through **four competing mechanisms**: DBUnit XML
converted to SQL via Python, direct SQL scripts, Liquibase `context="test"`
changesets (currently commented out), and Cypress JSON fixtures. The implicit
dependency chain between these is undocumented, fixture loading is
non-idempotent, and hardcoded ID ranges create invisible coupling across layers.

### Key Finding

> The project is already transitioning toward the right architecture. The
> `ConfigurationInitializationService` + plugin registration pattern is the
> correct direction. What's needed is a clear policy codifying which data
> belongs where, and a gradual migration of legacy reference data out of
> Liquibase.

---

## 2. Current State: Liquibase Usage in OE

### 2.1 Configuration

Liquibase is configured as a **Spring bean** that runs automatically on
application startup:

**File**: `src/main/java/org/openelisglobal/liquibase/LiquibaseConfig.java`

```java
@Configuration
public class LiquibaseConfig {
    @Bean("liquibase")
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:liquibase/base-changelog.xml");
        liquibase.setDataSource(dataSource);
        liquibase.setContexts(contexts); // ${spring.liquibase.contexts:default}
        return liquibase;
    }
}
```

Hibernate/JPA initialization depends on this bean (`@DependsOn("liquibase")`),
ensuring schema is ready before ORM mapping.

### 2.2 Changelog Organization

```
src/main/resources/liquibase/
├── base-changelog.xml          ← Master entry point
├── 2.0.x.x/base.xml           ← 6 files (initial)
├── 2.1.x.x/base.xml           ← 8 files (menu/admin)
├── 2.2.x.x/base.xml           ← 2 files (email/SMS)
├── 2.3.x.x/base.xml           ← 14 files (validation, tests, analyzer)
├── 2.4.x.x/base.xml           ← 2 files (roles)
├── 2.5.x.x/base.xml           ← 2 files (barcode)
├── 2.6.x.x/base.xml           ← 19 files (roles, methods, i18n)
├── 2.7.x.x/base.xml           ← 25 files (TB, HPV, recency)
├── 2.8.x.x/base.xml           ← 26 files (pathology, cytology, notifications)
├── 3.1.x.x/base.xml           ← 5 files (LOINC, clinical diagnosis)
├── 3.2.x.x/base.xml           ← 6 files (generic samples, notebooks)
├── 3.3.x.x/base.xml           ← 38 files (storage, patient merge, analyzer)
└── analyzer/base.xml           ← 6 files (ASTM field mapping)
                                   TOTAL: ~170 files, ~1,200–1,400 changesets
```

**Strengths**: Version-based organization makes historical navigation easy. The
`3.3.x.x/` directory uses numbered prefixes (`001-`, `002-`...) for clear
ordering. The `analyzer/` directory is a good precedent for feature-based
modules.

### 2.3 Types of Changesets (Breakdown)

| Category                | % of Files | Examples                                                                                 |
| ----------------------- | ---------- | ---------------------------------------------------------------------------------------- |
| **Pure Schema (DDL)**   | ~23%       | `001-create-storage-hierarchy-tables.xml`, `028-create-analyzer-configuration-table.xml` |
| **Mixed Schema + Data** | ~30%       | `immunohistochemistry.xml` (creates tables + inserts test definitions)                   |
| **Pure Data/Metadata**  | ~41%       | `new_tests.xml` (236 changesets!), `add_tb_tests.xml` (126 changesets)                   |
| **Configuration/Menu**  | ~6%        | `005-add-storage-menu-item.xml`, `update_roles.xml`                                      |

### 2.4 Metadata in Liquibase: Specific Examples

**Analyzer-Related Metadata** (in `3.3.x.x/028-038`):

- `analyzer_configuration` table with IP, port, protocol settings
- `analyzer_field`, `analyzer_field_mapping` tables
- `qualitative_result_mapping`, `unit_mapping` tables

**Test/Panel Definitions** (scattered across versions):

- COVID-19 PCR test in `2.3.x.x/new_tests.xml` (236 changesets)
- TB tests in `2.7.x.x/add_tb_tests.xml` (126 changesets)
- HPV testing in `2.7.x.x/add_hpv_testing.xml` (33 changesets)
- Immunohistochemistry in `2.8.x.x/immunohistochemistry.xml` (16 changesets,
  2,475 lines)

**Dictionary/Lookup Values**:

- TB result dictionaries ("MTB detected", "MTB resistant", etc.)
- Sample type definitions with localizations
- Result interpretation lookups

### 2.5 Anti-Patterns Identified

1. **Massive single files**: `new_tests.xml` has **236 changesets in one file**
   (2,173 lines). `add_tb_tests.xml` has 126 changesets (2,075 lines). These are
   extremely hard to review, maintain, and debug.

2. **Mixed schema + data in same changesets**: Example from
   `immunohistochemistry.xml` where localization inserts and test section
   creation happen in a single changeset, making rollback complex.

3. **Hardcoded IDs**: Test data uses `<column name="id" value="1"/>` with
   hardcoded identifiers rather than sequences.

4. **Defensive SQL checks everywhere**: Heavy use of
   `<preConditions onFail="MARK_RAN">` with raw SQL checks like:

   ```xml
   <sqlCheck expectedResult="0">
     SELECT count(*) FROM clinlims.localization
     WHERE english = 'COVID-19 PCR'
   </sqlCheck>
   ```

   This pattern, while ensuring idempotency, indicates the data _shouldn't be in
   migrations at all_ — it's acting as a workaround for the fact that this data
   might already exist from other sources.

5. **Environment-specific data not cleanly separated**: While `context="test"`
   is used for test fixtures, production reference data (test definitions,
   dictionary values) that varies by deployment site is baked into migrations.

---

## 3. Current State: Metadata Management Beyond Liquibase

### 3.1 The Good News: OE Already Has Better Mechanisms

The project has **three non-Liquibase metadata management systems** that
represent the correct architectural direction:

#### A. Plugin Self-Registration (`PluginAnalyzerService`)

**File**:
`src/main/java/org/openelisglobal/common/services/PluginAnalyzerService.java`

Analyzer plugins register their own metadata at application startup:

```java
@Service
public class PluginAnalyzerService {
    public String addAnalyzerDatabaseParts(String name, String description,
            List<TestMapping> nameMappings, boolean hasSetupPage) {
        // Idempotently creates/updates Analyzer + AnalyzerTestMapping records
    }
}
```

Plugins call this from `@PostConstruct`:

```java
// HoribaPentra60Analyzer.java
@Override
public boolean connect() {
    List<TestMapping> mappings = new ArrayList<>();
    mappings.add(new TestMapping("WBC", "White Blood Cells", "6690-2"));
    mappings.add(new TestMapping("RBC", "Red Blood Cells", "789-8"));
    // ... 24 more CBC parameters

    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(ANALYZER_NAME, DESCRIPTION, mappings, true);
    return true;
}
```

**This is the right pattern.** The plugin owns its metadata and registers it
idempotently at startup. No Liquibase changeset needed.

#### B. Configuration Initialization Service

**File**:
`src/main/java/org/openelisglobal/configuration/service/ConfigurationInitializationService.java`

Loads domain-specific reference data from CSV/JSON files at startup:

```java
@Component
public class ConfigurationInitializationService
    implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 1. Load from classpath: configuration/{domain}/*.{extension}
        // 2. Load from filesystem: /var/lib/openelis-global/configuration/backend/
        // 3. Check SHA-256 checksums to skip unchanged files
        // 4. Call domain-specific handlers in load order
    }
}
```

The service auto-discovers all Spring beans implementing the
`DomainConfigurationHandler` interface
(`src/main/java/org/openelisglobal/configuration/service/DomainConfigurationHandler.java`),
sorts them by `getLoadOrder()`, and invokes each one. The interface is minimal:

```java
public interface DomainConfigurationHandler {
    String getDomainName();           // subdirectory name, e.g. "tests"
    String getFileExtension();        // e.g. "csv", "json", "zip"
    default int getLoadOrder() { return 500; }
    void processConfiguration(InputStream inputStream, String fileName) throws Exception;
}
```

**All 8 existing handler implementations**:

| Order | Handler Class                        | Domain              | Format | What It Loads                   |
| ----- | ------------------------------------ | ------------------- | ------ | ------------------------------- |
| 100   | `TypeOfSampleConfigurationHandler`   | `sample-types`      | CSV    | Sample types + localization     |
| 100   | `TestSectionConfigurationHandler`    | `test-sections`     | CSV    | Lab departments/sections        |
| 200   | `TestConfigurationHandler`           | `tests`             | CSV    | Test definitions + LOINC codes  |
| 210   | `TestSampleTypeConfigurationHandler` | `test-sample-types` | CSV    | Test ↔ sample type mappings     |
| 300   | `RolesConfigurationHandler`          | `roles`             | CSV    | Role hierarchy                  |
| 300   | `DictionaryConfigurationHandler`     | `dictionaries`      | CSV    | Dictionary/lookup entries       |
| 400   | `OclConfigurationHandler`            | `ocl`               | ZIP    | Open Concept Lab imports        |
| 400   | `QuestionnaireConfigurationHandler`  | `questionnaires`    | JSON   | FHIR R4 Questionnaire resources |

Each handler scans two locations for files matching `{domain}/*.{extension}`:

1. **Classpath**: `classpath*:configuration/{domain}/`
2. **Filesystem**: `/var/lib/openelis-global/configuration/backend/{domain}/`

SHA-256 checksums are stored per-domain, so unchanged files are skipped on
restart (built-in idempotency).

**This is equally sound.** Deployments can customize reference data by placing
CSV/JSON files in `/var/lib/openelis-global/configuration/backend/` without
rebuilding the application.

> **Critical gap**: Despite the handler infrastructure being fully built, **no
> default configuration files currently exist on the classpath**
> (`src/main/resources/configuration/` is empty). This means fresh installations
> get all their sample types, test definitions, and dictionaries exclusively
> from Liquibase changesets — the exact anti-pattern this report identifies.
> Populating the classpath with default CSV/JSON files is a prerequisite for
> migrating data out of Liquibase.

#### C. Admin UI (React + REST)

Runtime management screens exist for:

| Feature                    | Frontend Component                              | REST Controller                      |
| -------------------------- | ----------------------------------------------- | ------------------------------------ |
| Analyzer CRUD              | `components/analyzers/`                         | `AnalyzerRestController`             |
| Field Mapping              | `components/analyzers/FieldMapping/`            | `AnalyzerFieldMappingRestController` |
| Test Connection            | `components/analyzers/TestConnectionModal/`     | (inline)                             |
| Serial Port Config         | `components/analyzers/SerialConfiguration/`     | `SerialPortRestController`           |
| File Import Config         | `components/analyzers/FileImportConfiguration/` | `FileImportRestController`           |
| Error Dashboard            | `components/analyzers/ErrorDashboard/`          | `AnalyzerErrorRestController`        |
| Analyzer Test Name Mapping | `components/admin/analyzerTestName/`            | (legacy controller)                  |

### 3.2 Caching: `AnalyzerTestNameCache`

**File**:
`src/main/java/org/openelisglobal/analyzerimport/util/AnalyzerTestNameCache.java`

An in-memory singleton cache that maps analyzer test names to OpenELIS test IDs:

```
Cache Structure:
  "Horiba Pentra 60" → {
    "WBC" → MappedTestName(testId="123", name="White Blood Cells"),
    "RBC" → MappedTestName(testId="124", name="Red Blood Cells"),
    ...
  }
```

This enables O(1) lookup during result import. The cache lazy-loads on first
access and can be invalidated via `reloadCache()`.

### 3.3 Application Startup Flow (Complete Picture)

```
1. DATABASE CONTAINER STARTS
   └─ PostgreSQL runs init scripts (user creation + baseline schema dump)

2. WEB APP CONTAINER STARTS
   └─ Tomcat starts → Spring Boot bootstraps

3. LIQUIBASE RUNS (Spring bean, before Hibernate)
   └─ Applies pending changesets from base-changelog.xml
   └─ Creates/modifies schema tables

4. HIBERNATE INITIALIZES (@DependsOn("liquibase"))
   └─ Maps entities to tables

5. PLUGIN LOADING (PluginLoader @PostConstruct)
   └─ Scans /var/lib/openelis-global/plugins/ for JARs
   └─ Each plugin's connect() method called
   └─ Plugins register with PluginAnalyzerService

6. CONFIGURATION INITIALIZATION (ContextRefreshedEvent)
   └─ ConfigurationInitializationService runs
   └─ Loads CSV/JSON files from classpath + filesystem
   └─ Handlers process in dependency order (100 → 200 → 300 → 500+)

7. CACHE POPULATION (lazy, on first access)
   └─ AnalyzerTestNameCache loads all analyzer/test mappings

8. APPLICATION READY
```

---

## 4. Docker Complications

### 4.1 Docker Architecture

```
docker-compose.yml
├── db.openelis.org          (PostgreSQL 14.4, port 15432)
├── oe.openelis.org          (Tomcat + Spring Boot, port 8080/8443)
├── fhir.openelis.org        (HAPI FHIR, port 8081)
├── frontend.openelis.org    (React app)
├── proxy                    (Nginx reverse proxy, port 80/443)
└── certs                    (SSL certificate generator)
```

### 4.2 Database Initialization: The Dual-Source Problem

**This is the core Docker/Liquibase complication.**

The database is initialized from **two sources**:

1. **PostgreSQL entrypoint scripts** (`db/dbInit/`):

   - `init.sh` → Creates users and roles
   - `OpenELIS-Global.sql` (~864KB) → Baseline schema dump with reference data
   - `siteInfo.sql` → Site-specific configuration

2. **Liquibase changesets** (run by Spring Boot on app startup):
   - Applies all changesets from `base-changelog.xml`
   - Adds/modifies schema on top of the baseline dump
   - Inserts additional reference data

**The problem**: The PostgreSQL dump (`OpenELIS-Global.sql`) contains old schema
and data. Liquibase changesets then modify/extend this. If they diverge — if the
dump contains data that Liquibase also tries to insert — you get conflicts. The
`<preConditions onFail="MARK_RAN">` guards are a workaround for this exact
issue.

### 4.3 Context Management Across Environments

| Environment | `SPRING_LIQUIBASE_CONTEXTS` | Effect                                  |
| ----------- | --------------------------- | --------------------------------------- |
| Production  | `default` (implicit)        | Only non-context and default changesets |
| Development | `test`                      | Includes test fixtures + default        |
| Build/CI    | `test`                      | Same as development                     |
| Unit Tests  | `test` (via BaseTestConfig) | Embedded PostgreSQL + test changesets   |

**Known Issue**: If CI forgets to set `SPRING_LIQUIBASE_CONTEXTS=test`, test
data (e.g., storage rooms) isn't loaded, and E2E tests fail. A
`scripts/simulate-ci-failure.sh` script exists to reproduce this locally.

### 4.4 Startup Race Conditions

```yaml
# docker-compose.yml
oe.openelis.org:
  depends_on:
    - db.openelis.org # Does NOT wait for healthcheck in Compose v3.3
```

Docker Compose v3.3 `depends_on` only ensures the container **starts**, not that
the database is **ready**. The database has a healthcheck (`pg_isready`), but
the app doesn't wait for it. If Liquibase connects before PostgreSQL is fully
initialized, it fails.

**Current mitigation**: The database healthcheck + `--wait` flag in CI
(`docker compose up --wait`). In development, manual restart if timing is bad.

### 4.5 Volume Persistence Behavior

| Scenario                 | Database Volume (`db-data`) | Effect                                                     |
| ------------------------ | --------------------------- | ---------------------------------------------------------- |
| First start              | Empty                       | PostgreSQL runs init scripts + Liquibase                   |
| Container restart        | Preserved                   | PostgreSQL skips init; Liquibase checks for new changesets |
| `docker compose down -v` | Deleted                     | Full re-initialization on next start                       |
| Failed migration         | Partially applied           | Database in inconsistent state; may need manual fix        |

---

## 5. Test Data Management: The Full Picture

Test data management is where Liquibase, Docker, and metadata management issues
converge most painfully. E2E tests require specific database state to exist, and
the current approach to achieving that state involves **four competing
mechanisms** with no clear policy on when to use which.

### 5.1 Current Test Data Loading Mechanisms

OE currently has **four different approaches** to test data management:

| #   | Mechanism                      | Files                                        | Used By                     | Idempotent? |
| --- | ------------------------------ | -------------------------------------------- | --------------------------- | ----------- |
| 1   | **DBUnit XML → Python → SQL**  | `testdata/storage-e2e.xml` + `xml-to-sql.py` | E2E (Cypress)               | ❌ No       |
| 2   | **Direct SQL scripts**         | `e2e-foundational-data.sql`                  | E2E (Cypress)               | ✅ Yes      |
| 3   | **Liquibase `context="test"`** | `004-insert-test-storage-data.xml`           | Integration (commented out) | ✅ Tracked  |
| 4   | **Cypress JSON fixtures**      | `frontend/cypress/fixtures/*.json`           | E2E (UI stubs)              | ✅ Yes      |

Plus supporting infrastructure:

| Tool                  | File                                                    | Purpose                               |
| --------------------- | ------------------------------------------------------- | ------------------------------------- |
| Master fixture loader | `src/test/resources/load-test-fixtures.sh` (409 lines)  | Orchestrates all fixture loading      |
| Database reset script | `src/test/resources/reset-test-database.sh` (174 lines) | Cleans test data ranges only          |
| DBUnit Java loader    | `testutils/DbUnitDatasetLoader.java`                    | Standalone DBUnit execution via Maven |
| Cypress tasks         | `cypress.config.js` (task handlers)                     | Node-side DB operations for Cypress   |

### 5.2 The E2E Fixture Loading Flow

When a Cypress E2E test needs fixtures, this is the actual execution path:

```
Cypress test starts
  └─ before() hook calls cy.setupStorageTests()
       └─ cy.waitForBackend("/rest/storage/samples")  // Wait for app
       └─ LoginPage → login as admin
       └─ Check env vars: SKIP_FIXTURES? FORCE_FIXTURES?
            └─ cy.task("checkStorageFixturesExist")
                 └─ SQL: SELECT COUNT(*) FROM storage_room WHERE code IN (...)
                 └─ SQL: SELECT COUNT(*) FROM storage_device WHERE id BETWEEN ...
            └─ If missing: cy.task("loadStorageTestData")
                 └─ Executes: load-test-fixtures.sh
                      └─ Check Python 3 available
                      └─ Check e2e-foundational-data.sql exists
                      └─ Run xml-to-sql.py to convert DBUnit XML → SQL
                      └─ Wait for Liquibase (retry loop, 10 attempts):
                           └─ SELECT COUNT(*) FROM type_of_sample ≥ 3?
                           └─ SELECT COUNT(*) FROM status_of_sample WHERE name='Entered'?
                           └─ SELECT COUNT(*) FROM storage_room WHERE code IN (...) ≥ 3?
                      └─ Optional: reset-test-database.sh (clean previous data)
                      └─ docker exec psql < e2e-foundational-data.sql
                      └─ docker exec psql < storage-e2e.generated.sql
                      └─ verify_fixtures() - check expected counts
```

**That is 12+ steps spanning 3 languages (Bash, Python, SQL), 2 runtimes
(Node.js, Docker), and ~600 lines of orchestration code** to load test fixtures.
This complexity is a direct consequence of not having a unified test data
strategy.

### 5.3 The Implicit Dependency Graph

No single document defines this, but through analysis the fixture dependency
chain is:

```
Level 0: PostgreSQL schema (Liquibase DDL changesets)
   │
Level 1: Liquibase reference data (type_of_sample, status_of_sample, etc.)
   │      ↑ Must exist before any E2E fixtures load
   │      ↑ Created by Liquibase during Spring startup
   │
Level 2: E2E foundational data (providers, organizations)
   │      File: e2e-foundational-data.sql
   │      Creates: "Optimus Prime" provider (ID: 9000001)
   │               "CAMES MAN" organization (ID: 9000100)
   │      ↑ Required by: Order.json, many Cypress tests
   │
Level 3: Storage hierarchy (rooms → devices → shelves → racks → boxes)
   │      File: storage-e2e.xml → storage-e2e.generated.sql
   │      Creates: 3 rooms, 4 devices, 4+ shelves, 10+ racks, boxes
   │      ↑ Required by: Storage CRUD tests, storage assignment tests
   │
Level 4: E2E test data (patients, samples, analyses, results)
   │      File: storage-e2e.xml (also contains this)
   │      Creates: E2E-* patients, DEV0100* samples, analyses, results
   │      ↑ Required by: Storage assignment tests, result validation tests
   │
Level 5: Cypress JSON fixtures (UI-level data)
          Files: Order.json, Patient.json, Storage.json, etc.
          Purpose: Drive UI interactions (form fills, search terms)
          ↑ References entities from Level 2-4 by name/code
```

**Critical insight**: If Level 1 (Liquibase reference data) doesn't load because
`SPRING_LIQUIBASE_CONTEXTS` is wrong, the `load-test-fixtures.sh` retry loop
will time out, and ALL E2E tests fail with no clear error message.

### 5.4 Identified Problems

#### Problem 1: Four Competing Approaches, No Policy

A developer adding a new E2E test has no guidance on which mechanism to use:

- Should they add a Liquibase changeset with `context="test"`?
- Should they write a SQL file loaded by `load-test-fixtures.sh`?
- Should they create a DBUnit XML dataset?
- Should they add data via `cy.request()` API calls?

All four patterns exist in the codebase. The storage feature uses DBUnit XML →
Python → SQL. The patient merge feature uses direct SQL. The Liquibase approach
exists but is commented out.

#### Problem 2: Non-Idempotent Fixture Loading

The `xml-to-sql.py` converter generates plain `INSERT INTO` statements with no
conflict handling:

```python
# xml-to-sql.py, line 71-75
insert_stmt = "INSERT INTO {} ({}) VALUES ({});".format(
    table_name, ", ".join(columns), ", ".join(values)
)
```

If fixtures already exist (e.g., from a previous failed run),
`load-test-fixtures.sh` **fails with duplicate key errors** unless
`reset-test-database.sh` runs first. Compare this to the foundational SQL which
uses `WHERE NOT EXISTS`:

```sql
-- e2e-foundational-data.sql (idempotent)
INSERT INTO clinlims.person (id, ...) SELECT 9000001, ...
WHERE NOT EXISTS (SELECT 1 FROM clinlims.person WHERE id = 9000001);
```

#### Problem 3: Hardcoded ID Ranges Across Layers

Test data uses fixed ID ranges that create invisible coupling:

| Entity          | ID Range    | Defined In                  | Referenced By            |
| --------------- | ----------- | --------------------------- | ------------------------ |
| Storage rooms   | 1-3         | `storage-e2e.xml`           | Cypress task SQL checks  |
| Storage devices | 10-13       | `storage-e2e.xml`           | Cypress task SQL checks  |
| Storage shelves | 20-23       | `storage-e2e.xml`           | Cypress task SQL checks  |
| Storage racks   | 30-40       | `storage-e2e.xml`           | Cypress task SQL checks  |
| Storage boxes   | 100-10000   | `storage-e2e.xml`           | Cypress task SQL checks  |
| Sample items    | 10000-20000 | `storage-e2e.xml`           | `reset-test-database.sh` |
| Providers       | 9000001-2   | `e2e-foundational-data.sql` | `Order.json` (by name)   |
| Organizations   | 9000100     | `e2e-foundational-data.sql` | `Order.json` (by name)   |

If production data ever uses IDs in these ranges (especially rooms 1-3), tests
will collide.

#### Problem 4: Liquibase Test Context Is Commented Out

The Liquibase changeset file `004-insert-test-storage-data.xml` exists and
contains the same storage hierarchy data as `storage-e2e.xml`, but the include
is **commented out** in `3.3.x.x/base.xml`:

```xml
<!-- NOTE: Storage test fixtures are loaded via DBUnit datasets in tests.
     Keep Liquibase focused on schema migrations; do not seed storage hierarchy
     test data here to avoid dual sources of truth. -->
<!-- <include relativeToChangelogFile="true"
            file="004-insert-test-storage-data.xml" context="test"/> -->
```

This means:

1. Two files define the same test data (`storage-e2e.xml` + the changeset)
2. They can drift out of sync
3. The comment acknowledges the "dual sources of truth" problem but the solution
   (commenting out Liquibase) means test data now lives outside the
   migration-tracked system

#### Problem 5: Docker Container Name Coupling

Multiple scripts hardcode the Docker container name `openelisglobal-database`
(found in `cypress.config.js`, `load-test-fixtures.sh`, and
`reset-test-database.sh`):

```bash
# load-test-fixtures.sh
CONTAINER_NAME="openelisglobal-database"
```

If the container name changes (different Docker Compose profile, CI environment,
Kubernetes), all fixture loading breaks.

#### Problem 6: No Test Data Builders or Factories

The codebase has **no** `DataFactory`, `TestDataBuilder`, `ObjectMother`, or
similar patterns. All test data is pre-baked in static files. This means:

- Tests cannot generate unique data per run (collision risk in parallel)
- Every new test scenario requires editing XML/SQL files
- Test data intent is hidden in opaque fixture files rather than expressed in
  test code
- Schema changes require updating multiple fixture files manually

#### Problem 7: Verification Doesn't Match Test Expectations

The `verify_fixtures()` function in `load-test-fixtures.sh` checks coarse counts
(rooms ≥ 3, samples ≥ 10) but not that specific entities exist with specific
attributes. A test that needs provider "Optimus Prime" will pass verification
but fail at runtime if the provider exists with a different name.

#### Problem 8: No Reset Between Individual Tests

Cypress runs with `testIsolation: false` and fixtures are loaded once per suite
(in `before()`), not per test (`beforeEach()`). This means:

- Test A creates a storage assignment → Test B sees it
- Test C deletes a room → Test D can't find it
- Test order matters; `it.only()` may fail for tests that depend on prior test
  side-effects

The Cypress official best practices docs explicitly warn: _"Tests should always
be able to be run independently from one another and still pass."_

### 5.5 How This Connects to the Metadata Problem

The test data problem is a **microcosm** of the broader metadata problem:

| Test Data Problem            | Metadata Problem                                                    |
| ---------------------------- | ------------------------------------------------------------------- |
| 4 ways to load test fixtures | Reference data in Liquibase AND ConfigInit AND plugins AND admin UI |
| No policy on which mechanism | No policy on which tier owns which data                             |
| Hardcoded IDs in fixtures    | Hardcoded IDs in Liquibase changesets                               |
| Fixtures drift from schema   | SQL dump drifts from Liquibase                                      |
| Docker coupling in scripts   | Docker startup race conditions                                      |
| No test data builders        | No API for programmatic metadata creation                           |

**Solving the metadata problem also solves the test data problem**, because both
require:

1. A clear data ownership policy
2. Idempotent, composable data loading
3. Decoupling from specific Docker container names
4. A programmatic API for creating data (not just static files)

### 5.6 Industry Best Practice for E2E Test Data

Based on research of Cypress official docs, the Cypress Real World App, and
Spring testing patterns:

#### The Gold Standard: Per-Test Database Seeding

```javascript
// cypress.config.js
on("task", {
  async "db:seed"() {
    const { data } = await axios.post(`${testDataApiEndpoint}/seed`);
    return data;
  },
});

// In each test file:
beforeEach(function () {
  cy.task("db:seed"); // Reset to known state BEFORE every test
});
```

This pattern from the Cypress Real World App ensures:

- Every test starts from a deterministic state
- No test depends on another test's side effects
- Tests can run in any order, including `it.only()`

#### API-Driven Test Setup

The Cypress docs recommend using `cy.request()` over UI-driven setup:

```javascript
beforeEach(() => {
  // Create test patient via API (fast, reliable)
  cy.request("POST", "/api/rest/patient", {
    firstName: `E2E-${Date.now()}`,
    lastName: "TestPatient",
    nationalId: `E2E-PAT-${Cypress._.uniqueId()}`,
  }).then((response) => {
    cy.wrap(response.body.id).as("patientId");
  });
});
```

#### Test Data Builders (Java Side)

For integration tests sharing data with E2E:

```java
public class PatientBuilder {
    private String firstName = "Test";
    private String nationalId = "E2E-" + UUID.randomUUID();

    public PatientBuilder withFirstName(String name) {
        this.firstName = name; return this;
    }
    public Patient build() { /* ... */ }

    public static PatientBuilder aPatient() {
        return new PatientBuilder();
    }
}
```

#### Composable SQL Fixtures with Symbolic References

```sql
-- Use names/codes, not hardcoded IDs
INSERT INTO clinlims.sample_item (samp_id, type_of_sample_id, sort_order)
SELECT s.id, tos.id, 1
FROM clinlims.sample s, clinlims.type_of_sample tos
WHERE s.accession_number = 'E2E-SAMPLE-001'
  AND tos.description = 'Serum'
ON CONFLICT DO NOTHING;
```

---

## 6. Industry Best Practices & Research

### 6.1 Liquibase Best Practices (Current Consensus)

**Separation of concerns**: Schema DDL and reference DML should be in separate
changelog files/directories. Schema changes are structural and
version-dependent. Reference data has a different lifecycle.

**Changelog organization**: The version-directory pattern (which OE uses) is
well-suited for release-based deployments. For modular subsystems, a
feature-directory pattern (like `analyzer/`) is recommended alongside.

**Performance**: With hundreds of changesets, Liquibase scanning adds 5–30
seconds to startup. For Docker/CI environments, consider running Liquibase as a
separate init step rather than on every application boot.

**Never modify executed changesets**: Create new changesets to alter data. This
is well-observed in OE.

### 6.2 The Core Anti-Pattern: Reference Data in Migrations

The most relevant anti-pattern for OE:

> **"Using database migration tools to manage reference data that changes
> independently of the schema."**

**Symptoms** (all present in OE):

- Dozens of `<insert>` statements for test definitions, dictionary values
- `<preConditions>` SQL checks to avoid duplicate inserts
- Environment-specific data (Madagascar tests vs. other deployments)
- Data that administrators should be able to modify at runtime

**Why this is problematic**:

- **Dual source of truth**: Data can be modified via admin UI AND via Liquibase
- **Deployment coupling**: A test definition change requires a database
  migration
- **Rollback complexity**: Rolling back data inserts that have dependencies is
  far harder than DDL rollbacks
- **Environment divergence**: Different sites need different reference data, but
  Liquibase contexts become unwieldy at scale

### 6.3 Init Container Pattern (Docker Best Practice)

Instead of running Liquibase inside the application on every startup:

```yaml
# Kubernetes-style (can be adapted for Docker Compose)
initContainers:
  - name: db-migrate
    image: openelis-global:latest
    command: ["liquibase", "update"]
    # Runs ONCE before app starts
containers:
  - name: app
    image: openelis-global:latest
    # Starts only after migration succeeds
```

**Benefits**: Migration runs exactly once, no lock contention, clear separation,
faster application startup.

### 6.4 Application-Level Seed Data

Spring Boot `ApplicationRunner` / `CommandLineRunner` pattern:

```java
@Component
@Order(1)
public class AnalyzerSeedDataLoader implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        if (!analyzerRepo.existsByName("Horiba ABX Pentra 60")) {
            analyzerRepo.save(new Analyzer("Horiba ABX Pentra 60", ...));
        }
    }
}
```

**This is essentially what `ConfigurationInitializationService` already does.**
The pattern is proven and correct — it just needs to be the _only_ source for
reference data, not a parallel one alongside Liquibase.

---

## 7. Comparable Projects: OpenMRS & FHIR

### 7.1 OpenMRS Approach (Most Relevant Comparison)

OpenMRS — the closest comparable open-source health informatics project — uses a
**multi-tier approach** that OE should learn from:

1. **Liquibase for schema only**: OpenMRS uses Liquibase exclusively for DDL.
   Reference data is NOT in Liquibase.

2. **Concept Dictionary**: A dedicated subsystem for clinical metadata
   (equivalent to test definitions in LIMS), managed through admin UI and API.

3. **Metadata Sharing Module (MDS)**: Packages metadata as importable/exportable
   ZIP packages, version-controlled separately from the application.

4. **Initializer Module**: Loads metadata from configuration files (CSV, JSON)
   on startup from a `configuration/` directory. **This is exactly what OE's
   `ConfigurationInitializationService` does.**

5. **Distribution bundles**: Specific deployments (e.g., "OpenMRS for Bahmni",
   "OpenMRS for KenyaEMR") bundle their own metadata packages applied on top of
   the base system. **This is what OE needs for Madagascar vs. other sites.**

**Key takeaway**: OpenMRS explicitly separates metadata lifecycle from schema
lifecycle. Metadata is managed through a dedicated subsystem, not database
migrations.

### 7.2 FHIR-Based Terminology Management

FHIR R4 provides standard resources for reference data:

- **CodeSystem**: Sets of codes (test types, result statuses) — equivalent to
  Liquibase dictionary/lookup inserts
- **ValueSet**: Subsets of codes for specific use cases
- **ConceptMap**: Maps between code systems (analyzer codes → LOINC) —
  equivalent to `AnalyzerTestMapping`
- **Bundle transactions**: Atomic loading of resource sets (standard "seeding")

Since OE's constitution mandates **FHIR R4 compliance**, managing reference data
as FHIR-compatible structures aligns with the project's architectural direction.

---

## 8. Gap Analysis: Where OE Stands vs. Best Practice

| Aspect                        | Current State                                                  | Best Practice                           | Gap                                                                             |
| ----------------------------- | -------------------------------------------------------------- | --------------------------------------- | ------------------------------------------------------------------------------- |
| **Schema management**         | Liquibase (DDL)                                                | Liquibase (DDL only)                    | ✅ Aligned                                                                      |
| **Analyzer metadata**         | Plugin self-registration                                       | Plugin self-registration                | ✅ Aligned                                                                      |
| **Configuration loading**     | `ConfigurationInitializationService`                           | File-based config loading               | ✅ Aligned                                                                      |
| **Admin UI management**       | React + REST controllers                                       | Runtime admin UI                        | ✅ Aligned                                                                      |
| **Test definitions**          | **Liquibase changesets** (legacy)                              | File-based or API-driven                | ⚠️ Gap                                                                          |
| **Dictionary/lookup values**  | **Liquibase changesets** (legacy)                              | File-based or API-driven                | ⚠️ Gap                                                                          |
| **Menu/localization**         | Liquibase changesets                                           | Liquibase OR config files (debatable)   | 🟡 Minor                                                                        |
| **Database initialization**   | Dual source (SQL dump + Liquibase)                             | Single source (Liquibase from empty DB) | ⚠️ Gap                                                                          |
| **Docker startup**            | App-level Liquibase + race risk                                | Init container + health-check wait      | ⚠️ Gap                                                                          |
| **Site-specific data**        | Not cleanly separated                                          | Deployment bundles / config overrides   | ⚠️ Gap                                                                          |
| **Data lifecycle tracking**   | Liquibase `DATABASECHANGELOG`                                  | Checksums + audit tables                | 🟡 Minor                                                                        |
| **E2E test fixtures**         | 4 competing mechanisms                                         | Single composable strategy              | ⚠️ Gap                                                                          |
| **Test data isolation**       | Suite-level, `testIsolation: false`                            | Per-test reset via `beforeEach`         | ⚠️ Gap                                                                          |
| **Test data idempotency**     | Mixed (some idempotent, some not)                              | All fixtures idempotent (ON CONFLICT)   | ⚠️ Gap                                                                          |
| **Fixture dependency docs**   | Implicit, undocumented                                         | Explicit dependency graph               | ⚠️ Gap                                                                          |
| **Test data builders**        | None (static files only)                                       | Builder pattern with factories          | ⚠️ Gap                                                                          |
| **Handler coverage**          | 8 handlers (tests, samples, dicts, roles, OCL, questionnaires) | Handler for every reference data domain | ⚠️ Gap — no handlers for UoM, panels, reference ranges, analyzer field mappings |
| **Classpath default configs** | Empty (`src/main/resources/configuration/` has no files)       | Bundled defaults for fresh installs     | ⚠️ Gap — fresh installs rely entirely on Liquibase for reference data           |

---

## 9. Recommendations

### 9.1 Policy: Clear Ownership Boundaries

**Recommended ownership model:**

| Tier                              | Owner                                | When to Add Data                                                                                      |
| --------------------------------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| **Tier 1: Schema (DDL)**          | Liquibase                            | New tables, columns, indexes, constraints                                                             |
| **Tier 2: Structural Data**       | Liquibase                            | Data the code depends on by constant ID (e.g., `site_information` keys, role definitions, menu items) |
| **Tier 3: Domain Reference Data** | `ConfigurationInitializationService` | Test definitions, sample types, dictionary values, test sections                                      |
| **Tier 4: Plugin Metadata**       | Plugin self-registration             | Analyzer definitions, test mappings, protocol config                                                  |
| **Tier 5: Runtime Config**        | Admin UI + REST API                  | IP addresses, ports, field mappings, deployment-specific settings                                     |
| **Tier 6: Site-Specific Data**    | Deployment bundles (config files)    | Madagascar-specific tests, country-specific dictionaries                                              |

### 9.2 Short-Term Improvements (No Architecture Change)

1. **Document the policy**: Add a `METADATA_MANAGEMENT.md` that explains which
   tier each type of data belongs to. Reference it in AGENTS.md and
   constitution.md.

2. **Stop adding reference data to Liquibase**: For all new features, use
   `ConfigurationInitializationService` or plugin registration for reference
   data. Only schema DDL and truly structural data goes in Liquibase.

3. **Fix the Docker startup race**: Add explicit database readiness checking in
   the Docker entrypoint script before starting Tomcat, or use Docker Compose
   v2's `depends_on: condition: service_healthy`.

4. **Consolidate Liquibase contexts documentation**: Create a clear table of
   which contexts are required for each environment type (dev, test, CI, prod).

### 9.3 Medium-Term Improvements (Incremental)

5. **Create deployment bundles**: Package site-specific reference data as
   configuration directories:

   ```
   /var/lib/openelis-global/configuration/
     tests/madagascar-hematology.csv
     tests/madagascar-biochemistry.csv
     dictionaries/madagascar-results.json
     analyzers/horiba-pentra-60.yml
   ```

6. **Migrate the largest data changesets**: Move the reference data from
   `new_tests.xml`, `add_tb_tests.xml`, and `immunohistochemistry.xml` into
   CSV/JSON files loaded by `ConfigurationInitializationService`. Keep the
   Liquibase changesets but mark them with `context="legacy"` and
   `<preConditions onFail="MARK_RAN">` to handle existing installations.

7. **Eliminate the dual-source database initialization**: Either:

   - (a) Migrate `OpenELIS-Global.sql` into Liquibase changesets (single source
     of truth), OR
   - (b) Use the SQL dump as the baseline and ensure Liquibase only handles
     changes _after_ the baseline (current approach, but formalize it)

8. **Add an init container for Liquibase**: Separate database migration from
   application startup. This improves boot time and prevents lock contention.

### 9.4 Test Data Strategy

9. **Standardize on ONE fixture loading pattern**: Deprecate the DBUnit XML →
   Python → SQL pipeline. Use idempotent SQL scripts (`ON CONFLICT DO NOTHING`
   or `WHERE NOT EXISTS`) as the single mechanism. All features should follow
   the pattern established by `e2e-foundational-data.sql`.

10. **Make all fixtures idempotent**: Convert `xml-to-sql.py` to generate
    `ON CONFLICT (id) DO NOTHING` or replace it entirely with hand-written
    idempotent SQL. This eliminates the need for `reset-test-database.sh` before
    every fixture load.

11. **Use symbolic references instead of hardcoded IDs**: Fixtures should look
    up parent entities by name/code, not ID:

    ```sql
    INSERT INTO storage_device (room_id, ...) SELECT id, ...
    FROM storage_room WHERE code = 'MAIN'
    ON CONFLICT DO NOTHING;
    ```

12. **Document the fixture dependency graph**: Create a `TEST_DATA.md` that
    explicitly lists the loading order and dependencies (see Section 5.3).

13. **Decouple from Docker container names**: Use environment variables for
    database connection in all scripts and Cypress tasks:

    ```bash
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-15432}"
    DB_CONTAINER="${DB_CONTAINER:-openelisglobal-database}"
    ```

14. **Introduce test data builders** (medium-term): Create Java-side builder
    classes in `src/test/java/.../testdata/builders/` and a REST seed API
    endpoint (gated by test profile) that Cypress can call:

    ```javascript
    cy.request("POST", "/api/test/seed/patient", {
      prefix: "E2E-STORAGE",
      count: 5,
    });
    ```

15. **Move toward per-test reset** (long-term): Reduce reliance on suite-level
    `before()` fixtures. Implement a fast reset mechanism (database snapshot
    restore or transactional rollback) that enables `beforeEach()` seeding.

### 9.5 Long-Term Vision

16. **FHIR Terminology Service**: Manage test definitions, code systems, and
    concept maps as FHIR resources. This aligns with the constitutional mandate
    for FHIR R4 compliance and provides a standards-based approach to reference
    data exchange between OE instances.

17. **Metadata Package Import/Export**: Like OpenMRS's Metadata Sharing Module,
    allow administrators to export a site's metadata configuration as a package
    and import it into another site. This would be invaluable for deploying OE
    to new countries.

18. **Reference Data Versioning**: Track changes to reference data with the same
    rigor as schema changes — audit trail, version numbers, rollback capability.
    The `ConfigurationInitializationService` checksum tracking is a start.

---

## 10. Proposed Architecture: Metadata Management Tiers

```
┌────────────────────────────────────────────────────────────────────┐
│                     DEPLOYMENT TIME                                │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  TIER 1: Liquibase — Schema Only                            │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │  │
│  │  │ CREATE TABLE   │  │ ALTER TABLE    │  │ CREATE INDEX │  │  │
│  │  │ DROP COLUMN    │  │ ADD CONSTRAINT │  │ Sequences    │  │  │
│  │  └────────────────┘  └────────────────┘  └──────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  TIER 2: Liquibase — Structural Data                        │  │
│  │  ┌─────────────────┐  ┌──────────────┐  ┌───────────────┐  │  │
│  │  │ site_information│  │ roles/perms  │  │ menu_items    │  │  │
│  │  │ (system keys)   │  │ (code refs)  │  │ (UI structure)│  │  │
│  │  └─────────────────┘  └──────────────┘  └───────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│                     APPLICATION STARTUP                             │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  TIER 3: ConfigurationInitializationService                 │  │
│  │  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐  │  │
│  │  │ Test defs     │  │ Sample types  │  │ Dictionaries   │  │  │
│  │  │ (CSV/JSON)    │  │ (CSV)         │  │ (JSON)         │  │  │
│  │  └───────────────┘  └───────────────┘  └────────────────┘  │  │
│  │  Source: classpath + /var/lib/openelis-global/configuration/ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  TIER 4: Plugin Self-Registration                           │  │
│  │  ┌────────────────┐  ┌──────────────────┐  ┌────────────┐  │  │
│  │  │ Analyzer defs  │  │ Test mappings    │  │ Protocol   │  │  │
│  │  │ (plugin JARs)  │  │ (plugin JARs)   │  │ configs    │  │  │
│  │  └────────────────┘  └──────────────────┘  └────────────┘  │  │
│  │  Source: /var/lib/openelis-global/plugins/                  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│                     RUNTIME                                        │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  TIER 5: Admin UI + REST API                                │  │
│  │  ┌────────────────┐  ┌──────────────────┐  ┌────────────┐  │  │
│  │  │ IP/port config │  │ Field mappings   │  │ Enable/    │  │  │
│  │  │ Serial config  │  │ QC rules         │  │ disable    │  │  │
│  │  └────────────────┘  └──────────────────┘  └────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  TIER 6: Site-Specific Deployment Bundles                   │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │ /var/lib/openelis-global/configuration/                │  │  │
│  │  │   tests/madagascar-hematology.csv                      │  │  │
│  │  │   dictionaries/madagascar-results.json                 │  │  │
│  │  │   analyzers/site-specific-configs.yml                  │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 11. Migration Path

### Phase 1: Policy & Prevention (Immediate)

- [ ] Write `METADATA_MANAGEMENT.md` documenting the tier model
- [ ] Update constitution.md to reference the policy
- [ ] Add PR review checklist item: "Is new reference data in the correct tier?"
- [ ] Stop adding new reference data to Liquibase (schema only going forward)

### Phase 2: Test Data Cleanup (Short-Term)

- [ ] Standardize on idempotent SQL as the single fixture loading mechanism
- [ ] Convert `xml-to-sql.py` output to use `ON CONFLICT DO NOTHING`
- [ ] Create `TEST_DATA.md` documenting fixture dependency graph
- [ ] Decouple all scripts from hardcoded Docker container names
- [ ] Remove the commented-out Liquibase test changeset (single source of truth)

### Phase 3: Docker Improvements (Short-Term)

- [ ] Fix Docker startup race condition (use
      `depends_on: condition: service_healthy`)
- [ ] Document Liquibase context requirements for all environments
- [ ] Add CI validation that `SPRING_LIQUIBASE_CONTEXTS` is set correctly
- [ ] Evaluate init container pattern for separating Liquibase from app startup

### Phase 4: Data Migration (Medium-Term)

- [ ] Formalize the dual-source database initialization (SQL dump as baseline +
      Liquibase for changes)
- [ ] Write missing `DomainConfigurationHandler` implementations:
  - [ ] `UnitOfMeasureConfigurationHandler` (order 100) — prerequisite for test
        migration
  - [ ] `PanelConfigurationHandler` (order 220) — test panels/bundles (e.g.,
        "CBC Panel")
  - [ ] `ResultReferenceRangeConfigurationHandler` (order 220) —
        age/gender-based normal ranges
- [ ] Create default classpath CSV/JSON files in
      `src/main/resources/configuration/`:
  - [ ] `sample-types/default.csv`, `test-sections/default.csv`,
        `units-of-measure/default.csv`
  - [ ] `tests/` — extract from `new_tests.xml` (236 changesets),
        `add_tb_tests.xml` (126), `add_hpv_testing.xml` (33)
  - [ ] `dictionaries/` — extract from TB/HPV/immunohistochemistry result
        dictionaries
- [ ] Add `context="legacy"` to the migrated Liquibase data changesets (keep for
      existing installations)
- [ ] Test both fresh install (handler-only) and upgrade (Liquibase `MARK_RAN` +
      handler) paths

### Phase 5: Deployment Bundles & Test Infra (Medium-Term)

- [ ] Define "site bundle" format (directory structure, file naming)
- [ ] Create Madagascar bundle as reference implementation
- [ ] Add bundle management to admin UI
- [ ] Document how to create new site bundles
- [ ] Create Java test data builders for Patient, Sample, Analyzer entities
- [ ] Implement a REST seed API endpoint (test profile only) for Cypress
- [ ] Evaluate per-test database reset via pg_dump snapshot restore

### Phase 6: FHIR Integration (Long-Term)

- [ ] Evaluate FHIR Terminology Service for code system management
- [ ] Prototype CodeSystem/ValueSet resources for test definitions
- [ ] Design ConceptMap resources for analyzer test mappings
- [ ] Plan migration of dictionary/lookup data to FHIR resources

---

## 12. Key References

### Official Documentation

- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html)
- [Liquibase Changelog Organization](https://docs.liquibase.com/concepts/changelogs/how-liquibase-finds-files.html)
- [Spring Boot Liquibase Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)
- [FHIR Terminology Module](https://www.hl7.org/fhir/terminology-module.html)
- [FHIR CodeSystem Resource](https://www.hl7.org/fhir/codesystem.html)

### Comparable Projects

- [OpenMRS Initializer Module](https://github.com/mekomsolutions/openmrs-module-initializer)
  — The most relevant comparable for healthcare metadata management
- [OpenMRS Metadata Sharing Module](https://wiki.openmrs.org/display/docs/Metadata+Sharing+Module)
- [OpenMRS Concept Dictionary](https://wiki.openmrs.org/display/docs/Concept+Dictionary)

### Architecture Patterns

- [Martin Fowler — Evolutionary Database Design](https://martinfowler.com/articles/evodb.html)
- [Kubernetes Init Containers](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)
- Pramod Sadalage & Martin Fowler — _Refactoring Databases_ (book)

### Test Data Management

- [Cypress Best Practices — Test Isolation](https://docs.cypress.io/guides/references/best-practices)
- [Cypress Testing Your App — Seeding Data](https://docs.cypress.io/guides/end-to-end-testing/testing-your-app)
- [Cypress `cy.task()` API](https://docs.cypress.io/api/commands/task)
- [Cypress `cy.fixture()` API](https://docs.cypress.io/api/commands/fixture)
- [Cypress Real World App](https://github.com/cypress-io/cypress-realworld-app)
  — Reference implementation for test data seeding
- [Spring `@Sql` Annotation](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/executing-sql.html)
- [Synthea — Synthetic Patient Generator](https://github.com/synthetichealth/synthea)
- [Martin Fowler — Eradicating Non-Determinism in Tests](https://martinfowler.com/articles/nonDeterminism.html)

### Alternative Tools

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Liquibase vs. Flyway Comparison](https://www.liquibase.com/liquibase-vs-flyway)

---

## Appendix A: Key Files Referenced

| File                                                                                             | Role                                   |
| ------------------------------------------------------------------------------------------------ | -------------------------------------- |
| `src/main/java/org/openelisglobal/liquibase/LiquibaseConfig.java`                                | Spring Liquibase configuration         |
| `src/main/resources/liquibase/base-changelog.xml`                                                | Master changelog entry point           |
| `src/main/java/org/openelisglobal/common/services/PluginAnalyzerService.java`                    | Plugin metadata registration           |
| `src/main/java/org/openelisglobal/configuration/service/ConfigurationInitializationService.java` | Config file loading                    |
| `src/main/java/org/openelisglobal/configuration/service/DomainConfigurationHandler.java`         | Domain handler interface               |
| `src/main/java/org/openelisglobal/analyzerimport/util/AnalyzerTestNameCache.java`                | In-memory test name cache              |
| `src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java`                            | Analyzer entity                        |
| `src/main/java/org/openelisglobal/analyzerimport/valueholder/AnalyzerTestMapping.java`           | Test mapping entity                    |
| `docker-compose.yml`                                                                             | Production Docker Compose              |
| `db/Dockerfile`                                                                                  | Database Docker image                  |
| `db/dbInit/OpenELIS-Global.sql`                                                                  | Baseline schema dump (~864KB)          |
| `install/docker-entrypoint.sh`                                                                   | Web app entrypoint                     |
| `scripts/simulate-ci-failure.sh`                                                                 | CI failure reproduction                |
| `src/test/resources/load-test-fixtures.sh`                                                       | Master E2E fixture loader (409 lines)  |
| `src/test/resources/reset-test-database.sh`                                                      | Test data range reset (174 lines)      |
| `src/test/resources/e2e-foundational-data.sql`                                                   | Providers + organizations (idempotent) |
| `src/test/resources/testdata/storage-e2e.xml`                                                    | Storage E2E DBUnit dataset             |
| `src/test/resources/testdata/xml-to-sql.py`                                                      | DBUnit XML → SQL converter             |
| `src/test/java/org/openelisglobal/testutils/DbUnitDatasetLoader.java`                            | Standalone DBUnit execution            |
| `src/test/java/org/openelisglobal/BaseTestConfig.java`                                           | Testcontainers + Liquibase config      |
| `frontend/cypress/fixtures/*.json`                                                               | 13 Cypress fixture files               |
| `frontend/cypress/support/commands.js`                                                           | Custom Cypress commands                |
| `frontend/cypress/support/storage-setup.js`                                                      | Storage test setup orchestration       |
| `frontend/cypress.config.js`                                                                     | Cypress task handlers for DB ops       |

## Appendix B: Liquibase Changelog Size Analysis

| File                                    | Lines | Changesets | Category          |
| --------------------------------------- | ----- | ---------- | ----------------- |
| `2.8.x.x/immunohistochemistry.xml`      | 2,475 | 16         | Mixed schema+data |
| `2.3.x.x/new_tests.xml`                 | 2,173 | 236        | Pure data         |
| `2.7.x.x/add_tb_tests.xml`              | 2,075 | 126        | Pure data         |
| `3.3.x.x/freezer-monitoring-schema.xml` | 863   | ~20        | Mixed schema+data |
| `2.7.x.x/add_hpv_testing.xml`           | 769   | 33         | Mixed schema+data |

These 5 files account for ~8,355 lines and ~431 changesets — roughly 1/3 of all
estimated changesets in the entire Liquibase history.
