import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1420 — Locations & Organizations fixes from the deep test: a file saved
 * by Excel as "CSV UTF-8" imports as typed and names the columns it ignores,
 * a new area name can be typed key by key, and a save message is shown where
 * the user is looking, however far down the page the form is.
 */

const API = "/api/OpenELIS-Global";
const LOCATIONS = "/MasterListsPage/locations";

async function apiCall(
  page: Page,
  method: string,
  path: string,
  body?: unknown,
) {
  return page.evaluate(
    async ({ api, method, path, body }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const response = await fetch(`${api}${path}`, {
        method,
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-CSRF-Token": csrf },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      return { status: response.status, json: await response.json() };
    },
    { api: API, method, path, body },
  );
}

test.describe("OGC-1420 Locations & Organizations fixes", () => {
  test("an Excel CSV UTF-8 file previews with its accents and names the column it ignores", async ({
    page,
  }) => {
    await page.goto(`${LOCATIONS}/import`, { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("locations-import")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    const name = `QA 1420 Clinique Sainte-Thérèse ${Date.now()}`;
    const csv = Buffer.concat([
      Buffer.from([0xef, 0xbb, 0xbf]),
      Buffer.from(`type,name,Colour\nreferring clinic,${name},blue\n`, "utf8"),
    ]);
    await page.locator('input[type="file"]').setInputFiles({
      name: "organizations-qa-1420.csv",
      mimeType: "text/csv",
      buffer: csv,
    });
    await expect(page.getByTestId("locations-import-file")).toHaveText(
      "organizations-qa-1420.csv",
    );
    const preview = page.waitForResponse((response) =>
      response.url().includes("/rest/locations/import/preview"),
    );
    await page.getByTestId("locations-import-preview").click();
    await preview;

    await expect(page.getByTestId("locations-import-ignored")).toContainText(
      "Colour",
      { timeout: UI_TIMEOUT },
    );
    await expect(page.getByTestId("locations-import-count-new")).toContainText(
      "1",
    );
    await expect(page.getByText(name)).toBeVisible();
    await expect(page.getByTestId("locations-import-file-error")).toHaveCount(
      0,
    );
  });

  test("a new geographic area name keeps every key typed", async ({ page }) => {
    await page.goto(`${LOCATIONS}/areas`, { waitUntil: "domcontentloaded" });
    const addTop = page.getByTestId("locations-area-add-top");
    await expect(addTop).toBeVisible({ timeout: NAV_TIMEOUT });
    const levels = await page.request.get(`${API}/rest/locations/areas/levels`);
    test.skip(
      ((await levels.json()) || []).length === 0,
      "no geographic levels configured",
    );
    await expect(addTop).toBeEnabled({ timeout: UI_TIMEOUT });
    await addTop.click();
    const field = page.locator("#locations-new-area-name");
    await field.click();
    await page.keyboard.type("Kerema 1420", { delay: 30 });

    await expect(field).toHaveValue("Kerema 1420");
    await expect(field).toBeFocused();
    await page
      .getByTestId("locations-area-add-row")
      .getByRole("button", { name: "Cancel" })
      .click();
  });

  test("a save at the bottom of a long page says so where the user is looking", async ({
    page,
  }) => {
    await page.goto(LOCATIONS, { waitUntil: "domcontentloaded" });
    const lists = await page.request.get(`${API}/rest/locations/lists`);
    const typeId = ((await lists.json()).facilityTypes || [])[0].id;
    const created = await apiCall(
      page,
      "POST",
      "/rest/locations/organizations",
      {
        kind: "facility",
        name: `QA 1420 Low Save ${Date.now()}`,
        typeIds: [typeId],
        identifiers: [],
      },
    );
    expect(created.status).toBe(201);
    const id = created.json.detail.row.id;
    try {
      await page.goto(`${LOCATIONS}?id=${id}`, {
        waitUntil: "domcontentloaded",
      });
      const form = page.getByTestId(`locations-form-${id}`);
      await expect(form).toBeVisible({ timeout: NAV_TIMEOUT });
      await form.locator(`#contact-${id}`).fill("QA 1420 contact");
      await form.getByTestId("locations-save").scrollIntoViewIfNeeded();
      await form.getByTestId("locations-save").click();

      const message = page.locator(".locationsToast").first();
      await expect(message).toContainText("saved.", { timeout: UI_TIMEOUT });
      await expect(message).toBeInViewport();
    } finally {
      await apiCall(page, "POST", "/rest/locations/organizations/active", {
        ids: [id],
        active: false,
        includeChildren: true,
      });
    }
  });
});
