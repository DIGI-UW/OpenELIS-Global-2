import { expect } from "@playwright/test";
import { test } from "../../../helpers/test-base";
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

    // The Add Sample step renders a per-sample labels section in legacy
    // (count-based) mode — an "Order labels" NumberInput + a "Running total"
    // line — as soon as a sample row exists (no test selection needed; the
    // API-mode two-table section lives on the later Add Order step). The OGC-285
    // rewrite kept this behaviour but dropped the data-testid this asserts on.
    const labelsSection = page.getByTestId("labels-section-root");
    await expect(labelsSection).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      labelsSection.getByLabel(/order labels/i).first(),
    ).toBeVisible();
    await expect(
      labelsSection.getByText(/running total/i).first(),
    ).toBeVisible();
  });

  test("Generic sample order shows shared labels section", async ({ page }) => {
    await page.goto("/GenericSample/Order", { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("labels-section-root")).toBeVisible();
    await expect(page.getByLabel(/order labels/i)).toBeVisible();
  });
});
