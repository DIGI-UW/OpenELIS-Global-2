# Playwright E2E Tests

> **Playwright is the recommended E2E framework** for OpenELIS Global 2. All new
> E2E tests should use Playwright. Cypress is deprecated and will be migrated.

> **Canonical best-practices guide:**  
> `.specify/guides/playwright-best-practices.md` (single source of truth).  
> This README focuses on repo-specific operational details (projects, CI mapping,
> fixtures, and local execution).

**Config:** `frontend/playwright.config.ts`
**Tests:** `frontend/playwright/tests/`
**Helpers:** `frontend/playwright/helpers/`

## AI Command Workflow

For AI-assisted Playwright work, start with:

- `/plan-record-playwright` to review feature/PR scope, identify flows, and map project/recording stages
- `/write-playwright-test` for source-first, first-time-correct test authoring
- `/debug-playwright` for evidence-first failure diagnosis (source + screenshot/trace)
- `/audit-playwright` for selector quality and anti-pattern audits

Packaged source for these commands lives in `.ai/skills/playwright/`.

## Projects

Tests are organized into projects via allowlist-based `testMatch` in
`playwright.config.ts`. New test files must be explicitly added to a project.

| Project               | Purpose                                         | CI                  | Infra Required          |
| --------------------- | ----------------------------------------------- | ------------------- | ----------------------- |
| `core-app`            | Core foundational UI verification               | Every PR (2 shards) | Build stack             |
| `core-demo`           | UI workflow demos on build stack + SQL fixtures | Every PR (2 shards) | Build stack             |
| `core-demo-video`     | `core-demo` + slowMo + video                    | Local only          | Build stack             |
| `harness-demo`        | Analyzer checkpoint UI stories (serial run)     | Every PR (2 shards) | Full harness            |
| `harness-mvp`         | Final assembled analyzer story, without video   | Acceptance run      | Full harness + traffic  |
| `harness-demo-video`  | Final assembled analyzer story + video          | Acceptance evidence | Full harness + traffic  |
| `harness-manual-only` | Real-device / operator-managed hardware checks  | Local only          | Full harness + hardware |

## CI Workflows

All Playwright tests run through a single parameterized reusable workflow
(`e2e-playwright-reusable.yml`), called twice by the orchestrator
(`e2e-authoritative-reusable.yml`):

| Call               | Compose Files                                 | Projects                 | Fixtures                                  |
| ------------------ | --------------------------------------------- | ------------------------ | ----------------------------------------- |
| Playwright Core    | `build.docker-compose.yml`                    | `core-app` + `core-demo` | `load-test-fixtures.sh --profile=core`    |
| Playwright Harness | `build.docker-compose.yml` + harness overlays | `harness-demo`           | `load-test-fixtures.sh --profile=harness` |

Both follow the same pattern: **test-shards → merge-reports → gate**. Each
produces a merged HTML report artifact:

- `core-playwright-report-html-attempt-*`
- `harness-playwright-report-html-attempt-*`

### Execution Policy

| Policy         | Where         | Video        | Projects                             |
| -------------- | ------------- | ------------ | ------------------------------------ |
| **CI**         | Every PR      | Off          | core-app, core-demo, harness-demo    |
| **Acceptance** | Local or demo | Off, then on | harness-mvp, then harness-demo-video |

No `workflow_dispatch` manual workflows exist for Playwright. Video recording
is local-only via the `-video` project variants.

## Fixtures

CI workflows load fixtures via the unified loader script:

- **`src/test/resources/load-test-fixtures.sh --profile=harness`** (analyzer
  harness job) — foundational data, storage E2E fixtures, then
  **`src/test/resources/fixtures/analyzer-harness-lane-data.sql`**
  (isolated `HARN-*` accessions; see **`projects/analyzer-harness/LANE-IDENTIFIERS.md`**)
- **`src/test/resources/fixtures/core-demo-patient.sql`** — Core demo patient fixture loaded by `--profile=core`

Analyzer rows used by harness tests are created via REST API seeding:

- **`projects/analyzer-harness/seed-analyzers.sh`** — Creates
  `Cepheid GeneXpert (ASTM Mode)`, `QuantStudio 5`, `QuantStudio 7`, and
  `FluoroCycler XT` using profile-based `defaultConfigId`
- **`projects/analyzer-harness/seed-mvp-traffic.sh`** — Prepares the final
  assembled story and sends real mock ASTM and FILE traffic through Bridge
  before the browser opens. The Playwright story does not create or mutate its
  own fixtures.

### Harness environment contract

