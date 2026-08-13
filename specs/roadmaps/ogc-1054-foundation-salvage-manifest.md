# OGC-1054 F0 Foundation Salvage Manifest

**Updated:** 2026-08-13  
**Checkpoint:** OE-F0  
**Status:** `IN_PROGRESS`  
**Repository:** `DIGI-UW/OpenELIS-Global-2`  
**Branch:** `codex/ogc-1054-f0-foundation`  
**Canonical predecessor:** OE-R0
`53e720c05aa1f6abee16a173bd76a3a4d0135568`
**Predecessor roadmap blob:**
`cb3e9f375375d42b54c88d75b24a57d24e547a96`
**Historical source:** PR #3792 at
`d985e6ce727b555c414b7db1129b3b1eeaf664cc`

**Red commits:** `aae2e6c640a4fc3c1b9bc22c1f04fff0281c8ad8`,
`875fe34f06bef2b1d68469f5fdd03cfdb0b2aeae`,
`f5f32b9ac6e55679db5c71b5bb70f6b504f2aea0`, and
`77b023add0fac2bc1a65527d09c1119299d7ffde`,
`7b12470690b387b64ddfd30b37af7f6f0fa070fd`, and
`d039fae4c`

**Green commits:** `784e4a37747befdb074678d6218664c0cd9d2859`,
`73425c750b1ae05fb7a7e860a3355b52e0407b86`, and
`35e06f987d5e26e5efc7b79bb08527d7458112ba`,
`612b1aabb52666f5fbb9c421accbe926b6dd1ada`, and
`877eea555`

**Refactor commit:** `ae6b170c0f5aa7a61c0f1b43bed3a59d529bbe3f`

This is the required F0 acceptance record for the
[authoritative OGC-1054 roadmap](./ogc-1054-analyzer-feature-roadmap.md). It
classifies behavior from PR #3792 against current code and the fixed
Bridge/OpenELIS ownership boundary. It is not a code-copy list. A historical
route, class, payload, persistence choice, or passing old demo is not evidence
that the behavior belongs in the target architecture.

## Scope

F0 preserves only small, architecture-compatible foundations that can be
characterized independently of the E0 contract decision. Its implemented
slices are direct and dependency-aware `MVP-023` guards for UI-only demo
stories, plus honest reclassification of transport/setup integration coverage.

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

