import { test, expect } from "../../../helpers/test-base";

const USER_LIST = "/rest/SearchUnifiedSystemUserMenu";

const filterParam = (url: string) =>
  (new URL(url).searchParams.get("filter") || "").split(",").filter(Boolean);

test.describe("User management filters", () => {
  test("filtering to active users asks the server and keeps the choice shown", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/userManagement", {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "User Management" }),
    ).toBeVisible();

    const onlyActive = page.locator("#only-active");
    await expect(onlyActive).toHaveCount(1);
    await expect(onlyActive).not.toBeChecked();

    // The reload the click triggers is what settles the control, so sync on it.
    const activeUsersLoaded = page.waitForResponse(
      (response) =>
        response.url().includes(USER_LIST) &&
        filterParam(response.url()).includes("isActive"),
    );

    // Carbon hides the input itself; the label is the clickable surface.
    await page.locator('label[for="only-active"]').click();
    await activeUsersLoaded;

    // The filter is the behaviour; the box showing it is what a user reads to
    // know the list is narrowed, so both have to hold once the reload lands.
    await expect(onlyActive).toBeChecked();
    await expect(page.locator(".cds--data-table")).toBeVisible();
  });

  test("keeps the choice shown when the screen was reached by navigation", async ({
    page,
  }) => {
    // The Cypress spec reaches this screen from the admin nav rather than a
    // fresh load, which is the only difference between its failing and passing
    // uses of the same filter.
    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
    await page
      .locator(".cds--side-nav")
      .getByText("User Management", { exact: true })
      .click();

    await expect(
      page.getByRole("heading", { name: "User Management" }),
    ).toBeVisible();

    const onlyActive = page.locator("#only-active");
    await expect(onlyActive).toHaveCount(1);
    await expect(onlyActive).not.toBeChecked();

    const activeUsersLoaded = page.waitForResponse(
      (response) =>
        response.url().includes(USER_LIST) &&
        filterParam(response.url()).includes("isActive"),
    );
    await page.locator('label[for="only-active"]').click();
    await activeUsersLoaded;

    await expect(onlyActive).toBeChecked();
  });
});
