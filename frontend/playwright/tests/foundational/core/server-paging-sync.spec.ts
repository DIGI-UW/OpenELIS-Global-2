import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Lists the server pages are shown behind one Carbon Pagination that drives
 * the server's paging: Carbon's page is the server's page, its items per page
 * is pinned to what the server returns, and the arrows above the table stay in
 * step with it. Every move asks the server for exactly the page chosen.
 */

test.describe("Server paging through Carbon", () => {
  test("the dictionary list pages by record number from either control", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/DictionaryMenu", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    const showing = main.getByText(/Showing\s+1\s*-\s*20\s+of\s+\d+/);
    await expect(showing).toBeVisible({ timeout: NAV_TIMEOUT });
    const total = Number(
      ((await showing.innerText()).match(/of\s+(\d+)/) || [])[1],
    );
    test.skip(total < 41, "needs at least three server pages of entries");

    const carbon = main.locator(".cds--pagination").first();
    await expect(carbon.locator("select").first()).toBeDisabled();
    await expect(carbon).toContainText("1-20 of");

    const pageTwo = page.waitForResponse((response) =>
      response.url().includes("DictionaryMenu?startingRecNo=21"),
    );
    await carbon.getByRole("button", { name: /next page/i }).click();
    await pageTwo;
    await expect(main.getByText(/Showing\s+21\s*-\s*40\s+of/)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon).toContainText("21-40 of");

    const arrows = main
      .locator("button:visible")
      .filter({ has: page.locator("svg") })
      .filter({ hasNot: page.locator(".cds--pagination__button") })
      .filter({ hasNot: page.locator(".cds--table-sort__icon") });
    const pageOne = page.waitForResponse((response) =>
      response.url().includes("DictionaryMenu?startingRecNo=1"),
    );
    await arrows.nth(0).click();
    await pageOne;
    await expect(main.getByText(/Showing\s+1\s*-\s*20\s+of/)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon).toContainText("1-20 of");
    await expect(arrows.nth(0)).toBeDisabled();

    const pageThree = page.waitForResponse((response) =>
      response.url().includes("DictionaryMenu?startingRecNo=41"),
    );
    await carbon.locator(".cds--pagination__right select").selectOption("3");
    await pageThree;
    await expect(main.getByText(/Showing\s+41\s*-\s*60\s+of/)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon).toContainText("41-60 of");
  });

  test("a session-paged worklist asks the server for the page Carbon moves to", async ({
    page,
  }) => {
    await page.goto("/WorkPlanByTestSection", {
      waitUntil: "domcontentloaded",
    });
    const unit = page.locator("#select-1");
    await expect(unit).toBeVisible({ timeout: NAV_TIMEOUT });
    const units = await unit
      .locator("option")
      .evaluateAll((options) =>
        options
          .map((option) => (option as HTMLOptionElement).value)
          .filter(Boolean),
      );
    test.skip(units.length === 0, "needs a lab unit to open");

    const main = page.getByRole("main");
    const carbon = main.locator(".cds--pagination").first();
    let totalPages = 0;
    for (const unitId of units) {
      const loaded = page.waitForResponse((response) =>
        response
          .url()
          .includes(`WorkPlanByTestSection?test_section_id=${unitId}`),
      );
      await unit.selectOption(unitId);
      await loaded;
      await carbon.waitFor({ state: "visible", timeout: 5_000 }).catch(() => {
        // a unit with nothing to work on renders no table and no pagination
      });
      if ((await carbon.count()) === 0) {
        continue;
      }
      const pagesText = await carbon
        .locator(".cds--pagination__right")
        .innerText();
      totalPages = Number((pagesText.match(/of (\d+) page/) || [])[1]);
      if (totalPages >= 2) {
        break;
      }
    }
    test.skip(totalPages < 2, "needs a worklist the server splits in pages");
    await expect(carbon).toContainText(/items? on this page/, {
      timeout: UI_TIMEOUT,
    });

    await expect(carbon.locator("select").first()).toBeDisabled();
    await expect(main.locator("#loadnextresults")).toBeEnabled();
    const pageTwo = page.waitForResponse(
      (response) =>
        response.url().includes("WorkPlanByTestSection?") &&
        response.url().endsWith("&page=2"),
    );
    await carbon.getByRole("button", { name: /next page/i }).click();
    await pageTwo;
    await expect(main.getByText(`2 / ${totalPages}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon.locator(".cds--pagination__right select")).toHaveValue(
      "2",
    );

    const pageOne = page.waitForResponse(
      (response) =>
        response.url().includes("WorkPlanByTestSection?") &&
        response.url().endsWith("&page=1"),
    );
    await main.locator("#loadpreviousresults").click();
    await pageOne;
    await expect(main.getByText(`1 / ${totalPages}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon.locator(".cds--pagination__right select")).toHaveValue(
      "1",
    );
    await expect(main.locator("#loadpreviousresults")).toBeDisabled();
  });
});
