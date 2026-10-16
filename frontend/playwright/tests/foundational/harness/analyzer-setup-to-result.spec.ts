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
  sendBc5380Fixture,
  sendGeneXpertFixture,
  writeFluoroCyclerFile,
  writeResultsFile,
} from "../../../helpers/analyzer-native-traffic";
import {
  activateShippedAnalyzer,
  activateShippedBc5380,
  activateShippedGeneXpert,
} from "../../../helpers/analyzer-setup-flow";
import { FLUOROCYCLER, QUANTSTUDIO } from "../../../helpers/shipped-profiles";
import { csrfToken, withAuthedPage } from "../../../helpers/api-session";
import { API } from "../../../helpers/analyzer-profile-api";
import {
  acceptAll,
  savedValue,
  showSample,
  type Order,
} from "../../../helpers/analyzer-review";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  applyMappings,
  confirmMapping,
  doNotReceive,
  saveMapping,
} from "../../../helpers/analyzer-mapping-page";
import {
  openFirstWaiting,
  openValidationQueue,
  releaseAllClear,
  releaseOpen,
  validateAndReleaseFirst,
} from "../../../helpers/validation-queue";

// What the mock's quantified HIV-1 viral load fixture saves on its order: the
// value and each part reported with a value. The order also carries the test's
// "HIV-1 call" part, which the fixture leaves empty. The internal quantitation
// standards (IQS-H, IQS-L) are the cartridge's own check, a flag on the sample.
const HIV_VL_VALUED_PARTS = [
  { part: "HIV-1 viral load", value: "1010" },
  { part: "Log viral load", value: "3.00" },
  { part: "HIV-1", value: "Positive" },
  { part: "HIV-1 Ct", value: "33.0" },
  { part: "HIV-1 EndPt", value: "773.0" },
  { part: "HIV-1 Delta Ct", value: "-6.2" },
];

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test.describe("A GeneXpert from setup to a clinical result", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-RES-${run}`;
  const secondSender = `GX-RES-2-${run}`;
  let analyzer: Analyzer;
  let second: Analyzer;

  test.beforeAll(async ({ browser }) => {
    await withAuthedPage(browser, async (page) => {
      analyzer = await activateShippedGeneXpert(
        page,
        `Results GeneXpert ${run}`,
        senderId,
      );
      second = await activateShippedGeneXpert(
        page,
        `Second GeneXpert ${run}`,
        secondSender,
      );
    });
  });

  test(
    "an HIV-1 viral load reaches its order, is accepted and is released after Validation",
    { tag: ["@evidence", "@astm", "@results"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "GeneXpert: an HIV-1 viral load to a released result",
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
      await expect(clinicalRow.first()).toContainText(analyzer.name);
      await demo.highlight(clinicalRow.first());

      await demo.caption(
        "In Validation, the viral load and its parts need review: the catalog gives them no reference range.",
      );
      await openValidationQueue(page, order.accession);
      const summary = page.getByTestId("validation-lane-summary");
      await expect(summary).toContainText("Clear: 0");
      await expect(summary).toContainText(
        `Needs review: ${HIV_VL_VALUED_PARTS.length}`,
      );
      await demo.highlight(summary);
      await demo.caption(
        "The parts are one analysis, so releasing the viral load releases every part with it.",
      );
      await openFirstWaiting(page);
      await releaseOpen(page);
      await expect(
        page.getByTestId("release-all-clear-why-queueEmpty"),
      ).toBeVisible();

      await demo.caption("Every part of the viral load is final on the order.");
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const final = page.getByRole("button", {
        name: `Results final (${HIV_VL_VALUED_PARTS.length + 1})`,
      });
      await expect(final).toBeVisible();
      for (const { part, value } of HIV_VL_VALUED_PARTS) {
        const row = page.getByRole("row", {
          name: new RegExp(`— ${escapeRegExp(part)} — `),
        });
        await expect(row, part).toContainText(value);
        await expect(row, part).toContainText("Results final");
      }
      await demo.highlight(final);
      await demo.verified(
        `HIV-1 viral load 1010 on ${order.accession} is released`,
        "Accepted from the analyzer, reviewed in Validation and final on the order.",
      );
    },
  );

  test(
    "a respiratory panel lands on each of its tests and components, each accepted as a clinical result",
    { tag: ["@evidence", "@astm", "@results"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "GeneXpert: one respiratory panel run, four tests",
        "SARS-CoV-2, influenza A and B and RSV each land on their own test. The sample processing control is the cartridge's own check, not a result.",
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

      const expected = [
        { testId: tests.sars, component: "SARS-CoV-2", value: "Positive" },
        { testId: tests.fluA, component: "Flu A 1", value: "Negative" },
        { testId: tests.fluB, component: "Flu B", value: "Negative" },
        { testId: tests.rsv, component: "RSV", value: "Negative" },
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
    },
  );

  test(
    "two GeneXperts on one listener each keep their own results",
    { tag: ["@evidence", "@astm", "@results"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Two GeneXperts on one listener keep their own results",
        "The Bridge tells them apart by the system name each instrument sends.",
      );
      await demo.caption(
        "Before this clip, a second GeneXpert was set up with its own system name, on the same listener as the first.",
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
          .poll(() =>
            savedValue(page, orders[index], testId, "HIV-1 viral load"),
          )
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
    },
  );
});

test.describe("A FluoroCycler from setup to a clinical result", () => {
  const run = randomUUID().slice(0, 8);
  const directory = `/data/analyzer-imports/fluorocycler-xt/incoming/${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedAnalyzer(
        page,
        FLUOROCYCLER,
        `Results FluoroCycler ${run}`,
        {
          importDirectory: directory,
        },
      ),
    );
  });

  test(
    "a results file in the watched folder reaches each order and is accepted",
    { tag: ["@evidence", "@file", "@results"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "FluoroCycler: a results file becomes clinical results",
        "The Bridge watches the folder named in setup and reads the file. OpenELIS never opens it.",
      );
      await demo.caption(
        "The FluoroCycler was set up on its shipped profile before this clip, with the folder the Bridge watches.",
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
      // The workbook writes two rows per sample: the viral load on channel 1 and the
      // internal control on channel 2. The profile reads channel 2 as a warning about
      // the sample, so a valid control leaves one row per order and nothing held.
      for (const order of orders) {
        await expect
          .poll(
            async () =>
              (
                await worklistFor<WorklistRow>(
                  page,
                  analyzer.id,
                  order.accession,
                )
              ).length,
          )
          .toBeGreaterThan(0);
        const rows = await worklistFor<WorklistRow>(
          page,
          analyzer.id,
          order.accession,
        );
        expect(rows.map((row) => row.importIssueReason ?? null)).toEqual([
          null,
        ]);
      }
      await demo.caption(
        "One row per order waits on the review screen: the file's internal-control rows are the instrument's word on each sample, not results. The reviewer accepts both and saves.",
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
    },
  );
});

