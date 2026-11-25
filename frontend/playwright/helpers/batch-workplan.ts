import { APIRequestContext, Page } from "@playwright/test";
import { expect } from "./test-base";
import { csrfToken } from "./api-session";
import { AMYLASE_TEST, API } from "./results-nce-ui";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "./timeouts";

/**
 * Batch Workplan fixtures (OGC-427), shared by the batch workplan specs: a
 * reagent with two lots and a control lot for Amylase, created through the
 * endpoints the app's own screens use and retired again, and the screen steps
 * that create a batch and assign its reagent lot.
 */

export interface Seed {
  run: string;
  batchIds: number[];
  itemId: number;
  lotNumbers: string[];
  controlLotId: string;
  controlLotNumber: string;
}

export async function batchJson(
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

export async function seedReagentAndControl(page: Page): Promise<Seed> {
  const run = `${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;
  const item = await batchJson(page, "POST", "/rest/inventory/items", {
    code: `PW427-${run}`,
    name: `PW427 Amylase reagent ${run}`,
    units: "mL",
    category: "REAGENT",
  });
  const lotNumbers = [`PW427-A-${run}`, `PW427-B-${run}`];
  const expirations = ["2030-01-31T00:00:00Z", "2030-06-30T00:00:00Z"];
  for (let index = 0; index < lotNumbers.length; index++) {
    await batchJson(page, "POST", "/rest/inventory/lots", {
      inventoryItem: { id: item.id },
      lotNumber: lotNumbers[index],
      initialQuantity: 50,
      currentQuantity: 50,
      qcStatus: "PASSED",
      status: "ACTIVE",
      expirationDate: expirations[index],
    });
  }
  await batchJson(page, "POST", `/rest/test-catalog/${AMYLASE_TEST}/reagents`, {
    reagentId: item.id,
    usageType: "PRIMARY",
    quantityPerTest: 0.5,
    quantityUnit: "mL",
  });
  const controlLotNumber = `PW427-CTL-${run}`;
  const controlLot = await batchJson(page, "POST", "/rest/qc/controlLot", {
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
    run,
    batchIds: [],
    itemId: item.id,
    lotNumbers,
    controlLotId: controlLot.id,
    controlLotNumber,
  };
}

export async function retire(page: Page, seed: Seed) {
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

export async function createBatch(
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

export async function assignReagent(
  page: Page,
  batchId: number,
  seed: Seed,
  lotIndex: number,
) {
  const tile = page.getByTestId(`batch-tile-${batchId}`);
  const reagent = tile.getByLabel("Reagent", { exact: true });
  await expect(
    reagent.locator("option", { hasText: `PW427 Amylase reagent ${seed.run}` }),
  ).toBeAttached();
  await reagent.selectOption({
    label: `PW427 Amylase reagent ${seed.run} (Amylase)`,
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
