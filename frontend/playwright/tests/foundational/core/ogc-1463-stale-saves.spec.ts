import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1463: a page loaded before another admin's change cannot undo it.
 *
 * In each test the page is admin A; admin B's change goes through the API
 * after A's page has loaded. A's save is refused as stale with a Refresh
 * button and B's change stands; after Refresh, A's save goes through, and a
 * second save from the same page needs no refresh. Localization sends only
 * the locales A changed, so A's save goes through and B's French stands.
 *
 * Every record the spec creates is retired.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;

interface TestRow {
  testId: string;
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

const stamp = () => `${Date.now()}`.slice(-7);

const seedTest = async (page: Page, headers: Record<string, string>) => {
  const s = stamp();
  const labUnits = await (await page.request.get(`${TC}/lab-units`)).json();
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name: `PW1463 ${s}`,
      reportingName: `PW1463 ${s}`,
      code: `PW63${s}`,
      domain: "CLINICAL",
      labUnitId: labUnits[0].id,
      sampleTypeIds: ["2"],
      description: `PW1463 ${s}`,
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

const putSampleResults = async (
  page: Page,
  headers: Record<string, string>,
  testId: string,
  label: string,
) => {
  const sr = await (
    await page.request.get(`${TC}/tests/${testId}/sample-results`)
  ).json();
  const response = await page.request.put(
    `${TC}/tests/${testId}/sample-results`,
    {
      headers,
      data: {
        testId,
        components: [
          {
            ...sr.components[0],
            label,
            resultType: "N",
            options: [],
            interpretations: [],
          },
        ],
      },
    },
  );
  expect(response.status(), await response.text()).toBe(200);
};

const saveButton = (page: Page) =>
  page.getByRole("button", { name: /^save$/i }).last();

/** Clicks and waits for the PUT it causes, so the next step sees its result. */
const saveVia = async (
  page: Page,
  urlEnd: string,
  click: () => Promise<void>,
) => {
  const saved = page.waitForResponse(
    (r) => r.url().endsWith(urlEnd) && r.request().method() === "PUT",
  );
  await click();
  return (await saved).status();
};

const refresh = (page: Page) =>
  page.getByRole("button", { name: "Refresh" }).click();

test.describe("A page loaded before another admin's change cannot undo it (OGC-1463)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("Terminology", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    const codes = async () =>
      (
        await (
          await page.request.get(`${TC}/tests/${testId}/terminology`)
        ).json()
      ).mappings.map((m: { code: string }) => m.code);
    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/terminology`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await expect(page.locator("#terminology-code")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      expect(
        (
          await page.request.put(`${TC}/tests/${testId}/terminology`, {
            headers,
            data: {
              testId,
              mappings: [
                { source: "LOINC", code: "3333-3", relationship: "SAME_AS" },
              ],
            },
          })
        ).status(),
      ).toBe(200);

      await page.locator("#terminology-source").selectOption("SNOMED");
      await page.locator("#terminology-code").fill("1463001");
      await saveButton(page).click();

      await expect(page.getByTestId("terminology-stale-save")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect(await codes()).toEqual(["3333-3"]);

      await refresh(page);
      await expect(page.getByTestId("terminology-stale-save")).toBeHidden();
      await expect(page.getByText("3333-3")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page.locator("#terminology-source").selectOption("SNOMED");
      await page.locator("#terminology-code").fill("1463001");
      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith("/terminology") && r.request().method() === "PUT",
      );
      await saveButton(page).click();
      expect((await saved).status()).toBe(200);
      await expect.poll(codes).toContain("1463001");

      await page.locator("#terminology-source").selectOption("SNOMED");
      await page.locator("#terminology-code").fill("1463002");
      const again = page.waitForResponse(
        (r) =>
          r.url().endsWith("/terminology") && r.request().method() === "PUT",
      );
      await saveButton(page).click();
      expect((await again).status(), "a second save needs no refresh").toBe(
        200,
      );
      expect(await codes()).toEqual(
        expect.arrayContaining(["3333-3", "1463001", "1463002"]),
      );
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("Sample & Results", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    const label = async () =>
      (
        await (
          await page.request.get(`${TC}/tests/${testId}/sample-results`)
        ).json()
      ).components[0].label;
    try {
      await putSampleResults(page, headers, testId, "PW1463 result");
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/sample-results`,
        { waitUntil: "domcontentloaded" },
      );
      const input = page.locator("#comp-label-0");
      await expect(input).toHaveValue("PW1463 result", {
        timeout: NAV_TIMEOUT,
      });
      await putSampleResults(page, headers, testId, "Renamed by B");

      await input.fill("Renamed by A");
      await saveButton(page).click();

      await expect(page.getByTestId("sample-results-stale-save")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect(await label()).toBe("Renamed by B");

      await refresh(page);
      await expect(input).toHaveValue("Renamed by B", { timeout: UI_TIMEOUT });
      await input.fill("Renamed by A");
      const reloaded = page.waitForResponse(
        (r) =>
          r.url().endsWith("/sample-results") && r.request().method() === "GET",
      );
      expect(
        await saveVia(page, "/sample-results", () => saveButton(page).click()),
      ).toBe(200);
      await reloaded;
      await expect(input).toHaveValue("Renamed by A");
      expect(await label()).toBe("Renamed by A");
      await input.fill("Renamed by A again");
      expect(
        await saveVia(page, "/sample-results", () => saveButton(page).click()),
        "a second save needs no refresh",
      ).toBe(200);
      expect(await label()).toBe("Renamed by A again");
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("Alerts", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    const rule = {
      name: `PW1463 rule ${stamp()}`,
      enabled: true,
      triggerType: "CRITICAL",
      triggerValue: null,
      componentId: null,
      sampleTypeId: null,
      notifySms: false,
      notifyEmail: true,
      notifyOrderingPhysician: false,
      notifyPatient: false,
      notifyReferringFacility: false,
      notifyCustomPhone: null,
      notifyCustomEmail: "qa@example.org",
      notifyRoleId: null,
      acknowledgmentRequired: false,
    };
    const created = await page.request.post(`${TC}/${testId}/alerts`, {
      headers,
      data: rule,
    });
    expect(created.status()).toBe(201);
    const ruleId = (await created.json()).id;
    const stored = async () =>
      (
        (await (await page.request.get(`${TC}/${testId}/alerts`)).json()) as {
          id: string;
          enabled: boolean;
        }[]
      ).find((r) => r.id === ruleId);
    try {
      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/alerts`, {
        waitUntil: "domcontentloaded",
      });
      const toggle = page.locator(`#enabled-${ruleId}`);
      const toggleLabel = page.locator(`label[for="enabled-${ruleId}"]`);
      await expect(toggle).toBeVisible({ timeout: NAV_TIMEOUT });
      expect(
        (
          await page.request.put(`${TC}/${testId}/alerts/${ruleId}`, {
            headers,
            data: { ...rule, enabled: false },
          })
        ).status(),
      ).toBe(200);

      await toggleLabel.click();

      await expect(page.getByTestId("alerts-stale-save")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect((await stored())?.enabled).toBe(false);
      await refresh(page);
      await expect(toggle).toHaveAttribute("aria-checked", "false", {
        timeout: UI_TIMEOUT,
      });
      expect(
        await saveVia(page, `/alerts/${ruleId}`, () => toggleLabel.click()),
      ).toBe(200);
      await expect(toggle).toHaveAttribute("aria-checked", "true");
      expect((await stored())?.enabled).toBe(true);
      expect(
        await saveVia(page, `/alerts/${ruleId}`, () => toggleLabel.click()),
        "a second toggle needs no refresh",
      ).toBe(200);
      expect((await stored())?.enabled).toBe(false);
    } finally {
      await page.request.delete(`${TC}/${testId}/alerts/${ruleId}`, {
        headers,
      });
      await retire(page, headers, testId);
    }
  });

  test("Panel members", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const rows = (
      await (
        await page.request.get(
          `${TC}/tests?domain=CLINICAL&status=active&page=1&pageSize=5`,
        )
      ).json()
    ).rows as TestRow[];
    test.skip(rows.length < 3, "needs three active clinical tests");
    const [a, b, c] = rows.map((r) => String(r.testId));
    const created = await page.request.post(`${TC}/panels`, {
      headers,
      data: { name: `PW1463 ${stamp()}`, active: false },
    });
    expect(created.status()).toBe(201);
    const panelId = (await created.json()).id;
    const members = async () =>
      (
        await (
          await page.request.get(`${TC}/panels/${panelId}/test-order`)
        ).json()
      ).tests.map((t: { testId: string }) => String(t.testId));
    const putMembers = (ids: string[]) =>
      page.request.put(`${TC}/panels/${panelId}/tests`, {
        headers,
        data: { tests: ids.map((testId, i) => ({ testId, position: i + 1 })) },
      });
    expect((await putMembers([a, b])).status()).toBe(200);
    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/panel/${panelId}/tests`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await expect(page.getByTestId(`panel-test-remove-${a}`)).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      expect((await putMembers([a, b, c])).status()).toBe(200);

      await page.getByTestId(`panel-test-remove-${a}`).click();
      await saveButton(page).click();

      await expect(page.getByTestId("panel-tests-stale-save")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect(await members()).toEqual([a, b, c]);

      await refresh(page);
      await expect(page.getByTestId(`panel-test-remove-${c}`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page.getByTestId(`panel-test-remove-${a}`).click();
      expect(
        await saveVia(page, `/panels/${panelId}/tests`, () =>
          saveButton(page).click(),
        ),
      ).toBe(200);
      expect(await members()).toEqual([b, c]);
      await expect(page.getByTestId(`panel-test-remove-${a}`)).toBeHidden();
      await page.getByTestId(`panel-test-remove-${b}`).click();
      expect(
        await saveVia(page, `/panels/${panelId}/tests`, () =>
          saveButton(page).click(),
        ),
        "a second save needs no refresh",
      ).toBe(200);
      expect(await members()).toEqual([c]);
    } finally {
      await putMembers([]);
    }
  });

  test("Localization keeps the French another admin saved", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      const refs = await (
        await page.request.get(`${TC}/tests/${testId}/localization`)
      ).json();
      const nameId = refs.fields.find(
        (f: { field: string }) => f.field === "name",
      ).localizationId;
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/localization`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      const input = page.locator("#localization-input-name");
      await expect(input).toBeVisible({ timeout: NAV_TIMEOUT });
      await page.locator("#localization-locale").selectOption("en");
      expect(
        (
          await page.request.put(
            `${API}/rest/localizations/${nameId}/translations`,
            {
              headers,
              data: { fr: "PW1463 nom par B" },
            },
          )
        ).status(),
      ).toBe(200);

      await input.fill("PW1463 renamed by A");
      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith("/translations") && r.request().method() === "PUT",
      );
      await saveButton(page).click();
      expect((await saved).status()).toBe(200);

      const stored = await (
        await page.request.get(`${API}/rest/localizations/${nameId}`)
      ).json();
      expect(stored.translations.en).toBe("PW1463 renamed by A");
      expect(stored.translations.fr).toBe("PW1463 nom par B");
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("Sample types", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const name = `PW1463 type ${stamp()}`;
    expect(
      (
        await page.request.post(`${API}/rest/SampleTypeCreate`, {
          headers,
          data: {
            sampleTypeEnglishName: name,
            sampleTypeFrenchName: name,
            description: name,
            domain: "CLINICAL",
            active: true,
          },
        })
      ).status(),
    ).toBe(200);
    const listed = (
      await (await page.request.get(`${API}/rest/sample-types`)).json()
    ).data.find((t: { description: string }) => t.description === name);
    const id = String(listed.id);
    const stored = async () =>
      (await (await page.request.get(`${API}/rest/sample-types/${id}`)).json())
        .data;
    const putType = async (data: Record<string, unknown>) =>
      page.request.put(`${API}/rest/sample-types/${id}`, {
        headers,
        data: { id, name, description: name, domain: "CLINICAL", ...data },
      });
    try {
      await page.goto(`/MasterListsPage/SampleTypeEditor/${id}/basic-info`, {
        waitUntil: "domcontentloaded",
      });
      const description = page.locator("#st-description");
      await expect(description).toHaveValue(name, { timeout: NAV_TIMEOUT });
      expect((await putType({ isActive: false })).status()).toBe(200);

      await description.fill(`${name} A`);
      await saveButton(page).click();

      await expect(page.getByTestId("sample-type-stale-save")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect((await stored()).isActive).toBe(false);
      expect((await stored()).description).toBe(name);
    } finally {
      await putType({ isActive: false });
    }
  });

  test("Lab units", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const created = await page.request.post(
      `${API}/rest/lab-units-management`,
      {
        headers,
        data: {
          names: { en: `PW1463 ${stamp()}` },
          domain: "CLINICAL",
          isActive: true,
        },
      },
    );
    expect(created.status(), await created.text()).toBe(201);
    const unit = (await created.json()).data;
    const id = String(unit.id);
    expect(
      (
        await page.request.put(`${API}/rest/lab-units-management/${id}`, {
          headers,
          data: { ...unit, isActive: true },
        })
      ).status(),
    ).toBe(200);
    const stored = async () =>
      (
        await (
          await page.request.get(`${API}/rest/lab-units-management/${id}`)
        ).json()
      ).data;
    try {
      await page.goto(`/MasterListsPage/LabUnitManagement/${id}/basic-info`, {
        waitUntil: "domcontentloaded",
      });
      const description = page.locator("#lu-description");
      await expect(description).toBeVisible({ timeout: NAV_TIMEOUT });
      expect(
        (
          await page.request.post(
            `${API}/rest/lab-units-management/${id}/deactivate`,
            {
              headers,
              data: { confirmation: "DEACTIVATE", option: "keep" },
            },
          )
        ).status(),
      ).toBe(200);

      await description.fill("Changed on an old page");
      await saveButton(page).click();

      await expect(page.getByTestId("lab-unit-stale-save")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      expect((await stored()).isActive).toBe(false);
    } finally {
      const now = await stored();
      if (now.isActive) {
        await page.request.post(
          `${API}/rest/lab-units-management/${id}/deactivate`,
          {
            headers,
            data: { confirmation: "DEACTIVATE", option: "keep" },
          },
        );
      }
    }
  });
});
