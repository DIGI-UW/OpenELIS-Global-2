import { test, expect } from "../../../helpers/test-base";

test.describe("Storage CRUD - Rooms", () => {
  test("add room flow", async ({ page }) => {
    const roomName = `PW Room ${Date.now()}`;
    await page.goto("/Storage/rooms/new", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/rooms\/new/);

    const nameInput = page.locator("#storage-add-name");
    test.skip(
      (await nameInput.count()) === 0,
      "Add room page unavailable in current test environment",
    );

    await nameInput.fill(roomName);
    await page
      .locator("#storage-add-code")
      .fill(`PW-${Date.now().toString().slice(-6)}`);
    await page.getByRole("button", { name: "Add" }).click();

    await expect(page).toHaveURL(/\/Storage\/rooms\?t=\d+/);
    await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();
  });

  test("edit room flow via overflow menu", async ({ page }) => {
    await page.goto("/Storage/rooms", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();

    const overflowMenu = page.locator(
      ".storage-resource-page .cds--overflow-menu",
    );
    test.skip((await overflowMenu.count()) === 0, "No room rows available");

    await overflowMenu.first().click();
    await page.getByRole("menuitem", { name: "Edit" }).click();

    await expect(page).toHaveURL(/\/Storage\/rooms\/\d+\/edit/);
    await expect(page.getByRole("heading", { name: /Edit/i })).toBeVisible();
  });

  test("delete room flow with cascade summary", async ({ page }) => {
    await page.goto("/Storage/rooms", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Rooms" })).toBeVisible();

    const overflowMenu = page.locator(
      ".storage-resource-page .cds--overflow-menu",
    );
    test.skip((await overflowMenu.count()) === 0, "No room rows available");

    await overflowMenu.first().click();
    await page.getByRole("menuitem", { name: "Delete" }).click();

    await expect(page.getByText("Cascade Delete Warning")).toBeVisible();

    const deleteButton = page.getByRole("button", { name: "Delete" });
    await expect(deleteButton).toBeDisabled();

    await page
      .getByLabel(
        "I confirm that I want to delete this location and all its child locations. All samples will be unassigned. This action cannot be undone.",
      )
      .check();
    await expect(deleteButton).toBeEnabled();

    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(page.getByText("Cascade Delete Warning")).toBeHidden();
  });
});
