import type { Page } from "@playwright/test";
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
 * OGC-1200 slice 9c (FR-B3, FR-D6, FR-D7): a result saved on a row in no run
 * is a run of one, which the row's Reagents and History sections show. A
 * control recorded on the row before the save opens the run of one; a failing
 * one holds the row until the test is decided, and the save then completes
 * the run.
 */

const openRow = async (page: Page, accession: string, labUnit = false) => {
  await page.goto(`/Results?accessionNumber=${accession}`);
  if (labUnit) {
    const unit = page.getByRole("combobox", { name: "Lab Unit" });
    await unit.click();
    await unit.fill("Biochem");
    await page.getByRole("option", { name: "Biochemistry" }).click();
    const search = page.getByRole("searchbox");
    await search.fill(accession);
    await search.press("Enter");
  }
  await expect(page.getByText(accession).first()).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
};

const saveValue = async (page: Page, value: string) => {
  await page.locator('input[id^="unifiedResultValue-"]').first().fill(value);
  await page.getByRole("button", { name: "Save" }).first().click();
  await signIfAsked(page);
};

test.describe("OGC-1200 a single save is a run of one", () => {
  let seed: Seed | undefined;

  test.afterEach(async ({ page }) => {
    if (seed) {
      await retire(page, seed);
      seed = undefined;
    }
  });

  test("a value saved on a row in no run is recorded in a run of one", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);

    await openRow(page, accession);
    const saved = page.waitForResponse(
      (response) =>
        response.request().method() === "POST" &&
        /\/rest\/results-entry/.test(new URL(response.url()).pathname),
    );
    await saveValue(page, "81");
    await saved;

    await openRow(page, accession);
    await page
      .getByRole("button", { name: /expand row/i })
      .first()
      .click();
    const summary = page.getByTestId("row-run-summary");
    await expect(summary).toContainText("Recorded in RUN-", {
      timeout: LONG_TIMEOUT,
    });
    await expect(summary).toContainText("Manual entry");

    await summary.getByTestId("row-run-link").click();
    const settings = page.getByTestId("run-settings");
    await expect(settings).toContainText("Manual entry", {
      timeout: LONG_TIMEOUT,
    });
    await expect(settings).toContainText("Complete");
    await expect(page.getByTestId("run-progress")).toContainText("1 of 1");
  });

  test("a control recorded on a row opens its run of one, which holds the row until the test is decided", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    seed = await seedReagentAndControl(page);
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);

    await openRow(page, accession, true);
    await page
      .getByRole("button", { name: /expand row/i })
      .first()
      .click();
    await page.getByTestId("qc-outcome").selectOption("FAIL");
    const lot = page.getByTestId("qc-lot");
    const lotValue = await lot
      .locator("option", { hasText: seed.controlLotNumber })
      .getAttribute("value");
    await lot.selectOption(lotValue || "");
    await page.getByRole("textbox", { name: "Measured value" }).fill("140");
    await page.getByLabel("Expected value").fill("80");
    await page.getByLabel("Uncertainty (±)").fill("8");
    await page.getByTestId("qc-record").click();
    const summary = page.getByTestId("row-run-summary");
    await expect(summary).toContainText("Held", { timeout: LONG_TIMEOUT });
    const runLabel = (
      (await summary.getByTestId("row-run-link").textContent()) || ""
    ).trim();
    expect(runLabel).toMatch(/^RUN-\d+$/);

    await openRow(page, accession);
    await expect(
      page.getByText(`Held: QC failed in ${runLabel}`, { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.locator('input[id^="unifiedResultValue-"]')).toHaveCount(
      0,
    );

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

    await openRow(page, accession);
    await saveValue(page, "82");
    await page.goto(`/Results?run=${runLabel.replace("RUN-", "")}`);
    await expect(page.getByTestId("run-settings")).toContainText("Complete", {
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByTestId("run-progress")).toContainText("1 of 1");
  });
});
