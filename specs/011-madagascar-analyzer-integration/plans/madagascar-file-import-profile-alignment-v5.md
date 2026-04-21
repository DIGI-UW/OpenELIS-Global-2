# Plan v5 — Madagascar Analyzer Integration: Simple Profile Model + Two-Way Bridge Sync

> **v5 reframing (2026-04-11 evening)**. v4 grew to 2519 lines describing
> file-level self-declaration scanning, multi-step validation, and revert
> choreography. The user's response cut through it: "I don't understand what is
> the issue — the profile setup is super simple. A profile per machine, with
> default settings/mappings that get loaded when the analyzer is configured in
> the UI/thru rest api... the analyzer gets registered with the bridge with a
> 2-way sync."
>
> v5 aligns the plan to that simple model. Most of the architecture is already
> closer to the simple model than v4 implied — Explore agent verification today
> confirmed the creation path is single-writer, the bridge's `PUT /sync`
> endpoint is fully implemented, and `autoCreateTestMappings` is idempotent. The
> v5 work is primarily **subtractive**: delete the early-return guard that
> blocks profile re-apply, delete the inline scanner-gating on admin upload,
> delete dead profile schema keys, and **wire the already-built two-way sync**
> from the webapp side.

---

## Context

**Why this plan exists**: finish Gate 1 (three real-file E2E videos — QS5, QS7,
Fluorocycler) and simplify the analyzer profile chain so future iterations don't
reproduce the stale-state / profile-clash class of bugs that consumed this
session.

**What prompted v5**:

1. **User's simple model statement** (rank-1 authority): profile per machine,
   defaults seed the DB at configure time, analyzer registers with bridge via
   two-way sync. v4's scanner-as-gate, dual mapping sources, and five-key
   profile schema don't match this model.
2. **Gate 1 Fluorocycler video blocked** on a concrete, reproducible bug:
   analyzer id=4 has a stale `column_mappings_json` (12 keys, matches no current
   profile source). Root cause verified: the `autoCreateFromProfile`
   early-return at `FileImportServiceImpl.java:66-69` silently refuses to
   overwrite existing config, locking in whatever earlier state the form
   submitted. Delete the guard → idempotent re-apply → stale state gets
   corrected on every analyzer creation.
3. **Two-way sync was always the design**. Bridge already exposes the full CRUD
   surface (`POST /register`, `DELETE /{id}`, `GET /`, `PUT /sync`). Webapp only
   wired the push verbs (`registerFile`/`registerTcp`/`unregister`). `GET` and
   `PUT /sync` are orphaned — no webapp code calls them. Wiring the
   pull/reconcile side is net-new work in v5, but it's trivial because the
   bridge side is complete and correct.

**Intended outcome**: three Gate 1 videos captured on a stack whose code
reflects the simple model; fewer moving parts; no dead schema keys; no
scanner-as-gate on admin upload; no orphaned sync endpoints.

---

## The Simple Model (user's vision, verbatim-faithful)

1. **Profile per machine**. One JSON per analyzer model (`fluorocycler-xt.json`,
   `quantstudio.json`, etc.). Canonical location:
   `OpenELIS-Global-2/projects/analyzer-profiles/file/*.json`, mirrored to
   `openelis-madagascar-distro/configs/analyzer-profiles/file/` (bind-mounted
   into webapp container as `/data/analyzer-profiles/`).

