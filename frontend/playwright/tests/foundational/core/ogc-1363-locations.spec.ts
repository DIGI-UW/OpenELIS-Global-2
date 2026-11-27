import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1363 — Locations & Organizations: one admin menu for facilities, wards,
 * sampling sites and geographic areas. A facility is added with its code,
 * found by that code, renamed with its former name still searchable, given a
 * ward, deactivated with Undo; a sampling site added here is a vector sampling
 * site; a CSV import previews before it writes and the export round-trips.
 */

const API = "/api/OpenELIS-Global";
const LOCATIONS = "/MasterListsPage/locations";
const SEARCH_PLACEHOLDER = "Search by name, code or any identifier";

async function deactivate(page: Page, ids: string[]) {
  if (!ids.length) {
    return;
  }
  await page.evaluate(
    async ({ api, ids }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      await fetch(`${api}/rest/locations/organizations/active`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": csrf,
        },
        body: JSON.stringify({ ids, active: false, includeChildren: true }),
      });
    },
    { api: API, ids },
  );
}

async function facilityTypeName(page: Page): Promise<string> {
  const response = await page.request.get(`${API}/rest/locations/lists`);
  expect(response.ok()).toBeTruthy();
  const lists = await response.json();
  const types = (lists.facilityTypes || []) as Array<{ name: string }>;
  expect(types.length).toBeGreaterThan(0);
  return types[0].name;
}

async function rowId(page: Page, code: string): Promise<string> {
  const search = page.getByPlaceholder(SEARCH_PLACEHOLDER);
  await search.fill(code);
  const rows = page.locator('[data-testid^="locations-row-"]');
  await expect(rows).toHaveCount(1, { timeout: UI_TIMEOUT });
  const testId = await rows.first().getAttribute("data-testid");
  return (testId || "").replace("locations-row-", "");
}

