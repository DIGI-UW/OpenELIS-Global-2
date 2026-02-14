import { expect, Page, test } from "@playwright/test";

async function openWorkplanPage(path: string, page: Page) {
  await page.goto(path);
  await expect(page).toHaveURL(new RegExp(path));
  await expect(page.locator("#select-1")).toBeVisible();
}

async function selectFirstWorkplanOption(page: Page, endpointFragment: string) {
  const select = page.locator("#select-1");
  const options = select.locator("option");
  const optionCount = await options.count();
  test.skip(optionCount < 2, "No selectable workplan options available");

  const optionValue = await options.nth(1).getAttribute("value");
  const responsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes(endpointFragment) &&
        response.request().method() === "GET",
      { timeout: 10000 },
    )
    .catch(() => null);

  await select.selectOption({ index: 1 });
  await responsePromise;

  if (optionValue) {
    await expect(select).toHaveValue(optionValue);
  }

  const loadingGif = page.getByAltText("Loading ...");
  if ((await loadingGif.count()) > 0) {
    await expect(loadingGif).toBeHidden();
  }
}

async function expectResultsSectionIfPresent(page: Page) {
  const table = page.locator("[data-cy='workplanResultsTable']");
  const printButton = page.getByRole("button", { name: /print workplan/i });

  if ((await table.count()) > 0) {
    await expect(table.first()).toBeVisible();
    await expect(printButton).toBeVisible();
  }
}

test.describe("Workplan (smoke)", () => {
  test("workplan by panel loads and accepts filter selection", async ({
    page,
  }) => {
    await openWorkplanPage("/WorkplanByPanel", page);
    await selectFirstWorkplanOption(page, "/rest/WorkPlanByPanel");
    await expectResultsSectionIfPresent(page);
  });

  test("workplan by unit loads and accepts filter selection", async ({
    page,
  }) => {
    await openWorkplanPage("/WorkplanByTestSection", page);
    await selectFirstWorkplanOption(page, "/rest/WorkPlanByTestSection");
    await expectResultsSectionIfPresent(page);
  });

  test("workplan by priority loads and accepts filter selection", async ({
    page,
  }) => {
    await openWorkplanPage("/WorkplanByPriority", page);
    await selectFirstWorkplanOption(page, "/rest/WorkPlanByPriority");
    await expectResultsSectionIfPresent(page);
  });
});
