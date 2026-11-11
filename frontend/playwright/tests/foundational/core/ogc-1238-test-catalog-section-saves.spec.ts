import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1238: Test Catalog section saves keep data and take effect.
 *
 * - "Edit related tests" > Ranges keeps a range's specimen scope.
 * - "Edit related tests" > Sample Storage names the fields that differ and
 *   writes only the fields the admin changed.
 * - Display Order opens on the edited test's sample type, and order entry
 *   lists that sample type's tests in the saved order.
 * - A Localization rename shows in the test list at once.
 * - The "Copy from test" pickers filter as you type.
 *
 * Every save restores what it changed.
 */

const API = "/api/OpenELIS-Global";

interface TestOption {
  id: string;
  value: string;
}

interface SampleTypeOption {
  id: string;
  name: string;
}

interface RangesResponse {
  ranges: Record<string, unknown>[];
  sampleTypes: SampleTypeOption[];
}

/** CSRF token saved by auth.setup into storageState localStorage (key "CSRF"). */
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

/** Two specimen siblings (same name stem) on different sample types. */
const siblingPair = async (page: Page) => {
  const tests = await getJson<TestOption[]>(page, "/rest/test-list");
  const byStem = new Map<string, TestOption[]>();
  for (const t of tests) {
    const paren = t.value.lastIndexOf("(");
    if (paren > 0) {
      const stem = t.value.slice(0, paren).trim();
      byStem.set(stem, [...(byStem.get(stem) || []), t]);
    }
  }
  for (const group of byStem.values()) {
    if (group.length < 2) continue;
    const [a, b] = group;
    const rangesA = await getJson<RangesResponse>(
      page,
      `/rest/test-catalog/tests/${a.id}/ranges`,
    );
    const rangesB = await getJson<RangesResponse>(
      page,
      `/rest/test-catalog/tests/${b.id}/ranges`,
    );
    const typeA = rangesA.sampleTypes[0];
    const typeB = rangesB.sampleTypes[0];
    if (typeA && typeB && typeA.id !== typeB.id) {
      return { a, b, typeA, rangesA, rangesB };
    }
  }
  return null;
};

const withoutIds = (ranges: Record<string, unknown>[]) =>
  ranges.map(({ id: _id, ...r }) => r);

