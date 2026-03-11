import { test, expect } from "@playwright/test";
import { loadFileImportFixtures } from "../fixtures/file-import-setup";
import fixtureData from "../fixtures/FileImport.json";

test.describe("File import configuration persistence", () => {
  test.skip(
    process.env.CI === "true" && process.env.ANALYZER_HARNESS !== "true",
    "Requires docker + database + file-import fixtures not available in default CI",
  );

  test.beforeAll(() => {
    loadFileImportFixtures();
  });

  test("updates and persists fileFormat", async ({ page }) => {
    const baseUrl = process.env.BASE_URL || "https://localhost";
    const apiBase = `${baseUrl}/api/OpenELIS-Global/rest/analyzer`;

    const analyzersRes = await page.request.get(`${apiBase}/analyzers`);
    expect(analyzersRes.ok()).toBeTruthy();
    const analyzersBody = await analyzersRes.json();
    const analyzers = (analyzersBody?.analyzers || []) as {
      id: string;
      name: string;
    }[];
    const analyzer = analyzers.find(
      (row) => row.name === fixtureData.analyzerName,
    );
    expect(analyzer).toBeDefined();
    const analyzerId = analyzer!.id;

    const cfgRes = await page.request.get(
      `${apiBase}/file-import/configurations/analyzer/${analyzerId}`,
    );
    expect(cfgRes.ok()).toBeTruthy();
    const cfgBody = await cfgRes.json();
    const configId = cfgBody.id;
    const hasFileFormat = cfgBody.fileFormat !== undefined;
    if (hasFileFormat) {
      expect(cfgBody.fileFormat).toBe(fixtureData.formatCsv);
    }

    const putRes = await page.request.put(
      `${apiBase}/file-import/configurations/${configId}`,
      {
        data: {
          importDirectory: fixtureData.importDirectory,
          archiveDirectory: fixtureData.archiveDirectory,
          errorDirectory: fixtureData.errorDirectory,
          filePattern: "*.tsv",
          ...(hasFileFormat && { fileFormat: fixtureData.formatTsv }),
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

    const getRes = await page.request.get(
      `${apiBase}/file-import/configurations/analyzer/${analyzerId}`,
    );
    expect(getRes.ok()).toBeTruthy();
    const getBody = await getRes.json();
    if (hasFileFormat) {
      expect(getBody.fileFormat).toBe(fixtureData.formatTsv);
    }
    expect(getBody.filePattern).toBe("*.tsv");
  });
});
