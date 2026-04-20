import { test, expect } from "../../../helpers/test-base";
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

    // Carbon ProgressStep composes the accessible name as
    // "{label} {state}" — label first, state second (e.g.
    // "Add Sample Incomplete"). Match on the label only so the
    // selector works regardless of step state and doesn't break
    // when Carbon's hardcoded English state prefix changes.
    const addSampleBtn = page.getByRole("button", { name: /add sample/i });
    await expect(addSampleBtn).toBeVisible({ timeout: UI_TIMEOUT });
    await addSampleBtn.click();
    await expect(page.getByTestId("labels-section-root")).toBeVisible();
    await expect(page.getByLabel(/order labels/i)).toBeVisible();
    await expect(page.getByText(/running total/i)).toBeVisible();
  });

  test("Generic sample order shows shared labels section", async ({ page }) => {
    await page.goto("/GenericSample/Order", { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("labels-section-root")).toBeVisible();
    await expect(page.getByLabel(/order labels/i)).toBeVisible();
  });
});
