# OGC-1054 F0 Foundation Salvage Manifest

**Updated:** 2026-08-13  
**Checkpoint:** OE-F0  
**Status:** `IN_PROGRESS`  
**Repository:** `DIGI-UW/OpenELIS-Global-2`  
**Branch:** `codex/ogc-1054-f0-foundation`  
**Initial base:** OE-R0 `56ab95ace2c88b9b6f41954c2d828ec17f096d3a`  
**Historical source:** PR #3792 at
`d985e6ce727b555c414b7db1129b3b1eeaf664cc`

This is the required F0 acceptance record for the
[authoritative OGC-1054 roadmap](./ogc-1054-analyzer-feature-roadmap.md). It
classifies behavior from PR #3792 against current code and the fixed
Bridge/OpenELIS ownership boundary. It is not a code-copy list. A historical
route, class, payload, persistence choice, or passing old demo is not evidence
that the behavior belongs in the target architecture.

## Scope

F0 preserves only small, architecture-compatible foundations that can be
characterized independently of the E0 contract decision. Its first implemented
slice is the static `MVP-023` guard for UI-only demo stories.

F0 does not implement Analyzer Types, profile lifecycle, mapping persistence,
guided setup, operational QC readiness, activation, traffic handling, or the
final Playwright story. Those belong to E0 and M1-M4 after the relevant
producer/consumer contracts are accepted.

## Permitted Dispositions

- `PROVIDED_BY_DEVELOP`: current code already owns the compatible behavior; F0
  names or adds a characterization test but does not replace production code.
- `REIMPLEMENT_WITH_TDD`: the outcome is compatible, but implementation starts
  only at the named checkpoint with a failing test at the owning layer.
- `DROP_INCOMPATIBLE`: the historical implementation or acceptance claim
  conflicts with the fixed architecture, duplicates a path, is stale evidence,
  or is unrelated churn. A later replacement checkpoint may still deliver the
  product outcome.

No commit from #3792 is cherry-picked. Origin commits below identify provenance
only.

## Behavior Classification

