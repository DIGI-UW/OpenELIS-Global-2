import type { Page, TestInfo } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { createAnalyzerClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import {
  sendGeneXpertAstm,
  writeFluoroCyclerFile,
} from "../../../helpers/analyzer-native-traffic";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  TIMEOUT_SCALE,
} from "../../../helpers/timeouts";

const API = "/api/OpenELIS-Global/rest";
const FILE_DIRECTORY = "/data/analyzer-imports/fluorocycler-xt/incoming";
const FILE_CASES = [
  { accession: "DEV01263000000000001", expectedValue: "1250" },
  { accession: "DEV01263000000000002", expectedValue: "450" },
];

type Analyzer = {
  id: string;
  name: string;
  profileId: string;
  profileRevision: number;
  bridgeConnectionId: string;
  status: string;
};

async function capture(page: Page, testInfo: TestInfo, name: string) {
  await testInfo.attach(name, {
    body: await page.screenshot({ fullPage: false }),
    contentType: "image/png",
  });
}

async function analyzerByName(
  page: Page,
  name: string,
  profileId: string,
): Promise<Analyzer> {
  const response = await page.request.get(`${API}/analyzer/analyzers`);
  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as { analyzers: Analyzer[] };
  const matches = payload.analyzers.filter(
    (candidate) => candidate.name === name && candidate.profileId === profileId,
  );
  expect(matches, `One provisioned ${name} connection`).toHaveLength(1);
  return matches[0];
}

