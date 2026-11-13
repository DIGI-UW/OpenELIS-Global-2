import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { letters } from "../../../helpers/seed-patient-order";

/**
 * The old units-of-measure and method rename screens were replaced by Units of
 * Measure and Manage Methods: each lists its entries, adds one and corrects
 * one. A blank name never leaves the page, and a name another entry already
 * has is refused on the name field instead of being reported as saved. The
 * Test Management page links both, and the result select list pages, which
 * have no replacement and stay; the old addresses open the new pages.
 */

const dialog = (page: Page) => page.getByRole("dialog");

test.describe("Test Management: units of measure, methods and result select lists", () => {
  test("the menu links the new pages and the old addresses open them", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/testManagementConfigMenu", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    await expect(
      main.getByRole("link", { name: /Units of Measure/ }),
    ).toHaveAttribute("href", "/MasterListsPage/UnitsOfMeasure", {
      timeout: NAV_TIMEOUT,
    });
    await expect(
      main.getByRole("link", { name: /Add result select list/ }),
    ).toHaveAttribute("href", "/MasterListsPage/ResultSelectListAdd");
    await expect(
      main.getByRole("link", { name: /Rename existing result list options/ }),
    ).toHaveAttribute("href", "/MasterListsPage/SelectListRenameEntry");

    for (const [legacy, target] of [
      ["UomManagement", "UnitsOfMeasure"],
      ["UomRenameEntry", "UnitsOfMeasure"],
      ["MethodRenameEntry", "MethodManagement"],
    ]) {
      await page.goto(`/MasterListsPage/${legacy}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page).toHaveURL(new RegExp(`/MasterListsPage/${target}$`), {
        timeout: NAV_TIMEOUT,
      });
    }
  });

  test("a unit is added, corrected, and a taken or blank name is refused", async ({
    page,
  }) => {
    const name = `PW unit ${letters(6)}`;
    const renamed = `${name} fixed`;
    await page.goto("/MasterListsPage/UnitsOfMeasure", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    const firstUnit = main.locator("tbody tr td").first();
    await expect(firstUnit).not.toBeEmpty({ timeout: NAV_TIMEOUT });
    const takenName = (await firstUnit.innerText()).trim();

    await main.getByRole("button", { name: "Add Unit" }).click();
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await expect(
      dialog(page).getByText("This field is required"),
    ).toBeVisible();

    await dialog(page).getByLabel("Unit Of Measure Name").fill(name);
    await dialog(page).getByLabel("UCUM code").fill("{cells}/uL");
    const created = page.waitForResponse(
      (r) => r.url().endsWith("/rest/uom") && r.request().method() === "POST",
    );
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await created;
    await expect(dialog(page)).toHaveCount(0);

    await main.getByRole("searchbox").fill(name);
    const row = main.locator("tbody tr", { hasText: name });
    await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
    await expect(row).toContainText("{cells}/uL");

    await row.getByRole("button", { name: "Edit" }).click();
    await expect(dialog(page).getByLabel("Unit Of Measure Name")).toHaveValue(
      name,
    );
    await dialog(page).getByLabel("Unit Of Measure Name").fill(takenName);
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await expect(
      dialog(page).getByText("Unit of Measure already exists"),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    await dialog(page).getByLabel("Unit Of Measure Name").fill(renamed);
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await expect(dialog(page)).toHaveCount(0, { timeout: UI_TIMEOUT });
    await main.getByRole("searchbox").fill(renamed);
    await expect(main.locator("tbody tr", { hasText: renamed })).toHaveCount(
      1,
      { timeout: UI_TIMEOUT },
    );
  });

  test("a method is added inactive, renamed, and a taken name is refused", async ({
    page,
  }) => {
    const english = `PW Meth ${letters(6)}`;
    const french = `PW Methode ${letters(4)}`;
    const renamed = `PW Ren ${letters(6)}`;
    await page.goto("/MasterListsPage/MethodManagement", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    const firstMethod = main.locator("tbody tr td").first();
    await expect(firstMethod).not.toBeEmpty({ timeout: NAV_TIMEOUT });
    const takenName = (await firstMethod.innerText()).trim();

    await main.getByRole("button", { name: "Add New Method" }).click();
    await dialog(page).getByLabel("English").fill(english);
    await dialog(page).getByLabel("French").fill(french);
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await expect(dialog(page)).toHaveCount(0, { timeout: UI_TIMEOUT });

    await main.getByRole("searchbox").fill(english);
    const row = main.locator("tbody tr", { hasText: english });
    await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
    await expect(row).toContainText(french);
    await expect(row).toContainText("Inactive");

    await row.getByRole("button", { name: "Edit" }).click();
    await dialog(page).getByLabel("English").fill(takenName);
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await expect(dialog(page).getByText(/already exists/)).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    await dialog(page).getByLabel("English").fill(renamed);
    await dialog(page).getByRole("button", { name: "Save" }).click();
    await expect(dialog(page)).toHaveCount(0, { timeout: UI_TIMEOUT });
    await main.getByRole("searchbox").fill(renamed);
    await expect(main.locator("tbody tr", { hasText: renamed })).toContainText(
      french,
      { timeout: UI_TIMEOUT },
    );
  });
});
