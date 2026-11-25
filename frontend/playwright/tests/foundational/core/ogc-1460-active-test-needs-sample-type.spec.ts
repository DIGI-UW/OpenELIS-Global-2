import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";

/**
 * OGC-1460: an active test always has a sample type.
 *
 * - Activation names the missing sample type and leaves the test inactive.
 * - The Sample Type side cannot unlink the last sample type of an active test;
 *   it can once the test has another one.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const seedNumericTest = async (
  page: Page,
  headers: Record<string, string>,
  sampleTypeIds: string[],
) => {
  const stamp = `${Date.now()}`.slice(-7);
  const labUnits = await (await page.request.get(`${TC}/lab-units`)).json();
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name: `PW1460 ${stamp}`,
      reportingName: `PW1460 ${stamp}`,
      code: `PW60${stamp}`,
      domain: "CLINICAL",
      labUnitId: labUnits[0].id,
      sampleTypeIds,
      description: `PW1460 ${stamp}`,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  const testId = String((await created.json()).testId);
  const sr = await (
    await page.request.get(`${TC}/tests/${testId}/sample-results`)
  ).json();
  expect(
    (
      await page.request.put(`${TC}/tests/${testId}/sample-results`, {
        headers,
        data: {
          testId,
          components: [
            {
              id: sr.components[0].id,
              code: "PRIMARY",
              label: `PW1460 ${stamp}`,
              resultType: "N",
              isPrimary: true,
              displayOrder: 1,
              showOnReport: true,
              allowMultipleReadings: false,
              options: [],
              interpretations: [],
            },
          ],
        },
      })
    ).status(),
  ).toBe(200);
  return testId;
};

const basicInfo = async (page: Page, testId: string) =>
  (await page.request.get(`${TC}/tests/${testId}/basic-info`)).json();

const retire = async (
  page: Page,
  headers: Record<string, string>,
  testId: string,
) => {
  const info = await basicInfo(page, testId);
  await page.request.put(`${TC}/tests/${testId}/basic-info`, {
    headers,
    data: { ...info, active: false, orderable: false },
  });
};

test.describe("An active test always has a sample type (OGC-1460)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("activation names the missing sample type", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedNumericTest(page, headers, ["2"]);
    const info = await basicInfo(page, testId);
    expect(
      (
        await page.request.put(`${TC}/tests/${testId}/basic-info`, {
          headers,
          data: {
            ...info,
            sampleTypeId: null,
            sampleTypeIds: [],
            orderable: false,
          },
        })
      ).status(),
    ).toBe(200);

    const refused = await page.request.post(`${TC}/tests/${testId}/activate`, {
      headers,
      data: { gapsAcknowledged: "{}" },
    });
    expect(refused.status()).toBe(422);
    expect((await refused.json()).missing).toContain("NO_SAMPLE_TYPE");
    expect((await basicInfo(page, testId)).active).toBe(false);
  });

  test("the last sample type of an active test cannot be unlinked", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedNumericTest(page, headers, ["2"]);
    expect(
      (
        await page.request.post(`${TC}/tests/${testId}/activate`, {
          headers,
          data: { gapsAcknowledged: "{}" },
        })
      ).status(),
    ).toBe(200);
    try {
      const refused = await page.request.delete(
        `${API}/rest/sample-types/2/tests/${testId}`,
        { headers },
      );
      expect(refused.status()).toBe(422);
      expect((await basicInfo(page, testId)).sampleTypeIds).toEqual(["2"]);

      expect(
        (
          await page.request.put(`${API}/rest/sample-types/3/tests/${testId}`, {
            headers,
            data: {},
          })
        ).status(),
      ).toBe(200);
      expect(
        (
          await page.request.delete(
            `${API}/rest/sample-types/2/tests/${testId}`,
            { headers },
          )
        ).status(),
      ).toBe(200);
      expect((await basicInfo(page, testId)).sampleTypeIds).toEqual(["3"]);
    } finally {
      await retire(page, headers, testId);
    }
  });
});
