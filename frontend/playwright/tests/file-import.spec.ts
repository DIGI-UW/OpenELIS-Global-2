import { test, expect } from "@playwright/test";
/**
 * FILE protocol E2E tests.
 *
 * These tests verify that every supported FILE analyzer has:
 *   1. An analyzer row in the database
 *   2. A FileImportConfiguration with correct format, pattern, and directory
 *   3. Config persistence (update + re-read)
 *
 * Harness runs seed analyzers through the REST API layer
 * (`projects/analyzer-harness/seed-analyzers.sh`) after SQL cleanup fixtures
 * are loaded. This spec validates the resulting FILE analyzers in the
 * `harness` Playwright project.
 */

/** All FILE analyzers seeded by projects/analyzer-harness/seed-analyzers.sh */
const FILE_ANALYZERS = [
  { name: "QuantStudio 5", fileFormat: "EXCEL", filePattern: "*.xls" },
  {
    name: "QuantStudio 7",
    fileFormat: "EXCEL",
    filePattern: "*.xlsx",
  },
  {
    name: "FluoroCycler XT",
    fileFormat: "EXCEL",
    filePattern: "*.xlsx",
  },
];

const PERSIST_ANALYZER_NAME = "QuantStudio 5";

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

    // Find the seeded analyzer used for persistence checks
    const listRes = await page.request.get(`${api}/analyzers`);
    expect(listRes.ok()).toBeTruthy();
    const { analyzers } = (await listRes.json()) as {
      analyzers: { id: string; name: string }[];
    };
    const found = analyzers.find((a) => a.name === PERSIST_ANALYZER_NAME);
    expect(found).toBeDefined();

    // Read current config
    const cfgRes = await page.request.get(
      `${api}/file-import/configurations/analyzer/${found!.id}`,
    );
    expect(cfgRes.ok()).toBeTruthy();
    const cfg = await cfgRes.json();
    expect(cfg.fileFormat).toBe("EXCEL");

    // Update to TSV
    const putRes = await page.request.put(
      `${api}/file-import/configurations/${cfg.id}`,
      {
        data: {
          importDirectory: cfg.importDirectory,
          archiveDirectory: cfg.archiveDirectory,
          errorDirectory: cfg.errorDirectory,
          filePattern: "*.tsv",
          fileFormat: "TSV",
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
    expect(updated.fileFormat).toBe("TSV");
    expect(updated.filePattern).toBe("*.tsv");

    // Revert to original so other tests see the seeded state
    const revertRes = await page.request.put(
      `${api}/file-import/configurations/${cfg.id}`,
      {
        data: {
          ...cfg,
          fileFormat: cfg.fileFormat,
          filePattern: cfg.filePattern,
          delimiter: cfg.delimiter,
        },
      },
    );
    expect(revertRes.ok()).toBeTruthy();
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
    const qs = analyzers.find((a) => a.name === "QuantStudio 5");
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
