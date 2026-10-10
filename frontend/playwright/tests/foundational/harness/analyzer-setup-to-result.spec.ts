import { randomUUID } from "node:crypto";
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
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test.describe("A GeneXpert from setup to a clinical result", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-RES-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedGeneXpert(page, `Results GeneXpert ${run}`, senderId),
    );
  });

  test("an HIV-1 viral load reaches its order and is accepted as a clinical result", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "GeneXpert: an HIV-1 viral load becomes a clinical result",
      "The GeneXpert was set up and activated on the shipped profile before this clip.",
    );
    await demo.caption(
      "Off screen: a patient order for HIV-1 Viral Load on plasma is created.",
    );
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    const order = await createClinicalOrder(page, {
      testIds: [testId],
      specimenName: "Plasma",
    });
    await demo.caption(
      `The mock GeneXpert sends its ASTM result for ${order.accession} through the Bridge.`,
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
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
    await demo.caption(
      "The result waits on the analyzer's review screen. The reviewer accepts it and saves.",
    );
    await acceptAll(page, analyzer, [order.accession], { demo });
    await expect
      .poll(() => savedValue(page, order, testId, "HIV-1 viral load"))
      .toBe("1010");

    await demo.caption("The order's Results screen now holds the value.");
    await page.goto(
      `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    const clinicalRow = page.getByRole("row", {
      name: new RegExp(order.accession),
    });
    await expect(clinicalRow.first()).toContainText("HIV-1 Viral Load");
    await expect(clinicalRow.first()).toContainText("1010");
    await demo.highlight(clinicalRow.first());
    await demo.verified(
      `HIV-1 viral load 1010 is saved on ${order.accession}`,
      "Read back from the order's saved results, the same data the Results screen shows.",
    );
  });

  test("a respiratory panel lands on each of its tests and components, each accepted as a clinical result", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "GeneXpert: one respiratory panel run, four tests",
      "SARS-CoV-2, influenza A and B, RSV and the sample processing control each land on their own test.",
    );
    await demo.caption(
      "Off screen: one nasopharyngeal swab is ordered for the four tests.",
    );
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
    await demo.caption(
      "The mock GeneXpert sends one CoV-2/Flu/RSV plus run: SARS-CoV-2 positive.",
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
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
    await demo.caption(
      "Each test is its own row, with its parts beneath it. The reviewer accepts them and saves.",
    );
    await acceptAll(page, analyzer, [order.accession], { demo });

    // The sample processing control rides on the test as a component of its own.
    const expected = [
      { testId: tests.sars, component: "SARS-CoV-2", value: "Positive" },
      { testId: tests.fluA, component: "Flu A 1", value: "Negative" },
      { testId: tests.fluB, component: "Flu B", value: "Negative" },
      { testId: tests.rsv, component: "RSV", value: "Negative" },
      {
        testId: tests.sars,
        component: "Sample processing control",
        value: "Not applicable",
      },
    ];
    for (const { testId, component, value } of expected) {
      await expect
        .poll(() => savedValue(page, order, testId, component))
        .toBe(value);
    }
    await demo.verified(
      `Every part of the panel is saved on ${order.accession}`,
      expected.map((row) => `${row.component}: ${row.value}`).join(" · "),
    );
  });

  test("two GeneXperts on one listener each keep their own results", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Two GeneXperts on one listener keep their own results",
      "The Bridge tells them apart by the system name each instrument sends.",
    );
    await demo.caption(
      "A second GeneXpert is set up with its own system name, on the same listener as the first.",
    );
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
    await demo.caption(
      "Each instrument sends an HIV-1 viral load for its own order: 1010 from the first, <40 from the second.",
    );
    const orders: Order[] = [];
    for (const instrument of instruments) {
      const order = await createClinicalOrder(page, {
        testIds: [testId],
        specimenName: "Plasma",
      });
      orders.push(order);
      await sendGeneXpertFixture(
        page,
        instrument.analyzer.id,
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
      await demo.caption(
        `Analyzer ${index + 1}'s review screen holds only its own result.`,
      );
      await acceptAll(page, instrument.analyzer, [orders[index].accession], {
        demo,
      });
      await expect
        .poll(() => savedValue(page, orders[index], testId, "HIV-1 viral load"))
        .toBe(instrument.value);
    }
    await demo.verified(
      "Each result is saved on its own order",
      instruments
        .map(
          (instrument, index) =>
            `GeneXpert ${index + 1}: ${instrument.value} on ${orders[index].accession}`,
        )
        .join(" · "),
    );
  });
});

