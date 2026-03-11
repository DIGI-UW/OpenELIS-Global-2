import { test, expect } from "@playwright/test";
import { loadFileImportFixtures } from "../fixtures/file-import-setup";

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

test.describe("File import analyzers (fixture verification)", () => {
  test.beforeAll(() => {
    if (process.env.CI !== "true") {
      loadFileImportFixtures();
    }
  });

  for (const analyzer of FILE_ANALYZERS) {
    test(`${analyzer.name} exists with ${analyzer.fileFormat} config`, async ({
      page,
    }) => {
      test.skip(
        process.env.CI === "true",
        "Requires docker + database + file-import fixtures not available in default CI",
      );
      const baseUrl = process.env.BASE_URL || "https://localhost";
      const apiBase = `${baseUrl}/api/OpenELIS-Global/rest/analyzer`;

      const analyzersRes = await page.request.get(`${apiBase}/analyzers`);
      expect(analyzersRes.ok()).toBeTruthy();
      const analyzersBody = await analyzersRes.json();
      const analyzers = (analyzersBody?.analyzers || []) as {
        id: string;
        name: string;
      }[];
      const found = analyzers.find((row) => row.name === analyzer.name);
      expect(found).toBeDefined();

      const cfgRes = await page.request.get(
        `${apiBase}/file-import/configurations/analyzer/${found!.id}`,
      );
      expect(cfgRes.ok()).toBeTruthy();
      const cfgBody = await cfgRes.json();
      expect(cfgBody.fileFormat).toBe(analyzer.fileFormat);
      expect(cfgBody.filePattern).toBe(analyzer.filePattern);
      expect(cfgBody.importDirectory).toContain("analyzer-imports");
    });
  }
});
