# Plan v2 — Madagascar Analyzer File-Import: Distro Video Validation + CI Alignment

> **v2 rewrite (2026-04-10 evening)**: Phase A has shipped (profiles, parser,
> plumbing, multi-observer). The open work is **two validation gates**, not one
> CI harness refactor. Gate 1 = video proof via the distro manual Playwright
> tests on a freshly-restarted distro stack. Gate 2 = align main-OE-repo
> CI/tests with the reality-based updates. The v1 plan lumped both into a "Phase
> B" that matched neither; this v2 splits them, wires the missing UI field
> (`defaultTestCode` has a backend but no form input yet), fixes the distro
> harness breakage (archive/error helper calls target removed form fields), and
> uses the real analyzer files already checked into
> `docs/debug-local/mnt-snapshot/` instead of fabricated fixtures.

---

## Context

**Problem**: Madagascar's LA2M lab is onboarding file-based molecular analyzers
(QuantStudio 5, QuantStudio 7 Flex, Bruker Fluorocycler XT) onto OpenELIS via
the analyzer bridge. The bridge polls a watched directory and forwards parsed
results to OE. Three gaps originally blocked end-to-end ingestion; two are now
shipped, one remains at the UI + validation layer:

1. ✅ **Profiles don't match real files** — shipped in Phase A.1 / A.2
   (QuantStudio arbovirus mappings, Fluorocycler v2.0.0 column rewrite).
2. ✅ **OE test catalog missing HIV VL + arbovirus rows** — shipped in Phase A.0
   (distro `example-tests.csv` CSV upsert via `TestConfigurationHandler`).
3. ✅ **Bridge single-observer-per-directory limitation** — shipped in Phase
   A.3.7 (`FileWatcher` refactor to `Map<Path, List<WatchRegistration>>`).
4. 🟡 **End-to-end validation not yet run** — unit tests green, but no video
   evidence of the distro stack ingesting real analyzer files via the true user
   workflow (create-analyzer-via-UI → drop-file → verify).
5. 🟡 **`defaultTestCode` field has no UI surface** — Phase A.3.6 added the
   `Analyzer.defaultTestCode` column + Liquibase + REST serializer + bridge
   plumbing, but `AnalyzerForm.jsx` Section 3b (FILE Import Settings) has no
   `TextInput` for it. Admins literally cannot configure the field via the form
   today. Needed for any FILE analyzer whose exports lack a per-row target
   column (e.g., Fluorocycler XT).
6. 🟡 **Distro manual Playwright harness is broken against Phase 1** —
   `create-analyzer-from-profile.ts:444-447` calls `form.fillArchiveDirectory`
   and `form.fillErrorDirectory` on UI fields that Phase 1's Liquibase changeset
   012 (drop-analyzer-archive-error-directories) removed. The distro's
   `analyzer-demo-flow.spec.ts` cannot currently pass against the post-Phase-1
   webapp.
7. 🟡 **Mock server `hain_fluorocycler` template is still outdated** — the
   mock's `templates/hain_fluorocycler.json` and
   `fixtures/fluorocycler-xt/results.xlsx` use the OLD Fluorocycler column
   layout (SampleID / Position / Result / Interpretation). After Phase A.2
   rewrote the profile to the REAL layout (Row / Col / Sample ID / Type / Calc.
   Conc. / Result), the mock fixture produces ZERO results against the new
   parser.

**What prompted this iteration**: User direction 2026-04-10 evening
distinguishes two validation gates and explicitly calls for:

- **Two gates, not one**: "We have two validation gates. The first and immediate
  one is video proof of the real runs for 3 file analyzers (quandstudios and
  fluorocycler) where we show dropping a file in the directory through analyzer
  config (or config first) thru result import and validation. The second gate is
  to align CI and tests on the main oe repo with our reality based updates."
- **Simple, true user workflow, no shortcuts**: "Make sure the tests are simple
  and test true user workflows ... Without shortcuts"
- **Evidence-grade runs**: "proper pacing and screenshots for evidence runs with
  video recording"
- **Use the documented restart flow**: "Doesn't the restart documentation and
  scripts explain exactly how to rebuild like we've been repeatedly doing??"
  (docs at `openelis-madagascar-distro/docs/validation.md`, script at
  `scripts/restart-stack.sh`).

**Intended outcome (Gate 1)**: **three** videos — QuantStudio 5 (Arbo),
QuantStudio 7 (HIV VL), and **one** Fluorocycler XT run that drops BOTH
`HIV-result.xlsx` and `ARBOVIROSE.xlsx` against the SAME Fluorocycler analyzer
instance. Reality (confirmed with user 2026-04-10 evening): **one physical
machine, one analyzer instance**. Both file types come out of the same
Fluorocycler XT on the LA2M bench; a lab running both HIV VL and Arbovirus
assays on the same instrument is the real workflow, not a contrived
multi-instance setup.

Each video shows the admin creating the analyzer via the unified AnalyzerForm,
dropping a REAL file from `docs/debug-local/mnt-snapshot/` into the watched
directory, the bridge parsing and forwarding the results, and the admin
accepting the results in the OE UI. The non-destructive invariants (files stay
in place, state store shows PROCESSED) are verified on every drop.

**Note on the A.3.7 multi-observer refactor**: it's still useful as a general
capability (e.g., two physically distinct machines writing to the same NFS
mount, or hot-swap scenarios), but it is NOT the framing for Fluorocycler.
Fluorocycler's reality is one machine → one instance. Gate 1 does not exercise
multi-observer on Fluorocycler.

**Intended outcome (Gate 2)**: the main-repo Playwright file-import harness
(`frontend/playwright/tests/demo/harness/file-import-results.spec.ts`) asserts
against the real parser output shape, not against mock-pre-parsed
`metadata.results`. CI runs green against the new profile + catalog reality.

**Scope gates** (carried over from v1, still valid):

- Webapp code changes + Liquibase structural changes are IN scope if git-tracked
  and landing on existing PRs (no new PRs).
- Site-specific test catalog content (LOINCs, test names) goes in distro CSV,
  NOT Liquibase.
- Legacy plugins in `plugins/` are reference only — not integrated.
- GeneXpert file path deferred until ASTM/HL7 primary path fails.
- Tecan / Multiskan / Attune CytPix / DT-prime out of scope for this iteration
  (Phase C follow-up).

---

## Completed — Phase A (shipped 2026-04-10)

All Phase A work is merged on the three working branches and pushed. 441 bridge
tests pass, 21 FileWatcher tests pass, webapp compiles. One webapp unit test
added for the new `defaultTestCode` plumbing through
`BridgeRegistrationService.registerFile`.

### Commits shipped this session

**`openelis-madagascar-distro` — PR #4, branch `demo/madagascar-analyzers`**:

- `2e719b5` A.0 — distro test catalog CSV: append `HIVVIRALLOAD` (LOINC
  20447-9), `DENGUEPCR` (LOINC 7855-0), `CHIKVPCR` (LOINC 60260-7), `ZIKVPCR`
  (LOINC 85622-9). `TestConfigurationHandler` upserts by normalized description
  so HIVVIRALLOAD / DENGUEPCR land in-place on existing Liquibase baseline rows;
  CHIKVPCR / ZIKVPCR are fresh inserts.
