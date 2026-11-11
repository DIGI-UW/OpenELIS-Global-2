import type { Page, Route } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1461: Test Catalog saves report what really happened.
 *
 * - A save answered with a login page (status 200) is a failure: an error is
 *   shown, the typing stays and nothing is stored.
 * - A failed Labels save keeps the typed values for a retry.
 * - A refused enrollment shows the server's reason.
 * - Linking two reagents where one fails keeps the dialog and its error, and
 *   the retry sends only the one that failed.
 * - Deep links to a sample type, lab unit or panel that does not exist say so
 *   instead of spinning or offering an empty form.
 *
 * Every record the spec creates is retired or deleted.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;
const ERROR =
  ".cds--inline-notification--error, .cds--toast-notification--error, .cds--actionable-notification--error";
const SUCCESS =
  ".cds--inline-notification--success, .cds--toast-notification--success, .cds--actionable-notification--success";
const LOGIN_PAGE =
  "<!DOCTYPE html><html><head><title>Login</title></head><body><form action='/login'><input name='loginName'/></form></body></html>";

interface Preset {
  id: number | string;
  printsPerSample?: boolean;
}

interface Reagent {
  id: number | string;
  name: string;
}

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const seedTest = async (page: Page, headers: Record<string, string>) => {
  const stamp = `${Date.now()}`.slice(-7);
  const labUnits = await (await page.request.get(`${TC}/lab-units`)).json();
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name: `PW1461 ${stamp}`,
      reportingName: `PW1461 ${stamp}`,
      code: `PW61${stamp}`,
      domain: "CLINICAL",
      labUnitId: labUnits[0].id,
      sampleTypeIds: ["2"],
      description: `PW1461 ${stamp}`,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  return String((await created.json()).testId);
};

const retire = async (
  page: Page,
  headers: Record<string, string>,
  testId: string,
) => {
  const info = await (
    await page.request.get(`${TC}/tests/${testId}/basic-info`)
  ).json();
  await page.request.put(`${TC}/tests/${testId}/basic-info`, {
    headers,
    data: { ...info, active: false, orderable: false },
  });
};

/** Answers the next `method` request matching `path` with `fault`. */
const faultOnce = async (
  page: Page,
  method: string,
  path: RegExp,
  fault: (route: Route) => Promise<void>,
) => {
  let fired = false;
  await page.route(
    (url) => path.test(url.pathname),
    async (route) => {
      if (fired || route.request().method() !== method) {
        return route.continue();
      }
      fired = true;
      return fault(route);
    },
  );
  return () => fired;
};

