import { expect, type Locator, type Page } from "@playwright/test";
import { csrfToken } from "./api-session";
import { NAV_TIMEOUT } from "./timeouts";

/**
 * A per-run suffix spelled in letters, for fields whose validators reject
 * digits (provider names, for one).
 */
export function letterSuffix(): string {
  return `${Date.now()}${Math.floor(Math.random() * 100)}`
    .slice(-8)
    .split("")
    .map((digit) => "ABCDEFGHIJ"[Number(digit)])
    .join("");
}

/**
 * The cell of `row` in the column headed `header`, located by the header's
 * position so the assertion names the column instead of an index.
 */
export async function cellUnder(
  table: Locator,
  row: Locator,
  header: string,
): Promise<Locator> {
  const headers = (await table.locator("thead th").allInnerTexts()).map(
    (text) => text.trim(),
  );
  const index = headers.indexOf(header);
  if (index < 0) {
    throw new Error(`no "${header}" column in [${headers.join(", ")}]`);
  }
  return row.locator("td").nth(index);
}

/**
 * Types `term` into an admin list's search box and returns the one row whose
 * text contains it.
 */
export async function searchListFor(
  page: Page,
  searchBox: Locator,
  term: string,
): Promise<Locator> {
  await searchBox.fill(term);
  const rows = page.locator("table tbody tr").filter({ hasText: term });
  await expect(rows).toHaveCount(1, { timeout: NAV_TIMEOUT });
  return rows.first();
}

/**
 * Waits for the POST a click triggers, so a following reload cannot cancel
 * it. Synchronisation only: the caller asserts the outcome on screen.
 */
export async function clickAndAwaitPost(
  page: Page,
  path: RegExp,
  click: () => Promise<void>,
): Promise<void> {
  const posted = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" && path.test(response.url()),
  );
  await click();
  await posted;
}

export const API_PREFIX = "/api/OpenELIS-Global";

/** POSTs as the session user and throws with the server's reply on failure. */
export async function apiPost(
  page: Page,
  path: string,
  data: unknown,
): Promise<unknown> {
  const response = await page.request.post(`${API_PREFIX}${path}`, {
    data,
    headers: { "X-CSRF-Token": await csrfToken(page) },
  });
  if (!response.ok()) {
    throw new Error(
      `POST ${path} failed: HTTP ${response.status()} ${(await response.text()).slice(0, 200)}`,
    );
  }
  return response.json().catch(() => undefined);
}

type AdminList = "organization" | "provider" | "dictionary";

interface MenuItem {
  id: string;
  organizationName?: string;
  dictEntry?: string;
  person?: { lastName?: string };
}

const LISTS: Record<
  AdminList,
  { search: string; deactivate: string; name: (item: MenuItem) => string }
> = {
  organization: {
    search: "SearchOrganizationMenu",
    deactivate: "DeleteOrganization",
    name: (item) => item.organizationName ?? "",
  },
  provider: {
    search: "SearchProviderMenu",
    deactivate: "DeleteProvider",
    name: (item) => item.person?.lastName ?? "",
  },
  dictionary: {
    search: "SearchDictionaryMenu",
    deactivate: "DeleteDictionary",
    name: (item) => item.dictEntry ?? "",
  },
};

/**
 * Deactivates the named records through the list's own Deactivate endpoint.
 * These lists never hard-delete; names missing from the list are skipped.
 */
export async function deactivateByName(
  page: Page,
  list: AdminList,
  names: string[],
): Promise<void> {
  const { search, deactivate, name } = LISTS[list];
  for (const wanted of names) {
    const response = await page.request.get(
      `${API_PREFIX}/rest/${search}?search=Y&startingRecNo=1&searchString=${encodeURIComponent(wanted)}`,
    );
    const { menuList = [] } = (await response.json()) as {
      menuList?: MenuItem[];
    };
    const ids = menuList
      .filter((item) => name(item) === wanted)
      .map((i) => i.id);
    if (ids.length === 0) continue;
    await apiPost(
      page,
      `/rest/${deactivate}?ID=${ids.join(",")}&startingRecNo=1`,
      list === "provider" ? [] : {},
    );
  }
}
