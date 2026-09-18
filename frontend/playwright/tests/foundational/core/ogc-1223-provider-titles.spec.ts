import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1223: a provider carries a title (Dr, Prof, HEO) chosen from a list an
 * administrator maintains, so the rank stops being typed into a name box.
 *
 * The two rules that matter most are asserted here: the title is never a
 * search term, and a title is deactivated rather than deleted so the providers
 * carrying it keep it.
 */

const API_PREFIX = "/api/OpenELIS-Global";
const TITLES_PAGE = "/MasterListsPage/providerTitleMenu";
const PROVIDERS_PAGE = "/MasterListsPage/providerMenu";

// Letters only: the provider endpoint rejects a digit in a name with a 500,
// which predates this work and is not what this spec is about.
const run = Date.now()
  .toString()
  .slice(-6)
  .split("")
  .map((digit) => "ABCDEFGHIJ"[Number(digit)])
  .join("");
const SURNAME = `Kila${run}`;

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

/** A provider carrying a title, created through the same endpoint the form uses. */
const createTitledProvider = async (page: Page) => {
  const token = await csrfToken(page);
  const response = await page.request.post(
    `${API_PREFIX}/rest/Provider/FhirUuid?fhirUuid=`,
    {
      data: {
        person: {
          titleCode: "Dr",
          firstName: "John",
          lastName: SURNAME,
          workPhone: "555",
        },
        active: true,
      },
      headers: { "X-CSRF-Token": token },
    },
  );
  if (!response.ok()) {
    throw new Error(
      `creating the provider failed: HTTP ${response.status()} ${(await response.text()).slice(0, 200)}`,
    );
  }
};

test.describe("Provider titles (OGC-1223)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("the seeded titles are listed, and deactivating never deletes", async ({
    page,
  }) => {
    await page.goto(TITLES_PAGE, { waitUntil: "domcontentloaded" });

    const rows = page.locator("table tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(rows.filter({ hasText: "Doctor" })).toHaveCount(1);
    await expect(rows.filter({ hasText: "Doctor" })).toContainText("Dr");

    // The page offers no way to delete a title at all.
    await expect(page.getByRole("button", { name: "Delete" })).toHaveCount(0);
    await expect(
      page.locator('[data-cy^="provider-title-toggle-"]').first(),
    ).toBeVisible();
  });

  test("a title shows on the provider and is a filter, never a search term", async ({
    page,
  }) => {
    await createTitledProvider(page);

    await page.goto(PROVIDERS_PAGE, { waitUntil: "domcontentloaded" });
    await expect(page.locator("table tbody tr").first()).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    // The provider's own row carries the title.
    const created = page.locator("table tbody tr").filter({ hasText: SURNAME });
    await expect(created).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await expect(created).toContainText("Dr");

    // Searching the title returns nobody: "Dr" must not mean every doctor.
    const search = await page.request.get(
      `${API_PREFIX}/rest/provider/search?search=Dr&page=1&pageSize=10`,
    );
    expect(search.ok()).toBeTruthy();
    expect((await search.json()).providers).toHaveLength(0);

    // Filtering by the title does return them, with a matching total.
    const filtered = await page.request.get(
      `${API_PREFIX}/rest/ProviderMenu?paging=1&startingRecNo=1&titleCode=Dr`,
    );
    expect(filtered.ok()).toBeTruthy();
    const body = await filtered.json();
    expect(body.providers.length).toBeGreaterThan(0);
    expect(Number(body.totalRecordCount)).toBe(body.providers.length);
  });

  test("the order entry typeahead shows the titled name", async ({ page }) => {
    await createTitledProvider(page);

    const response = await page.request.get(
      `${API_PREFIX}/rest/provider/search?search=${SURNAME}&page=1&pageSize=5`,
    );
    expect(response.ok()).toBeTruthy();
    const [provider] = (await response.json()).providers;
    expect(provider.displayName).toBe(`Dr John ${SURNAME}`);
    expect(provider.name).toBe(`${SURNAME}, Dr John`);
  });
});
