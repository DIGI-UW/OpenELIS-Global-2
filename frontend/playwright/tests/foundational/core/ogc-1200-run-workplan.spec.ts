import { test, expect } from "../../../helpers/test-base";
import {
  assignReagent,
  createBatch,
  retire,
  seedReagentAndControl,
  type Seed,
} from "../../../helpers/batch-workplan";
import {
  AMYLASE_TEST,
  SERUM,
  orderTests,
  signIfAsked,
} from "../../../helpers/results-nce-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1200 slice 9a (FR-B2, FR-D5, FR-E3, FR-G4): a released workplan batch is
 * a run. The worklist row reads "Batched in RUN-x" and stays open to entry,
 * the run shows the batch's reagent lot and analyses, a failed control
 * recorded on the run holds the batch's test everywhere, and once the test is
 * decided the value entered from the run completes it.
 */

test.describe("OGC-1200 a workplan batch is a run", () => {
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

  test("a released batch is a run its results and controls record into", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const { batchId, tile } = await createBatch(
      page,
      seed,
      accession,
      `PW1200 batch ${seed.run}`,
    );
    await assignReagent(page, batchId, seed, 1);

    await tile.getByRole("button", { name: "Run QC" }).click();
    const form = tile.getByTestId(`batch-${batchId}-qc-form`);
    await form.getByLabel("Value").fill("80.4");
    await form.locator(`label[for="batch-${batchId}-qc-pass"]`).click();
    await form.getByRole("button", { name: "Save QC result" }).click();
    await expect(tile.getByTestId(`batch-${batchId}-qc-tag`)).toHaveText(
      "QC pass",
      { timeout: LONG_TIMEOUT },
    );
    await tile.getByRole("button", { name: "Generate workplan" }).click();
    const runLink = tile.getByTestId(`batch-${batchId}-run`);
    await expect(runLink).toBeVisible({ timeout: LONG_TIMEOUT });
    const runLabel = ((await runLink.textContent()) || "").replace("Open ", "");
    expect(runLabel).toMatch(/^RUN-\d+$/);

    await test.step("The worklist row is batched in the run and stays open to entry", async () => {
      await page.goto(`/Results?accessionNumber=${accession}`);
      await expect(
        page.getByText(`Batched in ${runLabel}`, { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.locator('input[id^="unifiedResultValue-"]').first(),
      ).toBeEditable();
    });

    await test.step("The run shows the batch's lot and analysis", async () => {
      await page.goto(`/Results?run=${runLabel.replace("RUN-", "")}`);
      const settings = page.getByTestId("run-settings");
      await expect(settings).toContainText("Workplan batch", {
        timeout: LONG_TIMEOUT,
      });
      await expect(settings).toContainText(seed.lotNumbers[1]);
      await expect(page.getByTestId("run-rows")).toContainText(accession);
    });

    await test.step("A failed control on the run holds the batch's test everywhere", async () => {
      const qc = page.getByTestId("run-qc");
      await qc.getByTestId("run-qc-record").click();
      const record = qc.getByTestId("run-qc-form");
      await record.locator("label", { hasText: /^Control line$/ }).click();
      await record.getByLabel("Kit lot").fill(`KIT-${seed.run}`);
      await record.locator("label", { hasText: /^Invalid$/ }).click();
      await record.getByTestId("run-qc-save").click();
      await expect(qc.getByTestId(`run-qc-decide-${AMYLASE_TEST}`)).toBeVisible(
        {
          timeout: LONG_TIMEOUT,
        },
      );
      await expect(page.getByTestId("run-rows")).toContainText(
        "Held: QC failed",
      );

      await page.goto(`/Results?accessionNumber=${accession}`);
      await expect(
        page.getByText(`Held: QC failed in ${runLabel}`, { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Once decided, the value entered from the run completes it", async () => {
      await page.goto(`/Results?run=${runLabel.replace("RUN-", "")}`);
      const decide = page.getByTestId(`run-qc-decide-${AMYLASE_TEST}`);
      await decide
        .getByRole("button", { name: /Accept despite QC failure/ })
        .click();
      await decide
        .getByLabel(/Reason/)
        .fill("Control repeated on the bench and passed");
      await page.getByTestId(`run-qc-decide-confirm-${AMYLASE_TEST}`).click();
      await expect(
        page.getByTestId(`run-qc-decision-${AMYLASE_TEST}`),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const rows = page
        .getByTestId("run-rows")
        .locator('> tbody > tr[data-testid^="run-row-"]');
      await rows
        .first()
        .getByRole("button", { name: /expand row/i })
        .click();
      const entry = page.locator('[data-testid^="run-row-entry-"]');
      await entry.locator('input[id^="unifiedResultValue-"]').fill("81");
      await entry
        .getByRole("button", { name: /^accept$/i })
        .first()
        .click();
      await signIfAsked(page);
      await expect(rows).toHaveCount(0, { timeout: LONG_TIMEOUT });
      await expect(page.getByTestId("run-progress")).toContainText("1 of 1");
    });
  });
});