test.describe("OGC-1363 Locations & Organizations", () => {
  test("the admin menu has one Locations entry and the old routes land on it", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator(".cds--side-nav")).toContainText(
      "Locations & Organizations",
      { timeout: NAV_TIMEOUT },
    );
    await expect(
      page.getByText("Locations & Organizations").first(),
    ).toBeVisible();

    await page.goto("/MasterListsPage/organizationManagement", {
      waitUntil: "domcontentloaded",
    });
    await expect(page).toHaveURL(/\/MasterListsPage\/locations$/, {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByTestId("locations-organizations")).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    await page.goto("/MasterListsPage/vectorSurveillanceSetup/sampling-sites", {
      waitUntil: "domcontentloaded",
    });
    await expect(page).toHaveURL(/\/MasterListsPage\/locations\/sites$/, {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByTestId("locations-sites")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  test("a facility is added with its code, renamed with its former name kept, given a ward and deactivated with undo", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const stamp = Date.now().toString().slice(-7);
    const name = `Bumbu Clinic ${stamp}`;
    const renamed = `Lae Urban Centre ${stamp}`;
    const code = `BC${stamp}`;
    const created: string[] = [];

    try {
      await page.goto(LOCATIONS, {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      await page.getByTestId("locations-add").click();
      const form = page.getByTestId("locations-form-new");
      await expect(form).toBeVisible({ timeout: UI_TIMEOUT });
      await form.locator("#name-new").fill(name);
      await form.locator("#types-new").click();
      await page.getByRole("listbox").getByRole("option").first().click();
      await page.keyboard.press("Escape");
      await form.getByLabel("Code Value").fill(code);
      await form.getByTestId("locations-save").click();
      await expect(page.getByText(`${name} added.`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });

      const id = await rowId(page, code);
      expect(id).not.toBe("");
      created.push(id);
      const rows = page.locator('[data-testid^="locations-row-"]');
      await expect(rows.first()).toContainText(name);
      await expect(rows.first()).toContainText(code);

      await page.getByTestId(`locations-edit-${id}`).click();
      const edit = page.getByTestId(`locations-form-${id}`);
      await expect(edit.locator(`#name-${id}`)).toHaveValue(name, {
        timeout: UI_TIMEOUT,
      });
      await edit.locator(`#name-${id}`).fill(renamed);
      await edit.getByTestId("locations-save").click();
      await expect(page.getByText(`${renamed} saved.`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });

      const search = page.getByPlaceholder(SEARCH_PLACEHOLDER);
      await search.fill(name);
      await expect(rows).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(rows.first()).toContainText(renamed);
      await expect(rows.first()).toContainText(`Formerly ${name}`);

      await page.getByTestId(`locations-edit-${id}`).click();
      await expect(edit).toBeVisible({ timeout: UI_TIMEOUT });
      await edit.getByTestId("locations-add-ward").click();
      const draft = edit.getByTestId("locations-ward-draft");
      await draft
        .getByPlaceholder("Ward / dept name")
        .fill(`Outpatient ${stamp}`);
      await draft
        .locator("select")
        .first()
        .selectOption({ label: "Outpatient" });
      await edit.getByTestId("locations-save-wards").click();
      await expect(
        edit
          .locator('[data-testid^="locations-ward-"]')
          .filter({ hasText: `Outpatient ${stamp}` }),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      await edit.getByRole("button", { name: /^History/ }).click();
      await expect(edit.getByTestId("locations-history")).toContainText(name, {
        timeout: UI_TIMEOUT,
      });
      await edit.getByRole("button", { name: "Cancel" }).click();

      await search.fill(code);
      await expect(rows).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(rows.first()).toContainText("1");
      const toggle = page.locator(`#active-${id}`);
      await page.locator(`#active-${id}_label`).click();
      const guard = page.getByRole("dialog");
      await expect(guard).toContainText("1 active wards", {
        timeout: UI_TIMEOUT,
      });
      await guard.getByRole("button", { name: / only$/ }).click();
      await expect(page.getByText(`${renamed} deactivated.`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page.getByRole("button", { name: "Undo" }).click();
      await expect(page.getByText(`${renamed} reactivated.`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(toggle).toHaveAttribute("aria-checked", "true");
    } finally {
      await deactivate(page, created);
    }
  });

  test("a sampling site added here is a vector sampling site, and a CSV import previews before it writes", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const stamp = Date.now().toString().slice(-7);
    const siteName = `Bumbu light trap ${stamp}`;
    const siteCode = `VT${stamp}`;
    const imported = `Imported Clinic ${stamp}`;
    const importedCode = `IMP${stamp}`;
    const created: string[] = [];

    try {
      await page.goto(`${LOCATIONS}/sites`, {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      await page.getByTestId("locations-add").click();
      const form = page.getByTestId("locations-form-new");
      await expect(form).toBeVisible({ timeout: UI_TIMEOUT });
      await form.locator("#name-new").fill(siteName);
      await form.getByLabel("Code Value").fill(siteCode);
      await form.getByTestId("locations-save").click();
      await expect(page.getByText(`${siteName} added.`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      created.push(await rowId(page, siteCode));

      const vector = await page.request.get(
        `${API}/rest/admin/vector/sampling-sites`,
      );
      expect(vector.ok()).toBeTruthy();
      const sites = (await vector.json()) as Array<{ name?: string }>;
      expect(sites.some((site) => site.name === siteName)).toBeTruthy();

      const typeName = await facilityTypeName(page);
      await page.goto(`${LOCATIONS}/import`, {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      const fileName = `organizations-pw-${stamp}.csv`;
      await page.locator('input[type="file"]').setInputFiles({
        name: fileName,
        mimeType: "text/csv",
        buffer: Buffer.from(
          `type,code,name,active\n${typeName},${importedCode},${imported},Y\n`,
        ),
      });
      await expect(page.getByTestId("locations-import-file")).toHaveText(
        fileName,
      );
      await page.getByTestId("locations-import-preview").click();
      await expect(
        page.getByTestId("locations-import-count-new"),
      ).toContainText("1", { timeout: UI_TIMEOUT });
      await expect(page.getByText(imported)).toBeVisible();

      const listed = await page.request.get(
        `${API}/rest/locations/organizations?view=organizations&q=${importedCode}&status=all`,
      );
      expect(listed.ok()).toBeTruthy();
      expect((await listed.json()).total).toBe(0);

      await page.getByTestId("locations-import-apply").click();
      await page
        .getByRole("dialog")
        .getByRole("button", { name: "Apply" })
        .click();
      await expect(page.getByTestId("locations-import-done")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });

      await page.goto(LOCATIONS, {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      const importedId = await rowId(page, importedCode);
      created.push(importedId);
      await expect(
        page.getByTestId(`locations-row-${importedId}`),
      ).toContainText(imported);

      const exported = await page.request.get(
        `${API}/rest/locations/export?view=organizations&q=${importedCode}`,
      );
      expect(exported.ok()).toBeTruthy();
      const csv = await exported.text();
      expect(csv.startsWith("type,code,name")).toBeTruthy();
      expect(csv).toContain(`${importedCode},${imported}`);
    } finally {
      await deactivate(page, created);
    }
  });
});
