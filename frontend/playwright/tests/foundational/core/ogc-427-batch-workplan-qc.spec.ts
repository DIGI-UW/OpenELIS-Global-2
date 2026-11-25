import { APIRequestContext, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { csrfToken } from "../../../helpers/api-session";
import {
  AMYLASE_TEST,
  API,
  SERUM,
  orderTests,
} from "../../../helpers/results-nce-ui";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

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

interface Seed {
  batchIds: number[];
  itemId: number;
  lotNumbers: string[];
  controlLotId: string;
  controlLotNumber: string;
}

let run = "";

async function json(
  page: Page,
  method: "POST" | "PUT",
  path: string,
  data: unknown,
) {
  const headers = { "X-CSRF-Token": await csrfToken(page) };
  const request: APIRequestContext = page.request;
  const response =
    method === "POST"
      ? await request.post(`${API}${path}`, { headers, data })
      : await request.put(`${API}${path}`, { headers, data });
  expect(response.status(), `${method} ${path}`).toBeLessThan(300);
  return response.status() === 204 ? null : response.json();
}

async function seedReagentAndControl(page: Page): Promise<Seed> {
  run = `${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;
  const item = await json(page, "POST", "/rest/inventory/items", {
    code: `PW427-${run}`,
    name: `PW427 Amylase reagent ${run}`,
    units: "mL",
    category: "REAGENT",
  });
  const lotNumbers = [`PW427-A-${run}`, `PW427-B-${run}`];
  const expirations = ["2030-01-31T00:00:00Z", "2030-06-30T00:00:00Z"];
  for (let index = 0; index < lotNumbers.length; index++) {
    await json(page, "POST", "/rest/inventory/lots", {
      inventoryItem: { id: item.id },
      lotNumber: lotNumbers[index],
      initialQuantity: 50,
      currentQuantity: 50,
      qcStatus: "PASSED",
      status: "ACTIVE",
      expirationDate: expirations[index],
    });
  }
  await json(page, "POST", `/rest/test-catalog/${AMYLASE_TEST}/reagents`, {
    reagentId: item.id,
    usageType: "PRIMARY",
    quantityPerTest: 0.5,
    quantityUnit: "mL",
  });
  const controlLotNumber = `PW427-CTL-${run}`;
  const controlLot = await json(page, "POST", "/rest/qc/controlLot", {
    productName: "PW427 chemistry control",
    lotNumber: controlLotNumber,
    controlLevel: "NORMAL",
    testId: Number(AMYLASE_TEST),
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
  return {
    batchIds: [],
    itemId: item.id,
    lotNumbers,
    controlLotId: controlLot.id,
    controlLotNumber,
  };
}

async function retire(page: Page, seed: Seed) {
  const headers = { "X-CSRF-Token": await csrfToken(page) };
  // Only this test's own batches: the signed-in user may hold others.
  for (const batchId of seed.batchIds) {
    await page.request.put(
      `${API}/rest/batch-workplans/batches/${batchId}/status`,
      {
        headers,
        data: { status: "ARCHIVED" },
      },
    );
  }
  await page.request.put(
    `${API}/rest/qc/controlLot/${seed.controlLotId}/deactivate`,
    { headers },
  );
  await page.request.delete(
    `${API}/rest/test-catalog/${AMYLASE_TEST}/reagents/${seed.itemId}`,
    { headers },
  );
  await page.request.put(
    `${API}/rest/inventory/items/${seed.itemId}/deactivate`,
    {
      headers,
    },
  );
}

async function createBatch(
  page: Page,
  seed: Seed,
  accession: string,
  name: string,
) {
  await page.goto("/Workplan", { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  await expect(
    main.getByRole("heading", { name: "Batch Workplan" }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await main.getByLabel("Search", { exact: true }).fill(accession);
  const checkbox = main.getByLabel(`Select ${accession}`);
  await expect(checkbox).toBeAttached({ timeout: LONG_TIMEOUT });
  await main
    .locator(`label[for="${await checkbox.getAttribute("id")}"]`)
    .click();
  await main.getByLabel("Batch name (optional)").fill(name);
  const created = page.waitForResponse(
    (response) =>
      response.url().endsWith("/rest/batch-workplans/batches") &&
      response.request().method() === "POST",
  );
  await main.getByRole("button", { name: "Create batch (1)" }).click();
  const batchId: number = (await (await created).json()).id;
  seed.batchIds.push(batchId);
  const tile = page.getByTestId(`batch-tile-${batchId}`);
  await expect(tile.getByText(name)).toBeVisible();
  return { batchId, tile };
}

async function assignReagent(
  page: Page,
  batchId: number,
  seed: Seed,
  lotIndex: number,
) {
  const tile = page.getByTestId(`batch-tile-${batchId}`);
  const reagent = tile.getByLabel("Reagent", { exact: true });
  await expect(
    reagent.locator("option", { hasText: `PW427 Amylase reagent ${run}` }),
  ).toBeAttached();
  await reagent.selectOption({
    label: `PW427 Amylase reagent ${run} (Amylase)`,
  });
  await expect(tile.getByText(seed.lotNumbers[0])).toBeVisible();
  await expect(tile.getByText("FEFO Suggested")).toBeVisible();
  await tile.getByText(seed.lotNumbers[lotIndex], { exact: true }).click();
  const saved = page.waitForResponse((response) =>
    response.url().endsWith(`/batches/${batchId}/reagent`),
  );
  await tile.getByRole("button", { name: "Save reagent and lot" }).click();
  expect((await saved).status()).toBe(200);
}

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
      `PW427 bench ${run}`,
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
    await json(page, "POST", "/rest/qc/results", {
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
      `PW427 override ${run}`,
    );
    await assignReagent(page, batchId, seed, 0);
    const qcTag = tile.getByTestId(`batch-${batchId}-qc-tag`);
    await expect(qcTag).toHaveText(/QC failed|QC violation/);

    await tile.getByRole("button", { name: "Generate workplan" }).click();
    const dialog = page
      .getByRole("dialog")
      .filter({ hasText: /Most recent QC failed|Active Westgard violation/ });
    await expect(dialog).toBeVisible();
    const released = page.waitForResponse((response) =>
      response.url().endsWith(`/batches/${batchId}/generate`),
    );
    await dialog
      .getByRole("button", { name: /Proceed and create NCE/ })
      .click();
    const body = await (await released).json();
    const nceNumber = body.releases[0].nceNumber;
    expect(nceNumber).toBeTruthy();
    const nceTag = tile.getByText(`NCE: ${nceNumber}`);
    await expect(nceTag).toBeVisible();
    await expect(nceTag.locator("xpath=ancestor::a")).toHaveAttribute(
      "href",
      `/ViewNonConformingEvent?nceNumber=${nceNumber}`,
    );
  });
});