test.describe("A catalog test deactivated after setup", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-OFF-${run}`;
  let analyzer: Analyzer;
  let deactivated: string | null = null;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedGeneXpert(page, `Deactivation GeneXpert ${run}`, senderId),
    );
  });
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

  test(
    "its result is held while the rest are accepted, then recovers once the test is active again",
    { tag: ["@evidence", "@astm", "@exceptions"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "A catalog test turned off after setup",
        "Its result is held while the rest are saved, and recovers once the test is back on.",
      );
      const specimen = "Nasopharyngeal Swab";
      await demo.caption(
        "A GeneXpert was set up on the shipped profile before this clip.",
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
      await showSample(page, order.accession);
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
        name: "Apply and retry held results",
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
    },
  );
});

test.describe("A QuantStudio from setup to a clinical result", () => {
  const run = randomUUID().slice(0, 8);
  const directory = `/data/analyzer-imports/quantstudio/incoming/${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedAnalyzer(page, QUANTSTUDIO, `Results QuantStudio ${run}`, {
        importDirectory: directory,
      }),
    );
  });

  test(
    "a results workbook in the watched folder reaches each order and is accepted",
    { tag: ["@evidence", "@file", "@results"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "QuantStudio: a results workbook with rows nobody ordered",
        "Two ordered samples are saved; unordered samples, the standards and a positive control ride along.",
      );
      await demo.caption(
        "The QuantStudio was set up on its shipped profile before this clip, with the folder the Bridge watches.",
      );
      const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
      await demo.caption(
        "Off screen: two plasma orders are created, and the instrument's plate export lands in the folder.",
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
      // The plate's five patient wells take these names; its three standards and
      // positive control keep theirs. Each well also carries an internal-control
      // row, which the profile reads as a warning about the sample, never a result.
      const unordered = (index: number) => `UNORDERED-${run}-${index}`;
      const emitted = await writeResultsFile(
        page.request,
        "quantstudio7",
        directory,
        [
          orders[0].accession,
          orders[1].accession,
          unordered(3),
          unordered(5),
          unordered(7),
        ],
      );
      const emittedFor = (accession: string) =>
        emitted.find((row) => row.sampleId === accession)!;
      for (const order of orders) {
        await expect
          .poll(
            async () =>
              (
                await worklistFor<WorklistRow>(
                  page,
                  analyzer.id,
                  order.accession,
                )
              ).length,
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
      for (const order of orders) {
        // A viral load is saved in whole copies.
        const copies = Math.round(Number(emittedFor(order.accession).result));
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
    },
  );
});

/** The parameters Mindray's C.3.3 message reports a value for, with that value and whether it is in range. */
const REPORTED: Array<{ test: string; value: string; normal: boolean }> = [
  { test: "White Blood Cells", value: "9.81", normal: true },
  { test: "Red Blood Cells", value: "4.53", normal: true },
  { test: "Hemoglobin", value: "65", normal: false },
  { test: "Mean Corpuscular Volume", value: "89.5", normal: true },
  { test: "Mean Corpuscular Hemoglobin", value: "14.4", normal: false },
  {
    test: "Mean Corpuscular Hemoglobin Concentration",
    value: "160",
    normal: false,
  },
  { test: "RDW-CV", value: "0.133", normal: true },
  { test: "RDW-SD", value: "50.9", normal: true },
  { test: "Hematocrit", value: "0.405", normal: true },
  { test: "Platelets", value: "212", normal: true },
  { test: "Mean Platelet Volume", value: "6.6", normal: false },
  { test: "Platelet Distribution Width", value: "15.4", normal: true },
  { test: "Plateletcrit", value: "1.40", normal: true },
];
/** The manual differential a technician's count adds to a CBC, which this lab does not take from the analyzer. */
const MICROSCOPE_EXAM = [
  "747-6",
  "783-1",
  "749-2",
  "740-1",
  "764-1",
  "769-0",
  "714-6",
  "707-0",
  "33831-9",
  "6746-2",
  "737-7",
  "29261-5",
  "33840-0",
  "13599-6",
  "744-3",
  "18309-5",
  "31112-6",
  "11000",
  "11001",
];
/** The two microscope-exam codes the type cannot declare: Mindray gives them no LOINC code. */
const UNDECLARED_MICROSCOPE_EXAM = new Set(["11000", "11001"]);

const orderCbc = async (page: Page) => {
  const testIds = new Map<string, string>();
  for (const { test } of REPORTED) {
    testIds.set(test, await activeTestId(page, test, "Whole Blood"));
  }
  const order = await createClinicalOrder(page, {
    testIds: [...testIds.values()],
    specimenName: "Whole Blood",
  });
  return { order, testIds };
};

test.describe("A Mindray BC-5380 reports a microscope exam the lab does not do", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `BC-5380-MX-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedBc5380(page, `Microscope BC-5380 ${run}`, senderId),
    );
  });

  test(
    "its microscope exam is held until the lab marks those codes Do not receive, then the CBC is accepted",
    { tag: ["@evidence", "@hl7", "@exceptions"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Mindray BC-5380: a microscope exam the lab does not do",
        "A technician can enter a manual differential on the analyzer, and it then travels with the CBC. This lab counts differentials elsewhere and decides not to receive it.",
      );
      await demo.caption(
        "Off screen: a CBC is ordered on whole blood, and the BC-5380 sends it with a manual differential a technician entered.",
      );
      const { order, testIds } = await orderCbc(page);
      await sendBc5380Fixture(
        page,
        analyzer.id,
        order.accession,
        "manual-count",
        senderId,
      );
      const staged = () =>
        worklistFor<WorklistRow>(page, analyzer.id, order.accession);
      await expect
        .poll(async () => (await staged()).length)
        .toBe(REPORTED.length + MICROSCOPE_EXAM.length);
      const held = (await staged()).filter((row) => row.importIssueReason);
      expect(held.map((row) => row.rawTestCode).sort()).toEqual(
        [...MICROSCOPE_EXAM].sort(),
      );
      // The type names 17 of them, which start off in a lab with no test for
      // them; the other two it cannot declare.
      for (const row of held) {
        expect(row.importIssueReason, row.rawTestCode).toBe(
          UNDECLARED_MICROSCOPE_EXAM.has(row.rawTestCode)
            ? "unknown_analyzer_test"
            : "assay_not_enabled",
        );
      }

      await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByText(
          `${MICROSCOPE_EXAM.length} analyzer results need attention`,
        ),
      ).toBeVisible();
      await showSample(page, order.accession);
      const firstHeld = page.getByTestId(`held-analyzer-result-${held[0].id}`);
      await demo.caption(
        `The CBC's ${REPORTED.length} values wait for review. The ${MICROSCOPE_EXAM.length} manual-differential records are held for the lab's decision: no test in this lab's catalog carries their codes, so the ones the type names start off, and two it cannot name.`,
      );
      await demo.highlight(firstHeld);
      await demo.caption("A held row leads to this analyzer's mapping page.");
      await firstHeld
        .getByRole("link", { name: "Review analyzer mapping" })
        .click();
      await expect(page).toHaveURL(
        new RegExp(`/analyzers/${analyzer.id}/mapping`),
      );
      await demo.caption(
        "The lab counts differentials at its own microscope, so each manual-differential code is marked Do not receive.",
      );
      for (const code of MICROSCOPE_EXAM) {
        await doNotReceive(page, code);
      }
      await demo.caption(
        "Save, confirm, and apply: the held records are retried against the mapping, and records the lab does not receive are cleared.",
      );
      await saveMapping(page);
      await confirmMapping(page);
      await applyMappings(page);
      await expect
        .poll(async () => (await staged()).length)
        .toBe(REPORTED.length);
      expect((await staged()).filter((row) => row.importIssueReason)).toEqual(
        [],
      );

      await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByText(/analyzer results? needs? attention/),
      ).toHaveCount(0);
      await demo.caption(
        "Nothing is held. The reviewer accepts the CBC; a manual differential sent later is not received.",
      );
      await acceptAll(page, analyzer, [order.accession], {
        onScreen: true,
        demo,
      });
      for (const { test, value } of REPORTED) {
        await expect
          .poll(() => savedValue(page, order, testIds.get(test)!), {
            message: `Saved ${test}`,
          })
          .toBe(value);
      }
      await demo.verified(
        `The CBC on ${order.accession} is saved; the microscope exam never became a result`,
        `${MICROSCOPE_EXAM.length} records were held, then cleared by the lab's own decision on the mapping page.`,
      );
    },
  );
});

