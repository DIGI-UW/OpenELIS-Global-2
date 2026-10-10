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

| Project                | Purpose                                         | CI                  | Infra Required          |
| ---------------------- | ----------------------------------------------- | ------------------- | ----------------------- |
| `core-app`             | Core foundational UI verification               | Every PR (2 shards) | Build stack             |
| `core-demo`            | UI workflow demos on build stack + SQL fixtures | Every PR (2 shards) | Build stack             |
| `core-demo-video`      | `core-demo` + slowMo + video                    | Local only          | Build stack             |
| `harness-foundational` | Analyzer catalog and mapping UI verification    | Every PR (2 shards) | Full harness + traffic  |
| `harness-demo`         | Guided setup and assembled result UI stories    | Every PR (2 shards) | Full harness + traffic  |
| `harness-demo-video`   | Final assembled analyzer story + video          | Acceptance evidence | Full harness + traffic  |
| `harness-manual-only`  | Real-device / operator-managed hardware checks  | Local only          | Full harness + hardware |

## CI Workflows

All Playwright tests run through a single parameterized reusable workflow
(`e2e-playwright-reusable.yml`), called twice by the orchestrator
(`e2e-authoritative-reusable.yml`):

| Call               | Compose Files                                 | Projects                                | Fixtures                                  |
| ------------------ | --------------------------------------------- | --------------------------------------- | ----------------------------------------- |
| Playwright Core    | `build.docker-compose.yml`                    | `core-app` + `core-demo`                | `load-test-fixtures.sh --profile=core`    |
| Playwright Harness | `build.docker-compose.yml` + harness overlays | `harness-foundational` + `harness-demo` | `load-test-fixtures.sh --profile=harness` |

Both follow the same pattern: **test-shards → merge-reports → gate**. Each
produces a merged HTML report artifact:

- `core-playwright-report-html-attempt-*`
- `harness-playwright-report-html-attempt-*`

### Execution Policy

| Policy         | Where         | Video        | Projects                                                |
| -------------- | ------------- | ------------ | ------------------------------------------------------- |
| **CI**         | Every PR      | Off          | core-app, core-demo, harness-foundational, harness-demo |
| **Acceptance** | Local or demo | Off, then on | focused harness-demo, then harness-demo-video           |

No `workflow_dispatch` manual workflows exist for Playwright. Video recording
is local-only via the `-video` project variants.

Analyzer-ingress scenarios post as a machine client. Set
`ANALYZER_INGRESS_USER` / `ANALYZER_INGRESS_PASS` to use a dedicated
analyzer-import account; otherwise they fall back to `TEST_USER` / `TEST_PASS`,
and then to the `admin` fixture account, which carries the role.

## Fixtures

CI workflows load fixtures via the unified loader script:

- **`src/test/resources/load-test-fixtures.sh --profile=harness`** (analyzer
  harness job) — foundational data and storage E2E fixtures. Analyzer
  scenarios create clinical orders through the validated OpenELIS API (see
  **`projects/analyzer-harness/LANE-IDENTIFIERS.md`** for captured file IDs).
- **`src/test/resources/fixtures/core-demo-patient.sql`** — Core demo patient fixture loaded by `--profile=core`

Analyzer rows used by harness tests are created via REST API seeding:

- **`projects/analyzer-harness/seed-analyzers.sh`** — Creates
  `Cepheid GeneXpert (ASTM Mode)`, `QuantStudio 5`, `QuantStudio 7`, and
  `FluoroCycler XT` through the ordinary analyzer API. CI uses
  `--ensure-connections`, preserving mapping and activation for the workflow
  tests to verify in the UI. Native traffic belongs to those tests.

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

`core-demo` and `harness-demo` are UI-only stories. The analyzer result
stories use API-created clinical prerequisites and native mock traffic, so they
run in `harness-foundational`; they still perform setup review and result
acceptance through the visible UI. `harness-demo-video` records the same
registered UI setup and integrated result stories with their original assertions.

