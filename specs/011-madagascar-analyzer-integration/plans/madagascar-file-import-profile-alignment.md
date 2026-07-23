# Plan — Madagascar Analyzer File-Import: Align Profiles With Real Files

## Context

**Problem**: Madagascar's LA2M lab is onboarding file-based molecular analyzers
(QuantStudio 5, QuantStudio 7 Flex, Bruker Fluorocycler XT) onto OpenELIS via
the analyzer bridge. The bridge polls a watched mount and forwards parsed
results to OE. Three independent gaps are blocking end-to-end ingestion of real
files:

1. **Profiles don't match real files.** The Fluorocycler profile was written
   against a hypothetical template that never landed in Madagascar's exports;
   the QuantStudio profile is missing arbovirus test mappings. Both need
   reconciliation with the actual byte layouts on the mount (captured in
   `docs/debug-local/mnt-snapshot/`).
2. **The OE test catalog is missing HIV VL + arbovirus entries** at the
   Madagascar distro level. `autoCreateTestMappings` in the webapp silently
   drops profile mappings whose LOINC has no matching active test row. The
   `openelis-madagascar-distro` CSV at
   `configs/configuration/backend/tests/example-tests.csv` is the canonical
   layer for site-specific catalog content — not Liquibase.
3. **The bridge's `FileWatcher` can only attach ONE observer per watched
   directory.** Fluorocycler XT writes HIV VL and arbovirus files into the same
   folder, distinguished by filename. The single- observer limitation forces a
   choice of one analyzer per directory, which doesn't match how the lab
   operates.

**What prompted this**: Herbert Yiga's 2026-04-09 incident report where real
files were disappearing from the `/mnt` mount (root cause: the old destructive
bridge) kicked off a containment + forensics workstream that is now shipped
(Phase 1 in history). With the bridge non- destructive, we can safely iterate on
profile correctness against real files without risking more data loss. This plan
picks up from there and gets the three analyzers fully ingesting end-to-end.

**Intended outcome**: video demo through the Playwright `harness-demo-video`
project showing QS5 Arbo, QS7 HIV VL, Fluorocycler HIV VL, and Fluorocycler
Arbovirose files being dropped on a local bridge, parsed, and staged in OE with
correct accessions, test codes, and result values. The demo exercises the
non-destructive invariants (files stay in place, state store shows PROCESSED)
and the multi- observer fix (two Fluorocycler instances on one directory).

**Scope gates** (user direction 2026-04-10):

- Webapp code changes + Liquibase structural changes are IN scope if they're
  git-tracked and land on the existing PRs (no new PRs).
- Site-specific test catalog content (LOINCs, test names) goes into the distro
  CSV, NOT Liquibase.
- Legacy plugins in `plugins/` are reference only — not integrated.
- GeneXpert file path is deferred until ASTM/HL7 primary path fails.

---

## North star

Madagascar's QuantStudio 5, QuantStudio 7 Flex, and Fluorocycler XT instruments
drop real result files into the watched mount. The bridge must parse those files
and forward results into OpenELIS. **Right now the profiles don't match the real
files.** This plan gets them aligned, fixed, and validated end-to-end with a
video demo — in that order, iterating against actual data.

The non-destructive bridge work (Phase 1 in the history section below) is
already shipped. The open work is narrow: profiles → parser nits → test harness
swap → video. Then the other Madagascar analyzers follow the same recipe.

---

## Ground truth as of 2026-04-10

### Real files on `/mnt/la2m/central/analyzers_results/` (Herbert's UAT mount)

Backed up to `/home/ubuntu/madagascar-mnt-backup-20260410-145551/` + `.tar.gz` +
`docs/debug-local/mnt-snapshot/` (gitignored).