test.describe("A Mindray BC-5380 from setup to a released CBC", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `BC-5380-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, async (page) => {
      const active = await activateShippedBc5380(
        page,
        `Results BC-5380 ${run}`,
        senderId,
      );
      const { order } = await orderCbc(page);
      await sendBc5380Fixture(
        page,
        active.id,
        order.accession,
        "analysis-result",
        senderId,
      );
      await expect
        .poll(
          async () =>
            (await worklistFor<WorklistRow>(page, active.id, order.accession))
              .length,
        )
        .toBe(REPORTED.length);
      await acceptAll(page, active, [order.accession]);
      return active;
    });
  });

  test(
    "a CBC sent over HL7 is reviewed with its alarms, accepted, and released after Validation",
    { tag: ["@evidence", "@hl7", "@results"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Mindray BC-5380: a CBC over HL7 to a released result",
        "The BC-5380 was set up on its shipped HL7 profile before this clip.",
      );
      await demo.caption("Off screen: a CBC is ordered on whole blood.");
      const { order, testIds } = await orderCbc(page);
      await demo.caption(
        `The BC-5380 sends the CBC for ${order.accession} over HL7, as Mindray's manual prints it.`,
      );
      await sendBc5380Fixture(
        page,
        analyzer.id,
        order.accession,
        "analysis-result",
        senderId,
      );
      await expect
        .poll(
          async () =>
            (await worklistFor<WorklistRow>(page, analyzer.id, order.accession))
              .length,
        )
        .toBe(REPORTED.length);
      const staged = await worklistFor<WorklistRow>(
        page,
        analyzer.id,
        order.accession,
      );
      expect(staged.filter((row) => row.importIssueReason)).toEqual([]);

      await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
        waitUntil: "domcontentloaded",
      });
      await showSample(page, order.accession);
      const alarms = page.getByTestId("InstrumentSampleFlags");
      await expect(alarms).toHaveCount(1);
      await expect(alarms).toHaveText("Analyzer flags: Anemia, Hypochromia");
      await demo.caption(
        "The analyzer raised two alarms about this sample. They show with its lab number; they are not results.",
      );
      await demo.highlight(alarms);
      await demo.caption("The reviewer accepts the CBC and saves.");
      await acceptAll(page, analyzer, [order.accession], {
        onScreen: true,
        demo,
      });
      for (const { test, value } of REPORTED) {
        await expect
          .poll(() => savedValue(page, order, testIds.get(test)!), {
            message: `Saved ${test}`,
          })
          .toBe(value);
      }

      await demo.caption(
        "In Validation, the in-range values are in the Clear lane and the low ones need review.",
      );
      await openValidationQueue(page, order.accession);
      const normal = REPORTED.filter((parameter) => parameter.normal).length;
      const summary = page.getByTestId("validation-lane-summary");
      await expect(summary).toContainText(`Clear: ${normal}`);
      await expect(summary).toContainText(
        `Needs review: ${REPORTED.length - normal}`,
      );
      await demo.highlight(summary);
      await demo.caption(
        "The validator releases the clear values in one action.",
      );
      await releaseAllClear(page);
      await expect(summary).toContainText("Clear: 0");
      await demo.caption(
        "Each low value is reviewed on its own; the analyzer's alarms are among its notes.",
      );
      await openFirstWaiting(page);
      const notes = page.getByTestId("review-notes-list").first();
      await expect(notes).toContainText(
        "Analyzer flags on this sample: Anemia, Hypochromia",
      );
      await demo.highlight(notes);
      await releaseOpen(page);
      for (let left = REPORTED.length - normal - 1; left > 0; left--) {
        await validateAndReleaseFirst(page);
      }
      await expect(
        page.getByTestId("release-all-clear-why-queueEmpty"),
      ).toBeVisible();

      await demo.caption("The order's results are final.");
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        page.getByRole("button", {
          name: `Results final (${REPORTED.length})`,
        }),
      ).toBeVisible();
      await demo.verified(
        `The CBC for ${order.accession} is released`,
        `${normal} in-range values released together; HGB, MCH, MCHC and MPV after review, with the alarms in their notes.`,
      );
    },
  );
});