| ID       | Behavior considered                                                                                                                     | #3792 origin                                                                                                        | Current-code equivalent / characterization                                                                                                       | Disposition and rationale                                                                                                                                            | Criterion / checkpoint         |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| `F0-B01` | Deterministic desired Bridge registration, including explicit empty QC, control-lot, and test-binding collections                       | `261ce6294`, `e04cb7c64`, `88e728133`, `6229292c2`; `BridgeRegistrationService*`, `AnalyzerBridgeStartupRegistrar*` | Current services already emit `controlLots` and `testCodeLoinc`; `qcRules` can still be absent and collection ordering is not contractual        | `REIMPLEMENT_WITH_TDD`: exact payload semantics require BR-E0/OE-E0 producer-consumer fixtures before change                                                         | MVP-017; E0                    |
| `F0-B02` | String-safe analyzer, Test, and control-lot identifiers                                                                                 | `261ce6294`, `b56105ecf`; Bridge registration and `ControlLotSetup`                                                 | Current `QCControlLot` and Bridge registration paths use String IDs; add focused characterization rather than porting UI payload helpers         | `PROVIDED_BY_DEVELOP`: retain current type semantics and prove them in F0                                                                                            | MVP-015/017; F0                |
| `F0-B03` | Shipped profile summaries and readiness read from OpenELIS filesystem assets                                                            | `c03109e07`, `b8f7526d7`, `890747735`, `f71d152ca`                                                                  | Current OpenELIS profile files are a transitional mirror                                                                                         | `DROP_INCOMPATIBLE`: Bridge owns portable profiles; BR-M1 and OE-M1 reimplement the lab-facing composed view                                                         | MVP-001; M1                    |
| `F0-B04` | `defaultConfigId` applies copied profile defaults and mappings during OpenELIS analyzer creation                                        | `d9ebc1866`, `4c514bb70`, `35e443d52`, `b56105ecf`                                                                  | Current create flow and copied plugin JSON require migration characterization                                                                    | `DROP_INCOMPATIBLE`: do not extend copied-profile authority; characterize and cut over in E0, then implement setup through the Bridge revision contract              | MVP-003/010/017; E0-M3         |
| `F0-B05` | Catalog-bound qualitative result mapping, active-option filtering, save, and reload                                                     | `7c1796bd2`, `ad0cd28f9`, `2934037e4`, `d1e16bda6`, `6229292c2`                                                     | OpenELIS owns local Test and Result Option bindings; current branch has older qualitative mapping paths but not the accepted editor              | `REIMPLEMENT_WITH_TDD`: derive persistence in E0 and prove server ownership, enabled save, persistence, and reload in M2                                             | MVP-005/006/007; E0-M2         |
| `F0-B06` | Pending test/value resolution changes future matching without losing raw input                                                          | `7c1796bd2`, `2934037e4`, `6229292c2`                                                                               | Current pending-code and result paths are partial and duplicated                                                                                 | `REIMPLEMENT_WITH_TDD`: M2 owns catalog-safe resolution and M4 owns held traffic plus next-message proof                                                             | MVP-019/020; M2/M4             |
| `F0-B07` | Audited mapping verification with actor, time, revision, and fingerprint                                                                | `88e728133`, `2934037e4`, `6229292c2`                                                                               | Historical implementation stores mutable metadata in plugin JSON and couples whole-setup readiness                                               | `REIMPLEMENT_WITH_TDD`: OpenELIS audit is required, but fingerprint inputs and durable state follow E0/M2 contracts                                                  | MVP-008/012; E0-M2             |
| `F0-B08` | Operational QC rule/control-lot setup and activation readiness                                                                          | `e04cb7c64`, `88e728133`, `6229292c2`, `b56105ecf`                                                                  | Current `AnalyzerQcRule`, `QCControlLot`, `QCResult`, and Westgard services exist; the reviewed demo couples QC and mapping sign-off incorrectly | `REIMPLEMENT_WITH_TDD`: use existing QC entities only and keep operational QC readiness independent in M3                                                            | MVP-015/016; M3                |
| `F0-B09` | Bridge resync and activation occur after committed local changes                                                                        | `4c514bb70`, `118ee0137`, `ccd9d5224`, `7909077a8`, `6229292c2`                                                     | Current startup and status listeners provide a baseline, but historical fixes are tied to the old payload/readiness model                        | `REIMPLEMENT_WITH_TDD`: establish service transaction/event boundaries after E0, then prove update/delete/status synchronization in M3                               | MVP-016/017; E0/M3             |
| `F0-B10` | One inline setup shell with URL state, breadcrumbs, summaries, and stable Carbon controls                                               | `7ce95f561`, `f71d152ca`, `b56105ecf`, `b1f929b87`, `7148bae52`                                                     | Current F0 base has list/form/type pages but not the accepted full story                                                                         | `REIMPLEMENT_WITH_TDD`: retain the functional intent, use current reusable components, and implement in M1-M3 with RTL router tests                                  | MVP-002/010/011/022; M1-M3     |
| `F0-B11` | One mapping editor and safe lifecycle, with no hard delete, copy shortcut, duplicate editor, or duplicate pending queue                 | `35e443d52`, `2934037e4`, `f71d152ca`, `b56105ecf`                                                                  | Current F0 base still contains legacy mapping panels, hard delete, and copy mappings                                                             | `DROP_INCOMPATIBLE`: reject from F0 salvage; M1-M3 remove superseded paths alongside accepted replacement behavior                                                   | MVP-004/009; M1-M3             |
| `F0-B12` | Static enforcement that demo Playwright stories cannot use request APIs, browser fetch, response/poll synchronization, or network stubs | `2934037e4`; `pw-demo-no-backend-access*`                                                                           | Direct rule and analyzer `harness-demo` runtime-import guard cover entry specs and demo-facing helpers                                           | `REIMPLEMENT_WITH_TDD`: red `aae2e6c64`/`875fe34f0`/`f5f32b9ac`/`77b023add`/`7b1247069`; green `784e4a377`/`73425c750`/`35e06f987`/`612b1aabb`; refactor `ae6b170c0` | MVP-023; F0                    |
| `F0-B13` | Demo authentication is performed through the visible login UI                                                                           | `4f832b1f6`, `2934037e4`; `harness-ui-auth.setup.ts`, Playwright project config                                     | Current shared auth setup uses request APIs; no accepted analyzer full-story spec exists yet                                                     | `REIMPLEMENT_WITH_TDD`: M4 creates a dedicated visible-login demo setup and static project guard without disrupting foundational suites                              | MVP-023; M4                    |
| `F0-B14` | Analyzer transport tests and final MP4 story are correctly separated                                                                    | `a978ea80d`, `821e6cb8d`, `7beb83f0d`, `502654944`, `b1f929b87`, `7148bae52`                                        | Three legacy analyzer specs inject traffic or poll through direct/helper backend access and are now foundational                                 | `REIMPLEMENT_WITH_TDD`: 13 cases remain in `harness-foundational`; M4 creates the first accepted UI-only story and G0 records it                                     | MVP-018/021/023/024; F0/M4/G0  |
| `F0-B15` | OpenELIS ships or mounts generic analyzer runtime/profile implementations                                                               | `995910944`, `890747735`, `3bb313f75`, `545017f9b`, `0558af6aa`                                                     | Historical Docker/plugin changes put runtime/profile concerns in the application delivery path                                                   | `DROP_INCOMPATIBLE`: Bridge owns listeners, parsing, FILE watching, and portable profiles; no OE runtime fallback is extended                                        | MVP-017/021; BR-E0/BR-M1/BR-M4 |
| `F0-B16` | Deterministic catalog and simulator fixtures support accepted scenarios                                                                 | `995910944`, `ad0cd28f9`, `d1e16bda6`, `6229292c2`                                                                  | Existing harness data is useful provenance but was tailored to the superseded UI path                                                            | `REIMPLEMENT_WITH_TDD`: E0 characterizes catalog cardinality; MOCK-M4 owns transport fixtures; G0 fixture loading is precondition only                               | MVP-006/007/021; E0/MOCK-M4/G0 |
| `F0-B17` | RBAC is enforced for analyzer administration endpoints                                                                                  | `632979648`, `2934037e4`, `6229292c2`                                                                               | Current controller security tests already cover the existing analyzer surface                                                                    | `PROVIDED_BY_DEVELOP`: retain baseline tests; each later endpoint adds its own allowed/denied tests                                                                  | Security gate; F0 and later    |
| `F0-B18` | Carbon/localization/screenshot polish from the superseded flow                                                                          | `7ce95f561`, `48b231242`, `b56105ecf`                                                                               | Some reusable presentation ideas exist, but the old screens do not satisfy current product QA                                                    | `REIMPLEMENT_WITH_TDD`: apply current Carbon patterns at the owning milestone and compare new screenshots to current functional mocks                                | MVP-011/022; M1-M4             |
| `F0-B19` | July local/demo videos and completion claims establish MVP acceptance                                                                   | `bf371fed1`, `02b172002`, `2c840a55b`, `15044cff6`, `4344821bc`, `a12798df8`, `94585fded`                           | Evidence targets an obsolete app SHA and a narrower story                                                                                        | `DROP_INCOMPATIBLE`: retain as git history only; G0 requires exact-RC remote UAT and a newly inspected MP4                                                           | MVP-024; G0                    |
| `F0-B20` | The #3792 roadmap/spec version remains authoritative                                                                                    | `d985e6ce7` and earlier roadmap/spec commits                                                                        | OE-R0 PR #4049 carries the sole current roadmap; the historical branch has a divergent blob                                                      | `DROP_INCOMPATIBLE`: #3792 is closed and labeled historical; preserve its branch only as provenance                                                                  | R0/F0 provenance               |

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

