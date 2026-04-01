import { expect, test } from "@playwright/test";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

async function gotoSamplePatientEntry(page) {
  await page.goto("/SamplePatientEntry", { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-cy="searchPatientTabButton"]')).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

test.describe("OGC-284 labels UI", () => {
  test("Add Order shows shared labels section", async ({ page }) => {
    await gotoSamplePatientEntry(page);

    const addSampleBtn = page.getByRole("button", {
      name: /incomplete add sample/i,
    });
    await expect(addSampleBtn).toBeVisible({ timeout: UI_TIMEOUT });
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
