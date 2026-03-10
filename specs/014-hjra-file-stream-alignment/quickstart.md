# Quickstart: File Stream Alignment — GenericFile Lane

**Feature**: 014-hjra-file-stream-alignment  
**Date**: 2026-03-10

## Architecture Overview

This lane produces a **GenericFile plugin** as a peer to GenericASTM and
GenericHL7.

| Ownership                  | Location                           | What It Contains                                                                                                               |
| -------------------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| App (this repo)            | `src/main/java/...`                | Format-normalizing readers (`FileAnalyzerReader`, `ExcelAnalyzerReader`), service/watcher/REST, `PluginRegistryService` update |
| Plugin (plugins submodule) | `plugins/analyzers/GenericFile/`   | `GenericFileAnalyzer`, `GenericFileLineInserter`, `plugin.xml`                                                                 |
| Profiles (this repo)       | `projects/analyzer-profiles/file/` | `quantstudio.json`, `wondfo-csv.json`                                                                                          |

QuantStudio and Wondfo are the first two GenericFile **profiles**, not
standalone WAR parsers.

## Prerequisites

- Java 21 (`java -version` → 21.x.x)
- Docker + Docker Compose running
- OpenELIS dev environment up (`docker compose -f dev.docker-compose.yml up -d`)
- Plugins submodule checked out (`git submodule update --init plugins`)

## Branch Setup

```bash
# Start from develop (after syncing with upstream)
git checkout develop
git pull origin develop

# M1: File Config Foundation
git checkout -b feat/014-ogc-329-file-config-foundation

# After M1 merges, M2 and M3 can start in parallel:
git checkout develop && git pull
git checkout -b feat/014-ogc-324-upload-review-ui      # M2
git checkout -b feat/014-ogc-348-quantstudio-import     # M3 (includes GenericFile plugin)

# After M3 merges:
git checkout develop && git pull
git checkout -b feat/014-ogc-344-wondfo-csv-import      # M4
```

## M1: File Config Foundation (OGC-329)

### What Changes

1. Add `fileFormat` column to `file_import_configuration` table (Liquibase)
2. Update `FileImportConfiguration` entity with `fileFormat` field
3. Update admin UI to show `fileFormat` dropdown when file-import plugin
   selected
4. Remove `FILE: "ASTM_LIS2_A2"` from `constants.js`
5. Hide `ProtocolVersion` dropdown for file-import analyzers

### Build & Verify

```bash
# Backend
mvn spotless:apply
mvn clean install -DskipTests -Dmaven.test.skip=true
mvn test -pl . -Dtest="*FileImport*"

# Frontend
cd frontend
npm run format
npm test -- --watchAll=false --testPathPattern="FileImport"
cd ..

# E2E (Playwright) — requires analyzer harness + DATABASE_CONTAINER
# /restart-analyzer-harness --full-reset --build  # then:
# TEST_USER=admin TEST_PASS=adminADMIN! DATABASE_CONTAINER=analyzer-harness-db-1 \
#   npm run pw:test -- playwright/tests/fileImportConfig.spec.ts playwright/tests/fileImportQuantStudio.spec.ts

# Hot reload
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

## M3: GenericFile Plugin + QuantStudio Profile (OGC-348)

### What Changes

**App side:**

1. Add `poi-ooxml` 5.4.0 to `pom.xml` (sibling of existing `poi` dep; needed for
   .xlsx)
2. New `ExcelAnalyzerReader` in
   `src/main/java/.../analyzerimport/analyzerreaders/`
3. Format dispatch in `FileImportServiceImpl` (EXCEL → ExcelAnalyzerReader)
4. `PluginRegistryService` update for FILE protocol detection

**Plugin side (`plugins/analyzers/GenericFile/`):**

1. `GenericFileAnalyzer` — implements `AnalyzerImporterPlugin`,
   `isGenericPlugin()=true`
2. `GenericFileLineInserter` — profile-driven column mapping → `AnalyzerResults`
3. `plugin.xml` descriptor

**Profiles:**

1. `projects/analyzer-profiles/file/quantstudio.json` — QuantStudio QS5/QS7
   column mappings

### Build & Verify

```bash
# App-side (this repo)
mvn spotless:apply
mvn test -pl . -Dtest="ExcelAnalyzerReaderTest,FileImportServiceIntegrationTest"