test.describe("A FluoroCycler from setup to a clinical result", () => {
  test("a results file in the watched folder reaches each order and is accepted", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "FluoroCycler: a results file becomes clinical results",
      "The Bridge watches the folder named in setup and reads the file. OpenELIS never opens it.",
    );
    const run = randomUUID().slice(0, 8);
    const directory = `/data/analyzer-imports/fluorocycler-xt/incoming/${run}`;
    await demo.caption(
      "Set up the FluoroCycler on its shipped profile, with the folder the Bridge watches.",
    );
    const analyzer = await activateShippedAnalyzer(
      page,
      FLUOROCYCLER,
      `Results FluoroCycler ${run}`,
      { importDirectory: directory },
    );
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    await demo.caption(
      "Off screen: two plasma orders are created, and the instrument's results file for both lands in the folder.",
    );
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
    await demo.caption(
      "Both results wait on the review screen. The reviewer accepts them and saves.",
    );
    await acceptAll(
      page,
      analyzer,
      orders.map((order) => order.accession),
      { demo },
    );
    for (const [index, order] of orders.entries()) {
      await expect
        .poll(async () =>
          Number(await savedValue(page, order, testId, "HIV-1 viral load")),
        )
        .toBe(Number(emitted[index].result));
    }
    await demo.verified(
      "Both viral loads in the file are saved on their orders",
      orders
        .map((order, index) => `${order.accession}: ${emitted[index].result}`)
        .join(" · "),
    );
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
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "A catalog test turned off after setup",
      "Its result is held while the rest are saved, and recovers once the test is back on.",
    );
    const run = randomUUID().slice(0, 8);
    const senderId = `GX-OFF-${run}`;
    const specimen = "Nasopharyngeal Swab";
    await demo.caption("Set up a GeneXpert on the shipped profile.");
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
    await demo.caption(
      "Off screen: the lab turns Influenza B PCR off in its test catalog.",
    );
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const off = await page.request.put(
      `${API}/test-catalog/tests/${tests.fluB}/basic-info`,
      { headers, data: { active: false } },
    );
    expect(off.ok(), `Deactivate Influenza B: ${off.status()}`).toBeTruthy();
    deactivated = tests.fluB;

    await demo.caption(
      "The GeneXpert sends a CoV-2/Flu panel for the order: influenza B positive.",
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
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
    await demo.caption(
      "Influenza A and SARS-CoV-2 are accepted and saved. Influenza B is held: it has no box to accept.",
    );
    await acceptAll(page, analyzer, [order.accession], { demo });
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
    await demo.caption("The held Influenza B result stays on the screen.");
    await demo.highlight(held);

    // Influenza B is active again; the held observation is retried, not resent.
    await demo.caption(
      "Off screen: Influenza B PCR is turned back on. The reviewer applies the mapping and retries.",
    );
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

    await demo.caption(
      "The same result, not a resend, is now ready. The reviewer accepts it.",
    );
    await acceptAll(page, analyzer, [order.accession], { demo });
    await expect
      .poll(() => savedValue(page, order, tests.fluB, "Flu B"))
      .toBe("Positive");
    await demo.verified(
      `Influenza B Positive is saved on ${order.accession}`,
      "Influenza A and SARS-CoV-2 were saved first; the held result was retried without a resend.",
    );
  });
});

test.describe("A QuantStudio from setup to a clinical result", () => {
  test("a results workbook in the watched folder reaches each order and is accepted", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "QuantStudio: a results workbook with rows nobody ordered",
      "Two ordered samples are saved; unordered samples and a positive control ride along.",
    );
    const run = randomUUID().slice(0, 8);
    const directory = `/data/analyzer-imports/quantstudio/incoming/${run}`;
    await demo.caption(
      "Set up the QuantStudio on its shipped profile, with the folder the Bridge watches.",
    );
    const analyzer = await activateShippedAnalyzer(
      page,
      QUANTSTUDIO,
      `Results QuantStudio ${run}`,
      { importDirectory: directory },
    );
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    await demo.caption(
      "Off screen: two plasma orders are created, and a six-row workbook lands in the folder.",
    );
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
    await demo.caption(
      "The reviewer accepts the two ordered results and saves.",
    );
    await acceptAll(
      page,
      analyzer,
      orders.map((order) => order.accession),
      { demo },
    );
    const saved: number[] = [];
    for (const [index, order] of orders.entries()) {
      // A viral load is saved in whole copies.
      const copies = Math.round(Number(emitted[index].result));
      await expect
        .poll(async () =>
          Number(await savedValue(page, order, testId, "HIV-1 viral load")),
        )
        .toBe(copies);
      saved.push(copies);
    }
    await demo.verified(
      "Both ordered viral loads are saved, in whole copies",
      orders
        .map((order, index) => `${order.accession}: ${saved[index]}`)
        .join(" · "),
    );
  });
});
