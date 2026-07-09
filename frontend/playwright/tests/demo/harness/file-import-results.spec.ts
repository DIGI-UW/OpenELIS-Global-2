import { expect, Page, test } from "../../../helpers/test-base";
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
import {
  dropFixtureViaMock,
  type MockFileResult,
} from "../../../helpers/file-import-delivery";

/**
 * Analyzer harness: FILE drop → staged results → accept.
 *
 * All FILE analyzers use the same template workflow:
 * 1. Mock server drops the real fixture file into the watched folder
 * 2. Mock returns parsed metadata (accessions, results) — the single source of truth
 * 3. Test verifies results appear in OE and accepts them
 *
 * Fixture files live in the mock server (tools/analyzer-mock-server/fixtures/).
 * The test never parses files or hardcodes expected values.
 */

const MOCK_API_URL = process.env.MOCK_SIMULATOR_URL || "http://localhost:8085";

const DEFAULT_FILE_IMPORT_POLL_MS = 60_000;
const DEFAULT_FILE_IMPORT_DROP_BUFFER_MS = 45_000;

type FileImportHarnessScenario = {
  readonly analyzerName: string;
  /** Subdirectory under analyzer-imports (matches seeded analyzer import path). */
  readonly importDirSafeName: string;
  /** Mock server template name (maps to templates/{name}.json). */
  readonly mockTemplate: string;
  readonly demoTitle: string;
  readonly demoSubtitle: string;
  /**
   * Admin-declared test code for upload path (production parity). Set for
   * analyzers whose fixture files have no per-row test-code column —
   * matches the testCode field a lab tech fills in the bridge admin
   * upload UI. When set, the test uses the bridge `/admin/upload` flow
   * instead of a direct watched-dir drop.
   */
  readonly uploadTestCode?: string;
};

// Scenarios are limited to analyzers seeded by projects/analyzer-harness/
// seed-analyzers.sh. The harness baseline trimmed to 4 representative
// analyzers (one per transport class); FILE coverage is QS5 + QS7 here.
// Coverage for FluoroCycler / Wondfo / Tecan / Multiskan lives in
// analyzer-demo-flow.spec.ts, which creates analyzers from scratch via the
// dashboard UI rather than relying on the harness seed.
const FILE_IMPORT_SCENARIOS: readonly FileImportHarnessScenario[] = [
  {
    analyzerName: "QuantStudio 7",
    importDirSafeName: "quantstudio-7",
    mockTemplate: "quantstudio7",
    demoTitle: "QuantStudio 7 File Import",
    demoSubtitle: "Drop a result file, review staged results, and accept them.",
  },
  {
    analyzerName: "QuantStudio 5",
    importDirSafeName: "quantstudio-5",
    mockTemplate: "quantstudio5",
    demoTitle: "QuantStudio 5 File Import",
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
  return Math.max(2 * pollMs + bufferMs, 60_000);
}

async function verifyImportedResults(
  page: Page,
  presentation: DemoPresentation,
  scenario: FileImportHarnessScenario,
  expectedResults: ReadonlyArray<MockFileResult>,
) {
  const allAccessions = expectedResults
    .map((r) => r.sampleId)
    .filter((v, i, a) => a.indexOf(v) === i);

  await openAnalyzerResultsAndWaitForText(
    page,
    scenario.analyzerName,
    expectedResults[0].sampleId,
    {
      timeoutMs: fileImportTimeoutMs(),
      perAttemptTimeoutMs: 5_000,
      allExpectedAccessions: allAccessions,
    },
  );

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  for (const expected of expectedResults) {
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

    test("import and accept results from a watched folder", async ({
      page,
    }, testInfo) => {
      const presentation = createDemoPresentation(page, testInfo);

      await presentation.title(scenario.demoTitle, scenario.demoSubtitle);

      await goToAnalyzerDashboard(page, testInfo);

      await presentation.step(
        1,
        "Find the pre-configured analyzer for this lane",
      );
      await findAnalyzerRow(page, scenario.analyzerName, testInfo);

      await presentation.step(
        2,
        "Mock server drops a fixture file into the watched folder",
      );
      const mockResponse = await dropFixtureViaMock(page, {
        mockTemplate: scenario.mockTemplate,
        analyzerName: scenario.analyzerName,
        importDirSafeName: scenario.importDirSafeName,
        uploadTestCode: scenario.uploadTestCode,
        mockApiUrl: MOCK_API_URL,
      });
      const expectedResults = mockResponse.metadata.results;

      await presentation.pause(1_000);

      await presentation.step(3, "Review the imported results");
      await verifyImportedResults(
        page,
        presentation,
        scenario,
        expectedResults,
      );

      await acceptAndVerifyResults(
        page,
        presentation,
        3,
        expectedResults[0].sampleId,
      );

      await presentation.title(
        "Story Complete",
        "The file import flow relies on visible UI evidence only.",
      );
    });
  });
}
