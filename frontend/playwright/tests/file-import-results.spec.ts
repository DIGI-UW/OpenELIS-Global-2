import { expect, Page, test } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import { acceptAndVerifyResults } from "../helpers/accept-results";
import { createDemoPresentation } from "../helpers/demo-presentation";
import type { DemoPresentation } from "../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../helpers/analyzer-dashboard";
import { openAnalyzerResultsAndWaitForText } from "../helpers/results-ui";
import { UI_TIMEOUT } from "../helpers/timeouts";

const REPO_ROOT = path.resolve(__dirname, "../../..");
const FIXTURES_DIR = path.join(__dirname, "../fixtures");
const HOST_IMPORTS_BASE = path.join(
  REPO_ROOT,
  "projects/analyzer-harness/volume/analyzer-imports",
);

const DEFAULT_FILE_IMPORT_POLL_MS = 60_000;
const DEFAULT_FILE_IMPORT_DROP_BUFFER_MS = 45_000;

const QUANTSTUDIO = {
  name: "QuantStudio 7",
  safeName: "quantstudio-7",
  fixture: "quantstudio-e2e-results.xlsx",
  filePrefix: "qs7-results-",
  expectedResults: [
    { sampleId: "E2E001", result: "1520.5" },
    { sampleId: "E2E002", result: "45200" },
    { sampleId: "E2E005", result: "3200.8" },
  ],
};

function parsePositiveIntEnv(name: string, fallback: number): number {
  const parsed = parseInt(process.env[name] || "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function fileImportTimeoutMs(): number {
  const pollMs = parsePositiveIntEnv(
    "FILE_IMPORT_POLL_MS",
    DEFAULT_FILE_IMPORT_POLL_MS,
  );
  const bufferMs = parsePositiveIntEnv(
    "FILE_IMPORT_DROP_BUFFER_MS",
    DEFAULT_FILE_IMPORT_DROP_BUFFER_MS,
  );
  return 2 * pollMs + bufferMs;
}

function chmodSharedImportPathChain(dir: string) {
  const base = path.resolve(HOST_IMPORTS_BASE);
  let currentDir = path.resolve(dir);

  while (currentDir.startsWith(base) && currentDir !== base) {
    try {
      if (fs.existsSync(currentDir)) {
        fs.chmodSync(currentDir, 0o777);
      }
    } catch {
      // Best-effort local permission fix for harness bind mounts.
    }
    currentDir = path.dirname(currentDir);
  }
}

async function dropFixtureFile(
  hostImportDir: string,
  presentation: DemoPresentation,
) {
  const fixtureFile = path.join(FIXTURES_DIR, QUANTSTUDIO.fixture);
  const fileExtension = path.extname(QUANTSTUDIO.fixture);

  fs.mkdirSync(hostImportDir, { recursive: true });
  chmodSharedImportPathChain(hostImportDir);

  const droppedFilePath = path.join(
    hostImportDir,
    `${QUANTSTUDIO.filePrefix}${Date.now()}${fileExtension}`,
  );

  fs.copyFileSync(fixtureFile, droppedFilePath);
  expect(fs.existsSync(droppedFilePath)).toBeTruthy();
  await presentation.pause(1_000);

  return droppedFilePath;
}

async function verifyImportedResults(
  page: Page,
  presentation: DemoPresentation,
) {
  await openAnalyzerResultsAndWaitForText(
    page,
    QUANTSTUDIO.name,
    QUANTSTUDIO.expectedResults[0].sampleId,
    {
      timeoutMs: fileImportTimeoutMs(),
      perAttemptTimeoutMs: 5_000,
    },
  );

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  for (const expected of QUANTSTUDIO.expectedResults) {
    await expect(
      resultsRegion.getByText(expected.sampleId, { exact: false }).first(),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      resultsRegion.getByText(expected.result, { exact: false }).first(),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  }

  await presentation.pause(2_000);
}

test.describe("QuantStudio 7 file import demo story", () => {
  test.setTimeout(180_000);

  let droppedFilePath: string | undefined;

  test("import and accept results from a watched folder", async ({
    page,
  }, testInfo) => {
    test.skip(
      !fs.existsSync(HOST_IMPORTS_BASE),
      "Requires analyzer harness bind-mount (analyzer-imports not found)",
    );

    const presentation = createDemoPresentation(page, testInfo);
    const hostImportDir = path.join(
      HOST_IMPORTS_BASE,
      QUANTSTUDIO.safeName,
      "incoming",
    );

    await presentation.title(
      "QuantStudio 7 File Import",
      "Drop a result file, review staged results, and accept them.",
    );

    await goToAnalyzerDashboard(page, testInfo);

    await presentation.step(1, "Find the pre-configured QuantStudio analyzer");
    await findAnalyzerRow(page, QUANTSTUDIO.name, testInfo);

    await presentation.step(2, "Drop a result file into the watched folder");
    droppedFilePath = await dropFixtureFile(hostImportDir, presentation);

    await presentation.step(3, "Review the imported results");
    await verifyImportedResults(page, presentation);

    await acceptAndVerifyResults(page, presentation, 3);

    await presentation.title(
      "Story Complete",
      "The QuantStudio import flow now relies on visible UI evidence only.",
    );
  });

  test.afterEach(async () => {
    if (!droppedFilePath) {
      return;
    }

    try {
      if (fs.existsSync(droppedFilePath)) {
        fs.unlinkSync(droppedFilePath);
      }
    } catch {
      // Best-effort cleanup so repeated local runs start cleaner.
    }

    droppedFilePath = undefined;
  });
});
