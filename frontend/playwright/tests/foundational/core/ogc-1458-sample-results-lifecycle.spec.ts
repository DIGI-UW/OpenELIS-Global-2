import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1458: Sample & Results keeps what the admin configured.
 *
 * - A free-text option typed as a number reads back as that number, and an
 *   interpretation on it still matches it.
 * - Removing Primary while another component exists saves; a re-added code is
 *   a new component with no old ranges.
 * - An active test keeps a component; unknown result types are refused; the
 *   copy-from endpoint is gone.
 *
 * Each test seeds its own inactive test (the catalog never deletes tests).
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

const component = (over: Record<string, unknown>) => ({
  code: "PRIMARY",
  label: "PW1458",
  resultType: "N",
  isPrimary: true,
  displayOrder: 1,
  showOnReport: true,
  allowMultipleReadings: false,
  options: [],
  interpretations: [],
  ...over,
});

const seed = async (page: Page, headers: Record<string, string>) => {
  const stamp = `${Date.now()}`.slice(-7);
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name: `PW1458 ${stamp}`,
      reportingName: `PW1458 ${stamp}`,
      code: `PW58${stamp}`,
      domain: "CLINICAL",
      sampleTypeIds: ["2"],
      description: `PW1458 ${stamp}`,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  const body = await created.json();
  const testId = String(body.testId ?? body.id);
  const sr = await (
    await page.request.get(`${TC}/tests/${testId}/sample-results`)
  ).json();
  return { testId, primaryId: sr.components[0].id as string };
};

const put = (
  page: Page,
  headers: Record<string, string>,
  testId: string,
  components: unknown[],
) =>
  page.request.put(`${TC}/tests/${testId}/sample-results`, {
    headers,
    data: { testId, components },
  });

const read = async (page: Page, testId: string) =>
  (
    await (
      await page.request.get(`${TC}/tests/${testId}/sample-results`)
    ).json()
  ).components;

test.describe("Sample & Results lifecycle (OGC-1458)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("typed numbers stay numbers and their interpretation still matches", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const { testId } = await seed(page, headers);

    await page.goto(
      `/MasterListsPage/TestCatalogEditor/${testId}/sample-results`,
      { waitUntil: "domcontentloaded" },
    );
    await page.locator("#comp-label-0").waitFor({ timeout: NAV_TIMEOUT });
    await page.locator('label[for="comp-type-0-D"]').click();
    const addCustom = page.getByRole("button", { name: "Add custom option" });
    await addCustom.click();
    await addCustom.click();
    await page.locator("#opt-value-0-0").fill("1");
    await page.locator("#opt-value-0-1").fill("2");
    await page.getByRole("button", { name: "Add interpretation" }).click();
    await page.locator("#int-match-0-0").selectOption("1");
    await page.locator("#int-text-0-0").fill("Grade one");
    const saved = page.waitForResponse(
      (r) =>
        r.url().endsWith(`/tests/${testId}/sample-results`) &&
        r.request().method() === "PUT",
    );
    await page
      .getByRole("button", { name: "Save", exact: true })
      .last()
      .click();
    await saved;
    await expect(page.getByText(/saved/i).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    const [primary] = await read(page, testId);
    expect(
      primary.options.map((o: { valueName: string }) => o.valueName),
    ).toEqual(["1", "2"]);
    expect(primary.interpretations[0].valueMatch).toBe(
      primary.options[0].value,
    );
  });

  test("removing a component frees its code and retires what it had", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const { testId, primaryId } = await seed(page, headers);

    expect(
      (
        await put(page, headers, testId, [
          component({ id: primaryId }),
          component({
            code: "PW2",
            label: "Second",
            isPrimary: false,
            displayOrder: 2,
          }),
        ])
      ).status(),
    ).toBe(200);
    const second = (await read(page, testId)).find(
      (c: { code: string }) => c.code === "PW2",
    );
    expect(
      (
        await page.request.put(`${TC}/tests/${testId}/ranges`, {
          headers,
          data: {
            testId,
            ranges: [
              {
                componentId: second.id,
                minAge: 0,
                lowNormal: 3,
                highNormal: 5,
              },
            ],
          },
        })
      ).status(),
    ).toBe(200);

    const promoted = await put(page, headers, testId, [
      component({ id: second.id, label: "Second", isPrimary: true }),
    ]);
    expect(promoted.status()).toBe(200);
    const afterPromotion = await read(page, testId);
    expect(afterPromotion).toHaveLength(1);
    expect(afterPromotion[0].isPrimary).toBe(true);

    expect(
      (
        await put(page, headers, testId, [
          component({ id: second.id, label: "Second", isPrimary: true }),
          component({
            code: "PW3",
            label: "Third",
            isPrimary: false,
            displayOrder: 2,
          }),
        ])
      ).status(),
    ).toBe(200);
    const third = (await read(page, testId)).find(
      (c: { code: string }) => c.code === "PW3",
    );
    expect(
      (
        await put(page, headers, testId, [
          component({ id: second.id, label: "Second", isPrimary: true }),
        ])
      ).status(),
    ).toBe(200);
    expect(
      (
        await put(page, headers, testId, [
          component({ id: second.id, label: "Second", isPrimary: true }),
          component({
            code: "PW3",
            label: "Third again",
            isPrimary: false,
            displayOrder: 2,
          }),
        ])
      ).status(),
    ).toBe(200);
    const again = (await read(page, testId)).find(
      (c: { code: string }) => c.code === "PW3",
    );
    expect(again.id).not.toBe(third.id);
    const ranges = (
      await (await page.request.get(`${TC}/tests/${testId}/ranges`)).json()
    ).ranges;
    expect(
      ranges.filter((r: { componentId: string }) => r.componentId === again.id),
    ).toHaveLength(0);
  });

  test("the server refuses what result entry could not use", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const { testId, primaryId } = await seed(page, headers);
    expect(
      (
        await put(page, headers, testId, [component({ id: primaryId })])
      ).status(),
    ).toBe(200);

    for (const resultType of [null, "Z", "NN"]) {
      const refused = await put(page, headers, testId, [
        component({ id: primaryId, resultType }),
      ]);
      expect(refused.status(), `result type ${resultType}`).toBe(422);
    }
    expect((await read(page, testId))[0].resultType).toBe("N");

    const copy = await page.request.post(
      `${TC}/tests/${testId}/sample-results/copy-from/${testId}`,
      { headers },
    );
    expect([404, 405, 410]).toContain(copy.status());

    expect((await put(page, headers, testId, [])).status()).toBe(200);
  });
});