# Plugin-side (in plugins submodule — coordinate with plugins repo)
cd plugins/analyzers/GenericFile
mvn test -Dtest="GenericFileAnalyzerTest,GenericFileLineInserterTest"
cd ../../..
```

Place QS5/QS7 .xls files in `src/test/resources/testdata/quantstudio/` before
running integration tests. Playwright E2E uses
`frontend/playwright/fixtures/quantstudio-qs7.xls` and `file-import-setup.sql`
(loads E2E-FILE-CSV-Analyzer and E2E-FILE-QuantStudio-Analyzer).

## M4: Wondfo CSV Profile (OGC-344)

### What Changes

1. `projects/analyzer-profiles/file/wondfo-csv.json` — 40-column Wondfo mapping,
   comparison operators
2. `GenericFileLineInserter` extended for comparison operator handling in plugin
   submodule

### Build & Verify

```bash
# Plugin-side tests (in plugins submodule)
cd plugins/analyzers/GenericFile
mvn test -Dtest="GenericFileLineInserterTest"
cd ../../..

# Watcher integration test (app side)
mvn test -pl . -Dtest="FileImportWatchServiceIntegrationTest"
```

Place `history.csv` in `src/test/resources/testdata/wondfo/` before running
integration tests.

## Key Files to Know

| File                                                            | Ownership    | What It Does                                               |
| --------------------------------------------------------------- | ------------ | ---------------------------------------------------------- |
| `analyzer/valueholder/FileImportConfiguration.java`             | App          | File transport config (directories, `fileFormat`, watcher) |
| `analyzer/valueholder/AnalyzerPluginConfig.java`                | App          | Per-analyzer JSONB config; stores profile defaults         |
| `analyzer/service/FileImportServiceImpl.java`                   | App          | Format dispatch → reader → plugin handoff                  |
| `analyzer/service/FileImportWatchService.java`                  | App          | Polls directories for new files                            |
| `analyzer/service/PluginRegistryService.java`                   | App          | Auto-discovers plugins as `AnalyzerType` rows              |
| `analyzerimport/analyzerreaders/FileAnalyzerReader.java`        | App          | CSV/TSV normalization                                      |
| `analyzerimport/analyzerreaders/ExcelAnalyzerReader.java`       | App (new M3) | .xls/.xlsx normalization                                   |
| `plugins/analyzers/GenericFile/...GenericFileAnalyzer.java`     | Plugin       | Plugin entry point                                         |
| `plugins/analyzers/GenericFile/...GenericFileLineInserter.java` | Plugin       | Profile-driven mapper                                      |
| `projects/analyzer-profiles/file/quantstudio.json`              | Profile (M3) | QuantStudio QS5/QS7 column mapping                         |
| `projects/analyzer-profiles/file/wondfo-csv.json`               | Profile (M4) | Wondfo 40-column CSV mapping                               |
| `frontend/src/components/analyzers/constants.js`                | App          | Protocol defaults (FILE→ASTM removed)                      |
| `frontend/src/components/analyzers/FileImportConfiguration/`    | App          | Admin config panel                                         |

## Final Verification (M1A+M1B+M3)

- [ ] Backend: `mvn test -pl . -Dtest="*FileImport*,*ExcelAnalyzer*"`
- [ ] Frontend Jest:
      `npm test -- --watchAll=false --testPathPattern="FileImport"`
- [ ] Playwright E2E:
      `TEST_USER=admin TEST_PASS=adminADMIN! DATABASE_CONTAINER=analyzer-harness-db-1 npm run pw:test -- playwright/tests/fileImportConfig.spec.ts playwright/tests/fileImportQuantStudio.spec.ts`
- [ ] i18n: `fileImport.format.*` and `file.import.configuration.fileFormat*`
      present in `en.json` and `fr.json`

## Common Gotchas

- **Test skipping**: Always use BOTH flags:
  `mvn clean install -DskipTests -Dmaven.test.skip=true`
- **Format before commit**:
  `mvn spotless:apply && cd frontend && npm run format && cd ..`
- **JUnit 4, not 5**: Use `import org.junit.Test` not
  `org.junit.jupiter.api.Test`
- **jakarta, not javax**: Use `import jakarta.persistence.*`
- **Transactions in service only**: No `@Transactional` on controllers
- **Plugins submodule**: M3 plugin work requires
  `git submodule update --init plugins` — do NOT create WAR-local parsers as a
  workaround
