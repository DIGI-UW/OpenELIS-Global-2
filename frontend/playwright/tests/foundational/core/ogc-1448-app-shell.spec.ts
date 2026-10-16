import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1448, three app-shell defects:
 * - Logout behind a proxy on a non-default port: the server's redirect after
 *   the logout POST lost the port, the app followed it, the request failed
 *   and the page never left. The redirect is now not followed; the app opens
 *   its own login page. The lost port is simulated by rewriting the
 *   redirect's Location to a host that does not answer. The login page then
 *   checked the session with the cookie Logout had ended, was redirected to
 *   the login page, and took that for an outage: "Can't reach the server"
 *   showed for a second. It now asks again with the new cookie.
 * - Sortable columns read "Click to sort rows by [object Object] header"
 *   when the header was an element, and "by  header" when it was empty.
 * - The home Dashboard had no h1, and neither had the other screens walked
 *   for the fix: their titles were h2 or h3 elements.
 */

const LOGOUT = "**/api/OpenELIS-Global/Logout";
const SORT_DESCRIPTION = ".cds--table-sort__description";

const sortDescriptions = async (page) =>
  page.locator(SORT_DESCRIPTION).allTextContents();

const expectNamedSortColumns = (descriptions: string[]) => {
  expect(descriptions.length).toBeGreaterThan(0);
  for (const text of descriptions) {
    expect(text).not.toContain("[object Object]");
    expect(text).not.toMatch(/sort rows by\s+header/);
  }
};

test.describe("App shell (OGC-1448)", () => {
  test.describe("Logout", () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test("lands on this origin's login page when the server's redirect loses the port", async ({
      page,
    }) => {
      test.setTimeout(NAV_TIMEOUT * 3);
      const usernameInput = page.locator("#loginName");
      const sideNav = page.getByRole("navigation", {
        name: "Side navigation",
      });

      await page.goto("/login", { waitUntil: "domcontentloaded" });
      await expect(usernameInput).toBeVisible({ timeout: NAV_TIMEOUT });
      await usernameInput.fill(process.env.TEST_USER || "admin");
      await page
        .locator("#password")
        .fill(process.env.TEST_PASS || "adminADMIN!");
      await page.locator('[data-cy="loginButton"]').click();
      await expect(sideNav).toBeVisible({ timeout: NAV_TIMEOUT });

      await page.route(LOGOUT, async (route) => {
        const response = await route.fetch({ maxRedirects: 0 });
        await route.fulfill({
          response,
          headers: {
            ...response.headers(),
            location: "https://ogc-1448.invalid/api/OpenELIS-Global/LoginPage",
          },
        });
      });

      await page.getByRole("button", { name: "User", exact: true }).click();
      const outageNoticeShown = page
        .getByTestId("server-reconnect")
        .waitFor({ state: "visible", timeout: 5000 })
        .then(
          () => true,
          () => false,
        );
      await page
        .locator(".headerPanel")
        .getByText("Logout", { exact: true })
        .click();

      await expect(page).toHaveURL(/\/login$/, { timeout: NAV_TIMEOUT });
      await expect(usernameInput).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(sideNav).toBeHidden();
      expect(await outageNoticeShown).toBe(false);
      const signedOut = await page.request.get("/api/OpenELIS-Global/session");
      expect((await signedOut.json()).authenticated).toBe(false);
    });
  });

  test("the Dashboard has one h1, also with a tile opened", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const headings = page.getByRole("heading", { level: 1 });
    await expect(headings).toHaveText(["Dashboard"], { timeout: NAV_TIMEOUT });

    await page
      .locator(".dashboard-tile")
      .filter({ hasText: "Orders Entered By Users Today" })
      .click();
    await expect(page.locator(".dashboard-tile__value-view")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(headings).toHaveText(["Dashboard"]);
    await expect(
      page.getByRole("columnheader", { name: /Orders Entered/ }),
    ).toBeVisible();
    expectNamedSortColumns(await sortDescriptions(page));
  });

  for (const [path, title] of [
    ["/MasterListsPage/locations", "Organizations"],
    ["/MasterListsPage/locations/sites", "Sampling Sites"],
    ["/PathologyDashboard", "Pathology"],
    ["/ImmunohistochemistryDashboard", "Immunohistochemistry"],
    ["/CytologyDashboard", "Cytology"],
    ["/NoteBookDashboard", "Notebook"],
    ["/genericProgram", "Order Programs"],
    ["/inventory", "Items"],
    ["/Storage/racks", "Storage Management"],
    ["/qa/qms/audit-trail?type=system", "System Audit Trail"],
    ["/qa/qms/audit-trail?type=order", "Audit Trail"],
    ["/SamplePatientEntry", "Test Request"],
    ["/order/clinical/enter", "Enter Order"],
  ]) {
    test(`${path} has one h1, its title "${title}"`, async ({ page }) => {
      await page.goto(path, { waitUntil: "domcontentloaded" });
      await expect(page.getByRole("heading", { level: 1 })).toHaveText(
        [title],
        { timeout: NAV_TIMEOUT },
      );
    });
  }

  for (const [screen, path] of [
    ["Locations & Organizations", "/MasterListsPage/locations"],
    ["Pathology", "/PathologyDashboard"],
    ["Immunohistochemistry", "/ImmunohistochemistryDashboard"],
    ["Storage racks", "/Storage/racks"],
    ["QC control lots", "/qa/qc/control-lots"],
  ]) {
    test(`${screen} names every sortable column`, async ({ page }) => {
      await page.goto(path, { waitUntil: "domcontentloaded" });
      await expect(page.locator(SORT_DESCRIPTION).first()).toBeAttached({
        timeout: NAV_TIMEOUT,
      });
      expectNamedSortColumns(await sortDescriptions(page));
    });
  }
});