**Red commit:** `aae2e6c640a4fc3c1b9bc22c1f04fff0281c8ad8`
(`test(playwright): characterize UI-only demo guard`)

```bash
cd frontend
npm test -- --run eslint-local-rules/pw-demo-no-backend-access.test.js
```

Observed: 14 tests ran; 11 failed and 3 passed. Missing enforcement included
direct and aliased request contexts, computed request calls, browser fetch,
`expect.poll`, and page/context network stubs. Existing `waitForResponse`
enforcement and visible UI interaction examples passed.

**Green commit:** `784e4a37747befdb074678d6218664c0cd9d2859`
(`test(playwright): enforce UI-only demo specs`)

```bash
(cd frontend && npm test -- --run eslint-local-rules/pw-demo-no-backend-access.test.js)
```

Observed: 15 tests passed. The rule now catches direct/aliased/computed request
clients, browser fetch, response waits, `expect.poll`, and Playwright network
stubs. During refactor, an added allowed-case test first failed because any
method named `route` was rejected; network-stub detection was narrowed to
Playwright page/context owners, then all 15 tests passed again. The ASTM
simulator scenario moved to
`playwright/tests/foundational/harness/analyzer-astm-results.spec.ts` and
Playwright listed it under `harness-foundational`.

### Increment F0-I2: demo-facing dependency guard

