# Madagascar File-Based Analyzer Sprint — Implementation Prompt

> **Branch:** `feat/014-madagascar-file-analyzer-sprint` > **Worktree:** >
> `/home/pmanko/code/OpenELIS-file-analyzers` > **Base:** `origin/develop`
> (6ff0774690) **Deadline:** Mon Apr 6, 2026 (release 3.2.1.5 cut Tue Apr 7)
> **Jira Epic:** OGC-304 (Madagascar Analyzer Work) **Tickets:** OGC-417 (Tecan
> F50), OGC-418 (Multiskan FC), OGC-344 (Wondfo CSV)

---

## Mission

Implement file-import support for the **3 remaining Madagascar file-based
analyzers** so they can be configured and tested on the UAT server after 3.2.1.5
deploys. These are ELISA plate readers (Tecan, Multiskan) and a POC immunoassay
device (Wondfo) that export results as flat files (CSV/Excel).

**Ground rules:**

- Minimum viable implementation — these must be testable by Herbert on UAT
- Follow the existing GenericFile plugin architecture (profiles + plugin JAR)
- Real export files are now in hand for all 3 instruments (as of Apr 3)
- Herbert will validate column mappings against real site exports
- French locale support is required (semicolon delimiters, comma decimals)

---

## Architecture Context

### How File Import Works in OpenELIS

```
User uploads file (or bridge watches directory)
        |
        v
FileImportRestController (/rest/analyzer/file-import/parse)
        |
        v
FileImportServiceImpl.processFile()
   |-- resolves FileImportConfiguration for the analyzer
   |-- dispatches to reader based on fileFormat:
   |     CSV/TSV  -> FileAnalyzerReader (Apache Commons CSV)
   |     EXCEL    -> ExcelAnalyzerReader (Apache POI)
   |-- reader normalizes file to List<String> lines
   |-- finds matching plugin via PluginAnalyzerService.isTargetAnalyzer(lines)
   |-- plugin's AnalyzerLineInserter transforms lines -> AnalyzerResults
        |
        v
Preview shown to user (slot-based UI from OGC-324)
        |
        v
User submits -> results persisted to database
```

### Key Source Files (App Side — this repo)

| File                                                                                       | Purpose                                                                 |
| ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| `src/main/java/org/openelisglobal/analyzer/valueholder/FileImportConfiguration.java`       | Config entity: directories, fileFormat, columnMappings, delimiter       |
| `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java`             | Orchestrates parsing: format dispatch, reader selection, plugin handoff |
| `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReader.java`  | CSV/TSV parser (Apache Commons CSV)                                     |
| `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ExcelAnalyzerReader.java` | Excel parser (.xls/.xlsx via Apache POI)                                |
| `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/PlateGridNormalizer.java` | Converts 8x12 ELISA plate grids to well-per-row format                  |
| `src/main/java/org/openelisglobal/analyzer/controller/FileImportRestController.java`       | REST endpoints for file upload preview + submit                         |
| `src/main/java/org/openelisglobal/analyzer/service/PluginRegistryService.java`             | Auto-discovers plugins, creates AnalyzerType records                    |
| `src/main/java/org/openelisglobal/plugin/PluginLoader.java`                                | Loads plugin JARs from /var/lib/openelis-global/plugins/                |
| `frontend/src/components/analyzers/FileImportConfiguration/FileImportConfiguration.jsx`    | Admin config UI for file import settings                                |
| `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx`                          | Create/edit analyzer with plugin type selection                         |

### Key Source Files (Plugin Side — plugins submodule)

The GenericFile plugin lives in `plugins/analyzers/GenericFile/`. You must check
out the plugins submodule:

```bash
git submodule update --init plugins
```

| File                                                                 | Purpose                                                         |
| -------------------------------------------------------------------- | --------------------------------------------------------------- |
| `plugins/analyzers/GenericFile/src/.../GenericFileAnalyzer.java`     | Plugin entry: `isTargetAnalyzer()`, `getAnalyzerLineInserter()` |
| `plugins/analyzers/GenericFile/src/.../GenericFileLineInserter.java` | Profile-driven column mapping -> AnalyzerResults                |
| `plugins/analyzers/GenericFile/src/main/resources/plugin.xml`        | Plugin descriptor for PluginLoader                              |

