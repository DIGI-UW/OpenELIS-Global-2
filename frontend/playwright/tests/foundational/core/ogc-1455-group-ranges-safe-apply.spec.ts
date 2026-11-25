import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1455: "Edit related tests" > Ranges never applies a set it could not load.
 *
 * - A failed range load says which test failed, disables "Set all to these
 *   values" and sends nothing; Retry loads the tests and enables it again.
 * - Tests that differ only in their reporting range are flagged as different.
 * - A bad group request (non-numeric or unknown id, missing ranges) is refused
 *   before any test is written; the same holds for the group Storage save.
 *
 * Every change is restored.
 */

const API = "/api/OpenELIS-Global";

interface TestOption {
  id: string;
  value: string;
}

interface RangesResponse {
  ranges: Record<string, unknown>[];
}

interface Component {
  id: string;
  isPrimary?: boolean;
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

const getJson = async <T>(page: Page, url: string): Promise<T> => {
  const response = await page.request.get(`${API}${url}`);
  expect(response.status()).toBe(200);
  return (await response.json()) as T;
};

const rangesOf = (page: Page, id: string) =>
  getJson<RangesResponse>(page, `/rest/test-catalog/tests/${id}/ranges`);

const withoutIds = (ranges: Record<string, unknown>[]) =>
  ranges.map(({ id: _id, ...r }) => r);

/** Two tests that both hold at least one numeric range. */
const rangedPair = async (page: Page) => {
  const tests = await getJson<TestOption[]>(page, "/rest/test-list");
  const found: { id: string; ranges: Record<string, unknown>[] }[] = [];
  for (const t of tests) {
    const { ranges } = await rangesOf(page, t.id);
    if (ranges.length > 0) found.push({ id: t.id, ranges });
    if (found.length === 2) return found;
  }
  return null;
};

const primaryComponentOf = async (page: Page, id: string) => {
  const { components } = await getJson<{ components: Component[] }>(
    page,
    `/rest/test-catalog/tests/${id}/sample-results`,
  );
  const primary = components.find((c) => c.isPrimary) || components[0];
  return primary ? primary.id : null;
};

const groupUrl = (a: string, b: string) =>
  `/MasterListsPage/TestCatalogEditor/group/${a},${b}/ranges`;

test.describe("Group Ranges never applies a set it could not load (OGC-1455)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("a failed range load blocks Set all until Retry loads it", async ({
    page,
  }) => {
    const pair = await rangedPair(page);
    test.skip(!pair, "needs two tests with numeric ranges");
    const [a, b] = pair!;

    const failing = new RegExp(`/rest/test-catalog/tests/${a.id}/ranges$`);
    await page.route(failing, (route) =>
      route.request().method() === "GET"
        ? route.fulfill({ status: 500, body: "{}" })
        : route.continue(),
    );
    const puts: string[] = [];
    page.on("request", (r) => {
      if (r.method() === "PUT" && r.url().includes("/group/ranges")) {
        puts.push(r.url());
      }
    });

    await page.goto(groupUrl(a.id, b.id), { waitUntil: "domcontentloaded" });
    const setAll = page.getByRole("button", {
      name: "Set all to these values",
    });
    await expect(page.getByTestId("group-ranges-load-error")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(setAll).toBeDisabled();
    expect(puts).toHaveLength(0);

    await page.unroute(failing);
    await page.getByRole("button", { name: "Retry", exact: true }).click();
    await expect(page.getByTestId("group-ranges-load-error")).toBeHidden({
      timeout: UI_TIMEOUT,
    });
    await expect(setAll).toBeEnabled();
    expect((await rangesOf(page, a.id)).ranges).toHaveLength(a.ranges.length);
    expect((await rangesOf(page, b.id)).ranges).toHaveLength(b.ranges.length);
  });

  test("tests that differ only in their reporting range are flagged", async ({
    page,
  }) => {
    const pair = await rangedPair(page);
    test.skip(!pair, "needs two tests with numeric ranges");
    const [a, b] = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const primaryA = await primaryComponentOf(page, a.id);
    const primaryB = await primaryComponentOf(page, b.id);

    try {
      const shared = { minAge: 0, lowNormal: 2, highNormal: 9 };
      expect(
        (
          await page.request.put(
            `${API}/rest/test-catalog/tests/${a.id}/ranges`,
            {
              headers,
              data: {
                testId: a.id,
                ranges: [
                  {
                    ...shared,
                    componentId: primaryA,
                    lowReporting: 0,
                    highReporting: 50,
                  },
                ],
              },
            },
          )
        ).status(),
      ).toBe(200);
      expect(
        (
          await page.request.put(
            `${API}/rest/test-catalog/tests/${b.id}/ranges`,
            {
              headers,
              data: {
                testId: b.id,
                ranges: [
                  {
                    ...shared,
                    componentId: primaryB,
                    lowReporting: 0,
                    highReporting: 80,
                  },
                ],
              },
            },
          )
        ).status(),
      ).toBe(200);

      await page.goto(groupUrl(a.id, b.id), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("ranges-differ-warning")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });

      expect(
        (
          await page.request.put(
            `${API}/rest/test-catalog/tests/${b.id}/ranges`,
            {
              headers,
              data: {
                testId: b.id,
                ranges: [
                  {
                    ...shared,
                    componentId: primaryB,
                    lowReporting: 0,
                    highReporting: 50,
                  },
                ],
              },
            },
          )
        ).status(),
      ).toBe(200);
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("button", { name: "Set all to these values" }),
      ).toBeEnabled({ timeout: NAV_TIMEOUT });
      await expect(page.getByTestId("ranges-differ-warning")).toBeHidden();
    } finally {
      await page.request.put(`${API}/rest/test-catalog/tests/${a.id}/ranges`, {
        headers,
        data: { testId: a.id, ranges: withoutIds(a.ranges) },
      });
      await page.request.put(`${API}/rest/test-catalog/tests/${b.id}/ranges`, {
        headers,
        data: { testId: b.id, ranges: withoutIds(b.ranges) },
      });
    }
  });

  test("a bad group request is refused before any test is written", async ({
    page,
  }) => {
    const pair = await rangedPair(page);
    test.skip(!pair, "needs two tests with numeric ranges");
    const [a, b] = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const ranges = [{ minAge: 0, lowNormal: 1, highNormal: 2 }];
    const put = (url: string, data: unknown) =>
      page.request.put(`${API}${url}`, { headers, data });

    const badId = await put("/rest/test-catalog/group/ranges", {
      testIds: [a.id, b.id, "abc"],
      ranges,
    });
    expect(badId.status()).toBe(400);
    const unknownId = await put("/rest/test-catalog/group/ranges", {
      testIds: [a.id, "959999999"],
      ranges,
    });
    expect(unknownId.status()).toBe(404);
    const noRanges = await put("/rest/test-catalog/group/ranges", {
      testIds: [a.id, b.id],
      ranges: null,
    });
    expect(noRanges.status()).toBe(400);
    expect((await rangesOf(page, a.id)).ranges).toEqual(a.ranges);
    expect((await rangesOf(page, b.id)).ranges).toEqual(b.ranges);

    const storageBefore = await getJson<Record<string, unknown>>(
      page,
      `/rest/test-catalog/tests/${a.id}/storage`,
    );
    const badStorage = await put("/rest/test-catalog/group/storage", {
      testIds: [a.id, "abc"],
      storage: { stabilityNotes: "OGC-1455 probe" },
      fields: ["stabilityNotes"],
    });
    expect(badStorage.status()).toBe(400);
    expect(
      await getJson<Record<string, unknown>>(
        page,
        `/rest/test-catalog/tests/${a.id}/storage`,
      ),
    ).toEqual(storageBefore);
  });
});
