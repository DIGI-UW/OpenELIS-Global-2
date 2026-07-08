import { Page, expect } from "@playwright/test";

/** API context path — the JSESSIONID is scoped to the /api/OpenELIS-Global webapp. */
const API_PREFIX = "/api/OpenELIS-Global";

/** CSRF token saved by auth.setup into storageState localStorage (key "CSRF"). */
async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

export interface VectorSite {
  id?: number;
  code: string;
  name: string;
}

/**
 * Create a vector sampling site via the admin REST API (POST
 * /rest/admin/vector/sampling-sites) so an add-order E2E has a site to select.
 * Reference/transactional test data is seeded via the API, not SQL fixtures.
 * The caller should pass a unique code per run to avoid collisions.
 */
export async function seedVectorSite(
  page: Page,
  site: VectorSite,
): Promise<VectorSite> {
  const csrfToken = await getCsrfToken(page);
  const response = await page.request.post(
    `${API_PREFIX}/rest/admin/vector/sampling-sites`,
    {
      data: { code: site.code, name: site.name },
      headers: { "X-CSRF-Token": csrfToken },
    },
  );
  if (!response.ok()) {
    const text = await response.text().catch(() => "");
    throw new Error(
      `seedVectorSite: POST ${site.code} → HTTP ${response.status()} ${text.slice(0, 200)}`,
    );
  }
  const body = await response.json();
  return { id: body.id, code: site.code, name: site.name };
}

/** GET a JSON endpoint (session cookie carries auth); throws on non-2xx. */
async function apiGet<T = unknown>(page: Page, path: string): Promise<T> {
  const res = await page.request.get(`${API_PREFIX}${path}`);
  if (!res.ok()) {
    throw new Error(
      `GET ${path} → HTTP ${res.status()} ${(await res.text()).slice(0, 300)}`,
    );
  }
  return res.json() as Promise<T>;
}

/** POST JSON with the CSRF token; throws on non-2xx. */
async function apiPost(page: Page, path: string, data: unknown): Promise<void> {
  const csrfToken = await getCsrfToken(page);
  const res = await page.request.post(`${API_PREFIX}${path}`, {
    data: data as object,
    headers: { "X-CSRF-Token": csrfToken },
  });
  if (!res.ok()) {
    throw new Error(
      `POST ${path} → HTTP ${res.status()} ${(await res.text()).slice(0, 300)}`,
    );
  }
}

/**
 * Seed a POSITIVE Anopheles pool through the real workflow (no SQL): order a
 * Mosquito collection with both pathogen tests via the UI, identify its
 * specimens as Anopheles (CONFIRMED), then enter POSITIVE results via
 * LogbookResults. This produces the data the dashboard's MIR, positivity, and
 * sporozoite panels compute from — the catalog ships the significance
 * classification (configuration/test-results), this seeds the transactional
 * results that reference it.
 */
export async function seedVectorPositivity(
  page: Page,
): Promise<{ labNo: string; siteName: string }> {
  const stamp = Date.now().toString().slice(-6);
  const siteCode = `E2E-POS-${stamp}`;
  const siteName = `E2E Positivity Site ${stamp}`;
  await seedVectorSite(page, { code: siteCode, name: siteName });

  // 1. Order a Mosquito collection with both pathogen tests (creates analyses).
  await page.goto("/order/vector/enter");
  await page.locator(".generate-link").click();
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: 15_000,
  });
  const labNo = await page.locator("#labNumber").inputValue();

  await page.locator("#vec-site-search").fill(siteName);
  await page
    .locator(".search-results tr", { hasText: siteCode })
    .getByRole("button", { name: "Select" })
    .click();
  await page.locator("#sampleType-0").selectOption({ label: "Mosquito" });

  // Order both pathogen tests. Filter the test list, then click the (Carbon
  // visually-hidden) checkbox via its label.
  for (const testTerm of ["Malaria", "CSP"]) {
    await page.locator("#testSearch-0").fill(testTerm);
    await page.locator('label[for^="test-0-"]').first().click();
  }
  await page.locator("#collectedVolume-0").fill("8");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByText("Sample Order Entry has been saved successfully").first(),
  ).toBeVisible({ timeout: 20_000 });

  // 2. Identify the pool's specimens as Anopheles (CONFIRMED) — MIR is by species.
  const species = await apiGet<Array<{ id: number; genus: string }>>(
    page,
    "/rest/admin/vector/species",
  );
  const anopheles = species.find(
    (s) => (s.genus || "").toLowerCase() === "anopheles",
  );
  if (!anopheles) {
    throw new Error(
      "seedVectorPositivity: no Anopheles species in the catalog",
    );
  }
  const worklist = await apiGet<
    Array<{ sampleId: number; vectorPoolId: number; accessionNumber: string }>
  >(page, "/rest/vector/identification/worklist");
  const row = worklist.find((r) => r.accessionNumber === labNo);
  if (!row) {
    throw new Error(`seedVectorPositivity: no worklist row for ${labNo}`);
  }
  // getSpecimensForLot is keyed by the vector pool id (as the UI passes it).
  const specimens = await apiGet<Array<{ sampleItemId: number }>>(
    page,
    `/rest/vector/identification/lots/${row.vectorPoolId}/specimens`,
  );
  await apiPost(page, "/rest/vector/identification/specimens/bulk-identify", {
    sampleItemIds: specimens.map((s) => s.sampleItemId),
    vectorSpeciesId: anopheles.id,
    identificationMethod: "MORPHOLOGICAL",
    confidence: "CONFIRMED",
  });

  // 3. Enter POSITIVE results for both pathogen analyses via LogbookResults.
  const logbook = await apiGet<{
    testResult?: Array<Record<string, unknown>>;
  }>(page, `/rest/LogbookResults?labNumber=${labNo}`);
  const body = JSON.parse(JSON.stringify(logbook));
  for (const item of body.testResult ?? []) {
    const dict = (item.dictionaryResults ?? []) as Array<{
      id: string;
      value: string;
    }>;
    const positive = dict.find((d) =>
      (d.value || "").toLowerCase().startsWith("positive"),
    );
    if (!positive) continue;
    item.reportable = item.reportable === "N" ? false : true;
    item.resultValue = positive.id;
    item.shadowResultValue = positive.id;
    item.isModified = true;
    delete item.result;
  }
  await apiPost(page, "/rest/LogbookResults", body);

  return { labNo, siteName };
}
