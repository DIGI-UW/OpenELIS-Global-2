import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1134 — the Test Catalog list search asks the server as you type. It
 * waits for a pause (debounce), aborts a stale request, and, since this ticket,
 * does not ask at all for a single character, which matches most of the
 * catalog and is not a search yet.
 */

const TESTS_ENDPOINT = /\/rest\/test-catalog\/tests\?/;

const searchTermOf = (url: string): string | null =>
  new URL(url).searchParams.get("search");

test.describe("OGC-1134 Test Catalog list search", () => {
  test("a single character asks nothing, two characters ask once, a word asks once more", async ({
    page,
  }) => {
    const searchRequests: string[] = [];
    page.on("request", (request) => {
      if (TESTS_ENDPOINT.test(request.url())) {
        const term = searchTermOf(request.url());
        if (term !== null) {
          searchRequests.push(term);
        }
      }
    });

    await page.goto("/MasterListsPage/TestCatalogList", {
      waitUntil: "domcontentloaded",
    });
    const search = page.locator("#test-search");
    await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });
    await page.waitForLoadState("networkidle");

    await search.pressSequentially("a");
    // Hold longer than the debounce, inside the page: if a single character
    // were going to ask, it would have by now.
    await search.evaluate(
      () => new Promise((resolve) => setTimeout(resolve, 700)),
    );
    expect(searchRequests, "no request for a single character").toEqual([]);
    await expect(page).not.toHaveURL(/search=a(&|$)/);

    const twoCharacters = page.waitForRequest(
      (request) =>
        TESTS_ENDPOINT.test(request.url()) &&
        searchTermOf(request.url()) === "am",
    );
    await search.pressSequentially("m");
    await twoCharacters;

    const wholeWord = page.waitForRequest(
      (request) =>
        TESTS_ENDPOINT.test(request.url()) &&
        searchTermOf(request.url()) === "amylase",
    );
    await search.pressSequentially("ylase");
    await wholeWord;
    await page.waitForLoadState("networkidle");

    expect(searchRequests, "one request per pause, none per keystroke").toEqual(
      ["am", "amylase"],
    );
    await expect(page).toHaveURL(/search=amylase/, { timeout: UI_TIMEOUT });
  });
});