### Analyzer Profiles (this repo)

Profiles define per-instrument column mappings, file formats, and locale
settings:

```
projects/analyzer-profiles/file/
├── tecan-f50.json        # Tecan Infinite F50 — EXISTS, needs validation
├── multiskan-fc.json     # Thermo Multiskan FC — EXISTS, needs validation
├── quantstudio.json      # QuantStudio QS5/QS7 — DONE, reference implementation
├── fluorocycler-xt.json  # FluoroCycler XT — DONE
├── genexpert-csv.json    # GeneXpert CSV — DONE
└── dtprime.json          # DT-Prime XML — DONE
```

**Missing:** `wondfo-csv.json` — needs to be created (40-column CSV mapping).

### Existing Test Data

```
src/test/resources/testdata/files/
├── quantstudio-results.csv          # Reference: QuantStudio CSV
├── quantstudio7-flex-results.csv    # Reference: QS7 variant
├── genexpert-results.csv            # Reference: GeneXpert
├── fluorocycler-results.csv         # Reference: FluoroCycler
└── malformed-results.csv            # Error case

frontend/playwright/fixtures/
├── quantstudio-e2e-results.xlsx     # Reference: Excel E2E
├── quantstudio-e2e-results-qs5.xls  # Reference: QS5 variant
├── quantstudio-qs7.xls              # Reference: QS7 variant
└── fluorocycler-e2e-results.xlsx    # Reference: Excel E2E
```

---

## The 3 Analyzers

### 1. Tecan Infinite F50 (OGC-417) — ELISA Plate Reader

