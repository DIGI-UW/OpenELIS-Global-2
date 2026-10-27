import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1462: QC Targets stay savable.
 *
 * - A row added and switched off before it was filled does not block Save and
 *   is not stored.
 * - Changing the result type retires the targets of the old shape (keeping
 *   their values), so targets of the new shape can be saved; switching back
 *   lets the old target be reactivated.
 *
 * Every test the spec creates is retired.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;

interface Target {
  controlLevel: string;
  expectedValue: number | null;
  expectedDictResultName: string | null;
  active: boolean;
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
      name: `PW1462 ${stamp}`,
      reportingName: `PW1462 ${stamp}`,
      code: `PW62${stamp}`,
      domain: "CLINICAL",
      labUnitId: labUnits[0].id,
      sampleTypeIds: ["2"],
      description: `PW1462 ${stamp}`,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  return String((await created.json()).testId);
};

/** Saves the primary component as numeric, or coded with one typed answer. */
const setResultType = async (
  page: Page,
  headers: Record<string, string>,
  testId: string,
  coded: boolean,
) => {
  const sr = await (
    await page.request.get(`${TC}/tests/${testId}/sample-results`)
  ).json();
  const primary = sr.components[0];
  const saved = await page.request.put(`${TC}/tests/${testId}/sample-results`, {
    headers,
    data: {
      testId,
      components: [
        {
          ...primary,
          resultType: coded ? "D" : "N",
          options: coded
            ? [
                {
                  value: "PW1462 reactive",
                  valueName: "PW1462 reactive",
                  freeText: true,
                  resultType: "D",
                  sortOrder: 1,
                  normal: false,
                },
              ]
            : [],
          interpretations: [],
        },
      ],
    },
  });
  expect(saved.status(), await saved.text()).toBe(200);
};

const targetsOf = async (page: Page, testId: string) =>
  (await (await page.request.get(`${TC}/tests/${testId}/qc-targets`)).json())
    .targets as Target[];

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

const saveFromScreen = async (page: Page) => {
  const saved = page.waitForResponse(
    (r) => r.url().endsWith("/qc-targets") && r.request().method() === "PUT",
  );
  await page.getByTestId("qc-targets-save").click();
  return (await saved).status();
};

test.describe("QC Targets stay savable (OGC-1462)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("a row switched off before it was filled does not block Save", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      await setResultType(page, headers, testId, false);
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/qc-targets`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await page
        .getByTestId("qc-target-add-HIGH")
        .click({ timeout: NAV_TIMEOUT });
      await page.getByTestId("qc-target-toggle-HIGH").click();

      expect(await saveFromScreen(page)).toBe(200);
      expect(await targetsOf(page, testId)).toHaveLength(0);
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("a result type change retires the old targets and new ones save", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      await setResultType(page, headers, testId, false);
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/qc-targets`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await page
        .getByTestId("qc-target-add-LOW")
        .click({ timeout: NAV_TIMEOUT });
      await page.getByTestId("qc-target-LOW-expected").fill("5");
      await page.getByTestId("qc-target-LOW-uncertainty").fill("1");
      expect(await saveFromScreen(page)).toBe(200);

      await setResultType(page, headers, testId, true);
      const retired = (await targetsOf(page, testId))[0];
      expect(retired.active).toBe(false);
      expect(Number(retired.expectedValue)).toBe(5);

      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/qc-targets`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await page
        .getByTestId("qc-target-add-NORMAL")
        .click({ timeout: NAV_TIMEOUT });
      await page
        .getByTestId("qc-target-NORMAL-outcome")
        .selectOption({ label: "PW1462 reactive" });
      await page.getByTestId("qc-target-done-NORMAL").click();
      expect(await saveFromScreen(page)).toBe(200);

      const afterCoded = await targetsOf(page, testId);
      const normal = afterCoded.find((t) => t.controlLevel === "NORMAL");
      expect(normal?.active).toBe(true);
      expect(normal?.expectedDictResultName).toBe("PW1462 reactive");
      expect(
        Number(afterCoded.find((t) => t.controlLevel === "LOW")?.expectedValue),
      ).toBe(5);

      await setResultType(page, headers, testId, false);
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/qc-targets`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await page
        .locator('label[for="qc-targets-show-deactivated"]')
        .click({ timeout: NAV_TIMEOUT });
      await page.getByTestId("qc-target-toggle-LOW").click();
      expect(await saveFromScreen(page)).toBe(200);

      const final = await targetsOf(page, testId);
      expect(final.find((t) => t.controlLevel === "LOW")?.active).toBe(true);
      expect(final.find((t) => t.controlLevel === "NORMAL")?.active).toBe(
        false,
      );
    } finally {
      await retire(page, headers, testId);
    }
  });
});