The ordinary CI harness run covers the M1-M2 catalog, mapping and M4 integrated
result stories through `harness-foundational`, then M3 guided setup through
`harness-demo`. The setup-to-result stories can run alone through `pw:test:harness-results`.
Record those same tests with `harness-demo-video` after checking the screenshots,
trace, console output and runtime state.

Allowed in demo stories:

- User-triggered UI actions
- Visible page transitions and durable DOM evidence
- Presentation helpers such as `videoPause()`, `showTitleCard()`, and `showStepCard()`
- Deterministic fixture loading by the runner before the UI story begins

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

When a story requires clinical orders created through OE2 APIs or external
instrument traffic, place the integrated scenario in `harness-foundational`.
Keep the ordinary UI steps and independent clinical readback in that scenario;
never use its prerequisite helper to choose or repair analyzer mappings.

## Bucket Taxonomy

Playwright specs are classified on three axes:

- runtime: `core` or `harness`
- intent: `demo` (story proof, video-ready) or `foundational` (core functional verification)
- execution policy: `ci` or `manual-only`

Canonical directories:

- `playwright/tests/demo/core/`
- `playwright/tests/demo/harness/`
- `playwright/tests/foundational/core/`
- `playwright/tests/performance/core/`
- `playwright/tests/manual-only/harness/`

Only `demo/**` specs participate in auto-video CI evidence policy. `manual-only/**`
specs never run in ordinary PR CI.

`performance/**` specs are explicit qualification runs. They require
`MICROBIOLOGY_QUALIFICATION_DISPOSABLE=true`, an exact `OGC782_COMMIT`, and a
throwaway stack/database that is destroyed after evidence collection. They must
not run against shared review or clinical data.

## Local Execution

### Prerequisites

1. **Dependencies:** from `frontend/`, run **`npm run ci:deps`** (then **`npm run pw:install`**). Plain **`npm ci`** often prints almost nothing for several minutes while Cypress unpacks — it is not stuck; **`ci:deps`** forces progress + `loglevel=info` so you see steady output. `.npmrc` also sets `progress=true` for normal installs.
2. Start the isolated stack from the repository root with
   **`scripts/dev-stack up`**.

Authentication uses the shared setup project and the repository `.env` values;
the standard development credentials need no manual export.

### Commands

```bash
# Preferred developer entry point, from the repository root. It discovers the
# current worktree's URL and runs authentication automatically.
scripts/dev-stack playwright

# Run a specific test (core-app is the default project)
scripts/dev-stack playwright playwright/tests/foundational/core/microbiology-whonet-export.spec.ts

# Verify only the shared login/session contract
scripts/dev-stack playwright --project=setup

# Select another registered project
scripts/dev-stack playwright --project=harness-foundational

# Run the same test against a deployed target
BASE_URL=https://amr.openelis-global.org \
  scripts/dev-stack playwright playwright/tests/foundational/core/microbiology-whonet-export.spec.ts
```

The lower-level package commands below remain the CI interface and are useful
when debugging Playwright itself:

```bash
# From the repository root:
scripts/dev-stack up
eval "$(scripts/dev-stack env)"
cd frontend
npm run pw:test -- --project=core-app <spec-path>
# Use --project=setup to run authentication setup alone.
```

### Examples

**Core-app tests** (first export `scripts/dev-stack env` from the repository root):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=core-app
```

**Core demos** (against the configured development stack):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo
```

**Harness checkpoint stories** (M1-M4 — full harness):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo
```

**Final assembled analyzer story** (real ASTM and FILE traffic through Bridge,
then visible UI only):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-results
```

**Harness manual-only checks** (real hardware / operator-managed):

```bash
cd frontend
GENEXPERT_HOST='<ip-or-dns>' GENEXPERT_PORT='1200' TEST_USER=admin TEST_PASS='adminADMIN!' \
  npm run pw:test:harness-manual-only
```

### Analyzer Harness Remediation Loop

Use native Playwright for focused development tests against the development
stack, with `scripts/dev-stack env` exported. To reproduce an isolated CI lane,
use `scripts/run-ci-checks.sh --job playwright-analyzers-1` or the other job
listed by `--list-jobs`. A selected-job pass is partial validation. After a
push, run the full command alongside GitHub.