- `6e8c800` A.1 mirror — `configs/analyzer-profiles/file/quantstudio.json`
  arbovirus entries.
- `1ec8007` A.2 mirror — `configs/analyzer-profiles/file/fluorocycler-xt.json`
  v2.0.0 rewrite (real-layout column_mapping, union default_test_mappings).

**`openelis-analyzer-bridge` — PR #34, branch
`fix/madagascar-file-ingestion-stability`**:

- `6a2be3c` A.3.7 — `FileWatcher` multi-observer refactor. Replaced three
  single-entry `Map<Path, ...>` with `Map<Path, List<WatchRegistration>>`; added
  nested `WatchRegistration` record; rewrote `registerDirectoryInternal` /
  `determineAnalyzerId` / `shouldProcessFile` / `rescanAllDirectories` /
  `removeWatchDirectory`. Added `removeWatchRegistration(path, analyzerId)`
  helper. Added 5 new tests (`multipleObservers_*`, `removeWatchRegistration_*`,
  `reregisterSameAnalyzer_*`) on top of the 16 preserved invariant tests.
- `c65dd1b` A.3.5 — `defaultTestCode` plumbing: added field to
  `AnalyzerRegistryConfig.AnalyzerEntry`, read from webapp
  `/rest/analyzer/analyzers` response at `AnalyzerRegistryBootstrap.java:128`,
  passed from `FileMessageHandler.processFile:150` into the already-landed
  `FileResultParser.parse(InputStream, Map, String defaultTestCode)` +
  `parseCsv(..., String defaultTestCode)` overloads.

**`OpenELIS-Global-2` — PR #3372, branch
`fix/madagascar-accession-results-file-e2e`**:

- `8644624d` A.1 — `projects/analyzer-profiles/file/quantstudio.json` arbovirus
  entries.
- `567f0c4d` A.3.6 — webapp `Analyzer.defaultTestCode` field:
  - `Analyzer.java` valueholder: `@Column(name="default_test_code", length=50)`
    with getter/setter
  - `src/main/resources/liquibase/3.4.14.x/013-analyzer-default-test-code.xml`
    (+ include in `base.xml` alongside existing FILE config unification
    changesets)
  - `BridgeRegistrationService.registerFile` signature grows a `defaultTestCode`
    parameter; all 3 callers updated (`FileImportServiceImpl:153`,
    `AnalyzerBridgeStartupRegistrar:115`, `AnalyzerRestController:1087`)
  - `FileImportServiceImpl.autoCreateFromProfile` reads `default_test_code` from
    profile JSON top-level and writes to the entity before persist
  - `AnalyzerRestController` `/rest/analyzer/analyzers` response map includes
    `defaultTestCode` at line 661
  - `AnalyzerBridgeStartupRegistrarTest` updated for new 9-arg `registerFile`
    mocks + one new `shouldRegisterFileAnalyzerWithDefaultTestCode` case
- `3dd96a22` chore — bump `tools/openelis-analyzer-bridge` submodule to
  `c65dd1b` (picks up A.3.5 + A.3.7)
- `906a8228` A.2 — `projects/analyzer-profiles/file/fluorocycler-xt.json` v2.0.0
  rewrite

### End-to-end chain (post Phase A)

Profile JSON `default_test_code` → `FileImportServiceImpl.autoCreateFromProfile`
→ `Analyzer.defaultTestCode` column → `AnalyzerRestController` response map →
bridge `AnalyzerRegistryBootstrap` → `AnalyzerEntry.defaultTestCode` →
`FileMessageHandler.processFile` →
`FileResultParser.parse(..., defaultTestCode)` → per-row fallback for files with
no target column.

Profile `default_test_mappings` → `AnalyzerServiceImpl.autoCreateTestMappings` →
`testService.getActiveTestsByLoinc(loinc)` → `AnalyzerTestMapping` rows linking
analyzer test codes to OE test rows. Individual canonical LOINCs make
`tests.get(0)` deterministic (no disambiguation patch needed).

`FileWatcher` allows multiple analyzer registrations per directory. Each
registration's own glob pattern is captured in the observer's IOFileFilter
closure, so two observers at the same path don't cross-fire. Verified by 5
dedicated multi-observer tests.

---

## Current state — pre-Gate 1 snapshot

### Real files already available locally (gitignored, for validation)

`docs/debug-local/mnt-snapshot/la2m/central/analyzers_results/`:

| Analyzer                    | Real file                                                                                                                           | Backup / notes                                                      |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| QuantStudio 7 Flex (HIV VL) | `QuantStudio-7/archive/CVVIH 24 07 2024 serie 02 à valider.xlsx` (484 KB)                                                           | 6 sheets, `Results` header at row 50                                |
| QuantStudio 5 (Arbovirus)   | **missing from mnt-snapshot** (old bridge deleted it; only `.failed` sidecars remain at `QuantStudio-5/Arbo-extraitQS5.xls.failed`) | Backup preserved at `docs/debug-local/Arbo-extraitQS5.xls` (1.1 MB) |
| Fluorocycler XT (HIV VL)    | `Fluorocycler-XT/HIV-result.xlsx` (13 KB)                                                                                           | 1 sheet, header row 0, 95 rows                                      |
| Fluorocycler XT (Arbovirus) | `Fluorocycler-XT/ARBOVIROSE.xlsx` (10 KB)                                                                                           | 1 sheet, header row 0, 15 rows, no `Calc. Conc.` col                |
| Tecan Infinite F50          | `ELISA reader Tecan Infinite F50/INFINITE F50 MAGELLAN.xlsx`                                                                        | Out of scope Phase C                                                |
| Multiskan FC                | `ELISA reader Multiscan FC/*.xlsx` and `.csv`                                                                                       | Out of scope Phase C                                                |
| GeneXpert (×4)              | `genexpert-[1-4]/Export GE 03102024 N.csv`                                                                                          | Out of scope (ASTM primary)                                         |
| Attune CytPix               | `Invitrogen Attune CytPix/*.pdf`                                                                                                    | Out of scope (PDF)                                                  |

