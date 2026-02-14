import { expect, test } from "@playwright/test";
import { MenuNavigation } from "../fixtures/menu-navigation";

async function openNonConformRoute(
  menu: MenuNavigation,
  leafMenuId: string,
  expectedPath: RegExp,
) {
  await menu.page.goto("/Dashboard");
  const opened = await menu.clickMenuLeaf("menu_nonconformity", leafMenuId);
  test.skip(!opened, `${leafMenuId} is not available for this configuration`);
  await expect(menu.page).toHaveURL(expectedPath);
}

async function runBasicSearch(
  menu: MenuNavigation,
  endpointFragment: string,
  searchValue: string,
) {
  const page = menu.page;
  const searchType = page.locator("#type");
  const searchField = page.locator("[id='field.name']");
  const searchButton = page.getByTestId("nce-search-button");

  await expect(searchType).toBeVisible();
  await expect(searchField).toBeVisible();
  await expect(searchButton).toBeVisible();

  const optionCount = await searchType.locator("option").count();
  test.skip(optionCount < 2, "No selectable non-conform search type available");

  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes(endpointFragment) &&
        response.request().method() === "GET",
      { timeout: 10000 },
    )
    .catch(() => null);

  await searchType.selectOption({ index: 1 });
  await searchField.fill(searchValue);
  await searchButton.click();
  await responsePromise;
}

test.describe("Non-conforming workflows (smoke)", () => {
  test("report non-conforming event route is reachable from side nav", async ({
    page,
  }) => {
    const menu = new MenuNavigation(page);
    await openNonConformRoute(
      menu,
      "menu_non_conforming_report",
      /\/ReportNonConformingEvent/,
    );
    await runBasicSearch(menu, "/rest/nonconformevents?", "Smith");
  });

  test("view non-conforming event route is reachable from side nav", async ({
    page,
  }) => {
    const menu = new MenuNavigation(page);
    await openNonConformRoute(
      menu,
      "menu_non_conforming_view",
      /\/ViewNonConformingEvent/,
    );
    await runBasicSearch(menu, "/rest/viewNonConformEvents?", "DEV0126");
  });

  test("corrective action route is reachable from side nav", async ({
    page,
  }) => {
    const menu = new MenuNavigation(page);
    await openNonConformRoute(
      menu,
      "menu_non_conforming_corrective_actions",
      /\/NCECorrectiveAction/,
    );
    await runBasicSearch(
      menu,
      "/rest/nonconformingcorrectiveaction?",
      "DEV0126",
    );
  });
});