2. **Profile contents (minimal schema)**:

   - `profileMeta` (name, displayName, version)
   - `category` / `protocol` / `supported_extensions`
   - `column_mapping` — file-column → parser-field, **ONE authoritative mapping
     key**
   - `default_test_mappings` — `[{test_code, loinc, unit}, ...]`, choice list
     this analyzer can produce, seeds `analyzer_test_map` rows at
     analyzer-creation time
   - `configDefaults` — fileFormat, delimiter, hasHeader, skipRows
   - `sheet_detection` (where relevant — QuantStudio's row-50 header case)

3. **Analyzer creation (already mostly correct in code)**: admin fills
   `AnalyzerForm` or POSTs to `/rest/analyzer/analyzers` with `defaultConfigId`.
   `AnalyzerRestController` creates the entity directly from the form; then
   `autoCreateTestMappings` seeds `analyzer_test_map` rows by LOINC resolution
   (idempotent updates if profile evolves); then `autoCreateFromProfile` writes
   FILE fields (importDirectory, filePattern, columnMappings, etc.) from the
   profile; then `registerWithBridge` pushes to the bridge. **Single writer
   chain, profile-authoritative.**

4. **Both `column_mapping` and `default_test_mappings` are editable
   post-creation** via the AnalyzerForm UI. Edits overwrite the DB; the next
   `registerFile` call propagates them to the bridge. Re-apply from profile is
   always idempotent overwrite (after the early-return is deleted — see §1).

5. **Two-way bridge↔webapp sync**: webapp is the config authority. Webapp pushes
   on create/update/delete (already works). Webapp pulls bridge state via
   `GET /api/analyzers` for drift detection. Webapp calls
   `PUT /api/analyzers/sync` at boot with the full analyzer list for full-state
   reconciliation after bridge restarts.

6. **Admin upload flow (scanner as UX helper, not gate)**:
   - Admin opens `/admin/upload`, picks an analyzer (dropdown fetched from
     bridge's local state, which mirrors webapp's `analyzer_test_map`)
   - Admin picks a file → browser POSTs to `/admin/upload/scan` → bridge parses
     file content, runs scanner, returns a suggested test code → HTML
     pre-selects the suggestion in the Test dropdown
   - Admin confirms or overrides → clicks Upload
   - Browser POSTs `/admin/upload` with `analyzerId, testCode, file`
   - Bridge writes the file, calls
     `FileMessageHandler.processFile(path, analyzerId, testCode)`
   - Done — no scanner gating, no validateScanAgainstAdmin, no dual source of
     truth

---

## Current State Snapshot (2026-04-11 evening)

### Gate 1 progress

| Test                  | State                                                           | Video artifact                                         |
| --------------------- | --------------------------------------------------------------- | ------------------------------------------------------ |
| QS5 (Arbovirus)       | ✅ PASSED (iter 4, post scanner-advisory fix)                   | `gate1-videos/20260411-qs5-qs7-passing/qs5/video.webm` |
| QS7 (HIV VL)          | ✅ PASSED (iter 4)                                              | `gate1-videos/20260411-qs5-qs7-passing/qs7/video.webm` |
| Fluorocycler (HIV VL) | ❌ BLOCKED — stale `column_mappings_json` in DB (analyzer id=4) | not captured                                           |

### PR state (all rebased + squashed + spotless + force-pushed)

| Repo                         | PR    | Branch HEAD                       |
| ---------------------------- | ----- | --------------------------------- |
| `OpenELIS-Global-2`          | #3372 | `29bfa9a07` (post-spotless amend) |
| `openelis-analyzer-bridge`   | #34   | `119774b`                         |
| `openelis-madagascar-distro` | #4    | `85b5266`                         |

Backup branches preserved on all three for rollback.

### Ground-truth-verified facts (Explore agent, 2026-04-11 evening)

- **`FileImportServiceImpl.autoCreateFromProfile` lines 66-69**: early-return
  guard present. Log message: "FILE config already exists for analyzer".
  Subsequent lines (72+) proceed to set fileFormat + column mappings. Deleting
  lines 66-69 makes the method idempotent.
- **`AnalyzerServiceImpl.autoCreateTestMappings` lines 377-466**: reads
  `default_test_mappings` from profile config, resolves each by-LOINC via
  `testService.getActiveTestsByLoinc`, creates/updates `analyzer_test_map` rows,
  idempotent for re-apply.
- **`AnalyzerRestController.POST /rest/analyzer/analyzers` lines 170-318**:
  single-writer creation flow (create entity → insert → autoCreateTestMappings →
  autoCreateFromProfile → registerWithBridge). **Multi-writer concern was
  wrong** — architecture is already single-writer.
- **Bridge `AnalyzerRegistrationController` lines 41-188**: all 4 endpoints
  fully implemented.
  - `POST /register` (line 41) — register single, wires file watcher
  - `DELETE /{id}` (line 102) — unregister + remove watchers
  - `GET /` (line 114) — list registered analyzers
  - `PUT /sync` (line 125) — full-state reconciliation (delete stale + accept
    new). **Complete implementation, not a stub.**
- **Webapp `BridgeRegistrationService` lines 50-111**: only push methods wired
  (`registerTcp`, `registerFile`, `unregister`). `GET /api/analyzers` and
  `PUT /api/analyzers/sync` are orphaned — no webapp code calls them.
- **`FileUploadController.uploadFile` lines 201-202**: scanner is called inline.
  My earlier advisory fix made all scanner outcomes `passWithWarning()` in
  `validateScanAgainstAdmin` (lines 229-267), but the `scanner.scan(...)` call
  and the `validateScanAgainstAdmin(...)` invocation are still in the POST path.
  v5 removes them.
- **`FileMessageHandler.processFile` lines 87-132**: zero references to
  `stateStore` / `FileStateStore` / `markProcessed`. Pre-mark lives exclusively
  in `FileUploadController.uploadFile:175`. My earlier "pre-mark collision"
  theory was wrong; the architecture is correct.
- **Profile dead keys** (in
  `OpenELIS-Global-2/projects/analyzer-profiles/file/*.json`):

  | Profile file         | `column_spec` | `required_columns` | `optional_columns`         | `default_test_mappings` |
  | -------------------- | ------------- | ------------------ | -------------------------- | ----------------------- |
  | fluorocycler-xt.json | ❌            | ✅                 | ✅ (top-level)             | ✅ (stays)              |
  | quantstudio.json     | ✅            | ❌                 | ✅ (nested in column_spec) | ✅ (stays)              |
  | genexpert-csv.json   | ❌            | ✅                 | ✅ (top-level)             | ✅ (stays)              |
  | tecan-f50.json       | ❌            | ❌                 | ❌                         | ✅ (stays)              |
  | multiskan-fc.json    | ❌            | ❌                 | ❌                         | ✅ (stays)              |
  | wondfo-csv.json      | ✅            | ❌                 | ✅ (nested in column_spec) | ✅ (stays)              |

  `fallback_mapping` does NOT appear in any file (earlier session confusion: a
  `grep \| pattern` returned hits on other keys that I mis-attributed).
  `default_test_mappings` is correctly present in all 6 and stays per the simple
  model.

---

## §1 — Gate 1 Fluorocycler Unblock (blocks 3rd video)

**Tactical, ~10 min of code change**.

### §1.1 — Delete the early-return guard

**File**:
`OpenELIS-Global-2/src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java`

**Delete lines 66-70**:

```java
if (analyzer.getImportDirectory() != null && !analyzer.getImportDirectory().isBlank()) {
    LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateFromProfile",
            "FILE config already exists for analyzer " + analyzerId);
    return;
}
```

**Rationale**: the guard was defensive paranoia that converted profile re-apply
into a silent no-op. Under the simple model, profile re-apply is always
idempotent overwrite. The caller
(`AnalyzerRestController.POST /rest/analyzer/analyzers` line 287) is already
guarded against double-calls at the controller level — the service-layer guard
added nothing but silent staleness.

### §1.2 — Verification

1. Rebuild webapp image:
   `DOCKER_BUILDKIT=1 docker build --platform linux/amd64 -t itechuw/openelis-global-2:local .`
2. `./scripts/restart-stack.sh --clean` (already wipes postgres bind mount
   correctly per earlier session fix)
3. Verify post-boot: `SELECT COUNT(*) FROM analyzer` returns 0 (no preseeded
   rows — Liquibase `006-per-analyzer-test-mappings.xml` conditional INSERT
   fires 0 times on fresh DB)
4. Run Fluorocycler test only:
   ```
   COMPOSE_PROFILES=demo docker compose -f compose.yaml \
     -f docker-compose.validate.yml -f docker-compose.local-images.yml \
     -f compose.letsencrypt.yaml run --rm demo-tests \
     npx playwright test --project=harness-demo-video --grep FluoroCycler
   ```
5. Verify DB:
   `SELECT column_mappings_json FROM analyzer WHERE name = 'Demo: FluoroCycler XT'`
   → keys must match profile
   `['Sample ID', 'Row', 'Col', 'Type', 'Calc. Conc.', 'Result']` exactly
6. Verify downstream chain: `SELECT COUNT(*) FROM sample` = 1, `analysis` = 1,
   `result` = 1
7. Back up video:
   `cp -r test-results/test-output/demo-harness-...-fluoro.../video.webm gate1-videos/20260411-all3/fluoro.webm`

**Acceptance**: 87 `analyzer_results` persisted for Fluorocycler, video artifact
captured and backed up, test exits 0.

---

## §2 — Profile Schema Cleanup (OE PR #3372 + distro PR #4)

**Delete dead top-level keys** from profile files. Ground-truth verified
targets:

### §2.1 — OE repo (`projects/analyzer-profiles/file/`)

- `fluorocycler-xt.json`: delete `required_columns`, `optional_columns`
- `quantstudio.json`: delete `column_spec` (which nests `optional_columns`
  inside it — delete the entire nested structure)
- `genexpert-csv.json`: delete `required_columns`, `optional_columns`
- `wondfo-csv.json`: delete `column_spec` (nested `optional_columns` too)
- `tecan-f50.json`, `multiskan-fc.json`: no dead keys, unchanged

**Keep in all files**: `profileMeta`, `category`, `protocol`,
`supported_extensions`, `column_mapping`, `default_test_mappings`,
`configDefaults`, `sheet_detection` (where present).

### §2.2 — Distro repo mirror

Mirror the same deletions in
`openelis-madagascar-distro/configs/analyzer-profiles/file/*.json`.

### §2.3 — Verification

```bash
grep -rln '"column_spec"\|"required_columns"\|"optional_columns"' \
  OpenELIS-Global-2/projects/analyzer-profiles/ \
  openelis-madagascar-distro/configs/analyzer-profiles/
# expect: zero output
```

**Rationale**: these keys are not consumed by the single-writer creation flow.
`autoCreateFromProfile` reads only `column_mapping` and `configDefaults`. Dead
schema is legacy complexity per Principle X.

---

## §3 — Scanner as UX Helper (bridge PR #34)

### §3.1 — New endpoint: `POST /admin/upload/scan`

**File**:
`openelis-analyzer-bridge/src/main/java/org/itech/ahb/controller/FileUploadController.java`

Add a new method that accepts multipart `(analyzerId, file)`, runs the scanner
against a temp file, returns JSON
`{"suggestion": "VIH-1", "confidence": "selfDeclared" | "noDeclaration" | "ambiguous" | "notInterpretable"}`,
cleans up the temp file. The scanner logic (synonyms table, column mapping
lookup) stays unchanged — only its invocation path moves from the `uploadFile`
gate to this new advisory endpoint.

### §3.2 — Delete inline scanner calls from `uploadFile`

Delete from `FileUploadController.uploadFile(...)`:

- The `scanner.scan(...)` call at **line 201**
- The `validateScanAgainstAdmin(...)` call at **line 202**
- The `if (!outcome.proceed) return errorHtml(...)` block (already dead after my
  earlier advisory fix, but the variable assignments still exist)
- The `validateScanAgainstAdmin` helper method entirely (**lines 229-267**)
- The related `outcome.warning` reference in the success banner string (lines
  ~225) — banner becomes a plain success message

**Keep**: everything else in `uploadFile` — analyzer lookup, test code in
mapping-set validation, filename safety check, file write, pre-mark state store
call, `processFile` invocation.

### §3.3 — HTML form: pre-fill dropdown from /scan

**File**:
`openelis-analyzer-bridge/src/main/resources/static/admin/upload/index.html`

Add a `change` handler on `#file-input` that:

1. Reads the selected analyzer's id from `#analyzer-select`
2. POSTs the file to `/admin/upload/scan` as multipart
3. Reads the JSON response
4. If the response contains a `suggestion`, sets
   `#test-select.value = suggestion` (admin can still override by clicking the
   dropdown)

Non-blocking: if the scanner errors or returns no suggestion, the dropdown stays
at whatever the admin previously picked (or default).

### §3.4 — Verification

- `POST /admin/upload/scan` with HIV-result.xlsx + analyzerId for Fluorocycler →
  response has `suggestion: "VIH-1"`
- `POST /admin/upload` with HIV-result.xlsx for Fluorocycler → succeeds with 87
  results (same as before, no scanner gate)
- Browser demo: admin picks file, dropdown auto-selects VIH-1, admin can change
  it, admin clicks upload, success banner

---

## §4 — Wire Two-Way Bridge↔Webapp Sync (OE PR #3372)

Bridge already exposes the full surface. v5 work is webapp-side consumption.

### §4.1 — `BridgeRegistrationService.fetchBridgeState()`

**File**:
`OpenELIS-Global-2/src/main/java/org/openelisglobal/analyzer/service/BridgeRegistrationService.java`

Add:

```java
public List<Map<String, Object>> fetchBridgeState() {
    // HTTP GET https://{bridge-host}:8443/api/analyzers
    // with Basic auth (bridge:changeme via properties)
    // parse JSON response (Map from oeAnalyzerId → AnalyzerEntry)
    // return flattened list of {id, name, protocol, mappedTestCodes, ...}
}
```

Used by the analyzer-detail view in the webapp to show bridge's view
side-by-side with DB for drift detection.

### §4.2 — `BridgeRegistrationService.syncAll()`

Add:

```java
public boolean syncAll(List<Analyzer> webappAnalyzers) {
    // Build request body: array of registration payloads, one per
    // analyzer (same shape as POST /register but as a list)
    // HTTP PUT https://{bridge-host}:8443/api/analyzers/sync
    // Bridge reconciles: removes analyzers NOT in the list, adds/updates
    // the ones IN the list
    // returns true on 2xx
}
```

### §4.3 — Call `syncAll()` on webapp boot

**File**:
`OpenELIS-Global-2/src/main/java/org/openelisglobal/analyzer/service/AnalyzerBridgeStartupRegistrar.java`

After the existing per-analyzer `registerFile`/`registerTcp` loop in
`onStartup`, call `BridgeRegistrationService.syncAll(allAnalyzers)` as a final
reconciliation step. Handles:

- Bridge restarted between webapp's DB load and the per-analyzer push
- Bridge has a stale entry from an analyzer the webapp deleted
- Partial registration failure during webapp boot

### §4.4 — Verification

- Stop the bridge container, add a fake `AnalyzerEntry` to its local state via a
  test (or skip this check and rely on next webapp boot)
- Restart webapp → `syncAll()` fires → bridge's fake entry is removed
- `GET /api/analyzers` from the bridge returns only the analyzers the webapp's
  DB has

---

## §5 — No-Liquibase-Seeds Invariant

**Rank-1 feedback**: "we should not have any preseeded analyzers with liquibase
ever!!"

### §5.1 — Audit

Liquibase `006-per-analyzer-test-mappings.xml` contains a conditional
`INSERT INTO analyzer` as a backfill for orphan `analyzer_test_map` rows. On a
fresh DB (no baseline analyzer_test_map seeds), this INSERT fires 0 times.
**Verify** by querying after `--clean` restart:

```sql
SELECT COUNT(*) FROM clinlims.analyzer;
-- expect: 0
```

### §5.2 — Enforce in restart script

Add to `openelis-madagascar-distro/scripts/restart-stack.sh` after the readiness
check:

```bash
echo "[6/6] Verifying no pre-seeded analyzers..."
DB_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims \
  -tAc "SELECT COUNT(*) FROM clinlims.analyzer" 2>/dev/null | tr -d '[:space:]')
if [[ "$DB_COUNT" != "0" ]]; then
  echo "[6/6] FAIL: fresh DB has $DB_COUNT pre-seeded analyzer rows"
  docker exec openelisglobal-database psql -U clinlims -d clinlims \
    -c "SELECT id, name, analyzer_type_id FROM clinlims.analyzer;"
  exit 1
fi
echo "    OK: zero pre-seeded analyzers"
```

If this check fires, that's a bug to root-cause and fix before Gate 1 can be
called complete.

---

## §6 — Full Gate 1 Verification Procedure

After §1-§5 land in their respective PRs:

1. **Commit** changes to each PR:
   - **OE**: §1 `FileImportServiceImpl` early-return delete + §4.1/§4.2/§4.3
     sync wiring + §2.1 profile schema cleanup
   - **Bridge**: §3.1 new scan endpoint + §3.2 inline-scanner delete
     - §3.3 HTML scan handler
   - **Distro**: §2.2 profile schema cleanup mirror + §5.2 restart script
     invariant check
2. **Rebuild** all three images (webapp, bridge, demo-tests)
3. **Bump** OE's bridge submodule pointer to the new bridge HEAD
4. **Push** all three with `--force-with-lease` (or regular push if only new
   commits are added)
5. `./scripts/restart-stack.sh --clean`
6. Verify post-restart: `SELECT COUNT(*) FROM analyzer == 0` (via the new §5.2
   check)
7. **Run all 3 tests**:
   ```
   npx playwright test --project=harness-demo-video \
     --grep 'QuantStudio|FluoroCycler'
   ```
8. Verify `3 passed` + videos captured under
   `test-results/test-output/*/video.webm`
9. Back up all 3 videos to `gate1-videos/20260411-all3/`
10. Update PR descriptions: replace v4 narrative with v5 simple-model framing

---

## Completion Criteria ("v5 done")

- [ ] §1 Fluorocycler Gate 1 video captured + backed up
- [ ] All 3 Gate 1 videos (QS5, QS7, Fluorocycler) reviewed by user and accepted
- [ ] `FileImportServiceImpl.autoCreateFromProfile` early-return deleted
- [ ] Profile schema cleaned (no `column_spec` / `required_columns` /
      `optional_columns` in any profile file, OE repo + distro)
- [ ] `POST /admin/upload/scan` endpoint added; inline scanner call deleted from
      `uploadFile`; HTML form pre-selects dropdown from scan response
- [ ] `BridgeRegistrationService.fetchBridgeState()` + `syncAll()` added;
      `AnalyzerBridgeStartupRegistrar` calls `syncAll` on boot
- [ ] Post-boot `SELECT COUNT(*) FROM analyzer == 0` invariant enforced in
      `restart-stack.sh`
- [ ] All three PRs still MERGEABLE after the above changes
- [ ] PR descriptions updated to reflect v5 scope (simple model, two-way sync,
      scanner as UX helper)

---

## Out of Scope (deferred to v6+)

- **Relational `analyzer_column_mapping` table** (replacing
  `column_mappings_json` TEXT blob). Architectural improvement; real but not
  blocking.
- **Profile file canonicalization to one on-disk location** (collapse OE-repo +
  distro + container-view down to one). Sync the three is less work than
  collapsing, for now.
- **Periodic bridge drift check scheduler** — nice-to-have, not Gate 1 blocker.
- **Gate 2: main-repo CI alignment with real-file reality**. Plan unchanged from
  v4.
- **Phase C: Tecan, Multiskan, GeneXpert file ingestion**. Plan unchanged from
  v4.
- **`acceptAndVerifyResults` single-row-accept limitation** (file analyzers with
  N sample IDs only accept the first). Sufficient for Gate 1; enhancement for
  later.
- **Webapp `AnalyzerForm` UI for editing column_mapping / test_mappings
  post-creation** — already works via existing form fields; no v5 changes
  needed.

---

## Explicitly Rejected (not appearing in v5)

- **Scanner as gate on admin upload path** — deleted in §3.2
- **Dead profile schema keys** (`column_spec`, `required_columns`,
  `optional_columns`) — deleted in §2
- **`autoCreateFromProfile` silent no-op early-return** — deleted in §1
- **Pre-seeded analyzers in Liquibase or baseline CSVs** — invariant enforced in
  §5
- **One-way webapp→bridge sync** — replaced by two-way in §4
- **`Analyzer.defaultTestCode` singular scalar** — already reverted in v4, stays
  rejected
- **`fallback_mapping` profile key** — never actually existed in OE repo files;
  my earlier plan mentioned it as a dead key, but ground-truth verification
  showed zero matches in any file. Not a target because there's nothing to
  delete.

---

## Critical Files (paths for executor)

### OpenELIS-Global-2 (webapp) — PR #3372

- `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java`
  — delete lines 66-70 (early-return guard)
- `src/main/java/org/openelisglobal/analyzer/service/BridgeRegistrationService.java`
  — add `fetchBridgeState()`, `syncAll(List<Analyzer>)`
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerBridgeStartupRegistrar.java`
  — add `syncAll()` call at end of `onStartup`
- `projects/analyzer-profiles/file/fluorocycler-xt.json` — delete
  `required_columns`, `optional_columns`
- `projects/analyzer-profiles/file/quantstudio.json` — delete `column_spec`
  (entire nested block)
- `projects/analyzer-profiles/file/genexpert-csv.json` — delete
  `required_columns`, `optional_columns`
- `projects/analyzer-profiles/file/wondfo-csv.json` — delete `column_spec`
  (entire nested block)

### openelis-analyzer-bridge — PR #34

- `src/main/java/org/itech/ahb/controller/FileUploadController.java` — delete
  scanner call at line 201 + validateScanAgainstAdmin call at line 202 +
  `validateScanAgainstAdmin` helper method at lines 229-267; add new
  `POST /admin/upload/scan` handler method
- `src/main/resources/static/admin/upload/index.html` — add file-selection
  change handler that POSTs to `/admin/upload/scan` and pre-selects the
  suggestion in `#test-select`

### openelis-madagascar-distro — PR #4

- `configs/analyzer-profiles/file/*.json` — mirror OE-repo profile cleanup
- `scripts/restart-stack.sh` — add post-readiness
  `SELECT COUNT(*) FROM analyzer == 0` invariant check

---

## History (prior plans + completed work — reference only)

### Plan lineage

- **v1** (`madagascar-file-import-profile-alignment.md`): initial profile
  alignment, pre-Phase-1
- **v2** (`...-v2.md`): `default_test_code_overrides` filename routing design
  (reverted)
- **v3** (`...-v3.md`): `Analyzer.defaultTestCode` scalar approach (partially
  shipped, then reverted)
- **v4** (`...-v4.md`): file-level self-declaration scanner as gate (shipped,
  then retargeted by this v5 as over-engineered)
- **v5** (this plan): simplification + two-way sync wiring + Gate 1 finish

### Completed (shipped in prior iterations, not re-planned)

- **Phase 1**: non-destructive FileWatcher, FileStateStore SQLite,
  `/admin/file-state` REST endpoint — SHIPPED in bridge PR #34
- **Phase A**: profile rewrites (Fluorocycler v2.0.0, QuantStudio arbovirus),
  test catalog CSV (HIVVIRALLOAD, DENGUEPCR, CHIKVPCR, ZIKVPCR), FileWatcher
  multi-observer refactor — SHIPPED
- **v4 reverts**: `Analyzer.defaultTestCode` column drop + AnalyzerForm
  TextInput revert + Liquibase 014 forward migration + parser parameter rename
  `defaultTestCode → perFileTestCode` — SHIPPED
- **Gate 1 QS5 + QS7 videos**: captured, backed up to
  `gate1-videos/20260411-qs5-qs7-passing/`
- **PR hygiene**: three PRs rebased + squashed + formatted + titles and bodies
  updated + force-pushed — SHIPPED this session
- **Scanner advisory fix** (bridge commit `d60c28a`): scanner results now
  `passWithWarning` in the upload path — SHIPPED. v5 takes the next step: remove
  the inline scanner call entirely.
- **Pre-mark state store** (bridge commit `a160aa2`): FileUploadController
  pre-marks uploaded file as PROCESSED before disk write to prevent FileWatcher
  race — SHIPPED. Confirmed correct; not touched by v5.
- **`testMappings` through runtime registration** (bridge + webapp):
  RegistrationRequest DTO accepts `testMappings`, wired to bridge
  `AnalyzerEntry.mappedTestCodes` — SHIPPED. Required for the scanner suggestion
  logic (§3.1) and for upload UI dropdown population.

### Known pre-existing bugs (outside v5 scope)

- `acceptAndVerifyResults` filters staging rows by single scraped accession
  (file analyzers with many samples accept only 1 row). Sufficient for Gate 1
  test pass; enhancement deferred.
- `AuditTrailDAOImpl` "Baton System User ID is null" error during teardown
  delete. Pre-existing audit-trail bug, cosmetic log noise, teardown still
  succeeds.

---

## Why v5 is short vs v4

v4 grew to 2519 lines because each pivot (v1 → v2 → v3 → v4) added layers to an
increasingly complicated architecture. v5 describes the _simple_ system and the
_minimal_ set of deletions + one piece of new wiring (two-way sync) needed to
get the code to match.

Most of what v4 introduced (scanner gate, dual mapping sources, multi-step
validation chains) is deleted in v5 rather than re-described. Subtractive plans
are shorter than additive ones.

If v6 is needed, it should stay the same size as v5 or shrink. Plan length is a
reliable canary for architectural drift.
