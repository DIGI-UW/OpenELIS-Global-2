import { test, expect } from "../../../helpers/test-base";
import {
  assignReagent,
  batchJson,
  createBatch,
  retire,
  seedReagentAndControl,
  type Seed,
} from "../../../helpers/batch-workplan";
import {
  AMYLASE_TEST,
  API,
  SERUM,
  orderTests,
} from "../../../helpers/results-nce-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-427: a batch is set up with a reagent lot, its control lot's QC is
 * checked, a bench control is recorded from the batch itself, and the batch is
 * released for printing. A batch whose QC is not current is released only by
 * an override, which raises a non-conforming event shown on the batch.
 *
 * Every precondition is created through the endpoints the app's own screens
 * use (inventory, test catalog reagent link, control lot setup, order entry,
 * bench QC capture) and retired again at the end.
 */

test.describe("OGC-427 batch workplan with reagent QC", () => {
  let seed: Seed;

  test.beforeEach(async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    seed = await seedReagentAndControl(page);
  });

  test.afterEach(async ({ page }) => {
    if (seed) {
      await retire(page, seed);
    }
  });

  test("a bench batch records its own QC run and is released for printing", async ({
    page,
  }) => {
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const { batchId, tile } = await createBatch(
      page,
      seed,
      accession,
      `PW427 bench ${seed.run}`,
    );

    await assignReagent(page, batchId, seed, 1);
    const qcTag = tile.getByTestId(`batch-${batchId}-qc-tag`);
    await expect(qcTag).toHaveText("QC not run");
    await expect(
      tile.getByRole("cell", { name: new RegExp(seed.controlLotNumber) }),
    ).toBeVisible();

    await tile.getByRole("button", { name: "Run QC" }).click();
    const form = tile.getByTestId(`batch-${batchId}-qc-form`);
    await form.getByRole("button", { name: "Save QC result" }).click();
    await expect(
      form.getByText("A numeric QC value is required."),
    ).toBeVisible();
    await form.getByLabel("Value").fill("81.2");
    await form.locator(`label[for="batch-${batchId}-qc-pass"]`).click();
    const recorded = page.waitForResponse(
      (response) =>
        response.url().endsWith("/rest/qc/results") &&
        response.request().method() === "POST",
    );
    await form.getByRole("button", { name: "Save QC result" }).click();
    expect((await recorded).status()).toBe(201);
    await expect(qcTag).toHaveText("QC pass", { timeout: LONG_TIMEOUT });

    const released = page.waitForResponse((response) =>
      response.url().endsWith(`/batches/${batchId}/generate`),
    );
    await tile.getByRole("button", { name: "Generate workplan" }).click();
    expect((await released).status()).toBe(200);
    await expect(tile.getByText("Active", { exact: true })).toBeVisible();
    await expect(
      page.getByRole("dialog").filter({ visible: true }),
    ).toHaveCount(0);

    await page.goto(`/Workplan/batch/${batchId}/print`);
    const printout = page.getByTestId("batch-workplan-print");
    await expect(printout.getByText(seed.lotNumbers[1])).toBeVisible();
    await expect(printout.getByText(accession)).toBeVisible();
    await expect(printout.getByText("QC pass")).toBeVisible();
  });

  test("a batch on failed QC is released only by an override, which raises an NCE", async ({
    page,
  }) => {
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);
    await batchJson(page, "POST", "/rest/qc/results", {
      source: "MANUAL",
      qualitativeOutcome: "FAIL",
      resultValue: "95",
      controlLotId: seed.controlLotId,
      testId: AMYLASE_TEST,
      testSectionId: String(
        (
          await (
            await page.request.get(
              `${API}/rest/test-catalog/tests/${AMYLASE_TEST}/basic-info`,
            )
          ).json()
        ).labUnitId,
      ),
    });
    const { batchId, tile } = await createBatch(
      page,
      seed,
      accession,
      `PW427 override ${seed.run}`,
    );
    await assignReagent(page, batchId, seed, 0);
    const qcTag = tile.getByTestId(`batch-${batchId}-qc-tag`);
    await expect(qcTag).toHaveText(/QC failed|QC violation/);

    await tile.getByRole("button", { name: "Generate workplan" }).click();
    const dialog = page
      .getByRole("dialog")
      .filter({ hasText: /Most recent QC failed|Active Westgard violation/ });
    await expect(dialog).toBeVisible();
    // OGC-1588: the override is not sent until the user says why.
    await dialog
      .getByRole("button", { name: /Proceed and create NCE/ })
      .click();
    await expect(
      dialog.getByText("Give the reason before releasing the batch."),
    ).toBeVisible();
    const reason = `Control rerun booked ${seed.run}`;
    await dialog
      .getByLabel("Reason for releasing without passing QC")
      .fill(reason);
    const released = page.waitForResponse((response) =>
      response.url().endsWith(`/batches/${batchId}/generate`),
    );
    await dialog
      .getByRole("button", { name: /Proceed and create NCE/ })
      .click();
    const body = await (await released).json();
    const nceNumber = body.releases[0].nceNumber;
    expect(nceNumber).toBeTruthy();
    expect(body.releases[0].overrideReason).toBe(reason);
    await expect(
      tile.getByTestId(`batch-${batchId}-override-reason`),
    ).toHaveText(`Override reason: ${reason}`);
    const nceTag = tile.getByText(`NCE: ${nceNumber}`);
    await expect(nceTag).toBeVisible();
    await expect(nceTag.locator("xpath=ancestor::a")).toHaveAttribute(
      "href",
      `/ViewNonConformingEvent?nceNumber=${nceNumber}`,
    );
  });
});