test.describe("Test Catalog section saves keep data and take effect (OGC-1238)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("group Ranges keeps a range's specimen scope", async ({ page }) => {
    const pair = await siblingPair(page);
    test.skip(!pair, "needs two specimen siblings on different sample types");
    const { a, b, typeA, rangesA, rangesB } = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };

    try {
      const scoped = await page.request.put(
        `${API}/rest/test-catalog/tests/${a.id}/ranges`,
        {
          headers,
          data: {
            testId: a.id,
            ranges: [{ sampleTypeId: typeA.id, lowNormal: 1, highNormal: 5 }],
          },
        },
      );
      expect(scoped.status()).toBe(200);

      await page.goto(
        `/MasterListsPage/TestCatalogEditor/group/${a.id},${b.id}/ranges`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("group-range-sample-type-0")).toHaveText(
        typeA.name,
        { timeout: NAV_TIMEOUT },
      );
      await expect(page.getByTestId("ranges-differ-warning")).toBeVisible();

      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith("/rest/test-catalog/group/ranges") &&
          r.request().method() === "PUT",
      );
      await page
        .getByRole("button", { name: "Set all to these values" })
        .click();
      await saved;
      await expect(
        page.getByText(/Reference ranges saved to 2 tests/),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      const after = await getJson<RangesResponse>(
        page,
        `/rest/test-catalog/tests/${a.id}/ranges`,
      );
      expect(after.ranges.map((r) => r.sampleTypeId)).toEqual([typeA.id]);
    } finally {
      await page.request.put(`${API}/rest/test-catalog/tests/${a.id}/ranges`, {
        headers,
        data: { testId: a.id, ranges: withoutIds(rangesA.ranges) },
      });
      await page.request.put(`${API}/rest/test-catalog/tests/${b.id}/ranges`, {
        headers,
        data: { testId: b.id, ranges: withoutIds(rangesB.ranges) },
      });
    }
  });

  test("group Sample Storage names what differs and writes only what changed", async ({
    page,
  }) => {
    const pair = await siblingPair(page);
    test.skip(!pair, "needs two specimen siblings on different sample types");
    const { a, b } = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const storageUrl = (id: string) => `/rest/test-catalog/tests/${id}/storage`;
    const originalA = await getJson<Record<string, unknown>>(
      page,
      storageUrl(a.id),
    );
    const originalB = await getJson<Record<string, unknown>>(
      page,
      storageUrl(b.id),
    );

    try {
      await page.request.put(`${API}${storageUrl(a.id)}`, {
        headers,
        data: { storageCondition: "FROZEN", protectFromLight: true },
      });
      await page.request.put(`${API}${storageUrl(b.id)}`, {
        headers,
        data: { storageCondition: "REFRIGERATED", protectFromLight: false },
      });

      await page.goto(
        `/MasterListsPage/TestCatalogEditor/group/${a.id},${b.id}/ranges`,
        { waitUntil: "domcontentloaded" },
      );
      await page
        .getByRole("tab", { name: "Sample Storage" })
        .click({ timeout: NAV_TIMEOUT });
      const warning = page.getByTestId("storage-differ-warning");
      await expect(warning).toContainText("Storage condition", {
        timeout: UI_TIMEOUT,
      });
      await expect(warning).toContainText("Protect from light");
      const save = page
        .getByTestId("storage-section")
        .getByRole("button", { name: "Save", exact: true });
      await expect(save).toBeDisabled();

      await page.locator("#storage-stability-notes").fill("OGC-1238 upright");
      await expect(save).toBeEnabled();
      const saved = page.waitForResponse((r) =>
        r.url().endsWith("/rest/test-catalog/group/storage"),
      );
      await save.click();
      await saved;
      await expect(save).toBeDisabled({ timeout: UI_TIMEOUT });

      const storedA = await getJson<Record<string, unknown>>(
        page,
        storageUrl(a.id),
      );
      const storedB = await getJson<Record<string, unknown>>(
        page,
        storageUrl(b.id),
      );
      expect(storedA).toMatchObject({
        storageCondition: "FROZEN",
        protectFromLight: true,
        stabilityNotes: "OGC-1238 upright",
      });
      expect(storedB).toMatchObject({
        storageCondition: "REFRIGERATED",
        protectFromLight: false,
        stabilityNotes: "OGC-1238 upright",
      });
    } finally {
      await page.request.put(`${API}${storageUrl(a.id)}`, {
        headers,
        data: originalA,
      });
      await page.request.put(`${API}${storageUrl(b.id)}`, {
        headers,
        data: originalB,
      });
    }
  });

  test("Display Order opens on the test's sample type and order entry follows it", async ({
    page,
  }) => {
    const pair = await siblingPair(page);
    test.skip(!pair, "needs two specimen siblings on different sample types");
    const { a, typeA } = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const orderUrl = `/rest/test-catalog/sample-types/${typeA.id}/test-order`;
    const original = await getJson<{
      tests: { testId: string; displayOrder: number | null }[];
    }>(page, orderUrl);

    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${a.id}/display-order`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await expect(page.locator("#display-order-sample-type")).toHaveValue(
        typeA.id,
        { timeout: NAV_TIMEOUT },
      );
      await expect(
        page
          .getByTestId(`order-row-${a.id}`)
          .getByTestId("display-order-current-test"),
      ).toHaveText("This test");

      const saved = page.waitForResponse(
        (r) => r.url().endsWith(orderUrl) && r.request().method() === "PUT",
      );
      const rows = page.locator('[data-testid^="order-row-"]');
      const firstRow = rows.first();
      const firstId = (await firstRow.getAttribute("data-testid"))!.replace(
        "order-row-",
        "",
      );
      const mover = firstId === a.id ? rows.nth(1) : firstRow;
      const moverId = (await mover.getAttribute("data-testid"))!.replace(
        "order-row-",
        "",
      );
      await page.getByTestId(`move-down-${moverId}`).click();
      await saved;
      const expected = (
        await getJson<{ tests: { testId: string }[] }>(page, orderUrl)
      ).tests.map((t) => t.testId);

      const entry = await getJson<{ tests: { id: string }[] }>(
        page,
        `/rest/sample-type-tests?sampleType=${typeA.id}`,
      );
      const listed = entry.tests.map((t) => t.id);
      expect(listed).toEqual(expected.filter((id) => listed.includes(id)));
    } finally {
      await page.request.put(`${API}${orderUrl}`, {
        headers,
        data: {
          items: original.tests
            .filter((t) => t.displayOrder != null)
            .map((t) => ({ testId: t.testId, displayOrder: t.displayOrder })),
        },
      });
    }
  });

  test("a Localization rename shows in the test list and the copy picker filters", async ({
    page,
  }) => {
    const pair = await siblingPair(page);
    test.skip(!pair, "needs two specimen siblings on different sample types");
    const { a, b } = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const refs = await getJson<{
      fields: { field: string; localizationId: string }[];
    }>(page, `/rest/test-catalog/tests/${a.id}/localization`);
    const nameRef = refs.fields.find((f) => f.field === "name");
    test.skip(!nameRef, "the test has no name localization");
    const original = await getJson<{ translations: Record<string, string> }>(
      page,
      `/rest/localizations/${nameRef!.localizationId}`,
    );
    const renamed = `${original.translations.en} R${Date.now().toString().slice(-5)}`;

    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${a.id}/localization`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      const input = page.locator("#localization-input-name");
      await expect(input).toBeVisible({ timeout: NAV_TIMEOUT });
      await input.fill(renamed);
      const saved = page.waitForResponse(
        (r) =>
          r.url().includes(`/rest/localizations/${nameRef!.localizationId}`) &&
          r.request().method() === "PUT",
      );
      await page
        .getByTestId("localization-section")
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await saved;

      const list = await getJson<TestOption[]>(page, "/rest/test-list");
      expect(list.find((t) => t.id === a.id)?.value).toContain(renamed);

      await page.goto(`/MasterListsPage/TestCatalogEditor/${b.id}/methods`, {
        waitUntil: "domcontentloaded",
      });
      const picker = page.getByRole("combobox", {
        name: "Copy methods from test",
      });
      await expect(picker).toBeVisible({ timeout: NAV_TIMEOUT });
      await picker.fill(renamed);
      const options = page.locator(".cds--list-box__menu-item");
      await expect(options).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(options).toContainText(renamed);
    } finally {
      await page.request.put(
        `${API}/rest/localizations/${nameRef!.localizationId}/translations`,
        { headers, data: { en: original.translations.en } },
      );
    }
  });
});