- **Database container**: `openelisglobal-database` (service `db.openelis.org` in
  `build.docker-compose.yml` / `projects/analyzer-harness/docker-compose.base.yml`).
  Playwright helpers honor `HARNESS_DB_CONTAINER`, `DATABASE_CONTAINER`, or
  `DB_CONTAINER` (first match).
- **Host import directory**: `projects/analyzer-harness/volume/analyzer-imports`
  (bind-mounted for bridge file drops). Override with `HARNESS_ANALYZER_IMPORTS_DIR`
  if the workspace layout is non-standard.
- **CI readiness**: `scripts/e2e/wait-for-openelis-login.sh` (core E2E) and
  `scripts/e2e/wait-for-analyzer-harness-readiness.sh` (full harness) — prefer
  these over curling `/` so tests start only after `ValidateLogin` succeeds.

## Demo Contract

`core-demo`, `core-demo-video`, `harness-demo`, and `harness-demo-video` exist
to prove user stories through visible UI evidence. They are not the place for
backend or infrastructure assertions.

The ordinary harness demo project runs the bounded M1-M3 checkpoint stories.
The final M4 story runs separately as `harness-mvp`, then unchanged as
`harness-demo-video` after its screenshots, trace, console output, and runtime
state have been reviewed.

Allowed in demo stories:

- User-triggered UI actions
- Visible page transitions and durable DOM evidence
- Presentation helpers such as `videoPause()`, `showTitleCard()`, and `showStepCard()`
- Deterministic fixture loading before the user story begins

Banned in demo specs and demo-facing helpers:

- `page.on("console")` or `page.on("pageerror")`
- `captureDebugContext`
- Playwright request APIs or browser `fetch()`
- `waitForResponse()` used as proof
- `expect.poll()`; use Playwright's web-first visible UI assertions
- Network interception or stubbing
- Filesystem or server-state polling to decide success

The guard follows runtime local imports from harness demo specs, so moving a
prohibited operation into a helper does not make the story UI-only. Runner-level
diagnostics remain separate from demo-facing behavior helpers.

If a behavior needs backend consistency checks, config persistence checks, or
bridge/file-watcher proof, move it to backend integration tests or CI health
checks rather than demo specs.

## Bucket Taxonomy

Playwright specs are classified on three axes:

- runtime: `core` or `harness`
- intent: `demo` (story proof, video-ready) or `foundational` (core functional verification)
- execution policy: `ci` or `manual-only`

Canonical directories:

- `playwright/tests/demo/core/`
- `playwright/tests/demo/harness/`
- `playwright/tests/foundational/core/`
- `playwright/tests/manual-only/harness/`

Only `demo/**` specs participate in auto-video CI evidence policy. `manual-only/**`
specs never run in ordinary PR CI.

## Local Execution

### Prerequisites

1. **Dependencies:** from `frontend/`, run **`npm run ci:deps`** (then **`npm run pw:install`**). Plain **`npm ci`** often prints almost nothing for several minutes while Cypress unpacks — it is not stuck; **`ci:deps`** forces progress + `loglevel=info` so you see steady output. `.npmrc` also sets `progress=true` for normal installs.
2. App running at `https://localhost` (or set `BASE_URL`)
3. Auth env vars: `TEST_USER` and `TEST_PASS`

### Commands

```bash
cd frontend

# Run all projects
npm run pw:test

# Run specific project
npm run pw:test -- --project=core-app
npm run pw:test -- --project=core-demo
npm run pw:test -- --project=harness-demo
npm run pw:test:harness-mvp
npm run pw:test -- --project=harness-manual-only

# Convenience aliases
npm run pw:test:core-demo
npm run pw:test:harness-demo
npm run pw:test:core-foundational
npm run pw:test:harness-mvp
npm run pw:test:harness-manual-only
npm run pw:test:demo # alias → harness-demo (analyzer story tests)

# Run a specific checkpoint story
npm run pw:test -- --project=harness-demo playwright/tests/demo/harness/ogc-1054-m2-shared-mapping.spec.ts

# Interactive UI mode
npm run pw:test:ui
```

### Examples

**Core-app tests** (build stack — `docker compose -f build.docker-compose.yml`):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=core-app
```

**Core demos** (barcode workflow — build stack only):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo
```

**Harness checkpoint stories** (M1-M3 — full harness):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo
```

**Final assembled analyzer story** (real ASTM and FILE traffic through Bridge,
then visible UI only):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-mvp
```

**Harness manual-only checks** (real hardware / operator-managed):

