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

    // OGC-285: the labels section is now test-driven (API mode) — it renders
    // only once the order carries at least one test (the POST
    // /api/orderEntry/labelRequest aggregation returns preset columns). Select a
    // sample type and a panel so the section appears.
    const sampleSelect = page.locator("select#sampleId_0");
    await expect(sampleSelect).toBeVisible({ timeout: UI_TIMEOUT });
    await sampleSelect.selectOption({ index: 1 });

    const panelLabel = page.locator('label:has-text("Bilan Biochimique")');
    await expect(panelLabel).toBeVisible({ timeout: UI_TIMEOUT });
    await panelLabel.click();

    const labelsSection = page.getByTestId("labels-section-root");
    await expect(labelsSection).toBeVisible({ timeout: UI_TIMEOUT });
    // API mode renders the two preset tables ("Order Labels" / "Sample Labels")
    // plus a live "Total labels" row (replacing the legacy "Order labels" input
    // and "Running total" text).
    await expect(labelsSection.getByText("Order Labels")).toBeVisible();
    await expect(labelsSection.getByText(/total labels/i)).toBeVisible();
  });

  test("Generic sample order shows shared labels section", async ({ page }) => {
    await page.goto("/GenericSample/Order", { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("labels-section-root")).toBeVisible();
    await expect(page.getByLabel(/order labels/i)).toBeVisible();
  });
});
