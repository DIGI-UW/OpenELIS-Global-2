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
} from "../../../helpers/analyzer-native-traffic";
import {
  FLUOROCYCLER,
  activateShippedAnalyzer,
  activateShippedGeneXpert,
} from "../../../helpers/analyzer-setup-flow";
import { withAuthedPage } from "../../../helpers/api-session";
import {
  acceptAll,
  savedValue,
  type Order,
} from "../../../helpers/analyzer-review";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

const VIRAL_LOAD = "HIV-1 viral load";

/** The reviewer's row for what the instrument sent under this specimen ID. */
async function ownRow(page: Page, analyzer: Analyzer, specimenId: string) {
  type Row = WorklistRow & { componentId?: string | null };
  return (await worklistFor<Row>(page, analyzer.id, specimenId)).find(
    (row) => !row.componentId,
  );
}

/** The ID with a letter swapped in, as a person mistyping a label would, matching no order. */
const mistyped = (accession: string) => accession.replace("DEV", "DVE");

const SET_UP_BEFORE =
  "The GeneXpert was set up and activated on the shipped profile before this clip.";

test.describe("Where an instrument's result is placed", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-PLACE-${run}`;
  let analyzer: Analyzer;
  let testId: string;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, async (page) => {
      testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
      return activateShippedGeneXpert(
        page,
        `Placement GeneXpert ${run}`,
        senderId,
      );
    });
  });

  const order = (page: Page) =>
    createClinicalOrder(page, { testIds: [testId], specimenName: "Plasma" });
  const send = (
    page: Page,
    specimenId: string,
    outcome = "quantified",
    patient?: { id: string; name: string },
  ) =>
    sendGeneXpertFixture(
      page,
      analyzer.id,
      specimenId,
      { assay: "hivvl", outcome },
      senderId,
      {},
      patient,
    );
  const arrived = (page: Page, specimenId: string) =>
    expect
      .poll(async () => (await ownRow(page, analyzer, specimenId))?.testId)
      .toBe(testId);

  test("a result sent under its tube's ID is placed on that tube's analysis", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "A result sent under a tube's ID lands on that tube",
      SET_UP_BEFORE,
    );
    const placed = await order(page);
    const tube = `${placed.accession}-1`;
    await demo.caption(
      `Off screen: an HIV-1 viral load order is created. The GeneXpert sends its result under the tube's ID, ${tube}.`,
    );
    await send(page, tube);
    await arrived(page, placed.accession);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const row = page.getByRole("row", {
      name: new RegExp(placed.accession),
    });
    await expect(row.first()).toContainText(
      `One analysis on ${tube} is waiting for this result.`,
    );
    await demo.caption("The review row names the analysis the result goes to.");
    await demo.highlight(row.first());
    await demo.caption("The reviewer accepts it and saves.");
    await acceptAll(page, analyzer, [placed.accession], { demo });
    await expect
      .poll(() => savedValue(page, placed, testId, VIRAL_LOAD))
      .toBe("1010");
    await demo.verified(
      `HIV-1 viral load 1010 is saved on ${placed.accession}`,
      `Sent under the tube ID ${tube}.`,
    );
  });

  test("a mistyped ID is held for the reviewer, who places it on the right order with a reason", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "A mistyped specimen ID is held for the reviewer",
      SET_UP_BEFORE,
    );
    const intended = await order(page);
    const typo = mistyped(intended.accession);
    await demo.caption(
      `Off screen: the order is ${intended.accession}, but the instrument sends its result under ${typo}.`,
    );
    await send(page, typo);
    await arrived(page, typo);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const row = page.getByRole("row", { name: new RegExp(typo) });
    await expect(row.first()).toContainText(
      "No order or tube carries this ID. Saving creates a new sample for it.",
    );
    await demo.caption("No order carries that ID, and the row says so.");
    await demo.highlight(row.first());
    await demo.caption(
      "The reviewer places it on the intended order and says why.",
    );
    await row
      .getByRole("button", { name: "Place on another order" })
      .first()
      .click();
    await page
      .getByRole("textbox", { name: "Order (lab number)" })
      .fill(intended.accession);
    await page
      .getByRole("textbox", { name: "Why this belongs to that order" })
      .fill("The label was misread; this is the order's tube");
    await acceptAll(page, analyzer, [typo], { onScreen: true, demo });
    await expect
      .poll(() => savedValue(page, intended, testId, VIRAL_LOAD))
      .toBe("1010");
    await demo.verified(
      `HIV-1 viral load 1010 is saved on ${intended.accession}`,
      `Sent as ${typo}, placed by the reviewer with a reason.`,
    );
  });

  test("a patient mismatch is explained on the row and needs a note before it saves", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "A patient mismatch needs a note before it saves",
      SET_UP_BEFORE,
    );
    const placed = await order(page);
    await demo.caption(
      "Off screen: the instrument sends the order's ID with a different patient on the message.",
    );
    await send(page, placed.accession, "quantified", {
      id: `MRN-${run}`,
      name: "Roe^Jane",
    });
    await arrived(page, placed.accession);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const row = page.getByRole("row", {
      name: new RegExp(placed.accession),
    });
    await expect(row.first()).toContainText("Patient mismatch");
    await expect(row.first()).toContainText(
      "Add a note saying why before saving.",
    );
    await demo.caption(
      "The row explains the mismatch and asks for a note before saving.",
    );
    await demo.highlight(row.first());
    await demo.caption("The reviewer adds the note, accepts and saves.");
    await acceptAll(page, analyzer, [placed.accession], {
      onScreen: true,
      note: "Instrument keyed a different patient ID; the tube is correct",
      demo,
    });
    await expect
      .poll(() => savedValue(page, placed, testId, VIRAL_LOAD))
      .toBe("1010");
    await demo.verified(
      `HIV-1 viral load 1010 is saved on ${placed.accession}`,
      "The row asked for a note; the reviewer added one before saving.",
    );
  });

  test("a rerun replaces the saved result and says so", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "A rerun never replaces a saved result silently",
      SET_UP_BEFORE,
    );
    const placed = await order(page);
    await demo.caption(
      "Off screen: the order's first run sends 1010. The reviewer accepts it.",
    );
    await send(page, placed.accession);
    await arrived(page, placed.accession);
    await acceptAll(page, analyzer, [placed.accession], { demo });
    await expect
      .poll(() => savedValue(page, placed, testId, VIRAL_LOAD))
      .toBe("1010");

    // The sample is run again; the new result must not replace the old silently.
    await demo.caption("Off screen: the sample is run again and sends <40.");
    await send(page, placed.accession, "below-40");
    await expect
      .poll(async () => (await ownRow(page, analyzer, placed.accession))?.id)
      .toBeTruthy();
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const rerun = page
      .getByRole("row", { name: new RegExp(placed.accession) })
      .first();
    await expect(rerun).toContainText(
      "already holds a result. Saving replaces it and marks it corrected.",
    );
    await demo.caption(
      "The rerun's row warns that saving replaces the saved result and marks it corrected.",
    );
    await demo.highlight(rerun, 3500);
    await demo.verified(
      `The rerun for ${placed.accession} waits with a warning`,
      "Saving it would replace 1010 and mark the result corrected.",
    );
  });
});

