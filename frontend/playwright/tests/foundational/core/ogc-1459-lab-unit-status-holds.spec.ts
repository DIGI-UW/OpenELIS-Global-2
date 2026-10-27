import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1459: a deactivated lab unit stays deactivated.
 *
 * - A Basic Info save of one of its tests keeps it off, and the editor says
 *   the unit is inactive.
 * - Deactivating another unit cannot hand its tests to the inactive one.
 */

const API = "/api/OpenELIS-Global";

interface LabUnitRow {
  id: string;
  name: string;
  isActive: boolean;
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

const createLabUnit = async (
  page: Page,
  headers: Record<string, string>,
  name: string,
) => {
  const created = await page.request.post(`${API}/rest/lab-units-management`, {
    headers,
    data: { names: { en: name }, domain: "CLINICAL", isActive: true },
  });
  expect(created.status(), await created.text()).toBe(201);
  const unit = (await created.json()).data;
  const activated = await page.request.put(
    `${API}/rest/lab-units-management/${unit.id}`,
    { headers, data: { ...unit, isActive: true } },
  );
  expect(activated.status(), await activated.text()).toBe(200);
  return String(unit.id);
};

const labUnit = async (page: Page, id: string) => {
  const list = await (
    await page.request.get(`${API}/rest/lab-units-management`)
  ).json();
  return (list.data as LabUnitRow[]).find((u) => String(u.id) === id)!;
};

const deactivate = (
  page: Page,
  headers: Record<string, string>,
  id: string,
  extra: Record<string, unknown> = {},
) =>
  page.request.post(`${API}/rest/lab-units-management/${id}/deactivate`, {
    headers,
    data: { confirmation: "DEACTIVATE", option: "keep", ...extra },
  });

test.describe("A deactivated lab unit stays deactivated (OGC-1459)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("a Basic Info save keeps the unit off and the editor says so", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const stamp = `${Date.now()}`.slice(-6);
    const unitId = await createLabUnit(page, headers, `PW1459 A ${stamp}`);
    const created = await page.request.post(`${API}/rest/test-catalog/tests`, {
      headers,
      data: {
        name: `PW1459 ${stamp}`,
        reportingName: `PW1459 ${stamp}`,
        code: `PW59${stamp}`,
        domain: "CLINICAL",
        labUnitId: unitId,
        sampleTypeIds: ["2"],
        description: `PW1459 ${stamp}`,
      },
    });
    expect(created.status()).toBe(201);
    const testId = String((await created.json()).testId);
    expect((await deactivate(page, headers, unitId)).status()).toBe(200);
    expect((await labUnit(page, unitId)).isActive).toBe(false);

    await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/basic-info`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByText(
        /This lab unit is inactive, so the test cannot be ordered/,
      ),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    const saved = page.waitForResponse(
      (r) =>
        r.url().endsWith(`/tests/${testId}/basic-info`) &&
        r.request().method() === "PUT",
    );
    await page.getByRole("button", { name: "Save", exact: true }).click();
    expect((await saved).status()).toBe(200);

    expect((await labUnit(page, unitId)).isActive).toBe(false);
  });

  test("a deactivation cannot hand its tests to an inactive unit", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const stamp = `${Date.now()}`.slice(-6);
    const inactive = await createLabUnit(page, headers, `PW1459 B ${stamp}`);
    const source = await createLabUnit(page, headers, `PW1459 C ${stamp}`);
    expect((await deactivate(page, headers, inactive)).status()).toBe(200);

    const refused = await deactivate(page, headers, source, {
      option: "reassign",
      destinationLabUnitId: inactive,
    });
    expect(refused.status()).toBe(422);
    expect((await labUnit(page, inactive)).isActive).toBe(false);
    expect((await labUnit(page, source)).isActive).toBe(true);

    expect((await deactivate(page, headers, source)).status()).toBe(200);
  });
});
