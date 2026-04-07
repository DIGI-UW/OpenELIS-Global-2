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
};

type MockFileResult = {
  readonly sampleId: string;
  readonly result: string;
  readonly testCode?: string;
};

type MockFileResponse = {
  status: string;
  written_path: string | null;
  metadata: {
    analyzerName: string;
    format: string;
    fixture: string;
    results: MockFileResult[];
  };
};

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
  {
    analyzerName: "FluoroCycler XT",
    importDirSafeName: "fluorocycler-xt",
    mockTemplate: "hain_fluorocycler",
    demoTitle: "FluoroCycler XT File Import",
    demoSubtitle: "Drop a result file, review staged results, and accept them.",
  },
  {
    analyzerName: "Wondfo Finecare FS-205",
    importDirSafeName: "wondfo-finecare-fs-205",
    mockTemplate: "wondfo_finecare",
    demoTitle: "Wondfo Finecare FS-205 File Import",
    demoSubtitle:
      "CSV import from POCT immunoassay — comparison operators preserved.",
  },
  {
    analyzerName: "Tecan Infinite F50",
    importDirSafeName: "tecan-infinite-f50",
    mockTemplate: "tecan_f50",
    demoTitle: "Tecan Infinite F50 File Import",
    demoSubtitle: "ELISA OD results from well-per-row CSV export.",
  },
  {
    analyzerName: "Thermo Multiskan FC",
    importDirSafeName: "thermo-multiskan-fc",
    mockTemplate: "multiskan_fc",
    demoTitle: "Thermo Multiskan FC File Import",
    demoSubtitle:
      "ELISA OD results from well-per-row CSV export — French locale support.",
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

/**
 * Ask the mock server to drop a fixture file into the bridge watched folder
 * and return the parsed metadata (accessions + results).
 */
async function dropFixtureViaMock(
  scenario: FileImportHarnessScenario,
): Promise<MockFileResponse> {
  const targetDir = `/data/analyzer-imports/${scenario.importDirSafeName}/incoming`;

  const response = await fetch(
    `${MOCK_API_URL}/simulate/file/${scenario.mockTemplate}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ target_dir: targetDir }),
    },
  );

  if (!response.ok) {
    const text = await response.text();
    throw new Error(
      `Mock server POST /simulate/file/${scenario.mockTemplate} failed: ${response.status} ${text}`,
    );
  }

  const data = (await response.json()) as MockFileResponse;

  if (!data.metadata?.results?.length) {
    throw new Error(
      `Mock server returned no results for ${scenario.mockTemplate}: ${JSON.stringify(data)}`,
    );
  }

  return data;
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

const SKIP_IN_CI = !!process.env.CI;

for (const scenario of FILE_IMPORT_SCENARIOS) {
  const describeBlock = SKIP_IN_CI ? test.describe.skip : test.describe;
  describeBlock(`${scenario.analyzerName} file import harness`, () => {
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
      const mockResponse = await dropFixtureViaMock(scenario);
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
