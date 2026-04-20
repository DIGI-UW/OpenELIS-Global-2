# Plan v4 — Madagascar Analyzer File-Import: File-Level Self-Declaration + defaultTestCode Revert

> **v4 pivot (2026-04-11)**: The `Analyzer.defaultTestCode` scalar field (Phase
> A.3.6 + §2.1 TextInput) is a persistent assertion about test identity on an
> analyzer instance, based only on filename convention and lab workflow
> convention. The user raised a first-principles correctness concern: software
> should never assert test identity the incoming file doesn't explicitly carry.
> v4 pivots on four points:
>
> 1. **Maximal revert of `Analyzer.defaultTestCode`.** Column dropped via a new
>    Liquibase 014 changeset. Commit `756266369` (AnalyzerForm TextInput) +
>    `567f0c4d` (Analyzer entity + REST + Liquibase 013 + 3 callers + tests) +
>    `c65dd1b` (bridge `AnalyzerEntry.defaultTestCode` field +
>    `FileMessageHandler` read + `FileResultParser` parameter plumbing) all
>    reverted across webapp, bridge, and the webapp's submodule pointer. Partial
>    revert of `a9d9910` (archive/error dead code removal stays shipped,
>    `defaultTestCode` fixture bits revert). The
>    `FileResultParser.parse(InputStream, Map, String)` signature mechanically
>    survives but the third parameter is renamed to `perFileTestCode` and its
>    source changes: event-scoped (upload UI admin selection OR file-level
>    self-declaration scan), never persistent config.
>
> 2. **New parser mechanism: file-level self-declaration from Result column
>    content.** Parser scans the Result column across all rows for mentions of
>    mapped test codes (from the analyzer's `AnalyzerTestMapping` set). Accepts
>    the file only if exactly ONE mapped test is mentioned, with ZERO
>    contradictions. That self-declaration is the per-file test code and is
>    applied to every non-control row including ambiguously-labeled patient rows
>    (e.g. "Invalid" rows inherit the file's declared test). Zero rows mentioned
>    → rejected. Multiple distinct mapped tests mentioned → rejected
>    (contradiction). Source is 100% the file's own content. HIV-result.xlsx
>    passes cleanly (73 explicit HIV-1 / GENERIC_HIV_CV mentions, zero
>    contradictions, all 94 rows inherit VIH-1). ARBOVIROSE.xlsx will be
>    rejected cleanly (multiplex CHIKV/DENV/ZIKV → contradictions → file to
>    FAILED state — this is the correct outcome, deferred to Mekom verbally).
>
> 3. **Bridge admin upload UI gains a Test dropdown**, populated per-analyzer
>    from the analyzer's `AnalyzerTestMapping` rows (NOT from a persistent
>    default on the instance). Admin selects the test at upload time. Controller
>    validates the selection against the analyzer's mapping set (defense in
>    depth — UI only offered valid codes) and against the file's
>    self-declaration at parse time (admin-selected test MUST match what the
>    file declares, or the upload is rejected with a clear error). The tech is
>    the proximate source of truth at upload time; the file's content is the
>    final authority on what the bytes actually are.
>
> 4. **Fluorocycler stays in Gate 1** with 3 videos total (QS5, QS7,
>    Fluorocycler HIV VL). QuantStudio paths are unchanged (per-row Target Name
>    column, existing parser path). Fluorocycler video uses HIV-result.xlsx
>    through the new upload UI + parser path.
>
> **What stays from v3**: QuantStudio profile mappings, Fluorocycler v2.0.0
> profile rewrite, test catalog CSV, archive/error helper cleanup (still shipped
> in a9d9910), bridge upload UI framework (§2.5a/b/c/d), docker-compose bind
> mount, rebuild + restart + video steps, FileWatcher multi-observer refactor
> (unrelated to defaults), Phase 1 non-destructive bridge containment.
>
> **What's reverted from v3**: the entire `Analyzer.defaultTestCode` chain
> (webapp column, UI TextInput, REST field, bridge entry field, parser
> config-read, distro fixture). See §2.REVERT-A/B/C below for concrete
> commit-level revert steps. §2.1 and §2.3 as shipped are undone; §2.2
> (archive/error cleanup) is preserved.
>
> **What's new in v4**: §2.PARSER (file-level self-declaration mechanism design
> for `FileResultParser`), Test dropdown in upload UI §2.5b, new controller
> validation paths §2.5a, Mekom verbal clarification list in Open Questions.
>
> **v3 saved** at
> `specs/011-madagascar-analyzer-integration/plans/madagascar-file-import-profile-alignment-v3.md`
> for reference and rollback.

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
5. 🔴 **`Analyzer.defaultTestCode` scalar field is a persistent assertion about
   test identity** — Phase A.3.6 shipped a `default_test_code` column on the
   analyzer entity + Liquibase 013 + REST + bridge plumbing, and §2.1 (commit
   `756266369`) shipped a TextInput for it in `AnalyzerForm.jsx`. The whole
   chain is a persistent config saying "this analyzer instance produces test X
   on every file", which silently miscodes a row any time the lab deviates from
   the assumed filename/workflow convention. v4 maximally reverts this chain:
   the correctness bar is **test identity must come from the incoming file +
   tech, never from persistent config**. See §2.REVERT-A/B/C and §2.PARSER for
   the replacement mechanism.
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

**Intended outcome (Gate 1, v4 pivot)**: **three** videos — QuantStudio 5
Arbovirus, QuantStudio 7 HIV VL, and Fluorocycler XT HIV VL. The QuantStudio
videos are unchanged from v3 — both file types carry a per-row `Target Name`
column, so the parser's existing per-row path handles them without any
self-declaration mechanism. The Fluorocycler video uses the new upload UI + Test
dropdown path: admin creates a Fluorocycler XT analyzer via the unified
AnalyzerForm (no more `defaultTestCode` TextInput — reverted in v4), opens the
bridge upload UI in a new tab, selects the Fluorocycler analyzer, picks `VIH-1`
from the Test dropdown populated from the analyzer's `AnalyzerTestMapping` set,
picks `HIV-result.xlsx` via the file picker, clicks Upload. The bridge
controller validates the admin's selection against the analyzer's mapping set
and against the parser's file-level self-declaration scan (which finds 73
explicit HIV-1 mentions in the Result column with zero contradictions). Both
validations pass; parser applies VIH-1 as `perFileTestCode` to every non-control
row; 26 "Invalid" patient rows inherit VIH-1 via the file's self-declaration;
results land in OE staging; admin reviews and accepts. ARBOVIROSE.xlsx is
explicitly NOT tested — if the admin tried to upload it, the parser would reject
it as a multiplex contradiction (Result column mentions CHIKV + DENV + ZIKV =
more than one mapped test declared) and the file would go to FAILED state. That
rejection is the correct outcome, deferred to Mekom verbally. See Open Question
#3 for the Mekom clarification list.

Each video shows the admin creating the analyzer via the unified AnalyzerForm,
opening the bridge upload UI, selecting analyzer + test + real file, uploading,
the bridge parsing and forwarding the results, and the admin accepting the
results in the OE UI. The non-destructive invariants (files stay in place on
`/mnt`, state store shows PROCESSED, no bridge writes outside its
`/data/analyzer-imports/` target) are verified on every upload.

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

### v4 reverts (2026-04-11, pending execution as §2.REVERT-A/B/C)

The following commits from Phase A + Gate 1 pre-pivot work are being **rolled
back** in v4. The reason in every case is the same: they persist a test-identity
assertion on the analyzer instance that the incoming file doesn't explicitly
carry. That's the footgun we just drew a hard line against. The replacement
mechanism is §2.PARSER (file-level self-declaration from Result column
content) + the upload UI Test dropdown in §2.5b.

