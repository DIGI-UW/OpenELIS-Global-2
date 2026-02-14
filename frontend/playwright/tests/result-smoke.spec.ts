import { expect, test } from "@playwright/test";

test.describe("Results (smoke)", () => {
  test("logbook results exposes unit selector and accepts selection", async ({
    page,
  }) => {
    await page.goto("/LogbookResults");
    await expect(page).toHaveURL(/\/LogbookResults/);

    const unitType = page.locator("#unitType");
    await expect(unitType).toBeVisible();

    const options = unitType.locator("option");
    const optionCount = await options.count();
    test.skip(optionCount < 2, "No selectable unit options available");

    const selectedValue = await options.nth(1).getAttribute("value");
    const responsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes("/rest/LogbookResults?") &&
          response.request().method() === "GET",
        { timeout: 10000 },
      )
      .catch(() => null);

    await unitType.selectOption({ index: 1 });
    await responsePromise;

    if (selectedValue) {
      await expect(unitType).toHaveValue(selectedValue);
    }
  });

  test("patient results exposes patient search workflow", async ({ page }) => {
    await page.goto("/PatientResults");
    await expect(page).toHaveURL(/\/PatientResults/);

    await expect(page.locator("#lastName")).toBeVisible();
    await expect(page.locator("#firstName")).toBeVisible();
    await expect(page.locator("#local_search")).toBeVisible();

    const patientResponsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes("/rest/patient-search-results?") &&
          response.request().method() === "GET",
        { timeout: 10000 },
      )
      .catch(() => null);

    await page.locator("#lastName").fill("Smith");
    await page.locator("#firstName").fill("John");
    await page.locator("#local_search").click();
    await patientResponsePromise;
  });

  test("accession results can submit accession search", async ({ page }) => {
    await page.goto("/AccessionResults");
    await expect(page).toHaveURL(/\/AccessionResults/);

    await expect(page.locator("#accessionNumber")).toBeVisible();
    await expect(page.locator("#searchResults")).toBeVisible();

    const resultResponsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes("/rest/LogbookResults?") &&
          response.request().method() === "GET",
        { timeout: 10000 },
      )
      .catch(() => null);

    await page.locator("#accessionNumber").fill("DEV01260000000000001");
    await page.locator("#searchResults").click();
    await resultResponsePromise;
  });

  test("status and range result routes expose route-specific controls", async ({
    page,
  }) => {
    await page.goto("/StatusResults");
    await expect(page).toHaveURL(/\/StatusResults/);
    await expect(page.locator("#collectionDate")).toBeVisible();
    await expect(page.locator("#recievedDate")).toBeVisible();
    await expect(page.locator("#testName")).toBeVisible();
    await expect(page.locator("#searchResults")).toBeVisible();

    await page.goto("/RangeResults");
    await expect(page).toHaveURL(/\/RangeResults/);
    await expect(page.locator("#startLabNo")).toBeVisible();
    await expect(page.locator("#endLabNo")).toBeVisible();
    await expect(page.locator("#searchResults")).toBeVisible();
  });
});
