import { test, expect } from "@playwright/test";
import { loadFileImportFixtures } from "../fixtures/file-import-setup";

const QUANTSTUDIO_ANALYZER = "E2E-FILE-QuantStudio-Analyzer";

test.describe("File import QuantStudio (GenericFile + EXCEL)", () => {
  test.beforeAll(() => {
    if (process.env.CI !== "true") {
      loadFileImportFixtures();
    }
  });

  test("QuantStudio analyzer exists with EXCEL config", async ({ page }) => {
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
    const analyzer = analyzers.find((row) => row.name === QUANTSTUDIO_ANALYZER);
    expect(analyzer).toBeDefined();

    const cfgRes = await page.request.get(
      `${apiBase}/file-import/configurations/analyzer/${analyzer!.id}`,
    );
    expect(cfgRes.ok()).toBeTruthy();
    const cfgBody = await cfgRes.json();
    expect(cfgBody.fileFormat).toBe("EXCEL");
    expect(cfgBody.filePattern).toBe("*.xls");
  });

  test.skip("upload quantstudio-qs7.xls and verify preview — requires M2 upload UI", async () => {
    // 1. Navigate to upload page, select E2E-FILE-QuantStudio-Analyzer
    // 2. Upload frontend/playwright/fixtures/quantstudio-qs7.xls
    // 3. Verify preview table shows parsed rows with Sample Name, CT, VL columns
    // 4. Submit and verify results queued
  });
});
