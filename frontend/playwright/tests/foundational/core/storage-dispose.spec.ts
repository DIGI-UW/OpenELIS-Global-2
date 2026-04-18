import { test, expect } from "../../../helpers/test-base";

async function pickDisposableSample(page) {
  const rows = page.locator("tbody tr");
  const rowCount = await rows.count();
  expect(
    rowCount,
    "Sample Items table should contain at least one row",
  ).toBeGreaterThan(0);

  for (let i = 0; i < rowCount; i += 1) {
    const row = rows.nth(i);
    const rowText = await row.textContent();
    if (!/Disposed/i.test(rowText || "")) {
      const sampleItemId = (await row.locator("td").nth(0).innerText()).trim();
      return { row, sampleItemId };
    }
  }

  throw new Error("No non-disposed sample row available for disposal test");
}

test.describe("Storage Dispose", () => {
  test("dispose sample item from overflow menu", async ({ page }) => {
    await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("heading", { name: "Sample Items", exact: true }),
    ).toBeVisible();

    const { row: sampleRow, sampleItemId } = await pickDisposableSample(page);
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
    const disposedRow = page.locator("tbody tr", { hasText: sampleItemId });
    await expect(disposedRow).toBeVisible();
    await expect(disposedRow.getByText("Disposed")).toBeVisible();
  });
});
