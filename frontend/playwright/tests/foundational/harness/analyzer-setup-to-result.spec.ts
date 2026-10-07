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
import {
  sendGeneXpertFixture,
  writeFluoroCyclerFile,
  writeResultsFile,
} from "../../../helpers/analyzer-native-traffic";
import {
  FLUOROCYCLER,
  QUANTSTUDIO,
  activateShippedAnalyzer,
  activateShippedGeneXpert,
} from "../../../helpers/analyzer-setup-flow";
import { csrfToken, withAuthedPage } from "../../../helpers/api-session";
import { API } from "../../../helpers/analyzer-profile-api";
import {
  acceptAll,
  savedValue,
  type Order,
} from "../../../helpers/analyzer-review";

test.describe("A GeneXpert from setup to a clinical result", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-RES-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(180_000);
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
    await acceptAll(page, analyzer, [order.accession]);
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
    await acceptAll(page, analyzer, [order.accession]);

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
      await acceptAll(page, instrument.analyzer, [orders[index].accession]);
      await expect
        .poll(() => savedValue(page, orders[index], testId, "HIV-1 viral load"))
        .toBe(instrument.value);
    }
  });
});

test.describe("A FluoroCycler from setup to a clinical result", () => {
  test("a results file in the watched folder reaches each order and is accepted", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const directory = `/data/analyzer-imports/fluorocycler-xt/incoming/${run}`;
    const analyzer = await activateShippedAnalyzer(
      page,
      FLUOROCYCLER,
      `Results FluoroCycler ${run}`,
      { importDirectory: directory },
    );
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    const orders: Order[] = [];
    for (let index = 0; index < 2; index += 1) {
      orders.push(
        await createClinicalOrder(page, {
          testIds: [testId],
          specimenName: "Plasma",
        }),
      );
    }
    const emitted = await writeFluoroCyclerFile(
      page.request,
      directory,
      orders.map((order) => order.accession),
    );
    expect(emitted).toHaveLength(orders.length);
    for (const order of orders) {
      await expect
        .poll(
          async () =>
            (await worklistFor<WorklistRow>(page, analyzer.id, order.accession))
              .length,
        )
        .toBeGreaterThan(0);
    }
    await acceptAll(
      page,
      analyzer,
      orders.map((order) => order.accession),
    );
    for (const [index, order] of orders.entries()) {
      await expect
        .poll(async () =>
          Number(await savedValue(page, order, testId, "HIV-1 viral load")),
        )
        .toBe(Number(emitted[index].result));
    }
  });
});

test.describe("A catalog test deactivated after setup", () => {
  let deactivated: string | null = null;
  // The catalog is shared; a story that turns a test off puts it back even when it fails.
  test.afterEach(async ({ page }) => {
    if (!deactivated) return;
    const testId = deactivated;
    deactivated = null;
    const restored = await page.request.post(
      `${API}/test-catalog/tests/${testId}/activate`,
      { headers: { "X-CSRF-Token": await csrfToken(page) }, data: {} },
    );
    expect(restored.ok(), `Reactivate ${testId}`).toBeTruthy();
  });

  test("its result is held while the rest are accepted, then recovers once the test is active again", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const senderId = `GX-OFF-${run}`;
    const specimen = "Nasopharyngeal Swab";
    const analyzer = await activateShippedGeneXpert(
      page,
      `Deactivation GeneXpert ${run}`,
      senderId,
    );
    const tests = {
      sars: await activeTestId(page, "SARS-CoV-2 PCR", specimen),
      fluA: await activeTestId(page, "Influenza A PCR", specimen),
      fluB: await activeTestId(page, "Influenza B PCR", specimen),
    };
    const order = await createClinicalOrder(page, {
      testIds: Object.values(tests),
      specimenName: specimen,
    });

    // The lab retires Influenza B after the analyzer was set up.
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const off = await page.request.put(
      `${API}/test-catalog/tests/${tests.fluB}/basic-info`,
      { headers, data: { active: false } },
    );
    expect(off.ok(), `Deactivate Influenza B: ${off.status()}`).toBeTruthy();
    deactivated = tests.fluB;

    await sendGeneXpertFixture(
      page.request,
      analyzer.bridgeConnectionId,
      order.accession,
      { assay: "cov-flu-plus", outcome: "flu-b-positive" },
      senderId,
    );
    type Row = WorklistRow & { componentId?: string | null };
    const own = async (code: string) =>
      (await worklistFor<Row>(page, analyzer.id, order.accession)).find(
        (row) => row.rawTestCode === code && !row.componentId,
      );
    await expect
      .poll(async () => (await own("FLUB"))?.importIssueReason)
      .toBe("test_mapping_not_ready");
    expect((await own("FLUA"))?.importIssueReason).toBeFalsy();

    // The usable results are accepted; the held one stays on the screen.
    const fluBRow = (await own("FLUB"))!;
    await acceptAll(page, analyzer, [order.accession]);
    await expect
      .poll(() => savedValue(page, order, tests.fluA, "Flu A 1"))
      .toBe("Negative");
    await expect
      .poll(() => savedValue(page, order, tests.sars, "SARS-CoV-2"))
      .toBe("Negative");
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const held = page.getByTestId(`held-analyzer-result-${fluBRow.id}`);
    await expect(held).toBeVisible();

    // Influenza B is active again; the held observation is retried, not resent.
    const on = await page.request.post(
      `${API}/test-catalog/tests/${tests.fluB}/activate`,
      { headers, data: {} },
    );
    expect(on.ok(), `Reactivate Influenza B: ${on.status()}`).toBeTruthy();
    deactivated = null;
    await held.getByRole("link", { name: "Review analyzer mapping" }).click();
    const apply = page.getByRole("button", {
      name: "Apply mappings and retry held results",
    });
    await expect(apply).toBeEnabled();
    await apply.click();
    await expect
      .poll(async () => (await own("FLUB"))?.importIssueReason)
      .toBeFalsy();

    await acceptAll(page, analyzer, [order.accession]);
    await expect
      .poll(() => savedValue(page, order, tests.fluB, "Flu B"))
      .toBe("Positive");
  });
});

