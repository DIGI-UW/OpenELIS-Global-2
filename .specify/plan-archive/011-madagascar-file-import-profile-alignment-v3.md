# Plan v3 — Madagascar Analyzer File-Import: Distro Video Validation + Bridge Admin Upload UI

> **v3 pivot (2026-04-10 late evening)**: After the three research agents
> shipped their findings (Bruker format, legacy plugin, real file archaeology)
> and Gate 1 §2.1-§2.3 landed, we pivot on two points:
>
> 1. **Fluorocycler scope narrows to HIV VL only.** The ARBOVIROSE.xlsx file is
>    a human-edited WPS Sheets workbook (creator: `eldin`) with multiplex
>    results packed into inconsistently-delimited free-text cells and no
>    target-order metadata. The `default_test_code_overrides` plumbing was a
>    solution to the wrong problem and has been **reverted**. Arbovirose routing
>    is a product decision for Mekom (reconfigure AssayArea export, accept
>    composite catchall, or move to ASTM/HL7), and is deferred entirely out of
>    Gate 1.
>
> 2. **File drop mechanism becomes a real bridge admin UI, not a docker cp test
>    hack.** The v2 plan's §2.5 proposed shelling out to `docker cp` from inside
>    the Playwright runner — fragile, environment-dependent, and not a real user
>    workflow. Replacement: build a small HTTP admin UI on the bridge at
>    `GET /admin/upload` that a lab tech (or test-harness Playwright) can use to
>    select a registered FILE analyzer and upload a real file into its watched
>    directory. This is a real production feature — labs without NFS/mount
>    access to the analyzer filesystem can use it as a manual import path — AND
>    it doubles as the honest E2E video path: admin configures analyzer in OE →
>    opens bridge upload UI → selects analyzer + file → clicks Upload → bridge
>    writes to watched dir → FileWatcher processes → results in OE staging →
>    admin accepts. No shortcuts. The test runner gets file access via an
>    `ANALYZER_HOST_MOUNT` bind mount added to the `demo-tests` service in
>    `docker-compose.validate.yml` (matches the same bind mount already on
>    webapp + bridge from Phase 1).
>
> **What stays from v2**: test catalog CSV, QuantStudio profile mappings,
> Fluorocycler v2.0.0 profile (the HIV-VL half still works), scalar
> `defaultTestCode` form field, archive/error helper cleanup, the distro harness
> flow structure, rebuild + restart + video steps.
>
> **What's new in v3**: bridge `FileUploadController` + static HTML form
>
> - JSON analyzer endpoint (§2.5a/b), one-line bind mount in the distro validate
>   compose (§2.5c), `drop-real-analyzer-file.ts` rewritten to drive the bridge
>   UI via Playwright instead of `docker cp` (§2.5d).

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

### Gate 1 commits shipped this session (2026-04-10 evening, pre-pivot)

- **`OpenELIS-Global-2` PR #3372** — `756266369` §2.1
  `feat(analyzer-form): add Default Test Code input for FILE analyzers`. Exposed
  the scalar `Analyzer.defaultTestCode` field (Phase A.3.6) via a new TextInput
  in `AnalyzerForm.jsx` Section 3b, wired formData state + profile-apply
  auto-fill + non-FILE clear-to-null + save serialization + `en.json` i18n keys
  (`analyzer.form.defaultTestCode` +
  `analyzer.form.defaultTestCode.helperText`).
- **`openelis-madagascar-distro` PR #4** — `a9d9910` §2.2 + §2.3
  `test(playwright): remove archive/error dead code, add defaultTestCode support`.
  Deleted `buildFileImportDirectories` helper and the `fillArchiveDirectory` /
  `fillErrorDirectory` calls that targeted removed form fields (Phase 1
  Liquibase 012). Added `defaultTestCodeInput` locator + `fillDefaultTestCode`
  method to `analyzer-form.ts` fixture. Extended `AnalyzerTestConfig` with an
  optional `defaultTestCode` field. Wired the helper to fill the field when a
  config has it set. Clears the distro FILE test flow of the "form-fill stage
  crash against post-Phase-1 webapp images" blocker.

### Work reverted (uncommitted) this session