test.describe("Test Catalog saves report what really happened (OGC-1461)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("a save answered with a login page is a failure and stores nothing", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      const before = await (
        await page.request.get(`${TC}/tests/${testId}/storage`)
      ).json();
      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/storage`, {
        waitUntil: "domcontentloaded",
      });
      const notes = page.locator("#storage-stability-notes");
      await expect(notes).toBeVisible({ timeout: NAV_TIMEOUT });
      await notes.fill("PW1461 login page");
      const fired = await faultOnce(
        page,
        "PUT",
        /\/tests\/\d+\/storage$/,
        (route) =>
          route.fulfill({
            status: 200,
            contentType: "text/html",
            body: LOGIN_PAGE,
          }),
      );
      await page
        .getByRole("button", { name: /^save$/i })
        .last()
        .click();
      await expect.poll(fired).toBe(true);

      await expect(page.locator(ERROR).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(page.locator(SUCCESS)).toHaveCount(0);
      await expect(notes).toHaveValue("PW1461 login page");
      expect(
        await (await page.request.get(`${TC}/tests/${testId}/storage`)).json(),
      ).toEqual(before);
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("a failed Labels save keeps the typed maximum", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const presets = (await (
      await page.request.get(`${API}/api/labelPresets?status=ACTIVE`)
    ).json()) as Preset[];
    const preset = presets.find((p) => p.printsPerSample !== false);
    test.skip(!preset, "needs an active per-sample label preset");
    const testId = await seedTest(page, headers);
    try {
      expect(
        (
          await page.request.put(
            `${API}/rest/api/tests/${testId}/labelConfig`,
            {
              headers,
              data: {
                allowOrderEntryOverride: true,
                links: [
                  {
                    presetId: preset!.id,
                    defaultQty: 1,
                    maxQty: 3,
                    allowOverride: false,
                  },
                ],
              },
            },
          )
        ).status(),
      ).toBe(200);
      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/labels`, {
        waitUntil: "domcontentloaded",
      });
      const max = page.locator(`#max-${preset!.id}`);
      await expect(max).toBeVisible({ timeout: NAV_TIMEOUT });
      await max.fill("7");
      await max.press("Tab");
      const fired = await faultOnce(
        page,
        "PUT",
        /\/rest\/api\/tests\/\d+\/labelConfig$/,
        (route) =>
          route.fulfill({
            status: 500,
            contentType: "application/json",
            body: '{"status":500}',
          }),
      );
      await page.getByTestId("labels-save").click();
      await expect.poll(fired).toBe(true);

      await expect(
        page.getByTestId("labels-section").locator(ERROR).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(max).toHaveValue("7");
      await expect(page.getByTestId("labels-save")).toBeEnabled();
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("a refused enrollment shows the server's reason", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const stamp = `${Date.now()}`.slice(-6);
    const body = await page.request.post(`${API}/rest/accreditation/bodies`, {
      headers,
      data: {
        code: `PW${stamp}`,
        name: `PW1461 body ${stamp}`,
        expiresOn: "2035-01-01",
      },
    });
    expect(body.status(), await body.text()).toBe(201);
    const bodyId = String((await body.json()).id);
    const testId = await seedTest(page, headers);
    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/accreditation`,
        { waitUntil: "domcontentloaded" },
      );
      await page
        .getByTestId("add-accreditation-button")
        .click({ timeout: NAV_TIMEOUT });
      const dialog = page.getByRole("dialog").last();
      await dialog.locator("#accreditation-body-select").click();
      await page
        .getByRole("option", { name: new RegExp(`PW1461 body ${stamp}`) })
        .click();
      expect(
        (
          await page.request.post(`${API}/rest/accreditation/enrollments`, {
            headers,
            data: { testId, accreditingBodyId: Number(bodyId) },
          })
        ).status(),
      ).toBe(201);
      await dialog.getByRole("button", { name: /^save$/i }).click();

      await expect(
        page.getByTestId("accreditation-section").locator(ERROR).first(),
      ).toContainText(/already accredited/i, { timeout: UI_TIMEOUT });
    } finally {
      const enrollments = await (
        await page.request.get(
          `${API}/rest/accreditation/enrollments?testId=${testId}`,
        )
      ).json();
      for (const e of enrollments) {
        await page.request.delete(
          `${API}/rest/accreditation/enrollments/${e.id}`,
          { headers },
        );
      }
      await page.request.delete(`${API}/rest/accreditation/bodies/${bodyId}`, {
        headers,
      });
      await retire(page, headers, testId);
    }
  });

  test("a partly failed reagent link keeps the dialog, and the retry finishes it", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const reagents = (await (
      await page.request.get(`${API}/rest/inventory/items`)
    ).json()) as Reagent[];
    test.skip(reagents.length < 2, "needs two active reagents");
    const [first, second] = reagents;
    const testId = await seedTest(page, headers);
    const linksOf = async () =>
      (await (
        await page.request.get(`${TC}/${testId}/reagents`)
      ).json()) as unknown[];
    try {
      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/reagents`, {
        waitUntil: "domcontentloaded",
      });
      await page
        .getByTestId("link-reagent-button")
        .click({ timeout: NAV_TIMEOUT });
      const dialog = page.getByRole("dialog").last();
      await dialog.locator("#reagent-multiselect").click();
      for (const r of [first, second]) {
        await page.getByRole("option", { name: r.name }).first().click();
      }
      await dialog.locator(".cds--modal-header").click();

      let posts = 0;
      const linkPath = (url: URL) =>
        new RegExp(`/rest/test-catalog/${testId}/reagents$`).test(url.pathname);
      await page.route(linkPath, async (route) => {
        if (route.request().method() !== "POST") return route.continue();
        posts++;
        return posts === 2
          ? route.fulfill({
              status: 500,
              contentType: "application/json",
              body: '{"status":500}',
            })
          : route.continue();
      });
      await dialog.getByRole("button", { name: /link selected/i }).click();
      await expect(dialog.locator(ERROR).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect.poll(async () => (await linksOf()).length).toBe(1);

      await page.unroute(linkPath);
      const retried = page.waitForResponse(
        (r) => r.request().method() === "POST" && linkPath(new URL(r.url())),
      );
      await dialog.getByRole("button", { name: /link selected/i }).click();
      expect((await retried).status()).toBe(201);
      await expect.poll(async () => (await linksOf()).length).toBe(2);
      await expect(dialog).toBeHidden({ timeout: UI_TIMEOUT });
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("deep links to records that do not exist say so", async ({ page }) => {
    for (const [path, testId] of [
      [
        "/MasterListsPage/SampleTypeEditor/99999999/basic-info",
        "sample-type-not-found",
      ],
      [
        "/MasterListsPage/LabUnitManagement/99999999/basic-info",
        "lab-unit-not-found",
      ],
      [
        "/MasterListsPage/TestCatalogEditor/panel/99999999/basic-info",
        "panel-not-found",
      ],
    ]) {
      await page.goto(path, { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId(testId)).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await expect(page.locator(".cds--loading:visible")).toHaveCount(0);
    }
    await expect(page.getByRole("button", { name: /^save$/i })).toHaveCount(0);
  });
});
