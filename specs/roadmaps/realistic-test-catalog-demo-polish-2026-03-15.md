# Plan: Realistic Test Catalog + Demo Video Polish

## Context

PR #3070 has all analyzer infrastructure working (ASTM framing, plugin routing,
bridge TLS, generic-only plugins). All 4 E2E tests pass. But demo videos lack
realism:

1. **No molecular test catalog** — OE has no Xpert MTB/RIF, HIV Viral Load,
   COVID-19 PCR, or Internal Control tests. Analyzer test codes (`VIH-1`, `IC`,
   `MTB-RIF`) can't map to anything.
2. **GeneXpert created by fixture SQL** — in production, analyzers are always
   created via the UI, which triggers `autoCreateTestMappings()` from the
   profile. Fixture SQL bypasses this, hardcodes wrong test IDs, and isn't
   realistic.
3. **Profile formats broken** — quantstudio has `default_test_mappings` as a map
   (not array), fluorocycler has none. `autoCreateTestMappings()` silently skips
   both.
4. **Videos stop at "staging empty"** — `accept-results.ts` doesn't navigate to
   AccessionResults.
5. **PR review comments** — 6 actionable items from Copilot.

### Architecture: How test mappings should work

Profiles are the **source of truth** for analyzer→test mappings:

1. OE starts → `TestConfigurationHandler` loads test catalog from CSV (by LOINC)
2. User creates analyzer via UI, selecting a profile
3. `AnalyzerRestController` calls
   `autoCreateTestMappings(analyzerId, profileConfig)`
4. `autoCreateTestMappings()` reads `default_test_mappings[].loinc`, calls
   `testService.getActiveTestsByLoinc()`, inserts `analyzer_test_map` rows

**No SQL-based test mappings needed.** The profile + test catalog handle
everything.

---

## Fix 1: Molecular Test Catalog via Harness CSV

### 3 CSV files in `projects/analyzer-harness/volume/configuration/backend/`

**test-sections/molecular-sections.csv** (load order 100):

```csv
testSectionName,description,isActive,sortOrder,isExternal,localization:en,localization:fr
Molecular Biology,Molecular Biology Department,Y,8,N,Molecular Biology,Biologie Moléculaire
```

**sample-types/molecular-sample-types.csv** (load order 100):

```csv
description,localAbbreviation,domain,isActive,sortOrder,localization:en,localization:fr
Sputum,Sputum,H,Y,20,Sputum,Crachat
Respiratory Swab,Resp Swab,H,Y,21,Respiratory Swab,Écouvillon Respiratoire
```

**tests/molecular-tests.csv** (load order 200):

```csv
testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,localization:en,localization:fr
Xpert MTB/RIF,Molecular Biology,Sputum,23826-1,Y,Y,1,,Xpert MTB/RIF,Xpert MTB/RIF
Xpert RIF Resistance,Molecular Biology,Sputum,46244-0,Y,Y,2,,Xpert RIF Resistance,Résistance RIF Xpert
HIV Viral Load,Molecular Biology,Plasma|Serum,20447-9,Y,Y,3,copies/mL,HIV Viral Load,Charge Virale VIH
COVID-19 PCR,Molecular Biology,Respiratory Swab,94500-6,Y,Y,4,,COVID-19 PCR,PCR COVID-19
Internal Control DNA,Molecular Biology,,89578-3,Y,N,5,,Internal Control DNA,Contrôle Interne ADN
```

`Plasma`, `Serum`, `Whole Blood` already exist in default OE.

## Fix 2: Fix analyzer profiles — proper `default_test_mappings` format

`autoCreateTestMappings()` (`AnalyzerServiceImpl.java:312`) expects:

```json
"default_test_mappings": [
  { "analyzer_code": "...", "loinc": "...", "test_name_hint": "...", "unit": "" }
]
```

### `projects/analyzer-profiles/file/quantstudio.json`

**Current** (broken — map format, silently skipped):

```json
"default_test_mappings": {
  "VIH-1": "HIV-1 VL (LOINC 20447-9)",
  "IC": "Internal Control"
}
```

**Fix** → array format with LOINCs:

```json
"default_test_mappings": [
  { "analyzer_code": "VIH-1", "test_name_hint": "HIV-1 Viral Load", "loinc": "20447-9", "unit": "copies/mL" },
  { "analyzer_code": "IC", "test_name_hint": "Internal Control DNA", "loinc": "89578-3", "unit": "" }
]
```

