import { expect, test } from "@playwright/test";

test.describe("OGC-284 labels UI", () => {
  test("Add Order shows shared labels section", async ({ page }) => {
    await page.goto("/SamplePatientEntry");

    const addSampleBtn = page.getByRole("button", {
      name: /incomplete add sample/i,
    });
    await expect(addSampleBtn).toBeVisible({ timeout: 10_000 });
    await addSampleBtn.click();
    await expect(page.getByTestId("labels-section-root")).toBeVisible();
    await expect(page.getByLabel(/order labels/i)).toBeVisible();
    await expect(page.getByText(/running total/i)).toBeVisible();
  });

  test("Generic sample order shows shared labels section", async ({ page }) => {
    await page.goto("/GenericSample/Order");

    await expect(page.getByTestId("labels-section-root")).toBeVisible();
    await expect(page.getByLabel(/order labels/i)).toBeVisible();
  });
});
