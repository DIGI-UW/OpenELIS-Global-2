import type { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";

/**
 * OGC-1457: renaming a test (or any localized label) through
 * /rest/localizations is a catalog administration write. A lab technician is
 * refused on every write and nothing changes; an admin still saves.
 */

const API = "/api/OpenELIS-Global";

const letters = (n: number) =>
  Array.from(
    { length: n },
    () => "abcdefghijklmnopqrstuvwxyz"[Math.floor(Math.random() * 26)],
  ).join("");

const createLabTechnician = async (page: Page) => {
  await page.goto("/");
  const endpoint = `${API}/rest/UnifiedSystemUser`;
  const form = await (await page.request.get(endpoint)).json();
  const results = form.labUnitRoles.find(
    (role: { roleName: string }) => role.roleName === "Results",
  );
  expect(results?.roleId).toBeTruthy();
  const password = process.env.TEST_PASS;
  expect(
    password,
    "TEST_PASS is required for the disposable user",
  ).toBeTruthy();
  const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
  const username = `pwloc${letters(8)}`;
  const created = await page.request.post(endpoint, {
    headers: { "X-CSRF-Token": csrf },
    data: {
      userLoginName: username,
      userPassword: password,
      confirmPassword: password,
      userFirstName: "Localization",
      userLastName: username,
      expirationDate: form.expirationDate,
      timeout: form.timeout,
      accountActive: "Y",
      accountDisabled: "N",
      accountLocked: "N",
      allowCopyUserRoles: "N",
      selectedRoles: [],
      selectedTestSectionLabUnits: { AllLabUnits: [results.roleId] },
    },
  });
  expect(await created.json()).toEqual({
    forward: "redirect:/UnifiedSystemUser",
  });
  return username;
};

const signIn = async (browser: Browser, baseURL: string, username: string) => {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    baseURL,
    storageState: { cookies: [], origins: [] },
  });
  const page = await context.newPage();
  await page.goto("/login");
  await page.locator("#loginName").fill(username);
  await page.locator("#password").fill(process.env.TEST_PASS!);
  await page.locator('[data-cy="loginButton"]').click();
  await expect(page).toHaveURL(/\/$/);
  const session = await (await page.request.get(`${API}/session`)).json();
  return { context, page, csrf: session.csrf as string };
};

test.describe("Localization writes need the admin role (OGC-1457)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("a lab technician cannot rename a test; an admin can", async ({
    page,
    browser,
    baseURL,
  }) => {
    const tests = await (
      await page.request.get(
        `${API}/rest/test-catalog/tests?status=active&page=1&pageSize=1`,
      )
    ).json();
    const testId = tests.rows[0].testId;
    const refs = await (
      await page.request.get(
        `${API}/rest/test-catalog/tests/${testId}/localization`,
      )
    ).json();
    const nameRef = refs.fields.find(
      (f: { field: string }) => f.field === "name",
    );
    const localizationId = nameRef.localizationId;
    const before = (
      await (
        await page.request.get(`${API}/rest/localizations/${localizationId}`)
      ).json()
    ).translations;

    const technician = await createLabTechnician(page);
    const tech = await signIn(browser, baseURL!, technician);
    try {
      const headers = { "X-CSRF-Token": tech.csrf };
      const rename = await tech.page.request.put(
        `${API}/rest/localizations/${localizationId}/translations`,
        { headers, data: { en: `${before.en} renamed by a technician` } },
      );
      expect(rename.status()).toBe(403);
      const single = await tech.page.request.post(
        `${API}/rest/localizations/${localizationId}/translations/fr`,
        { headers, data: { value: "renommé par un technicien" } },
      );
      expect(single.status()).toBe(403);
      const imported = await tech.page.request.post(
        `${API}/rest/localizations/import`,
        {
          headers,
          data: [{ id: localizationId, translations: { en: "imported" } }],
        },
      );
      expect(imported.status()).toBe(403);
      const read = await tech.page.request.get(
        `${API}/rest/localizations/${localizationId}`,
      );
      expect(read.status()).toBe(200);
    } finally {
      await tech.context.close();
    }

    const after = (
      await (
        await page.request.get(`${API}/rest/localizations/${localizationId}`)
      ).json()
    ).translations;
    expect(after).toEqual(before);

    const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
    const adminSave = await page.request.put(
      `${API}/rest/localizations/${localizationId}/translations`,
      { headers: { "X-CSRF-Token": csrf }, data: { en: before.en } },
    );
    expect(adminSave.status()).toBe(200);
  });
});
