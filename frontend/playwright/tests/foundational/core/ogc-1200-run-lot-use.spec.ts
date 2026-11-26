import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  retire,
  seedReagentAndControl,
  type Seed,
} from "../../../helpers/batch-workplan";
import {
  AMYLASE_TEST,
  API,
  SERUM,
  orderTests,
  signIfAsked,
} from "../../../helpers/results-nce-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1200 slice 9d (FR-G5, FR-G2): a result accepted in a run that carries a
 * lot uses that lot once, by the quantity the Test Catalog links its reagent
 * with; the QC dashboard's Bench QC tab names the run a control was recorded
 * on and opens it on the Results page.
 */

const pickRow = async (page: Page, accession: string) => {
  const box = page.getByLabel(
    new RegExp(`^Select ${accession} Amylase.* for a batch$`),
  );
  await expect(box).toBeAttached({ timeout: LONG_TIMEOUT });
  await page.locator(`label[for="${await box.getAttribute("id")}"]`).click();
  await expect(box).toBeChecked();
};

const expandRow = async (page: Page) => {
  await page
    .getByRole("button", { name: /expand row/i })
    .first()
    .click();
  await expect(
    page.getByRole("button", { name: /Reagents, QC & Controls/ }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
};

const lotRemaining = async (page: Page, accession: string, lot: string) => {
  await page.goto(`/Results?accessionNumber=${accession}`);
  await expect(page.getByText(accession).first()).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expandRow(page);
  return page.getByTestId(`lot-${lot}`);
};

test.describe("OGC-1200 a run's lot is used by its results", () => {
  let seed: Seed | undefined;

  test.afterEach(async ({ page }) => {
    if (seed) {
      await retire(page, seed);
      seed = undefined;
    }
  });

  test("a result accepted in a batch run uses half a unit of the run's lot, once", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    seed = await seedReagentAndControl(page);
    const first = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const second = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const elsewhere = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const [lotA, lotB] = seed.lotNumbers;

    await page.goto(`/Results?accessionNumber=${first}`);
    await pickRow(page, first);
    const search = page.getByRole("searchbox");
    await search.fill(second);
    await search.press("Enter");
    await pickRow(page, second);
    const batched = page.waitForResponse((response) =>
      response.url().endsWith("/rest/batch-workplans/batches/from-worklist"),
    );
    await page.getByTestId("batch-these").click();
    seed.batchIds.push((await (await batched).json()).id);

    const lot = page.getByLabel("Reagent lot for this run");
    await lot.selectOption(
      (await lot.locator("option", { hasText: lotB }).getAttribute("value")) ||
        "",
    );
    await page.getByTestId("run-lot-save").click();
    await expect(
      page.getByTestId("run-setting-run.label.reagentLot"),
    ).toHaveText(lotB, { timeout: LONG_TIMEOUT });

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

    await expect(await lotRemaining(page, elsewhere, lotB)).toContainText(
      "49.5 mL remaining",
      { timeout: LONG_TIMEOUT },
    );
    await expect(page.getByTestId(`lot-${lotA}`)).toContainText(
      "50 mL remaining",
    );
  });

  test("the Bench QC tab opens the run a control was recorded on", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    seed = await seedReagentAndControl(page);
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);

    const units = (await (
      await page.request.get(`${API}/rest/results-entry/lab-units`)
    ).json()) as Array<{ id: string; value: string }>;
    const biochemistry = units.find((unit) => unit.value === "Biochemistry");
    expect(biochemistry, "Biochemistry is a lab unit").toBeTruthy();
    await page.goto(
      `/Results?testSectionId=${biochemistry!.id}&accessionNumber=${accession}`,
    );
    await expect(page.getByText(accession).first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expandRow(page);
    await page.getByTestId("qc-outcome").selectOption("PASS");
    const controlLot = page.getByTestId("qc-lot");
    await controlLot.selectOption(
      (await controlLot
        .locator("option", { hasText: seed.controlLotNumber })
        .getAttribute("value")) || "",
    );
    await page.getByRole("textbox", { name: "Measured value" }).fill("81");
    await page.getByLabel("Expected value").fill("80");
    await page.getByLabel("Uncertainty (±)").fill("8");
    await page.getByTestId("qc-record").click();
    const runLink = page
      .getByTestId("row-run-summary")
      .getByTestId("row-run-link");
    await expect(runLink).toHaveText(/^RUN-\d+$/, { timeout: LONG_TIMEOUT });
    const runLabel = ((await runLink.textContent()) || "").trim();

    await page.goto("/qa/qc/dashboard");
    await page.getByTestId("qc-tab-bench").click();
    const benchRun = page.locator(
      `[data-testid^="bench-qc-run-"][data-testid$="-${AMYLASE_TEST}-MANUAL"]`,
    );
    await expect(benchRun).toHaveText(runLabel, { timeout: LONG_TIMEOUT });
    await benchRun.click();
    await expect(page).toHaveURL(
      new RegExp(`/Results\\?run=${runLabel.replace("RUN-", "")}$`),
    );
    await expect(page.getByTestId("run-settings")).toContainText(
      "Manual entry",
      { timeout: LONG_TIMEOUT },
    );
  });
});