test.describe("A QuantStudio from setup to a clinical result", () => {
  test("a results workbook in the watched folder reaches each order and is accepted", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const directory = `/data/analyzer-imports/quantstudio/incoming/${run}`;
    const analyzer = await activateShippedAnalyzer(
      page,
      QUANTSTUDIO,
      `Results QuantStudio ${run}`,
      { importDirectory: directory },
    );
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    const orders: Order[] = [];
    for (let index = 0; index < 2; index += 1) {
      orders.push(
        await createClinicalOrder(page, {
          testIds: [testId],
          specimenName: "Plasma",
        }),
      );
    }
    // The workbook has six result rows: three samples nobody ordered here and a
    // positive control ride along with the two that were.
    const unordered = (index: number) => `UNORDERED-${run}-${index}`;
    const emitted = await writeResultsFile(
      page.request,
      "quantstudio7",
      directory,
      [
        orders[0].accession,
        orders[1].accession,
        unordered(3),
        "CPOS",
        unordered(5),
        unordered(7),
      ],
    );
    for (const order of orders) {
      await expect
        .poll(
          async () =>
            (await worklistFor<WorklistRow>(page, analyzer.id, order.accession))
              .length,
        )
        .toBeGreaterThan(0);
    }
    await acceptAll(
      page,
      analyzer,
      orders.map((order) => order.accession),
    );
    for (const [index, order] of orders.entries()) {
      // A viral load is saved in whole copies.
      await expect
        .poll(async () =>
          Number(await savedValue(page, order, testId, "HIV-1 viral load")),
        )
        .toBe(Math.round(Number(emitted[index].result)));
    }
  });
});

test.describe("An instrument that sends its own test code", () => {
  test("the code set in the Assays step is the code results arrive under, and they land on the right test", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const senderId = `GX-CODE-${run}`;
    // The lab's instrument is configured to send HIVU where the profile says HIVVL.
    const analyzer = await activateShippedGeneXpert(
      page,
      `Own codes GeneXpert ${run}`,
      senderId,
      { instrumentCodes: { HIVVL: "HIVU" } },
    );
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
      { HIVVL: "HIVU" },
    );
    type Row = WorklistRow & { componentId?: string | null };
    const own = async () =>
      (await worklistFor<Row>(page, analyzer.id, order.accession)).find(
        (row) => !row.componentId,
      );
    // The Bridge translates the lab's code back to the profile's, so the result binds as usual.
    await expect.poll(async () => (await own())?.testId).toBe(testId);
    expect((await own())?.importIssueReason).toBeFalsy();
    await acceptAll(page, analyzer, [order.accession]);
    await expect
      .poll(() => savedValue(page, order, testId, "HIV-1 viral load"))
      .toBe("1010");
  });
});
