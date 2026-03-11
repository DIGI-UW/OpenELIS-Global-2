import { expect, test } from "@playwright/test";

test.describe("Barcode configuration", () => {
  test("persists max count and optional field toggles after save/reload", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/barcodeConfiguration");
    await expect(page.locator("#maxOrder")).toBeVisible({ timeout: 30_000 });
    const maxOrderInput = page.locator("#maxOrder");
    const freezerCheckboxLabel = page.locator(
      'label[for="freezerCollectionDateCheck"]',
    );
    const initiallyChecked = await page
      .locator("#freezerCollectionDateCheck")
      .isChecked();

    await maxOrderInput.fill("22");
    await freezerCheckboxLabel.scrollIntoViewIfNeeded();
    await freezerCheckboxLabel.click();
    await expect(page.getByRole("button", { name: "Save" })).toBeEnabled({
      timeout: 5_000,
    });

    const saveResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("BarcodeConfiguration") &&
        resp.request().method() === "POST",
      { timeout: 15_000 },
    );
    await page.getByRole("button", { name: "Save" }).click();
    await saveResponse;

    await page.reload();
    await expect(page.locator("#maxOrder")).toBeVisible({ timeout: 15_000 });
    await expect(page.locator("#maxOrder")).toHaveValue("22");
    await expect(page.locator("#freezerCollectionDateCheck")).toBeChecked({
      checked: !initiallyChecked,
    });
  });
});