**Acceptance:** `F0-B12`, `F0-B14`, partial `MVP-023` static enforcement.

**Current-code baseline:** ESLint applies a file-local rule to demo specs. A
demo can therefore import a helper containing request APIs or backend polling
and still pass lint. The remaining analyzer demo files imported helpers that
performed simulator calls, watched-folder delivery, response synchronization,
and backend polling.

**Red commit:** `875fe34f06bef2b1d68469f5fdd03cfdb0b2aeae`
(`test(playwright): expose demo helper backend access`)

```bash
(cd frontend && npm test -- --run scripts/pw-harness-demo-dependency-guard.test.js)
```

Observed: 4 tests ran; the direct imported-helper and transitive-import cases
failed, while the visible-helper/type-only-import case and foundational-bucket
case passed.

**Green commit:** `73425c750b1ae05fb7a7e860a3355b52e0407b86`
(`test(playwright): guard demo helper dependencies`)

```bash
(
  cd frontend
  npm test -- --run \
    scripts/pw-harness-demo-dependency-guard.test.js \
    eslint-local-rules/pw-demo-no-backend-access.test.js
  npm run lint
  npm run pw:guard
)
```

Observed: 20 focused tests passed; Playwright lint, bucket assignment, and the
dependency guard passed. Before reclassification, the new repository guard
reported 18 prohibited helper calls across `analyzer-demo-flow.spec.ts` and
`file-import-results.spec.ts`. The final implementation follows static,
dynamic, and re-exported local runtime dependencies while ignoring type-only
imports and runner infrastructure. It moved those files to
`analyzer-protocol-flows.spec.ts` and `analyzer-file-results.spec.ts` under
`harness-foundational`; together with the ASTM file, Playwright lists 13
retained analyzer integration cases. `harness-demo` intentionally contains no
feature story until OE-M4 adds `ogc-1054-analyzer-mvp.spec.ts`.

### Increment F0-I3: capability and import bypass closure

**Acceptance:** `F0-B12`, `F0-B14`, partial `MVP-023` static enforcement.

