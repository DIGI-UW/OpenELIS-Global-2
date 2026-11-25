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
import { withAuthedPage } from "../../../helpers/api-session";
import { savedValue } from "../../../helpers/analyzer-review";

/**
 * OGC-1662 — after a save the worklist is reread with the server's defaults:
 * no notes, matched results ticked again. The next save sent those while the
 * screen still showed the reviewer's notes and an unticked box, so a result
 * left out was saved and a patient mismatch lost its note.
 */

const VIRAL_LOAD = "HIV-1 viral load";

test.describe("Saving analyser results twice from one screen", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-REREAD-${run}`;
  let analyzer: Analyzer;
  let testId: string;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, async (page) => {
      testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
      return activateShippedGeneXpert(
        page,
        `Reread GeneXpert ${run}`,
        senderId,
      );
    });
  });

  const order = (page: Page) =>
    createClinicalOrder(page, { testIds: [testId], specimenName: "Plasma" });
  const mainRow = async (page: Page, accession: string) => {
    type Row = WorklistRow & { componentId?: string | null };
    const rows = await worklistFor<Row>(page, analyzer.id, accession);
    return rows.find((row) => !row.componentId);
  };

  test("the second save sends the notes and boxes the screen shows", async ({
    page,
  }) => {
    const mismatch = await order(page);
    const saved = await order(page);
    const leftOut = await order(page);
    const fixture = { assay: "hivvl", outcome: "quantified" };
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      mismatch.accession,
      fixture,
      senderId,
      {},
      { id: `MRN-${run}`, name: "Roe^Jane" },
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      saved.accession,
      fixture,
      senderId,
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      leftOut.accession,
      fixture,
      senderId,
    );
    for (const placed of [mismatch, saved, leftOut]) {
      await expect
        .poll(async () => (await mainRow(page, placed.accession))?.testId)
        .toBe(testId);
    }
    const mismatchRow = (await mainRow(page, mismatch.accession))!;
    const leftOutRow = (await mainRow(page, leftOut.accession))!;
    const box = (row: WorklistRow) =>
      page.locator(`[id="resultList${row.id}.isAccepted"]`);
    const note = (row: WorklistRow) =>
      page.locator(`[id="resultList${row.id}.note"]`);

    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(box(leftOutRow)).toBeChecked();
    await note(mismatchRow).fill(
      "Instrument keyed a different patient ID; the tube is correct",
    );
    await page
      .locator(`label[for="resultList${leftOutRow.id}.isAccepted"]`)
      .click();
    await expect(box(leftOutRow)).not.toBeChecked();
    await note(leftOutRow).fill("Hold for the ward");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      page.getByRole("row", { name: new RegExp(saved.accession) }),
    ).toHaveCount(0);

    await expect(note(mismatchRow)).toHaveValue(
      "Instrument keyed a different patient ID; the tube is correct",
    );
    await expect(box(leftOutRow)).not.toBeChecked();
    await expect(note(leftOutRow)).toHaveValue("Hold for the ward");

    await page
      .locator(`label[for="resultList${mismatchRow.id}.isAccepted"]`)
      .click();
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(box(mismatchRow)).toHaveCount(0);

    await expect
      .poll(() => savedValue(page, mismatch, testId, VIRAL_LOAD))
      .toBe("1010");
    expect(await mainRow(page, leftOut.accession)).toBeDefined();
    await expect(box(leftOutRow)).not.toBeChecked();
  });

  test("a selected mismatch without a note is named as not saved", async ({
    page,
  }) => {
    const mismatch = await order(page);
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      mismatch.accession,
      { assay: "hivvl", outcome: "quantified" },
      senderId,
      {},
      { id: `MRN2-${run}`, name: "Roe^Jim" },
    );
    await expect
      .poll(async () => (await mainRow(page, mismatch.accession))?.testId)
      .toBe(testId);
    const row = (await mainRow(page, mismatch.accession))!;

    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    await page.locator(`label[for="resultList${row.id}.isAccepted"]`).click();
    await page.getByRole("button", { name: "Save", exact: true }).click();

    await expect(
      page.getByText(
        new RegExp(
          `not saved and are still in review: ${mismatch.accession}.*explain the patient mismatch in the note`,
        ),
      ),
    ).toBeVisible();
    await expect(
      page.getByText("Results have been validated successfully"),
    ).toHaveCount(0);
    expect((await mainRow(page, mismatch.accession))?.importIssueReason).toBe(
      "awaiting_placement",
    );
  });
});
