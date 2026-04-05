import { test, expect } from "../../../helpers/test-base";

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
 *
 * Auth note: PUT/POST requests to OE REST API require either a valid CSRF
 * token (session auth) or Basic Auth (which bypasses CSRF). We use Basic Auth
 * headers for API mutation calls to avoid CSRF complexity.
 */

/** Build Basic Auth header from env credentials. */
function basicAuthHeaders(): Record<string, string> {
  const user = process.env.TEST_USER || "admin";
  const pass = process.env.TEST_PASS || "adminADMIN!";
  return {
    Authorization: `Basic ${Buffer.from(`${user}:${pass}`).toString("base64")}`,
  };
}

/**
 * All FILE analyzers seeded by projects/analyzer-harness/seed-analyzers.sh.
 *
 * filePattern reflects deriveFilePattern() output:
 *   - QuantStudio profile has supported_extensions: [".xls", ".xlsx"] → "*{.xls,.xlsx}"
 *   - FluoroCycler XT profile has supported_extensions: [".xlsx", ".xls"] → "*{.xlsx,.xls}"
 *   Order matches the JSON array order in each profile.
 */
const FILE_ANALYZERS = [
  { name: "QuantStudio 5", fileFormat: "EXCEL", filePattern: "*{.xls,.xlsx}" },
  {
    name: "QuantStudio 7",
    fileFormat: "EXCEL",
    filePattern: "*{.xls,.xlsx}",
  },
  {
    name: "FluoroCycler XT",
    fileFormat: "EXCEL",
    filePattern: "*{.xlsx,.xls}",
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

    // Update to TSV — use Basic Auth to bypass CSRF on PUT
    const putRes = await page.request.put(
      `${api}/file-import/configurations/${cfg.id}`,
      {
        headers: basicAuthHeaders(),
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

    // Re-read and verify file config
    const verifyRes = await page.request.get(
      `${api}/file-import/configurations/analyzer/${found!.id}`,
    );
    expect(verifyRes.ok()).toBeTruthy();
    const updated = await verifyRes.json();
    expect(updated.fileFormat).toBe("TSV");
    expect(updated.filePattern).toBe("*.tsv");

    // OGC-526: Verify unified fields propagated to the Analyzer entity
    const analyzerRes = await page.request.get(`${api}/analyzers/${found!.id}`);
    expect(analyzerRes.ok()).toBeTruthy();
    const analyzerEntity = await analyzerRes.json();
    expect(analyzerEntity.fileFormat).toBe("TSV");
    expect(analyzerEntity.filePattern).toBe("*.tsv");

    // Revert to original so other tests see the seeded state
    const revertRes = await page.request.put(
      `${api}/file-import/configurations/${cfg.id}`,
      {
        headers: basicAuthHeaders(),
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
    expect(cfg.filePattern).toBe("*{.xls,.xlsx}");
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