**Current-code baseline:** the first two increments still allowed demo code to
receive a request capability from another helper, destructure or alias network
methods, synchronize through response events, alias `fetch` or `expect.poll`,
and reach helpers through several valid JavaScript import forms. The dependency
guard also needed to prove that it linted the entry spec itself and did not
silently skip unresolvable local imports.

**Initial red commit:** `f5f32b9ac6e55679db5c71b5bb70f6b504f2aea0`
(`test(playwright): expose demo guard capability bypasses`)

Observed: 23 focused tests ran; 8 failed and 15 passed. The failures exposed
request-capability handoff, imported/destructured request use,
`waitForRequest`, response listeners, aliased routing, and owner
false-positive handling.

**Expanded red commit:** `77b023add0fac2bc1a65527d09c1119299d7ffde`
(`test(playwright): close demo guard import bypasses`)

Observed: 40 focused tests ran; 8 failed and 32 passed. The additional failures
covered aliased browser fetch and polling, `waitForTimeout`, forced controls,
template dynamic imports, CommonJS imports, entry-spec linting, helper
extensions, and runner-diagnostic differentiation.

**Green commit:** `35e06f987d5e26e5efc7b79bb08527d7458112ba`
(`test(playwright): enforce complete UI-only demo boundary`)

Observed: 45 focused tests passed; lint and the Playwright project guard passed.
The implementation fails closed for unresolved local runtime dependencies,
follows static/template dynamic/CommonJS imports and re-exports, and detects
capability aliases without banning unrelated application methods.

**Refactor commit:** `ae6b170c0f5aa7a61c0f1b43bed3a59d529bbe3f`
(`refactor(playwright): remove no-op guard presentation`)

The refactor removed 162 lines of no-op presentation logic while preserving all
45 focused tests and the 13-case foundational analyzer assignment.

### Increment F0-I4: missing local import fail-closed behavior

**Acceptance:** `F0-B12`, partial `MVP-023` static enforcement.

**Red commit:** `7b12470690b387b64ddfd30b37af7f6f0fa070fd`
(`test(playwright): expose unresolved demo imports`)

Observed: the new inversion case failed while the other 10 dependency-guard
cases passed. A static local runtime import whose target did not exist produced
no violation, contradicting the fail-closed evidence claim.

**Green commit:** `612b1aabb52666f5fbb9c421accbe926b6dd1ada`
(`test(playwright): fail closed on missing demo imports`)

Observed: all 46 focused direct/dependency tests, lint, and both Playwright
guards passed. Runtime-import records now preserve source locations and report
missing local modules before the dependency walk can silently omit them.

### Increment F0-I5: substantive parity selection and precise force checks

**Acceptance:** `F0-B12`, `F0-B14`, partial `MVP-023` static enforcement.

**Red commit:** `d039fae4c` (`test(playwright): expose parity and force guard
gaps`)

Observed: 41 focused cases ran; 6 failed. The parity runner rejected the
documented foundational project and selected an empty demo project by default,
while the lint rule rejected `{ force: true }` on an unrelated domain method.

**Green commit:** `877eea555` (`fix(playwright): run substantive parity checks`)

Observed: all 43 focused cases, both Playwright guards, formatting, and shell
syntax passed. Non-video parity now selects `harness-foundational` and proves
that analyzer specs exist before running. Video mode still selects
`harness-demo-video` but fails closed until M4 supplies a real user-story spec.
Forced-action enforcement is limited to known Playwright locator actions and
continues to catch direct and aliased locators without rejecting domain code.

## Cross-Repository Companion Disposition

F0 changes Playwright classification and enforcement only. It neither consumes
nor produces an Analyzer Bridge runtime contract and it adds no simulator
fixture. Therefore empty companion PRs would provide no testable behavior and
are prohibited by the roadmap. Exact pointers are recorded so a reviewer can
reproduce that decision.