**Webapp (`OpenELIS-Global-2` PR #3372)**:

- Revert `756266369` — §2.1 AnalyzerForm TextInput + `en.json` i18n keys +
  `formData.defaultTestCode` state + profile-apply auto-fill
  - non-FILE clear-to-null + serializer
- Revert `567f0c4d` — `Analyzer.java` column + Liquibase
  `013-analyzer-default-test-code.xml` +
  `BridgeRegistrationService.registerFile` signature (drop `defaultTestCode`
  param) + `FileImportServiceImpl.autoCreateFromProfile` profile read +
  `AnalyzerRestController:661` response field +
  `AnalyzerBridgeStartupRegistrarTest` test case
- **New Liquibase 014**: `dropColumn` on `clinlims.analyzer.default_test_code`.
  Liquibase is forward-only — we can't "unapply" 013 on deployed DBs, so 014
  drops the column cleanly on upgrade. On fresh DBs (post-revert baseline), 013
  never runs because it's reverted from the `3.4.14.x/base.xml` include list.
- Re-bump submodule pointer in `tools/openelis-analyzer-bridge` to a commit
  **without** `c65dd1b` (the bridge revert commit — see below).

**Bridge (`openelis-analyzer-bridge` PR #34)**:

- Revert `c65dd1b` — `AnalyzerRegistryConfig.AnalyzerEntry.defaultTestCode`
  field (and getter/setter) + `AnalyzerRegistryBootstrap` read of
  `defaultTestCode` from webapp `/rest/analyzer/analyzers` response +
  `FileMessageHandler.processFile:146-162` reading `getDefaultTestCode()` from
  the entry
- Keep `FileResultParser.parse(InputStream, Map, String)` and
  `parseCsv(..., String)` signatures — the mechanical shape is still needed by
  the new caller path — but **rename the third parameter from `defaultTestCode`
  to `perFileTestCode`** in both overloads and in all Javadoc to make the new
  semantic explicit (event-scoped, not analyzer-scoped). Tests reference the
  parameter by name in some mocks; update those call sites.
- New commit on top of the revert: `FileMessageHandler.processFile` now receives
  `perFileTestCode` from the caller (`FileUploadController` in the upload path,
  or `FileNameSelfDeclarationScanner` in the FileWatcher path), not from
  `analyzerEntry.getDefaultTestCode()`.

**Distro (`openelis-madagascar-distro` PR #4)**:

- **Partial revert** of `a9d9910`:
  - **Keep** the archive/error dead code removal (still correct, post-Phase-1
    Liquibase 012 dropped those form fields — `§2.2` is permanently shipped and
    v4 does not touch it)
  - **Revert** the `defaultTestCodeInput` Locator and
    `fillDefaultTestCode(code)` method from
    `tests/playwright/playwright/fixtures/analyzer-form.ts`
  - **Revert** the optional `defaultTestCode?: string` field from the
    `AnalyzerTestConfig` interface in
    `tests/playwright/playwright/helpers/analyzer-test-config.ts`
  - **Revert** the `form.fillDefaultTestCode(config.defaultTestCode)` call from
    `tests/playwright/playwright/helpers/create-analyzer-from-profile.ts:429`
  - **Add** a new `uploadTestCode?: string` field on `AnalyzerTestConfig` — this
    is consumed by the new `drop-real-analyzer-file.ts` helper (§2.5d) to select
    a test from the upload UI Test dropdown, NOT persisted on the analyzer
    instance

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

### End-to-end chain (post v4 pivot)

The v3 chain (Profile `default_test_code` → `Analyzer.defaultTestCode` column →
REST → bridge `AnalyzerEntry.defaultTestCode` → parser) is **deleted** by the v4
revert. It's replaced by two converging paths that both terminate at the same
renamed parser signature.

**Path A — Upload UI (admin-driven, interactive)**:

```
Admin opens bridge /admin/upload UI
  ↓
Admin selects analyzer from dropdown (populated from registered FILE analyzers)
  ↓
UI fetches /admin/upload/analyzers/{id}/tests → returns that analyzer's
  AnalyzerTestMapping set (e.g. {VIH-1, CHIKV, DENV, ZIKV} for Fluorocycler)
  ↓
Admin selects test from Test dropdown (client-side: admin picks from the
  fetched mapping set; NOT a free text input, so admin can't typo)
  ↓
Admin picks file via file picker (setInputFiles)
  ↓
Admin clicks Upload — browser POSTs multipart to /admin/upload with
  (analyzerId, testCode, file)
  ↓
FileUploadController validates:
  (1) analyzerId maps to a registered FILE analyzer
  (2) testCode is in that analyzer's mapping set (defense in depth)
  (3) file is non-empty, safe filename, under size limit
  ↓
Controller writes file to the analyzer's importDirectory
  ↓
Controller invokes FileMessageHandler directly (bypassing FileWatcher
  because we already know the file exists and we already have the
  admin's test declaration)
  ↓
FileMessageHandler runs FileNameSelfDeclarationScanner on the file's
  Result column content → returns the single self-declared test OR
  an error (zero mentions / multiple mentions / contradiction)
  ↓
FileMessageHandler validates: self-declaration must match admin-selected
  testCode. If mismatch, upload is rejected with a clear error
  ("Admin selected VIH-1 but file self-declares as CHIKV") and the
  file goes to FileStateStore FAILED state
  ↓
On match, FileMessageHandler calls
  FileResultParser.parse(InputStream, columnMapping, perFileTestCode=testCode)
  ↓
Parser applies perFileTestCode to every non-control row, forwards to OE
  ↓
OE staging receives results; admin reviews and accepts
```

**Path B — FileWatcher (non-UI, NFS/external drop)**:

```
File lands in watched directory from external source (NFS, operator
  drop, external workflow)
  ↓
FileWatcher detects file at next poll, processes to stable state,
  invokes FileMessageHandler
  ↓
FileMessageHandler runs FileNameSelfDeclarationScanner on the file's
  Result column content
  ↓
If scanner returns exactly ONE mapped test → perFileTestCode = that test
If scanner returns ZERO or MULTIPLE → reject, file goes to FileStateStore
  FAILED state, operator sees in /admin/file-state
  ↓
FileResultParser.parse(InputStream, columnMapping, perFileTestCode)
  ↓
Every non-control row inherits perFileTestCode, forwards to OE staging
```

**Convergence point**: both paths call
`FileResultParser.parse(InputStream, Map, String perFileTestCode)` — the
renamed-but-mechanically-surviving parser signature from the v4 revert. The
parser's per-row decision is unchanged from Phase A: if a row has a non-empty
test code from `column_mapping` (QuantStudio path), use it; else fall back to
`perFileTestCode` (Fluorocycler path). The difference is the SOURCE of
`perFileTestCode` — event-scoped (upload UI or filename scanner) in v4,
analyzer-persistent in v3.

`FileNameSelfDeclarationScanner` is a new bridge class. Its responsibility is:
given an XLSX or CSV file + a set of mapped test codes, scan the Result column
(or whichever column the profile designates as the free-text identity source)
for case-insensitive substring mentions of each mapped code. Return:

- `SelfDeclared(testCode)` if exactly ONE mapped code appears
- `Ambiguous(codes)` if multiple distinct mapped codes appear
- `NoDeclaration` if no mapped code appears
- `NotInterpretable` if the file can't be opened

Implementation is deliberately naive: iterate rows, collect mentions into a Set,
assert size ≤ 1. This is ~40 LOC in Java. Tests cover: HIV-result.xlsx →
SelfDeclared(VIH-1); ARBOVIROSE.xlsx → Ambiguous; empty file → NoDeclaration; a
synthetic mixed file → Ambiguous; a file with "HIV-1" but no configured mapping
for HIV-1 → NoDeclaration (code must be in the analyzer's mapping set, not just
any string). See §2.PARSER for the full test plan.

Profile `default_test_mappings` → `AnalyzerServiceImpl.autoCreateTestMappings` →
`testService.getActiveTestsByLoinc(loinc)` → `AnalyzerTestMapping` rows linking
analyzer test codes to OE test rows. Individual canonical LOINCs make
`tests.get(0)` deterministic (no disambiguation patch needed). **This chain is
unchanged by v4** — it's how the analyzer's valid test code set is populated at
analyzer-create time, which feeds both the upload UI Test dropdown and the
self-declaration scanner's "known test codes" whitelist.

`FileWatcher` allows multiple analyzer registrations per directory. Each
registration's own glob pattern is captured in the observer's IOFileFilter
closure, so two observers at the same path don't cross-fire. Verified by 5
dedicated multi-observer tests. **Unchanged by v4.**

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

### Per-file test identity mechanism (replaces "defaultTestCode UI surface gap")

Post v4 revert, `AnalyzerForm.jsx` Section 3b (FILE Import Settings) will
contain only `importDirectory`, `filePattern`, `columnMappings`, `delimiter`,
and `skipRows` — no `defaultTestCode` field. That's deliberate: we rejected
persistent per-analyzer test code defaults as a correctness footgun. Admins do
NOT configure a "this analyzer always runs test X" assertion anywhere in OE's UI
— no such persistent config exists.

How does Fluorocycler HIV VL get routed to VIH-1 then? Two converging paths
(both built in v4):

1. **Upload UI path**: Admin opens the bridge's `/admin/upload` UI (not the OE
   frontend — it lives on the bridge), selects the analyzer, picks a test code
   from the Test dropdown populated from the analyzer's `AnalyzerTestMapping`
   set, picks a file, uploads. The test code is declared at the upload event,
   not persisted anywhere. The bridge's parser then validates against file-level
   self-declaration (see §2.PARSER) — if the file doesn't support the admin's
   claim, the upload is rejected.
2. **FileWatcher path** (NFS drops, no admin interaction): Bridge's
   `FileNameSelfDeclarationScanner` reads the file's Result column content,
   looks for mapped test codes via the profile's `scanner_synonyms` table,
   requires exactly one unique test to be mentioned (with zero contradictions),
   and applies that as the per-file test code. If the file is ambiguous
   (`NoDeclaration` or `Ambiguous`), it's rejected to FAILED state.

Both paths terminate at
`FileResultParser.parse(InputStream, Map, String perFileTestCode)` — the renamed
(not removed) parser signature. Per-row path still wins for QuantStudio files
with a Target Name column; `perFileTestCode` is the fallback for rows with no
per-row signal.

---

## Phase 2 Gate 1 — Distro video validation

**Goal**: produce **three** video recordings of the distro stack ingesting real
analyzer files end-to-end via the true user workflow: admin creates analyzer via
OE AnalyzerForm (no defaultTestCode field — reverted in v4) → opens the bridge
`/admin/upload` UI in a new tab → selects analyzer → Test dropdown populates
from analyzer's `AnalyzerTestMapping` set → admin picks a test → picks real file
from `/mnt` → clicks Upload → controller validates admin's test selection, runs
file-level self-declaration scan, validates scan result matches admin selection
→ handler parses with `perFileTestCode=admin-selected` → results appear in OE
staging → admin accepts. Videos land in `test-results/playwright-report/`. User
validates, iterates on pacing / clarity, re-records as needed.

**Non-goals for Gate 1** (v4 pivot):

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
- **Do NOT persist per-analyzer test code defaults ANYWHERE**. No
  `Analyzer.defaultTestCode` column (reverted in v4 Liquibase 014), no
  `default_test_code` top-level key in profile JSON, no TextInput in
  AnalyzerForm.jsx (reverted in v4). Per-analyzer persistent test identity is a
  footgun; test identity must come from the file + tech at ingestion time. See
  §2.REVERT-A/B/C.
- **Do NOT infer test identity from anything outside the file**. Not from the
  filename (not unless LA2M adopts a filename convention AND we add
  filename-extraction later — not Gate 1), not from the analyzer instance
  config, not from FluoroCycler hardware convention, not from research-derived
  assumptions. The file's own content (via `FileNameSelfDeclarationScanner`) OR
  the admin's explicit upload-time declaration are the ONLY sources of per-file
  test identity. See §2.PARSER.
- **Do NOT attempt to parse ARBOVIROSE.xlsx**. The parser will reject it cleanly
  as `Ambiguous` (multiple mapped test codes mentioned in the Result column =
  multiplex contradiction). That rejection is the correct Gate 1 outcome.
  Long-term routing is a verbal Mekom clarification — see Open Question #3.
- Do NOT ship `default_test_code_overrides` (v3 revert, still reverted in v4).
  See §2.0 tombstone below.

### 2.0 — ~~`default_test_code_overrides` filename routing~~ REVERTED (v3 pivot, extended in v4)

**v4 note**: The v4 pivot extends this revert principle to the singular
`Analyzer.defaultTestCode` scalar field as well. The v3 pivot removed the plural
overrides map (filename-glob → testCode). The v4 pivot removes the singular
scalar (single file-wide test code). The underlying principle is the same:
software must not assert test identity the incoming file doesn't explicitly
carry. See §2.REVERT-A (webapp), §2.REVERT-B (distro), §2.REVERT-C (bridge) for
the v4 revert recipes. The replacement mechanism is §2.PARSER (file-level
self-declaration from Result column) + the upload UI Test dropdown in §2.5b
(admin declares at upload time).

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

**What replaced it in v3**: Fluorocycler scope narrowed to HIV VL only + the
singular scalar `Analyzer.defaultTestCode` field (shipped in Phase A.3.6 + §2.1
TextInput commit `756266369`). The HIV file would have been routed via an
instance configured with `defaultTestCode=VIH-1`.

**What replaces it in v4**: the singular scalar itself was ALSO reverted in v4
as an extension of the same principle (see the v4 note above the v3 tombstone).
Replacement for v4 is **§2.PARSER** file-level self-declaration from the Result
column content, plus the upload UI Test dropdown in §2.5b for admin-initiated
uploads. Fluorocycler HIV VL still ships in Gate 1 — just via the new mechanism,
not the scalar config path. Arbovirose is a Mekom verbal clarification (see Open
Question #3); options remain:

- (a) reconfigure AssayArea to emit per-target rows (fixes the root problem but
  depends on Mekom + Bruker support)
- (b) accept composite-panel handling (a new `ARBOPANEL` CSV row with LOINC
  `81154-7`, tech reads the raw cell text for per-target interpretation)
- (c) move Fluorocycler to ASTM/HL7 transport and drop file-based integration
  entirely

Tracked as an open question below. Not Gate 1 work.

### 2.REVERT-A — Revert the webapp `Analyzer.defaultTestCode` chain

**Status**: v4 rollback of shipped commits `756266369` + `567f0c4d` on
`OpenELIS-Global-2` PR #3372, branch
`fix/madagascar-accession-results-file-e2e`.

**Why**: The `Analyzer.defaultTestCode` scalar field is a persistent assertion
that an analyzer instance's files all carry test X. That's a standing config bet
against the machine's real behavior — silently miscodes if the lab mis-names a
file, runs a different assay, or if a new assay gets introduced. The user's
direction: test identity must come from the file + tech at ingestion time, never
from persistent config. Replacement: §2.PARSER below, plus the upload UI Test
dropdown in §2.5b.

**Files to revert** (`OpenELIS-Global-2`):

From commit `756266369` (§2.1 TextInput):

- `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx` — remove
  `defaultTestCode: ""` from `formData` initial state; remove
  `defaultTestCode: analyzer.defaultTestCode || ""` from the existing- analyzer
  load `useEffect`; remove `defaultTestCode: ""` from the reset-on-new block;
  remove the `fileUpdates.defaultTestCode = configData.default_test_code` line
  from the profile-apply block; remove `defaultTestCode: null` from the non-FILE
  clear block; remove the `TextInput` with
  `data-testid="analyzer-form-default-test-code-input"` from Section 3b FILE
  Import Settings. These locations are concretely at lines 57, 123, 144, 360,
  500, 940-949 on HEAD of the PR branch; all go away.
- `frontend/src/languages/en.json` — remove the two keys
  `analyzer.form.defaultTestCode` and
  `analyzer.form.defaultTestCode.helperText`. No other locale files need editing
  (Transifex handles non-English; orphaned keys are a translation-management
  concern, not a repo concern).

From commit `567f0c4d` (webapp schema + REST + plumbing):

- `src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java` — remove
  the `@Column(name="default_test_code", length=50)` field and its getter/setter
- `src/main/java/org/openelisglobal/analyzer/service/BridgeRegistrationService.java`
  — revert the `registerFile` signature change (drop the `defaultTestCode`
  parameter, back to 8 args)
- `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java:153`
  — revert the `autoCreateFromProfile` addition that reads
  `configData.default_test_code` from the profile JSON and writes to
  `entity.setDefaultTestCode(...)` before persist. Three-line removal.
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerBridgeStartupRegistrar.java:115`
  — revert the caller update, back to the 8-arg `registerFile` call
- `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java:661`
  — remove `map.put("defaultTestCode", analyzer.getDefaultTestCode());` from the
  `/rest/analyzer/analyzers` response building and the caller at `:1087` (the
  3rd caller of `BridgeRegistrationService.registerFile`)
- `src/test/java/org/openelisglobal/analyzer/service/AnalyzerBridgeStartupRegistrarTest.java`
  — remove `shouldRegisterFileAnalyzerWithDefaultTestCode` test case and revert
  the 9-arg `registerFile` mocks back to 8-arg

**New forward-only Liquibase changeset** (NOT a revert of 013, because Liquibase
is forward-only and 013 has already been applied to the local dev DB):

- `src/main/resources/liquibase/3.4.14.x/014-drop-analyzer-default-test-code.xml`
  — `<dropColumn tableName="analyzer" columnName="default_test_code"/>`. Add to
  the `3.4.14.x/base.xml` include list AFTER the 013 include.
- Also **remove** the include of `013-analyzer-default-test-code.xml` from
  `base.xml` — on fresh DBs the 013 changeset should never run. Delete the
  `013-analyzer-default-test-code.xml` file from the filesystem as well (fresh
  DBs don't need it, and it would just be dead code if left behind). On
  already-deployed DBs that ran 013, Liquibase sees 013 in `databasechangelog`
  and skips it; 014 runs and drops the column cleanly.

From the `3dd96a22` submodule bump (v3's bridge submodule pointer → `c65dd1b`):
re-bump the submodule pointer in `tools/openelis-analyzer-bridge` to a commit
**without** the v4-reverted `c65dd1b` — i.e., the new HEAD of the bridge branch
`fix/madagascar-file-ingestion-stability` after the bridge revert (§2.REVERT-C
below) and the new §2.PARSER builds land. One-line chore commit:
`chore: bump openelis-analyzer-bridge submodule past defaultTestCode revert`.

**Verification**:

- `git grep -n defaultTestCode frontend/src` in `OpenELIS-Global-2` returns zero
  hits
- `git grep -n defaultTestCode src/main/java/org/openelisglobal` returns zero
  hits
- `git grep -n default_test_code src/main/resources/liquibase/3.4.14.x/013`
  returns zero hits
- `mvn test -Dtest=AnalyzerBridgeStartupRegistrarTest` passes
- Webapp compiles clean
- New DB smoke: after `--clean` restart with 014 applied,
  `SELECT column_name FROM information_schema.columns WHERE table_name='analyzer' AND column_name='default_test_code';`
  returns zero rows

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

### 2.REVERT-B — Partial revert of `a9d9910` (distro fixture)

**Status**: v4 rollback of the `defaultTestCode` bits of commit `a9d9910` on
`openelis-madagascar-distro` PR #4, branch `demo/madagascar-analyzers`. The
archive/error dead code removal from the same commit **stays shipped** — §2.2
remains permanently landed.

**Files to partially revert** (`openelis-madagascar-distro`):

- `tests/playwright/playwright/fixtures/analyzer-form.ts`:

  - Remove the `defaultTestCodeInput` `Locator` field declaration (currently
    line 31)
  - Remove the
    `this.defaultTestCodeInput = page.locator('[data-testid="analyzer-form-default-test-code-input"]')`
    initializer (currently line 85)
  - Remove the `fillDefaultTestCode(code: string)` method (currently line
    249-251)
  - Leave the archive/error-related removals from `a9d9910` in place — those are
    correct and stay

- `tests/playwright/playwright/helpers/analyzer-test-config.ts`:

  - Remove the optional `defaultTestCode?: string` field from the
    `AnalyzerTestConfig` interface
  - **Add** a new optional `uploadTestCode?: string` field. This is the test
    code the distro spec helper will select from the upload UI Test dropdown for
    FILE analyzers whose files need file-level self-declaration routing (e.g.
    Fluorocycler HIV VL → `"VIH-1"`). For QuantStudio files (per-row Target Name
    column), this field is left unset because the parser uses the per-row path.
    **Crucially, `uploadTestCode` is NOT persisted on the analyzer instance at
    creation time** — it's consumed later by the upload UI helper (§2.5d) when
    Playwright drives a file drop. No admin config surface; it's a test harness
    input only.

- `tests/playwright/playwright/helpers/create-analyzer-from-profile.ts`:
  - Remove the
    `if (config.defaultTestCode) { await form.fillDefaultTestCode(config.defaultTestCode); }`
    call (currently line ~429)
  - Leave the `fillImportDirectory` call and the archive/error-removal context
    in place

**Verification**:

- `git grep -n defaultTestCode tests/playwright/` in
  `openelis-madagascar-distro` returns zero hits
- `git grep -n fillDefaultTestCode tests/playwright/` returns zero hits
- `git grep -n uploadTestCode tests/playwright/helpers/analyzer-test-config.ts`
  shows exactly one hit (the new field declaration)
- `npx tsc --noEmit` in `tests/playwright` passes

### 2.REVERT-C — Revert the bridge `c65dd1b` plumbing + rename parser param

**Status**: v4 rollback of commit `c65dd1b` on `openelis-analyzer-bridge` PR
#34, branch `fix/madagascar-file-ingestion-stability`, followed by a new commit
that renames the parser's third parameter and wires the new caller.

**Files to revert**:

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/config/AnalyzerRegistryConfig.java`:

  - Remove the `defaultTestCode` field from the `AnalyzerEntry` static inner
    class (currently at line 292)
  - Remove the `getDefaultTestCode()` / `setDefaultTestCode()` accessors added
    in c65dd1b
  - Keep everything else (the `AnalyzerEntry` record, the
    `getRegisteredAnalyzers()` map, the Phase A.3.7 multi-observer refactor in
    `AnalyzerRegistryConfig`'s callers)

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/startup/AnalyzerRegistryBootstrap.java`:

  - Remove the read of `defaultTestCode` from the webapp's
    `/rest/analyzer/analyzers` response (currently around line 128). Revert the
    JSON extraction + `entry.setDefaultTestCode(...)` call.

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileMessageHandler.java`:
  - Revert the read
    `String defaultTestCode = analyzerEntry.getDefaultTestCode();` at line 146
  - Revert the log line at 155-156 that includes `defaultTestCode`
  - Revert the `parseCsv` and `parse` call sites at 157 and 162 to pass `null`
    for the third parameter by default — this is a temporary intermediate state;
    the next commit wires in the new source (either the upload path or the
    `FileNameSelfDeclarationScanner`)

**Parser parameter rename + semantic shift** (this is a NEW commit on top of the
revert, not part of the revert itself):

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileResultParser.java`:

  - Rename the `parse(InputStream, Map<String,String>, String defaultTestCode)`
    overload parameter from `defaultTestCode` to `perFileTestCode`
  - Rename the `parseCsv(String, Map, String, int, String defaultTestCode)`
    overload parameter from `defaultTestCode` to `perFileTestCode`
  - Update all Javadoc to clarify the new semantic:
    > `perFileTestCode` is the file-level test code applied to any row whose
    > `column_mapping` lookup returns null or empty. It is event-scoped: either
    > (a) selected by an admin at upload time via the bridge's `/admin/upload`
    > UI and validated against the analyzer's `AnalyzerTestMapping` set, OR (b)
    > derived from a file-level self-declaration scan of the Result column
    > content by `FileNameSelfDeclarationScanner`. It is never read from
    > persistent analyzer config — that path was removed in v4.

- All existing tests that reference `defaultTestCode` by name in variables or
  mock strings: update to `perFileTestCode`. The mechanical test shape is
  unchanged; only the parameter name drifts.

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileMessageHandler.java`:
  - After the revert, re-wire `processFile` to call the new
    `FileNameSelfDeclarationScanner` (§2.PARSER below) when invoked via
    FileWatcher; or accept a `perFileTestCode` directly when invoked via
    `FileUploadController` (§2.5a)
  - This is the step where the two ingestion paths (Path A upload, Path B
    FileWatcher) converge

**Verification**:

- `git grep -n 'defaultTestCode\b' tools/openelis-analyzer-bridge/src/main`
  returns zero hits
- `git grep -n perFileTestCode tools/openelis-analyzer-bridge/src/main` shows
  hits in `FileResultParser.java` + callers in `FileMessageHandler.java` +
  `FileUploadController.java` (the new upload controller from §2.5a)
- `mvn test` in `tools/openelis-analyzer-bridge` — all existing tests pass (441
  bridge tests + §2.PARSER's new tests)

### 2.WIRE — Wire analyzer test mappings through webapp → bridge

**Why this is a new section**: the v4 upload UI Test dropdown (§2.5b) and parser
self-declaration scanner (§2.PARSER) both need access to each analyzer's
`AnalyzerTestMapping` set (the codes the analyzer is configured to emit,
populated at analyzer-create time from the profile's `default_test_mappings`).
The bridge's `AnalyzerEntry` and `AnalyzerRegistryBootstrap` currently have NO
such field — the bridge has never needed the mapping set before, because nothing
before v4 ever iterated per-analyzer test codes at runtime.

Phase A.3.5 plumbed `defaultTestCode` (singular scalar) through the webapp's
`/rest/analyzer/analyzers` response at line 661 into
`AnalyzerEntry.defaultTestCode`. v4 reverts that field and replaces it with a
LIST of mapped codes — semantically different (not a default, just "the
analyzer's allowed test vocabulary") and safer (no single-value assertion).

**Webapp side** (extends `OpenELIS-Global-2` revert work):

- `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`
  — in the `toAnalyzerMap(...)` helper (around line 600-680), AFTER removing the
  `map.put("defaultTestCode", ...)` line at 661 (§2.REVERT-A), ADD a new field:
  ```java
  // Expose the analyzer's configured test code vocabulary so the
  // bridge can populate upload UI dropdowns and the parser's
  // self-declaration scanner whitelist. Source: AnalyzerTestMapping
  // rows linked to this analyzer. This is NOT a default — just the
  // allowed set.
  List<String> testMappings = analyzerTestMappingService
      .getAllForAnalyzer(analyzer.getId())
      .stream()
      .map(AnalyzerTestMapping::getAnalyzerTestName)
      .distinct()
      .collect(Collectors.toList());
  map.put("testMappings", testMappings);
  ```
- Inject `AnalyzerTestMappingService` into `AnalyzerRestController` if not
  already present (the controller already has several service deps; adding one
  more is a standard pattern match)
- `AnalyzerTestMappingService` location:
  `src/main/java/org/openelisglobal/analyzerimport/service/AnalyzerTestMappingService.java`
- Valueholder location:
  `src/main/java/org/openelisglobal/analyzerimport/valueholder/AnalyzerTestMapping.java`
- Test: extend `AnalyzerRestControllerTest` to verify the `testMappings` field
  is populated for a FILE analyzer with profile-derived mappings

**Bridge side** (extends §2.REVERT-C):

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/config/AnalyzerRegistryConfig.java`:

  - Add a new field to `AnalyzerEntry`:
    ```java
    /**
     * The analyzer's allowed test code vocabulary, populated from
     * the webapp's AnalyzerTestMapping rows at bootstrap time.
     * Used by:
     * - FileUploadController to populate the Test dropdown in the
     *   /admin/upload UI
     * - FileNameSelfDeclarationScanner as the whitelist of codes
     *   to search for in the file's Result column content
     *
     * This is NOT a default — it's the set of codes this analyzer
     * is configured to EMIT, not a claim about which one any given
     * file represents. File identity still comes from the file's
     * own content (via scanner) OR the admin's upload-time
     * declaration. The mapped set just constrains what admin and
     * scanner can legitimately produce.
     */
    private Set<String> mappedTestCodes = Collections.emptySet();
    ```
  - Add getter:
    `public Set<String> getMappedTestCodes() { return mappedTestCodes; }`
  - Add setter:
    `public void setMappedTestCodes(Set<String> codes) { this.mappedTestCodes = codes != null ? codes : Collections.emptySet(); }`

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/startup/AnalyzerRegistryBootstrap.java`:

  - In the response-parsing loop that builds `AnalyzerEntry` objects from the
    webapp's `/rest/analyzer/analyzers` response, read the new `testMappings`
    JSON array:
    ```java
    JsonNode mappingsNode = analyzerJson.get("testMappings");
    if (mappingsNode != null && mappingsNode.isArray()) {
        Set<String> codes = new LinkedHashSet<>();
        mappingsNode.forEach(n -> codes.add(n.asText()));
        entry.setMappedTestCodes(codes);
    }
    ```
  - This slots into the same parsing block where Phase A.3.5 used to read
    `defaultTestCode` (which is now reverted)

- Test: extend `AnalyzerRegistryBootstrapTest` with a case that verifies
  `testMappings` JSON array is parsed into `entry.getMappedTestCodes()`
  correctly

**Integration points** — downstream code that depends on this wiring:

- `FileUploadController.listTestCodes(...)` at §2.5a reads
  `entry.getMappedTestCodes()` and returns it as the JSON response for the
  `/admin/upload/analyzers/{id}/tests` endpoint
- `FileUploadController.uploadFile(...)` validates that the admin's `testCode`
  parameter is `in entry.getMappedTestCodes()` (defense-in-depth — UI only
  offered valid codes, but the server re-checks)
- `FileNameSelfDeclarationScanner.scan(...)` takes `mappedTestCodes` as an
  explicit constructor/parameter argument — the scanner's vocabulary whitelist
  comes from this field, combined with the profile's `scanner_synonyms` table
- `FileMessageHandler.processFile(...)` in the FileWatcher path passes
  `entry.getMappedTestCodes()` into the scanner

**Verification**:

- `curl -sk -u admin:adminADMIN! https://localhost/rest/analyzer/analyzers | jq '.[0].testMappings'`
  returns a JSON array of test codes for the first analyzer
- `curl -sk -u admin:adminADMIN! https://localhost:8443/admin/upload/analyzers/<fluo-id>/tests | jq .`
  returns `["VIH-1","CHIKV","DENV","ZIKV"]` for a properly-configured
  Fluorocycler instance
- `AnalyzerRegistryBootstrap` logs at startup show
  `entry.mappedTestCodes.size() > 0` for FILE analyzers with profile-derived
  mappings

### 2.PARSER — File-level self-declaration mechanism

**What it is**: a new bridge class `FileNameSelfDeclarationScanner` that reads a
file's Result-column content and returns the single self-declared test code, or
an error if the file is ambiguous. This replaces the v3 "read `defaultTestCode`
from analyzer entity" path for the FileWatcher ingestion route.

**New file**:
`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileNameSelfDeclarationScanner.java`

```java
/**
 * Scans a file's free-text content column (usually "Result") for
 * mentions of test codes from a known mapping set (the analyzer's
 * AnalyzerTestMapping entries). Returns the single self-declared
 * test code if exactly one code is mentioned, an explicit
 * Ambiguous result if multiple distinct codes appear (e.g.
 * multiplex panel files), or NoDeclaration if none appear.
 *
 * Source of truth is the file's own content. No config, no
 * filename inference, no analyzer-persistent defaults. This is
 * the v4 replacement for Analyzer.defaultTestCode — the test
 * identity is event-scoped, derived from the bytes we just
 * received.
 */
public class FileNameSelfDeclarationScanner {

    public sealed interface ScanResult {
        record SelfDeclared(String testCode) implements ScanResult {}
        record Ambiguous(Set<String> codes) implements ScanResult {}
        record NoDeclaration() implements ScanResult {}
        record NotInterpretable(String reason) implements ScanResult {}
    }

    /**
     * Scan a file for self-declared test identity.
     *
     * @param path path to the result file (XLSX or CSV)
     * @param columnMappings analyzer's column_mapping profile (tells
     *                       us which column is the free-text content
     *                       source — typically the "Result" column)
     * @param mappedTestCodes the analyzer's AnalyzerTestMapping set
     *                        (e.g. {VIH-1, CHIKV, DENV, ZIKV} for
     *                        Fluorocycler). Codes must be listed in
     *                        both exact form and common FluoroSoftware
     *                        synonyms (e.g. VIH-1 also matches "HIV-1"
     *                        and "GENERIC_HIV_CV"). Synonym handling
     *                        is a separate concern — see §2.PARSER
     *                        "synonym policy" below.
     */
    public ScanResult scan(Path path, Map<String,String> columnMappings,
                            Set<String> mappedTestCodes) {
        // 1. Open file (XLSX via Apache POI, CSV via existing reader)
        // 2. Find the content column via column_mapping — the column
        //    whose mapped role is "result" or "interpretation"
        // 3. Iterate rows, collect case-insensitive substring matches
        //    against mappedTestCodes into a Set<String>
        // 4. If set is empty → NoDeclaration
        //    If set has 1 element → SelfDeclared(element)
        //    If set has >1 element → Ambiguous(set)
        // 5. If file can't be opened or has no recognizable columns
        //    → NotInterpretable(reason)
    }
}
```

**Synonym policy** (important nuance): the real HIV-result.xlsx uses `"HIV-1"`
in patient rows and `"GENERIC_HIV_CV"` in standard rows, but the OE test code is
`VIH-1`. The scanner can't literally substring- match `VIH-1` against the file's
text — it won't find a match. So either:

- (a) The scanner's `mappedTestCodes` input includes **synonyms**. For each OE
  test code in the mapping set, we also include known Bruker / FluoroSoftware
  synonyms. For VIH-1, the synonym set is `{VIH-1, HIV-1, GENERIC_HIV_CV}`.
  Synonyms are declared in the profile JSON under a new `scannerSynonyms`
  top-level key (map OE code → array of strings to match). Match any synonym →
  treat as that OE code.
- (b) The scanner uses a fixed internal synonym list hardcoded in Java. Less
  flexible; per-site vocabulary drift would require code changes.
- (c) The scanner requires the file to literally contain the OE test code
  (strictest). HIV-result.xlsx would fail because it says "HIV-1" not "VIH-1".
  This forces the lab to produce files using OE terminology, which is a workflow
  change we can't enforce unilaterally.

**Decision**: (a) — profile-declared synonym list. The profile JSON is already
the right place for analyzer-specific parsing config, and synonyms are
per-analyzer-kind (Fluorocycler's HIV synonyms are different from
QuantStudio's). Adds one top-level key to
`projects/analyzer-profiles/file/fluorocycler-xt.json` +
`configs/analyzer-profiles/file/fluorocycler-xt.json`:

```json
{
  "scanner_synonyms": {
    "VIH-1": ["VIH-1", "HIV-1", "GENERIC_HIV_CV"],
    "CHIKV": ["CHIKV", "Chikungunya"],
    "DENV": ["DENV", "Dengue"],
    "ZIKV": ["ZIKV", "Zika"]
  }
}
```

**Is this a hardcoded default?** No. It's a **vocabulary translation table**,
not a test-identity assertion. The test identity still comes from the file's own
content — we're just declaring "the free-text strings `HIV-1` and
`GENERIC_HIV_CV` refer to the OE test code `VIH-1`". If a file mentions NONE of
those synonyms, we don't guess; we return `NoDeclaration` and the file is
rejected. The table says "here's what to look for", not "here's what to assume
if you can't find anything".

**Tests** (`FileNameSelfDeclarationScannerTest.java`):

1. `scan_hivResultFile_returnsSelfDeclaredVIH_1` — real `HIV-result.xlsx`,
   Fluorocycler mapping set, returns `SelfDeclared("VIH-1")`. Asserts the
   scanner normalizes `HIV-1` and `GENERIC_HIV_CV` to `VIH-1` via the synonym
   table.
2. `scan_arbovioroseFile_returnsAmbiguous` — real `ARBOVIROSE.xlsx`,
   Fluorocycler mapping set, returns `Ambiguous({CHIKV, DENV, ZIKV})` (or
   whatever subset the actual file mentions). Asserts the scanner correctly
   rejects multiplex files.
3. `scan_emptyFile_returnsNoDeclaration` — a synthetic empty XLSX, returns
   `NoDeclaration`
4. `scan_fileWithNoMappedCodes_returnsNoDeclaration` — a synthetic file
   mentioning only strings that are NOT in any mapped test's synonym list,
   returns `NoDeclaration`
5. `scan_fileWithOneMentionInAStandardRow_returnsSelfDeclared` — HIV-result.xlsx
   stripped down to just 1 standard row mentioning `GENERIC_HIV_CV`, returns
   `SelfDeclared("VIH-1")`. Asserts one mention is enough.
6. `scan_fileWhereOnlyInterpretationMentionsTest_returnsSelfDeclared` — a file
   where the Result column has `Pos`/`Neg` only but the Interpretation column
   has `HIV-1 +`. Asserts scanner reads BOTH result and interpretation columns
   (or whichever the profile designates via `column_mapping`)
7. `scan_fileWithQuantStudioTargetName_routedToPerRowPath` — a QuantStudio file
   with per-row Target Name. Scanner returns `SelfDeclared(first-target)` but
   caller ignores because parser's per-row path wins when column_mapping yields
   a non-null testCode per row. Documents that the scanner runs defensively even
   for per-row files, but its result is only consumed when there's no per-row
   signal.
8. `scan_corruptFile_returnsNotInterpretable` — a zero-byte or truncated file,
   returns `NotInterpretable("file could not be opened")`

**Integration with `FileMessageHandler`**:

`FileMessageHandler.processFile` gets a new branch. Pseudo-flow:

```
if caller provided perFileTestCode (upload UI path):
    use it directly — skip scanner
else:
    scanResult = scanner.scan(path, columnMappings, mappedTestCodes)
    switch scanResult:
        SelfDeclared(code) → perFileTestCode = code, proceed
        Ambiguous(codes)   → FAILED state, reason = "multiple test
                              codes declared: {codes}", no parse
        NoDeclaration      → FAILED state, reason = "no mapped test
                              code found in file content", no parse
        NotInterpretable   → FAILED state, reason = "file could not
                              be interpreted: {reason}", no parse

pass perFileTestCode into FileResultParser.parse(..., perFileTestCode)
```

**FileStateStore** failure reasons: add new enum values for
`AMBIGUOUS_SELF_DECLARATION` and `NO_SELF_DECLARATION`. Existing
`FAILED_NEEDS_HANDLING` state shape is unchanged; only the reason strings
expand. The `/admin/file-state` REST endpoint automatically surfaces these via
its existing reason-passthrough serialization.

### 2.4 — Update distro `analyzer-demo-flow.spec.ts` FILE CONFIGS

**Where**:
`openelis-madagascar-distro/tests/playwright/playwright/tests/demo/harness/analyzer-demo-flow.spec.ts`

**Changes** — keep exactly THREE FILE CONFIGS for Gate 1 (no duplicates):

- **`Demo: QuantStudio 7`** — unchanged config shape, but swap the mock-push
  step for a real-file-drop step via the upload UI (see 2.5 below). The QS7 real
  file is `QuantStudio-7/archive/CVVIH 24 07 2024 serie 02 à valider.xlsx`.
  `uploadTestCode` is **left unset** — QS7 has the per-row `Target Name` column
  so each row's test code comes from the parser's per-row `column_mapping` path.
  The upload UI Test dropdown is still offered to the admin at upload time, but
  if admin leaves it unset (or picks any value) the per-row path wins for rows
  with a populated `Target Name`. The scanner runs defensively but its result is
  consumed only for rows the per-row path leaves null (which should be zero for
  a properly-formed QS7 file).
- **`Demo: QuantStudio 5`** — same swap, same unchanged rationale. Real file is
  `docs/debug-local/Arbo-extraitQS5.xls`. `uploadTestCode` unset.
- **`Demo: Bruker FluoroCycler XT (HIV VL)`** — ONE analyzer instance,
  `profileName: "Bruker FluoroCycler XT"`, `uploadTestCode: "VIH-1"` (the Test
  the Playwright helper will select from the upload UI dropdown — NOT persisted
  on the analyzer instance), `realFileSourcePath` pointing at
  `${ANALYZER_HOST_MOUNT}/la2m/central/analyzers_results/Fluorocycler-XT/HIV-result.xlsx`
  (resolved at test runtime via `process.env.ANALYZER_HOST_MOUNT`, defaulting to
  `/mnt`). The test body opens the bridge upload UI in a new browser context,
  selects the Fluorocycler analyzer from the analyzer dropdown, selects `VIH-1`
  from the Test dropdown (populated from the analyzer's `AnalyzerTestMapping`
  set via `GET /admin/upload/analyzers/{id}/tests`), picks the real file via the
  file picker, clicks Upload. The bridge `FileUploadController` validates
  `VIH-1` is in the analyzer's mapping set (it is — the profile's
  `default_test_mappings` populated it at analyzer-create time), writes the file
  to the analyzer's `importDirectory`, invokes `FileMessageHandler` directly.
  Handler's caller-supplied `perFileTestCode = VIH-1` path skips the scanner
  (admin already declared). Parser applies VIH-1 to every non-control row
  including 26 "Invalid" patient rows and ~22 "HIV-1 -/+" rows; results land in
  OE staging; admin accepts. **Arbovirose is NOT in Gate 1** — see rationale.

**Rationale** (v4 pivot): `HIV-result.xlsx` was inspected via openpyxl during
the clarification session and verified to contain 73 explicit HIV mentions in
the Result column ("HIV-1" in patient rows, "GENERIC_HIV_CV" in standard rows)
with zero contradictions. The file-level self-declaration mechanism (§2.PARSER)
would independently determine the file's identity as VIH-1 even without the
admin's dropdown selection, via the `scanner_synonyms` profile entry mapping
`HIV-1` + `GENERIC_HIV_CV` → `VIH-1`. The admin's dropdown selection in the Gate
1 video is the proximate source of truth (tech declares "I'm uploading HIV");
the file's content is the final authority (scanner confirms 73 mentions). Both
paths converge on VIH-1. The video could also omit the dropdown selection and
rely purely on the scanner for routing, but explicit admin declaration is
clearer for evidence-theater and matches the "file + tech" principle verbatim.

`ARBOVIROSE.xlsx` is not tested in Gate 1 because the parser's file-level
self-declaration scan will return `Ambiguous` on it (Result column mentions
CHIKV + DENV + ZIKV — three mapped test codes, not one). File is rejected, goes
to FileStateStore FAILED state, operator sees the multiplex contradiction in
`/admin/file-state`. That's the correct outcome — we refuse to guess. Deferred
to Mekom **verbally** (no email draft in this plan per your direction); see Open
Question #3 for the list of clarifying questions to raise.

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
lines 25-46 (polling model Javadoc) and 506-557 (stability checker loop,
`// Check for stable files and process them` block). No FileWatcher code changes
required.

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
`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/controller/FileStateController.java:39-142`.
Same `@RestController` annotation, same `@RequestMapping` style, same Spring
Security `.requestMatchers("/admin/**").authenticated()` coverage at
`tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/config/SecurityConfig.java:102`
(so HTTP Basic auth is inherited for free — no new security rule).

Endpoints:

```java
@RestController
@RequestMapping("/admin/upload")
public class FileUploadController {

    private final AnalyzerRegistryConfig registry;
    private final FileMessageHandler fileMessageHandler;
    private final FileNameSelfDeclarationScanner scanner;

    public FileUploadController(AnalyzerRegistryConfig registry,
                                 FileMessageHandler fileMessageHandler,
                                 FileNameSelfDeclarationScanner scanner) {
        this.registry = registry;
        this.fileMessageHandler = fileMessageHandler;
        this.scanner = scanner;
    }

    /**
     * List FILE analyzers available as upload targets. Used by the
     * admin UI's analyzer dropdown. Returns: id, name, watchDirectory,
     * filePattern so the UI can show helpful context ("Watch:
     * /data/analyzer-imports/fluorocycler/incoming").
     *
     * Note: defaultTestCode is NOT in the response — that field was
     * removed in v4 revert. Test code routing lives on the
     * per-analyzer /tests endpoint below.
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
                "filePattern", a.getFilePattern() != null ? a.getFilePattern() : "*"
            ));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * List the test codes this analyzer is configured to accept. Used
     * by the admin UI's Test dropdown after the admin picks an analyzer.
     * Source is the analyzer's AnalyzerTestMapping set, populated at
     * analyzer-create time from the profile's default_test_mappings.
     * The bridge reads this set via the existing AnalyzerEntry's
     * test-mapping field (from Phase A.3.7 era plumbing — unchanged
     * by v4).
     */
    @GetMapping("/analyzers/{id}/tests")
    public ResponseEntity<List<String>> listTestCodes(
            @PathVariable("id") String analyzerId) {
        AnalyzerRegistryConfig.AnalyzerEntry entry =
            registry.getRegisteredAnalyzers().values().stream()
                .filter(e -> analyzerId.equals(e.getId()))
                .findFirst().orElse(null);
        if (entry == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new ArrayList<>(entry.getMappedTestCodes()));
    }

    /**
     * Multipart upload. Writes `file` to the selected analyzer's
     * watchDirectory, runs the file-level self-declaration scanner,
     * validates admin's testCode selection against both the
     * analyzer's mapping set and the file's self-declaration, then
     * invokes FileMessageHandler directly (bypassing FileWatcher
     * because we already have the file and the admin's declaration).
     * Returns an HTML banner on success or a 400-with-HTML-body on
     * validation failure.
     *
     * Validation:
     * - analyzerId present and maps to a registered FILE analyzer
     * - testCode present and in the analyzer's AnalyzerTestMapping set
     * - file not empty, safe filename (no path separators), under size limit
     * - target directory exists and is writable
     * - scanner result is SelfDeclared(code) where code matches testCode,
     *   OR scanner result is NoDeclaration (admin's choice is final in this
     *   case — uncommon, typically means scanner's synonym list is missing
     *   a vocabulary the file uses)
     * - scanner result MUST NOT be Ambiguous (multiplex file → reject
     *   regardless of admin's choice)
     * - scanner result MUST NOT be SelfDeclared(X) where X != testCode
     *   (admin-vs-file mismatch → reject with clear error)
     */
    @PostMapping(consumes = "multipart/form-data",
                 produces = "text/html")
    public ResponseEntity<String> uploadFile(
            @RequestParam("analyzerId") String analyzerId,
            @RequestParam("testCode") String testCode,
            @RequestParam("file") MultipartFile file) throws IOException {
        // 1. lookup analyzer, validate FILE protocol
        // 2. validate testCode is in analyzer's AnalyzerTestMapping set
        // 3. validate file (non-empty, safe filename, size)
        // 4. write to watchDirectory using CREATE option (allow overwrite —
        //    FileStateStore handles content-hash dedupe; --clean restart
        //    wipes state between video runs)
        // 5. invoke scanner.scan(targetPath, columnMappings, mappedTestCodes)
        // 6. validate scanner result vs admin's testCode:
        //       SelfDeclared(code) && code == testCode → PASS
        //       SelfDeclared(code) && code != testCode → REJECT (409 conflict
        //           with HTML body: "File self-declares as {code} but you
        //           selected {testCode}")
        //       Ambiguous(codes)   → REJECT (400 with HTML body: "File
        //           contains multiple test-code mentions: {codes}. This
        //           looks like a multiplex file; file-based ingestion
        //           cannot disambiguate.")
        //       NoDeclaration      → WARN but PASS (admin's choice is final;
        //           scanner couldn't find any mapped code, which usually
        //           means the file uses vocabulary not in the profile's
        //           scanner_synonyms table. Admin should verify before
        //           accepting at OE review time.)
        //       NotInterpretable   → REJECT (400 with HTML body: file
        //           corrupted or unreadable)
        // 7. on PASS, invoke fileMessageHandler.processFile(targetPath,
        //        analyzerId, testCode) — handler uses testCode as
        //        perFileTestCode, skipping its own scanner invocation
        // 8. return success HTML banner
    }
}
```

Tests (`FileUploadControllerTest.java` using `@WebMvcTest` + MockMvc

- mocked `FileNameSelfDeclarationScanner`):

1. `GET /admin/upload/analyzers` returns 200 + JSON list of FILE analyzers only
   (TCP analyzers excluded). Response map excludes `defaultTestCode` key.
2. `GET /admin/upload/analyzers/{id}/tests` returns 200 + JSON array of mapped
   test codes for the given analyzer
3. `GET /admin/upload/analyzers/{id}/tests` with unknown id returns 404
4. `POST /admin/upload` with valid analyzerId + testCode in mapping set + file
   where scanner returns `SelfDeclared(testCode)` writes file, invokes handler
   with `perFileTestCode=testCode`, returns 200 + HTML banner
5. `POST /admin/upload` with unknown analyzerId returns 400
6. `POST /admin/upload` with testCode NOT in analyzer's mapping set returns 400
   ("testCode 'FOO' is not in analyzer's configured test mappings")
7. `POST /admin/upload` with empty file returns 400
8. `POST /admin/upload` with a filename containing `../` returns 400
9. `POST /admin/upload` to a non-FILE analyzer (TCP) returns 400
10. `POST /admin/upload` with scanner result `Ambiguous({CHIKV, DENV, ZIKV})`
    returns 400, HTML banner mentions the conflicting codes
11. `POST /admin/upload` where scanner's SelfDeclared doesn't match admin's
    selection returns 409 with clear message
12. `POST /admin/upload` where scanner returns `NoDeclaration` and admin picked
    a valid code proceeds with a warning banner (admin's choice is final when
    scanner finds nothing)
13. Auth: `GET /admin/upload` without credentials returns 401 (inherited from
    `/admin/**` rule)

**Spring Boot multipart size config** (required — Spring's default 1MB file /
10MB request limit is not enough for real files):

- Real file sizes: `HIV-result.xlsx` 13 KB, `ARBOVIROSE.xlsx` 10 KB, QS7
  `CVVIH...xlsx` 484 KB, `Arbo-extraitQS5.xls` **1.1 MB**
- QS5 file exceeds the default 1 MB file limit → upload would 413 Payload Too
  Large without config
- Add to `tools/openelis-analyzer-bridge/src/main/resources/application.yml`:
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 20MB
        max-request-size: 20MB
  ```
  20 MB ceiling comfortably handles all current real files with room for larger
  files in the future (Fluorocycler multi-run exports, batched QuantStudio
  exports). Uniform limit for all multipart endpoints — one place to change if
  we ever need more.
- One additional controller test: `POST /admin/upload` with a 25 MB file returns
  413 / 400 (documents the size ceiling behavior)

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

        <label for="test-select">Test</label>
        <select name="testCode" id="test-select" required disabled>
          <option value="">Select an analyzer first</option>
        </select>
        <div class="watch-path" id="test-hint">
          The test code declares what this file represents. Only test codes
          configured on the selected analyzer can be chosen.
        </div>

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
        const refreshAnalyzerContext = async () => {
          const opt = select.options[select.selectedIndex];
          if (!opt || !opt.value) return;
          watch.textContent =
            "Watch: " +
            opt.dataset.watchDirectory +
            "   Pattern: " +
            opt.dataset.filePattern;
          await loadTests(opt.value);
        };
        select.addEventListener("change", refreshAnalyzerContext);
        if (analyzers.length > 0) refreshAnalyzerContext();
      }
      async function loadTests(analyzerId) {
        const res = await fetch(`/admin/upload/analyzers/${analyzerId}/tests`, {
          credentials: "include",
        });
        if (!res.ok) {
          document.getElementById("test-select").disabled = true;
          return;
        }
        const codes = await res.json();
        const testSelect = document.getElementById("test-select");
        testSelect.innerHTML = "";
        if (codes.length === 0) {
          const opt = document.createElement("option");
          opt.value = "";
          opt.textContent = "(No test mappings configured)";
          testSelect.appendChild(opt);
          testSelect.disabled = true;
          return;
        }
        for (const code of codes) {
          const opt = document.createElement("option");
          opt.value = code;
          opt.textContent = code;
          testSelect.appendChild(opt);
        }
        testSelect.disabled = false;
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
    /**
     * Test code to select from the upload UI Test dropdown. This is
     * the admin's event-scoped test declaration — NOT persisted on the
     * analyzer instance. For QuantStudio configs (per-row Target Name
     * column), this can be any valid test code on the analyzer's
     * mapping set; parser's per-row path wins for rows that carry
     * their own Target Name. For Fluorocycler HIV VL, use "VIH-1".
     */
    testCode: string;
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

  // The Test dropdown populates via fetch(/admin/upload/analyzers/{id}/tests)
  // when the analyzer change event fires. Wait for at least one
  // non-placeholder option to appear before selecting.
  await uploadPage.waitForFunction(
    () => {
      const sel = document.querySelector(
        "#test-select"
      ) as HTMLSelectElement | null;
      return (
        !!sel &&
        !sel.disabled &&
        sel.options.length > 0 &&
        sel.options[0].value !== ""
      );
    },
    { timeout: 5_000 }
  );

  // Select the test code the admin is declaring for this upload
  await opts.presentation.step(
    `Selecting test code ${opts.testCode} from upload UI dropdown`
  );
  await uploadPage.selectOption("#test-select", opts.testCode);
  await opts.presentation.pause(500);

  // Pick the real file from the bind-mounted /mnt path
  await uploadPage.setInputFiles("#file-input", opts.sourcePath);
  await opts.presentation.pause(750);

  // Submit
  await uploadPage.click("button[type='submit']");

  // Wait for success banner — controller returns HTML with a
  // `.banner.success` class on successful upload. On validation
  // failure (scanner Ambiguous, scanner mismatch, etc.) the browser
  // shows `.banner.error` instead. The test asserts success for
  // Gate 1 happy-path scenarios (QS5/QS7/HIV-result all produce
  // SelfDeclared outcomes matching the admin's selection).
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

`--clean` runs `docker compose down -v` to wipe ALL named volumes — DB (so
Liquibase runs from scratch, applying all Phase A changesets plus the new v4
`014-drop-analyzer-default-test-code`), bridge-state SQLite volume (so
FileStateStore re-initializes with no content-hash history, which lets video
runs re-process the same real file on repeat). The script also clears any stale
Liquibase lock from the prior run and waits for webapp + bridge + mock + proxy
health checks before returning.

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

# Verify the default_test_code column is ABSENT (v4 revert + Liquibase 014 drop)
docker exec openelisglobal-database \
  psql -U clinlims -d clinlims -c \
  "SELECT column_name FROM information_schema.columns WHERE table_name='analyzer' AND column_name='default_test_code';"
# expect: zero rows (column has been dropped)

# Verify the new Liquibase 014 changeset ran
docker exec openelisglobal-database \
  psql -U clinlims -d clinlims -c \
  "SELECT id, author FROM clinlims.databasechangelog WHERE filename LIKE '%014-drop-analyzer-default-test-code%';"
# expect: one row

# Verify the bridge upload UI endpoints respond
curl -sk -u admin:adminADMIN! https://localhost:8443/admin/upload/analyzers | jq .
# expect: JSON array of FILE analyzers, each with id/name/watchDirectory/filePattern,
# NO defaultTestCode field

# Verify the per-analyzer test code endpoint
curl -sk -u admin:adminADMIN! \
  "https://localhost:8443/admin/upload/analyzers/<fluorocycler-analyzer-id>/tests" | jq .
# expect: ["VIH-1", "CHIKV", "DENV", "ZIKV"]
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

- `QuantStudio 7 (FILE/Excel): full E2E flow.webm` — admin creates analyzer from
  `QuantStudio QS5/QS7` profile (no defaultTestCode TextInput in the form —
  reverted in v4). Admin opens the bridge `/admin/upload` UI in a new tab,
  selects the QS7 analyzer, sees the Test dropdown populate with
  `{VIH-1, IC, ...}` from the QS7 test mapping set, picks any test (doesn't
  matter — per-row Target Name wins), picks
  `CVVIH 24 07 2024 serie 02 à valider.xlsx` from the bind-mounted `/mnt` path,
  clicks Upload. Controller validates admin's test selection is in the mapping
  set, writes the file, scanner scans (may return `NoDeclaration` or
  `SelfDeclared(VIH-1)` depending on the Result column content), handler invokes
  parser with perFileTestCode=admin-selected (but per-row path wins for every
  row that has Target Name populated). 95+ HIV VL results land in OE staging
  with per-row test codes (`VIH-1`, `IC`). Admin accepts.
- `QuantStudio 5 (FILE/Excel): full E2E flow.webm` — same flow with
  `Arbo-extraitQS5.xls`; CHIKV + DENV + ZIKV target rows appear in staging
  (per-row Target Name column on the QS file); STANDARD/NTC rows flagged as
  controls and filtered by the qcTask filter (Phase A.3.2); admin accepts.
- `Bruker FluoroCycler XT HIV VL (FILE/Excel): full E2E flow.webm` — ONE
  Fluorocycler analyzer instance, created from the `Bruker FluoroCycler XT`
  profile (no defaultTestCode TextInput in the form — reverted in v4). Admin
  opens bridge `/admin/upload` UI, selects the Fluorocycler analyzer, sees Test
  dropdown populate with `{VIH-1, CHIKV, DENV, ZIKV}` from the Fluorocycler
  mapping set. Admin selects `VIH-1`. Admin picks `HIV-result.xlsx` via the file
  picker from bind-mounted `/mnt`. Admin clicks Upload. Bridge controller
  validates `VIH-1` ∈ mapping set ✓; scanner runs over the file's Result
  column + profile's `scanner_synonyms` table, finds 73 mentions mapping to
  `VIH-1` (via HIV-1 + GENERIC_HIV_CV synonyms), zero contradictions, returns
  `SelfDeclared("VIH-1")`; controller validates
  `SelfDeclared("VIH-1") == admin's "VIH-1"` ✓; handler parses with
  `perFileTestCode="VIH-1"`; 94 non-header rows process — 73 explicit HIV-1
  patient/standard rows get VIH-1 from file self-declaration, 26 patient
  "Invalid" rows inherit VIH-1 (the whole file declared VIH-1), 5 QC control
  rows (Negative Control valid / Positive Control invalid / Standard Not
  interpretable) filtered out by the qcTask filter; results land in OE staging
  under VIH-1; admin reviews and accepts. The video's closing title explains:
  "The parser's file-level self-declaration mechanism scanned 94 rows, found
  `HIV-1` / `GENERIC_HIV_CV` in 73 of them, mapped to the OE test code VIH-1 via
  the profile's scanner_synonyms table, applied VIH-1 to every non-control row
  including the 26 Invalid patient rows. Source of truth: the file's own
  content + the admin's declaration at upload time. No persistent config. No
  default. ARBOVIROSE.xlsx would be rejected cleanly — deferred to Mekom
  verbally."

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

**v4 revert commits (land first, before any new Gate 1 build work)**:

- **`OpenELIS-Global-2` PR #3372** — v4 revert commits:

  - `revert: remove defaultTestCode TextInput from AnalyzerForm` — undoes
    `756266369` (§2.REVERT-A frontend portion)
  - `revert: remove Analyzer.defaultTestCode column + REST + callers` — undoes
    `567f0c4d` (§2.REVERT-A backend portion)
  - `feat(liquibase): add 014-drop-analyzer-default-test-code` — new
    forward-only changeset (§2.REVERT-A) that drops the column on
    already-deployed DBs and removes the 013 include from base.xml
  - `chore: bump openelis-analyzer-bridge submodule past defaultTestCode revert`
    — re-pointer to post-revert bridge HEAD

- **`openelis-analyzer-bridge` PR #34** — v4 revert + rename:

  - `revert: remove defaultTestCode from AnalyzerEntry + bootstrap + FileMessageHandler`
    — undoes `c65dd1b` (§2.REVERT-C first commit)
  - `refactor(parser): rename defaultTestCode -> perFileTestCode across FileResultParser`
    — parser signature stays, parameter name shifts (§2.REVERT-C second commit)

- **`openelis-madagascar-distro` PR #4** — v4 revert:
  - `revert(playwright): remove defaultTestCode fixture bits, add uploadTestCode`
    — partial revert of `a9d9910` (§2.REVERT-B). Archive/error cleanup STAYS
    shipped.

**v4 new-build commits (land after revert commits are pushed)**:

- **`OpenELIS-Global-2` PR #3372** — new features on top of the revert:

  - `feat(analyzer): expose testMappings on /rest/analyzer/analyzers response` —
    new field in `AnalyzerRestController.toAnalyzerMap(...)` populated from
    `AnalyzerTestMappingService`, consumed by the bridge's
    `AnalyzerRegistryBootstrap` (§2.WIRE webapp side)

- **`openelis-analyzer-bridge` PR #34** — new features on top of the revert:

  - `feat(registry): add mappedTestCodes to AnalyzerEntry + bootstrap read` —
    new field + getter/setter on `AnalyzerEntry`, parse `testMappings` array
    from webapp response in `AnalyzerRegistryBootstrap` (§2.WIRE bridge side)
  - `feat(parser): add FileNameSelfDeclarationScanner` — new class + tests
    (§2.PARSER)
  - `feat(config): raise Spring multipart limits to 20 MB in application.yml` —
    required for QS5 Arbo file (1.1 MB) which exceeds default 1 MB (§2.5a
    multipart sizing note)
  - `feat(bridge): add FileUploadController + static /admin/upload UI` —
    §2.5a/b: controller + HTML form + analyzer dropdown + test-code dropdown +
    scanner integration
  - `feat(handler): wire FileNameSelfDeclarationScanner into FileWatcher path` —
    §2.PARSER integration with `FileMessageHandler.processFile`

- **`openelis-madagascar-distro` PR #4** — Gate 1 build:

  - `feat(compose): add ANALYZER_HOST_MOUNT bind mount to demo-tests` — §2.5c
    one-line compose edit
  - `feat(playwright): add drop-real-analyzer-file.ts helper driving bridge upload UI`
    — §2.5d new helper with Test dropdown selection step
  - `test(harness): update analyzer-demo-flow.spec.ts CONFIGS for 3 file analyzers`
    — §2.4 config updates
  - `test(harness): pacing + screenshot evidence capture` — §2.7

- **`OpenELIS-Global-2` PR #3372** — profile updates:

  - `feat(analyzer-profiles): add scanner_synonyms to fluorocycler-xt profile` —
    the synonym table for `FileNameSelfDeclarationScanner` so HIV-1 /
    GENERIC_HIV_CV map to OE test code VIH-1. Touches
    `projects/analyzer-profiles/file/fluorocycler-xt.json`.
  - (distro mirror) same profile edit in
    `openelis-madagascar-distro/configs/analyzer-profiles/file/fluorocycler-xt.json`

- **Both repos** — Gate 2 commits after Gate 1 approved (unchanged from v3):
  main-repo `file-import-results.spec.ts` alignment, mock server fixture
  regeneration or handler rework.

Each commit on its own, pushed to the existing branch. No new PRs. Order
matters: revert commits MUST land and be verified before new-build commits stack
on top — otherwise we'd have a window where the TextInput still exists but the
backing column is gone (or vice versa).

---

## Verification (what "done" looks like)

**Gate 1 exit criteria** (user validates via video review):

**v4 revert successfully applied**:

- [ ] `git grep -n defaultTestCode frontend/src` in `OpenELIS-Global-2` returns
      zero hits (§2.REVERT-A frontend)
- [ ] `git grep -n defaultTestCode src/main/java/org/openelisglobal` returns
      zero hits (§2.REVERT-A backend)
- [ ] `git grep -n 'defaultTestCode\b' tools/openelis-analyzer-bridge/src/main`
      returns zero hits (§2.REVERT-C bridge)
- [ ] `git grep -n defaultTestCode openelis-madagascar-distro/tests/playwright`
      returns zero hits (§2.REVERT-B distro)
- [ ] Parser signature renamed:
      `git grep -n perFileTestCode tools/openelis-analyzer-bridge/src/main`
      shows hits in `FileResultParser.java` + `FileMessageHandler.java` +
      `FileUploadController.java`

**Database state after `--clean` restart**:

- [ ] Distro stack restarted from scratch on new `:local` images; webapp +
      bridge + mock + proxy all healthy
- [ ] New DB catalog rows confirmed: HIVVIRALLOAD / DENGUEPCR / CHIKVPCR /
      ZIKVPCR with expected LOINCs (from Phase A.0 — unchanged by v4)
- [ ] `clinlims.analyzer.default_test_code` column confirmed **ABSENT** (v4
      Liquibase 014 dropped it); also no `default_test_code_overrides` column
      (reverted earlier in v3)
- [ ] `clinlims.databasechangelog` contains a row for
      `014-drop-analyzer-default-test-code` (v4 forward changeset ran)

**UI + bridge surfaces**:

- [ ] `AnalyzerForm.jsx` Section 3b FILE Import Settings **no longer contains**
      a `defaultTestCode` TextInput — reverted in v4. Manual smoke: open OE,
      click Add Analyzer, select Fluorocycler profile, verify only
      `importDirectory`, `filePattern`, `columnMappings`, `delimiter`,
      `skipRows` inputs appear in Section 3b.
- [ ] Bridge `/admin/upload/index.html` loads with HTTP Basic auth
- [ ] Bridge `GET /admin/upload/analyzers` returns a JSON list of FILE analyzers
      WITHOUT a `defaultTestCode` field on any entry
- [ ] Webapp `/rest/analyzer/analyzers` response includes a `testMappings` JSON
      array per analyzer (§2.WIRE webapp side). Verify with
      `curl -sk -u admin:adminADMIN! https://localhost/rest/analyzer/analyzers | jq '.[0].testMappings'`.
      Expected: JSON array of test codes for the first analyzer.
- [ ] Bridge `AnalyzerEntry.mappedTestCodes` is populated at bootstrap from the
      webapp's `testMappings` JSON (§2.WIRE bridge side). Verify via bridge logs
      at startup: should see a log line per FILE analyzer showing
      `mappedTestCodes.size() > 0`.
- [ ] Bridge `GET /admin/upload/analyzers/{id}/tests` returns the analyzer's
      `AnalyzerTestMapping` codes as a JSON array. For the Fluorocycler
      instance: `["VIH-1","CHIKV","DENV","ZIKV"]`. For a QS7 instance: whatever
      test codes the QS profile's `default_test_mappings` populated.
- [ ] Bridge `FileUploadController` tests pass: 13 MockMvc cases per §2.5a
      (analyzer listing, test-code listing, happy-path upload, unknown analyzer
      400, invalid test-code 400, empty file 400, path traversal 400,
      TCP-analyzer 400, scanner Ambiguous 400, scanner mismatch 409, scanner
      NoDeclaration warn-pass, auth 401)
- [ ] Bridge `FileNameSelfDeclarationScannerTest` passes: 8 cases per §2.PARSER
      (HIV-result → VIH-1, ARBOVIROSE → Ambiguous, empty/no-mapped-codes →
      NoDeclaration, single-standard-row, interpretation-col-only, QS defensive
      scan, corrupt file)

**Compose + bind mount**:

- [ ] `docker-compose.validate.yml` demo-tests service has the
      `${ANALYZER_HOST_MOUNT:-/mnt}:${ANALYZER_HOST_MOUNT:-/mnt}:ro` bind mount
      added (§2.5c); Playwright inside the container can
      `ls /mnt/la2m/central/analyzers_results/` and see the real files

**Distro harness**:

- [ ] Distro `harness-demo-video` project runs the three FILE scenarios without
      failing at the helper's form-fill stage (archive/error dead code was
      removed in `a9d9910`, which stays shipped)
- [ ] Distro helper + fixture have `uploadTestCode` (new) and no
      `defaultTestCode` / `fillDefaultTestCode` (removed)

**Videos (3 total)**:

- [ ] **Video 1 — QuantStudio 7 HIV VL**: admin creates analyzer from
      `QuantStudio QS5/QS7` profile via OE AnalyzerForm (no defaultTestCode
      TextInput visible). Opens bridge `/admin/upload` UI in a new tab, selects
      QS7 analyzer, Test dropdown populates with QS7 mapped tests, admin picks
      any test (per-row path wins), uses the file picker to pick
      `CVVIH 24 07 2024 serie 02 à valider.xlsx` from the bind-mounted `/mnt`
      path, clicks Upload. Success banner confirms file landed. Admin returns to
      OE, sees results in staging with per-row test codes (`VIH-1`, `IC`),
      accepts them.
- [ ] **Video 2 — QuantStudio 5 Arbovirus**: same flow with
      `Arbo-extraitQS5.xls`; CHIKV + DENV + ZIKV target rows appear in staging
      via per-row Target Name; STANDARD/NTC controls filtered by qcTask; admin
      accepts.
- [ ] **Video 3 — Fluorocycler XT HIV VL**: admin creates a Fluorocycler XT
      instance from the `Bruker FluoroCycler XT` profile (no defaultTestCode
      TextInput — reverted). Opens bridge upload UI, selects Fluorocycler
      analyzer, Test dropdown populates with `{VIH-1, CHIKV, DENV, ZIKV}`, admin
      selects `VIH-1`, picks `HIV-result.xlsx` via file picker, uploads. Bridge
      controller validates; scanner runs, finds 73 HIV-1 / GENERIC_HIV_CV
      mentions via profile's scanner_synonyms, returns `SelfDeclared("VIH-1")`,
      matches admin's selection; handler parses with `perFileTestCode="VIH-1"`;
      ~68 patient rows (explicit HIV-1 + Invalid) land in OE staging under
      VIH-1; 5 QC control rows filtered by qcTask filter; admin reviews and
      accepts. Closing title explains the self-declaration mechanism and that
      ARBOVIROSE.xlsx would be cleanly rejected as multiplex — deferred to Mekom
      verbally.

**Non-destructive invariants**:

- [ ] After each upload, the source file on `/mnt` is **untouched** (verified
      via `stat` on the host); the bridge's written copy in
      `/data/analyzer-imports/...` exists and FileStateStore shows PROCESSED
      (content hash + analyzer ID keyed); no `.error` / `.failed` sidecars
      written anywhere
- [ ] `/admin/file-state` REST endpoint shows PROCESSED rows for each uploaded
      file

**User approval**:

- [ ] Videos uploaded to Madagascar Drive + linked from tracker; user approves
      pacing, clarity, and evidence quality

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
3. **Fluorocycler — verbal clarification questions for Mekom/Herbert** (v4
   pivot, to be raised verbally, NOT as an email draft per user direction): the
   current state of Fluorocycler file ingestion at LA2M has several unknowns.
   The v4 parser mechanism is built to REJECT ambiguous files cleanly
   (ARBOVIROSE.xlsx → Ambiguous contradiction → FAILED state), which is the
   right outcome for Gate

   1. But the LONG-TERM path depends on facts Mekom/Herbert need to confirm.
      Piotr to raise verbally when next speaking with Mekom:

   - (i) Does FluoroSoftware export XLSX natively, or only PDF + CSV? If only
     PDF/CSV: is the current LA2M workflow (tech copies from FluoroSoftware into
     a WPS Sheets workbook) a permanent part of the process, or can the tech
     export CSV directly?
   - (ii) Both real files we have (`HIV-result.xlsx` + `ARBOVIROSE.xlsx`) are
     WPS Sheets files, creator=`eldin`, lastModifiedBy=`eldin`. They've been
     round-tripped through WPS Office by a human. Can we see a **raw, untouched,
     straight-from- FluoroSoftware** export to verify the authentic column
     layout?
   - (iii) The legacy `FluoroCyclerXT` plugin in
     `plugins/analyzers/FluoroCyclerXT/` expects a 4-column delimited-text CSV
     (Sample ID, Result, Interpretation, Position) which matches ZERO of the
     real LA2M files. Was that plugin written for a different Fluorocycler model
     or a different FluoroSoftware export mode? What changed between then and
     now?
   - (iv) Does LA2M ever mix multiple assays on a single Fluorocycler plate, or
     is it always "one plate = one assay"? (The parser assumes
     one-plate-one-assay via the file-level self-declaration mechanism; if
     plates can run multi-assay, that assumption breaks and we need a different
     approach.)
   - (v) For Arbovirose specifically — is the multiplex CHIKV/DENV/ ZIKV panel
     going to stay as free-text in one Result cell, or can the lab configure
     FluoroSoftware's AssayArea module to emit per-target rows (each with its
     own Target column like QuantStudio)? Options if not:
     - (a) reconfigure AssayArea to emit per-target rows → QuantStudio- like
       per-row ingestion, zero parser changes needed
     - (b) accept a composite-panel test representation (new `ARBOPANEL` row in
       the catalog CSV with LOINC `81154-7`, tech interprets the multiplex text
       downstream at accept time)
     - (c) move Fluorocycler to ASTM/HL7 transport — OBX-3 carries test identity
       at the wire level, bypasses the file-format ambiguity entirely
   - (vi) For HIV VL specifically — can LA2M adopt a filename convention where
     the test code appears in the filename (e.g., `VIH-1-<date>-<batch>.xlsx`)?
     This would let the `FileNameSelfDeclarationScanner` route files without
     requiring the admin to use the upload UI. It becomes a pure NFS-drop
     workflow with no UI step.

4. **Mekom LOINC confirmation**: validated individual LOINCs against loinc.org
   (20447-9 / 60260-7 / 7855-0 / 85622-9). Confirm Mekom's reference catalog
   uses the same codes.

5. **QS7 sheet choice**: `CVVIH 24 07 2024 serie 02 à valider.xlsx` has 6 sheets
   including `Results` (header at row 50) and `Results (2)` (header at row 0).
   Which does the workflow rely on? Default: `Results` (keep until Mekom
   confirms).

6. **"à valider" filename semantics**: tech-validation state or just naming
   convention? Default: ignore filename, parse file.

7. **Bridge admin upload UI — production feature scope** (carried over from v3):
   the upload UI is minimally documented and only reachable via direct bridge
   URL. Open: distro documentation, link from OE AnalyzerForm, or runbook entry
   at `openelis-madagascar-distro/docs/analyzer-bridge-deploy-runbook.md`?
   Default for Gate 1: standalone bridge URL, minimal docs in the controller
   Javadoc, no OE frontend link. Deferred decision to after Gate 1 videos land.

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
