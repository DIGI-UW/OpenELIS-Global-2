import { randomInt, randomUUID } from "node:crypto";
import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import {
  batchJson,
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
 * OGC-1200 slice 9c (FR-B3, FR-D6, FR-D7): a result saved on a row in no run
 * is a run of one, which the row's Reagents and History sections show. A
 * control recorded on the row before the save opens the run of one; a failing
 * one holds the row until the test is decided, and the save then completes
 * the run.
 */

/**
 * The worklist filtered to the lab number, and to Biochemistry when the row's
 * control capture is needed (it is scoped by lab unit). Both filters go in the
 * URL, so the worklist loads once: choosing the unit and then searching loads
 * it twice, and a late load re-renders the table and closes an opened row.
 */
const openRow = async (page: Page, accession: string, labUnit = false) => {
  let unitFilter = "";
  if (labUnit) {
    const units = (await (
      await page.request.get(`${API}/rest/results-entry/lab-units`)
    ).json()) as Array<{ id: string; value: string }>;
    const biochemistry = units.find((unit) => unit.value === "Biochemistry");
    expect(biochemistry, "Biochemistry is a lab unit").toBeTruthy();
    unitFilter = `testSectionId=${biochemistry!.id}&`;
  }
  await page.goto(`/Results?${unitFilter}accessionNumber=${accession}`);
  await expect(page.getByText(accession).first()).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(page.locator(".cds--loading-overlay")).toHaveCount(0, {
    timeout: LONG_TIMEOUT,
  });
};

const saveValue = async (page: Page, value: string) => {
  await page.locator('input[id^="unifiedResultValue-"]').first().fill(value);
  await page.getByRole("button", { name: "Save" }).first().click();
  await signIfAsked(page);
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

const recordControl = async (
  page: Page,
  controlLotNumber: string,
  outcome: "PASS" | "FAIL",
  measured: string,
) => {
  await page.getByTestId("qc-outcome").selectOption(outcome);
  const lot = page.getByTestId("qc-lot");
  await lot.selectOption(
    (await lot
      .locator("option", { hasText: controlLotNumber })
      .getAttribute("value")) || "",
  );
  await page.getByRole("textbox", { name: "Measured value" }).fill(measured);
  await page.getByLabel("Expected value").fill("80");
  await page.getByLabel("Uncertainty (±)").fill("8");
  await page.getByTestId("qc-record").click();
};

const rowRunLabel = async (page: Page) => {
  const link = page.getByTestId("row-run-summary").getByTestId("row-run-link");
  await expect(link).toHaveText(/^RUN-\d+$/, { timeout: LONG_TIMEOUT });
  return ((await link.textContent()) || "").trim();
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
    await expandRow(page);
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
    await expandRow(page);
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

  test("a retest on a run of one lets the row's next control and value open a fresh run", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    seed = await seedReagentAndControl(page);
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);

    await openRow(page, accession, true);
    await expandRow(page);
    await recordControl(page, seed.controlLotNumber, "FAIL", "140");
    const decided = await rowRunLabel(page);

    await page.goto(`/Results?run=${decided.replace("RUN-", "")}`);
    await page
      .getByTestId(`run-qc-decide-${AMYLASE_TEST}`)
      .getByRole("button", { name: "Retest", exact: true })
      .click();
    await page.getByTestId(`run-qc-decide-confirm-${AMYLASE_TEST}`).click();
    await expect(page.getByTestId("run-settings")).toContainText("Complete", {
      timeout: LONG_TIMEOUT,
    });

    await openRow(page, accession, true);
    await expect(page.getByText(`Held: QC failed in ${decided}`)).toHaveCount(
      0,
    );
    await expandRow(page);
    await recordControl(page, seed.controlLotNumber, "PASS", "81");
    const fresh = await rowRunLabel(page);
    expect(fresh).not.toBe(decided);

    await openRow(page, accession);
    await saveValue(page, "79");
    await page.goto(`/Results?run=${fresh.replace("RUN-", "")}`);
    await expect(page.getByTestId("run-settings")).toContainText("Complete", {
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByTestId("run-progress")).toContainText("1 of 1");
  });

  test("a run opened by a control before the reagent use takes the lot of that use at the save", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    seed = await seedReagentAndControl(page);
    const accession = await orderTests(page, SERUM, [AMYLASE_TEST]);

    await openRow(page, accession, true);
    await expandRow(page);
    await recordControl(page, seed.controlLotNumber, "PASS", "81");
    const runLabel = await rowRunLabel(page);
    await page.getByTestId(`record-use-${seed.itemId}`).click();
    await expect(
      page.getByText("Recorded 0.5 mL — consumed FEFO across lots."),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    await saveValue(page, "81");
    await page.goto(`/Results?run=${runLabel.replace("RUN-", "")}`);
    await expect(
      page.getByTestId("run-setting-run.label.reagentLot"),
    ).toHaveText(seed.lotNumbers[0], { timeout: LONG_TIMEOUT });
    await expect(page.getByTestId("run-settings")).toContainText("Complete");
  });

  test("a test that requires a control holds its first value in a run of one until the control is recorded", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const run = randomUUID().slice(0, 8);
    const [target] = await seedNumericTests(
      page,
      run,
      [
        {
          name: `Run of one hold ${run}`,
          loinc: `${randomInt(1_000_000, 9_000_000)}-${randomInt(0, 9)}`,
        },
      ],
      { testSection: "Biochemistry", sampleType: "Serum" },
    );
    const controlLotNumber = `PW1200-CTL-${run}`;
    const controlLot = await batchJson(page, "POST", "/rest/qc/controlLot", {
      productName: "PW1200 hold control",
      lotNumber: controlLotNumber,
      controlLevel: "NORMAL",
      testId: Number(target.id),
      calculationMethod: "MANUFACTURER_FIXED",
      manufacturerMean: 80,
      manufacturerStdDev: 4,
      activationDate: new Date().toISOString(),
      expirationDate: "2030-12-31T12:00:00Z",
      status: "ACTIVE",
      qcFrequencyType: "PER_RUN",
      shiftDefinitionHours: 8,
      qcRequired: true,
    });
    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${target.id}/qc-targets`,
        { waitUntil: "domcontentloaded" },
      );
      await page.getByTestId("qc-policy-control-required").selectOption("yes");
      await page
        .getByTestId("qc-policy-control-when-missing")
        .selectOption("HOLD");
      await page.getByTestId("qc-targets-save").click();
      await expect(page.getByText("QC targets saved.")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      const { accession } = await createClinicalOrder(page, {
        testIds: [target.id],
        specimenName: "Serum",
      });

      await openRow(page, accession);
      await saveValue(page, "81");
      await expect(
        page.getByText(/lacks QC this test requires on every run/).first(),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      await openRow(page, accession, true);
      await expect(
        page.getByText(/^Held: QC missing in RUN-\d+$/).first(),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.locator('input[id^="unifiedResultValue-"]'),
      ).toHaveCount(0);
      await expandRow(page);
      const held = await rowRunLabel(page);
      await recordControl(page, controlLotNumber, "PASS", "81");
      await expect(page.getByTestId("qc-capture-message")).toHaveText(
        `Control result recorded on ${held}.`,
        { timeout: LONG_TIMEOUT },
      );
      await expect(page.getByText(`Held: QC missing in ${held}`)).toHaveCount(
        0,
      );

      await saveValue(page, "81");
      await page.goto(`/Results?run=${held.replace("RUN-", "")}`);
      await expect(page.getByTestId("run-settings")).toContainText("Complete", {
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("run-progress")).toContainText("1 of 1");
    } finally {
      await batchJson(
        page,
        "PUT",
        `/rest/qc/controlLot/${controlLot.id}/deactivate`,
        {},
      );
    }
  });
});
