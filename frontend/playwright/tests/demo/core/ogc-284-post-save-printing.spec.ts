import { expect, test } from "@playwright/test";

test.describe("OGC-284 post-save printing", () => {
  test("Print Barcode page loads reprint flow entry point", async ({
    page,
  }) => {
    await page.goto("/PrintBarcode", { waitUntil: "domcontentloaded" });

    await expect(
      page.getByRole("heading", { name: /print bar code labels/i }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /pre-print|preprint/i }),
    ).toBeVisible();
    await expect(page.getByLabel(/search site name/i)).toBeVisible();
  });
});
