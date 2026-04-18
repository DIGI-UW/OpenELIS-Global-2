import { test, expect } from "../../../helpers/test-base";

test.describe("Storage Dispose", () => {
  test("dispose sample item from overflow menu", async ({ page }) => {
    await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("heading", { name: "Sample Items", exact: true }),
    ).toBeVisible();

    const overflowMenu = page.locator(
      '[data-testid="sample-actions-overflow-menu"]',
    );
    test.skip((await overflowMenu.count()) === 0, "No sample items available");

    const sampleRow = page.locator("tbody tr").first();
    await sampleRow
      .locator('[data-testid="sample-actions-overflow-menu"]')
      .click();
    await page.getByRole("menuitem", { name: "Dispose" }).click();

    await expect(page.locator('[data-testid="dispose-modal"]')).toBeVisible();
    await page.locator("#disposal-reason").click();
    await page.getByRole("option", { name: "Testing Complete" }).click();
    await page.locator("#disposal-method").click();
    await page.getByRole("option", { name: "Incineration" }).click();
    await page.locator('label[for="disposal-confirmation"]').click();
    await page.getByRole("button", { name: "Confirm Disposal" }).click();

    await expect(page.locator('[data-testid="dispose-modal"]')).toBeHidden();
  });
});
