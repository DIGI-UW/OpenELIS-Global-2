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

test.describe("Where an instrument's result is placed", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-PLACE-${run}`;
  let analyzer: Analyzer;
  let testId: string;

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(180_000);
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
      page.request,
      analyzer.bridgeConnectionId,
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
  }) => {
    test.setTimeout(180_000);
    const placed = await order(page);
    const tube = `${placed.accession}-1`;
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
    await acceptAll(page, analyzer, [placed.accession]);
    await expect
      .poll(() => savedValue(page, placed, testId, VIRAL_LOAD))
      .toBe("1010");
  });

  test("a mistyped ID is held for the reviewer, who places it on the right order with a reason", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const intended = await order(page);
    const typo = mistyped(intended.accession);
    await send(page, typo);
    await arrived(page, typo);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const row = page.getByRole("row", { name: new RegExp(typo) });
    await expect(row.first()).toContainText(
      "No order or tube carries this ID. Saving creates a new sample for it.",
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
    await acceptAll(page, analyzer, [typo], { onScreen: true });
    await expect
      .poll(() => savedValue(page, intended, testId, VIRAL_LOAD))
      .toBe("1010");
  });

  test("a patient mismatch is explained on the row and needs a note before it saves", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const placed = await order(page);
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
    await acceptAll(page, analyzer, [placed.accession], {
      onScreen: true,
      note: "Instrument keyed a different patient ID; the tube is correct",
    });
    await expect
      .poll(() => savedValue(page, placed, testId, VIRAL_LOAD))
      .toBe("1010");
  });

  test("a rerun replaces the saved result and says so", async ({ page }) => {
    test.setTimeout(180_000);
    const placed = await order(page);
    await send(page, placed.accession);
    await arrived(page, placed.accession);
    await acceptAll(page, analyzer, [placed.accession]);
    await expect
      .poll(() => savedValue(page, placed, testId, VIRAL_LOAD))
      .toBe("1010");

    // The sample is run again; the new result must not replace the old silently.
    await send(page, placed.accession, "below-40");
    await expect
      .poll(async () => (await ownRow(page, analyzer, placed.accession))?.id)
      .toBeTruthy();
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("row", { name: new RegExp(placed.accession) }).first(),
    ).toContainText(
      "already holds a result. Saving replaces it and marks it corrected.",
    );
  });
});

test.describe("Where a results file's sample is placed", () => {
  test("a plate with one mistyped sample name places the rest and holds that one for the reviewer", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 8);
    const directory = `/data/analyzer-imports/fluorocycler-xt/incoming/${run}`;
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
    });
    for (const [index, order] of orders.entries()) {
      await expect
        .poll(async () =>
          Number(await savedValue(page, order, testId, VIRAL_LOAD)),
        )
        .toBe(Number(emitted[index].result));
    }
  });
});