- **`default_test_code_overrides` end-to-end plumbing** — the v2 plan's §2.0
  proposed a filename-glob → testCode map threaded through webapp entity
  (Liquibase 014 + getter/setter + REST + callers + FileImportServiceImpl) and
  bridge (AnalyzerEntry + AnalyzerRegistryBootstrap + FileMessageHandler).
  Research agents showed this was a solution to the wrong problem —
  ARBOVIROSE.xlsx is a human-edited WPS Sheets workbook with no machine-readable
  target ordering, so filename routing can't produce clinically correct
  per-target result rows. The 7-file diff was reverted via
  `git checkout HEAD --` and the new Liquibase 014 file was deleted.
  `git status` is clean back to Phase A baseline.

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

For Gate 1, the Fluorocycler XT scope narrows to HIV VL only (v3 pivot) — the
file has a single assay ("HIV-1"), so the scalar `defaultTestCode=VIH-1` set on
the analyzer instance handles every row. Arbovirose routing is a deferred Mekom
decision (see §2.0 below for the reverted reasoning trail). Without the scalar
field exposed in the unified form, admins cannot configure VIH-1 via the true
user workflow — they'd have to reach around the UI with a direct REST API PUT.
Gate 1 adds the TextInput in §2.1 (already shipped as commit `756266369`).

---

## Phase 2 Gate 1 — Distro video validation

**Goal**: produce **three** video recordings of the distro stack ingesting real
analyzer files end-to-end via the true user workflow: admin creates analyzer via
OE AnalyzerForm → opens the bridge `/admin/upload` UI in a new tab → selects
analyzer + real file from `/mnt` → clicks Upload → bridge writes the file to the
watched directory → FileWatcher processes → results appear in OE staging → admin
accepts. Videos land in `test-results/playwright-report/`. User validates,
iterates on pacing / clarity, re-records as needed.

**Non-goals for Gate 1** (v3 pivot):

