import type { Page, TestInfo } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { createAnalyzerClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { sendGeneXpertAstm } from "../../../helpers/analyzer-native-traffic";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  TIMEOUT_SCALE,
} from "../../../helpers/timeouts";

test.use({ viewport: { width: 1600, height: 1000 } });

const API = "/api/OpenELIS-Global/rest";

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

async function expectClinicalReadback(
  page: Page,
  order: Awaited<ReturnType<typeof createAnalyzerClinicalOrder>>,
  expectedResult: string | RegExp,
) {
  const { accession, patientLastName, testId, specimenId } = order;
  const savedValue = expect.poll(
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
  );
  if (typeof expectedResult === "string") await savedValue.toBe(expectedResult);
  else await savedValue.toMatch(expectedResult);

  const response = await page.request.get(
    `${API}/order/search?labNumber=${encodeURIComponent(accession)}`,
  );
  expect(response.ok()).toBeTruthy();
  const savedOrder = await response.json();
  expect(savedOrder.labNumber).toBe(accession);
  expect(savedOrder.patientProperties.lastName).toBe(patientLastName);
  expect(savedOrder.samples).toHaveLength(1);
  expect(savedOrder.samples[0].sampleTypeId).toBe(specimenId);
  expect(
    savedOrder.samples[0].tests.map((test: { id: string }) => test.id),
  ).toContain(testId);
}

test.describe("OGC-1054 stock analyzer result workflow", () => {
  for (const scenario of [
    {
      code: "MTB-RIF",
      testName: "Xpert MTB/RIF",
      loinc: "85362-2",
      raw: "NOT DETECTED",
      result: "NOT DETECTED",
    },
    {
      code: "RIF",
      testName: "Xpert RIF Resistance",
      loinc: "46244-0",
      raw: "DETECTED",
      result: "DETECTED",
    },
    {
      code: "RIF",
      testName: "Xpert RIF Resistance",
      loinc: "46244-0",
      raw: "NOT DETECTED",
      result: "NOT DETECTED",
    },
    {
      code: "RIF",
      testName: "Xpert RIF Resistance",
      loinc: "46244-0",
      raw: "INDETERMINATE",
      result: "Indeterminate",
    },
  ]) {
    test(`GeneXpert sends ${scenario.code} ${scenario.raw} to the correct patient order`, async ({
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
      const analyzer = await analyzerByName(
        page,
        analyzerName,
        "genexpert-astm",
      );
      const order = await createAnalyzerClinicalOrder(page, {
        profileId: analyzer.profileId,
        profileRevision: analyzer.profileRevision,
        sourceCode: scenario.code,
        expectedTestName: scenario.testName,
        expectedLoinc: scenario.loinc,
        expectedMappedValue: scenario.raw,
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
        scenario.code,
        scenario.raw,
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
      await expect(row).toContainText(scenario.result, {
        timeout: LONG_TIMEOUT,
      });
      await capture(page, testInfo, "gene-received-result");
      await row.locator('label[for$=".isAccepted"]').click();
      await page.getByRole("button", { name: "Save", exact: true }).click();
      await expect(row).not.toBeVisible({ timeout: LONG_TIMEOUT });
      await expectClinicalReadback(page, order, scenario.result);
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
        { waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT },
      );
      const clinicalRow = page.getByRole("row", {
        name: new RegExp(order.accession),
      });
      await expect(clinicalRow).toContainText(scenario.testName, {
        timeout: LONG_TIMEOUT,
      });
      await expect(clinicalRow).toContainText(scenario.result);
      await capture(page, testInfo, "gene-clinical-result-saved");
    });
  }
});
