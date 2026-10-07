import { randomUUID } from "node:crypto";
import type { Page } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import {
  worklistFor,
  type Analyzer,
  type WorklistRow,
} from "../../../helpers/analyzer-api";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { API } from "../../../helpers/analyzer-profile-api";

type Order = Awaited<ReturnType<typeof createClinicalOrder>>;

/**
 * What the lab sees saved for one component of an ordered test, read as the
 * Results screen does. The screen names a component after the test.
 */
async function savedValue(
  page: Page,
  order: Order,
  testId: string,
  component: string,
) {
  const response = await page.request.get(
    `${API}/accession-results?accessionNumber=${encodeURIComponent(order.accession)}`,
  );
  expect(response.ok()).toBeTruthy();
  const data = (await response.json()) as {
    lastName: string;
    testResult: Array<{
      testId: string;
      testName: string;
      resultValue: string;
      resultType: string;
      result?: { id?: string };
      dictionaryResults?: Array<{ id: string; value: string }>;
    }>;
  };
  expect(data.lastName).toBe(order.patientLastName);
  const saved = data.testResult.filter(
    (row) =>
      row.testId === testId &&
      row.testName.endsWith(` — ${component}`) &&
      row.result?.id,
  );
  expect(
    saved.length,
    `Saved ${component} for ${order.accession}`,
  ).toBeGreaterThan(0);
  // A test's own result and an analyte can share a label; they carry the same call.
  const values = saved.map((row) =>
    row.resultType === "D"
      ? row.dictionaryResults?.find((entry) => entry.id === row.resultValue)
          ?.value
      : row.resultValue,
  );
  return [...new Set(values)].join(" | ");
}

/** Accept every row the analyzer sent for this accession, as the reviewer does, and save. */
async function acceptAll(page: Page, analyzer: Analyzer, accession: string) {
  await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
    waitUntil: "domcontentloaded",
  });
  const rows = page.getByRole("row", { name: new RegExp(accession) });
  await expect(rows.first()).toBeVisible();
  const accept = rows.locator('label[for$=".isAccepted"]');
  for (let index = 0; index < (await accept.count()); index += 1) {
    await accept.nth(index).click();
  }
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(rows).toHaveCount(0);
}

test.describe("A GeneXpert from setup to a clinical result", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-RES-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(180_000);
    const { withAuthedPage } = await import("../../../helpers/api-session");
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedGeneXpert(page, `Results GeneXpert ${run}`, senderId),
    );
  });

  test("an HIV-1 viral load reaches its order and is accepted as a clinical result", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    const order = await createClinicalOrder(page, {
      testIds: [testId],
      specimenName: "Plasma",
    });
    await sendGeneXpertFixture(
      page.request,
      analyzer.bridgeConnectionId,
      order.accession,
      { assay: "hivvl", outcome: "quantified" },
      senderId,
    );
    await expect
      .poll(
        async () =>
          (await worklistFor<WorklistRow>(page, analyzer.id, order.accession))
            .length,
      )
      .toBeGreaterThan(0);
    await acceptAll(page, analyzer, order.accession);
    await expect
      .poll(() => savedValue(page, order, testId, "HIV-1 viral load"))
      .toBe("1010");

    await page.goto(
      `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    const clinicalRow = page.getByRole("row", {
      name: new RegExp(order.accession),
    });
    await expect(clinicalRow.first()).toContainText("HIV-1 Viral Load");
    await expect(clinicalRow.first()).toContainText("1010");
  });

  test("a respiratory panel lands on each of its tests and components, each accepted as a clinical result", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const specimen = "Nasopharyngeal Swab";
    const tests = {
      sars: await activeTestId(page, "SARS-CoV-2 PCR", specimen),
      fluA: await activeTestId(page, "Influenza A PCR", specimen),
      fluB: await activeTestId(page, "Influenza B PCR", specimen),
      rsv: await activeTestId(page, "RSV PCR", specimen),
    };
    const order = await createClinicalOrder(page, {
      testIds: Object.values(tests),
      specimenName: specimen,
    });
    await sendGeneXpertFixture(
      page.request,
      analyzer.bridgeConnectionId,
      order.accession,
      { assay: "cov-flu-rsv-plus", outcome: "sars-cov-2-positive" },
      senderId,
    );
    await expect
      .poll(
        async () =>
          (await worklistFor<WorklistRow>(page, analyzer.id, order.accession))
            .length,
      )
      .toBeGreaterThan(0);
    await acceptAll(page, analyzer, order.accession);

    const saved = (testId: string, component: string) =>
      savedValue(page, order, testId, component);
    await expect.poll(() => saved(tests.sars, "SARS-CoV-2")).toBe("Positive");
    await expect.poll(() => saved(tests.fluA, "Flu A 1")).toBe("Negative");
    await expect.poll(() => saved(tests.fluB, "Flu B")).toBe("Negative");
    await expect.poll(() => saved(tests.rsv, "RSV")).toBe("Negative");
    // The sample processing control rides on the test as a component of its own.
    await expect
      .poll(() => saved(tests.sars, "Sample processing control"))
      .toBe("Not applicable");
  });

  test("two GeneXperts on one listener each keep their own results", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const secondSender = `GX-RES-2-${run}`;
    const second = await activateShippedGeneXpert(
      page,
      `Second GeneXpert ${run}`,
      secondSender,
    );
    expect(second.bridgeConnectionId).not.toBe(analyzer.bridgeConnectionId);
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    const instruments = [
      { analyzer, senderId, outcome: "quantified", value: "1010" },
      {
        analyzer: second,
        senderId: secondSender,
        outcome: "below-40",
        value: "<40",
      },
    ];
    const orders: Order[] = [];
    for (const instrument of instruments) {
      const order = await createClinicalOrder(page, {
        testIds: [testId],
        specimenName: "Plasma",
      });
      orders.push(order);
      await sendGeneXpertFixture(
        page.request,
        instrument.analyzer.bridgeConnectionId,
        order.accession,
        { assay: "hivvl", outcome: instrument.outcome },
        instrument.senderId,
      );
    }
    for (const [index, instrument] of instruments.entries()) {
      await expect
        .poll(
          async () =>
            (
              await worklistFor<WorklistRow>(
                page,
                instrument.analyzer.id,
                orders[index].accession,
              )
            ).length,
        )
        .toBeGreaterThan(0);
      // The other instrument's order never shows up here.
      const other = orders[1 - index];
      expect(
        await worklistFor<WorklistRow>(
          page,
          instrument.analyzer.id,
          other.accession,
        ),
      ).toHaveLength(0);
    }
    for (const [index, instrument] of instruments.entries()) {
      await acceptAll(page, instrument.analyzer, orders[index].accession);
      await expect
        .poll(() => savedValue(page, orders[index], testId, "HIV-1 viral load"))
        .toBe(instrument.value);
    }
  });
});