```bash
cd frontend
GENEXPERT_HOST='<ip-or-dns>' GENEXPERT_PORT='1200' TEST_USER=admin TEST_PASS='adminADMIN!' \
  npm run pw:test:harness-manual-only
```

### Analyzer Harness Remediation Loop

When remediating an analyzer story, reproduce it locally before using CI as the
diagnostic loop.

1. Run the authoritative local CI parity path from the repo root:

```bash
./projects/analyzer-harness/ci-parity-test.sh --preflight-only
./projects/analyzer-harness/ci-parity-test.sh --project harness-demo
```

2. If you are fixing a specific failing spec, run that file first:

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=harness-demo playwright/tests/<failing-spec>.spec.ts
```

3. For M4 acceptance, run the assembled non-video story and inspect its output
   and screenshots before recording:

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-mvp
```

4. Run `harness-demo-video` only after the unchanged non-video story is green
   and its screenshots, console output, trace, and runtime state are acceptable.

## Video Recording

`core-demo-video` mirrors `core-demo`. `harness-demo-video` runs the same final
assembled story as `harness-mvp`, with `slowMo: 500` and video enabled.

```bash
cd frontend
# Core stack (e.g. OGC-284 barcode stories)
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo-video
# Full harness demo story via parity bootstrap
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo-video
# Videos saved to frontend/test-results/<test-name>/video.webm
```

The harness video command executes
`../projects/analyzer-harness/ci-parity-test.sh --mode video`, so the recording
uses the same fixture, real mock traffic, and readiness gates as the non-video
acceptance run.

Customize slowMo: `PLAYWRIGHT_SLOWMO=300 npm run pw:test:harness-demo-video`

Build a distributable report bundle from the latest run:

```bash
cd frontend
npm run pw:bundle-report
```

`pw:bundle-report` merges `blob-report` into `playwright-report` when needed, then zips
`playwright-report` + `test-results` into a timestamped
`analyzer-harness-demo-video-playwright-report-*.zip`.
Use `PW_BUNDLE_REPORT_PREFIX=<custom-prefix>` to override the filename prefix.

### `videoPause` Pattern

Video-pacing timeouts (pauses between actions for viewer readability) use the
`videoPause()` helper instead of raw `page.waitForTimeout()`:

```typescript
import { videoPause } from "../helpers/video-pause";

test("my demo test", async ({ page }, testInfo) => {
  await page.click("#submit");
  await videoPause(page, 1000, testInfo); // No-op except in *-demo-video
});
```

- `videoPause(page, ms, testInfo)` — pauses only in `core-demo-video` /
  `harness-demo-video`
- `showTitleCard(page, title, subtitle, durationMs, testInfo)` — DOM overlay,
  skips in non-video projects
- `showStepCard(page, stepNumber, description, durationMs, testInfo)` — step
  banner overlay, skips in non-video projects
- `createDemoPresentation(page, testInfo)` — shared presentation wrapper so a
  single UI-only scenario can run in both its normal and `*-demo-video` modes

## Adding New Tests

1. Create the spec under the correct taxonomy bucket directory.
2. Add its glob to exactly one bucket list in `playwright.config.ts`:
   - `CORE_DEMO_TESTS`
   - `CORE_FOUNDATIONAL_TESTS`
   - `HARNESS_DEMO_TESTS`
   - `HARNESS_MVP_TESTS` for the single assembled analyzer acceptance story
   - `HARNESS_MANUAL_ONLY_TESTS`
3. Run bucket and demo guards: `npm run pw:guard`
4. Use `videoPause()` for any video pacing in demo specs (not `page.waitForTimeout()`)
5. Validate project registration with:
   `python .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/{feature}.spec.ts`
6. For AI-assisted workflows, run:
   `/plan-record-playwright` -> `/write-playwright-test` -> `/audit-playwright`
   and use `/debug-playwright` on runtime failures

## Environment Variables

| Variable            | Default             | Description                                                          |
| ------------------- | ------------------- | -------------------------------------------------------------------- |
| `BASE_URL`          | `https://localhost` | App URL                                                              |
| `TEST_USER`         | —                   | Login username (required)                                            |
| `TEST_PASS`         | —                   | Login password (required)                                            |
| `PLAYWRIGHT_SLOWMO` | `500`               | Milliseconds of slowMo for `*-demo-video` projects                   |
| `PLAYWRIGHT_VIDEO`  | `off`               | Global video override (prefer `*-demo-video` projects)               |
| `CI`                | —                   | Set by GitHub Actions; enables CI mode settings in Playwright config |