| Repository      | Exact F0 pointer                                                    | OGC-1054 PR now                                   | F0 disposition                             | First required companion             |
| --------------- | ------------------------------------------------------------------- | ------------------------------------------------- | ------------------------------------------ | ------------------------------------ |
| OpenELIS        | OE-F0 branch after OE-R0 `53e720c05aa1f6abee16a173bd76a3a4d0135568` | OE-R0 #4049; OE-F0 #4053; closed historical #3792 | `CHANGE`                                   | Current repository                   |
| Analyzer Bridge | `12a338992eaf791a63159b7e5016f75369722dbf`                          | None                                              | `NO_CHANGE`: no F0 runtime contract change | BR-E0 before OE-E0                   |
| Analyzer mock   | `d063356e5a8f82ca6a44cf809be1874a7d704f8e`                          | None                                              | `NO_CHANGE`: no F0 fixture change          | MOCK-M4 after BR-M4 and before OE-M4 |

The global train is invalid if OE-E0 opens without its BR-E0 producer contract,
or if OE-M4 opens before both BR-M4 and MOCK-M4 are accepted and pinned. A
future repository may be marked `NO_CHANGE` only with contract-test evidence at
an exact SHA; absence of a PR by itself is never evidence.

## Layer Validation

| Layer                    | F0 status           | Rationale / evidence                                                                                                                         |
| ------------------------ | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Guard unit               | `RUN`               | 54 focused Vitest cases pass across direct AST, capability-alias, import-graph, project selection, and fail-closed behavior                 |
| Frontend lint            | `RUN`               | `npm run lint` passes with zero warnings                                                                                                     |
| Playwright project guard | `RUN`               | Bucket/dependency guards pass; parity selects the non-empty foundational project; `--list` assigns 18 analyzer checks there                  |
| TypeScript diagnostic    | `RUN_BASELINE_FAIL` | Repo-wide typecheck reports 1,589 existing errors; none name the three changed analyzer specs, which compile through Playwright `--list`     |
| RTL                      | `NOT_APPLICABLE`    | No React behavior changes in F0                                                                                                              |
| Backend unit/security    | `RUN`               | Docker-backed analyzer/QC package suite: 793 tests, 0 failures, 0 errors, 0 skipped                                                          |
| Persistence              | `NOT_APPLICABLE`    | F0 makes no persistence change                                                                                                               |
| Cross-repo contract      | `RUN_NO_CHANGE`     | F0 pointers and live PR search recorded above; first mandatory pair remains BR-E0/OE-E0                                                      |
| Harness execution        | `LATER`             | Reclassified integration cases remain M4 assembled-stack inputs; F0 validates assignment, not runtime transport                              |
| UI-only Playwright story | `LATER`             | OE-M4 owns creation of the complete visible story                                                                                            |
| Remote UAT / MP4         | `LATER`             | OE-G0 only                                                                                                                                   |

## Acceptance Crosswalk

| Criterion         | F0 evidence                                                                                                   | Remaining proof                                                                 |
| ----------------- | ------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| MVP-023           | Direct and dependency-aware red/green tests, lint, bucket guard, and 13 honestly classified integration cases | M4 audit of the final UI-only spec and G0 execution/trace/video                 |
| Legacy-path guard | All three traffic-injecting/polling analyzer specs are foundational; helper indirection no longer bypasses CI | M4 final story contains no request APIs, fetch, polling, force, waits, or stubs |
| F0-B02            | 3 Bridge registration, 12 control-lot service, and 2 non-numeric analyzer ID integration regressions pass     | E0 contract fixtures define future identifier/payload semantics                 |
| F0-B17            | 3 analyzer plugin-config security tests pass: unauthenticated 401, non-admin 403, global admin 200            | Later endpoint PRs add their own allowed/denied tests                           |

## Code-QA Disposition

The code-qa review used the repository-pinned `digi-uw/code-qa` skills for
meaningful coverage, simplicity, spec-code alignment, and cross-repository
companions. The first adversarial pass returned actionable findings; each is
resolved through code, tests, or a narrower claim rather than an approval-only
review.

