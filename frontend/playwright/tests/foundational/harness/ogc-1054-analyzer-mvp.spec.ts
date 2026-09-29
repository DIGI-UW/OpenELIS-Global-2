import type { Page, TestInfo } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { createAnalyzerClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  sendGeneXpertAstm,
  writeFluoroCyclerFile,
} from "../../../helpers/analyzer-native-traffic";
import { TIMEOUT_SCALE } from "../../../helpers/timeouts";

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
    { waitUntil: "domcontentloaded" },
  );
  await expect(
    page.getByRole("button", { name: "Update shared mappings" }),
  ).toBeDisabled();
  if (mapping.confirmation.state !== "CURRENT") {
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
  } else {
    await expect(page.getByText("Current confirmation")).toBeVisible();
  }
}

async function expectClinicalReadback(
  page: Page,
  order: Awaited<ReturnType<typeof createAnalyzerClinicalOrder>>,
  expectedResult: string | RegExp,
  expectedUnits?: string,
) {
  const { accession, patientLastName, testId, specimenId } = order;
  await expect(async () => {
    const response = await page.request.get(
      `${API}/accession-results?accessionNumber=${encodeURIComponent(accession)}`,
    );
    expect(response.ok()).toBeTruthy();
    const data = (await response.json()) as {
      lastName: string;
      testResult: Array<{
        testId: string;
        resultValue: string;
        resultType: string;
        unitsOfMeasure: string;
        analysisId: string;
        testResultComponentId?: string;
        result?: { id?: string; testResult?: { componentId?: string } };
        dictionaryResults?: Array<{ id: string; value: string }>;
      }>;
    };
    expect(data.lastName).toBe(patientLastName);
    const results = data.testResult.filter(
      (result) => result.testId === testId,
    );
    expect(
      new Set(results.map((result) => result.analysisId)).size,
      `One ordered analysis for ${accession}`,
    ).toBe(1);
    const persisted = results.filter((result) => result.result?.id);
    expect(persisted, `One saved observation for ${accession}`).toHaveLength(1);
    const result = persisted[0];
    if (expectedUnits) {
      expect(result.resultType).toBe("N");
      expect(result.unitsOfMeasure).toBe(expectedUnits);
    }
    expect(
      result.result?.testResult?.componentId ?? null,
      "Saved result belongs to the catalog's primary component",
    ).toBe(order.primaryComponentId);
    for (const unreported of results.filter((item) => !item.result?.id)) {
      expect(
        unreported.resultValue ?? "",
        "Unreported components remain empty",
      ).toBe("");
    }
    let savedValue = result.resultValue;
    if (result.resultType === "D") {
      const choice = result.dictionaryResults?.find(
        (entry) => entry.id === result.resultValue,
      );
      expect(
        choice,
        `Saved dictionary choice ${result.resultValue}`,
      ).toBeDefined();
      savedValue = choice!.value;
    }
    if (typeof expectedResult === "string")
      expect(savedValue).toBe(expectedResult);
    else expect(savedValue).toMatch(expectedResult);
  }).toPass();

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
    {
      code: "HIV-VL",
      testName: "HIV Viral Load",
      loinc: "20447-9",
      raw: "1250",
      result: "1250",
      specimen: "Plasma",
      unit: "copies/mL",
      clinicalUnit: "copies/ml",
    },
    {
      code: "COVID19",
      testName: "COVID-19 PCR",
      loinc: "94500-6",
      raw: "POSITIVE",
      result: "SARS-CoV-2 RNA DETECTED",
      specimen: "Respiratory Swab",
    },
    {
      code: "COVID19",
      testName: "COVID-19 PCR",
      loinc: "94500-6",
      raw: "NEGATIVE",
      result: "SARS-COV-2 RNA NOT DETECTED",
      specimen: "Respiratory Swab",
    },
  ]) {
    test(`GeneXpert sends ${scenario.code} ${scenario.raw} to the correct patient order`, async ({
      page,
    }, testInfo) => {
      test.setTimeout(180_000 * TIMEOUT_SCALE);
      const presentation = createDemoPresentation(page, testInfo);
      await presentation.chapter({
        eyebrow: "OGC-1054 R0 · GeneXpert ASTM",
        title: "From analyzer setup to a clinical result",
        subtitle:
          "Confirm the shipped mapping, activate the connection, and receive an instrument result.",
        durationMs: 7000,
      });
      const runId = randomUUID().slice(0, 8);
      const analyzerName = `E2E GeneXpert ${runId}`;
      const senderId = `GX-${runId}`;
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
        expectedMappedValue: scenario.unit ? undefined : scenario.raw,
        specimenName: scenario.specimen || "Sputum",
      });
      await confirmShippedMapping(page, analyzer);
      await capture(page, testInfo, "gene-shipped-mapping-confirmed");
      await presentation.pause(4000);
      await page.goto(verifyUrl, {
        waitUntil: "domcontentloaded",
      });
      await setup.continueToConnect();
      await setup.fillSenderId(senderId);
      await page.getByRole("button", { name: "Finish and activate" }).click();
      const analyzerRow = page.getByTestId(`analyzer-row-${analyzer.id}`);
      await expect(analyzerRow).toContainText("Active");
      await capture(page, testInfo, "gene-connection-active");
      await presentation.chapter({
        eyebrow: "GeneXpert · Connection active",
        title: "The analyzer is ready",
        subtitle:
          "The saved connection is active and ready to receive ASTM traffic.",
        durationMs: 5000,
      });
      await presentation.pause(3000);

      await sendGeneXpertAstm(
        page.request,
        analyzer.bridgeConnectionId,
        order.accession,
        scenario.code,
        scenario.raw,
        senderId,
      );
      await expect
        .poll(async () => {
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
        })
        .toBe(true);
      await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
        waitUntil: "domcontentloaded",
      });
      const row = page.getByRole("row", {
        name: new RegExp(order.accession),
      });
      if (scenario.unit) {
        await expect(row.locator('input[id$=".result"]')).toHaveValue(
          /^1250(?:\.0+)?$/,
        );
        const response = await page.request.get(
          `${API}/AnalyzerResults?id=${analyzer.id}`,
        );
        const intake = await response.json();
        const received = intake.resultList.filter(
          (item: { accessionNumber: string }) =>
            item.accessionNumber === order.accession,
        );
        expect(received).toHaveLength(1);
        expect(received[0]).toMatchObject({
          testId: order.testId,
          testResultType: "N",
          units: scenario.unit,
        });
      } else {
        await expect(row).toContainText(scenario.result);
      }
      await capture(page, testInfo, "gene-received-result");
      await presentation.pause(3500);
      await row.locator('label[for$=".isAccepted"]').click();
      await page.getByRole("button", { name: "Save", exact: true }).click();
      await expect(row).not.toBeVisible();
      await expectClinicalReadback(
        page,
        order,
        scenario.unit ? /^1250(?:\.0+)?$/ : scenario.result,
        scenario.clinicalUnit,
      );
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const clinicalRow = page
        .getByRole("row", {
          name: new RegExp(order.accession),
        })
        .filter({ hasText: scenario.result });
      await expect(clinicalRow).toHaveCount(1);
      await expect(clinicalRow).toContainText(scenario.testName);
      await expect(clinicalRow).toContainText(scenario.result);
      await capture(page, testInfo, "gene-clinical-result-saved");
      await presentation.chapter({
        eyebrow: "GeneXpert · Verified result",
        title: "The result reached the ordered test",
        subtitle:
          "Playwright checked the patient, order, test, and clinical value.",
        durationMs: 6500,
      });
    });
  }

  test("Two GeneXpert instruments share one listener without mixing results", async ({
    page,
  }, testInfo) => {
    test.setTimeout(240_000 * TIMEOUT_SCALE);
    const runId = randomUUID().slice(0, 8);
    const instruments: Array<{
      analyzer: Analyzer;
      senderId: string;
      value: string;
      order: Awaited<ReturnType<typeof createAnalyzerClinicalOrder>>;
    }> = [];
    for (const [index, value] of ["DETECTED", "NOT DETECTED"].entries()) {
      await test.step(`Configure instrument ${index + 1} through the UI`, async () => {
        const name = `E2E Shared GeneXpert ${runId}-${index}`;
        const senderId = `GX-${runId}-${index}`;
        const list = new AnalyzerListPage(page);
        const setup = new AnalyzerSetupPage(page);
        await list.goto();
        await list.clickAdd();
        await setup.expectOpen();
        await setup.selectProfile("Cepheid GeneXpert (ASTM Mode)");
        await setup.fillName(name);
        await setup.selectLabUnit("Molecular Biology");
        await setup.continueToVerify();
        const verifyUrl = page.url();
        const analyzer = await analyzerByName(page, name, "genexpert-astm");
        const order = await createAnalyzerClinicalOrder(page, {
          profileId: analyzer.profileId,
          profileRevision: analyzer.profileRevision,
          sourceCode: "RIF",
          expectedTestName: "Xpert RIF Resistance",
          expectedLoinc: "46244-0",
          expectedMappedValue: value,
          specimenName: "Sputum",
        });
        await confirmShippedMapping(page, analyzer);
        await page.goto(verifyUrl, {
          waitUntil: "domcontentloaded",
        });
        await setup.continueToConnect();
        await setup.fillSenderId(senderId);
        await page.getByRole("button", { name: "Finish and activate" }).click();
        await expect(
          page.getByTestId(`analyzer-row-${analyzer.id}`),
        ).toContainText("Active");
        instruments.push({ analyzer, senderId, value, order });
      });
    }
    expect(
      new Set(instruments.map(({ analyzer }) => analyzer.bridgeConnectionId))
        .size,
    ).toBe(2);
    const destinations: string[] = [];
    await test.step("Send distinct results while both connections are active", async () => {
      for (const { analyzer, senderId, value, order } of instruments) {
        destinations.push(
          await sendGeneXpertAstm(
            page.request,
            analyzer.bridgeConnectionId,
            order.accession,
            "RIF",
            value,
            senderId,
          ),
        );
      }
      expect(
        new Set(destinations).size,
        "Both messages used the same host and listener port",
      ).toBe(1);
    });
    // Wait for both deliveries before asserting isolation; an absent result alone
    // would not distinguish correct routing from a message still in transit.
    for (const { analyzer, order } of instruments) {
      await expect(async () => {
        const response = await page.request.get(
          `${API}/AnalyzerResults?id=${analyzer.id}`,
        );
        expect(
          response.ok(),
          `Result readback: ${response.status()}`,
        ).toBeTruthy();
        const payload = await response.json();
        expect(
          (payload.resultList ?? []).filter(
            (result: { accessionNumber: string }) =>
              result.accessionNumber === order.accession,
          ),
        ).toHaveLength(1);
      }).toPass();
    }
    for (const { analyzer, value, order } of instruments) {
      await test.step(`Accept ${value} from ${analyzer.name} on its own order`, async () => {
        await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
          waitUntil: "domcontentloaded",
        });
        const ownRow = page.getByRole("row", {
          name: new RegExp(order.accession),
        });
        await expect(ownRow).toHaveCount(1);
        await expect(ownRow).toContainText(value);
        for (const other of instruments.filter(
          (item) => item.analyzer.id !== analyzer.id,
        )) {
          await expect(
            page.getByRole("row", {
              name: new RegExp(other.order.accession),
            }),
          ).toHaveCount(0);
        }
        await capture(
          page,
          testInfo,
          `shared-listener-${analyzer.id}-received`,
        );
        await ownRow.locator('label[for$=".isAccepted"]').click();
        await page.getByRole("button", { name: "Save", exact: true }).click();
        await expect(ownRow).not.toBeVisible();
        await expectClinicalReadback(page, order, value);
        await page.goto(
          `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
          {
            waitUntil: "domcontentloaded",
          },
        );
        const savedRow = page.getByRole("row", {
          name: new RegExp(order.accession),
        });
        await expect(savedRow).toHaveCount(1);
        await expect(savedRow).toContainText("Xpert RIF Resistance");
        await expect(savedRow).toContainText(value);
        await capture(page, testInfo, `shared-listener-${analyzer.id}-saved`);
      });
    }
  });

  test("GeneXpert accepts a usable result and recovers its held sibling", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const runId = randomUUID().slice(0, 8);
    const analyzerName = `E2E Recovery GeneXpert ${runId}`;
    const senderId = `GX-${runId}`;
    const knownValue = "NOT DETECTED";
    const rawValue = "RIF RESISTANCE INDETERMINATE";
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
      specimenName: "Sputum",
      expectedMappedValue: knownValue,
      additionalTests: [
        {
          profileId: analyzer.profileId,
          profileRevision: analyzer.profileRevision,
          sourceCode: "RIF",
          expectedTestName: "Xpert RIF Resistance",
          expectedLoinc: "46244-0",
          specimenName: "Sputum",
        },
      ],
    });
    await confirmShippedMapping(page, analyzer);
    await page.goto(verifyUrl, { waitUntil: "domcontentloaded" });
    await setup.continueToConnect();
    await setup.fillSenderId(senderId);
    await page.getByRole("button", { name: "Finish and activate" }).click();
    await expect(page.getByTestId(`analyzer-row-${analyzer.id}`)).toContainText(
      "Active",
    );

    await sendGeneXpertAstm(
      page.request,
      analyzer.bridgeConnectionId,
      order.accession,
      "MTB-RIF",
      knownValue,
      senderId,
      [{ testCode: "RIF", value: rawValue }],
    );
    let originalId: string | undefined;
    let knownId: string | undefined;
    await expect(async () => {
      const response = await page.request.get(
        `${API}/AnalyzerResults?id=${analyzer.id}`,
      );
      expect(response.ok()).toBeTruthy();
      const payload = (await response.json()) as {
        resultList: Array<{
          id: string;
          accessionNumber: string;
          rawTestCode: string;
          rawResultValue: string;
          importIssueReason: string | null;
        }>;
      };
      const rows = payload.resultList.filter(
        (row) => row.accessionNumber === order.accession,
      );
      expect(rows).toHaveLength(2);
      const known = rows.find((row) => row.rawTestCode === "MTB-RIF");
      const held = rows.find((row) => row.rawTestCode === "RIF");
      expect(known?.rawResultValue).toBe(knownValue);
      expect(known?.importIssueReason).toBeFalsy();
      expect(held?.rawResultValue).toBe(rawValue);
      expect(held?.importIssueReason).toBe("unknown_analyzer_result_value");
      knownId = known?.id;
      originalId = held?.id;
    }).toPass();
    expect(originalId).toBeTruthy();
    expect(knownId).toBeTruthy();

    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const held = page.getByTestId(`held-analyzer-result-${originalId}`);
    await expect(held).toContainText(rawValue);
    const known = page.getByRole("row").filter({
      has: page.locator(`[id="resultList${knownId}.isAccepted"]`),
    });
    await expect(known).toContainText(knownValue);
    await known.locator('label[for$=".isAccepted"]').click();
    await page.locator(`[id="resultList${knownId}.note"]`).fill("Reviewed");
    await capture(page, testInfo, "held-original-result");
    await held
      .getByRole("link", { name: "Review Analyzer Type mapping" })
      .click();
    const picker = page.getByRole("combobox", {
      name: `OpenELIS result for ${rawValue}`,
    });
    await expect(picker).toBeVisible();
    await picker.click();
    await page
      .getByRole("option", { name: "Indeterminate", exact: true })
      .click();
    await page.getByRole("button", { name: "Update shared mappings" }).click();
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled();
    await confirm.click();
    const apply = page.getByRole("button", {
      name: "Apply mappings and retry held results",
    });
    await expect(apply).toBeEnabled();
    await apply.click();
    await expect(
      page.getByText(
        "Current mappings applied to this analyzer. Eligible held results were retried.",
      ),
    ).toBeVisible();

    await expect(async () => {
      const response = await page.request.get(
        `${API}/AnalyzerResults?id=${analyzer.id}`,
      );
      expect(response.ok()).toBeTruthy();
      const payload = (await response.json()) as {
        resultList: Array<{ id: string; importIssueReason: string | null }>;
      };
      const original = payload.resultList.filter(
        (row) => row.id === originalId,
      );
      expect(original).toHaveLength(1);
      expect(original[0].importIssueReason).toBeFalsy();
    }).toPass();

    await page
      .locator(".analyzer-type-mapping__heading-actions")
      .getByRole("link", { name: "Analyzer Types" })
      .click();
    await expect(page).toHaveURL(/\/AnalyzerResults\?id=/);
    await expect(known.locator('[id$=".isAccepted"]')).toBeChecked();
    await expect(page.locator(`[id="resultList${knownId}.note"]`)).toHaveValue(
      "Reviewed",
    );
    const recovered = page.getByRole("row").filter({
      has: page.getByRole("cell", { name: "RIF", exact: true }),
    });
    await expect(recovered).toHaveCount(1);
    await expect(recovered).toContainText("Indeterminate");
    await capture(page, testInfo, "recovered-original-result");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(recovered).not.toBeVisible();
    await expect(known).not.toBeVisible();
    await expectClinicalReadback(page, order, knownValue);
    await expectClinicalReadback(
      page,
      {
        ...order,
        testId: order.orderedTests[1].testId,
        primaryComponentId: order.orderedTests[1].primaryComponentId,
      },
      "Indeterminate",
    );
    await page.goto(
      `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    const saved = page
      .getByRole("row", {
        name: new RegExp(order.accession),
      })
      .filter({ hasText: "Indeterminate" });
    await expect(saved).toHaveCount(1);
    await expect(saved).toContainText("Xpert RIF Resistance");
    await expect(saved).toContainText("Indeterminate");
    const savedKnown = page
      .getByRole("row", { name: new RegExp(order.accession) })
      .filter({ hasText: knownValue });
    await expect(savedKnown).toHaveCount(1);
    await expect(savedKnown).toContainText("Xpert MTB/RIF");
    await capture(page, testInfo, "recovered-clinical-result-saved");
  });

  test("FluoroCycler imports a watched file for the correct clinical orders", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const presentation = createDemoPresentation(page, testInfo);
    await presentation.chapter({
      eyebrow: "OGC-1054 R0 · FluoroCycler FILE",
      title: "From watched directory to clinical results",
      subtitle:
        "Configure file transport, import two orders, and verify each saved result.",
      durationMs: 6500,
    });
    const runId = randomUUID().slice(0, 8);
    const analyzerName = `E2E FluoroCycler ${runId}`;
    const directory = `/data/analyzer-imports/fluorocycler-xt/incoming/${runId}`;
    const values = ["1250", "450"];
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);
    await list.goto();
    await list.clickAdd();
    await setup.selectProfile("Bruker FluoroCycler XT");
    await setup.fillName(analyzerName);
    await setup.selectLabUnit("Molecular Biology");
    await setup.continueToVerify();
    const verifyUrl = page.url();
    const analyzer = await analyzerByName(
      page,
      analyzerName,
      "fluorocycler-xt",
    );
    const orders: Array<
      Awaited<ReturnType<typeof createAnalyzerClinicalOrder>>
    > = [];
    for (const _value of values) {
      orders.push(
        await createAnalyzerClinicalOrder(page, {
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
    await presentation.pause(3000);
    await page.goto(verifyUrl, {
      waitUntil: "domcontentloaded",
    });
    await setup.continueToConnect();
    await setup.fillImportDirectory(directory);
    await page.getByRole("button", { name: "Finish and activate" }).click();
    await expect(page.getByTestId(`analyzer-row-${analyzer.id}`)).toContainText(
      "Active",
    );
    await capture(page, testInfo, "file-watch-directory-configured");
    await presentation.chapter({
      eyebrow: "FluoroCycler · FILE transport",
      title: "The watched directory is configured",
      subtitle:
        "Bridge owns the directory watch and imports the instrument workbook.",
      durationMs: 5000,
    });
    await presentation.pause(3000);
    const emitted = await writeFluoroCyclerFile(
      page.request,
      directory,
      orders.map((order) => order.accession),
    );
    expect(emitted).toHaveLength(2);
    for (const [index, order] of orders.entries()) {
      expect(emitted[index]).toMatchObject({
        sampleId: order.accession,
        result: values[index],
      });
    }
    await expect
      .poll(async () => {
        const response = await page.request.get(
          `${API}/AnalyzerResults?id=${analyzer.id}`,
        );
        if (!response.ok()) return false;
        const data = await response.json();
        return orders.every((order) =>
          data.resultList?.some(
            (row: { accessionNumber: string }) =>
              row.accessionNumber === order.accession,
          ),
        );
      })
      .toBe(true);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    for (const [index, order] of orders.entries()) {
      const row = page.getByRole("row", {
        name: new RegExp(order.accession),
      });
      await expect(row.locator('input[id$=".result"]')).toHaveValue(
        new RegExp(`^${values[index]}(?:\\.0+)?$`),
      );
      await row.locator('label[for$=".isAccepted"]').click();
    }
    await capture(page, testInfo, "file-received-results");
    await presentation.pause(2000);
    await page.getByRole("button", { name: "Save", exact: true }).click();
    for (const [index, order] of orders.entries()) {
      await expectClinicalReadback(
        page,
        order,
        new RegExp(`^${values[index]}(?:\\.0+)?$`),
        "copies/ml",
      );
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = page.getByRole("row", {
        name: new RegExp(order.accession),
      });
      await expect(row).toContainText("HIV Viral Load");
      await expect(row).toContainText(values[index]);
      await capture(page, testInfo, `file-clinical-result-${index + 1}-saved`);
      await presentation.pause(1500);
    }
    await presentation.chapter({
      eyebrow: "FluoroCycler · Verified results",
      title: "Both orders have clinical results",
      subtitle:
        "The two workbook results were read back against their separate patient orders.",
      durationMs: 6000,
    });
  });
});
