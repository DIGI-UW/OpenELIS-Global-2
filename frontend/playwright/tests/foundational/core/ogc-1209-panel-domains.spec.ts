import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1209: a panel is filed under the domain set on the panel, never worked
 * out from the specimens its member tests use, so the Environmental and Vector
 * filters return the panels an administrator is looking for instead of an
 * empty table. A panel that does reach a specimen from another domain says so
 * on its row.
 *
 * The panel under test is created by the test, so the case holds on any
 * database rather than depending on which catalog the instance was seeded
 * with.
 */

const API_PREFIX = "/api/OpenELIS-Global";
const PANELS_LIST = "/MasterListsPage/TestCatalogList?entity=panels";

const run = Date.now().toString().slice(-6);
const PANEL_NAME = `E2E Vector ${run}`;

interface PanelRow {
  id: string;
  domain: string;
  sampleTypesOutsideDomain: string[];
}

/** CSRF token saved by auth.setup into storageState localStorage (key "CSRF"). */
const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

/** Creates a panel and files it under one domain, the way the editor does. */
const createPanelInDomain = async (
  page: Page,
  name: string,
  domain: string,
) => {
  const token = await csrfToken(page);
  const created = await page.request.post(
    `${API_PREFIX}/rest/test-catalog/panels`,
    { data: { name, active: false }, headers: { "X-CSRF-Token": token } },
  );
  expect(created.ok()).toBeTruthy();
  const { id } = await created.json();

  const saved = await page.request.put(
    `${API_PREFIX}/rest/test-catalog/panels/${id}/basic-info`,
    {
      data: { name, description: name, domain, active: false },
      headers: { "X-CSRF-Token": token },
    },
  );
  expect(saved.ok()).toBeTruthy();
  expect((await saved.json()).domain).toBe(domain);
  return id;
};

test.describe("Panel domains (OGC-1209)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("a panel filed under Vector keeps that domain and the Vector filter finds it", async ({
    page,
  }) => {
    await page.goto(PANELS_LIST, { waitUntil: "domcontentloaded" });
    const rows = page.locator("table tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: NAV_TIMEOUT });

    await createPanelInDomain(page, PANEL_NAME, "VECTOR");

    await page.goto(`${PANELS_LIST}&domain=VECTOR`, {
      waitUntil: "domcontentloaded",
    });
    const created = rows.filter({ hasText: PANEL_NAME });
    await expect(created).toHaveCount(1, { timeout: LONG_TIMEOUT });
    // Its Domain cell reads Vector, not the old CLINICAL default.
    await expect(created.getByText("Vector", { exact: true })).toBeVisible();

    // The filter that used to empty the table now excludes only other domains.
    await expect(rows.getByText("Clinical", { exact: true })).toHaveCount(0);

    await page.goto(`${PANELS_LIST}&domain=CLINICAL`, {
      waitUntil: "domcontentloaded",
    });
    await expect(rows.first()).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(rows.filter({ hasText: PANEL_NAME })).toHaveCount(0);
  });

  test("the row marks exactly the panels that reach a specimen outside their domain", async ({
    page,
  }) => {
    // The catalog decides how many panels are mixed; the screen must agree
    // with it exactly, marking those and no others.
    const response = await page.request.get(
      `${API_PREFIX}/rest/test-catalog/panels?includeInactive=true`,
    );
    expect(response.ok()).toBeTruthy();
    const mixed = ((await response.json()) as PanelRow[]).filter(
      (panel) => (panel.sampleTypesOutsideDomain || []).length > 0,
    );

    await page.goto(PANELS_LIST, { waitUntil: "domcontentloaded" });
    await expect(page.locator("table tbody tr").first()).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator('[data-cy^="panel-mixed-domain-"]')).toHaveCount(
      mixed.length,
    );

    for (const panel of mixed) {
      // The label names the offending specimens, so the row explains itself.
      await expect(
        page.locator(`[data-cy="panel-mixed-domain-${panel.id}"]`),
      ).toHaveAttribute(
        "title",
        `Sample types outside this panel's domain: ${panel.sampleTypesOutsideDomain.join(", ")}`,
      );
    }
  });
});
