import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1456: the Panel editor never loses members it could not load, and the
 * server keeps a panel's membership honest.
 *
 * - A failed member-list load says so and keeps Save disabled; Retry loads.
 * - An active panel keeps at least one test, from either side of the model.
 * - An inactive test cannot be added; a name over 20 characters cannot be
 *   created; a malformed member id is refused without a 500.
 * - A code that does not fit (LOINC over 10, any code over 80) is refused and
 *   the panel keeps its routing LOINC.
 *
 * Each test creates its own panel and removes it.
 */

const API = "/api/OpenELIS-Global";

interface PanelRow {
  testId: string;
}

interface TestRow {
  testId: string;
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

const activeClinicalTests = async (page: Page): Promise<TestRow[]> => {
  const response = await page.request.get(
    `${API}/rest/test-catalog/tests?domain=CLINICAL&status=active&page=1&pageSize=5`,
  );
  expect(response.status()).toBe(200);
  return ((await response.json()) as { rows: TestRow[] }).rows;
};

const createPanel = async (page: Page, headers: Record<string, string>) => {
  const name = `PW1456 ${Date.now() % 1000000}`;
  const response = await page.request.post(`${API}/rest/test-catalog/panels`, {
    headers,
    data: { name, active: false },
  });
  expect(response.status()).toBe(201);
  return ((await response.json()) as { id: string }).id;
};

const members = async (page: Page, panelId: string) => {
  const response = await page.request.get(
    `${API}/rest/test-catalog/panels/${panelId}/test-order`,
  );
  return ((await response.json()) as { tests: PanelRow[] }).tests.map(
    (t) => t.testId,
  );
};

const retirePanel = async (
  page: Page,
  headers: Record<string, string>,
  panelId: string,
) => {
  await page.request.put(
    `${API}/rest/test-catalog/panels/${panelId}/basic-info`,
    {
      headers,
      data: { active: false },
    },
  );
  await page.request.put(`${API}/rest/test-catalog/panels/${panelId}/tests`, {
    headers,
    data: { tests: [] },
  });
  await page.request.put(
    `${API}/rest/test-catalog/panels/${panelId}/terminology`,
    { headers, data: { mappings: [] } },
  );
};

test.describe("Panel membership guards (OGC-1456)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("a failed member load blocks Save until Retry loads the members", async ({
    page,
  }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const [first, second] = await activeClinicalTests(page);
    const panelId = await createPanel(page, headers);
    try {
      expect(
        (
          await page.request.put(
            `${API}/rest/test-catalog/panels/${panelId}/tests`,
            {
              headers,
              data: {
                tests: [
                  { testId: first.testId, position: 1 },
                  { testId: second.testId, position: 2 },
                ],
              },
            },
          )
        ).status(),
      ).toBe(200);

      const failing = new RegExp(`/panels/${panelId}/test-order$`);
      await page.route(failing, (route) =>
        route.fulfill({ status: 500, body: "{}" }),
      );
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/panel/${panelId}/tests`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("panel-tests-load-error")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      const save = page
        .getByTestId("panel-tests-table")
        .locator("xpath=following::button[normalize-space()='Save'][1]");
      await expect(save).toBeDisabled();

      await page.unroute(failing);
      await page.getByRole("button", { name: "Retry", exact: true }).click();
      await expect(page.getByTestId("panel-tests-load-error")).toBeHidden({
        timeout: UI_TIMEOUT,
      });
      await expect(page.getByTestId("panel-tests-count")).toContainText("2");
      await expect(save).toBeEnabled();
      expect(await members(page, panelId)).toEqual([
        first.testId,
        second.testId,
      ]);
    } finally {
      await retirePanel(page, headers, panelId);
    }
  });

  test("the server keeps membership honest", async ({ page }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const [first] = await activeClinicalTests(page);
    const panelId = await createPanel(page, headers);
    const putTests = (tests: unknown) =>
      page.request.put(`${API}/rest/test-catalog/panels/${panelId}/tests`, {
        headers,
        data: { tests },
      });
    try {
      expect(
        (
          await page.request.put(
            `${API}/rest/test-catalog/panels/${panelId}/tests`,
            {
              headers,
              data: {
                tests: [{ testId: first.testId, position: 1 }],
                autoActivate: true,
              },
            },
          )
        ).status(),
      ).toBe(200);

      const emptied = await putTests([]);
      expect(emptied.status()).toBe(422);
      expect((await emptied.json()).refusal).toBe("tests.activeNeedsTest");
      const fromTestSide = await page.request.put(
        `${API}/rest/test-catalog/tests/${first.testId}/panels`,
        {
          headers,
          data: {
            memberships: (
              (await (
                await page.request.get(
                  `${API}/rest/test-catalog/tests/${first.testId}/panels`,
                )
              ).json()) as {
                memberships: { panelId: string; position: number | null }[];
              }
            ).memberships
              .filter((m) => m.panelId !== panelId)
              .map((m) => ({ panelId: m.panelId, position: m.position })),
          },
        },
      );
      expect(fromTestSide.status()).toBe(422);
      expect((await fromTestSide.json()).refusal).toBe("panel.activeNeedsTest");
      expect(await members(page, panelId)).toEqual([first.testId]);

      expect((await putTests([{ testId: "abc", position: 1 }])).status()).toBe(
        422,
      );

      const longName = await page.request.post(
        `${API}/rest/test-catalog/panels`,
        { headers, data: { name: "PW1456 twenty-one chr", active: false } },
      );
      expect(longName.status()).toBe(422);
      expect((await longName.json()).refusal).toBe("name.tooLong");

      const terminology = `${API}/rest/test-catalog/panels/${panelId}/terminology`;
      expect(
        (
          await page.request.put(terminology, {
            headers,
            data: {
              mappings: [
                { source: "LOINC", code: "24331-1", relationship: "SAME_AS" },
              ],
            },
          })
        ).status(),
      ).toBe(200);
      const longLoinc = await page.request.put(terminology, {
        headers,
        data: {
          mappings: [
            { source: "LOINC", code: "12345678-90", relationship: "SAME_AS" },
          ],
        },
      });
      expect(longLoinc.status()).toBe(422);
      expect((await longLoinc.json()).refusal).toBe("loinc.tooLong");
      const panel = await (
        await page.request.get(`${API}/rest/test-catalog/panels/${panelId}`)
      ).json();
      expect(panel.loinc).toBe("24331-1");
    } finally {
      await retirePanel(page, headers, panelId);
    }
  });

  test("Tests and Terminology say why a save was refused", async ({ page }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const [first] = await activeClinicalTests(page);
    const panelId = await createPanel(page, headers);
    try {
      await page.request.put(
        `${API}/rest/test-catalog/panels/${panelId}/tests`,
        {
          headers,
          data: {
            tests: [{ testId: first.testId, position: 1 }],
            autoActivate: true,
          },
        },
      );

      await page.goto(
        `/MasterListsPage/TestCatalogEditor/panel/${panelId}/tests`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("panel-tests-count")).toContainText("1", {
        timeout: NAV_TIMEOUT,
      });
      await page.getByTestId(`panel-test-remove-${first.testId}`).click();
      await page
        .getByTestId("panel-tests-table")
        .locator("xpath=following::button[normalize-space()='Save'][1]")
        .click();
      await expect(
        page.getByText(/an active panel needs at least one test/i),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      expect(await members(page, panelId)).toEqual([first.testId]);

      await page.goto(
        `/MasterListsPage/TestCatalogEditor/panel/${panelId}/terminology`,
        { waitUntil: "domcontentloaded" },
      );
      await page
        .locator("#panel-terminology-source")
        .selectOption("LOINC", { timeout: NAV_TIMEOUT });
      await page.locator("#panel-terminology-code").fill("12345678-90");
      await page
        .getByTestId("panel-terminology-section")
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await expect(
        page.getByTestId("panel-terminology-save-error"),
      ).toContainText("12345678-90");
    } finally {
      await retirePanel(page, headers, panelId);
    }
  });
});
