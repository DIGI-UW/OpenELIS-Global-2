import { expect, Page, test } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import { acceptAndVerifyResults } from "../../../helpers/accept-results";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import type { DemoPresentation } from "../../../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../../../helpers/analyzer-dashboard";
import {
  accessionTextRegExp,
  expectResultVisible,
  openAnalyzerResultsAndWaitForText,
} from "../../../helpers/results-ui";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { resolveHarnessImportsDir } from "../../../helpers/workspace-paths";

/**
 * Analyzer harness: FILE drop → staged results → accept (one story per FILE
 * analyzer seeded by `projects/analyzer-harness/seed-analyzers.sh`).
 *
 * Coverage: QuantStudio 5, QuantStudio 7, FluoroCycler XT (Generic File).
 * Cepheid GeneXpert (ASTM) uses TCP + ASTM framing — see
 * `astm-genexpert-results.spec.ts`, not this file.
 *
 * Requires `projects/analyzer-harness/volume/analyzer-imports` bind-mount
 * (skipped automatically when the directory is absent).
 */

const FIXTURES_DIR = path.resolve(__dirname, "../../../fixtures");
const HOST_IMPORTS_BASE = resolveHarnessImportsDir(__dirname);

const DEFAULT_FILE_IMPORT_POLL_MS = 60_000;
const DEFAULT_FILE_IMPORT_DROP_BUFFER_MS = 45_000;

type FileImportHarnessScenario = {
  readonly analyzerName: string;
  /** Subdirectory under analyzer-imports (matches seeded FileImportConfig path). */
  readonly importDirSafeName: string;
  readonly fixture: string;
  readonly filePrefix: string;
  readonly expectedResults: ReadonlyArray<{
    readonly sampleId: string;
    readonly result: string;
  }>;
  readonly demoTitle: string;
  readonly demoSubtitle: string;
};

/** Fixtures use `HARN-*` accessions from `analyzer-harness-lane-data.sql`. */
const FILE_IMPORT_SCENARIOS: readonly FileImportHarnessScenario[] = [
  {
    analyzerName: "QuantStudio 7",
    importDirSafeName: "quantstudio-7",
    fixture: "quantstudio-e2e-results.xlsx",
    filePrefix: "qs7-results-",
    expectedResults: [
      { sampleId: "HARN-QS7-2026-00001", result: "1520.5" },
      { sampleId: "HARN-QS7-2026-00002", result: "45200" },
      { sampleId: "HARN-QS7-2026-00005", result: "3200.8" },
    ],
    demoTitle: "QuantStudio 7 File Import",
    demoSubtitle: "Drop a result file, review staged results, and accept them.",
  },
  {
    analyzerName: "QuantStudio 5",
    importDirSafeName: "quantstudio-5",
    fixture: "quantstudio-e2e-results-qs5.xls",
    filePrefix: "qs5-results-",
    expectedResults: [
      { sampleId: "HARN-QS5-2026-00001", result: "1520.5" },
      { sampleId: "HARN-QS5-2026-00002", result: "45200" },
      { sampleId: "HARN-QS5-2026-00005", result: "3200.8" },
    ],
    demoTitle: "QuantStudio 5 File Import",
    demoSubtitle: "Drop a result file, review staged results, and accept them.",
  },
  {
    analyzerName: "FluoroCycler XT",
    importDirSafeName: "fluorocycler-xt",
    fixture: "fluorocycler-e2e-results.xlsx",
    filePrefix: "fc-results-",
    expectedResults: [
      { sampleId: "HARN-FC-2026-00001", result: "28.5" },
      { sampleId: "HARN-FC-2026-00002", result: "31.2" },
      { sampleId: "HARN-FC-2026-00003", result: "Negative" },
    ],
    demoTitle: "FluoroCycler XT File Import",
    demoSubtitle: "Drop a result file, review staged results, and accept them.",
  },
];

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
  scenario: FileImportHarnessScenario,
) {
  const fixtureFile = path.join(FIXTURES_DIR, scenario.fixture);
  const fileExtension = path.extname(scenario.fixture);

  fs.mkdirSync(hostImportDir, { recursive: true });
  chmodSharedImportPathChain(hostImportDir);

  const droppedFilePath = path.join(
    hostImportDir,
    `${scenario.filePrefix}${Date.now()}${fileExtension}`,
  );

  // Copy fixture and append a unique timestamp so the bridge's hash-based
  // dedup doesn't skip re-imports of the same fixture across test runs.
  const original = fs.readFileSync(fixtureFile);
  const uniqueSuffix = Buffer.from(`\n${Date.now()}`);
  fs.writeFileSync(droppedFilePath, Buffer.concat([original, uniqueSuffix]));
  expect(fs.existsSync(droppedFilePath)).toBeTruthy();
  await presentation.pause(1_000);

  return droppedFilePath;
}

async function verifyImportedResults(
  page: Page,
  presentation: DemoPresentation,
  scenario: FileImportHarnessScenario,
) {
  await openAnalyzerResultsAndWaitForText(
    page,
    scenario.analyzerName,
    scenario.expectedResults[0].sampleId,
    {
      timeoutMs: fileImportTimeoutMs(),
      perAttemptTimeoutMs: 5_000,
    },
  );

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  for (const expected of scenario.expectedResults) {
    await expect(
      resultsRegion.getByText(accessionTextRegExp(expected.sampleId)).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectResultVisible(resultsRegion, expected.result);
  }

  await presentation.pause(2_000);
}

for (const scenario of FILE_IMPORT_SCENARIOS) {
  test.describe(`${scenario.analyzerName} file import harness`, () => {
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
        scenario.importDirSafeName,
        "incoming",
      );

      await presentation.title(scenario.demoTitle, scenario.demoSubtitle);

      await goToAnalyzerDashboard(page, testInfo);

      await presentation.step(
        1,
        "Find the pre-configured analyzer for this lane",
      );
      await findAnalyzerRow(page, scenario.analyzerName, testInfo);

      await presentation.step(2, "Drop a result file into the watched folder");
      droppedFilePath = await dropFixtureFile(
        hostImportDir,
        presentation,
        scenario,
      );

      await presentation.step(3, "Review the imported results");
      await verifyImportedResults(page, presentation, scenario);

      await acceptAndVerifyResults(
        page,
        presentation,
        3,
        scenario.expectedResults[0].sampleId,
      );

      await presentation.title(
        "Story Complete",
        "The file import flow relies on visible UI evidence only.",
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
}