### `projects/analyzer-profiles/file/fluorocycler-xt.json`

**Current** (broken — no `default_test_mappings` at all):

**Fix** → add:

```json
"default_test_mappings": [
  { "analyzer_code": "VIH-1", "test_name_hint": "HIV-1 Viral Load", "loinc": "20447-9", "unit": "copies/mL" },
  { "analyzer_code": "IC", "test_name_hint": "Internal Control DNA", "loinc": "89578-3", "unit": "" }
]
```

### `projects/analyzer-profiles/astm/genexpert-astm.json`

**Already correct** — uses array format with LOINCs. No change needed.

## Fix 3: GeneXpert E2E test — create via UI, not fixture SQL

### Remove GeneXpert from `analyzer-minimal.sql`

Remove:

- Analyzer record (ID 2013)
- All `analyzer_test_map` entries for analyzer 2013
- The `analyzer_type_id` linking UPDATE for 2013
- Verification block references to 2013

Keep:

- `analyzer_type` fallback inserts (Generic ASTM/HL7/File — safety net for envs
  without plugin lifecycle)
- File-import analyzer records (2014-2016) — **wait, these should also be
  removed** since the file-import E2E test already creates them via UI
- File import configurations for 2014-2016 — also remove
- Plugin configs — also remove

Actually, `analyzer-minimal.sql` should be reduced to **only the analyzer_type
safety-net inserts** + sequence advancement. Everything else is created via UI
in the E2E tests.

### Update `astm-genexpert-results.spec.ts`

Replace step 1 ("Verify GeneXpert exists in list") with a proper create flow:

1. Navigate to analyzer dashboard
2. Click "Add Analyzer"
3. Fill name: "Cepheid GeneXpert (ASTM Mode)"
4. Select plugin type: Generic ASTM
5. Select profile: genexpert-astm
6. Select category: MOLECULAR
7. Configure IP/port for ASTM bridge connection
8. Save → triggers `autoCreateTestMappings()` with LOINC-based lookup

This mirrors the file-import test pattern (`file-import-results.spec.ts` steps
2-3).

### ASTM-specific: bridge connection config

Unlike file-import analyzers, GeneXpert needs an IP address and port for the
ASTM bridge. The form should have fields for this — need to verify the analyzer
form supports TCP/IP config.

**Files**:

- `src/test/resources/analyzer-minimal.sql` — strip to analyzer_type only
- `frontend/playwright/tests/astm-genexpert-results.spec.ts` — add create-via-UI
  flow

## Fix 4: Accept helper navigates to AccessionResults

### Update `frontend/playwright/helpers/accept-results.ts`

Add optional `accessionNumber` parameter. After staging page empties, navigate
to AccessionResults:

```typescript
export async function acceptAndVerifyResults(
  page: Page,
  testInfo: TestInfo,
  stepOffset: number,
  accessionNumber?: string
) {
  // ... existing accept + save + staging-empty logic ...

  if (accessionNumber) {
    await showStepCard(
      page,
      stepOffset + 3,
      "View Accepted Results",
      2000,
      testInfo
    );
    await page.goto(`AccessionResults?accessionNumber=${accessionNumber}`, {
      waitUntil: "domcontentloaded",
    });
    await page.waitForResponse(
      (resp) => resp.url().includes("/rest/LogbookResults"),
      { timeout: 30_000 }
    );
    await expect(page.getByText(accessionNumber)).toBeVisible({
      timeout: 10_000,
    });
    await videoPause(page, 3_000, testInfo);
  }
}
```

### Update test specs to pass accession numbers

- `astm-genexpert-results.spec.ts`: `"SPECIMEN-GX-001"`
- `file-import-results.spec.ts`: first sample ID from each analyzer (e.g.,
  `"E2E001"`, `"E2E-FC001"`)

## Fix 5: Address PR review comments

6 items from Copilot review on PR #3070:

1. **DB-match fallback leaks to legacy** (`ASTMAnalyzerReader.java`) — early
   return + error log
2. **`pluginService` looked up twice** (`ASTMAnalyzerReader.java`) — extract to
   single variable
3. **`verify-login.sh` grep brittle** — whitespace-tolerant regex
4. **Bootstrap JAR wildcard** (`bootstrap.sh`) — filter `*-sources.jar`,
   `*-javadoc.jar`