| ID       | Behavior considered                                                                                                                     | #3792 origin                                                                                                        | Current-code equivalent / characterization                                                                                                       | Disposition and rationale                                                                                                                               | Criterion / checkpoint         |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| `F0-B01` | Deterministic desired Bridge registration, including explicit empty QC, control-lot, and test-binding collections                       | `261ce6294`, `e04cb7c64`, `88e728133`, `6229292c2`; `BridgeRegistrationService*`, `AnalyzerBridgeStartupRegistrar*` | Current services already emit `controlLots` and `testCodeLoinc`; `qcRules` can still be absent and collection ordering is not contractual        | `REIMPLEMENT_WITH_TDD`: exact payload semantics require BR-E0/OE-E0 producer-consumer fixtures before change                                            | MVP-017; E0                    |
| `F0-B02` | String-safe analyzer, Test, and control-lot identifiers                                                                                 | `261ce6294`, `b56105ecf`; Bridge registration and `ControlLotSetup`                                                 | Current `QCControlLot` and Bridge registration paths use String IDs; add focused characterization rather than porting UI payload helpers         | `PROVIDED_BY_DEVELOP`: retain current type semantics and prove them in F0                                                                               | MVP-015/017; F0                |
| `F0-B03` | Shipped profile summaries and readiness read from OpenELIS filesystem assets                                                            | `c03109e07`, `b8f7526d7`, `890747735`, `f71d152ca`                                                                  | Current OpenELIS profile files are a transitional mirror                                                                                         | `DROP_INCOMPATIBLE`: Bridge owns portable profiles; BR-M1 and OE-M1 reimplement the lab-facing composed view                                            | MVP-001; M1                    |
| `F0-B04` | `defaultConfigId` applies copied profile defaults and mappings during OpenELIS analyzer creation                                        | `d9ebc1866`, `4c514bb70`, `35e443d52`, `b56105ecf`                                                                  | Current create flow and copied plugin JSON require migration characterization                                                                    | `DROP_INCOMPATIBLE`: do not extend copied-profile authority; characterize and cut over in E0, then implement setup through the Bridge revision contract | MVP-003/010/017; E0-M3         |
| `F0-B05` | Catalog-bound qualitative result mapping, active-option filtering, save, and reload                                                     | `7c1796bd2`, `ad0cd28f9`, `2934037e4`, `d1e16bda6`, `6229292c2`                                                     | OpenELIS owns local Test and Result Option bindings; current branch has older qualitative mapping paths but not the accepted editor              | `REIMPLEMENT_WITH_TDD`: derive persistence in E0 and prove server ownership, enabled save, persistence, and reload in M2                                | MVP-005/006/007; E0-M2         |
| `F0-B06` | Pending test/value resolution changes future matching without losing raw input                                                          | `7c1796bd2`, `2934037e4`, `6229292c2`                                                                               | Current pending-code and result paths are partial and duplicated                                                                                 | `REIMPLEMENT_WITH_TDD`: M2 owns catalog-safe resolution and M4 owns held traffic plus next-message proof                                                | MVP-019/020; M2/M4             |
| `F0-B07` | Audited mapping verification with actor, time, revision, and fingerprint                                                                | `88e728133`, `2934037e4`, `6229292c2`                                                                               | Historical implementation stores mutable metadata in plugin JSON and couples whole-setup readiness                                               | `REIMPLEMENT_WITH_TDD`: OpenELIS audit is required, but fingerprint inputs and durable state follow E0/M2 contracts                                     | MVP-008/012; E0-M2             |
| `F0-B08` | Operational QC rule/control-lot setup and activation readiness                                                                          | `e04cb7c64`, `88e728133`, `6229292c2`, `b56105ecf`                                                                  | Current `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard services exist; the reviewed demo couples QC and mapping sign-off incorrectly | `REIMPLEMENT_WITH_TDD`: use existing QC entities only and keep operational QC readiness independent in M3                                               | MVP-015/016; M3                |
| `F0-B09` | Bridge resync and activation occur after committed local changes                                                                        | `4c514bb70`, `118ee0137`, `ccd9d5224`, `7909077a8`, `6229292c2`                                                     | Current startup and status listeners provide a baseline, but historical fixes are tied to the old payload/readiness model                        | `REIMPLEMENT_WITH_TDD`: establish service transaction/event boundaries after E0, then prove update/delete/status synchronization in M3                  | MVP-016/017; E0/M3             |
| `F0-B10` | One inline setup shell with URL state, breadcrumbs, summaries, and stable Carbon controls                                               | `7ce95f561`, `f71d152ca`, `b56105ecf`, `b1f929b87`, `7148bae52`                                                     | Current R0 base has list/form/type pages but not the accepted full story                                                                         | `REIMPLEMENT_WITH_TDD`: retain the functional intent, use current reusable components, and implement in M1-M3 with RTL router tests                     | MVP-002/010/011/022; M1-M3     |
| `F0-B11` | One mapping editor and safe lifecycle, with no hard delete, copy shortcut, duplicate editor, or duplicate pending queue                 | `35e443d52`, `2934037e4`, `f71d152ca`, `b56105ecf`                                                                  | Current R0 base still contains legacy mapping panels, hard delete, and copy mappings                                                             | `DROP_INCOMPATIBLE`: do not port intermediate removals piecemeal; M1/M2 removes superseded paths alongside replacement behavior                         | MVP-004/009; M1-M2             |
| `F0-B12` | Static enforcement that demo Playwright stories cannot use request APIs, browser fetch, response/poll synchronization, or network stubs | `2934037e4`; `pw-demo-no-backend-access*`                                                                           | Current rule blocks only a small subset and has no unit test                                                                                     | `REIMPLEMENT_WITH_TDD`: selected F0 increment; red commit `da3e65ebc`                                                                                   | MVP-023; F0                    |
| `F0-B13` | Demo authentication is performed through the visible login UI                                                                           | `4f832b1f6`, `2934037e4`; `harness-ui-auth.setup.ts`, Playwright project config                                     | Current shared auth setup uses request APIs; no accepted analyzer full-story spec exists yet                                                     | `REIMPLEMENT_WITH_TDD`: M4 creates a dedicated visible-login demo setup and static project guard without disrupting foundational suites                 | MVP-023; M4                    |
| `F0-B14` | Analyzer transport tests and final MP4 story are correctly separated                                                                    | `a978ea80d`, `821e6cb8d`, `7beb83f0d`, `502654944`, `b1f929b87`, `7148bae52`                                        | Current `astm-genexpert-results.spec.ts` is labeled demo but directly calls the simulator                                                        | `REIMPLEMENT_WITH_TDD`: reclassify direct-transport coverage as foundational in F0; M4 creates the UI-only story and G0 records it                      | MVP-018/021/023/024; F0/M4/G0  |
| `F0-B15` | OpenELIS ships or mounts generic analyzer runtime/profile implementations                                                               | `995910944`, `890747735`, `3bb313f75`, `545017f9b`, `0558af6aa`                                                     | Historical Docker/plugin changes put runtime/profile concerns in the application delivery path                                                   | `DROP_INCOMPATIBLE`: Bridge owns listeners, parsing, FILE watching, and portable profiles; no OE runtime fallback is extended                           | MVP-017/021; BR-E0/BR-M1/BR-M4 |
| `F0-B16` | Deterministic catalog and simulator fixtures support accepted scenarios                                                                 | `995910944`, `ad0cd28f9`, `d1e16bda6`, `6229292c2`                                                                  | Existing harness data is useful provenance but was tailored to the superseded UI path                                                            | `REIMPLEMENT_WITH_TDD`: E0 characterizes catalog cardinality; MOCK-M4 owns transport fixtures; G0 fixture loading is precondition only                  | MVP-006/007/021; E0/MOCK-M4/G0 |
| `F0-B17` | RBAC is enforced for analyzer administration endpoints                                                                                  | `632979648`, `2934037e4`, `6229292c2`                                                                               | Current controller security tests already cover the existing analyzer surface                                                                    | `PROVIDED_BY_DEVELOP`: retain baseline tests; each later endpoint adds its own allowed/denied tests                                                     | Security gate; F0 and later    |
| `F0-B18` | Carbon/localization/screenshot polish from the superseded flow                                                                          | `7ce95f561`, `48b231242`, `b56105ecf`                                                                               | Some reusable presentation ideas exist, but the old screens do not satisfy current product QA                                                    | `REIMPLEMENT_WITH_TDD`: apply current Carbon patterns at the owning milestone and compare new screenshots to current functional mocks                   | MVP-011/022; M1-M4             |
| `F0-B19` | July local/demo videos and completion claims establish MVP acceptance                                                                   | `bf371fed1`, `02b172002`, `2c840a55b`, `15044cff6`, `4344821bc`, `a12798df8`, `94585fded`                           | Evidence targets an obsolete app SHA and a narrower story                                                                                        | `DROP_INCOMPATIBLE`: retain as git history only; G0 requires exact-RC remote UAT and a newly inspected MP4                                              | MVP-024; G0                    |
| `F0-B20` | The #3792 roadmap/spec version remains authoritative                                                                                    | `d985e6ce7` and earlier roadmap/spec commits                                                                        | OE-R0 PR #4049 carries the sole current roadmap; the historical branch has a divergent blob                                                      | `DROP_INCOMPATIBLE`: after F0 opens, retitle and close #3792 while preserving its branch                                                                | R0/F0 provenance               |

## Historical Commit Coverage

Every commit unique to #3792 is covered by at least one behavior row above.
This grouping prevents a multi-purpose commit from being mistaken for an atomic
salvage unit.

| Behavior rows | Covered #3792 commits                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| B01-B02       | `261ce6294`, `e04cb7c64`, `88e728133`, `6229292c2`                                                     |
| B03-B04       | `c03109e07`, `d9ebc1866`, `b8f7526d7`, `4c514bb70`, `890747735`, `35e443d52`, `f71d152ca`, `b56105ecf` |
| B05-B07       | `7c1796bd2`, `90c55cbd6`, `ad0cd28f9`, `2934037e4`, `d1e16bda6`, `6229292c2`                           |
| B08-B09       | `e04cb7c64`, `88e728133`, `118ee0137`, `ccd9d5224`, `7909077a8`, `ff7bcc2b1`, `6229292c2`              |
| B10-B11       | `7ce95f561`, `35e443d52`, `2934037e4`, `f71d152ca`, `b56105ecf`, `b1f929b87`, `7148bae52`              |
| B12-B14       | `a978ea80d`, `821e6cb8d`, `7beb83f0d`, `4f832b1f6`, `2934037e4`, `502654944`, `b1f929b87`, `7148bae52` |
| B15-B16       | `995910944`, `890747735`, `3bb313f75`, `ad0cd28f9`, `545017f9b`, `d1e16bda6`, `0558af6aa`              |
| B17-B18       | `632979648`, `7ce95f561`, `2934037e4`, `48b231242`, `b56105ecf`                                        |
| B19-B20       | `bf371fed1`, `02b172002`, `2c840a55b`, `15044cff6`, `4344821bc`, `a12798df8`, `94585fded`, `d985e6ce7` |

## TDD Record

### Increment F0-I1: UI-only demo guard

**Acceptance:** `F0-B12`, partial `MVP-023` static enforcement.

**Current-code baseline:**

- `frontend/eslint-local-rules/pw-demo-no-backend-access.js` blocks
  `waitForResponse`, console/page-error listeners, and only
  `page.request.get/put/delete` shapes.
- It has no rule-level test on the R0 base.
- `frontend/playwright/tests/demo/harness/astm-genexpert-results.spec.ts`
  directly calls the simulator through `page.request.post`, so it is a harness
  integration test mislabeled as demo evidence.

**Red commit:** `da3e65ebc` (`test(playwright): characterize UI-only demo guard`)

```bash
cd frontend
npm test -- --run eslint-local-rules/pw-demo-no-backend-access.test.js
```

Observed: 14 tests ran; 11 failed and 3 passed. Missing enforcement included
direct and aliased request contexts, computed request calls, browser fetch,
`expect.poll`, and page/context network stubs. Existing `waitForResponse`
enforcement and visible UI interaction examples passed.

**Green:** pending.  
**Refactor:** pending.

## Layer Validation

| Layer                    | F0-I1 status     | Rationale / evidence                                                    |
| ------------------------ | ---------------- | ----------------------------------------------------------------------- |
| ESLint rule unit         | `RUN_RED`        | Rule behavior belongs in a fast AST-level unit test; red evidence above |
| Frontend lint            | `PENDING`        | Run after green against all Playwright files                            |
| Playwright bucket guard  | `PENDING`        | Proves every spec remains assigned after reclassification               |
| RTL                      | `NOT_APPLICABLE` | No React behavior changes in F0-I1                                      |
| Backend unit/persistence | `NOT_APPLICABLE` | No Java or persistence behavior changes in F0-I1                        |
| Cross-repo contract      | `NOT_APPLICABLE` | No runtime contract changes in F0-I1                                    |
| Harness execution        | `LATER`          | Reclassified ASTM integration remains an M4 assembled-stack input       |
| UI-only Playwright story | `LATER`          | OE-M4 owns creation of the complete visible story                       |
| Remote UAT / MP4         | `LATER`          | OE-G0 only                                                              |

## Acceptance Crosswalk

| Criterion         | F0 evidence                                           | Remaining proof                                                                 |
| ----------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------- |
| MVP-023           | Rule-level red/green test, lint, and bucket guard     | M4 audit of the final UI-only spec and G0 execution/trace/video                 |
| Legacy-path guard | Direct-request ASTM spec reclassified as foundational | M4 final story contains no request APIs, fetch, polling, force, waits, or stubs |

## Decisions

1. `openelis-work` is not used in this manifest to select code, persistence,
   repository ownership, or tests.
2. A test that sends analyzer traffic directly is useful integration coverage,
   but it is not demo evidence. It belongs in a foundational harness bucket.
3. F0 does not settle E0 payload or persistence contracts by copying #3792.
4. The old branch remains immutable provenance until its PR is closed as
   superseded after the F0 PR opens.

## Issues and Ambiguities

| ID             | Status     | Evidence and deterministic action                                                                                                         |
| -------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `ISSUE-F0-001` | `OPEN`     | Current ASTM spec is in the demo bucket but calls `page.request.post`; reclassify it as foundational in F0-I1 and run lint/bucket guards. |
| `AMB-F0-001`   | `RESOLVED` | The manifest classifies all #3792 behavior groups without treating any commit as an atomic salvage unit.                                  |
| `AMB-E0-001`   | `OPEN`     | Exact Bridge payload and local binding persistence remain E0 decisions; F0 makes no contract change.                                      |

## Final Gate

Pending green/refactor commits, targeted and broader frontend gates, PR CI,
review-thread closure, and update of the authoritative status ledger. F0 cannot
become `ACCEPTED` before OE-R0 is approved and merged.