| Gate                | Finding                                                                                                    | Disposition / evidence                                                                                                         |
| ------------------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Meaningful coverage | Request capability, alias, event, import-form, entry-spec, and runner-exemption bypasses remained after I2 | I3 red commits expose the bypasses; `35e06f987` makes 45 cases green without weakening the UI-only rule                        |
| Meaningful coverage | Missing static local imports could disappear from the dependency walk                                      | I4 red `7b1247069` proves the inversion; green `612b1aabb` reports the missing module with its source location                 |
| Meaningful coverage | Parity defaulted to an empty demo project and force detection overreached into domain calls                | I5 red `d039fae4c` proves both; green `877eea555` selects a non-empty foundational suite and recognizes locator actions        |
| Simplicity          | Foundational specs retained no-op demo presentation code and docs overstated the guard scope               | `ae6b170c0` removes 162 no-op lines; docs now name analyzer `harness-demo` specifically and use existing foundational examples |
| Spec/code alignment | F0 descended from an obsolete R0 and older claims described #3792-only behavior as current                 | F0 is rebased onto exact R0 `53e720c05`; current code, historical provenance, and MVP-010 round-trip behavior are explicit     |
| Companion review    | Bridge/mock PR status and the first mandatory pair were implicit                                           | Exact F0 `NO_CHANGE` pointers and the invalid-train conditions are recorded above; no empty companion PR is created            |

**Verdict:** F0 is aligned and right-sized for its static-boundary purpose. The
transitive scanner is retained as essential complexity because file-local lint
previously missed 18 prohibited helper calls. Product behavior, transport
contracts, remote execution, UAT, and MP4 evidence remain explicitly outside F0.

## Decisions

1. `openelis-work` is not used in this manifest to select code, persistence,
   repository ownership, or tests.
2. A test that sends analyzer traffic directly is useful integration coverage,
   but it is not demo evidence. It belongs in a foundational harness bucket.
3. F0 does not settle E0 payload or persistence contracts by copying #3792.
4. Static demo enforcement follows demo-facing runtime imports, including the
   entry spec itself; only shared runner diagnostics are exempt from user-story
   action checks.
5. F0 creates no empty Bridge or mock PR. The exact `NO_CHANGE` pointers are
   evidence only for F0, not waivers for later contract checkpoints.
6. Historical PR #3792 is closed as superseded and its branch remains immutable
   provenance.

## Issues and Ambiguities

| ID             | Status     | Evidence and deterministic action                                                                                                      |
| -------------- | ---------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| `ISSUE-F0-001` | `RESOLVED` | Three analyzer transport/setup specs were mislabeled as demo evidence; all are foundational now and 13 cases remain assigned.          |
| `ISSUE-F0-002` | `OPEN`     | Repo-wide typecheck has 1,589 baseline errors, none in changed analyzer specs. Targeted compilation passes; make the G0 command green. |
| `ISSUE-F0-003` | `RESOLVED` | Capability handoff, aliasing, import variants, unresolved imports, and entry-spec bypasses are covered by the 46-case guard suite.     |
| `AMB-F0-001`   | `RESOLVED` | The manifest classifies all #3792 behavior groups without treating any commit as an atomic salvage unit.                               |
| `AMB-E0-001`   | `OPEN`     | Exact Bridge payload and local binding persistence remain E0 decisions; F0 makes no contract change.                                   |

## Final Gate

Implementation, formatting, frontend production build, 46 focused guard tests,
the 1,057-pass frontend suite, Playwright classification, Spotless, and the
793-pass Docker-backed analyzer/QC suite are green. The repo-wide typecheck
baseline is recorded separately above. Code-qa findings and remediations are
recorded above. Pending gates are stacked PR CI/review-thread closure and the
required OE-R0 approval/merge. F0 cannot become `ACCEPTED` before OE-R0 is
approved and merged.