- Do NOT rewrite the main-repo Playwright harness (that's Gate 2).
- Do NOT write a synthetic fixture generator (Gate 1 uses real files from the
  `/mnt` bind mount).
- Do NOT commit real analyzer files to any repo (they stay on the host's `/mnt`
  — or dev-host `docs/debug-local/mnt-snapshot/`).
- Do NOT remove the mock server's FILE handler — it's off the Gate 1 critical
  path but still used by TCP/ASTM/HL7 scenarios.
- Do NOT create two Fluorocycler analyzer instances — one machine, one instance.
  The A.3.7 multi-observer refactor stays as a general capability, not exercised
  here.
- **Do NOT attempt to parse ARBOVIROSE.xlsx** — the file is a human-edited WPS
  Sheets workbook with no per-row target column and no machine-readable target
  ordering. Deferred to Mekom for a product decision (AssayArea reconfiguration,
  composite catchall, or ASTM/HL7 transport). Gate 1 Fluorocycler scope is HIV
  VL only.
- Do NOT ship `default_test_code_overrides` — the v2 plumbing was reverted. See
  §2.0 below for the trail.

### 2.0 — ~~`default_test_code_overrides` filename routing~~ REVERTED (v3 pivot)

**Status**: REVERTED in the v3 pivot. The v2 plan's §2.0 proposed extending the
profile JSON + Analyzer entity (+ Liquibase 014) + REST serializer +
BridgeRegistrationService signature + AnalyzerEntry +
AnalyzerRegistryBootstrap + FileMessageHandler with a filename-glob → testCode
map. I started building it in this session (7 webapp files touched +
014-analyzer-default-test-code-overrides.xml created) and reverted the entire
diff via `git checkout HEAD --` after the research agents shipped.

**Why reverted** — three independent findings converged:

1. **File archaeology (Agent 3)**: `ARBOVIROSE.xlsx` is a human-edited WPS
   Sheets workbook (creator: `eldin`, app: `WPS Sheets`, Chinese locale
   `HeadingPairs`). The delimiter is inconsistent — the `CPOS` row at `!E14`
   uses `)-` with no leading space, while other rows use ` -`. No defined names,
   no hidden sheets, no custom doc props. The file has round-tripped through a
   third-party Office suite and any original FluoroSoftware metadata is gone.
2. **Bruker research (Agent 1)**: no public documentation for the FluoroSoftware
   export schema exists. "CP" / "Calc. Conc." are Roche LightCycler 480
   vocabulary Bruker inherited. The Arbovirose multiplex is almost certainly a
   lab-developed test (LDT) run via FluoroSoftware's `AssayArea` module, not a
   Bruker catalog kit. The CHIKV/DENV/ZIKV target order is defined by whoever
   configured the AssayArea template at LA2M — NOT declared in the file and NOT
   standardized by any accessible document. Zero prior art: no FOSS LIMS has
   ever integrated Fluorocycler XT.
3. **Legacy plugin (Agent 2)**: the legacy `FluoroCyclerXT` plugin in
   `plugins/analyzers/FluoroCyclerXT/` never did filename routing, never split
   multiplex results into per-target rows, never handled XLSX, and was designed
   for a completely different 4-column semicolon-CSV format that LA2M doesn't
   produce. The plugin's model was "catchall + tech interprets free-text" — not
   per-target routing.

**What replaces it**: Fluorocycler scope in Gate 1 narrows to HIV VL only. The
HIV file has a real single-assay shape — every row is an HIV-1 result, and the
scalar `defaultTestCode=VIH-1` (from Phase A.3.6) already handles this cleanly.
Arbovirose is a deferred Mekom product decision:

- (a) reconfigure AssayArea to emit per-target rows (fixes the root problem but
  depends on Mekom + Bruker support)
- (b) accept composite-panel handling (a new `ARBOPANEL` CSV row with LOINC
  `81154-7`, tech reads the raw cell text for per-target interpretation)
- (c) move Fluorocycler to ASTM/HL7 transport and drop file-based integration
  entirely

Tracked as an open question below. Not Gate 1 work.

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
- **`Demo: Bruker FluoroCycler XT (HIV VL)`** — ONE analyzer instance,
  `profileName: "Bruker FluoroCycler XT"`, `filePattern: "HIV*.xlsx"`,
  `defaultTestCode: "VIH-1"`, `realFileSourcePath` pointing at
  `${ANALYZER_HOST_MOUNT}/la2m/central/analyzers_results/Fluorocycler-XT/HIV-result.xlsx`
  (resolved at test runtime via `process.env.ANALYZER_HOST_MOUNT`, defaulting to
  `/mnt`). The test body drops the file via the new bridge admin upload UI (§2.5
  below), waits for the bridge FileWatcher poll cycle to detect and process the
  file, and verifies ≥ 1 HIV VL result row appears in OE staging under the
  Fluorocycler analyzer's dashboard. The admin then accepts the results through
  the normal OE accession-review flow. **Arbovirose is NOT in Gate 1** — see the
  rationale block below.

**Rationale** (reality, per user direction 2026-04-10 late evening, v3 pivot):
the Fluorocycler XT Arbovirose file (`ARBOVIROSE.xlsx`) is a human-edited WPS
Sheets workbook with no per-row target column, no machine-readable target
ordering, and inconsistent delimiters. Every proposed programmatic parsing
strategy (filename overrides, result-text regex, multiplex split) was a
workaround for a file that isn't a proper machine export. Arbovirose routing is
deferred to Mekom as a product/ops question: either reconfigure AssayArea to
emit per-target rows, accept a composite catchall test, or move Fluorocycler to
ASTM/HL7 transport. Not Gate 1 work. The Fluorocycler HIV-result.xlsx file, by
contrast, IS a reasonable shape (single assay, single target, Calc. Conc. +
Result columns with LC480-lineage semantics) and ships cleanly via the scalar
`defaultTestCode=VIH-1` path that already landed in Phase A.3.6.

### 2.5 — Build a lightweight bridge admin file-upload UI

**Rationale (v3 pivot)**: the v2 plan's §2.5 proposed shelling out to
`docker cp` from inside the Playwright runner to stage files into the bridge's
watched directory. That's fragile (requires `sudo` + docker socket + OS-specific
paths), not reproducible across environments, and it's NOT a real user workflow
— no lab tech would ever SSH into a container to drop a file. The replacement is
a small HTTP admin UI on the bridge at `GET /admin/upload` that an admin (or
Playwright test) uses to:

1. Select a registered FILE analyzer from a dropdown (populated by calling the
   existing `AnalyzerRegistryConfig.getRegisteredAnalyzers()` public API
   filtered to `expectedProtocol == "FILE"`)
2. Pick a file via a standard HTML `<input type="file">` element
3. Click Upload → browser POSTs multipart form to `/admin/upload`
4. Bridge validates the analyzer id, writes the file using its original filename
   into that analyzer's `importDirectory`, returns a success HTML banner

This is a **real production feature** — labs without an NFS/SMB mount to the
analyzer output filesystem can use it as a manual import path (the ops team
already asked about this use case for sites where Herbert's NFS-to-bridge wiring
isn't available). It **also** doubles as the honest Gate 1 video path: the
Playwright test navigates to the bridge UI with HTTP Basic creds, uses
`setInputFiles()` against the file picker, clicks the submit button, waits for
the success banner, then switches back to OE to verify results landed in
staging.

The bridge's existing `FileWatcher` handles self-writes natively: files written
by the bridge process into a watched directory are detected on the next polling
cycle (default 1000 ms dev / 5000 ms prod) and processed identically to files
dropped externally. No loop safeguards, no race conditions — confirmed by
Explore agent against
`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileWatcher.java`
lines 26 (polling model) and 430-438 (stability checker). No FileWatcher code
changes required.

**Critical files to create**:

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/controller/FileUploadController.java`
  (new, ~120 LOC) — §2.5a below
- `tools/openelis-analyzer-bridge/src/main/resources/static/admin/upload/index.html`
  (new, ~60 LOC HTML + inline JS) — §2.5b below
- `tools/openelis-analyzer-bridge/src/test/java/org/itech/ahb/controller/FileUploadControllerTest.java`
  (new, 5-7 MockMvc tests) — §2.5a

**Critical files to modify**:

- `openelis-madagascar-distro/docker-compose.validate.yml` — add one bind-mount
  line to the `demo-tests` service (§2.5c)
- `openelis-madagascar-distro/tests/playwright/playwright/helpers/drop-real-analyzer-file.ts`
  (new, ~50 LOC) — Playwright helper that drives the bridge UI (§2.5d)
- `openelis-madagascar-distro/tests/playwright/playwright/helpers/push-analyzer-result.ts`
  — add a FILE dispatch branch that calls the new helper when config has
  `realFileSourcePath` (§2.5d)

#### 2.5a — `FileUploadController` Java implementation

Pattern: follows the existing `/admin/file-state` controller at
`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/controller/FileStateController.java`
(found by Explore agent, lines 39-142). Same `@RestController` annotation, same
`@RequestMapping` style, same Spring Security
`.requestMatchers("/admin/**").authenticated()` coverage at
`SecurityConfig.java:102` (so HTTP Basic auth is inherited for free — no new
security rule).

Endpoints:

```java
@RestController
@RequestMapping("/admin/upload")
public class FileUploadController {

    private final AnalyzerRegistryConfig registry;
    private final FileConfig fileConfig;

    public FileUploadController(AnalyzerRegistryConfig registry,
                                 FileConfig fileConfig) {
        this.registry = registry;
        this.fileConfig = fileConfig;
    }

    /**
     * List FILE analyzers available as upload targets. Used by the
     * admin UI's analyzer dropdown. Returns: analyzerId, name,
     * watchDirectory, filePattern, defaultTestCode so the UI can show
     * helpful context ("Watch: /data/analyzer-imports/fluorocycler/incoming").
     */
    @GetMapping("/analyzers")
    public ResponseEntity<List<Map<String, Object>>> listFileAnalyzers() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, AnalyzerRegistryConfig.AnalyzerEntry> entry :
                registry.getRegisteredAnalyzers().entrySet()) {
            AnalyzerRegistryConfig.AnalyzerEntry a = entry.getValue();
            if (!"FILE".equalsIgnoreCase(a.getExpectedProtocol())) continue;
            result.add(Map.of(
                "id", a.getId(),
                "name", a.getName() != null ? a.getName() : a.getId(),
                "watchDirectory", entry.getKey(),
                "filePattern", a.getFilePattern() != null ? a.getFilePattern() : "*",
                "defaultTestCode", a.getDefaultTestCode() != null ? a.getDefaultTestCode() : ""
            ));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Multipart upload. Writes `file` to the selected analyzer's
     * watchDirectory using the uploaded filename. Returns an HTML
     * success banner (Content-Type text/html) so the browser shows
     * the result inline without a JSON deserializer.
     *
     * Validation:
     * - analyzerId present and maps to a registered FILE analyzer
     * - file not empty
     * - file size under 20 MB (bridge.upload.max-size-mb, default 20)
     * - filename does not contain path separators (security)
     * - target directory exists and is writable
     */
    @PostMapping(consumes = "multipart/form-data",
                 produces = "text/html")
    public ResponseEntity<String> uploadFile(
            @RequestParam("analyzerId") String analyzerId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // 1. lookup analyzer, validate FILE protocol
        // 2. validate file (non-empty, safe filename, size)
        // 3. write to watchDirectory using Files.write(target, bytes, CREATE_NEW)
        //    — CREATE_NEW refuses to overwrite existing files by that name;
        //    if a duplicate drop is needed, operator renames at upload time
        // 4. return success HTML banner including the resolved target path
        //    and an "Upload another file" link back to /admin/upload
    }
}
```

Tests (`FileUploadControllerTest.java` using `@WebMvcTest` + MockMvc):

1. `GET /admin/upload/analyzers` returns 200 + JSON list of FILE analyzers only
   (TCP analyzers excluded)
2. `POST /admin/upload` with valid analyzerId + non-empty file writes the file
   and returns 200 + HTML banner
3. `POST /admin/upload` with unknown analyzerId returns 400
4. `POST /admin/upload` with empty file returns 400
5. `POST /admin/upload` with a filename containing `../` returns 400 (path
   traversal)
6. `POST /admin/upload` to a non-FILE analyzer (TCP) returns 400
7. `POST /admin/upload` with file size > 20 MB returns 400
8. Auth: GET /admin/upload without credentials returns 401 (inherited from
   existing security rule — prove via test that the rule covers the new
   endpoint)

#### 2.5b — Static HTML form at `static/admin/upload/index.html`

Single self-contained HTML file served by Spring Boot's default static resource
handler (`src/main/resources/static/` → `/static/` path). URL:
`GET /admin/upload/index.html` with HTTP Basic auth enforced by the `/admin/**`
rule. The controller's `GET /admin/upload` (no suffix) can do a server-side
redirect to `/admin/upload/index.html` for convenience.

Structure:

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>Analyzer File Upload — OpenELIS Bridge Admin</title>
    <style>
      body {
        font-family: system-ui, sans-serif;
        max-width: 640px;
        margin: 2rem auto;
        padding: 0 1rem;
        color: #222;
      }
      h1 {
        font-size: 1.25rem;
        margin-bottom: 1rem;
      }
      .card {
        border: 1px solid #ccc;
        border-radius: 6px;
        padding: 1rem;
      }
      label {
        display: block;
        margin-top: 0.75rem;
        font-weight: 600;
      }
      select,
      input[type="file"] {
        width: 100%;
        margin-top: 0.25rem;
      }
      .watch-path {
        font-family: monospace;
        color: #555;
        font-size: 0.85em;
      }
      button {
        margin-top: 1rem;
        padding: 0.5rem 1rem;
      }
      .banner {
        padding: 0.75rem;
        border-radius: 4px;
        margin-bottom: 1rem;
      }
      .banner.success {
        background: #d4edda;
        border: 1px solid #c3e6cb;
      }
      .banner.error {
        background: #f8d7da;
        border: 1px solid #f5c6cb;
      }
    </style>
  </head>
  <body>
    <h1>Analyzer File Upload — Bridge Admin</h1>
    <div id="banner"></div>
    <div class="card">
      <form
        id="upload-form"
        enctype="multipart/form-data"
        method="post"
        action="/admin/upload"
      >
        <label for="analyzer-select">Analyzer</label>
        <select name="analyzerId" id="analyzer-select" required></select>
        <div class="watch-path" id="watch-path"></div>

        <label for="file-input">File</label>
        <input type="file" name="file" id="file-input" required />

        <button type="submit">Upload File</button>
      </form>
    </div>

    <script>
      // Fetch analyzer list on page load, populate dropdown, show watch path
      async function loadAnalyzers() {
        const res = await fetch("/admin/upload/analyzers", {
          credentials: "include",
        });
        const analyzers = await res.json();
        const select = document.getElementById("analyzer-select");
        const watch = document.getElementById("watch-path");
        for (const a of analyzers) {
          const opt = document.createElement("option");
          opt.value = a.id;
          opt.textContent = a.name;
          opt.dataset.watchDirectory = a.watchDirectory;
          opt.dataset.filePattern = a.filePattern;
          select.appendChild(opt);
        }
        const showWatch = () => {
          const opt = select.options[select.selectedIndex];
          if (!opt) return;
          watch.textContent =
            "Watch: " +
            opt.dataset.watchDirectory +
            "   Pattern: " +
            opt.dataset.filePattern;
        };
        select.addEventListener("change", showWatch);
        showWatch();
      }
      loadAnalyzers();
    </script>
  </body>
</html>
```

Intentionally minimal: no framework, no bundler, no npm. Total < 80 LOC. Lives
in the bridge resources and is served via Spring Boot's automatic static
resource handling. No Thymeleaf. The form's native POST action handles the
multipart submission; the HTML banner returned by the controller replaces the
page (browser follows the POST's HTML response, not a JSON redirect).

#### 2.5c — Add `/mnt` bind mount to `demo-tests` in distro compose

The Playwright runner container (`demo-tests` service in
`openelis-madagascar-distro/docker-compose.validate.yml:83-127`) currently
mounts only `/var/run/docker.sock` and `./test-results`. To let the runner's
`setInputFiles()` call pick up real analyzer files that live on the host's
`/mnt` (on Herbert's UAT machine) or at a dev-only path like
`docs/debug-local/mnt-snapshot/` (locally), we add the same bind mount the base
compose already uses on webapp + bridge (see `docker-compose.yml:115` and
`:219`):

```yaml
# openelis-madagascar-distro/docker-compose.validate.yml, demo-tests service:
  demo-tests:
    ...
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./test-results:/work/test-results
      # Host filesystem for real analyzer file drops — read-only, matches
      # the same env-var-driven bind mount on webapp + bridge from Phase 1.
      # On Herbert's UAT box: ANALYZER_HOST_MOUNT=/mnt (default) → /mnt:/mnt:ro
      # On dev: export ANALYZER_HOST_MOUNT=/home/you/OpenELIS-Global-2/docs/debug-local/mnt-snapshot
      #         → that path appears inside the container at the same path
      # Playwright tests read from `${ANALYZER_HOST_MOUNT:-/mnt}/la2m/central/analyzers_results/...`
      - ${ANALYZER_HOST_MOUNT:-/mnt}:${ANALYZER_HOST_MOUNT:-/mnt}:ro
```

One line. Zero new env vars. Symmetrical with the existing
`docker-compose.yml:115` (webapp) and `:219` (bridge) bind mounts, so the runner
sees the same files at the same paths the other containers see, which makes
debugging trivial.

**Why this matches the user's intuition** ("i thought we had /mnt:/mnt"): we DID
— on webapp and bridge. This sub-section just extends the same bind mount to the
demo-tests container. No new volume primitive.

#### 2.5d — Playwright helper to drive the bridge upload UI

New file
`openelis-madagascar-distro/tests/playwright/playwright/helpers/drop-real-analyzer-file.ts`:

```typescript
/**
 * Drop a real analyzer file by driving the bridge's /admin/upload UI
 * with Playwright — the "true user workflow" path: same UI a lab tech
 * without NFS access would use manually.
 *
 * Preconditions:
 *   - Bridge is running and reachable at BRIDGE_URL (default
 *     https://openelis-analyzer-bridge:8443 inside the compose network)
 *   - Analyzer has been created via the OE AnalyzerForm and registered
 *     with the bridge via BridgeRegistrationService.registerFile (so it
 *     appears in the /admin/upload/analyzers dropdown)
 *   - The source file exists at a path Playwright can read (see §2.5c —
 *     the demo-tests container bind-mounts ${ANALYZER_HOST_MOUNT:-/mnt})
 */
export async function dropRealAnalyzerFileViaBridgeUI(
  page: Page,
  opts: {
    analyzerId: string;
    sourcePath: string; // absolute path inside the demo-tests container
    presentation: DemoPresentation;
  }
): Promise<void> {
  const bridgeUrl =
    process.env.BRIDGE_URL ?? "https://openelis-analyzer-bridge:8443";
  const creds = {
    username: process.env.BRIDGE_USER ?? "admin",
    password: process.env.BRIDGE_PASS ?? "adminADMIN!",
  };

  // Open the bridge upload UI in a new tab with HTTP Basic creds.
  // Playwright supports httpCredentials on context, or via URL auth.
  const ctx = await page.context().browser()?.newContext({
    httpCredentials: creds,
    ignoreHTTPSErrors: true,
  });
  if (!ctx)
    throw new Error("Failed to create new browser context for bridge UI");
  const uploadPage = await ctx.newPage();

  await opts.presentation.step("Opening bridge admin upload UI in a new tab");
  await uploadPage.goto(`${bridgeUrl}/admin/upload/index.html`);
  await uploadPage.waitForSelector("#analyzer-select option[value]");

  // Select the target analyzer by id
  await uploadPage.selectOption("#analyzer-select", opts.analyzerId);
  await opts.presentation.pause(750);

  // Pick the real file from the bind-mounted /mnt path
  await uploadPage.setInputFiles("#file-input", opts.sourcePath);
  await opts.presentation.pause(750);

  // Submit
  await uploadPage.click("button[type='submit']");

  // Wait for success banner — controller returns HTML with a
  // `.banner.success` class on successful upload
  await uploadPage.waitForSelector(".banner.success", { timeout: 10_000 });

  await opts.presentation.pause(1_500);
  await uploadPage.close();
  await ctx.close();
}
```

The existing `push-analyzer-result.ts` dispatcher gets a new FILE branch that
calls this helper when a config has `realFileSourcePath` set, instead of the
mock-server `POST /simulate/file/{template}` route. The mock branch stays for
backward compat with any TCP/ASTM/HL7 tests that still use it.

**Assertion strategy**: weak for Gate 1. The test waits for the success banner
on the bridge UI (proves the file landed on disk), then navigates back to the OE
Analyzer Results page and asserts that at least 1 result row appears under the
correct analyzer within a polling timeout. Exact accession matching is deferred
to Gate 2 when CI needs tight assertions. Videos are the primary proof for
Gate 1.

### 2.6 — Mock FILE handler (no change for Gate 1)

Mock server's `POST /simulate/file/{template}` stays untouched for Gate 1. The
pivot to the bridge upload UI means mock FILE templates are no longer on the
Gate 1 critical path. Mock fixture regeneration is a Gate 2 concern if we decide
to keep the mock FILE handler at all.

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
      ZIKVPCR with expected LOINCs (from Phase A.0)
- [ ] `clinlims.analyzer.default_test_code` column confirmed (from Phase A.3.6);
      **no** `default_test_code_overrides` column — that plumbing was reverted
      in the v3 pivot
- [ ] `AnalyzerForm.jsx` shows the new `defaultTestCode` TextInput in Section 3b
      when FILE protocol is selected (§2.1, committed as `756266369` on PR
      #3372)
- [ ] Bridge `/admin/upload/index.html` loads with HTTP Basic auth, fetches the
      registered FILE analyzer list via `GET /admin/upload/analyzers`, and shows
      a working dropdown with watch path + pattern metadata (§2.5a/b)
- [ ] Bridge `FileUploadController` tests pass: 7 MockMvc cases covering
      success, unknown analyzer, empty file, path traversal, TCP-analyzer
      rejection, oversized file, auth (§2.5a)
- [ ] `docker-compose.validate.yml` demo-tests service has the
      `${ANALYZER_HOST_MOUNT:-/mnt}:${ANALYZER_HOST_MOUNT:-/mnt}:ro` bind mount
      added (§2.5c); Playwright inside the container can
      `ls /mnt/la2m/central/analyzers_results/` and see the real files
- [ ] Distro `harness-demo-video` project runs the three FILE scenarios without
      failing at the helper's form-fill stage (archive/error dead code was
      removed in `a9d9910`)
- [ ] **Video 1 — QuantStudio 7 HIV VL**: admin creates analyzer from
      `QuantStudio QS5/QS7` profile via OE AnalyzerForm, opens the bridge
      `/admin/upload` UI in a new tab, selects the QS7 analyzer from the
      dropdown, uses the file picker to pick
      `CVVIH 24 07 2024 serie 02 à valider.xlsx` from the bind-mounted `/mnt`
      path, clicks Upload, sees the success banner showing the file landed in
      the watched directory. Admin returns to OE, sees results appear in staging
      with per-row test codes (`VIH-1`, `IC`), accepts them.
- [ ] **Video 2 — QuantStudio 5 Arbovirus**: same flow with
      `Arbo-extraitQS5.xls`; CHIKV + DENV + ZIKV target rows appear in staging
      (per-row Target Name column on the QS file); admin accepts.
- [ ] **Video 3 — Fluorocycler XT HIV VL only**: admin creates a Fluorocycler XT
      instance from the `Bruker FluoroCycler XT` profile with
      `defaultTestCode=VIH-1` (the new TextInput). Opens bridge upload UI,
      selects Fluorocycler analyzer, picks `HIV-result.xlsx` via file picker,
      uploads. Rows process with `defaultTestCode=VIH-1`, results appear in
      staging under the Fluorocycler analyzer, admin accepts. Arbovirose is
      explicitly not tested — a card in the video's closing title explains it's
      a deferred Mekom decision.
- [ ] Non-destructive invariant: after each upload, the real file STILL EXISTS
      in the watched directory and the source file on `/mnt` is untouched
      (verified via
      `docker exec openelis-analyzer-bridge ls /data/analyzer-imports/...` +
      `stat` on the host); no `.error` / `.failed` sidecars written
- [ ] `/admin/file-state` REST endpoint shows PROCESSED rows for each uploaded
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
3. **Fluorocycler Arbovirose — product decision for Mekom** (v3 pivot, out of
   Gate 1 scope): the Arbovirose file (`ARBOVIROSE.xlsx`) is a human-edited WPS
   Sheets workbook with no per-row target column and inconsistent ` -` vs `)-`
   delimiters in the multiplex Result column. Research showed no programmatic
   parsing strategy respects reality — filename routing, result-text regex, and
   multiplex split all require metadata the file doesn't contain. Ask Mekom to
   choose:
   - (a) reconfigure AssayArea to emit a proper per-target export
     (CHIKV/DENV/ZIKV each in their own row with their own target column),
     eliminating the multiplex-in-one-cell problem at the source
   - (b) accept a composite-panel representation: add an `ARBOPANEL` test row to
     the distro catalog CSV with LOINC `81154-7`, route Arbo files to that code,
     store the raw multiplex text as the result's interpretation, tech
     interprets per-target downstream
   - (c) move Fluorocycler to ASTM/HL7 transport and drop file-based integration
     entirely If they prefer three separate result rows per sample, the parser
     needs a split-row feature deferred to Phase C.
   - **Default for Gate 1**: `CHIKV` (reuse existing CHIKVPCR row). Confirm with
     Mekom before Gate 2 closes.
4. **Mekom LOINC confirmation**: validated individual LOINCs against loinc.org
   (20447-9 / 60260-7 / 7855-0 / 85622-9). Confirm Mekom's reference catalog
   uses the same codes.
5. **Bridge admin upload UI — production feature scope?** — the v3 pivot lands a
   real `GET /admin/upload` admin UI on the bridge for tech-initiated file
   drops. It's a real feature (lab techs without NFS/mount access can use it as
   a manual import path) but only minimally documented. Open: does it need
   distro documentation, a link from the OE AnalyzerForm, or an entry in the
   analyzer deploy runbook at
   `openelis-madagascar-distro/docs/analyzer-bridge-deploy-runbook.md`? Default
   for Gate 1: standalone bridge URL, minimal docs in the controller Javadoc, no
   OE frontend link yet. Deferred decision to after the Gate 1 videos land.

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