**Jira:** [OGC-417](https://uwdigi.atlassian.net/browse/OGC-417) **Site:** LA2M
(Institut Pasteur de Madagascar) **Software:** Tecan Magellan **Export
formats:** CSV/TSV (plate grid), XLSX (plate grid or well-per-row) **Protocol:**
FILE **Profile:** `projects/analyzer-profiles/file/tecan-f50.json` (v1.1.0,
PENDING validation)

**What the profile already defines:**

```json
{
  "column_mapping": {
    "WellPosition": "position",
    "SampleID": "sampleId",
    "OD_450": "result",
    "OD_620": "referenceOD",
    "CorrectedOD": "correctedOD",
    "Interpretation": "interpretation"
  },
  "layouts": {
    "plate_grid": {
      "metadata_rows": [
        "Application",
        "Instrument",
        "Method",
        "Date",
        "Time",
        "Wavelength",
        "Plate"
      ],
      "grid_rows": 8,
      "grid_cols": 12,
      "row_labels": ["A", "B", "C", "D", "E", "F", "G", "H"]
    },
    "well_per_row": {
      "columns": [
        "WellPosition",
        "SampleID",
        "OD_450",
        "OD_620",
        "CorrectedOD",
        "Interpretation"
      ]
    }
  },
  "configDefaults": {
    "fileFormat": "CSV",
    "delimiter": "\t",
    "hasHeader": true
  }
}
```

**Key considerations:**

- PlateGridNormalizer already handles Tecan Magellan format (8x12 grid
  detection)
- French locale: semicolons as delimiter, commas as decimal separator
- Real Magellan exports are now in hand — Herbert needs to verify column mapping
- Two layout modes: plate grid (standard Magellan) and well-per-row (custom
  template)
- Test assays: ELISA for HIV, Hepatitis (OD readings at 450nm and 620nm
  wavelengths)

**What needs to be done:**

1. Validate/update `tecan-f50.json` against real Magellan export files
2. Create test fixture files
   (`src/test/resources/testdata/files/tecan-f50-plate-grid.csv` and
   `tecan-f50-well-per-row.csv`)
3. Ensure GenericFileLineInserter handles Tecan profile column mappings
4. Write unit tests for Tecan-specific parsing (plate grid -> well-per-row
   normalization)
5. Create Playwright E2E fixture
   (`frontend/playwright/fixtures/tecan-f50-e2e-results.xlsx`)

---

### 2. Thermo Multiskan FC (OGC-418) — ELISA Plate Reader

**Jira:** [OGC-418](https://uwdigi.atlassian.net/browse/OGC-418) **Site:** LA2M
(Institut Pasteur de Madagascar) **Software:** Thermo SkanIt **Export formats:**
CSV (plate grid), XLSX **Protocol:** FILE **Profile:**
`projects/analyzer-profiles/file/multiskan-fc.json` (v1.1.0, PENDING validation)

**What the profile already defines:**

```json
{
  "column_mapping": {
    "Well": "position",
    "Sample ID": "sampleId",
    "Absorbance (450 nm)": "result",
    "Absorbance": "result",
    "Content": "content",
    "Concentration": "concentration"
  },
  "layouts": {
    "plate_grid": {
      "metadata_rows": [
        "Instrument",
        "Protocol",
        "Date",
        "Time",
        "Filter",
        "Plate Type"
      ],
      "grid_rows": 8,
      "grid_cols": 12
    },
    "well_per_row": {
      "columns": ["Well", "Sample ID", "Absorbance (450 nm)", "Content"]
    }
  },
  "configDefaults": { "fileFormat": "CSV", "delimiter": ";", "hasHeader": true }
}
```

**Key considerations:**

- Very similar to Tecan — both are ELISA plate readers with 8x12 grids
- PlateGridNormalizer handles Multiskan format (test already exists in
  PlateGridNormalizerTest)
- French locale confirmed — semicolons as delimiter, commas as decimal separator
- SkanIt software version at site needs confirmation
- Real exports are now in hand

**What needs to be done:**

1. Validate/update `multiskan-fc.json` against real SkanIt export files
2. Create test fixture files
   (`src/test/resources/testdata/files/multiskan-fc-plate-grid.csv` and
   `multiskan-fc-well-per-row.csv`)
3. Ensure GenericFileLineInserter handles Multiskan profile column mappings
4. Write unit tests for Multiskan-specific parsing
5. Create Playwright E2E fixture

---

### 3. Wondfo Finecare FS-205 CSV (OGC-344) — POC Immunoassay

**Jira:** [OGC-344](https://uwdigi.atlassian.net/browse/OGC-344) **Site:** LA2M
(Institut Pasteur de Madagascar) **Software:** Built-in device export via USB
**Export format:** CSV (40-column flat file named `history.csv`) **Protocol:**
FILE **Profile:** DOES NOT EXIST YET — must create
`projects/analyzer-profiles/file/wondfo-csv.json`

**What we know about the format (from spec):**

- 40-column CSV file exported via USB to shared directory
- Flat file (one row per result, NOT plate grid)
- Contains comparison operators in result values (`<2`, `>100`) that need
  profile-level handling
- Real CSV files from LA2M are now in hand
- OGC-345 (ASTM variant) is a SEPARATE integration path — do NOT conflate

**Column mapping (to be validated against real file):** Based on the 014 spec,
the Wondfo CSV has columns including:

- Sample ID
- Test name / assay
- Result value (may contain comparison operators)
- Unit
- Reference range
- Date/Time
- Device serial number
- QC status

**What needs to be done:**

1. Create `projects/analyzer-profiles/file/wondfo-csv.json` with 40-column
   mapping
2. Create test fixture
   `src/test/resources/testdata/files/wondfo-finecare-results.csv` from real
   export
3. Handle comparison operators (`<2`, `>100`) in GenericFileLineInserter
4. Write unit tests for Wondfo CSV parsing including comparison operator edge
   cases
5. Create Playwright E2E fixture
6. Ensure CSV delimiter is comma (standard) not semicollon (confirm with
   Herbert)

---

## Implementation Strategy

### Phase A: Profile Validation (all 3 in parallel)

For each analyzer:

1. Obtain real export file from Herbert/site (should be in Slack
   `#ext-madagascar-e-sil` or shared drive)
2. Validate existing profile JSON against actual column headers
3. Update profile if needed
4. Create test fixture files from real data (sanitize patient info)

### Phase B: Wondfo Profile Creation

1. Create `wondfo-csv.json` profile based on real `history.csv` structure
2. Add comparison operator handling to GenericFileLineInserter (if not already
   present)
3. Write tests

### Phase C: Test Coverage

For each analyzer, write:

1. **Unit test**: Profile-driven parsing of the real file format
   - Test file:
     `src/test/java/org/openelisglobal/analyzer/service/FileImportServiceTest.java`
     (extend existing)
   - Or create analyzer-specific:
     `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/TecanF50ReaderTest.java`,
     etc.
2. **PlateGridNormalizer test** (Tecan + Multiskan only): Grid detection and
   normalization
   - Existing:
     `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/PlateGridNormalizerTest.java`
3. **Integration test**: Full pipeline from file upload -> preview -> result
   persistence

### Phase D: E2E Validation

1. Create Playwright fixtures for each analyzer
2. Add E2E tests using the analyzer harness (if time permits)
3. Verify the admin UI correctly loads profile defaults for each analyzer type

---

## Build & Test Commands

```bash
# Check out plugins submodule (required for GenericFile plugin)
git submodule update --init plugins

# Backend build (skip tests for quick iteration)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Run file-import specific tests
mvn test -pl . -Dtest="*FileImport*,*ExcelAnalyzer*,*PlateGrid*"

# Format (mandatory before every commit)
mvn spotless:apply
cd frontend && npm run format && cd ..

# Frontend build
cd frontend && npm install && npm run build && cd ..

# Playwright E2E (requires analyzer harness running)
# TEST_USER=admin TEST_PASS=adminADMIN! DATABASE_CONTAINER=analyzer-harness-db-1 \
#   npm run pw:test -- playwright/tests/demo/harness/file-import-results.spec.ts
```

---

## Critical Reminders

1. **Plugins submodule**: GenericFile plugin code lives in
   `plugins/analyzers/GenericFile/`. You MUST check out the submodule. Do NOT
   create WAR-local parsers as a workaround.
2. **Test skipping**: ALWAYS use BOTH flags:
   `mvn clean install -DskipTests -Dmaven.test.skip=true`
3. **Format before commit**:
   `mvn spotless:apply && cd frontend && npm run format && cd ..`
4. **JUnit 4, not 5**: Use `import org.junit.Test` not
   `org.junit.jupiter.api.Test`
5. **jakarta, not javax**: Use `import jakarta.persistence.*`
6. **Transactions in service only**: No `@Transactional` on controllers
7. **PlateGridNormalizer**: Already handles both Tecan and Multiskan formats —
   reuse, don't rewrite
8. **French locale**: Madagascar sites use semicolons as CSV delimiters and
   commas as decimal separators
9. **Comparison operators**: Wondfo results may contain `<2`, `>100` — these
   need special handling in the line inserter
10. **Two Wondfo paths**: OGC-344 (CSV, this work) and OGC-345 (ASTM, separate)
    must NOT be conflated

---

## Reference: Existing Profile (QuantStudio) as Template

The QuantStudio profile (`projects/analyzer-profiles/file/quantstudio.json`) is
the reference implementation. It demonstrates:

- `profileMeta` with id, version, displayName, manufacturer
- `protocol` with name and format
- `supported_extensions` array
- `column_mapping` object (file column -> OpenELIS field)
- `column_spec` with core columns list
- `configDefaults` for FileImportConfiguration presets
- `defaultTestMappings` for LOINC-based test mapping

Use this as the structural template for the Wondfo profile.

---

## Reference: Confluence Pages

- [Analyzer Support Status Report](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1177419809/OpenELIS+Analyzer+Support+Status+Report)
  — Latest status of all analyzer integrations
- [Analyzer Integration Tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396/OpenELIS+Global+Analyzer+Integration+Tracker)
  — Full tracker with protocol, site, status per analyzer

---

## Reference: Key People

- **Herbert** — On-site testing at LA2M. Has real export files. Validates column
  mappings.
- **Piotr** — Lead developer. Owns OGC-417, OGC-418, OGC-344.
- **Casey** — Out Apr 7-11. Architectural decisions re: GenericFile plugin
  boundary.

---

## Success Criteria

1. All 3 analyzer profiles exist and are validated against real export files
2. Test fixtures created from sanitized real data
3. Unit tests pass for each analyzer's file parsing
4. PlateGridNormalizer works for both Tecan and Multiskan plate grid formats
5. Wondfo comparison operator handling works correctly
6. GenericFile plugin (in plugins submodule) handles all 3 profiles
7. Herbert can configure and upload files for all 3 analyzers on UAT after
   3.2.1.5 deploys