test.describe("Where a results file's sample is placed", () => {
  test("a plate with one mistyped sample name places the rest and holds that one for the reviewer", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "A plate with one mistyped sample name",
      "The rest of the plate is placed; the mistyped one waits for the reviewer.",
    );
    const run = randomUUID().slice(0, 8);
    const directory = `/data/analyzer-imports/fluorocycler-xt/incoming/${run}`;
    await demo.caption(
      "Set up a FluoroCycler with the folder the Bridge watches.",
    );
    const analyzer = await activateShippedAnalyzer(
      page,
      FLUOROCYCLER,
      `Placement FluoroCycler ${run}`,
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
    const typo = mistyped(orders[1].accession);
    await demo.caption(
      `Off screen: two orders are created. The plate's file names the second sample ${typo}.`,
    );
    const emitted = await writeFluoroCyclerFile(page.request, directory, [
      orders[0].accession,
      typo,
    ]);
    await expect
      .poll(async () => (await ownRow(page, analyzer, typo))?.id)
      .toBeTruthy();
    await expect
      .poll(async () => (await ownRow(page, analyzer, orders[0].accession))?.id)
      .toBeTruthy();

    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const row = page.getByRole("row", { name: new RegExp(typo) });
    await demo.caption(
      "The first sample is placed on its order. The mistyped one matches no order.",
    );
    await demo.highlight(row.first());
    await demo.caption(
      "The reviewer places it on the intended order with a reason, accepts both and saves.",
    );
    await row
      .getByRole("button", { name: "Place on another order" })
      .first()
      .click();
    await page
      .getByRole("textbox", { name: "Order (lab number)" })
      .fill(orders[1].accession);
    await page
      .getByRole("textbox", { name: "Why this belongs to that order" })
      .fill("The plate's sample name was mistyped");
    await acceptAll(page, analyzer, [orders[0].accession, typo], {
      onScreen: true,
      demo,
    });
    for (const [index, order] of orders.entries()) {
      await expect
        .poll(async () =>
          Number(await savedValue(page, order, testId, VIRAL_LOAD)),
        )
        .toBe(Number(emitted[index].result));
    }
    await demo.verified(
      "Both samples on the plate are saved on their orders",
      orders
        .map((order, index) => `${order.accession}: ${emitted[index].result}`)
        .join(" · "),
    );
  });
});