| Analyzer                         | File                                                                                                                                                                   | Size   | Sheet + header                                                                                                                      | Data columns                                                                                                                                                 |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **QuantStudio 7 Flex**           | `QuantStudio-7/archive/CVVIH 24 07 2024 serie 02 à valider.xlsx`                                                                                                       | 484 KB | 6 sheets; `Results` sheet header at **row 50** (classic SDS metadata preamble); also has a `Results (2)` sheet with header at row 0 | `Well / Well Position / Omit / Sample Name / Target Name / Task / Reporter / Quencher / CT / Ct Mean / Ct SD / Quantity / Quantity Mean / Quantity SD / ...` |
| **QuantStudio 5**                | `docs/debug-local/Arbo-extraitQS5.xls` (1.1 MB; the `/mnt` copy was deleted by the old bridge — the `.failed` sidecar and Herbert's 2026-04-09 incident report remain) | 1.1 MB | 3 sheets; `Results` sheet header at **row 20**                                                                                      | same column layout as QS7 (multiplex: `Target Name ∈ {CHIKV, DENV, ZIKV}`)                                                                                   |
| **Fluorocycler XT** — HIV VL     | `Fluorocycler-XT/HIV-result.xlsx`                                                                                                                                      | 13 KB  | 1 sheet; header at **row 0**                                                                                                        | `Row / Col / Sample ID / Type / Calc. Conc. / Result` — 95 rows                                                                                              |
| **Fluorocycler XT** — Arbovirose | `Fluorocycler-XT/ARBOVIROSE.xlsx`                                                                                                                                      | 10 KB  | 1 sheet; header at **row 0**                                                                                                        | `Row / Col / Sample ID / Type / Result` — 15 rows (no `Calc. Conc.` column)                                                                                  |

Key observations from the actual bytes:

- **QS5 and QS7 use the IDENTICAL column layout.** They're both Applied
  Biosystems SDS exports; only the metadata preamble length differs (20 rows vs
  50 rows). One profile can handle both.
- **Fluorocycler HIV and Arbovirose use the SAME layout minus one column.** The
  arbovirose file lacks `Calc. Conc.` but is otherwise identical. One profile
  can handle both — the `Calc. Conc.` mapping just needs to be optional.
- **Fluorocycler files have NO test-code / target column.** The assay is implied
  by the file (one file = one assay). Test code must come from analyzer-instance
  configuration in OE, not from a per-row column.
- **Fluorocycler `Result` column is free-text** like
  `"HIV-1 + (CP=38.0)Cont..."` or `"Negative -Not interpretable..."`. The
  numeric concentration is sometimes embedded; sometimes absent. Store raw
  string as an alphanumeric result; do not try to parse.
- **QS `Task` column takes `UNKNOWN` / `STANDARD` / `NTC`**. Only `UNKNOWN` rows
  are clinical; the rest are calibration + controls. The bridge parser must
  filter.

### Current profile files (source of truth: `OpenELIS-Global-2/projects/analyzer-profiles/file/`)

Deployed via the distro mirror at
`openelis-madagascar-distro/configs/analyzer-profiles/file/` which is
bind-mounted into the webapp container at `/data/analyzer-profiles` (see
`AnalyzerRestController.java:1178` for the loader).

#### `quantstudio.json` (`QuantStudio QS5/QS7`, version `1.2.0`)

**Status: 90% correct.** Column mapping matches real files perfectly.
`sheet_detection.strategy=header_scan` + `header_marker="Well"` documents the
right intent. The gaps are:

1. `default_test_mappings` only contains `VIH-1` and `IC`. **Missing `CHIKV`,
   `DENV`, `ZIKV`** for the Arbovirus assay on QS5.
2. No `Task` filtering — the parser will ingest STANDARD and NTC rows alongside
   UNKNOWN, producing calibration-curve noise in the staging table.
3. `findHeaderRow` in the bridge parser has a hardcoded 60-row scan window;
   QS7's CVVIH file header is at row 50 (fits but with only 10 rows of margin).
   Bump to ≥200 for safety.

#### `fluorocycler-xt.json` (`Bruker FluoroCycler XT`, version `1.1.0`)

**Status: completely wrong against the real files.** The profile was written
against a hypothetical "Loris manual Excel template" (see the profile's
`site_workflow.description` — "Loris uploaded the corrected Excel format with
headers on 2026-03-25"). That template never arrived in Madagascar's actual
exports. The profile expects columns
`SampleID / WellPosition / AssayName / TargetName / CP / Interpretation`; the
real files have `Row / Col / Sample ID / Type / [Calc. Conc. /] Result`.

All five `required_columns` are absent from the real files. The parser returns
"no results" every time. **The profile must be rewritten.**

#### `tecan-f50.json` and `multiskan-fc.json`

Also carry
`validation_status: "PENDING - awaiting real export files from site"`. Both
files exist on `/mnt` now. Out of scope for this iteration (follow-on work).

### How profiles flow through the system

1. **Webapp loads**: `AnalyzerRestController.java:1178` reads JSON files from
   `/data/analyzer-profiles`, exposes them to the unified AnalyzerForm dropdown.
   On analyzer-create it calls `AnalyzerServiceImpl.autoCreateTestMappings()` at
   line 377 which consumes `default_test_mappings` + `configDefaults` and
   creates OE `AnalyzerTestMapping` rows (analyzer test code → OE test ID via
   LOINC).

2. **Webapp writes** selected profile fields onto the `Analyzer` entity —
   specifically `importDirectory`, `filePattern`, `columnMappings` (as a Map),
   `fileFormat`, `delimiter`, `skipRows`. **Only these fields propagate.**
   `sheet_detection`, `header_marker`, `required_columns`,
   `default_test_mappings`, `fallback_mapping`, and `column_spec` stay in the
   profile JSON and do NOT reach the bridge.

3. **Bridge pulls**: `AnalyzerRegistryBootstrap.java:117` pulls the subset from
   `/rest/analyzer/analyzers` into `FileConfig.AnalyzerConfig`.

4. **Bridge parses**: `FileMessageHandler.java:150` calls
   `FileResultParser.parseCsv(content, columnMappings, delimiter, skipRows)` for
   CSVs, or `FileResultParser.parse(inputStream, columnMappings)` for Excel.
   **The parser takes ONLY `columnMappings` as a `Map<String, String>`.** It has
   its own hardcoded `resolveSheet` (prefers sheet named `"Results"`, falls back
   to first sheet) and `findHeaderRow` (scans up to 60 rows for a row whose
   first cell is `"Well"`, triggered only when row 0 contains `"Block Type"` or
   `"Experiment Name"`).

5. **Parser emits** `AnalyzerResult` tuples keyed by `sampleId`. Only
   `sampleId`, `testCode`, `result`, `units`, `interpretation`, `qcTask`, and
   `testDate` are extracted from column mappings (see
   `FileResultParser.java:94-101`). **Rows without `testCode` are dropped
   silently** (line 103:
   `if (testCode == null || testCode.isBlank()) continue;`).

### The bridge parser constraints that matter for Madagascar (as of 2026-04-10, after Phase 1)

| Feature the profile needs                                                | Parser supports today?                                                                                                                                     |
| ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `sampleId` from configured column                                        | ✅                                                                                                                                                         |
| `testCode` from per-row column                                           | ✅ required, but fallback to `defaultTestCode` parameter landed (commit `52e9070`)                                                                         |
| `result` as string or number                                             | ✅ auto-detected via `isNumericValue()`                                                                                                                    |
| QC row flagging (STANDARD/NTC/POS/NEG/STD/CALIBRATOR → `isControl=true`) | ✅ landed in `eae654f` — preserved, not dropped, so OE staging UI can filter                                                                               |
| Per-analyzer-instance `defaultTestCode` for files with no target column  | ✅ parser signature landed; bridge-side plumbing still pending (A.3.4)                                                                                     |
| Header row scan window                                                   | ✅ bumped 60 → 200 in `eae654f`                                                                                                                            |
| Result-value fallback `result → ctValue → interpretation`                | ✅ landed in `52e9070`                                                                                                                                     |
| Alternate sheet selection                                                | ⚠️ only `"Results"` preferred, no profile-driven selection — acceptable for QS which has that sheet; not needed for Fluorocycler (single sheet)            |
| `default_test_mappings` lookup                                           | ❌ ignored by the bridge — used only by the webapp for OE `AnalyzerTestMapping` row creation at profile-apply time                                         |
| **Multiple analyzer instances watching the same directory**              | ❌ **KEY BUG** — `FileWatcher` keys observer maps on `Path` alone; re-registering a directory removes the prior observer (lines 223-227). See Phase A.3.7. |

### Test catalog configuration layer (distro CSV, not Liquibase)

`autoCreateTestMappings()` in `AnalyzerServiceImpl.java:377-448` looks up OE
tests via `testService.getActiveTestsByLoinc(loinc)` (line 407) and takes
`tests.get(0)` (line 414) to link an analyzer test code to an OE test. If no
active test row matches the LOINC from `default_test_mappings`, the mapping is
silently skipped with a warning at line 410.

Site-specific test catalog content — including the HIV VL / arbovirus rows
Madagascar needs — is **configured through the distro CSV**, not through
Liquibase. The pipeline:

1. **Distro mounts** `./configs/configuration/` →
   `/var/lib/openelis-global/configuration/` via the `oe.openelis.org` service's
   volumes (see `openelis-madagascar-distro/docker-compose.yml:108`).
2. **Webapp boot** fires `ConfigurationInitializationService.onApplicationEvent`
   (`src/main/java/org/openelisglobal/configuration/service/ConfigurationInitializationService.java:67`).
3. **Domain handlers** run in load-order; `TestConfigurationHandler`
   (`src/main/java/org/openelisglobal/test/service/TestConfigurationHandler.java:60`,
   load order 200) claims `tests/*.csv` via
   `PathMatchingResourcePatternResolver.getResources(...)`.
4. **Filesystem files override classpath.** `collectFiles` at
   `ConfigurationInitializationService.java:170-179` tries
   `file:/var/lib/openelis-global/configuration/backend/tests/*.csv` first; if
   any match, classpath is ignored. So the distro CSV is the single source of
   truth for the Madagascar deployment's test catalog — **no glob filter
   excludes the `example-` filename prefix**; `example-tests.csv` is loaded.
5. **Upsert by normalized description.**
   `TestConfigurationHandler.processCsvLine` at line 289 calls
   `testService.getTestByNormalizedDescription(name)`. If a matching row exists,
   `updateTest` at line 341 updates `test.setLoinc(loinc)` in place. If not,
   `createTest` at line 390 inserts a new row. Either way, LOINCs land in
   `clinlims.test.loinc` at container startup.
6. **Current distro CSV content**:
   `openelis-madagascar-distro/configs/configuration/backend/tests/example-tests.csv`
   has 17 generic rows — CBC, glucose, HIV Rapid Test (serology, not PCR),
   malaria RDT, etc. **No HIV VL / DENGUE / CHIKV / ZIKV PCR entries.** That's
   the gap Phase A.0 fills.

**Implication:** for any new analyzer LOINC to resolve through
`autoCreateTestMappings`, the corresponding test row must exist in the catalog
with the same LOINC. The distro CSV is the right layer to add it — **not** a new
Liquibase changeset in `OpenELIS-Global-2`. Liquibase changesets in
`src/main/resources/liquibase/2.3.x.x/new_tests.xml` provide the baseline seed
that ships with the webapp image; deployment-specific tests belong in the
distro.

### E2E harness state

- `frontend/playwright/tests/demo/harness/file-import-results.spec.ts` already
  has `FILE_IMPORT_SCENARIOS` entries for `quantstudio-7`, `quantstudio-5`,
  `fluorocycler-xt` (via mock template `"hain_fluorocycler"`), plus others.
- Scenarios use `dropFixtureViaMock` which POSTs to
  `${MOCK_API_URL}/simulate/file/{mockTemplate}`. The mock server copies a
  fixture into the watch directory **and pre-parses it itself** returning canned
  `metadata.results` that the test asserts against. The real bridge parser does
  run against the same dropped file, but the test succeeds on the mock's
  metadata — so a real parser failure doesn't fail the test.
- Mock fixtures live at `tools/analyzer-mock-server/fixtures/` with synthetic
  `results.xlsx`/`results.xls`/`results.csv` files that are stripped-down
  versions: single sheet, header at row 0, no metadata preamble, hand-crafted to
  pass. **They do not look like real files.**
- Playwright config (`frontend/playwright.config.ts` line 140) has a
  `harness-demo-video` project that runs the harness specs with `video: "on"`
  for video generation.

---

## End-user flow for configuring a file-based analyzer

The admin workflow (already implemented in the unified AnalyzerForm +
`FileImportServiceImpl.autoCreateFromProfile`) is:

1. Open unified AnalyzerForm in OE UI
2. Enter the analyzer name and select the **profile** from a dropdown (sourced
   from `projects/analyzer-profiles/file/*.json`)
3. Profile auto-fills: `fileFormat`, `delimiter`, `hasHeader`, `skipRows`,
   `columnMappings`, `filePattern`, and (NEW) `defaultTestCode`
4. Admin enters the two workflow-specific fields:
   - `importDirectory` — the watched path for this analyzer
   - `filePattern` override if the profile's default doesn't match
5. Admin clicks **Test Connection** — verifies the directory is accessible
   (exists, readable by the bridge container)
6. Admin saves — webapp creates the Analyzer entity + calls
   `autoCreateTestMappings()` which creates `AnalyzerTestMapping` rows from the
   profile's `default_test_mappings`
7. Bridge picks up the new analyzer via `AnalyzerRegistryBootstrap`'s scheduled
   refresh (or an explicit push endpoint), starts polling the directory, parses
   dropped files, forwards results to OE

**The admin never types a test code, LOINC, column name, or assay label.**
Everything beyond directory + pattern is profile-driven.

---

## Common file-ingestion config pattern (2026-04-10 simplification)

Every file profile uses the SAME schema. One mechanism, no per-analyzer special
cases, no entity schema proliferation.

```jsonc
{
  "profileMeta": { "id": "...", "displayName": "...", "version": "..." },
  "protocol": { "name": "FILE", "format": "EXCEL|CSV" },
  "supported_extensions": [".xls", ".xlsx"],

  // How to read the file — maps spreadsheet column → semantic field
  "column_mapping": {
    "Sample Name": "sampleId",
    "Target Name": "testCode", // present on QS; absent on Fluorocycler
    "Quantity Mean": "result",
    "CT": "ctValue",
    "Task": "qcTask"
  },

  // File-wide assay when the file has no per-row test-code column.
  // Parser falls back to this when a row yields no testCode from column_mapping.
  // Optional: QS profiles omit it (TargetName column exists per row).
  "default_test_code": "VIH-1",

  // Test mappings consumed by webapp's autoCreateTestMappings() at profile-apply
  // time to pre-populate OE AnalyzerTestMapping rows.
  "default_test_mappings": [
    {
      "test_code": "VIH-1",
      "loinc": "20447-9",
      "test_name_hint": "HIV-1 Viral Load"
    }
  ],

  "configDefaults": { "fileFormat": "EXCEL", "hasHeader": true }
}
```

**QC handling is already done.** The parser flags STANDARD / NTC / CPOS / CNEG /
CONTROL / STD / CALIBRATOR rows as `isControl=true` and passes them through to
OE (bridge commit `eae654f` on PR #34). OE's staging UI filters by `isControl`.
**Rows are preserved, not dropped** — the tech can review QC curves if needed
but they don't pollute clinical results by default.

**File-wide assay routing via `default_test_code`** is the one new field needed
on the Analyzer entity — a single VARCHAR column. The webapp's profile-apply
flow writes it from the profile JSON, the REST serializer forwards it, the
bridge's `AnalyzerRegistryBootstrap` reads it into `AnalyzerConfig`, and
`FileResultParser.parse()` uses it as a fallback when a row has no per-row
testCode. No new form fields, no new entity schema proliferation, same plumbing
pattern as the existing `columnMappings` field. One Liquibase add-column
changeset, ~5 lines of wiring per layer.

**One profile per machine — profile captures the union of test types the machine
might produce.** Refined 2026-04-10 per Piotr: profiles are machine-specific,
not assay-specific. A Fluorocycler XT can run HIV VL _and_ Arbovirus, and a
single profile captures both via its `default_test_mappings` list (which the
webapp's `autoCreateTestMappings` walks to create `AnalyzerTestMapping` rows for
every test the machine can emit). When the lab runs multiple assays on the same
machine type, the admin creates **multiple analyzer instances** sharing the same
profile — each instance has its own `importDirectory` / `filePattern` /
`defaultTestCode`, distinguishing which assay that particular instance is
configured for. The profile itself is single-source-of-truth for the machine.

Example for Fluorocycler XT:

- **One profile**: `projects/analyzer-profiles/file/fluorocycler-xt.json`
  - `column_mapping`: one mapping covering the real file layout (`Sample ID` /
    `Type` / `Calc. Conc.` / `Result`)
  - `default_test_mappings`: union of all test codes Fluorocycler XT can emit —
    `VIH-1` (HIV VL) + `ARBO-PANEL` (Arbovirus multiplex) + any future assay
- **Two analyzer instances in OE (typical lab setup)**:
  - `Fluorocycler XT — HIV VL` →
    `importDirectory=/mnt/.../Fluorocycler-XT/hiv/`, `defaultTestCode=VIH-1`
  - `Fluorocycler XT — Arbovirose` →
    `importDirectory=/mnt/.../Fluorocycler-XT/arbo/`,
    `defaultTestCode=ARBO-PANEL`
- The admin picks the profile once, then chooses the assay for each instance in
  the AnalyzerForm (plain text input or small dropdown sourced from the
  profile's `default_test_mappings`). The `autoCreateTestMappings` flow ensures
  OE has `AnalyzerTestMapping` rows for BOTH assays, so either instance's
  results map cleanly to OE tests.

For QuantStudio (where `TargetName` is per-row), the admin leaves
`defaultTestCode` blank. The parser uses the per-row `testCode`. Single profile,
single analyzer instance covers every assay the file contains.

---

## Phase A — Align the QS5/QS7/Fluorocycler profiles with real files

**Goal**: one commit per artifact; each commit updates a single file (profile
JSON, distro CSV, or bridge Java source); each commit gets an accompanying
manual-smoke run through the bridge against the real file before moving on.

### A.0 — Extend the distro test catalog CSV with HIV VL + arbovirus rows

**Why first**: the profile LOINC mappings (A.1 / A.2) can only create
`AnalyzerTestMapping` rows when `getActiveTestsByLoinc` finds a matching test.
The catalog is fed by
`openelis-madagascar-distro/configs/configuration/backend/tests/example-tests.csv`
at webapp boot via `TestConfigurationHandler`. Add the missing rows there
_first_ so A.1 / A.2 don't land in a broken state.

**Upsert strategy**: use `testName` values that match the existing baseline rows
(from webapp Liquibase `new_tests.xml`) so the handler's
`getTestByNormalizedDescription` lookup UPDATES in place instead of creating
duplicate rows. CHIKV and ZIKV have no baseline rows — those are fresh inserts.

Edit
`openelis-madagascar-distro/configs/configuration/backend/tests/example-tests.csv`
to append these rows (preserve the existing header
`testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,englishName,frenchName`):

```csv
HIVVIRALLOAD,Molecular Biology,Plasma|Serum|DBS,20447-9,Y,Y,50,copies/mL,HIV-1 Viral Load,Charge Virale VIH-1
DENGUEPCR,Molecular Biology,Plasma|Serum,7855-0,Y,Y,60,,Dengue PCR,Dengue PCR
CHIKVPCR,Molecular Biology,Plasma|Serum,60260-7,Y,Y,61,,Chikungunya PCR,Chikungunya PCR
ZIKVPCR,Molecular Biology,Plasma|Serum,85622-9,Y,Y,62,,Zika PCR,Zika PCR
```

**Canonical LOINCs (internet-research validated via loinc.org, 2026-04-10)**:

- **`20447-9`** — "HIV 1 RNA [#/volume] (viral load) in Serum or Plasma by NAA
  with probe detection". Upgrades baseline `10351-5` (set by Liquibase changeSet
  670 at `new_tests.xml:2109`) to the more specific NAA-with-probe code. Matches
  QS / Fluorocycler HIV VL runs.
- **`7855-0`** — "Dengue virus 1+2+3+4 RNA [Presence] in Serum by NAA with probe
  detection". Pan-serotype dengue code — matches a multiplex that doesn't
  subtype. **Already the baseline LOINC** for `DENGUEPCR(%)` via Liquibase
  changeSet 665 at `new_tests.xml:2102`, so this CSV row is a no-op upsert for
  catalog state but makes the profile mapping explicit.
- **`60260-7`** — "Chikungunya virus RNA [Presence] in Serum or Plasma by NAA
  with probe detection". New test row — no baseline.
- **`85622-9`** — "Zika virus RNA [Presence] in Serum or Plasma by NAA with
  probe detection". New test row — no baseline.

**Why individual codes, not the panel code**: Madagascar's QS5 Arbo file emits
one result row per (sample, target) — each target carries its own CT value and
positive/negative interpretation. The techs need to see "this patient is CHIKV+,
DENV−, ZIKV−" with each target distinctly mapped to the correct LOINC. The panel
code `81154-7` ("Dengue and Chikungunya and Zika virus panel by NAA with probe
detection") is clinically correct for a single combined panel observation but
semantically wrong when each individual target result is stored as its own FHIR
`Observation.code`. Individual LOINCs match reality and result granularity.

**Mapping determinism**: because each target has a unique LOINC,
`autoCreateTestMappings` at `AnalyzerServiceImpl.java:407` calls
`testService.getActiveTestsByLoinc(loinc)` and the result set contains only the
rows for ONE test (across its sample-type variants). The `tests.get(0)` at line
414 deterministically returns the right test for each target — **no
disambiguation patch required**. One OE test row variant per
`(test, sampleType)`, but all share the same test id for mapping purposes;
sample-type resolution happens downstream at order- matching time.

**Deployment note**: after editing the CSV, the handler's checksum tracking will
detect the change on next webapp boot and reprocess the file. The checksum lives
at `/var/lib/openelis-global/configuration/backend/tests-checksums.properties`
(inside the container volume). No manual cleanup needed — the handler
re-computes and rewrites it when the CSV content hash differs from the stored
checksum (see `ConfigurationInitializationService.loadDomainConfiguration:134`).

### A.1 — QuantStudio profile (surgical arbovirus mapping addition)

Edit `projects/analyzer-profiles/file/quantstudio.json`:

- **Add three entries** to `default_test_mappings` with the individual canonical
  LOINCs from A.0:
  ```
  { "test_code": "CHIKV", "test_name_hint": "Chikungunya PCR", "loinc": "60260-7", "unit": "" },
  { "test_code": "DENV",  "test_name_hint": "Dengue PCR",      "loinc": "7855-0",  "unit": "" },
  { "test_code": "ZIKV",  "test_name_hint": "Zika PCR",        "loinc": "85622-9", "unit": "" }
  ```
- **Update existing VIH-1 entry** LOINC to `20447-9` if not already set.
  (Baseline has `10351-5`; A.0 upserts to `20447-9`; profile should point at
  whichever the catalog ends up with.)
- **No column_mapping changes.** The `column_mapping` already matches the real
  files (`Sample Name → sampleId`, `Target Name → testCode`,
  `Quantity Mean → result`, `CT → ctValue`, `Task → qcTask`). The
  `sheet_detection` block is documentation only — the bridge parser doesn't read
  it yet, but Phase A.3.1 already bumped `findHeaderRow` scan window to 200 rows
  which covers QS7's row-50 header.

Mirror into
`openelis-madagascar-distro/configs/analyzer-profiles/file/quantstudio.json`.

### A.2 — Fluorocycler profile (rewrite `column_mapping`)

Edit `projects/analyzer-profiles/file/fluorocycler-xt.json`:

Replace the entire profile body with a config that matches the REAL
Fluorocycler-XT file layout from `/mnt`:

```json
{
  "profileMeta": {
    "id": "fluorocycler-xt",
    "version": "2.0.0",
    "displayName": "Bruker FluoroCycler XT",
    "manufacturer": "Bruker"
  },
  "category": "MOLECULAR",
  "protocol": { "name": "FILE", "format": "EXCEL" },
  "supported_extensions": [".xlsx", ".xls"],
  "site_workflow": {
    "description": "Fluorocycler XT at LA2M exports single-sheet XLSX files with plate-position columns (Row/Col), Sample ID, Type (Standard/Unknown/Positive/Negative), optional Calc. Conc. (HIV VL only), and a free-text Result column carrying the clinical interpretation. One file = one assay; the assay is determined by the analyzer-instance's filePattern + defaultTestCode in OE (HIV VL vs Arbovirus), not by a per-row column.",
    "native_export": "PDF (tech exports Excel via FluoroSoftware result copy)",
    "import_format": "Single-sheet XLSX, headers at row 0"
  },
  "column_mapping": {
    "Sample ID": "sampleId",
    "Row": "row",
    "Col": "col",
    "Type": "qcTask",
    "Calc. Conc.": "result",
    "Result": "interpretation"
  },
  "required_columns": ["Sample ID", "Result"],
  "optional_columns": ["Row", "Col", "Type", "Calc. Conc."],
  "default_test_mappings": [
    {
      "test_code": "VIH-1",
      "test_name_hint": "HIV-1 Viral Load",
      "loinc": "20447-9",
      "unit": "copies/mL"
    },
    {
      "test_code": "CHIKV",
      "test_name_hint": "Chikungunya PCR",
      "loinc": "60260-7",
      "unit": ""
    },
    {
      "test_code": "DENV",
      "test_name_hint": "Dengue PCR",
      "loinc": "7855-0",
      "unit": ""
    },
    {
      "test_code": "ZIKV",
      "test_name_hint": "Zika PCR",
      "loinc": "85622-9",
      "unit": ""
    }
  ],
  "configDefaults": {
    "fileFormat": "EXCEL",
    "hasHeader": true,
    "sheetIndex": 0
  }
}
```

Key column-mapping decisions:

- **`Calc. Conc. → result`** (not `concentration`) so HIV VL numeric quantity
  lands in the result column directly. Arbovirus files have no `Calc. Conc.`;
  parser's result-value fallback (`result → ctValue → interpretation`, commit
  `52e9070`) picks up the `Result` column's free-text interpretation
  automatically, so the row still produces a value.
- **`Result → interpretation`** so the free-text clinical narrative
  (`"HIV-1 + (CP=38.0)Cont..."` / `"Negative -Not interpretable..."`) is
  preserved as the result's interpretation field. The parser stores alphanumeric
  values verbatim.
- **`Type → qcTask`** so the isControl flagging (landed in `eae654f`) catches
  Standard / Positive / Negative / NTC rows as controls and passes them to OE
  with `isControl=true`, for staging-UI filtering.

Mirror into
`openelis-madagascar-distro/configs/analyzer-profiles/file/fluorocycler-xt.json`.

**Design point resolved**: file-wide assay routing via `defaultTestCode` is the
chosen approach. The parser signature for `defaultTestCode` already landed in
commit `52e9070` (task #18 ✅). What's pending is the end-to-end plumbing webapp
→ bridge → parser — covered in Phase A.3.4 and A.3.5 below.

### A.3 — Bridge parser + plumbing fixes

Five work items. Three are landed on `openelis-analyzer-bridge` PR #34
(parser-side); two are pending on both the bridge and the webapp.

**Landed in PR #34:**

1. ✅ **DONE** (`eae654f`, task #16) — bump `findHeaderRow` scan window 60 → 200
   to cover QS7's row-50 header with margin.

2. ✅ **DONE** (`eae654f`, task #17) — flag `STANDARD` / `NTC` / `STD` /
   `CALIBRATOR` / `CONTROL` / `POSITIVE` / `NEGATIVE` as controls in
   `isControlRow`. QC rows are preserved in OE with `isControl=true` for the
   staging UI to filter (rows are never dropped). Satisfies the "QC handled and
   passed to OE properly" requirement.

3. ✅ **DONE** (`52e9070`, task #25) — result-value fallback chain
   `result → ctValue → interpretation`. A single QuantStudio profile handles
   both HIV VL (Quantity Mean populated) and Arbovirus (CT populated, Quantity
   Mean empty). Also unlocks Fluorocycler's `Result → interpretation` free-text
   column as a result value.

4. ✅ **DONE** (task #18) —
   `FileResultParser.parse(InputStream, Map, String defaultTestCode)` and
   `parseCsv(..., String defaultTestCode)` overloads added. Parser-side
   fallback:
   ```java
   if (testCode == null || testCode.isBlank()) {
       if (defaultTestCode != null && !defaultTestCode.isBlank()) {
           testCode = defaultTestCode.trim();
       } else {
           continue;
       }
   }
   ```

**Pending in PR #34 (bridge side):**

5. **PENDING (task #19)** — plumb `defaultTestCode` through the bridge:
   - `FileConfig.AnalyzerConfig` gains a `defaultTestCode` field
     (`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileConfig.java`).
   - `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/startup/AnalyzerRegistryBootstrap.java:117`
     reads `defaultTestCode` from the webapp's `/rest/analyzer/analyzers`
     response and writes it into `AnalyzerConfig`.
   - `FileMessageHandler.processFile` at
     `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileMessageHandler.java:150`
     reads the config for the matched analyzer and passes its `defaultTestCode`
     into the landed parser overload.
   - Unit tests extending `FileResultParserTest.MadagascarRealFiles` to assert
     Fluorocycler HIV VL drops the VIH-1 default onto rows missing per-row
     testCode.

**Pending in PR #3372 (webapp side):**

6. **PENDING (task #20)** — webapp entity surgery for `defaultTestCode` (single
   VARCHAR column, no Liquibase changeset in `OpenELIS-Global-2` baseline — see
   below for why):
   - Add `defaultTestCode` field to `Analyzer.java` valueholder
     (`src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java`)
     with Hibernate `@Column(name = "default_test_code", length = 50)`.
   - **Liquibase changeset in `OpenELIS-Global-2` IS appropriate here** because
     the schema change is a baseline structural addition to the
     `clinlims.analyzer` table — it ships with the webapp image for every
     deployment. This is different from test catalog LOINCs (A.0) which are
     site-specific CONTENT and belong in the distro CSV. Add
     `013-add-analyzer-default-test-code.xml` as a new include in
     `src/main/resources/liquibase/2.3.x.x/base.xml`.
   - `AnalyzerRestController` create/update wiring to copy `defaultTestCode`
     from the form DTO onto the entity, and include it in the
     `/rest/analyzer/analyzers` response map at line 1087.
   - `BridgeRegistrationService.registerFile` signature grows a
     `defaultTestCode` parameter
     (`src/main/java/org/openelisglobal/analyzer/service/BridgeRegistrationService.java:73`);
     callers at `FileImportServiceImpl.java:153`,
     `AnalyzerBridgeStartupRegistrar:115`, and `AnalyzerRestController:1087`
     updated accordingly.
   - `FileImportServiceImpl.autoCreateFromProfile` at
     `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java:38`
     reads `default_test_code` from the profile JSON's top level and writes it
     onto the newly-created Analyzer entity before
     `analyzerService.update(analyzer)` at line 149.
   - **No form UI changes.** The field is set automatically when a profile is
     selected. If the profile has no `default_test_code`, the entity's field
     stays NULL and the parser drops rows without per-row testCode (current
     behavior).

### A.3.7 — Fix bridge multi-observer limitation (KEY)

**Status**: PENDING. **Scope**:
`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileWatcher.java`.

**Problem**: `FileWatcher` keys all directory-scoped state by `Path` alone —
`observersByDirectory`, `directoryAnalyzerMap`, `directoryGlobPatternByDir` are
all `Map<Path, ...>` at lines 55-58. When a second analyzer is registered with
the same `importDirectory`, `registerDirectoryInternal` at lines 223-227
explicitly REMOVES the existing observer:

```java
// Remove existing observer if re-registering
FileAlterationObserver oldObserver = observersByDirectory.remove(dirPath);
if (oldObserver != null && monitor != null) {
    monitor.removeObserver(oldObserver);
}
```

…then puts the new observer in the same slot. Result: registering a Fluorocycler
HIV analyzer after a Fluorocycler Arbo analyzer at `/mnt/.../Fluorocycler-XT/`
silently deactivates the Arbo one. Only one glob pattern + one analyzerId are
honored per directory.

**Why this blocks Madagascar**: Herbert's `/mnt/la2m/central/analyzers_results/`
layout has a single directory per machine, with one machine running multiple
assays (Fluorocycler XT does HIV VL and Arbovirus in the same output folder).
The distinguishing feature between the two runs is `filePattern` (`HIV*.xlsx` vs
`ARBO*.xlsx`). The bridge must be able to register TWO analyzers against the
same dir with different patterns and different `defaultTestCode` values. Without
this fix, the Fluorocycler workflow can't be configured the way the lab actually
operates.

**Apache Commons IO supports this natively** (internet-research validated
2026-04-10): `FileAlterationMonitor` uses a `CopyOnWriteArrayList` internally
and accepts any number of `FileAlterationObserver` instances via
`addObserver(observer)`. Each observer has its own `IOFileFilter` (glob) and its
own listener. The bridge's current single-observer-per- directory constraint is
artificial — it comes from the data-structure choice, not the library.

**Fix**:

1. Replace `Map<Path, FileAlterationObserver> observersByDirectory` +
   `Map<Path, String> directoryAnalyzerMap` +
   `Map<Path, String> directoryGlobPatternByDir` with a single
   `Map<Path, List<WatchRegistration>> registrationsByDirectory` where
   `WatchRegistration` is a small record:

   ```java
   record WatchRegistration(
       String analyzerId,
       String globPattern,
       FileAlterationObserver observer
   ) {}
   ```

2. `registerDirectoryInternal(Path dirPath, String analyzerId, ...)` stops
   removing existing observers at the dirPath level. Instead it APPENDS a new
   `WatchRegistration` to the list, each with its own observer / filter /
   listener. Only an identical `(dirPath, analyzerId)` pair triggers a replace.

3. `removeWatchDirectory(Path)` (currently line 169) splits into:

   - `removeWatchRegistration(Path dirPath, String analyzerId)` — remove a
     single analyzer's registration, keeping others alive
   - `removeAllWatchRegistrations(Path dirPath)` — remove all at dirPath (used
     during shutdown)

4. `determineAnalyzerId(Path)` must return the analyzerId whose glob matches the
   file. The file's `Path` is matched against each registration's glob in the
   parent directory's list; the first match wins. If no glob matches, the file
   is ignored for that directory. This replaces the current "first analyzer
   wins" behavior which was OK when only one was ever registered.

5. `shouldProcessFile(Path)` and the observer's `IOFileFilter` already delegate
   to glob matching — the IOFileFilter closure in `registerDirectoryInternal`
   captures the correct glob per registration, so per-observer filtering works
   without listener changes. The listener still calls
   `trackFileForStability(Path)` on create/change; stability tracking is
   file-scoped, not registration- scoped, so no changes there.

6. `rescanAllDirectories()` at line 420 iterates
   `registrationsByDirectory.keySet()` and, for each dir, walks files and asks
   each registration's glob whether it applies. Files that match multiple
   registrations are deduped by `(analyzerId, contentHash)` in the state store,
   which already handles that cleanly.

**Tests to add** (extending
`tools/openelis-analyzer-bridge/src/test/java/org/itech/ahb/file/FileWatcherTest.java`
or a new `MultiObserverTest`):

- Register two analyzers against the same temp directory with different glob
  patterns; drop one matching-A file and one matching-B file; assert both
  processors observe their respective file, and neither observes the other.
- Remove analyzer A's registration; assert analyzer B's processor still observes
  B files after the removal.
- Register the same `(analyzerId, dirPath)` twice; assert the second call
  replaces (not appends) so there's no double-processing of the same analyzer.
- File-stability tracker is shared but state store dedupe prevents double-POST:
  assert a single file matching BOTH analyzers' globs (edge case) gets posted
  under both analyzer IDs, with independent `(analyzerId, contentHash)` state
  rows.

**Why this is KEY** (user direction 2026-04-10): without this fix,
Fluorocycler's one-directory-two-assays workflow requires either a directory
split at the filesystem layer (not under our control — the lab chooses folder
layout) OR operator-maintained file moves (defeats the non-destructive
contract). The multi-observer fix is the only way to honor "one profile per
machine, multiple analyzer instances per assay" when the machine writes all runs
into one folder.

### A.4 — Manual iteration against real files

For each profile (QS then Fluorocycler HIV then Fluorocycler Arbo):

1. Drop the real file into the watched directory locally.
2. Inspect the bridge log for `FileResultParser` warnings (header row not found,
   no results extracted, etc.).
3. Inspect the bridge state store via `GET /admin/file-state` to confirm
   `PROCESSED` status.
4. Inspect the OE `AnalyzerResults` staging table for the expected accessions
   and test codes.
5. Fix the profile or parser iteratively until results land correctly.
6. Commit each successful iteration; do not batch failures.

---

## Phase B — Generate format-grounded template fixtures and wire to harness

**Goal**: the `harness-demo-video` Playwright project must generate videos that
show the **real analyzer file format** being ingested correctly — not synthetic
stubs that short-circuit through the mock, and not anonymized real patient data.
The CI dataset is a **format-grounded, fully-fabricated template**: its
sheet/column layout mirrors the real analyzer exports byte-for-byte (sheet
count, sheet names, metadata preamble length, column headers, data types), but
every cell value is synthetic (fake sample IDs like `DEV0126*`, fabricated CT
values, made-up experiment names). This gives us full coverage of the parser's
real-format code paths without shipping any real lab data into the repo.

### B.1 — Write a fixture generator script

Script: `tools/analyzer-mock-server/scripts/generate-fixture.py` (doesn't yet
exist — add it).

The generator writes XLSX/XLS/CSV files from scratch using `openpyxl` (and
`xlwt` if we keep any `.xls` fixtures) to match each analyzer's observed format
exactly:

- **QuantStudio 7 (HIV VL)**: 6 sheets (Amplification Data, Results, Melt Curve
  Raw Data, Multicomponent Data, Raw Data, Results (2)), `Results` sheet has a
  **~50-row metadata preamble** (Experiment Name, Experiment Type, Run Start
  Date, etc. — all fabricated) followed by the column header row matching the
  real file's
  `Well / Well Position / Omit / Sample Name / Target Name / Task / Reporter / Quencher / CT / Ct Mean / Ct SD / Quantity / Quantity Mean / Quantity SD / ...`.
  Data rows include a mix of `Task=UNKNOWN`, `Task=STANDARD`, `Task=NTC`, plus
  `Target Name=VIH-1` and `Target Name=IC`.
- **QuantStudio 5 (Arbovirus)**: same column layout, 3 sheets, `Results` sheet
  header at row ~20 (shorter preamble). Data rows have
  `Target Name ∈ {CHIKV, DENV, ZIKV}` with `Task=UNKNOWN` for clinical rows plus
  STANDARD/NTC control rows. Every sample has three rows (one per target).
- **Fluorocycler XT (HIV VL)**: 1 sheet, header at row 0, columns
  `Row / Col / Sample ID / Type / Calc. Conc. / Result`. Data includes
  `Type=Standard` calibration rows, `Type=Positive`/`Type=Negative` controls,
  and `Type=Unknown` clinical rows. `Result` column carries fabricated free-text
  like `"HIV-1 + (CP=38.0)"`, `"Negative"`.
- **Fluorocycler XT (Arbovirose)**: 1 sheet, header at row 0, columns
  `Row / Col / Sample ID / Type / Result` (NO `Calc. Conc.` column — this is the
  edge case the parser's result-value fallback must handle). Data is a mix of
  positive/negative rows with fabricated free-text results.

Key properties of the generator:

1. **Deterministic**: same RNG seed produces the same fixture bytes every run.
   The committed fixtures stay byte-identical unless the generator is rerun with
   updated params.
2. **Grounded in observed format**: the generator reads
   `docs/debug-local/mnt-snapshot/` (the real file inventory, gitignored) at
   development time to cross-check its output shape against reality — sheet
   count, preamble length, column order — but emits NO real cell values.
3. **Idempotent**: re-running the generator overwrites the existing fixtures in
   place with byte-identical output, so diffs are trivial to review.
4. **Edge-case coverage**: the generator can emit variants not necessarily
   present in any one real file — e.g., an arbovirus fixture with one positive
   target and two negatives, or an HIV VL fixture with a sample at exactly the
   detection limit — driven by CLI flags.

Commit the generator script and the generated fixtures under
`tools/analyzer-mock-server/fixtures/`:

```
fixtures/quantstudio7/hiv-vl.xlsx      # 6 sheets, 50-row preamble, Results sheet
fixtures/quantstudio5/arbo.xls         # 3 sheets, 20-row preamble, Results sheet
fixtures/fluorocycler-xt/hiv-vl.xlsx   # 1 sheet, row-0 header, Calc. Conc. present
fixtures/fluorocycler-xt/arbo.xlsx     # 1 sheet, row-0 header, no Calc. Conc.
```

**Delete the existing synthetic fixtures** (the ones that were hand-crafted
single-sheet stubs with row-0 headers and no metadata preamble). They're the
wrong shape — they let tests pass against the mock's pre-parsed metadata while
the real parser would fail. The generator-produced fixtures replace them as the
single source of truth for both fast-CI and the video project. **One fixture
set, one truth.**

Each fixture sits next to a hand-curated `expected.json`:

```
fixtures/quantstudio7/hiv-vl.xlsx
fixtures/quantstudio7/hiv-vl.expected.json
fixtures/quantstudio5/arbo.xls
fixtures/quantstudio5/arbo.expected.json
...
```

`expected.json` lists the accession IDs, test codes, result values, `isControl`
flags, and test dates the bridge parser should emit for its paired fixture —
derived by running the fixture through the parser once manually and recording
the output, then committing that as the assertion source of truth. Regenerating
the fixture without updating `expected.json` will fail the tests, which is the
desired tight feedback loop.

### B.2 — Update the mock-server simulate route to pass-through, not pre-parse

In `tools/analyzer-mock-server/api.py`, the existing
`POST /simulate/file/{template}` route **currently pre-parses the fixture** and
returns canned `metadata.results` that the test asserts against. This hides real
bridge-parser failures. Replace the handler with a strict passthrough:

- Copy the format-grounded fixture from `fixtures/{templatePath}.(xlsx|xls|csv)`
  into the target directory inside the mock server's `/data/analyzer-imports/`.
- **Do NOT pre-parse.** Return
  `{status: "copied", written_path: ..., expected: <contents of the paired .expected.json>}`.
- Test asserts against `expected` (hand-curated assertion source) and against
  the OE staging table / UI — never against a mock-server parse.

The bridge parser is the thing under test here. If parsing fails, the test must
fail.

### B.3 — Update `file-import-results.spec.ts`

Rewrite the `FILE_IMPORT_SCENARIOS` entries for `quantstudio-7`,
`quantstudio-5`, and `fluorocycler-xt`:

- Point `mockTemplate` at the new single-source fixture paths (e.g.,
  `quantstudio7/hiv-vl`, `fluorocycler-xt/arbo`).
- Update the `dropFixtureViaMock` helper to expect the passthrough response
  shape (no `metadata.results` field; instead `expected` payload from the paired
  `.expected.json`).
- Change the assertion to compare the OE UI / REST output against the
  passthrough's `expected` payload.
- Add a new scenario for `fluorocycler-xt-arbo` (the no-`Calc. Conc.` arbovirus
  variant).
- Two Fluorocycler scenarios run against the SAME target directory to exercise
  the multi-observer fix (A.3.7): scenarios are keyed on separate analyzer
  instances with different `filePattern` values (`*hiv*.xlsx` vs `*arbo*.xlsx`).

### B.4 — Add post-success invariants to the specs

Per Phase 1's non-destructive contract:

- After ingestion, assert the file **still exists** in the watched directory
  (HTTP GET to a mock-server `/exists` endpoint).
- Assert no `.error` or `.failed` sidecar files were written.
- Assert the bridge state store has a `PROCESSED` row for the file's content
  hash (via `GET /admin/file-state/{analyzerId}/{hash}`).

These are the gates Phase 1's refactor promised; Phase B wires them into the
test harness so regressions are caught.

### B.5 — Generate videos

Run locally:

```
cd frontend && npm run pw:test -- --project=harness-demo-video
```

Videos land in `playwright-report/` or similar; attach to PR #3372 as evidence.

---

## Phase C — Iterate the same recipe on the other Madagascar analyzers

Once QS5/QS7/Fluorocycler are green, the same steps apply to:

- **Tecan Infinite F50** — real file
  `ELISA reader Tecan Infinite F50/INFINITE F50 MAGELLAN.xlsx` is a plate-map
  format with French locale (`Plan_plaque`, `DO_palque`). Requires a new
  plate-map parser on the bridge side. **New work**, not a profile tweak —
  separate PR.
- **Thermo Multiskan FC** — two real files
  (`MULTISCAN FC THERMOSCIENTIFIC.xlsx` + `temporarySkanitExport ....xlsx`),
  both plate-map shaped. Reuses the Tecan plate-map parser with different
  profile config.
- **GeneXpert Dx** (×4 instruments) — real files are UTF-16 CSV with a
  structured section format. **Deferred until the ASTM/HL7 transport path
  fails** (per user direction 2026-04-10). Primary GeneXpert integration is ASTM
  via existing HL7/ASTM bridge handlers; the file path is fallback only.
- **Invitrogen Attune CytPix** — real file is a PDF. **Out of scope** for the
  bridge; separate workflow.
- **DT-prime** — no exports yet; skeleton directory. Parked.

---

## Commit cadence

Per earlier user direction: commit and push frequently to the **existing** PRs.
No new PRs. Each commit should be a single self-contained slice of one of the
phases below.

- **`openelis-analyzer-bridge` PR #34** — Phase A.3 parser + plumbing (three
  commits already landed: `eae654f`, `52e9070`, defaultTestCode parser
  overloads). Remaining:
  - Commit: A.3.5 `FileConfig.AnalyzerConfig` + `AnalyzerRegistryBootstrap`
    - `FileMessageHandler` plumbing for `defaultTestCode`
  - Commit: A.3.7 `FileWatcher` multi-observer refactor + tests
- **`OpenELIS-Global-2` PR #3372** — profile JSONs + webapp entity + REST
  wiring + Playwright harness:
  - Commit: A.1 `projects/analyzer-profiles/file/quantstudio.json` — arbovirus
    mappings with individual LOINCs `60260-7` / `7855-0` / `85622-9`
  - Commit: A.2 `projects/analyzer-profiles/file/fluorocycler-xt.json` — rewrite
    against real layout with `default_test_code` support
  - Commit: A.3.6 webapp `defaultTestCode` field — `Analyzer.java` valueholder +
    `013-add-analyzer-default-test-code.xml` Liquibase +
    `AnalyzerRestController` + `BridgeRegistrationService.registerFile`
    signature + `FileImportServiceImpl.autoCreateFromProfile` update
  - Commit: Submodule pointer bumps — one for `tools/openelis-analyzer-bridge`
    pointing at the A.3.5 + A.3.7 commits, one for `tools/analyzer-mock-server`
    after B.1/B.2
  - Commit: B.3 Playwright spec update
    (`frontend/playwright/tests/demo/harness/file-import-results.spec.ts`)
    - B.4 post-success invariants
- **`analyzer-mock-server` submodule** — B.1 generator script + format-grounded
  template fixtures (delete old synthetic fixtures) + B.2 passthrough simulate
  route. Submodule PR lands first, then `OpenELIS-Global-2` PR #3372 picks up
  the new pointer.
- **`openelis-madagascar-distro` PR #4** — distro CSV + profile mirrors
  - existing Phase 1 compose changes:
  * Commit: A.0 `configs/configuration/backend/tests/example-tests.csv` — append
    HIV VL, CHIKV, DENV, ZIKV rows
  * Commit: A.1 mirror `configs/analyzer-profiles/file/quantstudio.json`
  * Commit: A.2 mirror `configs/analyzer-profiles/file/fluorocycler-xt.json`

**Liquibase layer decision summary** (answering the user's 2026-04-10 message
"why do we need liquibase update"):

- **Liquibase is NOT used for** site-specific test catalog content (LOINCs, test
  names, sample types). That's the distro CSV layer via
  `TestConfigurationHandler`.
- **Liquibase IS used for** the single baseline structural change — adding the
  `default_test_code` column to `clinlims.analyzer`. This is shipped with the
  webapp image for all deployments and is properly a schema change, not
  configuration.

---

## Verification (what "done" looks like for Phase A + B)

**A.0 — distro test catalog CSV**:

- After distro boot, SQL
  `SELECT description, loinc FROM clinlims.test WHERE description LIKE 'HIVVIRALLOAD%' OR description LIKE 'DENGUEPCR%' OR description LIKE 'CHIKVPCR%' OR description LIKE 'ZIKVPCR%'`
  shows:
  - HIVVIRALLOAD(Plasma/Serum/DBS) with LOINC `20447-9` (upgraded from baseline
    `10351-5`)
  - DENGUEPCR(Plasma/Serum) with LOINC `7855-0` (already baseline, now explicit)
  - CHIKVPCR(Plasma/Serum) with LOINC `60260-7` (fresh insert)
  - ZIKVPCR(Plasma/Serum) with LOINC `85622-9` (fresh insert)
- `tests-checksums.properties` in the container volume updates to the SHA of the
  new `example-tests.csv`.

**A.1 — QuantStudio profile**:

- Drop `CVVIH 24 07 2024 serie 02 à valider.xlsx` on a local bridge pointing at
  a directory with a QuantStudio analyzer registered. Bridge log shows
  `FileResultParser: extracted N results`.
- OE staging shows N rows with accession numbers matching the real file's
  `Sample Name` column. STANDARD/NTC rows land with `isControl=true` (filtered
  by staging-UI default).
- Same for `Arbo-extraitQS5.xls`: bridge log shows results for CHIKV + DENV +
  ZIKV targets. `autoCreateTestMappings` has created three `AnalyzerTestMapping`
  rows linking the analyzer to the three new CHIKV/DENV/ZIKV OE tests from A.0.

**A.2 — Fluorocycler profile**:

- Drop `Fluorocycler-XT/HIV-result.xlsx`. Bridge reads
  `Row/Col/ Sample ID/Type/Calc. Conc./Result` columns via the rewritten
  profile. `Type=Standard/Positive/Negative` rows flagged as controls.
  `Calc. Conc.` column populates `result`; `Result` populates `interpretation`.
  `defaultTestCode = "VIH-1"` applied to every row (after A.3.5 plumbing lands).
- Drop `ARBOVIROSE.xlsx` against a SECOND Fluorocycler-XT analyzer instance
  configured with `defaultTestCode = "CHIKV"` (or whichever arbovirus code Mekom
  confirms). Rows parse via the interpretation fallback since `Calc. Conc.` is
  absent. **Both analyzer instances point at the same physical directory** —
  this exercises A.3.7.

**A.3.7 — multi-observer**:

- Configure two Fluorocycler-XT analyzer instances in the OE UI, same
  `importDirectory`, different `filePattern` (`HIV*.xlsx` vs `ARBO*.xlsx`). Save
  both.
- `GET /admin/file-state` shows both analyzers' observers active.
- Drop `HIV-result.xlsx` and `ARBOVIROSE.xlsx` into the shared dir within the
  same polling window. Assert both files produce `PROCESSED` state rows keyed on
  their respective `(analyzerId, contentHash)`.
- Remove analyzer A via the OE UI. Assert analyzer B continues to observe new
  files in the same directory.

**B — Playwright harness + video**:

- Run `npm run pw:test -- --project=harness-demo-video`. Videos generated for
  QS5 Arbo, QS7 HIV VL, Fluorocycler HIV VL, Fluorocycler Arbo.
- Per Phase 1 invariants (non-destructive): after each ingestion the fixture
  file STILL EXISTS in the watched directory and no `.error` / `.failed` sidecar
  was written. State store shows `PROCESSED`.
- All four format-grounded template fixtures live at
  `tools/analyzer-mock-server/fixtures/{quantstudio7,quantstudio5,fluorocycler-xt}/`
  with paired `.expected.json` files. Fixtures are byte-deterministic output of
  `scripts/generate-fixture.py`; regenerating in CI produces identical bytes.
- The **older hand-crafted synthetic fixtures are deleted**, not kept. One
  fixture set, one truth: the format-grounded templates serve both fast-CI smoke
  and the `harness-demo-video` project.
- Fluorocycler multi-observer smoke: two analyzer instances configured at the
  same `importDirectory`, one with `filePattern=*hiv*.xlsx`, one with
  `*arbo*.xlsx`. Both fixtures dropped; state store shows two `PROCESSED` rows
  keyed on distinct `(analyzerId, contentHash)`.

---

## Open questions for Herbert / Mekom (do not block Phase A)

These can be answered iteratively against real results without blocking the
initial profile alignment:

1. **QS7 sheet choice**: the CVVIH file has `Results` (header row 50) AND
   `Results (2)` (header row 0). Which one does the workflow rely on? Default:
   `Results` until told otherwise.
2. **"à valider" in the filename**: is it a tech-validation state or just a
   filename convention? Default: ignore the filename, parse the file.
3. **Arbovirus LOINC confirmation with Mekom**: A.0 ships the individual
   canonical codes `60260-7` (CHIKV), `7855-0` (DENV, pan- serotype), and
   `85622-9` (ZIKV), all validated via loinc.org as "NAA with probe detection"
   codes. These match result granularity (one OE Observation per target per
   sample). If Mekom's reference catalog uses different individual codes, send
   them and we'll update the CSV + profile. Not blocking — the chosen codes are
   the canonical ones and the mapping is deterministic under `tests.get(0)`
   without any webapp code change.
4. **Fluorocycler `Result` free-text format**: confirm the parser's alphanumeric
   interpretation-fallback is what the lab wants, or if they prefer a numeric
   extraction from embedded `(CP=38.0)` patterns. Default: store verbatim; tech
   narrates the interpretation.
5. **Fluorocycler file-pattern discrimination**: can the lab commit to a
   filename convention (`HIV-*.xlsx` vs `ARBO-*.xlsx`) for multi-observer
   routing? If not, the fallback is per-analyzer subdirectories (which the lab
   controls).

---

## History — what's already shipped (reference only, not open work)

### Phase 1 — Non-destructive bridge ✅ COMPLETE (2026-04-10)

Herbert Yiga reported on 2026-04-09 that real files were disappearing from the
`/mnt` mount within minutes of upload. Root cause: the bridge deleted on success
(`bestEffortCleanup`), moved to archive/ on success, moved to error/ on failure,
and deleted as last-ditch fallback. Herbert's mount was the target.

**Fix shipped across three PRs:**

- **`openelis-analyzer-bridge` PR #34** — 4 commits: sqlite-jdbc dep,
  `FileStateStore` (WAL mode, corruption recovery, 10 unit tests),
  non-destructive `FileWatcher` rewrite (16 invariant tests asserting
  file-stays-in-place on success/retry/exhaustion), `/admin/file-state` REST
  endpoint (9 MockMvc tests). 418 tests green.
- **`OpenELIS-Global-2` PR #3372** — 4 commits: inventory manifest,
  `AnalyzerResultsServiceImpl` upsert contract documentation + tests (three-case
  dedupe: no-previous → insert; matching date or value → skip; different date
  AND value → linked correction), formatter chore, archive/error directory
  drop + Liquibase `012-drop-analyzer-archive-error-directories.xml`.
- **`openelis-madagascar-distro` PR #4** — 2 commits: `:ro` mount containment,
  `bridge-state` named Docker volume + deploy runbook.

**Invariants now in place:**

- Bridge never deletes, moves, or writes sidecar files to the watched directory
  under any outcome.
- Processing state lives in SQLite `FileStateStore` keyed on
  `(analyzerId, contentHash)` with three states (`PROCESSED`,
  `FAILED_NEEDS_HANDLING`, `RETRYING`).
- Retry backoff survives JVM restart via persistent `next_attempt_at`.
- OpenELIS `insertAnalyzerResults` upsert contract is documented + tested;
  bridge-side content-hash-keyed re-posts produce correct dedupe and
  correction-linking behavior.
- Corrupt state store recovers by renaming to `state.db.corrupt-<timestamp>` and
  creating fresh.
- Admin endpoint `GET /admin/file-state` exposes operator-visible state for
  inspection.
- Distro mounts the host analyzer path `:ro` so the bridge cannot write to it
  even if a future bug tried to.

**Forensic evidence of the pre-Phase-1 data loss**, preserved in
`docs/debug-local/mnt-snapshot/` and the backup tarball:
`/mnt/.../QuantStudio-5/Arbo-extraitQS5.xls.failed` (171 bytes),
`/mnt/.../QuantStudio-5/incoming/Arbo-extraitQS5.xls.failed` (180 bytes),
`/mnt/.../QuantStudio-5/test/Arbo.xls.failed` (154 bytes) — all written by the
old `markAsFailedInPlace()` between 17:17Z and 23:14Z on 2026-04-09, with the
original `.xls` files deleted in place. Share with Herbert in
`#oe-madagascar-internal` as proof-of-fix justification when PR #34 merges.

### Legacy plugins — reference only, do not integrate

The `plugins/` submodule (`DIGI-UW/openelisglobal-plugins`, SHA `8faf00561`)
contains `QuantStudio3`, `QuantStudio7Flex`, `FluoroCyclerXT`, `GeneXpertFile`
plugin implementations. These are **webapp-side extensions for previous
workflows** that operate on pre-loaded `List<String>` lines and cannot handle
xlsx directly. They are hardcoded to SARS-CoV-2 LOINCs. **Do not integrate or
import from these plugins** per user direction 2026-04-10 — they are consulted
only as reference for parsing patterns and legacy-compatibility questions. The
Madagascar path is strictly bridge-side.
