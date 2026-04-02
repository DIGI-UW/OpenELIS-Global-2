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

| Project                | Purpose                                         | CI Workflow                       | Infra Required          |
| ---------------------- | ----------------------------------------------- | --------------------------------- | ----------------------- |
| `core-app`             | Core foundational UI verification               | `e2e-playwright.yml`              | Build stack             |
| `core-demo`            | UI workflow demos on build stack + SQL fixtures | `e2e-playwright.yml`              | Build stack             |
| `core-demo-video`      | `core-demo` + slowMo + video                    | Local only                        | Build stack             |
| `harness-foundational` | Analyzer-stack foundational verification        | Local/manual targeted             | Full harness            |
| `harness-demo`         | Analyzer-stack story-proof demos (serial run)   | `e2e-playwright-analyzer-harness` | Full harness            |
| `harness-demo-video`   | `harness-demo` + slowMo + video (serial run)    | Manual harness workflow + local   | Full harness            |
| `harness-manual-only`  | Real-device / operator-managed hardware checks  | Manual only                       | Full harness + hardware |

## CI Workflows

| Workflow                                 | Compose Files                                          | Projects                 | Fixtures                                             |
| ---------------------------------------- | ------------------------------------------------------ | ------------------------ | ---------------------------------------------------- |
| `e2e-playwright.yml` (`playwright-core`) | `build.docker-compose.yml`                             | `core-app` + `core-demo` | `file-import-e2e.sql`                                |
| `e2e-playwright-analyzer-harness`        | `build.docker-compose.yml` + `ci.analyzer-harness.yml` | `harness-demo`           | `load-test-fixtures.sh --analyzers=full` (see below) |

`e2e-playwright-analyzer-harness-manual.yml` remains available for manual (`workflow_dispatch`) harness-only runs and delegates to the same reusable analyzer harness workflow used by `e2e-playwright.yml`.

### PR Evidence Artifacts (CI)

The reusable analyzer harness workflow publishes PR-review evidence directly from
the CI run:

- merged HTML report artifact: `analyzer-playwright-report-html-attempt-*`
- job summary with run/artifact links
- sticky PR comment (`Analyzer Playwright Evidence`) updated each run attempt

Normal PR validation runs publish HTML evidence for `harness-demo`.

Video-bearing demo evidence is opt-in and runs outside required checkpoint
graphs via manual workflows:

- zipped video evidence bundle: `analyzer-playwright-video-evidence-attempt-*`
  (contains `playwright-report` + `test-results` with videos)
- `playwright-core-demo-video-evidence-attempt-*` (core demo workflow)

This keeps required checkpoint graphs focused on authoritative suites while
preserving manual evidence collection when needed.

To generate CI-hosted video evidence for a PR, run the manual workflow
`E2E / Playwright / Analyzer Harness (Manual)` with:

- `playwright_project: harness-demo-video`
- optional `test_file` if you want a single demo spec only
- optional `pr_number` if you want the workflow to update the PR evidence comment

For core demo videos, run `E2E / Playwright / Demo Video Evidence (Manual)` and
enable `run_core_demo_video`.

## Fixtures

SQL fixtures are loaded via `docker exec psql` in CI workflows:

- **`src/test/resources/load-test-fixtures.sh --analyzers=full`** (analyzer
  harness job) — foundational data, `file-import-e2e.sql` cleanup, storage
  E2E fixtures, then **`src/test/resources/fixtures/analyzer-harness-lane-data.sql`**
  (isolated `HARN-*` accessions; see **`projects/analyzer-harness/LANE-IDENTIFIERS.md`**)
- **`src/test/resources/analyzer-harness-e2e.sql`** — Used by core Playwright
  workflow only (analyzer types + demo patient); not the full harness loader
- **`src/test/resources/fixtures/file-import-e2e.sql`** — Stale analyzer cleanup,
  **lane residue reset** for `HARN-*`, and dashboard type deactivation baseline

Analyzer rows used by harness tests are created via REST API seeding:

- **`projects/analyzer-harness/seed-analyzers.sh`** — Creates
  `Cepheid GeneXpert (ASTM Mode)`, `QuantStudio 5`, `QuantStudio 7`, and
  `FluoroCycler XT` using profile-based `defaultConfigId`

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

Allowed in demo stories:

- User-triggered UI actions
- Visible page transitions and durable DOM evidence
- Presentation helpers such as `videoPause()`, `showTitleCard()`, and `showStepCard()`
- Non-UI setup inputs only when unavoidable, such as simulator triggers or watched-folder drops