### Stakeholder Evidence Format

Feature walkthroughs use one editorial format in addition to the shared
recording mechanics:

1. Record at 16:9 through a registered `*-demo-video` project; target 45-90
   seconds for one milestone.
2. Open with a 3.5-4.5 second full-screen card: ticket/milestone eyebrow,
   literal feature title, and one-line outcome. Use `demo.chapter()` so the
   Carbon-dark card, left accent, type hierarchy, and spacing stay consistent.
3. Introduce each user story with a chapter card. Use compact scene labels for
   sustained interaction; reserve numbered step banners for genuinely ordered
   procedures rather than every click.
4. End with an outcome card that distinguishes automated evidence from human
   UAT. Do not imply acceptance when Review-overlay rulings are pending.
5. Capture 5-8 stable screenshots at acceptance checkpoints and inspect both a
   screenshot contact sheet and representative video frames for clipping,
   stale loading state, scroll position, and readable timing.
6. Package WebM as H.264/yuv420p/faststart MP4 and record the app SHA,
   deployment ID, checklist revision, and artifact checksums in the evidence
   manifest or README. The `tools/code-qa/skills/evidence-bundle` skill does
   this packaging and drafts the PR comment; it never commits the media.
7. Compare key screenshots with the authoritative product mock/spec. Record
   intentional OpenELIS-shell or Carbon differences; do not treat prototype
   routes, components, or navigation as implementation contracts.

Presentation pauses are allowed only through the video-gated helpers below.
Functional readiness and assertions must continue to use observable state.

```bash
cd frontend
# Core stack (e.g. OGC-284 barcode stories)
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:core-demo-video
# Full harness demo story via parity bootstrap
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:harness-demo-video
# Videos saved to frontend/test-results/<test-name>/video.webm
```

The harness video command runs the existing `harness-demo-video` Playwright
project against the configured development stack. It preserves the project's
video pacing and presentation. Export `scripts/dev-stack env` before entering
`frontend`; recording does not create a separate stack or load CI fixtures.

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
  single UI-only scenario can run in both its normal and `*-demo-video` modes;
  prefer its structured `chapter()` method for opening, story, and completion
  cards

## Adding New Tests

1. Create the spec under the correct taxonomy bucket directory.
2. Add its glob to exactly one bucket list in `playwright.config.ts`:
   - `CORE_DEMO_TESTS`
   - `CORE_FOUNDATIONAL_TESTS`
   - `HARNESS_DEMO_TESTS`
   - `HARNESS_DEMO_TESTS` for guided setup and the assembled analyzer story
   - `HARNESS_MANUAL_ONLY_TESTS`
3. Run bucket and demo guards: `npm run pw:guard`
4. Use `videoPause()` for any video pacing in demo specs (not `page.waitForTimeout()`)
5. Validate project registration with:
   `python .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/{feature}.spec.ts`
6. For AI-assisted workflows, run:
   `/plan-record-playwright` -> `/write-playwright-test` -> `/audit-playwright`
   and use `/debug-playwright` on runtime failures

## Environment Variables

| Variable                | Default              | Description                                                                  |
| ----------------------- | -------------------- | ---------------------------------------------------------------------------- |
| `BASE_URL`              | `https://localhost`  | App URL                                                                      |
| `TEST_USER`             | —                    | Login username (required)                                                    |
| `TEST_PASS`             | —                    | Login password (required)                                                    |
| `ANALYZER_INGRESS_USER` | `TEST_USER`, `admin` | Dedicated account with the Analyser Import role for analyzer-event scenarios |
| `ANALYZER_INGRESS_PASS` | `TEST_PASS`, fixture | Password for the dedicated analyzer-ingress account                          |
| `PLAYWRIGHT_SLOWMO`     | `500`                | Milliseconds of slowMo for `*-demo-video` projects                           |
| `PLAYWRIGHT_VIDEO`      | `off`                | Global video override (prefer `*-demo-video` projects)                       |
| `CI`                    | —                    | Set by GitHub Actions; enables CI mode settings in Playwright config         |