/** Review the supplied choices in the UI; this never edits or excludes a row. */
async function confirmShippedMapping(
  page: Page,
  analyzer: Analyzer,
): Promise<void> {
  const response = await page.request.get(
    `${API}/analyzer-types/${analyzer.profileId}/mapping?revision=${analyzer.profileRevision}`,
  );
  expect(response.ok()).toBeTruthy();
  const mapping = (await response.json()) as {
    confirmation: { state: string };
  };
  await page.goto(
    `/analyzers/types/${analyzer.profileId}/mapping?revision=${analyzer.profileRevision}`,
    { waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT },
  );
  await expect(
    page.getByRole("button", { name: "Update shared mappings" }),
  ).toBeDisabled();
  if (mapping.confirmation.state !== "CURRENT") {
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled({ timeout: LONG_TIMEOUT });
    await confirm.click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  } else {
    await expect(page.getByText("Current confirmation")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  }
}

async function activateSavedConnection(
  page: Page,
  analyzer: Analyzer,
  directory?: string,
) {
  await page.goto("/analyzers", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  const row = page.getByTestId(`analyzer-row-${analyzer.id}`);
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  if ((await row.innerText()).includes("Active")) return;

  await row.getByRole("button", { name: "Actions" }).click();
  await page.getByRole("menuitem", { name: "Configure connection" }).click();
  const setup = new AnalyzerSetupPage(page);
  await setup.expectOpen();
  if (directory) {
    await setup.fillImportDirectory(directory);
  }
  await expect(page.getByText("Analyzer is ready to activate")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await page.getByRole("button", { name: "Finish and activate" }).click();
  await expect(row).toContainText("Active", { timeout: LONG_TIMEOUT });
}

async function expectClinicalReadback(
  page: Page,
  accession: string,
  patientLastName: string,
  testId: string,
  expectedResult: string | RegExp,
) {
  await expect
    .poll(
      async () => {
        const response = await page.request.get(
          `${API}/accession-results?accessionNumber=${encodeURIComponent(accession)}`,
        );
        if (!response.ok()) return null;
        const data = (await response.json()) as {
          lastName: string;
          testResult: Array<{
            testId: string;
            resultValue: string;
            resultType: string;
            dictionaryResults?: Array<{ id: string; value: string }>;
          }>;
        };
        expect(data.lastName).toBe(patientLastName);
        const results = data.testResult.filter(
          (result) => result.testId === testId,
        );
        expect(results, `One clinical test for ${accession}`).toHaveLength(1);
        const result = results[0];
        if (result.resultType === "D") {
          const choice = result.dictionaryResults?.find(
            (entry) => entry.id === result.resultValue,
          );
          expect(
            choice,
            `Saved dictionary choice ${result.resultValue}`,
          ).toBeDefined();
          return choice?.value;
        }
        return result.resultValue;
      },
      { timeout: LONG_TIMEOUT },
    )
    .toMatch(expectedResult);
}

test.describe("OGC-1054 stock analyzer result workflow", () => {
  test("GeneXpert sends an ASTM result to the correct patient order", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const runId = Date.now();
    const analyzerName = `E2E GeneXpert ${runId}`;
    const listenerPort = String(40_000 + (runId % 20_000));
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);
    await list.goto();
    await list.clickAdd();
    await setup.expectOpen();
    await setup.selectProfile("Cepheid GeneXpert (ASTM Mode)");
    await setup.fillName(analyzerName);
    await setup.selectLabUnit("Molecular Biology");
    await setup.continueToVerify();
    const verifyUrl = page.url();
    const analyzer = await analyzerByName(page, analyzerName, "genexpert-astm");
    const order = await createAnalyzerClinicalOrder(page, {
      profileId: analyzer.profileId,
      profileRevision: analyzer.profileRevision,
      sourceCode: "MTB-RIF",
      expectedTestName: "Xpert MTB/RIF",
      expectedLoinc: "85362-2",
      expectedMappedValue: "NOT DETECTED",
      specimenName: "Sputum",
    });
    await confirmShippedMapping(page, analyzer);
    await capture(page, testInfo, "gene-shipped-mapping-confirmed");
    await page.goto(verifyUrl, {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await setup.continueToConnect();
    await setup.fillPort(listenerPort);
    await page.getByRole("button", { name: "Finish and activate" }).click();
    const analyzerRow = page.getByTestId(`analyzer-row-${analyzer.id}`);
    await expect(analyzerRow).toContainText("Active", {
      timeout: LONG_TIMEOUT,
    });
    await capture(page, testInfo, "gene-connection-active");

    await sendGeneXpertAstm(
      page.request,
      analyzer.bridgeConnectionId,
      order.accession,
      "MTB-RIF",
      "NOT DETECTED",
    );
    await expect
      .poll(
        async () => {
          const response = await page.request.get(
            `${API}/AnalyzerResults?id=${analyzer.id}`,
          );
          if (!response.ok()) return false;
          const worklist = (await response.json()) as {
            resultList?: Array<{ accessionNumber?: string }>;
          };
          return (worklist.resultList ?? []).some(
            (result) => result.accessionNumber === order.accession,
          );
        },
        { timeout: LONG_TIMEOUT },
      )
      .toBe(true);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    const row = page.getByRole("row", { name: new RegExp(order.accession) });
    await expect(row).toContainText("NOT DETECTED", { timeout: LONG_TIMEOUT });
    await capture(page, testInfo, "gene-received-result");
    await row.locator('label[for$=".isAccepted"]').click();
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(row).not.toBeVisible({ timeout: LONG_TIMEOUT });
    await expectClinicalReadback(
      page,
      order.accession,
      order.patientLastName,
      order.testId,
      "NOT DETECTED",
    );
    await page.goto(
      `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
      { waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT },
    );
    const clinicalRow = page.getByRole("row", {
      name: new RegExp(order.accession),
    });
    await expect(clinicalRow).toContainText("Xpert MTB/RIF", {
      timeout: LONG_TIMEOUT,
    });
    await expect(clinicalRow).toContainText("NOT DETECTED");
    await capture(page, testInfo, "gene-clinical-result-saved");
  });

  test("FluoroCycler imports a watched file for the correct clinical orders", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const analyzer = await analyzerByName(
      page,
      "FluoroCycler XT",
      "fluorocycler-xt",
    );
    const orders = [];
    for (const { accession } of FILE_CASES) {
      orders.push(
        await createAnalyzerClinicalOrder(page, {
          accession,
          profileId: analyzer.profileId,
          profileRevision: analyzer.profileRevision,
          sourceCode: "VIH-1",
          expectedTestName: "HIV Viral Load",
          expectedLoinc: "20447-9",
          specimenName: "Plasma",
        }),
      );
    }
    await confirmShippedMapping(page, analyzer);
    await capture(page, testInfo, "file-shipped-mapping-confirmed");
    await activateSavedConnection(page, analyzer, FILE_DIRECTORY);
    await capture(page, testInfo, "file-watch-directory-configured");

    const emitted = await writeFluoroCyclerFile(page.request, FILE_DIRECTORY);
    for (const { accession, expectedValue } of FILE_CASES) {
      expect(
        emitted.find((result) => result.sampleId === accession),
      ).toMatchObject({ sampleId: accession, result: expectedValue });
    }
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    for (const order of orders) {
      const row = page.getByRole("row", { name: new RegExp(order.accession) });
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await row.locator('label[for$=".isAccepted"]').click();
    }
    await capture(page, testInfo, "file-received-results");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    for (const order of orders) {
      const expectedValue = FILE_CASES.find(
        (entry) => entry.accession === order.accession,
      )?.expectedValue;
      expect(expectedValue).toBeDefined();
      await expectClinicalReadback(
        page,
        order.accession,
        order.patientLastName,
        order.testId,
        new RegExp(`^${expectedValue}(?:\\.0+)?$`),
      );
    }
    await capture(page, testInfo, "file-clinical-results-saved");
  });
});