Banned in demo specs and demo-facing helpers:

- `page.on("console")` or `page.on("pageerror")`
- `captureDebugContext`
- `page.request.get()`, `page.request.put()`, `page.request.delete()`
- `waitForResponse()` used as proof
- Filesystem or server-state polling to decide success

`expect.poll()` is allowed only for DOM predicates (not backend/file polling).

If a behavior needs backend consistency checks, config persistence checks, or
bridge/file-watcher proof, move it to backend integration tests or CI health
checks rather than demo specs.

## Bucket Taxonomy

Playwright specs are classified on three axes:

- runtime: `core` or `harness`
- intent: `demo` (story proof, video-ready) or `foundational` (functional verification)
- execution policy: `ci` or `manual-only`

Canonical directories:

- `playwright/tests/demo/core/`
- `playwright/tests/demo/harness/`
- `playwright/tests/foundational/core/`
- `playwright/tests/foundational/harness/`
- `playwright/tests/manual-only/harness/`

Only `demo/**` specs participate in auto-video CI evidence policy. `manual-only/**`
specs never run in ordinary PR CI.

### File import wait tuning (`file-import-results.spec.ts`)

CI sets **`FILE_IMPORT_POLL_MS=5000`** and **`FILE_IMPORT_DROP_BUFFER_MS=45000`** on
Playwright jobs (see
[`e2e-playwright-analyzer-harness-reusable.yml`](../../.github/workflows/e2e-playwright-analyzer-harness-reusable.yml))
to match the harness webapp (`-Dfile.import.poll.interval=5000` in
[`.github/ci/ci.analyzer-harness.yml`](../../.github/ci/ci.analyzer-harness.yml)).
Locally, defaults assume the server's **`file.import.poll.interval=60000`** in
`application.properties` unless you override JVM properties or the same env vars
when running tests.

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
npm run pw:test -- --project=harness-foundational
npm run pw:test -- --project=harness-manual-only

# Convenience aliases
npm run pw:test:core-demo
npm run pw:test:harness-demo
npm run pw:test:core-foundational
npm run pw:test:harness-foundational
npm run pw:test:harness-manual-only
npm run pw:test:demo # alias → harness-demo (analyzer story tests)

# Run specific test file
npm run pw:test -- playwright/tests/demo/harness/file-import-ui.spec.ts

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

**Harness demos** (file import / ASTM stories — full harness):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo
```

**Harness foundational checks** (non-demo harness verification):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-foundational
```

**Harness manual-only checks** (real hardware / operator-managed):

```bash
cd frontend
GENEXPERT_HOST='<ip-or-dns>' GENEXPERT_PORT='1200' TEST_USER=admin TEST_PASS='adminADMIN!' \
  npm run pw:test:harness-manual-only
```

### Analyzer Harness Remediation Loop

When remediating `harness-demo` failures, do not use CI as the
first repro. Follow this local loop after every substantive spec/helper change:

1. Run the authoritative local CI parity path from the repo root:

```bash
./projects/analyzer-harness/ci-parity-test.sh --preflight-only
./projects/analyzer-harness/ci-parity-test.sh
```

2. If you are fixing a specific failing spec, run that file first:

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=harness-demo playwright/tests/<failing-spec>.spec.ts
```

3. Run full analyzer parity locally before pushing:

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo
```

4. During remediation, keep local validation running before or alongside every
   push so CI confirms parity instead of discovering failures first. Push only
   after the targeted local run and at least one full local `harness-demo` pass
   completes.

## Video Recording

`*-demo-video` projects mirror `core-demo` / `harness-demo` with `slowMo: 500` and
`video: "on"` for stakeholder recordings.

```bash
cd frontend
# Core stack (e.g. OGC-284 barcode stories)
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo-video
# Full harness (QuantStudio / file import / GeneXpert demos) via parity bootstrap
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo-video
# Analyzer demo flow only (7 Madagascar-scoped flows) via parity bootstrap
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:analyzer-demo-flow-video
# Videos saved to frontend/test-results/<test-name>/video.webm
```

Both commands above execute `../projects/analyzer-harness/ci-parity-test.sh --mode video`
under the hood, so video recordings use the same fixture/seed/readiness gates as CI parity.

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
   - `HARNESS_FOUNDATIONAL_TESTS`
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
