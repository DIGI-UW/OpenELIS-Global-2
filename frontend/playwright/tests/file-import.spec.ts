import { test, expect } from "@playwright/test";
import fixtureData from "../fixtures/FileImport.json";

/**
 * FILE protocol E2E tests.
 *
 * These tests verify that every supported FILE analyzer has:
 *   1. An analyzer row in the database
 *   2. A FileImportConfiguration with correct format, pattern, and directory
 *   3. Config persistence (update + re-read)
 *
 * Fixtures are loaded by the CI workflow (docker exec psql < file-import-e2e.sql)
 * before tests run. No skip guards needed — the Playwright project split
 * ensures these only run when the "file-import" project is selected.
 */

/** All FILE analyzers seeded by file-import-e2e.sql */
const FILE_ANALYZERS = [
  { name: "E2E-FILE-CSV-Analyzer", fileFormat: "CSV", filePattern: "*.csv" },
  {
    name: "E2E-FILE-QuantStudio-Analyzer",
    fileFormat: "EXCEL",
    filePattern: "*.xls",
  },
  { name: "E2E-FILE-Tecan-F50", fileFormat: "CSV", filePattern: "*.csv" },
  { name: "E2E-FILE-Multiskan-FC", fileFormat: "CSV", filePattern: "*.csv" },
  {
    name: "E2E-FILE-FluoroCycler-XT",
    fileFormat: "EXCEL",
    filePattern: "*.xlsx",
  },
  { name: "E2E-FILE-DT-Prime", fileFormat: "XML", filePattern: "*.xml" },
];

function apiBase(baseURL: string | undefined): string {
  const base = baseURL || process.env.BASE_URL || "https://localhost";
  return `${base}/api/OpenELIS-Global/rest/analyzer`;
}

// ─── 1. Analyzer + FileImportConfiguration existence (data-driven) ───
test.describe("FILE analyzer configurations", () => {
  for (const analyzer of FILE_ANALYZERS) {
    test(`${analyzer.name} — ${analyzer.fileFormat} format, ${analyzer.filePattern} pattern`, async ({
      page,
      baseURL,
    }) => {
      const api = apiBase(baseURL);

      // Find analyzer by name
      const listRes = await page.request.get(`${api}/analyzers`);
      expect(listRes.ok()).toBeTruthy();
      const { analyzers } = (await listRes.json()) as {
        analyzers: { id: string; name: string }[];
      };
      const found = analyzers.find((a) => a.name === analyzer.name);
      expect(
        found,
        `Analyzer "${analyzer.name}" not found in database`,
      ).toBeDefined();

      // Verify FileImportConfiguration
      const cfgRes = await page.request.get(
        `${api}/file-import/configurations/analyzer/${found!.id}`,
      );
      expect(cfgRes.ok()).toBeTruthy();
      const cfg = await cfgRes.json();
      expect(cfg.fileFormat).toBe(analyzer.fileFormat);
      expect(cfg.filePattern).toBe(analyzer.filePattern);
      expect(cfg.importDirectory).toContain("analyzer-imports");
      expect(cfg.active).toBe(true);
    });
  }
});

// ─── 2. Config persistence (update + re-read round-trip) ─────────────
test.describe("FILE config persistence", () => {
  test("updates fileFormat and filePattern, then verifies persistence", async ({
    page,
    baseURL,
  }) => {
    const api = apiBase(baseURL);

    // Find the CSV test analyzer
    const listRes = await page.request.get(`${api}/analyzers`);
    expect(listRes.ok()).toBeTruthy();
    const { analyzers } = (await listRes.json()) as {
      analyzers: { id: string; name: string }[];
    };
    const found = analyzers.find((a) => a.name === fixtureData.analyzerName);
    expect(found).toBeDefined();

    // Read current config
    const cfgRes = await page.request.get(
      `${api}/file-import/configurations/analyzer/${found!.id}`,
    );
    expect(cfgRes.ok()).toBeTruthy();
    const cfg = await cfgRes.json();
    expect(cfg.fileFormat).toBe(fixtureData.formatCsv);

    // Update to TSV
    const putRes = await page.request.put(
      `${api}/file-import/configurations/${cfg.id}`,
      {
        data: {
          importDirectory: fixtureData.importDirectory,
          archiveDirectory: fixtureData.archiveDirectory,
          errorDirectory: fixtureData.errorDirectory,
          filePattern: "*.tsv",
          fileFormat: fixtureData.formatTsv,
          columnMappings: {
            Sample_ID: "sampleId",
            Test_Code: "testCode",
            Result: "result",
          },
          delimiter: "\t",
          hasHeader: true,
          active: true,
        },
      },
    );
    expect(putRes.ok()).toBeTruthy();

    // Re-read and verify
    const verifyRes = await page.request.get(
      `${api}/file-import/configurations/analyzer/${found!.id}`,
    );
    expect(verifyRes.ok()).toBeTruthy();
    const updated = await verifyRes.json();
    expect(updated.fileFormat).toBe(fixtureData.formatTsv);
    expect(updated.filePattern).toBe("*.tsv");
  });
});

// ─── 3. QuantStudio-specific: EXCEL config + column mappings ─────────
test.describe("QuantStudio EXCEL config", () => {
  test("has correct column mappings for QuantStudio XLS", async ({
    page,
    baseURL,
  }) => {
    const api = apiBase(baseURL);

    const listRes = await page.request.get(`${api}/analyzers`);
    expect(listRes.ok()).toBeTruthy();
    const { analyzers } = (await listRes.json()) as {
      analyzers: { id: string; name: string }[];
    };
    const qs = analyzers.find(
      (a) => a.name === "E2E-FILE-QuantStudio-Analyzer",
    );
    expect(qs).toBeDefined();

    const cfgRes = await page.request.get(
      `${api}/file-import/configurations/analyzer/${qs!.id}`,
    );
    expect(cfgRes.ok()).toBeTruthy();
    const cfg = await cfgRes.json();

    expect(cfg.fileFormat).toBe("EXCEL");
    expect(cfg.filePattern).toBe("*.xls");
    expect(cfg.hasHeader).toBe(true);

    // Verify QuantStudio column mappings from profile
    const mappings = cfg.columnMappings || {};
    expect(mappings["Sample Name"]).toBe("sampleId");
    expect(mappings["Target Name"]).toBe("testCode");
    expect(mappings["Quantity Mean"]).toBe("result");
    expect(mappings["CT"]).toBe("ctValue");
    expect(mappings["Well Position"]).toBe("position");
  });
});
