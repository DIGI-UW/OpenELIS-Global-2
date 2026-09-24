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
    await expect
      .poll(() => unit.locator("option").count(), { timeout: NAV_TIMEOUT })
      .toBeGreaterThan(1);
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

  test("the merge screen's two panels are the shared patient search and hide the patient chosen opposite", async ({
    page,
  }) => {
    await page.goto("/PatientMerge", { waitUntil: "domcontentloaded" });
    const firstLastName = page.locator("#patient1-lastName");
    await expect(firstLastName).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(page.locator("#patient2-lastName")).toBeVisible();
    await expect(page.locator("#lastName")).toHaveCount(0);

    await firstLastName.fill("a");
    const searched = page.waitForResponse((response) =>
      response.url().includes("/rest/patient-search-results"),
    );
    await page.locator("#patient1-local_search").click();
    await searched;
    const panels = page.locator(".patientSelectionSection");
    const firstRows = panels.nth(0).locator("tbody tr");
    const found = await firstRows.count();
    test.skip(found === 0, "needs at least one local patient to choose");
    await expect(panels.nth(0).locator(".cds--pagination")).toBeVisible();

    const chosenRow = firstRows.first();
    const chosen = (await chosenRow.getAttribute("data-cy")) || "";
    const details = page.waitForResponse((response) =>
      response.url().includes("/rest/patient/merge/details/"),
    );
    await chosenRow.locator(".cds--radio-button__label").first().click();
    await details;
    await expect(
      panels
        .nth(0)
        .getByRole("button", { name: /Search for different patient/i }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.locator("#patient1-lastName")).toHaveCount(0);

    await page.locator("#patient2-lastName").fill("a");
    const searchedAgain = page.waitForResponse((response) =>
      response.url().includes("/rest/patient-search-results"),
    );
    await page.locator("#patient2-local_search").click();
    await searchedAgain;
    const secondRows = panels.nth(1).locator("tbody tr");
    await expect(secondRows.first()).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(panels.nth(1).locator(`[data-cy="${chosen}"]`)).toHaveCount(0);
  });

  test("the clinical order dashboard pages the server's list from either control", async ({
    page,
  }) => {
    await page.goto("/order/clinical", { waitUntil: "domcontentloaded" });
    const main = page.getByRole("main");
    const carbon = main.locator(".cds--pagination").first();
    await expect(carbon).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(carbon).toContainText(/items? on this page/, {
      timeout: NAV_TIMEOUT,
    });
    const pagesText = await carbon
      .locator(".cds--pagination__right")
      .innerText();
    const totalPages = Number((pagesText.match(/of (\d+) page/) || [])[1]);
    test.skip(totalPages < 2, "needs more orders than one server page holds");
    await expect(carbon.locator("select").first()).toBeDisabled();

    const pageTwo = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/order/dashboard?page=2") ||
        response.url().endsWith("/rest/order/dashboard?page=2"),
    );
    await carbon.getByRole("button", { name: /next page/i }).click();
    await pageTwo;
    await expect(main.getByText(`2 / ${totalPages}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon.locator(".cds--pagination__right select")).toHaveValue(
      "2",
    );

    const pageOne = page.waitForResponse((response) =>
      response.url().includes("/rest/order/dashboard?page=1"),
    );
    await main.locator("#loadpreviousresults").click();
    await pageOne;
    await expect(main.getByText(`1 / ${totalPages}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(main.locator("#loadpreviousresults")).toBeDisabled();

    const filtered = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/order/dashboard?") &&
        response.url().includes("status=in_progress") &&
        !response.url().includes("page="),
    );
    await main.locator("#status-filter").click();
    await page.getByRole("option", { name: "In Progress" }).click();
    await filtered;
    await expect(carbon.locator(".cds--pagination__right select")).toHaveValue(
      "1",
      { timeout: UI_TIMEOUT },
    );
  });

  test("the pathology dashboard pages the server's cases from either control", async ({
    page,
  }) => {
    await page.goto("/PathologyDashboard", { waitUntil: "domcontentloaded" });
    const main = page.getByRole("main");
    const statusFilter = main
      .locator("select")
      .filter({ has: page.locator('option[value="All"]') })
      .first();
    await expect(statusFilter).toBeVisible({ timeout: NAV_TIMEOUT });
    const allLoaded = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/pathology/dashboard?") &&
        !response.url().includes("page="),
    );
    await statusFilter.selectOption("All");
    await allLoaded;
    const carbon = main.locator(".cds--pagination").first();
    await expect(carbon).toContainText(/items? on this page/, {
      timeout: UI_TIMEOUT,
    });
    const pagesText = await carbon
      .locator(".cds--pagination__right")
      .innerText();
    const totalPages = Number((pagesText.match(/of (\d+) page/) || [])[1]);
    test.skip(totalPages < 2, "needs more cases than one server page holds");

    const pageTwo = page.waitForResponse((response) =>
      response.url().includes("/rest/pathology/dashboard?page=2"),
    );
    await main.locator("#loadnextresults").click();
    await pageTwo;
    await expect(main.getByText(`2 / ${totalPages}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(carbon.locator(".cds--pagination__right select")).toHaveValue(
      "2",
    );

    const pageOne = page.waitForResponse((response) =>
      response.url().includes("/rest/pathology/dashboard?page=1"),
    );
    await carbon.locator(".cds--pagination__right select").selectOption("1");
    await pageOne;
    await expect(main.getByText(`1 / ${totalPages}`)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(main.locator("#loadpreviousresults")).toBeDisabled();
  });
});