5. **E2E assertions unscoped** (`astm-genexpert-results.spec.ts`) — scope to
   parent row
6. **Hostname verification comment** (`docker-compose.analyzer-test.yml`) — add
   test-only warning

## Fix 6: Clean test-results before recording

```bash
rm -rf frontend/test-results/*
```

---

## Execution Order

0. Archive this plan to `specs/roadmaps/` for project history
1. Create 3 harness CSV files (test-sections, sample-types, tests)
2. Fix quantstudio + fluorocycler profiles (`default_test_mappings` → array
   format)
3. Strip `analyzer-minimal.sql` to analyzer_type-only
4. Rewrite GeneXpert E2E test to create analyzer via UI
5. Update `accept-results.ts` with AccessionResults navigation
6. Update test specs to pass accession numbers
7. Address 6 PR review comments
8. Update `bootstrap.sh`: checksum cleanup + JAR filter
9. Restart OE (picks up CSVs → molecular tests exist)
10. Clean test-results dir
11. Run `--project=harness` (no video, verify all pass)
12. Record `--project=demo-video`
13. Commit + push

## Files

| File                                                                                             | Change                                                   |
| ------------------------------------------------------------------------------------------------ | -------------------------------------------------------- |
| `projects/analyzer-harness/volume/configuration/backend/tests/molecular-tests.csv`               | **NEW** — 5 molecular tests                              |
| `projects/analyzer-harness/volume/configuration/backend/test-sections/molecular-sections.csv`    | **NEW** — Molecular Biology section                      |
| `projects/analyzer-harness/volume/configuration/backend/sample-types/molecular-sample-types.csv` | **NEW** — Sputum, Respiratory Swab                       |
| `projects/analyzer-profiles/file/quantstudio.json`                                               | Fix `default_test_mappings` → array with LOINCs          |
| `projects/analyzer-profiles/file/fluorocycler-xt.json`                                           | Add `default_test_mappings` array with LOINCs            |
| `src/test/resources/analyzer-minimal.sql`                                                        | Strip to analyzer_type safety-net only                   |
| `frontend/playwright/tests/astm-genexpert-results.spec.ts`                                       | Create GeneXpert via UI, pass accession, scope selectors |
| `frontend/playwright/tests/file-import-results.spec.ts`                                          | Pass accession number                                    |
| `frontend/playwright/helpers/accept-results.ts`                                                  | AccessionResults navigation (optional param)             |
| `src/main/.../ASTMAnalyzerReader.java`                                                           | No legacy fallback, single pluginService                 |
| `projects/analyzer-harness/scripts/verify-login.sh`                                              | Whitespace-tolerant grep                                 |
| `projects/analyzer-harness/bootstrap.sh`                                                         | Checksum cleanup, JAR filter                             |
| `projects/analyzer-harness/docker-compose.analyzer-test.yml`                                     | Test-only comment                                        |

## LOINC Reference

| Analyzer Code | LOINC   | OE Test Name         | Used By                |
| ------------- | ------- | -------------------- | ---------------------- |
| MTB-RIF       | 23826-1 | Xpert MTB/RIF        | GeneXpert ASTM         |
| RIF           | 46244-0 | Xpert RIF Resistance | GeneXpert ASTM         |
| HIV-VL        | 20447-9 | HIV Viral Load       | GeneXpert ASTM         |
| COVID19       | 94500-6 | COVID-19 PCR         | GeneXpert ASTM         |
| VIH-1         | 20447-9 | HIV Viral Load       | QS5, QS7, FluoroCycler |
| IC            | 89578-3 | Internal Control DNA | QS5, QS7, FluoroCycler |

## Verification

- [ ] OE startup logs show "Successfully loaded X tests from
      molecular-tests.csv"
- [ ] All 4 E2E tests create analyzers via UI (no fixture SQL analyzers)
- [ ] Profile `autoCreateTestMappings` resolves all LOINCs to real tests
- [ ] Demo videos: create → configure → results → accept → AccessionResults with
      real test names
- [ ] GeneXpert shows "Xpert MTB/RIF", "HIV Viral Load", "COVID-19 PCR"
- [ ] File imports show "HIV Viral Load", "Internal Control DNA"
- [ ] PR review comments addressed
- [ ] CI passes
