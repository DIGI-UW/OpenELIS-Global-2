import { test, expect } from "../../../helpers/test-base";
import {
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
 * OGC-1200 slice 9b (FR-B4, FR-E3): pending rows of one test, picked on the
 * worklist one lab number at a time, are batched into a run that opens at
 * once; the lot is chosen on the run with nothing pre-selected, a value entered
 * from the run counts toward it, and Mark complete closes the run and its batch.
 */

test.describe("OGC-1200 Batch these from the worklist", () => {
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

  test("rows of one test picked on the worklist become a run whose lot is chosen on it", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    const first = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const second = await orderTests(page, SERUM, [AMYLASE_TEST]);
    let runId = 0;
    const pickRow = async (accession: string) => {
      const box = page.getByLabel(
        new RegExp(`^Select ${accession} Amylase.* for a batch$`),
      );
      await expect(box).toBeAttached({ timeout: LONG_TIMEOUT });
      await page
        .locator(`label[for="${await box.getAttribute("id")}"]`)
        .click();
      await expect(box).toBeChecked();
    };

    await test.step("Two rows picked one search at a time are batched into a run", async () => {
      await page.goto(`/Results?accessionNumber=${first}`);
      await pickRow(first);
      await expect(page.getByTestId("batch-these")).toBeDisabled();
      await expect(page.getByTestId("batch-these-hint")).toHaveText(
        "Select two or more pending rows of one test to batch them.",
      );

      const search = page.getByRole("searchbox");
      await search.fill(second);
      await search.press("Enter");
      await pickRow(second);
      await expect(page.getByTestId("batch-these-count")).toHaveText(
        "Rows selected for a batch: 2",
      );

      const batched = page.waitForResponse((response) =>
        response.url().endsWith("/rest/batch-workplans/batches/from-worklist"),
      );
      await page.getByTestId("batch-these").click();
      const body = await (await batched).json();
      seed.batchIds.push(body.id);
      runId = body.runId;
      expect(runId).toBeGreaterThan(0);
      await expect(page.getByTestId("run-settings")).toContainText(
        "Workplan batch",
        { timeout: LONG_TIMEOUT },
      );
      await expect(page.getByTestId("run-rows")).toContainText(first);
      await expect(page.getByTestId("run-rows")).toContainText(second);
      await expect(page.getByTestId("run-progress")).toContainText("0 of 2");
    });

    await test.step("The lot is chosen on the run, with nothing pre-selected", async () => {
      const lot = page.getByLabel("Reagent lot for this run");
      await expect(lot).toHaveValue("");
      await expect(page.getByTestId("run-lot-save")).toBeDisabled();
      const chosen = await lot
        .locator("option", { hasText: seed.lotNumbers[1] })
        .getAttribute("value");
      await lot.selectOption(chosen || "");
      await page.getByTestId("run-lot-save").click();
      await expect(
        page.getByTestId("run-setting-run.label.reagentLot"),
      ).toHaveText(seed.lotNumbers[1], { timeout: LONG_TIMEOUT });
    });

    await test.step("A value entered from the run counts toward it", async () => {
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
      await expect(page.getByTestId("run-progress")).toContainText("1 of 2", {
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("Mark complete closes the run and its batch", async () => {
      await page.getByTestId("run-mark-complete").click();
      await expect(page.getByTestId("run-settings")).toContainText("Complete", {
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("run-mark-complete")).toHaveCount(0);
      await expect(page.getByTestId("run-lot-picker")).toHaveCount(0);
    });
  });
});