All files are already under the gitignored `docs/debug-local/mnt-snapshot/`
path, so they can be COPIED into the running distro stack's watched directory at
test time without polluting commits. The `docs/debug-local/` `.gitignore` entry
(shipped earlier in the Phase 1 containment workstream, task #14) covers both
the mnt-snapshot tree and the root-level `Arbo-extraitQS5.xls` backup.

### Distro test harness state (what exists, what's broken)

**Location**: `openelis-madagascar-distro/tests/playwright/`.

**Spec file**: `tests/demo/harness/analyzer-demo-flow.spec.ts` (391 lines).
Contains a `CONFIGS` array with entries for 10+ analyzers including:

- `Demo: QuantStudio 7` — profile `QuantStudio QS5/QS7`, mock template
  `quantstudio7`
- `Demo: QuantStudio 5` — profile `QuantStudio QS5/QS7`, mock template
  `quantstudio5`
- `Demo: FluoroCycler XT` — profile `Bruker FluoroCycler XT`, mock template
  `hain_fluorocycler`

Each test walks the true user workflow: create analyzer via UI → test connection
(TCP only) → push via mock server → wait → verify results on AnalyzerResults
page → accept results → teardown. This is exactly the structure the user wants
for Gate 1 videos.

**Playwright config**: `tests/playwright/playwright.config.ts:152-164` already
has a `harness-demo-video` project with `video: "on"` and
`slowMo: process.env.PLAYWRIGHT_SLOWMO || "500"`, matching
`**/demo/harness/**/*.spec.ts`. Invoked via:

```bash
COMPOSE_PROFILES=demo docker compose \
  -f docker-compose.yml -f docker-compose.validate.yml \
  run --rm demo-tests
```

Videos land in `test-results/playwright-report/` and
`test-results/test-output/`.

**Distro image overlays**:

- `docker-compose.yml` — base (pulls `:develop` images from Docker Hub)
- `docker-compose.validate.yml` — adds mock server, MLLP, demo-tests runner
- `docker-compose.local-images.yml` — overrides webapp / bridge / mock /
  frontend with `:local` tags for PR validation

**Restart script**: `scripts/restart-stack.sh` (verified in session). Runs
`docker compose down`; optional `--clean` flag runs `down -v` to wipe named
volumes (DB, certs, indexes); clears Liquibase lock; `up -d`; waits for webapp /
bridge / mock health. Documented at `docs/validation.md:92-117`.

**Image rebuild process** (from `docs/validation.md:15-52`):

```bash
cd /home/ubuntu/OpenELIS-Global-2
DOCKER_BUILDKIT=1 docker build --platform linux/amd64 \
  -t itechuw/openelis-global-2:local .

cd /home/ubuntu/OpenELIS-Global-2/tools/openelis-analyzer-bridge
DOCKER_BUILDKIT=1 docker build --platform linux/amd64 \
  -t itechuw/openelis-analyzer-bridge:local .
```

Then `./scripts/restart-stack.sh --clean` reconstitutes the stack on the new
images with a fresh DB (so Liquibase 013 runs).

### Distro harness breakage discovered

**Helper breakage** at
`tests/playwright/playwright/helpers/create-analyzer-from-profile.ts:444-447`:

```typescript
if (config.protocol === "FILE" && config.push.targetDir) {
  await presentation.pause(1_000);
  const dirs = buildFileImportDirectories(config.push.targetDir);
  await form.fillImportDirectory(dirs.importDirectory);
  await form.fillArchiveDirectory(dirs.archiveDirectory); // ← removed from form
  await form.fillErrorDirectory(dirs.errorDirectory); // ← removed from form
  await presentation.pause(500);
}
```

The `fillArchiveDirectory` / `fillErrorDirectory` methods in
`fixtures/analyzer-form.ts` target locators that no longer exist in
`AnalyzerForm.jsx` — Phase 1 Liquibase changeset 012 dropped the columns, and
the subsequent unified-form commits removed the corresponding `TextInput`
elements. The distro's FILE tests currently fail at config-time against any
webapp image newer than the Phase 1 cutover.

**`buildFileImportDirectories`** helper (same file, lines 334-353) also derives
`archiveDirectory` + `errorDirectory` as siblings of the incoming path. The
derivation is dead code post-Phase-1.

**Mock Fluorocycler template misalignment** —
`tools/analyzer-mock-server/templates/hain_fluorocycler.json` (inside the main
OE repo's submodule, also loaded by distro's validation overlay):

```json
{
  "columns": [
    {"index": 0, "name": "Position", ...},
    {"index": 1, "name": "Sample ID", ...},
    {"index": 2, "name": "Result", ...},
    {"index": 3, "name": "Interpretation", ...}
  ],
  "fixture": {
    "file": "fixtures/fluorocycler-xt/results.xlsx",
    "column_mapping": {
      "sampleId": "SampleID",
      "result": "CP",
      "testCode": "TargetName"
    },
    "testCodeFilter": "VIH-1"
  }
}
```

This is the OLD profile layout that never matched the real Fluorocycler files.
The mock's `results.xlsx` fixture was generated against this schema. After Phase
A.2 rewrote the OE profile to the REAL layout, the mock fixture's column names
no longer match what the parser expects. When the distro harness pushes via
`POST /simulate/file/hain_fluorocycler`, the mock copies `results.xlsx` into the
watched directory, the bridge parser reads it with the new `column_mapping`, and
finds zero matches — test fails. This is the "still outdated" gap the user
flagged.

**QS5 / QS7 mock fixtures** (not yet audited in detail) — the templates are at
`templates/quantstudio5.json` and `templates/quantstudio7.json` with fixtures at
`fixtures/quantstudio5/` and `fixtures/quantstudio7/`. They likely have the same
mismatch issue because the old profiles they targeted had simpler column layouts
than the real SDS exports.

### `defaultTestCode` UI surface gap

`AnalyzerForm.jsx:40-57` defines the `formData` state. Section 3b (FILE Import
Settings, line 807) has `TextInput` elements for `importDirectory`,
`filePattern`, `columnMappings`, `delimiter`, and `skipRows` but NO field for
`defaultTestCode`. The backend schema, REST serializer, and bridge plumbing are
all ready, but admins cannot set the value through the form.

For Gate 1, the Fluorocycler XT workflow is ONE analyzer instance that handles
both HIV and Arbovirose files from the same physical machine. The parser needs a
way to pick a file-wide test code fallback per file type (see 2.0 below for the
`default_test_code_overrides` map). BUT the scalar `defaultTestCode` field in
the form is still needed for other single-assay FILE analyzers that don't have
per-row target columns. Without a UI field for the scalar, admins cannot
configure them via the true user workflow — the test would have to reach around
the UI with a direct REST API PUT. Gate 1 adds the TextInput in 2.1.

---

## Phase 2 Gate 1 — Distro video validation

**Goal**: produce **three** video recordings of the distro stack ingesting real
analyzer files end-to-end via the true user workflow (create-analyzer via UI
form → drop file in watched directory → wait → see results → accept results).
The Fluorocycler video drops BOTH `HIV-result.xlsx` and `ARBOVIROSE.xlsx`
against a single Fluorocycler analyzer instance (reality: one machine runs both
assays). Videos land in `test-results/playwright-report/`. User validates,
iterates on pacing / clarity, re-records as needed.

**Non-goals for Gate 1**:

- Do NOT rewrite the main-repo Playwright harness (that's Gate 2).
- Do NOT write a synthetic fixture generator (Gate 1 uses real files from
  `docs/debug-local/mnt-snapshot/`).
- Do NOT commit real analyzer files to the repo (they stay in gitignored
  `docs/debug-local/`).
- Do NOT remove the mock server's FILE handler — it may still be the right
  answer for Gate 2, and removing it now would break TCP/ASTM/HL7 tests that
  share the same mock infrastructure.
- Do NOT create two Fluorocycler analyzer instances for Gate 1. Reality is one
  machine, one instance. The multi-observer refactor shipped in Phase A.3.7
  stays in place as a general capability but is not exercised by the
  Fluorocycler scenario.

### 2.0 — Add `default_test_code_overrides` filename → testCode routing

**Why this is needed**: the Fluorocycler XT has ONE analyzer instance that
processes multiple file types distinguished only by filename (`HIV-result.xlsx`
vs `ARBOVIROSE.xlsx`). The files have no per-row target column, so the parser
can't look up testCode from `column_mapping`. The existing `default_test_code`
supports only ONE file-wide fallback — insufficient for a multi-file-type
workflow on a single instance.

**Solution**: extend the profile JSON and the end-to-end plumbing chain to
support an OPTIONAL filename-glob-to-testCode map. The parser picks the
effective defaultTestCode by walking the override map and using the first
matching filename glob; falls back to the existing scalar `default_test_code` if
no override matches; falls back to per-row testCode extraction (current
behavior) if neither is set.

**Plumbing chain** (mirrors the Phase A.3.6 + A.3.5 pattern):

1. **Profile JSON** — add top-level field:

   ```jsonc
   "default_test_code_overrides": {
     "HIV*.xlsx":  "VIH-1",
     "ARBO*.xlsx": "CHIKV"
   }
   ```

   Edit `projects/analyzer-profiles/file/fluorocycler-xt.json` (+ distro
   mirror).

2. **Webapp `Analyzer` entity** — add a new `defaultTestCodeOverrides` field
   persisted as JSON text:

   ```java
   @Column(name = "default_test_code_overrides", columnDefinition = "TEXT")
   private String defaultTestCodeOverridesJson;

   public Map<String, String> getDefaultTestCodeOverrides() {
     // Jackson deserialize; return empty map if null/invalid
   }
   public void setDefaultTestCodeOverrides(Map<String, String> overrides) {
     // Jackson serialize
   }
   ```

   Same pattern as `columnMappings` already uses (line 116-117, 298-373).

3. **Liquibase changeset** `014-analyzer-default-test-code-overrides.xml` —
   `ALTER TABLE clinlims.analyzer ADD COLUMN default_test_code_overrides TEXT;`
   Include in `3.4.14.x/base.xml` alongside the existing 013 changeset.

4. **`FileImportServiceImpl.autoCreateFromProfile`** — read
   `default_test_code_overrides` from profile JSON at profile-apply time and
   call `analyzer.setDefaultTestCodeOverrides(overridesMap)` before persist.

5. **`AnalyzerRestController` response map** — include
   `defaultTestCodeOverrides` (as a Map, already deserialized by the entity
   getter) in the `/rest/analyzer/analyzers` JSON.

6. **`BridgeRegistrationService.registerFile`** — grow the signature to accept
   `Map<String, String> defaultTestCodeOverrides`; include in the POST body to
   the bridge when non-empty.

7. **Bridge `AnalyzerRegistryConfig.AnalyzerEntry`** — add
   `defaultTestCodeOverrides` Map field.

8. **Bridge `AnalyzerRegistryBootstrap`** — read `defaultTestCodeOverrides` from
   the webapp response and populate `AnalyzerEntry`.

9. **Bridge `FileMessageHandler.processFile`** — compute
   `effectiveDefaultTestCode` by walking the overrides map against the file's
   name using
   `java.nio.file.FileSystems.getDefault().getPathMatcher("glob:<key>")`,
   returning the first match's value; falls back to
   `analyzerEntry.getDefaultTestCode()` if no match:
   ```java
   String effectiveDefaultTestCode = analyzerEntry.getDefaultTestCode();
   Map<String, String> overrides = analyzerEntry.getDefaultTestCodeOverrides();
   if (overrides != null && !overrides.isEmpty()) {
     Path fileName = filePath.getFileName();
     for (Map.Entry<String, String> entry : overrides.entrySet()) {
       PathMatcher matcher = FileSystems.getDefault()
           .getPathMatcher("glob:" + entry.getKey());
       if (matcher.matches(fileName)) {
         effectiveDefaultTestCode = entry.getValue();
         log.info("Filename {} matched override glob {} -> testCode {}",
             fileName, entry.getKey(), entry.getValue());
         break;
       }
     }
   }
   // Pass effectiveDefaultTestCode into FileResultParser.parse(...)
   ```
   The `FileResultParser.parse(..., String defaultTestCode)` overload is
   unchanged — it still takes a single scalar string; the override resolution
   happens one layer up.

**Tests to add**:

- **Webapp `AnalyzerBridgeStartupRegistrarTest`** (or equivalent serializer
  test): assert that an `Analyzer` entity with a non-empty
  `defaultTestCodeOverrides` map serializes correctly in the REST response.
- **Bridge `FileMessageHandlerTest`** (new file or extend existing): assert that
  `processFile` on `HIV-result.xlsx` routes through with `VIH-1`, and
  `ARBOVIROSE.xlsx` routes through with `CHIKV`, given an `AnalyzerEntry` with
  the overrides map.
- **Bridge `FileResultParserTest`** MadagascarRealFiles class: extend existing
  real-file tests to verify `HIV-result.xlsx` emits 95 result rows keyed to
  `VIH-1` when `defaultTestCode="VIH-1"` is passed; and `ARBOVIROSE.xlsx` emits
  15 rows keyed to `CHIKV` when `defaultTestCode="CHIKV"` is passed.

**Unified form UI** — the admin still enters `defaultTestCode` (scalar) in the
form. The overrides map is auto-populated from the profile JSON at profile-apply
time and is NOT editable in the form (it's considered profile-level config, not
instance-level). Profiles without overrides work exactly as before.

**Open question** (captured for Mekom): the Fluorocycler Arbovirose file
contains multiplex results where one tech-validated run covers all three
arbovirus targets in free-text form. OE has three separate test rows (CHIKVPCR,
DENVPCR, ZIKVPCR) shipped in Phase A.0, each with its own individual LOINC. For
Gate 1 we use `CHIKV` as the override value as a pragmatic catchall (reuses the
existing CHIKVPCR row; no new catalog work needed). If Mekom wants a proper
panel representation, we add an `ARBOPANEL` test row with LOINC `81154-7` in the
distro CSV and switch the override value — a small follow-up, not a blocker. If
they want three separate result rows per sample, the parser needs a split-row
feature deferred to Phase C.

### 2.1 — Add `defaultTestCode` TextInput to `AnalyzerForm.jsx`

**Where**: `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx`,
inside Section 3b (`isFileProtocol && (...)`), after the `skipRows` input.

**Changes**:

- `formData` initial state (line ~50): add `defaultTestCode: ""`.
- `useEffect` that reads an existing analyzer (line ~112): add
  `defaultTestCode: analyzer.defaultTestCode || ""`.
- Reset-on-new block (line ~135): add `defaultTestCode: ""`.
- Profile-apply / profile-selection block: if the profile JSON has a
  `default_test_code` top-level field, auto-fill it into
  `formData.defaultTestCode` (mirrors how `fileFormat` / `delimiter` etc.
  already pre-fill from profile defaults).
- New `TextInput` inside Section 3b:
  ```jsx
  <TextInput
    id="analyzer-default-test-code"
    data-testid="analyzer-form-default-test-code-input"
    labelText={intl.formatMessage({
      id: "analyzer.form.defaultTestCode",
      defaultMessage: "Default Test Code (optional)",
    })}
    placeholder="VIH-1"
    value={formData.defaultTestCode}
    onChange={(e) => handleFieldChange("defaultTestCode", e.target.value)}
    helperText={intl.formatMessage({
      id: "analyzer.form.defaultTestCode.helperText",
      defaultMessage:
        "Used when result files have no per-row target column (e.g., Fluorocycler HIV VL). The bridge parser applies this code as a fallback for rows whose column_mapping yields no testCode.",
    })}
  />
  ```
- Save handler must include `defaultTestCode` in the POST/PUT body
  (`handleSubmit` or equivalent — check existing code path for how
  `importDirectory` / `filePattern` get serialized).
- i18n: add `analyzer.form.defaultTestCode` and
  `analyzer.form.defaultTestCode.helperText` keys to `en.json` ONLY (per
  constitution: Transifex is source of truth for non-English).

**Verification**: manually open OE in a browser after the rebuild, click Add
Analyzer, select the Fluorocycler profile, confirm the new field appears with an
empty placeholder, enter `VIH-1`, save, then re-open the analyzer and confirm
the value persisted.

### 2.2 — Remove archive/error dead code from distro helper + form fixture

**Where**:

- `openelis-madagascar-distro/tests/playwright/playwright/helpers/create-analyzer-from-profile.ts`
- `openelis-madagascar-distro/tests/playwright/playwright/fixtures/analyzer-form.ts`

**Changes**:

- Delete `form.fillArchiveDirectory(dirs.archiveDirectory)` and
  `form.fillErrorDirectory(dirs.errorDirectory)` calls at
  `create-analyzer-from-profile.ts:446-447`.
- Delete `buildFileImportDirectories` helper at lines 334-353 — inline the
  import-directory derivation at the one call site (just use
  `config.push.targetDir` directly).
- Delete `archiveDirectoryInput` / `errorDirectoryInput` locators and
  `fillArchiveDirectory` / `fillErrorDirectory` methods from
  `fixtures/analyzer-form.ts` (lines 28-29, 77-80, 238-244 per earlier grep).

**Constitution compliance**: Principle X (Legacy Code Removal) — "if it's gone,
delete the callers; don't guard-comment them." No backward-compat shims.

### 2.3 — Add `defaultTestCode` to distro form fixture + AnalyzerTestConfig

**Where**:

- `openelis-madagascar-distro/tests/playwright/playwright/fixtures/analyzer-form.ts`
- `openelis-madagascar-distro/tests/playwright/playwright/helpers/analyzer-test-config.ts`
- `openelis-madagascar-distro/tests/playwright/playwright/helpers/create-analyzer-from-profile.ts`

**Changes**:

- `analyzer-form.ts` — add `defaultTestCodeInput` locator targeting
  `[data-testid="analyzer-form-default-test-code-input"]` and a
  `fillDefaultTestCode(code: string)` method.
- `analyzer-test-config.ts` — extend `AnalyzerTestConfig` type with optional
  `defaultTestCode?: string` field.
- `create-analyzer-from-profile.ts` — inside the `isFileProtocol` block after
  `fillImportDirectory`, call `form.fillDefaultTestCode(config.defaultTestCode)`
  when `config.defaultTestCode` is set.

### 2.4 — Update distro `analyzer-demo-flow.spec.ts` FILE CONFIGS

**Where**:
`openelis-madagascar-distro/tests/playwright/playwright/tests/demo/harness/analyzer-demo-flow.spec.ts`

**Changes** — keep exactly THREE FILE CONFIGS for Gate 1 (no duplicates):

- **`Demo: QuantStudio 7`** — unchanged config shape, but swap the mock-push
  step for a real-file-drop step (see 2.5 below). The QS7 real file is
  `QuantStudio-7/archive/CVVIH 24 07 2024 serie 02 à valider.xlsx`.
  `defaultTestCode` stays unset — QS7 has the per-row `Target Name` column so
  each row's test code comes from `column_mapping`.
- **`Demo: QuantStudio 5`** — same swap. Real file is
  `docs/debug-local/Arbo-extraitQS5.xls`. `defaultTestCode` stays unset for the
  same reason (per-row `Target Name` covers CHIKV / DENV / ZIKV).
- **`Demo: Bruker FluoroCycler XT`** — **ONE** analyzer instance, **ONE**
  `importDirectory`, **ONE** `filePattern` (probably `*.xlsx` to catch both file
  types), and a NEW configuration field `defaultTestCodeOverrides` (see 2.0
  below) that maps filename globs to test codes:
  ```typescript
  defaultTestCodeOverrides: {
    "HIV*.xlsx": "VIH-1",
    "ARBO*.xlsx": "CHIKV",
  }
  ```
  The test body drops BOTH `HIV-result.xlsx` and `ARBOVIROSE.xlsx` into the
  single instance's watched directory, waits for the bridge to process each, and
  verifies both sets of results appear in OE staging with the correct test
  codes.

**Rationale** (reality, per user direction 2026-04-10 evening): the Fluorocycler
XT at LA2M is **one machine** that runs both HIV VL and Arbovirus assays. A
lab's inventory has ONE Fluorocycler instrument, so OE should have ONE
Fluorocycler analyzer entity. The parser needs to distinguish which test code
applies to which file it processes because the files themselves have no per-row
target column. Filename pattern is the distinguishing signal — `HIV*.xlsx` →
VIH-1, `ARBO*.xlsx` → CHIKV. See 2.0 for the plumbing.

### 2.5 — Switch FILE protocol from mock-push to direct file drop

**Rationale**: the user's "simple + true user workflow + no shortcuts" direction
means the test should drop a real file via the filesystem path a lab tech would
actually use (shared network mount, USB import, NFS, etc.), not via the mock
server's `POST /simulate/file/{template}` route. The mock pre-parses and returns
`metadata.results` that bypass the real parser — that's exactly the shortcut to
eliminate for Gate 1.

**Where**: new helper
`openelis-madagascar-distro/tests/playwright/playwright/helpers/drop-real-analyzer-file.ts`
and updated `pushAnalyzerResult` dispatch in `push-analyzer-result.ts`.

**Implementation** — new helper function:

```typescript
/**
 * Drop a real analyzer file from docs/debug-local/mnt-snapshot/ into the
 * bridge's watched volume, bypassing the mock server. This exercises the
 * true user workflow: operator writes a file to the watched directory,
 * bridge polls, parser runs, FHIR forwarded to OE.
 *
 * Uses `docker cp` into the openelis-analyzer-bridge container because
 * /data/analyzer-imports is a named Docker volume, not a host bind mount.
 * The test runner runs on the host and cannot write to the volume directly.
 */
export async function dropRealAnalyzerFileViaDocker(
  sourceFile: string,
  targetDir: string,
  fileName?: string
): Promise<string> {
  const finalName = fileName || path.basename(sourceFile);
  const targetPath = `${targetDir}/${finalName}`;

  // Ensure target dir exists inside the container.
  execFileSync("sudo", [
    "docker",
    "exec",
    "openelis-analyzer-bridge",
    "mkdir",
    "-p",
    targetDir,
  ]);

  // Copy the real file into the bridge's watched volume.
  execFileSync("sudo", [
    "docker",
    "cp",
    sourceFile,
    `openelis-analyzer-bridge:${targetPath}`,
  ]);

  return targetPath;
}
```

The dispatcher in `push-analyzer-result.ts` gets a new branch: if
`push.protocol === "FILE"` AND `push.realFileSourcePath` is set, call the new
helper instead of `POST /simulate/file/{template}`. For backward compat with any
remaining mock-path tests, the mock branch stays as a fallback.

For assertion: after `dropRealAnalyzerFileViaDocker`, the test waits (via
`presentation.pause`) and then polls the OE UI for expected accession IDs.
**Expected IDs are extracted directly from the real file at test setup time**
using a small openpyxl / xlrd snippet (or by reading a hand-curated
`.expected.json` sidecar file for each real file, committed under
`docs/debug-local/mnt-snapshot/*.expected.json`). The expected-ids file is also
gitignored.

**Alternative if `sudo docker` is unavailable in the test runner**: use the
existing named volume at the host level via
`docker run --rm -v analyzer-imports:/v alpine cp ...`. The chosen mechanism is
whichever works in this environment.

### 2.6 — Regenerate mock fixtures OR disable mock FILE handler (Gate 1 scope)

**Recommendation**: Leave the mock FILE handler alone for Gate 1 — the CONFIGS
switch to `realFileSourcePath` (via 2.5) means the mock's `hain_fluorocycler` /
`quantstudio5` / `quantstudio7` templates are no longer on the Gate 1 critical
path. Their misalignment becomes Gate 2 concern (either regenerate the fixtures
to match the new profiles or delete them entirely).

**Why not regenerate now**: the mock templates are entangled with several other
tests (ASTM, HL7) and touching them risks breaking unrelated tests just before
the Gate 1 videos need to be recorded. Keep the regeneration as explicit Gate 2
work.

### 2.7 — Pacing, screenshots, evidence capture in specs

Per user direction "proper pacing and screenshots for evidence runs."

**Where**:

- `openelis-madagascar-distro/tests/playwright/playwright/tests/demo/harness/analyzer-demo-flow.spec.ts`
- `openelis-madagascar-distro/tests/playwright/playwright/helpers/demo-presentation.ts`

**Changes**:

- Confirm `harness-demo-video` project has video capture enabled (it already
  does: `video: "on"` at `playwright.config.ts:158`).
- Ensure `slowMo` is 500ms or higher (already configured via `PLAYWRIGHT_SLOWMO`
  env var defaulting to `"500"`). For the video run, set `PLAYWRIGHT_SLOWMO=750`
  for clarity.
- Add
  `await testInfo.attach('step-N-after-X', { body: await page.screenshot(), contentType: 'image/png' })`
  at the key moments:
  - After analyzer form opened
  - After profile selected (shows auto-filled fields)
  - After form saved
  - After file dropped (shows the watched directory)
  - After results appeared in staging
  - After accept completed
- Add explicit `presentation.step(N, "Friendly description")` calls in the spec
  body so the presentation.ts adds title cards / captions in the video. The
  existing `demo-presentation.ts` helper already supports this pattern — just
  confirm the calls are present at each stage.

### 2.8 — Rebuild local Docker images from PR branches

**Commands** (per `openelis-madagascar-distro/docs/validation.md:15-52`):

```bash
# Webapp (from PR #3372 branch — already checked out locally)
cd /home/ubuntu/OpenELIS-Global-2
DOCKER_BUILDKIT=1 docker build --platform linux/amd64 \
  -t itechuw/openelis-global-2:local .

# Bridge (from PR #34 branch — already checked out via submodule)
cd /home/ubuntu/OpenELIS-Global-2/tools/openelis-analyzer-bridge
DOCKER_BUILDKIT=1 docker build --platform linux/amd64 \
  -t itechuw/openelis-analyzer-bridge:local .

# Verify tags were created
docker image ls | grep ':local'
```

**Sanity check** — `docker-compose.local-images.yml` in the distro repo already
pins webapp + frontend + bridge + analyzer-mock to `:local` tags, so no compose
edit is needed. Just build the images.

### 2.9 — Restart distro stack with `--clean`

```bash
cd /home/ubuntu/openelis-madagascar-distro
./scripts/restart-stack.sh --clean
```

`--clean` runs `docker compose down -v` to wipe the DB volume so Liquibase
changeset 013 (defaultTestCode column) runs on a fresh schema. The script also
clears any stale Liquibase lock from the prior run and waits for webapp +
bridge + mock + proxy health checks before returning.

**Post-restart sanity**:

```bash
# Verify the new images are running
docker inspect openelisglobal-webapp --format '{{.Config.Image}}'
# expect: itechuw/openelis-global-2:local
docker inspect openelis-analyzer-bridge --format '{{.Config.Image}}'
# expect: itechuw/openelis-analyzer-bridge:local

# Verify the new catalog rows exist
docker exec openelisglobal-database \
  psql -U clinlims -d clinlims -c \
  "SELECT description, loinc FROM clinlims.test WHERE description LIKE 'HIVVIRALLOAD%' OR description LIKE 'CHIKVPCR%' OR description LIKE 'DENGUEPCR%' OR description LIKE 'ZIKVPCR%' ORDER BY description;"
# expect: 6+ rows with expected LOINCs

# Verify the new analyzer column exists
docker exec openelisglobal-database \
  psql -U clinlims -d clinlims -c \
  "SELECT column_name FROM information_schema.columns WHERE table_name='analyzer' AND column_name='default_test_code';"
# expect: default_test_code row
```

### 2.10 — Run `harness-demo-video` and capture videos

```bash
cd /home/ubuntu/openelis-madagascar-distro
PLAYWRIGHT_SLOWMO=750 \
COMPOSE_PROFILES=demo docker compose \
  -f docker-compose.yml \
  -f docker-compose.validate.yml \
  -f docker-compose.local-images.yml \
  run --rm demo-tests \
  npx playwright test --project=harness-demo-video \
  --grep "QuantStudio|FluoroCycler"
```

Video artifacts land in `test-results/playwright-report/` and
`test-results/test-output/`. Each test run produces one `.webm` file per
scenario. Expected THREE videos:

- `QuantStudio 7 (FILE/Excel): full E2E flow.webm` — drops
  `CVVIH 24 07 2024 serie 02 à valider.xlsx`, 95+ HIV VL results on `VIH-1` test
  code via per-row Target Name
- `QuantStudio 5 (FILE/Excel): full E2E flow.webm` — drops
  `Arbo-extraitQS5.xls`, arbovirus results split across CHIKV / DENV / ZIKV via
  per-row Target Name; STANDARD/NTC rows flagged as controls
- `Bruker FluoroCycler XT (FILE/Excel): full E2E flow.webm` — ONE analyzer
  instance, drops BOTH `HIV-result.xlsx` and `ARBOVIROSE.xlsx` in sequence;
  filename override map routes HIV file to `VIH-1` and Arbovirose file to
  `CHIKV`; both result sets appear in staging under the same analyzer row

Upload to the Madagascar Drive folder and link from the tracking Jira ticket /
Slack thread. User validates pacing + clarity; iterate on `PLAYWRIGHT_SLOWMO`
and screenshot points as needed.

### 2.11 — User validation loop

After the first recording pass:

1. User watches each video end-to-end
2. Feedback categories (if any):
   - Too fast / too slow at step N → adjust slowMo or add explicit pauses
   - Step missing evidence → add screenshot attachment
   - Result assertion too weak → tighten UI polling
   - Real file doesn't exercise a needed code path → pick a different real file
     from mnt-snapshot
3. Re-record until user approves. Commit the spec changes incrementally.

---

## Phase 2 Gate 2 — Align main-OE-repo CI / tests with reality

**Goal**: the main-repo Playwright file-import harness
(`frontend/playwright/tests/demo/harness/file-import-results.spec.ts`) and the
mock server's FILE fixtures align with the new profiles so CI runs green on
post-Phase-A code. Videos are NOT required — CI assertions on real parser output
are the validation mechanism.

**Non-goals**:

- Do NOT record videos in main-repo CI (Gate 1 owns videos via distro).
- Do NOT commit real analyzer files (same gitignore invariant).

### 2.G2.1 — Decide fixture strategy

Three options, in order of preference:

**(A) Fabricated format-grounded templates (RECOMMENDED)**: Generator script
`tools/analyzer-mock-server/scripts/generate-fixture.py` writes XLSX / CSV files
from scratch using `openpyxl`, replicating the REAL format (sheet count,
preamble length, column headers) with fully synthetic data (fake `DEV0126*`
sample IDs, fabricated CT values). Cross-checked at development time against
`docs/debug-local/mnt-snapshot/` for shape fidelity, but emits zero real bytes.
Deterministic on re-run.

- **Pro**: Zero PHI exposure. Deterministic. Edge-case control (can emit a
  fixture with one positive + two negatives that may not exist in any real
  file).
- **Con**: Requires openpyxl proficiency + a development pass to match real
  format byte-for-byte.

**(B) Delete mock FILE handler, test parser directly via unit tests**: Drop the
FILE protocol path from the mock server entirely. Main-repo Playwright FILE
tests are replaced with bridge-side unit tests that feed real-file bytes (or
fabricated format-grounded bytes) through `FileResultParser` directly, asserting
on the `List<ParsedResults>` output. UI coverage for FILE analyzer creation
stays in the main-repo Playwright but uses a canned stub for the actual
file-drop step.

- **Pro**: Simplest. Skips the passthrough rewrite entirely.
- **Con**: Loses end-to-end coverage of the HTTP FHIR forward path in CI (unit
  tests don't exercise the bridge's FHIR POST handler).

**(C) Passthrough mode on mock FILE handler**: Mock server's
`POST /simulate/file/{template}` stops pre-parsing. Returns
`{status, written_path, expected: <hand-curated JSON>}` instead of
`{metadata: {results: [...]}}`. Tests assert against OE UI / REST instead of
mock metadata. Fixtures still needed — regenerated per (A).

- **Pro**: Preserves the end-to-end HTTP path in CI.
- **Con**: Mock template + fixture regeneration on top of the passthrough
  rewrite. Most expensive.

**Decision to ask the user at Gate 2 planning time**: Which strategy? Defer
until Gate 1 is approved so the decision is informed by what Gate 1 revealed
about the parser's real behavior.

### 2.G2.2 — Implement the chosen strategy

Left intentionally vague — concrete steps depend on (A) vs (B) vs (C) decision
in 2.G2.1.

### 2.G2.3 — Delete stale synthetic fixtures

Regardless of strategy, the old hand-crafted synthetic fixtures at
`tools/analyzer-mock-server/fixtures/quantstudio5/results-demo.xlsx`,
`fixtures/quantstudio7/`, `fixtures/fluorocycler-xt/`, etc. are replaced or
deleted. The constitution's Principle X (Legacy Code Removal) applies — no
dual-track fixture sets.

### 2.G2.4 — CI green on the new spec

Iterate on the main-repo `file-import-results.spec.ts` until CI passes against
the Phase A + Gate 1 webapp + bridge code. Merge Gate 2 commits onto the
existing PR #3372 branch alongside the Gate 1 spec fixes.

---

## Phase C — Other Madagascar analyzers (out of scope this iteration)

Unchanged from v1. After QS5/QS7/Fluorocycler are green and videos are accepted,
the same recipe applies to:

- **Tecan Infinite F50** — plate-map format, French locale. New plate-map parser
  required (new work, not profile tweak).
- **Thermo Multiskan FC** — same plate-map shape; reuses Tecan parser.
- **GeneXpert Dx** (×4) — UTF-16 CSV. Deferred until ASTM/HL7 primary path
  fails.
- **Invitrogen Attune CytPix** — PDF only. Out of scope for bridge entirely.
- **DT-prime** — no exports yet. Parked.

---

## Commit cadence

- **`openelis-madagascar-distro` PR #4** — Gate 1 commits:
  - Remove archive/error dead code from `create-analyzer-from-profile.ts`
    - `analyzer-form.ts` (2.2)
  - Add `defaultTestCode` to form fixture + `AnalyzerTestConfig` + helper (2.3)
  - Update `analyzer-demo-flow.spec.ts` CONFIGS + add Fluorocycler Arbo
    - multi-observer (2.4)
  - Add `drop-real-analyzer-file.ts` helper (2.5)
  - Add pacing + screenshot attachments (2.7)
- **`OpenELIS-Global-2` PR #3372** — Gate 1 commits:
  - Add `defaultTestCode` UI field to `AnalyzerForm.jsx` + `en.json` (2.1)
- **Both repos** — Gate 2 commits after Gate 1 approved:
  - Main-repo `file-import-results.spec.ts` alignment (2.G2)
  - Mock server fixture regeneration or handler rework (2.G2)

Each commit on its own, pushed to the existing branch. No new PRs.

---

## Verification (what "done" looks like)

**Gate 1 exit criteria** (user validates via video review):

- [ ] Distro stack restarted from scratch on new `:local` images; webapp +
      bridge + mock + proxy all healthy
- [ ] New DB catalog rows confirmed: HIVVIRALLOAD / DENGUEPCR / CHIKVPCR /
      ZIKVPCR with expected LOINCs
- [ ] New `clinlims.analyzer.default_test_code` column confirmed
- [ ] New `clinlims.analyzer.default_test_code_overrides` column confirmed
      (TEXT, populated from profile JSON on Fluorocycler instance)
- [ ] `AnalyzerForm.jsx` shows the new `defaultTestCode` TextInput in Section 3b
      when FILE protocol is selected
- [ ] Distro `harness-demo-video` project runs the three FILE scenarios without
      failing at the helper's form-fill stage (archive/error dead code removed)
- [ ] **Video 1 — QuantStudio 7 HIV VL**: admin creates analyzer from
      `QuantStudio QS5/QS7` profile, drops the real
      `CVVIH 24 07 2024 serie 02 à valider.xlsx` via
      `drop-real-analyzer-file.ts`, bridge logs
      `FileResultParser: extracted N results`, results appear in OE staging with
      per-row test codes (`VIH-1`, `IC`), admin accepts.
- [ ] **Video 2 — QuantStudio 5 Arbovirus**: same flow with
      `Arbo-extraitQS5.xls`; CHIKV + DENV + ZIKV target rows appear in staging;
      `isControl=true` rows (STANDARD, NTC) filtered from the default view;
      admin accepts.
- [ ] **Video 3 — Fluorocycler XT (unified)**: admin creates a SINGLE
      Fluorocycler instance from the `Bruker FluoroCycler XT` profile (which
      auto-populates `default_test_code_overrides` from the profile JSON). Test
      body drops `HIV-result.xlsx` FIRST; rows process with
      `defaultTestCode=VIH-1` (matched via `HIV*.xlsx` override glob); results
      appear in staging. Test body then drops `ARBOVIROSE.xlsx` into the SAME
      directory on the SAME instance; rows process with `defaultTestCode=CHIKV`
      (matched via `ARBO*.xlsx` override glob); results appear in staging under
      the same analyzer row; admin accepts both result batches.
- [ ] Non-destructive invariant: after each drop, the real file STILL EXISTS in
      the watched directory (verified via
      `docker exec openelis-analyzer-bridge ls /data/analyzer-imports/...`); no
      `.error` / `.failed` sidecars written
- [ ] `/admin/file-state` REST endpoint shows PROCESSED rows for each dropped
      file (content hash + analyzer ID keyed)
- [ ] Videos uploaded to Madagascar Drive + linked from tracker; user approves

**Gate 2 exit criteria** (to be planned after Gate 1):

- [ ] Main-repo `file-import-results.spec.ts` asserts on real parser output
- [ ] Mock fixtures regenerated / deleted / replaced per 2.G2.1 decision
- [ ] CI green on PR #3372 analyzer-e2e workflow
- [ ] Constitution Principle V (no assert-on-mock-return) satisfied

---

## Open questions (non-blocking, tracking only)

1. **QS7 sheet choice**: `CVVIH` file has `Results` (row 50 header) and
   `Results (2)` (row 0 header). Which does the workflow rely on? Default:
   `Results`.
2. **"à valider" filename semantics**: tech-validation state or just naming
   convention? Default: ignore filename, parse file.
3. **Fluorocycler Arbovirose test-code model**: the Arbovirose file has no
   per-row target column, but the lab runs a multiplex assay that tests CHIKV +
   DENV + ZIKV simultaneously per sample. For Gate 1 the profile's
   `default_test_code_overrides` maps `ARBO*.xlsx` → `CHIKV` as a pragmatic
   catchall — CHIKV is already wired to `CHIKVPCR(Plasma)` with individual LOINC
   `60260-7` in the distro catalog CSV (shipped in Phase A.0). All Arbo file
   rows get recorded under the CHIKV test; the tech reviews the `Result`
   column's narrative text (which mentions CHIKV/DENV/ZIKV individually) to see
   per-target interpretation. If Mekom prefers a proper panel test, we add an
   `ARBOPANEL` CSV row with LOINC `81154-7` and switch the override value — a
   follow-up commit, not a blocker. If they prefer three separate result rows
   per sample, the parser needs a split-row feature deferred to Phase C.
   - **Default for Gate 1**: `CHIKV` (reuse existing CHIKVPCR row). Confirm with
     Mekom before Gate 2 closes.
4. **Mekom LOINC confirmation**: validated individual LOINCs against loinc.org
   (20447-9 / 60260-7 / 7855-0 / 85622-9). Confirm Mekom's reference catalog
   uses the same codes.
5. **`docker cp` requires sudo in this environment?** — the existing distro
   helper at `create-analyzer-from-profile.ts:599` tries `sudo docker` first and
   falls back to `docker` without sudo. The new `drop-real-analyzer-file.ts`
   should use the same fallback pattern.

---

## History — shipped before this iteration (reference only)

### Phase 1 — Non-destructive bridge ✅ COMPLETE (2026-04-10 morning)

Herbert's 2026-04-09 incident report where real files were disappearing from
`/mnt` kicked off a containment workstream. Root cause: the old bridge deleted /
moved / wrote `.failed` sidecars to the watched directory. Fix shipped across:

- **`openelis-analyzer-bridge` PR #34** — sqlite-jdbc dep, `FileStateStore` (WAL
  mode, corruption recovery, 10 unit tests), non-destructive `FileWatcher`
  rewrite (16 invariant tests), `/admin/file-state` REST endpoint (9 MockMvc
  tests). 418 tests green before A.3.7 added 5 more.
- **`OpenELIS-Global-2` PR #3372** — inventory manifest,
  `AnalyzerResultsServiceImpl` upsert contract documentation + tests (three-case
  dedupe), archive/error directory drop + Liquibase 012.
- **`openelis-madagascar-distro` PR #4** — `:ro` mount containment,
  `bridge-state` named Docker volume, deploy runbook.

**Invariants** now guaranteed:

- Bridge never deletes / moves / writes sidecars to the watched directory
- Processing state in SQLite `FileStateStore` keyed on
  `(analyzerId, contentHash)` with PROCESSED / FAILED_NEEDS_HANDLING / RETRYING
  states
- Retry backoff survives JVM restart
- Corrupt state store auto-recovers via rename-to-corrupt + fresh init
- Admin endpoint `GET /admin/file-state` exposes operator-visible state
- Distro mounts host analyzer path `:ro` so the bridge can't write even if a
  future bug tried to

### Phase A — Profiles + catalog + plumbing + multi-observer ✅ COMPLETE (2026-04-10 afternoon)

Summarized in the "Completed" section at the top. 9 commits across 3 PRs. 441
bridge tests + webapp compile green. Unit coverage for all shipped changes.

### Legacy plugins — reference only, do not integrate

`plugins/` submodule contains `QuantStudio3`, `QuantStudio7Flex`,
`FluoroCyclerXT`, `GeneXpertFile` implementations. These are webapp-side
extensions for PREVIOUS workflows that operate on pre-loaded `List<String>`
lines and cannot handle xlsx directly. Hardcoded to SARS-CoV-2 LOINCs. **Do not
integrate or import** per user direction 2026-04-10 — consulted only as
reference for parsing patterns.
