import { test, expect } from "../../../helpers/test-base";

test.describe("Storage CRUD - Boxes", () => {
  test("add box flow with preset dimensions", async ({ page }) => {
    await page.goto("/Storage/boxes/new", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/boxes\/new/);

    const labelInput = page.locator("#box-add-label");
    test.skip(
      (await labelInput.count()) === 0,
      "Add box page unavailable in current test environment",
    );

    await labelInput.fill(`PW Box Preset ${Date.now()}`);
    await page
      .locator("#box-add-code")
      .fill(`BX-${Date.now().toString().slice(-6)}`);
    await page.locator("#box-add-preset").click();
    await page.getByRole("option", { name: "8x12 (96-well plate)" }).click();
    await page.getByRole("button", { name: "Add" }).click();

    await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
  });

  test("add box flow with custom dimensions", async ({ page }) => {
    await page.goto("/Storage/boxes/new", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/boxes\/new/);

    const labelInput = page.locator("#box-add-label");
    test.skip(
      (await labelInput.count()) === 0,
      "Add box page unavailable in current test environment",
    );

    await labelInput.fill(`PW Box Custom ${Date.now()}`);
    await page
      .locator("#box-add-code")
      .fill(`BC-${Date.now().toString().slice(-6)}`);
    await page.locator("#box-add-preset").click();
    await page.getByRole("option", { name: "Custom" }).click();
    await page.locator("#box-add-rows").fill("5");
    await page.locator("#box-add-columns").fill("7");
    await page.getByRole("button", { name: "Add" }).click();

    await expect(page).toHaveURL(/\/Storage\/boxes\?t=\d+/);
  });

  test("edit box flow via overflow menu", async ({ page }) => {
    await page.goto("/Storage/boxes", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();

    const overflowMenu = page.locator(
      ".storage-resource-page .cds--overflow-menu",
    );
    test.skip((await overflowMenu.count()) === 0, "No box rows available");
    await overflowMenu.first().click();
    await page.getByRole("menuitem", { name: "Edit" }).click();

    await expect(page).toHaveURL(/\/Storage\/boxes\/\d+\/edit/);
  });

  test("delete box flow with validation handling", async ({ page }) => {
    await page.goto("/Storage/boxes", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Boxes" })).toBeVisible();

    const overflowMenu = page.locator(
      ".storage-resource-page .cds--overflow-menu",
    );
    test.skip((await overflowMenu.count()) === 0, "No box rows available");
    await overflowMenu.first().click();
    await page.getByRole("menuitem", { name: "Delete" }).click();

    await expect(page.getByText("Delete Location")).toBeVisible();
    await page.getByRole("button", { name: "Cancel" }).click();
  });
});
