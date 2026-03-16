# Playwright E2E Tests

> **Playwright is the recommended E2E framework** for OpenELIS Global 2. All new
> E2E tests should use Playwright. Cypress is deprecated and will be migrated.

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

Tests are organized into 4 projects via allowlist-based `testMatch` in
`playwright.config.ts`. New test files must be explicitly added to a project.

| Project      | Purpose                                           | CI Workflow          | Infra Required |
| ------------ | ------------------------------------------------- | -------------------- | -------------- |
| `core-app`   | Core UI tests (no plugins/bridge)                 | `playwright-e2e.yml` | Build stack    |
| `harness`    | Analyzer infra tests (bridge, simulator, plugins) | `analyzer-e2e.yml`   | Full harness   |
| `demo`       | Workflow demos at normal speed                    | `analyzer-e2e.yml`   | Full harness   |
| `demo-video` | Same demos with slowMo + video                    | Local only           | Harness        |

## CI Workflows

| Workflow             | Compose Files                                          | Projects           | Fixtures                                           |
| -------------------- | ------------------------------------------------------ | ------------------ | -------------------------------------------------- |
| `playwright-e2e.yml` | `build.docker-compose.yml`                             | `core-app`         | `file-import-e2e.sql`                              |
| `analyzer-e2e.yml`   | `build.docker-compose.yml` + `ci.analyzer-harness.yml` | `harness` + `demo` | `analyzer-harness-e2e.sql` + `file-import-e2e.sql` |

## Fixtures

SQL fixtures are loaded via `docker exec psql` in CI workflows:

- **`src/test/resources/analyzer-harness-e2e.sql`** — Analyzer type safety-net
  and cleanup-only fixture (does not preload analyzer rows)
- **`src/test/resources/fixtures/file-import-e2e.sql`** — Cleanup +
  deactivation fixture for a clean dashboard baseline

Analyzer rows used by harness tests are created via REST API seeding:

- **`projects/analyzer-harness/seed-analyzers.sh`** — Creates
  `Cepheid GeneXpert (ASTM Mode)`, `QuantStudio 5`, `QuantStudio 7`, and
  `FluoroCycler XT` using profile-based `defaultConfigId`

## Local Execution

### Prerequisites

1. App running at `https://localhost` (or set `BASE_URL`)
2. Auth env vars: `TEST_USER` and `TEST_PASS`

### Commands

```bash
cd frontend

# Run all projects
npm run pw:test

# Run specific project
npm run pw:test -- --project=core-app
npm run pw:test -- --project=harness
npm run pw:test -- --project=demo

# Run specific test file
npm run pw:test -- playwright/tests/file-import-ui.spec.ts

# Interactive UI mode
npm run pw:test:ui
```

### Examples

**Core-app tests** (build stack — `docker compose -f build.docker-compose.yml`):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=core-app
```

**Harness tests** (analyzer harness — see `/restart-analyzer-harness`):

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=harness
```

## Video Recording

The `demo-video` project runs the same `DEMO_TESTS` as `demo` but with
`slowMo: 500` and `video: "on"`. This produces watchable recordings for
stakeholder demos.

```bash
cd frontend
TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test -- --project=demo-video
# Videos saved to frontend/test-results/<test-name>/video.webm
```

Customize slowMo speed: `PLAYWRIGHT_SLOWMO=300 npm run pw:test -- --project=demo-video`

### `videoPause` Pattern

Video-pacing timeouts (pauses between actions for viewer readability) use the
`videoPause()` helper instead of raw `page.waitForTimeout()`:

```typescript
import { videoPause } from "../helpers/video-pause";

test("my demo test", async ({ page }, testInfo) => {
  await page.click("#submit");
  await videoPause(page, 1000, testInfo); // No-op except in demo-video
});
```

- `videoPause(page, ms, testInfo)` — pauses only in `demo-video` project
- `showTitleCard(page, title, subtitle, durationMs, testInfo)` — DOM overlay,
  skips in non-video projects
- `showStepCard(page, stepNumber, description, durationMs, testInfo)` — step
  banner overlay, skips in non-video projects

## Adding New Tests

1. Create `frontend/playwright/tests/{feature}.spec.ts`
2. Add the glob pattern to the appropriate project's `testMatch` in
   `playwright.config.ts`
3. For demo workflow tests, add to the `DEMO_TESTS` constant (shared between
   `demo` and `demo-video`)
4. Use `videoPause()` for any video pacing (not `page.waitForTimeout()`)
5. Validate project registration with:
   `python .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/{feature}.spec.ts`
6. For AI-assisted workflows, run:
   `/plan-record-playwright` -> `/write-playwright-test` -> `/audit-playwright`
   and use `/debug-playwright` on runtime failures

## Environment Variables

| Variable            | Default             | Description                                                 |
| ------------------- | ------------------- | ----------------------------------------------------------- |
| `BASE_URL`          | `https://localhost` | App URL                                                     |
| `TEST_USER`         | —                   | Login username (required)                                   |
| `TEST_PASS`         | —                   | Login password (required)                                   |
| `PLAYWRIGHT_SLOWMO` | `500`               | Milliseconds of slowMo for `demo-video`                     |
| `PLAYWRIGHT_VIDEO`  | `off`               | Global video override (prefer `demo-video` project instead) |
| `CI`                | —                   | Set by GitHub Actions; enables single worker + retries      |
