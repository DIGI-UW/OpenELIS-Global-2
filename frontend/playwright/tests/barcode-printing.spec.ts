import { expect, test } from "@playwright/test";

test.describe("Barcode printing", () => {
  test("Print Barcode page loads and accepts lab number search", async ({
    page,
  }) => {
    await page.goto("/PrintBarcode");
    await expect(
      page.getByRole("heading", { name: "Print Bar Code Labels" }),
    ).toBeVisible({ timeout: 30_000 });
    await expect(page.getByText("Pre-Print Barcodes")).toBeVisible();
    await expect(
      page.getByText("Print Barcodes for Existing Orders"),
    ).toBeVisible();
    await expect(page.locator("#labNumber")).toBeVisible();
    await expect(page.getByRole("button", { name: "Submit" })).toBeVisible();

    await page.locator("#labNumber").fill("TEST-001");
    await expect(page.getByRole("button", { name: "Submit" })).toBeEnabled();
    await page.getByRole("button", { name: "Submit" }).click();

    await expect(page).not.toHaveURL(/\/login/);
  });
});
